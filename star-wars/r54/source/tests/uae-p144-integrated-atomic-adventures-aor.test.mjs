/** P144: integration/regression for atomic Android controller + AoR contextual talent consumers. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {createAorCoreCharacter,AOR_CAREERS} from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import {SKILLS} from '../app/src/main/assets/vtt/js/rules/character.mjs';
import {talentRankByOriginal,talentSkillPoolModifiers,talentTaskTimeSpec,codebreakerDifficultyReduction,purchaseTalent} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
const copy=structuredClone;
class Repo {
  constructor(id){this.state=createCampaign(id);this.writes=0;this.fail=false;}
  load(){return copy(this.state)}
  save(state){if(this.fail)throw Error('P144 simulated storage failure');this.state=copy(state);this.writes++;}
}
function game(id){const repo=new Repo(id);const g=new GameController(repo);g.loadStarterCharacter('cael');return {g,repo};}
for (const {id} of publishedAdventureCatalog()) {
  test(`P144 ${id}: início transacional único, falha de armazenamento sem meia sessão`,()=>{
    const {g,repo}=game('start:'+id),before=copy(g.state),writes=repo.writes;
    repo.fail=true;
    assert.throws(()=>g.startPublishedAdventureSession(id,{rng:()=>.5}),/storage failure/);
    assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);assert.equal(repo.writes,writes);
    repo.fail=false;
    const started=g.startPublishedAdventureSession(id,{rng:()=>.5});
    assert.equal(started.adventure.active,true);
    assert.equal(repo.writes,writes+1,'apenas um save para sessão, cena, mundo e UAE');
    assert.equal(g.state.world.publishedAdventureRuntime.adventureId,id);
    assert.deepEqual(new GameController(repo).state.world.publishedAdventureRuntime,g.state.world.publishedAdventureRuntime);
  });
  test(`P144 ${id}: escolha transacional não deixa cena/UAE parcial em falha de save`,()=>{
    const {g,repo}=game('choice:'+id);g.startPublishedAdventureSession(id,{rng:()=>.5});
    const adapter=getPublishedAdventureAdapter(id),choice=adapter.status(g.state.world).node.choices[0];
    assert.ok(choice,`sem escolha inicial em ${id}`);
    const before=copy(g.state),writes=repo.writes;
    repo.fail=true;assert.throws(()=>g.advancePublishedAdventure(choice.id),/storage failure/);
    assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);assert.equal(repo.writes,writes);
    repo.fail=false;const after=g.advancePublishedAdventure(choice.id);
    assert.equal(repo.writes,writes+1);assert.equal(after.adventure.progress.currentNodeId,g.state.world.publishedAdventureRuntime.currentNodeId);
    assert.notEqual(after.adventure.progress.currentNodeId,before.world.publishedAdventure.currentNodeId);
    assert.deepEqual(new GameController(repo).state.world.publishedAdventureRuntime,g.state.world.publishedAdventureRuntime);
  });
}
function aorWithTalent(originalName,rank=1){
  const match=Object.entries(AOR_TALENT_TREES).flatMap(([career,specs])=>Object.entries(specs).map(([specialization,tree])=>({career,specialization,tree})))
    .find(({tree})=>tree.nodes.filter(n=>n.originalName===originalName).length>=rank);
  assert.ok(match,originalName);
  const {career,specialization,tree}=match;
  const cskills=AOR_CAREERS[career].skills,sskills=AOR_CAREERS[career].specializations[specialization].skills;
  const nonCareer=Object.keys(SKILLS).filter(key=>![...cskills,...sskills].includes(key));
  const c=createAorCoreCharacter({name:'P144',speciesKey:'human',careerKey:career,specializationKey:specialization,partySize:1,
    careerFreeSkills:cskills.slice(0,4),specializationFreeSkills:sskills.slice(0,2),speciesChoiceSkills:nonCareer.slice(0,2),motivation:'Liberdade',dutyType:'Superioridade espacial'});
  const slots=tree.nodes.filter(n=>n.originalName===originalName).slice(0,rank);
  c.talentPurchases=slots.map(n=>({nodeId:n.id,originalName:n.originalName,name:n.name,cost:n.cost,source:n.source}));
  c.talents.push(...slots.map(n=>n.name));
  return c;
}
test('P144: 9 posições AoR executáveis novas, sem conceder efeitos a outras posições',()=>{
  assert.ok(aorTalentCoverage().mechanicalPositions>=224);
  for(const [name,key,rank] of [['DOTES DE MANDO','COMMANDPRES',2],['OBSERVADOR','KEENEYED',1],['DESCIFRADOR','CODEBREAKER',2],['MONTARAZ','OUTDOORSMAN',2]]){
    assert.equal(aorCanonicalTalentName(name),key);
    assert.equal(talentRankByOriginal(aorWithTalent(name,rank),key),rank);
  }
});
test('P144: Dotes de comando + Observador removem somente revés nas perícias permitidas',()=>{
  const commander=aorWithTalent('DOTES DE MANDO',2),observer=aorWithTalent('OBSERVADOR');
  for(const skill of ['liderazgo','frialdad']) assert.equal(talentSkillPoolModifiers(commander,skill,{setback:3}).setback,1);
  for(const skill of ['percepcion','alerta']) assert.equal(talentSkillPoolModifiers(observer,skill,{setback:2}).setback,1);
  assert.equal(talentSkillPoolModifiers(commander,'computadoras',{setback:3}).setback,3);
  assert.equal(talentSkillPoolModifiers(observer,'liderazgo',{setback:2}).setback,2);
});
test('P144: Decifrador só reduz dificuldade e revés de código explicitamente declarado',()=>{
  const c=aorWithTalent('DESCIFRADOR',2);
  assert.equal(codebreakerDifficultyReduction(c,'computadoras',{codebreaking:true}),1);
  assert.equal(codebreakerDifficultyReduction(c,'educacion',{codebreaking:true}),1);
  assert.equal(codebreakerDifficultyReduction(c,'computadoras'),0);
  assert.equal(codebreakerDifficultyReduction(c,'medicina',{codebreaking:true}),0);
  assert.equal(talentSkillPoolModifiers(c,'computadoras',{setback:3,codebreaking:true}).setback,1);
  assert.equal(talentSkillPoolModifiers(c,'computadoras',{setback:3}).setback,3);
});
test('P144: Mateiro reduz revés e tempo apenas em deslocamento ambiental',()=>{
  const c=aorWithTalent('MONTARAZ',2);
  assert.equal(talentSkillPoolModifiers(c,'supervivencia',{setback:4,outdoors:true}).setback,2);
  assert.equal(talentSkillPoolModifiers(c,'supervivencia',{setback:4}).setback,4);
  assert.equal(talentTaskTimeSpec(c,'supervivencia',{outdoors:true,taskDurationMinutes:120}).durationMinutes,60);
  assert.equal(talentTaskTimeSpec(c,'supervivencia',{taskDurationMinutes:120}).durationMinutes,120);
});
test('P144: Observador reduz tempo de busca; Cinturão de acessórios continua pendente',()=>{
  const observer=aorWithTalent('OBSERVADOR');
  assert.equal(talentTaskTimeSpec(observer,'percepcion',{searchingArea:true,taskDurationMinutes:40}).durationMinutes,20);
  assert.equal(talentTaskTimeSpec(observer,'percepcion',{taskDurationMinutes:40}).durationMinutes,40);
  assert.equal(aorCanonicalTalentName('CINTURÓN DE ACCESORIOS'),'UTILITYBELT');
});
test('P144: controlador real persiste modificadores AoR e tempo sem alterar a ficha',()=>{
  const {g,repo}=game('p144:skill');
  const c=aorWithTalent('DESCIFRADOR',2);g.setCharacter(c);
  const before=copy(g.state.player.character);
  const ordinary=g.resolveSkillCheck({skillKey:'computadoras',difficulty:3,setback:3,rng:()=>.5});
  const code=g.resolveSkillCheck({skillKey:'computadoras',difficulty:3,setback:3,codebreaking:true,rng:()=>.5});
  assert.equal(ordinary.pool.difficulty+ordinary.pool.challenge,3);
  assert.equal(code.pool.difficulty+code.pool.challenge,2);
  assert.equal(code.pool.setback,1);
  assert.deepEqual(g.state.player.character,before);
  assert.equal(new GameController(repo).state.lastCheck.pool.setback,1);
});
for (const id of ['under-black-sun','beyond-the-rim']) {
  test(`P144 ${id}: teste de perícia da aventura persiste junto com UAE ou reverte tudo`,()=>{
    const {g,repo}=game('check:'+id);g.startPublishedAdventureSession(id,{rng:()=>.5});
    const before=copy(g.state),writes=repo.writes;
    repo.fail=true;
    assert.throws(()=>g.resolvePublishedAdventureCheck(0,{rng:()=>.5}),/storage failure/);
    assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);assert.equal(repo.writes,writes);
    repo.fail=false;
    const result=g.resolvePublishedAdventureCheck(0,{rng:()=>.5});
    assert.ok(result.check);assert.equal(repo.writes,writes+1);
    assert.equal(g.state.world.publishedAdventureRuntime.adventureId,id);
    assert.deepEqual(new GameController(repo).state.world.publishedAdventureRuntime,g.state.world.publishedAdventureRuntime);
  });
}
test('P144: início e encerramento de combate publicado não criam combate órfão se armazenamento falhar',()=>{
  const {g,repo}=game('combat:under-black-sun');g.startPublishedAdventureSession('under-black-sun',{rng:()=>.5});
  const before=copy(g.state),writes=repo.writes;
  repo.fail=true;
  assert.throws(()=>g.beginPublishedAdventureEncounter(null,{rng:()=>.5}),/storage failure/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);assert.equal(repo.writes,writes);
  repo.fail=false;
  const start=g.beginPublishedAdventureEncounter(null,{rng:()=>.5});
  assert.equal(start.encounter.id,'black-sun-pursuers');assert.equal(g.state.combat.active,true);
  assert.equal(repo.writes,writes+1);
  const fighting=copy(g.state),startWrites=repo.writes;
  repo.fail=true;
  assert.throws(()=>g.finishCombat({rng:()=>.5}),/storage failure/);
  assert.deepEqual(g.state,fighting);assert.deepEqual(repo.state,fighting);assert.equal(repo.writes,startWrites);
  repo.fail=false;
  const done=g.finishCombat({rng:()=>.5});
  assert.equal(done.publishedAdventureOutcome.encounterId,'black-sun-pursuers');
  assert.equal(g.state.combat.active,false);assert.equal(repo.writes,startWrites+1);
  const reopened=new GameController(repo);
  assert.equal(reopened.state.world.publishedAdventureRuntime.adventureId,'under-black-sun');
  assert.equal(reopened.state.world.publishedAdventure.systems.encounters.history.length,1);
});

test('P144: compra real de Dotes de comando, Mateiro e Decifrador conecta a árvore ao motor',()=>{
  for(const [name,career,spec,skill,ctx,expected] of [
    ['DOTES DE MANDO','commander','tactician','liderazgo',{},1],
    ['MONTARAZ','commander','tactician','supervivencia',{outdoors:true},1],
    ['DESCIFRADOR','spy','slicer','computadoras',{codebreaking:true},1]
  ]) {
    const source=AOR_CAREERS[career],specialization=source.specializations[spec];
    const excluded=[...source.skills,...specialization.skills];
    const c=createAorCoreCharacter({name:'Compra P144',speciesKey:'human',careerKey:career,specializationKey:spec,partySize:1,
      careerFreeSkills:source.skills.slice(0,4),specializationFreeSkills:specialization.skills.slice(0,2),
      speciesChoiceSkills:Object.keys(SKILLS).filter(k=>!excluded.includes(k)).slice(0,2),motivation:'Liberdade',dutyType:'Superioridade espacial'});
    c.xp.available=300;c.xp.total=300;
    const node=AOR_TALENT_TREES[career][spec].nodes.find(n=>n.originalName===name&&n.row===1);
    assert.ok(node,name);
    const bought=purchaseTalent(c,node.id,{careerKey:career,specializationKey:spec});
    assert.equal(bought.character.xp.available,300-node.cost);
    assert.equal(talentRankByOriginal(bought.character,aorCanonicalTalentName(name)),1);
    assert.equal(talentSkillPoolModifiers(bought.character,skill,{setback:2,...ctx}).setback,expected);
    assert.equal(talentSkillPoolModifiers(c,skill,{setback:2,...ctx}).setback,2);
    assert.equal(talentRankByOriginal(JSON.parse(JSON.stringify(bought.character)),aorCanonicalTalentName(name)),1);
  }
});
