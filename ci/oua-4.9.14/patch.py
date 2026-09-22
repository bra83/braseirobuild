from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4914.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)
imm, n = re.subn(
    r'partyMarkerMode=\{node\s*&&\s*props\.characters\.length\s*\?\s*"none"\s*:\s*"footprints"\}',
    'partyMarkerMode="none"',
    imm,
    count=1,
)
if n != 1 and 'partyMarkerMode="none"' not in imm:
    raise SystemExit("immersive party marker anchor missing")
write(imm_rel, imm)

map_rel = "components/EriadorMap.tsx"
emap = read(map_rel)
old_party = """  const currentPartyPresentation = useMemo(() => {
    const activeHexId = discovery?.currentHexId || currentHexId || null;
    if (!immersive || !activeHexId || !partyCharacters.length) return null;
    const hex = byId.get(activeHexId);
    if (!hex) return null;
    return { left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, discovery?.currentHexId, currentHexId, partyCharacters.length, byId, camera.x, camera.y, camera.scale]);
"""
new_party = """  const currentPartyPresentation = useMemo(() => {
    const activeHexId = currentHexId || discovery?.currentHexId || null;
    if (!immersive || !activeHexId || !partyCharacters.length) return null;
    const hex = byId.get(activeHexId);
    if (!hex) return null;
    return { hexId: activeHexId, left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, currentHexId, discovery?.currentHexId, partyCharacters.length, byId, camera.x, camera.y, camera.scale]);
"""
if old_party in emap:
    emap = emap.replace(old_party, new_party, 1)
else:
    old_party2 = """  const currentPartyPresentation = useMemo(() => {
    if (!immersive || !discovery?.currentHexId || !partyCharacters.length) return null;
    const hex = byId.get(discovery.currentHexId);
    if (!hex) return null;
    return { left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, discovery?.currentHexId, partyCharacters.length, byId, camera.x, camera.y, camera.scale]);
"""
    if old_party2 not in emap:
        raise SystemExit("currentPartyPresentation block missing")
    emap = emap.replace(old_party2, new_party, 1)

emap = emap.replace(
    """        {immersive && currentPartyPresentation && (
          <PartyCluster
""",
    """        {immersive && currentPartyPresentation && (
          <div
            data-party-current-hex={currentPartyPresentation.hexId}
            data-party-map-marker="true"
            style={{ position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 45 }}
          >
          <PartyCluster
""",
    1,
)
emap = emap.replace(
    """            mapMarker
          />
        )}
""",
    """            mapMarker
          />
          </div>
        )}
""",
    1,
)
write(map_rel, emap)

cluster_rel = "components/immersive/PartyCluster.tsx"
cluster = read(cluster_rel)

candidate_anchor = """        if (!builtIn && builtIns) {
          const candidates = builtIns.tokens.filter(item => partyTokenMatchesCulture(item, culture));
          if (candidates.length) {
            const hash = Array.from(String(character.id || character.name || culture)).reduce((sum, ch) => (sum * 31 + ch.charCodeAt(0)) >>> 0, 7);
            builtIn = candidates[hash % candidates.length] || null;
          }
        }
"""
replacement = """        if (!builtIn && builtIns) {
          const candidates = builtIns.tokens.filter(item => partyTokenMatchesCulture(item, culture));
          const pool = candidates.length ? candidates : builtIns.tokens;
          if (pool.length) {
            const hash = Array.from(String(character.id || character.name || culture || 'companhia')).reduce((sum, ch) => (sum * 31 + ch.charCodeAt(0)) >>> 0, 7);
            builtIn = pool[hash % pool.length] || null;
          }
        }
"""
if candidate_anchor not in cluster:
    raise SystemExit("PartyCluster token fallback anchor missing")
cluster = cluster.replace(candidate_anchor, replacement, 1)
cluster = cluster.replace("      }).filter(entry => entry.url);\n", "      });\n", 1)

cluster = cluster.replace(
    """  return <div
    aria-label="Companhia no hex atual"
""",
    """  return <div
    aria-label="Companhia no hex atual"
    data-party-cluster="true"
    data-party-members={portraits.length}
""",
    1,
)

old_img = """        return <img
          key={entry.id}
          src={entry.url || undefined}
          alt={entry.name}
          draggable={false}
          style={{
            position: 'absolute', width: 58, height: 80, maxWidth: '16vw', maxHeight: '12vh', objectFit: 'contain', objectPosition: 'center bottom',
            left: \`\${p.x}%\`, top: \`\${p.y}%\`, transform: \`translate(-50%, -50%) scale(\${effectiveScale})\`, transformOrigin: '50% 82%', zIndex: p.z,
            filter: 'drop-shadow(0 3px 2px rgba(0,0,0,.78))',
          }}
        />;
"""
new_img = """        const commonStyle: React.CSSProperties = {
          position: 'absolute', width: 58, height: 80, maxWidth: '16vw', maxHeight: '12vh',
          left: \`\${p.x}%\`, top: \`\${p.y}%\`, transform: \`translate(-50%, -50%) scale(\${effectiveScale})\`, transformOrigin: '50% 82%', zIndex: p.z,
          filter: 'drop-shadow(0 3px 2px rgba(0,0,0,.78))',
        };
        if (entry.url) return <img
          key={entry.id}
          src={entry.url}
          alt={entry.name}
          draggable={false}
          style={{ ...commonStyle, objectFit: 'contain', objectPosition: 'center bottom' }}
        />;
        return <div
          key={entry.id}
          aria-label={entry.name}
          style={{
            ...commonStyle, height: 58, width: 58, borderRadius: 999, display: 'flex', alignItems: 'center', justifyContent: 'center',
            border: '2px solid rgba(117,77,36,.96)', background: 'radial-gradient(circle at 40% 34%, #dcc79f, #8a673e 72%)',
            color: '#2b1b0f', fontFamily: 'Georgia, serif', fontSize: 24, fontWeight: 900, boxShadow: '0 3px 7px rgba(0,0,0,.52)',
          }}
        >{(entry.name || 'C').trim().slice(0, 1).toUpperCase()}</div>;
"""
if old_img not in cluster:
    raise SystemExit("PartyCluster portrait render anchor missing")
cluster = cluster.replace(old_img, new_img, 1)
write(cluster_rel, cluster)

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
cluster = read(cluster_rel)
checks = {
    "no immersive footprints": 'partyMarkerMode="none"' in imm and 'partyMarkerMode={node && props.characters.length ? "none" : "footprints"}' not in imm,
    "party current hex canonical": "const activeHexId = currentHexId || discovery?.currentHexId || null;" in emap,
    "real party marker": 'data-party-map-marker="true"' in emap and 'data-party-cluster="true"' in cluster,
    "party art fallback": "const pool = candidates.length ? candidates : builtIns.tokens;" in cluster,
    "party css last-resort": "radial-gradient(circle at 40% 34%" in cluster,
    "version": "4.9.14" in read(version_rel) and "49014" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4914_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.14_PARTY_HEX_OK")
