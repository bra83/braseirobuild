import test from 'node:test';
import assert from 'node:assert/strict';
import { AOR_CAREERS, createAorCoreCharacter } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { ownedSpecializationRefs, specializationPurchaseCost, purchaseSpecialization } from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import { eligibleTalents, purchaseTalent, treeForCharacter } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, publishedAdventureUaeSnapshot, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const clone=structuredClone;
const opts={name:'Tala Nira',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
  careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],
  specializationFreeSkills:['astronavegacion','artilleria'],
  speciesChoiceSkills:['atletismo','medicina'],motivation:'Defender a Rebelião.',dutyType:'Superioridade espacial'};
const make=()=>createAorCoreCharacter(opts);
class Repo {constructor(){this.state=createCampaign('P131');} load(){return clone(this.state);}save(v){this.state=clone(v);}}

test('P131 AoR: segunda especialização da carreira custa 20 PE, concede perícias, abre sua árvore e persiste',()=>{
  const original=make();
  const quote=specializationPurchaseCost(original,'ace','gunner');
  assert.equal(quote.cost,20);
  assert.equal(quote.inCareer,true);
  assert.deepEqual(quote.careerSkillsGranted,AOR_CAREERS.ace.specializations.gunner.skills);
  const result=purchaseSpecialization(original,'ace','gunner');
  assert.deepEqual(original,make());
  assert.equal(result.character.xp.available,original.xp.available-20);
  assert.equal(result.character.specializationPurchases[0].rulesProfile,'AOR_CORE');
  assert.equal(ownedSpecializationRefs(result.character).length,2);
  assert.ok(AOR_CAREERS.ace.specializations.gunner.skills.every(s=>result.character.careerSkills.includes(s)));
  const restored=JSON.parse(JSON.stringify(result.character));
  assert.equal(treeForCharacter(restored,{careerKey:'ace',specializationKey:'gunner'}).nodes.length,20);
  const first=eligibleTalents(restored,{careerKey:'ace',specializationKey:'gunner'}).find(n=>n.row===1);
  const purchased=purchaseTalent(restored,first.id);
  assert.equal(purchased.character.talentPurchases[0].specializationKey,'gunner');
  assert.equal(purchased.character.xp.available,restored.xp.available-first.cost);
  assert.throws(()=>purchaseSpecialization(restored,'ace','gunner'),/já possui/);
});

test('P131 AoR: terceira especialização e fora da carreira têm custo escalonado e ownership isolado',()=>{
  let c=make();
  assert.equal(specializationPurchaseCost(c,'soldier','commando').cost,30);
  c=purchaseSpecialization(c,'ace','gunner').character;
  assert.equal(specializationPurchaseCost(c,'ace','driver').cost,30);
  assert.equal(specializationPurchaseCost(c,'soldier','commando').cost,40);
  c=purchaseSpecialization(c,'soldier','commando').character;
  assert.equal(ownedSpecializationRefs(c).length,3);
  assert.ok(treeForCharacter(c,{careerKey:'soldier',specializationKey:'commando'}));
  assert.equal(treeForCharacter(c,{careerKey:'spy',specializationKey:'slicer'}),null);
  assert.throws(()=>purchaseTalent(c,'spy:slicer:r1c1'),/não pertence/);
  assert.throws(()=>specializationPurchaseCost(c,'smuggler','scoundrel'),/não encontrada/);
  assert.throws(()=>specializationPurchaseCost(c,'universal','force_sensitive_exile'),/não encontrada/);
});

test('P131 AoR: todas as 18 especializações podem ser compradas a partir de personagem de outra árvore',()=>{
  let total=0;
  for(const [career,specs] of Object.entries(AOR_CAREERS)) for(const spec of Object.keys(specs.specializations)) {
    if(career==='ace'&&spec==='pilot')continue;
    const c=make();
    const quote=specializationPurchaseCost(c,career,spec);
    assert.equal(quote.cost,career==='ace'?20:30);
    const next=purchaseSpecialization(c,career,spec).character;
    assert.ok(treeForCharacter(next,{careerKey:career,specializationKey:spec}));
    assert.equal(eligibleTalents(next,{careerKey:career,specializationKey:spec}).filter(n=>n.row===1).length,4);
    total++;
  }
  assert.equal(total,17);
});

test('P131 AoR: controlador e UI bridge compartilham compra persistente de especialização',()=>{
  const repo=new Repo();let game=new GameController(repo);game.setCampaignMode('AOR');game.createAorCharacter(opts);
  const before=game.state.player.character.xp.available;
  const result=game.buySpecialization('ace','gunner');
  assert.equal(result.cost,20);
  assert.equal(game.state.player.character.xp.available,before-20);
  game=new GameController(repo);
  assert.ok(game.ownedSpecializations().some(s=>s.specializationKey==='gunner'));
  const bridge=new BraseiroUiBridge({repository:repo});
  assert.equal(bridge.ownedSpecializations().ok,true);
  assert.ok(bridge.eligibleTalents({careerKey:'ace',specializationKey:'gunner'}).data.some(n=>n.row===1));
  const quote=bridge.specializationPurchaseCost('soldier','commando');
  assert.equal(quote.ok,true);assert.equal(quote.data.cost,40);
  const bought=bridge.buySpecialization('soldier','commando');
  assert.equal(bought.ok,true);
  const reloaded=new GameController(repo);
  assert.ok(reloaded.ownedSpecializations().some(s=>s.careerKey==='soldier'&&s.specializationKey==='commando'));
  assert.equal(reloaded.state.player.character.xp.available,before-20-40);
});

for(const {id} of publishedAdventureCatalog()) {
  test(`P131 ${id}: checkpoint corrupto é rejeitado antes da migração sem alterar mundo`,()=>{
    const adapter=getPublishedAdventureAdapter(id);
    const started=adapter.start({}).world;
    const module=publishedAdventureRuntimeModule(adapter.meta);
    const state=started.publishedAdventureRuntime;
    assert.equal(validatePublishedAdventureRuntimeState(module,state,started.publishedAdventure),true);
    const corruptions=[
      s=>{s.visitedNodeIds.push('foreign-node');},
      s=>{s.triggersFired['fake-trigger']=-2;},
      s=>{s.canonicalEvents[0].turn=0;},
      s=>{s.canonicalEvents[0].consequenceIds='invalid';},
      s=>{s.status='UNRECOGNIZED';},
      s=>{s.objectives['episode-1'].status='COMPLETED';s.objectives['episode-1'].progress=1;},
      s=>{s.clocks[Object.keys(s.clocks)[0]].status='UNKNOWN';},
      s=>{s.eventSequence=0;}
    ];
    for(const mutate of corruptions){
      const bad=clone(started);mutate(bad.publishedAdventureRuntime);
      const before=clone(bad);
      assert.throws(()=>publishedAdventureUaeSnapshot(bad,adapter),undefined,id);
      assert.deepEqual(bad,before);
    }
    assert.equal(publishedAdventureUaeSnapshot(started,adapter).adventureId,id);
  });
}
