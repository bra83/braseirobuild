import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelBankPreparation} from '../app/src/main/assets/vtt/js/adventure/jewelBankPreparations.mjs';
const copy=structuredClone,adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P188');this.saves=0;}load(){return copy(this.state);}save(s){this.state=copy(s);this.saves++;}}
function game(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.state.player.character.gear.push({id:'restraining_bolt',name:'Parafuso de restrição (droide)',quantity:1,encumbrance:0});g.state.player.character.gear.push({id:'tool_kit',name:'Kit de ferramentas',quantity:1,encumbrance:4});
 g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'a-'+i});}
 g.advancePublishedAdventure('bank-operation');
 return {g,repo};
}
function prep(g,step,details={}){return g.prepareJewelBank({actionId:'prep-'+step,step,...details});}
function bank(g,step,details={}){return g.resolveJewelBankStep({actionId:'bank-'+step+'-'+(g.state.world.publishedAdventure.operations?.bank?.history.length||0),step,rng:()=>.49,...details});}
test('P188 preparation rejects unapproved samples and Lobot outcomes',()=>{
 const {g,repo}=game(),before=copy(g.state),saves=repo.saves;
 assert.throws(()=>prep(g,'sample',{method:'arend_access_card'}),/aprovação/);
 assert.throws(()=>prep(g,'sample',{method:'invented',gmConfirmed:true}),/aprovação/);
 assert.throws(()=>prep(g,'lobot',{mode:'neutralized'}),/aprovação/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
});
test('P188 full encryption and neutralized Lobot change real opposed pool',()=>{
 const {g}=game();
 prep(g,'sample',{method:'arend_access_card',gmConfirmed:true});
 prep(g,'lobot',{mode:'neutralized',gmConfirmed:true});
 bank(g,'acquire',{target:'industrial_docks',restrainingBolt:true});
 bank(g,'disable');
 const r=bank(g,'slice');
 assert.equal(r.adjudication.evidence.pool.difficulty,4);
 assert.equal(r.adjudication.evidence.pool.challenge,0);
 assert.equal(r.adjudication.evidence.pool.setback,0);
 assert.equal(validateJewelBankPreparation(g.state.world.publishedAdventure),true);
});
test('P188 basic sample and Bea alert add actual setbacks, distraction downgrades',()=>{
 const {g}=game();
 prep(g,'sample',{method:'public_office_distraction',gmConfirmed:true});
 prep(g,'lobot',{mode:'distracted',beaAlerted:true,gmConfirmed:true});
 bank(g,'acquire',{target:'industrial_docks',restrainingBolt:true});bank(g,'disable');
 const r=bank(g,'slice');
 assert.equal(r.adjudication.evidence.pool.setback,2);
 assert.equal(r.adjudication.evidence.pool.challenge,1);
 assert.equal(r.adjudication.evidence.pool.difficulty,4);
});
test('P188 no sample carries three setbacks once prep contract opted in',()=>{
 const {g}=game();prep(g,'lobot',{mode:'distracted',gmConfirmed:true});
 bank(g,'acquire',{target:'industrial_docks',restrainingBolt:true});bank(g,'disable');
 assert.equal(bank(g,'slice').adjudication.evidence.pool.setback,3);
});
test('P188 spike consumes 300 credits and rolls Hard Computers, never grants bonus on failed check',()=>{
 const {g}=game();
 assert.throws(()=>prep(g,'spike',{rng:()=>.99}),/300 créditos/);
 g.commit('CHARACTER_UPDATED',{character:{...g.state.player.character,credits:350}});
 const r=prep(g,'spike',{rng:()=>.99});
 assert.equal(r.adjudication.evidence.pool.difficulty,3);
 assert.equal(r.adjudication.creditsSpent,300);
 assert.equal(g.state.player.character.credits,50);
 assert.equal(r.adjudication.success,false);
 assert.equal(r.preparation.spike.boost,0);
});
test('P188 spike roll replay rejects face, cost and boost forgery',()=>{
 const {g}=game();g.commit('CHARACTER_UPDATED',{character:{...g.state.player.character,credits:350}});
 prep(g,'spike',{rng:()=>.49});
 for(const change of [
  p=>p.operations.receipts['prep-spike'].creditsSpent=0,
  p=>p.operations.receipts['prep-spike'].evidence.roll.faces[0].index=99,
  p=>p.operations.bankPreparation.spike.boost+=2,
 ]){
  const altered=copy(g.state.world);change(altered.publishedAdventure);
  assert.throws(()=>adapter.status(altered),/adulterad|inválid|não corresponde/);
 }
});
test('P188 duplicate preparation, post-slice retroactivity and invalid step roll back',()=>{
 const {g,repo}=game();prep(g,'sample',{method:'public_office_distraction',gmConfirmed:true});
 const before=copy(g.state),saves=repo.saves;
 assert.throws(()=>prep(g,'sample',{method:'public_office_distraction',gmConfirmed:true}),/registrada/);
 assert.throws(()=>prep(g,'invented'),/inválida/);
 assert.deepEqual(g.state,before);assert.equal(repo.saves,saves);
 bank(g,'acquire',{target:'industrial_docks',restrainingBolt:true});bank(g,'disable');bank(g,'slice');
 assert.throws(()=>prep(g,'lobot',{mode:'neutralized',gmConfirmed:true}),/retroativamente/);
});
test('P188 preparation, real bank evidence and character credits persist over three reopenings',()=>{
 const {g,repo}=game();g.commit('CHARACTER_UPDATED',{character:{...g.state.player.character,credits:350}});
 prep(g,'spike',{rng:()=>.49});prep(g,'sample',{method:'government_infiltration',gmConfirmed:true});
 bank(g,'acquire',{target:'industrial_docks',restrainingBolt:true});bank(g,'disable');bank(g,'slice');
 const expected=copy(g.state.world.publishedAdventure.operations.bankPreparation);
 for(let i=0;i<3;i++){
  const restored=new GameController(repo);
  assert.deepEqual(restored.state.world.publishedAdventure.operations.bankPreparation,expected);
  assert.equal(restored.state.player.character.credits,50);
  assert.equal(adapter.status(restored.state.world).active,true);
 }
});
test('P188 successful spike applies real boost dice only to first slicing attempt',()=>{
 const {g}=game();
 const ch=copy(g.state.player.character);
 ch.credits=350;ch.characteristics.intelecto=5;ch.skills.computadoras=5;
 g.commit('CHARACTER_UPDATED',{character:ch});
 const crafted=prep(g,'spike',{rng:()=>.6});
 assert.equal(crafted.adjudication.success,true);
 assert.ok(crafted.preparation.spike.boost>0);
 bank(g,'acquire',{target:'industrial_docks',restrainingBolt:true,rng:()=>.49});
 bank(g,'disable',{rng:()=>.6});
 const first=bank(g,'slice',{rng:()=>.99});
 assert.equal(first.adjudication.evidence.pool.boost,crafted.preparation.spike.boost);
 if(first.bank.stage==='READY'){
   const second=bank(g,'slice',{rng:()=>.99});
   assert.equal(second.adjudication.evidence.pool.boost,0);
 }
});
test('P188 basic encryption can be upgraded to full without losing causal receipts',()=>{
 const {g}=game();
 prep(g,'sample',{method:'public_office_distraction',gmConfirmed:true});
 const result=g.prepareJewelBank({actionId:'upgrade-sample',step:'sample',method:'arend_access_card',gmConfirmed:true});
 assert.equal(result.preparation.sample,'full');
 prep(g,'lobot',{mode:'distracted',gmConfirmed:true});
 const next=g.prepareJewelBank({actionId:'lobot-neutralized',step:'lobot',mode:'neutralized',gmConfirmed:true});
 assert.equal(next.preparation.lobot,'neutralized');
 bank(g,'acquire',{target:'industrial_docks',restrainingBolt:true});bank(g,'disable');
 const roll=bank(g,'slice');
 assert.equal(roll.adjudication.evidence.pool.setback,0);
 assert.equal(roll.adjudication.evidence.pool.difficulty,4);
 assert.equal(validateJewelBankPreparation(g.state.world.publishedAdventure),true);
});
test('P188 Android bridge exposes preparation action',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 assert.equal(typeof BraseiroUiBridge.prototype.prepareJewelBank,'function');
 assert.equal(typeof GameController.prototype.prepareJewelBank,'function');
});
test('P188 Android scene offers explicit GM confirmation and bank controls',async()=>{
 const {readFileSync}=await import('node:fs');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 for(const token of ['data-jewel-gm','data-jewel-prep','data-jewel-bank','prepareJewelBank','resolveJewelBankStep'])
   assert.ok(ui.includes(token),`Controle Android ausente: ${token}`);
});
