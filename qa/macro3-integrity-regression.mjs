import {validateCampaignState} from '../app/src/main/assets/www/core/campaign-integrity.js';
const base={schemaVersion:1,authorityBindings:{system:'WWN'},campaignId:'c',revision:1,rngState:{masterSeed:'x',counters:{}},charactersState:{p:{characterId:'p',advancedMagic:{labs:[],workings:[],itemProjects:[]}}},characterState:null,partyState:{groups:{}},worldState:{},factionState:{},encounterState:{combat:{active:true,round:1,pcId:'p',specialState:{proneByCombatant:{},holds:{},screens:{},grapples:{},maneuverHistory:[]}}}};
let r=validateCampaignState(base);if(!r.ok)throw new Error(r.errors.join(','));
const bad=structuredClone(base);bad.charactersState.p.advancedMagic.workings={};bad.encounterState.combat.specialState.grapples=[];r=validateCampaignState(bad);if(r.ok||!r.errors.includes('ADVANCED_MAGIC_WORKINGS_INVALID:p')||!r.errors.includes('SPECIAL_COMBAT_MAP_INVALID:grapples'))throw new Error('deep integrity failed');
console.log('MACRO3_INTEGRITY PASS');
