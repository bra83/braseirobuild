import test from 'node:test';
import assert from 'node:assert/strict';
import { requestHttpMasterProposal, validateProviderEndpoint } from '../app/src/main/assets/vtt/js/master/provider.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('endpoint remoto exige HTTPS, exceto localhost de desenvolvimento',()=>{
  assert.match(validateProviderEndpoint('https://mestre.example/api'),/^https:/);
  assert.match(validateProviderEndpoint('http://127.0.0.1:8080/master'),/^http:/);
  assert.throws(()=>validateProviderEndpoint('http://example.com/master'),/HTTPS/i);
});

test('adaptador HTTP envia contexto e recebe somente proposta estruturada',async()=>{
  const context=buildMasterContext(createCampaign('HTTP'),{ruleQuery:'segunda manobra'});
  let body;
  const fetchImpl=async (_url,opts)=>{ body=JSON.parse(opts.body); return {ok:true,status:200,json:async()=>({proposal:{narration:'A patrulha se aproxima.',sourceRuleIds:[]}})}; };
  const result=await requestHttpMasterProposal({endpoint:'https://mestre.example/api',context,fetchImpl,timeoutMs:100});
  assert.equal(body.protocol,'braseiro.master.v1');
  assert.equal(result.proposal.narration,'A patrulha se aproxima.');
});

test('falha de rede do Mestre não altera o estado válido',async()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'provider-fail'));
  game.loadStarterCharacter('cael');
  const before=JSON.stringify(game.state);
  const fetchImpl=async()=>{ throw new Error('offline'); };
  await assert.rejects(()=>game.requestMasterTurn({intentText:'Avanço pelo corredor',endpoint:'https://mestre.example/api',fetchImpl}),/offline/);
  assert.equal(JSON.stringify(game.state),before);
});

test('resposta remota validada altera só a camada narrativa autorizada',async()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'provider-ok'));
  game.loadStarterCharacter('cael');
  const characterBefore=structuredClone(game.state.player.character);
  const fetchImpl=async()=>({ok:true,status:200,json:async()=>({proposal:{sceneTitle:'Corredor de serviço',narration:'Luzes vermelhas piscam no corredor.',askPlayer:'Você avança ou se esconde?',sourceRuleIds:[]}})});
  const result=await game.requestMasterTurn({intentText:'Olho o corredor',endpoint:'https://mestre.example/api',fetchImpl});
  assert.equal(result.proposal.sceneTitle,'Corredor de serviço');
  assert.equal(game.state.scene.narration,'Luzes vermelhas piscam no corredor.');
  assert.deepEqual(game.state.player.character,characterBefore);
});
