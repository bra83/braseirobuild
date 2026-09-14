package braseiro.ose.rag

import braseiro.ose.model.RuleProfile
import braseiro.ose.rules.api.EvidenceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleEvidenceRegistryTest {
    private val registry = RuleEvidenceRegistry()

    @Test fun catalogContainsExactly252UniqueRuleIds() {
        assertEquals(252, registry.registeredRuleIds().size)
    }

    @Test fun profileFirewallRejectsAdvancedOnlyRuleInClassic() {
        val result = registry.lookup(RuleProfile.OSE_CLASSIC_FANTASY, "ADV.PARRY")
        assertEquals(EvidenceStatus.MISSING_EVIDENCE, result.trace.status)
        assertEquals("MISSING_EVIDENCE", result.text)
    }

    @Test fun sharedRuleIsAvailableInBothProfiles() {
        assertTrue(registry.lookup(RuleProfile.OSE_CLASSIC_FANTASY, "REACTION.MONSTER").trace.evidence.isNotEmpty())
        assertTrue(registry.lookup(RuleProfile.OSE_ADVANCED_FANTASY, "REACTION.MONSTER").trace.evidence.isNotEmpty())
    }

    @Test fun missingEvidenceRuleKeepsMissingEvidenceStatus() {
        val id = registry.registeredRuleIds().first {
            registry.lookup(RuleProfile.OSE_ADVANCED_FANTASY, it).trace.status == EvidenceStatus.MISSING_EVIDENCE ||
            registry.lookup(RuleProfile.OSE_CLASSIC_FANTASY, it).trace.status == EvidenceStatus.MISSING_EVIDENCE
        }
        val statuses = listOf(
            registry.lookup(RuleProfile.OSE_CLASSIC_FANTASY, id).trace.status,
            registry.lookup(RuleProfile.OSE_ADVANCED_FANTASY, id).trace.status
        )
        assertTrue(EvidenceStatus.MISSING_EVIDENCE in statuses)
    }
}
