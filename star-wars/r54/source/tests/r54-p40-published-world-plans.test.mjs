import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { startUnderBlackSun, advanceUnderBlackSun, recordUnderBlackSunEncounterOutcome } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
import { advanceLivingWorldDepth } from '../app/src/main/assets/vtt/js/world/livingWorldDepth.mjs';

const step=(w,id)=>advanceUnderBlackSun(w,id).world;
function finish({weapons=true,outcome='kaato-vanishes'}={}){
 let w=startUnderBlackSun(createWorld()).world;
 for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-warn-black-sun','go-zelcomm',weapons?'zelcomm-weapons':'zelcomm-clean','move-endgame','plan-direct','through-offices','enter-abandoned','face-kaato']) w=step(w,id);
 const neutralized=outcome==='kaato-captured'||outcome==='kaato-killed';
 w=recordUnderBlackSunEncounterOutcome(w,{encounterId:'kaato-showdown',outcome:neutralized?'adversaries_defeated':'ended',adversaries:[{worldNpcId:'uabs.npc.kaato',profileKey:'kaato_leeachos',status:neutralized?'incapacitated':'active'}]}).world;
 w=step(w,outcome);
 return w;
}
function advanceDays(w,days){w.clock={...(w.clock||{}),tick:(Number(w.clock?.tick)||0)+days*4};return advanceLivingWorldDepth(w,{ticks:days*4});}

test('P40: planos só existem após epílogo e avançam pelo relógio',()=>{
 let w=startUnderBlackSun(createWorld()).world;
 assert.equal(w.publishedAdventureWorldPlans,undefined);
 w=finish(); const plans=w.publishedAdventureWorldPlans['under-black-sun'];
 assert.equal(plans.find(p=>p.id==='zelcomm-retaliation').status,'latent');
 w=advanceDays(w,3); const z=w.publishedAdventureWorldPlans['under-black-sun'].find(p=>p.id==='zelcomm-retaliation');
 assert.ok(['active','resolved'].includes(z.status)); assert.ok(z.progress>0);
});

test('P40: Pykes resolvem consequência e alteram influência persistentemente',()=>{
 let w=finish({weapons:false,outcome:'kaato-captured'}); const beforeRel=w.livingDepth.factions['uabs.fac.pykes'].relations['player-group'];
 w=advanceDays(w,12); const p=w.publishedAdventureWorldPlans['under-black-sun'].find(p=>p.id==='pykes-aftermath');
 assert.equal(p.status,'resolved'); assert.equal(p.progress,100); assert.ok(w.livingDepth.factions['uabs.fac.pykes'].relations['player-group']>beforeRel);
 assert.ok(w.livingDepth.recentEvents.some(e=>e.planId==='pykes-aftermath'&&e.kind==='plan-resolved'));
});

test('P40: retaliação Zelcomm cria consequência jogável conhecida',()=>{
 let w=advanceDays(finish(),15); const z=w.publishedAdventureWorldPlans['under-black-sun'].find(p=>p.id==='zelcomm-retaliation');
 assert.equal(z.status,'resolved'); assert.ok(w.pendingWorldConsequences.some(c=>c.kind==='faction-pressure'&&c.factionId==='uabs.fac.zelcomm'&&c.playerKnown));
});

test('P40: retorno de Kaa’to resolve sem vazar ao jogador antes de entrar em cena',()=>{
 let w=advanceDays(finish(),18); const k=w.publishedAdventureWorldPlans['under-black-sun'].find(p=>p.id==='kaato-return');
 assert.equal(k.status,'resolved'); assert.equal(w.livingDepth.npcs['uabs.npc.kaato'].lifeState,'alive');
 const c=w.pendingWorldConsequences.find(c=>c.kind==='npc-return'); assert.ok(c); assert.equal(c.playerKnown,false);
 const evt=w.livingDepth.recentEvents.find(e=>e.planId==='kaato-return'&&e.kind==='plan-resolved'); assert.ok(evt); assert.equal(evt.publicSummary,null);
});

test('P40: Kaa’to morto continua sem plano de retorno',()=>{
 let w=finish({outcome:'kaato-killed'}); w=advanceDays(w,20);
 assert.equal(w.publishedAdventureWorldPlans['under-black-sun'].some(p=>p.id==='kaato-return'),false);
 assert.equal(w.livingDepth.npcs['uabs.npc.kaato'].lifeState,'dead');
});
