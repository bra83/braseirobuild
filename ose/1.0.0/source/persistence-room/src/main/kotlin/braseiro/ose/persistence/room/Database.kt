package braseiro.ose.persistence.room

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "campaigns")
data class CampaignEntity(@PrimaryKey val campaignId: String, val envelopeJson: String, val archived: Boolean = false, val commitSequence: Long = 0)
@Entity(tableName = "action_audit")
data class ActionAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: String,
    val transitionId: String,
    val sequence: Long,
    @ColumnInfo(defaultValue = "") val postStateHash: String = ""
)
@Entity(tableName = "checkpoints")
data class CheckpointEntity(
    @PrimaryKey val checkpointId: String,
    val campaignId: String,
    val commitSequence: Long,
    @ColumnInfo(defaultValue = "1") val schemaVersion: Int = 1,
    @ColumnInfo(defaultValue = "") val profile: String = "",
    @ColumnInfo(defaultValue = "") val authorityRevision: String = "",
    @ColumnInfo(defaultValue = "") val stateHash: String = "",
    @ColumnInfo(defaultValue = "") val envelopeJson: String = ""
)

@Dao
interface CampaignDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertCampaign(entity: CampaignEntity)
    @Query("SELECT * FROM campaigns WHERE campaignId = :campaignId LIMIT 1") fun findCampaign(campaignId: String): CampaignEntity?
    @Query("SELECT * FROM campaigns ORDER BY campaignId") fun listCampaigns(): List<CampaignEntity>
    @Query("UPDATE campaigns SET envelopeJson = :json, commitSequence = :sequence WHERE campaignId = :campaignId") fun updateState(campaignId: String, json: String, sequence: Long): Int
    @Query("UPDATE campaigns SET archived = 1 WHERE campaignId = :campaignId") fun archive(campaignId: String): Int
    @Insert fun insertAudit(entity: ActionAuditEntity)
    @Query("SELECT COUNT(*) FROM action_audit WHERE campaignId = :campaignId") fun auditCount(campaignId: String): Int
    @Query("SELECT * FROM action_audit WHERE campaignId = :campaignId AND transitionId = :transitionId ORDER BY sequence DESC LIMIT 1") fun findTransitionAudit(campaignId: String, transitionId: String): ActionAuditEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertCheckpoint(entity: CheckpointEntity)
    @Query("SELECT * FROM checkpoints WHERE checkpointId = :checkpointId AND campaignId = :campaignId LIMIT 1") fun findCheckpoint(campaignId: String, checkpointId: String): CheckpointEntity?
}

@Database(entities = [CampaignEntity::class, ActionAuditEntity::class, CheckpointEntity::class], version = 2, exportSchema = false)
abstract class BraseiroOseDatabase : RoomDatabase() { abstract fun campaignDao(): CampaignDao }
