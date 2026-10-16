import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {npcBlastReceipt} from '../app/src/main/assets/vtt/js/rules/aorP179Reactions.mjs';
const cp=structuredClone;
const talents=['SUPERVIVIENTE','CUERPO A TIERRA','CUERPO A TIERRA MEJORADO','NO ES PARA TANTO'];
const catalog=aorTalentCatalog();
class Repo {constructor(){this.saved=null;this.fail=false;}load(){return this.saved===null?null:cp(this.saved);}save(state){if(this.fail)throw Error('falha de armazenamento P179');this.saved=cp(state);}}
function fixture(owned=talents,{combat=false,companion=true}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=cp(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=owned.map(name=>{const node=catalog.find(n=>n.originalName===name);assert.ok(node,name);return {nodeId:node.id,originalName:name,name:node.name};});
 c.talents=c.talentPurchases.map(p=>p.name);c.characteristics.intelecto=5;c.skills.medicina=5;
 c.skills.aguante=2;c.skills.supervivencia=2;
 g.setCharacter(c);g.setCampaignMode('AOR');g.startSession({rng:()=>0.1});
 let ally=null;
 if(companion)ally=g.addCompanion({name:'Mika',woundsThreshold:2,strainThreshold:10});
 if(combat){const a=createAdversary('imperial_soldier',{range:'Curto'});a.weapons=[a.weapons[1]];
  const cb=cp(g.state.combat);cb.active=true;cb.round=1;cb.adversaries=[a];g.commit('COMBAT_UPDATED',{combat:cb});}
 return {g,repo,ally};
}
function pendingBlast(g,rng=()=>0.9){
 const combat=cp(g.state.combat),a=combat.adversaries[0];
 const result=g._resolveNpcTurn(combat,g.state.player.character,a.id,rng,g.state.group);
 assert.equal(result.combat.pendingNpcBlasts?.length,1,'a real NPC blast receipt should be issued');
 g.commit('COMBAT_ACTION',{combat:result.combat,character:result.character,group:result.group??g.state.group});
 return result.combat.pendingNpcBlasts[0];
}
test('P179 360 AoR source positions have executable consumers (not editorial certification)',()=>{
 const c=aorTalentCoverage();assert.equal(c.positions,360);assert.equal(c.mechanicalPositions,360);
 for(const n of talents)assert.ok(catalog.some(x=>x.originalName===n&&x.mechanicallyActive),n);
});
test('P179 Tactician Superviviente is Natural Outdoorsman, not Unmatched Courage',()=>{
 const {g}=fixture(['SUPERVIVIENTE']);const c=g.state.player.character;
 assert.equal(sourceBackedTalentRank(c,'NATOUT'),1);
 assert.equal(sourceBackedTalentRank(c,'UNMATCHED_COURAGE'),0);
 const r=g.resolveSkillCheck({skillKey:'supervivencia',difficulty:2,useNaturalReroll:true,rng:()=>0.1});
 assert.equal(r.naturalReroll?.used,true);
});
test('P179 Medic blocks a real threshold critical with Hard Medicine, not the wound damage',()=>{
 const {g,repo,ally}=fixture();const before=cp(g.state);
 const r=g.applyCompanionDamage({companionId:ally.id,damage:4,useItsNotThatBad:true,approved:true,medicineRng:()=>0.1,criticalRng:()=>0.2});
 assert.equal(r.criticalAdded,false);assert.equal(r.criticalPrevention?.prevented,true);
 assert.equal(g.state.group.companions[0].character.derived.wounds.current,4);
 assert.equal(g.state.group.companions[0].character.criticalInjuries.length,0);
 assert.equal(g.state.player.character.sessionEffects.talentUses.itsNotThatBad,true);
 assert.equal(g.state.combat.log.at(-1).type,'AOR_ITS_NOT_THAT_BAD');
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
 assert.notDeepEqual(g.state,before);
});
test('P179 Medic failed Medicine check consumes the session use and leaves the critical intact',()=>{
 const {g,ally}=fixture();const r=g.applyCompanionDamage({companionId:ally.id,damage:4,useItsNotThatBad:true,approved:true,medicineRng:()=>0.9,criticalRng:()=>0.2});
 assert.equal(r.criticalAdded,true);assert.equal(r.criticalPrevention,null);
 assert.equal(g.state.player.character.sessionEffects.talentUses.itsNotThatBad,true);
 assert.equal(g.state.group.companions[0].character.criticalInjuries.length,1);
 assert.equal(g.state.combat.log.at(-1).success,false);
});
test('P179 Medic does not consume once/session when no critical event occurs',()=>{
 const {g,ally}=fixture();g.applyCompanionDamage({companionId:ally.id,damage:1,useItsNotThatBad:true,approved:true,medicineRng:()=>0.1});
 assert.equal(g.state.player.character.sessionEffects?.talentUses?.itsNotThatBad,undefined);
});
test('P179 Medic rejects self, unowned, repeated, unapproved, and invalid targets atomically',()=>{
 const {g,ally}=fixture();for(const opts of [{companionId:'ghost',damage:4,useItsNotThatBad:true,approved:true},
  {companionId:ally.id,damage:4,useItsNotThatBad:true,approved:false}]){
  const old=cp(g.state);assert.throws(()=>g.applyCompanionDamage(opts));assert.deepEqual(g.state,old);
 }
 const {g:unowned,ally:target}=fixture([]);const old=cp(unowned.state);
 assert.throws(()=>unowned.applyCompanionDamage({companionId:target.id,damage:4,useItsNotThatBad:true,approved:true}),/não é tão grave/i);
 assert.deepEqual(unowned.state,old);
 g.applyCompanionDamage({companionId:ally.id,damage:4,useItsNotThatBad:true,approved:true,medicineRng:()=>0.1});
 const used=cp(g.state);assert.throws(()=>g.applyCompanionDamage({companionId:ally.id,damage:1,useItsNotThatBad:true,approved:true}),/não é tão grave/i);
 assert.deepEqual(g.state,used);
});
test('P179 Medicine action rollback restores ally and medic on failed persistent save',()=>{
 const {g,repo,ally}=fixture();const old=cp(g.state);repo.fail=true;
 assert.throws(()=>g.applyCompanionDamage({companionId:ally.id,damage:4,useItsNotThatBad:true,approved:true,medicineRng:()=>0.1}),/armazenamento/);
 assert.deepEqual(g.state,old);assert.deepEqual(repo.saved,old);
});
test('P179 prepared Medic reacts during actual NPC attack and persists once/session',()=>{
 const {g,repo,ally}=fixture(talents,{combat:true});
 g.prepareItsNotThatBad({companionId:ally.id,approved:true});
 const cb=cp(g.state.combat);cb.npcCompanionTargetId=ally.id;
 const npc=g._resolveNpcTurn(cb,g.state.player.character,cb.adversaries[0].id,()=>0.2,g.state.group);
 assert.equal(npc.attack.hit,true);
 assert.equal(npc.combat.aorCriticalAid,undefined);
 assert.equal(npc.character.sessionEffects.talentUses.itsNotThatBad,true);
 assert.ok(npc.combat.log.some(e=>e.type==='AOR_ITS_NOT_THAT_BAD'));
 g.commit('COMBAT_ACTION',{combat:npc.combat,character:npc.character,group:npc.group});
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
});
test('P179 preparing Medic is atomic and validates real ally, ownership and GM approval',()=>{
 const {g,repo,ally}=fixture(talents,{combat:true});const old=cp(g.state);
 assert.throws(()=>g.prepareItsNotThatBad({companionId:ally.id,approved:false}),/Mestre/);
 assert.throws(()=>g.prepareItsNotThatBad({companionId:'ghost',approved:true}),/companheiro/i);
 assert.deepEqual(g.state,old);repo.fail=true;
 assert.throws(()=>g.prepareItsNotThatBad({companionId:ally.id,approved:true}),/armazenamento/);
 assert.deepEqual(g.state,old);
});
test('P179 NPC blast receipt is derived from a real grenade and real attack Advantage',()=>{
 const {g,repo}=fixture(talents,{combat:true});const r=pendingBlast(g);
 assert.equal(r.blastDamage,6);assert.equal(r.hit,false);assert.equal(r.advantageSpent,3);
 assert.equal(r.sourceAdversaryId,g.state.combat.adversaries[0].id);
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
});
test('P179 NPC blast requires actual Advantage and Blast quality, not a GM fabricated event',()=>{
 const {g}=fixture(talents,{combat:true});const a=g.state.combat.adversaries[0];
 assert.equal(npcBlastReceipt({attackId:'x',attacker:a,weapon:a.weapons[0],check:{netAdvantage:1},hit:true,sessionId:g.state.sessionId,round:1}),null);
 assert.equal(npcBlastReceipt({attackId:'x',attacker:a,weapon:{name:'Blaster',damage:5},check:{netAdvantage:5},hit:true,sessionId:g.state.sessionId,round:1}),null);
});
test('P179 Time to Go escapes NPC blast, spends exactly one light Destiny and saves companion via Improved',()=>{
 const {g,repo,ally}=fixture(talents,{combat:true});const receipt=pendingBlast(g);
 const group=cp(g.state.group);group.destiny={light:2,dark:0};g.commit('GROUP_UPDATED',{group});
 const before=cp(g.state.group.companions[0].character.derived.wounds.current);
 const result=g.resolveIncomingBlast({attackId:receipt.attackId,includePlayer:true,affectedCompanionIds:[ally.id],
  useTimeToGo:true,escapeConfirmed:true,improvedCompanionId:ally.id,companionEscapeConfirmed:true,engagedConfirmed:true,approved:true});
 assert.equal(result.results.length,2);assert.equal(result.results.every(r=>r.escaped===true),true);
 assert.equal(g.state.group.destiny.light,1);assert.equal(g.state.group.destiny.dark,1);
 assert.equal(g.state.group.companions[0].character.derived.wounds.current,before);
 assert.equal(g.state.combat.pendingNpcBlasts.length,0);
 assert.deepEqual(g.state.combat.aorBlastEscapes.map(e=>e.actorId),['player',ally.id]);
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
 const after=cp(g.state);assert.throws(()=>g.resolveIncomingBlast({attackId:receipt.attackId,approved:true}),/recibo/i);assert.deepEqual(g.state,after);
});
test('P179 Blast with no Time to Go damages real player and companion with soak',()=>{
 const {g,ally}=fixture(talents,{combat:true});const receipt=pendingBlast(g);
 const wounds=g.state.player.character.derived.wounds.current;
 const result=g.resolveIncomingBlast({attackId:receipt.attackId,includePlayer:true,affectedCompanionIds:[ally.id],approved:true});
 assert.equal(result.damage,6);assert.equal(result.results.length,2);
 assert.ok(g.state.player.character.derived.wounds.current>wounds);
 assert.equal(g.state.group.companions[0].character.derived.wounds.current,4); // capped at 2× wound threshold
});
test('P179 Blast refuses phantom, duplicate, unsupported and unapproved escapes atomically',()=>{
 const {g,ally}=fixture(talents,{combat:true});const r=pendingBlast(g);const group=cp(g.state.group);group.destiny={light:2,dark:0};g.commit('GROUP_UPDATED',{group});
 for(const args of [
  {attackId:r.attackId,approved:false,includePlayer:true},
  {attackId:r.attackId,approved:true,includePlayer:true,affectedCompanionIds:['phantom']},
  {attackId:r.attackId,approved:true,includePlayer:true,affectedCompanionIds:[ally.id,ally.id]},
  {attackId:r.attackId,approved:true,includePlayer:true,useTimeToGo:true,escapeConfirmed:false},
  {attackId:r.attackId,approved:true,includePlayer:true,useTimeToGo:true,escapeConfirmed:true,improvedCompanionId:ally.id,affectedCompanionIds:[ally.id],engagedConfirmed:false},
  {attackId:'forged',approved:true,includePlayer:true},
 ]){const before=cp(g.state);assert.throws(()=>g.resolveIncomingBlast(args));assert.deepEqual(g.state,before);}
});
test('P179 Blast rejects an unowned Time to Go and preserves Destiny',()=>{
 const {g}=fixture([],{combat:true});const r=pendingBlast(g);const group=cp(g.state.group);group.destiny={light:2,dark:0};g.commit('GROUP_UPDATED',{group});
 const before=cp(g.state);assert.throws(()=>g.resolveIncomingBlast({attackId:r.attackId,includePlayer:true,useTimeToGo:true,escapeConfirmed:true,approved:true}),/talento/);
 assert.deepEqual(g.state,before);
});
test('P179 Blast cannot be resolved without light Destiny, even when all targets would escape',()=>{
 const {g}=fixture(talents,{combat:true});const r=pendingBlast(g);const group=cp(g.state.group);group.destiny={light:0,dark:2};g.commit('GROUP_UPDATED',{group});
 const before=cp(g.state);assert.throws(()=>g.resolveIncomingBlast({attackId:r.attackId,includePlayer:true,useTimeToGo:true,escapeConfirmed:true,approved:true}),/Destino/);
 assert.deepEqual(g.state,before);
});
test('P179 blast rollback restores spent Destiny, wounds and attack receipt',()=>{
 const {g,repo,ally}=fixture(talents,{combat:true});const r=pendingBlast(g);const group=cp(g.state.group);group.destiny={light:2,dark:0};g.commit('GROUP_UPDATED',{group});
 const before=cp(g.state);repo.fail=true;
 assert.throws(()=>g.resolveIncomingBlast({attackId:r.attackId,includePlayer:true,affectedCompanionIds:[ally.id],useTimeToGo:true,escapeConfirmed:true,approved:true}),/armazenamento/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P179 forged pending blast and forged Medic preparation rejected by state validator',()=>{
 const {g,ally}=fixture(talents,{combat:true});const forged=cp(g.state);forged.combat.pendingNpcBlasts=[{attackId:'fake',blastDamage:10}];
 assert.throws(()=>validateState(forged),/Explosão/);
 const forgedAid=cp(g.state);forgedAid.combat.aorCriticalAid={companionId:ally.id,sessionId:g.state.sessionId};
 assert.throws(()=>validateState(forgedAid),/medicina reativa/);
});
test('P179 combat closure removes pending blast, prepared Medic and encounter-only escape receipts',()=>{
 const {g,ally}=fixture(talents,{combat:true});pendingBlast(g);
 g.prepareItsNotThatBad({companionId:ally.id,approved:true});
 assert.ok(g.state.combat.pendingNpcBlasts.length);
 const result=g.finishCombat({rng:()=>0.1});
 assert.equal(g.state.combat.active,false);
 assert.equal(g.state.combat.pendingNpcBlasts,undefined);
 assert.equal(g.state.combat.aorCriticalAid,undefined);
 assert.equal(g.state.combat.aorBlastEscapes,undefined);
 assert.equal(validateState(g.state),true);
});
test('P179 rejects forged blast escape without a matching resolved attack',()=>{
 const {g}=fixture(talents,{combat:true});const old=cp(g.state);
 old.combat.aorBlastEscapes=[{attackId:'fake',actorId:'player',sessionId:g.state.sessionId,round:1}];
 assert.throws(()=>validateState(old),/Deslocamento reativo/);
});
test('P179 UI bundle and Android source include actual controller reaction bridge',()=>{
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 for(const key of ['aorP179Status','prepareItsNotThatBad','resolveIncomingBlast','ITS_NOT_THAT_BAD','TIME_TO_GO_IMPROVED'])assert.ok(bundle.includes(key),key);
 assert.ok(ui.includes('P179 — AoR reactive Medicine'));assert.ok(ui.includes('Mestre confirma saída real da zona'));
});
for(const {id} of publishedAdventureCatalog())test(`P179 ${id}: critical reaction and blast preserve adventure runtime after 3 reopens`,()=>{
 const {g,repo,ally}=fixture(talents,{combat:false});g.setCampaignMode('MIXED');g.startPublishedAdventureSession(id,{rng:()=>0.5});
 const before=cp(g.state.world.publishedAdventureRuntime);
 g.applyCompanionDamage({companionId:ally.id,damage:4,useItsNotThatBad:true,approved:true,medicineRng:()=>0.1});
 assert.deepEqual(g.state.world.publishedAdventureRuntime,before);
 for(let i=0;i<3;i++){const again=new GameController(repo);assert.equal(validateState(again.state),true);assert.deepEqual(again.state.world.publishedAdventureRuntime,before);}
});
