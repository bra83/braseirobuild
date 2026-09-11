from pathlib import Path
import base64
import io
import re
import sys
from PIL import Image, ImageOps

root = Path(sys.argv[1])

# Preserve the complete 1.3.9 delta over the proven 1.3.8 source.
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

# Launcher source is the approved stacked TALES / FROM THE / LOOP branding visible
# in the approved Menu/Mochila reference. It is stored next to this transform so
# no character portrait or generic Android icon can be selected by accident.
master_file = Path(__file__).with_name("tales-launcher-approved.webp.b64")
if not master_file.is_file():
    raise SystemExit("Approved Tales launcher source is missing")
raw = base64.b64decode(master_file.read_text(encoding="utf-8").strip())
with Image.open(io.BytesIO(raw)) as im:
    logo = im.convert("RGBA")
print("LAUNCHER_ICON_SOURCE=approved_stacked_tales_from_the_loop_header_logo")

BG = (5, 15, 21, 255)

def compose(size: int, round_icon: bool = False) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), BG)
    safe = int(size * 0.86)
    fitted = ImageOps.contain(logo, (safe, safe), Image.Resampling.LANCZOS)
    x = (size - fitted.width) // 2
    y = (size - fitted.height) // 2
    canvas.alpha_composite(fitted, (x, y))
    if round_icon:
        from PIL import ImageDraw
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(canvas, (0, 0), mask)
        return out
    return canvas

# The 1.3.8 source already has generic .webp launcher resources. Android considers
# resources with the same basename duplicates even when extensions differ, so remove
# every old variant before writing the approved replacement.
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
    for stem in ("ic_launcher", "ic_launcher_round"):
        for ext in (".png", ".webp", ".jpg", ".jpeg", ".xml"):
            (d / f"{stem}{ext}").unlink(missing_ok=True)
    compose(size, False).save(d / "ic_launcher.png", "PNG", optimize=True)
    compose(size, True).save(d / "ic_launcher_round.png", "PNG", optimize=True)

# Adaptive icon foreground. Remove the old XML/vector resource sharing this basename.
drawable = res / "drawable"
drawable.mkdir(parents=True, exist_ok=True)
for ext in (".png", ".webp", ".jpg", ".jpeg", ".xml"):
    (drawable / f"ic_launcher_foreground{ext}").unlink(missing_ok=True)
fg_size = 432
fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
fitted = ImageOps.contain(logo, (320, 320), Image.Resampling.LANCZOS)
fg.alpha_composite(fitted, ((fg_size - fitted.width) // 2, (fg_size - fitted.height) // 2))
fg.save(drawable / "ic_launcher_foreground.png", "PNG", optimize=True)

values = res / "values"
values.mkdir(parents=True, exist_ok=True)
colors = values / "launcher_icon_colors.xml"
colors.write_text('''<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="launcher_icon_background">#050F15</color>\n</resources>\n''', encoding="utf-8")

# Replace adaptive icon declarations explicitly.
d = res / "mipmap-anydpi-v26"
d.mkdir(parents=True, exist_ok=True)
for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
    (d / name).write_text('''<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/launcher_icon_background" />\n    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n</adaptive-icon>\n''', encoding="utf-8")

manifest = root / "app" / "src" / "main" / "AndroidManifest.xml"
m = manifest.read_text(encoding="utf-8")
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
base += '''\n## Bloco 1.3.10\n- ÍCONE DO APP: launcher Android substituído pelo logo empilhado TALES / FROM THE / LOOP da referência visual aprovada.\n- O retrato de personagem selecionado incorretamente no primeiro ensaio deste bloco foi descartado antes de qualquer APK ser publicado.\n- Ícones normal, redondo e adaptativo foram atualizados sem alterar telas aprovadas.\n- Correções 1.3.9 preservadas integralmente.\n'''
resume.write_text(base, encoding="utf-8")

# Hard checks before CI continues.
assert 'versionName = "1.3.10"' in gradle.read_text(encoding="utf-8")
assert '@mipmap/ic_launcher' in manifest.read_text(encoding="utf-8")
assert (res / "mipmap-mdpi" / "ic_launcher.png").is_file()
assert (res / "mipmap-xxxhdpi" / "ic_launcher.png").is_file()
assert (drawable / "ic_launcher_foreground.png").is_file()
assert (res / "mipmap-anydpi-v26" / "ic_launcher.xml").is_file()
for folder in sizes:
    d = res / folder
    assert not (d / "ic_launcher.webp").exists()
    assert not (d / "ic_launcher_round.webp").exists()
assert not (drawable / "ic_launcher_foreground.xml").exists()
print("TALES 1.3.10 transform: OK")
