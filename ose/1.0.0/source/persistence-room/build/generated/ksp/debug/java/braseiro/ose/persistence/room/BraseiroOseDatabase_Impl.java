package braseiro.ose.persistence.room;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BraseiroOseDatabase_Impl extends BraseiroOseDatabase {
  private volatile CampaignDao _campaignDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `campaigns` (`campaignId` TEXT NOT NULL, `envelopeJson` TEXT NOT NULL, `archived` INTEGER NOT NULL, `commitSequence` INTEGER NOT NULL, PRIMARY KEY(`campaignId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `action_audit` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `campaignId` TEXT NOT NULL, `transitionId` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `postStateHash` TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS `checkpoints` (`checkpointId` TEXT NOT NULL, `campaignId` TEXT NOT NULL, `commitSequence` INTEGER NOT NULL, `schemaVersion` INTEGER NOT NULL DEFAULT 1, `profile` TEXT NOT NULL DEFAULT '', `authorityRevision` TEXT NOT NULL DEFAULT '', `stateHash` TEXT NOT NULL DEFAULT '', `envelopeJson` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`checkpointId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7f25f2a884127be7a1c250ca3136365b')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `campaigns`");
        db.execSQL("DROP TABLE IF EXISTS `action_audit`");
        db.execSQL("DROP TABLE IF EXISTS `checkpoints`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCampaigns = new HashMap<String, TableInfo.Column>(4);
        _columnsCampaigns.put("campaignId", new TableInfo.Column("campaignId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaigns.put("envelopeJson", new TableInfo.Column("envelopeJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaigns.put("archived", new TableInfo.Column("archived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaigns.put("commitSequence", new TableInfo.Column("commitSequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCampaigns = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCampaigns = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCampaigns = new TableInfo("campaigns", _columnsCampaigns, _foreignKeysCampaigns, _indicesCampaigns);
        final TableInfo _existingCampaigns = TableInfo.read(db, "campaigns");
        if (!_infoCampaigns.equals(_existingCampaigns)) {
          return new RoomOpenHelper.ValidationResult(false, "campaigns(braseiro.ose.persistence.room.CampaignEntity).\n"
                  + " Expected:\n" + _infoCampaigns + "\n"
                  + " Found:\n" + _existingCampaigns);
        }
        final HashMap<String, TableInfo.Column> _columnsActionAudit = new HashMap<String, TableInfo.Column>(5);
        _columnsActionAudit.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionAudit.put("campaignId", new TableInfo.Column("campaignId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionAudit.put("transitionId", new TableInfo.Column("transitionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionAudit.put("sequence", new TableInfo.Column("sequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionAudit.put("postStateHash", new TableInfo.Column("postStateHash", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysActionAudit = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesActionAudit = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoActionAudit = new TableInfo("action_audit", _columnsActionAudit, _foreignKeysActionAudit, _indicesActionAudit);
        final TableInfo _existingActionAudit = TableInfo.read(db, "action_audit");
        if (!_infoActionAudit.equals(_existingActionAudit)) {
          return new RoomOpenHelper.ValidationResult(false, "action_audit(braseiro.ose.persistence.room.ActionAuditEntity).\n"
                  + " Expected:\n" + _infoActionAudit + "\n"
                  + " Found:\n" + _existingActionAudit);
        }
        final HashMap<String, TableInfo.Column> _columnsCheckpoints = new HashMap<String, TableInfo.Column>(8);
        _columnsCheckpoints.put("checkpointId", new TableInfo.Column("checkpointId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckpoints.put("campaignId", new TableInfo.Column("campaignId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckpoints.put("commitSequence", new TableInfo.Column("commitSequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckpoints.put("schemaVersion", new TableInfo.Column("schemaVersion", "INTEGER", true, 0, "1", TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckpoints.put("profile", new TableInfo.Column("profile", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckpoints.put("authorityRevision", new TableInfo.Column("authorityRevision", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckpoints.put("stateHash", new TableInfo.Column("stateHash", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckpoints.put("envelopeJson", new TableInfo.Column("envelopeJson", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCheckpoints = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCheckpoints = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCheckpoints = new TableInfo("checkpoints", _columnsCheckpoints, _foreignKeysCheckpoints, _indicesCheckpoints);
        final TableInfo _existingCheckpoints = TableInfo.read(db, "checkpoints");
        if (!_infoCheckpoints.equals(_existingCheckpoints)) {
          return new RoomOpenHelper.ValidationResult(false, "checkpoints(braseiro.ose.persistence.room.CheckpointEntity).\n"
                  + " Expected:\n" + _infoCheckpoints + "\n"
                  + " Found:\n" + _existingCheckpoints);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "7f25f2a884127be7a1c250ca3136365b", "c819f75314bc85d98d117a1ab83e5b1f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "campaigns","action_audit","checkpoints");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `campaigns`");
      _db.execSQL("DELETE FROM `action_audit`");
      _db.execSQL("DELETE FROM `checkpoints`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CampaignDao.class, CampaignDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CampaignDao campaignDao() {
    if (_campaignDao != null) {
      return _campaignDao;
    } else {
      synchronized(this) {
        if(_campaignDao == null) {
          _campaignDao = new CampaignDao_Impl(this);
        }
        return _campaignDao;
      }
    }
  }
}
