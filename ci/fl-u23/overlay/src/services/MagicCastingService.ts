import type { Character } from '../types';
import { withTransaction } from '../db/db';
import { ForbiddenLandsMagicRulesEngine } from './ForbiddenLandsMagicRulesEngine';
import { ForbiddenLandsMagicCatalogService } from './ForbiddenLandsMagicCatalogService.ts';
import { MagicTalentAccessService } from './MagicTalentAccessService';
import { verifyElementalIngredient, resolveBitterReachResistance } from './BitterReachSpellRulesEngine.ts';
import { ForbiddenLandsSpellEffectEngine, type SpellTarget, type SpellEffectResult } from './ForbiddenLandsSpellEffectEngine.ts';
import { DiseaseRulesEngine } from './DiseaseRulesEngine.ts';
import { WorldStateService } from './WorldStateService.ts';
import { CriticalInjuryRulesEngine } from './CriticalInjuryRulesEngine.ts';
import { BasiliskWartsRulesEngine } from './BasiliskWartsRulesEngine.ts';
import { WillpowerRulesEngine } from './WillpowerRulesEngine.ts';
import { ForbiddenLandsDiceEngine } from './ForbiddenLandsDiceEngine.ts';
import { BitterReachAreaSpellEngine, distributeAreaPower, isBitterReachAreaSpell, type AreaResolution } from './BitterReachAreaSpellEngine.ts';
import type { CriticalInjuryState } from '../types';
import { BitterReachElementalEngine } from './BitterReachElementalEngine.ts';
import { BitterReachMagicTimeEngine } from './BitterReachMagicTimeEngine.ts';
import { BloodmarchBetaModule, isBloodmarchBetaSpell } from './BloodmarchBetaModule.ts';
import { BloodmarchBetaCombatEngine } from './BloodmarchBetaCombatEngine.ts';
import { BloodmarchBetaFireGuard } from './BloodmarchBetaFireGuard.ts';
import { OfficialEntDamageRulesEngine } from './OfficialEntDamageRulesEngine.ts';
import { OfficialEncounterCombatRulesEngine } from './OfficialEncounterCombatRulesEngine.ts';
import { OfficialEncounterRulesEngine } from './OfficialEncounterRulesEngine.ts';
import { BloodmarchBetaMentalGuard } from './BloodmarchBetaMentalGuard.ts';
import { EncumbranceRulesEngine } from './EncumbranceRulesEngine.ts';
import { BloodmarchBetaResistanceGuard } from './BloodmarchBetaResistanceGuard.ts';
import { BloodmarchBetaSleepEngine, type SleepClassification, type SleepSpellId } from './BloodmarchBetaSleepEngine.ts';
import {BloodmarchBetaChantService} from './BloodmarchBetaChantService.ts';
import {BloodmarchBetaMaterialWorldEngine, type MaterialOperation} from './BloodmarchBetaMaterialWorldEngine.ts';
import {BloodmarchBetaMentalSceneEngine, type MentalPlan} from './BloodmarchBetaMentalSceneEngine.ts';
import {BloodmarchBetaActiveWorldEngine, type ActiveWorldPlan} from './BloodmarchBetaActiveWorldEngine.ts';
import {BloodmarchBetaDreamDirectiveEngine, type DirectivePlan} from './BloodmarchBetaDreamDirectiveEngine.ts';
import {BloodmarchBetaPropheticDreamsEngine, type PropheticPlan} from './BloodmarchBetaPropheticDreamsEngine.ts';
import {BloodmarchBetaDreamTravelEngine, type DreamTravelPlan} from './BloodmarchBetaDreamTravelEngine.ts';
import {BloodmarchBetaMaraEngine, type MaraPlan} from './BloodmarchBetaMaraEngine.ts';
import {BloodmarchBetaFireWyrmEngine, type FireWyrmPlan} from './BloodmarchBetaFireWyrmEngine.ts';

const normalize = (value: unknown) => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').replace(/[^a-z0-9]+/g,' ').trim();
const eventId = (characterId: string) => `magic-casts:${characterId}`;

export function parseDurationToQuartersOrMinutes(duration: string | undefined, power: number): { remainingQuarters?: number; remainingMinutes?: number; ongoingRounds?: number } {
  if (!duration) return {};
  const d = duration.toLowerCase();
  if (d.includes('qd')) {
    const match = d.match(/(\d+)\s*qd/);
    const count = match ? parseInt(match[1]) : power;
    return { remainingQuarters: count };
  }
  if (d.includes('dia')) {
    const match = d.match(/(\d+)\s*dia/);
    const count = match ? parseInt(match[1]) : 1;
    return { remainingQuarters: count * 4 };
  }
  if (d.includes('minuto')) {
    const match = d.match(/(\d+)\s*minuto/);
    const count = match ? parseInt(match[1]) : 15;
    return { remainingMinutes: count };
  }
  if (d.includes('rodada')) {
    const match = d.match(/(\d+)\s*rodada/);
    return { ongoingRounds: match ? parseInt(match[1]) : power };
  }
  if (d.includes('turno')) {
    const match = d.match(/(\d+)\s*turno/);
    return { remainingMinutes: (match ? parseInt(match[1]) : 1) * 15 };
  }
  return {};
}

export function isBlind(actor: any): boolean {
  if (!actor) return false;
  const quarters = actor.magicMishapState?.blindnessQuartersRemaining || actor.quickSheet?.magicMishapState?.blindnessQuartersRemaining || 0;
  if (quarters > 0) return true;
  const effects = actor.statusEffects || [];
  return effects.some((e: any) => e.id === 'magic:blind' || e.id.startsWith('magic-mishap:blindness:') || e.blinded === true || e.id === 'monster-blinded');
}

export type CastCharacterMagicInput = {
  characterId: string;
  spellId: string;
  spellName: string;
  spellRank: number;
  talentName: string;
  willpowerSpent: number;
  safe?: boolean;
  ingredient?: boolean;
  obscure?: boolean;
  bindingWillpower?: number;
  ritual?: boolean;
  sessionId?: string;
  effectTargetCharacterId?: string;
  /** The Bloodmarch beta p.29: each final Power Level permits one additional person nearby. */
  fireFriendAllyCharacterIds?: string[];
  /** The player explicitly verified that the chosen persons are within vicinity. */
  fireFriendVicinityConfirmed?: boolean;
  /** Bloodmarch beta p.31: allied recipients, no implicit caster protection. */
  mentalStrengthTargetCharacterIds?: string[];
  mentalStrengthVicinityConfirmed?: boolean;
  /** Bloodmarch beta p.32: OTHER intelligent persons, up to final Power Level including the primary. */
  amnesiaAdditionalTargetIds?: string[];
  /** Bloodmarch beta: primary plus one extra sleeping creature per TWO extra power levels. */
  nightmaresAdditionalTargetIds?: string[];
  /** The Bloodmarch beta p.32: optional extra recipients (characters or active NPC combatants). */
  tracelessRecipientIds?: string[];
  /** Explicit vicinity confirmation for every additional recipient. */
  tracelessVicinityConfirmed?: boolean;
  effectTargetKind?: 'character'|'demon'|'undead'|'animal'|'other';
  effectContext?: Record<string, any>;
  /** Explicit, player-confirmed set of combatant IDs in the affected zone. */
  sleepClassification?: Record<string,SleepClassification>;
  /** The caster confirms the COMPLETE eligible roster, including allies. */
  sleepRosterConfirmed?: boolean;
  areaTargetIds?: string[];
  /** Distribution by combatant ID; mandatory for multi-target allocation spells. */
  areaDamageAllocation?: Record<string, number>;
  rng?: () => number;
};

export class MagicCastingService {
  /** Premonição: rolagem antecipada e escolha são duas transações separadas;
   * NENHUM efeito da ação é executado por esta interface até o jogador aceitá-la.
   * O resultado NÃO pode ser forçado, trocado ou rerrolado após uma recusa. */
  static async previewBloodmarchPremonition(input:{campaignId:string;casterId:string;actorId:string;effectId:string;actionId:string;
    description:string;attribute:'strength'|'agility'|'wits'|'empathy';skill:string;gear:number;rng?:()=>number}){
    return withTransaction(['app_meta','campaign','characters','world_state','rolls'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const campaign=await tx.objectStore('campaign').get(input.campaignId);
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      const actor=await tx.objectStore('characters').get(input.actorId) as Character|undefined;
      const worldRaw=await tx.objectStore('world_state').get(input.campaignId);
      if(active!==input.campaignId||!campaign||!caster||!actor||!worldRaw||caster.campaignId!==active||actor.campaignId!==active||actor.status==='dead')
        throw Error('Premonição exige pessoa viva pertencente à campanha ativa.');
      if(Array.isArray(campaign.partyCharacterIds)&&(!campaign.partyCharacterIds.includes(actor.id)||!campaign.partyCharacterIds.includes(caster.id)))
        throw Error('Ação de Premonição exige pessoas do grupo real.');
      const world=WorldStateService.normalize(worldRaw,active);
      const effect=BloodmarchBetaDreamDirectiveEngine.effects(world).find(e=>e.id===input.effectId&&e.spellId==='bm-premonition'&&e.casterId===input.casterId);
      if(!effect||!effect.premonitionCharges||effect.pendingPremonition||!(actor.id===caster.id||effect.alliedIds?.includes(actor.id)))
        throw Error('Premonição sem carga ou com ação pendente.');
      if(!input.actionId||input.actionId.length>120||typeof input.description!=='string'||input.description.trim().length<3||input.description.length>300)
        throw Error('Premonição exige uma ação real, identificada de forma única.');
      if(!Object.prototype.hasOwnProperty.call(actor.skills||{},input.skill)||!Number.isInteger(input.gear)||input.gear<0||input.gear>12)
        throw Error('Premonição exige perícia da ficha e equipamento real informado.');
      const pool={base:actor.attributes[input.attribute].current+BloodmarchBetaDreamDirectiveEngine.diceBonus(world,actor.id,'attribute',input.attribute),
        skill:Number(actor.skills[input.skill])+BloodmarchBetaDreamDirectiveEngine.diceBonus(world,actor.id,'skill',input.skill),gear:input.gear};
      const roll=ForbiddenLandsDiceEngine.roll(pool,{characterId:actor.id,sceneId:world.encounter?.startedSceneId||world.encounter?.id||active,
        checkId:input.actionId,attributeName:input.attribute,skillName:input.skill,skillRankAtRoll:actor.skills[input.skill],
        criticalInjuries:actor.criticalInjuryStates,rng:input.rng});
      const changed=BloodmarchBetaDreamDirectiveEngine.reservePremonition(world,{effectId:effect.id,actorId:actor.id,actionId:input.actionId,description:input.description,roll});
      await tx.objectStore('rolls').put({...roll,premonitionLocked:true});await tx.objectStore('world_state').put(changed);
      return {roll:{...roll,premonitionLocked:true},pending:changed.magicDirectiveEffects?.find(e=>e.id===effect.id)?.pendingPremonition};
    });
  }
  static async decideBloodmarchPremonition(input:{campaignId:string;casterId:string;actorId:string;effectId:string;rollId:string;accepted:boolean}){
    return withTransaction(['app_meta','characters','world_state'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      const actor=await tx.objectStore('characters').get(input.actorId) as Character|undefined;
      const raw=await tx.objectStore('world_state').get(input.campaignId);
      if(active!==input.campaignId||!caster||!actor||caster.campaignId!==active||actor.campaignId!==active||!raw)
        throw Error('Decisão de Premonição pertence a outra campanha.');
      const world=WorldStateService.normalize(raw,active);
      if(!BloodmarchBetaDreamDirectiveEngine.effects(world).some(e=>e.id===input.effectId&&e.casterId===caster.id))
        throw Error('Não existe Premonição deste conjurador nesta campanha.');
      const changed=BloodmarchBetaDreamDirectiveEngine.decidePremonition(world,{effectId:input.effectId,actorId:actor.id,rollId:input.rollId,accepted:input.accepted});
      await tx.objectStore('world_state').put(changed);
      return {accepted:input.accepted,world:changed};
    });
  }
  static async fulfillBloodmarchGeas(input:{campaignId:string;effectId:string;targetId:string;confirmed:boolean}){
    return withTransaction(['app_meta','world_state'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      if(active!==input.campaignId)throw Error('Geas de outra campanha bloqueado.');
      const raw=await tx.objectStore('world_state').get(active);if(!raw)throw Error('Mundo ausente.');
      const updated=BloodmarchBetaDreamDirectiveEngine.resolveGeas(WorldStateService.normalize(raw,active),
        {effectId:input.effectId,targetId:input.targetId,fulfillmentConfirmed:input.confirmed});
      await tx.objectStore('world_state').put(updated);return updated;
    });
  }
  /** Pool de perícia modificado por Sono Restaurador, sem adulterar os valores permanentes da ficha. */
  static restorativePool(world:import('../types').WorldState,actor:Character,attribute:'strength'|'agility'|'wits'|'empathy',skill:string){
    if(world.id!==actor.campaignId||!Object.prototype.hasOwnProperty.call(actor.skills,skill))
      throw Error('Perícia e campanha do Sono Restaurador devem estar verificadas.');
    return {base:actor.attributes[attribute].current+BloodmarchBetaDreamDirectiveEngine.diceBonus(world,actor.id,'attribute',attribute),
      skill:actor.skills[skill]+BloodmarchBetaDreamDirectiveEngine.diceBonus(world,actor.id,'skill',skill),gear:0};
  }
  /** Stop the song voluntarily, without a PFV refund. Never wake victims
   * whose one-turn delay has already completed. This action is campaign-scoped.
   */
  /** Movement of Steam Flight: one zone horizontal or one metre vertical in a combat round. */
  static async moveSteamFlight(input:{campaignId:string;casterId:string;effectId:string;direction:'horizontal'|'vertical'}){
    return withTransaction(['app_meta','campaign','characters','world_state'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const campaign=await tx.objectStore('campaign').get(input.campaignId);
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      if(!active||active!==input.campaignId||!campaign||!caster||caster.campaignId!==active||caster.status==='dead')throw Error('Conjuradora fora da campanha ativa.');
      const raw=await tx.objectStore('world_state').get(active);if(!raw)throw Error('Mundo ausente.');
      const world=WorldStateService.normalize(raw,active);
      const spell=BloodmarchBetaActiveWorldEngine.effects(world).find(x=>x.id===input.effectId);
      if(!spell||spell.hexId!==campaign.currentHexId)throw Error('Voo não está mais neste hex.');
      const changed=BloodmarchBetaActiveWorldEngine.flightStep(world,{effectId:input.effectId,casterId:input.casterId,direction:input.direction});
      await tx.objectStore('world_state').put(changed);return changed;
    });
  }
  /** One point Strength from directed steam per living creature per combat round,
   * independently resolved in the same campaign and encounter transaction. */
  static async applySteamContact(input:{campaignId:string;effectId:string;casterId:string;combatantId:string;eventId:string;armLengthConfirmed:boolean;livingConfirmed:boolean}){
    return withTransaction(['app_meta','campaign','characters','world_state'],'readwrite',async tx=>{
      if(input.armLengthConfirmed!==true||input.livingConfirmed!==true||!/^[-\w.:]{8,120}$/.test(input.eventId))
        throw Error('Contato de vapor exige criatura viva, alcance das mãos e evento único confirmados.');
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const campaign=await tx.objectStore('campaign').get(input.campaignId);
      if(!active||active!==input.campaignId||!campaign?.currentHexId)throw Error('Contato de vapor pertence a outra campanha.');
      const raw=await tx.objectStore('world_state').get(active);if(!raw)throw Error('Mundo de vapor ausente.');
      const world=WorldStateService.normalize(raw,active);
      const effect=BloodmarchBetaActiveWorldEngine.effects(world).find(e=>e.id===input.effectId&&e.spellId==='bm-steam-flight'&&e.casterId===input.casterId);
      if(!effect||effect.hexId!==campaign.currentHexId||!effect.remainingRounds)throw Error('Voo a Vapor expirado ou em outro hex.');
      const encounter=world.encounter;
      if(encounter?.status!=='active'||!encounter.combatants.some(c=>c.kind==='character'&&c.entityId===input.casterId&&c.status==='active'))throw Error('Conjuradora ausente do encontro ativo.');
      const matches=encounter.combatants.filter(c=>c.id===input.combatantId&&['active','broken'].includes(c.status));
      if(matches.length!==1)throw Error('Criatura atingida ausente ou ambígua no encontro.');
      const combatant=matches[0], contactKey=`steam:${effect.id}:${combatant.id}:${encounter.round}`;
      if((world.appliedOperationIds||[]).some(id=>id===input.eventId||id===contactKey))throw Error('Dano de vapor duplicado na mesma rodada.');
      if(combatant.kind==='character'){
        const target=await tx.objectStore('characters').get(combatant.entityId) as Character|undefined;
        if(!target||target.campaignId!==active)throw Error('Personagem atingido pertence a outra campanha.');
        const before=Math.max(0,Number(target.attributes.strength.current)||0),after=Math.max(0,before-1);
        await tx.objectStore('characters').put({...target,attributes:{...target.attributes,strength:{...target.attributes.strength,current:after}},
          ...(before>0&&after===0?{brokenAttribute:'strength' as const}:{})});
      }
      const remaining=Math.max(0,combatant.currentStrength-1),now=Date.now();
      const combatants=encounter.combatants.map(c=>c.id!==combatant.id?c:{...c,currentStrength:remaining,
        status:remaining===0?'broken' as const:c.status,brokenAttribute:remaining===0?'strength' as const:c.brokenAttribute});
      const updated={...world,encounter:{...encounter,combatants,log:[...encounter.log,
        {id:input.eventId,round:encounter.round,combatantId:combatant.id,text:`Voo a Vapor: ${combatant.name} sofreu 1 dano em Força ao alcance das mãos.`,createdAt:now}].slice(-300),updatedAt:now},
        appliedOperationIds:[...(world.appliedOperationIds||[]),input.eventId,contactKey].slice(-2000),updatedAt:now};
      await tx.objectStore('world_state').put(updated);return {damage:1,strengthRemaining:remaining,world:updated};
    });
  }
  /** GM-confirmed dream observation: no new PFV, no access to gmNotes or invisible secrets. */
  static async recordDreamVisitObservation(input:{campaignId:string;casterId:string;effectId:string;text:string;gmConfirmed:boolean;casterStillSleeping:boolean;communicated:boolean}){
    return withTransaction(['app_meta','characters','world_state'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      if(active!==input.campaignId||!caster||caster.campaignId!==active||caster.status==='dead')throw Error('Conjuradora fora da campanha ativa.');
      const raw=await tx.objectStore('world_state').get(active);if(!raw)throw Error('Mundo da Visita Onírica ausente.');
      const world=WorldStateService.normalize(raw,active);
      const updated=BloodmarchBetaActiveWorldEngine.recordDreamObservation(world,input);
      await tx.objectStore('world_state').put(updated);return updated;
    });
  }
  /** Explicit passage through a wall: one actual creature, one unique event ID,
   * character and encounter snapshots are committed with the world in ONE transaction. */
  static async crossBloodmarchFirewall(input:{campaignId:string;effectId:string;combatantId:string;eventId:string;crossingConfirmed:boolean}){
    return withTransaction(['app_meta','campaign','characters','world_state'],'readwrite',async tx=>{
      if(input.crossingConfirmed!==true||!/^[-\w.:]{8,120}$/.test(input.eventId))throw Error('Travessia deve ser confirmada com identificador único.');
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const campaign=await tx.objectStore('campaign').get(input.campaignId);
      if(active!==input.campaignId||!campaign?.currentHexId)throw Error('Muralha pertence a outra campanha.');
      const raw=await tx.objectStore('world_state').get(active);if(!raw)throw Error('Mundo não encontrado.');
      const world=WorldStateService.normalize(raw,active);
      if((world.appliedOperationIds||[]).includes(input.eventId))throw Error('Travessia já resolvida; dano duplicado bloqueado.');
      const effect=BloodmarchBetaActiveWorldEngine.effects(world).find(x=>x.id===input.effectId);
      if(!effect||effect.hexId!==campaign.currentHexId)throw Error('Muralha não está neste hex.');
      const amount=BloodmarchBetaActiveWorldEngine.wallCrossing(world,{effectId:input.effectId,hexId:campaign.currentHexId,locationId:effect.locationId});
      const encounter=world.encounter;
      if(encounter?.status!=='active')throw Error('Travessia exige criatura presente em encontro ativo.');
      const matches=encounter.combatants.filter(c=>c.id===input.combatantId&&['active','broken'].includes(c.status));
      if(matches.length!==1||!['character','npc'].includes(matches[0].kind))throw Error('Criatura ausente ou ambígua na cena de travessia.');
      const combatant=matches[0];
      if(combatant.kind==='character'){
        const ch=await tx.objectStore('characters').get(combatant.entityId) as Character|undefined;
        if(!ch||ch.campaignId!==active)throw Error('Personagem da travessia não pertence à campanha.');
        const before=Math.max(0,Number(ch.attributes.strength.current)||0),after=Math.max(0,before-amount);
        await tx.objectStore('characters').put({...ch,attributes:{...ch.attributes,strength:{...ch.attributes.strength,current:after}},
          ...(before>0&&after===0?{brokenAttribute:'strength' as const}:{})});
      }
      const after=Math.max(0,combatant.currentStrength-amount);
      const updated=encounter.combatants.map(c=>c.id===combatant.id?{...c,currentStrength:after,
        status:after===0?'broken' as const:c.status,brokenAttribute:after===0?'strength' as const:c.brokenAttribute}:c);
      const next={...world,encounter:{...encounter,combatants:updated,log:[...encounter.log,{id:input.eventId,round:encounter.round,combatantId:combatant.id,text:`Muralha de Fogo: ${combatant.name} sofreu ${amount} dano em Força ao atravessar.`,createdAt:Date.now()}].slice(-300),updatedAt:Date.now()},
        appliedOperationIds:[...(world.appliedOperationIds||[]),input.eventId].slice(-2000),updatedAt:Date.now()};
      await tx.objectStore('world_state').put(next);return {damage:amount,strengthRemaining:after,world:next};
    });
  }
  static async stopLullaby(campaignId:string,casterCharacterId:string):Promise<Character>{
    return withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(!world.encounter||world.encounter.status!=='active')throw new Error('Não há encontro ativo nesta campanha.');
      const caster=world.encounter.combatants.find(c=>c.kind==='character'&&c.entityId===casterCharacterId);
      if(!caster)throw new Error('A conjuradora não está no encontro desta campanha.');
      const characterStore=tx.objectStore('characters');
      const character=await characterStore.get(casterCharacterId) as Character|undefined;
      if(!character||!BloodmarchBetaSleepEngine.chanting(character.statusEffects))
        throw new Error('A conjuradora não está cantando uma Canção de Ninar pendente.');
      const next=await BloodmarchBetaChantService.interrupt(tx,world,casterCharacterId,'canto interrompido voluntariamente');
      if(next===world)throw new Error('O canto não possui alvos pendentes neste encontro.');
      await store.put(next);
      return await characterStore.get(casterCharacterId) as Character;
    });
  }
  /** Persiste gasto de PFV, rolagem e consequência junto à ficha do conjurador. */
  static async castCharacter(input: CastCharacterMagicInput) {
    return withTransaction(['characters','app_meta','world_state','rolls','campaign','map_discovery','individual_hexes'],'readwrite',async tx=>{
      const character=await tx.objectStore('characters').get(input.characterId) as Character|undefined;
      if(!character)throw new Error('Personagem conjurador não encontrado.');
      if(character.status==='dead')throw new Error('Personagem morto não pode lançar magia.');
      if(BloodmarchBetaSleepEngine.asleep(character.statusEffects))throw new Error('Personagem adormecida por magia não pode conjurar.');
      if(BloodmarchBetaSleepEngine.chanting(character.statusEffects))throw new Error('Conclua ou interrompa a Canção de Ninar antes de lançar outra magia; PFV preservados.');
      const spell=ForbiddenLandsMagicCatalogService.get(input.spellId);
      if(!spell)throw new Error('Feitiço não pertence aos catálogos verificados.');
      let officialSoulBinding:{eventId:string;tokenId:string;targetId:string;targetKind:'animal'|'character'}|undefined;
      let officialSoulDispositionD6:1|2|3|4|5|6|undefined;
      const soulContext=input.effectContext||{};
      const hasOfficialSoulContext=Boolean(soulContext.officialSoulTokenId||soulContext.officialSoulEventId||soulContext.officialSoulEncounterId);
      if(hasOfficialSoulContext){
        if(input.spellId!=='resurrection'||Number(soulContext.officialSoulEncounterId)!==41)
          throw new Error('Alma de garrafa R88 #41 exige o feitiço Ressurreição e o encontro #41; PFV preservados.');
        if(!normalize(character.profession).includes('druid')&&!normalize(character.profession).includes('druida'))
          throw new Error('A R88 #41 exige um Druida para lançar Ressurreição sobre a alma da garrafa; PFV preservados.');
        const eventId=String(soulContext.officialSoulEventId||'');const tokenId=String(soulContext.officialSoulTokenId||'');
        const targetId=String(input.effectTargetCharacterId||'');const targetKind=input.effectTargetKind;
        if(!eventId||!tokenId||!targetId||!['animal','character'].includes(String(targetKind)))
          throw new Error('Selecione alma livre, criatura viva e tipo de destino (animal ou personagem); PFV preservados.');
        const activeId=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
        if(!activeId||character.campaignId&&character.campaignId!==activeId)
          throw new Error('A alma, a conjuradora e o destino precisam pertencer à campanha ativa; PFV preservados.');
        const world=WorldStateService.normalize(await tx.objectStore('world_state').get(activeId),activeId);
        const event=world.journey.events.find(item=>item.id===eventId&&Number((item.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===41);
        const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
        const soul=(decision?.releasedSoulTokens as Array<Record<string,unknown>>|undefined)?.find(item=>item.id===tokenId&&item.state==='released'&&item.resurrectionPending===true);
        if(!soul)throw new Error('A alma escolhida não está livre ou já foi vinculada; PFV preservados.');
        const target=await tx.objectStore('characters').get(targetId) as Character|undefined;
        if(!target||target.status==='dead'||target.campaignId&&target.campaignId!==activeId)
          throw new Error('A Ressurreição R88 #41 exige destino vivo na mesma campanha; PFV preservados.');
        const collection=String(soulContext.officialSoulTargetItemCollection||'');
        const index=Number(soulContext.officialSoulTargetItemIndex);
        if(!['gear','weapons','armor'].includes(collection)||!Number.isInteger(index)||index<0)
          throw new Error('Escolha um item pessoal real do destino como ingrediente da Ressurreição; PFV preservados.');
        const targetItem=(target[collection as 'gear'|'weapons'|'armor']||[])[index];
        if(!targetItem||JSON.stringify(targetItem)!==String(soulContext.officialSoulTargetItemFingerprint||''))
          throw new Error('O item pessoal do destino mudou ou não pertence a ele; PFV preservados.');
        if(targetKind==='animal'&&!/(^| )(animal|cavalo|horse|lobo|wolf|cao|dog|gato|cat|raposa|fox|urso|bear|javali|boar|veado|deer|corvo|raven|coruja|owl|cabrito|goat|ovelha|sheep|alce|elk|aguia|eagle)($| )/.test(normalize(target.kin)))
          throw new Error('O destino marcado como animal precisa ter uma espécie animal identificada na ficha; PFV preservados.');
        if(targetKind==='character'&&/(^| )(animal|cavalo|horse|lobo|wolf|cao|dog|gato|cat|raposa|fox|urso|bear|javali|boar|veado|deer|corvo|raven|coruja|owl|cabrito|goat|ovelha|sheep|alce|elk|aguia|eagle)($| )/.test(normalize(target.kin)))
          throw new Error('Escolha Animal para um destino animal; PFV preservados.');
        officialSoulBinding={eventId,tokenId,targetId,targetKind:targetKind as 'animal'|'character'};
      }
      const betaSpell=isBloodmarchBetaSpell(spell.id)?BloodmarchBetaModule.getSpell(spell.id):undefined;
      if(input.spellId==='bm-fires-friend'&&input.effectTargetCharacterId&&input.effectTargetCharacterId!==character.id)
        throw new Error('Amigo do Fogo sempre inclui a conjuradora; escolha outras pessoas no campo de proteção adicional. PFV preservados.');
      if(input.spellId!=='bm-fires-friend'&&input.fireFriendAllyCharacterIds?.length)
        throw new Error('A lista de pessoas adicionais só pode ser usada com Amigo do Fogo.');
      if(input.spellId==='bm-mental-strength'&&input.effectTargetCharacterId)throw new Error('Força Mental usa apenas a seleção explícita de aliados; não há alvo implícito.');
      if(input.spellId!=='bm-mental-strength'&&input.mentalStrengthTargetCharacterIds?.length)throw new Error('Seleção de Força Mental usada em outra magia.');
      if(input.spellId==='bm-traceless'&&input.effectTargetCharacterId&&input.effectTargetCharacterId!==character.id)
        throw new Error('Sem Rastros sempre inclui a conjuradora; use a lista de destinatários adicionais.');
      if(input.spellId!=='bm-traceless'&&input.tracelessRecipientIds?.length)
        throw new Error('Destinatários de Sem Rastros não podem ser usados com outra magia.');
      if(input.spellId!=='bm-amnesia'&&input.amnesiaAdditionalTargetIds?.length)
        throw new Error('Destinatários adicionais de Amnésia só podem ser usados com Amnésia.');
      if(input.spellId!=='bm-nightmares'&&input.nightmaresAdditionalTargetIds?.length)throw new Error('Alvos adicionais de Pesadelos pertencem apenas a essa magia.');
      if(betaSpell&&!betaSpell.executable)throw new Error('Magia Bloodmarch beta ainda sem executor mecânico; PFV não serão gastos.');
      // Forma Falsa: no narrative-only ingredient. Verify a recorded LIVE
      // pupa on the caster's sheet before rolling; spend exactly one unit on
      // commit, along with the PFV and effect status.
      const pupaIndex=input.spellId==='bm-false-shape'
        ?(character.gear||[]).findIndex(item=>['pupa viva','crisálida viva','crisalida viva','live pupa'].includes(normalize((item as any)?.name))
          &&Number((item as any)?.quantity??1)>=1):-1;
      // Sono de Areia p.36 requires a handful of actual fine sand, thrown
      // around the caster. The item is consumed only when the cast commits.
      const sandIndex=input.spellId==='bm-sand-sleep'
        ?(character.gear||[]).findIndex(item=>['areia fina','punhado de areia fina','fine sand','handful of fine sand'].includes(normalize((item as any)?.name))
          &&Number((item as any)?.quantity??1)>=1):-1;
      if(input.spellId==='bm-sand-sleep'&&sandIndex<0)
        throw new Error('Sono de Areia exige um punhado de areia fina no inventário; PFV preservados.');
      if(input.spellId==='bm-lullaby'&&(character.statusEffects||[]).some(e=>e.speechBlocked))
        throw new Error('Canção de Ninar exige que a conjuradora possa cantar; PFV preservados.');
      if(input.spellId==='bm-false-shape'){
        if(input.effectTargetCharacterId&&input.effectTargetCharacterId!==character.id)
          throw new Error('Forma Falsa só pode afetar a própria conjuradora.');
        if(pupaIndex<0)throw new Error('Registre uma pupa viva no inventário da conjuradora antes de lançar Forma Falsa; PFV preservados.');
        const ctx=input.effectContext||{};
        const size=Number(ctx.illusionSizeRatio);
        if(typeof ctx.illusionAppearance!=='string'||ctx.illusionAppearance.trim().length<3||ctx.illusionAppearance.trim().length>320
            ||!Number.isFinite(size)||size<0.5||size>2||ctx.targetLivingCreature!==true)
          throw new Error('Forma Falsa exige criatura viva, descrição e tamanho aparente entre 0,5× e 2×; PFV preservados.');
      }

      if(spell.discipline==='Afinidade com o Gelo'&&input.ingredient&&!['ice','snow'].includes(String(input.effectContext?.availableIngredient||'')))throw new Error('Para receber +1 poder, é necessário gelo ou neve disponível.');
      if(spell.mandatoryIngredient&&!(betaSpell?spell.id==='bm-ignite'&&input.effectContext?.availableElement==='fire':verifyElementalIngredient(spell.id,input.effectContext?.availableElement)))throw new Error(`Ingrediente obrigatório não disponível: ${spell.ingredientElement}. A magia não pode criar elementos do nada.`);
      if(Math.trunc(Number(input.spellRank))!==spell.rank)throw new Error('Categoria do feitiço diverge do Manual do Jogador.');
      if(Boolean(input.ritual)!==spell.ritual)throw new Error(spell.ritual?'Este feitiço exige procedimento ritual.':'Este feitiço não é um ritual.');
      const talentKey=Object.keys(character.talents||{}).find(key=>normalize(key)===normalize(input.talentName));
      if(!MagicTalentAccessService.isCompatible(talentKey||input.talentName,spell))throw new Error(`Talento incompatível com a magia. ${MagicTalentAccessService.description(spell)}.`);
      const learnedRank=betaSpell?Number(character.bloodmarchBetaTraditions?.[betaSpell.discipline]||0):0;
      if(betaSpell&&(!Number.isInteger(learnedRank)||learnedRank<1||learnedRank>3))throw new Error('A tradição Bloodmarch beta deve ser aprendida com mestre e registrada na ficha antes da conjuração.');
      const talentRank=Math.max(0,Math.min(3,Math.trunc(Number(betaSpell?learnedRank:talentKey?character.talents[talentKey]:0)||0)));
      const rng=input.rng||Math.random;
      const result=ForbiddenLandsMagicRulesEngine.cast({
        spellRank:input.spellRank,talentRank,willpowerAvailable:WillpowerRulesEngine.normalize(character.willpowerPoints),willpowerSpent:input.willpowerSpent,
        ancestryWillpowerBonus:normalize(character.kin).includes('meio elfo')||normalize(character.kin).includes('half elf')?1:0,
        safe:input.safe,ingredient:spell.mandatoryIngredient?false:input.ingredient,obscure:input.obscure,bindingWillpower:input.bindingWillpower,ritual:input.ritual,rng,
      });
      if(!result.eligible)throw new Error(talentRank===0?'A personagem não possui o talento mágico informado.':'Categoria, PFV disponível ou custos adicionais não permitem esse lançamento.');

      let effectResult:SpellEffectResult|undefined;
      let effectTarget:Character|undefined;
      let effectCombatant:any|undefined;
      let activeEncounter:any|undefined;
      let worldStateRecord:any|undefined;
      let materialPlan:MaterialOperation|undefined;
      let materialWorld:any|undefined;
      let mentalPlan:MentalPlan|undefined;
      let mentalWorld:any|undefined;
      let activeWorldPlan:ActiveWorldPlan|undefined;
      let activeSpellWorld:any|undefined;
      let directivePlan:DirectivePlan|undefined;
      let directiveWorld:any|undefined;
      let propheticPlan:PropheticPlan|undefined;
      let propheticWorld:any|undefined;
      let dreamTravelPlan:DreamTravelPlan|undefined;
      let dreamTravelWorld:any|undefined;
      let fireWyrmPlan:FireWyrmPlan|undefined;
      let fireWyrmWorld:any|undefined;
      let maraPlan:MaraPlan|undefined;
      let maraWorld:any|undefined;

      // R45: campaign isolation. A character ID can appear in restored/duplicated
      // worlds. Never choose the first encounter returned by IndexedDB.
      const worlds = await tx.objectStore('world_state').getAll();
      const configuredCampaignId = String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value || '');
      if (character.campaignId && configuredCampaignId && character.campaignId!==configuredCampaignId)
        throw new Error('Conjuradora pertence a outra campanha ativa; PFV preservados.');
      const eligibleWorlds = worlds.filter((raw:any) => {
        const world = WorldStateService.normalize(raw, raw.id);
        return world.encounter?.status === 'active' &&
          world.encounter.combatants.some((item:any)=>item.kind==='character' && item.entityId===character.id && ['active','broken'].includes(item.status));
      });
      const selectedWorld = configuredCampaignId
        ? eligibleWorlds.find((raw:any)=>raw.id===configuredCampaignId)
        : eligibleWorlds.length===1 ? eligibleWorlds[0] : undefined;
      if (!configuredCampaignId && eligibleWorlds.length>1)
        throw new Error('Personagem em múltiplas campanhas ativas: escolha a campanha antes de conjurar. PFV preservados.');
      // No generic narrative-only world mutation: the caster MUST stand in the
      // current hex and name an existing location. Validate after final Power
      // Level is determined, while still inside the PFV/roll/world transaction.
      if(BloodmarchBetaMaterialWorldEngine.isMaterialSpell(input.spellId)){
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const rawWorld=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!rawWorld||!character.campaignId||character.campaignId!==campaign.id)
          throw Error('Magia de matéria exige campanha ativa própria e estado de mundo persistido; PFV preservados.');
        if(Array.isArray(campaign.partyCharacterIds)&&campaign.partyCharacterIds.length&&!campaign.partyCharacterIds.includes(character.id))
          throw Error('Conjurador não pertence ao grupo desta campanha; PFV preservados.');
        materialWorld=WorldStateService.normalize(rawWorld,campaign.id);
        materialPlan=BloodmarchBetaMaterialWorldEngine.plan({spellId:input.spellId,power:result.power,
          world:materialWorld,campaign,context:input.effectContext||{}});
        if(input.effectTargetCharacterId)throw Error('Magia de matéria afeta objeto do local, não ficha de personagem.');
      }
      if(BloodmarchBetaMentalSceneEngine.isMentalSpell(input.spellId)){
        if(input.effectTargetCharacterId)throw Error('Magia mental de cena usa destinatários selecionados na própria cena, não o alvo genérico.');
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const rawWorld=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!rawWorld||character.campaignId!==campaign.id)
          throw Error('Magia mental exige campanha própria persistida; PFV preservados.');
        if(Array.isArray(campaign.partyCharacterIds)&&campaign.partyCharacterIds.length&&!campaign.partyCharacterIds.includes(character.id))
          throw Error('Conjuradora não pertence ao grupo da campanha; PFV preservados.');
        mentalWorld=WorldStateService.normalize(rawWorld,campaign.id);
        mentalPlan=BloodmarchBetaMentalSceneEngine.plan({spellId:input.spellId,casterId:character.id,power:result.power,world:mentalWorld,campaign,context:input.effectContext||{}});
      }
      if(BloodmarchBetaActiveWorldEngine.isSpell(input.spellId)){
        if(input.effectTargetCharacterId)throw Error('Magia de ambiente/dream usa local validado, não alvo genérico.');
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const rawWorld=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!rawWorld||character.campaignId!==campaign.id)
          throw Error('Magia Bloodmarch exige campanha própria persistida; PFV preservados.');
        if(Array.isArray(campaign.partyCharacterIds)&&campaign.partyCharacterIds.length&&!campaign.partyCharacterIds.includes(character.id))
          throw Error('Conjuradora não pertence ao grupo ativo; PFV preservados.');
        activeSpellWorld=WorldStateService.normalize(rawWorld,campaign.id);
        activeWorldPlan=BloodmarchBetaActiveWorldEngine.plan({spellId:input.spellId,casterId:character.id,power:result.power,
          world:activeSpellWorld,campaign,context:input.effectContext||{}});
      }
      if(BloodmarchBetaDreamDirectiveEngine.isSpell(input.spellId)){
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const raw=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!raw||character.campaignId!==campaign.id)
          throw Error('Diretiva Bloodmarch exige campanha própria ativa; PFV preservados.');
        directiveWorld=WorldStateService.normalize(raw,campaign.id);
        const ctx=input.effectContext||{};
        let targetId:string|undefined,targetCombatantId:string|undefined,resistanceRoll:ReturnType<typeof ForbiddenLandsDiceEngine.roll>|undefined;
        if(input.spellId==='bm-geas'){
          const id=String(input.effectTargetCharacterId||'');
          const encounter=directiveWorld.encounter;
          if(!id||encounter?.status!=='active'||!encounter.combatants.some((c:any)=>c.kind==='character'&&c.entityId===character.id&&['active','broken'].includes(c.status)))
            throw Error('Geas exige encontro ativo com conjuradora e pessoa identificada.');
          const candidates=encounter.combatants.filter((c:any)=>(c.id===id||c.entityId===id)&&['npc','character'].includes(c.kind)&&['active','broken'].includes(c.status));
          if(candidates.length!==1||candidates[0].entityId===character.id)throw Error('Destinatário de Geas ausente, ambíguo ou próprio conjurador.');
          const victim=candidates[0];targetId=victim.entityId||victim.id;targetCombatantId=victim.id;
          const actor=victim.kind==='character'?await tx.objectStore('characters').get(victim.entityId):victim;
          if(!actor||actor.status==='dead'||(victim.kind==='character'&&actor.campaignId!==configuredCampaignId))
            throw Error('Pessoa destinatária não pertence à campanha.');
          const ownedItem=String(ctx.geasPersonalItemName||'').trim();
          if(!ownedItem||!(character.gear||[]).some(item=>normalize(item.name)===normalize(ownedItem)&&Number((item as any).quantity??1)>0)
             ||ctx.personalItemBelongsToTargetConfirmed!==true)
            throw Error('Geas exige item real da vítima no inventário da conjuradora.');
          // Resistência oficial: Astúcia + Discernimento, modificador −poder, não forçável.
          const wits=BloodmarchBetaResistanceGuard.wits(actor),insight=BloodmarchBetaResistanceGuard.insight(actor);
          resistanceRoll=ForbiddenLandsDiceEngine.roll({base:wits,skill:insight,gear:0,modifier:-result.power},
            {sceneId:encounter.startedSceneId||encounter.id,checkId:`bm-geas:${character.id}:${targetId}`,characterId:victim.kind==='character'?targetId:null,skillName:'insight',attributeName:'wits',criticalInjuries:actor.criticalInjuryStates||actor.criticalInjuries,rng});
        }else if(input.effectTargetCharacterId&&input.effectTargetCharacterId!==character.id)
          throw Error('Premonição e Sono Restaurador são efeitos pessoais da conjuradora.');
        directivePlan=BloodmarchBetaDreamDirectiveEngine.plan({spellId:input.spellId,caster:character,power:result.power,
          campaign,world:directiveWorld,context:ctx,geasResistanceRoll:resistanceRoll,geasTargetId:targetId,geasTargetCombatantId:targetCombatantId});
      }
      if(input.spellId==='bm-summon-fire-wyrm'){
        if(input.effectTargetCharacterId)throw Error('Serpe invocada não permite escolher alvo para atacar instantaneamente.');
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const raw=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!raw||character.campaignId!==campaign.id)
          throw Error('Serpe exige campanha e mundo persistidos próprios.');
        const hexEvidence=await tx.objectStore('individual_hexes').get(campaign.currentHexId);
        fireWyrmWorld=WorldStateService.normalize(raw,campaign.id);
        fireWyrmPlan=BloodmarchBetaFireWyrmEngine.plan({caster:character,campaign,world:fireWyrmWorld,
          power:result.power,context:input.effectContext||{},hexEvidence});
      }
      if(input.spellId==='bm-mara'){
        if(input.effectTargetCharacterId)throw Error('Mara usa vítima do registro de sonho, não um alvo de ataque instantâneo.');
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const raw=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!raw||character.campaignId!==campaign.id)
          throw Error('Mara exige campanha própria e mundo persistido.');
        const victimId=String(input.effectContext?.maraVictimId||'');
        const victim=await tx.objectStore('characters').get(victimId) as Character|undefined;
        if(!victim)throw Error('Vítima de Mara não consta das fichas desta campanha.');
        maraWorld=WorldStateService.normalize(raw,campaign.id);
        maraPlan=BloodmarchBetaMaraEngine.plan({caster:character,victim,campaign,world:maraWorld,
          power:result.power,context:input.effectContext||{}});
      }
      if(input.spellId==='bm-dream-travel'){
        if(input.effectTargetCharacterId)throw Error('Viagem dos Sonhos não usa alvo de combate.');
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const raw=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!raw||character.campaignId!==campaign.id)
          throw Error('Viagem dos Sonhos requer campanha ativa da conjuradora.');
        const hexId=String(input.effectContext?.destinationHexId||'');
        const hex=await tx.objectStore('individual_hexes').get(hexId);
        if(!hex)throw Error('Destino não existe no mapa persistido.');
        const discovery=await tx.objectStore('map_discovery').get(`${campaign.id}:${hex.mapId}`);
        const ids=campaign.partyCharacterIds||[];
        const members=await Promise.all(ids.map((id:string)=>tx.objectStore('characters').get(id)));
        dreamTravelWorld=WorldStateService.normalize(raw,campaign.id);
        dreamTravelPlan=BloodmarchBetaDreamTravelEngine.plan({caster:character,members,campaign,world:dreamTravelWorld,
          destinationHex:hex,discovery,power:result.power,context:input.effectContext||{}});
      }
      if(input.spellId==='bm-prophetic-dreams'){
        if(input.effectTargetCharacterId)throw Error('Sonhos Proféticos usa destinatário do próprio grupo, não alvo de combate.');
        const campaign=await tx.objectStore('campaign').get(configuredCampaignId);
        const raw=await tx.objectStore('world_state').get(configuredCampaignId);
        if(!configuredCampaignId||!campaign||!raw||character.campaignId!==campaign.id)
          throw Error('Sonhos Proféticos exige campanha e mundo próprios; PFV preservados.');
        const requested=String(input.effectContext?.propheticTargetId||character.id);
        const target=await tx.objectStore('characters').get(requested) as Character|undefined;
        if(!target)throw Error('Destinatário de Sonhos Proféticos não consta na campanha.');
        propheticWorld=WorldStateService.normalize(raw,campaign.id);
        propheticPlan=BloodmarchBetaPropheticDreamsEngine.plan({caster:character,target,campaign,world:propheticWorld,
          power:result.power,context:input.effectContext||{}});
        if(BloodmarchBetaPropheticDreamsEngine.effects(propheticWorld).some(e=>e.status==='pending'&&e.casterId===character.id))
          throw Error('Conjuradora já tem um sonho profético aguardando um Quarto de Dia de sono.');
      }
      if (selectedWorld) {
        worldStateRecord=selectedWorld;
        activeEncounter=WorldStateService.normalize(selectedWorld,selectedWorld.id).encounter;
        if (input.effectTargetCharacterId)
          effectCombatant=activeEncounter.combatants.find((item:any)=>item.id===input.effectTargetCharacterId||item.entityId===input.effectTargetCharacterId);
      }
      if(activeEncounter&&input.effectTargetCharacterId&&input.effectTargetCharacterId!==character.id&&
          !activeEncounter.combatants.some((item:any)=>
            (item.id===input.effectTargetCharacterId||item.entityId===input.effectTargetCharacterId)&&
            ['active','broken'].includes(item.status)))
        throw new Error('Alvo não está presente no encontro da campanha ativa. PFV preservados.');
      // If a campaign is selected but the caster is in a DIFFERENT campaign's
      // encounter, never take targets from that other encounter.
      if(configuredCampaignId && !selectedWorld && eligibleWorlds.length &&
          eligibleWorlds.every((raw:any)=>raw.id!==configuredCampaignId) &&
          (input.areaTargetIds?.length || input.tracelessRecipientIds?.length ||
           input.fireFriendAllyCharacterIds?.length || input.mentalStrengthTargetCharacterIds?.length || input.amnesiaAdditionalTargetIds?.length || input.nightmaresAdditionalTargetIds?.length))
        throw new Error('O encontro do conjurador pertence a outra campanha. PFV preservados.');

      // R53: one living intelligent person per Power Level, including the
      // primary target. All EXTRA targets are validated against the currently
      // active encounter BEFORE any roll, with atomic application after all
      // targets have passed preflight. The previous turn alone is forgotten.
      const amnesiaRecipients:Array<{combatant:any;character?:Character}>=[];
      if(input.spellId==='bm-amnesia'){
        const ids=input.amnesiaAdditionalTargetIds??[];
        if(!Array.isArray(ids)||ids.some(id=>typeof id!=='string'||!id.trim())||new Set(ids).size!==ids.length)
          throw new Error('Amnésia: destinatários adicionais inválidos ou repetidos; PFV preservados.');
        if(!input.effectTargetCharacterId)throw new Error('Amnésia exige um alvo principal identificado; PFV preservados.');
        if(ids.length+1>result.power)throw new Error(`Amnésia permite até ${result.power} pessoa(s) pelo poder final; PFV preservados.`);
        if(ids.length){
          if(!activeEncounter||!worldStateRecord)throw new Error('Amnésia em grupo exige encontro ativo para validar todos os destinatários.');
          const primary=activeEncounter.combatants.filter((c:any)=>(c.id===input.effectTargetCharacterId||c.entityId===input.effectTargetCharacterId)&&['active','broken'].includes(c.status));
          if(primary.length!==1)throw new Error('Amnésia: alvo principal ausente ou ambíguo.');
          const selected=new Set([primary[0].id]);
          for(const id of ids){
            const matching=activeEncounter.combatants.filter((c:any)=>(c.id===id||c.entityId===id)&&['active','broken'].includes(c.status));
            if(matching.length!==1||selected.has(matching[0].id))throw new Error('Amnésia: destinatário adicional ausente, duplicado ou ambíguo.');
            selected.add(matching[0].id);
            const c=matching[0];
            if(!['character','npc'].includes(c.kind))throw new Error('Amnésia só afeta pessoas inteligentes, não monstros.');
            const actor=c.kind==='character'?await tx.objectStore('characters').get(c.entityId) as Character|undefined:undefined;
            if(c.kind==='character'&&(!actor||actor.status==='dead'||actor.campaignId&&configuredCampaignId&&actor.campaignId!==configuredCampaignId))
              throw new Error('Amnésia: ficha adicional ausente, morta ou de outra campanha.');
            if(c.status==='dead')throw new Error('Amnésia não afeta pessoas mortas.');
            BloodmarchBetaResistanceGuard.wits(actor||c);
            BloodmarchBetaResistanceGuard.insight(actor||c);
            amnesiaRecipients.push({combatant:c,character:actor});
          }
        }
      }

      // R54: Nightmares — validate ALL selected sleeping creatures before dice,
      // willpower or injury rolls. No invented attributes or foreign encounter.
      const nightmareRecipients:Array<{combatant:any;character?:Character}>=[];
      if(input.spellId==='bm-nightmares'){
        const ids=input.nightmaresAdditionalTargetIds??[];
        if(!Array.isArray(ids)||ids.some(id=>typeof id!=='string'||!id.trim())||new Set(ids).size!==ids.length)
          throw new Error('Pesadelos: destinatários adicionais inválidos ou repetidos; PFV preservados.');
        if(!input.effectTargetCharacterId||!worldStateRecord||!activeEncounter)
          throw new Error('Pesadelos exige alvo principal dormindo, presente em encontro da campanha atual.');
        if(input.effectContext?.sleepConfirmed!==true)
          throw new Error('Pesadelos exige confirmação da condição de sono dos destinatários.');
        const maxTargets=1+Math.floor((result.power-1)/2);
        if(ids.length+1>maxTargets)
          throw new Error(`Pesadelos permite até ${maxTargets} vítima(s) pelo poder final; PFV preservados.`);
        const selected=new Set<string>();
        for(const id of [input.effectTargetCharacterId,...ids]){
          const matching=activeEncounter.combatants.filter((c:any)=>(c.id===id||c.entityId===id)&&['active','broken'].includes(c.status));
          if(matching.length!==1||selected.has(matching[0].id))throw new Error('Pesadelos: vítima ausente, repetida ou ambígua.');
          const c=matching[0];selected.add(c.id);
          if(!['character','npc','animal'].includes(c.kind)||c.status==='dead'||c.isMonster===true)
            throw new Error('Pesadelos não afeta monstros, mortos ou objetos.');
          const actor=c.kind==='character'?await tx.objectStore('characters').get(c.entityId) as Character|undefined:undefined;
          if(c.kind==='character'&&(!actor||actor.status==='dead'||actor.campaignId&&configuredCampaignId&&actor.campaignId!==configuredCampaignId))
            throw new Error('Pesadelos: ficha ausente, morta ou de outra campanha.');
          BloodmarchBetaResistanceGuard.wits(actor||c);
          nightmareRecipients.push({combatant:c,character:actor});
        }
      }

      // R46: area sleep preflight BEFORE committing PFV. Every active creature
      // must be explicitly classified, not merely selected from a subset.
      const sleepSpell=['bm-lullaby','bm-sand-sleep'].includes(input.spellId);
      const sleepTargetCombatants:any[]=[];
      const sleepActors=new Map<string,Character|any>();
      if(sleepSpell){
        if(!activeEncounter||!worldStateRecord)throw new Error('Magia de sono exige encontro ativo na campanha da conjuradora.');
        if(input.sleepRosterConfirmed!==true)throw new Error('Confirme a classificação de TODOS os presentes antes de lançar sono.');
        const targetIds=BloodmarchBetaSleepEngine.targets(input.spellId as SleepSpellId,
          activeEncounter.combatants,input.sleepClassification||{},character.id);
        if(!targetIds.length)throw new Error('Nenhuma criatura elegível dentro do alcance da magia de sono; PFV preservados.');
        for(const id of targetIds){
          const matching=activeEncounter.combatants.filter((item:any)=>item.id===id&&['active','broken'].includes(item.status));
          if(matching.length!==1)throw new Error('Alvo de sono não é único no encontro.');
          const item=matching[0];
          const actor=item.kind==='character'&&item.entityId
            ?await tx.objectStore('characters').get(item.entityId) as Character|undefined:item;
          if(!actor||actor.status==='dead')throw new Error('Ficha do alvo de sono ausente ou morta.');
          // Never invent Wits for NPCs. Preflight ALL targets before any roll.
          BloodmarchBetaResistanceGuard.wits(actor);
          BloodmarchBetaResistanceGuard.insight(actor);
          sleepTargetCombatants.push(item);sleepActors.set(item.id,actor);
        }
      }

      // Bloodmarch beta p.29: caster + up to one additional person per FINAL Power Level.
      // Vicinity is confirmed by the player AND each recipient must be in the
      // current active encounter with the caster. No invented off-map distance.
      // Validation happens before mutation or PFV persistence in this transaction.
      const fireFriendRecipients: Character[]=[];
      if(input.spellId==='bm-fires-friend'){
        const ids=input.fireFriendAllyCharacterIds??[];
        if(!Array.isArray(ids)||ids.some(id=>typeof id!=='string'||!id.trim())||new Set(ids).size!==ids.length)
          throw new Error('Amigo do Fogo: seleção de pessoas adicionais inválida ou repetida. PFV preservados.');
        if(ids.length>result.power)throw new Error(`Amigo do Fogo permite no máximo ${result.power} pessoa(s) adicional(is) pelo poder final. PFV preservados.`);
        if(ids.includes(character.id))throw new Error('A conjuradora já está protegida; não a selecione novamente.');
        if(ids.length){
          if(input.fireFriendVicinityConfirmed!==true)throw new Error('Confirme que todas as pessoas escolhidas estão próximas; PFV preservados.');
          if(!activeEncounter||!activeEncounter.combatants.some((item:any)=>item.kind==='character'&&item.entityId===character.id))
            throw new Error('Proteção de outras pessoas exige um encontro ativo com conjuradora e destinatários identificados.');
          const store=tx.objectStore('characters');
          for(const id of ids){
            const members=activeEncounter.combatants.filter((item:any)=>item.kind==='character'&&item.entityId===id&&['active','broken'].includes(item.status));
            if(members.length!==1)throw new Error(`Pessoa ${id} não está identificada no encontro atual. PFV preservados.`);
            const ally=await store.get(id) as Character|undefined;
            if(!ally||ally.status==='dead')throw new Error(`Destinatário ${id} inexistente ou morto; PFV preservados.`);
            fireFriendRecipients.push(ally);
          }
        }
      }

      // Mental Strength (p.31) is a *chosen* allied group, not an automatic
      // caster buff. Fail closed if distance/side cannot be verified; all
      // recipients are validated before any PFV is committed.
      const mentalRecipients:Character[]=[];
      if(input.spellId==='bm-mental-strength'){
        const ids=input.mentalStrengthTargetCharacterIds;
        if(!Array.isArray(ids)||!ids.length||ids.some(id=>typeof id!=='string'||!id.trim())||new Set(ids).size!==ids.length)
          throw new Error('Força Mental exige 1 ou mais aliados únicos selecionados; PFV preservados.');
        if(ids.length>result.power)throw new Error(`Força Mental protege até ${result.power} aliado(s) pelo poder FINAL; PFV preservados.`);
        if(input.mentalStrengthVicinityConfirmed!==true)throw new Error('Força Mental exige confirmar alcance Perto para todos os aliados.');
        if(!activeEncounter||!worldStateRecord)throw new Error('Força Mental exige encontro ativo para verificar posição e lado dos aliados.');
        const caster=activeEncounter.combatants.find((c:any)=>c.kind==='character'&&c.entityId===character.id&&['active','broken'].includes(c.status));
        if(!caster)throw new Error('Conjuradora não está no encontro ativo.');
        for(const id of ids){
          const matching=activeEncounter.combatants.filter((c:any)=>c.kind==='character'&&c.entityId===id&&['active','broken'].includes(c.status));
          if(matching.length!==1||matching[0].side!==caster.side)throw new Error(`Alvo ${id} não é aliado único e ativo no encontro.`);
          const ally=await tx.objectStore('characters').get(id) as Character|undefined;
          if(!ally||ally.status==='dead')throw new Error(`Alvo ${id} ausente ou morto; PFV preservados.`);
          mentalRecipients.push(ally);
        }
      }

      // Sem Rastros: the caster is implicit; each additional creature must
      // be uniquely present with the caster. Do not invent off-screen NPCs,
      // distances or apply effects before the full group has been validated.
      const tracelessRecipients:Array<{character?:Character;combatant?:any}>=[];
      if(input.spellId==='bm-traceless'){
        const ids=input.tracelessRecipientIds??[];
        if(!Array.isArray(ids)||ids.some(id=>typeof id!=='string'||!id.trim())||new Set(ids).size!==ids.length)
          throw new Error('Sem Rastros exige destinatários únicos e identificados; PFV preservados.');
        if(ids.length>result.power)throw new Error(`Sem Rastros permite no máximo ${result.power} pessoa(s) ou criatura(s) adicional(is).`);
        if(ids.includes(character.id))throw new Error('A conjuradora já recebe Sem Rastros automaticamente.');
        if(ids.length){
          if(input.tracelessVicinityConfirmed!==true)throw new Error('Confirme a proximidade de todas as criaturas escolhidas.');
          if(!activeEncounter||!activeEncounter.combatants.some((item:any)=>item.kind==='character'&&item.entityId===character.id&&['active','broken'].includes(item.status)))
            throw new Error('Sem Rastros em grupo exige encontro ativo com conjuradora e criaturas presentes.');
          const seen=new Set<string>();
          for(const id of ids){
            const matching=activeEncounter.combatants.filter((item:any)=>(item.id===id||item.entityId===id)&&['active','broken'].includes(item.status));
            if(matching.length!==1)throw new Error(`Destinatário de Sem Rastros ausente ou ambíguo: ${id}`);
            const combatant=matching[0];
            if(combatant.entityId===character.id||seen.has(combatant.id))throw new Error('Destinatário de Sem Rastros duplicado.');
            seen.add(combatant.id);
            if(combatant.kind==='character'){
              const ally=await tx.objectStore('characters').get(combatant.entityId) as Character|undefined;
              if(!ally||ally.status==='dead')throw new Error(`Ficha do destinatário não encontrada: ${id}`);
              tracelessRecipients.push({character:ally});
            }else tracelessRecipients.push({combatant});
          }
        }
      }

      // Bloodmarch beta p.30: safe mechanical subset, no thrown-stone
      // deviation and no unmodelled scene object ignition. Checks occur
      // INSIDE the same transaction, BEFORE any PFV is persisted.
      if(input.spellId==='bm-magma-cascade'){
        if(input.effectContext?.volcanicRockAtBlastCenter!==true)throw new Error('Cascata de Magma exige pedra vulcânica colocada no centro da explosão; arremesso e desvio ainda não estão implementados. PFV preservados.');
        if(input.effectContext?.combustiblesPresent!==false)throw new Error('Combustíveis no alcance exigem resolução persistente dos objetos; lançamento bloqueado para não omitir incêndios. PFV preservados.');
        if(input.effectContext?.confirmedArmLengthRadius!==true)throw new Error('Confirme a lista de TODOS os alvos ao alcance das mãos do centro da explosão, incluindo aliados.');
      }
      const areaSpell=isBitterReachAreaSpell(input.spellId);
      const selectedAreaCombatants: any[]=[];
      if(areaSpell){
        if(!activeEncounter||!worldStateRecord)throw new Error('Magia de zona exige encontro ativo com combatentes identificados.');
        if(!Array.isArray(input.areaTargetIds)||!input.areaTargetIds.length)throw new Error('Selecione explicitamente todos os alvos na mesma zona.');
        for(const id of input.areaTargetIds){
          const matches=activeEncounter.combatants.filter((item:any)=>item.id===id||item.entityId===id);
          if(matches.length!==1)throw new Error(`Alvo de zona ausente ou ambíguo: ${id}.`);
          if(selectedAreaCombatants.some(item=>item.id===matches[0].id))throw new Error('Alvo repetido na zona.');
          selectedAreaCombatants.push(matches[0]);
        }
        if(input.spellId==='br-fireball'&&!selectedAreaCombatants.some(item=>item.id===input.effectTargetCharacterId||item.entityId===input.effectTargetCharacterId))
          throw new Error('Selecione o alvo principal da Bola de Fogo entre as criaturas da zona.');
      }
      let areaResolution:AreaResolution|undefined;
      const mishap=result.mishap;
      let finalTargetId = input.spellId==='bm-fires-friend'?character.id:input.effectTargetCharacterId;

      // R88 #41: a resurrection mishap cannot bind this soul to a randomly
      // selected combatant. Keep the released soul pending for GM adjudication.
      if(officialSoulBinding&&mishap){
        finalTargetId=undefined;
        effectTarget=undefined;
        effectCombatant=undefined;
        effectResult={spellId:'resurrection' as any,power:result.power,valid:true,
          effect:'A Ressurreição teve um infortúnio mágico; a alma continua livre para nova adjudicação.',
          requiresGm:true,requiresTarget:false,targetPatch:{}};
      }

      // Handle Unintended Target mishap (rolls 42-45)
      if (['unintended-target','banished'].includes(mishap?.code||'')&&['bm-fires-friend','bm-mental-strength','bm-traceless','bm-lullaby','bm-sand-sleep'].includes(input.spellId)) {
        effectResult={spellId:input.spellId as any,power:result.power,valid:true,
          effect:'Infortúnio: proteção em grupo não aplicada; o Mestre deve decidir como resolver o alvo não intencional.',
          requiresGm:true,requiresTarget:false,targetPatch:{}};
      }
      if (!officialSoulBinding&&mishap?.code === 'unintended-target'&&!['bm-fires-friend','bm-mental-strength','bm-traceless','bm-lullaby','bm-sand-sleep'].includes(input.spellId)) {
        // R45: an unintended target MUST be perceptibly in the same encounter
        // and campaign. A global getAll() here could damage a PC in another save!
        const visible=activeEncounter?.combatants.filter((c:any)=>c.kind==='character'&&
          c.entityId&&['active','broken'].includes(c.status)&&c.entityId!==character.id&&
          c.entityId!==input.effectTargetCharacterId)||[];
        const seen=new Set<string>();
        const candidateIds=visible.map((c:any)=>String(c.entityId)).filter((id:string)=>{
          if(seen.has(id))return false;seen.add(id);return true;
        });
        const candidates:Character[]=[];
        for(const id of candidateIds){
          const actual=await tx.objectStore('characters').get(id) as Character|undefined;
          if(actual && actual.status!=='dead')candidates.push(actual);
        }
        if(candidates.length>0){
          const idx=Math.min(candidates.length-1,Math.max(0,Math.floor(rng()*candidates.length)));
          finalTargetId=candidates[idx].id;
          effectCombatant=activeEncounter.combatants.find((item:any)=>item.entityId===finalTargetId);
        } else {
          finalTargetId=undefined;
          effectTarget=undefined;
          effectCombatant=undefined;
          effectResult={spellId:input.spellId as any,valid:true,power:result.power,
            effect:'Infortúnio de alvo não intencional sem alvo comprovado no mesmo encontro: efeito suspenso para decisão do Mestre.',
            requiresGm:true,requiresTarget:false,targetPatch:{}};
        }
      }

      let resistanceRollDetail: ReturnType<typeof resolveBitterReachResistance>|undefined;
      const auxiliaryRollIds:string[]=[];
      const resistanceContext=(actor:any,attribute:'strength'|'wits',skill:string)=>{
        const rawSkills=actor?.skills||actor?.quickSheet?.skills||{};
        const aliases=skill==='Endurance'?['Endurance','Resilience','Resiliência']:skill==='Insight'?['Insight','Discernimento']:['Might','Potência'];
        const skillValue=Object.entries(rawSkills).find(([name])=>aliases.some(alias=>normalize(name)===normalize(alias)))?.[1];
        const base=actor?.attributes?.[attribute]?.current ?? actor?.quickSheet?.attributes?.[attribute]?.current ?? (attribute==='strength'?actor?.currentStrength:0);
        const roll=resolveBitterReachResistance(input.spellId,result.power,Math.max(0,Math.trunc(Number(base)||0)+Math.trunc(Number(skillValue)||0)),rng);
        if(!roll)return input.effectContext;
        resistanceRollDetail=roll;
        return {...(input.effectContext||{}),opposedSuccess:input.spellId==='br-heat-of-moment'?!roll.targetResisted:roll.targetResisted,failedEndurance:!roll.targetResisted,resistanceRoll:roll};
      };
      const targetSpellContext=async(actor:any)=>{
        if(input.spellId==='bm-amnesia'){
          const wits=BloodmarchBetaResistanceGuard.wits(actor);
          const insight=BloodmarchBetaResistanceGuard.insight(actor);
          const roll=ForbiddenLandsDiceEngine.roll({base:Math.trunc(wits),skill:insight,gear:0,
            modifier:-result.power+BloodmarchBetaMentalGuard.power(actor?.statusEffects)},
            {characterId:actor?.kind==='character'?actor?.id:actor?.id||null,
              sceneId:activeEncounter?.startedSceneId||activeEncounter?.id||`magic:${character.id}`,
              checkId:`bloodmarch-amnesia:${character.id}:${actor?.id||'target'}:${Date.now()}`,rng});
          await tx.objectStore('rolls').put(roll);auxiliaryRollIds.push(roll.id);
          return {...(input.effectContext||{}),resisted:roll.successes>0,resistanceRollId:roll.id};
        }
        if(input.spellId==='bm-confusion'){
          const wits=BloodmarchBetaResistanceGuard.wits(actor);
          if(wits===0)throw new Error('Confusão não pode mirar quem já está com Astúcia zero.');
          const insight=BloodmarchBetaResistanceGuard.insight(actor);
          const roll=ForbiddenLandsDiceEngine.roll({base:Math.trunc(wits),skill:insight,gear:0,modifier:-result.power+BloodmarchBetaMentalGuard.power(actor?.statusEffects)},
            {characterId:actor?.kind==='character'?actor?.id:actor?.id||null,
              sceneId:activeEncounter?.startedSceneId||activeEncounter?.id||`magic:${character.id}`,
              checkId:`bloodmarch-confusion:${character.id}:${actor?.id||'target'}:${Date.now()}`,rng});
          await tx.objectStore('rolls').put(roll);auxiliaryRollIds.push(roll.id);
          return {...(input.effectContext||{}),resisted:roll.successes>0,resistanceRollId:roll.id};
        }
        if(input.spellId==='bm-attract'||input.spellId==='bm-repel'){
          const armor=actor?.armor||actor?.quickSheet?.armor||[];
          return {...(input.effectContext||{}),metalArmor:BloodmarchBetaCombatEngine.metalArmor(armor)};
        }
        if(input.spellId==='bm-bloodshock'){
          const wits=BloodmarchBetaResistanceGuard.wits(actor);
          const dice=Math.max(0,wits-result.power+BloodmarchBetaMentalGuard.power(actor?.statusEffects));
          const roll=ForbiddenLandsDiceEngine.roll({base:dice,skill:0,gear:0},{characterId:actor?.id||null,
            sceneId:activeEncounter?.startedSceneId||activeEncounter?.id||`magic:${character.id}`,
            checkId:`bloodmarch-bloodshock:${character.id}:${actor?.id||'target'}:${Date.now()}`,rng});
          await tx.objectStore('rolls').put(roll);auxiliaryRollIds.push(roll.id);
          return {...(input.effectContext||{}),resisted:roll.successes>0,resistanceRollId:roll.id};
        }
        const context=resistanceContext(actor,input.spellId==='br-heat-of-moment'?'wits':'strength',input.spellId==='br-cold-snap'?'Endurance':input.spellId==='br-heat-of-moment'?'Insight':'Might');
        if(['br-combustion','bm-ignite'].includes(input.spellId)&&input.effectContext?.worn===true){
          const roll=ForbiddenLandsDiceEngine.roll({base:Math.min(30,result.power*2),skill:0,gear:0},
            {characterId:null,sceneId:activeEncounter?.startedSceneId||activeEncounter?.id||`magic:${character.id}`,
              checkId:`bitter-combustion:${character.id}:${actor.id||'target'}:${Date.now()}`,rng});
          await tx.objectStore('rolls').put(roll);
          auxiliaryRollIds.push(roll.id);
          return {...context,attackSuccesses:roll.successes};
        }
        return context;
      };
      if (finalTargetId && !directivePlan && !effectResult && !areaSpell && input.spellId!=='br-summon-elemental') {
        if (effectCombatant && effectCombatant.kind === 'character' && effectCombatant.entityId) {
          effectTarget = await tx.objectStore('characters').get(effectCombatant.entityId) as Character | undefined;
        } else if (!effectCombatant) {
          effectTarget = await tx.objectStore('characters').get(finalTargetId) as Character | undefined;
        }

        if (effectTarget) {
          if (effectTarget.campaignId && configuredCampaignId && effectTarget.campaignId!==configuredCampaignId)
            throw new Error('Alvo pertence a outra campanha; PFV preservados.');
          const activeInjury=(effectTarget.criticalInjuryStates||[]).find(injury=>(injury.status==='active'||injury.status==='treated')&&(input.spellId!=='bm-stanch-blood'||injury.lethal));
          const spellTarget:SpellTarget={id:effectTarget.id,kind:input.effectTargetKind||'character',livingHumanoid:effectTarget.status!=='dead',dead:effectTarget.status==='dead',self:effectTarget.id===character.id,
            strength:effectTarget.attributes.strength.current,strengthMax:effectTarget.attributes.strength.max,agility:effectTarget.attributes.agility.current,agilityMax:effectTarget.attributes.agility.max,
            wits:effectTarget.attributes.wits.current,witsMax:effectTarget.attributes.wits.max,currentWits:effectTarget.attributes.wits.current,
            empathy:effectTarget.attributes.empathy.current,empathyMax:effectTarget.attributes.empathy.max,
            criticalInjury:activeInjury?{id:activeInjury.id,lethal:activeInjury.lethal,status:activeInjury.status}:undefined,
            diseaseVirulence:effectTarget.diseaseState?.virulence,poisonPotency:effectTarget.poisonState?.active?effectTarget.poisonState.potency:0,
            sameKin:normalize(effectTarget.kin)===normalize(character.kin),animalStrength:Number(input.effectContext?.animalStrength)||undefined,
            animalAgility:Number(input.effectContext?.animalAgility)||undefined,isMonster:Boolean(input.effectContext?.isMonster),agitated:Boolean(input.effectContext?.agitated),
            frightened:Boolean(input.effectContext?.frightened),inCombat:Boolean(input.effectContext?.inCombat),
          };
          if(officialSoulBinding){
            if(mishap){
              effectResult={spellId:'resurrection' as any,power:result.power,valid:true,effect:'A Ressurreição teve um infortúnio mágico; a alma continua livre para nova adjudicação.',
                requiresGm:true,requiresTarget:false,targetPatch:{}};
            }else{
              const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte de dados inválida para o D6 da alma R88 #41.');
              officialSoulDispositionD6=(1+Math.floor(value*6)) as 1|2|3|4|5|6;
              const disposition=OfficialEncounterRulesEngine.soulBottleDisposition(officialSoulDispositionD6)==='rational'
                ?'personalidade original e comportamento racional':'personalidade irracional e enlouquecida';
              effectResult={spellId:'resurrection' as any,power:result.power,valid:true,requiresTarget:true,targetPatch:{},
                effect:`Ressurreição R88 #41: a alma foi dirigida a ${effectTarget.name}. D6 ${officialSoulDispositionD6}: ${disposition}.`};
            }
          }else{
            effectResult=ForbiddenLandsSpellEffectEngine.resolve({spellId:input.spellId as any,power:result.power,target:spellTarget,context:await targetSpellContext(effectTarget)});
          }
          if(!effectResult.valid)throw new Error(effectResult.effect);
        } else if (effectCombatant) {
          // Synthetic target for non-character combatant (NPC / Monster)
          const activeInjury = (effectCombatant.criticalInjuries || []).find((injury: any) =>
            (injury.status === 'active' || injury.status === 'treated') && (input.spellId !== 'bm-stanch-blood' || injury.lethal));
          const spellTarget: SpellTarget = {
            id: effectCombatant.id,
            kind: effectCombatant.kind || 'other',
            livingHumanoid: effectCombatant.status !== 'dead' && effectCombatant.kind !== 'monster' && effectCombatant.kind !== 'undead',
            dead: effectCombatant.status === 'dead',
            self: false,
            strength: effectCombatant.currentStrength,
            strengthMax: effectCombatant.maxStrength,
            agility: effectCombatant.quickSheet?.attributes?.agility?.current ?? 2,
            agilityMax: effectCombatant.quickSheet?.attributes?.agility?.max ?? 3,
            wits: effectCombatant.quickSheet?.attributes?.wits?.current ?? 2,
            witsMax: effectCombatant.quickSheet?.attributes?.wits?.max ?? 3,
            currentWits: effectCombatant.quickSheet?.attributes?.wits?.current || 2,
            empathy: effectCombatant.quickSheet?.attributes?.empathy?.current ?? 2,
            empathyMax: effectCombatant.quickSheet?.attributes?.empathy?.max ?? 3,
            criticalInjury: activeInjury ? { id: activeInjury.id, lethal: activeInjury.lethal, status: activeInjury.status } : undefined,
            diseaseVirulence: effectCombatant.quickSheet?.diseaseState?.virulence,
            poisonPotency: effectCombatant.quickSheet?.poisonState?.active ? effectCombatant.quickSheet.poisonState.potency : 0,
            sameKin: false,
            isMonster: effectCombatant.kind === 'monster',
            inCombat: true
          };
          effectResult = ForbiddenLandsSpellEffectEngine.resolve({spellId:input.spellId as any,power:result.power,target:spellTarget,context:await targetSpellContext(effectCombatant)});
          if(!effectResult.valid)throw new Error(effectResult.effect);
        } else {
          throw new Error('Alvo de magia não encontrado.');
        }
      }
      if(!effectResult && areaSpell){
        effectResult={spellId:input.spellId as any,power:result.power,valid:true,effect:'Resolução por zona: alvos, armaduras e resistências serão aplicados individualmente.',requiresTarget:false};
      }
      if(sleepSpell&&!effectResult){
        effectResult={spellId:input.spellId as any,power:result.power,valid:true,requiresTarget:false,
          effect:`Sono em área: ${sleepTargetCombatants.length} alvo(s) elegível(is), com resistência individual sem forçar.`,
          duration:input.spellId==='bm-lullaby'?'375 minutos':'360 minutos'};
      }
      if(input.spellId==='bm-traceless'&&!effectResult){
        effectResult=ForbiddenLandsSpellEffectEngine.resolve({spellId:input.spellId as any,power:result.power,
          target:{id:character.id,kind:'character',self:true,dead:false,livingHumanoid:true}});
        if(!effectResult.valid)throw new Error(effectResult.effect);
      }
      if(input.spellId==='bm-false-shape'&&!effectResult){
        effectResult=ForbiddenLandsSpellEffectEngine.resolve({spellId:input.spellId as any,power:result.power,
          target:{id:character.id,kind:'character',self:true,dead:false,livingHumanoid:true},
          context:{...(input.effectContext||{}),livePupaConsumed:true}});
        if(!effectResult.valid)throw new Error(effectResult.effect);
      }
      if(!effectResult){
        effectResult=ForbiddenLandsSpellEffectEngine.resolve({spellId:input.spellId as any,power:result.power,context:materialPlan?{...input.effectContext,worldValidated:true,worldPlan:materialPlan}:mentalPlan?{...input.effectContext,mentalWorldValidated:true,mentalWorldPlan:mentalPlan}:activeWorldPlan?{...input.effectContext,activeWorldValidated:true,activeWorldPlan}:directivePlan?{...input.effectContext,directiveValidated:true,directivePlan}:propheticPlan?{...input.effectContext,propheticValidated:true,propheticPlan}:dreamTravelPlan?{...input.effectContext,dreamTravelValidated:true,dreamTravelPlan}:maraPlan?{...input.effectContext,maraValidated:true,maraPlan}:fireWyrmPlan?{...input.effectContext,fireWyrmValidated:true,fireWyrmPlan}:input.effectContext});
        if(!effectResult.valid)throw new Error(effectResult.effect);
      }

      let reversedEffectPending=false;
      if(mishap?.code==='reversed'){
        if(effectResult.healing && effectResult.healing > 0 && (effectTarget || effectCombatant)) {
          // Reverse healing: it deals damage of the same amount!
          const healAmount = effectResult.healing;
          const currentTgt = effectTarget || effectCombatant;
          const targetStr = currentTgt.attributes?.strength?.current ?? currentTgt.currentStrength;
          effectResult = {
            ...effectResult,
            healing: 0,
            damage: healAmount,
            effect: `Infortúnio reverso: a magia de cura foi invertida e causou ${healAmount} de dano!`,
            targetPatch: {
              ...effectResult.targetPatch,
              strength: Math.max(0, targetStr - healAmount)
            }
          };
        } else if(effectResult.damage&&effectResult.damage>0&&effectResult.requiresTarget){
          // Infortúnio 62–65: magia ofensiva atinge quem a conjurou. Recalcular
          // patches que dependem dos atributos do alvo usando a ficha da conjuradora.
          const injury=(character.criticalInjuryStates||[]).find(item=>item.status==='active'||item.status==='treated');
          const casterTarget:SpellTarget={id:character.id,kind:'character',livingHumanoid:true,dead:false,self:true,
            strength:character.attributes.strength.current,strengthMax:character.attributes.strength.max,
            agility:character.attributes.agility.current,agilityMax:character.attributes.agility.max,
            wits:character.attributes.wits.current,witsMax:character.attributes.wits.max,currentWits:character.attributes.wits.current,
            empathy:character.attributes.empathy.current,empathyMax:character.attributes.empathy.max,
            criticalInjury:injury?{id:injury.id,lethal:injury.lethal,status:injury.status}:undefined,
            diseaseVirulence:character.diseaseState?.virulence,poisonPotency:character.poisonState?.active?character.poisonState.potency:0,
            sameKin:true,inCombat:Boolean(input.effectContext?.inCombat)};
          const redirected=ForbiddenLandsSpellEffectEngine.resolve({spellId:input.spellId as any,power:result.power,target:casterTarget,context:input.effectContext});
          if(redirected.valid){
            effectTarget=character;
            effectResult={
              ...redirected,
              effect: `Infortúnio reverso: a magia ofensiva foi redirecionada e atingiu a própria conjuradora ${character.name}!`
            };
          }else{
            effectTarget=undefined;
            reversedEffectPending=true;
            effectResult={...effectResult,damage:undefined,healing:undefined,targetPatch:undefined,requiresTarget:false,requiresGm:true,
              effect:'Infortúnio reverso: alvo incompatível com a conjuradora; o Mestre define a consequência.'};
          }
        }else{
          // Cura, proteção, metamorfose e invocações têm inversões distintas
          // sob decisão do Mestre; não aplicar silenciosamente o efeito original.
          effectTarget=undefined;
          reversedEffectPending=true;
          effectResult={...effectResult,damage:undefined,healing:undefined,targetPatch:undefined,requiresTarget:false,requiresGm:true,
            effect:'Infortúnio reverso: efeito original suspenso; o Mestre define a consequência invertida.'};
        }
      }
      if(effectResult.requiresTarget&&!effectTarget&&!effectCombatant)throw new Error('Este efeito exige selecionar um alvo antes de gastar PFV.');
      // The Bloodmarch p.31: direct magical Wits/Empathy damage is blocked by
      // an active Mental Strength marker. Confusion and mind control are NOT
      // ordinary damage and retain their separate resistance rules.
      if((effectTarget||effectCombatant)&&effectResult?.valid&&!effectResult.requiresGm&&Number(effectResult.damage)>0){
        const effects=effectTarget?.statusEffects||effectCombatant?.statusEffects;
        if(BloodmarchBetaMentalGuard.power(effects)>0){
          const patch={...(effectResult.targetPatch||{})};
          const attrs=effectTarget?.attributes||effectCombatant?.quickSheet?.attributes;
          const blocked:string[]=[];
          for(const key of ['wits','empathy'] as const){
            const current=attrs?.[key]?.current;
            const attacksAttribute=patch.damageAttribute===key || typeof patch[key]==='number'&&patch[key]<current;
            if(attacksAttribute&&Number.isFinite(current)){
              patch[key]=current;blocked.push(key);
            }
          }
          if(blocked.length){
            effectResult={...effectResult,targetPatch:patch,
              effect:`${effectResult.effect} Força Mental impediu dano em ${blocked.join(' e ')}.`};
          }
        }
      }
      // The magical identity is randomized, never chosen by the player. This
      // changes visible/scent/track clues but does not confer invisibility.
      const traceDisguises=['voz rouca e cheiro de couro úmido','voz clara e cheiro de ervas secas',
        'passos pesados e odor de ferro','passos leves e aroma de fumaça','voz baixa e odor de terra molhada',
        'passos irregulares e cheiro de lã'];
      const randomTraceMask=()=>traceDisguises[Math.min(traceDisguises.length-1,Math.max(0,Math.floor(rng()*traceDisguises.length)))];
      if(input.spellId==='bm-traceless'&&effectResult?.valid&&!effectResult.requiresGm&&effectResult.targetPatch?.tracesAltered===true)
        effectResult={...effectResult,targetPatch:{...effectResult.targetPatch,traceDisguise:randomTraceMask()}};
      if(effectCombatant?.quickSheet?.officialProcessionEnt===true&&Number(effectResult.damage)>0&&
        ['immolate','br-combustion','bm-ignite'].includes(input.spellId)){
        const entFire=OfficialEntDamageRulesEngine.resolve({damage:Number(effectResult.damage),incomingKind:'fire',magical:true,targetQuickSheet:effectCombatant.quickSheet});
        const oldPatch=effectResult.targetPatch||{};
        const burningBase=Number(oldPatch.burningDamagePerRound??oldPatch.damagePerRound)||1;
        effectResult={...effectResult,damage:entFire.damage,targetPatch:{...oldPatch,
          strength:Math.max(0,Number(effectCombatant.currentStrength)-entFire.damage),
          ...(oldPatch.burning===true?{burningDamagePerRound:burningBase*entFire.multiplier,damagePerRound:burningBase*entFire.multiplier}:{})}};
      }
      if(effectCombatant?.quickSheet?.officialTeramalda===true){
        const patch={...(effectResult.targetPatch||{})};
        const attrs=effectCombatant.quickSheet.attributes as Record<string,{current?:number}>|undefined;
        const blocked:string[]=[];
        for(const key of ['strength','agility','wits','empathy'] as const){
          const current=key==='strength'?effectCombatant.currentStrength:Number(attrs?.[key]?.current||0);
          if(typeof patch[key]==='number'&&patch[key]<current){patch[key]=current;blocked.push(key);}
        }
        const damage=Number(effectResult.damage)||0;
        const immunity=OfficialEncounterCombatRulesEngine.incomingDamage({targetQuickSheet:effectCombatant.quickSheet,damage});
        if(immunity.immune&&(damage>0||blocked.length))
          effectResult={...effectResult,damage:0,targetPatch:patch,effect:`${effectResult.effect} Teramalda é imune a todo dano; o efeito não reduziu seus atributos.`};
      }
      if(effectTarget&&effectResult.damage)effectResult=MagicCastingService.withDirectDamage(effectTarget,effectResult);
      if(effectResult.targetPatch&&!effectResult.requiresTarget&&!effectTarget&&!effectCombatant&&input.spellId!=='bm-mental-strength'&&!sleepSpell)effectTarget=character;

      let next={...character,willpowerPoints:WillpowerRulesEngine.change(character.willpowerPoints,-result.willpowerSpent)};
      if(input.spellId==='bm-false-shape'){
        const gear=[...(next.gear||[])];
        const item=gear[pupaIndex] as any;
        if(!item)throw new Error('Pupa viva não encontrada ao persistir a magia.');
        const amount=Number(item.quantity??1);
        if(amount===1)gear.splice(pupaIndex,1);
        else gear[pupaIndex]={...item,quantity:amount-1};
        next={...next,gear};
        next={...next,encumbrance:EncumbranceRulesEngine.calculate(next)};
      }

      if(input.spellId==='bm-sand-sleep'){
        const gear=[...(next.gear||[])];const item=gear[sandIndex] as any;
        if(!item)throw new Error('Areia fina desapareceu antes de concluir a magia.');
        const quantity=Number(item.quantity??1);
        if(quantity===1)gear.splice(sandIndex,1);
        else gear[sandIndex]={...item,quantity:quantity-1};
        next={...next,gear,encumbrance:EncumbranceRulesEngine.calculate({...next,gear})};
      }

      const appliedConsequences: string[]=[];
      const pendingConsequences: string[]=[];
      if(mishap){
        if(mishap.code==='reputation'){next.reputation=Math.max(0,(Number(next.reputation)||0)+1);appliedConsequences.push('reputation+1');}
        else if(mishap.code==='hungry'||mishap.code==='thirsty'){
          const condition=mishap.code==='hungry'?'Starving':'Thirsty';
          next.conditions=Array.from(new Set([...(next.conditions||[]),condition])); appliedConsequences.push(condition);
        } else if(['strength','agility','wits','empathy'].includes(mishap.code)){
          const key=mishap.code as keyof Character['attributes'];
          const before=next.attributes[key].current; const current=Math.max(0,before-1);
          next.attributes={...next.attributes,[key]:{...next.attributes[key],current}};
          if(before>0&&current===0){next.brokenAttribute=key;next.conditions=Array.from(new Set([...(next.conditions||[]),'Broken']));}
          appliedConsequences.push(`${key}-1`);
        } else if(mishap.code==='horror-injury'||mishap.code==='concussion-injury'){
          const table:CriticalInjuryState['table']=mishap.code==='horror-injury'?'horror':'blunt';
          const rolled=CriticalInjuryRulesEngine.rollWithResult(table,rng),entry=rolled.entry,now=Date.now();
          const deathTimer=CriticalInjuryRulesEngine.rollDeathTimer(entry,rng);
          const healingTime=CriticalInjuryRulesEngine.rollHealing(entry,rng);
          const injury:CriticalInjuryState={id:crypto.randomUUID(),table,roll:rolled.roll,ruleId:entry.id,name:entry.name,lethal:entry.lethal,
            deathTimer,healingTime,effects:entry.effects,sourcePage:entry.sourcePage,status:'active',createdAt:now,
            ...(deathTimer&&'amount' in deathTimer?{deathTimeRemaining:{amount:deathTimer.amount,unit:deathTimer.unit}}:{}),
            ...(healingTime&&'amount' in healingTime?{healingDaysRemaining:healingTime.amount}:{})};
          next={...next,criticalInjuryStates:[...(next.criticalInjuryStates||[]),injury],criticalInjuries:[...(next.criticalInjuries||[]),`${table}:${rolled.roll}:${entry.name}`],...(injury.deathTimer?.immediate?{status:'dead' as const}:{})};
          appliedConsequences.push(`critical:${table}:${rolled.roll}:${entry.id}`);
          if(injury.deathTimer?.immediate)appliedConsequences.push('status:dead');
        } else if(mishap.code==='insomnia'||mishap.code==='blindness'||mishap.code==='magic-disease'){
          const detail=mishap.code==='insomnia'?`Sem conseguir dormir por ${mishap.days||1} dia(s); recebe Sonolento.`:mishap.code==='blindness'?'Age como em escuridão total por um dia inteiro.':`Doença mágica, Virulência ${mishap.virulence||2}; conjuradora e criaturas ao alcance das mãos ficam suscetíveis ao contágio por 1 QD.`;
          const effect={id:`magic-mishap:${mishap.code}:${crypto.randomUUID()}`,label:`Infortúnio mágico: ${mishap.code}`,detail};
          const before=next.magicMishapState||{};
          const magicMishapState={...before,...(mishap.code==='insomnia'?{insomniaQuartersRemaining:Math.max(before.insomniaQuartersRemaining||0,(mishap.days||1)*4)}:{}),...(mishap.code==='blindness'?{blindnessQuartersRemaining:Math.max(before.blindnessQuartersRemaining||0,4)}:{}),...(mishap.code==='magic-disease'?{contagionQuartersRemaining:Math.max(before.contagionQuartersRemaining||0,1),magicalDiseaseVirulence:mishap.virulence||2}:{})};
          next={...next,magicMishapState,statusEffects:[...(next.statusEffects||[]),effect],...(mishap.code==='insomnia'?{conditions:Array.from(new Set([...next.conditions,'Sleep Deprived' as const]))}:{})};
          pendingConsequences.push(mishap.code==='magic-disease'?'magic-disease-nearby-characters-need-resilience-exposure-roll':mishap.code==='insomnia'?'insomnia-blocks-sleep-until-day-clock-expires':'blindness-duration-is-clocked');
        } else if(mishap.code==='reversed'){
          if(reversedEffectPending)pendingConsequences.push('reversed-effect-awaits-gm; original-effect-not-applied');
          else appliedConsequences.push('reversed-offensive-effect-hit-caster');
        } else if(mishap.code==='banished'){
          next={...next,status:'dead'};appliedConsequences.push('status:dead');pendingConsequences.push('new-character; schedule altered NPC return after D66 days');
        } else if(mishap.code==='appearance') {
          // Permanently reduce Empathy maximum by 1
          const beforeMax = next.attributes.empathy.max;
          const nextMax = Math.max(1, beforeMax - 1);
          next.attributes = {
            ...next.attributes,
            empathy: {
              max: nextMax,
              current: Math.min(next.attributes.empathy.current, nextMax)
            }
          };
          appliedConsequences.push('empathyMax-1');
        } else if(mishap.code==='demon') {
          appliedConsequences.push('demon-summoned');
          pendingConsequences.push('demon-attacks-party');
        } else pendingConsequences.push(mishap.code);
      }

      // Nightmares applies all recipients together below, against the latest
      // encounter snapshot; never run the generic primary-target write first.
      if(input.spellId==='bm-nightmares'&&effectResult.valid&&!effectResult.requiresGm
          &&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        effectResult={...effectResult,requiresTarget:false,targetPatch:undefined};
        effectTarget=undefined;effectCombatant=undefined;
      }
      if(['bm-stanch-blood','bm-bloodshock','bm-amnesia'].includes(input.spellId)&&!effectTarget&&!effectCombatant)
        throw new Error('Esta magia exige personagem ou PNJ identificado; PFV preservados.');
      // An effect that still needs GM adjudication is only a pending receipt.
      // Do NOT mutate a sheet or combatant until mechanical resolution is known.
      // In particular, a targetPatch attached to requiresGm is NOT permission
      // to apply it, and a reversed/redirected/banished mishap suspends it.
      const genericEffectCommitted=Boolean(effectResult?.valid&&!effectResult.requiresGm
        &&!['reversed','unintended-target','banished'].includes(mishap?.code||''));
      if(effectTarget&&effectResult?.targetPatch&&genericEffectCommitted){
        if(effectTarget.id===next.id){
          next=MagicCastingService.applyCharacterEffect(next,effectResult);
          if(officialSoulBinding&&officialSoulDispositionD6){
            const binding={soulTokenId:officialSoulBinding.tokenId,sourceEventId:officialSoulBinding.eventId,targetKind:officialSoulBinding.targetKind,
              disposition:OfficialEncounterRulesEngine.soulBottleDisposition(officialSoulDispositionD6),d6:officialSoulDispositionD6,boundAt:Date.now(),sourcePage:165 as const};
            next={...next,officialSoulBindings:[...(next.officialSoulBindings||[]),binding]};
          }
          effectTarget=next;
        } else {
          effectTarget=MagicCastingService.applyCharacterEffect(effectTarget,effectResult);
          if(officialSoulBinding&&officialSoulDispositionD6){
            const binding={soulTokenId:officialSoulBinding.tokenId,sourceEventId:officialSoulBinding.eventId,targetKind:officialSoulBinding.targetKind,
              disposition:OfficialEncounterRulesEngine.soulBottleDisposition(officialSoulDispositionD6),d6:officialSoulDispositionD6,boundAt:Date.now(),sourcePage:165 as const};
            effectTarget={...effectTarget,officialSoulBindings:[...(effectTarget.officialSoulBindings||[]),binding]};
          }
          await tx.objectStore('characters').put(effectTarget);
        }
        await MagicCastingService.syncActiveEncounters(tx.objectStore('world_state'),effectTarget,effectResult,Date.now(),configuredCampaignId||worldStateRecord?.id);
      } else if (effectCombatant && effectResult?.targetPatch && genericEffectCommitted
        && Object.keys(effectResult.targetPatch).length>0 && worldStateRecord) {
        // Apply direct patch to NPC / Monster combatant inside the transaction
        const patch = effectResult.targetPatch;
        const updatedCombatants = activeEncounter.combatants.map((item: any) => {
          if (item.id !== effectCombatant.id) return item;
          const oldAttributes=(item.quickSheet?.attributes||{}) as Record<string,{current?:number;max?:number}>;
          const attributes={...oldAttributes};
          for(const key of ['strength','agility','wits','empathy']){
            const current=key==='strength'?item.currentStrength:Number(oldAttributes[key]?.current??0);
            if(typeof patch[key]==='number')attributes[key]={...(oldAttributes[key]||{}),current:Math.max(0,Math.trunc(patch[key]))};
            else if(key==='strength'&&!attributes.strength)attributes.strength={current,max:item.maxStrength};
          }
          const updatedStrength=Number(attributes.strength?.current??item.currentStrength);
          const brokenKey=['strength','agility','wits','empathy'].find(key=>
            typeof patch[key]==='number'&&Number(attributes[key]?.current)===0&&Number(oldAttributes[key]?.current??(key==='strength'?item.currentStrength:1))>0);
          const addedConditions=(Array.isArray(patch.conditionsAdd)?patch.conditionsAdd:[]).filter((condition:any)=>
            ['Cold','Thirsty','Starving','Sleep Deprived','Sick','Broken'].includes(String(condition)));
          const conditions=Array.from(new Set([...(item.conditions||[]),...addedConditions,...(brokenKey?['Broken']:[])]));
          // The Bloodmarch p.37: stop only the fatal countdown; do not heal
          // the injury, erase its sequelae or create an endless magic marker.
          const criticalInjuries=patch.stopFatalBleeding===true
            ?(item.criticalInjuries||[]).map((injury:any)=>
                injury.id===patch.stopFatalBleedingInjuryId&&injury.lethal&&['active','treated'].includes(injury.status)
                  ?{...injury,status:'treated',deathTimer:null,deathTimeRemaining:null}:injury)
            :item.criticalInjuries;
          const durationClock=parseDurationToQuartersOrMinutes(effectResult!.duration,result.power);
          const npcRemainingMinutes=typeof durationClock.remainingQuarters==='number'
            ?durationClock.remainingQuarters*360:durationClock.remainingMinutes;
          const effectStatus=patch.stopFatalBleeding===true?[]:[
              { id: `magic:${input.spellId}`, name: input.spellId, detail: JSON.stringify({ patch, duration: effectResult!.duration }),
                ...(typeof npcRemainingMinutes==='number'?{remainingMinutes:npcRemainingMinutes}:{}),
                ...(input.spellId==='br-suffocate'?{ongoingRounds:Number(patch.ongoingRounds)||result.power,ongoingDamage:1,speechBlocked:true}:{}),
                ...(input.spellId==='br-icy-breath'?{remainingMinutes:15}:{}),
                ...(typeof patch.incomingMetalAttackModifier==='number'?{incomingMetalAttackModifier:patch.incomingMetalAttackModifier}:{}),
                ...(typeof patch.remainingRounds==='number'?{remainingRounds:patch.remainingRounds}:{}),
                ...(input.spellId==='bm-confusion'?{confusionRecoveryTo:Number((patch.bmConfusion as any)?.recoveryTo)||0,remainingRounds:Number((patch.bmConfusion as any)?.remainingRounds)||result.power}:{}),
              }];
          return {
            ...item,
            currentStrength:updatedStrength,conditions,brokenAttribute:brokenKey||item.brokenAttribute,
            status:brokenKey||updatedStrength===0?'broken' as const:item.status,
            quickSheet:{...(item.quickSheet||{}),attributes,conditions},criticalInjuries,
            statusEffects: [
              ...(item.statusEffects || []).filter((e: any) => e.id !== `magic:${input.spellId}`&&(!patch.burning||e.id!=='burning')&&!(patch.stopFatalBleeding&&e.id.startsWith('bitter-harpoon-bleed:'))),
              ...effectStatus,
              ...(patch.burning?[{id:'burning',name:'Em chamas',source:spell.name,ongoingDamage:Number(patch.burningDamagePerRound??patch.damagePerRound)||1,ongoingRounds:0,detail:`${Number(patch.burningDamagePerRound??patch.damagePerRound)||1} dano de Força por rodada até apagar com Movimentação.`}]:[])
            ]
          };
        });

        await tx.objectStore('world_state').put({
          ...worldStateRecord,
          encounter: {
            ...activeEncounter,
            combatants: updatedCombatants,
            log: [...activeEncounter.log, { id: crypto.randomUUID(), round: activeEncounter.round, text: `${effectCombatant.name}: sofreu efeito da magia ${input.spellId}.`, createdAt: Date.now() }].slice(-300),
            updatedAt: Date.now()
          },
          updatedAt: Date.now()
        });
      }
      if(officialSoulBinding&&officialSoulDispositionD6&&effectResult?.valid&&!effectResult.requiresGm&&!mishap){
        const activeId=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
        const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(activeId),activeId);
        const events=world.journey.events.map(event=>{
          if(event.id!==officialSoulBinding!.eventId)return event;
          const decision=event.result?.officialSceneDecision as Record<string,unknown>|undefined;
          const tokens=[...((decision?.releasedSoulTokens as Array<Record<string,unknown>>|undefined)||[])];
          const index=tokens.findIndex(token=>token.id===officialSoulBinding!.tokenId&&token.state==='released'&&token.resurrectionPending===true);
          if(index<0)throw new Error('A alma foi vinculada em outra operação; Ressurreição cancelada e transação revertida.');
          tokens[index]={...tokens[index],state:'bound',resurrectionPending:false,targetId:officialSoulBinding!.targetId,
            targetKind:officialSoulBinding!.targetKind,personalityD6:officialSoulDispositionD6,
            disposition:OfficialEncounterRulesEngine.soulBottleDisposition(officialSoulDispositionD6),boundAt:Date.now(),casterId:character.id,
            targetItemRequirementVerified:true};
          return {...event,detail:`${event.detail} Ressurreição R88 #41 vinculou uma alma a ${effectTarget?.name||'destino'}.`,
            result:{...(event.result||{}),officialSceneDecision:{...decision,releasedSoulTokens:tokens,
              soulResurrectionD6Pending:tokens.some(token=>token.state==='released'&&token.resurrectionPending===true)}}};
        });
        if(!events.some(event=>event.id===officialSoulBinding!.eventId))throw new Error('Encontro da alma desapareceu durante a conjuração; transação revertida.');
        await store.put({...world,journey:{...world.journey,events,updatedAt:Date.now()},updatedAt:Date.now()});
        appliedConsequences.push(`official-r88-soul:${officialSoulDispositionD6}`);
      }

      // R54: immediate Wits damage, next-day sleeplessness and an ACTUAL D66
      // horror injury for newly broken victims. Character + NPC commits are
      // atomic with the same PFV/event writes; never patch another campaign.
      if(input.spellId==='bm-nightmares'&&nightmareRecipients.length&&effectResult.valid&&!effectResult.requiresGm
          &&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const store=tx.objectStore('world_state');
        const raw=await store.get(worldStateRecord.id);
        const world=WorldStateService.normalize(raw,worldStateRecord.id);
        if(!world.encounter||world.encounter.status!=='active')throw new Error('Encontro encerrado durante Pesadelos; transação cancelada.');
        const affected=[] as string[];
        let combatants=[...world.encounter.combatants];
        for(const recipient of nightmareRecipients){
          const c=combatants.find((item:any)=>item.id===recipient.combatant.id);
          if(!c||!['active','broken'].includes(c.status))throw new Error('Vítima desapareceu antes do efeito de Pesadelos.');
          const actor=recipient.character?.id===next.id?next:recipient.character;
          const wits=BloodmarchBetaResistanceGuard.wits(actor||c);
          const resolved=ForbiddenLandsSpellEffectEngine.resolve({spellId:'bm-nightmares' as any,power:result.power,
            target:{id:actor?.id||c.id,kind:actor?'character':c.kind,dead:false,isMonster:false,livingHumanoid:true,wits},
            context:{sleepConfirmed:true}});
          if(!resolved.valid||!resolved.targetPatch)throw new Error(`Pesadelos: ${resolved.effect}`);
          const patch=resolved.targetPatch as Record<string,any>;
          let injury:CriticalInjuryState|undefined;
          if(patch.horrorCriticalRequired===true){
            const rolled=CriticalInjuryRulesEngine.rollWithResult('horror',rng);
            const entry=rolled.entry,now=Date.now();
            injury={id:crypto.randomUUID(),table:'horror',roll:rolled.roll,ruleId:entry.id,name:entry.name,lethal:entry.lethal,
              deathTimer:CriticalInjuryRulesEngine.rollDeathTimer(entry,rng),healingTime:CriticalInjuryRulesEngine.rollHealing(entry,rng),
              effects:entry.effects,sourcePage:entry.sourcePage,status:'active',createdAt:now};
            if(injury.deathTimer&&'amount' in injury.deathTimer)injury.deathTimeRemaining={amount:injury.deathTimer.amount,unit:injury.deathTimer.unit};
            if(injury.healingTime&&'amount' in injury.healingTime)injury.healingDaysRemaining=injury.healingTime.amount;
          }
          const fatal=injury?.deathTimer?.immediate===true;
          let currentAttributes:any;
          let conditions:string[];
          if(actor){
            let updated=MagicCastingService.applyCharacterEffect(actor,resolved);
            if(injury)updated={...updated,criticalInjuryStates:[...(updated.criticalInjuryStates||[]),injury],
              criticalInjuries:[...(updated.criticalInjuries||[]),`horror:${injury.roll}:${injury.name}`],
              ...(fatal?{status:'dead' as const}:{})};
            if(actor.id===next.id)next=updated;
            else await tx.objectStore('characters').put(updated);
            currentAttributes=updated.attributes;conditions=updated.conditions;
          }else{
            const npcAttributes=c.quickSheet?.attributes as Record<string,any>|undefined;
            currentAttributes={...(npcAttributes||{}),wits:{...(npcAttributes?.wits||{}),current:patch.wits}};
            conditions=Array.from(new Set([...(c.conditions||[]),'Sleep Deprived',...(wits>0&&patch.wits===0?['Broken']:[])]));
          }
          combatants=combatants.map((item:any)=>item.id!==c.id?item:{...item,
            ...(fatal?{status:'dead'}:wits>0&&patch.wits===0?{status:'broken',brokenAttribute:'wits'}:{}),
            conditions,quickSheet:{...(item.quickSheet||{}),attributes:currentAttributes,conditions},
            ...(injury?{criticalInjuries:[...(item.criticalInjuries||[]),injury]}:{}),
            statusEffects:[...(item.statusEffects||[]).filter((e:any)=>e.id!=='magic:bm-nightmares'),
              {id:'magic:bm-nightmares',name:'Pesadelos',detail:JSON.stringify({patch,duration:'dia seguinte'}),remainingQuarters:4},
              ...(injury?[{id:`critical-injury:${injury.id}`,name:`Ferimento grave: ${injury.name}`,
                detail:`Horror D66 ${injury.roll}`,criticalTable:'horror',criticalInjury:injury}]:[])]});
          affected.push(c.name);
          appliedConsequences.push(`nightmares:${actor?.id||c.id}`);
        }
        const now=Date.now();
        await store.put({...world,encounter:{...world.encounter,combatants,
          log:[...(world.encounter.log||[]),{id:crypto.randomUUID(),round:world.encounter.round,
            text:`Pesadelos: ${affected.join(', ')}; dano em Astúcia e sono comprometido.`,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now});
        effectResult={...effectResult,effect:`Pesadelos atingiu ${affected.length} vítima(s): ${affected.join(', ')}. Horror D66 rolado quando Astúcia foi quebrada.`};
      }

      // R53: Amnésia em múltiplas pessoas, pp.32: independent Insight saves,
      // one atomic write set for PFV, rolls, NPC encounter and character sheets.
      // Never apply extra recipients after an unresolved mishap.
      if(input.spellId==='bm-amnesia'&&amnesiaRecipients.length&&effectResult?.valid&&!effectResult.requiresGm
          &&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const forgotten:string[]=[];
        const resisted:string[]=[];
        for(const recipient of amnesiaRecipients){
          const actor=recipient.character||recipient.combatant;
          const ctx=await targetSpellContext(actor);
          const resolved=ForbiddenLandsSpellEffectEngine.resolve({spellId:'bm-amnesia',power:result.power,
            target:{id:actor.id,kind:recipient.character?'character':'npc',livingHumanoid:true,dead:false,
              wits:BloodmarchBetaResistanceGuard.wits(actor)},context:ctx});
          if(!resolved.valid)throw new Error(`Amnésia: ${resolved.effect}`);
          if(!resolved.targetPatch?.amnesiaLastTurn){resisted.push(recipient.combatant.name);continue;}
          if(recipient.character){
            const updated=MagicCastingService.applyCharacterEffect(recipient.character,resolved);
            await tx.objectStore('characters').put(updated);
            await MagicCastingService.syncActiveEncounters(tx.objectStore('world_state'),updated,resolved,Date.now(),configuredCampaignId||worldStateRecord?.id);
            appliedConsequences.push(`amnesia:${updated.id}`);
          }else{
            const store=tx.objectStore('world_state');
            const raw=await store.get(worldStateRecord.id);
            const world=WorldStateService.normalize(raw,worldStateRecord.id);
            if(!world.encounter||world.encounter.status!=='active'||!world.encounter.combatants.some(c=>c.id===recipient.combatant.id))
              throw new Error('O encontro mudou antes de aplicar Amnésia; PFV preservados.');
            const combatants=world.encounter.combatants.map(c=>c.id!==recipient.combatant.id?c:{...c,
              statusEffects:[...(c.statusEffects||[]).filter(e=>e.id!=='magic:bm-amnesia'),
                {id:'magic:bm-amnesia',name:'Amnésia',detail:JSON.stringify({patch:resolved.targetPatch})}]});
            await store.put({...world,encounter:{...world.encounter,combatants,updatedAt:Date.now()},updatedAt:Date.now()});
            appliedConsequences.push(`amnesia:${recipient.combatant.id}`);
          }
          forgotten.push(recipient.combatant.name);
        }
        effectResult={...effectResult,effect:`${effectResult.effect} Outros alvos afetados: ${forgotten.join(', ')||'nenhum'}; resistiram: ${resisted.join(', ')||'nenhum'}.`};
      }

      // R46: roll ALL resistances and commit sleep markers, character sheets,
      // NPC encounter state and event in the SAME transaction. A reverse,
      // banishment or unintended-target mishap never applies a phantom sleep.
      if(sleepSpell&&effectResult?.valid&&!effectResult.requiresGm&&
          !['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const sleeping=new Map<string,ReturnType<typeof BloodmarchBetaSleepEngine.status>>();
        const chantId=input.spellId==='bm-lullaby'?crypto.randomUUID():undefined;
        const resisted:string[]=[];
        for(const item of sleepTargetCombatants){
          const actor=sleepActors.get(item.id)!;
          const roll=ForbiddenLandsDiceEngine.roll({
            base:BloodmarchBetaResistanceGuard.wits(actor),
            skill:BloodmarchBetaResistanceGuard.insight(actor),gear:0,modifier:-result.power},
            {characterId:item.kind==='character'?item.entityId:null,
              sceneId:activeEncounter.startedSceneId||activeEncounter.id,
              checkId:`bloodmarch-sleep:${input.spellId}:${character.id}:${item.id}:${Date.now()}`,rng});
          await tx.objectStore('rolls').put(roll);auxiliaryRollIds.push(roll.id);
          if(roll.successes>0){resisted.push(item.name);continue;}
          sleeping.set(item.id,BloodmarchBetaSleepEngine.status(input.spellId as SleepSpellId,character.id,result.power,chantId));
        }
        const charStore=tx.objectStore('characters');
        for(const item of sleepTargetCombatants){
          const effect=sleeping.get(item.id);
          if(!effect||item.kind!=='character'||!item.entityId)continue;
          const before=item.entityId===next.id?next:await charStore.get(item.entityId) as Character|undefined;
          if(!before)throw new Error('Ficha de sono desapareceu durante transação.');
          const updated={...before,statusEffects:[...(before.statusEffects||[]).filter(e=>e.id!==effect.id),effect]};
          if(updated.id===next.id)next=updated;
          await charStore.put(updated);
          appliedConsequences.push(`sleep:${item.entityId}`);
        }
        // Caster must continue singing for one turn. The marker is persisted
        // on BOTH sheet and encounter, so combat actions can interrupt it.
        const chant=input.spellId==='bm-lullaby'&&sleeping.size>0&&chantId
          ?BloodmarchBetaSleepEngine.chantStatus(character.id,chantId):null;
        if(chant){next={...next,statusEffects:[...(next.statusEffects||[]).filter(e=>e.id!==chant.id),chant]};await charStore.put(next);}
        const raw=await tx.objectStore('world_state').get(worldStateRecord.id);
        const world=WorldStateService.normalize(raw,worldStateRecord.id);
        if(!world.encounter||world.encounter.id!==activeEncounter.id||world.encounter.status!=='active')
          throw new Error('Encontro mudou durante conjuração de sono. PFV preservados.');
        const combatants=world.encounter.combatants.map(item=>{
          const effect=sleeping.get(item.id);
          if(!effect&&!(chant&&item.kind==='character'&&item.entityId===character.id))return item;
          return {...item,statusEffects:[...(item.statusEffects||[]).filter(e=>e.id!==effect?.id&&e.id!==chant?.id),
            ...(effect?[effect]:[]),...(chant&&item.kind==='character'&&item.entityId===character.id?[chant]:[])]};
        });
        const ids=Array.from(new Set([...(world.magicCharacterIds||[]),
          ...sleepTargetCombatants.filter(c=>sleeping.has(c.id)&&c.kind==='character'&&c.entityId).map(c=>String(c.entityId)),
          ...(chant?[character.id]:[])]));
        await tx.objectStore('world_state').put({...world,magicCharacterIds:ids,
          encounter:{...world.encounter,combatants,updatedAt:Date.now(),
            log:[...world.encounter.log,{id:crypto.randomUUID(),round:world.encounter.round,
              text:`${input.spellId}: ${sleeping.size} afetado(s), ${resisted.length} resistiu(ram).`,createdAt:Date.now()}].slice(-300)},updatedAt:Date.now()});
        effectResult={...effectResult,effect:`${sleeping.size} afetado(s) por ${input.spellId}; ${resisted.length} resistiu(ram). ${input.spellId==='bm-lullaby'?'Sono começa após um turno.':'Sono imediato.'}`};
        appliedConsequences.push(`sleep-resisted:${resisted.length}`);
      }

      // Sem Rastros: caster status was applied above; additional creatures
      // receive distinct randomized appearances in the SAME transaction.
      // NPCs have no character sheet, so their effect lives in encounter state.
      if(input.spellId==='bm-traceless'&&effectResult?.valid&&!effectResult.requiresGm
          &&effectResult.targetPatch?.tracesAltered===true
          &&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const names:string[]=[character.name];
        for(const recipient of tracelessRecipients){
          const personal:SpellEffectResult={...effectResult,targetPatch:{...effectResult.targetPatch,traceDisguise:randomTraceMask()}};
          if(recipient.character){
            const updated=MagicCastingService.applyCharacterEffect(recipient.character,personal);
            await tx.objectStore('characters').put(updated);
            await MagicCastingService.syncActiveEncounters(tx.objectStore('world_state'),updated,personal,Date.now(),configuredCampaignId||worldStateRecord?.id);
            names.push(updated.name);
            appliedConsequences.push(`traceless:${updated.id}`);
          }else if(recipient.combatant){
            const npc=recipient.combatant;
            const raw=await tx.objectStore('world_state').get(worldStateRecord.id);
            const currentWorld=WorldStateService.normalize(raw,worldStateRecord.id);
            if(!currentWorld.encounter||currentWorld.encounter.status!=='active'||!currentWorld.encounter.combatants.some(c=>c.id===npc.id))
              throw new Error('Encontro mudou durante Sem Rastros; PFV preservados.');
            const stamp=Date.now();
            const combatants=currentWorld.encounter.combatants.map(c=>c.id!==npc.id?c:{...c,statusEffects:[...(c.statusEffects||[]).filter(e=>e.id!=='magic:bm-traceless'),
              {id:'magic:bm-traceless',name:'Sem Rastros',detail:JSON.stringify({patch:personal.targetPatch,duration:personal.duration}),remainingMinutes:result.power*60}]});
            await tx.objectStore('world_state').put({...currentWorld,encounter:{...currentWorld.encounter,combatants,updatedAt:stamp},updatedAt:stamp});
            names.push(npc.name);
            appliedConsequences.push(`traceless:${npc.id}`);
          }
        }
        appliedConsequences.push(`traceless:${character.id}`);
        effectResult={...effectResult,effect:`${effectResult.effect} Destinatários: ${names.join(', ')}.`};
      }

      // Mental Strength is persisted on EACH chosen ally, never automatically
      // on the caster. No effects are granted for a redirected/reversed mishap.
      if(input.spellId==='bm-mental-strength'&&effectResult?.valid&&effectResult.targetPatch?.mentalStrengthPower===result.power
          &&!effectResult.requiresGm&&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        for(const recipient of mentalRecipients){
          const updated=MagicCastingService.applyCharacterEffect(recipient.id===next.id?next:recipient,effectResult);
          if(updated.id===next.id)next=updated;
          else await tx.objectStore('characters').put(updated);
          await MagicCastingService.syncActiveEncounters(tx.objectStore('world_state'),updated,effectResult,Date.now(),configuredCampaignId||worldStateRecord?.id);
          appliedConsequences.push(`mental-strength:${recipient.id}`);
        }
        effectResult={...effectResult,effect:`${effectResult.effect} Protegidos: ${mentalRecipients.map(c=>c.name).join(', ')}.`};
      }

      // R41: distribute protection in one IndexedDB transaction. Mishaps that
      // redirect, reverse or banish are held for the GM, never grant phantom
      // immunity. Each recipient's actual sheet AND active combat status are
      // updated; only the caster pays PFV once.
      if(input.spellId==='bm-fires-friend'&&fireFriendRecipients.length&&effectResult?.valid
          &&!effectResult.requiresGm&&effectResult.targetPatch?.fireImmune===true
          &&!['unintended-target','reversed','banished'].includes(mishap?.code||'')){
        for(const recipient of fireFriendRecipients){
          const updated=MagicCastingService.applyCharacterEffect(recipient,effectResult);
          await tx.objectStore('characters').put(updated);
          await MagicCastingService.syncActiveEncounters(tx.objectStore('world_state'),updated,effectResult,Date.now(),configuredCampaignId||worldStateRecord?.id);
          appliedConsequences.push(`fire-friend:${recipient.id}`);
        }
        effectResult={...effectResult,effect:`${effectResult.effect} Proteção adicional aplicada a ${fireFriendRecipients.map(c=>c.name).join(', ')}.`};
      }

      // R33: resolve all targets of an explicit zone ATOMICALLY with the caster's
      // willpower/mishap, armor wear, status markers and each physical D6 roll.
      // An unintended-target or reversed mishap cannot silently keep the old
      // zone; it is held for the GM rather than applying an incorrect attack.
      if(areaSpell){
        if(mishap?.code==='unintended-target'||mishap?.code==='reversed'){
          pendingConsequences.push('area-target-mishap-needs-explicit-zone-reselection');
          effectResult={...effectResult,requiresGm:true,effect:'Infortúnio: resolução da zona suspensa até o Mestre definir os alvos; nenhum dano aplicado.'};
        }else{
          const main=selectedAreaCombatants.find(item=>item.id===input.effectTargetCharacterId||item.entityId===input.effectTargetCharacterId);
          const areas=selectedAreaCombatants.map(item=>{
            const quick:any=item.quickSheet||{};
            const skillRank=(names:string[])=>{
              const skills=quick.skills||{};
              const key=Object.keys(skills).find(name=>names.some(alias=>normalize(alias)===normalize(name)));
              return Math.max(0,Math.trunc(Number(key?skills[key]:0)||0));
            };
            return {id:item.id,name:item.name,kind:item.kind,status:item.status,
              strength:Number(quick.attributes?.strength?.current??item.currentStrength),
              agility:Number(quick.attributes?.agility?.current??0),armor:Number(item.armor)||0,
              fireImmune:BloodmarchBetaFireGuard.immune(item.statusEffects),
              fireDamageMultiplier:Number(quick.fireDamageMultiplier)||1,
              might:skillRank(['Might','Potência','Potencia']),endurance:skillRank(['Endurance','Resiliência','Resiliencia','Resilience'])};
          });
          const areaAllocation:Record<string,number>={};
          for(const [key,value] of Object.entries(input.areaDamageAllocation||{})){
            const target=selectedAreaCombatants.find(item=>item.id===key||item.entityId===key);
            if(!target)throw new Error(`Distribuição de dano aponta para alvo fora da zona: ${key}`);
            if(areaAllocation[target.id]!==undefined)throw new Error('Distribuição duplicada para um alvo.');
            areaAllocation[target.id]=value;
          }
          const distributed=['br-tornado','br-flood-wave','br-blizzard'].includes(input.spellId)
            ?distributeAreaPower(result.power,areas.map(area=>area.id),areaAllocation):{};
          areaResolution=BitterReachAreaSpellEngine.resolve({spellId:input.spellId as any,power:result.power,
            targets:areas,mainTargetId:main?.id,allocations:distributed,
            snow:Boolean(input.effectContext?.snow||input.effectContext?.availableElement==='snow'),sceneId:activeEncounter.startedSceneId||activeEncounter.id,
            castId:`${character.id}:${Date.now()}`,rng});
          const charStore=tx.objectStore('characters');
          const affected=new Map<string,any>();
          for(const impact of areaResolution.effects){
            const combatant=selectedAreaCombatants.find(item=>item.id===impact.targetId)!;
            if(combatant.kind==='character'&&combatant.entityId){
              const original=combatant.entityId===next.id?next:await charStore.get(combatant.entityId) as Character|undefined;
              if(!original)throw new Error(`Ficha da criatura na zona não encontrada: ${combatant.name}.`);
              const patch:Record<string,unknown>={};
              if(impact.attribute&&impact.damage>0)patch[impact.attribute]=Math.max(0,original.attributes[impact.attribute].current-impact.damage);
              if(impact.cold)patch.conditionsAdd=['Cold'];
              const targetEffect:SpellEffectResult={spellId:input.spellId as any,power:result.power,valid:true,effect:areaResolution.description,
                requiresTarget:true,targetPatch:patch};
              let updated=Object.keys(patch).length?MagicCastingService.applyCharacterEffect(original,targetEffect):original;
              if(impact.burning){const burningDamage=Number(combatant.quickSheet?.fireDamageMultiplier)||1;updated={...updated,statusEffects:[...(updated.statusEffects||[]).filter(e=>e.id!=='burning'),
                {id:'burning',label:'Em chamas',detail:`Bola de Fogo: ${burningDamage} dano de Força por rodada até Movimentação (ação lenta).`,ongoingDamage:burningDamage,ongoingRounds:0,source:'Bola de Fogo'}]};}
              if(impact.prone){updated={...updated,statusEffects:[...(updated.statusEffects||[]).filter(e=>e.id!=='condition:prone'),
                {id:'condition:prone',label:'Caído',detail:'Levantar exige ação rápida.'}]};}
              if(combatant.entityId===next.id)next=updated;
              await charStore.put(updated);
              affected.set(combatant.id,updated);
            }
          }
          const currentRaw=await tx.objectStore('world_state').get(worldStateRecord.id);
          const currentWorld=WorldStateService.normalize(currentRaw,worldStateRecord.id);
          const encounter=currentWorld.encounter;
          if(!encounter||encounter.id!==activeEncounter.id||encounter.status!=='active')throw new Error('Encontro mudou durante a conjuração.');
          const byId=new Map(areaResolution.effects.map(effect=>[effect.targetId,effect]));
          const combatants=encounter.combatants.map(item=>{
            const impact=byId.get(item.id);
            if(!impact)return item;
            const saved=affected.get(item.id) as Character|undefined;
            const previousAgility=Number((item.quickSheet as any)?.attributes?.agility?.current??0);
            const remainingStrength=saved?.attributes.strength.current??(impact.attribute==='strength'?Math.max(0,item.currentStrength-impact.damage):item.currentStrength);
            const remainingAgility=saved?.attributes.agility.current??(impact.attribute==='agility'?Math.max(0,previousAgility-impact.damage):previousAgility);
            const broke=(impact.attribute==='strength'&&remainingStrength===0)||(impact.attribute==='agility'&&remainingAgility===0);
            const conditions=saved?[...saved.conditions]:impact.cold?Array.from(new Set([...item.conditions,'Cold'])):[...item.conditions];
            const statusEffects=[...(item.statusEffects||[])];
            if(impact.burning){const idx=statusEffects.findIndex(e=>e.id==='burning');if(idx>=0)statusEffects.splice(idx,1);const burningDamage=Number(item.quickSheet?.fireDamageMultiplier)||1;
              statusEffects.push({id:'burning',name:'Em chamas',detail:`Bola de Fogo: ${burningDamage} dano/rodada até apagar com Movimentação.`,source:'Bola de Fogo',ongoingDamage:burningDamage,ongoingRounds:0});}
            if(impact.prone&&!statusEffects.some(e=>e.id==='condition:prone'))statusEffects.push({id:'condition:prone',name:'Caído',detail:'Levantar exige ação rápida.'});
            if(impact.cold&&!statusEffects.some(e=>e.id==='magic:br-cold-snap'&&input.spellId==='br-cold-snap'))statusEffects.push({id:`magic:${input.spellId}`,name:spell.name,detail:'Frio aplicado pela magia da zona.'});
            return {...item,currentStrength:remainingStrength,armor:Math.max(0,item.armor-impact.armorLoss),
              conditions,statusEffects,brokenAttribute:saved?.brokenAttribute??(broke?impact.attribute||undefined:item.brokenAttribute),
              status:saved?.status==='dead'?'dead' as const:broke?'broken' as const:item.status,
              quickSheet:{...(item.quickSheet||{}),...(saved?{attributes:saved.attributes,conditions:saved.conditions,brokenAttribute:saved.brokenAttribute}:
                impact.attribute==='agility'?{attributes:{...((item.quickSheet as any)?.attributes||{}),agility:{current:remainingAgility,max:Number((item.quickSheet as any)?.attributes?.agility?.max||remainingAgility)}}}:{}),armor:Math.max(0,item.armor-impact.armorLoss)}};
          });
          const stamp=Date.now();
          const logs=areaResolution.effects.map(impact=>({id:crypto.randomUUID(),round:encounter.round,combatantId:impact.targetId,
            text:`${spell.name}: ${encounter.combatants.find(c=>c.id===impact.targetId)?.name} recebeu ${impact.damage} dano ${impact.attribute||'sem atributo'} (armadura ${impact.armorDice.join(',')||'—'}, desgaste ${impact.armorLoss})${impact.cold?'; Frio':''}${impact.prone?'; Caído':''}${impact.burning?'; Em chamas':''}.`,createdAt:stamp}));
          await tx.objectStore('world_state').put({...currentWorld,encounter:{...encounter,combatants,
            log:[...encounter.log,...logs].slice(-300),updatedAt:stamp},updatedAt:stamp});
          for(const roll of areaResolution.rolls)await tx.objectStore('rolls').put(roll);
          appliedConsequences.push(`area:${areaResolution.effects.length}-targets:${areaResolution.rolls.length}-rolls`);
          effectResult={...effectResult,effect:`${areaResolution.description} ${areaResolution.effects.length} alvo(s), ${areaResolution.rolls.length} rolagem(ns).`};
        }
      }

      // Timed environment persists in world state, rather than a narrative-only result.
      if(['br-cold-snap','br-blizzard'].includes(input.spellId)&&areaResolution&&!['reversed','unintended-target'].includes(mishap?.code||'')){
        const raw=await tx.objectStore('world_state').get(worldStateRecord.id);
        const currentWorld=WorldStateService.normalize(raw,worldStateRecord.id);
        const timed=BitterReachMagicTimeEngine.addEnvironment(currentWorld,input.spellId as 'br-cold-snap'|'br-blizzard',character.id,activeEncounter.id,result.power);
        await tx.objectStore('world_state').put(timed);
        appliedConsequences.push(`environment:${input.spellId}:${input.spellId==='br-cold-snap'?15:result.power*15}-minutes`);
      }

      // The Bitter Reach pp.25–26: summon/banish is committed with PFV and spell log.
      if(input.spellId==='br-summon-elemental'&&!['reversed','unintended-target'].includes(mishap?.code||'')){
        const chosen=String(input.effectContext?.availableElement||'');
        if(!BitterReachElementalEngine.isElement(chosen))throw new Error('Escolha Fogo, Água, Terra ou Vento.');
        if(!worldStateRecord)throw new Error('Invocar Elemental exige um mundo ativo.');
        const currentWorld=WorldStateService.normalize(await tx.objectStore('world_state').get(worldStateRecord.id),worldStateRecord.id);
        const encounter=currentWorld.encounter;
        if(!encounter||encounter.status!=='active')throw new Error('Invocar Elemental exige encontro ativo para criar combatente real.');
        const caster=encounter.combatants.find(c=>c.entityId===character.id&&c.status==='active');
        if(!caster||next.attributes.strength.current<=0)throw new Error('Conjurador precisa estar ativo e não Quebrado no encontro.');
        if(encounter.combatants[encounter.turnIndex]?.id!==caster.id)throw new Error('Invocar ou banir elemental só pode ser conjurado no próprio turno.');
        if(!caster.slowActionAvailable)throw new Error('Invocar ou banir elemental exige ação lenta disponível.');
        const banishTargetId=String(input.effectContext?.banishTargetId||'');
        let combatants=encounter.combatants;
        if(banishTargetId){
          const target=combatants.find(c=>c.id===banishTargetId);
          if(!target?.summonedElemental||target.summonedElemental.casterId===character.id)throw new Error('Banimento exige elemental de outro conjurador.');
          const remaining=Math.max(0,target.currentStrength-result.power);
          combatants=remaining===0?combatants.filter(c=>c.id!==target.id):combatants.map(c=>c.id===target.id?{...c,currentStrength:remaining,quickSheet:{...c.quickSheet,attributes:{...((c.quickSheet?.attributes||{}) as any),strength:{current:remaining,max:target.maxStrength}}}}:c);
          appliedConsequences.push(`banish:${target.id}:${result.power}`);
          effectResult={...effectResult,effect:`Banimento: ${result.power} dano direto em ${target.name}${remaining===0?'; dissipado':''}.`};
        }else{
          if(BitterReachElementalEngine.existing(encounter,character.id))throw new Error('O conjurador só pode controlar um elemental ao mesmo tempo.');
          const summoned=BitterReachElementalEngine.create({kind:chosen,power:result.power,casterId:character.id,casterName:character.name,casterSide:caster.side,nowMinute:Number(currentWorld.magicElapsedMinutes||0)});
          combatants=[...combatants,summoned].sort((a,b)=>a.initiative-b.initiative||a.name.localeCompare(b.name,'pt-BR'));
          appliedConsequences.push(`summoned-elemental:${summoned.id}:${chosen}`);
          effectResult={...effectResult,effect:`${summoned.name} invocado: Força ${summoned.currentStrength}, Armadura ${summoned.armor}, 15 minutos; comando gratuito no turno do conjurador.`};
        }
        combatants=combatants.map(c=>c.id===caster.id?{...c,slowActionAvailable:false}:c);
        const turnActor=encounter.combatants[encounter.turnIndex];
        const turnIndex=Math.max(0,combatants.findIndex(c=>c.id===turnActor?.id));
        const stamp=Date.now();
        await tx.objectStore('world_state').put({...currentWorld,encounter:{...encounter,combatants,turnIndex,
          log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:caster.id,text:effectResult.effect,createdAt:stamp}].slice(-300),updatedAt:stamp},updatedAt:stamp});
      }

      // Handle demon spawning in active encounter
      if (mishap?.code === 'demon' && activeEncounter && worldStateRecord) {
        const demonWorld=WorldStateService.normalize(await tx.objectStore('world_state').get(worldStateRecord.id),worldStateRecord.id);
        const demonEncounter=demonWorld.encounter;
        if(!demonEncounter||demonEncounter.status!=='active')throw new Error('Encontro não está mais ativo para o infortúnio demoníaco.');
        const demonCombatant = {
          id: `demon:${crypto.randomUUID()}`,
          name: 'Demônio Invocado',
          kind: 'monster',
          side: 'enemy',
          initiative: 10,
          currentStrength: 6,
          maxStrength: 6,
          armor: 4,
          conditions: [],
          fastActionAvailable: true,
          slowActionAvailable: true,
          status: 'active',
          notes: 'Invocado por infortúnio mágico.',
          quickSheet: {
            attributes: { strength: { current: 6, max: 6 } }
          }
        };
        const updatedCombatants = [...demonEncounter.combatants, demonCombatant];
        await tx.objectStore('world_state').put({
          ...demonWorld,
          encounter: {
            ...demonEncounter,
            combatants: updatedCombatants,
            log: [...demonEncounter.log, { id: crypto.randomUUID(), round: demonEncounter.round, text: `Um Demônio Invocado surgiu e atacará o grupo!`, createdAt: Date.now() }].slice(-300),
            updatedAt: Date.now()
          },
          updatedAt: Date.now()
        });
      }

      if(mishap&&next.id!==effectTarget?.id)await MagicCastingService.syncActiveEncounters(tx.objectStore('world_state'),next,undefined,Date.now(),configuredCampaignId||worldStateRecord?.id);
      if (['healing-hands','nature-cure','purify-spirit'].includes(input.spellId)) {
        next = {
          ...next,
          magicMishapState: undefined,
          statusEffects: (next.statusEffects || []).filter(e => !e.id.startsWith('magic-mishap:'))
        };
      }
      const casterWillpowerDelta=Number(effectResult?.targetPatch?.castersWillpowerDelta)||0;
      const temporaryWillpower=Boolean(effectResult?.duration?.includes('próxima rodada'));
      const willpowerGain=temporaryWillpower?0:Math.max(0,Math.trunc(Number(effectResult?.willpowerGain)||0));
      if(casterWillpowerDelta||willpowerGain)next={...next,willpowerPoints:WillpowerRulesEngine.change(next.willpowerPoints,casterWillpowerDelta+willpowerGain)};
      // A caster outside combat must still have timed magic expire, without
      // accidentally advancing every character belonging to other campaigns.
      // Register only real, successfully affected sheets on the active world.
      if(effectResult?.valid&&!effectResult.requiresGm&&!['reversed','unintended-target','banished'].includes(mishap?.code||'')
          &&parseDurationToQuartersOrMinutes(effectResult.duration,result.power).remainingMinutes){
        const affectedIds=new Set<string>();
        for(const member of [next,effectTarget,...fireFriendRecipients,...mentalRecipients,
          ...tracelessRecipients.map(recipient=>recipient.character).filter((value):value is Character=>Boolean(value))]){
          if(member?.statusEffects?.some(e=>e.id===`magic:${input.spellId}`&&typeof e.remainingMinutes==='number'&&e.remainingMinutes>0))
            affectedIds.add(member.id);
        }
        if(affectedIds.size){
          const activeId=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
          const worldsNow=await tx.objectStore('world_state').getAll();
          const chosen=worldsNow.find(world=>world.id===activeId)||(worldsNow.length===1?worldsNow[0]:undefined);
          if(chosen){
            const ids=Array.from(new Set([...(Array.isArray(chosen.magicCharacterIds)?chosen.magicCharacterIds:[]),...affectedIds]));
            await tx.objectStore('world_state').put({...chosen,magicCharacterIds:ids,updatedAt:Date.now()});
          }
        }
      }
      if(materialPlan && materialWorld && effectResult?.valid && !effectResult.requiresGm
          && !['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const currentWorld=await tx.objectStore('world_state').get(materialWorld.id);
        if(!currentWorld)throw Error('Mundo material desapareceu; custo de magia revertido.');
        const changed=BloodmarchBetaMaterialWorldEngine.apply(WorldStateService.normalize(currentWorld,materialWorld.id),materialPlan);
        await tx.objectStore('world_state').put(changed);
        appliedConsequences.push(`world-material:${materialPlan.siteId}:${materialPlan.after}`);
      }else if(materialPlan){pendingConsequences.push('world-material-effect-suspended-by-mishap');}
      if(input.spellId!=='bm-fata-morgana'&&configuredCampaignId){
        const rawConcentration=await tx.objectStore('world_state').get(configuredCampaignId);
        if(rawConcentration){
          const present=WorldStateService.normalize(rawConcentration,configuredCampaignId);
          if((present.magicMentalEffects||[]).some(e=>e.spellId==='bm-fata-morgana'&&e.casterId===character.id)){
            await tx.objectStore('world_state').put(BloodmarchBetaMentalSceneEngine.breakConcentration(present,character.id));
            appliedConsequences.push('fata-morgana:concentration-ended-by-other-spell');
          }
        }
      }
      if(mentalPlan&&mentalWorld&&effectResult?.valid&&!effectResult.requiresGm
          &&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const stored=await tx.objectStore('world_state').get(mentalWorld.id);
        if(!stored)throw Error('Mundo mental desapareceu, lançamento revertido.');
        const updated=BloodmarchBetaMentalSceneEngine.apply(BloodmarchBetaMentalSceneEngine.breakConcentration(WorldStateService.normalize(stored,mentalWorld.id),character.id),mentalPlan);
        await tx.objectStore('world_state').put(updated);
        appliedConsequences.push(`mental-scene:${mentalPlan.spellId}:${mentalPlan.locationId}`);
        if(mentalPlan.spellId==='bm-truth-sense')pendingConsequences.push('Mestre deve revelar uma verdade real por nível de poder; não gerar segredos automaticamente.');
        if(mentalPlan.spellId==='bm-wordplay')pendingConsequences.push('A fala é reinterpretada; decisões/ações posteriores não são impostas automaticamente.');
        if(mentalPlan.spellId==='bm-fata-morgana')pendingConsequences.push('Concentração deve cessar em qualquer outra ação, toque revela a ilusão.');
      }else if(mentalPlan){pendingConsequences.push('mental-scene-effect-suspended-by-mishap');}
      if(activeWorldPlan&&activeSpellWorld&&effectResult?.valid&&!effectResult.requiresGm
          &&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const raw=await tx.objectStore('world_state').get(activeSpellWorld.id);
        if(!raw)throw Error('Mundo de magia Bloodmarch desapareceu; PFV revertidos.');
        const changed=BloodmarchBetaActiveWorldEngine.apply(WorldStateService.normalize(raw,activeSpellWorld.id),activeWorldPlan);
        await tx.objectStore('world_state').put(changed);
        appliedConsequences.push(`active-world:${activeWorldPlan.spellId}:${activeWorldPlan.locationId}`);
        if(activeWorldPlan.spellId==='bm-dream-visit')pendingConsequences.push('Visita Onírica permite observar o destino conhecido; informações secretas exigem confirmação do Mestre, sem ação física.');
      }else if(activeWorldPlan){pendingConsequences.push('active-world-suspended-by-mishap');}
      if(directivePlan&&directiveWorld&&effectResult?.valid&&!effectResult.requiresGm
          &&!['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const raw=await tx.objectStore('world_state').get(directiveWorld.id);
        if(!raw)throw Error('Mundo da diretiva desapareceu: PFV revertidos.');
        const updated=BloodmarchBetaDreamDirectiveEngine.apply(WorldStateService.normalize(raw,directiveWorld.id),directivePlan);
        await tx.objectStore('world_state').put(updated);
        if(directivePlan.geasResistanceRoll)await tx.objectStore('rolls').put(directivePlan.geasResistanceRoll);
        appliedConsequences.push(`directive:${directivePlan.spellId}`);
        if(directivePlan.spellId==='bm-geas'&&!directivePlan.geasResisted)pendingConsequences.push('Geas: a obrigação deve influenciar a decisão da vítima, sem executar ações automaticamente.');
        if(directivePlan.spellId==='bm-premonition')pendingConsequences.push('Premonição: a rolagem antecipada requer decisão do jogador antes de aplicar consequências.');
        if(directivePlan.spellId==='bm-restorative-sleep')pendingConsequences.push('Sono Restaurador: dados temporários aplicados via cálculo do pool, não alteram ficha base.');
      }else if(directivePlan)pendingConsequences.push('directive-suspended-by-mishap');
      if(fireWyrmPlan&&fireWyrmWorld&&effectResult?.valid&&!effectResult.requiresGm&&
          !['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const stored=await tx.objectStore('world_state').get(fireWyrmWorld.id);
        if(!stored)throw Error('Serpe: mundo ausente; PFV revertidos.');
        const changed=BloodmarchBetaFireWyrmEngine.apply(WorldStateService.normalize(stored,fireWyrmWorld.id),fireWyrmPlan);
        await tx.objectStore('world_state').put(changed);
        next=BloodmarchBetaFireWyrmEngine.consumeSpear(next,fireWyrmPlan);
        next={...next,encumbrance:EncumbranceRulesEngine.calculate(next)};
        appliedConsequences.push('fire-wyrm:spear-broken:pending-15-minutes');
        pendingConsequences.push('Serpe chega após 15 minutos, não imediatamente; ataca todos à vista menos a conjuradora; GM confirma tamanho.');
      }else if(fireWyrmPlan)pendingConsequences.push('fire-wyrm-suspended-by-mishap');
      if(maraPlan&&maraWorld&&effectResult?.valid&&!effectResult.requiresGm&&
          !['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const raw=await tx.objectStore('world_state').get(maraWorld.id);
        if(!raw)throw Error('Mundo da Mara desapareceu antes do gasto de PFV.');
        await tx.objectStore('world_state').put(BloodmarchBetaMaraEngine.apply(WorldStateService.normalize(raw,maraWorld.id),maraPlan));
        appliedConsequences.push('mara:perseguição registrada por um Quarto de Dia');
        pendingConsequences.push('Mara somente ataca vítima adormecida, enquanto existir contato e posição válida; ataque não é automático na conjuração.');
      }else if(maraPlan)pendingConsequences.push('mara-suspended-by-mishap');
      if(dreamTravelPlan&&dreamTravelWorld&&effectResult?.valid&&!effectResult.requiresGm&&
          !['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const stored=await tx.objectStore('world_state').get(dreamTravelWorld.id);
        if(!stored)throw Error('Mundo de Viagem dos Sonhos ausente; PFV revertidos.');
        const changed=BloodmarchBetaDreamTravelEngine.apply(WorldStateService.normalize(stored,dreamTravelWorld.id),dreamTravelPlan);
        await tx.objectStore('world_state').put(changed);
        appliedConsequences.push('dream-travel:pending-sleep');
        pendingConsequences.push('O grupo só viaja depois de dormir um Quarto de Dia válido; animais e bagagem volumosa não acompanham.');
      }else if(dreamTravelPlan)pendingConsequences.push('dream-travel-suspended-by-mishap');
      if(propheticPlan&&propheticWorld&&effectResult?.valid&&!effectResult.requiresGm&&
          !['reversed','unintended-target','banished'].includes(mishap?.code||'')){
        const stored=await tx.objectStore('world_state').get(propheticWorld.id);
        if(!stored)throw Error('Mundo profético sumiu antes da gravação: PFV revertidos.');
        const changed=BloodmarchBetaPropheticDreamsEngine.apply(WorldStateService.normalize(stored,propheticWorld.id),propheticPlan);
        await tx.objectStore('world_state').put(changed);
        next=BloodmarchBetaPropheticDreamsEngine.consumeBone(next);
        appliedConsequences.push('prophetic:bone-consumed-and-effect-pending');
        pendingConsequences.push('Sonho aguarda uma atividade oficial de Dormir bem-sucedida; sem item ou cura antecipada.');
      }else if(propheticPlan)pendingConsequences.push('prophetic-suspended-by-mishap');
      const now=Date.now();
      if(effectResult?.requiresGm)pendingConsequences.push('spell-effect-requires-gm');
      const entry={id:crypto.randomUUID(),characterId:character.id,spellId:input.spellId,spellName:spell.name,discipline:spell.discipline,sourcePage:spell.page,spellRank:result.spellRank,talentName:talentKey||input.talentName,result,effectResult,resistanceRoll:resistanceRollDetail,effectTargetCharacterId:effectTarget?.id || effectCombatant?.id,areaResults:areaResolution?{effects:areaResolution.effects,rollIds:areaResolution.rolls.map(roll=>roll.id)}:undefined,auxiliaryRollIds,appliedConsequences,pendingConsequences,sessionId:input.sessionId,createdAt:now};
      const metadata=await tx.objectStore('app_meta').get(eventId(character.id));
      const oldEvents=Array.isArray(metadata?.value?.events)?metadata.value.events:[];
      await tx.objectStore('characters').put(next);
      await tx.objectStore('app_meta').put({id:eventId(character.id),value:{events:[...oldEvents,entry].slice(-500)},updatedAt:now});
      return {character:next,event:entry};
    });
  }

  /** Retry a previously paid-for Wyrm arrival without paying PFV or breaking
   * another spear. Every decision is scoped to the active encounter and time. */
  static async manifestBloodmarchFireWyrm(input:{campaignId:string;casterId:string;effectId:string}){
    return withTransaction(['app_meta','campaign','world_state','characters'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const campaign=await tx.objectStore('campaign').get(input.campaignId);
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      if(!active||active!==input.campaignId||!campaign||!caster||caster.campaignId!==active||
        campaign.currentHexId==='')throw Error('Manifestação da serpe fora da campanha ativa.');
      const raw=await tx.objectStore('world_state').get(active);
      if(!raw)throw Error('Mundo de serpe ausente.');
      const world=WorldStateService.normalize(raw,active);
      const effect=BloodmarchBetaFireWyrmEngine.effects(world).find(e=>e.id===input.effectId&&e.casterId===caster.id);
      if(!effect||campaign.currentHexId!==effect.hexId)throw Error('Serpe não pertence ao hex da campanha ativa.');
      const changed=BloodmarchBetaFireWyrmEngine.manifest(world,effect.id);
      await tx.objectStore('world_state').put(changed);return changed;
    });
  }

  /** Cast consequences and later manifestations never share an unverified, global world. */
  static async manifestBloodmarchMara(input:{campaignId:string;casterId:string;effectId:string;victimSleeping:boolean}){
    return withTransaction(['app_meta','campaign','world_state','characters'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const campaign=await tx.objectStore('campaign').get(input.campaignId);
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      if(!active||active!==input.campaignId||!campaign||!caster||caster.campaignId!==active)
        throw Error('Manifestação de Mara fora da campanha ativa.');
      const raw=await tx.objectStore('world_state').get(active);if(!raw)throw Error('Mundo de Mara ausente.');
      const world=WorldStateService.normalize(raw,active);
      const effect=BloodmarchBetaMaraEngine.effects(world).find(e=>e.id===input.effectId&&e.casterId===caster.id);
      if(!effect||campaign.currentHexId!==effect.originHexId)throw Error('Mara não pertence a este local/campanha.');
      const changed=BloodmarchBetaMaraEngine.manifest(world,{effectId:effect.id,victimSleeping:input.victimSleeping});
      await tx.objectStore('world_state').put(changed);return changed;
    });
  }
  static async endBloodmarchMaraContact(input:{campaignId:string;casterId:string;effectId:string}){
    return withTransaction(['app_meta','characters','world_state'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      if(active!==input.campaignId||!caster||caster.campaignId!==active)throw Error('Contato fora da campanha.');
      const store=tx.objectStore('world_state');const raw=await store.get(active);
      if(!raw)throw Error('Mundo não encontrado.');
      const changed=BloodmarchBetaMaraEngine.endObservation(WorldStateService.normalize(raw,active),input);
      await store.put(changed);return changed;
    });
  }
  static async recordBloodmarchMaraObservation(input:{campaignId:string;casterId:string;effectId:string;observationId:string;text:string;gmConfirmed:boolean;casterStillDreaming:boolean}){
    return withTransaction(['app_meta','characters','world_state'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      const caster=await tx.objectStore('characters').get(input.casterId) as Character|undefined;
      if(active!==input.campaignId||!caster||caster.campaignId!==active)throw Error('Observação fora da campanha.');
      const store=tx.objectStore('world_state');const raw=await store.get(active);
      if(!raw)throw Error('Mundo da Mara não encontrado.');
      const changed=BloodmarchBetaMaraEngine.observe(WorldStateService.normalize(raw,active),input);
      await store.put(changed);return changed;
    });
  }

  /** GM-approved disclosure spends one actual Truth Sense charge in this campaign. */
  static async recordTruthSenseRevelation(input:{campaignId:string;casterCharacterId:string;effectId:string;fact:string;gmConfirmed:boolean}){
    return withTransaction(['characters','world_state','app_meta'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      if(active!==input.campaignId)throw Error('Outra campanha está ativa.');
      const caster=await tx.objectStore('characters').get(input.casterCharacterId) as Character|undefined;
      if(!caster||caster.campaignId!==active)throw Error('Conjuradora não pertence à campanha.');
      const store=tx.objectStore('world_state');const raw=await store.get(active);
      if(!raw)throw Error('Mundo desta campanha ausente.');
      const current=WorldStateService.normalize(raw,active);
      const updated=BloodmarchBetaMentalSceneEngine.recordTruth(current,{effectId:input.effectId,casterId:caster.id,fact:input.fact,gmConfirmed:input.gmConfirmed});
      await store.put(updated);return updated;
    });
  }
  /** Explicit verified physical contact: all viewers recognize the illusion; no resistance roll. */
  static async revealFataMorganaOnTouch(input:{campaignId:string;actorCharacterId:string;effectId:string;contactConfirmed:boolean}){
    if(input.contactConfirmed!==true)throw Error('Toque físico precisa ser confirmado antes de revelar a ilusão.');
    return withTransaction(['characters','world_state','app_meta'],'readwrite',async tx=>{
      const active=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      if(active!==input.campaignId)throw Error('Outra campanha está ativa.');
      const actor=await tx.objectStore('characters').get(input.actorCharacterId) as Character|undefined;
      if(!actor||actor.campaignId!==active)throw Error('Personagem não pertence à campanha.');
      const store=tx.objectStore('world_state');const raw=await store.get(active);
      if(!raw)throw Error('Mundo desta campanha ausente.');
      const current=WorldStateService.normalize(raw,active);
      const updated=BloodmarchBetaMentalSceneEngine.touchIllusion(current,input.effectId);
      await store.put(updated);return updated;
    });
  }
  /** Explicit wake action. Sand Sleep permits ONLY its original caster. */
  static async wakeSleepingTarget(input:{campaignId:string;actorCharacterId:string;targetCombatantId:string}){
    return withTransaction(['characters','world_state','app_meta'],'readwrite',async tx=>{
      const configured=String((await tx.objectStore('app_meta').get('activeCampaignId'))?.value||'');
      if(!configured||configured!==input.campaignId)throw Error('Campanha ativa não corresponde ao encontro de despertar.');
      const actor=await tx.objectStore('characters').get(input.actorCharacterId) as Character|undefined;
      if(!actor||actor.status==='dead')throw Error('A pessoa que desperta está ausente ou morta.');
      if(BloodmarchBetaSleepEngine.asleep(actor.statusEffects))throw Error('Pessoa adormecida não pode despertar outras criaturas.');
      const raw=await tx.objectStore('world_state').get(input.campaignId);
      if(!raw)throw Error('Campanha não encontrada.');
      const world=WorldStateService.normalize(raw,input.campaignId);
      const encounter=world.encounter;
      if(!encounter||encounter.status!=='active')throw Error('Nenhum encontro ativo para despertar.');
      const actors=encounter.combatants.filter(c=>c.kind==='character'&&c.entityId===actor.id&&['active','broken'].includes(c.status));
      if(actors.length!==1)throw Error('Pessoa que desperta não está presente no encontro.');
      const matches=encounter.combatants.filter(c=>c.id===input.targetCombatantId&&['active','broken'].includes(c.status));
      if(matches.length!==1)throw Error('Pessoa adormecida ausente ou ambígua.');
      const target=matches[0];
      const effect=target.statusEffects?.find(e=>BloodmarchBetaSleepEngine.active(e));
      if(!effect)throw Error('Alvo não está adormecido por magia ativa.');
      if(!BloodmarchBetaSleepEngine.mayWake(effect,input.actorCharacterId))
        throw Error('Sono de Areia: somente a conjuradora original pode despertar a vítima.');
      if(target.kind==='character'&&target.entityId){
        const sheet=await tx.objectStore('characters').get(target.entityId) as Character|undefined;
        if(!sheet)throw Error('Ficha adormecida ausente.');
        await tx.objectStore('characters').put({...sheet,statusEffects:(sheet.statusEffects||[]).filter(e=>e.id!==effect.id)});
      }
      const now=Date.now();
      const combatants=encounter.combatants.map(c=>c.id===target.id?{...c,statusEffects:(c.statusEffects||[]).filter(e=>e.id!==effect.id)}:c);
      await tx.objectStore('world_state').put({...world,encounter:{...encounter,combatants,
        log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,text:`${actor.name} despertou ${target.name}.`,createdAt:now}].slice(-300),updatedAt:now},updatedAt:now});
      return {actorId:actor.id,targetId:target.id,spellId:effect.id};
    });
  }

  private static applyCharacterEffect(character:Character,effect:SpellEffectResult):Character {
    const patch=effect.targetPatch||{}; let next={...character};
    const attributes={...next.attributes};
    for(const key of ['strength','agility','wits','empathy'] as const){
      const value=patch[key]; if(typeof value==='number')attributes[key]={...attributes[key],current:Math.max(0,Math.min(attributes[key].max,Math.trunc(value)))};
      const maxKey=`${key}Max`; const maxValue=patch[maxKey];
      if(typeof maxValue==='number')attributes[key]={...attributes[key],max:Math.max(0,Math.trunc(maxValue)),current:Math.min(attributes[key].current,Math.max(0,Math.trunc(maxValue)))};
    }
    next={...next,attributes};
    const addedConditions=(Array.isArray(patch.conditionsAdd)?patch.conditionsAdd:[]).filter((condition):condition is Character['conditions'][number]=>['Starving','Thirsty','Sleep Deprived','Cold','Sick','Drowning','Broken','Lice'].includes(String(condition)));
    if(addedConditions.length)next={...next,conditions:Array.from(new Set([...next.conditions,...addedConditions]))};
    if(attributes.wits.current>0&&next.statusEffects?.some(value=>value.id.startsWith('monster-catatonic:')))next={...next,statusEffects:next.statusEffects.filter(value=>!value.id.startsWith('monster-catatonic:'))};
    if(Number(effect.healing)>0)next={...next,statusEffects:(next.statusEffects||[]).filter(value=>!value.id.startsWith('monster-eggs:'))};
    if(Number(effect.healing)>0||patch.diseaseState===null)next={...next,statusEffects:(next.statusEffects||[]).filter(value=>!value.id.startsWith('monster-plague-boils:'))};
    if(['healing-hands','nature-cure','close-wounds'].includes(effect.spellId))next={...next,statusEffects:(next.statusEffects||[]).filter(value=>!value.id.startsWith('monster-otherworldly-hunger:')&&!value.id.startsWith('monster-otherworldly-thirst:'))};
    if(next.basiliskWarts&&['healing-hands','nature-cure','purify-spirit','close-wounds'].includes(effect.spellId))next=BasiliskWartsRulesEngine.cure(next);
    if(next.statusEffects?.some(value=>value.id==='monster-petrified')&&['healing-hands','nature-cure','close-wounds'].includes(effect.spellId))next={...next,statusEffects:next.statusEffects.filter(value=>value.id!=='monster-petrified')};
    
    // Healing magic cures magic mishaps (blindness, insomnia, contagion)
    if (['healing-hands','nature-cure','purify-spirit'].includes(effect.spellId)) {
      next = {
        ...next,
        magicMishapState: undefined,
        statusEffects: (next.statusEffects || []).filter(e => !e.id.startsWith('magic-mishap:'))
      };
    }

    const clearedBroken=next.brokenAttribute&&attributes[next.brokenAttribute].current>0;
    if(clearedBroken){
      next={...next,brokenAttribute:undefined,conditions:next.conditions.filter(value=>value!=='Broken')};
    }
    if(typeof patch.willpowerDelta==='number')next={...next,willpowerPoints:WillpowerRulesEngine.change(next.willpowerPoints,patch.willpowerDelta)};
    const newlyBroken=(['strength','agility','wits','empathy'] as const).find(key=>attributes[key].current===0&&character.attributes[key].current>0);
    if(newlyBroken)next={...next,brokenAttribute:newlyBroken,conditions:Array.from(new Set([...next.conditions,'Broken']))};
    if(patch.diseaseState===null)next={...next,diseaseState:undefined,conditions:next.conditions.filter(value=>value!=='Sick')};
    else if(typeof patch.diseaseVirulence==='number'){
      const virulence=Math.max(1,Math.min(30,Math.trunc(patch.diseaseVirulence)));
      next=DiseaseRulesEngine.expose(next,virulence,0,1);
    }
    if(patch.poisonState===null)next={...next,poisonState:undefined};
    if(patch.status==='dead'||patch.dead===true)next={...next,status:'dead'};
    if(patch.status==='alive'||patch.dead===false)next={...next,status:'alive'};
    if(patch.stopFatalBleeding===true){
      // The Bloodmarch BETA p.37: saves from a fatal injury, NOT healing the injury.
      // Preserve critical effects and healing clock, only cancel the death countdown.
      next={...next,criticalInjuryStates:(next.criticalInjuryStates||[]).map(injury=>
        injury.id===patch.stopFatalBleedingInjuryId&&injury.lethal&&(injury.status==='active'||injury.status==='treated')
          ?{...injury,status:'treated' as const,deathTimer:null,deathTimeRemaining:null}:injury),
        statusEffects:(next.statusEffects||[]).filter(effect=>!effect.id.startsWith('bitter-harpoon-bleed:'))};
    }
    if(patch.criticalInjury&&typeof patch.criticalInjury==='object'){
      const changed=patch.criticalInjury as {id?:string;status?:string};
      next={...next,criticalInjuryStates:(next.criticalInjuryStates||[]).map(state=>state.id===changed.id&&changed.status==='healed'?{...state,status:'healed' as const}:state)};
    }
    const effectDetail=JSON.stringify({patch,duration:effect.duration,temporaryWillpower:effect.willpowerGain&&effect.duration?.includes('próxima rodada')?effect.willpowerGain:undefined});
    const directKeys=new Set(['strength','agility','wits','empathy','strengthMax','agilityMax','witsMax','empathyMax','diseaseState','diseaseVirulence','diseasePhase','poisonState','criticalInjury','status','dead','stopFatalBleeding','stopFatalBleedingInjuryId']);
    const persistentPatch=Object.fromEntries(Object.entries(patch).filter(([key])=>!directKeys.has(key)));
    if(Object.keys(persistentPatch).length||Boolean(effect.willpowerGain&&effect.duration?.includes('próxima rodada'))){
      const statusEffects=(next.statusEffects||[]).filter(item=>item.id!==`magic:${effect.spellId}`);
      // Parse continuous durations
      const parsedTimes = parseDurationToQuartersOrMinutes(effect.duration, effect.power);
      next={...next,statusEffects:[...statusEffects,{id:`magic:${effect.spellId}`,label:effect.spellId,detail:effectDetail, ...parsedTimes,
        ...(String(effect.spellId)==='br-suffocate'?{ongoingRounds:Number(patch.ongoingRounds)||effect.power,ongoingDamage:1,speechBlocked:true}:{}),
        ...(typeof patch.incomingMetalAttackModifier==='number'?{incomingMetalAttackModifier:Number(patch.incomingMetalAttackModifier)}:{}),
        ...(typeof patch.mentalStrengthPower==='number'?{mentalStrengthPower:Number(patch.mentalStrengthPower)}:{}),
        ...(typeof patch.remainingRounds==='number'?{remainingRounds:Number(patch.remainingRounds)}:{}),
        ...(patch.bmConfusion?{remainingRounds:Number((patch.bmConfusion as any).remainingRounds)||effect.power,confusionRecoveryTo:Number((patch.bmConfusion as any).recoveryTo)||0}:{}),
      }]};
      if(patch.burning)next={...next,statusEffects:[...(next.statusEffects||[]).filter(item=>item.id!=='burning'),
        {id:'burning',label:'Em chamas',source:effect.spellId,ongoingDamage:1,ongoingRounds:0,detail:'1 dano de Força por rodada até apagar com Movimentação.'}]};
    }
    return next;
  }

  private static withDirectDamage(target:Character,effect:SpellEffectResult):SpellEffectResult {
    const patch={...(effect.targetPatch||{})};
    const amount=Math.max(0,Math.trunc(Number(effect.damage)||0));
    const attributes=target.attributes;
    const apply=(key:'strength'|'agility'|'wits'|'empathy',damage:number)=>{
      if(key in patch)return;
      (patch as Record<string,unknown>)[key]=Math.max(0,attributes[key].current-Math.max(0,Math.trunc(damage)));
    };
    const damageAttribute=patch.damageAttribute;
    if(['strength','agility','wits','empathy'].includes(String(damageAttribute)))apply(damageAttribute as 'strength'|'agility'|'wits'|'empathy',amount);
    else if(['bear-claw','rockstorm','immolate','hand-of-doom','earthquake'].includes(effect.spellId))apply('strength',amount);
    else if(effect.spellId==='grave-shiver'){apply('strength',1);apply('wits',1);}
    return {...effect,targetPatch:patch};
  }

  private static async syncActiveEncounters(store:any,target:Character,effect:SpellEffectResult|undefined,updatedAt:number,campaignId?:string) {
    const worlds=await store.getAll();
    for(const raw of worlds){
      if (!campaignId || raw.id!==campaignId) continue;
      const world=WorldStateService.normalize(raw,raw.id);
      const encounter=world.encounter;
      if(!encounter||encounter.status!=='active'||!encounter.combatants.some(item=>item.kind==='character'&&item.entityId===target.id))continue;
      const patch=effect?.targetPatch||{};
      const combatants=encounter.combatants.map(item=>{
        if(item.kind!=='character'||item.entityId!==target.id)return item;
        let statusEffects=[...(item.statusEffects||[])];
        if(effect&&(Number(effect.healing)>0||patch.diseaseState===null))statusEffects=statusEffects.filter(value=>!value.id.startsWith('monster-plague-boils:'));
        if(effect&&['healing-hands','nature-cure','close-wounds'].includes(effect.spellId))statusEffects=statusEffects.filter(value=>!value.id.startsWith('monster-otherworldly-hunger:')&&!value.id.startsWith('monster-otherworldly-thirst:'));
        if(effect&&Object.keys(patch).some(key=>!['strength','agility','wits','empathy','strengthMax','agilityMax','witsMax','empathyMax','diseaseState','diseaseVirulence','diseasePhase','poisonState','criticalInjury','status','dead','stopFatalBleeding','stopFatalBleedingInjuryId'].includes(key))){
          const effectStatus={id:`magic:${effect.spellId}`,name:effect.spellId,
            detail:JSON.stringify({patch,duration:effect.duration}),
            ...parseDurationToQuartersOrMinutes(effect.duration,effect.power)};
          statusEffects=[...statusEffects.filter(value=>value.id!==effectStatus.id),effectStatus];
        }
        for(const saved of target.statusEffects||[]){
          if(saved.id==='burning'){
            statusEffects=[...statusEffects.filter(effect=>effect.id!=='burning'),
              {id:'burning',name:saved.label,detail:saved.detail,source:saved.source,
                ongoingDamage:saved.ongoingDamage||1,ongoingRounds:saved.ongoingRounds||0}];
            continue;
          }
          if(!saved.id.startsWith('magic')) continue;
          statusEffects=[...statusEffects.filter(value=>value.id!==saved.id),{id:saved.id,name:saved.label,detail:saved.detail,
            ...(saved.remainingMinutes?{remainingMinutes:saved.remainingMinutes}:{}),
            ...(saved.ongoingRounds?{ongoingRounds:saved.ongoingRounds}:{}),
            ...(saved.ongoingDamage?{ongoingDamage:saved.ongoingDamage}:{}),
            ...(saved.speechBlocked?{speechBlocked:true}:{}),
            ...(typeof saved.incomingMetalAttackModifier==='number'?{incomingMetalAttackModifier:saved.incomingMetalAttackModifier}:{}),
            ...(typeof saved.mentalStrengthPower==='number'?{mentalStrengthPower:saved.mentalStrengthPower}:{}),
            ...(typeof saved.remainingRounds==='number'?{remainingRounds:saved.remainingRounds}:{}),
            ...(typeof saved.confusionRecoveryTo==='number'?{confusionRecoveryTo:saved.confusionRecoveryTo}:{}),
          }];
        }
        statusEffects=statusEffects.filter(value=>value.id!=='basilisk-warts');
        const warts=target.statusEffects?.find(value=>value.id==='basilisk-warts');
        if(warts)statusEffects.push({id:warts.id,name:warts.label,detail:warts.detail});
        statusEffects=statusEffects.filter(value=>value.id!=='monster-petrified');
        const petrification=target.statusEffects?.find(value=>value.id==='monster-petrified');
        if(petrification)statusEffects.push({id:petrification.id,name:petrification.label,detail:petrification.detail,ongoingRounds:petrification.ongoingRounds});
        statusEffects=statusEffects.filter(value=>!value.id.startsWith('monster-eggs:'));
        for(const eggs of target.statusEffects||[]){if(eggs.id.startsWith('monster-eggs:'))statusEffects.push({id:eggs.id,name:eggs.label,detail:eggs.detail,ongoingRounds:eggs.ongoingRounds});}
        statusEffects=statusEffects.filter(value=>!value.id.startsWith('monster-catatonic:'));
        for(const catatonia of target.statusEffects||[]){if(catatonia.id.startsWith('monster-catatonic:'))statusEffects.push({id:catatonia.id,name:catatonia.label,detail:catatonia.detail,speechBlocked:catatonia.speechBlocked});}
        return {...item,currentStrength:target.attributes.strength.current,brokenAttribute:target.brokenAttribute,
          status:target.status==='dead'?'dead':target.brokenAttribute||target.attributes.strength.current===0?'broken':item.status==='dead'||item.status==='broken'?'active':item.status,
          conditions:[...target.conditions],criticalInjuries:target.criticalInjuryStates||[],statusEffects,
          ...(patch.loseFastAction?{fastActionAvailable:false}:{}),...(patch.loseSlowAction?{slowActionAvailable:false}:{}),
          quickSheet:{...(item.quickSheet||{}),attributes:target.attributes,conditions:target.conditions,brokenAttribute:target.brokenAttribute,willpowerPoints:target.willpowerPoints,status:target.status,criticalInjuryStates:target.criticalInjuryStates||[],diseaseState:target.diseaseState,poisonState:target.poisonState}};
      });
      await store.put({...world,encounter:{...encounter,combatants,log:[...encounter.log,{id:crypto.randomUUID(),round:encounter.round,combatantId:combatants.find(item=>item.entityId===target.id)?.id,text:effect?`${target.name}: magia ${effect.spellId} aplicada.`:`${target.name}: consequência mágica persistida.`,createdAt:updatedAt}].slice(-300),updatedAt},updatedAt});
    }
  }

  static async resolveContagionExposure(campaignId: string, contagiousId: string, targetId: string, rng?: () => number) {
    return withTransaction(['characters', 'world_state', 'rolls'], 'readwrite', async tx => {
      const caster = await tx.objectStore('characters').get(contagiousId) as Character | undefined;
      const target = await tx.objectStore('characters').get(targetId) as Character | undefined;
      if (!caster || !target || target.status === 'dead') return;
      const state = caster.magicMishapState;
      if (!state || !state.contagionQuartersRemaining || !state.magicalDiseaseVirulence) return;
      
      const virulence = state.magicalDiseaseVirulence;
      const rand = rng || Math.random;
      
      const skillRank = Object.entries(target.skills || {}).find(([name]) => ['endurance', 'resiliencia', 'resiliência'].includes(name.toLowerCase()))?.[1] || 0;
      const targetRoll = ForbiddenLandsDiceEngine.roll(
        { base: target.attributes.strength.current, skill: skillRank, gear: 0 },
        { characterId: target.id, sceneId: `contagion:${campaignId}`, skillName: 'endurance', attributeName: 'strength', rng: rand }
      );
      
      const diseaseRoll = ForbiddenLandsDiceEngine.roll(
        { base: virulence, skill: 0, gear: 0 },
        { characterId: null, sceneId: `contagion:${campaignId}`, rng: rand }
      );
      
      await tx.objectStore('rolls').put(targetRoll);
      await tx.objectStore('rolls').put(diseaseRoll);
      
      let nextTarget = { ...target };
      let text = '';
      if (targetRoll.successes > diseaseRoll.successes) {
        text = `${target.name} resistiu ao contágio de doença mágica de ${caster.name} (Resiliência ${targetRoll.successes} vs Virulência ${diseaseRoll.successes}).`;
      } else {
        nextTarget = DiseaseRulesEngine.infect(target, virulence);
        if (nextTarget.diseaseState) {
          nextTarget.statusEffects = [
            ...(nextTarget.statusEffects || []),
            { id: `magic-disease:${crypto.randomUUID()}`, label: 'Doença Mágica', detail: `Infectado por contágio mágico de ${caster.name} com Virulência ${virulence}.` }
          ];
        }
        text = `${target.name} contraiu a doença mágica de ${caster.name} (Resiliência ${targetRoll.successes} vs Virulência ${diseaseRoll.successes}).`;
      }
      
      await tx.objectStore('characters').put(nextTarget);
      
      const worlds = await tx.objectStore('world_state').getAll();
      for (const raw of worlds) {
        const world = WorldStateService.normalize(raw, raw.id);
        const encounter = world.encounter;
        if (!encounter || encounter.status !== 'active') continue;
        const inEncounter = encounter.combatants.some(item => item.entityId === targetId);
        if (!inEncounter) continue;
        
        const combatants = encounter.combatants.map(item => {
          if (item.entityId !== targetId) return item;
          return {
            ...item,
            currentStrength: nextTarget.attributes.strength.current,
            brokenAttribute: nextTarget.brokenAttribute,
            conditions: [...nextTarget.conditions],
            status: nextTarget.status === 'dead' ? 'dead' : nextTarget.attributes.strength.current === 0 ? 'broken' : item.status,
            statusEffects: [
              ...(item.statusEffects || []),
              ...(nextTarget.diseaseState ? [{ id: 'magic-disease', name: 'Doença Mágica', detail: `Infectado por contágio mágico de ${caster.name} com Virulência ${virulence}.` }] : [])
            ],
            quickSheet: {
              ...(item.quickSheet || {}),
              attributes: nextTarget.attributes,
              conditions: nextTarget.conditions,
              brokenAttribute: nextTarget.brokenAttribute,
              diseaseState: nextTarget.diseaseState
            }
          };
        });
        
        await tx.objectStore('world_state').put({
          ...world,
          encounter: {
            ...encounter,
            combatants,
            log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, text, createdAt: Date.now() }].slice(-300),
            updatedAt: Date.now()
          },
          updatedAt: Date.now()
        });
      }
    });
  }

  static async checkConcentration(campaignId: string, characterId: string, damage: number, rng?: () => number) {
    return withTransaction(['characters', 'world_state', 'rolls'], 'readwrite', async tx => {
      const character = await tx.objectStore('characters').get(characterId) as Character | undefined;
      if (!character || character.status === 'dead') return;
      const hasConcentration = character.statusEffects?.some(effect => effect.id.includes('concentration') || effect.id.startsWith('magic:'));
      if (!hasConcentration) return;
      
      const rand = rng || Math.random;
      const skillRank = Object.entries(character.skills || {}).find(([name]) => ['endurance', 'resiliencia', 'resiliência'].includes(name.toLowerCase()))?.[1] || 0;
      const roll = ForbiddenLandsDiceEngine.roll(
        { base: character.attributes.strength.current, skill: skillRank, gear: 0 },
        { characterId, sceneId: `concentration:${campaignId}`, skillName: 'endurance', attributeName: 'strength', rng: rand }
      );
      
      await tx.objectStore('rolls').put(roll);
      
      let next = { ...character };
      let text = '';
      if (roll.successes >= damage) {
        text = `${character.name} manteve a concentração na magia apesar de sofrer ${damage} de dano (Resiliência ${roll.successes} vs dano ${damage}).`;
      } else {
        next.statusEffects = (character.statusEffects || []).filter(effect => !effect.id.includes('concentration') && !effect.id.startsWith('magic:'));
        text = `${character.name} perdeu a concentração na magia ao sofrer ${damage} de dano (Resiliência ${roll.successes} vs dano ${damage}).`;
      }
      
      await tx.objectStore('characters').put(next);
      
      const worlds = await tx.objectStore('world_state').getAll();
      for (const raw of worlds) {
        const world = WorldStateService.normalize(raw, raw.id);
        const encounter = world.encounter;
        if (!encounter || encounter.status !== 'active') continue;
        const inEncounter = encounter.combatants.some(item => item.entityId === characterId);
        if (!inEncounter) continue;
        
        const combatants = encounter.combatants.map(item => {
          if (item.entityId !== characterId) return item;
          return {
            ...item,
            statusEffects: (item.statusEffects || []).filter(effect => !effect.id.includes('concentration') && !effect.id.startsWith('magic:'))
          };
        });
        
        await tx.objectStore('world_state').put({
          ...world,
          encounter: {
            ...encounter,
            combatants,
            log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, text, createdAt: Date.now() }].slice(-300),
            updatedAt: Date.now()
          },
          updatedAt: Date.now()
        });
      }
    });
  }
}
