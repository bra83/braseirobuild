#!/usr/bin/env python3
from pathlib import Path
import hashlib
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
ASSETS = ROOT / "android/app/src/main/assets"

files = {}
def src(name):
    if name not in files:
        files[name] = (JAVA / name).read_text("utf-8")
    return files[name]

checks = []
def check(label, condition):
    ok = bool(condition)
    checks.append((label, ok))
    print(("OK    " if ok else "FALHA ") + label)

version = (ROOT / "version.properties").read_text("utf-8")
db = src("AppDb.java")
main = src("MainActivity.java")
master_ctx = src("MasterGameContext.java")
master = src("MasterOrchestrator.java")
validator = src("MasterResponseValidator.java")
arbiter = src("MasterRulesArbiter.java")
world = src("WorldStateEngine.java")
sim = src("LivingWorldSimulationEngine.java")
catchup = src("LivingWorldCatchUpEngine.java")
events = src("WorldEventConsequenceEngine.java")
living = src("LivingWorldEngine.java")
options = src("SceneOptionDiversifier.java")
transactions = src("StateTransactionManager.java")
backup_validator = src("BackupContractValidator.java")

# Versão e migração
version_values=dict(line.split("=",1) for line in version.splitlines() if "=" in line)
version_tuple=tuple(int(part) for part in version_values.get("VERSION_NAME","0.0.0").split(".")[:3])
check("versão 4.4.0+", version_tuple >= (4,4,0))
check("versionCode 4.4.0+", int(version_values.get("VERSION_CODE","0")) >= 440000)
check("backup v18", ("BACKUP_FORMAT=18" in version or "BACKUP_FORMAT=19" in version or "BACKUP_FORMAT=20" in version) and ('put("version",18)' in db or 'put("version",19)' in db or 'put("version",20)' in db) and ("backupVersion>18" in db or "backupVersion>19" in db or "backupVersion>20" in db))
check("banco v51+ com migrações preservadas", bool(re.search(r"DB_VERSION\s*=\s*(?:5[1-9]|[6-9][0-9])", db)) and "oldVersion < 42" in db and "oldVersion < 47" in db and "oldVersion < 49" in db)
for col in (
    "background_simulation_enabled", "background_game_hours_per_real_day", "background_max_hours",
    "resolution_json", "applied_at", "itinerary_index", "plan_steps_json", "plan_interval_hours",
    "interval_hours", "trigger_condition", "consequence_status", "last_action_day", "option_history_json"
):
    check("coluna/migração " + col, col in db)

# Mundo Vivo
check("simulador local sem IA", "class LivingWorldSimulationEngine" in sim and "Gemini" not in sim)
check("Mundo Vivo respeita enabled", 'settings.optInt("enabled",1)==1' in sim and 'report.put("living_world_enabled",livingEnabled)' in world)
check("facções executam planos", all(t in sim for t in ("previewFactions", "plan_steps_json", "applyFactionSimulationStep", "plan_interval_hours")))
check("planos de facção limitam retomada", ("Math.min(executable,24)" in sim) or ("Math.min(executable,budget.stepsPerFaction)" in sim and "stepsPerFaction=4" in (JAVA/"WorldSimulationBudget.java").read_text()))
check("NPCs executam agenda fora de cena", all(t in sim for t in ("previewNpcs", "applyNpcs", "scheduled", "last_action_day")))
check("ações mecânicas de NPC/facção vão para revisão", sim.count("addPendingTransaction") >= 2 and "requires_master_review" in sim)
check("atividades passam por estados reais", all(t in sim for t in ("aguardando_aplicacao", "aguardando_regra", "concluida")))
check("itinerário da trupe avança", all(t in sim for t in ("previewTroupe", "itinerary_index", "applyTroupe")))
check("sincronização offline opcional", "class LivingWorldCatchUpEngine" in catchup and "background_simulation_enabled" in catchup)
check("retomada offline tem taxa e limite", all(t in catchup for t in ("background_game_hours_per_real_day", "background_max_hours", "Math.min(max,gameHours)")))
check("retomada exige aventura iniciada", 'adventure_not_started' in catchup and 'getStoryState().optInt("started",0)!=1' in catchup)
check("retomada reutiliza motor transacional", "world.preview" in catchup and "world.commit" in catchup)
check("retomada ligada na inicialização", "runLivingWorldCatchUpAsync" in main and "beforeDestructiveChange" in main)
check("relógios possuem cadência própria", all(t in world for t in ("interval_hours", "accumulated_hours", "updateWorldClockSimulation")))
check("relógios possuem condição própria", "clockConditionMatches" in world and "trigger_condition" in world)
check("eventos não são encerrados sem consequência", "WorldEventConsequenceEngine" in world and "applyTriggered" in world and "markWorldEventPendingReview" in events)
check("consequências estruturais de evento são aplicadas", all(t in events for t in ("knowledge_updates", "quest_updates", "faction_updates", "clock_updates", "world_events", "npc_updates", "journal_updates", "map_updates", "tactical_updates")))
check("efeitos mecânicos de evento são bloqueados", all(t in events for t in ('"state_updates"', '"combat"', '"xp"', '"gold"', '"damage"', '"healing"')))
check("resolução de evento é idempotente", "status='scheduled'" in db and "status='awaiting_review'" in db and "resolveWorldEventAfterReview" in db)
check("pendência de evento é encerrada junto", "resolvePendingEffectChecked" in master and "_world_event_id" in master)

# Atividades mecânicas e regras
check("atividade mecânica tem autorização", "authorizeMechanicalOutcome" in living and "rule_authorized" in living)
check("atividade aplica resultado idempotente", "applyAuthorizedActivityOutcome" in living and '"idempotent",true' in living and "applied_at=0" in living)
check("atividade altera ficha e recursos", all(t in living for t in ("updateCharacterSheet", "setCharacterEquipmentAndNotes", "provisions_days", "water_days")))
check("manual exige página local verificável", "verifyEnabledManualExcerpt" in db and "manual_local_pages" in db and "manualExcerptOverlap" in db)
check("árbitro usa prova textual local", "db.verifyEnabledManualExcerpt(book,page,excerpt)" in arbiter)
check("atividade usa prova textual local", living.count("db.verifyEnabledManualExcerpt") >= 3)
check("mecânica sem prova é bloqueada", "blockedMechanics" in arbiter and "Alteração mecânica" in arbiter and "foi bloqueada" in arbiter)
check("rolagem local exige vínculo exato", "verifyBoundLocalRoll" in arbiter and "MechanicalRollBinding.verify" in arbiter and "getDiceHistoryById" in arbiter)
check("combate exige base mecânica", "validateCombat" in arbiter and "mechanical_basis" in arbiter and "enemyStats" in arbiter)
check("teste pendente adia todas as mutações", "_deferred_until_test" in arbiter and all(k in arbiter for k in ("map_updates", "tactical_updates", "journal_updates", "event_resolutions")))
check("relatório distingue verificado/bloqueado", "verified_mechanics" in arbiter and "blocked_mechanics" in arbiter)

# Barramento do Mestre
for key in (
    "active_heroes_full", "scene", "combat", "combatants", "map", "tactical_tokens", "recent_journal",
    "recent_dice", "npcs", "npc_cognition", "knowledge_facts", "quests", "factions", "world_clocks",
    "world_events", "living_world", "party_state", "travel_rules_state", "pending_transactions"
):
    check("game_bus contém " + key, ('"' + key + '"') in master_ctx)
check("Mestre envia game_bus em toda cena", ('gameContext.narratorSnapshot' in master and 'root.put("game_bus",bus)' in master))
check("compactação preserva game_bus", '"game_bus"' in master[master.index("fitContext"):])
check("compactação emergencial preserva game_bus", 'emergency.put("game_bus",criticalGameBus' in master and 'all_systems_preserved' in master)
check("fichas completas incluem sheet/equipment/spells", all(t in master_ctx for t in ('x.put("sheet"', '"equipment"', '"spells"')))
check("manifesto de leitura/escrita", "connection_manifest" in master_ctx and "write_after_review" in master_ctx)
check("auditoria mede barramento usado", all(t in master_ctx for t in ("narrator_bus_connected", "full_sheets_in_narrator_bus", "map_bidirectional_connected", "tactical_bidirectional_connected", "journal_bidirectional_connected")))
check("cache narrativo desativado", "if(plan==null||plan.narrative)return \"\"" in master and "if(plan==null||plan.narrative||raw==null" in master)

# Escrita integrada
for channel in ("map_updates", "tactical_updates", "journal_updates", "event_resolutions"):
    check("canal estruturado " + channel, channel in validator and channel in master and channel in main and channel in transactions)
check("contratos D&D e Mystara incluem integração", main.count('\\"map_updates\\":[],\\"tactical_updates\\":[],\\"journal_updates\\":[]') >= 3)
check("mapa valida hex do atlas", "getRawMapHex" in validator and "não existe no atlas ativo" in validator)
check("rota remove hex inválido", "Um hex inexistente foi removido da rota" in validator)
check("movimento tático exige combatente real", "findCombatantById" in validator and "combatente não existe" in validator)
check("planos de facção são saneados", "sanitizeFactionPlans" in validator and "Um plano de facção inválido" in validator)
check("revisão permite desligar canais separadamente", all(t in main for t in ('values.containsKey("map_updates")', 'values.containsKey("tactical_updates")', 'values.containsKey("journal_updates")')))

# Sugestões
check("histórico de opções pertence ao mundo", "getSceneOptionHistory" in db and "saveSceneOptionHistory" in db and "world_profile" in db[db.index("saveSceneOptionHistory"):db.index("clearStoryState")])
check("histórico entra no backup/restauração", 'root.put("storyState"' in db and 'saveSceneOptionHistory(ss.optString("option_history_json","[]"))' in db)
check("streaming passa pelo diversificador", "finalizeStreamingNarrationNow" in main and "SceneOptionDiversifier.diversify(split.options" in main)
check("janela cobre 24 opções", "MAX_HISTORY = 24" in options)
check("filtro lexical continua ativo", "jaccard >= 0.68" in options and "containment >= 0.82" in options)
check("filtro por intenção usa limite de palavra correto", options.count('\\\\b(') >= 8 and 'intentSignature' in options)
check("famílias de intenção cobertas", all(t in options for t in ('return "conversar"', 'return "investigar"', 'return "confrontar"', 'return "recuar"', 'return "mover"', 'return "negociar"', 'return "furtividade"', 'return "descansar"')))

# Backup e documentação
check("backup restaura histórico de opções", 'saveSceneOptionHistory(ss.optString("option_history_json","[]"))' in db)
check("validador aceita backup 18", "MAX_SUPPORTED_VERSION=18" in backup_validator or "MAX_SUPPORTED_VERSION=19" in backup_validator or "MAX_SUPPORTED_VERSION=20" in backup_validator or "version>18" in backup_validator or "version>19" in backup_validator or "version>20" in backup_validator)
for doc in ("CHANGELOG_4.4.0.md", "documentacao/AUDITORIA_E_VALIDACAO_4.4.0.md", "LEIA_PRIMEIRO.txt", "README.md"):
    check("documento " + doc, (ROOT / doc).is_file())

# Assets
pdf = ASSETS / "manuals/Mythic 2e.pdf"
check("Mythic 2e completo por tamanho", pdf.is_file() and pdf.stat().st_size == 42592266)
check("Mythic 2e SHA-256 conhecido", pdf.is_file() and hashlib.sha256(pdf.read_bytes()).hexdigest() == "edf8b571db51edcb8071d230beb0759db917569f72a4c2e0bdd26c8cbccb24f1")
try:
    from pypdf import PdfReader
    reader = PdfReader(str(pdf))
    check("Mythic 2e tem 230 páginas", len(reader.pages) == 230)
except Exception as exc:
    check("Mythic 2e legível: " + str(exc), False)

map_hashes = {
    "faerun_maxres_integrated.zip": "21b2eca2305133550663b7f2924815fe3692a333c33aef097cbdca4a531efeb7",
    "mystara_known_world_integrated.zip": "6dd275487be249fab1dbebc61a66cff4fd0f928fbdac648624a4d91fecd469b9",
}
for name, expected in map_hashes.items():
    path = ASSETS / "maps" / name
    check(name + " SHA-256 preservado", path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest() == expected)
    try:
        with zipfile.ZipFile(path) as zf:
            check(name + " íntegro", zf.testzip() is None)
    except Exception:
        check(name + " íntegro", False)

failed = [label for label, ok in checks if not ok]
print(f"RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.")
if failed:
    print("FALHAS:")
    for label in failed:
        print(" -", label)
    sys.exit(1)
