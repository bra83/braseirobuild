from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
GEM=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java').read_text(encoding='utf-8')
MARK=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiTtsMarkupDirector.java').read_text(encoding='utf-8')
POL=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationPlaybackPolicy.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')

def test_identity_current():
    assert ('VERSION_NAME=9.0.31' in PROPS or 'VERSION_NAME=9.0.29' in PROPS)
    assert ('VERSION_CODE=90031' in PROPS or 'VERSION_CODE=90029' in PROPS)
    assert 'DB_SCHEMA=59' in PROPS and 'BACKUP_FORMAT=22' in PROPS

def test_25_primary_has_31_model_fallback():
    assert 'gemini-2.5-flash-preview-tts' in MAIN
    resolve=GEM[GEM.index('private List<String> resolveCandidates'):GEM.index('private static String canonicalTtsModel')]
    assert 'ordered.add("gemini-2.5-flash-preview-tts")' in resolve
    assert 'ordered.add("gemini-3.1-flash-tts-preview")' in resolve

def test_inline_tags_are_wired_into_real_runtime():
    assert 'GeminiTtsMarkupDirector.annotateForModel(model,sentence,delivery,intensity,pauseProfile,speaker)' in MAIN
    assert 'final String ttsText=sentence;' not in MAIN
    assert 'ttsText,ttsDirection,new GeminiService.FileCallback' in MAIN

def test_tags_cover_25_and_31_and_use_documented_english_forms():
    assert 'n.contains("gemini-2.5")||n.contains("gemini-3.1")' in MARK
    for tag in ['sighs','laughs','shouting','whispers','gasp','cough']:
        assert f'"{tag}"' in MARK
    for legacy in ['"sigh"','"laughing"','"whispering"','"extremely fast"','"short pause"','"medium pause"','"long pause"']:
        assert legacy not in MARK

def test_advanced_prompt_has_audio_profile_scene_notes_and_transcript():
    for label in ['# AUDIO PROFILE:','## THE SCENE','### DIRECTOR\'S NOTES','#### TRANSCRIPT']:
        assert label in POL
    assert 'Bracketed English audio tags inside the transcript are performance controls' in POL
    assert 'Read the entire transcript' in POL

def test_scene_audio_plan_and_npc_identity_reach_tts_direction():
    for field in ['delivery','intensity','pause_profile','environment','weather','location','time_of_day','music_mood']:
        assert f'out.put("{field}"' in MAIN
    for field in ['register_name','speaking_style','tempo','warmth','formality']:
        assert f'profile.optString("{field}"' in MAIN or f'profile.optDouble("{field}"' in MAIN or f'profile.optInt("{field}"' in MAIN

def test_cache_pipeline_bumped_for_directed_audio_semantics():
    assert 'charon-v19-r28-safe-arcs' in POL
    assert 'tts-v8' in (ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/TtsCache.java').read_text(encoding='utf-8')
