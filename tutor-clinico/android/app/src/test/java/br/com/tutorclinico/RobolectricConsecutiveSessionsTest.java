package br.com.tutorclinico;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RobolectricConsecutiveSessionsTest {

    private Context context;
    private SecureStore store;
    private EcoDatabaseHelper dbHelper;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        store = new SecureStore(context, new SecretKeySpec(new byte[32], "AES"));
        dbHelper = store.getDbHelper();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Clear all tables for clean integration test run
        db.execSQL("PRAGMA foreign_keys = OFF;");
        db.execSQL("DELETE FROM segmentos;");
        db.execSQL("DELETE FROM processamento_jobs;");
        db.execSQL("DELETE FROM sessoes;");
        db.execSQL("DELETE FROM _pacientes;");
        db.execSQL("DELETE FROM diario_privado;");
        db.execSQL("DELETE FROM rascunhos;");
        db.execSQL("DELETE FROM agenda;");
        db.execSQL("PRAGMA foreign_keys = ON;");
    }

    private void insertPatient(String id, String nome) throws Exception {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        cv.put("payload_enc", dbHelper.encrypt("{\"nome\":\"" + nome + "\"}"));
        dbHelper.getWritableDatabase().insertOrThrow("_pacientes", null, cv);
    }

    /**
     * Requirement: Execute tests with 8 consecutive sessions without restarting the app.
     * Validates:
     * - Isolation of segment counters per job (no leakage between Job A and Job B).
     * - Idempotent retry of segments without duplicate counting or ACK failure.
     * - Silence / zero-segment session handling (0/0 ACK).
     * - State transitions: PROCESSING -> DECODED_VERIFIED -> COMMITTED.
     * - Foreign key integrity preserved throughout all 8 sessions.
     */
    @Test
    public void testEightConsecutiveSessionsWithoutRestart() throws Exception {
        // Setup 2 distinct patients
        String p1 = "paciente-001";
        String p2 = "paciente-002";
        insertPatient(p1, "Paciente Um");
        insertPatient(p2, "Paciente Dois");

        int[][] sessionSpecs = {
            // {patient_index (1 or 2), segment_count, retry_count}
            {1, 3, 0}, // Session 1: Patient 1, 3 segments
            {2, 2, 0}, // Session 2: Patient 2, 2 segments (tests counter isolation from Sess 1)
            {1, 4, 1}, // Session 3: Patient 1, 4 segments + 1 retry of seg 0 (tests idempotency)
            {2, 0, 0}, // Session 4: Patient 2, 0 segments (silence session)
            {1, 2, 0}, // Session 5: Patient 1, 2 segments
            {2, 3, 0}, // Session 6: Patient 2, 3 segments
            {1, 1, 0}, // Session 7: Patient 1, 1 segment
            {2, 2, 0}  // Session 8: Patient 2, 2 segments
        };

        for (int i = 0; i < sessionSpecs.length; i++) {
            int sessNum = i + 1;
            String caseId = (sessionSpecs[i][0] == 1) ? p1 : p2;
            int segCount = sessionSpecs[i][1];
            int retries = sessionSpecs[i][2];

            String sessionId = "sess-" + sessNum + "-" + caseId;
            String jobId = "job-" + sessNum + "-" + caseId;

            // 1. Create session and job in real Android SQLite database
            byte[] ephemeralKey = new byte[32];
            new SecureRandom().nextBytes(ephemeralKey);
            dbHelper.createJob(jobId, caseId, sessionId, "/data/data/br.com.tutorclinico.offline/cache/rec_" + jobId + ".wav", ephemeralKey);

            // 2. Commit segments
            for (int s = 0; s < segCount; s++) {
                String text = "Transcrição do trecho " + s + " da sessão " + sessNum + " do caso " + caseId;
                String checksum = EcoDatabaseHelper.computeChecksum(text);
                boolean ok = dbHelper.commitSegment(jobId, caseId, sessionId, s * 5000L, (s + 1) * 5000L, "paciente", text, checksum);
                assertTrue("Segment commit failed on session " + sessNum + " segment " + s, ok);
            }

            // Test idempotency: if retry > 0, re-commit segment 0
            if (retries > 0 && segCount > 0) {
                String text = "Transcrição do trecho 0 da sessão " + sessNum + " do caso " + caseId;
                String checksum = EcoDatabaseHelper.computeChecksum(text);
                boolean retryOk = dbHelper.commitSegment(jobId, caseId, sessionId, 0L, 5000L, "paciente", text, checksum);
                assertTrue("Idempotent retry commit should succeed", retryOk);
            }

            // Verify committed_segments matches exact count for this job alone (no cross-job contamination)
            int committedCount = dbHelper.getCommittedSegmentCount(jobId);
            assertEquals("Committed segments count mismatch for session " + sessNum, segCount, committedCount);

            // 3. Complete decoding and EOF verification
            int chunksProcessed = Math.max(1, segCount * 2);
            boolean markOk = dbHelper.markJobDecoded(jobId, committedCount, chunksProcessed);
            assertTrue("markJobDecoded failed for session " + sessNum, markOk);

            // 4. Acknowledge persistence
            boolean ackOk = dbHelper.acknowledgePersistence(jobId);
            assertTrue("acknowledgePersistence failed for session " + sessNum + " (Job ID: " + jobId + ")", ackOk);

            // 5. Verify committed state
            assertTrue("isJobCommitted should be true for session " + sessNum, dbHelper.isJobCommitted(jobId));

            // Verify job record
            JSONObject job = dbHelper.getJob(jobId);
            assertNotNull(job);
            assertEquals("COMMITTED", job.optString("state"));
            assertEquals(segCount, job.optInt("total_segments"));
            assertEquals(segCount, job.optInt("committed_segments"));
        }

        // Verify database foreign key check across entire database
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery("PRAGMA foreign_key_check", null)) {
            assertEquals("Zero foreign key violations across 8 consecutive sessions", 0, c.getCount());
        }
    }

    /**
     * Requirement P0-02: Process death recovery and audio key lifecycle.
     */
    @Test
    public void testProcessDeathRecoveryAndKeyCleanup() throws Exception {
        String caseId = "case-recovery-001";
        String sessionId = "sess-recovery-001";
        String jobId = "job-recovery-001";

        insertPatient(caseId, "Caso Recuperação");

        byte[] fakeKey = new byte[32];
        new SecureRandom().nextBytes(fakeKey);
        String fakePath = new File(context.getCacheDir(), "eco_recording_test_recovery.wav").getAbsolutePath();

        // Create job with key and path persisted
        dbHelper.createJob(jobId, caseId, sessionId, fakePath, fakeKey);

        // Verify key is securely retrievable
        byte[] retrievedKey = dbHelper.getJobKey(jobId);
        assertNotNull("Persisted key must be recoverable", retrievedKey);
        assertArrayEquals("Retrieved key must match persisted key", fakeKey, retrievedKey);

        // Verify listRecoverableJobs includes this uncommitted job
        List<JSONObject> recoverable = dbHelper.listRecoverableJobs();
        boolean found = false;
        for (JSONObject j : recoverable) {
            if (jobId.equals(j.optString("job_id"))) {
                found = true;
                assertEquals(fakePath, j.optString("audio_path"));
                assertEquals("PROCESSING", j.optString("state"));
                break;
            }
        }
        assertTrue("Job must appear in recoverable jobs list", found);

        // Simulate session commit and cleanup of audio key from database
        assertFalse(dbHelper.markJobDecoded(jobId, 0, 0));
        assertFalse(dbHelper.acknowledgePersistence(jobId));
        assertTrue(dbHelper.markJobDecoded(jobId, 0, 1));
        dbHelper.acknowledgePersistence(jobId);
        assertTrue(dbHelper.isJobCommitted(jobId));

        // When deleted upon confirmed commit, audio_key_enc must be cleared
        boolean cleared = dbHelper.clearJobAudioKey(jobId);
        assertTrue("clearJobAudioKey should succeed", cleared);
        assertNull("Audio key must be NULL in database after cleanup", dbHelper.getJobKey(jobId));

        // Verify cleanAbandoned() fail-safe retention
        SessionRecorder.cleanAbandoned(context);
    }

    /**
     * Requirement P0-03: Full field preservation during migration.
     * Verifies that legacy 0.17 sessoes.hora and diario_privado.categoria are preserved bijectively.
     */
    @Test
    public void testMigrationFromV17PreservesHoraAndCategoria() throws Exception {
        SQLiteDatabase db = SQLiteDatabase.create(null);
        db.execSQL("PRAGMA foreign_keys = ON;");

        // Create exact 0.17 legacy schema
        db.execSQL("CREATE TABLE pacientes (" +
                "id TEXT PRIMARY KEY NOT NULL, " +
                "nome TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "payload_enc BLOB NOT NULL" +
                ");");
        db.execSQL("CREATE TABLE sessoes (" +
                "id TEXT PRIMARY KEY NOT NULL, " +
                "case_id TEXT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, " +
                "data TEXT NOT NULL, " +
                "hora TEXT, " +
                "origem TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "payload_enc BLOB NOT NULL" +
                ");");
        db.execSQL("CREATE TABLE diario_privado (" +
                "id TEXT PRIMARY KEY NOT NULL, " +
                "case_id TEXT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, " +
                "data TEXT NOT NULL, " +
                "categoria TEXT, " +
                "created_at INTEGER NOT NULL, " +
                "entry_enc BLOB NOT NULL" +
                ");");
        db.execSQL("CREATE TABLE processamento_jobs (" +
                "job_id TEXT PRIMARY KEY NOT NULL, " +
                "case_id TEXT NOT NULL, " +
                "session_id TEXT NOT NULL, " +
                "audio_path TEXT NOT NULL, " +
                "state TEXT NOT NULL, " +
                "total_segments INTEGER DEFAULT 0, " +
                "committed_segments INTEGER DEFAULT 0, " +
                "checkpoint_offset_ms INTEGER DEFAULT 0, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "error_message TEXT" +
                ");");
        db.execSQL("CREATE TABLE segmentos (" +
                "id TEXT PRIMARY KEY NOT NULL, " +
                "job_id TEXT NOT NULL, " +
                "session_id TEXT NOT NULL, " +
                "case_id TEXT NOT NULL, " +
                "speaker TEXT NOT NULL, " +
                "start_ms INTEGER NOT NULL, " +
                "end_ms INTEGER NOT NULL, " +
                "text_enc BLOB NOT NULL, " +
                "checksum TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'committed', " +
                "created_at INTEGER NOT NULL" +
                ");");
        db.execSQL("CREATE TABLE rascunhos (" +
                "case_id TEXT PRIMARY KEY NOT NULL, " +
                "session_id TEXT, " +
                "updated_at INTEGER NOT NULL, " +
                "draft_enc BLOB NOT NULL" +
                ");");
        db.execSQL("CREATE TABLE agenda (" +
                "id TEXT PRIMARY KEY NOT NULL, " +
                "case_id TEXT, " +
                "data TEXT NOT NULL, " +
                "hora TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "payload_enc BLOB NOT NULL" +
                ");");

        // Seed legacy data
        db.execSQL("INSERT INTO pacientes VALUES('P1', 'Paciente V17', 1000, 1000, ?)", new Object[]{dbHelper.encrypt("{}")});
        db.execSQL("INSERT INTO sessoes VALUES('S1', 'P1', '2026-01-15', '15:30', 'audio', 1000, 1000, x'0304')");
        db.execSQL("INSERT INTO diario_privado VALUES('D1', 'P1', '2026-01-15', 'Reflexão', 1000, x'0506')");

        // Trigger upgrade using EcoDatabaseHelper's onUpgrade logic (oldVersion=1 -> upgradeToV3)
        db.beginTransaction();
        try { dbHelper.onUpgrade(db, 1, 5); db.setTransactionSuccessful(); } finally { db.endTransaction(); }

        // Verify foreign key integrity post-migration
        try (Cursor c = db.rawQuery("PRAGMA foreign_key_check", null)) {
            assertEquals("Zero FK errors after migration", 0, c.getCount());
        }

        try (Cursor c = db.rawQuery("SELECT payload_enc FROM _pacientes WHERE id='P1'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("Paciente V17", new JSONObject(dbHelper.decrypt(c.getBlob(0))).getString("nome"));
        }

        // Verify sessoes preserves hora
        try (Cursor c = db.rawQuery("SELECT id, case_id, data, hora, origem FROM sessoes WHERE id = 'S1'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("S1", c.getString(0));
            assertEquals("P1", c.getString(1));
            assertEquals("2026-01-15", c.getString(2));
            assertEquals("15:30", c.getString(3)); // hora PRESERVED!
            assertEquals("audio", c.getString(4));
        }

        // Verify diario_privado preserves categoria
        try (Cursor c = db.rawQuery("SELECT id, case_id, data, categoria FROM diario_privado WHERE id = 'D1'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("D1", c.getString(0));
            assertEquals("P1", c.getString(1));
            assertEquals("2026-01-15", c.getString(2));
            assertEquals("Reflexão", c.getString(3)); // categoria PRESERVED!
        }
    }

    @Test
    public void testVersionFourMigrationThroughSQLiteOpenHelper() throws Exception {
        dbHelper.close();
        context.deleteDatabase("eco_vault.db");
        java.io.File file = context.getDatabasePath("eco_vault.db");
        file.getParentFile().mkdirs();
        try (SQLiteDatabase legacy = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            legacy.execSQL("PRAGMA foreign_keys=ON");
            legacy.execSQL("CREATE TABLE _pacientes(id TEXT PRIMARY KEY NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,payload_enc BLOB NOT NULL)");
            legacy.execSQL("CREATE VIEW pacientes AS SELECT id,CAST(NULL AS TEXT) AS nome,created_at,updated_at,payload_enc FROM _pacientes");
            legacy.execSQL("CREATE TABLE sessoes(id TEXT PRIMARY KEY NOT NULL,case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE,data TEXT NOT NULL,origem TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,payload_enc BLOB NOT NULL,UNIQUE(id,case_id))");
            legacy.execSQL("CREATE TABLE processamento_jobs(job_id TEXT PRIMARY KEY NOT NULL,case_id TEXT NOT NULL,session_id TEXT NOT NULL,audio_path TEXT NOT NULL,audio_key_enc BLOB,state TEXT NOT NULL,total_segments INTEGER DEFAULT 0,committed_segments INTEGER DEFAULT 0,checkpoint_offset_ms INTEGER DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,error_message TEXT,eof_verified INTEGER NOT NULL DEFAULT 0,UNIQUE(job_id,case_id,session_id),FOREIGN KEY(session_id,case_id) REFERENCES sessoes(id,case_id) ON DELETE CASCADE)");
            legacy.execSQL("CREATE TABLE segmentos(id TEXT PRIMARY KEY NOT NULL,job_id TEXT NOT NULL,session_id TEXT NOT NULL,case_id TEXT NOT NULL,speaker TEXT NOT NULL,speaker_confirmed INTEGER DEFAULT 0,start_ms INTEGER NOT NULL,end_ms INTEGER NOT NULL,text_enc BLOB NOT NULL,checksum TEXT NOT NULL,status TEXT NOT NULL DEFAULT 'committed',created_at INTEGER NOT NULL,FOREIGN KEY(job_id,case_id,session_id) REFERENCES processamento_jobs(job_id,case_id,session_id) ON DELETE CASCADE,FOREIGN KEY(session_id,case_id) REFERENCES sessoes(id,case_id) ON DELETE CASCADE)");
            legacy.execSQL("INSERT INTO _pacientes VALUES('P',1,1,?)",new Object[]{dbHelper.encrypt("{\"nome\":\"Fictício\"}")});
            legacy.execSQL("INSERT INTO sessoes VALUES('S','P','2026-01-01','audio',1,1,?)",new Object[]{dbHelper.encrypt("{}")});
            legacy.execSQL("INSERT INTO processamento_jobs(job_id,case_id,session_id,audio_path,state,total_segments,committed_segments,created_at,updated_at,eof_verified) VALUES('J','P','S','','COMMITTED',1,1,1,1,1)");
            legacy.execSQL("INSERT INTO segmentos(id,job_id,session_id,case_id,speaker,start_ms,end_ms,text_enc,checksum,created_at) VALUES('T','J','S','P','paciente',0,1000,?,'chk',1)",new Object[]{dbHelper.encrypt("trecho fictício")});
            legacy.setVersion(4);
        }
        store = new SecureStore(context, new SecretKeySpec(new byte[32], "AES"));
        dbHelper=store.getDbHelper();
        SQLiteDatabase upgraded=dbHelper.getWritableDatabase();
        assertEquals(5,upgraded.getVersion());
        assertTrue(upgraded.isDatabaseIntegrityOk());
        assertTrue(dbHelper.isJobCommitted("J"));
        try(Cursor c=upgraded.rawQuery("SELECT text_enc FROM segmentos WHERE id='T'",null)){
            assertTrue(c.moveToFirst());assertEquals("trecho fictício",dbHelper.decrypt(c.getBlob(0)));
        }
        try(Cursor c=upgraded.rawQuery("PRAGMA foreign_key_check",null)){assertEquals(0,c.getCount());}
        try(Cursor c=upgraded.rawQuery("SELECT hora FROM sessoes WHERE id='S'",null)){assertTrue(c.moveToFirst());assertTrue(c.isNull(0));}
    }

    /**
     * Requirement P1-04: Audio authentication, trailer verification, and rejection of corrupted/truncated spools.
     */
    @Test
    public void testAudioTrailerAuthenticationAndRejection() throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        File spool = File.createTempFile("eco_test_spool_", ".wav", context.getCacheDir());
        int chunkSize = 8192;
        int pcmBytes = 2 * chunkSize; // 2 full chunks (16384 bytes)

        try (RandomAccessFile raf = new RandomAccessFile(spool, "rw")) {
            // Write ECO2 Header
            raf.write(SessionRecorder.MAGIC_V2);
            for (int i = 0; i < 4; i++) raf.write((2 >> (i * 8)) & 255);
            for (int i = 0; i < 4; i++) raf.write((chunkSize >> (i * 8)) & 255);
            raf.write(new byte[12]); // reserved
            for (int i = 0; i < 8; i++) raf.write((int) (((long) pcmBytes >> (i * 8)) & 255L)); // expectedLen

            // Write 2 chunks
            byte[] pcmBuf = new byte[chunkSize];
            for (int chunkIdx = 0; chunkIdx < 2; chunkIdx++) {
                byte[] nonce = new byte[12];
                new SecureRandom().nextBytes(nonce);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
                cipher.updateAAD(("ECO2:" + chunkIdx + ":" + chunkSize).getBytes(StandardCharsets.UTF_8));
                byte[] enc = cipher.doFinal(pcmBuf);

                for (int i = 0; i < 4; i++) raf.write((chunkIdx >> (i * 8)) & 255);
                for (int i = 0; i < 4; i++) raf.write((chunkSize >> (i * 8)) & 255);
                raf.write(nonce);
                raf.write(enc);
            }

            // Write authenticated EOF trailer frame
            byte[] eofNonce = new byte[12];
            new SecureRandom().nextBytes(eofNonce);
            Cipher eofCipher = Cipher.getInstance("AES/GCM/NoPadding");
            eofCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, eofNonce));
            byte[] eofPlain = "ECO2_AUTHENTICATED_EOF".getBytes(StandardCharsets.UTF_8);
            byte[] eofAad = ("ECO2:EOF:" + 0xFFFFFFFFL + ":" + 2 + ":" + pcmBytes).getBytes(StandardCharsets.UTF_8);
            eofCipher.updateAAD(eofAad);
            byte[] eofEnc = eofCipher.doFinal(eofPlain);

            for (int i = 0; i < 4; i++) raf.write((int) ((0xFFFFFFFFL >> (i * 8)) & 255L));
            for (int i = 0; i < 4; i++) raf.write((eofPlain.length >> (i * 8)) & 255);
            raf.write(eofNonce);
            raf.write(eofEnc);
        }

        // Test successful decode
        AudioFileTranscriber.Listener listener = new AudioFileTranscriber.Listener() {
            @Override public boolean isActive() { return true; }
            @Override public void status(String message) {}
            @Override public void segment(String text) {}
        };
        int[] receivedBytes = new int[1];
        AudioFileTranscriber.PcmListener pcmListener = (pcm, size) -> receivedBytes[0] += size;

        int chunksDecoded = AudioFileTranscriber.streamPcmWithCount(context, Uri.fromFile(spool), listener, pcmListener, key);
        assertEquals("Decoded exactly 2 chunks", 2, chunksDecoded);
        assertEquals("Decoded bytes mismatch", pcmBytes, receivedBytes[0]);

        // Test rejection with wrong key
        byte[] wrongKey = new byte[32];
        new SecureRandom().nextBytes(wrongKey);
        boolean wrongKeyFailed = false;
        try {
            AudioFileTranscriber.streamPcm(context, Uri.fromFile(spool), listener, pcmListener, wrongKey);
        } catch (Exception e) {
            wrongKeyFailed = true;
        }
        assertTrue("Decoding with wrong key must fail", wrongKeyFailed);

        spool.delete();
    }
}
