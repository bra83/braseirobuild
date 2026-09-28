from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
MAIN=(SRC/'MainActivity.java').read_text(encoding='utf-8')
SOLO=(SRC/'KingmakerSoloDirector.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_r8_identity_and_locks():
    assert 'VERSION_NAME=9.0.' in PROPS and 'VERSION_CODE=900' in PROPS
    assert 'DB_SCHEMA=59' in PROPS and 'BACKUP_FORMAT=22' in PROPS

def test_r8_carries_pending_exploration_origin_into_solo_guide():
    for token in ['pending_event_id','pending_event_title','pending_event_source','pending_event_hex','pending_event_here','explorationHex(source)']:
        assert token in SOLO

def test_r8_blocks_new_planning_until_remote_pending_scene_is_resolved():
    assert 'A expedição não abre outro trecho enquanto essa cena estiver pendente' in MAIN
    assert 'Conclua primeiro a exploração pendente no hex' in MAIN
    planner=MAIN[MAIN.index('private void showKingmakerExpeditionPlanner'):MAIN.index('private void planKingmakerNeighbor')]
    assert 'pending_event_hex' in planner
    assert 'showKingmakerExplorationGuide()' in planner
    guide=MAIN[MAIN.index('private void showKingmakerExplorationGuide'):MAIN.index('private void showKingmakerExpeditionPlanner')]
    assert 'ABRIR ATLAS' in guide
    assert 'não teletransporta o grupo' in guide
