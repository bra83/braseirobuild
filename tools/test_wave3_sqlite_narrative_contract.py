from pathlib import Path
import sqlite3, json
ROOT=Path(__file__).resolve().parents[1]
schema=(ROOT/'tools/m5_schema_v1.sql').read_text()
con=sqlite3.connect(':memory:')
con.executescript(schema)
state0='{"revision":0}'
con.execute("INSERT INTO campaign_meta VALUES (?,?,?,?,?,?,?,?,?)",('c','C',0,1,1,'h0','splitmix64-counter-v1','seed',0))
con.execute("INSERT INTO campaign_state VALUES (?,?,?,?)",('c',0,state0,'h0'))
con.execute("INSERT INTO campaign_event VALUES (?,?,?,?,?,?,?,?)",('mech','c',1,'PLAYER_ACTION_SKILL_CHECK','{}','h0','h1',1))
con.execute("UPDATE campaign_state SET revision=1,state_json=?,checksum='h1' WHERE campaign_id='c'",('{"revision":1}',))
con.execute("UPDATE campaign_meta SET revision=1,state_checksum='h1' WHERE campaign_id='c'")
before=con.execute("SELECT revision,state_json,checksum FROM campaign_state WHERE campaign_id='c'").fetchone()
# Narrative shares the mechanical revision and checksum but does not touch aggregate state.
con.execute("INSERT INTO campaign_event VALUES (?,?,?,?,?,?,?,?)",('narrative:mech','c',1,'NARRATIVE',json.dumps({'parentMechanicalEventId':'mech','text':'ok'}),'h1','h1',2))
after=con.execute("SELECT revision,state_json,checksum FROM campaign_state WHERE campaign_id='c'").fetchone()
events=con.execute("SELECT event_kind,revision FROM campaign_event WHERE campaign_id='c' ORDER BY created_at_audit").fetchall()
assert before==after
assert events==[('PLAYER_ACTION_SKILL_CHECK',1),('NARRATIVE',1)]
print('WAVE3_SQLITE_NARRATIVE_CONTRACT_PASS state_unchanged=YES shared_revision=1 events=2')
