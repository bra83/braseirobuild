package braseiro.ose.rules.shared

import braseiro.ose.model.RuleProfile
import braseiro.ose.rules.api.*
import kotlin.test.*

class SharedResolutionFamiliesTest {
    private val p=RuleProfile.OSE_CLASSIC_FANTASY
    @Test fun surpriseUsesOneOrTwo() { assertTrue(SharedConfirmedRules.surprise(p,2).surprised); assertFalse(SharedConfirmedRules.surprise(p,3).surprised) }
    @Test fun initiativeComparesD6AndPreservesTie() { assertEquals(InitiativeOutcome.ACTS_FIRST,SharedConfirmedRules.sideInitiative(p,6,1).outcome); assertEquals(InitiativeOutcome.TIE,SharedConfirmedRules.sideInitiative(p,4,4).outcome) }
    @Test fun reactionBandsMatchGss() { assertEquals(ReactionDisposition.HOSTILE_ATTACKS,SharedConfirmedRules.reaction(p,1,1,0).disposition); assertEquals(ReactionDisposition.NEUTRAL_UNCERTAIN,SharedConfirmedRules.reaction(p,3,3,0).disposition); assertEquals(ReactionDisposition.FRIENDLY_HELPFUL,SharedConfirmedRules.reaction(p,6,6,0).disposition) }
    @Test fun moraleSucceedsAtOrBelowAndIgnoresAdjustmentsAtTwoOrTwelve() { assertTrue(SharedConfirmedRules.morale(p,3,3,6,0).holds); assertFalse(SharedConfirmedRules.morale(p,4,3,6,0).holds); assertEquals(2,SharedConfirmedRules.morale(p,1,1,2,2).adjustedMorale); assertEquals(12,SharedConfirmedRules.morale(p,6,6,12,-2).adjustedMorale) }
    @Test fun xIn6AndPercentileAreRollUnderOrEqual() { assertTrue(SharedConfirmedRules.xIn6(p,2,2).success); assertFalse(SharedConfirmedRules.xIn6(p,3,2).success); assertTrue(SharedConfirmedRules.percentile(p,35,35).success); assertFalse(SharedConfirmedRules.percentile(p,36,35).success) }
}
