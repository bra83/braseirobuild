import test from 'node:test';
import assert from 'node:assert/strict';
import { createEoteCoreCharacter, EOTE_CAREERS } from '../app/src/main/assets/vtt/js/rules/eoteCharacterCreation.mjs';
import { ownedSpecializationRefs, specializationPurchaseCost, purchaseSpecialization } from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import { treeForCharacter, eligibleTalents, purchaseTalent } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class Mem { constructor(){ this.m=new Map(); } getItem(k){ return this.m.get(k) ?? null; } setItem(k,v){ this.m.set(k,String(v)); } removeItem(k){ this.m.delete(k); } }

function baseCharacter(){
  const c=createEoteCoreCharacter({
    name:'Nira', speciesKey:'human', careerKey:'smuggler', specializationKey:'pilot', partySize:1,
    careerFreeSkills:['coordinacion','engano','bajos_fondos','percepcion'],
    specializationFreeSkills:['astronavegacion','artilleria'], speciesChoiceSkills:['atletismo','medicina'],
    motivation:'Pagar a dívida.', obligationType:'Dívida', obligationBenefits:{}
  });
  c.xp.available=500;
  c.xp.total=Math.max(c.xp.total,500);
  return c;
}

test('segunda especialização dentro da carreira custa 20 PE; fora da carreira custa 30 PE',()=>{
  const c=baseCharacter();
  const inCareer=specializationPurchaseCost(c,'smuggler','scoundrel');
  const outCareer=specializationPurchaseCost(c,'bounty_hunter','assassin');
  assert.equal(inCareer.cost,20); assert.equal(inCareer.inCareer,true); assert.equal(inCareer.totalAfterPurchase,2);
  assert.equal(outCareer.cost,30); assert.equal(outCareer.inCareer,false); assert.equal(outCareer.totalAfterPurchase,2);
});

test('terceira especialização usa o total após a compra e mantém sobretaxa fora da carreira original',()=>{
  const c=purchaseSpecialization(baseCharacter(),'smuggler','scoundrel').character;
  assert.equal(specializationPurchaseCost(c,'smuggler','thief').cost,30);
  assert.equal(specializationPurchaseCost(c,'bounty_hunter','assassin').cost,40);
});

test('compra adiciona especialização e perícias de carreira sem conceder ranks gratuitos',()=>{
  const before=baseCharacter();
  const skill='frialdad';
  assert.equal(before.skills[skill],0);
  assert.equal(before.careerSkills.includes(skill),false);
  const result=purchaseSpecialization(before,'smuggler','scoundrel');
  const c=result.character;
  assert.equal(c.xp.available,before.xp.available-20);
  assert.ok(c.specializations.includes('Patife'));
  for(const careerSkill of EOTE_CAREERS.smuggler.specializations.scoundrel.skills) assert.ok(c.careerSkills.includes(careerSkill));
  assert.equal(c.skills[skill],0);
  assert.deepEqual(c.specializationPurchases.map(x=>[x.careerKey,x.specializationKey,x.cost]),[['smuggler','scoundrel',20]]);
  assert.equal(ownedSpecializationRefs(c).length,2);
});

test('especialização já possuída não pode ser comprada novamente e PE insuficiente falha sem mutar',()=>{
  const c=baseCharacter();
  assert.throws(()=>purchaseSpecialization(c,'smuggler','pilot'),/já possui/i);
  c.xp.available=19;
  const snapshot=structuredClone(c);
  assert.throws(()=>purchaseSpecialization(c,'smuggler','scoundrel'),/PE insuficientes/i);
  assert.deepEqual(c,snapshot);
});

test('árvore de talentos da especialização comprada fica acessível e a inicial continua intacta',()=>{
  let c=purchaseSpecialization(baseCharacter(),'smuggler','scoundrel').character;
  const initial=treeForCharacter(c);
  const acquired=treeForCharacter(c,{careerKey:'smuggler',specializationKey:'scoundrel'});
  assert.equal(initial.specializationKey,'pilot');
  assert.equal(acquired.specializationKey,'scoundrel');
  assert.equal(acquired.nodes.length,20);
  const option=eligibleTalents(c,{careerKey:'smuggler',specializationKey:'scoundrel'}).find(x=>x.row===1 && x.unlocked);
  assert.ok(option);
  const bought=purchaseTalent(c,option.id);
  c=bought.character;
  assert.equal(c.talentPurchases.at(-1).specializationKey,'scoundrel');
  assert.ok(c.talents.includes(option.name));
});

test('não é permitido acessar árvore de especialização que o personagem não possui',()=>{
  const c=baseCharacter();
  assert.equal(treeForCharacter(c,{careerKey:'bounty_hunter',specializationKey:'assassin'}),null);
  const nodeId='bounty_hunter:assassin:r1c1';
  assert.throws(()=>purchaseTalent(c,nodeId),/especialização possuída/i);
});

test('controller compra e persiste especialização adicional com custo e career skills corretos',()=>{
  const storage=new Mem(); const repo=new LocalStorageRepository(storage,'spec-progress'); const game=new GameController(repo);
  game.setCharacter(baseCharacter());
  const quote=game.specializationPurchaseCost('bounty_hunter','assassin');
  assert.equal(quote.cost,30);
  const result=game.buySpecialization('bounty_hunter','assassin');
  assert.equal(result.cost,30);
  const reopened=new GameController(new LocalStorageRepository(storage,'spec-progress'));
  assert.equal(reopened.ownedSpecializations().length,2);
  assert.ok(reopened.state.player.character.careerSkills.includes('sigilo'));
  assert.equal(reopened.talentTree({careerKey:'bounty_hunter',specializationKey:'assassin'}).nodes.length,20);
});


test('RAG fundamenta compra de especialização adicional no Core EotE real',()=>{
  const game=new GameController(new LocalStorageRepository(new Mem(),'spec-rag'));
  const result=game.consultRules('compra de especialização adicional custo segunda especialização',{limit:8});
  assert.equal(result.retrieval?.grounded ?? result.grounded,true);
  const rule=result.results.find(row=>row.id==='SW-EOTE-SPECIALIZATION-PURCHASE-001');
  assert.ok(rule);
  assert.match(rule.source,/Edge of the Empire/i);
  assert.equal(result.results.some(row=>String(row.id).startsWith('SW-AOR-')),false);
});
