package braseiro.ose.persistence.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Add durable transition hashes and real checkpoint snapshots. No destructive downgrade/fallback. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE action_audit ADD COLUMN postStateHash TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE checkpoints ADD COLUMN schemaVersion INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE checkpoints ADD COLUMN profile TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE checkpoints ADD COLUMN authorityRevision TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE checkpoints ADD COLUMN stateHash TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE checkpoints ADD COLUMN envelopeJson TEXT NOT NULL DEFAULT ''")
    }
}
