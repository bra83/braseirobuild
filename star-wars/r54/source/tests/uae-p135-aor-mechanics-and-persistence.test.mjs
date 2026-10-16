import test from 'node:test';
import assert from 'node:assert/strict';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {AOR_RUNTIME_TALENT_ALIASES} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {aorTalentCoverage,aorTalentCatalog} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {createAorCoreCharacter} from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import {purchaseSpecialization} from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import {eligibleTalents,purchaseTalent,talentRankByOriginal,talentSkillPoolModifiers,
  frenziedAttackOption,intenseFocusOption,personalCombatTalentDamage,
  targetedBlowOption,anatomyLessonsOption,sniperShotOption,armorMasterBonuses,
  naturalRerollOption,masterSkillDifficultyOption
} from '../app/src/main/assets/vtt/js/rules/talents.mjs';

const sourceAliases = Object.freeze({
  'CONCENTRACIÓN INTENSA':'INTFOC', 'LECCIONES DE ANATOMÍA':'ANAT',
  'FUERZA SALVAJE':'FERSTR', 'ATAQUE FURIOSO':'FRENZ',
  'GOLPE DIRIGIDO':'TARGBL', 'DISPARO DE PRECISIÓN':'SNIPSHOT',
  'DOMINIO DE BLINDAJES':'ARM', 'DOMINIO DE BLINDAJES MEJORADO':'ARMIMP',
  'ACECHADOR':'STALK', 'ENTRENAMIENTO FÍSICO':'PHYTRAIN',
  'DELINCUENTE NATO':'NATROG', 'ERUDITO NATO':'NATSCH',
  'MATÓN NATO':'NATENF', 'CHAPUCERO NATO':'NATTIN',
  'PIRATA INFORMÁTICO CONSUMADO':'SKILSLI'
});
const create = () => {
  const c=createAorCoreCharacter({name:'Teste P135',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
    careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],
    specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],
    motivation:'Liberdade',dutyType:'Superioridade espacial'});
  c.xp.available=1500;c.xp.total=1500;
  return c;
};
function buy(career,spec,originalName,{row=null}={}) {
  let character=create();
  if(career!=='ace'||spec!=='pilot') character=purchaseSpecialization(character,career,spec).character;
  const tree=AOR_TALENT_TREES[career][spec];
  const target=tree.nodes.find(n=>n.originalName===originalName&&(row===null||n.row===row));
  assert.ok(target,`Missing source node: ${career}/${spec}/${originalName}`);
  const adjacent=new Map(tree.nodes.map(n=>[n.id,[]]));
  for(const [a,b] of tree.edges){adjacent.get(a).push(b);adjacent.get(b).push(a);}
  const queue=tree.nodes.filter(n=>n.row===1).map(n=>[n.id]);
  const visited=new Set();let path=null;
  while(queue.length){const next=queue.shift(),id=next.at(-1);if(visited.has(id))continue;visited.add(id);
    if(id===target.id){path=next;break;}
    for(const neighbor of adjacent.get(id))if(!visited.has(neighbor))queue.push([...next,neighbor]);
  }
  assert.ok(path,`Disconnected source node ${target.id}`);
  for(const id of path){
    const node=eligibleTalents(character,{careerKey:career,specializationKey:spec}).find(n=>n.id===id);
    assert.ok(node.unlocked,`Locked path ${id}`);
    const opts={careerKey:career,specializationKey:spec};
    if(node.choiceType==='characteristic')opts.characteristicKey='intelecto';
    if(node.choiceType==='combatSkill')opts.skillKey='armas_distancia_ligeras';
    if(node.choiceType==='twoSkills')opts.skillKeys=['sigilo','medicina'];
    character=purchaseTalent(character,id,opts).character;
  }
  return {character,target};
}

test('P135 catálogo AoR mantém todas as 360 posições e habilita somente efeitos conectados',()=>{
  const all=aorTalentCatalog();const coverage=aorTalentCoverage();
  assert.equal(all.length,360);assert.equal(coverage.positions,360);
  for(const [original,canonical] of Object.entries(sourceAliases)){
    assert.equal(AOR_RUNTIME_TALENT_ALIASES[original],canonical);
    assert.ok(all.some(n=>n.originalName===original));
    assert.ok(all.filter(n=>n.originalName===original).every(n=>n.mechanicallyActive));
  }
  assert.ok(all.filter(n=>n.originalName==='DISPARO CERTERO').every(n=>n.mechanicallyActive)); // P137 True Aim
  assert.equal(coverage.mechanicalPositions,all.filter(n=>n.mechanicallyActive).length);
});

for(const [career,spec,name,canonical] of [
  ['diplomat','quartermaster','CONCENTRACIÓN INTENSA','INTFOC'],
  ['soldier','medic','LECCIONES DE ANATOMÍA','ANAT'],
  ['soldier','commando','FUERZA SALVAJE','FERSTR'],
  ['spy','infiltrator','ATAQUE FURIOSO','FRENZ'],
  ['soldier','sharpshooter','GOLPE DIRIGIDO','TARGBL'],
  ['soldier','sharpshooter','DISPARO DE PRECISIÓN','SNIPSHOT'],
  ['soldier','commando','DOMINIO DE BLINDAJES','ARM'],
  ['soldier','commando','DOMINIO DE BLINDAJES MEJORADO','ARMIMP'],
  ['spy','scout','ACECHADOR','STALK'],
  ['soldier','commando','ENTRENAMIENTO FÍSICO','PHYTRAIN'],
  ['spy','infiltrator','DELINCUENTE NATO','NATROG'],
  ['engineer','scientist','ERUDITO NATO','NATSCH'],
  ['diplomat','agitator','MATÓN NATO','NATENF'],
  ['engineer','mechanic','CHAPUCERO NATO','NATTIN'],
  ['spy','slicer','PIRATA INFORMÁTICO CONSUMADO','SKILSLI']
]) {
  test(`P135 compra AoR ${name}: efeito no motor, PE e restauração JSON`,()=>{
    const {character,target}=buy(career,spec,name);
    assert.equal(character.creation.rulesProfile,'AOR_CORE');
    assert.equal(talentRankByOriginal(character,canonical),1);
    assert.ok(character.talentPurchases.some(p=>p.nodeId===target.id));
    assert.ok(character.xp.available<1500);
    const restored=JSON.parse(JSON.stringify(character));
    assert.equal(talentRankByOriginal(restored,canonical),1);
    switch(canonical){
      case 'INTFOC': assert.equal(intenseFocusOption(restored).available,true);break;
      case 'ANAT': assert.equal(anatomyLessonsOption(restored).available,true);break;
      case 'FERSTR': assert.equal(personalCombatTalentDamage(restored,{weapon:{skill:'pelea'},targetRange:'Interação'}).feralStrength,1);break;
      case 'FRENZ': assert.equal(frenziedAttackOption(restored,1).abilityUpgrades,1);break;
      case 'TARGBL': assert.equal(targetedBlowOption(restored).available,true);break;
      case 'SNIPSHOT': assert.equal(sniperShotOption(restored,1).difficultyUpgrades,1);break;
      case 'ARM': assert.equal(armorMasterBonuses(restored,{armorSoak:1,worn:true}).soakBonus,1);break;
      case 'ARMIMP': assert.equal(armorMasterBonuses(restored,{armorSoak:2,worn:true}).defenseBonus,1);break;
      case 'STALK': assert.equal(talentSkillPoolModifiers(restored,'sigilo').talentBoost,1);break;
      case 'NATROG': assert.equal(naturalRerollOption(restored,'sigilo').available,true);break;
      case 'NATSCH': assert.equal(naturalRerollOption(restored,'educacion').available,true);break;
      case 'NATENF': assert.equal(naturalRerollOption(restored,'coaccion').available,true);break;
      case 'NATTIN': assert.equal(naturalRerollOption(restored,'mecanica').available,true);break;
      case 'SKILSLI': assert.equal(masterSkillDifficultyOption(restored,'slicer').available,false);break;
      case 'PHYTRAIN': assert.equal(talentSkillPoolModifiers(restored,'atletismo').talentBoost,1);
        assert.equal(talentSkillPoolModifiers(restored,'aguante').talentBoost,1);
        assert.equal(talentSkillPoolModifiers(restored,'sigilo').talentBoost,0);break;
    }
  });
}

test('P135 talentos AoR não adquiridos não alteram testes nem combate',()=>{
  const c=create();
  assert.equal(intenseFocusOption(c).available,false);
  assert.equal(frenziedAttackOption(c,0).available,false);
  assert.equal(talentSkillPoolModifiers(c,'atletismo').talentBoost,0);
  assert.equal(armorMasterBonuses(c,{armorSoak:2,worn:true}).defenseBonus,0);
});
