#!/usr/bin/env python3
"""Validação estrutural sem dependências externas da implantação Kingmaker 9.0.0."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

def read(name):
    return (SRC / name).read_text(encoding="utf-8")

rules = read("KingmakerRulesEngine.java")
repo = read("KingmakerRepository.java")
service = read("KingmakerService.java")
main = read("MainActivity.java")
db = read("AppDb.java")
backup = read("BackupContractValidator.java")
atlas = read("MapBoardView.java")
map_manager = read("MapPackageManager.java")
master = read("MasterGameContext.java") + read("MasterOrchestrator.java")
truth = read("SourceOfTruthRegistry.java")
props = (ROOT / "version.properties").read_text(encoding="utf-8")
catalog_path = ROOT / "android/app/src/main/assets/pathfinder/kingmaker/kingmaker_catalog.json"
manifest_path = ROOT / "android/app/src/main/assets/pathfinder/system_manifest.json"
catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

checks = []

def require(label, condition):
    if not condition:
        raise AssertionError(label)
    checks.append(label)

require("versão 9.0.1 ou sucessora", any((f"VERSION_NAME=9.0.{n}" in props and f"VERSION_CODE={90000+n}" in props) for n in range(1,100)))
require("schema 59", "DB_SCHEMA=59" in props and "DB_VERSION = 59" in db)
require("backup v22", "BACKUP_FORMAT=22" in props and '.put("version",22)' in db and "MAX_SUPPORTED_VERSION=22" in backup)

for table in ("kingmaker_state", "kingmaker_skills", "kingmaker_leaders", "kingmaker_features", "kingmaker_hexes", "kingmaker_settlements", "kingmaker_structures", "kingmaker_turns", "kingmaker_turn_activities", "kingmaker_events", "kingmaker_armies", "kingmaker_war_encounters", "kingmaker_companions", "kingmaker_camps", "kingmaker_trade_agreements"):
    require("tabela " + table, "CREATE TABLE IF NOT EXISTS " + table in repo)

require("16 perícias", rules.count("SKILL_ABILITIES.put(") == 16)
require("habilidades corrigidas", 'SKILL_ABILITIES.put("engineering","stability")' in rules and 'SKILL_ABILITIES.put("exploration","economy")' in rules)
require("CDs de Controle 1–20", "14,15,16,18,20,22,23,24,26,27,28,30,31,32,34,35,36,38,39,40" in rules)
require("dados de recurso oficiais", "kingdomLevel))+4" in rules and "value>=100?12:value>=50?10:value>=25?8:value>=10?6:4" in rules)
require("reserva de XP preservada no limite do grupo", 'if(level>=cap)xp=Math.min(999,xp)' not in repo and 'if(nextLevel>=20)nextXp=Math.min(999,nextXp)' not in rules)
require("Tamanho e armazenamento", "sizeControlModifier" in rules and "commodityStorage" in rules)
require("Unrest por limiares", "value>=15?4:value>=10?3:value>=5?2:value>=1?1:0" in rules)
require("Consumo oficial na Manutenção", "consumptionSummary" in repo and "resolveConsumption" in repo and "influencedFarmlandCount" in repo and "Resolva o Consumo antes de concluir a Manutenção." in repo and "RESOLVER CONSUMO" in main)
require("assentamentos oficiais", all(token in rules for token in ("minimum_kingdom_level", "population_floor", "consumption", "settlementInfluence")))
require("grade urbana e superlotação", all(('ensureColumn(sql,"kingmaker_settlements","' + token + '"') in repo for token in ("developed_blocks", "occupied_lots", "residential_lots", "overcrowded", "max_item_bonus")) and "A categoria do assentamento só pode avançar uma etapa por vez." in repo and "A estrutura excede os " in repo)
require("exército derrotado a zero PV", '.put("defeated",next<=0)' in rules and '.put("destroyed",false)' in rules)
require("influência sem atitude inventada", "source_specific_thresholds" in rules and 'v.put("attitude",result.optString("attitude"))' not in repo)

for method in ("configure", "startTurn", "advancePhase", "completeTurn", "recordCheck", "initializeFoundingHex", "claimHex", "addSettlement", "addStructure", "addEvent", "addArmy", "startWarEncounter", "addCompanion", "startCamp", "recordCampHour", "completeCamp", "addTradeAgreement", "exportJson", "restoreJson"):
    require("método " + method, (" " + method + "(") in repo)

require("hex fundador e capital aldeia", "initializeFoundingHex" in service and 'String settlementType="village"' in service)
require("Carta/Heartland/Governo persistidos", service.count("persistFoundationChoice(") >= 4)
require("progressão no contexto do Mestre", '.put("features",new JSONArray(features("")))' in repo and 'root.put("kingmaker"' in master)
require("Atlas somente leitura com overlay", "drawKingmakerTerritory" in atlas and "setKingmakerHexes" in atlas and "canonical Atlas remains read-only" in truth)

for ui_method in ("showKingmakerSetupDialog", "showKingmakerFeaturesDialog", "showStartKingdomTurnDialog", "showKingdomCheckDialog", "showKingmakerHexDialog", "showKingmakerSettlementsDialog", "showKingmakerArmiesDialog", "showKingmakerWarEncountersDialog", "showKingmakerCompanionsDialog", "showKingmakerCampsDialog", "showKingmakerCampHourDialog"):
    require("UI " + ui_method, (" " + ui_method + "(") in main)

require("vacância do Governante na UI", "Governante vago — 1d4 automático" in main and "startTurn(values,rulerVacant?" in main)
require("Kingmaker solo como fluxo principal", "KINGMAKER SOLO ATIVO" in main and "showKingmakerSoloActivationDialog" in main and "showKingmakerSoloHome" in main and "ABRIR ADMINISTRAÇÃO AVANÇADA" in main)
require("números administrativos ocultos e automatizados", "Rolagem automática de " in main and "O QUE O REINO FARÁ?" in main and "Comércio, RP, CDs e cálculos do Reino não são perguntas para você." in main)
require("escolhas narrativas Kingmaker solo", "BARBARA NARRA • VOCÊ ESCOLHE" in main and "ESCOLHAS — O QUE VOCÊ FAZ?" in main)
require("Atlas V6 aceito pelo importador runtime", "expectedWidth=v6?4270f:2135f" in map_manager and "expectedHeight=v6?5388f:2720f" in map_manager and "shieldwulf_hexmap_rectified_v1" in map_manager)
require("categorias de atividade", all(token in main for token in ('"leadership"', '"region"', '"civic"', '"commerce"', '"upkeep"', '"warfare"')))
require("camping oficial na UI", "CD inicial de encontro" in main and "Prepare Camp" in main and "showKingmakerCampHourDialog" in main)
require("sem qualidade de camping inventada", "quality_levels" not in catalog and 'optString("quality")' not in main)
require("marcos oficiais de XP", catalog.get("milestones", {}).get("first_landmark") == 40 and catalog.get("milestones", {}).get("first_trade_agreement") == 80 and catalog.get("milestones", {}).get("spent_100_rp_in_turn") == 80)
require("eventos e XP idempotentes", 'Este evento já foi resolvido.' in repo and '"random".equals(key(old.optString("event_type")))' in repo and catalog.get("events", {}).get("random_event_xp") == 30)
require("Landmark e Refuge persistentes", 'ensureColumn(sql,"kingmaker_hexes","landmark"' in repo and 'ensureColumn(sql,"kingmaker_hexes","refuge"' in repo and "Landmark oficial neste hex" in main)
require("backup inclui features", "kingmakerFeatures" in repo and "kingmakerFeatures" in backup)
require("catálogo Kingmaker válido", catalog.get("schema") == "braseiro-pathfinder2e-kingmaker-catalog-v1" and len(catalog.get("skills", [])) == 16)
require("manifest declara Kingmaker", manifest.get("kingmaker", {}).get("schema") == 1 and "kingdom creation and advancement" in manifest["kingmaker"]["subsystems"])

print("OK: Kingmaker 9.0.x passou por", len(checks), "portões estruturais sem dependências externas.")
