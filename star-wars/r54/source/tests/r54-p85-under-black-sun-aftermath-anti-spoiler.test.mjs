import test from 'node:test';
import assert from 'node:assert/strict';
import { seedUnderBlackSunWorld, startUnderBlackSun, advanceUnderBlackSun } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
import { advanceLivingWorldDepth } from '../app/src/main/assets/vtt/js/world/livingWorldDepth.mjs';

function epilogueWorld({kaatoRecurring=false,weaponsTaken=false}={}){
 let w=seedUnderBlackSunWorld({clock:{tick:40}}); w=startUnderBlackSun(w).world;
 const p=w.publishedAdventure; p.flags.kaatoRecurring=kaatoRecurring; p.flags.weaponsTaken=weaponsTaken; p.flags.kaatoOutcome=kaatoRecurring?'uncertain':'captured';
 p.currentNodeId='showdown'; p.visitedNodeIds.push('showdown'); p.systems.encounters.history.push({encounterId:'kaato-showdown',outcome:'resolved'});
 if(kaatoRecurring){p.flags.kaatoNeutralized=false; return advanceUnderBlackSun(w,'kaato-vanishes').world;}
 p.flags.kaatoNeutralized=true; return advanceUnderBlackSun(w,'kaato-captured').world;
}

test('P85: planos pós-epílogo recebem createdDay estável',()=>{
 const w=epilogueWorld({kaatoRecurring:true,weaponsTaken:true});
 for(const plan of w.publishedAdventureWorldPlans['under-black-sun']) assert.equal(plan.createdDay,10);
});

test('P85: retorno latente de Kaa’to não vaza evento público ao ativar',()=>{
 let w=epilogueWorld({kaatoRecurring:true});
 w.clock.tick=52; w=advanceLivingWorldDepth(w,{ticks:12});
 const plan=w.publishedAdventureWorldPlans['under-black-sun'].find(x=>x.id==='kaato-return');
 assert.equal(plan.status,'active');
 const leak=(w.livingDepth.recentEvents||[]).find(e=>e.planId==='kaato-return'&&e.kind==='plan-activated');
 assert.equal(leak?.publicSummary??null,null);
});

test('P85: Zelcomm continua podendo produzir consequência conhecida depois do epílogo',()=>{
 let w=epilogueWorld({weaponsTaken:true});
 for(let day=11;day<=30;day++){w.clock.tick=day*4;w=advanceLivingWorldDepth(w,{ticks:4});}
 const plan=w.publishedAdventureWorldPlans['under-black-sun'].find(x=>x.id==='zelcomm-retaliation');
 assert.equal(plan.status,'resolved');
 assert.ok((w.pendingWorldConsequences||[]).some(c=>c.kind==='faction-pressure'&&c.playerKnown===true));
});

test('P85: retorno de Kaa’to só vira consequência pendente oculta quando realmente resolve',()=>{
 let w=epilogueWorld({kaatoRecurring:true});
 for(let day=11;day<=35;day++){w.clock.tick=day*4;w=advanceLivingWorldDepth(w,{ticks:4});}
 const plan=w.publishedAdventureWorldPlans['under-black-sun'].find(x=>x.id==='kaato-return');
 assert.equal(plan.status,'resolved');
 const c=(w.pendingWorldConsequences||[]).find(x=>x.kind==='npc-return');
 assert.ok(c); assert.equal(c.playerKnown,false);
});
