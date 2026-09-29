from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
REPO=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java').read_text(encoding='utf-8')
APPDB=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_r5_identity_preserves_storage_contract():
    assert 'VERSION_NAME=9.0.' in PROPS
    assert 'VERSION_CODE=900' in PROPS
    assert 'DB_SCHEMA=59' in PROPS and 'BACKUP_FORMAT=22' in PROPS

def test_solo_shortcuts_do_not_force_advanced_admin():
    start=MAIN.index('private void showKingmakerSoloHome')
    end=MAIN.index('private void runKingmakerSoloStep',start)
    block=MAIN[start:end]
    assert 'kingdom.setOnClickListener' in block
    assert 'runGuidedKingdomDecision()' in block
    assert 'kingdom_admin_mode",true' not in block

def test_r5_closes_context_companion_and_restore_gaps():
    assert 'contextualCampProposal()' in MAIN
    assert 'showKingmakerCompanionFocus' in MAIN and 'sendAi("Narrador",prompt)' in MAIN
    assert 'validateRestoreAgainst' in REPO
    assert 'kingmakerRestoreAudit' in APPDB
