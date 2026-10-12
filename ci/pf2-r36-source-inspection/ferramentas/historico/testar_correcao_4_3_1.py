#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
java = root / "android/app/src/main/java/com/braseiro/pathfinder2e"
main = (java / "MainActivity.java").read_text("utf-8")
gemini = (java / "GeminiService.java").read_text("utf-8")
diagnostics = (java / "AudioDiagnosticsRepository.java").read_text("utf-8")
library = (java / "SmartAudioLibrary.java").read_text("utf-8")
sanitizer = (java / "PortugueseNarrationSanitizer.java").read_text("utf-8")
version = (root / "version.properties").read_text("utf-8")

checks = []

def check(label, condition):
    checks.append((label, bool(condition)))

check("versão 4.3.1", "VERSION_NAME=4.3.1" in version and "VERSION_CODE=430100" in version)

extract_start = gemini.index("private String extractText")
extract_end = gemini.index("private static String read", extract_start)
extract = gemini[extract_start:extract_end]
check("deltas preservam espaços", "return b.toString();" in extract and "return b.toString().trim()" not in extract)
check("streaming usa fusão incremental", "mergeStreamText(full.toString(),chunk)" in gemini)

event_start = diagnostics.index("public void event(final String sessionId")
event_end = diagnostics.index("private synchronized void writeEvent", event_start)
event = diagnostics[event_start:event_end]
check("diagnóstico enfileirado", "writer.execute" in event)
check("fsync fora do evento da interface", "getFD().sync()" not in event and "getFD().sync()" in diagnostics)
check("diagnóstico encerrável", "public void shutdown()" in diagnostics)

check("persistência de áudio dedicada", "private final ExecutorService persistence" in library)
check("sessão SQLite em segundo plano", "persistence.execute(task)" in library and library.count("persist(new Runnable") >= 4)
check("biblioteca de áudio encerrável", "public void shutdown()" in library and "persistence.shutdown()" in library)

speak_start = main.index("private void speakGemini")
speak_end = main.index("private String ttsCacheModelKey", speak_start)
speak = main[speak_start:speak_end]
native_start = main.index("private void speakNativeNarrationWithSfx")
native_end = main.index("private void speakNextNativeNarrationChunk", native_start)
native = main[native_start:native_end]
check("Charon não depende do catálogo", "ensureAudioEngineReadyOrQueue" not in speak and "if(engine==null)ui.post(beginVoice)" in speak)
check("Android não depende do catálogo", "ensureAudioEngineReadyOrQueue" not in native and "if(engine==null)ui.post(beginVoice)" in native)
check("PLAY não consulta mapa novamente", "NarrativeLocationNaturalizer.naturalize" not in speak and "NarrativeLocationNaturalizer.naturalize" not in native)
check("fallback Charon em dez segundos", "}},10000L);" in speak)
check("primeiro bloco Charon menor", "firstBlock?180:620" in main)

success_start = main.index("public void onSuccess(final File file,String mime)", native_end)
success_end = main.index("public void onError(final String error)", success_start)
success = main[success_start:success_end]
check("cache Charon antes da interface", success.index("ttsCache.store") < success.index("ui.post"))
check("cache falho não perde a voz", "O bloco será reproduzido sem cache" in success)

check("normalização NFC", "Normalizer.normalize(raw,Normalizer.Form.NFC)" in sanitizer)
check("parágrafos preservados", 'replaceAll("\\\\n{3,}","\\n\\n")' in sanitizer)
check("caracteres invisíveis removidos", 'replace("\\u200B","").replace("\\u2060","")' in sanitizer)
check("pontuação recompõe espaço", '([,.;:!?…])(?=[A-Za-zÀ-ÿ])' in sanitizer)

response_start = main.index("private void handleAiResponse")
response_end = main.index("private void showTransactionReviewDialog", response_start)
check("cena não inicia áudio automaticamente", "speakGemini(" not in main[response_start:response_end] and "speakNativeNarrationWithSfx(" not in main[response_start:response_end])
check("interface explica PLAY manual", "só começam quando você toca em PLAY" in main)
check("D&D e Mystara continuam isolados", 'book.startsWith("mystara_")' in main and "if(isMystaraWorld()!=sourceIsMystara)continue" in main)

failed = [label for label, ok in checks if not ok]
for label, ok in checks:
    print(("OK  " if ok else "FALHA  ") + label)
print(f"RESULTADO: {len(checks) - len(failed)}/{len(checks)} verificações aprovadas.")
if failed:
    sys.exit(1)
