from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_android_version.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

main_rel = "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main = read(main_rel)
old_share = """        @JavascriptInterface fun shareText(title: String, text: String) = runOnUiThread {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(sendIntent, "Enviar prompt ao Gemini"))
        }
"""
new_share = """        @JavascriptInterface fun shareText(title: String, text: String) = runOnUiThread {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            try {
                startActivity(Intent(sendIntent).setPackage("com.google.android.apps.bard"))
            } catch (_: Exception) {
                startActivity(Intent.createChooser(sendIntent, "Enviar prompt ao Gemini"))
            }
        }
"""
if old_share not in main and "com.google.android.apps.bard" not in main:
    raise SystemExit("Android shareText anchor missing")
if old_share in main:
    main = main.replace(old_share, new_share, 1)
main = re.sub(r"index\.html\?v=\d+&immersive=1", "index.html?v=49008&immersive=1", main)
write(main_rel, main)

pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.8"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = re.sub(r'"version":\s*"4\.9\.[0-9]+"', '"version": "4.9.8"', t, count=2)
    lock.write_text(t, encoding="utf-8")

version = read("lib/version.ts")
version = re.sub(r"export const APP_VERSION = '[^']+';", "export const APP_VERSION = '4.9.8';", version)
version = re.sub(r"export const APP_VERSION_CODE = \d+;", "export const APP_VERSION_CODE = 49008;", version)
write("lib/version.ts", version)

gradle = read("android/app/build.gradle.kts")
gradle = re.sub(r"versionCode\s*=\s*\d+", "versionCode = 49008", gradle, count=1)
gradle = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "4.9.8"', gradle, count=1)
write("android/app/build.gradle.kts", gradle)

write("CHANGELOG_OUA_4.9.8_SCENE_ENTITIES_GEMINI.md", """# O Um Anel 4.9.8 — cenas, ilustrações e entidades persistentes

- imagem de cena horizontal 16:9 acima da narração do Mestre;
- prompt visual curto persistido em cada SceneNode;
- botão principal abre diretamente o app Google Gemini com o prompt preenchido;
- retorno por Compartilhar → O Um Anel reaproveita a caixa de entrada Android já existente;
- compartilhamento é roteado por destino: cena ou npcId;
- NPCs presentes aparecem separados do local e da narrativa;
- nomes e speakers reais citados na narrativa são reconciliados com NPCs cadastrados;
- localidades nunca são criadas como NPC por análise de texto;
- NPC sem retrato abre geração externa; retrato recebido vira token persistente por npcId;
- NPC com retrato reutiliza automaticamente a mesma imagem entre cenas e sessões;
- regeneração substitui apenas o retrato daquele mesmo ID;
- modo imersivo recebe a mesma imagem 16:9 e a mesma faixa de NPCs presentes;
- geração interna de cena também passa a pedir 16:9;
- preserva o overlay visual 4.9.7, Atlas, Companhia e Motor Barbara 1.1.3.
""")

checks = {
    "gemini package": "com.google.android.apps.bard" in read(main_rel),
    "version": "4.9.8" in read("lib/version.ts"),
    "version code": "49008" in read("android/app/build.gradle.kts"),
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL"), name)
if not all(checks.values()):
    raise SystemExit("PATCH_ANDROID_VERSION_498_FAILED")
print("PATCH_ANDROID_VERSION_498_OK")
