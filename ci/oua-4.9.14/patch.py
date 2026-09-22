from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4914.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

# Party presentation correction only:
# - no footprint placeholder in immersive mode;
# - PartyCluster stays anchored to the canonical current hex at all times.

imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)
imm, changed = re.subn(
    r'partyMarkerMode=\{node\s*&&\s*props\.characters\.length\s*\?\s*"none"\s*:\s*"footprints"\}',
    'partyMarkerMode="none"',
    imm,
    count=1,
)
if changed != 1 and 'partyMarkerMode="none"' not in imm:
    raise SystemExit("immersive partyMarkerMode anchor missing")
write(imm_rel, imm)

map_rel = "components/EriadorMap.tsx"
emap = read(map_rel)

# Normalize the current-party projection regardless of whether the 4.9.9
# discovery fallback is already present in the baseline.
pattern = re.compile(
    r"  const currentPartyPresentation = useMemo\(\(\) => \{[\s\S]{0,750}?\n  \}, \[[^\n]+\]\);",
    re.M,
)
match = pattern.search(emap)
if not match:
    raise SystemExit("currentPartyPresentation block missing")

replacement = """  const currentPartyPresentation = useMemo(() => {
    const activeHexId = currentHexId || discovery?.currentHexId || null;
    if (!immersive || !activeHexId || !partyCharacters.length) return null;
    const hex = byId.get(activeHexId);
    if (!hex) return null;
    return { hexId: activeHexId, left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, currentHexId, discovery?.currentHexId, partyCharacters.length, byId, camera.x, camera.y, camera.scale]);"""
emap = emap[:match.start()] + replacement + emap[match.end():]

old_render = """        {immersive && currentPartyPresentation && (
          <PartyCluster
            characters={partyCharacters}
            manifest={immersiveAssets}
            position={{ left: currentPartyPresentation.left, top: currentPartyPresentation.top }}
            mapMarker
          />
        )}
"""
new_render = """        {immersive && currentPartyPresentation && (
          <div
            data-party-map-marker="true"
            data-party-current-hex={currentPartyPresentation.hexId}
            style={{ position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 45 }}
          >
            <PartyCluster
              characters={partyCharacters}
              manifest={immersiveAssets}
              position={{ left: currentPartyPresentation.left, top: currentPartyPresentation.top }}
              mapMarker
            />
          </div>
        )}
"""
if old_render not in emap:
    raise SystemExit("PartyCluster map render anchor missing")
emap = emap.replace(old_render, new_render, 1)
write(map_rel, emap)

pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.14"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.13"', '"version": "4.9.14"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.13'", "APP_VERSION = '4.9.14'", version)
version = re.sub(r"APP_VERSION_CODE = 49013", "APP_VERSION_CODE = 49014", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49013", "versionCode = 49014", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.13"', 'versionName = "4.9.14"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49013&immersive=1", "index.html?v=49014&immersive=1")
write(main_rel, main)

imm = read(imm_rel)
emap = read(map_rel)
checks = {
    "immersive footprints removed": 'partyMarkerMode="none"' in imm and 'partyMarkerMode={node && props.characters.length ? "none" : "footprints"}' not in imm,
    "party uses canonical current hex": "const activeHexId = currentHexId || discovery?.currentHexId || null;" in emap,
    "actual PartyCluster retained": "<PartyCluster" in emap and "mapMarker" in emap,
    "visual evidence hook": 'data-party-map-marker="true"' in emap and "data-party-current-hex" in emap,
    "version": "4.9.14" in read(version_rel) and "49014" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4914_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.14_PARTY_HEX_OK")
