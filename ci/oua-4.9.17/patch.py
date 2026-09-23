from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4917.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

# OUA 4.9.17
# 1) Clear the local "Gemini criando..." latch as soon as a real currentNode exists,
#    even when the scene id is reused/updated instead of replaced.
# 2) Fix silent default audio: Number(null) is 0, so first-run voice volume was zero.

imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)

old_effect = """  useEffect(() => {
    if (!props.currentNode?.id) return;
    setChronicleOpen(true);
    setStartRequested(false);
  }, [props.currentNode?.id]);"""
new_effect = """  useEffect(() => {
    if (!props.currentNode) return;
    // A cena real já existe. Não deixe o latch local de abertura sobreviver
    // quando o backend atualiza/reaproveita o mesmo id de SceneNode.
    if (startRequested) setStartRequested(false);
    setChronicleOpen(true);
  }, [props.currentNode, startRequested]);"""
if old_effect in imm:
    imm = imm.replace(old_effect, new_effect, 1)
elif "if (startRequested) setStartRequested(false);" not in imm:
    raise SystemExit("ImmersiveJourneyView startRequested effect anchor missing")

old_overlay = "{(startRequested || (props.isStoryLoading && !node)) && <div aria-live=\"polite\""
new_overlay = "{((startRequested || props.isStoryLoading) && !node) && <div aria-live=\"polite\""
if old_overlay in imm:
    imm = imm.replace(old_overlay, new_overlay, 1)
elif new_overlay not in imm:
    raise SystemExit("Gemini creating overlay anchor missing")

old_action_gate = "{!chronicleOpen && !startRequested && <div style={{"
new_action_gate = "{!chronicleOpen && (!startRequested || Boolean(node)) && <div style={{"
if old_action_gate in imm:
    imm = imm.replace(old_action_gate, new_action_gate, 1)
elif new_action_gate not in imm:
    raise SystemExit("map action panel gate anchor missing")

old_button_label = "{props.isStoryLoading || startRequested ? 'Gemini criando a cena…' : node ? 'Abrir crônica' : 'Começar aventura'}"
new_button_label = "{!node && (props.isStoryLoading || startRequested) ? 'Gemini criando a cena…' : node ? 'Abrir crônica' : 'Começar aventura'}"
if old_button_label in imm:
    imm = imm.replace(old_button_label, new_button_label, 1)
elif new_button_label not in imm:
    raise SystemExit("start/open chronicle label anchor missing")

write(imm_rel, imm)

audio_rel = "lib/AudioEngine.ts"
audio = read(audio_rel)
old_volume = """function storedVolume(channel: AudioChannel): number {
  try {
    const value = Number(localStorage.getItem(`braseiro.audio.volume.${channel}`));
    return Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : DEFAULT_VOLUMES[channel];
  } catch {
    return DEFAULT_VOLUMES[channel];
  }
}"""
new_volume = """function storedVolume(channel: AudioChannel): number {
  try {
    const raw = localStorage.getItem(`braseiro.audio.volume.${channel}`);
    // Number(null) === 0. Sem este guard, uma instalação nova inicia todos os
    // canais — inclusive voice/Charon — silenciosos até o usuário mexer no volume.
    if (raw == null || raw.trim() === '') return DEFAULT_VOLUMES[channel];
    const value = Number(raw);
    return Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : DEFAULT_VOLUMES[channel];
  } catch {
    return DEFAULT_VOLUMES[channel];
  }
}"""
if old_volume in audio:
    audio = audio.replace(old_volume, new_volume, 1)
elif "if (raw == null || raw.trim() === '') return DEFAULT_VOLUMES[channel];" not in audio:
    raise SystemExit("AudioEngine storedVolume anchor missing")
write(audio_rel, audio)

# Version bump, preserving package id/database/signing contract.
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.17"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.16"', '"version": "4.9.17"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.16'", "APP_VERSION = '4.9.17'", version)
version = re.sub(r"APP_VERSION_CODE = 49016", "APP_VERSION_CODE = 49017", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49016", "versionCode = 49017", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.16"', 'versionName = "4.9.17"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49016&immersive=1", "index.html?v=49017&immersive=1")
write(main_rel, main)

# Hard gates: exact regressions + party preservation.
imm = read(imm_rel)
audio = read(audio_rel)
emap = read("components/EriadorMap.tsx")
svc = read("services/geminiService.ts")
main = read(main_rel)
checks = {
    "scene clears start latch even same id": "if (startRequested) setStartRequested(false);" in imm and "[props.currentNode, startRequested]" in imm,
    "creating overlay requires missing scene": "((startRequested || props.isStoryLoading) && !node)" in imm,
    "existing scene always exposes open chronicle": "!startRequested || Boolean(node)" in imm and "!node && (props.isStoryLoading || startRequested)" in imm,
    "audio missing key uses defaults": "raw == null || raw.trim() === ''" in audio and "return DEFAULT_VOLUMES[channel]" in audio,
    "voice default remains audible": "voice: 1" in audio,
    "gemini tts native bridge preserved": "generateTts" in svc and "playGeneratedVoice" in audio and "Charon" in svc,
    "party canonical world anchor preserved": 'data-party-world-anchored="true"' in emap and "mapX: hex.centerX, mapY: hex.centerY" in emap,
    "db identity preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts"),
    "db schema preserved": "const DB_VERSION = 19;" in read("lib/db.ts"),
    "version": "4.9.17" in read(version_rel) and "49017" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4917_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.17_SCENE_TTS_OK")
