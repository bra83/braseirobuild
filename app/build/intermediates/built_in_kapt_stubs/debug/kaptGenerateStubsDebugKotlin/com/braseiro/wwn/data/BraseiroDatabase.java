package com.braseiro.wwn.data;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u00062\u00020\u0001:\u0001\u0006B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0004\u001a\u00020\u0005H&\u00a8\u0006\u0007"}, d2 = {"Lcom/braseiro/wwn/data/BraseiroDatabase;", "Landroidx/room/RoomDatabase;", "<init>", "()V", "dao", "Lcom/braseiro/wwn/data/BraseiroDao;", "Companion", "app"})
@androidx.room.Database(entities = {com.braseiro.wwn.data.CampaignMetaEntity.class, com.braseiro.wwn.data.CampaignStateEntity.class, com.braseiro.wwn.data.CampaignEventEntity.class, com.braseiro.wwn.data.CampaignSnapshotEntity.class, com.braseiro.wwn.data.AppMetaEntity.class}, version = 1, exportSchema = true)
public abstract class BraseiroDatabase extends androidx.room.RoomDatabase {
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.braseiro.wwn.data.BraseiroDatabase INSTANCE;
    @org.jetbrains.annotations.NotNull()
    public static final com.braseiro.wwn.data.BraseiroDatabase.Companion Companion = null;
    
    public BraseiroDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.braseiro.wwn.data.BraseiroDao dao();
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\bR\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/braseiro/wwn/data/BraseiroDatabase$Companion;", "", "<init>", "()V", "INSTANCE", "Lcom/braseiro/wwn/data/BraseiroDatabase;", "get", "context", "Landroid/content/Context;", "app"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.braseiro.wwn.data.BraseiroDatabase get(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}