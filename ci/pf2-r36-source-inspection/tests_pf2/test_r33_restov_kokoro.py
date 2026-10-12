from pathlib import Path
import shutil
import subprocess
import textwrap

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"


def source(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def test_r33_version_and_restov_opening_contract():
    version = (ROOT / "version.properties").read_text(encoding="utf-8")
    main = source("MainActivity.java")
    solo = source("KingmakerSoloDirector.java")
    repo = source("KingmakerRepository.java")

    assert "VERSION_NAME=9.0.37" in version
    assert "VERSION_CODE=90037" in version
    assert "Restov, Rostland, Brevoy" in main
    assert "prepareKingmakerRestovStart" in main
    assert "A PRIMEIRA CENA DEVE MOSTRAR OS PERSONAGENS CHEGANDO A RESTOV" in source("KingmakerPreludePolicy.java")
    assert "5.000 a 6.000 caracteres" in main
    assert "campaign_story_phase" in repo
    assert "main_adventure_started" in repo
    assert "go_jamandi_mansion" in solo
    assert "Restov permanece sandbox" in solo
    assert "jogador decidir ir até lá" in solo
    assert "Começar no Atlas" not in solo
    assert "iniciar a aventura no posto comercial de Oleg." not in solo


def test_restov_prelude_is_persistent_and_does_not_teleport_to_oleg():
    main = source("MainActivity.java")
    policy = source("KingmakerPreludePolicy.java")
    repo = source("KingmakerRepository.java")

    assert "resetCampaignPrelude()" in repo
    assert "KingmakerPreludePolicy.isKnownPhase" in repo
    assert "PHASE_JAMANDI_APPROACH" in repo
    assert "ensurePreludeQuestState" in repo
    assert "âncora urbana; hexploração ainda não iniciada" in main
    assert 'db.saveMapTravelState(false,"H_OLEG","[]","Kingmaker — expedição às Terras Roubadas")' in main
    assert "H_OLEG é destino canônico, não teleporte" in main
    assert "NÃO transporte o grupo para a mansão" in policy
    assert "não teleporte" in policy.lower()


def test_mansion_trigger_requires_explicit_intent(tmp_path):
    javac = shutil.which("javac")
    java = shutil.which("java")
    assert javac and java, "JDK necessário para validar KingmakerPreludePolicy"

    pkg = tmp_path / "com/braseiro/pathfinder2e"
    pkg.mkdir(parents=True)
    shutil.copy2(JAVA / "KingmakerPreludePolicy.java", pkg / "KingmakerPreludePolicy.java")
    harness = pkg / "PreludeHarness.java"
    harness.write_text(textwrap.dedent("""
        package com.braseiro.pathfinder2e;
        public final class PreludeHarness {
            public static void main(String[] args) {
                if (KingmakerPreludePolicy.isMansionIntent("Quero saber mais sobre Lady Jamandi.")) throw new RuntimeException("menção virou gatilho");
                if (KingmakerPreludePolicy.isMansionIntent("Não vou à mansão de Lady Jamandi ainda.")) throw new RuntimeException("negação virou gatilho");
                if (!KingmakerPreludePolicy.isMansionIntent("Vamos até a mansão de Lady Jamandi.")) throw new RuntimeException("intenção explícita não detectada");
                if (!KingmakerPreludePolicy.isCharterAcceptanceIntent("Aceito a carta para explorar as Terras Roubadas.")) throw new RuntimeException("carta não detectada");
                if (!KingmakerPreludePolicy.isExpeditionDepartureIntent("Partimos de Restov rumo às Terras Roubadas.")) throw new RuntimeException("partida não detectada");
                if (!KingmakerPreludePolicy.isRestovPhase(KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX)) throw new RuntimeException("fase sandbox inválida");
                System.out.println("PRELUDE_PASS");
            }
        }
    """), encoding="utf-8")
    subprocess.run([javac, "-encoding", "UTF-8", "-d", str(tmp_path), str(pkg / "KingmakerPreludePolicy.java"), str(harness)], check=True)
    out = subprocess.check_output([java, "-cp", str(tmp_path), "com.braseiro.pathfinder2e.PreludeHarness"], text=True)
    assert "PRELUDE_PASS" in out


def test_kokoro_uses_forbidden_lands_two_layer_segmentation(tmp_path):
    chunker = source("NarrationChunker.java")
    main = source("MainActivity.java")

    assert "KOKORO_FIRST_CHAPTER_MAX = 320" in chunker
    assert "KOKORO_CHAPTER_MAX = 680" in chunker
    assert "KOKORO_ANDROID_MAX = 150" in chunker
    assert "KOKORO_ANDROID_MIN = 90" in chunker
    assert "kokoroChapters" in chunker
    assert "kokoroAndroidSegments" in chunker
    assert "com.k2fsa.sherpa.onnx.tts.engine" in main
    assert "com.k2fsa.sherpa.onnx.tts.engine.debug" in main
    assert "Braseiro Kokoro PT-BR · neural" in main
    assert "android.speech.tts.TextToSpeech" in main
    assert "model.onnx" not in main
    assert "voices.bin" not in main

    javac = shutil.which("javac")
    java = shutil.which("java")
    assert javac and java, "JDK necessário para validar NarrationChunker"

    pkg = tmp_path / "com/braseiro/pathfinder2e"
    pkg.mkdir(parents=True)
    shutil.copy2(JAVA / "NarrationChunker.java", pkg / "NarrationChunker.java")
    (pkg / "PortugueseNarrationSanitizer.java").write_text(textwrap.dedent("""
        package com.braseiro.pathfinder2e;
        public final class PortugueseNarrationSanitizer {
            private PortugueseNarrationSanitizer() {}
            public static String sanitize(String value) { return value == null ? "" : value; }
        }
    """), encoding="utf-8")
    (pkg / "ChunkHarness.java").write_text(textwrap.dedent("""
        package com.braseiro.pathfinder2e;
        import java.util.List;
        public final class ChunkHarness {
            private static String norm(String s) { return s.replaceAll("\\s+", " ").trim(); }
            public static void main(String[] args) {
                StringBuilder b = new StringBuilder();
                for (int i=0;i<55;i++) {
                    b.append("A estrada molhada atravessa os campos, e os viajantes continuam avançando sem interromper nenhuma palavra importante. ");
                    b.append("Ao longe, sinos marcam a passagem do tempo; carroças rangem, cavalos bufam e a cidade cresce diante deles. ");
                    b.append("Ninguém precisa parar a narração no primeiro bloco. ");
                }
                String text=b.toString().trim();
                List<String> chapters=NarrationChunker.kokoroChapters(text);
                if(chapters.size()<2)throw new RuntimeException("capítulos insuficientes");
                if(chapters.get(0).length()>NarrationChunker.KOKORO_FIRST_CHAPTER_MAX)throw new RuntimeException("primeiro capítulo grande");
                for(int i=1;i<chapters.size();i++)if(chapters.get(i).length()>NarrationChunker.KOKORO_CHAPTER_MAX)throw new RuntimeException("capítulo grande");
                List<String> parts=NarrationChunker.kokoro(text);
                if(parts.size()<10)throw new RuntimeException("fila curta");
                for(String part:parts){
                    if(part.length()>NarrationChunker.KOKORO_ANDROID_MAX)throw new RuntimeException("segmento Android grande: "+part.length());
                    if(part.length()==0)throw new RuntimeException("segmento vazio");
                }
                String rebuilt=norm(String.join(" ",parts));
                if(!rebuilt.equals(norm(text)))throw new RuntimeException("texto alterado durante segmentação");
                System.out.println("KOKORO_PASS "+chapters.size()+" "+parts.size());
            }
        }
    """), encoding="utf-8")

    subprocess.run([javac, "-encoding", "UTF-8", "-d", str(tmp_path),
                    str(pkg / "PortugueseNarrationSanitizer.java"),
                    str(pkg / "NarrationChunker.java"),
                    str(pkg / "ChunkHarness.java")], check=True)
    out = subprocess.check_output([java, "-cp", str(tmp_path), "com.braseiro.pathfinder2e.ChunkHarness"], text=True)
    assert "KOKORO_PASS" in out


def test_major_scene_length_is_in_canonical_human_gm_profile():
    profile = source("HumanGmNarrativeProfile.java")
    assert "5.000 a 6.000 caracteres" in profile
    assert "Não aplique essa extensão a cenas rotineiras" in profile
