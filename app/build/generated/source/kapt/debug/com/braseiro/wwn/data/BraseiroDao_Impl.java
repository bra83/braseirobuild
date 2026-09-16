package com.braseiro.wwn.data;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import java.lang.Class;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class BraseiroDao_Impl implements BraseiroDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<CampaignMetaEntity> __insertAdapterOfCampaignMetaEntity;

  private final EntityInsertAdapter<CampaignStateEntity> __insertAdapterOfCampaignStateEntity;

  private final EntityInsertAdapter<CampaignEventEntity> __insertAdapterOfCampaignEventEntity;

  private final EntityInsertAdapter<CampaignSnapshotEntity> __insertAdapterOfCampaignSnapshotEntity;

  private final EntityInsertAdapter<AppMetaEntity> __insertAdapterOfAppMetaEntity;

  private final EntityDeleteOrUpdateAdapter<CampaignMetaEntity> __updateAdapterOfCampaignMetaEntity;

  private final EntityDeleteOrUpdateAdapter<CampaignStateEntity> __updateAdapterOfCampaignStateEntity;

  public BraseiroDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfCampaignMetaEntity = new EntityInsertAdapter<CampaignMetaEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `campaign_meta` (`campaignId`,`displayName`,`revision`,`schemaVersion`,`appDataVersion`,`stateChecksum`,`rngAlgorithm`,`masterSeed`,`updatedAtAudit`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final CampaignMetaEntity entity) {
        if (entity.getCampaignId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getCampaignId());
        }
        if (entity.getDisplayName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getDisplayName());
        }
        statement.bindLong(3, entity.getRevision());
        statement.bindLong(4, entity.getSchemaVersion());
        statement.bindLong(5, entity.getAppDataVersion());
        if (entity.getStateChecksum() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getStateChecksum());
        }
        if (entity.getRngAlgorithm() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getRngAlgorithm());
        }
        if (entity.getMasterSeed() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getMasterSeed());
        }
        statement.bindLong(9, entity.getUpdatedAtAudit());
      }
    };
    this.__insertAdapterOfCampaignStateEntity = new EntityInsertAdapter<CampaignStateEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `campaign_state` (`campaignId`,`revision`,`stateJson`,`checksum`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final CampaignStateEntity entity) {
        if (entity.getCampaignId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getCampaignId());
        }
        statement.bindLong(2, entity.getRevision());
        if (entity.getStateJson() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getStateJson());
        }
        if (entity.getChecksum() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getChecksum());
        }
      }
    };
    this.__insertAdapterOfCampaignEventEntity = new EntityInsertAdapter<CampaignEventEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `campaign_event` (`eventId`,`campaignId`,`revision`,`eventKind`,`payloadJson`,`checksumBefore`,`checksumAfter`,`createdAtAudit`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final CampaignEventEntity entity) {
        if (entity.getEventId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getEventId());
        }
        if (entity.getCampaignId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getCampaignId());
        }
        statement.bindLong(3, entity.getRevision());
        if (entity.getEventKind() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getEventKind());
        }
        if (entity.getPayloadJson() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getPayloadJson());
        }
        if (entity.getChecksumBefore() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getChecksumBefore());
        }
        if (entity.getChecksumAfter() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getChecksumAfter());
        }
        statement.bindLong(8, entity.getCreatedAtAudit());
      }
    };
    this.__insertAdapterOfCampaignSnapshotEntity = new EntityInsertAdapter<CampaignSnapshotEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `campaign_snapshot` (`snapshotId`,`campaignId`,`revision`,`stateJson`,`checksum`,`schemaVersion`,`rngAlgorithm`,`rngCountersJson`,`lastEventId`,`createdAtAudit`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final CampaignSnapshotEntity entity) {
        if (entity.getSnapshotId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getSnapshotId());
        }
        if (entity.getCampaignId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getCampaignId());
        }
        statement.bindLong(3, entity.getRevision());
        if (entity.getStateJson() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getStateJson());
        }
        if (entity.getChecksum() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getChecksum());
        }
        statement.bindLong(6, entity.getSchemaVersion());
        if (entity.getRngAlgorithm() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getRngAlgorithm());
        }
        if (entity.getRngCountersJson() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getRngCountersJson());
        }
        if (entity.getLastEventId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getLastEventId());
        }
        statement.bindLong(10, entity.getCreatedAtAudit());
      }
    };
    this.__insertAdapterOfAppMetaEntity = new EntityInsertAdapter<AppMetaEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_meta` (`metaKey`,`metaValue`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final AppMetaEntity entity) {
        if (entity.getMetaKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getMetaKey());
        }
        if (entity.getMetaValue() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getMetaValue());
        }
      }
    };
    this.__updateAdapterOfCampaignMetaEntity = new EntityDeleteOrUpdateAdapter<CampaignMetaEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `campaign_meta` SET `campaignId` = ?,`displayName` = ?,`revision` = ?,`schemaVersion` = ?,`appDataVersion` = ?,`stateChecksum` = ?,`rngAlgorithm` = ?,`masterSeed` = ?,`updatedAtAudit` = ? WHERE `campaignId` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final CampaignMetaEntity entity) {
        if (entity.getCampaignId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getCampaignId());
        }
        if (entity.getDisplayName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getDisplayName());
        }
        statement.bindLong(3, entity.getRevision());
        statement.bindLong(4, entity.getSchemaVersion());
        statement.bindLong(5, entity.getAppDataVersion());
        if (entity.getStateChecksum() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getStateChecksum());
        }
        if (entity.getRngAlgorithm() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getRngAlgorithm());
        }
        if (entity.getMasterSeed() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getMasterSeed());
        }
        statement.bindLong(9, entity.getUpdatedAtAudit());
        if (entity.getCampaignId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindText(10, entity.getCampaignId());
        }
      }
    };
    this.__updateAdapterOfCampaignStateEntity = new EntityDeleteOrUpdateAdapter<CampaignStateEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `campaign_state` SET `campaignId` = ?,`revision` = ?,`stateJson` = ?,`checksum` = ? WHERE `campaignId` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          @NonNull final CampaignStateEntity entity) {
        if (entity.getCampaignId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getCampaignId());
        }
        statement.bindLong(2, entity.getRevision());
        if (entity.getStateJson() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getStateJson());
        }
        if (entity.getChecksum() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getChecksum());
        }
        if (entity.getCampaignId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getCampaignId());
        }
      }
    };
  }

  @Override
  public void insertCampaignMeta(final CampaignMetaEntity entity) {
    if (entity == null) throw new NullPointerException();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfCampaignMetaEntity.insert(_connection, entity);
      return null;
    });
  }

  @Override
  public void insertCampaignState(final CampaignStateEntity entity) {
    if (entity == null) throw new NullPointerException();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfCampaignStateEntity.insert(_connection, entity);
      return null;
    });
  }

  @Override
  public void insertEvent(final CampaignEventEntity entity) {
    if (entity == null) throw new NullPointerException();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfCampaignEventEntity.insert(_connection, entity);
      return null;
    });
  }

  @Override
  public void insertSnapshot(final CampaignSnapshotEntity entity) {
    if (entity == null) throw new NullPointerException();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfCampaignSnapshotEntity.insert(_connection, entity);
      return null;
    });
  }

  @Override
  public void upsertAppMeta(final AppMetaEntity entity) {
    if (entity == null) throw new NullPointerException();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfAppMetaEntity.insert(_connection, entity);
      return null;
    });
  }

  @Override
  public void updateCampaignMeta(final CampaignMetaEntity entity) {
    if (entity == null) throw new NullPointerException();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfCampaignMetaEntity.handle(_connection, entity);
      return null;
    });
  }

  @Override
  public void updateCampaignState(final CampaignStateEntity entity) {
    if (entity == null) throw new NullPointerException();
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfCampaignStateEntity.handle(_connection, entity);
      return null;
    });
  }

  @Override
  public CampaignMetaEntity getCampaignMeta(final String campaignId) {
    final String _sql = "SELECT * FROM campaign_meta WHERE campaignId = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfDisplayName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "displayName");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfSchemaVersion = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "schemaVersion");
        final int _columnIndexOfAppDataVersion = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appDataVersion");
        final int _columnIndexOfStateChecksum = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stateChecksum");
        final int _columnIndexOfRngAlgorithm = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rngAlgorithm");
        final int _columnIndexOfMasterSeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "masterSeed");
        final int _columnIndexOfUpdatedAtAudit = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "updatedAtAudit");
        final CampaignMetaEntity _result;
        if (_stmt.step()) {
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final String _tmpDisplayName;
          if (_stmt.isNull(_columnIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final int _tmpSchemaVersion;
          _tmpSchemaVersion = (int) (_stmt.getLong(_columnIndexOfSchemaVersion));
          final int _tmpAppDataVersion;
          _tmpAppDataVersion = (int) (_stmt.getLong(_columnIndexOfAppDataVersion));
          final String _tmpStateChecksum;
          if (_stmt.isNull(_columnIndexOfStateChecksum)) {
            _tmpStateChecksum = null;
          } else {
            _tmpStateChecksum = _stmt.getText(_columnIndexOfStateChecksum);
          }
          final String _tmpRngAlgorithm;
          if (_stmt.isNull(_columnIndexOfRngAlgorithm)) {
            _tmpRngAlgorithm = null;
          } else {
            _tmpRngAlgorithm = _stmt.getText(_columnIndexOfRngAlgorithm);
          }
          final String _tmpMasterSeed;
          if (_stmt.isNull(_columnIndexOfMasterSeed)) {
            _tmpMasterSeed = null;
          } else {
            _tmpMasterSeed = _stmt.getText(_columnIndexOfMasterSeed);
          }
          final long _tmpUpdatedAtAudit;
          _tmpUpdatedAtAudit = _stmt.getLong(_columnIndexOfUpdatedAtAudit);
          _result = new CampaignMetaEntity(_tmpCampaignId,_tmpDisplayName,_tmpRevision,_tmpSchemaVersion,_tmpAppDataVersion,_tmpStateChecksum,_tmpRngAlgorithm,_tmpMasterSeed,_tmpUpdatedAtAudit);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public CampaignStateEntity getCampaignState(final String campaignId) {
    final String _sql = "SELECT * FROM campaign_state WHERE campaignId = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfStateJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stateJson");
        final int _columnIndexOfChecksum = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksum");
        final CampaignStateEntity _result;
        if (_stmt.step()) {
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final String _tmpStateJson;
          if (_stmt.isNull(_columnIndexOfStateJson)) {
            _tmpStateJson = null;
          } else {
            _tmpStateJson = _stmt.getText(_columnIndexOfStateJson);
          }
          final String _tmpChecksum;
          if (_stmt.isNull(_columnIndexOfChecksum)) {
            _tmpChecksum = null;
          } else {
            _tmpChecksum = _stmt.getText(_columnIndexOfChecksum);
          }
          _result = new CampaignStateEntity(_tmpCampaignId,_tmpRevision,_tmpStateJson,_tmpChecksum);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<CampaignMetaEntity> listCampaignMeta() {
    final String _sql = "SELECT * FROM campaign_meta ORDER BY updatedAtAudit DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfDisplayName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "displayName");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfSchemaVersion = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "schemaVersion");
        final int _columnIndexOfAppDataVersion = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "appDataVersion");
        final int _columnIndexOfStateChecksum = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stateChecksum");
        final int _columnIndexOfRngAlgorithm = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rngAlgorithm");
        final int _columnIndexOfMasterSeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "masterSeed");
        final int _columnIndexOfUpdatedAtAudit = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "updatedAtAudit");
        final List<CampaignMetaEntity> _result = new ArrayList<CampaignMetaEntity>();
        while (_stmt.step()) {
          final CampaignMetaEntity _item;
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final String _tmpDisplayName;
          if (_stmt.isNull(_columnIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final int _tmpSchemaVersion;
          _tmpSchemaVersion = (int) (_stmt.getLong(_columnIndexOfSchemaVersion));
          final int _tmpAppDataVersion;
          _tmpAppDataVersion = (int) (_stmt.getLong(_columnIndexOfAppDataVersion));
          final String _tmpStateChecksum;
          if (_stmt.isNull(_columnIndexOfStateChecksum)) {
            _tmpStateChecksum = null;
          } else {
            _tmpStateChecksum = _stmt.getText(_columnIndexOfStateChecksum);
          }
          final String _tmpRngAlgorithm;
          if (_stmt.isNull(_columnIndexOfRngAlgorithm)) {
            _tmpRngAlgorithm = null;
          } else {
            _tmpRngAlgorithm = _stmt.getText(_columnIndexOfRngAlgorithm);
          }
          final String _tmpMasterSeed;
          if (_stmt.isNull(_columnIndexOfMasterSeed)) {
            _tmpMasterSeed = null;
          } else {
            _tmpMasterSeed = _stmt.getText(_columnIndexOfMasterSeed);
          }
          final long _tmpUpdatedAtAudit;
          _tmpUpdatedAtAudit = _stmt.getLong(_columnIndexOfUpdatedAtAudit);
          _item = new CampaignMetaEntity(_tmpCampaignId,_tmpDisplayName,_tmpRevision,_tmpSchemaVersion,_tmpAppDataVersion,_tmpStateChecksum,_tmpRngAlgorithm,_tmpMasterSeed,_tmpUpdatedAtAudit);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public CampaignEventEntity getEvent(final String eventId) {
    final String _sql = "SELECT * FROM campaign_event WHERE eventId = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (eventId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, eventId);
        }
        final int _columnIndexOfEventId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "eventId");
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfEventKind = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "eventKind");
        final int _columnIndexOfPayloadJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "payloadJson");
        final int _columnIndexOfChecksumBefore = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksumBefore");
        final int _columnIndexOfChecksumAfter = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksumAfter");
        final int _columnIndexOfCreatedAtAudit = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createdAtAudit");
        final CampaignEventEntity _result;
        if (_stmt.step()) {
          final String _tmpEventId;
          if (_stmt.isNull(_columnIndexOfEventId)) {
            _tmpEventId = null;
          } else {
            _tmpEventId = _stmt.getText(_columnIndexOfEventId);
          }
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final String _tmpEventKind;
          if (_stmt.isNull(_columnIndexOfEventKind)) {
            _tmpEventKind = null;
          } else {
            _tmpEventKind = _stmt.getText(_columnIndexOfEventKind);
          }
          final String _tmpPayloadJson;
          if (_stmt.isNull(_columnIndexOfPayloadJson)) {
            _tmpPayloadJson = null;
          } else {
            _tmpPayloadJson = _stmt.getText(_columnIndexOfPayloadJson);
          }
          final String _tmpChecksumBefore;
          if (_stmt.isNull(_columnIndexOfChecksumBefore)) {
            _tmpChecksumBefore = null;
          } else {
            _tmpChecksumBefore = _stmt.getText(_columnIndexOfChecksumBefore);
          }
          final String _tmpChecksumAfter;
          if (_stmt.isNull(_columnIndexOfChecksumAfter)) {
            _tmpChecksumAfter = null;
          } else {
            _tmpChecksumAfter = _stmt.getText(_columnIndexOfChecksumAfter);
          }
          final long _tmpCreatedAtAudit;
          _tmpCreatedAtAudit = _stmt.getLong(_columnIndexOfCreatedAtAudit);
          _result = new CampaignEventEntity(_tmpEventId,_tmpCampaignId,_tmpRevision,_tmpEventKind,_tmpPayloadJson,_tmpChecksumBefore,_tmpChecksumAfter,_tmpCreatedAtAudit);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public CampaignEventEntity latestNarrativeByPrefix(final String campaignId,
      final String eventIdPrefix) {
    final String _sql = "SELECT * FROM campaign_event WHERE campaignId = ? AND eventKind = 'NARRATIVE' AND eventId LIKE ? ORDER BY createdAtAudit DESC, eventId DESC LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        _argIndex = 2;
        if (eventIdPrefix == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, eventIdPrefix);
        }
        final int _columnIndexOfEventId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "eventId");
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfEventKind = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "eventKind");
        final int _columnIndexOfPayloadJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "payloadJson");
        final int _columnIndexOfChecksumBefore = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksumBefore");
        final int _columnIndexOfChecksumAfter = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksumAfter");
        final int _columnIndexOfCreatedAtAudit = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createdAtAudit");
        final CampaignEventEntity _result;
        if (_stmt.step()) {
          final String _tmpEventId;
          if (_stmt.isNull(_columnIndexOfEventId)) {
            _tmpEventId = null;
          } else {
            _tmpEventId = _stmt.getText(_columnIndexOfEventId);
          }
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final String _tmpEventKind;
          if (_stmt.isNull(_columnIndexOfEventKind)) {
            _tmpEventKind = null;
          } else {
            _tmpEventKind = _stmt.getText(_columnIndexOfEventKind);
          }
          final String _tmpPayloadJson;
          if (_stmt.isNull(_columnIndexOfPayloadJson)) {
            _tmpPayloadJson = null;
          } else {
            _tmpPayloadJson = _stmt.getText(_columnIndexOfPayloadJson);
          }
          final String _tmpChecksumBefore;
          if (_stmt.isNull(_columnIndexOfChecksumBefore)) {
            _tmpChecksumBefore = null;
          } else {
            _tmpChecksumBefore = _stmt.getText(_columnIndexOfChecksumBefore);
          }
          final String _tmpChecksumAfter;
          if (_stmt.isNull(_columnIndexOfChecksumAfter)) {
            _tmpChecksumAfter = null;
          } else {
            _tmpChecksumAfter = _stmt.getText(_columnIndexOfChecksumAfter);
          }
          final long _tmpCreatedAtAudit;
          _tmpCreatedAtAudit = _stmt.getLong(_columnIndexOfCreatedAtAudit);
          _result = new CampaignEventEntity(_tmpEventId,_tmpCampaignId,_tmpRevision,_tmpEventKind,_tmpPayloadJson,_tmpChecksumBefore,_tmpChecksumAfter,_tmpCreatedAtAudit);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<CampaignEventEntity> listEvents(final String campaignId) {
    final String _sql = "SELECT * FROM campaign_event WHERE campaignId = ? ORDER BY revision ASC, createdAtAudit ASC, eventId ASC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        final int _columnIndexOfEventId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "eventId");
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfEventKind = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "eventKind");
        final int _columnIndexOfPayloadJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "payloadJson");
        final int _columnIndexOfChecksumBefore = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksumBefore");
        final int _columnIndexOfChecksumAfter = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksumAfter");
        final int _columnIndexOfCreatedAtAudit = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createdAtAudit");
        final List<CampaignEventEntity> _result = new ArrayList<CampaignEventEntity>();
        while (_stmt.step()) {
          final CampaignEventEntity _item;
          final String _tmpEventId;
          if (_stmt.isNull(_columnIndexOfEventId)) {
            _tmpEventId = null;
          } else {
            _tmpEventId = _stmt.getText(_columnIndexOfEventId);
          }
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final String _tmpEventKind;
          if (_stmt.isNull(_columnIndexOfEventKind)) {
            _tmpEventKind = null;
          } else {
            _tmpEventKind = _stmt.getText(_columnIndexOfEventKind);
          }
          final String _tmpPayloadJson;
          if (_stmt.isNull(_columnIndexOfPayloadJson)) {
            _tmpPayloadJson = null;
          } else {
            _tmpPayloadJson = _stmt.getText(_columnIndexOfPayloadJson);
          }
          final String _tmpChecksumBefore;
          if (_stmt.isNull(_columnIndexOfChecksumBefore)) {
            _tmpChecksumBefore = null;
          } else {
            _tmpChecksumBefore = _stmt.getText(_columnIndexOfChecksumBefore);
          }
          final String _tmpChecksumAfter;
          if (_stmt.isNull(_columnIndexOfChecksumAfter)) {
            _tmpChecksumAfter = null;
          } else {
            _tmpChecksumAfter = _stmt.getText(_columnIndexOfChecksumAfter);
          }
          final long _tmpCreatedAtAudit;
          _tmpCreatedAtAudit = _stmt.getLong(_columnIndexOfCreatedAtAudit);
          _item = new CampaignEventEntity(_tmpEventId,_tmpCampaignId,_tmpRevision,_tmpEventKind,_tmpPayloadJson,_tmpChecksumBefore,_tmpChecksumAfter,_tmpCreatedAtAudit);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<CampaignSnapshotEntity> listSnapshots(final String campaignId) {
    final String _sql = "SELECT * FROM campaign_snapshot WHERE campaignId = ? ORDER BY revision ASC, createdAtAudit ASC, snapshotId ASC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        final int _columnIndexOfSnapshotId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "snapshotId");
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfStateJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stateJson");
        final int _columnIndexOfChecksum = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksum");
        final int _columnIndexOfSchemaVersion = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "schemaVersion");
        final int _columnIndexOfRngAlgorithm = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rngAlgorithm");
        final int _columnIndexOfRngCountersJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rngCountersJson");
        final int _columnIndexOfLastEventId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastEventId");
        final int _columnIndexOfCreatedAtAudit = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createdAtAudit");
        final List<CampaignSnapshotEntity> _result = new ArrayList<CampaignSnapshotEntity>();
        while (_stmt.step()) {
          final CampaignSnapshotEntity _item;
          final String _tmpSnapshotId;
          if (_stmt.isNull(_columnIndexOfSnapshotId)) {
            _tmpSnapshotId = null;
          } else {
            _tmpSnapshotId = _stmt.getText(_columnIndexOfSnapshotId);
          }
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final String _tmpStateJson;
          if (_stmt.isNull(_columnIndexOfStateJson)) {
            _tmpStateJson = null;
          } else {
            _tmpStateJson = _stmt.getText(_columnIndexOfStateJson);
          }
          final String _tmpChecksum;
          if (_stmt.isNull(_columnIndexOfChecksum)) {
            _tmpChecksum = null;
          } else {
            _tmpChecksum = _stmt.getText(_columnIndexOfChecksum);
          }
          final int _tmpSchemaVersion;
          _tmpSchemaVersion = (int) (_stmt.getLong(_columnIndexOfSchemaVersion));
          final String _tmpRngAlgorithm;
          if (_stmt.isNull(_columnIndexOfRngAlgorithm)) {
            _tmpRngAlgorithm = null;
          } else {
            _tmpRngAlgorithm = _stmt.getText(_columnIndexOfRngAlgorithm);
          }
          final String _tmpRngCountersJson;
          if (_stmt.isNull(_columnIndexOfRngCountersJson)) {
            _tmpRngCountersJson = null;
          } else {
            _tmpRngCountersJson = _stmt.getText(_columnIndexOfRngCountersJson);
          }
          final String _tmpLastEventId;
          if (_stmt.isNull(_columnIndexOfLastEventId)) {
            _tmpLastEventId = null;
          } else {
            _tmpLastEventId = _stmt.getText(_columnIndexOfLastEventId);
          }
          final long _tmpCreatedAtAudit;
          _tmpCreatedAtAudit = _stmt.getLong(_columnIndexOfCreatedAtAudit);
          _item = new CampaignSnapshotEntity(_tmpSnapshotId,_tmpCampaignId,_tmpRevision,_tmpStateJson,_tmpChecksum,_tmpSchemaVersion,_tmpRngAlgorithm,_tmpRngCountersJson,_tmpLastEventId,_tmpCreatedAtAudit);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int countEvents(final String campaignId) {
    final String _sql = "SELECT COUNT(*) FROM campaign_event WHERE campaignId = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        final int _result;
        if (_stmt.step()) {
          _result = (int) (_stmt.getLong(0));
        } else {
          _result = 0;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public CampaignSnapshotEntity latestSnapshot(final String campaignId) {
    final String _sql = "SELECT * FROM campaign_snapshot WHERE campaignId = ? ORDER BY revision DESC LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        final int _columnIndexOfSnapshotId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "snapshotId");
        final int _columnIndexOfCampaignId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "campaignId");
        final int _columnIndexOfRevision = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revision");
        final int _columnIndexOfStateJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "stateJson");
        final int _columnIndexOfChecksum = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "checksum");
        final int _columnIndexOfSchemaVersion = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "schemaVersion");
        final int _columnIndexOfRngAlgorithm = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rngAlgorithm");
        final int _columnIndexOfRngCountersJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "rngCountersJson");
        final int _columnIndexOfLastEventId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "lastEventId");
        final int _columnIndexOfCreatedAtAudit = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createdAtAudit");
        final CampaignSnapshotEntity _result;
        if (_stmt.step()) {
          final String _tmpSnapshotId;
          if (_stmt.isNull(_columnIndexOfSnapshotId)) {
            _tmpSnapshotId = null;
          } else {
            _tmpSnapshotId = _stmt.getText(_columnIndexOfSnapshotId);
          }
          final String _tmpCampaignId;
          if (_stmt.isNull(_columnIndexOfCampaignId)) {
            _tmpCampaignId = null;
          } else {
            _tmpCampaignId = _stmt.getText(_columnIndexOfCampaignId);
          }
          final long _tmpRevision;
          _tmpRevision = _stmt.getLong(_columnIndexOfRevision);
          final String _tmpStateJson;
          if (_stmt.isNull(_columnIndexOfStateJson)) {
            _tmpStateJson = null;
          } else {
            _tmpStateJson = _stmt.getText(_columnIndexOfStateJson);
          }
          final String _tmpChecksum;
          if (_stmt.isNull(_columnIndexOfChecksum)) {
            _tmpChecksum = null;
          } else {
            _tmpChecksum = _stmt.getText(_columnIndexOfChecksum);
          }
          final int _tmpSchemaVersion;
          _tmpSchemaVersion = (int) (_stmt.getLong(_columnIndexOfSchemaVersion));
          final String _tmpRngAlgorithm;
          if (_stmt.isNull(_columnIndexOfRngAlgorithm)) {
            _tmpRngAlgorithm = null;
          } else {
            _tmpRngAlgorithm = _stmt.getText(_columnIndexOfRngAlgorithm);
          }
          final String _tmpRngCountersJson;
          if (_stmt.isNull(_columnIndexOfRngCountersJson)) {
            _tmpRngCountersJson = null;
          } else {
            _tmpRngCountersJson = _stmt.getText(_columnIndexOfRngCountersJson);
          }
          final String _tmpLastEventId;
          if (_stmt.isNull(_columnIndexOfLastEventId)) {
            _tmpLastEventId = null;
          } else {
            _tmpLastEventId = _stmt.getText(_columnIndexOfLastEventId);
          }
          final long _tmpCreatedAtAudit;
          _tmpCreatedAtAudit = _stmt.getLong(_columnIndexOfCreatedAtAudit);
          _result = new CampaignSnapshotEntity(_tmpSnapshotId,_tmpCampaignId,_tmpRevision,_tmpStateJson,_tmpChecksum,_tmpSchemaVersion,_tmpRngAlgorithm,_tmpRngCountersJson,_tmpLastEventId,_tmpCreatedAtAudit);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public AppMetaEntity getAppMeta(final String metaKey) {
    final String _sql = "SELECT * FROM app_meta WHERE metaKey = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (metaKey == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, metaKey);
        }
        final int _columnIndexOfMetaKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "metaKey");
        final int _columnIndexOfMetaValue = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "metaValue");
        final AppMetaEntity _result;
        if (_stmt.step()) {
          final String _tmpMetaKey;
          if (_stmt.isNull(_columnIndexOfMetaKey)) {
            _tmpMetaKey = null;
          } else {
            _tmpMetaKey = _stmt.getText(_columnIndexOfMetaKey);
          }
          final String _tmpMetaValue;
          if (_stmt.isNull(_columnIndexOfMetaValue)) {
            _tmpMetaValue = null;
          } else {
            _tmpMetaValue = _stmt.getText(_columnIndexOfMetaValue);
          }
          _result = new AppMetaEntity(_tmpMetaKey,_tmpMetaValue);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void deleteEvents(final String campaignId) {
    final String _sql = "DELETE FROM campaign_event WHERE campaignId = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void deleteSnapshots(final String campaignId) {
    final String _sql = "DELETE FROM campaign_snapshot WHERE campaignId = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void deleteCampaignState(final String campaignId) {
    final String _sql = "DELETE FROM campaign_state WHERE campaignId = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void deleteCampaignMeta(final String campaignId) {
    final String _sql = "DELETE FROM campaign_meta WHERE campaignId = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (campaignId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, campaignId);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
