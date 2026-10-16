import { generalGearCatalogEntry, weaponCatalogEntry, armorCatalogEntry } from './equipmentCatalog.mjs';

const clone=v=>structuredClone(v);
const number=v=>Number.isFinite(Number(v))?Number(v):0;
const norm=s=>String(s??'').trim().toLocaleLowerCase('pt-BR');

function quantity(item){const q=Number(item?.quantity??1);return Number.isInteger(q)&&q>0?q:1;}
function byName(list,name){const n=norm(name);return list.find(x=>norm(x.name)===n||norm(x.sourceNameEs)===n)??null;}

export function baseEncumbranceThreshold(character){
  const brawn=number(character?.characteristics?.fortaleza);
  return 5+brawn;
}

export function encumbranceThresholdBonus(character){
  let bonus=0;
  for(const item of character?.gear??[]){
    const id=item?.id??null;
    const entry=id?generalGearCatalogEntry(id):generalGearCatalogEntryByName(item?.name);
    const q=quantity(item);
    if(entry?.encumbranceThresholdBonus) bonus+=Number(entry.encumbranceThresholdBonus)*q;
  }
  return bonus;
}

export function encumbranceThreshold(character){return baseEncumbranceThreshold(character)+encumbranceThresholdBonus(character);}

export function weaponEncumbrance(item){
  if(Number.isFinite(Number(item?.encumbrance))) return Math.max(0,Number(item.encumbrance));
  const ref=(item?.id&&weaponCatalogEntry(item.id))||weaponCatalogEntryByName(item?.name);
  return Math.max(0,number(ref?.encumbrance));
}

export function armorEncumbrance(item,{worn=true}={}){
  const ref=(item?.id&&armorCatalogEntry(item.id))||armorCatalogEntryByName(item?.name);
  const raw=Number.isFinite(Number(item?.encumbrance))?Number(item.encumbrance):number(ref?.encumbrance);
  return Math.max(0,raw-(worn?3:0));
}

export function gearEncumbrance(item){
  if(Number.isFinite(Number(item?.encumbrance))) return Math.max(0,Number(item.encumbrance));
  const ref=(item?.id&&generalGearCatalogEntry(item.id))||generalGearCatalogEntryByName(item?.name);
  return Math.max(0,number(ref?.encumbrance));
}

export function generalGearCatalogEntryByName(name){
  const catalog=[
    'glow_rod','comlink','long_range_comlink','electrobinoculars','hand_scanner','multi_scanner','electronic_lockpick','binders','stimpack_injector','tool_kit','macrobinoculars','restraining_device','restraining_bolt','slicer_gear','breath_mask','disguise_material','medpac','emergency_medpac','backpack','data_module','emergency_repair_patch','rations','space_suit','power_pack_or_ammo_reload','utility_belt','fusion_lantern','climbing_gear'
  ].map(generalGearCatalogEntry).filter(Boolean);
  return byName(catalog,name);
}
export function weaponCatalogEntryByName(name){
  const n=norm(name); const catalog=[];
  for(const id of ['pocket_blaster','light_blaster_pistol','blaster_pistol','heavy_blaster_pistol','blaster_carbine','blaster_rifle','heavy_blaster_rifle','light_repeating_blaster','heavy_repeating_blaster','ion_blaster','disruptor_pistol','disruptor_rifle','slugthrower_pistol','slugthrower_rifle','flamethrower','missile_tube','stun_grenade','frag_grenade','concussion_grenade','anti_personnel_mine','anti_vehicle_mine','thermal_detonator','shock_gloves','brass_knuckles','combat_knife','force_pike','truncheon','lightsaber','vibroknife','vibrosword','vibroaxe']){const e=weaponCatalogEntry(id);if(e)catalog.push(e);} return catalog.find(x=>norm(x.name)===n)??null;
}
export function armorCatalogEntryByName(name){
  const n=norm(name); const catalog=[];
  for(const id of ['adverse_environment_gear','armored_clothing','heavy_battle_armor','heavy_clothing','laminate_armor','personal_deflector_shield','padded_armor']){const e=armorCatalogEntry(id);if(e)catalog.push(e);} return catalog.find(x=>norm(x.name)===n)??null;
}

export function characterEncumbrance(character){
  let total=0; const details=[];
  for(const item of character?.weapons??[]){const v=weaponEncumbrance(item)*quantity(item);total+=v;details.push({kind:'weapon',name:item.name,value:v});}
  for(const item of character?.armor??[]){const worn=item?.worn!==false;const v=armorEncumbrance(item,{worn})*quantity(item);total+=v;details.push({kind:'armor',name:item.name,value:v,worn});}
  for(const item of character?.gear??[]){
    const armor=armorCatalogEntryByName(item?.name);
    const v=(armor?armorEncumbrance({...armor,...item},{worn:item?.worn!==false}):gearEncumbrance(item))*quantity(item);
    total+=v; details.push({kind:armor?'armor':'gear',name:item.name,value:v,worn:armor?item?.worn!==false:undefined});
  }
  return {total,details};
}

export function encumbranceStatus(character){
  const threshold=encumbranceThreshold(character); const inventory=characterEncumbrance(character); const over=Math.max(0,inventory.total-threshold); const brawn=Math.max(0,number(character?.characteristics?.fortaleza));
  return {value:inventory.total,total:inventory.total,threshold,over,setback:over,setbackOnBrawnAgility:over,severelyEncumbered:over>=brawn&&over>0,noFreeManeuver:over>=brawn&&over>0,brawn,details:inventory.details};
}

export function encumbranceSetback(character,characteristicKey){
  if(!['fortaleza','agilidad'].includes(characteristicKey)) return 0;
  return encumbranceStatus(character).setback;
}

export function liftingDifficulty(character,objectEncumbrance,{helpers=[],helperBrawn=[]}={}){
  const helperValues=[...(helpers??[]),...(helperBrawn??[]).map(b=>({brawn:b}))];
  const helperTotal=helperValues.reduce((sum,h)=>sum+Math.max(0,number(h?.characteristics?.fortaleza??h?.brawn)),0);
  const threshold=encumbranceThreshold(character)+helperTotal;
  const over=Math.max(0,Math.ceil(number(objectEncumbrance)-threshold));
  return {difficulty:Math.min(4,over),possible:over<=4,threshold,objectEncumbrance:number(objectEncumbrance),over,helperBrawn:helperTotal};
}

export function encumbranceSnapshot(character){return clone(encumbranceStatus(character));}
