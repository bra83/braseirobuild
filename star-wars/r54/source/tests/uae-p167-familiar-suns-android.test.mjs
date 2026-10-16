/** P167 Familiar Suns source-backed, once/session, transactional and Android. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCoverage,aorTalentCatalog} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
const clone=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(spec=>Object.values(spec).flatMap(tree=>tree.nodes)).filter(n=>n.originalName==='SOLES FAMILIARES');
class Repo {constructor(){this.saved=createCampaign('P167');this.fail=false;}load(){return clone(this.saved);}save(s){if(this.fail)throw Error('P167 disk failure');this.saved=clone(s);}}
function setup(ranks=1){const repo=new Repo(),game=new GameController(repo);game.loadStarterCharacter('cael');
 const character=clone(game.state.player.character);character.creation={...character.creation,rulesProfile:'AOR_CORE'};
 character.talents=[];character.talentPurchases=[];
 for(const node of nodes.slice(0,ranks)){character.talents.push(node.name);character.talentPurchases.push({nodeId:node.id,originalName:node.originalName,name:node.name,source:node.source});}
 character.characteristics.intelecto=5;character.skills.borde_exterior=4;character.skills.mundos_del_nucleo=4;
 game.setCharacter(character);game.startSession({rng:()=>0.5});return {repo,game};}
test('P167 two Familiar Suns AoR slots active; no unrelated slot or EotE display name grants use',()=>{
 assert.equal(nodes.length,2);assert.equal(aorTalentCoverage().positions,360);assert.ok(aorTalentCoverage().mechanicalPositions>=304);
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='SOLES FAMILIARES'&&n.mechanicallyActive).length,2);
 for(let ranks=0;ranks<=2;ranks++){const {game}=setup(ranks);assert.equal(sourceBackedTalentRank(game.state.player.character,'FAMILIARSUNS'),ranks);assert.equal(game.familiarSunsStatus().available,ranks>0);}
 const {game}=setup(0);const c=clone(game.state.player.character);c.talents=['Sóis familiares'];c.talentPurchases=[{nodeId:nodes[0].id,originalName:'MENTAL FORTRESS'}];game.setCharacter(c);
 assert.equal(game.familiarSunsStatus().available,false);
});
test('P167 Hard planetary Knowledge check is once per session and never invents lore',()=>{
 const {repo,game}=setup(2);
 assert.equal(game.familiarSunsStatus().difficulty,3);
 assert.throws(()=>game.useFamiliarSuns({skillKey:'medicina'}),/Conhecimento/);
 assert.equal(game.familiarSunsStatus().usedThisSession,false);
 const check=game.useFamiliarSuns({skillKey:'borde_exterior',rng:()=>0.85});
 assert.equal(check.used,true);assert.equal(check.difficulty,3);assert.equal(check.skillKey,'borde_exterior');
 assert.equal(check.gmRevelationRequired,check.success);
 assert.equal(game.familiarSunsStatus().usedThisSession,true);
 assert.throws(()=>game.useFamiliarSuns({skillKey:'mundos_del_nucleo'}),/já foi utilizado/);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.familiarSunsStatus().usedThisSession,true);assert.equal(validateState(reopened.state),true);}
});
test('P167 repository write failure rolls back check, use ledger and session count',()=>{
 const {repo,game}=setup(1);const before=clone(game.state),saved=clone(repo.saved);repo.fail=true;
 assert.throws(()=>game.useFamiliarSuns({skillKey:'mundos_del_nucleo',rng:()=>0.85}),/disk failure/);
 assert.deepEqual(game.state,before);assert.deepEqual(repo.saved,saved);
 repo.fail=false;assert.equal(game.useFamiliarSuns({skillKey:'mundos_del_nucleo',rng:()=>0.85}).used,true);
});
test('P167 Android bridge and PT-BR control route to actual controller, not dummy UI',()=>{
 const {repo}=setup(1);const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 assert.equal(bridge.familiarSunsStatus().data.available,true);
 assert.equal(bridge.useFamiliarSuns({skillKey:'medicina'}).ok,false);
 assert.equal(bridge.useFamiliarSuns({skillKey:'mundos_del_nucleo',rng:()=>0.85}).ok,true);
 assert.equal(bridge.familiarSunsStatus().data.usedThisSession,true);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.ok(ui.includes("call('useFamiliarSuns'"));assert.ok(ui.includes('EXAMINAR AMBIENTE PLANETÁRIO'));
});
