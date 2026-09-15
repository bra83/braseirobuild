export const WWN_RELOAD_SOURCES=Object.freeze({
  traits:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.4.3 Weapon Traits — R/SR/SS'}),
  common:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.4.4 Reload a Weapon'}),
  bows:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.4.2 Bows/Crossbows'}),
});

export function reloadTrait(weaponOrItem){
  const traits=weaponOrItem?.traits??weaponOrItem?.weaponContract?.traits??[];
  if(traits.includes('SS'))return 'SS';
  if(traits.includes('SR'))return 'SR';
  if(traits.includes('R'))return 'R';
  return null;
}

export function reloadActionCost({weaponOrItem,shootSkillLevel=null}){
  const trait=reloadTrait(weaponOrItem);
  if(trait===null)return Object.freeze({status:'NOT_REQUIRED',trait:null,sourceRefs:[WWN_RELOAD_SOURCES.traits]});
  if(trait==='R')return Object.freeze({status:'OK',trait,cost:Number.isInteger(shootSkillLevel)&&shootSkillLevel>=1?'ON_TURN':'MOVE',roundsRequired:0,sourceRefs:[WWN_RELOAD_SOURCES.traits,WWN_RELOAD_SOURCES.common,WWN_RELOAD_SOURCES.bows]});
  if(trait==='SR')return Object.freeze({status:'OK',trait,cost:'MAIN',roundsRequired:0,sourceRefs:[WWN_RELOAD_SOURCES.traits,WWN_RELOAD_SOURCES.common,WWN_RELOAD_SOURCES.bows]});
  return Object.freeze({status:'OK',trait,cost:'CONTINUOUS_ROUNDS',roundsRequired:10,sourceRefs:[WWN_RELOAD_SOURCES.traits,WWN_RELOAD_SOURCES.common]});
}

export function compatibleReadiedAmmo(inventory,ammoType){
  if(!Array.isArray(inventory)||!ammoType)return null;
  return inventory.find(i=>i?.kind==='AMMO'&&i?.ammoType===ammoType&&String(i?.carryState??'STOWED').toUpperCase()==='READIED'&&Number.isInteger(i?.quantity)&&i.quantity>0)??null;
}

export function singleShotReloadProgress(item){
  const raw=item?.reloadProgress;
  if(!raw||raw.trait!=='SS')return null;
  const completed=Number(raw.roundsCompleted),required=Number(raw.roundsRequired),lastRound=Number(raw.lastProgressRound);
  if(!Number.isInteger(completed)||completed<1||!Number.isInteger(required)||required<1||!Number.isInteger(lastRound)||lastRound<1)return null;
  return Object.freeze({trait:'SS',roundsCompleted:Math.min(completed,required),roundsRequired:required,startedRound:Number.isInteger(Number(raw.startedRound))?Number(raw.startedRound):lastRound,lastProgressRound:lastRound});
}

export function nextSingleShotReloadProgress({item,currentRound,roundsRequired=10}){
  if(!Number.isInteger(currentRound)||currentRound<1)throw new RangeError('currentRound must be >= 1');
  if(!Number.isInteger(roundsRequired)||roundsRequired<1)throw new RangeError('roundsRequired must be >= 1');
  const prior=singleShotReloadProgress(item);
  if(prior&&prior.lastProgressRound===currentRound)return Object.freeze({status:'ALREADY_PROGRESS_THIS_ROUND',progress:prior,completed:false,restarted:false});
  const continuous=prior&&prior.lastProgressRound===currentRound-1;
  const completed=Math.min(roundsRequired,continuous?prior.roundsCompleted+1:1);
  const progress=Object.freeze({trait:'SS',roundsCompleted:completed,roundsRequired,startedRound:continuous?prior.startedRound:currentRound,lastProgressRound:currentRound});
  return Object.freeze({status:'OK',progress,completed:completed>=roundsRequired,restarted:Boolean(prior&&!continuous)});
}

export function interruptSingleShotReload(item){
  const prior=singleShotReloadProgress(item);
  if(!prior)return Object.freeze({status:'NO_PROGRESS',interrupted:false,progress:null});
  return Object.freeze({status:'OK',interrupted:true,progress:prior});
}

export function inspectWeaponReadiness({item,inventory,shootSkillLevel=null}){
  if(!item||item.kind!=='WEAPON')return Object.freeze({status:'NOT_A_WEAPON'});
  if(String(item.carryState??'').toUpperCase()!=='READIED')return Object.freeze({status:'WEAPON_NOT_READIED'});
  const reload=reloadActionCost({weaponOrItem:item,shootSkillLevel});
  if(reload.status==='NOT_REQUIRED')return Object.freeze({status:'READY',reload,ammo:null});
  if(item.loaded===true)return Object.freeze({status:'READY',reload,ammo:null});
  const ammo=compatibleReadiedAmmo(inventory,item.ammoType);
  return Object.freeze({status:ammo?'RELOAD_REQUIRED':'MISSING_READIED_AMMO',reload,ammo:ammo?Object.freeze(structuredClone(ammo)):null,progress:singleShotReloadProgress(item)});
}

export function planReload({item,inventory,shootSkillLevel=null}){
  if(!item||item.kind!=='WEAPON')return Object.freeze({status:'NOT_A_WEAPON'});
  if(String(item.carryState??'').toUpperCase()!=='READIED')return Object.freeze({status:'WEAPON_NOT_READIED'});
  const reload=reloadActionCost({weaponOrItem:item,shootSkillLevel});
  if(reload.status==='NOT_REQUIRED')return Object.freeze({status:'WEAPON_DOES_NOT_RELOAD'});
  if(item.loaded===true)return Object.freeze({status:'ALREADY_LOADED'});
  const ammo=compatibleReadiedAmmo(inventory,item.ammoType);if(!ammo)return Object.freeze({status:'MISSING_READIED_AMMO',reload});
  if(reload.trait==='SS')return Object.freeze({status:'SS_RELOAD_REQUIRES_ROUND_TRACKER',reload,ammoItemId:ammo.itemId??ammo.id,ammoType:item.ammoType,consume:1,progress:singleShotReloadProgress(item)});
  return Object.freeze({status:'OK',reload,ammoItemId:ammo.itemId??ammo.id,ammoType:item.ammoType,consume:1});
}
