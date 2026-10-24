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

/** Add adventure packages, adventure NPC ledger, and life store tables. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS adventure_packages (
                packageKey TEXT PRIMARY KEY NOT NULL,
                scope TEXT NOT NULL,
                worldId TEXT NOT NULL,
                timelineId TEXT NOT NULL,
                activeJson TEXT NOT NULL,
                historyJson TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS adventure_npc_ledger (
                scope TEXT NOT NULL,
                npcId TEXT NOT NULL,
                dataJson TEXT NOT NULL,
                PRIMARY KEY (scope, npcId)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS life_store (
                scope TEXT PRIMARY KEY NOT NULL,
                revision INTEGER NOT NULL,
                day INTEGER NOT NULL,
                stateJson TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
