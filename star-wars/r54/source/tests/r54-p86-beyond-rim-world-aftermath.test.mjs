import test from 'node:test';
import assert from 'node:assert/strict';
import { startBeyondTheRim, advanceBeyondTheRim, beyondTheRimStatus } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';
import { advanceLivingWorldDepth } from '../app/src/main/assets/vtt/js/world/livingWorldDepth.mjs';

function completedWorld({exposure=4,yiyar=true}={}){
  let {world}=startBeyondTheRim({clock:{tick:40}});
  const p=world.publishedAdventure;
  p.currentNodeId='flying-junk';
  if(!p.visitedNodeIds.includes('flying-junk'))p.visitedNodeIds.push('flying-junk');
  p.flags.finalEscapeSucceeded=true;
  p.flags.yiyarEscalated=yiyar;
  p.systems.imperialExposure=exposure;
  return advanceBeyondTheRim(world,'complete-beyond-rim').world;
}

test('P86: planos pós-epílogo de Além da Orla nascem no dia real da conclusão',()=>{
  const w=completedWorld();
  const plans=w.publishedAdventureWorldPlans['beyond-the-rim'];
  assert.deepEqual(plans.map(x=>x.id),['isotech-aftermath','yiyar-retaliation','isb-followup']);
  assert.ok(plans.every(x=>x.createdDay===10));
  assert.equal(plans.find(x=>x.id==='isb-followup').visibility,'secret-until-resolved');
});

test('P86: ativação do acompanhamento imperial não vaza aviso público',()=>{
  let w=completedWorld();
  w.clock.tick=56;
  w=advanceLivingWorldDepth(w,{ticks:16});
  const plan=w.publishedAdventureWorldPlans['beyond-the-rim'].find(x=>x.id==='isb-followup');
  assert.equal(plan.status,'active');
  const event=(w.livingDepth.recentEvents||[]).find(e=>e.planId==='isb-followup'&&e.kind==='plan-activated');
  assert.equal(event?.publicSummary??null,null);
});

test('P86: IsoTech, Yiyar e ISB resolvem consequências persistentes distintas',()=>{
  let w=completedWorld();
  for(let day=11;day<=40;day++){w.clock.tick=day*4;w=advanceLivingWorldDepth(w,{ticks:4});}
  const plans=w.publishedAdventureWorldPlans['beyond-the-rim'];
  assert.ok(plans.every(x=>x.status==='resolved'));
  const kinds=(w.pendingWorldConsequences||[]).filter(x=>x.adventureId==='beyond-the-rim').map(x=>x.kind);
  assert.ok(kinds.includes('allied-faction-growth'));
  assert.ok(kinds.includes('rival-retaliation'));
  assert.ok(kinds.includes('imperial-surveillance'));
});

test('P86: status público mostra somente aftermath conhecido pelo jogador',()=>{
  let w=completedWorld();
  w.pendingWorldConsequences=[
    {id:'known',adventureId:'beyond-the-rim',kind:'rival-retaliation',status:'pending',playerKnown:true,createdDay:12,summary:'O Clã Yiyar voltou a agir contra interesses ligados à tripulação.'},
    {id:'hidden',adventureId:'beyond-the-rim',kind:'imperial-surveillance',status:'pending',playerKnown:false,createdDay:12,summary:'SEGREDO IMPERIAL'}
  ];
  const status=beyondTheRimStatus(w);
  assert.ok(status.knownConsequences.some(x=>x.id==='known'));
  assert.ok(!JSON.stringify(status).includes('SEGREDO IMPERIAL'));
});
