from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

def read(name):
    return (SRC / name).read_text(encoding="utf-8")

RULES = read("KingmakerRulesEngine.java")
REPO = read("KingmakerRepository.java")
SERVICE = read("KingmakerService.java")
MAIN = read("MainActivity.java")
DB = read("AppDb.java")
BACKUP = read("BackupContractValidator.java")

def test_official_kingdom_tables_and_skill_abilities_are_embedded():
    assert "14,15,16,18,20,22,23,24,26,27,28,30,31,32,34,35,36,38,39,40" in RULES
    assert 'SKILL_ABILITIES.put("engineering","stability")' in RULES
    assert 'SKILL_ABILITIES.put("exploration","economy")' in RULES
    assert RULES.count("SKILL_ABILITIES.put(") == 16
    assert "kingdomLevel))+4" in RULES
    assert 'if(level>=cap)xp=Math.min(999,xp)' not in REPO
    assert 'if(nextLevel>=20)nextXp=Math.min(999,nextXp)' not in RULES

def test_database_and_backup_cover_every_kingmaker_aggregate():
    for table in ("kingmaker_state", "kingmaker_skills", "kingmaker_leaders", "kingmaker_features", "kingmaker_hexes", "kingmaker_settlements", "kingmaker_structures", "kingmaker_turns", "kingmaker_turn_activities", "kingmaker_events", "kingmaker_armies", "kingmaker_war_encounters", "kingmaker_companions", "kingmaker_camps", "kingmaker_trade_agreements"):
        assert "CREATE TABLE IF NOT EXISTS " + table in REPO
    assert '.put("version",22)' in DB
    assert "MAX_SUPPORTED_VERSION=22" in BACKUP
    assert "kingmakerFeatures" in REPO and "kingmakerFeatures" in BACKUP

def test_foundation_turns_atlas_settlements_and_warfare_are_wired():
    for token in ("initializeFoundingHex", "startTurn", "advancePhase", "completeTurn", "recordCheck", "claimHex", "addSettlement", "addStructure", "startWarEncounter"):
        assert " " + token + "(" in REPO
    assert 'String settlementType="village"' in SERVICE
    assert '.put("defeated",next<=0)' in RULES
    assert '.put("destroyed",false)' in RULES
    assert 'ensureColumn(sql,"kingmaker_hexes","landmark"' in REPO
    assert 'ensureColumn(sql,"kingmaker_hexes","refuge"' in REPO
    for column in ("developed_blocks", "occupied_lots", "residential_lots", "overcrowded", "max_item_bonus"):
        assert f'ensureColumn(sql,"kingmaker_settlements","{column}"' in REPO
    assert "A categoria do assentamento só pode avançar uma etapa por vez." in REPO
    assert "A estrutura excede os " in REPO

def test_official_xp_awards_and_reserve_are_implemented_once():
    catalog = json.loads((ROOT / "android/app/src/main/assets/pathfinder/kingmaker/kingmaker_catalog.json").read_text(encoding="utf-8"))
    milestones = catalog["milestones"]
    assert milestones["first_landmark"] == 40
    assert milestones["first_trade_agreement"] == 80
    assert milestones["spent_100_rp_in_turn"] == 80
    assert milestones["random_event"] == 30
    assert "grantMilestoneOnce(\"first_trade_agreement\",80" in REPO
    assert "grantMilestoneOnce(\"spent_100_rp_turn\",80" in REPO
    assert '"random".equals(key(old.optString("event_type")))' in REPO
    assert 'throw new IllegalStateException("Este evento já foi resolvido.")' in REPO

def test_upkeep_consumption_overcrowding_and_farmland_are_enforced():
    assert "consumptionSummary" in REPO
    assert "resolveConsumption" in REPO
    assert "influencedFarmlandCount" in REPO
    assert 'overcrowded=1' in REPO
    assert "Resolva o Consumo antes de concluir a Manutenção." in REPO
    assert "RESOLVER CONSUMO" in MAIN
    assert 'ensureColumn(sql,"kingmaker_armies","consumption"' in REPO

def test_companion_and_camping_subsystems_are_fully_implemented():
    assert "source_specific_thresholds" not in RULES
    assert 'v.put("attitude"' in REPO
    assert "companionCatalog" in RULES
    assert "companionProfile" in RULES
    assert "campZonesCatalog" in RULES
    assert "campsiteSelection" in RULES
    assert "prepareCamp" in RULES
    assert "campingActivitiesCatalog" in RULES
    assert "recipesCatalog" in RULES
    assert "resolveCookingMeal" in RULES
    assert "resolveSleepAndRest" in RULES
    assert "aggregateCampEffects" in RULES
    assert "recordCampHour" in REPO
    assert "current_encounter_dc" in REPO
    assert "quality_levels" not in json.loads((ROOT / "android/app/src/main/assets/pathfinder/kingmaker/kingmaker_catalog.json").read_text(encoding="utf-8")).get("camping", {})
    assert 'optString("quality")' not in MAIN

def test_mobile_ui_exposes_all_kingmaker_domains():
    for method in ("showKingmakerFeaturesDialog", "showKingmakerLeadershipDialog", "showKingmakerHexDialog", "showKingmakerSettlementsDialog", "showKingmakerArmiesDialog", "showKingmakerWarEncountersDialog", "showKingmakerCompanionsDialog", "showKingmakerCampsDialog", "showKingmakerCampHourDialog"):
        assert method in MAIN
    assert "Governante vago — 1d4 automático" in MAIN
    assert "CATEGORIA DA ATIVIDADE" in MAIN

def test_kingmaker_solo_is_the_primary_flow_and_hides_the_admin_sheet():
    assert "KINGMAKER SOLO ATIVO" in MAIN
    assert "showKingmakerSoloActivationDialog" in MAIN
    assert "showKingmakerSoloHome" in MAIN
    assert "CONTINUAR KINGMAKER SOLO" in MAIN
    assert "Comércio, RP, CDs e cálculos do Reino não são perguntas para você." in MAIN
    assert 'putBoolean("kingmaker_admin_mode",false)' in MAIN
    assert "ABRIR ADMINISTRAÇÃO AVANÇADA" in MAIN
    assert "Rolagem automática de " in MAIN
    assert "O QUE O REINO FARÁ?" in MAIN
