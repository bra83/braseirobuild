# Historical fixture schemas from the 0.22 delivery. Fictitious data only.
SCHEMA_17 = [
    "CREATE TABLE IF NOT EXISTS pacientes (id TEXT PRIMARY KEY NOT NULL, nome TEXT NOT NULL, idade TEXT, modalidade TEXT, status TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, payload_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS sessoes (id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, data TEXT NOT NULL, hora TEXT, origem TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, payload_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS segmentos (id TEXT PRIMARY KEY NOT NULL, job_id TEXT NOT NULL, session_id TEXT NOT NULL, case_id TEXT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, speaker TEXT NOT NULL, speaker_confirmed INTEGER DEFAULT 0, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, text_enc BLOB NOT NULL, checksum TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'committed', created_at INTEGER NOT NULL);",
    "CREATE TABLE IF NOT EXISTS rascunhos (case_id TEXT PRIMARY KEY NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, session_id TEXT, updated_at INTEGER NOT NULL, draft_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS diario_privado (id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE, data TEXT NOT NULL, categoria TEXT, created_at INTEGER NOT NULL, entry_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS processamento_jobs (job_id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL, session_id TEXT NOT NULL, audio_path TEXT NOT NULL, audio_key_enc BLOB, state TEXT NOT NULL, total_segments INTEGER DEFAULT 0, committed_segments INTEGER DEFAULT 0, checkpoint_offset_ms INTEGER DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, error_message TEXT);",
    "CREATE TABLE IF NOT EXISTS agenda (id TEXT PRIMARY KEY NOT NULL, case_id TEXT, data TEXT NOT NULL, hora TEXT NOT NULL, created_at INTEGER NOT NULL, payload_enc BLOB NOT NULL);"
]

SCHEMA_18_19 = [
    "CREATE TABLE IF NOT EXISTS _pacientes (id TEXT PRIMARY KEY NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, payload_enc BLOB NOT NULL);",
    "CREATE VIEW IF NOT EXISTS pacientes AS SELECT id, CAST(NULL AS TEXT) AS nome, created_at, updated_at, payload_enc FROM _pacientes;",
    "CREATE TABLE IF NOT EXISTS sessoes (id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, data TEXT NOT NULL, origem TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, payload_enc BLOB NOT NULL, UNIQUE(id, case_id));",
    "CREATE TABLE IF NOT EXISTS processamento_jobs (job_id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL, session_id TEXT NOT NULL, audio_path TEXT NOT NULL, audio_key_enc BLOB, state TEXT NOT NULL, total_segments INTEGER DEFAULT 0, committed_segments INTEGER DEFAULT 0, checkpoint_offset_ms INTEGER DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, error_message TEXT, UNIQUE(job_id, case_id, session_id), FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE);",
    "CREATE TABLE IF NOT EXISTS segmentos (id TEXT PRIMARY KEY NOT NULL, job_id TEXT NOT NULL, session_id TEXT NOT NULL, case_id TEXT NOT NULL, speaker TEXT NOT NULL, speaker_confirmed INTEGER DEFAULT 0, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, text_enc BLOB NOT NULL, checksum TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'committed', created_at INTEGER NOT NULL, FOREIGN KEY (job_id, case_id, session_id) REFERENCES processamento_jobs(job_id, case_id, session_id) ON DELETE CASCADE, FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE);",
    "CREATE TABLE IF NOT EXISTS rascunhos (case_id TEXT PRIMARY KEY NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, session_id TEXT, updated_at INTEGER NOT NULL, draft_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS diario_privado (id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, data TEXT NOT NULL, created_at INTEGER NOT NULL, entry_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS agenda (id TEXT PRIMARY KEY NOT NULL, case_id TEXT, data TEXT NOT NULL, hora TEXT NOT NULL, created_at INTEGER NOT NULL, payload_enc BLOB NOT NULL);"
]

SCHEMA_20_21 = [
    "CREATE TABLE IF NOT EXISTS _pacientes (id TEXT PRIMARY KEY NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, payload_enc BLOB NOT NULL);",
    "CREATE VIEW IF NOT EXISTS pacientes AS SELECT id, CAST(NULL AS TEXT) AS nome, created_at, updated_at, payload_enc FROM _pacientes;",
    "CREATE TABLE IF NOT EXISTS sessoes (id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, data TEXT NOT NULL, origem TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, payload_enc BLOB NOT NULL, UNIQUE(id, case_id));",
    "CREATE TABLE IF NOT EXISTS processamento_jobs (job_id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL, session_id TEXT NOT NULL, audio_path TEXT NOT NULL, audio_key_enc BLOB, state TEXT NOT NULL, total_segments INTEGER DEFAULT 0, committed_segments INTEGER DEFAULT 0, eof_verified INTEGER DEFAULT 0, chunks_processed INTEGER DEFAULT 0, checkpoint_offset_ms INTEGER DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, error_message TEXT, UNIQUE(job_id, case_id, session_id), FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE);",
    "CREATE TABLE IF NOT EXISTS segmentos (id TEXT PRIMARY KEY NOT NULL, job_id TEXT NOT NULL, session_id TEXT NOT NULL, case_id TEXT NOT NULL, speaker TEXT NOT NULL, speaker_confirmed INTEGER DEFAULT 0, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, text_enc BLOB NOT NULL, checksum TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'committed', created_at INTEGER NOT NULL, FOREIGN KEY (job_id, case_id, session_id) REFERENCES processamento_jobs(job_id, case_id, session_id) ON DELETE CASCADE, FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE);",
    "CREATE TABLE IF NOT EXISTS rascunhos (case_id TEXT PRIMARY KEY NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, session_id TEXT, updated_at INTEGER NOT NULL, draft_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS diario_privado (id TEXT PRIMARY KEY NOT NULL, case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE CASCADE, data TEXT NOT NULL, created_at INTEGER NOT NULL, entry_enc BLOB NOT NULL);",
    "CREATE TABLE IF NOT EXISTS agenda (id TEXT PRIMARY KEY NOT NULL, case_id TEXT, data TEXT NOT NULL, hora TEXT NOT NULL, created_at INTEGER NOT NULL, payload_enc BLOB NOT NULL);"
]

def seed_db(db, v, orphan=False):
    if v == 17:
        db.execute("INSERT INTO pacientes(id,nome,created_at,updated_at,payload_enc) VALUES('A','Ficticio',1,1,x'01')")
    else:
        db.execute("INSERT INTO _pacientes(id,created_at,updated_at,payload_enc) VALUES('A',1,1,x'01')")
    hora_val = "'14:30'" if v == 17 else "NULL"
    db.execute(f"INSERT INTO sessoes(id,case_id,data," + ("hora," if v == 17 else "") + "origem,created_at,updated_at,payload_enc) VALUES('S','A','2026-01-01'," + (hora_val + "," if v == 17 else "") + "'audio',1,1,x'02')")
    db.execute("INSERT INTO processamento_jobs(job_id,case_id,session_id,audio_path,state,total_segments,committed_segments,created_at,updated_at) VALUES('J','A','S','','PROCESSING',0,1,1,1)")
    db.execute("INSERT INTO segmentos(id,job_id,session_id,case_id,speaker,start_ms,end_ms,text_enc,checksum,created_at) VALUES('T',?,'S','A','paciente',0,1000,x'03','chk',1)", ('missing' if orphan else 'J',))
    db.execute("INSERT INTO rascunhos(case_id,session_id,updated_at,draft_enc) VALUES('A','S',1,x'04')")
    cat_val = "'Reflexao'" if v == 17 else "NULL"
    db.execute(f"INSERT INTO diario_privado(id,case_id,data," + ("categoria," if v == 17 else "") + "created_at,entry_enc) VALUES('D','A','2026-01-01'," + (cat_val + "," if v == 17 else "") + "1,x'05')")
    db.execute("INSERT INTO agenda(id,case_id,data,hora,created_at,payload_enc) VALUES('G','A','2026-01-01','10:00',1,x'06')")

