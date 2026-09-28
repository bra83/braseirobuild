from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e"

def read(name):
    return (JAVA/name).read_text(encoding="utf-8")

def test_persona_is_runtime_not_decorative():
    director=read("BraseiroNarrativeDirector.java")
    profile=read("HumanGmNarrativeProfile.java")
    asset=ROOT/"android/app/src/main/assets/pathfinder/master/PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md"
    assert asset.exists() and asset.stat().st_size>20000
    assert "HumanGmNarrativeProfile.prompt()" in director
    for token in ["PIPELINE OBRIGATÓRIO","AGÊNCIA:","AUTORIDADE:","CICLO FUNDAMENTAL:","CÂMERA:","QA POR RESPOSTA:"]:
        assert token in profile

def test_character_gemini_image_has_dedicated_target():
    main=read("MainActivity.java")
    assert 'sharePromptToGeminiForCharacter' in main
    assert 'sharePromptToGeminiTarget(prompt,"character",characterId,"")' in main
    assert 'if("character".equals(target)){applyReceivedCharacterImage(stored);return;}' in main
    assert "PREF_PENDING_CHARACTER_ID" in main
    assert "db.setCharacterToken(characterId,portrait.toString())" in main
    # The character button must no longer call the scene helper.
    marker='Button gemini=button("DESCREVER PERSONAGEM PARA O GEMINI"'
    chunk=main[main.index(marker):main.index(marker)+900]
    assert "sharePromptToGeminiForCharacter" in chunk
    assert "sharePromptToGemini(PathfinderTokenPromptBuilder" not in chunk

def test_tts_tom_tags_unicode_regex_is_runtime_safe():
    playback=read("NarrationPlaybackPolicy.java")
    assert r'.replaceAll("\\\\p{M}+","")' not in playback
    assert r'.replaceAll("\\p{M}+","")' in playback

def test_android_kokoro_contract_preserved():
    main=read("MainActivity.java")
    for token in [
        "com.k2fsa.sherpa.onnx.tts.engine",
        "com.k2fsa.sherpa.onnx.tts.engine.debug",
        "Braseiro Kokoro PT-BR · neural",
        "availableNativeTtsVoices()",
        "nativeTts.setVoice(chosen)",
        "TextToSpeech.QUEUE_ADD",
        "TextToSpeech.QUEUE_FLUSH",
        "onDone"
    ]:
        assert token in main

def test_solo_multiple_characters_player_or_gm_uses_existing_profiles():
    db=read("AppDb.java")
    main=read("MainActivity.java")
    cognitive=read("MasterCognitiveEngine.java")
    assert "character_control_profiles" in db
    assert 'new String[]{"Jogador","Mestre"}' in main
    assert 'return "Mestre".equals(label)?"gm":"player";' in main
    assert "showCharacterControlChoice" in main
    assert "Controle PLAYER/Jogador" in cognitive
    assert "toda consequência mecânica passa pelo mesmo motor PF2e/Kingmaker" in cognitive

def test_kingdom_guide_reads_canonical_engine():
    main=read("MainActivity.java")
    guide=read("KingdomTurnGuideEngine.java")
    assert "new KingdomTurnGuideEngine(kingmakerRepository,kingmakerRules).snapshot()" in main
    assert "?  GUIA COMPLETO DO TURNO" in main
    assert "rules.activityCatalog()" in guide
    assert "repository.activeTurn()" in guide
    assert "repository.state()" in guide
    assert "rules.canPerformActivity" in guide
    assert "Este guia é somente leitura" in guide
