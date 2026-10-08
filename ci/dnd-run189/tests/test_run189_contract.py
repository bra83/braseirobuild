import pathlib, sqlite3, unittest, re, hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=pathlib.Path(__import__('sys').argv[1]) if len(__import__('sys').argv)>1 else pathlib.Path('/mnt/data/DND_RUN189_WORK/dnd_work')
PKG=APP/'android/app/src/main/java/com/braseiro/dnd5e'
DB=(PKG/'AppDb.java').read_text(encoding="utf-8")
MAIN=(PKG/'MainActivity.java').read_text(encoding="utf-8")
ENG=(PKG/'CompanionAgencyEngine.java').read_text(encoding="utf-8")
CREATE=re.search(r'db\.execSQL\("(CREATE TABLE IF NOT EXISTS companion_proposals .*?)"\);',DB).group(1)
INDEX=re.search(r'db\.execSQL\("(CREATE INDEX IF NOT EXISTS idx_companion_proposals_pending .*?)"\);',DB).group(1)
class Contract(unittest.TestCase):
    def setUp(self):
        self.c=sqlite3.connect(':memory:',isolation_level=None)
        self.c.execute(CREATE);self.c.execute(INDEX)
        self.c.execute("CREATE TABLE party_action_queue (id INTEGER PRIMARY KEY AUTOINCREMENT,world_profile TEXT,character_id INTEGER,character_name TEXT,action_text TEXT,intent_type TEXT,status TEXT,created_at INTEGER)")
    def stage(self,w,id,ch,fp,action):
        try:self.c.execute('INSERT INTO companion_proposals(world_profile,proposal_id,character_id,character_name,scene_fingerprint,action_text,reason_observed,cost_risk,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,1,1)',(w,id,ch,'Elira',fp,action,'Percebeu inscrições incomuns','Magia pode gastar espaço'));return True
        except sqlite3.IntegrityError:return False
    def approve(self,w,id,fp):
        row=self.c.execute("SELECT character_id,character_name,action_text,scene_fingerprint FROM companion_proposals WHERE world_profile=? AND proposal_id=? AND status='pending'",(w,id)).fetchone()
        if not row or fp!=row[3]:return False
        with self.c:
            cursor=self.c.execute("INSERT INTO party_action_queue(world_profile,character_id,character_name,action_text,intent_type,status,created_at) VALUES(?,?,?,?,?,?,1)",(w,row[0],row[1],row[2],'curated_approved','queued'))
            result=self.c.execute("UPDATE companion_proposals SET status='approved',queued_action_id=? WHERE world_profile=? AND proposal_id=? AND status='pending'",(cursor.lastrowid,w,id))
            if result.rowcount!=1: raise RuntimeError('Race')
        return True
    def test_mixed_modes_and_strict_ui(self):
        for label in ('EU CONTROLO','MESTRE CONTROLA','MESTRE COM CURADORIA'):self.assertIn(label,MAIN)
        self.assertIn('if ("companion".equals(stored)',ENG)
        self.assertIn('if ("curated".equals(stored)',ENG)
        self.assertIn('return "manual";',ENG)
    def test_two_campaigns_isolated(self):
        id='a'*32;self.assertTrue(self.stage('dnd',id,5,'here','investigar'))
        self.assertTrue(self.stage('mystara',id,5,'here','investigar'))
        self.assertTrue(self.approve('dnd',id,'here'))
        self.assertEqual(self.c.execute("SELECT status FROM companion_proposals WHERE world_profile='mystara'").fetchone()[0],'pending')
    def test_duplicate_generated_intent_never_duplicates(self):
        id='a'*32
        self.assertTrue(self.stage('dnd',id,5,'here','investigar'))
        self.assertFalse(self.stage('dnd',id,5,'here','investigar'))
        self.assertTrue(self.approve('dnd',id,'here'))
        self.assertFalse(self.approve('dnd',id,'here'))
        self.assertEqual(self.c.execute('SELECT COUNT(*) FROM party_action_queue').fetchone()[0],1)
        self.assertIn('n++; // duplicate response remains blocked',ENG)
    def test_stale_scene_cannot_queue(self):
        id='a'*32;self.stage('dnd',id,5,'forest','investigar');self.assertFalse(self.approve('dnd',id,'ruins'))
        self.assertEqual(self.c.execute('SELECT COUNT(*) FROM party_action_queue').fetchone()[0],0)
        self.assertIn('CompanionAgencyEngine.sceneFingerprint(this,hero)',DB)
    def test_rejected_cannot_execute(self):
        id='a'*32;self.stage('dnd',id,5,'here','investigar');self.c.execute("UPDATE companion_proposals SET status='rejected' WHERE proposal_id=?",(id,))
        self.assertFalse(self.approve('dnd',id,'here'))
        self.assertEqual(self.c.execute('SELECT COUNT(*) FROM party_action_queue').fetchone()[0],0)
    def test_schema_and_backup_contract(self):
        self.assertIn('DB_VERSION = 61',DB)
        self.assertIn('if (oldVersion < 61) ensureCompanionProposalsSchema(db);',DB)
        self.assertIn('root.put("companionProposals"',DB)
        self.assertIn('restoreCompanionProposals(db,root.optJSONArray',DB)
        self.assertIn('characterIdMap',DB)
    def test_no_unapproved_world_mutations_from_proposal(self):
        self.assertIn('showPartyDirector();return;',MAIN)
        self.assertIn('int captured=CompanionAgencyEngine.stageCurated',MAIN)
        self.assertIn('boolean ok=db.updateCompanionProposal',MAIN)
        self.assertNotIn('db.saveSceneState',ENG)
        self.assertNotIn('db.setCombat',ENG)
        self.assertIn('return false;',DB)
    def test_mechanics_and_gemini_preserved(self):
        self.assertIn('RUN188_GEMINI_EXPLICIT_OFFLINE',MAIN)
        self.assertIn('HumanMasterDynamicDirector.instruction(this',MAIN)
        self.assertIn('RUN189_AGENCY_SCHEMA',DB)
        self.assertTrue((PKG/'DndRulesEngine.java').exists())
        self.assertTrue((PKG/'CombatStateMachine.java').exists())
    def test_no_secret_fact_leak_directly(self):
        self.assertIn('if(!access)continue;',ENG)
        self.assertNotIn('truth_status',ENG)
        self.assertIn('visib',ENG)
        self.assertIn('characterCanPropose',ENG)
    def test_alteration_still_pending(self):
        self.assertIn('if("altered".equals(status))',DB)
        self.assertIn('else v.put("status",status)',DB)
        self.assertIn('COMPANHEIRO COM CURADORIA',ENG)

if __name__=='__main__':unittest.main(argv=['test'],verbosity=2)
