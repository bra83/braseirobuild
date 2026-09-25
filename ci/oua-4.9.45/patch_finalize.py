from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_finalize.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding="utf-8")

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.45"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.44"','"version": "4.9.45"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.44'","APP_VERSION = '4.9.45'",t)
t=re.sub(r"APP_VERSION_CODE = 49044","APP_VERSION_CODE = 49045",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49044","versionCode = 49045",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.44"','versionName = "4.9.45"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49044&immersive=1","index.html?v=49045&immersive=1")
write(rel,t)

types=read("lib/master/types.ts")
commands=read("lib/master/commands.ts")
panel=read("components/SkillResolutionPanel.tsx")
whole="\n".join(read(p) for p in [
    "App.tsx","lib/db.ts","lib/master/types.ts","lib/master/state.ts","lib/master/commands.ts",
    "components/MechanicsHub.tsx","components/SkillResolutionPanel.tsx",
])

checks={
  "version-4945":"4.9.45" in read("lib/version.ts") and "49045" in read("android/app/build.gradle.kts"),
  "db-preserved":"const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
  "tts-preserved":"Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
  "skill-session-state":"export interface SkillTestSession" in types and "skillTestSession?: SkillTestSession | null" in types,
  "support-command":"case 'roll_skill_test_session'" in commands and "Suporte exige uma Perícia apropriada com pelo menos 1 ponto" in commands,
  "support-cost":"updateHero(supporter.id,sheet=>adjustHope(sheet,-1))" in commands and "supportDice=1" in commands,
  "one-supporter":"supporterCharacterId?: string" in types and "Um Herói não pode prestar Suporte a si mesmo" in commands,
  "cancel-failure":"choice==='cancel-failure'" in commands and "cancelledFailureBy" in types,
  "additional-success":"choice==='additional-success'" in commands and "additionalSuccesses+=1" in commands,
  "information":"choice==='information'" in commands and "informationDiscoveries+=1" in commands,
  "silent":"choice==='silent'" in commands and "silent=true" in commands,
  "hurry":"choice==='hurry'" in commands and "timeMultiplier=0.5" in commands,
  "influence":"choice==='influence'" in commands and "additionalTargetGroups+=1" in commands,
  "icons-not-grade":"iconsSpent:source.iconsSpent+1" in commands and "successIcons" in types,
  "ui-six":all(label in panel for label in ["Cancelar falha","+1 sucesso","Informação","Silêncio","Metade do tempo","+ alvo/grupo"]),
  "mixed-rewards-preserved":"magicalRewards" in read("lib/db.ts"),
  "lone-lands-preserved":"LoneLandsState" in types,
  "manual-journey-preserved":"Decisão de Jornada bloqueada no fluxo automático" in commands,
  "no-data-clear":"indexedDB.deleteDatabase" not in whole and "clearApplicationUserData" not in whole and "pm clear" not in whole,
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4945_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.45_SUPPORT_SPECIAL_SUCCESSES_OK")
