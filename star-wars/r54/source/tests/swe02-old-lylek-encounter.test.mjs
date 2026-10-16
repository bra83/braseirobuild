import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {advanceNabatMorningController} from './swe02-act1-fixture.mjs';

const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
class Repo { constructor(){this.state=createCampaign('SWE02 Old Lylek');this.saves=0;} load(){return structuredClone(this.state);} save(s){this.state=structuredClone(s);this.saves++;} }
function fixture(playerCount=1){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount,rng:()=>.5});g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.5});
 for(let i=1;i<playerCount;i++){const c=structuredClone(g.state.player.character);c.name=`PC ${i+1}`;g.registerPartyCharacter({character:c,id:`test-pc-${i+1}`});}
 for(const choice of ['dock-ryloth','help-new-meen'])g.advancePublishedAdventure(choice);
 advanceNabatMorningController(g);
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
 const {repo,g}=fixture(2);reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
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

test('schema 14 singleton migration to schema 15 creates one stable PC ID and mirrors active-sheet updates without importing companions',()=>{
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

test('p.17 rock cover adds two setback to ranged checks and hunters begin with stun/capture intent, then switch to lethal after a hunter falls',()=>{
 const runAttack=behind=>{
   const {g}=fixture();reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
   const target=g.state.combat.adversaries[0];
   g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,targetBehindRockPile:behind,rng:()=>.5});
   return g.state.lastCheck;
 };
 const exposed=runAttack(false),covered=runAttack(true);
 assert.equal(covered.swe02OldLylekCover.setback,2);assert.equal(covered.pool.setback-exposed.pool.setback,2);

 const {repo,g}=fixture(2);reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 g.setSWE02OldLylekPlayerCover({behindRockPile:true});
 let combat=structuredClone(g.state.combat);const [first,second]=combat.adversaries;
 const capture=g._resolveNpcTurn(combat,structuredClone(g.state.player.character),first.id,()=>.5,g.state.group);
 assert.equal(capture.attack.weapon.damageType,'strain');assert.equal(capture.attack.weapon.stunSettingActive,true);assert.equal(capture.attack.coverSetback,2);
 assert.equal(capture.combat.swe02HunterIntent.mode,'STUN_ARMED_RESISTANCE');
 combat=structuredClone(capture.combat);const down=structuredClone(combat.adversaries.find(a=>a.id===first.id));down.wounds={...down.wounds,current:down.wounds.threshold+1};down.status='incapacitated';
 combat.adversaries=combat.adversaries.map(a=>a.id===down.id?down:a);
 const lethal=g._resolveNpcTurn(combat,structuredClone(g.state.player.character),second.id,()=>.5,g.state.group);
 assert.equal(lethal.attack.weapon.damageType,undefined);assert.equal(lethal.attack.weapon.stunSettingActive,false);assert.equal(lethal.combat.swe02HunterIntent.mode,'LETHAL_AFTER_HUNTER_FALLEN');
 g.commit('COMBAT_ACTION',{combat:lethal.combat});
 reopenThree(repo,s=>{assert.equal(s.combat.swe02OldLylekCover.playerBehindRockPile,true);assert.equal(s.combat.swe02HunterIntent.mode,'LETHAL_AFTER_HUNTER_FALLEN');});
});

test('p.16 eastern cave flank is a persisted position choice; frontal fighting retreat uses the dotted path and persists the escaped outcome',()=>{
 const {repo,g}=fixture();
 g.chooseSWE02OldLylekRoute({slowDown:true});g.approachSWE02OldLylekOnFoot();g.recordSWE02OldLylekRuse({oskaraPresent:false});
 g.chooseSWE02OldLylekTactics({approach:'east_cave_flank',lylekBranch:'stay_passive'});
 g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 assert.equal(g.state.combat.swe02OldLylekTactics.startingPosition,'partially blocked eastern cave entrance');
 assert.equal(g.state.combat.swe02OldLylekTactics.hunterRetreatPath,'unselected');
 reopenThree(repo,s=>{assert.equal(s.combat.swe02OldLylekTactics.startingPosition,'partially blocked eastern cave entrance');assert.equal(s.world.publishedAdventure.oldLylek.tactics.approach,'east_cave_flank');});
 g.finishCombat({rng:()=>.5});
 const {repo:repo2,g:g2}=fixture();reachHuntersReady(g2);g2.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 const receipt=g2.resolveSWE02OldLylekHunterRetreat();assert.equal(receipt.route,'dotted map path');
 assert.equal(g2.state.combat.encounter.outcome,'escaped');assert.ok(g2.state.combat.adversaries.every(a=>a.status==='retreated'));
 g2.finishCombat({rng:()=>.5});
 reopenThree(repo2,s=>{assert.equal(s.world.publishedAdventure.oldLylek.hunterRetreatReceipt.route,'dotted map path');assert.equal(s.world.publishedAdventure.oldLylek.huntersOutcome,'retreated');});
});

test('p.17 repair Advantage leaves reduction amount to GM and persists an explicit chosen reduction',()=>{
 const enterRepair=g=>{g.chooseSWE02OldLylekRoute({slowDown:false});g.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',rng:()=>0});g.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',rng:()=>0});g.proceedSWE02OldLylekBluff();g.resolveSWE02OldLylekCrashCheck({skillKey:'aguante',rng:()=>0});};
 const {repo,g}=fixture();enterRepair(g);
 g.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',rng:()=>.5});
 let receipt=g.state.world.publishedAdventure.oldLylek.repairChecks.at(-1);
 assert.ok(receipt.netAdvantage>0);assert.equal(receipt.timeMinutes,30);assert.equal(receipt.advantageReduction.status,'PENDING_GM_CHOICE');
 reopenThree(repo,s=>assert.equal(s.world.publishedAdventure.oldLylek.repairChecks.at(-1).advantageReduction.status,'PENDING_GM_CHOICE'));
 const {repo:repo2,g:g2}=fixture();enterRepair(g2);
 g2.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',repairReductionMinutes:5,rng:()=>.5});
 receipt=g2.state.world.publishedAdventure.oldLylek.repairChecks.at(-1);assert.equal(receipt.timeMinutes,25);assert.equal(receipt.advantageReduction.minutes,5);assert.equal(receipt.advantageReduction.amountUnspecifiedByBook,true);
 reopenThree(repo2,s=>assert.equal(s.world.publishedAdventure.oldLylek.repairChecks.at(-1).timeMinutes,25));
});

test('any player Advantage automatically kills one hunter for prey when GM selected the beast branch, with persisted one-time receipt',()=>{
 const {repo,g}=fixture();reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 const before=g.state.combat.adversaries.map(a=>({id:a.id,status:a.status}));
 const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:before[0].id,rng:()=>.9});
 assert.ok(attack.lylekEvent);assert.equal(attack.lylekEvent.source.printedPage,18);assert.match(attack.lylekEvent.disposition,/prey taken/);
 const killed=g.state.combat.adversaries.find(a=>a.id===before[0].id);assert.equal(killed.status,'incapacitated');assert.equal(killed.swe02Fate.preyReturnedToNest,true);
 assert.equal(g.state.combat.adversaries.filter(a=>a.swe02KilledByLylek).length,1);
 reopenThree(repo,s=>{const old=s.world.publishedAdventure.oldLylek;assert.equal(old.lylekTriggered,true);assert.equal(old.lylekKillReceipt.hunterId,before[0].id);assert.equal(s.combat.adversaries.filter(a=>a.swe02KilledByLylek).length,1);});
 assert.equal(g._applySWE02OldLylekAdvantage(structuredClone(g.state.combat),{netAdvantage:1}).event,null);
});

test('New Meen conditional developer details require a PC question and persist with their printed pp.18–19 source',()=>{
 const {repo,g}=fixture();
 // Arrival is already covered by the full optional-lylek path; place this test at the same source-backed arrival state.
 reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 const combat=structuredClone(g.state.combat);combat.encounter.outcome='adversaries_defeated';for(const enemy of combat.adversaries)enemy.status='incapacitated';combat.swe02OldLylekAdvantageTriggered=true;
 g.commit('COMBAT_ACTION',{combat});g.finishCombat({rng:()=>.5});g.advancePublishedAdventure('reach-new-meen');
 assert.throws(()=>g.revealSWE02OldLylekDeveloperDetails({asked:false}),/perguntarem/);
 const facts=g.revealSWE02OldLylekDeveloperDetails({asked:true});assert.deepEqual(facts.source.printedPages,[18,19]);
 assert.match(facts.facts.contractor,/Angu Drombb/);assert.match(facts.facts.underworld,/ryll/);
 reopenThree(repo,s=>{const d=s.world.publishedAdventure.oldLylek.arrivalReceipt.developerDetails;assert.equal(d.status,'SHARED_AFTER_ASK');assert.deepEqual(d.source.printedPages,[18,19]);assert.match(d.facts.identity,/anônimo/);});
});

test('printed p.16 map exposes source-named zones and persists only explicit PC/hunter locations across three reopenings',()=>{
 const {repo,g}=fixture(2),crew=g.state.player.roster.entries.map(x=>x.id);
 g.state.group.companions=[{id:'npc-companion',name:'NPC Companion',character:structuredClone(g.state.player.character)}];repo.save(g.state);
 reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 const map=g.state.combat.swe02OldLylekMap,hunters=g.state.combat.adversaries,pcKey=`pc:${crew[0]}`,hunterKey=`hunter:${hunters[0].id}`;
 assert.deepEqual(map.zones.map(z=>z.id),['heroes_approach','cave_mouth','hunter_speeder','eastern_flank','southern_nest']);
 assert.equal(map.actors[pcKey].label,g.state.player.roster.entries[0].character.name);assert.equal(map.actorLocations[pcKey],'heroes_approach');
 assert.equal(map.actors[hunterKey].profileKey,'laoth_gand_bounty_hunter');assert.equal(map.actorLocations[hunterKey],'cave_mouth');
 assert.equal(map.fixedFeatures.huntersSpeeder,'hunter_speeder');assert.equal(map.fixedFeatures.lylekNest,'southern_nest');
 assert.deepEqual(map.geometry,{scale:null,distances:null,rangeBands:null,coverGeometry:null,lineOfSight:null,note:'Source-faithful named areas only; no scale, distance, range, cover geometry, or line-of-sight rules are inferred.'});
 assert.ok(!Object.keys(map.actors).some(key=>key.includes('npc-companion')));
 g.setSWE02OldLylekMapLocation({actorKey:pcKey,locationId:'eastern_flank'});
 g.setSWE02OldLylekMapLocation({actorKey:hunterKey,locationId:'hunter_speeder'});
 assert.throws(()=>g.setSWE02OldLylekMapLocation({actorKey:'pc:npc-companion',locationId:'cave_mouth'}),/áreas nomeadas|caçador registrado/);
 assert.throws(()=>g.setSWE02OldLylekMapLocation({actorKey:pcKey,locationId:'north_30m'}),/áreas nomeadas/);
 reopenThree(repo,s=>{const saved=s.combat.swe02OldLylekMap;assert.equal(saved.actorLocations[pcKey],'eastern_flank');assert.equal(saved.actorLocations[hunterKey],'hunter_speeder');assert.equal(saved.moves.length,2);assert.equal(saved.source.printedPage,16);assert.equal(saved.source.relatedPrintedPage,17);});
});

