/** P157 — Eight source-backed AoR slots, actual combat/skill/economy flows. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {talentRankByOriginal,talentSkillPoolModifiers,indistinguishableIdentificationDefense,wheelAndDealLegalSale,feintTalentOption} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {grantCatalogAsset,sellOwnedAsset,genericEconomyCatalog} from '../app/src/main/assets/vtt/js/rules/economy.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {validatePublishedAdventureGraph} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
const copy=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(y=>y.nodes));
class Repo{constructor(id='p157'){this.saved=createCampaign(id);this.writes=0;this.fail=false;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('P157 disk failure');this.saved=copy(s);this.writes++}}
function fixture(names=[],id='p157'){
 const repo=new Repo(id),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];
 for(const [name,rank] of names){const bought=nodes.filter(n=>n.originalName===name).slice(0,rank);assert.equal(bought.length,rank,name);
  for(const n of bought){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});}}
 g.setCharacter(c);return {g,repo};
}
const ranks=[['DON DE GENTES','KILL',2],['INDISTINGUIBLE','INDISTINGUISHABLE',2],['CHANCHULLERO','WHEELDEAL',2],['FINTA','FEINT',2]];
test('P157: exact 8 AoR source positions are wired and no unrelated slots are marked complete',()=>{
 const c=aorTalentCoverage();assert.equal(c.positions,360);assert.ok(c.mechanicalPositions>=262);
 for(const [name,canonical,rank] of ranks){assert.equal(aorCanonicalTalentName(name),canonical);assert.equal(aorTalentCatalog().filter(n=>n.originalName===name&&n.mechanicallyActive).length,rank);assert.equal(talentRankByOriginal(fixture([[name,rank]]).g.state.player.character,canonical),rank)}
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='GUARDAESPALDAS'&&n.mechanicallyActive).length,2); // P170
});
for(const rank of [0,1,2])for(const skill of ['carisma','liderazgo','engano','mecanica'])test(`P157: Don de gentes rank ${rank} affects ${skill} only`,()=>{
 const c=fixture(rank?[['DON DE GENTES',rank]]:[]).g.state.player.character;
 const result=talentSkillPoolModifiers(c,skill,{setback:4});
 assert.equal(result.setback,4-(['carisma','liderazgo'].includes(skill)?rank:0));
});
test('P157: Don de gentes actually changes controller skill dice and persists result',()=>{
 const {g,repo}=fixture([['DON DE GENTES',2]]);
 const check=g.resolveSkillCheck({skillKey:'carisma',difficulty:2,setback:3,rng:()=>0.5});
 assert.equal(check.pool.setback,1);assert.equal(check.talentModifiers.setbackRemoved,2);
 assert.equal(new GameController(repo).state.combat.lastAttack??null,null);assert.equal(validateState(repo.saved),true);
});
for(const rank of [0,1,2])test(`P157: Indistinguível rank ${rank} is strictly identity-contextual`,()=>{
 const c=fixture(rank?[['INDISTINGUIBLE',rank]]:[]).g.state.player.character;
 assert.equal(indistinguishableIdentificationDefense(c,{identifyingCharacter:true}).difficultyUpgrades,rank);
 assert.equal(indistinguishableIdentificationDefense(c,{identifyingCharacter:false}).difficultyUpgrades,0);
});
test('P157: NPC identification upgrades negative dice and saves one authoritative result',()=>{
 const {g,repo}=fixture([['INDISTINGUIBLE',2]]);const writes=repo.writes;
 const x=g.resolveIncomingIdentificationCheck({actorName:'Inspetor ISB',actorCharacteristic:3,actorSkill:2,difficulty:2,rng:()=>0.5});
 assert.equal(x.talent.rank,2);assert.equal(x.check.pool.challenge,2);assert.equal(x.check.pool.proficiency,2);
 assert.equal(repo.writes,writes+1);assert.equal(new GameController(repo).state.advancedChecks.last.type,'incoming-identification');assert.equal(validateState(repo.saved),true);
});
for(const input of [{actorCharacteristic:-1},{actorCharacteristic:99},{actorSkill:6},{difficulty:-1},{challenge:7}])test(`P157: forged identity check ${JSON.stringify(input)} rejected atomically`,()=>{
 const {g,repo}=fixture([['INDISTINGUIBLE',2]]),saved=copy(repo.saved),writes=repo.writes;
 assert.throws(()=>g.resolveIncomingIdentificationCheck(input));assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
test('P157: identity check repository failure restores state',()=>{
 const {g,repo}=fixture([['INDISTINGUIBLE',2]]),before=copy(g.state);repo.fail=true;
 assert.throws(()=>g.resolveIncomingIdentificationCheck({rng:()=>0.5}),/disk failure/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
for(const rank of [0,1,2])for(const legal of [false,true])test(`P157: Chanchullero rank ${rank}, legal=${legal} applies only legal sale bonus`,()=>{
 const c=fixture(rank?[['CHANCHULLERO',rank]]:[]).g.state.player.character;
 const q=wheelAndDealLegalSale(c,{legalSale:legal,basePrice:101});assert.equal(q.finalPrice,101+(legal?Math.floor(101*rank/10):0));
});
test('P157: legal sale removes owned item, increases credits by +20%, and survives reload',()=>{
 const {g,repo}=fixture([['CHANCHULLERO',2]]);
 const granted=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:'p157_owned_rod'});
 const before=granted.state.player.character.credits;
 const sold=sellOwnedAsset(granted.state,'p157_owned_rod',{price:105,legalSale:true,sourceRuleIds:['P157:AOR:WHEELDEAL']});
 assert.equal(sold.record.basePrice,105);assert.equal(sold.record.talentBonus,21);assert.equal(sold.record.price,126);
 assert.equal(sold.state.player.character.credits,before+126);assert.equal(sold.state.group.ownedAssets.length,0);
 assert.equal(sold.state.player.character.gear.some(x=>x.ownedAssetId==='p157_owned_rod'),false);
 repo.save(sold.state);assert.equal(new GameController(repo).state.player.character.credits,before+126);assert.equal(validateState(repo.saved),true);
});
test('P157: illegal sale gets no bonus and restricted legal sale fails closed',()=>{
 const {g}=fixture([['CHANCHULLERO',2]]),legal=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:'p157_illegal'});
 const sold=sellOwnedAsset(legal.state,'p157_illegal',{price:100,legalSale:false,sourceRuleIds:['GM']});assert.equal(sold.record.talentBonus,0);
 const restricted=genericEconomyCatalog().weapons.find(x=>x.restricted);
 assert.ok(restricted,'catalog must contain a restricted weapon');
 const item=grantCatalogAsset(g.state,{kind:'weapon',itemId:restricted.id,ownedAssetId:'p157_restricted'});
 assert.throws(()=>sellOwnedAsset(item.state,'p157_restricted',{price:100,legalSale:true,sourceRuleIds:['GM']}),/restrito/);
});
for(const value of [-1,1.5,Infinity,1e15])test(`P157: illegal sale quote ${value} cannot create credits`,()=>{
 const c=fixture([['CHANCHULLERO',2]]).g.state.player.character;
 assert.throws(()=>wheelAndDealLegalSale(c,{legalSale:true,basePrice:value}));
});
for(const rank of [0,1,2])test(`P157: Finta rank ${rank} has exact strain budget`,()=>{
 const c=fixture(rank?[['FINTA',rank]]:[]).g.state.player.character;
 assert.equal(feintTalentOption(c,0).rank,rank);
 assert.throws(()=>feintTalentOption(c,rank+1),/máximo/);
 assert.throws(()=>feintTalentOption(c,-1),/inválida/);
});
function feintFixture(rank=2){const x=fixture(rank?[['FINTA',rank]]:[],'p157:feint');x.g.beginCombat();return x;}
test('P157: Finta spends maneuver and strain, survives reload, rejects same-round replay',()=>{
 const {g,repo}=feintFixture(),before=g.state.player.character.derived.strain.current;
 const r=g.useFeint({strain:2});assert.equal(r.difficultyUpgrades,2);assert.equal(g.state.combat.playerTurn.maneuversUsed,1);
 assert.equal(g.state.player.character.derived.strain.current,before+2);assert.equal(new GameController(repo).state.player.character.combatEffects.feint.difficultyUpgrades,2);
 const writes=repo.writes;assert.throws(()=>g.useFeint({strain:1}),/já foi usada/);assert.equal(repo.writes,writes);
});
test('P157: Finta modifies ranged NPC attack but not melee; expires next player turn',()=>{
 const {g}=feintFixture();g.useFeint({strain:2});
 const npc=createAdversary('imperial_officer',{range:'Curto'}),combat=copy(g.state.combat);combat.adversaries=[npc];
 const ranged=g._resolveNpcTurn(combat,g.state.player.character,npc.id,()=>0.5);
 assert.equal(ranged.attack.feintUpgrades,2);assert.equal(ranged.attack.challenge,1);assert.ok(ranged.attack.difficulty>=1);
 const meleeNpc=copy(npc);meleeNpc.weapons=[{name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:4,crit:2}];meleeNpc.range='Interação';
 const meleeCombat=copy(combat);meleeCombat.adversaries=[meleeNpc];
 const melee=g._resolveNpcTurn(meleeCombat,g.state.player.character,meleeNpc.id,()=>0.5);
 assert.equal(melee.attack.feintUpgrades,0);
 g.advancePlayerTurn({rng:()=>0.5});assert.equal(g.state.player.character.combatEffects?.feint,undefined);
});
for(const [rank,strain] of [[0,1],[1,2],[2,0],[2,-1]])test(`P157: Finta unauthorized rank=${rank}, strain=${strain} no writes`,()=>{
 const {g,repo}=feintFixture(rank),before=copy(g.state),writes=repo.writes;
 assert.throws(()=>g.useFeint({strain}));assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P157: Finta repository failure rolls back strain, maneuver and effect',()=>{
 const {g,repo}=feintFixture(),before=copy(g.state),writes=repo.writes;repo.fail=true;
 assert.throws(()=>g.useFeint({strain:2}),/disk failure/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P157: Android bridge/contract/UI wired to real talent and economy actions',async()=>{
 const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
 const contract=readFileSync(new URL('../gameBridgeClient.ts',import.meta.url),'utf8');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 for(const name of ['useFeint','resolveIncomingIdentificationCheck','legalSaleQuote']){
  assert.ok(bridge.includes(name));assert.ok(contract.includes(name+'?'));assert.ok(ui.includes(name));
 }
 for(const marker of ['data-p157-feint','data-p157-identify','data-p157-quote'])assert.ok(ui.includes(marker));
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const {g,repo}=feintFixture();const b=new BraseiroUiBridge({repository:repo,storage:null});
 const f=b.useFeint({strain:1});assert.equal(f.ok,true,f.message);
 const i=b.resolveIncomingIdentificationCheck({actorCharacteristic:2,actorSkill:1,rng:()=>0.5});assert.equal(i.ok,true,i.message);
 assert.equal(validateState(repo.saved),true);
});
for(const {id} of publishedAdventureCatalog())test(`P157: five-adventure UAE graph, choice and 3 recoveries ${id}`,()=>{
 const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta);
 assert.ok(validatePublishedAdventureGraph(module));assert.ok(validateAdventureSemantics(module));
 const repo=new Repo('p157:'+id),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const actionId='p157:'+id+':start';assert.equal(g.executePublishedAdventureCommand({actionId,kind:'start',adventureId:id}).applied,true);
 const choiceId=adapter.status(repo.saved.world).node?.choices?.[0]?.id;
 assert.equal(g.executePublishedAdventureCommand({actionId:'p157:'+id+':choice',kind:'choice',adventureId:id,choiceId}).applied,true);
 const saved=copy(repo.saved),writes=repo.writes;
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.executePublishedAdventureCommand({actionId,kind:'start',adventureId:id}).duplicate,true);assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,saved);assert.equal(validateState(repo.saved),true)}
});
test('P157: GM validated structured legal-sale proposal actually grants Chanchullero and records provenance',()=>{
 const {g,repo}=fixture([['CHANCHULLERO',2]],'p157:gm-sale');
 const granted=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:'p157:gm-owned'});
 granted.state.master.lastContext={ruleIds:['P157:AOR:WHEELDEAL']};repo.save(granted.state);
 const controller=new GameController(repo),before=controller.state.player.character.credits;
 controller.applyMasterProposal({narration:'O mercador paga o preço negociado.',suggestedActions:[],
  sourceRuleIds:['P157:AOR:WHEELDEAL'],economyOperations:[{type:'sell_asset',assetId:'p157:gm-owned',price:100,legalSale:true}]});
 assert.equal(controller.state.player.character.credits,before+120);
 assert.equal(controller.state.group.ownedAssets.length,0);
 assert.equal(controller.state.master.acquisitionHistory.at(-1).talentBonus,20);
 assert.equal(new GameController(repo).state.player.character.credits,before+120);
 assert.equal(validateState(repo.saved),true);
});
test('P157: GM proposal rejects stringified legalSale and never mutates state',()=>{
 const {g,repo}=fixture([['CHANCHULLERO',2]],'p157:gm-invalid');
 const granted=grantCatalogAsset(g.state,{kind:'gear',itemId:'glow_rod',ownedAssetId:'p157:gm-owned'});
 granted.state.master.lastContext={ruleIds:['P157:AOR:WHEELDEAL']};repo.save(granted.state);
 const controller=new GameController(repo),before=copy(repo.saved),writes=repo.writes;
 assert.throws(()=>controller.applyMasterProposal({narration:'x',suggestedActions:[],sourceRuleIds:['P157:AOR:WHEELDEAL'],
  economyOperations:[{type:'sell_asset',assetId:'p157:gm-owned',price:100,legalSale:'true'}]}),/booleana/);
 assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
