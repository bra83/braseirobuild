from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'

def read(name): return (JAVA/name).read_text(encoding='utf-8')

def test_narrator_retrieval_is_need_based():
    src=read('RuleRetrievalEngine.java')
    assert 'public static boolean shouldRetrieve' in src
    assert 'return detectIntents(current).length()>0||looksMechanical(current);' in src
    assert 'out.put("regra_contextual")' not in src

def test_scene_rule_cache_is_wired_into_runtime():
    src=read('MainActivity.java')
    assert 'new SceneRuleCache()' in src
    assert 'sceneRuleCache.get' in src and 'sceneRuleCache.put' in src
    assert 'not_required_fiction' in src and 'scene_rule_cache_hit' in src

def test_world_beat_and_adaptive_pacing_are_in_master_context():
    src=read('MasterOrchestrator.java')
    assert 'WorldBeatDirector.preview' in src
    assert 'NarrativePacingDirector.plan' in src
    assert 'root.put("world_beat",worldBeat)' in src
    assert 'root.put("narrative_pacing",narrativePacing)' in src

def test_reduced_context_preserves_living_scene_directors():
    src=read('MasterOrchestrator.java')
    assert '"world_beat","narrative_pacing"' in src
    assert 'emergency.put("world_beat"' in src
    assert 'emergency.put("narrative_pacing"' in src

def test_world_simulation_skips_manual_lookup_for_narrative_only_actions():
    src=read('LivingWorldSimulationEngine.java')
    assert 'containsMechanicalAction' in src
    assert 'narrativeWorldRules("world_tick")' in src
    assert 'mechanical?retrievalEngine.retrieveForWorldAction' in src

def test_prompt_no_longer_claims_mandatory_search_every_scene():
    src=read('MainActivity.java')
    assert 'consulta obrigatoriamente todos os manuais locais habilitados em cada cena' not in src
    assert 'ficção pura não dispara busca' in src

def test_variable_scene_length_is_explicit_not_fixed_quota():
    src=read('NarrativePacingDirector.java')
    assert 'beat_phase' in src
    assert 'Comprimento segue a importância da batida, não uma faixa fixa' in src
    assert 'int[][] bands' in src
