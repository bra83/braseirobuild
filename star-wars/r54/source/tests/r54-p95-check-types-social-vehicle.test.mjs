import test from 'node:test';
import assert from 'node:assert/strict';
import { buildOpposedNegativePool, buildAssistedCheckPool, compareCompetitiveResults } from '../app/src/main/assets/vtt/js/rules/narrativeDice.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('P95 opposed: maior valor define dados negativos e menor faz upgrades',()=>{
  const p=buildOpposedNegativePool(4,2);
  assert.equal(p.difficulty,2);
  assert.equal(p.challenge,2);
  assert.deepEqual(p.opposition,{characteristic:4,skill:2,total:4,upgrades:2});
});

test('P95 assistência hábil combina melhor característica e melhor perícia',()=>{
  const p=buildAssistedCheckPool({
    primaryActor:{characteristics:{intelecto:4},skills:{mecanica:1}},
    assistantActor:{characteristics:{intelecto:2},skills:{mecanica:3}},
    skillKey:'mecanica'
  });
  assert.equal(p.isSkilled,true);
  assert.equal(p.characteristicUsed,4);
  assert.equal(p.skillUsed,3);
  assert.equal(p.proficiency,3);
  assert.equal(p.ability,1);
  assert.equal(p.boost,0);
});

test('P95 assistência não-hábil adiciona exatamente um Boost',()=>{
  const p=buildAssistedCheckPool({
    primaryActor:{characteristics:{intelecto:4},skills:{mecanica:2}},
    assistantActor:{characteristics:{intelecto:2},skills:{mecanica:1}},
    skillKey:'mecanica'
  });
  assert.equal(p.isSkilled,false);
  assert.equal(p.boost,1);
  assert.equal(p.proficiency,2);
  assert.equal(p.ability,2);
});

test('P95 competitivo desempata por Triumph e depois Advantage',()=>{
  let r=compareCompetitiveResults([
    {id:'a',label:'A',check:{success:true,netSuccess:2,triumph:1,netAdvantage:0}},
    {id:'b',label:'B',check:{success:true,netSuccess:2,triumph:0,netAdvantage:5}}
  ]);
  assert.equal(r.winnerId,'a');
  r=compareCompetitiveResults([
    {id:'a',label:'A',check:{success:true,netSuccess:2,triumph:1,netAdvantage:1}},
    {id:'b',label:'B',check:{success:true,netSuccess:2,triumph:1,netAdvantage:3}}
  ]);
  assert.equal(r.winnerId,'b');
});

test('P95 competitivo sem sucesso é empate e empate completo continua empate',()=>{
  let r=compareCompetitiveResults([
    {id:'a',label:'A',check:{success:false,netSuccess:0,triumph:0,netAdvantage:2}},
    {id:'b',label:'B',check:{success:false,netSuccess:-1,triumph:1,netAdvantage:5}}
  ]);
  assert.equal(r.draw,true); assert.equal(r.reason,'NO_SUCCESS');
  r=compareCompetitiveResults([
    {id:'a',label:'A',check:{success:true,netSuccess:1,triumph:0,netAdvantage:2}},
    {id:'b',label:'B',check:{success:true,netSuccess:1,triumph:0,netAdvantage:2}}
  ]);
  assert.equal(r.draw,true); assert.equal(r.reason,'TIED_AFTER_TIEBREAKERS');
});

test('P95 social: Coerção é oposta por Disciplina e persiste em save/reopen',()=>{
  const storage=new MemoryStorage();
  let g=new GameController(new LocalStorageRepository(storage,'p95-social'));
  g.loadStarterCharacter('cael');
  const r=g.resolveSocialInfluenceCheck({skillKey:'coaccion',opponentCharacteristic:3,opponentSkill:2,rng:()=>0.2});
  assert.equal(r.type,'social');
  assert.equal(r.opposingSkill,'disciplina');
  assert.equal(r.opposition.total,3);
  assert.equal(r.opposition.upgrades,2);
  assert.equal(g.state.social.lastInfluence.actingSkill,'coaccion');
  assert.equal(g.state.social.history.length,1);
  g=new GameController(new LocalStorageRepository(storage,'p95-social'));
  assert.equal(g.state.social.lastInfluence.opposingSkill,'disciplina');
  assert.equal(g.state.social.history.length,1);
});

test('P95 social rejeita combinação que contorna oposição oficial',()=>{
  const g=new GameController(new LocalStorageRepository(new MemoryStorage(),'p95-invalid'));
  g.loadStarterCharacter('zal');
  assert.throws(()=>g.resolveSocialInfluenceCheck({skillKey:'carisma',opponentSkillKey:'disciplina',opponentCharacteristic:3,opponentSkill:2}),/oposição social inválida/i);
  assert.throws(()=>g.resolveSocialInfluenceCheck({skillKey:'percepcao',opponentCharacteristic:3,opponentSkill:2}),/não suportada/i);
});

test('P95 social: Negociação aceita Negociação ou Frialdade',()=>{
  const g=new GameController(new LocalStorageRepository(new MemoryStorage(),'p95-neg'));
  g.loadStarterCharacter('tendaar');
  const a=g.resolveSocialInfluenceCheck({skillKey:'negociacion',opponentSkillKey:'negociacion',opponentCharacteristic:3,opponentSkill:2,rng:()=>0.2});
  assert.equal(a.opposingSkill,'negociacion');
  const b=g.resolveSocialInfluenceCheck({skillKey:'negociacion',opponentSkillKey:'frialdad',opponentCharacteristic:3,opponentSkill:2,rng:()=>0.2});
  assert.equal(b.opposingSkill,'frialdad');
});

test('P95 checks avançados persistem tipo e resultado para reopen',()=>{
  const storage=new MemoryStorage();
  let g=new GameController(new LocalStorageRepository(storage,'p95-advanced'));
  g.loadStarterCharacter('tendaar');
  const a=g.resolveAssistedSkillCheck({skillKey:'mecanica',assistantCharacteristic:2,assistantSkill:3,difficulty:2,rng:()=>0.2});
  assert.equal(a.type,'assisted');
  assert.equal(g.state.advancedChecks.last.type,'assisted');
  const c=g.resolveCompetitiveSkillCheck({skillKey:'mecanica',difficulty:2,opponentCharacteristic:3,opponentSkill:1,rng:()=>0.2,opponentRng:()=>0.2});
  assert.equal(c.type,'competitive');
  assert.equal(g.state.advancedChecks.last.type,'competitive');
  g=new GameController(new LocalStorageRepository(storage,'p95-advanced'));
  assert.equal(g.state.advancedChecks.last.type,'competitive');
  assert.ok(g.state.advancedChecks.history.length>=2);
});

test('P95 bridge não expõe criação arbitrária de crítico veicular',async()=>{
  const fs=await import('node:fs/promises');
  const bridge=await fs.readFile(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  assert.doesNotMatch(bridge,/applyActiveVehicleCritical\(options/);
  assert.doesNotMatch(bridge,/repairActiveVehicleCritical\(index/);
  assert.match(bridge,/attemptRepairActiveVehicleCritical\(index/);
  assert.match(bridge,/vehicleDamageControl\(options/);
});

test('P95 reparo de crítico veicular exige teste de Mecânica e persiste somente no sucesso',()=>{
  const storage=new MemoryStorage();
  let g=new GameController(new LocalStorageRepository(storage,'p95-vehicle-repair'));
  g.loadStarterCharacter('tendaar');
  g.addVehicle('xwing_t65b');
  const critical=g.applyActiveVehicleCritical({roll:20});
  assert.equal(critical.hit.repaired,false);
  const attempt=g.attemptRepairActiveVehicleCritical(0,{rng:()=>0.2});
  assert.equal(attempt.difficulty,1);
  assert.equal(attempt.repaired,true);
  assert.equal(attempt.check.success,true);
  g=new GameController(new LocalStorageRepository(storage,'p95-vehicle-repair'));
  assert.equal(g.activeVehicle().criticalHits[0].repaired,true);
});

test('P95 falha em reparo de crítico não limpa o dano e consome ação durante combate',()=>{
  const g=new GameController(new LocalStorageRepository(new MemoryStorage(),'p95-vehicle-repair-fail'));
  g.loadStarterCharacter('tendaar');
  g.addVehicle('xwing_t65b');
  g.applyActiveVehicleCritical({roll:20});
  g.beginVehicleCombat();
  const attempt=g.attemptRepairActiveVehicleCritical(0,{rng:()=>0});
  assert.equal(attempt.repaired,false);
  assert.equal(g.activeVehicle().criticalHits[0].repaired,false);
  assert.equal(g.state.vehicles.combat.actionUsed,true);
});
