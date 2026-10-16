/** P233: SWE09 Kashyyyk vertigo on additional REAL player actions. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {downgradeVertigoPositivePool,validateJewelHoloVertigoConsumption} from '../app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P233');this.saves=0;this.fail=false;}load(){return clone(this.state);}save(s){if(this.fail)throw Error('P233 save failure');this.state=clone(s);this.saves++;}}
function fixture({stim=false}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p233-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=5;ch.skills.medicina=4;ch.characteristics.voluntad=6;ch.skills.disciplina=5;ch.skills.mecanica=5;ch.skills.frialdad=5;
 ch.derived.wounds.current=2;
 ch.gear=[...(ch.gear??[]),{id:'medpac',name:'Medpac',quantity:1},{id:'stimpack_injector',name:'Injetor',quantity:1}];
 if(stim){ch.creation={...(ch.creation??{}),rulesProfile:'AOR_CORE'};const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
  const node=nodes.find(n=>n.originalName==='APLICACIÓN DE ESTIMULANTES');assert.ok(node);
  ch.talents=[...(ch.talents??[]),node.name];ch.talentPurchases=[...(ch.talentPurchases??[]),{nodeId:node.id,name:node.name,originalName:node.originalName,source:node.source}];}
 g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'p233-bomb-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'p233-bomb-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 const w=clone(g.state.world),p=w.publishedAdventure;p.encounterHistory=[...(p.encounterHistory??[]),{encounterId:'arend-shen-four-goons',outcome:'adversaries_defeated',nodeId:p.currentNodeId}];p.activeEncounterId=null;
 g.commit('WORLD_UPDATED',{world:w});
 g.beginJewelHoloParkWingGuard({actionId:'p233-holo-enter',gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,rng:()=>.86});
 const defender=clone(g.state.player.character);defender.derived.wounds.current=0;defender.derived.strain.current=0;defender.combatEffects={...(defender.combatEffects??{}),dodgeNextIncoming:{difficultyUpgrades:1}};g.commit('CHARACTER_UPDATED',{character:defender});
 const combat=g.state.combat,enemy=combat.adversaries.find(x=>x.kind==='minion'&&!x.wounds?.incapacitated)||combat.adversaries[0];
 const npc=g._resolveNpcTurn(combat,g.state.player.character,enemy.id,()=>.999,g.state.group);
 const pending=npc.combat.holoParkPendingNpcDespair?.at(-1);assert.ok(pending,'NPC real Despair');
 g.commit('COMBAT_ACTION',{combat:npc.combat,character:npc.character});
 const vertigo=g.applyJewelHoloParkEffect({actionId:'p233-vertigo',effect:'kashyyyk_vertigo',npcAttackId:pending.attackId,gmSymbolConfirmed:true,rng:()=>.02});
 assert.equal(vertigo.adjudication.result.downgradePending,true);
 return {g,repo};
}
function verify(g,repo,kind,check){
 const events=g.state.combat.log.filter(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE');
 assert.equal(events.length,1);const e=events[0];assert.equal(e.actionKind,kind);
 assert.equal(e.downgraded,true);assert.equal(e.poolBefore.proficiency,4);assert.equal(e.poolAfter.proficiency,3);
 assert.equal(e.poolAfter.ability,e.poolBefore.ability+1);
 assert.equal(check.pool.proficiency,3);assert.equal(check.pool.ability,2);
 assert.deepEqual(e.checkProof.faces,check.faces);
 assert.equal(g.state.combat.holoParkVertigo.active,false);
 assert.equal(validateState(g.state),true);
 for(let i=0;i<3;i++){const re=new GameController(repo);assert.equal(validateState(re.state),true);assert.equal(re.state.combat.log.filter(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE').length,1);}
}
test('P233 Medicine consumes Kashyyyk vertigo in actual healing dice, once',()=>{
 const {g,repo}=fixture();const result=g.useMedicine({rng:()=>.5});verify(g,repo,'PLAYER_MEDICINE',result.check);
});
test('P233 Stim Application consumes Kashyyyk vertigo in real talent dice, once',()=>{
 const {g,repo}=fixture({stim:true});const result=g.applyStimApplication({characteristicKey:'agilidad',rng:()=>.5});verify(g,repo,'PLAYER_STIM_APPLICATION',result.check);
});
test('P233 failed Medicine action rolls back combat, character and vertigo if save fails',()=>{
 const {g,repo}=fixture();const before=clone(g.state),saves=repo.saves;repo.fail=true;
 assert.throws(()=>g.useMedicine({rng:()=>.5}),/P233 save failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);assert.equal(repo.saves,saves);
 repo.fail=false;const r=g.useMedicine({rng:()=>.5});assert.equal(r.check.pool.proficiency,3);
});
test('P233 failed Stim save rolls back real gear, character and pending vertigo',()=>{
 const {g,repo}=fixture({stim:true});const before=clone(g.state);repo.fail=true;
 assert.throws(()=>g.applyStimApplication({characteristicKey:'agilidad',rng:()=>.5}),/P233 save failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.state,before);
});
test('P233 save validator rejects altered dice face and forged pool, duplicate consumption',()=>{
 const {g}=fixture();g.useMedicine({rng:()=>.5});
 const edits=[s=>s.combat.log.find(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE').checkProof.faces[0].index=99,
  s=>s.combat.log.find(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE').poolAfter.proficiency=6,
  s=>s.combat.log.push(clone(s.combat.log.find(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE'))),
  s=>s.combat.log.find(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE').actionKind='NPC_ACTION'];
 for(const edit of edits){const state=clone(g.state);edit(state);assert.throws(()=>validateState(state));}
});
test('P233 pure downgrade never changes NPC pools, no proficiency cannot fabricate die',()=>{
 const original={ability:2,proficiency:3,difficulty:2};
 assert.deepEqual(downgradeVertigoPositivePool(original,false),original);
 assert.deepEqual(downgradeVertigoPositivePool(original,true),{ability:3,proficiency:2,difficulty:2});
 assert.deepEqual(original,{ability:2,proficiency:3,difficulty:2});
 assert.deepEqual(downgradeVertigoPositivePool({ability:2,proficiency:0},true),{ability:2,proficiency:0});
 assert.throws(()=>downgradeVertigoPositivePool(original,'yes'));
});
test('P233 Android PT-BR status, offline bridge and version are integrated',()=>{
 const fs=requireFs();
 const vtt=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,fragment] of [
  ['r46/final-runtime.js','P233 · VERTIGEM DE KASHYYYK'],
  ['r46/final-runtime.js','Medicina e Aplicação de Estimulantes'],
  ['js/core/uiBridge.bundle.js','PLAYER_STIM_APPLICATION'],
  ['js/core/uiBridge.bundle.js','validateJewelHoloVertigoConsumption'],
  ['js/core/controller.mjs','attachJewelHoloVertigoRoll'],
  ['js/core/state.mjs','validateJewelHoloVertigoConsumption']])
   assert.ok(fs(new URL(file,vtt)).includes(fragment),file);
 assert.match(fs(new URL('../app/build.gradle.kts',import.meta.url)),/versionCode = 10(?:23[3-9]|2[4-9]\d|[3-9]\d{2})/);
 assert.match(fs(new URL('../app/src/main/assets/vtt/r46-shell.html',import.meta.url)),/data-version="1\.0\.(?:23[3-9]|2[4-9]\d|[3-9]\d{2})"/);
});
function requireFs(){return url=>readFileSync(url,'utf8');}
