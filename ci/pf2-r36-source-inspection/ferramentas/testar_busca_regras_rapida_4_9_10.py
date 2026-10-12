#!/usr/bin/env python3
from pathlib import Path
import sqlite3, tempfile, time, re, sys

ROOT=Path(__file__).resolve().parents[1]
APP=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text()
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text()
RETR=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/RuleRetrievalEngine.java').read_text()
VER=(ROOT/'version.properties').read_text()
checks=[]
def check(name, ok):
    checks.append((name,bool(ok)))
    print(('OK  ' if ok else 'FALHA  ')+name)

ver=dict(line.strip().split('=',1) for line in VER.splitlines() if '=' in line)
check('versão 4.9.10 ou superior', int(ver.get('VERSION_CODE','0'))>=490910)
check('banco migra para v52 ou superior', bool(re.search(r'DB_VERSION\s*=\s*(?:5[2-9]|[6-9][0-9])',APP)) and 'if (oldVersion < 52)' in APP)
check('índice invertido FTS4 usa conteúdo externo', "USING fts4(content='manual_local_pages'" in APP)
check('migração cria FTS sem reconstrução síncrona', "VALUES('rebuild')" in APP and 'createManualLocalFtsIndex(db,false)' in APP and 'createManualLocalFtsIndex(db,true)' not in APP)
check('gatilhos mantêm inserção atualização e exclusão', all(x in APP for x in ('manual_local_pages_ai','manual_local_pages_bu','manual_local_pages_au','manual_local_pages_ad')))
check('consulta usa MATCH em vez de varrer tudo com LIKE', 'manual_local_pages_fts MATCH ?' in APP)
check('candidatos são ranqueados antes do limite', 'ORDER BY LENGTH(offsets(manual_local_pages_fts)) DESC' in APP)
check('abertura usa uma consulta compacta', 'searchManualLocalPagesFast' in RETR and 'buildOpeningFastQueries' in RETR and 'appendQuery(out,trimWords(consolidated.toString(),48),1)' in RETR)
check('consulta inicial contém âncoras gerais de regras', all(x in RETR for x in ('ability check exploration social interaction encounter initiative travel','reaction roll encounter exploration movement morale')))
check('watchdog cobre somente recuperação de regras', 'retrieval=engine.retrieve' in MAIN and 'Regras localizadas; montando o estado da campanha' in MAIN)
check('FTS é aquecido em segundo plano', 'warmManualSearchIndexAsync();' in MAIN and 'rebuildManualSearchIndexIfNeeded' in MAIN)
check('abertura possui fallback restrito aos manuais centrais', 'coreManuals=' in APP and 'LIMIT 12' in APP and 'varredura integral da biblioteca' in APP)
# O prepare precisa estar no segundo executor, após claimAiRuleSearch.
deliver=MAIN[MAIN.index('private void deliverAiRuleSearch'):MAIN.index('/** Continua a chamada somente depois')]
send=MAIN[MAIN.index('private void sendAi'):MAIN.index('private synchronized boolean claimAiRuleSearch')]
check('montagem do contexto ocorre depois que a busca é resolvida', 'masterOrchestrator.prepare' in deliver and 'masterOrchestrator.prepare' not in send)
check('timeout não pode vencer depois de o índice responder', 'aiRuleSearchResolvedPipeline==pipelineId' in MAIN and 'claimAiRuleSearch(pipelineId)' in deliver)
check('status não afirma múltiplas varreduras', 'passagens recuperadas pelo índice invertido local' in MAIN and 'passagens recuperadas em múltiplas consultas' not in MAIN)

# Exercício real do desenho SQL com uma biblioteca grande sintética.
with tempfile.TemporaryDirectory() as td:
    db=sqlite3.connect(str(Path(td)/'rules.db'))
    db.executescript('''
    CREATE TABLE manual_local_pages(id INTEGER PRIMARY KEY, manual_id INTEGER, source_key TEXT, page_no INTEGER, title TEXT, content TEXT, normalized TEXT, section_title TEXT, chapter_title TEXT);
    CREATE VIRTUAL TABLE manual_local_pages_fts USING fts4(content='manual_local_pages', normalized, title, section_title, chapter_title, tokenize=unicode61);
    CREATE TRIGGER manual_local_pages_ai AFTER INSERT ON manual_local_pages BEGIN INSERT INTO manual_local_pages_fts(docid,normalized,title,section_title,chapter_title) VALUES(new.id,new.normalized,new.title,new.section_title,new.chapter_title); END;
    CREATE TRIGGER manual_local_pages_ad BEFORE DELETE ON manual_local_pages BEGIN DELETE FROM manual_local_pages_fts WHERE docid=old.id; END;
    CREATE TRIGGER manual_local_pages_bu BEFORE UPDATE ON manual_local_pages BEGIN DELETE FROM manual_local_pages_fts WHERE docid=old.id; END;
    CREATE TRIGGER manual_local_pages_au AFTER UPDATE ON manual_local_pages BEGIN INSERT INTO manual_local_pages_fts(docid,normalized,title,section_title,chapter_title) VALUES(new.id,new.normalized,new.title,new.section_title,new.chapter_title); END;
    ''')
    rows=[]
    for i in range(30000):
        text='historia cenário criatura equipamento descrição comum '+str(i)
        if i%997==0: text+=' ability check exploration social interaction encounter initiative travel'
        rows.append((i+1,(i%200)+1,'src'+str(i%200),(i%350)+1,'Página '+str(i),'conteúdo',text,'seção','capítulo'))
    db.executemany('INSERT INTO manual_local_pages VALUES(?,?,?,?,?,?,?,?,?)',rows)
    db.execute("INSERT INTO manual_local_pages_fts(manual_local_pages_fts) VALUES('rebuild')")
    db.commit()
    started=time.perf_counter()
    result=db.execute("SELECT p.id FROM manual_local_pages_fts JOIN manual_local_pages p ON p.id=manual_local_pages_fts.docid WHERE manual_local_pages_fts MATCH ? ORDER BY LENGTH(offsets(manual_local_pages_fts)) DESC LIMIT 320",('ability* OR explor* OR social* OR encounter* OR initiative* OR travel*',)).fetchall()
    elapsed=time.perf_counter()-started
    check('FTS encontra regras na biblioteca sintética', len(result)>0)
    check('consulta FTS termina sem varredura prolongada', elapsed<2.0)
    # Gatilhos continuam coerentes depois da migração.
    db.execute("UPDATE manual_local_pages SET normalized='saving throw concentration spellcasting' WHERE id=2")
    db.commit()
    check('gatilho de atualização torna a página pesquisável', db.execute("SELECT COUNT(*) FROM manual_local_pages_fts WHERE manual_local_pages_fts MATCH 'concentration'").fetchone()[0]==1)
    db.execute('DELETE FROM manual_local_pages WHERE id=2'); db.commit()
    check('gatilho de exclusão remove a página do índice', db.execute("SELECT COUNT(*) FROM manual_local_pages_fts WHERE manual_local_pages_fts MATCH 'concentration'").fetchone()[0]==0)
    print(f'TEMPO_FTS_30000_PAGINAS={elapsed:.4f}s')

failed=[n for n,o in checks if not o]
print(f'RESULTADO 4.9.10: {len(checks)-len(failed)}/{len(checks)}')
if failed:
    print('FALHAS:')
    for x in failed: print(' - '+x)
    sys.exit(1)
