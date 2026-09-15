package com.braseiro.wwn.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CampaignMetaEntity::class,
        CampaignStateEntity::class,
        CampaignEventEntity::class,
        CampaignSnapshotEntity::class,
        AppMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BraseiroDatabase : RoomDatabase() {
    abstract fun dao(): BraseiroDao

    companion object {
        @Volatile private var INSTANCE: BraseiroDatabase? = null

        fun get(context: Context): BraseiroDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                BraseiroDatabase::class.java,
                "braseiro_wwn.db",
            ).build().also { INSTANCE = it }
        }
    }
}
