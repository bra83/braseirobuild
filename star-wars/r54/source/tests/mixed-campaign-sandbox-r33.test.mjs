import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { seedLivingRegion, sandboxOpening, sandboxSummary, advanceLivingWorld } from '../app/src/main/assets/vtt/js/world/sandbox.mjs';
import { dutyCheck } from '../app/src/main/assets/vtt/js/rules/duty.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function game(key='r33'){ return new GameController(new LocalStorageRepository(new MemoryStorage(),key)); }

const aorHuman={
  name:'Nira Valen',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
  careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],
  specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],
  motivation:'Libertar mundos ocupados pelo Império.',background:'Pilota civil recrutada pela Aliança.',
  dutyType:'Superioridade espacial'
};

test('modo padrão continua EOTE e bloqueia Duty legado sem regredir guard',()=>{
  const g=game('mode-default'); assert.equal(g.campaignModeSummary().campaignMode,'EOTE');
  assert.throws(()=>g.addGroupDuty({value:10,member:'X'}),/legado Age of Rebellion/i);
});

test('modo MIXED mantém Obligation e Duty simultaneamente',()=>{
  const g=game('mixed'); g.loadStarterCharacter('zal'); const name=g.state.player.character.name;
  g.setCampaignMode('MIXED'); g.addGroupObligation({value:40,member:name,type:'Dívida'}); g.addGroupDuty({value:40,member:name,type:'Apoio'});
  const baseW=g.state.player.character.derived.wounds.threshold, baseS=g.state.player.character.derived.strain.threshold;
  const r=g.startSession({playerCount:1,obligationRoll:1,dutyRoll:1,rng:()=>0.99});
  assert.equal(r.obligation.triggered,true); assert.equal(r.duty.triggered,true);
  const h=g.healthStatus(); assert.equal(h.effectiveWoundThreshold,baseW+2); assert.equal(h.effectiveStrainThreshold,baseS-2);
  g.closeSession(); const closed=g.healthStatus(); assert.equal(closed.effectiveWoundThreshold,baseW); assert.equal(closed.effectiveStrainThreshold,baseS);
});

test('Duty d100 usa faixa cumulativa e doubles dobram bônus de sessão',()=>{
  const c={name:'Nira',derived:{wounds:{threshold:12}}}; const group={dutyEntries:[{id:'a',value:50,member:'Nira',type:'Apoio'}]};
  const r=dutyCheck(group,c,{roll:22}); assert.equal(r.triggered,true); assert.equal(r.doubles,true); assert.equal(r.groupWoundThresholdBonus,2); assert.equal(r.characterWoundThresholdBonus,4);
});

test('criação AoR fica disponível em AOR/MIXED e Duty entra no grupo',()=>{
  const g=game('aor-create'); g.setCampaignMode('AOR'); const c=g.createAorCharacter(aorHuman);
  assert.equal(c.source.type,'custom_aor_core'); assert.equal(g.groupSummary().dutyTotal,c.duties[0].value); assert.equal(g.groupSummary().obligationTotal,0);
});

test('Contribution Rank sobe somente com Duty total >=100 e zera Duty corrente',()=>{
  const g=game('contribution'); g.setCampaignMode('AOR'); g.loadStarterCharacter('zal');
  const entry=g.addGroupDuty({value:90,member:g.state.player.character.name,type:'Apoio'}).dutyEntries[0];
  assert.equal(g.groupSummary().contributionMilestoneReady,false); g.addDutyProgress(entry.id,10); assert.equal(g.groupSummary().contributionMilestoneReady,true);
  const r=g.resolveContributionMilestone(); assert.equal(r.contributionRank,1); assert.equal(g.groupSummary().dutyTotal,0);
});

test('seed de mundo vivo cria 6 facções, 36 NPCs, 12 locais, clocks e oportunidades sem roteiro obrigatório',()=>{
  const r=seedLivingRegion(createWorld(),{startingPoint:'Mos Eisley',regionLabel:'Tatooine e rotas próximas'});
  assert.deepEqual(r.counts,{factions:6,npcs:36,locations:12,opportunities:12,clocks:6});
  const s=sandboxSummary(r.world); assert.equal(s.marketCount,12); assert.equal(s.routeCount,22); assert.equal(s.activeClockCount,6);
  const opening=sandboxOpening(r.world); assert.equal(opening.ready,true); assert.equal(opening.location.name,'Mos Eisley'); assert.match(opening.principle,/não force missão/i);
});

test('seed é idempotente e não duplica o mundo ao reabrir campanha',()=>{
  const first=seedLivingRegion(createWorld(),{startingPoint:'Ord Mantell'}); const second=seedLivingRegion(first.world,{startingPoint:'Outro lugar'});
  assert.equal(second.created,false); assert.equal(Object.keys(second.world.factions).length,6); assert.equal(Object.keys(second.world.npcs).length,36); assert.equal(Object.keys(second.world.locations).length,12);
});

test('tempo fora da cena move planos, clocks e transforma consequências em oportunidades',()=>{
  const seeded=seedLivingRegion(createWorld(),{startingPoint:'Porto Livre'}); const r=advanceLivingWorld(seeded.world,{ticks:10,maxEvents:50});
  assert.equal(r.world.clock.tick,10); assert.ok(r.processed.length>0); assert.ok(r.clockAdvances.length>0); assert.ok(r.opportunitiesCreated.length>0);
});

test('controller inicializa sandbox, persiste local e viagem avança o mundo',()=>{
  const storage=new MemoryStorage(); const g=new GameController(new LocalStorageRepository(storage,'travel'));
  const init=g.initializeLivingSandbox({startingPoint:'Porto Cego',regionLabel:'Setor de teste'}); assert.equal(init.created,true); assert.equal(g.state.scene.locationId,init.startLocationId);
  const startTick=g.state.world.clock.tick; const target=Object.keys(g.state.world.locations).find(id=>id!==init.startLocationId && g.state.world.routes.some(r=>r.fromLocationId===init.startLocationId&&r.toLocationId===id));
  const t=g.travelToLocation(target); assert.ok(t.route.ticks>=1); assert.equal(g.state.scene.locationId,target); assert.ok(g.state.world.clock.tick>startTick);
  const reopened=new GameController(new LocalStorageRepository(storage,'travel')); assert.equal(reopened.state.scene.locationId,target); assert.equal(reopened.livingSandboxStatus().sandbox.initialized,true);
});

test('contexto do Mestre inclui modo, mundo vivo e abertura estruturada separada da narração',()=>{
  const g=game('context'); g.setCampaignMode('MIXED'); g.initializeLivingSandbox({startingPoint:'Doca 7'}); const ctx=buildMasterContext(g.state,{intentText:'Olho ao redor.'});
  assert.equal(ctx.contextVersion,8); assert.equal(ctx.campaignMode,'MIXED'); assert.equal(ctx.opening.ready,true); assert.equal(ctx.opening.location.name,'Doca 7'); assert.equal(ctx.sandbox.activeWorldClocks,undefined); assert.equal(ctx.sandbox.activeClockCount,6);
});

test('Mestre pode operar oportunidades/clocks/mercado apenas por operações estruturadas',()=>{
  const g=game('master-ops'); g.initializeLivingSandbox({startingPoint:'Doca Livre'}); const opening=g.sandboxOpening(); const loc=opening.location; const clock=opening.clocks[0] ?? Object.values(g.state.world.clocks)[0];
  g.prepareMasterContext('Há trabalho por aqui?');
  const beforeOpp=g.state.world.opportunities.length;
  g.applyMasterProposal({worldOperations:[
    {type:'create_opportunity',kind:'contract',title:'Carga desaparecida',summary:'Um carregamento sumiu.',locationId:loc.id,knownToPlayer:true,tags:['contrato']},
    {type:'advance_clock',clockId:clock.id,steps:1},
    {type:'set_market',locationId:loc.id,rarityModifier:1,restrictedAccess:'controlled',tags:['porto'],notes:'Inspeção intensificada.'}
  ]});
  assert.equal(g.state.world.opportunities.length,beforeOpp+1); assert.equal(g.state.world.clocks[clock.id].progress,clock.progress+1); assert.equal(g.currentMarketProfile(loc.id).rarityModifier,1);
});
