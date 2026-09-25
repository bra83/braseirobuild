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
data["version"]="4.9.44"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.43"','"version": "4.9.44"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.43'","APP_VERSION = '4.9.44'",t)
t=re.sub(r"APP_VERSION_CODE = 49043","APP_VERSION_CODE = 49044",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49043","versionCode = 49044",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.43"','versionName = "4.9.44"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49043&immersive=1","index.html?v=49044&immersive=1")
write(rel,t)

db=read("lib/db.ts")
types=read("lib/master/types.ts")
treasure=read("lib/rules/magicalTreasure.ts")
equipment=read("lib/rules/equipment.ts")
gear=read("lib/rules/magicalGearMechanics.ts")
progress=read("lib/rules/progression.ts")
commands=read("lib/master/commands.ts")
ui=read("components/TreasureIndexPanel.tsx")
whole="\n".join(read(p) for p in [
    "App.tsx","lib/db.ts","lib/master/types.ts","lib/master/commands.ts",
    "lib/rules/magicalTreasure.ts","lib/rules/magicalGearMechanics.ts","lib/rules/equipment.ts",
    "lib/rules/progression.ts","components/TreasureIndexPanel.tsx",
])

checks={
  "version-4944":"4.9.44" in read("lib/version.ts") and "49044" in read("android/app/build.gradle.kts"),
  "db-preserved":"const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
  "tts-preserved":"Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
  "mixed-model":"magicalRewards?:" in db and "rewards?: Array<{ kind:'common'|'enchanted'; id:string }>" in types,
  "max-three-min-one-enchanted":"rewards.length<1||rewards.length>3" in treasure and "pelo menos uma Recompensa Encantada" in treasure,
  "common-six":all(name in treasure for name in ["Afiada","Ajustado","Fabricação Habilidosa","Cruel","Atroz","Reforçado"]),
  "descriptor-conflicts":"ENHANCED_CONFLICTS" in treasure and "não pode coexistir" in treasure,
  "origin-item-validation":"enchantedRewardAllowed" in treasure and "commonRewardAllowed" in treasure,
  "bane-requirement":"Arma Élfica ou Numenoriana exige pelo menos uma Recompensa Encantada de Flagelo" in treasure,
  "ordered-first-awake":"magicalRewardsUnlocked:entry.kind==='famous-war-gear'?1:0" in commands,
  "common-runtime":"activeCommonRewardNames" in equipment and "itemReward(sheet,item" in equipment,
  "enchanted-runtime":"activeEnchantedRewardIds" in gear,
  "valor-awakening":"magicalRewardsUnlocked" in progress and "advanceValorForFamousQuality" in progress,
  "treasury-awakening":"famous.magicalRewardsUnlocked" in commands and "Visitar Tesouro" in ui,
  "ownership-suppression":"magicalCurse==='ownership'" in treasure and "activeFamousRewardSequence" in treasure,
  "ordered-ui":"Recompensas da peça · 1 a 3 · ordem de despertar" in ui and "+ Comum" in ui and "+ Encantada" in ui,
  "lone-lands-preserved":"LoneLandsPanel" in whole and "loneLands" in types,
  "manual-journey-preserved":"Decisão de Jornada bloqueada no fluxo automático" in commands,
  "no-data-clear":"indexedDB.deleteDatabase" not in whole and "clearApplicationUserData" not in whole and "pm clear" not in whole,
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4944_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.44_MIXED_FAMOUS_REWARDS_OK")
