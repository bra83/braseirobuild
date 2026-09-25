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
data["version"]="4.9.42"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.41"','"version": "4.9.42"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.41'","APP_VERSION = '4.9.42'",t)
t=re.sub(r"APP_VERSION_CODE = 49041","APP_VERSION_CODE = 49042",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49041","versionCode = 49042",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.41"','versionName = "4.9.42"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49041&immersive=1","index.html?v=49042&immersive=1")
write(rel,t)

gear=read("lib/rules/magicalGearMechanics.ts")
commands=read("lib/master/commands.ts")
combat=read("lib/rules/combat.ts")
equipment=read("lib/rules/equipment.ts")
shadow=read("lib/rules/shadow.ts")
treasure=read("lib/rules/magicalTreasure.ts")
combat_ui=read("components/CombatRulesPanel.tsx")
treasure_ui=read("components/TreasureIndexPanel.tsx")
db=read("lib/db.ts")
types=read("lib/master/types.ts")
whole="\n".join(read(p) for p in [
    "App.tsx","lib/db.ts","lib/dice.ts","lib/master/types.ts","lib/master/state.ts","lib/master/commands.ts",
    "lib/rules/combat.ts","lib/rules/equipment.ts","lib/rules/shadow.ts","lib/rules/magicalTreasure.ts",
    "lib/rules/magicalGearMechanics.ts","components/DiceView.tsx","components/JourneyResolutionPanel.tsx",
    "components/MechanicsHub.tsx","components/CombatRulesPanel.tsx","components/TreasureIndexPanel.tsx",
])

checks={
  "version-4942": "4.9.42" in read("lib/version.ts") and "49042" in read("android/app/build.gradle.kts"),
  "db-preserved": "const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
  "tts-preserved": "Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
  "famous-bane-runtime": "actorIsBane" in gear and "set_combat_actor_banes" in commands and "Categorias de Flagelo" in combat_ui,
  "attribute-level-heavy": "attributeLevel" in types and "heavyKnocksDown" in gear and "stand_up_combat_actor" in commands and "Levantar após Pesada" in combat_ui,
  "hate-or-determination": "determination?: number" in types and "set_combat_actor_determination" in commands and "target.determination" in commands and "Salvar Determinação" in combat_ui,
  "hollow-steel": "hollowSteelExtraVolley" in commands and "opening-volley-count:" in commands,
  "superior-keen": "famousPiercingThreshold" in commands and "superior-keen" in gear,
  "ancient-close-fitting": "enchantedProtectionResultBonus" in combat and "ancient-close-fitting" in gear,
  "rune-weapon": "runeScoredWeaponIgnoresConditions" in combat,
  "rune-armour": "runeScoredArmourIgnoresConditions" in combat,
  "mithril-load": "mithril-armour" in gear and "enchantedLoad" in equipment,
  "terror-shine": "terror-shine" in gear and "famousOnHitHateLoss" in commands,
  "flame-hope": "flameOfHopeRecovery" in commands and "for(const characterId of partyIds)" in commands,
  "foe-slayer": "foeSlayerProtection" in commands and "automaticWound" in commands,
  "rune-shield": "runeScoredShieldMakesAttackerWeary" in commands,
  "rune-helm": "runeScoredHelmIgnoresCombatTaskConditions" in commands,
  "ancient-cunning-load": "ancient-cunning-make" in gear and "enchantedLoad" in equipment,
  "luminous": "luminousAutoAvoidAmbush" in commands and "luminous-warning:" in commands,
  "superior-grievous": "famousInjuryBonus" in commands and "superior-grievous" in gear,
  "very-fell": "famousDamageBonus" in commands and "very-fell" in gear,
  "cleaving": "resolve_cleaving_attack" in commands and "cleavingReadyByHero" in types and "Perfuradora" in combat_ui,
  "heavy": "heavyKnocksDown" in commands and "knocked-down" in commands,
  "bitter-missile": "bitter-missile" in gear and "famousOnHitHateLoss" in commands,
  "superior-reinforced": "famousShieldDynamicParryBonus" in commands and "famousShieldStaticParryBonus" in equipment,
  "straight-flight": "straightFlightIgnoresComplications" in combat,
  "curse-ill-luck": "applyIllLuckToResult" in whole and "ill-luck" in gear,
  "curse-hunted": "curseHuntedBy" in commands and "hunted-curse-detected:" in commands,
  "curse-weakening": "curseTnModifier" in treasure and "curse-attribute:" in commands,
  "curse-darkening": "curseDarkeningDicePenalty" in whole and "magicalCurseRevealed" in db,
  "curse-shadow-taint": "curseShadowTaintAmount" in shadow and "magicalCurseShadowApplied" in db,
  "curse-weakness": "magicalCurseWeaknessFlaw" in db and "flawsForShadowPath" in commands,
  "curse-malice": "curseBlocksHopeForWeapon" in commands and "curseBlocksHopeForSkill" in whole,
  "curse-ill-omen": "illOmenCouncilPenalty" in whole,
  "curse-ownership": "magicalItemSuppressed" in gear and "magicalCurseOwnerPresent" in db,
  "curse-ui": "Ativar Maldição" in treasure_ui and "Quebrar Maldição" in treasure_ui,
  "manual-journey-preserved": "Decisão de Jornada bloqueada no fluxo automático" in commands,
  "paths-preserved": "characterPath" in db and "treasureIndex" in types,
  "no-data-clear": "indexedDB.deleteDatabase" not in whole and "clearApplicationUserData" not in whole and "pm clear" not in whole,
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4942_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.42_MAGICAL_GEAR_CURSES_OK")
