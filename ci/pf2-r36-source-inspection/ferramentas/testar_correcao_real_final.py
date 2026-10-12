#!/usr/bin/env python3
from pathlib import Path
import sys
import re
root=Path(__file__).resolve().parents[1]
java=root/'android/app/src/main/java/com/braseiro/pathfinder2e'
files={n:(java/n).read_text('utf-8') for n in ['BackupReferenceRemapper.java','AppDb.java','LivingWorldEngine.java','MainActivity.java','MasterRulesArbiter.java','MechanicalRollBinding.java','RuleProcedureCompiler.java','RuleResolutionVerifier.java','LivingWorldSimulationEngine.java','WorldRuleGuidance.java','WorldAutonomyPlanner.java','TravelProgressEngine.java','CharacterIdentityCoherence.java','LegacyTransactionIdentity.java','LegacyWorldEventIdentity.java','ManualIdentityPolicy.java','LocalManualIndexer.java']}
checks=[]
def check(name,ok):
    checks.append((name,bool(ok)));print(('OK  ' if ok else 'FALHA ')+name)
backup=files['BackupReferenceRemapper.java'];app=files['AppDb.java'];living=files['LivingWorldEngine.java'];main=files['MainActivity.java'];arb=files['MasterRulesArbiter.java'];bind=files['MechanicalRollBinding.java'];comp=files['RuleProcedureCompiler.java'];ver=files['RuleResolutionVerifier.java'];sim=files['LivingWorldSimulationEngine.java'];guide=files['WorldRuleGuidance.java'];travel=files['TravelProgressEngine.java']
identity=files['CharacterIdentityCoherence.java'];legacy_tx=files['LegacyTransactionIdentity.java'];legacy_event=files['LegacyWorldEventIdentity.java'];manual_identity=files['ManualIdentityPolicy.java'];indexer=files['LocalManualIndexer.java']
on_create=app[app.find('public void onCreate'):app.find('public void onUpgrade')]
check('instalação limpa recebe esquema de integração','ensureMasterIntegrationColumns(db);' in on_create)
check('banco v47+ migra identidades, transações e eventos legados',re.search(r'DB_VERSION\s*=\s*(49|[5-9][0-9]+)',app) is not None and 'oldVersion < 46' in app and 'oldVersion < 47' in app and 'repairLegacyCharacterIdentities(db);' in app and 'backfillLegacyTransactionIdentities(db,null);' in app and 'backfillLegacyWorldEventIdentities(db,null);' in app)
check('export 4.4.2 é reparado durante a importação','CharacterIdentityCoherence.repairLegacyRow(c);long oldId' in app and 'prepareLegacyTransactionRows' in app and 'LegacyTransactionIdentity.keyForBackupRow(source)' in app and 'backfillLegacyTransactionIdentities(db,backupProfile)' in app)
check('transação aplicada legada recebe recibo',all(x in app for x in ['migração de transação legada','CONFLICT_IGNORE','não possui recibo']) and 'SHA-256' in legacy_tx)
check('evento legado recebe identidade e recibo estáveis',all(x in app for x in ['prepareLegacyWorldEventRows','LegacyWorldEventIdentity.keyForBackupRow(source)','migração de evento legado','evento aplicado ']) and 'legacy-event:' in legacy_event)
check('saúde do banco bloqueia falhas de idempotência',all(x in app for x in ['transaction_integrity','blank_effect_keys','applied_without_receipt','open_effect_key_collisions','duplicate_active_events','event_identity_errors','applied_events_without_receipt']) and '&&transactionIntegrity' in app)
check('identidade gerada obedece espécie e classe da ficha',all(x in identity for x in ['repairLegacyRow','detectForeignSpecies','Traços de espécie:','Subclasse de nível']))
check('duplicidade de manuais preserva números','duplicateKey' in manual_identity and 'replaceAll("[^a-z0-9]+"," ")' in manual_identity and 'ManualIdentityPolicy.duplicateKey' in indexer)
check('sincronização invalida alterações e desativa ausentes','upsertManualSynced' in main and 'disableMissingManuals(sourceRoot,seen' in main and all(x in app for x in ['rag_status","not_indexed','clearManualLocalIndex','availability","missing']))
check('backup remapeia referências vinculadas',all(x in backup for x in ['roll_id','actor_character_id','combatant_id','faction_id','event_id','clock_id']) and 'restoreDiceHistory' in app and 'restoreLifeActivities' in app)
check('banco registra identidade completa da rolagem',all(x in app for x in ['actor_character_id','test_key','action_signature','source_rule_id','scene_fingerprint','consumed_at']))
check('rolagem genérica só pode ser vinculada a um contexto','bindDiceHistoryChecked' in app and 'A rolagem já pertence a outro contexto mecânico' in app)
check('rolagem consumida não pode ser reutilizada','consumed_at' in bind and 'já foi consumida' in bind)
check('vínculo confere teste, ação, cena, regra e ator',all(x in bind for x in ['test_key','action_signature','scene_fingerprint','source_rule_id','actor_character_id']))
check('atividade vincula a rolagem atomicamente','sql.beginTransaction()' in living and 'activityRollBinding' in living and 'bindDiceHistoryChecked' in living)
check('atividade consome a rolagem dentro da aplicação','consumeDiceHistoryChecked(activityCheck.optLong("roll_id",0)' in living)
check('atividade não usa mais busca aproximada no histórico','for(JSONObject roll:db.listDiceHistory())if(roll.optLong("id",0)==rollId)' not in living)
check('UI só oferece rolagens livres','consumed_at' in main and 'test_key' in main and 'ROLAGEM LIVRE PARA VINCULAR' in main)
check('árbitro rejeita rolagem recente por texto','verifyRecentLocalRoll' not in arb)
check('árbitro exige MechanicalRollBinding','verifyBoundLocalRoll' in arb and 'MechanicalRollBinding.verify' in arb)
check('procedimentos são atômicos por sentença','atomic_procedures' in comp and 'sentence_rules' in comp and 'multiple_independent_procedures' in comp)
check('reação não é confundida com ação comum','REACTION_PHRASE' in comp and 'ACTION_PHRASE' in comp)
check('múltiplos procedimentos exigem procedure_id','requires_procedure_id' in comp and 'procedure_id é obrigatório' in ver)
check('verificador rejeita mutação sem efeito suportado','A alteração não contém mutação mecânica suportada' in ver)
check('condição textual de combate exige operação semântica','verifyConditionText' in ver and 'não possui operação de aplicação no procedimento selecionado' in ver)
check('bônus de ataque de criatura é conferido','ATTACK_BONUS' in ver and 'O bônus de ataque' in ver)
check('CD de efeito de criatura é conferida','SAVE_DC' in ver and 'A CD de efeito' in ver)
check('Mundo Vivo pesquisa regras antes de planejar facção','retrieveForWorldAction("faction_planning"' in sim and 'autonomy.factionPlan' in sim)
check('Mundo Vivo pesquisa regras antes de decidir NPC','retrieveForWorldAction("npc_planning"' in sim and 'autonomy.npcAction' in sim)
check('orientação rejeita evidência de baixa pertinência','retrievalScore<35' in guide and 'source.optInt("score",0)>=35' in guide)
check('viagem mantém progresso parcial',all(x in travel for x in ['remaining_hours','hours_advanced','completed']))
check('localização só muda ao concluir o trecho','step.optBoolean("completed",false)' in sim and 'travel_progress_json' in sim)
check('sugestões opcionais permanecem','SceneOptionDiversifier.diversify' in main and 'SUGESTÕES OPCIONAIS' in main and 'currentOptions.length()==0' in main)
check('Charon mantém leitura ampliada','60000' in (java/'GeminiService.java').read_text('utf-8') or 'READ_TIMEOUT_MS=60_000' in (java/'GeminiService.java').read_text('utf-8'))
check('perfil híbrido permanece','Híbrido compatível' in main)
failed=[n for n,ok in checks if not ok]
print(f'RESULTADO CORREÇÃO REAL: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.')
if failed:
    print('FALHAS:');[print(' - '+x) for x in failed];sys.exit(1)
