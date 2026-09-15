package com.braseiro.wwn.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "campaign_meta")
data class CampaignMetaEntity(
    @PrimaryKey val campaignId: String,
    val displayName: String,
    val revision: Long,
    val schemaVersion: Int,
    val appDataVersion: Int,
    val stateChecksum: String,
    val rngAlgorithm: String,
    val masterSeed: String,
    val updatedAtAudit: Long,
)

@Entity(tableName = "campaign_state")
data class CampaignStateEntity(
    @PrimaryKey val campaignId: String,
    val revision: Long,
    val stateJson: String,
    val checksum: String,
)

@Entity(
    tableName = "campaign_event",
    indices = [Index(value = ["campaignId", "revision"])],
)
data class CampaignEventEntity(
    @PrimaryKey val eventId: String,
    val campaignId: String,
    val revision: Long,
    val eventKind: String,
    val payloadJson: String,
    val checksumBefore: String?,
    val checksumAfter: String,
    val createdAtAudit: Long,
)

@Entity(
    tableName = "campaign_snapshot",
    indices = [Index(value = ["campaignId", "revision"])],
)
data class CampaignSnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val campaignId: String,
    val revision: Long,
    val stateJson: String,
    val checksum: String,
    val schemaVersion: Int,
    val rngAlgorithm: String,
    val rngCountersJson: String,
    val lastEventId: String?,
    val createdAtAudit: Long,
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val metaKey: String,
    val metaValue: String,
)
