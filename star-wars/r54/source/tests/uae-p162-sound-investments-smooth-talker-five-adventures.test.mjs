/** R54 P162 — source-backed AoR session income, social Triumph, Android and UAE. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {sourceBackedTalentRank,smoothTalkerRank,bindSmoothTalker,SMOOTH_TALKER_SKILLS,purchaseTalent} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const clone=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
class Repo{constructor(){this.saved=createCampaign('P162');this.fail=false;this.writes=0}load(){return clone(this.saved)}save(s){if(this.fail)throw Error('P162 storage failure');this.saved=clone(s);this.writes++}}
function game(purchases=[],credits=500){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');const c=clone(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];c.credits=credits;
 for(const [name,rank,choice=null] of purchases){const found=nodes.filter(n=>n.originalName===name).slice(0,rank);assert.equal(found.length,rank);for(const n of found){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,name:n.name,originalName:n.originalName,source:n.source,choice});}}
 g.setCharacter(c);return {g,repo};
}
function reopened(repo){const g=new GameController(repo);assert.equal(validateState(g.state),true);return g;}
function persists(repo,check){for(let i=0;i<3;i++)check(reopened(repo));}
test('P162: 360 positions preserved, four new source-backed positions active',()=>{
 assert.equal(aorTalentCoverage().positions,360);assert.ok(aorTalentCoverage().mechanicalPositions>=287);
 for(const [name,count] of [['LABIA',2],['INVERSIONES RENTABLES',2]])assert.equal(aorTalentCatalog().filter(x=>x.originalName===name&&x.mechanicallyActive).length,count);
});
test('P162: Sound Investments 2 ranks credits and session receipt are atomic',()=>{
 const {g,repo}=game([['INVERSIONES RENTABLES',2]],125);const r=g.startSession({rng:()=>0.999});assert.equal(r.soundInvestments.creditsGranted,200);assert.equal(g.state.player.character.credits,325);
 assert.deepEqual(g.state.session.campaignRules.soundInvestments,{rank:2,creditsGranted:200,sessionNumber:1});
 persists(repo,x=>{assert.equal(x.state.player.character.credits,325);assert.equal(x.state.session.campaignRules.soundInvestments.creditsGranted,200);assert.throws(()=>x.startSession({rng:()=>0.999}),/Já existe uma sessão/);});
 g.closeSession();g.startSession({rng:()=>0.999});assert.equal(g.state.player.character.credits,525);assert.equal(g.state.session.campaignRules.soundInvestments.sessionNumber,2);
 persists(repo,x=>assert.equal(x.state.player.character.credits,525));
});
test('P162: Sound Investments 0 and 1 rank grant exact amounts',()=>{
 for(const [rank,credits] of [[0,0],[1,100]]){const x=game(rank?[['INVERSIONES RENTABLES',rank]]:[],0);x.g.startSession({rng:()=>0.999});assert.equal(x.g.state.player.character.credits,credits);}
});
test('P162: Sound Investments cannot activate via display-only names or forged node',()=>{
 const x=game();const c=clone(x.g.state.player.character);c.talents.push('Investimentos rentáveis','Investimentos rentáveis');c.talentPurchases.push({nodeId:'ace:pilot:r1c4',originalName:'INVERSIONES RENTABLES'});x.g.setCharacter(c);
 assert.equal(sourceBackedTalentRank(x.g.state.player.character,'SOUNDINVEST'),0);x.g.startSession({rng:()=>0.999});assert.equal(x.g.state.player.character.credits,500);
});
test('P162: session start failure restores Destiny, income, character, session and repository',()=>{
 const {g,repo}=game([['INVERSIONES RENTABLES',2]],0),before=clone(g.state),writes=repo.writes;repo.fail=true;
 assert.throws(()=>g.startSession({rng:()=>0.999}),/storage failure/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P162: session start failure due to already active session is rollback safe',()=>{
 const {g,repo}=game([['INVERSIONES RENTABLES',2]],0);g.startSession({rng:()=>0.999});const before=clone(g.state),writes=repo.writes;
 assert.throws(()=>g.startSession({rng:()=>0.999}),/Já existe uma sessão/);assert.deepEqual(g.state,before);assert.equal(repo.writes,writes);
});
test('P162: credit overflow is rejected before commit',()=>{
 const {g,repo}=game([['INVERSIONES RENTABLES',2]],Number.MAX_SAFE_INTEGER-150),before=clone(g.state);assert.throws(()=>g.startSession({rng:()=>0.999}),/fora do limite/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
for(const skill of SMOOTH_TALKER_SKILLS)test(`P162: Lábia ranks are bound only to ${skill}`,()=>{
 const {g,repo}=game([['LABIA',2,skill]]);assert.equal(smoothTalkerRank(g.state.player.character,skill),2);
 for(const other of SMOOTH_TALKER_SKILLS.filter(x=>x!==skill))assert.equal(smoothTalkerRank(g.state.player.character,other),0);
 persists(repo,x=>assert.equal(smoothTalkerRank(x.state.player.character,skill),2));
});
test('P162: Lábia spends one real Triumph and adds ranked successes',()=>{
 const {g,repo}=game([['LABIA',2,'negociacion']]);const c=clone(g.state.player.character);c.skills.negociacion=3;g.setCharacter(c);
 const roll=g.resolveSkillCheck({skillKey:'negociacion',difficulty:0,spendSmoothTalkerTriumph:true,rng:()=>0.999});
 assert.ok(roll.triumph>=1);assert.equal(roll.smoothTalker.triumphSpent,1);assert.equal(roll.smoothTalker.bonusSuccesses,2);assert.equal(roll.remainingTriumph,roll.triumph-1);
 persists(repo,x=>assert.equal(x.state.lastCheck.smoothTalker.bonusSuccesses,2));
});
test('P162: Lábia never invents Triumph when check fails',()=>{
 const {g,repo}=game([['LABIA',1,'carisma']]);const r=g.resolveSkillCheck({skillKey:'carisma',difficulty:2,spendSmoothTalkerTriumph:true,rng:()=>0});
 assert.equal(r.triumph,0);assert.equal(r.smoothTalker.bonusSuccesses,0);assert.equal(r.smoothTalker.triumphSpent,0);persists(repo,x=>assert.equal(x.state.lastCheck.smoothTalker.triumphSpent,0));
});
test('P162: Lábia cannot be spent on an unpurchased skill or with fake boolean',()=>{
 const {g,repo}=game([['LABIA',1,'carisma']]),before=clone(g.state),writes=repo.writes;
 for(const opt of [{skillKey:'coaccion',spendSmoothTalkerTriumph:true},{skillKey:'carisma',spendSmoothTalkerTriumph:'yes'},{skillKey:'carisma',spendSmoothTalkerTriumph:true,spendKnowledgeTriumph:true}])assert.throws(()=>g.resolveSkillCheck({difficulty:0,rng:()=>0.999,...opt}));
 assert.deepEqual(g.state,before);assert.equal(repo.writes,writes);
});
test('P162: Lábia cannot be forged by translated labels and wrong original ID',()=>{
 const {g}=game();const c=clone(g.state.player.character);c.talents.push('Lábia');c.talentPurchases.push({nodeId:'ace:gunner:r1c1',originalName:'LABIA',choice:'carisma'});g.setCharacter(c);
 assert.equal(smoothTalkerRank(g.state.player.character,'carisma'),0);assert.throws(()=>g.resolveSkillCheck({skillKey:'carisma',difficulty:0,spendSmoothTalkerTriumph:true}),/não foi adquirida/);
});
test('P162: legacy Lábia node can be bound once without XP',()=>{
 const {g,repo}=game([['LABIA',1]]),node=g.state.player.character.talentPurchases[0].nodeId,xp=g.state.player.character.xp.available;
 assert.equal(g.assignSmoothTalker({nodeId:node,skillKey:'engano'}).skillKey,'engano');assert.equal(g.state.player.character.xp.available,xp);assert.equal(smoothTalkerRank(g.state.player.character,'engano'),1);
 persists(repo,x=>{assert.equal(smoothTalkerRank(x.state.player.character,'engano'),1);assert.throws(()=>x.assignSmoothTalker({nodeId:node,skillKey:'carisma'}),/já tem perícia/)});
});
test('P162: binding rejects foreign nodes, invalid skills and storage failures atomically',()=>{
 const {g,repo}=game([['LABIA',1]]),node=g.state.player.character.talentPurchases[0].nodeId,before=clone(g.state),writes=repo.writes;
 for(const args of [{nodeId:'fake',skillKey:'carisma'},{nodeId:node,skillKey:'mecanica'},{nodeId:node,skillKey:null}])assert.throws(()=>g.assignSmoothTalker(args));
 assert.deepEqual(g.state,before);assert.equal(repo.writes,writes);repo.fail=true;assert.throws(()=>g.assignSmoothTalker({nodeId:node,skillKey:'carisma'}),/storage failure/);assert.deepEqual(g.state,before);
});
test('P162: purchase of Lábia demands selected social skill',()=>{
 const x=game(),c=clone(x.g.state.player.character),node=nodes.find(n=>n.originalName==='LABIA');
 c.creation={...c.creation,careerKey:'diplomat',specializationKey:'quartermaster'};c.specializations=['quartermaster'];c.xp.available=100;
 assert.throws(()=>purchaseTalent(c,node.id,{careerKey:'diplomat',specializationKey:'quartermaster',skillKey:'mecanica'}));
});
for(const {id} of publishedAdventureCatalog())test(`P162: ${id} UAE objectives, triggers, consequences, replay and three reopenings`,()=>{
 const mod=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 mod.initialObjectives.p162={title:'Verificação P162',type:'SECONDARY',target:2};mod.initialClocks.p162={value:0,max:2,status:'ACTIVE'};
 mod.triggers.push({id:'p162-trigger',event:'P162_SIGNAL',maxFires:2,effects:[{type:'progress-objective',objectiveId:'p162',delta:1},{type:'advance-clock',clockId:'p162',delta:1}],consequence:{id:'p162-consequence',description:'Consequência P162'}});
 const runtime=new AdventureRuntime(mod);for(let i=1;i<=2;i++)assert.equal(runtime.commitAction(`${id}:p162:${i}`,[{type:'event',eventType:'P162_SIGNAL'}]).applied,true);
 const saved=runtime.getState();assert.equal(saved.objectives.p162.status,'COMPLETED');assert.equal(saved.clocks.p162.status,'COMPLETED');assert.equal(validateAdventureCheckpoint(mod,saved),true);
 for(let i=0;i<3;i++){const restored=AdventureRuntime.restore(mod,runtime.serialize());assert.equal(restored.commitAction(`${id}:p162:2`,[{type:'event',eventType:'P162_SIGNAL'}]).applied,false);assert.deepEqual(restored.getState(),saved);}
 assert.throws(()=>runtime.commitAction(`${id}:p162:bad`,[{type:'complete-objective',objectiveId:'missing'}]));assert.deepEqual(runtime.getState(),saved);
});
test('P162: Android bridge binds Lábia, rolls, and offline bundle exposes methods',()=>{
 const {g,repo}=game([['LABIA',1]]),node=g.state.player.character.talentPurchases[0].nodeId;
 const bridge=new BraseiroUiBridge({repository:repo,storage:null});assert.equal(bridge.assignSmoothTalker({nodeId:node,skillKey:'carisma'}).ok,true);
 const check=bridge.resolveSkillCheck({skillKey:'carisma',difficulty:0,spendSmoothTalkerTriumph:true,rng:()=>0.999});assert.equal(check.ok,true);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const token of ['data-p162-panel','data-p162-skill','data-p162-check','data-p162-bind','INVESTIMENTOS RENTÁVEIS'])assert.ok(ui.includes(token),token);
 for(const token of ['assignSmoothTalker','spendSmoothTalkerTriumph','soundInvestments'])assert.ok(bundle.includes(token),token);assert.ok(Number(bundle.match(/modules:(\d+)/)?.[1]??0)>=80,'O bundle não pode perder módulos da base P172');
});
