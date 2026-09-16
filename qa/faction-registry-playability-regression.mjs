import assert from 'node:assert/strict';
import {FactionRegistryService} from '../app/src/main/assets/www/core/faction-registry-service.js';
import {WorldActionService} from '../app/src/main/assets/www/core/world-service.js';
import {makeEmptyCampaignState} from '../app/src/main/assets/www/core/campaign-state.js';
class P{constructor(s){this.s=structuredClone(s);this.events=[]}loadCampaign(){return{status:'OK',revision:this.s.revision,stateJson:JSON.stringify(this.s)}}commit(x){assert.equal(x.expectedRevision,this.s.revision);this.s=JSON.parse(x.newStateJson);this.events.push(x);return{status:'OK',revision:this.s.revision}}}
const state=makeEmptyCampaignState({campaignId:'c',displayName:'C',masterSeed:'seed'});const p=new P(state),reg=new FactionRegistryService({persistence:p}),world=new WorldActionService({persistence:p});
let r=reg.upsert({campaignId:'c',factionId:'salt',name:'Companhia do Sal',force:2,cunning:3,wealth:4,treasure:5,goal:'Controlar rotas'});assert.equal(r.status,'OK');assert.equal(r.faction.maxHp,12);assert.equal(r.faction.hp,12);
r=reg.upsert({campaignId:'c',factionId:'ash',name:'Cinzas',force:3,cunning:2,wealth:2,treasure:2});assert.equal(r.status,'OK');
let turn=world.startFactionTurn({campaignId:'c'});assert.ok(['UPKEEP_REQUIRED','GM_TIEBREAK_REQUIRED_NO_MUTATION'].includes(turn.status));
if(turn.status==='GM_TIEBREAK_REQUIRED_NO_MUTATION'){turn=world.startFactionTurn({campaignId:'c',tieBreakOrder:['salt','ash']});assert.equal(turn.status,'UPKEEP_REQUIRED')}
assert.equal(reg.remove({campaignId:'c',factionId:'salt'}).status,'ACTIVE_FACTION_TURN_NO_MUTATION');
console.log('faction-registry-playability-regression: PASS');
