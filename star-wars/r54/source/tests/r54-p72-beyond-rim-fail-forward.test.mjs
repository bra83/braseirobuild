import test from 'node:test';
import assert from 'node:assert/strict';
import { startBeyondTheRim, adjudicateBeyondTheRimCheck, beyondTheRimStatus, advanceBeyondTheRim } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

const cases=[
 ['cholganna-search','follow-debris-costly','crash-trail'],
 ['crash-trail','push-through-trail','sa-nalaor-wreck'],
 ['imperial-contingency','evacuate-under-pressure','episode2-boundary'],
 ['raxus-arrival','reach-scrapheap-exposed','scrapheap-point'],
 ['dropoff-point','abandon-dropoff','imperial-interruption'],
 ['imperial-interruption','breakout-costly','flying-junk'],
 ['flying-junk','complete-beyond-rim-costly','adventure-complete']
];
for(const [nodeId,choiceId,target] of cases)test(`P72 fail-forward ${nodeId}`,()=>{
 let {world}=startBeyondTheRim({campaignMode:'EOTE',party:{obligationTotal:25}});
 world.publishedAdventure.currentNodeId=nodeId; if(!world.publishedAdventure.visitedNodeIds.includes(nodeId)) world.publishedAdventure.visitedNodeIds.push(nodeId);
 const before=structuredClone(world.party);
 world=adjudicateBeyondTheRimCheck(world,{checkIndex:0,result:{success:false,netAdvantage:-1}}).world;
 const status=beyondTheRimStatus(world);
 assert.ok(status.node.choices.some(c=>c.id===choiceId),`missing ${choiceId}`);
 const out=advanceBeyondTheRim(world,choiceId);
 assert.equal(out.progress.currentNodeId,target);
 assert.equal(out.world.party?.obligationTotal,before?.obligationTotal);
 assert.equal(out.world.party?.duty,before?.duty);
});

test('P72 costly completion persists consequence without exposing future nodes',()=>{
 let {world}=startBeyondTheRim({campaignMode:'EOTE'}); world.publishedAdventure.currentNodeId='flying-junk'; world.publishedAdventure.visitedNodeIds.push('flying-junk');
 world=adjudicateBeyondTheRimCheck(world,{checkIndex:0,result:{success:false,despair:1}}).world;
 const out=advanceBeyondTheRim(world,'complete-beyond-rim-costly');
 assert.equal(out.completed,true); assert.equal(out.world.publishedAdventure.flags.finalEscapeCostly,true);
 assert.equal(out.world.publishedAdventureConsequences['beyond-the-rim'].completed,true);
});
