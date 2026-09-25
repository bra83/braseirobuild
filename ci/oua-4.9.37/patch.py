from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4937.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

# Combat positions are canonical normalized coordinates (0..1). The narrative
# checkpoint was rounding them directly, collapsing nearly every token to 0/1
# and giving the Master useless tactical context. Convert them to readable
# percentages while preserving normalized state.
narr_rel="lib/narrativeEngine.ts"
narr=read(narr_rel)
old='''.map(actor => `${actor.actorId}: x=${Math.round(actor.position!.x)}, y=${Math.round(actor.position!.y)}`);'''
new='''.map(actor => `${actor.actorId}: x=${Math.round(actor.position!.x * 100)}%, y=${Math.round(actor.position!.y * 100)}%`);'''
narr=req(narr,old,new,"combat narrative coordinate context")
write(narr_rel,narr)

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.37"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.36"','"version": "4.9.37"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.36'","APP_VERSION = '4.9.37'",t)
t=re.sub(r"APP_VERSION_CODE = 49036","APP_VERSION_CODE = 49037",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"
t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49036","versionCode = 49037",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.36"','versionName = "4.9.37"',t)
write(gradle_rel,t)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel).replace("index.html?v=49036&immersive=1","index.html?v=49037&immersive=1")
write(main_rel,main)

narr=read(narr_rel); app=read("App.tsx"); main=read(main_rel)
checks={
 "combat-narrative-percent-context": "actor.position!.x * 100" in narr and "actor.position!.y * 100" in narr,
 "combat-state-fix-preserved": "const normalizedCombatActors" in read("lib/master/state.ts") and "safeX=clamp(normalizedX,0.03,0.97)" in read("lib/rules/combat.ts"),
 "tts-current-architecture-preserved": "kokoroBackendPackages" in main and "Braseiro Kokoro PT-BR · neural" in main,
 "music-persistence-preserved": "scene-music-preserved-manual" in app,
 "journey-lock-preserved": "Movimento livre bloqueado: selecione um destino e use Planejar rota" in app,
 "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "version": "4.9.37" in read(version_rel) and "49037" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4937_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.37_COMBAT_NARRATIVE_CONTEXT_OK")
