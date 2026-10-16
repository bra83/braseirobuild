import test from 'node:test';
import assert from 'node:assert/strict';
import { TALENT_TREES, talentSkillPoolModifiers, talentCloseRangeDamageBonus, purchaseTalent } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { createEoteCoreCharacter } from '../app/src/main/assets/vtt/js/rules/eoteCharacterCreation.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function translated(originalName){ for(const careers of Object.values(TALENT_TREES)) for(const tree of Object.values(careers)){ const n=tree.nodes.find(x=>x.originalName===originalName); if(n) return n.name; } throw new Error(`Talento não encontrado: ${originalName}`); }
function addTalent(character,originalName,times=1){ character.talents=Array.isArray(character.talents)?character.talents:[]; for(let i=0;i<times;i++) character.talents.push(translated(originalName)); return character; }
function pathTo(tree,targetId){ const starts=tree.nodes.filter(n=>n.row===1).map(n=>n.id),adj=new Map(tree.nodes.map(n=>[n.id,[]])); for(const [a,b] of tree.edges){adj.get(a).push(b);adj.get(b).push(a);} const seen=new Set(starts),q=starts.map(id=>[id]); while(q.length){const p=q.shift(),id=p.at(-1);if(id===targetId)return p;for(const n of adj.get(id))if(!seen.has(n)){seen.add(n);q.push([...p,n]);}} return null; }
function charFor(career='bounty_hunter',spec='assassin'){
  const c=createEoteCoreCharacter({name:'Nira',speciesKey:'human',careerKey:'bounty_hunter',specializationKey:'assassin',careerFreeSkills:['atividade_criminal','armas_distancia_pesadas','percepcao','alerta'].map(x=>x.replace('atividade_criminal','atletismo').replace('percepcao','percepcion')),specializationFreeSkills:['armas_cuerpo_a_cuerpo','sigilo'],speciesChoiceSkills:['medicina','liderazgo'],motivation:'Sobreviver na Orla Exterior.',obligationType:'Dívida'});
  c.creation.careerKey=career; c.creation.specializationKey=spec; c.xp.available=500; return c;
}

test('Perseguidor e Comando acrescentam Impulso apenas às perícias previstas',()=>{
  const c=addTalent(addTalent(charFor(),'STALK',2),'COM');
  assert.equal(talentSkillPoolModifiers(c,'sigilo').boost,2);
  assert.equal(talentSkillPoolModifiers(c,'coordinacion',{boost:1}).boost,3);
  assert.equal(talentSkillPoolModifiers(c,'liderazgo').boost,1);
  assert.equal(talentSkillPoolModifiers(c,'medicina').boost,0);
});

test('talentos EotE de perícia removem Contratempo sem tornar a reserva negativa',()=>{
  const c=addTalent(addTalent(charFor(),'KILL',2),'PLAUSDEN');
  let m=talentSkillPoolModifiers(c,'liderazgo',{setback:4}); assert.equal(m.setback,2); assert.equal(m.setbackRemoved,2);
  m=talentSkillPoolModifiers(c,'coaccion',{setback:1}); assert.equal(m.setback,0); assert.equal(m.setbackRemoved,1);
  assert.equal(talentSkillPoolModifiers(c,'mecanica',{setback:2}).setback,2);
});

test('À queima-roupa soma dano por grau só com armas à distância leves/pesadas em curto/interação',()=>{
  const c=addTalent(charFor(),'POINTBL',2);
  assert.equal(talentCloseRangeDamageBonus(c,{weapon:{skill:'armas_distancia_ligeras'},targetRange:'Curto'}),2);
  assert.equal(talentCloseRangeDamageBonus(c,{weapon:{skill:'armas_distancia_pesadas'},targetRange:'Interação'}),2);
  assert.equal(talentCloseRangeDamageBonus(c,{weapon:{skill:'armas_distancia_ligeras'},targetRange:'Médio'}),0);
  assert.equal(talentCloseRangeDamageBonus(c,{weapon:{skill:'armas_corpo_a_corpo'},targetRange:'Interação'}),0);
});

test('resolveSkillCheck aplica Perseguidor à reserva real',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'talent-skill')); game.loadStarterCharacter('zal');
  addTalent(game.state.player.character,'STALK');
  const r=game.resolveSkillCheck({skillKey:'sigilo',difficulty:0,setback:1,rng:()=>0.2});
  assert.equal(r.pool.boost,1); assert.equal(r.pool.setback,1); assert.equal(r.talentModifiers.talentBoost,1);
});

test('ataque real aplica À queima-roupa e não aplica fora do alcance permitido',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'talent-shot')); game.loadStarterCharacter('arkhan'); addTalent(game.state.player.character,'POINTBL');
  const close=game.resolveWeaponAttack({weaponIndex:0,targetRange:'Curto',rng:()=>0.2}); assert.equal(close.talentDamageBonus,1);
  const game2=new GameController(new LocalStorageRepository(new MemoryStorage(),'talent-shot2')); game2.loadStarterCharacter('arkhan'); addTalent(game2.state.player.character,'POINTBL');
  const medium=game2.resolveWeaponAttack({weaponIndex:0,targetRange:'Médio',rng:()=>0.2}); assert.equal(medium.talentDamageBonus,0);
});

test('Piloto consumado e Cartógrafo galáctico removem Contratempo nos testes veiculares correspondentes',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'talent-vehicle')); game.loadStarterCharacter('zal'); game.addVehicle('xwing_t65b');
  addTalent(game.state.player.character,'SKILLJOCK'); addTalent(game.state.player.character,'GALMAP');
  const pil=game.vehiclePilotingHazardCheck({setback:2,rng:()=>0.2}); assert.equal(pil.check.pool.setback,1); assert.equal(pil.talentModifiers.setbackRemoved,1);
  const nav=game.plotHyperspaceCourse({difficulty:1,setback:2,rng:()=>0.2}); assert.equal(nav.check.pool.setback,1); assert.equal(nav.talentModifiers.setbackRemoved,1);
});

test('Firmeza adquirida pela árvore aumenta o limiar de Tensão exatamente em 1',()=>{
  let c=charFor('bounty_hunter','assassin'); const tree=TALENT_TREES.bounty_hunter.assassin; const target=tree.nodes.find(n=>n.originalName==='GRIT');
  assert.ok(target); const path=pathTo(tree,target.id); assert.ok(path); for(const id of path.slice(0,-1)) c=purchaseTalent(c,id).character;
  const before=c.derived.strain.threshold; c=purchaseTalent(c,target.id).character; assert.equal(c.derived.strain.threshold,before+1);
});
