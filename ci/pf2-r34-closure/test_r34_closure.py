from pathlib import Path
import json
import shutil
import subprocess
import textwrap

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
CHAPTERS = ROOT / "android/app/src/main/assets/pathfinder/kingmaker/chapters"


def source(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def test_r34_identity_and_baseline():
    version = (ROOT / "version.properties").read_text(encoding="utf-8")
    assert "VERSION_NAME=9.0.35" in version
    assert "VERSION_CODE=90035" in version
    main = source("MainActivity.java")
    assert "Restov, Rostland, Brevoy" in main
    assert "KingmakerPreludePolicy" in main
    assert "Braseiro Kokoro PT-BR · neural" in main


def test_restov_arrival_is_random_but_persistent():
    main = source("MainActivity.java")
    policy = source("KingmakerPreludePolicy.java")
    assert "ARRIVAL_MODES" in policy
    assert "CITY_PRESSURES" in policy
    assert "randomWeather" in policy
    assert "randomDaypart" in policy
    assert "kingmaker_restov_arrival_seed" in main
    assert "alreadyInRestov" in main
    assert "retoma o relógio persistido; nunca sorteia novamente clima/horário" in main
    assert '.put("map_grounded",true)' in main
    assert '.put("atlas_mode","urban_anchor")' in main
    assert "Kingmaker está no prólogo urbano de Restov" in main
    assert "New Stetven" not in main


def test_mansion_is_staged_and_cannot_jump_to_stolen_lands(tmp_path):
    policy = source("KingmakerPreludePolicy.java")
    required = [
        "PHASE_JAMANDI_APPROACH",
        "PHASE_JAMANDI_RECEPTION",
        "PHASE_JAMANDI_FEAST",
        "PHASE_JAMANDI_GUEST_QUARTERS",
        "PHASE_JAMANDI_ATTACK",
        "PHASE_JAMANDI_AFTER_ATTACK",
        "PHASE_JAMANDI_CHARTER",
        "PHASE_CHARTER_RECEIVED",
        "PHASE_STOLEN_LANDS_EXPEDITION",
    ]
    for key in required:
        assert key in policy

    javac = shutil.which("javac")
    java = shutil.which("java")
    assert javac and java, "JDK necessário"
    pkg = tmp_path / "com/braseiro/pathfinder2e"
    pkg.mkdir(parents=True)
    shutil.copy2(JAVA / "KingmakerPreludePolicy.java", pkg / "KingmakerPreludePolicy.java")
    harness = pkg / "R34PreludeHarness.java"
    harness.write_text(textwrap.dedent("""
        package com.braseiro.pathfinder2e;
        public final class R34PreludeHarness {
            private static void eq(String a,String b,String label){
                if(!a.equals(b))throw new RuntimeException(label+": "+a+" != "+b);
            }
            public static void main(String[] args){
                String p=KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH;
                eq(KingmakerPreludePolicy.nextMansionPhaseForAction(p,"Vamos partir para as Terras Roubadas."),
                   KingmakerPreludePolicy.PHASE_JAMANDI_APPROACH,"salto antes da recepção");
                p=KingmakerPreludePolicy.nextMansionPhaseForContinue(p);
                eq(p,KingmakerPreludePolicy.PHASE_JAMANDI_RECEPTION,"recepção");
                p=KingmakerPreludePolicy.nextMansionPhaseForContinue(p);
                eq(p,KingmakerPreludePolicy.PHASE_JAMANDI_FEAST,"banquete");
                p=KingmakerPreludePolicy.nextMansionPhaseForContinue(p);
                eq(p,KingmakerPreludePolicy.PHASE_JAMANDI_GUEST_QUARTERS,"aposentos");
                p=KingmakerPreludePolicy.nextMansionPhaseForContinue(p);
                eq(p,KingmakerPreludePolicy.PHASE_JAMANDI_ATTACK,"ataque");
                eq(KingmakerPreludePolicy.nextMansionPhaseForContinue(p),p,"ataque não pode auto-pular");
                eq(KingmakerPreludePolicy.nextMansionPhaseForAction(p,"Partimos para Oleg agora."),p,"ataque não pode pular");
                p=KingmakerPreludePolicy.PHASE_JAMANDI_AFTER_ATTACK;
                p=KingmakerPreludePolicy.nextMansionPhaseForContinue(p);
                eq(p,KingmakerPreludePolicy.PHASE_JAMANDI_CHARTER,"carta");
                eq(KingmakerPreludePolicy.nextMansionPhaseForAction(p,"Partimos para Oleg agora."),p,"carta exige aceite");
                p=KingmakerPreludePolicy.nextMansionPhaseForAction(p,"Aceitamos a carta e a missão.");
                eq(p,KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED,"aceite");
                eq(KingmakerPreludePolicy.nextMansionPhaseForAction(p,"Vamos comprar suprimentos."),p,"preparação livre");
                p=KingmakerPreludePolicy.nextMansionPhaseForAction(p,"Partimos de Restov rumo às Terras Roubadas.");
                eq(p,KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION,"partida explícita");
                if(KingmakerPreludePolicy.isMansionIntent("Não vamos à mansão de Lady Jamandi ainda."))
                    throw new RuntimeException("negação ativou mansão");
                if(!KingmakerPreludePolicy.isMansionIntent("Vamos até a mansão de Lady Jamandi."))
                    throw new RuntimeException("ida explícita não detectada");
                System.out.println("R34_PRELUDE_PASS");
            }
        }
    """), encoding="utf-8")
    subprocess.run([javac, "-encoding", "UTF-8", "-d", str(tmp_path),
                    str(pkg / "KingmakerPreludePolicy.java"), str(harness)], check=True)
    out = subprocess.check_output([java, "-cp", str(tmp_path),
                                   "com.braseiro.pathfinder2e.R34PreludeHarness"], text=True)
    assert "R34_PRELUDE_PASS" in out


def test_structured_prologue_and_campaign_prerequisites_are_sane():
    prelude_path = CHAPTERS / "chapter_00_call_for_heroes.json"
    assert prelude_path.exists()
    prelude = json.loads(prelude_path.read_text(encoding="utf-8"))
    assert prelude["chapter_number"] == 0
    assert prelude["quests"][0]["quest_key"] == "q_call_for_heroes"

    chapters = []
    quest_owner = {}
    for path in sorted(CHAPTERS.glob("chapter_*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        chapters.append((data.get("chapter_number", 999), path.name, data))
        for q in data.get("quests", []):
            key = q.get("quest_key") or q.get("questKey")
            assert key, f"quest sem chave em {path.name}"
            assert key not in quest_owner, f"quest duplicada {key}"
            quest_owner[key] = path.name
    numbers = sorted(n for n, _, _ in chapters)
    assert 0 in numbers
    assert all(n in numbers for n in range(1, 13))

    # Auditoria de integridade: pré-requisito não pode apontar para uma missão inexistente.
    dangling = []
    for _, path_name, data in chapters:
        for q in data.get("quests", []):
            for req in q.get("prerequisites", []):
                if req not in quest_owner:
                    dangling.append((path_name, q.get("quest_key"), req))
    assert not dangling, f"pré-requisitos órfãos: {dangling[:20]}"


def test_character_gemini_image_returns_to_character_not_scene():
    main = source("MainActivity.java")
    assert 'PREF_PENDING_CHARACTER_ID="pending_image_character_id_v1"' in main
    assert "sharePromptToGeminiForCharacter" in main
    assert 'sharePromptToGeminiTarget(prompt,"character",characterId,"")' in main
    assert 'if("character".equals(target)){applyReceivedCharacterImage(stored);return;}' in main
    assert 'ImageStorageManager.importImage(MainActivity.this,source,"hero_avatars"' in main
    assert "db.setCharacterToken(characterId,avatar.toString())" in main
    assert 'setStatus("Imagem salva exclusivamente como token de "+name+".")' in main

    # Os dois fluxos visuais de personagem devem marcar o destino character.
    assert 'pendingTokenCharacterId=String.valueOf(id);sharePromptToGeminiForCharacter(' in main
    assert 'sharePromptToGeminiForCharacter(PathfinderTokenPromptBuilder.build(merged' in main


def test_kokoro_matches_forbidden_lands_runtime_semantics():
    main = source("MainActivity.java")
    chunk = source("NarrationChunker.java")
    assert "KOKORO_FIRST_CHAPTER_MAX = 320" in chunk
    assert "KOKORO_CHAPTER_MAX = 680" in chunk
    assert "KOKORO_ANDROID_MAX = 150" in chunk
    assert "KOKORO_ANDROID_MIN = 90" in chunk
    assert "int queueMode=TextToSpeech.QUEUE_FLUSH;" in main
    assert "onDone(final String id)" in main
    assert "speakNextNativeNarrationChunk()" in main
    assert "if(neural)return; // inferência neural lenta não recebe watchdog/timeout" in main
    assert "nativeTts.setSpeechRate(nativeNeuralRate())" in main
    assert "nativeTts.stop()" in main
    assert "com.k2fsa.sherpa.onnx.tts.engine" in main
    assert "com.k2fsa.sherpa.onnx.tts.engine.debug" in main
    assert "model.onnx" not in main
    assert "voices.bin" not in main


def test_major_scene_contract_survives_r34():
    main = source("MainActivity.java")
    profile = source("HumanGmNarrativeProfile.java")
    assert "CENA PRINCIPAL KINGMAKER" in main
    assert "5.000 a 6.000 caracteres" in main
    assert "5.000 a 6.000 caracteres" in profile
    assert '!"combate".equals(mode)' in main


def test_restov_atlas_does_not_fake_stolen_lands_hex():
    main = source("MainActivity.java")
    assert 'putString("kingmaker_atlas_mode","restov_city")' in main
    assert "âncora urbana; hexploração ainda não iniciada" in main
    assert "sem inventar um hex das Terras Roubadas" in main
    assert 'putString("kingmaker_atlas_mode","stolen_lands")' in main
    assert 'db.saveMapTravelState(false,"H_OLEG"' in main
