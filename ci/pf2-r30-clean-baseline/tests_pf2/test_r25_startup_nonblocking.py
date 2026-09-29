from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_r25_version():
    assert ('VERSION_NAME=9.0.31' in PROPS or 'VERSION_NAME=9.0.29' in PROPS)
    assert ('VERSION_CODE=90031' in PROPS or 'VERSION_CODE=90029' in PROPS)

def test_opening_never_uses_cached_scene():
    assert 'if(!openingRequest&&masterOrchestrator!=null&&activeMasterPlan!=null)' in MAIN

def test_opening_expands_instead_of_rejecting_short_text():
    assert 'BRAS_OPENING_COMPLETER BRAS_SINGLE_CALL' in MAIN
    assert '650 A 900 PALAVRAS EM 6 A 8 PARÁGRAFOS' in MAIN
    assert 'a abertura NUNCA é barrada por npc_updates' in MAIN
    assert 'Depois de uma tentativa de expansão, a cena útil SEMPRE começa' in MAIN

def test_npc_metadata_does_not_gate_scene():
    opening_block=MAIN[MAIN.index('private void acceptOrRepairNarration'):MAIN.index('private String textModelStatus')]
    assert 'npc_updates' in opening_block
    assert 'handleAiResponse(envelope.toString(),originalUserText);' in opening_block
    assert 'nunca podem travar a aventura' in opening_block

def test_portrait_requires_structured_identity_but_scene_does_not():
    assert 'hasStrongNpcVisualIdentity' in MAIN
    assert 'enrichNpcVisualIdentityThenGenerate' in MAIN
    assert 'ancestry_or_species' in MAIN
    assert 'gender_presentation' in MAIN
    assert 'apparent_age' in MAIN
    assert 'IDENTIDADE CANÔNICA — não omita nem altere estes dados' in MAIN
