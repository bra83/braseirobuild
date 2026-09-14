package braseiro.ose.closure

import java.security.MessageDigest

enum class RecoveryDecision { NOTHING_TO_DO, NARRATION_ONLY, REJECT_CORRUPT_RECORD, REJECT_STATE_MISMATCH }
enum class BackupCollisionPolicy { REJECT_EXISTING, REPLACE_SAME_CAMPAIGN_EXPLICIT }

data class RngCounters(val counters: Map<String, Long>) {
    init { require(counters.values.all { it >= 0 }) }
    fun canonical() = counters.toSortedMap()
}

data class DurableTransitionRecord(
    val campaignId: String,
    val transitionId: String,
    val preStateHash: String,
    val postStateHash: String,
    val rngBefore: RngCounters,
    val rngAfter: RngCounters,
    val mechanicalCommitted: Boolean,
    val narrationCommitted: Boolean
) {
    init {
        require(campaignId.isNotBlank() && transitionId.isNotBlank())
        require(preStateHash.matches(Regex("[0-9a-f]{64}")) && postStateHash.matches(Regex("[0-9a-f]{64}")))
        if (narrationCommitted) require(mechanicalCommitted)
    }
}

data class CheckpointRecord(
    val checkpointId: String,
    val campaignId: String,
    val schemaVersion: Int,
    val profile: OseProfile,
    val authorityRevision: String,
    val stateHash: String,
    val payload: String
) {
    init { require(checkpointId.isNotBlank() && campaignId.isNotBlank()); require(schemaVersion >= 1); require(authorityRevision.isNotBlank()) }
}

data class BackupEnvelope(
    val campaignId: String,
    val schemaVersion: Int,
    val profile: OseProfile,
    val authorityRevision: String,
    val optionSetHash: String,
    val payload: String,
    val payloadSha256: String
) {
    init {
        require(campaignId.isNotBlank()); require(schemaVersion >= 1); require(authorityRevision.isNotBlank())
        require(optionSetHash.matches(Regex("[0-9a-f]{64}"))); require(payloadSha256.matches(Regex("[0-9a-f]{64}")))
    }
}

data class PortableCharacterEnvelope(
    val characterId: String,
    val profile: OseProfile,
    val authorityRevision: String,
    val optionSetHash: String,
    val payload: String,
    val payloadSha256: String
)

data class MigrationStep(val from: Int, val to: Int, val id: String) {
    init { require(from >= 1); require(to == from + 1); require(id.isNotBlank()) }
}

sealed class ValidationResult {
    data object Accepted : ValidationResult()
    data class Rejected(val reason: String) : ValidationResult()
}

class MonotonicMigrationRegistry(steps: List<MigrationStep>) {
    private val byFrom = steps.associateBy { it.from }
    init { require(byFrom.size == steps.size) }
    fun path(from: Int, to: Int): List<MigrationStep>? {
        if (to < from) return null // destructive downgrade forbidden
        if (to == from) return emptyList()
        val out = mutableListOf<MigrationStep>(); var cursor = from
        while (cursor < to) {
            val step = byFrom[cursor] ?: return null
            out += step; cursor = step.to
        }
        return out
    }
}

/** Idempotency must be persisted by the actual Room adapter. */
class TransitionIdempotencyLedger {
    private val committed = linkedMapOf<String, String>()
    fun commit(transitionId: String, postStateHash: String): ValidationResult {
        require(transitionId.isNotBlank())
        val old = committed[transitionId]
        if (old == null) { committed[transitionId] = postStateHash; return ValidationResult.Accepted }
        return if (old == postStateHash) ValidationResult.Accepted else ValidationResult.Rejected("TRANSITION_ID_COLLISION")
    }
}

object PersistenceRecoveryContract {
    fun recoveryDecision(record: DurableTransitionRecord, durableStateHash: String, durableRng: RngCounters): RecoveryDecision {
        if (!record.mechanicalCommitted) return RecoveryDecision.NOTHING_TO_DO
        if (durableStateHash != record.postStateHash || durableRng.canonical() != record.rngAfter.canonical()) return RecoveryDecision.REJECT_STATE_MISMATCH
        return if (record.narrationCommitted) RecoveryDecision.NOTHING_TO_DO else RecoveryDecision.NARRATION_ONLY
    }

    /** A NARRATION_ONLY recovery explicitly does not replay mechanics or consume RNG. */
    fun recoveryConsumesRulesRng(decision: RecoveryDecision): Boolean = false
    fun recoveryReplaysMechanics(decision: RecoveryDecision): Boolean = false

    fun validateBackup(
        backup: BackupEnvelope,
        expectedCurrentSchema: Int,
        migrationRegistry: MonotonicMigrationRegistry,
        existingCampaignIds: Set<String>,
        collisionPolicy: BackupCollisionPolicy,
        expectedProfile: OseProfile? = null,
        expectedAuthorityRevision: String? = null
    ): ValidationResult {
        if (sha256(backup.payload) != backup.payloadSha256) return ValidationResult.Rejected("CHECKSUM_MISMATCH")
        if (backup.schemaVersion > expectedCurrentSchema) return ValidationResult.Rejected("FUTURE_SCHEMA_NOT_SUPPORTED")
        if (migrationRegistry.path(backup.schemaVersion, expectedCurrentSchema) == null) return ValidationResult.Rejected("MIGRATION_PATH_MISSING_OR_DOWNGRADE")
        if (expectedProfile != null && backup.profile != expectedProfile) return ValidationResult.Rejected("PROFILE_MISMATCH")
        if (expectedAuthorityRevision != null && backup.authorityRevision != expectedAuthorityRevision) return ValidationResult.Rejected("AUTHORITY_MISMATCH")
        if (backup.campaignId in existingCampaignIds && collisionPolicy == BackupCollisionPolicy.REJECT_EXISTING) return ValidationResult.Rejected("CAMPAIGN_ID_COLLISION")
        return ValidationResult.Accepted
    }

    fun validateCheckpoint(
        checkpoint: CheckpointRecord,
        expectedCampaignId: String,
        expectedProfile: OseProfile,
        expectedAuthorityRevision: String,
        currentSchema: Int,
        migrationRegistry: MonotonicMigrationRegistry
    ): ValidationResult {
        if (checkpoint.campaignId != expectedCampaignId) return ValidationResult.Rejected("CAMPAIGN_ID_MISMATCH")
        if (checkpoint.profile != expectedProfile) return ValidationResult.Rejected("PROFILE_MISMATCH")
        if (checkpoint.authorityRevision != expectedAuthorityRevision) return ValidationResult.Rejected("AUTHORITY_MISMATCH")
        if (migrationRegistry.path(checkpoint.schemaVersion, currentSchema) == null) return ValidationResult.Rejected("MIGRATION_PATH_MISSING_OR_DOWNGRADE")
        if (sha256(checkpoint.payload) != checkpoint.stateHash) return ValidationResult.Rejected("CHECKPOINT_HASH_MISMATCH")
        return ValidationResult.Accepted
    }

    fun validatePortableCharacter(
        character: PortableCharacterEnvelope,
        campaignProfile: OseProfile,
        campaignAuthorityRevision: String,
        campaignOptionSetHash: String
    ): ValidationResult {
        if (character.characterId.isBlank()) return ValidationResult.Rejected("CHARACTER_ID_REQUIRED")
        if (sha256(character.payload) != character.payloadSha256) return ValidationResult.Rejected("CHARACTER_CHECKSUM_MISMATCH")
        if (character.profile != campaignProfile) return ValidationResult.Rejected("CHARACTER_PROFILE_MISMATCH")
        if (character.authorityRevision != campaignAuthorityRevision) return ValidationResult.Rejected("CHARACTER_AUTHORITY_MISMATCH")
        if (character.optionSetHash != campaignOptionSetHash) return ValidationResult.Rejected("CHARACTER_RULE_OPTIONS_MISMATCH")
        return ValidationResult.Accepted
    }

    fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
