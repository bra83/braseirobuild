#!/usr/bin/env python3
"""Finalize P186 only after real tests and cross-version Java integrity checks."""
from pathlib import Path
import hashlib,json,zipfile,subprocess,xml.etree.ElementTree as ET,shutil
root=Path(__file__).resolve().parent.parent
out=Path('/mnt/data')
old=out/'BRASEIRO_STAR_WARS_EDGE_R54_P185_SOURCE_CHECKPOINT.zip'
assert hashlib.sha256(old.read_bytes()).hexdigest()=='ac8110a640908ef0b59ed7b28709b26151663aa5e49e23ae623159eeefba4813'
evidence=root/'evidence/P186'
regular=json.loads((evidence/'test-results/shard_0_1_regular.json').read_text())
stress=json.loads((evidence/'test-results/shard_0_1_stress.json').read_text())
assert regular['passed']==regular['tests']==2751 and regular['failed']==0 and not regular['badFiles'],regular
assert stress['passed']==stress['tests']==6 and stress['failed']==0 and not stress['badFiles'],stress
js=list((root/'app/src/main/assets/vtt/js').rglob('*.mjs'))
for f in js:
 r=subprocess.run(['node','--check',str(f)],capture_output=True,text=True)
 if r.returncode:raise RuntimeError(f'{f}: {r.stderr}')
jsonfiles=list((root/'app/src/main/assets/vtt').rglob('*.json'))
for f in jsonfiles:json.loads(f.read_text())
xmlfiles=list((root/'app/src/main').rglob('*.xml'))
for f in xmlfiles:ET.parse(f)
with zipfile.ZipFile(old) as z:
 oldjava={n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith('app/src/main/java/') and n.endswith('.java')}
newjava={f.relative_to(root).as_posix():hashlib.sha256(f.read_bytes()).hexdigest() for f in (root/'app/src/main/java').rglob('*.java')}
assert oldjava==newjava,'Kokoro/Java changed unexpectedly'
summary={'checkpoint':'P186','base':'P185','regular':regular,'stress':stress,'totalTests':regular['tests']+stress['tests'],'totalPassed':regular['passed']+stress['passed'],'testFiles':regular['files']+stress['files'],'focalP186':12,'nodeSyntaxFiles':len(js),'jsonValidated':len(jsonfiles),'xmlValidated':len(xmlfiles),'javaSourcesUnchanged':len(newjava),'bundleModules':87,'suiteMode':'per-file runner, not monolithic','androidVersion':'1.0.186'}
(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
(evidence/'JAVA_SOURCE_HASHES.json').write_text(json.dumps({'previous':'P185','identical':True,'sources':newjava},indent=2))
(evidence/'IMPLEMENTATION_EVIDENCE.json').write_text(json.dumps({'source':'SWE09 Grand Prix PDF pp.49-53, previously targeted P185; no new full-book certification','code':['app/src/main/assets/vtt/js/adventure/jewelGrandPrixDice.mjs','app/src/main/assets/vtt/js/adventure/jewelOfYavinOperations.mjs','app/src/main/assets/vtt/js/core/controller.mjs','app/src/main/assets/vtt/js/core/uiBridge.mjs','app/src/main/assets/vtt/js/core/uiBridge.bundle.js'],'effects':['Actual narrative dice with pilot skill and setback for terrain/missed buoy','challenge upgrades for real Despair; EotE critical table applied to actual selected vehicle','tibannuck system strain applied to selected vehicle','persisted dice faces, source receipt, vehicle effects in controller state','atomic rollback for invalid/duplicate action, three reopens'], 'notCertified':['Five adventures scene-by-scene against PDFs','EotE/AoR 360 talents semantic equivalence','Full Grand Prix race source and all rival AI','Android physical build and Kokoro audio'], 'tests':'evidence/P186/TEST_SUMMARY.json'},indent=2,ensure_ascii=False))
(root/'CURRENT_CHECKPOINT.md').write_text('''# Braseiro Star Wars VTT — R54 P186 (13/10/2026)\n\nBase P185 verificada SHA256 ac8110a640908ef0b59ed7b28709b26151663aa5e49e23ae623159eeefba4813.\n\nImplementado: Grande Prêmio SWE09 com rolagem real Pilotagem Planetária, dificuldades/reveses ambientais e boia perdida no pool efetivo; Desespero/Tensão/crítico aplicados ao veículo selecionado usando tabela EotE; recibos de faces e pool revalidados, rollback e três reaberturas; Android bridge e bundle offline 87 módulos.\n\nTestes: 2751/2751 regulares em 305 arquivos, 6/6 stress, total 2757/2757 por arquivo; 12 testes P186. Fontes Java/Kokoro 8/8 byte a byte preservadas. Android 1.0.186.\n\nAinda pendente: regras bancárias/fuga, cenas completas das cinco aventuras, certificação EotE/AoR e expansões, APK/Android real. Não declarar certificação editorial integral.\n''')
handoff='''# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P186\n\n**Base:** P185 ZIP SHA256 `ac8110a640908ef0b59ed7b28709b26151663aa5e49e23ae623159eeefba4813`; preservar sem regressão.\n\n## Código P186\n\n- Novo `jewelGrandPrixDice.mjs`: rolagem real com `buildPositivePool` e `rollPool`, característica Agilidade e perícia Pilotagem Planetária do personagem; dificuldade Difícil, revés de tibannuck/beldon e revés de boia perdida no **pool efetivo**; melhorias opcionais de dificuldade até três para Desespero real. Validação de cada face de dado, pool e símbolos em replay, não confiar em resultados narrados.\n- `jewelOfYavinOperations.mjs`: P186 recebe `pilotEvidence`/`vehicleId`, valida correspondência entre dados, sucesso, boia e perigos, preserva recibos P184/P185 sem prova de rolagem.\n- `GameController.resolveJewelGrandPrixLeg`: ação atômica que exige veículo real ativo, aplica Tensão de sistemas e crítico real via `applyVehicleCritical` tabela EotE, inclusive modificador -20 de tibannuck; salva veículo e mundo juntos, com rollback para erros e duplicação.\n- `GameController.adjudicateJewelOperation('race_roll', ...)` e `BraseiroUiBridge.resolveJewelGrandPrixLeg` expõem operação ao Android; bundle offline **87 módulos**. Android `1.0.186`.\n- **Limite:** críticos são persistidos no estado de veículo do controlador, não há ainda certificação de todos os obstáculos/rivais nem comparação integral do livro. O modo manual anterior continua acessível para saves legados.\n\n## Testes e integridade\n\n- `tests/uae-p186-grand-prix-dice-vehicle-atomic.test.mjs`: **12/12**, incluindo replay antifalsificação, dificuldade efetiva, Tensão, críticos, 3 reaberturas, idempotência, rollback, bridge.\n- `evidence/P186/TEST_SUMMARY.json`: **2751/2751 regulares + 6/6 stress = 2757/2757**, 311 arquivos por execução individual, não suíte monolítica.\n- Java/Kokoro **8/8 byte a byte idênticos P185**. ZIP completo + SHA256 + delta verificável. Sem compilação APK nem teste Android físico.\n\n## Próximas prioridades P187+\n\n1. Completar corrida (rival AI, velker, gás), efeitos de veículos e operações bancárias/traição/fuga SWE09 contra PDFs.\n2. New Meen, Geonosis e Teemo SWE02; concluir cinco aventuras cena a cena com PDFs e testes de migração/reabertura.\n3. Certificar semanticamente AoR 360/360, EotE e expansões; consumidores declarados não equivalem a equivalência com livros.\n4. Compilar APK no runner BRELGA somente com CI real verificável, testar instalação, persistência e Kokoro/TTS Android.\n5. Fazer P187+ apenas com código substantivo, testes, ZIP Android Studio integral, SHA/delta/handoff e readback Biblioteca. Nunca inventar CI, APK ou certificação.\n'''
(root/'HANDOFF_STAR_WARS_EDGE_R54_P186_2026-10-13.md').write_text(handoff)
(out/'HANDOFF_STAR_WARS_EDGE_R54_P186_2026-10-13.md').write_text(handoff)
files={f.relative_to(root).as_posix():f for f in root.rglob('*') if f.is_file()}
full=out/'BRASEIRO_STAR_WARS_EDGE_R54_P186_SOURCE_CHECKPOINT.zip'
delta=out/'BRASEIRO_STAR_WARS_EDGE_R54_P185_TO_P186_DELTA.zip'
with zipfile.ZipFile(old) as z:
 oldhash={n:hashlib.sha256(z.read(n)).digest() for n in z.namelist()}
assert not (set(oldhash)-set(files)),f'Deleted files: {set(oldhash)-set(files)}'
changed=[n for n,f in sorted(files.items()) if n not in oldhash or hashlib.sha256(f.read_bytes()).digest()!=oldhash[n]]
with zipfile.ZipFile(full,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for n,f in sorted(files.items()):z.write(f,n)
with zipfile.ZipFile(delta,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for n in changed:z.write(files[n],n)
with zipfile.ZipFile(full) as z:
 assert z.testzip() is None
 fullhash={n:hashlib.sha256(z.read(n)).digest() for n in z.namelist()}
with zipfile.ZipFile(delta) as z:
 assert z.testzip() is None
 for n in z.namelist():oldhash[n]=hashlib.sha256(z.read(n)).digest()
assert fullhash==oldhash,'Delta reconstruction failed'
sha=hashlib.sha256(full.read_bytes()).hexdigest()
(out/(full.name+'.sha256')).write_text(f'{sha}  {full.name}\n')
print(json.dumps({'checkpoint':'P186','zip':str(full),'sha256':sha,'size':full.stat().st_size,'files':len(files),'changedFiles':len(changed),'deltaBytes':delta.stat().st_size,'deltaReproduced':True,'tests':summary['totalPassed'],'javaHashesIdentical':True,'syntax':{'mjs':len(js),'json':len(jsonfiles),'xml':len(xmlfiles)}},indent=2))
