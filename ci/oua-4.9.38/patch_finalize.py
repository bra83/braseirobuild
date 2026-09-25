from pathlib import Path
import json, re, shutil, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_finalize.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8")

def req(text, old, new, label):
    if old not in text:
        raise SystemExit("missing anchor: " + label)
    return text.replace(old, new, 1)

templates = Path(__file__).resolve().parent / "templates"
for src_rel, dst_rel in [
    ("components/JourneyResolutionPanel.tsx", "components/JourneyResolutionPanel.tsx"),
    ("components/MechanicalStatusStrip.tsx", "components/MechanicalStatusStrip.tsx"),
    ("lib/actionResolution.ts", "lib/actionResolution.ts"),
]:
    src = templates / src_rel
    dst = root / dst_rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)

imm_rel = "components/immersive/ImmersiveJourneyView.tsx"
imm = read(imm_rel)
old_eye = """function eyeAttention(masterState: MasterState): { current: number; max: number } {
  const threatClock = masterState.threatClocks.find(item => /olho|sauron|inimigo/i.test(item.name));
  if (threatClock) return { current: Math.max(0, threatClock.current), max: Math.max(1, threatClock.max) };
  return { current: Math.max(0, Math.min(12, Math.round((masterState.scene.tension || 0) * 1.2))), max: 12 };
}"""
new_eye = """function eyeAttention(masterState: MasterState): { current: number; max: number } {
  return {
    current: Math.max(0, Number(masterState.company.eyeAwareness) || 0),
    max: Math.max(1, Number(masterState.company.eyeHuntThreshold) || 16),
  };
}"""
imm = req(imm, old_eye, new_eye, "real Eye HUD")

old_card = """      <div style={{ ...glassPanel, width: 100, borderRadius: 12, padding: '7px 8px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 10, opacity: .78 }}>
          {sauronEye && <img src={sauronEye} alt="" style={{ width: 18, height: 18, objectFit: 'contain', flex: '0 0 auto' }} />}
          <span>Olho</span><strong style={{ marginLeft: 'auto', fontSize: 11 }}>{eye.current}/{eye.max}</strong>
        </div>
        <div style={{ height: 5, borderRadius: 99, overflow: 'hidden', marginTop: 5, background: 'rgba(255,255,255,.12)' }}>
          <div style={{ height: '100%', width: {`${Math.max(0, Math.min(100, eye.current / eye.max * 100))}%`}, background: 'linear-gradient(90deg,#8f6e45,#b8793d,#b84026)', borderRadius: 99 }} />
        </div>
      </div>"""
new_card = """      {props.masterState.company.eyeAwarenessEnabled && (
        <div style={{ ...glassPanel, width: 100, borderRadius: 12, padding: '7px 8px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 10, opacity: .78 }}>
            {sauronEye && <img src={sauronEye} alt="" style={{ width: 18, height: 18, objectFit: 'contain', flex: '0 0 auto' }} />}
            <span>Olho</span><strong style={{ marginLeft: 'auto', fontSize: 11 }}>{eye.current}/{eye.max}</strong>
          </div>
          <div style={{ height: 5, borderRadius: 99, overflow: 'hidden', marginTop: 5, background: 'rgba(255,255,255,.12)' }}>
            <div style={{ height: '100%', width: {`${Math.max(0, Math.min(100, eye.current / eye.max * 100))}%`}, background: 'linear-gradient(90deg,#8f6e45,#b8793d,#b84026)', borderRadius: 99 }} />
          </div>
        </div>
      )}"""
imm = req(imm, old_card, new_card, "optional Eye card")
write(imm_rel, imm)

pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.38"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.37"', '"version": "4.9.38"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version_rel = "lib/version.ts"
t = read(version_rel)
t = re.sub(r"APP_VERSION = '4\.9\.37'", "APP_VERSION = '4.9.38'", t)
t = re.sub(r"APP_VERSION_CODE = 49037", "APP_VERSION_CODE = 49038", t)
write(version_rel, t)

gradle_rel = "android/app/build.gradle.kts"
t = read(gradle_rel)
t = re.sub(r"versionCode\s*=\s*49037", "versionCode = 49038", t)
t = re.sub(r'versionName\s*=\s*"4\.9\.37"', 'versionName = "4.9.38"', t)
write(gradle_rel, t)

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t = read(main_rel).replace("index.html?v=49037&immersive=1", "index.html?v=49038&immersive=1")
write(main_rel, t)

print("PATCH_4938_FINALIZE_OK")
