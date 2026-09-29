from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerSoloDirector.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')
def test_r10_real_play_guides_and_restore_contract():
    for key in ['war_guide','camp_guide','continuity_checkpoint','choose_army_action','continue_camp_hour','restore_contract']:
        assert key in SRC
    assert 'autoridades mecânicas existentes' in SRC
def test_r10_identity():
    assert 'VERSION_NAME=9.0.' in PROPS and 'VERSION_CODE=900' in PROPS
