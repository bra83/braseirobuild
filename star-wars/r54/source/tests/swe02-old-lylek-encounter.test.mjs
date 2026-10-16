import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
class Repo { constructor(){this.state=createCampaign('SWE02 Old Lylek');this.saves=0;} load(){return structuredClone(this.state);} save(s){this.state=structuredClone(s);this.saves++;} }
function fixture(playerCount=1){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount,rng:()=>.5});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.5});
 for(let i=1;i<playerCount;i++){const c=structuredClone(g.state.player.character);c.name=`PC ${i+1}`;g.registerPartyCharacter({character:c,id:`test-pc-${i+1}`});}
 for(const choice of ['dock-ryloth','help-new-meen','travel-lylek-den'])g.advancePublishedAdventure(choice);
 g.selectSWE02TrexCarryover({history:'confirmed_dead'});
 g.chooseSWE02OldLylekCrew({crewPcIds:g.state.player.roster.entries.map(x=>x.id)});
 return {repo,g};
}
function reopenThree(repo,assertState){let last;for(let n=0;n<3;n++){last=new GameController(repo);assertState(last.state);}return last;}
function reachHuntersReady(g,{slowDown=true}={}){
 g.chooseSWE02OldLylekRoute({slowDown});
 if(slowDown)g.approachSWE02OldLylekOnFoot();
 else {for(let n=0;n<2;n++)g.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',rng:()=>.5});g.proceedSWE02OldLylekBluff();}
 if(g.state.world.publishedAdventure.oldLylek.phase==='ruse')g.recordSWE02OldLylekRuse({recognizedByHunters:false,oskaraPresent:false});
 if(g.state.world.publishedAdventure.oldLylek.phase==='combat-ready')g.chooseSWE02OldLylekTactics({approach:'frontal',frontalOutcome:'fighting_retreat',lylekBranch:'disturb_on_advantage'});
 assert.equal(g.state.world.publishedAdventure.oldLylek.phase,'combat-ready');
}

test('Old Lylek foot approach is available without a Perception declaration; hunter loot, warning and optional verified lylek attack survive three reopenings',()=>{
 const {repo,g}=fixture();
 assert.throws(()=>g.advancePublishedAdventure('reach-new-meen'),/Escolha inválida/);
 assert.throws(()=>g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5}),/pp.15–17/);
 reachHuntersReady(g);
 g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 const hunters=structuredClone(g.state.combat);hunters.encounter.outcome='adversaries_defeated';
 for(const enemy of hunters.adversaries)enemy.status='incapacitated';
 hunters.swe02OldLylekAdvantageTriggered=true;
 g.commit('COMBAT_ACTION',{combat:hunters});repo.save(g.state);g.finishCombat({rng:()=>.5});
 g.inspectSWE02OldLylekSpeeder();
 assert.deepEqual(g.state.group.belongings.filter(x=>x.id.startsWith('swe02-old-lylek-')).map(x=>[x.name,x.quantity]),[['Stimpack',1],['Stimpack',1],['Medpac',1],['Emergency Repair Kit',1]]);
 assert.throws(()=>g.inspectSWE02OldLylekSpeeder(),/após derrotar|inspecione/);
 let reopened=reopenThree(repo,s=>{
   const old=s.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'post-hunters');assert.equal(old.lylekTriggered,true);assert.equal(old.lylekKilledHunter,true);
   assert.equal(old.hunterLoot.status,'found');assert.equal(old.hunterLoot.inspected,true);
   assert.equal(old.hunterLoot.inventoryReceipt.status,'GRANTED');assert.equal(old.hunterLoot.inventoryReceipt.inventoryIds.length,4);
   assert.deepEqual(old.hunterLoot.items,[{name:'stimpack',quantity:2},{name:'medpac',quantity:1},{name:'emergency repair kit',quantity:1}]);
   assert.equal(s.world.publishedAdventure.flags.lylekHuntersResolved,true);
 });
 reopened.insistSWE02OldLylek();
 assert.deepEqual(reopened.state.scene.visibleNpcIds,['laoth.npc.bura_ban']);
 assert.match(reopened.state.scene.narration,/carne fresca|presa/);
 reopened=reopenThree(repo,s=>assert.equal(s.world.publishedAdventure.oldLylek.phase,'lylek-warned'));
 const lylekProof=reopened.persistSWE02OldLylek();
 assert.equal(lylekProof.profileKey,'swe02_captive_rancor');
 assert.equal(lylekProof.source.profileSourceVerified,true);
 reopened=reopenThree(repo,s=>{
   const old=s.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'lylek-ready');assert.equal(old.lylekEncounter.source.referencePrintedPage,47);
 });
 const profile=PUBLISHED_ADVENTURE_ADVERSARY_PROFILES.swe02_captive_rancor;
 assert.equal(profile.source.productCode,'SWE01');assert.equal(profile.source.printedPage,47);
 assert.deepEqual(profile.characteristics,{fortaleza:6,agilidad:2,intelecto:1,astucia:3,voluntad:3,presencia:1});
 assert.equal(profile.soak,12);assert.equal(profile.woundsThreshold,40);assert.equal(profile.strainThreshold,15);
 assert.equal(profile.skills.pelea,2);assert.equal(profile.weapons[0].damage,20);assert.equal(profile.weapons[0].crit,3);
 reopened.beginPublishedAdventureEncounter('lylek-den-lylek',{rng:()=>.5});
 assert.equal(reopened.state.combat.adversaries[0].profileKey,'swe02_captive_rancor');
 const lylek=reopened.state.combat.adversaries[0];
 const npc=reopened._resolveNpcTurn(structuredClone(reopened.state.combat),structuredClone(reopened.state.player.character),lylek.id,()=>.5,reopened.state.group);
 assert.equal(npc.attack.weapon.name,'Garras enormes');
 const ended=structuredClone(npc.combat);ended.encounter.outcome='ended';
 reopened.commit('COMBAT_ACTION',{combat:ended,character:npc.character,group:npc.group??reopened.state.group});repo.save(reopened.state);
 reopened.finishCombat({rng:()=>.5});
 reopened=reopenThree(repo,s=>{
   const old=s.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'post-lylek');assert.equal(old.lylekResolution.outcome,'ended');
   assert.equal(old.lylekResolution.beastProfileVerified,true);
   assert.equal(old.lylekResolution.profileKey,'swe02_captive_rancor');
 });
 reopened.advancePublishedAdventure('reach-new-meen');
 assert.equal(reopened.state.world.publishedAdventure.currentNodeId,'new-meen-miners');
 assert.deepEqual(reopened.state.world.publishedAdventure.oldLylek.arrivalReceipt,{distanceMiles:1,buraExplainsAid:true,residentsWelcomeAsHeroes:true,invitedToLargerDwelling:true,basicRationsOffered:true,rationQuantity:null,developerDetails:{status:'ONLY_IF_ASKED'},source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:18,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}});
});

test('lylek submission requires an explicit GM confirmation and preserves source absence of a numeric threshold',()=>{
 const {repo,g}=fixture();reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 const hunters=structuredClone(g.state.combat);hunters.encounter.outcome='adversaries_defeated';hunters.swe02OldLylekAdvantageTriggered=true;for(const enemy of hunters.adversaries)enemy.status='incapacitated';
 g.commit('COMBAT_ACTION',{combat:hunters});g.finishCombat({rng:()=>.5});g.insistSWE02OldLylek();g.persistSWE02OldLylek();
 g.beginPublishedAdventureEncounter('lylek-den-lylek',{rng:()=>.5});const beast=structuredClone(g.state.combat);beast.encounter.outcome='player_incapacitated';
 g.commit('COMBAT_ACTION',{combat:beast});assert.throws(()=>g.finishCombat({rng:()=>.5}),/confirme submissão/);
 g.finishCombat({gmConfirmedSubmission:true,rng:()=>.5});
 reopenThree(repo,s=>{const resolution=s.world.publishedAdventure.oldLylek.lylekResolution;assert.equal(resolution.gmConfirmedSubmission,true);assert.equal(resolution.numericSubmissionThreshold,null);assert.equal(resolution.source.printedPage,18);});
});

test('failed fast-route Pilot checks apply party-wide strain by stable PC ID and persist after reopen',()=>{
 const {repo,g}=fixture(2);g.chooseSWE02OldLylekRoute({slowDown:false});
 for(let n=0;n<2;n++)g.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',rng:()=>0});
 const old=g.state.world.publishedAdventure.oldLylek,ids=g.state.player.roster.entries.map(x=>x.id);
 assert.deepEqual(old.pilotChecks.map(x=>x.success),[false,false]);
 for(const check of old.pilotChecks){assert.equal(check.collision,true);assert.equal(check.onboardPcStrain.status,'APPLIED');assert.deepEqual(check.onboardPcStrain.crewPcIds,ids);}
 for(const entry of g.state.player.roster.entries)assert.equal(entry.character.derived.strain.current,2,entry.id);
 reopenThree(repo,s=>{assert.equal(s.player.roster.entries.length,2);for(const entry of s.player.roster.entries)assert.equal(entry.character.derived.strain.current,2);assert.equal(s.world.publishedAdventure.oldLylek.pilotChecks[1].onboardPcStrain.status,'APPLIED');});
});

test('crash checks resolve once for each onboard PC, apply failure damage to the matching sheet, and exclude companions',()=>{
 const {repo,g}=fixture(2),roster=g.state.player.roster,ids=roster.entries.map(x=>x.id);
 g.state.group.companions=[{id:'npc-friend',name:'NPC Companion',character:structuredClone(roster.entries[0].character)}];
 repo.save(g.state);
 g.chooseSWE02OldLylekRoute({slowDown:false});
 for(let n=0;n<2;n++)g.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',rng:()=>0});
 g.proceedSWE02OldLylekBluff();assert.equal(g.state.world.publishedAdventure.oldLylek.phase,'crash-checks');
 const before=roster.entries.map(x=>x.character.derived.wounds.current);
 const first=g.resolveSWE02OldLylekCrashCheck({skillKey:'aguante',rng:()=>0});
 const second=g.resolveSWE02OldLylekCrashCheck({skillKey:'coordinacion',rng:()=>0});
 assert.deepEqual([first.characterId,second.characterId].sort(),ids.slice().sort());
 const checks=g.state.world.publishedAdventure.oldLylek.crashChecks;assert.equal(checks.length,2);assert.equal(g.state.world.publishedAdventure.oldLylek.phase,'repair');
 for(const [i,entry] of g.state.player.roster.entries.entries()){const check=checks.find(x=>x.characterId===entry.id);assert.equal(entry.character.derived.wounds.current,before[i]+(check.success?0:Math.max(0,6-entry.character.derived.soak)));}
 assert.equal(g.state.group.companions[0].character.derived.wounds.current,0);
 reopenThree(repo,s=>{assert.deepEqual(s.world.publishedAdventure.oldLylek.crashChecks.map(x=>x.characterId).sort(),ids.slice().sort());for(const entry of s.player.roster.entries){const check=s.world.publishedAdventure.oldLylek.crashChecks.find(x=>x.characterId===entry.id);assert.equal(entry.character.derived.wounds.current,check.success?0:Math.max(0,6-entry.character.derived.soak));}});
});

test('schema 14 singleton migration creates one stable PC ID and mirrors active-sheet updates without importing companions',()=>{
 const legacy=createCampaign('legacy roster');legacy.schemaVersion=14;legacy.player.character=structuredClone((()=>{const {g}=fixture();return g.state.player.character;})());delete legacy.player.roster;
 const migrated=migrateState(legacy),id=migrated.player.roster.activeCharacterId;
 assert.equal(migrated.schemaVersion,15);assert.equal(migrated.player.roster.entries.length,1);assert.ok(id);
 assert.equal(migrateState(migrated).player.roster.activeCharacterId,id);
 const repo=new Repo();repo.state=migrated;const g=new GameController(repo);const second=structuredClone(g.state.player.character);second.name='Second PC';g.registerPartyCharacter({character:second,id:'pc-second'});
 g.state.group.companions.push({id:'npc-only',name:'NPC only',character:structuredClone(second)});g.selectPartyCharacter('pc-second');
 const update=structuredClone(g.state.player.character);update.credits+=1;g.commit('CHARACTER_UPDATED',{character:update});
 assert.equal(g.state.player.roster.entries.length,2);assert.equal(g.state.player.roster.entries.find(x=>x.id==='pc-second').character.credits,update.credits);
 assert.equal(g.state.player.character.name,'Second PC');assert.ok(!g.state.player.roster.entries.some(x=>x.id==='npc-only'));
});

test('retreat or incapacitation settles the hunter branch while preserving the lylek trigger and its p.18 loot rule',()=>{
 for(const outcome of ['escaped','player_incapacitated']){
   const {g}=fixture();reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
   const combat=structuredClone(g.state.combat);combat.encounter.outcome=outcome;
   for(const enemy of combat.adversaries)enemy.status='active';combat.swe02OldLylekAdvantageTriggered=true;
   g.commit('COMBAT_ACTION',{combat});g.finishCombat({rng:()=>.5});
   const old=g.state.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'post-hunters');assert.equal(old.lylekTriggered,true);
   assert.equal(old.huntersOutcome,outcome==='player_incapacitated'?'party-incapacitated':'retreated');
   assert.equal(old.hunterLoot,null);
 }
});

