package com.braseiro.wwn.data;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\bg\u0018\u00002\u00020\u0001J\u0012\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0012\u0010\u0006\u001a\u0004\u0018\u00010\u00072\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u000e\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00030\tH\'J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0003H\'J\u0010\u0010\r\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0007H\'J\u0010\u0010\u000e\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0003H\'J\u0010\u0010\u000f\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0007H\'J\u0010\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0011H\'J\u0012\u0010\u0012\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0013\u001a\u00020\u0005H\'J\u001a\u0010\u0014\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0015\u001a\u00020\u0005H\'J\u0016\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00110\t2\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0010\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0018H\'J\u0016\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00180\t2\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0010\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0012\u0010\u001c\u001a\u0004\u0018\u00010\u00182\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0012\u0010\u001d\u001a\u0004\u0018\u00010\u001e2\u0006\u0010\u001f\u001a\u00020\u0005H\'J\u0010\u0010 \u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u001eH\'J\u0010\u0010!\u001a\u00020\u000b2\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0010\u0010\"\u001a\u00020\u000b2\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0010\u0010#\u001a\u00020\u000b2\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0010\u0010$\u001a\u00020\u000b2\u0006\u0010\u0004\u001a\u00020\u0005H\'\u00a8\u0006%\u00c0\u0006\u0003"}, d2 = {"Lcom/braseiro/wwn/data/BraseiroDao;", "", "getCampaignMeta", "Lcom/braseiro/wwn/data/CampaignMetaEntity;", "campaignId", "", "getCampaignState", "Lcom/braseiro/wwn/data/CampaignStateEntity;", "listCampaignMeta", "", "insertCampaignMeta", "", "entity", "insertCampaignState", "updateCampaignMeta", "updateCampaignState", "insertEvent", "Lcom/braseiro/wwn/data/CampaignEventEntity;", "getEvent", "eventId", "latestNarrativeByPrefix", "eventIdPrefix", "listEvents", "insertSnapshot", "Lcom/braseiro/wwn/data/CampaignSnapshotEntity;", "listSnapshots", "countEvents", "", "latestSnapshot", "getAppMeta", "Lcom/braseiro/wwn/data/AppMetaEntity;", "metaKey", "upsertAppMeta", "deleteEvents", "deleteSnapshots", "deleteCampaignState", "deleteCampaignMeta", "app"})
@androidx.room.Dao()
public abstract interface BraseiroDao {
    
    @androidx.room.Query(value = "SELECT * FROM campaign_meta WHERE campaignId = :campaignId LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract com.braseiro.wwn.data.CampaignMetaEntity getCampaignMeta(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "SELECT * FROM campaign_state WHERE campaignId = :campaignId LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract com.braseiro.wwn.data.CampaignStateEntity getCampaignState(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "SELECT * FROM campaign_meta ORDER BY updatedAtAudit DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.List<com.braseiro.wwn.data.CampaignMetaEntity> listCampaignMeta();
    
    @androidx.room.Insert(onConflict = 3)
    public abstract void insertCampaignMeta(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.CampaignMetaEntity entity);
    
    @androidx.room.Insert(onConflict = 3)
    public abstract void insertCampaignState(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.CampaignStateEntity entity);
    
    @androidx.room.Update()
    public abstract void updateCampaignMeta(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.CampaignMetaEntity entity);
    
    @androidx.room.Update()
    public abstract void updateCampaignState(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.CampaignStateEntity entity);
    
    @androidx.room.Insert(onConflict = 3)
    public abstract void insertEvent(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.CampaignEventEntity entity);
    
    @androidx.room.Query(value = "SELECT * FROM campaign_event WHERE eventId = :eventId LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract com.braseiro.wwn.data.CampaignEventEntity getEvent(@org.jetbrains.annotations.NotNull()
    java.lang.String eventId);
    
    @androidx.room.Query(value = "SELECT * FROM campaign_event WHERE campaignId = :campaignId AND eventKind = \'NARRATIVE\' AND eventId LIKE :eventIdPrefix ORDER BY createdAtAudit DESC, eventId DESC LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract com.braseiro.wwn.data.CampaignEventEntity latestNarrativeByPrefix(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId, @org.jetbrains.annotations.NotNull()
    java.lang.String eventIdPrefix);
    
    @androidx.room.Query(value = "SELECT * FROM campaign_event WHERE campaignId = :campaignId ORDER BY revision ASC, createdAtAudit ASC, eventId ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.List<com.braseiro.wwn.data.CampaignEventEntity> listEvents(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Insert(onConflict = 3)
    public abstract void insertSnapshot(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.CampaignSnapshotEntity entity);
    
    @androidx.room.Query(value = "SELECT * FROM campaign_snapshot WHERE campaignId = :campaignId ORDER BY revision ASC, createdAtAudit ASC, snapshotId ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.List<com.braseiro.wwn.data.CampaignSnapshotEntity> listSnapshots(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM campaign_event WHERE campaignId = :campaignId")
    public abstract int countEvents(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "SELECT * FROM campaign_snapshot WHERE campaignId = :campaignId ORDER BY revision DESC LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract com.braseiro.wwn.data.CampaignSnapshotEntity latestSnapshot(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "SELECT * FROM app_meta WHERE metaKey = :metaKey LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract com.braseiro.wwn.data.AppMetaEntity getAppMeta(@org.jetbrains.annotations.NotNull()
    java.lang.String metaKey);
    
    @androidx.room.Insert(onConflict = 1)
    public abstract void upsertAppMeta(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.AppMetaEntity entity);
    
    @androidx.room.Query(value = "DELETE FROM campaign_event WHERE campaignId = :campaignId")
    public abstract void deleteEvents(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "DELETE FROM campaign_snapshot WHERE campaignId = :campaignId")
    public abstract void deleteSnapshots(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "DELETE FROM campaign_state WHERE campaignId = :campaignId")
    public abstract void deleteCampaignState(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
    
    @androidx.room.Query(value = "DELETE FROM campaign_meta WHERE campaignId = :campaignId")
    public abstract void deleteCampaignMeta(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId);
}