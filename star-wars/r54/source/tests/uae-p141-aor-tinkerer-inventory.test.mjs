import test from 'node:test';
import assert from 'node:assert/strict';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {createAorCoreCharacter} from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import {purchaseSpecialization} from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import {eligibleTalents,purchaseTalent,talentRankByOriginal,tinkererStatus,applyTinkererHardPoint} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {weaponCatalogEntry,armorCatalogEntry,itemHardPoints,itemAvailableHardPoints,canInstallAttachment} from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const copy=structuredClone;
class Repo {constructor(){this.state=createCampaign('P141')}load(){return copy(this.state)}save(s){this.state=copy(s)}}
function hero(){
  let c=createAorCoreCharacter({name:'Mecânico P141',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
    careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['astronavegacion','artilleria'],
    speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',dutyType:'Superioridade espacial'});
  c=purchaseSpecialization(c,'engineer','mechanic').character;
  c.xp.available=4000;c.xp.total=4000;
  c.weapons=[{...copy(weaponCatalogEntry('blaster_pistol')),ownedAssetId:'owned-pistol'},
    {...copy(weaponCatalogEntry('blaster_rifle')),ownedAssetId:'owned-rifle'}];
  c.armor=[{...copy(armorCatalogEntry('padded_armor')),ownedAssetId:'owned-armor'}];
  return c;
}
function buy(c,ordinal=0){
  const tree=AOR_TALENT_TREES.engineer.mechanic;
  const target=tree.nodes.filter(n=>n.originalName==='MAÑOSO')[ordinal];
  assert.ok(target);
  const adj=new Map(tree.nodes.map(n=>[n.id,[]]));
  for(const [a,b] of tree.edges){adj.get(a).push(b);adj.get(b).push(a);}
  const queue=tree.nodes.filter(n=>n.row===1).map(n=>[n.id]);
  const visited=new Set();let path;
  while(queue.length){const p=queue.shift(),id=p.at(-1);if(visited.has(id))continue;visited.add(id);if(id===target.id){path=p;break;}for(const n of adj.get(id))if(!visited.has(n))queue.push([...p,n]);}
  assert.ok(path);
  for(const id of path){const node=eligibleTalents(c,{careerKey:'engineer',specializationKey:'mechanic'}).find(n=>n.id===id);
    if(node.purchased)continue;
    const opts={careerKey:'engineer',specializationKey:'mechanic'};
    if(node.choiceType==='characteristic')opts.characteristicKey='intelecto';
    if(node.choiceType==='combatSkill')opts.skillKey='armas_distancia_ligeras';
    c=purchaseTalent(c,id,opts).character;
  }
  return c;
}

test('P141 AoR Mañoso: 4 posições mecânicas, equivalência TINK e compra pela árvore real',()=>{
 assert.equal(aorCanonicalTalentName('MAÑOSO'),'TINK');
 assert.ok(aorTalentCoverage().mechanicalPositions>=224);
 let c=hero();c=buy(c);assert.equal(talentRankByOriginal(c,'TINK'),1);
 c=buy(c,1);assert.equal(talentRankByOriginal(c,'TINK'),2);
 assert.equal(tinkererStatus(c).available,2);
 assert.ok(c.xp.available<4000);
 assert.equal(talentRankByOriginal(JSON.parse(JSON.stringify(c)),'TINK'),2);
});

test('P141 Mañoso: +1 hard point por item, limites por rank e instalação real',()=>{
 const c=buy(buy(hero()),1),before=copy(c);
 const originalHP=itemHardPoints(c.weapons[0]);
 const first=applyTinkererHardPoint(c,{kind:'weapons',itemIndex:0});
 assert.equal(itemHardPoints(first.character.weapons[0]),originalHP+1);
 assert.equal(itemAvailableHardPoints(first.character.weapons[0]),originalHP+1);
 assert.equal(canInstallAttachment(first.character.weapons[0],{hardPointsRequired:originalHP+1}),true);
 assert.equal(canInstallAttachment(c.weapons[0],{hardPointsRequired:originalHP+1}),false);
 assert.equal(first.status.available,1);
 const second=applyTinkererHardPoint(first.character,{kind:'armor',itemIndex:0});
 assert.equal(second.status.available,0);assert.equal(second.status.applied,2);
 assert.throws(()=>applyTinkererHardPoint(second.character,{kind:'weapons',itemIndex:1}),/alocados/);
 assert.throws(()=>applyTinkererHardPoint(first.character,{kind:'weapons',itemIndex:0}),/já recebeu/);
 assert.deepEqual(c,before,'origem permanece imutável');
 assert.equal(itemHardPoints(JSON.parse(JSON.stringify(second.character)).armor[0]),itemHardPoints(c.armor[0])+1);
});

test('P141 Mañoso: tentativas inválidas não concedem bônus nem alteram ficha',()=>{
 const c=hero(),before=copy(c);
 assert.throws(()=>applyTinkererHardPoint(c,{kind:'weapons',itemIndex:0}),/não possui/);
 const trained=buy(c);
 for(const args of [{kind:'weapons',itemIndex:-1},{kind:'spaceships',itemIndex:0},{kind:'weapons',itemIndex:999}])
  assert.throws(()=>applyTinkererHardPoint(trained,args));
 assert.deepEqual(c,before);
 assert.equal(itemHardPoints({hardPoints:3,tinkererHardPointBonus:500}),3,'save adulterado não cria capacidade arbitrária');
});

test('P141 Mañoso: bônus falsificados e acima dos ranks são recusados no inventário',()=>{
 const c=buy(hero());
 const invalid=copy(c);invalid.weapons[0].tinkererHardPointBonus=500;
 assert.throws(()=>tinkererStatus(invalid),/incompatível/);
 const extra=copy(c);extra.weapons[0].tinkererHardPointBonus=1;extra.weapons[1].tinkererHardPointBonus=1;
 assert.throws(()=>tinkererStatus(extra),/incompatível/);
 assert.equal(tinkererStatus(c).available,1);
});

test('P141 Mañoso: controller e reabertura persistem capacidade, inventário e limite',()=>{
 const repo=new Repo(),g=new GameController(repo);
 g.setCharacter(buy(buy(hero()),1));
 const before=g.equipmentOwnedSummary();
 assert.equal(before.tinkerer.available,2);
 const applied=g.applyTinkererHardPoint({kind:'weapons',itemIndex:0});
 assert.equal(applied.status.available,1);
 assert.equal(applied.equipment.weapons[0].hardPoints,before.weapons[0].hardPoints+1);
 const reopened=new GameController(repo);
 assert.equal(reopened.tinkererStatus().available,1);
 assert.equal(reopened.equipmentOwnedSummary().weapons[0].hardPoints,before.weapons[0].hardPoints+1);
 assert.throws(()=>reopened.applyTinkererHardPoint({kind:'weapons',itemIndex:0}),/já recebeu/);
 const after=new GameController(repo);
 assert.equal(after.tinkererStatus().available,1);
});

test('P141 Mañoso: interface Android expõe status e aplicação, preservando erro tipado',()=>{
 const repo=new Repo(),g=new GameController(repo);g.setCharacter(buy(hero()));
 const bridge=new BraseiroUiBridge({repository:repo});
 const status=bridge.tinkererStatus();assert.equal(status.ok,true);assert.equal(status.data.available,1);
 const result=bridge.applyTinkererHardPoint({kind:'weapons',itemIndex:1});
 assert.equal(result.ok,true);assert.equal(result.data.status.available,0);
 const repeat=bridge.applyTinkererHardPoint({kind:'weapons',itemIndex:1});
 assert.equal(repeat.ok,false);assert.equal(repeat.error,'TINKERER_APPLY_FAILED');
 assert.equal(new GameController(repo).tinkererStatus().applied,1);
});
