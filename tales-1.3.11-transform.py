from pathlib import Path
import re
import shutil
import sys

root = Path(sys.argv[1])
repo = Path(__file__).resolve().parent

# Preserve every proven correction through 1.3.10 first.
ns = {"__name__": "__main__", "__file__": str(repo / "tales-1.3.10-transform.py")}
code = (repo / "tales-1.3.10-transform.py").read_text(encoding="utf-8")
old_argv = sys.argv[:]
sys.argv = [ns["__file__"], str(root)]
try:
    exec(compile(code, ns["__file__"], "exec"), ns)
finally:
    sys.argv = old_argv

# Version 1.3.11 / versionCode 15.
gradle = root / "app" / "build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
g = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 15', g, count=1)
g = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.3.11"', g, count=1)
if 'testImplementation("junit:junit:4.13.2")' not in g:
    g = g.replace('dependencies {', 'dependencies {\n  testImplementation("junit:junit:4.13.2")', 1)
gradle.write_text(g, encoding="utf-8")

# Replace only the two UI/runtime files needed by this block. Approved screen geometry is preserved.
overlay = repo / "tales-1.3.11-overlay"
for rel in [
    "app/src/main/java/com/braseiro/talesvtt/screens/MapScreen.kt",
    "app/src/main/java/com/braseiro/talesvtt/components/NarrationTts.kt",
]:
    src = overlay / Path(rel).name
    dst = root / rel
    if not src.is_file():
        raise SystemExit(f"Missing overlay: {src}")
    shutil.copy2(src, dst)

# Persist an ordered travel history in the authoritative public game state.
tales = root / "app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt"
t = tales.read_text(encoding="utf-8")
anchor = '  val revealed: RevealedState = RevealedState(),\n  val extended: ExtendedTroubleState = ExtendedTroubleState(),'
replacement = '  val revealed: RevealedState = RevealedState(),\n  val routeHistory: List<String> = emptyList(),\n  val extended: ExtendedTroubleState = ExtendedTroubleState(),'
if anchor not in t:
    raise SystemExit("GameState routeHistory anchor not found")
t = t.replace(anchor, replacement, 1)

old_patch = '''      is GameEvent.GmWorldPatch -> {
        val nextLocation = event.location?.takeIf { it.isNotBlank() } ?: state.scene.location
        val nextVisual = event.visualAssetKey?.takeIf { it.isNotBlank() } ?: state.scene.visualAssetKey
        val changedScene = nextLocation != state.scene.location || nextVisual != state.scene.visualAssetKey
        ok(state.copy(scene = state.scene.copy(
          location = nextLocation,
          weather = event.weather?.takeIf { it.isNotBlank() } ?: state.scene.weather,
          time = event.time?.takeIf { it.isNotBlank() } ?: state.scene.time,
          daylight = event.daylight?.takeIf { it.isNotBlank() } ?: state.scene.daylight,
          visualAssetKey = nextVisual,
          importedImageUri = if (changedScene) null else state.scene.importedImageUri
        )))
      }
'''
new_patch = '''      is GameEvent.GmWorldPatch -> {
        val nextLocation = event.location?.takeIf { it.isNotBlank() } ?: state.scene.location
        val nextVisual = event.visualAssetKey?.takeIf { it.isNotBlank() } ?: state.scene.visualAssetKey
        val locationChanged = !nextLocation.equals(state.scene.location, ignoreCase = true)
        val nextRouteHistory = if (locationChanged) {
          val base = state.routeHistory.ifEmpty { listOf(state.scene.location) }
          (base + nextLocation)
            .fold(mutableListOf<String>()) { acc, location ->
              if (acc.lastOrNull()?.equals(location, ignoreCase = true) != true) acc += location
              acc
            }
            .takeLast(24)
        } else {
          state.routeHistory.ifEmpty { listOf(state.scene.location) }
        }
        val changedScene = locationChanged || nextVisual != state.scene.visualAssetKey
        ok(state.copy(
          scene = state.scene.copy(
            location = nextLocation,
            weather = event.weather?.takeIf { it.isNotBlank() } ?: state.scene.weather,
            time = event.time?.takeIf { it.isNotBlank() } ?: state.scene.time,
            daylight = event.daylight?.takeIf { it.isNotBlank() } ?: state.scene.daylight,
            visualAssetKey = nextVisual,
            importedImageUri = if (changedScene) null else state.scene.importedImageUri
          ),
          routeHistory = nextRouteHistory
        ))
      }
'''
if old_patch not in t:
    raise SystemExit("GmWorldPatch block not found")
t = t.replace(old_patch, new_patch, 1)
tales.write_text(t, encoding="utf-8")

# New adventures start a fresh route from their actual first location.
vm = root / "app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt"
v = vm.read_text(encoding="utf-8")
old = '      revealed = RevealedState(clues = emptySet(), locations = setOf(seed.location), connections = emptySet())\n    )'
new = '      revealed = RevealedState(clues = emptySet(), locations = setOf(seed.location), connections = emptySet()),\n      routeHistory = listOf(seed.location)\n    )'
if old not in v:
    raise SystemExit("startRandomAdventure route anchor not found")
v = v.replace(old, new, 1)
vm.write_text(v, encoding="utf-8")

# Snapshot v9 persists route history and migrates old saves to the current location.
codec = root / "app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt"
c = codec.read_text(encoding="utf-8")
c = c.replace('private const val VERSION = 8', 'private const val VERSION = 9', 1)
enc_anchor = '    line("reveal.locations", state.revealed.locations.joinToString("|") { enc(it) })\n    line("reveal.connections", state.revealed.connections.joinToString("|") { (a, b) -> "${enc(a)}~${enc(b)}" })'
enc_new = '    line("reveal.locations", state.revealed.locations.joinToString("|") { enc(it) })\n    line("route.history", state.routeHistory.joinToString("|") { enc(it) })\n    line("reveal.connections", state.revealed.connections.joinToString("|") { (a, b) -> "${enc(a)}~${enc(b)}" })'
if enc_anchor not in c:
    raise SystemExit("snapshot encode route anchor not found")
c = c.replace(enc_anchor, enc_new, 1)

dec_anchor = '''    val revealed = RevealedState(
      clues = decodeSet("reveal.clues"),
      locations = decodeSet("reveal.locations"),
      connections = connections
    )

    val scenes = map["ext.scenes"].orEmpty().split('|')'''
dec_new = '''    val revealed = RevealedState(
      clues = decodeSet("reveal.clues"),
      locations = decodeSet("reveal.locations"),
      connections = connections
    )
    val routeHistory = if (version >= 9) {
      map["route.history"].orEmpty().split('|').filter { it.isNotBlank() }.map(::dec).ifEmpty { listOf(scene.location) }
    } else listOf(scene.location)

    val scenes = map["ext.scenes"].orEmpty().split('|')'''
if dec_anchor not in c:
    raise SystemExit("snapshot decode route anchor not found")
c = c.replace(dec_anchor, dec_new, 1)

ctor_anchor = '''      testResolution = result,
      revealed = revealed,
      extended = extended,'''
ctor_new = '''      testResolution = result,
      revealed = revealed,
      routeHistory = routeHistory,
      extended = extended,'''
if ctor_anchor not in c:
    raise SystemExit("snapshot GameState constructor route anchor not found")
c = c.replace(ctor_anchor, ctor_new, 1)
codec.write_text(c, encoding="utf-8")

# Make Gemini playback use media audio, fail faster, and allow the Android fallback to take over.
gem = root / "app/src/main/java/com/braseiro/talesvtt/audio/GeminiNarrationTts.kt"
a = gem.read_text(encoding="utf-8")
if 'import android.media.AudioAttributes' not in a:
    a = a.replace('import android.media.MediaPlayer', 'import android.media.AudioAttributes\nimport android.media.MediaPlayer', 1)
a = a.replace('.connectTimeout(30, TimeUnit.SECONDS)\n    .readTimeout(90, TimeUnit.SECONDS)', '.connectTimeout(15, TimeUnit.SECONDS)\n    .readTimeout(25, TimeUnit.SECONDS)\n    .callTimeout(30, TimeUnit.SECONDS)', 1)
media_anchor = '''      player = MediaPlayer().apply {
        setDataSource(file.absolutePath)'''
media_new = '''      player = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        setDataSource(file.absolutePath)'''
if media_anchor not in a:
    raise SystemExit("Gemini MediaPlayer anchor not found")
a = a.replace(media_anchor, media_new, 1)
a = a.replace('        prepare()\n        start()\n      }', '        prepare()\n        start()\n        check(isPlaying) { "A reprodução da voz Gemini não iniciou." }\n      }', 1)
gem.write_text(a, encoding="utf-8")

# Real unit tests for route persistence and long-narration chunking.
test_dir = root / "app/src/test/java/com/braseiro/talesvtt"
(test_dir / "game").mkdir(parents=True, exist_ok=True)
(test_dir / "components").mkdir(parents=True, exist_ok=True)
(test_dir / "game/RouteHistoryTest.kt").write_text(r'''package com.braseiro.talesvtt.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RouteHistoryTest {
  @Test
  fun worldPatchRecordsOnlyRealLocationChanges() {
    val start = GameState(
      scene = SceneState(location = "Escola Björkskolan"),
      routeHistory = listOf("Escola Björkskolan")
    )
    val moved = TalesReducer.reduce(start, GameEvent.GmWorldPatch(location = "Praça Central"))
    assertEquals(listOf("Escola Björkskolan", "Praça Central"), moved.routeHistory)

    val weatherOnly = TalesReducer.reduce(moved, GameEvent.GmWorldPatch(weather = "Neve leve"))
    assertEquals(moved.routeHistory, weatherOnly.routeHistory)
  }

  @Test
  fun snapshotRoundTripPreservesRoute() {
    val state = GameState(
      scene = SceneState(location = "Loop Industries"),
      routeHistory = listOf("Escola Björkskolan", "Praça Central", "Loop Industries")
    )
    val decoded = GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))
    assertNotNull(decoded)
    assertEquals(state.routeHistory, decoded!!.routeHistory)
  }
}
''', encoding="utf-8")

(test_dir / "components/NarrationTextChunkerTest.kt").write_text(r'''package com.braseiro.talesvtt.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationTextChunkerTest {
  @Test
  fun longNarrativeIsSplitWithoutOversizedChunks() {
    val text = List(900) { "chuva vento bicicletas escola mistério" }.joinToString(" ") + "."
    val chunks = NarrationTextChunker.split(text, 1000)
    assertTrue(chunks.size > 1)
    assertTrue(chunks.all { it.length <= 1000 })
    assertEquals(text.replace(Regex("\\s+"), " ").trim(), chunks.joinToString(" ").replace(Regex("\\s+"), " ").trim())
  }
}
''', encoding="utf-8")

# Update resume inside the generated source.
resume = root / "TALES_RETOMADA_CURRENT.md"
r = resume.read_text(encoding="utf-8") if resume.exists() else "# TALES FROM THE LOOP — RETOMADA ATUAL\n"
r += '''\n## Bloco 1.3.11\n- MAPA: adicionada camada dinâmica de rota percorrida. A rota cresce somente quando o Mestre muda o local real da cena, acompanha o progresso da aventura e é salva/restaurada com a campanha.\n- TTS: voz Gemini usa áudio de mídia e timeout menor; em falha cai para TTS Android. Narrativas longas são divididas em trechos seguros em vez de falharem silenciosamente.\n- TTS Android tenta pt-BR, pt-PT, idioma do aparelho e fallback final; informa quando a voz não pôde iniciar.\n- Correções e ícone da 1.3.10 preservados.\n'''
resume.write_text(r, encoding="utf-8")

# Hard guards.
assert 'versionName = "1.3.11"' in gradle.read_text(encoding="utf-8")
assert 'val routeHistory: List<String>' in tales.read_text(encoding="utf-8")
assert 'line("route.history"' in codec.read_text(encoding="utf-8")
assert 'routeHistory = listOf(seed.location)' in vm.read_text(encoding="utf-8")
assert 'MapTravelRouteOverlay' in (root / "app/src/main/java/com/braseiro/talesvtt/screens/MapScreen.kt").read_text(encoding="utf-8")
assert 'NarrationTextChunker' in (root / "app/src/main/java/com/braseiro/talesvtt/components/NarrationTts.kt").read_text(encoding="utf-8")
assert '.callTimeout(30, TimeUnit.SECONDS)' in gem.read_text(encoding="utf-8")
print("TALES 1.3.11 transform: OK")
