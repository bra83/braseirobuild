import test from 'node:test';
import assert from 'node:assert/strict';
import { BEYOND_THE_RIM, seedBeyondTheRimWorld, startBeyondTheRim, adjudicateBeyondTheRimCheck } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

test('P71 Beyond the Rim graph remains closed and fully localized',()=>{
 const ids=new Set(Object.keys(BEYOND_THE_RIM.nodes));
 assert.ok(ids.has(BEYOND_THE_RIM.startNodeId));
 for(const [id,node] of Object.entries(BEYOND_THE_RIM.nodes)){
   assert.ok(node.title && node.summary && node.narration, id);
   for(const c of node.choices||[]) assert.ok(ids.has(c.to), `${id} -> ${c.to}`);
 }
});

test('P71 episode 2/3 check feedback is scene-specific and anti-spoiler',()=>{
 let world=startBeyondTheRim(seedBeyondTheRimWorld({campaignMode:'EOTE'})).world;
 const samples=[
  ['journey-beyond-rim',false,'trilha operacional'],['cholganna-search',true,'rastro'],['harsol-contact',true,'confiança'],
  ['raxus-arrival',false,'exposição'],['dropoff-point',false,'vulnerável'],['flying-junk',true,'fuga final']
 ];
 for(const [nodeId,success,needle] of samples){
   world.publishedAdventure.currentNodeId=nodeId;
   const out=adjudicateBeyondTheRimCheck(world,{checkIndex:0,result:{success,netAdvantage:0,triumph:0,despair:0}});
   assert.match(out.adjudication.summary,new RegExp(needle,'i'),nodeId);
   assert.doesNotMatch(out.adjudication.summary,/próxim[oa]|depois vai|futuro nó|spoiler/i,nodeId);
   world=out.world;
 }
});

test('P71 adjudication keeps EotE framing and does not create Duty or overwrite Obligation',()=>{
 let world=startBeyondTheRim(seedBeyondTheRimWorld({campaignMode:'EOTE',party:{obligationTotal:20}})).world;
 world.publishedAdventure.currentNodeId='raxus-arrival';
 const before=structuredClone(world.party);
 const out=adjudicateBeyondTheRimCheck(world,{checkIndex:0,result:{success:true}}).world;
 assert.equal(out.party?.obligationTotal,before?.obligationTotal);
 assert.equal(out.party?.duty,before?.duty);
});
