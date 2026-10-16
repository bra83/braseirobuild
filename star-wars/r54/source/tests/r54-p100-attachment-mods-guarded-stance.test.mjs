import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { grantCatalogAsset } from '../app/src/main/assets/vtt/js/rules/economy.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { effectiveWeaponWithAttachments, attachmentModStatus } from '../app/src/main/assets/vtt/js/rules/attachments.mjs';
import { weaponAttackModifiers, activeWeaponQualities } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';

class MemoryRepository{constructor(state=createCampaign('P100')){this.state=structuredClone(state)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function modGame({attachment='rapid_recharge_xciter'}={}){
  const repo=new MemoryRepository(); let g=new GameController(repo); g.loadStarterCharacter('cael');
  let s=g.state; s.player.character.credits=10000; s.player.character.characteristics.intelecto=5; s.player.character.skills.mecanica=5;
  s=grantCatalogAsset(s,{kind:'weapon',itemId:'blaster_rifle',method:'reward'}).state;
  s=grantCatalogAsset(s,{kind:'attachment',itemId:attachment,method:'reward'}).state;
  s=grantCatalogAsset(s,{kind:'gear',itemId:'tool_kit',method:'reward'}).state;
  repo.save(s); g=new GameController(repo);
  const sum=g.attachmentSummary(), w=sum.weapons.find(x=>x.sourceCatalogId==='blaster_rifle'), a=sum.owned.find(x=>x.catalogId===attachment);
  g.installOwnedAttachment(a.assetId,w.id);
  return {g,repo,weaponId:w.id,assetId:a.assetId};
}
function combatGame(){
  const repo=new MemoryRepository();const g=new GameController(repo);g.loadStarterCharacter('tendaar');
  let c=structuredClone(g.state.player.character);c.characteristics.agilidad=4;c.characteristics.fortaleza=4;c.skills.armas_distancia_ligeras=4;
  c.weapons=[{id:'p100-pistol',name:'Pistola P100',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Curto',qualities:[],active:true,equipped:true}];c.activeWeaponId='p100-pistol';
  g.commit('CHARACTER_UPDATED',{character:c});
  const target=createAdversary('imperial_officer');target.range='Curto';g.beginEncounter({adversaries:[target],rng:()=>0});
  return g;
}

test('P100: primeira modificação custa 100 créditos, exige Mecânica Difícil e altera o attachment instalado',()=>{
  const {g,assetId}=modGame(); const before=g.state.player.character.credits;
  const r=g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25});
  assert.equal(r.success,true); assert.equal(r.attempt.cost,100); assert.equal(r.attempt.difficulty,3); assert.equal(r.creditsRemaining,before-100);
  const status=g.attachmentSummary().owned.find(x=>x.assetId===assetId).modStatus.find(x=>x.id==='reduce_inaccurate');
  assert.equal(status.installed,1); assert.equal(status.remaining,1); assert.equal(status.difficulty,4);
  const w=g.state.player.character.weapons.find(x=>x.sourceCatalogId==='blaster_rifle'),eff=effectiveWeaponWithAttachments(w),mods=weaponAttackModifiers(eff,{character:g.state.player.character});
  assert.equal(mods.inaccurate,2);
});

test('P100: segunda modificação fica mais difícil, consome o último slot e reduz Imprecisa novamente',()=>{
  const {g,assetId}=modGame();g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25});
  const r=g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25});
  assert.equal(r.success,true);assert.equal(r.attempt.difficulty,4);
  const status=g.attachmentSummary().owned.find(x=>x.assetId===assetId).modStatus.find(x=>x.id==='reduce_inaccurate');
  assert.equal(status.installed,2);assert.equal(status.remaining,0);
  assert.throws(()=>g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25}),/não possui tentativas/i);
  const w=g.state.player.character.weapons.find(x=>x.sourceCatalogId==='blaster_rifle'),eff=effectiveWeaponWithAttachments(w);
  assert.equal(weaponAttackModifiers(eff,{character:g.state.player.character}).inaccurate,1);
});

test('P100: falha de Mod custa créditos, consome uma opção e não concede o efeito',()=>{
  const {g,assetId}=modGame({attachment:'spread_barrel'}); const before=g.state.player.character.credits;
  const r=g.attemptAttachmentMod(assetId,'blast_plus_1',{rng:()=>0});
  assert.equal(r.success,false); assert.equal(r.creditsRemaining,before-100);
  const status=g.attachmentSummary().owned.find(x=>x.assetId===assetId).modStatus.find(x=>x.id==='blast_plus_1');
  assert.equal(status.failed,1);assert.equal(status.installed,0);assert.equal(status.remaining,1);
  const w=g.state.player.character.weapons.find(x=>x.sourceCatalogId==='blaster_rifle'),eff=effectiveWeaponWithAttachments(w);
  const blast=activeWeaponQualities(eff,{hit:true}).find(x=>x.key==='blast');
  assert.equal(blast.rank,4);
});

test('P100: Mod é bloqueado sem kit de ferramentas, com créditos insuficientes e durante combate',()=>{
  const {g,assetId}=modGame();
  let c=structuredClone(g.state.player.character);c.gear=(c.gear??[]).filter(x=>String(x.sourceCatalogId??x.id)!=='tool_kit');g.commit('CHARACTER_UPDATED',{character:c});
  assert.throws(()=>g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25}),/Kit de ferramentas/i);
  c=structuredClone(g.state.player.character);c.gear=[{id:'tool_kit',name:'Kit de ferramentas'}];c.credits=99;g.commit('CHARACTER_UPDATED',{character:c});
  assert.throws(()=>g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25}),/Créditos insuficientes/i);
  c=structuredClone(g.state.player.character);c.credits=1000;g.commit('CHARACTER_UPDATED',{character:c});
  g.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  assert.throws(()=>g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25}),/tempo de oficina/i);
});

test('P100: Mods instalados chegam ao resolveWeaponAttack real',()=>{
  const modded=modGame();modded.g.attemptAttachmentMod(modded.assetId,'reduce_inaccurate',{rng:()=>0.25});modded.g.attemptAttachmentMod(modded.assetId,'reduce_inaccurate',{rng:()=>0.25});
  const control=modGame();
  const run=(g)=>{const weaponIndex=g.state.player.character.weapons.findIndex(x=>x.sourceCatalogId==='blaster_rifle');assert.ok(weaponIndex>=0);const target=createAdversary('imperial_officer');target.range='Médio';g.beginEncounter({adversaries:[target],rng:()=>0});return g.resolveWeaponAttack({weaponIndex,targetAdversaryId:g.state.combat.adversaries[0].id,useAutoFire:true,rng:()=>0.25});};
  const a=run(modded.g),b=run(control.g);
  assert.equal(b.check.pool.setback-a.check.pool.setback,2);
  assert.equal(a.check.pool.difficulty,b.check.pool.difficulty);
});

test('P100: Mods pertencem ao attachment e sobrevivem remoção, reopen e reinstalação',()=>{
  let {g,repo,assetId,weaponId}=modGame();g.attemptAttachmentMod(assetId,'reduce_inaccurate',{rng:()=>0.25});
  g.removeOwnedAttachment(assetId);
  let owned=g.attachmentSummary().owned.find(x=>x.assetId===assetId);assert.equal(owned.state.installed,false);assert.equal(owned.modStatus.find(x=>x.id==='reduce_inaccurate').installed,1);
  g=new GameController(repo);const sum=g.attachmentSummary(),w=sum.weapons.find(x=>x.sourceCatalogId==='blaster_rifle');g.installOwnedAttachment(assetId,w.id??w.sourceCatalogId??w.name);
  const weapon=g.state.player.character.weapons.find(x=>x.sourceCatalogId==='blaster_rifle'),eff=effectiveWeaponWithAttachments(weapon);
  assert.equal(weaponAttackModifiers(eff,{character:g.state.player.character}).inaccurate,2);
  owned=g.attachmentSummary().owned.find(x=>x.assetId===assetId);assert.equal(owned.modStatus.find(x=>x.id==='reduce_inaccurate').installed,1);
});

test('P100: Postura Defensiva consome manobra e adiciona Setback aos próprios testes de combate',()=>{
  const g=combatGame();const r=g.takeGuardedStance({maneuverMethod:'normal'});
  assert.equal(r.combat.playerTurn.maneuversUsed,1);assert.equal(r.strainCost,0);assert.ok(r.guardedStance);
  assert.throws(()=>g.takeGuardedStance(),/já está ativa/i);
  const a=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:g.state.combat.adversaries[0].id,rng:()=>0.25});
  assert.ok(a.check.pool.setback>=1);
});

test('P100: Postura Defensiva adiciona Setback a ataque melee contra o jogador, mas não a ranged',()=>{
  const g=combatGame();g.takeGuardedStance({maneuverMethod:'normal'});
  const melee=createAdversary('stormtrooper');melee.range='Interação';melee.weapons=melee.weapons.filter(w=>w.skill==='armas_cuerpo_a_cuerpo');
  const combat=structuredClone(g.state.combat);combat.adversaries=[melee];combat.round=1;
  const m=g._resolveNpcTurn(combat,g.state.player.character,melee.id,()=>0.25);
  assert.ok(m.summary.attack?.check?.pool?.setback>=1 || m.attack?.check?.pool?.setback>=1);
  const ranged=createAdversary('imperial_officer');ranged.range='Médio';const combat2=structuredClone(g.state.combat);combat2.adversaries=[ranged];combat2.round=1;
  const rr=g._resolveNpcTurn(combat2,g.state.player.character,ranged.id,()=>0.25);
  const mPool=m.summary.attack?.check?.pool??m.attack?.check?.pool;const rPool=rr.summary.attack?.check?.pool??rr.attack?.check?.pool;
  assert.ok(mPool.setback>=rPool.setback+1);
});

test('P100: bridge/contratos/R46 expõem Mods e Postura Defensiva sem mutação bruta',()=>{
  const bridge=fs.readFileSync('app/src/main/assets/vtt/js/core/uiBridge.mjs','utf8'),runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');
  for(const method of ['attemptAttachmentMod','takeGuardedStance'])assert.ok(bridge.includes(`${method}(`),method);
  for(const f of ['gameBridgeClient.ts','shell-r46-run87/gameBridgeClient.ts']){const txt=fs.readFileSync(f,'utf8');for(const method of ['attemptAttachmentMod','takeGuardedStance'])assert.ok(txt.includes(method),`${f}:${method}`)}
  assert.ok(runtime.includes('MODS DE ATTACHMENTS'));assert.ok(runtime.includes('POSTURA DEFENSIVA'));assert.equal(runtime.includes('setGuardedStance'),false);
});
