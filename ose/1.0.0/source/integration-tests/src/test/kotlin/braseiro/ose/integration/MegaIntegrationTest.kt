package braseiro.ose.integration

import braseiro.ose.backup.BackupCodec
import braseiro.ose.barbara.DeterministicBarbaraSupervisor
import braseiro.ose.character.CharacterCreator
import braseiro.ose.dungeon.DungeonGeneratorV1
import braseiro.ose.hex.GeographicCoherenceValidator
import braseiro.ose.hex.HexWorldGeneratorV1
import braseiro.ose.model.*
import braseiro.ose.npc.NpcDomain
import braseiro.ose.persistence.api.*
import braseiro.ose.referee.RulesRefereeBoundary
import braseiro.ose.rules.api.*
import braseiro.ose.session.SessionEngine
import braseiro.ose.settlement.SettlementPackages
import braseiro.ose.settlement.SettlementService
import braseiro.ose.testsupport.Fixtures
import braseiro.ose.world.WorldEventScheduler
import kotlin.test.*

class MegaIntegrationTest {
    private class MemoryRepo : CampaignRepository {
        private val store = linkedMapOf<String, CampaignEnvelope>()
        private val archived = mutableSetOf<String>()
        override fun create(envelope: CampaignEnvelope) {
            check(store.putIfAbsent(envelope.campaignId.value, envelope) == null)
        }
        override fun load(campaignId: CampaignId): CampaignLoadResult =
            store[campaignId.value]?.let { CampaignLoadResult.Loaded(it) } ?: CampaignLoadResult.NotFound(campaignId)
        override fun commit(campaignId: CampaignId, transition: StateTransition) {
            val current = store[campaignId.value] ?: error("missing campaign")
            store[campaignId.value] = current.copy(campaignState = transition.updatedState)
        }
        override fun checkpoint(campaignId: CampaignId, checkpointId: String) {
            check(store.containsKey(campaignId.value)); check(checkpointId.isNotBlank())
        }
        override fun listCampaigns(): List<CampaignSummary> = store.keys.sorted().map { CampaignSummary(CampaignId(it), it in archived) }
        override fun archive(campaignId: CampaignId) { check(store.containsKey(campaignId.value)); archived += campaignId.value }
    }

    @Test
    fun `classic and advanced creation remain isolated and evidence backed`() {
        val creator = CharacterCreator()
        val classic = creator.create(
            CharacterCreationRequest(
                profile = RuleProfile.OSE_CLASSIC_FANTASY,
                method = CreationMethod.CLASSIC,
                characterId = "c",
                name = "Classic",
                rolledAttributes = Attributes(15, 9, 9, 13, 12, 10),
                classIds = listOf("FIGHTER"),
                hpRolls = listOf(6)
            )
        )
        assertIs<CharacterCreationResult.Created>(classic)
        assertEquals(EvidenceStatus.CANONICAL_PROCEDURE, classic.trace.status)

        val advanced = creator.create(
            CharacterCreationRequest(
                profile = RuleProfile.OSE_ADVANCED_FANTASY,
                method = CreationMethod.ADVANCED_BASIC,
                characterId = "a",
                name = "Advanced",
                rolledAttributes = Attributes(13, 15, 12, 13, 12, 10),
                classIds = listOf("MAGIC_USER"),
                hpRolls = listOf(4)
            )
        )
        assertIs<CharacterCreationResult.Created>(advanced)
        assertEquals(EvidenceStatus.CANONICAL_PROCEDURE, advanced.trace.status)

        val forbiddenFallback = creator.create(
            CharacterCreationRequest(
                profile = RuleProfile.OSE_CLASSIC_FANTASY,
                method = CreationMethod.ADVANCED_BASIC,
                characterId = "bad",
                name = "No fallback",
                rolledAttributes = Attributes(10, 10, 10, 10, 10, 10),
                classIds = listOf("FIGHTER"),
                hpRolls = listOf(1)
            )
        )
        assertIs<CharacterCreationResult.Rejected>(forbiddenFallback)
        assertEquals(CreationFailureCode.PROFILE_METHOD_MISMATCH, forbiddenFallback.code)
        assertEquals(EvidenceStatus.MISSING_EVIDENCE, forbiddenFallback.trace.status)
    }

    @Test
    fun `33 long sessions preserve GM_HELP zero delta and PLAYER_REACTION authority`() {
        repeat(33) { i ->
            val repo = MemoryRepo()
            val envelope = Fixtures.campaign(
                "stress-$i",
                if (i % 2 == 0) RuleProfile.OSE_CLASSIC_FANTASY else RuleProfile.OSE_ADVANCED_FANTASY
            )
            repo.create(envelope)
            val session = SessionEngine(repo, RulesRefereeBoundary(), DeterministicBarbaraSupervisor())
            repeat(120) { turn ->
                val beforeHelp = CanonicalStateHash.sha256(session.loadSession(envelope.campaignId))
                session.gmHelp(envelope.campaignId, "turno $turn")
                val afterHelp = CanonicalStateHash.sha256(session.loadSession(envelope.campaignId))
                assertEquals(beforeHelp, afterHelp)

                val reaction = if (turn % 3 == 0) "WAIT_TURN" else "observar"
                val result = session.submitPlayerReaction(envelope.campaignId, reaction)
                assertEquals(turn % 3 == 0, result.mechanicalMutation)
                assertTrue(result.narration.startsWith("Mestre:"))
            }
            val finalState = session.loadSession(envelope.campaignId).campaignState
            assertEquals(40, finalState.time.turns)
            assertEquals(120, finalState.game.actionLog.count { it.channel == "PLAYER_REACTION" })
            assertEquals(SpatialRef.Hex("world-1", 0, 0), finalState.position.primary)
        }
    }

    @Test
    fun `procedural world settlement hundreds of NPCs and backup compose deterministically`() {
        val classicHex = HexWorldGeneratorV1.generate("world-c", 991u, RuleProfile.OSE_CLASSIC_FANTASY)
        val advancedHex = HexWorldGeneratorV1.generate("world-a", 991u, RuleProfile.OSE_ADVANCED_FANTASY)
        assertTrue(GeographicCoherenceValidator.validate(classicHex, RuleProfile.OSE_CLASSIC_FANTASY).pass)
        assertTrue(GeographicCoherenceValidator.validate(advancedHex, RuleProfile.OSE_ADVANCED_FANTASY).pass)
        assertEquals(classicHex, HexWorldGeneratorV1.generate("world-c", 991u, RuleProfile.OSE_CLASSIC_FANTASY))

        val dungeon = DungeonGeneratorV1.generate("dungeon", 992u, 24)
        assertTrue(DungeonGeneratorV1.isReachable(dungeon))
        assertEquals(dungeon, DungeonGeneratorV1.generate("dungeon", 992u, 24))

        val settlement = SettlementPackages.canonicalVillage("town")
        var state = Fixtures.campaign("whole", RuleProfile.OSE_ADVANCED_FANTASY).campaignState.copy(
            game = GameExtensions(
                dungeon = dungeon,
                hexWorld = advancedHex,
                settlement = settlement,
                rngRootSeed = "991"
            )
        )
        state = SettlementService.enter(state, "INN")
        assertEquals(SpatialRef.Settlement("town", "INN"), state.position.primary)

        val npcMap = (0 until 300).associate { i ->
            val id = "npc-$i"
            id to NpcDomain.createStable(
                id,
                "NPC $i",
                if (i % 2 == 0) "town:INN" else "town:GATE",
                setOf("public-$i", "mission-$i")
            ).copy(consequenceFlags = setOf("MISSION_OPEN:mission-$i"))
        }
        val events = (0 until 300).map { i ->
            WorldEventSnapshot("event-$i", (i % 12 + 1).toLong(), "NPC_MISSION_TICK", "npc-$i", "mission-$i progressed")
        }
        var game = state.game.copy(npcs = npcMap, worldEvents = events).canonical()
        assertEquals(300, game.npcs.size)
        assertEquals(300, game.npcs.values.count { npc -> npc.consequenceFlags.any { it.startsWith("MISSION_OPEN:") } })

        game = NpcDomain.remember(game, "npc-12", "the party kept its promise")
        game = NpcDomain.move(game, "npc-12", "world-a:1,1")
        assertEquals(setOf("public-12", "mission-12"), NpcDomain.projectPlausibleKnowledge(game, "npc-12", setOf("public-12", "mission-12", "secret-global")))

        game = WorldEventScheduler.advance(game, 0, 12)
        assertEquals(300, game.worldEvents.count { it.resolved })
        assertTrue(game.npcs.getValue("npc-12").memorySummaries.isNotEmpty())
        state = state.copy(game = game)

        val campaign = Fixtures.campaign("whole", RuleProfile.OSE_ADVANCED_FANTASY).copy(campaignState = state)
        val backup = BackupCodec.exportCampaign(campaign)
        val restored = BackupCodec.importCampaign(backup)
        assertEquals(campaign.canonicalMechanical(), restored.canonicalMechanical())
        assertEquals(CanonicalStateHash.sha256(campaign), CanonicalStateHash.sha256(restored))
    }
}
