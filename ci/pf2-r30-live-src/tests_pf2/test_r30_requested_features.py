from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e"

def read(name):
    return (JAVA/name).read_text(encoding="utf-8")

def test_solo_multi_character_control_is_real_and_persistent():
    main=read("MainActivity.java")
    party=read("PartyDirector.java")
    db=read("AppDb.java")
    assert "CONTROLE • MESTRE / BARBARA" in main
    assert "PASSAR AO MESTRE" in main
    assert "ASSUMIR CONTROLE" in main
    assert "saveCharacterControl" in main
    assert '"gm".equalsIgnoreCase' in party
    assert "mechanics_rule" in party
    assert "pending_test" in party
    assert "getCharacterControl" in db
    assert "saveCharacterControl" in db

def test_kingdom_turn_guide_is_bound_to_live_mechanics():
    main=read("MainActivity.java")
    assert "ⓘ GUIA COMPLETO DO TURNO" in main
    assert "showKingdomTurnGuideDialog" in main
    assert "new KingmakerTurnGuide(db,kingmakerRepository,kingmakerRules)" in main
    assert "RESOLVER PRÓXIMA DECISÃO" in main
    assert "runGuidedKingdomDecision" in main
    assert "showGuidedKingdomPhaseDialog" in main

def test_canonical_human_gm_profile_is_runtime_wired():
    director=read("BraseiroNarrativeDirector.java")
    profile=read("HumanGmNarrativeProfile.java")
    assert "HumanGmNarrativeProfile.prompt()" in director
    assert "HIERARQUIA:" in director
    assert "PERFIL TÉCNICO CANÔNICO" in profile
    assert "Mestre humano conduzindo uma mesa de RPG em tempo real" in profile
    assert "Ele NÃO substitui:" in profile
    assert "regras do sistema" in profile
