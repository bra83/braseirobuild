#!/usr/bin/env python3
from pathlib import Path
import json
import re
import sys
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
java = root / "android/app/src/main/java/com/braseiro/pathfinder2e"
main = (java / "MainActivity.java").read_text("utf-8")
gemini = (java / "GeminiService.java").read_text("utf-8")
sanitizer = (java / "PortugueseNarrationSanitizer.java").read_text("utf-8")
naturalizer = (java / "NarrativeLocationNaturalizer.java").read_text("utf-8")
appdb = (java / "AppDb.java").read_text("utf-8")
receiver = (java / "ShareReceiverActivity.java").read_text("utf-8")
manifest_path = root / "android/app/src/main/AndroidManifest.xml"
manifest = manifest_path.read_text("utf-8")
version = (root / "version.properties").read_text("utf-8")

checks = []
def check(label, condition): checks.append((label, bool(condition)))

version_values=dict(line.split("=",1) for line in version.splitlines() if "=" in line)
version_tuple=tuple(int(part) for part in version_values.get("VERSION_NAME","0.0.0").split(".")[:3])
check("versão 4.3.3+ preservada", version_tuple >= (4,3,3) and int(version_values.get("VERSION_CODE","0")) >= 430300)
check("streaming usa montagem com fronteira lexical", "PortugueseNarrationSanitizer.mergeStreamingText(current,chunk)" in gemini)
for glued in ("vastaextensão", "trazendoconsigo", "madeiravindo", "viajantescostumam", "novasoportunidades", "deaco", "boaposição", "terrabatida", "desfrutadeste"):
    check("correção explícita: " + glued, glued in sanitizer)
check("segmentação geral de duas a quatro palavras", "bestSegmentation" in sanitizer and "paths[i].size()>=4" in sanitizer)
check("histórico também é higienizado", "PortugueseNarrationSanitizer.sanitize(sanitizeNarrationPayload(stored))" in main)

check("prompt proíbe hex na prosa", main.count("nunca diga hex, hexágono, célula, grade") >= 2)
check("prompt recebe referência narrativa local", main.count("NarrativeLocationNaturalizer.currentNarrativeLabel(db)") >= 2)
check("naturalizador trata menções genéricas", "GENERIC_AT_CURRENT" in naturalizer and "GENERIC_BARE" in naturalizer)
check("fallback cartográfico não expõe código", '"a região indicada"' in naturalizer and '"nesta região"' in naturalizer)
check("prioridade para local, nota, região e terreno", all(token in naturalizer for token in ("confirmedLocationName", "getMapHexNote", "politicalLabel", "reliableTerrainLabel")))

speak = main[main.index("private void speakGemini"):main.index("private String ttsCacheModelKey")]
chunks = (java / "NarrationChunker.java").read_text("utf-8")
native = main[main.index("private void speakNativeNarrationWithSfx"):main.index("private void speakNextNativeNarrationChunk")]
check("Charon aquece fallback Android", "ensureNativeTtsInitialized" in speak)
check("Charon não aguarda catálogo", "prepareSceneAsync" in speak and ",null);" in speak and "ui.post(new Runnable" in speak)
check("Android não aguarda catálogo", "prepareSceneAsync" in native and ",null);" in native and "ui.post(new Runnable" in native)
policy = (java / "NarrationPlaybackPolicy.java").read_text("utf-8")
check("cena normal usa uma só síntese Charon", "CHARON_SINGLE_BLOCK" in chunks and "out.add(clean)" in chunks)
check("cache novo não reaproveita prosódia antiga", ("charon-v11-primeira-fala-rapida" in policy or "charon-v10-prioridade-rede" in policy) and "tts-v7" in (java / "TtsCache.java").read_text("utf-8"))
check("instrução TTS exige timbre estável", all(token in policy for token in ("mesmo timbre", "mesmo", "reiniciar a")))
check("timeout de rede TTS limitado", "CHARON_READ_TIMEOUT_MS = 60000" in policy and "CHARON_RECOVERABLE_RETRIES = 1" in policy)
check("requisição TTS é cancelável", "class SpeechRequest" in gemini and "request.bind" in gemini and "activeCharonRequest" in main)

check("receptor de compartilhamento dedicado", "class ShareReceiverActivity" in receiver)
check("receptor devolve para tarefa original", all(token in receiver for token in ("FLAG_ACTIVITY_NEW_TASK", "FLAG_ACTIVITY_CLEAR_TOP", "FLAG_ACTIVITY_SINGLE_TOP")))
pending = (java / "PendingImageImportStore.java").read_text("utf-8")
check("receptor copia antes de perder a permissão", "ImageStorageManager.importImage" in receiver and "PendingImageImportStore.enqueue" in receiver)
check("fila de imagem sobrevive ao processo", "SharedPreferences" in pending and "claim" in pending and "ACTION_CONSUME" in pending)
check("MainActivity não é mais alvo ACTION_SEND", manifest.count('android.intent.action.SEND') == 1 and "ShareReceiverActivity" in manifest)
check("receptor não aparece nos recentes", 'android:excludeFromRecents="true"' in manifest and 'android:noHistory="true"' in manifest)
check("imagem recebida abre Mestre IA", 'navigate("Mestre IA")' in main[main.index("private void handleIncomingIntent"):main.index("private void displaySceneImage")])
check("intent recebido é consumido", "intent.removeExtra(Intent.EXTRA_STREAM)" in main and "intent.setClipData(null)" in main)

check("mapa diferencia catálogo de posicionamento", "countMapCatalogLocations" in appdb and "countMapPositionedLocations" in appdb)
check("interface não chama catálogo de vinculado", "locais catalogados / " in main and "locais vinculados" not in main)

zip_path = root / "android/app/src/main/assets/maps/faerun_maxres_integrated.zip"
try:
    with zipfile.ZipFile(zip_path) as archive:
        locations = json.load(archive.open("faerun_5e_locations_v2.json"))["locations"]
        index = json.load(archive.open("98_indice_geral.json"))
        atlas = json.load(archive.open("faerun_5e_hex_atlas_v2.json"))["hexes"]
        positioned = sum(1 for item in locations if item.get("hexId") or item.get("hex_id") or item.get("placement"))
        linked = sum(1 for item in atlas.values() if isinstance(item, dict) and item.get("locationIds"))
        check("catálogo Faerûn preservado com 351 verbetes", len(locations) == 351)
        check("índice de categorias preserva 397 registros", index.get("total_registros_base_pronta") == 397)
        check("diagnóstico honesto: catálogo ainda não georreferenciado", positioned == 0 and linked == 0)
except Exception:
    check("ZIP Faerûn legível", False)

try:
    ET.parse(manifest_path)
    check("Manifest XML válido", True)
except Exception:
    check("Manifest XML válido", False)

failed = [label for label, ok in checks if not ok]
for label, ok in checks: print(("OK  " if ok else "FALHA  ") + label)
print(f"RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.")
if failed: sys.exit(1)
