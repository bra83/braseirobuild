import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeLibraryAuthority, libraryAuthorityLabel, libraryAuthorityWeight, buildAuthorityDecision } from '../app/src/main/assets/vtt/js/rag/sourceAuthority.mjs';
import { mergeGroundedSources } from '../app/src/main/assets/vtt/js/rag/sourceSearch.mjs';
import { updateLibraryDocumentPolicy, searchImportedLibrary } from '../app/src/main/assets/vtt/js/library/nativeLibrary.mjs';

test('tipos de autoridade da Biblioteca são normalizados e rotulados em PT-BR',()=>{
  assert.equal(normalizeLibraryAuthority('errata'),'ERRATA');
  assert.equal(normalizeLibraryAuthority('desconhecido'),'SUPLEMENTO');
  assert.equal(libraryAuthorityLabel('BASE'),'Livro básico');
  assert.ok(libraryAuthorityWeight('ERRATA')>libraryAuthorityWeight('BASE'));
  assert.ok(libraryAuthorityWeight('BASE')>libraryAuthorityWeight('SUPLEMENTO'));
});

test('mecânica testada do motor prevalece sobre qualquer PDF importado',()=>{
  const verified=[{id:'RULE',title:'Regra',status:'TESTADO',score:1,summary:'ok',sourceKind:'rule_matrix'}];
  const imported=[{id:'LIB',title:'Errata',status:'FONTE_IMPORTADA',score:99,summary:'texto',sourceKind:'library_pdf',authorityType:'ERRATA',documentId:'pdf_a'}];
  const results=mergeGroundedSources(verified,imported,{limit:2});
  assert.equal(results[0].id,'RULE');
});

test('PDF importado pode preencher consulta onde a matriz só tem lacuna pendente',()=>{
  const pending=[{id:'PENDING',title:'Criação',status:'A_LER',score:30,summary:'A_AUDITAR',sourceKind:'rule_matrix'}];
  const imported=[{id:'LIB',title:'Livro básico',status:'FONTE_IMPORTADA',score:2,summary:'trecho verificável',sourceKind:'library_pdf',authorityType:'BASE',documentId:'pdf_b'}];
  const results=mergeGroundedSources(pending,imported,{limit:2});
  assert.equal(results[0].id,'LIB');
  assert.equal(results[0].status,'FONTE_IMPORTADA');
});

test('entre PDFs igualmente pertinentes, errata prevalece sobre livro básico e complemento',()=>{
  const imported=[
    {id:'SUP',status:'FONTE_IMPORTADA',score:10,sourceKind:'library_pdf',authorityType:'SUPLEMENTO',documentId:'pdf_s'},
    {id:'BASE',status:'FONTE_IMPORTADA',score:10,sourceKind:'library_pdf',authorityType:'BASE',documentId:'pdf_b'},
    {id:'ERR',status:'FONTE_IMPORTADA',score:10,sourceKind:'library_pdf',authorityType:'ERRATA',documentId:'pdf_e'}
  ];
  const results=mergeGroundedSources([],imported,{limit:3});
  assert.deepEqual(results.map(r=>r.id),['ERR','BASE','SUP']);
  const decision=buildAuthorityDecision(results);
  assert.equal(decision.primary.id,'ERR');
  assert.equal(decision.precedenceApplied,true);
});

test('ponte atualiza autoridade e estado ativo de uma fonte',()=>{
  let args;
  const bridge={
    setDocumentPolicy(...values){ args=values; return JSON.stringify({ok:true,document:{docId:values[0],authorityType:values[1],enabled:values[2],printedPageOffset:Number(values[3])}}); }
  };
  const result=updateLibraryDocumentPolicy('pdf_123',{authorityType:'BASE',enabled:false,printedPageOffset:1},bridge);
  assert.equal(result.ok,true);
  assert.equal(result.document.authorityType,'BASE');
  assert.equal(result.document.enabled,false);
  assert.equal(result.document.printedPageOffset,1);
  assert.deepEqual(args,['pdf_123','BASE',false,'1']);
  assert.throws(()=>updateLibraryDocumentPolicy('pdf_123',{printedPageOffset:1.25},bridge),/inteiro/);
});

test('busca importada preserva autoridade devolvida pelo Android',()=>{
  const bridge={
    listDocuments(){ return '[]'; },
    searchDocuments(){ return JSON.stringify({results:[{id:'x',docId:'pdf_x',title:'Errata',page:2,excerpt:'ajuste',score:4,authorityType:'ERRATA',enabled:true}]}); }
  };
  const [result]=searchImportedLibrary('ajuste',{bridge});
  assert.equal(result.authorityType,'ERRATA');
  assert.equal(result.enabled,true);
  assert.equal(result.authorityWeight,500);
});

test('fonte desativada fica disponível na Biblioteca, mas não chega ao RAG',()=>{
  const bridge={
    listDocuments(){ return JSON.stringify([{docId:'pdf_disabled',title:'Aventura futura',status:'INDEXADO',enabled:false}]); },
    searchDocuments(){ return JSON.stringify({results:[{id:'disabled',docId:'pdf_disabled',title:'Aventura futura',page:1,excerpt:'não usar',score:99,authorityType:'AVENTURA',enabled:false}]}); }
  };
  assert.equal(searchImportedLibrary('futuro',{bridge}).length,0);
  const merged=mergeGroundedSources([], [{id:'disabled',sourceKind:'library_pdf',enabled:false}], {limit:5});
  assert.equal(merged.length,0);
});

test('decisão de autoridade fica disponível para persistência da consulta',async()=>{
  const { createCampaign }=await import('../app/src/main/assets/vtt/js/core/state.mjs');
  const { applyEvent }=await import('../app/src/main/assets/vtt/js/core/events.mjs');
  const s=createCampaign('Autoridade persistente');
  const next=applyEvent(s,{id:'evt_auth',type:'MASTER_RULE_QUERY',at:'2026-09-05T00:00:00Z',payload:{query:'teste',results:[],authority:{policyVersion:1,primary:{id:'X'},precedenceApplied:true,competingSources:1}}});
  assert.equal(next.master.lastRuleAuthority.primary.id,'X');
  assert.equal(next.master.lastRuleAuthority.precedenceApplied,true);
});

test('ponte de reindexação retorna erro legível quando indisponível',async()=>{
  const { reindexLibraryDocument }=await import('../app/src/main/assets/vtt/js/library/nativeLibrary.mjs');
  assert.throws(()=>reindexLibraryDocument('pdf_x',{}),/Reindexação indisponível/);
});
