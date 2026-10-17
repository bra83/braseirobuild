import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { listLibraryDocuments, searchImportedLibrary, nativeLibraryAvailable, libraryStatusLabel } from '../app/src/main/assets/vtt/js/library/nativeLibrary.mjs';
import { searchGroundedSources } from '../app/src/main/assets/vtt/js/rag/sourceSearch.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';

const bridge={
  listDocuments(){ return JSON.stringify([{docId:'pdf_abc',title:'Manual de teste',status:'INDEXADO',pageCount:20,indexedPages:20,sizeBytes:1000}]); },
  searchDocuments(query,limit){
    return JSON.stringify({results:[{id:'LIB-pdf_abc-P12',docId:'pdf_abc',title:'Manual de teste',fileName:'manual.pdf',source:'Biblioteca · manual.pdf',page:12,excerpt:`Trecho sobre ${query}`,score:10.5}].slice(0,limit)});
  }
};

test('ponte de Biblioteca lista documentos anexados e traduz status',()=>{
  assert.equal(nativeLibraryAvailable(bridge),true);
  const docs=listLibraryDocuments(bridge);
  assert.equal(docs.length,1);
  assert.equal(docs[0].status,'INDEXADO');
  assert.equal(libraryStatusLabel('INDEXADO'),'Pronto para uso offline');
});

test('busca em PDF importado vira fonte local citável',()=>{
  const results=searchImportedLibrary('talentos',{limit:5,bridge});
  assert.equal(results[0].status,'FONTE_IMPORTADA');
  assert.equal(results[0].sourceKind,'library_pdf');
  assert.equal(results[0].page,12);
  assert.equal(results[0].printedPage,null);
  assert.match(results[0].summary,/talentos/i);
});

test('RAG combina corpus auditado e Biblioteca importada sem tratar PDF como regra implementada',()=>{
  const librarySearch=(query,{limit})=>searchImportedLibrary(query,{limit,bridge});
  const found=searchGroundedSources('assunto exclusivo do suplemento',{limit:3,librarySearch});
  assert.equal(found.retrieval.grounded,true);
  assert.equal(found.results[0].status,'FONTE_IMPORTADA');
  assert.notEqual(found.results[0].status,'IMPLEMENTADO');
});

test('contexto do Mestre incorpora PDF da Biblioteca quando a ponte Android está disponível',()=>{
  const previous=globalThis.BraseiroLibrary;
  globalThis.BraseiroLibrary=bridge;
  try {
    const state=createCampaign('Teste Biblioteca');
    const context=buildMasterContext(state,{ruleQuery:'regra do suplemento xyz',intentText:'uso a regra do suplemento xyz',maxRules:4});
    assert.ok(context.rules.some(r=>r.sourceKind==='library_pdf'));
    assert.equal(context.retrieval.grounded,true);
  } finally {
    if (previous===undefined) delete globalThis.BraseiroLibrary; else globalThis.BraseiroLibrary=previous;
  }
});

test('shell Android contém importação privada e indexação PDFBox, sem permissão de armazenamento externo',()=>{
  const java=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/MainActivity.java',import.meta.url),'utf8');
  const gradle=fs.readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8');
  const manifest=fs.readFileSync(new URL('../app/src/main/AndroidManifest.xml',import.meta.url),'utf8');
  assert.match(java,/ACTION_OPEN_DOCUMENT/);
  assert.match(java,/getFilesDir\(\).*library/s);
  assert.match(java,/PDFTextStripper/);
  assert.match(java,/pages\.jsonl/);
  assert.match(gradle,/com\.tom-roush:pdfbox-android:2\.0\.27\.0/);
  assert.doesNotMatch(manifest,/READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|MANAGE_EXTERNAL_STORAGE/);
});

test('shell Android persiste autoridade e permite ativar/desativar fonte sem apagar PDF',()=>{
  const java=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/MainActivity.java',import.meta.url),'utf8');
  assert.match(java,/setDocumentPolicy/);
  assert.match(java,/authorityType/);
  assert.match(java,/ERRATA\|BASE\|SUPLEMENTO\|AVENTURA\|REFERENCIA/);
  assert.match(java,/optBoolean\("enabled", true\)/);
  assert.match(java,/printedPageOffset/);
  assert.match(java,/printedPage\(meta, page\)/);
  assert.match(java,/\.put\("printedPageOffset", meta\.has\("printedPageOffset"\)/);
});

test('Biblioteca permite reindexar PDF preservado e registra falha de indexação',()=>{
  const java=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/MainActivity.java',import.meta.url),'utf8');
  assert.match(java,/reindexDocument/);
  assert.match(java,/markIndexError/);
  assert.match(java,/"ERRO"/);
  assert.match(java,/lastError/);
});
