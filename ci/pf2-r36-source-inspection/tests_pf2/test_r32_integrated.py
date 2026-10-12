from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

def test_solo_means_one_human_with_multiple_character_control():
    main=read("MainActivity.java")
    party=read("PartyDirector.java")
    master=read("MasterOrchestrator.java")
    db=read("AppDb.java")
    assert 'QUEM CONTROLA ESTE PERSONAGEM?' in main
    assert 'Jogador — eu controlo' in main
    assert 'Mestre — Barbara controla' in main
    assert 'CONTROLE • MESTRE / BARBARA' in main
    assert 'ASSUMIR CONTROLE' in main
    assert 'PASSAR AO MESTRE' in main
    assert 'saveCharacterControl(id,controlMode(String.valueOf(characterControl.getSelectedItem()))' in main
    assert 'normalizeCharacterControlMode' in db
    assert '"mode","solo_multi_character"' in party
    assert '"human_players",1' in party
    assert '"player_controlled"' in party
    assert '"gm_controlled"' in party
    assert '"mechanics_rule"' in party
    assert 'party.hasGmControlledHeroes()' in master
    assert 'party_control_policy' in master

def test_gm_control_does_not_create_second_mechanics_engine():
    party=read("PartyDirector.java")
    master=read("MasterOrchestrator.java")
    assert 'Nenhuma decisão de Barbara altera PV, condições, inventário, recursos, spell slots, posição ou outro estado diretamente' in party
    assert 'pending_test/combat/transação estruturada' in party
    assert 'mesmo protocolo/motor PF2e' in party
    assert 'party.buildActionBatch' in master

def test_kingdom_help_reads_the_real_rules_engine():
    main=read("MainActivity.java")
    guide=read("KingmakerTurnGuide.java")
    rules=read("KingmakerRulesEngine.java")
    assert 'ⓘ GUIA COMPLETO DO TURNO' in main
    assert 'showKingdomTurnGuideDialog' in main
    assert 'rules.turnProcedureGuide()' in guide
    assert 'rules.activityGuideCatalog()' in guide
    assert 'repository.activeTurn()' in guide
    assert 'repository.controlSummary()' in guide
    assert 'canPerformActivity' in guide
    assert 'SEQUÊNCIA COMPLETA' in guide
    assert 'CONFIGURAÇÃO QUE EXIGE SUA ATENÇÃO' in guide
    assert 'OPÇÕES VÁLIDAS NESTE MOMENTO' in guide
    assert 'turnProcedureGuide()' in rules
    assert 'activityGuideCatalog()' in rules
    assert 'KingmakerRepository.resolveKingdomActivity' in rules

def test_attached_human_gm_profile_is_in_runtime_but_below_rules():
    profile=read("HumanGmNarrativeProfile.java")
    director=read("BraseiroNarrativeDirector.java")
    assert 'PERFIL TÉCNICO CANÔNICO' in profile
    assert 'MESTRE HUMANO IMERSIVO PARA VTT' in profile
    assert 'HumanGmNarrativeProfile.prompt()' in director
    assert 'governa SOMENTE apresentação narrativa' in director
    assert 'Resultado mecânico, estado persistido, regras PF2e/Kingmaker e decisões do jogador sempre têm precedência' in director
