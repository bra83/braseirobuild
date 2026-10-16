import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
class Repo{constructor(){this.state=createCampaign('P244');this.saves=0}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
function fixture({cantina=false}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});
 let w=adapter.start(g.state.world).world;
 for(const c of ['dock-ryloth','help-new-meen','travel-lylek-den','reach-new-meen'])w=adapter.advance(w,c).world;
 w.publishedAdventure.flags.confirmedPartySize=3;
 if(cantina)w=adapter.advance(w,'confront-drombb-directly').world;
 g.state.world=w;repo.save(g.state);return {repo,g};
}
function reopen(repo){for(let i=0;i<3;i++){const g=new GameController(repo);assert.ok(adapter.status(g.state.world).active);}}
test('P244 surviving militia returns exact borrowed weapon, once, with three save reopens',()=>{
 const {repo,g}=fixture();const before=g.state.player.character.weapons.length;
 g.armSWE02NewMeenMilitia({weaponIndex:0});const loan=g.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[0];
 assert.equal(g.state.player.character.weapons.length,before-1);
 const result=g.returnSWE02NewMeenMilitiaWeapon({loanId:loan.loanId});
 assert.equal(result.type,'NEW_MEEN_MILITIA_WEAPON_RETURNED');assert.equal(g.state.player.character.weapons.length,before);
 assert.equal(g.state.world.publishedAdventure.newMeenMilitia.returned,1);
 assert.equal(g.state.world.publishedAdventure.newMeenMilitia.members,0);reopen(repo);
 const saves=repo.saves;assert.throws(()=>g.returnSWE02NewMeenMilitiaWeapon({loanId:loan.loanId}),/já devolvida/);assert.equal(repo.saves,saves);
});
test('P244 loan owner cannot be spoofed; no duplicate or fabricated weapon',()=>{
 const {repo,g}=fixture();g.armSWE02NewMeenMilitia({weaponIndex:0});const loan=g.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[0];
 const saves=repo.saves;
 for(const id of ['bogus','new-meen-weapon-999',undefined])assert.throws(()=>g.returnSWE02NewMeenMilitiaWeapon({loanId:id}),/inválido|não consta/);
 assert.equal(repo.saves,saves);
 const w=structuredClone(g.state.world);w.publishedAdventure.newMeenMilitia.loanedWeapons[0].ownerKey='other';
 assert.throws(()=>adapter.status(w),/adulterad|custódia|recibo/);
 // The real owner check is enforced by executor, independently of UI.
 g.state.player.character.id='other';assert.throws(()=>g.returnSWE02NewMeenMilitiaWeapon({loanId:loan.loanId}),/proprietário/);
 assert.equal(repo.saves,saves);
});
test('P244 an eliminated ally never returns a weapon without separate recovery proof',()=>{
 const {repo,g}=fixture({cantina:true});g.armSWE02NewMeenMilitia({weaponIndex:0});
 const loanId=g.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[0].loanId;
 g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});
 const target=g.state.combat.adversaries[0];g.applySWE02NewMeenMilitiaRound({targetGroupId:target.id});
 const loan=g.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[0];assert.equal(loan.status,'LOST');
 const c=structuredClone(g.state.combat);c.active=false;c.encounter.outcome='escaped';g.commit('COMBAT_ACTION',{combat:c});
 const saves=repo.saves;assert.throws(()=>g.returnSWE02NewMeenMilitiaWeapon({loanId}),/não foi recuperada/);assert.equal(repo.saves,saves);reopen(repo);
});
test('P244 no return during active combat; failure does not save',()=>{
 const {repo,g}=fixture({cantina:true});g.armSWE02NewMeenMilitia({weaponIndex:0});
 const loanId=g.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[0].loanId;
 g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});const saves=repo.saves;
 assert.throws(()=>g.returnSWE02NewMeenMilitiaWeapon({loanId}),/após encerrar/);assert.equal(repo.saves,saves);
});
test('P244 P243 save with unmarked loans is migrated before return, preserving casualty count',()=>{
 const {repo,g}=fixture({cantina:true});g.armSWE02NewMeenMilitia({weaponIndex:0});
 g.state.player.character.weapons.push({name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3});
 g.armSWE02NewMeenMilitia({weaponIndex:g.state.player.character.weapons.length-1});
 g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});g.applySWE02NewMeenMilitiaRound({targetGroupId:g.state.combat.adversaries[0].id});
 const combat=structuredClone(g.state.combat);combat.active=false;combat.encounter.outcome='escaped';g.commit('COMBAT_ACTION',{combat});
 const m=g.state.world.publishedAdventure.newMeenMilitia;
 for(const l of m.loanedWeapons)delete l.status; // emulate old P243 save
 repo.save(g.state);
 const fresh=new GameController(repo);
 assert.equal(adapter.status(fresh.state.world).active,true);
 const id=m.loanedWeapons[1].loanId;fresh.returnSWE02NewMeenMilitiaWeapon({loanId:id});
 assert.equal(fresh.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[0].status,'LOST');
 assert.equal(fresh.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[1].status,'RETURNED');reopen(repo);
});
test('P244 rejects tampered custody, duplicate loans, forged return receipts',()=>{
 const {g}=fixture();g.armSWE02NewMeenMilitia({weaponIndex:0});const original=g.state.world;
 for(const mutate of [p=>p.newMeenMilitia.members=0,p=>p.newMeenMilitia.loanedWeapons[0].status='RETURNED',p=>p.newMeenMilitia.returned=1]){
  const w=structuredClone(original);mutate(w.publishedAdventure);assert.throws(()=>adapter.status(w),/custódia|efetivo|recibos/);
 }
});
test('P244 return action is wired into bridge and offline Android UI',()=>{
 const bridge=new BraseiroUiBridge({repository:new Repo()});assert.equal(typeof bridge.returnSWE02NewMeenMilitiaWeapon,'function');
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02MilitiaReturn','swe02MilitiaReturnLoan']){assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
});
