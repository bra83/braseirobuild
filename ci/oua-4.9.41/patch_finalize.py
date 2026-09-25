from pathlib import Path
import json, re, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_finalize.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.41"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.40"','"version": "4.9.41"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.40'","APP_VERSION = '4.9.41'",t)
t=re.sub(r"APP_VERSION_CODE = 49040","APP_VERSION_CODE = 49041",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49040","versionCode = 49041",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.40"','versionName = "4.9.41"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49040&immersive=1","index.html?v=49041&immersive=1")
write(rel,t)

paths=read("lib/characterPaths.ts")
treasure=read("lib/rules/magicalTreasure.ts")
commands=read("lib/master/commands.ts")
app=read("App.tsx")
dice=read("lib/dice.ts")
db=read("lib/db.ts")
types=read("lib/master/types.ts")
registry=read("components/CharacterPathsCreator.tsx")
index_ui=read("components/TreasureIndexPanel.tsx")
whole="\n".join(read(p) for p in [
    "App.tsx","lib/db.ts","lib/dice.ts","lib/characterFactory.ts","lib/characterPaths.ts",
    "lib/master/types.ts","lib/master/state.ts","lib/master/commands.ts","lib/characterRules.ts",
    "lib/rules/shadow.ts","lib/rules/progression.ts","lib/rules/equipment.ts","lib/rules/magicalTreasure.ts",
    "components/DiceView.tsx","components/JourneyResolutionPanel.tsx","components/MechanicsHub.tsx",
    "components/CharacterPathsCreator.tsx","components/TreasureIndexPanel.tsx",
])

checks={
  "version-4941": "4.9.41" in read("lib/version.ts") and "49041" in read("android/app/build.gradle.kts"),
  "db-preserved": "const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
  "tts-preserved": "Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
  "paths-36": paths.count("cultureId:'") == 36,
  "paths-six-cultures": all(token in paths for token in ["bardings","dwarves","elves","hobbits","bree","rangers"]),
  "important-events-12": all(token in paths for token in ["shadow-haunted","destitute","half-wise","grim","inept","reclusive","jovial","slender","simpleton","vigorous","blessed","grey-wizard"]),
  "paths-before-xp": "const previousExperienceBudget=Math.max(0,baseBudget+Number(event.previousExperienceDelta||0));" in read("lib/characterFactory.ts"),
  "paths-first-flaw": "firstFlawForShadowPath" in read("lib/characterFactory.ts") and "startsWithFlaw" in paths,
  "paths-company-deltas": "characterPathCompanyDeltas" in app and "characterPathFellowshipDelta" in types and "characterPathEyeDelta" in types,
  "grey-wizard-runtime": "applyFeatOneAsEye" in dice and "sheetFeatOneBecomesEye" in whole,
  "paths-ui": "Trilhas de Personagens" in registry and "Criar Herói por Trilhas" in registry,
  "treasure-index-state": "TreasureIndexStateEntry" in types and "pendingMagicalTreasure" in types,
  "treasure-index-explicit": "treasureIndexEntryId" in commands and "Selecione uma entrada disponível do Índice de Tesouro" in commands,
  "magical-discovery": "rollMagicalTreasureDiscoveries" in commands and "feat===11||feat===12" in treasure,
  "greed-shadow-test": "resolveShadowGain(next,discovery.greedShadow,'greed'" in commands and "targetNumberForSkill(next,'Sabedoria')" in commands,
  "blessing-plus-two": "magicalBlessingDice" in read("components/DiceView.tsx") and "return (sheet.inventory||[]).some" in treasure and "? 2 : 0" in treasure,
  "blessing-magical-success": "identifiedBlessingCanMagicalSuccess" in whole and "blessingSource" in whole,
  "famous-first-quality": "magicalQualitiesUnlocked:entry.kind==='famous-war-gear'?1:0" in commands,
  "famous-valor-unlock": "advanceValorForFamousQuality" in read("lib/rules/progression.ts") and "source:'valor'" in index_ui,
  "famous-treasury-unlock": "sacrificedInventoryItemId" in commands and "Visitar Tesouro do povo" in index_ui,
  "magical-base-item": "magicalBaseItemName" in db and "canonicalWeapon(weapon.magicalBaseItemName" in read("lib/rules/equipment.ts"),
  "malice-skill-block": "curseBlocksHopeForSkill" in read("components/DiceView.tsx"),
  "manual-journey-preserved": "Decisão de Jornada bloqueada no fluxo automático" in commands,
  "no-data-clear": "indexedDB.deleteDatabase" not in whole and "clearApplicationUserData" not in whole and "pm clear" not in whole,
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4941_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.41_PATHS_TREASURE_OK")
