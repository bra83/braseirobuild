import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {obligationTotal} from '../app/src/main/assets/vtt/js/rules/obligation.mjs';
import {validateJewelLandoPardon} from '../app/src/main/assets/vtt/js/adventure/jewelLandoPardon.mjs';
const clone=structuredClone;
class Repo {constructor(){this.state=createCampaign('P218');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});return {g,repo};}
function args(g,extra={}){return {actionId:'p218-lando',members:[g.state.player.character.name],gmLandoAudienceConfirmed:true,gmRestorationPlanAccepted:true,gmPardonGranted:true,gmPartyRosterConfirmed:true,gmMembersArePlayerCharactersConfirmed:true,...extra};}
function atomic(g,fn){const state=clone(g.state),n=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,state);assert.equal(g.repository.saves,n);}

test('P218 official p.90: +5 Favor per GM-confirmed real PC, not a narrative flag',()=>{
 const {g,repo}=game(),before=obligationTotal(g.state.group,g.state.player.character),n=repo.saves;
 const r=g.grantJewelLandoPardon(args(g,{members:['Cael','Jaina','Borsk']}));
 assert.equal(repo.saves,n+1);assert.equal(r.obligationTotal,before+15);
 assert.deepEqual(g.state.group.obligationEntries.map(x=>[x.member,x.type,x.value]),[['Cael','Favor',5],['Jaina','Favor',5],['Borsk','Favor',5]]);
 assert.equal(g.state.world.publishedAdventure.operations.landoPardon.kind,'JEWEL_LANDO_PARDON');
 assert.equal(g.state.world.publishedAdventure.currentNodeId,'port-town-rendezvous','no scene teleport');
 assert.equal(g.state.world.publishedAdventure.operations.landoPardon.gmPardonGranted,true);
});
test('P218 no automatic pardon before Lando actually accepts restoration plan and grants pardon',()=>{
 const {g}=game();for(const k of ['gmLandoAudienceConfirmed','gmRestorationPlanAccepted','gmPardonGranted','gmPartyRosterConfirmed','gmMembersArePlayerCharactersConfirmed'])atomic(g,()=>g.grantJewelLandoPardon(args(g,{[k]:false})));
 atomic(g,()=>g.grantJewelLandoPardon(args(g,{members:['Other']})));
 atomic(g,()=>g.grantJewelLandoPardon(args(g,{members:['Cael','cael']})));
 atomic(g,()=>g.grantJewelLandoPardon(args(g,{members:['Cael','']})));
 atomic(g,()=>g.grantJewelLandoPardon(args(g,{members:Array(9).fill('Cael')})));
});
test('P218 exactly once even if caller changes actionId, plus three real reopenings',()=>{
 const {g,repo}=game();g.grantJewelLandoPardon(args(g));
 atomic(g,()=>g.grantJewelLandoPardon(args(g,{actionId:'different'})));
 for(let i=0;i<3;i++){const h=new GameController(repo);assert.equal(h.state.group.obligationEntries.length,1);assert.equal(validateJewelLandoPardon(h.state.world.publishedAdventure).obligationPerMember,5);}
});
test('P218 existing group and personal legacy Obligation retained instead of disappearing',()=>{
 const {g}=game();const ch=clone(g.state.player.character);ch.obligations=[{id:'old',type:'Dívida',value:13,description:'Dívida anterior'}];g.commit('CHARACTER_UPDATED',{character:ch});
 assert.equal(obligationTotal(g.state.group,ch),13);
 g.grantJewelLandoPardon(args(g));assert.equal(obligationTotal(g.state.group,ch),18);
 assert.equal(g.state.group.obligationEntries[0].value,13);
 assert.equal(g.state.group.obligationEntries[1].value,5);
});
test('P218 no double-count of pre-existing group obligations',()=>{
 const {g}=game();g.addGroupObligation({id:'existing',member:'Cael',type:'Dívida',description:'Anterior',value:8});
 g.grantJewelLandoPardon(args(g));assert.equal(obligationTotal(g.state.group,g.state.player.character),13);
});
test('P218 replay rejects tampering, extra flags, duplicate names and removed proof',()=>{
 const {g}=game();g.grantJewelLandoPardon(args(g));const p=clone(g.state.world.publishedAdventure);
 p.operations.landoPardon.obligationPerMember=10;assert.throws(()=>validateJewelLandoPardon(p));
 p.operations.landoPardon=clone(g.state.world.publishedAdventure.operations.landoPardon);
 p.operations.receipts['p218-lando'].members.push('cael');assert.throws(()=>validateJewelLandoPardon(p));
 p.operations.receipts['p218-lando']=clone(g.state.world.publishedAdventure.operations.receipts['p218-lando']);
 p.operations.receipts['p218-lando'].gmPardonGranted=false;assert.throws(()=>validateJewelLandoPardon(p));
 delete p.operations.receipts['p218-lando'];assert.throws(()=>validateJewelLandoPardon(p));
});
test('P218 cannot grant pardon during combat, or in AoR-only campaign',()=>{
 const {g}=game();const c=clone(g.state.combat);c.active=true;g.commit('COMBAT_ACTION',{combat:c});
 atomic(g,()=>g.grantJewelLandoPardon(args(g)));
 g.commit('COMBAT_ACTION',{combat:{...c,active:false}});
 g.state.campaignMode='AOR';atomic(g,()=>g.grantJewelLandoPardon(args(g)));
});
test('P218 real bridge, status, UI and version wired, Java/Kokoro source untouched',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [path,token] of [['js/core/controller.mjs','grantJewelLandoPardon'],['js/core/uiBridge.mjs','grantJewelLandoPardon'],['js/core/uiBridge.bundle.js','grantJewelLandoPardon'],['r46/final-runtime.js','data-p218-lando-grant'],['js/adventure/jewelOfYavin.mjs','landoPardon: clone(validateJewelLandoPardon(p))'],['js/core/state.mjs','validateJewelLandoPardon']])
  assert.ok(readFileSync(new URL(path,root),'utf8').includes(token),path);
 assert.ok(Number((readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode = (\d+)/)||[])[1])>=10218);
});
