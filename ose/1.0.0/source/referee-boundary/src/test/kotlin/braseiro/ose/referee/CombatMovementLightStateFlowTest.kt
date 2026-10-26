package braseiro.ose.referee

import braseiro.ose.model.*
import braseiro.ose.rng.NamedRngStreams
import braseiro.ose.rng.RngStreamId
import braseiro.ose.rules.shared.MovementLightRules
import kotlin.test.*

class CombatMovementLightStateFlowTest {
    private fun envelope(
        profile: RuleProfile = RuleProfile.OSE_CLASSIC_FANTASY,
        options: List<String> = listOf("OPT.MORALE", "OPT.ENCUMBRANCE=BASIC"),
        encounter: EncounterSnapshot = EncounterSnapshot("enc", phase = EncounterPhase.COMBAT)
    ) = CampaignEnvelope(
        campaignId = CampaignId("test"), createdAtMetadata = "test", ruleProfile = profile,
        authorityRevision = AuthorityRevision("OSE", "test", "hash"), optionSet = OptionSet(options),
        generatorRegistry = GeneratorRegistry(), assetManifestRevision = AssetManifestRevision("assets", "1", "hash"),
        campaignState = CampaignState(
            party = PartyState(), time = TimeState(),
            position = PositionState(SpatialRef.Scene("scene", "scene")),
            game = GameExtensions(encounter = encounter)
        )
    )

    @Test fun encumbranceTablesAndProfileSpecificJewelryWeightAreBound() {
        val basic = MovementLightRules.movementRate(RuleProfile.OSE_CLASSIC_FANTASY, OptionSet(listOf("OPT.ENCUMBRANCE=BASIC")), ArmorLoadCategory.HEAVY, 100, 0, true)
        assertEquals(9, basic.baseMoveMeters); assertEquals(3, basic.encounterMoveMeters)
        val detailed = MovementLightRules.movementRate(RuleProfile.OSE_ADVANCED_FANTASY, OptionSet(listOf("OPT.ENCUMBRANCE=DETAILED")), ArmorLoadCategory.NONE, 500, 50, false)
        assertEquals(27, detailed.baseMoveMeters)
        assertEquals(1, MovementLightRules.treasureWeightCoins(RuleProfile.OSE_CLASSIC_FANTASY, "JEWELRY"))
        assertEquals(10, MovementLightRules.treasureWeightCoins(RuleProfile.OSE_ADVANCED_FANTASY, "JEWELRY"))
    }

    @Test fun torchDurationTracksTurnsAndVisibleLightPreventsPartySurprisingOpposition() {
        val boundary = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(71u)
        val torchStack = InventoryItemSnapshot(
            itemId = "torch-stack",
            ownerId = "hero",
            catalogId = "CLASSIC_EQUIP_GEAR_22",
            profile = RuleProfile.OSE_CLASSIC_FANTASY,
            sourceId = "Classic Rules Tome pp.40-45",
            kind = InventoryItemKind.LIGHT_SOURCE,
            quantity = 6,
            carried = true,
            lightSourceKind = LightSourceKind.TORCH
        )
        var e = envelope().let { base ->
            base.copy(campaignState = base.campaignState.copy(
                party = PartyState(listOf("hero")),
                game = base.campaignState.game.copy(
                    inventory = InventorySnapshot(mapOf(torchStack.itemId to torchStack))
                )
            ))
        }
        fun apply(c:String) { e=e.copy(campaignState=boundary.resolve(e,c,rng).updatedState) }
        apply("LIGHT_IGNITE_ITEM:torch-stack:hero")
        val litTorch = e.campaignState.game.expedition.lightSources.single { it.sourceItemId == "torch-stack" }
        assertEquals(6, litTorch.remainingTurns)
        assertTrue(litTorch.lit)
        assertEquals(5, e.campaignState.game.inventory.items.getValue("torch-stack").quantity)
        repeat(6){ apply("WAIT_TURN") }
        assertTrue(e.campaignState.game.expedition.lightSources.none { it.sourceItemId == "torch-stack" && it.lit })

        e = envelope(encounter=EncounterSnapshot("surprise",phase=EncounterPhase.SURPRISE_PENDING)).let { base ->
            base.copy(campaignState=base.campaignState.copy(game=base.campaignState.game.copy(
                expedition=ExpeditionSnapshot(lightSources=listOf(LightSourceSnapshot("t2",LightSourceKind.TORCH,"hero",6,true)))
            )))
        }
        // Named streams are materialized lazily. The surprise command must consume exactly one
        // RULES_DICE draw here: party checks surprise; opposition does not because party light is visible.
        val before=rng.snapshot().streams[RngStreamId.RULES_DICE]?.counter
        val out=boundary.resolve(e,"ENCOUNTER_SURPRISE:BOTH",rng)
        val after=rng.snapshot().streams[RngStreamId.RULES_DICE]?.counter
        val consumed = when {
            before != null && after != null -> after - before
            before == null && after != null -> after
            else -> 0L
        }
        assertEquals(1L,consumed)
        assertFalse(out.updatedState.game.encounter!!.oppositionSurprised!!)
        assertTrue(out.trace.evidence.any{it.ruleId=="LIGHT.SURPRISE"})
    }

    @Test fun combatRoundUsesSourceOrderAndAdvancesExactlyOneRound() {
        val boundary=RulesRefereeBoundary(); val rng=NamedRngStreams.fromRootSeed(123u); var e=envelope()
        fun apply(c:String)=boundary.resolve(e,c,rng).also{e=e.copy(campaignState=it.updatedState)}
        assertTrue(apply("COMBAT_ROUND_START").mechanicalMutation)
        apply("COMBAT_DECLARE:PARTY:hero:NONE"); apply("COMBAT_DECLARATIONS_DONE")
        apply("COMBAT_INIT_SIDE")
        if(e.campaignState.game.encounter!!.combatRound!!.phase==CombatActionPhase.INITIATIVE) apply("COMBAT_INIT_TIE_SIMULTANEOUS")
        repeat(10){ apply("COMBAT_STEP_DONE") }
        assertEquals(CombatActionPhase.COMPLETE,e.campaignState.game.encounter!!.combatRound!!.phase)
        assertEquals(1,e.campaignState.time.rounds)
    }

    @Test fun sideInitiativeDoesNotOverrideIndividualInitiativeOption() {
        val boundary=RulesRefereeBoundary(); val rng=NamedRngStreams.fromRootSeed(9u); var e=envelope(options=listOf("OPT.ENCUMBRANCE=BASIC","OPT.INDIVIDUAL_INITIATIVE"))
        fun apply(c:String)=boundary.resolve(e,c,rng).also{e=e.copy(campaignState=it.updatedState)}
        apply("COMBAT_ROUND_START"); apply("COMBAT_DECLARATIONS_DONE")
        val before=rng.snapshot().streams[RngStreamId.RULES_DICE]?.counter ?: 0L
        val out=apply("COMBAT_INIT_SIDE")
        val after=rng.snapshot().streams[RngStreamId.RULES_DICE]?.counter ?: 0L
        assertFalse(out.mechanicalMutation); assertFalse(out.rngConsumed); assertEquals(before,after)
    }

    @Test fun combatMoveRetreatCalculatesHalfRateAndRegistersMovement() {
        val boundary = RulesRefereeBoundary()
        val rng = NamedRngStreams.fromRootSeed(42u)
        val actor = CombatantSnapshot(
            actorId = "hero",
            side = EncounterSide.PARTY,
            characterId = "hero",
            hpCurrentNumerator = 10,
            hpDenominator = 1,
            armorClassDescending = 9,
            armorClassAscending = 10,
            thac0 = 19,
            attackBonusAscending = 0,
            encounterMoveMeters = 12,
            alive = true
        )
        val combatRound = CombatRoundSnapshot(
            roundNumber = 1,
            phase = CombatActionPhase.MOVEMENT,
            activeSide = EncounterSide.PARTY,
            partyInitiative = 5,
            oppositionInitiative = 3
        )
        val encounter = EncounterSnapshot("enc", phase = EncounterPhase.COMBAT, combatants = mapOf("hero" to actor), combatRound = combatRound)
        val env = envelope(encounter = encounter).let { base ->
            base.copy(campaignState = base.campaignState.copy(
                party = PartyState(listOf("hero")),
                game = base.campaignState.game.copy(encounter = encounter)
            ))
        }

        // RETREAT movement
        val out = boundary.resolve(env, "COMBAT_MOVE:PARTY:hero:RETREAT", rng)
        assertTrue(out.mechanicalMutation, "RETREAT should mutate combat round state")
        assertTrue(out.feedback.contains("Recuo tático de hero registrado: 6m"), "Expected half rate 6m in: ${out.feedback}")
        val roundAfter = out.updatedState.game.encounter?.combatRound
        assertNotNull(roundAfter)
        assertTrue(roundAfter.movedActorKeys.contains("PARTY:hero"))

        // Numeric validated move
        val outNumeric = boundary.resolve(env, "COMBAT_MOVE:PARTY:hero:10", rng)
        assertTrue(outNumeric.mechanicalMutation)
        assertTrue(outNumeric.feedback.contains("10m"))

        // Exceeds encounter move rate -> rejected
        val outExceeds = boundary.resolve(env, "COMBAT_MOVE:PARTY:hero:20", rng)
        assertFalse(outExceeds.mechanicalMutation)
        assertTrue(outExceeds.feedback.contains("excede"))
    }

    @Test fun dungeonProceduresRequireActualDungeonAndExistingDoorsAndSecrets() {
        val boundary = RulesRefereeBoundary()
        val rng = NamedRngStreams.fromRootSeed(99u)

        // 1. Not in dungeon -> rejected
        val notInDungeon = envelope()
        val outNotInDungeon = boundary.resolve(notInDungeon, "DUNGEON_SEARCH_ROOM", rng)
        assertFalse(outNotInDungeon.mechanicalMutation)
        assertTrue(outNotInDungeon.feedback.contains("Ação exclusiva para masmorras"))

        // 2. In dungeon, room with NO secrets -> search does NOT invent secrets
        val node0 = DungeonNodeSnapshot("R00", "ROOM", secret = false)
        val node1 = DungeonNodeSnapshot("R01", "ROOM", secret = false)
        val edge0 = DungeonEdgeSnapshot("R00", "R01", kind = "PASSAGE", secret = false)
        val dWithoutSecret = DungeonSnapshot("d1", 1, 100u, "R00", listOf(node0, node1), listOf(edge0))
        val envDungeon = envelope().let { base ->
            base.copy(campaignState = base.campaignState.copy(
                position = PositionState(SpatialRef.Dungeon("d1", "R00")),
                game = base.campaignState.game.copy(dungeon = dWithoutSecret)
            ))
        }
        val outNoSecret = boundary.resolve(envDungeon, "DUNGEON_SEARCH_ROOM", rng)
        assertTrue(outNoSecret.mechanicalMutation)
        assertTrue(outNoSecret.feedback.contains("Nenhum indício de passagem ou mecanismo oculto foi percebido"))
        assertFalse(outNoSecret.feedback.contains("inexistente", ignoreCase = true), "Busca nao pode revelar conhecimento secreto do Mestre")
        assertFalse(outNoSecret.feedback.contains("não existe", ignoreCase = true), "Ausencia de segredos deve permanecer desconhecida")
        assertEquals(dWithoutSecret.edges, outNoSecret.updatedState.game.dungeon?.edges)

        // 3. Room with NO doors -> listening or forcing door is rejected without mutating state
        val outListenNoDoor = boundary.resolve(envDungeon, "DUNGEON_LISTEN_DOOR", rng)
        assertFalse(outListenNoDoor.mechanicalMutation)
        assertTrue(outListenNoDoor.feedback.contains("Não há portas adjacentes"))

        val outOpenNoDoor = boundary.resolve(envDungeon, "DUNGEON_OPEN_STUCK_DOOR", rng)
        assertFalse(outOpenNoDoor.mechanicalMutation)
        assertTrue(outOpenNoDoor.feedback.contains("Não há portas diante do grupo"))

        // 4. Room with door and secret
        val secretEdge = DungeonEdgeSnapshot("R00", "R02", kind = "PASSAGE", secret = true)
        val doorEdge = DungeonEdgeSnapshot("R00", "R01", kind = "DOOR", secret = false, open = false)
        val node2 = DungeonNodeSnapshot("R02", "ROOM", secret = true)
        val dWithSecret = DungeonSnapshot("d1", 1, 100u, "R00", listOf(node0, node1, node2), listOf(doorEdge, secretEdge))
        val heroChar = CharacterSnapshot(
            characterId = "hero",
            name = "Hero",
            creationMethod = "CLASSIC",
            raceId = "HUMAN",
            classIds = listOf("FIGHTER"),
            levelByClass = mapOf("FIGHTER" to 1),
            xpByClass = mapOf("FIGHTER" to 0L),
            attributes = Attributes(str = 18, int = 10, wis = 10, dex = 10, con = 10, cha = 10),
            hitPoints = HitPoints(10, 10),
            armorClassDescending = 9,
            armorClassAscending = 10,
            thac0 = 19,
            attackBonusAscending = 0,
            savingThrows = SavingThrows(12, 13, 14, 15, 16)
        )
        val envWithFeatures = envelope().let { base ->
            base.copy(campaignState = base.campaignState.copy(
                party = PartyState(listOf("hero")),
                position = PositionState(SpatialRef.Dungeon("d1", "R00")),
                game = base.campaignState.game.copy(
                    characters = mapOf("hero" to heroChar),
                    dungeon = dWithSecret
                )
            ))
        }

        // Listen at existing door
        val outListen = boundary.resolve(envWithFeatures, "DUNGEON_LISTEN_DOOR", rng)
        assertTrue(outListen.mechanicalMutation)
        assertTrue(outListen.feedback.contains("Escutar ruídos na porta da sala R00"))

        // Force door: STR 18 -> success on 1..5 in 6 -> opens door
        val outDoor = boundary.resolve(envWithFeatures, "DUNGEON_OPEN_STUCK_DOOR", rng)
        assertTrue(outDoor.mechanicalMutation)
        val openedDoor = outDoor.updatedState.game.dungeon?.edges?.first { it.from == "R00" && it.to == "R01" }
        assertNotNull(openedDoor)
        assertTrue(openedDoor.open)

        // Forcing an already opened door is rejected
        val envAlreadyOpened = envWithFeatures.copy(campaignState = outDoor.updatedState)
        val outAlreadyOpened = boundary.resolve(envAlreadyOpened, "DUNGEON_OPEN_STUCK_DOOR", rng)
        assertFalse(outAlreadyOpened.mechanicalMutation)
        assertTrue(outAlreadyOpened.feedback.contains("já está aberta"))
    }

    @Test fun settlementRestDayRequiresInnAndSufficientGoldDebitingCanonically() {
        val boundary = RulesRefereeBoundary()
        val rng = NamedRngStreams.fromRootSeed(77u)

        val hero = CharacterSnapshot(
            characterId = "hero",
            name = "Hero",
            creationMethod = "CLASSIC",
            raceId = "HUMAN",
            classIds = listOf("FIGHTER"),
            levelByClass = mapOf("FIGHTER" to 1),
            xpByClass = mapOf("FIGHTER" to 0L),
            attributes = Attributes(str = 10, int = 10, wis = 10, dex = 10, con = 10, cha = 10),
            hitPoints = HitPoints(5, 10, 1),
            armorClassDescending = 9,
            armorClassAscending = 10,
            thac0 = 19,
            attackBonusAscending = 0,
            savingThrows = SavingThrows(12, 13, 14, 15, 16)
        )
        val coins = InventoryItemSnapshot(
            itemId = "gold-coins",
            ownerId = "hero",
            catalogId = "STARTING_GOLD_GP",
            profile = RuleProfile.OSE_CLASSIC_FANTASY,
            sourceId = "Classic Rules Tome",
            kind = InventoryItemKind.COIN,
            quantity = 2
        )

        // 1. Not in settlement -> rejected without mutation or RNG
        val notInSettlement = envelope().let { base ->
            base.copy(campaignState = base.campaignState.copy(
                party = PartyState(listOf("hero")),
                position = PositionState(SpatialRef.Hex("worldId", 0, 0)),
                game = base.campaignState.game.copy(
                    characters = mapOf("hero" to hero),
                    inventory = InventorySnapshot(mapOf(coins.itemId to coins))
                )
            ))
        }
        val outNotSettlement = boundary.resolve(notInSettlement, "SETTLEMENT_REST_DAY", rng)
        assertFalse(outNotSettlement.mechanicalMutation)
        assertFalse(outNotSettlement.rngConsumed)
        assertTrue(outNotSettlement.feedback.contains("atualmente fora de assentamento"))

        // 2. In settlement with 0 gold -> rejected without deducting or curing
        val brokeEnv = envelope().let { base ->
            base.copy(campaignState = base.campaignState.copy(
                party = PartyState(listOf("hero")),
                position = PositionState(SpatialRef.Settlement("village", "inn")),
                game = base.campaignState.game.copy(
                    characters = mapOf("hero" to hero),
                    inventory = InventorySnapshot(emptyMap())
                )
            ))
        }
        val outBroke = boundary.resolve(brokeEnv, "SETTLEMENT_REST_DAY", rng)
        assertFalse(outBroke.mechanicalMutation)
        assertFalse(outBroke.rngConsumed)
        assertTrue(outBroke.feedback.contains("Fundos insuficientes"))
        assertEquals(5, brokeEnv.campaignState.game.characters.getValue("hero").hitPoints.currentNumerator)

        // 3. In settlement with sufficient gold (2 gp for 1 hero) -> 1 gp debited, healed 1d3, 144 turns advanced
        val fundedEnv = envelope().let { base ->
            base.copy(campaignState = base.campaignState.copy(
                party = PartyState(listOf("hero")),
                position = PositionState(SpatialRef.Settlement("village", "inn")),
                game = base.campaignState.game.copy(
                    characters = mapOf("hero" to hero, "reserve" to hero.copy(characterId = "reserve", name = "Reserve")),
                    inventory = InventorySnapshot(mapOf(coins.itemId to coins))
                )
            ))
        }
        val outFunded = boundary.resolve(fundedEnv, "SETTLEMENT_REST_DAY", rng)
        assertTrue(outFunded.mechanicalMutation)
        assertTrue(outFunded.rngConsumed)
        assertTrue(outFunded.feedback.contains("1 po debitados"))
        val remainingCoins = outFunded.updatedState.game.inventory.items.getValue("gold-coins")
        assertEquals(1, remainingCoins.quantity, "Expected exactly 1 gp remaining after 1 gp fee")
        val healedHero = outFunded.updatedState.game.characters.getValue("hero")
        assertTrue(healedHero.hitPoints.currentNumerator > 5, "Hero should have healed 1d3 HP")
        assertEquals(144, outFunded.updatedState.time.turns)
        assertEquals(1, outFunded.updatedState.time.days)
        assertEquals(5, outFunded.updatedState.game.characters.getValue("reserve").hitPoints.currentNumerator,
            "Characters outside the current party must never receive healing or consume dice")

        // Loot is valid gold; neither its origin nor its item ID may block inn payment.
        val goldLoot = coins.copy(itemId = "loot-gold", catalogId = "LOOT_GOLD_GP", quantity = 2)
        val lootEnv = fundedEnv.copy(campaignState = fundedEnv.campaignState.copy(
            game = fundedEnv.campaignState.game.copy(
                inventory = InventorySnapshot(mapOf(goldLoot.itemId to goldLoot))
            )
        ))
        val outLoot = boundary.resolve(lootEnv, "SETTLEMENT_REST_DAY", rng)
        assertTrue(outLoot.mechanicalMutation)
        assertEquals(1, outLoot.updatedState.game.inventory.items.getValue("loot-gold").quantity)
        assertEquals(5, outLoot.updatedState.game.characters.getValue("reserve").hitPoints.currentNumerator)

        // Currency denomination safety: silver is never silently charged as gold.
        val silver = coins.copy(itemId = "silver", catalogId = "SILVER_SP", quantity = 200)
        val silverEnv = fundedEnv.copy(campaignState = fundedEnv.campaignState.copy(
            game = fundedEnv.campaignState.game.copy(inventory = InventorySnapshot(mapOf("silver" to silver)))
        ))
        val outSilver = boundary.resolve(silverEnv, "SETTLEMENT_REST_DAY", rng)
        assertFalse(outSilver.mechanicalMutation)
        assertFalse(outSilver.rngConsumed)
        assertTrue(outSilver.feedback.contains("Fundos insuficientes"))
    }
}
