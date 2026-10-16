import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {queueWingGuardTactic,applyQueuedWingGuardTactic,consumeWingGuardFreeManeuver} from '../app/src/main/assets/vtt/js/rules/wingGuardTactics.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
const clone=structuredClone;
function setup(range='Médio'){
  const s=createAdversary('bespin_wing_guard_sergeant'),a=createAdversary('bespin_wing_guard_officer',{range}),b=createAdversary('bespin_wing_guard_officer');
  return {active:true,round:1,adversaries:[s,a,b],wingGuardOrders:[],log:[],pendingNpcBoost:0,encounter:{slots:[{side:'npc'}],activeActorId:a.id}};
}
const queue=(c,d='approach')=>queueWingGuardTactic(c,{actorId:c.adversaries[0].id,kind:'tactical_direction',targetIds:[c.adversaries[1].id],confirmedRange:'medium',mode:'free_maneuver',movementDirection:d,confirmedMovement:true});
function granted(c,d='approach'){const q=queue(c,d);return applyQueuedWingGuardTactic(q,c.adversaries[0].id).combat;}
test('P198 queues a single free maneuver, moves immediately on sergeant initiative',()=>{
 const c=setup();const q=queue(c);assert.equal(q.adversaries[1].range,'Médio');const grant=applyQueuedWingGuardTactic(q,c.adversaries[0].id);assert.equal(grant.applied,true);assert.equal(grant.moved,true);assert.equal(grant.combat.adversaries[1].range,'Curto');assert.equal(grant.combat.adversaries[1].wingFreeManeuver,undefined);assert.equal(grant.combat.log.at(-1).type,'WING_GUARD_FREE_MOVE');assert.equal(consumeWingGuardFreeManeuver(grant.combat,c.adversaries[1].id).moved,false);
});
test('P198 withdraw from Engaged to Short and Short to Medium are one-band movements',()=>{
 for(const [from,to] of [['Engajado','Curto'],['Curto','Médio']]){const c=setup(from),g=granted(c,'withdraw');assert.equal(g.adversaries[1].range,to);}
});
test('P198 rejects teleport across Medium/Long, Extreme and out-of-range one-maneuver moves',()=>{
 for(const from of ['Longo','Extremo'])assert.throws(()=>queue(setup(from)),/duas manobras/);
 assert.throws(()=>queue(setup('Médio'),'withdraw'),/limite de alcance/);
 assert.throws(()=>queue(setup('Engajado'),'approach'),/limite de alcance/);
});
test('P198 refuses free maneuver without GM path confirmation, invalid direction, or spoofed mode',()=>{
 const c=setup(),s=c.adversaries[0].id,a=c.adversaries[1].id;
 const base={actorId:s,kind:'tactical_direction',targetIds:[a],confirmedRange:'medium',mode:'free_maneuver',movementDirection:'approach'};
 assert.throws(()=>queueWingGuardTactic(c,base),/Mestre deve confirmar/);
 assert.throws(()=>queueWingGuardTactic(c,{...base,confirmedMovement:true,movementDirection:'warp'}),/Direção/);
 assert.throws(()=>queueWingGuardTactic(c,{...base,confirmedMovement:true,confirmedRange:'short'}),/Médio/);
 assert.throws(()=>queueWingGuardTactic(c,{...base,confirmedMovement:true,mode:'teleport'}),/inválido/);
});
test('P198 refuses a second pending grant, a duplicate sergeant order, and forged target identity',()=>{
 const c=setup(),g=granted(c);assert.throws(()=>queue(g),/já realizada/);
 const other=setup();other.adversaries[1].wingFreeManeuver={sourceId:'fake'};assert.throws(()=>queue(other),/já possui/);
 const fake=clone(g);fake.adversaries[1].wingFreeManeuver={sourceId:'hacker',grantedRound:1,fromRange:'Médio',toRange:'Curto',direction:'approach'};assert.throws(()=>consumeWingGuardFreeManeuver(fake,c.adversaries[1].id),/Recibo/);
});
test('P198 cancels if target changes range after queue but before sergeant acts',()=>{
 const c=setup(),q=queue(c);q.adversaries[1].range='Curto';const r=applyQueuedWingGuardTactic(q,c.adversaries[0].id);assert.equal(r.cancelled,true);assert.equal(r.combat.wingGuardOrders.length,0);assert.equal(r.combat.adversaries[1].wingFreeManeuver,undefined);
});
test('P198 rejects forged stale-range grant on manual replay',()=>{
 const c=setup(),g=granted(c);g.adversaries[1].wingFreeManeuver={sourceId:c.adversaries[0].id,grantedRound:1,fromRange:'Médio',toRange:'Curto',direction:'approach'};const r=consumeWingGuardFreeManeuver(g,c.adversaries[1].id);assert.equal(r.cancelled,true);assert.equal(r.combat.adversaries[1].wingFreeManeuver,undefined);assert.equal(r.combat.adversaries[1].range,'Curto');
});
test('P198 persistence: queued order and granted maneuver survive three save/reopen cycles',()=>{
 const c=setup();let q=queue(c);for(let i=0;i<3;i++)q=JSON.parse(JSON.stringify(q));let g=applyQueuedWingGuardTactic(q,c.adversaries[0].id).combat;for(let i=0;i<3;i++)g=JSON.parse(JSON.stringify(g));assert.equal(g.adversaries[1].range,'Curto');assert.equal(g.log.at(-1).type,'WING_GUARD_FREE_MOVE');
});
test('P198 real NPC turn consumes free movement before range-dependent attack roll',()=>{
 class Repo{constructor(){this.state=createCampaign('P198')}load(){return clone(this.state)}save(s){this.state=clone(s)}}
 const g=new GameController(new Repo());g.loadStarterCharacter('cael');const c=setup('Médio');const a=c.adversaries[1].id;const noMove=g._resolveNpcTurn(c,g.state.player.character,a,()=>.5,g.state.group);
 const withGrant=g._resolveNpcTurn(granted(c),g.state.player.character,a,()=>.5,g.state.group);
 assert.equal(withGrant.combat.adversaries[1].range,'Curto');assert.equal(withGrant.combat.adversaries[1].wingFreeManeuver,undefined);
 assert.equal(withGrant.combat.log.some(x=>x.type==='WING_GUARD_FREE_MOVE'),true);
 assert.ok(withGrant.attack?.check||withGrant.summary);assert.ok(noMove.attack?.check||noMove.summary);
});
test('P198 does not move an incapacitated target and cancels its one-time grant',()=>{
 const c=setup(),q=queue(c);q.adversaries[1].wounds.current=10;const r=applyQueuedWingGuardTactic(q,c.adversaries[0].id);assert.equal(r.cancelled,true);assert.equal(r.combat.adversaries[1].range,'Médio');
});
test('P198 Android bridge/UI exposes free-maneuver direction and explicit path confirmation',()=>{
 const src=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');assert.match(src,/mode:kind.value==='tactical_direction'\?mode.value/);assert.match(src,/confirmedMovement:!direction.disabled&&path.checked/);assert.match(src,/Manobra livre imediata: mover 1 faixa/);
});
test('P198 sergeant real NPC initiative triggers immediate minion movement without consuming minion slot',()=>{
 class Repo{constructor(){this.state=createCampaign('P198')}load(){return clone(this.state)}save(s){this.state=clone(s)}}
 const g=new GameController(new Repo());g.loadStarterCharacter('cael');const c=setup('Médio');const q=queue(c);
 const s=q.adversaries[0].id;const r=g._resolveNpcTurn(q,g.state.player.character,s,()=>.5,g.state.group);
 assert.equal(r.combat.adversaries[1].range,'Curto');assert.equal(r.combat.log.some(x=>x.type==='WING_GUARD_FREE_MOVE'),true);
 assert.equal(r.combat.wingGuardOrders.length,0);assert.equal(r.combat.adversaries[0].wingGuardManeuverRound,1);
});
test('P198 minion may still use its regular NPC action after immediate free maneuver',()=>{
 class Repo{constructor(){this.state=createCampaign('P198')}load(){return clone(this.state)}save(s){this.state=clone(s)}}
 const g=new GameController(new Repo());g.loadStarterCharacter('cael');const c=setup('Médio');const moved=granted(c);
 const result=g._resolveNpcTurn(moved,g.state.player.character,c.adversaries[1].id,()=>.5,g.state.group);
 assert.equal(result.combat.adversaries[1].range,'Curto');assert.equal(result.combat.log.filter(x=>x.type==='WING_GUARD_FREE_MOVE').length,1);
 assert.equal(result.combat.adversaries[1].wingFreeManeuver,undefined);
});
