import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createPendingAcquisition, resolvePendingAcquisition, genericEconomyCatalog } from '../app/src/main/assets/vtt/js/rules/economy.mjs';

class MemoryRepository {
  constructor(state=createCampaign('P93')){this.state=structuredClone(state)}
  load(){return structuredClone(this.state)}
  save(s){this.state=structuredClone(s)}
}
function game(){
  const repo=new MemoryRepository(),g=new GameController(repo);
  g.loadStarterCharacter('cael');
  return {g,repo};
}
function seq(values, fallback=0.2){let i=0;return ()=>i<values.length?values[i++]:fallback}

test('P93: resolução de Astronavegação usa símbolos reais do rollPool e salto bem-sucedido avança tempo/local',()=>{
  const {g,repo}=game();
  g.initializeLivingSandbox({startingPoint:'Origem P93',regionLabel:'Setor P93'});
  g.addVehicle('yt1300');
  const from=g.state.scene.locationId;
  const dest=Object.keys(g.state.world.locations).find(id=>id!==from);
  const tick0=g.state.world.clock.tick;
  const r=g.resolveHyperspaceJump({routeType:'MAJOR',destinationLocationId:dest,rng:()=>0.2});
  assert.equal(r.outcome.jumpSuccessful,true);
  assert.ok(r.timeAdvancedTicks>=1);
  assert.equal(g.state.world.clock.tick,tick0+r.timeAdvancedTicks);
  assert.equal(g.state.scene.locationId,dest);
  assert.equal(g.state.vehicles.travel.lastHyperspaceJump.arrived,true);
  const reopened=new GameController(repo);
  assert.equal(reopened.state.scene.locationId,dest);
  assert.equal(reopened.state.world.clock.tick,tick0+r.timeAdvancedTicks);
});

test('P93: desvio hiperespacial avança o tempo mas não teleporta ao destino pretendido',()=>{
  const {g}=game();
  g.initializeLivingSandbox({startingPoint:'Origem P93',regionLabel:'Setor P93'});
  g.addVehicle('yt1300');
  const from=g.state.scene.locationId;
  const dest=Object.keys(g.state.world.locations).find(id=>id!==from);
  const tick0=g.state.world.clock.tick;
  // 2 ability = sucesso duplo; challenge = despair; mishap 30 => off_course.
  const r=g.resolveHyperspaceJump({routeType:'MAJOR',rushedJump:true,destinationLocationId:dest,mishapRoll:30,rng:seq([0.4,0.4,0.99])});
  assert.equal(r.outcome.jumpSuccessful,true);
  assert.equal(r.outcome.offCourse,true);
  assert.ok(g.state.world.clock.tick>tick0);
  assert.equal(g.state.scene.locationId,from);
  assert.equal(g.state.vehicles.travel.lastHyperspaceJump.arrived,false);
});

test('P93: falha de Astronavegação não avança relógio nem posição',()=>{
  const {g}=game();
  g.initializeLivingSandbox({startingPoint:'Origem P93',regionLabel:'Setor P93'});
  g.addVehicle('yt1300');
  const from=g.state.scene.locationId;
  const dest=Object.keys(g.state.world.locations).find(id=>id!==from);
  const tick0=g.state.world.clock.tick;
  const r=g.resolveHyperspaceJump({routeType:'MAJOR',destinationLocationId:dest,rng:()=>0});
  assert.equal(r.outcome.jumpSuccessful,false);
  assert.equal(r.timeAdvancedTicks,0);
  assert.equal(g.state.world.clock.tick,tick0);
  assert.equal(g.state.scene.locationId,from);
});

test('P93: catálogo econômico inclui cibernéticos mas aquisição não instala efeitos automaticamente',()=>{
  const cat=genericEconomyCatalog();
  assert.ok(cat.cybernetics.some(x=>x.id==='cybernetic_subdermal_armor'));
  const {g,repo}=game();
  g.state.player.character.credits=10000; repo.save(g.state);
  const pending=createPendingAcquisition('cybernetic','cybernetic_subdermal_armor',g.state.player.character,{requestId:'cy_p93'});
  const resolved=resolvePendingAcquisition(g.state,pending,{approved:true,finalPrice:3000,sourceRuleIds:['EOTE_CORE_CYBERNETICS']});
  repo.save(resolved.state);
  const reopened=new GameController(repo);
  assert.equal(reopened.cyberneticsSummary().installed.length,0);
  assert.equal(reopened.cyberneticsSummary().owned.length,1);
  assert.equal(reopened.state.player.character.credits,7000);
});

test('P93: somente cibernético adquirido pode ser instalado e remoção preserva propriedade',()=>{
  const {g,repo}=game();
  g.state.player.character.credits=10000; repo.save(g.state);
  assert.throws(()=>g.installOwnedCybernetic('asset_missing'),/adquirido não encontrado/i);
  const pending=createPendingAcquisition('cybernetic','cybernetic_subdermal_armor',g.state.player.character,{requestId:'cy_p93_owned'});
  const resolved=resolvePendingAcquisition(g.state,pending,{approved:true,finalPrice:3000,sourceRuleIds:['EOTE_CORE_CYBERNETICS']});
  repo.save(resolved.state);
  const h=new GameController(repo);
  const asset=h.cyberneticsSummary().owned[0];
  const soak0=Number(h.state.player.character.derived.soak||0);
  h.installOwnedCybernetic(asset.assetId);
  assert.equal(h.cyberneticsSummary().installed.length,1);
  assert.equal(h.state.player.character.derived.soak,soak0+1);
  assert.equal(h.cyberneticsSummary().owned[0].installed,true);
  h.removeOwnedCybernetic(asset.assetId);
  assert.equal(h.cyberneticsSummary().installed.length,0);
  assert.equal(h.cyberneticsSummary().owned.length,1);
  assert.equal(h.cyberneticsSummary().owned[0].installed,false);
  assert.equal(h.state.player.character.derived.soak,soak0);
});

test('P93: UI final expõe progressão de talentos, recuperação, hiperespaço real e cibernéticos sem consultRules',()=>{
  const runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');
  const start=runtime.indexOf('R54 P93 — P0 WAVE 2');
  assert.ok(start>0);
  const p93=runtime.slice(start);
  for(const marker of ['HIPERESPAÇO → TEMPO → LOCAL','TALENTOS / ESPECIALIZAÇÕES POSSUÍDAS','RECUPERAÇÃO / CONDIÇÕES','CIBERNÉTICOS']) assert.ok(p93.includes(marker),marker);
  for(const method of ['talentProgressionStatus','buyTalent','recoveryRuntimeStatus','usePlayerStimpack','useMedicine','restFullNight','proposeCyberneticAcquisition','installOwnedCybernetic','removeOwnedCybernetic','resolveHyperspaceJump']) assert.ok(p93.includes(method),method);
  assert.equal(/consultRules|MASTER_RULE_QUERY|rag\//.test(p93),false);
});


test('P93: recuperação pela UI usa regras reais de Stimpack, Medicina e descanso',()=>{
  const {g,repo}=game();
  g.state.player.character.derived.wounds.current=6;
  g.state.player.character.derived.strain.current=4;
  repo.save(g.state);
  const stim=g.usePlayerStimpack();
  assert.ok(stim.healed>0);
  assert.equal(g.state.player.character.gear.find(x=>/estimulantes/i.test(x.name)).quantity,4);
  g.state.player.character.derived.wounds.current=5; repo.save(g.state);
  const med=g.useMedicine({medicalSupplies:true,rng:seq([0.4,0.4,0,0,0])});
  assert.equal(med.check.success,true);
  assert.ok(med.healed>=1);
  g.state.player.character.derived.strain.current=5; repo.save(g.state);
  const rest=g.restFullNight();
  assert.equal(g.state.player.character.derived.strain.current,0);
  assert.ok(rest.woundsRecovered>=0);
});

test('P93: contratos TypeScript incluem apenas as operações novas seguras',()=>{
  for(const file of ['gameBridgeClient.ts','shell-r46-run87/gameBridgeClient.ts']){
    const text=fs.readFileSync(file,'utf8');
    for(const method of ['talentProgressionStatus','recoveryRuntimeStatus','usePlayerStimpack','useMedicine','restFullNight','proposeCyberneticAcquisition','installOwnedCybernetic','removeOwnedCybernetic']) assert.ok(text.includes(method),`${file}:${method}`);
  }
});

test('P93: bridge não oferece instalação cibernética bruta sem ativo adquirido',()=>{
  const bridge=fs.readFileSync('app/src/main/assets/vtt/js/core/uiBridge.mjs','utf8');
  assert.equal(/\n\s*installCybernetic\(itemId\)\s*\{/.test(bridge),false);
  assert.ok(bridge.includes('installOwnedCybernetic(assetId)'));
  assert.ok(bridge.includes('proposeCyberneticAcquisition(itemId)'));
});
