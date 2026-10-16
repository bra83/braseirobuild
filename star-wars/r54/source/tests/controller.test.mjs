import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('personagem inicial entra no estado persistente e a perícia usa seus valores',()=>{
  const storage=new MemoryStorage(); const repo=new LocalStorageRepository(storage,'state'); const game=new GameController(repo);
  game.loadStarterCharacter('zal');
  assert.equal(game.state.player.character.name,'Zal');
  const result=game.resolveSkillCheck({skillKey:'pilotar',difficulty:0});
  assert.deepEqual(result.pool,{ability:2,proficiency:2,difficulty:0,challenge:0,boost:0,setback:0});
  const reopened=new GameController(new LocalStorageRepository(storage,'state'));
  assert.equal(reopened.state.player.character.name,'Zal');
});

test('combate consome ação no ataque e segunda manobra por Tensão persiste na ficha',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'combat'));
  game.loadStarterCharacter('zal');
  game.beginCombat();
  game.useManeuver({method:'normal',label:'Mover'});
  game.useManeuver({method:'strain',label:'Cobertura'});
  assert.equal(game.state.player.character.derived.strain.current,2);
  const attack=game.resolveWeaponAttack({weaponIndex:0,difficulty:0,rng:()=>0.2});
  assert.equal(game.state.combat.playerTurn.actionUsed,true);
  assert.equal(typeof attack.hit,'boolean');
  const reopened=new GameController(new LocalStorageRepository(storage,'combat'));
  assert.equal(reopened.state.player.character.derived.strain.current,2);
  assert.equal(reopened.state.combat.playerTurn.maneuversUsed,2);
});

test('dificuldade automática por alcance, Mirar e crítico são aplicados no ataque',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'auto-combat'));
  game.loadStarterCharacter('zal');
  game.beginCombat();
  game.aim();
  game.applyPlayerCritical('Teste crítico', {roll:21});
  const attack=game.resolveWeaponAttack({weaponIndex:0,targetRange:'Médio',rng:()=>0.2});
  assert.equal(attack.difficulty,2);
  assert.equal(attack.check.pool.boost,1);
  assert.equal(attack.check.pool.setback,1);
  assert.equal(game.state.combat.pendingAttackBoost,0);
  const pending=game.state.player.character.criticalInjuries.find(x=>x.code==='off_balance');
  assert.equal(pending.pending,false);
});

test('não rola uma segunda ação no mesmo turno nem permite agir incapacitado',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'actions'));
  game.loadStarterCharacter('arkhan');
  game.beginCombat();
  game.resolveSkillCheck({skillKey:'liderazgo',difficulty:0,rng:()=>0.2});
  assert.throws(()=>game.resolveSkillCheck({skillKey:'liderazgo',difficulty:0,rng:()=>0.2}),/ação deste turno já foi usada/i);
  game.finishCombat();
  game.applyPlayerCritical('Crítico mortal', {roll:100,modifier:51});
  assert.throws(()=>game.resolveSkillCheck({skillKey:'liderazgo',difficulty:0}),/incapacitado/);
});


test('consulta de regras e contexto do Mestre persistem sem narrar livremente',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'master'));
  game.loadStarterCharacter('jin-rio');
  const query=game.consultRules('segunda manobra por tensão');
  assert.equal(query.grounded,true);
  assert.equal(game.state.master.lastRuleQuery,'segunda manobra por tensão');
  const intent=game.submitIntent('Eu avanço até a cobertura e observo os soldados');
  assert.equal(intent.master.canNarrateFreely,false);
  assert.equal(game.state.master.lastContext.playerName,'Jin-Rio');
  const reopened=new GameController(new LocalStorageRepository(storage,'master'));
  assert.equal(reopened.state.master.lastRuleResults.length>0,true);
});

test('personagem personalizado EotE entra no mesmo estado persistente',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'custom-aor'));
  game.createCustomCharacter({
    name:'Nira Valen',speciesKey:'human',careerKey:'smuggler',specializationKey:'pilot',partySize:1,
    careerFreeSkills:['coordinacion','engano','bajos_fondos','percepcion'],
    specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],
    motivation:'Libertar mundos ocupados pelo Império.',obligationType:'Dívida',characteristics:{agilidad:3}
  });
  assert.equal(game.state.player.character.name,'Nira Valen');
  assert.equal(game.state.player.character.source.type,'custom_eote_core');
  const reopened=new GameController(new LocalStorageRepository(storage,'custom-aor'));
  assert.equal(reopened.state.player.character.name,'Nira Valen');
  assert.equal(reopened.state.player.character.skills.astronavegacion,1);
});

test('Base, recursos e Obligation persistem ao reabrir a campanha',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'group'));
  game.updateGroupBase({name:'Eco',location:'Dantooine',description:'Refúgio oculto.'});
  game.updateGroupShip({name:'Andarilha'}); game.setGroupDestiny({light:3,dark:1});
  game.setGroupList('resources',['Hangar']); game.setGroupList('contacts',['Capitã Varo']);
  game.addGroupObligation({value:10,member:'Nira',type:'Dívida'});
  const reopened=new GameController(new LocalStorageRepository(storage,'group'));
  assert.equal(reopened.state.group.base.location,'Dantooine'); assert.equal(reopened.state.group.ship.name,'Andarilha');
  assert.equal(reopened.groupSummary().obligationTotal,10); assert.deepEqual(reopened.state.group.destiny,{light:3,dark:1});
});

test('compra de talento de primeira linha persiste e desconta PE uma única vez',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'talent-buy'));
  game.createCustomCharacter({name:'Nira',speciesKey:'human',careerKey:'smuggler',specializationKey:'pilot',careerFreeSkills:['coordinacion','engano','bajos_fondos','percepcion'],specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],motivation:'Libertar a galáxia.',obligationType:'Dívida'});
  const option=game.availableFirstRowTalents()[0], before=game.state.player.character.xp.available;
  game.buyFirstRowTalent(option.id); assert.equal(game.state.player.character.xp.available,before-5);
  const reopened=new GameController(new LocalStorageRepository(storage,'talent-buy'));
  assert.equal(reopened.state.player.character.talentPurchases.length,1);
  assert.throws(()=>reopened.buyFirstRowTalent(option.id),/já foi adquirido/i);
});

test('compra genérica da árvore completa desbloqueia conexão e persiste linha inferior',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'talent-tree-full'));
  game.createCustomCharacter({name:'Nira',speciesKey:'human',careerKey:'smuggler',specializationKey:'pilot',careerFreeSkills:['coordinacion','engano','bajos_fondos','percepcion'],specializationFreeSkills:['astronavegacion','artilleria'],speciesChoiceSkills:['atletismo','medicina'],motivation:'Libertar a galáxia.',obligationType:'Dívida'});
  assert.equal(game.availableTalents().filter(t=>t.unlocked).length,4);
  game.buyTalent('smuggler:pilot:r1c1');
  assert.equal(game.availableTalents().find(t=>t.id==='smuggler:pilot:r2c1').unlocked,true);
  game.buyTalent('smuggler:pilot:r2c1');
  const reopened=new GameController(new LocalStorageRepository(storage,'talent-tree-full'));
  assert.deepEqual(reopened.state.player.character.talentPurchases.map(t=>t.nodeId),['smuggler:pilot:r1c1','smuggler:pilot:r2c1']);
});

test('propor aquisição de arma consulta o Mestre sem descontar créditos',()=>{
  const storage=new MemoryStorage(); const game=new GameController(new LocalStorageRepository(storage,'equipment-proposal')); game.loadStarterCharacter('zal');
  const before=game.state.player.character.credits; const result=game.proposeWeaponAcquisition('blaster_pistol');
  assert.equal(game.state.player.character.credits,before); assert.match(game.state.events.at(-2)?.payload?.text ?? game.state.events.at(-1)?.payload?.text ?? '',/Pistola blaster|adquirir/i); assert.ok(result.master);
});
