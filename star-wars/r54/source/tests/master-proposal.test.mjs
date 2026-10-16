import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { validateMasterProposal, applyValidatedNarrativeProposal } from '../app/src/main/assets/vtt/js/master/proposal.mjs';

test('proposta do Mestre aceita apenas narração, pergunta e teste estruturado',()=>{
  const p=validateMasterProposal({
    narration:'As portas do hangar se fecham atrás de você.',
    sceneTitle:'Hangar imperial',
    suggestedCheck:{skillKey:'sigilo',difficulty:2,challenge:0,boost:1,setback:0,reason:'Evitar a patrulha'},
    sourceRuleIds:['SW-AOR-POOL-001']
  },{knownRuleIds:['SW-AOR-POOL-001']});
  assert.equal(p.suggestedCheck.skillKey,'sigilo');
  assert.equal(p.sourceRuleIds[0],'SW-AOR-POOL-001');
});

test('proposta rejeita mutação direta de ficha ou regra não recuperada',()=>{
  assert.throws(()=>validateMasterProposal({narration:'x',player:{credits:9999}}),/não autorizado/i);
  assert.throws(()=>validateMasterProposal({narration:'x',sourceRuleIds:['INVENTADA']},{knownRuleIds:['SW-AOR-POOL-001']}),/não estava no contexto/i);
});

test('aplicar proposta narrativa não altera ficha nem combate',()=>{
  const s=createCampaign('Seguro');
  s.player.character={sentinel:'preservar'};
  const beforeCombat=structuredClone(s.combat);
  const p=validateMasterProposal({narration:'Nova narração',askPlayer:'O que você faz?'});
  const next=applyValidatedNarrativeProposal(s,p);
  assert.equal(next.scene.narration,'Nova narração');
  assert.deepEqual(next.player.character,{sentinel:'preservar'});
  assert.deepEqual(next.combat,beforeCombat);
  assert.equal(next.master.pendingQuestion,'O que você faz?');
});
