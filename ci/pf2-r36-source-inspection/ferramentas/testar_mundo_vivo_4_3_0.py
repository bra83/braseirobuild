from pathlib import Path
import json
import re
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
java = root / "android/app/src/main/java/com/braseiro/pathfinder2e"
db = (java / "AppDb.java").read_text("utf-8")
engine = (java / "LivingWorldEngine.java").read_text("utf-8")
policy = (java / "LivingWorldRulePolicy.java").read_text("utf-8")
main = (java / "MainActivity.java").read_text("utf-8")
context = (java / "MasterGameContext.java").read_text("utf-8")
orchestrator = (java / "MasterOrchestrator.java").read_text("utf-8")

checks = []
def check(label, condition):
    checks.append((label, bool(condition)))
    print(("OK  " if condition else "FALHA ") + label)

version_text = (root / "version.properties").read_text("utf-8")
version_match = re.search(r"VERSION_NAME=(\d+)\.(\d+)\.(\d+)", version_text)
version_tuple = tuple(map(int, version_match.groups())) if version_match else (0, 0, 0)
db_match = re.search(r"DB_VERSION\s*=\s*(\d+)", db)
db_version = int(db_match.group(1)) if db_match else 0
check("versão 4.3.3+", version_tuple >= (4, 3, 3))
check("migração de banco 47+", db_version >= 47 and "oldVersion < 42" in db and "oldVersion < 47" in db)
for table in ("living_world_state", "hero_life_profiles", "life_relationships", "life_activities", "living_troupes"):
    check("tabela " + table, ("CREATE TABLE IF NOT EXISTS " + table) in db)
for key in ("livingWorldState", "heroLifeProfiles", "lifeRelationships", "lifeActivities", "livingTroupes"):
    check("backup " + key, ('root.put("' + key + '"') in db and ('root.has("' + key + '")' in db or ('root.optJSONObject("' + key + '")' in db)))
check("remapeamento de heróis no restore", "restoreLivingRows" in db and "characterIdMap.containsKey" in db)
check("tela Mundo Vivo", '"Mundo Vivo"' in main and "showLivingWorld()" in main)
check("calendário explícito", "mundo_vivo_avanco_confirmado" in (java / "WorldStateEngine.java").read_text("utf-8").lower())
check("snapshot antes do avanço", 'beforeDestructiveChange("avançar o calendário do Mundo Vivo")' in main)
check("idade sem efeito mecânico", "age_is_narrative_only" in engine and "ageYears" in policy)
check("familiares também envelhecem", "related_birth_day" in db and 'row.put("age_years"' in engine)
check("atividade narrativa sem prêmio", "mechanics_applied\",false" in engine and "não concede automaticamente XP" in main)
check("mecânica exige fonte completa", all(x in policy for x in ("authorized&&page>0", "excerpt.trim().length()>=8", "sourceBelongsToWorld")))
check("fonte precisa estar na biblioteca e na página local", "verifyEnabledManualExcerpt" in db and engine.count("db.verifyEnabledManualExcerpt") >= 3 and "manual_local_pages" in db)
check("prefixo Mystara exato", 'ManualIdentityPolicy.isMystaraTitle(name)' in policy and 'looksLikeMalformedMystaraTitle' in policy)
check("Mestre recebe Mundo Vivo", '"living_world"' in context and '"living_world"' in orchestrator and "jamais concedem XP" in orchestrator)

map_zip = root / "android/app/src/main/assets/maps/faerun_maxres_integrated.zip"
try:
    with zipfile.ZipFile(map_zip) as archive:
        check("ZIP Faerûn íntegro", archive.testzip() is None)
        names = set(archive.namelist())
        manifest_name = next(n for n in names if "manifest" in n.lower() and n.endswith(".json"))
        manifest = json.loads(archive.read(manifest_name))
        atlas = json.loads(archive.read(manifest["dataFiles"]["terrainAtlas"]))
        check("mapa Faerûn completo", archive.getinfo(manifest["sourceMap"]["filename"]).file_size == 143172800)
        check("atlas conservador", len(atlas["hexes"]) == 59334 and all(v["review"]["status"] == "needs_review" for v in list(atlas["hexes"].values())[:100]))
except Exception as exc:
    check("pacote Faerûn legível: " + str(exc), False)

failed = [name for name, ok in checks if not ok]
print(f"RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.")
if failed:
    sys.exit(1)
