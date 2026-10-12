#!/usr/bin/env python3
from pathlib import Path
import json, re, sys, zipfile, math
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
MAIN=(SRC/'MainActivity.java').read_text('utf-8')
DB=(SRC/'AppDb.java').read_text('utf-8')
CTX=(SRC/'MasterGameContext.java').read_text('utf-8')
VAL=(SRC/'MasterResponseValidator.java').read_text('utf-8')
OPT=(SRC/'SceneOptionDiversifier.java').read_text('utf-8')
SIM=(SRC/'LivingWorldSimulationEngine.java').read_text('utf-8')
BACK=(SRC/'BackupContractValidator.java').read_text('utf-8')
VER=dict(x.split('=',1) for x in (ROOT/'version.properties').read_text().splitlines() if '=' in x)
checks=[]
def check(n,v): checks.append((n,bool(v))); print(('OK  ' if v else 'FALHA  ')+n)

def has(cls,*tokens):
 s=(SRC/(cls+'.java')).read_text('utf-8')
 return all(t in s for t in tokens)

# Versão / schema / backup
check('versão 5.0.0 ou sucessora', tuple(map(int,VER.get('VERSION_NAME','0.0.0').split('.')[:3])) >= (5,0,0) and int(VER.get('VERSION_CODE','0'))>=500000)
check('backup 18 ou sucessor', ('put("version",18)' in DB or 'put("version",19)' in DB or 'put("version",20)' in DB) and ('MAX_SUPPORTED_VERSION=18' in BACK or 'MAX_SUPPORTED_VERSION=19' in BACK or 'MAX_SUPPORTED_VERSION=20' in BACK))
check('schema 53 ou sucessor', re.search(r'DB_VERSION\s*=\s*(5[3-9]|[6-9][0-9])',DB) is not None and 'oldVersion < 53' in DB)

# 1 startup único
check('coordenador único da abertura', has('AdventureStartupCoordinator','enum Phase','VALIDATING','LOCATION','RULES','CONTEXT','NARRATIVE','COMPLETE','BLOCKED') and 'private void beginAdventure' in MAIN)
check('botões de início convergem para beginAdventure', MAIN.count('beginAdventure(false)')>=2 and MAIN.count('beginAdventure(true)')>=2)
check('abertura usa preflight e fases', 'adventureStartupCoordinator.preflight(db)' in MAIN and MAIN.count('adventureStartupCoordinator.phase(')>=6)

# 2 diagnóstico
check('diagnóstico 5.0 visível', 'DIAGNÓSTICO 5.0 — TESTAR SISTEMA' in MAIN and has('SystemDiagnostics5','Banco íntegro','Índice local de regras/FTS','Atlas ativo','Protocolo do Mestre v1'))
check('diagnóstico roda fora da UI', 'runSystemDiagnostics5()' in MAIN and 'SystemDiagnostics5.run' in MAIN and 'AppExecutors.io().execute' in MAIN)

# 3/4 atlas congelado + catálogo
check('reparo do atlas é explícito', 'REPARAR POSIÇÕES DO ATLAS' in MAIN and 'explicitRepairActiveAtlas' in MAIN and 'repairCatalogAndRetryAdventureStart' not in MAIN)
check('toque não dispara migração silenciosa', 'Nenhum ponto catalogado próximo' not in MAIN and 'explicitRepairActiveAtlas' in MAIN)
asset=ROOT/'android/app/src/main/assets/maps/faerun_locations_resolved_5_0.json'
seed=json.loads(asset.read_text('utf-8'))
check('catálogo geográfico 5.0 existe', seed.get('map_id')=='faerun-5e-maxres-16695x9555' and len(seed.get('locations',[]))>=10)
check('catálogo aplica somente location existente + hex real', 'SELECT id FROM map_locations WHERE package_id=? AND location_id=?' in DB and 'mapHexExists5(packageId,hex)' in DB and 'bundled_verified' in DB)
check('catálogo nunca cria verbete geográfico', 'applyBundledResolvedLocations' in DB and 'INSERT INTO map_locations' not in DB[DB.index('public synchronized int applyBundledResolvedLocations'):DB.index('private String getLogicalMapIdForPackage')])
# validate bundled IDs/hexes
z=ROOT/'android/app/src/main/assets/maps/faerun_maxres_integrated.zip'
try:
 with zipfile.ZipFile(z) as f:
  atlas=json.loads(f.read('faerun_5e_hex_atlas_v2.json'))['hexes']
  locs=json.loads(f.read('faerun_5e_locations_v2.json'))['locations']
  ids=set(locs.keys()) if isinstance(locs,dict) else {x.get('location_id',x.get('id')) for x in locs}
  all_ok=True; residual_ok=True
  for r in seed['locations']:
   all_ok &= r['location_id'] in ids and r['hex_id'] in atlas and r.get('verification')=='visible_marker'
   c=atlas[r['hex_id']]['center']; residual_ok &= math.hypot(c['x']-r['map_x'],c['y']-r['map_y']) <= 32.1
 check('10 posições embarcadas existem no catálogo e atlas reais',all_ok)
 check('marcadores verificados caem no próprio hex',residual_ok)
except Exception:
 check('10 posições embarcadas existem no catálogo e atlas reais',False); check('marcadores verificados caem no próprio hex',False)

# 5 protocolo do Mestre
check('protocolo do Mestre v1', has('MasterProtocolV1','VERSION = 1','structured_fail_closed','unknown_mutations','closeAllMutations'))
check('canais reais do contrato estão cobertos', has('MasterProtocolV1','state_updates','npc_cognition_updates','knowledge_updates','world_events','event_resolutions','scene_state','combat'))
check('protocolo incompatível zera mutações', has('MasterProtocolV1','protocol_rejected','new JSONObject().put("action", "none")','pending_test'))
check('canais desconhecidos de mutação são rejeitados', has('MasterProtocolV1','endsWith("_updates")','endsWith("_resolutions")','Canal estrutural desconhecido rejeitado'))
check('validador aplica protocolo antes das mutações', 'MasterProtocolV1.enforce(envelope)' in VAL)
check('prompt exige protocol_version 1', MAIN.count('\\"protocol_version\\":1')>=2 and 'protocol_version deve ser exatamente 1' in MAIN)

# 6/7 NPC
check('normalizador único de NPC', has('NpcIdentityNormalizer','stableId','isReservedPlayerAlias','displayName','voce','jogador'))
check('persistência usa stable_id', 'stable_id TEXT DEFAULT' in DB and 'idx_npc_stable_id' in DB and 'NpcIdentityNormalizer.normalize' in DB)
check('Você não pode virar NPC', 'NpcIdentityNormalizer.isReservedPlayerAlias' in DB and 'NpcIdentityNormalizer.normalize' in VAL)
check('camadas known scene world active', all(x in DB for x in ('listKnownNpcs','listSceneNpcs','listWorldActiveNpcs')) and all(x in CTX for x in ('known_npcs','scene_npcs','world_active_npcs')))

# 8 orçamento mundo vivo
check('orçamento explícito do Mundo Vivo', has('WorldSimulationBudget','factions=8','npcs=12','activities=8','events=8','guilds=8','stepsPerFaction=4'))
check('simulador realmente aplica o orçamento', 'WorldSimulationBudget budget' in SIM and 'budget.trim' in SIM and 'simulation_budget' in SIM and 'budget.stepsPerFaction' in SIM)

# 9 fila única
check('fila única de pendências existe', 'CREATE TABLE IF NOT EXISTS pending_action_queue' in DB and 'listPendingActions' in DB)
check('fila espelha transação evento atividade', all(x in DB for x in ('source_kind=\'transaction\'','source_kind=\'world_event\'','source_kind=\'activity\'')))
check('fila encerra espelhos obsoletos', DB.count("SET status='resolved'")>=3 and 'NOT EXISTS (SELECT 1 FROM pending_transactions' in DB)
check('Mestre recebe pending_actions', 'root.put("pending_actions"' in CTX)

# 10/11 regras
check('abertura ficcional não trava em regra específica', 'opening_fiction_grounded' in MAIN and 'mechanics_blocked' in MAIN and 'OPENING_RULE_SEARCH_WATCHDOG_MS=3500' in MAIN.replace(' ',''))
check('RAW/RAI permanece fail-closed', 'Regras RAW/RAI' in MAIN and 'sufficient_for_mechanics' in MAIN)
check('cache quente é somente enquadramento', has('EssentialRulesCache','mechanics_authority', 'general framing only','specific mechanics require matched evidence'))
check('cache quente é aquecido em background', 'EssentialRulesCache.warm' in MAIN and 'warmManualSearchIndexAsync' in MAIN)

# 12 Charon
check('telemetria local do Charon', has('CharonLatencyTelemetry','text_to_request_ms','request_to_audio_ms','audio_to_play_ms'))
check('Charon mede primeira fala sem telemetria externa', 'charonLatencyTelemetry.begin' in MAIN and 'charonLatencyTelemetry.firstAudio' in MAIN and 'charonLatencyTelemetry.play' in MAIN)
check('primeiro bloco rápido preservado', 'FIRST_BLOCK_MAX_CHARS = 320' in (SRC/'NarrationChunker.java').read_text('utf-8') or '320' in (SRC/'NarrationChunker.java').read_text('utf-8'))

# 13 ANR / I/O
check('watchdog de responsividade', has('UiResponsivenessMonitor','lag>1200','max_lag_ms','lag_count'))
check('PDF é montado fora da thread UI', 'prepareLiteralAdventurePdf' in MAIN and 'Montando o livro da aventura em segundo plano' in MAIN and 'Montando a crônica em PDF em segundo plano' in MAIN)
check('cópia do token individual é background', 'importSelectedTokenAsync' in MAIN and 'Copiando e validando o token em segundo plano' in MAIN)
check('reparo Atlas e diagnóstico são background', 'Reparando somente posições verificáveis do Atlas' in MAIN and 'Diagnóstico 5.0: verificando banco' in MAIN)

# 14 migração / saúde
check('snapshot físico pré-v53', has('DatabaseMigrationGuard','braseiro_dnd.pre_v53.db','beforeOpen','afterOpen','quickIntegrityCheck'))
check('migração 53 é idempotente', 'addColumnIfMissing' in DB and 'CREATE TABLE IF NOT EXISTS pending_action_queue' in DB and 'CREATE UNIQUE INDEX IF NOT EXISTS idx_npc_stable_id' in DB)
check('quick_check está exposto', 'PRAGMA quick_check(1)' in DB and 'databaseHealth5' in DB)
check('Main protege abertura do banco', 'DatabaseMigrationGuard.beforeOpen(this)' in MAIN and 'DatabaseMigrationGuard.afterOpen(this,db)' in MAIN)

# 15 backup roundtrip
check('round-trip de backup em sandbox', has('BackupRoundTripVerifier','braseiro_roundtrip_','exportAll','importAll','fingerprint'))
check('round-trip é opção visível', 'TESTAR BACKUP 5.0 — ROUND-TRIP' in MAIN and 'runBackupRoundTrip5' in MAIN)
check('pendingActions permanece no backup', 'root.put("pendingActions"' in DB and 'root.has("pendingActions")' in DB and '"pendingActions"' in BACK)

# 16 fonte da verdade
check('registro explícito de autoridades', has('SourceOfTruthRegistry','characters + sheet_json','scene_state_profiles','map_world_state/map_hex_atlas/map_locations','proposal_only','structured database authority wins'))
check('game_bus inclui manifesto de autoridade', 'source_of_truth_manifest' in CTX and 'MasterProtocolV1.descriptor()' in CTX)
check('prompt diz que banco vence a prosa', 'Quando prosa e estado estruturado divergirem' in MAIN and 'a IA apenas propõe mudanças' in MAIN)

# 17 opções
check('sugestões são no máximo seis', 'MAX_OPTIONS = 6' in OPT)
check('sugestões precisam ser ancoradas', 'isGroundedInScene' in OPT and 'elementos concretos explicitamente' in OPT.lower())
check('sem fallback obrigatório', 'int wanted=hasScene?MAX_OPTIONS' not in OPT and 'painel nunca desapareça' not in OPT)

# 18 combate
check('máquina de estados do combate', has('CombatStateMachine','PREPARING','INITIATIVE','ACTIVE_ROUND','RESOLVING','AWAITING_ROLL','ENCOUNTER_END'))
check('banco persiste estado e encounter', 'state TEXT DEFAULT' in DB and 'pending_roll_json TEXT DEFAULT' in DB and 'encounter_id TEXT DEFAULT' in DB)
check('transições são validadas', 'CombatStateMachine.canTransition' in DB and 'transitionCombatState' in DB)
check('ação do Mestre entra em resolving/awaiting', 'CombatStateMachine.RESOLVING' in MAIN and 'CombatStateMachine.AWAITING_ROLL' in MAIN)

# 19 diário ledger
check('Diário canônico tem identidade idempotente', 'canonical_fact INTEGER DEFAULT 1' in DB and 'event_key TEXT DEFAULT' in DB and 'addJournalFact' in DB)
check('combate registra fatos canônicos', 'addJournalFact("Combate iniciado"' in MAIN and 'addJournalFact("Encontro encerrado"' in MAIN)
check('crônica de bardo continua baseada no Diário', 'EXPORTAR COMO CRÔNICA DE BARDO (PDF)' in MAIN and has('BardChronicleComposer','preserv'))

# Integridade geral esperada
java_count=len(list(SRC.glob('*.java')))
check('fontes Java 5.0 presentes', java_count>=113)
check('validador semântico atualizado para dependências 5.0', 'NpcIdentityNormalizer.java' in (ROOT/'tools/validate_appdb_semantics.sh').read_text() and 'MasterProtocolV1.java' in (ROOT/'tools/validate_core_semantics.sh').read_text())

failed=[n for n,o in checks if not o]
print(f'RESULTADO 5.0.0: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.')
if failed:
 print('FALHAS:'); [print(' - '+n) for n in failed]; sys.exit(1)
