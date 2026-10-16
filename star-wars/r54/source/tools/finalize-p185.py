#!/usr/bin/env python3
"""Create a verifiable, full P185 Android Studio source checkpoint and delta."""
from pathlib import Path
import hashlib, json, zipfile, subprocess, xml.etree.ElementTree as ET, datetime, os
root=Path(__file__).resolve().parent.parent
out=Path('/mnt/data')
prev=out/'BRASEIRO_STAR_WARS_EDGE_R54_P184_SOURCE_CHECKPOINT.zip'
basehash=hashlib.sha256(prev.read_bytes()).hexdigest()
assert basehash=='63430cb5688a9c55bde7b06c48293d8fb51437d6d6845564214d582af1bf38b1'
evidence=root/'evidence/P185';evidence.mkdir(parents=True,exist_ok=True)
reg=json.loads((evidence/'test-results/shard_0_1_regular.json').read_text())
stress=json.loads((evidence/'test-results/shard_0_1_stress.json').read_text())
assert not reg['badFiles'] and reg['passed']==reg['tests']==2739
assert not stress['badFiles'] and stress['passed']==stress['tests']==6
js=list((root/'app/src/main/assets/vtt/js').rglob('*.mjs'))
for p in js:
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 if r.returncode: raise RuntimeError(f'JS syntax {p}: {r.stderr}')
json_files=list((root/'app/src/main/assets/vtt').rglob('*.json'))
for p in json_files: json.loads(p.read_text())
xml_files=list((root/'app/src/main').rglob('*.xml'))
for p in xml_files: ET.parse(p)
with zipfile.ZipFile(prev) as z:
 old_java={n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith('app/src/main/java/') and n.endswith('.java')}
new_java={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (root/'app/src/main/java').rglob('*.java')}
assert old_java==new_java,(len(old_java),len(new_java))
(evidence/'JAVA_SOURCE_HASHES.json').write_text(json.dumps({'previous':'P184','identical':True,'javaSources':new_java},ensure_ascii=False,indent=2))
(evidence/'PDF_TARGETED_SOURCE_CROSSCHECK.json').write_text(json.dumps({
 'source':{'title':'The Jewel of Yavin','productCode':'SWE09','driveFileId':'1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K','retrievedVia':'Google Drive connected PDF text'},
 'comparison':'direcionada, não integral',
 'facts':[{'sourcePages':'Grand Prix pp.49-53','rule':'buoy miss causes setback on following Piloting check','implementation':'race.legs[*].appliedSetbacks; status.raceNextLegRequirements'},
 {'sourcePages':'Grand Prix pp.50-51','rule':'tibannuck leg adds two setbacks; threats add one system strain; despair causes vehicle critical with -20','implementation':'race leg4 optional GM hazard receipt and replay'},
 {'sourcePages':'Grand Prix pp.51-52','rule':'beldon leg adds one setback; three threats or despair cause vehicle critical','implementation':'race leg5 optional GM hazard receipt and replay'},
 {'sourcePages':'Auction pp.58-68','rule':'bidder Bid Value increases per round; influence adds success/triumph modifiers; completed round determines leader','implementation':'receipt-based deterministic auction replay and tamper gate'}],
 'notCertified':['entire Grand Prix vehicle critical effect integration (critical modifier is recorded but not applied to a specific vehicle)','gas fields, velker and rivals full AI','bank slicing, betrayal and escape','all five adventures scene-by-scene','EotE/AoR expansions semantic certification']
},ensure_ascii=False,indent=2))
summary={'checkpoint':'P185','base':'P184','regular':reg,'stress':stress,'totalTests':reg['tests']+stress['tests'],'totalPassed':reg['passed']+stress['passed'],'nodeSyntaxFiles':len(js),'jsonValidated':len(json_files),'xmlValidated':len(xml_files),'javaSourcesUnchanged':len(new_java),'bundleModules':86,'suiteMode':'per-file runner; not monolithic'}
(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
(root/'CURRENT_CHECKPOINT.md').write_text('''# Braseiro Star Wars VTT — checkpoint R54 P185 (13/10/2026)\n\nBase P184 preservada. Android 1.0.185; bundle offline 86 módulos; oito fontes Java/Kokoro byte a byte idênticas.\n\nP185: replay determinístico dos recibos do leilão (licitantes, rodadas, influência, valores, líder e encerramento); auditoria ampliada de corrida (boias, tensão, críticos, classificação e vitória); aplicação da penalidade de boia perdida no próximo trecho; perigos de tibannuck e beldon adjudicados com dados reais e modificadores de críticos pendentes; indicação de dificuldade ambiental via status Android; bloqueio de saída com corrida/leilão iniciados e não concluídos, preservando saves legados.\n\nTestes: 2739/2739 regulares (304 arquivos) + 6/6 stress = 2745/2745, por arquivo; ver evidence/P185. Cotejo direcionado SWE09, não certificação editorial integral. Sem APK, CI BRELGA ou homologação física.\n''')
(root/'HANDOFF_STAR_WARS_EDGE_R54_P185_2026-10-13.md').write_text('''# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P185\n\n**Base:** P184 SHA-256 `63430cb5688a9c55bde7b06c48293d8fb51437d6d6845564214d582af1bf38b1`. Não regredir.\n\n## Implementação P185\n\n- `jewelOfYavinOperations.mjs`: auditoria determinística integral dos recibos P184/P185. Reconstrói leilão a partir de cada influência e rodada, recalcula Bid Value, lances, líder, eliminações e conclusão. Corrida reconstituída com checagem de resultado, velocidade, tensão, boias, colisões, classificação, rivais e flag de vitória. Novos recibos de influência incluem sucessos e Triunfos verificáveis; recibos antigos continuam aceitos sem fabricar dados.\n- Corrida SWE09: penalidade de uma boia perdida no teste seguinte; tibannuck adiciona 2 dados de revés, Ameaças causam 1 Tensão de sistemas, Desespero registra crítico com modificador -20; beldon adiciona 1 dado de revés e registra crítico com 3 Ameaças ou Desespero. Modificador crítico registrado para adjudicação, **ainda não aplicado ao veículo**. As opções são recebidas por adjudicação do Mestre, sem gerar dados fictícios.\n- `jewelOfYavin.mjs`: não permite sair da corrida/leilão **já iniciados** antes da conclusão; saves P184 sem operação continuam compatíveis. Status publicado fornece exigências do próximo trecho para o Android. Checkpoint adulterado não avança.\n- Android `1.0.185`, bundle offline 86 módulos, Java/Kokoro 8 fontes idênticas.\n\n## Evidências e testes\n\n- `tests/uae-p185-race-auction-integrity-source.test.mjs`: 31 novos testes (integridade, perigos, 3 reaberturas, UAE, legado e status Android).\n- `evidence/P185/TEST_SUMMARY.json`: **2739/2739 regulares + 6/6 stress = 2745/2745**, em 310 arquivos por execução individual. Não declarar suíte monolítica verde.\n- `evidence/P185/PDF_TARGETED_SOURCE_CROSSCHECK.json`: cotejo DIRECIONADO ao PDF SWE09 The Jewel of Yavin pp.49–53 e 58–68. Não equivale a certificação integral.\n- Oito fontes Java/Kokoro com SHA idêntico ao P184.\n\n## Pendências reais\n\n- Resolver efeitos dos críticos de veículos no estado do carro, gas fields/velker/rivais, completar regras bancárias, traição e fuga em SWE09.\n- Completar Long Arm of the Hutt New Meen/Geonosis/Teemo, Beyond the Rim, Under a Black Sun e Mask of the Pirate Queen; cotejar **cinco aventuras integralmente** com PDFs.\n- Auditar semanticamente 360 posições AoR e todas as expansões EotE/AoR; 360/360 consumidores não é certificação.\n- APK ainda não compilado. Sem CI BRELGA, SDK/Gradle/adb ou homologação física demonstrados.\n\n## Continuidade P186+\n\nPriorizar efeitos reais de crítico veicular, banco e fuga SWE09 e New Meen/Geonosis/Teemo SWE02; executar testes focal+regressão+stress, preservar Kokoro, ZIP Android Studio completo + SHA256 + delta + handoff, Biblioteca com readback. Nunca inventar certificação ou APK.\n''')
# The generated delta is a patch-by-replacement archive; only files changed or added.
for tmp in ['tools/patch-p185.py','tools/patch-p185-race.py','tools/patch-p185-gates.py']:
 (root/tmp).unlink(missing_ok=True)
files=sorted(p for p in root.rglob('*') if p.is_file())
filemap={p.relative_to(root).as_posix():p for p in files}
full=out/'BRASEIRO_STAR_WARS_EDGE_R54_P185_SOURCE_CHECKPOINT.zip'
delta=out/'BRASEIRO_STAR_WARS_EDGE_R54_P184_TO_P185_DELTA.zip'
with zipfile.ZipFile(prev) as old:
 oldnames=set(old.namelist()); oldhash={n:hashlib.sha256(old.read(n)).digest() for n in oldnames}
changed=[n for n,p in filemap.items() if n not in oldhash or hashlib.sha256(p.read_bytes()).digest()!=oldhash[n]]
removed=sorted(oldnames-set(filemap))
assert not removed, f'Deletion would need manifest: {removed[:20]}'
with zipfile.ZipFile(full,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for n,p in filemap.items():z.write(p,n)
with zipfile.ZipFile(delta,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for n in changed:z.write(filemap[n],n)
with zipfile.ZipFile(full) as z:
 assert z.testzip() is None
 assert len(z.namelist())==len(filemap)
 fullhash={n:hashlib.sha256(z.read(n)).digest() for n in z.namelist()}
with zipfile.ZipFile(prev) as old,zipfile.ZipFile(delta) as d:
 rebuilt={n:hashlib.sha256(old.read(n)).digest() for n in old.namelist()}
 rebuilt.update({n:hashlib.sha256(d.read(n)).digest() for n in d.namelist()})
 assert rebuilt==fullhash,'Delta not reproducible'
sha=hashlib.sha256(full.read_bytes()).hexdigest()
(out/(full.name+'.sha256')).write_text(f'{sha}  {full.name}\n')
print(json.dumps({'checkpoint':'P185','zip':str(full),'sha256':sha,'size':full.stat().st_size,'files':len(filemap),'changedFiles':len(changed),'deltaBytes':delta.stat().st_size,'deltaReproduced':True,'tests':summary['totalPassed'],'javaHashesIdentical':True,'syntax':{'mjs':len(js),'json':len(json_files),'xml':len(xml_files)}},indent=2))
