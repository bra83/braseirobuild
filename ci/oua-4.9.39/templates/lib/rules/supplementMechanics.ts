import type { CharacterSheet } from '../db';
import { supplementCultureFor, supplementPatronByName } from '../supplementRules';

export type RollMode = 'favoured'|'normal'|'ill-favoured';
export type SupplementRollContext = {
  kind?: 'skill'|'attack'|'protection'|'shadow'|'first-aid'|'journey';
  skill?: string;
  terrain?: string;
  isNight?: boolean;
  combatRound?: number;
  openingVolley?: boolean;
  stance?: string;
  weaponName?: string;
  weaponGroup?: string;
  dreadSource?: 'ordinary'|'spirit'|'ghost'|'possessed';
  dogAvailable?: boolean;
  journeyMode?: 'standard'|'strider';
};

function norm(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').trim();
}

export function hasVirtue(sheet: CharacterSheet, name: string): boolean {
  const wanted=norm(name);
  return String(sheet.other.virtues || '').split(/[;\n,]+/).some(value => norm(value)===wanted || norm(value).startsWith(wanted+' '));
}

export function isCulture(sheet: CharacterSheet, idOrName: string): boolean {
  const culture=supplementCultureFor(sheet.identity.culture);
  return Boolean(culture && (culture.id===idOrName || norm(culture.culture)===norm(idOrName)));
}

function isForest(terrain?: string): boolean {
  return /floresta|bosque|wood|forest|mirkwood/i.test(String(terrain||''));
}

function isLeatherOrNoArmor(sheet: CharacterSheet): boolean {
  const armor=norm(sheet.combat.armor);
  return !armor || armor==='nenhuma' || armor==='none' || /couro|leather/.test(armor);
}

export function supplementMaxHopeBonus(sheet: CharacterSheet): number {
  let bonus=0;
  if(hasVirtue(sheet,'Beleza das Estrelas')) bonus+=1;
  if(hasVirtue(sheet,'Música de Estancamento')) bonus+=1;
  return bonus;
}

export function supplementMaxEnduranceBonus(sheet: CharacterSheet): number {
  return hasVirtue(sheet,'Irmão para os Ursos') ? 1 : 0;
}

export function supplementFellowshipBonus(sheet: CharacterSheet): number {
  let bonus=0;
  if(hasVirtue(sheet,'Bolos de Mel Duas Vezes Assados')) bonus+=1;
  return bonus;
}

export function supplementPatronFellowshipBonus(patronName: string): number {
  return Math.max(0, Number(supplementPatronByName(patronName)?.fellowshipBonus) || 0);
}

export function supplementParryBonus(sheet: CharacterSheet, context: SupplementRollContext): number {
  if(isCulture(sheet,'woodmen-wilderland') && isForest(context.terrain)) return 2;
  return 0;
}

export function supplementBonusDice(sheet: CharacterSheet, context: SupplementRollContext): number {
  let dice=0;
  const skill=norm(context.skill);
  if(hasVirtue(sheet,'Remédios Herbais') && skill==='cura') dice+=1;
  if(hasVirtue(sheet,'Cão da Floresta das Trevas') && context.dogAvailable!==false && (skill==='vigilancia' || skill==='cacada')) dice+=1;
  if(hasVirtue(sheet,'Contra o Invisível') && context.kind==='shadow' && ['spirit','ghost','possessed'].includes(String(context.dreadSource))) dice+=1;
  if(hasVirtue(sheet,'Casaco de Pele') && context.kind==='protection' && isLeatherOrNoArmor(sheet)) dice+=1;
  // Peregrino is Inspired during Journey rolls; that changes Hope from +1d to +2d,
  // but never grants a free die by itself.
  return dice;
}

export function supplementRollMode(sheet: CharacterSheet, context: SupplementRollContext, base: RollMode='normal'): RollMode {
  if(base==='ill-favoured') return base;
  if(isCulture(sheet,'beornings') && sheet.attributes.wounded && (context.kind==='attack' || context.kind==='protection')) return 'favoured';
  if(hasVirtue(sheet,'Exploração Florestal') && context.kind==='attack' && (context.openingVolley || Number(context.combatRound||0)===1)) return 'favoured';
  if(hasVirtue(sheet,'Contra o Invisível') && context.kind==='shadow' && context.dreadSource) return 'favoured';
  return base;
}

export function supplementInspired(sheet: CharacterSheet, context: SupplementRollContext): boolean {
  if(context.journeyMode==='strider' && context.kind==='journey') return true;
  if(hasVirtue(sheet,'Tiros no Escuro') && context.kind==='attack' && (context.isNight || isForest(context.terrain))) return true;
  return false;
}

export function magicalSuccessSource(sheet: CharacterSheet, context: SupplementRollContext): string | null {
  if(context.kind!=='skill') return null;
  const skill=norm(context.skill);
  const culture=norm(sheet.identity.culture);
  if(culture==='elfo de lindon' && !sheet.attributes.miserable) return 'Perícia Élfica';
  if(isCulture(sheet,'high-elves-rivendell') && !sheet.attributes.miserable) return 'Elfo de Valfenda';
  if(isCulture(sheet,'mirkwood-elves') && (context.isNight || isForest(context.terrain))) return 'Povo Crepuscular';
  if(hasVirtue(sheet,'Vida nos Ermos') && ['exploracao','cacada','viagem'].includes(skill)) return 'Vida nos Ermos';
  if(hasVirtue(sheet,'Vigilância Natural') && ['vigilancia','exploracao','busca'].includes(skill)) return 'Vigilância Natural';
  if(hasVirtue(sheet,'O Encantamento de Beorn')) {
    const selected=sheet.other.supplementSelections?.beornEnchantedSkills || [];
    if(selected.some(value=>norm(value)===skill)) return 'O Encantamento de Beorn';
  }
  if(hasVirtue(sheet,'Pedaços de Encantamentos')) {
    const selected=(sheet.other.supplementSelections?.magicalSuccessSkills as string[] | undefined) || [];
    if(selected.some(value=>norm(value)===skill)) return 'Pedaços de Encantamentos';
  }
  return null;
}

export function canSpendHopeForSupplementMagicalSuccess(sheet: CharacterSheet, context: SupplementRollContext): boolean {
  return sheet.attributes.hopeCurrent>0 && Boolean(magicalSuccessSource(sheet,context));
}

export function gandalfIsAutomaticMagicalSuccess(sheet: CharacterSheet, featResult: number): boolean {
  const feat=Number(featResult);
  return hasVirtue(sheet,'Habilidade dos Eldar') && (feat===10 || feat===12);
}

export function journeyFatigueAfterSupplementReduction(sheet: CharacterSheet, gained: number): number {
  const value=Math.max(0, Math.trunc(Number(gained)||0));
  if(value<=0) return 0;
  return hasVirtue(sheet,'Bolos de Mel Duas Vezes Assados') ? Math.max(0,value-1) : value;
}

export function effectiveRestKind(sheet: CharacterSheet, requested: 'short'|'long'): 'short'|'long' {
  if(requested==='short' && hasVirtue(sheet,'Sonhos Élficos')) return 'long';
  return requested;
}

export function canUseSongForFirstAid(sheet: CharacterSheet): boolean {
  return hasVirtue(sheet,'Música de Estancamento');
}

export function canCoverLookoutWithAnotherJourneyRole(sheet: CharacterSheet): boolean {
  return hasVirtue(sheet,'Vigilância Natural');
}

export function hunterResolveRecovery(sheet: CharacterSheet): number {
  if(!hasVirtue(sheet,'A Resolução do Caçador')) return 0;
  if(sheet.attributes.wounded || sheet.attributes.miserable || sheet.attributes.hopeCurrent<=0) return 0;
  return Math.max(0, Math.max(Number(sheet.attributes.heart)||0, Number(sheet.attributes.valor)||0));
}

export function applyHuntersResolve(sheet: CharacterSheet): CharacterSheet {
  const recovery=hunterResolveRecovery(sheet);
  if(recovery<=0) return sheet;
  return {
    ...sheet,
    attributes:{
      ...sheet.attributes,
      hopeCurrent:Math.max(0,sheet.attributes.hopeCurrent-1),
      enduranceCurrent:Math.min(sheet.attributes.enduranceMax, sheet.attributes.enduranceCurrent+recovery),
    },
  };
}

export function supplementEnemyHateLossOnSuccessfulAttack(sheet: CharacterSheet, successIcons: number): number {
  return hasVirtue(sheet,'Luzir de Ira') ? 1+Math.max(0,Math.trunc(Number(successIcons)||0)) : 0;
}

export function supplementProtectionIllFavouredForTarget(sheet: CharacterSheet, melee: boolean, piercing: boolean): boolean {
  return Boolean(melee && piercing && hasVirtue(sheet,'Golpe Ruptor'));
}

export function supplementHeavyBlowStrengthBonus(sheet: CharacterSheet): number {
  return hasVirtue(sheet,'Grande Força') ? 2 : 0;
}

export function skirmishHeroAttackDiceModifier(stance: string, ranged: boolean, escaping=false): number {
  if(stance!=='skirmish' || !ranged) return 0;
  return escaping ? 0 : -1;
}

export function skirmishIncomingAttackDiceModifier(heroStance: string, attackerRanged: boolean): number {
  return heroStance==='skirmish' && !attackerRanged ? -1 : 0;
}

export function canUseSkirmishWeapon(stance: string, ranged: boolean): boolean {
  return stance!=='skirmish' || ranged;
}

export function gainGroundBonusDice(success: boolean, successIcons: number): number {
  return success ? 1+Math.max(0,Math.trunc(Number(successIcons)||0)) : 0;
}

export function councilTimeLimitBonus(sheet: CharacterSheet, spokesperson: boolean): number {
  return spokesperson && hasVirtue(sheet,'Beleza das Estrelas') ? 1 : 0;
}

export function canCancelDeadlyAbility(sheet: CharacterSheet): boolean {
  return hasVirtue(sheet,'Poder do Primogênito') && sheet.attributes.hopeCurrent>0;
}

export function supplementMagicalEffectEyeCost(effect: 'minor'|'major'|'powerful'): number {
  return effect==='powerful'?3:effect==='major'?2:1;
}
