import type {GeneratedDemonAttack} from './OfficialDemonGenerator';
import { retainJourneyEvents } from './JourneyEventRetention';
import {BloodmarchBetaCombatEngine} from './BloodmarchBetaCombatEngine.ts';
import {EncounterCampaignGuard} from './EncounterCampaignGuard.ts';
import {BloodmarchBetaFireWyrmWeakSpotEngine} from './BloodmarchBetaFireWyrmWeakSpotEngine.ts';
import {BloodmarchBetaFireWyrmAreaEngine, type WyrmSpaceConfirmation} from './BloodmarchBetaFireWyrmAreaEngine.ts';
import {BloodmarchBetaFireGuard} from './BloodmarchBetaFireGuard.ts';
import {BloodmarchBetaMentalGuard} from './BloodmarchBetaMentalGuard.ts';
import {BloodmarchBetaMentalSceneEngine} from './BloodmarchBetaMentalSceneEngine.ts';
import {BloodmarchBetaActiveWorldEngine} from './BloodmarchBetaActiveWorldEngine.ts';
import {BloodmarchBetaRoundEngine} from './BloodmarchBetaRoundEngine.ts';
import {BloodmarchBetaSleepEngine} from './BloodmarchBetaSleepEngine.ts';
import {BloodmarchBetaChantService} from './BloodmarchBetaChantService.ts';
import { withTransaction, type BraseiroDB } from '../db/db';
import { EncumbranceRulesEngine, type OverloadFailureChoice } from './EncumbranceRulesEngine';
import { ForbiddenLandsDiceEngine } from './ForbiddenLandsDiceEngine';
import {
  BestiaryEntry,
  Character,
  CriticalInjuryState,
  DiceRoll,
  EncounterCombatant,
  EncounterParticipantStatus,
  EncounterState,
  JourneyEvent,
  WorldOperation,
  WorldState,
} from '../types';
import { WorldStateService } from './WorldStateService';
import { CampaignPartyService } from './CampaignPartyService';
import { CriticalInjuryRulesEngine } from './CriticalInjuryRulesEngine';
import { CharacterActionRulesEngine } from './CharacterActionRulesEngine';
import { PoisonRulesEngine } from './PoisonRulesEngine';
import { FearRulesEngine, type FearDamageAttribute } from './FearRulesEngine';
import { FireRulesEngine } from './FireRulesEngine';
import { BitterReachOngoingSpellEngine } from './BitterReachOngoingSpellEngine.ts';
import { BitterReachElementalEngine } from './BitterReachElementalEngine.ts';
import { BookOfBeastsCatalogService } from './BookOfBeastsCatalogService';
import { BloodMistDemonRulesEngine } from './BloodMistDemonRulesEngine';
import { OfficialEntDamageRulesEngine } from './OfficialEntDamageRulesEngine';
import { OfficialGhostDamageRulesEngine } from './OfficialGhostDamageRulesEngine';
import { OfficialEncounterCombatRulesEngine } from './OfficialEncounterCombatRulesEngine';
import { OfficialEncounterNpcFactory } from './OfficialEncounterNpcFactory';
import { CombatRulesEngine, type AttackResolution } from './CombatRulesEngine';
import { BitterReachTalentRulesEngine } from './BitterReachTalentRulesEngine';
import { PublishedMonsterDamageRulesEngine } from './PublishedMonsterDamageRulesEngine';
import { DiseaseRulesEngine } from './DiseaseRulesEngine';
import { BasiliskWartsRulesEngine } from './BasiliskWartsRulesEngine';
import { DrowningRulesEngine } from './DrowningRulesEngine';
import { WillpowerRulesEngine } from './WillpowerRulesEngine';
import { findEquipmentCatalogEntry } from '../data/forbidden-lands-encumbrance-catalog';
import { HiddenCombinationsEngine, type CardDeclaration, type CombatantContext } from './HiddenCombinationsEngine';

export type EncounterEnemySelection = { entryId: string; quantity: number };

const initiative = () => Math.floor(Math.random() * 10) + 1;
const ATTRIBUTE_LABEL = (attribute:'strength'|'agility'|'wits'|'empathy') => attribute==='strength'?'Força':attribute==='agility'?'Agilidade':attribute==='wits'?'Astúcia':'Empatia';
const cleanName = (value: unknown) => String(value || '').trim().slice(0, 160) || 'Adversário';
const monsterIncapacitated = (item: EncounterCombatant) => BloodmarchBetaSleepEngine.asleep(item.statusEffects) || item.statusEffects?.some(effect => effect.id.startsWith('monster-incapacitated:')||effect.id.startsWith('monster-possessed:')||effect.id==='monster-petrified'||effect.id==='magic:bm-bloodshock') || false;
const normalizedSkillRank = (character: Character, names: string[]) => {
  const wanted = new Set(names.map(name => name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '')));
  return Object.entries(character.skills || {}).find(([name]) => wanted.has(name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '')))?.[1] || 0;
};

const syncCombatantFromCharacter = (combatant: EncounterCombatant, character: Character): EncounterCombatant => {
  const broken = Boolean(character.brokenAttribute || character.conditions?.includes('Broken'));
  const dead = character.status === 'dead';
  return {
    ...combatant,
    currentStrength: character.attributes.strength.current,
    status: dead ? 'dead' : broken ? 'broken' : combatant.status === 'broken' ? 'active' : combatant.status,
    brokenAttribute: character.brokenAttribute,
    conditions: [...(character.conditions || [])],
    quickSheet: {
      ...(combatant.quickSheet || {}),
      attributes: character.attributes,
      conditions: character.conditions,
      brokenAttribute: character.brokenAttribute,
    },
  };
};

const applyCriticalRollConsequences = (character: Character, roll: Pick<DiceRoll, 'skillName'>) =>
  CriticalInjuryRulesEngine.applyCheckConsequences(character, roll.skillName);

const criticalRollConsequenceText = (triggers: ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>) =>
  triggers.length
    ? `; sequela de ferimento grave causou ${triggers.map(trigger => `${trigger.amount} dano em ${trigger.attribute}`).join(' e ')}`
    : '';

const normalizeRuleKey = (value: unknown) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase().replace(/[^a-z0-9]/g, '');
const combatantAttribute = (combatant: EncounterCombatant, character: Character | undefined, key: 'strength'|'agility'|'wits'|'empathy') => {
  if (character) return Number(character.attributes[key]?.current || 0);
  const attributes = (combatant.quickSheet?.attributes || {}) as Record<string, { current?: number } | number>;
  const value = attributes[key];
  return Number(typeof value === 'object' && value ? value.current || 0 : value || (key === 'strength' ? combatant.currentStrength : 0));
};
const combatantSkill = (combatant: EncounterCombatant, character: Character | undefined, names: string[]) => {
  if (character) return Number(normalizedSkillRank(character, names)) || 0;
  const wanted = new Set(names.map(normalizeRuleKey));
  const skills = (combatant.quickSheet?.skills || {}) as Record<string, number>;
  return Number(Object.entries(skills).find(([name]) => wanted.has(normalizeRuleKey(name)))?.[1] || 0);
};
const combatantWeapons = (combatant: EncounterCombatant, character?: Character) =>
  (character?.weapons || ((combatant.quickSheet?.weapons || []) as Record<string, unknown>[])) as Record<string, unknown>[];
const isShield = (item: Record<string, unknown> | undefined) => /escudo|shield/i.test(String(item?.name || item?.type || ''));
const quickDrawRank = (talents: Record<string, number> | undefined) => Number(Object.entries(talents || {}).find(([name]) => normalizeRuleKey(name) === 'saquerapido')?.[1] || 0);
const itemEncumbranceClass = (item: Record<string, unknown>) => EncumbranceRulesEngine.normalizeClass(item.encumbranceClass || findEquipmentCatalogEntry(String(item.name || ''))?.encumbranceClass);
const combatantTalentRank = (combatant: EncounterCombatant, character: Character | undefined, label: string) => {
  const talents = character?.talents || ((combatant.quickSheet?.talents || {}) as Record<string, number>);
  const wanted = normalizeRuleKey(label);
  return Math.max(0, Math.min(3, Math.trunc(Number(Object.entries(talents || {}).find(([name]) => normalizeRuleKey(name) === wanted)?.[1] || 0))));
};
const fastActionSlots = (combatant: Pick<EncounterCombatant, 'fastActionAvailable'|'slowActionAvailable'>) => Number(Boolean(combatant.fastActionAvailable)) + Number(Boolean(combatant.slowActionAvailable));
const canSpendFastAction = (combatant: Pick<EncounterCombatant, 'fastActionAvailable'|'slowActionAvailable'>) => fastActionSlots(combatant) > 0;
const spendFastAction = <T extends Pick<EncounterCombatant, 'fastActionAvailable'|'slowActionAvailable'> & Partial<Pick<EncounterCombatant,'statusEffects'>>>(combatant: T): T => {
  const cleared = { ...combatant, ...(combatant.statusEffects ? { statusEffects: combatant.statusEffects.filter(effect=>!effect.id.startsWith('core-ranged-prepared:')) } : {}) } as T;
  if (combatant.fastActionAvailable) return { ...cleared, fastActionAvailable: false };
  if (combatant.slowActionAvailable) return { ...cleared, slowActionAvailable: false };
  throw new Error('Não há ação disponível que possa ser convertida em ação rápida.');
};
const turnEligible = (combatant: Pick<EncounterCombatant, 'status'|'statusEffects'>) =>
  (combatant.status === 'active' || combatant.status === 'broken') && !BloodmarchBetaSleepEngine.asleep(combatant.statusEffects);
const defenderBlockPlan = (combatant: EncounterCombatant, character: Character | undefined, round: number) => {
  const rank = combatantTalentRank(combatant, character, 'Defensor');
  const freeId = `core-defender-free-block:${round}`;
  const actionId = `core-defender-action-block:${round}`;
  const effects = combatant.statusEffects || [];
  const freeUsed = effects.some(effect => effect.id === freeId);
  const actionUsed = effects.some(effect => effect.id === actionId);
  if (rank >= 1 && !freeUsed) return { rank, allowed: true, consumeFast: false, marker: { id: freeId, name: 'Bloqueio gratuito (Defensor)', detail: `Defensor ${rank}: o bloqueio gratuito desta rodada foi usado.` } };
  if (rank >= 3 && actionUsed) return { rank, allowed: true, consumeFast: false, marker: null };
  if (!canSpendFastAction(combatant)) return { rank, allowed: false, consumeFast: false, marker: null };
  return { rank, allowed: true, consumeFast: true, marker: rank >= 3 ? { id: actionId, name: 'Bloqueios de Defensor', detail: 'Defensor 3: os bloqueios adicionais desta rodada não custam mais ações.' } : null };
};
const defenderDodgePlan = (combatant: EncounterCombatant, character: Character | undefined, round: number) => {
  const rank = combatantTalentRank(combatant, character, 'Pés Ligeiros');
  const freeId = `core-footwork-free-dodge:${round}`;
  const actionId = `core-footwork-action-dodge:${round}`;
  const effects = combatant.statusEffects || [];
  const freeUsed = effects.some(effect => effect.id === freeId);
  const actionUsed = effects.some(effect => effect.id === actionId);
  if (rank >= 1 && !freeUsed) {
    return { rank, allowed: true, consumeFast: false, marker: { id: freeId, name: 'Esquiva gratuita (Pés Ligeiros)', detail: `Pés Ligeiros ${rank}: a esquiva gratuita desta rodada foi usada.` } };
  }
  if (rank >= 3 && actionUsed) {
    return { rank, allowed: true, consumeFast: false, marker: null };
  }
  if (!canSpendFastAction(combatant)) {
    if (rank >= 3) {
      return { rank, allowed: true, consumeFast: false, marker: { id: actionId, name: 'Esquivas de Pés Ligeiros', detail: 'Pés Ligeiros 3: esquiva adicional permitida sem ações restantes.' } };
    }
    return { rank, allowed: false, consumeFast: false, marker: null };
  }
  return { rank, allowed: true, consumeFast: true, marker: rank >= 3 ? { id: actionId, name: 'Esquivas de Pés Ligeiros', detail: 'Pés Ligeiros 3: as esquivas adicionais desta rodada não custam mais ações.' } : null };
};
const applyReactiveDefenseCost = (combatant: EncounterCombatant, character: Character | undefined, round: number, defense: 'none'|'dodge'|'block') => {
  if (defense === 'none') return { combatant, modifier: 0 };
  if (defense === 'dodge') {
    const plan = defenderDodgePlan(combatant, character, round);
    if (!plan.allowed) throw new Error(`${combatant.name} não possui uma ação disponível para Esquivar.`);
    let next = plan.consumeFast ? spendFastAction(combatant) : combatant;
    if (plan.marker) next = { ...next, statusEffects: [...(next.statusEffects || []).filter(effect => effect.id !== plan.marker!.id), plan.marker] };
    if (!plan.consumeFast) {
      next = { ...next, freeDodgeAvailable: false };
    }
    return { combatant: next, modifier: plan.rank >= 2 ? 1 : 0 };
  }
  const plan = defenderBlockPlan(combatant, character, round);
  if (!plan.allowed) throw new Error(`${combatant.name} não possui uma ação disponível para Bloquear.`);
  let next = plan.consumeFast ? spendFastAction(combatant) : combatant;
  if (plan.marker) next = { ...next, statusEffects: [...(next.statusEffects || []).filter(effect => effect.id !== plan.marker!.id), plan.marker] };
  return { combatant: next, modifier: plan.rank >= 2 ? 1 : 0 };
};

export class EncounterService {
  /** PREPARAR ARMA: arco/funda usam ação rápida; Atirador Veloz 1+ dispensa a preparação. */
  static async prepareRangedWeapon(campaignId:string,combatantId:string,weaponIndex:number):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];if(!actor||actor.id!==combatantId||actor.kind!=='character'||!actor.entityId||actor.status!=='active')throw new Error('Apenas a personagem ativa pode Preparar Arma.');
      if(!canSpendFastAction(actor))throw new Error('Não há ação disponível que possa ser usada como rápida para Preparar Arma.');
      const character=await tx.objectStore('characters').get(actor.entityId);if(!character)throw new Error('Ficha da personagem não encontrada.');CharacterActionRulesEngine.assertCanAct(character);
      const weapon=character.weapons?.[weaponIndex];const name=String(weapon?.name||'');
      if(!weapon||/besta|crossbow/i.test(name)||!/arco|bow|funda|sling/i.test(name))throw new Error('Preparar Arma neste fluxo se aplica a arco ou funda; bestas usam Carregar.');
      const rank=combatantTalentRank(actor,character,'Atirador Veloz');if(rank>=1)throw new Error('Atirador Veloz 1+ dispensa Preparar Arma para arco/funda.');
      const now=Date.now();const marker={id:`core-ranged-prepared:${weaponIndex}:${encounter.round}`,name:'Arma à distância preparada',detail:`${name} preparada; até Atirar, só Mirar pode ser usado sem perder a preparação.`};
      const combatants=encounter.combatants.map(item=>item.id===actor.id?(()=>{const spent=spendFastAction(item);return{...spent,statusEffects:[...(spent.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-ranged-prepared:')),marker]};})():item);
      const next={...current,encounter:{...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text:`${actor.name} preparou ${name} para Atirar.`,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(world);return world;
  }

  /** Carregar besta é ação lenta e o estado carregado acompanha a ficha persistida. */
  static async loadCrossbow(campaignId: string, combatantId: string, weaponIndex: number): Promise<WorldState> {
    const world = await withTransaction(['world_state', 'characters'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const actor = encounter.combatants[encounter.turnIndex];
      if (!actor || actor.id !== combatantId || actor.kind !== 'character' || !actor.entityId) throw new Error('Apenas a personagem do turno atual pode carregar sua besta.');
      if (actor.status !== 'active') throw new Error('A personagem precisa estar ativa para carregar a besta.');
      if (actor.statusEffects?.some(effect => effect.id === 'condition:prone')) throw new Error(`${actor.name} está Caída e precisa se levantar antes de carregar a besta.`);
      if (monsterIncapacitated(actor) || actor.statusEffects?.some(effect => effect.id.startsWith('monster-lead-arms:'))) throw new Error(`${actor.name} está incapacitada e não pode carregar a besta.`);
      if (!actor.slowActionAvailable) throw new Error(`${actor.name} já gastou a ação lenta desta rodada.`);
      const characters = tx.objectStore('characters');
      const character = await characters.get(actor.entityId);
      const weapon = character?.weapons?.[weaponIndex];
      if (!character || !weapon || !/besta|crossbow/i.test(String(weapon.name || ''))) throw new Error('Selecione uma besta equipada.');
      CharacterActionRulesEngine.assertCanAct(character);
      if (weapon.loaded === true) throw new Error('Esta besta já está carregada.');
      const weapons = character.weapons.map((item: any, index: number) => index === weaponIndex ? { ...item, loaded: true } : item);
      const updated = { ...character, weapons, updatedAt: Date.now() };
      await characters.put(updated);
      const combatants = encounter.combatants.map(item => item.id === actor.id ? { ...item, slowActionAvailable: false, quickSheet: { ...(item.quickSheet || {}), weapons } } : item);
      const now = Date.now();
      const nextEncounter = { ...encounter, combatants, log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId: actor.id, text: `${actor.name} carregou ${weapon.name} com uma ação lenta.`, createdAt: now }].slice(-300), updatedAt: now };
      const next = { ...current, encounter: nextEncounter, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  /** Dispara e descarrega a besta usada, sem alterar outros itens da ficha. */
  static async consumeCrossbow(campaignId: string, combatantId: string, weaponIndex: number): Promise<WorldState> {
    const world = await withTransaction(['world_state', 'characters'], 'readwrite', async tx => {
      const current = WorldStateService.normalize(await tx.objectStore('world_state').get(campaignId), campaignId);
      const actor = current.encounter?.combatants.find(item => item.id === combatantId);
      if (!actor?.entityId) throw new Error('Personagem do ataque não encontrada.');
      const characters = tx.objectStore('characters');
      const character = await characters.get(actor.entityId);
      const weapon = character?.weapons?.[weaponIndex];
      if (!character || !weapon || !/besta|crossbow/i.test(String(weapon.name || ''))) throw new Error('A besta equipada mudou durante a resolução do ataque.');
      if (weapon.loaded !== true) throw new Error('A besta precisa ser carregada com uma ação lenta antes do disparo.');
      const weapons = character.weapons.map((item: any, index: number) => index === weaponIndex ? { ...item, loaded: false } : item);
      await characters.put({ ...character, weapons, updatedAt: Date.now() });
      const encounter = current.encounter ? { ...current.encounter, combatants: current.encounter.combatants.map(item => item.id === actor.id ? { ...item, quickSheet: { ...(item.quickSheet || {}), weapons } } : item), updatedAt: Date.now() } : null;
      const next = { ...current, ...(encounter ? { encounter } : {}), updatedAt: Date.now() };
      await tx.objectStore('world_state').put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  /** Bloodmarch beta p.111: a aura ocorre no TURNO da serpe, sem ação,
   * defesa ou armadura. O tabuleiro não possui uma escala garantida: o Mestre
   * deve classificar TODOS os demais seres presentes (alcance/fora) antes de
   * confirmar a resolução. Só há uma aplicação por rodada do encontro.
   */
  static async resolveFireWyrmAura(campaignId:string, wyrmCombatantId:string,
    reach:Record<string,'arms-reach'|'outside'>, rng:()=>number=Math.random):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const worldStore=tx.objectStore('world_state');
      const original=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(original.id!==campaignId||original.campaignId!==campaignId)throw Error('Aura: campanha inválida.');
      const encounter=original.encounter;
      if(!encounter||encounter.status!=='active')throw Error('Aura: não há encontro ativo.');
      const wyrm=encounter.combatants[encounter.turnIndex];
      if(!wyrm||wyrm.id!==wyrmCombatantId||wyrm.status!=='active'||
        wyrm.entityId!=='bloodmarch:fire-wyrm'||Number(wyrm.quickSheet?.fireAuraDice)!==3)
        throw Error('Aura: somente a Serpe de Fogo ativa, no próprio turno, emite calor.');
      const source=(original.magicFireWyrmEffects||[]).find(effect=>effect.id===wyrm.quickSheet?.fireWyrmEffectId&&
        effect.status==='manifested'&&effect.campaignId===campaignId&&effect.monsterCombatantId===wyrm.id&&
        effect.encounterId===encounter.id&&effect.hexId===encounter.hexId);
      if(!source)throw Error('Aura: vínculo do monstro com a campanha/encontro não validado.');
      const marker=`bloodmarch:fire-wyrm-aura:${encounter.round}`;
      if(wyrm.statusEffects?.some(effect=>effect.id===marker))throw Error('Aura: esta rodada já foi resolvida.');
      const possible=encounter.combatants.filter(item=>item.id!==wyrm.id&&
        (item.status==='active'||item.status==='broken'));
      const keys=Object.keys(reach||{});
      if(keys.length!==possible.length||keys.some(id=>!possible.some(item=>item.id===id))||
        possible.some(item=>!['arms-reach','outside'].includes(reach[item.id])))
        throw Error('Aura: confirme alcance ou distância de TODOS os seres vivos, inclusive conjuradora e adversários.');
      const characterStore=tx.objectStore('characters');
      const rollStore=tx.objectStore('rolls');
      const combatants=[...encounter.combatants];
      const logs:EncounterState['log']=[];
      const now=Date.now();
      for(const item of possible.filter(item=>reach[item.id]==='arms-reach')){
        const roll=ForbiddenLandsDiceEngine.roll({base:3,skill:0,gear:0},{
          characterId:null,sceneId:encounter.startedSceneId||encounter.id,
          checkId:`fire-wyrm-aura:${encounter.id}:${encounter.round}:${wyrm.id}:${item.id}`,rng});
        await rollStore.put(roll);
        const character=item.kind==='character'&&item.entityId?await characterStore.get(item.entityId):undefined;
        if(item.kind==='character'&&(!character||character.campaignId!==campaignId||character.status==='dead'))
          throw Error('Aura: personagem não pertence à campanha ou ficha ausente.');
        const immune=character?BloodmarchBetaFireGuard.immune(character.statusEffects):BloodmarchBetaFireGuard.immune(item.statusEffects);
        // 3 dados de base + dano de arma 1; sem sucessos não há acerto.
        // Cada sucesso além do primeiro soma +1 ao dano.
        const raw=roll.successes>0?roll.successes:0;
        const inflicted=immune?0:raw;
        const remaining=Math.max(0,(character?.attributes.strength.current??item.currentStrength)-inflicted);
        const broke=(character?.attributes.strength.current??item.currentStrength)>0&&remaining===0;
        let conditions=item.conditions||[];
        let brokenAttribute=item.brokenAttribute;
        let sheet=character;
        if(character){
          const strength={...character.attributes.strength,current:remaining};
          conditions=remaining===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
          brokenAttribute=remaining===0?'strength':character.brokenAttribute;
          const pending=broke?{id:`critical-pending:fire-wyrm-aura:${encounter.id}:${encounter.round}:${item.id}`,
            label:'Ferimento grave pendente',detail:'Aura da Serpe Quebrou Força; aplicar dano atípico conforme regras.',criticalTable:'other' as const}:undefined;
          sheet={...character,attributes:{...character.attributes,strength},conditions,brokenAttribute,
            statusEffects:[...(character.statusEffects||[]),...(pending?[pending]:[])],
            ...(broke?{criticalInjuries:[...(character.criticalInjuries||[]),'PENDENTE: ferimento grave — aura de Serpe de Fogo']}: {})};
          await characterStore.put(sheet);
        }
        const index=combatants.findIndex(entry=>entry.id===item.id);
        const critical=broke?{id:`critical-pending:fire-wyrm-aura:${encounter.id}:${encounter.round}:${item.id}`,
          name:'Ferimento grave pendente',detail:'Aura térmica: resolver dano atípico.',criticalTable:'other' as const}:undefined;
        combatants[index]={...item,currentStrength:remaining,conditions,brokenAttribute,
          status:remaining===0?'broken' as const:item.status,
          quickSheet:{...(item.quickSheet||{}),...(sheet?{attributes:sheet.attributes}:{}),conditions,brokenAttribute},
          statusEffects:[...(item.statusEffects||[]),...(critical?[critical]:[])]};
        logs.push({id:crypto.randomUUID(),round:encounter.round,combatantId:item.id,
          text:`Aura da Serpe em ${item.name}: 3 dados, ${roll.successes} sucesso(s), ${inflicted} dano de Força sem armadura${immune?' (Amigo do Fogo: imune)':''}${broke?'; Quebrada, ferimento grave pendente':''}.`,createdAt:now});
      }
      combatants[encounter.turnIndex]={...wyrm,statusEffects:[...(wyrm.statusEffects||[]),{
        id:marker,name:'Aura térmica resolvida',detail:`Rodada ${encounter.round}; alcance confirmado, sem esquiva, bloqueio ou armadura.`}]};
      const next={...original,encounter:{...encounter,combatants,
        log:[...encounter.log,...logs,{id:crypto.randomUUID(),round:encounter.round,combatantId:wyrm.id,
          text:`Aura térmica resolvida: ${possible.filter(item=>reach[item.id]==='arms-reach').length} criatura(s) ao alcance das mãos.`,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};
      await worldStore.put(next);return next;
    });
    this.notify(world);
    return world;
  }

  /** R88 #20: rola primeiro o D6 dos Ents; os alvos são escolhidos depois que
   * o resultado revela quantidade e alcance do ataque.
   */
  static async prepareOfficialProcessionEntAttack(campaignId:string,attackerId:string,rng:()=>number=Math.random){
    const outcome=await withTransaction(['world_state'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=world.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const attacker=encounter.combatants[encounter.turnIndex];
      if(!attacker||attacker.id!==attackerId||!attacker.entityId?.startsWith('official-procession-ent:')||
        attacker.quickSheet?.officialProcessionEnt!==true)
        throw new Error('Apenas um Ent oficial da procissão pode rolar esta tabela.');
      if(attacker.status!=='active'||!attacker.slowActionAvailable)
        throw new Error('O Ent não possui ação lenta disponível neste turno.');
      if(attacker.quickSheet?.pendingOfficialProcessionEntAttack)
        throw new Error('O resultado do D6 já aguarda seleção de alvos.');
      if(monsterIncapacitated(attacker))throw new Error(`${attacker.name} está incapacitado e perde as ações até o próximo turno.`);
      const table=attacker.quickSheet?.monsterAttackTable as Array<{roll:number;name:string}>|undefined;
      if(!table||table.length!==6)throw new Error('A tabela oficial D6 do Ent está ausente.');
      const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte de D6 inválida.');
      const dieValue=1+Math.floor(value*6);const attack=table.find(row=>row.roll===dieValue);
      if(!attack)throw new Error('Resultado D6 sem ataque correspondente.');
      const now=Date.now();
      const combatants=encounter.combatants.map(item=>item.id!==attacker.id?item:{...item,slowActionAvailable:false,
        quickSheet:{...(item.quickSheet||{}),pendingOfficialProcessionEntAttack:{round:encounter.round,dieValue,attackName:attack.name}}});
      const log={id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,
        text:`${attacker.name}: rolou a tabela oficial D6 e obteve ${dieValue} — ${attack.name}. Selecione agora o(s) alvo(s) dentro do alcance indicado.`,createdAt:now};
      const updated={...world,encounter:{...encounter,combatants,log:[...encounter.log,log].slice(-300),updatedAt:now},updatedAt:now};
      await store.put(updated);return{world:updated,attack,dieValue};
    });this.notify(outcome.world);return outcome;
  }

  /** R88: rola primeiro a tabela D6 de um monstro oficial; seleção de alvos vem depois. */
  static async prepareOfficialMonsterAttack(campaignId:string,attackerId:string,rng:()=>number=Math.random){
    const outcome=await withTransaction(['world_state'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=world.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const attacker=encounter.combatants[encounter.turnIndex];
      if(!attacker||attacker.id!==attackerId||attacker.kind!=='monster'||attacker.quickSheet?.officialBookMonster!==true)
        throw new Error('Apenas um monstro oficial com tabela R88 pode preparar este ataque.');
      if(attacker.status!=='active'||!attacker.slowActionAvailable)throw new Error('O monstro não possui ação lenta disponível neste turno.');
      if(attacker.quickSheet?.pendingOfficialMonsterAttack||attacker.quickSheet?.pendingOfficialProcessionEntAttack)
        throw new Error('O resultado D6 já aguarda seleção de alvos.');
      if(monsterIncapacitated(attacker))throw new Error(`${attacker.name} está incapacitado e perde as ações até o próximo turno.`);
      const table=attacker.quickSheet?.monsterAttackTable as Array<{roll:number;name:string}>|undefined;
      if(!table||table.length!==6)throw new Error('A tabela oficial D6 do monstro está ausente.');
      const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte de D6 inválida.');
      const dieValue=1+Math.floor(value*6);const attack=table.find(row=>row.roll===dieValue);
      if(!attack)throw new Error('Resultado D6 sem ataque correspondente.');
      const now=Date.now();
      const combatants=encounter.combatants.map(item=>item.id!==attacker.id?item:{...item,slowActionAvailable:false,
        quickSheet:{...(item.quickSheet||{}),pendingOfficialMonsterAttack:{round:encounter.round,dieValue,attackName:attack.name}}});
      const log={id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,
        text:`${attacker.name}: rolou a tabela oficial D6 e obteve ${dieValue} — ${attack.name}. Selecione agora o(s) alvo(s) dentro do alcance confirmado.`,createdAt:now};
      const updated={...world,encounter:{...encounter,combatants,log:[...encounter.log,log].slice(-300),updatedAt:now},updatedAt:now};
      await store.put(updated);return{world:updated,attack,dieValue};
    });this.notify(outcome.world);return outcome;
  }

  /** R88 #20: registra um resgate externo ou uma libertação adjudicada pela Mestra. */
  static async recordOfficialEntExternalRescue(campaignId:string,victimCombatantId:string):Promise<WorldState>{
    const next=await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=world.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('O resgate exige o combate oficial ativo.');
      const victim=encounter.combatants.find(item=>item.id===victimCombatantId&&item.status==='active');
      const entEffects=(victim?.statusEffects||[]).filter(effect=>effect.id.startsWith('official-ent-roots:')||effect.id.startsWith('official-ent-embrace:'));
      if(!victim||!entEffects.length)throw new Error('A personagem não está presa por raízes ou abraço de um Ent.');
      const now=Date.now();const removeIds=new Set(entEffects.map(effect=>effect.id));
      const combatants=encounter.combatants.map(item=>item.id!==victim.id?item:{...item,
        statusEffects:(item.statusEffects||[]).filter(effect=>!removeIds.has(effect.id))});
      if(victim.kind==='character'&&victim.entityId){
        const chars=tx.objectStore('characters');const character=await chars.get(victim.entityId) as Character|undefined;
        if(!character||character.campaignId!==campaignId)throw new Error('A vítima do Ent não pertence a esta campanha.');
        await chars.put({...character,statusEffects:(character.statusEffects||[]).filter(effect=>!removeIds.has(effect.id))});
      }
      const updated={...world,encounter:{...encounter,combatants,
        log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:victim.id,
          text:`${victim.name}: a Mestra registrou libertação externa de um efeito do Ent; nenhum teste ou custo de ação foi presumido.`,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  static async resolveBookOfBeastsAttack(campaignId:string,attackerId:string,targetIds:string|string[],rng:()=>number=Math.random,fearDamageAttribute:FearDamageAttribute='wits',wyrmSpace?:WyrmSpaceConfirmation,officialDieValue?:number){
    const resolved=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const attacker=encounter.combatants[encounter.turnIndex];
      if(!attacker||attacker.id!==attackerId||attacker.kind!=='monster')throw new Error('Apenas o monstro do turno atual pode usar a tabela de ataques.');
      const pendingEntAttack=attacker.quickSheet?.pendingOfficialProcessionEntAttack as {round?:number;dieValue?:number;attackName?:string}|undefined;
      const pendingMonsterAttack=attacker.quickSheet?.pendingOfficialMonsterAttack as {round?:number;dieValue?:number;attackName?:string}|undefined;
      const pendingOfficialAttack=pendingMonsterAttack||pendingEntAttack;
      if(!attacker.slowActionAvailable&&!pendingOfficialAttack)throw new Error('O monstro já gastou sua ação lenta nesta rodada.');
      if(pendingEntAttack&&(attacker.quickSheet?.officialProcessionEnt!==true||pendingEntAttack.round!==encounter.round||
        !Number.isInteger(pendingEntAttack.dieValue)||officialDieValue!==pendingEntAttack.dieValue))
        throw new Error('O ataque do Ent não corresponde ao resultado D6 pendente nesta rodada.');
      if(pendingMonsterAttack&&(attacker.quickSheet?.officialBookMonster!==true||pendingMonsterAttack.round!==encounter.round||
        !Number.isInteger(pendingMonsterAttack.dieValue)||officialDieValue!==pendingMonsterAttack.dieValue))
        throw new Error('O ataque do monstro não corresponde ao resultado D6 pendente nesta rodada.');
      if(!pendingOfficialAttack&&officialDieValue!==undefined)throw new Error('Não há resultado oficial D6 pendente para resolver.');
      if(attacker.statusEffects?.some(effect=>effect.id==='condition:prone'))throw new Error(`${attacker.name} está Caído e precisa gastar uma ação rápida para se levantar antes de atacar.`);
      if(monsterIncapacitated(attacker))throw new Error(`${attacker.name} está incapacitado e perde as ações até o próximo turno.`);
      const table=attacker.quickSheet?.monsterAttackTable as Array<{roll:number;name:string;baseDice:number;damage:number;damageType:string;tags:string[];poisonKind?:import('../types').PoisonKind;poisonPotency?:number;poisonTrigger?:string;poisonSecondaryEffect?:string;contaminatesFood?:boolean;saveAttribute?:'strength'|'agility'|'wits'|'empathy';saveSkill?:string;failureEffect?:'incapacitated'|'prone'|'attack_on_failure'|'wits_attack'|'damage_on_failure'|'disease'|'plague_attack'|'bog_mist'|'blinded'|'webbed'|'petrified'|'drowning'|'life_drain'|'impregnated'|'consumed'|'dragged'|'magic_leech'|'flatulence'|'lead_arms'|'donkey_head'|'tongue_twisting'|'otherworldly_hunger'|'otherworldly_thirst'|'possessed'|'slimed'|'hypnotic_gaze'|'undead_mass';failureDamage?:number;healOnFailure?:number;diseaseVirulence?:number;saveModifier?:number;highestMaxStrength?:boolean;halveDamageOnResilienceSuccess?:boolean;saveOnHit?:boolean;releaseDamage?:number;suffocationRecovery?:'endurance';regenerateStrengthOnDamage?:boolean}>|undefined;
      if(!table||table.length!==6)throw new Error('Esta criatura não possui tabela D6 própria; use combate de NPC comum.');
      const dieValue=pendingOfficialAttack?Number(pendingOfficialAttack.dieValue):1+Math.floor(Math.max(0,Math.min(.999999999,rng()))*6);
      const attack=table.find(item=>item.roll===dieValue);
      if(!attack)throw new Error('Resultado D6 sem ataque correspondente.');
      const tags=attack.tags||[];
      const fearOnly=tags.includes('fear')&&attack.damage===0;
      const effectTags=tags.filter(tag=>!['area','two_targets','highest_strength','gm_selects','swarm'].includes(tag)&&!(tag==='fear'&&fearOnly));
      const boundFireWyrmCaster=String(attacker.quickSheet?.fireWyrmCasterId||'');
      if(boundFireWyrmCaster&&!(current.magicFireWyrmEffects||[]).some(e=>e.id===attacker.quickSheet?.fireWyrmEffectId&&e.status==='manifested'&&e.campaignId===campaignId))
        throw Error('Serpe: vínculo de invocação ausente ou de outra campanha.');
      if(attacker.entityId==='bloodmarch:fire-wyrm'&&boundFireWyrmCaster&&
         !attacker.statusEffects?.some(effect=>effect.id===`bloodmarch:fire-wyrm-aura:${encounter.round}`))
        throw Error('Serpe: resolva a aura antes de executar o ataque do bestiário.');
      const activeTargets=encounter.combatants.filter(item=>item.status==='active'&&
        (boundFireWyrmCaster?item.id!==attacker.id&&!(item.kind==='character'&&item.entityId===boundFireWyrmCaster):item.side===(attacker.side==='party'?'enemy':'party')));
      // Bloodmarch pp.110–111: D6 3/4/6 are area hazards, NOT zero-dice
      // attacks that merely consume a slow action. Resolve inside this same
      // transaction, with every potential target explicitly classified.
      if(boundFireWyrmCaster && ['bloodmarch:kton','bloodmarch:fire-wyrm'].includes(attacker.entityId)
          && [3,4,6].includes(dieValue)){
        const participants=encounter.combatants.filter(item=>item.id!==attacker.id&&
          item.status==='active'&&!(item.kind==='character'&&item.entityId===boundFireWyrmCaster));
        const confirmed=BloodmarchBetaFireWyrmAreaEngine.validate(participants.map(p=>p.id),wyrmSpace,dieValue);
        const relevant=participants.filter(item=>BloodmarchBetaFireWyrmAreaEngine.isInArea(confirmed.zones[item.id],dieValue));
        const now=Date.now(),characterStore=tx.objectStore('characters'),rollStore=tx.objectStore('rolls');
        const logs:EncounterState['log']=[];
        const combatants=[...encounter.combatants];
        const sceneId=encounter.startedSceneId||encounter.id;
        // One roll for the entire collapse; same sinkhole height for all creatures.
        const height=dieValue===6?BloodmarchBetaFireWyrmAreaEngine.height(rng):0;
        for(const target of relevant){
          const idx=combatants.findIndex(c=>c.id===target.id);
          const character=target.kind==='character'&&target.entityId?await characterStore.get(target.entityId):undefined;
          if(target.kind==='character'&&(!character||character.campaignId!==campaignId||character.status==='dead'))
            throw Error('Serpe: ficha de alvo inválida ou de outra campanha.');
          let success=false,amount=0,armorLoss=0;
          let resultDetails='';
          if(dieValue===3||dieValue===4){
            const save=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(target,character,'agility'),
              skill:combatantSkill(target,character,['Move','Movimentação','Movement']),gear:0},
              {characterId:character?.id||null,sceneId,checkId:`wyrm-move:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}:${dieValue}`,
                rng,criticalInjuries:character?.criticalInjuryStates,skillName:'move',attributeName:'agility'});
            await rollStore.put(save);
            success=save.successes>0;
            resultDetails=`Movimentação: ${save.successes} sucesso(s)`;
          }
          if(dieValue===3&&!success){
            const baseDice=attacker.entityId==='bloodmarch:fire-wyrm'?12:10;
            const blow=ForbiddenLandsDiceEngine.roll({base:baseDice,skill:0,gear:0},
              {characterId:null,sceneId,checkId:`wyrm-crush:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
            await rollStore.put(blow);
            // Bestiary specifies dice and blunt force, not a different
            // weapon-damage number: use the ordinary default 1.
            const hit=CombatRulesEngine.resolveAttack({attackSuccesses:blow.successes,weaponDamage:1,armorValue:target.armor,rng});
            amount=hit.finalDamage;armorLoss=hit.armorLoss;
            resultDetails+=`; esmagamento ${baseDice}D: ${blow.successes} sucesso(s), ${amount} dano após armadura`;
          }
          const swallowed=dieValue===4&&!success;
          if(dieValue===6){
            const baseDice=confirmed.hardSurface?Math.max(0,height-2):0;
            if(baseDice){
              const fall=ForbiddenLandsDiceEngine.roll({base:baseDice,skill:0,gear:0},
                {characterId:null,sceneId,checkId:`wyrm-fall:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
              await rollStore.put(fall);amount=fall.successes;
            }
            resultDetails=`queda ${height}m, superfície ${confirmed.hardSurface?'dura':'não dura'}, ${amount} dano de Força`;
          }
          const before=character?.attributes.strength.current??target.currentStrength;
          const after=Math.max(0,before-amount);
          const broke=before>0&&after===0;
          const conditions=broke?Array.from(new Set([...(character?.conditions||target.conditions||[]),'Broken' as const])):(character?.conditions||target.conditions||[]);
          const mouthId=`monster-grappled:fire-wyrm-mouth:${attacker.id}`;
          const mouth=swallowed?{id:mouthId,name:'Na boca da Serpe',
            detail:`Sofre ${attacker.entityId==='bloodmarch:fire-wyrm'?3:1} de Força por rodada; 4+ dano num ataque único contra a Serpe liberta a vítima.`}:undefined;
          const pending=broke?{id:`critical-pending:wyrm-area:${encounter.id}:${encounter.round}:${target.id}`,name:'Ferimento grave pendente',
            detail:dieValue===3?'Esmagamento: resolver trauma concussivo.':'Queda: resolver trauma concussivo.',criticalTable:'blunt' as const}:undefined;
          const effects=[...(target.statusEffects||[]),...(mouth?[mouth]:[]),...(pending?[pending]:[])];
          if(character){
            const sheet={...character,attributes:{...character.attributes,strength:{...character.attributes.strength,current:after}},
              conditions,brokenAttribute:broke?'strength' as const:character.brokenAttribute,
              statusEffects:[...(character.statusEffects||[]),...(mouth?[{...mouth,label:mouth.name}]:[])],
              ...(broke?{criticalInjuries:[...(character.criticalInjuries||[]),`PENDENTE: ferimento grave — ${attack.name}`]}:{})};
            await characterStore.put(sheet);
            combatants[idx]={...target,currentStrength:after,armor:Math.max(0,target.armor-armorLoss),
              status:after===0?'broken' as const:target.status,brokenAttribute:broke?'strength' as const:target.brokenAttribute,
              conditions,statusEffects:effects,quickSheet:{...(target.quickSheet||{}),attributes:sheet.attributes}};
          }else combatants[idx]={...target,currentStrength:after,armor:Math.max(0,target.armor-armorLoss),
            status:after===0?'broken' as const:target.status,brokenAttribute:broke?'strength' as const:target.brokenAttribute,
            conditions,statusEffects:effects};
          logs.push({id:crypto.randomUUID(),round:encounter.round,combatantId:target.id,
            text:`${attack.name}: ${target.name}: ${resultDetails}${swallowed?'; engolido(a), dano a cada rodada':''}${broke?'; Quebrado(a)':''}.`,createdAt:now});
        }
        combatants[encounter.turnIndex]={...attacker,slowActionAvailable:false};
        logs.push({id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,
          text:`${attacker.name}: D6 ${dieValue} — ${attack.name}; ${relevant.length} alvo(s) na área confirmada${dieValue===6?`; poço de ${height}m`:''}.`,createdAt:now});
        const updated=this.resolveIfFinished({...encounter,combatants,
          log:[...encounter.log,...logs].slice(-300),updatedAt:now},now);
        const world={...current,encounter:updated,updatedAt:now};
        await store.put(world);
        return {world,attack,dieValue,outcomes:[],specialPending:false,summonedUndead:[]};
      }
      // In Bloodmarch rolls 2 (fear) and 5 (tail sweep) strike EVERYONE
      // confirmed inside Near, not merely checkboxes the user happened to tick.
      const areaWyrmIds=boundFireWyrmCaster&&[2,5].includes(dieValue)
        ?Object.entries(BloodmarchBetaFireWyrmAreaEngine.validate(activeTargets.map(item=>item.id),wyrmSpace,dieValue).zones)
          .filter(([,zone])=>zone==='arms-reach'||zone==='near').map(([id])=>id)
        :null;
      const requested=areaWyrmIds??Array.from(new Set((Array.isArray(targetIds)?targetIds:[targetIds]).filter(Boolean)));
      const boundMaraVictim=String(attacker.quickSheet?.maraVictimId||'');
      if(boundMaraVictim&&!(current.magicMaraEffects||[]).some(e=>e.id===attacker.quickSheet?.maraEffectId&&e.status==='hunting'))
        throw Error('Invocação de Mara expirou ou não pertence ao mundo ativo.');
      if(boundMaraVictim&&(requested.length!==1||!encounter.combatants.some(c=>c.id===requested[0]&&c.kind==='character'&&c.entityId===boundMaraVictim&&c.status==='active')))
        throw Error('Mara só pode atacar a vítima a que foi vinculada, enquanto estiver ativa.');
      let targets:EncounterCombatant[]=[];
      if(attack.failureEffect==='undead_mass'){
        targets=[];
      }else if(attack.failureEffect==='magic_leech'){
        const score=(item:EncounterCombatant)=>{const skills=(item.quickSheet?.skills||{}) as Record<string,number>;const skillMagic=Object.entries(skills).filter(([name])=>['magic','spellcasting','sorcery','sorcerer'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim())).reduce((sum,[,value])=>sum+(Number(value)||0),0);const talents=(item.quickSheet?.talents||{}) as Record<string,number>;const talentMagic=Math.max(0,...Object.entries(talents).filter(([name])=>/caminho|magia|magic/i.test(name)).map(([,rank])=>Number(rank)||0));const attributes=(item.quickSheet?.attributes||{}) as Record<string,{current?:number}>;const magical=Math.max(skillMagic,talentMagic);return magical>0?1000+magical:Number(attributes.wits?.current)||0;};
        targets=[...activeTargets].sort((a,b)=>score(b)-score(a)||a.name.localeCompare(b.name,'pt-BR')).slice(0,1);
      }else if(attack.failureEffect==='lead_arms'){
        targets=[...activeTargets].filter(item=>item.kind==='character'&&Boolean(item.entityId)).sort((a,b)=>b.currentStrength-a.currentStrength||a.name.localeCompare(b.name,'pt-BR')).slice(0,2);
      }else if(attack.failureEffect==='donkey_head'){
        const weaponLoad=(item:EncounterCombatant)=>Math.max(0,...(((item.quickSheet?.weapons||[]) as Array<Record<string,unknown>>).map(weapon=>Number(weapon.weight)||Number(weapon.damage)||0)));
        targets=[...activeTargets].filter(item=>item.kind==='character'&&Boolean(item.entityId)).sort((a,b)=>weaponLoad(b)-weaponLoad(a)||a.name.localeCompare(b.name,'pt-BR')).slice(0,1);
      }else if(attacker.entityId==='bitter-wendigo'&&dieValue===3){
        // The Bitter Reach p.109: a Mordida Voraz vai ao alvo menos protegido;
        // escolher manualmente outro personagem não pode burlar a regra.
        const leastArmored=[...activeTargets].sort((a,b)=>a.armor-b.armor||a.name.localeCompare(b.name,'pt-BR'))[0];
        if(leastArmored)targets=[leastArmored];
      }else if(attacker.entityId===BloodMistDemonRulesEngine.PROFILE_ID&&dieValue===5){
        // A jugular atinge a aventureira de menor Força dentre as elegíveis.
        const weakest=[...activeTargets].sort((a,b)=>a.currentStrength-b.currentStrength||a.id.localeCompare(b.id))[0];
        if(weakest)targets=[weakest];
      }else if(tags.includes('highest_strength')){
        const strongest=[...activeTargets].sort((a,b)=>(attack.highestMaxStrength?b.maxStrength-a.maxStrength:b.currentStrength-a.currentStrength)||a.name.localeCompare(b.name,'pt-BR'))[0];
        if(strongest)targets=[strongest];
      }else if(attack.baseDice>0){
        targets=tags.includes('area')?requested.map(id=>activeTargets.find(item=>item.id===id)).filter((item):item is EncounterCombatant=>Boolean(item)):requested.map(id=>activeTargets.find(item=>item.id===id)).filter((item):item is EncounterCombatant=>Boolean(item));
        if(requested.length!==targets.length)throw new Error('Um alvo escolhido não está mais ativo no encontro.');
        if(tags.includes('two_targets')){
          const expected=Math.min(2,activeTargets.length);
          if(targets.length!==expected)throw new Error(`Este ataque exige ${expected} alvo(s) ativo(s).`);
        }else if(!tags.includes('area')&&targets.length!==1){
          targets=targets.slice(0,1);
        }else if(tags.includes('area')&&!targets.length){
          throw new Error('Escolha ao menos um alvo dentro do alcance da área.');
        }
      }else if(tags.includes('area')){
        targets=requested.map(id=>activeTargets.find(item=>item.id===id)).filter((item):item is EncounterCombatant=>Boolean(item));
        if(requested.length!==targets.length)throw new Error('Um alvo escolhido para a área não está mais ativo no encontro.');
        if(!targets.length)throw new Error('Escolha os alvos dentro do alcance da área.');
      }else if(attack.baseDice===0){
        targets=activeTargets.filter(item=>requested.includes(item.id)).slice(0,1);
        if(!targets.length)throw new Error('Escolha um alvo ativo para este efeito especial.');
      }
      // Tables may override requested targets (area/highest-strength/special attacks).
      // Mara has a single bound sleeping victim: recheck the RESOLVED targets,
      // not merely the requested ids, before any roll, damage or status write.
      if(boundFireWyrmCaster&&targets.some(t=>t.id===attacker.id||t.kind==='character'&&t.entityId===boundFireWyrmCaster))
        throw Error('Serpe: invocador imune à seleção e aos alvos automáticos do monstro.');
      if(boundMaraVictim&&(targets.length!==1||targets[0].kind!=='character'||targets[0].entityId!==boundMaraVictim))
        throw Error('O ataque da Mara não pode atingir pessoa diferente da vítima vinculada.');
      if(tags.includes('two_targets')&&targets.length>2)throw new Error('Este ataque pode atingir no máximo dois alvos.');
      if(attack.baseDice>0&&!targets.length)throw new Error('Nenhum alvo ativo elegível para este ataque.');
      const poisonSupported=Boolean(attack.poisonKind&&attack.poisonPotency&&(!attack.poisonSecondaryEffect||(attack.poisonSecondaryEffect==='empathy_warts'&&attack.poisonTrigger==='move_failure'))&&['on_damage','on_hit','move_failure','automatic','wits_failure','endurance_failure','ongoing_grapple'].includes(attack.poisonTrigger||'')&&targets.length>0&&targets.every(item=>item.kind==='character'&&Boolean(item.entityId)));
      const saveEffectSupported=Boolean(attack.failureEffect&&attack.saveAttribute&&attack.saveSkill&&targets.length>0&&targets.every(item=>item.kind==='character'&&Boolean(item.entityId)));
      const proneSupported=tags.includes('prone')&&targets.length>0;
      const publishedNanuikFurious=attacker.entityId==='bitter-nanuik'&&dieValue===6;
      const publishedIceGiantMorsel=attacker.entityId==='bitter-ice-giant'&&dieValue===3;
      const publishedStoneDisarm=attacker.entityId==='bitter-stone-guardian'&&dieValue===3;
      const pendingEffectTags=tags.filter(tag=>!['area','two_targets','highest_strength','gm_selects','grapple','ongoing','swarm','damage_agility','damage_empathy','extra_attack','double_attack','killing_cold_broken','automatic_prone','prone_on_miss','ent_roots','ent_embrace'].includes(tag)&&!(tag==='followup_attack'&&(publishedNanuikFurious||publishedIceGiantMorsel||(attacker.entityId==='bitter-nanuik'&&dieValue===2)))&&!(publishedStoneDisarm&&['disarm','opposed_might'].includes(tag))&&!(tag==='damage_wits'&&attack.name==='Decapitation')&&!(tag==='bloodmist_dual_wits'&&attacker.entityId===BloodMistDemonRulesEngine.PROFILE_ID)&&!(tag==='drowning'&&(attack.baseDice>0&&!fearOnly||saveEffectSupported||attack.name==='Smothering Fog'))&&!(tag==='fear'&&targets.every(item=>item.kind==='character'&&Boolean(item.entityId)))&&!(tag==='cold'&&targets.every(item=>item.kind==='character'&&Boolean(item.entityId)))&&!(tag==='poison'&&poisonSupported)&&!(tag==='disease'&&Number(attack.diseaseVirulence)>0)&&!(tag==='prone'&&(proneSupported||saveEffectSupported))&&!(tag==='resistance'&&saveEffectSupported)&&!(tag==='incapacitate'&&saveEffectSupported));
      const now=Date.now();
      const outcomes=[] as Array<{target:EncounterCombatant;roll:DiceRoll;finalDamage:number;armorDice:number[];armorLoss:number;criticalTable?:'slash'|'puncture'|'blunt'|'other';fearCharacter?:Character;fearDamage?:number;fearAttribute?:FearDamageAttribute;fearPending?:boolean;fearSummary?:string;fearSuccesses?:number;eggIncubationRounds?:number;coldApplied?:boolean;witsDamage?:number;plagueSuccesses?:number;hit?:boolean;consumedApplied?:boolean;proneApplied?:boolean;grappleApplied?:boolean;drowningApplied?:boolean;entRootsApplied?:boolean;entEmbraceApplied?:boolean;moveFailed?:boolean;saveFailed?:boolean;movementSaveFailed?:boolean;automaticPoison?:boolean;contaminatesFood?:boolean;tongueTwisted?:boolean;specialEffect?:'incapacitated'|'prone'|'avoided'|'mind_disintegration'|'attack_on_failure'|'wits_attack'|'damage_on_failure'|'disease'|'plague_attack'|'bog_mist'|'blinded'|'webbed'|'petrified'|'drowning'|'life_drain'|'impregnated'|'consumed'|'dragged'|'magic_leech'|'flatulence'|'lead_arms'|'donkey_head'|'otherworldly_hunger'|'otherworldly_thirst'|'possessed'|'slimed'|'hypnotic_gaze';poisonKind?:import('../types').PoisonKind;poisonPotency?:number;poisonTrigger?:string;poisonSummary?:string;furiousFollowup?:boolean;furiousMightWin?:boolean;disarmAttempt?:boolean}>;
      const bogMistRounds=attack.failureEffect==='bog_mist'?1+Math.floor(Math.max(0,Math.min(.999999999,rng()))*3):undefined;
      let droppedEquipment=encounter.droppedEquipment||[];
      const tongueTwistedTargets=new Set<string>();
      for(const target of targets){
        if(attack.failureEffect==='wits_attack'&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha do alvo da desintegração mental está ausente ou morta.');
          const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`gatekeeper-wits-defense:${encounter.id}:${encounter.round}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,attributeName:'wits'});
          const gatekeeperWits=Number(((attacker.quickSheet?.attributes||{}) as Record<string,{current?:number}>).wits?.current)||3;
          const gatekeeperRoll=ForbiddenLandsDiceEngine.roll({base:gatekeeperWits,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`gatekeeper-wits-contest:${encounter.id}:${encounter.round}:${target.id}`,rng});
          await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(gatekeeperRoll);
          if(victimRoll.successes>gatekeeperRoll.successes){outcomes.push({target,roll:victimRoll,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:false,specialEffect:'avoided'});continue;}
          const psychicAttack=ForbiddenLandsDiceEngine.roll({base:attack.baseDice,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`gatekeeper-psychic-attack:${encounter.id}:${encounter.round}:${target.id}`,rng});
          await tx.objectStore('rolls').put(psychicAttack);
          outcomes.push({target,roll:psychicAttack,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:true,specialEffect:'mind_disintegration',witsDamage:psychicAttack.successes});
          continue;
        }
        if(attack.failureEffect==='tongue_twisting'&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo de Língua Embaralhada está ausente ou morta.');
          const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();const insight=Object.entries(character.skills||{}).find(([name])=>['insight','discernment','discernimento'].includes(normalizeSkill(name)))?.[1]||0;
          const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-tongue-twisting-victim:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits'});
          const attributes=(attacker.quickSheet?.attributes||{}) as Record<string,{current?:number}>;const impRoll=ForbiddenLandsDiceEngine.roll({base:Number(attributes.wits?.current)||6,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-tongue-twisting-wits:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(impRoll);
          if(victimRoll.successes>impRoll.successes){outcomes.push({target,roll:victimRoll,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:false,specialEffect:'avoided'});continue;}
          tongueTwistedTargets.add(target.id);
        }
        if(attack.failureEffect==='magic_leech'&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo do roubo mágico está ausente ou morta.');
          const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();const insight=Object.entries(character.skills||{}).find(([name])=>['insight','discernment','discernimento'].includes(normalizeSkill(name)))?.[1]||0;
          const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-leech-insight:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits'});
          const attributes=(attacker.quickSheet?.attributes||{}) as Record<string,{current?:number}>;const impRoll=ForbiddenLandsDiceEngine.roll({base:Number(attributes.wits?.current)||3,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-leech-wits:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(impRoll);const caught=impRoll.successes>=victimRoll.successes;
          outcomes.push({target,roll:victimRoll,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:caught,specialEffect:caught?'magic_leech':'avoided'});continue;
        }
        if(attack.failureEffect==='lead_arms'&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo de Braços Pesados está ausente ou morta.');
          const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();const insight=Object.entries(character.skills||{}).find(([name])=>['insight','discernment','discernimento'].includes(normalizeSkill(name)))?.[1]||0;
          const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-lead-arms-victim:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits'});
          const attributes=(attacker.quickSheet?.attributes||{}) as Record<string,{current?:number}>;const impRoll=ForbiddenLandsDiceEngine.roll({base:Number(attributes.wits?.current)||6,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-lead-arms-wits:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(impRoll);const caught=impRoll.successes>=victimRoll.successes;
          outcomes.push({target,roll:victimRoll,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:caught,specialEffect:caught?'lead_arms':'avoided'});continue;
        }
        if(attack.failureEffect==='donkey_head'&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo da Cabeça de Burro está ausente ou morta.');
          const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();const insight=Object.entries(character.skills||{}).find(([name])=>['insight','discernment','discernimento'].includes(normalizeSkill(name)))?.[1]||0;
          const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-donkey-head-victim:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits'});
          const attributes=(attacker.quickSheet?.attributes||{}) as Record<string,{current?:number}>;const impRoll=ForbiddenLandsDiceEngine.roll({base:Number(attributes.wits?.current)||6,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`imp-donkey-head-wits:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(impRoll);const caught=impRoll.successes>=victimRoll.successes;
          outcomes.push({target,roll:victimRoll,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:caught,specialEffect:caught?'donkey_head':'avoided'});continue;
        }
        if(attack.failureEffect==='dragged'&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo do arrasto está ausente ou morta.');
          const might=Object.entries(character.skills||{}).find(([name])=>['might','potencia','potência'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
          const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(might)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`spider-drag-victim:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'might',attributeName:'strength'});
          const spiderRoll=ForbiddenLandsDiceEngine.roll({base:attacker.currentStrength,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`spider-drag-strength:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(spiderRoll);
          const victimConsequence=applyCriticalRollConsequences(character,victimRoll);
          if(victimConsequence.triggers.length)await tx.objectStore('characters').put(victimConsequence.character);
          const dragged=spiderRoll.successes>victimRoll.successes;
          outcomes.push({target,roll:victimRoll,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:dragged,specialEffect:dragged?'dragged':'avoided'});continue;
        }
        if(saveEffectSupported&&attack.failureEffect&&attack.failureEffect!=='tongue_twisting'&&!attack.saveOnHit&&attack.saveAttribute&&attack.saveSkill&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha do alvo do teste de resistência está ausente ou morta.');
          const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
          const aliases=attack.saveSkill==='movement'?['move','movement','movimentacao']:attack.saveSkill==='endurance'?['endurance','resilience','resiliencia']:attack.saveSkill==='insight'?['insight','discernimento']:[normalizeSkill(attack.saveSkill)];
          const skill=Object.entries(character.skills||{}).find(([name])=>aliases.includes(normalizeSkill(name)))?.[1]||0;
          const save=ForbiddenLandsDiceEngine.roll({base:character.attributes[attack.saveAttribute].current,skill:Number(skill)||0,gear:0,modifier:Number(attack.saveModifier)||0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-save:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}:${attack.roll}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:attack.saveSkill,attributeName:attack.saveAttribute});
          await tx.objectStore('rolls').put(save);
          const saveConsequence=applyCriticalRollConsequences(character,save);
          if(saveConsequence.triggers.length)await tx.objectStore('characters').put(saveConsequence.character);
          if(attack.failureEffect==='hypnotic_gaze'){
            const empathy=Number(((attacker.quickSheet?.attributes||{}) as Record<string,{current?:number}>).empathy?.current)||4;
            const vampyrRoll=ForbiddenLandsDiceEngine.roll({base:empathy,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`vampyr-hypnotic-gaze:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
            await tx.objectStore('rolls').put(vampyrRoll);
            outcomes.push({target,roll:save,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:save.successes<=vampyrRoll.successes,specialEffect:save.successes>vampyrRoll.successes?'avoided':'hypnotic_gaze'});
            continue;
          }
          if(attack.failureEffect==='bog_mist'){
            outcomes.push({target,roll:save,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:save.successes===0,specialEffect:'bog_mist'});
            continue;
          }
          if(attack.failureEffect==='plague_attack'){
            const disease=ForbiddenLandsDiceEngine.roll({base:Number(attack.diseaseVirulence)||7,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-plague-virulence:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
            await tx.objectStore('rolls').put(disease);
            outcomes.push({target,roll:save,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:save.successes<=disease.successes,plagueSuccesses:disease.successes,specialEffect:'plague_attack'});
            continue;
          }
          if(attack.failureEffect==='attack_on_failure'&&save.successes===0){
            const attackRoll=ForbiddenLandsDiceEngine.roll({base:attack.baseDice,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-after-save:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}:${attack.roll}`,rng});
            await tx.objectStore('rolls').put(attackRoll);
            const resolution=CombatRulesEngine.resolveAttack({attackSuccesses:attackRoll.successes,weaponDamage:attack.damage,armorValue:target.armor,rng});
            let finalDamage=resolution.finalDamage;
            if(attack.halveDamageOnResilienceSuccess&&resolution.hit){
              const resilience=Object.entries(character.skills||{}).find(([name])=>aliases.includes(normalizeSkill(name)))?.[1]||0;
              const mitigation=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(resilience)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-heart-mitigation:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'endurance',attributeName:'strength'});
              await tx.objectStore('rolls').put(mitigation);
              if(mitigation.successes>0)finalDamage=Math.floor(finalDamage/2);
            }
            let fearSuccesses:number|undefined;
            if(tags.includes('fear')&&target.kind==='character'){
              const fearRoll=ForbiddenLandsDiceEngine.roll({base:attack.baseDice,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-heart-fear:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
              await tx.objectStore('rolls').put(fearRoll);
              fearSuccesses=fearRoll.successes;
            }
            const critical=CombatRulesEngine.criticalRequired({previousStrength:target.currentStrength,finalDamage,damageAttribute:'strength'});
            outcomes.push({target,roll:attackRoll,finalDamage,armorDice:resolution.armorDice,armorLoss:finalDamage>0?resolution.armorLoss:0,hit:resolution.hit,saveFailed:true,movementSaveFailed:true,fearSuccesses,grappleApplied:tags.includes('grapple')&&resolution.hit,drowningApplied:tags.includes('drowning')&&resolution.hit,...(critical?{criticalTable:(attack.damageType==='slash'?'slash':attack.damageType==='puncture'?'puncture':attack.damageType==='blunt'?'blunt':'other') as 'slash'|'puncture'|'blunt'|'other'}:{})});
          }else{
            outcomes.push({target,roll:save,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:save.successes===0,
              // The Bitter Reach p.109: vento gelado provoca Frio mesmo se a
              // vítima passar no teste de Potência e permanecer de pé.
              coldApplied:attacker.entityId==='bitter-wendigo'&&dieValue===1,
              specialEffect:attack.failureEffect==='attack_on_failure'||attack.failureEffect==='undead_mass'?'avoided':attack.failureEffect});
          }
          continue;
        }
        if(attack.poisonTrigger==='automatic'&&attack.poisonKind&&attack.poisonPotency&&target.kind==='character'&&target.entityId){
          const potencyRoll=ForbiddenLandsDiceEngine.roll({base:attack.poisonPotency,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-poison-potency:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          await tx.objectStore('rolls').put(potencyRoll);
          outcomes.push({target,roll:potencyRoll,finalDamage:0,armorDice:[],armorLoss:0,hit:true,automaticPoison:true,contaminatesFood:attack.contaminatesFood===true,coldApplied:tags.includes('cold'),poisonKind:attack.poisonKind,poisonPotency:attack.poisonPotency,poisonTrigger:attack.poisonTrigger});
          continue;
        }
        if(attack.poisonTrigger==='wits_failure'&&attack.poisonKind&&attack.poisonPotency&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha do alvo do teste de Astúcia está ausente ou morta.');
          const save=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-wits-save:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,attributeName:'wits'});
          await tx.objectStore('rolls').put(save);
          outcomes.push({target,roll:save,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:save.successes===0,poisonKind:attack.poisonKind,poisonPotency:attack.poisonPotency,poisonTrigger:attack.poisonTrigger});
          continue;
        }
        if(attack.poisonTrigger==='move_failure'&&attack.poisonKind&&attack.poisonPotency&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha do alvo do teste de Movimentação está ausente ou morta.');
          const movement=Object.entries(character.skills||{}).find(([name])=>['move','movement','movimentacao'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
          const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.agility.current,skill:Number(movement)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-move-check:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'movement',attributeName:'agility'});
          await tx.objectStore('rolls').put(roll);
          const moveConsequence=applyCriticalRollConsequences(character,roll);
          if(moveConsequence.triggers.length)await tx.objectStore('characters').put(moveConsequence.character);
          outcomes.push({target,roll,finalDamage:0,armorDice:[],armorLoss:0,moveFailed:roll.successes===0,poisonKind:attack.poisonKind,poisonPotency:attack.poisonPotency,poisonTrigger:attack.poisonTrigger});
          continue;
        }
        const blindPenalty=attacker.statusEffects?.some(effect=>effect.id.startsWith('monster-blinded:'))?-2:0;
        // Nanuik p.101, D6=2: a segunda mordida atinge OUTRO alvo,
        // com 6 Dados Base e Dano 1, não dois ataques idênticos.
        const secondNanuikBite=attacker.entityId==='bitter-nanuik'&&dieValue===2&&targets.indexOf(target)===1;
        const targetBaseDice=secondNanuikBite?6:attack.baseDice;
        const targetWeaponDamage=secondNanuikBite?1:attack.damage;
        const roll=ForbiddenLandsDiceEngine.roll({base:targetBaseDice,skill:0,gear:0,modifier:blindPenalty},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster:${encounter.id}:${encounter.round}:${attacker.id}:${dieValue}:${target.id}`,rng});
        await tx.objectStore('rolls').put(roll);
        if(fearOnly&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha do alvo do ataque de medo está ausente ou morta.');
          const fearlessRank=FearRulesEngine.fearlessRank(character);
          let defenseRoll:DiceRoll|undefined;
          if(fearlessRank>=1&&fearlessRank<3&&!BloodmarchBetaMentalGuard.power(character.statusEffects)){
            const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
            const insight=Object.entries(character.skills||{}).find(([name])=>['insight','discernimento'].includes(normalizeSkill(name)))?.[1]||0;
            defenseRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`fear-defense:${encounter.id}:${encounter.round}:${target.id}:${attacker.id}`,criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits',rng});
            await tx.objectStore('rolls').put(defenseRoll);
          }
          const fear=FearRulesEngine.resolve(character,roll.successes,{defenseSuccesses:defenseRoll?.successes||0,damageAttribute:fearDamageAttribute});
          let fearCharacter=fear.character;
          const coldApplied=tags.includes('cold')&&fear.damage>0;
          if(coldApplied)fearCharacter={...fearCharacter,conditions:Array.from(new Set([...fearCharacter.conditions,'Cold' as const]))};
          if(tongueTwistedTargets.has(target.id)){const id=`monster-tongue-twisting:${attacker.id}:${encounter.round}`;fearCharacter={...fearCharacter,statusEffects:[...(fearCharacter.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-tongue-twisting:')),{id,label:'Língua Embaralhada',detail:'Não pode falar por um Quarto de Dia.',remainingQuarters:1,speechBlocked:true}]};}
          if(fear.horrorPending){const marker='PENDENTE: ferimento grave — tabela de horror';if(!(fearCharacter.criticalInjuries||[]).includes(marker))fearCharacter={...fearCharacter,criticalInjuries:[...(fearCharacter.criticalInjuries||[]),marker]};}
          await tx.objectStore('characters').put(fearCharacter);
          outcomes.push({target,roll,finalDamage:0,armorDice:[],armorLoss:0,fearCharacter,fearDamage:fear.damage,fearAttribute:fear.attribute,fearPending:fear.horrorPending,coldApplied,proneApplied:tags.includes('prone')&&fear.damage>0,tongueTwisted:tongueTwistedTargets.has(target.id)});
          if(tags.includes('drowning')){
            const resilience=Object.entries(character.skills||{}).find(([name])=>['endurance','resilience','resiliencia'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
            const save=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(resilience)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-smothering-fog:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'endurance',attributeName:'strength'});
            await tx.objectStore('rolls').put(save);
            outcomes.push({target,roll:save,finalDamage:0,armorDice:[],armorLoss:0,saveFailed:save.successes===0,specialEffect:'drowning'});
          }
          continue;
        }
        const effectiveSuccesses=attack.baseDice===0&&attack.damage>0?1:roll.successes;
        const rawResolution=CombatRulesEngine.resolveAttack({attackSuccesses:effectiveSuccesses,weaponDamage:targetWeaponDamage,armorValue:target.armor,rng});
        let resolution=tags.includes('swarm')?{...rawResolution,finalDamage:Math.min(1,rawResolution.finalDamage)}:rawResolution;
        let twoDamagingHits=false;
        // Two complete armor/attack resolutions; no duplicate slow-action cost.
        if(tags.includes('double_attack')&&['bitter-nanuik','bitter-stone-guardian','bitter-wendigo'].includes(attacker.entityId||'')){
          const extraRoll=ForbiddenLandsDiceEngine.roll({base:targetBaseDice,skill:0,gear:0,modifier:blindPenalty},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-double-strike:${encounter.id}:${encounter.round}:${attacker.id}:${dieValue}:${target.id}`,rng});
          await tx.objectStore('rolls').put(extraRoll);
          const second=CombatRulesEngine.resolveAttack({attackSuccesses:extraRoll.successes,weaponDamage:targetWeaponDamage,armorValue:Math.max(0,target.armor-resolution.armorLoss),rng});
          twoDamagingHits=resolution.finalDamage>0&&second.finalDamage>0;
          resolution=PublishedMonsterDamageRulesEngine.combineIndependent(resolution,second);
        }
        // The Bitter Reach p.95, Lobo Atroz: um ataque separado (5 Dados Base,
        // dano de arma 2) se o primeiro salto acertar. Armadura rola novamente.
        // Ambos os danos pertencem à mesma ação lenta, mesmo que o alvo fique Quebrado.
        if(attacker.entityId==='bitter-dire-wolf'&&dieValue===4&&tags.includes('extra_attack')&&rawResolution.hit){
          const extraRoll=ForbiddenLandsDiceEngine.roll({base:5,skill:0,gear:0,modifier:blindPenalty},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-wolf-second-strike:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          await tx.objectStore('rolls').put(extraRoll);
          const second=CombatRulesEngine.resolveAttack({attackSuccesses:extraRoll.successes,weaponDamage:2,armorValue:Math.max(0,target.armor-resolution.armorLoss),rng});
          resolution=PublishedMonsterDamageRulesEngine.combineAfterHit(resolution,second);
        }
        // The Bitter Reach p. 99 — a mordida inicial do Gigante de Gelo
        // apenas AGARRA: não causa dano. O acerto dispara um ataque distinto
        // de 12 Dados Base, dano 3; segue em cada rodada até escapar/Quebrar.
        if(publishedIceGiantMorsel){
          const initialHit=rawResolution.hit;
          resolution={...resolution,finalDamage:0,rawDamage:0,armorLoss:0,armorDice:[]};
          if(initialHit){
            const bite=ForbiddenLandsDiceEngine.roll({base:12,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-giant-first-bite:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
            await tx.objectStore('rolls').put(bite);
            const biteDamage=CombatRulesEngine.resolveAttack({attackSuccesses:bite.successes,weaponDamage:3,armorValue:target.armor,rng});
            resolution=PublishedMonsterDamageRulesEngine.combineAfterHit(resolution,biteDamage);
          }
        }
        // The Bitter Reach p. 101 — o Ataque Furioso do Nanuik exige dano
        // no primeiro golpe, uma disputa de Potência, depois ataque 12D/Dano 3.
        // A segunda mordida NÃO é garantida e não gasta outra ação lenta.
        let furiousMightWin=false;
        let furiousFollowup=false;
        let furiousSecondBroke=false;
        if(publishedNanuikFurious&&resolution.finalDamage>0&&target.kind==='character'&&target.entityId){
          const victim=await tx.objectStore('characters').get(target.entityId);
          if(!victim||victim.status==='dead')throw new Error('A ficha do alvo do Ataque Furioso do Nanuik não foi encontrada.');
          const monsterMight=ForbiddenLandsDiceEngine.roll({base:Math.max(0,attacker.currentStrength),skill:combatantSkill(attacker,undefined,['Might','Potência','Potencia']),gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-nanuik-might:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
          const victimMight=ForbiddenLandsDiceEngine.roll({base:Math.max(0,victim.attributes.strength.current),skill:normalizedSkillRank(victim,['Might','Potência','Potencia']),gear:0},{characterId:victim.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-nanuik-victim-might:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:victim.criticalInjuryStates,skillName:'might',attributeName:'strength'});
          await tx.objectStore('rolls').put(monsterMight);
          await tx.objectStore('rolls').put(victimMight);
          furiousMightWin=monsterMight.successes>victimMight.successes;
          if(furiousMightWin){
            const followupRoll=ForbiddenLandsDiceEngine.roll({base:12,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-nanuik-ragdoll:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
            await tx.objectStore('rolls').put(followupRoll);
            const followup=CombatRulesEngine.resolveAttack({attackSuccesses:followupRoll.successes,weaponDamage:3,armorValue:Math.max(0,target.armor-resolution.armorLoss),rng});
            // A tabela grave deve seguir o golpe QUE QUEBRA, não meramente
            // o último golpe que tenha produzido dano.
            furiousSecondBroke=resolution.finalDamage<target.currentStrength&&resolution.finalDamage+followup.finalDamage>=target.currentStrength;
            resolution=PublishedMonsterDamageRulesEngine.combineAfterHit(resolution,followup);
            furiousFollowup=true;
          }
        }
        let poisonSaveFailed=false;
        if(attack.poisonTrigger==='endurance_failure'&&attack.poisonKind&&attack.poisonPotency&&resolution.finalDamage>0&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha do alvo do teste de Resiliência está ausente ou morta.');
          const resilience=Object.entries(character.skills||{}).find(([name])=>['endurance','resilience','resiliencia'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
          const save=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(resilience)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-endurance-save:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'endurance',attributeName:'strength'});
          await tx.objectStore('rolls').put(save);
          poisonSaveFailed=save.successes===0;
        }
        let eggIncubationRounds:number|undefined;
        if(attack.saveOnHit&&attack.failureEffect==='impregnated'&&resolution.hit&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha da vítima do ataque de impregnação está ausente ou morta.');
          const endurance=Object.entries(character.skills||{}).find(([name])=>['endurance','resilience','resiliencia'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
          const save=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(endurance)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`monster-impregnation:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'endurance',attributeName:'strength'});
          await tx.objectStore('rolls').put(save);
          if(save.successes===0)eggIncubationRounds=1+Math.floor(Math.max(0,Math.min(.999999999,rng()))*10);
        }
        const critical=PublishedMonsterDamageRulesEngine.attribute(tags)==='strength'&&CombatRulesEngine.criticalRequired({previousStrength:target.currentStrength,finalDamage:resolution.finalDamage,damageAttribute:'strength'});
        outcomes.push({target,roll,finalDamage:resolution.finalDamage,armorDice:resolution.armorDice,armorLoss:resolution.armorLoss,hit:resolution.hit,furiousMightWin,furiousFollowup,disarmAttempt:publishedStoneDisarm&&resolution.hit,coldApplied:tags.includes('cold')&&resolution.finalDamage>0,consumedApplied:attack.failureEffect==='consumed'&&resolution.hit,proneApplied:(proneSupported||tags.includes('prone_on_miss'))&&((attacker.entityId==='bitter-wendigo'&&dieValue===4)?twoDamagingHits:tags.includes('automatic_prone')||(attacker.entityId==='bitter-nanuik'&&dieValue===3?resolution.finalDamage>0:resolution.hit)),grappleApplied:tags.includes('grapple')&&resolution.hit,drowningApplied:tags.includes('drowning')&&resolution.hit,entRootsApplied:attacker.quickSheet?.officialProcessionEnt===true&&tags.includes('ent_roots')&&resolution.hit,entEmbraceApplied:attacker.quickSheet?.officialProcessionEnt===true&&tags.includes('ent_embrace')&&resolution.hit,eggIncubationRounds,saveFailed:poisonSaveFailed,poisonKind:attack.poisonKind,poisonPotency:attack.poisonPotency,poisonTrigger:attack.poisonTrigger,...(critical?{criticalTable:(furiousSecondBroke?'blunt':attack.damageType==='slash'?'slash':attack.damageType==='puncture'?'puncture':attack.damageType==='blunt'?'blunt':'other') as 'slash'|'puncture'|'blunt'|'other'}:{})});
      }
      let combatants=encounter.combatants.map(item=>{
        if(item.id!==attacker.id)return item;
        const quickSheet={...(item.quickSheet||{})};delete quickSheet.pendingOfficialProcessionEntAttack;delete quickSheet.pendingOfficialMonsterAttack;
        return{...item,slowActionAvailable:false,quickSheet};
      });
      const resolvedNoDicePoison=poisonSupported&&['automatic','wits_failure','move_failure','ongoing_grapple'].includes(attack.poisonTrigger||'');
      let specialPending=(attack.failureEffect!=='undead_mass'&&attack.baseDice===0&&attack.damage===0&&!resolvedNoDicePoison&&!saveEffectSupported)||pendingEffectTags.length>0;
      let summonedUndead:EncounterCombatant[]=[];
      let specialEffectSummary='';
      if(attack.failureEffect==='undead_mass'){
        const summonScene=encounter.startedSceneId||encounter.id;
        const countRoll=ForbiddenLandsDiceEngine.roll({base:0,skill:0,gear:0,artifact:10},{characterId:null,sceneId:summonScene,checkId:`undead-mass-count:${encounter.id}:${encounter.round}:${attacker.id}`,rng});
        const typeRoll=ForbiddenLandsDiceEngine.roll({base:0,skill:0,gear:0,artifact:10},{characterId:null,sceneId:summonScene,checkId:`undead-mass-type:${encounter.id}:${encounter.round}:${attacker.id}`,rng});
        await tx.objectStore('rolls').put(countRoll);
        await tx.objectStore('rolls').put(typeRoll);
        const count=countRoll.results[0].value;
        const typeValue=typeRoll.results[0].value;
        const profile=typeValue<=4
          ?{name:'Morto Inquieto',strength:3,melee:2,scouting:2,weapon:'Mordida',weaponDamage:2,armor:0,traits:[] as string[]}
          :typeValue<=7
            ?{name:'Esqueleto',strength:3,melee:3,scouting:3,weapon:'Espada larga enferrujada',weaponDamage:2,armor:0,traits:['Flechas e armas Pontiagudas causam no máximo 1 dano.']}
            :{name:'Carniçal',strength:4,melee:3,scouting:3,weapon:'Mordida',weaponDamage:2,armor:0,traits:['Mordida: Virulência 4.']};
        const initiativeValue=Math.max(10,...encounter.combatants.map(item=>item.initiative));
        summonedUndead=Array.from({length:count},(_,index)=>({
          id:`undead:${attacker.id}:${encounter.round}:${index+1}:${crypto.randomUUID()}`,
          name:`${profile.name} convocado ${index+1}`,
          kind:'npc' as const,
          side:'enemy' as const,
          initiative:initiativeValue,
          currentStrength:profile.strength,
          maxStrength:profile.strength,
          armor:profile.armor,
          conditions:[],
          fastActionAvailable:true,
          slowActionAvailable:true,
          status:'active' as const,
          notes:`Convocado pelo ataque de ${attacker.name}. ${profile.traits.join(' ')}`,
          x:.82,
          y:(index+1)/(count+1),
          quickSheet:{profile:'undead_npc',attributes:{strength:{current:profile.strength,max:profile.strength},agility:{current:2,max:2}},skills:{Melee:profile.melee,Scouting:profile.scouting},weapons:[{name:profile.weapon,damage:profile.weaponDamage}],attacks:[{name:profile.weapon,damage:profile.weaponDamage,...(profile.name==='Carniçal'?{diseaseVirulence:4}:{})}],standardNpcCombat:true,traits:profile.traits},
        }));
        specialEffectSummary=`D10=${countRoll.results[0].value}: ${count} ${profile.name.toLocaleLowerCase('pt-BR')}(s) convocado(s); tipo D10=${typeValue}. Os mortos-vivos entram como combatentes NPC aliados ao dragão e agem em seus próprios turnos.`;
      }
      for(const outcome of outcomes){
        const {target,roll}=outcome;
        // The Bitter Reach p.109, Wendigo — Rajada Congelante: TODAS as
        // vítimas ficam Frias independentemente da salvaguarda para não cair.
        // A salvaguarda apenas determina se o alvo fica Caído.
        if(attacker.entityId==='bitter-wendigo'&&dieValue===1&&outcome.coldApplied&&target.kind==='character'&&target.entityId){
          const characterStore=tx.objectStore('characters');
          const character=await characterStore.get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A vítima da Rajada Congelante não possui ficha válida.');
          const conditions=Array.from(new Set([...character.conditions,'Cold' as const]));
          await characterStore.put({...character,conditions});
          combatants=combatants.map(item=>item.id===target.id?{...item,conditions:Array.from(new Set([...(item.conditions||[]),'Cold'])),quickSheet:{...(item.quickSheet||{}),conditions}}:item);
        }
        if(outcome.specialEffect==='bog_mist'&&target.kind==='character'&&target.entityId){
          const character=await tx.objectStore('characters').get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha da vítima da névoa do Bog Man está ausente ou morta.');
          const expiresRound=encounter.round+(bogMistRounds||1);
          const id=`monster-bog-mist:${attacker.id}:${encounter.round}`;
          const detail=`Névoa do Bog Man: −2 em ataques e rolagens de combate por ${bogMistRounds||1} rodada(s).${outcome.saveFailed?' Falha em Resiliência: ficou Frio.':''}`;
          const conditions=outcome.saveFailed?Array.from(new Set([...character.conditions,'Cold' as const])):character.conditions;
          const effect={id,label:'Névoa do Bog Man',detail,skillModifier:-2,expiresRound,expiresAtCombatantId:target.id};
          const updated={...character,conditions,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-bog-mist:')) ,effect]};
          await tx.objectStore('characters').put(updated);
          combatants=combatants.map(item=>item.id!==target.id?item:{...item,conditions:[...conditions],statusEffects:[...(item.statusEffects||[]).filter(value=>!value.id.startsWith('monster-bog-mist:')),{...effect,name:effect.label}],quickSheet:{...(item.quickSheet||{}),conditions}});
          continue;
        }
        if(outcome.specialEffect){
          if(outcome.saveFailed){
            if(outcome.specialEffect==='plague_attack'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha da vítima da peste demoníaca está ausente ou morta.');
              const virulence=Number((attack as {diseaseVirulence?:number}).diseaseVirulence)||7;
              const infected=DiseaseRulesEngine.expose(character,virulence,outcome.roll.successes,outcome.plagueSuccesses||0);
              const boilId=`monster-plague-boils:${attacker.id}:${encounter.round}`;
              const detail='Pústulas da peste demoníaca: explodem no início da próxima rodada e causam 1 dano de Força. Cura da Natureza bem-sucedida ou magia curativa remove antes da explosão.';
              const updated={...infected,statusEffects:[...(infected.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-plague-boils:')),{id:boilId,label:'Pústulas demoníacas',detail,ongoingRounds:1}]};
              await tx.objectStore('characters').put(updated);
              combatants=combatants.map(item=>item.id!==target.id?item:{...item,conditions:[...updated.conditions],statusEffects:[...(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-plague-boils:')),{id:boilId,name:'Pústulas demoníacas',detail,ongoingRounds:1}],quickSheet:{...(item.quickSheet||{}),attributes:updated.attributes,conditions:updated.conditions,diseaseState:updated.diseaseState}});
              continue;
            }
            if(outcome.specialEffect==='damage_on_failure'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha do alvo do ataque de consumo está ausente ou morta.');
              const strength={...character.attributes.strength,current:Math.max(0,character.attributes.strength.current-(Number((attack as {failureDamage?:number}).failureDamage)||1))};
              const broke=strength.current===0&&character.attributes.strength.current>0;
              const conditions=strength.current===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
              const brokenAttribute=strength.current===0?'strength':character.brokenAttribute==='strength'?undefined:character.brokenAttribute;
              const criticalId=`critical-pending:swarm:${attacker.id}:${encounter.round}:${target.id}`;
              const criticalEffect=broke?{id:criticalId,label:'Ferimento grave pendente',detail:'O ataque de consumo Quebrou Força; resolver a tabela de ferimento grave.',criticalTable:'other' as const}:undefined;
              const criticalMarker='PENDENTE: ferimento grave causado pelo consumo da Morte Enxame';
              const statusEffects=[...(character.statusEffects||[]).filter(effect=>!effect.id.startsWith('critical-pending:swarm:')),...(criticalEffect?[criticalEffect]:[])];
              const updated={...character,attributes:{...character.attributes,strength},conditions,brokenAttribute,statusEffects,...(broke&&!(character.criticalInjuries||[]).includes(criticalMarker)?{criticalInjuries:[...(character.criticalInjuries||[]),criticalMarker]}:{})};
              await tx.objectStore('characters').put(updated);
              combatants=combatants.map(item=>item.id!==target.id?item:{...item,currentStrength:strength.current,status:strength.current===0?'broken' as const:item.status,brokenAttribute,conditions,statusEffects:[...(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('critical-pending:swarm:')),...(criticalEffect?[{id:criticalEffect.id,name:criticalEffect.label,detail:criticalEffect.detail,criticalTable:criticalEffect.criticalTable}]:[])],quickSheet:{...(item.quickSheet||{}),attributes:updated.attributes,conditions,brokenAttribute}});
              continue;
            }
            if(outcome.specialEffect==='mind_disintegration'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha da vítima da desintegração mental está ausente ou morta.');
              const wits={...character.attributes.wits,current:Math.max(0,character.attributes.wits.current-(outcome.witsDamage||0))};
              const catatonic=wits.current===0;
              const conditions=catatonic?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
              const brokenAttribute=catatonic?'wits':character.brokenAttribute==='wits'?undefined:character.brokenAttribute;
              const catatonicEffect={id:`monster-catatonic:${attacker.id}`,label:'Catatônico',detail:'Astúcia Quebrada pela desintegração mental; incapaz de agir ou se comunicar.',speechBlocked:true};
              const statusEffects=[...(character.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-catatonic:')),...(catatonic?[catatonicEffect]:[])];
              const updated={...character,attributes:{...character.attributes,wits},conditions,brokenAttribute,statusEffects};
              await tx.objectStore('characters').put(updated);
              combatants=combatants.map(item=>item.id!==target.id?item:{...item,status:catatonic?'broken' as const:item.status,brokenAttribute,conditions,statusEffects:[...(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-catatonic:')),...(catatonic?[{...catatonicEffect,name:catatonicEffect.label}]:[])],quickSheet:{...(item.quickSheet||{}),attributes:updated.attributes,conditions,brokenAttribute}});
              continue;
            }
            if(outcome.specialEffect==='donkey_head'&&target.kind==='character'&&target.entityId){
              const characterStore=tx.objectStore('characters');const character=await characterStore.get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo da Cabeça de Burro está ausente ou morta.');
              const id=`monster-donkey-head:${attacker.id}:${encounter.round}`;const incapacitatedId=`monster-incapacitated:donkey:${attacker.id}:${encounter.round}`;const detail='Cabeça de Burro: não pode agir até este mesmo ponto da próxima rodada, não pode falar e sofre −3 em testes baseados em Empatia por um Quarto de Dia.';
              const characterEffects=[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-donkey-head:')&&!value.id.startsWith('monster-incapacitated:donkey:')),{id,label:'Cabeça de Burro',detail,remainingQuarters:1,empathySkillModifier:-3,speechBlocked:true},{id:incapacitatedId,label:'Atordoado pela transformação',detail:'Não pode realizar ações até este mesmo ponto da próxima rodada.',expiresRound:encounter.round+1}];
              await characterStore.put({...character,statusEffects:characterEffects});
              combatants=combatants.map(item=>item.id!==target.id?item:{...item,statusEffects:[...(item.statusEffects||[]).filter(value=>!value.id.startsWith('monster-donkey-head:')&&!value.id.startsWith('monster-incapacitated:donkey:')),{id,name:'Cabeça de Burro',detail,remainingQuarters:1,empathySkillModifier:-3,speechBlocked:true},{id:incapacitatedId,name:'Atordoado pela transformação',detail:'Não pode agir até este mesmo ponto da próxima rodada.',expiresRound:encounter.round+1,expiresAtCombatantId:target.id}]});
              continue;
            }
            if(outcome.specialEffect==='lead_arms'&&target.kind==='character'&&target.entityId){
              const characterStore=tx.objectStore('characters');const character=await characterStore.get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo de Braços Pesados está ausente ou morta.');
              const nowDropped=character.weapons||[];for(const item of nowDropped)droppedEquipment=[...droppedEquipment,{id:`dropped:lead-arms:${encounter.id}:${encounter.round}:${target.id}:${crypto.randomUUID()}`,characterId:character.id,characterName:character.name,collection:'weapons' as const,item,droppedAt:Date.now()}];
              const id=`monster-lead-arms:${attacker.id}:${encounter.round}`;const detail='Braços Pesados: armas largadas; não pode atacar nem usar os braços até o próximo Quarto de Dia.';
              await characterStore.put({...character,weapons:[],statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-lead-arms:')),{id,label:'Braços Pesados',detail,remainingQuarters:1}]});
              combatants=combatants.map(item=>item.id!==target.id?item:{...item,quickSheet:{...(item.quickSheet||{}),weapons:[]},statusEffects:[...(item.statusEffects||[]).filter(value=>!value.id.startsWith('monster-lead-arms:')),{id,name:'Braços Pesados',detail,remainingQuarters:1}]});
              continue;
            }
            if(outcome.specialEffect==='flatulence'){
              const nearby=combatants.filter(item=>item.id!==target.id&&item.side==='party'&&item.status==='active'&&Math.hypot(Number(item.x??.5)-Number(target.x??.5),Number(item.y??.5)-Number(target.y??.5))<=.25);
              const effectId=`monster-flatulence:${attacker.id}:${encounter.round}:${target.id}`;
              for(const affected of nearby){
                if(affected.kind!=='character'||!affected.entityId)continue;
                const character=await tx.objectStore('characters').get(affected.entityId);
                if(!character||character.status==='dead')continue;
                await tx.objectStore('characters').put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-flatulence:')),{id:effectId,label:'Flatulência do Imp',detail:'−2 em todos os testes de perícia por 15 minutos.',remainingMinutes:15,skillModifier:-2}]});
              }
              combatants=combatants.map(item=>nearby.some(affected=>affected.id===item.id)?{...item,statusEffects:[...(item.statusEffects||[]).filter(value=>!value.id.startsWith('monster-flatulence:')),{id:effectId,name:'Flatulência do Imp',detail:'−2 em todos os testes de perícia por 15 minutos.',remainingMinutes:15,skillModifier:-2}]}:item);
              continue;
            }
            if(outcome.specialEffect==='disease'&&target.kind==='character'&&target.entityId){
              const characterStore=tx.objectStore('characters');
              const character=await characterStore.get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha do alvo da infecção está ausente ou morta.');
              const diseaseVirulence=Number((attack as {diseaseVirulence?:number}).diseaseVirulence)||1;
              const infected=DiseaseRulesEngine.infect(character,diseaseVirulence);
              await characterStore.put(infected);
              combatants=combatants.map(item=>item.id===target.id?{...item,conditions:[...infected.conditions],quickSheet:{...(item.quickSheet||{}),conditions:infected.conditions,diseaseState:infected.diseaseState}}:item);
              continue;
            }
            if(outcome.specialEffect==='drowning'&&target.kind==='character'&&target.entityId){
              const characterStore=tx.objectStore('characters');
              const character=await characterStore.get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha do alvo sufocado está ausente ou morta.');
              const existing=DrowningRulesEngine.normalize(character.drowningState);
              const entered=existing?{...character,drowningState:{...existing,phase:'drowning' as const}}:DrowningRulesEngine.enterWater(character,false);
              const drowningInput={...entered,drowningState:{...(DrowningRulesEngine.normalize(entered.drowningState)||{metalArmor:false}),phase:'drowning' as const}};
              const result=DrowningRulesEngine.resolveTurn(drowningInput,0,rng);
              const broke=character.attributes.strength.current>0&&result.character.attributes.strength.current===0;
              const criticalMarker='PENDENTE: ferimento grave causado por afogamento';
              const detail=`Sufocado pela névoa de ${attacker.name}; sofre 1 dano de Força por turno${result.character.drowningState?.minutesUntilDeath?`; morte em ${result.character.drowningState.minutesUntilDeath} minuto(s) sem resgate`:''}.`;
              const bristleEffect=attack.suffocationRecovery?{id:`monster-bristles:${attacker.id}`,label:'Cerdas nas vias respiratórias',detail:'Teste gratuito de Resiliência no próprio turno para tossir as cerdas e encerrar o sufocamento.'}:undefined;
              const updatedCharacter={...result.character,statusEffects:[...(result.character.statusEffects||[]).filter(effect=>effect.id!=='drowning'&&!effect.id.startsWith('critical-pending:drowning:')&&(!bristleEffect||!effect.id.startsWith('monster-bristles:'))),{id:'drowning',label:'Afogamento',detail},...(bristleEffect?[bristleEffect]:[]),...(broke?[{id:`critical-pending:drowning:${crypto.randomUUID()}`,label:'Ferimento grave pendente',detail:'Afogamento Quebrou Força; resolva dano atípico.'}]:[])],...(broke&&!(result.character.criticalInjuries||[]).includes(criticalMarker)?{criticalInjuries:[...(result.character.criticalInjuries||[]),criticalMarker]}:{})};
              await characterStore.put(updatedCharacter);
              combatants=combatants.map(item=>item.id!==target.id?item:{...item,currentStrength:updatedCharacter.attributes.strength.current,brokenAttribute:updatedCharacter.brokenAttribute,status:updatedCharacter.attributes.strength.current===0?'broken' as const:item.status,conditions:[...updatedCharacter.conditions],statusEffects:[...(item.statusEffects||[]).filter(effect=>effect.id!=='drowning'&&(!bristleEffect||!effect.id.startsWith('monster-bristles:'))),{id:'drowning',name:'Afogamento',detail},...(bristleEffect?[{...bristleEffect,name:bristleEffect.label}]:[]),...(broke?[{id:`critical-pending:drowning:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'Afogamento Quebrou Força; resolva dano atípico.',criticalTable:'other' as const}]:[])],quickSheet:{...(item.quickSheet||{}),attributes:updatedCharacter.attributes,conditions:updatedCharacter.conditions,brokenAttribute:updatedCharacter.brokenAttribute,drowningState:updatedCharacter.drowningState}});
              continue;
            }
            const petrificationRounds=outcome.specialEffect==='petrified'?2+Math.floor(Math.max(0,Math.min(.999999999,rng()))*6)+2+Math.floor(Math.max(0,Math.min(.999999999,rng()))*6):undefined;
            const effect=outcome.specialEffect==='possessed'
              ?{id:`monster-possessed:${attacker.id}`,name:'Possuída',detail:`Controlada como marionete mental por ${attacker.name}. No próprio turno pode tentar romper o controle com um teste gratuito de Astúcia + Discernimento; uma tentativa por rodada.`,source:attacker.name,lastAttemptRound:encounter.round-1}
              :outcome.specialEffect==='magic_leech'
              ?{id:`monster-magic-leech:${attacker.id}`,name:'Agarrado pelo Imp',detail:`Preso pelo roubo mágico: teste gratuito de Astúcia + Discernimento contra Astúcia ${Number(((attacker.quickSheet?.attributes||{}) as Record<string,{current?:number}>).wits?.current)||3} do Imp em cada turno; falha perde 1 PFV e 1 Astúcia. O Imp solta se sofrer 2 de dano de um único ataque.`,source:attacker.name}
              :outcome.specialEffect==='dragged'
              ?{id:`monster-dragged:${attacker.id}`,name:'Sendo arrastado',detail:`Preso pelas aranhas jovens: arrastado uma zona para longe do grupo por rodada. Pode tentar escapar com ação lenta, Força + Potência contra a Força das aranhas.`,source:attacker.name}
              :outcome.specialEffect==='hypnotic_gaze'
              ?{id:`monster-hypnotized:${attacker.id}:${encounter.round}`,name:'Olhar Hipnótico',detail:`Deve atacar outro aventureiro até o próximo ponto da iniciativa; recebe +2 no ataque e +1 no dano da arma.`,source:attacker.name,attackModifier:2,weaponDamageModifier:1,expiresRound:encounter.round+1,expiresAtCombatantId:target.id}
              :outcome.specialEffect==='slimed'
              ?{id:`monster-slimed:${crypto.randomUUID()}`,name:'Coberto por gosma',detail:`−2 em ataques até a próxima iniciativa de ${target.name}.`,source:attacker.name,attackModifier:-2,expiresRound:encounter.round+1,expiresAtCombatantId:target.id}
              :outcome.specialEffect==='prone'
              ?{id:'condition:prone',name:'Caído',detail:`Derrubado por ${attacker.name}; gaste uma ação rápida para se levantar.`,source:attacker.name}
              :outcome.specialEffect==='blinded'
                ?{id:`monster-blinded:${crypto.randomUUID()}`,name:'Cego',detail:`−2 em ataques e rolagens de combate até a próxima iniciativa de ${target.name}.`,source:attacker.name,expiresRound:encounter.round+1,expiresAtCombatantId:target.id}
                :outcome.specialEffect==='webbed'
                  ?{id:`monster-webbed:${crypto.randomUUID()}`,name:'Preso na teia',detail:`Imóvel e encapsulado por ${attacker.name}; sofre 1 dano de Força no início de cada rodada. Escapar exige uma ação lenta e um teste de Força + Potência.`,source:attacker.name}
                  :outcome.specialEffect==='petrified'
                  ?{id:'monster-petrified',name:'Petrificado',detail:`Imóvel e sem ações; teste gratuito de Resiliência no próprio turno. Morte em ${petrificationRounds} rodada(s) se não reverter a petrificação.`,source:attacker.name,ongoingRounds:petrificationRounds}
              :outcome.specialEffect==='life_drain'
                    ?{id:`monster-life-drain:${attacker.id}`,name:'Preso pelo dreno vital',detail:`Preso pelas garras de ${attacker.name}; sofre 1 dano de Força por rodada e não pode escapar sozinho. Um aliado rompe o efeito ao causar pelo menos 3 de dano em ${attacker.name}.`,source:attacker.name,ongoingDamage:1}
                  :outcome.specialEffect==='otherworldly_hunger'
                    ?{id:`monster-otherworldly-hunger:${attacker.id}`,name:'Fome sobrenatural',detail:'−1 em todos os testes de perícia durante este combate. Comer não remove o efeito; magia druídica curativa encerra.',source:attacker.name,skillModifier:-1}
                  :outcome.specialEffect==='otherworldly_thirst'
                    ?{id:`monster-otherworldly-thirst:${attacker.id}`,name:'Sede sobrenatural',detail:'−2 em ataques e testes de perícia durante este combate. Beber não remove o efeito; magia druídica curativa encerra.',source:attacker.name,skillModifier:-2}
                  :{id:`monster-incapacitated:${crypto.randomUUID()}`,name:'Incapacitado',detail:`Perde ações até o mesmo ponto da iniciativa na próxima rodada por efeito de ${attacker.name}.`,source:attacker.name,expiresRound:encounter.round+1,expiresAtCombatantId:target.id};
            if(petrificationRounds!==undefined&&target.kind==='character'&&target.entityId){
              const characterStore=tx.objectStore('characters');
              const character=await characterStore.get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha do alvo petrificado está ausente ou morta.');
              await characterStore.put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>value.id!=='monster-petrified'),{id:'monster-petrified',label:'Petrificado',detail:effect.detail,ongoingRounds:petrificationRounds}]});
            }
            if(outcome.specialEffect==='incapacitated'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha do alvo incapacitado está ausente ou morta.');
              await tx.objectStore('characters').put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-incapacitated:')),{id:effect.id,label:effect.name,detail:effect.detail,expiresRound:effect.expiresRound,expiresAtCombatantId:target.id}]});
            }
            if(outcome.specialEffect==='possessed'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha da vítima da possessão está ausente ou morta.');
              await tx.objectStore('characters').put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-possessed:')),{id:effect.id,label:effect.name,detail:effect.detail,lastAttemptRound:encounter.round-1}]});
            }
            if(outcome.specialEffect==='hypnotic_gaze'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha da vítima do Olhar Hipnótico está ausente ou morta.');
              await tx.objectStore('characters').put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-hypnotized:')),{id:effect.id,label:effect.name,detail:effect.detail,source:attacker.name,attackModifier:2,weaponDamageModifier:1,expiresRound:encounter.round+1,expiresAtCombatantId:target.id}]});
            }
            if(outcome.specialEffect==='life_drain'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);
              if(!character||character.status==='dead')throw new Error('A ficha da vítima do dreno vital está ausente ou morta.');
              await tx.objectStore('characters').put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-life-drain:')),{id:effect.id,label:effect.name,detail:effect.detail}]});
            }
            if(outcome.specialEffect==='dragged'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha da vítima do arrasto está ausente ou morta.');
              await tx.objectStore('characters').put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-dragged:')),{id:effect.id,label:effect.name,detail:effect.detail}]});
            }
            if(outcome.specialEffect==='magic_leech'&&target.kind==='character'&&target.entityId){
              const character=await tx.objectStore('characters').get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha da vítima do roubo mágico está ausente ou morta.');
              await tx.objectStore('characters').put({...character,statusEffects:[...(character.statusEffects||[]).filter(value=>!value.id.startsWith('monster-magic-leech:')),{id:effect.id,label:effect.name,detail:effect.detail}]});
            }
            combatants=combatants.map(item=>item.id===target.id?{...item,statusEffects:[...(item.statusEffects||[]).filter(value=>!['condition:prone','monster-petrified'].includes(value.id)&&!(value.id.startsWith('monster-incapacitated:'))&&!(value.id.startsWith('monster-blinded:'))&&!(value.id.startsWith('monster-slimed:'))&&!(value.id.startsWith('monster-webbed:'))&&!(value.id.startsWith('monster-life-drain:'))&&!(value.id.startsWith('monster-dragged:'))&&!(value.id.startsWith('monster-magic-leech:'))&&!(value.id.startsWith('monster-otherworldly-hunger:'))&&!(value.id.startsWith('monster-otherworldly-thirst:'))),effect]}:item);
          }
          continue;
        }
        if(outcome.fearCharacter){
          const pending=outcome.fearPending?[{id:`critical-pending:monster-horror:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'O ataque de medo Quebrou Astúcia; resolver a tabela D66 de horror.',criticalTable:'horror' as const}]:[];
          const special=pendingEffectTags.length?[{id:`monster-special:${crypto.randomUUID()}`,name:'Efeito especial do monstro pendente',detail:`${attack.name}: resolver para ${pendingEffectTags.join(', ')} conforme a entrada oficial.`,source:attacker.name}]:[];
          const tongueEffect=outcome.tongueTwisted?[{id:`monster-tongue-twisting:${attacker.id}:${encounter.round}`,name:'Língua Embaralhada',detail:'Não pode falar por um Quarto de Dia.',remainingQuarters:1,speechBlocked:true}]:[];
          const proneEffect=outcome.proneApplied?{id:'condition:prone',name:'Caído',detail:`Derrubado por ${attacker.name}; gaste uma ação rápida para se levantar.`}:undefined;
          const statusEffects=[...(target.statusEffects||[]).filter(effect=>!effect.id.startsWith('critical-pending:')&&!effect.id.startsWith('monster-tongue-twisting:')&&(!proneEffect||effect.id!=='condition:prone')),...pending,...special,...tongueEffect,...(proneEffect?[proneEffect]:[])];
          combatants=combatants.map(item=>item.id===target.id?{...item,status:outcome.fearCharacter!.brokenAttribute?'broken' as const:item.status,brokenAttribute:outcome.fearCharacter!.brokenAttribute,conditions:[...outcome.fearCharacter!.conditions],statusEffects,quickSheet:{...(item.quickSheet||{}),attributes:outcome.fearCharacter!.attributes,conditions:outcome.fearCharacter!.conditions,brokenAttribute:outcome.fearCharacter!.brokenAttribute}}:item);
          continue;
        }
        const strength=Math.max(0,target.currentStrength-(PublishedMonsterDamageRulesEngine.attribute(tags)==='strength'?outcome.finalDamage:0));
        const pendingCritical=outcome.criticalTable?{id:`critical-pending:monster:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'O ataque do monstro Quebrou Força; resolver a tabela indicada.',criticalTable:outcome.criticalTable}:undefined;
        let conditions=outcome.coldApplied?Array.from(new Set([...(target.conditions||[]),'Cold'])):(target.conditions||[]);
        let brokenAttribute=target.brokenAttribute;
        if(target.kind==='character'&&target.entityId){
          const characterStore=tx.objectStore('characters');
          const character=await characterStore.get(target.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha do alvo está ausente ou morta; o ataque não foi confirmado.');
          if(character){
            const specialDamage=PublishedMonsterDamageRulesEngine.apply(character,outcome.finalDamage,tags,`${attacker.name}: ${attack.name}`);
            let updatedCharacter:Character=specialDamage.character;
            // Sangrento nº 5: o MESMO golpe causa dano em Força E Astúcia.
            // Dano real já calculado após a armadura; dano em Astúcia não reduz duas vezes a armadura.
            if(attacker.entityId===BloodMistDemonRulesEngine.PROFILE_ID&&dieValue===5&&outcome.finalDamage>0){
              const beforeWits=updatedCharacter.attributes.wits.current;
              updatedCharacter=CriticalInjuryRulesEngine.applyAttributeDamage(updatedCharacter,'wits',outcome.finalDamage);
              if(beforeWits>0&&updatedCharacter.attributes.wits.current===0){
                const marker='PENDENTE: ferimento grave de Horror por mordida do Sangrento';
                if(!updatedCharacter.criticalInjuries.includes(marker))updatedCharacter={...updatedCharacter,criticalInjuries:[...updatedCharacter.criticalInjuries,marker]};
              }
              outcome.witsDamage=outcome.finalDamage;
            }

            if(outcome.disarmAttempt){
              // The Bitter Reach p. 104: attack D8 hits, then opposed MIGHT;
              // only if Guardian wins does the held weapon leave the sheet.
              const guardianMight=ForbiddenLandsDiceEngine.roll({base:Math.max(0,attacker.currentStrength),skill:combatantSkill(attacker,undefined,['Might','Potência','Potencia']),gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-guardian-might:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
              const victimMight=ForbiddenLandsDiceEngine.roll({base:Math.max(0,updatedCharacter.attributes.strength.current),skill:normalizedSkillRank(updatedCharacter,['Might','Potência','Potencia']),gear:0},{characterId:updatedCharacter.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-guardian-victim-might:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:updatedCharacter.criticalInjuryStates,skillName:'might',attributeName:'strength'});
              await tx.objectStore('rolls').put(guardianMight);
              await tx.objectStore('rolls').put(victimMight);
              if(guardianMight.successes>victimMight.successes){
                const index=(updatedCharacter.weapons||[]).findIndex(weapon=>!isShield(weapon as Record<string,unknown>));
                if(index>=0){
                  const selected=updatedCharacter.weapons[index];
                  updatedCharacter={...updatedCharacter,weapons:updatedCharacter.weapons.filter((_,i)=>i!==index)};
                  droppedEquipment=[...droppedEquipment,{id:`dropped:bitter-guardian:${encounter.id}:${encounter.round}:${target.id}:${crypto.randomUUID()}`,characterId:updatedCharacter.id,characterName:updatedCharacter.name,collection:'weapons' as const,item:{...selected},droppedAt:Date.now()}];
                  outcome.poisonSummary=[outcome.poisonSummary,`desarmado: ${selected.name} caiu no chão`].filter(Boolean).join('; ');
                }
              }
            }
            const witsBroke=specialDamage.newlyBroken&&specialDamage.attribute==='wits';
            conditions=updatedCharacter.conditions;
            brokenAttribute=updatedCharacter.brokenAttribute;
            if(tags.includes('disease')&&outcome.finalDamage>0&&Number(attack.diseaseVirulence)>0&&!updatedCharacter.diseaseState){
              // Livro do Mestre: teste resistido de Resiliência contra Virulência.
              const rank=normalizedSkillRank(updatedCharacter,['Endurance','Resiliência','Resilience']);
              const victimRoll=ForbiddenLandsDiceEngine.roll({base:updatedCharacter.attributes.strength.current,skill:rank,gear:0},{characterId:updatedCharacter.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-disease-save:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
              const diseaseRoll=ForbiddenLandsDiceEngine.roll({base:Number(attack.diseaseVirulence),skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`bitter-disease-virulence:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
              await tx.objectStore('rolls').put(victimRoll);
              await tx.objectStore('rolls').put(diseaseRoll);
              updatedCharacter=DiseaseRulesEngine.expose(updatedCharacter,Number(attack.diseaseVirulence),victimRoll.successes,diseaseRoll.successes);
            }
            if(outcome.coldApplied)updatedCharacter={...updatedCharacter,conditions:Array.from(new Set([...updatedCharacter.conditions,'Cold' as const]))};
            if(outcome.drowningApplied){
              const existing=DrowningRulesEngine.normalize(updatedCharacter.drowningState);
              if(existing)updatedCharacter={...updatedCharacter,drowningState:{...existing,phase:'underwater'}};
              else{const entered=DrowningRulesEngine.enterWater(updatedCharacter,false);updatedCharacter={...entered,drowningState:{phase:'underwater',metalArmor:false}};}
            }
            let poisonEffect:{id:string;name:string;detail:string;source:string}|undefined;
            const poisonTriggerMet=(outcome.poisonTrigger==='on_hit'&&outcome.hit)||(outcome.poisonTrigger==='on_damage'&&outcome.finalDamage>0)||(outcome.poisonTrigger==='move_failure'&&outcome.moveFailed===true)||(outcome.poisonTrigger==='automatic'&&outcome.automaticPoison===true)||(outcome.poisonTrigger==='ongoing_grapple'&&outcome.grappleApplied)||(['wits_failure','endurance_failure'].includes(outcome.poisonTrigger||'')&&outcome.saveFailed===true);
            if(poisonTriggerMet&&outcome.poisonKind&&outcome.poisonPotency&&character.poisonState?.active){
              outcome.poisonSummary='veneno não aplicado porque a personagem já sofre um efeito total ativo';
            }else if(poisonTriggerMet&&outcome.poisonKind&&outcome.poisonPotency){
              const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
              const resilience=Object.entries(updatedCharacter.skills||{}).find(([name])=>['resilience','resiliencia'].includes(normalizeSkill(name)))?.[1]||0;
              const poisonScene=encounter.startedSceneId||encounter.id;
              const victimRoll=ForbiddenLandsDiceEngine.roll({base:updatedCharacter.attributes.strength.current,skill:Number(resilience)||0,gear:0},{characterId:updatedCharacter.id,sceneId:poisonScene,checkId:`monster-poison-resistance:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:updatedCharacter.criticalInjuryStates,skillName:'resilience',attributeName:'strength'});
              const poisonRoll=outcome.automaticPoison?roll:ForbiddenLandsDiceEngine.roll({base:outcome.poisonPotency,skill:0,gear:0},{characterId:null,sceneId:poisonScene,checkId:`monster-poison-potency:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng});
              await tx.objectStore('rolls').put(victimRoll);if(!outcome.automaticPoison)await tx.objectStore('rolls').put(poisonRoll);
              const exposure=PoisonRulesEngine.expose(updatedCharacter,outcome.poisonKind,outcome.poisonPotency,victimRoll.successes,poisonRoll.successes);
              updatedCharacter=exposure.character;
              outcome.poisonSummary=exposure.totalEffect?`veneno ${outcome.poisonKind} Potência ${outcome.poisonPotency} ativo (${poisonRoll.successes} × ${victimRoll.successes})`:`resistiu ao veneno ${outcome.poisonKind}; 1 dano em ${exposure.attribute}`;
              poisonEffect={id:`monster-poison:${crypto.randomUUID()}`,name:`Veneno ${outcome.poisonKind}`,detail:outcome.poisonSummary,source:attacker.name};
            }
            if(outcome.contaminatesFood&&updatedCharacter.resourceDice?.food&&outcome.poisonKind&&outcome.poisonPotency){
              updatedCharacter={...updatedCharacter,foodPoisonState:{kind:outcome.poisonKind,potency:outcome.poisonPotency,source:attacker.name}};
            }
            const wartsApplied=attack.poisonSecondaryEffect==='empathy_warts'&&outcome.moveFailed===true;
            if(wartsApplied){updatedCharacter=BasiliskWartsRulesEngine.apply(updatedCharacter);outcome.poisonSummary=[outcome.poisonSummary,'verrugas reduziram Empatia até tratamento com Cura ou magia'].filter(Boolean).join('; ');}
            let combinedFearPending:{id:string;name:string;detail:string;criticalTable:'horror'}|undefined;
            const agilityPending=specialDamage.newlyBroken&&specialDamage.attribute==='agility'?{id:`critical-pending:monster-agility:${target.id}:${encounter.round}`,name:'Agilidade Quebrada',detail:`${attacker.name}: ${attack.name} Quebrou Agilidade; incapacitação e ferimento grave devem ser resolvidos conforme as regras.`,criticalTable:'other' as const}:undefined;
            const witsPending=witsBroke?{id:`critical-pending:monster-wits:${target.id}:${encounter.round}`,name:'Ferimento grave pendente',detail:'A decapitação Quebrou Astúcia; resolver a tabela de Horror.',criticalTable:'horror' as const}:undefined;
            if(tags.includes('fear')&&!fearOnly&&(outcome.hit||outcome.fearSuccesses!==undefined)&&target.kind==='character'){
              const fearlessRank=FearRulesEngine.fearlessRank(updatedCharacter);
              let defenseRoll:DiceRoll|undefined;
              if(fearlessRank>=1&&fearlessRank<3&&!BloodmarchBetaMentalGuard.power(character.statusEffects)){
                const normalizedFearSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
                const insight=Object.entries(updatedCharacter.skills||{}).find(([name])=>['insight','discernimento'].includes(normalizedFearSkill(name)))?.[1]||0;
                defenseRoll=ForbiddenLandsDiceEngine.roll({base:updatedCharacter.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:updatedCharacter.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`fear-defense:${encounter.id}:${encounter.round}:${target.id}:${attacker.id}`,criticalInjuries:updatedCharacter.criticalInjuryStates,skillName:'insight',attributeName:'wits',rng});
                await tx.objectStore('rolls').put(defenseRoll);
              }
              const fear=FearRulesEngine.resolve(updatedCharacter,outcome.fearSuccesses??roll.successes,{defenseSuccesses:defenseRoll?.successes||0,damageAttribute:fearDamageAttribute});
              updatedCharacter=fear.character;
              if(tags.includes('cold')&&(outcome.coldApplied||fear.damage>0))updatedCharacter={...updatedCharacter,conditions:Array.from(new Set([...updatedCharacter.conditions,'Cold' as const]))};
              outcome.fearSummary=`medo: ${fear.damage} dano em ${fear.attribute}${fear.horrorPending?'; Horror grave pendente':''}`;
              if(fear.horrorPending){const marker='PENDENTE: ferimento grave — tabela de horror';if(!(updatedCharacter.criticalInjuries||[]).includes(marker))updatedCharacter={...updatedCharacter,criticalInjuries:[...(updatedCharacter.criticalInjuries||[]),marker]};combinedFearPending={id:`critical-pending:monster-horror:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'O ataque combinado Quebrou Astúcia; resolver a tabela D66 de horror.',criticalTable:'horror'};}
            }
            const targetPendingTags=pendingEffectTags;
            if(targetPendingTags.length)specialPending=true;
            const pendingEffects=targetPendingTags.length?[{id:`monster-special:${crypto.randomUUID()}`,name:'Efeito especial do monstro pendente',detail:`${attack.name}: resolver para ${targetPendingTags.join(', ')} conforme a entrada oficial.`,source:attacker.name}]:[];
            const proneEffect=outcome.proneApplied?{id:'condition:prone',name:'Caído',detail:`Derrubado por ${attacker.name}; gaste uma ação rápida para se levantar.`}:undefined;
            const wartsEffect=updatedCharacter.basiliskWarts?{id:'basilisk-warts',name:'Verrugas de basilisco',detail:`Empatia reduzida em ${updatedCharacter.basiliskWarts.loss}; Cura ou magia curativa restaura Empatia ${updatedCharacter.basiliskWarts.originalEmpathy}.`,source:attacker.name}:undefined;
            const grappleEffect=outcome.grappleApplied?{id:publishedIceGiantMorsel?`monster-grappled:ice-giant-morsel:${attacker.id}`:`monster-grappled:${attacker.id}`,name:'Agarrado',detail:publishedIceGiantMorsel?'Gigante de Gelo: mordida automática 12D/Dano 3 a cada rodada até escapar com Potência (ação lenta).':`Impedido de se deslocar por ${attacker.name}; escapar exige ação lenta e Força + Potência.`,source:attacker.name}:undefined;
            const entRootsEffect=outcome.entRootsApplied?{id:`official-ent-roots:${attacker.entityId}`,name:'Preso pelas raízes nodosas · Força 3',detail:'Raízes do Ent prenderam a personagem. A R88 não define uma rolagem de fuga; a Mestra pode adjudicar o rompimento ou registrar um resgate externo.',source:attacker.name}:undefined;
            const entEmbraceEffect=outcome.entEmbraceApplied?{id:`official-ent-embrace:${attacker.entityId}`,name:'Preso no corpo do Ent',detail:'A personagem pode ser libertada se um único golpe causar pelo menos 4 dano ao Ent que a prendeu ou se alguém a resgatar de fora.',source:attacker.name}:undefined;
            const drowningEffect=updatedCharacter.drowningState?{id:'drowning',name:'Afogamento',detail:`${updatedCharacter.drowningState.phase==='underwater'?'Submerso: teste de Resiliência no próprio turno.':'Afogando: 1 dano de Força por turno.'}${updatedCharacter.drowningState.minutesUntilDeath?` Morte em ${updatedCharacter.drowningState.minutesUntilDeath} minuto(s) sem resgate.`:''}`,source:attacker.name}:undefined;
            const eggsEffect=outcome.eggIncubationRounds?{id:`monster-eggs:${attacker.id}`,name:'Ovos da Skolopendra',detail:`Larvas eclodem em ${outcome.eggIncubationRounds} rodada(s); um teste de Cura bem-sucedido ou magia curativa interrompe a infestação.`,source:attacker.name,ongoingRounds:outcome.eggIncubationRounds}:undefined;
            const foodEffect=updatedCharacter.foodPoisonState?{id:`monster-befouled-food:${attacker.id}`,name:'Comida contaminada',detail:`A reserva de comida está contaminada com veneno ${updatedCharacter.foodPoisonState.kind} de Potência ${updatedCharacter.foodPoisonState.potency}; a exposição ocorre ao consumir a próxima porção.`,source:updatedCharacter.foodPoisonState.source}:undefined;
            const consumedEffect=outcome.consumedApplied?{id:`monster-consumed:${attacker.id}`,name:'Engolido',detail:`Dentro de ${attacker.name}: 1 dano de Força por rodada; ao Quebrar, morre. Escape ao causar ${Number((attack as {releaseDamage?:number}).releaseDamage)||3} dano em um ataque.${attacker.name.toLowerCase().includes('shapeshifter')?' Um aliado também pode libertar a vítima com ação lenta.':''}`,source:attacker.name,ongoingDamage:Number((attack as {releaseDamage?:number}).releaseDamage)||3}:undefined;
            // The Bitter Reach p.109: dois acertos com dano prendem a vítima nos chifres.
            // Na rodada seguinte o Wendigo arranca os chifres causando 2 dano automático.
            const antlersEffect=attacker.entityId==='bitter-wendigo'&&dieValue===4&&outcome.proneApplied&&outcome.finalDamage>0?{id:`bitter-wendigo-antlers:${attacker.id}:${encounter.round}`,name:'Empalado pelos chifres do Wendigo',detail:'Na próxima rodada sofre automaticamente 2 dano em Força, sem nova rolagem de ataque.',source:attacker.id,ongoingDamage:2,expiresRound:encounter.round+1}:undefined;
            const effects=[...(target.statusEffects||[]).filter(effect=>(!proneEffect||effect.id!=='condition:prone')&&(!wartsEffect||effect.id!=='basilisk-warts')&&(!grappleEffect||!effect.id.startsWith('monster-grappled:'))&&(!entRootsEffect||!effect.id.startsWith('official-ent-roots:'))&&(!entEmbraceEffect||!effect.id.startsWith('official-ent-embrace:'))&&(!drowningEffect||effect.id!=='drowning')&&(!eggsEffect||!effect.id.startsWith('monster-eggs:'))&&(!consumedEffect||!effect.id.startsWith('monster-consumed:'))&&(!foodEffect||!effect.id.startsWith('monster-befouled-food:'))),...(poisonEffect?[poisonEffect]:[]),...(proneEffect?[proneEffect]:[]),...(grappleEffect?[grappleEffect]:[]),...(entRootsEffect?[entRootsEffect]:[]),...(entEmbraceEffect?[entEmbraceEffect]:[]),...(drowningEffect?[drowningEffect]:[]),...(eggsEffect?[eggsEffect]:[]),...(consumedEffect?[consumedEffect]:[]),...(antlersEffect?[antlersEffect]:[]),...(foodEffect?[foodEffect]:[]),...(wartsEffect?[wartsEffect]:[]),...pendingEffects,...(pendingCritical?[pendingCritical]:[]),...(combinedFearPending?[combinedFearPending]:[]),...(witsPending?[witsPending]:[]),...(agilityPending?[agilityPending]:[])];
            if(grappleEffect||entRootsEffect||entEmbraceEffect||drowningEffect||eggsEffect||consumedEffect||foodEffect||antlersEffect)updatedCharacter={...updatedCharacter,statusEffects:[...(updatedCharacter.statusEffects||[]).filter(effect=>(!grappleEffect||!effect.id.startsWith('monster-grappled:'))&&(!entRootsEffect||!effect.id.startsWith('official-ent-roots:'))&&(!entEmbraceEffect||!effect.id.startsWith('official-ent-embrace:'))&&(!drowningEffect||effect.id!=='drowning')&&(!eggsEffect||!effect.id.startsWith('monster-eggs:'))&&(!consumedEffect||!effect.id.startsWith('monster-consumed:'))&&(!foodEffect||!effect.id.startsWith('monster-befouled-food:'))),...(grappleEffect?[{id:grappleEffect.id,label:grappleEffect.name,detail:grappleEffect.detail}]:[]),...(entRootsEffect?[{id:entRootsEffect.id,label:entRootsEffect.name,detail:entRootsEffect.detail,source:entRootsEffect.source}]:[]),...(entEmbraceEffect?[{id:entEmbraceEffect.id,label:entEmbraceEffect.name,detail:entEmbraceEffect.detail,source:entEmbraceEffect.source}]:[]),...(drowningEffect?[{id:drowningEffect.id,label:drowningEffect.name,detail:drowningEffect.detail}]:[]),...(eggsEffect?[{id:eggsEffect.id,label:eggsEffect.name,detail:eggsEffect.detail,ongoingRounds:eggsEffect.ongoingRounds}]:[]),...(consumedEffect?[{id:consumedEffect.id,label:consumedEffect.name,detail:consumedEffect.detail,ongoingDamage:consumedEffect.ongoingDamage}]:[]),...(antlersEffect?[{id:antlersEffect.id,label:antlersEffect.name,detail:antlersEffect.detail,source:antlersEffect.source,expiresRound:antlersEffect.expiresRound}]:[]),...(foodEffect?[{id:foodEffect.id,label:foodEffect.name,detail:foodEffect.detail}]:[])]};
            const finalAttr=updatedCharacter.attributes.strength;
            const finalConditions=updatedCharacter.conditions;
            const finalBroken=updatedCharacter.brokenAttribute;
            const brokeAfterEffect=finalAttr.current===0&&character.attributes.strength.current>0;
            const criticalMarker='PENDENTE: ferimento grave causado por monstro';
            if(brokeAfterEffect&&!(updatedCharacter.criticalInjuries||[]).includes(criticalMarker))updatedCharacter={...updatedCharacter,criticalInjuries:[...(updatedCharacter.criticalInjuries||[]),criticalMarker]};
            await characterStore.put(updatedCharacter);
            combatants=combatants.map(item=>item.id===target.id?{...item,currentStrength:finalAttr.current,brokenAttribute:finalBroken,status:updatedCharacter.status==='dead'?'dead' as const:finalBroken?'broken' as const:item.status,conditions:[...finalConditions],statusEffects:effects,armor:Math.max(0,item.armor-outcome.armorLoss),quickSheet:{...(item.quickSheet||{}),weapons:updatedCharacter.weapons,attributes:updatedCharacter.attributes,conditions:finalConditions,brokenAttribute:finalBroken,poisonState:updatedCharacter.poisonState,foodPoisonState:updatedCharacter.foodPoisonState,drowningState:updatedCharacter.drowningState}}:item);
          }
        }else{
          const targetPendingTags=tags.filter(tag=>!['area','two_targets','highest_strength','gm_selects','grapple','drowning','ongoing','ent_roots','ent_embrace','damage_empathy'].includes(tag)&&!(tag==='prone'&&outcome.proneApplied));
          if(targetPendingTags.length)specialPending=true;
          const proneEffect=outcome.proneApplied?{id:'condition:prone',name:'Caído',detail:`Derrubado por ${attacker.name}; gaste uma ação rápida para se levantar.`}:undefined;
          const grappleEffect=outcome.grappleApplied?{id:publishedIceGiantMorsel?`monster-grappled:ice-giant-morsel:${attacker.id}`:`monster-grappled:${attacker.id}`,name:'Agarrado',detail:publishedIceGiantMorsel?'Gigante de Gelo: mordida automática 12D/Dano 3 a cada rodada até escapar com Potência (ação lenta).':`Impedido de se deslocar por ${attacker.name}; escapar exige ação lenta e Força + Potência.`,source:attacker.name}:undefined;
          const entRootsEffect=outcome.entRootsApplied?{id:`official-ent-roots:${attacker.entityId}`,name:'Preso pelas raízes nodosas · Força 3',detail:'Raízes do Ent prenderam a personagem. A R88 não define uma rolagem de fuga; a Mestra pode adjudicar o rompimento ou registrar um resgate externo.',source:attacker.name}:undefined;
          const entEmbraceEffect=outcome.entEmbraceApplied?{id:`official-ent-embrace:${attacker.entityId}`,name:'Preso no corpo do Ent',detail:'A personagem pode ser libertada se um único golpe causar pelo menos 4 dano ao Ent que a prendeu ou se alguém a resgatar de fora.',source:attacker.name}:undefined;
          const drowningEffect=outcome.drowningApplied?{id:`monster-drowning:${attacker.id}`,name:'Afogamento',detail:'Sofre 1 dano de Força no início do próprio turno até ser retirado da água.',source:attacker.name,ongoingDamage:1}:undefined;
          const effects=[...(target.statusEffects||[]).filter(effect=>(!proneEffect||effect.id!=='condition:prone')&&(!grappleEffect||!effect.id.startsWith('monster-grappled:'))&&(!entRootsEffect||!effect.id.startsWith('official-ent-roots:'))&&(!entEmbraceEffect||!effect.id.startsWith('official-ent-embrace:'))&&(!drowningEffect||!effect.id.startsWith('monster-drowning:'))),...(proneEffect?[proneEffect]:[]),...(grappleEffect?[grappleEffect]:[]),...(entRootsEffect?[entRootsEffect]:[]),...(entEmbraceEffect?[entEmbraceEffect]:[]),...(drowningEffect?[drowningEffect]:[]),...(targetPendingTags.length?[{id:`monster-special:${crypto.randomUUID()}`,name:'Efeito especial do monstro pendente',detail:`${attack.name}: resolver para ${targetPendingTags.join(', ')} conforme a entrada oficial.`,source:attacker.name}]:[]),...(pendingCritical?[pendingCritical]:[])];
          combatants=combatants.map(item=>item.id===target.id?{...item,currentStrength:strength,brokenAttribute:strength===0?'strength':item.brokenAttribute==='strength'?undefined:item.brokenAttribute,status:strength===0?'broken' as const:item.status,armor:Math.max(0,item.armor-outcome.armorLoss),conditions:strength===0?Array.from(new Set([...conditions,'Broken'])):conditions,statusEffects:effects}:item);
        }
      }
      let swarmHealing=0;
      if(attack.failureEffect==='damage_on_failure'){
        const currentAttacker=combatants.find(item=>item.id===attacker.id);
        if(currentAttacker&&currentAttacker.currentStrength<currentAttacker.maxStrength){
          swarmHealing=Math.min(Number((attack as {healOnFailure?:number}).healOnFailure)||2,currentAttacker.maxStrength-currentAttacker.currentStrength);
          combatants=combatants.map(item=>item.id===attacker.id?{...item,currentStrength:item.currentStrength+swarmHealing}:item);
        }
      }
      let batSwarmHealing=0;
      if(attack.regenerateStrengthOnDamage){
        const currentAttacker=combatants.find(item=>item.id===attacker.id);
        const inflicted=outcomes.reduce((sum,outcome)=>sum+Math.min(outcome.finalDamage,outcome.target.currentStrength),0);
        if(currentAttacker&&inflicted>0)batSwarmHealing=Math.min(inflicted,currentAttacker.maxStrength-currentAttacker.currentStrength);
        if(batSwarmHealing>0)combatants=combatants.map(item=>item.id===attacker.id?{...item,currentStrength:item.currentStrength+batSwarmHealing}:item);
      }
      if(!outcomes.length&&(specialPending||pendingEffectTags.length)){
        specialPending=true;
      }
      const outcomeText=outcomes.map(({target,roll,finalDamage,armorDice,furiousMightWin,furiousFollowup,witsDamage,fearDamage,fearAttribute,fearPending,fearSummary,coldApplied,poisonSummary,moveFailed,movementSaveFailed,proneApplied,grappleApplied,drowningApplied,entRootsApplied,entEmbraceApplied,automaticPoison,contaminatesFood,saveFailed,poisonTrigger,specialEffect})=>fearAttribute?`${target.name}: medo ${roll.successes}, ${fearDamage} dano em ${fearAttribute}${coldApplied?'; ficou Frio':''}${fearPending?' e ferimento grave de horror pendente':''}`:specialEffect?`${target.name}: teste ${saveFailed?'falhou':'passou'}${saveFailed?specialEffect==='prone'?' e caiu':specialEffect==='slimed'?' e ficou coberto por gosma, −2 em ataques':specialEffect==='avoided'?' e evitou o ataque':specialEffect==='mind_disintegration'?`e sofreu ${witsDamage||0} dano em Astúcia`:specialEffect==='damage_on_failure'?' e sofreu 1 dano de Força':specialEffect==='disease'?' e contraiu doença':specialEffect==='plague_attack'?' e contraiu peste demoníaca com pústulas pendentes':specialEffect==='blinded'?' e ficou Cego':specialEffect==='petrified'?' e virou pedra':specialEffect==='drowning'?' e começou a se afogar':specialEffect==='life_drain'?' e ficou preso no dreno vital':specialEffect==='dragged'?' e foi preso e arrastado':specialEffect==='magic_leech'?' e foi agarrado pelo roubo mágico do Imp':specialEffect==='lead_arms'?' e largou as armas; não pode usar os braços por um Quarto de Dia':specialEffect==='donkey_head'?' e recebeu Cabeça de Burro: sem ações até a próxima iniciativa, sem fala e −3 em Empatia por um Quarto de Dia':specialEffect==='flatulence'?' e o Imp afetou com −2 os testes de perícia dos outros aventureiros próximos por 15 minutos':' e ficou incapacitado':''}`:moveFailed!==undefined?`${target.name}: Movimentação ${moveFailed?'falhou':'evitou a nuvem'}${poisonSummary?`; ${poisonSummary}`:''}`:automaticPoison?`${target.name}: veneno automático ${poisonSummary||'resolvido'}${contaminatesFood ? "; reserva de comida contaminada" : ""}`:saveFailed!==undefined&&['wits_failure','endurance_failure'].includes(poisonTrigger||'')?`${target.name}: salvaguarda ${saveFailed?'falhou':'resistiu'}${poisonSummary?`; ${poisonSummary}`:''}`:`${target.name}: ${roll.successes} sucesso(s), ${finalDamage} ${attack.damageType==='empathy'?'dano em Empatia':`dano após armadura ${armorDice.join(',')||'—'}`}${movementSaveFailed?'; falhou em Movimentação e sofreu o ataque':''}${proneApplied?'; Caído (ação rápida para levantar)':''}${grappleApplied?'; Agarrado (deslocamento bloqueado)':''}${entRootsApplied?'; preso pelas raízes do Ent (Força 3)':''}${entEmbraceApplied?'; preso no corpo do Ent (golpe de 4+ dano ou resgate externo)':''}${drowningApplied?'; submerso (Resiliência no próprio turno':''}${witsDamage?`; +${witsDamage} dano em Astúcia`:''}${fearSummary?`; ${fearSummary}`:''}${poisonSummary?`; ${poisonSummary}`:''}${publishedNanuikFurious?furiousFollowup?'; ataque adicional 12D / dano 3 após Potência resistida':furiousMightWin?'; Potência vencida, sem golpe adicional':' ; sem golpe adicional (não causou dano ou perdeu Potência)':''}`).join('; ');
      const totalHealing=swarmHealing+batSwarmHealing;
      const text=`${attacker.name}: D6 ${dieValue} — ${attack.name}${outcomeText?`; ${outcomeText}`:''}${specialEffectSummary?`; ${specialEffectSummary}`:''}${totalHealing?`; recuperou ${totalHealing} de Força`:''}${specialPending?`; pendência do Mestre: ${pendingEffectTags.join(', ')||'selecionar alvos e aplicar efeito especial'}`:''}.`;
      const updated=this.resolveIfFinished({...encounter,combatants:[...combatants,...summonedUndead],droppedEquipment,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,text,createdAt:now}].slice(-300),updatedAt:now},now);
      let world:WorldState={...current,encounter:updated,updatedAt:now};
      for(const outcome of outcomes){
        if(outcome.target.entityId&&(outcome.finalDamage>0||Number(outcome.witsDamage)>0||Number(outcome.fearDamage)>0))
          world=await BloodmarchBetaChantService.interrupt(tx,world,outcome.target.entityId,'ataque de criatura');
      }
      await store.put(world);
      return{world,attack,dieValue,outcomes,specialPending,summonedUndead};
    });
    this.notify(resolved.world);
    return resolved;
  }

  static characterCombatant(character: Character): EncounterCombatant {
    return {
      id: `character:${character.id}`,
      entityId: character.id,
      name: character.name,
      kind: 'character',
      side: 'party',
      initiative: initiative(),
      currentStrength: Number(character.attributes?.strength?.current || 0),
      brokenAttribute: character.brokenAttribute,
      maxStrength: Number(character.attributes?.strength?.max || character.attributes?.strength?.current || 1),
      armor: this.armorValue(character.armor),
      conditions: [...(character.conditions || [])],
      statusEffects:[...(character.statusEffects||[]).map(effect=>({...effect,name:effect.label})),...(character.drowningState&&!(character.statusEffects||[]).some(effect=>effect.id==='drowning')?[{id:'drowning',name:'Afogamento',detail:`${character.drowningState.phase==='afloat'?'Boiando':character.drowningState.phase==='underwater'?'Submerso: teste de Resiliência no próprio turno':'Afogando: 1 dano de Força por turno'}${character.drowningState.minutesUntilDeath?`; morte em ${character.drowningState.minutesUntilDeath} minuto(s) sem resgate`:''}.`}]:[])],
      fastActionAvailable: true,
      slowActionAvailable: true,
      status: character.status === 'dead' ? 'dead' : Number(character.attributes?.strength?.current || 0) <= 0 ? 'broken' : 'active',
      notes: '',
      tokenId: character.tokenId,
      quickSheet: { kin: character.kin, profession: character.profession, attributes: character.attributes, skills: character.skills, talents: character.talents, willpowerPoints: character.willpowerPoints, weapons: character.weapons, armor: character.armor, gear: character.gear, conditions: character.conditions, brokenAttribute: character.brokenAttribute },
    };
  }

  /** Sangrento, Livro do Mestre p.111: tocha/lanterna ACESA ao alcance das mãos
   * inflige D3 dano adicional de Força. Uma exposição concreta é confirmada
   * pelo Mestre e aplicada UMA vez por fonte/portador/rodada (anti-clique duplo).
   * A regra não estabelece frequência automática; esta ação NÃO infere luz, gasto
   * de equipamento, posição ou bônus. O D3 e o recibo permanecem no combate.
   */
  static async exposeBloodMistDemonToBrightLight(campaignId:string,demonId:string,carrierId:string,gearIndex:number,
    litAndArmsReachConfirmed:boolean,rng:()=>number=Math.random):Promise<{world:WorldState;damage:number}>{
    if(!litAndArmsReachConfirmed)throw new Error('Confirme luz acesa ao alcance das mãos do Sangrento.');
    if(!Number.isInteger(gearIndex)||gearIndex<0)throw new Error('Índice inválido de luz.');
    const result=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const camp=await tx.objectStore('campaign').get(campaignId);
      if(!camp?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const combat=world.encounter;
      if(!combat||combat.status!=='active'||combat.hexId!==camp.currentHexId)
        throw new Error('O combate do Sangrento não está ativo no hex atual.');
      const demon=combat.combatants.find(item=>item.id===demonId&&item.entityId===BloodMistDemonRulesEngine.PROFILE_ID&&item.side==='enemy');
      const carrier=combat.combatants.find(item=>item.entityId===carrierId&&item.kind==='character'&&item.side==='party'&&item.status==='active');
      if(!demon||demon.status!=='active'||!carrier)
        throw new Error('Sangrento ou portador da luz não participa ativamente do combate.');
      const actor=await tx.objectStore('characters').get(carrierId) as Character|undefined;
      EncounterCampaignGuard.assertCharacter(actor,campaignId,'portador da luz',{
        partyRoster:Array.isArray(camp.partyCharacterIds)?camp.partyCharacterIds as string[]:undefined,partySide:true});
      if(actor?.status==='dead'||actor?.brokenAttribute)throw new Error('O portador não está capaz de aproximar a luz.');
      const item=actor.gear?.[gearIndex] as Record<string,unknown>|undefined;
      const label=String(item?.name||item?.label||'');
      if(!/tocha|torch|lanterna|lantern/i.test(label)||Number(item?.quantity??1)<=0)
        throw new Error('O portador não possui tocha ou lanterna válida na posição selecionada.');
      const marker=`bloodmist:brightlight:${combat.round}:${carrierId}:${gearIndex}`;
      if(demon.statusEffects?.some(effect=>effect.id===marker))throw new Error('Esta mesma luz já foi aplicada nesta rodada.');
      const damage=BloodMistDemonRulesEngine.brightLightDamage(true,rng);
      const strength=Math.max(0,demon.currentStrength-damage);
      const now=Date.now();
      const updatedDemon={...demon,currentStrength:strength,status:strength===0?'broken' as const:demon.status,
        brokenAttribute:strength===0?'strength' as const:demon.brokenAttribute,
        conditions:strength===0?Array.from(new Set([...demon.conditions,'Broken'])):demon.conditions,
        statusEffects:[...(demon.statusEffects||[]),{id:marker,name:'Exposição à luz forte',
          detail:`${actor.name}: ${label} acesa ao alcance das mãos, D3=${damage}; dano direto de Força.`}]};
      const combatants=combat.combatants.map(item=>item.id===demonId?updatedDemon:item);
      const updatedCombat=EncounterService.resolveIfFinished({...combat,combatants,
        log:[...combat.log,{id:crypto.randomUUID(),round:combat.round,combatantId:demon.id,
          text:`Sangrento exposto à luz forte de ${actor.name} (${label}): D3=${damage}; Força ${demon.currentStrength} → ${strength}, ignorando armadura.`,createdAt:now}].slice(-300),updatedAt:now},now);
      const updated={...world,encounter:updatedCombat,updatedAt:now};
      await store.put(updated);return {world:updated,damage};
    });this.notify(result.world);return result;
  }

  /** R87: ataque corpo a corpo dos PNJs oficiais #3/#7/#8/#12 e resgate #7.
   * O jogador decide a defesa; o commit canônico aplica ações, armadura,
   * dano, ferimentos e persistência de rolagens em uma transação.
   */
  /** #10 generated attacks are selected from their generated list; no
   * fictitious D6 attack distribution is introduced for the two table rolls.
   */
  static async resolveOfficialGrelfAttack(campaignId:string,attackerId:string,targetId:string,attackIndex:number,rng:()=>number=Math.random){
    const w=await WorldStateService.get(campaignId),combat=w.encounter;
    const actor=combat?.combatants[combat.turnIndex];
    const event=w.journey.events.find(e=>e.id===combat?.startedSceneId&&e.status==='open');
    const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
    const list=actor?.quickSheet?.generatedAttacks as GeneratedDemonAttack[]|undefined;
    const attack=list?.[attackIndex];
    if(!combat||combat.status!=='active'||actor?.id!==attackerId||actor.entityId!=='official-generated-grelf'||
      Number((event?.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==10||
      d?.stage!=='combat_active'||d.combatEncounterId!==combat.id||!attack||!Number.isInteger(attackIndex))
      throw new Error('Ataque gerado de Grelf sem vínculo válido à ocorrência.');
    if(attack.kind==='fear')return this.resolveFearAttack(campaignId,attackerId,targetId,attack.baseDice,rng);
    const roll=ForbiddenLandsDiceEngine.roll({base:attack.baseDice,skill:0,gear:0},{characterId:null,sceneId:combat.startedSceneId||combat.id,checkId:`generated-grelf:${combat.id}:${combat.round}:${attackIndex}`,rng});
    return this.commitBasicAttackResult(campaignId,attackerId,targetId,{attackRoll:roll,weaponDamage:attack.damage,criticalTable:attack.damageType,
      attackKind:'unarmed',fireAttack:attack.kind==='fire',rng});
  }

  static async resolveOfficialNpcMelee(campaignId:string,npcId:string,targetId:string,
    defense:'none'|'dodge'|'block',armsReachConfirmed:boolean,rng:()=>number=Math.random):
    Promise<{world:WorldState;resolution:AttackResolution;critical:boolean;poisonRoll?:DiceRoll}>{
    if(!['none','dodge','block'].includes(defense))throw new Error('Defesa inválida.');
    if(!armsReachConfirmed)throw new Error('Confirme que o alvo está ao alcance das mãos do PNJ.');
    const world=await WorldStateService.get(campaignId);
    const combat=world.encounter;
    if(!combat||combat.status!=='active')throw new Error('Combate inexistente ou encerrado.');
    const campaign=await withTransaction('campaign','readonly',async tx=>tx.store.get(campaignId));
    if(!campaign?.id||!combat.hexId||combat.hexId!==campaign.currentHexId)
      throw new Error('Campanha ou hex não corresponde ao combate do PNJ.');
    const official=world.journey.events.find(e=>e.id===combat.startedSceneId&&e.status==='open');
    const decision=official?.result?.officialSceneDecision as Record<string,unknown>|undefined;
    const encounterNumber=Number((official?.result?.mechanic as Record<string,unknown>|undefined)?.encounterId);
    const isRescue=encounterNumber===7&&decision?.stage==='slaver_rescue_combat_active'&&decision.slaverRescueEncounterId===combat.id;
    if(!official||![3,7,8,12,29,41,42,43].includes(encounterNumber)||
      !(isRescue||(decision?.stage==='combat_active'&&decision.combatEncounterId===combat.id)))
      throw new Error('Ataque de PNJ sem vínculo válido à ocorrência oficial.');
    const npc=combat.combatants[combat.turnIndex];
    if(!npc||npc.id!==npcId||npc.kind!=='npc'||npc.side!=='enemy'||npc.status!=='active'||
      !(npc.entityId?.startsWith('official-slaver:')||npc.entityId?.startsWith('official-orc-escort:')||npc.entityId?.startsWith('official-hungry-bandit:')||npc.entityId==='official-khardnal'||npc.entityId?.startsWith('official-skeleton:')||npc.entityId?.startsWith('official-r88-skeleton:')||npc.entityId?.startsWith('official-r88-soul-merchant-guard:')||npc.entityId?.startsWith('official-r88-saurian-hunter:')||npc.entityId?.startsWith('official-r88-bounty-hunter:')))
      throw new Error('Somente o PNJ oficial ativo pode atacar neste turno.');
    if((encounterNumber===3&&!npc.entityId?.startsWith('official-orc-escort:'))||
      (encounterNumber===7&&!npc.entityId?.startsWith('official-slaver:'))||
      (encounterNumber===8&&npc.entityId!=='official-khardnal')||
      (encounterNumber===12&&!npc.entityId?.startsWith('official-hungry-bandit:'))||
      (encounterNumber===29&&!npc.entityId?.startsWith('official-skeleton:')&&!npc.entityId?.startsWith('official-r88-skeleton:'))||
      (encounterNumber===41&&!npc.entityId?.startsWith('official-r88-soul-merchant-guard:'))||
      (encounterNumber===42&&!npc.entityId?.startsWith('official-r88-saurian-hunter:'))||
      (encounterNumber===43&&!npc.entityId?.startsWith('official-r88-bounty-hunter:')))
      throw new Error('O PNJ não corresponde ao encontro oficial registrado.');
    if(!npc.slowActionAvailable)throw new Error('A ação lenta do PNJ já foi usada.');
    const target=combat.combatants.find(c=>c.id===targetId&&c.side==='party'&&c.kind==='character'&&c.status==='active');
    if(!target?.entityId)throw new Error('Escolha um aventureiro ativo como alvo.');
    const weapons=(npc.quickSheet?.weapons||[]) as Array<Record<string,unknown>>;
    const weapon=weapons[0];
    if(!weapon||!Number.isFinite(Number(weapon.damage))||Number(weapon.damage)<1)
      throw new Error('Arma canônica do PNJ ausente ou inválida.');
    const sceneId=combat.startedSceneId||combat.id;
    const skill=combatantSkill(npc,undefined,['Luta','Melee']);
    const attackRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(npc,undefined,'strength'),skill,gear:0},{
      characterId:null,sceneId,checkId:`official-npc:${combat.id}:${combat.round}:${npc.id}:${target.id}`,
      rng,skillName:'melee',attributeName:'strength'});
    let defenseRoll:DiceRoll|undefined;
    if(defense!=='none'){
      const character=await withTransaction('characters','readonly',async tx=>tx.store.get(target.entityId!)) as Character|undefined;
      if(!character||character.status==='dead')throw new Error('Defensor não encontrado ou morto.');
      const dodge=defense==='dodge';const attribute=dodge?'agility':'strength';
      const defenseSkill=dodge?normalizedSkillRank(character,['Movimentação','Move']):normalizedSkillRank(character,['Luta','Melee']);
      defenseRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes[attribute].current,skill:defenseSkill,gear:0},{
        characterId:character.id,sceneId,checkId:`official-npc-defense:${combat.id}:${combat.round}:${npc.id}:${target.id}`,
        rng,criticalInjuries:character.criticalInjuryStates,skillName:dodge?'move':'melee',attributeName:attribute});
    }
    return this.commitBasicAttackResult(campaignId,npc.id,target.id,{
      attackRoll,defenseRoll,defenseKind:defense==='none'?undefined:defense,
      weaponDamage:Number(weapon.damage),weaponIndex:0,attackKind:'strike',actionCost:'slow',
      criticalTable:weapon.damageType==='slash'?'slash':weapon.damageType==='puncture'?'puncture':'blunt',rng,
    });
  }

  /** R84: alternância explícita de forma do Sangrento durante combate.
   * O livro não define uma frequência fixa de mudança, então não se inventa
   * recarga/ação; o registro fica auditável e ataques consultam este estado.
   */
  static async setBloodMistDemonForm(campaignId:string,demonId:string,form:'mist'|'manifested'):Promise<WorldState>{
    if(form!=='mist'&&form!=='manifested')throw new Error('Forma inválida do Sangrento.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId);
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const combat=world.encounter;
      if(!combat||combat.status!=='active'||combat.hexId!==campaign.currentHexId)
        throw new Error('Combate do Sangrento ausente ou fora do hex atual.');
      const demon=combat.combatants.find(c=>c.id===demonId&&c.entityId===BloodMistDemonRulesEngine.PROFILE_ID&&c.side==='enemy');
      if(!demon||demon.status!=='active')throw new Error('Sangrento ativo não encontrado no combate.');
      const isMist=Boolean(demon.statusEffects?.some(e=>e.id==='bloodmist:incorporeal'));
      if(isMist===(form==='mist'))throw new Error('O Sangrento já está nesta forma.');
      const now=Date.now();const effects=(demon.statusEffects||[]).filter(e=>e.id!=='bloodmist:incorporeal');
      const changed={...demon,statusEffects:form==='mist'?[...effects,{id:'bloodmist:incorporeal',name:'Forma incorpórea',detail:'Imune a ataques físicos; luz forte ainda causa D3 Força.'}]:effects};
      const combatants=combat.combatants.map(c=>c.id===demonId?changed:c);
      const updated={...world,encounter:{...combat,combatants,
        log:[...combat.log,{id:crypto.randomUUID(),round:combat.round,combatantId:demonId,
          text:form==='mist'?'O Sangrento dispersou o corpo e tornou-se névoa incorpórea.':'O Sangrento condensou a névoa em um corpo materializado.',createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  static bestiaryCombatant(entry: BestiaryEntry, ordinal = 1, rng: () => number = Math.random): EncounterCombatant {
    const range=entry.mechanicalData?.strengthRange as number[]|undefined;
    const strength=range?.length===2?range[0]+Math.floor(Math.max(0,Math.min(.999999999,rng()))*(range[1]-range[0]+1)):Number(entry.mechanicalData?.attributes?.strength||1);
    return {
      id: `monster:${entry.id}:${crypto.randomUUID()}`,
      entityId: entry.id,
      name: ordinal > 1 ? `${entry.name} ${ordinal}` : entry.name,
      kind: 'monster',
      side: 'enemy',
      initiative: initiative(),
      currentStrength: strength,
      maxStrength: strength,
      armor: Number(entry.armor || 0),
      conditions: [],
      fastActionAvailable: true,
      slowActionAvailable: true,
      status: 'active',
      notes: (entry.attacks || []).map((attack: any) => `${attack.name}: ${attack.dice || ''}${attack.damage ? `, dano ${attack.damage}` : ''}`).join(' | ').slice(0, 1000),
      tokenId: entry.tokenId,
      quickSheet: { category: entry.category, armor: entry.armor, skills: entry.skills, attacks: entry.attacks, monsterAttackTable:entry.mechanicalData?.monsterAttackTable, standardNpcCombat:entry.mechanicalData?.standardNpcCombat, mechanicalData: entry.mechanicalData },
    };
  }

  static operationCombatant(operation: WorldOperation, index = 0): EncounterCombatant {
    const source = operation.combatants?.[index];
    const strength = Number(source?.strength || operation.strength || 1);
    return {
      id: source?.id || operation.combatantId || `encounter:${crypto.randomUUID()}`,
      entityId: source?.entityId || operation.targetId,
      name: cleanName(source?.name || operation.name || operation.title),
      kind: source?.kind || operation.participantKind || 'monster',
      side: source?.side || operation.side || 'enemy',
      initiative: source?.initiative || operation.initiative || initiative(),
      currentStrength: strength,
      maxStrength: strength,
      armor: Number(source?.armor ?? operation.armor ?? 0),
      conditions: [],
      fastActionAvailable: true,
      slowActionAvailable: true,
      status: 'active',
      notes: source?.notes || operation.detail || operation.description || '',
      tokenId: source?.tokenId,
      x: source?.x, y: source?.y,
      quickSheet: { source: operation.source, rule: operation.reason },
    };
  }

  static createEncounter(title: string, hexId: string | undefined, characters: Character[], enemies: EncounterCombatant[], sceneId?: string): EncounterState {
    const now = Date.now();
    const ordered = [...characters.map(character => this.characterCombatant(character)), ...enemies]
      .sort((a, b) => a.initiative - b.initiative || a.name.localeCompare(b.name, 'pt-BR'));
    const party = ordered.filter(item => item.side === 'party');
    const opposition = ordered.filter(item => item.side !== 'party');
    const combatants = ordered.map(item => {
      const group = item.side === 'party' ? party : opposition;
      const index = group.findIndex(candidate => candidate.id === item.id);
      const span = Math.max(1, group.length);
      return { ...item, x: item.x ?? (item.side === 'party' ? .18 : .82), y: item.y ?? ((index + 1) / (span + 1)), size: item.size ?? 1 };
    });
    return {
      id: crypto.randomUUID(),
      title: title.trim() || 'Encontro',
      hexId,
      status: 'active',
      round: 1,
      turnIndex: 0,
      combatants,
      log: [{ id: crypto.randomUUID(), round: 1, text: 'Encontro iniciado e iniciativa definida.', createdAt: now }],
      startedSceneId: sceneId,
      startedAt: now,
      updatedAt: now,
    };
  }

  static triggerWatchOnEncounter(world:WorldState,encounter:EncounterState,characters:Character[],day:number,period:string,rng:()=>number=Math.random):{world:WorldState;encounter:EncounterState;rolls:DiceRoll[]}{
    const watches=world.journey.events.filter(event=>event.status==='open'&&event.result?.activity==='keepWatch'&&Number(event.result?.watchDay??event.day)===day&&String(event.result?.watchPeriod??event.period)===period);
    if(!watches.length)return {world,encounter,rolls:[]};
    const rolls:DiceRoll[]=[];
    const checkByEvent=new Map<string,{characterId:string;characterName:string;rollId?:string;successes?:number;unavailable?:boolean}>();
    const encounterLog=[...encounter.log];
    for(const watch of watches){
      const watcherId=String(watch.result?.actorId||'');
      const character=characters.find(item=>item.id===watcherId);
      if(!character||character.status==='dead'||!CharacterActionRulesEngine.canAct(character)){
        checkByEvent.set(watch.id,{characterId:watcherId,characterName:character?.name||watcherId||'Vigia',unavailable:true});
        encounterLog.push({id:crypto.randomUUID(),round:encounter.round,text:`A Vigília de ${character?.name||watcherId||'uma personagem'} foi acionada pelo encontro, mas a pessoa não podia agir; não houve teste de PATRULHA.`,createdAt:Date.now()});
        continue;
      }
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:normalizedSkillRank(character,['Scouting','Patrulha']),gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`watch:${watch.id}:encounter:${encounter.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'scouting',attributeName:'wits'});
      rolls.push(roll);
      checkByEvent.set(watch.id,{characterId:character.id,characterName:character.name,rollId:roll.id,successes:roll.successes});
      encounterLog.push({id:crypto.randomUUID(),round:encounter.round,text:`Vigília acionada pelo encontro: ${character.name} rolou PATRULHA e obteve ${roll.successes} sucesso(s).`,createdAt:Date.now()});
    }
    const events=world.journey.events.map(event=>{
      const check=checkByEvent.get(event.id);
      if(!check)return event;
      const checks=[...((event.result?.watchChecks as unknown[]|undefined)||[]),{...check,encounterId:encounter.id,at:Date.now()}];
      return {...event,status:'resolved' as const,result:{...(event.result||{}),watchChecks:checks,triggeredByEncounterId:encounter.id},detail:`${event.detail} Encontro ${encounter.title}: ${check.unavailable?`${check.characterName} não podia agir, então não houve rolagem.`:`${check.characterName} rolou PATRULHA: ${check.successes} sucesso(s).`}`};
    });
    const nextEncounter={...encounter,log:encounterLog,updatedAt:Date.now()};
    return {world:{...world,journey:{...world.journey,events,updatedAt:Date.now()},encounter:nextEncounter,updatedAt:Date.now()},encounter:nextEncounter,rolls};
  }

  static async startManual(campaignId: string, title: string, selections: EncounterEnemySelection[], officialJourneyEventId?: string): Promise<WorldState> {
    const world = await withTransaction(['world_state', 'characters', 'bestiary', 'campaign', 'rolls'], 'readwrite', async tx => {
      const characters = await tx.objectStore('characters').getAll();
      const entries = await tx.objectStore('bestiary').getAll();
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha não encontrada.');
      const enemies: EncounterCombatant[] = [];
      for (const selection of selections) {
        const entry = entries.find(item => item.id === selection.entryId) || BookOfBeastsCatalogService.entries().find(item=>item.id===selection.entryId);
        if (!entry) continue;
        const quantity = Math.max(1, Math.min(20, Math.trunc(selection.quantity || 1)));
        for (let index = 1; index <= quantity; index += 1) enemies.push(this.bestiaryCombatant(entry, index));
      }
      if (!enemies.length) throw new Error('Escolha pelo menos um adversário do bestiário.');
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      if (current.encounter?.status === 'active') throw new Error('Já existe um encontro em andamento.');
      // R80: combates de cenas oficiais exigem vínculo explícito à ocorrência,
      // jamais um combate arbitrário escolhido por título ou pela coincidência do hex.
      const sceneEvent = officialJourneyEventId ? current.journey.events.find(event =>
        event.id === officialJourneyEventId && event.status === 'open' &&
        (event.result?.mechanic as Record<string, unknown> | undefined)?.kind === 'official_random_encounter'
      ) : undefined;
      if (officialJourneyEventId) {
        const encounterNumber=Number((sceneEvent?.result?.mechanic as Record<string,unknown>|undefined)?.encounterId);
        if(encounterNumber===2)
          throw new Error('O encontro nº 2 não possui fichas mecânicas oficiais de Heldemin ou Alachran na base R88. Não substitua os duelistas por criaturas genéricas; retire a escolha antes de executá-la ou aguarde uma ficha sustentada pela fonte.');
        if ([3,4,5,7,8,10,11,12,17,18,20,21,23,24,25,29,30,33,35,37,40,41,42,43].includes(encounterNumber))
          throw new Error('Este encontro oficial exige Iniciar combate oficial para aplicar os perfis e a quantidade canônicos; escolta orc também exige o vínculo explícito.');
        const decision=sceneEvent?.result?.officialSceneDecision as Record<string, unknown> | undefined;
        if (!sceneEvent || sceneEvent.hexId !== campaign.currentHexId ||
          decision?.stage !== 'pending_external_mechanics' || decision.pending !== 'combat' ||
          !decision.choiceId || decision.combatEncounterId)
          throw new Error('Vínculo inválido: a ocorrência não possui combate oficial aguardando execução neste hex.');
      }
      // Nunca criar combate desvinculado enquanto há uma decisão oficial de combate.
      if(current.journey.events.some(event => event.status === 'open' &&
        ['in_combat','combat_active','blood_mist_combat_active','teramalda_spike_removed'].includes(String((event.result?.officialSceneDecision as Record<string, unknown> | undefined)?.stage))))
        throw new Error('Conclua o combate oficial vinculado à jornada antes de iniciar outro encontro.');
      const pendingCombat=current.journey.events.filter(event => event.status === 'open' &&
        (event.result?.officialSceneDecision as Record<string, unknown> | undefined)?.stage === 'pending_external_mechanics' &&
        ((event.result?.officialSceneDecision as Record<string, unknown> | undefined)?.pending === 'combat' ||
        (Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===7 &&
         (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='help')));
      if (pendingCombat.length && !sceneEvent)
        throw new Error('Vincule o combate à ocorrência oficial pendente para não perder consequências.');
      const members=CampaignPartyService.members(campaign,characters);
      const party=sceneEvent?members.filter(character=>character.status!=='dead'):members;
      if(sceneEvent && !party.length)throw new Error('A campanha não possui heróis ativos para este combate.');
      const encounter=this.createEncounter(title,campaign.currentHexId||undefined,party,enemies,sceneEvent?.id);
      const triggered=this.triggerWatchOnEncounter(current,encounter,party,Number(campaign.currentDay||1),String(campaign.currentPeriod||'Morning'));
      for(const roll of triggered.rolls)await tx.objectStore('rolls').put(roll);
      const now=Date.now();
      const bound=sceneEvent?{
        ...triggered.world,journey:{...triggered.world.journey,
          events:triggered.world.journey.events.map(event => event.id===sceneEvent.id?{
            ...event,result:{...(event.result||{}),officialSceneDecision:{
              ...((event.result?.officialSceneDecision as Record<string,unknown>)||{}),
              stage:'in_combat',combatEncounterId:encounter.id,combatStartedAt:now,
              automaticCombatStarted:false,combatStartedViaExistingEngine:true,
            }},
          }:event),updatedAt:now},updatedAt:now,
      }:triggered.world;
      await store.put(bound);
      return bound;
    });
    this.notify(world);
    return world;
  }

  static async nextTurn(campaignId: string): Promise<WorldState> {
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const worldState=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(!worldState.encounter||worldState.encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const departingWyrm=worldState.encounter.combatants[worldState.encounter.turnIndex];
      if(departingWyrm?.quickSheet?.pendingOfficialProcessionEntAttack||departingWyrm?.quickSheet?.pendingOfficialMonsterAttack)
        throw new Error('Selecione os alvos e resolva o resultado D6 pendente do monstro antes de avançar o turno.');
      if(departingWyrm?.entityId==='bloodmarch:fire-wyrm'&&departingWyrm.status==='active'&&
         !departingWyrm.statusEffects?.some(effect=>effect.id===`bloodmarch:fire-wyrm-aura:${worldState.encounter!.round}`))
        throw Error('Serpe: resolva a aura térmica deste turno, com todos os alcances confirmados, antes de avançar.');
      const now=Date.now();
      const characters = await tx.objectStore('characters').getAll();
      const characterStore=tx.objectStore('characters');
      const rollStore=tx.objectStore('rolls');
      const checkedWithDeathSync = {
        ...worldState.encounter,
        combatants: worldState.encounter.combatants.map(combatant => {
          if (combatant.kind !== 'character' || !combatant.entityId) return combatant;
          const character = characters.find(item => item.id === combatant.entityId);
          // Time advancement removes expired Bloodshock from the sheet; never leave a stale combat lock.
          const hasBloodshock=character?.statusEffects?.some(effect=>effect.id==='magic:bm-bloodshock');
          const currentCombatant=hasBloodshock?combatant:{...combatant,statusEffects:(combatant.statusEffects||[]).filter(effect=>effect.id!=='magic:bm-bloodshock')};
          return character?.status === 'dead' ? { ...currentCombatant, status: 'dead' as const, fastActionAvailable: false, slowActionAvailable: false } : currentCombatant;
        }),
      };
      // A singer who died, became Broken or lost the ability to speak cannot
      // complete the pending turn. Reconcile BEFORE advancing round clocks.
      let chantChecked=checkedWithDeathSync;
      for(const singer of [...chantChecked.combatants]){
        if(singer.kind!=='character'||!singer.entityId||!BloodmarchBetaSleepEngine.chanting(singer.statusEffects))continue;
        const sheet=characters.find(c=>c.id===singer.entityId);
        if(singer.status==='active'&&sheet?.status!=='dead'&&!sheet?.brokenAttribute&&
          !sheet?.statusEffects?.some(effect=>effect.speechBlocked))continue;
        const corrected=await BloodmarchBetaChantService.interrupt(tx,{...worldState,encounter:chantChecked},singer.entityId,'incapacitação antes da próxima rodada');
        if(corrected.encounter)chantChecked=corrected.encounter;
      }
      // Summoned servants vanish when their caster is Broken or the 15-minute clock expires.
      const dissipated=new Set(BitterReachElementalEngine.expired(chantChecked,Number(worldState.magicElapsedMinutes||0)));
      const activeTurnId=chantChecked.combatants[chantChecked.turnIndex]?.id;
      const validCombatants=chantChecked.combatants.filter(item=>!dissipated.has(item.id));
      const checked = this.resolveIfFinished({...chantChecked,combatants:validCombatants,
        turnIndex:Math.max(0,validCombatants.findIndex(item=>item.id===activeTurnId)),
        log:dissipated.size?[...chantChecked.log,{id:crypto.randomUUID(),round:chantChecked.round,text:`${dissipated.size} elemental(is) dissipado(s) por fim do vínculo com o conjurador.`,createdAt:now}].slice(-300):chantChecked.log}, now);
      if (checked.status === 'resolved' || !checked.combatants.length) {
        const next={...worldState,encounter:checked,updatedAt:now};await store.put(next);return next;
      }
      const activeIndexes = checked.combatants.map((item, index) => turnEligible(item) ? index : -1).filter(index => index >= 0);
      if (!activeIndexes.length) {
        const next={...worldState,encounter:this.resolveIfFinished(checked, now, 'Não restaram combatentes ativos.'),updatedAt:now};
        await store.put(next);return next;
      }
      let nextIndex = checked.turnIndex;
      let round = checked.round;
      for (let attempt = 0; attempt < checked.combatants.length; attempt += 1) {
        nextIndex += 1;
        if (nextIndex >= checked.combatants.length) {
          nextIndex = 0;
          round += 1;
        }
        if (turnEligible(checked.combatants[nextIndex])) break;
      }
      const newRound = round !== checked.round;
      const departing=checked.combatants[checked.turnIndex];
      const concentrationBroken=departing?.kind==='character'&&departing.entityId&&
        (!departing.fastActionAvailable||!departing.slowActionAvailable);
      const mentalWorld=concentrationBroken?BloodmarchBetaMentalSceneEngine.breakConcentration(worldState,departing.entityId!):worldState;
      const timedMentalWorld=newRound?BloodmarchBetaActiveWorldEngine.advanceRounds(BloodmarchBetaMentalSceneEngine.advanceRounds(mentalWorld,1),1):mentalWorld;
      let combatants = newRound
        ? checked.combatants.map(item => ({ ...item, fastActionAvailable: turnEligible(item), slowActionAvailable: turnEligible(item), freeDodgeAvailable: turnEligible(item), statusEffects:(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-defender-free-block:')&&!effect.id.startsWith('core-defender-action-block:')&&!effect.id.startsWith('core-footwork-free-dodge:')&&!effect.id.startsWith('core-footwork-action-dodge:')&&!effect.id.startsWith('core-fast-shooter-run:')&&!effect.id.startsWith('core-ranged-prepared:')) }))
        : checked.combatants;
      if (newRound && combatants.some(item => item.nextInitiative !== undefined)) {
        combatants = combatants
          .map(item => item.nextInitiative === undefined ? item : ({ ...item, initiative: item.nextInitiative, nextInitiative: undefined }))
          .sort((a, b) => a.initiative - b.initiative);
        nextIndex = Math.max(0, combatants.findIndex(item => turnEligible(item)));
      }
      const automaticRoundLogs: EncounterState['log'] = [];
      let nextBaseWorld=timedMentalWorld;
      let officialEncounterState=checked.officialEncounter;
      if(newRound&&checked.officialEncounter?.encounterId===37&&checked.officialEncounter.reinforcementsPending===true&&
        round>=Number(checked.officialEncounter.reinforcementsAtRound||Number.MAX_SAFE_INTEGER)){
        const arrived=OfficialEncounterNpcFactory.roadBlockadeDwarves(4,3);
        combatants=[...combatants,...arrived].sort((a,b)=>a.initiative-b.initiative);
        nextIndex=Math.max(0,combatants.findIndex(item=>turnEligible(item)));
        officialEncounterState={...checked.officialEncounter,reinforcementsPending:false};
        automaticRoundLogs.push({id:crypto.randomUUID(),round,text:'A trombeta do bloqueio foi ouvida: os três guerreiros anões de reforço chegaram.',createdAt:now});
        const events=nextBaseWorld.journey.events.map(event=>event.id!==checked.startedSceneId?event:{
          ...event,detail:`${event.detail} Três reforços anões chegaram após ${round-1} rodada(s).`,
          result:{...(event.result||{}),officialSceneDecision:{...((event.result?.officialSceneDecision as Record<string,unknown>|undefined)||{}),
            dwarfReinforcementsAdded:true,dwarfReinforcementsArrivedAtRound:round,dwarfReinforcementCount:3}},
        });
        nextBaseWorld={...nextBaseWorld,journey:{...nextBaseWorld.journey,events,updatedAt:now}};
      }
      if(newRound){
        const characterStore=tx.objectStore('characters');
        // R36: atomic Bloodmarch beta round clock and Wits recovery, including
        // combatant status, sheet and incoming-metal-attack modifiers.
        for(const combatant of [...combatants]){
          const index=characters.findIndex(item=>item.id===combatant.entityId);
          if(index>=0){
            const before=characters[index];
            const after=BloodmarchBetaRoundEngine.advanceTimedEffects(before,1,1/6);
            if(JSON.stringify(after)!==JSON.stringify(before)){
              characters[index]=after;await characterStore.put(after);
              combatants=combatants.map(item=>item.id===combatant.id?{
                ...item,statusEffects:(item.statusEffects||[]).filter(effect=>!['magic:bm-confusion','magic:bm-attract','magic:bm-lullaby','magic:bm-sand-sleep'].includes(effect.id))
                  .concat((after.statusEffects||[]).filter(effect=>['magic:bm-confusion','magic:bm-attract','magic:bm-lullaby','magic:bm-sand-sleep'].includes(effect.id)).map(effect=>({
                    id:effect.id,name:effect.label,detail:effect.detail,remainingRounds:effect.remainingRounds,
                    confusionRecoveryTo:effect.confusionRecoveryTo,incomingMetalAttackModifier:effect.incomingMetalAttackModifier,sleepPendingMinutes:effect.sleepPendingMinutes,remainingMinutes:effect.remainingMinutes,
                    sleepCasterId:effect.sleepCasterId,sleepOnlyCasterCanWake:effect.sleepOnlyCasterCanWake
                  }))),
                brokenAttribute:after.brokenAttribute,conditions:[...after.conditions],
                status:after.status==='dead'?'dead' as const:after.brokenAttribute?'broken' as const:'active' as const,
                quickSheet:{...(item.quickSheet||{}),attributes:after.attributes,conditions:after.conditions,brokenAttribute:after.brokenAttribute}
              }:item);
            }
          }else{
            const after=BloodmarchBetaRoundEngine.advanceCombatant(combatant,1,1/6);
            if(after!==combatant)combatants=combatants.map(item=>item.id===combatant.id?after:item);
          }
        }
        // Bloodmarch p.111: victims inside the wyrm's mouth lose Strength
        // each round, with no repeated hit or armor roll. Remove the hold if
        // wyrm is defeated. All writes share nextTurn's IndexedDB transaction.
        for(const victim of [...combatants]){
          const mouth=(victim.statusEffects||[]).find(effect=>effect.id.startsWith('monster-grappled:fire-wyrm-mouth:'));
          if(!mouth)continue;
          const monsterId=mouth.id.slice('monster-grappled:fire-wyrm-mouth:'.length);
          const wyrm=combatants.find(item=>item.id===monsterId&&
            ['bloodmarch:fire-wyrm','bloodmarch:kton'].includes(item.entityId)&&item.status==='active');
          const index=characters.findIndex(item=>item.id===victim.entityId);
          const sheet=index>=0?characters[index]:undefined;
          // A Broken victim is not automatically freed from the creature.
          const valid=Boolean(wyrm&&['active','broken'].includes(victim.status));
          const remainingEffects=(victim.statusEffects||[]).filter(effect=>effect.id!==mouth.id);
          if(!valid){
            if(sheet){const updated={...sheet,statusEffects:(sheet.statusEffects||[]).filter(effect=>effect.id!==mouth.id)};
              characters[index]=updated;await characterStore.put(updated);}
            combatants=combatants.map(c=>c.id===victim.id?{...c,statusEffects:remainingEffects}:c);
            continue;
          }
          const damage=wyrm!.entityId==='bloodmarch:fire-wyrm'?3:1;
          const before=sheet?.attributes.strength.current??victim.currentStrength;
          const remaining=Math.max(0,before-damage),broke=before>0&&remaining===0;
          const conditions=broke?Array.from(new Set([...(sheet?.conditions||victim.conditions||[]),'Broken' as const])):(sheet?.conditions||victim.conditions||[]);
          if(sheet){
            const updated={...sheet,attributes:{...sheet.attributes,strength:{...sheet.attributes.strength,current:remaining}},conditions,
              brokenAttribute:broke?'strength' as const:sheet.brokenAttribute,
              statusEffects:(sheet.statusEffects||[]).filter(effect=>effect.id!==mouth.id).concat(remaining>0?[{id:mouth.id,label:mouth.name,detail:mouth.detail}]:[]),
              ...(broke?{criticalInjuries:[...(sheet.criticalInjuries||[]),'PENDENTE: ferimento grave — boca da Serpe']}:{})};
            characters[index]=updated;await characterStore.put(updated);
          }
          combatants=combatants.map(c=>c.id===victim.id?{...c,currentStrength:remaining,conditions,
            status:broke?'broken' as const:c.status,brokenAttribute:broke?'strength' as const:c.brokenAttribute,
            statusEffects:remaining>0?[...remainingEffects,mouth]:remainingEffects,
            quickSheet:{...(c.quickSheet||{}),...(sheet?{attributes:characters[index].attributes}: {})}}:c);
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:victim.id,
            text:`${victim.name} dentro da boca de ${wyrm!.name}: ${damage} de Força sem nova rolagem.${broke?' Quebrado(a), ferimento grave pendente.':''}`,createdAt:now});
        }
        // The Bitter Reach p. 99 — Gigante de Gelo mantém a vítima entre
        // os dentes: um ataque NOVO de 12D, Dano 3 por rodada, enquanto
        // ambos estiverem ativos e a vítima não tiver escapado (marcador).
        // Persistir dano, armadura, dados e remoção do marcador num único tx.
        for(const combatant of [...combatants]){
          const hold=(combatant.statusEffects||[]).find(effect=>effect.id.startsWith('monster-grappled:ice-giant-morsel:'));
          if(!hold)continue;
          const attackerId=hold.id.slice('monster-grappled:ice-giant-morsel:'.length);
          const giant=combatants.find(item=>item.id===attackerId&&item.entityId==='bitter-ice-giant');
          const valid=giant?.status==='active'&&combatant.status==='active';
          const index=characters.findIndex(item=>item.id===combatant.entityId);
          const character=index>=0?characters[index]:undefined;
          const effectList=(combatant.statusEffects||[]).filter(effect=>effect.id!==hold.id);
          if(!valid){
            if(character){const next={...character,statusEffects:(character.statusEffects||[]).filter(effect=>effect.id!==hold.id)};characters[index]=next;await characterStore.put(next);}
            combatants=combatants.map(item=>item.id===combatant.id?{...item,statusEffects:effectList}:item);
            continue;
          }
          const bite=ForbiddenLandsDiceEngine.roll({base:12,skill:0,gear:0},{characterId:null,sceneId:checked.startedSceneId||checked.id,checkId:`bitter-giant-ongoing-bite:${checked.id}:${round}:${attackerId}:${combatant.id}`});
          await rollStore.put(bite);
          const damage=CombatRulesEngine.resolveAttack({attackSuccesses:bite.successes,weaponDamage:3,armorValue:combatant.armor});
          const strength=character?.attributes.strength.current??combatant.currentStrength;
          const remaining=Math.max(0,strength-damage.finalDamage);
          const broke=remaining===0&&strength>0;
          const conditions=broke?Array.from(new Set([...(character?.conditions||combatant.conditions||[]),'Broken' as const])):(character?.conditions||combatant.conditions||[]);
          const effects=remaining>0?[...effectList,hold]:effectList;
          if(character){
            const injuryMarker='PENDENTE: ferimento grave pela mordida do Gigante de Gelo';
            const next={...character,attributes:{...character.attributes,strength:{...character.attributes.strength,current:remaining}},conditions,brokenAttribute:remaining===0?'strength' as const:character.brokenAttribute,statusEffects:(character.statusEffects||[]).filter(effect=>effect.id!==hold.id).concat(remaining>0?[{id:hold.id,label:hold.name,detail:hold.detail}]:[]),...(broke&&!character.criticalInjuries?.includes(injuryMarker)?{criticalInjuries:[...(character.criticalInjuries||[]),injuryMarker]}:{})};
            characters[index]=next;
            await characterStore.put(next);
          }
          combatants=combatants.map(item=>item.id===combatant.id?{...item,currentStrength:remaining,brokenAttribute:remaining===0?'strength' as const:item.brokenAttribute,status:remaining===0?'broken' as const:item.status,armor:Math.max(0,item.armor-damage.armorLoss),conditions,statusEffects:effects,quickSheet:{...(item.quickSheet||{}),...(character?{attributes:characters[index].attributes}: {})}}:item);
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${giant!.name}: mordida contínua 12D, ${damage.finalDamage} dano em ${combatant.name} (armadura ${damage.armorDice.join(',')||'—'}).${broke?' Personagem Quebrado.':''}`,createdAt:now});
        }
        // The Bitter Reach p.109: o Wendigo arranca os chifres na rodada seguinte.
        // Persistente, idempotente por marker removido dentro da mesma transação.
        for (const combatant of [...combatants]) {
          const antlers=(combatant.statusEffects||[]).find(effect=>effect.id.startsWith('bitter-wendigo-antlers:'));
          if (!antlers) continue;
          const monster=combatants.find(item=>item.id===antlers.source);
          const triggered=monster?.status==='active'&&combatant.status==='active';
          const victimIndex=characters.findIndex(item=>item.id===combatant.entityId);
          const victim=victimIndex>=0?characters[victimIndex]:undefined;
          const damage=triggered?2:0;
          let nextStrength=Math.max(0,combatant.currentStrength-damage);
          let brokenAttribute=combatant.brokenAttribute;
          let nextConditions=combatant.conditions;
          if(victim){
            nextStrength=Math.max(0,victim.attributes.strength.current-damage);
            const broken=damage>0&&victim.attributes.strength.current>0&&nextStrength===0;
            brokenAttribute=nextStrength===0?'strength':victim.brokenAttribute;
            nextConditions=broken?Array.from(new Set([...victim.conditions,'Broken' as const])):victim.conditions;
            const marker='PENDENTE: ferimento grave pela retirada dos chifres do Wendigo';
            const updated={...victim,attributes:{...victim.attributes,strength:{...victim.attributes.strength,current:nextStrength}},conditions:nextConditions,brokenAttribute,statusEffects:(victim.statusEffects||[]).filter(effect=>effect.id!==antlers.id),...(broken&&!victim.criticalInjuries?.includes(marker)?{criticalInjuries:[...(victim.criticalInjuries||[]),marker]}:{})};
            characters[victimIndex]=updated;
            await characterStore.put(updated);
          }
          combatants=combatants.map(item=>item.id===combatant.id?{...item,currentStrength:nextStrength,brokenAttribute,conditions:nextConditions,status:nextStrength===0?'broken' as const:item.status,statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==antlers.id)}:item);
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:triggered?`${monster!.name} arrancou os chifres de ${combatant.name}: 2 dano automático em Força.`:`O efeito dos chifres do Wendigo foi encerrado sem dano (Wendigo ou vítima incapacitado).`,createdAt:now});
        }
        for (const combatant of [...combatants]) {
          if (combatant.kind !== 'character' || !combatant.entityId) continue;
          const characterIndex = characters.findIndex(item => item.id === combatant.entityId);
          const character = characters[characterIndex];
          if (!character?.poisonState?.active || character.status === 'dead') continue;
          const result = PoisonRulesEngine.resolveRound(character);
          characters[characterIndex] = result.character;
          await characterStore.put(result.character);
          const lethalBreak = result.character.poisonState?.criticalPending === true;
          const criticalPending = lethalBreak
            ? [{ id: `critical-pending:poison:${crypto.randomUUID()}`, name: 'Ferimento grave pendente', detail: 'Veneno letal Quebrou Força; resolver dano atípico conforme ferimentos graves.', criticalTable: 'other' as const }]
            : [];
          combatants = combatants.map(item => item.id === combatant.id ? {
            ...item,
            currentStrength: result.character.attributes.strength.current,
            brokenAttribute: result.character.brokenAttribute,
            status: result.character.brokenAttribute ? 'broken' as const : item.status,
            conditions: [...result.character.conditions],
            quickSheet: { ...(item.quickSheet || {}), attributes: result.character.attributes, conditions: result.character.conditions, brokenAttribute: result.character.brokenAttribute },
            ...(lethalBreak ? { statusEffects: [...(item.statusEffects || []).filter(effect => !effect.id.startsWith('critical-pending:')), ...criticalPending] } : {}),
          } : item);
          automaticRoundLogs.push({ id: crypto.randomUUID(), round, combatantId: combatant.id, text: `${combatant.name}: veneno ${character.poisonState.kind} causou ${result.damageApplied ? `1 dano em ${result.attribute}` : 'nenhum dano adicional'}${result.broke ? '; atributo Quebrado e efeito encerrado' : ''}${lethalBreak ? '; dano atípico grave pendente' : ''}.`, createdAt: now });
        }
        for(const combatant of [...combatants]){
          if(combatant.kind!=='character'||!combatant.entityId||!combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-plague-boils:')))continue;
          const index=characters.findIndex(item=>item.id===combatant.entityId);
          const character=characters[index];
          if(!character||character.status==='dead')continue;
          const strength={...character.attributes.strength,current:Math.max(0,character.attributes.strength.current-1)};
          const broke=character.attributes.strength.current>0&&strength.current===0;
          const conditions=strength.current===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
          const brokenAttribute=strength.current===0?'strength':character.brokenAttribute==='strength'?undefined:character.brokenAttribute;
          const criticalMarker='PENDENTE: ferimento grave causado pelas pústulas da peste demoníaca';
          const pending=broke?{id:`critical-pending:plague:${combatant.id}:${round}`,label:'Ferimento grave pendente',detail:'Pústulas demoníacas Quebraram Força; resolver dano atípico.',criticalTable:'other' as const}:undefined;
          const statusEffects=[...(character.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-plague-boils:')&&!(pending&&effect.id.startsWith('critical-pending:plague:'))),...(pending?[pending]:[])];
          const updated={...character,attributes:{...character.attributes,strength},conditions,brokenAttribute,statusEffects,...(broke&&!(character.criticalInjuries||[]).includes(criticalMarker)?{criticalInjuries:[...(character.criticalInjuries||[]),criticalMarker]}:{})};
          characters[index]=updated;
          await characterStore.put(updated);
          combatants=combatants.map(item=>item.id!==combatant.id?item:{...item,currentStrength:strength.current,brokenAttribute,status:strength.current===0?'broken' as const:item.status,conditions,statusEffects:[...(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-plague-boils:')&&!(pending&&effect.id.startsWith('critical-pending:plague:'))),...(pending?[{id:pending.id,name:pending.label,detail:pending.detail,criticalTable:pending.criticalTable}]:[])],quickSheet:{...(item.quickSheet||{}),attributes:updated.attributes,conditions,brokenAttribute}});
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: as pústulas da peste demoníaca explodiram e causaram 1 dano de Força${broke?'; Força Quebrada, ferimento grave pendente':''}.`,createdAt:now});
        }
        for(const combatant of [...combatants]){
          const fire=(combatant.statusEffects||[]).find(effect=>effect.id==='burning');
          if(!fire||combatant.status==='dead'||combatant.status==='fled'||combatant.status==='yielded')continue;
          const tick=FireRulesEngine.advance({id:fire.id,source:fire.source||'fogo',damagePerRound:Number(fire.ongoingDamage||1),rounds:Number(fire.ongoingRounds||0)});
          const previous=combatant.currentStrength;
          const protectedPerson=BloodmarchBetaFireGuard.immune(combatant.statusEffects)
            ||BloodmarchBetaFireGuard.immune(characters.find(c=>c.id===combatant.entityId)?.statusEffects);
          const fireDamage=protectedPerson?0:tick.damage;
          const currentStrength=Math.max(0,previous-fireDamage);
          const broke=previous>0&&currentStrength===0;
          const effects=(combatant.statusEffects||[]).map(effect=>effect.id==='burning'?{...effect,ongoingRounds:tick.state.rounds,detail:`${effect.source||'Fogo'}: ${tick.state.rounds} rodada(s); ${tick.damage} dano por rodada até apagar.`}:effect);
          let conditions=combatant.conditions||[];
          let brokenAttribute=combatant.brokenAttribute;
          let status:EncounterParticipantStatus=currentStrength===0?'broken':combatant.status;
          if(combatant.kind==='character'&&combatant.entityId){
            const index=characters.findIndex(item=>item.id===combatant.entityId);
            const character=characters[index];
            if(character){
              const strength={...character.attributes.strength,current:Math.min(character.attributes.strength.current,currentStrength)};
              conditions=strength.current===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
              brokenAttribute=strength.current===0?'strength':character.brokenAttribute;
              const updated={...character,attributes:{...character.attributes,strength},conditions,...(brokenAttribute?{brokenAttribute}:{}),...(broke?{criticalInjuries:[...(character.criticalInjuries||[]),'PENDENTE: ferimento grave — dano atípico de fogo']}: {})};
              characters[index]=updated;
              await characterStore.put(updated);
            }
          }
          combatants=combatants.map(item=>item.id===combatant.id?{
            ...item,currentStrength,conditions,brokenAttribute,status,
            quickSheet:{...(item.quickSheet||{}),attributes:item.kind==='character'&&item.entityId?characters.find(character=>character.id===item.entityId)?.attributes:item.quickSheet?.attributes,conditions,brokenAttribute},
            statusEffects:[...effects,...(broke?[{id:`critical-pending:fire:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'O fogo Quebrou Força; resolva dano atípico.',criticalTable:'other' as const}]:[])],
          }:item);
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: fogo causou ${fireDamage} dano${protectedPerson?' (Amigo do Fogo; equipamentos não protegidos)':''}${broke?'; Força Quebrada, dano atípico pendente':''}.`,createdAt:now});
        }
        // The Bitter Reach p.23: SUFFOCATE ticks at the START of each subsequent
        // round, for exactly Power Level rounds. Death on becoming Broken is
        // scheduled with a real saved D6; healing Strength cancels the timer.
        for(const combatant of [...combatants]){
          const suffocate=(combatant.statusEffects||[]).find(effect=>effect.id==='magic:br-suffocate');
          const death=(combatant.statusEffects||[]).find(effect=>effect.id==='magic:suffocate-death');
          if(!suffocate&&!death)continue;
          const index=characters.findIndex(character=>character.id===combatant.entityId);
          const character=index>=0?characters[index]:undefined;
          const currentStrength=character?.attributes.strength.current??combatant.currentStrength;
          let outcome;
          if(death&&currentStrength===0&&death.expiresRound!==undefined&&round>=death.expiresRound){
            outcome=BitterReachOngoingSpellEngine.tickSuffocate({strength:currentStrength,roundsRemaining:0,round,deathRound:death.expiresRound});
          }else if(suffocate){
            const rollsRemaining=Math.max(0,Number(suffocate.ongoingRounds)||0);
            let deathFace=1;
            if(currentStrength===1&&rollsRemaining>0){
              const deathRoll=ForbiddenLandsDiceEngine.roll({base:1,skill:0,gear:0},
                {characterId:character?.id||null,sceneId:checked.startedSceneId||checked.id,
                  checkId:`bitter-suffocate-death:${checked.id}:${round}:${combatant.id}`});
              await rollStore.put(deathRoll);
              deathFace=deathRoll.results[0]?.value||1;
            }
            outcome=BitterReachOngoingSpellEngine.tickSuffocate({strength:currentStrength,roundsRemaining:rollsRemaining,round,rng:()=>((deathFace-1)+.5)/6});
            // Breaking does not cancel the spell's remaining rounds. Preserve
            // the D6 deadline until a successful HEAL restores Strength.
            if(death&&currentStrength===0&&death.expiresRound!==undefined)
              outcome={...outcome,deathRound:death.expiresRound};
          }else if(death){
            outcome=BitterReachOngoingSpellEngine.tickSuffocate({strength:currentStrength,roundsRemaining:0,round,deathRound:death.expiresRound});
          }else continue;
          let effects=(combatant.statusEffects||[]).filter(effect=>effect.id!=='magic:br-suffocate'&&effect.id!=='magic:suffocate-death');
          if(suffocate&&!outcome.expired){effects.push({...suffocate,ongoingRounds:outcome.roundsRemaining,ongoingDamage:1,speechBlocked:true,
            detail:`Sufocar: ${outcome.roundsRemaining} rodada(s) restantes; sem fala.`});}
          if(outcome.deathRound&&!outcome.dead&&outcome.strength===0){effects.push({id:'magic:suffocate-death',name:'Morte por sufocamento',
            detail:`Sem socorro de Cura, morrerá na rodada ${outcome.deathRound}.`,expiresRound:outcome.deathRound});}
          if(outcome.brokenNow){effects.push({id:`critical-pending:suffocate:${checked.id}:${round}:${combatant.id}`,name:'Ferimento grave pendente',
            detail:'Sufocar Quebrou Força; socorro de Cura necessário.',criticalTable:'other' as const});}
          const conditions=outcome.strength===0?Array.from(new Set([...combatant.conditions,'Broken'])):combatant.conditions;
          if(character){
            let charEffects=(character.statusEffects||[]).filter(effect=>effect.id!=='magic:br-suffocate'&&effect.id!=='magic:suffocate-death');
            for(const effect of effects.filter(effect=>effect.id==='magic:br-suffocate'||effect.id==='magic:suffocate-death')){
              charEffects=[...charEffects,{id:effect.id,label:effect.name,detail:effect.detail,
                ...(effect.ongoingRounds?{ongoingRounds:effect.ongoingRounds}:{}),
                ...(effect.ongoingDamage?{ongoingDamage:effect.ongoingDamage}:{}),
                ...(effect.speechBlocked?{speechBlocked:true}:{}),
                ...(effect.expiresRound?{expiresRound:effect.expiresRound}:{})}];
            }
            const nextConditions=outcome.strength===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
            const updated:Character={...character,attributes:{...character.attributes,strength:{...character.attributes.strength,current:outcome.strength}},
              conditions:nextConditions,statusEffects:charEffects,
              ...(outcome.strength===0?{brokenAttribute:'strength' as const}:{}),
              ...(outcome.dead?{status:'dead' as const}:{}),
              ...(outcome.brokenNow?{criticalInjuries:[...(character.criticalInjuries||[]),'PENDENTE: dano atípico de Sufocar']}:{}),
            };
            characters[index]=updated;await characterStore.put(updated);
          }
          combatants=combatants.map(item=>item.id===combatant.id?{...item,currentStrength:outcome.strength,
            conditions,statusEffects:effects,status:outcome.dead?'dead' as const:outcome.strength===0?'broken' as const:item.status,
            brokenAttribute:outcome.strength===0?'strength' as const:item.brokenAttribute,
            quickSheet:{...(item.quickSheet||{}),...(character?{attributes:characters[index].attributes}:{}),conditions}}:item);
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,
            text:outcome.dead?`${combatant.name}: morreu por sufocamento sem Cura.`:
              outcome.damage?`${combatant.name}: Sufocar causou 1 dano em Força; restam ${outcome.roundsRemaining} rodada(s)${outcome.deathRound?`; morte em D6 rodadas se não houver Cura (rodada ${outcome.deathRound})`:''}.`:
              `${combatant.name}: efeito de Sufocar ${outcome.expired?'encerrado':'permanece ativo'}.`,createdAt:now});
        }
        // The Bitter Reach p.20: persistent Harpooner bleeding is resolved once per round.
        for (const combatant of [...combatants]) {
          const bleed = (combatant.statusEffects || []).find(effect => effect.id.startsWith('bitter-harpoon-bleed:'));
          if (!bleed || combatant.status === 'dead' || combatant.status === 'fled' || combatant.status === 'yielded') continue;
          const nextStrength = Math.max(0, combatant.currentStrength - 1);
          const broke = combatant.currentStrength > 0 && nextStrength === 0;
          const deathRound = broke ? round + (1 + Math.floor(Math.random() * 6)) * 6 : bleed.expiresRound;
          const statusEffects = (combatant.statusEffects || []).map(effect => effect.id === bleed.id ? {
            ...effect, ongoingRounds: Number(effect.ongoingRounds || 0) + 1,
            ...(deathRound ? { expiresRound: deathRound } : {}),
            detail: broke ? 'Sangramento: Força Quebrada. Morte após D6 minutos se Cura não interromper.' : 'Sangramento: 1 dano de Força por rodada até Cura bem-sucedida.',
          } : effect);
          let conditions = combatant.conditions;
          let brokenAttribute = combatant.brokenAttribute;
          let status: EncounterParticipantStatus = broke ? 'broken' : combatant.status;
          if (combatant.kind === 'character' && combatant.entityId) {
            const index = characters.findIndex(character => character.id === combatant.entityId);
            const character = characters[index];
            if (character) {
              const strength = { ...character.attributes.strength, current: nextStrength };
              conditions = nextStrength === 0 ? Array.from(new Set([...character.conditions, 'Broken' as const])) : character.conditions;
              brokenAttribute = nextStrength === 0 ? 'strength' : character.brokenAttribute;
              const updated: Character = { ...character, attributes: { ...character.attributes, strength }, conditions,
                ...(brokenAttribute ? { brokenAttribute } : {}), statusEffects: (character.statusEffects || []).map(effect => effect.id === bleed.id ? {
                  ...effect, ongoingRounds: Number(effect.ongoingRounds || 0) + 1,
                  ...(deathRound ? { expiresRound: deathRound } : {}),
                } : effect),
              };
              characters[index] = updated;
              await characterStore.put(updated);
            }
          }
          const expires = nextStrength === 0 && deathRound !== undefined && round >= deathRound;
          if (expires) {
            status = 'dead';
            if (combatant.kind === 'character' && combatant.entityId) {
              const index = characters.findIndex(character => character.id === combatant.entityId);
              if (index >= 0) {
                characters[index] = { ...characters[index], status: 'dead' };
                await characterStore.put(characters[index]);
              }
            }
          }
          combatants = combatants.map(item => item.id === combatant.id ? {
            ...item, currentStrength: nextStrength, conditions, brokenAttribute, status, statusEffects,
            quickSheet: { ...(item.quickSheet || {}), attributes: combatant.kind === 'character' && combatant.entityId
              ? characters.find(character => character.id === combatant.entityId)?.attributes : item.quickSheet?.attributes, conditions, brokenAttribute },
          } : item);
          automaticRoundLogs.push({ id: crypto.randomUUID(), round, combatantId: combatant.id,
            text: `${combatant.name}: sangramento por arpão causou 1 dano de Força${broke ? '; Força Quebrada, morte em D6 minutos sem Cura' : ''}${expires ? '; morreu' : ''}.`, createdAt: now });
        }
        for(const combatant of [...combatants]){
          const web=(combatant.statusEffects||[]).find(effect=>effect.id.startsWith('monster-webbed:'));
          if(!web||combatant.status==='dead'||combatant.status==='fled'||combatant.status==='yielded')continue;
          const previous=combatant.currentStrength;
          const currentStrength=Math.max(0,previous-1);
          const broke=previous>0&&currentStrength===0;
          let conditions=combatant.conditions||[];
          let brokenAttribute=combatant.brokenAttribute;
          if(combatant.kind==='character'&&combatant.entityId){
            const index=characters.findIndex(item=>item.id===combatant.entityId);
            const character=characters[index];
            if(character){
              const strength={...character.attributes.strength,current:Math.max(0,character.attributes.strength.current-1)};
              conditions=strength.current===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
              brokenAttribute=strength.current===0?'strength':character.brokenAttribute;
              const updated={...character,attributes:{...character.attributes,strength},conditions,...(brokenAttribute?{brokenAttribute}:{})};
              characters[index]=updated;
              await characterStore.put(updated);
            }
          }
          combatants=combatants.map(item=>item.id===combatant.id?{
            ...item,currentStrength,conditions,brokenAttribute,status:currentStrength===0?'broken' as const:item.status,
            quickSheet:{...(item.quickSheet||{}),attributes:item.kind==='character'&&item.entityId?characters.find(character=>character.id===item.entityId)?.attributes:item.quickSheet?.attributes,conditions,brokenAttribute},
            statusEffects:[...(item.statusEffects||[]),...(broke?[{id:`critical-pending:web:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'A teia tóxica Quebrou Força; resolva o ferimento grave.',criticalTable:'other' as const}]:[])],
          }:item);
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: a teia tóxica causou 1 dano de Força${broke?'; Força Quebrada, ferimento grave pendente':''}.`,createdAt:now});
        }
        for(const combatant of [...combatants]){
          const drain=(combatant.statusEffects||[]).find(effect=>effect.id.startsWith('monster-life-drain:'));
          if(!drain||combatant.status==='dead'||combatant.status==='fled'||combatant.status==='yielded')continue;
          const previous=combatant.currentStrength;const currentStrength=Math.max(0,previous-1);const broke=previous>0&&currentStrength===0;
          let conditions=combatant.conditions||[];let brokenAttribute=combatant.brokenAttribute;
          if(combatant.kind==='character'&&combatant.entityId){
            const index=characters.findIndex(item=>item.id===combatant.entityId);const character=characters[index];
            if(character){const strength={...character.attributes.strength,current:Math.max(0,character.attributes.strength.current-1)};conditions=strength.current===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;brokenAttribute=strength.current===0?'strength':character.brokenAttribute;const marker='PENDENTE: ferimento grave causado por dreno vital';const updated={...character,attributes:{...character.attributes,strength},conditions,brokenAttribute,...(broke&&!(character.criticalInjuries||[]).includes(marker)?{criticalInjuries:[...(character.criticalInjuries||[]),marker]}:{})};characters[index]=updated;await characterStore.put(updated);}
          }
          combatants=combatants.map(item=>item.id!==combatant.id?item:{...item,currentStrength,conditions,brokenAttribute,status:currentStrength===0?'broken' as const:item.status,quickSheet:{...(item.quickSheet||{}),attributes:item.kind==='character'&&item.entityId?characters.find(character=>character.id===item.entityId)?.attributes:item.quickSheet?.attributes,conditions,brokenAttribute},statusEffects:[...(item.statusEffects||[]),...(broke?[{id:`critical-pending:life-drain:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'O dreno vital Quebrou Força; resolva dano atípico.',criticalTable:'other' as const}]:[])]});
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: o dreno vital causou 1 dano de Força${broke?'; Força Quebrada, ferimento grave pendente':''}.`,createdAt:now});
        }
        for(const combatant of [...combatants]){
          const infestation=(combatant.statusEffects||[]).find(effect=>effect.id.startsWith('monster-eggs:'));
          if(!infestation||combatant.status==='dead'||combatant.status==='fled'||combatant.status==='yielded')continue;
          const roundsRemaining=Math.max(0,Math.trunc(Number(infestation.ongoingRounds)||0)-1);
          const source=infestation.source||'Skolopendra';
          if(roundsRemaining>0){
            const detail=`Larvas eclodem em ${roundsRemaining} rodada(s); um teste de Cura bem-sucedido ou magia curativa interrompe a infestação.`;
            combatants=combatants.map(item=>item.id!==combatant.id?item:{...item,statusEffects:(item.statusEffects||[]).map(effect=>effect.id===infestation.id?{...effect,ongoingRounds:roundsRemaining,detail}:effect)});
            if(combatant.kind==='character'&&combatant.entityId){
              const index=characters.findIndex(item=>item.id===combatant.entityId);const character=characters[index];
              if(character){const statusEffects=(character.statusEffects||[]).map(effect=>effect.id===infestation.id?{...effect,ongoingRounds:roundsRemaining,detail}:effect);const updated={...character,statusEffects};characters[index]=updated;await characterStore.put(updated);}
            }
            automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: ovos da ${source}; eclosão em ${roundsRemaining} rodada(s).`,createdAt:now});
            continue;
          }
          const roll=ForbiddenLandsDiceEngine.roll({base:9,skill:0,gear:0},{characterId:null,sceneId:checked.startedSceneId||checked.id,checkId:`monster-eggs-eclosion:${checked.id}:${round}:${combatant.id}`,rng:Math.random});
          await rollStore.put(roll);
          const resolution=CombatRulesEngine.resolveAttack({attackSuccesses:roll.successes,weaponDamage:2,armorValue:combatant.armor,rng:Math.random});
          const previous=combatant.currentStrength;const currentStrength=Math.max(0,previous-resolution.finalDamage);const broke=previous>0&&currentStrength===0;
          let conditions=combatant.conditions||[];let brokenAttribute=combatant.brokenAttribute;
          const remainingEffects=(combatant.statusEffects||[]).filter(effect=>effect.id!==infestation.id);
          if(combatant.kind==='character'&&combatant.entityId){
            const index=characters.findIndex(item=>item.id===combatant.entityId);const character=characters[index];
            if(character){
              const attributes={...character.attributes,strength:{...character.attributes.strength,current:Math.max(0,character.attributes.strength.current-resolution.finalDamage)}};
              conditions=attributes.strength.current===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;
              brokenAttribute=attributes.strength.current===0?'strength':character.brokenAttribute;
              const marker='PENDENTE: ferimento grave causado pela eclosão dos ovos da Skolopendra';
              const updated={...character,attributes,conditions,brokenAttribute,statusEffects:(character.statusEffects||[]).filter(effect=>effect.id!==infestation.id),...(broke&&!(character.criticalInjuries||[]).includes(marker)?{criticalInjuries:[...(character.criticalInjuries||[]),marker]}:{})};
              characters[index]=updated;await characterStore.put(updated);
            }
          }
          combatants=combatants.map(item=>item.id!==combatant.id?item:{...item,currentStrength,conditions,brokenAttribute,status:currentStrength===0?'broken' as const:item.status,quickSheet:{...(item.quickSheet||{}),attributes:item.kind==='character'&&item.entityId?characters.find(character=>character.id===item.entityId)?.attributes:item.quickSheet?.attributes,conditions,brokenAttribute},statusEffects:[...remainingEffects,...(broke?[{id:`critical-pending:eggs:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'A eclosão dos ovos Quebrou Força; resolva o ferimento grave.',criticalTable:'slash' as const}]:[])]});
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: ovos da ${source} eclodiram; ataque 9 BD, dano cortante 2, ${resolution.finalDamage} dano após armadura${broke?'; Força Quebrada, ferimento grave pendente':''}.`,createdAt:now});
        }
        for(const combatant of [...combatants]){
          const consumed=(combatant.statusEffects||[]).find(effect=>effect.id.startsWith('monster-consumed:'));
          if(!consumed||combatant.status==='dead'||combatant.status==='fled'||combatant.status==='yielded')continue;
          const currentStrength=Math.max(0,combatant.currentStrength-1);const broken=currentStrength===0;
          let conditions=broken?Array.from(new Set([...(combatant.conditions||[]),'Broken' as const])):(combatant.conditions||[]);
          if(combatant.kind==='character'&&combatant.entityId){
            const index=characters.findIndex(item=>item.id===combatant.entityId);const character=characters[index];
            if(character){const strength={...character.attributes.strength,current:Math.max(0,character.attributes.strength.current-1)};conditions=strength.current===0?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;const updated={...character,attributes:{...character.attributes,strength},conditions,brokenAttribute:strength.current===0?'strength':character.brokenAttribute,...(strength.current===0?{status:'dead' as const}:{}),statusEffects:strength.current===0?(character.statusEffects||[]).filter(effect=>effect.id!==consumed.id):character.statusEffects};characters[index]=updated;await characterStore.put(updated);}
          }
          combatants=combatants.map(item=>item.id!==combatant.id?item:{...item,currentStrength,conditions,brokenAttribute:broken?'strength':item.brokenAttribute,status:broken?'dead' as const:item.status,quickSheet:{...(item.quickSheet||{}),attributes:item.kind==='character'&&item.entityId?characters.find(character=>character.id===item.entityId)?.attributes:item.quickSheet?.attributes,conditions,brokenAttribute:broken?'strength':item.brokenAttribute,...(broken?{status:'dead'}:{})},statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==consumed.id)});
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: ${broken?'morreu ao ser quebrado dentro do monstro':'sofreu 1 dano de Força dentro do monstro'}; ${broken?'a morte foi imediata pela regra de ingestão':''}.`,createdAt:now});
        }
        for(const combatant of [...combatants]){
          const dragged=(combatant.statusEffects||[]).find(effect=>effect.id.startsWith('monster-dragged:'));
          if(!dragged||combatant.status==='dead'||combatant.status==='fled'||combatant.status==='yielded')continue;
          const sourceId=dragged.id.slice('monster-dragged:'.length);const source=combatants.find(item=>item.id===sourceId);
          if(!source||source.status!=='active'){
            combatants=combatants.map(item=>item.id!==combatant.id?item:{...item,statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==dragged.id)});
            if(combatant.kind==='character'&&combatant.entityId){const index=characters.findIndex(item=>item.id===combatant.entityId);const character=characters[index];if(character){const updated={...character,statusEffects:(character.statusEffects||[]).filter(effect=>effect.id!==dragged.id)};characters[index]=updated;await characterStore.put(updated);}}
            continue;
          }
          const allies=combatants.filter(item=>item.side==='party'&&item.status==='active'&&item.id!==combatant.id);const centerX=allies.length?allies.reduce((sum,item)=>sum+Number(item.x??.5),0)/allies.length:Number(source.x??.5);const centerY=allies.length?allies.reduce((sum,item)=>sum+Number(item.y??.5),0)/allies.length:Number(source.y??.5);
          let dx=Number(combatant.x??.5)-centerX;let dy=Number(combatant.y??.5)-centerY;const length=Math.hypot(dx,dy)||1;dx/=length;dy/=length;
          const x=Math.max(.02,Math.min(.98,Number(combatant.x??.5)+dx*.12));const y=Math.max(.02,Math.min(.98,Number(combatant.y??.5)+dy*.12));const detail=`Preso pelas aranhas jovens; já foi arrastado ${Number(dragged.ongoingRounds||0)+1} zona(s). Escape com ação lenta e Força + Potência contra Força ${source.currentStrength}.`;
          combatants=combatants.map(item=>item.id!==combatant.id?item:{...item,x,y,statusEffects:(item.statusEffects||[]).map(effect=>effect.id===dragged.id?{...effect,ongoingRounds:Number(effect.ongoingRounds||0)+1,detail}:effect)});
          if(combatant.kind==='character'&&combatant.entityId){const index=characters.findIndex(item=>item.id===combatant.entityId);const character=characters[index];if(character){const statusEffects=(character.statusEffects||[]).map(effect=>effect.id===dragged.id?{...effect,ongoingRounds:Number(effect.ongoingRounds||0)+1,detail}:effect);const updated={...character,statusEffects};characters[index]=updated;await characterStore.put(updated);}}
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:combatant.id,text:`${combatant.name}: as aranhas arrastaram a vítima mais uma zona para longe do grupo.`,createdAt:now});
        }
        for(const combatant of combatants){
          if(!combatant.criticalInjuries?.some(injury=>injury.deathTimeRemaining?.unit==='rounds'))continue;
          const criticalInjuries=combatant.criticalInjuries.map(injury=>CriticalInjuryRulesEngine.advanceTime(injury,'rounds',1));
          const newlyFatal=criticalInjuries.some((injury,index)=>injury.status==='fatal'&&combatant.criticalInjuries?.[index]?.status!=='fatal');
          combatants=combatants.map(item=>item.id===combatant.id?{...item,criticalInjuries,status:newlyFatal?'dead':item.status,...(newlyFatal?{quickSheet:{...(item.quickSheet||{}),status:'dead'}}:{}),statusEffects:(item.statusEffects||[]).map(effect=>effect.criticalInjury?.id?{...effect,criticalInjury:criticalInjuries.find(injury=>injury.id===effect.criticalInjury.id)||effect.criticalInjury,detail:newlyFatal?'O prazo letal do ferimento terminou; o combatente morreu.':effect.detail}:effect)}:item);
          if(combatant.kind==='character'&&combatant.entityId){
            const character=characters.find(item=>item.id===combatant.entityId);
            if(character){
              const nextStates=(character.criticalInjuryStates||[]).map(injury=>criticalInjuries.find(nextInjury=>nextInjury.id===injury.id)||injury);
              await characterStore.put({...character,...(newlyFatal?{status:'dead' as const}:{}),criticalInjuryStates:nextStates});
            }
          }
        }
      }
      const enteringActorId=combatants[nextIndex]?.id;
      if(enteringActorId){
        const enteringBefore=combatants.find(item=>item.id===enteringActorId);
        const expiredEffectIds=(enteringBefore?.statusEffects||[]).filter(effect=>effect.expiresAtCombatantId===enteringActorId&&typeof effect.expiresRound==='number'&&round>=effect.expiresRound).map(effect=>effect.id);
        combatants=combatants.map(item=>item.id!==enteringActorId?item:{...item,statusEffects:(item.statusEffects||[]).filter(effect=>!(effect.expiresAtCombatantId===enteringActorId&&typeof effect.expiresRound==='number'&&round>=effect.expiresRound))});
        if(expiredEffectIds.length&&enteringBefore?.kind==='character'&&enteringBefore.entityId){const index=characters.findIndex(item=>item.id===enteringBefore.entityId);const character=characters[index];if(character){const updated={...character,statusEffects:(character.statusEffects||[]).filter(effect=>!expiredEffectIds.includes(effect.id))};characters[index]=updated;await characterStore.put(updated);}}
        const leeched=combatants.find(item=>item.id===enteringActorId&&item.kind==='character'&&item.entityId&&item.statusEffects?.some(effect=>effect.id.startsWith('monster-magic-leech:')));
        if(leeched?.kind==='character'&&leeched.entityId&&leeched.status!=='dead'){
          const character=characters.find(item=>item.id===leeched.entityId);const effect=leeched.statusEffects?.find(item=>item.id.startsWith('monster-magic-leech:'));
          if(character&&effect){
            const imp=combatants.find(item=>item.id===effect.id.slice('monster-magic-leech:'.length));
            if(!imp||imp.status!=='active'){
              const updated={...character,statusEffects:(character.statusEffects||[]).filter(item=>item.id!==effect.id)};characters[characters.findIndex(item=>item.id===character.id)]=updated;await characterStore.put(updated);combatants=combatants.map(item=>item.id!==leeched.id?item:{...item,statusEffects:(item.statusEffects||[]).filter(value=>value.id!==effect.id)});
              automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:leeched.id,text:`${leeched.name}: o Imp não pode mais sustentar o roubo mágico; a vítima foi solta.`,createdAt:now});
            }else{
              const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();const insight=Object.entries(character.skills||{}).find(([name])=>['insight','discernment','discernimento'].includes(normalizeSkill(name)))?.[1]||0;
              const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:character.id,sceneId:checked.startedSceneId||checked.id,checkId:`imp-leech-escape:${checked.id}:${round}:${character.id}`,rng:Math.random,criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits'});
              const impAttributes=(imp.quickSheet?.attributes||{}) as Record<string,{current?:number}>;const impRoll=ForbiddenLandsDiceEngine.roll({base:Number(impAttributes.wits?.current)||3,skill:0,gear:0},{characterId:null,sceneId:checked.startedSceneId||checked.id,checkId:`imp-leech-contest:${checked.id}:${round}:${character.id}`,rng:Math.random});
              await rollStore.put(victimRoll);await rollStore.put(impRoll);const escaped=victimRoll.successes>impRoll.successes;
              if(escaped){const updated={...character,statusEffects:(character.statusEffects||[]).filter(item=>item.id!==effect.id)};characters[characters.findIndex(item=>item.id===character.id)]=updated;await characterStore.put(updated);combatants=combatants.map(item=>item.id!==leeched.id?item:{...item,statusEffects:(item.statusEffects||[]).filter(value=>value.id!==effect.id)});automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:leeched.id,text:`${leeched.name}: venceu a disputa de Discernimento contra o Imp e escapou do roubo mágico.`,createdAt:now});}
              else{
                const wits={...character.attributes.wits,current:Math.max(0,character.attributes.wits.current-1)};const newlyBroken=wits.current===0;const conditions=newlyBroken?Array.from(new Set([...character.conditions,'Broken' as const])):character.conditions;const updated={...character,attributes:{...character.attributes,wits},willpowerPoints:WillpowerRulesEngine.change(character.willpowerPoints,-1),...(newlyBroken?{brokenAttribute:'wits' as const}:{}),conditions};characters[characters.findIndex(item=>item.id===character.id)]=updated;await characterStore.put(updated);combatants=combatants.map(item=>item.id!==leeched.id?item:{...item,currentStrength:updated.attributes.strength.current,conditions:[...conditions],brokenAttribute:updated.brokenAttribute,status:newlyBroken?'broken' as const:item.status,statusEffects:(item.statusEffects||[]).map(value=>value.id===effect.id?{...value,detail:`Preso pelo roubo mágico: −1 PFV e −1 Astúcia nesta rodada. Teste gratuito de Astúcia + Discernimento contra Astúcia do Imp no próximo turno; 2 de dano de um único ataque soltam a vítima.`}:value),quickSheet:{...(item.quickSheet||{}),attributes:updated.attributes,conditions,brokenAttribute:updated.brokenAttribute,willpowerPoints:updated.willpowerPoints}});automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:leeched.id,text:`${leeched.name}: perdeu a disputa de Discernimento contra o Imp; sofreu 1 dano de Astúcia e perdeu 1 PFV${newlyBroken?'; Astúcia Quebrada':''}.`,createdAt:now});
              }
            }
          }
        }
        const petrified=combatants.find(item=>item.id===enteringActorId&&item.statusEffects?.some(effect=>effect.id==='monster-petrified'));
        if(petrified&&petrified.kind==='character'&&petrified.entityId&&petrified.status!=='dead'){
          const character=characters.find(item=>item.id===petrified.entityId);
          if(!character||character.status==='dead')throw new Error('A ficha da personagem petrificada está ausente ou morta.');
          const endurance=Object.entries(character.skills||{}).find(([name])=>['endurance','resilience','resiliencia'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
          const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(endurance)||0,gear:0},{characterId:character.id,sceneId:checked.startedSceneId||checked.id,checkId:`petrification-resistance:${checked.id}:${round}:${character.id}`,rng:Math.random,criticalInjuries:character.criticalInjuryStates,skillName:'endurance',attributeName:'strength'});
          await rollStore.put(roll);
          const petrification=character.statusEffects?.find(effect=>effect.id==='monster-petrified');
          const roundsRemaining=Math.max(0,Math.max(1,Number(petrification?.ongoingRounds)||1)-1);
          if(roll.successes>0){
            const statusEffects=(character.statusEffects||[]).filter(effect=>effect.id!=='monster-petrified');
            const updatedCharacter={...character,statusEffects};
            characters[characters.findIndex(item=>item.id===character.id)]=updatedCharacter;
            await characterStore.put(updatedCharacter);
            combatants=combatants.map(item=>item.id!==petrified.id?item:{...item,statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!=='monster-petrified')});
            automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:petrified.id,text:`${petrified.name}: teste gratuito de Resiliência (${roll.successes} sucesso(s)) rompeu a petrificação.`,createdAt:now});
          }else if(roundsRemaining<=0){
            const updatedCharacter={...character,status:'dead' as const,statusEffects:(character.statusEffects||[]).filter(effect=>effect.id!=='monster-petrified')};
            characters[characters.findIndex(item=>item.id===character.id)]=updatedCharacter;
            await characterStore.put(updatedCharacter);
            combatants=combatants.map(item=>item.id!==petrified.id?item:{...item,status:'dead' as const,quickSheet:{...(item.quickSheet||{}),status:'dead'},statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!=='monster-petrified')});
            automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:petrified.id,text:`${petrified.name}: falhou no teste gratuito de Resiliência; a petrificação matou a personagem.`,createdAt:now});
          }else{
            const detail=`Imóvel e sem ações; teste gratuito de Resiliência no próprio turno. Morte em ${roundsRemaining} rodada(s) se não reverter a petrificação.`;
            const statusEffects=(character.statusEffects||[]).map(effect=>effect.id==='monster-petrified'?{...effect,detail,ongoingRounds:roundsRemaining}:effect);
            const updatedCharacter={...character,statusEffects};
            characters[characters.findIndex(item=>item.id===character.id)]=updatedCharacter;
            await characterStore.put(updatedCharacter);
            combatants=combatants.map(item=>item.id!==petrified.id?item:{...item,statusEffects:(item.statusEffects||[]).map(effect=>effect.id==='monster-petrified'?{...effect,detail,ongoingRounds:roundsRemaining}:effect)});
            automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:petrified.id,text:`${petrified.name}: falhou no teste gratuito de Resiliência; restam ${roundsRemaining} rodada(s) até a morte pela petrificação.`,createdAt:now});
          }
        }
        const bristleVictim=combatants.find(item=>item.id===enteringActorId&&item.kind==='character'&&item.entityId&&item.status!=='dead'&&item.statusEffects?.some(effect=>effect.id.startsWith('monster-bristles:')));
        let bristleRecoveryFailed=false;
        if(bristleVictim?.kind==='character'&&bristleVictim.entityId){
          const character=characters.find(item=>item.id===bristleVictim.entityId);
          if(!character)throw new Error('A ficha da personagem sufocada pelas cerdas está ausente.');
          const endurance=Object.entries(character.skills||{}).find(([name])=>['endurance','resilience','resiliencia'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
          const check=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(endurance)||0,gear:0},{characterId:character.id,sceneId:checked.startedSceneId||checked.id,checkId:`monster-bristle-recovery:${checked.id}:${round}:${character.id}`,rng:Math.random,criticalInjuries:character.criticalInjuryStates,skillName:'endurance',attributeName:'strength'});
          await rollStore.put(check);
          if(check.successes>0){
            const recovered=DrowningRulesEngine.rescue(character);
            const statusEffects=(recovered.statusEffects||[]).filter(effect=>effect.id!=='drowning'&&!effect.id.startsWith('monster-bristles:'));
            const updated={...recovered,statusEffects};
            characters[characters.findIndex(item=>item.id===character.id)]=updated;
            await characterStore.put(updated);
            combatants=combatants.map(item=>item.id!==bristleVictim.id?item:{...item,conditions:[...updated.conditions],statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!=='drowning'&&!effect.id.startsWith('monster-bristles:')),quickSheet:{...(item.quickSheet||{}),attributes:updated.attributes,conditions:updated.conditions,drowningState:undefined}});
            automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:bristleVictim.id,text:`${bristleVictim.name}: teste gratuito de Resiliência (${check.successes} sucesso(s)) tossiu as cerdas e encerrou o sufocamento.`,createdAt:now});
          }else bristleRecoveryFailed=true;
        }
        const submerged=combatants.find(item=>item.id===enteringActorId&&item.kind==='character'&&item.entityId&&item.status!=='dead'&&(!item.statusEffects?.some(effect=>effect.id.startsWith('monster-bristles:'))||bristleRecoveryFailed)&&DrowningRulesEngine.normalize(characters.find(character=>character.id===item.entityId)?.drowningState));
        if(submerged?.kind==='character'&&submerged.entityId){
          const character=characters.find(item=>item.id===submerged.entityId);
          if(!character)throw new Error('A ficha da personagem submersa está ausente.');
          let successes=0;
          let check:DiceRoll|undefined;
          if(DrowningRulesEngine.needsResilienceCheck(character)){
            const resilience=Object.entries(character.skills||{}).find(([name])=>['resilience','resiliencia','endurance'].includes(name.normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
            check=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(resilience)||0,gear:0},{characterId:character.id,sceneId:checked.startedSceneId||checked.id,checkId:`combat-drowning:${checked.id}:${round}:${character.id}`,rng:Math.random,criticalInjuries:character.criticalInjuryStates,skillName:'resilience',attributeName:'strength'});
            await rollStore.put(check);
            successes=check.successes;
          }
          const result=DrowningRulesEngine.resolveTurn(character,successes,Math.random);
          const broke=character.attributes.strength.current>0&&result.character.attributes.strength.current===0;
          const criticalMarker='PENDENTE: ferimento grave causado por afogamento';
          const detail=`${result.phaseAfter==='afloat'?'Boiando':result.phaseAfter==='underwater'?'Submerso: teste de Resiliência no próprio turno':'Afogando: 1 dano de Força por turno'}${result.character.drowningState?.minutesUntilDeath?`; morte em ${result.character.drowningState.minutesUntilDeath} minuto(s) sem resgate`:''}.`;
          const updatedCharacter={...result.character,statusEffects:[...(result.character.statusEffects||[]).filter(effect=>effect.id!=='drowning'&&!effect.id.startsWith('critical-pending:drowning:')),{id:'drowning',label:'Afogamento',detail},...(broke?[{id:`critical-pending:drowning:${crypto.randomUUID()}`,label:'Ferimento grave pendente',detail:'Afogamento Quebrou Força; resolva dano atípico.'}]:[])],...(broke&&!(result.character.criticalInjuries||[]).includes(criticalMarker)?{criticalInjuries:[...(result.character.criticalInjuries||[]),criticalMarker]}:{})};
          characters[characters.findIndex(item=>item.id===character.id)]=updatedCharacter;
          await characterStore.put(updatedCharacter);
          combatants=combatants.map(item=>item.id!==submerged.id?item:{...item,currentStrength:updatedCharacter.attributes.strength.current,brokenAttribute:updatedCharacter.brokenAttribute,status:updatedCharacter.attributes.strength.current===0?'broken' as const:item.status,conditions:[...updatedCharacter.conditions],statusEffects:[...(item.statusEffects||[]).filter(effect=>effect.id!=='drowning'&&!effect.id.startsWith('critical-pending:drowning:')),{id:'drowning',name:'Afogamento',detail},...(broke?[{id:`critical-pending:drowning:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:'Afogamento Quebrou Força; resolva dano atípico.',criticalTable:'other' as const}]:[])],quickSheet:{...(item.quickSheet||{}),attributes:updatedCharacter.attributes,conditions:updatedCharacter.conditions,brokenAttribute:updatedCharacter.brokenAttribute,drowningState:updatedCharacter.drowningState}});
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:submerged.id,text:`${submerged.name}: ${check?`Resiliência ${check.successes} sucesso(s); `:''}${result.damageApplied?'afogamento causou 1 dano de Força':result.phaseAfter==='underwater'?'permanece submerso prendendo a respiração':result.phaseAfter==='afloat'?'permanece boiando':'entrou em estado de afogamento'}${broke?'; Força Quebrada, morte em 1D6 minutos se não houver resgate':''}.`,createdAt:now});
        }
        if(bristleVictim&&bristleRecoveryFailed)automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:bristleVictim.id,text:`${bristleVictim.name}: falhou no teste gratuito de Resiliência para tossir as cerdas; o sufocamento causou 1 dano de Força.`,createdAt:now});
        const drowningMonster=combatants.find(item=>item.id===enteringActorId&&item.kind==='monster'&&item.status==='active'&&item.statusEffects?.some(effect=>effect.id.startsWith('monster-drowning:')));
        if(drowningMonster){
          const currentStrength=Math.max(0,drowningMonster.currentStrength-1);
          combatants=combatants.map(item=>item.id!==drowningMonster.id?item:{...item,currentStrength,status:currentStrength===0?'broken' as const:item.status,brokenAttribute:currentStrength===0?'strength':item.brokenAttribute,conditions:currentStrength===0?Array.from(new Set([...item.conditions,'Broken'])):item.conditions});
          automaticRoundLogs.push({id:crypto.randomUUID(),round,combatantId:drowningMonster.id,text:`${drowningMonster.name}: afogamento causou 1 dano de Força${currentStrength===0?'; Força Quebrada':''}.`,createdAt:now});
        }
      }
      const nextActive = combatants[nextIndex];
      const nextEncounter=this.resolveIfFinished({
        ...checked, ...(officialEncounterState?{officialEncounter:officialEncounterState}:{}),round, turnIndex: nextIndex, combatants,
        log: [...checked.log, ...automaticRoundLogs, { id: crypto.randomUUID(), round, combatantId: nextActive?.id, text: `Turno de ${nextActive?.name || 'combatente'}.`, createdAt: now }].slice(-300),
        updatedAt: now,
      },now);
      const next={...nextBaseWorld,encounter:nextEncounter,updatedAt:now};
      await store.put(next);return next;
    });
    this.notify(world);
    return world;
  }

  static async spendAction(campaignId: string, combatantId: string, action: 'fast' | 'slow'): Promise<WorldState> {
    return this.updateCombatActions(campaignId, [], (encounter, now) => {
      const current = encounter.combatants[encounter.turnIndex];
      if (!current || current.id !== combatantId) throw new Error('Apenas o combatente do turno atual pode gastar ações.');
      if (current.status !== 'active') throw new Error('Este combatente não está ativo.');
      if (monsterIncapacitated(current)) throw new Error(`${current.name} não pode escolher ações enquanto estiver incapacitado ou possuído.`);
      if (action === 'fast' && !canSpendFastAction(current)) throw new Error('As duas oportunidades de ação deste turno já foram usadas.');
      if (action === 'slow' && !current.slowActionAvailable) throw new Error('A ação lenta deste turno já foi usada.');
      return {
        ...encounter,
        combatants: encounter.combatants.map(item => {
          if (item.id !== combatantId) return item;
          if (action === 'fast') return spendFastAction(item);
          return { ...item, slowActionAvailable: false };
        }),
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${action === 'fast' ? 'Ação rápida' : 'Ação lenta'} usada por ${current.name}.`, createdAt: now }].slice(-300),
        updatedAt: now,
      };
    });
  }

  /** ARREMETER ARMA: ação rápida; arma corpo a corpo Pesada concede +1 dano ao Golpear/Estocar imediatamente depois, na mesma rodada. */
  static async windUpWeapon(campaignId:string,combatantId:string,weaponIndex:number):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];if(!actor||actor.id!==combatantId||actor.status!=='active')throw new Error('Apenas o combatente ativo pode Arremeter Arma.');
      if(!canSpendFastAction(actor))throw new Error(`${actor.name} não possui mais uma ação que possa ser usada como rápida nesta rodada.`);
      if(actor.statusEffects?.some(effect=>effect.id==='condition:prone'||effect.id.startsWith('core-grappled:')||effect.id.startsWith('core-grappling:')))throw new Error(`${actor.name} não pode Arremeter Arma nesse estado.`);
      if(monsterIncapacitated(actor))throw new Error(`${actor.name} está incapacitado.`);
      const chars=tx.objectStore('characters');const character=actor.kind==='character'&&actor.entityId?await chars.get(actor.entityId):undefined;if(character)CharacterActionRulesEngine.assertCanAct(character);
      const weapons=combatantWeapons(actor,character);const weapon=weapons[weaponIndex];if(!weapon)throw new Error('Escolha uma arma empunhada para Arremeter.');
      if(itemEncumbranceClass(weapon)!=='heavy')throw new Error('Arremeter Arma exige uma arma corpo a corpo PESADA.');
      if(/arco|besta|funda|bow|crossbow|sling/i.test(String(weapon.name||weapon.type||'')))throw new Error('Arremeter Arma exige uma arma corpo a corpo PESADA.');
      const now=Date.now();const effect={id:`core-windup:${weaponIndex}`,name:'Arma arremetida',detail:`${String(weapon.name||'Arma pesada')} foi arremetida: o próximo Golpear/Estocar nesta rodada causa +1 dano.`,source:String(weapon.name||'Arma pesada'),expiresRound:encounter.round+1,expiresAtCombatantId:actor.id};
      const combatants=encounter.combatants.map(item=>{if(item.id!==actor.id)return item;const base=character?syncCombatantFromCharacter(item,character):item;const spent=spendFastAction(base);return{...spent,statusEffects:[...(spent.statusEffects||[]).filter(value=>!value.id.startsWith('core-windup:')),effect]};});
      const next={...current,encounter:{...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text:`${actor.name} arremeteu ${String(weapon.name||'uma arma pesada')} e gastou a ação rápida; o próximo Golpear/Estocar nesta rodada recebe +1 dano.`,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(world);return world;
  }

  static async standUp(campaignId: string, combatantId: string): Promise<WorldState> {
    return this.updateCombatActions(campaignId, [], (encounter, now) => {
      const actor = encounter.combatants[encounter.turnIndex];
      if (!actor || actor.id !== combatantId) throw new Error('Apenas o combatente do turno atual pode se levantar.');
      if (actor.status !== 'active') throw new Error('Um combatente inativo não pode se levantar.');
      if (!actor.statusEffects?.some(effect => effect.id === 'condition:prone')) throw new Error(`${actor.name} não está Caído.`);
      if (!canSpendFastAction(actor)) throw new Error(`${actor.name} não possui mais uma ação que possa ser usada como rápida neste turno.`);
      return {
        ...encounter,
        combatants: encounter.combatants.map(item => item.id === combatantId
          ? { ...spendFastAction(item), statusEffects: (item.statusEffects || []).filter(effect => effect.id !== 'condition:prone') }
          : item),
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${actor.name} gastou a ação rápida para se levantar.`, createdAt: now }].slice(-300),
        updatedAt: now,
      };
    });
  }

  /** DERRUBAR: ação rápida, Luta; 2 sucessos se o alvo tiver Força maior. Esquiva ou Bloqueio com escudo podem cancelar sucessos. */
  static async knockDown(campaignId: string, attackerId: string, targetId: string, input: {
    defense?: 'none'|'dodge'|'block'; attackerGear?: number; defenderGear?: number; defenderUsesShield?: boolean; rng?: () => number;
  } = {}): Promise<{ world: WorldState; attackRoll: DiceRoll; defenseRoll: DiceRoll | null; success: boolean; requiredSuccesses: number; netSuccesses: number }> {
    const rng = input.rng || Math.random;
    const result = await withTransaction(['world_state','characters','rolls'],'readwrite', async tx => {
      const worldStore = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const attacker = encounter.combatants[encounter.turnIndex];
      const target = encounter.combatants.find(item => item.id === targetId);
      if (!attacker || attacker.id !== attackerId || attacker.status !== 'active') throw new Error('Apenas o combatente ativo pode Derrubar.');
      if (!target || target.id === attacker.id || target.status !== 'active') throw new Error('Escolha um alvo ativo para Derrubar.');
      if (monsterIncapacitated(attacker)) throw new Error(`${attacker.name} está incapacitado.`);
      if (!canSpendFastAction(attacker)) throw new Error(`${attacker.name} não possui mais uma ação que possa ser usada como rápida nesta rodada.`);
      const defense = input.defense || 'none';
      if (defense === 'block' && !input.defenderUsesShield) throw new Error('Bloquear uma tentativa de Derrubar requer um escudo.');
      const characterStore = tx.objectStore('characters');
      let attackerCharacter = attacker.kind === 'character' && attacker.entityId ? await characterStore.get(attacker.entityId) : undefined;
      let targetCharacter = target.kind === 'character' && target.entityId ? await characterStore.get(target.entityId) : undefined;
      if (attackerCharacter) CharacterActionRulesEngine.assertCanAct(attackerCharacter);
      if (targetCharacter) CharacterActionRulesEngine.assertCanAct(targetCharacter);
      if (defense === 'dodge' && !defenderDodgePlan(target, targetCharacter, encounter.round).allowed) throw new Error(`${target.name} não possui uma ação disponível para Esquivar.`);
      if (defense === 'block' && !defenderBlockPlan(target, targetCharacter, encounter.round).allowed) throw new Error(`${target.name} não possui uma ação disponível para Bloquear.`);
      const defenderTalentRank = combatantTalentRank(target, targetCharacter, 'Defensor');
      const steadyFeetRank = combatantTalentRank(target, targetCharacter, 'Pés Firmes');
      const fastFootworkRank = combatantTalentRank(target, targetCharacter, 'Pés Ligeiros');
      const attackerStrength = combatantAttribute(attacker, attackerCharacter, 'strength');
      const targetStrength = combatantAttribute(target, targetCharacter, 'strength');
      const attackRoll = ForbiddenLandsDiceEngine.roll(
        { base: attackerStrength, skill: combatantSkill(attacker, attackerCharacter, ['Melee','Luta']), gear: Math.max(0, Math.trunc(input.attackerGear || 0)) },
        { characterId: attackerCharacter?.id, sceneId: encounter.startedSceneId || encounter.id, checkId: `knockdown:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`, rng, criticalInjuries: attackerCharacter?.criticalInjuryStates || attacker.criticalInjuries, skillName: 'melee', attributeName: 'strength' },
      );
      let defenseRoll: DiceRoll | null = null;
      if (defense !== 'none') {
        const dodge = defense === 'dodge';
        defenseRoll = ForbiddenLandsDiceEngine.roll(
          { base: combatantAttribute(target, targetCharacter, dodge ? 'agility' : 'strength'), skill: combatantSkill(target, targetCharacter, dodge ? (target.mounted ? ['Animal Handling', 'Adestramento'] : ['Movement','Movimentação']) : ['Melee','Luta']), gear: dodge ? 0 : Math.max(0, Math.trunc(input.defenderGear || 0)), modifier: dodge ? (fastFootworkRank >= 2 ? 1 : 0) : (!dodge && defenderTalentRank >= 2 ? 1 : 0) },
          { characterId: targetCharacter?.id, sceneId: encounter.startedSceneId || encounter.id, checkId: `knockdown-defense:${encounter.id}:${encounter.round}:${target.id}:${attacker.id}`, rng, criticalInjuries: targetCharacter?.criticalInjuryStates || target.criticalInjuries, skillName: dodge ? (target.mounted ? 'animal_handling' : 'movement') : 'melee', attributeName: dodge ? 'agility' : 'strength' },
        );
      }
      await tx.objectStore('rolls').put(attackRoll); if (defenseRoll) await tx.objectStore('rolls').put(defenseRoll);
      let attackTriggers: ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck> = [];
      let defenseTriggers: ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck> = [];
      if (attackerCharacter) { const applied = applyCriticalRollConsequences(attackerCharacter, attackRoll); attackerCharacter = applied.character; attackTriggers = applied.triggers; await characterStore.put(attackerCharacter); }
      if (targetCharacter && defenseRoll) { const applied = applyCriticalRollConsequences(targetCharacter, defenseRoll); targetCharacter = applied.character; defenseTriggers = applied.triggers; await characterStore.put(targetCharacter); }
      const resolution = CombatRulesEngine.resolveKnockDown({ attackerSuccesses: attackRoll.successes, defenderSuccesses: defenseRoll?.successes || 0, attackerStrength, targetStrength, steadyFeetRank });
      const now = Date.now();
      let combatants = encounter.combatants.map(item => {
        if (item.id === attacker.id) {
          const synced = attackerCharacter ? syncCombatantFromCharacter(item, attackerCharacter) : item;
          return spendFastAction(synced);
        }
        if (item.id === target.id) {
          const synced = targetCharacter ? syncCombatantFromCharacter(item, targetCharacter) : item;
          const defended = applyReactiveDefenseCost(synced, targetCharacter, encounter.round, defense).combatant;
          return { ...defended, ...(resolution.success ? { statusEffects: [...(defended.statusEffects || []).filter(effect => effect.id !== 'condition:prone'), { id:'condition:prone', name:'Caído', detail:'Derrubado em combate; Levantar-se custa uma ação rápida.' }] } : {}) };
        }
        return item;
      });
      const detail = `${attacker.name} tentou Derrubar ${target.name}: ${attackRoll.successes} sucesso(s)${defenseRoll ? ` contra ${defenseRoll.successes}` : ''}; precisava de ${resolution.requiredSuccesses}; ${resolution.success ? `${target.name} ficou Caído` : 'não conseguiu'}${criticalRollConsequenceText(attackTriggers)}${criticalRollConsequenceText(defenseTriggers)}.`;
      const nextEncounter = this.resolveIfFinished({ ...encounter, combatants, log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,text:detail,createdAt:now}].slice(-300), updatedAt:now }, now);
      const world = { ...current, encounter: nextEncounter, updatedAt: now }; await worldStore.put(world);
      return { world, attackRoll, defenseRoll, success: resolution.success, requiredSuccesses: resolution.requiredSuccesses, netSuccesses: resolution.netSuccesses };
    });
    this.notify(result.world); return result;
  }

  /** DESARMAR: ação rápida; um sucesso contra arma de uma mão, dois contra arma de duas mãos. */
  static async disarm(campaignId: string, attackerId: string, targetId: string, targetWeaponIndex: number, input: {
    defense?: 'none'|'dodge'|'block'; attackerGear?: number; defenderGear?: number; rng?: () => number;
  } = {}): Promise<{ world: WorldState; attackRoll: DiceRoll; defenseRoll: DiceRoll | null; success: boolean; requiredSuccesses: number; netSuccesses: number }> {
    const rng = input.rng || Math.random;
    const result = await withTransaction(['world_state','characters','rolls'],'readwrite', async tx => {
      const worldStore=tx.objectStore('world_state'); const current=WorldStateService.normalize(await worldStore.get(campaignId),campaignId); const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const attacker=encounter.combatants[encounter.turnIndex]; const target=encounter.combatants.find(item=>item.id===targetId);
      if(!attacker||attacker.id!==attackerId||attacker.status!=='active')throw new Error('Apenas o combatente ativo pode Desarmar.');
      if(!target||target.id===attacker.id||target.status!=='active')throw new Error('Escolha um alvo ativo para Desarmar.');
      if(monsterIncapacitated(attacker))throw new Error(`${attacker.name} está incapacitado.`); if(!canSpendFastAction(attacker))throw new Error(`${attacker.name} não possui mais uma ação que possa ser usada como rápida nesta rodada.`);
      const defense=input.defense||'none';
      const characterStore=tx.objectStore('characters'); let attackerCharacter=attacker.kind==='character'&&attacker.entityId?await characterStore.get(attacker.entityId):undefined; let targetCharacter=target.kind==='character'&&target.entityId?await characterStore.get(target.entityId):undefined;
      if(attackerCharacter)CharacterActionRulesEngine.assertCanAct(attackerCharacter); if(targetCharacter)CharacterActionRulesEngine.assertCanAct(targetCharacter);
      if(defense==='dodge'&&!defenderDodgePlan(target,targetCharacter,encounter.round).allowed)throw new Error(`${target.name} não possui uma ação disponível para Esquivar.`);
      if(defense==='block'&&!defenderBlockPlan(target,targetCharacter,encounter.round).allowed)throw new Error(`${target.name} não possui uma ação disponível para Bloquear.`);
      const defenderTalentRank=combatantTalentRank(target,targetCharacter,'Defensor');
      const firmGripRank=combatantTalentRank(target,targetCharacter,'Empunhadura Firme');
      const fastFootworkRank=combatantTalentRank(target,targetCharacter,'Pés Ligeiros');
      const weapons=combatantWeapons(target,targetCharacter); const weapon=weapons[targetWeaponIndex]; if(!weapon)throw new Error('Escolha a arma empunhada que será desarmada.'); if(isShield(weapon))throw new Error('Escudos não podem ser Desarmados.');
      const attackRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(attacker,attackerCharacter,'strength'),skill:combatantSkill(attacker,attackerCharacter,['Melee','Luta']),gear:Math.max(0,Math.trunc(input.attackerGear||0))},{characterId:attackerCharacter?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`disarm:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:attackerCharacter?.criticalInjuryStates||attacker.criticalInjuries,skillName:'melee',attributeName:'strength'});
      let defenseRoll:DiceRoll|null=null; if(defense!=='none'){const dodge=defense==='dodge';defenseRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(target,targetCharacter,dodge?'agility':'strength'),skill:combatantSkill(target,targetCharacter,dodge?(target.mounted?['Animal Handling','Adestramento']:['Movement','Movimentação']):['Melee','Luta']),gear:dodge?0:Math.max(0,Math.trunc(input.defenderGear||0)),modifier:dodge?(fastFootworkRank>=2?1:0):(!dodge&&defenderTalentRank>=2?1:0)},{characterId:targetCharacter?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`disarm-defense:${encounter.id}:${encounter.round}:${target.id}:${attacker.id}`,rng,criticalInjuries:targetCharacter?.criticalInjuryStates||target.criticalInjuries,skillName:dodge?(target.mounted?'animal_handling':'movement'):'melee',attributeName:dodge?'agility':'strength'});}
      await tx.objectStore('rolls').put(attackRoll);if(defenseRoll)await tx.objectStore('rolls').put(defenseRoll);
      let attackTriggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];let defenseTriggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];
      if(attackerCharacter){const applied=applyCriticalRollConsequences(attackerCharacter,attackRoll);attackerCharacter=applied.character;attackTriggers=applied.triggers;await characterStore.put(attackerCharacter);} if(targetCharacter&&defenseRoll){const applied=applyCriticalRollConsequences(targetCharacter,defenseRoll);targetCharacter=applied.character;defenseTriggers=applied.triggers;await characterStore.put(targetCharacter);}
      const twoHanded=CriticalInjuryRulesEngine.weaponRequiresTwoHands(weapon); const resolution=CombatRulesEngine.resolveDisarm({attackerSuccesses:attackRoll.successes,defenderSuccesses:defenseRoll?.successes||0,targetWeaponTwoHanded:twoHanded,firmGripRank});
      let targetWeapons=weapons; let dropped=encounter.droppedEquipment||[];
      if(resolution.success){targetWeapons=weapons.filter((_,index)=>index!==targetWeaponIndex);if(targetCharacter){targetCharacter={...targetCharacter,weapons:targetWeapons as Character['weapons']};await characterStore.put(targetCharacter);} dropped=[...dropped,{id:`dropped:disarm:${encounter.id}:${encounter.round}:${target.id}:${crypto.randomUUID()}`,characterId:target.entityId||target.id,characterName:target.name,collection:'weapons' as const,item:{...weapon},droppedAt:Date.now()}];}
      const now=Date.now(); const combatants=encounter.combatants.map(item=>{if(item.id===attacker.id){const synced=attackerCharacter?syncCombatantFromCharacter(item,attackerCharacter):item;return spendFastAction(synced);}if(item.id===target.id){const synced=targetCharacter?syncCombatantFromCharacter(item,targetCharacter):item;const defended=applyReactiveDefenseCost(synced,targetCharacter,encounter.round,defense).combatant;return{...defended,...(resolution.success?{quickSheet:{...(defended.quickSheet||{}),weapons:targetWeapons}}:{})};}return item;});
      const detail=`${attacker.name} tentou Desarmar ${target.name}: ${attackRoll.successes} sucesso(s)${defenseRoll?` contra ${defenseRoll.successes}`:''}; precisava de ${resolution.requiredSuccesses}; ${resolution.success?`${String(weapon.name||'arma')} caiu ao Alcance das Mãos`:'não conseguiu'}${criticalRollConsequenceText(attackTriggers)}${criticalRollConsequenceText(defenseTriggers)}.`;
      const nextEncounter=this.resolveIfFinished({...encounter,combatants,droppedEquipment:dropped,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,text:detail,createdAt:now}].slice(-300),updatedAt:now},now);const world={...current,encounter:nextEncounter,updatedAt:now};await worldStore.put(world);return{world,attackRoll,defenseRoll,success:resolution.success,requiredSuccesses:resolution.requiredSuccesses,netSuccesses:resolution.netSuccesses};
    });this.notify(result.world);return result;
  }

  /** FINTAR: troca as iniciativas com o alvo, mas a nova ordem só entra em vigor na rodada seguinte. */
  static async feint(campaignId:string,attackerId:string,targetId:string):Promise<WorldState>{
    return this.updateCombatActions(campaignId,[targetId],(encounter,now)=>{
      const attacker=encounter.combatants[encounter.turnIndex];const target=encounter.combatants.find(item=>item.id===targetId);
      if(!attacker||attacker.id!==attackerId||attacker.status!=='active')throw new Error('Apenas o combatente ativo pode Fintar.');
      if(!target||target.id===attacker.id||target.status!=='active')throw new Error('Escolha um alvo ativo para Fintar.');
      if(!canSpendFastAction(attacker))throw new Error(`${attacker.name} não possui mais uma ação que possa ser usada como rápida nesta rodada.`);
      const swapped=CombatRulesEngine.feintInitiatives(attacker.initiative,target.initiative);
      return{...encounter,combatants:encounter.combatants.map(item=>item.id===attacker.id?{...spendFastAction(item),nextInitiative:swapped.attackerInitiative}:item.id===target.id?{...item,nextInitiative:swapped.targetInitiative}:item),log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,text:`${attacker.name} fintou ${target.name}: iniciativas ${attacker.initiative} ↔ ${target.initiative} a partir da próxima rodada.`,createdAt:now}].slice(-300),updatedAt:now};
    });
  }


  /** AGARRAR: ação lenta desarmada. Em sucesso ambos ficam Caídos; o alvo larga a arma empunhada e só pode Libertar-se. */
  static async grapple(campaignId:string,attackerId:string,targetId:string,input:{defense?:'none'|'dodge'|'block';defenderGear?:number;heldWeaponIndex?:number;rng?:()=>number}={}):Promise<{world:WorldState;attackRoll:DiceRoll;defenseRoll:DiceRoll|null;success:boolean}>{
    const rng=input.rng||Math.random;
    const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');const attacker=encounter.combatants[encounter.turnIndex];const target=encounter.combatants.find(item=>item.id===targetId);
      if(!attacker||attacker.id!==attackerId||attacker.status!=='active')throw new Error('Apenas o combatente ativo pode Agarrar.');if(!target||target.id===attacker.id||target.status!=='active')throw new Error('Escolha um alvo ativo para Agarrar.');
      if(monsterIncapacitated(attacker)||attacker.statusEffects?.some(effect=>effect.id.startsWith('core-grappled:')||effect.id.startsWith('core-grappling:')))throw new Error(`${attacker.name} não pode iniciar outro Agarrão.`);if(!attacker.slowActionAvailable)throw new Error(`${attacker.name} já gastou a ação lenta desta rodada.`);
      const defense=input.defense||'none';
      const characterStore=tx.objectStore('characters');let attackerCharacter=attacker.kind==='character'&&attacker.entityId?await characterStore.get(attacker.entityId):undefined;let targetCharacter=target.kind==='character'&&target.entityId?await characterStore.get(target.entityId):undefined;
      if(attackerCharacter)CharacterActionRulesEngine.assertCanAct(attackerCharacter);if(targetCharacter)CharacterActionRulesEngine.assertCanAct(targetCharacter);
      if(defense==='dodge'&&!defenderDodgePlan(target,targetCharacter,encounter.round).allowed)throw new Error(`${target.name} não possui uma ação disponível para Esquivar.`);
      if(defense==='block'&&!defenderBlockPlan(target,targetCharacter,encounter.round).allowed)throw new Error(`${target.name} não possui uma ação disponível para Bloquear.`);
      const defenderTalentRank=combatantTalentRank(target,targetCharacter,'Defensor');
      const fastFootworkRank=combatantTalentRank(target,targetCharacter,'Pés Ligeiros');
      const targetWeapons=combatantWeapons(target,targetCharacter);if(targetWeapons.length&&input.heldWeaponIndex===undefined)throw new Error('Informe qual arma o alvo está empunhando para que ela seja largada em caso de Agarrão bem-sucedido.');if(input.heldWeaponIndex!==undefined&&!targetWeapons[input.heldWeaponIndex])throw new Error('A arma empunhada selecionada não existe mais.');
      const attackRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(attacker,attackerCharacter,'strength'),skill:combatantSkill(attacker,attackerCharacter,['Melee','Luta']),gear:0},{characterId:attackerCharacter?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`grapple:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:attackerCharacter?.criticalInjuryStates||attacker.criticalInjuries,skillName:'melee',attributeName:'strength'});
      let defenseRoll:DiceRoll|null=null;if(defense!=='none'){const dodge=defense==='dodge';defenseRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(target,targetCharacter,dodge?'agility':'strength'),skill:combatantSkill(target,targetCharacter,dodge?(target.mounted?['Animal Handling','Adestramento']:['Movement','Movimentação']):['Melee','Luta']),gear:dodge?0:Math.max(0,Math.trunc(input.defenderGear||0)),modifier:dodge?(fastFootworkRank>=2?1:0):(!dodge&&defenderTalentRank>=2?1:0)},{characterId:targetCharacter?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`grapple-defense:${encounter.id}:${encounter.round}:${target.id}:${attacker.id}`,rng,criticalInjuries:targetCharacter?.criticalInjuryStates||target.criticalInjuries,skillName:dodge?(target.mounted?'animal_handling':'movement'):'melee',attributeName:dodge?'agility':'strength'});}
      await tx.objectStore('rolls').put(attackRoll);if(defenseRoll)await tx.objectStore('rolls').put(defenseRoll);let attackTriggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];let defenseTriggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];
      if(attackerCharacter){const applied=applyCriticalRollConsequences(attackerCharacter,attackRoll);attackerCharacter=applied.character;attackTriggers=applied.triggers;await characterStore.put(attackerCharacter);}if(targetCharacter&&defenseRoll){const applied=applyCriticalRollConsequences(targetCharacter,defenseRoll);targetCharacter=applied.character;defenseTriggers=applied.triggers;await characterStore.put(targetCharacter);}
      const resolution=CombatRulesEngine.resolveGrapple({attackerSuccesses:attackRoll.successes,defenderSuccesses:defenseRoll?.successes||0});let dropped=encounter.droppedEquipment||[];let nextTargetWeapons=targetWeapons;
      if(resolution.success&&input.heldWeaponIndex!==undefined){const weapon=targetWeapons[input.heldWeaponIndex];nextTargetWeapons=targetWeapons.filter((_,index)=>index!==input.heldWeaponIndex);dropped=[...dropped,{id:`dropped:grapple:${encounter.id}:${encounter.round}:${target.id}:${crypto.randomUUID()}`,characterId:target.entityId||target.id,characterName:target.name,collection:'weapons' as const,item:{...weapon},droppedAt:Date.now()}];if(targetCharacter){targetCharacter={...targetCharacter,weapons:nextTargetWeapons as Character['weapons']};await characterStore.put(targetCharacter);}}
      const now=Date.now();const prone={id:'condition:prone',name:'Caído',detail:'Caído no chão.'};
      const combatants=encounter.combatants.map(item=>{if(item.id===attacker.id){const synced=attackerCharacter?syncCombatantFromCharacter(item,attackerCharacter):item;return{...synced,slowActionAvailable:false,...(resolution.success?{statusEffects:[...(synced.statusEffects||[]).filter(effect=>effect.id!=='condition:prone'&&!effect.id.startsWith('core-grappling:')),{...prone},{id:`core-grappling:${target.id}`,name:'Agarrando',detail:`Agarrando ${target.name}; só pode fazer Ataque Atracado enquanto mantiver o agarrão.`}]}:{})};}if(item.id===target.id){const synced=targetCharacter?syncCombatantFromCharacter(item,targetCharacter):item;const defended=applyReactiveDefenseCost(synced,targetCharacter,encounter.round,defense).combatant;return{...defended,...(resolution.success?{statusEffects:[...(defended.statusEffects||[]).filter(effect=>effect.id!=='condition:prone'&&!effect.id.startsWith('core-grappled:')),{...prone},{id:`core-grappled:${attacker.id}`,name:'Agarrado',detail:`Agarrado por ${attacker.name}; não pode se mover e sua única ação é Libertar-se.`}],quickSheet:{...(defended.quickSheet||{}),weapons:nextTargetWeapons}}:{})};}return item;});
      const detail=`${attacker.name} tentou Agarrar ${target.name}: ${attackRoll.successes} sucesso(s)${defenseRoll?` contra ${defenseRoll.successes}`:''}; ${resolution.success?'Agarrão estabelecido, ambos Caídos':'falhou'}${criticalRollConsequenceText(attackTriggers)}${criticalRollConsequenceText(defenseTriggers)}.`;const nextEncounter=this.resolveIfFinished({...encounter,combatants,droppedEquipment:dropped,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,text:detail,createdAt:now}].slice(-300),updatedAt:now},now);const world={...current,encounter:nextEncounter,updatedAt:now};await store.put(world);return{world,attackRoll,defenseRoll,success:resolution.success};
    });this.notify(result.world);return result;
  }

  /** LIBERTAR-SE: ação lenta e disputa de Luta contra quem mantém o Agarrão; empate mantém o Agarrão. */
  static async breakFreeCoreGrapple(campaignId:string,victimId:string,rng:()=>number=Math.random):Promise<{world:WorldState;victimRoll:DiceRoll;grapplerRoll:DiceRoll;success:boolean}>{
    const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');const victim=encounter.combatants[encounter.turnIndex];if(!victim||victim.id!==victimId||victim.status!=='active')throw new Error('Apenas o combatente ativo pode Libertar-se.');if(!victim.slowActionAvailable)throw new Error(`${victim.name} já gastou a ação lenta desta rodada.`);const held=victim.statusEffects?.find(effect=>effect.id.startsWith('core-grappled:'));if(!held)throw new Error(`${victim.name} não está Agarrado.`);const grapplerId=held.id.slice('core-grappled:'.length);const grappler=encounter.combatants.find(item=>item.id===grapplerId);if(!grappler||grappler.status!=='active')throw new Error('Quem mantinha o Agarrão não está mais ativo.');
      const chars=tx.objectStore('characters');let victimCharacter=victim.kind==='character'&&victim.entityId?await chars.get(victim.entityId):undefined;let grapplerCharacter=grappler.kind==='character'&&grappler.entityId?await chars.get(grappler.entityId):undefined;if(victimCharacter)CharacterActionRulesEngine.assertCanAct(victimCharacter);
      const victimRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(victim,victimCharacter,'strength'),skill:combatantSkill(victim,victimCharacter,['Melee','Luta']),gear:0},{characterId:victimCharacter?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`grapple-break:${encounter.id}:${encounter.round}:${victim.id}`,rng,criticalInjuries:victimCharacter?.criticalInjuryStates||victim.criticalInjuries,skillName:'melee',attributeName:'strength'});const grapplerRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(grappler,grapplerCharacter,'strength'),skill:combatantSkill(grappler,grapplerCharacter,['Melee','Luta']),gear:0},{characterId:grapplerCharacter?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`grapple-hold:${encounter.id}:${encounter.round}:${grappler.id}`,rng,criticalInjuries:grapplerCharacter?.criticalInjuryStates||grappler.criticalInjuries,skillName:'melee',attributeName:'strength'});await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(grapplerRoll);
      let victimTriggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];let grapplerTriggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];if(victimCharacter){const a=applyCriticalRollConsequences(victimCharacter,victimRoll);victimCharacter=a.character;victimTriggers=a.triggers;await chars.put(victimCharacter);}if(grapplerCharacter){const a=applyCriticalRollConsequences(grapplerCharacter,grapplerRoll);grapplerCharacter=a.character;grapplerTriggers=a.triggers;await chars.put(grapplerCharacter);}const success=CombatRulesEngine.grappleEscapeSucceeded(victimRoll.successes,grapplerRoll.successes);const now=Date.now();const combatants=encounter.combatants.map(item=>{if(item.id===victim.id){const synced=victimCharacter?syncCombatantFromCharacter(item,victimCharacter):item;return{...synced,slowActionAvailable:false,...(success?{statusEffects:(synced.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-grappled:'))}:{})};}if(item.id===grappler.id){const synced=grapplerCharacter?syncCombatantFromCharacter(item,grapplerCharacter):item;return success?{...synced,statusEffects:(synced.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-grappling:'))}:synced;}return item;});const text=`${victim.name} tentou Libertar-se: ${victimRoll.successes} × ${grapplerRoll.successes}; ${success?'venceu e encerrou o Agarrão':'permanece Agarrado'}${criticalRollConsequenceText(victimTriggers)}${criticalRollConsequenceText(grapplerTriggers)}.`;const nextEncounter=this.resolveIfFinished({...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:victim.id,text,createdAt:now}].slice(-300),updatedAt:now},now);const world={...current,encounter:nextEncounter,updatedAt:now};await store.put(world);return{world,victimRoll,grapplerRoll,success};});this.notify(result.world);return result;
  }

  /** ATAQUE ATRACADO: ataque desarmado rápido, sem Esquiva/Bloqueio, contra o alvo do Agarrão. */
  static async grappledAttack(campaignId:string,attackerId:string,rng:()=>number=Math.random):Promise<{world:WorldState;attackRoll:DiceRoll;resolution:AttackResolution;critical:boolean}>{
    const current=await WorldStateService.get(campaignId);const encounter=current.encounter;if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');const attacker=encounter.combatants[encounter.turnIndex];if(!attacker||attacker.id!==attackerId||attacker.status!=='active')throw new Error('Apenas o combatente ativo pode realizar Ataque Atracado.');const holding=attacker.statusEffects?.find(effect=>effect.id.startsWith('core-grappling:'));if(!holding)throw new Error(`${attacker.name} não está agarrando um oponente.`);if(!canSpendFastAction(attacker))throw new Error(`${attacker.name} não possui mais uma ação que possa ser usada como rápida nesta rodada.`);const targetId=holding.id.slice('core-grappling:'.length);const target=encounter.combatants.find(item=>item.id===targetId);if(!target||target.status!=='active')throw new Error('O alvo do Agarrão não está mais ativo.');
    let character:Character|undefined;if(attacker.kind==='character'&&attacker.entityId){character=await withTransaction('characters','readonly',async tx=>tx.store.get(attacker.entityId!));if(character)CharacterActionRulesEngine.assertCanAct(character);}const attackRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(attacker,character,'strength'),skill:combatantSkill(attacker,character,['Melee','Luta']),gear:0},{characterId:character?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`grappled-attack:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character?.criticalInjuryStates||attacker.criticalInjuries,skillName:'melee',attributeName:'strength'});const committed=await this.commitBasicAttackResult(campaignId,attacker.id,target.id,{attackRoll,defenseRoll:null,weaponDamage:1,criticalTable:'blunt',rng,actionCost:'fast',ignoreProne:true,grappledAttack:true});return{world:committed.world,attackRoll,resolution:committed.resolution,critical:committed.critical};
  }


  /** RECUAR: ação rápida. Move narrativamente para Perto; falha concede um ataque livre imediato sem defesa. */
  static async retreat(campaignId:string,actorId:string,enemyId:string,rng:()=>number=Math.random):Promise<{world:WorldState;roll:DiceRoll;success:boolean;freeAttack:boolean}>{
    const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');const actor=encounter.combatants[encounter.turnIndex];const enemy=encounter.combatants.find(item=>item.id===enemyId);if(!actor||actor.id!==actorId||actor.status!=='active')throw new Error('Apenas o combatente ativo pode Recuar.');if(!enemy||enemy.id===actor.id||enemy.status!=='active')throw new Error('Escolha o inimigo ativo ao Alcance das Mãos.');if(enemy.side===actor.side)throw new Error('Recuar exige um inimigo ativo ao Alcance das Mãos.');if(!canSpendFastAction(actor))throw new Error(`${actor.name} não possui mais uma ação que possa ser usada como rápida nesta rodada.`);if(actor.statusEffects?.some(effect=>effect.id.startsWith('core-grappled:')||effect.id.startsWith('core-grappling:')))throw new Error('Não é possível Recuar enquanto participa de um Agarrão.');if(monsterIncapacitated(actor))throw new Error(`${actor.name} está incapacitado.`);
      const chars=tx.objectStore('characters');let character=actor.kind==='character'&&actor.entityId?await chars.get(actor.entityId):undefined;if(character)CharacterActionRulesEngine.assertCanAct(character);const roll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(actor,character,'agility'),skill:combatantSkill(actor,character,['Movement','Movimentação']),gear:0},{characterId:character?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`retreat:${encounter.id}:${encounter.round}:${actor.id}:${enemy.id}`,rng,criticalInjuries:character?.criticalInjuryStates||actor.criticalInjuries,skillName:'movement',attributeName:'agility'});await tx.objectStore('rolls').put(roll);let triggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];if(character){const applied=applyCriticalRollConsequences(character,roll);character=applied.character;triggers=applied.triggers;await chars.put(character);}const outcome=CombatRulesEngine.retreat(roll.successes);const now=Date.now();const retreatEffect={id:`core-retreated:${enemy.id}:${encounter.round}`,name:'Recuou para Perto',detail:`Recuou de ${enemy.name} para a distância Perto.`,expiresRound:encounter.round+1,expiresAtCombatantId:actor.id};const freeEffect={id:`core-free-attack:${crypto.randomUUID()}`,name:'Ataque livre de Recuar',detail:`${actor.name} falhou ao Recuar. ${enemy.name} pode fazer Golpe, Estocada ou Soco sem custo de ação; não pode ser Esquivado nem Bloqueado.`,source:actor.id};const combatants=encounter.combatants.map(item=>item.id===actor.id?(()=>{const base=character?syncCombatantFromCharacter(item,character):item;const spent=spendFastAction(base);return{...spent,statusEffects:[...(spent.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-retreated:')),retreatEffect]};})():item.id===enemy.id&&outcome.freeAttack?{...item,statusEffects:[...(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-free-attack:')),freeEffect]}:item);const text=`${actor.name} Recuou de ${enemy.name}: Movimentação ${roll.successes} sucesso(s); moveu-se para Perto${outcome.freeAttack?`, mas ${enemy.name} ganhou um ataque livre sem defesa`:''}${criticalRollConsequenceText(triggers)}.`;const nextEncounter=this.resolveIfFinished({...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text,createdAt:now}].slice(-300),updatedAt:now},now);const world={...current,encounter:nextEncounter,updatedAt:now};await store.put(world);return{world,roll,success:outcome.success,freeAttack:outcome.freeAttack};});this.notify(result.world);return result;
  }

  /** Resolve o ataque livre criado por falha em Recuar. Não consome ação e a vítima não pode Esquivar/Bloquear. */
  static async resolveRetreatFreeAttack(campaignId:string,attackerId:string,input:{attackKind:'strike'|'stab'|'unarmed';gear?:number;weaponDamage:number;criticalTable:'slash'|'puncture'|'blunt';rng?:()=>number}):Promise<{world:WorldState;attackRoll:DiceRoll;resolution:AttackResolution;critical:boolean}>{
    const rng=input.rng||Math.random;const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');const attacker=encounter.combatants.find(item=>item.id===attackerId);const pending=attacker?.statusEffects?.find(effect=>effect.id.startsWith('core-free-attack:'));if(!attacker||attacker.status!=='active'||!pending)throw new Error('Este combatente não possui ataque livre de Recuar pendente.');if(monsterIncapacitated(attacker))throw new Error(`${attacker.name} não pode realizar o ataque livre enquanto estiver incapacitado.`);const targetId=String(pending.source||'');const target=encounter.combatants.find(item=>item.id===targetId);if(!target||target.status!=='active')throw new Error('A vítima do ataque livre não está mais ativa.');const chars=tx.objectStore('characters');let attackerCharacter=attacker.kind==='character'&&attacker.entityId?await chars.get(attacker.entityId):undefined;let targetCharacter=target.kind==='character'&&target.entityId?await chars.get(target.entityId):undefined;if(attackerCharacter)CharacterActionRulesEngine.assertCanAct(attackerCharacter);
      const attackRoll=ForbiddenLandsDiceEngine.roll({base:combatantAttribute(attacker,attackerCharacter,'strength'),skill:combatantSkill(attacker,attackerCharacter,['Melee','Luta']),gear:Math.max(0,Math.trunc(input.gear||0))},{characterId:attackerCharacter?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`retreat-free-attack:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:attackerCharacter?.criticalInjuryStates||attacker.criticalInjuries,skillName:'melee',attributeName:'strength'});await tx.objectStore('rolls').put(attackRoll);let triggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];if(attackerCharacter){const applied=applyCriticalRollConsequences(attackerCharacter,attackRoll);attackerCharacter=applied.character;triggers=applied.triggers;await chars.put(attackerCharacter);}const resolution=CombatRulesEngine.resolveAttack({attackSuccesses:attackRoll.successes,weaponDamage:Math.max(0,Math.trunc(input.weaponDamage||0)),armorValue:target.armor,rng});const critical=CombatRulesEngine.criticalRequired({previousStrength:target.currentStrength,finalDamage:resolution.finalDamage});const strength=Math.max(0,target.currentStrength-resolution.finalDamage);const now=Date.now();let combatants=encounter.combatants.map(item=>item.id===attacker.id?{...(attackerCharacter?syncCombatantFromCharacter(item,attackerCharacter):item),statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==pending.id)}:item.id===target.id?{...item,currentStrength:strength,armor:Math.max(0,item.armor-resolution.armorLoss),status:strength===0?'broken' as const:item.status,brokenAttribute:strength===0?'strength':item.brokenAttribute,statusEffects:critical?[...(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('critical-pending:')),{id:`critical-pending:${crypto.randomUUID()}`,name:'Ferimento grave pendente',detail:`Ataque livre de Recuar Quebrou Força; resolver D66 de ${input.criticalTable==='slash'?'laceração':input.criticalTable==='puncture'?'perfuração':'trauma concussivo'}.`,criticalTable:input.criticalTable}]:item.statusEffects}:item);
      if(targetCharacter){const conditions=strength===0?Array.from(new Set([...(targetCharacter.conditions||[]),'Broken' as const])):targetCharacter.conditions;const marker=`PENDENTE: ferimento grave — tabela de ${input.criticalTable==='slash'?'laceração':input.criticalTable==='puncture'?'perfuração':'trauma concussivo'}`;targetCharacter={...targetCharacter,attributes:{...targetCharacter.attributes,strength:{...targetCharacter.attributes.strength,current:strength}},conditions,brokenAttribute:strength===0?'strength':targetCharacter.brokenAttribute,...(critical?{criticalInjuries:(targetCharacter.criticalInjuries||[]).includes(marker)?targetCharacter.criticalInjuries:[...(targetCharacter.criticalInjuries||[]),marker]}:{})};await chars.put(targetCharacter);combatants=combatants.map(item=>item.id===target.id?syncCombatantFromCharacter(item,targetCharacter!):item);}
      const text=`Ataque livre de ${attacker.name} contra ${target.name}: ${attackRoll.successes} sucesso(s), ${resolution.finalDamage} dano após armadura; ${target.name} Força ${target.currentStrength} → ${strength}${critical?'; ferimento grave pendente':''}${criticalRollConsequenceText(triggers)}.`;const nextEncounter=this.resolveIfFinished({...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,text,createdAt:now}].slice(-300),updatedAt:now},now);const world={...current,encounter:nextEncounter,updatedAt:now};await store.put(world);return{world,attackRoll,resolution,critical};});this.notify(result.world);return result;
  }


  /** FUGIR: ação lenta de Movimentação; sucesso encerra a participação no conflito. */
  static async flee(campaignId:string,actorId:string,input:{distance:'near'|'short'|'long';terrain?:'normal'|'open'|'rough';rng?:()=>number}):Promise<{world:WorldState;roll:DiceRoll;success:boolean;modifier:number}>{
    const rng=input.rng||Math.random;
    const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];
      if(!actor||actor.id!==actorId||actor.status!=='active')throw new Error('Apenas o combatente ativo pode Fugir.');
      if(!actor.slowActionAvailable)throw new Error(`${actor.name} já gastou a ação lenta desta rodada.`);
      if(monsterIncapacitated(actor))throw new Error(`${actor.name} está incapacitado e não pode Fugir.`);
      if(actor.statusEffects?.some(effect=>effect.id.startsWith('core-grappled:')||effect.id.startsWith('core-grappling:')||effect.id.startsWith('monster-grappled:')||effect.id.startsWith('monster-webbed:')||effect.id.startsWith('official-ent-roots:')||effect.id.startsWith('official-ent-embrace:')))throw new Error(`${actor.name} está preso e não pode Fugir do conflito.`);
      const characterStore=tx.objectStore('characters');
      let character=actor.kind==='character'&&actor.entityId?await characterStore.get(actor.entityId):undefined;
      if(character)CharacterActionRulesEngine.assertCanAct(character);
      const modifier=CombatRulesEngine.fleeModifier(input.distance,input.terrain||'normal');
      const roll=ForbiddenLandsDiceEngine.roll(
        {base:combatantAttribute(actor,character,'agility'),skill:combatantSkill(actor,character,['Movement','Movimentação']),gear:0,modifier},
        {characterId:character?.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`flee:${encounter.id}:${encounter.round}:${actor.id}`,rng,criticalInjuries:character?.criticalInjuryStates||actor.criticalInjuries,skillName:'movement',attributeName:'agility'},
      );
      await tx.objectStore('rolls').put(roll);
      let triggers:ReturnType<typeof CriticalInjuryRulesEngine.damageForCheck>=[];
      if(character){const applied=applyCriticalRollConsequences(character,roll);character=applied.character;triggers=applied.triggers;await characterStore.put(character);}
      const outcome=CombatRulesEngine.flee(roll.successes);
      const now=Date.now();
      const combatants=encounter.combatants.map(item=>item.id!==actor.id?item:{...(character?syncCombatantFromCharacter(item,character):item),slowActionAvailable:false,...(outcome.success?{status:'fled' as const}:{})});
      const terrainLabel=input.terrain==='open'?'zona Aberta':input.terrain==='rough'?'zona Acidentada':'terreno normal';
      const distanceLabel=input.distance==='near'?'Perto':input.distance==='short'?'Curta':'Longa';
      const text=`${actor.name} tentou Fugir a distância ${distanceLabel} em ${terrainLabel}: modificador ${modifier>=0?'+':''}${modifier}, ${roll.successes} sucesso(s); ${outcome.success?'escapou do conflito':'permanece no conflito'}${criticalRollConsequenceText(triggers)}.`;
      const nextEncounter=this.resolveIfFinished({...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text,createdAt:now}].slice(-300),updatedAt:now},now);
      const world={...current,encounter:nextEncounter,updatedAt:now};await store.put(world);return{world,roll,success:outcome.success,modifier};
    });
    this.notify(result.world);return result;
  }

  /** DESEMBAINHAR/PEGAR DO CHÃO: pegar item ao Alcance das Mãos custa ação rápida; Saque Rápido pode tornar a arma gratuita. */
  static async pickupDroppedEquipment(campaignId:string,actorId:string,dropId:string,input:{atArmsReach:boolean}):Promise<{world:WorldState;actionCost:'free'|'fast'}>{
    const result=await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];if(!actor||actor.id!==actorId||actor.status!=='active')throw new Error('Apenas o combatente ativo pode pegar equipamento do chão.');
      if(!input.atArmsReach)throw new Error('O item precisa estar ao Alcance das Mãos para ser pego com Desembainhar.');
      if(monsterIncapacitated(actor))throw new Error(`${actor.name} está incapacitado e não pode pegar o item.`);
      if(actor.statusEffects?.some(effect=>effect.id.startsWith('core-grappled:')||effect.id.startsWith('core-grappling:')||effect.id.startsWith('monster-lead-arms:')))throw new Error(`${actor.name} não pode pegar equipamento enquanto está Agarrado, agarrando ou com os braços inutilizados.`);
      const drop=encounter.droppedEquipment?.find(item=>item.id===dropId);if(!drop)throw new Error('O item não está mais disponível no encontro.');
      const characterStore=tx.objectStore('characters');let character=actor.kind==='character'&&actor.entityId?await characterStore.get(actor.entityId):undefined;
      if(character)CharacterActionRulesEngine.assertCanAct(character);
      const talents=(character?.talents||((actor.quickSheet?.talents||{}) as Record<string,number>));const rank=quickDrawRank(talents);
      const encumbranceClass=itemEncumbranceClass(drop.item);const actionCost=CombatRulesEngine.pickupActionCost({collection:drop.collection,encumbranceClass,quickDrawRank:rank});
      if(actionCost==='fast'&&!canSpendFastAction(actor))throw new Error(`${actor.name} não possui mais uma ação que possa ser usada como rápida para pegar o item.`);
      let nextCollection:unknown[];
      if(character){nextCollection=[...((character[drop.collection]||[]) as unknown[]),drop.item];const partial={...character,[drop.collection]:nextCollection};character={...partial,encumbrance:EncumbranceRulesEngine.calculate(partial)} as Character;await characterStore.put(character);}else{nextCollection=[...(((actor.quickSheet?.[drop.collection] as unknown[])||[])),drop.item];}
      const now=Date.now();const combatants=encounter.combatants.map(item=>{if(item.id!==actor.id)return item;const base=character?syncCombatantFromCharacter(item,character):item;const spent=actionCost==='fast'?spendFastAction(base):base;return{...spent,quickSheet:{...(spent.quickSheet||{}),[drop.collection]:nextCollection}};});
      const nextEncounter={...encounter,combatants,droppedEquipment:(encounter.droppedEquipment||[]).filter(item=>item.id!==dropId),log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text:`${actor.name} pegou ${String(drop.item.name||'item')} do chão ao Alcance das Mãos${actionCost==='free'?` sem gastar ação (Saque Rápido ${rank})`:' com uma ação rápida'}.`,createdAt:now}].slice(-300),updatedAt:now};
      const world={...current,encounter:nextEncounter,updatedAt:now};await store.put(world);return{world,actionCost};
    });this.notify(result.world);return result;
  }


  static async resistPossession(campaignId:string,combatantId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];if(!actor||actor.id!==combatantId||actor.kind!=='character'||!actor.entityId||actor.status!=='active')throw new Error('Apenas a personagem ativa pode tentar romper a possessão.');
      const effect=actor.statusEffects?.find(item=>item.id.startsWith('monster-possessed:'));if(!effect)throw new Error(`${actor.name} não está possuída.`);
      if(Number(effect.lastAttemptRound)===encounter.round)throw new Error('A tentativa gratuita de romper a possessão já foi feita nesta rodada.');
      const character=await tx.objectStore('characters').get(actor.entityId);if(!character||character.status==='dead')throw new Error('Ficha da vítima da possessão não encontrada.');
      const normalize=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
      const insight=Object.entries(character.skills||{}).find(([name])=>['insight','discernimento'].includes(normalize(name)))?.[1]||0;
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:Number(insight)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`possessor-resist:${encounter.id}:${encounter.round}:${actor.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits'});
      await tx.objectStore('rolls').put(roll);const freed=roll.successes>0;const now=Date.now();
      const updatedEffects=freed?(actor.statusEffects||[]).filter(item=>!item.id.startsWith('monster-possessed:')):(actor.statusEffects||[]).map(item=>item.id===effect.id?{...item,lastAttemptRound:encounter.round,detail:`Ainda controlada por ${effect.source||'o Possessor'}; a tentativa gratuita de Astúcia + Discernimento desta rodada falhou.`}:item);
      const characterEffects=freed?(character.statusEffects||[]).filter(item=>!item.id.startsWith('monster-possessed:')):(character.statusEffects||[]).map(item=>item.id===effect.id?{...item,lastAttemptRound:encounter.round,detail:`Ainda controlada por ${effect.source||'o Possessor'}; a tentativa gratuita de Astúcia + Discernimento desta rodada falhou.`}:item);
      await tx.objectStore('characters').put({...character,statusEffects:characterEffects});
      const combatants=encounter.combatants.map(item=>item.id===actor.id?{...item,statusEffects:updatedEffects}:item);
      const text=`${actor.name} tenta romper a possessão com Astúcia + Discernimento (${roll.successes} sucesso(s)): ${freed?'recupera o controle':'continua como marionete'}${freed?'.':'; nova tentativa no próximo turno.'}`;
      const next={...current,encounter:{...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(world);return world;
  }

  static async breakFreeWeb(campaignId:string,combatantId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];
      if(!actor||actor.id!==combatantId)throw new Error('Apenas o combatente do turno atual pode tentar se soltar.');
      if(actor.status!=='active')throw new Error('Um combatente inativo não pode escapar da teia.');
      if(monsterIncapacitated(actor))throw new Error(`${actor.name} está incapacitado e não pode tentar escapar.`);
      if(!actor.statusEffects?.some(effect=>effect.id.startsWith('monster-webbed:')))throw new Error(`${actor.name} não está preso na teia.`);
      if(!actor.slowActionAvailable)throw new Error(`${actor.name} já gastou a ação lenta deste turno.`);
      let character:Character|undefined;
      if(actor.kind==='character'&&actor.entityId){
        character=await tx.objectStore('characters').get(actor.entityId);
        if(!character)throw new Error('Ficha do combatente não encontrada.');
        CharacterActionRulesEngine.assertCanAct(character);
      }
      const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
      const skillSource=character?.skills||(actor.quickSheet?.skills as Record<string,number>|undefined)||{};
      const might=Object.entries(skillSource).find(([name])=>['might','forca','potencia'].includes(normalizeSkill(name)))?.[1]||0;
      const strength=character?.attributes.strength.current||Number((actor.quickSheet?.attributes as Record<string,{current?:number}>|undefined)?.strength?.current)||actor.currentStrength;
      const roll=ForbiddenLandsDiceEngine.roll({base:strength,skill:Number(might)||0,gear:0},{characterId:character?.id||null,sceneId:encounter.startedSceneId||encounter.id,checkId:`web-escape:${encounter.id}:${encounter.round}:${actor.id}`,rng,criticalInjuries:character?.criticalInjuryStates,skillName:'might',attributeName:'strength'});
      await tx.objectStore('rolls').put(roll);
      const freed=roll.successes>0;
      const now=Date.now();
      const consequence=character?applyCriticalRollConsequences(character,roll):null;
      if(character&&consequence){
        character={...consequence.character,statusEffects:freed?(consequence.character.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-webbed:')):consequence.character.statusEffects};
        await tx.objectStore('characters').put(character);
      }
      const combatants=encounter.combatants.map(item=>item.id!==actor.id?item:{...(character?syncCombatantFromCharacter(item,character):item),slowActionAvailable:false,statusEffects:freed?(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-webbed:')):item.statusEffects});
      const text=`${actor.name} gastou uma ação lenta para escapar da teia: Força + Potência ${roll.results.map(result=>result.value).join(',')||'—'} (${roll.successes} sucesso${roll.successes===1?'':'s'}); ${freed?'rompeu a teia':'continua preso'}${criticalRollConsequenceText(consequence?.triggers||[])}.`;
      const nextEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text,createdAt:now}].slice(-300),updatedAt:now};
      const next={...current,encounter:nextEncounter,updatedAt:now};
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async breakFreeGrapple(campaignId:string,combatantId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];
      if(!actor||actor.id!==combatantId)throw new Error('Apenas o combatente do turno atual pode tentar escapar do agarrão.');
      if(actor.status!=='active'||monsterIncapacitated(actor))throw new Error('Um combatente inativo ou incapacitado não pode escapar do agarrão.');
      if(actor.statusEffects?.some(effect=>effect.id.startsWith('monster-grappled:fire-wyrm-mouth:')))
        throw new Error('Serpe: não é possível escapar da boca por Potência; cause 4+ dano num único ataque contra a criatura.');
      if(!actor.statusEffects?.some(effect=>effect.id.startsWith('monster-grappled:')))throw new Error(`${actor.name} não está Agarrado.`);
      if(!actor.slowActionAvailable)throw new Error(`${actor.name} já gastou a ação lenta deste turno.`);
      let character:Character|undefined;
      if(actor.kind==='character'&&actor.entityId){character=await tx.objectStore('characters').get(actor.entityId);if(!character)throw new Error('Ficha do combatente não encontrada.');CharacterActionRulesEngine.assertCanAct(character);}
      const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
      const skillSource=character?.skills||(actor.quickSheet?.skills as Record<string,number>|undefined)||{};
      const might=Object.entries(skillSource).find(([name])=>['might','forca','potencia'].includes(normalizeSkill(name)))?.[1]||0;
      const strength=character?.attributes.strength.current||Number((actor.quickSheet?.attributes as Record<string,{current?:number}>|undefined)?.strength?.current)||actor.currentStrength;
      const roll=ForbiddenLandsDiceEngine.roll({base:strength,skill:Number(might)||0,gear:0},{characterId:character?.id||null,sceneId:encounter.startedSceneId||encounter.id,checkId:`grapple-escape:${encounter.id}:${encounter.round}:${actor.id}`,rng,criticalInjuries:character?.criticalInjuryStates,skillName:'might',attributeName:'strength'});
      await tx.objectStore('rolls').put(roll);
      const freed=roll.successes>0;const now=Date.now();
      const consequence=character?applyCriticalRollConsequences(character,roll):null;
      if(character&&consequence){
        character={...consequence.character,statusEffects:freed?(consequence.character.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-grappled:')):consequence.character.statusEffects};
        await tx.objectStore('characters').put(character);
      }
      const combatants=encounter.combatants.map(item=>item.id!==actor.id?item:{...(character?syncCombatantFromCharacter(item,character):item),slowActionAvailable:false,statusEffects:freed?(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-grappled:')):item.statusEffects});
      const text=`${actor.name} gastou uma ação lenta para escapar do agarrão: Força + Potência ${roll.results.map(result=>result.value).join(',')||'—'} (${roll.successes} sucesso${roll.successes===1?'':'s'}); ${freed?'se soltou':'continua Agarrado'}${criticalRollConsequenceText(consequence?.triggers||[])}.`;
      const nextEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text,createdAt:now}].slice(-300),updatedAt:now};const next={...current,encounter:nextEncounter,updatedAt:now};await store.put(next);return next;
    });this.notify(world);return world;
  }

  static async breakFreeFromDrag(campaignId:string,combatantId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');const actor=encounter.combatants[encounter.turnIndex];
      if(!actor||actor.id!==combatantId||actor.kind!=='character'||!actor.entityId||actor.status!=='active')throw new Error('Apenas a personagem ativa pode tentar escapar do arrasto.');
      const dragged=actor.statusEffects?.find(effect=>effect.id.startsWith('monster-dragged:'));if(!dragged)throw new Error(`${actor.name} não está sendo arrastado.`);
      if(!actor.slowActionAvailable)throw new Error('A ação lenta deste turno já foi usada.');const character=await tx.objectStore('characters').get(actor.entityId);
      if(!character||character.status==='dead')throw new Error('Ficha do combatente não encontrada.');CharacterActionRulesEngine.assertCanAct(character);
      const spider=encounter.combatants.find(item=>item.id===dragged.id.slice('monster-dragged:'.length));if(!spider)throw new Error('As aranhas que arrastam a personagem não estão mais no encontro.');
      const might=Object.entries(character.skills||{}).find(([name])=>['might','potencia','forca'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim()))?.[1]||0;
      const victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:Number(might)||0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`spider-drag-escape:${encounter.id}:${encounter.round}:${actor.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'might',attributeName:'strength'});
      const spiderRoll=ForbiddenLandsDiceEngine.roll({base:spider.currentStrength,skill:0,gear:0},{characterId:null,sceneId:encounter.startedSceneId||encounter.id,checkId:`spider-drag-resist:${encounter.id}:${encounter.round}:${actor.id}`,rng});
      await tx.objectStore('rolls').put(victimRoll);await tx.objectStore('rolls').put(spiderRoll);const freed=victimRoll.successes>=spiderRoll.successes;const now=Date.now();
      const consequence=applyCriticalRollConsequences(character,victimRoll);
      const updatedCharacter={...consequence.character,statusEffects:freed?(consequence.character.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-dragged:')):consequence.character.statusEffects};
      await tx.objectStore('characters').put(updatedCharacter);
      const combatants=encounter.combatants.map(item=>item.id!==actor.id?item:{...syncCombatantFromCharacter(item,updatedCharacter),slowActionAvailable:false,statusEffects:freed?(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-dragged:')):item.statusEffects});
      const text=`${actor.name} gastou uma ação lenta para escapar do arrasto: Força + Potência ${victimRoll.successes} sucesso(s) contra Força das aranhas ${spiderRoll.successes}; ${freed?'se soltou':'continua sendo arrastado'}${criticalRollConsequenceText(consequence.triggers)}.`;
      const nextEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,text,createdAt:now}].slice(-300),updatedAt:now};const next={...current,encounter:nextEncounter,updatedAt:now};await store.put(next);return next;
    });this.notify(world);return world;
  }

  static async rescueDrowning(campaignId:string,rescuerId:string,targetId:string):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const rescuer=encounter.combatants[encounter.turnIndex];const target=encounter.combatants.find(item=>item.id===targetId);
      if(!rescuer||rescuer.id!==rescuerId||rescuer.kind!=='character'||!rescuer.entityId||rescuer.status!=='active')throw new Error('Apenas uma personagem ativa no turno atual pode resgatar alguém.');
      if(monsterIncapacitated(rescuer))throw new Error(`${rescuer.name} está incapacitada e não pode resgatar alguém.`);
      if(!target||target.kind!=='character'||!target.entityId)throw new Error('O alvo precisa ser uma personagem com estado de afogamento.');
      if(!target.statusEffects?.some(effect=>effect.id==='drowning'))throw new Error('O alvo não está se afogando.');
      if(!rescuer.slowActionAvailable)throw new Error('A ação lenta deste turno já foi usada.');
      const characters=tx.objectStore('characters');
      const rescuerCharacter=await characters.get(rescuer.entityId);if(!rescuerCharacter)throw new Error('A ficha da personagem resgatadora não está disponível.');
      CharacterActionRulesEngine.assertCanAct(rescuerCharacter);
      if(rescuer.id===target.id)throw new Error('Uma personagem não pode se resgatar enquanto está se afogando.');
      const character=await characters.get(target.entityId);if(!character||character.status==='dead')throw new Error('A ficha do alvo não está disponível para resgate.');
      const rescued=DrowningRulesEngine.rescue(character);const statusEffects=(rescued.statusEffects||[]).filter(effect=>effect.id!=='drowning');await characters.put({...rescued,statusEffects});
      const now=Date.now();const combatants=encounter.combatants.map(item=>item.id===target.id?{...item,...(item.id===rescuer.id?{slowActionAvailable:false}:{}),conditions:[...rescued.conditions],statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!=='drowning'),quickSheet:{...(item.quickSheet||{}),attributes:rescued.attributes,conditions:rescued.conditions,drowningState:undefined}}:item.id===rescuer.id?{...item,slowActionAvailable:false}:item);
      const nextEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:target.id,text:`${rescuer.name} resgatou ${target.name}; o estado de afogamento foi encerrado.`,createdAt:now}].slice(-300),updatedAt:now};const next={...current,encounter:nextEncounter,updatedAt:now};await store.put(next);return next;
    });this.notify(world);return world;
  }

  static async treatBasiliskWarts(campaignId:string,targetCombatantId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const healerCombatant=encounter.combatants[encounter.turnIndex];
      if(!healerCombatant||healerCombatant.kind!=='character'||!healerCombatant.entityId||healerCombatant.status!=='active')throw new Error('Cura exige uma personagem ativa no turno atual.');
      if(monsterIncapacitated(healerCombatant))throw new Error(`${healerCombatant.name} está incapacitada e não pode tratar as verrugas.`);
      if(!healerCombatant.slowActionAvailable)throw new Error('A ação lenta desta rodada já foi usada.');
      const characterStore=tx.objectStore('characters');
      const healer=await characterStore.get(healerCombatant.entityId);
      if(!healer)throw new Error('Ficha da curandeira não encontrada.');
      CharacterActionRulesEngine.assertCanAct(healer);
      const target=encounter.combatants.find(item=>item.id===targetCombatantId);
      if(!target||target.kind!=='character'||!target.entityId||target.status==='dead')throw new Error('Escolha uma personagem viva afetada pelas verrugas.');
      const patient=await characterStore.get(target.entityId);
      if(!patient?.basiliskWarts)throw new Error(`${target.name} não está com verrugas de basilisco.`);
      const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
      const healing=Object.entries(healer.skills||{}).find(([name])=>['healing','cura'].includes(normalizeSkill(name)))?.[1]||0;
      const selfTreatment=healer.id===patient.id;
      const roll=ForbiddenLandsDiceEngine.roll({base:healer.attributes.empathy.current,skill:Number(healing)||0,gear:0,modifier:selfTreatment?-2:0},{characterId:healer.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`basilisk-warts:${encounter.id}:${encounter.round}:${healer.id}:${patient.id}`,rng,criticalInjuries:healer.criticalInjuryStates,skillName:'healing',attributeName:'empathy'});
      await tx.objectStore('rolls').put(roll);
      const cured=roll.successes>0;
      const updatedPatient=cured?BasiliskWartsRulesEngine.cure(patient):patient;
      if(cured)await characterStore.put(updatedPatient);
      const now=Date.now();
      const combatants=encounter.combatants.map(item=>item.id!==healerCombatant.id&&item.id!==target.id?item:{...item,...(item.id===healerCombatant.id?{slowActionAvailable:false}:{}),...(item.id===target.id?{quickSheet:{...(item.quickSheet||{}),attributes:updatedPatient.attributes},statusEffects:cured?(item.statusEffects||[]).filter(effect=>effect.id!=='basilisk-warts'):item.statusEffects}:{})});
      const detail=`${healer.name} tentou tratar as verrugas de ${target.name} com Cura: ${roll.successes} sucesso(s)${selfTreatment?' (autocura, −2)':''}; ${cured?'Empatia restaurada ao valor original':'as verrugas permanecem'}.`;
      const nextEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:healerCombatant.id,text:detail,createdAt:now}].slice(-300),updatedAt:now};
      const next={...current,encounter:nextEncounter,updatedAt:now};
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async treatSkolopendraEggs(campaignId:string,targetCombatantId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const healerCombatant=encounter.combatants[encounter.turnIndex];
      if(!healerCombatant||healerCombatant.kind!=='character'||!healerCombatant.entityId||healerCombatant.status!=='active')throw new Error('O tratamento exige uma personagem ativa no turno atual.');
      if(monsterIncapacitated(healerCombatant))throw new Error(`${healerCombatant.name} está incapacitada e não pode tratar a infestação.`);
      if(!healerCombatant.slowActionAvailable)throw new Error('A ação lenta desta rodada já foi usada.');
      const characterStore=tx.objectStore('characters');const healer=await characterStore.get(healerCombatant.entityId);
      if(!healer)throw new Error('Ficha da curandeira não encontrada.');CharacterActionRulesEngine.assertCanAct(healer);
      const target=encounter.combatants.find(item=>item.id===targetCombatantId);
      if(!target||target.kind!=='character'||!target.entityId||target.status==='dead')throw new Error('Escolha uma personagem viva afetada pelos ovos.');
      const patient=await characterStore.get(target.entityId);const infestation=(target.statusEffects||[]).find(effect=>effect.id.startsWith('monster-eggs:'));
      if(!patient||!infestation)throw new Error(`${target.name} não está afetado pelos ovos da Skolopendra.`);
      const normalizeSkill=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase().trim();
      const healing=Object.entries(healer.skills||{}).find(([name])=>['healing','cura'].includes(normalizeSkill(name)))?.[1]||0;
      const selfTreatment=healer.id===patient.id;
      const roll=ForbiddenLandsDiceEngine.roll({base:healer.attributes.empathy.current,skill:Number(healing)||0,gear:0,modifier:selfTreatment?-2:0},{characterId:healer.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`skolopendra-eggs:${encounter.id}:${encounter.round}:${healer.id}:${patient.id}`,rng,criticalInjuries:healer.criticalInjuryStates,skillName:'healing',attributeName:'empathy'});
      await tx.objectStore('rolls').put(roll);const cured=roll.successes>0;const now=Date.now();
      if(cured)await characterStore.put({...patient,statusEffects:(patient.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-eggs:'))});
      const combatants=encounter.combatants.map(item=>item.id!==healerCombatant.id&&item.id!==target.id?item:{...item,...(item.id===healerCombatant.id?{slowActionAvailable:false}:{}),...(item.id===target.id&&cured?{statusEffects:(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-eggs:'))}:{})});
      const detail=`${healer.name} tentou tratar os ovos da Skolopendra em ${target.name} com Cura: ${roll.successes} sucesso(s)${selfTreatment?' (autocura, −2)':''}; ${cured?'a infestação foi removida':'os ovos permanecem'}.`;
      const nextEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:healerCombatant.id,text:detail,createdAt:now}].slice(-300),updatedAt:now};const next={...current,encounter:nextEncounter,updatedAt:now};
      await store.put(next);return next;
    });
    this.notify(world);return world;
  }

  static async freeConsumedAlly(campaignId:string,targetCombatantId:string):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const rescuer=encounter.combatants[encounter.turnIndex];if(!rescuer||rescuer.kind!=='character'||!rescuer.entityId||rescuer.status!=='active')throw new Error('A libertação exige uma personagem ativa no turno atual.');
      if(monsterIncapacitated(rescuer))throw new Error(`${rescuer.name} está incapacitada e não pode libertar um aliado.`);if(!rescuer.slowActionAvailable)throw new Error('A ação lenta desta rodada já foi usada.');
      const patient=encounter.combatants.find(item=>item.id===targetCombatantId);const consumed=patient?.statusEffects?.find(effect=>effect.id.startsWith('monster-consumed:'));
      if(!patient||patient.kind!=='character'||!patient.entityId||!consumed)throw new Error('O alvo não está preso dentro de um monstro.');
      if(!String(consumed.source||'').toLocaleLowerCase().includes('shapeshifter'))throw new Error('A regra de resgate externo só se aplica à vítima do ataque devorador do Metamorfo.');
      if(rescuer.id===patient.id)throw new Error('A personagem presa não pode libertar a si mesma.');
      const characters=tx.objectStore('characters');
      const rescuerCharacter=await characters.get(rescuer.entityId);if(!rescuerCharacter)throw new Error('A ficha da personagem resgatadora não está disponível.');
      CharacterActionRulesEngine.assertCanAct(rescuerCharacter);
      const character=await characters.get(patient.entityId);if(!character||character.status==='dead')throw new Error('A vítima não está disponível para resgate.');
      await characters.put({...character,statusEffects:(character.statusEffects||[]).filter(effect=>effect.id!==consumed.id)});
      const now=Date.now();const combatants=encounter.combatants.map(item=>item.id===rescuer.id?{...item,slowActionAvailable:false}:item.id===patient.id?{...item,statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==consumed.id)}:item);
      const nextEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:patient.id,text:`${rescuer.name} libertou ${patient.name} de dentro do Metamorfo com uma ação lenta.`,createdAt:now}].slice(-300),updatedAt:now};const next={...current,encounter:nextEncounter,updatedAt:now};await store.put(next);return next;
    });this.notify(world);return world;
  }

  static async spendReactiveFastAction(campaignId: string, combatantId: string, reason = 'defesa reativa'): Promise<WorldState> {
    return this.updateCombatActions(campaignId, [combatantId], (encounter, now) => {
      const target = encounter.combatants.find(item => item.id === combatantId);
      if (!target) throw new Error('Combatente defensor não encontrado.');
      if (target.status !== 'active') throw new Error('O defensor não está ativo.');
      if (!canSpendFastAction(target)) throw new Error(`${target.name} não possui ação disponível que possa ser usada como rápida nesta rodada.`);
      return {
        ...encounter,
        combatants: encounter.combatants.map(item => item.id === combatantId ? spendFastAction(item) : item),
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${target.name} gastou uma ação em ${reason}.`, createdAt: now }].slice(-300),
        updatedAt: now,
      };
    });
  }

  /** Recuperação imediata de personagem Acabada: Cura para Força/Agilidade, Atuação para Astúcia/Empatia. */
  static async recoverBroken(campaignId:string, healerId:string, targetId:string, input:{enemyAtArmsReach:boolean;rng?:()=>number}):Promise<{world:WorldState;roll:DiceRoll;recovered:number;success:boolean}>{
    const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const healerCombatant=encounter.combatants[encounter.turnIndex];
      if(!healerCombatant||healerCombatant.id!==healerId||healerCombatant.kind!=='character'||!healerCombatant.entityId||healerCombatant.status!=='active')throw new Error('A recuperação exige a personagem ativa do turno atual.');
      if(input.enemyAtArmsReach)throw new Error('Cura/Atuação para recuperar uma personagem Acabada não pode ser realizada com inimigo Ao Alcance das Mãos.');
      if(!healerCombatant.slowActionAvailable)throw new Error(`${healerCombatant.name} já gastou a ação lenta desta rodada.`);
      const targetCombatant=encounter.combatants.find(item=>item.id===targetId);
      if(!targetCombatant||targetCombatant.kind!=='character'||!targetCombatant.entityId||targetCombatant.status!=='broken'||!targetCombatant.brokenAttribute)throw new Error('Escolha uma personagem Acabada com atributo Quebrado.');
      if(targetCombatant.id===healerCombatant.id)throw new Error('A recuperação imediata exige outra pessoa; autocura de ferimento letal é tratada pelo fluxo de Ferimentos Graves.');
      const characters=tx.objectStore('characters');
      const healer=await characters.get(healerCombatant.entityId);const patient=await characters.get(targetCombatant.entityId);
      if(!healer||!patient)throw new Error('Ficha da curandeira ou paciente não encontrada.');
      CharacterActionRulesEngine.assertCanAct(healer);
      const attribute=targetCombatant.brokenAttribute;
      const skill=CombatRulesEngine.recoverySkillForBroken(attribute);
      const attemptId=`core-recovery-attempt:${healer.id}:${attribute}`;
      if((patient.statusEffects||[]).some(effect=>effect.id===attemptId))throw new Error(`${healer.name} já tentou recuperar ${patient.name} desta condição Acabada.`);
      const skillRank=skill==='healing'?normalizedSkillRank(healer,['Healing','Cura']):normalizedSkillRank(healer,['Performance','Atuação','Performance']);
      const sceneId=encounter.startedSceneId||encounter.id;
      const roll=ForbiddenLandsDiceEngine.roll({base:healer.attributes.empathy.current,skill:Number(skillRank)||0,gear:0},{characterId:healer.id,sceneId,checkId:`recover-broken:${encounter.id}:${encounter.round}:${healer.id}:${patient.id}:${attribute}`,rng:input.rng,criticalInjuries:healer.criticalInjuryStates,skillName:skill,attributeName:'empathy'});
      await tx.objectStore('rolls').put(roll);
      const consequence=applyCriticalRollConsequences(healer,roll);const healedHealer={...consequence.character,updatedAt:Date.now()};await characters.put(healedHealer);
      const recovered=Math.max(0,Math.min(patient.attributes[attribute].max,roll.successes));
      const success=recovered>0;
      let nextPatient:Character={...patient,statusEffects:success?(patient.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-recovery-attempt:')):[...(patient.statusEffects||[]).filter(effect=>effect.id!==attemptId),{id:attemptId,label:'Tentativa de recuperação usada',detail:`${healer.name} já tentou recuperar ${ATTRIBUTE_LABEL(attribute)} nesta condição Acabada.`}],updatedAt:Date.now()};
      if(success){
        nextPatient={...nextPatient,attributes:{...nextPatient.attributes,[attribute]:{...nextPatient.attributes[attribute],current:recovered}},brokenAttribute:undefined,conditions:(nextPatient.conditions||[]).filter(condition=>condition!=='Broken')};
      }
      await characters.put(nextPatient);
      const now=Date.now();
      let combatants=encounter.combatants.map(item=>item.id===healerCombatant.id?{...syncCombatantFromCharacter(item,healedHealer),slowActionAvailable:false}:item.id===targetCombatant.id?syncCombatantFromCharacter(item,nextPatient):item);
      const detail=`${healer.name} usou ${skill==='healing'?'Cura':'Atuação'} para recuperar ${patient.name}: ${roll.successes} sucesso(s). ${success?`${ATTRIBUTE_LABEL(attribute)} recuperou ${recovered} ponto(s) e a condição Acabada terminou.`:'A tentativa falhou; a mesma pessoa não pode tentar novamente nesta condição Acabada.'}${criticalRollConsequenceText(consequence.triggers)}`;
      const updated=this.resolveIfFinished({...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:healerCombatant.id,text:detail,createdAt:now}].slice(-300),updatedAt:now},now);
      const world={...current,encounter:updated,updatedAt:now};await store.put(world);return{world,roll,recovered,success};
    });
    this.notify(result.world);return result;
  }

  /** Golpe de Misericórdia contra alvo indefeso, incluindo os três ranks de Sangue Frio. */
  static async coupDeGrace(campaignId:string, attackerId:string, targetId:string, rng:()=>number=Math.random):Promise<{world:WorldState;roll:DiceRoll|null;killed:boolean}>{
    const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');const current=WorldStateService.normalize(await store.get(campaignId),campaignId);const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const attacker=encounter.combatants[encounter.turnIndex];if(!attacker||attacker.id!==attackerId||attacker.kind!=='character'||!attacker.entityId||attacker.status!=='active')throw new Error('Apenas a personagem ativa pode executar o Golpe de Misericórdia.');
      const target=encounter.combatants.find(item=>item.id===targetId);if(!target||target.status!=='broken'||!['strength','agility'].includes(String(target.brokenAttribute||'')))throw new Error('Golpe de Misericórdia exige alvo indefeso com Força ou Agilidade Acabada.');
      if(target.side===attacker.side)throw new Error('Escolha um oponente indefeso.');
      const chars=tx.objectStore('characters');const character=await chars.get(attacker.entityId);if(!character)throw new Error('Ficha do atacante não encontrada.');CharacterActionRulesEngine.assertCanAct(character);
      const targetCharacter=target.kind==='character'&&target.entityId?await chars.get(target.entityId):undefined;
      const targetWits=targetCharacter?targetCharacter.attributes.wits.current:combatantAttribute(target,undefined,'wits');
      const intelligent=target.kind==='npc'||targetWits>0;
      const rank=combatantTalentRank(attacker,character,'Sangue Frio');const profile=CombatRulesEngine.coupDeGraceProfile(rank,intelligent);
      if(profile.willpowerCost>character.willpowerPoints)throw new Error('Falta Força de Vontade para completar o Golpe de Misericórdia.');
      let roll:DiceRoll|null=null;let killed=true;
      if(profile.empathyRollRequired){
        roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.empathy.current,skill:0,gear:0},{characterId:character.id,sceneId:encounter.startedSceneId||encounter.id,checkId:`coup-de-grace:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'empathy',attributeName:'empathy'});
        await tx.objectStore('rolls').put(roll);killed=roll.successes===0;
      }
      const now=Date.now();let updatedAttacker=character;
      if(killed){
        const empathy=Math.max(0,Math.min(character.attributes.empathy.max,character.attributes.empathy.current-profile.empathyDamage+profile.empathyRecovery));
        const empathyBroken=profile.empathyDamage>0&&character.attributes.empathy.current>0&&empathy===0;updatedAttacker={...character,willpowerPoints:Math.max(0,character.willpowerPoints-profile.willpowerCost),attributes:{...character.attributes,empathy:{...character.attributes.empathy,current:empathy}},...(empathyBroken?{brokenAttribute:'empathy' as const,conditions:[...new Set([...(character.conditions||[]),'Broken' as const])]}:{}),updatedAt:now};await chars.put(updatedAttacker);
        if(targetCharacter)await chars.put({...targetCharacter,status:'dead',updatedAt:now});
      }
      let combatants=encounter.combatants.map(item=>item.id===attacker.id?syncCombatantFromCharacter(item,updatedAttacker):item.id===target.id&&killed?{...item,status:'dead' as const,fastActionAvailable:false,slowActionAvailable:false,quickSheet:{...(item.quickSheet||{}),status:'dead'}}:item);
      const text=killed?`${attacker.name} executou Golpe de Misericórdia em ${target.name}${rank?` com Sangue Frio ${rank}`:''}${profile.willpowerCost?`; −${profile.willpowerCost} PFV`:''}${profile.empathyDamage?`; −${profile.empathyDamage} Empatia`:''}${profile.empathyRecovery?`; +${profile.empathyRecovery} Empatia`:''}.`:`${attacker.name} hesitou no Golpe de Misericórdia contra ${target.name}: a rolagem de Empatia teve ${roll?.successes||0} sucesso(s), portanto o alvo não foi morto.`;
      const updated=this.resolveIfFinished({...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:attacker.id,text,createdAt:now}].slice(-300),updatedAt:now},now);const world={...current,encounter:updated,updatedAt:now};await store.put(world);return{world,roll,killed};
    });this.notify(result.world);return result;
  }

  static async markPendingCritical(campaignId: string, combatantId: string, table: 'slash' | 'puncture' | 'blunt' | 'horror' | 'other'): Promise<WorldState> {
    const labels = { slash: 'laceração', puncture: 'perfuração', blunt: 'trauma concussivo', horror: 'horror', other: 'outros' } as const;
    const world = await withTransaction(['world_state', 'characters'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      if (!current.encounter || current.encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const target = current.encounter.combatants.find(item => item.id === combatantId);
      if (!target) throw new Error('Combatente não encontrado.');
      const now = Date.now();
      const marker = `PENDENTE: ferimento grave — tabela de ${labels[table]}`;
      const combatants = current.encounter.combatants.map(item => item.id === combatantId ? {
        ...item,
        statusEffects: [...(item.statusEffects || []).filter(effect => !effect.id.startsWith('critical-pending:')), { id: `critical-pending:${crypto.randomUUID()}`, name: 'Ferimento grave pendente', detail: table === 'other' ? 'Resolver dano atípico conforme a regra de ferimentos graves.' : `Resolver D66 na tabela de ${labels[table]}.`, criticalTable:table }],
      } : item);
      if (target.kind === 'character' && target.entityId) {
        const characterStore = tx.objectStore('characters');
        const character = await characterStore.get(target.entityId);
        if (character && !(character.criticalInjuries || []).includes(marker)) await characterStore.put({ ...character, criticalInjuries: [...(character.criticalInjuries || []), marker] });
      }
      const encounter = { ...current.encounter, combatants, log: [...current.encounter.log, { id: crypto.randomUUID(), round: current.encounter.round, combatantId, text: `${target.name} ficou Acabado por dano de Força; ${marker}.`, createdAt: now }].slice(-300), updatedAt: now };
      const next = { ...current, encounter, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async resolvePendingCritical(
    campaignId: string,
    combatantId: string,
    optionsOrRng: (() => number) | {
      executionerChoice?: number;
      luckyInvert?: boolean;
      luckyReroll?: boolean;
      luckyCancel?: boolean;
      attackerId?: string;
      rng?: () => number;
    } = {}
  ): Promise<CriticalInjuryState | null> {
    const options = typeof optionsOrRng === 'function' ? { rng: optionsOrRng } : optionsOrRng;
    const rng = options.rng || Math.random;
    const result = await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const target=encounter.combatants.find(item=>item.id===combatantId);
      if(!target)throw new Error('Combatente não encontrado.');
      const pending=(target.statusEffects||[]).find(effect=>effect.id.startsWith('critical-pending:'));
      if(!pending?.criticalTable)throw new Error('Este combatente não tem ferimento grave pendente.');
      const table=pending.criticalTable;

      const charStore = tx.objectStore('characters');
      const targetCharacter = target.kind === 'character' && target.entityId ? await charStore.get(target.entityId) : undefined;
      const luckyRank = combatantTalentRank(target, targetCharacter, 'Sortudo');

      const attackerId = options.attackerId || pending.source;
      const attacker = encounter.combatants.find(item => item.id === attackerId);
      const attackerCharacter = attacker?.kind === 'character' && attacker.entityId ? await charStore.get(attacker.entityId) : undefined;
      const executionerRank = attacker ? combatantTalentRank(attacker, attackerCharacter, 'Carrasco') : 0;

      // Sortudo 3 cancel
      if (luckyRank >= 3 && options.luckyCancel) {
        const now = Date.now();
        const combatants = encounter.combatants.map(item => item.id === combatantId ? {
          ...item,
          statusEffects: (item.statusEffects || []).filter(effect => !effect.id.startsWith('critical-pending:')),
        } : item);
        if (targetCharacter) {
          await charStore.put({
            ...targetCharacter,
            criticalInjuries: (targetCharacter.criticalInjuries || []).filter((v: string) => !v.startsWith('PENDENTE: ferimento grave')),
          });
        }
        const updatedEncounter = this.resolveIfFinished({
          ...encounter,
          combatants,
          log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${target.name} usou Sortudo (rank 3) para cancelar o ferimento grave pendente!`, createdAt: now }].slice(-300),
          updatedAt: now,
        }, now);
        await store.put({ ...current, encounter: updatedEncounter, updatedAt: now });
        return null;
      }

      let rolled: { roll: number; entry: ReturnType<typeof CriticalInjuryRulesEngine.get> } | null = null;
      if (table !== 'other') {
        const d6 = () => 1 + Math.floor(Math.max(0, Math.min(0.999999999, rng())) * 6);
        const rollD66 = () => d6() * 10 + d6();
        let roll1 = rollD66();
        let chosen = roll1;
        const explicitExecutionerChoice = executionerRank >= 3 && options.executionerChoice !== undefined;
        if (executionerRank >= 1) {
          const roll2 = rollD66();
          chosen = CombatRulesEngine.executionerResolveChoice(roll1, roll2, executionerRank, options.executionerChoice);
        }
        if (explicitExecutionerChoice && (options.luckyReroll || options.luckyInvert)) {
          throw new Error('Escolha explícita de Carrasco 3 não pode ser combinada com rerrolagem/inversão de Sortudo na mesma resolução.');
        }
        if (!explicitExecutionerChoice && luckyRank >= 1 && options.luckyReroll) {
          chosen = rollD66();
        }
        if (!explicitExecutionerChoice && luckyRank >= 2 && options.luckyInvert) {
          chosen = CombatRulesEngine.luckyInvertRoll(chosen);
        }
        const entry = CriticalInjuryRulesEngine.get(table, chosen);
        rolled = { roll: chosen, entry };
      }

      const entry = rolled?.entry || CriticalInjuryRulesEngine.otherInjuries()[0];
      const now = Date.now();
      const state: CriticalInjuryState = {
        id: crypto.randomUUID(),
        table,
        roll: rolled?.roll ?? null,
        ruleId: entry.id,
        name: entry.name,
        lethal: entry.lethal,
        deathTimer: CriticalInjuryRulesEngine.rollDeathTimer(entry, rng),
        healingTime: CriticalInjuryRulesEngine.rollHealing(entry, rng),
        effects: entry.effects,
        sourcePage: entry.sourcePage,
        status: 'active',
        createdAt: now,
      };
      if (state.deathTimer && 'amount' in state.deathTimer) state.deathTimeRemaining = { amount: state.deathTimer.amount, unit: state.deathTimer.unit };
      if (state.healingTime && 'amount' in state.healingTime) state.healingDaysRemaining = state.healingTime.amount;
      const healingLabel = state.healingTime === null ? 'sem prazo de cura indicado' : 'amount' in state.healingTime ? `${state.healingTime.amount} ${state.healingTime.unit}` : 'permanente';
      const detail = `${entry.name}${state.roll === null ? '' : ` (D66 ${state.roll})`}; ${entry.lethal ? 'ferimento letal' : 'ferimento não letal'}; cura: ${healingLabel}${state.deathTimer?.immediate ? ' — morte imediata' : state.deathTimer && 'amount' in state.deathTimer ? ` — prazo ${state.deathTimer.amount} ${state.deathTimer.unit}` : ''}.${executionerRank >= 1 ? ` (Carrasco rank ${executionerRank})` : ''}${luckyRank >= 1 && (options.luckyInvert || options.luckyReroll) ? ` (Sortudo rank ${luckyRank})` : ''}`;
      const combatants = encounter.combatants.map(item => item.id === combatantId ? {
        ...item,
        status: state.deathTimer?.immediate ? 'dead' as const : item.status,
        ...(state.deathTimer?.immediate ? { quickSheet: { ...(item.quickSheet || {}), status: 'dead' } } : {}),
        criticalInjuries: [...(item.criticalInjuries || []), state],
        statusEffects: [...(item.statusEffects || []).filter(effect => !effect.id.startsWith('critical-pending:')), { id: `critical-injury:${state.id}`, name: `Ferimento grave: ${entry.name}`, detail, criticalTable: table, criticalInjury: state }],
      } : item);
      if (target.kind === 'character' && target.entityId) {
        const characters = tx.objectStore('characters');
        const character = await characters.get(target.entityId);
        if (character) {
          const marker = `${table}:${state.roll ?? 'other'}:${entry.name}`;
          await characters.put({
            ...character,
            ...(state.deathTimer?.immediate ? { status: 'dead' as const } : {}),
            criticalInjuries: [...(character.criticalInjuries || []).filter((value: string) => !value.startsWith('PENDENTE: ferimento grave')), marker],
            criticalInjuryStates: [...(character.criticalInjuryStates || []), state],
          });
        }
      }
      const updatedEncounter = this.resolveIfFinished({
        ...encounter,
        combatants,
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${target.name}: ${detail} Fonte: Livro do Jogador, p. ${entry.sourcePage}.`, createdAt: now }].slice(-300),
        updatedAt: now,
      }, now);
      await store.put({ ...current, encounter: updatedEncounter, updatedAt: now });
      return state;
    });
    const world = await WorldStateService.get(campaignId);
    this.notify(world);
    return result;
  }

  /** Commits a basic attack's actions, dice, damage, armor loss, critical marker and crossbow consumption together. */
  static async commitBasicAttackResult(campaignId: string, attackerId: string, targetId: string, input: {
    attackRoll: DiceRoll; defenseRoll?: DiceRoll | null; aimed?: boolean; aimAtFireWyrmMouth?: boolean; defenseReason?: string; defenseKind?: 'dodge'|'block';
    weaponDamage: number; criticalTable: 'slash' | 'puncture' | 'blunt' | 'horror' | 'other';
    fireAttack?: boolean; crossbowWeaponIndex?: number; poisonWeaponIndex?: number; helperId?: string; rng?: () => number; actionCost?: 'slow'|'fast'; ignoreProne?: boolean; grappledAttack?: boolean; attackKind?: 'strike'|'stab'|'unarmed'|'ranged'; weaponIndex?: number; runWithShot?: boolean;
    chargeDeclared?: boolean; targetMount?: boolean; magicalAttack?: boolean;
  }): Promise<{ world: WorldState; resolution: AttackResolution; critical: boolean; poisonRoll?: DiceRoll }> {
    const committed = await withTransaction(['world_state', 'characters', 'rolls', 'campaign'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const attacker = encounter.combatants[encounter.turnIndex];
      if (!attacker || attacker.id !== attackerId || attacker.status !== 'active') throw new Error('Apenas o combatente ativo pode atacar.');
      if(attacker.quickSheet?.officialBookMonster===true)throw new Error('Este monstro oficial precisa usar sua tabela D6 R88.');
      if (attacker.statusEffects?.some(effect => effect.id === 'condition:prone') && !input.ignoreProne) throw new Error(`${attacker.name} está Caído e precisa se levantar antes de atacar.`);
      const coreGrappling = attacker.statusEffects?.find(effect => effect.id.startsWith('core-grappling:'));
      const coreGrappled = attacker.statusEffects?.find(effect => effect.id.startsWith('core-grappled:'));
      if (coreGrappled) throw new Error(`${attacker.name} está Agarrado; a única ação lenta permitida é Libertar-se.`);
      if (coreGrappling && !input.grappledAttack) throw new Error(`${attacker.name} está agarrando um oponente; sua única ação ofensiva é Ataque Atracado.`);
      if (monsterIncapacitated(attacker) || attacker.statusEffects?.some(effect => effect.id.startsWith('monster-lead-arms:'))) throw new Error(`${attacker.name} está incapacitado e não pode atacar.`);
      const actionCost = input.actionCost || 'slow';
      const attackerRuleCharacter=attacker.kind==='character'&&attacker.entityId?await tx.objectStore('characters').get(attacker.entityId):undefined;
      const activeCampaign=await tx.objectStore('campaign').get(campaignId);
      const partyRoster=Array.isArray(activeCampaign?.partyCharacterIds)?activeCampaign.partyCharacterIds as string[]:undefined;
      if(attacker.kind==='character')EncounterCampaignGuard.assertCharacter(attackerRuleCharacter,campaignId,'atacante',
        {partyRoster,partySide:attacker.side==='party'});
      const fastShooterRank = combatantTalentRank(attacker, attackerRuleCharacter, 'Atirador Veloz');
      if (actionCost === 'fast' && !input.grappledAttack && !(input.attackKind === 'ranged' && fastShooterRank >= 3)) throw new Error('Ataque rápido só é permitido para Ataque Atracado ou Atirador Veloz 3 com arma à distância.');
      if(input.runWithShot&&!(input.attackKind==='ranged'&&CombatRulesEngine.fastShooterCanRunWithShot(fastShooterRank)))throw new Error('Atirar e Correr na mesma ação exige Atirador Veloz 2+.');
      if (actionCost === 'slow' && !attacker.slowActionAvailable) throw new Error(`${attacker.name} já gastou a ação lenta desta rodada.`);
      const fastNeeded = (actionCost === 'fast' ? 1 : 0) + (input.aimed ? 1 : 0);
      if (fastActionSlots(attacker) < fastNeeded) throw new Error(`${attacker.name} não possui ações suficientes para ${input.aimed ? 'Mirar e atacar' : 'realizar este ataque rápido'}.`);
      const target = encounter.combatants.find(item => item.id === targetId);
      if (!target || target.status !== 'active') throw new Error('O alvo não está ativo no encontro.');
      const insideMouth=Boolean(attacker.statusEffects?.some(effect=>effect.id===`monster-grappled:fire-wyrm-mouth:${target.id}`));
      const aimedFireWyrmMouth=BloodmarchBetaFireWyrmWeakSpotEngine.validate(target,encounter.id,Boolean(input.aimAtFireWyrmMouth),input.attackRoll.fireWyrmMouthAim);
      if(attacker.statusEffects?.some(effect=>effect.id.startsWith('monster-grappled:fire-wyrm-mouth:'))&&!insideMouth)
        throw Error('Quem está na boca da Serpe só pode atacar a criatura que o engoliu.');
      const targetRuleCharacter=target.kind==='character'&&target.entityId?await tx.objectStore('characters').get(target.entityId):undefined;
      if(target.kind==='character')EncounterCampaignGuard.assertCharacter(targetRuleCharacter,campaignId,'alvo',
        {partyRoster,partySide:target.side==='party'});
      if (input.grappledAttack && coreGrappling && !coreGrappling.id.endsWith(`:${target.id}`)) throw new Error('Ataque Atracado só pode atingir o oponente que está sendo agarrado.');
      
      // Validação de arma montado
      const horseRiderRank = combatantTalentRank(attacker, attackerRuleCharacter, 'Mestre da Montaria') || combatantTalentRank(attacker, attackerRuleCharacter, 'Caminho do Cavaleiro');
      const attackerWeapons = combatantWeapons(attacker, attackerRuleCharacter);
      const chosenWeapon = Number.isInteger(input.weaponIndex) ? attackerWeapons[Number(input.weaponIndex)] : undefined;
      const chosenWeaponName = String(chosenWeapon?.name || '');
      const metalAttackModifier=BloodmarchBetaCombatEngine.incomingMetalAttackModifier(target.statusEffects||[],chosenWeapon,(targetRuleCharacter?.armor||target.quickSheet?.armor||[]) as any[]);
      if((target.statusEffects||[]).some(effect=>['magic:bm-attract','magic:bm-repel'].includes(effect.id))
        &&input.attackRoll.magicMetalModifier!==metalAttackModifier)
        throw new Error('Rolagem desatualizada: modificador de magnetismo deve ser calculado sobre arma e alvo persistidos.');
      const harpoonTalent = attackerRuleCharacter
        ? BitterReachTalentRulesEngine.harpoonAttack(attackerRuleCharacter, chosenWeaponName, input.attackKind || '')
        : { modifier: 0, bleedOnDamage: false, artifactDie: null };
      if (harpoonTalent.artifactDie && !input.attackRoll.results.some(die => die.type === 'artifact' && die.sides === 8)) {
        throw new Error('Arpoador 3 exige o D8 de Artefato na rolagem física de ataque.');
      }
      if (attacker.mounted && chosenWeapon) {
        const isTwoHanded = CriticalInjuryRulesEngine.weaponRequiresTwoHands(chosenWeapon);
        if (!CombatRulesEngine.canUseWeaponMounted(isTwoHanded, chosenWeaponName, horseRiderRank)) {
          if (/arco|bow/i.test(chosenWeaponName)) throw new Error('Disparar arco montado exige Mestre da Montaria 2+.');
          throw new Error('Armas de duas mãos proibidas montado, exceto lanças.');
        }
      }

      // Restrições de Fúria Berserker no atacante
      if (attacker.rageState?.active && input.attackKind === 'ranged') {
        throw new Error('Personagem em Fúria Berserker não pode realizar ataques à distância.');
      }
      if (attacker.rageState?.active) {
        const enemies = encounter.combatants.filter(item => item.side !== attacker.side && item.status === 'active');
        const positioned = enemies.filter(item => Number.isFinite(item.x) && Number.isFinite(item.y));
        if (positioned.length > 0 && Number.isFinite(attacker.x) && Number.isFinite(attacker.y)) {
          const distanceSq = (item: EncounterCombatant) => Math.pow(Number(item.x) - Number(attacker.x), 2) + Math.pow(Number(item.y) - Number(attacker.y), 2);
          const nearest = Math.min(...positioned.map(distanceSq));
          const nearestIds = new Set(positioned.filter(item => distanceSq(item) === nearest).map(item => item.id));
          if (!nearestIds.has(target.id)) throw new Error('Fúria Berserker obriga atacar um inimigo mais próximo enquanto houver posição tática conhecida.');
        }
      }
      const berserkerMeleeBonus = attacker.rageState?.active && input.attackKind !== 'ranged'
        ? CombatRulesEngine.berserkerMeleeDamageBonus(true)
        : 0;

      // Bônus de Investida (Charge)
      const chargeBonus = CombatRulesEngine.chargeDamageBonus({
        chargeDeclared: input.chargeDeclared,
        mounted: Boolean(attacker.mounted),
        horseRiderRank,
        weaponName: chosenWeaponName,
      });

      if(input.attackKind==='ranged'&&Number.isInteger(input.weaponIndex)){
        const needsPrepare=CombatRulesEngine.rangedRequiresPreparation(chosenWeaponName,fastShooterRank);
        if(needsPrepare&&fastShooterRank<1&&!attacker.statusEffects?.some(effect=>effect.id===`core-ranged-prepared:${input.weaponIndex}:${encounter.round}`))throw new Error('Arco ou funda precisam ser preparados com uma ação rápida antes de Atirar, salvo Atirador Veloz 1+.');
      }
      const windUpEffect=attacker.statusEffects?.find(effect=>effect.id.startsWith('core-windup:'));
      let windUpBonus=0;
      if(windUpEffect){
        const windUpWeaponIndex=Number(windUpEffect.id.split(':').at(-1));
        const validAttack=input.attackKind==='strike'||input.attackKind==='stab';
        if(!validAttack||!Number.isInteger(input.weaponIndex)||input.weaponIndex!==windUpWeaponIndex)throw new Error('Após Arremeter Arma, o próximo ataque deve ser imediatamente Golpear ou Estocar com a mesma arma pesada.');
        windUpBonus=CombatRulesEngine.windUpDamageBonus({heavyWeapon:true,strikeOrStab:true,sameRound:true});
      }
      const hypnoticControl=attacker.statusEffects?.find(effect=>effect.id.startsWith('monster-hypnotized:'));
      if(hypnoticControl&&(target.id===attacker.id||target.side!==attacker.side||target.kind!=='character'))throw new Error('Sob Olhar Hipnótico, a vítima deve atacar outro aventureiro do grupo.');
      if (input.defenseRoll && input.defenseKind === 'dodge' && !defenderDodgePlan(target, targetRuleCharacter, encounter.round).allowed) throw new Error(`${target.name} não possui uma ação disponível para Esquivar.`);
      if (input.defenseRoll && input.defenseKind === 'block' && !defenderBlockPlan(target, targetRuleCharacter, encounter.round).allowed) throw new Error(`${target.name} não possui uma ação disponível para Bloquear.`);
      const helper = input.helperId ? encounter.combatants.find(item => item.id === input.helperId) : undefined;
      if (input.helperId && (!helper || helper.id === attacker.id || helper.id === target.id || helper.status !== 'active' || helper.side !== attacker.side)) throw new Error('Ajudante precisa ser outro aliado ativo no encontro e não pode ser o alvo.');
      if (helper && !helper.slowActionAvailable) throw new Error(`${helper.name} já gastou a ação lenta desta rodada e não pode ajudar.`);
      if (helper && helper.statusEffects?.some(effect => effect.id === 'condition:prone' || effect.id.startsWith('monster-lead-arms:'))) throw new Error(`${helper.name} não pode ajudar enquanto estiver Caído ou com Braços Pesados.`);
      const sceneId=encounter.startedSceneId||encounter.id;
      const expectedAttackerCharacterId=attacker.kind==='character'?attacker.entityId||null:null;
      if(!input.attackRoll.id||input.attackRoll.sceneId!==sceneId||input.attackRoll.characterId!==expectedAttackerCharacterId||input.attackRoll.successes!==ForbiddenLandsDiceEngine.countSuccesses(input.attackRoll.results))throw new Error('A rolagem de ataque não pertence ao atacante/encontro atual ou tem sucessos inconsistentes.');
      const recordedHelpers=input.attackRoll.assistedBy||[];
      if((helper&&(recordedHelpers.length!==1||recordedHelpers[0]!==helper.id))||(!helper&&recordedHelpers.length>0))throw new Error('A ajuda registrada na rolagem precisa corresponder ao ajudante selecionado e à ação consumida.');
      if(input.defenseRoll){
        const expectedDefenderCharacterId=target.kind==='character'?target.entityId||null:null;
        if(!input.defenseRoll.id||input.defenseRoll.sceneId!==sceneId||input.defenseRoll.characterId!==expectedDefenderCharacterId||input.defenseRoll.successes!==ForbiddenLandsDiceEngine.countSuccesses(input.defenseRoll.results)||(input.defenseRoll.assistedBy||[]).length>0)throw new Error('A rolagem de defesa não pertence ao defensor/encontro atual ou tem sucessos inconsistentes.');
      }
      for (const combatant of [attacker, ...(helper ? [helper] : []), ...(input.defenseRoll ? [target] : [])]) {
        if (monsterIncapacitated(combatant)) throw new Error(`${combatant.name} está incapacitado e não pode agir.`);
        if (combatant.kind !== 'character' || !combatant.entityId) continue;
        const character = await tx.objectStore('characters').get(combatant.entityId);
        EncounterCampaignGuard.assertCharacter(character,campaignId,'combatente',
          {partyRoster,partySide:combatant.side==='party'});
        CharacterActionRulesEngine.assertCanAct(character);
      }
      let attackerCharacter: Character | undefined;
      if (input.crossbowWeaponIndex !== undefined || input.poisonWeaponIndex !== undefined) {
        if (attacker.kind !== 'character' || !attacker.entityId) throw new Error('O efeito de arma exige ficha de personagem vinculada.');
        attackerCharacter = await tx.objectStore('characters').get(attacker.entityId);
        if (!attackerCharacter) throw new Error('Ficha do atacante não encontrada.');
        if (input.crossbowWeaponIndex !== undefined) {
          const crossbow = attackerCharacter.weapons?.[input.crossbowWeaponIndex];
          if (!crossbow || !/besta|crossbow/i.test(String(crossbow.name || '')) || crossbow.loaded !== true) throw new Error('A besta precisa estar carregada antes do disparo.');
        }
      }

      // The Bitter Reach p.26: elementals resist nonmagical attacks, except
      // water against fire and fire against water. Magical weapons are not blocked.
      const weaponLabel=normalizeRuleKey(chosenWeaponName);
      const incomingKind=/balde|bucket|agua|water/.test(weaponLabel)?'water':input.fireAttack||/tocha|torch|fogo|fire/.test(weaponLabel)?'fire':'physical';
      const incomingMagical=Boolean(input.magicalAttack)||/magico|magic|artefato|artifact|encantad|enchanted/.test(weaponLabel);
      const immuneElemental=BitterReachElementalEngine.immuneTo(target,{magical:incomingMagical,kind:incomingKind});
      const immuneMara=target.quickSheet?.magicWeaponsOnly===true&&!incomingMagical;
      const halfMaraFire=target.quickSheet?.fireDamageHalved===true&&incomingKind==='fire';
      if(target.summonedElemental&&!incomingMagical&&incomingKind!=='physical'&&input.attackRoll.results.some(die=>die.type==='gear'))
        throw new Error('Ataque com balde de água/tocha contra elemental não usa Dados de Equipamento.');
      const targetingMount = Boolean(input.targetMount && target.mounted);
      // Mouth aim: validated at commit, before dice/damage writes. The -2 was applied
      // to the physical roll template before revealing any die faces.
      const resolution = CombatRulesEngine.resolveAttack({
        attackSuccesses: input.attackRoll.successes,
        defenseSuccesses: input.defenseRoll?.successes || 0,
        weaponDamage: target.summonedElemental&&!incomingMagical&&incomingKind!=='physical'?2:input.weaponDamage + windUpBonus + chargeBonus + berserkerMeleeBonus + (hypnoticControl ? Math.max(1,Number(hypnoticControl.weaponDamageModifier)||1) : 0),
        // Bloodmarch p.111: from INSIDE the mouth the wyrm has no armor.
        armorValue: targetingMount ? 0 : BloodmarchBetaFireWyrmWeakSpotEngine.armorValue(target.armor,insideMouth,aimedFireWyrmMouth),
        naturalArmor: target.quickSheet?.officialProcessionEnt===true||target.quickSheet?.officialEncounterTroll===true||target.quickSheet?.officialR88Minotaur===true||target.quickSheet?.officialR88SaurianHunter===true||target.quickSheet?.officialGeneratedDemon===true,
        rng: input.rng,
      });
      const entDamage=OfficialEntDamageRulesEngine.resolve({damage:resolution.finalDamage,incomingKind,magical:incomingMagical,targetQuickSheet:target.quickSheet});
      const ghostDamage=OfficialGhostDamageRulesEngine.resolve({damage:resolution.finalDamage,incomingKind,magical:incomingMagical,targetQuickSheet:target.quickSheet});

      // Sangrento, Guia do Mestre pp.110–111: dano após ARMADURA, não antes.
      // Material vem da arma realmente selecionada na ficha; sem seleção não há cobre presumido.
      if(BloodMistDemonRulesEngine.isDemon(target.entityId)&&incomingKind==='physical'){
        const mistForm=target.statusEffects?.some(effect=>effect.id==='bloodmist:incorporeal');
        const after=BloodMistDemonRulesEngine.physicalDamageAfterArmor(
          resolution.finalDamage,mistForm?'mist':'manifested',BloodMistDemonRulesEngine.weaponIsCopper(chosenWeapon));
        resolution.finalDamage=after;
        if(mistForm){resolution.rawDamage=0;resolution.armorLoss=0;resolution.armorAbsorbed=0;}
      }

      // Livro do Mestre p.106 — OSSUDO: uma estocada ou flecha jamais inflige
      // mais de 1 ponto de dano por golpe em um esqueleto. Após armadura,
      // sobre a resolução efetiva que será gravada no combate e na ficha.
      if((target.quickSheet?.officialSkeleton===true||target.quickSheet?.officialR88Skeleton===true)&&target.entityId?.startsWith('official-skeleton:')&&
         (input.attackKind==='stab'||input.criticalTable==='puncture'||(input.attackKind==='ranged'&&/(arco|bow|besta|crossbow|flecha|arrow)/i.test(chosenWeaponName)))){
        resolution.finalDamage=Math.min(1,resolution.finalDamage);
      }

      if(immuneElemental||immuneMara||entDamage.immune||ghostDamage.immune||
        (target.quickSheet?.officialGeneratedDemon===true&&((target.quickSheet?.immuneToWeapons===true&&input.attackKind!=='unarmed'&&!incomingMagical)||(target.quickSheet?.fireImmune===true&&incomingKind==='fire')))){
        resolution.rawDamage=0;resolution.finalDamage=0;resolution.armorLoss=0;resolution.armorAbsorbed=0;
      }else if(halfMaraFire){
        resolution.finalDamage=Math.floor(resolution.finalDamage/2);
      }else if(entDamage.multiplier>1){
        // R88 Ent p.89: fire attacks cause double damage after armor.
        resolution.finalDamage=entDamage.damage;
      }

      if(!targetingMount&&incomingKind==='fire'&&(
        BloodmarchBetaFireGuard.immune(target.statusEffects)||
        BloodmarchBetaFireGuard.immune(targetRuleCharacter?.statusEffects))){
        // Fire's Friend protects the body, not the armor. Keep armor dice/loss.
        resolution.finalDamage=0;
      }

      const officialEncounterDamage=OfficialEncounterCombatRulesEngine.incomingDamage({
        targetQuickSheet:target.quickSheet,damage:resolution.finalDamage,attackKind:input.attackKind,
        weaponName:chosenWeaponName,damageType:String(chosenWeapon?.damageType||''),magical:incomingMagical,
      });
      if(officialEncounterDamage.immune){
        resolution.rawDamage=0;resolution.finalDamage=0;resolution.armorLoss=0;resolution.armorAbsorbed=0;
      }else if(officialEncounterDamage.capped){
        resolution.finalDamage=officialEncounterDamage.damage;
      }

      let entEmbraceReleased=false;
      let mountDying = false;
      let newMountStrength = target.mounted ? target.mounted.mountStrength : 0;
      let strength = target.currentStrength;
      let critical = false;
      let berserkActivated = false;
      let berserkRecovered = 0;

      if (targetingMount && target.mounted) {
        newMountStrength = Math.max(0, target.mounted.mountStrength - resolution.finalDamage);
        mountDying = newMountStrength <= 0;
      } else {
        const rawStrength = Math.max(0, Math.min(target.maxStrength, target.currentStrength - resolution.finalDamage));
        strength = rawStrength;
        const _compat = () => { const strength = Math.max(0, Math.min(target.maxStrength, target.currentStrength - resolution.finalDamage)); return strength; };
        critical = CombatRulesEngine.criticalRequired({ previousStrength: target.currentStrength, finalDamage: resolution.finalDamage });

        // Intervenção Berserker
        const berserkerRank = combatantTalentRank(target, targetRuleCharacter, 'Berserker');
        const canBerserk = berserkerRank >= 1 && !target.rageState?.exhausted && !target.rageState?.active;
        if (strength <= 0 && canBerserk) {
          berserkRecovered = CombatRulesEngine.berserkerRecoveryPoints(berserkerRank);
          strength = berserkRecovered;
          berserkActivated = true;
          critical = false;
        }
      }

      const consumedDeath = target.kind === 'character' && strength <= 0 && target.statusEffects?.some(effect => effect.id.startsWith('monster-consumed:')) === true;
      const status: EncounterParticipantStatus = consumedDeath ? 'dead' : strength <= 0 ? 'broken' : target.status;
      const now = Date.now();
      let combatants = encounter.combatants.map(item => {
        if (item.id !== target.id) return item;
        let updatedItem: EncounterCombatant = {
          ...item,
          currentStrength: strength,
          armor: targetingMount ? item.armor : Math.max(0, item.armor - resolution.armorLoss),
          status: berserkActivated ? 'active' : status,
          brokenAttribute: (!berserkActivated && strength <= 0) ? 'strength' as const : item.brokenAttribute === 'strength' ? undefined : item.brokenAttribute,
          ...(consumedDeath ? { quickSheet: { ...(item.quickSheet || {}), status: 'dead' } } : {}),
          statusEffects: consumedDeath ? (item.statusEffects || []).filter(effect => !effect.id.startsWith('monster-consumed:')) : item.statusEffects,
        };
        if (targetingMount && item.mounted) {
          updatedItem = {
            ...updatedItem,
            mounted: {
              ...item.mounted,
              mountStrength: newMountStrength,
              status: mountDying ? 'dying' : item.mounted.status,
            },
            ...(mountDying ? {
              statusEffects: [
                ...(updatedItem.statusEffects || []).filter(effect => effect.id !== 'condition:prone'),
                { id: 'condition:prone', name: 'Caído', detail: 'Montaria caiu moribunda; cavaleiro foi derrubado Caído.' }
              ]
            } : {})
          };
        }
        if (berserkActivated) {
          const berserkTalentRank = combatantTalentRank(target, targetRuleCharacter, 'Berserker');
          updatedItem = {
            ...updatedItem,
            status: 'active',
            brokenAttribute: undefined,
            rageState: { active: true, rank: berserkTalentRank, startedRound: encounter.round },
          };
        }
        return updatedItem;
      });

      if (input.crossbowWeaponIndex !== undefined && attackerCharacter) {
        const weapons = attackerCharacter.weapons.map((weapon, index) => index === input.crossbowWeaponIndex ? { ...weapon, loaded: false } : weapon);
        await tx.objectStore('characters').put({ ...attackerCharacter, weapons, updatedAt: now });
        combatants = combatants.map(item => item.id === attacker.id ? { ...item, quickSheet: { ...(item.quickSheet || {}), weapons } } : item);
      }

      if (target.kind === 'character' && target.entityId && !targetingMount) {
        const characterStore = tx.objectStore('characters');
        const character = await characterStore.get(target.entityId);
        if (character) {
          const clearingStrengthBreak = strength > 0 && (character.brokenAttribute === 'strength' || (!character.brokenAttribute && character.conditions?.includes('Broken')));
          const conditions = (strength <= 0 && !berserkActivated)
            ? [...new Set([...(character.conditions || []), 'Broken' as const])]
            : clearingStrengthBreak || berserkActivated
              ? (character.conditions || []).filter(condition => condition !== 'Broken')
              : character.conditions;
          const marker = `PENDENTE: ferimento grave — tabela de ${input.criticalTable === 'other' ? 'outros' : input.criticalTable === 'slash' ? 'laceração' : input.criticalTable === 'puncture' ? 'perfuração' : input.criticalTable === 'blunt' ? 'trauma concussivo' : 'horror'}`;
          await characterStore.put({
            ...character,
            ...(consumedDeath ? { status: 'dead' as const } : {}),
            conditions,
            brokenAttribute: (!berserkActivated && strength <= 0) ? 'strength' : clearingStrengthBreak || berserkActivated ? undefined : character.brokenAttribute,
            attributes: { ...character.attributes, strength: { ...character.attributes.strength, current: strength } },
            ...(consumedDeath ? { statusEffects: (character.statusEffects || []).filter(effect => !effect.id.startsWith('monster-consumed:')) } : {}),
            ...(critical ? { criticalInjuries: (character.criticalInjuries || []).includes(marker) ? character.criticalInjuries : [...(character.criticalInjuries || []), marker] } : {}),
            ...(input.crossbowWeaponIndex !== undefined && attackerCharacter?.id === character.id ? { weapons: attackerCharacter.weapons.map((weapon, index) => index === input.crossbowWeaponIndex ? { ...weapon, loaded: false } : weapon), updatedAt: now } : {}),
          });
        }
      } else if (target.kind === 'monster') {
        const clearEffects = (prefix: string, shouldClear: (effect: NonNullable<EncounterCombatant['statusEffects']>[number]) => boolean) => {
          const victims = combatants.filter(item => item.statusEffects?.some(effect => effect.id === prefix && shouldClear(effect)));
          combatants = combatants.map(item => victims.some(victim => victim.id === item.id) ? { ...item, statusEffects: (item.statusEffects || []).filter(effect => effect.id !== prefix || !shouldClear(effect)) } : item);
          return victims;
        };
        const victims = [
          ...clearEffects(`monster-life-drain:${target.id}`, () => resolution.finalDamage >= 3),
          ...clearEffects(`monster-consumed:${target.id}`, effect => strength <= 0 || resolution.finalDamage >= Math.max(1, Number(effect.ongoingDamage) || 3)),
          ...clearEffects(`monster-magic-leech:${target.id}`, () => strength <= 0 || resolution.finalDamage >= 2),
          ...clearEffects(`monster-grappled:fire-wyrm-mouth:${target.id}`, () => strength <= 0),
        ];
        // Bloodmarch: a vítima presa pode cortar a saída causando 4+ dano
        // em UM ataque. Não liberte todas as outras vítimas por um único golpe.
        const mouthId=`monster-grappled:fire-wyrm-mouth:${target.id}`;
        if(resolution.finalDamage>=4&&combatants.some(item=>item.id===attacker.id&&
            item.statusEffects?.some(effect=>effect.id===mouthId))){
          combatants=combatants.map(item=>item.id===attacker.id?{...item,
            statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==mouthId)}:item);
          if(attacker.kind==='character'&&attacker.entityId){
            const trapped=await tx.objectStore('characters').get(attacker.entityId);
            if(trapped)await tx.objectStore('characters').put({...trapped,
              statusEffects:(trapped.statusEffects||[]).filter(effect=>effect.id!==mouthId)});
          }
        }
        const entEmbraceId=`official-ent-embrace:${target.entityId||''}`;
        if(target.quickSheet?.officialProcessionEnt===true&&resolution.finalDamage>=4&&
          combatants.some(item=>item.id===attacker.id&&item.statusEffects?.some(effect=>effect.id===entEmbraceId))){
          entEmbraceReleased=true;
          combatants=combatants.map(item=>item.id===attacker.id?{...item,
            statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==entEmbraceId)}:item);
          if(attacker.kind==='character'&&attacker.entityId){
            const trapped=await tx.objectStore('characters').get(attacker.entityId);
            if(trapped)await tx.objectStore('characters').put({...trapped,
              statusEffects:(trapped.statusEffects||[]).filter(effect=>effect.id!==entEmbraceId)});
          }
        }
        for (const victim of victims) if (victim.kind === 'character' && victim.entityId) {
          const character = await tx.objectStore('characters').get(victim.entityId);
          if (character) await tx.objectStore('characters').put({ ...character, statusEffects: (character.statusEffects || []).filter(effect => !effect.id.endsWith(`:${target.id}`)) });
        }
      }

      if (critical) {
        const tableLabel = input.criticalTable === 'other' ? 'outros' : input.criticalTable === 'slash' ? 'laceração' : input.criticalTable === 'puncture' ? 'perfuração' : input.criticalTable === 'blunt' ? 'trauma concussivo' : 'horror';
        combatants = combatants.map(item => item.id === target.id ? { ...item, statusEffects: [...(item.statusEffects || []).filter(effect => !effect.id.startsWith('critical-pending:')), { id: `critical-pending:${crypto.randomUUID()}`, name: 'Ferimento grave pendente', detail: input.criticalTable === 'other' ? 'Resolver dano atípico conforme a regra de ferimentos graves.' : `Resolver D66 na tabela de ${tableLabel}.`, criticalTable: input.criticalTable, source: attacker.id }] } : item);
      }
      if (input.fireAttack && resolution.finalDamage > 0) {
        const burning = FireRulesEngine.ignite(`Ataque de fogo de ${attacker.name}`, resolution.finalDamage);
        combatants = combatants.map(item => item.id === target.id ? { ...item, statusEffects: [...(item.statusEffects || []).filter(effect => effect.id !== 'burning'), { id: burning.id, name: 'Em chamas', source: burning.source, ongoingDamage: burning.damagePerRound, ongoingRounds: 0, detail: `${burning.source}: ${burning.damagePerRound} dano por rodada até apagar com Movimentação.` }] } : item);
      }
      // The Bitter Reach p.20 — Arpoador 2+: bleeding starts only after an actual damaging hit.
      // Persist the effect on both the encounter and the canonical character sheet.
      if (harpoonTalent.bleedOnDamage && resolution.finalDamage > 0 && !targetingMount) {
        const bleedingId = `bitter-harpoon-bleed:${attacker.id}`;
        const deathRound = strength === 0 ? encounter.round + (1 + Math.floor((input.rng || Math.random)() * 6)) * 6 : undefined;
        const bleedingEffect = { id: bleedingId, name: 'Sangramento por arpão', detail: '1 dano de Força no início de cada rodada; Cura bem-sucedida interrompe.', ongoingDamage: 1, ongoingRounds: 0, source: attacker.name, ...(deathRound ? { expiresRound: deathRound } : {}) };
        combatants = combatants.map(item => item.id === target.id ? {
          ...item, statusEffects: [...(item.statusEffects || []).filter(effect => !effect.id.startsWith('bitter-harpoon-bleed:')), bleedingEffect],
        } : item);
        if (target.kind === 'character' && target.entityId) {
          const characterStore = tx.objectStore('characters');
          const victim = await characterStore.get(target.entityId);
          if (victim) await characterStore.put({ ...victim, statusEffects: [
            ...(victim.statusEffects || []).filter(effect => !effect.id.startsWith('bitter-harpoon-bleed:')),
            { id: bleedingId, label: 'Sangramento por arpão', detail: bleedingEffect.detail, ongoingDamage: 1, ongoingRounds: 0, ...(deathRound ? { expiresRound: deathRound } : {}) },
          ] });
        }
      }
      let nextJourney = current.journey;
      let poisonRoll: DiceRoll | undefined;
      if (input.poisonWeaponIndex !== undefined && resolution.finalDamage > 0 && attacker.entityId && attackerCharacter) {
        const characterStore = tx.objectStore('characters');
        const latestAttacker = await characterStore.get(attackerCharacter.id);
        if (!latestAttacker) throw new Error('Atacante da arma envenenada não encontrado.');
        const consumed = PoisonRulesEngine.consumeWeaponCoating(latestAttacker, input.poisonWeaponIndex);
        if (!consumed.coating) throw new Error('A arma não tem mais veneno aplicado.');
        attackerCharacter = consumed.character;
        await characterStore.put(consumed.character);
        combatants = combatants.map(item => item.entityId === attackerCharacter?.id ? { ...item, quickSheet: { ...(item.quickSheet || {}), weapons: consumed.character.weapons } } : item);
        const poisonTarget = await characterStore.get(target.entityId || target.id);
        if (poisonTarget && poisonTarget.status !== 'dead' && !poisonTarget.poisonState?.active) {
          const campaign = await tx.objectStore('campaign').get(campaignId);
          if (!campaign) throw new Error('Campanha não encontrada.');
          const potency = Math.max(1, Math.min(30, Math.trunc(consumed.coating.potency)));
          const sceneId = `hazard:weapon-poison:${campaignId}:${now}`;
          const victimRoll = ForbiddenLandsDiceEngine.roll({ base: poisonTarget.attributes.strength.current, skill: normalizedSkillRank(poisonTarget, ['Resilience', 'Resiliência']), gear: 0 }, { characterId: poisonTarget.id, sceneId, checkId: `weapon-poison-resistance:${now}`, rng: input.rng, criticalInjuries: poisonTarget.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
          poisonRoll = ForbiddenLandsDiceEngine.roll({ base: potency, skill: 0, gear: 0 }, { characterId: null, sceneId, checkId: `weapon-poison-potency:${now}`, rng: input.rng });
          const effect = PoisonRulesEngine.expose(poisonTarget, consumed.coating.kind, potency, victimRoll.successes, poisonRoll.successes);
          await characterStore.put(effect.character);
          await tx.objectStore('rolls').put(victimRoll);
          await tx.objectStore('rolls').put(poisonRoll);
          combatants = combatants.map(item => item.entityId === poisonTarget.id ? { ...item, currentStrength: effect.character.attributes.strength.current, status: effect.character.brokenAttribute ? 'broken' as const : item.status, brokenAttribute: effect.character.brokenAttribute, conditions: [...effect.character.conditions], quickSheet: { ...(item.quickSheet || {}), attributes: effect.character.attributes, conditions: effect.character.conditions, brokenAttribute: effect.character.brokenAttribute } } : item);
          const event: JourneyEvent = {
            id: crypto.randomUUID(), type: 'mishap', title: `Veneno da arma ${consumed.coating.kind}`,
            detail: effect.totalEffect ? `${poisonTarget.name} recebeu efeito total do veneno após ataque; 1 dano em ${effect.attribute} por rodada.` : `${poisonTarget.name} resistiu ao efeito total (${victimRoll.successes} × ${poisonRoll.successes}) e sofreu apenas 1 dano em ${effect.attribute}.`,
            result: { hazard: 'poison_weapon_hit', attackerId: attackerCharacter.id, targetId: poisonTarget.id, weaponIndex: input.poisonWeaponIndex, kind: consumed.coating.kind, potency, totalEffect: effect.totalEffect, attribute: effect.attribute, damageApplied: effect.damageApplied, victimRollId: victimRoll.id, poisonRollId: poisonRoll.id },
            ruleSource: 'Livro do Jogador, pp. 113–114 — Envenenamento e Veneno em Armas', day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: now,
          };
          nextJourney = { ...nextJourney, events: retainJourneyEvents([...nextJourney.events, event]) };
        }
      }
      const criticalRollLogs: EncounterState['log'] = [];
      const characterStoreForRollConsequences = tx.objectStore('characters');
      if (attacker.kind === 'character' && attacker.entityId && input.attackRoll.skillName) {
        const latestAttacker = await characterStoreForRollConsequences.get(attacker.entityId);
        if (latestAttacker) {
          const consequence = applyCriticalRollConsequences(latestAttacker, input.attackRoll);
          if (consequence.triggers.length) {
            const updatedAttacker = { ...consequence.character, updatedAt: now };
            await characterStoreForRollConsequences.put(updatedAttacker);
            combatants = combatants.map(item => item.id === attacker.id ? syncCombatantFromCharacter(item, updatedAttacker) : item);
            criticalRollLogs.push({ id: crypto.randomUUID(), round: encounter.round, combatantId: attacker.id, text: `${attacker.name}${criticalRollConsequenceText(consequence.triggers)} ao realizar ${input.attackRoll.skillName}.`, createdAt: now });
          }
        }
      }
      if (input.defenseRoll && target.kind === 'character' && target.entityId && input.defenseRoll.skillName) {
        const latestDefender = await characterStoreForRollConsequences.get(target.entityId);
        if (latestDefender) {
          const consequence = applyCriticalRollConsequences(latestDefender, input.defenseRoll);
          if (consequence.triggers.length) {
            const updatedDefender = { ...consequence.character, updatedAt: now };
            await characterStoreForRollConsequences.put(updatedDefender);
            combatants = combatants.map(item => item.id === target.id ? syncCombatantFromCharacter(item, updatedDefender) : item);
            criticalRollLogs.push({ id: crypto.randomUUID(), round: encounter.round, combatantId: target.id, text: `${target.name}${criticalRollConsequenceText(consequence.triggers)} ao realizar ${input.defenseRoll!.skillName}.`, createdAt: now });
          }
        }
      }

      combatants = combatants.map(item => {
        let next = item;
        if (item.id === attacker.id) {
          next = actionCost === 'slow' ? { ...next, slowActionAvailable: false } : spendFastAction(next);
          if (input.aimed) next = spendFastAction(next);
          next = { ...next, statusEffects:(next.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-ranged-prepared:')) };
          if(input.runWithShot)next={...next,statusEffects:[...(next.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-fast-shooter-run:')),{id:`core-fast-shooter-run:${encounter.round}`,name:'Correr junto do disparo',detail:'Atirador Veloz 2+: pode realizar um Correr sem gastar ação antes de encerrar este turno.'}]};
          if (windUpEffect) next = { ...next, statusEffects:(next.statusEffects||[]).filter(effect=>effect.id!==windUpEffect.id) };
        }
        if (helper && item.id === helper.id) next = { ...next, slowActionAvailable: false };
        if (input.defenseRoll && item.id === target.id) next = applyReactiveDefenseCost(next, targetRuleCharacter, encounter.round, input.defenseKind || 'dodge').combatant;
        return next;
      });
      await tx.objectStore('rolls').put(input.attackRoll);
      if (input.defenseRoll) await tx.objectStore('rolls').put(input.defenseRoll);
      const log = [
        { id: crypto.randomUUID(), round: encounter.round, combatantId: attacker.id, text: `${attacker.name} atacou${helper ? ` com ajuda de ${helper.name}` : ''}${input.aimed ? ' após Mirar' : ''}${input.runWithShot ? ' combinando Atirar + Correr (Atirador Veloz)' : ''}${windUpBonus ? ' após Arremeter Arma (+1 dano)' : ''} e consumiu a ação ${actionCost === 'fast' ? 'rápida' : 'lenta'}${helper ? ' (ajudante também)' : ''}${input.aimed ? ' e a rápida' : ''}.`, createdAt: now },
        ...(input.defenseRoll ? [{ id: crypto.randomUUID(), round: encounter.round, combatantId: target.id, text: `${target.name} gastou ação rápida em ${input.defenseReason || 'defesa reativa'}.`, createdAt: now }] : []),
        { id: crypto.randomUUID(), round: encounter.round, combatantId: target.id, text: `${target.name}: ${resolution.finalDamage} dano após armadura; Força ${target.currentStrength} → ${strength}; armadura ${target.armor} → ${Math.max(0, target.armor - resolution.armorLoss)}${critical ? `; ferimento grave pendente (${input.criticalTable})` : ''}${entEmbraceReleased?'; o golpe libertou a vítima presa no corpo do Ent':''}${input.fireAttack && resolution.finalDamage > 0 ? '; em chamas' : ''}${poisonRoll ? '; veneno resolvido' : ''}.`, createdAt: now },
      ];
      const updatedEncounter = this.resolveIfFinished({ ...encounter, combatants, log: [...encounter.log, ...log, ...criticalRollLogs].slice(-300), updatedAt: now }, now);
      let world:WorldState = { ...current, encounter: updatedEncounter, journey: nextJourney, updatedAt: now };
      // An attack spends the singer's action; an actual wound breaks the victim's chant.
      if(attacker.entityId)world=await BloodmarchBetaChantService.interrupt(tx,world,attacker.entityId,'ataque realizado');
      if(resolution.finalDamage>0&&target.entityId)world=await BloodmarchBetaChantService.interrupt(tx,world,target.entityId,'dano de ataque');
      if(helper?.entityId)world=await BloodmarchBetaChantService.interrupt(tx,world,helper.entityId,'ajuda no ataque');
      await store.put(world);
      return { world, resolution, critical, poisonRoll };
    });
    this.notify(committed.world);
    return committed;
  }

  static async applyBurning(campaignId:string,combatantId:string,source:string,damagePerRound=1):Promise<WorldState>{
    const burning=FireRulesEngine.ignite(source,damagePerRound);
    return this.update(campaignId,(encounter,now)=>{
      const target=encounter.combatants.find(item=>item.id===combatantId);
      if(!target||target.status==='dead'||target.status==='fled'||target.status==='yielded')throw new Error('O alvo do fogo não pode mais ser afetado.');
      const statusEffects=[...(target.statusEffects||[]).filter(effect=>effect.id!=='burning'),{id:burning.id,name:'Em chamas',source:burning.source,ongoingDamage:burning.damagePerRound,ongoingRounds:0,detail:`${burning.source}: ${burning.damagePerRound} dano por rodada até apagar com Movimentação.`}];
      return {...encounter,combatants:encounter.combatants.map(item=>item.id===combatantId?{...item,statusEffects}:item),log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId,text:`${target.name} está em chamas (${burning.source}); recebe ${burning.damagePerRound} dano por rodada até apagar.`,createdAt:now}].slice(-300),updatedAt:now};
    });
  }

  static async extinguishBurning(campaignId:string,actorId:string,targetId:string,roll:DiceRoll):Promise<WorldState>{
    const world=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const actor=encounter.combatants[encounter.turnIndex];
      if(!actor||actor.id!==actorId||actor.kind!=='character'||!actor.entityId||actor.status!=='active'||!canSpendFastAction(actor))throw new Error('Apagar o fogo exige uma ação disponível que possa ser usada como rápida pela personagem ativa.');
      if(monsterIncapacitated(actor))throw new Error(`${actor.name} está incapacitado e não pode apagar o fogo.`);
      const sceneId=encounter.startedSceneId||encounter.id;
      if(!roll.id||roll.characterId!==actor.entityId||roll.sceneId!==sceneId||roll.checkId!==`extinguish:${encounter.id}:${encounter.round}:${actor.id}:${targetId}`||roll.successes!==ForbiddenLandsDiceEngine.countSuccesses(roll.results))throw new Error('O teste de Movimentação não pertence a quem está apagando as chamas neste encontro ou tem sucessos inconsistentes.');
      const character=await tx.objectStore('characters').get(actor.entityId);
      if(!character)throw new Error('Ficha da personagem ativa não encontrada.');
      CharacterActionRulesEngine.assertCanAct(character);
      const target=encounter.combatants.find(item=>item.id===targetId);
      if(!target||!(target.statusEffects||[]).some(effect=>effect.id==='burning'))throw new Error('O alvo não está em chamas.');
      const outcome=FireRulesEngine.extinguish({id:'burning',source:'fire',damagePerRound:1,rounds:0},roll.successes);
      await tx.objectStore('rolls').put(roll);
      if(outcome.extinguished&&target.kind==='character'&&target.entityId){
        const burningCharacter=await tx.objectStore('characters').get(target.entityId) as Character|undefined;
        if(burningCharacter)await tx.objectStore('characters').put({...burningCharacter,
          statusEffects:(burningCharacter.statusEffects||[]).filter(effect=>effect.id!=='burning')});
      }
      const combatants=encounter.combatants.map(item=>item.id===actorId?spendFastAction(item):item.id===targetId?{...item,statusEffects:outcome.extinguished?(item.statusEffects||[]).filter(effect=>effect.id!=='burning'):(item.statusEffects||[]).map(effect=>effect.id==='burning'?{...effect,detail:`${effect.source||'Fogo'} continua: Movimentação sem sucesso; ${effect.ongoingDamage||1} dano por rodada.`}:effect)}:item);
      const now=Date.now();
      const updated={...current,encounter:{...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:targetId,text:`${character.name} tentou apagar o fogo em ${target.name} com ${roll.successes} sucesso(s): ${outcome.extinguished?'apagado':'continua queimando'}.`,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};
      await store.put(updated);
      return updated;
    });
    this.notify(world);
    return world;
  }

  static async resolveFearAttack(campaignId: string, attackerId: string, targetId: string, baseDice: number, rng: () => number = Math.random, damageAttribute: FearDamageAttribute = 'wits'): Promise<{ world: WorldState; roll: DiceRoll; defenseRoll: DiceRoll | null; damage: number }> {
    let roll: DiceRoll | null = null;
    let defenseRoll: DiceRoll | null = null;
    let damage = 0;
    const world = await withTransaction(['world_state', 'characters', 'rolls'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const attacker = encounter.combatants[encounter.turnIndex];
      const target = encounter.combatants.find(item => item.id === targetId);
      if (!attacker || attacker.id !== attackerId || attacker.status !== 'active') throw new Error('Apenas o combatente ativo pode realizar o ataque de medo.');
      if (monsterIncapacitated(attacker)) throw new Error(`${attacker.name} está incapacitado e perde as ações até o próximo turno.`);
      if (!attacker.slowActionAvailable) throw new Error(`${attacker.name} já gastou a ação lenta desta rodada.`);
      if (attacker.statusEffects?.some(effect => effect.id === 'condition:prone')) throw new Error(`${attacker.name} está Caído e precisa gastar uma ação rápida para se levantar antes de atacar.`);
      if (!target || target.id === attacker.id || target.side === attacker.side || target.kind !== 'character' || !target.entityId || target.status !== 'active') throw new Error('Escolha uma personagem ativa da equipe adversária.');
      const characterStore = tx.objectStore('characters');
      const character = await characterStore.get(target.entityId);
      if (!character) throw new Error('Ficha da personagem alvo não encontrada.');
      const count = Math.max(1, Math.min(30, Math.trunc(Number(baseDice) || 1)));
      const blindPenalty=attacker.statusEffects?.some(effect=>effect.id.startsWith('monster-blinded:'))?-2:0;
      roll = ForbiddenLandsDiceEngine.roll({ base: count, skill: 0, gear: 0, modifier: blindPenalty }, { characterId: null, sceneId: encounter.startedSceneId || encounter.id, checkId: `fear:${encounter.id}:${encounter.round}:${attacker.id}:${target.id}`, rng });
      const fearlessRank = FearRulesEngine.fearlessRank(character);
      if (fearlessRank >= 1 && fearlessRank < 3) {
        const normalizeSkill = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase().trim();
        const insight = Object.entries(character.skills || {}).find(([name]) => ['insight', 'discernimento'].includes(normalizeSkill(name)))?.[1] || 0;
        defenseRoll = ForbiddenLandsDiceEngine.roll(
          { base: character.attributes.wits.current, skill: Number(insight) || 0, gear: 0 },
          { characterId: character.id, sceneId: encounter.startedSceneId || encounter.id, checkId: `fear-defense:${encounter.id}:${encounter.round}:${target.id}`, criticalInjuries: character.criticalInjuryStates, skillName: 'insight', attributeName: 'wits', rng },
        );
        await tx.objectStore('rolls').put(defenseRoll);
      }
      const outcome = FearRulesEngine.resolve(character, roll.successes, { defenseSuccesses: defenseRoll?.successes || 0, damageAttribute });
      damage = outcome.damage;
      await characterStore.put(outcome.character);
      await tx.objectStore('rolls').put(roll);
      const marker = 'PENDENTE: ferimento grave — tabela de horror';
      if (outcome.horrorPending && !(outcome.character.criticalInjuries || []).includes(marker)) {
        await characterStore.put({ ...outcome.character, criticalInjuries: [...(outcome.character.criticalInjuries || []), marker] });
      }
      const combatants = encounter.combatants.map(item => {
        if (item.id === attacker.id) return { ...item, slowActionAvailable: false };
        if (item.id !== target.id) return item;
        const pending = outcome.horrorPending ? [{ id: `critical-pending:${crypto.randomUUID()}`, name: 'Ferimento grave pendente', detail: 'O ataque de medo Quebrou Astúcia; role na tabela D66 de horror.', criticalTable: 'horror' as const }] : [];
        return {
          ...item,
          status: outcome.character.brokenAttribute ? 'broken' as const : item.status,
          brokenAttribute: outcome.character.brokenAttribute,
          conditions: [...outcome.character.conditions],
          quickSheet: { ...(item.quickSheet || {}), attributes: outcome.character.attributes, conditions: outcome.character.conditions, brokenAttribute: outcome.character.brokenAttribute },
          ...(outcome.horrorPending ? { statusEffects: [...(item.statusEffects || []).filter(effect => !effect.id.startsWith('critical-pending:')), ...pending] } : {}),
        };
      });
      const now = Date.now();
      const protection = outcome.immune ? 'Destemido 3 tornou o alvo imune' : defenseRoll ? `Destemido ${fearlessRank}: Discernimento ${defenseRoll.successes} sucesso(s) cancelou${defenseRoll.successes === 1 ? '' : 'ram'} sucessos` : '';
      const updated = { ...encounter, combatants, log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId: target.id, text: `${attacker.name} atacou com medo (${roll.successes} sucesso(s))${protection ? `; ${protection}` : ''}: ${target.name} sofreu ${damage} dano em ${outcome.attribute === 'wits' ? 'Astúcia' : 'Empatia'}${outcome.horrorPending ? ' e precisa rolar ferimento grave de horror' : ''}.`, createdAt: now }].slice(-300), updatedAt: now };
      const next = { ...current, encounter: updated, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
    if (!roll) throw new Error('Não foi possível confirmar o ataque de medo.');
    return { world, roll, defenseRoll, damage };
  }

  /** The Bitter Reach p.20 — successful HEALING stops Harpooner bleeding (not an automatic cure). */
  static async treatHarpoonBleeding(campaignId: string, healerCombatantId: string, targetId: string, roll: DiceRoll): Promise<WorldState> {
    const world = await withTransaction(['world_state','characters','rolls'],'readwrite',async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const healer = encounter.combatants[encounter.turnIndex];
      if (!healer || healer.id !== healerCombatantId || healer.kind !== 'character' || !healer.entityId || healer.status !== 'active') throw new Error('Somente a personagem ativa pode tratar o sangramento.');
      if (!healer.slowActionAvailable) throw new Error('Ação lenta indisponível para Cura.');
      const target = encounter.combatants.find(item => item.id === targetId);
      if (!target || !target.statusEffects?.some(effect => effect.id.startsWith('bitter-harpoon-bleed:'))) throw new Error('O alvo não apresenta sangramento por arpão.');
      const healerSheet = await tx.objectStore('characters').get(healer.entityId);
      if (!healerSheet) throw new Error('Ficha do curandeiro não encontrada.');
      CharacterActionRulesEngine.assertCanAct(healerSheet);
      const sceneId = encounter.startedSceneId || encounter.id;
      const checkId = `bitter-harpoon-heal:${encounter.id}:${encounter.round}:${healer.id}:${target.id}`;
      if (!roll.id || roll.characterId !== healerSheet.id || roll.sceneId !== sceneId || roll.checkId !== checkId || roll.successes !== ForbiddenLandsDiceEngine.countSuccesses(roll.results)) {
        throw new Error('Rolagem de Cura não corresponde ao curandeiro, encontro ou alvo.');
      }
      const cured = roll.successes > 0;
      const combatants = encounter.combatants.map(item => item.id === healer.id ? { ...item, slowActionAvailable: false } : item.id === target.id && cured
        ? { ...item, statusEffects: (item.statusEffects || []).filter(effect => !effect.id.startsWith('bitter-harpoon-bleed:')) } : item);
      if (target.kind === 'character' && target.entityId && cured) {
        const victim = await tx.objectStore('characters').get(target.entityId);
        if (victim) await tx.objectStore('characters').put({ ...victim, statusEffects: (victim.statusEffects || []).filter(effect => !effect.id.startsWith('bitter-harpoon-bleed:')) });
      }
      await tx.objectStore('rolls').put(roll);
      const now = Date.now();
      const log = { id: crypto.randomUUID(), round: encounter.round, combatantId: healer.id, text: `${healer.name} tratou o sangramento de ${target.name} com Cura: ${roll.successes} sucesso(s); ${cured ? 'sangramento estancado' : 'sangramento persiste'}.`, createdAt: now };
      const next = { ...current, encounter: { ...encounter, combatants, log: [...encounter.log, log].slice(-300), updatedAt: now }, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async applyHealingCheck(campaignId:string,combatantId:string,injuryId:string,healerId:string,roll:DiceRoll):Promise<CriticalInjuryState>{
    const result=await withTransaction(['world_state','characters','rolls'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      const sceneId=encounter.startedSceneId||encounter.id;
      if(!roll.id||roll.characterId!==healerId||roll.sceneId!==sceneId||roll.checkId!==`healing:${encounter.id}:${injuryId}:${healerId}`||roll.successes!==ForbiddenLandsDiceEngine.countSuccesses(roll.results))throw new Error('A rolagem de Cura não pertence a este curandeiro/ferimento/encontro ou tem sucessos inconsistentes.');
      const target=encounter.combatants.find(item=>item.id===combatantId);
      if(!target)throw new Error('Combatente ferido não encontrado.');
      const injury=(target.criticalInjuries||[]).find(item=>item.id===injuryId);
      if(!injury)throw new Error('Ferimento crítico não encontrado no encontro.');
      const now=Date.now();
      const updatedInjury=CriticalInjuryRulesEngine.attemptTreatment(injury,healerId,roll.successes,now);
      const characterStore=tx.objectStore('characters');
      const healer=await characterStore.get(healerId);
      if(!healer)throw new Error('Curandeiro não encontrado.');
      let patient=target.kind==='character'&&target.entityId?await characterStore.get(target.entityId):null;
      const wartsCured=Boolean(patient?.basiliskWarts&&roll.successes>0);
      const brokenAttribute=patient?.brokenAttribute||(patient?.conditions.includes('Broken')?'strength':undefined);
      const brokenValue=patient&&brokenAttribute?Math.min(patient.attributes[brokenAttribute].max,patient.attributes[brokenAttribute].current+roll.successes):undefined;
      const recoveredBrokenAttribute=typeof brokenValue==='number'&&brokenValue>0;
      const nextStrength=brokenAttribute==='strength'?brokenValue:patient?.attributes.strength.current;
      if(patient){
        const nextStates=(patient.criticalInjuryStates||[]).map(item=>item.id===injuryId?updatedInjury:item);
        const conditions=recoveredBrokenAttribute?patient.conditions.filter(item=>item!=='Broken'):patient.conditions;
        const attributes=brokenAttribute&&brokenValue!==undefined?{...patient.attributes,[brokenAttribute]:{...patient.attributes[brokenAttribute],current:brokenValue}}:patient.attributes;
        patient={...patient,criticalInjuryStates:nextStates,attributes,conditions,brokenAttribute:recoveredBrokenAttribute?undefined:patient.brokenAttribute};
        if(wartsCured)patient=BasiliskWartsRulesEngine.cure(patient);
        await characterStore.put(patient);
      }
      const combatants=encounter.combatants.map(item=>item.id===combatantId?{
        ...item,
        ...(patient?{quickSheet:{...(item.quickSheet||{}),attributes:patient.attributes}}:{}),
        ...(brokenAttribute==='strength'&&nextStrength!==undefined?{currentStrength:nextStrength,status:nextStrength>0&&item.status==='broken'?'active' as const:item.status,brokenAttribute:recoveredBrokenAttribute?undefined:item.brokenAttribute}:{}),
        criticalInjuries:(item.criticalInjuries||[]).map(state=>state.id===injuryId?updatedInjury:state),
        statusEffects:(item.statusEffects||[]).filter(effect=>!(wartsCured&&effect.id==='basilisk-warts')).map(effect=>effect.criticalInjury?.id===injuryId?{...effect,detail:`${updatedInjury.name}: ${roll.successes} sucesso(s) em Cura; ${updatedInjury.healingDaysRemaining===undefined?'sem prazo de cura definido':`${updatedInjury.healingDaysRemaining} dia(s) restantes`}.`,criticalInjury:updatedInjury}:effect),
      }:item);
      const updatedEncounter={...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId,text:`${target.name}: ${roll.successes} sucesso(s) no teste de Cura de ${healerId}; ferimento ${updatedInjury.status}.`,createdAt:now}].slice(-300),updatedAt:now};
      await tx.objectStore('rolls').put(roll);
      await store.put({...current,encounter:updatedEncounter,updatedAt:now});
      return updatedInjury;
    });
    this.notify(await WorldStateService.get(campaignId));
    return result;
  }

  static async adjustStrength(campaignId: string, combatantId: string, delta: number): Promise<WorldState> {
    const world = await withTransaction(['world_state', 'characters'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const target = encounter.combatants.find(item => item.id === combatantId);
      if (!target) throw new Error('Combatente não encontrado.');
      const strength = Math.max(0, Math.min(target.maxStrength, target.currentStrength + Math.trunc(delta)));
      const consumedDeath=target.kind==='character'&&strength<=0&&target.statusEffects?.some(effect=>effect.id.startsWith('monster-consumed:'))===true;
      const status: EncounterParticipantStatus = consumedDeath?'dead':strength <= 0 ? 'broken' : target.status === 'broken' ? 'active' : target.status;
      const now = Date.now();
      let combatants = encounter.combatants.map(item => item.id === combatantId ? { ...item, currentStrength: strength, status, ...(consumedDeath?{quickSheet:{...(item.quickSheet||{}),status:'dead'}}:{}),statusEffects:consumedDeath?(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-consumed:')):item.statusEffects, brokenAttribute: strength <= 0 ? 'strength' as const : item.brokenAttribute === 'strength' ? undefined : item.brokenAttribute } : item);
      let lifeDrainVictims:EncounterCombatant[]=[];
      let consumedVictims:EncounterCombatant[]=[];
      let magicLeechVictims:EncounterCombatant[]=[];
      if(target.kind==='monster'&&delta<=-3){
        lifeDrainVictims=combatants.filter(item=>item.statusEffects?.some(effect=>effect.id===`monster-life-drain:${target.id}`));
        combatants=combatants.map(item=>lifeDrainVictims.some(victim=>victim.id===item.id)?{...item,statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==`monster-life-drain:${target.id}`)}:item);
        for(const victim of lifeDrainVictims){
          if(victim.kind!=='character'||!victim.entityId)continue;
          const victimCharacter=await tx.objectStore('characters').get(victim.entityId);
          if(victimCharacter)await tx.objectStore('characters').put({...victimCharacter,statusEffects:(victimCharacter.statusEffects||[]).filter(effect=>effect.id!==`monster-life-drain:${target.id}`)});
        }
      }
      if(target.kind==='monster'){
        consumedVictims=combatants.filter(item=>item.statusEffects?.some(effect=>effect.id===`monster-consumed:${target.id}`&&(strength<=0||delta<=-Math.max(1,Number(effect.ongoingDamage)||3))));
        combatants=combatants.map(item=>consumedVictims.some(victim=>victim.id===item.id)?{...item,statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==`monster-consumed:${target.id}`)}:item);
        for(const victim of consumedVictims){if(victim.kind!=='character'||!victim.entityId)continue;const victimCharacter=await tx.objectStore('characters').get(victim.entityId);if(victimCharacter)await tx.objectStore('characters').put({...victimCharacter,statusEffects:(victimCharacter.statusEffects||[]).filter(effect=>effect.id!==`monster-consumed:${target.id}`)});}
        magicLeechVictims=combatants.filter(item=>item.statusEffects?.some(effect=>effect.id===`monster-magic-leech:${target.id}`&&(strength<=0||delta<=-2)));
        combatants=combatants.map(item=>magicLeechVictims.some(victim=>victim.id===item.id)?{...item,statusEffects:(item.statusEffects||[]).filter(effect=>effect.id!==`monster-magic-leech:${target.id}`)}:item);
        for(const victim of magicLeechVictims){if(victim.kind!=='character'||!victim.entityId)continue;const victimCharacter=await tx.objectStore('characters').get(victim.entityId);if(victimCharacter)await tx.objectStore('characters').put({...victimCharacter,statusEffects:(victimCharacter.statusEffects||[]).filter(effect=>effect.id!==`monster-magic-leech:${target.id}`)});}
      }
      if (target.kind === 'character' && target.entityId) {
        const characterStore = tx.objectStore('characters');
        const character = await characterStore.get(target.entityId);
        if (character) {
          const clearingStrengthBreak = strength > 0 && (character.brokenAttribute === 'strength' || (!character.brokenAttribute && character.conditions?.includes('Broken')));
          const conditions = strength <= 0
            ? [...new Set([...(character.conditions || []), 'Broken' as const])]
            : clearingStrengthBreak ? (character.conditions || []).filter(condition => condition !== 'Broken') : character.conditions;
          await characterStore.put({ ...character, ...(consumedDeath?{status:'dead' as const}:{}),conditions, brokenAttribute: strength <= 0 ? 'strength' : clearingStrengthBreak ? undefined : character.brokenAttribute, attributes: { ...character.attributes, strength: { ...character.attributes.strength, current: strength } },...(consumedDeath?{statusEffects:(character.statusEffects||[]).filter(effect=>!effect.id.startsWith('monster-consumed:'))}:{}) });
        }
      }
      const nextEncounter = this.resolveIfFinished({
        ...encounter, combatants,
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${target.name}: Força ${target.currentStrength} → ${strength}.${lifeDrainVictims.length?` Um ataque de pelo menos 3 de dano rompeu o dreno vital de ${lifeDrainVictims.map(item=>item.name).join(', ')}.`:''}${consumedVictims.length?` ${consumedVictims.map(item=>item.name).join(', ')} saiu de dentro do monstro.`:''}${magicLeechVictims.length?` O dano interrompeu o roubo mágico de ${magicLeechVictims.map(item=>item.name).join(', ')}.`:''}`, createdAt: now }].slice(-300),
        updatedAt: now,
      }, now);
      let next:WorldState = { ...current, encounter: nextEncounter, updatedAt: now };
      if(delta<0&&target.entityId)next=await BloodmarchBetaChantService.interrupt(tx,next,target.entityId,'dano de Força');
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async adjustArmor(campaignId: string, combatantId: string, delta: number): Promise<WorldState> {
    return this.update(campaignId, (encounter, now) => {
      const target = encounter.combatants.find(item => item.id === combatantId);
      if (!target) throw new Error('Combatente não encontrado.');
      const armor = Math.max(0, Number(target.armor || 0) + Math.trunc(delta));
      return { ...encounter, combatants: encounter.combatants.map(item => item.id === combatantId ? { ...item, armor } : item), log: [...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId,text:`${target.name}: Armadura ${target.armor} → ${armor}.`,createdAt:now}].slice(-300), updatedAt: now };
    });
  }

  static async setStatus(campaignId: string, combatantId: string, status: EncounterParticipantStatus): Promise<WorldState> {
    return this.update(campaignId, (encounter, now) => {
      const target = encounter.combatants.find(item => item.id === combatantId);
      if (!target) throw new Error('Combatente não encontrado.');
      const updated = {
        ...encounter,
        combatants: encounter.combatants.map(item => item.id === combatantId ? { ...item, status } : item),
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${target.name}: estado alterado para ${status}.`, createdAt: now }].slice(-300),
        updatedAt: now,
      };
      return this.resolveIfFinished(updated, now);
    });
  }

  static async end(campaignId: string, reason = 'Encontro encerrado.'): Promise<WorldState> {
    // U19: an ordinary "Encerrar" click cannot certify an official battle while
    // both sides are still fighting. Surrender, fleeing and defeat must first be
    // reflected in real combatant states, or the original journey stays open.
    const world = await withTransaction('world_state','readwrite',async tx => {
      const store=tx.store;
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const encounter=current.encounter;
      if(!encounter||encounter.status!=='active')throw new Error('Não há encontro ativo.');
      if(encounter.combatants[encounter.turnIndex]?.quickSheet?.pendingOfficialProcessionEntAttack||
        encounter.combatants[encounter.turnIndex]?.quickSheet?.pendingOfficialMonsterAttack)
        throw new Error('Resolva os alvos do ataque D6 pendente do monstro antes de encerrar o combate.');
      const official=encounter.startedSceneId && current.journey.events.find(event=>
        event.id===encounter.startedSceneId && event.status==='open' &&
        (event.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter');
      const partyActive=encounter.combatants.some(c=>c.side==='party'&&c.status==='active');
      const enemyActive=encounter.combatants.some(c=>c.side==='enemy'&&c.status==='active');
      if(official&&partyActive&&enemyActive)
        throw new Error('Combate oficial ainda tem os dois lados ativos. Registre derrota, rendição ou fuga nos estados dos combatentes antes de encerrar; apenas texto ou turnos não comprovam o desfecho.');
      const now=Date.now();
      const next={...current,encounter:{...encounter,status:'resolved' as const,resolvedAt:now,
        log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,text:reason,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now};
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async moveCombatant(campaignId: string, combatantId: string, x: number, y: number, overload?: { roll: import('../types').DiceRoll; failureChoice?: OverloadFailureChoice }, movement?: { run?: boolean; crawl?: boolean }): Promise<WorldState> {
    const world = await withTransaction(['world_state', 'characters', 'rolls'], 'readwrite', async tx => {
      const current = WorldStateService.normalize(await tx.objectStore('world_state').get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const combatant = encounter.combatants.find(item => item.id === combatantId);
      if (!combatant) throw new Error('Combatente não encontrado.');
      if (combatant.status !== 'active' && !combatant.brokenAttribute) throw new Error('Um combatente inativo não pode se mover.');
      const ran = movement?.run === true || Boolean(overload);
      const crawled = movement?.crawl === true;
      if (ran && crawled) throw new Error('Escolha Correr ou Arrastar-se, não ambos.');
      if ((ran || crawled) && encounter.combatants[encounter.turnIndex]?.id !== combatantId) throw new Error('Movimento mecânico só pode ser realizado pelo combatente do turno atual.');
      if (ran && combatant.statusEffects?.some(effect => effect.id === 'condition:prone')) throw new Error(`${combatant.name} está Caído e precisa Arrastar-se ou Levantar-se.`);
      if (crawled && !combatant.statusEffects?.some(effect => effect.id === 'condition:prone') && !['strength','agility'].includes(String(combatant.brokenAttribute || ''))) throw new Error('Arrastar-se exige estar Caído.');
      if (monsterIncapacitated(combatant)) throw new Error(`${combatant.name} está incapacitado e não pode se mover até o próximo turno.`);
      if(combatant.statusEffects?.some(effect=>effect.id==='magic:br-icy-breath'))throw new Error(`${combatant.name} está congelado pelo Sopro Gelado e não pode se mover até o efeito terminar.`);
      if(combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-webbed:')))throw new Error(`${combatant.name} está preso na teia e não pode se mover até escapar.`);
      if(combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-grappled:')||effect.id.startsWith('core-grappled:')))throw new Error(`${combatant.name} está Agarrado e precisa escapar antes de se deslocar.`);
      if(combatant.statusEffects?.some(effect=>effect.id.startsWith('official-ent-roots:')||effect.id.startsWith('official-ent-embrace:')))throw new Error(`${combatant.name} está preso por um Ent e não pode se deslocar antes de ser libertado.`);
      if(combatant.statusEffects?.some(effect=>effect.id.startsWith('core-grappling:')))throw new Error(`${combatant.name} está mantendo um Agarrão e não pode se deslocar.`);
      if(combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-dragged:')))throw new Error(`${combatant.name} está sendo arrastado e precisa vencer a disputa de Força + Potência para escapar.`);
      if(combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-life-drain:')))throw new Error(`${combatant.name} está preso pelo dreno vital e só sai quando um aliado causar 3 ou mais de dano no monstro.`);
      if(combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-magic-leech:')))throw new Error(`${combatant.name} está preso pelo Imp e não pode se mover antes de vencer a disputa de Discernimento.`);
      const now = Date.now();
      let nextCharacter: import('../types').Character | undefined;
      let dropped = encounter.droppedEquipment || [];
      if (combatant.kind === 'character') {
        if (!combatant.entityId) throw new Error('A personagem não está vinculada a uma ficha.');
        const characterStore = tx.objectStore('characters');
        nextCharacter = await characterStore.get(combatant.entityId);
        if (!nextCharacter) throw new Error('Ficha da personagem não encontrada.');
        CharacterActionRulesEngine.assertCanAct(nextCharacter, crawled ? 'crawl' : ran ? 'run' : 'general');
        if (nextCharacter && EncumbranceRulesEngine.calculate(nextCharacter).overloaded) {
          const expectedCheckPrefix=`overload:${encounter.id}:run:${encounter.round}:${combatant.id}:${x}:${y}:${nextCharacter.id}:`;
          if (!overload || !overload.roll.id || overload.roll.characterId !== nextCharacter.id || overload.roll.sceneId !== encounter.id || !overload.roll.checkId?.startsWith(expectedCheckPrefix) || overload.roll.skillName !== 'endurance' || overload.roll.attributeName !== 'strength' || overload.roll.successes !== ForbiddenLandsDiceEngine.countSuccesses(overload.roll.results)) {
            throw new Error('A personagem está sobrecarregada e precisa passar em Resiliência para correr neste turno.');
          }
          await tx.objectStore('rolls').put(overload.roll);
          if (overload.roll.successes === 0) {
            if (!overload.failureChoice) throw new Error('Falha em Resiliência: escolha largar um item ou sofrer 1 dano de Agilidade para continuar.');
            const consequence = EncumbranceRulesEngine.applyFailure(nextCharacter, overload.failureChoice);
            nextCharacter = consequence.character;
            await characterStore.put(nextCharacter);
            if (overload.failureChoice.kind === 'drop') {
              dropped = [...dropped, {
                id: `dropped:${overload.roll.id}`,
                characterId: nextCharacter.id,
                characterName: nextCharacter.name,
                collection: overload.failureChoice.collection,
                item: consequence.droppedItem || {},
                droppedAt: now,
              }];
            }
          }
        } else if (overload) {
          throw new Error('Esta personagem não está sobrecarregada; a rolagem de corrida ficou desatualizada.');
        }
      }
      const runCost = ran && nextCharacter ? CriticalInjuryRulesEngine.actionCostFor(nextCharacter.criticalInjuryStates, 'run') : null;
      const fastShooterFreeRun=ran&&runCost!=='slow'&&combatant.statusEffects?.some(effect=>effect.id===`core-fast-shooter-run:${encounter.round}`)===true;
      const movementCost: 'fast'|'slow'|null = crawled ? 'slow' : ran ? (runCost === 'slow' ? 'slow' : fastShooterFreeRun?null:'fast') : null;
      if (movementCost === 'slow' && !combatant.slowActionAvailable) throw new Error(`${combatant.name} precisa da ação lenta disponível para ${crawled ? 'Arrastar-se' : 'Correr'}.`);
      if (movementCost === 'fast' && !canSpendFastAction(combatant)) throw new Error(`${combatant.name} precisa de uma ação disponível que possa ser usada como rápida para Correr.`);
      const nextEncounter = {
        ...encounter,
        combatants: encounter.combatants.map(item => item.id === combatantId ? {
          ...item,
          x: Math.max(.04, Math.min(.96, x)),
          y: Math.max(.06, Math.min(.94, y)),
          ...(fastShooterFreeRun?{statusEffects:(item.statusEffects||[]).filter(effect=>!effect.id.startsWith('core-fast-shooter-run:'))}:{}),
          ...(movementCost === 'slow' ? { slowActionAvailable: false } : movementCost === 'fast' ? (()=>{const spent=spendFastAction(item);return { fastActionAvailable:spent.fastActionAvailable, slowActionAvailable:spent.slowActionAvailable, statusEffects:spent.statusEffects };})() : {}),
          ...(nextCharacter ? { currentStrength: nextCharacter.attributes.strength.current, brokenAttribute: nextCharacter.brokenAttribute, status: nextCharacter.brokenAttribute ? 'broken' as const : item.status } : {}),
        } : item),
        log: movementCost || overload?.roll || fastShooterFreeRun ? [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${combatant.name} ${crawled ? 'se Arrastou' : ran ? 'Correu' : 'foi reposicionado'}${overload?.roll ? ` após Resiliência (${overload.roll.successes} sucesso(s))` : ''}; ${fastShooterFreeRun?'Correr já estava incluído no disparo por Atirador Veloz 2+':`gastou ação ${movementCost === 'slow' ? 'lenta' : movementCost === 'fast' ? 'rápida' : 'nenhuma'}`}.`, createdAt: now }].slice(-300) : encounter.log,
        droppedEquipment: dropped,
        updatedAt: now,
      };
      let updated:WorldState = { ...current, encounter: nextEncounter, updatedAt: now };
      if(combatant.entityId&&(combatant.x!==x||combatant.y!==y||movementCost!==null))
        updated=await BloodmarchBetaChantService.interrupt(tx,updated,combatant.entityId,'movimento');
      await tx.objectStore('world_state').put(updated);
      return updated;
    });
    this.notify(world);
    return world;
  }

  static async addNpc(campaignId: string, npc: import('../types').Npc, side: import('../types').EncounterSide = 'neutral'): Promise<WorldState> {
    return this.update(campaignId, (encounter, now) => {
      if (encounter.combatants.some(item => item.entityId === npc.id)) return encounter;
      const combatant: EncounterCombatant = { id:`npc:${npc.id}`, entityId:npc.id, name:npc.name, kind:'npc', side, initiative:initiative(), currentStrength:3, maxStrength:3, armor:0, conditions:[], fastActionAvailable:true, slowActionAvailable:true, status:'active', notes:[npc.attitude,npc.goals,npc.lastAction].filter(Boolean).join(' · '), tokenId:npc.tokenId, x:side==='party'?.28:side==='enemy'?.72:.5, y:.5, size:1, quickSheet:{kin:npc.kin,profession:npc.profession,faction:npc.faction,attitude:npc.attitude,goals:npc.goals,fear:npc.fear,relationship:npc.relationshipToParty,personality:npc.personality} };
      return {...encounter,combatants:[...encounter.combatants,combatant].sort((a,b)=>a.initiative-b.initiative),log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:combatant.id,text:`${npc.name} entrou no encontro como ${side}.`,createdAt:now}].slice(-300),updatedAt:now};
    });
  }

  static async addNarration(campaignId: string, text: string): Promise<WorldState> {
    const value=String(text||'').trim();if(!value)throw new Error('Informe a narrativa do combate.');
    return this.update(campaignId,(encounter,now)=>({...encounter,narrativeBlocks:[...(encounter.narrativeBlocks||[]),{id:crypto.randomUUID(),text:value.slice(0,2000),round:encounter.round,createdAt:now}].slice(-80),log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,text:value.slice(0,500),createdAt:now}].slice(-300),updatedAt:now}));
  }

  static async setBackground(campaignId: string, backgroundImageUrl?: string): Promise<WorldState> {
    return this.update(campaignId,(encounter,now)=>({...encounter,backgroundImageUrl,updatedAt:now}));
  }

  private static async update(campaignId: string, mutate: (encounter: EncounterState, now: number) => EncounterState, allowResolved = false): Promise<WorldState> {
    const world = await withTransaction('world_state', 'readwrite', async tx => {
      const current = WorldStateService.normalize(await tx.store.get(campaignId), campaignId);
      if (!current.encounter || (!allowResolved && current.encounter.status !== 'active')) throw new Error('Não há encontro ativo.');
      const next = { ...current, encounter: mutate(current.encounter, Date.now()), updatedAt: Date.now() };
      await tx.store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  /** Ações de combate validam o combatente do turno e reações na mesma transação que grava o gasto. */
  private static async updateCombatActions(
    campaignId: string,
    additionalCombatantIds: string[],
    mutate: (encounter: EncounterState, now: number) => EncounterState,
    rolls: DiceRoll[] = [],
  ): Promise<WorldState> {
    const stores: (keyof BraseiroDB)[] = ['world_state', 'characters', ...(rolls.length ? ['rolls' as keyof BraseiroDB] : [])];
    const world = await withTransaction(stores, 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      if (!current.encounter || current.encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const encounter = current.encounter;
      const currentActor = encounter.combatants[encounter.turnIndex];
      const guarded = [currentActor, ...additionalCombatantIds.map(id => encounter.combatants.find(item => item.id === id))].filter(Boolean) as EncounterCombatant[];
      for (const combatant of guarded) {
        if (monsterIncapacitated(combatant)) throw new Error(`${combatant.name} está incapacitado e não pode realizar ações até o próximo turno.`);
        if (combatant.kind !== 'character' || !combatant.entityId) continue;
        const character = await tx.objectStore('characters').get(combatant.entityId);
        if (!character) throw new Error('Ficha do combatente não encontrada.');
        CharacterActionRulesEngine.assertCanAct(character);
      }
      for (const roll of rolls) await tx.objectStore('rolls').put(roll);
      let next:WorldState = { ...current, encounter: mutate(encounter, Date.now()), updatedAt: Date.now() };
      // Interrupt only if the actor actually spent an action, moved, or changed status.
      // Reactive defense is checked independently for its participant.
      for(const before of guarded){
        const after=next.encounter?.combatants.find(item=>item.id===before.id);
        if(after&&before.entityId&&BloodmarchBetaChantService.actionChanged(before,after))
          next=await BloodmarchBetaChantService.interrupt(tx,next,before.entityId,'ação de combate');
      }
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static resolveIfFinished(encounter: EncounterState, now = Date.now(), forcedReason?: string): EncounterState {
    if (encounter.status === 'resolved') return encounter;
    const partyActive = encounter.combatants.some(item => item.side === 'party' && item.status === 'active');
    const enemyActive = encounter.combatants.some(item => item.side === 'enemy' && item.status === 'active');
    const scheduledR88Reinforcements=encounter.officialEncounter?.encounterId===37&&encounter.officialEncounter.reinforcementsPending===true;
    const octopusRetreatMustBeRecorded=encounter.officialEncounter?.encounterId===40&&
      encounter.combatants.some(item=>item.entityId==='core-bestiary:giant-octopus'&&item.status==='broken');
    const shouldResolve = Boolean(forcedReason) || (!partyActive && encounter.combatants.some(item => item.side === 'party')) ||
      (!enemyActive && encounter.combatants.some(item => item.side === 'enemy')&&!scheduledR88Reinforcements&&!octopusRetreatMustBeRecorded);
    if (!shouldResolve) return encounter;
    const reason = forcedReason || (!partyActive ? 'O grupo não possui combatentes ativos.' : 'Não restaram adversários ativos.');
    return {
      ...encounter,
      status: 'resolved',
      resolvedAt: now,
      log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, text: reason, createdAt: now }].slice(-300),
      updatedAt: now,
    };
  }

  /** COMBATE MONTADO: Montar / Desmontar (ação rápida). */
  static async mount(campaignId: string, combatantId: string, mountData?: {
    mountId?: string;
    mountName?: string;
    mountStrength?: number;
    mountMaxStrength?: number;
    movementRate?: number;
    agility?: number;
  }): Promise<WorldState> {
    return this.updateCombatActions(campaignId, [], (encounter, now) => {
      const actor = encounter.combatants[encounter.turnIndex];
      if (!actor || actor.id !== combatantId) throw new Error('Apenas o combatente do turno atual pode Montar.');
      if (actor.status !== 'active') throw new Error('Combatente inativo não pode Montar.');
      if (actor.mounted) throw new Error(`${actor.name} já está montado.`);
      if (!canSpendFastAction(actor)) throw new Error(`${actor.name} não possui ação rápida disponível para Montar.`);
      if (actor.statusEffects?.some(e => e.id === 'condition:prone')) throw new Error(`${actor.name} está Caído e precisa se levantar antes de Montar.`);

      const mount = {
        mountId: mountData?.mountId || `mount-${crypto.randomUUID().slice(0, 8)}`,
        mountName: mountData?.mountName || 'Cavalo de Montaria',
        mountStrength: mountData?.mountStrength ?? 6,
        mountMaxStrength: mountData?.mountMaxStrength ?? 6,
        mountMovementRate: mountData?.movementRate ?? 2,
        mountAgility: mountData?.agility ?? 3,
        status: 'ready' as const,
      };

      return {
        ...encounter,
        combatants: encounter.combatants.map(item => item.id === combatantId
          ? {
              ...spendFastAction(item),
              mounted: mount,
              statusEffects: [
                ...(item.statusEffects || []).filter(e => !e.id.startsWith('core-mounted:')),
                { id: `core-mounted:${mount.mountId}`, name: 'Montado', detail: `Montado em ${mount.mountName}. Movimento usa Adestramento.` }
              ]
            }
          : item),
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${actor.name} montou em ${mount.mountName} com uma ação rápida.`, createdAt: now }].slice(-300),
        updatedAt: now,
      };
    });
  }

  static async dismount(campaignId: string, combatantId: string): Promise<WorldState> {
    return this.updateCombatActions(campaignId, [], (encounter, now) => {
      const actor = encounter.combatants[encounter.turnIndex];
      if (!actor || actor.id !== combatantId) throw new Error('Apenas o combatente do turno atual pode Desmontar.');
      if (actor.status !== 'active') throw new Error('Combatente inativo não pode Desmontar.');
      if (!actor.mounted) throw new Error(`${actor.name} não está montado.`);
      if (!canSpendFastAction(actor)) throw new Error(`${actor.name} não possui ação rápida disponível para Desmontar.`);

      const mountName = actor.mounted.mountName;
      return {
        ...encounter,
        combatants: encounter.combatants.map(item => item.id === combatantId
          ? {
              ...spendFastAction(item),
              mounted: undefined,
              statusEffects: (item.statusEffects || []).filter(e => !e.id.startsWith('core-mounted:')),
            }
          : item),
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text: `${actor.name} desmontou de ${mountName} com uma ação rápida.`, createdAt: now }].slice(-300),
        updatedAt: now,
      };
    });
  }

  /** COMBATE MONTADO: Movimentação da Montaria (substituição por Adestramento). */
  static async mountedMove(campaignId: string, combatantId: string, rng: () => number = Math.random): Promise<{
    world: WorldState;
    roll: DiceRoll;
    skillUsed: string;
    movementZones: number;
  }> {
    const result = await withTransaction(['world_state', 'characters', 'rolls'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const actor = encounter.combatants[encounter.turnIndex];
      if (!actor || actor.id !== combatantId) throw new Error('Apenas o combatente do turno atual pode se mover.');
      if (actor.status !== 'active') throw new Error('Combatente inativo não pode se mover.');
      if (!actor.mounted) throw new Error(`${actor.name} não está montado para realizar movimento de montaria.`);
      if (actor.mounted.status === 'dying') throw new Error('A montaria está moribunda e não pode se mover.');
      if (!canSpendFastAction(actor)) throw new Error(`${actor.name} não possui ação rápida disponível para movimentação.`);

      const chars = tx.objectStore('characters');
      const character = actor.kind === 'character' && actor.entityId ? await chars.get(actor.entityId) : undefined;
      if (character) CharacterActionRulesEngine.assertCanAct(character);

      // Regra: montado substitui Movimentação por Adestramento (Animal Handling)
      const skillName = 'animal_handling';
      const skillRank = combatantSkill(actor, character, ['Animal Handling', 'Adestramento', 'animal_handling']);
      const agilityAttr = combatantAttribute(actor, character, 'agility');

      const roll = ForbiddenLandsDiceEngine.roll(
        { base: agilityAttr, skill: skillRank, gear: 0 },
        {
          characterId: character?.id || null,
          sceneId: encounter.startedSceneId || encounter.id,
          checkId: `mounted-move:${encounter.id}:${encounter.round}:${actor.id}`,
          rng,
          criticalInjuries: character?.criticalInjuryStates || actor.criticalInjuries,
          skillName: 'animal_handling',
          attributeName: 'agility',
        }
      );
      await tx.objectStore('rolls').put(roll);

      const movementRate = actor.mounted.mountMovementRate || 2;
      const zonesMoved = roll.successes > 0 ? movementRate : Math.max(1, movementRate - 1);
      const now = Date.now();

      const combatants = encounter.combatants.map(item => item.id === combatantId
        ? spendFastAction(item)
        : item
      );

      const text = `${actor.name} realizou Movimentação Montada usando Adestramento (${roll.successes} sucesso(s)): avançou ${zonesMoved} zona(s) no dorso de ${actor.mounted.mountName}.`;
      const nextEncounter = this.resolveIfFinished({
        ...encounter,
        combatants,
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId: actor.id, text, createdAt: now }].slice(-300),
        updatedAt: now,
      }, now);

      const world = { ...current, encounter: nextEncounter, updatedAt: now };
      await store.put(world);
      return { world, roll, skillUsed: 'animal_handling', movementZones: zonesMoved };
    });

    this.notify(result.world);
    return result;
  }

  /** COMBATE MONTADO: Salvar montaria moribunda (janela de 1 hora / teste de Adestramento ou Cura). */
  static async saveDyingMount(campaignId: string, healerId: string, riderId: string, roll: DiceRoll): Promise<WorldState> {
    const world = await withTransaction(['world_state', 'characters', 'rolls'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const rider = encounter.combatants.find(item => item.id === riderId);
      if (!rider || !rider.mounted) throw new Error('Montaria do cavaleiro não encontrada.');
      if (rider.mounted.status !== 'dying') throw new Error('A montaria não está em estado moribundo.');

      const saved = roll.successes > 0;
      await tx.objectStore('rolls').put(roll);
      const now = Date.now();

      const combatants = encounter.combatants.map(item => item.id === riderId ? {
        ...item,
        mounted: {
          ...item.mounted!,
          mountStrength: saved ? 1 : 0,
          status: (saved ? 'lame' : 'dead') as 'ready' | 'lame' | 'dying' | 'dead',
        },
      } : item);

      const text = `${rider.name}: tentativa de salvar montaria moribunda (${roll.successes} sucesso(s)) — ${saved ? 'montaria estabilizada (manca, 1 Força)' : 'montaria não resistiu e pereceu'}.`;
      const nextEncounter = this.resolveIfFinished({
        ...encounter,
        combatants,
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId: riderId, text, createdAt: now }].slice(-300),
        updatedAt: now,
      }, now);

      const next = { ...current, encounter: nextEncounter, updatedAt: now };
      await store.put(next);
      return next;
    });

    this.notify(world);
    return world;
  }

  /** BERSERKER: Encerramento da Fúria Berserker (Fim do Encontro ou voluntário). */
  static async endBerserkerRage(campaignId: string, combatantId: string): Promise<WorldState> {
    const world = await withTransaction(['world_state', 'characters'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter) throw new Error('Não há encontro.');
      const combatant = encounter.combatants.find(item => item.id === combatantId);
      if (!combatant) throw new Error('Combatente não encontrado.');
      if (!combatant.rageState?.active) throw new Error(`${combatant.name} não está em Fúria Berserker.`);

      const now = Date.now();
      const chars = tx.objectStore('characters');
      let character = combatant.kind === 'character' && combatant.entityId ? await chars.get(combatant.entityId) : undefined;

      const strength = combatant.currentStrength;
      const isBroken = strength <= 0;

      const updatedCombatant: EncounterCombatant = {
        ...combatant,
        status: isBroken ? 'broken' : combatant.status,
        brokenAttribute: isBroken ? 'strength' : combatant.brokenAttribute,
        rageState: {
          active: false,
          rank: combatant.rageState.rank,
          startedRound: combatant.rageState.startedRound,
          exhausted: true,
        },
        statusEffects: [
          ...(combatant.statusEffects || []).filter(e => !e.id.startsWith('core-berserk:')),
          { id: `core-berserk-exhausted:${now}`, name: 'Exausto pós-Fúria', detail: 'Fúria Berserker encerrada; exausto até descansar.' }
        ]
      };

      if (character) {
        const conditions = isBroken
          ? Array.from(new Set([...(character.conditions || []), 'Broken' as const]))
          : character.conditions;
        await chars.put({
          ...character,
          conditions,
          brokenAttribute: isBroken ? 'strength' : character.brokenAttribute,
        });
      }

      const combatants = encounter.combatants.map(item => item.id === combatantId ? updatedCombatant : item);
      const text = `${combatant.name}: Fúria Berserker encerrou-se.${isBroken ? ' Ficou Acabado por Força 0.' : ' Permanece de pé, porém exausto.'}`;
      const nextEncounter = this.resolveIfFinished({
        ...encounter,
        combatants,
        log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId, text, createdAt: now }].slice(-300),
        updatedAt: now,
      }, now);

      const next = { ...current, encounter: nextEncounter, updatedAt: now };
      await store.put(next);
      return next;
    });

    this.notify(world);
    return world;
  }

  /** HIDDEN COMBINATIONS: Iniciar disputa de cartas ocultas ligada ao encontro real. */
  static async startHiddenExchange(campaignId: string, combatantAId: string, combatantBId: string): Promise<WorldState> {
    const world = await withTransaction('world_state', 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');
      const combatantA = encounter.combatants.find(item => item.id === combatantAId);
      const combatantB = encounter.combatants.find(item => item.id === combatantBId);
      if (!combatantA || !combatantB) throw new Error('Ambos os combatentes precisam existir no encontro.');

      const now = Date.now();
      const updatedEncounter: EncounterState = {
        ...encounter,
        exchangeCursor: {
          round: encounter.round,
          step: 1,
          status: 'planning',
          combatantAId,
          combatantBId,
          declarations: {},
          history: [],
        },
        log: [...encounter.log, {
          id: crypto.randomUUID(),
          round: encounter.round,
          text: `Iniciada troca de cartas táticas ocultas entre ${combatantA.name} e ${combatantB.name}.`,
          createdAt: now,
        }].slice(-300),
        updatedAt: now,
      };

      const next = { ...current, encounter: updatedEncounter, updatedAt: now };
      await store.put(next);
      return next;
    });

    this.notify(world);
    return world;
  }

  /** HIDDEN COMBINATIONS: Resolver passo da troca de cartas no EncounterService. */
  static async resolveHiddenExchangeStep(
    campaignId: string,
    declarationA: CardDeclaration,
    declarationB: CardDeclaration,
    combatantA: CombatantContext,
    combatantB: CombatantContext,
    options: {
      initiativeWinnerId?: string;
      step: 1 | 2;
      successesA?: number;
      successesB?: number;
      rng?: () => number;
      applyDamageToEncounter?: boolean;
    }
  ): Promise<{
    world: WorldState;
    stepOutcome: ReturnType<typeof HiddenCombinationsEngine.resolveStep>;
  }> {
    const stepOutcome = HiddenCombinationsEngine.resolveStep({
      combatantA,
      combatantB,
      declarationA,
      declarationB,
      step: options.step,
      successesA: options.successesA,
      successesB: options.successesB,
      rng: options.rng,
    });

    const world = await withTransaction(['world_state', 'characters'], 'readwrite', async tx => {
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const encounter = current.encounter;
      if (!encounter || encounter.status !== 'active') throw new Error('Não há encontro ativo.');

      const now = Date.now();
      let combatants = [...encounter.combatants];

      if (options.applyDamageToEncounter) {
        if (stepOutcome.damageToB > 0) {
          combatants = combatants.map(item => {
            if (item.id !== combatantB.id) return item;
            const nextStr = Math.max(0, item.currentStrength - stepOutcome.damageToB);
            return {
              ...item,
              currentStrength: nextStr,
              status: nextStr <= 0 ? 'broken' as const : item.status,
              brokenAttribute: nextStr <= 0 ? 'strength' as const : item.brokenAttribute,
            };
          });
        }
        if (stepOutcome.damageToA > 0) {
          combatants = combatants.map(item => {
            if (item.id !== combatantA.id) return item;
            const nextStr = Math.max(0, item.currentStrength - stepOutcome.damageToA);
            return {
              ...item,
              currentStrength: nextStr,
              status: nextStr <= 0 ? 'broken' as const : item.status,
              brokenAttribute: nextStr <= 0 ? 'strength' as const : item.brokenAttribute,
            };
          });
        }
      }

      const nextCursor: NonNullable<EncounterState['exchangeCursor']> = {
        round: encounter.round,
        step: options.step === 1 ? 2 : 1,
        status: options.step === 1 ? 'step1_resolved' : 'completed',
        combatantAId: combatantA.id,
        combatantBId: combatantB.id,
        declarations: {
          ...(encounter.exchangeCursor?.declarations || {}),
          [combatantA.id]: { ...(encounter.exchangeCursor?.declarations?.[combatantA.id] || {}), [`step${options.step}`]: declarationA },
          [combatantB.id]: { ...(encounter.exchangeCursor?.declarations?.[combatantB.id] || {}), [`step${options.step}`]: declarationB },
        },
        history: [
          ...(encounter.exchangeCursor?.history || []),
          { step: options.step, stepOutcome },
        ],
      };

      const updatedEncounter = this.resolveIfFinished({
        ...encounter,
        combatants,
        exchangeCursor: nextCursor,
        log: [...encounter.log, {
          id: crypto.randomUUID(),
          round: encounter.round,
          text: `Troca de cartas passo ${options.step} resolvido: Dano em A: ${stepOutcome.damageToA}, Dano em B: ${stepOutcome.damageToB}.`,
          createdAt: now,
        }].slice(-300),
        updatedAt: now,
      }, now);

      const next = { ...current, encounter: updatedEncounter, updatedAt: now };
      await store.put(next);
      return next;
    });

    this.notify(world);
    return { world, stepOutcome };
  }

  private static armorValue(armor: any[]) {
    return (armor || []).reduce((best, item: any) => Math.max(best, Number(item?.armor || item?.rating || item?.protection || 0)), 0);
  }

  private static notify(world: WorldState) {
    if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('world-state-updated', { detail: world }));
  }
}
