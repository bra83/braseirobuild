import { EquipmentModRuntimeService } from '../app/src/main/assets/www/core/equipment-mod-runtime-service.js';
import { RNG_ALGORITHM } from '../app/src/main/assets/www/core/rng-core.js';
const assert=(v,m)=>{if(!v)throw new Error(m)};
class MemoryPersistence{constructor(state){this.state=structuredClone(state)}loadCampaign(){return{status:'OK',revision:this.state.revision,stateJson:JSON.stringify(this.state)}}commit({expectedRevision,newStateJson,eventId}){if(expectedRevision!==this.state.revision)return{status:'REVISION_CONFLICT'};this.state=JSON.parse(newStateJson);return{status:'OK',revision:this.state.revision,eventId}}}
const great={itemId:'great',kind:'WEAPON',name:'Great Hurlant',encumbrance:15,carryState:'READIED',traits:['SS'],ammoType:'HURLANT_BOLT',loaded:false,mods:[{modId:'AUTOMATIC_RELOAD',status:'ACTIVE'}]};
const bolts={itemId:'bolts',kind:'AMMO',name:'bolts',ammoType:'HURLANT_BOLT',quantity:2,encumbrance:1,carryState:'READIED'};
const pc={characterId:'pc',attributes:{Strength:10,Dexterity:10,Constitution:10,Intelligence:10,Wisdom:10,Charisma:10},skills:{Shoot:1},hp:{current:10,max:10},inventory:[great,bolts]};
const state={revision:0,rngState:{algorithm:RNG_ALGORITHM,masterSeed:'auto-reload-portability',counters:{}},characterState:structuredClone(pc),charactersState:{pc:structuredClone(pc)}};
const svc=new EquipmentModRuntimeService({persistence:new MemoryPersistence(state)});
const r=svc.reloadWeapon({campaignId:'c',pcId:'pc',weaponId:'great',automatic:true,sceneToken:'scene-A'});
assert(r.status==='AUTOMATIC_RELOAD_ON_TURN_REQUIRES_MAN_PORTABLE_HURLANT_NO_MUTATION','Great Hurlant must not receive man-portable On Turn reload');
console.log('WWN Automatic Reload portability smoke: OK');
