from pathlib import Path
import re, sys
from PIL import Image, ImageOps

root = Path(sys.argv[1])

# Reapply the complete 1.3.9 delta over the preserved 1.3.8 source.
ns = {"__name__": "__main__", "__file__": str(Path(__file__).with_name("tales-1.3.9-transform.py"))}
code = Path(__file__).with_name("tales-1.3.9-transform.py").read_text(encoding="utf-8")
old_argv = sys.argv[:]
sys.argv = [ns["__file__"], str(root)]
try:
    exec(compile(code, ns["__file__"], "exec"), ns)
finally:
    sys.argv = old_argv

# Version bump.
gradle = root / "app" / "build.gradle.kts"
t = gradle.read_text(encoding="utf-8")
t = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 14', t, count=1)
t = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.3.10"', t, count=1)
gradle.write_text(t, encoding="utf-8")

res = root / "app" / "src" / "main" / "res"
if not res.is_dir():
    raise SystemExit("Android res directory not found")

# Use the game's own canonical Tales branding as the launcher source. Avoid generic
# launcher/nav/portrait assets and prefer a Tales/Loop logo already present in the approved UI.
raster_ext = {".png", ".webp", ".jpg", ".jpeg"}
candidates = []
for p in res.rglob("*"):
    if not p.is_file() or p.suffix.lower() not in raster_ext:
        continue
    low = p.stem.lower()
    parent = p.parent.name.lower()
    if "launcher" in low or parent.startswith("mipmap"):
        continue
    score = 0
    if "logo" in low: score += 100
    if "tales" in low: score += 80
    if "loop" in low: score += 70
    if "title" in low: score += 25
    if "brand" in low: score += 20
    if "header" in low: score += 10
    if any(x in low for x in ("portrait", "kid_", "scene_", "item_", "nav_", "condition")):
        score -= 120
    if score > 0:
        try:
            with Image.open(p) as im:
                w, h = im.size
            area = w * h
        except Exception:
            continue
        candidates.append((score, area, p))

if not candidates:
    raise SystemExit("No canonical Tales/Loop logo raster found; refusing to invent a launcher icon")

candidates.sort(key=lambda x: (x[0], x[1]), reverse=True)
_, _, source = candidates[0]
print(f"LAUNCHER_ICON_SOURCE={source.relative_to(root)}")

with Image.open(source) as im:
    logo = im.convert("RGBA")

# Crop transparent/empty outer space, preserving the approved logo itself.
alpha = logo.getchannel("A")
bbox = alpha.getbbox()
if bbox:
    logo = logo.crop(bbox)

# Launcher uses the same dark visual family already used by the VTT shell.
BG = (11, 17, 24, 255)

def compose(size: int, round_icon: bool = False) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), BG)
    safe = int(size * 0.76)
    fitted = ImageOps.contain(logo, (safe, safe), Image.Resampling.LANCZOS)
    x = (size - fitted.width) // 2
    y = (size - fitted.height) // 2
    canvas.alpha_composite(fitted, (x, y))
    if round_icon:
        mask = Image.new("L", (size, size), 0)
        from PIL import ImageDraw
        d = ImageDraw.Draw(mask)
        d.ellipse((0, 0, size - 1, size - 1), fill=255)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(canvas, (0, 0), mask)
        return out
    return canvas

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for folder, size in sizes.items():
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    compose(size, False).save(d / "ic_launcher.png", "PNG", optimize=True)
    compose(size, True).save(d / "ic_launcher_round.png", "PNG", optimize=True)

# Adaptive icon foreground, with extra safe space to avoid Android masking the approved logo.
drawable = res / "drawable"
drawable.mkdir(parents=True, exist_ok=True)
fg_size = 432
fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
fitted = ImageOps.contain(logo, (250, 250), Image.Resampling.LANCZOS)
fg.alpha_composite(fitted, ((fg_size - fitted.width)//2, (fg_size - fitted.height)//2))
fg.save(drawable / "ic_launcher_foreground.png", "PNG", optimize=True)

values = res / "values"
values.mkdir(parents=True, exist_ok=True)
colors = values / "launcher_icon_colors.xml"
colors.write_text('''<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="launcher_icon_background">#0B1118</color>\n</resources>\n''', encoding="utf-8")

for folder in ("mipmap-anydpi-v26",):
    d = res / folder
    d.mkdir(parents=True, exist_ok=True)
    (d / "ic_launcher.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/launcher_icon_background" />\n    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n</adaptive-icon>\n''', encoding="utf-8")
    (d / "ic_launcher_round.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/launcher_icon_background" />\n    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n</adaptive-icon>\n''', encoding="utf-8")

manifest = root / "app" / "src" / "main" / "AndroidManifest.xml"
m = manifest.read_text(encoding="utf-8")
# Preserve any other manifest attributes while making the launcher resources explicit.
if 'android:icon=' in m:
    m = re.sub(r'android:icon="@[^"]+"', 'android:icon="@mipmap/ic_launcher"', m, count=1)
else:
    m = m.replace('<application', '<application\n        android:icon="@mipmap/ic_launcher"', 1)
if 'android:roundIcon=' in m:
    m = re.sub(r'android:roundIcon="@[^"]+"', 'android:roundIcon="@mipmap/ic_launcher_round"', m, count=1)
else:
    m = m.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@mipmap/ic_launcher"\n        android:roundIcon="@mipmap/ic_launcher_round"', 1)
manifest.write_text(m, encoding="utf-8")

# Update the resume record inside the source tree.
resume = root / "TALES_RETOMADA_CURRENT.md"
base = resume.read_text(encoding="utf-8") if resume.exists() else "# TALES FROM THE LOOP — RETOMADA ATUAL\n"
base += f'''\n## Bloco 1.3.10\n- ÍCONE DO APP: launcher Android trocado para a identidade visual Tales já existente no projeto, sem inventar identidade nova.\n- Fonte usada para o ícone: `{source.relative_to(root)}`.\n- Ícones normal, redondo e adaptativo atualizados.\n- Correções 1.3.9 preservadas integralmente.\n'''
resume.write_text(base, encoding="utf-8")

# Hard checks before letting CI compile anything.
assert 'versionName = "1.3.10"' in gradle.read_text(encoding="utf-8")
assert '@mipmap/ic_launcher' in manifest.read_text(encoding="utf-8")
assert (res / "mipmap-mdpi" / "ic_launcher.png").is_file()
assert (res / "mipmap-xxxhdpi" / "ic_launcher.png").is_file()
assert (drawable / "ic_launcher_foreground.png").is_file()
assert (res / "mipmap-anydpi-v26" / "ic_launcher.xml").is_file()
print("TALES 1.3.10 transform: OK")
