from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4911.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)

# Pending test must be visible/routable in immersive mode.
imm = imm.replace(
    "import type { Character, IndividualToken, Npc, SceneNode } from '../../lib/db';",
    "import type { Character, IndividualToken, Npc, PendingTest, SceneNode } from '../../lib/db';",
    1,
)
if "  pendingTest?: PendingTest | null;\n" not in imm:
    imm = imm.replace(
        "  isStoryLoading?: boolean;\n",
        "  isStoryLoading?: boolean;\n  pendingTest?: PendingTest | null;\n  isNarrationLoading?: boolean;\n  onPlayNarration: () => void | Promise<void>;\n  onStopNarration: () => void;\n",
        1,
    )

# Map button must CLOSE the chronicle, never reopen it.
# 4.9.10 may format this handler over one or several lines, so patch semantically.
imm, map_count = re.subn(
    r"if\s*\(tab\s*===\s*'jogo'\)\s*\{[\s\S]{0,240}?return;\s*\}",
    "if (tab === 'jogo') { setChronicleOpen(false); return; }",
    imm,
    count=1,
)
if map_count == 0 and "if (tab === 'jogo') { setChronicleOpen(false); return; }" not in imm:
    raise SystemExit("map navigation branch not found")

# Choice dispatch: provide visible result for taps and route pending tests to Dice.
location_anchor = "  const locationName = props.masterState.position.locationName || node?.location || 'Eriador';\n"
if location_anchor not in imm:
    raise SystemExit("location anchor missing")
helper = """  const locationName = props.masterState.position.locationName || node?.location || 'Eriador';

  const runSceneChoice = (choice: StoryChoice) => {
    if (props.isStoryLoading) return;
    setMoreOpen(false);
    setChronicleOpen(false);
    if (props.pendingTest) {
      props.onOpenDice();
      return;
    }
    props.onChoice(choice);
  };

  const submitSceneAction = (event: React.FormEvent) => {
    if (props.pendingTest) {
      event.preventDefault();
      setChronicleOpen(false);
      props.onOpenDice();
      return;
    }
    if (props.isStoryLoading) {
      event.preventDefault();
      return;
    }
    setChronicleOpen(false);
    props.onCustomActionSubmit(event, false);
  };
"""
imm = imm.replace(location_anchor, helper, 1)

# Root panel remains interactive and touch-friendly.
imm = imm.replace(
    "display: 'flex', flexDirection: 'column', overflowY: 'auto', overflowX: 'hidden',",
    "display: 'flex', flexDirection: 'column', overflowY: 'auto', overflowX: 'hidden', pointerEvents: 'auto', touchAction: 'pan-y', WebkitOverflowScrolling: 'touch',",
    1,
)

# Add one compact play/stop control beside the Master narration title.
narration_label_plain = """      <div style={{ flex: '0 0 auto', marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Narração do Mestre</div>
"""
if narration_label_plain not in imm:
    raise SystemExit("master narration label anchor missing")
imm = imm.replace(
    narration_label_plain,
    """      <div style={{ flex: '0 0 auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 6 }}>
        <div style={{ fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Narração do Mestre</div>
        <button
          type="button"
          onClick={() => props.isNarrationLoading ? props.onStopNarration() : void props.onPlayNarration()}
          style={{ minHeight: 36, padding: '6px 11px', borderRadius: 999, border: '1px solid #7b562d', background: props.isNarrationLoading ? '#5e2b22' : '#5b3d20', color: '#fff0cf', fontFamily: 'inherit', fontSize: 11, fontWeight: 900, touchAction: 'manipulation', whiteSpace: 'nowrap' }}
        >{props.isNarrationLoading ? '■ Parar narração' : '🔊 Ouvir narração'}</button>
      </div>
""",
    1,
)

# Always-visible close button. Insert immediately before the scene image block.
scene_anchor = "      {node && <div style={{ flex: '0 0 auto', marginBottom: 10 }}>\n"
if scene_anchor not in imm:
    raise SystemExit("scene image anchor missing")
close_button = """      <div style={{ position: 'sticky', top: 0, zIndex: 20, display: 'flex', justifyContent: 'flex-end', height: 0, overflow: 'visible', pointerEvents: 'none' }}>
        <button
          type="button"
          aria-label="Fechar crônica e mostrar mapa"
          onClick={() => setChronicleOpen(false)}
          style={{ pointerEvents: 'auto', touchAction: 'manipulation', width: 42, height: 42, marginTop: -8, marginRight: -8, borderRadius: 999, border: '1px solid rgba(83,53,25,.55)', background: '#5b3d20', color: '#fff0cf', fontFamily: 'inherit', fontSize: 24, fontWeight: 900, lineHeight: 1, boxShadow: '0 5px 14px rgba(0,0,0,.28)' }}
        >×</button>
      </div>

"""
imm = imm.replace(scene_anchor, close_button + scene_anchor, 1)

# Pending test banner: no more silent ignored commands.
quick_anchor = "      <div style={{ flex: '0 0 auto', marginTop: 9 }}>\n        <div style={{ marginBottom: 5, fontSize: 10, fontWeight: 900, letterSpacing: '.08em', textTransform: 'uppercase', color: '#745432' }}>Ações rápidas</div>"
if quick_anchor not in imm:
    raise SystemExit("quick actions anchor missing")
pending_block = """      {props.pendingTest && <button
        type="button"
        onClick={() => { setChronicleOpen(false); props.onOpenDice(); }}
        style={{ width: '100%', minHeight: 46, marginTop: 9, borderRadius: 9, border: '1px solid #7b562d', background: '#6a431f', color: '#fff0cf', fontFamily: 'inherit', fontSize: 12, fontWeight: 900, touchAction: 'manipulation' }}
      >Teste pendente — abrir Dados</button>}

"""
imm = imm.replace(quick_anchor, pending_block + quick_anchor, 1)

# Fixed actions now dispatch through the robust handler.
imm = imm.replace(
    "onClick={() => props.onChoice({ id: 'preset-' + action.id, text: action.text })}",
    "onClick={() => runSceneChoice({ id: 'preset-' + action.id, text: action.text })}",
)
imm = imm.replace(
    "style={{ minHeight: 40, borderRadius: 8,",
    "style={{ minHeight: 44, borderRadius: 8, touchAction: 'manipulation',",
)

# Generated Master suggestions use the same handler.
imm = imm.replace("onClick={() => props.onChoice(choice)}", "onClick={() => runSceneChoice(choice)}")
imm = imm.replace(
    "minHeight: 36, gridColumn:",
    "minHeight: 40, touchAction: 'manipulation', gridColumn:",
)

# Custom action submit must also be responsive and close to map immediately.
imm = imm.replace(
    "<form onSubmit={event => props.onCustomActionSubmit(event, false)}",
    "<form onSubmit={submitSceneAction}",
    1,
)

# Reintroduce an explicit bottom close control too, for accessibility and discoverability.
form_end = """      </form>
"""
if form_end not in imm:
    raise SystemExit("form end missing")
bottom_close = """      </form>
      <button
        type="button"
        onClick={() => setChronicleOpen(false)}
        style={{ width: '100%', minHeight: 40, marginTop: 8, borderRadius: 8, border: '1px solid rgba(83,53,25,.35)', background: 'rgba(255,248,231,.28)', color: '#5b3d20', fontFamily: 'inherit', fontSize: 11, fontWeight: 900, touchAction: 'manipulation' }}
      >Voltar ao mapa</button>
"""
imm = imm.replace(form_end, bottom_close, 1)

# Bottom navigation itself must always accept taps.
imm = imm.replace(
    "...glassPanel, position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 100,",
    "...glassPanel, position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 100, pointerEvents: 'auto', touchAction: 'manipulation',",
    1,
)

write(imm_rel, imm)

# Pass pending test from GameView to immersive view.
game_rel = "components/GameView.tsx"
game = read(game_rel)
anchor = "      isStoryLoading={isStoryLoading}\n      onNavigate={onNavigate}\n"
if anchor not in game:
    raise SystemExit("GameView immersive pending-test anchor missing")
game = game.replace(
    anchor,
    "      isStoryLoading={isStoryLoading}\n      pendingTest={pendingTest}\n      onNavigate={onNavigate}\n",
    1,
)
write(game_rel, game)

# Version bump only; database and package identity remain untouched.
pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.11"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.10"', '"version": "4.9.11"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
version = read(version_rel)
version = re.sub(r"APP_VERSION = '4\.9\.10'", "APP_VERSION = '4.9.11'", version)
version = re.sub(r"APP_VERSION_CODE = 49010", "APP_VERSION_CODE = 49011", version)
write(version_rel, version)

gradle_rel = "android/app/build.gradle.kts"
gradle = read(gradle_rel)
gradle = re.sub(r"versionCode\s*=\s*49010", "versionCode = 49011", gradle)
gradle = re.sub(r'versionName\s*=\s*"4\.9\.10"', 'versionName = "4.9.11"', gradle)
write(gradle_rel, gradle)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
main = main.replace("index.html?v=49010&immersive=1", "index.html?v=49011&immersive=1")
write(main_rel, main)

imm = read(imm_rel)
game = read(game_rel)
checks = {
    "map closes chronicle": "if (tab === 'jogo') { setChronicleOpen(false); return; }" in imm,
    "top close button": 'aria-label="Fechar crônica e mostrar mapa"' in imm,
    "bottom map button": ">Voltar ao mapa</button>" in imm,
    "quick choice closes": "runSceneChoice" in imm and "setChronicleOpen(false)" in imm,
    "pending test routes to dice": "Teste pendente — abrir Dados" in imm and "props.pendingTest" in imm,
    "custom action responsive": "<form onSubmit={submitSceneAction}" in imm,
    "GameView passes pending test": "pendingTest={pendingTest}" in game,
    "narration button wired": "🔊 Ouvir narração" in imm and "onPlayNarration={() => onPlayNarration(currentNode || undefined)}" in game,
    "version 4.9.11": "4.9.11" in read(version_rel) and "49011" in read(gradle_rel),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4911_FAILED: " + ", ".join(failed))
print("PATCH_OUA_4.9.11_OK")
