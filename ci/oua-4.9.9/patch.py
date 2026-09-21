from pathlib import Path
import re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_499.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)

# Keep chronicle visible whenever there is an active scene.
imm = imm.replace(
    "  useEffect(() => { if (props.currentNode?.id) setChronicleOpen(true); }, [props.currentNode?.id]);",
    "  useEffect(() => { setChronicleOpen(Boolean(props.currentNode?.id)); }, [props.currentNode?.id]);",
    1,
)

# Mapa must not hide narration/actions.
imm = imm.replace(
    "            if (tab === 'jogo') { setChronicleOpen(false); return; }",
    "            if (tab === 'jogo') { if (node) setChronicleOpen(true); return; }",
    1,
)

# Remove explicit close-cronica action: map + narration coexist.
imm = imm.replace(
    """      <button type="button" onClick={() => setChronicleOpen(false)} style={{ marginTop: 8, alignSelf: 'center', border: 0, background: 'transparent', color: '#624521', fontFamily: 'inherit', fontSize: 11, fontWeight: 700, textDecoration: 'underline' }}>Fechar crônica e voltar ao mapa</button>
""",
    "",
    1,
)

# Make the panel compact enough to preserve map context but always show narration + choices.
imm = imm.replace(
    "maxHeight: '72dvh', minHeight: choices.length ? 390 : 330,",
    "maxHeight: '46dvh', minHeight: choices.length ? 278 : 218,",
    1,
)

# Add clear section labels for Master narration and predefined actions.
scroll = """      <div style={{ flex: '1 1 auto', minHeight: 0, overflowY: 'auto', paddingRight: 3, fontSize: 15, lineHeight: 1.48 }}>
"""
if scroll not in imm:
    raise SystemExit("narrative scroll anchor missing")
imm = imm.replace(scroll, """      <div style={{ flex: '0 0 auto', marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Narração do Mestre</div>
      <div style={{ flex: '1 1 auto', minHeight: 0, overflowY: 'auto', paddingRight: 3, fontSize: 15, lineHeight: 1.48 }}>
""", 1)

choice_anchor = """      {choices.length > 0 && <div style={{ display: 'grid', gridTemplateColumns: choices.length === 1 ? '1fr' : 'repeat(2, minmax(0, 1fr))', gap: 7, marginTop: 10 }}>
"""
if choice_anchor not in imm:
    raise SystemExit("choices anchor missing")
imm = imm.replace(choice_anchor, """      {choices.length > 0 && <>
        <div style={{ flex: '0 0 auto', marginTop: 9, marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Ações sugeridas</div>
        <div style={{ display: 'grid', gridTemplateColumns: choices.length === 1 ? '1fr' : 'repeat(2, minmax(0, 1fr))', gap: 7 }}>
""", 1)
imm = imm.replace(
    """      </div>}

      <form onSubmit={event => props.onCustomActionSubmit(event, false)}""",
    """      </div></>}

      <form onSubmit={event => props.onCustomActionSubmit(event, false)}""",
    1,
)

write(imm_rel, imm)

map_rel = "components/EriadorMap.tsx"
emap = read(map_rel)

# Party cluster must use campaign currentHexId when discovery storage lags behind.
old = """  const currentPartyPresentation = useMemo(() => {
    if (!immersive || !discovery?.currentHexId || !partyCharacters.length) return null;
    const hex = byId.get(discovery.currentHexId);
    if (!hex) return null;
    return { left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, discovery?.currentHexId, partyCharacters.length, byId, camera.x, camera.y, camera.scale]);
"""
new = """  const currentPartyPresentation = useMemo(() => {
    const activeHexId = discovery?.currentHexId || currentHexId || null;
    if (!immersive || !activeHexId || !partyCharacters.length) return null;
    const hex = byId.get(activeHexId);
    if (!hex) return null;
    return { left: camera.x + hex.centerX * camera.scale, top: camera.y + hex.centerY * camera.scale };
  }, [immersive, discovery?.currentHexId, currentHexId, partyCharacters.length, byId, camera.x, camera.y, camera.scale]);
"""
if old not in emap:
    raise SystemExit("party presentation anchor missing")
emap = emap.replace(old, new, 1)

# Keep footprint fallback visible too.
emap = emap.replace(
    "if (partyMarkerMode === 'footprints' && discovery?.currentHexId) {\n      const current = byId.get(discovery.currentHexId);",
    "if (partyMarkerMode === 'footprints' && (discovery?.currentHexId || currentHexId)) {\n      const current = byId.get(discovery?.currentHexId || currentHexId || '');",
    1,
)

write(map_rel, emap)

# Version bump.
pkg = root / "package.json"
if pkg.exists():
    p = pkg.read_text(encoding="utf-8")
    p = re.sub(r'"version":\s*"4\.9\.8"', '"version": "4.9.9"', p, count=1)
    pkg.write_text(p, encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.8"', '"version": "4.9.9"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.8'", "APP_VERSION = '4.9.9'", version)
version = re.sub(r"APP_VERSION_CODE = 49008", "APP_VERSION_CODE = 49009", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49008", "versionCode = 49009", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.8"', 'versionName = "4.9.9"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49008&immersive=1", "index.html?v=49009&immersive=1")
write(main_rel, main)

checks = {
    "map keeps chronicle": "if (node) setChronicleOpen(true)" in imm,
    "master narration label": "Narração do Mestre" in imm,
    "suggested actions label": "Ações sugeridas" in imm,
    "party currentHex fallback": "discovery?.currentHexId || currentHexId" in emap,
    "v499": "4.9.9" in read(version_rel) and "49009" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
if not all(checks.values()):
    raise SystemExit("PATCH_499_FAILED")
print("PATCH_499_OK")
