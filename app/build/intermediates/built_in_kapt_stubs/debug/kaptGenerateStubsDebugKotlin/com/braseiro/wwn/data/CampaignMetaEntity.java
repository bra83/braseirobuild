package com.braseiro.wwn.data;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u001e\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0087\b\u0018\u00002\u00020\u0001BO\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\b\u0012\u0006\u0010\n\u001a\u00020\u0003\u0012\u0006\u0010\u000b\u001a\u00020\u0003\u0012\u0006\u0010\f\u001a\u00020\u0003\u0012\u0006\u0010\r\u001a\u00020\u0006\u00a2\u0006\u0004\b\u000e\u0010\u000fJ\t\u0010\u001c\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001f\u001a\u00020\bH\u00c6\u0003J\t\u0010 \u001a\u00020\bH\u00c6\u0003J\t\u0010!\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\"\u001a\u00020\u0003H\u00c6\u0003J\t\u0010#\u001a\u00020\u0003H\u00c6\u0003J\t\u0010$\u001a\u00020\u0006H\u00c6\u0003Jc\u0010%\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\b\b\u0002\u0010\n\u001a\u00020\u00032\b\b\u0002\u0010\u000b\u001a\u00020\u00032\b\b\u0002\u0010\f\u001a\u00020\u00032\b\b\u0002\u0010\r\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010&\u001a\u00020\'2\b\u0010(\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010)\u001a\u00020\bH\u00d6\u0001J\t\u0010*\u001a\u00020\u0003H\u00d6\u0001R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0011R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0016R\u0011\u0010\n\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0011R\u0011\u0010\u000b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0011R\u0011\u0010\f\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0011R\u0011\u0010\r\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0014\u00a8\u0006+"}, d2 = {"Lcom/braseiro/wwn/data/CampaignMetaEntity;", "", "campaignId", "", "displayName", "revision", "", "schemaVersion", "", "appDataVersion", "stateChecksum", "rngAlgorithm", "masterSeed", "updatedAtAudit", "<init>", "(Ljava/lang/String;Ljava/lang/String;JIILjava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V", "getCampaignId", "()Ljava/lang/String;", "getDisplayName", "getRevision", "()J", "getSchemaVersion", "()I", "getAppDataVersion", "getStateChecksum", "getRngAlgorithm", "getMasterSeed", "getUpdatedAtAudit", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "toString", "app"})
@androidx.room.Entity(tableName = "campaign_meta")
public final class CampaignMetaEntity {
    @androidx.room.PrimaryKey()
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String campaignId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String displayName = null;
    private final long revision = 0L;
    private final int schemaVersion = 0;
    private final int appDataVersion = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String stateChecksum = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String rngAlgorithm = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String masterSeed = null;
    private final long updatedAtAudit = 0L;
    
    public CampaignMetaEntity(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId, @org.jetbrains.annotations.NotNull()
    java.lang.String displayName, long revision, int schemaVersion, int appDataVersion, @org.jetbrains.annotations.NotNull()
    java.lang.String stateChecksum, @org.jetbrains.annotations.NotNull()
    java.lang.String rngAlgorithm, @org.jetbrains.annotations.NotNull()
    java.lang.String masterSeed, long updatedAtAudit) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCampaignId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDisplayName() {
        return null;
    }
    
    public final long getRevision() {
        return 0L;
    }
    
    public final int getSchemaVersion() {
        return 0;
    }
    
    public final int getAppDataVersion() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStateChecksum() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRngAlgorithm() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMasterSeed() {
        return null;
    }
    
    public final long getUpdatedAtAudit() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    public final long component3() {
        return 0L;
    }
    
    public final int component4() {
        return 0;
    }
    
    public final int component5() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component8() {
        return null;
    }
    
    public final long component9() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.braseiro.wwn.data.CampaignMetaEntity copy(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId, @org.jetbrains.annotations.NotNull()
    java.lang.String displayName, long revision, int schemaVersion, int appDataVersion, @org.jetbrains.annotations.NotNull()
    java.lang.String stateChecksum, @org.jetbrains.annotations.NotNull()
    java.lang.String rngAlgorithm, @org.jetbrains.annotations.NotNull()
    java.lang.String masterSeed, long updatedAtAudit) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}