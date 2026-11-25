"""SQL regression on the literal DDL/DML from EcoDatabaseHelper (not Android instrumentation).
Pass path to historical schema fixture script or use bundled fixture.
"""
import pathlib, sqlite3, re, ast, sys
root=pathlib.Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/br/com/tutorclinico/EcoDatabaseHelper.java').read_text()
fixture=pathlib.Path(__file__).with_name('eco22_historical_schemas.py')
ns={};exec(fixture.read_text(),ns)
SCHEMA_17=ns['SCHEMA_17']; SCHEMA_18_19=ns['SCHEMA_18_19']; SCHEMA_20_21=ns['SCHEMA_20_21'];seed_db=ns['seed_db']
# Use actual statement strings; choose the same branch as Java by schema introspection.
def literals(fragment):
    return [''.join(re.findall(r'"([^"\n]*)"',x)) for x in re.findall(r'db\.execSQL\((.*?)\);[ \t]*\n',fragment,re.S)]
def cols(db,t):return {x[1] for x in db.execute(f'PRAGMA table_info({t})')}
def migrate(db,inject_failure=False):
    assert db.execute('PRAGMA foreign_keys').fetchone()==(1,)
    section=source.split('private void upgradeToV3(')[1].split('private boolean hasColumn(')[0]
    tables={x[0] for x in db.execute("SELECT name FROM sqlite_master WHERE type='table'")};view=db.execute("SELECT 1 FROM sqlite_master WHERE name='pacientes' AND type='view'").fetchone()
    pac='_pacientes' if '_pacientes' in tables else 'pacientes'
    oldcols={t:cols(db,t) for t in tables}
    db.execute('BEGIN') # SQLiteOpenHelper outer transaction; FKs remain ON
    try:
        for st in literals(section):
            if st.startswith('INSERT INTO _pacientes_v3'):st=st.replace('FROM ;','FROM '+pac+';')
            if st.startswith('INSERT INTO sessoes_v3') and (('CAST(NULL AS TEXT)' in st)==('hora' in oldcols['sessoes'])):continue
            if st.startswith('INSERT INTO processamento_jobs_v3') and 'FROM processamento_jobs;' in st:
                if ('CAST(NULL AS BLOB)' in st)==('audio_key_enc' in oldcols['processamento_jobs']):continue
                st=st.replace('processamento_jobseof_verifiedeof_verified0','eof_verified' if 'eof_verified' in oldcols['processamento_jobs'] else '0')
            if st.startswith('INSERT INTO segmentos_v3') and (('speaker, 0,' in st)==('speaker_confirmed' in oldcols['segmentos'])):continue
            if st.startswith('INSERT INTO diario_privado_v3') and (('CAST(NULL AS TEXT)' in st)==('categoria' in oldcols['diario_privado'])):continue
            if st=='DROP VIEW IF EXISTS pacientes;' and not view:continue
            if st=='DROP TABLE IF EXISTS pacientes;' and view:continue
            if st.startswith('DROP TABLE IF EXISTS segmentos;') and inject_failure:raise RuntimeError('injected failure before drop')
            db.execute(st)
        assert db.execute('PRAGMA foreign_key_check').fetchall()==[]
        assert db.execute('PRAGMA integrity_check').fetchall()==[('ok',)]
        db.execute('COMMIT')
    except BaseException:
        db.execute('ROLLBACK');raise

def run(v,orphan=False,fresh22=False,rollback=False):
    db=sqlite3.connect(':memory:',isolation_level=None);db.execute('PRAGMA foreign_keys=ON')
    schema=SCHEMA_17 if v==17 else SCHEMA_18_19 if v<=20 else SCHEMA_20_21
    for ddl in schema:db.execute(ddl)
    if fresh22:
        db.execute('ALTER TABLE sessoes ADD COLUMN hora TEXT');db.execute('ALTER TABLE diario_privado ADD COLUMN categoria TEXT')
    seed_db(db,v,orphan)
    if fresh22:db.execute("UPDATE sessoes SET hora='14:30'");db.execute("UPDATE diario_privado SET categoria='Reflexao'")
    if 'eof_verified' in cols(db,'processamento_jobs'):db.execute("UPDATE processamento_jobs SET eof_verified=1,state='COMMITTED',total_segments=1")
    # Add second patient/session/job to ensure no accidental cross-case cascade.
    if v==17:db.execute("INSERT INTO pacientes(id,nome,created_at,updated_at,payload_enc) VALUES('B','B',1,1,x'09')")
    else:db.execute("INSERT INTO _pacientes(id,created_at,updated_at,payload_enc) VALUES('B',1,1,x'09')")
    db.execute("INSERT INTO sessoes(id,case_id,data,origem,created_at,updated_at,payload_enc) VALUES('S2','B','2026-01-02','audio',1,1,x'09')")
    db.execute("INSERT INTO processamento_jobs(job_id,case_id,session_id,audio_path,state,total_segments,committed_segments,created_at,updated_at) VALUES('J2','B','S2','','PROCESSING',0,1,1,1)")
    db.execute("INSERT INTO segmentos(id,job_id,session_id,case_id,speaker,start_ms,end_ms,text_enc,checksum,created_at) VALUES('T2','J2','S2','B','paciente',0,1000,x'09','chk2',1)")
    preserve={t:list(db.execute(f'SELECT * FROM {t} ORDER BY 1')) for t in ('sessoes','segmentos','rascunhos','diario_privado','agenda')}
    # Compare every original column retained by the target rather than count only.
    names={t:[x[1] for x in db.execute(f'PRAGMA table_info({t})')] for t in preserve}
    try:migrate(db,rollback)
    except RuntimeError:
        if not rollback:raise
        for t,rows in preserve.items():assert list(db.execute(f'SELECT * FROM {t} ORDER BY 1'))==rows
        print('PASS rollback retains original rows');return
    assert not rollback
    for t,rows in preserve.items():assert list(db.execute('SELECT '+','.join(names[t])+f' FROM {t} ORDER BY 1'))==rows,t
    assert db.execute('SELECT COUNT(*) FROM _pacientes').fetchone()==(2,)
    if orphan:assert db.execute("SELECT state FROM processamento_jobs WHERE job_id='missing'").fetchone()==('ORPHAN_RECOVERED',)
    if v==17 or fresh22:
        assert db.execute("SELECT hora FROM sessoes WHERE id='S'").fetchone()==('14:30',)
        assert db.execute("SELECT categoria FROM diario_privado WHERE id='D'").fetchone()==('Reflexao',)
    if v>=21:assert db.execute("SELECT eof_verified,state FROM processamento_jobs WHERE job_id='J'").fetchone()==(1,'COMMITTED')
    assert db.execute('PRAGMA foreign_keys').fetchone()==(1,)
    print(f'PASS migration 0.{v} -> v5, FK ON, 2 patients, full retained-column equality'+(' (orphan)' if orphan else '')+(' (0.22 fresh)' if fresh22 else ''))
for v in (17,18,19,20,21):run(v)
run(17,orphan=True);run(21,fresh22=True);run(21,rollback=True)
assert 'DB_VERSION = 5;' in source
print('PASS schema version 5')
