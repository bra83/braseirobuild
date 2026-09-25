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
data["version"]="4.9.43"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.42"','"version": "4.9.43"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.42'","APP_VERSION = '4.9.43'",t)
t=re.sub(r"APP_VERSION_CODE = 49042","APP_VERSION_CODE = 49043",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49042","versionCode = 49043",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.42"','versionName = "4.9.43"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49042&immersive=1","index.html?v=49043&immersive=1")
write(rel,t)

types=read("lib/master/types.ts")
commands=read("lib/master/commands.ts")
rules=read("lib/rules/loneLands.ts")
panel=read("components/LoneLandsPanel.tsx")
app=read("App.tsx")
hub=read("components/MechanicsHub.tsx")
whole="\n".join(read(p) for p in [
    "App.tsx","lib/db.ts","lib/master/types.ts","lib/master/state.ts","lib/master/commands.ts",
    "lib/rules/loneLands.ts","lib/rules/combat.ts","components/MechanicsHub.tsx","components/LoneLandsPanel.tsx",
])

checks={
  "version-4943":"4.9.43" in read("lib/version.ts") and "49043" in read("android/app/build.gradle.kts"),
  "db-preserved":"const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
  "tts-preserved":"Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
  "lone-state":"export interface LoneLandsState" in types and "loneLands?: LoneLandsState" in types,
  "estelang":"ESTELANG" in rules and "case 'claim_estelang'" in commands and "Reivindicar Estelang" in panel,
  "heir-oath":"case 'invoke_heir_oath'" in commands and "loneLandsOathHeroAttackBonus" in read("lib/rules/combat.ts") and "loneLandsOathShadowBonus" in hub,
  "hill-shadow":"case 'mark_hill_shadow_dreamer'" in commands and "case 'destroy_hill_of_fear'" in commands,
  "lossoth-undertakings":"case 'undertake_hunt_lossoth'" in commands and "case 'undertake_stone_unhappy'" in commands and "resolve_lossoth_safe_refuge_rest" in commands,
  "forodwaith-state":"loneLandsForodwaith" in types and "case 'prepare_forodwaith_journey'" in commands,
  "forodwaith-real-roll":"forodwaithJourneyDiceModifier(master.journey,skill)" in app and "forodwaithRollMode(master.journey,skill" in app,
  "forodwaith-special-fatigue":"specialEventsResolved" in commands and "Math.max(0,2-cramReduction)" in commands,
  "dwarven-council":"start_lone_lands_dwarven_council" in commands and "set_lone_lands_council_request" in commands,
  "egel-council":"start_egel_council" in commands and "lossothHuntCouncilBonus" in commands,
  "combat-tasks":"case 'resolve_combat_task'" in commands and "Intimidar Inimigo" in panel and "Inspirar Camaradas" in panel and "Proteger Companheiro" in panel,
  "deadly-abilities":"set_combat_actor_deadly_abilities" in commands and "deadlyAbilities?: string[]" in types,
  "ui-wired":"LoneLandsPanel" in hub,
  "manual-journey-preserved":"Decisão de Jornada bloqueada no fluxo automático" in commands,
  "treasure-preserved":"treasureIndex" in types and "magicalGearMechanics" in whole,
  "paths-preserved":"characterPath" in read("lib/db.ts"),
  "no-data-clear":"indexedDB.deleteDatabase" not in whole and "clearApplicationUserData" not in whole and "pm clear" not in whole,
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4943_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.43_LONE_LANDS_OK")
