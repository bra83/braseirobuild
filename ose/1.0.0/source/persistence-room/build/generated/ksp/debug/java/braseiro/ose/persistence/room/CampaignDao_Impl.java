package braseiro.ose.persistence.room;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CampaignDao_Impl implements CampaignDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CampaignEntity> __insertionAdapterOfCampaignEntity;

  private final EntityInsertionAdapter<ActionAuditEntity> __insertionAdapterOfActionAuditEntity;

  private final EntityInsertionAdapter<CheckpointEntity> __insertionAdapterOfCheckpointEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateState;

  private final SharedSQLiteStatement __preparedStmtOfArchive;

  public CampaignDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCampaignEntity = new EntityInsertionAdapter<CampaignEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `campaigns` (`campaignId`,`envelopeJson`,`archived`,`commitSequence`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CampaignEntity entity) {
        statement.bindString(1, entity.getCampaignId());
        statement.bindString(2, entity.getEnvelopeJson());
        final int _tmp = entity.getArchived() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindLong(4, entity.getCommitSequence());
      }
    };
    this.__insertionAdapterOfActionAuditEntity = new EntityInsertionAdapter<ActionAuditEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `action_audit` (`id`,`campaignId`,`transitionId`,`sequence`,`postStateHash`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ActionAuditEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCampaignId());
        statement.bindString(3, entity.getTransitionId());
        statement.bindLong(4, entity.getSequence());
        statement.bindString(5, entity.getPostStateHash());
      }
    };
    this.__insertionAdapterOfCheckpointEntity = new EntityInsertionAdapter<CheckpointEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `checkpoints` (`checkpointId`,`campaignId`,`commitSequence`,`schemaVersion`,`profile`,`authorityRevision`,`stateHash`,`envelopeJson`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CheckpointEntity entity) {
        statement.bindString(1, entity.getCheckpointId());
        statement.bindString(2, entity.getCampaignId());
        statement.bindLong(3, entity.getCommitSequence());
        statement.bindLong(4, entity.getSchemaVersion());
        statement.bindString(5, entity.getProfile());
        statement.bindString(6, entity.getAuthorityRevision());
        statement.bindString(7, entity.getStateHash());
        statement.bindString(8, entity.getEnvelopeJson());
      }
    };
    this.__preparedStmtOfUpdateState = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE campaigns SET envelopeJson = ?, commitSequence = ? WHERE campaignId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfArchive = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE campaigns SET archived = 1 WHERE campaignId = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertCampaign(final CampaignEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCampaignEntity.insert(entity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertAudit(final ActionAuditEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfActionAuditEntity.insert(entity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertCheckpoint(final CheckpointEntity entity) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCheckpointEntity.insert(entity);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public int updateState(final String campaignId, final String json, final long sequence) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateState.acquire();
    int _argIndex = 1;
    _stmt.bindString(_argIndex, json);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, sequence);
    _argIndex = 3;
    _stmt.bindString(_argIndex, campaignId);
    try {
      __db.beginTransaction();
      try {
        final int _result = _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
        return _result;
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateState.release(_stmt);
    }
  }

  @Override
  public int archive(final String campaignId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfArchive.acquire();
    int _argIndex = 1;
    _stmt.bindString(_argIndex, campaignId);
    try {
      __db.beginTransaction();
      try {
        final int _result = _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
        return _result;
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfArchive.release(_stmt);
    }
  }

  @Override
  public CampaignEntity findCampaign(final String campaignId) {
    final String _sql = "SELECT * FROM campaigns WHERE campaignId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, campaignId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfCampaignId = CursorUtil.getColumnIndexOrThrow(_cursor, "campaignId");
      final int _cursorIndexOfEnvelopeJson = CursorUtil.getColumnIndexOrThrow(_cursor, "envelopeJson");
      final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
      final int _cursorIndexOfCommitSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "commitSequence");
      final CampaignEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpCampaignId;
        _tmpCampaignId = _cursor.getString(_cursorIndexOfCampaignId);
        final String _tmpEnvelopeJson;
        _tmpEnvelopeJson = _cursor.getString(_cursorIndexOfEnvelopeJson);
        final boolean _tmpArchived;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfArchived);
        _tmpArchived = _tmp != 0;
        final long _tmpCommitSequence;
        _tmpCommitSequence = _cursor.getLong(_cursorIndexOfCommitSequence);
        _result = new CampaignEntity(_tmpCampaignId,_tmpEnvelopeJson,_tmpArchived,_tmpCommitSequence);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<CampaignEntity> listCampaigns() {
    final String _sql = "SELECT * FROM campaigns ORDER BY campaignId";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfCampaignId = CursorUtil.getColumnIndexOrThrow(_cursor, "campaignId");
      final int _cursorIndexOfEnvelopeJson = CursorUtil.getColumnIndexOrThrow(_cursor, "envelopeJson");
      final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
      final int _cursorIndexOfCommitSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "commitSequence");
      final List<CampaignEntity> _result = new ArrayList<CampaignEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final CampaignEntity _item;
        final String _tmpCampaignId;
        _tmpCampaignId = _cursor.getString(_cursorIndexOfCampaignId);
        final String _tmpEnvelopeJson;
        _tmpEnvelopeJson = _cursor.getString(_cursorIndexOfEnvelopeJson);
        final boolean _tmpArchived;
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfArchived);
        _tmpArchived = _tmp != 0;
        final long _tmpCommitSequence;
        _tmpCommitSequence = _cursor.getLong(_cursorIndexOfCommitSequence);
        _item = new CampaignEntity(_tmpCampaignId,_tmpEnvelopeJson,_tmpArchived,_tmpCommitSequence);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int auditCount(final String campaignId) {
    final String _sql = "SELECT COUNT(*) FROM action_audit WHERE campaignId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, campaignId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public ActionAuditEntity findTransitionAudit(final String campaignId, final String transitionId) {
    final String _sql = "SELECT * FROM action_audit WHERE campaignId = ? AND transitionId = ? ORDER BY sequence DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, campaignId);
    _argIndex = 2;
    _statement.bindString(_argIndex, transitionId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfCampaignId = CursorUtil.getColumnIndexOrThrow(_cursor, "campaignId");
      final int _cursorIndexOfTransitionId = CursorUtil.getColumnIndexOrThrow(_cursor, "transitionId");
      final int _cursorIndexOfSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "sequence");
      final int _cursorIndexOfPostStateHash = CursorUtil.getColumnIndexOrThrow(_cursor, "postStateHash");
      final ActionAuditEntity _result;
      if (_cursor.moveToFirst()) {
        final long _tmpId;
        _tmpId = _cursor.getLong(_cursorIndexOfId);
        final String _tmpCampaignId;
        _tmpCampaignId = _cursor.getString(_cursorIndexOfCampaignId);
        final String _tmpTransitionId;
        _tmpTransitionId = _cursor.getString(_cursorIndexOfTransitionId);
        final long _tmpSequence;
        _tmpSequence = _cursor.getLong(_cursorIndexOfSequence);
        final String _tmpPostStateHash;
        _tmpPostStateHash = _cursor.getString(_cursorIndexOfPostStateHash);
        _result = new ActionAuditEntity(_tmpId,_tmpCampaignId,_tmpTransitionId,_tmpSequence,_tmpPostStateHash);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public CheckpointEntity findCheckpoint(final String campaignId, final String checkpointId) {
    final String _sql = "SELECT * FROM checkpoints WHERE checkpointId = ? AND campaignId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, checkpointId);
    _argIndex = 2;
    _statement.bindString(_argIndex, campaignId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfCheckpointId = CursorUtil.getColumnIndexOrThrow(_cursor, "checkpointId");
      final int _cursorIndexOfCampaignId = CursorUtil.getColumnIndexOrThrow(_cursor, "campaignId");
      final int _cursorIndexOfCommitSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "commitSequence");
      final int _cursorIndexOfSchemaVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "schemaVersion");
      final int _cursorIndexOfProfile = CursorUtil.getColumnIndexOrThrow(_cursor, "profile");
      final int _cursorIndexOfAuthorityRevision = CursorUtil.getColumnIndexOrThrow(_cursor, "authorityRevision");
      final int _cursorIndexOfStateHash = CursorUtil.getColumnIndexOrThrow(_cursor, "stateHash");
      final int _cursorIndexOfEnvelopeJson = CursorUtil.getColumnIndexOrThrow(_cursor, "envelopeJson");
      final CheckpointEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpCheckpointId;
        _tmpCheckpointId = _cursor.getString(_cursorIndexOfCheckpointId);
        final String _tmpCampaignId;
        _tmpCampaignId = _cursor.getString(_cursorIndexOfCampaignId);
        final long _tmpCommitSequence;
        _tmpCommitSequence = _cursor.getLong(_cursorIndexOfCommitSequence);
        final int _tmpSchemaVersion;
        _tmpSchemaVersion = _cursor.getInt(_cursorIndexOfSchemaVersion);
        final String _tmpProfile;
        _tmpProfile = _cursor.getString(_cursorIndexOfProfile);
        final String _tmpAuthorityRevision;
        _tmpAuthorityRevision = _cursor.getString(_cursorIndexOfAuthorityRevision);
        final String _tmpStateHash;
        _tmpStateHash = _cursor.getString(_cursorIndexOfStateHash);
        final String _tmpEnvelopeJson;
        _tmpEnvelopeJson = _cursor.getString(_cursorIndexOfEnvelopeJson);
        _result = new CheckpointEntity(_tmpCheckpointId,_tmpCampaignId,_tmpCommitSequence,_tmpSchemaVersion,_tmpProfile,_tmpAuthorityRevision,_tmpStateHash,_tmpEnvelopeJson);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
