from pathlib import Path
import sqlite3, tempfile
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
DB=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text(encoding='utf-8')
ORCH=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MasterOrchestrator.java').read_text(encoding='utf-8')
def segment(text,start,end):
    a=text.index(start); b=text.index(end,a); return text[a:b]

def test_scene_apply_is_one_checked_transaction_and_rollback_is_not_double_restored():
    s=segment(MAIN,'private void applyMasterConsequences(final long transactionId','private JSONObject copyJsonObject')
    assert 'db.runInTransactionChecked' in s
    assert 'masterOrchestrator.afterConsequencesApplied' in s
    assert 'db.quickIntegrityCheck()' in s
    assert 'if(databaseRolledBackCleanly)' in s
    assert 'else if(rollback!=null&&safety!=null)' in s
    assert s.index('masterOrchestrator.afterConsequencesApplied') < s.index('}});')

def test_master_cognitive_state_is_write_then_read_confirmed():
    s=segment(DB,'public void saveMasterRuntimeStateChecked','public void savePartyStateChecked')
    assert 'saveMasterRuntimeState(state)' in s
    assert 'after=getMasterRuntimeState()' in s
    assert 'Persistência cognitiva divergente' in s
    assert 'updated_at' in s
    assert 'storeValidationState(envelope,plan);' in ORCH

def test_quick_check_is_targeted_sqlite_integrity_check():
    s=segment(DB,'public boolean quickIntegrityCheck()','public JSONObject databaseHealth5')
    assert 'PRAGMA quick_check(1)' in s
    assert 'fullHealth' not in s

def test_transaction_model_rolls_back_every_scene_write_on_failure():
    con=sqlite3.connect(':memory:')
    con.executescript('create table scene(v text); create table master(v text); create table npc(v text);')
    con.execute("insert into scene values('old')"); con.execute("insert into master values('old')"); con.commit()
    try:
        con.execute('begin')
        con.execute("update scene set v='new'")
        con.execute("update master set v='new'")
        con.execute("insert into npc values('new')")
        raise RuntimeError('injeção de falha depois do estado cognitivo')
    except RuntimeError:
        con.rollback()
    assert con.execute('select v from scene').fetchone()[0]=='old'
    assert con.execute('select v from master').fetchone()[0]=='old'
    assert con.execute('select count(*) from npc').fetchone()[0]==0
    assert con.execute('pragma quick_check').fetchone()[0]=='ok'
