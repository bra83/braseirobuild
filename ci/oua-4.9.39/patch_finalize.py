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
data["version"]="4.9.39"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.38"','"version": "4.9.39"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.38'","APP_VERSION = '4.9.39'",t)
t=re.sub(r"APP_VERSION_CODE = 49038","APP_VERSION_CODE = 49039",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49038","versionCode = 49039",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.38"','versionName = "4.9.39"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49038&immersive=1","index.html?v=49039&immersive=1")
write(rel,t)

checks={
  "supplement-rules-data": all(x in read("lib/supplementRules.ts") for x in ["Alto Elfo de Valfenda","Beorning","Elfo da Floresta das Trevas","Homem da Floresta das Terras Ermas","Elrond Meio-Elfo"]),
  "strider-authority": "a==='solo-2e'" in read("lib/sourceAuthority.ts"),
  "strider-tn18": "base = sheet.other.rulesMode === 'strider' ? 18 : 20" in read("lib/characterRules.ts"),
  "strider-xp15": "previousExperienceBudget=rulesMode==='strider'?15:10" in read("lib/characterFactory.ts"),
  "strider-feature": "features.push('Peregrino')" in read("lib/characterFactory.ts"),
  "strider-tables": all(x in read("lib/rules/striderMode.ts") for x in ["resolveNarrationTable","striderLore","striderFortune","striderMisfortune","STRIDER_JOURNEY_DETAILS"]),
  "strider-skirmish": "stance==='skirmish'" in read("lib/rules/combat.ts") and "context.escaping&&actor.stance==='skirmish'?0" in read("lib/rules/combat.ts"),
  "supplement-passives": all(x in read("lib/rules/supplementMechanics.ts") for x in ["Bolos de Mel Duas Vezes Assados","Sonhos Élficos","Luzir de Ira","A Resolução do Caçador","Remédios Herbais"]) and "isCulture(sheet,'beornings') && sheet.attributes.wounded" in read("lib/rules/supplementMechanics.ts"),
  "journey-explicit-preserved": "Decisão de Jornada bloqueada no fluxo automático" in read("lib/master/commands.ts"),
  "journey-hope-ui": "Gastar 1 Esperança" in read("components/JourneyResolutionPanel.tsx"),
  "notable-event-ui": "Encontro Notável" in read("components/JourneyResolutionPanel.tsx"),
  "mechanics-ui": "SupplementRulesPanel" in read("components/MechanicsHub.tsx"),
  "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
  "tts-preserved": "Braseiro Kokoro PT-BR · neural" in read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"),
  "version": "4.9.39" in read("lib/version.ts") and "49039" in read("android/app/build.gradle.kts"),
  "magical-success-result": "rollMagicalSuccess" in read("lib/dice.ts") and "magicalSuccess?: boolean" in read("lib/dice.ts"),
  "magical-success-ui": "Gastar 1 Esperança · Sucesso Mágico" in read("components/DiceView.tsx") and "Sucesso Mágico" in read("components/JourneyResolutionPanel.tsx"),
  "eldar-auto-magical": "gandalfIsAutomaticMagicalSuccess" in read("components/DiceView.tsx"),
  "virtue-max-effects": "supplementMaxHopeBonus" in read("lib/rules/progression.ts") and "supplementMaxEnduranceBonus" in read("lib/rules/progression.ts"),
  "beorn-choice": "set_beorn_enchanted_skills" in read("lib/master/commands.ts") and "Salvar 3 Perícias" in read("components/SupplementRulesPanel.tsx"),
  "natural-watchfulness-role": "naturalWatchfulnessIds" in read("lib/rules/journey.ts"),
  "elrond-undertaking": "meet-patron" in read("lib/master/commands.ts") and "Encontrar Patrono · Elrond" in read("components/MechanicsHub.tsx"),
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4939_FINALIZE_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.39_FINALIZE_OK")
