from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text('utf-8')
MARK=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiTtsMarkupDirector.java').read_text('utf-8')
POLICY=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationPlaybackPolicy.java').read_text('utf-8')
OPTIONS=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/SceneOptionDiversifier.java').read_text('utf-8')
PROPS=(ROOT/'version.properties').read_text('utf-8')

def test_r27_version():
    assert ('VERSION_NAME=9.0.31' in PROPS or 'VERSION_NAME=9.0.29' in PROPS)
    assert ('VERSION_CODE=90031' in PROPS or 'VERSION_CODE=90029' in PROPS)

def test_tts_director_keeps_inline_tags_conservative_and_moves_semantic_arc_to_prompt():
    for tag in ['whispers','shouting','laughs','sighs','gasp','cough']:
        assert f'"{tag}"' in MARK
    for risky in ['"very slow"','"very fast"','"serious"','"panicked"','"trembling"','"amazed"']:
        assert risky not in MARK
    assert 'performanceArcFor' in POLICY
    for word in ['patrulha','ameaca','silencio','fumaca']:
        assert word in POLICY

def test_tts_prompt_explicitly_requires_audible_performance_changes():
    assert 'Make the performance changes clearly audible' in POLICY
    assert 'Do not depend on inline tags for the emotional arc' in POLICY
    assert 'charon-v19-r28-safe-arcs' in POLICY

def test_later_charon_block_failure_retries_same_block_before_stopping():
    assert 'CHARON_BLOCK_RECOVERY_ATTEMPTS = 2' in POLICY
    assert 'CHARON_BLOCK_RETRY_BASE_MS = 3500L' in POLICY
    assert 'charon-block-retry' in MAIN
    assert 'synthesizeChunk(chunks,index,session,coordinated,nextAttempt)' in MAIN
    assert 'Charon perdeu o bloco ' in MAIN
    assert 'charon-partial-error-after-retries' in MAIN

def test_actions_refill_from_current_scene_even_if_history_filtered_them():
    marker='for(JSONObject fallback:contextualFallbacks(narration,declaredAction,scene,beatNumber))'
    assert marker in OPTIONS
    assert 'List<JSONObject> fallbacks=contextualFallbacks' in OPTIONS
    assert 'A cena jogável vale mais que evitar repetição' in OPTIONS

def test_minimum_action_fallbacks_cover_smoke_sound_access_and_threat():
    for label in [
        'OBSERVAR A FUMAÇA ANTES DE SE APROXIMAR',
        'MUDAR DE POSIÇÃO ANTES DE RESPONDER AO SOM',
        'PROCURAR OUTRA PASSAGEM',
        'MANTER COBERTURA E OBSERVAR A AMEAÇA',
    ]:
        assert label in OPTIONS
