import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
const require=createRequire(path.join(process.env.ECO_UI_DEPS,'package.json'));
const {chromium}=require('playwright-core');
const html=fs.readFileSync(new URL('../app/src/main/assets/index.html',import.meta.url),'utf8').replaceAll('{{ECO_APP_ICON}}','data:image/svg+xml,%3Csvg%20xmlns=%22http://www.w3.org/2000/svg%22%20viewBox=%220%200%2032%2032%22%3E%3Ccircle%20cx=%2216%22%20cy=%2216%22%20r=%2215%22%20fill=%22%2399dd55%22/%3E%3C/svg%3E');
const fixture={version:1,uiTheme:'dark',cases:[{id:'browser-a',nome:'Caso fictício A',sessoes:[]},{id:'browser-b',nome:'Caso fictício B',sessoes:[]}],appointments:[]};
const browser=await chromium.launch({executablePath:process.env.ECO_CHROME,headless:true,args:['--no-sandbox']});
const out=path.resolve('build/eco-ui');fs.mkdirSync(out,{recursive:true});
try{
 const page=await browser.newPage({viewport:{width:393,height:852},deviceScaleFactor:1,isMobile:true,hasTouch:true});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.setContent(html);
 await page.evaluate(f=>{let raw=JSON.stringify(f);const editors={};window.NativeVault={loadEditorDraft:slot=>editors[slot]||'',saveEditorDraft:(slot,json)=>{editors[slot]=json;return 'OK'},clearEditorDraft:(slot,id)=>{if(JSON.parse(editors[slot]||'{}').value?.id===id)delete editors[slot];return 'OK'},loadState:()=>raw,saveState:x=>{raw=x;return 'OK'},getEngineInfo:()=>JSON.stringify({semanticAvailable:true,nliAvailable:true,rerankerAvailable:true,llmAvailable:true}),printReport:()=>{throw Error('Unexpected print')}};window.appUnlock();},fixture);
 const home=()=>page.locator('#bottom [data-id="home"]').click();
 const tile=id=>page.locator('.eco-home-grid [data-id="'+id+'"]');
 for(const width of [360,393,415]){
  await page.setViewportSize({width,height:852});
  for(const theme of ['dark','light']){
   if(await page.locator('html').getAttribute('data-theme')!==theme)await page.locator('#themeToggle').click();
   assert.equal(await page.locator('html').getAttribute('data-theme'),theme);
   const metrics=await page.locator('.eco-home-tile').evaluateAll(tiles=>tiles.map(t=>{const r=t.getBoundingClientRect();return {width:r.width,height:r.height,x:r.x,y:r.y,scrollWidth:t.scrollWidth,clientWidth:t.clientWidth,scrollHeight:t.scrollHeight,clientHeight:t.clientHeight}}));
   assert.equal(metrics.length,16);
   assert.ok(metrics.every(r=>Math.abs(r.width-r.height)<=2&&r.scrollWidth<=r.clientWidth+2&&r.scrollHeight<=r.clientHeight+2),'square dimensions or clipped tile');
   assert.ok(Math.abs(metrics[0].y-metrics[1].y)<=1&&metrics[2].y>metrics[0].y,'expected two columns');
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'horizontal overflow');
   await page.screenshot({path:path.join(out,'home-'+width+'-'+theme+'.png'),fullPage:true});
  }
 }
 const modules=await page.evaluate(()=>ecoHomeModules.map(x=>[x[0],x[4]]));
 for(const [id,route] of modules){
   await home();await tile(id).click();
   if(route==='case'||route==='manual'){
     await page.locator('.eco-home-pick [data-id="browser-b"]').click();
     assert.equal(await page.evaluate(()=>openedId),'browser-b');
     assert.equal(await page.evaluate(()=>screen),route==='manual'?'manualSession':'case');
     if(route==='case')assert.equal(await page.evaluate(()=>tab),id);
     if(['assistenteIA','sonhos','genograma','documentos','atendimento'].includes(id))await page.screenshot({path:path.join(out,id+'.png'),fullPage:true});
   }else assert.equal(await page.evaluate(()=>screen),route==='new'?'wizard':id);
 }
 await home();await tile('manual').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 await page.locator('#manualSessTexto').fill('Nota de teste sem áudio.');
 await page.locator('[data-act="saveManualSession"]').click();
 assert.ok(await page.evaluate(()=>state.cases.find(x=>x.id==='browser-b').sessoes.some(s=>s.texto==='Nota de teste sem áudio.')));
 assert.equal(await page.evaluate(()=>state.cases.find(x=>x.id==='browser-a').sessoes.length),0);
 // Dream form uses literal suggestions, preserves professional input, and does not auto-save.
 await home();await tile('sonhos').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 await page.locator('#dreamTexto').fill('Sonhei com minha mãe na cozinha. Não senti medo. Senti alegria. Vi uma porta.');
 await page.locator('#dreamPersonagens').fill('Figura já revisada');
 await page.locator('[data-act="ecoDreamExtractFields"]').click();
 assert.equal(await page.locator('#dreamPersonagens').inputValue(),'Figura já revisada');
 assert.match(await page.locator('#dreamLugares').inputValue(),/cozinha/);
 assert.equal(await page.locator('#dreamAfetos').inputValue(),'alegria');
 assert.match(await page.locator('#dreamSimbolos').inputValue(),/porta/);
 assert.equal(await page.evaluate(()=>state.cases.find(c=>c.id==='browser-b').sonhosList?.length||0),0);
 const samples=await page.evaluate(()=>{
  const c={id:'test-dream',sessoes:[{id:'s1',data:'2026-11-25',origem:'manual',texto:'Sonhei com uma casa. Senti medo.'}],sonhosList:[]};
  const d=ecoDreamCandidates(c)[0];c.sonhosList.push({...d,originKey:'old-key',originalQuote:d.texto});
  return {first:d,duplicates:ecoDreamCandidates(c).length,sizes:[1,3,10,100].map(count=>ecoCloudSize(count,[{count:1},{count:100}]))};
 });
 assert.match(samples.first.texto,/Sonhei/);assert.equal(samples.duplicates,0);
 assert.ok(samples.sizes.every((x,i,a)=>i===0||x>a[i-1]));
 await home();await tile('genograma').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 const graph=page.locator('.eco-genogram-svg');assert.equal(await graph.count(),1);
 assert.equal(await graph.getAttribute('viewBox'),'0 0 360 480');
 assert.ok(await graph.locator('g.gen-node title').count()>=1);
 await graph.screenshot({path:path.join(out,'genograma-legivel.png')});
 // Integrated RAG: test the real UI and callback routing with a simulated native bridge.
 await home();await tile('assistenteIA').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 await page.locator('[data-act="ecoAiScope"][data-id="integrado"]').click();
 await page.locator('#ecoAiQuery').fill('Organize o relato e as referências teóricas, sem inferir diagnóstico.');
 await page.evaluate(()=>{window.ecoTestRequests=[];window.NativeVault.analyzeSemantic=raw=>{window.ecoTestRequests.push(JSON.parse(raw));return 'OK';};});
 await page.locator('[data-act="ecoAiStart"]').click();
 const catalog=await page.evaluate(()=>{const rows=ecoKnowledgeRows(),chunks=ecoLibraryChunks();return {chapters:rows.filter(r=>r.type==='capitulos').length,glossary:rows.filter(r=>r.type==='glossario').length,entries:rows.length,chunks:chunks.length,empty:chunks.filter(r=>!r.text.trim()).length};});
 assert.equal(catalog.chapters,14);assert.equal(catalog.glossary,61);assert.equal(catalog.entries,117);assert.equal(catalog.chunks,215);assert.equal(await page.evaluate(()=>ecoLibraryChunks().filter(x=>String(x.entry).startsWith('perspective:')).length),42);assert.equal(catalog.empty,0);
 const first=await page.evaluate(()=>window.ecoTestRequests[0]);
 assert.equal(first.scope,'todos');assert.ok(first.rows.length>0);
 assert.ok(first.rows.every(x=>x.sessionId&&x.text.includes('Nota de teste')));
 await page.evaluate(()=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,count:x.rows.length,scope:x.scope,hits:[{i:0,score:.75}]}));});
 const second=await page.evaluate(()=>window.ecoTestRequests[1]);
 assert.equal(second.scope,'desconhecido');assert.equal(second.cacheScope,'library');
 assert.ok(second.rows.length>0&&second.rows.every(x=>!x.sessionId&&!x.text.includes('Nota de teste')));
 const theoryIndex=second.rows.findIndex(x=>x.text.length<1200);
 assert.ok(theoryIndex>=0);
 await page.evaluate(i=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,count:x.rows.length,scope:x.scope,hits:[{i,score:.8}]}));},theoryIndex);
 const third=await page.evaluate(()=>window.ecoTestRequests[2]);
 assert.equal(third.mode,'rerank');assert.equal(third.rows.length,2);
 await page.evaluate(()=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,count:2,hits:[{i:1,score:.9},{i:0,score:.8}],skipped:[]}));});
 await page.locator('#ecoAiPick_0').check();await page.locator('#ecoAiPick_1').check();
 await page.locator('[data-act="ecoAiGenerate"]').click();
 const fourth=await page.evaluate(()=>window.ecoTestRequests[3]);
 assert.equal(fourth.mode,'generate');assert.match(fourth.sources[0].text,/REFERÊNCIA TEÓRICA/);assert.match(fourth.sources[1].text,/NOTA PROFISSIONAL/);
 await page.evaluate(()=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,sourceCount:2,clinicalValidation:false,draft:'Nota profissional registrada [S2]. Referência teórica [S1].',model:'fixture'}));});
 await page.locator('#ecoAiArchiveTitle').fill('Síntese integrada fictícia');
 await page.locator('#ecoAiArchiveConfirm').check();
 await page.locator('[data-act="ecoAiArchiveSave"]').click();
 const result=await page.evaluate(()=>{const c=state.cases.find(x=>x.id==='browser-b'),rec=c.ecoAiArchive.at(-1);return {scope:rec.scope,sources:rec.sources.map(x=>x.scope),valid:ecoAiArchiveValid(c,rec),other:state.cases.find(x=>x.id==='browser-a').ecoAiArchive?.length||0};});
 assert.deepEqual(result,{scope:'integrado',sources:['teoria','manual'],valid:true,other:0});
 // Monthly atlas and actual browser PDF pagination, exclusively synthetic records.
 const atlas=await page.evaluate(()=>{
  const s=(id,data,origem,tags)=>({id,data,origem,mlTags:tags,mlEvidence:[],texto:'Nota fictícia'});
  const c={id:'atlas-fixture',nome:'Paciente de teste',sessoes:[s('a1','2026-10-12','manual',['sono','sono']),s('a2','2026-10-12','audio',['família']),s('a3','2026-11-01','manual',['sono'])],sonhosList:[{data:'2026-10-12',texto:'Sonho independente'},{data:'2026-10-12',texto:'Fonte ausente',sessionId:'missing'}]};
  const rows=ecoAtlasMonthly(c),filtered=ecoAtlasMonthly(c,{sessionId:'a1'}),period=ecoAtlasMonthly(c,{docDateFrom:'2026-11-01'});
  return {rows,filtered,period};
 });
 assert.deepEqual(atlas.rows,[{month:'2026-10',sessions:2,manual:1,audio:1,tags:2,dreams:1,evidence:0},{month:'2026-11',sessions:1,manual:1,audio:0,tags:1,dreams:0,evidence:0}]);
 assert.equal(atlas.filtered[0].sessions,1);assert.equal(atlas.filtered[0].dreams,0);assert.equal(atlas.period.length,1);
 const htmlDoc=await page.evaluate(()=>{
  const text=Array.from({length:220},(_,i)=>'Parágrafo fictício '+(i+1)+': atenção, vínculos, revisão, emoções e informações registradas.').join('\n');
  const c={id:'pdf-fixture',nome:'Paciente Fictício',sessoes:[{id:'s1',data:'2026-11-25',origem:'manual',texto:text,mlTranscricao:text,mlTags:['sono']},{id:'s2',data:'2026-10-25',origem:'manual',texto:'EXCLUIR_PELO_FILTRO'}],diarioNotas:[{texto:'MARCADOR_PRIVADO_NUNCA_IMPRIMIR'}]};
  return ecoFormatClinicalDoc(c,'ficha',{identificacao:true,sessoes:true,bruto:true,graficos:true},{confirmed:true,sessionId:'s1',profNome:'Profissional fictício'});
 });
 assert.ok(htmlDoc.includes('Atlas longitudinal'));assert.ok(htmlDoc.includes('Parágrafo fictício 220'));
 assert.ok(!htmlDoc.includes('EXCLUIR_PELO_FILTRO')&&!htmlDoc.includes('MARCADOR_PRIVADO_NUNCA_IMPRIMIR'));
 const printPage=await browser.newPage();await printPage.setContent(htmlDoc);await printPage.emulateMedia({media:'print'});
 const pdf=await printPage.pdf({path:path.join(out,'documento-ficticio-a4.pdf'),preferCSSPageSize:true,printBackground:true});
 const pdfText=pdf.toString('latin1');assert.ok(pdfText.startsWith('%PDF-'));
 assert.ok((pdfText.match(/\/Type\s*\/Page\b/g)||[]).length>=3,'Expected multipage A4 output');
 await printPage.close();
 // Real Atlas filter, case switch, and source navigation.
 await page.evaluate(()=>act('nav','atlas'));
 await page.locator('#ecoAtlasCase').selectOption('browser-b');
 await page.locator('#ecoAtlasMode').selectOption('audio');
 await page.locator('[data-act="ecoAtlasFilter"]').click();
 assert.equal(await page.locator('[data-act="ecoAtlasOpenSession"]').count(),0);
 await page.locator('#ecoAtlasMode').selectOption('manual');
 await page.locator('[data-act="ecoAtlasFilter"]').click();
 assert.equal(await page.locator('[data-act="ecoAtlasOpenSession"]').count(),1);
 const date=await page.evaluate(()=>state.cases.find(c=>c.id==='browser-b').sessoes[0].data);
 await page.locator('#ecoAtlasFrom').fill(date);await page.locator('#ecoAtlasTo').fill(date);
 await page.locator('[data-act="ecoAtlasFilter"]').click();
 assert.equal(await page.locator('[data-act="ecoAtlasOpenSession"]').count(),1);
 await page.locator('#ecoAtlasCase').selectOption('browser-a');
 assert.equal(await page.locator('#ecoAtlasFrom').inputValue(),'');
 assert.equal(await page.locator('#ecoAtlasMode').inputValue(),'todos');
 assert.equal(await page.locator('[data-act="ecoAtlasOpenSession"]').count(),0);
 await page.locator('#ecoAtlasCase').selectOption('browser-b');
 const sessionButton=page.locator('[data-act="ecoAtlasOpenSession"]');
 const panel=sessionButton.locator('xpath=ancestor::details[1]');
 if(!await panel.evaluate(x=>x.open))await panel.locator('summary').click();
 await sessionButton.click();
 assert.equal(await page.evaluate(()=>openedId),'browser-b');assert.equal(await page.evaluate(()=>screen),'session');
 // Multi-batch semantic retrieval: no silent loss, global source indices, separated theoretical corpus.
 const batchResult=await page.evaluate(()=>{
  const rows=Array.from({length:2600},(_,i)=>({text:'Registro fictício '+i,speaker:'paciente',sourceScope:'paciente',sessionId:'s'+i})).concat(Array.from({length:173},(_,i)=>({text:'Teoria fictícia '+i,speaker:'desconhecido',sourceScope:'teoria'})));
  const batches=ecoAiBatchPlan(rows,'integrado'),r={rows,scope:'integrado',hits:[]};
  for(const batch of batches){r.groupIndices=batch.indices;ecoAiMergeHits(r,batch.indices.slice(0,20).map((_,i)=>({i,score:.5+i/100})));}
  return {sizes:batches.map(x=>x.indices.length),unique:new Set(batches.flatMap(x=>x.indices)).size,covered:batches.flatMap(x=>x.indices).length,clinical:r.hits.filter(x=>rows[x.i].sourceScope==='paciente').length,theory:r.hits.filter(x=>rows[x.i].sourceScope==='teoria').length,byteBound:ecoAiBatchPlan(Array.from({length:100},()=>({text:'x'.repeat(10000),sourceScope:'paciente'})),'paciente').map(x=>x.indices.length)};
 });
 assert.deepEqual(batchResult.sizes,[1200,1200,200,173]);assert.equal(batchResult.unique,2773);assert.equal(batchResult.covered,2773);
 assert.equal(batchResult.clinical,10);assert.equal(batchResult.theory,10);assert.equal(batchResult.byteBound.length,3);
 // NLI selectors can reach a passage beyond the old 1200-row limit without dropping A.
 const nli=await page.evaluate(()=>{
  const c={id:'nli-large',sessoes:[{id:'long',data:'2026-11-25',origem:'manual',texto:Array.from({length:1305},(_,i)=>'Nota fictícia '+i+'.').join('\n')}]};
  const old=[ecoNliScope,ecoNliFrom,ecoNliTo,ecoNliA,ecoNliB,ecoNliPage];
  try{ecoNliScope='manual';ecoNliFrom='';ecoNliTo='';ecoNliA='0';ecoNliB='';ecoNliPage=13;
   const html=ecoNliPanel(c),doc=new DOMParser().parseFromString(html,'text/html');
   return {a:doc.querySelector('#ecoNliA').value,last:!!doc.querySelector('#ecoNliB option[value="1304"]'),count:doc.querySelector('#ecoNliB').options.length,page:doc.body.textContent.includes('Página 14 de 14')};
  }finally{[ecoNliScope,ecoNliFrom,ecoNliTo,ecoNliA,ecoNliB,ecoNliPage]=old;}
 });
 assert.deepEqual(nli,{a:'0',last:true,count:6,page:true});
 // Real DOM: recover draft from case B without exposing it to case A.
 await page.evaluate(()=>{window.ecoDraftFixture={};Object.assign(window.NativeVault,{
  saveFormDraft:(id,raw)=>{window.ecoDraftFixture[id]=raw;return 'OK';},loadFormDraft:id=>window.ecoDraftFixture[id]||'',clearFormDraft:(id,sid)=>{if(JSON.parse(window.ecoDraftFixture[id]||'{}').session?.id===sid)delete window.ecoDraftFixture[id];return 'OK';}
 });act('nav','home');});
 await tile('manual').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 await page.locator('#manualSessTexto').fill('Rascunho fictício não arquivado.');
 await page.locator('[data-act="openCase"][data-case="browser-b"]').click();
 await home();await tile('manual').click();await page.locator('.eco-home-pick [data-id="browser-a"]').click();
 assert.equal(await page.locator('#manualSessTexto').inputValue(),'');
 await home();await tile('manual').click();await page.locator('.eco-home-pick [data-id="browser-b"]').click();
 assert.equal(await page.locator('#manualSessTexto').inputValue(),'Rascunho fictício não arquivado.');
 await page.locator('[data-act="saveManualSession"]').click();
 assert.ok(await page.evaluate(()=>state.cases.find(c=>c.id==='browser-b').sessoes.some(s=>s.texto==='Rascunho fictício não arquivado.')));
 assert.equal(await page.evaluate(()=>window.ecoDraftFixture['browser-b']),undefined);
 await home();await tile('agenda').click();
 await page.locator('#ecoFinCase').selectOption('browser-b');await page.locator('#ecoFinValue').fill('150,50');await page.locator('#ecoFinRef').fill('Fatura fictícia');
 await page.locator('[data-act="ecoFinAdd"]').click();assert.equal(await page.evaluate(()=>state.ecoFinance[0].cents),15050);
 await page.locator('[data-act="ecoFinPaid"]').click();assert.equal(await page.evaluate(()=>state.ecoFinance[0].status),'pago');
 await page.screenshot({path:path.join(out,'financeiro.png'),fullPage:true});
 await home();await tile('settings').click();
 await page.locator('#ecoTemplateTitle').fill('Questionário fictício');await page.locator('#ecoTemplateQuestions').fill('Qual a pergunta?');
 await page.locator('[data-act="ecoTemplateAdd"]').click();assert.equal(await page.evaluate(()=>state.ecoQuestionTemplates[0].questions[0]),'Qual a pergunta?');
 await page.evaluate(()=>{window.NativeVault.databaseDiagnostics=()=>JSON.stringify({integrity:'ok',foreignKeysOk:true,_pacientes:2,sessoes:2,segmentos:0,freeBytes:104857600,checkpoints:[{slot:0,date:Date.now(),bytes:2000}]});});
 await page.locator('[data-act="ecoHealthCheck"]').click();assert.equal(await page.locator('[data-act="ecoCheckpointRestore"]').count(),1);
 await page.screenshot({path:path.join(out,'integridade.png'),fullPage:true});
 // Clinical form recovery uses case/kind identity and does not modify the approved record.
 await page.evaluate(()=>{const journal={};Object.assign(window.NativeVault,{
  loadClinicalDraft:(c,k)=>journal[c+'|'+k]||'',
  saveClinicalDraft:(c,k,raw)=>{journal[c+'|'+k]=raw;return 'OK'},
  clearClinicalDraft:(c,k,id)=>{if(JSON.parse(journal[c+'|'+k]||'{}').id===id)delete journal[c+'|'+k];return 'OK'}
 });act('openCase','browser-a');act('caseTab','enquadre');});
 await page.locator('#enq_frequencia').evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await page.locator('#enq_frequencia').fill('Rascunho de frequência fictício');
 await home();
 await page.evaluate(()=>{act('openCase','browser-b');act('caseTab','enquadre');});
 assert.equal(await page.locator('#ecoClinicalDraftPanel').count(),0);
 assert.notEqual(await page.locator('#enq_frequencia').inputValue(),'Rascunho de frequência fictício');
 await page.evaluate(()=>{act('openCase','browser-a');act('caseTab','enquadre');});
 assert.equal(await page.locator('#ecoClinicalDraftPanel').count(),1);
 assert.equal(await page.locator('#enq_frequencia').isDisabled(),true);
 assert.notEqual(await page.evaluate(()=>state.cases.find(c=>c.id==='browser-a').enquadre?.frequencia),'Rascunho de frequência fictício');
 await page.locator('[data-act="ecoClinicalDraftResume"]').click();
 assert.equal(await page.locator('#enq_frequencia').inputValue(),'Rascunho de frequência fictício');
 await page.locator('[data-act="ecoSaveEnquadre"]').evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await page.locator('[data-act="ecoSaveEnquadre"]').click();
 assert.equal(await page.evaluate(()=>state.cases.find(c=>c.id==='browser-a').enquadre.frequencia),'Rascunho de frequência fictício');
 assert.equal(await page.locator('#ecoClinicalDraftPanel').count(),0);
 // Unsaved chronology and dream text must survive leaving and returning to their patient.
 await page.evaluate(()=>act('caseTab','cronologia'));
 const bio=page.locator('#bioTitulo');
 await bio.evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await bio.fill('Evento biográfico fictício ainda não salvo');
 await home();
 await page.evaluate(()=>{act('openCase','browser-a');act('caseTab','cronologia');});
 assert.equal(await page.locator('#ecoClinicalDraftPanel').count(),1);
 await page.locator('[data-act="ecoClinicalDraftResume"]').click();
 assert.equal(await bio.inputValue(),'Evento biográfico fictício ainda não salvo');
 const bioSave=page.locator('[data-act="ecoAddBioEvent"]');
 await bioSave.evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await bioSave.click();
 assert.equal(await page.evaluate(()=>state.cases.find(c=>c.id==='browser-a').cronologia.some(x=>x.titulo==='Evento biográfico fictício ainda não salvo')),true);
 assert.equal(await page.locator('#ecoClinicalDraftPanel').count(),0);
 await page.evaluate(()=>act('caseTab','sonhos'));
 const dream=page.locator('#dreamTexto');
 await dream.evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await dream.fill('Sonhei com minha mãe em uma casa.');
 await home();
 await page.evaluate(()=>{act('openCase','browser-a');act('caseTab','sonhos');});
 await page.locator('[data-act="ecoClinicalDraftResume"]').click();
 assert.equal(await dream.inputValue(),'Sonhei com minha mãe em uma casa.');
 const dreamSave=page.locator('[data-act="ecoSaveDream"]');
 await dreamSave.evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await dreamSave.click();
 assert.equal(await page.evaluate(()=>state.cases.find(c=>c.id==='browser-a').sonhosList.some(x=>x.texto==='Sonhei com minha mãe em uma casa.')),true);
 assert.equal(await page.locator('#ecoClinicalDraftPanel').count(),0);
 // Perspective comparison: three independently retrieved author corpora, patient-only quote and explicit non-archival.
 await page.evaluate(()=>{
  const c=state.cases.find(x=>x.id==='browser-a');c.sessoes.push({id:'perspectivas-fixture',data:'2026-11-25',origem:'texto',mlSegments:[{id:'p1',speaker:'paciente',text:'Quando alguém se aproxima, eu me afasto.',startMs:1000},{id:'t1',speaker:'terapeuta',text:'MARCADOR_TERAPEUTA_EXCLUIR',startMs:2000}]});
  window.ecoTestRequests=[];window.NativeVault.analyzeSemantic=raw=>{window.ecoTestRequests.push(JSON.parse(raw));return 'OK'};
  act('openCase','browser-a');act('caseTab','perspectivas');
 });
 assert.equal(await page.locator('#ecoPerspectivePassage option').count(),1);
 assert.equal(await page.locator('#ecoPerspectiveQuote').inputValue(),'Quando alguém se aproxima, eu me afasto.');
 const perspectives=await page.evaluate(()=>({authors:ecoPerspectiveAuthors,groups:new Set(ecoPerspectiveCatalog.map(x=>x[0])).size,rows:ecoPerspectiveCatalogRows().length,isolated:ecoPerspectiveAuthors.every(a=>ecoPerspectiveTheory(a).length>=2&&ecoPerspectiveTheory(a).every(x=>x.author===a&&x.ref))}));
 assert.equal(perspectives.authors.length,21);assert.equal(new Set(perspectives.authors).size,21);assert.equal(perspectives.groups,6);assert.equal(perspectives.rows,42);assert.equal(perspectives.isolated,true);
 await page.locator('[data-act="ecoPerspectivePreset"][data-id="todos"]').click();
 assert.equal(await page.locator('[data-eco-perspective-author]:checked').count(),21);
 await page.locator('[data-act="ecoPerspectivePreset"][data-id="nenhum"]').click();
 assert.equal(await page.locator('[data-eco-perspective-author]:checked').count(),0);
 await page.locator('[data-act="ecoPerspectivePreset"][data-id="contemporanea"]').click();
 assert.equal(await page.locator('[data-eco-perspective-author]:checked').count(),6);
 await page.locator('[data-act="ecoPerspectivePreset"][data-id="todos"]').click();
 await page.locator('[data-act="ecoPerspectiveStart"]').click();
 for(const author of perspectives.authors){
  const request=await page.evaluate(()=>window.ecoTestRequests.at(-1));
  assert.equal(request.query,'Quando alguém se aproxima, eu me afasto.');
  assert.ok(request.rows.length>0&&request.rows.every(x=>x.text.includes(' — '+author+'\n')&&!x.text.includes('MARCADOR_TERAPEUTA_EXCLUIR')));
  const index=request.rows.findIndex(x=>x.text.length<1000);assert.ok(index>=0);
  await page.evaluate(i=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,count:x.rows.length,scope:x.scope,hits:[{i,score:.8}]}));},index);
  const rerank=await page.evaluate(()=>window.ecoTestRequests.at(-1));assert.equal(rerank.mode,'rerank');
  await page.evaluate(()=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,count:x.rows.length,hits:[{i:0,score:.9}],skipped:[]}));});
  const gen=await page.evaluate(()=>window.ecoTestRequests.at(-1));assert.equal(gen.mode,'generate');assert.equal(gen.profile,'perspective');assert.equal(gen.author,author);assert.ok(gen.query.includes('[S1]')&&gen.query.includes('[S2]'));assert.ok(!gen.query.includes(author));assert.equal(gen.sources.length,2);assert.ok(gen.sources[1].text.startsWith(author+' · '));assert.ok(gen.sources[1].focus&&gen.sources[1].text.includes(gen.sources[1].focus));
  await page.evaluate(()=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,sourceCount:x.sources.length,clinicalValidation:false,draft:'Conceito: foco teórico [S2].\nHipótese provisória: possibilidade de explorar a fala [S1].\nJustificativa: relacionar fala [S1] e teoria [S2].\nExemplo: Como vive essa situação?\nInvestigar: outros contextos e alternativas.\nEvitar: certeza ou diagnóstico.',model:'fixture'}));});
  const verify=await page.evaluate(()=>window.ecoTestRequests.at(-1));assert.equal(verify.mode,'nli');assert.equal(verify.pairs.length,1);assert.ok(!verify.pairs[0].a.includes('Quando alguém se aproxima'));
  await page.evaluate(()=>{const x=window.ecoTestRequests.at(-1);window.nativeSemanticResult(JSON.stringify({id:x.id,labels:['contradiction','entailment','neutral'],comparisons:[{i:0,ab:[.1,.7,.2],ba:[.1,.2,.7]}]}));});
 }
 assert.deepEqual(await page.evaluate(()=>ecoPerspectiveRun.results.map(x=>[x.author,x.refsValid])),perspectives.authors.map(a=>[a,true]));
 assert.equal(await page.evaluate(()=>state.cases.find(x=>x.id==='browser-a').ecoAiArchive?.length||0),0);
 await page.screenshot({path:path.join(out,'perspectivas-psicanaliticas.png'),fullPage:true});
 await home();assert.equal(await page.evaluate(()=>ecoPerspectiveRun),null);
 // Personal material is opt-in, never retrieved after archive, and exact substrings are reconstructable.
 const personal=await page.evaluate(()=>{
  const previous=state.ecoPersonalLibrary;
  const body=Array.from({length:135},(_,i)=>'Parágrafo teórico autorizado '+i+' sobre vínculo e escuta. ').join('');
  state.ecoPersonalLibrary=[{id:'p-theory',title:'Notas de estudo',author:'Klein',ref:'Edição declarada',body,semanticConfirmed:true},{id:'not-authorized',title:'NÃO INDEXAR',ref:'Teste',body:'texto excluído'},{id:'archived',title:'ARQUIVADO',ref:'Teste',body:'texto excluído',semanticConfirmed:true,archived:true}];
  const rows=ecoPersonalSemanticChunks(),scope=ecoPerspectiveTheory('Klein');
  const result={rows:rows.length,exact:rows.map(x=>body.slice(x.offset,x.end)).join('')===body,excluded:!ecoLibraryChunks().some(x=>['not-authorized','archived'].includes(x.personalId)),general:ecoLibraryChunks().some(x=>x.personalId==='p-theory'),klein:scope.some(x=>x.personalId==='p-theory'),freud:ecoPerspectiveTheory('Freud').some(x=>x.personalId==='p-theory'),catalogOnly:scope.length===2&&scope.every(x=>x.author==='Klein'&&!x.personalId)};
  state.ecoPersonalLibrary=previous;return result;
 });
 assert.ok(personal.rows>1);assert.equal(personal.exact,true);assert.equal(personal.excluded,true);assert.equal(personal.general,true);assert.equal(personal.klein,false);assert.equal(personal.freud,false);assert.equal(personal.catalogOnly,true);
 // Residual editor tests use the persistent bridge contract with synthetic content.
 await page.evaluate(()=>{act('openCase','browser-a');act('caseTab','seguranca');});
 await page.locator('#safePlano').evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await page.locator('#safePlano').fill('Plano fictício ainda não salvo.');
 await page.evaluate(()=>{act('openCase','browser-b');act('caseTab','seguranca');});
 assert.notEqual(await page.locator('#safePlano').inputValue(),'Plano fictício ainda não salvo.');
 await page.evaluate(()=>{act('openCase','browser-a');act('caseTab','seguranca');});
 assert.equal(await page.locator('#safePlano').isDisabled(),true);
 await page.locator('[data-act="ecoResidualResume"][data-id="browser-a|res_safety"]').click();
 assert.equal(await page.locator('#safePlano').inputValue(),'Plano fictício ainda não salvo.');
 await page.locator('[data-act="ecoSaveSafety"]').click();
 assert.equal(await page.evaluate(()=>state.cases.find(x=>x.id==='browser-a').seguranca.plano),'Plano fictício ainda não salvo.');
 assert.equal(await page.evaluate(()=>window.NativeVault.loadClinicalDraft('browser-a','res_safety')),'');
 await page.evaluate(()=>{act('caseTab','atendimento');});
 await page.locator('#ecoPrepGoals').evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await page.locator('#ecoPrepGoals').fill('Objetivo sintético recuperável.');
 await page.evaluate(()=>{act('caseTab','sintese');act('caseTab','atendimento');});
 await page.locator('[data-act="ecoResidualResume"][data-id="browser-a|res_prep"]').click();
 assert.equal(await page.locator('#ecoPrepGoals').inputValue(),'Objetivo sintético recuperável.');
 await page.locator('[data-act="ecoPrepSave"]').click();
 assert.equal(await page.evaluate(()=>state.cases.find(x=>x.id==='browser-a').ecoPreparation.goals),'Objetivo sintético recuperável.');
 assert.equal(await page.evaluate(()=>window.NativeVault.loadClinicalDraft('browser-a','res_prep')),'');
 await page.evaluate(()=>act('nav','settings'));
 await page.locator('#ecoTemplateTitle').evaluate(e=>{for(let n=e.parentElement;n;n=n.parentElement)if(n.tagName==='DETAILS')n.open=true;});
 await page.locator('#ecoTemplateTitle').fill('Questionário fictício recuperável');
 await page.locator('#ecoTemplateQuestions').fill('Pergunta sintética?');
 await page.evaluate(()=>{act('nav','home');act('nav','settings');});
 await page.locator('[data-act="ecoResidualResume"][data-id="__eco_admin__|res_template"]').click();
 assert.equal(await page.locator('#ecoTemplateTitle').inputValue(),'Questionário fictício recuperável');
 await page.locator('[data-act="ecoTemplateAdd"]').click();
 assert.equal(await page.evaluate(()=>state.ecoQuestionTemplates.some(t=>t.title==='Questionário fictício recuperável')),true);
 assert.equal(await page.evaluate(()=>window.NativeVault.loadClinicalDraft('__eco_admin__','res_template')),'');
 const bibliography=await page.evaluate(()=>({rows:ecoPerspectiveCatalogRows().length,identified:ecoPerspectiveCatalogRows().filter(x=>ecoBibliographicStatus(x.ref).url).length,pdm:ecoPerspectiveCatalogRows('Lingiardi').every(x=>x.ref.includes('2026')&&x.ref.includes('40948049')),unknown:ecoBibliographicStatus('obra não verificada').url}));
 assert.equal(bibliography.rows,42);assert.ok(bibliography.identified>=20);assert.equal(bibliography.pdm,true);assert.equal(bibliography.unknown,'');

 // Exercise the scoped editor in a real DOM, using only simulated records.
 await page.evaluate(()=>{
  ecoLargeVaultMode=true;ecoPagedCase='browser-a';ecoPagedSession='scoped-session';ecoPagedField='texto';
  window.scopedWrites=[];
  window.NativeVault.browseVault=()=>JSON.stringify({text:'Texto sintético anterior',offset:0,end:24,total:24,next:-1,revision:'a'.repeat(64)});
  window.NativeVault.editVaultPage=(...args)=>{window.scopedWrites.push(args);return JSON.stringify({text:args[6],offset:0,end:args[6].length,total:args[6].length,next:-1,revision:'b'.repeat(64)});};
  ecoPagedShow('text');
 });
 await page.locator('#ecoPagedEdit').fill('Trecho sintético revisado');
 page.once('dialog',dialog=>dialog.dismiss());
 await page.locator('[data-act="ecoPagedSave"]').click();
 assert.equal(await page.evaluate(()=>window.scopedWrites.length),0);
 page.once('dialog',dialog=>dialog.accept());
 await page.locator('[data-act="ecoPagedSave"]').click();
 assert.deepEqual(await page.evaluate(()=>window.scopedWrites[0]),['browser-a','scoped-session','texto',0,24,'a'.repeat(64),'Trecho sintético revisado']);
 await page.evaluate(()=>{ecoPagedField='mlTranscricao';ecoPagedShow('text');});
 assert.equal(await page.locator('#ecoPagedEdit').count(),0);
 await page.evaluate(()=>{ecoLargeVaultMode=false;closeModal();});
 // Help is reachable without choosing a patient, and shortcuts explicitly require a selection.
 await page.evaluate(()=>act('nav','home'));
 await page.locator('[data-act="nav"][data-id="help"]').click();
 assert.equal(await page.evaluate(()=>screen),'help');
 assert.equal(await page.locator('.eco-help-step').count(),7);
 assert.equal(await page.locator('#ecoHelpResults details').count(),28);
 await page.locator('#ecoHelpSearch').fill('sonhos');
 assert.ok(await page.locator('#ecoHelpResults details').count()>0);
 assert.ok(await page.locator('#ecoHelpResults details').count()<28);
 await page.locator('#ecoHelpSearch').fill('');
 const selected=await page.evaluate(()=>openedId);
 await page.locator('[data-act="ecoHelpGo"][data-id="perspectivas"]').first().click();
 assert.equal(await page.evaluate(()=>screen),'help');assert.equal(await page.evaluate(()=>openedId),selected);
 await page.locator('[data-act="ecoHelpCase"][data-id="browser-b"]').click();
 assert.equal(await page.evaluate(()=>screen),'case');assert.equal(await page.evaluate(()=>tab),'perspectivas');assert.equal(await page.evaluate(()=>openedId),'browser-b');
 await page.evaluate(()=>act('nav','home'));await page.locator('[data-act="nav"][data-id="help"]').click();
 await page.screenshot({path:path.join(out,'central-ajuda.png'),fullPage:false});
 // Personal module uses only dedicated bridge, never case state.
 await page.evaluate(()=>{let record={version:1,entries:[],revision:'rev0'};window.NativeVault.readSelfAnalysis=()=>JSON.stringify(record);window.NativeVault.saveSelfAnalysis=(revision,raw)=>{if(revision!==record.revision)return 'CONFLICT';record={...JSON.parse(raw),revision:'rev'+Date.now()};return 'OK';};act('nav','home');});
 await page.locator('[data-act="nav"][data-id="selfAnalysis"]').click();
 assert.equal(await page.locator('[data-act="ecoSelfTopic"]').count(),9);
 await page.locator('[data-act="ecoSelfTopic"][data-id="sonhos"]').click();
 await page.locator('#ecoSelfReflection').fill('REGISTRO PESSOAL FICTÍCIO');
 await page.locator('[data-act="ecoSelfSave"]').click();
 assert.equal(await page.evaluate(()=>JSON.parse(window.NativeVault.readSelfAnalysis()).entries.length),1);
 assert.ok(await page.evaluate(()=>!JSON.stringify(state).includes('REGISTRO PESSOAL FICTÍCIO')));
 await page.evaluate(()=>{act('nav','home');act('nav','selfAnalysis');});
 assert.match(await page.locator('#app').textContent(),/REGISTRO PESSOAL FICTÍCIO/);
 await page.locator('[data-act="ecoSelfReview"]').click();
 assert.match(await page.locator('#ecoSelfReviewResult').textContent(),/REGISTRO PESSOAL FICTÍCIO/);
 assert.match(await page.locator('#ecoSelfReviewResult').textContent(),/Transforme a reflexão em aprendizado/);
 // Open an already stored dream and save distinct dream work without modifying the text.
 await page.locator('[data-act="ecoDreamWorkStart"][data-id="single"]').click();
 await page.locator('#ecoDWInsight').fill('COMPREENSÃO PESSOAL SINTÉTICA');
 await page.locator('[data-act="ecoDreamWorkSave"]').click();
 assert.equal(await page.evaluate(()=>JSON.parse(window.NativeVault.readSelfAnalysis()).entries[0].reflection),'REGISTRO PESSOAL FICTÍCIO');
 assert.equal(await page.evaluate(()=>JSON.parse(window.NativeVault.readSelfAnalysis()).entries[0].dreamWork.insight),'COMPREENSÃO PESSOAL SINTÉTICA');
 assert.ok(await page.evaluate(()=>!JSON.stringify(state).includes('COMPREENSÃO PESSOAL SINTÉTICA')));
 await page.screenshot({path:path.join(out,'autoanalise.png')});
 // Responsive audit: actual DOM across every home destination and every case module.
 const layoutReport=[];
 const routes=await page.evaluate(()=>({home:ecoHomeModules.map(x=>[x[0],x[4]]),case:["sintese","atendimento","fichaEvolutiva","sessoes","assistenteIA","perspectivas","compararRelatos","revisaoInteligente","analisar","anamnese","enquadre","investigar","tecnicas","sonhos","genograma","rede","cronologia","documentos","diario","seguranca"]}));
 const fixtureNames=Array.from({length:54},(_,i)=>'Vínculo fictício '+(i+1)+' — '+(i===0?'NomeComComprimentoExtremo'.repeat(4):'contexto relatado em atendimento'));
 await page.evaluate(names=>{const c=state.cases.find(x=>x.id==='browser-b');c.ecoNetwork=names.map((name,i)=>({id:'responsive-net-'+i,name,kind:['familia','amizade','trabalho','comunidade','cuidado','outro'][i%6],relation:['apoio','conflito','misto','distante','nao_informado'][i%5],notes:'Observação sintética.'}));},fixtureNames);
 const measure=async(label)=>{
  const m=await page.evaluate(()=>{
   const root=document.documentElement;
   const overflow=[];
   for(const el of document.querySelectorAll('#app input:not([type=checkbox]):not([type=radio]),#app select,#app textarea,#app .eco-network-node,#app .eco-module-card,#app .eco-home-tile')){
    if(!el.getClientRects().length)continue;
    const r=el.getBoundingClientRect();
    if(r.width>0&&(r.left<-2||r.right>innerWidth+2))overflow.push({id:el.id||el.className,left:Math.round(r.left),right:Math.round(r.right)});
   }
   return {width:innerWidth,documentWidth:root.scrollWidth,overflow};
  });
  layoutReport.push({label,...m});
  assert.ok(m.documentWidth<=m.width+2,label+' document horizontal overflow: '+JSON.stringify(m));
  assert.deepEqual(m.overflow,[],label+' off-screen controls');
 };
 for(const [width,height] of [[320,740],[360,800],[393,852],[415,915],[852,393]]){
  await page.setViewportSize({width,height});
  for(const theme of ['light','dark']){
   if(await page.locator('html').getAttribute('data-theme')!==theme)await page.locator('#themeToggle').click();
   await page.evaluate(()=>act('nav','help'));await measure(width+'x'+height+'/'+theme+'/help');
   await page.evaluate(()=>act('nav','selfAnalysis'));await measure(width+'x'+height+'/'+theme+'/selfAnalysis');
   for(const [id,route] of routes.home){
    await page.evaluate(()=>act('nav','home'));await tile(id).click();
    if(route==='case'||route==='manual')await page.locator('.eco-home-pick [data-id="browser-b"]').click();
    await measure(width+'x'+height+'/'+theme+'/home:'+id);
   }
   for(const id of routes.case){
    await page.evaluate(id=>{act('openCase','browser-b');act('caseTab',id);},id);
    await measure(width+'x'+height+'/'+theme+'/case:'+id);
    if(id==='rede'){
     const nodes=page.locator('.eco-network-node');assert.equal(await nodes.count(),54);
     assert.equal(await nodes.first().locator('strong').textContent(),fixtureNames[0]);
     const clipping=await nodes.evaluateAll(ns=>ns.filter(n=>n.scrollWidth>n.clientWidth+2||n.scrollHeight>n.clientHeight+2).length);assert.equal(clipping,0);
     const branches=await page.locator('.eco-network-branches').evaluate(el=>getComputedStyle(el).gridTemplateColumns.split(' ').length);
     assert.equal(branches,width>=852?2:1);
     if(width===393||width===852)await page.screenshot({path:path.join(out,'rede-'+width+'-'+theme+'.png')});
    }
   }
  }
 }
 await page.evaluate(()=>{act('openCase','browser-b');act('caseTab','rede');});
 await page.locator('.eco-network-node').first().click();
 assert.equal(await page.locator('#ecoNetName').inputValue(),fixtureNames[0]); // full label preserved in the map
 fs.writeFileSync(path.join(out,'responsive-audit.json'),JSON.stringify(layoutReport,null,2));
 console.log('PASS responsive audit: '+layoutReport.length+' route/viewport/theme checks');
 assert.deepEqual(errors,[]);
 console.log('PASS ECO real Chromium DOM: 16 destinations, 3 widths, 2 themes, square layout, manual record isolation');
}finally{await browser.close();}
