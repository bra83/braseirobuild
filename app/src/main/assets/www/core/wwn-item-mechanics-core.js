const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.8.1–2.8.4 Crafting and Modifying Gear'});
const f=v=>Object.freeze(v);
function activeMods(item,characterId){return (item?.mods??[]).filter(m=>m?.status==='ACTIVE'&&(m.specificUserId==null||String(m.specificUserId)===String(characterId)));}
function count(mods,id){return mods.filter(m=>String(m.modId).toUpperCase()===id).length;}
function capped(v){return Math.min(3,Math.max(0,v));}
export function weaponMechanicalBonuses(character,item){
  if(!item||String(item.kind).toUpperCase()!=='WEAPON')return f({hitBonus:0,damageBonus:0,shockBonus:0,rangeMultiplier:1,selfGeneratingAmmo:false,automaticReloadOncePerScene:false,returnsAfterAttack:false,sourceRefs:[SRC]});
  const mods=activeMods(item,character?.characterId);let hit=0;if(item?.crafting?.quality==='MASTERCRAFTED')hit+=Number(item.crafting.mastercraftedWeaponHitBonus??1);
  hit+=count(mods,'CUSTOMIZED');hit+=count(mods,'OMENED_AIM');hit+=count(mods,'THIRSTING_BLADE');
  const razor=count(mods,'RAZOR_EDGE')*2;
  return f({hitBonus:capped(hit),damageBonus:capped(razor),shockBonus:capped(razor),rangeMultiplier:count(mods,'LONG_ARM')>0?2:1,selfGeneratingAmmo:count(mods,'ARROW_STORM')>0,automaticReloadOncePerScene:count(mods,'AUTOMATIC_RELOAD')>0,returnsAfterAttack:count(mods,'FLYING_RAZOR')>0,sourceRefs:[SRC]});
}
export function armorMechanicalBonuses(character,item){
  if(!item||String(item.kind).toUpperCase()!=='ARMOR')return f({armorClassBonus:0,preservingGrace:false,harmonizedAegis:false,sourceRefs:[SRC]});
  const mods=activeMods(item,character?.characterId);return f({armorClassBonus:capped(count(mods,'CUSTOMIZED')),preservingGrace:count(mods,'PRESERVING_GRACE')>0,harmonizedAegis:count(mods,'HARMONIZED_AEGIS')>0,sourceRefs:[SRC]});
}
export function augmentedGearSkillBonus(character,item,{purpose}={}){
  const mods=activeMods(item,character?.characterId).filter(m=>String(m.modId).toUpperCase()==='AUGMENTED_GEAR'&&String(m.chosenPurpose??'')===String(purpose??''));return f({bonus:mods.length?1:0,sourceRefs:[SRC]});
}
