import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { aorTalentCoverage, aorTalentCatalog } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { aorCanonicalTalentName } from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import { createAorCoreCharacter } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { purchaseSpecialization } from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import { eligibleTalents, purchaseTalent, talentRankByOriginal, talentTaskTimeSpec, talentSkillPoolModifiers } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';

const copy = structuredClone;
const mkModule = id => publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
const mk = id => new AdventureRuntime(mkModule(id));

for (const {id} of publishedAdventureCatalog()) {
  test(`P138 ${id}: comando único preserva flags, contadores, journal e objetivos após 3 reaberturas`, () => {
    const module = mkModule(id);
    const runtime = new AdventureRuntime(module);
    module.triggers.push({ id: 'p138-idempotent-trigger', event: 'P138_COMMAND', maxFires: 100,
      effects: [{ type: 'adjust-counter', counter: 'commandCount', delta: 1 },
        { type: 'progress-objective', objectiveId: 'episode-1', delta: 0.1 }] });
    // Use a fresh runtime with the complete authored module.
    let r = new AdventureRuntime(module);
    const initial = r.state.canonicalEvents.length;
    const opts = { idempotencyKey: 'android:tap:0001' };
    assert.deepEqual(r.dispatchEvent('P138_COMMAND', {node:'A', n:1}, 'android', opts), ['p138-idempotent-trigger']);
    assert.equal(r.state.counters.commandCount,1);
    assert.equal(r.state.objectives['episode-1'].progress,0.1);
    assert.equal(r.state.canonicalEvents.length,initial+3);
    for (let i=0; i<3; i++) {
      const before = r.serialize();
      r = AdventureRuntime.restore(module,JSON.parse(before));
      assert.deepEqual(r.dispatchEvent('P138_COMMAND', {n:1,node:'A'}, 'android',opts),[]);
      assert.equal(r.serialize(),before);
      assert.equal(r.state.counters.commandCount,1);
      assert.equal(validateAdventureCheckpoint(module,r.getState()),true);
    }
    assert.throws(() => r.dispatchEvent('P138_COMMAND',{n:2,node:'A'},'android',opts),/reutilizada/);
    assert.throws(() => r.dispatchEvent('DIFFERENT',{n:1,node:'A'},'android',opts),/reutilizada/);
    assert.equal(r.state.counters.commandCount,1);
    assert.equal(validatePublishedAdventureRuntimeState(module,r.getState()),true);
    // Same key from an independent source is not the same command.
    r.dispatchEvent('P138_COMMAND',{n:1,node:'A'},'script',opts);
    assert.equal(r.state.counters.commandCount,2);
  });
}

test('P138 idempotência: chave inválida ou comando divergente nunca altera checkpoint', () => {
  const r=mk('under-black-sun');
  const before=r.serialize();
  for(const key of ['', ' ', '__proto__', 'á', 'a'.repeat(129), 1, {}, [], '\n']) {
    assert.throws(() => r.dispatchEvent('X',{},'android',{idempotencyKey:key}),/inválida/);
    assert.equal(r.serialize(),before);
  }
  r.dispatchEvent('X',{x:1},'android',{idempotencyKey:'id1'});
  const after=r.serialize();
  assert.throws(() => r.dispatchEvent('X',{x:2},'android',{idempotencyKey:'id1'}),/reutilizada/);
  assert.equal(r.serialize(),after);
});

test('P138 idempotência: checkpoint com duplicata ou chave malformada é rejeitado', () => {
  const module=mkModule('under-black-sun');
  const r=new AdventureRuntime(module);
  r.dispatchEvent('X',{x:1},'android',{idempotencyKey:'id1'});
  r.dispatchEvent('X',{x:2},'android',{idempotencyKey:'id2'});
  const state=r.getState();
  const dup=copy(state); dup.canonicalEvents.at(-1).idempotencyKey='id1';
  assert.throws(()=>AdventureRuntime.restore(module,dup),/duplicado/);
  const bad=copy(state);bad.canonicalEvents.at(-1).idempotencyKey=' '; 
  assert.throws(()=>AdventureRuntime.restore(module,bad),/inválida/);
  assert.equal(r.serialize(),JSON.stringify(state));
});

test('P138 idempotência: trigger quebrado reverte chave e permite retry corrigido', () => {
  const module=mkModule('under-black-sun');
  module.triggers.push({id:'p138-bad',event:'P138_BROKEN',effects:[{type:'adjust-counter',counter:'x',delta:'broken'}]});
  const r=new AdventureRuntime(module),before=r.serialize();
  assert.throws(()=>r.dispatchEvent('P138_BROKEN',{},'android',{idempotencyKey:'retry:1'}),/Variação de contador inválida/);
  assert.equal(r.serialize(),before);
  assert.equal(r.state.canonicalEvents.filter(e=>e.idempotencyKey==='retry:1').length,0);
  r.dispatchEvent('P138_SAFE',{},'android',{idempotencyKey:'retry:1'});
  assert.equal(r.state.canonicalEvents.filter(e=>e.idempotencyKey==='retry:1').length,1);
});

function create() {
 const c=createAorCoreCharacter({name:'Explorador P138',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
 careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['astronavegacion','artilleria'],
 speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',dutyType:'Superioridade espacial'});
 c.xp.available=5000;c.xp.total=5000;return c;
}
function buy(career,spec,name,ordinal=0,existing=null) {
 let c=existing??create();if(!existing && (career!=='ace'||spec!=='pilot'))c=purchaseSpecialization(c,career,spec).character;
 const tree=AOR_TALENT_TREES[career][spec];const target=tree.nodes.filter(n=>n.originalName===name)[ordinal];
 assert.ok(target,`${career}/${spec}/${name}#${ordinal}`);
 const adjacent=new Map(tree.nodes.map(n=>[n.id,[]]));
 for(const [a,b] of tree.edges){adjacent.get(a).push(b);adjacent.get(b).push(a);}
 const queue=tree.nodes.filter(n=>n.row===1).map(n=>[n.id]),seen=new Set();let path=null;
 while(queue.length){const p=queue.shift(),id=p.at(-1);if(seen.has(id))continue;seen.add(id);
 if(id===target.id){path=p;break;}for(const n of adjacent.get(id))if(!seen.has(n))queue.push([...p,n]);}
 assert.ok(path);
 for(const id of path){const n=eligibleTalents(c,{careerKey:career,specializationKey:spec}).find(x=>x.id===id);
 if(n.purchased)continue;
 const opt={careerKey:career,specializationKey:spec};
 if(n.choiceType==='characteristic')opt.characteristicKey='intelecto';
 if(n.choiceType==='combatSkill')opt.skillKey='armas_distancia_ligeras';
 c=purchaseTalent(c,id,opt).character;}
 return c;
}

class Repo {constructor(){this.state=createCampaign('P138')}load(){return copy(this.state)}save(s){this.state=copy(s)}}
function game(c){const repo=new Repo();const g=new GameController(repo);g.setCharacter(c);return {g,repo};}
const names={INVESTIGADOR:['RESEARCHER',2], 'RASTREADOR EXPERTO':['EXPTRACK',2], 'APTITUD TECNOLÓGICA':['TECHAPT',2]};

test('P138 AoR cobertura real aumenta de 205 para 211 posições sem marcar outras como implementadas',()=>{
 const cov=aorTalentCoverage();assert.equal(cov.positions,360);assert.ok(cov.mechanicalPositions>=211);
 for(const [name,[alias,amount]] of Object.entries(names)) {
  assert.equal(aorCanonicalTalentName(name),alias);
  const rows=aorTalentCatalog().filter(n=>n.originalName===name);
  assert.equal(rows.length,amount);assert.ok(rows.every(n=>n.mechanicallyActive&&n.purchaseActive));
 }
 assert.ok(aorTalentCatalog().filter(n=>n.originalName==='CINTURÓN DE ACCESORIOS').every(n=>n.mechanicallyActive));
});

for (const [name,[alias,amount]] of Object.entries(names)) {
 test(`P138 ${name}: compra na árvore oficial, PE, ranking e persistência`,()=>{
  const found=Object.entries(AOR_TALENT_TREES).flatMap(([career,specs])=>Object.entries(specs).map(([spec,tree])=>({career,spec,nodes:tree.nodes.filter(n=>n.originalName===name)}))).find(x=>x.nodes.length);
  assert.ok(found);
  let c=buy(found.career,found.spec,name,0);
  assert.equal(talentRankByOriginal(c,alias),1);
  assert.ok(c.xp.available<5000);
  assert.equal(talentRankByOriginal(JSON.parse(JSON.stringify(c)),alias),1);
  const time=talentTaskTimeSpec(c,alias==='EXPTRACK'?'supervivencia':alias==='RESEARCHER'?'educacion':'computadoras',
   {tracking:true,research:true,taskDurationMinutes:120});
  assert.ok(time.multiplier<1);
 });
}

test('P138 Investigador: somente perícias de conhecimento, remoção por nível e pesquisa em metade do tempo',()=>{
 const found=Object.entries(AOR_TALENT_TREES).flatMap(([career,specs])=>Object.entries(specs).map(([spec,tree])=>({career,spec,nodes:tree.nodes.filter(n=>n.originalName==='INVESTIGADOR')}))).find(x=>x.nodes.length);
 const c=buy(found.career,found.spec,'INVESTIGADOR');
 assert.equal(talentSkillPoolModifiers(c,'educacion',{setback:3}).setback,2);
 assert.equal(talentSkillPoolModifiers(c,'supervivencia',{setback:3}).setback,3);
 assert.equal(talentTaskTimeSpec(c,'educacion',{research:true,taskDurationMinutes:90}).durationMinutes,45);
 assert.equal(talentTaskTimeSpec(c,'educacion',{research:false,taskDurationMinutes:90}).durationMinutes,90);
 const {g,repo}=game(c);
 const check=g.resolveSkillCheck({skillKey:'educacion',difficulty:2,setback:2,research:true,taskDurationMinutes:90,rng:()=>0.4});
 assert.equal(check.pool.setback,1);assert.equal(check.taskTime.durationMinutes,45);
 const reopened=new GameController(repo);
 assert.equal(reopened.state.lastCheck.taskTime.durationMinutes,45);
});

test('P138 Rastreador: apenas Sobrevivência com contexto de rastreamento; nunca altera teste genérico',()=>{
 const found=Object.entries(AOR_TALENT_TREES).flatMap(([career,specs])=>Object.entries(specs).map(([spec,tree])=>({career,spec,nodes:tree.nodes.filter(n=>n.originalName==='RASTREADOR EXPERTO')}))).find(x=>x.nodes.length);
 const c=buy(found.career,found.spec,'RASTREADOR EXPERTO');
 assert.equal(talentSkillPoolModifiers(c,'supervivencia',{setback:3,tracking:true}).setback,2);
 assert.equal(talentSkillPoolModifiers(c,'supervivencia',{setback:3}).setback,3);
 assert.equal(talentTaskTimeSpec(c,'supervivencia',{tracking:true,taskDurationMinutes:60}).durationMinutes,30);
 const {g,repo}=game(c);
 const check=g.resolveSkillCheck({skillKey:'supervivencia',difficulty:2,setback:3,tracking:true,taskDurationMinutes:60,rng:()=>0.4});
 assert.equal(check.pool.setback,2);assert.equal(check.taskTime.durationMinutes,30);
 assert.equal(new GameController(repo).state.lastCheck.taskTime.durationMinutes,30);
});

test('P138 Aptidão tecnológica: computadores 25% por nível, sem alterar outros testes',()=>{
 const found=Object.entries(AOR_TALENT_TREES).flatMap(([career,specs])=>Object.entries(specs).map(([spec,tree])=>({career,spec,nodes:tree.nodes.filter(n=>n.originalName==='APTITUD TECNOLÓGICA')}))).find(x=>x.nodes.length);
 let c=buy(found.career,found.spec,'APTITUD TECNOLÓGICA');
 assert.equal(talentTaskTimeSpec(c,'computadoras',{taskDurationMinutes:100}).durationMinutes,75);
 if(found.nodes.length>1) c=buy(found.career,found.spec,'APTITUD TECNOLÓGICA',1,c);
 assert.equal(talentTaskTimeSpec(c,'computadoras',{taskDurationMinutes:100}).durationMinutes,found.nodes.length>1?50:75);
 assert.equal(talentTaskTimeSpec(c,'mecanica',{taskDurationMinutes:100}).durationMinutes,100);
 const {g,repo}=game(c);
 const check=g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,taskDurationMinutes:100,rng:()=>0.4});
 assert.equal(check.taskTime.durationMinutes,found.nodes.length>1?50:75);
 assert.equal(new GameController(repo).state.lastCheck.taskTime.durationMinutes,check.taskTime.durationMinutes);
});

test('P138 duração inválida rejeitada antes de rolagem e commit do controlador',()=>{
 const {g,repo}=game(create());
 const before=copy(g.state);
 for(const duration of [-1,Infinity,NaN,'20',{},true]){
  assert.throws(()=>g.resolveSkillCheck({skillKey:'computadoras',taskDurationMinutes:duration,rng:()=>0.4}),/Duração de tarefa inválida/);
  assert.deepEqual(g.state,before);
  assert.deepEqual(repo.state,before);
 }
});

test('P138 tempo e contratempos somam níveis de Investigador e Rastreador sem duplicar metade do tempo',()=>{
 for (const [name,alias,skill,opts] of [
  ['INVESTIGADOR','RESEARCHER','educacion',{research:true}],
  ['RASTREADOR EXPERTO','EXPTRACK','supervivencia',{tracking:true}]
 ]){
  const found=Object.entries(AOR_TALENT_TREES).flatMap(([career,specs])=>Object.entries(specs).map(([spec,tree])=>({career,spec,nodes:tree.nodes.filter(n=>n.originalName===name)}))).find(x=>x.nodes.length>1);
  if(!found)continue;
  let c=buy(found.career,found.spec,name);
  c=buy(found.career,found.spec,name,1,c);
  assert.equal(talentRankByOriginal(c,alias),2);
  assert.equal(talentSkillPoolModifiers(c,skill,{setback:3,...opts}).setback,1);
  assert.equal(talentTaskTimeSpec(c,skill,{taskDurationMinutes:80,...opts}).durationMinutes,40);
 }
});

test('P138 Android oferece pesquisa, rastreamento e computadores somente a personagens com talentos AoR',async()=>{
 const {readFileSync}=await import('node:fs');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.match(ui,/data-p138-task-skill/);
 assert.match(ui,/p138Names\.has\('INVESTIGADOR'\)/);
 assert.match(ui,/p138Names\.has\('RASTREADOR EXPERTO'\)/);
 assert.match(ui,/p138Names\.has\('APTITUD TECNOLÓGICA'\)/);
 assert.match(ui,/tracking:context==='tracking',research:context==='research'/);
 assert.match(ui,/taskDurationMinutes:num\(panel,'\[data-p138-task-time\]'/);
});
