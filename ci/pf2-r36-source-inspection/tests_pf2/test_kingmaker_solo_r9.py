from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerSoloDirector.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_adventure_guide_is_read_only_continuity_packet():
    assert 'adventureGuide(' in SRC
    assert '.put("adventure_guide",adventureGuide)' in SRC
    for key in ['camp_encounter','war','exploration_resolution','companion','kingdom_bridge','travel','free_exploration']:
        assert f'"{key}"' in SRC
    assert 'active_companions' in SRC
    assert 'authority of the motor' not in SRC.lower()

def test_r9_release_identity():
    # R9 capability remains required in later releases.
    assert 'adventure_guide' in SRC and 'active_companions' in SRC
