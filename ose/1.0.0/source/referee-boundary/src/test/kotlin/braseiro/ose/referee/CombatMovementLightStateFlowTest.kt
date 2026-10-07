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
}