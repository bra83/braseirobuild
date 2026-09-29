from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

def test_multi_character_control_creation_and_runtime():
    main = read("MainActivity.java")
    party = read("PartyDirector.java")
    appdb = read("AppDb.java")
    assert 'QUEM CONTROLA ESTE PERSONAGEM?' in main
    assert 'Jogador — eu controlo' in main
    assert 'Mestre — Barbara controla' in main
    assert 'saveCharacterControl(id,controlMode(String.valueOf(characterControl.getSelectedItem()))' in main
    assert 'CONTROLE • MESTRE / BARBARA' in main
    assert '"mode","solo_multi_character"' in party
    assert '"human_players",1' in party
    assert '"gm_controlled"' in party
    assert '"mechanics_rule"' in party
    assert 'saveCharacterControl' in appdb
    assert 'getCharacterControl' in appdb

def test_kingdom_turn_guide_reads_real_rules_and_repository():
    main = read("MainActivity.java")
    guide = read("KingmakerTurnGuide.java")
    rules = read("KingmakerRulesEngine.java")
    assert 'ⓘ GUIA COMPLETO DO TURNO' in main
    assert 'showKingdomTurnGuideDialog' in main
    assert 'rules.turnProcedureGuide()' in guide
    assert 'rules.activityGuideCatalog()' in guide
    assert 'repository.activeTurn()' in guide
    assert 'repository.controlSummary()' in guide
    assert 'SEQUÊNCIA COMPLETA' in guide
    assert 'CONFIGURAÇÃO QUE EXIGE SUA ATENÇÃO' in guide
    assert 'OPÇÕES VÁLIDAS NESTE MOMENTO' in guide
    assert 'canPerformActivity' in guide
    assert 'turnProcedureGuide' in rules
    assert 'activityGuideCatalog' in rules

def test_human_gm_profile_is_runtime_narrative_authority_only():
    profile = read("HumanGmNarrativeProfile.java")
    director = read("BraseiroNarrativeDirector.java")
    orchestrator = read("MasterOrchestrator.java")
    assert 'PERFIL TÉCNICO CANÔNICO' in profile
    assert 'HumanGmNarrativeProfile.prompt()' in director
    assert 'governa SOMENTE apresentação narrativa' in director
    assert 'Resultado mecânico, estado persistido, regras PF2e/Kingmaker e decisões do jogador sempre têm precedência' in director
    assert 'BraseiroNarrativeDirector' in orchestrator
