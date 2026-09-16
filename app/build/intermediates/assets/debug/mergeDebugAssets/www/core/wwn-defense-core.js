export const WWN_DEFENSE_SOURCES=Object.freeze({
  shock:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.4.6.4 Shock'}),
  shields:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.3.2 Shields'}),
  grandPlate:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.3.3 Types of Armor — Grand Plate'}),
  rangedMelee:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§3.4.1 Weapon Statistics; §3.4.3 2H/T'}),
  totalDefense:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§2.4.4 Total Defense'}),
});

function readied(character){return Array.isArray(character?.inventory)?character.inventory.filter(i=>String(i?.carryState??'').toUpperCase()==='READIED'):[];}
function conditions(character){const raw=character?.conditions??[];return Array.isArray(raw)?raw.map(c=>String(typeof c==='string'?c:(c?.id??c?.type??c?.name??'')).toUpperCase()):Object.entries(raw).filter(([,v])=>Boolean(v)).map(([k])=>String(k).toUpperCase());}
export function readiedShield(character){return readied(character).filter(i=>i?.kind==='SHIELD'&&Number.isInteger(i.shieldBaseArmorClass)).sort((a,b)=>b.shieldBaseArmorClass-a.shieldBaseArmorClass||String(a.itemId??a.catalogId).localeCompare(String(b.itemId??b.catalogId)))[0]??null;}
export function readiedGrandPlate(character){return readied(character).find(i=>i?.kind==='ARMOR'&&(i.specialRules??[]).includes('GRAND_PLATE_WEAPON_IMMUNITY'))??null;}
export function isGrappled(character){return conditions(character).includes('GRAPPLED');}

export function grandPlateWeaponImmunity({character,weapon}){
  const armor=readiedGrandPlate(character);if(!armor)return Object.freeze({status:'NOT_APPLICABLE',blocked:false,sourceRefs:[WWN_DEFENSE_SOURCES.grandPlate]});
  const traits=[...(weapon?.traits??[])];const magical=weapon?.magical===true;const melee=weapon?.melee!==false;const thrown=weapon?.melee===false&&traits.includes('T');
  if(magical||(!melee&&!thrown)||traits.includes('2H')||Number(weapon?.shock?.ac)>=16||isGrappled(character))return Object.freeze({status:'BYPASSED',blocked:false,armorItemId:armor.itemId??null,sourceRefs:[WWN_DEFENSE_SOURCES.grandPlate]});
  return Object.freeze({status:'BLOCKED',blocked:true,armorItemId:armor.itemId??null,reason:'GRAND_PLATE_NONMAGICAL_MELEE_OR_THROWN_IMMUNITY',sourceRefs:[WWN_DEFENSE_SOURCES.grandPlate]});
}

export function shockContributedToDamage(resolution){
  if(!resolution?.shockEligible)return false;
  if(resolution.hit!==true)return Number(resolution.damage??0)>0;
  const pre=Number(resolution.preShockDamage??resolution.damage??0);return Number(resolution.damage??0)>pre;
}


export function totalDefenseActive(combat,targetId){return combat?.defenseState?.[targetId]?.totalDefenseActive===true;}
export function setTotalDefense(combat,targetId,active=true){
  if(!combat||!targetId)throw new TypeError('combat and targetId required');
  combat.defenseState={...(combat.defenseState??{}),[targetId]:{...(combat.defenseState?.[targetId]??{}),totalDefenseActive:active===true,totalDefenseActivatedRound:active===true?combat.round:null}};
  return combat.defenseState[targetId];
}
export function clearTotalDefenseForPcSide(combat){
  if(!combat?.defenseState)return combat;
  for(const id of Object.keys(combat.defenseState)){if(combat.defenseState[id]?.totalDefenseActive===true)combat.defenseState[id]={...combat.defenseState[id],totalDefenseActive:false,totalDefenseClearedRound:combat.round};}
  return combat;
}

export function shieldShockAvailable({character,combat,targetId}){
  const shield=readiedShield(character);if(!shield)return Object.freeze({available:false,shieldItemId:null,sourceRefs:[WWN_DEFENSE_SOURCES.shields,WWN_DEFENSE_SOURCES.shock]});
  const usedRound=combat?.defenseState?.[targetId]?.shieldShockUsedRound;return Object.freeze({available:usedRound!==combat?.round,shieldItemId:shield.itemId??shield.catalogId??null,usedRound:Number.isInteger(usedRound)?usedRound:null,sourceRefs:[WWN_DEFENSE_SOURCES.shields,WWN_DEFENSE_SOURCES.shock]});
}

export function applyDefensesToAttack({character,weapon,resolution,combat=null,targetId=null}){
  if(!resolution||typeof resolution!=='object')throw new TypeError('resolution required');
  const grand=grandPlateWeaponImmunity({character,weapon});
  if(grand.blocked){return Object.freeze({resolution:Object.freeze({...resolution,damageBeforeDefense:resolution.damage,damage:0,grandPlateBlocked:true,shieldShockNegated:false}),consumeShieldShock:false,grandPlate:grand,shield:Object.freeze({available:false,shieldItemId:null}),sourceRefs:[...grand.sourceRefs]});}
  const contributes=shockContributedToDamage(resolution);const totalDefense=totalDefenseActive(combat,targetId);
  if(totalDefense&&contributes){const reduced=resolution.hit===true?Math.max(0,Number(resolution.preShockDamage??0)):0;return Object.freeze({resolution:Object.freeze({...resolution,damageBeforeDefense:resolution.damage,damage:reduced,grandPlateBlocked:false,totalDefenseShockImmune:true,shieldShockNegated:false}),consumeShieldShock:false,grandPlate:grand,shield:Object.freeze({available:false,shieldItemId:null}),totalDefense:true,sourceRefs:[WWN_DEFENSE_SOURCES.totalDefense,WWN_DEFENSE_SOURCES.shock]});}
  const shield=shieldShockAvailable({character,combat,targetId});
  if(shield.available&&contributes){const reduced=resolution.hit===true?Math.max(0,Number(resolution.preShockDamage??0)):0;return Object.freeze({resolution:Object.freeze({...resolution,damageBeforeDefense:resolution.damage,damage:reduced,grandPlateBlocked:false,totalDefenseShockImmune:false,shieldShockNegated:true,shieldItemId:shield.shieldItemId}),consumeShieldShock:true,grandPlate:grand,shield,totalDefense:false,sourceRefs:[...shield.sourceRefs]});}
  return Object.freeze({resolution:Object.freeze({...resolution,damageBeforeDefense:resolution.damage,grandPlateBlocked:false,totalDefenseShockImmune:false,shieldShockNegated:false}),consumeShieldShock:false,grandPlate:grand,shield,totalDefense,totalDefenseActive:totalDefense,sourceRefs:[...grand.sourceRefs,...shield.sourceRefs,...(totalDefense?[WWN_DEFENSE_SOURCES.totalDefense]:[])]});
}

export function recordShieldShockUse(combat,targetId){if(!combat||!targetId)throw new TypeError('combat and targetId required');combat.defenseState={...(combat.defenseState??{}),[targetId]:{...(combat.defenseState?.[targetId]??{}),shieldShockUsedRound:combat.round}};return combat.defenseState[targetId];}

export function inspectRangedWeaponInMelee({weapon,attackerPosition,enemyPositions=[]}){
  if(!weapon||weapon.melee!==false)return Object.freeze({status:'OK',hitModifier:0,engagedEnemyIds:Object.freeze([]),sourceRefs:[WWN_DEFENSE_SOURCES.rangedMelee]});
  const engaged=[];for(const e of enemyPositions??[]){const p=e?.position;if(!p)continue;const dx=Number(p.xFeet)-Number(attackerPosition?.xFeet),dy=Number(p.yFeet)-Number(attackerPosition?.yFeet);if(Number.isFinite(dx)&&Number.isFinite(dy)&&Math.hypot(dx,dy)<=5+1e-9)engaged.push(String(e.actorId));}
  if(!engaged.length)return Object.freeze({status:'OK',hitModifier:0,engagedEnemyIds:Object.freeze([]),sourceRefs:[WWN_DEFENSE_SOURCES.rangedMelee]});
  if((weapon.traits??[]).includes('2H'))return Object.freeze({status:'TWO_HANDED_RANGED_BLOCKED_IN_MELEE',hitModifier:0,engagedEnemyIds:Object.freeze(engaged),sourceRefs:[WWN_DEFENSE_SOURCES.rangedMelee]});
  return Object.freeze({status:'OK',hitModifier:-4,engagedEnemyIds:Object.freeze(engaged),sourceRefs:[WWN_DEFENSE_SOURCES.rangedMelee]});
}
