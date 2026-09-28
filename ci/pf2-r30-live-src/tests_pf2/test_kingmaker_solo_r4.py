from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
REPO=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java').read_text(encoding='utf-8')
SOLO=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerSoloDirector.java').read_text(encoding='utf-8')
APPDB=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text(encoding='utf-8')

def test_guided_turn_stays_in_solo_home():
    assert 'runGuidedKingdomDecision()' in MAIN
    assert 'showGuidedKingdomPhaseDialog' in MAIN
    assert '"resolve_consumption".equals(action)' in MAIN
    assert '"resolve_event".equals(action)' in MAIN
    assert '"phase_choice".equals(action)' in MAIN
    assert '"advance_phase".equals(action)' in MAIN

def test_guide_is_one_human_decision_at_a_time():
    for token in ['kingdom_turn_guide','resolve_consumption','phase_choice','resolve_event','advance_phase']:
        assert token in SOLO
    assert 'uma decisão humana' in SOLO or 'primeira decisão humana' in SOLO

def test_war_attack_targets_real_army_ac_and_applies_damage():
    assert 'resolveWarAttack' in REPO
    assert 'target.optInt("ac")' in REPO
    assert 'damageArmy(targetClean' in REPO
    assert 'battle_complete' in REPO
    assert 'showKingmakerWarAttackDialog' in MAIN
    assert 'CA vem da ficha do exército-alvo' in MAIN

def test_backup_restore_keeps_kingmaker_block_and_canonical_atlas_boundary():
    assert 'KingmakerRepository' in APPDB
    assert 'exportJson' in APPDB and 'restoreJson' in APPDB
    assert 'Atlas canônico' in SOLO or 'Atlas canônico' in MAIN

def test_campaign_flow_covers_long_campaign_surfaces():
    for token in ['"camp"','"war"','"event"','"companions"','"explore"','"kingdom_turn"','"kingdom_transition"']:
        assert token in SOLO

def test_camping_is_contextual_and_has_no_fabricated_default_dcs():
    assert 'contextualCampProposal()' in MAIN
    start=MAIN.index('private void showKingmakerStartCampDialog')
    end=MAIN.index('private void showKingmakerCampDetails',start)
    block=MAIN[start:end]
    assert '"0"' in block
    assert 'CD da Zona","15"' not in block
    assert 'CD inicial de encontro","17"' not in block
    assert 'não inventa dificuldade' in block


def test_restore_is_audited_inside_appdb_transaction():
    assert 'validateRestoreAgainst' in REPO
    assert 'kingmakerRestoreAudit' in APPDB
    assert 'A restauração foi revertida pela auditoria Kingmaker' in APPDB
    assert 'setTransactionSuccessful' in APPDB


def test_companion_focus_becomes_playable_narrative_scene_without_direct_mechanics():
    assert 'showKingmakerCompanionFocus' in MAIN
    assert 'Hook pessoal persistido' in MAIN
    assert 'Não altere influência, Reino, inventário ou qualquer mecânica diretamente' in MAIN
    assert 'sendAi("Narrador",prompt)' in MAIN
