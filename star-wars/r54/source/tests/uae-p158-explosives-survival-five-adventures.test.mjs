/** P158 — integrated, source-backed AoR explosives, survival and five-adventure regression. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {talentRankByOriginal,talentSkillPoolModifiers,selectiveDetonationOption,masterGrenadierBlastCost} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {validatePublishedAdventureGraph} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
const copy=structuredClone;
class Repo {constructor(id='p158'){this.saved=createCampaign(id);this.writes=0;this.fail=false;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('P158 storage failure');this.saved=copy(s);this.writes++}}
function fixture(names=[],id='p158'){
 const repo=new Repo(id),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];
 for(const [name,rank] of names){const selected=nodes.filter(n=>n.originalName===name).slice(0,rank);assert.equal(selected.length,rank,name);
  for(const n of selected){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});}}
 g.setCharacter(c);return {g,repo};
}
function encounter(names=[],targets=3){const x=fixture(names);const enemies=Array.from({length:targets},(_,i)=>createAdversary('imperial_officer',{name:`Alvo P158 ${i}`,range:'Interação'}));x.g.beginEncounter({adversaries:enemies,rng:()=>0});return {...x,enemies};}
function pending(x,{hit=true,advantage=4,triumph=1,qualityCost=2}={}){
 const a=x.enemies[0];x.g.state.combat.pendingPlayerSpend={attackId:'p158-attack',adversaryId:a.id,weaponIndex:0,weapon:'granada',remainingAdvantage:advantage,remainingTriumph:triumph,
  qualities:[{key:'blast',label:'Explosão',rank:5,cost:qualityCost}],activations:{},attackSnapshot:{hit,damage:7,pierce:0,damageType:'wounds',baseDamage:6,netSuccess:1}};
 x.repo.save(x.g.state);
}
test('P158: six source slots become mechanically active, all other pending slots remain fail closed',()=>{
 const coverage=aorTalentCoverage();assert.equal(coverage.positions,360);assert.ok(coverage.mechanicalPositions>=268);
 for(const [name,canonical,count] of [['DETONACIÓN SELECTIVA','SELECTDET',2],['GRANADERO EXPERTO','MASTERGREN',1],['ACLIMATACIÓN','ACCLIM',2],['RESISTENCIA A VENENOS','POISONRES',1]]){
  assert.equal(aorCanonicalTalentName(name),canonical);assert.equal(aorTalentCatalog().filter(n=>n.originalName===name&&n.mechanicallyActive).length,count);
  assert.equal(talentRankByOriginal(fixture([[name,count]]).g.state.player.character,canonical),count);
 }
 for(const name of ['TENACIDAD','GUARDAESPALDAS'])assert.equal(aorTalentCatalog().filter(n=>n.originalName===name&&n.mechanicallyActive).length,2); // P170
});
for(const rank of [0,1,2])test(`P158: Aclimatação ${rank} removes at most two setback in foraging only`,()=>{
 const c=fixture(rank?[['ACLIMATACIÓN',rank]]:[]).g.state.player.character;
 assert.equal(talentSkillPoolModifiers(c,'supervivencia',{foraging:true,setback:4}).setback,4-(rank?2:0));
 assert.equal(talentSkillPoolModifiers(c,'supervivencia',{setback:4}).setback,4);
 assert.equal(talentSkillPoolModifiers(c,'mecanica',{foraging:true,setback:4}).setback,4);
 assert.equal(talentSkillPoolModifiers(c,'supervivencia',{foraging:true,setback:1}).setback,rank?0:1);
});
for(const rank of [0,1])test(`P158: Poison Resistance ${rank} grants Boost only on toxin resistance`,()=>{
 const c=fixture(rank?[['RESISTENCIA A VENENOS',rank]]:[]).g.state.player.character;
 assert.equal(talentSkillPoolModifiers(c,'aguante',{poisonResistance:true}).boost,rank);
 assert.equal(talentSkillPoolModifiers(c,'aguante',{}).boost,0);
 assert.equal(talentSkillPoolModifiers(c,'disciplina',{poisonResistance:true}).boost,0);
});
test('P158: foraging and poison flags change actual dice, persist and survive three reopens',()=>{
 const {g,repo}=fixture([['ACLIMATACIÓN',2],['RESISTENCIA A VENENOS',1]]);
 const a=g.resolveSkillCheck({skillKey:'supervivencia',foraging:true,setback:3,rng:()=>0.5});assert.equal(a.pool.setback,1);
 const b=g.resolveSkillCheck({skillKey:'aguante',poisonResistance:true,setback:1,rng:()=>0.5});assert.equal(b.pool.boost,1);
 for(let i=0;i<3;i++){const again=new GameController(repo);assert.equal(validateState(again.state),true);assert.equal(again.state.player.character.talentPurchases.length,3);}
});
for(const bad of [{foraging:'true'},{poisonResistance:1},{foraging:[]},{poisonResistance:null}])test(`P158: forged skill context ${JSON.stringify(bad)} rejected without writing`,()=>{
 const {g,repo}=fixture([['ACLIMATACIÓN',1]]),saved=copy(repo.saved),writes=repo.writes;
 assert.throws(()=>g.resolveSkillCheck({skillKey:'supervivencia',...bad,rng:()=>0.5}),/contexto/i);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
for(const rank of [0,1,2])test(`P158: selective detonation rank ${rank} never excludes more targets than purchased`,()=>{
 const c=fixture(rank?[['DETONACIÓN SELECTIVA',rank]]:[]).g.state.player.character;
 assert.equal(selectiveDetonationOption(c,{excluded:rank}).advantageCost,rank);
 assert.equal(selectiveDetonationOption(c,{excluded:rank,useTriumph:true}).triumphCost,rank?1:0);
 assert.throws(()=>selectiveDetonationOption(c,{excluded:rank+1}),/graduações/);
});
for(const rank of [0,1])test(`P158: Master Grenadier rank ${rank} discounts Blast by one, never below one`,()=>{
 const c=fixture(rank?[['GRANADERO EXPERTO',rank]]:[]).g.state.player.character;
 assert.equal(masterGrenadierBlastCost(c,2).cost,rank?1:2);
 assert.equal(masterGrenadierBlastCost(c,3).cost,rank?2:3);
 assert.equal(masterGrenadierBlastCost(c,1).cost,1);
});
test('P158: Master Grenadier is included in the actual attack pending quality receipt',()=>{
 const x=encounter([['GRANADERO EXPERTO',1]],1);const c=copy(x.g.state.player.character);
 c.weapons=[{id:'frag_grenade',name:'Granada de fragmentação',skill:'armas_distancia_ligeras',damage:8,crit:4,range:'Curto',qualities:['Explosão 6','Munição limitada 1']}];x.g.setCharacter(c);
 x.g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:x.enemies[0].id,rng:()=>0.5});
 const q=x.g.state.combat.pendingPlayerSpend?.qualities?.find(q=>q.key==='blast');assert.ok(q);assert.equal(q.cost,q.missBlast?2:1);
 assert.equal(new GameController(x.repo).state.combat.pendingPlayerSpend.qualities.find(q=>q.key==='blast').cost,q.cost);
});
test('P158: selective blast excludes one of two adjudicated affected targets, pays shared budget and persists',()=>{
 const x=encounter([['DETONACIÓN SELECTIVA',2]]);pending(x,{advantage:3});const [primary,one,two]=x.enemies;
 const r=x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[one.id,two.id],excludedTargetIds:[one.id]});
 assert.deepEqual(r.effect.excludedTargetIds,[one.id]);assert.equal(r.remainingAdvantage,0);assert.equal(r.effect.targets.length,1);assert.equal(r.effect.targets[0].id,two.id);
 assert.equal(x.g.state.combat.adversaries.find(a=>a.id===one.id).wounds.current,0);
 const reload=new GameController(x.repo);assert.equal(validateState(reload.state),true);assert.equal(reload.state.combat.pendingPlayerSpend.remainingAdvantage,0);
 assert.throws(()=>reload.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[two.id]}),/já foi ativada/);
});
test('P158: one Triumph excludes two blast targets, but base activation still costs Advantage',()=>{
 const x=encounter([['DETONACIÓN SELECTIVA',2]]);pending(x,{advantage:2,triumph:1});const [primary,one,two]=x.enemies;
 const r=x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[one.id,two.id],excludedTargetIds:[one.id,two.id],useSelectiveTriumph:true});
 assert.equal(r.remainingAdvantage,0);assert.equal(r.remainingTriumph,0);assert.equal(r.effect.targets.length,0);
 assert.equal(x.g.state.combat.adversaries.find(a=>a.id===one.id).wounds.current,0);
});
test('P158: Master Grenadier and Selective Detonation combine with Powerful Blast in a single ledger',()=>{
 const x=encounter([['GRANADERO EXPERTO',1],['DETONACIÓN SELECTIVA',1],['EXPLOSIÓN POTENTE',2]]);pending(x,{advantage:2,qualityCost:1});const [primary,one,two]=x.enemies;
 const r=x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[one.id,two.id],excludedTargetIds:[one.id]});
 assert.equal(r.remainingAdvantage,0);assert.equal(r.effect.talentBlastBonus,2);assert.equal(r.effect.targets.length,1);
 assert.equal(r.effect.damage,8);assert.equal(validateState(x.repo.saved),true);
});
for(const args of [
 {excludedTargetIds:['unknown']}, {excludedTargetIds:['unknown'],useSelectiveTriumph:true},
 {excludedTargetIds:['duplicate','duplicate']}, {excludedTargetIds:['not-affected']},
 {excludedTargetIds:[],useSelectiveTriumph:'true'}
])test(`P158: forged blast exclusion ${JSON.stringify(args)} cannot mutate combat`,()=>{
 const x=encounter([['DETONACIÓN SELECTIVA',2]]);pending(x,{advantage:4,triumph:1});const saved=copy(x.repo.saved),writes=x.repo.writes;
 assert.throws(()=>x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[x.enemies[1].id],...args}));
 assert.deepEqual(x.repo.saved,saved);assert.equal(x.repo.writes,writes);
});
test('P158: cannot exclude more than rank or pay with absent Advantage/Triumph',()=>{
 for(const [advantage,triumph,useTriumph] of [[2,0,true],[2,1,false],[1,1,false]]){
  const x=encounter([['DETONACIÓN SELECTIVA',1]]);pending(x,{advantage,triumph});const saved=copy(x.repo.saved);
  assert.throws(()=>x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[x.enemies[1].id],excludedTargetIds:[x.enemies[1].id],useSelectiveTriumph:useTriumph}));
  assert.deepEqual(x.repo.saved,saved);
 }
});
test('P158: repository failure rolls back target wounds, excluded targets and entire Advantage/Triumph ledger',()=>{
 const x=encounter([['DETONACIÓN SELECTIVA',2]]);pending(x,{advantage:2,triumph:1});const before=copy(x.g.state),saved=copy(x.repo.saved);x.repo.fail=true;
 assert.throws(()=>x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[x.enemies[1].id],excludedTargetIds:[x.enemies[1].id],useSelectiveTriumph:true}),/storage failure/);
 assert.deepEqual(x.g.state,before);assert.deepEqual(x.repo.saved,saved);
});
test('P158: Android UI and bundle expose blast exclusion, Triumph payment, foraging and poison context',()=>{
 const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 for(const key of ['data-p158-blast-exclude','data-p158-blast-triumph','data-p158-forage','data-p158-poison','excludedTargetIds','foraging:true','poisonResistance:true'])assert.ok(runtime.includes(key),key);
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');assert.match(bundle,/FINAL 1\.0\.(?:158|159|1[6-9]\d|[2-9]\d{2}) offline engine bundle/);
});
for(const {id} of publishedAdventureCatalog())test(`P158: UAE published adventure ${id} preserves objective graph and idempotent choice after three reopens`,()=>{
 const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta);assert.ok(validatePublishedAdventureGraph(module));assert.ok(validateAdventureSemantics(module));
 const {g,repo}=fixture([['ACLIMATACIÓN',1],['DETONACIÓN SELECTIVA',1]],'p158:'+id);const actionId='p158:'+id+':start';
 assert.equal(g.executePublishedAdventureCommand({actionId,kind:'start',adventureId:id}).applied,true);
 const choiceId=adapter.status(repo.saved.world).node?.choices?.[0]?.id;assert.ok(choiceId);
 assert.equal(g.executePublishedAdventureCommand({actionId:'p158:'+id+':choice',kind:'choice',adventureId:id,choiceId}).applied,true);
 const saved=copy(repo.saved),writes=repo.writes;
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.executePublishedAdventureCommand({actionId,kind:'start',adventureId:id}).duplicate,true);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);assert.equal(validateState(reopened.state),true)}
});
test('P158: a character without Selective Detonation cannot exclude a target even with spare Advantage',()=>{
 const x=encounter();pending(x,{advantage:5,triumph:1});const saved=copy(x.repo.saved),writes=x.repo.writes;
 assert.throws(()=>x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[x.enemies[1].id],excludedTargetIds:[x.enemies[1].id]}),/graduações/);
 assert.deepEqual(x.repo.saved,saved);assert.equal(x.repo.writes,writes);
});
test('P158: a missed explosive shot may exclude the original target with one Advantage after paying miss-Blast cost',()=>{
 const x=encounter([['DETONACIÓN SELECTIVA',1]]);pending(x,{hit:false,advantage:4,triumph:0,qualityCost:3});
 const r=x.g.activatePendingWeaponQuality({qualityKey:'blast',excludedTargetIds:[x.enemies[0].id]});
 assert.equal(r.remainingAdvantage,0);assert.equal(r.effect.targets.length,0);assert.deepEqual(r.effect.excludedTargetIds,[x.enemies[0].id]);
 assert.equal(new GameController(x.repo).state.combat.adversaries[0].wounds.current,0);
});
