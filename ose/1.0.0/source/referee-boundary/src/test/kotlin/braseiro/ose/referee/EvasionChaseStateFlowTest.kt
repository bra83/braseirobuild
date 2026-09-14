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

class EvasionChaseStateFlowTest {
    private fun envelope(profile: RuleProfile) = CampaignEnvelope(
        campaignId = CampaignId("test"), createdAtMetadata = "test", ruleProfile = profile,
        authorityRevision = AuthorityRevision("authority", if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "v1.02" else "v1.2", "hash"),
        optionSet = OptionSet(listOf(OptionIds.MORALE)), generatorRegistry = GeneratorRegistry(),
        assetManifestRevision = AssetManifestRevision("assets", "1", "hash"),
        campaignState = CampaignState(PartyState(), TimeState(), PositionState(SpatialRef.Scene("scene", "scene")))
    )

    private fun toActions(profile: RuleProfile, context: EncounterDistanceContext, seed: ULong = 42u): Pair<CampaignEnvelope, NamedRngStreams> {
        val referee = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(seed)
        var e = envelope(profile)
        fun apply(command: String) { e = e.copy(campaignState = referee.resolve(e, command, rng).updatedState) }
        apply("ENCOUNTER_START:test"); apply("ENCOUNTER_SURPRISE:NONE")
        apply(if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "ENCOUNTER_DISTANCE_CLASSIC:${context.name}" else "ENCOUNTER_DISTANCE_ADVANCED:${context.name}")
        apply("ENCOUNTER_INIT_SIDE")
        return e to rng
    }

    @Test fun advancedDistanceUsesBoundAdvancedSource() {
        val referee = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(10u); var e=envelope(RuleProfile.OSE_ADVANCED_FANTASY)
        fun apply(c:String)=referee.resolve(e,c,rng).also{e=e.copy(campaignState=it.updatedState)}
        apply("ENCOUNTER_START:test"); apply("ENCOUNTER_SURPRISE:NONE")
        val before=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        val r=apply("ENCOUNTER_DISTANCE_ADVANCED:DUNGEON")
        val after=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        assertTrue(r.mechanicalMutation); assertEquals(2L,after-before)
        assertTrue(e.campaignState.game.encounter!!.distanceMeters!! in 6..36)
    }

    @Test fun classicBothSurprisedWildernessUsesOneD4NotD6() {
        val referee = RulesRefereeBoundary(); val rng = NamedRngStreams.fromRootSeed(11u); var e=envelope(RuleProfile.OSE_CLASSIC_FANTASY)
        e=e.copy(campaignState=referee.resolve(e,"ENCOUNTER_START:test",rng).updatedState)
        val enc=e.campaignState.game.encounter!!.copy(phase=EncounterPhase.DISTANCE_PENDING,partySurprised=true,oppositionSurprised=true)
        e=e.copy(campaignState=e.campaignState.copy(game=e.campaignState.game.copy(encounter=enc)))
        val before=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        val r=referee.resolve(e,"ENCOUNTER_DISTANCE_CLASSIC:WILDERNESS",rng)
        val after=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        assertTrue(r.mechanicalMutation); assertEquals(1L,after-before)
        assertTrue(r.updatedState.game.encounter!!.distanceMeters!! in 10..40)
    }

    @Test fun noPursuitMeansAutomaticEscapeWithoutRng() {
        val (start,rng)=toActions(RuleProfile.OSE_CLASSIC_FANTASY,EncounterDistanceContext.DUNGEON,12u); val referee=RulesRefereeBoundary()
        val before=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        val r=referee.resolve(start,"ENCOUNTER_EVADE:DUNGEON:LET_GO:PARTY",rng)
        val after=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        assertEquals(ChasePhase.ESCAPED,r.updatedState.game.encounter!!.chase!!.phase); assertEquals(before,after); assertFalse(r.rngConsumed)
    }

    @Test fun wildernessBaseTableAndSpeedModifierAreApplied() {
        val (start,rng)=toActions(RuleProfile.OSE_CLASSIC_FANTASY,EncounterDistanceContext.WILDERNESS,13u); val referee=RulesRefereeBoundary()
        val r=referee.resolve(start,"ENCOUNTER_EVADE:WILDERNESS:PURSUE:PARTY:20:40:4:1",rng)
        assertEquals(25,r.updatedState.game.encounter!!.chase!!.evasionChancePercent) // base 50 -25 because pursuer is twice as fast
        assertTrue(r.rngConsumed)
    }

    @Test fun surprisedFleeingSideRequiresRefereeJudgmentAndConsumesNoRng() {
        val referee=RulesRefereeBoundary(); val rng=NamedRngStreams.fromRootSeed(14u); var e=envelope(RuleProfile.OSE_ADVANCED_FANTASY)
        e=e.copy(campaignState=referee.resolve(e,"ENCOUNTER_START:test",rng).updatedState)
        val enc=e.campaignState.game.encounter!!.copy(phase=EncounterPhase.ACTIONS,partySurprised=true,oppositionSurprised=false,distanceContext=EncounterDistanceContext.WILDERNESS,distanceMeters=90)
        e=e.copy(campaignState=e.campaignState.copy(game=e.campaignState.game.copy(encounter=enc)))
        val before=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        val r=referee.resolve(e,"ENCOUNTER_EVADE:WILDERNESS:PURSUE:PARTY:30:30:4:1",rng)
        val after=rng.snapshot().streams.getValue(RngStreamId.RULES_DICE).counter
        assertEquals(EvidenceStatus.REFEREE_JUDGMENT,r.trace.status); assertFalse(r.mechanicalMutation); assertEquals(before,after)
    }

    @Test fun waterEvasionUsesMovementDifferenceTable() {
        val (start,rng)=toActions(RuleProfile.OSE_ADVANCED_FANTASY,EncounterDistanceContext.WATERBORNE,15u); val referee=RulesRefereeBoundary()
        val r=referee.resolve(start,"ENCOUNTER_EVADE:WATERBORNE:PURSUE:PARTY:20:30",rng)
        assertEquals(40,r.updatedState.game.encounter!!.chase!!.evasionChancePercent)
        assertTrue(r.rngConsumed)
    }
}
