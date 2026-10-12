from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
MAIN=(SRC/'MainActivity.java').read_text(encoding='utf-8')
REPO=(SRC/'KingmakerRepository.java').read_text(encoding='utf-8')
SOLO=(SRC/'KingmakerSoloDirector.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_r6_identity_and_locks():
    assert 'VERSION_NAME=9.0.' in PROPS and 'VERSION_CODE=900' in PROPS
    assert 'DB_SCHEMA=59' in PROPS and 'BACKUP_FORMAT=22' in PROPS

def test_camp_random_encounter_is_a_real_blocking_state():
    assert 'resumeCampAfterEncounter' in REPO
    assert 'Resolva o encontro aleatório antes de registrar outra hora' in REPO
    assert 'Resolva o encontro aleatório antes de concluir o acampamento' in REPO
    assert '"resolved",false' in REPO
    assert 'playKingmakerPendingCampEncounter' in MAIN
    assert 'Retomar acampamento após resolver' in MAIN

def test_companion_personal_event_is_persistent_but_not_kingdom_event():
    assert 'ensureCompanionPersonalEvent' in REPO
    assert 'pendingCompanionEvent' in REPO
    assert 'recordCompanionQuestProgress' in REPO
    assert 'event_type' in SOLO and 'companionEvents' in SOLO
    assert 'companion_event' in SOLO and 'playPendingKingmakerCompanionEvent' in MAIN
    # Companion event must not be fed to the kingdom EVENT phase.
    assert 'kingdomGuide=kingdomTurnGuide(turn,pendingEvents)' in SOLO

def test_atlas_exploration_guide_uses_manifest_neighbors_without_writes():
    assert 'exploration_guide' in SOLO
    assert 'MapManifestGeometry.parse' in SOLO and '.neighbors(current)' in SOLO
    assert 'repository.hex(id)' in SOLO
    assert 'getWritableDatabase' not in SOLO
    assert 'Adjacentes do Atlas:' in MAIN

def test_r6_long_campaign_gate_is_wired():
    script=(ROOT/'tools/test_kingmaker_solo_campaign_r6.sh').read_text(encoding='utf-8')
    harness=(ROOT/'tests_pf2/java/KingmakerSoloCampaignR6Harness.java').read_text(encoding='utf-8')
    assert 'KingmakerSoloCampaignR6Harness' in script
    for token in ['campEncounters','campResumes','companionProgress','neighborWindows','restoreAtMidpoint']:
        assert token in harness
