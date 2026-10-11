import { withDbRetry, withTransaction } from '../db/db';
import { CampaignState, IndividualHex, JourneyMount, MapDiscovery, WorldState } from '../types';
import { CampaignService } from './CampaignService';
import { JourneyService, retainJourneyEvents } from './JourneyService';
import { MapTravelService } from './MapTravelService';
import { WorldStateService } from './WorldStateService';
import { EnvironmentalDirectorService } from './EnvironmentalDirectorService';
import { CharacterTimeRulesEngine } from './CharacterTimeRulesEngine';
import { EncumbranceRulesEngine, type OverloadFailureChoice } from './EncumbranceRulesEngine';
import { JourneyRulesEngine } from './JourneyRulesEngine';
import { SurvivalRulesEngine } from './SurvivalRulesEngine';
import type { Character, DiceRoll } from '../types';
import { ForbiddenLandsDiceEngine } from './ForbiddenLandsDiceEngine';
import { ForbiddenLandsCharacterRules } from './ForbiddenLandsCharacterRules';
import { CriticalInjuryRulesEngine } from './CriticalInjuryRulesEngine';
import { CharacterActionRulesEngine } from './CharacterActionRulesEngine';
import { BitterReachRuntimeService } from './BitterReachRuntimeService';
import { BitterReachModule } from './BitterReachModule';
import { CampaignModuleStateService } from './CampaignModuleStateService';
import { CampaignPartyService } from './CampaignPartyService';
import { OfficialEncounterFollowerEngine } from './OfficialEncounterFollowerEngine';

export type TravelCommitResult = {
  campaign: CampaignState;
  previousCampaign: CampaignState;
  hex: IndividualHex;
  sourceHex?: IndividualHex | null;
  worldState: WorldState;
  travelAdvanced: boolean;
  travelId: string | null;
};

const canonicalHex = (hex: IndividualHex | null | undefined) => hex?.coordinate || hex?.id || '';
const quarterKey = (campaign: CampaignState) => `${Math.max(1, Number(campaign.currentDay) || 1)}:${campaign.currentPeriod || 'Morning'}`;

type ForcedMarchStep = { quarterIndex: number; quarterKey: string; walkingCount: number; penalty: number };
type ForcedMarchRoll = { characterId: string; mountId: string | null; roll: DiceRoll; quarterIndex: number; quarterKey: string; walkingCount: number; penalty: number };
export type MaritimeNavigationRoll = { captainCharacterId:string; roll:DiceRoll; sailorRank:number; targetId:string };


export const readyMountFor = (mounts: JourneyMount[] | undefined, characterId: string) => (mounts || []).find(mount => mount.riderCharacterId === characterId && mount.status === 'ready') || null;
export const mountedParty = (world: WorldState, party: Character[]) => {
  const ableParty=party.filter(member=>member.status!=='dead');
  return ableParty.length>0&&ableParty.every(member=>Boolean(readyMountFor(world.journey.mounts,member.id)));
};

const talentRank=(character:Character,label:string)=>{
  const wanted=label.toLocaleLowerCase('pt-BR').normalize('NFD').replace(/[\u0300-\u036f]/g,'');
  const found=Object.entries(character.talents||{}).find(([name])=>name.toLocaleLowerCase('pt-BR').normalize('NFD').replace(/[\u0300-\u036f]/g,'')===wanted);
  return Math.max(0,Math.trunc(Number(found?.[1]||0)));
};
const survivalRank=(character:Character)=>Number(Object.entries(character.skills||{}).find(([name])=>ForbiddenLandsCharacterRules.skillKey(name)==='survival')?.[1]||0);

const validForcedMarchPool=(check:ForcedMarchRoll,member:Character,mount:JourneyMount|null,penalty:number)=>{
  const skillName=mount?'animal handling':'endurance';
  const skillRank=Number(Object.entries(member.skills||{}).find(([name])=>ForbiddenLandsCharacterRules.skillKey(name)===skillName)?.[1]||0);
  const modifiers=CriticalInjuryRulesEngine.modifiersForCheck(member.criticalInjuryStates||[],skillName,'strength');
  const attributeModifier=modifiers.filter(item=>item.effectKind==='attribute_modifier').reduce((total,item)=>total+item.modifier,0);
  const skillModifier=modifiers.filter(item=>item.effectKind==='skill_modifier').reduce((total,item)=>total+item.modifier,0);
  const expectedBase=Math.max(0,Math.min(30,Math.trunc((mount?.strength??member.attributes.strength.current)+attributeModifier)));
  const expectedSkill=Math.max(-30,Math.min(30,Math.max(0,skillRank)+skillModifier+penalty));
  const baseDice=check.roll.results.filter(die=>die.type==='base').length;
  const positiveSkillDice=check.roll.results.filter(die=>die.type==='skill').length;
  const negativeSkillDice=check.roll.results.filter(die=>die.type==='negativeSkill').length;
  return check.roll.skillRankAtRoll===skillRank
    &&baseDice===expectedBase
    &&positiveSkillDice===Math.max(0,expectedSkill)
    &&negativeSkillDice===Math.max(0,-expectedSkill)
    &&check.roll.results.every(die=>die.type==='base'||die.type==='skill'||die.type==='negativeSkill')
    &&JSON.stringify(check.roll.ruleModifiersApplied||[])===JSON.stringify(modifiers);
};

const forcedMarchPlan = (campaign: CampaignState, world: WorldState, periodCost: number, mounted=false): ForcedMarchStep[] => {
  // A malformed preflight must never enter the quarter-building loop.
  // Four quarters is the official daily upper bound; longer travel is split.
  if (!Number.isFinite(periodCost) || periodCost < 0 || periodCost > 4)
    throw new Error('Custo de viagem inválido ou maior que quatro Quartos de Dia. Divida a viagem.');
  if (periodCost === 0) return [];
  const walking = world.journey.walkingDay === campaign.currentDay ? Number(world.journey.walkingQuartersToday || 0) : 0;
  const failures = world.journey.walkingDay === campaign.currentDay ? Number(world.journey.forcedMarchFailuresToday || 0) : 0;
  const progress = Math.max(0, Math.min(0.999999, Number(world.journey.quarterTravelProgress || 0)));
  const positions = [0];
  for (let position = 1 - progress; position < periodCost - 1e-9; position += 1) positions.push(position);
  const steps: ForcedMarchStep[] = [];
  for (const position of positions) {
    const quarterIndex = Math.floor(progress + position + 1e-9);
    const walkingCount = walking + failures + quarterIndex;
    if (walkingCount < 2) continue;
    const rule = JourneyRulesEngine.forcedMarch(walkingCount,mounted);
    if (!rule.allowed) throw new Error('O grupo já caminhou quatro Quartos de Dia. Descanse até o próximo dia para voltar a viajar.');
    const nextCampaign = quarterIndex > 0 ? CampaignService.advanceState(campaign, quarterIndex) : campaign;
    const key = quarterKey(nextCampaign);
    if (quarterIndex === 0 && world.journey.forcedMarchCheckQuarterKey === key) continue;
    steps.push({ quarterIndex, quarterKey: key, walkingCount, penalty: rule.penalty });
  }
  return steps;
};

/**
 * Grava deslocamento, tempo, descoberta do mapa, jornada e ambiente em uma única
 * transação. A máquina climática avança somente quando o grupo realmente muda
 * de hex. Repetir a mesma ordem ou tocar PLAY não consome outra etapa.
 */
export class TravelCommitService {
  static async prepareForcedMarchChecks(input:{sceneId:string;targetId:string;periods?:number;rng?:()=>number}):Promise<ForcedMarchRoll[]>{
    const campaign=await CampaignService.getActive(false);
    if(!campaign||campaign.id!==input.sceneId)return [];
    const world=WorldStateService.normalize(await withDbRetry(db=>db.get('world_state',campaign.id)),campaign.id);
    const target=await withDbRetry(db=>db.get('individual_hexes',input.targetId));
    if(target&&MapTravelService.movementClassForHex(target)==='water')return [];
    const fogEvent=[...world.journey.events].reverse().find(event=>event.status==='open'&&event.type==='mishap'&&(event.result?.mechanic as Record<string,unknown>|undefined)?.kind==='fog'&&event.day===(campaign.currentDay||1)&&event.period===(campaign.currentPeriod||'Morning'));
    const difficultFog=Boolean(fogEvent&&target&&MapTravelService.movementClassForHex(target)==='difficult');
    if(difficultFog)return [];
    const requestedPeriods=Number(input.periods??1);
    if (!Number.isFinite(requestedPeriods) || requestedPeriods < 0)
      throw new Error('Número de Quartos de Dia inválido; refaça a preparação da marcha.');
    const periodCost=requestedPeriods*(fogEvent?2:1);
    const characters=CampaignPartyService.members(campaign,await withDbRetry(db=>db.getAll('characters')));
    const mounted=mountedParty(world,characters);
    const plan=forcedMarchPlan(campaign,world,periodCost,mounted);
    if(!plan.length)return [];
    return plan.flatMap(step=>characters.filter(member=>member.status!=='dead').map(member=>{
      const mount=readyMountFor(world.journey.mounts,member.id);
      const requiredSkill=mount?'animal handling':'endurance';
      const skill=Object.entries(member.skills||{}).find(([name])=>ForbiddenLandsCharacterRules.skillKey(name)===requiredSkill)?.[1]||0;
      return {...step,characterId:member.id,mountId:mount?.id||null,roll:ForbiddenLandsDiceEngine.roll({base:mount?.strength??member.attributes.strength.current,skill:Number(skill),gear:0,modifier:step.penalty},{
        characterId:member.id,sceneId:input.sceneId,checkId:`forced-march:${input.sceneId}:${step.quarterKey}:${member.id}`,rng:input.rng,
        criticalInjuries:member.criticalInjuryStates,skillName:mount?'animal_handling':'endurance',attributeName:'strength',skillRankAtRoll:Number(skill),
      })};
    }));
  }

  static async prepareMaritimeNavigation(input:{sceneId:string;targetId:string;rng?:()=>number}):Promise<MaritimeNavigationRoll|null>{
    const campaign=await CampaignService.getActive(false);
    if(!campaign||campaign.id!==input.sceneId)return null;
    const target=await withDbRetry(db=>db.get('individual_hexes',input.targetId));
    if(!target||MapTravelService.movementClassForHex(target)!=='water')return null;
    const world=WorldStateService.normalize(await withDbRetry(db=>db.get('world_state',campaign.id)),campaign.id);
    const maritime=world.journey.maritime;
    if(!maritime?.active)throw new Error('Água aberta exige iniciar uma Viagem Marítima e escolher uma capitã.');
    if(['aground','sunk','abandoned'].includes(maritime.hullStatus))throw new Error(`A embarcação não pode navegar enquanto estiver ${maritime.hullStatus==='aground'?'encalhada':maritime.hullStatus==='sunk'?'afundada':'abandonada'}.`);
    const captain=await withDbRetry(db=>db.get('characters',maritime.captainCharacterId));
    CampaignPartyService.assertMember(campaign, captain);
    if(!captain||captain.status==='dead')throw new Error('A capitã da embarcação não está disponível.');
    const skill=survivalRank(captain);
    const sailorRank=talentRank(captain,'Marinheiro');
    const modifier=sailorRank>=1?1:0;
    const artifact=sailorRank>=3?8 as const:undefined;
    const roll=ForbiddenLandsDiceEngine.roll({base:captain.attributes.wits.current,skill,gear:0,modifier,artifact},{characterId:captain.id,sceneId:campaign.id,checkId:`maritime-nav:${campaign.id}:${target.id}:${campaign.currentDay||1}:${campaign.currentPeriod||'Morning'}`,rng:input.rng,criticalInjuries:captain.criticalInjuryStates,skillName:'survival',skillRankAtRoll:skill,attributeName:'wits'});
    return {captainCharacterId:captain.id,roll,sailorRank,targetId:target.id};
  }

  static async prepareOverloadChecks(input: { sceneId: string; targetId: string; periods: number; characterIds?: string[]; rng?: () => number }): Promise<Array<{ characterId: string; roll: DiceRoll; failureChoice?: OverloadFailureChoice }>> {
    const campaign = await CampaignService.getActive(false);
    if (!campaign || campaign.id !== input.sceneId) throw new Error('A campanha ativa mudou antes da checagem de carga.');
    const members = CampaignPartyService.members(campaign, await withDbRetry(db => db.getAll('characters')));
    const selectedIds = input.characterIds ? new Set(input.characterIds) : null;
    let alreadyChecked = new Set<string>();
    if (!selectedIds) {
      const campaign = await CampaignService.getActive(false);
      if (campaign?.id === input.sceneId) {
        const world = WorldStateService.normalize(await withDbRetry(db => db.get('world_state', campaign.id)), campaign.id);
        if (world.journey.overloadCheckedQuarterKey === quarterKey(campaign)) alreadyChecked = new Set(world.journey.overloadCheckedCharacterIds || []);
      }
    }
    const overloaded = members.filter(member => (!selectedIds || selectedIds.has(member.id)) && !alreadyChecked.has(member.id) && EncumbranceRulesEngine.calculate(member).overloaded);
    const checks: Array<{ characterId: string; roll: DiceRoll; failureChoice?: OverloadFailureChoice }> = [];
    for (const member of overloaded) {
      const roll = EncumbranceRulesEngine.rollOverloadCheck(member, {
        sceneId: input.sceneId,
        checkId: `overload:${input.sceneId}:${input.targetId}:${member.id}:${Date.now()}`,
        rng: input.rng,
      });
      checks.push({ characterId: member.id, roll });
    }
    return checks;
  }

  static async commit(input: {
    target: IndividualHex;
    periods?: number;
    distanceKm?: number;
    actorId?: string | null;
    profile?: CampaignState['profile'];
    overloadChecks?: Array<{ characterId: string; roll: DiceRoll; failureChoice?: OverloadFailureChoice }>;
    forcedMarchChecks?: ForcedMarchRoll[];
    maritimeNavigation?: MaritimeNavigationRoll | null;
    rng?:()=>number;
  }): Promise<TravelCommitResult> {
    const requestedPeriods = Number(input.periods ?? 0);
    const requestedDistanceKm = Number(input.distanceKm ?? requestedPeriods * 10);
    // U18: malformed or negative input must not bypass cost/roll gates or
    // contaminate the calendar and travel progress with NaN/Infinity.
    if (!Number.isFinite(requestedPeriods) || !Number.isFinite(requestedDistanceKm) ||
        requestedPeriods < 0 || requestedDistanceKm < 0) {
      throw new Error('Tempo ou distância de viagem inválidos; refaça a escolha do hexágono.');
    }
    const snapshot = await CampaignService.getActive(true);
    if (!snapshot) throw new Error('Campanha indisponível.');

    // A leitura é feita antes da transação para que o Diretor Ambiental possa
    // analisar o hex atual e até cinco passos futuros sem abrir outra conexão.
    const allHexes = await withDbRetry(db => db.getAll('individual_hexes'));
    const preTravelHex = allHexes.find(hex => hex.id === snapshot.currentHexId || hex.coordinate === snapshot.currentHexId) || null;
    if (requestedPeriods > 0 && preTravelHex && BitterReachModule.isBitterReachHex(preTravelHex)) {
      await BitterReachRuntimeService.ensureWeather(snapshot, preTravelHex, input.rng || Math.random);
    }
    const neighbors = await MapTravelService.getAvailableNeighborIds(input.target.coordinate);
    const updatedHex: IndividualHex = { ...input.target, neighbors, discovered: true, visited: true };
    const now = Date.now();

    const result = await withTransaction(
      ['campaign', 'app_meta', 'map_discovery', 'individual_hexes', 'world_state', 'characters', 'rolls', 'npcs'],
      'readwrite',
      async tx => {
        const campaignStore = tx.objectStore('campaign');
        const activeMeta = await tx.objectStore('app_meta').get('activeCampaignId');
        if (activeMeta?.value && activeMeta.value !== snapshot.id) {
          throw new Error('A campanha ativa mudou antes de confirmar a viagem. Selecione o destino novamente.');
        }
        const persistedCampaign = await campaignStore.get(snapshot.id);
        // On first launch getActive(true) can return a not-yet-persisted campaign.
        // Never use that bootstrap fallback to resurrect a DELETED saved campaign.
        if (!persistedCampaign && activeMeta?.value) {
          throw new Error('A campanha foi removida antes de confirmar a viagem.');
        }
        const current = CampaignService.normalize(persistedCampaign || snapshot);
        const partyInTx = CampaignPartyService.members(current, await tx.objectStore('characters').getAll());
        const worldStore = tx.objectStore('world_state');
        const world = WorldStateService.normalize(await worldStore.get(current.id), current.id);
        const previousHex = allHexes.find(hex => hex.id === current.currentHexId || hex.coordinate === current.currentHexId) || null;
        const previousCanonical = canonicalHex(previousHex) || String(current.currentHexId || '');
        const targetCanonical = canonicalHex(updatedHex);
        const changesHex = Boolean(!current.currentHexId || previousCanonical !== targetCanonical);
        // Initial placement is free; movement after placement is never free.
        // Reject here, inside the transaction, before discovering or mutating a hex.
        if (changesHex && current.currentHexId && requestedPeriods <= 0) {
          throw new Error('Deslocamento entre hexágonos exige custo de viagem maior que zero.');
        }
        const adjacent = previousHex && (MapTravelService.getNeighborIds?.(previousHex.coordinate) || previousHex.neighbors || []);
        if (changesHex && previousHex && adjacent?.length && !adjacent.includes(targetCanonical)) {
          throw new Error('O destino não é adjacente ao hexágono atual. Escolha um dos vizinhos disponíveis.');
        }
        if (changesHex && world.encounter?.status === 'active') {
          throw new Error('Encerre ou resolva o encontro ativo antes de mover o grupo para outro hex.');
        }
        if (changesHex && world.journey.events.some(event => event.status === 'open' && ['lost','group_escape','trap','official_random_encounter','creature_encounter','maritime_overboard','maritime_item_overboard'].includes(String((event.result?.mechanic as Record<string, unknown> | undefined)?.kind || '')))) {
          throw new Error('O grupo ainda precisa resolver o encontro ou obstáculo pendente antes de avançar no mapa.');
        }
        const fogEvent = changesHex ? [...world.journey.events].reverse().find(event =>
          event.status === 'open' && event.type === 'mishap' &&
          (event.result?.mechanic as Record<string, unknown> | undefined)?.kind === 'fog' &&
          event.day === (current.currentDay || 1) && event.period === (current.currentPeriod || 'Morning')) : undefined;
        const difficultFog = Boolean(fogEvent && MapTravelService.movementClassForHex(updatedHex) === 'difficult');
        if (difficultFog && previousHex) {
          if (partyInTx.some(member => member.survivalState?.mustSleep)) {
            throw new Error('Uma personagem desabada precisa dormir antes de continuar a viagem.');
          }
          const nextCampaign = { ...CampaignService.advanceState(current, 1), updatedAt: now };
          const memberStore = tx.objectStore('characters');
          for (const member of CampaignPartyService.members(current, await memberStore.getAll())) await memberStore.put(CharacterTimeRulesEngine.advanceQuarters(member, 1));
          const consumedWorld = WorldStateService.advanceTime({
            ...world,
            journey: {
              ...world.journey,
              quarterTravelProgress: 0,
              periodsTravelled: world.journey.periodsTravelled + 1,
              events: world.journey.events.map(event => event.id === fogEvent?.id
                ? { ...event, status: 'resolved' as const, result: { ...(event.result || {}), mapPenaltyApplied: true }, detail: `${event.detail} O grupo permaneceu no hex inicial por um Quarto de Dia.` }
                : event),
              updatedAt: now,
            },
            updatedAt: now,
          }, current, nextCampaign, 1, now);
          await campaignStore.put(nextCampaign);
          await tx.objectStore('app_meta').put({ id: 'activeCampaignId', value: nextCampaign.id, updatedAt: now });
          await worldStore.put(consumedWorld);
          return { campaign: nextCampaign, previousCampaign: current, hex: previousHex, sourceHex: previousHex, worldState: consumedWorld, travelAdvanced: false, travelId: null };
        }

        // Impede atravessar dois QDs em um único commit: um encontro pode ocorrer
        // no primeiro e deve interromper a viagem antes do segundo. Execute cada
        // QD separadamente (inclusive em clima/neblina) antes de consumir recursos.
        const projectedPeriodCost = changesHex ? requestedPeriods * (fogEvent ? 2 : 1) : 0;
        if (changesHex && JourneyRulesEngine.travelCrossesMultipleQuarters(Number(world.journey.quarterTravelProgress || 0), projectedPeriodCost)) {
          throw new Error('A viagem cruza mais de um Quarto de Dia. Divida o deslocamento em etapas para resolver cada encontro antes de avançar.');
        }
        // Uma segunda confirmação do mesmo destino não equivale a outra viagem.
        const memberStore = tx.objectStore('characters');
        const party = partyInTx;
        const isWaterTravel=changesHex&&MapTravelService.movementClassForHex(updatedHex)==='water';
        let maritimeAfter=world.journey.maritime;
        const maritimeEvents: import('../types').JourneyEvent[]=[];
        let maritimeCaptainId:string|undefined;
        let maritimeSailorRank=0;
        if(isWaterTravel){
          const maritime=world.journey.maritime;
          if(!maritime?.active)throw new Error('Água aberta exige uma Viagem Marítima ativa e uma capitã escolhida.');
          if(['aground','sunk','abandoned'].includes(maritime.hullStatus))throw new Error(`A embarcação não pode navegar enquanto estiver ${maritime.hullStatus==='aground'?'encalhada':maritime.hullStatus==='sunk'?'afundada':'abandonada'}.`);
          const captain=party.find(member=>member.id===maritime.captainCharacterId);
          if(!captain||captain.status==='dead')throw new Error('A capitã da embarcação não está disponível.');
          const prepared=input.maritimeNavigation;
          if(!prepared||prepared.captainCharacterId!==captain.id||prepared.targetId!==updatedHex.id)throw new Error('Faça a rolagem de DESBRAVAR da capitã antes de navegar para o próximo hexágono.');
          const skill=survivalRank(captain); const sailorRank=talentRank(captain,'Marinheiro'); const modifier=sailorRank>=1?1:0; const artifact=sailorRank>=3?8:undefined;
          const expectedCheckId=`maritime-nav:${current.id}:${updatedHex.id}:${current.currentDay||1}:${current.currentPeriod||'Morning'}`;
          if(prepared.sailorRank!==sailorRank||prepared.roll.characterId!==captain.id||prepared.roll.sceneId!==current.id||prepared.roll.checkId!==expectedCheckId||prepared.roll.skillName!=='survival'||prepared.roll.attributeName!=='wits'||prepared.roll.skillRankAtRoll!==skill||prepared.roll.isPushed||prepared.roll.prideUsed||prepared.roll.assistedBy?.length||prepared.roll.successes!==ForbiddenLandsDiceEngine.countSuccesses(prepared.roll.results))throw new Error('Rolagem marítima inválida; refaça DESBRAVAR com a capitã atual.');
          const expectedBase=Math.max(0,captain.attributes.wits.current+(prepared.roll.ruleModifiersApplied||[]).filter(item=>item.effectKind==='attribute_modifier').reduce((sum,item)=>sum+item.modifier,0));
          const baseDice=prepared.roll.results.filter(die=>die.type==='base').length; const skillDice=prepared.roll.results.filter(die=>die.type==='skill').length; const negDice=prepared.roll.results.filter(die=>die.type==='negativeSkill').length; const artDice=prepared.roll.results.filter(die=>die.type==='artifact');
          const skillMod=(prepared.roll.ruleModifiersApplied||[]).filter(item=>item.effectKind==='skill_modifier').reduce((sum,item)=>sum+item.modifier,0); const expectedSkill=skill+skillMod+modifier;
          if(baseDice!==expectedBase||skillDice!==Math.max(0,expectedSkill)||negDice!==Math.max(0,-expectedSkill)||(artifact?artDice.length!==1||artDice[0].sides!==8:artDice.length!==0))throw new Error('Pool marítimo inválido; confira Sobrevivência e o talento Marinheiro.');
          await tx.objectStore('rolls').put(prepared.roll);
          maritimeCaptainId=captain.id; maritimeSailorRank=sailorRank;
          if(prepared.roll.successes<=0){
            const mishap=JourneyRulesEngine.maritimeMishap(input.rng||Math.random);
            const baseEvent:import('../types').JourneyEvent={id:crypto.randomUUID(),type:'mishap',title:`Viagem Marítima — ${mishap.title}`,detail:mishap.effect,result:{mechanic:mishap.mechanic,mishapRoll:mishap.roll,navigationRollId:prepared.roll.id,captainCharacterId:captain.id,vesselName:maritime.vesselName},ruleSource:'Livro do Jogador, Cap. 7 — Viagem Marítima',day:current.currentDay||1,period:current.currentPeriod||'Morning',hexId:current.currentHexId||undefined,status:'open',createdAt:now};
            if(mishap.roll===1){
              const nextCampaign={...CampaignService.advanceState(current,1),updatedAt:now};
              for(const member of party){let advanced=CharacterTimeRulesEngine.advanceQuarters(member,1);if(member.id===captain.id&&sailorRank>=2)advanced=SurvivalRulesEngine.recoverAfterQuarter(advanced);await memberStore.put(advanced);}
              const resolved={...baseEvent,status:'resolved' as const,detail:`${baseEvent.detail} O Quarto de Dia foi consumido sem deslocamento.`};
              const nextWorld=WorldStateService.advanceTime({...world,journey:{...world.journey,maritime:{...maritime,lastNavigationRollId:prepared.roll.id,lastMishapRoll:mishap.roll,updatedAt:now},periodsTravelled:world.journey.periodsTravelled+1,events:retainJourneyEvents([...world.journey.events,resolved]),updatedAt:now},updatedAt:now},current,nextCampaign,1,now);
              await campaignStore.put(nextCampaign);await tx.objectStore('app_meta').put({id:'activeCampaignId',value:nextCampaign.id,updatedAt:now});await worldStore.put(nextWorld);
              return {campaign:nextCampaign,previousCampaign:current,hex:previousHex||updatedHex,sourceHex:previousHex,worldState:nextWorld,travelAdvanced:false,travelId:null};
            }
            if(mishap.roll===3){
              const second=ForbiddenLandsDiceEngine.roll({base:captain.attributes.wits.current,skill,gear:0,modifier,artifact:artifact as 8|undefined},{characterId:captain.id,sceneId:current.id,checkId:`maritime-whirlpool:${current.id}:${updatedHex.id}:${now}`,rng:input.rng,criticalInjuries:captain.criticalInjuryStates,skillName:'survival',skillRankAtRoll:skill,attributeName:'wits'});await tx.objectStore('rolls').put(second);baseEvent.result={...(baseEvent.result||{}),whirlpoolRollId:second.id,whirlpoolSuccesses:second.successes};if(second.successes>0){baseEvent.status='resolved';baseEvent.detail+=` ${captain.name} venceu o redemoinho (${second.successes} sucesso(s)).`;}else{maritimeAfter={...maritime,hullStatus:'aground',lastNavigationRollId:prepared.roll.id,lastMishapRoll:mishap.roll,updatedAt:now};baseEvent.detail+=` ${captain.name} falhou; ${maritime.vesselName} encalhou e precisa de reparo antes de continuar.`;}
            }else if(mishap.roll===4){const hours=JourneyRulesEngine.d6(input.rng||Math.random);maritimeAfter={...maritime,hullStatus:'leaking',leakHoursRemaining:hours,lastNavigationRollId:prepared.roll.id,lastMishapRoll:mishap.roll,updatedAt:now};baseEvent.result={...(baseEvent.result||{}),leakHoursRemaining:hours};baseEvent.detail+=` O casco afundará em ${hours} hora(s) se o vazamento não for reparado.`;}
            else if(mishap.roll===6){maritimeAfter={...maritime,hullStatus:'aground',lastNavigationRollId:prepared.roll.id,lastMishapRoll:mishap.roll,updatedAt:now};}
            else maritimeAfter={...maritime,lastNavigationRollId:prepared.roll.id,lastMishapRoll:mishap.roll,updatedAt:now};
            maritimeEvents.push(baseEvent);
          }else{
            maritimeAfter={...maritime,lastNavigationRollId:prepared.roll.id,lastMishapRoll:undefined,updatedAt:now};
            maritimeEvents.push({id:crypto.randomUUID(),type:'travel',title:'Desbravar no mar',detail:`${captain.name} conduziu ${maritime.vesselName} com ${prepared.roll.successes} sucesso(s) em Sobrevivência${sailorRank>=1?' (Marinheiro aplicado)':''}.`,result:{mechanic:{kind:'maritime_navigation'},navigationRollId:prepared.roll.id,sailorRank},ruleSource:'Livro do Jogador, Cap. 7 — Viagem Marítima',day:current.currentDay||1,period:current.currentPeriod||'Morning',hexId:updatedHex.id,status:'resolved',createdAt:now});
          }
        }
        if (changesHex && requestedPeriods > 0 && !isWaterTravel) {
          const guideAssignment = world.journey.assignments.find(item => item.role === 'guide');
          if (!guideAssignment) throw new Error('Escolha manualmente quem será o Guia antes de viajar.');
          const guide = party.find(member => member.id === guideAssignment.characterId && member.status !== 'dead');
          if (!guide || !CharacterActionRulesEngine.canAct(guide)) throw new Error('O Guia escolhido não está disponível para Desbravar.');
          const leadEvent = [...world.journey.events].reverse().find(event => {
            const result = event.result as Record<string, unknown> | undefined;
            return event.day === (current.currentDay || 1)
              && event.period === (current.currentPeriod || 'Morning')
              && result?.activity === 'leadWay'
              && result?.actorId === guide.id;
          });
          if (!leadEvent) throw new Error(`${guide.name} precisa rolar Desbravar neste Quarto de Dia antes de mover o grupo.`);
          const mechanic = leadEvent.result?.mechanic as Record<string, unknown> | undefined;
          if (leadEvent.status === 'open') throw new Error('Resolva o infortúnio de Desbravar antes de mover o grupo.');
          if (leadEvent.result?.noMapProgressApplied === true || mechanic?.noMapProgress === true) {
            throw new Error('O resultado de Desbravar consumiu o Quarto de Dia sem progresso no mapa.');
          }
        }

        const dailyWeather = world.journey.dailyWeather?.day === (current.currentDay || 1) ? world.journey.dailyWeather : undefined;
        if (changesHex && requestedPeriods > 0 && dailyWeather) {
          const weather = JourneyRulesEngine.dailyWeather(dailyWeather.windRoll, dailyWeather.rainfallRoll, dailyWeather.temperatureRoll);
          const weatherChecksRequired = weather.marchCheckRequired || weather.coldCheckRequired || weather.thirstCheckRequired;
          const weatherQuarterKey = quarterKey(current);
          if (weatherChecksRequired && world.journey.weatherCheckQuarterKey !== weatherQuarterKey) {
            throw new Error('Resolva os testes de clima da jornada antes de mover o grupo neste Quarto de Dia.');
          }
          if (weatherChecksRequired && world.journey.weatherStoppedQuarterKey === weatherQuarterKey) {
            throw new Error('A falha no teste de clima interrompeu a marcha neste Quarto de Dia. Descanse e tente novamente no próximo.');
          }
        }
        const bitterWeather = world.journey.bitterReachWeather?.day === (current.currentDay || 1) ? world.journey.bitterReachWeather : undefined;
        if (changesHex && requestedPeriods > 0 && previousHex && BitterReachModule.isBitterReachHex(previousHex) && bitterWeather) {
          const key = quarterKey(current);
          const required = bitterWeather.hikeEnduranceRequired || bitterWeather.hikeEnduranceRequiredSnow;
          if (required && world.journey.bitterReachTravelCheckQuarterKey !== key) {
            throw new Error('Resolva os testes de marcha do clima do Alcance Amargo antes de mover o grupo neste Quarto de Dia.');
          }
          if (required && world.journey.bitterReachTravelStoppedQuarterKey === key) {
            throw new Error('A tempestade/nevasca do Alcance Amargo interrompeu a marcha neste Quarto de Dia.');
          }
        }
        if (changesHex && requestedPeriods > 0) {
          const movement = MapTravelService.movementCostForHex(updatedHex, { mounted: mountedParty(world, party), waterborne:isWaterTravel });
          // Recheck terrain and mount state inside this transaction; callers may request slower travel,
          // but cannot claim a faster trip than the terrain allows.
          if (movement.movementClass !== 'water' && requestedPeriods + 1e-9 < movement.periods) {
            throw new Error(`The requested travel time is shorter than the terrain cost (${movement.periods} quarter-day(s)).`);
          }
        }
        const periodCost = changesHex ? requestedPeriods * (fogEvent ? 2 : 1) : 0;
        if (periodCost > 0 && partyInTx.some(member => member.survivalState?.mustSleep)) {
          throw new Error('Uma personagem desabada precisa dormir antes de continuar a viagem.');
        }
        const distanceKm = changesHex ? requestedDistanceKm : 0;
        const walkingToday=world.journey.walkingDay===current.currentDay?Number(world.journey.walkingQuartersToday||0):0;
        const failedMarches=world.journey.walkingDay===current.currentDay?Number(world.journey.forcedMarchFailuresToday||0):0;
        const marchParty=party.filter(member=>member.status!=='dead');
        const marchPlan=changesHex&&periodCost>0&&!isWaterTravel?forcedMarchPlan(current,world,periodCost,mountedParty(world,party)):[];
        const marchChecks=changesHex&&periodCost>0&&!isWaterTravel?(input.forcedMarchChecks||[]):[];
        if(marchChecks.length!==marchPlan.length*marchParty.length)throw new Error(marchPlan.length?'Faça o teste de Resiliência de marcha forçada de cada Quarto de Dia para todo o grupo antes de caminhar.':'A rolagem de marcha forçada não corresponde ao Quarto de Dia atual.');
        for(const step of marchPlan){
          const stepChecks=marchChecks.filter(check=>check.quarterIndex===step.quarterIndex&&check.quarterKey===step.quarterKey);
          if(stepChecks.length!==marchParty.length||marchParty.some(member=>!stepChecks.some(check=>check.characterId===member.id)))throw new Error('Faça o teste de Resiliência de marcha forçada para todo o grupo em cada Quarto de Dia.');
          for(const check of stepChecks){
            const member=marchParty.find(candidate=>candidate.id===check.characterId)!;
            const mount=readyMountFor(world.journey.mounts,member.id);
            const expectedSkill=mount?'animal_handling':'endurance';
            if(check.mountId!==(mount?.id||null)||check.walkingCount!==step.walkingCount||check.penalty!==step.penalty||check.roll.characterId!==member.id||check.roll.sceneId!==current.id||check.roll.checkId!==`forced-march:${current.id}:${step.quarterKey}:${member.id}`||check.roll.skillName!==expectedSkill||check.roll.attributeName!=='strength'||check.roll.isPushed||check.roll.prideUsed||check.roll.assistedBy?.length||check.roll.difficultyModifier||!validForcedMarchPool(check,member,mount,step.penalty)||(step.penalty<0&&!check.roll.expression.includes(`mod ${step.penalty}`))||check.roll.successes!==ForbiddenLandsDiceEngine.countSuccesses(check.roll.results))throw new Error('Rolagem de marcha forçada inválida; confira a perícia e a montaria antes de caminhar.');
          }
        }
        if(marchPlan.length){
          const failedStep=marchPlan.find(step=>marchChecks.some(check=>check.quarterIndex===step.quarterIndex&&check.roll.successes===0));
          const consumedChecks=failedStep?marchChecks.filter(check=>check.quarterIndex<=failedStep.quarterIndex):marchChecks;
          for(const check of consumedChecks)await tx.objectStore('rolls').put(check.roll);
          if(failedStep){
            const failures=consumedChecks.filter(check=>check.quarterIndex===failedStep.quarterIndex&&check.roll.successes===0);
            const failedIds=new Set(failures.filter(check=>!check.mountId).map(check=>check.characterId));
            const failedMountIds=new Set(failures.map(check=>check.mountId).filter((id):id is string=>Boolean(id)));
            for(const member of party){
              let advanced=CharacterTimeRulesEngine.advanceQuarters(member,failedStep.quarterIndex);
              if(failedIds.has(member.id)){
                const agility=Math.max(0,advanced.attributes.agility.current-1);
                advanced={...advanced,attributes:{...advanced.attributes,agility:{...advanced.attributes.agility,current:agility}},conditions:agility===0?Array.from(new Set([...advanced.conditions,'Broken' as const])):advanced.conditions,...(agility===0?{brokenAttribute:'agility' as const}:{})};
              }
              advanced=CharacterTimeRulesEngine.advanceQuarters(advanced,1);
              if(failedIds.has(member.id))advanced=SurvivalRulesEngine.recoverAfterQuarter(advanced);
              await memberStore.put(advanced);
            }
            const consumedPeriods=failedStep.quarterIndex+1;
            const walkedBeforeFailure=Math.min(4,walkingToday+failedStep.quarterIndex);
            const nextCampaign={...CampaignService.advanceState(current,consumedPeriods),updatedAt:now};
            const nextMounts=(world.journey.mounts||[]).map(mount=>failedMountIds.has(mount.id)?{...mount,status:'lame' as const,lameDay:current.currentDay||1}:mount);
            const footFailures=failures.filter(check=>!check.mountId).map(check=>marchParty.find(member=>member.id===check.characterId)?.name||'Aventureira');
            const mountFailures=failures.filter(check=>check.mountId).map(check=>`${(world.journey.mounts||[]).find(mount=>mount.id===check.mountId)?.name||'Montaria'} (de ${marchParty.find(member=>member.id===check.characterId)?.name||'Aventureira'})`);
            const event={id:crypto.randomUUID(),type:'travel' as const,title:'Marcha forçada interrompida',detail:`O grupo esperou junto. ${footFailures.length?`${footFailures.join(', ')} falhou em Resiliência e sofreu 1 dano de Agilidade. `:''}${mountFailures.length?`${mountFailures.join(', ')} ficou manca e não pode continuar montada. `:''}O grupo descansou neste Quarto de Dia.`,result:{mechanic:'forced_march',walkingQuartersToday:walkedBeforeFailure,penalty:failedStep.penalty,rolls:consumedChecks.map(check=>({characterId:check.characterId,mountId:check.mountId,rollId:check.roll.id,successes:check.roll.successes,quarterKey:check.quarterKey,penalty:check.penalty}))},day:current.currentDay||1,period:current.currentPeriod||'Morning',hexId:current.currentHexId||undefined,status:'resolved' as const,createdAt:now};
            const nextWorld=WorldStateService.advanceTime({...world,journey:{...world.journey,mounts:nextMounts,quarterTravelProgress:0,walkingDay:current.currentDay||1,walkingQuartersToday:walkedBeforeFailure,forcedMarchFailuresToday:failedMarches+1,forcedMarchCheckQuarterKey:failedStep.quarterKey,events:retainJourneyEvents([...world.journey.events,event]),updatedAt:now},updatedAt:now},current,nextCampaign,consumedPeriods,now);
            await campaignStore.put(nextCampaign);
            await tx.objectStore('app_meta').put({id:'activeCampaignId',value:nextCampaign.id,updatedAt:now});
            await worldStore.put(nextWorld);
            return {campaign:nextCampaign,previousCampaign:current,hex:previousHex||updatedHex,sourceHex:previousHex,worldState:nextWorld,travelAdvanced:false,travelId:null};
          }
        }
        const currentQuarterKey = quarterKey(current);
        const checkedThisQuarter = world.journey.overloadCheckedQuarterKey === currentQuarterKey
          ? new Set(world.journey.overloadCheckedCharacterIds || [])
          : new Set<string>();
        const requiresOverloadCheck = changesHex && periodCost > 0;
        const overloaded = requiresOverloadCheck
          ? party.filter(member => !checkedThisQuarter.has(member.id) && EncumbranceRulesEngine.calculate(member).overloaded)
          : [];
        const checks = requiresOverloadCheck ? (input.overloadChecks || []) : [];
        if (overloaded.length !== checks.length || overloaded.some(member => !checks.some(check => check.characterId === member.id))) {
          throw new Error('A sobrecarga mudou desde a rolagem. Refaça a verificação de Resiliência antes de caminhar.');
        }
        const sourceHex = previousHex ? await tx.objectStore('individual_hexes').get(previousHex.id) : null;
        let droppedEquipment = [...(sourceHex?.droppedEquipment || [])];
        let persistedSourceHex = sourceHex;
        for (const check of checks) {
          const member = party.find(candidate => candidate.id === check.characterId)!;
          if (check.roll.characterId !== member.id || check.roll.sceneId !== current.id || check.roll.skillName !== 'endurance' || check.roll.attributeName !== 'strength' || check.roll.successes !== ForbiddenLandsDiceEngine.countSuccesses(check.roll.results)) {
            throw new Error('Rolagem de Resiliência inválida; refaça a verificação de carga.');
          }
          await tx.objectStore('rolls').put(check.roll);
          if (check.roll.successes > 0) continue;
          if (!check.failureChoice) throw new Error(`${member.name} falhou em Resiliência. Escolha largar um item ou sofrer 1 dano de Agilidade para continuar.`);
          const consequence = EncumbranceRulesEngine.applyFailure(member, check.failureChoice);
          await memberStore.put(consequence.character);
          party[party.findIndex(candidate => candidate.id === member.id)] = consequence.character;
          if (consequence.droppedItem) droppedEquipment.push({
            id: `dropped:${check.roll.id}`,
            campaignId: current.id,
            characterId: member.id,
            characterName: member.name,
            collection: (check.failureChoice as Extract<OverloadFailureChoice, { kind: 'drop' }>).collection,
            item: consequence.droppedItem,
            droppedAt: now,
          });
        }
        if (sourceHex && droppedEquipment.length !== (sourceHex.droppedEquipment || []).length) {
          persistedSourceHex = { ...sourceHex, droppedEquipment, updatedAt: now };
          await tx.objectStore('individual_hexes').put(persistedSourceHex);
        }
        const priorTravelProgress = Math.max(0, Math.min(0.999999, Number(world.journey.quarterTravelProgress || 0)));
        const travelProgress = changesHex ? priorTravelProgress + periodCost : priorTravelProgress;
        const elapsedPeriods = changesHex ? Math.floor(travelProgress + 1e-9) : 0;
        const remainingTravelProgress = changesHex ? Math.max(0, travelProgress - elapsedPeriods) : priorTravelProgress;
        const confirmedTravel = Boolean(previousHex && changesHex && (periodCost > 0 || distanceKm > 0));
        const nextTravelSequence = Math.max(0, Number(world.environment?.travelSequence || 0)) + 1;
        const travelId = confirmedTravel
          ? `travel:${current.id}:${previousCanonical}>${targetCanonical}:seq-${nextTravelSequence}`
          : null;

        if (input.actorId && !partyInTx.some(member => member.id === input.actorId)) throw new Error('Ator da viagem não pertence a esta campanha.');
        let nextCampaign: CampaignState = {
          ...current,
          profile: input.profile || current.profile,
          activeCharacterId: input.actorId || current.activeCharacterId || null,
          currentHexId: updatedHex.id,
          discoveredHexIds: Array.from(new Set([...(current.discoveredHexIds || []), updatedHex.id])),
          updatedAt: now,
        };
        if (elapsedPeriods > 0) nextCampaign = CampaignService.advanceState(nextCampaign, elapsedPeriods);
        if (elapsedPeriods > 0) {
          for (const member of CampaignPartyService.members(current, await memberStore.getAll())) {
            let advanced=CharacterTimeRulesEngine.advanceQuarters(member, elapsedPeriods);
            if(isWaterTravel&&member.id===maritimeCaptainId&&maritimeSailorRank>=2)advanced=SurvivalRulesEngine.recoverAfterQuarter(advanced);
            await memberStore.put(advanced);
          }
        }
        const sleepyCharacterIds=new Set(marchPlan.some(step=>step.walkingCount===3)?marchParty.filter(member=>!readyMountFor(world.journey.mounts,member.id)).map(member=>member.id):[]);
        if(sleepyCharacterIds.size){
          for(const member of await memberStore.getAll()){
            if(!sleepyCharacterIds.has(member.id))continue;
            const id=`forced-march:sleepy:${current.currentDay||1}:${member.id}`;
            if((member.statusEffects||[]).some(effect=>effect.id===id))continue;
            await memberStore.put({...member,statusEffects:[...(member.statusEffects||[]),{id,label:'Sonolento',detail:'Quarto deslocamento do dia; penalidade de −2 em Resiliência já aplicada. Descanse ou durma para remover.',remainingQuarters:4}]});
          }
        }

        const discoveryId = `${nextCampaign.id}:${updatedHex.mapId}`;
        const discovery: MapDiscovery = await tx.objectStore('map_discovery').get(discoveryId) || {
          id: discoveryId,
          campaignId: nextCampaign.id,
          mapId: updatedHex.mapId,
          discoveredHexIds: [],
          visitedHexIds: [],
          currentHexId: null,
          updatedAt: 0,
        };
        const nextDiscovery: MapDiscovery = {
          ...discovery,
          currentHexId: updatedHex.id,
          discoveredHexIds: Array.from(new Set([...(discovery.discoveredHexIds || []), updatedHex.id])),
          visitedHexIds: Array.from(new Set([...(discovery.visitedHexIds || []), updatedHex.id])),
          updatedAt: now,
        };

        let nextWorld = periodCost > 0 || distanceKm > 0
          ? JourneyService.applyTravel(world, current, nextCampaign, updatedHex, distanceKm, periodCost, now, remainingTravelProgress)
          : WorldStateService.triggerDueConsequences(world, nextCampaign.currentDay || 1, nextCampaign.currentPeriod || 'Morning', now);
        if (fogEvent) nextWorld = {
          ...nextWorld,
          journey: {
            ...nextWorld.journey,
            events: nextWorld.journey.events.map(event => event.id === fogEvent.id
              ? { ...event, status: 'resolved' as const, result: { ...(event.result || {}), mapPenaltyApplied: true }, detail: `${fogEvent.detail} Um hexágono de deslocamento foi perdido na neblina.` }
              : event),
          },
        };
        const nextQuarterKey = quarterKey(nextCampaign);
        nextWorld = {
          ...nextWorld,
          journey: {
            ...nextWorld.journey,
            walkingDay:isWaterTravel?world.journey.walkingDay:(current.currentDay||1),
            walkingQuartersToday:isWaterTravel?world.journey.walkingQuartersToday:Math.min(4,walkingToday+elapsedPeriods),
            forcedMarchFailuresToday:isWaterTravel?world.journey.forcedMarchFailuresToday:failedMarches,
            ...(marchPlan.length?{forcedMarchCheckQuarterKey:marchPlan.at(-1)!.quarterKey}:{}),
            overloadCheckedQuarterKey: elapsedPeriods > 0 ? nextQuarterKey : currentQuarterKey,
            overloadCheckedCharacterIds: elapsedPeriods > 0
              ? []
              : Array.from(new Set([...checkedThisQuarter, ...checks.map(check => check.characterId)])),
          },
        };

        // Remove da rota o trecho já percorrido, preservando no máximo cinco
        // próximos passos. Isto também evita que a chegada seja reprocessada.
        const reached = canonicalHex(updatedHex);
        const planned = nextWorld.journey.plannedRouteHexIds || [];
        const reachedIndex = planned.findIndex(id => id === updatedHex.id || id === reached);
        const remainingRoute = reachedIndex >= 0 ? planned.slice(reachedIndex + 1) : planned;
        nextWorld = {
          ...nextWorld,
          journey: { ...nextWorld.journey, plannedRouteHexIds: remainingRoute.slice(0, 5), updatedAt: now },
          updatedAt: now,
        };
        if(isWaterTravel&&maritimeAfter){
          nextWorld={...nextWorld,journey:{...nextWorld.journey,maritime:maritimeAfter,events:retainJourneyEvents([...nextWorld.journey.events,...maritimeEvents]),updatedAt:now},updatedAt:now};
        }

        // Regra oficial: uma rolagem de encontro por Quarto de Dia efetivamente
        // completado enquanto o grupo CAMINHA. Deslocamentos fracionários só
        // rolam quando a soma alcança um QD inteiro.
        if (confirmedTravel && elapsedPeriods > 0) {
          const encounterEvents = [] as import('../types').JourneyEvent[];
          for (let offset = 0; offset < elapsedPeriods; offset += 1) {
            const periodState = offset > 0 ? CampaignService.advanceState(current, offset) : current;
            encounterEvents.push(JourneyService.buildOfficialRandomEncounterEvent(
              periodState,
              updatedHex,
              'walking',
              input.rng || Math.random,
              { day: periodState.currentDay || 1, period: periodState.currentPeriod || 'Morning' },
            ));
          }
          nextWorld = {
            ...nextWorld,
            journey: { ...nextWorld.journey, events: retainJourneyEvents([...nextWorld.journey.events, ...encounterEvents]), updatedAt: now },
            updatedAt: now,
          };
        }

        if (confirmedTravel && travelId) {
          const environmental = EnvironmentalDirectorService.advanceForConfirmedTravel({
            campaignBefore: current,
            campaignAfter: nextCampaign,
            previousHex,
            currentHex: updatedHex,
            worldState: nextWorld,
            allHexes: [...allHexes.filter(hex => hex.id !== updatedHex.id), updatedHex],
            travelId,
            now,
          });
          nextWorld = { ...nextWorld, environment: environmental.environment, updatedAt: now };
        } else {
          const environmental = EnvironmentalDirectorService.evaluate({
            campaign: nextCampaign,
            currentHex: updatedHex,
            previousHex,
            worldState: nextWorld,
            allHexes: [...allHexes.filter(hex => hex.id !== updatedHex.id), updatedHex],
            now,
          });
          nextWorld = { ...nextWorld, environment: environmental.environment, updatedAt: now };
        }

        await campaignStore.put(nextCampaign);
        await tx.objectStore('app_meta').put({ id: 'activeCampaignId', value: nextCampaign.id, updatedAt: now });
        await tx.objectStore('map_discovery').put(nextDiscovery);
        await tx.objectStore('individual_hexes').put(updatedHex);
        if(confirmedTravel && changesHex){
          const npcStore=tx.objectStore('npcs');
          const movingNpcs=OfficialEncounterFollowerEngine.advanceForTravel(
            await npcStore.getAll(),current.id,current.currentHexId,nextCampaign.currentHexId,now,
            MapTravelService.movementClassForHex(updatedHex)==='water');
          for(const follower of movingNpcs)
            await npcStore.put(follower);
        }
        await worldStore.put(nextWorld);
        return {
          campaign: nextCampaign,
          previousCampaign: current,
          hex: updatedHex,
          sourceHex: persistedSourceHex,
          worldState: nextWorld,
          travelAdvanced: confirmedTravel,
          travelId,
        };
      },
    );

    const beforeIndex=CampaignService.periodIndex(result.previousCampaign);
    const afterIndex=CampaignService.periodIndex(result.campaign);
    if(afterIndex>beforeIndex){
      result.worldState = await CampaignService.reconcileElapsedTime(result.previousCampaign,result.campaign,`${afterIndex-beforeIndex} período(s) de viagem transcorrido(s)`,result.worldState);
    }
    await CampaignModuleStateService.reconcilePosition(result.campaign.id, result.campaign.currentHexId);
    CampaignService.notify(result.campaign);
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('world-state-updated', { detail: result.worldState }));
      window.dispatchEvent(new CustomEvent('environment-updated', { detail: result.worldState.environment }));
      window.dispatchEvent(new CustomEvent('center-map', { detail: { hexId: result.hex.id } }));
    }
    return result;
  }
}
