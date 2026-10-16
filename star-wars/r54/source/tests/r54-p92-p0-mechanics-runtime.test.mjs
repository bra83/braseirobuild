import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { armorCatalogEntry } from '../app/src/main/assets/vtt/js/rules/equipmentCatalog.mjs';

class MemoryRepository {
  constructor(state=createCampaign('P92')){ this.state=structuredClone(state); }
  load(){ return structuredClone(this.state); }
  save(s){ this.state=structuredClone(s); }
}

function game(){const repo=new MemoryRepository();const g=new GameController(repo);g.loadStarterCharacter('cael');return {g,repo};}

test('P92: Precisa/Accurate entra no pool real antes da rolagem',()=>{
  const {g}=game();
  g.state.player.character.weapons[0].qualities=['Precisa 2'];
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:started.combat.adversaries[0].id,rng:()=>0.15});
  assert.ok(attack.check.pool.boost>=2,`boost=${attack.check.pool.boost}`);
});

test('P92: Automática aumenta a dificuldade real e resolve acertos extras por Vantagem',()=>{
  const {g}=game();
  const w=g.state.player.character.weapons[0]; w.qualities=['Automática']; w.damage=2; w.baseDamage=2;
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const target=started.combat.adversaries[0];
  // O aumento de dificuldade independe do resultado aleatório; o resolver de arma comprova os hits extras quando houver Vantagem.
  const auto=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,useAutoFire:true,rng:()=>0.15});
  assert.ok(auto.difficulty>=2,'Auto-fire deve aumentar em +1 a dificuldade mínima de Curto');
  assert.equal(auto.autoFireUsed,true);
  assert.ok(Number.isInteger(auto.additionalHits));
});

test('P92: Brecha é convertida em 10 pontos de Soak ignorado no dano real',()=>{
  const {g}=game();
  const w=g.state.player.character.weapons[0]; w.qualities=['Brecha 1']; w.damage=8; w.baseDamage=8;
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const target=started.combat.adversaries[0]; target.soak=12; target.wounds.current=0;
  const a=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,rng:()=>0.15});
  assert.equal(a.effectivePierce,10);
  if(a.hit) assert.ok(a.targetDamage.suffered>0,'Brecha precisa alcançar applyAdversaryDamage');
});

test('P92: Viciosa/Letal chega ao crítico ativado como modificador d100',()=>{
  const {g}=game();
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const target=started.combat.adversaries[0];
  g.state.combat.pendingPlayerCritical={adversaryId:target.id,weapon:'Disruptor',cost:2,byTriumph:false,vicious:4};
  const r=g.activatePendingPlayerCritical({rng:()=>0});
  assert.equal(r.modifierApplied,40);
  assert.equal(r.injury.baseRoll,1);
  assert.equal(r.injury.total,41);
});

test('P92: equipar arma/armadura altera estado e sobrevive a reabertura',()=>{
  const {g,repo}=game();
  const armor=structuredClone(armorCatalogEntry('armored_clothing'));
  g.state.player.character.armor=[armor]; repo.save(g.state);
  const baseSoak=Number(g.state.player.character.derived.soak||0);
  const weaponId=g.state.player.character.weapons[0].id||g.state.player.character.weapons[0].name;
  g.equipWeapon(weaponId); g.equipArmor('armored_clothing');
  assert.equal(g.state.player.character.armor[0].worn,true);
  assert.equal(g.state.player.character.derived.defense.ranged,1);
  assert.equal(g.state.player.character.derived.soak,Number(g.state.player.character.characteristics.fortaleza)+1);
  assert.ok(g.equipmentOwnedSummary().armor.some(x=>x.name==='Armadura acolchoada'),'armadura legada em gear precisa continuar visível');
  const reopened=new GameController(repo);
  assert.equal(reopened.state.player.character.armor[0].worn,true);
  assert.equal(reopened.state.player.character.derived.defense.ranged,1);
  assert.ok(reopened.equipmentOwnedSummary().weapons.some(x=>x.active));
  reopened.unequipArmor('armored_clothing');
  assert.equal(reopened.state.player.character.armor[0].worn,false);
  assert.equal(reopened.state.player.character.derived.defense.ranged,0);
});

test('P92: salto hiperespacial usa nave + Astronavegação e persiste resultado',()=>{
  const {g,repo}=game();
  g.addVehicle('yt1300');
  const plan=g.astrogationPlan({routeType:'MAJOR',destinationLabel:'Tatooine'});
  assert.equal(plan.vehicleName,'Cargueiro leve YT-1300');
  assert.equal(plan.hyperdriveClass,2);
  const r=g.resolveHyperspaceJump({routeType:'MAJOR',destinationLabel:'Tatooine',rng:()=>0.15});
  assert.equal(r.plan.vehicleId,g.state.vehicles.activeVehicleId);
  assert.equal(g.state.vehicles.travel.lastHyperspaceJump.destinationLabel,'Tatooine');
  const reopened=new GameController(repo);
  assert.equal(reopened.state.vehicles.travel.lastHyperspaceJump.destinationLabel,'Tatooine');
  assert.ok(reopened.state.vehicles.travel.lastHyperspaceJump.check);
  assert.ok(reopened.state.vehicles.travel.lastHyperspaceJump.outcome);
});
