from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OPTIONS=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/SceneOptionDiversifier.java').read_text(encoding='utf-8')
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
VALIDATOR=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MasterResponseValidator.java').read_text(encoding='utf-8')
COGNITIVE=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MasterCognitiveEngine.java').read_text(encoding='utf-8')

def test_r14_choice_budget_is_four_to_six_when_context_allows():
    assert 'MAX_OPTIONS = 6' in OPTIONS
    assert 'MIN_USEFUL_OPTIONS = 4' in OPTIONS
    assert 'normalmente 4 a 6 escolhas' in MAIN or 'normalmente de quatro a seis escolhas' in MAIN
    assert 'options_max",6' in COGNITIVE
    assert 'out.length()<6' in VALIDATOR

def test_r14_removes_vague_reassessment_fallback():
    assert 'Reavaliar posição e saídas' not in OPTIONS
    for token in ['SEGUIR OS RASTROS RECENTES','EXAMINAR OS RASTROS ANTES DE AVANÇAR','AVANÇAR PARALELO À TRILHA EM SILÊNCIO','PROCURAR UM PONTO DE INTERCEPTAÇÃO DOS RASTROS']:
        assert token in OPTIONS

def test_r14_choices_explain_tradeoff_not_only_label():
    for token in ['o risco é entrar no caminho','custa tempo e pode esfriar','pode economizar tempo','risco social de endurecer']:
        assert token in OPTIONS

def test_r14_runtime_gate_exists():
    assert (ROOT/'tools/test_scene_choices_r14.sh').is_file()
    assert (ROOT/'ferramentas/SceneChoicesR14SelfTest.java').is_file()
