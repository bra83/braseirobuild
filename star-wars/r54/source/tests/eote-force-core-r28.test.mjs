import test from 'node:test';
import assert from 'node:assert/strict';
import { createEoteCoreCharacter } from '../app/src/main/assets/vtt/js/rules/eoteCharacterCreation.mjs';
import { specializationPurchaseCost, purchaseSpecialization, ownedSpecializationRefs } from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import { treeForCharacter, eligibleTalents, purchaseTalent, talentSkillPoolModifiers, forceTalentSummary, overwhelmEmotionsModifiers } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { FORCE_DIE_FACES, normalizeForceState, forceRating, availableForceRating, rollForceDice, resolveForceResources, purchaseForcePower, eligibleForceUpgrades, purchaseForceUpgrade, commitForceDice, releaseForceDice, forcePowerCapabilities, resolveSenseBasic, resolveInfluenceBasic, influenceEmotionAllowed, moveBasicLimits, moveHurlDamage } from '../app/src/main/assets/vtt/js/rules/force.mjs';
import { createFringeGroup } from '../app/src/main/assets/vtt/js/rules/fringeGroup.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class Mem { constructor(){this.m=new Map();} getItem(k){return this.m.get(k)??null;} setItem(k,v){this.m.set(k,String(v));} removeItem(k){this.m.delete(k);} }
function human(){const c=createEoteCoreCharacter({name:'Kara',speciesKey:'human',careerKey:'smuggler',specializationKey:'pilot',partySize:1,careerFreeSkills:['coordinacion','engano','bajos_fondos','percepcion'],specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',obligationType:'Dívida',obligationBenefits:{}});c.xp.available=1000;c.xp.total=1000;return c;}
function droid(){const c=createEoteCoreCharacter({name:'R4',speciesKey:'droid',careerKey:'technician',specializationKey:'mechanic',partySize:1,careerFreeSkills:['astronavegacion','computadoras','coordinacion','disciplina','borde_exterior','mecanica'],specializationFreeSkills:['mecanica','pelea','pilotar_vehiculo_espacial'],speciesChoiceSkills:[],motivation:'Serviço',obligationType:'Responsabilidade',obligationBenefits:{}});c.xp.available=1000;c.xp.total=1000;return c;}
function exile(){return purchaseSpecialization(human(),'universal','force_sensitive_exile').character;}

test('dado da Força reproduz as 12 faces oficiais do Core',()=>{
  assert.equal(FORCE_DIE_FACES.length,12);
  assert.deepEqual(FORCE_DIE_FACES.slice(0,6).map(x=>[x.light,x.dark]),Array(6).fill([0,1]));
  assert.deepEqual([FORCE_DIE_FACES[6].light,FORCE_DIE_FACES[6].dark],[0,2]);
  assert.deepEqual(FORCE_DIE_FACES.slice(7,9).map(x=>[x.light,x.dark]),Array(2).fill([1,0]));
  assert.deepEqual(FORCE_DIE_FACES.slice(9).map(x=>[x.light,x.dark]),Array(3).fill([2,0]));
});

test('Exilado Sensível à Força é universal, custa como carreira e concede Force Rating 1 sem skills grátis',()=>{
  const c=human(); const quote=specializationPurchaseCost(c,'universal','force_sensitive_exile');
  assert.equal(quote.cost,20); assert.equal(quote.universal,true); assert.equal(quote.inCareer,false); assert.deepEqual(quote.careerSkillsGranted,[]);
  const result=purchaseSpecialization(c,'universal','force_sensitive_exile');
  assert.equal(forceRating(result.character),1); assert.equal(result.character.careerSkills.length,c.careerSkills.length);
  assert.ok(ownedSpecializationRefs(result.character).some(x=>x.specializationKey==='force_sensitive_exile'));
});

test('Droide não pode adquirir Exilado Sensível à Força',()=>{
  assert.throws(()=>specializationPurchaseCost(droid(),'universal','force_sensitive_exile'),/Droides não podem/i);
});

test('árvore universal do Exilado tem 20 nós e talentos de primeira linha acessíveis',()=>{
  const c=exile(); const tree=treeForCharacter(c,{careerKey:'universal',specializationKey:'force_sensitive_exile'});
  assert.equal(tree.nodes.length,20); assert.equal(tree.nodes.filter(n=>n.row===1).length,4);
  const first=eligibleTalents(c,{careerKey:'universal',specializationKey:'force_sensitive_exile'}).filter(n=>n.row===1);
  assert.equal(first.length,4); assert.ok(first.every(n=>n.unlocked));
});

test('Perspicácia torna Percepção e Disciplina perícias de carreira sem dar ranks',()=>{
  let c=exile(); const before=[c.skills.percepcao,c.skills.disciplina];
  c=purchaseTalent(c,'universal:force_sensitive_exile:r1c2',{careerKey:'universal',specializationKey:'force_sensitive_exile'}).character;
  assert.ok(c.careerSkills.includes('percepcao')); assert.ok(c.careerSkills.includes('disciplina'));
  assert.deepEqual([c.skills.percepcao,c.skills.disciplina],before);
});

test('Sentidos e Reações sobrenaturais alimentam modificadores de perícia',()=>{
  let c=exile();
  c=purchaseTalent(c,'universal:force_sensitive_exile:r1c1',{careerKey:'universal',specializationKey:'force_sensitive_exile'}).character;
  c=purchaseTalent(c,'universal:force_sensitive_exile:r1c4',{careerKey:'universal',specializationKey:'force_sensitive_exile'}).character;
  assert.equal(talentSkillPoolModifiers(c,'percepcao').boost,1);
  assert.equal(talentSkillPoolModifiers(c,'alerta').boost,1);
});

test('rolagem usa Force Rating disponível e dados comprometidos reduzem a quantidade rolada',()=>{
  let c=exile(); c={...c,force:{...normalizeForceState(c),rating:2}};
  c=commitForceDice(c,{count:1,effectId:'sense-defense'}).character;
  assert.equal(availableForceRating(c),1);
  const roll=rollForceDice(c,{rng:()=>11.5/12}); assert.equal(roll.dice,1); assert.equal(roll.light,2); assert.equal(roll.dark,0);
  c=releaseForceDice(c,'sense-defense').character; assert.equal(availableForceRating(c),2);
});

test('usuário luminoso pode converter resultados sombrios com um Destino e Tensão por ponto usado',()=>{
  const c=exile(); const g=createFringeGroup(); g.destiny={light:2,dark:1};
  const r=resolveForceResources(c,g,{light:1,dark:2},{useOppositePoints:2});
  assert.equal(r.points,3); assert.equal(r.strain,2); assert.equal(r.character.derived.strain.current,c.derived.strain.current+2);
  assert.deepEqual(r.group.destiny,{light:1,dark:2});
});

test('sem Destino luminoso não é permitido usar resultados sombrios',()=>{
  const g=createFringeGroup(); g.destiny={light:0,dark:3};
  assert.throws(()=>resolveForceResources(exile(),g,{light:0,dark:1},{useOppositePoints:1}),/Sem Ponto de Destino luminoso/i);
});

test('Sense, Influence e Move custam 10 PE e exigem Force Rating',()=>{
  for(const key of ['sense','influence','move']){
    const base=exile(); const before=base.xp.available; const bought=purchaseForcePower(base,key);
    assert.equal(bought.cost,10); assert.equal(bought.character.xp.available,before-10); assert.equal(forcePowerCapabilities(bought.character,key).purchased,true);
  }
  assert.throws(()=>purchaseForcePower(human(),'sense'),/Force Rating 1/i);
});

test('aprimoramentos da Força respeitam conexões e custos da árvore',()=>{
  let c=purchaseForcePower(exile(),'sense').character;
  assert.ok(eligibleForceUpgrades(c,'sense').find(x=>x.id==='control_defense'&&x.unlocked));
  assert.ok(eligibleForceUpgrades(c,'sense').find(x=>x.id==='control_thoughts'&&x.unlocked));
  assert.throws(()=>purchaseForceUpgrade(c,'sense','strength'),/não está conectado/i);
  c=purchaseForceUpgrade(c,'sense','control_defense').character;
  c=purchaseForceUpgrade(c,'sense','duration').character;
  c=purchaseForceUpgrade(c,'sense','strength').character;
  const caps=forcePowerCapabilities(c,'sense'); assert.equal(caps.defense,true); assert.equal(caps.triggersPerRound,2); assert.equal(caps.ongoingUpgradeCount,2);
});

test('Influência básica causa 1 Tensão por ponto ou 2 com Strength',()=>{
  let c=purchaseForcePower(exile(),'influence').character;
  assert.equal(resolveInfluenceBasic(c,{activations:2}).totalStrain,2);
  c=purchaseForceUpgrade(c,'influence','control_mind').character;
  c=purchaseForceUpgrade(c,'influence','strength').character;
  assert.equal(resolveInfluenceBasic(c,{activations:2}).totalStrain,4);
});

test('regra especial de Influência separa emoções positivas/negativas por lado',()=>{
  assert.equal(influenceEmotionAllowed({side:'light',emotion:'positive'}),true);
  assert.equal(influenceEmotionAllowed({side:'light',emotion:'negative'}),false);
  assert.equal(influenceEmotionAllowed({side:'dark',emotion:'negative'}),true);
  assert.equal(influenceEmotionAllowed({side:'dark',emotion:'positive'}),false);
  assert.equal(influenceEmotionAllowed({side:'dark',emotion:'neutral'}),true);
});

test('Mover com Control de arremesso calcula dano oficial por silhueta',()=>{
  let c=purchaseForcePower(exile(),'move').character;
  c=purchaseForceUpgrade(c,'move','range_1a').character;
  c=purchaseForceUpgrade(c,'move','control_hurl').character;
  assert.equal(moveHurlDamage(c,0),5); assert.equal(moveHurlDamage(c,1),10); assert.equal(moveHurlDamage(c,4),40);
});

test('árvore do Exilado respeita a malha oficial e não cria atalhos sob Insight/Forager',()=>{
  let c=exile();
  c=purchaseTalent(c,'universal:force_sensitive_exile:r1c2',{careerKey:'universal',specializationKey:'force_sensitive_exile'}).character;
  const afterInsight=eligibleTalents(c,{careerKey:'universal',specializationKey:'force_sensitive_exile'});
  assert.equal(afterInsight.find(n=>n.id==='universal:force_sensitive_exile:r2c2').unlocked,false);
  c=exile();
  for(const id of ['r1c1','r2c1','r3c1','r4c1','r4c2','r3c2','r2c2']) c=purchaseTalent(c,`universal:force_sensitive_exile:${id}`,{careerKey:'universal',specializationKey:'force_sensitive_exile'}).character;
  assert.ok(c.talentPurchases.some(p=>p.nodeId==='universal:force_sensitive_exile:r2c2'));
});

test('Sentir calcula alcance, magnitude e pensamentos sem confundir gasto de pontos',()=>{
  let c=purchaseForcePower(exile(),'sense').character;
  assert.deepEqual(resolveSenseBasic(c,{mode:'living'}),{mode:'living',baseRange:'short',rangeBandIncrease:0,maxTargets:1,forcePointCost:1});
  assert.throws(()=>resolveSenseBasic(c,{mode:'thoughts'}),/pensamentos superficiais/i);
  c=purchaseForceUpgrade(c,'sense','control_thoughts').character;
  c=purchaseForceUpgrade(c,'sense','range_1').character;
  c=purchaseForceUpgrade(c,'sense','magnitude_1').character;
  const r=resolveSenseBasic(c,{mode:'thoughts',rangeActivations:2,magnitudeActivations:1});
  assert.equal(r.rangeBandIncrease,2); assert.equal(r.maxTargets,2); assert.equal(r.forcePointCost,4);
});

test('Mover separa ativações de Strength, Range e Magnitude conforme o Core',()=>{
  let c=purchaseForcePower(exile(),'move').character;
  c=purchaseForceUpgrade(c,'move','strength_1').character;
  c=purchaseForceUpgrade(c,'move','range_1a').character;
  c=purchaseForceUpgrade(c,'move','magnitude_1').character;
  assert.deepEqual(moveBasicLimits(c,{strengthActivations:2,rangeActivations:1,magnitudeActivations:2}),{maxSilhouette:2,rangeBandIncrease:1,maxTargets:3,forcePointCost:6});
});

test('Sobrecarregar Emoções usa lados diferentes em Carisma versus Coerção/Enganação',()=>{
  let c=exile();
  for(const id of ['r1c1','r2c1','r3c1','r4c1','r4c2','r3c2','r2c2']) c=purchaseTalent(c,`universal:force_sensitive_exile:${id}`,{careerKey:'universal',specializationKey:'force_sensitive_exile'}).character;
  assert.deepEqual(overwhelmEmotionsModifiers(c,'carisma',{light:2,dark:3}),{available:true,forceDice:1,success:2,threat:1});
  assert.deepEqual(overwhelmEmotionsModifiers(c,'coaccion',{light:3,dark:2}),{available:true,forceDice:1,success:2,threat:1});
});

test('controller persiste compra universal, poderes e consumo de Destino',()=>{
  const mem=new Mem(); const game=new GameController(new LocalStorageRepository(mem,'force-r28')); game.setCharacter(human());
  game.state.player.character.xp.available=1000; game.repository.save(game.state);
  game.setGroupDestiny({light:2,dark:0});
  game.buySpecialization('universal','force_sensitive_exile'); game.buyForcePower('sense');
  assert.equal(game.forceStatus().sensitive,true); assert.equal(game.forceStatus().powers.sense.purchased,true);
  const reopened=new GameController(new LocalStorageRepository(mem,'force-r28')); assert.equal(reopened.forceStatus().force.rating,1);
});
