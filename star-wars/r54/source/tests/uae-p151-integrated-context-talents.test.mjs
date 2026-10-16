/** P151 — contextual AoR talents, directed droid, atomic skill and navigation. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {talentRankByOriginal,talentSkillPoolModifiers,naturalRerollOption,respectedScholarDowngrades,speaksBinaryBoost,expertSkillTalentSpec} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
const all=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(y=>y.nodes));
class Repo{constructor(id){this.saved=createCampaign(id);this.writes=0;this.fail=false;}load(){return structuredClone(this.saved)}save(s){if(this.fail)throw Error('P151 disk fail');this.saved=structuredClone(s);this.writes++;}}
function game(id='p151'){const repo=new Repo(id),controller=new GameController(repo);controller.loadStarterCharacter('cael');return {repo,controller};}
function talents(...items){const c=game().controller.state.player.character;c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talentPurchases=[];c.talents=[];for(const [name,count] of items){const nodes=all.filter(n=>n.originalName===name).slice(0,count);assert.equal(nodes.length,count,name);for(const n of nodes){c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});c.talents.push(n.name);}}return c;}
function configured(...items){const x=game('p151:configured');x.controller.setCharacter(talents(...items));return x;}
const cases=[['ATAJO','SHORTCUT',2],['INVENTOR','INVENTOR',2],['ERUDITO RESPETADO','RESSCH',2],['HABLAR BINARIO','SPEAKBIN',2],['CONDUCTOR TODOTERRENO','ALLTERR',1],['CONDUCTOR NATO','NATDRI',1],['ASTRONAVEGANTE EXPERTO','MASSTAR',1]];
test('P151: 11 P151 source slots remain active after P154',()=>{
 const cov=aorTalentCoverage();assert.equal(cov.positions,360);assert.ok(cov.mechanicalPositions>=247);assert.equal(cov.positions-cov.mechanicalPositions,360-cov.mechanicalPositions);
 for(const [name,canonical,count] of cases){assert.equal(aorCanonicalTalentName(name),canonical);assert.equal(aorTalentCatalog().filter(x=>x.originalName===name&&x.mechanicallyActive).length,count);assert.equal(talentRankByOriginal(talents([name,count]),canonical),count);}
 for(const name of ['FAMILIARES'])assert.equal(aorTalentCatalog().filter(x=>x.originalName===name&&x.mechanicallyActive).length,0);
});
test('P151: Shortcut and Inventor apply only on explicitly selected tasks',()=>{
 const c=talents(['ATAJO',2],['INVENTOR',2]);
 assert.equal(talentSkillPoolModifiers(c,'pilotar_vehiculo_espacial',{chase:true}).boost,2);
 assert.equal(talentSkillPoolModifiers(c,'pilotar_vehiculo_espacial').boost,0);
 assert.equal(talentSkillPoolModifiers(c,'mecanica',{crafting:true}).boost,2);
 assert.equal(talentSkillPoolModifiers(c,'mecanica').boost,0);
 assert.equal(talentSkillPoolModifiers(c,'medicina',{crafting:true,chase:true}).boost,0);
});
test('P151: All-Terrain Driver cancels only terrain setback, not other penalties',()=>{
 const c=talents(['CONDUCTOR TODOTERRENO',1]);
 const active=talentSkillPoolModifiers(c,'pilotar_vehiculo_planetario',{setback:2,terrainSetback:3});
 assert.equal(active.setback,2);assert.equal(active.details.find(x=>x.originalName==='ALLTERR').amount,3);
 assert.equal(talentSkillPoolModifiers(c,'pilotar_vehiculo_espacial',{setback:2,terrainSetback:3}).setback,5);
 assert.equal(talentSkillPoolModifiers(talents(),'pilotar_vehiculo_planetario',{setback:2,terrainSetback:3}).setback,5);
 for(const n of [-1,1.5,21,NaN])assert.throws(()=>talentSkillPoolModifiers(c,'pilotar_vehiculo_planetario',{terrainSetback:n}));
});
test('P151: Scholar only downgrades institutional social checks, Natural Driver rerolls planetary piloting',()=>{
 const c=talents(['ERUDITO RESPETADO',2],['CONDUCTOR NATO',1]);
 assert.equal(respectedScholarDowngrades(c,'negociacion',{scholarlyInstitution:true}),2);
 assert.equal(respectedScholarDowngrades(c,'negociacion',{}),0);
 assert.equal(respectedScholarDowngrades(c,'computadoras',{scholarlyInstitution:true}),0);
 assert.equal(naturalRerollOption(c,'pilotar_vehiculo_planetario').originalName,'NATDRI');
 assert.equal(naturalRerollOption(c,'pilotar_vehiculo_espacial').available,false);
});
test('P151: contextual skill checks persist only one transaction and work after reopening',()=>{
 const {repo,controller}=configured(['ATAJO',2],['INVENTOR',2],['ERUDITO RESPETADO',2],['CONDUCTOR TODOTERRENO',1]);
 let writes=repo.writes;
 const chase=controller.resolveSkillCheck({skillKey:'pilotar_vehiculo_espacial',chase:true,rng:()=>.5});assert.equal(chase.talentModifiers.talentBoost,2);assert.equal(repo.writes,++writes);
 const craft=controller.resolveSkillCheck({skillKey:'mecanica',crafting:true,rng:()=>.5});assert.equal(craft.talentModifiers.talentBoost,2);assert.equal(repo.writes,++writes);
 const baseline=controller.resolveSkillCheck({skillKey:'pilotar_vehiculo_planetario',setback:1,terrainSetback:0,rng:()=>.5});assert.equal(repo.writes,++writes);
 const terrain=controller.resolveSkillCheck({skillKey:'pilotar_vehiculo_planetario',setback:1,terrainSetback:3,rng:()=>.5});assert.equal(terrain.talentModifiers.setback,baseline.talentModifiers.setback);assert.equal(repo.writes,++writes);
 const scholar=controller.resolveSkillCheck({skillKey:'negociacion',difficulty:3,challenge:1,scholarlyInstitution:true,rng:()=>.5});assert.equal(scholar.scholarDowngrades,2);assert.equal(repo.writes,++writes);
 assert.equal(new GameController(repo).state.lastCheck.scholarDowngrades,2);assert.equal(validateState(repo.saved),true);
});
test('P151: Respected Scholar downgrades only red dice and never deletes purple dice',()=>{
 const {controller}=configured(['ERUDITO RESPETADO',2]);
 const red=controller.resolveSkillCheck({skillKey:'negociacion',difficulty:2,challenge:1,scholarlyInstitution:true,rng:()=>.5});
 assert.equal(red.pool.challenge,0);assert.equal(red.pool.difficulty,3);
 const purple=controller.resolveSkillCheck({skillKey:'negociacion',difficulty:2,scholarlyInstitution:true,rng:()=>.5});
 const ordinary=controller.resolveSkillCheck({skillKey:'negociacion',difficulty:2,rng:()=>.5});
 assert.equal(purple.pool.difficulty,ordinary.pool.difficulty);
 assert.equal(purple.pool.challenge,ordinary.pool.challenge);
});
test('P151: directed droid uses NPC stats and 2 boosts, not owner skill',()=>{
 const {repo,controller}=configured(['HABLAR BINARIO',2]);const writes=repo.writes;
 assert.equal(speaksBinaryBoost(controller.state.player.character,{directingDroid:true}),2);
 assert.equal(speaksBinaryBoost(controller.state.player.character),0);
 const check=controller.resolveDirectedDroidSkillCheck({skillKey:'computadoras',droidCharacteristic:2,droidSkill:1,difficulty:2,confirmedDroid:true,rng:()=>.5});
 assert.equal(check.binaryBoost,2);assert.equal(check.pool.boost,2);assert.equal(check.pool.ability,1);assert.equal(check.pool.proficiency,1);assert.equal(repo.writes,writes+1);
 assert.equal(new GameController(repo).state.lastCheck.kind,'directed_npc_droid');assert.equal(validateState(repo.saved),true);
});
test('P151: invalid droid or invalid contextual parameters cannot write partial state',()=>{
 const {repo,controller}=configured(['HABLAR BINARIO',2]);const before=structuredClone(repo.saved),writes=repo.writes;
 for(const args of [{skillKey:'computadoras',droidCharacteristic:3,droidSkill:2},
  {skillKey:'computadoras',droidCharacteristic:3,droidSkill:2,confirmedDroid:'yes'},
  {skillKey:'fake',droidCharacteristic:3,droidSkill:2,confirmedDroid:true},
  {skillKey:'computadoras',droidCharacteristic:7,droidSkill:2,confirmedDroid:true},
  {skillKey:'computadoras',droidCharacteristic:3,droidSkill:6,confirmedDroid:true},
  {skillKey:'computadoras',droidCharacteristic:3,droidSkill:2,difficulty:1,challenge:2,confirmedDroid:true}])assert.throws(()=>controller.resolveDirectedDroidSkillCheck(args));
 assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P151: repository failure rolls back skill, droid and character strain changes',()=>{
 const {repo,controller}=configured(['INVENTOR',2],['HABLAR BINARIO',2]);const before=structuredClone(repo.saved);repo.fail=true;
 assert.throws(()=>controller.resolveSkillCheck({skillKey:'mecanica',crafting:true,rng:()=>.5}),/disk fail/);
 assert.deepEqual(controller.state,before);
 assert.throws(()=>controller.resolveDirectedDroidSkillCheck({skillKey:'computadoras',droidCharacteristic:3,droidSkill:2,confirmedDroid:true,rng:()=>.5}),/disk fail/);
 assert.deepEqual(controller.state,before);assert.deepEqual(repo.saved,before);
});
test('P151: Expert Astrogator spends 2 strain, reduces difficulty, persists and rolls back on save error',()=>{
 const {repo,controller}=configured(['ASTRONAVEGANTE EXPERTO',1]);const c=controller.state.player.character;
 assert.equal(expertSkillTalentSpec(c,'astronavegacion').available,true);
 controller.addVehicle('xwing_t65b');const beforeStrain=controller.state.player.character.derived.strain.current;
 const r=controller.plotHyperspaceCourse({difficulty:3,useExpertTalent:true,rng:()=>.5});
 assert.equal(r.difficulty,2);assert.equal(r.check.expertTalent.used,true);
 assert.equal(controller.state.player.character.derived.strain.current,beforeStrain+2);
 assert.equal(new GameController(repo).state.player.character.derived.strain.current,beforeStrain+2);
 const snapshot=structuredClone(repo.saved);repo.fail=true;
 assert.throws(()=>controller.plotHyperspaceCourse({difficulty:3,useExpertTalent:true,rng:()=>.5}),/disk fail/);
 assert.deepEqual(controller.state,snapshot);assert.deepEqual(repo.saved,snapshot);
});
test('P151: Android bridge, UI and version contain all contextual mechanics',()=>{
 const read=x=>readFileSync(new URL(x,import.meta.url),'utf8');
 assert.match(read('../app/src/main/assets/vtt/js/core/uiBridge.mjs'),/resolveDirectedDroidSkillCheck/);
 assert.match(read('../app/src/main/assets/vtt/r46/final-runtime.js'),/data-p151-droid/);
 assert.match(read('../app/src/main/assets/vtt/r46/final-runtime.js'),/data-p151-context/);
 assert.match(read('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js'),/resolveDirectedDroidSkillCheck/);
 assert.ok(Number(read('../app/build.gradle.kts').match(/versionName = "1\.0\.(\d+)"/)?.[1])>=151);
});

import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
for(const {id} of publishedAdventureCatalog()){
 test(`P151 ${id}: contextual skill + droid roll survive three published-adventure reopens`,()=>{
  const {repo,controller}=configured(['INVENTOR',2],['HABLAR BINARIO',2]);
  const startId=`p151:${id}:start`;
  const started=controller.executePublishedAdventureCommand({actionId:startId,kind:'start',adventureId:id});
  assert.equal(started.applied,true);
  const crafted=controller.resolveSkillCheck({skillKey:'mecanica',crafting:true,rng:()=>.5});
  assert.equal(crafted.talentModifiers.talentBoost,2);
  const droid=controller.resolveDirectedDroidSkillCheck({skillKey:'computadoras',droidCharacteristic:3,droidSkill:2,confirmedDroid:true,rng:()=>.5});
  assert.equal(droid.binaryBoost,2);
  const snapshot=structuredClone(repo.saved),writes=repo.writes;
  for(let i=0;i<3;i++){
   const opened=new GameController(repo);
   assert.equal(opened.executePublishedAdventureCommand({actionId:startId,kind:'start',adventureId:id}).duplicate,true);
   assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,snapshot);assert.equal(validateState(opened.state),true);
  }
 });
}
test('P151: atomic skill check restores spent destiny on storage failure',()=>{
 const {repo,controller}=game('p151:destiny');
 const before=structuredClone(repo.saved);repo.fail=true;
 assert.throws(()=>controller.resolveSkillCheck({skillKey:'mecanica',difficulty:2,useLightDestiny:true,rng:()=>.5}));
 assert.deepEqual(controller.state,before);assert.deepEqual(repo.saved,before);
});
