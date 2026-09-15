package com.braseiro.wwn.data

sealed interface CampaignLoadResult {
    data class Ok(
        val campaignId: String,
        val displayName: String,
        val revision: Long,
        val stateJson: String,
        val checksum: String,
        val rngAlgorithm: String,
        val masterSeed: String,
    ) : CampaignLoadResult

    data object NotFound : CampaignLoadResult
    data class Corrupt(val reason: String) : CampaignLoadResult
    data class IoError(val reason: String) : CampaignLoadResult
}

data class CommitRequest(
    val campaignId: String,
    val expectedRevision: Long,
    val eventId: String,
    val eventKind: String,
    val eventPayloadJson: String,
    val newStateJson: String,
    val snapshotId: String? = null,
    val rngCountersJson: String = "{}",
)

data class CommitReceipt(
    val campaignId: String,
    val previousRevision: Long,
    val newRevision: Long,
    val eventId: String,
    val stateChecksum: String,
    val snapshotId: String?,
)

data class NarrativeAppendRequest(
    val campaignId: String,
    val eventId: String,
    val mechanicalEventId: String,
    val mechanicalRevision: Long,
    val eventPayloadJson: String,
)

data class NarrativeAppendReceipt(
    val campaignId: String,
    val revision: Long,
    val eventId: String,
    val idempotent: Boolean,
)

data class PortableRestoreEvent(
    val eventId: String,
    val campaignId: String,
    val revision: Long,
    val eventKind: String,
    val payloadJson: String,
    val checksumBefore: String?,
    val checksumAfter: String,
    val createdAtAudit: Long,
)

data class PortableRestoreSnapshot(
    val snapshotId: String,
    val campaignId: String,
    val revision: Long,
    val stateJson: String,
    val rngCountersJson: String,
    val lastEventId: String?,
    val createdAtAudit: Long,
)

data class PortableRestorePayload(
    val campaignId: String,
    val displayName: String,
    val revision: Long,
    val stateJson: String,
    val masterSeed: String,
    val rngAlgorithm: String,
    val events: List<PortableRestoreEvent>,
    val snapshots: List<PortableRestoreSnapshot>,
)

data class PortableRestoreReceipt(
    val campaignId: String,
    val revision: Long,
    val stateChecksum: String,
    val eventsRestored: Int,
    val snapshotsRestored: Int,
    val overwritten: Boolean,
)

class RevisionConflictException(message: String) : IllegalStateException(message)
class CampaignRestoreConflictException(message: String) : IllegalStateException(message)
class CampaignRestoreValidationException(message: String) : IllegalArgumentException(message)
