package com.braseiro.wwn.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BraseiroDao {
    @Query("SELECT * FROM campaign_meta WHERE campaignId = :campaignId LIMIT 1")
    fun getCampaignMeta(campaignId: String): CampaignMetaEntity?

    @Query("SELECT * FROM campaign_state WHERE campaignId = :campaignId LIMIT 1")
    fun getCampaignState(campaignId: String): CampaignStateEntity?

    @Query("SELECT * FROM campaign_meta ORDER BY updatedAtAudit DESC")
    fun listCampaignMeta(): List<CampaignMetaEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertCampaignMeta(entity: CampaignMetaEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertCampaignState(entity: CampaignStateEntity)

    @Update
    fun updateCampaignMeta(entity: CampaignMetaEntity)

    @Update
    fun updateCampaignState(entity: CampaignStateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertEvent(entity: CampaignEventEntity)

    @Query("SELECT * FROM campaign_event WHERE eventId = :eventId LIMIT 1")
    fun getEvent(eventId: String): CampaignEventEntity?

    @Query("SELECT * FROM campaign_event WHERE campaignId = :campaignId AND eventKind = 'NARRATIVE' AND eventId LIKE :eventIdPrefix ORDER BY createdAtAudit DESC, eventId DESC LIMIT 1")
    fun latestNarrativeByPrefix(campaignId: String, eventIdPrefix: String): CampaignEventEntity?

    @Query("SELECT * FROM campaign_event WHERE campaignId = :campaignId ORDER BY revision ASC, createdAtAudit ASC, eventId ASC")
    fun listEvents(campaignId: String): List<CampaignEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertSnapshot(entity: CampaignSnapshotEntity)

    @Query("SELECT * FROM campaign_snapshot WHERE campaignId = :campaignId ORDER BY revision ASC, createdAtAudit ASC, snapshotId ASC")
    fun listSnapshots(campaignId: String): List<CampaignSnapshotEntity>

    @Query("SELECT COUNT(*) FROM campaign_event WHERE campaignId = :campaignId")
    fun countEvents(campaignId: String): Int

    @Query("SELECT * FROM campaign_snapshot WHERE campaignId = :campaignId ORDER BY revision DESC LIMIT 1")
    fun latestSnapshot(campaignId: String): CampaignSnapshotEntity?

    @Query("SELECT * FROM app_meta WHERE metaKey = :metaKey LIMIT 1")
    fun getAppMeta(metaKey: String): AppMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAppMeta(entity: AppMetaEntity)

    @Query("DELETE FROM campaign_event WHERE campaignId = :campaignId")
    fun deleteEvents(campaignId: String)

    @Query("DELETE FROM campaign_snapshot WHERE campaignId = :campaignId")
    fun deleteSnapshots(campaignId: String)

    @Query("DELETE FROM campaign_state WHERE campaignId = :campaignId")
    fun deleteCampaignState(campaignId: String)

    @Query("DELETE FROM campaign_meta WHERE campaignId = :campaignId")
    fun deleteCampaignMeta(campaignId: String)
}
