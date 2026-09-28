from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def src(name): return (ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'/name).read_text(encoding='utf-8')
def test_flow_is_read_only_and_present():
 s=src('KingmakerSoloDirector.java'); assert 'campaign_flow' in s and 'campaignFlow(' in s and 'Explorar pelo Atlas' in s
 assert 'getWritableDatabase' not in s and 'insert' not in s and 'update(' not in s
def test_companions_are_playable_context():
 s=src('KingmakerSoloDirector.java'); assert 'companionBriefs' in s and 'personal_hook' in s and 'arco(s) pessoal(is)' in s
def test_camp_proposal_uses_atlas_without_inventing_dc():
 s=src('KingmakerService.java'); assert 'contextualCampProposal' in s and 'db.getMapHex' in s and 'CDs e efeitos não são inferidos' in s
def test_locks_preserved():
 s=src('KingmakerService.java'); assert 'KingmakerRulesEngine + Pathfinder2eRulesEngine' in s and 'proposal_only' in s
 v=(ROOT/'version.properties').read_text(encoding='utf-8'); assert 'VERSION_NAME=9.0.' in v and 'DB_SCHEMA=59' in v and 'BACKUP_FORMAT=22' in v
