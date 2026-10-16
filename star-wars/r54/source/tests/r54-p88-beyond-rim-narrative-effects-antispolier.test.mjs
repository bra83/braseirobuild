import test from 'node:test';
import assert from 'node:assert/strict';
import {seedBeyondTheRimWorld,startBeyondTheRim,adjudicateBeyondTheRimCheck,beyondTheRimStatus} from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

function at(nodeId){let w=startBeyondTheRim(seedBeyondTheRimWorld({campaignMode:'EOTE'})).world;w.publishedAdventure.currentNodeId=nodeId;if(!w.publishedAdventure.visitedNodeIds.includes(nodeId))w.publishedAdventure.visitedNodeIds.push(nodeId);return w}

test('P88: Triumph/Despair de Além da Orla geram efeitos narrativos persistentes específicos',()=>{
 let w=at('dropoff-point');
 let out=adjudicateBeyondTheRimCheck(w,{checkIndex:0,result:{success:true,netAdvantage:2,triumph:1,despair:0}});
 assert.ok(out.adjudication.narrativeEffects.some(x=>x.kind==='triumph'));
 assert.ok(out.adjudication.narrativeEffects.some(x=>/Yiyar|disputa/i.test(x.summary)));
 assert.equal(out.world.publishedAdventure.flags.yiyarRivalryDefused,true);
 w=at('flying-junk');
 out=adjudicateBeyondTheRimCheck(w,{checkIndex:0,result:{success:false,netAdvantage:-2,triumph:0,despair:1}});
 assert.ok(out.adjudication.narrativeEffects.some(x=>x.kind==='despair'));
 assert.equal(out.world.publishedAdventure.flags.shipStress,true);
});

test('P88: feedback público leva os efeitos da rolagem para a UI',()=>{
 let w=at('cholganna-search');
 w=adjudicateBeyondTheRimCheck(w,{checkIndex:0,result:{success:true,netAdvantage:2,triumph:1}}).world;
 const status=beyondTheRimStatus(w); const fb=status.checkFeedback.at(-1);
 assert.ok(Array.isArray(fb.effects)); assert.ok(fb.effects.length>=2);
 assert.ok(fb.effects.every(x=>typeof x.summary==='string'&&x.summary.length>5));
});

test('P88: projeção pública esconde atenção ISB e rastreamento não detectado',()=>{
 let w=at('ship-security'); const p=w.publishedAdventure;
 p.flags.isbAttention=true; p.flags.imperialTrackerAttached=true; p.flags.departureTracked=true; p.flags.imperialTrackerDetected=false;
 const status=beyondTheRimStatus(w); const raw=JSON.stringify(status.progress.flags);
 assert.ok(!raw.includes('isbAttention')); assert.ok(!raw.includes('imperialTrackerAttached')); assert.ok(!raw.includes('departureTracked'));
});

test('P88: rastreador detectado pode ser mostrado sem revelar atenção interna do ISB',()=>{
 let w=at('ship-security'); const p=w.publishedAdventure;
 p.flags.isbAttention=true; p.flags.imperialTrackerDetected=true; p.flags.imperialTrackerAttached=true;
 const flags=beyondTheRimStatus(w).progress.flags;
 assert.equal(flags.imperialTrackerDetected,true); assert.equal(flags.imperialTrackerAttached,true); assert.equal('isbAttention' in flags,false);
});

test('P88: exposição alta sozinha não revela facção ISB nem epílogo imperial',()=>{
 let w=at('flying-junk'); const p=w.publishedAdventure;p.status='completed';p.systems.imperialExposure=6;p.flags.imperialTrackerDetected=false;
 w.factions['btr.fac.isb'].knownToPlayer=false;
 const status=beyondTheRimStatus(w);
 assert.ok(!status.epilogue.some(x=>x.id==='imperial-trail'));
 assert.equal(w.factions['btr.fac.isb'].knownToPlayer,false);
});
