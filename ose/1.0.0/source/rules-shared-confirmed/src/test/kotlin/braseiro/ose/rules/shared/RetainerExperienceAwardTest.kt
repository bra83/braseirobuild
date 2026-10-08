package braseiro.ose.rules.shared

import braseiro.ose.model.*
import kotlin.test.*

/** Classic p.57: retainers receive a -50% XP penalty, not 100% PC XP. */
class RetainerExperienceAwardTest {
    private fun xp(profile:RuleProfile,gp:Long,employed:Boolean=true):ExperienceRuntimeSnapshot {
        val pc=XpRecipientSnapshot(
            recipientId="hero",profile=profile,classIds=listOf("FIGHTER"),
            primeModifierPercentByClass=mapOf("FIGHTER" to 0),
            xpByClass=mapOf("FIGHTER" to 0L)
        )
        val retainer=XpRecipientSnapshot(
            recipientId="retainer",profile=profile,retainer=true,
            awardEligible=employed,classIds=listOf("FIGHTER"),
            primeModifierPercentByClass=mapOf("FIGHTER" to 0),
            xpByClass=mapOf("FIGHTER" to 0L)
        )
        return ExperienceRuntimeSnapshot(
            recipients=mapOf(pc.recipientId to pc,retainer.recipientId to retainer),
            treasures=mapOf("hoard" to TreasureXpSnapshot(
                treasureId="hoard",profile=profile,sourceId="SOURCE_TEST",
                gpValue=gp,magical=false,lifecycle=TreasureLifecycle.RECOVERED
            ))
        )
    }

    @Test fun retainersReceiveHalfAwardInBothProfileLedgers() {
        for(profile in RuleProfile.entries) {
            val plan=assertNotNull(TreasureXpRuntimeRules.planAward(profile,xp(profile,100)).plan)
            assertEquals(50L,plan.deltasByRecipientClass.getValue("hero:FIGHTER"))
            assertEquals(25L,plan.deltasByRecipientClass.getValue("retainer:FIGHTER"))
            val applied=TreasureXpRuntimeRules.applyAward(
                GameExtensions(experience=xp(profile,100)),plan
            )
            assertEquals(25L,applied.experience.recipients.getValue("retainer").xpByClass.getValue("FIGHTER"))
            assertEquals(50L,applied.experience.recipients.getValue("hero").xpByClass.getValue("FIGHTER"))
            assertTrue(applied.experience.treasures.getValue("hoard").xpAwarded)
        }
    }

    @Test fun fractionsAreBlockedRatherThanRounded() {
        for(profile in RuleProfile.entries) {
            val recipientRemainder=TreasureXpRuntimeRules.planAward(profile,xp(profile,101))
            assertTrue(recipientRemainder.blocker?.contains("XP_SPLIT_REMAINDER")==true)
            val retainerHalfFraction=TreasureXpRuntimeRules.planAward(profile,xp(profile,102))
            assertTrue(retainerHalfFraction.blocker?.contains("XP_INDIVIDUAL_FRACTION")==true)
        }
    }

    @Test fun retiredRetainerIsAliveButNotEligibleForTheNextAward() {
        for(profile in RuleProfile.entries) {
            val experience=xp(profile,100,employed=false)
            val planned=assertNotNull(TreasureXpRuntimeRules.planAward(profile,experience).plan)
            assertEquals(100L,planned.deltasByRecipientClass.getValue("hero:FIGHTER"))
            assertFalse(planned.deltasByRecipientClass.containsKey("retainer:FIGHTER"))
            assertTrue(experience.recipients.getValue("retainer").surviving)
        }
    }
}
