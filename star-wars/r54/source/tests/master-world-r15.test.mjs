import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createNpc, upsertNpc, recordNpcFact, scheduleWorldEvent } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';

test('contexto do Mestre recebe goal/relation/facts reais e agenda pendente do mundo',()=>{
  const s=createCampaign('Mundo'); const npc=createNpc({name:'Kara',goal:'Espionar o porto',relation:'desconfiada'}); s.world=upsertNpc(s.world,npc); s.world=recordNpcFact(s.world,npc.id,{text:'Tem um contato imperial',knownToPlayer:false}); s.world=scheduleWorldEvent(s.world,{type:'move_npc',dueTick:5,payload:{npcId:npc.id,locationId:'porto'}}).world;
  const c=buildMasterContext(s,{intentText:'Procuro Kara',ruleQuery:'interação com PNPC'}); const k=c.world.npcs.find(n=>n.id===npc.id);
  assert.equal(c.contextVersion,8); assert.equal(k.goal,'Espionar o porto'); assert.equal(k.relation,'desconfiada'); assert.equal(k.facts[0].text,'Tem um contato imperial'); assert.equal(c.world.pendingEvents.length,1);
});
