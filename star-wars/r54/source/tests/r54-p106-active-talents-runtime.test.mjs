import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { applyStatusEffect } from '../app/src/main/assets/vtt/js/rules/statusEffects.mjs';
import { setForceRating } from '../app/src/main/assets/vtt/js/rules/force.mjs';

class MemoryRepository{constructor(state=createCampaign('P106')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function hero(){
  let c=createBlankCharacter('P106');
  c.characteristics={fortaleza:2,agilidad:3,intelecto:3,astucia:3,voluntad:3,presencia:2};
  c.skills.percepcion=2;c.skills.disciplina=2;c.skills.frialdad=2;c.skills.armas_distancia_ligeras=2;
  c.derived={soak:2,wounds:{threshold:12,current:0},strain:{threshold:14,current:5},defense:{ranged:0,melee:0}};
  c.talents=['Foco intenso','Sentir o perigo','Toque do destino','Levantar rápido','Equilíbrio'];
  c=setForceRating(c,2);
  return c;
}
function game(){const g=new GameController(new MemoryRepository());g.setCharacter(hero());return g;}

test('P106: Foco Intenso custa manobra +1 Tensão e aprimora a habilidade do próximo teste',()=>{
  const g=game();g.beginCombat();const before=g.state.player.character.derived.strain.current;
  const r=g.resolveSkillCheck({skillKey:'percepcion',difficulty:2,useIntenseFocus:true,rng:()=>0.42});
  assert.equal(r.intenseFocus.used,true);assert.equal(r.intenseFocus.strainCost,1);
  assert.equal(g.state.combat.playerTurn.maneuversUsed,1);assert.equal(g.state.combat.playerTurn.actionUsed,true);
  assert.equal(g.state.player.character.derived.strain.current,before+1);
  assert.equal(r.pool.proficiency,3);assert.equal(r.pool.ability,0);
});

test('P106: Sentir o Perigo remove até 2 Setbacks e Toque do Destino adiciona 2 Boost, ambos 1x por sessão',()=>{
  const g=game();g.startSession({rng:()=>0.99});
  const r=g.resolveSkillCheck({skillKey:'percepcion',difficulty:2,setback:3,useSenseDanger:true,useTouchOfFate:true,rng:()=>0.42});
  assert.equal(r.pool.setback,1);assert.equal(r.senseDanger.setbackRemoved,2);assert.ok(r.pool.boost>=2);assert.equal(r.touchOfFate.boost,2);
  assert.equal(g.talentActionStatus().senseDanger.usedThisSession,true);assert.equal(g.talentActionStatus().touchOfFate.usedThisSession,true);
  assert.throws(()=>g.resolveSkillCheck({skillKey:'percepcion',difficulty:1,useSenseDanger:true,rng:()=>0.42}),/já foi usado nesta sessão/i);
  assert.throws(()=>g.resolveSkillCheck({skillKey:'percepcion',difficulty:1,useTouchOfFate:true,rng:()=>0.42}),/já foi usado nesta sessão/i);
});

test('P106: nova sessão limpa os usos únicos de Sentir o Perigo e Toque do Destino',()=>{
  const g=game();g.startSession({rng:()=>0.99});g.resolveSkillCheck({skillKey:'percepcion',difficulty:1,useSenseDanger:true,useTouchOfFate:true,rng:()=>0.42});
  g.closeSession();g.startSession({rng:()=>0.99});const st=g.talentActionStatus();
  assert.equal(st.senseDanger.usedThisSession,false);assert.equal(st.touchOfFate.usedThisSession,false);
});

test('P106: Levantar Rápido remove prone como incidental sem gastar manobra',()=>{
  const g=game();g.setCharacter(applyStatusEffect(g.state.player.character,{type:'prone',duration:'encounter'}));g.beginCombat();
  const r=g.standUpFromProne({useJumpUp:true});assert.equal(r.incidental,true);assert.equal(r.combat.playerTurn.maneuversUsed,0);assert.equal(g.statusSummary().prone,false);assert.equal(g.talentActionStatus().jumpUp.usedThisTurn,true);
});

test('P106: Equilíbrio acrescenta recuperação real de Tensão ao encerrar encontro',()=>{
  const g=game();g.beginCombat();const r=g.finishCombat({strainSkill:'frialdad',useBalance:true,rng:()=>0.99});
  assert.equal(r.recovery.balance.available,true);assert.ok(r.recovery.balance.recovered>=1);assert.ok(g.state.player.character.derived.strain.current<5);
});

test('P106: bridge expõe teste simples e estado dos talentos ativos',()=>{
  const bridge=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  assert.match(bridge,/resolveSkillCheck\(options = \{\}\)/);assert.match(bridge,/talentActionStatus\(\)/);
});

test('P106: R46 expõe talentos ativos sem inventar aquisição',()=>{
  const runtime=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  assert.match(runtime,/R54 P106 — EOTE CORE ACTIVE TALENTS/);assert.match(runtime,/call\('resolveSkillCheck'/);assert.match(runtime,/useJumpUp:true/);assert.match(runtime,/useBalance:true/);
});
