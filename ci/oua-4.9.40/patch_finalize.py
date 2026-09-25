from pathlib import Path
import json, re, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_finalize.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel):
    return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(text,encoding="utf-8")

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.40"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.39"','"version": "4.9.40"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.39'","APP_VERSION = '4.9.40'",t)
t=re.sub(r"APP_VERSION_CODE = 49039","APP_VERSION_CODE = 49040",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49039","versionCode = 49040",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.39"','versionName = "4.9.40"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49039&immersive=1","index.html?v=49040&immersive=1")
write(rel,t)

whole="\n".join(read(p) for p in [
    "lib/db.ts",
    "lib/master/types.ts",
    "lib/master/state.ts",
    "lib/master/commands.ts",
    "lib/rules/combat.ts",
    "lib/rules/progression.ts",
    "lib/rules/journey.ts",
    "lib/rules/supplementMechanics.ts",
    "components/CombatRulesPanel.tsx",
    "components/JourneyResolutionPanel.tsx",
    "components/SupplementRulesPanel.tsx",
    "components/MechanicsHub.tsx",
    "components/DiceView.tsx",
    "App.tsx",
])
checks={
    "version-4940": "4.9.40" in read("lib/version.ts") and "49040" in read("android/app/build.gradle.kts"),
    "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
    "manual-journey-preserved": "Decisão de Jornada bloqueada no fluxo automático" in read("lib/master/commands.ts"),
    "magical-success-real": "rollMagicalSuccess" in read("lib/dice.ts") and "magicalSuccess?: boolean" in read("lib/dice.ts"),
    "prepare-shot": "case 'prepare_shot'" in read("lib/master/commands.ts") and "prepared-shot:" in read("lib/rules/combat.ts"),
    "deadly-archery-secondary": "deadlyArcheryMakesPrepareShotSecondary" in read("lib/master/commands.ts") and "temp-round:main-action-used:prepare-shot" in read("lib/master/commands.ts"),
    "hound-explicit-choice": "pendingHoundPiercing" in read("lib/master/types.ts") and "resolve_hound_piercing_choice" in read("lib/master/commands.ts") and "Ferir o cão" in read("components/CombatRulesPanel.tsx"),
    "hound-recovery": "resolve_hound_recovery" in read("lib/master/commands.ts") and "recuperar Cão da Floresta" in read("components/SupplementRulesPanel.tsx"),
    "hound-rearward": "companionMeleeCount" in read("lib/rules/combat.ts") and "houndMelee" in read("lib/master/commands.ts"),
    "elven-lights-power": "activate_elven_lights" in read("lib/master/commands.ts") and "elvenLightsTargetPower" in read("lib/master/commands.ts") and "Poder do adversário" in read("components/CombatRulesPanel.tsx"),
    "elven-lights-surprise": "elven-light-no-action-round:" in read("lib/master/commands.ts") and "elven-light-weary-through:" in read("lib/master/commands.ts"),
    "herbal-journey-end": "record_journey_herbal_roll" in read("lib/master/commands.ts") and "herbalRecoveryByHero" in read("lib/master/types.ts") and "Remédios Herbais" in read("components/JourneyResolutionPanel.tsx"),
    "beauty-stars-council": "apply_beauty_of_stars" in read("lib/master/commands.ts") and "set_council_spokesperson" in read("lib/master/commands.ts") and "Véu de memória" in read("components/SupplementRulesPanel.tsx"),
    "artificer-study": "artificer_study_item" in read("lib/master/commands.ts") and "artificerStudyUsedPhaseStartedAt" in read("lib/db.ts"),
    "artificer-next-yule-once": "artificerYuleReady" in read("lib/master/commands.ts") and "artificerAcquiredPhaseStartedAt" in read("lib/db.ts") and "artificerYuleReady:false" in read("lib/master/commands.ts"),
    "magical-item-metadata": "magicalKind" in read("lib/db.ts") and "magicalQualities" in read("lib/db.ts") and "magicalIdentified" in read("lib/db.ts"),
    "eye-magic-hooks": "Luzes Élficas" in read("lib/master/commands.ts") and "Beleza das Estrelas" in read("lib/master/commands.ts") and "raiseEyeAwareness(1" in read("lib/master/commands.ts"),
    "tts-preserved": "Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
    "no-data-clear": "indexedDB.deleteDatabase" not in whole and "clearApplicationUserData" not in whole and "pm clear" not in whole,
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4940_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.40_STATEFUL_SUPPLEMENTS_OK")
