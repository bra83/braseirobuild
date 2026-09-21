from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4912.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.12"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.11"', '"version": "4.9.12"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version = read("lib/version.ts")
version = re.sub(r"APP_VERSION = '4\.9\.11'", "APP_VERSION = '4.9.12'", version)
version = re.sub(r"APP_VERSION_CODE = 49011", "APP_VERSION_CODE = 49012", version)
write("lib/version.ts", version)

gradle = read("android/app/build.gradle.kts")
gradle = re.sub(r"versionCode\s*=\s*49011", "versionCode = 49012", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.11"', 'versionName = "4.9.12"', gradle)
write("android/app/build.gradle.kts", gradle)

main = read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt")
main = main.replace("index.html?v=49011&immersive=1", "index.html?v=49012&immersive=1")
write("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt", main)

imm = read("components/immersive/ImmersiveJourneyView.tsx")
checks = {
    "map primary": "data-braseiro-immersive-root" in imm and "pointerEvents: 'auto'" in imm,
    "bounded sheet": "data-braseiro-chronicle-sheet" in imm and "maxHeight: '58dvh'" in imm,
    "nav above sheet": "bottom: 0, zIndex: 100" in imm,
    "map closes": "if (tab === 'jogo') { setChronicleOpen(false); return; }" in imm,
    "image generator": "Gerar ilustração 16:9 no Gemini" in imm,
    "narration audio": "🔊 Ouvir narração" in imm and "■ Parar narração" in imm,
    "quick actions": "Ações rápidas" in imm,
    "npc list": "NPCs presentes" in imm,
    "party marker": 'partyMarkerMode="footprints"' in imm and "partyCharacters={props.characters}" in imm,
    "version": "4.9.12" in read("lib/version.ts") and "49012" in read("android/app/build.gradle.kts"),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4912_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.12_OK")
