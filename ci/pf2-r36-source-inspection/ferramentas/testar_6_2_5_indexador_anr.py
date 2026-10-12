from pathlib import Path
root=Path(__file__).resolve().parents[1]
idx=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/LocalManualIndexer.java').read_text()
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text()
db=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text()
ver=(root/'version.properties').read_text()
checks=[]
def ok(name,cond):
    checks.append((name,cond)); print(('PASS' if cond else 'FAIL')+' - '+name)
ok('versão 6.2.5','VERSION_NAME=6.2.5' in ver and 'VERSION_CODE=602500' in ver)
ok('sem pré-hash SAF','sourceContentKey' not in idx)
ok('nativo antes de PDFBox',idx.index('TextResult nativeResult=extractWithPlatform') < idx.index('TextResult portableResult=extractWithPdfBoxAndOcr'))
ok('yield por páginas','cooperativeYield(pageNo)' in idx and 'cooperativeYield(i+1)' in idx)
ok('prioridade background',main.count('THREAD_PRIORITY_BACKGROUND')>=2)
ok('executor dedicado de indexação','ExecutorService INDEXING' in (root/'android/app/src/main/java/com/braseiro/pathfinder2e/AppExecutors.java').read_text() and main.count('AppExecutors.indexing().execute')>=2)
ok('contagem leve na UI','countLocalReadyStatusForWorld' in main and 'hasAnyLocalReadyStatusForWorld' in main)
ok('sem count por card','Math.max(m.optInt("rag_page_count"),countLocalPagesSafe(m))' not in main)
ok('Cello traduzido','cello error' in main.lower() and 'sns:' in main.lower())
ok('duplicado só após pronto','successfulDuplicateKeys' in main and 'Duplicado já coberto por outra cópia pronta' in main)
ok('failover individual','final ArrayList<JSONObject> candidates' in main and 'tentando duplicada quando existir' in main)
ok('auditoria profunda preservada','countLocalReadyForWorld(boolean mystara)' in db and 'countLocalReadyStatusForWorld(boolean mystara)' in db)
failed=[n for n,c in checks if not c]
print(f'RESULTADO: {len(checks)-len(failed)}/{len(checks)}')
if failed: raise SystemExit('Falhas: '+', '.join(failed))
