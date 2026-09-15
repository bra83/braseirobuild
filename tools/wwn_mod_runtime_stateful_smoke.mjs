import { EquipmentModRuntimeService, harmonizedAegisProtects } from '../app/src/main/assets/www/core/equipment-mod-runtime-service.js';
import { RNG_ALGORITHM } from '../app/src/main/assets/www/core/rng-core.js';
const assert=(v,m)=>{if(!v)throw new Error(m)};
class MemoryPersistence{
 constructor(state){this.state=structuredClone(state);}
 loadCampaign(){return {status:'OK',revision:this.state.revision,stateJson:JSON.stringify(this.state)};}
 commit({expectedRevision,newStateJson,eventId}){if(expectedRevision!==this.state.revision)return {status:'REVISION_CONFLICT'};this.state=JSON.parse(newStateJson);return {status:'OK',revision:this.state.revision,eventId};}
}
const weapon=(id,{traits=['R'],ammoType='ARROW',mods=[]}={})=>({itemId:id,catalogId:id,kind:'WEAPON',name:id,encumbrance:1,carryState:'READIED',traits,ammoType,loaded:false,mods,weaponModes:[{modeId:'RANGED',combatSkill:'Shoot',attributeOptions:['Dexterity'],damage:'1d6',melee:false,rangeFeet:{normal:50,long:300},traits}]});
const ammo=(id,type,qty=20)=>({itemId:id,kind:'AMMO',name:id,ammoType:type,quantity:qty,encumbrance:1,carryState:'READIED'});
function baseState(inventory){const pc={characterId:'pc',level:3,attributes:{Strength:10,Dexterity:12,Constitution:10,Intelligence:12,Wisdom:10,Charisma:10},skills:{Shoot:1,Notice:1,Craft:3},hp:{current:10,max:10},conditions:[],inventory};return {revision:0,rngState:{algorithm:RNG_ALGORITHM,masterSeed:'mod-runtime-smoke',counters:{}},characterState:structuredClone(pc),charactersState:{pc:structuredClone(pc)},worldState:{}};}

// Ordinary R reload uses Shoot-1 On Turn and consumes one Readied ammo.
{
 const p=new MemoryPersistence(baseState([weapon('bow'),ammo('arrows','ARROW',3)])),svc=new EquipmentModRuntimeService({persistence:p});
 const r=svc.reloadWeapon({campaignId:'c',pcId:'pc',weaponId:'bow'});assert(r.status==='OK'&&r.cost==='ON_TURN'&&r.ammoConsumed===true,'R reload');assert(p.state.charactersState.pc.inventory.find(x=>x.itemId==='arrows').quantity===2,'R ammo decrement');assert(p.state.charactersState.pc.inventory.find(x=>x.itemId==='bow').loaded===true,'R loaded');
}
// Arrow Storm self-generates ammo but keeps ordinary reload speed.
{
 const bow=weapon('storm',{mods:[{modId:'ARROW_STORM',status:'ACTIVE'}]});const p=new MemoryPersistence(baseState([bow])),svc=new EquipmentModRuntimeService({persistence:p});const r=svc.reloadWeapon({campaignId:'c',pcId:'pc',weaponId:'storm'});assert(r.status==='OK'&&r.cost==='ON_TURN'&&r.ammoConsumed===false,'Arrow Storm reload');
}
// Automatic Reload works once per scene and still consumes hurlant ammunition.
{
 const h=weapon('hurlant',{traits:['SS'],ammoType:'HURLANT_BOLT',mods:[{modId:'AUTOMATIC_RELOAD',status:'ACTIVE'}]});const p=new MemoryPersistence(baseState([h,ammo('bolts','HURLANT_BOLT',2)])),svc=new EquipmentModRuntimeService({persistence:p});let r=svc.reloadWeapon({campaignId:'c',pcId:'pc',weaponId:'hurlant',automatic:true,sceneToken:'scene-A'});assert(r.status==='OK'&&r.cost==='ON_TURN'&&r.automaticUsed===true,'automatic reload');p.state.charactersState.pc.inventory.find(x=>x.itemId==='hurlant').loaded=false;p.state.characterState=structuredClone(p.state.charactersState.pc);r=svc.reloadWeapon({campaignId:'c',pcId:'pc',weaponId:'hurlant',automatic:true,sceneToken:'scene-A'});assert(r.status==='AUTOMATIC_RELOAD_ALREADY_USED_THIS_SCENE_NO_MUTATION','automatic once scene');
}
// SS takes ten continuous rounds and consumes ammo only on completion.
{
 const h=weapon('single',{traits:['SS'],ammoType:'HURLANT_BOLT'});const p=new MemoryPersistence(baseState([h,ammo('bolts','HURLANT_BOLT',2)])),svc=new EquipmentModRuntimeService({persistence:p});for(let round=1;round<=9;round++){const r=svc.reloadWeapon({campaignId:'c',pcId:'pc',weaponId:'single',currentRound:round});assert(r.status==='OK'&&r.completed===false,`SS progress ${round}`);}let r=svc.reloadWeapon({campaignId:'c',pcId:'pc',weaponId:'single',currentRound:10});assert(r.status==='OK'&&r.completed===true&&r.ammoConsumed===true,'SS complete');assert(p.state.charactersState.pc.inventory.find(x=>x.itemId==='bolts').quantity===1,'SS ammo at completion');
}
// Assassin's Trinket, Manifold Mail, Harmonized Aegis and Preserving Grace have persistent state.
{
 const dagger=weapon('dagger',{traits:[],ammoType:null,mods:[{modId:'ASSASSINS_TRINKET',status:'ACTIVE'}]});
 const armor={itemId:'armor',kind:'ARMOR',name:'armor',encumbrance:1,carryState:'READIED',mods:[{modId:'MANIFOLD_MAIL',status:'ACTIVE'},{modId:'HARMONIZED_AEGIS',status:'ACTIVE'}]};
 const grace={itemId:'grace',kind:'ARMOR',name:'grace',encumbrance:1,carryState:'READIED',mods:[{modId:'PRESERVING_GRACE',status:'ACTIVE'}]};
 const p=new MemoryPersistence(baseState([dagger,armor,grace])),svc=new EquipmentModRuntimeService({persistence:p});
 let r=svc.toggleAssassinsTrinket({campaignId:'c',pcId:'pc',itemId:'dagger'});assert(r.status==='OK'&&r.value==='JEWELRY','trinket jewelry');
 r=svc.configureManifoldMail({campaignId:'c',pcId:'pc',itemId:'armor',appearances:['a','b','c','d','e']});assert(r.status==='OK','manifold config');r=svc.toggleManifoldMail({campaignId:'c',pcId:'pc',itemId:'armor',appearanceIndex:3});assert(r.status==='OK'&&r.appearance==='d','manifold toggle');
 r=svc.coordinateHarmonizedAegis({campaignId:'c',pcId:'pc',itemId:'armor',casterId:'mage',dayToken:'day-1',coordinationMinutesEvidence:10});assert(r.status==='OK','aegis coordinate');const pc=p.state.charactersState.pc;assert(harmonizedAegisProtects(pc,pc.inventory.find(x=>x.itemId==='armor'),{casterId:'mage',dayToken:'day-1'}).protected===true,'aegis protects');
 p.state.charactersState.pc.hp.current=0;p.state.charactersState.pc.conditions=['MORTALLY_INJURED'];p.state.charactersState.pc.mortalInjury={stabilized:false,dead:false};p.state.characterState=structuredClone(p.state.charactersState.pc);r=svc.usePreservingGrace({campaignId:'c',pcId:'pc',itemId:'grace',weekToken:'week-1'});assert(r.status==='OK'&&r.character.mortalInjury.stabilized===true,'preserving grace');const again=svc.usePreservingGrace({campaignId:'c',pcId:'pc',itemId:'grace',weekToken:'week-1'});assert(again.status==='PRESERVING_GRACE_ALREADY_USED_THIS_WEEK_NO_MUTATION','grace once week');
}
// Augmented Gear contributes exactly +1 for its chosen purpose.
{
 const tool={itemId:'tool',kind:'GEAR',name:'tool',encumbrance:1,carryState:'READIED',mods:[{modId:'AUGMENTED_GEAR',status:'ACTIVE',chosenPurpose:'track ruins'}]};const p=new MemoryPersistence(baseState([tool])),svc=new EquipmentModRuntimeService({persistence:p});const r=svc.augmentedSkillCheck({campaignId:'c',pcId:'pc',itemId:'tool',purpose:'track ruins',skillName:'Notice',attributeName:'Wisdom',difficulty:7});assert(r.status==='OK'&&r.resolution.equipmentBonus===1,'augmented gear +1');
}
console.log('WWN equipment/mod runtime stateful smoke tests: OK');
