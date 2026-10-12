from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text('utf-8')
OPEN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/AdventureOpeningPlanner.java').read_text('utf-8')
CHUNK=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/NarrationChunker.java').read_text('utf-8')
GEM=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java').read_text('utf-8')
CACHE=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/TtsCache.java').read_text('utf-8')

def test_opening_contract_is_playable_not_purple_craft_prose():
    assert 'primeira cena jogável' in MAIN.lower()
    assert 'NOS PRIMEIROS 80 A 120 TERMOS' in MAIN
    assert 'microdescrição técnica de ofício' in MAIN
    assert 'não transforme profissão, ferramenta, material ou rotina cotidiana em assunto principal' in MAIN.lower()
    assert 'no máximo UMA frase' in OPEN

def test_gross_craft_microdetail_has_targeted_hard_gate():
    assert 'a abertura ficou presa em microdescrição técnica de ofício/material' in MAIN
    for word in ['formao','espiga','ranhura','espessura','batente','travessa','lasca']:
        assert f'"{word}"' in MAIN

def test_tts_chunks_reduce_call_count_without_one_giant_scene_request():
    assert 'CHARON_SINGLE_BLOCK = 1700' in CHUNK
    assert 'CHARON_FIRST_BLOCK = 1300' in CHUNK
    assert 'CHARON_NEXT_BLOCK = 2200' in CHUNK

def test_tts_direction_is_part_of_cache_identity():
    assert 'cacheDirection=ttsDirection.toString()' in MAIN
    assert 'ttsCache.get(cacheModel,voice,ttsText,cacheDirection)' in MAIN
    assert 'ttsCache.store(cacheModel,voice,ttsText,cacheDirection,playable)' in MAIN
    assert 'directed+"|"+normalized' in CACHE

def test_service_builds_prompt_from_direction_and_strips_tags_for_duration_check():
    assert 'NarrationPlaybackPolicy.charonPrompt(text,direction)' in GEM
    assert 'GeminiTtsMarkupDirector.stripTags(text)' in GEM

def test_settings_test_exercises_tone_and_tags_not_plain_sentence():
    assert 'TESTAR CHARON — TOM + TAGS' in MAIN
    assert 'Testando Charon com direção de voz e tags invisíveis' in MAIN
    assert 'GeminiTtsMarkupDirector.annotateForModel(testTtsModel' in MAIN
