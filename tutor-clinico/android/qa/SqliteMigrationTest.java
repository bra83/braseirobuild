package br.com.tutorclinico;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Validates B01 & B02: Transactional SQLite migration from DB_VERSION 1 to 2,
 * anti-cascade triggers, composite foreign key isolation, and zero plaintext columns.
 */
public final class SqliteMigrationTest {
    public static void main(String[] args) throws Exception {
        System.out.println("--- SQLITE MIGRATION & INTEGRITY TEST (B01, B02, B03) ---");
        Class.forName("org.sqlite.JDBC");

        File dbFile = File.createTempFile("eco_migration_test_", ".db");
        try {
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            // 1. Initialize v1 schema (simulating ECO 0.17 / v1 database)
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA user_version = 1;");

                stmt.execute("CREATE TABLE pacientes (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "nome TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL, " +
                        "payload_enc BLOB NOT NULL" +
                        ");");

                stmt.execute("CREATE TABLE sessoes (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "case_id TEXT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, " +
                        "data TEXT NOT NULL, " +
                        "origem TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL, " +
                        "payload_enc BLOB NOT NULL" +
                        ");");

                stmt.execute("CREATE TABLE rascunhos (" +
                        "case_id TEXT PRIMARY KEY NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, " +
                        "session_id TEXT, " +
                        "updated_at INTEGER NOT NULL, " +
                        "draft_enc BLOB NOT NULL" +
                        ");");

                stmt.execute("CREATE TABLE diario_privado (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "case_id TEXT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, " +
                        "data TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "entry_enc BLOB NOT NULL" +
                        ");");

                // Seed with v1 fictitious records
                stmt.execute("INSERT INTO pacientes VALUES('CASE_A', 'Paciente Ficticio A', 1000, 1000, x'01020304');");
                stmt.execute("INSERT INTO sessoes VALUES('SESS_A1', 'CASE_A', '2026-01-01', 'audio', 1000, 1000, x'0506');");
                stmt.execute("INSERT INTO rascunhos VALUES('CASE_A', 'SESS_A1', 1000, x'0708');");
                stmt.execute("INSERT INTO diario_privado VALUES('DIARY_A1', 'CASE_A', '2026-01-01', 1000, x'090a');");
            }
            System.out.println("v1 Schema and Data initialized successfully.");

            // 2. Perform v2 12-step Migration
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF;");
                conn.setAutoCommit(false);

                // Step 1: Create _pacientes
                stmt.execute("CREATE TABLE IF NOT EXISTS _pacientes (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL, " +
                        "payload_enc BLOB NOT NULL" +
                        ");");

                // Step 2: Migrate data from old pacientes table
                stmt.execute("INSERT OR IGNORE INTO _pacientes (id, created_at, updated_at, payload_enc) " +
                        "SELECT id, created_at, updated_at, payload_enc FROM pacientes;");
                stmt.execute("DROP TABLE pacientes;");

                // Step 3: Create view and anti-cascade triggers
                stmt.execute("CREATE VIEW pacientes AS " +
                        "SELECT id, created_at, updated_at, payload_enc FROM _pacientes;");

                stmt.execute("CREATE TRIGGER trg_pacientes_insert " +
                        "INSTEAD OF INSERT ON pacientes " +
                        "BEGIN " +
                        "INSERT INTO _pacientes(id, created_at, updated_at, payload_enc) " +
                        "VALUES(NEW.id, NEW.created_at, NEW.updated_at, NEW.payload_enc) " +
                        "ON CONFLICT(id) DO UPDATE SET " +
                        "updated_at = excluded.updated_at, " +
                        "payload_enc = excluded.payload_enc; " +
                        "END;");

                stmt.execute("CREATE TRIGGER trg_pacientes_update " +
                        "INSTEAD OF UPDATE ON pacientes " +
                        "BEGIN " +
                        "UPDATE _pacientes SET " +
                        "updated_at = NEW.updated_at, " +
                        "payload_enc = NEW.payload_enc " +
                        "WHERE id = NEW.id; " +
                        "END;");

                stmt.execute("CREATE TRIGGER trg_pacientes_delete " +
                        "INSTEAD OF DELETE ON pacientes " +
                        "BEGIN " +
                        "DELETE FROM _pacientes WHERE id = OLD.id; " +
                        "END;");

                // Step 4: Recreate sessoes pointing to _pacientes with UNIQUE(id, case_id)
                stmt.execute("CREATE TABLE sessoes_v2 (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, " +
                        "data TEXT NOT NULL, " +
                        "origem TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL, " +
                        "payload_enc BLOB NOT NULL, " +
                        "UNIQUE(id, case_id)" +
                        ");");
                stmt.execute("INSERT OR IGNORE INTO sessoes_v2 SELECT id, case_id, data, origem, created_at, updated_at, payload_enc FROM sessoes;");
                stmt.execute("DROP TABLE sessoes;");
                stmt.execute("ALTER TABLE sessoes_v2 RENAME TO sessoes;");

                // Step 5: Recreate rascunhos pointing to _pacientes
                stmt.execute("CREATE TABLE rascunhos_v2 (" +
                        "case_id TEXT PRIMARY KEY NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, " +
                        "session_id TEXT, " +
                        "updated_at INTEGER NOT NULL, " +
                        "draft_enc BLOB NOT NULL" +
                        ");");
                stmt.execute("INSERT OR IGNORE INTO rascunhos_v2 SELECT case_id, session_id, updated_at, draft_enc FROM rascunhos;");
                stmt.execute("DROP TABLE rascunhos;");
                stmt.execute("ALTER TABLE rascunhos_v2 RENAME TO rascunhos;");

                // Step 6: Recreate diario_privado pointing to _pacientes
                stmt.execute("CREATE TABLE diario_privado_v2 (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, " +
                        "data TEXT NOT NULL, " +
                        "created_at INTEGER NOT NULL, " +
                        "entry_enc BLOB NOT NULL" +
                        ");");
                stmt.execute("INSERT OR IGNORE INTO diario_privado_v2 SELECT id, case_id, data, created_at, entry_enc FROM diario_privado;");
                stmt.execute("DROP TABLE diario_privado;");
                stmt.execute("ALTER TABLE diario_privado_v2 RENAME TO diario_privado;");

                // Step 7: Create processamento_jobs
                stmt.execute("CREATE TABLE IF NOT EXISTS processamento_jobs (" +
                        "job_id TEXT PRIMARY KEY NOT NULL, " +
                        "case_id TEXT NOT NULL, " +
                        "session_id TEXT NOT NULL, " +
                        "audio_path TEXT NOT NULL, " +
                        "audio_key_enc BLOB, " +
                        "state TEXT NOT NULL, " +
                        "total_segments INTEGER DEFAULT 0, " +
                        "committed_segments INTEGER DEFAULT 0, " +
                        "checkpoint_offset_ms INTEGER DEFAULT 0, " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL, " +
                        "error_message TEXT, " +
                        "UNIQUE(job_id, case_id, session_id), " +
                        "FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE" +
                        ");");

                // Step 8: Create segmentos with composite FKs
                stmt.execute("CREATE TABLE IF NOT EXISTS segmentos (" +
                        "id TEXT PRIMARY KEY NOT NULL, " +
                        "job_id TEXT NOT NULL, " +
                        "session_id TEXT NOT NULL, " +
                        "case_id TEXT NOT NULL, " +
                        "speaker TEXT NOT NULL, " +
                        "speaker_confirmed INTEGER DEFAULT 0, " +
                        "start_ms INTEGER NOT NULL, " +
                        "end_ms INTEGER NOT NULL, " +
                        "text_enc BLOB NOT NULL, " +
                        "checksum TEXT NOT NULL, " +
                        "status TEXT NOT NULL DEFAULT 'committed', " +
                        "created_at INTEGER NOT NULL, " +
                        "FOREIGN KEY (job_id, case_id, session_id) REFERENCES processamento_jobs(job_id, case_id, session_id) ON DELETE CASCADE, " +
                        "FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE" +
                        ");");

                // Step 9: Create configuracoes
                stmt.execute("CREATE TABLE IF NOT EXISTS configuracoes (" +
                        "chave TEXT PRIMARY KEY NOT NULL, " +
                        "valor_enc BLOB NOT NULL, " +
                        "updated_at INTEGER NOT NULL" +
                        ");");

                // Step 10: Create indexes
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessoes_case ON sessoes(case_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_segmentos_session ON segmentos(session_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_segmentos_case ON segmentos(case_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_segmentos_job_case_sess ON segmentos(job_id, case_id, session_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_diario_case ON diario_privado(case_id);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_jobs_case ON processamento_jobs(case_id);");

                // Step 11: PRAGMA foreign_key_check
                try (ResultSet fk = stmt.executeQuery("PRAGMA foreign_key_check;")) {
                    if (fk.next()) {
                        throw new IllegalStateException("Foreign key violations after migration!");
                    }
                }

                stmt.execute("PRAGMA user_version = 2;");
                conn.commit();
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            System.out.println("Migration to v2 completed with PRAGMA foreign_key_check verified.");

            // 3. Verification of Data Preservation and Constraints
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");

                // Check version
                try (ResultSet rs = stmt.executeQuery("PRAGMA user_version;")) {
                    rs.next();
                    int ver = rs.getInt(1);
                    if (ver != 2) throw new IllegalStateException("Expected DB_VERSION 2, got: " + ver);
                }

                // Check rows in _pacientes
                try (ResultSet rs = stmt.executeQuery("SELECT id FROM _pacientes WHERE id = 'CASE_A';")) {
                    if (!rs.next()) throw new IllegalStateException("CASE_A missing from _pacientes!");
                }

                // Check rows in sessoes
                try (ResultSet rs = stmt.executeQuery("SELECT id FROM sessoes WHERE id = 'SESS_A1' AND case_id = 'CASE_A';")) {
                    if (!rs.next()) throw new IllegalStateException("SESS_A1 missing from sessoes!");
                }

                // Check rows in rascunhos
                try (ResultSet rs = stmt.executeQuery("SELECT case_id FROM rascunhos WHERE case_id = 'CASE_A';")) {
                    if (!rs.next()) throw new IllegalStateException("CASE_A missing from rascunhos!");
                }

                // Check rows in diario_privado
                try (ResultSet rs = stmt.executeQuery("SELECT id FROM diario_privado WHERE id = 'DIARY_A1';")) {
                    if (!rs.next()) throw new IllegalStateException("DIARY_A1 missing from diario_privado!");
                }

                // Check configuracoes table exists
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM configuracoes;")) {
                    rs.next();
                }

                // Check that _pacientes has NO 'nome' column
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(_pacientes);")) {
                    while (rs.next()) {
                        String col = rs.getString("name");
                        if ("nome".equalsIgnoreCase(col)) {
                            throw new IllegalStateException("Plaintext 'nome' column found in _pacientes!");
                        }
                    }
                }
                System.out.println("Zero plaintext column in _pacientes verified ✅");

                // Check Anti-Cascade on Replace: replace CASE_A
                stmt.execute("INSERT OR REPLACE INTO pacientes(id, created_at, updated_at, payload_enc) " +
                        "VALUES('CASE_A', 1000, 2000, x'9999');");
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sessoes WHERE case_id = 'CASE_A';")) {
                    rs.next();
                    if (rs.getInt(1) != 1) throw new IllegalStateException("Cascade wipeout of sessoes occurred!");
                }
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM rascunhos WHERE case_id = 'CASE_A';")) {
                    rs.next();
                    if (rs.getInt(1) != 1) throw new IllegalStateException("Cascade wipeout of rascunhos occurred!");
                }
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM diario_privado WHERE case_id = 'CASE_A';")) {
                    rs.next();
                    if (rs.getInt(1) != 1) throw new IllegalStateException("Cascade wipeout of diario_privado occurred!");
                }
                System.out.println("Anti-cascade protection verified: 0 child entities lost on replace ✅");

                // Check Cross-Case Segment Injection Rejection
                stmt.execute("INSERT INTO pacientes(id, created_at, updated_at, payload_enc) VALUES('CASE_B', 1, 1, x'01');");
                stmt.execute("INSERT INTO sessoes(id, case_id, data, origem, created_at, updated_at, payload_enc) VALUES('SESS_B1', 'CASE_B', '2026-01-01', 'audio', 1, 1, x'01');");
                stmt.execute("INSERT INTO processamento_jobs(job_id, case_id, session_id, audio_path, state, created_at, updated_at) " +
                        "VALUES('JOB_B', 'CASE_B', 'SESS_B1', '/tmp/b.wav', 'PROCESSING', 1, 1);");

                boolean crossCaseRejected = false;
                try {
                    // Try to insert segment for CASE_A using SESS_B1
                    stmt.execute("INSERT INTO segmentos(id, job_id, session_id, case_id, speaker, start_ms, end_ms, text_enc, checksum, created_at) " +
                            "VALUES('SEG_X', 'JOB_B', 'SESS_B1', 'CASE_A', 'paciente', 0, 1000, x'01', 'chk', 1);");
                } catch (Exception e) {
                    crossCaseRejected = true;
                }
                if (!crossCaseRejected) throw new IllegalStateException("Cross-case segment injection was accepted!");
                System.out.println("Cross-case referential isolation verified: FK rejected mismatch ✅");

                // Check Cross-Job Segment Injection Rejection
                stmt.execute("INSERT INTO processamento_jobs(job_id, case_id, session_id, audio_path, state, created_at, updated_at) " +
                        "VALUES('JOB_A', 'CASE_A', 'SESS_A1', '/tmp/a.wav', 'PROCESSING', 1, 1);");
                boolean crossJobRejected = false;
                try {
                    // Try to insert segment for SESS_A1/CASE_A using JOB_B
                    stmt.execute("INSERT INTO segmentos(id, job_id, session_id, case_id, speaker, start_ms, end_ms, text_enc, checksum, created_at) " +
                            "VALUES('SEG_Y', 'JOB_B', 'SESS_A1', 'CASE_A', 'paciente', 0, 1000, x'01', 'chk', 1);");
                } catch (Exception e) {
                    crossJobRejected = true;
                }
                if (!crossJobRejected) throw new IllegalStateException("Cross-job segment injection was accepted!");
                System.out.println("Cross-job referential isolation verified: FK rejected mismatch ✅");
            }

            System.out.println("GATE B01 & B02 APROVADOS COM SUCESSO: Migração transacional e isolamento relacional 100%!");
        } finally {
            dbFile.delete();
        }
    }
}
