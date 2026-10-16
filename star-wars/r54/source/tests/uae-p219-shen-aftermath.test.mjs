import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {validateJewelShenAftermath} from '../app/src/main/assets/vtt/js/adventure/jewelShenAftermath.mjs';
import {validateJewelOperations} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavinOperations.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P219');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p219-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const w=clone(g.state.world);w.publishedAdventure.operations.escapeBomb={status:'DISARMED'};w.publishedAdventure.encounterHistory=[{encounterId:'arend-shen-four-goons',outcome:'escaped'}];g.commit('WORLD_UPDATED',{world:w});
 const c=clone(g.state.player.character);c.characteristics.intelecto=7;c.skills.computadoras=7;g.commit('CHARACTER_UPDATED',{character:c});return {g,repo};}
function located(){const x=fixture();x.g.investigateJewelArendHoldings({actionId:'lead219',skill:'computadoras',gmMainframeAccessConfirmed:true,rng:()=>.28});x.g.visitJewelArendHolding({actionId:'visit219',holdingId:'plaza-residence',gmSearchConfirmed:true,rng:()=>0});return x;}
const ar=(status='INCARCERATED',extra={})=>({actionId:'p219-arend',status,gmArendIdentified:true,gmPhysicalConfrontationConfirmed:true,gmOutcomeConfirmed:true,gmDeathConfirmed:status==='DEAD',gmCustodyConfirmed:status==='INCARCERATED',gmEscapeConfirmed:status==='AT_LARGE',...extra});
const as=(extra={})=>({actionId:'p219-aris',gmArisMetInPerson:true,gmArisAgreedToAlliance:true,gmPartyAcceptedAlliance:true,gmArisNoLongerHostile:true,...extra});
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
test('P219: Arend outcome only after real search, four-goon resolution and physical confrontation',()=>{
 const {g}=fixture();atomic(g,()=>g.recordJewelArendOutcome(ar()));g.investigateJewelArendHoldings({actionId:'lead219',skill:'computadoras',gmMainframeAccessConfirmed:true,rng:()=>.28});atomic(g,()=>g.recordJewelArendOutcome(ar()));
});
test('P219: incarcerated Arend creates durable campaign consequence without XP, credits or travel',()=>{
 const {g,repo}=located();const before=clone(g.state.player.character);const node=g.state.world.publishedAdventure.currentNodeId;
 const result=g.recordJewelArendOutcome(ar());assert.equal(result.aftermath.arend.status,'INCARCERATED');assert.equal(result.aftermath.arend.holdingId,'plaza-residence');assert.equal(g.state.world.publishedAdventure.currentNodeId,node);assert.deepEqual(g.state.player.character,before);
 for(let i=0;i<3;i++){const reopen=new GameController(repo);assert.equal(validateJewelShenAftermath(reopen.state.world.publishedAdventure).arend.status,'INCARCERATED');}
});
test('P219: all three official Arend fates distinct and require matching GM evidence',()=>{
 for(const status of ['DEAD','INCARCERATED','AT_LARGE']){const {g}=located();atomic(g,()=>g.recordJewelArendOutcome(ar(status,{gmOutcomeConfirmed:false})));atomic(g,()=>g.recordJewelArendOutcome(ar(status,{gmDeathConfirmed:false,gmCustodyConfirmed:false,gmEscapeConfirmed:false})));assert.equal(g.recordJewelArendOutcome(ar(status)).aftermath.arend.status,status);atomic(g,()=>g.recordJewelArendOutcome(ar(status,{actionId:'another'})));}
});
test('P219: Aris alliance requires genuine meeting, both consents and no hostility',()=>{
 const {g}=fixture();for(const k of ['gmArisMetInPerson','gmArisAgreedToAlliance','gmPartyAcceptedAlliance','gmArisNoLongerHostile'])atomic(g,()=>g.recordJewelArisAlliance(as({[k]:false})));
 const r=g.recordJewelArisAlliance(as());assert.equal(r.aftermath.aris.allied,true);atomic(g,()=>g.recordJewelArisAlliance(as({actionId:'another'})));
});
test('P219: Arend and Aris outcomes coexist with Lando favor and no invented reward',()=>{
 const {g}=located();g.recordJewelArisAlliance(as());g.recordJewelArendOutcome(ar('DEAD'));const p=g.state.world.publishedAdventure;assert.equal(p.operations.shenAftermath.arend.status,'DEAD');assert.equal(p.operations.shenAftermath.aris.allied,true);assert.equal(p.operations.landoPardon,undefined);assert.equal(validateJewelShenAftermath(g.state.world.publishedAdventure).aris.allied,true);
});
test('P219: tampered receipts and derived state rejected',()=>{
 const {g}=located();g.recordJewelArendOutcome(ar());g.recordJewelArisAlliance(as());const p=g.state.world.publishedAdventure;
 for(const edit of [x=>x.operations.receipts['p219-arend'].status='DEAD',x=>x.operations.receipts['p219-arend'].holdingId='plaza-yacht',x=>x.operations.receipts['p219-aris'].gmPartyAcceptedAlliance=false,x=>x.operations.shenAftermath.aris.allied=false,x=>delete x.operations.receipts['p219-arend']]){const v=clone(p);edit(v);assert.throws(()=>validateJewelShenAftermath(v));}
});
test('P219: combat prevents outcomes and transaction rolls back',()=>{
 const {g}=located();const c=clone(g.state.combat);c.active=true;g.commit('COMBAT_ACTION',{combat:c});atomic(g,()=>g.recordJewelArendOutcome(ar()));atomic(g,()=>g.recordJewelArisAlliance(as()));
});
test('P219: Lando P218 receipt is now allowed in global operations validation',()=>{
 const {g}=fixture();g.grantJewelLandoPardon({actionId:'lando219',members:[g.state.player.character.name],gmLandoAudienceConfirmed:true,gmRestorationPlanAccepted:true,gmPardonGranted:true,gmPartyRosterConfirmed:true,gmMembersArePlayerCharactersConfirmed:true});assert.doesNotThrow(()=>validateJewelOperations(g.state.world));
});
test('P219: Android UI, bridge, offline bundle and version actually connected',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p219-arend-record'],['r46/final-runtime.js','data-p219-aris-record'],['js/core/controller.mjs','recordJewelArendOutcome'],['js/core/uiBridge.mjs','recordJewelArisAlliance'],['js/core/uiBridge.bundle.js','jewelShenAftermath.mjs'],['js/core/state.mjs','validateJewelShenAftermath'],['js/adventure/jewelOfYavin.mjs','shenAftermath: clone(validateJewelShenAftermath(p))']])assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10219);
});
