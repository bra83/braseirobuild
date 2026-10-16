import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function game(key='p101'){return new GameController(new LocalStorageRepository(new MemoryStorage(),key))}
function hero(){
  const c=createBlankCharacter('Rin');
  c.characteristics={fortaleza:2,agilidad:3,intelecto:3,astucia:2,voluntad:3,presencia:2};
  c.skills.mecanica=2;c.skills.percepcion=2;c.skills.disciplina=2;c.skills.frialdad=1;c.skills.atletismo=1;
  c.derived={soak:2,wounds:{threshold:12,current:0},strain:{threshold:13,current:0},defense:{ranged:0,melee:0}};
  c.gear=[];c.weapons=[];c.armor=[];return c;
}

test('P101: assistência de equipamento rejeita item que não está realmente possuído',()=>{
  const g=game('owned-gear');const c=hero();g.setCharacter(c);
  assert.throws(()=>g.resolveSkillCheck({skillKey:'mecanica',difficulty:0,setback:1,gearAssist:{itemId:'tool_kit',missingToolsSetback:1},rng:()=>0}),/realmente possuído/);
});

test('P101: Kit de ferramentas possuído remove apenas setback declarado de falta de ferramentas',()=>{
  const g=game('tool-kit');const c=hero();c.gear.push({id:'tool_kit',name:'Kit de ferramentas',quantity:1});g.setCharacter(c);
  const r=g.resolveSkillCheck({skillKey:'mecanica',difficulty:0,setback:3,gearAssist:{itemId:'tool_kit',missingToolsSetback:2},rng:()=>0});
  assert.equal(r.gearModifier.removedSetback,2);assert.equal(r.pool.setback,1);
});

test('P101: Eletrobinóculos possuídos removem no máximo 1 setback auditado',()=>{
  const g=game('optics');const c=hero();c.gear.push({id:'electrobinoculars',name:'Eletrobinóculos',quantity:1});g.setCharacter(c);
  const r=g.resolveSkillCheck({skillKey:'percepcion',difficulty:0,setback:3,gearAssist:{itemId:'electrobinoculars',longRangeSetback:1,poorLightingSetback:1},rng:()=>0});
  assert.equal(r.gearModifier.removedSetback,1);assert.equal(r.pool.setback,2);
});

test('P101: remendo de reparo cura droide, consome item e persiste limite diário',()=>{
  const g=game('droid-patch');const c=hero();c.creation={...(c.creation||{}),speciesKey:'droid'};c.species='Droide';c.derived.wounds.current=7;c.gear.push({id:'emergency_repair_patch',name:'Remendo de reparo de emergência',quantity:2});g.setCharacter(c);
  const r=g.useEmergencyRepairPatch();
  assert.equal(r.healed,3);assert.equal(r.remaining,1);assert.equal(r.character.derived.wounds.current,4);assert.equal(r.character.repairPatchUsesToday,1);
  const reopened=new GameController(g.repository);assert.equal(reopened.state.player.character.repairPatchUsesToday,1);assert.equal(reopened.state.player.character.gear.find(x=>x.id==='emergency_repair_patch').quantity,1);
});

test('P101: remendo é ação real em combate e orgânico não pode usá-lo',()=>{
  const organic=game('organic');const c1=hero();c1.gear.push({id:'emergency_repair_patch',name:'Remendo',quantity:1});organic.setCharacter(c1);
  assert.throws(()=>organic.useEmergencyRepairPatch(),/droides/);
  const g=game('combat-patch');const c=hero();c.creation={...(c.creation||{}),speciesKey:'droid'};c.species='Droide';c.derived.wounds.current=5;c.gear.push({id:'emergency_repair_patch',name:'Remendo de reparo de emergência',quantity:1});g.setCharacter(c);g.beginCombat();
  const r=g.useEmergencyRepairPatch();assert.equal(r.combat.playerTurn.actionUsed,true);assert.equal(r.healed,3);
});

test('P101: Fear continua source-bound, persiste efeitos e não rerrola a mesma fonte',()=>{
  const g=game('fear');const c=hero();g.setCharacter(c);g.beginCombat();
  const a=g.resolveFearCheck({sourceId:'scene:test:fear:moderate',level:'moderate',rng:()=>0});
  const b=g.resolveFearCheck({sourceId:'scene:test:fear:moderate',level:'moderate',rng:()=>0});
  assert.equal(a.skipped,undefined);assert.equal(b.skipped,true);assert.equal(b.reason,'already_checked');
  assert.ok(g.state.player.character.statusEffects.some(e=>e.type==='fear_checked'&&e.sourceId==='scene:test:fear:moderate'));
});

test('P101: Encumbrance/lifting continuam no engine e refletem carga real',()=>{
  const g=game('load');const c=hero();c.weapons=[{id:'cargo',name:'Carga',encumbrance:9,quantity:1}];g.setCharacter(c);
  const s=g.encumbranceStatus();assert.equal(s.threshold,7);assert.equal(s.over,2);assert.equal(s.setbackOnBrawnAgility,2);
  const lift=g.liftingDifficulty(10,{helperBrawn:[2]});assert.equal(typeof lift.difficulty,'number');
});

test('P101: bridge/contratos/R46 expõem Fear, carga, gear assist e reparo sem grant de item ou raw status',()=>{
  const bridge=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  const ts=fs.readFileSync(new URL('../gameBridgeClient.ts',import.meta.url),'utf8');
  assert.match(bridge,/useEmergencyRepairPatch\(\)/);
  assert.match(bridge,/resolveFearCheck\(options/);
  assert.match(ts,/useEmergencyRepairPatch/);
  assert.match(runtime,/P101 · MECÂNICA EXECUTÁVEL · SEM RAG/);
  assert.match(runtime,/RESOLVER TESTE DE MEDO/);
  assert.match(runtime,/CALCULAR LEVANTAMENTO/);
  assert.match(runtime,/ROLAR TESTE COM EQUIPAMENTO/);
  assert.match(runtime,/USAR REMENDO DE REPARO/);
  const p101=(runtime.split('/* R54 P101 —')[1]||'').split('/* R54 P102 —')[0];
  assert.ok(!/applyStatus\(|applyPlayerCondition\(|grant/i.test(p101),'P101 não deve expor mutação bruta/grant');
});
