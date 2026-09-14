package braseiro.ose.referee

import braseiro.ose.model.*
import braseiro.ose.rng.NamedRngStreams
import braseiro.ose.rng.RngStreamId
import braseiro.ose.rules.api.EvidenceStatus
import braseiro.ose.rules.api.OptionIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncounterStateFlowTest {
    private fun envelope(profile: RuleProfile = RuleProfile.OSE_CLASSIC_FANTASY, morale: Boolean = true) = CampaignEnvelope(
        campaignId = CampaignId("test"), createdAtMetadata = "test", ruleProfile = profile,
        authorityRevision = AuthorityRevision("authority", if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "v1.02" else "v1.2", "hash"),
        optionSet = OptionSet(if (morale) listOf(OptionIds.MORALE) else emptyList()),
        generatorRegistry = GeneratorRegistry(), assetManifestRevision = AssetManifestRevision("assets", "1", "hash"),
        campaignState = CampaignState(PartyState(), TimeState(), PositionState(SpatialRef.Scene("scene", "scene")))
    )

    @Test
    fun classicEncounterFollowsCanonicalSequenceAndConclusionCostsOneTurn() {
        val referee = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(42u)
        var e = envelope()
        fun apply(command: String): ResolutionOutcome {
            val result = referee.resolve(e, command, rng)
            e = e.copy(campaignState = result.updatedState)
            return result
        }
        apply("ENCOUNTER_START:test")
        assertEquals(EncounterPhase.SURPRISE_PENDING, e.campaignState.game.encounter!!.phase)
        apply("ENCOUNTER_SURPRISE:BOTH")
        assertEquals(EncounterPhase.DISTANCE_PENDING, e.campaignState.game.encounter!!.phase)
        apply("ENCOUNTER_DISTANCE_CLASSIC:DUNGEON")
        assertEquals(EncounterPhase.INITIATIVE_PENDING, e.campaignState.game.encounter!!.phase)
        assertTrue(e.campaignState.game.encounter!!.distanceMeters in 20..120)
        apply("ENCOUNTER_INIT_SIDE")
        assertEquals(EncounterPhase.ACTIONS, e.campaignState.game.encounter!!.phase)
        apply("ENCOUNTER_REACTION:0")
        apply("ENCOUNTER_COMBAT_START")
        assertEquals(EncounterPhase.COMBAT, e.campaignState.game.encounter!!.phase)
        apply("ENCOUNTER_MORALE:7")
        val before = e.campaignState.time.turns
        apply("ENCOUNTER_RESOLVE")
        assertEquals(EncounterPhase.RESOLVED, e.campaignState.game.encounter!!.phase)
        assertEquals(before + 1, e.campaignState.time.turns)
    }

    @Test
    fun surpriseOnlyRollsSidesThatNeedACheck() {
        val referee = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(9u)
        var e = envelope()
        e = e.copy(campaignState = referee.resolve(e, "ENCOUNTER_START:test", rng).updatedState)
        val before = rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        val result = referee.resolve(e, "ENCOUNTER_SURPRISE:PARTY", rng)
        val after = rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        assertEquals(1L, after - before)
        assertFalse(result.updatedState.game.encounter!!.oppositionSurprised!!)
    }

    @Test
    fun classicDistanceDoesNotFallbackIntoAdvanced() {
        val referee = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(5u)
        var e = envelope(RuleProfile.OSE_ADVANCED_FANTASY)
        e = e.copy(campaignState = referee.resolve(e, "ENCOUNTER_START:test", rng).updatedState)
        e = e.copy(campaignState = referee.resolve(e, "ENCOUNTER_SURPRISE:NONE", rng).updatedState)
        val before = rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        val result = referee.resolve(e, "ENCOUNTER_DISTANCE_CLASSIC:DUNGEON", rng)
        val after = rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        assertEquals(EvidenceStatus.MISSING_EVIDENCE, result.trace.status)
        assertFalse(result.mechanicalMutation)
        assertFalse(result.rngConsumed)
        assertEquals(before, after)
    }

    @Test
    fun disabledMoraleConsumesNoRng() {
        val referee = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(11u)
        var e = envelope(morale = false)
        fun apply(command: String) { e = e.copy(campaignState = referee.resolve(e, command, rng).updatedState) }
        apply("ENCOUNTER_START:test"); apply("ENCOUNTER_SURPRISE:NONE"); apply("ENCOUNTER_DISTANCE_CLASSIC:DUNGEON")
        apply("ENCOUNTER_INIT_SIDE"); apply("ENCOUNTER_COMBAT_START")
        val before = rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        val result = referee.resolve(e, "ENCOUNTER_MORALE:7", rng)
        val after = rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        assertFalse(result.mechanicalMutation); assertFalse(result.rngConsumed); assertEquals(before, after)
    }
}
