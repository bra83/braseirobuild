from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
MAIN=(SRC/'MainActivity.java').read_text(encoding='utf-8')
REPO=(SRC/'KingmakerRepository.java').read_text(encoding='utf-8')
SOLO=(SRC/'KingmakerSoloDirector.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_r7_historical_locks_remain_preserved():
    assert 'VERSION_NAME=9.0.' in PROPS and 'VERSION_CODE=900' in PROPS
    assert 'DB_SCHEMA=59' in PROPS and 'BACKUP_FORMAT=22' in PROPS

def test_exploration_event_is_persistent_and_separate_from_kingdom_event_phase():
    for token in ['pendingExplorationEvent','ensureExplorationEvent','resolveExplorationEvent','event_type=\'exploration\'']:
        assert token in REPO
    assert 'explorationEvents' in SOLO
    assert 'kingdomGuide=kingdomTurnGuide(turn,pendingEvents)' in SOLO
    assert '"exploration".equalsIgnoreCase(type)' in MAIN
    assert 'Eventos pessoais e de expedição ficam no fluxo Solo' in MAIN

def test_expedition_planning_uses_canonical_neighbor_and_never_moves_directly():
    start=MAIN.index('private void planKingmakerNeighbor')
    end=MAIN.index('private void playKingmakerCurrentHexExploration', start)
    method=MAIN[start:end]
    assert 'MapManifestGeometry.parse' in method
    assert 'geometry.neighbors(current).contains(target)' in method
    assert 'db.saveMapTravelState' in method
    assert 'new JSONArray().put(target)' in method
    assert 'setCurrentHexId' not in method
    assert 'saveMapViewExtras' not in method
    assert 'reconnoiterHex' not in method and 'clearHex' not in method

def test_arrival_exploration_requires_scene_then_explicit_resolution():
    assert 'playKingmakerCurrentHexExploration' in MAIN
    assert 'playKingmakerExplorationEvent' in MAIN
    assert 'showKingmakerExplorationResultDialog' in MAIN
    assert 'não marque o hex automaticamente' in MAIN
    assert 'resolveExplorationEvent' in MAIN
    assert 'reconnoiterHex(hex,resolutionNotes)' in REPO
    assert 'clearHex(hex,resolutionNotes)' in REPO

def test_solo_flow_exposes_route_and_current_hex_stages():
    for token in ['destination_hex','next_hex','route_length','expedition_travel','explore_current','plan_expedition','exploration_event']:
        assert token in SOLO
    for token in ['EXPEDIÇÃO','ABRIR ATLAS E AVANÇAR','PLANEJAR PRÓXIMO TRECHO']:
        assert token in MAIN

def test_r7_long_campaign_gate_is_wired():
    script=(ROOT/'tools/test_kingmaker_solo_campaign_r7.sh').read_text(encoding='utf-8')
    harness=(ROOT/'tests_pf2/java/KingmakerSoloCampaignR7Harness.java').read_text(encoding='utf-8')
    assert 'KingmakerSoloCampaignR7Harness' in script
    for token in ['plannedSegments','travelSegments','reconEvents','clearEvents','canonicalNeighborChecks','restoreAtMidpoint']:
        assert token in harness

def test_pending_exploration_cannot_be_abandoned_for_a_new_expedition():
    for token in ['pending_event_hex','pending_event_here','explorationHex(source)']:
        assert token in SOLO
    assert 'A expedição não abre outro trecho enquanto essa cena estiver pendente' in MAIN
    assert 'Conclua primeiro a exploração pendente no hex' in MAIN
    planner=MAIN[MAIN.index('private void showKingmakerExpeditionPlanner'):MAIN.index('private void planKingmakerNeighbor')]
    assert 'pending_event_hex' in planner
    assert 'showKingmakerExplorationGuide()' in planner
