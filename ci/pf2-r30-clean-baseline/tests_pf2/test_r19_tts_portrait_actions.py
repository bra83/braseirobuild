from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
GEM=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java').read_text(encoding='utf-8')
MARK=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiTtsMarkupDirector.java').read_text(encoding='utf-8')
OPT=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/SceneOptionDiversifier.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_r19_identity():
    assert ('VERSION_NAME=9.0.31' in PROPS or 'VERSION_NAME=9.0.29' in PROPS)
    assert ('VERSION_CODE=90031' in PROPS or 'VERSION_CODE=90029' in PROPS)
    assert 'DB_SCHEMA=59' in PROPS and 'BACKUP_FORMAT=22' in PROPS

def test_tts_25_primary_and_safe_markup():
    assert 'gemini-2.5-flash-preview-tts' in GEM
    assert 'canonicalTtsModel' in GEM
    assert 'GeminiTtsMarkupDirector.annotateForModel(model,sentence,delivery,intensity,pauseProfile,speaker)' in MAIN
    assert 'tts_android_fallback_enabled' in MAIN
    assert 'whispers' in MARK and 'gasp' in MARK
    assert 'shouting' in MARK and 'sighs' in MARK

def test_npc_portrait_is_stable_and_individual():
    for token in ['handleSceneNpcPortrait','buildNpcPortraitPrompt','generateNpcPortrait','applyNpcPortraitToScene','showNpcPortraitFullscreen','sharePromptToGeminiForNpc']:
        assert token in MAIN
    assert 'do peito para cima' in MAIN
    assert 'APENAS esta pessoa' in MAIN
    assert 'db.setNpcToken' in MAIN
    assert 'npc_'+ '"+npcId+"_portrait' in MAIN or 'npc_"+npcId+"_portrait' in MAIN

def test_generic_bad_choices_removed():
    assert 'TESTAR A CONSEQUÊNCIA DA AÇÃO' not in OPT
    assert 'qual mudança acabou de perceber' not in OPT
    assert 'firstNamedNpc(scene)' in OPT
    assert 'OBSERVAR A REAÇÃO DE ' in OPT
