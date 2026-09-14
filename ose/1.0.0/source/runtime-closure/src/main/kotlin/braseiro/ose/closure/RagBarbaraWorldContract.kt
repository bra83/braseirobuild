package braseiro.ose.closure

enum class ImportedSourceRole { REFERENCE_ONLY, ADVENTURE_LOCAL, RULE_AUTHORITY }
enum class AuthorityDecision { REFERENCE_ONLY, APPROVED_LOCAL, APPROVED_RULE_AUTHORITY }

data class ImportedPdfEvidence(
    val documentId: String,
    val sha256: String,
    val requestedRole: ImportedSourceRole,
    val approval: AuthorityDecision = AuthorityDecision.REFERENCE_ONLY
) {
    init { require(documentId.isNotBlank()); require(sha256.matches(Regex("[0-9a-f]{64}"))) }
}

object PdfAuthorityFirewall {
    fun effectiveRole(evidence: ImportedPdfEvidence): ImportedSourceRole = when (evidence.approval) {
        AuthorityDecision.REFERENCE_ONLY -> ImportedSourceRole.REFERENCE_ONLY
        AuthorityDecision.APPROVED_LOCAL -> ImportedSourceRole.ADVENTURE_LOCAL
        AuthorityDecision.APPROVED_RULE_AUTHORITY -> ImportedSourceRole.RULE_AUTHORITY
    }
    fun importCanAutoEscalateAuthority(evidence: ImportedPdfEvidence): Boolean = false
}

data class CanonicalBarbaraInput(
    val campaignId: String,
    val profile: OseProfile,
    val mechanicalStateHash: String,
    val visibleNarration: String,
    val playerKnownFacts: Map<String, String>,
    val visibleNpcSummaries: Map<String, String>,
    val position: TypedSpatialRef,
    val allowedRuleEvidenceRefs: List<String>,
    val secretFacts: Map<String, String> = emptyMap(),
    val hiddenNpcState: Map<String, String> = emptyMap(),
    val rngInternalState: Map<String, ULong> = emptyMap()
)

data class BarbaraSafeProjection(
    val campaignId: String,
    val profile: OseProfile,
    val mechanicalStateHash: String,
    val visibleNarration: String,
    val playerKnownFacts: Map<String, String>,
    val visibleNpcSummaries: Map<String, String>,
    val position: TypedSpatialRef,
    val allowedRuleEvidenceRefs: List<String>
)

object BarbaraProjectionFirewall {
    fun project(input: CanonicalBarbaraInput): BarbaraSafeProjection = BarbaraSafeProjection(
        input.campaignId, input.profile, input.mechanicalStateHash, input.visibleNarration,
        input.playerKnownFacts.toSortedMap(), input.visibleNpcSummaries.toSortedMap(), input.position,
        input.allowedRuleEvidenceRefs.distinct().sorted()
    )
}

data class TypedNpcState(
    val npcId: String,
    val location: TypedSpatialRef,
    val memoryFactIds: Set<String> = emptySet()
) { init { require(npcId.isNotBlank()) } }

data class WorldEventProposal(
    val eventId: String,
    val dueTurn: Long,
    val location: TypedSpatialRef,
    val narrativeCueId: String,
    /** This is a command identifier only. The scheduler never applies mechanics itself. */
    val rulesCommandId: String? = null,
    val resolved: Boolean = false
) {
    init { require(eventId.isNotBlank()); require(dueTurn >= 0); require(narrativeCueId.isNotBlank()) }
}

data class DueWorldEvent(val event: WorldEventProposal, val schedulerAppliedMechanicalMutation: Boolean = false)

object WorldSchedulerContract {
    fun due(events: Collection<WorldEventProposal>, currentTurn: Long): List<DueWorldEvent> {
        require(currentTurn >= 0)
        return events.asSequence().filter { !it.resolved && it.dueTurn <= currentTurn }
            .sortedWith(compareBy<WorldEventProposal>({ it.dueTurn }, { it.eventId }))
            .map { DueWorldEvent(it, false) }.toList()
    }
}
