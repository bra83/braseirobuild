package com.braseiro.wwn.data;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class BraseiroDatabase_Impl extends BraseiroDatabase {
  private volatile BraseiroDao _braseiroDao;

  @Override
  @NonNull
  protected RoomOpenDelegate createOpenDelegate() {
    final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(1, "90c9faaa3fff4c2a1edded1f6a293783", "5fe60d709ed5c25abced12d82cba1389") {
      @Override
      public void createAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `campaign_meta` (`campaignId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `revision` INTEGER NOT NULL, `schemaVersion` INTEGER NOT NULL, `appDataVersion` INTEGER NOT NULL, `stateChecksum` TEXT NOT NULL, `rngAlgorithm` TEXT NOT NULL, `masterSeed` TEXT NOT NULL, `updatedAtAudit` INTEGER NOT NULL, PRIMARY KEY(`campaignId`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `campaign_state` (`campaignId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `stateJson` TEXT NOT NULL, `checksum` TEXT NOT NULL, PRIMARY KEY(`campaignId`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `campaign_event` (`eventId` TEXT NOT NULL, `campaignId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `eventKind` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `checksumBefore` TEXT, `checksumAfter` TEXT NOT NULL, `createdAtAudit` INTEGER NOT NULL, PRIMARY KEY(`eventId`))");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_campaign_event_campaignId_revision` ON `campaign_event` (`campaignId`, `revision`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `campaign_snapshot` (`snapshotId` TEXT NOT NULL, `campaignId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `stateJson` TEXT NOT NULL, `checksum` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `rngAlgorithm` TEXT NOT NULL, `rngCountersJson` TEXT NOT NULL, `lastEventId` TEXT, `createdAtAudit` INTEGER NOT NULL, PRIMARY KEY(`snapshotId`))");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_campaign_snapshot_campaignId_revision` ON `campaign_snapshot` (`campaignId`, `revision`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `app_meta` (`metaKey` TEXT NOT NULL, `metaValue` TEXT NOT NULL, PRIMARY KEY(`metaKey`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '90c9faaa3fff4c2a1edded1f6a293783')");
      }

      @Override
      public void dropAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `campaign_meta`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `campaign_state`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `campaign_event`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `campaign_snapshot`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `app_meta`");
      }

      @Override
      public void onCreate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      public void onOpen(@NonNull final SQLiteConnection connection) {
        internalInitInvalidationTracker(connection);
      }

      @Override
      public void onPreMigrate(@NonNull final SQLiteConnection connection) {
        DBUtil.dropFtsSyncTriggers(connection);
      }

      @Override
      public void onPostMigrate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      @NonNull
      public RoomOpenDelegate.ValidationResult onValidateSchema(
          @NonNull final SQLiteConnection connection) {
        final Map<String, TableInfo.Column> _columnsCampaignMeta = new HashMap<String, TableInfo.Column>(9);
        _columnsCampaignMeta.put("campaignId", new TableInfo.Column("campaignId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("revision", new TableInfo.Column("revision", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("schemaVersion", new TableInfo.Column("schemaVersion", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("appDataVersion", new TableInfo.Column("appDataVersion", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("stateChecksum", new TableInfo.Column("stateChecksum", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("rngAlgorithm", new TableInfo.Column("rngAlgorithm", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("masterSeed", new TableInfo.Column("masterSeed", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignMeta.put("updatedAtAudit", new TableInfo.Column("updatedAtAudit", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysCampaignMeta = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesCampaignMeta = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCampaignMeta = new TableInfo("campaign_meta", _columnsCampaignMeta, _foreignKeysCampaignMeta, _indicesCampaignMeta);
        final TableInfo _existingCampaignMeta = TableInfo.read(connection, "campaign_meta");
        if (!_infoCampaignMeta.equals(_existingCampaignMeta)) {
          return new RoomOpenDelegate.ValidationResult(false, "campaign_meta(com.braseiro.wwn.data.CampaignMetaEntity).\n"
                  + " Expected:\n" + _infoCampaignMeta + "\n"
                  + " Found:\n" + _existingCampaignMeta);
        }
        final Map<String, TableInfo.Column> _columnsCampaignState = new HashMap<String, TableInfo.Column>(4);
        _columnsCampaignState.put("campaignId", new TableInfo.Column("campaignId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignState.put("revision", new TableInfo.Column("revision", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignState.put("stateJson", new TableInfo.Column("stateJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignState.put("checksum", new TableInfo.Column("checksum", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysCampaignState = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesCampaignState = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCampaignState = new TableInfo("campaign_state", _columnsCampaignState, _foreignKeysCampaignState, _indicesCampaignState);
        final TableInfo _existingCampaignState = TableInfo.read(connection, "campaign_state");
        if (!_infoCampaignState.equals(_existingCampaignState)) {
          return new RoomOpenDelegate.ValidationResult(false, "campaign_state(com.braseiro.wwn.data.CampaignStateEntity).\n"
                  + " Expected:\n" + _infoCampaignState + "\n"
                  + " Found:\n" + _existingCampaignState);
        }
        final Map<String, TableInfo.Column> _columnsCampaignEvent = new HashMap<String, TableInfo.Column>(8);
        _columnsCampaignEvent.put("eventId", new TableInfo.Column("eventId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignEvent.put("campaignId", new TableInfo.Column("campaignId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignEvent.put("revision", new TableInfo.Column("revision", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignEvent.put("eventKind", new TableInfo.Column("eventKind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignEvent.put("payloadJson", new TableInfo.Column("payloadJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignEvent.put("checksumBefore", new TableInfo.Column("checksumBefore", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignEvent.put("checksumAfter", new TableInfo.Column("checksumAfter", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignEvent.put("createdAtAudit", new TableInfo.Column("createdAtAudit", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysCampaignEvent = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesCampaignEvent = new HashSet<TableInfo.Index>(1);
        _indicesCampaignEvent.add(new TableInfo.Index("index_campaign_event_campaignId_revision", false, Arrays.asList("campaignId", "revision"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoCampaignEvent = new TableInfo("campaign_event", _columnsCampaignEvent, _foreignKeysCampaignEvent, _indicesCampaignEvent);
        final TableInfo _existingCampaignEvent = TableInfo.read(connection, "campaign_event");
        if (!_infoCampaignEvent.equals(_existingCampaignEvent)) {
          return new RoomOpenDelegate.ValidationResult(false, "campaign_event(com.braseiro.wwn.data.CampaignEventEntity).\n"
                  + " Expected:\n" + _infoCampaignEvent + "\n"
                  + " Found:\n" + _existingCampaignEvent);
        }
        final Map<String, TableInfo.Column> _columnsCampaignSnapshot = new HashMap<String, TableInfo.Column>(10);
        _columnsCampaignSnapshot.put("snapshotId", new TableInfo.Column("snapshotId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("campaignId", new TableInfo.Column("campaignId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("revision", new TableInfo.Column("revision", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("stateJson", new TableInfo.Column("stateJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("checksum", new TableInfo.Column("checksum", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("schemaVersion", new TableInfo.Column("schemaVersion", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("rngAlgorithm", new TableInfo.Column("rngAlgorithm", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("rngCountersJson", new TableInfo.Column("rngCountersJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("lastEventId", new TableInfo.Column("lastEventId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampaignSnapshot.put("createdAtAudit", new TableInfo.Column("createdAtAudit", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysCampaignSnapshot = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesCampaignSnapshot = new HashSet<TableInfo.Index>(1);
        _indicesCampaignSnapshot.add(new TableInfo.Index("index_campaign_snapshot_campaignId_revision", false, Arrays.asList("campaignId", "revision"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoCampaignSnapshot = new TableInfo("campaign_snapshot", _columnsCampaignSnapshot, _foreignKeysCampaignSnapshot, _indicesCampaignSnapshot);
        final TableInfo _existingCampaignSnapshot = TableInfo.read(connection, "campaign_snapshot");
        if (!_infoCampaignSnapshot.equals(_existingCampaignSnapshot)) {
          return new RoomOpenDelegate.ValidationResult(false, "campaign_snapshot(com.braseiro.wwn.data.CampaignSnapshotEntity).\n"
                  + " Expected:\n" + _infoCampaignSnapshot + "\n"
                  + " Found:\n" + _existingCampaignSnapshot);
        }
        final Map<String, TableInfo.Column> _columnsAppMeta = new HashMap<String, TableInfo.Column>(2);
        _columnsAppMeta.put("metaKey", new TableInfo.Column("metaKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppMeta.put("metaValue", new TableInfo.Column("metaValue", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysAppMeta = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesAppMeta = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAppMeta = new TableInfo("app_meta", _columnsAppMeta, _foreignKeysAppMeta, _indicesAppMeta);
        final TableInfo _existingAppMeta = TableInfo.read(connection, "app_meta");
        if (!_infoAppMeta.equals(_existingAppMeta)) {
          return new RoomOpenDelegate.ValidationResult(false, "app_meta(com.braseiro.wwn.data.AppMetaEntity).\n"
                  + " Expected:\n" + _infoAppMeta + "\n"
                  + " Found:\n" + _existingAppMeta);
        }
        return new RoomOpenDelegate.ValidationResult(true, null);
      }
    };
    return _openDelegate;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final Map<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final Map<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "campaign_meta", "campaign_state", "campaign_event", "campaign_snapshot", "app_meta");
  }

  @Override
  public void clearAllTables() {
    super.performClear(false, "campaign_meta", "campaign_state", "campaign_event", "campaign_snapshot", "app_meta");
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BraseiroDao.class, BraseiroDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final Set<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
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
  public BraseiroDao dao() {
    if (_braseiroDao != null) {
      return _braseiroDao;
    } else {
      synchronized(this) {
        if(_braseiroDao == null) {
          _braseiroDao = new BraseiroDao_Impl(this);
        }
        return _braseiroDao;
      }
    }
  }
}
