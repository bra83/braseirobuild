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
data["version"]="4.9.46"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.45"','"version": "4.9.46"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.45'","APP_VERSION = '4.9.46'",t)
t=re.sub(r"APP_VERSION_CODE = 49045","APP_VERSION_CODE = 49046",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49045","versionCode = 49046",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.45"','versionName = "4.9.46"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49045&immersive=1","index.html?v=49046&immersive=1")
write(rel,t)

db=read("lib/db.ts")
factory=read("lib/characterFactory.ts")
commands=read("lib/master/commands.ts")
panel=read("components/HeirPanel.tsx")
types=read("lib/master/types.ts")
whole="\n".join(read(p) for p in [
    "App.tsx","lib/db.ts","lib/characterFactory.ts","lib/master/types.ts","lib/master/commands.ts",
    "components/MechanicsHub.tsx","components/HeirPanel.tsx",
])

checks={
  "version-4946":"4.9.46" in read("lib/version.ts") and "49046" in read("android/app/build.gradle.kts"),
  "db-preserved":"const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
  "tts-preserved":"Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
  "heir-state":"heirState?:" in db and "previousExperiencePool: number" in db,
  "heir-origin":"heirOrigin?:" in db and "creationPreviousExperienceBudget?: number" in db,
  "educate-yule":"case 'educate_heir'" in commands and "Fase de Sociedade em Yule" in commands,
  "paired-cost":"next.resources.treasure-=amount" in commands and "next.resources.adventurePoints-=amount" in commands,
  "spend-1-5":"amount<1||amount>5" in commands,
  "pool-max-20":"current+amount>20" in commands and "previousExperiencePool:pool" in commands,
  "ready-at-10":"ready:pool>=10" in commands,
  "family-skill":"case 'set_heir_family_skill'" in commands and "skill?.favorite" in commands,
  "heir-xp-budget":"previousExperienceBudget:budget" in factory and "creationPreviousExperienceBudget" in factory,
  "inherited-lifestyle":"sheet.identity.lifestyle=predecessor.sheet.identity.lifestyle" in factory,
  "inheritance-15-20":"const allowance=pool>=20?2:pool>=15?1:0" in commands,
  "inheritance-eligible":"Artefato/Item Maravilhoso ou Arma/Armadura Famosa" in commands,
  "inheritance-preserve":"Heranças mantêm todas as Recompensas/qualidades" in factory,
  "blessing-revealed":"item.magicalIdentified=true" in factory,
  "party-replacement":"partyIds.delete(predecessor.id)" in commands and "partyIds.add(heir.id)" in commands,
  "ui-wired":"HeirPanel" in whole and "Herdeiros e Heranças" in panel,
  "support-focus-fix":"fellowshipFocus?.[supporter.id]===hero.id?2:1" in commands,
  "skill-specials-preserved":"SkillTestSession" in types,
  "mixed-rewards-preserved":"magicalRewards?:" in db,
  "lone-lands-preserved":"LoneLandsState" in types,
  "no-data-clear":"indexedDB.deleteDatabase" not in whole and "clearApplicationUserData" not in whole and "pm clear" not in whole,
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4946_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.46_HEIRS_INHERITANCE_OK")
