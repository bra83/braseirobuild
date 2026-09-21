from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4910.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

# ------------------------------------------------------------------
# 1) Campaign -> GameView: legacy/invalid party ids must never hide party.
# ------------------------------------------------------------------
app_rel = "App.tsx"
app = read(app_rel)

party_line = "    const partyCharacters = characters.filter(character => (campaign.partyCharacterIds || []).includes(character.id));"
if party_line not in app:
    raise SystemExit("renderActiveView partyCharacters anchor missing")
app = app.replace(
    party_line,
    """    const configuredPartyCharacters = characters.filter(character => (campaign.partyCharacterIds || []).includes(character.id));
    const partyCharacters = configuredPartyCharacters.length ? configuredPartyCharacters : characters;""",
    1,
)

# Replace all direct filtered props in render branches with the effective fallback.
app = app.replace(
    "partyCharacterIds={campaign.partyCharacterIds || []} partyCharacters={characters.filter(character => (campaign.partyCharacterIds || []).includes(character.id))}",
    "partyCharacterIds={partyCharacters.map(character => character.id)} partyCharacters={partyCharacters}",
)
write(app_rel, app)

# ------------------------------------------------------------------
# 2) Immersive view: party visible, narration guaranteed, real preset actions.
# ------------------------------------------------------------------
imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)

# The camera should keep the current party hex on screen.
if 'followParty={false}' not in imm:
    raise SystemExit("followParty anchor missing")
imm = imm.replace('followParty={false}', 'followParty={true}', 1)

# Add true fixed player actions, distinct from generated Master suggestions.
choices_anchor = "  const choices = (node?.choices || []).slice(0, 3);\n"
if choices_anchor not in imm:
    raise SystemExit("choices declaration anchor missing")
preset_decl = """  const choices = (node?.choices || []).slice(0, 3);
  const presetActions = useMemo(() => [
    { id: 'observe', label: 'Observar', text: 'Observar atentamente o ambiente, procurando detalhes, perigos e mudanças ao redor.' },
    { id: 'investigate', label: 'Investigar', text: 'Investigar o local em busca de pistas, rastros, objetos ou sinais relevantes.' },
    { id: 'talk', label: 'Conversar', text: 'Conversar com alguém presente na cena, buscando informação ou reação.' },
    { id: 'explore', label: 'Explorar', text: 'Explorar com cautela os arredores imediatos, sem abandonar a posição da Companhia.' },
  ] as const, []);
"""
imm = imm.replace(choices_anchor, preset_decl, 1)

# Make the chronicle large enough, but not a full-screen replacement for the map.
imm = re.sub(
    r"maxHeight:\s*'46dvh',\s*minHeight:\s*choices\.length\s*\?\s*278\s*:\s*218,",
    "maxHeight: '72dvh', minHeight: choices.length ? 430 : 360,",
    imm,
    count=1,
)
# Fallback for a source that still contains the older 72/43 sizing.
imm = re.sub(
    r"maxHeight:\s*choices\.length\s*\?\s*'43dvh'\s*:\s*'34dvh',\s*minHeight:\s*choices\.length\s*\?\s*250\s*:\s*190,",
    "maxHeight: '72dvh', minHeight: choices.length ? 430 : 360,",
    imm,
    count=1,
)

# Parent scroll is a safety net. Important content must not disappear by flex shrinking.
imm = imm.replace(
    "display: 'flex', flexDirection: 'column', overflow: 'hidden',",
    "display: 'flex', flexDirection: 'column', overflowY: 'auto', overflowX: 'hidden',",
    1,
)

# Scene illustration stays 16:9 but is compact enough to leave room for the text.
imm = imm.replace(
    "width: '100%', aspectRatio: '16 / 9', overflow: 'hidden'",
    "width: 'min(78vw, 300px)', margin: '0 auto', aspectRatio: '16 / 9', overflow: 'hidden'",
    1,
)

# Narration must have a real visible area; never flex down to zero.
imm = imm.replace(
    "style={{ flex: '1 1 auto', minHeight: 0, overflowY: 'auto', paddingRight: 3, fontSize: 15, lineHeight: 1.48 }}",
    "style={{ flex: '0 0 auto', minHeight: 108, maxHeight: 170, overflowY: 'auto', padding: '2px 4px 4px 0', fontSize: 15, lineHeight: 1.48 }}",
    1,
)

# Insert fixed preset action grid immediately after narration and before AI suggestions.
narration_close = """      </div>

      {choices.length > 0 && <>
"""
if narration_close not in imm:
    # Older source before 4.9.9 label wrapper.
    narration_close = """      </div>

      {choices.length > 0 && <div"""
    if narration_close not in imm:
        raise SystemExit("narration -> choices anchor missing")
    fixed_block = """      </div>

      <div style={{ flex: '0 0 auto', marginTop: 9 }}>
        <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Ações rápidas</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 6 }}>
          {presetActions.map(action => <button
            key={action.id}
            type="button"
            onClick={() => props.onChoice({ id: 'preset-' + action.id, text: action.text })}
            style={{ minHeight: 40, borderRadius: 8, border: '1px solid rgba(83,53,25,.42)', background: 'rgba(255,248,231,.34)', color: '#2d2116', fontFamily: 'inherit', fontSize: 12, fontWeight: 800 }}
          >{action.label}</button>)}
        </div>
      </div>

      {choices.length > 0 && <div"""
    imm = imm.replace(narration_close, fixed_block, 1)
else:
    fixed_block = """      </div>

      <div style={{ flex: '0 0 auto', marginTop: 9 }}>
        <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Ações rápidas</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 6 }}>
          {presetActions.map(action => <button
            key={action.id}
            type="button"
            onClick={() => props.onChoice({ id: 'preset-' + action.id, text: action.text })}
            style={{ minHeight: 40, borderRadius: 8, border: '1px solid rgba(83,53,25,.42)', background: 'rgba(255,248,231,.34)', color: '#2d2116', fontFamily: 'inherit', fontSize: 12, fontWeight: 800 }}
          >{action.label}</button>)}
        </div>
      </div>

      {choices.length > 0 && <>
"""
    imm = imm.replace(narration_close, fixed_block, 1)

# Rename generated choices truthfully.
imm = imm.replace(">Ações sugeridas</div>", ">Sugestões do Mestre</div>", 1)

# Generated suggestions should no longer dominate the screen.
imm = imm.replace(
    "minHeight: 44, gridColumn:",
    "minHeight: 36, gridColumn:",
)
imm = imm.replace(
    "padding: '7px 9px', color: '#2d2116', fontFamily: 'inherit', fontSize: 13, fontWeight: 700, lineHeight: 1.2,",
    "padding: '5px 7px', color: '#2d2116', fontFamily: 'inherit', fontSize: 11, fontWeight: 700, lineHeight: 1.18,",
)

write(imm_rel, imm)

# ------------------------------------------------------------------
# 3) Version bump, update-safe.
# ------------------------------------------------------------------
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.10"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.9"', '"version": "4.9.10"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.9'", "APP_VERSION = '4.9.10'", version)
version = re.sub(r"APP_VERSION_CODE = 49009", "APP_VERSION_CODE = 49010", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49009", "versionCode = 49010", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.9"', 'versionName = "4.9.10"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49009&immersive=1", "index.html?v=49010&immersive=1")
write(main_rel, main)

# Behavioral source gates, not just labels.
app = read(app_rel)
imm = read(imm_rel)
checks = {
    "party fallback uses existing characters": "configuredPartyCharacters.length ? configuredPartyCharacters : characters" in app,
    "GameView receives effective party": "partyCharacters={partyCharacters}" in app,
    "map follows party": "followParty={true}" in imm,
    "narration cannot collapse": "minHeight: 108, maxHeight: 170" in imm,
    "fixed actions exist": "Ações rápidas" in imm and "presetActions.map" in imm,
    "generated choices renamed": "Sugestões do Mestre" in imm,
    "version 4.9.10": "4.9.10" in read(version_rel) and "49010" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4910_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.10_OK")
