import {OfficialDemonGenerator,type OfficialGeneratedDemon} from './OfficialDemonGenerator';
import { BloodmarchBetaFireGuard } from './BloodmarchBetaFireGuard.ts';
import { getDb, withDbRetry, withTransaction } from '../db/db';
import {
  CampaignState,
  Character,
  GamePeriod,
  IndividualHex,
  JourneyPace,
  JourneyRole,
  ResourceDie,
  WorldState,
  JourneyEvent,
  JourneyMount,
  JourneyDailyWeather,
  JourneyPersistentAnimalComplication,
  EncounterCombatant,
} from '../types';
import { WorldStateService } from './WorldStateService';
import { CampaignService } from './CampaignService';
import { EnvironmentalDirectorService } from './EnvironmentalDirectorService';
import { MapTravelService } from './MapTravelService';
import { ForbiddenLandsDiceEngine } from './ForbiddenLandsDiceEngine';
import { CombatRulesEngine } from './CombatRulesEngine';
import { JourneyRulesEngine, Season } from './JourneyRulesEngine';
import { OfficialEncounterRulesEngine } from './OfficialEncounterRulesEngine';
import { CampaignPoiService } from './CampaignPoiService';
import { CriticalInjuryRulesEngine } from './CriticalInjuryRulesEngine';
import { SurvivalRulesEngine } from './SurvivalRulesEngine';
import { CharacterTimeRulesEngine } from './CharacterTimeRulesEngine';
import { CharacterActionRulesEngine } from './CharacterActionRulesEngine';
import { EncumbranceRulesEngine } from './EncumbranceRulesEngine';
import { ForbiddenLandsEconomyService } from './ForbiddenLandsEconomyService';
import { DiseaseRulesEngine } from './DiseaseRulesEngine';
import { DrowningRulesEngine } from './DrowningRulesEngine';
import { PoisonRulesEngine } from './PoisonRulesEngine';
import { LiceRulesEngine } from './LiceRulesEngine';
import { BasiliskWartsRulesEngine } from './BasiliskWartsRulesEngine';
import { ForbiddenLandsCharacterRules } from './ForbiddenLandsCharacterRules';
import { EncounterService } from './EncounterService';
import { BookOfBeastsCatalogService } from './BookOfBeastsCatalogService';
import { BloodMistDemonRulesEngine } from './BloodMistDemonRulesEngine';
import { OfficialEncounterNpcFactory } from './OfficialEncounterNpcFactory';
import { OfficialEncounterTreasureEngine } from './OfficialEncounterTreasureEngine';
import { OfficialEncounterFollowerEngine } from './OfficialEncounterFollowerEngine';
import { BitterReachModule } from './BitterReachModule';
import { BitterReachRuntimeService } from './BitterReachRuntimeService';
import { CampaignPartyService } from './CampaignPartyService';
import { InventorySelectionSnapshot } from './InventorySelectionSnapshot';
import { BloodmarchBetaPropheticDreamsEngine } from './BloodmarchBetaPropheticDreamsEngine';
import {BloodmarchBetaDreamTravelEngine} from './BloodmarchBetaDreamTravelEngine.ts';

export const JOURNEY_ROLE_LABELS: Record<JourneyRole, string> = {
  guide: 'Guia',
  lookout: 'Vigia',
  quartermaster: 'Responsável pelos recursos',
  hunter: 'Caçador',
  forager: 'Forrageador',
  campMaker: 'Responsável pelo acampamento',
  captain: 'Capitã da embarcação',
};

export const JOURNEY_PACE_LABELS: Record<JourneyPace, string> = {
  careful: 'Cauteloso',
  normal: 'Normal',
  forced: 'Forçado',
};

const periodLabel: Record<GamePeriod, string> = {
  Morning: 'manhã',
  Day: 'dia',
  Evening: 'entardecer',
  Night: 'noite',
};

const provisionLabel = (kind: import('../types').JourneyProvisionKind) => ({ vegetables:'Vegetais', meat:'Carne', fish:'Peixe', pelts:'Peles', food:'Comida' } as const)[kind];
const readyMountFor = (mounts: JourneyMount[] | undefined, characterId: string) => (mounts || []).find(mount => mount.riderCharacterId === characterId && mount.status === 'ready') || null;
const appendJourneyProvision = (character: Character, kind: import('../types').JourneyProvisionKind, quantity: number, day: number, sourceEventId?: string): Character => {
  const safeQuantity=Math.max(0,Math.trunc(quantity));
  if(!safeQuantity)return character;
  const provisions=(character.journeyProvisions||[]).filter(batch=>batch.quantity>0);
  const batch=JourneyRulesEngine.journeyProvision(kind,safeQuantity,day,sourceEventId,crypto.randomUUID());
  return {...character,journeyProvisions:[...provisions,batch]};
};

const criticalStateForJourney = (table:'slash'|'blunt'|'puncture'|'horror',roll:number,rng:()=>number,createdAt:number) => {
  const entry=CriticalInjuryRulesEngine.get(table,roll);
  const state:import('../types').CriticalInjuryState={
    id:crypto.randomUUID(),table,roll,ruleId:entry.id,name:entry.name,lethal:entry.lethal,
    deathTimer:CriticalInjuryRulesEngine.rollDeathTimer(entry,rng),healingTime:CriticalInjuryRulesEngine.rollHealing(entry,rng),
    effects:entry.effects,sourcePage:entry.sourcePage,status:'active',createdAt,
  };
  if(state.deathTimer&&'amount' in state.deathTimer)state.deathTimeRemaining={amount:state.deathTimer.amount,unit:state.deathTimer.unit};
  if(state.healingTime&&'amount' in state.healingTime)state.healingDaysRemaining=state.healingTime.amount;
  return state;
};

const dieSides = (die: ResourceDie) => die ? Number(die.slice(1)) : 0;

export type ResourceRollResult = {
  roll: number | null;
  before: ResourceDie;
  after: ResourceDie;
  depleted: boolean;
  poisonSummary?: string;
  empathyRecovered?: boolean;
};

export type FallHazardResult = { damage: number; critical: import('../types').CriticalInjuryState | null; roll: number };
export type ColdHazardResult = { roll: import('../types').DiceRoll; becameHypothermic: boolean; damageApplied: boolean; died: boolean };
export type DiseaseHazardResult = { character: Character; patientRoll: import('../types').DiceRoll; diseaseRoll: import('../types').DiceRoll; infected?: boolean; recovered?: boolean; damageApplied?: boolean };
export type DrowningHazardResult = { character: Character; roll: import('../types').DiceRoll | null; phaseBefore: 'afloat'|'underwater'|'drowning'; phaseAfter: 'afloat'|'underwater'|'drowning'; damageApplied: boolean; died: boolean };
export type PoisonHazardResult = { character: Character; victimRoll: import('../types').DiceRoll; poisonRoll: import('../types').DiceRoll; totalEffect: boolean; attribute: 'strength'|'agility'|'wits'|'empathy'; damageApplied: boolean };
export type WeaponPoisonHitResult = { attacker: Character; target?: Character; victimRoll?: import('../types').DiceRoll; poisonRoll?: import('../types').DiceRoll; totalEffect?: boolean; attribute?: 'strength'|'agility'|'wits'|'empathy'; damageApplied: boolean; affected: boolean };


export type JourneyActivityInput = {
  characterId: string;
  activity: 'hike'|'leadWay'|'keepWatch'|'forageFood'|'forageWater'|'hunt'|'fish'|'makeCamp'|'rest'|'sleep';
  /** For a shared-position party, companions must actually be sleeping in this same QD. */
  dreamTravelCompanionsSleepingConfirmed?:boolean;
  terrainKey?: string;
  season?: Season;
  dark?: boolean;
  equipmentBonus?: number;
  helpBonus?: number;
  helperIds?: string[];
  huntMethod?: 'weapon'|'trap';
  fishingMethod?: 'rod'|'net';
  metalArmor?: boolean;
  /** RNG substituível para reprodução e testes comportamentais. */
  rng?: () => number;
};

export type JourneyActivityResult = {
  event: JourneyEvent;
  rollSuccesses?: number;
  secondarySuccesses?: number;
  advancedPeriod: boolean;
};

const JOURNEY_RULE_SOURCE = 'Livro do Jogador, Cap. 7 — Jornadas';
const normalizedSkill = (character: Character, names: string[]) => {
  const map = new Map(Object.entries(character.skills || {}).map(([key, value]) => [key.toLocaleLowerCase('pt-BR'), Number(value || 0)]));
  for (const name of names) { const value = map.get(name.toLocaleLowerCase('pt-BR')); if (value !== undefined) return value; }
  return 0;
};
const normalizeRuleName=(value:string)=>String(value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').replace(/[^a-z0-9]/g,'');
const normalizedTalentRank=(character:Character,names:string[])=>{
  const wanted=new Set(names.map(normalizeRuleName));
  for(const [key,value] of Object.entries(character.talents||{}))if(wanted.has(normalizeRuleName(key)))return Math.max(0,Math.min(3,Math.trunc(Number(value)||0)));
  return 0;
};
const addCookedFoodQuality=(character:Character,steps:number):Character=>{
  const add=Math.max(0,Math.trunc(Number(steps)||0)); if(!add)return character;
  const effects=[...(character.statusEffects||[])]; const index=effects.findIndex(effect=>effect.id==='journey:cook-rank3-food');
  const existing=index>=0?effects[index]:undefined; const resourceQualitySteps=Math.max(0,Math.trunc(Number(existing?.resourceQualitySteps)||0))+add;
  const next={id:'journey:cook-rank3-food',label:'Comida excepcional (Cozinheiro 3)',detail:`As próximas reservas de Comida preparadas por Cozinheiro 3 recuperam 1 Empatia por refeição enquanto ${resourceQualitySteps} passo(s) desse lote permanecerem no Dado de Recurso.`,resourceQualityKind:'food' as const,resourceQualitySteps,empathyRecoveryOnMeal:1};
  if(index>=0)effects[index]=next;else effects.push(next); return {...character,statusEffects:effects};
};
const campSleepDenialReason = (world: WorldState, campaign: CampaignState): string | null => {
  for (const event of [...world.journey.events].reverse()) {
    if (event.status !== 'open' || event.type !== 'mishap') continue;
    const mechanic = event.result?.mechanic as Record<string, unknown> | undefined;
    if (mechanic?.kind === 'sleep_denied' && mechanic.condition === 'until_new_camp') return 'O acampamento atual é inadequado: ninguém consegue dormir até preparar um novo acampamento.';
    if (mechanic?.kind === 'attribute_damage' && mechanic.sleepDenied === true) return 'Formigas impedem dormir neste acampamento; prepare um novo acampamento.';
    if (mechanic?.kind === 'storm' && mechanic.sleepDenied === true && event.day === (campaign.currentDay || 1) && (campaign.currentPeriod || 'Morning') === 'Night') return 'O temporal impede dormir nesta noite.';
  }
  return null;
};

/** R86: manter ocorrências ainda abertas e recompensas oficiais pendentes.
 * A janela de 200 registros é somente para eventos históricos concluídos;
 * não pode apagar um cativo, combate ou cavalo em devolução após longa viagem.
 */
export const retainJourneyEvents=(events:JourneyEvent[]):JourneyEvent[]=>{
  const cutoff=Math.max(0,events.length-200);
  return events.filter((event,index)=>{
    if(index>=cutoff||event.status==='open'||(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.foxFollowerActive===true||(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.linnFollowerActive===true)return true;
    const mechanic=event.result?.mechanic as Record<string,unknown>|undefined;
    const decision=event.result?.officialSceneDecision as Record<string,unknown>|undefined;
    const id=Number(mechanic?.encounterId);
    // U11: a cena pode estar resolvida, mas obrigações do mundo continuam.
    // Não perder eventos de referência após >200 ações de outros serviços.
    if(id===12 && decision?.choiceId==='offer_camp'){
      const gift=decision.banditFoodGift as Record<string,unknown>|undefined;
      const quantity=gift?.quantity;
      if(Number.isSafeInteger(quantity) && Number(quantity)>=1 && Number(quantity)<=30 &&
        decision.banditReaction!=='accept' && decision.banditReaction!=='decline')return true;
    }
    if(id===35 && decision?.kargOfferAccepted===true && decision.kargGiftTransferRequired===true &&
      decision.stage!=='karg_offer_refused'){
      const gift=decision.kargGift as Record<string,unknown>|undefined;
      if(!(typeof gift?.transferredAt==='number' && Number.isFinite(gift.transferredAt) && gift.transferredAt>0))return true;
    }
    // U12: garrafas compradas/trocadas continuam pertencendo ao inventário
    // mesmo após a partida do veleiro. Sem o evento de origem não seria
    // possível abrir uma garrafa legítima depois de 200 ações da jornada.
    if(id===41 && decision){
      const purchased=Array.isArray(decision.soulBottlePurchases)?decision.soulBottlePurchases.reduce((sum:number,entry:unknown)=>{
        if(!entry||typeof entry!=='object')return sum;
        const record=entry as Record<string,unknown>,quantity=record.quantity;
        return typeof quantity==='number'&&Number.isSafeInteger(quantity)&&quantity>=1&&quantity<=99&&
          record.priceSilver===quantity*10&&typeof record.paidAt==='number'&&
          Number.isFinite(record.paidAt)&&record.paidAt>0?sum+quantity:sum;
      },0):0;
      const bartered=Array.isArray(decision.soulBottleBarters)?decision.soulBottleBarters.filter(entry=>
        entry&&typeof entry==='object'&&typeof (entry as Record<string,unknown>).characterId==='string'&&
        Boolean((entry as Record<string,unknown>).characterId)).length:0;
      const released=Array.isArray(decision.releasedSoulTokens)?new Set(decision.releasedSoulTokens.filter(token=>
        token&&typeof token==='object'&&typeof (token as Record<string,unknown>).id==='string'&&
        Boolean((token as Record<string,unknown>).id)).map(token=>(token as Record<string,unknown>).id)).size:0;
      if(purchased+bartered>released)return true;
    }
    if(id===41 && decision?.soulResurrectionD6Pending===true &&
      Array.isArray(decision.releasedSoulTokens) && decision.releasedSoulTokens.some(token=>
        token && typeof token==='object' && typeof (token as Record<string,unknown>).id==='string' &&
        Boolean((token as Record<string,unknown>).id)))return true;
    return id===6&&decision?.horseDisposition==='claim'&&
      !decision.horseFamilyRewardAt;
  });
};

export class JourneyService {
  static async startMaritimeTravel(campaignId:string,captainCharacterId:string,vesselName:string):Promise<WorldState>{
    const name=String(vesselName||'').trim();
    if(!name)throw new Error('Informe o nome ou tipo da embarcação.');
    const now=Date.now();
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId); if(!campaign)throw new Error('Campanha não encontrada.');
      const captain=await tx.objectStore('characters').get(captainCharacterId) as Character|undefined;
      if(!captain||captain.status==='dead')throw new Error('Escolha uma capitã válida para a viagem marítima.');
      CampaignPartyService.assertMember(campaign, captain);
      CharacterActionRulesEngine.assertCanAct(captain);
      const store=tx.objectStore('world_state'); const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.journey.maritime?.hullStatus==='sunk')throw new Error('A embarcação anterior afundou; inicie a viagem com outra embarcação.');
      const assignments=[...world.journey.assignments.filter(item=>item.role!=='captain'),{role:'captain' as const,characterId:captain.id,assignedAt:now}];
      const maritime={active:true,vesselName:name,captainCharacterId:captain.id,hullStatus:'sound' as const,startedAt:now,updatedAt:now};
      const event:JourneyEvent={id:crypto.randomUUID(),type:'travel',title:'Viagem marítima iniciada',detail:`${captain.name} assume como capitã de ${name}. Cada novo hexágono de água exige DESBRAVAR com Sobrevivência; falha gera um infortúnio marítimo.`,result:{mechanic:{kind:'maritime_started'},captainCharacterId:captain.id,vesselName:name},ruleSource:JOURNEY_RULE_SOURCE,day:campaign.currentDay||1,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      const updated={...world,journey:{...world.journey,assignments,maritime,events:retainJourneyEvents([...world.journey.events,event]),updatedAt:now},updatedAt:now}; await store.put(updated); return updated;
    }); this.notify(next); return next;
  }

  static async endMaritimeTravel(campaignId:string):Promise<WorldState>{
    const now=Date.now();
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId); if(!campaign)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state'); const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(!world.journey.maritime?.active)return world;
      const maritime={...world.journey.maritime,active:false,updatedAt:now};
      const event:JourneyEvent={id:crypto.randomUUID(),type:'travel',title:'Viagem marítima encerrada',detail:`O grupo deixou ${maritime.vesselName} ou encerrou o trecho navegável.`,result:{mechanic:{kind:'maritime_ended'}},ruleSource:JOURNEY_RULE_SOURCE,day:campaign.currentDay||1,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      const updated={...world,journey:{...world.journey,maritime,assignments:world.journey.assignments.filter(item=>item.role!=='captain'),events:retainJourneyEvents([...world.journey.events,event]),updatedAt:now},updatedAt:now}; await store.put(updated); return updated;
    }); this.notify(next); return next;
  }

  static async repairMaritimeVessel(campaignId:string,crafterId:string,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;repaired:boolean;status:string;world:WorldState}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId); if(!campaign)throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state'); const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      const maritime=world.journey.maritime; if(!maritime||!['leaking','aground'].includes(maritime.hullStatus))throw new Error('Não há dano marítimo que exija reparo.');
      const crafter=await tx.objectStore('characters').get(crafterId) as Character|undefined; if(!crafter)throw new Error('Artesã não encontrada.'); CampaignPartyService.assertMember(campaign, crafter); CharacterActionRulesEngine.assertCanAct(crafter);
      const skill=normalizedSkill(crafter,['Crafting','Artesanato']);
      const roll=ForbiddenLandsDiceEngine.roll({base:crafter.attributes.strength.current,skill,gear:0},{characterId:crafter.id,sceneId:`journey:maritime-repair:${campaignId}`,checkId:`journey:maritime-repair:${now}`,rng,criticalInjuries:crafter.criticalInjuryStates,skillName:'crafting',skillRankAtRoll:skill,attributeName:'strength'}); await tx.objectStore('rolls').put(roll);
      const repaired=roll.successes>0; const nextMaritime=repaired?{...maritime,hullStatus:'sound' as const,leakHoursRemaining:undefined,updatedAt:now}:{...maritime,updatedAt:now};
      const events=world.journey.events.map(event=>event.status==='open'&&['maritime_leak','maritime_aground','maritime_whirlpool'].includes(String((event.result?.mechanic as Record<string,unknown>|undefined)?.kind||''))&&repaired?{...event,status:'resolved' as const,detail:`${event.detail} ${crafter.name} reparou ${maritime.vesselName} com Artesanato (${roll.successes} sucesso(s)).`}:event);
      const event:JourneyEvent={id:crypto.randomUUID(),type:'activity',title:repaired?'Embarcação reparada':'Reparo marítimo falhou',detail:`${crafter.name} rolou Artesanato: ${roll.successes} sucesso(s). ${repaired?`${maritime.vesselName} pode continuar navegando.`:'O dano permanece e precisa de nova tentativa.'}`,result:{mechanic:{kind:'maritime_repair'},rollId:roll.id,repaired},ruleSource:JOURNEY_RULE_SOURCE,day:campaign.currentDay||1,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      const nextWorld={...world,journey:{...world.journey,maritime:nextMaritime,events:retainJourneyEvents([...events,event]),updatedAt:now},updatedAt:now}; await worldStore.put(nextWorld); return {roll,repaired,status:nextMaritime.hullStatus,world:nextWorld};
    }); this.notify(outcome.world); return outcome;
  }

  static async abandonMaritimeVessel(campaignId:string):Promise<WorldState>{
    const now=Date.now(); const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{const campaign=await tx.objectStore('campaign').get(campaignId);if(!campaign)throw new Error('Campanha não encontrada.');const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);const maritime=world.journey.maritime;if(!maritime)throw new Error('Nenhuma embarcação ativa.');const updated={...world,journey:{...world.journey,maritime:{...maritime,active:false,hullStatus:'abandoned' as const,updatedAt:now},assignments:world.journey.assignments.filter(item=>item.role!=='captain'),events:world.journey.events.map(event=>event.status==='open'&&['maritime_aground','maritime_whirlpool'].includes(String((event.result?.mechanic as Record<string,unknown>|undefined)?.kind||''))?{...event,status:'resolved' as const,detail:`${event.detail} A embarcação foi abandonada.`}:event),updatedAt:now},updatedAt:now};await store.put(updated);return updated;});this.notify(next);return next;
  }

  static async resolveMaritimeOverboard(campaignId:string,eventId:string,characterId:string,metalArmor=false):Promise<WorldState>{
    const now=Date.now(); const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{const campaign=await tx.objectStore('campaign').get(campaignId);if(!campaign)throw new Error('Campanha não encontrada.');const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);const source=world.journey.events.find(event=>event.id===eventId&&event.status==='open'&&(event.result?.mechanic as Record<string,unknown>|undefined)?.kind==='maritime_overboard');if(!source)throw new Error('Queda ao mar não está pendente.');const characters=tx.objectStore('characters');const character=await characters.get(characterId) as Character|undefined;if(!character||character.status==='dead')throw new Error('Escolha uma personagem válida.');CampaignPartyService.assertMember(campaign,character);await characters.put(DrowningRulesEngine.enterWater(character,metalArmor));const events=world.journey.events.map(event=>event.id===eventId?{...event,status:'resolved' as const,result:{...(event.result||{}),characterId,metalArmor},detail:`${event.detail} ${character.name} caiu na água${metalArmor?' usando armadura de metal':''}; o estado de afogamento foi iniciado.`}:event);const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;});this.notify(next);return next;
  }

  static async resolveMaritimeItemOverboard(campaignId:string,eventId:string,characterId:string,collection:'weapons'|'armor'|'gear',index:number,expectedSnapshot?:string):Promise<{itemName:string;world:WorldState}>{
    const now=Date.now(); const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{const campaign=await tx.objectStore('campaign').get(campaignId);if(!campaign)throw new Error('Campanha não encontrada.');const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);const source=world.journey.events.find(event=>event.id===eventId&&event.status==='open'&&(event.result?.mechanic as Record<string,unknown>|undefined)?.kind==='maritime_item_overboard');if(!source)throw new Error('Item ao mar não está pendente.');const characters=tx.objectStore('characters');const character=await characters.get(characterId) as Character|undefined;if(!character)throw new Error('Personagem não encontrada.');CampaignPartyService.assertMember(campaign,character);const items=character[collection] as any[];if(!Number.isInteger(index)||index<0||index>=items.length)throw new Error('Escolha um item válido.');const item=items[index];if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(item,expectedSnapshot);const itemName=String(item?.name||item?.label||'Equipamento');const afterLoss={...character,[collection]:items.filter((_,position)=>position!==index)};await characters.put({...afterLoss,encumbrance:EncumbranceRulesEngine.calculate(afterLoss)});const events=world.journey.events.map(event=>event.id===eventId?{...event,status:'resolved' as const,result:{...(event.result||{}),characterId,collection,index,lostItem:item},detail:`${event.detail} A MdJ escolheu ${itemName}, de ${character.name}; o item caiu na água e foi perdido.`}:event);const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return {itemName,world:updated};});this.notify(outcome.world);return outcome;
  }

  /** Trata um ferimento fora de combate, registrando a rolagem e a tentativa na ficha. */
  static async treatCriticalInjury(campaignId:string,patientId:string,injuryId:string,healerId:string,rng:()=>number=Math.random){
    const result=await withTransaction(['campaign','characters','rolls','world_state'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId);
      if(!campaign)throw new Error('Campanha não encontrada.');
      const world=await tx.objectStore('world_state').get(campaignId);
      if(world?.encounter?.status==='active')throw new Error('Resolva o encontro antes de tratar ferimentos fora de combate.');
      const characters=tx.objectStore('characters');
      const patient=await characters.get(patientId),healer=await characters.get(healerId);
      if(!patient||!healer)throw new Error('Pessoa ferida ou curandeiro não encontrado.');
      CampaignPartyService.assertMember(campaign,patient);
      CampaignPartyService.assertMember(campaign,healer);
      const injury=(patient.criticalInjuryStates||[]).find((item:import('../types').CriticalInjuryState)=>item.id===injuryId);
      if(!injury)throw new Error('Ferimento grave não encontrado na ficha.');
      const skill=Object.entries(healer.skills||{}).find(([name])=>['cura','healing'].includes(name.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR')))?.[1]||0;
      const roll=ForbiddenLandsDiceEngine.roll({base:healer.attributes.empathy.current,skill:Number(skill),gear:0,modifier:healer.id===patient.id?-2:0},{
        characterId:healer.id,sceneId:`journey:critical-treatment:${campaignId}`,checkId:`critical-treatment:${patient.id}:${injury.id}:${healer.id}`,
        rng,criticalInjuries:healer.criticalInjuryStates,skillName:'healing',attributeName:'empathy',
      });
      const updatedInjury=CriticalInjuryRulesEngine.attemptTreatment(injury,healer.id,roll.successes,Date.now());
      const brokenAttribute=patient.brokenAttribute||(patient.conditions.includes('Broken')?'strength':undefined);
      const attributeValue=brokenAttribute?Math.min(patient.attributes[brokenAttribute].max,patient.attributes[brokenAttribute].current+roll.successes):undefined;
      const recovered=typeof attributeValue==='number'&&attributeValue>0;
      const attributes=brokenAttribute&&attributeValue!==undefined?{...patient.attributes,[brokenAttribute]:{...patient.attributes[brokenAttribute],current:attributeValue}}:patient.attributes;
      let updatedPatient:Character={...patient,criticalInjuryStates:(patient.criticalInjuryStates||[]).map((item:import('../types').CriticalInjuryState)=>item.id===injuryId?updatedInjury:item),attributes,
        conditions:recovered?patient.conditions.filter((condition:string)=>condition!=='Broken'):patient.conditions,
        ...(recovered?{brokenAttribute:undefined}:{})};
      if(patient.basiliskWarts&&roll.successes>0)updatedPatient=BasiliskWartsRulesEngine.cure(updatedPatient);
      await characters.put(updatedPatient);
      await tx.objectStore('rolls').put(roll);
      return {roll,injury:updatedInjury,patientName:patient.name,healerName:healer.name};
    });
    return result;
  }

  static applyTravel(
    worldInput: WorldState,
    campaignBefore: CampaignState,
    campaignAfter: CampaignState,
    target: IndividualHex,
    distanceKm: number,
    periods: number,
    now = Date.now(),
    quarterTravelProgress = 0,
  ): WorldState {
    let world = WorldStateService.normalize(worldInput, campaignAfter.id);
    const event = {
      id: crypto.randomUUID(),
      type: 'travel' as const,
      title: `Viagem até ${target.coordinate}`,
      detail: `${distanceKm} km percorridos; ${periods} período(s) consumido(s). Chegada durante ${periodLabel[campaignAfter.currentPeriod || 'Morning']} do dia ${campaignAfter.currentDay || 1}.`,
      day: campaignAfter.currentDay || 1,
      period: campaignAfter.currentPeriod || 'Morning',
      hexId: target.id,
      status: 'resolved' as const,
      createdAt: now,
    };
    world = {
      ...world,
      journey: {
        ...world.journey,
        active: true,
        distanceKm: Math.max(0, Number(world.journey.distanceKm || 0) + Math.max(0, distanceKm)),
        periodsTravelled: Math.max(0, Number(world.journey.periodsTravelled || 0) + Math.max(0, periods)),
        quarterTravelProgress: Math.max(0, Math.min(0.999999, Number(quarterTravelProgress || 0))),
        events: retainJourneyEvents([...world.journey.events, event]),
        lastTravelAt: now,
        updatedAt: now,
      },
      updatedAt: now,
    };
    const periodOrder: GamePeriod[]=['Morning','Day','Evening','Night'];
    const beforeIndex=(Math.max(1,campaignBefore.currentDay||1)-1)*4+Math.max(0,periodOrder.indexOf(campaignBefore.currentPeriod||'Morning'));
    const afterIndex=(Math.max(1,campaignAfter.currentDay||1)-1)*4+Math.max(0,periodOrder.indexOf(campaignAfter.currentPeriod||'Morning'));
    return WorldStateService.advanceTime(world, campaignBefore, campaignAfter, Math.max(0,afterIndex-beforeIndex), now);
  }


  static buildOfficialRandomEncounterEventForTerrain(campaign:CampaignState,hexId:string|undefined,terrainInput:string,cadence:'walking'|'stationary',rng:()=>number=Math.random,at?:{day:number;period:GamePeriod}):JourneyEvent {
    const rolled=JourneyRulesEngine.rollRandomEncounter(String(terrainInput||'plains'),rng);
    const day=at?.day||campaign.currentDay||1;
    const period=at?.period||campaign.currentPeriod||'Morning';
    const sourcePage=rolled.none?null:JourneyRulesEngine.officialEncounterSourcePage(rolled.encounterId);
    const beggars=(!rolled.none&&rolled.encounterId===15)?OfficialEncounterRulesEngine.beggarsEncounter():null;
    const specialScene=(!rolled.none&&!beggars)?OfficialEncounterRulesEngine.specialScene(rolled.encounterId):null;
    return {
      id:crypto.randomUUID(),
      type:'encounter',
      title:rolled.none?'Nenhum encontro aleatório':`Encontro aleatório — ${rolled.name}`,
      detail:rolled.none
        ? `D66 ${rolled.roll} em ${rolled.terrain}: nenhum encontro significativo.`
        : beggars?`D66 ${rolled.roll} em ${rolled.terrain}: ${beggars.publicDetail} Não há teste obrigatório nem recompensa automática nesta entrada; registre a decisão do grupo.`
        : specialScene?`D66 ${rolled.roll} em ${rolled.terrain}: ${specialScene.publicDetail} Escolha a ação do grupo. Consequências não automáticas exigem resolução própria.`
        : `D66 ${rolled.roll} em ${rolled.terrain}: encontro ${rolled.encounterId} — ${rolled.name}. Consulte a entrada oficial do Guia do Mestre e resolva suas escolhas/efeitos sem substituir a regra por improvisação automática.`,
      gmDetail:rolled.none?undefined:`Guia do Mestre, Cap. 7, p. ${sourcePage ?? 'a conferir'}. Resultado ${rolled.encounterId}: ${rolled.name}.${beggars?' '+beggars.gmSecret:''}${specialScene?' '+specialScene.gmSecret+' '+specialScene.gmRule:''}`, 
      result:{mechanic:{kind:'official_random_encounter',encounterId:rolled.encounterId,encounterName:rolled.name,terrain:rolled.terrain,d66:rolled.roll,cadence,sourcePage,automation:beggars?'official_narrative_no_mandatory_roll':specialScene?'official_branching_pending_mechanics':'manual_source_required',...(beggars?{npcGroupSize:beggars.count}: {}),...(specialScene?{officialSceneVersion:1}: {})},encounterId:rolled.encounterId,encounterName:rolled.name,terrain:rolled.terrain,d66:rolled.roll,cadence,sourcePage},
      ruleSource:rolled.none?'Guia do Mestre, Cap. 7 — Encontros, tabela p. 147':`Guia do Mestre, Cap. 7, p. ${sourcePage ?? 'a conferir'}; Manual do Jogador, Cap. 7 — cadência de encontros`,
      day,period,hexId,status:rolled.none?'resolved':'open',createdAt:Date.now(),
    };
  }

  static buildOfficialRandomEncounterEvent(campaign:CampaignState,hex:IndividualHex,cadence:'walking'|'stationary',rng:()=>number=Math.random,at?:{day:number;period:GamePeriod}):JourneyEvent {
    const terrain=JourneyRulesEngine.encounterTerrainForHex(hex);
    return this.buildOfficialRandomEncounterEventForTerrain(campaign,hex.id,String(terrain),cadence,rng,at);
  }

  private static recordOfficialEncounterSiteVisit(world:WorldState,event:JourneyEvent):{world:WorldState;event:JourneyEvent}{
    const mechanic=event.result?.mechanic as Record<string,unknown>|undefined;
    const encounterId=Number(mechanic?.encounterId);
    if(mechanic?.kind!=='official_random_encounter'||encounterId<1||encounterId>43||!event.hexId)
      return {world,event};
    const memory=JourneyRulesEngine.recordOfficialEncounterSiteVisit(world,event.hexId,encounterId);
    const visitEvent:JourneyEvent={...event,result:{...(event.result||{}),officialEncounterVisitNumber:memory.visitNumber}};
    if(encounterId===18){
      const detail=OfficialEncounterRulesEngine.specialScenePublicDetail(18,memory.visitNumber);
      if(detail)visitEvent.detail=`D66 ${String(event.result?.d66)} em ${String(event.result?.terrain)}: ${detail} Escolha a ação do grupo.`;
    }
    return {world:memory.world,event:visitEvent};
  }

  private static applyAutomaticOfficialSceneAttack(event:JourneyEvent,party:Character[],rng:()=>number=Math.random):JourneyEvent{
    const mechanic=event.result?.mechanic as Record<string,unknown>|undefined;
    if(mechanic?.kind!=='official_random_encounter')return event;
    const encounterId=Number(mechanic.encounterId);
    if(encounterId!==29&&encounterId!==30)return event;
    const visit=Number(event.result?.officialEncounterVisitNumber||1);
    const containsDwarf=party.some(item=>['anao','dwarf'].includes(String(item.kin||'').toLocaleLowerCase('pt-BR').normalize('NFD').replace(/[\u0300-\u036f]/g,'')));
    const automaticAttack=encounterId===29||(encounterId===30&&(visit>=3||(visit===2&&containsDwarf)));
    if(!automaticAttack)return event;
    const skeletonD6=encounterId===29?1+Math.floor(Math.max(0,Math.min(.999999999,rng()))*6):undefined;
    return {...event,result:{...(event.result||{}),officialSceneDecision:{version:1,
      encounterId,choiceId:'attack',stage:'pending_external_mechanics',pending:'combat',
      notes:'A agressão automática da visita oficial foi registrada pela cadência normal.',
      decidedAt:Date.now(),automaticResourcesChanged:false,automaticRollsApplied:false,automaticCombatStarted:false,
      ...(encounterId===29?{skeletonCountD6:skeletonD6,skeletonCount:Number(skeletonD6)+3}:{}),
      ...(encounterId===30?{teramaldaVisit:visit,teramaldaEncounterStage:OfficialEncounterRulesEngine.teramaldaEncounterStage(visit),
        teramaldaPursuesAfterFlee:visit>=4,attacksDwarves:visit===2&&containsDwarf,partyContainsDwarf:containsDwarf}:{})}}};
  }

  static async rollOfficialRandomEncounter(campaignId:string,cadence:'walking'|'stationary',rng:()=>number=Math.random):Promise<JourneyEvent>{
    const result=await withTransaction(['campaign','world_state','individual_hexes','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id||!campaign.currentHexId)throw new Error('Campanha ou hex atual não encontrado.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro atual antes de rolar outro encontro aleatório.');
      // Um encontro de jornada aberto deve ser resolvido antes de uma nova rolagem:
      // sem esta guarda, o botão de encontro podia ser usado repetidamente até obter
      // um resultado favorável, deixando as ocorrências anteriores sem consequência.
      if(world.journey.events.some(event=>event.status==='open' &&
        ['official_random_encounter','creature_encounter'].includes(String((event.result?.mechanic as Record<string,unknown>|undefined)?.kind||'')))){
        throw new Error('Resolva o encontro de jornada pendente antes de rolar outro encontro.');
      }
      const hex=await tx.objectStore('individual_hexes').get(campaign.currentHexId) as IndividualHex|undefined;
      if(!hex)throw new Error('Hex atual não encontrado.');
      const event=this.buildOfficialRandomEncounterEvent(campaign,hex,cadence,rng);
      const recorded=this.recordOfficialEncounterSiteVisit(world,event);
      const party=CampaignPartyService.members(campaign,await tx.objectStore('characters').getAll() as Character[]);
      const encounterEvent=this.applyAutomaticOfficialSceneAttack(recorded.event,party,rng);
      const next={...recorded.world,journey:{...recorded.world.journey,events:retainJourneyEvents([...recorded.world.journey.events,encounterEvent]),updatedAt:Date.now()},updatedAt:Date.now()};
      await worldStore.put(next);
      return {event:encounterEvent,world:next};
    });
    this.notify(result.world);
    return result.event;
  }

  static officialWildAnimals(){return JourneyRulesEngine.officialWildAnimals();}

  static async resolveOfficialRandomEncounterEvent(campaignId:string,eventId:string,resolutionInput:string):Promise<void>{
    const resolution=String(resolutionInput||'').trim().slice(0,800);
    if(!resolution)throw new Error('Registre como o encontro oficial foi resolvido antes de encerrá-lo.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter');
      if(!source)throw new Error('Encontro aleatório oficial não está pendente.');
      if([9,13,15,...OfficialEncounterRulesEngine.specialSceneIds()].includes(Number((source.result?.mechanic as Record<string,unknown>|undefined)?.encounterId))){
        throw new Error('Este encontro possui resolução oficial específica; use as ações próprias da ocorrência.');
      }
      if(world.encounter?.status==='active')throw new Error('Encerre o combate ativo antes de concluir o encontro de jornada.');
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,result:{...(item.result||{}),resolution,adjudicationMode:'manual',resolvedAt:Date.now()},detail:`${item.detail} Resolução registrada: ${resolution}`}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:Date.now()},updatedAt:Date.now()};
      await store.put(updated); return updated;
    });
    this.notify(next);
  }

  /** R79: uma transação única registra a escolha específica de cada cena, sem
   * fingir que rolagens, equipamentos, magia ou um combate ocorreram.
   * Escolhas com consequências pendentes NÃO liberam nova rolagem/jornada.
   */
  static async decideOfficialScene(campaignId:string,eventId:string,choiceId:string,notesInput:string=''):Promise<WorldState>{
    const notes=String(notesInput||'').trim();
    if(notes.length>800)throw new Error('A anotação da cena excede 800 caracteres.');
    const outcome=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate ativo antes de decidir a cena oficial.');
      const item=world.journey.events.find(event=>event.id===eventId&&event.status==='open'&&
        (event.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter');
      const id=Number((item?.result?.mechanic as Record<string,unknown>|undefined)?.encounterId);
      const siteVisit=Number(item?.result?.officialEncounterVisitNumber||1);
      const spec=OfficialEncounterRulesEngine.specialScene(id);
      const choice=spec?OfficialEncounterRulesEngine.specialSceneChoice(id,choiceId,siteVisit):null;
      if(!item||!spec||!choice)throw new Error('Ação inválida ou encontro oficial sem resolução específica pendente.');
      const previousDecision=item.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(previousDecision?.choiceId&&!(id===35&&previousDecision.stage==='karg_offer_refused'))
        throw new Error('A decisão deste encontro já foi registrada; não é possível repeti-la.');
      if(!item.hexId||item.hexId!==campaign.currentHexId)throw new Error('O grupo saiu do hex de origem da ocorrência.');
      const now=Date.now();
      const resolved=choice.closes;
      const processionStage=id===20
        ?['interfere','attack'].includes(choice.id)?'procession_warning':'procession_undisturbed'
        :undefined;
      const encounter32Stage=id===32&&choice.id==='leave_baby'?'rat_baby_unrescued_pending':
        id===32&&choice.id==='visit_belinda'?'belinda_response_pending':undefined;
      const encounter36Stage=id===36&&choice.id==='talk'?'hairless_lupines_met':
        id===36&&choice.id==='seek_cure'?'hairless_cave_search_pending':undefined;
      const encounter37Stage=id===37&&choice.id==='pay_toll'?'dwarf_toll_pending':undefined;
      const encounter38Stage=id===38&&choice.id==='contact'?'blue_plague_contact_pending':undefined;
      const encounter39Stage=id===39&&choice.id==='board'?'merchant_ship_contact':
        id===39&&choice.id==='continue'?'stowaway_hidden_aboard':undefined;
      const encounter41Stage=id===41&&choice.id==='board'?'soul_merchant_contact':undefined;
      const encounter42Stage=id===42&&choice.id==='parley'?'saurian_parley_pending':undefined;
      const encounter43Stage=id===43&&choice.id==='intervene'?'burial_rescue_pending':undefined;
      const resolution={version:1,encounterId:id,choiceId:choice.id,
        stage:encounter32Stage||encounter36Stage||encounter37Stage||encounter38Stage||encounter39Stage||encounter41Stage||encounter42Stage||encounter43Stage||processionStage||(resolved?'resolved':'pending_external_mechanics'),
        pending:id===20?'gm_decision':(choice.pending||null),notes,decidedAt:now,
        automaticResourcesChanged:false,automaticRollsApplied:false,automaticCombatStarted:false,
        ...(id===32?{ratCount:30,babyStatisticsInvented:false,ratStatisticsInvented:false}:{}),
        ...(id===32&&choice.id==='leave_baby'?{babyCryDeadlineUnspecified:true,babyRescued:false}:{}),
        ...(id===32&&choice.id==='rescue_baby'?{babyRescued:true,babyStatisticsInvented:false}:{}),
        ...(id===34&&['question_captain','help_escort'].includes(choice.id)?{ironGuardCountUnspecified:true,ironGuardStatisticsInvented:false}:{}),
        ...(id===35?{trollStrengthUnselected:true,trollOfferHasNoAutomaticOutcome:true}:{}),
        ...(id===36?{lupineCount:4,lupineCombatStatisticsDefined:false,crystalDiseaseVirulence:9,crystalDamageThreshold:4,hatchlingStartingStrength:4,hatchlingStartingArmor:3,hatchlingWeeklyStrengthGrowth:1,hatchlingWeeklyArmorGrowth:1}:{}),
        ...(id===37?{initialDwarfGuardCount:3,dwarfReinforcementCount:3,dwarfReinforcementDelayDie:'D6',dwarfTollSilverPerAdventurer:3}:{}),
        ...(id===38?{bluePlagueVirulence:5,linnayaCannotSpeak:true}:{}),
        ...(id===39&&choice.id==='continue'?{merogghaHiddenAboard:true,stowawaySecretNotPublic:true}:{}),
        ...(id===41?{soulBottleStockUnspecified:true,soulResurrectionD6Pending:true}:{}),
        ...(id===42?{saurianCountFormula:'party_size_plus_D6',sheeerzHumanHostilityOnly:true}:{}),
        ...(id===43?{familyMemberCount:4,familyStatisticsInvented:false,rescueNoRollInvented:true}:{}),
        ...(id===40?{giantOctopusProfileId:'core-bestiary:giant-octopus',tentacleTotalUnspecified:true,retreatAtHalfTentaclesCut:true}:{}),
        ...(id===20&&['interfere','attack'].includes(choice.id)?{processionWarning:'Vão embora, sacos de carne maldita!'}:{}),
        ...(id===10&&choice.id==='tolerate'?{foxFollowerActive:true}:{}),
        ...(id===35&&previousDecision?.stage==='karg_offer_refused'?{
          kargOfferHistory:[...(Array.isArray(previousDecision.kargOfferHistory)?previousDecision.kargOfferHistory:[]),
            {choiceId:previousDecision.choiceId,outcome:'refused',evidence:previousDecision.adjudicationEvidence}],
        }:{}),
      };
      const events=world.journey.events.map(event=>event.id===eventId?{
        ...event,status:resolved?'resolved' as const:'open' as const,
        // Nunca copiar gmDetail para o texto visível ao jogador.
        detail:`${event.detail} ${choice.narration}${notes?' Observações do grupo: '+notes:''}`,
        result:{...(event.result||{}),officialSceneDecision:resolution,
          ...(resolved?{resolvedAt:now,adjudicationMode:'official_scene_explicit_choice'}:{})},
      }:event);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      // R87 #2 e #10: personagens persistem no cadastro de NPCs, não apenas
      // numa fala descartável. Identidade secreta de Grelf só em campo da MdJ.
      if((id===2&&['watch','greet'].includes(choice.id))||
         (id===10&&['observe','tolerate'].includes(choice.id))||
         (id===16&&['help','rob'].includes(choice.id))||
         (id===17&&['attend','prevent_fight','join_fight'].includes(choice.id))||
         (id===20&&['respect','watch','interfere'].includes(choice.id))||
         (id===27&&choice.id==='comfort')||
         (id===31&&choice.id==='talk')||
         (id===32&&['rescue_baby','visit_belinda'].includes(choice.id))||
         (id===34&&choice.id==='question_captain')||id===35||
         (id===36&&['talk','seek_cure'].includes(choice.id))||
         (id===38&&choice.id==='contact')||
         (id===39&&['board','continue'].includes(choice.id))||
         (id===41&&choice.id==='board')||(id===42&&choice.id==='parley')||
         (id===43&&choice.id==='intervene')){
        const npcStore=tx.objectStore('npcs');
        const identities:Array<{key:string;name:string;kin:string;profession:string;goal:string;secret:string;activity:string;hidden?:boolean;combatStatisticsDefined?:boolean}>=id===41?[
          {key:'vergevil',name:'Vergevil',kin:'Humano',profession:'Mercador de almas',goal:'Vender sua mercadoria ou trocá-la por itens valiosos.',secret:'Transporta almas em garrafas de vinho; não sabe exatamente o que são.',activity:'A bordo de um pequeno veleiro com dez lanternas e uma lira.'},
          {key:'ginda',name:'Ginda',kin:'Humana',profession:'Guarda das mercadorias',goal:'Proteger a carga de Vergevil.',secret:'Irmã de Aneta; perfil de Soldado típico R88 p.183.',activity:'A bordo do veleiro.'},
          {key:'aneta',name:'Aneta',kin:'Humana',profession:'Guarda das mercadorias',goal:'Proteger a carga de Vergevil.',secret:'Irmã de Ginda; perfil de Soldado típico R88 p.183.',activity:'A bordo do veleiro.'},
        ]:id===42?[{key:'sheeerz',name:'Sheeerz',kin:'Sáurio',profession:'Caçador reptiliano',goal:'Encontrar porcos grandes.',
          secret:'Odeia humanos porque caçadores mataram seus pais; não é especialmente hostil às outras raças.',activity:'Caça no pântano com outros sáurios.'}]:id===43?[
          {key:'penazul',name:'Penazul',kin:'Elfa',profession:'Caçadora de recompensas',goal:'Executar a família Ylendidra sem levá-la a julgamento.',secret:'Capturou a família após ela abrigar orcs condenados; decidiu enterrá-los vivos depois que a mãe a atacou com uma faca.',activity:'Um dos sete caçadores no fosso.'},
          {key:'ylendidra-mother',name:'Mãe da família Ylendidra',kin:'Elfa',profession:'Cativa',goal:'Sobreviver ao enterramento vivo.',secret:'Atacou Penazul com uma faca.',activity:'Dentro do fosso.'},
          {key:'ylendidra-father',name:'Pai da família Ylendidra',kin:'Elfo',profession:'Cativo',goal:'Sobreviver ao enterramento vivo.',secret:'A R88 não fornece ficha ou nome próprio.',activity:'Dentro do fosso.'},
          ...[1,2].map(index=>({key:`ylendidra-child-${index}`,name:`Criança Ylendidra ${index}`,kin:'Elfo',profession:'Cativo',goal:'Sobreviver ao enterramento vivo.',secret:'A R88 não fornece ficha ou nome próprio.',activity:'Dentro do fosso.'})),
        ]:id===36?[
          {key:'abaken',name:'Abaken',kin:'Lupino',profession:'Líder sem pelos',goal:'Encontrar uma cura para a doença que fez os quatro perderem a pelagem.',
            secret:'Foi expulso da alcateia com três amigos após perder todos os pelos; encontrou cristais contaminados e um filhote de dragão negro.',activity:'Esconde-se nas montanhas à procura de ajuda.',combatStatisticsDefined:false},
          ...[1,2,3].map(index=>({key:`hairless-lupine-${index}`,name:`Amigo de Abaken ${index}`,kin:'Lupino',profession:'Lupino sem pelos',goal:'Encontrar uma cura para a doença que fez os quatro perderem a pelagem.',
            secret:'Um dos três lupinos expulsos da alcateia após perder todos os pelos.',activity:'Esconde-se nas montanhas com Abaken.',combatStatisticsDefined:false})),
        ]:id===38?[{key:'linnaya',name:'Linnaya',kin:'Humana',profession:'Doente com Praga Azul',goal:'Não especificado na R88.',
          secret:'Foi colocada à deriva porque tem Praga Azul, Virulência 5. Está fraca, coberta de pústulas e não consegue falar.',activity:'À deriva sob cobertores em um pequeno barco.'}]:
          id===39?choice.id==='continue'?[{key:'meroggha',name:'Meroggha',kin:'Goblin',profession:'Clandestino',goal:'Fugir do irmão brutal, líder de clã regional.',
            secret:'Escondeu-se a bordo do barco das aventureiras quando elas retomaram a viagem no lago.',activity:'Escondido no barco da companhia.',hidden:true}]:
            [{key:'captain-sertold',name:'Capitão Sertold',kin:'Humano',profession:'Capitão da nau Maçã Lunar',goal:'Levar uma carga de especiarias, carne e outras mercadorias a uma aldeia do outro lado do lago.',
              secret:'Não sabe que Meroggha é clandestino a bordo do Maçã Lunar.',activity:'Recebeu a visita das aventureiras a bordo.'}]:
          id===31?[{key:'porisia',name:'Porisia Garganta-de-prata',kin:'Anã',profession:'Aeda da pedra',
          goal:'Retomar a construção da Esfera do Mundo em honra a Vasto.',
          secret:'Lidera trabalhadores do clã Belderande na obra.',activity:'Mede e planeja a estrutura pétrea com ferramentas e mapas.'}]:
          id===32?choice.id==='rescue_baby'?[{key:'belinda-baby',name:'Bebê de Belinda',kin:'Halfling',profession:'Bebê',
            goal:'Não especificado na R88.',secret:'Sobreviveu sem ferimentos à queda do penhasco; Belinda é sua mãe.',
            activity:'Resgatado do meio dos ratos.'}]:[{key:'belinda',name:'Belinda',kin:'Halfling',profession:'Mãe do bebê abandonado',
            goal:'Não especificado na R88.',secret:'Lançou o bebê do penhasco. Finge não o reconhecer; pressionada, conta o que ocorreu.',
            activity:'Mora numa cabana próxima; a reação ao grupo ainda está pendente.'}]:
          id===34?[{key:'holmar',name:'Capitão Holmar',kin:'Humano',profession:'Capitão da Guarda de Ferro',
            goal:'Levar os mortos-vivos ao templo para “reeducação” e prender os aldeões por blasfêmia.',
            secret:'Finge ignorar a natureza dos artistas até revelar sua intenção.',activity:'Interroga a trupe e exige uma apresentação.'}]:
          id===35?[{key:'karg',name:'Karg',kin:'Troll',profession:'Troll da montanha',
            goal:'Bloquear a passagem até receber uma oferta aceita.',secret:'Gosta de coisas brilhantes; a R88 não fixa sua Força.',
            activity:'Acordou mal-humorado na caverna.'}]:id===2?[
          {key:'heldemin',name:choice.id==='greet'?'Heldemin':'Elfa tatuada',kin:'Elfa',profession:'Entalhadora de seiva viajante',
            goal:'Conhecer os costumes do clã Roka e preparar uma peça teatral com Alachran.',
            secret:'Amizade e parceria teatral controversa com Alachran.',activity:'Ensaiando duelo encenado.'},
          {key:'alachran',name:choice.id==='greet'?'Alachran':'Orc cantor',kin:'Orc',profession:'Ferreiro do clã Roka e cantor',
            goal:'Ensaiar espetáculo dramático com Heldemin.',
            secret:'A encenação parece um combate real, mas os dois são amigos.',activity:'Cantando e ensaiando com Heldemin.'},
        ]:id===10?[{key:'grelf',name:'Raposa curiosa',kin:'Raposa (forma observada)',profession:'Viajante curioso',
          goal:'Observar os aventureiros e descobrir suas histórias.',
          secret:'Grelf é um demônio capaz de assumir forma verdadeira; não revelar ao jogador automaticamente.',
          activity:choice.id==='tolerate'?'Acompanhando discretamente o grupo (sem deslocamento automático)':'Observando à distância.'}]
        :id===16?[{key:'andred',name:'Andred',kin:'Anão',profession:'Piloto de balão',
          goal:'Proteger seu balão e permanecer longe do chão.',
          secret:'Perdeu a família num desabamento e teme tocar o solo; tem perna quebrada. Possui três bestas carregadas e um machado; o conteúdo do balão não é saque do jogador.',
          activity:'Imobilizado com a perna quebrada no balão preso à árvore.'}]
        :id===17?[
          {key:'tregga',name:'Tregga',kin:'Orc',profession:'Noiva, ex-guerreira',
            goal:'Celebrar o casamento com Robert em paz.',secret:'Alguns convidados pretendem provocar uma briga; os culpados não são determinados automaticamente.',activity:'Celebrando o casamento.'},
          {key:'robert',name:'Robert',kin:'Humano',profession:'Noivo, ex-guerreiro',
            goal:'Celebrar o casamento com Tregga em paz.',secret:'Alguns convidados pretendem provocar uma briga; os culpados não são determinados automaticamente.',activity:'Celebrando o casamento.'}]
        :id===20?[{key:'ent-cortejo',name:'Ent do cortejo',kin:'Ent',profession:'Enlutado',
          goal:'Levar seu parente falecido sem interrupção.',secret:'Elfos ocultos escoltam a procissão; não inventar número nem atributos de combatentes.',activity:'Participando de procissão funerária.'}]
        :id===27?[{key:'linn',name:'Linn',kin:'Humana',profession:'Sobrevivente de Mataselvagem',
          goal:'Encontrar segurança após a devastação.',secret:'Única sobrevivente de desastre mágico entre feiticeiras; não revelar causa sem investigação.',activity:'Imóvel e muda, em estado de choque na clareira.'}]
        :[{key:'porisia',name:'Porisia Garganta-de-prata',kin:'Anã',profession:'Aeda da pedra',
          goal:'Retomar a construção da Esfera do Mundo.',secret:'Trabalha com o clã Belderande por devoção a Vasto; sem estatísticas de combate inventadas.',activity:'Supervisionando as medições da estrutura pétrea.'}];
        for(const identity of identities){
          const npcId=`official-npc:${campaignId}:${eventId}:${identity.key}`;
          if(await npcStore.get(npcId)){
            if(id===35&&identity.key==='karg')continue;
            throw new Error('NPC oficial já criado para esta ocorrência.');
          }
          await npcStore.put({id:npcId,campaignId,name:identity.name,kin:identity.kin,
            profession:identity.profession,currentLocationId:item.hexId,faction:identity.key==='alachran'?'Roka':'Independente',
            attitude:'Neutra',goals:identity.goal,fear:'Desconhecido',secret:identity.secret,
            relationshipToParty:identity.hidden?'Desconhecido pelo grupo':choice.id==='greet'?'Apresentados ao grupo':'Avistados pelo grupo',
            status:'Alive',playerNotes:'',gmNotes:identity.secret,firstSeenSceneId:eventId,
            lastSeenSceneId:eventId,currentActivity:identity.activity,immediateGoal:identity.goal,
            ...(identity.hidden?{hiddenFromParty:true,officialStowaway:true,officialFollowParty:true,officialFollowEventId:eventId}:{}),
            ...(identity.key==='grelf'&&choice.id==='tolerate'?{officialFollowParty:true,officialFollowEventId:eventId}:{}),
            memories:[{id:`official-memory:${eventId}:${identity.key}`,sceneId:eventId,
              summary:`Encontro oficial #${id}: ${choice.id}.`,valence:'neutral',createdAt:now}]});
        }
      }
      await store.put(updated);
      return updated;
    });
    this.notify(outcome);
    return outcome;
  }

  /** R90 #27 — acolhimento não equivale a cura, recrutamento como herói ou
   * teletransporte. A Mestra registra se Linn aceita acompanhar a companhia.
   * O NPC original e o recibo de jornada são alterados atomicamente.
   */
  static async resolveOfficialLinnShelter(campaignId:string,eventId:string,
    response:'accompany'|'remain',reasonInput:string):Promise<WorldState>{
    if(!['accompany','remain'].includes(response))throw new Error('Decisão sobre Linn inválida.');
    const reason=String(reasonInput||'').trim();
    if(reason.length<12||reason.length>600)throw new Error('A decisão sobre Linn exige justificativa de 12 a 600 caracteres.');
    const outcome=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede acolhimento de Linn.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==27||
        decision?.choiceId!=='comfort'||decision.stage!=='pending_external_mechanics'||
        event.hexId!==campaign.currentHexId)
        throw new Error('Linn não está aguardando acolhimento neste hex.');
      const npcs=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:linn`;
      const linn=await npcs.get(npcId);
      if(!linn||linn.campaignId!==campaignId||linn.status!=='Alive'||linn.currentLocationId!==event.hexId||
        linn.officialFollowParty===true)throw new Error('Linn original não está presente ou já foi acolhida.');
      const now=Date.now();const follows=response==='accompany';
      await npcs.put({...linn,officialFollowParty:follows,
        ...(follows?{officialFollowEventId:eventId}:{}),
        currentActivity:follows?'Acompanha o grupo em silêncio, ainda abalada; cuidados médicos não aplicados.':'Permanece na clareira, sem transporte ou cura automática.',
        relationshipToParty:follows?'Acolhida como viajante, sem virar personagem jogável.':'O grupo tentou acolhê-la e seguiu sem levá-la.',
        lastAction:follows?'Aceitou acompanhar a companhia.':'Permaneceu na clareira.',
        memories:[...(linn.memories||[]),{id:`official-linn-shelter:${eventId}`,sceneId:eventId,
          summary:follows?'O grupo acolheu Linn e ela aceitou viajar com eles.':'O grupo ofereceu acolhimento; Linn permaneceu na clareira.',
          valence:'neutral' as const,createdAt:now}]});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,status:'resolved' as const,
        detail:`${e.detail} ${follows?'Linn aceitou acompanhar a companhia.':'Linn permaneceu na clareira.'} Nenhuma cura foi presumida.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'linn_shelter_recorded',pending:null,
          linnFollowerActive:follows,linnResponse:response,linnReason:reason,linnShelterAt:now,
          automaticHealing:false,automaticCompanionRecruitment:false},
          resolvedAt:now,adjudicationMode:'official_linn_explicit_shelter'}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(outcome);return outcome;
  }

  /** R91 #27 — Livro do Mestre p.159: depois de D6 dias em que Linn
   * recebeu alimento REAL, ela consegue falar do desastre. Cada dia do
   * calendário da campanha conta apenas uma vez, sem relógio artificial.
   * Não atribui magia, ficha, cura ou controle da garota aos jogadores.
   */
  static async feedOfficialLinn(campaignId:string,eventId:string,characterId:string,
    foodSource:'provision'|'gear',foodId:string,rng:()=>number=Math.random,expectedSnapshot?:string):Promise<{world:WorldState;daysFed:number;daysRequired:number;disclosed:boolean}>{
    if(!['provision','gear'].includes(foodSource)||!foodId)throw new Error('Origem real de comida inválida.');
    const result=await withTransaction(['campaign','world_state','characters','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const day=Number(campaign.currentDay);
      if(!Number.isInteger(day)||day<1)throw new Error('A alimentação exige dia de campanha confirmado.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede alimentar Linn.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='resolved');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==27||
         decision?.linnResponse!=='accompany'||decision.linnFollowerActive!==true||decision.linnDisclosedAt)
        throw new Error('Linn deve acompanhar o grupo e ainda precisar recuperar a fala.');
      const npcs=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:linn`;
      const linn=await npcs.get(npcId);
      if(!linn||linn.campaignId!==campaignId||linn.status!=='Alive'||linn.officialFollowParty!==true||
         linn.officialFollowEventId!==eventId||linn.currentLocationId!==campaign.currentHexId)
        throw new Error('Linn original não está junto da companhia neste hex.');
      const fedDays=Array.isArray(decision.linnFedDays)?(decision.linnFedDays as number[]):[];
      if(fedDays.includes(day))throw new Error('Linn já recebeu alimento neste dia da campanha.');
      if(fedDays.some(d=>!Number.isInteger(d)||d>=day))throw new Error('Calendário de alimentação de Linn inconsistente.');
      const chars=tx.objectStore('characters');const character=await chars.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      let fed:Character;
      if(foodSource==='provision'){
        const provision=[...(character.journeyProvisions||[])];
        const index=provision.findIndex(batch=>batch.id===foodId&&batch.kind==='food'&&batch.quantity>=1);
        if(index<0)throw new Error('Comida preparada indisponível.');
        if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(provision[index],expectedSnapshot);
        if(provision[index].quantity===1)provision.splice(index,1);else provision[index]={...provision[index],quantity:provision[index].quantity-1};
        fed={...character,journeyProvisions:provision};
      }else{
        const gear=[...(character.gear||[])];const index=foodId.startsWith('gear:')?Number(foodId.slice(5)):gear.findIndex(item=>String(item?.id||'')===foodId);
        if(!Number.isInteger(index)||index<0||index>=gear.length)throw new Error('Ração física não encontrada.');
        const item=gear[index];if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(item,expectedSnapshot);const label=normalizeRuleName(String(item?.name||item?.label||''));
        if(!['comida','racao','racoes','provisao','provisoes','refeicao','refeicoes'].includes(label))throw new Error('Este objeto não é comida física reconhecida.');
        const quantity=Number(item.quantity??1);
        if(!Number.isInteger(quantity)||quantity<1)throw new Error('Quantidade de rações inválida.');
        if(quantity===1)gear.splice(index,1);else gear[index]={...item,quantity:quantity-1};
        fed={...character,gear};
      }
      fed={...fed,encumbrance:EncumbranceRulesEngine.calculate(fed)};
      const die=()=>{const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('D6 de recuperação inválido.');return 1+Math.floor(value*6);};
      const required=Number(decision.linnFeedingDaysRequired||die());
      if(!Number.isInteger(required)||required<1||required>6)throw new Error('Prazo de alimentação inconsistente.');
      const days=[...fedDays,day],disclosed=days.length>=required,now=Date.now();
      await chars.put(fed);
      await npcs.put({...linn,
        currentActivity:disclosed?'Consegue contar o que aconteceu em Mataselvagem, mas ainda precisa de acolhimento.':'Recebeu alimento; continua em estado de choque.',
        lastAction:`Recebeu uma ração de ${character.name} no dia ${day}.`,
        relationshipToParty:disclosed?'Acolhida e alimentada; confidenciou o desastre mágico.':linn.relationshipToParty,
        memories:[...(linn.memories||[]),{id:`official-linn-fed:${eventId}:${day}`,sceneId:eventId,
          summary:`Recebeu uma unidade real de comida no dia ${day}.`,valence:'positive' as const,createdAt:now},
          ...(disclosed?[{id:`official-linn-disclosed:${eventId}`,sceneId:eventId,
            summary:'Após dias alimentada, contou sobre o desastre mágico em Mataselvagem e seu desejo de aprender magia. Nenhuma capacidade de feitiçaria foi concedida.',
            valence:'neutral' as const,createdAt:now}]:[])]});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:disclosed?`${e.detail} Depois de ${days.length} dias alimentada, Linn finalmente contou o que ocorreu em Mataselvagem.`:
          `${e.detail} Linn foi alimentada no dia ${day}; ainda não consegue contar sua história.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,linnFedDays:days,
          linnFeedingDaysRequired:required,linnFeedingRequired:!disclosed,
          ...(disclosed?{linnDisclosedAt:now,linnMagicUncontrolled:true,linnHasLearnedSpells:false}:{}),
          linnLastFedCharacterId:characterId,linnLastFedAt:now,automaticResourcesChanged:true}}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return {world:updated,daysFed:days.length,daysRequired:required,disclosed};
    });this.notify(result.world);return result;
  }

  /** R90 #27 — despedida exige Linn viva e fisicamente no mesmo hex.
   * Impede que continue a viajar depois de deixá-la, sem apagar sua memória.
   */
  static async dismissOfficialLinn(campaignId:string,eventId:string):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede despedida.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='resolved');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==27||
        decision?.linnFollowerActive!==true)throw new Error('Linn não acompanha a companhia.');
      const npcs=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:linn`;
      const linn=await npcs.get(npcId);
      if(!linn||linn.campaignId!==campaignId||linn.status!=='Alive'||linn.officialFollowParty!==true||
        linn.currentLocationId!==campaign.currentHexId)throw new Error('Linn precisa estar viva e presente para despedida.');
      const now=Date.now();await npcs.put({...linn,officialFollowParty:false,
        currentActivity:'Permanece no hex em que a companhia se despediu.',lastAction:'Despediu-se da companhia.',
        memories:[...(linn.memories||[]),{id:`official-linn-farewell:${eventId}`,sceneId:eventId,
          summary:'Acompanhamento encerrado; permaneceu no hex atual.',valence:'neutral' as const,createdAt:now}]});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        result:{...(e.result||{}),officialSceneDecision:{...decision,linnFollowerActive:false,linnDismissedAt:now}}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(outcome);return outcome;
  }

  /** R88 #36: open the nearby cave quest without moving the party or inventing
   * its map coordinates. The crystal approach remains a separate action. */
  static async openOfficialHairlessLupineCave(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de explorar a caverna.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==36||
        !['hairless_lupines_met','hairless_cave_search_pending'].includes(String(decision?.stage))||
        !event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('A caverna só pode ser registrada após conversar com os lupinos neste hex.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        detail:`${item.detail} A companhia aceitou investigar a caverna subterrânea próxima.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'hairless_cave_open',pending:'item_or_skill',
          caveOpenedAt:now,crystalDiseaseVirulence:9,crystalExposureRepeatsEachRound:true,crystalDamageThreshold:4,
          crystalBroken:false,hatchlingHatched:false}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #37: the toll is 3 silver from each living adventurer. It is debited
   * atomically from actual character coin balances before the scene closes. */
  static async payOfficialRoadBlockadeToll(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de pagar o pedágio.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==37||decision?.choiceId!=='pay_toll'||
        decision.stage!=='dwarf_toll_pending'||!event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('O pedágio do bloqueio não está pendente neste hex.');
      const characters=tx.objectStore('characters');
      const members=CampaignPartyService.members(campaign,await characters.getAll() as Character[]).filter(c=>c.status!=='dead');
      if(!members.length)throw new Error('Não há aventureiras vivas para pagar o pedágio.');
      const costCopper=30;
      const debited=[] as Array<{characterId:string;name:string;before:Character['coins'];after:Character['coins']}>
      for(const character of members){
        const available=ForbiddenLandsEconomyService.coinsToCopper(character.coins);
        if(available<costCopper)throw new Error(`${character.name} não tem 3 pratas para pagar a própria passagem.`);
        const after=ForbiddenLandsEconomyService.copperToCoins(available-costCopper);
        const updated={...character,coins:after};
        updated.encumbrance=EncumbranceRulesEngine.calculate(updated);
        await characters.put(updated);
        debited.push({characterId:character.id,name:character.name,before:{...character.coins},after});
      }
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,status:'resolved' as const,
        detail:`${item.detail} O pedágio de 3 pratas por aventureira foi pago.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'dwarf_toll_paid',pending:null,
          tollSilverPerAdventurer:3,tollSilverTotal:3*members.length,tollPayers:debited,paidAt:now,resolvedAt:now},
          resolvedAt:now,adjudicationMode:'official_dwarf_toll_coin_transfer'}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #36: leave the cave without claiming the disease was cured. */
  static async leaveOfficialHairlessLupineCave(campaignId:string,eventId:string,leaveHatchlingUnbonded=false):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de deixar a caverna.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||mechanic?.kind!=='official_random_encounter'||Number(mechanic.encounterId)!==36||
        !['hairless_cave_open','hairless_hatchling_hatched'].includes(String(decision?.stage))||
        !event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('A caverna deste encontro não está aberta neste hex.');
      if(decision.hatchlingHatched===true&&!decision.hatchlingBondAttempt&&!leaveHatchlingUnbonded)
        throw new Error('Registre Adestramento ou confirme explicitamente que o grupo deixa o filhote sem tentar criar vínculo.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,status:'resolved' as const,
        detail:`${item.detail} O grupo deixou a caverna; nenhuma cura foi presumida.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'hairless_cave_left',pending:null,
          hatchlingLeftUnbonded:decision.hatchlingHatched===true&&!decision.hatchlingBondAttempt,
          cureClaimed:false,caveLeftAt:now,resolvedAt:now},resolvedAt:now,adjudicationMode:'official_cave_departure'}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #38: record leaving Linnaya after any actual contacts. Existing
   * infections remain on the character sheets and continue through disease rules. */
  static async leaveOfficialBluePlagueScene(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de deixar Linnaya.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||mechanic?.kind!=='official_random_encounter'||Number(mechanic.encounterId)!==38||
        decision?.choiceId!=='contact'||decision.stage!=='blue_plague_contact_pending'||
        !event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('A cena de Linnaya não está aberta para encerramento neste hex.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,status:'resolved' as const,
        detail:`${item.detail} O grupo deixou o barco; contatos e infecções registrados foram preservados.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'blue_plague_scene_left',pending:null,leftAt:now,resolvedAt:now},
          resolvedAt:now,adjudicationMode:'official_blue_plague_contact_departure'}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #39: interactions do not create merchant goods, meals or prices that
   * the book never specifies. Continuing after a visit registers the hidden stowaway. */
  static async resolveOfficialMerchantShipContact(campaignId:string,eventId:string,
    action:'stories'|'trade'|'dinner'|'depart',notesInput=''):Promise<WorldState>{
    const notes=String(notesInput||'').trim();
    if(action!=='depart'&&(notes.length<12||notes.length>600))
      throw new Error('Registre a interação da tripulação em 12–600 caracteres.');
    if(notes.length>600)throw new Error('A anotação excede 600 caracteres.');
    const next=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de deixar o navio mercante.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||mechanic?.kind!=='official_random_encounter'||Number(mechanic.encounterId)!==39||
        decision?.choiceId!=='board'||decision.stage!=='merchant_ship_contact'||
        !event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('O Maçã Lunar não está com uma visita em andamento neste hex.');
      const now=Date.now();
      const interactions=Array.isArray(decision.merchantShipInteractions)?decision.merchantShipInteractions as Array<Record<string,unknown>>:[];
      const interaction=action==='depart'?undefined:{action,notes,recordedAt:now,
        automaticInventoryTransfer:false,automaticMealOrRest:false,automaticTimeAdvance:false};
      const nextDecision={...decision,
        ...(interaction?{merchantShipInteractions:[...interactions,interaction]}:{}),
        ...(action==='depart'?{stage:'stowaway_hidden_aboard',pending:null,merogghaHiddenAboard:true,stowawaySecretNotPublic:true,departedAt:now,resolvedAt:now}:{}),
        ...(action!=='depart'?{stage:'merchant_ship_contact',pending:'follow_up'}:{})};
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        ...(action==='depart'?{status:'resolved' as const,detail:`${item.detail} A companhia retomou a viagem.`,resolvedAt:now,adjudicationMode:'official_merchant_ship_departure'}:{}),
        detail:action==='depart'?`${item.detail} A companhia retomou a viagem.`:`${item.detail} Interação com a tripulação registrada: ${notes}`,
        result:{...(item.result||{}),officialSceneDecision:nextDecision,...(action==='depart'?{resolvedAt:now}:{})}});
      if(action==='depart'){
        const npcStore=tx.objectStore('npcs');
        const npcId=`official-npc:${campaignId}:${eventId}:meroggha`;
        if(!await npcStore.get(npcId))await npcStore.put({id:npcId,campaignId,name:'Meroggha',kin:'Goblin',profession:'Clandestino',
          currentLocationId:event.hexId,faction:'Independente',attitude:'Neutra',
          goals:'Fugir do irmão brutal, líder de clã regional.',fear:'Desconhecido',
          secret:'Escondeu-se a bordo do barco das aventureiras quando elas retomaram a viagem no lago.',
          relationshipToParty:'Desconhecido pelo grupo',status:'Alive',playerNotes:'',
          gmNotes:'Clandestino oficial R88 #39; segredo não deve ser revelado automaticamente.',firstSeenSceneId:eventId,lastSeenSceneId:eventId,
          currentActivity:'Escondido no barco da companhia.',immediateGoal:'Fugir do irmão brutal.',hiddenFromParty:true,officialStowaway:true,
          memories:[{id:`official-memory:${eventId}:meroggha`,sceneId:eventId,summary:'R88 #39: escondeu-se no barco quando a viagem recomeçou.',valence:'neutral',createdAt:now}]});
      }
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #36 crystal exposure and #38 Blue Plague contact. Each real exposure
   * rolls Strength/Resiliência against the printed virulence and persists both
   * rolls with the originating encounter. A #36 check is unique per character
   * per recorded close-to-crystal round. */
  static async resolveOfficialEncounterDiseaseExposure(campaignId:string,eventId:string,characterId:string,
    exposure:'hairless_crystal'|'blue_plague_contact',roundNumber=1,rng:()=>number=Math.random):Promise<{
      world:WorldState;infected:boolean;skippedBecauseAlreadySick:boolean;patientRoll?:import('../types').DiceRoll;diseaseRoll?:import('../types').DiceRoll;
    }>{
    if(exposure==='hairless_crystal'&&(!Number.isInteger(roundNumber)||roundNumber<1))throw new Error('Rodada de exposição inválida.');
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de registrar a exposição.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const id=Number(mechanic?.encounterId);
      const valid=id===36&&exposure==='hairless_crystal'&&decision?.stage==='hairless_cave_open'&&decision.crystalBroken!==true||
        id===38&&exposure==='blue_plague_contact'&&decision?.choiceId==='contact'&&decision.stage==='blue_plague_contact_pending';
      if(!event||mechanic?.kind!=='official_random_encounter'||!valid||!event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('Esta exposição não pertence a uma cena oficial aberta neste hex.');
      const characters=tx.objectStore('characters');
      const character=await characters.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      if(!character||character.status==='dead')throw new Error('Aventureira viva não encontrada.');
      const previousChecks=Array.isArray(decision?.crystalExposureChecks)?decision.crystalExposureChecks as Array<{characterId:string;round:number}>:[];
      const previousContacts=Array.isArray(decision?.bluePlagueContactCharacterIds)?decision.bluePlagueContactCharacterIds as string[]:[];
      if(exposure==='hairless_crystal'&&previousChecks.some(check=>check.characterId===characterId&&Number(check.round)===roundNumber))
        throw new Error(`${character.name} já rolou exposição aos cristais na rodada ${roundNumber}.`);
      if(exposure==='blue_plague_contact'&&previousContacts.includes(characterId))
        throw new Error(`${character.name} já teve contato registrado com Linnaya.`);
      const virulence=exposure==='hairless_crystal'?9:5;
      let updatedCharacter=character;let patientRoll:import('../types').DiceRoll|undefined;let diseaseRoll:import('../types').DiceRoll|undefined;
      const skipped=Boolean(character.diseaseState);
      if(!skipped){
        const sceneId=`official-encounter:${eventId}`;
        patientRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,
          skill:normalizedSkill(character,['Resilience','Resiliência']),gear:0},
          {characterId,sceneId,checkId:`${exposure}:${characterId}:${exposure==='hairless_crystal'?roundNumber:'contact'}`,
            rng,criticalInjuries:character.criticalInjuryStates,skillName:'resilience',attributeName:'strength'});
        diseaseRoll=ForbiddenLandsDiceEngine.roll({base:virulence,skill:0,gear:0},
          {characterId:null,sceneId,checkId:`${exposure}-virulence:${eventId}:${characterId}:${exposure==='hairless_crystal'?roundNumber:'contact'}`,rng});
        updatedCharacter=DiseaseRulesEngine.expose(character,virulence,patientRoll.successes,diseaseRoll.successes);
        await characters.put(updatedCharacter);
        await tx.objectStore('rolls').put(patientRoll);await tx.objectStore('rolls').put(diseaseRoll);
      }
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        detail:`${item.detail} Exposição de ${character.name} registrada${skipped?' (doença já presente, preservada)':updatedCharacter.diseaseState?' (doença em incubação)':' (resistiu)'}.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,
          ...(exposure==='hairless_crystal'?{crystalExposureChecks:[...previousChecks,{characterId,round:roundNumber,virulence,infected:Boolean(updatedCharacter.diseaseState),skippedBecauseAlreadySick:skipped,at:now}]}:{}),
          ...(exposure==='blue_plague_contact'?{bluePlagueContactCharacterIds:[...previousContacts,characterId],bluePlagueVirulence:5,bluePlagueExposure:{characterId,infected:Boolean(updatedCharacter.diseaseState),skippedBecauseAlreadySick:skipped,at:now}}:{}),
          lastDiseaseExposureAt:now}}});
      const updatedWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updatedWorld);
      return{world:updatedWorld,infected:Boolean(updatedCharacter.diseaseState),skippedBecauseAlreadySick:skipped,
        ...(patientRoll?{patientRoll}:{}),...(diseaseRoll?{diseaseRoll}:{})};
    });this.notify(outcome.world);return outcome;
  }

  /** R88 #36: a real weapon attack must deal at least four damage to break the
   * crystal. No crystal armor or durability is invented by this resolver. */
  static async strikeOfficialHairlessDragonCrystal(campaignId:string,eventId:string,characterId:string,weaponIndex:number,
    roundNumber=1,rng:()=>number=Math.random):Promise<{world:WorldState;roll:import('../types').DiceRoll;damage:number;crystalBroken:boolean}>{
    if(!Number.isInteger(roundNumber)||roundNumber<1)throw new Error('Rodada do ataque ao cristal inválida.');
    const outcome=await withTransaction(['campaign','world_state','characters','npcs','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de atacar o cristal.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==36||decision?.stage!=='hairless_cave_open'||
        decision.crystalBroken===true||!event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('O cristal não está disponível para ataque neste encontro.');
      const character=await tx.objectStore('characters').get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      if(!character||character.status==='dead')throw new Error('Aventureira viva não encontrada.');
      CharacterActionRulesEngine.assertCanAct(character);
      const weapon=character.weapons?.[weaponIndex] as Record<string,unknown>|undefined;
      if(!weapon||!Number.isInteger(weaponIndex)||weaponIndex<0)throw new Error('Escolha uma arma real do inventário.');
      const weaponDamage=Number(weapon.damage??weapon.weaponDamage??weapon.damageValue);
      if(!Number.isFinite(weaponDamage)||weaponDamage<0)throw new Error('A arma selecionada não tem dano canônico registrado na ficha.');
      const previous=Array.isArray(decision.crystalStrikeChecks)?decision.crystalStrikeChecks as Array<{characterId:string;round:number}>:[];
      if(previous.some(check=>check.characterId===characterId&&Number(check.round)===roundNumber))
        throw new Error(`${character.name} já atacou o cristal na rodada ${roundNumber}.`);
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,
        skill:normalizedSkill(character,['Melee','Luta']),gear:0},
        {characterId,sceneId:`official-encounter:${eventId}`,checkId:`hairless-crystal-strike:${characterId}:${roundNumber}`,
          rng,criticalInjuries:character.criticalInjuryStates,skillName:'melee',attributeName:'strength'});
      await tx.objectStore('rolls').put(roll);
      const damage=CombatRulesEngine.resolveAttack({attackSuccesses:roll.successes,weaponDamage,armorValue:0,rng}).finalDamage;
      const crystalBroken=damage>=4;
      const now=Date.now();
      if(crystalBroken){
        const npcStore=tx.objectStore('npcs');
        const npcId=`official-npc:${campaignId}:${eventId}:black-dragon-hatchling`;
        if(await npcStore.get(npcId))throw new Error('O filhote desta ocorrência já foi registrado.');
        await npcStore.put({id:npcId,campaignId,name:'Filhote de dragão negro',kin:'Dragão',profession:'Filhote eclodido',
          currentLocationId:event.hexId,faction:'Independente',attitude:'Neutra',goals:'Não especificado na R88.',fear:'Desconhecido',
          secret:'Arma secreta da Feiticeira Jagsatin; estava adormecido em um cristal contaminado.',relationshipToParty:'Recém-eclodido; vínculo ainda não realizado.',
          status:'Alive',playerNotes:'',gmNotes:'Guia do Mestre R88 encontro #36 p.163 e Dragão p.84.',firstSeenSceneId:eventId,lastSeenSceneId:eventId,
          currentActivity:'Eclodiu após o cristal ser quebrado.',immediateGoal:'Não especificado na R88.',
          officialHatchling:true,officialHatchlingState:{strength:4,armor:3,hatchDay:Number(campaign.currentDay||1),growthAppliedThroughDay:Number(campaign.currentDay||1),
            strengthPerWeek:1,armorPerWeek:1,fullSizeMaximumUnspecified:true},
          quickSheet:{attributes:{strength:{current:4,max:4}},armor:3,officialHatchling:true,source:'Guia do Mestre R88, encontro #36 p.163',sourcePrintedPage:163,
            weeklyStrengthGrowth:1,weeklyArmorGrowth:1}});
      }
      const checks=[...previous,{characterId,round:roundNumber,weaponIndex,weaponName:String(weapon.name||'Arma'),rollId:roll.id,
        attackSuccesses:roll.successes,damage,crystalBroken,at:now}];
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        detail:`${item.detail} ${character.name} atacou o cristal com ${String(weapon.name||'uma arma')}: ${damage} dano${crystalBroken?' e o cristal se rompeu':''}.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,crystalStrikeChecks:checks,
          ...(crystalBroken?{crystalBroken:true,hatchlingHatched:true,hatchlingNpcId:`official-npc:${campaignId}:${eventId}:black-dragon-hatchling`,hatchlingHatchedAtDay:Number(campaign.currentDay||1),stage:'hairless_hatchling_hatched',pending:'follow_up'}:{}),
          lastCrystalStrikeAt:now}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return{world:updated,roll,damage,crystalBroken};
    });this.notify(outcome.world);return outcome;
  }

  /** R88 #36: a successful Animal Handling/Adestramento test creates the
   * first bond; failure is recorded without inventing another consequence. */
  static async bondOfficialHairlessDragonHatchling(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<{
    world:WorldState;roll:import('../types').DiceRoll;bonded:boolean;
  }>{
    const outcome=await withTransaction(['campaign','world_state','characters','npcs','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de tentar criar o vínculo.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==36||decision?.crystalBroken!==true||
        !event.hexId||event.hexId!==campaign.currentHexId)throw new Error('O filhote ainda não eclodiu neste hex.');
      const character=await tx.objectStore('characters').get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      if(!character||character.status==='dead')throw new Error('Aventureira viva não encontrada.');
      CharacterActionRulesEngine.assertCanAct(character);
      if(decision.hatchlingBondCharacterId)throw new Error('O filhote já tem um vínculo registrado nesta ocorrência.');
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.empathy.current,
        skill:normalizedSkill(character,['Animal Handling','Adestramento']),gear:0},
        {characterId,sceneId:`official-encounter:${eventId}`,checkId:`hairless-hatchling-bond:${characterId}`,
          rng,criticalInjuries:character.criticalInjuryStates,skillName:'animal_handling',attributeName:'empathy'});
      await tx.objectStore('rolls').put(roll);
      const bonded=roll.successes>0;const now=Date.now();
      if(bonded){
        const npcStore=tx.objectStore('npcs');const npc=await npcStore.get(String(decision.hatchlingNpcId||''));
        if(!npc)throw new Error('Ficha persistente do filhote ausente.');
        await npcStore.put({...npc,relationshipToParty:`Vínculo com ${character.name}`,
          bondedCharacterId:characterId,officialFollowParty:true,currentActivity:'Ligado à aventureira que obteve sucesso em Adestramento.',
          officialHatchlingState:{...(npc.officialHatchlingState||{}),bondedCharacterId:characterId,bondedAt:now}});
      }
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        detail:`${item.detail} ${character.name} testou Adestramento (${roll.successes} sucesso(s)); ${bonded?'o filhote criou vínculo':'nenhum vínculo foi criado'}.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,hatchlingBondAttempt:{characterId,rollId:roll.id,successes:roll.successes,bonded,at:now},
          ...(bonded?{hatchlingBondCharacterId:characterId}:{}),lastHatchlingBondAt:now}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);
      return{world:updated,roll,bonded};
    });this.notify(outcome.world);return outcome;
  }

  /** Apply only complete in-game weeks since the hatch day: +1 Strength and
   * +1 armor per week, exactly as R88 #36 states. */
  static async applyOfficialHairlessDragonWeeklyGrowth(campaignId:string,eventId:string):Promise<{world:WorldState;weeksApplied:number;strength:number;armor:number}>{
    const outcome=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&['open','resolved'].includes(item.status));
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==36||decision?.crystalBroken!==true)
        throw new Error('O filhote não foi eclodido neste encontro.');
      const npcStore=tx.objectStore('npcs');const npc=await npcStore.get(String(decision.hatchlingNpcId||''));
      if(!npc?.officialHatchlingState)throw new Error('Ficha de crescimento do filhote ausente.');
      const state=npc.officialHatchlingState as Record<string,unknown>;
      const through=Math.max(0,Math.trunc(Number(state.growthAppliedThroughDay)||Number(state.hatchDay)||1));
      const elapsed=Math.max(0,Math.trunc(Number(campaign.currentDay||1))-through);
      const weeksApplied=Math.floor(elapsed/7);
      if(!weeksApplied)throw new Error('Ainda não se completou uma semana de campanha desde o último crescimento.');
      const strength=Math.max(4,Math.trunc(Number(state.strength)||4))+weeksApplied;
      const armor=Math.max(3,Math.trunc(Number(state.armor)||3))+weeksApplied;
      const growthAppliedThroughDay=through+weeksApplied*7;const now=Date.now();
      const nextState={...state,strength,armor,growthAppliedThroughDay};
      await npcStore.put({...npc,officialHatchlingState:nextState,
        quickSheet:{...(npc.quickSheet||{}),attributes:{strength:{current:strength,max:strength}},armor}});
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        result:{...(item.result||{}),officialSceneDecision:{...decision,hatchlingGrowth:{weeksApplied,strength,armor,growthAppliedThroughDay,at:now}}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);
      return{world:updated,weeksApplied,strength,armor};
    });this.notify(outcome.world);return outcome;
  }

  /** R88 #40: the tentacle count is deliberately supplied by the GM because
   * the printed encounter says “half” but does not state a total. */
  static async recordOfficialGiantOctopusRetreat(campaignId:string,eventId:string,tentaclesCut:number,tentacleTotal:number,evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim();
    if(!Number.isInteger(tentaclesCut)||!Number.isInteger(tentacleTotal)||tentacleTotal<1||tentaclesCut<1||tentaclesCut>tentacleTotal||tentaclesCut*2<tentacleTotal)
      throw new Error('O polvo só recua quando o número registrado de tentáculos cortados é pelo menos metade do total.');
    if(evidence.length<8||evidence.length>500)throw new Error('Registre a adjudicação do corte dos tentáculos (8–500 caracteres).');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const encounter=world.encounter;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==40||decision?.stage!=='combat_active'||
        encounter?.status!=='active'||encounter.startedSceneId!==eventId||
        !encounter.combatants.some(item=>item.entityId==='core-bestiary:giant-octopus'))
        throw new Error('Não há combate oficial ativo do Polvo Gigante ligado a esta ocorrência.');
      const octopus=encounter.combatants.find(item=>item.entityId==='core-bestiary:giant-octopus');
      if(!octopus||!['active','broken'].includes(octopus.status))throw new Error('O Polvo Gigante não está ativo para recuar.');
      const now=Date.now();
      const combatants=encounter.combatants.map(item=>item.id!==octopus.id?item:{...item,status:'fled' as const,fastActionAvailable:false,slowActionAvailable:false,
        notes:`${item.notes} Recuou para as profundezas quando ${tentaclesCut} de ${tentacleTotal} tentáculos foram cortados (adjudicação da Mestra).`});
      const resolved=EncounterService.resolveIfFinished({...encounter,combatants,log:[...encounter.log,
        {id:crypto.randomUUID(),round:encounter.round,combatantId:octopus.id,text:`O Polvo Gigante fugiu para as profundezas após a Mestra confirmar ${tentaclesCut}/${tentacleTotal} tentáculos cortados.`,createdAt:now}].slice(-300),updatedAt:now},now);
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        result:{...(item.result||{}),officialSceneDecision:{...decision,octopusRetreated:true,octopusTentaclesCut:tentaclesCut,
          octopusTentacleTotal:tentacleTotal,octopusRetreatEvidence:evidence,octopusRetreatedAt:now}}});
      const updated={...world,encounter:resolved,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #20: a advertência precede o combate; presentes continuam decisão da Mestra. */
  static async advanceOfficialFuneralProcession(campaignId:string,eventId:string,
    action:'leave'|'attack'|'record_gift',notesInput=''):Promise<WorldState>{
    const notes=String(notesInput||'').trim();
    if(notes.length>600)throw new Error('A adjudicação do presente excede 600 caracteres.');
    if(action==='record_gift'&&notes.length<12)
      throw new Error('Registre o presente escolhido pela Mestra (12–600 caracteres); não há item automático.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate ativo antes de avançar a procissão.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const stage=String(decision?.stage||'');
      if(!event||mechanic?.kind!=='official_random_encounter'||Number(mechanic.encounterId)!==20||
        !event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('A procissão fúnebre não está aberta neste hex.');
      if(action==='leave'&&!['procession_warning','procession_undisturbed'].includes(stage))
        throw new Error('O grupo só pode deixar a procissão depois de observá-la ou receber a advertência.');
      if(action==='attack'&&stage!=='procession_warning')
        throw new Error('A luta só começa depois que a procissão é perturbada e os elfos dão a advertência.');
      if(action==='record_gift'&&stage!=='procession_undisturbed')
        throw new Error('Um presente só pode ser adjudicado se o grupo não interferiu na procissão.');
      const now=Date.now();
      const resolved=action!=='attack';
      const nextStage=action==='attack'?'pending_external_mechanics':action==='leave'?'procession_departed':'procession_gift_recorded';
      const message=action==='attack'
        ?'O grupo ignorou a advertência e atacou. Os Ents pousaram o corpo do parente; os cinco Ents vivos entraram em combate.'
        :action==='leave'
          ?'O grupo deixou a procissão sem interferir mais.'
          :`Presente adjudicado pela Mestra: ${notes}. O registro não cria nem transfere item automaticamente.`;
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        ...(resolved?{status:'resolved' as const}:{}),detail:`${item.detail} ${message}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,
          choiceId:action==='attack'?'attack':decision?.choiceId,stage:nextStage,
          pending:action==='attack'?'combat':null,processionAction:action,processionUpdatedAt:now,
          ...(action==='attack'?{warningHeard:true,deadEntBodySetDown:true,combatantCount:5}:{}),
          ...(action==='record_gift'?{giftAdjudication:notes,itemTransferredAutomatically:false}:{}),
          ...(resolved?{resolvedAt:now}:{})},
        ...(resolved?{resolvedAt:now,adjudicationMode:action==='record_gift'?'official_funeral_procession_gm_gift':'official_funeral_procession_departure'}:{})}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });
    this.notify(next);return next;
  }

  /** R88 #3: a procissão pode ser abordada sem combate. O Guia do Mestre
   * não determina a resposta da escolta: a Mestra decide explicitamente.
   * A libertação altera o NPC Sargah real; não concede ficha inventada.
   */
  static async resolveOfficialOrcParley(campaignId:string,eventId:string,
    response:'release_sargah'|'escort_continues',reasonInput:string):Promise<WorldState>{
    if(!['release_sargah','escort_continues'].includes(response))throw new Error('Resposta inválida da escolta.');
    const reason=String(reasonInput||'').trim();
    if(reason.length<12||reason.length>600)throw new Error('A Mestra deve registrar o motivo da decisão (12–600 caracteres).');
    const next=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Não resolva uma conversa enquanto há combate ativo.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==3||
        decision?.choiceId!=='parley'||decision.stage!=='pending_external_mechanics'||decision.pending!=='gm_decision')
        throw new Error('Esta escolta não aguarda negociação oficial.');
      if(!event.hexId||event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex da procissão.');
      const now=Date.now();const released=response==='release_sargah';
      if(released){
        const npcStore=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:sargah`;
        if(await npcStore.get(npcId))throw new Error('Sargah já foi libertado nesta ocorrência.');
        await npcStore.put({id:npcId,campaignId,name:'Sargah',kin:'Orc',profession:'Fugitivo do próprio clã',
          currentLocationId:event.hexId,faction:'Independente',attitude:'Cauteloso',
          goals:'Buscar o próprio destino nas Terras Proibidas.',fear:'Ser reconduzido à força ao clã.',
          secret:'',relationshipToParty:'Libertado após negociação com a escolta.',status:'Alive',
          playerNotes:'',gmNotes:'O livro não fornece atributos próprios para Sargah; não inventar ficha.',
          firstSeenSceneId:eventId,lastSeenSceneId:eventId,currentActivity:'Livre e capaz de escolher o próprio caminho.',
          memories:[{id:`official-orc-parley:${eventId}`,sceneId:eventId,
            summary:'A escolta aceitou libertá-lo durante a negociação.',valence:'positive',createdAt:now}]});
      }
      const events=world.journey.events.map(e=>e.id===eventId?{...e,status:'resolved' as const,
        detail:`${e.detail} A Mestra registrou a resposta da escolta: ${released?'Sargah foi solto.':'A procissão prosseguiu com Sargah ainda amarrado.'}`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'orc_parley_resolved',
          pending:null,orcParleyResponse:response,orcParleyReason:reason,orcParleyAt:now,
          sargahReleased:released},resolvedAt:now,adjudicationMode:'official_orc_parley_explicit_gm'}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #2: o grupo pode se apresentar DEPOIS de observar o ensaio.
   * Atualiza os NPCs originais; nunca duplica personagens nem simula combate.
   */
  static async greetOfficialDuelistsAfterWatching(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Conclua o combate antes de cumprimentar os artistas.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='resolved');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==2||
        decision?.choiceId!=='watch'||decision.duelistsIntroducedAt)
        throw new Error('Apresentação disponível apenas depois de observar o duelo, uma vez.');
      if(event.hexId!==campaign.currentHexId)throw new Error('O grupo não está mais junto dos artistas.');
      const npcs=tx.objectStore('npcs');const now=Date.now();
      for(const [key,name] of [['heldemin','Heldemin'],['alachran','Alachran']] as const){
        const id=`official-npc:${campaignId}:${eventId}:${key}`;
        const npc=await npcs.get(id);
        if(!npc||npc.campaignId!==campaignId||npc.currentLocationId!==event.hexId||npc.status!=='Alive')
          throw new Error('Artista original indisponível; não criar uma cópia fictícia.');
        await npcs.put({...npc,name,relationshipToParty:'Apresentados ao grupo',
          memories:[...(npc.memories||[]),{id:`official-greeting:${eventId}:${key}`,sceneId:eventId,
            summary:'Os aventureiros cumprimentaram os artistas após assistir ao ensaio.',valence:'positive',createdAt:now}]});
      }
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} Os artistas se apresentaram como Heldemin e Alachran; o duelo era um ensaio teatral.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,duelistsIntroducedAt:now}}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #14, Guia do Mestre p. 153: observar o símbolo da procissão exige
   * Discernimento real; a falha mantém apenas o efeito perceptivo enquanto
   * o símbolo estiver à vista. Nenhum recurso ou atributo é alterado.
   */
  static async inspectOfficialPenitentSymbol(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<{world:WorldState;roll:import('../types').DiceRoll;affected:boolean}>{
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede observar o símbolo.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==14||mechanic?.kind!=='official_random_encounter'||
        decision?.stage!=='pending_external_mechanics'||decision.choiceId!=='inspect_symbol')
        throw new Error('Não há observação do símbolo penitente aguardando resolução.');
      if(event.hexId!==campaign.currentHexId)throw new Error('O grupo não está mais no hex da procissão.');
      if(decision.penitentSymbolObserverId)throw new Error('Esta observação do símbolo já foi resolvida.');
      const characters=tx.objectStore('characters');const character=await characters.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      const skill=normalizedSkill(character,['Insight','Discernimento']);
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill,gear:0},{
        characterId:character.id,sceneId:eventId,checkId:`official-penitent-symbol:${eventId}:${character.id}`,rng,
        criticalInjuries:character.criticalInjuryStates,skillName:'insight',attributeName:'wits',skillRankAtRoll:skill});
      const consequences=CriticalInjuryRulesEngine.applyCheckConsequences(character,'insight');
      const affected=roll.successes===0;const now=Date.now();
      const effectId=`official-penitent-illusion:${eventId}:${character.id}`;
      const updatedCharacter=affected?{...consequences.character,statusEffects:[...(consequences.character.statusEffects||[]).filter(effect=>effect.id!==effectId),{
        id:effectId,name:'Ilusão do símbolo de ferro',detail:'Enquanto o símbolo estiver à vista, você vê o mundo pelos olhos da Mãe Catyala. O efeito cessa quando o símbolo é destruído ou sai de vista.',sourceEventId:eventId,
      }]}:consequences.character;
      if(affected||consequences.triggers.length)await characters.put(updatedCharacter);
      await tx.objectStore('rolls').put(roll);
      const nextDecision={...decision,stage:affected?'penitent_illusion_active':'penitent_symbol_resisted',
        pending:affected?'leave_symbol_view':null,penitentSymbolObserverId:character.id,penitentSymbolRollId:roll.id,
        penitentSymbolSuccesses:roll.successes,penitentSymbolEffectId:affected?effectId:null,
        automaticRollsApplied:true,penitentSymbolResolvedAt:now};
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:affected?'open' as const:'resolved' as const,
        detail:`${item.detail} ${character.name} observou o símbolo e rolou Discernimento: ${roll.successes} sucesso(s). ${affected?'A ilusão permanece enquanto o símbolo estiver à vista.':'A personagem resistiu à ilusão.'}`,
        result:{...(item.result||{}),officialSceneDecision:nextDecision,...(!affected?{resolvedAt:now,adjudicationMode:'official_penitent_symbol_real_discernment'}:{})},
      }:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return{world:updated,roll,affected};
    });this.notify(outcome.world);return outcome;
  }

  /** Sair do campo visual encerra a ilusão exatamente para a personagem que
   * falhou; o estado de percepção não sobrevive ao símbolo fora de vista.
   */
  static async leaveOfficialPenitentSymbolView(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==14||
        decision?.stage!=='penitent_illusion_active'||!decision.penitentSymbolObserverId||!decision.penitentSymbolEffectId)
        throw new Error('Não existe ilusão penitente ativa para encerrar.');
      if(event.hexId!==campaign.currentHexId)throw new Error('O grupo precisa sair da cena pelo fluxo de jornada.');
      const character=await tx.objectStore('characters').get(String(decision.penitentSymbolObserverId)) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      const effectId=String(decision.penitentSymbolEffectId);
      if(!(character.statusEffects||[]).some(effect=>effect.id===effectId))throw new Error('O efeito da ilusão não corresponde à ficha da personagem.');
      await tx.objectStore('characters').put({...character,statusEffects:(character.statusEffects||[]).filter(effect=>effect.id!==effectId)});
      const now=Date.now();const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:'resolved' as const,detail:`${item.detail} O símbolo saiu de vista; a ilusão cessou.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'penitent_symbol_out_of_view',pending:null,
          penitentSymbolEffectId:null,illusionEndedAt:now},resolvedAt:now,adjudicationMode:'official_penitent_symbol_left_view'},
      }:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #10: o jogador pode encerrar a companhia da raposa sem matar NPC nem
   * apagar lembranças. O próximo movimento não deverá mais deslocar Grelf.
   */
  static async dismissOfficialFox(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede despedida.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='resolved');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==10||
        decision?.choiceId!=='tolerate'||decision.foxFollowerActive!==true)
        throw new Error('Esta raposa não está acompanhando o grupo.');
      const npcs=tx.objectStore('npcs');const id=`official-npc:${campaignId}:${eventId}:grelf`;
      const fox=await npcs.get(id);if(!fox||fox.campaignId!==campaignId||fox.officialFollowParty!==true||
        fox.currentLocationId!==campaign.currentHexId)
        throw new Error('A raposa precisa estar no mesmo hex para se despedir.');
      const now=Date.now();await npcs.put({...fox,officialFollowParty:false,
        currentActivity:'Vagueia por conta própria, sem acompanhar o grupo.',lastAction:'O grupo encerrou a companhia.',
        memories:[...(fox.memories||[]),{id:`official-fox-farewell:${eventId}`,sceneId:eventId,
          summary:'O grupo decidiu seguir sem a raposa.',valence:'neutral',createdAt:now}]});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} O grupo se despediu da raposa; ela não seguirá automaticamente.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,foxFollowerActive:false,foxDismissedAt:now}}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #12: a oferta retira comida REAL da ficha, registra quantidade e
   * história, mas NÃO força aceitação dos bandidos. Sem teste inventado.
   * Uma unidade de reserva cozida ou ração física conta como comida oferecida.
   */
  static async offerOfficialHungryBanditsCamp(campaignId:string,eventId:string,characterId:string,
    foodSource:'provision'|'gear',foodId:string,quantity:number,storyInput:string,expectedSnapshot?:string):Promise<WorldState>{
    const story=String(storyInput||'').trim();
    if(story.length<12||story.length>800)throw new Error('Conte uma história concreta (12 a 800 caracteres).');
    if(!Number.isInteger(quantity)||quantity<1||quantity>30)throw new Error('Escolha de 1 a 30 unidades reais de comida.');
    if(!['provision','gear'].includes(foodSource))throw new Error('Origem de comida inválida.');
    const next=await withTransaction(['campaign','world_state','characters','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de oferecer comida.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==12||
        decision?.choiceId!=='offer_camp'||decision.stage!=='pending_external_mechanics'||decision.banditFoodGift)
        throw new Error('A oferta só pode ocorrer uma vez na cena dos assaltantes famintos.');
      if(!event.hexId||event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex de Argam.');
      const chars=tx.objectStore('characters');const character=await chars.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      let updated:Character;let foodName:string;
      if(foodSource==='provision'){
        const batches=[...(character.journeyProvisions||[])];const index=batches.findIndex(b=>b.id===foodId&&b.kind==='food');
        if(index<0||!Number.isInteger(batches[index].quantity)||batches[index].quantity<quantity)throw new Error('Comida preparada insuficiente na reserva.');
        foodName='Comida preparada';const batch=batches[index];
        if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(batch,expectedSnapshot);
        if(batch.quantity===quantity)batches.splice(index,1);else batches[index]={...batch,quantity:batch.quantity-quantity};
        updated={...character,journeyProvisions:batches};
      }else{
        const gear=[...(character.gear||[])];const index=foodId.startsWith('gear:')?Number(foodId.slice(5)):gear.findIndex(item=>String(item?.id||'')===foodId);
        if(!Number.isInteger(index)||index<0||index>=gear.length)throw new Error('Ração física não encontrada no inventário.');
        const item=gear[index];if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(item,expectedSnapshot);const label=normalizeRuleName(String(item?.name||item?.label||''));
        if(!['comida','racao','racoes','provisao','provisoes','refeicao','refeicoes'].includes(label))
          throw new Error('Este item não é uma ração alimentar reconhecida.');
        const available=Number(item?.quantity??1);
        if(!Number.isInteger(available)||available<quantity||available<1)throw new Error('Quantidade de rações insuficiente.');
        foodName=String(item.name||item.label);
        if(available===quantity)gear.splice(index,1);else gear[index]={...item,quantity:available-quantity};
        updated={...character,gear};
      }
      updated={...updated,encumbrance:EncumbranceRulesEngine.calculate(updated)};
      await chars.put(updated);
      const now=Date.now();const npcs=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:argam`;
      if(await npcs.get(npcId))throw new Error('Argam já recebeu uma oferta nesta ocorrência.');
      await npcs.put({id:npcId,campaignId,name:'Argam Duasbarbas',kin:'Humano',profession:'Chefe dos assaltantes famintos',
        currentLocationId:event.hexId,faction:'Assaltantes famintos',attitude:'Desconfiado',
        goals:'Encontrar comida e recursos para seus companheiros.',fear:'Fome e falta de recursos.',secret:'',
        relationshipToParty:'Recebeu comida e ouviu a proposta de acampamento.',status:'Alive',
        playerNotes:'',gmNotes:'Aceitação depende de decisão explícita da Mestra, não de sucesso automático.',
        firstSeenSceneId:eventId,lastSeenSceneId:eventId,currentActivity:'Ouvindo a história junto às provisões oferecidas.',
        memories:[{id:`official-bandit-offer:${eventId}`,sceneId:eventId,
          summary:`Recebeu ${quantity} unidade(s) de ${foodName} de ${character.name}; ouviu uma história.`,
          valence:'neutral',createdAt:now}]});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} ${character.name} entregou ${quantity} unidade(s) de ${foodName} a Argam e contou uma história. A resposta dos assaltantes ainda depende da Mestra.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'bandit_offer_pending_reaction',
          pending:'gm_decision',banditFoodGift:{characterId,foodSource,foodId,foodName,quantity,at:now},
          banditStory:story,automaticResourcesChanged:true}}}:e);
      const updatedWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updatedWorld);return updatedWorld;
    });this.notify(next);return next;
  }

  /** A Mestra escolhe se Argam aceita dividir o acampamento; não cria repouso,
   * cura, avanço de tempo ou teste de Persuasão inexistentes no livro.
   */
  static async resolveOfficialHungryBanditsReaction(campaignId:string,eventId:string,
    reaction:'accept'|'decline'):Promise<WorldState>{
    if(!['accept','decline'].includes(reaction))throw new Error('Resposta inválida de Argam.');
    const next=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de resolver a negociação.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==12||
        decision?.choiceId!=='offer_camp'||decision.stage!=='bandit_offer_pending_reaction'||!decision.banditFoodGift)
        throw new Error('É necessário entregar comida e contar uma história antes da resposta.');
      if(event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex dos assaltantes.');
      const npcs=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:argam`;
      const npc=await npcs.get(npcId);if(!npc||npc.campaignId!==campaignId||npc.currentLocationId!==event.hexId)
        throw new Error('Argam original não está presente.');
      const now=Date.now();const accepted=reaction==='accept';
      await npcs.put({...npc,attitude:accepted?'Cordial':'Desconfiado',
        relationshipToParty:accepted?'Aceitou compartilhar o acampamento.':'Recusou compartilhar o acampamento.',
        currentActivity:accepted?'Compartilhando acampamento com o grupo.':'Seguindo seu próprio caminho.',
        memories:[...(npc.memories||[]),{id:`official-bandit-reaction:${eventId}`,sceneId:eventId,
          summary:accepted?'Aceitou a oferta de acampamento.':'Recusou a oferta de acampamento.',
          valence:accepted?'positive':'neutral',createdAt:now}]});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,status:'resolved' as const,
        detail:`${e.detail} Argam ${accepted?'aceitou dividir o acampamento':'recusou o convite'}. A comida já entregue não retorna; descansar ainda exige a ação normal de jornada.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'bandit_parley_resolved',pending:null,
          banditReaction:reaction,banditReactionAt:now},resolvedAt:now,
          adjudicationMode:'official_bandit_food_story_and_explicit_reaction'}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  static async finishOfficialSceneAfterCombat(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||(event.result?.mechanic as Record<string,unknown>|undefined)?.kind!=='official_random_encounter'||
        decision?.stage!=='in_combat'||decision.pending!=='combat'||
        !decision.combatEncounterId)throw new Error('Ocorrência sem combate oficial vinculado e pendente.');
      if(!event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('O grupo saiu do hex de origem da ocorrência.');
      const combat=world.encounter;
      if(!combat||combat.status!=='resolved'||combat.id!==decision.combatEncounterId||
        combat.startedSceneId!==eventId||combat.hexId!==event.hexId||
        !combat.resolvedAt||combat.resolvedAt<Number(decision.combatStartedAt||0))
        throw new Error('É necessário concluir o combate ORIGINAL no motor de encontros antes de liberar a jornada.');
      if(!combat.combatants.some(item=>item.side==='enemy'))
        throw new Error('O combate vinculado não contém adversários reais.');
      // U19: mesmo num save importado que já marque resolved, logs/rodadas
      // não substituem rendição, fuga, derrota ou estados mecânicos reais.
      if(combat.combatants.some(item=>item.side==='enemy' && item.status==='active') &&
         combat.combatants.some(item=>item.side==='party' && item.status==='active'))
        throw new Error('O combate foi encerrado sem ação nem desfecho mecânico; registre a resolução real da oposição.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:'resolved' as const,
        detail:`${item.detail} Combate vinculado ${combat.id} concluído após ${combat.round} rodada(s); consequências mantidas no motor de combate.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'resolved_after_combat',combatResolvedAt:combat.resolvedAt},
          adjudicationMode:'official_scene_real_combat_receipt',resolvedAt:now,
          officialCombatId:combat.id,officialCombatRounds:combat.round},
      }:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });
    this.notify(next);return next;
  }

  /** R82: Névoa de Sangue, encontro nº 1, Guia do Mestre p. 148.
   * Vigia testa Vigilância; se perceber, cada personagem pode tentar escapar
   * antes de ser envolvida. Depois, por rodada, Movimentação e Discernimento
   * com dano REAL de 1 Empatia ao falhar Discernimento. Nenhum teste é pulado.
   */
  static async startOfficialBloodMist(campaignId:string,eventId:string,rng:()=>number=Math.random):Promise<{world:WorldState;watchRoll:import('../types').DiceRoll|null;canEscapeBeforeFog:boolean}>{
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate ativo antes de iniciar a Névoa de Sangue.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||mechanic?.kind!=='official_random_encounter'||Number(mechanic.encounterId)!==1||
        decision?.stage!=='pending_external_mechanics'||!['seek_escape','endure'].includes(String(decision.choiceId)))
        throw new Error('A ocorrência de Névoa de Sangue não está pronta para iniciar.');
      if(!event.hexId||event.hexId!==campaign.currentHexId)throw new Error('O grupo saiu do hex da ocorrência.');
      const all=await tx.objectStore('characters').getAll() as Character[];
      const party=CampaignPartyService.members(campaign,all).filter(c=>c.status!=='dead');
      if(!party.length)throw new Error('Não há aventureiros vivos no grupo.');
      const lookoutId=world.journey.assignments.find(item=>item.role==='lookout')?.characterId;
      const lookout=party.find(item=>item.id===lookoutId);
      let watchRoll:import('../types').DiceRoll|null=null;
      if(lookout && CharacterActionRulesEngine.canAct(lookout)){
        const skill=normalizedSkill(lookout,['Scouting','Vigilância']);
        watchRoll=ForbiddenLandsDiceEngine.roll({base:lookout.attributes.wits.current,skill,gear:0},{
          characterId:lookout.id,sceneId:eventId,checkId:`blood-mist:watch:${eventId}`,rng,
          criticalInjuries:lookout.criticalInjuryStates,skillName:'scouting',attributeName:'wits',skillRankAtRoll:skill});
        await tx.objectStore('rolls').put(watchRoll);
      }
      const canEscapeBeforeFog=Boolean(watchRoll&&watchRoll.successes>0);
      const now=Date.now();
      const actors=party.map(item=>item.id);
      const nextDecision={...decision,stage:'blood_mist_active',pending:'item_or_skill',
        mistActorIds:actors,mistEscapedIds:[] as string[],mistActedIds:[] as string[],
        mistRound:canEscapeBeforeFog?0:1,mistWatchRollId:watchRoll?.id||null,
        mistLookoutId:lookout?.id||null,mistWatchSucceeded:canEscapeBeforeFog,
        mistStartedAt:now,mistRollIds:[] as string[],automaticRollsApplied:Boolean(watchRoll)};
      const events=world.journey.events.map(item=>item.id===eventId?{...item,
        detail:`${item.detail} A vigia ${canEscapeBeforeFog?'viu a névoa a tempo de tentar fugir.':'não conseguiu dar o alerta; a névoa cercou o grupo.'}`,
        result:{...(item.result||{}),officialSceneDecision:nextDecision}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return {world:updated,watchRoll,canEscapeBeforeFog};
    });this.notify(outcome.world);return outcome;
  }

  /** Um ator por chamada; controle de rodada e transação evitam rerrolar resultados.
   * Sair da névoa encerra somente a participação do ator, não a jornada inteira.
   */
  static async attemptOfficialBloodMistEscape(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<{world:WorldState;movement:import('../types').DiceRoll;insight:import('../types').DiceRoll|null;escaped:boolean;empathyDamage:number;round:number}>{
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede esta rodada de fuga.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==1||decision?.stage!=='blood_mist_active')
        throw new Error('Não existe uma Névoa de Sangue ativa nesta ocorrência.');
      if(event.hexId!==campaign.currentHexId)throw new Error('Grupo saiu do hex da névoa.');
      const actors=Array.isArray(decision.mistActorIds)?decision.mistActorIds.map(String):[];
      const escaped=Array.isArray(decision.mistEscapedIds)?decision.mistEscapedIds.map(String):[];
      const acted=Array.isArray(decision.mistActedIds)?decision.mistActedIds.map(String):[];
      if(!actors.includes(characterId)||escaped.includes(characterId)||acted.includes(characterId))
        throw new Error('Personagem não participa, já escapou ou já agiu nesta rodada.');
      const charStore=tx.objectStore('characters');const character=await charStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      CharacterActionRulesEngine.assertCanAct(character);
      const round=Math.max(0,Math.trunc(Number(decision.mistRound)||0));
      const movementSkill=normalizedSkill(character,['Move','Movement','Movimentação']);
      const movement=ForbiddenLandsDiceEngine.roll({base:character.attributes.agility.current,skill:movementSkill,gear:0},{
        characterId,sceneId:eventId,checkId:`blood-mist:movement:${eventId}:round:${round}:${characterId}`,rng,
        criticalInjuries:character.criticalInjuryStates,skillName:'movement',attributeName:'agility',skillRankAtRoll:movementSkill});
      let updated=CriticalInjuryRulesEngine.applyCheckConsequences(character,'movement').character;
      const didEscape=movement.successes>0;
      let insight:import('../types').DiceRoll|null=null;
      let empathyDamage=0;
      if(round>0){
        const insightSkill=normalizedSkill(updated,['Insight','Discernimento']);
        insight=ForbiddenLandsDiceEngine.roll({base:updated.attributes.wits.current,skill:insightSkill,gear:0},{
          characterId,sceneId:eventId,checkId:`blood-mist:insight:${eventId}:round:${round}:${characterId}`,rng,
          criticalInjuries:updated.criticalInjuryStates,skillName:'insight',attributeName:'wits',skillRankAtRoll:insightSkill});
        updated=CriticalInjuryRulesEngine.applyCheckConsequences(updated,'insight').character;
        if(insight.successes===0){
          const before=updated.attributes.empathy.current;
          updated=CriticalInjuryRulesEngine.applyAttributeDamage(updated,'empathy',1);
          empathyDamage=Math.min(1,before);
        }
      }
      await charStore.put(updated);
      const rollStore=tx.objectStore('rolls');await rollStore.put(movement);if(insight)await rollStore.put(insight);
      const escapedNext=didEscape?[...escaped,characterId]:escaped;
      const actedNext=[...acted,characterId];
      const everyoneEscaped=actors.every(id=>escapedNext.includes(id));
      const pending=actors.filter(id=>!escapedNext.includes(id));
      const nextRound=!everyoneEscaped&&pending.every(id=>actedNext.includes(id))?round+1:round;
      const nextActed=nextRound>round?[]:actedNext;
      const now=Date.now();
      const nextDecision={...decision,stage:everyoneEscaped?'blood_mist_resolved':'blood_mist_active',pending:everyoneEscaped?null:'item_or_skill',
        mistEscapedIds:escapedNext,mistActedIds:nextActed,mistRound:nextRound,
        mistRollIds:[...(Array.isArray(decision.mistRollIds)?decision.mistRollIds.map(String):[]),movement.id,...(insight?[insight.id]:[])],
        automaticRollsApplied:true,mistCompletedAt:everyoneEscaped?now:undefined};
      const detail=`${character.name}: Movimentação ${movement.successes} sucesso(s)${insight?`; Discernimento ${insight.successes} sucesso(s)`:''}${empathyDamage?'; -1 Empatia':''}. ${didEscape?'Escapou.':'Ainda presa na névoa.'}`;
      const events=world.journey.events.map(item=>item.id===eventId?{...item,
        status:everyoneEscaped?'resolved' as const:'open' as const,detail:`${item.detail} ${detail}`,
        result:{...(item.result||{}),officialSceneDecision:nextDecision,
          ...(everyoneEscaped?{resolvedAt:now,adjudicationMode:'blood_mist_all_escaped'}:{})}}:item);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(nextWorld);
      return {world:nextWorld,movement,insight,escaped:didEscape,empathyDamage,round};
    });this.notify(outcome.world);return outcome;
  }

  /** Arrastar alguém Quebrado em Empatia é requisito da cena, não rerrolagem.
   * O socorrista precisa ter escapado e poder agir; todos continuam no hex.
   */
  static async dragOfficialBloodMistVictim(campaignId:string,eventId:string,victimId:string,rescuerId:string):Promise<WorldState>{
    if(victimId===rescuerId)throw new Error('Ninguém pode se arrastar para fora da névoa como seu próprio socorrista.');
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==1||decision?.stage!=='blood_mist_active')
        throw new Error('Nenhuma Névoa de Sangue ativa para resgatar vítima.');
      if(world.encounter?.status==='active'||event.hexId!==campaign.currentHexId)
        throw new Error('Combate ativo ou grupo fora do hex de origem.');
      const actors=Array.isArray(decision.mistActorIds)?decision.mistActorIds.map(String):[];
      const escaped=Array.isArray(decision.mistEscapedIds)?decision.mistEscapedIds.map(String):[];
      if(!actors.includes(victimId)||!actors.includes(rescuerId)||escaped.includes(victimId)||!escaped.includes(rescuerId))
        throw new Error('O resgate exige vítima ainda presa e socorrista já fora da névoa.');
      const charStore=tx.objectStore('characters');const victim=await charStore.get(victimId) as Character|undefined;
      const rescuer=await charStore.get(rescuerId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,victim);CampaignPartyService.assertMember(campaign,rescuer);
      CharacterActionRulesEngine.assertCanAct(rescuer);
      if(victim.brokenAttribute!=='empathy'||victim.attributes.empathy.current!==0)
        throw new Error('Esta ação de resgate exige uma vítima Quebrada em Empatia.');
      const escapedNext=[...escaped,victimId];const everyoneEscaped=actors.every(id=>escapedNext.includes(id));
      const now=Date.now();const nextDecision={...decision,mistEscapedIds:escapedNext,
        mistRescues:[...(Array.isArray(decision.mistRescues)?decision.mistRescues:[]),{victimId,rescuerId,at:now}],
        stage:everyoneEscaped?'blood_mist_resolved':'blood_mist_active',mistCompletedAt:everyoneEscaped?now:undefined};
      const events=world.journey.events.map(item=>item.id===eventId?{...item,
        status:everyoneEscaped?'resolved' as const:'open' as const,
        detail:`${item.detail} ${rescuer.name} arrastou ${victim.name} para fora da Névoa de Sangue; dano de Empatia permanece.`,
        result:{...(item.result||{}),officialSceneDecision:nextDecision,
          ...(everyoneEscaped?{resolvedAt:now,adjudicationMode:'blood_mist_all_escaped'}:{})}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R83: aparição explícita do Sangrento após a Névoa de Sangue ter engolfado o grupo.
   * O Guia do Mestre NÃO indica duração fixa. A decisão da Mestra determina o instante,
   * e nenhuma rolagem, combate ou monstro é criado a cada rodada por heurística.
   */
  static async manifestOfficialBloodMistDemon(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Conclua o combate ativo antes de manifestar outra criatura.');
      if(world.journey.events.some(e=>e.status==='open'&&['combat_active','in_combat','blood_mist_combat_active'].includes(String((e.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))))
        throw new Error('Já existe outra batalha oficial pendente de conciliação.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==1||
        decision?.stage!=='blood_mist_active'||decision.mistDemonEncounterId||Number(decision.mistRound)<1)
        throw new Error('O Sangrento só pode surgir enquanto a Névoa de Sangue engolfa o grupo, uma única vez.');
      if(!event.hexId||event.hexId!==campaign.currentHexId)throw new Error('O grupo saiu do hex original da Névoa de Sangue.');
      const all=await tx.objectStore('characters').getAll() as Character[];
      const party=CampaignPartyService.members(campaign,all).filter(c=>c.status!=='dead');
      if(!party.length)throw new Error('Não há aventureiros vivos na campanha.');
      const id=BloodMistDemonRulesEngine.PROFILE_ID;
      // Fonte canônica incorporada ao aplicativo: uma cópia antiga/editada no banco
      // não pode sobrepor os atributos e os ataques publicados.
      const profile=BookOfBeastsCatalogService.entries().find(item=>item.id===id);
      if(!profile||Number(profile.mechanicalData?.attributes?.strength)!==8||Number(profile.mechanicalData?.attributes?.agility)!==4||Number(profile.armor)!==6||
        !Array.isArray(profile.mechanicalData?.monsterAttackTable)||profile.mechanicalData.monsterAttackTable.length!==6)
        throw new Error('Ficha canônica do Sangrento ausente/inválida; não reutilizar Sanguinário.');
      const enemy=EncounterService.bestiaryCombatant(profile);
      const combat=EncounterService.createEncounter('Sangrento — Névoa de Sangue',event.hexId,party,[enemy],event.id);
      const now=Date.now();
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} O Sangrento se materializou na Névoa de Sangue. Combate ${combat.id} iniciado.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'blood_mist_combat_active',
          mistDemonEncounterId:combat.id,mistDemonManifestedAt:now}}}:e);
      const updated={...world,encounter:combat,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** Combate não substitui as rolagens de fuga pendentes: jornada só avança
   * depois que a batalha real é conciliada E todos escapam da névoa.
   */
  static async finishOfficialBloodMistDemon(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==1||
        decision?.stage!=='blood_mist_combat_active'||!combat||combat.id!==decision.mistDemonEncounterId||
        combat.startedSceneId!==eventId||combat.hexId!==event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('O recibo de combate do Sangrento não corresponde a esta campanha, cena ou hex.');
      if(combat.status!=='resolved'||!combat.resolvedAt||combat.resolvedAt<Number(decision.mistDemonManifestedAt||0)||
        !combat.combatants.some(c=>c.entityId===BloodMistDemonRulesEngine.PROFILE_ID&&c.side==='enemy'))
        throw new Error('O combate original do Sangrento ainda não possui desfecho válido.');
      if(combat.combatants.some(c=>c.side==='enemy'&&c.status==='active')&&
        combat.combatants.some(c=>c.side==='party'&&c.status==='active'))
        throw new Error('Combate encerrado com ambos os lados ainda capazes de lutar: falta desfecho mecânico.');
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} Desfecho do combate Sangrento ${combat.id} conciliado; as tentativas de fuga continuam necessárias.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'blood_mist_active',
          mistDemonResolvedAt:combat.resolvedAt,mistDemonResolutionRecordedAt:now}}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** Harpias nº 5: exige um OBJETO REAL, de um herói desta campanha.
   * O livro cita a cobiça das harpias, mas não determina bônus automático:
   * a seleção não consome o item nem convence as harpias por decreto.
   */
  static async prepareOfficialHarpyDistraction(campaignId:string,eventId:string,characterId:string,
      collection:'gear'|'weapons'|'armor',index:number):Promise<WorldState>{
    if(!['gear','weapons','armor'].includes(collection)||!Number.isInteger(index)||index<0)
      throw new Error('Categoria ou índice do objeto inválido.');
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate já iniciado; o objeto deve ser preparado antes.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==5||
        decision?.stage!=='pending_external_mechanics'||decision.choiceId!=='bribe')
        throw new Error('Distração somente pode ser preparada na cena oficial das três harpias.');
      if(event.hexId!==campaign.currentHexId)throw new Error('Grupo saiu do hex das harpias.');
      const character=await tx.objectStore('characters').get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      const inventory=character[collection]||[];const item=inventory[index];
      if(!item||typeof item!=='object'||!String(item.name||item.label||'').trim())
        throw new Error('O objeto escolhido não existe no inventário desta personagem.');
      const previous=decision.harpyBaitItem as {characterId?:string;collection?:string;index?:number;fingerprint?:string}|undefined;
      if(previous?.characterId===characterId&&previous.collection===collection&&previous.index===index&&previous.fingerprint===JSON.stringify(item))
        throw new Error('Este mesmo objeto já está preparado.');
      const now=Date.now();const nextDecision={...decision,
        harpyBaitHistory:[...(Array.isArray(decision.harpyBaitHistory)?decision.harpyBaitHistory:[]),...(previous?[{...previous,replacedAt:now}]:[])],
        harpyBaitItem:{characterId,collection,index,itemName:String(item.name||item.label),
        fingerprint:JSON.stringify(item),preparedAt:now},
        harpyBaitOutcome:'pending_real_combat',automaticResourcesChanged:false};
      const events=world.journey.events.map(entry=>entry.id===eventId?{
        ...entry,detail:`${entry.detail} ${character.name} preparou ${String(item.name||item.label)} para atrair a atenção das harpias durante o combate, sem gastá-lo.`,
        result:{...(entry.result||{}),officialSceneDecision:nextDecision}}:entry);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(outcome);return outcome;
  }

  /** R84 #5: arremessar de verdade o objeto preparado, durante o turno do portador.
   * Ação rápida de manipulação, item sai do inventário e fica no chão do combate.
   * O livro NÃO concede distração garantida: a harpia ainda decide como agir.
   */
  static async throwOfficialHarpyBait(campaignId:string,eventId:string):Promise<WorldState>{
    const updated=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==5||
        decision?.choiceId!=='bribe'||decision.stage!=='combat_active'||decision.harpyBaitThrownAt||
        !combat||combat.status!=='active'||combat.id!==decision.combatEncounterId||combat.startedSceneId!==eventId||
        combat.hexId!==event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('Isca indisponível ou combate das harpias não corresponde à ocorrência.');
      const bait=decision.harpyBaitItem as {characterId?:string;collection?:string;index?:number;fingerprint?:string}|undefined;
      if(!bait||!['gear','weapons','armor'].includes(String(bait.collection))||!Number.isInteger(bait.index)||Number(bait.index)<0)
        throw new Error('Objeto da distração não foi preparado corretamente.');
      const actor=combat.combatants[combat.turnIndex];
      if(!actor||actor.kind!=='character'||actor.side!=='party'||actor.entityId!==bait.characterId||actor.status!=='active')
        throw new Error('O portador só pode arremessar o objeto no próprio turno ativo.');
      if(!actor.fastActionAvailable&&!actor.slowActionAvailable)throw new Error('O portador não tem ação para arremessar o objeto.');
      const charStore=tx.objectStore('characters');const character=await charStore.get(String(bait.characterId)) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      const category=bait.collection as 'gear'|'weapons'|'armor';
      const inventory=character[category]||[];const item=inventory[Number(bait.index)];
      if(!item||JSON.stringify(item)!==bait.fingerprint)throw new Error('Objeto alterado ou já transferido: escolha outra isca.');
      const nextInventory=inventory.filter((_,index)=>index!==Number(bait.index));
      const now=Date.now();const dropId=crypto.randomUUID();
      const combatants=combat.combatants.map(c=>c.id===actor.id?{
        ...c,...(c.fastActionAvailable?{fastActionAvailable:false}:{slowActionAvailable:false})}:c);
      const nextCombat={...combat,combatants,droppedEquipment:[...(combat.droppedEquipment||[]),{
        id:dropId,campaignId,characterId:character.id,characterName:character.name,collection:category,
        item:{...item,officialHarpyBait:true},droppedAt:now}],
        log:[...combat.log,{id:crypto.randomUUID(),round:combat.round,combatantId:actor.id,
          text:`${character.name} arremessou ${String(item.name||item.label)}; a isca caiu no campo. A cobiça das harpias não equivale a sucesso automático.`,createdAt:now}].slice(-300),updatedAt:now};
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        result:{...(e.result||{}),officialSceneDecision:{...decision,
          harpyBaitThrownAt:now,harpyBaitThrownRound:combat.round,harpyBaitDropId:dropId,
          harpyBaitOutcome:'thrown_reaction_not_guaranteed',automaticResourcesChanged:true}}}:e);
      const baitCarrier={...character,[category]:nextInventory};
      await charStore.put({...baitCarrier,encumbrance:EncumbranceRulesEngine.calculate(baitCarrier)});
      const next={...world,encounter:nextCombat,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return next;
    });this.notify(updated);return updated;
  }

  /** R85 #5: a Mestra decide a reação da harpia ativa à isca real.
   * Investigar/apanhar consome ação rápida (ou lenta se rápida já usada).
   * Apanhar exige confirmação de alcance e move o objeto do chão para a
   * ficha do PNJ; não declara fuga, acerto ou distração automática.
   */
  static async reactOfficialHarpyToBait(campaignId:string,eventId:string,harpyId:string,
    reaction:'ignore'|'investigate'|'pick_up',armsReachConfirmed:boolean):Promise<WorldState>{
    if(!['ignore','investigate','pick_up'].includes(reaction))throw new Error('Reação da harpia inválida.');
    if(reaction==='pick_up'&&!armsReachConfirmed)throw new Error('Confirme que a isca está ao alcance das mãos da harpia.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==5||
        decision?.stage!=='combat_active'||!decision.harpyBaitThrownAt||!decision.harpyBaitDropId||
        !combat||combat.status!=='active'||combat.id!==decision.combatEncounterId||
        combat.startedSceneId!==eventId||combat.hexId!==event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('A isca e o combate oficial das harpias não correspondem à ocorrência.');
      const actor=combat.combatants[combat.turnIndex];
      if(!actor||actor.id!==harpyId||actor.side!=='enemy'||actor.status!=='active'||
        !['Krag','Mag','Serag'].includes(actor.name)||actor.entityId!=='core-bestiary:harpies')
        throw new Error('Somente a harpia oficial ativa pode reagir no próprio turno.');
      const responses=Array.isArray(decision.harpyBaitReactions)?decision.harpyBaitReactions as Array<Record<string,unknown>>:[];
      if(responses.some(r=>r.harpyId===actor.id&&r.round===combat.round))
        throw new Error('Esta harpia já decidiu como reagir à isca nesta rodada.');
      const drop=combat.droppedEquipment?.find(d=>d.id===decision.harpyBaitDropId);
      if(!drop)throw new Error('Isca não está mais no chão do combate.');
      if(reaction!=='ignore'&&!actor.fastActionAvailable&&!actor.slowActionAvailable)
        throw new Error('A harpia não possui ação disponível para investigar a isca.');
      const now=Date.now();
      const changed={...actor,
        ...(reaction==='ignore'?{}:actor.fastActionAvailable?{fastActionAvailable:false}:{slowActionAvailable:false}),
        quickSheet:{...(actor.quickSheet||{}),
          ...(reaction==='pick_up'?{officialHarpyHeldBait:{...drop.item,sourceDropId:drop.id}}:
            reaction==='investigate'?{officialHarpyBaitFocusId:drop.id}:{})}};
      const combatants=combat.combatants.map(c=>c.id===actor.id?changed:c);
      const droppedEquipment=reaction==='pick_up'?(combat.droppedEquipment||[]).filter(d=>d.id!==drop.id):combat.droppedEquipment;
      const text=reaction==='ignore'?`${actor.name} ignorou a isca por enquanto.`:
        reaction==='investigate'?`${actor.name} gastou uma ação para investigar a isca, sem recuperá-la.`:
        `${actor.name} recolheu ${String(drop.item.name||'a isca')} do chão, ao alcance confirmado.`;
      const log=[...combat.log,{id:crypto.randomUUID(),round:combat.round,combatantId:actor.id,text,createdAt:now}].slice(-300);
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        result:{...(e.result||{}),officialSceneDecision:{...decision,
          harpyBaitReactions:[...responses,{harpyId:actor.id,round:combat.round,reaction,at:now}],
          harpyBaitOutcome:reaction==='pick_up'?'recovered_by_harpy':reaction==='investigate'?'harpy_investigating':'harpy_ignored',
          ...(reaction==='pick_up'?{harpyBaitRecoveredBy:actor.id,harpyBaitRecoveredAt:now}:{})}}}:e);
      const updated={...world,encounter:{...combat,combatants,droppedEquipment,log,updatedAt:now},
        journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #18: confirma que passaram algumas horas na casa vazia e Melkar voltou
   * com os halflings acorrentados. A fonte não fixa uma duração exata, portanto
   * a escolha é registrada sem avançar um número arbitrário de horas no relógio.
   */
  static async resolveOfficialDoughBakerReturn(campaignId:string,eventId:string):Promise<WorldState>{
    const updated=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==18||
        Number(event.result?.officialEncounterVisitNumber||1)<3||decision?.stage!=='pending_external_mechanics'||
        decision.choiceId!=='wait_for_melkar'||decision.pending!=='gm_decision'||event.hexId!==campaign.currentHexId)
        throw new Error('Melkar só retorna após a escolha de esperar na casa vazia na terceira visita ou depois.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,
        detail:`${item.detail} Após algumas horas, Melkar retornou com halflings acorrentados e as 200 massas de pão.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'dough_captives_returned',pending:'combat',
          doughMasses:200,doughReturnConfirmedAt:now}}}:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return next;
    });
    this.notify(updated);return updated;
  }

  /** R88 #18: registra a tentativa de assar massa e a arbitragem explícita
   * da Mestra. A p.155 não especifica teste, alvo ou quantidade de corrupção.
   */
  static async adjudicateOfficialDoughCorruption(campaignId:string,eventId:string,notesInput:string):Promise<WorldState>{
    const notes=String(notesInput||'').trim();
    if(notes.length<12||notes.length>800)throw new Error('Registre a adjudicação em 12–800 caracteres.');
    const updated=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==18||
        decision?.stage!=='pending_external_mechanics'||decision.choiceId!=='bake_dough'||decision.pending!=='gm_decision'||
        event.hexId!==campaign.currentHexId||world.encounter?.status==='active')
        throw new Error('Não há tentativa de assar massa aguardando adjudicação neste hex.');
      const now=Date.now();const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,
        detail:`${item.detail} Adjudicação da Mestra: ${notes}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'dough_corruption_adjudicated',pending:null,
          doughCorruptionAdjudication:notes,doughCorruptionAdjudicatedAt:now,corruptionEffectsAutomaticallyApplied:false},
          resolvedAt:now,adjudicationMode:'official_dough_corruption_gm_adjudication'}}:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return next;
    });this.notify(updated);return updated;
  }

  /** R88 #26 p.158: cada aventureira testa Sobrevivência; cada falha sofre ataque de fogo crescente. */
  static async resolveOfficialWildfireEscapeAttempt(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<{survivalRoll:import('../types').DiceRoll;fireRoll:import('../types').DiceRoll|null;fireDamage:number;fireBaseDice:number;status:'escaped'|'pending'|'broken';completed:boolean}>{
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o combate ativo antes de continuar a fuga do incêndio.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==26||decision?.choiceId!=='escape_fire'||!event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('O incêndio florestal não aguarda uma tentativa de fuga neste hex.');
      const characterStore=tx.objectStore('characters');const character=await characterStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      const progress={...((decision.fireEscapeProgress as Record<string,Record<string,unknown>>|undefined)||{})};
      const partyIds=Array.from(new Set((campaign.partyCharacterIds||[]).map(String)));
      for(const id of partyIds){
        if(progress[id])continue;
        const member=await characterStore.get(id) as Character|undefined;
        if(!member)throw new Error('Uma ficha do grupo está ausente; não é possível concluir a fuga do incêndio.');
        progress[id]={attempts:0,status:member.status==='dead'?'dead':member.attributes.strength.current<=0?'broken':'pending'};
      }
      const previous=progress[characterId];
      if(previous&&['escaped','broken','dead'].includes(String(previous.status)))throw new Error('Esta personagem já escapou ou não pode mais agir no incêndio.');
      CharacterActionRulesEngine.assertCanAct(character);
      const attempt=Math.max(1,Math.trunc(Number(previous?.attempts)||0)+1);
      const skill=normalizedSkill(character,['Survival','Sobrevivência']);
      const survivalRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill,gear:0},{
        characterId,sceneId:eventId,checkId:`wildfire-survival:${eventId}:${characterId}:${attempt}`,rng,
        criticalInjuries:character.criticalInjuryStates,skillName:'survival',attributeName:'wits',skillRankAtRoll:skill});
      let updated=CriticalInjuryRulesEngine.applyCheckConsequences(character,'survival').character;
      let fireRoll:import('../types').DiceRoll|null=null;let fireDamage=0;let fireBaseDice=0;
      let fireCriticalPending=false;
      let status:'escaped'|'pending'|'broken'=survivalRoll.successes>0?'escaped':'pending';
      if(survivalRoll.successes===0){
        fireBaseDice=5+attempt;
        fireRoll=ForbiddenLandsDiceEngine.roll({base:fireBaseDice,skill:0,gear:0},{characterId:null,sceneId:eventId,
          checkId:`wildfire-fire:${eventId}:${characterId}:${attempt}`,rng});
        const armor=Array.isArray(updated.armor)?Math.max(0,...updated.armor.map((item:any)=>Number(item?.armor||item?.rating||item?.protection||0))):0;
        const previousStrength=updated.attributes.strength.current;
        const damage=CombatRulesEngine.resolveAttack({attackSuccesses:fireRoll.successes,weaponDamage:1,armorValue:armor,rng});
        fireDamage=damage.finalDamage;
        if(fireDamage>0){
          updated=CriticalInjuryRulesEngine.applyAttributeDamage(updated,'strength',fireDamage);
          if(CombatRulesEngine.criticalRequired({previousStrength,finalDamage:fireDamage})){
            const criticalId=`critical-pending:wildfire:${eventId}:${characterId}:${attempt}`;
            const marker='PENDENTE: ferimento grave — ataque do incêndio florestal';
            fireCriticalPending=true;
            updated={...updated,criticalInjuries:Array.from(new Set([...(updated.criticalInjuries||[]),marker])),
              statusEffects:[...(updated.statusEffects||[]),{id:criticalId,label:'Ferimento grave pendente',
                detail:'O ataque de fogo Quebrou Força; concluir o ferimento grave pela tabela Outros.',source:'R88 encontro #26'}]};
          }
        }
        status=updated.attributes.strength.current<=0?'broken':'pending';
      }
      await characterStore.put(updated);
      const rollStore=tx.objectStore('rolls');await rollStore.put(survivalRoll);if(fireRoll)await rollStore.put(fireRoll);
      const nextProgress={...progress,[characterId]:{attempts:attempt,status,survivalRollId:survivalRoll.id,
        survivalSuccesses:survivalRoll.successes,...(fireRoll?{fireRollId:fireRoll.id,fireBaseDice,fireDamage}:{}),
        ...(fireCriticalPending?{criticalInjuryPending:true,criticalTable:'other'}:{}),updatedAt:Date.now()}};
      const completed=partyIds.length>0&&partyIds.every(id=>['escaped','broken','dead'].includes(String(nextProgress[id]?.status)));
      const now=Date.now();const nextDecision={...decision,stage:completed?'wildfire_escape_resolved':'wildfire_escape_pending',
        pending:completed?null:'item_or_skill',fireEscapeProgress:nextProgress,...(completed?{resolvedAt:now,adjudicationMode:'official_wildfire_escape'}:{})};
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:completed?'resolved' as const:'open' as const,
        detail:`${item.detail} ${character.name}: Sobrevivência ${survivalRoll.successes} sucesso(s)${fireRoll?`; ataque de fogo ${fireBaseDice} Dados Base, Dano de Arma 1, dano final ${fireDamage}`:''}${status==='escaped'?'; escapou':status==='broken'?'; ficou Acabada':' ainda precisa escapar'}.`,
        result:{...(item.result||{}),officialSceneDecision:nextDecision}}:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await worldStore.put(next);
      return{world:next,survivalRoll,fireRoll,fireDamage,fireBaseDice,status,completed};
    });
    this.notify(outcome.world);return{survivalRoll:outcome.survivalRoll,fireRoll:outcome.fireRoll,fireDamage:outcome.fireDamage,
      fireBaseDice:outcome.fireBaseDice,status:outcome.status,completed:outcome.completed};
  }

  /** R88 #21–#25: registra decisões que a fonte deixa para a MdJ, sem rolagens inventadas. */
  static async adjudicateOfficialEncounterFollowUp(campaignId:string,eventId:string,outcome:string,evidenceInput:string,rng:()=>number=Math.random):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre a decisão da MdJ com pelo menos 12 caracteres.');
    const updated=await withTransaction(['campaign','world_state','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const id=Number(mechanic?.encounterId);
      if(!event||mechanic?.kind!=='official_random_encounter'||![14,17,18,21,22,23,25,27,28,32,34,35].includes(id)||!decision||
        !event.hexId||event.hexId!==campaign.currentHexId||world.encounter?.status==='active')
        throw new Error('A cena oficial não está pronta para adjudicação neste hex.');
      // Escopo de adjudicação: uma mesma decisão não pode ser repetida para
      // rolar novamente D6, mudar o resultado da MdJ, duplicar NPCs ou reabrir
      // fases já consumidas. Validar ANTES de invocar RNG e escrever no banco.
      // A matriz reflete só transições previstas nos fluxos oficiais abaixo;
      // não substitui nenhum teste/perícia ou decisão de combate.
      const phase=String(decision.stage||'');
      const action=String(decision.choiceId||'');
      const outcomesByPhase:Record<number,Record<string,Record<string,readonly string[]>>>={
        14:{pending_external_mechanics:{consider_joining:['invitation_declined_after_discussion']}},
        17:{pending_external_mechanics:{attend:['celebration_completed_without_fight'],prevent_fight:['wedding_fight_prevented']}},
        18:{pending_external_mechanics:{tea:['tea_visit_completed'],inspect:['dough_origin_explained']}},
        21:{pending_external_mechanics:{observe_dagger:['dagger_spotted','dagger_not_spotted'],
            speak:['speak','conversation_succeeded','conversation_failed'],
            withdraw:['withdrawal_succeeded','withdrawal_failed']},
          ghost_conversation_pending:{speak:['speak','conversation_succeeded','conversation_failed'],observe_dagger:['speak','conversation_succeeded','conversation_failed']},
          ghost_withdrawal_pending:{withdraw:['withdrawal_succeeded','withdrawal_failed'],observe_dagger:['withdrawal_succeeded','withdrawal_failed']}},
        22:{pending_external_mechanics:{hear_request:['accept','decline'],collect_mist:['collection_succeeded','collection_not_performed']},
          glass_collection_pending:{hear_request:['collection_succeeded','collection_not_performed']}},
        23:{pending_external_mechanics:{talk_troll:['troll_agrees','troll_refuses']}},
        25:{pending_external_mechanics:{accept_candle_quest:['gonlab_found','search_unresolved']},
          candle_search_pending:{accept_candle_quest:['gonlab_found','search_unresolved']}},
        27:{pending_external_mechanics:{comfort:['linn_fed_and_kept','linn_not_fed','linn_abandoned'],
            investigate:['linn_clearing_investigated']},
          linn_care_pending:{comfort:['linn_fed_and_kept','linn_not_fed','linn_abandoned']},
          linn_waiting:{comfort:['linn_d6_days_elapsed']},
          linn_revelation_pending:{comfort:['linn_follows_party','linn_left']}},
        28:{pending_external_mechanics:{search:['tracks_found','tracks_lost'],track:['captors_tracked','trail_lost']},
          cabin_tracks_found:{search:['captors_tracked','trail_lost']}},
        32:{rat_baby_unrescued_pending:{leave_baby:['baby_cry_quiet']},
          belinda_response_pending:{visit_belinda:['belinda_not_pressed','belinda_pressed']}},
        34:{pending_external_mechanics:{question_captain:['holmar_released_troupe','villagers_protected','party_withdrew'],
          help_escort:['holmar_released_troupe','villagers_protected','party_withdrew']}},
        35:{pending_external_mechanics:{offer_gift:['troll_offer_accepted','troll_offer_refused'],
          offer_goblin:['troll_offer_accepted','troll_offer_refused'],
          offer_adventurer:['troll_offer_accepted','troll_offer_refused'],
          offer_horse:['troll_offer_accepted','troll_offer_refused']}},
      };
      // No encontro #21 o seguimento recebe followUpChoiceId, sem reescrever
      // a observação da adaga que iniciou o evento.
      const permitted=outcomesByPhase[id]?.[phase]?.[action]||
        (id===21&&['ghost_conversation_pending','ghost_withdrawal_pending'].includes(phase)
          ?outcomesByPhase[id]?.[phase]?.[String(decision.followUpChoiceId||'')]:undefined);
      if(!permitted?.includes(outcome))
        throw new Error('A adjudicação já foi consumida ou não pertence à etapa atual deste encontro.');
      let nextDecision:Record<string,unknown>={...decision,adjudicationEvidence:evidence,adjudicatedAt:Date.now()};
      let closes=false;
      if([14,17,18].includes(id)){
        // Only the source-defined non-mechanical social outcomes whitelisted
        // above may conclude here. No gear abandonment, injury, corruption,
        // rolls or combat are inferred from the GM's note.
        closes=true;
        nextDecision={...nextDecision,stage:`${outcome}_adjudicated`,pending:null,
          socialOutcome:outcome,socialOutcomeEvidence:evidence,
          socialSourcePage:id===14?153:id===17?154:155,
          automaticResourcesChanged:false,automaticRollsApplied:false,
          ...(id===18&&decision.choiceId==='inspect'?{doughOriginExplained:true}:{}),
          ...(id===17?{weddingFightOccurred:false}:{}),
          ...(id===14?{invitationAccepted:false,equipmentAbandoned:false}:{})};
      }else if(id===21&&decision.choiceId==='observe_dagger'&&['dagger_spotted','dagger_not_spotted'].includes(outcome)){
        nextDecision={...nextDecision,stage:'ghost_follow_up_pending',pending:'follow_up',daggerSpotted:outcome==='dagger_spotted'};
      }else if(id===21&&(['speak','withdraw'].includes(String(decision.choiceId))||['speak','withdraw'].includes(String(decision.followUpChoiceId)))&&
        ((['speak','conversation_succeeded','conversation_failed'].includes(outcome)&&((decision.choiceId==='speak'||decision.followUpChoiceId==='speak')))||
         (['withdrawal_succeeded','withdrawal_failed'].includes(outcome)&&((decision.choiceId==='withdraw'||decision.followUpChoiceId==='withdraw'))))){
        const success=outcome==='conversation_succeeded'||outcome==='withdrawal_succeeded';
        closes=success;
        nextDecision={...nextDecision,stage:success?'resolved':'ghost_follow_up_pending',pending:success?null:'follow_up',
          ghostInteractionOutcome:outcome};
      }else if(id===22&&decision.choiceId==='hear_request'&&['accept','decline'].includes(outcome)){
        closes=outcome==='decline';
        nextDecision={...nextDecision,stage:closes?'glassmaker_request_declined':'glass_collection_pending',pending:closes?null:'gm_decision',
          varamakRequestAccepted:!closes};
      }else if(id===22&&(decision.choiceId==='collect_mist'||decision.stage==='glass_collection_pending')&&
        ['collection_succeeded','collection_not_performed'].includes(outcome)){
        if(outcome==='collection_not_performed'){
          closes=true;nextDecision={...nextDecision,stage:'glass_collection_not_performed',pending:null,mistCollected:false};
        }else{
          const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte D6 inválida para o prazo oficial de quebra do vidro.');
          const shatterHours=1+Math.floor(value*6);
          nextDecision={...nextDecision,stage:'glass_mist_progression',pending:'wait_for_d6_hours',mistCollected:true,
            glassShatterHoursD6:shatterHours,glassBreakElapsed:false,wholeHexCovered:false};
        }
      }else if(id===23&&decision.choiceId==='talk_troll'&&['troll_agrees','troll_refuses'].includes(outcome)){
        const refuses=outcome==='troll_refuses';
        nextDecision={...nextDecision,stage:refuses?'pending_external_mechanics':'troll_negotiation_resolved',
          pending:refuses?'combat':null,trollNegotiation:outcome,...(refuses?{choiceId:'protect_puppy'}:{})};
        closes=!refuses;
      }else if(id===25&&decision.choiceId==='accept_candle_quest'&&['gonlab_found','search_unresolved'].includes(outcome)){
        const found=outcome==='gonlab_found';
        nextDecision={...nextDecision,stage:found?'pending_external_mechanics':'candle_search_pending',pending:found?'combat':'follow_up',
          candleSearchOutcome:outcome,...(found?{choiceId:'confront_gonlab'}:{})};
      }else if(id===27&&decision.choiceId==='comfort'&&['linn_fed_and_kept','linn_not_fed','linn_abandoned'].includes(outcome)){
        if(outcome==='linn_abandoned'){
          closes=true;nextDecision={...nextDecision,stage:'linn_left_in_clearing',pending:null,linnAbandoned:true};
        }else if(outcome==='linn_not_fed'){
          nextDecision={...nextDecision,stage:'linn_care_pending',pending:'follow_up',linnFed:false};
        }else{
          const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte D6 inválida para o intervalo de Linn.');
          const days=1+Math.floor(value*6);const startDay=Math.max(1,Math.trunc(Number(campaign.currentDay)||1));
          nextDecision={...nextDecision,stage:'linn_waiting',pending:'wait_for_d6_days',linnFed:true,
            linnWaitDaysD6:days,linnWaitStartedDay:startDay,linnRevealDay:startDay+days,linnCareEvidence:evidence};
        }
      }else if(id===27&&decision.choiceId==='investigate'&&outcome==='linn_clearing_investigated'){
        closes=true;nextDecision={...nextDecision,stage:'linn_clearing_investigated',pending:null,investigationEvidence:evidence};
      }else if(id===27&&decision.stage==='linn_care_pending'&&outcome==='linn_fed_and_kept'){
        const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte D6 inválida para o intervalo de Linn.');
        const days=1+Math.floor(value*6);const startDay=Math.max(1,Math.trunc(Number(campaign.currentDay)||1));
        nextDecision={...nextDecision,stage:'linn_waiting',pending:'wait_for_d6_days',linnFed:true,
          linnWaitDaysD6:days,linnWaitStartedDay:startDay,linnRevealDay:startDay+days,linnCareEvidence:evidence};
      }else if(id===27&&decision.stage==='linn_waiting'&&outcome==='linn_d6_days_elapsed'){
        const revealDay=Math.max(1,Math.trunc(Number(decision.linnRevealDay)||0));
        if(Math.trunc(Number(campaign.currentDay)||1)<revealDay)throw new Error('O D6 de dias ainda não transcorreu no calendário da campanha.');
        nextDecision={...nextDecision,stage:'linn_revelation_pending',pending:'follow_up',linnRevealedAtDay:Math.trunc(Number(campaign.currentDay)||1)};
      }else if(id===27&&decision.stage==='linn_revelation_pending'&&['linn_follows_party','linn_left'].includes(outcome)){
        if(outcome==='linn_follows_party'){
          const npcStore=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:linn`;
          const existing=await npcStore.get(npcId);
          if(!existing)await npcStore.put({id:npcId,campaignId,name:'Linn',kin:'Não especificada na R88',profession:'Sobrevivente de Mataselvagem',
            currentLocationId:event.hexId,faction:'Sobrevivente de Mataselvagem',attitude:'Acompanha o grupo após acolhimento',
            goals:'Aprender a lançar magias e matar feiticeiros das Terras Proibidas',fear:'Seus poderes mágicos descontrolados',
            secret:'A destruição de Mataselvagem foi causada pelo infortúnio de duas feiticeiras rivais; Linn é a única sobrevivente.',
            relationshipToParty:'Segue o grupo se as aventureiras escolherem mantê-la após a revelação',status:'Alive',
            playerNotes:'Linn é uma jovem sobrevivente da clareira de areia negra.',
            gmNotes:'Guia do Mestre R88, encontro #27 p.159. A fonte não fornece atributos, perícias ou ficha de combate.',
            officialFollowParty:true,officialFollowEventId:eventId,currentActivity:'Viaja com o grupo após a escolha da MdJ.'});
          closes=true;nextDecision={...nextDecision,stage:'linn_following_party',pending:null,linnContinuesWithParty:true,linnNpcId:npcId,linnStatisticsInvented:false};
        }else{
          closes=true;nextDecision={...nextDecision,stage:'linn_left_after_revelation',pending:null,linnContinuesWithParty:false};
        }
      }else if(id===28&&decision.choiceId==='search'&&['tracks_found','tracks_lost'].includes(outcome)){
        closes=outcome==='tracks_lost';nextDecision={...nextDecision,stage:closes?'cabin_tracks_lost':'cabin_tracks_found',
          pending:closes?null:'follow_up',cabinTracksFound:!closes};
      }else if(id===28&&(['track'].includes(String(decision.choiceId))||decision.stage==='cabin_tracks_found')&&
        ['captors_tracked','trail_lost'].includes(outcome)){
        closes=true;nextDecision={...nextDecision,stage:outcome==='captors_tracked'?'silver_mine_captors_found':'cabin_trail_lost',
          pending:null,captorsTracked:outcome==='captors_tracked',localLordNeedsSilverMineSlaves:outcome==='captors_tracked',
          banditStatisticsInvented:false};
      }else if(id===32&&decision.choiceId==='leave_baby'&&decision.stage==='rat_baby_unrescued_pending'&&outcome==='baby_cry_quiet'){
        closes=true;nextDecision={...nextDecision,stage:'rat_baby_left_unrescued',pending:null,babyCryEnded:true,
          elapsedMinutesNotInvented:true,babyFateAdjudicatedByGm:true};
      }else if(id===32&&decision.choiceId==='visit_belinda'&&decision.stage==='belinda_response_pending'&&
        ['belinda_not_pressed','belinda_pressed'].includes(outcome)){
        closes=true;nextDecision={...nextDecision,stage:outcome==='belinda_pressed'?'belinda_confessed':'belinda_left_unpressed',
          pending:null,belindaConfessed:outcome==='belinda_pressed',babyOriginRevealed:outcome==='belinda_pressed'};
      }else if(id===34&&['question_captain','help_escort'].includes(String(decision.choiceId))&&
        ['holmar_released_troupe','villagers_protected','party_withdrew'].includes(outcome)){
        closes=true;nextDecision={...nextDecision,stage:`iron_clown_${outcome}`,pending:null,
          ironGuardCountUnspecified:true,undeadStatisticsInvented:false,gmOutcome:outcome};
      }else if(id===35&&['offer_gift','offer_goblin','offer_adventurer','offer_horse'].includes(String(decision.choiceId))&&
        ['troll_offer_accepted','troll_offer_refused'].includes(outcome)){
        const accepted=outcome==='troll_offer_accepted';
        const fatePending=accepted&&['offer_adventurer','offer_horse'].includes(String(decision.choiceId));
        closes=accepted&&!fatePending&&decision.choiceId!=='offer_gift';
        // U10: uma oferta recusada NÃO autoriza transferência nem sacrifício.
        // A ocorrência continua aberta apenas para a escolha de outra abordagem.
        nextDecision={...nextDecision,stage:accepted?(fatePending?'karg_sacrifice_pending':'karg_offer_accepted'):'karg_offer_refused',pending:accepted?(fatePending?'follow_up':null):'follow_up',
          kargOfferType:decision.choiceId,kargOfferAccepted:accepted,
          kargGiftTransferRequired:accepted && decision.choiceId==='offer_gift',
          ...(decision.choiceId==='offer_adventurer'?{adventurerFateRequiresExplicitResolution:fatePending}:{}),
          ...(decision.choiceId==='offer_horse'?{horseFateRequiresExplicitResolution:fatePending}:{})};
      }else throw new Error('Resultado inválido para a decisão atual desta cena R88.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:closes?'resolved' as const:'open' as const,
        detail:`${item.detail} Adjudicação da MdJ: ${evidence}`,
        result:{...(item.result||{}),officialSceneDecision:{...nextDecision,...(closes?{resolvedAt:now,adjudicationMode:'official_scene_gm_adjudication'}:{})}}}:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(updated);return updated;
  }

  /** R88 #35: a shiny gift leaves the donor's inventory only after the GM
   * records that Karg accepted it. The transferred object is saved with Karg. */
  static async offerOfficialKargGift(campaignId:string,eventId:string,characterId:string,
    collection:'gear'|'weapons'|'armor',itemIndex:number,evidenceInput:string,expectedSnapshot?:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre por que a MdJ aceitou o presente (12–600 caracteres).');
    const next=await withTransaction(['campaign','world_state','characters','npcs'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==35||decision?.choiceId!=='offer_gift'||decision.stage!=='karg_offer_accepted'||
        decision.kargGiftTransferRequired!==true||world.encounter?.status==='active'||event.hexId!==campaign.currentHexId)
        throw new Error('Karg só recebe o presente após a MdJ registrar que ele o aceitou nesta cena.');
      if(!['gear','weapons','armor'].includes(collection)||!Number.isInteger(itemIndex)||itemIndex<0)
        throw new Error('Selecione um objeto válido do inventário.');
      const characterStore=tx.objectStore('characters');const character=await characterStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      if(character!.status==='dead')throw new Error('Uma personagem morta não pode entregar um objeto.');
      const items=[...(character![collection]||[])];const original=items[itemIndex] as Record<string,unknown>|undefined;
      if(!original)throw new Error('O objeto selecionado não está mais no inventário.');
      if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(original,expectedSnapshot);
      const quantity=Number(original.quantity??1);
      if(!Number.isSafeInteger(quantity)||quantity<1)throw new Error('Quantidade do presente inválida.');
      const gift:Record<string,unknown>={...original,...(quantity>1?{quantity:1,id:`official-karg-gift:${eventId}:${crypto.randomUUID()}`,sourceStackId:original.id||null}:{})};
      if(quantity>1)items[itemIndex]={...original,quantity:quantity-1};else items.splice(itemIndex,1);
      const giver={...character!,[collection]:items};
      // U10: carga atualizada mesmo em chamadas legadas sem token da UI.
      await characterStore.put({...giver,encumbrance:EncumbranceRulesEngine.calculate(giver)});
      const npcStore=tx.objectStore('npcs');const npcId=`official-npc:${campaignId}:${eventId}:karg`;
      const karg=await npcStore.get(npcId);
      if(!karg)throw new Error('O registro de Karg não foi encontrado para receber o presente.');
      await npcStore.put({...karg,gifts:[...(Array.isArray(karg.gifts)?karg.gifts:[]),{...gift,collection,sourceEventId:eventId,acceptedAt:Date.now()}],
        currentActivity:'Recebeu um presente aceito pelo grupo.'});
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,
        detail:`${item.detail} Karg aceitou ${String(gift.name||gift.label||'o presente')}; o objeto saiu do inventário e foi registrado com ele.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'karg_gift_transferred',pending:null,
          kargGiftTransferRequired:false,kargGift:{characterId,item:gift,collection,transferredAt:now},kargGiftEvidence:evidence,
          resolvedAt:now,adjudicationMode:'official_karg_gift_inventory_transfer'}}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #35: finalize an explicitly accepted sacrifice; a mount leaves the
   * party roster, while a named adventurer is marked dead in the character. */
  static async resolveOfficialKargSacrifice(campaignId:string,eventId:string,
    kind:'adventurer'|'horse',targetId:string,evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre quem foi devorado e a adjudicação da MdJ (12–600 caracteres).');
    const stores=kind==='adventurer'?['campaign','world_state','characters']:['campaign','world_state'];
    const next=await withTransaction(stores,'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const expected=kind==='adventurer'?'offer_adventurer':'offer_horse';
      if(!event||Number(mechanic?.encounterId)!==35||decision?.choiceId!==expected||decision.stage!=='karg_sacrifice_pending'||
        decision.kargOfferAccepted!==true||world.encounter?.status==='active'||event.hexId!==campaign.currentHexId)
        throw new Error('Karg só pode receber o sacrifício que a MdJ aceitou nesta cena.');
      let targetName='';let nextWorld=world;
      if(kind==='adventurer'){
        const character=await tx.objectStore('characters').get(targetId) as Character|undefined;
        CampaignPartyService.assertMember(campaign,character);
        if(character!.status==='dead')throw new Error('A personagem escolhida já está morta.');
        targetName=character!.name;await tx.objectStore('characters').put({...character!,status:'dead'});
      }else{
        const mounts=world.journey.mounts||[];const mount=mounts.find(item=>item.id===targetId);
        if(!mount)throw new Error('A montaria escolhida não está registrada na jornada.');
        targetName=mount.name;
        nextWorld={...world,journey:{...world.journey,mounts:mounts.filter(item=>item.id!==targetId)}};
      }
      const now=Date.now();
      const events=nextWorld.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,
        detail:`${item.detail} Karg devorou ${targetName}; o sacrifício aceito pela MdJ foi registrado.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'karg_sacrifice_adjudicated',pending:null,
          kargSacrifice:{kind,targetId,targetName,adjudicatedAt:now,evidence},resolvedAt:now,
          adjudicationMode:'official_karg_sacrifice'}}}:item);
      const updated={...nextWorld,journey:{...nextWorld.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #21: a adaga descoberta permite mencionar morte/adaga, atacar ou tentar sair. */
  static async advanceOfficialVengefulGhostScene(campaignId:string,eventId:string,choice:'speak'|'attack'|'withdraw'):Promise<WorldState>{
    const updated=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==21||decision?.stage!=='ghost_follow_up_pending'||
        !event.hexId||event.hexId!==campaign.currentHexId||world.encounter?.status==='active')
        throw new Error('A cena de Garmagol não aguarda uma decisão de acompanhamento.');
      if(!['speak','attack','withdraw'].includes(choice))throw new Error('Escolha inválida para Garmagol.');
      const nextDecision:Record<string,unknown>={...decision,followUpChoiceId:choice,
        stage:choice==='attack'?'pending_external_mechanics':choice==='speak'?'ghost_conversation_pending':'ghost_withdrawal_pending',
        pending:choice==='attack'?'combat':'gm_decision',...(choice==='attack'?{choiceId:'attack'}:{})};
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,
        detail:`${item.detail} Acompanhamento do grupo: ${choice}.`,
        result:{...(item.result||{}),officialSceneDecision:nextDecision}}:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(updated);return updated;
  }

  /** R88 #22: confirma a passagem dos intervalos impressos sem criar progressão intermediária. */
  static async advanceOfficialGlassmakerTimeline(campaignId:string,eventId:string,action:'confirm_d6_hours_elapsed'|'confirm_d6_weeks_elapsed',evidenceInput:string,rng:()=>number=Math.random):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre como a mesa confirmou o intervalo de tempo (mínimo 12 caracteres).');
    const updated=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==22||!event.hexId||event.hexId!==campaign.currentHexId)
        throw new Error('A Vidraceira não está ativa no hex atual.');
      let nextDecision:Record<string,unknown>;let closes=false;
      if(action==='confirm_d6_hours_elapsed'&&decision?.stage==='glass_mist_progression'&&decision.mistCollected===true){
        const value=rng();if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte D6 inválida para a dispersão oficial.');
        nextDecision={...decision,stage:'blood_mist_hex_covered',pending:'wait_for_d6_weeks',glassBreakElapsed:true,
          wholeHexCovered:true,bloodMistDispersalWeeksD6:1+Math.floor(value*6),glassBreakEvidence:evidence,glassBrokenAt:Date.now()};
      }else if(action==='confirm_d6_weeks_elapsed'&&decision?.stage==='blood_mist_hex_covered'&&decision.wholeHexCovered===true){
        nextDecision={...decision,stage:'blood_mist_dispersed',pending:null,wholeHexCovered:false,
          dispersalEvidence:evidence,bloodMistDispersedAt:Date.now()};closes=true;
      }else throw new Error('O intervalo indicado não está pendente para esta etapa da estrutura.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:closes?'resolved' as const:'open' as const,
        detail:`${item.detail} ${evidence}`,
        result:{...(item.result||{}),officialSceneDecision:{...nextDecision,...(closes?{resolvedAt:now,adjudicationMode:'official_glassmaker_timeline'}:{})}}}:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(updated);return updated;
  }

  /** R88 #24: a fonte não define ficha de Oker nem seu destino após a captura. */
  static async resolveOfficialCursedOgrePrisoner(campaignId:string,eventId:string,choice:'free_oker'|'leave_oker'):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const postCombat=decision?.stage==='oker_fate_pending'&&world.encounter?.status==='resolved'&&world.encounter.startedSceneId===eventId;
      const directRescue=decision?.stage==='pending_external_mechanics'&&decision.choiceId==='free_oker'&&world.encounter?.status!=='active';
      if(!event||Number(mechanic?.encounterId)!==24||(!postCombat&&!directRescue))
        throw new Error('Oker só pode receber um destino explícito após a tentativa de libertação ou o combate vinculado.');
      if(!['free_oker','leave_oker'].includes(choice))throw new Error('Escolha inválida para o destino de Oker.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,
        detail:`${item.detail} ${choice==='free_oker'?'O grupo libertou Oker.':'O grupo deixou Oker no saco.'} A ficha e as consequências adicionais não são definidas pela entrada R88.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:choice==='free_oker'?'oker_freed':'oker_left_with_kurge',pending:null,okerFate:choice,okerStatisticsInvented:false,resolvedAt:now,adjudicationMode:'official_prisoner_fate_choice'}}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #25: concede as dez velas apenas após confirmação registrada de que o filho voltou. */
  static async completeOfficialCandleQuest(campaignId:string,eventId:string,recipientId:string,returnEvidenceInput:string):Promise<WorldState>{
    const evidence=String(returnEvidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre como o filho foi encontrado e retornou com pelo menos 12 caracteres.');
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==25||decision?.stage!=='candle_child_rescue_pending'||
        decision.candleRewardPending!==true||world.encounter?.status!=='resolved'||world.encounter.startedSceneId!==eventId)
        throw new Error('As velas só podem ser concedidas após vencer Gonlab e registrar o retorno do filho.');
      if(!event.hexId||event.hexId!==campaign.currentHexId)throw new Error('O grupo precisa retornar ao hex de Engsle para receber a recompensa.');
      const characters=tx.objectStore('characters');const character=await characters.get(recipientId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      if(character!.status==='dead')throw new Error('Uma personagem morta não pode receber as velas.');
      const itemId=`official-candle-engsle:${eventId}`;
      if((character!.gear||[]).some((item:any)=>item?.id===itemId))throw new Error('As dez velas deste encontro já foram concedidas.');
      const candles={id:itemId,name:'Vela religiosa de Engsle',quantity:10,source:'Guia do Mestre R88, encontro #25 p.158',
        prepared:false,requiresBrilhazulPowderedDip:true,effect:'Purificar Espírito',powerLevel:1};
      await characters.put({...character!,gear:[...(character!.gear||[]),candles]});
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,
        detail:`${item.detail} O filho retornou conforme adjudicação registrada. Dez velas foram entregues; ainda exigem banho em Brilhazul pulverizada para funcionar.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'candle_reward_claimed',pending:null,candleChildReturned:true,
          candleRewardPending:false,candleCount:10,candleRecipientId:recipientId,candleItemId:itemId,candlePrepared:false,
          returnEvidence:evidence,resolvedAt:now,adjudicationMode:'official_child_return_confirmed'}}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** #10: first generate the source profile and retain every actual die face.
   * A printed-table gap stays visible and cannot be silently rerolled.
   */
  static async generateOfficialGrelfProfile(campaignId:string,eventId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==10||
        d?.choiceId!=='confront'||d.stage!=='pending_external_mechanics'||d.grelfGeneratedProfile||event.hexId!==campaign.currentHexId||world.encounter?.status==='active')
        throw new Error('Grelf não está pronto para geração oficial neste hex.');
      const generated=OfficialDemonGenerator.generate(rng),now=Date.now();
      const events=world.journey.events.map(e=>e.id===eventId?{...e,result:{...(e.result||{}),officialSceneDecision:{...d,
        stage:'grelf_profile_generated',pending:generated.sourceGaps.length?'source_gap':'combat',grelfGeneratedProfile:generated,grelfGeneratedAt:now}}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(outcome);return outcome;
  }
  static async startOfficialGrelfCombat(campaignId:string,eventId:string):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const profile=d?.grelfGeneratedProfile as OfficialGeneratedDemon|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==10||
        d?.stage!=='grelf_profile_generated'||d.choiceId!=='confront'||!profile||profile.sourceGaps.length||
        profile.combatant.entityId!=='official-generated-grelf'||event.hexId!==campaign.currentHexId||world.encounter?.status==='active')
        throw new Error('A ficha gerada de Grelf ainda possui lacuna de fonte ou o combate não está autorizado.');
      const party=CampaignPartyService.members(campaign,await tx.objectStore('characters').getAll() as Character[]).filter(c=>c.status!=='dead');
      if(!party.length)throw new Error('É necessário grupo vivo.');
      const encounter=EncounterService.createEncounter('Grelf — geração oficial pp.79–83',event.hexId,party,[profile.combatant],eventId);
      const triggered=EncounterService.triggerWatchOnEncounter(world,encounter,party,Number(campaign.currentDay||1),String(campaign.currentPeriod||'Morning'));
      for(const roll of triggered.rolls)await tx.objectStore('rolls').put(roll);
      const now=Date.now();const events=triggered.world.journey.events.map(e=>e.id===eventId?{...e,result:{...(e.result||{}),officialSceneDecision:{...d,stage:'combat_active',pending:'combat',combatEncounterId:encounter.id,combatStartedAt:now,automaticCombatStarted:true}}}:e);
      const next={...triggered.world,encounter:triggered.encounter,journey:{...triggered.world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(outcome);return outcome;
  }
  /** Situational exposure/contact and optional abilities remain explicit GM
   * operations. This receipt never declares victory or replaces battle rolls.
   */
  static async recordOfficialGrelfSituationalResolution(campaignId:string,eventId:string,evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,1200);if(evidence.length<20)throw new Error('Registre as regras situacionais e efeitos aplicados (mínimo 20 caracteres).');
    const outcome=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==10||d?.stage!=='combat_active'||d.grelfSituationalResolutionAt||
        world.encounter?.id!==d.combatEncounterId||world.encounter.status!=='resolved'||event.hexId!==campaign.currentHexId)
        throw new Error('A conferência de Grelf exige o combate real encerrado e ainda não conciliado.');
      const now=Date.now(),events=world.journey.events.map(e=>e.id===eventId?{...e,result:{...(e.result||{}),officialSceneDecision:{...d,grelfSituationalEvidence:evidence,grelfSituationalResolutionAt:now}}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(outcome);return outcome;
  }

  /** #2/#17: the GM explicitly selects source-backed typical profiles where
   * the encounter provides no individual stats/counts. Generic manual combat
   * remains blocked. The receipt identifies the adoption and its evidence.
   */
  static async startOfficialAdoptedSocialCombat(campaignId:string,eventId:string,evidenceInput:string,orcCount?:number,humanCount?:number):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre a adoção explícita dos perfis típicos pela Mestra (mínimo 12 caracteres).');
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const id=Number((event?.result?.mechanic as Record<string,unknown>|undefined)?.encounterId);
      if(!event||![2,17].includes(id)||d?.stage!=='pending_external_mechanics'||d.pending!=='combat'||
        d.choiceId!==(id===2?'attack':'join_fight')||event.hexId!==campaign.currentHexId||world.encounter?.status==='active'||
        world.journey.events.some(e=>e.id!==eventId&&e.status==='open'&&['combat_active','in_combat'].includes(String((e.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))))
        throw new Error('Este confronto não está pronto para adoção de perfis neste hex.');
      const party=CampaignPartyService.members(campaign,await tx.objectStore('characters').getAll() as Character[]).filter(c=>c.status!=='dead');
      if(!party.length)throw new Error('É necessário um grupo vivo no combate.');
      const enemies=OfficialEncounterNpcFactory.adoptedSocialProfiles(id as 2|17,Number(orcCount),Number(humanCount));
      const encounter=EncounterService.createEncounter(id===2?'Ensaio interrompido — perfis típicos adotados':'Briga do casamento — composição da Mestra',event.hexId,party,enemies,eventId);
      const triggered=EncounterService.triggerWatchOnEncounter(world,encounter,party,Number(campaign.currentDay||1),String(campaign.currentPeriod||'Morning'));
      for(const roll of triggered.rolls)await tx.objectStore('rolls').put(roll);
      const now=Date.now();const events=triggered.world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} A Mestra adotou perfis típicos publicados; a fonte não fornece estas fichas individuais.`,
        result:{...(e.result||{}),officialSceneDecision:{...d,stage:'combat_active',pending:'combat',
          combatEncounterId:encounter.id,combatStartedAt:now,automaticCombatStarted:true,
          adoptedProfileEvidence:evidence,individualStatisticsPublished:false,adoptedProfiles:enemies.map(a=>({entityId:a.entityId,profile:a.quickSheet?.adoptedTypicalProfile,sourcePrintedPage:a.quickSheet?.sourcePrintedPage})),
          weddingOrcCount:id===17?orcCount:null,weddingHumanCount:id===17?humanCount:null}}}:e);
      const next={...triggered.world,encounter:triggered.encounter,journey:{...triggered.world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(outcome);return outcome;
  }

  /** R85: ponte ATÔMICA para combate oficial dos encontros #3–#5, #7 e #8.
   * Não passa por startManual: isso deixaria a ocorrência de jornada solta.
   * A força total das três harpias (9) é dividida em três corpos de força 3.
   */
  static async startOfficialSceneCombat(campaignId:string,eventId:string,orcClan?:'Roka'|'Urhur'|'Isir',trollStrength?:number|'leather'|'mail',rng:()=>number=Math.random,skeletonChainmailCount?:number,options?:{
    minotaurSide?:'markut'|'soldiers';
    saurianSpear?:'trident'|'long_spear';
    merchantSisterLoadouts?:{
      ginda:{melee:'broad_sword'|'battle_axe';ranged:'long_spear'|'long_bow'|'heavy_crossbow';armor:'leather'|'chainmail'};
      aneta:{melee:'broad_sword'|'battle_axe';ranged:'long_spear'|'long_bow'|'heavy_crossbow';armor:'leather'|'chainmail'};
    };
  }):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state','characters','bestiary','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede iniciar outra batalha.');
      if(world.journey.events.some(item=>item.status==='open' &&
         ['in_combat','combat_active','blood_mist_combat_active','teramalda_spike_removed'].includes(String((item.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))))
        throw new Error('Conclua primeiro o combate oficial já vinculado à jornada.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const id=Number(mechanic?.encounterId);
      const siteVisit=Number(event?.result?.officialEncounterVisitNumber||1);
      if(!event||mechanic?.kind!=='official_random_encounter'||![3,4,5,7,8,11,12,18,20,21,23,24,25,29,30,33,35,37,40,41,42,43].includes(id)||
        (decision?.stage!=='pending_external_mechanics'&&!(id===18&&decision?.stage==='dough_captives_returned'))||
        !(decision.pending==='combat'||(id===7&&decision.choiceId==='help'&&decision.pending==='gm_decision')||
          (id===5&&decision.choiceId==='flee'&&decision.pending==='item_or_skill'))||
        !OfficialEncounterRulesEngine.specialSceneChoice(id,String(decision.choiceId),siteVisit))
        throw new Error('Esta ocorrência não possui combate oficial preparado para iniciar.');
      if(!event.hexId || event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex da ocorrência.');
      const all=await tx.objectStore('characters').getAll() as Character[];
      const party=CampaignPartyService.members(campaign,all).filter(item=>item.status!=='dead');
      if(!party.length)throw new Error('Não há aventureiros vivos para participar do combate.');
      if(id===5&&decision.choiceId==='bribe'){
        const bait=decision.harpyBaitItem as {characterId?:string;collection?:string;index?:number;fingerprint?:string}|undefined;
        const owner=party.find(item=>item.id===bait?.characterId);
        const category=bait?.collection;
        const currentItem=owner&&['gear','weapons','armor'].includes(String(category))
          ?owner[category as 'gear'|'weapons'|'armor']?.[Number(bait?.index)]:undefined;
        if(!owner||!currentItem||!bait?.fingerprint||JSON.stringify(currentItem)!==bait.fingerprint)
          throw new Error('O objeto brilhante preparado não está mais com a personagem. Escolha outro antes do combate.');
      }
      let enemies:EncounterCombatant[];
      let title:string;
      let roadBlockadeDelay:number|undefined;
      if(id===3){
        if(decision.choiceId!=='rescue')throw new Error('Escolta Roka só entra em combate se o grupo tentar resgatar Sargah.');
        if(!orcClan)throw new Error('Escolha explicitamente o clã orc da escolta antes do combate.');
        enemies=OfficialEncounterNpcFactory.orcEscort(party.length,orcClan);
        enemies.push(OfficialEncounterNpcFactory.sargahPrisoner());
        title=`O Fugitivo Orc — escolta ${orcClan} e Sargah`;
      }else if(id===12){
        if(decision.choiceId!=='fight')throw new Error('Os assaltantes só entram em combate se o grupo escolher enfrentá-los.');
        enemies=OfficialEncounterNpcFactory.hungryBandits(party.length);
        title='Assaltantes famintos — Argam Duasbarbas';
      }else if(id===7){
        // A trombeta de Harwa é o gatilho da emboscada, não um ferimento real.
        enemies=OfficialEncounterNpcFactory.slavers();
        title='Isca do Massacre — Harwa e os escravagistas';
      }else if(id===8){
        if(decision.choiceId!=='attack')throw new Error('Khardnal só combate quando atacado.');
        enemies=[OfficialEncounterNpcFactory.khardnal()];
        title='Khardnal — Grande Insultador';
      }else if(id===18){
        const returningCaptives=siteVisit>=3&&decision.choiceId==='wait_for_melkar'&&decision.stage==='dough_captives_returned';
        if(!returningCaptives&&!(siteVisit<=2&&decision.choiceId==='attack'))
          throw new Error('A massa demoníaca só entra em combate após ataque nas duas primeiras visitas ou retorno de Melkar com cativos na terceira.');
        const masses=returningCaptives?200:siteVisit===2?70:20;
        enemies=[OfficialEncounterNpcFactory.demonicDough(masses)];
        title=returningCaptives?'O Padeiro Demoníaco retorna com cativos — duzentas massas':`O Padeiro Demoníaco — ${masses} massas de pão`;
      }else if(id===20){
        if(decision.choiceId!=='attack'||decision.pending!=='combat'||decision.processionAction!=='attack'||decision.warningHeard!==true)
          throw new Error('Os Ents só entram em combate depois da advertência dos elfos e da decisão explícita de atacar.');
        enemies=OfficialEncounterNpcFactory.processionEnts();
        title='A Procissão Fúnebre — cinco Ents';
      }else if(id===21){
        if(decision.choiceId!=='attack')throw new Error('Garmagol só entra em combate quando as aventureiras escolhem atacá-lo.');
        enemies=[OfficialEncounterNpcFactory.vengefulGhostGarmagol()];
        title='O Espírito Vingativo — Garmagol';
      }else if(id===23){
        if(decision.choiceId!=='protect_puppy')throw new Error('Ghormald só entra em combate quando o grupo escolhe proteger o filhote.');
        if(!Number.isInteger(trollStrength)||Number(trollStrength)<12||Number(trollStrength)>16)
          throw new Error('A Mestra deve escolher a Força do troll entre 12 e 16, conforme R88 p.114.');
        enemies=[OfficialEncounterNpcFactory.encounterTroll('Ghormald',Number(trollStrength))];
        title='O Filhote — Ghormald';
      }else if(id===24){
        if(decision.choiceId!=='attack_kurge')throw new Error('Kurge só entra em combate após escolha explícita de ataque.');
        enemies=[OfficialEncounterNpcFactory.encounterOgre('Kurge')];
        title='O Ogro Amaldiçoado — Kurge e Oker';
      }else if(id===25){
        if(decision.choiceId!=='confront_gonlab')throw new Error('Gonlab só entra em combate após escolha explícita de confronto.');
        enemies=[OfficialEncounterNpcFactory.encounterOgre('Gonlab')];
        title='Fabricantes de Velas — Gonlab';
      }else if(id===29){
        if(trollStrength===undefined&&skeletonChainmailCount===undefined)throw new Error('Escolha a armadura típica dos esqueletos antes de iniciar.');
        if(decision.choiceId!=='attack')throw new Error('Os esqueletos atacam sem aviso; a ocorrência deve estar na etapa de combate.');
        let die=Number.isInteger(Number(decision.skeletonCountD6))?Number(decision.skeletonCountD6):1+Math.floor(Math.max(0,Math.min(.999999999,rng()))*6);
        const skeletonCount=Number.isInteger(Number(decision.skeletonCount))?Number(decision.skeletonCount):die+3;
        if(die<1||die>6||skeletonCount!==die+3)throw new Error('O registro de quantidade dos esqueletos não corresponde a D6+3.');
        const chainmailCount=skeletonChainmailCount??0;
        if(!Number.isInteger(chainmailCount)||chainmailCount<0||chainmailCount>skeletonCount)
          throw new Error(`Escolha entre zero e ${skeletonCount} esqueletos com cota de malha.`);
        // R105 compatibility: existing 4th-argument 'leather'/'mail' calls remain valid.
        // New Codex numeric distribution remains canonical for the expanded panel.
        const legacyArmor=trollStrength==='leather'||trollStrength==='mail'?trollStrength:undefined;
        enemies=legacyArmor?OfficialEncounterNpcFactory.skeletonAmbush(legacyArmor,rng)
          :OfficialEncounterNpcFactory.skeletons(skeletonCount,rng,chainmailCount);
        title=`O Chacoalhar dos Ossos — ${enemies.length} esqueletos`;
        if(legacyArmor)die=enemies.length-3;
        decision.skeletonCount=enemies.length;decision.skeletonCountD6=die;
        decision.skeletonChainmailCount=legacyArmor?(legacyArmor==='mail'?enemies.length:0):chainmailCount;
        decision.skeletonLeatherCount=enemies.length-Number(decision.skeletonChainmailCount);
        decision.skeletonArmorChosen=legacyArmor||'mixed';decision.skeletonArmorRatingSourceMissing=true;
      }else if(id===30){
        if(decision.choiceId!=='attack')throw new Error('Teramalda só inicia combate por ataque do grupo ou agressão automática da visita R88.');
        const saved=await tx.objectStore('bestiary').getAll();
        const entry=saved.find(item=>item.id==='core-bestiary:death-knight')||BookOfBeastsCatalogService.entries().find(item=>item.id==='core-bestiary:death-knight');
        if(!entry)throw new Error('Tabela oficial de ataques do Cavaleiro da Morte R88 p.76 ausente.');
        const base=EncounterService.bestiaryCombatant(entry,1,rng);
        enemies=[OfficialEncounterNpcFactory.teramalda(base)];
        decision.teramaldaVisit=siteVisit;decision.teramaldaEncounterStage=OfficialEncounterRulesEngine.teramaldaEncounterStage(siteVisit,siteVisit===1);
        decision.teramaldaPursuesAfterFlee=siteVisit>=4;decision.teramaldaDwarfHunt=true;
        title='Teramalda — Cavaleira da Morte';
      }else if(id===33){
        const supported=decision.choiceId==='help_markut'?'markut':decision.choiceId==='help_soldiers'?'soldiers':null;
        if(!supported||options?.minotaurSide!==supported)
          throw new Error('Escolha o lado que as aventureiras apoiarão nesta luta; a R88 deixa essa decisão à mesa.');
        const markut=OfficialEncounterNpcFactory.minotaurMarkut(rng);
        const soldiers=OfficialEncounterNpcFactory.minotaurEncounterSoldiers(rng);
        const allies=supported==='markut'?[{...markut,side:'party' as const}]:soldiers.map(item=>({...item,side:'party' as const}));
        const opposition=supported==='markut'?soldiers:[{...markut,side:'enemy' as const}];
        enemies=[...allies,...opposition];
        title=`O Lamento do Minotauro — lado dos ${supported==='markut'?'minotauro':'soldados'}`;
        decision.minotaurSupportedSide=supported;decision.minotaurSoldierCount=4;
        decision.minotaurId=markut.entityId;decision.minotaurCombatProfileSource='R88_GM_GUIDE_P104';
      }else if(id===35){
        if(decision.choiceId!=='fight')throw new Error('Karg só entra em combate quando a mesa escolhe enfrentá-lo.');
        if(!Number.isInteger(trollStrength)||Number(trollStrength)<12||Number(trollStrength)>16)
          throw new Error('A Mestra deve escolher a Força do troll entre 12 e 16, conforme R88 pp.114–115.');
        enemies=[OfficialEncounterNpcFactory.encounterTroll('Karg',Number(trollStrength),rng)];
        title='O Troll Sonolento — Karg';
      }else if(id===37){
        if(decision.choiceId!=='attack')throw new Error('O bloqueio só inicia combate depois da escolha de atacar os guardas.');
        const die=rng();
        if(!Number.isFinite(die)||die<0||die>=1)throw new Error('Fonte de dados inválida para o reforço anão D6.');
        roadBlockadeDelay=1+Math.floor(die*6);
        enemies=OfficialEncounterNpcFactory.roadBlockadeDwarves(1,3,rng);
        title='Bloqueio na Estrada — guerreiros de Guntermann';
      }else if(id===40){
        if(decision.choiceId!=='fight')throw new Error('O ataque do polvo gigante exige iniciar o combate oficial.');
        const saved=await tx.objectStore('bestiary').getAll();
        const entry=saved.find(item=>item.id==='core-bestiary:giant-octopus')||BookOfBeastsCatalogService.entries().find(item=>item.id==='core-bestiary:giant-octopus');
        if(!entry)throw new Error('A ficha oficial do Polvo Gigante R88 está ausente.');
        enemies=[{...EncounterService.bestiaryCombatant(entry,1,rng),name:'Polvo Gigante'}];
        title='A Morte Vem de Baixo — Polvo Gigante';
      }else if(id===41){
        if(decision.choiceId!=='attack'||!options?.merchantSisterLoadouts)
          throw new Error('Ataque aos guardas do mercador exige escolha explícita do equipamento de Ginda e Aneta.');
        enemies=[OfficialEncounterNpcFactory.merchantGuard('Ginda',1,options.merchantSisterLoadouts.ginda,rng),
          OfficialEncounterNpcFactory.merchantGuard('Aneta',2,options.merchantSisterLoadouts.aneta,rng)];
        title='O Mercador de Almas — Ginda e Aneta';
      }else if(id===42){
        if(decision.choiceId!=='attack')throw new Error('Os caçadores sáurios só entram em combate após escolha explícita de ataque.');
        const dieValue=rng();if(!Number.isFinite(dieValue)||dieValue<0||dieValue>=1)throw new Error('Fonte de dados inválida para o D6 dos caçadores sáurios.');
        const die=1+Math.floor(dieValue*6);
        const spear=options?.saurianSpear;
        if(!spear)throw new Error('Escolha explicitamente tridente ou lança longa para os caçadores sáurios.');
        const partySize=CampaignPartyService.members(campaign,all).filter(item=>item.status!=='dead').length;
        enemies=OfficialEncounterNpcFactory.saurianHunters(partySize,die,spear,rng);
        decision.saurianCountD6=die;decision.saurianPartySize=partySize;decision.saurianTotalCount=partySize+die;decision.saurianSpear=spear;
        title=`Os Caçadores Reptilianos — Sheeerz e ${partySize+die-1} caçadores`;
      }else if(id===43){
        if(decision.choiceId!=='attack')throw new Error('Os caçadores só entram em combate após escolha explícita de ataque.');
        enemies=OfficialEncounterNpcFactory.bountyHunters(rng);
        title='Enterrados Vivos — Penazul e caçadores de recompensa';
      }else{
        const profileId=id===4?'core-bestiary:griffin':id===11?'core-bestiary:death-knight':'core-bestiary:harpies';
        const saved=await tx.objectStore('bestiary').getAll();
        const entry=saved.find(item=>item.id===profileId)||BookOfBeastsCatalogService.entries().find(item=>item.id===profileId);
        if(!entry)throw new Error('Perfil oficial do bestiário ausente; combate não iniciado.');
        const names=id===5?['Krag','Mag','Serag']:id===11?['Príncipe esquecido — Cavaleiro da Morte']:['Grifo territorial'];
        enemies=names.map((name,index)=>{
          const combatant=EncounterService.bestiaryCombatant(entry,index+1);
          return {...combatant,name,...(id===5?{currentStrength:3,maxStrength:3}:{})};
        });
        title=id===4?'Grifo — território defendido':id===11?'O Príncipe Esquecido — Cavaleiro da Morte':'Harpias — Krag, Mag e Serag';
      }
      const encounter=EncounterService.createEncounter(title,event.hexId,party,enemies,event.id);
      encounter.combatants=encounter.combatants.map(actor=>({...actor,quickSheet:{...(actor.quickSheet||{}),initialArmor:actor.armor}}));
      if(id===37)encounter.officialEncounter={encounterId:37,reinforcementsAtRound:1+Number(roadBlockadeDelay),reinforcementsPending:true};
      if(id===40)encounter.officialEncounter={encounterId:40};
      if(id===41||id===42||id===43)encounter.officialEncounter={encounterId:id};
      const triggered=EncounterService.triggerWatchOnEncounter(world,encounter,party,Number(campaign.currentDay||1),String(campaign.currentPeriod||'Morning'));
      for(const roll of triggered.rolls)await tx.objectStore('rolls').put(roll);
      const now=Date.now();
      const events=triggered.world.journey.events.map(item=>item.id===eventId?{
        ...item,result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'combat_active',
          combatEncounterId:encounter.id,automaticCombatStarted:true,combatStartedAt:now,
          ...(id===3?{orcEscortCount:party.length*2,orcEscortClan:orcClan,sargahPrisonerId:'official-sargah',sargahFate:'pending_actual_release'}:{}),
          ...(id===12?{banditCount:party.length,banditLeaderId:'official-hungry-bandit:1'}:{}),
          ...(id===29?{skeletonCount:enemies.length,skeletonArmorChosen:String(decision.skeletonArmorChosen||'mixed'),skeletonsDoNotPursueOutsideLineOfSight:true}:{}),
          ...(id===11?{tombGuardianId:'core-bestiary:death-knight',tombLoot:'unclaimed'}:{}),
          ...(id===18?{doughMasses:Number((enemies[0].quickSheet as Record<string,unknown>|undefined)?.doughMasses||20),
            doughCreatureStrength:enemies[0].currentStrength,doughAttackDamage:1,doughAttackDamageType:'piercing'}:{}),
          ...(id===20?{processionEntCount:5,deadEntBodySetDown:true,elvesCombatantsCreated:false}:{}),
          ...(id===21?{ghostId:'official-vengeful-ghost:garmagol',ghostCanBeDestroyedOnlyByPurgeUndead:true,returnsAfterQuarterDay:true}:{}),
          ...(id===23?{trollId:'official-encounter-troll:ghormald',trollStrength:Number(trollStrength),trollRegenerationPerRound:1}:{}),
          ...(id===24?{ogreId:'official-encounter-ogre:kurge',prisoner:'Oker'}:{}),
          ...(id===25?{ogreId:'official-encounter-ogre:gonlab',capturedChild:true,candleRewardPending:true}:{}),
          ...(id===29?{skeletonCount:Number(decision.skeletonCount),skeletonCountD6:Number(decision.skeletonCountD6),
            skeletonChainmailCount:Number(decision.skeletonChainmailCount||0),skeletonLeatherCount:Number(decision.skeletonLeatherCount||0),skeletonArmorRatingSourceMissing:true}:{}),
          ...(id===35?{trollId:'official-encounter-troll:karg',trollStrength:Number(trollStrength),trollRegenerationPerRound:1}:{}),
          ...(id===37?{dwarfInitialGuardCount:3,dwarfReinforcementCount:3,dwarfReinforcementDelay:roadBlockadeDelay,
            dwarfReinforcementArrivalRound:1+Number(roadBlockadeDelay),dwarfReinforcementsAdded:false}:{}),
          ...(id===40?{giantOctopusId:'core-bestiary:giant-octopus',octopusHalfTentacleRulePending:true,tentacleTotalUnspecified:true}:{}),
          ...(id===41?{merchantGuardIds:enemies.map(enemy=>enemy.entityId),merchantGuardLoadouts:options?.merchantSisterLoadouts,soulBottleStockUnspecified:true}:{}),
          ...(id===42?{saurianHunterCount:enemies.length,saurianPartySize:decision.saurianPartySize,
            saurianCountD6:decision.saurianCountD6,saurianSpear:decision.saurianSpear,sheeerzId:'official-r88-saurian-hunter:1'}:{}),
          ...(id===43?{bountyHunterCount:7,penazulId:'official-r88-bounty-hunter:1',familyRescuePending:true}:{}),
          ...(id===30?{teramaldaId:'official-teramalda',teramaldaVisit:siteVisit,
            teramaldaEncounterStage:Number(decision.teramaldaEncounterStage||siteVisit),teramaldaPursuesAfterFlee:siteVisit>=4,
            teramaldaImmobilized:false,spikeRemoved:false}:{}),
          ...(id===7?{slaversTrumpetSounded:true,slaversDoNotPursue:true,slaversCaptureIntent:true}:{}),
          ...(id===8?{khardnalProvokedIntoCombat:true}:{})}}
      }:item);
      const updated={...triggered.world,encounter:triggered.encounter,
        journey:{...triggered.world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });
    this.notify(outcome);return outcome;
  }

  /** R88 #30 p.160: a Mestra precisa confirmar a imobilização antes do teste
   * de retirar o espigão; texto livre não é convertido em efeito de combate.
   */
  static async recordOfficialTeramaldaImmobilization(campaignId:string,eventId:string,evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre como a MdJ imobilizou Teramalda (mínimo 12 caracteres).');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const encounter=world.encounter;
      const teramalda=encounter?.combatants.find(item=>item.entityId==='official-teramalda'&&item.side==='enemy'&&item.status==='active');
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==30||
        decision?.stage!=='combat_active'||!encounter||encounter.status!=='active'||encounter.id!==decision.combatEncounterId||
        encounter.startedSceneId!==eventId||!teramalda||event.hexId!==campaign.currentHexId)
        throw new Error('Teramalda não está em combate oficial ativo neste hex.');
      if(decision.teramaldaImmobilized===true)throw new Error('A imobilização já foi registrada.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{...item,detail:`${item.detail} Imobilização de Teramalda confirmada pela MdJ: ${evidence}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,teramaldaImmobilized:true,teramaldaImmobilizationEvidence:evidence,teramaldaImmobilizedAt:now}}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** Ação lenta real de Potência Formidável (−3). Um sucesso retira o espigão
   * e encerra Teramalda; falha não causa dano nem a remove da luta.
   */
  static async attemptOfficialTeramaldaSpikeRemoval(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<{
    world:WorldState;roll:import('../types').DiceRoll;removed:boolean;
  }>{
    const result=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const encounter=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==30||
        decision?.stage!=='combat_active'||decision.teramaldaImmobilized!==true||decision.spikeRemoved===true||
        !encounter||encounter.status!=='active'||encounter.id!==decision.combatEncounterId||encounter.startedSceneId!==eventId||
        event.hexId!==campaign.currentHexId)
        throw new Error('Teramalda precisa estar imobilizada no combate oficial antes de retirar o espigão.');
      const actor=encounter.combatants[encounter.turnIndex];
      if(!actor||actor.kind!=='character'||actor.side!=='party'||actor.entityId!==characterId||actor.status!=='active'||!actor.slowActionAvailable)
        throw new Error('A retirada do espigão exige a ação lenta da personagem ativa no combate.');
      const target=encounter.combatants.find(item=>item.entityId==='official-teramalda'&&item.side==='enemy'&&item.status==='active');
      if(!target||target.quickSheet?.officialTeramalda!==true)throw new Error('A Teramalda ativa não corresponde à ficha oficial.');
      const characters=tx.objectStore('characters');let character=await characters.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      const skill=normalizedSkill(character,['Might','Potência']);
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill,gear:0,modifier:-3},{
        characterId,sceneId:encounter.startedSceneId||encounter.id,
        checkId:`official-teramalda-spike:${eventId}:${encounter.round}:${characterId}`,
        rng,criticalInjuries:character.criticalInjuryStates,skillName:'might',attributeName:'strength',skillRankAtRoll:skill,
      });
      await tx.objectStore('rolls').put(roll);
      const consequences=CriticalInjuryRulesEngine.applyCheckConsequences(character,'might');character=consequences.character;
      if(consequences.triggers.length)await characters.put(character);
      const removed=roll.successes>0;const now=Date.now();
      const combatants=encounter.combatants.map(item=>item.id===actor.id?{...item,slowActionAvailable:false}:item.id===target.id&&removed?{
        ...item,currentStrength:0,status:'dead' as const,brokenAttribute:undefined,
        quickSheet:{...(item.quickSheet||{}),attributes:{...((item.quickSheet?.attributes||{}) as Record<string,unknown>),
          strength:{current:0,max:item.maxStrength}},spikeRemoved:true},
      }:item);
      const log={id:crypto.randomUUID(),round:encounter.round,combatantId:actor.id,
        text:`${character.name} tentou retirar o espigão de Teramalda: Potência Formidável (−3), ${roll.successes} sucesso(s). ${removed?'O espigão foi removido; Teramalda foi morta.':'Falhou; Teramalda permanece imune e ativa.'}`,
        createdAt:now};
      const nextEncounter=EncounterService.resolveIfFinished({...encounter,combatants,
        log:[...encounter.log,log].slice(-300),updatedAt:now},now,removed?'Espigão mágico removido de Teramalda conforme R88 p.160.':undefined);
      const events=world.journey.events.map(item=>item.id===eventId?{...item,
        detail:`${item.detail} ${log.text}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,
          ...(removed?{stage:'teramalda_spike_removed',pending:null,spikeRemoved:true,spikeRemovedBy:characterId,spikeRemovalRollId:roll.id}:{}),
          teramaldaSpikeAttempts:[...((decision.teramaldaSpikeAttempts as unknown[]|undefined)||[]),{characterId,rollId:roll.id,successes:roll.successes,at:now}],
        }}}:item);
      const updated={...world,encounter:nextEncounter,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return{world:updated,roll,removed};
    });this.notify(result.world);return result;
  }

  /** #29: a MdJ confirms that the party broke line of sight after real flee
   * actions. Active skeletons stop pursuing only at that source-defined point.
   */
  static async confirmOfficialSkeletonPursuitEnded(campaignId:string,eventId:string,evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Descreva como o grupo rompeu a linha de visão (mínimo 12 caracteres).');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const encounter=world.encounter;
      const party=encounter?.combatants.filter(item=>item.side==='party')||[];
      const skeletons=encounter?.combatants.filter(item=>item.side==='enemy'&&item.entityId?.startsWith('official-r88-skeleton:'))||[];
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==29||
        decision?.stage!=='combat_active'||!encounter||encounter.status!=='resolved'||encounter.id!==decision.combatEncounterId||
        encounter.startedSceneId!==eventId||event.hexId!==campaign.currentHexId||!party.length||party.some(item=>item.status!=='fled')||
        !skeletons.length||skeletons.some(item=>item.status!=='active'))
        throw new Error('Confirme a perseguição apenas depois de todo o grupo fugir no combate #29 e perder os esqueletos de vista.');
      const now=Date.now();const log={id:crypto.randomUUID(),round:encounter.round,
        text:`A MdJ confirmou que os esqueletos perderam o grupo de vista: ${evidence}`,createdAt:now};
      const combatants=encounter.combatants.map(item=>skeletons.some(skeleton=>skeleton.id===item.id)?{...item,status:'fled' as const}:item);
      const updatedEncounter={...encounter,combatants,log:[...encounter.log,log].slice(-300),updatedAt:now};
      const events=world.journey.events.map(item=>item.id===eventId?{...item,detail:`${item.detail} A perseguição terminou quando os esqueletos perderam o grupo de vista.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,skeletonPursuitBroken:true,skeletonPursuitEvidence:evidence,
          skeletonPursuitEndedAt:now}}}:item);
      const updated={...world,encounter:updatedEncounter,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R88 #19: avança o santuário sem dano, deslocamento ou combate presumidos.
   * As quantidades de animais não são fixadas pela fonte; combate da líder permanece pendente.
   */
  static async advanceOfficialAnimalSanctuary(campaignId:string,eventId:string,
    action:'approach'|'attack_animals'|'wait_for_leader'|'attack_leader'|'leave_sanctuary'):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==19||mechanic?.kind!=='official_random_encounter'||
        !event.hexId||event.hexId!==campaign.currentHexId||!decision?.choiceId)
        throw new Error('A cena do santuário animal não está aberta neste hex.');
      const stage=String(decision.stage||'');let nextStage:string;let message:string;let closes=false;
      if(action==='approach'&&decision.choiceId==='linger'&&stage==='pending_external_mechanics'){
        nextStage='sanctuary_animals_approaching';message='Mais animais chegam e tentam empurrar gentilmente o grupo e seus pertences.';
      }else if(action==='attack_animals'&&((stage==='sanctuary_animals_approaching')||
        (stage==='pending_external_mechanics'&&decision.choiceId==='attack'))){
        nextStage='sanctuary_animals_withdrew';message='Os animais se afastaram, mas permanecem no santuário. Nenhum dano foi aplicado.';
      }else if(action==='wait_for_leader'&&['sanctuary_animals_approaching','sanctuary_animals_withdrew'].includes(stage)){
        nextStage='sanctuary_black_elk_arrived';message='A Alce Negra chegou, espalhou a fogueira com os chifres e bufou.';
      }else if(action==='attack_leader'&&stage==='sanctuary_black_elk_arrived'){
        nextStage='sanctuary_leader_attacked_pending_combat';message='A Alce Negra foi atacada. Todos os animais defendem o santuário até o grupo deixar a floresta; a ficha e a quantidade dos combatentes ainda precisam de adjudicação com os perfis oficiais da p. 126.';
      }else if(action==='leave_sanctuary'&&['pending_external_mechanics','sanctuary_animals_approaching','sanctuary_animals_withdrew','sanctuary_black_elk_arrived'].includes(stage)){
        nextStage='sanctuary_left';message='O grupo desmontou o acampamento e deixou o santuário em paz. Movimento adicional continua a cargo da viagem normal.';closes=true;
      }else throw new Error('Ação não disponível nesta etapa do santuário animal.');
      const now=Date.now();const events=world.journey.events.map(item=>item.id===eventId?{...item,
        ...(closes?{status:'resolved' as const}:{}),detail:`${item.detail} ${message}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:nextStage,pending:closes?null:'gm_decision',
          animalSanctuaryAction:action,animalSanctuaryUpdatedAt:now,...(closes?{resolvedAt:now}:{})},
          ...(closes?{resolvedAt:now,adjudicationMode:'official_sanctuary_explicit_departure'}:{})}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(outcome);return outcome;
  }

  /** Só conclui a ocorrência depois que o motor de combate registrou o desfecho.
   * Não declara vitória, loot ou dano por conta própria.
   */
  static async completeOfficialSceneCombat(campaignId:string,eventId:string):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const encounter=world.encounter;
      const officialId=Number((event?.result?.mechanic as Record<string,unknown>|undefined)?.encounterId);
      const teramaldaKilled=officialId===30&&decision?.stage==='teramalda_spike_removed'&&decision.spikeRemoved===true;
      if(!event||(decision?.stage!=='combat_active'&&!teramaldaKilled)||!decision.combatEncounterId||
        !encounter||encounter.id!==decision.combatEncounterId||encounter.startedSceneId!==eventId)
        throw new Error('O combate vinculado à ocorrência não foi encontrado.');
      if(encounter.status!=='resolved')throw new Error('Finalize o combate no motor antes de encerrar a ocorrência.');
      if(!event.hexId || event.hexId!==campaign.currentHexId || encounter.hexId!==event.hexId)
        throw new Error('O grupo está fora do hex de origem ou o combate pertence a outro hex.');
      if(!encounter.resolvedAt || encounter.resolvedAt<Number(decision.combatStartedAt||0))
        throw new Error('O combate original não possui recibo temporal válido.');
      if(!encounter.combatants.some(item=>item.side==='enemy'))
        throw new Error('O combate vinculado não possui adversários reais.');
      if(officialId===10){
        const profile=decision.grelfGeneratedProfile as OfficialGeneratedDemon|undefined;
        const demons=encounter.combatants.filter(c=>c.side==='enemy'&&c.entityId==='official-generated-grelf');
        if(!profile||profile.sourceGaps.length||demons.length!==1||demons[0].maxStrength!==profile.combatant.maxStrength||
          demons[0].quickSheet?.officialGeneratedDemon!==true||!decision.grelfSituationalResolutionAt||!decision.grelfSituationalEvidence)
          throw new Error('Grelf exige ficha gerada oficial, combate real e conferência das regras situacionais pela Mestra.');
      }
      if([2,17].includes(officialId)){
        const adopted=decision.adoptedProfiles as Array<{entityId:string;profile:string;sourcePrintedPage:number}>|undefined;
        const actual=encounter.combatants.filter(c=>c.side==='enemy');
        if(!adopted||!String(decision.adoptedProfileEvidence||'').trim()||decision.individualStatisticsPublished!==false||
          actual.length!==adopted.length||actual.some(a=>!adopted.some(p=>p.entityId===a.entityId&&p.profile===a.quickSheet?.adoptedTypicalProfile&&p.sourcePrintedPage===a.quickSheet?.sourcePrintedPage)))
          throw new Error('O combate não corresponde aos perfis típicos explicitamente adotados pela Mestra.');
      }
      if(officialId===3 && (encounter.combatants.filter(c=>c.entityId?.startsWith('official-orc-escort:')&&c.side==='enemy').length!==Number(decision.orcEscortCount)||
        !encounter.combatants.some(c=>c.entityId==='official-sargah'&&c.side==='neutral')))
        throw new Error('O recibo não corresponde à escolta orc e ao prisioneiro Sargah.');
      if(officialId===7 && (encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId?.startsWith('official-slaver:')).length!==12||
        !encounter.combatants.some(item=>item.entityId==='official-slaver:1'&&item.name.startsWith('Harwa'))))
        throw new Error('O recibo não contém os doze escravagistas oficiais e Harwa.');
      if(officialId===29 && (encounter.combatants.filter(item=>(item.entityId?.startsWith('official-skeleton:')||item.entityId?.startsWith('official-r88-skeleton:'))&&item.side==='enemy').length!==Number(decision.skeletonCount)||
        Number(decision.skeletonCount)<4||Number(decision.skeletonCount)>9))
        throw new Error('Recibo de combate não corresponde ao número D6+3 de esqueletos.');
      if(officialId===8 && !encounter.combatants.some(item=>item.entityId==='official-khardnal'))
        throw new Error('O recibo não contém Khardnal com ficha oficial.');
      if(officialId===18){
        const dough=encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-demonic-dough');
        const masses=Number(decision.doughMasses||0);
        if(dough.length!==1||masses<1||Number((dough[0].quickSheet as Record<string,unknown>|undefined)?.doughMasses)!==masses||
          dough[0].maxStrength!==Math.ceil(masses/5)||Object.keys(((dough[0].quickSheet as Record<string,unknown>|undefined)?.skills||{}) as object).length!==0)
          throw new Error('O recibo não corresponde à massa do grupo, à Força derivada e à ausência de perícias.');
      }
      if(officialId===20){
        const ents=encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId?.startsWith('official-procession-ent:'));
        if(ents.length!==5||ents.some(item=>item.kind!=='monster'||item.maxStrength!==16||item.armor!==6||
          Number((item.quickSheet as Record<string,unknown>|undefined)?.fireDamageMultiplier)!==2||
          (item.quickSheet as Record<string,unknown>|undefined)?.physicalDamageImmune!==true||
          (item.quickSheet as Record<string,unknown>|undefined)?.officialProcessionEnt!==true)||
          decision.processionEntCount!==5||decision.deadEntBodySetDown!==true)
          throw new Error('O recibo não contém os cinco Ents canônicos da procissão R88.');
      }
      if(officialId===21){
        const ghosts=encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-vengeful-ghost:garmagol');
        if(ghosts.length!==1||ghosts[0].quickSheet?.officialVengefulGhost!==true||ghosts[0].maxStrength!==8||
          decision.ghostCanBeDestroyedOnlyByPurgeUndead!==true||decision.returnsAfterQuarterDay!==true)
          throw new Error('O recibo não contém Garmagol e as regras de banimento/destruição da fonte R88.');
      }
      if(officialId===23){
        const trolls=encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-encounter-troll:ghormald');
        if(trolls.length!==1||trolls[0].quickSheet?.officialEncounterTroll!==true||trolls[0].armor!==4||
          trolls[0].maxStrength!==Number(decision.trollStrength)||trolls[0].quickSheet?.trollRegenerationPerRound!==1)
          throw new Error('O recibo não contém Ghormald com a ficha e Força escolhida conforme R88 p.114.');
      }
      if(officialId===24&&encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-encounter-ogre:kurge').length!==1)
        throw new Error('O recibo não contém Kurge com a ficha típica de ogro R88 p.61.');
      if(officialId===25&&encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-encounter-ogre:gonlab').length!==1)
        throw new Error('O recibo não contém Gonlab com a ficha típica de ogro R88 p.61.');
      if(officialId===29){
        const skeletons=encounter.combatants.filter(item=>item.side==='enemy'&&(item.entityId?.startsWith('official-r88-skeleton:')||item.entityId?.startsWith('official-skeleton:')));
        if(skeletons.length!==Number(decision.skeletonCount)||skeletons.some(item=>(item.quickSheet?.officialR88Skeleton!==true&&item.quickSheet?.officialSkeleton!==true)||(item.quickSheet?.officialR88Skeleton===true&&item.quickSheet?.armorRatingSourceMissing!==true)||
          Number((item.quickSheet?.skills as Record<string,number>|undefined)?.Patrulha||0)!==3||
          !['leather','chainmail'].includes(String(item.quickSheet?.skeletonArmorType|| (item.quickSheet?.armorChosenByGm==='mail'?'chainmail':item.quickSheet?.armorChosenByGm)))))
          throw new Error('O recibo não contém a ficha típica de esqueleto da R88 p.106.');
        const chainmail=skeletons.filter(item=>(item.quickSheet?.skeletonArmorType==='chainmail'||item.quickSheet?.armorChosenByGm==='mail')).length;
        const leather=skeletons.filter(item=>(item.quickSheet?.skeletonArmorType==='leather'||item.quickSheet?.armorChosenByGm==='leather')).length;
        if(chainmail!==Number(decision.skeletonChainmailCount||0)||leather!==Number(decision.skeletonLeatherCount||0)||
          !Number.isInteger(Number(decision.skeletonCountD6))||Number(decision.skeletonCount)!==Number(decision.skeletonCountD6)+3)
          throw new Error('O recibo não corresponde ao D6+3, à armadura escolhida e às fichas de esqueleto da R88 p.106.');
        if(skeletons.some(item=>item.status==='active')&&decision.skeletonPursuitBroken!==true)
          throw new Error('Esqueletos ativos continuam perseguindo enquanto mantêm o grupo à vista.');
      }
      if(officialId===33){
        const supported=String(decision.minotaurSupportedSide||'');
        const minotaurs=encounter.combatants.filter(item=>item.entityId==='official-r88-minotaur-markut');
        const soldiers=encounter.combatants.filter(item=>item.entityId?.startsWith('official-r88-minotaur-soldier:'));
        const expectedMarkutSide=supported==='markut'?'party':'enemy';
        const expectedSoldierSide=supported==='markut'?'enemy':'party';
        if(!['markut','soldiers'].includes(supported)||minotaurs.length!==1||minotaurs[0].side!==expectedMarkutSide||
          minotaurs[0].quickSheet?.officialR88Minotaur!==true||minotaurs[0].maxStrength!==10||minotaurs[0].armor!==2||
          soldiers.length!==4||soldiers.some(item=>item.side!==expectedSoldierSide||item.quickSheet?.officialR88MinotaurEncounterSoldier!==true||
            item.maxStrength!==4||Number((item.quickSheet?.skills as Record<string,number>|undefined)?.Luta)!==2))
          throw new Error('O recibo não corresponde ao lado escolhido, ao Minotauro R88 p.104 e aos quatro soldados de #33 p.161.');
      }
      if(officialId===35){
        const trolls=encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-encounter-troll:karg');
        if(trolls.length!==1||trolls[0].quickSheet?.officialEncounterTroll!==true||
          Number(trolls[0].quickSheet?.officialEncounterId)!==35||trolls[0].armor!==4||
          trolls[0].maxStrength!==Number(decision.trollStrength)||trolls[0].quickSheet?.trollRegenerationPerRound!==1)
          throw new Error('O recibo não contém Karg com a ficha e Força escolhida conforme R88 pp.114–115.');
      }
      if(officialId===40){
        const octopus=encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='core-bestiary:giant-octopus');
        const cut=Number(decision.octopusTentaclesCut),total=Number(decision.octopusTentacleTotal);
        if(octopus.length!==1||octopus[0].status!=='fled'||decision.octopusRetreated!==true||
          !Number.isInteger(cut)||!Number.isInteger(total)||total<1||cut*2<total||
          !String(decision.octopusRetreatEvidence||'').trim())
          throw new Error('O Polvo Gigante só conclui o encontro após a fuga registrada à metade dos tentáculos cortados.');
      }
      if(officialId===41){
        const guards=encounter.combatants.filter(item=>item.side==='enemy'&&item.quickSheet?.officialR88SoulMerchantGuard===true);
        if(guards.length!==2||guards.map(item=>item.name).sort().join('|')!=='Aneta|Ginda'||guards.some(item=>item.maxStrength!==4||
          Number((item.quickSheet?.skills as Record<string,number>|undefined)?.Luta)!==2||!item.quickSheet?.rangedWeapon))
          throw new Error('O combate #41 exige Ginda e Aneta com as fichas de soldado típicas R88 p.183.');
      }
      if(officialId===42){
        const hunters=encounter.combatants.filter(item=>item.side==='enemy'&&item.quickSheet?.officialR88SaurianHunter===true);
        const bonus=Number(decision.saurianCountD6),partySize=Number(decision.saurianPartySize);
        if(!Number.isInteger(bonus)||bonus<1||bonus>6||!Number.isInteger(partySize)||hunters.length!==partySize+bonus||
          hunters.filter(item=>item.entityId==='official-r88-saurian-hunter:1').length!==1||
          hunters.some(item=>item.maxStrength!==4||item.armor!==4||Number((item.quickSheet?.skills as Record<string,number>|undefined)?.Luta)!==2))
          throw new Error('A composição #42 não corresponde ao grupo + D6 e aos perfis sáurios R88 p.166.');
      }
      if(officialId===43){
        const hunters=encounter.combatants.filter(item=>item.side==='enemy'&&item.quickSheet?.officialR88BountyHunter===true);
        if(hunters.length!==7||hunters.filter(item=>item.entityId==='official-r88-bounty-hunter:1').length!==1||
          hunters.some(item=>item.maxStrength!==2||!(item.armor===3||(item.quickSheet?.initialArmor===3&&item.armor>=0&&item.armor<=3))||Number((item.quickSheet?.skills as Record<string,number>|undefined)?.Patrulha)!==4))
          throw new Error('O combate #43 exige sete caçadores com o perfil R88 p.166.');
      }
      if(officialId===30){
        const teramalda=encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-teramalda');
        const sheet=teramalda[0]?.quickSheet as Record<string,unknown>|undefined;
        const attributes=sheet?.attributes as Record<string,{current?:number}>|undefined;
        const skills=sheet?.skills as Record<string,number>|undefined;
        if(teramalda.length!==1||sheet?.officialTeramalda!==true||sheet?.physicalDamageImmune!==true||
          teramalda[0].maxStrength!==12||attributes?.agility?.current!==4||attributes?.wits?.current!==3||attributes?.empathy?.current!==2||
          Number(skills?.Patrulha)!==2||(teramaldaKilled&&(teramalda[0].status!=='dead'||sheet?.spikeRemoved!==true)))
          throw new Error('O recibo não corresponde à ficha oficial e à condição de morte de Teramalda R88 p.160.');
        const visit=Number(event.result?.officialEncounterVisitNumber||1);
        const encounterStage=Number(decision.teramaldaEncounterStage||visit);
        const party=encounter.combatants.filter(item=>item.side==='party');
        const allFled=party.length>0&&party.every(item=>item.status==='fled');
        if(teramalda.some(item=>item.status==='active')&&!(OfficialEncounterRulesEngine.teramaldaFleeEndsPursuit(encounterStage)&&allFled))
          throw new Error('Teramalda continua ativa. Só a terceira etapa permite fuga sem perseguição; no primeiro encontro atacado, a R88 manda avançar direto para essa etapa.');
      }
      if(encounter.log.length<=2 && encounter.combatants.some(item=>item.side==='enemy'&&item.status==='active') &&
         encounter.combatants.some(item=>item.side==='party'&&item.status==='active'))
        throw new Error('O combate foi encerrado sem ação nem desfecho mecânico; resolva a oposição antes de liberar a jornada.');
      const now=Date.now();
      const partyStanding=encounter.combatants.filter(item=>item.side==='party'&&item.status==='active').length;
      const enemiesStanding=encounter.combatants.filter(item=>item.side==='enemy'&&item.status==='active').length;
      const doughDefeated=officialId===18&&encounter.combatants.some(item=>item.side==='enemy'&&item.entityId==='official-demonic-dough')&&
        encounter.combatants.filter(item=>item.side==='enemy'&&item.entityId==='official-demonic-dough')
          .every(item=>['broken','dead','yielded'].includes(item.status));
      // R84: um clique em Encerrar com log extenso não pode apagar uma
      // emboscada nem a resistência de Khardnal enquanto ambos lutam.
      if([2,3,4,5,7,8,10,11,12,17,18,20,21,23,24,25,29,33,35,41,42,43].includes(officialId)&&partyStanding>0&&enemiesStanding>0)
        throw new Error('Ainda há combatentes capazes de lutar dos dois lados; registre fuga, rendição ou desfecho real.');
      if([11,12,29].includes(officialId)&&
        (!encounter.log.some(row=>Boolean(row.combatantId))||encounter.log.length<2))
        throw new Error('Combate oficial sem ações mecânicas registradas.');
      if(officialId===8&&encounter.combatants.some(c=>c.entityId==='official-khardnal'&&['fled','yielded'].includes(c.status)))
        throw new Error('Khardnal luta até ser derrotado; fuga ou rendição não encerram esta ocorrência.');
      const sargahPending=officialId===3&&partyStanding>0&&enemiesStanding===0;
      const tombLootPending=officialId===11&&partyStanding>0&&enemiesStanding===0;
      const siteVisit=Number(event.result?.officialEncounterVisitNumber||1);
      const doughLootPending=officialId===18&&siteVisit<3&&partyStanding>0&&doughDefeated;
      const doughCaptivesPending=officialId===18&&siteVisit>=3&&partyStanding>0&&doughDefeated;
      const captiveCombatants=officialId===7&&enemiesStanding>0
        ?encounter.combatants.filter(c=>c.side==='party'&&['broken','yielded'].includes(c.status)):[];
      const captivesPending=captiveCombatants.length>0;
      // A captura só ocorre se houver escravagistas ainda capazes de agir.
      // Vencer os escravagistas não transforma aventureiros Quebrados em cativos.
      if(captivesPending){
        const charStore=tx.objectStore('characters');
        for(const actor of captiveCombatants){
          const id=String(actor.entityId||'');
          const character=await charStore.get(id) as Character|undefined;
          CampaignPartyService.assertMember(campaign,character);
          if(character.status==='dead')throw new Error('Uma pessoa morta não pode ser capturada.');
          const effectId=`official-slaver-captive:${eventId}`;
          if((character.statusEffects||[]).some(effect=>effect.id===effectId))
            throw new Error('Captura já registrada na ficha; não é possível repetir.');
          await charStore.put({...character,statusEffects:[...(character.statusEffects||[]),{
            id:effectId,label:'Cativo de Harwa',
            detail:'Capturado pelos escravagistas após combate real. Resgate/fuga ainda exige resolução; não foi vendido automaticamente.',
            source:'official-encounter:7'}]});
        }
      }
      const ghostBanished=officialId===21&&partyStanding>0&&enemiesStanding===0;
      const trollDefeated=[23,35].includes(officialId)&&partyStanding>0&&enemiesStanding===0;
      const ogreDefeated=[24,25].includes(officialId)&&partyStanding>0&&enemiesStanding===0;
      const candleRescuePending=officialId===25&&ogreDefeated;
      const ogrePrisonerPending=officialId===24&&ogreDefeated;
      const burialRescuePending=officialId===43&&partyStanding>0&&enemiesStanding===0;
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:(captivesPending||sargahPending||tombLootPending||doughLootPending||doughCaptivesPending||candleRescuePending||ogrePrisonerPending||burialRescuePending)?'open' as const:'resolved' as const,
        detail:burialRescuePending?`${item.detail} Os sete caçadores foram derrotados; a família continua no fosso. Registre o resgate e destino conforme adjudicação da Mestra.`:sargahPending?`${item.detail} A escolta foi vencida, mas Sargah ainda está amarrado. Escolha libertá-lo ou deixá-lo antes de continuar.`:tombLootPending?`${item.detail} O Cavaleiro da Morte foi derrotado. Os três objetos continuam na sepultura até uma personagem recolhê-los.`:doughLootPending?`${item.detail} A criatura de massa foi derrotada. O baú da casa ainda precisa ser encontrado e o diamante recolhido fisicamente.`:doughCaptivesPending?`${item.detail} As 200 massas foram derrotadas. Os halflings acorrentados que Melkar trouxe de Pernty ainda aguardam uma decisão explícita do grupo.`:captivesPending?`${item.detail} Os escravagistas sobreviventes capturaram aventureiros Quebrados ou rendidos. O cativeiro está salvo nas fichas; resgate e destino ainda precisam de resolução real.`:
          officialId===30&&partyStanding===0&&Number(item.result?.officialEncounterVisitNumber||1)===3?`${item.detail} Na terceira visita, o grupo fugiu; Teramalda não o perseguiu, conforme R88 p.160.`:
          ghostBanished?`${item.detail} Garmagol foi banido por um Quarto de Dia; a fonte diz que retorna e só Purgar Morto-vivo o destrói.`:trollDefeated?`${item.detail} Ghormald foi derrotada no combate; a ficha R88 prevê regeneração de 1 Força por rodada.`:candleRescuePending?`${item.detail} Gonlab foi derrotado. A libertação e o retorno do filho ainda precisam ser registrados antes das dez velas.`:ogrePrisonerPending?`${item.detail} Kurge foi derrotado; Oker continua sujeito a decisão explícita do grupo.`:ogreDefeated?`${item.detail} O ogro foi derrotado; resultado preservado no registro do combate.`:
          `${item.detail} O combate vinculado foi concluído; resultado preservado no registro do combate.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:burialRescuePending?'burial_rescue_pending':sargahPending?'sargah_release_pending':tombLootPending?'tomb_loot_pending':doughLootPending?'dough_loot_pending':doughCaptivesPending?'dough_captives_pending':captivesPending?'slaver_captives_pending':candleRescuePending?'candle_child_rescue_pending':ogrePrisonerPending?'oker_fate_pending':ghostBanished?'ghost_banished_return_pending':trollDefeated?'troll_defeated':ogreDefeated?'ogre_defeated':officialId===30&&partyStanding===0&&OfficialEncounterRulesEngine.teramaldaFleeEndsPursuit(Number(decision.teramaldaEncounterStage||item.result?.officialEncounterVisitNumber||1))?'teramalda_escaped_without_pursuit':'resolved_after_combat',
          pending:(captivesPending||sargahPending||tombLootPending||doughLootPending||doughCaptivesPending||candleRescuePending||ogrePrisonerPending||burialRescuePending)?'follow_up':null,
          combatResolvedAt:now,partyStanding,enemiesStanding,
          ...(burialRescuePending?{familyRescuePending:true,familyMemberCount:4,rescueRollInvented:false}:{}),
          ...(officialId===7?{slaversDoNotPursue:true,slaversCaptureIntent:true,
            partyFledIds:encounter.combatants.filter(c=>c.side==='party'&&c.status==='fled').map(c=>c.entityId||c.id),
            possibleCaptivesRequireAdjudication:captiveCombatants.map(c=>c.entityId||c.id),
            slaverCapturedCharacterIds:captiveCombatants.map(c=>c.entityId||c.id),
            slaverCaptivityRecordedAt:captivesPending?now:null,slaverSaleDestination:'not_assumed'}:{}),
          ...(ghostBanished?{ghostBanishedUntilQuarterDays:1,ghostDestroyed:false}:{}),
          ...(trollDefeated?{trollRegenerationRequiresAdjudication:true}:{}),
          ...(officialId===33?{minotaurEncounterSupportedSide:decision.minotaurSupportedSide,minotaurFightConcluded:true}:{}),
          ...(officialId===35?{kargRegenerationRequiresAdjudication:trollDefeated}:{} )},
          ...((captivesPending||sargahPending||tombLootPending||doughLootPending||doughCaptivesPending||candleRescuePending||ogrePrisonerPending||burialRescuePending)?{}:{resolvedAt:now,adjudicationMode:'official_combat_verified'})}
      }:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });
    this.notify(outcome);return outcome;
  }

  /** R88 #18: recolher o diamante real do baú depois da vitória e busca explícita.
   * O valor de 1 ouro vem do livro; a ação não concede moeda nem presume que o baú foi encontrado.
   */
  static async claimOfficialDemonicDoughDiamond(campaignId:string,eventId:string,recipientId:string):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      const siteKey=event?.hexId?`${event.hexId}:official-encounter:18`:'';
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==18||
        Number(event.result?.officialEncounterVisitNumber||1)>=3||decision?.stage!=='dough_loot_pending'||decision.doughDiamondClaimedAt||
        world.officialEncounterSiteFlags?.[siteKey]?.doughDiamondClaimedAt||!combat||
        combat.id!==decision.combatEncounterId||combat.startedSceneId!==eventId||combat.hexId!==event.hexId||
        event.hexId!==campaign.currentHexId||combat.status!=='resolved'||!combat.resolvedAt||
        !combat.combatants.some(item=>item.entityId==='official-demonic-dough'&&item.side==='enemy'&&['broken','dead'].includes(item.status))||
        combat.combatants.some(item=>item.side==='enemy'&&item.status==='active')||
        !combat.combatants.some(item=>item.side==='party'&&item.status==='active'))
        throw new Error('O diamante exige vitória real sobre a massa demoníaca no hex da casa.');
      const characters=tx.objectStore('characters');const recipient=await characters.get(recipientId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,recipient);CharacterActionRulesEngine.assertCanAct(recipient);
      const id=`official-dough-diamond:${eventId}`;
      if([...(recipient.gear||[]),...(recipient.weapons||[]),...(recipient.armor||[])].some(item=>String((item as Record<string,unknown>).id)===id))
        throw new Error('O diamante já consta no inventário.');
      await characters.put({...recipient,gear:[...(recipient.gear||[]),{
        id,name:'Diamante do baú de Melkar',type:'treasure',valueGold:1,sourceEventId:eventId,sourcePrintedPage:155,
      }]});
      const now=Date.now();const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,
        detail:`${item.detail} ${recipient.name} encontrou no baú e recolheu o diamante de valor 1 ouro.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'dough_diamond_claimed',pending:null,doughDiamondClaimedAt:now,
          doughDiamondRecipientId:recipientId,doughDiamondItemId:id},resolvedAt:now,
        adjudicationMode:'official_demonic_dough_victory_and_physical_loot'}}:item);
      const updated={...world,officialEncounterSiteFlags:{...(world.officialEncounterSiteFlags||{}),[siteKey]:{
        ...(world.officialEncounterSiteFlags?.[siteKey]||{}),doughDiamondClaimedAt:now,doughDiamondEventId:eventId}},
        journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(outcome);return outcome;
  }

  /** R88 #18: registra a escolha sobre os halflings depois da vitória em Pernty.
   * Não cria contagem de habitantes, itens, XP ou testes que a ocorrência não especifica.
   */
  static async resolveOfficialDoughCaptives(campaignId:string,eventId:string,choice:'release'|'leave'):Promise<WorldState>{
    if(!['release','leave'].includes(choice))throw new Error('Escolha inválida para os halflings de Pernty.');
    const updated=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==18||
        decision?.stage!=='dough_captives_pending'||Number(event.result?.officialEncounterVisitNumber||1)<3||
        !combat||combat.id!==decision.combatEncounterId||combat.startedSceneId!==eventId||combat.status!=='resolved'||
        !combat.combatants.some(item=>item.entityId==='official-demonic-dough'&&item.side==='enemy'&&['broken','dead','yielded'].includes(item.status))||
        combat.combatants.some(item=>item.entityId==='official-demonic-dough'&&item.side==='enemy'&&item.status==='active')||
        !combat.combatants.some(item=>item.side==='party'&&item.status==='active')||event.hexId!==campaign.currentHexId)
        throw new Error('A decisão dos cativos exige a vitória real sobre as 200 massas do retorno de Melkar.');
      const now=Date.now();const stage=choice==='release'?'dough_captives_released':'dough_captives_left';
      const action=choice==='release'?'O grupo libertou os halflings acorrentados trazidos de Pernty.':'O grupo deixou os halflings acorrentados no local.';
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,
        detail:`${item.detail} ${action}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage,pending:null,doughCaptivesChoice:choice,
          doughCaptivesResolvedAt:now,adjudicationMode:'official_encounter_explicit_choice'},resolvedAt:now,
          adjudicationMode:'official_dough_captives_explicit_choice'}}:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return next;
    });this.notify(updated);return updated;
  }

  /** R87 #11: saque físico após vitória no combate vinculado. Sem prêmio ao fugir.
   * O colar permanece objeto com valor D6+D6 em pratas, não moedas mágicas.
   */
  static async claimOfficialForgottenPrinceTreasure(campaignId:string,eventId:string,recipientId:string,
    rng:()=>number=Math.random):Promise<{world:WorldState;silverValue:number}>{
    const die=()=>{const v=rng();if(!Number.isFinite(v)||v<0||v>=1)throw new Error('Dado inválido.');return 1+Math.floor(v*6)};
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==11||
        decision?.stage!=='tomb_loot_pending'||decision.tombLootClaimedAt||!combat||
        combat.id!==decision.combatEncounterId||combat.startedSceneId!==eventId||combat.hexId!==event.hexId||
        event.hexId!==campaign.currentHexId||combat.status!=='resolved'||!combat.resolvedAt||
        !combat.combatants.some(c=>c.entityId==='core-bestiary:death-knight'&&c.side==='enemy'&&['broken','dead'].includes(c.status))||
        combat.combatants.some(c=>c.side==='enemy'&&c.status==='active')||
        !combat.combatants.some(c=>c.side==='party'&&c.status==='active'))
        throw new Error('O saque exige vitória real sobre o Cavaleiro da Morte no hex do túmulo.');
      const cs=tx.objectStore('characters');const recipient=await cs.get(recipientId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,recipient);CharacterActionRulesEngine.assertCanAct(recipient);
      const silverValue=die()+die();const now=Date.now();
      const ids=['necklace','sword','mail'].map(k=>`official-prince:${eventId}:${k}`);
      if([...(recipient.gear||[]),...(recipient.weapons||[]),...(recipient.armor||[])].some((item:any)=>ids.includes(item.id)))
        throw new Error('Itens do túmulo já registrados no inventário.');
      const gear=[...(recipient.gear||[]),{id:ids[0],name:'Colar de prata do príncipe esquecido',type:'treasure',valueSilver:silverValue,sourceEventId:eventId}];
      const weapons=[...(recipient.weapons||[]),{id:ids[1],name:'Espada larga velha',type:'weapon',sourceEventId:eventId}];
      const armor=[...(recipient.armor||[]),{id:ids[2],name:'Cota de malha enferrujada',type:'armor',sourceEventId:eventId}];
      const looter={...recipient,gear,weapons,armor};
      await cs.put({...looter,encumbrance:EncumbranceRulesEngine.calculate(looter)});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,status:'resolved' as const,
        detail:`${e.detail} ${recipient.name} recolheu o colar de valor ${silverValue} pratas, a espada larga velha e a cota de malha enferrujada.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'tomb_loot_claimed',pending:null,tombLootClaimedAt:now,
          tombLootRecipientId:recipientId,tombNecklaceSilverValue:silverValue,tombLootItemIds:ids},
          resolvedAt:now,adjudicationMode:'official_death_knight_victory_and_physical_loot'}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return {world:updated,silverValue};
    });this.notify(outcome.world);return outcome;
  }

  /** R87 #7: um herói LIVRE tenta resgatar cativos; não cria aliados fictícios,
   * não apaga cativeiro e só utiliza escravagistas sobreviventes do combate original.
   */
  static async startOfficialSlaverRescue(campaignId:string,eventId:string):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Há outro combate ativo.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const old=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==7||
        decision?.stage!=='slaver_captives_pending'||!old||old.status!=='resolved'||
        old.hexId!==event.hexId||event.hexId!==campaign.currentHexId||
        ![decision.combatEncounterId,decision.slaverRescueEncounterId].includes(old.id)||
        !Array.isArray(decision.slaverCapturedCharacterIds)||!decision.slaverCapturedCharacterIds.length)
        throw new Error('Não há cativeiro de Harwa conciliado neste hex.');
      const all=await tx.objectStore('characters').getAll() as Character[];
      const members=CampaignPartyService.members(campaign,all).filter(c=>c.status!=='dead');
      const capturedIds=new Set((decision.slaverCapturedCharacterIds as string[]).map(String));
      for(const id of capturedIds){
        const captive=members.find(c=>c.id===id);
        if(!captive||!(captive.statusEffects||[]).some(e=>e.id===`official-slaver-captive:${eventId}`))
          throw new Error('O recibo de cativeiro não corresponde às fichas da campanha.');
      }
      const rescuers=members.filter(c=>!capturedIds.has(c.id)&&CharacterActionRulesEngine.canAct(c));
      if(!rescuers.length)throw new Error('Nenhum herói livre e apto está no hex para iniciar o resgate. Requer ajuda externa real.');
      const survivors=old.combatants.filter(c=>c.side==='enemy'&&c.entityId?.startsWith('official-slaver:')&&c.status==='active');
      if(!survivors.length)throw new Error('Nenhum escravagista ativo permanece para impedir a libertação.');
      const combat=EncounterService.createEncounter('Resgate dos cativos de Harwa',event.hexId,rescuers,
        survivors.map(c=>({...c,id:`rescue:${crypto.randomUUID()}`,initiative:1+Math.floor(Math.random()*10),
          fastActionAvailable:true,slowActionAvailable:true,status:'active' as const})),event.id);
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'slaver_rescue_combat_active',
          slaverRescueEncounterId:combat.id,slaverRescueStartedAt:now,slaverRescuers:rescuers.map(c=>c.id),
          slaverRescueEnemyIds:survivors.map(c=>c.entityId)}}}:e);
      const updated={...world,encounter:combat,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(outcome);return outcome;
  }

  /** A fuga do resgatador não liberta ninguém. Apenas vitória REAL remove
   * statusEffects de cativeiro, dentro da mesma transação que resolve a cena.
   */
  static async completeOfficialSlaverRescue(campaignId:string,eventId:string):Promise<WorldState>{
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==7||
        decision?.stage!=='slaver_rescue_combat_active'||!combat||combat.id!==decision.slaverRescueEncounterId||
        combat.startedSceneId!==eventId||combat.hexId!==event.hexId||event.hexId!==campaign.currentHexId||
        combat.status!=='resolved'||!combat.resolvedAt||combat.resolvedAt<Number(decision.slaverRescueStartedAt||0))
        throw new Error('Resgate sem combate real concluído e vinculado ao cativeiro.');
      if(combat.log.length<2||!combat.log.some(row=>Boolean(row.combatantId)))
        throw new Error('Resgate sem ações mecânicas registradas no combate.');
      const enemyIds=(decision.slaverRescueEnemyIds as string[]||[]).map(String);
      if(!enemyIds.length||combat.combatants.filter(c=>c.side==='enemy').length!==enemyIds.length||
        !enemyIds.every(id=>combat.combatants.some(c=>c.entityId===id&&c.side==='enemy')))
        throw new Error('A oposição do resgate não corresponde aos escravagistas sobreviventes.');
      const enemiesStanding=combat.combatants.filter(c=>c.side==='enemy'&&c.status==='active');
      const rescuersStanding=combat.combatants.filter(c=>c.side==='party'&&c.status==='active');
      if(enemiesStanding.length&&rescuersStanding.length)throw new Error('Combate ainda sem desfecho real entre os lados.');
      const rescued=enemiesStanding.length===0&&rescuersStanding.length>0&&
        combat.combatants.filter(c=>c.side==='enemy').every(c=>['broken','dead','yielded'].includes(c.status));
      const cs=tx.objectStore('characters');const captiveIds=(decision.slaverCapturedCharacterIds as string[]||[]).map(String);
      if(rescued){
        for(const id of captiveIds){const char=await cs.get(id) as Character|undefined;
          CampaignPartyService.assertMember(campaign,char);
          if(!(char.statusEffects||[]).some(e=>e.id===`official-slaver-captive:${eventId}`))
            throw new Error('Ficha cativa sem efeito correspondente; resgate abortado.');
          await cs.put({...char,statusEffects:(char.statusEffects||[]).filter(e=>e.id!==`official-slaver-captive:${eventId}`)});
        }
      }
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,
        status:rescued?'resolved' as const:'open' as const,
        detail:rescued?`${e.detail} Resgatadores venceram os escravagistas sobreviventes; ${captiveIds.length} cativo(s) libertado(s).`:
          `${e.detail} O resgate falhou ou os resgatadores fugiram; o cativeiro permanece.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,
          stage:rescued?'slaver_rescue_completed':'slaver_captives_pending',
          slaverRescueResolvedAt:now,slaverRescueSucceeded:rescued,
          slaverRescueRemainingEnemyIds:enemiesStanding.map(c=>c.entityId)},
          ...(rescued?{resolvedAt:now,adjudicationMode:'official_captive_rescue_via_combat'}:{})}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(outcome);return outcome;
  }

  /** R85 #3: libertação de Sargah só após combate comprovado e escolta neutralizada.
   * Não torna Sargah companheiro nem resolve seu destino fora de cena por decreto.
   */
  static async resolveOfficialSargah(campaignId:string,eventId:string,choice:'release'|'leave_bound'):Promise<WorldState>{
    if(!['release','leave_bound'].includes(choice))throw new Error('Decisão sobre Sargah inválida.');
    const result=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      const combat=world.encounter;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==3||
        decision?.stage!=='sargah_release_pending'||!combat||combat.id!==decision.combatEncounterId||
        combat.startedSceneId!==eventId||combat.status!=='resolved'||combat.hexId!==event.hexId||
        event.hexId!==campaign.currentHexId||!combat.resolvedAt||
        combat.combatants.some(c=>c.side==='enemy'&&c.status==='active')||
        !combat.combatants.some(c=>c.entityId==='official-sargah'&&c.side==='neutral'))
        throw new Error('O resgate de Sargah não tem recibo de combate e escolta vencida válidos.');
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,status:'resolved' as const,
        detail:`${e.detail} ${choice==='release'?'Sargah foi desamarrado pelo grupo.':'O grupo decidiu deixar Sargah amarrado.'}`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,stage:'sargah_fate_recorded',pending:null,sargahFate:choice,
          sargahFateAt:now,automaticCompanionRecruitment:false},resolvedAt:now,
          adjudicationMode:'official_roka_combat_and_explicit_prisoner_decision'}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return next;
    });this.notify(result);return result;
  }

  /** R80 #6: Adestramento real (Empatia + perícia), dado gravado uma única vez.
   * O sucesso acalma o cavalo, mas não transfere sua posse por si só.
   */
  static async calmOfficialHorse(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<{world:WorldState;roll:import('../types').DiceRoll;calmed:boolean}>{
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede tentar acalmar o cavalo.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||mechanic?.kind!=='official_random_encounter'||Number(mechanic.encounterId)!==6||
        decision?.stage!=='pending_external_mechanics'||decision.choiceId!=='calm')
        throw new Error('Não existe tentativa de Adestramento pendente neste encontro.');
      if(event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex do cavalo.');
      const charStore=tx.objectStore('characters');const character=await charStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);
      CharacterActionRulesEngine.assertCanAct(character);
      const skill=normalizedSkill(character,['Animal Handling','Adestramento']);
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.empathy.current,skill,gear:0},{
        characterId:character.id,sceneId:event.id,checkId:`official-horse:${event.id}:${character.id}`,
        rng,criticalInjuries:character.criticalInjuryStates,skillName:'animal_handling',attributeName:'empathy',skillRankAtRoll:skill});
      const critical=CriticalInjuryRulesEngine.applyCheckConsequences(character,'animalHandling');
      if(critical.triggers.length)await charStore.put(critical.character);
      await tx.objectStore('rolls').put(roll);
      const calmed=roll.successes>0;const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,detail:`${item.detail} ${character.name} rolou Adestramento: ${roll.successes} sucesso(s). ${calmed?'O cavalo foi acalmado.':'O cavalo permanece arisco.'}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'horse_test_recorded',
          automaticRollsApplied:true,horseCalmed:calmed,horseRollId:roll.id,horseHandlerId:character.id,horseRolledAt:now}}
      }:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return {world:updated,roll,calmed};
    });
    this.notify(outcome.world);return outcome;
  }

  /** R84 #6: o brasão da casa de Lavide é um item físico com proveniência.
   * O VALIOSO do alforje e o PRECIOSO da família exigem tabela oficial,
   * portanto ficam pendentes, jamais substituídos por recompensa fictícia.
   */
  static async retrieveOfficialHorseInsignia(campaignId:string,eventId:string,characterId:string):Promise<WorldState>{
    const updated=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede vasculhar os alforjes.');
      const event=world.journey.events.find(e=>e.id===eventId&&(e.status==='open'||e.status==='resolved'));
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==6||
        !((event.status==='open'&&['horse_test_recorded','horse_capture_recorded'].includes(String(decision?.stage)))||
          (event.status==='resolved'&&decision?.horseDisposition==='claim'))||
        (decision.horseCalmed!==true&&decision.horseCaptureConfirmed!==true)||decision.horseInsigniaRecoveredAt)
        throw new Error('É necessário acalmar o cavalo e ainda não ter retirado o símbolo.');
      if(event.hexId!==campaign.currentHexId)throw new Error('O grupo saiu do hex do cavalo.');
      const charStore=tx.objectStore('characters');const character=await charStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      const item={id:`official-horse-crest:${eventId}`,name:'Insígnia da casa de Lavide (rosa)',
        type:'quest',encumbranceClass:'small',sourceEventId:eventId,
        description:'Símbolo de rosa encontrado nas bolsas do cavalo de Lavide; pertence à família do nobre.',
        isUnique:true,hasAssignedSaleValue:false};
      if((character.gear||[]).some(g=>g?.id===item.id))throw new Error('Símbolo já está com esta personagem.');
      const now=Date.now();
      const crestHolder={...character,gear:[...(character.gear||[]),item]};
      await charStore.put({...crestHolder,encumbrance:EncumbranceRulesEngine.calculate(crestHolder)});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} ${character.name} guardou a insígnia da casa de Lavide. O conteúdo VALIOSO ainda requer determinação pela tabela oficial.`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,horseInsigniaRecoveredAt:now,
          horseInsigniaHolderId:character.id,horseValuableLoot:'pending_official_table',
          horseFamilyReward:'pending_actual_return_and_precious_table'}}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return next;
    });this.notify(updated);return updated;
  }

  /** R85 #6: sorteio oficial do item VALIOSO CARREGADO (Guia do Mestre p.189).
   * Moedas são creditadas na ficha real; objetos entram no inventário real.
   * Idempotência por evento e recibo atômico impede duplicar o tesouro.
   */
  static async recoverOfficialHorseValuable(campaignId:string,eventId:string,characterId:string,
    rng:()=>number=Math.random):Promise<{world:WorldState;treasure:import('./OfficialEncounterTreasureEngine').OfficialTreasureResult}>{
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede vasculhar o alforje.');
      const event=world.journey.events.find(e=>e.id===eventId&&(e.status==='open'||e.status==='resolved'));
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==6||
        !((event.status==='open'&&['horse_test_recorded','horse_capture_recorded'].includes(String(decision?.stage)))||
          (event.status==='resolved'&&decision?.horseDisposition==='claim'))||(decision.horseCalmed!==true&&decision.horseCaptureConfirmed!==true)||
        !decision.horseInsigniaRecoveredAt||decision.horseValuableRecoveredAt)
        throw new Error('O item VALIOSO só pode ser recolhido uma vez após acalmar o cavalo e identificar Lavide.');
      if(event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex do cavalo.');
      const charStore=tx.objectStore('characters');const character=await charStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      const treasure=OfficialEncounterTreasureEngine.rollCarried('valuable',rng);
      const now=Date.now();
      const updatedCharacter=treasure.coins?{...character,coins:{...character.coins,
        [treasure.coins.coin]:(character.coins?.[treasure.coins.coin]||0)+treasure.coins.quantity}}:{
        ...character,gear:[...(character.gear||[]),{
          id:`official-horse-valuable:${eventId}`,name:treasure.name,type:'treasure',
          encumbranceClass:treasure.weight||'normal',sourceEventId:eventId,
          valueRoll:treasure.value,treasureD66:treasure.roll,
          sourcePrintedPage:189,isUnique:true}]};
      await charStore.put({...updatedCharacter,encumbrance:EncumbranceRulesEngine.calculate(updatedCharacter)});
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} ${character.name} recolheu do alforje ${treasure.name} (D66 ${treasure.roll}).`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,
          horseValuableLoot:'recovered_from_official_table',horseValuableRecoveredAt:now,
          horseValuableRecipientId:character.id,horseValuableRoll:treasure.roll,
          horseValuableItemName:treasure.name,horseValuableCoins:treasure.coins,
          horseValuableValue:treasure.value}}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return {world:next,treasure};
    });this.notify(outcome.world);return outcome;
  }

  /** R88 #16: depois de a Mestra confirmar que a companhia obteve acesso ao
   * balão, rola os dois itens de covil descritos na p. 154 usando as tabelas
   * oficiais das pp. 188–190. Os resultados são transferidos uma só vez para
   * a ficha real; nenhum método presume que a tentativa de roubo teve sucesso.
   */
  static async recoverOfficialBalloonLoot(campaignId:string,eventId:string,characterId:string,
    accessAdjudicationInput:string,rng:()=>number=Math.random):Promise<{world:WorldState;simple:import('./OfficialEncounterTreasureEngine').OfficialLairTreasureResult;valuable:import('./OfficialEncounterTreasureEngine').OfficialLairTreasureResult}>{
    const adjudication=String(accessAdjudicationInput||'').trim();
    if(adjudication.length<12||adjudication.length>400)throw new Error('Registre como a Mestra concedeu acesso ao balão (12–400 caracteres).');
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede vasculhar o balão.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||mechanic?.kind!=='official_random_encounter'||Number(mechanic.encounterId)!==16||
        decision?.choiceId!=='rob'||decision.stage!=='pending_external_mechanics'||decision.balloonLootRecoveredAt)
        throw new Error('O acesso ao balão do encontro #16 não está autorizado ou o saque já foi recolhido.');
      if(!event.hexId||event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex do balão.');
      const characters=tx.objectStore('characters');const character=await characters.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);CharacterActionRulesEngine.assertCanAct(character);
      const simple=OfficialEncounterTreasureEngine.rollLair('simple',rng);
      const valuable=OfficialEncounterTreasureEngine.rollLair('valuable',rng);
      const now=Date.now();
      let nextCharacter:Character={...character};
      const inventory=[...(nextCharacter.gear||[])];
      for(const [kind,loot] of [['simple',simple],['valuable',valuable]] as const){
        if(loot.coins){nextCharacter={...nextCharacter,coins:{...nextCharacter.coins,
          [loot.coins.coin]:(nextCharacter.coins?.[loot.coins.coin]||0)+loot.coins.quantity}};}
        else inventory.push({id:`official-balloon-loot:${eventId}:${kind}`,name:loot.name,type:'treasure',quantity:1,
          encumbranceClass:loot.weight||'normal',...(loot.weightUnits!==null?{sourceWeightUnits:loot.weightUnits}:{}),
          sourceEventId:eventId,valueRoll:loot.value,treasureD66:loot.roll,treasureCategory:kind,
          sourcePrintedPage:loot.sourcePrintedPage,isUnique:true});
      }
      if(!simple.coins||!valuable.coins)nextCharacter={...nextCharacter,gear:inventory};
      await characters.put(nextCharacter);
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,detail:`${item.detail} A Mestra registrou acesso ao balão; foram encontrados ${simple.name} (D66 ${simple.roll}) e ${valuable.name} (D66 ${valuable.roll}).`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'balloon_loot_recovered',pending:'andred_response',
          balloonLootAccessAdjudication:adjudication,balloonLootRecoveredAt:now,balloonLootRecipientId:character.id,
          balloonLootSimple:{...simple},balloonLootValuable:{...valuable},automaticResourcesChanged:true}}}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return{world:updated,simple,valuable};
    });this.notify(outcome.world);return outcome;
  }

  /** R88 #6: a captura à força não tem teste definido na entrada.
   * A Mestra registra a contenção real; isso não equivale a acalmar o animal.
   */
  static async recordOfficialHorseCapture(campaignId:string,eventId:string,handlerId:string,evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Descreva como o cavalo foi contido pela mesa (mínimo 12 caracteres).');
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==6||
        d?.choiceId!=='capture'||d.stage!=='pending_external_mechanics'||world.encounter?.status==='active'||event.hexId!==campaign.currentHexId)
        throw new Error('A captura não está pendente neste hex.');
      const handler=await tx.objectStore('characters').get(handlerId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,handler);CharacterActionRulesEngine.assertCanAct(handler);
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} A Mestra confirmou a contenção do cavalo; o animal não foi considerado acalmado.`,
        result:{...(e.result||{}),officialSceneDecision:{...d,stage:'horse_capture_recorded',pending:'follow_up',
          horseCaptureConfirmed:true,horseCaptureEvidence:evidence,horseCaptureRecordedAt:now,
          horseCalmed:false,horseHandlerId:handlerId,automaticRollsApplied:false}}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(outcome);return outcome;
  }

  /** #16 p.154: two separate evidence fields avoid claiming automatic repair
   * or healing. The entry publishes no NPC attributes or treatment/repair test.
   */
  static async recordOfficialBalloonAssistance(campaignId:string,eventId:string,repairInput:string,careInput:string):Promise<WorldState>{
    const repair=String(repairInput||'').trim().slice(0,600),care=String(careInput||'').trim().slice(0,600);
    if(repair.length<12||care.length<12)throw new Error('Registre separadamente o reparo e o atendimento da perna (mínimo 12 caracteres cada).');
    const outcome=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==16||
        d?.choiceId!=='help'||d.stage!=='pending_external_mechanics'||event.hexId!==campaign.currentHexId||world.encounter?.status==='active')
        throw new Error('A assistência ao balão não está pendente neste hex.');
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,status:'resolved' as const,
        detail:`${e.detail} Reparo e atendimento da perna registrados pela Mestra; sem presumir cura instantânea, tesouro ou atributos do piloto.`,
        result:{...(e.result||{}),resolvedAt:now,adjudicationMode:'official_balloon_repair_and_care_confirmed',
          officialSceneDecision:{...d,stage:'balloon_assistance_recorded',pending:null,balloonRepairEvidence:repair,
          andredLegCareEvidence:care,andredLegAutomaticallyHealed:false,automaticResourcesChanged:false,automaticRollsApplied:false,resolvedAt:now}}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(outcome);return outcome;
  }

  /** #16: after actual loot transfer, record the explicit noncombat response.
   * Cannot finish an active battle or skip access/treasure resolution.
   */
  static async recordOfficialBalloonPilotResponse(campaignId:string,eventId:string,evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim().slice(0,600);
    if(evidence.length<12)throw new Error('Registre por que Andred não abriu fogo e como o grupo deixou a cena (mínimo 12 caracteres).');
    const outcome=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='open');
      const d=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==16||
        d?.choiceId!=='rob'||d.stage!=='balloon_loot_recovered'||!d.balloonLootRecoveredAt||
        world.encounter?.status==='active'||event.hexId!==campaign.currentHexId)
        throw new Error('A resposta do piloto exige o saque real já transferido e nenhum combate ativo.');
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,status:'resolved' as const,
        detail:`${e.detail} Andred não abriu fogo; resposta e afastamento confirmados pela Mestra.`,
        result:{...(e.result||{}),resolvedAt:now,adjudicationMode:'official_balloon_loot_and_explicit_pilot_response',
          officialSceneDecision:{...d,stage:'balloon_pilot_response_recorded',pending:null,andredDidNotOpenFire:true,
          andredResponseEvidence:evidence,resolvedAt:now}}}:e);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(next);return next;
    });this.notify(outcome);return outcome;
  }

  /** O cavalo só entra na jornada mediante escolha explícita DEPOIS do teste.
   * Cavalo treinado para combate usa o perfil publicado Cavalo de Guerra.
   */
  static async finishOfficialHorse(campaignId:string,eventId:string,action:'leave'|'claim'):Promise<WorldState>{
    if(!['leave','claim'].includes(action))throw new Error('Destino do cavalo inválido.');
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de decidir o destino do cavalo.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==6||!['horse_test_recorded','horse_capture_recorded'].includes(String(decision?.stage)))
        throw new Error('O teste de Adestramento não foi realizado.');
      if(event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex do cavalo.');
      let mounts=world.journey.mounts||[];let mountId:string|undefined;
      if(action==='claim'){
        if(decision.horseCalmed!==true&&decision.horseCaptureConfirmed!==true)throw new Error('O cavalo ainda está arisco; não pode ser reivindicado por este fluxo.');
        const character=await tx.objectStore('characters').get(String(decision.horseHandlerId)) as Character|undefined;
        CampaignPartyService.assertMember(campaign,character);
        CharacterActionRulesEngine.assertCanAct(character);
        if(mounts.some(mount=>mount.riderCharacterId===character.id))throw new Error('O condutor já possui montaria registrada.');
        const profile=JourneyRulesEngine.officialWildAnimal('warhorse');
        if(!profile)throw new Error('Perfil oficial do cavalo de guerra ausente.');
        mountId=crypto.randomUUID();
        mounts=[...mounts,{id:mountId,name:'Garanhão de Lavide',riderCharacterId:character.id,
          strength:profile.strength,status:'ready' as const,sourceEventId:eventId,ownership:'custody' as const,createdAt:Date.now()}];
      }
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:'resolved' as const,
        detail:`${item.detail} ${action==='claim'?'O grupo decidiu conduzir o cavalo, ainda pertencente à família Lavide.':'O grupo deixou o cavalo no local.'}`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'horse_disposition_resolved',pending:null,
          horseDisposition:action,horseMountId:mountId||null,automaticResourcesChanged:action==='claim',horseDispositionAt:now},
          resolvedAt:now,adjudicationMode:'official_horse_skill_and_disposition'}
      }:item);
      const updated={...world,journey:{...world.journey,mounts,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });
    this.notify(outcome);return outcome;
  }

  /** R86 #6 — a MdJ associa a família de Lavide a um castelo REAL do atlas.
   * O livro não fornece coordenadas. Nunca cria um castelo fictício nem presume
   * que qualquer fortaleza pertença à família; a associação é explícita.
   */
  static async assignOfficialHorseFamilyCastle(campaignId:string,eventId:string,poiId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state','individual_hexes'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(e=>e.id===eventId);
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==6||
        event.status!=='resolved'||decision?.horseDisposition!=='claim'||!decision.horseMountId||
        !decision.horseInsigniaRecoveredAt||decision.horseFamilyRewardAt||decision.horseFamilyCastlePoiId)
        throw new Error('A associação exige cavalo levado, insígnia recuperada e recompensa ainda pendente.');
      const poi=world.campaignPois.find(p=>p.id===poiId&&p.campaignId===campaignId);
      if(!poi||!poi.discovered||poi.hidden||!poi.hexId||!poi.mapId||
        !/(castle|castelo|fortress|fortaleza|stronghold)/i.test([poi.type,...poi.tags].join(' '))||
        ['destroyed','ruined','abandoned'].includes(poi.status))
        throw new Error('Selecione um castelo/fortaleza existente, descoberto e habitável no atlas da campanha.');
      const hexStore=tx.objectStore('individual_hexes');
      const originHex=await hexStore.get(event.hexId);
      const castleHex=await hexStore.get(poi.hexId);
      if(!originHex||!castleHex||originHex.mapId!==poi.mapId||castleHex.mapId!==poi.mapId)
        throw new Error('O castelo e o local do encontro precisam existir no mesmo mapa real.');
      if(!world.journey.mounts?.some(m=>m.id===decision.horseMountId))
        throw new Error('O cavalo da ocorrência não consta como montaria da campanha.');
      const now=Date.now();const events=world.journey.events.map(e=>e.id===eventId?{...e,
        result:{...(e.result||{}),officialSceneDecision:{...decision,
          horseFamilyCastlePoiId:poi.id,horseFamilyCastleMapId:poi.mapId,
          horseFamilyCastleAssignedAt:now,horseFamilyCastleAssignment:'gm_explicit'}}}:e);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R86 #6 — entrega REAL: presença no castelo escolhido, cavalo montado
   * presente e insígnia física no inventário. O D66 PRECIOSO só é sorteado
   * DEPOIS da entrega. O resultado 66 fica pendente de seleção de artefato.
   */
  static async returnOfficialHorseToFamily(campaignId:string,eventId:string,recipientId:string,
    rng:()=>number=Math.random):Promise<{world:WorldState;treasure:import('./OfficialEncounterTreasureEngine').OfficialTreasureResult}>{
    const result=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha inexistente.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo impede entregar o cavalo à família.');
      const event=world.journey.events.find(e=>e.id===eventId&&e.status==='resolved');
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)!==6||
        decision?.horseDisposition!=='claim'||!decision.horseMountId||!decision.horseFamilyCastlePoiId||
        decision.horseFamilyRewardAt||decision.horseFamilyArtifactPending)
        throw new Error('Entrega inválida, já realizada ou castelo da família ainda não associado.');
      const poi=world.campaignPois.find(p=>p.id===decision.horseFamilyCastlePoiId&&p.campaignId===campaignId);
      if(!poi||poi.hidden||!poi.discovered||poi.mapId!==decision.horseFamilyCastleMapId||
        ['destroyed','ruined','abandoned'].includes(poi.status)||!poi.hexId||
        campaign.currentHexId!==poi.hexId)
        throw new Error('O grupo precisa estar fisicamente no castelo associado à família de Lavide.');
      const mount=(world.journey.mounts||[]).find(m=>m.id===decision.horseMountId);
      if(!mount||mount.status!=='ready')throw new Error('O cavalo original deve estar presente e apto para devolução.');
      const charStore=tx.objectStore('characters');const all=await charStore.getAll() as Character[];
      const party=CampaignPartyService.members(campaign,all);
      const rider=party.find(c=>c.id===mount.riderCharacterId);
      if(!rider||rider.status==='dead')throw new Error('O cavalo não está com um condutor vivo do grupo.');
      const crestId=`official-horse-crest:${eventId}`;
      const holder=party.find(c=>(c.gear||[]).some(g=>g?.id===crestId));
      if(!holder||holder.status==='dead')throw new Error('A insígnia física da casa Lavide deve estar no inventário de um membro do grupo.');
      const recipient=party.find(c=>c.id===recipientId);
      if(!recipient)throw new Error('A recompensa precisa ser atribuída a um membro da campanha.');
      CharacterActionRulesEngine.assertCanAct(recipient);
      const treasure=OfficialEncounterTreasureEngine.rollCarried('precious',rng);
      const now=Date.now();
      const crestRemoved={...holder,gear:(holder.gear||[]).filter(g=>g?.id!==crestId)};
      await charStore.put({...crestRemoved,encumbrance:EncumbranceRulesEngine.calculate(crestRemoved)});
      if(!treasure.requiresArtifactSelection){
        const current=recipient.id===holder.id?crestRemoved:recipient;
        const rewarded=treasure.coins?{...current,coins:{...current.coins,
          [treasure.coins.coin]:(current.coins?.[treasure.coins.coin]||0)+treasure.coins.quantity}}:
          {...current,gear:[...(current.gear||[]),{
            id:`official-horse-precious:${eventId}`,name:treasure.name,type:'treasure',
            encumbranceClass:treasure.weight||'normal',sourceEventId:eventId,
            valueRoll:treasure.value,treasureD66:treasure.roll,sourcePrintedPage:191,isUnique:true}]};
        await charStore.put({...rewarded,encumbrance:EncumbranceRulesEngine.calculate(rewarded)});
      }
      const events=world.journey.events.map(e=>e.id===eventId?{...e,
        detail:`${e.detail} O cavalo e a insígnia foram apresentados no castelo da família Lavide. ${treasure.requiresArtifactSelection?'D66 66: artefato ainda exige escolha oficial.':`A família concedeu ${treasure.name}.`}`,
        result:{...(e.result||{}),officialSceneDecision:{...decision,
          stage:treasure.requiresArtifactSelection?'horse_artifact_pending':'horse_family_rewarded',
          horseFamilyRewardAt:now,horseFamilyRewardRecipientId:recipient.id,
          horseFamilyCastleVisitedHexId:campaign.currentHexId,
          horseFamilyRewardRoll:treasure.roll,horseFamilyRewardName:treasure.name,
          horseFamilyArtifactPending:treasure.requiresArtifactSelection,
          horseFamilyRewardCoins:treasure.coins,horseFamilyRewardValue:treasure.value,
          horseInsigniaReturnedAt:now,horseOwnershipAwardedAt:now}}}:e);
      const mounts=(world.journey.mounts||[]).map(m=>m.id===mount.id?{...m,
        sourceEventId:eventId,ownership:'awarded' as const}:m);
      const updated={...world,journey:{...world.journey,mounts,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return {world:updated,treasure};
    });this.notify(result.world);return result;
  }

  /** R88 #41: stock is set by the GM because the source gives no quantity. */
  static async setOfficialSoulMerchantStock(campaignId:string,eventId:string,quantity:number):Promise<WorldState>{
    if(!Number.isInteger(quantity)||quantity<0||quantity>999)throw new Error('Informe um estoque de 0 a 999 garrafas.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de ajustar o estoque.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||Number(mechanic?.encounterId)!==41||decision?.stage!=='soul_merchant_contact'||event.hexId!==campaign.currentHexId)
        throw new Error('O estoque do Mercador de Almas não pode ser definido nesta etapa.');
      const now=Date.now();const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'soul_merchant_stock_set',soulBottleStock:quantity,
          soulBottleStockUnspecified:false,stockSetAt:now,pending:quantity?'trade':'leave'}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  static async buyOfficialSoulBottle(campaignId:string,eventId:string,buyerId:string,quantity=1):Promise<WorldState>{
    if(!Number.isInteger(quantity)||quantity<1||quantity>99)throw new Error('Quantidade inválida para a compra.');
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(world.encounter?.status==='active'||!event||Number(mechanic?.encounterId)!==41||decision?.stage!=='soul_merchant_stock_set'||event.hexId!==campaign.currentHexId)
        throw new Error('A compra de garrafa de alma não está disponível.');
      // U10: NaN, strings, frações ou estoques negativos nunca podem liberar
      // compra de itens/moedas por comparação NaN < quantity (sempre falsa).
      const stock=decision.soulBottleStock;
      if(!Number.isSafeInteger(stock)||Number(stock)<0||Number(stock)>999||Number(stock)<quantity)
        throw new Error('O estoque adjudicado está inválido ou insuficiente para a compra.');
      const characterStore=tx.objectStore('characters');const character=await characterStore.get(buyerId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);if(!character||character.status==='dead')throw new Error('Escolha uma aventureira viva do grupo.');
      CharacterActionRulesEngine.assertCanAct(character);const costCopper=quantity*100;
      const available=ForbiddenLandsEconomyService.coinsToCopper(character.coins);if(available<costCopper)throw new Error(`Compra exige ${quantity*10} pratas; saldo insuficiente.`);
      const gear=[...(character.gear||[])];for(let i=0;i<quantity;i++)gear.push({id:`official-soul-bottle:${eventId}:${crypto.randomUUID()}`,
        name:'Garrafa de vinho selada com alma',itemKind:'official_soul_bottle',soulBottleState:'sealed',officialEncounterId:41,sourceEventId:eventId,
        source:'Guia do Mestre R88 p.165',quantity:1});
      const updatedCharacter={...character,coins:ForbiddenLandsEconomyService.copperToCoins(available-costCopper),gear};
      updatedCharacter.encumbrance=EncumbranceRulesEngine.calculate(updatedCharacter);await characterStore.put(updatedCharacter);
      const now=Date.now();const remaining=Number(stock)-quantity;
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,detail:`${item.detail} ${character.name} comprou ${quantity} garrafa(s) por ${quantity*10} pratas.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,soulBottleStock:remaining,soulBottlePurchases:[...((decision.soulBottlePurchases as unknown[])||[]),
          {characterId:character.id,quantity,priceSilver:quantity*10,paidAt:now}],pending:remaining?'trade':'leave'}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  static async leaveOfficialSoulMerchant(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(world.encounter?.status==='active'||!event||Number(mechanic?.encounterId)!==41||!['soul_merchant_contact','soul_merchant_stock_set'].includes(String(decision?.stage))||event.hexId!==campaign.currentHexId)
        throw new Error('A conversa com Vergevil não está pronta para encerrar.');
      const now=Date.now();const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,status:'resolved' as const,
        detail:`${item.detail} A companhia deixou o veleiro sem alterar o que não foi comprado ou aberto.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'soul_merchant_departed',pending:null,resolvedAt:now},resolvedAt:now,
          adjudicationMode:'official_r88_soul_merchant_departure'}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  static async barterOfficialSoulBottle(campaignId:string,eventId:string,characterId:string,collection:'gear'|'weapons'|'armor',index:number,expectedSnapshot?:string):Promise<WorldState>{
    if(!['gear','weapons','armor'].includes(collection)||!Number.isInteger(index)||index<0)throw new Error('Selecione um objeto válido para a troca.');
    const next=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(world.encounter?.status==='active'||!event||Number(mechanic?.encounterId)!==41||decision?.stage!=='soul_merchant_stock_set'||event.hexId!==campaign.currentHexId)
        throw new Error('A troca do Mercador de Almas não está disponível.');
      const stock=decision.soulBottleStock;
      if(!Number.isSafeInteger(stock)||Number(stock)<1||Number(stock)>999)
        throw new Error('O estoque adjudicado está inválido ou sem garrafas para troca.');
      const characterStore=tx.objectStore('characters');const character=await characterStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);if(!character||character.status==='dead')throw new Error('Escolha uma aventureira viva do grupo.');
      CharacterActionRulesEngine.assertCanAct(character);const items=[...(character[collection]||[])];const offered=items[index] as Record<string,unknown>|undefined;
      if(!offered||!['valuable','precious'].includes(String(offered.treasureCategory||offered.valueCategory||'')))
        throw new Error('Selecione um item marcado VALIOSO ou PRECIOSO no inventário.');
      if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(offered,expectedSnapshot);
      const offeredQuantity=Number(offered.quantity??1);
      if(!Number.isSafeInteger(offeredQuantity)||offeredQuantity<1)throw new Error('Quantidade do item valioso inválida.');
      if(offeredQuantity===1)items.splice(index,1);else items[index]={...offered,quantity:offeredQuantity-1};
      const bottle={id:`official-soul-bottle:${eventId}:${crypto.randomUUID()}`,
        name:'Garrafa de vinho selada com alma',itemKind:'official_soul_bottle',soulBottleState:'sealed',officialEncounterId:41,sourceEventId:eventId,
        source:'Guia do Mestre R88 p.165',quantity:1};
      const updatedCharacter=collection==='gear'?{...character,gear:[...items,bottle]}:
        {...character,[collection]:items,gear:[...(character.gear||[]),bottle]};
      updatedCharacter.encumbrance=EncumbranceRulesEngine.calculate(updatedCharacter);await characterStore.put(updatedCharacter);
      const now=Date.now();const remaining=Number(stock)-1;
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,detail:`${item.detail} ${character.name} trocou um item valioso por uma garrafa; a equivalência foi adjudicada pela Mestra.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,soulBottleStock:remaining,
          soulBottleBarters:[...((decision.soulBottleBarters as unknown[])||[]),{characterId,collection,itemName:String(offered.name||offered.label||'item valioso'),
            barterValueSetByGm:true,tradedAt:now}],pending:remaining?'trade':'leave'}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** Opening a bottle releases a tracked soul but never fakes a spell cast. */
  static async openOfficialSoulBottle(campaignId:string,eventId:string,characterId:string,index:number,expectedSnapshot?:string):Promise<{world:WorldState;soulTokenId:string}>{
    if(!Number.isInteger(index)||index<0)throw new Error('Garrafa inválida.');
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId);const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(world.encounter?.status==='active'||!event||Number(mechanic?.encounterId)!==41)
        throw new Error('Não é possível abrir a garrafa durante outro combate ou sem o encontro R88 correspondente.');
      const characterStore=tx.objectStore('characters');const character=await characterStore.get(characterId) as Character|undefined;
      CampaignPartyService.assertMember(campaign,character);if(!character||character.status==='dead')throw new Error('Escolha uma aventureira viva do grupo.');
      CharacterActionRulesEngine.assertCanAct(character);
      const gear=[...(character.gear||[])];const bottle=gear[index] as Record<string,unknown>|undefined;
      if(!bottle||bottle.itemKind!=='official_soul_bottle'||bottle.soulBottleState!=='sealed'||bottle.sourceEventId!==eventId)
        throw new Error('A garrafa selecionada não foi comprada neste encontro ou já está aberta.');
      if(expectedSnapshot !== undefined) InventorySelectionSnapshot.assertCurrent(bottle,expectedSnapshot);
      const tokenId=`official-released-soul:${eventId}:${crypto.randomUUID()}`;const quantity=Number(bottle.quantity??1);
      if(!Number.isSafeInteger(quantity)||quantity<1)throw new Error('Quantidade de garrafas inconsistente.');
      if(quantity>1)gear[index]={...bottle,quantity:quantity-1};else gear.splice(index,1);
      const updatedCharacter={...character,gear};updatedCharacter.encumbrance=EncumbranceRulesEngine.calculate(updatedCharacter);await characterStore.put(updatedCharacter);
      const now=Date.now();const tokens=[...((decision?.releasedSoulTokens as Array<Record<string,unknown>>|undefined)||[]),
        {id:tokenId,state:'released',releasedBy:characterId,releasedAt:now,resurrectionPending:true,sourceEventId:eventId}];
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,detail:`${item.detail} Uma garrafa foi aberta; a alma está livre e aguarda Ressurreição conforme R88.`,
        result:{...(item.result||{}),officialSceneDecision:{...(decision||{}),releasedSoulTokens:tokens,soulResurrectionD6Pending:true}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return {world:updated,soulTokenId:tokenId};
    });this.notify(outcome.world);return outcome;
  }

  static async resolveOfficialSaurianParley(campaignId:string,eventId:string,outcome:'peaceful'|'attack',evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim();if(evidence.length<8||evidence.length>600)throw new Error('Registre a adjudicação da conversa em 8–600 caracteres.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(world.encounter?.status==='active'||!event||Number(mechanic?.encounterId)!==42||decision?.stage!=='saurian_parley_pending'||event.hexId!==campaign.currentHexId)
        throw new Error('A conversa com Sheeerz não aguarda adjudicação.');
      const now=Date.now();const events=world.journey.events.map(item=>item.id!==eventId?item:outcome==='peaceful'?{...item,status:'resolved' as const,
        detail:`${item.detail} Conversa encerrada conforme adjudicação da Mestra.`,result:{...(item.result||{}),officialSceneDecision:{...decision,stage:'saurian_parley_resolved',pending:null,
          gmAdjudication:evidence,automaticRollsApplied:false,resolvedAt:now},resolvedAt:now,adjudicationMode:'official_r88_saurian_parley'}}:{...item,
        detail:`${item.detail} A conversa escalou para combate conforme adjudicação da Mestra.`,result:{...(item.result||{}),officialSceneDecision:{...decision,
          choiceId:'attack',stage:'pending_external_mechanics',pending:'combat',gmAdjudication:evidence,automaticRollsApplied:false,automaticCombatStarted:false}}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  static async resolveOfficialBurialRescue(campaignId:string,eventId:string,outcome:'rescued'|'executed'|'withdrawn',evidenceInput:string):Promise<WorldState>{
    const evidence=String(evidenceInput||'').trim();if(evidence.length<8||evidence.length>600)throw new Error('Registre a adjudicação do resgate em 8–600 caracteres.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const decision=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(world.encounter?.status==='active'||!event||Number(mechanic?.encounterId)!==43||decision?.stage!=='burial_rescue_pending'||event.hexId!==campaign.currentHexId)
        throw new Error('A cena de enterramento não aguarda adjudicação do resgate.');
      const now=Date.now();const fate={rescued:'family_rescued',executed:'family_executed',withdrawn:'party_withdrew'}[outcome];
      const events=world.journey.events.map(item=>item.id!==eventId?item:{...item,status:'resolved' as const,
        detail:`${item.detail} Destino da família registrado conforme adjudicação da Mestra: ${outcome}.`,
        result:{...(item.result||{}),officialSceneDecision:{...decision,stage:fate,pending:null,familyFate:outcome,
          gmAdjudication:evidence,rescueRollInvented:false,resolvedAt:now},resolvedAt:now,adjudicationMode:'official_r88_buried_alive_family'}});
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};await store.put(updated);return updated;
    });this.notify(next);return next;
  }

  /** R79: permite reconsiderar uma ação AINDA NÃO EXECUTADA, sem desbloquear
   * transações ou batalhas que tenham realmente começado. Mantém histórico
   * e o ID da primeira decisão para impedir a limpeza silenciosa do registro.
   */
  static async withdrawUnexecutedOfficialSceneChoice(campaignId:string,eventId:string,reasonInput:string):Promise<WorldState>{
    const reason=String(reasonInput||'').trim();
    if(reason.length<12||reason.length>400)throw new Error('Explique a retirada em 12–400 caracteres.');
    const updated=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Combate ativo: não é possível cancelar o encontro.');
      const event=world.journey.events.find(item=>item.id===eventId&&item.status==='open');
      const mechanic=event?.result?.mechanic as Record<string,unknown>|undefined;
      const spec=OfficialEncounterRulesEngine.specialScene(Number(mechanic?.encounterId));
      const current=event?.result?.officialSceneDecision as Record<string,unknown>|undefined;
      if(!event||mechanic?.kind!=='official_random_encounter'||!spec||
        current?.stage!=='pending_external_mechanics'||!current.choiceId)
        throw new Error('Não há escolha pendente que possa ser reconsiderada.');
      if(event.hexId!==campaign.currentHexId)throw new Error('Grupo fora do hex da ocorrência.');
      if(current.automaticResourcesChanged!==false||current.automaticRollsApplied!==false||current.automaticCombatStarted!==false)
        throw new Error('Ações mecânicas já registradas não podem ser canceladas.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:'resolved' as const,
        detail:`${item.detail} O grupo desistiu da ação antes de iniciá-la e se retirou. Justificativa: ${reason}`,
        result:{...(item.result||{}),officialSceneDecision:{...current,stage:'withdrawn_before_execution',
          priorChoiceId:current.choiceId,withdrawalReason:reason,withdrawnAt:now,
          automaticResourcesChanged:false,automaticRollsApplied:false,automaticCombatStarted:false},
          resolvedAt:now,adjudicationMode:'official_scene_before_execution_withdrawal'},
      }:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return next;
    });
    this.notify(updated);
    return updated;
  }

  /** Encontro nº 15 — Pedintes (Guia do Mestre, pp. 153–154).
   * Resolve somente a interação narrativa: nenhuma comida, moeda, dano,
   * perícia ou recompensa é aplicada pela fonte. Toda doação de itens deve
   * passar pela transação normal de inventário, não por este método.
   */
  static async resolveBeggarsEncounter(campaignId:string,eventId:string,
    reaction:'observed'|'spoke'|'continued',notesInput:string=''):Promise<WorldState>{
    if(!['observed','spoke','continued'].includes(reaction))throw new Error('Reação inválida para os pedintes.');
    const notes=String(notesInput||'').trim();
    if(notes.length>800)throw new Error('A anotação dos pedintes excede 800 caracteres.');
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de prosseguir com os pedintes.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&
        (item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter'&&
        Number((item.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===15);
      if(!source)throw new Error('O encontro nº 15 não está pendente.');
      if(!source.hexId||source.hexId!==campaign.currentHexId)throw new Error('O grupo saiu do hex dos pedintes.');
      const sourceData=OfficialEncounterRulesEngine.beggarsEncounter();
      if(sourceData.mandatoryChecks||sourceData.automaticResourceTransfer)
        throw new Error('Fonte de regras dos pedintes inconsistente.');
      const now=Date.now();
      const label=reaction==='observed'?'observou os pedintes':reaction==='spoke'?'tentou conversar com os pedintes':'prosseguiu sem interagir';
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:'resolved' as const,
        detail:`${item.detail} O grupo ${label}. Nenhum recurso foi transferido automaticamente.${notes?' Registro do jogador: '+notes:''}`,
        result:{...(item.result||{}),beggarsCount:sourceData.count,beggarsReaction:reaction,
          beggarsPlayerNotes:notes,resourceTransferApplied:false,mandatoryRollApplied:false,
          adjudicationMode:'official_beggars_no_mandatory_mechanics',resolvedAt:now},
      }:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);
      return updated;
    });
    this.notify(next);
    return next;
  }

  /** Encontro nº 13 — O Irmão da Praga, Guia do Mestre p. 153.
   * Tocar o cadáver transmite a praga demoníaca de virulência 3 segundo
   * o texto da ocorrência. A doença usa o motor existente de incubação,
   * manifestação e verificações diárias. Não há rolagem de resistência
   * especificada NESTA ocorrência para o contato inicial.
   */
  static async interactPlagueBrother(campaignId:string,eventId:string,characterId:string,action:'touch'|'take_medallion'):Promise<{world:WorldState;infected:boolean;newlyInfected:boolean;medallionTaken:boolean}>{
    if(action!=='touch'&&action!=='take_medallion')throw new Error('Interação inválida com o corpo infectado.');
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate antes de interagir com o cadáver.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&
        (item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter'&&
        Number((item.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===13);
      if(!source)throw new Error('O Irmão da Praga não está pendente.');
      if(!source.hexId||source.hexId!==campaign.currentHexId)throw new Error('A personagem não está no hex do cadáver.');
      const characters=tx.objectStore('characters');
      const character=await characters.get(characterId) as Character|undefined;
      if(!character||character.status==='dead')throw new Error('Personagem viva não encontrada.');
      CampaignPartyService.assertMember(campaign,character);
      CharacterActionRulesEngine.assertCanAct(character);
      const contactIds=(source.result?.plagueTouchedCharacterIds as string[]|undefined)||[];
      if(!Array.isArray(contactIds)||contactIds.some(id=>typeof id!=='string'))throw new Error('Histórico de contato do cadáver inválido.');
      const taking=action==='take_medallion';
      if(taking&&source.result?.plagueMedallionCharacterId)throw new Error('O medalhão já foi retirado deste cadáver.');
      if(contactIds.includes(characterId)&&!taking)throw new Error('O contato desta personagem já foi registrado.');
      const newlyExposed=!contactIds.includes(characterId);
      const alreadySick=Boolean(character.diseaseState);
      const infected=newlyExposed?DiseaseRulesEngine.infect(character,3):character;
      const afterContact=!alreadySick&&newlyExposed&&infected.diseaseState
        ? {...infected,diseaseState:{...infected.diseaseState,origin:'plague_brother' as const}}
        : infected;
      // Distinguish an infection actually acquired here from a pre-existing
      // disease. The UI must not tell the player a second infection occurred.
      const newlyInfected=!alreadySick&&newlyExposed&&Boolean(afterContact.diseaseState);
      const newGear=taking?[...(afterContact.gear||[]),{
        id:`plague-brother-medallion:${eventId}`,
        name:'Medalhão de bronze da Irmandade Enferrujada',quantity:1,
        encumbranceClass:'small',description:'Medalhão de bronze simples com o símbolo da Irmandade Enferrujada; encontro oficial nº 13.',
      }]:afterContact.gear;
      await characters.put({...afterContact,gear:newGear});
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,result:{...(item.result||{}),plagueTouchedCharacterIds:newlyExposed?[...contactIds,characterId]:contactIds,
          ...(taking?{plagueMedallionCharacterId:characterId}:{}),plagueVirulence:3,
          plagueInfectionApplied:Boolean(source.result?.plagueInfectionApplied)||newlyInfected,
          plagueLastContactAt:now},
        detail:`${item.detail} ${character.name} ${taking?'retirou o medalhão de bronze':'tocou o cadáver'}; contato com praga de Virulência 3 registrado${alreadySick?' (doença preexistente preservada)':''}.`,
      }:item);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);
      return {world:next,infected:Boolean(afterContact.diseaseState),newlyInfected,medallionTaken:taking};
    });
    this.notify(outcome.world);return outcome;
  }

  /** Encerrar o nº 13 sem tocar (ou depois de coletar evidências).
   * Não produz cura ou resistência retroativa para quem foi infectado.
   */
  static async leavePlagueBrother(campaignId:string,eventId:string):Promise<WorldState>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&
        (item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter'&&
        Number((item.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===13);
      if(!source)throw new Error('O encontro nº 13 não está pendente.');
      if(source.hexId!==campaign.currentHexId)throw new Error('A campanha saiu do hex do cadáver.');
      if(world.encounter?.status==='active')throw new Error('Finalize o combate atual antes de prosseguir.');
      const now=Date.now();
      const events=world.journey.events.map(item=>item.id===eventId?{
        ...item,status:'resolved' as const,
        result:{...(item.result||{}),adjudicationMode:'official_plague_brother',resolvedAt:now},
        detail:`${item.detail} O grupo prosseguiu; contatos e doença registrados permanecem válidos.`
      }:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(updated);return updated;
    });
    this.notify(next);return next;
  }

  /** Encontro oficial nº 9, Guia do Mestre p. 151.
   * Resolve a tabela, cria um POI VISÍVEL e grava evento + histórico
   * na mesma transação. Não dispensa o teste normal para FAZER ACAMPAMENTO:
   * o livro diz apenas que a ruína é um local seguro, não concede bônus.
   */
  static async discoverAncientRuin(campaignId:string,eventId:string,rng:()=>number=Math.random):Promise<{world:WorldState;roll:number;ruinType:string|null;requiresRuling:boolean;poiId?:string}> {
    const result=await withTransaction(['campaign','world_state','individual_hexes'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate ativo antes de descobrir as ruínas.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&
        (item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter'&&
        Number((item.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===9);
      if(!source)throw new Error('Encontro nº 9 não encontrado ou já resolvido.');
      if(!source.hexId||!campaign.currentHexId||source.hexId!==campaign.currentHexId)
        throw new Error('As ruínas somente podem ser descobertas no hex onde ocorreu o encontro.');
      if(source.result?.ancientRuinRoll!==undefined)
        throw new Error('O D66 dessas ruínas já foi lançado; não é permitido rolar novamente.');
      const {roll,ruin}=OfficialEncounterRulesEngine.rollAncientRuin(rng);
      const hex=await tx.objectStore('individual_hexes').get(source.hexId) as IndividualHex|undefined;
      if(!hex?.mapId)throw new Error('O mapa do hex das ruínas não foi encontrado; descoberta não foi gravada.');
      const now=Date.now();
      if(!ruin){
        const events=world.journey.events.map(item=>item.id===eventId?
          {...item,result:{...(item.result||{}),ancientRuinRoll:roll,tableGapRequiresRuling:true,adjudicationMode:'pending_manual_source_gap'},
            detail:`${item.detail} D66 ${roll}: não há linha para este resultado na tabela impressa (p. 151). A mesa deve arbitrar e documentar a decisão, sem inventar um resultado automático.`}:item);
        const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
        await store.put(next);return {world:next,roll,ruinType:null,requiresRuling:true};
      }
      const poi=CampaignPoiService.normalize({
        id:`official-encounter-9:${eventId}`,campaignId,mapId:hex.mapId,hexId:source.hexId,
        name:`${ruin.kind} antiga`,type:'ruin',
        description:'Ruína descoberta pelo grupo. Local seguro para acampar, sem bônus automático nem dispensa da preparação de acampamento.',
        layer:'CAMPAIGN',canonical:false,hidden:false,discovered:true,createdBy:'system',status:'ruined',
        tags:['official-encounter:9','safe-campsite','ruin-table:151'],developmentLevel:1,createdAt:now,updatedAt:now,
      },campaignId,now);
      const poiHistory={id:crypto.randomUUID(),poiId:poi.id,campaignId,at:now,day:campaign.currentDay||1,kind:'created' as const,reason:`Guia do Mestre p. 151, Ruínas Antigas, D66 ${roll}`,after:poi};
      const events=world.journey.events.map(item=>item.id===eventId?
        {...item,status:'resolved' as const,result:{...(item.result||{}),ancientRuinRoll:roll,ruinType:ruin.kind,formerInhabitants:ruin.formerInhabitants,safeCampsite:true,poiId:poi.id,adjudicationMode:'automated_ruin_table',resolvedAt:now},
          gmDetail:`${item.gmDetail||''} Histórico do local (uso do Mestre): ${ruin.formerInhabitants}.`,detail:`${item.detail} Encontradas: ${ruin.kind}. D66 ${roll}. Local seguro para montar acampamento.`}:item);
      const next={...world,campaignPois:[...(world.campaignPois||[]),poi].slice(-3000),
        poiHistory:[...(world.poiHistory||[]),poiHistory].slice(-12000),
        journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);
      return {world:next,roll,ruinType:ruin.kind,requiresRuling:false,poiId:poi.id};
    });
    this.notify(result.world);
    return result;
  }

  /** Resolução transparente para o D66 63 omitido na página impressa 151.
   * Não muda o dado; a mesa escolhe uma linha EXISTENTE e explica por quê.
   */
  static async adjudicateAncientRuinGap(campaignId:string,eventId:string,ruinKind:string,reasonInput:string):Promise<{world:WorldState;poiId:string}>{
    const reason=String(reasonInput||'').trim();
    if(reason.length<12||reason.length>500)throw new Error('Descreva a arbitragem para a lacuna D66 63 (mínimo 12, máximo 500 caracteres).');
    const choice=OfficialEncounterRulesEngine.ancientRuinByManualChoice(ruinKind);
    if(!choice)throw new Error('Escolha uma das ruínas efetivamente listadas na tabela impressa.');
    const result=await withTransaction(['campaign','world_state','individual_hexes'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Finalize o combate ativo antes de descobrir as ruínas.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&
        Number((item.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===9&&
        (item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='official_random_encounter');
      if(!source||source.result?.ancientRuinRoll!==63||source.result?.tableGapRequiresRuling!==true)
        throw new Error('Nenhuma lacuna D66 63 pendente neste encontro.');
      if(!source.hexId||source.hexId!==campaign.currentHexId)throw new Error('Personagens não estão mais no hex do encontro.');
      const hex=await tx.objectStore('individual_hexes').get(source.hexId) as IndividualHex|undefined;
      if(!hex?.mapId)throw new Error('O mapa do hex das ruínas não foi encontrado; arbitragem não foi gravada.');
      const now=Date.now();
      const poi=CampaignPoiService.normalize({
        id:`official-encounter-9:${eventId}`,campaignId,mapId:hex.mapId,hexId:source.hexId,
        name:`${choice.kind} antiga`,type:'ruin',description:'Ruína descoberta pelo grupo. Local seguro para acampar; escolha da mesa após lacuna D66 63.',
        layer:'CAMPAIGN',canonical:false,hidden:false,discovered:true,createdBy:'system',status:'ruined',
        tags:['official-encounter:9','safe-campsite','ruin-table:151','manual-table-gap:63'],developmentLevel:1,createdAt:now,updatedAt:now,
      },campaignId,now);
      const events=world.journey.events.map(item=>item.id===eventId?
        {...item,status:'resolved' as const,result:{...(item.result||{}),ruinType:choice.kind,formerInhabitants:choice.formerInhabitants,safeCampsite:true,poiId:poi.id,adjudicationMode:'manual_source_gap_63',rulingReason:reason,resolvedAt:now},
          gmDetail:`${item.gmDetail||''} Histórico do local (uso do Mestre): ${choice.formerInhabitants}. Arbitragem: ${reason}`,detail:`${item.detail} Arbitragem registrada: ${choice.kind} (D66 63); decisão documentada.`}:item);
      const next={...world,campaignPois:[...(world.campaignPois||[]),poi].slice(-3000),
        poiHistory:[...(world.poiHistory||[]),{id:crypto.randomUUID(),poiId:poi.id,campaignId,at:now,day:campaign.currentDay||1,kind:'created' as const,reason:`Encontro nº 9, D66 63: ${reason}`,after:poi}].slice(-12000),
        journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await store.put(next);return {world:next,poiId:poi.id};
    });
    this.notify(result.world);return result;
  }

  static async startWildAnimalEncounter(campaignId:string,eventId:string,animalId:string,rng:()=>number=Math.random):Promise<WorldState>{
    const now=Date.now();
    const world=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(current.encounter?.status==='active')throw new Error('Já existe um encontro em andamento.');
      const source=current.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='creature_encounter');
      if(!source)throw new Error('Infortúnio de animal selvagem não está pendente.');
      const animal=JourneyRulesEngine.officialWildAnimal(animalId);
      if(!animal)throw new Error('Escolha um animal da tabela oficial do Guia do Mestre.');
      const characters=CampaignPartyService.members(campaign, await tx.objectStore('characters').getAll() as Character[]);
      const enemy:EncounterCombatant={
        id:`journey-animal:${crypto.randomUUID()}`,name:animal.name,kind:'npc',side:'enemy',initiative:1+Math.floor(Math.max(0,Math.min(.999999,rng()))*10),
        currentStrength:animal.strength,maxStrength:animal.strength,armor:0,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',
        notes:[`Animal oficial do Guia do Mestre. Movimento ${animal.movement}.`,animal.notes||''].filter(Boolean).join(' '),x:.82,y:.5,size:1,
        quickSheet:{category:'animal',attributes:{strength:animal.strength,agility:animal.agility},skills:animal.skills,movement:animal.movement,weapons:[{name:animal.weapon,damage:animal.damage,damageType:animal.damageType}],source:'Guia do Mestre, pp. 126–127 — Animais'},
      };
      const encounter=EncounterService.createEncounter(`Animal Selvagem — ${animal.name}`,campaign.currentHexId||undefined,characters,[enemy]);
      const triggered=EncounterService.triggerWatchOnEncounter(current,encounter,characters,campaign.currentDay||1,campaign.currentPeriod||'Morning',rng);
      for(const roll of triggered.rolls)await tx.objectStore('rolls').put(roll);
      const events=triggered.world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,result:{...(item.result||{}),selectedAnimalId:animal.id,selectedAnimalName:animal.name,encounterId:encounter.id},detail:`${item.detail} O Mestre escolheu ${animal.name}; encontro de combate iniciado.`}:item);
      const next={...triggered.world,journey:{...triggered.world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async resolveFishingGearDamage(campaignId:string,eventId:string,gearIndex:number,crafterId:string,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;repaired:boolean;destroyed:boolean;itemName:string}> {
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de reparar o equipamento de pesca.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='fishing_gear_damaged');
      if(!source)throw new Error('Equipamento de pesca quebrado não está pendente.');
      const actorId=String(source.result?.actorId||'');
      const characters=tx.objectStore('characters');
      const actor=await characters.get(actorId) as Character|undefined;
      const crafter=await characters.get(crafterId) as Character|undefined;
      if(!actor||!crafter)throw new Error('Pescadora ou artesã não encontrada.');
      const fishingCampaign = await tx.objectStore('campaign').get(campaignId);
      CampaignPartyService.assertMember(fishingCampaign,actor);
      CampaignPartyService.assertMember(fishingCampaign,crafter);
      CharacterActionRulesEngine.assertCanAct(crafter);
      if(!Number.isInteger(gearIndex)||gearIndex<0||gearIndex>=actor.gear.length)throw new Error('Escolha o equipamento de pesca quebrado.');
      const item=actor.gear[gearIndex] as any;
      const itemName=String(item?.name||item?.label||`Equipamento ${gearIndex+1}`);
      if(!/anzol|linha|rede|pesca|fishing|hook|net/i.test(itemName))throw new Error('O item escolhido não é equipamento de pesca reconhecido.');
      const officialBonus=/rede|net/i.test(itemName)?2:1;
      const originalBonus=Math.max(officialBonus,Number(item?.originalBonus ?? item?.bonus ?? item?.equipmentBonus ?? 0)||0);
      const currentBonus=Math.max(0,Math.min(originalBonus,Number(item?.currentBonus ?? item?.bonus ?? 0)||0));
      if(!item?.broken&&currentBonus>=originalBonus)throw new Error('Este equipamento de pesca não está danificado.');
      const crafting=normalizedSkill(crafter,['Crafting','Artesanato']);
      const roll=ForbiddenLandsDiceEngine.roll({base:crafter.attributes.strength.current,skill:crafting,gear:0},{characterId:crafter.id,sceneId:`journey:fishing-repair:${campaignId}`,checkId:`journey:fishing-repair:${eventId}:${now}`,rng,criticalInjuries:crafter.criticalInjuryStates,skillName:'crafting',skillRankAtRoll:crafting,attributeName:'strength'});
      await tx.objectStore('rolls').put(roll);
      let repaired=false, destroyed=false;
      let nextGear=actor.gear.slice() as any[];
      if(roll.successes>0){
        const nextCurrent=Math.min(originalBonus,currentBonus+roll.successes);
        nextGear[gearIndex]={...item,originalBonus,currentBonus:nextCurrent,bonus:nextCurrent,broken:nextCurrent<1,journeyDamage:nextCurrent<1?{kind:'fishing_gear_damaged',eventId}:undefined};
        repaired=nextCurrent>currentBonus;
      }else if(currentBonus===0){
        nextGear.splice(gearIndex,1);
        destroyed=true;
      }else{
        nextGear[gearIndex]={...item,originalBonus:currentBonus,currentBonus,bonus:currentBonus,broken:currentBonus<1};
      }
      await characters.put({...actor,gear:nextGear});
      const closed=repaired||destroyed;
      const outcomeText=destroyed?'falhou no reparo e o item foi destruído':repaired?'reparou':'falhou ao reparar';
      const events=world.journey.events.map(entry=>entry.id===eventId?{...entry,status:closed?'resolved' as const:'open' as const,result:{...(entry.result||{}),gearIndex,itemName,broken:!repaired,repairRollIds:[...((entry.result?.repairRollIds as string[]|undefined)||[]),roll.id],repaired,destroyed},detail:`${entry.detail} ${crafter.name} ${outcomeText} ${itemName} (${roll.successes} sucesso(s)).`}:entry);
      const next={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(next);
      return {roll,repaired,destroyed,itemName,world:next};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,repaired:outcome.repaired,destroyed:outcome.destroyed,itemName:outcome.itemName};
  }

  static async discardFishingGear(campaignId:string,eventId:string,gearIndex:number):Promise<string>{
    const result=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='fishing_gear_damaged');
      if(!source)throw new Error('Equipamento de pesca quebrado não está pendente.');
      const characters=tx.objectStore('characters');
      const actor=await characters.get(String(source.result?.actorId||'')) as Character|undefined;
      if(!actor)throw new Error('Pescadora não encontrada.');
      CampaignPartyService.assertMember(await tx.objectStore('campaign').get(campaignId),actor);
      if(!Number.isInteger(gearIndex)||gearIndex<0||gearIndex>=actor.gear.length)throw new Error('Escolha o equipamento quebrado.');
      const item:any=actor.gear[gearIndex]; const itemName=String(item?.name||item?.label||`Equipamento ${gearIndex+1}`);
      if(!/anzol|linha|rede|pesca|fishing|hook|net/i.test(itemName))throw new Error('O item escolhido não é equipamento de pesca reconhecido.');
      await characters.put({...actor,gear:actor.gear.filter((_,index)=>index!==gearIndex)});
      const events=world.journey.events.map(entry=>entry.id===eventId?{...entry,status:'resolved' as const,result:{...(entry.result||{}),discarded:true,itemName},detail:`${entry.detail} ${itemName} foi descartado; será necessário obter outro equipamento para PESCAR.`}:entry);
      const next={...world,journey:{...world.journey,events,updatedAt:Date.now()},updatedAt:Date.now()}; await worldStore.put(next); return {itemName,world:next};
    });
    this.notify(result.world); return result.itemName;
  }

  static async assignRole(campaignId: string, role: JourneyRole, characterId: string | null): Promise<WorldState> {
    const world = await withTransaction(['campaign', 'world_state', 'characters'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      if (characterId) CampaignPartyService.assertMember(await tx.objectStore('campaign').get(campaignId), await characterStore.get(characterId));
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const assignments = current.journey.assignments.filter(item => item.role !== role);
      if (characterId) assignments.push({ role, characterId, assignedAt: Date.now() });
      const next: WorldState = {
        ...current,
        journey: { ...current.journey, assignments, updatedAt: Date.now() },
        updatedAt: Date.now(),
      };
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async setPace(campaignId: string, pace: JourneyPace): Promise<WorldState> {
    const world = await withTransaction('world_state', 'readwrite', async tx => {
      const current = WorldStateService.normalize(await tx.store.get(campaignId), campaignId);
      const next = { ...current, journey: { ...current.journey, pace, updatedAt: Date.now() }, updatedAt: Date.now() };
      await tx.store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async rollDailyWeather(campaignId: string, rng: () => number = Math.random): Promise<WorldState> {
    const now = Date.now();
    const world = await withTransaction(['campaign','world_state','rolls'], 'readwrite', async tx => {
      const campaign = await tx.objectStore('campaign').get(campaignId) as CampaignState | undefined;
      if (!campaign) throw new Error('Campanha não encontrada.');
      const day = Math.max(1, Number(campaign.currentDay) || 1);
      const worldStore = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      if (current.journey.dailyWeather?.day === day) return current;

      const sceneId = `journey-weather:${campaignId}:${day}`;
      const rolls = (['wind','rainfall','temperature'] as const).map(kind => ForbiddenLandsDiceEngine.roll(
        { base: 1, skill: 0, gear: 0 },
        { characterId: null, sceneId, checkId: `weather:${day}:${kind}`, rng },
      ));
      const weather = JourneyRulesEngine.dailyWeather(
        rolls[0].results[0]?.value || 1,
        rolls[1].results[0]?.value || 1,
        rolls[2].results[0]?.value || 1,
      );
      const dailyWeather: JourneyDailyWeather = {
        day,
        windRoll: weather.windRoll,
        rainfallRoll: weather.rainfallRoll,
        temperatureRoll: weather.temperatureRoll,
        wind: weather.wind,
        precipitation: weather.precipitation,
        temperature: weather.temperature,
        rollIds: rolls.map(roll => roll.id),
      };
      const windLabel = { none:'sem vento', breeze:'brisa', gales:'vendaval', storm:'tempestade' }[weather.wind];
      const rainLabel = { clear:'céu aberto', none:'sem precipitação', drizzle:'garoa/neve fraca', downpour:'chuva/neve forte' }[weather.precipitation];
      const temperatureLabel = { hot:'quente', pleasant:'agradável', cool:'frio' }[weather.temperature];
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'weather', title: `Clima da jornada — Dia ${day}`,
        detail: `Vento ${rolls[0].results[0]?.value || 1}: ${windLabel}; precipitação ${rolls[1].results[0]?.value || 1}: ${rainLabel}; temperatura ${rolls[2].results[0]?.value || 1}: ${temperatureLabel}.`,
        result: { mechanic: 'daily_weather', ...dailyWeather, makeCampModifier: weather.makeCampModifier, leadWayModifier: weather.leadWayModifier, marchCheckRequired: weather.marchCheckRequired, marchModifier: weather.marchModifier, coldCheckRequired: weather.coldCheckRequired, thirstCheckRequired: weather.thirstCheckRequired },
        ruleSource: 'Book of Beasts, p. 152–153 — Weather (regra opcional)',
        day, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const next = { ...current, journey: { ...current.journey, dailyWeather, events: retainJourneyEvents([...current.journey.events, event]), updatedAt: now }, updatedAt: now };
      for (const roll of rolls) await tx.objectStore('rolls').put(roll);
      await worldStore.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async resolveWeatherTravelChecks(campaignId: string, rng: () => number = Math.random) {
    const now = Date.now();
    const result = await withTransaction(['campaign','world_state','characters','rolls'], 'readwrite', async tx => {
      const campaign = await tx.objectStore('campaign').get(campaignId) as CampaignState | undefined;
      if (!campaign) throw new Error('Campanha não encontrada.');
      const day = Math.max(1, Number(campaign.currentDay) || 1);
      const period = campaign.currentPeriod || 'Morning';
      const key = `${day}:${period}`;
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const daily = currentWorld.journey.dailyWeather;
      if (!daily || daily.day !== day) return { world: currentWorld, checks: [], stopped: false, alreadyResolved: false };
      const weather = JourneyRulesEngine.dailyWeather(daily.windRoll, daily.rainfallRoll, daily.temperatureRoll);
      const kinds = [
        ...(weather.marchCheckRequired ? ['march' as const] : []),
        ...(weather.coldCheckRequired ? ['cold' as const] : []),
        ...(weather.thirstCheckRequired ? ['heat' as const] : []),
      ];
      if (!kinds.length) return { world: currentWorld, checks: [], stopped: false, alreadyResolved: false };
      if (currentWorld.journey.weatherCheckQuarterKey === key) return { world: currentWorld, checks: [], stopped: currentWorld.journey.weatherStoppedQuarterKey === key, alreadyResolved: true };

      const characterStore = tx.objectStore('characters');
      const party = CampaignPartyService.members(campaign, await characterStore.getAll() as Character[]).filter(member => member.status !== 'dead');
      const checks: Array<{ characterId: string; characterName: string; kind: 'march'|'cold'|'heat'; successes: number; rollId: string; modifier: number }> = [];
      let stopped = false;
      for (const member of party) {
        for (const kind of kinds) {
          const current = await characterStore.get(member.id) as Character;
          const skill = normalizedSkill(current, ['Endurance','Resilience','Resiliência']);
          const modifier = kind === 'march' ? weather.marchModifier : 0;
          const roll = ForbiddenLandsDiceEngine.roll({ base: current.attributes.strength.current, skill, gear: 0, modifier }, {
            characterId: current.id,
            sceneId: campaign.id,
            checkId: `journey-weather:${kind}:${campaign.id}:${key}:${current.id}`,
            rng,
            criticalInjuries: current.criticalInjuryStates,
            skillName: 'endurance',
            skillRankAtRoll: skill,
            attributeName: 'strength',
          });
          await tx.objectStore('rolls').put(roll);
          checks.push({ characterId: current.id, characterName: current.name, kind, successes: roll.successes, rollId: roll.id, modifier });
          if (kind === 'march' && roll.successes === 0) stopped = true;
          if (kind === 'cold') await characterStore.put(SurvivalRulesEngine.applyColdCheck(current, roll.successes).character);
          if (kind === 'heat') await characterStore.put(SurvivalRulesEngine.applyHeatCheck(current, roll.successes));
        }
      }
      const summary = checks.map(check => `${check.characterName}: ${check.kind === 'march' ? 'manter a marcha' : check.kind === 'cold' ? 'resistir ao frio' : 'resistir ao calor'} — ${check.successes} sucesso(s)${check.modifier ? ' (−2)' : ''}`).join('; ');
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'weather', title: stopped ? 'Clima interrompe a marcha' : 'Testes de clima da jornada',
        detail: `${summary || 'Nenhum viajante vivo precisou testar.'}${stopped ? ' A viagem não pode avançar neste Quarto de Dia.' : ' O grupo pode continuar a viagem neste Quarto de Dia.'}`,
        result: { mechanic: 'weather_travel_checks', day, period, weatherRollIds: daily.rollIds, checks, stopped },
        ruleSource: 'Book of Beasts, p. 152–153 — Weather (regra opcional)',
        day, period, hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const nextWorld = {
        ...currentWorld,
        journey: {
          ...currentWorld.journey,
          weatherCheckQuarterKey: key,
          weatherStoppedQuarterKey: stopped ? key : currentWorld.journey.weatherStoppedQuarterKey,
          events: retainJourneyEvents([...currentWorld.journey.events, event]),
          updatedAt: now,
        },
        updatedAt: now,
      };
      await worldStore.put(nextWorld);
      return { world: nextWorld, checks, stopped, alreadyResolved: false };
    });
    this.notify(result.world);
    return result;
  }


  static async resolveBitterReachTravelChecks(campaignId:string,rng:()=>number=Math.random){
    const outcome=await BitterReachRuntimeService.resolveTravelChecks(campaignId,rng);
    this.notify(outcome.world);
    return outcome;
  }


  static async registerMount(campaignId: string, input: { name: string; riderCharacterId: string; strength: number }): Promise<WorldState> {
    const name=String(input.name||'').trim().slice(0,48);
    const riderCharacterId=String(input.riderCharacterId||'').trim();
    const strength=Math.max(1,Math.min(30,Math.trunc(Number(input.strength)||0)));
    if(!name||!riderCharacterId)throw new Error('Informe o nome da montaria e escolha quem a conduz.');
    const now=Date.now();
    const world=await withTransaction(['world_state','characters'],'readwrite',async tx=>{
      const character=await tx.objectStore('characters').get(riderCharacterId) as Character|undefined;
      if(!character||character.status==='dead')throw new Error('A pessoa que conduz precisa existir e estar viva.');
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      if(current.journey.mounts?.some(mount=>mount.riderCharacterId===riderCharacterId))throw new Error('Essa pessoa já tem uma montaria registrada na jornada.');
      const mount:JourneyMount={id:crypto.randomUUID(),name,riderCharacterId,strength,status:'ready',createdAt:now};
      const next={...current,journey:{...current.journey,mounts:[...(current.journey.mounts||[]),mount],updatedAt:now},updatedAt:now};
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async removeMount(campaignId: string, mountId: string): Promise<WorldState> {
    const now=Date.now();
    const world=await withTransaction('world_state','readwrite',async tx=>{
      const store=tx.store;
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const mounts=current.journey.mounts||[];
      if(!mounts.some(mount=>mount.id===mountId))throw new Error('Montaria não encontrada na jornada.');
      const next={...current,journey:{...current.journey,mounts:mounts.filter(mount=>mount.id!==mountId),updatedAt:now},updatedAt:now};
      await store.put(next);
      return next;
    });
    this.notify(world);
    return world;
  }

  static async recoverMount(campaignId: string, mountId: string, rng:()=>number=Math.random): Promise<{world:WorldState;roll:import('../types').DiceRoll;recovered:boolean}> {
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaignRaw=await tx.objectStore('campaign').get(campaignId);
      if(!campaignRaw)throw new Error('Campanha não encontrada.');
      const campaign=CampaignService.normalize(campaignRaw);
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      const mount=(world.journey.mounts||[]).find(item=>item.id===mountId);
      if(!mount)throw new Error('Montaria não encontrada na jornada.');
      if(mount.status!=='lame')throw new Error('Esta montaria não está manca.');
      if((campaign.currentDay||1)<=(mount.lameDay||campaign.currentDay||1))throw new Error('A montaria precisa descansar até o próximo dia antes do teste de recuperação.');
      const rider=await tx.objectStore('characters').get(mount.riderCharacterId) as Character|undefined;
      if(!rider||rider.status==='dead')throw new Error('A pessoa que conduz a montaria não está disponível.');
      CampaignPartyService.assertMember(campaign,rider);
      CharacterActionRulesEngine.assertCanAct(rider);
      const skill=normalizedSkill(rider,['Animal Handling','Adestramento']);
      const roll=ForbiddenLandsDiceEngine.roll({base:mount.strength,skill,gear:0},{characterId:rider.id,sceneId:`journey:mount-recovery:${campaignId}`,checkId:`mount-recovery:${mount.id}:${campaign.currentDay||1}`,rng,criticalInjuries:rider.criticalInjuryStates,skillName:'animal_handling',attributeName:'strength',skillRankAtRoll:skill});
      await tx.objectStore('rolls').put(roll);
      const recovered=roll.successes>0;
      const nextMount={...mount,...(recovered?{status:'ready' as const,lameDay:undefined}:{})};
      const detail=recovered?`${rider.name} recuperou ${mount.name} com Adestramento (${roll.successes} sucesso(s)); a montaria pode viajar novamente.`:`${rider.name} tentou recuperar ${mount.name} com Adestramento e falhou; a montaria continua manca.`;
      const event:JourneyEvent={id:crypto.randomUUID(),type:'travel',title:recovered?'Montaria recuperada':'Recuperação de montaria falhou',detail,result:{mechanic:'mount_recovery',mountId:mount.id,riderCharacterId:rider.id,rollId:roll.id,successes:roll.successes,recovered},ruleSource:JOURNEY_RULE_SOURCE,day:campaign.currentDay||1,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      const nextWorld={...world,journey:{...world.journey,mounts:(world.journey.mounts||[]).map(item=>item.id===mount.id?nextMount:item),events:retainJourneyEvents([...world.journey.events,event]),updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {world:nextWorld,roll,recovered};
    });
    this.notify(outcome.world);
    return outcome;
  }

  static async setPlannedRoute(campaignId: string, routeIds: string[]): Promise<WorldState> {
    const requested = Array.from(new Set((routeIds || []).map(value => String(value || '').trim().toUpperCase()).filter(Boolean))).slice(0, 5);
    const world = await withTransaction(['campaign', 'world_state', 'individual_hexes'], 'readwrite', async tx => {
      const campaignRaw = await tx.objectStore('campaign').get(campaignId);
      if (!campaignRaw) throw new Error('Campanha não encontrada.');
      const campaign = CampaignService.normalize(campaignRaw);
      if (!campaign.currentHexId) throw new Error('Defina primeiro o hex atual da campanha.');
      const hexes = await tx.objectStore('individual_hexes').getAll();
      const byId = new Map<string, IndividualHex>();
      for (const hex of hexes) { byId.set(hex.id, hex); byId.set(hex.coordinate, hex); }
      const currentHex = byId.get(campaign.currentHexId);
      if (!currentHex) throw new Error(`O hex atual ${campaign.currentHexId} não existe no mapa importado.`);

      const validated: string[] = [];
      let previous = currentHex.coordinate || currentHex.id;
      for (const requestedId of requested) {
        const hex = byId.get(requestedId);
        if (!hex) throw new Error(`O hex ${requestedId} não existe no mapa importado.`);
        const coordinate = hex.coordinate || hex.id;
        if (!MapTravelService.getNeighborIds(previous).includes(coordinate)) {
          throw new Error(`${coordinate} não é adjacente a ${previous}. A rota precisa ser contínua.`);
        }
        validated.push(coordinate);
        previous = coordinate;
      }

      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const now = Date.now();
      let next: WorldState = {
        ...current,
        journey: { ...current.journey, plannedRouteHexIds: validated, updatedAt: now },
        updatedAt: now,
      };
      const environmental = EnvironmentalDirectorService.evaluate({
        campaign,
        currentHex,
        worldState: next,
        allHexes: hexes,
        destinationHex: validated[0] ? byId.get(validated[0]) : null,
        now,
      });
      next = { ...next, environment: environmental.environment, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
    if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('environment-updated', { detail: world.environment }));
    return world;
  }


  static async resolveActivity(campaignId: string, input: JourneyActivityInput): Promise<JourneyActivityResult> {
    const campaignRaw = await withDbRetry(db => db.get('campaign', campaignId));
    if (!campaignRaw) throw new Error('Campanha não encontrada.');
    const campaign = CampaignService.normalize(campaignRaw);
    const character = await withDbRetry(db => db.get('characters', input.characterId));
    if (!character) throw new Error('Personagem não encontrado.');
    CampaignPartyService.assertMember(campaign,character);
    const nocturnal = (character.criticalInjuryStates || []).some(state => state.status !== 'healed' && state.status !== 'fatal' && (state.effects || []).some(effect => effect.kind === 'sleep_restriction' && effect.onlyDuringDaylight === true));
    if (input.activity === 'sleep' && nocturnal && !['Morning', 'Day'].includes(campaign.currentPeriod || 'Morning')) {
      throw new Error('O ferimento Noturna permite dormir somente durante o dia (manhã ou dia).');
    }
    if (input.activity === 'sleep') {
      if (character.status === 'dead') CharacterActionRulesEngine.assertCanAct(character);
    } else CharacterActionRulesEngine.assertCanAct(character);
    if(input.activity==='sleep'&&(character.magicMishapState?.insomniaQuartersRemaining||0)>0)throw new Error('O infortúnio mágico de insônia impede dormir até o relógio de dias terminar.');
    const worldBefore = WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaignId)), campaignId);
    const currentHex = campaign.currentHexId ? await withDbRetry(db => db.get('individual_hexes', campaign.currentHexId!)) : undefined;
    const bitterTerrain = BitterReachModule.terrainForHex(currentHex);
    const bitterWeather = worldBefore.journey.bitterReachWeather?.day === (campaign.currentDay || 1) ? worldBefore.journey.bitterReachWeather : null;
    if (worldBefore.encounter?.status === 'active') throw new Error('Encerre o encontro antes de executar uma atividade de jornada.');
    if (worldBefore.journey.events.some(event=>event.status==='open'&&['official_random_encounter','creature_encounter','hunt_prey_choice'].includes(String((event.result?.mechanic as Record<string,unknown>|undefined)?.kind||'')))) {
      throw new Error('Resolva o encontro de jornada pendente antes de iniciar outra atividade.');
    }
    const sleepDenial = input.activity === 'sleep' ? campSleepDenialReason(worldBefore, campaign) : null;
    if (sleepDenial) throw new Error(sleepDenial);
    if(input.activity==='sleep'&&character.liceState)throw new Error('Piolhos impedem dormir até serem removidos com Cura.');

    const now = Date.now();
    const season = input.season || 'summer';
    const terrainKey = input.terrainKey || 'plains';
    const dailyWeather = worldBefore.journey.dailyWeather?.day === (campaign.currentDay || 1)
      ? JourneyRulesEngine.dailyWeather(worldBefore.journey.dailyWeather.windRoll, worldBefore.journey.dailyWeather.rainfallRoll, worldBefore.journey.dailyWeather.temperatureRoll)
      : null;
    const helperIds = [...new Set((input.helperIds || []).map(String).filter(id => id && id !== input.characterId))];
    if (helperIds.length > 3) throw new Error('No máximo três pessoas podem ajudar em uma atividade.');
    const helperCharacters = await withDbRetry(async db => Promise.all(helperIds.map(id => db.get('characters', id) as Promise<Character | undefined>)));
    if (helperCharacters.some(helper => !helper || !CampaignPartyService.belongs(campaign, helper) || helper.status === 'dead' || !CharacterActionRulesEngine.canAct(helper))) {
      throw new Error('Cada ajudante precisa estar presente, vivo e capaz de agir.');
    }
    const legacyHelpBonus = helperIds.length ? 0 : Math.max(0, Math.min(3, Math.trunc(Number(input.helpBonus || 0))));
    const rng = input.rng || Math.random;
    const gear = Math.max(0, Math.min(10, Math.trunc(Number(input.equipmentBonus || 0))));
    const baseWits = Number(character.attributes.wits.current || 0);
    const baseAgility = Number(character.attributes.agility.current || 0);
    const survival = normalizedSkill(character, ['Survival', 'Sobrevivência']);
    const marksmanship = normalizedSkill(character, ['Marksmanship', 'Pontaria']);
    const sceneId = `journey:${campaign.id}:${campaign.currentDay || 1}:${campaign.currentPeriod || 'Morning'}`;
    const rolls = [];
    let eventType: JourneyEvent['type'] = 'activity';
    let title = '';
    let detail = '';
    let gmDetail: string | undefined;
    let status: JourneyEvent['status'] = 'resolved';
    let result: Record<string, unknown> = {};
    let rollSuccesses: number | undefined;
    let secondarySuccesses: number | undefined;
    let sleepCountsAsRest = true;
    let advancesPeriod = input.activity !== 'leadWay' && input.activity !== 'keepWatch';

    const rollSurvival = (modifier = 0, useGear = 0, suffix:string = input.activity) => {
      const roll = ForbiddenLandsDiceEngine.roll({ base: baseWits, skill: survival, gear: useGear, modifier: modifier + legacyHelpBonus }, { characterId: character.id, sceneId, checkId: `journey:${suffix}:${campaign.id}:${character.id}:${now}`, rng, criticalInjuries: character.criticalInjuryStates, skillName: 'survival', attributeName: 'wits', assistance: { helperIds } });
      rolls.push(roll);
      return roll;
    };

    if (input.activity === 'leadWay') {
      const rule = JourneyRulesEngine.leadWayModifier({ dark: input.dark, returningToVisitedHex: false });
      const modifier = rule.modifier + (dailyWeather?.leadWayModifier || 0) + (bitterWeather?.leadTheWayModifier || 0);
      const roll = rollSurvival(modifier, gear, 'lead-way');
      rollSuccesses = roll.successes;
      title = 'Desbravar';
      if (roll.successes > 0) detail = `${character.name} desbravou com ${roll.successes} sucesso(s)${input.dark ? ' no escuro' : ''}.`;
      else {
        eventType = 'mishap'; status = 'open';
        const mishap = JourneyRulesEngine.mishap('leadWay', rng);
        detail = `${character.name} falhou ao Desbravar: D66 ${mishap.roll} — ${mishap.title}. ${mishap.effect}`;
        gmDetail = `${mishap.title}: resultado mecânico ${JSON.stringify(mishap.mechanic)}. Verificar se há escolhas ou rolagens pendentes antes de encerrar.`;
        result = { activity:'leadWay', successes:0, modifier:modifier + legacyHelpBonus + helperIds.length, weatherModifier:(dailyWeather?.leadWayModifier || 0) + (bitterWeather?.leadTheWayModifier || 0), bitterReachWeatherModifier:bitterWeather?.leadTheWayModifier || 0, assistedBy:helperIds, dark:Boolean(input.dark), mishapRoll:mishap.roll, mishapId:`journey.lead_way.d66.${mishap.roll}`, mishap:mishap.title, mechanic:mishap.mechanic, sourcePage:mishap.sourcePage };
      }
      if (roll.successes > 0) result = { activity:'leadWay', actorId:input.characterId, successes:roll.successes, modifier:modifier + legacyHelpBonus + helperIds.length, weatherModifier:(dailyWeather?.leadWayModifier || 0) + (bitterWeather?.leadTheWayModifier || 0), bitterReachWeatherModifier:bitterWeather?.leadTheWayModifier || 0, assistedBy:helperIds, dark:Boolean(input.dark) };
      else result = { ...result, actorId: input.characterId };
    } else if (input.activity === 'keepWatch') {
      title = 'Manter Vigília';
      detail = `${character.name} está de vigia. A rolagem de PATRULHA só será exigida quando houver encontro/ameaça; não foi criada uma rolagem artificial agora.`;
      status = 'open';
      result = { activity:'keepWatch', actorId:character.id, watchDay:campaign.currentDay||1, watchPeriod:campaign.currentPeriod||'Morning', reactiveCheck:'SCOUTING/PATRULHA' };
    } else if (input.activity === 'hike') {
      const isMounted = Boolean(readyMountFor(worldBefore.journey.mounts, character.id));
      const walkingToday = worldBefore.journey.walkingDay === (campaign.currentDay || 1) ? Number(worldBefore.journey.walkingQuartersToday || 0) : 0;
      const failedMarches = worldBefore.journey.walkingDay === (campaign.currentDay || 1) ? Number(worldBefore.journey.forcedMarchFailuresToday || 0) : 0;
      const totalHikesToday = walkingToday + failedMarches;

      if (totalHikesToday >= 4) {
        throw new Error('O grupo já caminhou quatro Quartos de Dia hoje. Descanse ou durma até o próximo dia para voltar a caminhar.');
      }

      const forcedMarchRule = JourneyRulesEngine.forcedMarch(totalHikesToday, isMounted);
      const bitterKey=`${campaign.currentDay||1}:${campaign.currentPeriod||'Morning'}`;
      if(bitterWeather&&(bitterWeather.hikeEnduranceRequired||bitterWeather.hikeEnduranceRequiredSnow)){
        if(worldBefore.journey.bitterReachTravelCheckQuarterKey!==bitterKey)throw new Error('Resolva os testes de marcha do clima do Alcance Amargo antes de caminhar.');
        if(worldBefore.journey.bitterReachTravelStoppedQuarterKey===bitterKey)throw new Error('A tempestade/nevasca do Alcance Amargo interrompeu a marcha neste Quarto de Dia.');
      }
      const dailyWeatherMarchMod = (dailyWeather?.marchModifier || 0) + (bitterWeather?.hikeCombinedEnduranceModifier || 0);

      if (totalHikesToday < 2) {
        rollSuccesses = 1;
        title = isMounted ? 'Cavalgar' : 'Caminhar';
        detail = isMounted
          ? `${character.name} cavalgou durante este Quarto de Dia (${totalHikesToday + 1}º QD de deslocamento hoje).`
          : `${character.name} caminhou durante este Quarto de Dia (${totalHikesToday + 1}º QD de caminhada hoje).`;
        result = {
          activity: 'hike',
          walkingQuartersToday: walkingToday + 1,
          forcedMarch: false,
          mounted: isMounted,
          successes: 1,
        };
      } else {
        title = isMounted ? 'Marcha forçada a cavalo' : 'Marcha forçada';
        const mount = readyMountFor(worldBefore.journey.mounts, character.id);
        const requiredSkill = isMounted ? 'animal handling' : 'endurance';
        const skillRank = normalizedSkill(character, isMounted ? ['Animal Handling', 'Adestramento'] : ['Endurance', 'Resiliência']);
        const baseAttr = isMounted ? (mount?.strength ?? Number(character.attributes.strength.current || 0)) : Number(character.attributes.strength.current || 0);
        const totalModifier = forcedMarchRule.penalty + dailyWeatherMarchMod + legacyHelpBonus;

        const roll = ForbiddenLandsDiceEngine.roll(
          { base: baseAttr, skill: skillRank, gear, modifier: totalModifier },
          {
            characterId: character.id,
            sceneId,
            checkId: `journey:hike:${campaign.id}:${character.id}:${now}`,
            rng,
            criticalInjuries: character.criticalInjuryStates,
            skillName: isMounted ? 'animal_handling' : 'endurance',
            attributeName: 'strength',
            assistance: { helperIds },
          }
        );
        rolls.push(roll);
        rollSuccesses = roll.successes;

        if (roll.successes > 0) {
          detail = isMounted
            ? `${character.name} conduziu a montaria com sucesso na marcha forçada (${roll.successes} sucesso(s)${totalModifier ? ` mod ${totalModifier}` : ''}).`
            : `${character.name} suportou a marcha forçada (${roll.successes} sucesso(s)${totalModifier ? ` mod ${totalModifier}` : ''}).`;
          result = {
            activity: 'hike',
            walkingQuartersToday: walkingToday + 1,
            forcedMarch: true,
            successes: roll.successes,
            mounted: isMounted,
            penalty: forcedMarchRule.penalty,
            weatherModifier: dailyWeatherMarchMod,
          };
        } else {
          eventType = 'mishap';
          status = 'open';
          detail = isMounted
            ? `${character.name} falhou na marcha forçada a cavalo: a montaria ficou manca e não pode continuar montada hoje.`
            : `${character.name} falhou na marcha forçada: sofreu 1 dano de Agilidade e não caminha neste Quarto de Dia; deve descansar ou dormir.`;
          result = {
            activity: 'hike',
            walkingQuartersToday: walkingToday,
            forcedMarch: true,
            successes: 0,
            failure: true,
            mounted: isMounted,
            penalty: forcedMarchRule.penalty,
            weatherModifier: dailyWeatherMarchMod,
            agilityDamage: isMounted ? 0 : 1,
            failedMountId: isMounted ? mount?.id : undefined,
            mechanic: {
              kind: isMounted ? 'lame_mount' : 'forced_march_failure',
              failedMountId: isMounted ? mount?.id : undefined,
              agilityDamage: isMounted ? 0 : 1,
            },
          };
        }

        if (totalHikesToday === 3 && !isMounted) {
          result.automaticSleepy = true;
          detail += ' A 4ª caminhada do dia impõe a condição Sonolento automaticamente.';
        }
      }
    } else if (input.activity === 'forageFood' || input.activity === 'forageWater') {
      let modifier = bitterTerrain ? bitterTerrain.foragingModifier : JourneyRulesEngine.forageModifier(terrainKey, season);
      if (modifier === null) throw new Error('Este terreno não permite Coletar pelas regras configuradas.');
      const roll = rollSurvival(modifier, gear, input.activity);
      rollSuccesses = roll.successes;
      const food = input.activity === 'forageFood';
      title = food ? 'Coletar comida' : 'Coletar água';
      if (roll.successes > 0) {
        detail = food
          ? `${character.name} encontrou ${roll.successes} unidade(s) de Vegetais. Elas ficam registradas como rendimento da atividade e precisam ser consumidas/cozidas conforme a regra.`
          : `${character.name} encontrou água potável suficiente para o grupo encher os cantis até D12.`;
        result = food ? { activity:'forageFood', vegetables:roll.successes, successes:roll.successes } : { activity:'forageWater', waterRefill:'d12', successes:roll.successes };
      } else {
        const mishap = JourneyRulesEngine.mishap('forage', rng);
        eventType = 'mishap'; status = 'open';
        detail = `${character.name} falhou ao Coletar: D6 ${mishap.roll} — ${mishap.title}. ${mishap.effect}`;
        result = { activity:input.activity, successes:0, mishapRoll:mishap.roll, mishap:mishap.title, mechanic:mishap.mechanic, sourcePage:mishap.sourcePage };
      }
    } else if (input.activity === 'hunt') {
      let modifier = bitterTerrain ? bitterTerrain.huntingModifier : JourneyRulesEngine.huntModifier(terrainKey);
      if (modifier === null) throw new Error('Este terreno não permite Caçar pelas regras configuradas.');
      const search = rollSurvival(Number(modifier || 0), 0, 'hunt-search');
      rollSuccesses = search.successes;
      title = 'Caçar';
      if (search.successes <= 0) {
        const mishap = JourneyRulesEngine.mishap('hunt', rng);
        eventType = 'mishap'; status = 'open';
        detail = `${character.name} não encontrou uma presa e sofreu infortúnio: D6 ${mishap.roll} — ${mishap.title}. ${mishap.effect}`;
        result = { activity:'hunt', successes:0, mishapRoll:mishap.roll, mishap:mishap.title, mechanic:mishap.mechanic, sourcePage:mishap.sourcePage };
      } else {
        const preyRoll = JourneyRulesEngine.d6(rng);
        const prey = JourneyRulesEngine.huntPrey(preyRoll);
        const method = input.huntMethod || 'weapon';
        const huntHelpBonus=helperIds.length+legacyHelpBonus;
        if(search.successes>1){
          // Cada sucesso além do primeiro permite rerrolar a presa. A escolha pertence à jogadora;
          // não é correto aceitar automaticamente o primeiro resultado.
          status='open';
          detail=`${character.name} encontrou ${prey.animal} (D6 ${preyRoll}). Há ${search.successes-1} rerrolagem(ns) opcional(is) de presa antes do teste para abatê-la.`;
          result={activity:'hunt',actorId:character.id,prey:prey.animal,preyRoll,searchSuccesses:search.successes,remainingPreyRerolls:search.successes-1,preyRollHistory:[preyRoll],method,huntEquipmentBonus:gear,huntHelperIds:helperIds,huntHelpBonus,mechanic:{kind:'hunt_prey_choice'}};
        }else if(method==='trap'&&prey.requirement==='Arma'){
          detail=`${character.name} encontrou ${prey.animal} (D6 ${preyRoll}), mas essa presa exige uma arma e não pode ser abatida pela armadilha usada. Nenhuma Carne ou Pele foi obtida.`;
          result={activity:'hunt',prey:prey.animal,preyRoll,searchSuccesses:search.successes,killSuccesses:0,meat:0,pelts:0,method,incompatibleTrap:true};
        }else{
          const kill = method === 'weapon'
            ? ForbiddenLandsDiceEngine.roll({ base:baseAgility, skill:marksmanship, gear, modifier:prey.difficulty+huntHelpBonus }, { characterId:character.id, sceneId, checkId:`journey:hunt-kill:${campaign.id}:${character.id}:${now}`, rng, criticalInjuries:character.criticalInjuryStates, skillName:'marksmanship', attributeName:'agility', assistance:{helperIds} })
            : ForbiddenLandsDiceEngine.roll({ base:baseWits, skill:survival, gear, modifier:prey.difficulty+huntHelpBonus }, { characterId:character.id, sceneId, checkId:`journey:hunt-trap:${campaign.id}:${character.id}:${now}`, rng, criticalInjuries:character.criticalInjuryStates, skillName:'survival', attributeName:'wits', assistance:{helperIds} });
          rolls.push(kill); secondarySuccesses = kill.successes;
          if (kill.successes > 0) {
            detail = `${character.name} encontrou ${prey.animal} (D6 ${preyRoll}) e abateu a presa: ${prey.meat} Carne, ${prey.pelts} Pele(s).`;
            result = { activity:'hunt', prey:prey.animal, preyRoll, meat:prey.meat, pelts:prey.pelts, searchSuccesses:search.successes, killSuccesses:kill.successes, method, assistedBy:helperIds };
          } else if ('attacksOnFailure' in prey && prey.attacksOnFailure) {
            eventType = 'mishap'; status = 'open';
            title = 'Javali ataca';
            detail = `${character.name} encontrou ${prey.animal}, mas falhou em abatê-lo. O javali ataca imediatamente.`;
            result = { activity:'hunt', prey:prey.animal, preyRoll, searchSuccesses:search.successes, killSuccesses:0, boarAttacks:true, method, mechanic:{kind:'creature_encounter',selectionTable:'official_animals',forcedAnimalId:'boar'} };
          } else {
            detail = `${character.name} encontrou ${prey.animal}, mas não conseguiu abatê-lo. Nenhuma Carne ou Pele foi obtida.`;
            result = { activity:'hunt', prey:prey.animal, preyRoll, searchSuccesses:search.successes, killSuccesses:0, meat:0, pelts:0, method };
          }
        }
      }
    } else if (input.activity === 'fish') {
      const fishingMethod=input.fishingMethod||'rod';
      const fishingCandidates=(character.gear||[]).map((item:any,index:number)=>({item,index,name:String(item?.name||item?.label||'')})).filter(entry=>!entry.item?.broken&&/anzol|linha|rede|pesca|fishing|hook|net/i.test(entry.name));
      const fishingItem=fishingCandidates.find(entry=>fishingMethod==='net'?/rede|net/i.test(entry.name):/anzol|linha|hook/i.test(entry.name));
      if(!fishingItem)throw new Error(fishingMethod==='net'?'PESCAR com rede exige uma Rede de Pesca funcional na ficha.':'PESCAR exige Anzol e Linha funcional na ficha.');
      const officialFishingBonus=fishingMethod==='net'?2:1;
      const fishingGearBonus=Math.max(0,Math.min(officialFishingBonus,Number((fishingItem.item as any)?.currentBonus ?? (fishingItem.item as any)?.bonus ?? officialFishingBonus)||0));
      if(fishingGearBonus<1)throw new Error(`${fishingItem.name} está quebrado e precisa ser reparado ou substituído antes de PESCAR.`);
      const roll = rollSurvival(0, fishingGearBonus, 'fish');
      rollSuccesses = roll.successes;
      title = 'Pescar';
      if (roll.successes > 0) {
        const fish = JourneyRulesEngine.fishYield(roll.successes);
        detail = `${character.name} pescou ${fish} unidade(s) de Peixe.`;
        result = { activity:'fish', fish, successes:roll.successes, fishingMethod, fishingGearIndex:fishingItem.index, fishingGearName:fishingItem.name, equipmentBonus:fishingGearBonus };
      } else {
        let mishap = JourneyRulesEngine.mishap('fish', rng);
        const rerolled: number[]=[];
        while(mishap.mechanic.kind==='hook_in_finger'&&input.fishingMethod==='net'&&rerolled.length<12){
          rerolled.push(mishap.roll);
          mishap=JourneyRulesEngine.mishap('fish',rng);
        }
        eventType = 'mishap'; status = 'open';
        detail = `${character.name} falhou ao Pescar: D6 ${mishap.roll} — ${mishap.title}. ${mishap.effect}${rerolled.length?` A rede permitiu repetir ${rerolled.length} resultado(s) de Anzol no Dedo.`:''}`;
        title=mishap.title;
        result = { activity:'fish', successes:0, mishapRoll:mishap.roll, rerolledHookResults:rerolled, fishingMethod, fishingGearIndex:fishingItem.index, fishingGearName:fishingItem.name, equipmentBonus:fishingGearBonus, mishap:mishap.title, mechanic:mishap.mechanic, sourcePage:mishap.sourcePage };
      }
    } else if (input.activity === 'makeCamp') {
      const weatherModifier = (dailyWeather?.makeCampModifier || 0) + (bitterWeather?.makeCampModifier || 0);
      const roll = rollSurvival(weatherModifier, gear, 'make-camp');
      rollSuccesses = roll.successes;
      const camp = JourneyRulesEngine.makeCamp(roll.successes);
      title = camp.success ? 'Acampamento preparado' : 'Acampamento preparado com infortúnio';
      if (camp.success) {
        detail = `${character.name} encontrou e preparou um local protegido. O grupo pode DESCANSAR e DORMIR ali.`;
        result = { activity:'makeCamp', successes:roll.successes, modifier:weatherModifier + legacyHelpBonus + helperIds.length, weatherModifier, canRest:true, canSleep:true };
      } else {
        const mishap = JourneyRulesEngine.mishap('makeCamp', rng);
        eventType = 'mishap'; status = 'open';
        detail = `${character.name} montou o acampamento, mas a rolagem falhou. Há um infortúnio oculto pendente para o Mestre.`;
        gmDetail = `D66 ${mishap.roll} — ${mishap.title}. ${mishap.effect}`;
        result = { activity:'makeCamp', successes:0, modifier:weatherModifier + legacyHelpBonus + helperIds.length, weatherModifier, hiddenMishap:true, mishapRoll:mishap.roll, mishap:mishap.title, mechanic:mishap.mechanic, sourcePage:mishap.sourcePage, canRest:true, canSleep:true };
      }
    } else if (input.activity === 'rest') {
      title = 'Descansar'; detail = `${character.name} dedicou o Quarto de Dia ao DESCANSO. Se não houver interrupção, recupera todos os pontos de atributo permitidos pelas condições atuais.`;
      result = { activity:'rest', recovery:'all_eligible_attribute_points' };
    } else {
      title = 'Dormir';
      // Bloodmarch beta p.34: a victim struck by magical Nightmares is
      // sleepless for the following day. Checking ordinary horror injuries
      // must not override this veto or award XP/recovery for the quarter.
      const magicalNightmares=(character.statusEffects||[]).some(effect=>
        effect.id==='magic:bm-nightmares'&&Number(effect.remainingQuarters)>0);
      if(magicalNightmares){
        sleepCountsAsRest=false;
        detail=`${character.name} continua sob Pesadelos mágicos: este Quarto de Dia não conta como sono nem recupera atributos.`;
        result={activity:'sleep',magicNightmares:true,sleepCounts:false,recovery:'none',clearsSleepDeprivation:false};
      }
      const nightmares = (character.criticalInjuryStates || []).filter(state => state.status !== 'healed' && state.status !== 'fatal' && (state.effects || []).some(effect => effect.kind === 'sleep_check' && effect.skill === 'insight' && effect.failure === 'sleep_does_not_count'));
      if (!magicalNightmares && nightmares.length) {
        const insight = normalizedSkill(character, ['Insight', 'Discernimento']);
        const check = ForbiddenLandsDiceEngine.roll({ base: baseWits, skill: insight, gear: 0 }, {
          characterId: character.id, sceneId, checkId: `horror-nightmares:${campaign.id}:${character.id}:${now}`, rng,
          criticalInjuries: character.criticalInjuryStates, skillName: 'insight', attributeName: 'wits',
        });
        rolls.push(check);
        rollSuccesses = check.successes;
        sleepCountsAsRest = check.successes > 0;
        detail = sleepCountsAsRest
          ? `${character.name} superou os pesadelos no teste de DISCERNIMENTO (${check.successes} sucesso(s)); o SONO conta normalmente.`
          : `${character.name} falhou no teste de DISCERNIMENTO contra os pesadelos; este Quarto de Dia de SONO não conta e não recupera atributos nem encerra a privação de sono.`;
        result = { activity:'sleep', nightmareCheck:true, rollId:check.id, successes:check.successes, sleepCounts:sleepCountsAsRest, recovery:sleepCountsAsRest?'all_eligible_attribute_points':'none', clearsSleepDeprivation:sleepCountsAsRest, nightmareInjuryIds:nightmares.map(state=>state.id) };
      } else if(!magicalNightmares) {
        detail = `${character.name} dedicou o Quarto de Dia ao SONO e ao DESCANSO. Se não houver interrupção, recupera atributos permitidos e encerra a privação de sono.`;
        result = { activity:'sleep', recovery:'all_eligible_attribute_points', clearsSleepDeprivation:true };
      }
    }

    const automaticMechanic=result.mechanic as Record<string,unknown>|undefined;
    // Desbravar é feito antes do deslocamento. Um infortúnio sem progresso
    // consome o Quarto de Dia aqui, sem descobrir ou visitar outro hexágono.
    if(input.activity==='leadWay'&&automaticMechanic?.noMapProgress===true){
      advancesPeriod=true;
      result={...result,noMapProgressApplied:true};
      detail+=' O Quarto de Dia foi consumido sem avanço no mapa.';
    }
    if(eventType==='mishap')result={...result,actorId:input.characterId};
    if(eventType==='mishap'&&automaticMechanic&&automaticMechanic.sleepDenied!==true&&['resource_degrade','attribute_damage','animal_attack','critical_injury','hook_in_finger','group_attack','group_attack_on_failed_check','fall_into_water'].includes(String(automaticMechanic.kind))){
      status='resolved';
      detail += ' Consequência mecânica aplicada e persistida.';
    }
    if(helperIds.length){
      result={...result,assistedBy:helperIds};
      detail+=` Ajuda de ${helperCharacters.filter(Boolean).map(helper=>helper!.name).join(', ')} aplicada como +${helperIds.length} dado(s) de Perícia.`;
    }
    const activityEvent: JourneyEvent = {
      id:crypto.randomUUID(), type:eventType, title, detail, gmDetail, result, ruleSource:JOURNEY_RULE_SOURCE,
      day:campaign.currentDay || 1, period:campaign.currentPeriod || 'Morning', hexId:campaign.currentHexId || undefined,
      status, createdAt:now,
    };

    const transactionStores: any[] = ['campaign','app_meta','world_state','characters','rolls','map_discovery','individual_hexes','npcs'];
    const persisted = await withTransaction(transactionStores as any, 'readwrite', async tx => {
      const campaignStore = tx.objectStore('campaign');
      const worldStore = tx.objectStore('world_state');
      let currentWorld = WorldStateService.normalize(await worldStore.get(campaign.id), campaign.id);
      if (currentWorld.encounter?.status === 'active') throw new Error('Um encontro começou antes da atividade ser confirmada; resolva-o primeiro.');
      const characterStore=tx.objectStore('characters');
      let confirmedActor=await characterStore.get(input.characterId);
      if(!confirmedActor)throw new Error('Personagem não encontrado.');
      const confirmedCampaign=CampaignService.normalize(await campaignStore.get(campaign.id) || campaign);
      CampaignPartyService.assertMember(confirmedCampaign,confirmedActor);
      for (const helperId of helperIds) {
        const helper = await characterStore.get(helperId);
        if (!helper || !CampaignPartyService.belongs(confirmedCampaign,helper) || helper.status === 'dead' || !CharacterActionRulesEngine.canAct(helper)) throw new Error('Uma pessoa ajudante deixou de estar no grupo ou capaz de agir antes da confirmação da atividade.');
      }
      const confirmedSleepDenial=input.activity==='sleep'?campSleepDenialReason(currentWorld,confirmedCampaign):null;
      if(confirmedSleepDenial)throw new Error(confirmedSleepDenial);
      if(input.activity==='sleep'&&confirmedActor.liceState)throw new Error('Piolhos impedem dormir até serem removidos com Cura.');
      if(input.activity==='sleep'&&(confirmedActor.magicMishapState?.insomniaQuartersRemaining||0)>0)throw new Error('O infortúnio mágico de insônia impede dormir até o relógio de dias terminar.');
      const confirmedNocturnal=(confirmedActor.criticalInjuryStates||[]).some(state=>state.status!=='healed'&&state.status!=='fatal'&&(state.effects||[]).some(effect=>effect.kind==='sleep_restriction'&&effect.onlyDuringDaylight===true));
      if(input.activity==='sleep'&&confirmedNocturnal&&!['Morning','Day'].includes(confirmedCampaign.currentPeriod||'Morning'))throw new Error('O ferimento Noturna permite dormir somente durante o dia (manhã ou dia).');
      if(input.activity==='sleep'){
        if(confirmedActor.status==='dead')CharacterActionRulesEngine.assertCanAct(confirmedActor);
      }else CharacterActionRulesEngine.assertCanAct(confirmedActor);

      // Rendimentos oficiais de Coletar/Caçar/Pescar viram estado persistente e utilizável.
      // Vegetais, Carne e Peixe só podem ser consumidos no mesmo dia; Peles não estragam.
      let provisionedActor=confirmedActor as Character;
      if(input.activity==='forageFood'&&Number(activityEvent.result?.vegetables)>0){
        provisionedActor=appendJourneyProvision(provisionedActor,'vegetables',Number(activityEvent.result?.vegetables),confirmedCampaign.currentDay||1,activityEvent.id);
      }
      if(input.activity==='hunt'){
        if(Number(activityEvent.result?.meat)>0)provisionedActor=appendJourneyProvision(provisionedActor,'meat',Number(activityEvent.result?.meat),confirmedCampaign.currentDay||1,activityEvent.id);
        if(Number(activityEvent.result?.pelts)>0)provisionedActor=appendJourneyProvision(provisionedActor,'pelts',Number(activityEvent.result?.pelts),confirmedCampaign.currentDay||1,activityEvent.id);
      }
      if(input.activity==='fish'&&Number(activityEvent.result?.fish)>0){
        provisionedActor=appendJourneyProvision(provisionedActor,'fish',Number(activityEvent.result?.fish),confirmedCampaign.currentDay||1,activityEvent.id);
      }
      if(provisionedActor!==confirmedActor)await characterStore.put(provisionedActor);

      if (input.activity === 'hike') {
        const isHikeMounted = Boolean(readyMountFor(currentWorld.journey.mounts, confirmedActor.id));
        if (result.failure) {
          if (result.failedMountId) {
            currentWorld = {
              ...currentWorld,
              journey: {
                ...currentWorld.journey,
                mounts: (currentWorld.journey.mounts || []).map(m => m.id === result.failedMountId ? { ...m, status: 'lame' as const, lameDay: confirmedCampaign.currentDay || 1 } : m),
                forcedMarchFailuresToday: (currentWorld.journey.forcedMarchFailuresToday || 0) + 1,
              },
            };
          } else if (result.agilityDamage) {
            const agility = Math.max(0, confirmedActor.attributes.agility.current - 1);
            const broken = agility === 0 ? 'agility' : confirmedActor.brokenAttribute;
            const conditions = agility === 0 ? Array.from(new Set([...(confirmedActor.conditions || []), 'Broken' as const])) : confirmedActor.conditions;
            confirmedActor = {
              ...confirmedActor,
              attributes: { ...confirmedActor.attributes, agility: { ...confirmedActor.attributes.agility, current: agility } },
              conditions,
              ...(agility === 0 ? { brokenAttribute: 'agility' as const } : {}),
            };
            await characterStore.put(confirmedActor);
            currentWorld = {
              ...currentWorld,
              journey: {
                ...currentWorld.journey,
                forcedMarchFailuresToday: (currentWorld.journey.forcedMarchFailuresToday || 0) + 1,
              },
            };
          }
          activityEvent.status = 'resolved';
        } else {
          const currentWalking = currentWorld.journey.walkingDay === (confirmedCampaign.currentDay || 1) ? Number(currentWorld.journey.walkingQuartersToday || 0) : 0;
          currentWorld = {
            ...currentWorld,
            journey: {
              ...currentWorld.journey,
              active: true,
              walkingDay: confirmedCampaign.currentDay || 1,
              walkingQuartersToday: Math.min(4, currentWalking + 1),
            },
          };
          if (result.automaticSleepy) {
            const conditions = Array.from(new Set([...(confirmedActor.conditions || []), 'Sleep Deprived' as const]));
            confirmedActor = {
              ...confirmedActor,
              conditions,
              survivalState: { ...(confirmedActor.survivalState || { daysWithoutSleep: 0 }), mustSleep: true },
            };
            await characterStore.put(confirmedActor);
          }

          if (currentWorld.journey.plannedRouteHexIds && currentWorld.journey.plannedRouteHexIds.length > 0) {
            const nextHexId = currentWorld.journey.plannedRouteHexIds[0];
            const nextHex = await tx.objectStore('individual_hexes').get(nextHexId);
            if (nextHex) {
              const moveCost = MapTravelService.movementCostForHex(nextHex, { mounted: isHikeMounted, waterborne: Boolean(currentWorld.journey.maritime?.active) });
              const hexPeriodCost = moveCost.periods || 1;
              const priorProgress = Math.max(0, Math.min(0.999999, Number(currentWorld.journey.quarterTravelProgress || 0)));
              const totalProg = priorProgress + 1;
              if (totalProg >= hexPeriodCost) {
                const leftover = Math.max(0, totalProg - hexPeriodCost);
                const nextRemainingRoute = currentWorld.journey.plannedRouteHexIds.slice(1);
                currentWorld = {
                  ...currentWorld,
                  journey: {
                    ...currentWorld.journey,
                    quarterTravelProgress: leftover,
                    plannedRouteHexIds: nextRemainingRoute,
                    distanceKm: (currentWorld.journey.distanceKm || 0) + 10,
                  },
                };
                const priorHexId=confirmedCampaign.currentHexId;
                confirmedCampaign.currentHexId = nextHex.id;
                const nextMovementClass=MapTravelService.movementClassForHex(nextHex);
                const movingNpcs=nextMovementClass==='water'
                  ?OfficialEncounterFollowerEngine.advanceWaterStowaways(await tx.objectStore('npcs').getAll(),confirmedCampaign.id,priorHexId,nextHex.id,now)
                  :OfficialEncounterFollowerEngine.advance(await tx.objectStore('npcs').getAll(),confirmedCampaign.id,priorHexId,nextHex.id,now);
                for(const follower of movingNpcs)
                  await tx.objectStore('npcs').put(follower);
                confirmedCampaign.discoveredHexIds = Array.from(new Set([...(confirmedCampaign.discoveredHexIds || []), nextHex.id]));
                const updatedHex = { ...nextHex, discovered: true, visited: true };
                await tx.objectStore('individual_hexes').put(updatedHex);
                activityEvent.detail += ` O grupo avançou para ${nextHex.coordinate}${nextHex.terrain ? ` (${nextHex.terrain})` : ''}.`;
                activityEvent.result = { ...activityEvent.result, reachedHexId: nextHex.id, reachedHexCoordinate: nextHex.coordinate, distanceKm: currentWorld.journey.distanceKm };
              } else {
                currentWorld = {
                  ...currentWorld,
                  journey: {
                    ...currentWorld.journey,
                    quarterTravelProgress: totalProg,
                  },
                };
                activityEvent.detail += ` O grupo progrediu em direção a ${nextHex.coordinate} (${Math.round((totalProg / hexPeriodCost) * 100)}% concluído).`;
              }
            }
          }
        }
      }

      for (const roll of rolls) await tx.objectStore('rolls').put(roll);
      const mechanic=result.mechanic as Record<string,any>|undefined;
      if(mechanic?.kind==='creature_encounter'&&mechanic.forcedAnimalId){
        const animal=JourneyRulesEngine.officialWildAnimal(String(mechanic.forcedAnimalId));
        if(!animal)throw new Error('Animal obrigatório do encontro não existe na tabela oficial.');
        const party=CampaignPartyService.members(confirmedCampaign, await characterStore.getAll()) as Character[];
        const enemy:EncounterCombatant={
          id:`journey-animal:${crypto.randomUUID()}`,name:animal.name,kind:'npc',side:'enemy',initiative:1+Math.floor(Math.max(0,Math.min(.999999,rng()))*10),
          currentStrength:animal.strength,maxStrength:animal.strength,armor:0,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',
          notes:[`Animal oficial do Guia do Mestre. Movimento ${animal.movement}.`,animal.notes||''].filter(Boolean).join(' '),x:.82,y:.5,size:1,
          quickSheet:{category:'animal',attributes:{strength:animal.strength,agility:animal.agility},skills:animal.skills,movement:animal.movement,weapons:[{name:animal.weapon,damage:animal.damage,damageType:animal.damageType}],source:'Guia do Mestre, pp. 126–127 — Animais'},
        };
        const encounter=EncounterService.createEncounter(`${animal.name} — ataque durante a Caça`,confirmedCampaign.currentHexId||undefined,party,[enemy]);
        const triggered=EncounterService.triggerWatchOnEncounter(currentWorld,encounter,party,confirmedCampaign.currentDay||1,confirmedCampaign.currentPeriod||'Morning',rng);
        for(const watchRoll of triggered.rolls)await tx.objectStore('rolls').put(watchRoll);
        currentWorld=triggered.world;
        activityEvent.status='resolved';
        activityEvent.result={...(activityEvent.result||{}),forcedAnimalId:animal.id,forcedAnimalName:animal.name,encounterId:encounter.id};
        activityEvent.detail+=` Encontro de combate com ${animal.name} iniciado automaticamente.`;
      }
      if(mechanic?.kind==='lost_equipment'&&mechanic.scope==='random_party_member'){
        const eligible=(CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())).filter(member=>member.status!=='dead'&&Boolean((member.weapons||[]).length||(member.gear||[]).length||(member.armor||[]).length));
        if(eligible.length){
          const selected=eligible[Math.min(eligible.length-1,Math.floor(Math.max(0,Math.min(0.999999,rng()))*eligible.length))];
          activityEvent.result={...(activityEvent.result||{}),actorId:selected.id,selectedCharacterName:selected.name};
          activityEvent.detail+=` Equipamento a escolher de ${selected.name}.`;
        }else{activityEvent.status='resolved';activityEvent.result={...(activityEvent.result||{}),noEligibleEquipment:true};activityEvent.detail+=' Ninguém possui equipamento elegível para perder.';}
      }
      if(mechanic?.kind==='gear_damaged'&&mechanic.scope==='random_party_member'){
        const eligible=(CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())).filter(member=>member.status!=='dead'&&Boolean((member.weapons||[]).length||(member.gear||[]).length||(member.armor||[]).length));
        if(eligible.length){
          const selected=eligible[Math.min(eligible.length-1,Math.floor(Math.max(0,Math.min(0.999999,rng()))*eligible.length))];
          activityEvent.result={...(activityEvent.result||{}),actorId:selected.id,selectedCharacterName:selected.name};
          activityEvent.detail+=` A MdJ deve escolher qual item de ${selected.name} quebrou e quem tentará o reparo.`;
        }else{
          activityEvent.status='resolved';
          activityEvent.result={...(activityEvent.result||{}),noEligibleEquipment:true};
          activityEvent.detail+=' Ninguém possui equipamento elegível para quebrar.';
        }
      }
      if(mechanic?.kind==='lost_equipment'&&mechanic.scope!=='random_party_member'){
        const owned=await characterStore.get(input.characterId);
        if(owned&&!(owned.weapons||[]).length&&!(owned.gear||[]).length){activityEvent.status='resolved';activityEvent.result={...(activityEvent.result||{}),noEligibleEquipment:true};activityEvent.detail+=' Nenhuma arma ou equipamento elegível para perder.';}
      }
      if(mechanic?.kind==='fishing_gear_damaged'){
        const owned=await characterStore.get(input.characterId) as Character|undefined;
        const gearIndex=Math.trunc(Number(activityEvent.result?.fishingGearIndex));
        if(owned&&Number.isInteger(gearIndex)&&gearIndex>=0&&gearIndex<owned.gear.length){
          const item:any=owned.gear[gearIndex];
          const itemName=String(item?.name||item?.label||`Equipamento ${gearIndex+1}`);
          const officialBonus=/rede|net/i.test(itemName)?2:1;
          const originalBonus=Math.max(officialBonus,Number(item?.originalBonus ?? item?.bonus ?? item?.equipmentBonus ?? 0)||0);
          await characterStore.put({...owned,gear:owned.gear.map((entry:any,index:number)=>index===gearIndex?{...entry,originalBonus,currentBonus:0,bonus:0,broken:true,journeyDamage:{kind:'fishing_gear_damaged',eventId:activityEvent.id}}:entry)});
          activityEvent.result={...(activityEvent.result||{}),gearIndex,itemName,broken:true,originalBonus};
          activityEvent.detail+=` ${itemName} foi marcado como quebrado e não pode ser usado para PESCAR até ser reparado ou substituído.`;
        }
      }
      if(mechanic?.kind==='stuck_fishing_gear'){
        const owned=await characterStore.get(input.characterId);
        if(owned&&!(owned.gear||[]).length){activityEvent.status='resolved';activityEvent.result={...(activityEvent.result||{}),noEligibleEquipment:true};activityEvent.detail+=' Nenhum equipamento de pesca cadastrado para perder.';}
      }
      if(mechanic?.kind==='lice'){
        const candidates=(CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())).filter(member=>member.status!=='dead'&&!member.liceState);
        if(candidates.length){
          const victim=candidates[Math.min(candidates.length-1,Math.floor(Math.max(0,Math.min(.999999,rng()))*candidates.length))];
          // O infortúnio acontece durante o QD de montagem do acampamento,
          // que avança os relógios mais adiante nesta mesma transação.
          await characterStore.put(LiceRulesEngine.infect(victim, advancesPeriod ? 1 : 0));
          activityEvent.result={...(activityEvent.result||{}),actorId:victim.id,victimName:victim.name};
          activityEvent.detail+=` ${victim.name} pegou piolhos; sofre 1 dano de Agilidade por dia e não pode dormir até Cura bem-sucedida.`;
        }else{activityEvent.status='resolved';activityEvent.result={...(activityEvent.result||{}),noEligibleVictim:true};activityEvent.detail+=' Nenhuma aventureira viva sem piolhos para receber o infortúnio.';}
      }
      if(mechanic?.coldCheck===true||mechanic?.kind==='cold_check'){
        const exposed=mechanic?.kind==='clothing_damaged'?[input.characterId]:(CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())).filter(member=>member.status!=='dead').map(member=>member.id);
        activityEvent.result={...(activityEvent.result||{}),pendingColdCheckCharacterIds:exposed};
      }
      if(mechanic?.kind==='fog'){
        const groupDamage=mechanic.groupDamage as Record<string,unknown>|undefined;
        const attribute=String(groupDamage?.attribute||'');
        const amount=Math.max(0,Math.trunc(Number(groupDamage?.amount)||0));
        if(['strength','agility','wits','empathy'].includes(attribute)&&amount>0){
          const key=attribute as keyof Character['attributes'];
          const outcomes:Array<{characterId:string;characterName:string;damage:number}>=[];
          for(const member of CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())){
            if(member.status==='dead')continue;
            const before=member.attributes[key].current;
            const current=Math.max(0,before-amount);
            const conditions=current===0?Array.from(new Set([...(member.conditions||[]),'Broken' as const])):member.conditions;
            await characterStore.put({...member,attributes:{...member.attributes,[key]:{...member.attributes[key],current}},conditions,...(current===0?{brokenAttribute:key}:{})});
            outcomes.push({characterId:member.id,characterName:member.name,damage:before-current});
          }
          activityEvent.result={...(activityEvent.result||{}),damageOutcomes:outcomes};
          activityEvent.detail+=` Neblina: ${outcomes.map(item=>`${item.characterName}: ${item.damage} dano(s) de Empatia`).join('; ')}.`;
        }
      }
      if(mechanic?.kind==='resource_degrade'){
        const members=CampaignPartyService.members(confirmedCampaign, await characterStore.getAll());
        for(const member of members){
          const key=mechanic.resource==='water'?'water':'food';
          const nextMember={...member,resourceDice:{...member.resourceDice,[key]:WorldStateService.stepResourceDie(member.resourceDice?.[key]||null,Math.max(1,Number(mechanic.steps)||1))}};
          await characterStore.put({...nextMember,encumbrance:EncumbranceRulesEngine.calculate(nextMember)});
        }
      }
      if(['attribute_damage','animal_attack','hook_in_finger','trap','leech_infestation'].includes(String(mechanic?.kind))){
        const damage=mechanic.kind==='attribute_damage'?mechanic:{kind:'attribute_damage',...(mechanic.damage||mechanic.initialDamage||{})};
        const attribute=damage.attribute;
        if(['strength','agility','wits','empathy'].includes(attribute)){
          const members=damage.scope==='party'?CampaignPartyService.members(confirmedCampaign, await characterStore.getAll()):[character];
          for(const member of members){
            const current=Math.max(0,member.attributes[attribute].current-Math.max(0,Math.trunc(Number(damage.amount)||0)));
            const broken=current===0?attribute:member.brokenAttribute;
            const conditions=current===0?Array.from(new Set([...(member.conditions||[]),'Broken' as const])):member.conditions;
            await characterStore.put({...member,brokenAttribute:broken,conditions,attributes:{...member.attributes,[attribute]:{...member.attributes[attribute],current}}});
          }
        }
      }
      if(mechanic?.kind==='group_attack'){
        const attack=mechanic.attack as Record<string,unknown>|undefined;
        const attribute=String(attack?.damageAttribute||'');
        if(['strength','agility','wits','empathy'].includes(attribute)){
          const damageAttribute=attribute as keyof Character['attributes'];
          const outcomes: Array<{characterId:string;characterName:string;rollId:string;successes:number;damage:number}>=[];
          for(const member of CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())){
            if(member.status==='dead')continue;
            const roll=ForbiddenLandsDiceEngine.roll({base:Math.max(0,Math.min(30,Math.trunc(Number(attack?.baseDice)||0))),skill:0,gear:0},{characterId:member.id,sceneId,checkId:`journey:group-attack:${activityEvent.id}:${member.id}`,rng});
            const before=member.attributes[damageAttribute].current;
            const current=Math.max(0,before-roll.successes);
            const conditions=current===0?Array.from(new Set([...(member.conditions||[]),'Broken' as const])):member.conditions;
            await characterStore.put({...member,attributes:{...member.attributes,[damageAttribute]:{...member.attributes[damageAttribute],current}},conditions,...(current===0?{brokenAttribute:damageAttribute}:{})});
            await tx.objectStore('rolls').put(roll);
            outcomes.push({characterId:member.id,characterName:member.name,rollId:roll.id,successes:roll.successes,damage:before-current});
          }
          activityEvent.result={...(activityEvent.result||{}),attackOutcomes:outcomes};
          activityEvent.detail+=` Ataque ao grupo resolvido: ${outcomes.map(item=>`${item.characterName}: ${item.damage} dano(s)`).join('; ')}.`;
        }
      }
      if(mechanic?.kind==='group_attack_on_failed_check'){
        const attack=mechanic.attack as Record<string,unknown>|undefined;
        const damageAttribute=(attack?.damageAttribute==='agility'?'agility':'strength') as keyof Character['attributes'];
        const outcomes: Array<{characterId:string;characterName:string;checkRollId:string;attackRollId?:string;escaped:boolean;damage:number;criticalInjuryId?:string}>=[];
        for(const member of CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())){
          if(member.status==='dead')continue;
          const movement=normalizedSkill(member,['Move','Movement','Movimentação']);
          const check=ForbiddenLandsDiceEngine.roll({base:member.attributes.agility.current,skill:movement,gear:0},{characterId:member.id,sceneId,checkId:`journey:mishap-escape:${activityEvent.id}:${member.id}`,rng,criticalInjuries:member.criticalInjuryStates,skillName:'movement',attributeName:'agility'});
          await tx.objectStore('rolls').put(check);
          const checkConsequence=CriticalInjuryRulesEngine.applyCheckConsequences(member,'movement');
          const checkedMember=checkConsequence.character as Character;
          if(checkConsequence.triggers.length)await characterStore.put(checkedMember);
          if(check.successes>0){outcomes.push({characterId:member.id,characterName:member.name,checkRollId:check.id,escaped:true,damage:0});continue;}
          const attackRoll=ForbiddenLandsDiceEngine.roll({base:Math.max(0,Math.min(30,Math.trunc(Number(attack?.baseDice)||0))),skill:0,gear:0},{characterId:member.id,sceneId,checkId:`journey:mishap-attack:${activityEvent.id}:${member.id}`,rng});
          await tx.objectStore('rolls').put(attackRoll);
          const weaponDamage=Math.max(1,Math.trunc(Number(attack?.weaponDamage)||1));
          const rawDamage=attackRoll.successes>0?weaponDamage+attackRoll.successes-1:0;
          const before=checkedMember.attributes[damageAttribute].current;
          const current=Math.max(0,before-rawDamage);
          const conditions=current===0?Array.from(new Set([...(checkedMember.conditions||[]),'Broken' as const])):checkedMember.conditions;
          let updated:Character={...checkedMember,attributes:{...checkedMember.attributes,[damageAttribute]:{...checkedMember.attributes[damageAttribute],current}},conditions,...(current===0?{brokenAttribute:damageAttribute}:{})};
          let criticalInjuryId:string|undefined;
          if(damageAttribute==='strength'&&before>0&&current===0){
            const d66=CriticalInjuryRulesEngine.rollWithResult('blunt',rng);
            const critical=criticalStateForJourney('blunt',d66.roll,rng,now);
            criticalInjuryId=critical.id;
            updated={...updated,criticalInjuryStates:[...(updated.criticalInjuryStates||[]),critical],criticalInjuries:[...(updated.criticalInjuries||[]),`blunt:${d66.roll}:${critical.name}`],...(critical.deathTimer?.immediate?{status:'dead' as const}:{})};
          }
          await characterStore.put(updated);
          outcomes.push({characterId:member.id,characterName:member.name,checkRollId:check.id,attackRollId:attackRoll.id,escaped:false,damage:before-current,...(criticalInjuryId?{criticalInjuryId}:{})});
        }
        activityEvent.result={...(activityEvent.result||{}),attackOutcomes:outcomes};
        activityEvent.detail+=` ${outcomes.filter(item=>!item.escaped).map(item=>`${item.characterName}: ${item.damage} dano(s)${item.criticalInjuryId?' e ferimento grave':''}`).join('; ')||'Todas escaparam do ataque.'}`;
      }
      if(mechanic?.kind==='group_attack_and_check'){
        const attack=mechanic.attack as Record<string,unknown>|undefined;
        const outcomes:Array<{characterId:string;characterName:string;attackRollId:string;movementRollId:string;damage:number;savedGear:boolean;criticalPending?:boolean}>=[];
        const pending:string[]=[];
        const pendingCritical:string[]=[];
        for(const member of CampaignPartyService.members(confirmedCampaign, await characterStore.getAll())){
          if(member.status==='dead')continue;
          const attackRoll=ForbiddenLandsDiceEngine.roll({base:Math.max(0,Math.min(30,Math.trunc(Number(attack?.baseDice)||5))),skill:0,gear:0},{characterId:member.id,sceneId,checkId:'journey:campfire-attack:'+activityEvent.id+':'+member.id,rng});
          const damage=attackRoll.successes>0?Math.max(1,Math.trunc(Number(attack?.weaponDamage)||1))+attackRoll.successes-1:0;
          await tx.objectStore('rolls').put(attackRoll);
          const movement=normalizedSkill(member,['Move','Movement','Movimentação']);
          const movementRoll=ForbiddenLandsDiceEngine.roll({base:member.attributes.agility.current,skill:movement,gear:0},{characterId:member.id,sceneId,checkId:'journey:campfire-save:'+activityEvent.id+':'+member.id,rng,criticalInjuries:member.criticalInjuryStates,skillName:'movement',attributeName:'agility'});
          await tx.objectStore('rolls').put(movementRoll);
          const movementConsequence=CriticalInjuryRulesEngine.applyCheckConsequences(member,'movement');
          const movedMember=movementConsequence.character as Character;
          const savedGear=movementRoll.successes>0;
          if(!savedGear&&Boolean((member.weapons||[]).length||(member.armor||[]).length||(member.gear||[]).length))pending.push(member.id);
          // Bloodmarch Fire's Friend protects the BODY in campfire incidents;
          // the Move roll for saving equipment must still happen normally.
          const bodyFireDamage=BloodmarchBetaFireGuard.incomingDamage(damage,movedMember.statusEffects);
          const effectiveStrength=Math.max(0,movedMember.attributes.strength.current-bodyFireDamage);
          const conditions=effectiveStrength===0?Array.from(new Set([...(movedMember.conditions||[]),'Broken' as const])):movedMember.conditions;
          const criticalPending=movedMember.attributes.strength.current>0&&effectiveStrength===0;
          if(criticalPending)pendingCritical.push(member.id);
          const marker='PENDENTE: ferimento grave — tabela aguardando escolha após Incêndio';
          await characterStore.put({...movedMember,attributes:{...movedMember.attributes,strength:{...movedMember.attributes.strength,current:effectiveStrength}},conditions,...(effectiveStrength===0?{brokenAttribute:'strength' as const}:{}),...(criticalPending?{criticalInjuries:[...(movedMember.criticalInjuries||[]),marker]}:{})});
          outcomes.push({characterId:member.id,characterName:member.name,attackRollId:attackRoll.id,movementRollId:movementRoll.id,damage:movedMember.attributes.strength.current-effectiveStrength,savedGear,...(criticalPending?{criticalPending:true}:{})});
        }
        activityEvent.result={...(activityEvent.result||{}),attackOutcomes:outcomes,pendingGearLossCharacterIds:pending,pendingCriticalInjuryCharacterIds:pendingCritical};
        activityEvent.status=pending.length||pendingCritical.length?'open':'resolved';
        activityEvent.detail+=' Incêndio: '+outcomes.map(item=>`${item.characterName} sofreu ${item.damage} dano(s)${item.criticalPending?'; Força Quebrada, escolha a tabela do ferimento grave':''} e ${item.savedGear?'salvou':'perdeu'} equipamento`).join('; ')+'.';
      }
      if(mechanic?.kind==='fall_into_water'){
        const current=await characterStore.get(input.characterId);
        if(current&&!current.drowningState){
          await characterStore.put(DrowningRulesEngine.enterWater(current,input.metalArmor===true));
          activityEvent.result={...(activityEvent.result||{}),drowningStarted:true,characterId:current.id,metalArmor:input.metalArmor===true};
          activityEvent.detail+=` ${current.name} caiu na água e precisa resolver os testes de natação e afogamento.`;
        }
      }
      if(mechanic?.kind==='critical_injury'&&['slash','blunt','puncture','horror'].includes(mechanic.table)){
        const state=criticalStateForJourney(mechanic.table,Number(mechanic.roll),rng,now);
        const states=[...(character.criticalInjuryStates||[]),state];
        await characterStore.put({...character,criticalInjuryStates:states,criticalInjuries:[...(character.criticalInjuries||[]),`${state.table}:${state.roll}:${state.name}`]});
      }

      if (input.activity === 'forageWater' && rollSuccesses && rollSuccesses > 0) {
        const all = CampaignPartyService.members(confirmedCampaign, await characterStore.getAll());
        for (const member of all) {
          const replenished={...member,resourceDice:{...member.resourceDice,water:'d12' as const}};
          await characterStore.put({...replenished,encumbrance:EncumbranceRulesEngine.calculate(replenished)});
        }
      }

      if (advancesPeriod) {
        const party = CampaignPartyService.members(confirmedCampaign, await characterStore.getAll());
        const nextCampaign = CampaignService.advanceState(confirmedCampaign, 1);
        for (const member of party) {
          let advanced = CharacterTimeRulesEngine.advanceQuarters(member, 1, {
            sleepingQuarters: input.activity === 'sleep' && sleepCountsAsRest && member.id === character.id ? 1 : 0,
          });
          // XP pode ser gasto dentro do QD recém-concluído de descanso/sono.
          // Qualquer outra passagem de tempo invalida uma janela anterior.
          const { xpSpendWindow: _expiredWindow, ...withoutExpiredWindow } = advanced;
          advanced = withoutExpiredWindow;
          if ((input.activity === 'rest' || (input.activity === 'sleep' && sleepCountsAsRest)) && member.id === character.id) {
            advanced = { ...advanced, xpSpendWindow: {
              campaignId: campaign.id,
              day: nextCampaign.currentDay || 1,
              period: nextCampaign.currentPeriod || 'Morning',
              activity: input.activity,
              openedAt: now,
            } };
          }
          if ((input.activity === 'rest' || (input.activity === 'sleep' && sleepCountsAsRest)) && member.id === character.id) {
            advanced = SurvivalRulesEngine.recoverAfterQuarter(advanced);
          }
          await characterStore.put(advanced);
        }
      }

      let nextCampaign = advancesPeriod ? { ...CampaignService.advanceState(confirmedCampaign, 1), updatedAt:now } : confirmedCampaign;
      const journeyEvents = currentWorld.journey.events.map(event => {
          if (event.status !== 'open') return event;
          const mechanic = event.result?.mechanic as Record<string, unknown> | undefined;
          if(event.type!=='mishap')return event;
          const newCampClears=input.activity==='makeCamp'&&((mechanic?.kind==='sleep_denied'&&mechanic.condition==='until_new_camp')||(mechanic?.kind==='attribute_damage'&&mechanic.sleepDenied===true));
          const nightEnded=mechanic?.kind==='storm'&&mechanic.sleepDenied===true&&(nextCampaign.currentDay||1)>event.day&&!((event.result?.pendingColdCheckCharacterIds as string[]|undefined)?.length);
          return newCampClears||nightEnded?{...event,status:'resolved' as const,detail:`${event.detail} Restrição de sono encerrada.`}:event;
        });
      const stationaryAlreadyRolled = currentWorld.journey.events.some(event =>
        event.day === (campaign.currentDay || 1) &&
        (event.result?.cadence === 'stationary' || (event.result?.mechanic as Record<string,unknown>|undefined)?.cadence === 'stationary')
      );
      const encounterCadence = input.activity === 'hike' ? 'walking' : 'stationary';
      const shouldRollEncounter = input.activity === 'hike' || !stationaryAlreadyRolled;
      // The selected activity terrain can be a coarse map classification. Random
      // encounter tables must follow the adjudicated final terrain stored on the
      // current hex, just like direct encounter rolls and map travel do.
      const encounterTerrain = currentHex
        ? JourneyRulesEngine.encounterTerrainForHex(currentHex)
        : terrainKey;
      let cadenceEncounter = advancesPeriod && shouldRollEncounter && Boolean(confirmedCampaign.currentHexId || campaign.currentHexId)
        ? this.buildOfficialRandomEncounterEventForTerrain(
            confirmedCampaign,
            confirmedCampaign.currentHexId || campaign.currentHexId || undefined,
            encounterTerrain,
            encounterCadence,
            rng,
            { day: confirmedCampaign.currentDay || campaign.currentDay || 1, period: confirmedCampaign.currentPeriod || campaign.currentPeriod || 'Morning' }
          )
        : null;
      if(cadenceEncounter){
        const recorded=this.recordOfficialEncounterSiteVisit(currentWorld,cadenceEncounter);
        currentWorld=recorded.world;cadenceEncounter=recorded.event;
        cadenceEncounter=this.applyAutomaticOfficialSceneAttack(cadenceEncounter,
          CampaignPartyService.members(confirmedCampaign,await characterStore.getAll() as Character[]),rng);
      }
      if (cadenceEncounter && cadenceEncounter.status === 'open' && input.activity === 'hike') {
        activityEvent.detail += ` Encontro de jornada iniciado: ${cadenceEncounter.title}. A rota foi pausada.`;
      }
      currentWorld = {
        ...currentWorld,
        journey:{
          ...currentWorld.journey,
          active: input.activity === 'makeCamp' ? false : (input.activity === 'hike' ? true : currentWorld.journey.active),
          lastCampDay: input.activity === 'makeCamp' ? campaign.currentDay || 1 : currentWorld.journey.lastCampDay,
          events:retainJourneyEvents([...journeyEvents, activityEvent, ...(cadenceEncounter?[cadenceEncounter]:[])]),
          updatedAt:now,
        },
        updatedAt:now,
      };
      if (advancesPeriod) currentWorld = WorldStateService.advanceTime(currentWorld, campaign, nextCampaign, 1, now);
      if (advancesPeriod) {
        await campaignStore.put(nextCampaign);
        await tx.objectStore('app_meta').put({ id:'activeCampaignId', value:nextCampaign.id, updatedAt:now });
      }
      // Bloodmarch beta p.36: resolve ONLY after this exact official sleep QD,
      // in the existing campaign+character+world transaction; never on mere time passage.
      if(input.activity==='sleep'&&sleepCountsAsRest&&advancesPeriod){
        const prophecies=BloodmarchBetaPropheticDreamsEngine.effects(currentWorld).filter(e=>
          e.status==='pending'&&e.casterId===confirmedActor.id&&e.sourceHexId===confirmedCampaign.currentHexId);
        for(const prophecy of prophecies){
          const target=await characterStore.get(prophecy.targetId) as Character|undefined;
          if(!target||target.status==='dead'||!CampaignPartyService.belongs(confirmedCampaign,target))continue;
          const finished=BloodmarchBetaPropheticDreamsEngine.finishSleep(currentWorld,confirmedActor,target,
            {campaign:confirmedCampaign,eventId:activityEvent.id,sleepCounts:sleepCountsAsRest},now);
          if(finished.completed.length){
            await characterStore.put(finished.target);
            currentWorld=finished.world;
            activityEvent.detail+=` Sonhos Proféticos concretizado: ${finished.completed.map(e=>e.kind==='heal-condition'?`cura de ${e.condition}`:`${e.kind}: ${e.itemName}`).join('; ')}.`;
          }
        }
      }
      // Bloodmarch beta p.36: atomic relocation only after a legitimate QD sleep.
      if(input.activity==='sleep'&&sleepCountsAsRest&&advancesPeriod){
        const pending=BloodmarchBetaDreamTravelEngine.effects(currentWorld).find(e=>
          e.status==='pending'&&e.casterId===confirmedActor.id);
        if(pending){
          const members=await Promise.all((confirmedCampaign.partyCharacterIds||[]).map((id:string)=>characterStore.get(id)));
          const travel=await BloodmarchBetaDreamTravelEngine.commitSleep(tx,{world:currentWorld,campaign:nextCampaign,
            actor:confirmedActor,members,eventId:activityEvent.id,sleepCounts:sleepCountsAsRest,
            companionsSleepingConfirmed:input.dreamTravelCompanionsSleepingConfirmed===true,now});
          if(travel?.completed&&travel.destinationHex){
            currentWorld=travel.world;nextCampaign=travel.campaign;
            const oldHex=await tx.objectStore('individual_hexes').get(travel.completed.fromHexId);
            const allHexes=await tx.objectStore('individual_hexes').getAll();
            const environmental=EnvironmentalDirectorService.evaluate({
              campaign:nextCampaign,currentHex:travel.destinationHex,previousHex:oldHex||undefined,
              worldState:currentWorld,allHexes,now});
            currentWorld={...currentWorld,environment:environmental.environment,updatedAt:now};
            activityEvent.detail+=` Viagem dos Sonhos concluída: grupo inteiro chegou ao hex ${nextCampaign.currentHexId}. Nenhuma distância de marcha foi somada.`;
            activityEvent.result={...activityEvent.result,dreamTravelEffectId:travel.completed.id,
              dreamTravelDestination:travel.completed.destinationHexId};
          }else activityEvent.detail+=' Viagem dos Sonhos ainda pendente: confirme os companheiros dormindo, destino visitado, grupo completo e ausência de animais.';
        }
      }
      await worldStore.put(currentWorld);
      return { world:currentWorld, campaign:nextCampaign, previousCampaign:campaign };
    });

    let finalWorld = persisted.world;
    if (advancesPeriod) {
      CampaignService.notify(persisted.campaign);
      finalWorld = await CampaignService.reconcileElapsedTime(persisted.previousCampaign, persisted.campaign, `Atividade de jornada: ${input.activity}`, persisted.world);
    }
    this.notify(finalWorld);
    return { event:activityEvent, rollSuccesses, secondarySuccesses, advancedPeriod:advancesPeriod };
  }

  static async rerollHuntPrey(campaignId:string,eventId:string,rng:()=>number=Math.random):Promise<{prey:string;preyRoll:number;remaining:number}> {
    const outcome=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const index=world.journey.events.findIndex(event=>event.id===eventId&&event.status==='open'&&(event.result?.mechanic as Record<string,unknown>|undefined)?.kind==='hunt_prey_choice');
      if(index<0)throw new Error('Escolha de presa pendente não encontrada.');
      const event=world.journey.events[index];
      const remaining=Math.max(0,Math.trunc(Number(event.result?.remainingPreyRerolls)||0));
      if(remaining<1)throw new Error('Não restam rerrolagens opcionais de presa.');
      const preyRoll=JourneyRulesEngine.d6(rng);
      const prey=JourneyRulesEngine.huntPrey(preyRoll);
      const history=[...((event.result?.preyRollHistory as number[]|undefined)||[]),preyRoll];
      const nextEvent:JourneyEvent={...event,detail:`${String(event.result?.actorId||'A caçadora')} rerrolou a presa: D6 ${preyRoll} — ${prey.animal}. Restam ${remaining-1} rerrolagem(ns) opcional(is).`,result:{...(event.result||{}),prey:prey.animal,preyRoll,preyRollHistory:history,remainingPreyRerolls:remaining-1}};
      const events=[...world.journey.events];events[index]=nextEvent;
      const next={...world,journey:{...world.journey,events,updatedAt:Date.now()},updatedAt:Date.now()};
      await store.put(next);
      return {prey:prey.animal,preyRoll,remaining:remaining-1,world:next};
    });
    this.notify(outcome.world);
    return {prey:outcome.prey,preyRoll:outcome.preyRoll,remaining:outcome.remaining};
  }

  static async resolveHuntPreyChoice(campaignId:string,eventId:string,rng:()=>number=Math.random):Promise<{prey:string;killSuccesses:number;meat:number;pelts:number;encounterStarted:boolean;incompatibleTrap:boolean}> {
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaignRaw=await tx.objectStore('campaign').get(campaignId);
      if(!campaignRaw)throw new Error('Campanha não encontrada.');
      const campaign=CampaignService.normalize(campaignRaw);
      const worldStore=tx.objectStore('world_state');
      let world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro ativo antes de concluir a caça.');
      const eventIndex=world.journey.events.findIndex(event=>event.id===eventId&&event.status==='open'&&(event.result?.mechanic as Record<string,unknown>|undefined)?.kind==='hunt_prey_choice');
      if(eventIndex<0)throw new Error('Escolha de presa pendente não encontrada.');
      const event=world.journey.events[eventIndex];
      const actorId=String(event.result?.actorId||'');
      const characterStore=tx.objectStore('characters');
      const actor=await characterStore.get(actorId) as Character|undefined;
      if(!actor)throw new Error('Caçadora da atividade não encontrada.');
      CampaignPartyService.assertMember(campaign,actor);
      CharacterActionRulesEngine.assertCanAct(actor);
      const preyRoll=Math.max(1,Math.min(6,Math.trunc(Number(event.result?.preyRoll)||1)));
      const prey=JourneyRulesEngine.huntPrey(preyRoll);
      const method=event.result?.method==='trap'?'trap':'weapon';
      const helpIds=((event.result?.huntHelperIds as string[]|undefined)||[]).filter(Boolean);
      const helpBonus=Math.max(0,Math.trunc(Number(event.result?.huntHelpBonus)||0));
      const gear=Math.max(0,Math.trunc(Number(event.result?.huntEquipmentBonus)||0));
      const now=Date.now();
      if(method==='trap'&&prey.requirement==='Arma'){
        const nextEvent:JourneyEvent={...event,status:'resolved',detail:`${actor.name} manteve ${prey.animal}, mas a presa exige arma e não pode ser abatida com armadilha. Nenhuma Carne ou Pele foi obtida.`,result:{...(event.result||{}),killSuccesses:0,meat:0,pelts:0,incompatibleTrap:true,mechanic:undefined}};
        const events=[...world.journey.events];events[eventIndex]=nextEvent;
        world={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
        await worldStore.put(world);
        return {prey:prey.animal,killSuccesses:0,meat:0,pelts:0,encounterStarted:false,incompatibleTrap:true,world};
      }
      const skill=method==='weapon'?normalizedSkill(actor,['Marksmanship','Pontaria']):normalizedSkill(actor,['Survival','Sobrevivência']);
      const base=method==='weapon'?actor.attributes.agility.current:actor.attributes.wits.current;
      const skillName=method==='weapon'?'marksmanship':'survival';
      const attributeName=method==='weapon'?'agility':'wits';
      const kill=ForbiddenLandsDiceEngine.roll({base,skill,gear,modifier:prey.difficulty+helpBonus},{characterId:actor.id,sceneId:`journey:hunt-choice:${campaignId}:${eventId}`,checkId:`journey:hunt-choice-kill:${eventId}:${now}`,rng,criticalInjuries:actor.criticalInjuryStates,skillName,attributeName,assistance:{helperIds:helpIds}});
      await tx.objectStore('rolls').put(kill);
      let meat=0,pelts=0,encounterStarted=false;
      let nextActor=actor;
      let detail='';
      if(kill.successes>0){
        meat=prey.meat;pelts=prey.pelts;
        if(meat>0)nextActor=appendJourneyProvision(nextActor,'meat',meat,event.day,event.id);
        if(pelts>0)nextActor=appendJourneyProvision(nextActor,'pelts',pelts,event.day,event.id);
        await characterStore.put(nextActor);
        detail=`${actor.name} manteve ${prey.animal} (D6 ${preyRoll}) e abateu a presa com ${kill.successes} sucesso(s): ${meat} Carne, ${pelts} Pele(s).`;
      }else if('attacksOnFailure' in prey&&prey.attacksOnFailure){
        const animal=JourneyRulesEngine.officialWildAnimal('boar');
        if(!animal)throw new Error('Javali oficial não encontrado.');
        const party=await characterStore.getAll() as Character[];
        const enemy:EncounterCombatant={id:`journey-animal:${crypto.randomUUID()}`,name:animal.name,kind:'npc',side:'enemy',initiative:1+Math.floor(Math.max(0,Math.min(.999999,rng()))*10),currentStrength:animal.strength,maxStrength:animal.strength,armor:0,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',notes:[`Animal oficial do Guia do Mestre. Movimento ${animal.movement}.`,animal.notes||''].filter(Boolean).join(' '),x:.82,y:.5,size:1,quickSheet:{category:'animal',attributes:{strength:animal.strength,agility:animal.agility},skills:animal.skills,movement:animal.movement,weapons:[{name:animal.weapon,damage:animal.damage,damageType:animal.damageType}],source:'Guia do Mestre, pp. 126–127 — Animais'}};
        const encounter=EncounterService.createEncounter(`${animal.name} — ataque durante a Caça`,campaign.currentHexId||undefined,party,[enemy]);
        const triggered=EncounterService.triggerWatchOnEncounter(world,encounter,party,campaign.currentDay||1,campaign.currentPeriod||'Morning',rng);
        for(const watchRoll of triggered.rolls)await tx.objectStore('rolls').put(watchRoll);
        world=triggered.world;encounterStarted=true;
        detail=`${actor.name} falhou ao abater ${prey.animal}; o javali ataca imediatamente e o encontro foi iniciado.`;
      }else detail=`${actor.name} tentou abater ${prey.animal}, mas falhou. Nenhuma Carne ou Pele foi obtida.`;
      const currentEventIndex=world.journey.events.findIndex(item=>item.id===eventId);
      const finalEvent:JourneyEvent={...event,status:'resolved',detail,result:{...(event.result||{}),killRollId:kill.id,killSuccesses:kill.successes,meat,pelts,encounterStarted,mechanic:undefined}};
      const events=[...world.journey.events];
      if(currentEventIndex>=0)events[currentEventIndex]=finalEvent;else events.push(finalEvent);
      world={...world,journey:{...world.journey,events:retainJourneyEvents(events),updatedAt:now},updatedAt:now};
      await worldStore.put(world);
      return {prey:prey.animal,killSuccesses:kill.successes,meat,pelts,encounterStarted,incompatibleTrap:false,world};
    });
    this.notify(outcome.world);
    return {prey:outcome.prey,killSuccesses:outcome.killSuccesses,meat:outcome.meat,pelts:outcome.pelts,encounterStarted:outcome.encounterStarted,incompatibleTrap:outcome.incompatibleTrap};
  }

  static async cureLice(campaignId:string,eventId:string,healerId:string,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;cured:boolean}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de tratar os piolhos.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='lice');
      if(!source)throw new Error('Infortúnio de piolhos não está pendente.');
      const victim=await tx.objectStore('characters').get(String(source.result?.actorId||''));
      const healer=await tx.objectStore('characters').get(healerId);
      if(!victim?.liceState)throw new Error('Vítima dos piolhos não encontrada ou já tratada.');
      if(!healer)throw new Error('Curandeira não encontrada.');
      CharacterActionRulesEngine.assertCanAct(healer);
      const roll=ForbiddenLandsDiceEngine.roll({base:healer.attributes.empathy.current,skill:normalizedSkill(healer,['Healing','Cura']),gear:0},{characterId:healerId,sceneId:`journey:lice:${campaignId}`,checkId:`journey:lice:${eventId}:${now}`,rng,criticalInjuries:healer.criticalInjuryStates,skillName:'healing',attributeName:'empathy'});
      await tx.objectStore('rolls').put(roll);
      const cured=roll.successes>0;
      if(cured)await tx.objectStore('characters').put(LiceRulesEngine.cure(victim));
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:cured?'resolved' as const:'open' as const,result:{...(item.result||{}),healingRollIds:[...((item.result?.healingRollIds as string[]|undefined)||[]),roll.id],cured},detail:`${item.detail} ${healer.name} rolou Cura (${roll.successes} sucesso(s)) e ${cured?'removeu':'não removeu'} os piolhos de ${victim.name}.`}:item);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll,cured,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,cured:outcome.cured};
  }

  static async resolveBrokenJourneyEquipment(campaignId:string,eventId:string,collection:'weapons'|'armor'|'gear',index:number,crafterId:string,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;repaired:boolean;destroyed:boolean;itemName:string}> {
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de reparar o equipamento quebrado.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='gear_damaged');
      if(!source)throw new Error('Equipamento quebrado não está pendente.');
      const ownerId=String(source.result?.actorId||'');
      const characters=tx.objectStore('characters');
      const owner=await characters.get(ownerId) as Character|undefined;
      const crafter=await characters.get(crafterId) as Character|undefined;
      if(!owner||!crafter)throw new Error('Proprietária ou artesã não encontrada.');
      const gearCampaign=await tx.objectStore('campaign').get(campaignId);
      CampaignPartyService.assertMember(gearCampaign,owner);
      CampaignPartyService.assertMember(gearCampaign,crafter);
      CharacterActionRulesEngine.assertCanAct(crafter);
      const items=owner[collection] as unknown[];
      if(!Number.isInteger(index)||index<0||index>=items.length)throw new Error('Escolha o item que quebrou.');
      const value=items[index];
      const sourceItem=value&&typeof value==='object'?value as Record<string,unknown>:{name:String(value??`Equipamento ${index+1}`)};
      const itemName=String(sourceItem.name||sourceItem.label||`Equipamento ${index+1}`);
      const broken=JourneyRulesEngine.breakJourneyEquipmentItem(sourceItem);
      const crafting=normalizedSkill(crafter,['Crafting','Artesanato']);
      const roll=ForbiddenLandsDiceEngine.roll({base:crafter.attributes.strength.current,skill:crafting,gear:0},{characterId:crafter.id,sceneId:`journey:gear-repair:${campaignId}`,checkId:`journey:gear-repair:${eventId}:${now}`,rng,criticalInjuries:crafter.criticalInjuryStates,skillName:'crafting',skillRankAtRoll:crafting,attributeName:'strength'});
      await tx.objectStore('rolls').put(roll);
      const repair=JourneyRulesEngine.resolveJourneyEquipmentRepair(broken.item,roll.successes);
      const nextItems=items.slice();
      if(repair.destroyed)nextItems.splice(index,1);
      else nextItems[index]=repair.item;
      await characters.put({...owner,[collection]:nextItems});
      const closed=repair.repaired||repair.destroyed;
      const detail=repair.destroyed
        ? `${crafter.name} falhou no reparo de ${itemName}; como o Bônus de Equipamento estava em zero, o item foi destruído.`
        : repair.repaired
          ? `${crafter.name} reparou ${itemName} com ${roll.successes} sucesso(s) em Artesanato.`
          : `${crafter.name} falhou ao reparar ${itemName}; o item continua quebrado.`;
      const events=world.journey.events.map(entry=>entry.id===eventId?{...entry,status:closed?'resolved' as const:'open' as const,result:{...(entry.result||{}),collection,index,itemName,broken:true,repairRollIds:[...((entry.result?.repairRollIds as string[]|undefined)||[]),roll.id],repaired:repair.repaired,destroyed:repair.destroyed,hasEquipmentBonus:repair.hasEquipmentBonus,originalBonus:repair.originalBonus,currentBonus:repair.currentBonus},detail:`${entry.detail} ${detail}`}:entry);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll,repaired:repair.repaired,destroyed:repair.destroyed,itemName,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,repaired:outcome.repaired,destroyed:outcome.destroyed,itemName:outcome.itemName};
  }

  static async resolveSnaggedFishingGear(campaignId:string,eventId:string,gearIndex:number,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;recovered:boolean;itemName:string}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de recuperar o equipamento de pesca.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='stuck_fishing_gear');
      if(!source)throw new Error('Equipamento de pesca preso não está pendente.');
      const characterStore=tx.objectStore('characters');
      const actor=await characterStore.get(String(source.result?.actorId||''));
      if(!actor)throw new Error('Pescadora não encontrada.');
      CharacterActionRulesEngine.assertCanAct(actor);
      if(!Number.isInteger(gearIndex)||gearIndex<0||gearIndex>=actor.gear.length)throw new Error('Escolha o equipamento de pesca preso.');
      const item=actor.gear[gearIndex];
      const itemName=String(item?.name||item?.label||`Equipamento ${gearIndex+1}`);
      const roll=ForbiddenLandsDiceEngine.roll({base:actor.attributes.strength.current,skill:normalizedSkill(actor,['Might','Potência']),gear:0},{characterId:actor.id,sceneId:`journey:fishing-snag:${campaignId}`,checkId:`journey:fishing-snag:${eventId}:${now}`,rng,criticalInjuries:actor.criticalInjuryStates,skillName:'might',attributeName:'strength'});
      await tx.objectStore('rolls').put(roll);
      const checkConsequence=CriticalInjuryRulesEngine.applyCheckConsequences(actor,'might');
      const checkedActor=checkConsequence.character as Character;
      if(checkConsequence.triggers.length)await characterStore.put(checkedActor);
      const recovered=roll.successes>0;
      if(!recovered)await characterStore.put({...checkedActor,gear:checkedActor.gear.filter((_,index)=>index!==gearIndex)});
      const events=world.journey.events.map(entry=>entry.id===eventId?{...entry,status:'resolved' as const,result:{...(entry.result||{}),recoveryRollId:roll.id,recovered,gearIndex,itemName,...(recovered?{}:{lostItem:item})},detail:`${entry.detail} ${actor.name} ${recovered?'recuperou':'perdeu'} ${itemName} (${roll.successes} sucesso(s)).`}:entry);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll,recovered,itemName,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,recovered:outcome.recovered,itemName:outcome.itemName};
  }

  static async resolveLostEquipment(campaignId:string,eventId:string,collection:'weapons'|'armor'|'gear',index:number,affectedCharacterId?:string):Promise<{itemName:string}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de registrar a perda.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&['lost_equipment','group_attack_and_check'].includes(String((item.result?.mechanic as Record<string,unknown>|undefined)?.kind||'')));
      if(!source)throw new Error('Perda de equipamento não está pendente.');
      const mechanic=source.result?.mechanic as Record<string,unknown>;
      const pending=(source.result?.pendingGearLossCharacterIds as string[]|undefined)||[];
      if(mechanic.kind==='group_attack_and_check'&&(!affectedCharacterId||!pending.includes(affectedCharacterId)))throw new Error('Escolha uma personagem que falhou ao salvar equipamento.');
      if(mechanic.kind==='lost_equipment'&&mechanic.scope!=='random_party_member'&&collection==='armor')throw new Error('Neste infortúnio, escolha uma arma ou equipamento.');
      const actorId=mechanic.kind==='group_attack_and_check'?String(affectedCharacterId):String(source.result?.actorId||'');
      const characterStore=tx.objectStore('characters');
      const actor=await characterStore.get(actorId);
      if(!actor)throw new Error('Personagem do infortúnio não encontrado.');
      const items=actor[collection] as unknown[];
      if(!Number.isInteger(index)||index<0||index>=items.length)throw new Error('Item inválido para a perda.');
      const item=items[index];
      const itemName=typeof item==='object'&&item!==null?String((item as Record<string,unknown>).name||(item as Record<string,unknown>).label||'Equipamento'):String(item);
      await characterStore.put({...actor,[collection]:items.filter((_,position)=>position!==index)});
      const remaining=mechanic.kind==='group_attack_and_check'?pending.filter(id=>id!==actorId):[];
      const criticalRemaining=(source.result?.pendingCriticalInjuryCharacterIds as string[]|undefined)||[];
      const coldRemaining=(source.result?.pendingColdCheckCharacterIds as string[]|undefined)||[];
      const events=world.journey.events.map(entry=>entry.id===eventId?{...entry,status:remaining.length||criticalRemaining.length||coldRemaining.length?'open' as const:'resolved' as const,result:{...(entry.result||{}),pendingGearLossCharacterIds:remaining,lostItems:[...((entry.result?.lostItems as unknown[]|undefined)||[]),{characterId:actorId,collection,index,item,name:itemName}]},detail:`${entry.detail} ${actor.name} perdeu ${itemName}.`}:entry);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {itemName,world:nextWorld};
    });
    this.notify(outcome.world);
    return {itemName:outcome.itemName};
  }

  static async resolvePendingJourneyCritical(campaignId:string,eventId:string,characterId:string,table:'slash'|'puncture'|'blunt',rng:()=>number=Math.random):Promise<{roll:number;name:string;table:string}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de tratar o ferimento do incêndio.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='group_attack_and_check');
      const pending=(source?.result?.pendingCriticalInjuryCharacterIds as string[]|undefined)||[];
      if(!source||!pending.includes(characterId))throw new Error('Ferimento do incêndio não está pendente para esta personagem.');
      const characters=tx.objectStore('characters');
      const character=await characters.get(characterId);
      if(!character)throw new Error('Personagem do infortúnio não encontrada.');
      const rolled=CriticalInjuryRulesEngine.rollWithResult(table,rng);
      const state=criticalStateForJourney(table,rolled.roll,rng,now);
      const pendingMarker='PENDENTE: ferimento grave — tabela aguardando escolha após Incêndio';
      const marker=`${table}:${rolled.roll}:${state.name}`;
      const immediatelyDead=Boolean(state.deathTimer?.immediate);
      await characters.put({...character,criticalInjuryStates:[...(character.criticalInjuryStates||[]),state],criticalInjuries:[...(character.criticalInjuries||[]).filter((item:string)=>item!==pendingMarker),marker],...(immediatelyDead?{status:'dead' as const}:{})});
      const remaining=pending.filter(id=>id!==characterId);
      const pendingGear=(source.result?.pendingGearLossCharacterIds as string[]|undefined)||[];
      const pendingCold=(source.result?.pendingColdCheckCharacterIds as string[]|undefined)||[];
      const unresolved=remaining.length||pendingGear.length||pendingCold.length;
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:unresolved?'open' as const:'resolved' as const,result:{...(item.result||{}),pendingCriticalInjuryCharacterIds:remaining,criticalInjuryRolls:[...((item.result?.criticalInjuryRolls as unknown[]|undefined)||[]),{characterId,table,roll:rolled.roll,name:state.name,state}]},detail:`${item.detail} ${character.name}: ${state.name} (${table}, D66 ${rolled.roll})${immediatelyDead?'; morte imediata':''}.`}:item);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll:rolled.roll,name:state.name,table,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,name:outcome.name,table:outcome.table};
  }

  static async escapeHuntingTrap(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;escaped:boolean}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de escapar da armadilha.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='trap');
      if(!source)throw new Error('Armadilha não está pendente.');
      if(source.result?.actorId!==characterId)throw new Error('Apenas a personagem presa pode tentar escapar.');
      const character=await tx.objectStore('characters').get(characterId);
      if(!character)throw new Error('Personagem não encontrado.');
      CharacterActionRulesEngine.assertCanAct(character);
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.agility.current,skill:normalizedSkill(character,['Move','Movement','Movimentação']),gear:0},{characterId,sceneId:`journey:trap:${campaignId}`,checkId:`journey:trap:${eventId}:${now}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'movement',attributeName:'agility'});
      await tx.objectStore('rolls').put(roll);
      const escaped=roll.successes>0;
      const checkConsequence=CriticalInjuryRulesEngine.applyCheckConsequences(character,'movement');
      if(checkConsequence.triggers.length)await tx.objectStore('characters').put(checkConsequence.character as Character);
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:escaped?'resolved' as const:'open' as const,result:{...(item.result||{}),escapeRollIds:[...((item.result?.escapeRollIds as string[]|undefined)||[]),roll.id],escaped},detail:`${item.detail} ${character.name} ${escaped?'escapou':'falhou ao escapar'} (${roll.successes} sucesso(s)).`}:item);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll,escaped,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,escaped:outcome.escaped};
  }

  static async repairTornClothing(campaignId:string,eventId:string,crafterId:string,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;repaired:boolean}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de reparar as roupas.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='clothing_damaged');
      if(!source)throw new Error('Roupas danificadas não estão pendentes.');
      if((source.result?.pendingColdCheckCharacterIds as string[]|undefined)?.length)throw new Error('Resolva primeiro o teste de frio das roupas rasgadas.');
      const crafter=await tx.objectStore('characters').get(crafterId);
      if(!crafter)throw new Error('Personagem não encontrado.');
      CharacterActionRulesEngine.assertCanAct(crafter);
      const roll=ForbiddenLandsDiceEngine.roll({base:crafter.attributes.strength.current,skill:normalizedSkill(crafter,['Crafting','Artesanato']),gear:0},{characterId:crafterId,sceneId:`journey:clothing:${campaignId}`,checkId:`journey:clothing:${eventId}:${now}`,rng,criticalInjuries:crafter.criticalInjuryStates,skillName:'crafting',attributeName:'strength'});
      await tx.objectStore('rolls').put(roll);
      const repaired=roll.successes>0;
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:repaired?'resolved' as const:'open' as const,result:{...(item.result||{}),repairRollIds:[...((item.result?.repairRollIds as string[]|undefined)||[]),roll.id],repaired},detail:`${item.detail} ${crafter.name} ${repaired?'reparou as roupas':'falhou no reparo'} (${roll.successes} sucesso(s)).`}:item);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll,repaired,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,repaired:outcome.repaired};
  }

  static async resolveLeeches(campaignId:string,eventId:string,healerId:string,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;damage:number}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de remover as sanguessugas.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='leech_infestation');
      if(!source)throw new Error('Sanguessugas não estão pendentes.');
      const victimId=String(source.result?.actorId||'');
      if(!victimId)throw new Error('A vítima do infortúnio não está identificada.');
      const characterStore=tx.objectStore('characters');
      const healer=await characterStore.get(healerId);
      const victim=await characterStore.get(victimId);
      if(!healer||!victim)throw new Error('Personagem não encontrado.');
      const campaign=await tx.objectStore('campaign').get(campaignId);
      CampaignPartyService.assertMember(campaign,healer);
      CampaignPartyService.assertMember(campaign,victim);
      CharacterActionRulesEngine.assertCanAct(healer);
      const roll=ForbiddenLandsDiceEngine.roll({base:healer.attributes.empathy.current,skill:normalizedSkill(healer,['Healing','Cura']),gear:0},{characterId:healerId,sceneId:`journey:leeches:${campaignId}`,checkId:`journey:leeches:${eventId}:${now}`,rng,criticalInjuries:healer.criticalInjuryStates,skillName:'healing',attributeName:'empathy'});
      await tx.objectStore('rolls').put(roll);
      const before=victim.attributes.strength.current;
      const current=roll.successes>0?before:Math.max(0,before-1);
      const conditions=current===0?Array.from(new Set([...(victim.conditions||[]),'Broken' as const])):victim.conditions;
      await characterStore.put({...victim,attributes:{...victim.attributes,strength:{...victim.attributes.strength,current}},conditions,...(current===0?{brokenAttribute:'strength' as const}:{})});
      const damage=before-current;
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,result:{...(item.result||{}),healingRollId:roll.id,healerId,removalSuccess:roll.successes>0,additionalDamage:damage},detail:`${item.detail} ${healer.name} removeu as sanguessugas${damage?`; ${victim.name} sofreu mais ${damage} dano de Força`:''}.`}:item);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll,damage,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,damage:outcome.damage};
  }

  static async resolveGroupEscape(campaignId:string,eventId:string,characterId:string,choice:'might'|'movement'='might',helpBonus=0,rng:()=>number=Math.random):Promise<{roll:import('../types').DiceRoll;escaped:boolean;damage:number;allEscaped:boolean}>{
    const now=Date.now();
    const outcome=await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de escapar do obstáculo.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='group_escape');
      if(!source)throw new Error('Obstáculo do grupo não está pendente.');
      const mechanic=source.result?.mechanic as Record<string,unknown>;
      if(choice==='movement'&&!Array.isArray(mechanic.attributes))throw new Error('Este obstáculo exige Potência.');
      const characterStore=tx.objectStore('characters');
      const character=await characterStore.get(characterId);
      if(!character)throw new Error('Personagem não encontrado.');
      CharacterActionRulesEngine.assertCanAct(character);
      const campaign=await tx.objectStore('campaign').get(campaignId);
      CampaignPartyService.assertMember(campaign,character);
      const party=CampaignPartyService.members(campaign,await characterStore.getAll()).filter(member=>member.status!=='dead');
      if(party.length===0)throw new Error('O grupo da campanha não possui integrantes vivos.');
      const previous=(source.result?.escapedCharacterIds as string[]|undefined)||[];
      if(!Array.isArray(previous)||previous.some(id=>!party.some(member=>member.id===id)))
        throw new Error('Evento de fuga registra personagens de outra campanha ou não vivos.');
      if(previous.includes(characterId))throw new Error('Esta personagem já escapou do obstáculo.');
      const appliedHelp=Math.max(0,Math.min(3,Math.trunc(Number(helpBonus)||0)));
      if(appliedHelp>previous.length)throw new Error('A ajuda excede o número de personagens que já escaparam.');
      const attribute=choice==='movement'?'agility':'strength';
      const skill=choice==='movement'?normalizedSkill(character,['Move','Movement','Movimentação']):normalizedSkill(character,['Might','Potência']);
      const assistanceIds = previous.slice(0, appliedHelp);
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes[attribute].current,skill,gear:0},{characterId,sceneId:`journey:escape:${campaignId}`,checkId:`journey:escape:${eventId}:${characterId}:${now}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:choice,attributeName:attribute,assistance:{helperIds:assistanceIds}});
      await tx.objectStore('rolls').put(roll);
      const escaped=roll.successes>0;
      const failure=mechanic.failureDamage as Record<string,unknown>|undefined;
      const damageAttribute=(failure?.attribute==='agility'?'agility':'strength') as keyof Character['attributes'];
      const before=character.attributes[damageAttribute].current;
      const current=escaped?before:Math.max(0,before-Math.max(0,Math.trunc(Number(failure?.amount)||0)));
      const damage=before-current;
      if(!escaped){
        const conditions=current===0?Array.from(new Set([...(character.conditions||[]),'Broken' as const])):character.conditions;
        await characterStore.put({...character,attributes:{...character.attributes,[damageAttribute]:{...character.attributes[damageAttribute],current}},conditions,...(current===0?{brokenAttribute:damageAttribute}:{})});
      }
      const escapedIds=escaped?[...previous,characterId]:previous;
      const allEscaped=party.every(member=>escapedIds.includes(member.id));
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:allEscaped?'resolved' as const:'open' as const,result:{...(item.result||{}),escapedCharacterIds:escapedIds,escapeAttempts:[...((item.result?.escapeAttempts as unknown[]|undefined)||[]),{characterId,choice,assistedBy:assistanceIds,helpBonus:appliedHelp,rollId:roll.id,escaped,damage}]},detail:`${item.detail} ${character.name}: ${escaped?'escapou':'falhou e sofreu '+damage+' dano(s)'} (${roll.successes} sucesso(s), ajuda ${appliedHelp}).`}:item);
      const nextWorld={...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now};
      await worldStore.put(nextWorld);
      return {roll,escaped,damage,allEscaped,world:nextWorld};
    });
    this.notify(outcome.world);
    return {roll:outcome.roll,escaped:outcome.escaped,damage:outcome.damage,allEscaped:outcome.allEscaped};
  }

  static async resolveLostRoute(campaignId: string, eventId: string, characterId: string, rng: () => number = Math.random): Promise<{ roll: import('../types').DiceRoll; found: boolean }> {
    const now=Date.now();
    const outcome=await withTransaction(['campaign','app_meta','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaignStore=tx.objectStore('campaign');
      const campaignRaw=await campaignStore.get(campaignId);
      if(!campaignRaw)throw new Error('Campanha não encontrada.');
      const campaign=CampaignService.normalize(campaignRaw);
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de procurar a rota.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='lost');
      if(!source)throw new Error('Infortúnio Perdidos não está pendente.');
      if(source.result?.actorId&&source.result.actorId!==characterId)throw new Error('A desbravadora que se perdeu deve procurar a rota.');
      const characterStore=tx.objectStore('characters');
      const character=await characterStore.get(characterId);
      if(!character)throw new Error('Desbravadora não encontrada.');
      CampaignPartyService.assertMember(campaign,character);
      CharacterActionRulesEngine.assertCanAct(character);
      const quarter=`${campaign.currentDay||1}:${campaign.currentPeriod||'Morning'}`;
      if(source.result?.lastAttemptQuarter===quarter)throw new Error('A desbravadora já tentou encontrar a rota neste Quarto de Dia.');
      const roll=ForbiddenLandsDiceEngine.roll({base:character.attributes.wits.current,skill:normalizedSkill(character,['Survival','Sobrevivência']),gear:0},{characterId,sceneId:`journey:lost:${campaignId}`,checkId:`journey:lost:${eventId}:${quarter}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'survival',attributeName:'wits'});
      await tx.objectStore('rolls').put(roll);
      const found=roll.successes>0;
      const nextCampaign=found?campaign:{...CampaignService.advanceState(campaign,1),updatedAt:now};
      if(!found){
        for(const member of CampaignPartyService.members(campaign,await characterStore.getAll()))
          await characterStore.put(CharacterTimeRulesEngine.advanceQuarters(member,1));
        await campaignStore.put(nextCampaign);
        await tx.objectStore('app_meta').put({id:'activeCampaignId',value:nextCampaign.id,updatedAt:now});
      }
      const nextWorldBase={...world,journey:{...world.journey,events:world.journey.events.map(item=>item.id===eventId?{...item,status:found?'resolved' as const:'open' as const,result:{...(item.result||{}),lastAttemptQuarter:quarter,recoveryRollIds:[...((item.result?.recoveryRollIds as string[]|undefined)||[]),roll.id]},detail:`${item.detail} ${character.name} ${found?'encontrou a rota':'falhou; outro Quarto de Dia passou sem progresso'} (${roll.successes} sucesso(s)).`}:item),updatedAt:now},updatedAt:now};
      const nextWorld=found?nextWorldBase:WorldStateService.advanceTime(nextWorldBase,campaign,nextCampaign,1,now);
      await worldStore.put(nextWorld);
      return {roll,found,campaign:nextCampaign,previousCampaign:campaign,world:nextWorld};
    });
    let lostWorld=outcome.world;
    if(!outcome.found){
      CampaignService.notify(outcome.campaign);
      lostWorld=await CampaignService.reconcileElapsedTime(outcome.previousCampaign,outcome.campaign,'Busca da rota após se perder',outcome.world);
    }
    this.notify(lostWorld);
    return {roll:outcome.roll,found:outcome.found};
  }

  static async recordCamp(campaignInput: CampaignState): Promise<WorldState> {
    const result = await withTransaction(['campaign', 'app_meta', 'world_state', 'characters'], 'readwrite', async tx => {
      const campaignStore = tx.objectStore('campaign');
      const campaign = CampaignService.normalize(await campaignStore.get(campaignInput.id) || campaignInput);
      const worldStore = tx.objectStore('world_state');
      let world = WorldStateService.normalize(await worldStore.get(campaign.id), campaign.id);
      if (world.encounter?.status === 'active') throw new Error('Encerre o encontro antes de preparar o acampamento.');
      const characterStore = tx.objectStore('characters');
      const party=CampaignPartyService.members(campaign,await characterStore.getAll());
      if (party.some(member => member.survivalState?.mustSleep)) throw new Error('Uma personagem desabada precisa dormir antes de avançar o período.');
      const now = Date.now();
      const nextCampaign = { ...CampaignService.advanceState(campaign, 1), updatedAt: now };
      for (const member of party) await characterStore.put(CharacterTimeRulesEngine.advanceQuarters(member, 1));
      const event = {
        id: crypto.randomUUID(), type: 'camp' as const, title: 'Acampamento preparado',
        detail: `O grupo dedicou o período de ${periodLabel[campaign.currentPeriod || 'Night']} ao acampamento. O tempo avançou para ${periodLabel[nextCampaign.currentPeriod || 'Morning']} do dia ${nextCampaign.currentDay || 1}.`,
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Night', hexId: campaign.currentHexId || undefined,
        status: 'resolved' as const, createdAt: now,
      };
      world = {
        ...world,
        journey: { ...world.journey, active: false, lastCampDay: campaign.currentDay || 1, events: retainJourneyEvents([...world.journey.events, event]), updatedAt: now },
        updatedAt: now,
      };
      world = WorldStateService.advanceTime(world, campaign, nextCampaign, 1, now);
      await campaignStore.put(nextCampaign);
      await tx.objectStore('app_meta').put({ id: 'activeCampaignId', value: nextCampaign.id, updatedAt: now });
      await worldStore.put(world);
      return { world, campaign: nextCampaign, previousCampaign: campaign };
    });
    CampaignService.notify(result.campaign);
    const finalWorld=await CampaignService.reconcileElapsedTime(result.previousCampaign,result.campaign,'Acampamento preparado',result.world);
    this.notify(finalWorld);
    return finalWorld;
  }

  static async passPeriod(campaignId: string): Promise<WorldState> {
    const result = await withTransaction(['campaign', 'app_meta', 'world_state', 'characters'], 'readwrite', async tx => {
      const campaignStore = tx.objectStore('campaign');
      const current = await campaignStore.get(campaignId);
      if (!current) throw new Error('Campanha não encontrada.');
      const campaign = CampaignService.normalize(current);
      const worldStore = tx.objectStore('world_state');
      let world = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      if (world.encounter?.status === 'active') throw new Error('Não avance o tempo enquanto houver um encontro ativo.');
      const characterStore = tx.objectStore('characters');
      const party=CampaignPartyService.members(campaign,await characterStore.getAll());
      if (party.some(member => member.survivalState?.mustSleep)) throw new Error('Uma personagem desabada precisa dormir antes de avançar o período.');
      const now = Date.now();
      const nextCampaign = { ...CampaignService.advanceState(campaign, 1), updatedAt: now };
      for (const member of party) await characterStore.put(CharacterTimeRulesEngine.advanceQuarters(member, 1));
      const event = {
        id: crypto.randomUUID(), type: 'note' as const, title: 'Um período passou',
        detail: `O mundo avançou de ${periodLabel[campaign.currentPeriod || 'Morning']} do dia ${campaign.currentDay || 1} para ${periodLabel[nextCampaign.currentPeriod || 'Morning']} do dia ${nextCampaign.currentDay || 1}.`,
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined,
        status: 'resolved' as const, createdAt: now,
      };
      world = { ...world, journey: { ...world.journey, events: retainJourneyEvents([...world.journey.events, event]), updatedAt: now }, updatedAt: now };
      world = WorldStateService.advanceTime(world, campaign, nextCampaign, 1, now);
      await campaignStore.put(nextCampaign);
      await tx.objectStore('app_meta').put({ id: 'activeCampaignId', value: nextCampaign.id, updatedAt: now });
      await worldStore.put(world);
      return { world, campaign: nextCampaign, previousCampaign: campaign };
    });
    CampaignService.notify(result.campaign);
    const finalWorld=await CampaignService.reconcileElapsedTime(result.previousCampaign,result.campaign,'Passagem de período da jornada',result.world);
    this.notify(finalWorld);
    return finalWorld;
  }

  static async resolveFall(campaignId: string, characterId: string, heightMeters: number, hardSurface: boolean, rng: () => number = Math.random): Promise<FallHazardResult> {
    const height = Math.max(0, Math.trunc(Number(heightMeters) || 0));
    const campaign = await withDbRetry(db => db.get('campaign', campaignId));
    if (!campaign) throw new Error('Campanha não encontrada.');
    const target = await withDbRetry(db => db.get('characters', characterId));
    if (!target) throw new Error('Personagem não encontrado.');
    CharacterActionRulesEngine.assertCanAct(target);
    if (height < 3 || !hardSurface) return { damage: 0, critical: null, roll: 0 };

    const now = Date.now();
    const sceneId = `hazard:fall:${campaignId}:${characterId}:${now}`;
    const roll = ForbiddenLandsDiceEngine.roll({ base: Math.max(0, height - 2), skill: 0, gear: 0 }, {
      characterId, sceneId, checkId: `fall:${campaignId}:${characterId}:${now}`, rng,
    });
    const worldBefore = WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaignId)), campaignId);
    if (worldBefore.encounter?.status === 'active') throw new Error('Resolva a queda dentro do fluxo do encontro ativo.');
    const damage = roll.successes;
    let critical: import('../types').CriticalInjuryState | null = null;

    await withTransaction(['characters', 'world_state', 'rolls', 'campaign'], 'readwrite', async tx => {
      const currentWorld = WorldStateService.normalize(await tx.objectStore('world_state').get(campaignId), campaignId);
      if (currentWorld.encounter?.status === 'active') throw new Error('Um encontro começou antes da queda ser confirmada.');
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current || current.status === 'dead') throw new Error('A ficha mudou antes da queda ser confirmada.');
      CharacterActionRulesEngine.assertCanAct(current);
      const fallEffect = SurvivalRulesEngine.applyFallDamage(current, damage);
      const { strengthBefore, strengthAfter, brokeFromFall } = fallEffect;
      let updated = fallEffect.character;
      if (damage > 0 && brokeFromFall) {
        const d66 = CriticalInjuryRulesEngine.rollWithResult('blunt', rng);
        critical = criticalStateForJourney('blunt', d66.roll, rng, now);
        if (critical.deathTimer?.immediate) critical = { ...critical, status: 'fatal' };
        updated = {
          ...updated,
          criticalInjuryStates: [...(current.criticalInjuryStates || []), critical],
          criticalInjuries: [...(current.criticalInjuries || []), `blunt:${d66.roll}:${critical.name}`],
          ...(critical.deathTimer?.immediate ? { status: 'dead' as const } : {}),
        };
      }
      await characterStore.put(updated);
      await tx.objectStore('rolls').put(roll);
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: 'Queda',
        detail: damage ? `${current.name} caiu de ${height} m sobre uma superfície dura: ${damage} de dano em Força${critical ? ` e ferimento grave (${critical.name})` : ''}.` : `${current.name} caiu de ${height} m sobre uma superfície dura, mas não sofreu dano de Força.`,
        result: { hazard: 'fall', heightMeters: height, hardSurface, baseDice: height - 2, successes: damage, strengthBefore, strengthAfter, ...(critical ? { criticalInjuryId: critical.id, criticalInjuryName: critical.name } : {}) },
        ruleSource: 'Livro do Jogador, p. 113 — Caindo',
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined,
        status: 'resolved', createdAt: now,
      };
      await tx.objectStore('world_state').put({ ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]), updatedAt: now }, updatedAt: now });
      return event;
    });
    const world = WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaignId)), campaignId);
    this.notify(world);
    return { damage, critical, roll: roll.successes };
  }

  static async resolveColdCheck(campaignId: string, characterId: string, equipmentBonus = 0, rng: () => number = Math.random): Promise<ColdHazardResult> {
    const campaign = await withDbRetry(db => db.get('campaign', campaignId));
    if (!campaign) throw new Error('Campanha n\u00e3o encontrada.');
    const target = await withDbRetry(db => db.get('characters', characterId));
    if (!target) throw new Error('Personagem n\u00e3o encontrado.');
    if (target.status === 'dead') throw new Error('Personagem morto não pode testar frio.');
    const worldBefore = WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaignId)), campaignId);
    if (worldBefore.encounter?.status === 'active') throw new Error('Resolva o frio dentro do fluxo do encontro ativo.');
    const now = Date.now();
    const sceneId = `hazard:cold:${campaignId}:${characterId}:${now}`;
    const skill = normalizedSkill(target, ['Resilience', 'Resili\u00eancia']);
    const roll = ForbiddenLandsDiceEngine.roll({
      base: target.attributes.strength.current,
      skill,
      gear: Math.max(0, Math.min(10, Math.trunc(Number(equipmentBonus) || 0))),
    }, { characterId, sceneId, checkId: `cold:${campaignId}:${characterId}:${now}`, rng, criticalInjuries: target.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
    let effect: ReturnType<typeof SurvivalRulesEngine.applyColdCheck> | null = null;
    await withTransaction(['characters', 'world_state', 'rolls'], 'readwrite', async tx => {
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      if (currentWorld.encounter?.status === 'active') throw new Error('Um encontro come\u00e7ou antes da rolagem de frio ser confirmada.');
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current || current.status === 'dead') throw new Error('A ficha mudou antes da rolagem de frio ser confirmada.');
      effect = SurvivalRulesEngine.applyColdCheck(current, roll.successes);
      await characterStore.put(effect.character);
      await tx.objectStore('rolls').put(roll);
      const became = effect.becameHypothermic;
      const died = effect.died;
      const pendingSource=[...currentWorld.journey.events].reverse().find(item=>item.status==='open'&&(item.result?.pendingColdCheckCharacterIds as string[]|undefined)?.includes(characterId));
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: 'Exposi\u00e7\u00e3o ao frio',
        detail: died ? `${current.name} morreu por hipotermia ao fazer nova rolagem com For\u00e7a Acabada.` : roll.successes > 0 ? `${current.name} resistiu ao frio com ${roll.successes} sucesso(s).${current.conditions.includes('Cold') ? ' A hipotermia permanece at\u00e9 que se aque\u00e7a.' : ''}` : `${current.name} falhou em Resili\u00eancia e sofreu 1 dano em For\u00e7a e Ast\u00facia.${became ? ' Est\u00e1 Hipot\u00e9rmica.' : ' A hipotermia piorou.'}`,
        result: { hazard: 'cold', successes: roll.successes, equipmentBonus: Math.max(0, Math.min(10, Math.trunc(Number(equipmentBonus) || 0))), becameHypothermic: became, damageApplied: effect.damageApplied, died, rollId: roll.id, ...(pendingSource?{sourceEventId:pendingSource.id}:{}) },
        ruleSource: 'Livro do Jogador, p. 111 - Condi\u00e7\u00f5es / Hipot\u00e9rmica',
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined,
        status: 'resolved', createdAt: now,
      };
      const priorEvents=currentWorld.journey.events.map(item=>{
        if(item.id!==pendingSource?.id)return item;
        const remaining=(item.result?.pendingColdCheckCharacterIds as string[]).filter(id=>id!==characterId);
        const mechanic=item.result?.mechanic as Record<string,unknown>|undefined;
        const stormStillActive=mechanic?.kind==='storm'&&item.day===(campaign.currentDay||1)&&(campaign.currentPeriod||'Morning')==='Night';
        const repairPending=mechanic?.kind==='clothing_damaged';
        return {...item,result:{...(item.result||{}),pendingColdCheckCharacterIds:remaining},status:remaining.length||stormStillActive||repairPending?'open' as const:'resolved' as const};
      });
      await worldStore.put({ ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...priorEvents, event]), updatedAt: now }, updatedAt: now });
    });
    const world = WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaignId)), campaignId);
    this.notify(world);
    if (!effect) throw new Error('N\u00e3o foi poss\u00edvel confirmar a rolagem de frio.');
    return { roll, becameHypothermic: effect.becameHypothermic, damageApplied: effect.damageApplied, died: effect.died };
  }

  static async warmFromCold(campaignId: string, characterId: string): Promise<void> {
    const world = await withTransaction(['characters', 'world_state', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current) throw new Error('Personagem n\u00e3o encontrado.');
      if (current.status === 'dead') throw new Error('Um personagem morto n\u00e3o pode ser aquecido.');
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha n\u00e3o encontrada.');
      const now = Date.now();
      await characterStore.put(SurvivalRulesEngine.warmFromCold(current));
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'activity', title: 'Aquecido',
        detail: `${current.name} foi aquecido; For\u00e7a e Ast\u00facia podem voltar a se recuperar por descanso.`,
        result: { hazard: 'warm_from_cold', characterId }, ruleSource: 'Livro do Jogador, p. 112 - Hipot\u00e9rmica',
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined,
        status: 'resolved', createdAt: now,
      };
      const nextWorld = { ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]), updatedAt: now }, updatedAt: now };
      await worldStore.put(nextWorld);
      return nextWorld;
    });
    this.notify(world);
  }

  static async exposeToDisease(campaignId: string, characterId: string, virulence = 3, rng: () => number = Math.random): Promise<DiseaseHazardResult> {
    const strength = Math.max(1, Math.min(30, Math.trunc(Number(virulence) || 3)));
    const now = Date.now();
    const sceneId = `hazard:disease:${campaignId}:${characterId}:${now}`;
    let patientRoll: import('../types').DiceRoll | null = null;
    let diseaseRoll: import('../types').DiceRoll | null = null;
    let updated: Character | undefined;
    await withTransaction(['characters', 'world_state', 'rolls', 'campaign'], 'readwrite', async tx => {
      const current = await tx.objectStore('characters').get(characterId);
      if (!current || current.status === 'dead') throw new Error('Personagem vivo não encontrado.');
      CharacterActionRulesEngine.assertCanAct(current);
      if (current.diseaseState) throw new Error('Esta personagem já está doente.');
      const skill = normalizedSkill(current, ['Resilience', 'Resiliência']);
      patientRoll = ForbiddenLandsDiceEngine.roll({ base: current.attributes.strength.current, skill, gear: 0 }, { characterId, sceneId, checkId: `disease-exposure:${now}`, rng, criticalInjuries: current.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
      diseaseRoll = ForbiddenLandsDiceEngine.roll({ base: strength, skill: 0, gear: 0 }, { characterId: null, sceneId, checkId: `disease-virulence:${now}`, rng });
      updated = DiseaseRulesEngine.expose(current, strength, patientRoll.successes, diseaseRoll.successes);
      await tx.objectStore('characters').put(updated);
      await tx.objectStore('rolls').put(patientRoll);
      await tx.objectStore('rolls').put(diseaseRoll);
      const campaign = await tx.objectStore('campaign').get(campaignId);
      const worldStore = tx.objectStore('world_state');
      const world = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const infected = Boolean(updated.diseaseState);
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: 'Exposição a doença',
        detail: infected ? `${current.name} falhou contra Virulência ${strength}; ficará doente após um dia.` : `${current.name} resistiu à infecção (Resiliência ${patientRoll.successes} × Virulência ${diseaseRoll.successes}).`,
        result: { hazard: 'disease_exposure', virulence: strength, patientSuccesses: patientRoll.successes, diseaseSuccesses: diseaseRoll.successes, infected, patientRollId: patientRoll.id, diseaseRollId: diseaseRoll.id },
        ruleSource: 'Livro do Jogador, p. 114 — Doença', day: campaign?.currentDay || 1, period: campaign?.currentPeriod || 'Morning', hexId: campaign?.currentHexId || undefined,
        status: 'resolved', createdAt: now,
      };
      await worldStore.put({ ...world, journey: { ...world.journey, events: retainJourneyEvents([...world.journey.events, event]), updatedAt: now }, updatedAt: now });
    });
    this.notify(WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaignId)), campaignId));
    if (!updated || !patientRoll || !diseaseRoll) throw new Error('Não foi possível registrar a exposição.');
    return { character: updated, patientRoll, diseaseRoll, infected: Boolean(updated.diseaseState) };
  }

  static async eatPoisonousVegetable(campaignId:string,eventId:string,characterId:string,kind:import('../types').PoisonKind,rng:()=>number=Math.random):Promise<PoisonHazardResult>{
    const now=Date.now();
    let victimRoll:import('../types').DiceRoll|null=null;
    let poisonRoll:import('../types').DiceRoll|null=null;
    let updated:Character|undefined;
    let totalEffect=false; let attribute:'strength'|'agility'|'wits'|'empathy'='strength'; let damageApplied=false;
    await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=CampaignService.normalize(await tx.objectStore('campaign').get(campaignId));
      if(!campaign?.id)throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes da refeição.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='poisonous_plant');
      if(!source)throw new Error('Vegetal venenoso não está disponível.');
      const ownerId=String(source.result?.actorId||'');
      if(ownerId&&ownerId!==characterId)throw new Error('O vegetal venenoso pertence à personagem que o coletou.');
      const characterStore=tx.objectStore('characters');
      const character=await characterStore.get(characterId) as Character|undefined;
      if(!character||character.status==='dead')throw new Error('Personagem viva não encontrada.');
      CharacterActionRulesEngine.assertCanAct(character);
      if(character.poisonState?.active)throw new Error('Esta personagem já sofre um efeito total de veneno.');
      const potency=Math.max(1,Math.min(30,Math.trunc(Number((source.result?.mechanic as Record<string,unknown>).virulence)||3)));
      const resilience=normalizedSkill(character,['Resilience','Resiliência']);
      const sceneId=`journey:poisonous-vegetable:${campaignId}:${eventId}`;
      victimRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:resilience,gear:0},{characterId,sceneId,checkId:`poisonous-vegetable-resistance:${eventId}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'resilience',attributeName:'strength'});
      poisonRoll=ForbiddenLandsDiceEngine.roll({base:potency,skill:0,gear:0},{characterId:null,sceneId,checkId:`poisonous-vegetable-potency:${eventId}`,rng});
      await tx.objectStore('rolls').put(victimRoll); await tx.objectStore('rolls').put(poisonRoll);
      const exposure=PoisonRulesEngine.expose(character,kind,potency,victimRoll.successes,poisonRoll.successes);
      updated=exposure.character; totalEffect=exposure.totalEffect; attribute=exposure.attribute; damageApplied=exposure.damageApplied;
      await characterStore.put(updated);
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,result:{...(item.result||{}),consumedByCharacterId:characterId,poisonKind:kind,potency,victimRollId:victimRoll!.id,poisonRollId:poisonRoll!.id,totalEffect},detail:`${item.detail} ${character.name} comeu o vegetal na refeição; veneno ${kind} Potência ${potency}: ${poisonRoll!.successes} × Resiliência ${victimRoll!.successes}${totalEffect?' — efeito total':' — efeito limitado'}.`}:item);
      await worldStore.put({...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now});
    });
    this.notify(WorldStateService.normalize(await withDbRetry(db=>db.get('world_state',campaignId)),campaignId));
    if(!updated||!victimRoll||!poisonRoll)throw new Error('Não foi possível resolver o vegetal venenoso.');
    return {character:updated,victimRoll,poisonRoll,totalEffect,attribute,damageApplied};
  }

  static async discardPoisonousVegetable(campaignId:string,eventId:string):Promise<void>{
    const next=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state'); const world=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='poisonous_plant');
      if(!source)throw new Error('Vegetal venenoso não está disponível.');
      const events=world.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,result:{...(item.result||{}),discarded:true},detail:`${item.detail} O vegetal foi descartado antes de ser consumido.`}:item);
      const updated={...world,journey:{...world.journey,events,updatedAt:Date.now()},updatedAt:Date.now()}; await store.put(updated); return updated;
    }); this.notify(next);
  }

  static async eatDiseasedPrey(campaignId:string,eventId:string,characterId:string,rng:()=>number=Math.random):Promise<DiseaseHazardResult>{
    const now=Date.now();
    let patientRoll:import('../types').DiceRoll|null=null;
    let diseaseRoll:import('../types').DiceRoll|null=null;
    let updated:Character|undefined;
    await withTransaction(['campaign','world_state','characters','rolls'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId);
      if(!campaign)throw new Error('Campanha não encontrada.');
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de comer a carne da presa.');
      const source=world.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='diseased_prey');
      if(!source)throw new Error('A carne da presa doente não está disponível.');
      const eaten=(source.result?.eatenByCharacterIds as string[]|undefined)||[];
      if(eaten.includes(characterId))throw new Error('Esta personagem já comeu uma porção desta presa.');
      const characters=tx.objectStore('characters');
      const character=await characters.get(characterId);
      if(!character||character.status==='dead')throw new Error('Personagem viva não encontrada.');
      CharacterActionRulesEngine.assertCanAct(character);
      if(character.diseaseState)throw new Error('Esta personagem já está doente.');
      const mechanic=source.result?.mechanic as Record<string,unknown>;
      const virulence=Math.max(1,Math.min(30,Math.trunc(Number(mechanic.virulence)||3)));
      const sceneId=`journey:diseased-prey:${campaignId}:${eventId}:${characterId}`;
      const resilience=normalizedSkill(character,['Resilience','Resiliência']);
      patientRoll=ForbiddenLandsDiceEngine.roll({base:character.attributes.strength.current,skill:resilience,gear:0},{characterId,sceneId,checkId:`diseased-prey-resistance:${eventId}:${characterId}`,rng,criticalInjuries:character.criticalInjuryStates,skillName:'resilience',attributeName:'strength'});
      diseaseRoll=ForbiddenLandsDiceEngine.roll({base:virulence,skill:0,gear:0},{characterId:null,sceneId,checkId:`diseased-prey-virulence:${eventId}:${characterId}`,rng});
      updated=DiseaseRulesEngine.expose(character,virulence,patientRoll.successes,diseaseRoll.successes);
      await characters.put(updated);
      await tx.objectStore('rolls').put(patientRoll);
      await tx.objectStore('rolls').put(diseaseRoll);
      const infected=Boolean(updated.diseaseState);
      const exposure={characterId,characterName:character.name,virulence,patientRollId:patientRoll.id,diseaseRollId:diseaseRoll.id,patientSuccesses:patientRoll.successes,diseaseSuccesses:diseaseRoll.successes,infected,at:now};
      const outcomeText=infected?`contraiu doença de Virulência ${virulence}, que se manifesta após um dia`:`resistiu à doença (${patientRoll.successes} × ${diseaseRoll.successes})`;
      const events=world.journey.events.map(item=>item.id===eventId?{...item,result:{...(item.result||{}),eatenByCharacterIds:[...eaten,characterId],mealExposures:[...((item.result?.mealExposures as unknown[]|undefined)||[]),exposure]},detail:`${item.detail} ${character.name} comeu a carne; ${outcomeText}.`}:item);
      await worldStore.put({...world,journey:{...world.journey,events,updatedAt:now},updatedAt:now});
    });
    this.notify(WorldStateService.normalize(await withDbRetry(db=>db.get('world_state',campaignId)),campaignId));
    if(!updated||!patientRoll||!diseaseRoll)throw new Error('Não foi possível registrar a refeição da presa doente.');
    return {character:updated,patientRoll,diseaseRoll,infected:Boolean(updated.diseaseState)};
  }

  static async discardDiseasedPrey(campaignId:string,eventId:string):Promise<void>{
    const world=await withTransaction(['campaign','world_state'],'readwrite',async tx=>{
      if(!await tx.objectStore('campaign').get(campaignId))throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('world_state');
      const current=WorldStateService.normalize(await store.get(campaignId),campaignId);
      const source=current.journey.events.find(item=>item.id===eventId&&item.status==='open'&&(item.result?.mechanic as Record<string,unknown>|undefined)?.kind==='diseased_prey');
      if(!source)throw new Error('A carne da presa doente não está disponível.');
      const events=current.journey.events.map(item=>item.id===eventId?{...item,status:'resolved' as const,result:{...(item.result||{}),discarded:true},detail:`${item.detail} A carne restante foi descartada.`}:item);
      const next={...current,journey:{...current.journey,events,updatedAt:Date.now()},updatedAt:Date.now()};
      await store.put(next);
      return next;
    });
    this.notify(world);
  }

  static async startPersistentAnimalComplication(campaignId: string, eventId: string, input: { animal: string; problem: string }): Promise<void> {
    const animal = String(input.animal || '').trim().slice(0, 80);
    const problem = String(input.problem || '').trim().slice(0, 500);
    if (!animal || !problem) throw new Error('Informe o animal e a complicação definida pelo Mestre.');
    const now = Date.now();
    const world = await withTransaction(['campaign','world_state'], 'readwrite', async tx => {
      if (!await tx.objectStore('campaign').get(campaignId)) throw new Error('Campanha não encontrada.');
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const source = current.journey.events.find(event => event.id === eventId && event.status === 'open' && (event.result?.mechanic as Record<string, unknown> | undefined)?.kind === 'persistent_complication');
      if (!source) throw new Error('O infortúnio de animal persistente não está aberto.');
      if (source.persistentComplication) throw new Error('O acompanhamento desse animal já foi iniciado.');
      const persistentComplication: JourneyPersistentAnimalComplication = {
        animal,
        problem,
        history: [{ note: 'O animal passou a acompanhar o grupo.', createdAt: now }],
      };
      const events = current.journey.events.map(event => event.id === eventId ? {
        ...event,
        persistentComplication,
        detail: `${event.detail} O acompanhamento persistente está ativo.`,
        gmDetail: `${event.gmDetail ? `${event.gmDetail} ` : ''}Animal: ${animal}. Complicação definida pelo Mestre: ${problem}`,
      } : event);
      const next = { ...current, journey: { ...current.journey, events, updatedAt: now }, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
  }

  static async recordPersistentAnimalComplication(campaignId: string, eventId: string, noteInput: string): Promise<void> {
    const note = String(noteInput || '').trim().slice(0, 500);
    if (!note) throw new Error('Descreva o que aconteceu com o animal persistente.');
    const now = Date.now();
    const world = await withTransaction(['campaign','world_state'], 'readwrite', async tx => {
      if (!await tx.objectStore('campaign').get(campaignId)) throw new Error('Campanha não encontrada.');
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const source = current.journey.events.find(event => event.id === eventId && event.status === 'open' && event.persistentComplication && !event.persistentComplication.endedAt);
      if (!source?.persistentComplication) throw new Error('Não há uma complicação de animal ativa para atualizar.');
      const persistentComplication = { ...source.persistentComplication, history: [...source.persistentComplication.history, { note, createdAt: now }].slice(-100) };
      const events = current.journey.events.map(event => event.id === eventId ? {
        ...event,
        persistentComplication,
        gmDetail: `${event.gmDetail ? `${event.gmDetail} ` : ''}Registro: ${note}`,
      } : event);
      const next = { ...current, journey: { ...current.journey, events, updatedAt: now }, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
  }

  static async resolvePersistentAnimalComplication(campaignId: string, eventId: string, resolutionInput: string): Promise<void> {
    const resolution = String(resolutionInput || '').trim().slice(0, 500);
    if (!resolution) throw new Error('Registre como a complicação terminou.');
    const now = Date.now();
    const world = await withTransaction(['campaign','world_state'], 'readwrite', async tx => {
      if (!await tx.objectStore('campaign').get(campaignId)) throw new Error('Campanha não encontrada.');
      const store = tx.objectStore('world_state');
      const current = WorldStateService.normalize(await store.get(campaignId), campaignId);
      const source = current.journey.events.find(event => event.id === eventId && event.status === 'open' && event.persistentComplication && !event.persistentComplication.endedAt);
      if (!source?.persistentComplication) throw new Error('Não há uma complicação de animal ativa para encerrar.');
      const persistentComplication = { ...source.persistentComplication, resolution, endedAt: now, history: [...source.persistentComplication.history, { note: `Resolvida: ${resolution}`, createdAt: now }].slice(-100) };
      const events = current.journey.events.map(event => event.id === eventId ? {
        ...event,
        status: 'resolved' as const,
        persistentComplication,
        detail: `${event.detail} O acompanhamento de ${persistentComplication.animal} terminou.`,
        gmDetail: `${event.gmDetail ? `${event.gmDetail} ` : ''}Resolução: ${resolution}`,
      } : event);
      const next = { ...current, journey: { ...current.journey, events, updatedAt: now }, updatedAt: now };
      await store.put(next);
      return next;
    });
    this.notify(world);
  }

  static async resolveDiseaseCheck(campaignId: string, characterId: string, rng: () => number = Math.random): Promise<DiseaseHazardResult> {
    const now = Date.now();
    const sceneId = `hazard:disease-daily:${campaignId}:${characterId}:${now}`;
    let patientRoll: import('../types').DiceRoll | null = null;
    let diseaseRoll: import('../types').DiceRoll | null = null;
    let updated: Character | undefined;
    await withTransaction(['characters', 'world_state', 'rolls', 'campaign'], 'readwrite', async tx => {
      const current = await tx.objectStore('characters').get(characterId);
      if (!current || current.status === 'dead') throw new Error('Personagem vivo não encontrado.');
      const state = DiseaseRulesEngine.normalize(current.diseaseState);
      if (!state || state.phase !== 'active' || state.pendingDailyChecks < 1) throw new Error('Ainda não venceu uma rolagem diária da doença.');
      const skill = normalizedSkill(current, ['Resilience', 'Resiliência']);
      patientRoll = ForbiddenLandsDiceEngine.roll({ base: current.attributes.strength.current, skill, gear: 0 }, { characterId, sceneId, checkId: `disease-daily:${now}`, rng, criticalInjuries: current.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
      diseaseRoll = ForbiddenLandsDiceEngine.roll({ base: state.virulence, skill: 0, gear: 0 }, { characterId: null, sceneId, checkId: `disease-virulence:${now}`, rng });
      const result = DiseaseRulesEngine.resolveDailyCheck(current, patientRoll.successes, diseaseRoll.successes);
      updated = result.character;
      await tx.objectStore('characters').put(updated);
      await tx.objectStore('rolls').put(patientRoll);
      await tx.objectStore('rolls').put(diseaseRoll);
      const campaign = await tx.objectStore('campaign').get(campaignId);
      const worldStore = tx.objectStore('world_state');
      const world = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: 'Adoecimento diário',
        detail: result.recovered ? `${current.name} superou a doença com Resiliência ${patientRoll.successes} contra ${diseaseRoll.successes}.` : `${current.name} não superou a doença e sofreu 1 dano em Força e Agilidade.`,
        result: { hazard: 'disease_daily_check', patientSuccesses: patientRoll.successes, diseaseSuccesses: diseaseRoll.successes, recovered: result.recovered, damageApplied: result.damageApplied, patientRollId: patientRoll.id, diseaseRollId: diseaseRoll.id },
        ruleSource: 'Livro do Jogador, p. 114 — Doença', day: campaign?.currentDay || 1, period: campaign?.currentPeriod || 'Morning', hexId: campaign?.currentHexId || undefined,
        status: 'resolved', createdAt: now,
      };
      await worldStore.put({ ...world, journey: { ...world.journey, events: retainJourneyEvents([...world.journey.events, event]), updatedAt: now }, updatedAt: now });
    });
    this.notify(WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaignId)), campaignId));
    if (!updated || !patientRoll || !diseaseRoll) throw new Error('Não foi possível confirmar a rolagem diária da doença.');
    return { character: updated, patientRoll, diseaseRoll, recovered: !updated.diseaseState, damageApplied: updated.diseaseState !== undefined };
  }

  static async enterWater(campaignId: string, characterId: string, metalArmor: boolean): Promise<void> {
    const world = await withTransaction(['characters', 'world_state', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current) throw new Error('Personagem não encontrado.');
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha não encontrada.');
      const next = DrowningRulesEngine.enterWater(current, metalArmor);
      await characterStore.put(next);
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const now = Date.now();
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: 'Personagem caiu na água',
        detail: `${current.name} está na água${metalArmor ? ' usando armadura de metal e precisa testar a cada rodada' : ' e testa Resiliência a cada turno de 15 minutos'}.`,
        result: { hazard: 'drowning_started', characterId, metalArmor }, ruleSource: 'Livro do Jogador, p. 113 — Afogamento',
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const updated = { ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]), updatedAt: now }, updatedAt: now };
      await worldStore.put(updated);
      return updated;
    });
    this.notify(world);
  }

  static async resolveDrowningTurn(campaignId: string, characterId: string, rng: () => number = Math.random): Promise<DrowningHazardResult> {
    let result: ReturnType<typeof DrowningRulesEngine.resolveTurn> | null = null;
    let roll: import('../types').DiceRoll | null = null;
    const world = await withTransaction(['characters', 'world_state', 'rolls', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current) throw new Error('Personagem não encontrado.');
      const phaseBefore = DrowningRulesEngine.normalize(current.drowningState)?.phase;
      if (!phaseBefore) throw new Error('Personagem não está na água.');
      if (DrowningRulesEngine.needsResilienceCheck(current)) {
        const skill = normalizedSkill(current, ['Resilience', 'Resiliência']);
        roll = ForbiddenLandsDiceEngine.roll({ base: current.attributes.strength.current, skill, gear: 0 }, { characterId, sceneId: `hazard:drowning:${campaignId}:${characterId}`, checkId: `drowning:${crypto.randomUUID()}`, rng, criticalInjuries: current.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
      }
      result = DrowningRulesEngine.resolveTurn(current, roll?.successes || 0, rng);
      await characterStore.put(result.character);
      if (roll) await tx.objectStore('rolls').put(roll);
      const campaign = await tx.objectStore('campaign').get(campaignId);
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const now = Date.now();
      const description = result.damageApplied ? `${current.name} está se afogando e sofreu 1 dano em Força${result.character.drowningState?.minutesUntilDeath ? `; Força Quebrada inicia morte em ${result.character.drowningState.minutesUntilDeath} minuto(s) sem resgate` : ''}.` : roll?.successes ? `${current.name} obteve ${roll.successes} sucesso(s) em Resiliência e permanece ${result.phaseAfter === 'afloat' ? 'boiando' : 'submerso, prendendo a respiração'}.` : `${current.name} falhou ao se manter boiando e afundou.`;
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: result.phaseAfter === 'drowning' ? 'Afogamento' : 'Teste na água', detail: description,
        result: { hazard: 'drowning_turn', phaseBefore, phaseAfter: result.phaseAfter, successes: roll?.successes ?? null, damageApplied: result.damageApplied, minutesUntilDeath: result.character.drowningState?.minutesUntilDeath ?? null, rollId: roll?.id ?? null },
        ruleSource: 'Livro do Jogador, p. 113 — Afogamento', day: campaign?.currentDay || 1, period: campaign?.currentPeriod || 'Morning', hexId: campaign?.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const updated = { ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]), updatedAt: now }, updatedAt: now };
      await worldStore.put(updated);
      return updated;
    });
    this.notify(world);
    if (!result) throw new Error('Não foi possível confirmar o turno na água.');
    return { character: result.character, roll, phaseBefore: result.phaseBefore, phaseAfter: result.phaseAfter, damageApplied: result.damageApplied, died: result.died };
  }

  static async rescueFromWater(campaignId: string, characterId: string): Promise<void> {
    const world = await withTransaction(['characters', 'world_state', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current) throw new Error('Personagem não encontrado.');
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha não encontrada.');
      await characterStore.put(DrowningRulesEngine.rescue(current));
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const now = Date.now();
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'activity', title: 'Resgate da água', detail: `${current.name} foi retirado da água; o relógio de afogamento foi encerrado.`,
        result: { hazard: 'drowning_rescued', characterId }, ruleSource: 'Livro do Jogador, p. 113 — Afogamento',
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const updated = { ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]), updatedAt: now }, updatedAt: now };
      await worldStore.put(updated);
      return updated;
    });
    this.notify(world);
  }

  static async exposeToPoison(campaignId: string, characterId: string, kind: import('../types').PoisonKind, potency = 3, rng: () => number = Math.random): Promise<PoisonHazardResult> {
    const power = Math.max(1, Math.min(30, Math.trunc(Number(potency) || 3)));
    const now = Date.now();
    const sceneId = `hazard:poison:${campaignId}:${characterId}:${now}`;
    let victimRoll: import('../types').DiceRoll | null = null;
    let poisonRoll: import('../types').DiceRoll | null = null;
    let outcome: ReturnType<typeof PoisonRulesEngine.expose> | null = null;
    const world = await withTransaction(['characters', 'world_state', 'rolls', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current || current.status === 'dead') throw new Error('Personagem vivo não encontrado.');
      if (current.poisonState?.active) throw new Error('A personagem já está sob efeito de veneno.');
      const skill = normalizedSkill(current, ['Resilience', 'Resiliência']);
      victimRoll = ForbiddenLandsDiceEngine.roll({ base: current.attributes.strength.current, skill, gear: 0 }, { characterId, sceneId, checkId: `poison-resistance:${now}`, rng, criticalInjuries: current.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
      poisonRoll = ForbiddenLandsDiceEngine.roll({ base: power, skill: 0, gear: 0 }, { characterId: null, sceneId, checkId: `poison-potency:${now}`, rng });
      outcome = PoisonRulesEngine.expose(current, kind, power, victimRoll.successes, poisonRoll.successes);
      await characterStore.put(outcome.character);
      await tx.objectStore('rolls').put(victimRoll);
      await tx.objectStore('rolls').put(poisonRoll);
      const campaign = await tx.objectStore('campaign').get(campaignId);
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: `Exposição a veneno ${kind}`,
        detail: outcome.totalEffect ? `Veneno venceu a disputa (${poisonRoll.successes} × ${victimRoll.successes}); ${current.name} sofre 1 dano em ${outcome.attribute} por rodada até Quebrar esse atributo.` : `${current.name} resistiu ao veneno (${victimRoll.successes} × ${poisonRoll.successes}) e sofreu apenas 1 dano em ${outcome.attribute}.`,
        result: { hazard: 'poison_exposure', kind, potency: power, totalEffect: outcome.totalEffect, attribute: outcome.attribute, damageApplied: outcome.damageApplied, victimRollId: victimRoll.id, poisonRollId: poisonRoll.id },
        ruleSource: 'Livro do Jogador, p. 113–114 — Envenenamento', day: campaign?.currentDay || 1, period: campaign?.currentPeriod || 'Morning', hexId: campaign?.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const updated = { ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]), updatedAt: now }, updatedAt: now };
      await worldStore.put(updated);
      return updated;
    });
    this.notify(world);
    if (!outcome || !victimRoll || !poisonRoll) throw new Error('Não foi possível confirmar a exposição ao veneno.');
    return { character: outcome.character, victimRoll, poisonRoll, totalEffect: outcome.totalEffect, attribute: outcome.attribute, damageApplied: outcome.damageApplied };
  }

  static async coatWeaponWithPoison(campaignId: string, characterId: string, weaponIndex: number, kind: import('../types').PoisonKind, potency = 3): Promise<Character> {
    let result: Character | null = null;
    const world = await withTransaction(['characters', 'world_state', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current || current.status === 'dead') throw new Error('Personagem vivo não encontrado.');
      CharacterActionRulesEngine.assertCanAct(current);
      const coated = PoisonRulesEngine.coatWeapon(current, weaponIndex, kind, potency);
      result = coated;
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha não encontrada.');
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const encounter = currentWorld.encounter;
      let nextWorld = currentWorld;
      if (encounter?.status === 'active') {
        const actor = encounter.combatants[encounter.turnIndex];
        if (actor?.entityId !== characterId || actor.kind !== 'character') throw new Error('Só a personagem do turno atual pode aplicar veneno.');
        if (!actor.slowActionAvailable) throw new Error('A ação lenta deste turno já foi usada.');
        const nextEncounter = { ...encounter, combatants: encounter.combatants.map(item => item.id === actor.id ? { ...item, slowActionAvailable: false } : item), log: [...encounter.log, { id: crypto.randomUUID(), round: encounter.round, combatantId: actor.id, text: `${current.name} aplicou veneno na arma ${current.weapons[weaponIndex]?.name || `#${weaponIndex + 1}`} (Potência ${coated.weapons[weaponIndex]?.poisonCoating?.potency}); ação lenta gasta.`, createdAt: Date.now() }].slice(-300), updatedAt: Date.now() };
        nextWorld = { ...currentWorld, encounter: nextEncounter };
      }
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'activity', title: 'Veneno aplicado à arma',
        detail: `${current.name} aplicou veneno ${kind} (Potência ${coated.weapons[weaponIndex]?.poisonCoating?.potency}) em ${current.weapons[weaponIndex]?.name || `arma #${weaponIndex + 1}`}; dura até um ataque válido ou o fim do próximo Quarto de Dia.`,
        result: { hazard: 'poison_weapon_coated', characterId, weaponIndex, kind, potency: coated.weapons[weaponIndex]?.poisonCoating?.potency, quartersRemaining: 1 },
        ruleSource: 'Livro do Jogador, p. 114 — Veneno em Armas', day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: Date.now(),
      };
      nextWorld = { ...nextWorld, journey: { ...nextWorld.journey, events: retainJourneyEvents([...nextWorld.journey.events, event]) }, updatedAt: Date.now() };
      await characterStore.put(result);
      await worldStore.put(nextWorld);
      return nextWorld;
    });
    this.notify(world);
    if (!result) throw new Error('Não foi possível envenenar a arma.');
    return result;
  }

  static async resolveWeaponPoisonOnHit(campaignId: string, attackerId: string, targetId: string, weaponIndex: number, rng: () => number = Math.random): Promise<WeaponPoisonHitResult> {
    let output: WeaponPoisonHitResult | null = null;
    const world = await withTransaction(['characters', 'world_state', 'rolls', 'campaign'], 'readwrite', async tx => {
      const characters = tx.objectStore('characters');
      const attacker = await characters.get(attackerId);
      if (!attacker || attacker.status === 'dead') throw new Error('Atacante vivo não encontrado.');
      const consumed = PoisonRulesEngine.consumeWeaponCoating(attacker, weaponIndex);
      if (!consumed.coating) throw new Error('A arma não tem mais veneno aplicado.');
      const target = await characters.get(targetId);
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha não encontrada.');
      const now = Date.now();
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      let nextWorld = currentWorld;
      const coating = consumed.coating;
      if (!target || target.status === 'dead') {
        output = { attacker: consumed.character, damageApplied: false, affected: false };
      } else if (target.poisonState?.active) {
        output = { attacker: consumed.character, target, damageApplied: false, affected: false };
      } else {
        const potency = Math.max(1, Math.min(30, Math.trunc(coating.potency)));
        const victimRoll = ForbiddenLandsDiceEngine.roll({ base: target.attributes.strength.current, skill: normalizedSkill(target, ['Resilience', 'Resiliência']), gear: 0 }, { characterId: targetId, sceneId: `hazard:weapon-poison:${campaignId}:${now}`, checkId: `weapon-poison-resistance:${now}`, rng, criticalInjuries: target.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
        const poisonRoll = ForbiddenLandsDiceEngine.roll({ base: potency, skill: 0, gear: 0 }, { characterId: null, sceneId: victimRoll.sceneId, checkId: `weapon-poison-potency:${now}`, rng });
        const effect = PoisonRulesEngine.expose(target, coating.kind, potency, victimRoll.successes, poisonRoll.successes);
        await characters.put(effect.character);
        await tx.objectStore('rolls').put(victimRoll);
        await tx.objectStore('rolls').put(poisonRoll);
        output = { attacker: consumed.character, target: effect.character, victimRoll, poisonRoll, totalEffect: effect.totalEffect, attribute: effect.attribute, damageApplied: effect.damageApplied, affected: true };
        const event: JourneyEvent = {
          id: crypto.randomUUID(), type: 'mishap', title: `Veneno da arma ${coating.kind}`,
          detail: effect.totalEffect ? `${target.name} recebeu efeito total do veneno após ataque; 1 dano em ${effect.attribute} por rodada.` : `${target.name} resistiu ao efeito total (${victimRoll.successes} × ${poisonRoll.successes}) e sofreu apenas 1 dano em ${effect.attribute}.`,
          result: { hazard: 'poison_weapon_hit', attackerId, targetId, weaponIndex, kind: coating.kind, potency, totalEffect: effect.totalEffect, attribute: effect.attribute, damageApplied: effect.damageApplied, victimRollId: victimRoll.id, poisonRollId: poisonRoll.id },
          ruleSource: 'Livro do Jogador, pp. 113–114 — Envenenamento e Veneno em Armas', day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: now,
        };
        const combatants = currentWorld.encounter?.combatants.map(item => {
          if (item.entityId === attackerId) return { ...item, quickSheet: { ...(item.quickSheet || {}), weapons: consumed.character.weapons } };
          if (item.entityId === targetId) return { ...item, currentStrength: effect.character.attributes.strength.current, status: effect.character.brokenAttribute ? 'broken' as const : item.status, brokenAttribute: effect.character.brokenAttribute, conditions: [...effect.character.conditions], quickSheet: { ...(item.quickSheet || {}), attributes: effect.character.attributes, conditions: effect.character.conditions, brokenAttribute: effect.character.brokenAttribute } };
          return item;
        });
        nextWorld = { ...currentWorld, ...(currentWorld.encounter && combatants ? { encounter: { ...currentWorld.encounter, combatants, updatedAt: now } } : {}), journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]) }, updatedAt: now };
      }
      await characters.put(consumed.character);
      await worldStore.put(nextWorld);
      return nextWorld;
    });
    this.notify(world);
    if (!output) throw new Error('Não foi possível resolver o veneno da arma.');
    return output;
  }

  static async resolvePoisonRound(campaignId: string, characterId: string, rng: () => number = Math.random): Promise<{ character: Character; damageApplied: boolean; attribute: string; broke: boolean }> {
    let result: ReturnType<typeof PoisonRulesEngine.resolveRound> | null = null;
    const world = await withTransaction(['characters', 'world_state', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current || current.status === 'dead') throw new Error('Personagem vivo não encontrado.');
      result = PoisonRulesEngine.resolveRound(current, rng);
      await characterStore.put(result.character);
      const campaign = await tx.objectStore('campaign').get(campaignId);
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const now = Date.now();
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'mishap', title: `Dano periódico de veneno ${current.poisonState?.kind || ''}`,
        detail: result.damageApplied ? `${current.name} sofreu 1 dano em ${result.attribute} nesta rodada${result.broke ? '; o atributo Quebrou e o efeito cessou' : ''}.` : `${current.name} não sofreu dano adicional; o atributo do veneno já estava Quebrado.`,
        result: { hazard: 'poison_round', kind: current.poisonState?.kind, attribute: result.attribute, damageApplied: result.damageApplied, broke: result.broke, criticalPending: result.character.poisonState?.criticalPending || false, unconsciousMinutesRemaining: result.character.poisonState?.unconsciousMinutesRemaining ?? null },
        ruleSource: 'Livro do Jogador, p. 113–114 — Envenenamento', day: campaign?.currentDay || 1, period: campaign?.currentPeriod || 'Morning', hexId: campaign?.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const updated = { ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]), updatedAt: now }, updatedAt: now };
      await worldStore.put(updated);
      return updated;
    });
    this.notify(world);
    if (!result) throw new Error('Não foi possível avançar o efeito do veneno.');
    return { character: result.character, damageApplied: result.damageApplied, attribute: result.attribute, broke: result.broke };
  }

  static async applyAntidote(campaignId: string, characterId: string): Promise<void> {
    const world = await withTransaction(['characters', 'world_state', 'campaign'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const current = await characterStore.get(characterId);
      if (!current) throw new Error('Personagem não encontrado.');
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha não encontrada.');
      await characterStore.put(PoisonRulesEngine.antidote(current));
      const worldStore = tx.objectStore('world_state');
      const currentWorld = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const now = Date.now();
      const event: JourneyEvent = {
        id: crypto.randomUUID(), type: 'activity', title: 'Antídoto aplicado', detail: `O antídoto interrompeu o efeito do veneno de ${current.name}.`,
        result: { hazard: 'poison_antidote', characterId }, ruleSource: 'Livro do Jogador, p. 113–114 — Envenenamento',
        day: campaign.currentDay || 1, period: campaign.currentPeriod || 'Morning', hexId: campaign.currentHexId || undefined, status: 'resolved', createdAt: now,
      };
      const updated = { ...currentWorld, journey: { ...currentWorld.journey, events: retainJourneyEvents([...currentWorld.journey.events, event]) }, updatedAt: now };
      await worldStore.put(updated);
      return updated;
    });
    this.notify(world);
  }

  static async cookJourneyProvision(campaignId:string,ownerId:string,provisionId:string,cookId:string,requestedQuantity:number,hasCampfire:boolean,rng:()=>number=Math.random):Promise<{rawConsumed:number;foodProduced:number;storedSteps:number;overflow:number;cookRank:number;rollCap:number}> {
    if(!hasCampfire)throw new Error('Cozinhar durante uma jornada requer uma fogueira.');
    const requested=Math.max(1,Math.trunc(Number(requestedQuantity)||1));
    const now=Date.now();
    const outcome=await withTransaction(['campaign','characters','world_state','app_meta'],'readwrite',async tx=>{
      const campaignStore=tx.objectStore('campaign');
      const campaignRaw=await campaignStore.get(campaignId); if(!campaignRaw)throw new Error('Campanha não encontrada.');
      const campaign=CampaignService.normalize(campaignRaw);
      const worldStore=tx.objectStore('world_state'); const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      if(world.encounter?.status==='active')throw new Error('Resolva o encontro antes de cozinhar.');
      const characters=tx.objectStore('characters');
      const owner=await characters.get(ownerId) as Character|undefined;
      const cook=await characters.get(cookId) as Character|undefined;
      if(!owner||!cook)throw new Error('Personagem ou Cozinheiro não encontrado.');
      CharacterActionRulesEngine.assertCanAct(cook);
      const cookRank=normalizedTalentRank(cook,['Cozinheiro','Cook']);
      if(cookRank<1)throw new Error('A personagem precisa do talento Cozinheiro para transformar rendimentos em Comida.');
      const provisions=[...(owner.journeyProvisions||[])];
      const index=provisions.findIndex(batch=>batch.id===provisionId);
      if(index<0)throw new Error('Rendimento de jornada não encontrado.');
      const batch=provisions[index];
      if(!['vegetables','meat','fish'].includes(batch.kind))throw new Error('Cozinheiro só transforma Vegetais, Carne ou Peixe em Comida.');
      const day=campaign.currentDay||1;
      if(JourneyRulesEngine.provisionIsSpoiled(batch,day))throw new Error(`${provisionLabel(batch.kind)} estragaram e não podem mais ser cozidos.`);
      const rolledCap=JourneyRulesEngine.d6(rng);
      const cooked=JourneyRulesEngine.cookJourneyYield({available:batch.quantity,requested,cookRank,d6:rolledCap});
      const rollCap=cooked.cap; const rawConsumed=cooked.rawConsumed; const foodProduced=cooked.foodProduced;
      if(rawConsumed<1)throw new Error('Não há unidades válidas para cozinhar.');
      const remaining=batch.quantity-rawConsumed;
      if(remaining>0)provisions[index]={...batch,quantity:remaining};else provisions.splice(index,1);
      let ownerNext={...owner,journeyProvisions:provisions};
      await characters.put(ownerNext);

      const recipientCurrent=(ownerId===cookId?ownerNext:await characters.get(cookId)) as Character;
      let foodDie=recipientCurrent.resourceDice?.food||null;
      let storedSteps=0;
      for(let step=0;step<foodProduced;step+=1){
        const increased=WorldStateService.increaseResourceDie(foodDie,1);
        if(increased===foodDie)break;
        foodDie=increased; storedSteps+=1;
      }
      const overflow=foodProduced-storedSteps;
      const nextRecipientProvisions=[...(recipientCurrent.journeyProvisions||[])];
      if(overflow>0)nextRecipientProvisions.push({...JourneyRulesEngine.journeyProvision('food',overflow,day,undefined,crypto.randomUUID()),preparedByCookRank:cookRank});
      let recipientNext: Character = {...recipientCurrent,resourceDice:{...recipientCurrent.resourceDice,food:foodDie},journeyProvisions:nextRecipientProvisions};
      if(cookRank>=3&&storedSteps>0)recipientNext=addCookedFoodQuality(recipientNext,storedSteps);
      recipientNext={...recipientNext,encumbrance:EncumbranceRulesEngine.calculate(recipientNext)};
      await characters.put(recipientNext);

      const nextCampaign={...CampaignService.advanceState(campaign,1),updatedAt:now};
      const party=await characters.getAll() as Character[];
      for(const member of party){
        const current=member.id===recipientNext.id?recipientNext:member.id===ownerNext.id?ownerNext:member;
        await characters.put(CharacterTimeRulesEngine.advanceQuarters(current,1));
      }
      let nextWorld=WorldStateService.advanceTime(world,campaign,nextCampaign,1,now);
      const event:JourneyEvent={id:crypto.randomUUID(),type:'activity',title:'Cozinhar',detail:`${cook.name} cozinhou ${rawConsumed} unidade(s) de ${provisionLabel(batch.kind)} em uma fogueira (limite D6 ${rollCap}) e produziu ${foodProduced} unidade(s) de Comida${cookRank>=2?' incluindo +1 do Cozinheiro 2':''}. ${storedSteps} unidade(s) aumentaram o Dado de Recurso de Comida de ${cook.name}; ${overflow} ficaram em reserva. Um Quarto de Dia passou.`,result:{kind:'cook_journey_provision',ownerId,cookId,cookRank,rawConsumed,foodProduced,rollCap,storedSteps,overflow},ruleSource:'Livro do Jogador, p. 75 — Cozinheiro; pp. 38–39 — Itens Consumíveis; Cap. 7 — Coletar/Caçar/Pescar',day,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      nextWorld={...nextWorld,journey:{...nextWorld.journey,events:retainJourneyEvents([...nextWorld.journey.events,event]),quarterTravelProgress:0,updatedAt:now},updatedAt:now};
      await campaignStore.put(nextCampaign); await tx.objectStore('app_meta').put({id:'activeCampaignId',value:nextCampaign.id,updatedAt:now}); await worldStore.put(nextWorld);
      return {rawConsumed,foodProduced,storedSteps,overflow,cookRank,rollCap,world:nextWorld,previousCampaign:campaign,campaign:nextCampaign};
    });
    const cookedWorld=await CampaignService.reconcileElapsedTime(outcome.previousCampaign,outcome.campaign,'Cozinhar durante a jornada',outcome.world);
    CampaignService.notify(outcome.campaign);
    this.notify(cookedWorld);
    return {rawConsumed:outcome.rawConsumed,foodProduced:outcome.foodProduced,storedSteps:outcome.storedSteps,overflow:outcome.overflow,cookRank:outcome.cookRank,rollCap:outcome.rollCap};
  }

  static async storeCookedFoodUnit(campaignId:string,characterId:string,provisionId:string):Promise<{remaining:number;foodDie:ResourceDie}> {
    const outcome=await withTransaction(['campaign','characters','world_state'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId); if(!campaign)throw new Error('Campanha não encontrada.');
      const store=tx.objectStore('characters'); const character=await store.get(characterId) as Character|undefined; if(!character)throw new Error('Personagem não encontrado.');
      const provisions=[...(character.journeyProvisions||[])]; const index=provisions.findIndex(batch=>batch.id===provisionId);
      if(index<0||provisions[index].kind!=='food')throw new Error('Reserva de Comida não encontrada.');
      const before=character.resourceDice?.food||null; const after=WorldStateService.increaseResourceDie(before,1);
      if(after===before)throw new Error('O Dado de Recurso de Comida já está em D12; guarde a unidade em reserva.');
      const batch=provisions[index]; const remaining=batch.quantity-1; if(remaining>0)provisions[index]={...batch,quantity:remaining}; else provisions.splice(index,1);
      let updatedCharacter: Character = {...character,resourceDice:{...character.resourceDice,food:after},journeyProvisions:provisions};
      if((batch.preparedByCookRank||0)>=3)updatedCharacter=addCookedFoodQuality(updatedCharacter,1);
      updatedCharacter={...updatedCharacter,encumbrance:EncumbranceRulesEngine.calculate(updatedCharacter)};
      await store.put(updatedCharacter);
      const worldStore=tx.objectStore('world_state'); const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId); const now=Date.now();
      const event:JourneyEvent={id:crypto.randomUUID(),type:'resource',title:'Comida adicionada à reserva',detail:`${character.name} adicionou 1 unidade de Comida à reserva: ${before||'vazio'} → ${after}. Restam ${remaining} unidade(s) fora do Dado de Recurso.`,result:{kind:'store_cooked_food',remaining,before,after},ruleSource:'Livro do Jogador, pp. 38–39 — Itens Consumíveis',day:campaign.currentDay||1,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      const nextWorld={...world,journey:{...world.journey,events:retainJourneyEvents([...world.journey.events,event]),updatedAt:now},updatedAt:now}; await worldStore.put(nextWorld);
      return {remaining,foodDie:after,world:nextWorld};
    }); this.notify(outcome.world); return {remaining:outcome.remaining,foodDie:outcome.foodDie};
  }

  static async tanJourneyPelts(campaignId:string,ownerId:string,provisionId:string,tannerId:string,requestedQuantity:number,rng:()=>number=Math.random):Promise<{processed:number;leather:number;successes:number;destroyedPelts:number}> {
    const requested=Math.max(1,Math.trunc(Number(requestedQuantity)||1)); const now=Date.now();
    const outcome=await withTransaction(['campaign','characters','world_state','rolls'],'readwrite',async tx=>{
      const campaign=await tx.objectStore('campaign').get(campaignId); if(!campaign)throw new Error('Campanha não encontrada.');
      const characters=tx.objectStore('characters'); const owner=await characters.get(ownerId) as Character|undefined; const tanner=await characters.get(tannerId) as Character|undefined;
      if(!owner||!tanner)throw new Error('Personagem ou Curtidor não encontrado.'); CharacterActionRulesEngine.assertCanAct(tanner);
      const rank=normalizedTalentRank(tanner,['Curtidor','Tanner']); if(rank<1)throw new Error('A personagem precisa do talento Curtidor.');
      const provisions=[...(owner.journeyProvisions||[])]; const index=provisions.findIndex(batch=>batch.id===provisionId); if(index<0||provisions[index].kind!=='pelts')throw new Error('Reserva de Peles não encontrada.');
      const batch=provisions[index]; const tanning=JourneyRulesEngine.tanningBatchSize({available:batch.quantity,requested,d6:JourneyRulesEngine.d6(rng)}); const cap=tanning.cap; const processed=tanning.processed; if(processed<1)throw new Error('Não há Peles para curtir.');
      const crafting=normalizedSkill(tanner,['Crafting','Artesanato']); const roll=ForbiddenLandsDiceEngine.roll({base:tanner.attributes.strength.current,skill:crafting,gear:0},{characterId:tanner.id,sceneId:`journey:tanning:${campaignId}`,checkId:`journey:tanning:${provisionId}:${now}`,rng,criticalInjuries:tanner.criticalInjuryStates,skillName:'crafting',skillRankAtRoll:crafting,attributeName:'strength'}); await tx.objectStore('rolls').put(roll);
      const remaining=batch.quantity-processed; if(remaining>0)provisions[index]={...batch,quantity:remaining}; else provisions.splice(index,1);
      const leather=roll.successes>0?processed:0; const destroyedPelts=roll.successes>0?0:processed;
      await characters.put({...owner,journeyProvisions:provisions});
      if(leather>0){
        const current=(ownerId===tannerId?{...owner,journeyProvisions:provisions}:await characters.get(tannerId)) as Character;
        const gear=[...(current.gear||[])]; const leatherIndex=gear.findIndex(item=>item&&typeof item==='object'&&normalizeRuleName(String((item as any).name||''))==='couro');
        if(leatherIndex>=0){const item=gear[leatherIndex] as any;gear[leatherIndex]={...item,quantity:Math.max(1,Math.trunc(Number(item.quantity)||1))+leather};}
        else gear.push({name:'Couro',quantity:leather,encumbranceClass:'normal',source:'Livro do Jogador, p. 75 — Curtidor'});
        await characters.put({...current,gear});
      }
      const worldStore=tx.objectStore('world_state'); const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      const event:JourneyEvent={id:crypto.randomUUID(),type:'activity',title:'Curtir peles',detail:roll.successes>0?`${tanner.name} rolou Artesanato (${roll.successes} sucesso(s)) e transformou ${processed} Pele(s) em ${leather} Couro.`:`${tanner.name} falhou em Artesanato; ${processed} Pele(s) foram destruídas.`,result:{kind:'tan_pelts',ownerId,tannerId,rank,processed,leather,destroyedPelts,rollId:roll.id,cap},ruleSource:'Livro do Jogador, p. 75 — Curtidor',day:campaign.currentDay||1,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      const nextWorld={...world,journey:{...world.journey,events:retainJourneyEvents([...world.journey.events,event]),updatedAt:now},updatedAt:now}; await worldStore.put(nextWorld);
      return {processed,leather,destroyedPelts,successes:roll.successes,world:nextWorld};
    }); this.notify(outcome.world); return {processed:outcome.processed,leather:outcome.leather,successes:outcome.successes,destroyedPelts:outcome.destroyedPelts};
  }

  static async consumeRawJourneyProvision(campaignId:string,characterId:string,provisionId:string):Promise<{kind:import('../types').JourneyProvisionKind;remaining:number}> {
    const outcome=await withTransaction(['campaign','characters','world_state'],'readwrite',async tx=>{
      const campaignRaw=await tx.objectStore('campaign').get(campaignId);
      if(!campaignRaw)throw new Error('Campanha não encontrada.');
      const campaign=CampaignService.normalize(campaignRaw);
      const store=tx.objectStore('characters');
      const character=await store.get(characterId) as Character|undefined;
      if(!character)throw new Error('Personagem não encontrado.');
      CharacterActionRulesEngine.assertCanAct(character);
      const provisions=[...(character.journeyProvisions||[])];
      const index=provisions.findIndex(batch=>batch.id===provisionId);
      if(index<0)throw new Error('Rendimento de jornada não encontrado.');
      const batch=provisions[index];
      if(batch.kind==='pelts'||batch.kind==='food')throw new Error(batch.kind==='food'?'Comida preparada deve ser incorporada ao Dado de Recurso antes do consumo.':'Peles não podem ser consumidas como refeição.');
      const day=campaign.currentDay||1;
      if(JourneyRulesEngine.provisionIsSpoiled(batch,day))throw new Error(`${provisionLabel(batch.kind)} estragaram: precisam ser consumidos no mesmo dia em que foram obtidos.`);
      if(batch.quantity<1)throw new Error('Não resta nenhuma unidade deste rendimento.');
      const remaining=batch.quantity-1;
      if(remaining>0)provisions[index]={...batch,quantity:remaining};else provisions.splice(index,1);
      const fed=SurvivalRulesEngine.consumeDirectFood({...character,journeyProvisions:provisions});
      await store.put(fed);
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(campaignId),campaignId);
      const now=Date.now();
      const event:JourneyEvent={id:crypto.randomUUID(),type:'resource',title:`Refeição de ${provisionLabel(batch.kind)}`,detail:`${character.name} consumiu 1 unidade de ${provisionLabel(batch.kind)} cru(a) em vez de rolar o Dado de Recurso de Comida. Restam ${remaining}.`,result:{kind:'raw_journey_provision_meal',provisionKind:batch.kind,remaining},ruleSource:'Livro do Jogador, Cap. 7 — Coletar/Caçar/Pescar',day,period:campaign.currentPeriod||'Morning',hexId:campaign.currentHexId||undefined,status:'resolved',createdAt:now};
      const next={...world,journey:{...world.journey,events:retainJourneyEvents([...world.journey.events,event]),updatedAt:now},updatedAt:now};
      await worldStore.put(next);
      return {kind:batch.kind,remaining,world:next};
    });
    this.notify(outcome.world);
    return {kind:outcome.kind,remaining:outcome.remaining};
  }

  static async discardJourneyProvision(campaignId:string,characterId:string,provisionId:string):Promise<string> {
    const character=await withDbRetry(db=>db.get('characters',characterId)) as Character|undefined;
    if(!character)throw new Error('Personagem não encontrado.');
    const batch=(character.journeyProvisions||[]).find(item=>item.id===provisionId);
    if(!batch)throw new Error('Rendimento de jornada não encontrado.');
    await withDbRetry(db=>db.put('characters',{...character,journeyProvisions:(character.journeyProvisions||[]).filter(item=>item.id!==provisionId)}));
    return `${batch.quantity} unidade(s) de ${provisionLabel(batch.kind)}`;
  }

  static async rollResource(campaignId: string, characterId: string, resource: keyof Character['resourceDice'], rng: () => number = Math.random): Promise<ResourceRollResult> {
    const result = await withTransaction(['characters', 'world_state', 'campaign', 'rolls'], 'readwrite', async tx => {
      const characterStore = tx.objectStore('characters');
      const character = await characterStore.get(characterId);
      if (!character) throw new Error('Personagem não encontrado.');
      CharacterActionRulesEngine.assertCanAct(character);
      const before = character.resourceDice?.[resource] || null;
      const consumed = resource === 'food' || resource === 'water'
        ? SurvivalRulesEngine.consumeRation(character, resource, rng)
        : null;
      const sides = dieSides(before);
      const roll = consumed ? consumed.roll : sides ? Math.floor(rng() * sides) + 1 : null;
      const after = consumed ? consumed.after : roll !== null && roll <= 2 ? WorldStateService.stepResourceDie(before) : before;
      let updatedCharacter = consumed?.character || character;
      let empathyRecovered=false;
      if(resource==='food'&&roll!==null){
        const quality=(updatedCharacter.statusEffects||[]).find(effect=>effect.id==='journey:cook-rank3-food'&&(effect.resourceQualitySteps||0)>0);
        if(quality){
          const current=updatedCharacter.attributes.empathy.current; const max=updatedCharacter.attributes.empathy.max;
          empathyRecovered=current<max;
          const attributes=empathyRecovered?{...updatedCharacter.attributes,empathy:{...updatedCharacter.attributes.empathy,current:Math.min(max,current+Math.max(1,Number(quality.empathyRecoveryOnMeal)||1))}}:updatedCharacter.attributes;
          const depletedStep=after!==before;
          const statusEffects=(updatedCharacter.statusEffects||[]).flatMap(effect=>{
            if(effect.id!=='journey:cook-rank3-food')return [effect];
            const remaining=Math.max(0,Math.trunc(Number(effect.resourceQualitySteps)||0)-(depletedStep?1:0));
            return remaining>0?[{...effect,resourceQualitySteps:remaining,detail:`As próximas reservas de Comida preparadas por Cozinheiro 3 recuperam 1 Empatia por refeição enquanto ${remaining} passo(s) desse lote permanecerem no Dado de Recurso.`}]:[];
          });
          updatedCharacter={...updatedCharacter,attributes,statusEffects};
        }
      }
      let poisonSummary: string | undefined;
      if (resource === 'food' && roll !== null && character.foodPoisonState) {
        const contamination = character.foodPoisonState;
        const statusEffects = (updatedCharacter.statusEffects || []).filter(effect => after !== null || !effect.id.startsWith('monster-befouled-food:'));
        updatedCharacter = { ...updatedCharacter, foodPoisonState: after === null ? undefined : contamination, statusEffects };
        if (updatedCharacter.poisonState?.active) {
          poisonSummary = `a porção estava contaminada por veneno ${contamination.kind} de Potência ${contamination.potency}, mas ${character.name} já sofre um efeito total de veneno`;
        } else {
          const sceneId = `journey:contaminated-food:${campaignId}:${characterId}:${Date.now()}`;
          const resilience = normalizedSkill(updatedCharacter, ['Resilience', 'Resiliência']);
          const victimRoll = ForbiddenLandsDiceEngine.roll({ base: updatedCharacter.attributes.strength.current, skill: resilience, gear: 0 }, { characterId, sceneId, checkId: `contaminated-food-resistance:${sceneId}`, rng, criticalInjuries: updatedCharacter.criticalInjuryStates, skillName: 'resilience', attributeName: 'strength' });
          const poisonRoll = ForbiddenLandsDiceEngine.roll({ base: contamination.potency, skill: 0, gear: 0 }, { characterId: null, sceneId, checkId: `contaminated-food-potency:${sceneId}`, rng });
          await tx.objectStore('rolls').put(victimRoll);
          await tx.objectStore('rolls').put(poisonRoll);
          const exposure = PoisonRulesEngine.expose(updatedCharacter, contamination.kind, contamination.potency, victimRoll.successes, poisonRoll.successes);
          updatedCharacter = exposure.character;
          poisonSummary = exposure.totalEffect
            ? `a comida contaminada aplicou veneno ${contamination.kind} Potência ${contamination.potency} (${poisonRoll.successes} × ${victimRoll.successes})`
            : `a comida estava contaminada; resistiu ao veneno (${victimRoll.successes} × ${poisonRoll.successes}) e sofreu 1 dano em ${exposure.attribute}`;
        }
      }
      if (consumed) {
        if (consumed.roll !== null) await characterStore.put(updatedCharacter);
      } else if (after !== before) {
        const changed={...updatedCharacter,resourceDice:{...character.resourceDice,[resource]:after}};
        await characterStore.put({...changed,encumbrance:EncumbranceRulesEngine.calculate(changed)});
      }
      const campaign = await tx.objectStore('campaign').get(campaignId);
      const worldStore = tx.objectStore('world_state');
      const world = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const now = Date.now();
      const labels: Record<string, string> = { food: 'comida', water: 'água', arrows: 'flechas', torches: 'tochas' };
      const event = {
        id: crypto.randomUUID(), type: 'resource' as const, title: `Consumo de ${labels[resource]}`,
        detail: roll === null ? `${character.name} não possui ${labels[resource]}.` : `${character.name} consumiu ${labels[resource]} e rolou ${roll} em ${before}; reserva ${after === before ? 'mantida' : after ? `reduzida para ${after}` : 'esgotada'}${poisonSummary ? `; ${poisonSummary}` : ''}.`,
        day: campaign?.currentDay || 1, period: campaign?.currentPeriod || 'Morning', hexId: campaign?.currentHexId || undefined,
        status: 'resolved' as const, createdAt: now,
      };
      const nextWorld: WorldState = { ...world, journey: { ...world.journey, events: retainJourneyEvents([...world.journey.events, event]), updatedAt: now }, updatedAt: now };
      await worldStore.put(nextWorld);
      return { roll, before, after, depleted: after === null, poisonSummary, empathyRecovered, world: nextWorld };
    });
    this.notify(result.world);
    return { roll: result.roll, before: result.before, after: result.after, depleted: result.depleted, poisonSummary: result.poisonSummary, empathyRecovered: result.empathyRecovered };
  }

  static async startJourney(campaignId: string, plannedRouteHexIds?: string[]): Promise<WorldState> {
    const now = Date.now();
    const result = await withTransaction(['campaign', 'world_state'], 'readwrite', async tx => {
      const campaign = await tx.objectStore('campaign').get(campaignId);
      if (!campaign) throw new Error('Campanha não encontrada.');
      const worldStore = tx.objectStore('world_state');
      const world = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);
      const updated: WorldState = {
        ...world,
        journey: {
          ...world.journey,
          active: true,
          ...(Array.isArray(plannedRouteHexIds) ? { plannedRouteHexIds: plannedRouteHexIds.slice(0, 5) } : {}),
          updatedAt: now,
        },
        updatedAt: now,
      };
      await worldStore.put(updated);
      return updated;
    });
    this.notify(result);
    return result;
  }

  static async finishJourney(campaignId: string): Promise<{ world: WorldState; campaign: CampaignState; event: JourneyEvent }> {
    const now = Date.now();
    const result = await withTransaction(
      ['campaign', 'app_meta', 'world_state'],
      'readwrite',
      async tx => {
        const campaignStore = tx.objectStore('campaign');
        const campaignRaw = await campaignStore.get(campaignId);
        if (!campaignRaw) throw new Error('Campanha não encontrada.');
        const campaign = CampaignService.normalize(campaignRaw);
        const worldStore = tx.objectStore('world_state');
        const world = WorldStateService.normalize(await worldStore.get(campaignId), campaignId);

        if (world.encounter?.status === 'active') {
          throw new Error('Encerre o combate ou encontro ativo antes de finalizar a jornada.');
        }
        const blockingEvent = world.journey.events.find(
          e => e.status === 'open' && ['official_random_encounter', 'creature_encounter', 'lost', 'group_escape', 'trap'].includes(
            String((e.result?.mechanic as Record<string, unknown> | undefined)?.kind || '')
          )
        );
        if (blockingEvent) {
          throw new Error(`Resolva a pendência de jornada (${blockingEvent.title}) antes de finalizar a jornada.`);
        }

        const totalKm = world.journey.distanceKm || 0;
        const totalPeriods = world.journey.periodsTravelled || 0;
        const currentHexCoord = campaign.currentHexId || 'ponto atual';

        const finishEvent: JourneyEvent = {
          id: crypto.randomUUID(),
          type: 'travel',
          title: 'Jornada Finalizada',
          detail: `Jornada concluída em ${currentHexCoord}. Total percorrido: ${totalKm} km em ${totalPeriods} período(s). A rota foi encerrada e o grupo está seguro no local.`,
          result: {
            mechanic: { kind: 'finish_journey' },
            hexId: campaign.currentHexId,
            distanceKm: totalKm,
            periodsTravelled: totalPeriods,
          },
          ruleSource: JOURNEY_RULE_SOURCE,
          day: campaign.currentDay || 1,
          period: campaign.currentPeriod || 'Morning',
          hexId: campaign.currentHexId || undefined,
          status: 'resolved',
          createdAt: now,
        };

        const nextWorld: WorldState = {
          ...world,
          journey: {
            ...world.journey,
            active: false,
            plannedRouteHexIds: [],
            quarterTravelProgress: 0,
            lastCampDay: campaign.currentDay || 1,
            events: retainJourneyEvents([...world.journey.events, finishEvent]),
            updatedAt: now,
          },
          updatedAt: now,
        };

        await worldStore.put(nextWorld);
        return { world: nextWorld, campaign, event: finishEvent };
      }
    );

    this.notify(result.world);
    return result;
  }

  static async resumeJourneyRoute(campaignId: string, rng: () => number = Math.random) {
    const db = await getDb();
    const campaignRaw = await db.get('campaign', campaignId);
    if (!campaignRaw) throw new Error('Campanha não encontrada.');
    const campaign = CampaignService.normalize(campaignRaw);
    const world = WorldStateService.normalize(await db.get('world_state', campaignId), campaignId);

    if (world.encounter?.status === 'active') {
      throw new Error('Resolva o encontro ativo antes de retomar a rota.');
    }
    const openBlocking = world.journey.events.find(
      e => e.status === 'open' && ['official_random_encounter', 'creature_encounter', 'lost', 'group_escape', 'trap'].includes(
        String((e.result?.mechanic as Record<string, unknown> | undefined)?.kind || '')
      )
    );
    if (openBlocking) {
      throw new Error(`Resolva o evento pendente (${openBlocking.title}) antes de continuar a viagem.`);
    }

    const planned = world.journey.plannedRouteHexIds || [];
    if (!planned.length) {
      throw new Error('Nenhuma etapa restante na rota planejada.');
    }

    const targetId = planned[0];
    const targetHex = await db.get('individual_hexes', targetId);
    if (!targetHex) {
      throw new Error(`O próximo hex da rota (${targetId}) não foi encontrado.`);
    }

    const characters = CampaignPartyService.members(campaign, await db.getAll('characters'));
    const { TravelCommitService, mountedParty } = await import('./TravelCommitService');
    const mounted = mountedParty(world, characters);
    const isWater = MapTravelService.movementClassForHex(targetHex) === 'water';
    const travelValidation = await MapTravelService.validateMove(campaign.currentHexId, targetHex.id, {
      mounted,
      waterborne: isWater,
    });

    if (!travelValidation.valid) {
      throw new Error(travelValidation.reason || 'Deslocamento inválido para a próxima etapa da rota.');
    }

    const maritimeNavigation = isWater
      ? await TravelCommitService.prepareMaritimeNavigation({ sceneId: campaign.id, targetId: targetHex.id, rng })
      : null;
    const forcedMarchChecks = travelValidation.periods > 0 && !isWater
      ? await TravelCommitService.prepareForcedMarchChecks({ sceneId: campaign.id, targetId: targetHex.id, periods: travelValidation.periods, rng })
      : [];
    const overloadChecks = travelValidation.periods > 0
      ? await TravelCommitService.prepareOverloadChecks({ sceneId: campaign.id, targetId: targetHex.id, periods: travelValidation.periods, rng })
      : [];

    return TravelCommitService.commit({
      target: targetHex,
      periods: travelValidation.periods,
      distanceKm: travelValidation.distanceKm,
      actorId: campaign.activeCharacterId,
      profile: campaign.profile,
      overloadChecks,
      forcedMarchChecks,
      maritimeNavigation,
      rng,
    });
  }

  private static notify(world: WorldState) {
    if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('world-state-updated', { detail: world }));
  }
}
