import type { CharacterSheet } from '../db';
import type { CombatActorState } from '../master/types';
import { activeQualityIds, type Bane } from './magicalTreasure';

type InventoryItem = CharacterSheet['inventory'][number];

function norm(value:unknown):string {
  return String(value??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
}

export function magicalItemSuppressed(item:InventoryItem):boolean {
  return Boolean(item.magicalCurse==='ownership' && item.magicalCurseActive && item.magicalCurseOwnerPresent);
}

export function activeQualitiesForItem(item:InventoryItem):string[] {
  if(item.magicalKind!=='famous-war-gear' || magicalItemSuppressed(item)) return [];
  return activeQualityIds(item.magicalQualities,item.magicalQualitiesUnlocked);
}

export function famousItemForWeapon(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string}|undefined|null,
):InventoryItem|undefined {
  if(!weapon) return undefined;
  return (sheet.inventory||[]).find(item=>
    item.magicalKind==='famous-war-gear'
    && (
      (weapon.treasureIndexEntryId && item.treasureIndexEntryId===weapon.treasureIndexEntryId)
      || norm(item.name)===norm(weapon.name)
    )
  );
}

export function equippedFamousItems(sheet:CharacterSheet):InventoryItem[] {
  return (sheet.inventory||[]).filter(item=>item.magicalKind==='famous-war-gear' && item.equipped!==false && !magicalItemSuppressed(item));
}

export function itemHasQuality(item:InventoryItem|undefined,qualityId:string):boolean {
  return Boolean(item && activeQualitiesForItem(item).includes(qualityId));
}

export function actorIsBane(actor:CombatActorState,item:InventoryItem|undefined):boolean {
  if(!item||magicalItemSuppressed(item)) return false;
  const actorTags=new Set((actor.baneTags||[]).map(String));
  return (item.magicalBanes||[]).some(value=>actorTags.has(String(value)));
}

export function famousPiercingThreshold(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
  target:CombatActorState,
):number|null {
  const item=famousItemForWeapon(sheet,weapon);
  if(!itemHasQuality(item,'superior-keen')) return null;
  if(item?.magicalOrigin==='Dwarven') return 8;
  if(item?.magicalOrigin==='Elven') {
    return actorIsBane(target,item)
      ? Math.max(2,10-Math.max(1,Number(sheet.attributes.valor)||1))
      : 9;
  }
  return null;
}

export function famousDamageBonus(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
  target:CombatActorState,
):number {
  const item=famousItemForWeapon(sheet,weapon);
  if(!itemHasQuality(item,'very-fell')) return 0;
  if(item?.magicalOrigin==='Dwarven') return 2;
  if(item?.magicalOrigin==='Numenorean') {
    return actorIsBane(target,item) ? Math.max(1,Number(sheet.attributes.valor)||1) : 1;
  }
  return 0;
}

export function famousInjuryBonus(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
  target:CombatActorState,
):number {
  const item=famousItemForWeapon(sheet,weapon);
  if(!itemHasQuality(item,'superior-grievous')) return 0;
  if(item?.magicalOrigin==='Elven') return 4;
  if(item?.magicalOrigin==='Numenorean') {
    return actorIsBane(target,item) ? Math.max(2,Number(sheet.attributes.valor)||1) : 2;
  }
  return 0;
}

export function famousOnHitHateLoss(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
  target:CombatActorState,
):number {
  const item=famousItemForWeapon(sheet,weapon);
  if(!item) return 0;
  let loss=0;
  if(itemHasQuality(item,'terror-shine')) loss+=2;
  if(itemHasQuality(item,'bitter-missile')) loss+=actorIsBane(target,item)?3:1;
  return loss;
}

export function flameOfHopeRecovery(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
  successIcons:number,
):number {
  const item=famousItemForWeapon(sheet,weapon);
  return itemHasQuality(item,'flame-of-hope') ? 1+Math.max(0,Math.trunc(Number(successIcons)||0)) : 0;
}

export function foeSlayerProtection(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
  target:CombatActorState,
  piercing:boolean,
  alreadyIllFavoured:boolean,
):{illFavoured:boolean;automaticWound:boolean} {
  const item=famousItemForWeapon(sheet,weapon);
  if(!piercing || !itemHasQuality(item,'foe-slayer') || !actorIsBane(target,item)) {
    return {illFavoured:alreadyIllFavoured,automaticWound:false};
  }
  return alreadyIllFavoured
    ? {illFavoured:true,automaticWound:true}
    : {illFavoured:true,automaticWound:false};
}

export function runeScoredWeaponIgnoresConditions(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
):boolean {
  return itemHasQuality(famousItemForWeapon(sheet,weapon),'rune-scored-weapon');
}

export function straightFlightIgnoresComplications(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
):boolean {
  return itemHasQuality(famousItemForWeapon(sheet,weapon),'straight-flight');
}

export function hollowSteelExtraVolley(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
):boolean {
  return itemHasQuality(famousItemForWeapon(sheet,weapon),'hollow-steel');
}

export function cleavingEnabled(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
):boolean {
  return itemHasQuality(famousItemForWeapon(sheet,weapon),'cleaving');
}

export function heavyKnocksDown(
  sheet:CharacterSheet,
  weapon:{name:string;treasureIndexEntryId?:string;magicalBaseItemName?:string},
  target:CombatActorState,
  enduranceLoss:number,
):boolean {
  const item=famousItemForWeapon(sheet,weapon);
  if(!itemHasQuality(item,'heavy')) return false;
  const level=Math.max(1,Math.trunc(Number(target.attributeLevel)||0));
  return Math.max(0,Number(enduranceLoss)||0)>=2*level;
}

export function enchantedProtectionResultBonus(sheet:CharacterSheet):number {
  const qualifying=equippedFamousItems(sheet).filter(item=>itemHasQuality(item,'ancient-close-fitting'));
  return qualifying.length ? Math.max(3,Math.max(1,Number(sheet.attributes.valor)||1)) : 0;
}

export function runeScoredArmourIgnoresConditions(sheet:CharacterSheet):boolean {
  return equippedFamousItems(sheet).some(item=>itemHasQuality(item,'rune-scored-armour'));
}

export function runeScoredShieldMakesAttackerWeary(sheet:CharacterSheet):boolean {
  return equippedFamousItems(sheet).some(item=>itemHasQuality(item,'rune-scored-shield'));
}

export function runeScoredHelmIgnoresCombatTaskConditions(sheet:CharacterSheet):boolean {
  return equippedFamousItems(sheet).some(item=>itemHasQuality(item,'rune-scored-helm'));
}

export function famousShieldStaticParryBonus(item:InventoryItem):number {
  return itemHasQuality(item,'superior-reinforced') ? 2 : 0;
}

export function famousShieldDynamicParryBonus(sheet:CharacterSheet,attacker:CombatActorState):number {
  const shield=equippedFamousItems(sheet).find(item=>itemHasQuality(item,'superior-reinforced') && /escudo|broquel|shield/.test(norm(item.magicalBaseItemName||item.name)));
  if(!shield || !['Elven','Numenorean'].includes(String(shield.magicalOrigin||''))) return 0;
  return actorIsBane(attacker,shield) ? Math.max(1,Number(sheet.attributes.valor)||1) : 1;
}

export function enchantedLoad(sheet:CharacterSheet,item:InventoryItem,baseLoad:number):number {
  if(magicalItemSuppressed(item)) return Math.max(0,baseLoad);
  let load=Math.max(0,baseLoad);
  const qualities=activeQualitiesForItem(item);
  if(qualities.includes('mithril-armour')){
    const base=norm(item.magicalBaseItemName||item.name);
    if(base==='camisa de malha') load=3;
    if(base==='cota de malha') load=6;
  }
  if(qualities.includes('ancient-cunning-make')) {
    load=Math.max(0,load-Math.max(3,Math.max(1,Number(sheet.attributes.valor)||1)));
  }
  return load;
}

export function luminousAutoAvoidAmbush(sheet:CharacterSheet,baneTags:Bane[]):boolean {
  const tags=new Set(baneTags.map(String));
  return equippedFamousItems(sheet).some(item=>
    itemHasQuality(item,'luminous')
    && (item.magicalBanes||[]).some(bane=>tags.has(String(bane)))
  );
}

export function ownershipSuppressesItem(item:InventoryItem):boolean {
  return magicalItemSuppressed(item);
}

export function curseDarkeningDicePenalty(sheet:CharacterSheet):number {
  return (sheet.inventory||[]).some(item=>
    item.magicalCurse==='darkening'
    && item.magicalCurseActive
    && item.magicalCurseRevealed
  ) ? -1 : 0;
}

export function curseEyeIsAutomaticFailure(sheet:CharacterSheet):boolean {
  return (sheet.inventory||[]).some(item=>item.magicalCurse==='ill-luck' && item.magicalCurseActive);
}

export function curseShadowTaintAmount(sheet:CharacterSheet):number {
  return (sheet.inventory||[]).reduce((sum,item)=>{
    if(item.magicalCurse!=='shadow-taint'||!item.magicalCurseActive) return sum;
    return sum+Math.max(0,Number(item.magicalCurseShadowApplied)||0);
  },0);
}

export function curseWeaknessFlaws(sheet:CharacterSheet):string[] {
  return (sheet.inventory||[])
    .filter(item=>item.magicalCurse==='weakness-curse'&&item.magicalCurseActive&&item.magicalCurseWeaknessFlaw)
    .map(item=>String(item.magicalCurseWeaknessFlaw));
}

export function curseHuntedBy(sheet:CharacterSheet,enemyTag:string):boolean {
  const key=norm(enemyTag);
  return (sheet.inventory||[]).some(item=>
    item.magicalCurse==='hunted'
    && item.magicalCurseActive
    && norm(item.magicalCurseHunterTag)===key
  );
}

export function applyIllLuckToResult<T extends {featResult?:number;success?:boolean;successLevel?:any}>(sheet:CharacterSheet,result:T):T {
  if(curseEyeIsAutomaticFailure(sheet)&&Number(result.featResult)===11) {
    return {...result,success:false,successLevel:'failure'};
  }
  return result;
}
