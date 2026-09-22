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

# Center the atlas from the same canonical currentHexId used by the party
# marker. This removes a race where an empty discovery record could leave the
# world-space marker outside the initial viewport even though master state
# already had the correct hex.
center_pattern = re.compile(
    r"""  const hasInitialCentered = useRef\(false\);
  useEffect\(\(\) => \{
    if \(manifest && discovery\?\.currentHexId && size\.width > 0 && !hasInitialCentered\.current\) \{
      // Small timeout to ensure container dimensions are settled
      const timer = setTimeout\(\(\) => \{
        centerOnHex\(discovery\.currentHexId!, 0\.7\);
        hasInitialCentered\.current = true;
      \}, 100\);
      return \(\) => clearTimeout\(timer\);
    \}
  \}, \[manifest, discovery\?\.currentHexId, size\.width, centerOnHex\]\);""",
    re.M,
)
center_replacement = """  const hasInitialCentered = useRef(false);
  const canonicalPartyHexId = currentHexId || discovery?.currentHexId || null;
  useEffect(() => {
    if (manifest && canonicalPartyHexId && size.width > 0 && !hasInitialCentered.current) {
      const timer = setTimeout(() => {
        centerOnHex(canonicalPartyHexId, 0.7);
        hasInitialCentered.current = true;
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [manifest, canonicalPartyHexId, size.width, centerOnHex]);"""
emap, center_changed = center_pattern.subn(center_replacement, emap, count=1)
if center_changed != 1 and "const canonicalPartyHexId = currentHexId || discovery?.currentHexId || null;" not in emap:
    raise SystemExit("canonical initial-centering anchor missing")

write(map_rel, emap)

cluster_rel = "components/immersive/PartyCluster.tsx"
cluster = read(cluster_rel)

# The 4.9.15 upgrade baseline can still carry the older 4.9.4 PartyCluster
# contract: it only destructures characters/manifest and hard-codes 50%/42%.
# EriadorMap may pass position/mapMarker, but React simply ignores those props,
# which is exactly why the party remains pinned to screen-space. Restore the
# 4.9.5 positional contract before placing the component in the world layer.
if "position?: { left: number | string; top: number | string };" not in cluster:
    one_line_props = "interface PartyClusterProps { characters: Character[]; manifest: ImmersiveAssetManifest | null; }"
    expanded_props = """interface PartyClusterProps {
  characters: Character[];
  manifest: ImmersiveAssetManifest | null;
  position?: { left: number | string; top: number | string };
  mapMarker?: boolean;
}"""
    if one_line_props in cluster:
        cluster = cluster.replace(one_line_props, expanded_props, 1)
    else:
        props_match = re.search(r"interface PartyClusterProps\s*\{([\s\S]*?)\}", cluster)
        if not props_match:
            raise SystemExit("PartyClusterProps interface missing")
        props_body = props_match.group(1)
        extra = ""
        if "position?:" not in props_body:
            extra += "\n  position?: { left: number | string; top: number | string };"
        if "mapMarker?:" not in props_body:
            extra += "\n  mapMarker?: boolean;"
        cluster = cluster[:props_match.end(1)] + extra + cluster[props_match.end(1):]

signature = re.search(
    r"export const PartyCluster:\s*React\.FC<PartyClusterProps>\s*=\s*\(\{([\s\S]{0,320}?)\}\)\s*=>\s*\{",
    cluster,
)
if not signature:
    raise SystemExit("PartyCluster component signature not found")
params = signature.group(1)
if "position" not in params or "mapMarker" not in params:
    cleaned = params.strip()
    names = [item.strip() for item in cleaned.split(",") if item.strip()]
    if not any(item.startswith("position") for item in names):
        names.append("position")
    if not any(item.startswith("mapMarker") for item in names):
        names.append("mapMarker = false")
    else:
        names = ["mapMarker = false" if item == "mapMarker" else item for item in names]
    new_params = " " + ", ".join(names) + " "
    cluster = cluster[:signature.start(1)] + new_params + cluster[signature.end(1):]

old_fixed_position = "      pointerEvents: 'none', position: 'absolute', left: '50%', top: '42%', transform: 'translate(-50%, -50%)',"
new_position = """      pointerEvents: 'none', position: 'absolute',
      left: position?.left ?? '50%', top: position?.top ?? '42%', transform: 'translate(-50%, -50%)',"""
if old_fixed_position in cluster:
    cluster = cluster.replace(old_fixed_position, new_position, 1)
elif "left: position?.left ?? '50%'" not in cluster:
    raise SystemExit("PartyCluster fixed-position style anchor missing")

old_fixed_box = "      zIndex: 35, height: 122, width: 148, maxWidth: '38vw', maxHeight: '18vh', overflow: 'visible',"
new_box = """      zIndex: mapMarker ? 45 : 35,
      height: mapMarker ? 96 : 122, width: mapMarker ? 116 : 148,
      maxWidth: mapMarker ? '30vw' : '38vw', maxHeight: mapMarker ? '14vh' : '18vh', overflow: 'visible',
      filter: mapMarker ? 'drop-shadow(0 5px 5px rgba(0,0,0,.65))' : undefined,"""
if old_fixed_box in cluster:
    cluster = cluster.replace(old_fixed_box, new_box, 1)
elif "zIndex: mapMarker ? 45 : 35" not in cluster:
    raise SystemExit("PartyCluster map-marker sizing anchor missing")

# Never let an empty/late portrait list suppress the map marker. 4.9.15 added
# visiblePortraits fallback, but remove any surviving legacy early-return from
# older source variants as well.
cluster = re.sub(r"\s*if\s*\(\s*!portraits\.length\s*\)\s*return\s+null\s*;\s*", "\n", cluster, count=1)

# The PartyCluster is scaled by the parent world layer. Keep its existing marker
# box dimensions; the regression is fixed by moving that box into the exact map
# transform rather than by mutating viewport sizing here.

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
    "PartyCluster accepts world position": "position?: { left: number | string; top: number | string };" in cluster and "left: position?.left ?? '50%'" in cluster,
    "PartyCluster mapMarker prop scoped": "mapMarker = false" in cluster and "zIndex: mapMarker ? 45 : 35" in cluster,
    "party world transform": 'data-party-world-anchored="true"' in emap and "translate3d(" in emap and "scale(" in emap,
    "screen-space projection removed": "currentPartyPresentation.left" not in emap and "currentPartyPresentation.top" not in emap,
    "party map marker scales with camera": "position={{ left: currentPartyPresentation.mapX, top: currentPartyPresentation.mapY }}" in emap,
    "initial camera uses canonical party hex": "const canonicalPartyHexId = currentHexId || discovery?.currentHexId || null;" in emap,
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
