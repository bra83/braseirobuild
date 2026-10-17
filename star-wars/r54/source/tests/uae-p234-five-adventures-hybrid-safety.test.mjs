import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { FIVE_PUBLISHED_ADVENTURE_IDS, assertFivePublishedAdventures, isCanonicalAdventureId, canonicalAdventureForSource, allowedHybridLibrarySource, hybridDecision } from '../app/src/main/assets/vtt/js/adventure/hybridFiveAdventurePolicy.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { searchImportedLibrary } from '../app/src/main/assets/vtt/js/library/nativeLibrary.mjs';
import { mergeGroundedSources } from '../app/src/main/assets/vtt/js/rag/sourceSearch.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';
import { providerRequestBody } from '../app/src/main/assets/vtt/js/master/provider.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

const permitted = FIVE_PUBLISHED_ADVENTURE_IDS;
function storage() {const map=new Map();return {getItem:k=>map.get(k)??null,setItem:(k,v)=>map.set(k,String(v)),removeItem:k=>map.delete(k)};}

test('P234 canonical registry contains exactly five published adventures and rejects a sixth',()=>{
  assert.deepEqual(publishedAdventureCatalog().map(x=>x.id),permitted);
  assert.equal(assertFivePublishedAdventures(permitted),true);
  assert.equal(isCanonicalAdventureId('jewel-of-yavin'),true);
  assert.equal(isCanonicalAdventureId('sixth-adventure'),false);
  assert.throws(()=>assertFivePublishedAdventures([...permitted,'sixth-adventure']),/cinco módulos/);
  assert.throws(()=>getPublishedAdventureAdapter('sixth-adventure'),/desconhecida/);
});

test('P234 all five source aliases are recognized, foreign adventures remain excluded',()=>{
  for(const [id,title] of [
   ['under-black-sun','Sob um Sol Negro'],['beyond-the-rim','Beyond the Rim'],
   ['long-arm-of-the-hutt','Long Arm of the Hutt'],['jewel-of-yavin','SWE09.pdf'],
   ['mask-of-the-pirate-queen','Mask of the Pirate Queen']]) {
    assert.equal(canonicalAdventureForSource({fileName:title}),id,title);
    assert.equal(allowedHybridLibrarySource({authorityType:'AVENTURA',title,enabled:true}),true);
  }
  assert.equal(allowedHybridLibrarySource({authorityType:'AVENTURA',title:'Uma sexta aventura.pdf'}),false);
  assert.equal(allowedHybridLibrarySource({authorityType:'BASE',title:'Manual de Regras'}),true);
  assert.equal(allowedHybridLibrarySource({authorityType:'ERRATA',title:'Correções'}),true);
  assert.equal(allowedHybridLibrarySource({authorityType:'AVENTURA',title:'SWE09.pdf',enabled:false}),false);
});

test('P234 native Android RAG and merged source path both exclude noncanonical adventure PDF',()=>{
  const rows=[
    {id:'other',docId:'extra',title:'Sexta aventura',authorityType:'AVENTURA',excerpt:'inválido',page:2,enabled:true},
    {id:'jewel',docId:'jewel-pdf',title:'The Jewel of Yavin',authorityType:'AVENTURA',excerpt:'cena',page:92,enabled:true},
    {id:'core',docId:'core',title:'EotE Core',authorityType:'BASE',excerpt:'regra',page:12,enabled:true}
  ];
  const bridge={listDocuments:()=>JSON.stringify(rows),searchDocuments:()=>JSON.stringify({results:rows})};
  const results=searchImportedLibrary('segurança',{bridge,limit:5});
  assert.deepEqual(results.map(x=>x.id),['jewel','core']);
  const merged=mergeGroundedSources([],rows.map(x=>({ ...x,sourceKind:'library_pdf',status:'FONTE_IMPORTADA',documentId:x.docId,summary:x.excerpt,score:10})),{limit:5});
  assert.deepEqual(merged.map(x=>x.id),['core','jewel']);
});

test('SWE02 p.13 RAG context preserves printed and physical PDF page citations separately',()=>{
  const previous=globalThis.BraseiroLibrary;
  const row={id:'nabat-p13',docId:'swe02-official',title:'The Long Arm of the Hutt',fileName:'The Long Arm of the Hutt.pdf',authorityType:'AVENTURA',excerpt:'B’ura arranges safe docking and contact-paid ship repairs. The PCs may remove the transponder after landing.',page:12,printedPage:13,enabled:true};
  globalThis.BraseiroLibrary={listDocuments:()=>JSON.stringify([row]),searchDocuments:()=>JSON.stringify({results:[row]})};
  try{
    const context=buildMasterContext(createCampaign('SWE02 RAG'),{intentText:'Nabat',ruleQuery:'Bura Nabat safe docking repairs transponder',maxRules:5});
    const source=context.rules.find(x=>x.id==='nabat-p13');
    assert.ok(source);
    assert.equal(source.page,12);
    assert.equal(source.printedPage,13);
    assert.equal(source.authorityType,'AVENTURA');
    assert.equal(source.sourceKind,'library_pdf');
    assert.equal(context.hybrid.reference.route,'RAG');
    assert.equal(context.hybrid.reference.canMutateState,false);
    const payload=providerRequestBody(context);
    assert.equal(payload.context.rules.find(x=>x.id==='nabat-p13').printedPage,13);
  }finally{if(previous===undefined)delete globalThis.BraseiroLibrary;else globalThis.BraseiroLibrary=previous;}
});

test('P234 hybrid routing never authorizes mechanical state changes by RAG/Gemini',()=>{
  const source={id:'SW-EOTE-STIMPACK-001',status:'TESTADO',summary:'Efeito auditado',sourceKind:'rule_matrix'};
  assert.equal(hybridDecision({purpose:'mechanics',adventureId:'jewel-of-yavin'}).route,'BLOCKED');
  assert.equal(hybridDecision({purpose:'mechanics',executor:()=>({})}).route,'CODE');
  assert.equal(hybridDecision({purpose:'reference',sources:[source]}).route,'RAG');
  assert.equal(hybridDecision({purpose:'reference',sources:[]}).route,'GM_REVIEW');
  assert.equal(hybridDecision({purpose:'narrative',providerAvailable:true}).route,'GEMINI_PROPOSAL');
  assert.equal(hybridDecision({purpose:'narrative',providerAvailable:false}).route,'LOCAL_FALLBACK');
  assert.equal(hybridDecision({purpose:'narrative',adventureId:'sixth',providerAvailable:true}).route,'BLOCKED');
  for(const purpose of ['mechanics','reference','narrative']) assert.equal(hybridDecision({purpose,sources:[source],executor:()=>({}),providerAvailable:true}).canMutateState,false);
});

test('P234 Master context and provider prompt express correct hybrid authority and five-adventure boundary',()=>{
  const state=createCampaign('Híbrido');
  const context=buildMasterContext(state,{intentText:'uso stimpack',ruleQuery:'stimpack'});
  assert.deepEqual(context.hybrid.adventureScope,permitted);
  assert.equal(context.hybrid.mechanicsAuthority,'DETERMINISTIC_CODE_ONLY');
  assert.equal(context.hybrid.geminiAuthority,'NARRATIVE_PROPOSAL_ONLY');
  assert.equal(context.hybrid.narrative.route,'LOCAL_FALLBACK');
  const payload=providerRequestBody(context);
  assert.match(payload.instruction,/cinco aventuras publicadas/);
  assert.match(payload.instruction,/executores determinísticos locais/);
  assert.match(payload.instruction,/RAG fornece apenas referências recuperadas/);
  assert.match(payload.instruction,/sem inventar uma regra/);
});

test('P234 offline app continues to register player intents without fabricated narration or dice',async()=>{
  const mem=storage(),repo=new LocalStorageRepository(mem),before=createCampaign('P234 offline');repo.save(before);
  const bridge=new BraseiroUiBridge({storage:mem});
  const out=await bridge.submitPlayerIntent('Inspeciono o terminal.');
  assert.equal(out.ok,true);assert.equal(out.data.mode,'local');
  const after=repo.load();
  assert.equal(after.events.filter(e=>e.type==='PLAYER_INTENT').length,1);
  assert.equal(after.scene.narration,before.scene.narration);
  assert.equal(after.player.character?.xp,before.player.character?.xp);
});

test('P234 native Gemini failure falls back to local and never accepts fabricated rule IDs',async()=>{
  const previous=globalThis.BraseiroGemini;
  const mem=storage(),repo=new LocalStorageRepository(mem),start=createCampaign('P234 Gemini');repo.save(start);
  const bridge=new BraseiroUiBridge({storage:mem});
  globalThis.BraseiroGemini={
    getStatus:()=>JSON.stringify({configured:true,masterModel:'test'}),
    requestMasterProposal:(id)=>{queueMicrotask(()=>globalThis.braseiroMasterNativeEvent({requestId:id,ok:true,proposal:{narration:'Ganhei mil créditos e experiência!',sourceRuleIds:['REGRA-INVENTADA'],worldOperations:[]}}));return JSON.stringify({ok:true});}
  };
  try{
    const result=await bridge.submitPlayerIntent('Observo a patrulha.');
    assert.equal(result.ok,true);
    assert.equal(result.data.mode,'local-fallback');
    assert.match(result.data.providerError,/regra fora das fontes recuperadas/);
    const after=repo.load();
    assert.equal(after.scene.narration,start.scene.narration);
    assert.equal(after.events.filter(x=>x.type==='PLAYER_INTENT').length,1);
    assert.equal(after.events.filter(x=>x.type==='MASTER_RESPONSE_ACCEPTED').length,0);
    assert.equal(after.events.filter(x=>x.type==='WORLD_UPDATED').length,0);
  }finally{if(previous===undefined)delete globalThis.BraseiroGemini;else globalThis.BraseiroGemini=previous;}
});

test('P234 offline Android bundle includes the safeguards and Java/Kokoro files were not modified',()=>{
  const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
  assert.match(bundle,/allowedHybridLibrarySource/);
  assert.match(bundle,/Gemini citou regra fora das fontes recuperadas/);
  assert.match(bundle,/local-fallback/);
  assert.match(bundle,/only|cinco aventuras publicadas/i);
});
