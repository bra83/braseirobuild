from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text('utf-8')
MARK=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiTtsMarkupDirector.java').read_text('utf-8')
POL=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationPlaybackPolicy.java').read_text('utf-8')
PROPS=(ROOT/'version.properties').read_text('utf-8')

def test_r28_version():
    assert ('VERSION_NAME=9.0.37' in PROPS or 'VERSION_NAME=9.0.37' in PROPS)
    assert ('VERSION_CODE=90037' in PROPS or 'VERSION_CODE=90037' in PROPS)

def test_inline_tags_are_reliable_subset_and_capped():
    for tag in ['whispers','shouting','laughs','sighs','gasp','cough']:
        assert f'"{tag}"' in MARK
    for risky in ['"very slow"','"very fast"','"serious"','"panicked"','"trembling"','"amazed"']:
        assert risky not in MARK
    assert 'tags<4' in MARK
    assert 'tagCount' in MARK

def test_semantic_expression_is_in_director_notes_not_experimental_markup():
    assert 'performanceArcFor' in POL
    assert 'Performance arc for THIS block:' in POL
    assert 'Do not depend on inline tags for the emotional arc' in POL
    assert 'direct speech audibly distinct from exposition' in POL

def test_failed_expressive_block_retries_clean_transcript():
    assert 'final boolean safeRecovery=recoveryAttempt>0' in MAIN
    assert 'final String ttsText=safeRecovery?sentence:expressiveText' in MAIN
    assert 'clean transcript, same performance direction' in MAIN
    assert 'clean transcript, simplified reliable delivery' in MAIN
    assert 'Repetindo o mesmo trecho sem tags inline' in MAIN

def test_cache_is_invalidated_for_safe_arc_pipeline():
    assert 'charon-v19-r28-safe-arcs' in POL
    assert 'pt-BR-charon-mestre-profundo-natural-v9-safe-arcs' in POL
