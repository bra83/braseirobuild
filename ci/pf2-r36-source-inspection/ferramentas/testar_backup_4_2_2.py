#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
APPDB = ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java'
VALIDATOR = ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e/BackupContractValidator.java'
PORTABLE = ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e/PortableBackupManager.java'
TEST = ROOT / 'qa_tests/androidTest/java/com/braseiro/pathfinder2e/AppDbBackupIsolationTest.java'
SAFETY = ROOT / 'qa_tests/androidTest/java/com/braseiro/pathfinder2e/CampaignSafetyIsolationTest.java'
SAFETY_MANAGER = ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e/CampaignSafetyManager.java'

errors=[]
def require(condition, message):
    if not condition: errors.append(message)

appdb=APPDB.read_text('utf-8')
validator=VALIDATOR.read_text('utf-8')
portable=PORTABLE.read_text('utf-8')
test=TEST.read_text('utf-8')
safety_test=SAFETY.read_text('utf-8')
safety_manager=SAFETY_MANAGER.read_text('utf-8')

start=appdb.find('public synchronized void importAll(JSONObject root)')
end=appdb.find('private JSONObject portableMapState', start)
# portableMapState appears before importAll in current source, so use next helper after importAll.
if end <= start:
    end=appdb.find('private String restorePortableMapState', start)
if end <= start:
    end=min(len(appdb), start+30000)
import_body=appdb[start:end]

require(start >= 0, 'importAll não encontrado')
require(('put("version",18)' in appdb or 'put("version",19)' in appdb or 'put("version",20)' in appdb) and 'put("backupScope","campaign")' in appdb,
        'exportação não usa contrato de campanha v16')
require('externalLibrariesExcluded' in appdb and 'derivedIndexesExcluded' in appdb,
        'exclusões de bibliotecas/índices não são declaradas')
require('root.put("diceHistory"' in appdb, 'histórico de dados da campanha não é exportado')
require('restoreDiceHistory(db,root.optJSONArray("diceHistory"),backupProfile,backupVersion,characterIdMap)' in import_body,
        'histórico de dados da campanha não usa restauração perfilada e compatível com legado')
require('profileRowsForRestore' in appdb,
        'filtro de linhas por perfil para backups legados não encontrado')
require('"diceHistory"' not in re.search(r'FORBIDDEN_GLOBAL_KEYS=\{([^}]*)\}', validator, re.S).group(1),
        'validador trata diceHistory perfilado como biblioteca global')

for table in ('manuals','manual_rag_parts','manual_local_pages','tokens','token_pdfs','pdf_tokens','map_packages'):
    patterns=[rf'db\.delete\("{table}"', rf'DELETE FROM {table}\b']
    require(not any(re.search(p, import_body, re.I) for p in patterns),
            f'importAll ainda apaga tabela global {table}')

for physical in ('image_uri','overlay_uri','map_package_id'):
    portable_method=appdb[appdb.find('private JSONObject portableMapState'):start]
    require(f'out.put("{physical}"' not in portable_method,
            f'backup portátil ainda grava referência física {physical}')

require('BackupContractValidator.requireValid' in portable,
        'PortableBackupManager não valida o contrato antes de gravar/restaurar')
require('importingDndDoesNotDeleteMystaraOrLibraries' in test,
        'teste instrumentado de isolamento D&D/Mystara ausente')
require('exportedMapStateHasNoLocalPaths' in test,
        'teste instrumentado de portabilidade do mapa ausente')
require(('backupVersion<=0||backupVersion>18' in appdb or 'backupVersion<=0||backupVersion>19' in appdb or 'backupVersion<=0||backupVersion>20' in appdb),
        'faixa explícita de versões legadas não encontrada')
require('snapshot_" + world + "_' in safety_manager,
        'snapshots não possuem namespace por mundo')
require('snapshotsAreRetainedAndListedPerWorld' in safety_test,
        'teste de retenção isolada de snapshots ausente')
require('currentPrefix=PREFIX+' in portable and 'startsWith(currentPrefix)' in portable,
        'retenção de backup portátil não está isolada por mundo')

if errors:
    for e in errors: print('FALHA', e)
    sys.exit(1)
print('OK — contrato de backup v18+ isolado por mundo, sem exclusão global e sem caminhos físicos.')
