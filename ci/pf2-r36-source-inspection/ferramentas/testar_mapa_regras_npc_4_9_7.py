#!/usr/bin/env python3
from pathlib import Path
import re, subprocess, unicodedata
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(SRC/'MainActivity.java').read_text('utf-8')
retrieval=(SRC/'RuleRetrievalEngine.java').read_text('utf-8')
extractor=(SRC/'NarratedNpcExtractor.java').read_text('utf-8')
version=dict(line.split('=',1) for line in (ROOT/'version.properties').read_text('utf-8').splitlines() if '=' in line)
checks={
 'versão 4.9.7 ou superior':int(version.get('VERSION_CODE','0'))>=490700,
 'abertura tem watchdog próprio e busca otimizada':re.search(r'OPENING_RULE_SEARCH_WATCHDOG_MS=(?:[1-5]\d{3,4})L',main) is not None and 'openingRequest?OPENING_RULE_SEARCH_WATCHDOG_MS:RULE_SEARCH_WATCHDOG_MS' in main and 'manual_local_pages_fts' in (SRC/'AppDb.java').read_text('utf-8'),
 'abertura sem fonte segue só como ficção e bloqueia mecânica':'if(openingRequest&&!openingGrounded)' in main and 'opening_fiction_grounded' in main and 'mechanics_blocked' in main and 'sufficient_for_mechanics' in main,
 'abertura exige status e evidência':'opening_rules_ready' in main and 'opening_rules_partial' in main and 'openingEvidence>0' in main,
 'recuperação registra saúde do índice':'manualIndexHealth' in retrieval and 'index_health' in retrieval and 'openingRuleBlockReason' in main and 'manuais habilitados estão PRONTOS' in main,
 'Você é identidade reservada no JSON':'isReservedSceneIdentity' in main and '"voce"' in main and 'appendPreviewNpc' in main,
 'campanhas antigas são saneadas':'sanitizeReservedNpcAliases();' in main and 'db.deleteNpcMemory' in main and 'db.saveSceneState(scene)' in main,
 'extrator de prosa rejeita jogador':'isReservedIdentity' in extractor and '"voce"' in extractor and '"jogador"' in extractor,
 'aba mapa possui ScrollView':'final ScrollView mapPageScroll=new ScrollView(this)' in main and 'mapPageScroll.addView(root' in main,
 'mapa ocupa área dominante':'Math.round(screenHeightDp*.68f)' in main and 'Math.max(460' in main,
 'página continua rolável fora do mapa':'Fora do mapa: deslize a página' in main,
 'preferência do contorno é por mapa':'mapVectorGridPreferenceKey' in main and 'map_vector_grid_visible_'+'' in main,
 'mapa legado com grade bloqueia segunda grade':'mapUsesBakedHexGrid' in main and 'CONTORNO JÁ INCLUSO' in main,
 'pacote ativo legado inicia migração':'activeLegacy' in main and 'Migrando o mapa ativo legado de Faerûn' in main and 'forceReplaceActiveLegacy' in main,
}
failed=[]
for name,ok in checks.items():
    print(('OK  ' if ok else 'FALHA  ')+name)
    if not ok: failed.append(name)
if failed: raise SystemExit(f'{len(failed)} falha(s): '+', '.join(failed))

def norm(v):
    v=''.join(c for c in unicodedata.normalize('NFD',v.lower()) if unicodedata.category(c)!='Mn')
    return re.sub(r'[^a-z0-9]+',' ',v).strip()
reserved={norm(v) for v in ['Você','Vocês','Eu','Nós','Jogador','Jogadora','Grupo','Companhia','Herói','Aventureiro','Player','Party','Narrador','Mestre do jogo','GM']}
behavior=[]
for value,expected in [('Você',True),('Jogador',True),('Kaelen',False),('Mestre Kaelen',False),('Varash',False)]:
    got=norm(value) in reserved; behavior.append((f'identidade {value}',got==expected))
for status,evidence,expected in [
    ('opening_rules_ready',1,True),('opening_rules_partial',2,True),('opening_rules_ready',0,False),
    ('opening_no_relevant_evidence',0,False),('opening_index_incomplete',0,False),('index_unavailable',0,False),('retrieval_timeout',0,False)]:
    got=status in {'opening_rules_ready','opening_rules_partial'} and evidence>0
    behavior.append((f'abertura {status}/{evidence}',got==expected))
keys=lambda world,mapid:'map_vector_grid_visible_'+world+'_'+norm(mapid).replace(' ','_')
behavior.append(('preferências de mapas não colidem',keys('dnd','faerun-5e-10200x6600')!=keys('dnd','faerun-maxres-2026')))
for name,ok in behavior:
    print(('OK  ' if ok else 'FALHA  ')+name)
    if not ok: failed.append(name)
if failed: raise SystemExit(f'{len(failed)} falha(s) comportamental(is): '+', '.join(failed))
subprocess.run(['bash',str(ROOT/'tools/test_narrated_npc_extractor.sh')],check=True)
print(f'RESULTADO 4.9.7: {len(checks)}/{len(checks)} verificações estáticas, {len(behavior)} simulações e teste JVM aprovados.')
