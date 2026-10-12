from pathlib import Path
from zipfile import ZipFile
import json

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')
ATLAS=ROOT/'android/app/src/main/assets/pathfinder/atlas/inner_sea_canonical_v6_grid_native_hires_compat.zip'


def test_r12_identity_and_locks():
    assert ('VERSION_NAME=9.0.37' in PROPS or 'VERSION_NAME=9.0.37' in PROPS)
    assert ('VERSION_CODE=90037' in PROPS or 'VERSION_CODE=90037' in PROPS)
    assert 'DB_SCHEMA=59' in PROPS
    assert 'BACKUP_FORMAT=22' in PROPS


def test_companion_focus_and_playback_share_one_canonical_key_policy():
    director=(SRC/'KingmakerSoloDirector.java').read_text(encoding='utf-8')
    main=(SRC/'MainActivity.java').read_text(encoding='utf-8')
    policy=(SRC/'KingmakerSoloEventPolicy.java').read_text(encoding='utf-8')
    assert '.put("companion_key",c.optString("companion_key"))' in director
    assert 'KingmakerSoloEventPolicy.playable' in director
    assert 'KingmakerSoloEventPolicy.choose' in main
    assert 'return KingmakerSoloEventPolicy.companionKey(focus);' in main
    assert 'String key=kingmakerCompanionKey(focus)' not in main
    assert 'pending_event_id' in main
    assert 'available",1' in main
    assert 'companionAvailable' in policy


def test_restore_contract_tracks_real_mid_scene_state_not_only_counts():
    director=(SRC/'KingmakerSoloDirector.java').read_text(encoding='utf-8')
    contract=(SRC/'KingmakerContinuityContract.java').read_text(encoding='utf-8')
    repo=(SRC/'KingmakerRepository.java').read_text(encoding='utf-8')
    for token in (
        'kingdom_turn_number','turn_phase','turn_status','turn_resource_roll_json',
        'camp_hours','camp_encounter_json','companion_states',
        'pending_companion_event_states','pending_exploration_event_states','active_war_states',
    ):
        assert token in director and token in contract
    for token in ('validateSemanticRows','kingmakerWarEncounters','kingmakerCompanions','kingmakerCamps','kingmakerArmies'):
        assert token in repo


def test_war_turn_enforces_active_army_and_ui_no_longer_selects_attacker():
    repo=(SRC/'KingmakerRepository.java').read_text(encoding='utf-8')
    main=(SRC/'MainActivity.java').read_text(encoding='utf-8')
    assert 'Somente o exército ativo pode agir nesta rodada.' in repo
    method=main[main.index('private void showKingmakerWarAttackDialog'):main.index('private void showKingmakerWarAdvanceDialog')]
    assert 'Exército ativo:' in method
    assert 'Spinner attacker' not in method
    assert 'targets.get' in method


def test_camp_context_names_match_ui_and_hours_advance_scene_clock():
    service=(SRC/'KingmakerService.java').read_text(encoding='utf-8')
    main=(SRC/'MainActivity.java').read_text(encoding='utf-8')
    assert '.put("hex_id",hex)' in service
    assert '.put("day_number",scene.optInt("day_number",1))' in service
    assert 'advanceSceneHours(1d)' in main


def test_real_atlas_v6_neighbor_graph_supports_long_campaign_walk():
    with ZipFile(ATLAS) as z:
        atlas=json.loads(z.read('pathfinder2e_inner_sea_hex_atlas_v1.json'))['hexes']
    assert len(atlas)==3300
    current='10.10'
    previous=None
    visited=[current]
    for _ in range(72):
        neighbors=list(atlas[current]['neighbors'].values())
        assert neighbors and all(n in atlas for n in neighbors)
        candidates=[n for n in neighbors if n != previous] or neighbors
        nxt=sorted(candidates)[-1]
        assert current in atlas[nxt]['neighbors'].values(), (current,nxt)
        previous,current=current,nxt
        visited.append(current)
    assert len(visited)==73
