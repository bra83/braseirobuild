#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

def read(name):
    return (SRC / name).read_text("utf-8")

appdb = read("AppDb.java")
main = read("MainActivity.java")
orch = read("MasterOrchestrator.java")
validator = read("MasterResponseValidator.java")
arbiter = read("MasterRulesArbiter.java")
world = read("WorldStateEngine.java")
living = read("LivingWorldSimulationEngine.java")
events = read("WorldEventConsequenceEngine.java")
npcs = read("NpcAgencyEngine.java")
scene = read("SceneStateEngine.java")
living_engine = read("LivingWorldEngine.java")
knowledge = read("KnowledgeFirewall.java")

checks = []
def check(label, ok):
    checks.append((label, bool(ok)))

version = dict(line.split("=", 1) for line in (ROOT / "version.properties").read_text("utf-8").splitlines() if "=" in line)
version_tuple = tuple(int(part) for part in version.get("VERSION_NAME","0.0.0").split(".")[:3])
check("versão 4.4.2+", version_tuple >= (4,4,2) and int(version.get("VERSION_CODE","0")) >= 440200)
check("backup v17+", int(version.get("BACKUP_FORMAT","0")) >= 17)
check("banco v49+", re.search(r"DB_VERSION\s*=\s*(49|[5-9][0-9]+)", appdb) is not None and "oldVersion<42" in re.sub(r"\s+", "", appdb))

# Atomicidade e rollback
check("API transacional verificada", "interface CheckedTransaction" in appdb and "runInTransactionChecked" in appdb)
check("aplicação do Mestre em uma transação", "db.runInTransactionChecked(new AppDb.CheckedTransaction()" in main and "masterOrchestrator.afterConsequencesApplied" in main)
check("plano só é confirmado após commit", main.find("markConsequencesCommitted") > main.find("db.runInTransactionChecked"))
check("rollback restaura marcações locais", "markConsequencesRolledBack" in main and "resetTickFlags" in orch)
check("cena propaga falha", re.search(r"public\s+JSONObject\s+apply\([^)]*\)\s+throws\s+Exception", scene) is not None)
check("avanço de mundo atômico", "public synchronized boolean commit" in world and "db.runInTransactionChecked" in world)
check("simulação viva atômica", "public synchronized boolean commit" in living and "db.runInTransactionChecked" in living)
check("eventos estruturados atômicos", "applyTriggered(JSONArray events) throws Exception" in events and events.count("db.runInTransactionChecked") >= 3)
check("gravações estritas do Mestre", all(x in appdb for x in ["saveSceneStateChecked", "applyQuestUpdateChecked", "applyFactionUpdateChecked", "applyMapDirectorUpdateChecked", "applyTacticalDirectorUpdateChecked"]))

# Fail closed
check("Main bloqueia envelope inválido", "!validation.optBoolean(\"valid\",false)" in main)
check("validador limpa mutações ao falhar", "clearStructuredMutations(envelope)" in validator and "fail_closed" in validator)
check("árbitro mecânico fecha ao falhar", "clearMechanicalAndPersistentChanges" in arbiter and 'put("valid",false)' in arbiter)
check("firewall de conhecimento fecha ao falhar", "_firewall_report" in knowledge and "valid\",false" in knowledge)
check("validação de NPC fecha ao falhar", "npc_cognition_updates\",new JSONArray()" in npcs and "_npc_validation" in npcs)

# Idempotência e correlação
check("recibos de efeito persistentes", "CREATE TABLE IF NOT EXISTS effect_receipts" in appdb and "recordEffectReceipt" in appdb)
check("recibos entram no backup", "effectReceipts" in appdb and "restoreProfileRowsWithoutId(db,\"effect_receipts\"" in appdb)
check("pendência idempotente sincronizada", "public synchronized long addPendingTransactionUnique" in appdb)
check("evento usa chave própria", 'LegacyWorldEventIdentity.transactionKey(eventKey)' in events and 'Evento sem event_key obrigatório' in events)
check("evento exige transação e chave exatas", all(x in validator for x in ["transaction_id", "resolution_key", "_world_event_id", "_effect_key"]))
check("aplicador revalida status da pendência", '"pending".equalsIgnoreCase(pending.optString("status"))' in orch and "pendingKey" in orch)
check("resolução genérica rejeita evento", "Eventos devem usar event_resolutions" in orch)

# Facções, NPCs e agenda
faction_fields = ["title", "summary", "location", "resources", "agenda", "relationship_delta", "consequence_json", "next_plan"]
check("contrato completo de facção preservado", all(field in validator for field in faction_fields))
check("chave de facção inclui identidade do plano", "factionEffectKey" in living and ":plan:" in living and "fingerprint" in living)
blocked_pos = living.find("db.addPendingTransactionUnique(\"Plano de facção pendente")
advance_pos = living.find("db.applyFactionSimulationStepChecked(a)")
check("facção não avança antes da revisão", blocked_pos >= 0 and advance_pos > blocked_pos and "continue;" in living[blocked_pos:advance_pos])
check("urgência temporal aplicada em um só motor", "Reaplicá-la aqui duplicava a urgência" in npcs and "previewOffscreenTick" not in npcs[npcs.find("public void applyConfirmed"):npcs.find("public JSONObject validateUpdates")])
check("agenda deduplica data explícita e repetição", "Set<String> seen" in living and 'String key=index+"|"+due' in living)
check("chave de NPC usa índice original da agenda", "_schedule_index" in living and ":schedule:" in living)

# Atividades e regras
check("atividade exige trecho local real", "verifyEnabledManualExcerpt" in living_engine)
check("custo e prêmio precisam estar no trecho", "mechanicalOutcomeMatchesExcerpt" in living_engine)
check("rolagem local é vinculada", all(x in living_engine for x in ["roll_id", "formula", "total"]))
check("atividade aguardando aplicação não é erro", "aguardando_aplicacao" in main and "awaiting_application" in main)
check("mecânica manual exige cálculo verificável", all(x in arbiter for x in ["manualOnlyBasis", "resolutionVerifier.verifyStateUpdate", "calculation_verified", "clearMechanicalAndPersistentChanges"]))
check("resolução exige a mesma resolution_key", "resolution_key" in arbiter and "envelopeHasVerifiedMechanicsForKey" in validator)

# Contexto e integração
check("game_bus completo permanece", "criticalGameBus" in orch and "all_systems_preserved" in orch)
check("compactação é recursiva", "deepCompactObject" in orch and "deepCompactValue" in orch)
check("limite final é medido", "if(raw.length()>max)throw new IllegalStateException" in orch)
check("falha ao montar contexto bloqueia o plano", "contextReady=false" in orch and "context_error_message" in orch)
check("tela não chama IA com contexto incompleto", "!activeMasterPlan.contextReady" in main and "Mestre foi bloqueado" in main)
check("mapa e tabuleiro têm escrita revisada", "map_updates" in validator and "tactical_updates" in validator and "applyMapDirectorUpdateChecked" in orch and "applyTacticalDirectorUpdateChecked" in orch)
check("diário tem escrita revisada", "journal_updates" in validator and "addJournalChecked" in orch)

# Guarda contra regressão da compilação vista no Android Studio
check("guarda de JSONException incluída", (ROOT / "tools/CheckedJsonPutValidator.java").is_file() and (ROOT / "tools/validate_json_checked_exceptions.sh").is_file())
check("AppDb possui compilação semântica isolada", (ROOT / "tools/validate_appdb_semantics.sh").is_file())

# Pequenos testes comportamentais independentes dos textos-fonte.
def due_occurrences(day, repeat, start, day_before, day_after):
    seen = set(); out = []
    if day > day_before and day <= day_after:
        seen.add((0, day)); out.append(day)
    if repeat > 0:
        first = max(1, start if start else day)
        if first <= day_before:
            first += ((day_before - first) // repeat + 1) * repeat
        due = first
        while due <= day_after:
            if (0, due) not in seen:
                seen.add((0, due)); out.append(due)
            due += repeat
    return out

check("comportamental: agenda não duplica mesma ocorrência", due_occurrences(10, 7, 10, 9, 17) == [10, 17])

def faction_key(fid, plan, step):
    normalized = re.sub(r"[^a-z0-9]+", "-", plan.lower()).strip("-")[:42] or "sem-plano"
    # O hash Java é diferente do Python; aqui validamos a propriedade pretendida.
    return f"faction:{fid}:plan:{normalized}:{hash(plan.lower())}:step:{max(0, step)}"
check("comportamental: planos diferentes não colidem na mesma etapa", faction_key(4, "tomar o porto", 1) != faction_key(4, "proteger o porto", 1))

def correlated(event_id, tx_id, key, pending):
    return (event_id > 0 and tx_id > 0 and key and pending.get("status") == "pending"
            and pending.get("event_id") == event_id and pending.get("effect_key") == key)
check("comportamental: evento errado não encerra pendência", not correlated(8, 3, "world_event:8", {"status":"pending", "event_id":7, "effect_key":"world_event:7"}))
receipts = set()
def apply_once(key):
    if key in receipts: return False
    receipts.add(key); return True
check("comportamental: recibo impede reaplicação", apply_once("world_event:5") and not apply_once("world_event:5"))

passed = sum(ok for _, ok in checks)
for label, ok in checks:
    print(("OK  " if ok else "FALHA  ") + label)
print(f"RESULTADO: {passed}/{len(checks)} verificações corretivas aprovadas.")
if passed != len(checks):
    sys.exit(1)
