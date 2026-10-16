/** P168: AoR six Diplomat social nodes + two Solid Repairs nodes. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {startCombat} from '../app/src/main/assets/vtt/js/rules/combat.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {applyDamageControl,applyManualRepairs} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import {activeInspiringBoost,activeScathingSetback} from '../app/src/main/assets/vtt/js/rules/aorSocialTalents.mjs';
const copy=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(specs=>Object.values(specs).flatMap(tree=>tree.nodes));
const names={inspiring:['RETÓRICA INSPIRADORA','RETÓRICA INSPIRADORA MEJORADA','RETÓRICA INSPIRADORA SUPREMA'],
 scathing:['LENGUA AFILADA','LENGUA AFILADA MEJORADA','LENGUA AFILADA SUPREMA'],
 solid:['DE UNA PIEZA']};
class Repo{constructor(){this.saved=createCampaign('P168');this.fail=false;this.writes=0;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('P168 disk failure');this.saved=copy(s);this.writes++;}}
function game({kind='scathing',improved=true,supreme=true,close=true,companionStrain=0}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talentPurchases=[];c.talents=[];c.skills.coaccion=3;c.skills.liderazgo=3;c.characteristics.presencia=4;
 for(const [index,name] of names[kind].entries()){
  if(index===1&&!improved||index===2&&!supreme)continue;
  const n=nodes.find(n=>n.originalName===name);assert.ok(n);
  c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});c.talents.push(n.name);
 }
 g.setCharacter(c);g.setCampaignMode('AOR');
 const a=createAdversary('son_barab',{range:close?'Curto':'Longo'});
 const b=createAdversary('imperial_officer',{range:'Curto'});
 g.commit('COMBAT_UPDATED',{combat:{...startCombat(g.state.combat),adversaries:[a,b]}});
 const companion=g.addCompanion({name:'Apoio'});
 if(companionStrain){const group=copy(g.state.group);group.companions[0].character.derived.strain.current=companionStrain;g.commit('GROUP_UPDATED',{group});}
 return {g,repo,a,b,companion};
}
const lucky=()=>0.25; // deterministic positive dice and usually nonzero net successes
const unlucky=()=>0.999999;
test('P168: six Diplomat positions and two Solid Repairs positions are executable, not mere labels',()=>{
 assert.equal(aorTalentCoverage().positions,360);
 assert.ok(aorTalentCoverage().mechanicalPositions>=312);
 for(const n of [...names.inspiring,...names.scathing,'DE UNA PIEZA']){
  const count=aorTalentCatalog().filter(x=>x.originalName===n&&x.mechanicallyActive).length;
  assert.equal(count,n==='DE UNA PIEZA'?2:1,n);
 }
});
test('P168: source-backed social talent blocks forged labels and wrong-tree purchases',()=>{
 const {g,companion}=game({kind:'inspiring'});const c=copy(g.state.player.character);
 c.talentPurchases=[];c.talents=['Retórica inspiradora','Retórica inspiradora suprema'];
 g.setCharacter(c);assert.equal(sourceBackedTalentRank(g.state.player.character,'INSPIRINGRHET'),0);
 assert.throws(()=>g.performAoRSocialTalent({kind:'inspiring',targetIds:[companion.id],confirmedShortRange:true}),/não adquirido/);
});
test('P168: Scathing Tirade requires real short-range adversary, uses action, persists real strain and nonstacking setback',()=>{
 const {g,repo,a,b}=game();assert.throws(()=>g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id,a.id]}),/distintos/);
 const result=g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id,b.id],rng:lucky});
 assert.equal(g.state.combat.playerTurn.actionUsed,true);
 assert.equal(result.check.kind,undefined); // uses canonical skill engine
 assert.ok(result.check.netSuccess>=0);
 assert.equal(result.affected.length,Math.min(2,Math.max(0,result.check.netSuccess)));
 for(const affected of result.affected){const adv=g.state.combat.adversaries.find(x=>x.id===affected.id);assert.ok(adv.strain?.current>0||adv.wounds.current>0);assert.equal(activeScathingSetback(adv,1),1);}
 for(let i=0;i<3;i++){const restored=new GameController(repo);assert.equal(validateState(restored.state),true);assert.deepEqual(restored.state.combat.adversaries,g.state.combat.adversaries);}
 assert.throws(()=>g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id],rng:lucky}),/ação.*usada/);
});
test('P168: Scathing refuses long range and does not charge action on validation failure',()=>{
 const {g,a}=game({close:false});const before=copy(g.state);
 assert.throws(()=>g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id]}),/fora do alcance/);
 assert.deepEqual(g.state,before);
});
test('P168: Supreme Scathing costs one voluntary strain and a maneuver instead of action',()=>{
 const {g,a}=game();const before=g.state.player.character.derived.strain.current;
 const r=g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id],asManeuver:true,rng:lucky});
 assert.equal(r.asManeuver,true);assert.equal(g.state.combat.playerTurn.actionUsed,false);
 assert.equal(g.state.combat.playerTurn.maneuversUsed,1);
 assert.equal(g.state.player.character.derived.strain.current,before+1);
 assert.throws(()=>g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id],asManeuver:true,rng:lucky}),/segunda manobra/);
});
test('P168: Inspiring Rhetoric recovers actual companion strain and improves companion dice pool',()=>{
 const {g,repo,companion}=game({kind:'inspiring',companionStrain:5});
 const result=g.performAoRSocialTalent({kind:'inspiring',targetIds:[companion.id],confirmedShortRange:true,rng:lucky});
 assert.equal(result.check.netSuccess>0,true);
 const member=g.state.group.companions[0];assert.ok(member.character.derived.strain.current<5);
 assert.equal(activeInspiringBoost(member.character,1,g.state.combat.socialEncounterSerial),1);
 const check=g.resolveCompanionSkillCheck({companionId:companion.id,skillKey:'percepcion',rng:lucky});
 assert.equal(check.inspiringBoost,1);
 for(let i=0;i<3;i++){const restored=new GameController(repo);assert.equal(validateState(restored.state),true);assert.equal(restored.state.group.companions[0].character.derived.strain.current,member.character.derived.strain.current);}
});
test('P168: Inspiring cannot affect a companion without explicit proximity confirmation',()=>{
 const {g,companion}=game({kind:'inspiring'}),before=copy(g.state);
 assert.throws(()=>g.performAoRSocialTalent({kind:'inspiring',targetIds:[companion.id]}),/Confirme/);
 assert.deepEqual(g.state,before);
});
test('P168: improved and supreme versions require separate authentic source purchases',()=>{
 const {g,a}=game({kind:'scathing',improved:false,supreme:false});
 assert.throws(()=>g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id],asManeuver:true}),/Versão suprema/);
 const r=g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id],rng:lucky});
 assert.ok(r.affected.every(x=>x.setbackUntilRound===null));
});
test('P168: failed storage write rolls back check, costs, adversary/companion state and logs atomically',()=>{
 for(const kind of ['scathing','inspiring']){
  const {g,repo,a,companion}=game({kind,companionStrain:5}),before=copy(g.state);
  repo.fail=true;
  assert.throws(()=>g.performAoRSocialTalent({kind,targetIds:[kind==='scathing'?a.id:companion.id],confirmedShortRange:true,rng:lucky}),/disk failure/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
 }
});
test('P168: two Solid Repairs AoR nodes improve successful hull repairs but never heal on failure or forged label',()=>{
 const {g}=game();let c=copy(g.state.player.character);c.talentPurchases=[];c.talents=[];
 const v={id:'test',key:'xwing_t65b'}; // use a real validated ship from the runtime
 g.addVehicle('xwing_t65b');const vehicle=copy(g.activeVehicle());vehicle.hullDamage=7;
 for(const n of nodes.filter(n=>n.originalName==='DE UNA PIEZA')){
  c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});c.talents.push(n.name);
 }
 assert.equal(sourceBackedTalentRank(c,'SOLREP'),2);
 assert.equal(applyDamageControl(vehicle,{successes:1,operator:c,mode:'hull'}).hullRecovered,3);
 assert.equal(applyDamageControl(vehicle,{successes:0,operator:c,mode:'hull'}).hullRecovered,0);
 assert.equal(applyManualRepairs(vehicle,{successes:1,operator:c}).hullRecovered,3);
 c.talentPurchases=[];assert.equal(applyDamageControl(vehicle,{successes:1,operator:c,mode:'hull'}).hullRecovered,1);
});

test('P168: Intense Presence spends one light Destiny and recovers only Presence strain, transactionally',()=>{
 const {g,repo}=game({kind:'inspiring'});let c=copy(g.state.player.character);
 const node=nodes.find(x=>x.originalName==='PRESENCIA IMPONENTE');assert.ok(node);
 c.talentPurchases.push({nodeId:node.id,originalName:node.originalName,name:node.name,source:node.source});
 c.talents.push(node.name);c.derived.strain.current=6;g.setCharacter(c);
 const group=copy(g.state.group);group.destiny={light:3,dark:2};g.commit('GROUP_UPDATED',{group});
 const before=copy(g.state);
 const result=g.useIntensePresence();assert.equal(result.recovered,4);
 assert.equal(g.state.player.character.derived.strain.current,2);
 assert.equal(g.state.group.destiny.light,2);assert.equal(g.state.group.destiny.dark,3);
 const restored=new GameController(repo);assert.equal(restored.state.player.character.derived.strain.current,2);
 repo.fail=true;assert.throws(()=>g.useIntensePresence(),/disk failure/);assert.equal(g.state.player.character.derived.strain.current,2);
});
test('P168: Inspiring bonus cannot reactivate in a later encounter with the same round number',()=>{
 const {g,companion}=game({kind:'inspiring',companionStrain:5});
 g.performAoRSocialTalent({kind:'inspiring',targetIds:[companion.id],confirmedShortRange:true,rng:lucky});
 const first=g.state.combat.socialEncounterSerial,member=g.state.group.companions[0];
 assert.equal(activeInspiringBoost(member.character,1,first),1);
 g.commit('COMBAT_UPDATED',{combat:startCombat(g.state.combat)});
 assert.equal(g.state.combat.socialEncounterSerial,first+1);
 assert.equal(activeInspiringBoost(g.state.group.companions[0].character,1,g.state.combat.socialEncounterSerial),0);
});
test('P168: UI bridge exposes social, companion and Intense Presence APIs',()=>{
 const {g,repo,companion}=game({kind:'inspiring',companionStrain:5});
 const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 assert.equal(bridge.aorSocialStatus().ok,true);
 const result=bridge.performAoRSocialTalent({kind:'inspiring',targetIds:[companion.id],confirmedShortRange:true,rng:lucky});
 assert.equal(result.ok,true);
 assert.equal(bridge.resolveCompanionSkillCheck({companionId:companion.id,skillKey:'percepcion',difficulty:2,rng:lucky}).ok,true);
 assert.equal(bridge.useIntensePresence().ok,false);
});
test('P168: invalid social effect markers rejected rather than silently normalized',()=>{
 const {g}=game();for(const value of [-1,Infinity,'3',null]){
  const state=copy(g.state);state.combat.adversaries[0].aorScathingUntilRound=value;
  assert.throws(()=>validateState(state),/AoR de reprimenda/);
 }
});

test('P168: improved Scathing setback affects NPC combat, social and computer checks, but expires by round',()=>{
 const {g,a}=game({kind:'scathing'});
 const r=g.performAoRSocialTalent({kind:'scathing',targetIds:[a.id],rng:lucky});assert.equal(r.affected.length,1);
 const npc=g.state.combat.adversaries.find(x=>x.id===a.id);
 assert.equal(activeScathingSetback(npc,1),1);
 const social=g.resolveIncomingSocialInfluenceCheck({skillKey:'coaccion',actorAdversaryId:a.id,rng:lucky});
 assert.equal(social.check.pool.setback,1);
 const cyber=g.resolveIncomingComputerIntrusionCheck({actorAdversaryId:a.id,defendingOwnedSystem:true,rng:lucky});
 assert.ok(cyber.check.pool.setback>=1);
 assert.equal(activeScathingSetback(npc,4),0);
});
