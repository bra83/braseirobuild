package braseiro.ose.session

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ActionAuthorityPolicyTest {
    @Test fun onlyContractEmployerCanRecruitReconcileOrPay() {
        listOf("RETAINER_RECRUIT:hero:npc:3:1",
               "RETAINER_WAGE_BASELINE:hero:npc",
               "RETAINER_PAY_DAILY:hero:npc").forEach { intent ->
            ActionAuthorityPolicy.requireBoundActor(intent, "hero")
            assertFailsWith<IllegalArgumentException> {
                ActionAuthorityPolicy.requireBoundActor(intent, "other-hero")
            }
            assertFailsWith<IllegalArgumentException> {
                ActionAuthorityPolicy.requireBoundActor(intent, null)
            }
            assertFailsWith<IllegalArgumentException> {
                ActionAuthorityPolicy.requireBoundActor(intent.replace(":hero:", "::"), "hero")
            }
        }
    }
    @Test fun unrelatedActionsKeepCurrentAuthorityPath() {
        ActionAuthorityPolicy.requireBoundActor("LOOK:known-door", null)
        ActionAuthorityPolicy.requireBoundActor("RETAINER_LOYALTY:npc:DANGER", "hero")
    }
    @Test fun administrativeWagesDoNotTriggerCompanionAutonomy() {
        assertFalse(ActionAuthorityPolicy.triggersCompanionAutonomy("RETAINER_PAY_DAILY:hero:npc"))
        assertFalse(ActionAuthorityPolicy.triggersCompanionAutonomy("RETAINER_WAGE_BASELINE:hero:npc"))
        assertTrue(ActionAuthorityPolicy.triggersCompanionAutonomy("WILD_MOVE:0:1"))
    }
}
