from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4915.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

# 4.9.15 fixes the real production map path exposed by the Android screenshot:
# 1) route-history footprints must never occupy the active/current hex in immersive mode;
# 2) the real PartyCluster must remain visible even when no token portrait is resolved yet;
# 3) the visual gate must exercise ImmersiveJourneyView, not an isolated EriadorMap with a fake token.

map_rel = "components/EriadorMap.tsx"
emap = read(map_rel)

# Keep the actual party projection anchored to the canonical current hex, even
# before characters/tokens finish hydrating. PartyCluster itself provides a
# deterministic visible fallback.
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
    return { hexId: activeHexId, left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, currentHexId, discovery?.currentHexId, byId, camera.x, camera.y, camera.scale]);"""
emap = emap[:m.start()] + party_replacement + emap[m.end():]

# The screenshot showed the history footprints sitting exactly on the current
# hex. That path is independent from partyMarkerMode, so 4.9.14 disabled the
# wrong marker. Preserve trail footprints behind the party, but never draw one
# on the current hex in immersive mode.
route_anchor = """        route.forEach((hex, index) => {
          const previous = route[Math.max(0, index - 1)];"""
route_replacement = """        route.forEach((hex, index) => {
          const activePartyHexId = currentHexId || discovery.currentHexId;
          if (immersive && activePartyHexId && hex.id === activePartyHexId) return;
          const previous = route[Math.max(0, index - 1)];"""
if route_anchor not in emap:
    if "if (immersive && activePartyHexId && hex.id === activePartyHexId) return;" not in emap:
        raise SystemExit("route footprint loop anchor missing")
else:
    emap = emap.replace(route_anchor, route_replacement, 1)

# Ensure canvas redraws if these marker inputs change; older code omitted them
# from draw() dependencies and could leave a stale footprint painted.
old_deps = "  }, [byId, camera, discovery, editingArea, gmMode, immersive, immersiveTileRevision, manifest, newlyDiscoveredIds, selected, selectedLocation, showGrid, showIds, size, strongholds]);"
new_deps = "  }, [byId, camera, currentHexId, discovery, editingArea, gmMode, immersive, immersiveTileRevision, manifest, newlyDiscoveredIds, partyMarkerMode, selected, selectedLocation, showGrid, showIds, size, strongholds]);"
if old_deps in emap:
    emap = emap.replace(old_deps, new_deps, 1)
elif "partyMarkerMode, selected" not in emap:
    raise SystemExit("draw dependency anchor missing")

write(map_rel, emap)

cluster_rel = "components/immersive/PartyCluster.tsx"
cluster = read(cluster_rel)

fallback_b64 = "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNjAiIGhlaWdodD0iMjIwIiB2aWV3Qm94PSIwIDAgMTYwIDIyMCI+CjxlbGxpcHNlIGN4PSI4MCIgY3k9IjIwMyIgcng9IjU1IiByeT0iMTIiIGZpbGw9InJnYmEoMCwwLDAsLjM1KSIvPgo8Y2lyY2xlIGN4PSI4MCIgY3k9IjU1IiByPSIzMCIgZmlsbD0iI2Q4Yjk4YSIvPgo8cGF0aCBkPSJNDUUgNDQgODAgNSAxMTUgNDQgMTA0IDY2IDgwIDQ1IDU2IDY2WiIgZmlsbD0iIzVmNDMyNyIvPgo8cGF0aCBkPSJNMzUgMTkwUTM4IDg4IDgwIDg0UTEyMiA4OCAxMjUgMTkwWiIgZmlsbD0iIzM1NWEzYiIvPgo8cGF0aCBkPSJNNDggMTEwIDgwIDE0NSAxMTIgMTEwIiBmaWxsPSJub25lIiBzdHJva2U9IiNkM2JlODgiIHN0cm9rZS13aWR0aD0iNyIvPgo8Y2lyY2xlIGN4PSI2OSIgY3k9IjU0IiByPSIzIiBmaWxsPSIjMjUxYjEyIi8+CjxjaXJjbGUgY3g9IjkxIiBjeT0iNTQiIHI9IjMiIGZpbGw9IiMyNTFiMTIiLz4KPC9zdmc+"
fallback_const = "const FALLBACK_TRAVELER = 'data:image/svg+xml;base64," + fallback_b64 + "';\n\n"
if "const FALLBACK_TRAVELER =" not in cluster:
    anchor = "};\n\nexport const PartyCluster"
    if anchor not in cluster:
        raise SystemExit("PartyCluster constant anchor missing")
    cluster = cluster.replace(anchor, "};\n\n" + fallback_const + "export const PartyCluster", 1)

# Do not discard a real character just because its portrait lookup failed.
cluster = cluster.replace("      }).filter(entry => entry.url);", "      });", 1)

visible_anchor = """  const base = resolveImmersiveAsset(manifest, 'party.base');
  const shadow = resolveImmersiveAsset(manifest, 'party.shadow');
  const formation = FORMATIONS[Math.max(1, Math.min(6, portraits.length))] || FORMATIONS[1];

  if (!portraits.length) return null;"""
visible_replacement = """  const base = resolveImmersiveAsset(manifest, 'party.base');
  const shadow = resolveImmersiveAsset(manifest, 'party.shadow');
  const visiblePortraits: PortraitEntry[] = portraits.length ? portraits : [{
    id: 'company-fallback',
    name: characters[0]?.sheet?.identity?.name || characters[0]?.name || 'Companhia',
    culture: characters[0]?.sheet?.identity?.culture || characters[0]?.culture || '',
    url: FALLBACK_TRAVELER,
    token: null,
    scale: 1,
  }];
  const formation = FORMATIONS[Math.max(1, Math.min(6, visiblePortraits.length))] || FORMATIONS[1];"""
if visible_anchor in cluster:
    cluster = cluster.replace(visible_anchor, visible_replacement, 1)
elif "const visiblePortraits: PortraitEntry[]" not in cluster:
    raise SystemExit("PartyCluster visibility anchor missing")

cluster = cluster.replace("{portraits.map((entry,index) => {", "{visiblePortraits.map((entry,index) => {", 1)
cluster = cluster.replace("src={entry.url || undefined}", "src={entry.url || FALLBACK_TRAVELER}", 1)

# If the decorative atlas base fails to load, keep a visible brass medallion so
# the party never collapses into an invisible DOM node.
base_anchor = """    {base && <img src={base} alt="" draggable={false} style={{ position: 'absolute', bottom: 3, left: '4%', zIndex: 1, height: '70%', width: '92%', objectFit: 'contain', filter: 'drop-shadow(0 4px 4px rgba(0,0,0,.55))' }}/>}
    <div style={{ position: 'absolute', inset: 0, zIndex: 2, overflow: 'visible' }}>"""
base_replacement = """    {base ? <img src={base} alt="" draggable={false} style={{ position: 'absolute', bottom: 3, left: '4%', zIndex: 1, height: '70%', width: '92%', objectFit: 'contain', filter: 'drop-shadow(0 4px 4px rgba(0,0,0,.55))' }}/> : <div aria-hidden="true" style={{ position: 'absolute', left: '18%', right: '18%', bottom: 8, height: '42%', zIndex: 1, borderRadius: '50%', border: '3px solid #9a6b34', background: 'radial-gradient(ellipse at 50% 38%, #7f542c 0%, #513319 48%, #2b1a0d 78%, #150d08 100%)', boxShadow: 'inset 0 0 0 2px rgba(232,195,130,.3), 0 4px 5px rgba(0,0,0,.65)' }} />}
    <div style={{ position: 'absolute', inset: 0, zIndex: 2, overflow: 'visible' }}>"""
if base_anchor in cluster:
    cluster = cluster.replace(base_anchor, base_replacement, 1)

if 'data-party-cluster="true"' not in cluster:
    marker = '    aria-label="Companhia no hex atual"\n'
    if marker not in cluster:
        raise SystemExit("PartyCluster root anchor missing")
    cluster = cluster.replace(marker, marker + '    data-party-cluster="true"\n', 1)

write(cluster_rel, cluster)

# Force the production immersive view to keep the dedicated footprint marker off.
imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)
imm = re.sub(
    r'partyMarkerMode=\{[^\n]+\}',
    'partyMarkerMode="none"',
    imm,
    count=1,
)
if 'partyMarkerMode="none"' not in imm:
    raise SystemExit("immersive partyMarkerMode none missing")
write(imm_rel, imm)

# Version bump.
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.15"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.14"', '"version": "4.9.15"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.14'", "APP_VERSION = '4.9.15'", version)
version = re.sub(r"APP_VERSION_CODE = 49014", "APP_VERSION_CODE = 49015", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49014", "versionCode = 49015", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.14"', 'versionName = "4.9.15"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49014&immersive=1", "index.html?v=49015&immersive=1")
write(main_rel, main)

# Hard gates against the exact regression shown by the user's Android capture.
emap = read(map_rel)
cluster = read(cluster_rel)
imm = read(imm_rel)
checks = {
    "current hex excludes route footprints": "if (immersive && activePartyHexId && hex.id === activePartyHexId) return;" in emap,
    "party marker mode none": 'partyMarkerMode="none"' in imm,
    "party uses canonical current hex": "const activeHexId = currentHexId || discovery?.currentHexId || null;" in emap,
    "party no longer waits for character hydration": "if (!immersive || !activeHexId) return null;" in emap and "!partyCharacters.length" not in emap[emap.find("const currentPartyPresentation"):emap.find("const currentPartyPresentation")+700],
    "PartyCluster fallback token": "FALLBACK_TRAVELER" in cluster and "visiblePortraits" in cluster,
    "PartyCluster visible hook": 'data-party-cluster="true"' in cluster,
    "draw reacts to marker changes": "currentHexId, discovery" in emap and "partyMarkerMode, selected" in emap,
    "version": "4.9.15" in read(version_rel) and "49015" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4915_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.15_REAL_PARTY_CURRENT_HEX_OK")
