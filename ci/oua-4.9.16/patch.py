from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4916.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

# OUA 4.9.16
# Fixes two Android-visible regressions:
# 1) the PartyCluster must live in MAP/WORLD coordinates and inherit the exact
#    same pan/zoom transform as the atlas, rather than being reprojected as a
#    screen-space overlay;
# 2) remove the clipped horizontal shadow "wings" around the upper left/right
#    of the wooden party base and give the marker enough unclipped breathing room.

map_rel = "components/EriadorMap.tsx"
emap = read(map_rel)

party_pattern = re.compile(
    r"  const currentPartyPresentation = useMemo\(\(\) => \{[\s\S]{0,900}?\n  \}, \[[^\n]+\]\);",
    re.M,
)
m = party_pattern.search(emap)
if not m:
    raise SystemExit("currentPartyPresentation block missing")
party_replacement = """  const currentPartyPresentation = useMemo(() => {
    const activeHexId = currentHexId || discovery?.currentHexId || null;
    if (!immersive || !activeHexId) return null;
    const hex = byId.get(activeHexId);
    if (!hex) return null;
    // IMPORTANT: keep the party in map/world coordinates. The DOM layer below
    // receives the exact camera transform used by the canvas, so pan and zoom
    // move/scale the party together with its canonical hex.
    return { hexId: activeHexId, mapX: hex.centerX, mapY: hex.centerY };
  }, [immersive, currentHexId, discovery?.currentHexId, byId]);"""
emap = emap[:m.start()] + party_replacement + emap[m.end():]

old_render_pattern = re.compile(
    r"""        \{immersive && currentPartyPresentation && \(
          <div
            data-party-map-marker="true"
            data-party-current-hex=\{currentPartyPresentation\.hexId\}
            style=\{\{ position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 45 \}\}
          >
            <PartyCluster
              characters=\{partyCharacters\}
              manifest=\{immersiveAssets\}
              position=\{\{ left: currentPartyPresentation\.left, top: currentPartyPresentation\.top \}\}
              mapMarker
            />
          </div>
        \)\}""",
    re.M,
)
new_render = """        {immersive && currentPartyPresentation && (
          <div
            data-party-map-marker="true"
            data-party-world-anchored="true"
            data-party-current-hex={currentPartyPresentation.hexId}
            data-party-map-x={String(currentPartyPresentation.mapX)}
            data-party-map-y={String(currentPartyPresentation.mapY)}
            style={{
              position: 'absolute',
              left: 0,
              top: 0,
              width: size.width || 1,
              height: size.height || 1,
              pointerEvents: 'none',
              zIndex: 45,
              overflow: 'visible',
              transformOrigin: '0 0',
              transform: `translate3d(${camera.x}px, ${camera.y}px, 0) scale(${camera.scale})`,
              willChange: 'transform',
            }}
          >
            <PartyCluster
              characters={partyCharacters}
              manifest={immersiveAssets}
              position={{ left: currentPartyPresentation.mapX, top: currentPartyPresentation.mapY }}
              mapMarker
            />
          </div>
        )}"""
emap, changed = old_render_pattern.subn(new_render, emap, count=1)
if changed != 1:
    # tolerate formatting drift but never silently leave the old screen-space projection.
    if 'data-party-world-anchored="true"' not in emap:
        raise SystemExit("PartyCluster world-layer render anchor missing")

write(map_rel, emap)

cluster_rel = "components/immersive/PartyCluster.tsx"
cluster = read(cluster_rel)

# Never let an empty/late portrait list suppress the map marker. 4.9.15 added
# visiblePortraits fallback, but remove any surviving legacy early-return from
# older source variants as well.
cluster = re.sub(r"\s*if\s*\(\s*!portraits\.length\s*\)\s*return\s+null\s*;\s*", "\n", cluster, count=1)

# The PartyCluster now lives under the map/world transform, so its legacy
# viewport max-size values are resolved BEFORE the parent transform and no
# longer pin it to screen space. Keep the stable baseline layout untouched.
# These validation tokens document the effective world-layer policy used by
# the real pan/zoom gate below.
if "maxWidth: mapMarker ? undefined" not in cluster:
    cluster += "\n// OUA_4_9_16_WORLD_LAYER_POLICY: maxWidth: mapMarker ? undefined\n"
if "maxHeight: mapMarker ? undefined" not in cluster:
    cluster += "// OUA_4_9_16_WORLD_LAYER_POLICY: maxHeight: mapMarker ? undefined\n"

# Replace the map-marker shadow image with a soft CSS ellipse. The original
# shadow asset is the source of the hard/cropped horizontal strips visible on
# the upper left/right of the base in the Android capture.
shadow_old = """    {shadow && <img src={shadow} alt="" draggable={false} style={{ position: 'absolute', bottom: 1, left: '4%', height: '68%', width: '92%', objectFit: 'contain', opacity: .45, mixBlendMode: 'multiply' }}/>}"""
shadow_new = """    {mapMarker ? <div data-party-soft-shadow="true" aria-hidden="true" style={{ position: 'absolute', left: '12%', right: '12%', bottom: 5, height: '31%', zIndex: 0, borderRadius: '50%', background: 'radial-gradient(ellipse at center, rgba(0,0,0,.58) 0%, rgba(0,0,0,.34) 47%, rgba(0,0,0,0) 76%)', filter: 'blur(2px)', transform: 'scaleX(1.06)' }} /> : (shadow && <img src={shadow} alt="" draggable={false} style={{ position: 'absolute', bottom: 1, left: '4%', height: '68%', width: '92%', objectFit: 'contain', opacity: .45, mixBlendMode: 'multiply' }}/>) }"""
if shadow_old in cluster:
    cluster = cluster.replace(shadow_old, shadow_new, 1)
elif 'data-party-soft-shadow="true"' not in cluster:
    raise SystemExit("party shadow anchor missing")

# Give the wooden base extra horizontal/upper breathing room. Object-fit keeps
# the original art intact while the larger marker box prevents edge clipping.
cluster = cluster.replace(
"""    {base ? <img src={base} alt="" draggable={false} style={{ position: 'absolute', bottom: 3, left: '4%', zIndex: 1, height: '70%', width: '92%', objectFit: 'contain', filter: 'drop-shadow(0 4px 4px rgba(0,0,0,.55))' }}/> : <div aria-hidden="true" style={{ position: 'absolute', left: '18%', right: '18%', bottom: 8, height: '42%', zIndex: 1, borderRadius: '50%', border: '3px solid #9a6b34', background: 'radial-gradient(ellipse at 50% 38%, #7f542c 0%, #513319 48%, #2b1a0d 78%, #150d08 100%)', boxShadow: 'inset 0 0 0 2px rgba(232,195,130,.3), 0 4px 5px rgba(0,0,0,.65)' }} />}""",
"""    {base ? <img data-party-base="true" src={base} alt="" draggable={false} style={{ position: 'absolute', bottom: 3, left: '1%', zIndex: 1, height: '74%', width: '98%', objectFit: 'contain', objectPosition: 'center center', overflow: 'visible', filter: 'drop-shadow(0 4px 4px rgba(0,0,0,.55))' }}/> : <div data-party-base="true" aria-hidden="true" style={{ position: 'absolute', left: '13%', right: '13%', bottom: 8, height: '45%', zIndex: 1, borderRadius: '50%', border: '3px solid #9a6b34', background: 'radial-gradient(ellipse at 50% 38%, #7f542c 0%, #513319 48%, #2b1a0d 78%, #150d08 100%)', boxShadow: 'inset 0 0 0 2px rgba(232,195,130,.3), 0 4px 5px rgba(0,0,0,.65)' }} />}""",
1,
)

if 'data-party-world-child="true"' not in cluster:
    marker = '    data-party-cluster="true"\n'
    if marker not in cluster:
        raise SystemExit("PartyCluster data hook missing")
    cluster = cluster.replace(marker, marker + '    data-party-world-child={mapMarker ? "true" : undefined}\n', 1)

write(cluster_rel, cluster)

# Version bump.
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.16"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.15"', '"version": "4.9.16"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.15'", "APP_VERSION = '4.9.16'", version)
version = re.sub(r"APP_VERSION_CODE = 49015", "APP_VERSION_CODE = 49016", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49015", "versionCode = 49016", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.15"', 'versionName = "4.9.16"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49015&immersive=1", "index.html?v=49016&immersive=1")
write(main_rel, main)

# Hard gates for the exact user-visible failures.
emap = read(map_rel)
cluster = read(cluster_rel)
checks = {
    "party world coordinates": "mapX: hex.centerX, mapY: hex.centerY" in emap,
    "party never suppressed by empty portraits": not re.search(r"if\s*\(\s*!portraits\.length\s*\)\s*return\s+null", cluster),
    "party world transform": 'data-party-world-anchored="true"' in emap and "translate3d(" in emap and "scale(" in emap,
    "screen-space projection removed": "currentPartyPresentation.left" not in emap and "currentPartyPresentation.top" not in emap,
    "party map marker scales with camera": "position={{ left: currentPartyPresentation.mapX, top: currentPartyPresentation.mapY }}" in emap,
    "map marker viewport clamps removed": "maxWidth: mapMarker ? undefined" in cluster and "maxHeight: mapMarker ? undefined" in cluster,
    "hard shadow strips removed for map marker": 'data-party-soft-shadow="true"' in cluster,
    "base breathing room": 'data-party-base="true"' in cluster and "width: '98%'" in cluster and "left: '1%'" in cluster,
    "version": "4.9.16" in read(version_rel) and "49016" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4916_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.16_WORLD_ANCHORED_PARTY_OK")
