import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Backpack, CloudRain, Compass, Droplets, Flame, Footprints, Gauge, Hourglass, UtensilsCrossed } from 'lucide-react';
import { CampaignState, Character, IndividualHex, JourneyPace, JourneyRole, WorldState } from '../types';
import { UiActionCommitGate } from '../services/UiActionCommitGate';
import { InventorySelectionSnapshot } from '../services/InventorySelectionSnapshot';
import { JOURNEY_PACE_LABELS, JOURNEY_ROLE_LABELS, JourneyService, JourneyActivityInput } from '../services/JourneyService';
import { withDbRetry } from '../db/db';
import { MapTravelService } from '../services/MapTravelService';
import { JourneyRulesEngine, Season } from '../services/JourneyRulesEngine';
import { OfficialEncounterRulesEngine } from '../services/OfficialEncounterRulesEngine';
import { OfficialEncounterReceiptService } from '../services/OfficialEncounterReceiptService';
import { PoisonRulesEngine } from '../services/PoisonRulesEngine';
import { CharacterActionRulesEngine } from '../services/CharacterActionRulesEngine';
import { CampaignPartyService } from '../services/CampaignPartyService';
import { BitterReachModule } from '../services/BitterReachModule';
import { BitterReachRuntimeService } from '../services/BitterReachRuntimeService';

const roles = Object.keys(JOURNEY_ROLE_LABELS) as JourneyRole[];
const paces = Object.keys(JOURNEY_PACE_LABELS) as JourneyPace[];

export function JourneyPanel(props: {
  campaign: CampaignState;
  characters: Character[];
  worldState: WorldState;
  onError: (message: string) => void;
}) {
  const { campaign, characters, worldState, onError } = props;
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState('');
  const actionGate = useRef(new UiActionCommitGate());
  const [lastRoll, setLastRoll] = useState('');
  const [adoptedSocialDrafts,setAdoptedSocialDrafts]=useState<Record<string,{orcs:string;humans:string}>>({});
  const [balloonAssistanceDrafts,setBalloonAssistanceDrafts]=useState<Record<string,{repair:string;care:string}>>({});
  const [hexes, setHexes] = useState<IndividualHex[]>([]);
  const [routeDraft, setRouteDraft] = useState<string[]>(worldState.journey.plannedRouteHexIds || []);
  const [activityCharacterId, setActivityCharacterId] = useState(campaign.activeCharacterId || characters[0]?.id || '');
  const [terrainKey, setTerrainKey] = useState('plains');
  const [season, setSeason] = useState<Season>('summer');
  const [equipmentBonus, setEquipmentBonus] = useState(0);
  const [activityHelperIds, setActivityHelperIds] = useState<string[]>([]);
  const [dark, setDark] = useState(false);
  const [huntMethod, setHuntMethod] = useState<'weapon'|'trap'>('weapon');
  const [fishingMethod, setFishingMethod] = useState<'rod'|'net'>('rod');
  const [fallHeight, setFallHeight] = useState(3);
  const [diseaseVirulence, setDiseaseVirulence] = useState(3);
  const [poisonKind, setPoisonKind] = useState<'lethal'|'paralyzing'|'soporific'|'hallucinogenic'>('lethal');
  const [poisonPotency, setPoisonPotency] = useState(3);
  const [poisonWeaponIndex, setPoisonWeaponIndex] = useState(0);
  const [waterMetalArmor, setWaterMetalArmor] = useState(false);
  const [dreamTravelCompanionsSleepingConfirmed,setDreamTravelCompanionsSleepingConfirmed]=useState(false);
  const [mountName, setMountName] = useState('');
  const [mountRiderId, setMountRiderId] = useState(characters.find(character=>character.status!=='dead')?.id||'');
  const [mountStrength, setMountStrength] = useState(3);
  const [vesselName,setVesselName]=useState('');
  const [maritimeCaptainId,setMaritimeCaptainId]=useState(characters.find(character=>character.status!=='dead')?.id||'');
  const [animalComplicationDrafts, setAnimalComplicationDrafts] = useState<Record<string,{animal:string;problem:string;note:string;resolution:string}>>({});
  const [encounterResolutionDrafts,setEncounterResolutionDrafts]=useState<Record<string,string>>({});
  const [doughCorruptionDrafts,setDoughCorruptionDrafts]=useState<Record<string,string>>({});
  const [ruinChoices,setRuinChoices]=useState<Record<string,string>>({});
  const [banditFoodQuantities,setBanditFoodQuantities]=useState<Record<string,number>>({});
  const [trollStrengthDrafts,setTrollStrengthDrafts]=useState<Record<string,number>>({});
  const [trollSacrificeDrafts,setTrollSacrificeDrafts]=useState<Record<string,string>>({});
  const [skeletonChainmailDrafts,setSkeletonChainmailDrafts]=useState<Record<string,number>>({});
  const [soulMerchantStockDrafts,setSoulMerchantStockDrafts]=useState<Record<string,number>>({});
  const [merchantLoadoutDrafts,setMerchantLoadoutDrafts]=useState<Record<string,{ginda:{melee:string;ranged:string;armor:string};aneta:{melee:string;ranged:string;armor:string}}>>({});
  const [saurianSpearDrafts,setSaurianSpearDrafts]=useState<Record<string,'trident'|'long_spear'|''>>({});
  const [saurianParleyEvidenceDrafts,setSaurianParleyEvidenceDrafts]=useState<Record<string,string>>({});
  const [burialRescueEvidenceDrafts,setBurialRescueEvidenceDrafts]=useState<Record<string,string>>({});
  const [officialExposureRounds,setOfficialExposureRounds]=useState<Record<string,number>>({});
  const [officialCrystalStrikeRounds,setOfficialCrystalStrikeRounds]=useState<Record<string,number>>({});
  const [octopusTentacleDrafts,setOctopusTentacleDrafts]=useState<Record<string,{cut:string;total:string;evidence:string}>>({});

  useEffect(() => {
    setRouteDraft(worldState.journey.plannedRouteHexIds || []);
  }, [worldState.journey.plannedRouteHexIds]);

  useEffect(() => {
    if (!open || hexes.length) return;
    void withDbRetry(db => db.getAll('individual_hexes')).then(setHexes).catch(error => onError(error?.message || 'Não foi possível carregar os hexes da rota.'));
  }, [open, hexes.length, onError]);


  useEffect(() => {
    if (!open || !campaign.currentHexId) return;
    void withDbRetry(db => db.get('individual_hexes', campaign.currentHexId!)).then(async hex => {
      if (!hex || !BitterReachModule.isBitterReachHex(hex)) return;
      await BitterReachRuntimeService.ensureWeather(campaign, hex);
    }).catch(error => onError(error?.message || 'Não foi possível atualizar o clima do Alcance Amargo.'));
  }, [open, campaign.id, campaign.currentDay, campaign.currentPeriod, campaign.currentHexId, onError]);

  const hexById = useMemo(() => {
    const map = new Map<string, IndividualHex>();
    for (const item of hexes) { map.set(item.id, item); map.set(item.coordinate, item); }
    return map;
  }, [hexes]);

  const routeOptions = (step: number) => {
    const previous = step === 0 ? campaign.currentHexId : routeDraft[step - 1];
    if (!previous) return [];
    return MapTravelService.getNeighborIds(previous)
      .map(id => hexById.get(id))
      .filter((item): item is IndividualHex => Boolean(item));
  };

  const updateRouteStep = (step: number, value: string) => {
    setRouteDraft(current => {
      const next = current.slice(0, step);
      if (value) next[step] = value;
      return next.slice(0, 5);
    });
  };

  const act = async (label: string, action: () => Promise<unknown>) => {
    // A state update alone does not block a second touch before React renders.
    const ticket = actionGate.current.acquire(campaign.id, label);
    if (!ticket) return;
    setBusy(label);
    try { await action(); onError(''); }
    catch (error: any) { onError(error?.message || String(error)); }
    finally { if (actionGate.current.release(ticket)) setBusy(''); }
  };

  const encounterResolution=(eventId:string)=>encounterResolutionDrafts[eventId]||'';
  const setEncounterResolution=(eventId:string,value:string)=>setEncounterResolutionDrafts(current=>({...current,[eventId]:value}));

  const advanceRoute = async () => {
    await act('route:advance', async () => {
      const outcome = await JourneyService.resumeJourneyRoute(campaign.id);
      setLastRoll(`Avanço da rota: grupo moveu para ${outcome.hex.coordinate || outcome.hex.id} (total acumulado: ${outcome.worldState.journey.periodsTravelled} QD, ${outcome.worldState.journey.distanceKm} km percorridos).`);
    });
  };

  const finishJourney = async () => {
    await act('journey:finish', async () => {
      const outcome = await JourneyService.finishJourney(campaign.id);
      setRouteDraft([]);
      setLastRoll(`Jornada finalizada em ${outcome.campaign.currentHexId || 'ponto atual'}. Total percorrido: ${outcome.world.journey.distanceKm || 0} km em ${outcome.world.journey.periodsTravelled || 0} período(s).`);
    });
  };

  const runActivity = async (activity: JourneyActivityInput['activity']) => {
    if (!activityCharacterId) { onError('Escolha quem executa a atividade.'); return; }
    await act(`activity:${activity}`, async () => {
      const outcome = await JourneyService.resolveActivity(campaign.id, {
        characterId: activityCharacterId,
        activity,
        terrainKey,
        season,
        equipmentBonus,
        helperIds: activityHelperIds,
        dark,
        huntMethod,
        fishingMethod,
        metalArmor: waterMetalArmor,
        dreamTravelCompanionsSleepingConfirmed:activity==='sleep'&&dreamTravelCompanionsSleepingConfirmed,
      });
      const suffix = outcome.rollSuccesses === undefined ? '' : ` · ${outcome.rollSuccesses} sucesso(s)${outcome.secondarySuccesses === undefined ? '' : ` / ${outcome.secondarySuccesses} no segundo teste`}`;
      setLastRoll(`${outcome.event.title}: ${outcome.event.detail}${suffix}`);
    });
  };

  const rollResource = async (character: Character, resource: keyof Character['resourceDice']) => {
    await act(`${character.id}:${resource}`, async () => {
      const result = await JourneyService.rollResource(campaign.id, character.id, resource);
      const labels: Record<string, string> = { food: 'comida', water: 'água', arrows: 'flechas', torches: 'tochas' };
      setLastRoll(result.roll === null
        ? `${character.name} está sem ${labels[resource]}.`
        : `${character.name}: ${labels[resource]} ${result.before} → rolou ${result.roll}${result.after !== result.before ? ` → ${result.after || 'esgotado'}` : ' → reserva mantida'}.${result.poisonSummary ? ` ${result.poisonSummary}.` : ''}${result.empathyRecovered?' Cozinheiro 3: recuperou 1 Empatia.':''}`);
    });
  };

  const resolveFall = async () => {
    if (!activityCharacterId) { onError('Escolha quem sofreu a queda.'); return; }
    await act('hazard:fall', async () => {
      const outcome = await JourneyService.resolveFall(campaign.id, activityCharacterId, fallHeight, true);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'}: queda de ${fallHeight} m — ${outcome.damage} dano(s) em Força${outcome.critical ? `; ferimento grave: ${outcome.critical.name}` : ''}.`);
    });
  };

  const resolveColdCheck = async () => {
    if (!activityCharacterId) { onError('Escolha quem est\u00e1 exposto ao frio.'); return; }
    await act('hazard:cold', async () => {
      const outcome = await JourneyService.resolveColdCheck(campaign.id, activityCharacterId, equipmentBonus);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'}: Resili\u00eancia ${outcome.roll.successes} sucesso(s)${outcome.damageApplied ? '; 1 dano em For\u00e7a e Ast\u00facia' : ''}${outcome.died ? '; morreu por hipotermia' : outcome.becameHypothermic ? '; hipotermia' : ''}.`);
    });
  };

  const warmFromCold = async () => {
    if (!activityCharacterId) { onError('Escolha quem foi aquecido.'); return; }
    await act('hazard:warm', async () => {
      await JourneyService.warmFromCold(campaign.id, activityCharacterId);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'} foi aquecido; For\u00e7a e Ast\u00facia podem se recuperar com descanso.`);
    });
  };

  const exposeToDisease = async () => {
    if (!activityCharacterId) { onError('Escolha quem foi exposto à infecção.'); return; }
    await act('hazard:disease', async () => {
      const outcome = await JourneyService.exposeToDisease(campaign.id, activityCharacterId, diseaseVirulence);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'}: Resiliência ${outcome.patientRoll.successes} × Virulência ${outcome.diseaseRoll.successes}${outcome.infected ? '; doença contraída, manifesta após 1 dia' : '; resistiu à infecção'}.`);
    });
  };

  const resolveDiseaseCheck = async () => {
    if (!activityCharacterId) { onError('Escolha quem fará a rolagem diária.'); return; }
    await act('hazard:disease-daily', async () => {
      const outcome = await JourneyService.resolveDiseaseCheck(campaign.id, activityCharacterId);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'}: teste diário de Resiliência ${outcome.patientRoll.successes} × Virulência ${outcome.diseaseRoll.successes}${outcome.recovered ? '; doença superada' : '; sofreu 1 dano em Força e Agilidade'}.`);
    });
  };

  const enterWater = async () => {
    if (!activityCharacterId) { onError('Escolha quem caiu na água.'); return; }
    await act('hazard:drowning-start', async () => {
      await JourneyService.enterWater(campaign.id, activityCharacterId, waterMetalArmor);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'} caiu na água; ${waterMetalArmor ? 'armadura de metal exige teste a cada rodada' : 'Resiliência será testada por turno de 15 minutos'}.`);
    });
  };

  const resolveWaterTurn = async () => {
    if (!activityCharacterId) { onError('Escolha quem está na água.'); return; }
    await act('hazard:drowning-turn', async () => {
      const outcome = await JourneyService.resolveDrowningTurn(campaign.id, activityCharacterId);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'}: ${outcome.roll ? `Resiliência ${outcome.roll.successes} sucesso(s)` : 'rodada de afogamento'}${outcome.damageApplied ? '; 1 dano em Força' : outcome.phaseAfter === 'underwater' ? '; afundou, teste para prender a respiração no próximo turno' : '; permanece boiando / segurando a respiração'}${outcome.character.drowningState?.minutesUntilDeath ? `; morrerá em ${outcome.character.drowningState.minutesUntilDeath} minuto(s) sem resgate` : ''}.`);
    });
  };

  const rescueFromWater = async () => {
    if (!activityCharacterId) { onError('Escolha quem será resgatado.'); return; }
    await act('hazard:drowning-rescue', async () => {
      await JourneyService.rescueFromWater(campaign.id, activityCharacterId);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'} foi resgatado; o relógio de afogamento foi encerrado.`);
    });
  };

  const exposeToPoison = async () => {
    if (!activityCharacterId) { onError('Escolha quem foi exposto ao veneno.'); return; }
    await act('hazard:poison', async () => {
      const outcome = await JourneyService.exposeToPoison(campaign.id, activityCharacterId, poisonKind, poisonPotency);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'}: resistência ${outcome.victimRoll.successes} × veneno ${outcome.poisonRoll.successes}${outcome.totalEffect ? `; efeito total, 1 dano em ${outcome.attribute} por rodada` : `; efeito limitado, 1 dano em ${outcome.attribute}`}.`);
    });
  };

  const resolvePoisonRound = async () => {
    if (!activityCharacterId) { onError('Escolha quem sofre o efeito do veneno.'); return; }
    await act('hazard:poison-round', async () => {
      const outcome = await JourneyService.resolvePoisonRound(campaign.id, activityCharacterId);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`${target?.name || 'Personagem'}: ${outcome.damageApplied ? `1 dano em ${outcome.attribute}` : `sem dano adicional em ${outcome.attribute}`}${outcome.broke ? '; atributo Quebrado, efeito encerrado' : ''}${outcome.character.poisonState?.criticalPending ? '; dano atípico grave pendente' : ''}${outcome.character.poisonState?.unconsciousMinutesRemaining ? `; inconsciente por ${outcome.character.poisonState.unconsciousMinutesRemaining} min` : ''}.`);
    });
  };

  const applyAntidote = async () => {
    if (!activityCharacterId) { onError('Escolha quem receberá o antídoto.'); return; }
    await act('hazard:antidote', async () => {
      await JourneyService.applyAntidote(campaign.id, activityCharacterId);
      const target = characters.find(character => character.id === activityCharacterId);
      setLastRoll(`Antídoto interrompeu o efeito de veneno de ${target?.name || 'personagem'}.`);
    });
  };

  const coatWeaponWithPoison = async () => {
    if (!activityCharacterId) { onError('Escolha quem vai aplicar o veneno.'); return; }
    await act('hazard:poison-coat', async () => {
      const coated = await JourneyService.coatWeaponWithPoison(campaign.id, activityCharacterId, poisonWeaponIndex, poisonKind, poisonPotency);
      setLastRoll(`${coated.name} aplicou veneno ${poisonKind} à arma ${coated.weapons[poisonWeaponIndex]?.name || `#${poisonWeaponIndex + 1}`}; expira ao final do próximo Quarto de Dia ou no primeiro ataque que atingir.`);
    });
  };

  const resolveWeatherChecks = async () => {
    await act('weather:checks',async()=>{
      const outcome=await JourneyService.resolveWeatherTravelChecks(campaign.id);
      setLastRoll(outcome.alreadyResolved
        ? outcome.stopped?'O grupo já falhou no teste do clima deste Quarto de Dia; a marcha fica interrompida.':'Os testes do clima deste Quarto de Dia já foram resolvidos.'
        : outcome.checks.length?outcome.checks.map(check=>`${check.characterName}: ${check.kind} ${check.successes} sucesso(s)`).join(' · '):'O clima atual não exige testes de viagem.');
    });
  };


  const resolveBitterReachChecks = async () => {
    await act('weather:bitter-reach', async () => {
      const outcome = await JourneyService.resolveBitterReachTravelChecks(campaign.id);
      setLastRoll(outcome.checks ? `Alcance Amargo: ${outcome.checks} teste(s) de marcha resolvido(s). ${outcome.stopped ? 'A marcha foi interrompida.' : 'O grupo pode prosseguir.'}` : 'O clima do Alcance Amargo não exige teste de marcha neste QD.');
    });
  };

  const pendingColdCheckIds=Array.from(new Set(worldState.journey.events.flatMap(event=>event.status==='open'&&Array.isArray(event.result?.pendingColdCheckCharacterIds)?event.result.pendingColdCheckCharacterIds.filter((id):id is string=>typeof id==='string'):[])));
  const visibleJourneyEvents=[...new Map([...worldState.journey.events.filter(event=>event.status==='open'),
    ...worldState.journey.events.filter(event=>event.status==='resolved'&&
      Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===6&&
      (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseDisposition==='claim'&&
      !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseFamilyRewardAt),
    ...worldState.journey.events.filter(event=>event.status==='resolved'&&(
      (Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===2&&
        (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='watch'&&
        !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.duelistsIntroducedAt)||
      (Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)===10&&
        (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.foxFollowerActive===true))),
    ...worldState.journey.events.filter(event=>event.status==='resolved'&&OfficialEncounterReceiptService.fromEvent(event)?.followUpPending),
    ...worldState.journey.events.filter(event=>event.status==='resolved').slice(-10)].map(event=>[event.id,event])).values()]
    .sort((a,b)=>Number(Boolean(OfficialEncounterReceiptService.fromEvent(b)?.followUpPending))-Number(Boolean(OfficialEncounterReceiptService.fromEvent(a)?.followUpPending))||b.createdAt-a.createdAt)
    .slice(0,Math.max(50,worldState.journey.events.filter(event=>OfficialEncounterReceiptService.fromEvent(event)?.followUpPending).length));
  const animalDraft=(eventId:string)=>animalComplicationDrafts[eventId]||{animal:'',problem:'',note:'',resolution:''};
  const setAnimalDraft=(eventId:string,key:keyof ReturnType<typeof animalDraft>,value:string)=>setAnimalComplicationDrafts(current=>({...current,[eventId]:{...animalDraft(eventId),...current[eventId],[key]:value}}));

  return <section className="bg-bg-surface border border-border rounded">
    <button onClick={() => setOpen(value => !value)} className="w-full p-3 flex items-center gap-3 text-left">
      <Compass className="text-accent" />
      <div className="flex-1"><b>Jornada e sobrevivência</b><p className="text-xs text-text-secondary">{worldState.journey.distanceKm} km · {worldState.journey.periodsTravelled} período(s) · ritmo {JOURNEY_PACE_LABELS[worldState.journey.pace].toLowerCase()}</p></div>
      <span className="text-xs text-accent">{open ? 'Fechar' : 'Gerir'}</span>
    </button>
    {open && <div className="border-t border-border p-4 space-y-5">
      <div>
        <div className="flex items-center gap-2 mb-2"><Gauge className="w-4 h-4 text-accent" /><b className="text-sm">Ritmo da jornada</b></div>
      <div className="flex flex-wrap gap-2">{paces.map(pace => <button key={pace} disabled={!!busy} onClick={() => void act(`pace:${pace}`, () => JourneyService.setPace(campaign.id, pace))} className={`px-3 py-2 text-xs rounded border ${worldState.journey.pace === pace ? 'border-accent bg-accent/10' : 'border-border'}`}>{JOURNEY_PACE_LABELS[pace]}</button>)}</div>
      </div>

      <div className="border border-border rounded p-3 space-y-2">
        <div className="flex items-center gap-2"><CloudRain className="w-4 h-4 text-sky-300"/><b className="text-sm">Clima diário (regra opcional)</b></div>
        <p className="text-xs text-text-secondary">Role vento, precipitação e temperatura uma vez pela manhã. O resultado fica salvo na jornada.</p>
        {worldState.journey.dailyWeather&&<p className="text-xs">Dia {worldState.journey.dailyWeather.day}: vento {worldState.journey.dailyWeather.wind} ({worldState.journey.dailyWeather.windRoll}), precipitação {worldState.journey.dailyWeather.precipitation} ({worldState.journey.dailyWeather.rainfallRoll}), temperatura {worldState.journey.dailyWeather.temperature} ({worldState.journey.dailyWeather.temperatureRoll}).</p>}
        {worldState.journey.dailyWeather?.day!==(campaign.currentDay||1)&&<button disabled={!!busy} onClick={()=>void act('weather:daily',async()=>{const next=await JourneyService.rollDailyWeather(campaign.id);const weather=next.journey.dailyWeather;setLastRoll(weather?`Clima do Dia ${weather.day}: vento ${weather.windRoll}, precipitação ${weather.rainfallRoll}, temperatura ${weather.temperatureRoll}.`:'Clima não rolado.');})} className="rounded border border-sky-800 px-3 py-2 text-xs disabled:opacity-50">Sortear clima de hoje</button>}
        {worldState.journey.dailyWeather?.day===(campaign.currentDay||1)&&<p className="text-xs text-emerald-300">Clima do dia definido; os dados das três rolagens estão no histórico.</p>}
        {worldState.journey.dailyWeather?.day===(campaign.currentDay||1)&&(()=>{const weather=JourneyRulesEngine.dailyWeather(worldState.journey.dailyWeather!.windRoll,worldState.journey.dailyWeather!.rainfallRoll,worldState.journey.dailyWeather!.temperatureRoll);const required=weather.marchCheckRequired||weather.coldCheckRequired||weather.thirstCheckRequired;const key=`${campaign.currentDay||1}:${campaign.currentPeriod||'Morning'}`;if(!required)return null;const resolved=worldState.journey.weatherCheckQuarterKey===key;return <div className="space-y-2"><p className="text-xs text-text-secondary">{weather.marchCheckRequired?`Todos testam Endurance para manter a marcha${weather.marchModifier?' com −2':''}. `:''}{weather.coldCheckRequired?'Todos testam Endurance contra o frio. ':''}{weather.thirstCheckRequired?'Quem está caminhando testa Endurance contra o calor.':''}</p>{!resolved?<button disabled={!!busy} onClick={()=>void resolveWeatherChecks()} className="rounded border border-sky-800 px-3 py-2 text-xs disabled:opacity-50">Resolver testes de clima deste QD</button>:<p className={`text-xs ${worldState.journey.weatherStoppedQuarterKey===key?'text-amber-300':'text-emerald-300'}`}>{worldState.journey.weatherStoppedQuarterKey===key?'A falha no clima impede mover o grupo neste QD.':'Testes do clima concluídos para este QD.'}</p>}</div>;})()}
        {worldState.journey.bitterReachWeather?.day===(campaign.currentDay||1)&&(()=>{const weather=worldState.journey.bitterReachWeather!;const key=`${campaign.currentDay||1}:${campaign.currentPeriod||'Morning'}`;const required=weather.hikeEnduranceRequired||weather.hikeEnduranceRequiredSnow;const resolved=worldState.journey.bitterReachTravelCheckQuarterKey===key;return <div className="mt-3 border-t border-sky-900/60 pt-3 space-y-2"><p className="text-xs font-bold text-sky-200">Alcance Amargo: {weather.coldLevel} · {weather.windType} · {weather.snowType}</p><p className="text-[11px] text-text-secondary">Desbravar {weather.leadTheWayModifier||0}; acampar {weather.makeCampModifier||0}. As checagens de frio são aplicadas pelo relógio do mundo na cadência oficial.</p>{required&&!resolved?<button disabled={!!busy} onClick={()=>void resolveBitterReachChecks()} className="rounded border border-sky-700 px-3 py-2 text-xs disabled:opacity-50">Resolver marcha do Alcance Amargo</button>:required?<p className={`text-xs ${worldState.journey.bitterReachTravelStoppedQuarterKey===key?'text-amber-300':'text-emerald-300'}`}>{worldState.journey.bitterReachTravelStoppedQuarterKey===key?'Tempestade/nevasca interrompeu a marcha.':'Marcha liberada pelo clima boreal.'}</p>:<p className="text-xs text-emerald-300">O clima boreal não exige teste extra de marcha neste QD.</p>}</div>;})()}
      </div>

      <div className="border border-border rounded p-3 space-y-3">
        <div className="flex items-center gap-2"><Footprints className="w-4 h-4 text-amber-300"/><b className="text-sm">Montarias da jornada</b></div>
        <p className="text-xs text-text-secondary">O mapa usa deslocamento montado quando todo o grupo vivo tem uma montaria apta. Na marcha forçada, cada montaria usa sua Força e o cavaleiro usa Adestramento; uma falha deixa o animal manco.</p>
        <div className="grid sm:grid-cols-4 gap-2">
          <label className="text-xs">Nome<input value={mountName} onChange={event=>setMountName(event.target.value)} maxLength={48} className="field mt-1" placeholder="Cavalo de Iara"/></label>
          <label className="text-xs">Cavaleiro<select value={mountRiderId} onChange={event=>setMountRiderId(event.target.value)} className="field mt-1">{characters.filter(character=>character.status!=='dead'&&!(worldState.journey.mounts||[]).some(mount=>mount.riderCharacterId===character.id)).map(character=><option key={character.id} value={character.id}>{character.name}</option>)}</select></label>
          <label className="text-xs">Força do animal<input type="number" min={1} max={30} value={mountStrength} onChange={event=>setMountStrength(Math.max(1,Math.min(30,Number(event.target.value)||1)))} className="field mt-1"/></label>
          <button disabled={!!busy||!mountName.trim()||!mountRiderId} onClick={()=>void act('mount:add',async()=>{await JourneyService.registerMount(campaign.id,{name:mountName,riderCharacterId:mountRiderId,strength:mountStrength});setMountName('');setMountRiderId(characters.find(character=>character.status!=='dead'&&character.id!==mountRiderId&&!(worldState.journey.mounts||[]).some(mount=>mount.riderCharacterId===character.id))?.id||'');setLastRoll('Montaria registrada na jornada.');})} className="self-end rounded border border-amber-700 px-3 py-2 text-xs disabled:opacity-50">Registrar montaria</button>
        </div>
        {(worldState.journey.mounts||[]).length>0?<div className="grid sm:grid-cols-2 gap-2">{worldState.journey.mounts.map(mount=>{const rider=characters.find(character=>character.id===mount.riderCharacterId);return <article key={mount.id} className="border border-border rounded p-2 flex items-center gap-2 text-xs"><div className="flex-1"><b>{mount.name}</b><span className="text-text-secondary"> · Força {mount.strength} · {rider?.name||'Cavaleiro ausente'} · {mount.status==='lame'?'manca':'apta'}</span></div>{mount.status==='lame'&&<button disabled={!!busy||(campaign.currentDay||1)<=(mount.lameDay||campaign.currentDay||1)} title={(campaign.currentDay||1)<=(mount.lameDay||campaign.currentDay||1)?'A montaria precisa descansar até amanhã.':''} onClick={()=>void act(`mount:recover:${mount.id}`,async()=>{const outcome=await JourneyService.recoverMount(campaign.id,mount.id);setLastRoll(`${rider?.name||'Cavaleiro'}: Adestramento ${outcome.roll.successes} sucesso(s); ${outcome.recovered?'montaria recuperada':'montaria continua manca'}.`);})} className="rounded border border-emerald-700 px-2 py-1 disabled:opacity-40">Tentar recuperar</button>}<button disabled={!!busy} title="Deixa de acompanhar o grupo e remove o vínculo com o cavaleiro." onClick={()=>void act(`mount:remove:${mount.id}`,()=>JourneyService.removeMount(campaign.id,mount.id))} className="rounded border border-border px-2 py-1">Deixar</button></article>;})}</div>:<p className="text-xs text-text-secondary">Nenhuma montaria registrada.</p>}
      </div>

      <div className="border border-cyan-900/70 rounded p-3 space-y-3">
        <div className="flex items-center gap-2"><Compass className="w-4 h-4 text-cyan-300"/><b className="text-sm">Viagem Marítima</b></div>
        {!worldState.journey.maritime?.active?<>
          <p className="text-xs text-text-secondary">Para entrar em hexágonos de água, registre a embarcação e escolha manualmente a capitã. Cada novo hexágono faz DESBRAVAR com Sobrevivência; Marinheiro é aplicado automaticamente.</p>
          <div className="grid sm:grid-cols-3 gap-2"><label className="text-xs">Embarcação<input value={vesselName} onChange={e=>setVesselName(e.target.value)} className="field mt-1" placeholder="Canoa, barco a remo, veleiro..."/></label><label className="text-xs">Capitã<select value={maritimeCaptainId} onChange={e=>setMaritimeCaptainId(e.target.value)} className="field mt-1"><option value="">Escolha</option>{characters.filter(character=>character.status!=='dead').map(character=><option key={character.id} value={character.id}>{character.name}</option>)}</select></label><button disabled={!!busy||!vesselName.trim()||!maritimeCaptainId} onClick={()=>void act('maritime:start',async()=>{await JourneyService.startMaritimeTravel(campaign.id,maritimeCaptainId,vesselName);setLastRoll(`Viagem marítima iniciada em ${vesselName}.`);})} className="self-end border border-cyan-700 rounded px-3 py-2 text-xs disabled:opacity-50">Iniciar viagem marítima</button></div>
        </>:<>
          <p className="text-xs text-cyan-200"><b>{worldState.journey.maritime.vesselName}</b> · capitã {characters.find(c=>c.id===worldState.journey.maritime?.captainCharacterId)?.name||'não encontrada'} · casco {worldState.journey.maritime.hullStatus}{worldState.journey.maritime.leakHoursRemaining!==undefined?` · ${worldState.journey.maritime.leakHoursRemaining} h até afundar`:''}</p>
          <div className="flex flex-wrap gap-2">{['leaking','aground'].includes(worldState.journey.maritime.hullStatus)&&<button disabled={!!busy||!activityCharacterId} onClick={()=>void act('maritime:repair',async()=>{const outcome=await JourneyService.repairMaritimeVessel(campaign.id,activityCharacterId);setLastRoll(outcome.repaired?`${worldState.journey.maritime?.vesselName} reparada.`:'O reparo falhou; o dano permanece.');})} className="border border-amber-700 rounded px-3 py-2 text-xs disabled:opacity-50">Reparar com Artesanato ({characters.find(c=>c.id===activityCharacterId)?.name||'escolha personagem'})</button>}{worldState.journey.maritime.hullStatus==='aground'&&<button disabled={!!busy} onClick={()=>void act('maritime:abandon',()=>JourneyService.abandonMaritimeVessel(campaign.id))} className="border border-red-800 rounded px-3 py-2 text-xs disabled:opacity-50">Abandonar embarcação</button>}<button disabled={!!busy} onClick={()=>void act('maritime:end',()=>JourneyService.endMaritimeTravel(campaign.id))} className="border border-border rounded px-3 py-2 text-xs disabled:opacity-50">Encerrar trecho marítimo</button></div>
        </>}
      </div>

      <div>
        <div className="flex items-center gap-2 mb-2"><Compass className="w-4 h-4 text-sky-300" /><b className="text-sm">Rota prevista · até cinco etapas</b></div>
        <p className="text-xs text-text-secondary mb-3">O Diretor Ambiental inspeciona esta rota sem mover o grupo. O clima só avança quando um deslocamento é confirmado.</p>
        <div className="grid md:grid-cols-5 gap-2">{Array.from({ length: 5 }, (_, step) => {
          const options = routeOptions(step);
          const disabled = !!busy || (step > 0 && !routeDraft[step - 1]);
          return <label key={step} className="p-2 border border-border rounded text-xs">
            <span className="block text-text-secondary mb-1">Etapa {step + 1}</span>
            <select value={routeDraft[step] || ''} disabled={disabled} onChange={event => updateRouteStep(step, event.target.value)} className="field py-2">
              <option value="">{step === 0 ? 'Escolha um vizinho' : 'Sem próxima etapa'}</option>
              {options.map(item => <option key={item.id} value={item.coordinate || item.id}>{item.coordinate}{item.terrainResolution?.finalTerrainLabel || item.physicalTerrainLabel || item.terrain ? ` · ${item.terrainResolution?.finalTerrainLabel || item.physicalTerrainLabel || item.terrain}` : ''}</option>)}
            </select>
          </label>;
        })}</div>
        <div className="flex flex-wrap gap-2 mt-3">
          <button disabled={!!busy} onClick={() => void act('route', () => JourneyService.setPlannedRoute(campaign.id, routeDraft))} className="px-3 py-2 text-xs rounded border border-sky-600 text-sky-200 disabled:opacity-50">Salvar rota prevista</button>
          <button disabled={!!busy || !routeDraft.length} onClick={() => { setRouteDraft([]); void act('route-clear', () => JourneyService.setPlannedRoute(campaign.id, [])); }} className="px-3 py-2 text-xs rounded border border-border disabled:opacity-50">Limpar rota</button>
          {((worldState.journey.plannedRouteHexIds && worldState.journey.plannedRouteHexIds.length > 0) || routeDraft.length > 0) && (
            <button disabled={!!busy} onClick={() => void advanceRoute()} className="px-3 py-2 text-xs rounded border border-emerald-600 text-emerald-200 disabled:opacity-50">Avançar próxima etapa da rota</button>
          )}
          {(worldState.journey.active || (worldState.journey.distanceKm || 0) > 0 || (worldState.journey.plannedRouteHexIds && worldState.journey.plannedRouteHexIds.length > 0)) && (
            <button disabled={!!busy} onClick={() => void finishJourney()} className="px-3 py-2 text-xs rounded border border-amber-600 text-amber-200 disabled:opacity-50">Finalizar jornada</button>
          )}
          {!!routeDraft.length && <span className="self-center text-xs text-text-secondary">{[campaign.currentHexId, ...routeDraft].filter(Boolean).join(' → ')}</span>}
        </div>
      </div>

      <div>
        <div className="flex items-center gap-2 mb-2"><Footprints className="w-4 h-4 text-accent" /><b className="text-sm">Funções do grupo</b></div>
        <div className="grid md:grid-cols-2 gap-2">{roles.map(role => {
          const assigned = worldState.journey.assignments.find(item => item.role === role)?.characterId || '';
          return <label key={role} className="p-2 border border-border rounded text-xs"><span className="block text-text-secondary mb-1">{JOURNEY_ROLE_LABELS[role]}</span><select value={assigned} disabled={!!busy} onChange={event => void act(`role:${role}`, () => JourneyService.assignRole(campaign.id, role, event.target.value || null))} className="field py-2"><option value="">Não atribuído</option>{characters.filter(character => character.status !== 'dead').map(character => <option key={character.id} value={character.id}>{character.name}</option>)}</select></label>;
        })}</div>
      </div>

      <div className="border border-border rounded p-3 space-y-3">
        <div className="flex items-center gap-2"><Footprints className="w-4 h-4 text-emerald-300" /><b className="text-sm">Ações do Quarto de Dia</b></div>
        <p className="text-xs text-text-secondary">Coletar, Caçar, Pescar, Acampar, Descansar e Dormir avançam exatamente um Quarto de Dia. Desbravar e Vigília são checks/estados combináveis e não avançam o relógio sozinhos.</p>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
          <label className="text-xs">Personagem<select value={activityCharacterId} onChange={event=>{setActivityCharacterId(event.target.value);setActivityHelperIds(ids=>ids.filter(id=>id!==event.target.value));}} className="field mt-1"><option value="">Escolha</option>{characters.filter(character => character.status !== 'dead').map(character=><option key={character.id} value={character.id}>{character.name}</option>)}</select></label>
          <label className="text-xs">Terreno<select value={terrainKey} onChange={event=>setTerrainKey(event.target.value)} className="field mt-1"><option value="plains">Planícies</option><option value="forest">Floresta</option><option value="dark_forest">Floresta densa</option><option value="hills">Colinas</option><option value="mountains">Montanhas</option><option value="high_mountains">Altas montanhas</option><option value="lake_river">Lago/rio</option><option value="swamp">Pântano</option><option value="marsh">Charco</option><option value="ruins">Ruínas</option></select></label>
          <label className="text-xs">Estação<select value={season} onChange={event=>setSeason(event.target.value as Season)} className="field mt-1"><option value="spring">Primavera</option><option value="summer">Verão</option><option value="autumn">Outono</option><option value="winter">Inverno</option></select></label>
          <label className="text-xs">Método de caça<select value={huntMethod} onChange={event=>setHuntMethod(event.target.value as 'weapon'|'trap')} className="field mt-1"><option value="weapon">Arma</option><option value="trap">Armadilha</option></select></label>
          <label className="text-xs">Equipamento de pesca<select value={fishingMethod} onChange={event=>setFishingMethod(event.target.value as 'rod'|'net')} className="field mt-1"><option value="rod">Vara</option><option value="net">Rede</option></select></label>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
          <label className="text-xs">Bônus equipamento<input type="number" min="0" max="10" value={equipmentBonus} onChange={event=>setEquipmentBonus(Number(event.target.value))} className="field mt-1" /></label>
          <label className="text-xs flex items-center gap-2 border border-border rounded px-3 mt-5"><input type="checkbox" checked={dark} onChange={event=>setDark(event.target.checked)} />Desbravar no escuro (−2)</label>
          <div className="text-[11px] text-text-secondary flex items-center">Falhas em Coletar/Caçar/Pescar geram infortúnio persistente. Falha no Acampamento guarda o resultado oculto apenas para o Mestre.</div>
        </div>
        <fieldset className="border border-border rounded p-3">
          <legend className="px-1 text-xs text-text-secondary">Ajudantes presentes e capazes (até 3; +1 dado de Perícia cada)</legend>
          <div className="grid sm:grid-cols-2 gap-1">{characters.filter(member=>member.id!==activityCharacterId&&CharacterActionRulesEngine.canAct(member)).map(member=><label key={member.id} className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={activityHelperIds.includes(member.id)} disabled={!activityHelperIds.includes(member.id)&&activityHelperIds.length>=3} onChange={event=>setActivityHelperIds(ids=>event.target.checked?[...ids,member.id].slice(0,3):ids.filter(id=>id!==member.id))}/>{member.name}
          </label>)}</div>
        </fieldset>
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2 text-xs">
          <button disabled={!!busy} onClick={()=>void runActivity('hike')} className="border border-emerald-600/70 text-emerald-300 rounded p-2 disabled:opacity-50">Marcha (Caminhar / Cavalgar)</button>
          <button disabled={!!busy} onClick={()=>void runActivity('leadWay')} className="border border-border rounded p-2 disabled:opacity-50">Desbravar</button>
          <button disabled={!!busy} onClick={()=>void runActivity('keepWatch')} className="border border-border rounded p-2 disabled:opacity-50">Manter Vigília</button>
          <button disabled={!!busy} onClick={()=>void runActivity('forageFood')} className="border border-border rounded p-2 disabled:opacity-50">Coletar comida</button>
          <button disabled={!!busy} onClick={()=>void runActivity('forageWater')} className="border border-border rounded p-2 disabled:opacity-50">Coletar água</button>
          <button disabled={!!busy} onClick={()=>void runActivity('hunt')} className="border border-border rounded p-2 disabled:opacity-50">Caçar</button>
          <button disabled={!!busy} onClick={()=>void runActivity('fish')} className="border border-border rounded p-2 disabled:opacity-50">Pescar</button>
          <button disabled={!!busy} onClick={()=>void runActivity('makeCamp')} className="border border-accent/60 text-accent rounded p-2 disabled:opacity-50">Montar acampamento</button>
          <button disabled={!!busy} onClick={()=>void runActivity('rest')} className="border border-border rounded p-2 disabled:opacity-50">Descansar</button>
          <button disabled={!!busy} onClick={()=>void runActivity('sleep')} className="border border-border rounded p-2 disabled:opacity-50">Dormir</button>
        </div>
        {worldState.magicDreamTravels?.some(e=>e.status==='pending'&&e.casterId===activityCharacterId)&&<label className="flex gap-2 items-start text-xs border border-amber-500/40 rounded p-2 mt-2">
          <input type="checkbox" checked={dreamTravelCompanionsSleepingConfirmed} onChange={e=>setDreamTravelCompanionsSleepingConfirmed(e.target.checked)}/>
          Para Viagem dos Sonhos: confirmo que todos os companheiros do grupo dormiram neste mesmo Quarto de Dia, sem impedimentos.
        </label>}
        <div className="mt-3 flex flex-wrap items-end gap-2 border border-border rounded p-3">
          <label className="text-xs">Queda em superfície dura (metros)<input type="number" min="3" step="1" value={fallHeight} onChange={event=>setFallHeight(Math.max(3, Number(event.target.value) || 3))} className="field mt-1 w-28" /></label>
          <button disabled={!!busy} onClick={()=>void resolveFall()} className="border border-border rounded px-3 py-2 text-xs disabled:opacity-50">Resolver dano da queda</button>
          <button disabled={!!busy} onClick={()=>void resolveColdCheck()} className="border border-sky-700 rounded px-3 py-2 text-xs disabled:opacity-50">{pendingColdCheckIds.includes(activityCharacterId)?'Resolver frio pendente':'Rolar Resili\u00eancia contra frio'}</button>
          <button disabled={!!busy} onClick={()=>void warmFromCold()} className="border border-border rounded px-3 py-2 text-xs disabled:opacity-50">Aquecer personagem</button>
          <span className="text-[11px] text-text-secondary">Dano usa Dados Base iguais à altura −2; armadura metálica não protege.</span>
          {!!pendingColdCheckIds.length&&<span className="text-xs text-sky-200">Teste de frio pendente por infortúnio: {pendingColdCheckIds.map(id=>characters.find(character=>character.id===id)?.name||id).join(', ')}. Escolha cada personagem e role Resiliência.</span>}
        </div>
        <div className="mt-3 flex flex-wrap items-end gap-2 border border-amber-800/60 rounded p-3">
          <label className="text-xs">Virulência da infecção<input type="number" min="1" max="30" value={diseaseVirulence} onChange={event=>setDiseaseVirulence(Math.max(1, Math.min(30, Number(event.target.value) || 1)))} className="field mt-1 w-24" /></label>
          <button disabled={!!busy} onClick={()=>void exposeToDisease()} className="border border-amber-700 rounded px-3 py-2 text-xs disabled:opacity-50">Resolver exposição à doença</button>
          {characters.find(character=>character.id===activityCharacterId)?.diseaseState?.phase === 'active' && <button disabled={!!busy || !((characters.find(character=>character.id===activityCharacterId)?.diseaseState?.pendingDailyChecks || 0) > 0)} onClick={()=>void resolveDiseaseCheck()} className="border border-amber-700 rounded px-3 py-2 text-xs disabled:opacity-50">Resolver teste diário pendente ({characters.find(character=>character.id===activityCharacterId)?.diseaseState?.pendingDailyChecks || 0})</button>}
          <span className="text-[11px] text-text-secondary">Falha na exposição inicia incubação de 1 dia; depois, cada dia gera uma rolagem de Resiliência contra Virulência.</span>
        </div>
        <div className="mt-3 flex flex-wrap items-end gap-2 border border-cyan-800/60 rounded p-3">
          <label className="text-xs flex items-center gap-2 border border-border rounded px-3 py-2"><input type="checkbox" checked={waterMetalArmor} onChange={event=>setWaterMetalArmor(event.target.checked)} />Usando armadura de metal</label>
          {!characters.find(character=>character.id===activityCharacterId)?.drowningState
            ? <button disabled={!!busy} onClick={()=>void enterWater()} className="border border-cyan-700 rounded px-3 py-2 text-xs disabled:opacity-50">Registrar queda na água</button>
            : <>
              <span className="text-xs text-cyan-200">Estado: {characters.find(character=>character.id===activityCharacterId)?.drowningState?.phase === 'afloat' ? 'boiando' : characters.find(character=>character.id===activityCharacterId)?.drowningState?.phase === 'underwater' ? 'submerso, prendendo a respiração' : `afogando${characters.find(character=>character.id===activityCharacterId)?.drowningState?.minutesUntilDeath ? ` · ${characters.find(character=>character.id===activityCharacterId)?.drowningState?.minutesUntilDeath} min até a morte` : ''}`}</span>
              <button disabled={!!busy} onClick={()=>void resolveWaterTurn()} className="border border-cyan-700 rounded px-3 py-2 text-xs disabled:opacity-50">{characters.find(character=>character.id===activityCharacterId)?.drowningState?.phase === 'drowning' ? 'Avançar rodada de afogamento' : 'Resolver teste na água'}</button>
              <button disabled={!!busy} onClick={()=>void rescueFromWater()} className="border border-emerald-700 rounded px-3 py-2 text-xs disabled:opacity-50">Resgatar da água</button>
            </>}
          <span className="text-[11px] text-text-secondary">Falha ao boiar afunda; falha ao prender a respiração inicia afogamento e dano de Força por rodada. Força Quebrada gera prazo aleatório de D6 minutos.</span>
        </div>
        <div className="mt-3 flex flex-wrap items-end gap-2 border border-purple-800/60 rounded p-3">
          <label className="text-xs">Tipo de veneno<select value={poisonKind} onChange={event=>setPoisonKind(event.target.value as typeof poisonKind)} className="field mt-1 block"><option value="lethal">Letal — Força</option><option value="paralyzing">Paralisante — Agilidade</option><option value="soporific">Sonífero — Astúcia</option><option value="hallucinogenic">Alucinógeno — Empatia</option></select></label>
          <label className="text-xs">Potência<input type="number" min="1" max="30" value={poisonPotency} onChange={event=>setPoisonPotency(Math.max(1, Math.min(30, Number(event.target.value) || 1)))} className="field mt-1 w-20" /></label>
          <label className="text-xs">Arma<select value={poisonWeaponIndex} onChange={event=>setPoisonWeaponIndex(Number(event.target.value))} className="field mt-1 block">{(characters.find(character=>character.id===activityCharacterId)?.weapons || []).map((weapon:any,index:number)=>({weapon,index})).filter(item=>PoisonRulesEngine.canCoatWeapon(item.weapon)).map(item=><option key={item.index} value={item.index}>{item.weapon.name || ("Arma " + (item.index + 1))}{item.weapon.poisonCoating?.quartersRemaining ? (" · já envenenada (" + item.weapon.poisonCoating.kind + ")") : ""}</option>)}</select></label><button disabled={!!busy || !((characters.find(character=>character.id===activityCharacterId)?.weapons || []).some(weapon=>PoisonRulesEngine.canCoatWeapon(weapon)))} onClick={()=>void coatWeaponWithPoison()} className="border border-purple-700 rounded px-3 py-2 text-xs disabled:opacity-50">Aplicar na arma (ação lenta)</button><button disabled={!!busy} onClick={()=>void exposeToPoison()} className="border border-purple-700 rounded px-3 py-2 text-xs disabled:opacity-50">Resolver exposição</button>
          {characters.find(character=>character.id===activityCharacterId)?.poisonState?.active && <button disabled={!!busy} onClick={()=>void resolvePoisonRound()} className="border border-purple-700 rounded px-3 py-2 text-xs disabled:opacity-50">Avançar rodada do veneno</button>}
          {characters.find(character=>character.id===activityCharacterId)?.poisonState && <button disabled={!!busy} onClick={()=>void applyAntidote()} className="border border-emerald-700 rounded px-3 py-2 text-xs disabled:opacity-50">Aplicar antídoto</button>}
          <span className="text-[11px] text-text-secondary">Resistência usa Força + Resiliência. Efeito total causa 1 dano por rodada até o atributo Quebrar; sonífero causa inconsciência temporária.</span>
        </div>
      </div>

      <div>
        <div className="flex items-center gap-2 mb-2"><Backpack className="w-4 h-4 text-accent" /><b className="text-sm">Dados de recurso</b><span className="text-[11px] text-text-secondary">Comida e água são consumidas ao tocar; 1-2 reduz a reserva. Flechas e tochas rolam quando usadas.</span></div>
        <div className="space-y-2">{characters.map(character => <div key={character.id} className="p-3 bg-bg-base rounded border border-border/60">
          <b className="text-sm">{character.name}{character.status === 'dead' ? ' — Morto' : ''}</b>
          {!!character.conditions?.length && <p className="text-[11px] text-text-secondary mt-1">Condições: {character.conditions.join(', ')}{character.survivalState?.mustSleep ? ' — precisa dormir' : ''}</p>}{!!character.statusEffects?.length&&<p className="text-[11px] text-amber-200 mt-1">Efeitos: {character.statusEffects.map(effect=>`${effect.label}${effect.detail?` — ${effect.detail}`:''}`).join('; ')}</p>}
          {character.diseaseState && <p className="text-[11px] text-amber-200 mt-1">Doença: Virulência {character.diseaseState.virulence} · {character.diseaseState.phase === 'incubating' ? `${character.diseaseState.quartersUntilManifestation} QD até manifestação` : `${character.diseaseState.pendingDailyChecks} teste(s) diário(s) pendente(s)`}</p>}
          {character.diseaseState?.origin==='plague_brother'&&character.diseaseState.phase==='active'&&<p className="text-[11px] text-purple-200 mt-1">Alucinações da praga demoníaca: pessoas podem parecer demônios distorcidos para {character.name}. Percepções são subjetivas; o Mestre não deve transformar a alucinação em fatos do mundo.</p>}
          {character.liceState && <p className="text-[11px] text-amber-200 mt-1">Piolhos: próximo dano de Agilidade em {character.liceState.quartersUntilDamage} QD; sono bloqueado até Cura.</p>}
          {character.drowningState && <p className="text-[11px] text-cyan-200 mt-1">Água: {character.drowningState.phase}{character.drowningState.minutesUntilDeath ? ` · prazo de morte ${character.drowningState.minutesUntilDeath} min` : ''}.</p>}
          {character.poisonState && <p className="text-[11px] text-purple-200 mt-1">Veneno: {character.poisonState.kind}{character.poisonState.active ? ` · Potência ${character.poisonState.potency}, 1 dano/rodada em ${character.poisonState.kind === 'lethal' ? 'Força' : character.poisonState.kind === 'paralyzing' ? 'Agilidade' : character.poisonState.kind === 'soporific' ? 'Astúcia' : 'Empatia'}` : ''}{character.poisonState.unconsciousMinutesRemaining ? ` · inconsciente ${character.poisonState.unconsciousMinutesRemaining} min` : ''}{character.poisonState.criticalPending ? ' · dano atípico grave pendente' : ''}.</p>}
          {character.weapons?.filter(weapon=>weapon?.poisonCoating?.quartersRemaining>0).map((weapon:any,index:number)=><p key={`poison-coated-${character.id}-${index}`} className="text-[11px] text-purple-200 mt-1">Arma envenenada: {weapon.name || 'arma'} · {weapon.poisonCoating.kind} · expira no fim do próximo Quarto de Dia ou no primeiro ataque válido.</p>)}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2 mt-2">
            {([
              ['food', 'Comida', UtensilsCrossed], ['water', 'Água', Droplets], ['arrows', 'Flechas', Footprints], ['torches', 'Tochas', Flame],
            ] as const).map(([resource, label, Icon]) => <button key={resource} disabled={!!busy} onClick={() => void rollResource(character, resource)} className="p-2 border border-border rounded text-xs flex items-center justify-center gap-2 hover:border-accent disabled:opacity-50"><Icon className="w-4 h-4" />{label}{resource==='food'&&character.foodPoisonState?' (contaminada P'+character.foodPoisonState.potency+')':''}: {character.resourceDice?.[resource] || '—'}</button>)}
          </div>
          {!!character.journeyProvisions?.length&&<div className="mt-2 rounded border border-emerald-900/60 p-2 space-y-1"><p className="text-[11px] text-emerald-200">Rendimentos e reservas da jornada</p>{character.journeyProvisions.map(batch=>{const spoiled=batch.perishable&&batch.acquiredDay!==(campaign.currentDay||1);const label=batch.kind==='vegetables'?'Vegetais':batch.kind==='meat'?'Carne':batch.kind==='fish'?'Peixe':batch.kind==='food'?'Comida':'Peles';const rawFood=['vegetables','meat','fish'].includes(batch.kind);return <div key={batch.id} className="flex flex-wrap items-center gap-2 text-xs"><span>{label}: {batch.quantity}{spoiled?' · estragado':''}</span>{rawFood&&!spoiled&&<button disabled={!!busy||character.status==='dead'} onClick={()=>void act(`raw-meal:${character.id}:${batch.id}`,async()=>{const outcome=await JourneyService.consumeRawJourneyProvision(campaign.id,character.id,batch.id);setLastRoll(`${character.name} consumiu 1 ${label}; restam ${outcome.remaining}. O Dado de Comida não foi rolado.`);})} className="rounded border border-emerald-800 px-2 py-1 disabled:opacity-50">Comer cru</button>}{rawFood&&!spoiled&&<button disabled={!!busy||!activityCharacterId} onClick={()=>void act(`cook-provision:${character.id}:${batch.id}`,async()=>{const cook=characters.find(item=>item.id===activityCharacterId);const outcome=await JourneyService.cookJourneyProvision(campaign.id,character.id,batch.id,activityCharacterId,batch.quantity,true);setLastRoll(`${cook?.name||'Cozinheiro'} preparou ${outcome.rawConsumed} unidade(s) e gerou ${outcome.foodProduced} de Comida; ${outcome.storedSteps} passo(s) foram para o Dado de Comida e ${outcome.overflow} ficaram em reserva.`);})} className="rounded border border-orange-700 px-2 py-1 disabled:opacity-50">Cozinhar na fogueira com selecionada</button>}{batch.kind==='pelts'&&<button disabled={!!busy||!activityCharacterId} onClick={()=>void act(`tan-pelts:${character.id}:${batch.id}`,async()=>{const tanner=characters.find(item=>item.id===activityCharacterId);const outcome=await JourneyService.tanJourneyPelts(campaign.id,character.id,batch.id,activityCharacterId,batch.quantity);setLastRoll(outcome.successes>0?`${tanner?.name||'Curtidor'} transformou ${outcome.processed} Pele(s) em ${outcome.leather} Couro.`:`Falha de ${tanner?.name||'Curtidor'}: ${outcome.destroyedPelts} Pele(s) destruídas.`);})} className="rounded border border-amber-700 px-2 py-1 disabled:opacity-50">Curtir com selecionada</button>}{batch.kind==='food'&&<button disabled={!!busy||character.resourceDice.food==='d12'} onClick={()=>void act(`store-food:${character.id}:${batch.id}`,async()=>{const outcome=await JourneyService.storeCookedFoodUnit(campaign.id,character.id,batch.id);setLastRoll(`${character.name}: Dado de Comida agora ${outcome.foodDie}; restam ${outcome.remaining} unidade(s) em reserva.`);})} className="rounded border border-emerald-700 px-2 py-1 disabled:opacity-50">Adicionar 1 ao Dado de Comida</button>}{(spoiled||batch.kind==='pelts'||batch.kind==='food')&&<button disabled={!!busy} onClick={()=>void act(`discard-provision:${character.id}:${batch.id}`,async()=>{const removed=await JourneyService.discardJourneyProvision(campaign.id,character.id,batch.id);setLastRoll(`${removed} descartado(s).`);})} className="rounded border border-border px-2 py-1 disabled:opacity-50">Descartar</button>}</div>;})}</div>}
        </div>)}</div>
        {lastRoll && <p className="mt-2 text-xs text-accent">{lastRoll}</p>}
      </div>

      <button disabled={!!busy} onClick={() => void act('time', () => JourneyService.passPeriod(campaign.id))} className="w-full p-3 border border-border rounded flex items-center justify-center gap-2 text-sm disabled:opacity-50"><Hourglass className="w-4 h-4" />Passar 1 período sem atividade</button>
      {!!worldState.journey.events.length && <details className="border border-border rounded"><summary className="p-3 cursor-pointer text-sm">Eventos recentes e pendências da jornada</summary><div className="p-3 pt-0 space-y-2">{visibleJourneyEvents.map(event => {
        const mechanic=event.result?.mechanic as Record<string,unknown>|undefined;
        const officialReceipt=OfficialEncounterReceiptService.fromEvent(event);
        const escaped=(event.result?.escapedCharacterIds as string[]|undefined)||[];
        const affected=characters.find(character=>character.id===event.result?.actorId);
        const fishingGear=(affected?.gear||[]).map((item:any,index:number)=>({item,index,name:String(item?.name||item?.label||`item ${index+1}`)})).filter(entry=>/anzol|linha|rede|pesca|fishing|hook|net/i.test(entry.name));
        return <div key={event.id} data-testid={`fl-journey-event-${event.id}`} className="text-xs"><b>{event.title}</b><p className="text-text-secondary">{event.detail}</p>
          {officialReceipt && <output data-testid={`fl-official-receipt-${event.id}`}
            data-encounter-id={officialReceipt.encounterId}
            data-status={officialReceipt.status}
            data-visit={officialReceipt.visit}
            data-choice-id={officialReceipt.choiceId}
            data-stage={officialReceipt.stage}
            data-committed={officialReceipt.committed ? 'true' : 'false'}
            data-follow-up-pending={officialReceipt.followUpPending ? 'true' : 'false'}
            data-resource-transfer-committed={officialReceipt.resourceTransferCommitted ? 'true' : 'false'}
            aria-live="polite" className="mt-2 block rounded border border-teal-800/70 p-2 text-teal-100">
            {officialReceipt.label}
          </output>}
          {event.status==='resolved'&&Number(mechanic?.encounterId)===2&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='watch'&&
            !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.duelistsIntroducedAt&&
            <button disabled={!!busy||event.hexId!==campaign.currentHexId} onClick={()=>void act(`duel-greet:${event.id}`,async()=>{
              await JourneyService.greetOfficialDuelistsAfterWatching(campaign.id,event.id);
              setLastRoll('Os artistas se apresentaram: Heldemin e Alachran; identidades atualizadas nos NPCs existentes.');
            })} className="mt-2 rounded border border-teal-700 px-3 py-2 disabled:opacity-50">Cumprimentar artistas após observar</button>}
          {event.status==='resolved'&&Number(mechanic?.encounterId)===10&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.foxFollowerActive===true&&
            <div className="mt-2 rounded border border-teal-800/60 p-2 space-y-1">
              <p>A raposa curiosa acompanha a companhia entre hexes terrestres; seus deslocamentos ficam no cadastro de NPCs. Ela não recebe atributos demoníacos inventados.</p>
              <button disabled={!!busy} onClick={()=>void act(`fox-dismiss:${event.id}`,async()=>{
                await JourneyService.dismissOfficialFox(campaign.id,event.id);
                setLastRoll('O grupo se despediu da raposa. Ela deixou de seguir a companhia.');
              })} className="rounded border border-teal-700 px-3 py-2 disabled:opacity-50">Despedir-se da raposa (no mesmo hex)</button>
            </div>}
          {event.status==='open'&&Number(mechanic?.encounterId)===3&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='parley'&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
            <div className="mt-2 rounded border border-teal-800 p-3 space-y-2">
              <p>A Mestra decide a resposta da escolta. O livro não fixa teste nem garante que os orcs soltem Sargah. Registre o motivo sem simular um combate.</p>
              <textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Motivo da decisão da escolta (mínimo 12 caracteres)"/>
              {(['release_sargah','escort_continues'] as const).map(response=><button key={response} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`orc-parley:${event.id}:${response}`,async()=>{
                await JourneyService.resolveOfficialOrcParley(campaign.id,event.id,response,encounterResolution(event.id));
                setEncounterResolution(event.id,'');setLastRoll(response==='release_sargah'?'Sargah foi libertado e cadastrado como NPC persistente.':'Escolta prosseguiu com Sargah; nenhuma libertação artificial.');
              })} className="mr-2 rounded border border-teal-700 px-3 py-2 disabled:opacity-50">{response==='release_sargah'?'Mestra: escolta liberta Sargah':'Mestra: escolta não liberta Sargah'}</button>)}
            </div>}
          {event.status==='open'&&Number(mechanic?.encounterId)===12&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='offer_camp'&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
            <div className="mt-2 rounded border border-amber-800 p-3 space-y-2">
              <p>Argam aceita comida e histórias, mas não há sucesso automático. Escolha comida física ou uma reserva preparada e descreva a história. O item será consumido de verdade.</p>
              <textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={800} className="field" placeholder="História contada aos assaltantes (mínimo 12 caracteres)"/>
              <label className="block">Unidades reais oferecidas <input type="number" min={1} max={30} className="field w-20 ml-2" value={banditFoodQuantities[event.id]||1} onChange={e=>setBanditFoodQuantities(v=>({...v,[event.id]:Math.max(1,Math.min(30,Number(e.target.value)||1))}))}/></label>
              <div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).flatMap(c=>[
                ...(c.journeyProvisions||[]).filter(b=>b.kind==='food'&&b.quantity>0).map(b=>({character:c,source:'provision' as const,id:b.id,label:`${c.name} · Comida preparada (${b.quantity})`,available:b.quantity,snapshot:InventorySelectionSnapshot.capture(b)})),
                ...(c.gear||[]).map((g:any,index:number)=>({g,index})).filter(({g})=>['comida','racao','racoes','provisao','provisoes','refeicao','refeicoes'].includes(String(g?.name||g?.label||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]/g,''))).map(({g,index})=>({character:c,source:'gear' as const,id:`gear:${index}`,label:`${c.name} · ${String(g.name||g.label)} (${Math.max(0,Number(g.quantity??1)||0)})`,available:Math.max(0,Number(g.quantity??1)||0),snapshot:InventorySelectionSnapshot.capture(g)})),
              ]).map(option=><button key={`${option.character.id}:${option.source}:${option.id}`} data-testid={`fl-bandit-offer-${event.id}-${option.character.id}-${option.source}-${option.id}`} disabled={!!busy||encounterResolution(event.id).trim().length<12||option.available<(banditFoodQuantities[event.id]||1)} onClick={()=>void act(`bandit-offer:${event.id}:${option.character.id}`,async()=>{
                await JourneyService.offerOfficialHungryBanditsCamp(campaign.id,event.id,option.character.id,option.source,option.id,banditFoodQuantities[event.id]||1,encounterResolution(event.id),option.snapshot);
                setEncounterResolution(event.id,'');setLastRoll('Comida transferida, história registrada. A Mestra ainda decide a reação de Argam.');
              })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Oferecer {option.label}</button>)}</div>
            </div>}
          {event.status==='open'&&Number(mechanic?.encounterId)===12&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='bandit_offer_pending_reaction'&&
            <div className="mt-2 rounded border border-emerald-800 p-3 space-y-2"><p>Comida já entregue, história registrada. A Mestra decide a resposta de Argam. Não conceder descanso, cura ou tempo automaticamente.</p>
              {(['accept','decline'] as const).map(reaction=><button key={reaction} disabled={!!busy} onClick={()=>void act(`bandit-react:${event.id}:${reaction}`,async()=>{
                await JourneyService.resolveOfficialHungryBanditsReaction(campaign.id,event.id,reaction);
                setLastRoll(reaction==='accept'?'Argam aceitou dividir o acampamento; use as ações normais de jornada para descansar.':'Argam recusou; a comida oferecida permanece entregue.');
              })} className="mr-2 rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">{reaction==='accept'?'Mestra: Argam aceita':'Mestra: Argam recusa'}</button>)}
            </div>}
          {event.status==='open'&&Number(mechanic?.encounterId)===27&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='comfort'&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
            <div className="mt-2 rounded border border-teal-800 p-3 space-y-2">
              <p>Linn permanece em choque. A Mestra decide se ela aceita acompanhar a companhia; isto não cura ferimentos nem cria uma personagem jogável.</p>
              <textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Justificativa da decisão de Linn (12–600 caracteres)"/>
              {(['accompany','remain'] as const).map(response=><button key={response} disabled={!!busy||encounterResolution(event.id).trim().length<12}
                onClick={()=>void act(`linn-shelter:${event.id}:${response}`,async()=>{
                  await JourneyService.resolveOfficialLinnShelter(campaign.id,event.id,response,encounterResolution(event.id));
                  setEncounterResolution(event.id,'');setLastRoll(response==='accompany'?'Linn acompanha o grupo por rotas terrestres confirmadas.':'Linn permanece na clareira, sem transporte presumido.');
                })} className="mr-2 rounded border border-teal-700 px-3 py-2 disabled:opacity-50">{response==='accompany'?'Mestra: Linn aceita acompanhar':'Mestra: Linn permanece'}</button>)}
            </div>}
          {event.status==='resolved'&&Number(mechanic?.encounterId)===27&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.linnFollowerActive===true&&
            !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.linnDisclosedAt&&
            <div className="mt-2 space-y-2 rounded border border-teal-800 p-3">
              <p>Linn — recuperação pelo alimento: {((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.linnFedDays as number[]||[]).length} dia(s) alimentada. A meta D6 é sorteada no primeiro alimento real. Só conta uma vez por dia, sem curar ou ensinar magia automaticamente.</p>
              {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).flatMap(c=>[
                ...(c.journeyProvisions||[]).filter(p=>p.kind==='food'&&p.quantity>0).map(p=>({c,source:'provision' as const,foodId:p.id,label:`${c.name} · Comida preparada (${p.quantity})`,snapshot:InventorySelectionSnapshot.capture(p)})),
                ...(c.gear||[]).map((g:any,i:number)=>({c,source:'gear' as const,foodId:`gear:${i}`,label:`${c.name} · ${g.name||''} (${g.quantity||1})`,snapshot:InventorySelectionSnapshot.capture(g)}))
                  .filter(x=>/^(comida|ra[çc][aã]o|ra[çc][oõ]es|provis[aã]o|provis[oõ]es|refei[çc][aã]o|refei[çc][oõ]es)$/i.test(x.label.split(' · ')[1]?.replace(/ \(.*\)$/,'')||'')),
              ]).map(o=><button key={`linn-feed:${event.id}:${o.c.id}:${o.source}:${o.foodId}`} data-testid={`fl-linn-feed-${event.id}-${o.c.id}-${o.source}-${o.foodId}`} disabled={!!busy}
                onClick={()=>void act(`linn-feed:${event.id}:${o.c.id}:${o.foodId}`,async()=>{
                  const r=await JourneyService.feedOfficialLinn(campaign.id,event.id,o.c.id,o.source,o.foodId,Math.random,o.snapshot);
                  setLastRoll(`Linn: ${r.daysFed}/${r.daysRequired} dias alimentada. ${r.disclosed?'Contou o que houve em Mataselvagem.':'Ainda guarda silêncio.'}`);
                })} className="mr-2 rounded border border-teal-700 px-3 py-2 disabled:opacity-50">Oferecer 1 ração · {o.label}</button>)}
            </div>}
          {event.status==='resolved'&&Number(mechanic?.encounterId)===27&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.linnFollowerActive===true&&
            <button disabled={!!busy} onClick={()=>void act(`linn-farewell:${event.id}`,async()=>{
              await JourneyService.dismissOfficialLinn(campaign.id,event.id);setLastRoll('Linn permaneceu no hex atual; não seguirá mais a companhia.');
            })} className="mt-2 rounded border border-teal-700 px-3 py-2 disabled:opacity-50">Deixar Linn em segurança neste hex (sem cura automática)</button>}
          {event.status==='resolved'&&Number(mechanic?.encounterId)===6&&
           (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseDisposition==='claim'&&
           !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseFamilyRewardAt&&
           <div className="mt-2 rounded border border-emerald-800 p-3 space-y-2">
             <p className="text-emerald-200">Família Lavide: o cavalo está sob guarda, ainda não pertence definitivamente ao grupo. A recompensa PRECIOSA exige entregar cavalo e insígnia no castelo associado.</p>
             {campaign.currentHexId===event.hexId&&
              !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseInsigniaRecoveredAt&&
              <div className="flex flex-wrap gap-2"><p className="w-full">O cavalo foi levado antes de retirar a insígnia. O grupo ainda está no hex de origem e pode recolhê-la.</p>
                {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy}
                 onClick={()=>void act(`horse-late-crest:${event.id}:${c.id}`,async()=>{
                   await JourneyService.retrieveOfficialHorseInsignia(campaign.id,event.id,c.id);
                   setLastRoll(`${c.name} recuperou a insígnia da casa Lavide no local de origem.`);
                 })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name} recolhe insígnia</button>)}
              </div>}
             {campaign.currentHexId===event.hexId&&
              !!(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseInsigniaRecoveredAt&&
              !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseValuableRecoveredAt&&
              <div className="flex flex-wrap gap-2"><p className="w-full">O item VALIOSO ainda está no alforje; sorteie-o antes de partir.</p>
                {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy}
                 onClick={()=>void act(`horse-late-valuable:${event.id}:${c.id}`,async()=>{
                   const o=await JourneyService.recoverOfficialHorseValuable(campaign.id,event.id,c.id);
                   setLastRoll(`D66 ${o.treasure.roll}: ${o.treasure.name} transferido para ${c.name}.`);
                 })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name} recolhe VALIOSO</button>)}
              </div>}
             {!(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseFamilyCastlePoiId?
              <div className="flex flex-wrap gap-2"><p className="w-full">A Mestra deve associar explicitamente um castelo já descoberto do atlas. O livro não fornece coordenadas.</p>
               {worldState.campaignPois.filter(p=>p.discovered&&!p.hidden&&
                 /(castle|castelo|fortress|fortaleza|stronghold)/i.test([p.type,...p.tags].join(' '))&&
                 !['destroyed','ruined','abandoned'].includes(p.status)).map(p=><button key={p.id} disabled={!!busy}
                 onClick={()=>void act(`lavide-castle:${event.id}:${p.id}`,async()=>{
                   await JourneyService.assignOfficialHorseFamilyCastle(campaign.id,event.id,p.id);
                   setLastRoll(`Castelo ${p.name} associado à família Lavide; é necessário viajar até lá.`);
                 })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Associar castelo: {p.name}</button>)}
              </div>:
              <div className="space-y-2"><p>Castelo escolhido: {worldState.campaignPois.find(p=>p.id===(event.result?.officialSceneDecision as Record<string,unknown>).horseFamilyCastlePoiId)?.name||'não encontrado'}. O botão só funciona no hex correto, com a insígnia e o cavalo original presentes.</p>
               {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy}
                onClick={()=>void act(`lavide-return:${event.id}:${c.id}`,async()=>{
                  const o=await JourneyService.returnOfficialHorseToFamily(campaign.id,event.id,c.id);
                  setLastRoll(o.treasure.requiresArtifactSelection?'D66 66: cavalo devolvido e reconhecido, artefato pendente de seleção oficial.':`Cavalo concedido pela família e ${o.treasure.name} recebido por ${c.name}.`);
                })} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Entregar cavalo e insígnia · recompensa para {c.name}</button>)}
              </div>}
           </div>}
          {event.status==='resolved'&&Number(mechanic?.encounterId)===6&&
           (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseFamilyArtifactPending===true&&
           <p className="rounded border border-amber-800 p-2 text-amber-200">D66 66 PRECIOSO: a família reconheceu a entrega, mas o artefato oficial ainda exige escolha pela Mestra. Não foi gerado item genérico.</p>}
          {Number(mechanic?.encounterId)===36&&['open','resolved'].includes(event.status)&&
            (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.hatchlingHatched===true&&(()=>{
              const decision=event.result?.officialSceneDecision as Record<string,unknown>;
              const growth=decision.hatchlingGrowth as Record<string,unknown>|undefined;
              const through=Number(growth?.growthAppliedThroughDay??decision.hatchlingHatchedAtDay??campaign.currentDay??1);
              const weeks=Math.floor(Math.max(0,Number(campaign.currentDay||1)-through)/7);
              return weeks>0?<div className="rounded border border-amber-800 p-2 space-y-2"><p>Filhote de dragão negro: a R88 concede +1 Força e +1 Armadura por semana completa desde a eclosão/último crescimento.</p><button disabled={!!busy} onClick={()=>void act(`hatchling-growth:${event.id}`,async()=>{const result=await JourneyService.applyOfficialHairlessDragonWeeklyGrowth(campaign.id,event.id);setLastRoll(`Crescimento após ${result.weeksApplied} semana(s): Força ${result.strength}, Armadura ${result.armor}.`);})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Aplicar {weeks} semana(s) completa(s) de crescimento</button></div>:null;
            })()}
          {event.status==='open'&&mechanic?.kind==='official_random_encounter'&&Number(mechanic.encounterId)===9&&event.result?.ancientRuinRoll===undefined&&<div className="mt-2 rounded border border-emerald-800/70 p-3 space-y-2"><p className="text-emerald-100">Ruínas Antigas · Guia do Mestre p. 151. A tabela D66 determinará o tipo de ruína e seu antigo ocupante. O local descoberto será preservado no mapa da campanha.</p><button data-testid={`fl-ruin-roll-${event.id}`} disabled={!!busy} onClick={()=>void act(`ancient-ruins:${event.id}`,async()=>{const outcome=await JourneyService.discoverAncientRuin(campaign.id,event.id);setLastRoll(outcome.requiresRuling?`D66 ${outcome.roll}: lacuna na tabela original. Registre a arbitragem da mesa.`:`D66 ${outcome.roll}: ${outcome.ruinType}. Ruína descoberta e salva no mapa da campanha.`);})} className="border border-emerald-700 rounded px-3 py-2 disabled:opacity-50">Rolar D66 e descobrir a ruína</button></div>}
          {event.status==='open'&&Number(mechanic?.encounterId)===9&&event.result?.ancientRuinRoll===63&&<div className="mt-2 rounded border border-amber-800/70 p-3 space-y-2"><p className="text-amber-200">A tabela oficial da p. 151 não apresenta linha para D66 63. O resultado não foi alterado. A mesa deve escolher uma linha impressa e registrar a justificativa antes de criar o local.</p><select value={ruinChoices[event.id]||''} onChange={e=>setRuinChoices(cur=>({...cur,[event.id]:e.target.value}))} className="field"><option value="">Escolha uma linha oficial</option>{OfficialEncounterRulesEngine.ancientRuinOptions().map(option=><option key={option.kind} value={option.kind}>{option.kind}</option>)}</select><textarea rows={2} maxLength={500} value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} className="field" placeholder="Justificativa da arbitragem (mínimo 12 caracteres)"/><button data-testid={`fl-ruin-gap-${event.id}`} disabled={!!busy||!ruinChoices[event.id]||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`ruin-gap:${event.id}`,async()=>{await JourneyService.adjudicateAncientRuinGap(campaign.id,event.id,ruinChoices[event.id],encounterResolution(event.id));setLastRoll('D66 63 mantido, arbitragem registrada e ruína persistida no mapa.');setEncounterResolution(event.id,'');})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Registrar arbitragem e descobrir ruína</button></div>}
          {event.status==='open'&&mechanic?.kind==='official_random_encounter'&&Number(mechanic.encounterId)===13&&<div className="mt-2 rounded border border-amber-800/70 p-3 space-y-2"><p className="text-amber-100">Irmão da Praga · Guia do Mestre p. 153. Tocar o corpo causa infecção com Virulência 3, que segue o motor de doenças nos próximos dias. O medalhão pode ser retirado uma única vez.</p><div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead'&&CharacterActionRulesEngine.canAct(c)).map(c=><React.Fragment key={c.id}>{!((event.result?.plagueTouchedCharacterIds as string[]|undefined)||[]).includes(c.id)&&<button disabled={!!busy} onClick={()=>void act(`plague-touch:${event.id}:${c.id}`,async()=>{const o=await JourneyService.interactPlagueBrother(campaign.id,event.id,c.id,'touch');setLastRoll(`${c.name} tocou o cadáver; ${o.newlyInfected?'nova infecção em incubação':'nenhuma nova infecção (doença preexistente preservada)'}.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">{c.name} toca o corpo</button>}{!event.result?.plagueMedallionCharacterId&&<button data-testid={`fl-plague-medallion-${event.id}-${c.id}`} disabled={!!busy} onClick={()=>void act(`plague-medallion:${event.id}:${c.id}`,async()=>{const o=await JourneyService.interactPlagueBrother(campaign.id,event.id,c.id,'take_medallion');setLastRoll(`${c.name} retirou o medalhão; ${o.newlyInfected?'nova infecção em incubação':'sem nova infecção; doença preexistente preservada'}.`);})} className="border border-orange-700 rounded px-3 py-2 disabled:opacity-50">{c.name} recolhe medalhão (contato)</button>}</React.Fragment>)}</div><button data-testid={`fl-plague-leave-${event.id}`} disabled={!!busy} onClick={()=>void act(`plague-leave:${event.id}`,async()=>{await JourneyService.leavePlagueBrother(campaign.id,event.id);setLastRoll('O grupo deixou o cadáver. As infecções adquiridas permanecem registradas.');})} className="rounded border border-border px-3 py-2 disabled:opacity-50">Deixar o corpo e prosseguir</button></div>}
          {event.status==='open'&&mechanic?.kind==='official_random_encounter'&&Number(mechanic.encounterId)===15&&<div className="mt-2 rounded border border-amber-800/70 p-3 space-y-2"><p className="text-amber-100">Pedintes · Livro do Mestre pp. 153–154: vinte pessoas famintas, de mãos estendidas e em silêncio. Nenhuma rolagem ou transferência de comida/moedas é determinada nesta entrada. Qualquer doação deve ser realizada pelo inventário antes de prosseguir.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={800} className="field" placeholder="Observações opcionais do grupo (sem aplicar efeitos ou gastar recursos)"/><div className="flex flex-wrap gap-2">{([['observed','Observar'],['spoke','Tentar conversar'],['continued','Prosseguir']] as const).map(([reaction,label])=><button key={reaction} data-testid={`fl-beggars-${event.id}-${reaction}`} disabled={!!busy} onClick={()=>void act(`beggars:${event.id}:${reaction}`,async()=>{await JourneyService.resolveBeggarsEncounter(campaign.id,event.id,reaction,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('Interação com os vinte pedintes registrada, sem transferências ou rolagens automáticas.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{label}</button>)}</div></div>}
          {event.status==='open'&&mechanic?.kind==='official_random_encounter'&&OfficialEncounterRulesEngine.specialScene(Number(mechanic.encounterId))&&<div className="mt-2 rounded border border-teal-800/70 p-3 space-y-2"><p className="text-teal-100">{OfficialEncounterRulesEngine.specialScenePublicDetail(Number(mechanic.encounterId),Number(event.result?.officialEncounterVisitNumber||1))}</p><p className="text-text-secondary">{Number(mechanic.encounterId)===20?'Decida como o grupo reage. A advertência e o combate dos cinco Ents estão ligados às regras oficiais; a Mestra continua decidindo se oferece um presente e qual é.':[32,34].includes(Number(mechanic.encounterId))?'A R88 não fixa duração exata nem quantidade de participantes desta cena; registre a decisão da MdJ sem criar estatísticas ou relógios.':Number(mechanic.encounterId)===33?'Escolha qual lado apoiar. Os quatro soldados e o Minotauro R88 entram no combate vinculado.':Number(mechanic.encounterId)===35?'Karg aceita passagem por uma oferta da R88 ou o grupo pode recuar/enfrentá-lo. A Força dele é escolha da MdJ.':'Decida como o grupo reage; ações com consequências permanecem registradas até serem resolvidas.'}</p>{(!(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId||(Number(mechanic.encounterId)===35&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='karg_offer_refused'))?<><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={800} className="field" placeholder="Observações opcionais das aventureiras"/><div className="flex flex-wrap gap-2">{OfficialEncounterRulesEngine.specialSceneChoices(Number(mechanic.encounterId),Number(event.result?.officialEncounterVisitNumber||1)).map(choice=><button key={choice.id} data-testid={`fl-official-choice-${Number(mechanic.encounterId)}-${choice.id}`} disabled={!!busy} onClick={()=>void act(`official-scene:${event.id}:${choice.id}`,async()=>{await JourneyService.decideOfficialScene(campaign.id,event.id,choice.id,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(choice.closes?'Escolha registrada, encontro concluído sem efeitos artificiais.':'Escolha registrada; consequência pendente da cena R88.');})} className="border border-teal-700 rounded px-3 py-2 disabled:opacity-50">{choice.label}</button>)}</div></>:<><p className="text-amber-200">Consequência pendente: {String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.pending||'manual')}. A escolha está persistida; não é permitido ignorar combate, teste ou interação que ainda precise ser resolvida.</p>{Number(mechanic.encounterId)===2&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='attack'&&<p role="alert" className="rounded border border-red-700 p-2 text-red-200">A base R88 não contém fichas de combate oficiais para Heldemin e Alachran. O seletor de criaturas genéricas está bloqueado para esta escolha; não improvise atributos nem substitua os artistas por monstros. A adoção explícita de perfis típicos publicados está disponível abaixo; eles não serão apresentados como fichas individuais da fonte.</p>}{Number(mechanic.encounterId)===14&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='inspect_symbol'&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&<div className="rounded border border-amber-800 p-2 space-y-2"><p>Observar o símbolo exige Discernimento real. A falha mantém a ilusão enquanto o símbolo está à vista.</p>{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead'&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`penitent-symbol:${event.id}:${c.id}`,async()=>{const result=await JourneyService.inspectOfficialPenitentSymbol(campaign.id,event.id,c.id);setLastRoll(`${c.name}: Discernimento ${result.roll.successes} sucesso(s). ${result.affected?'A ilusão permanece enquanto o símbolo estiver à vista.':'A personagem resistiu à ilusão.'}`);})} className="mr-2 rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name} observa o símbolo</button>)}</div>}{Number(mechanic.encounterId)===14&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='penitent_illusion_active'&&<div className="rounded border border-red-700 p-2 space-y-2"><p>A ilusão persiste para a personagem afetada. Deixe a procissão fora de vista para encerrar esse efeito.</p><button disabled={!!busy} onClick={()=>void act(`penitent-symbol-leave:${event.id}`,async()=>{await JourneyService.leaveOfficialPenitentSymbolView(campaign.id,event.id);setLastRoll('O símbolo saiu de vista; a ilusão cessou e foi removida da ficha.');})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Deixar o símbolo fora de vista</button></div>}{Number(mechanic.encounterId)===16&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='rob'&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&<div className="rounded border border-amber-800 p-2 space-y-2"><p>O livro não determina se a tentativa consegue acesso. A Mestra precisa registrar a adjudicação; só depois o motor rola o item SIMPLES e o VALIOSO das tabelas de covil (MdJ pp. 188–190) e transfere o resultado uma vez.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={400} className="field" placeholder="Como a Mestra adjudicou o acesso ao balão? (12–400 caracteres)"/><div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`balloon-loot:${event.id}:${c.id}`,async()=>{const result=await JourneyService.recoverOfficialBalloonLoot(campaign.id,event.id,c.id,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(`Tesouro do balão: ${result.simple.name} (D66 ${result.simple.roll}) e ${result.valuable.name} (D66 ${result.valuable.roll}) transferidos para ${c.name}.`);})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Mestra confirma acesso · recolher para {c.name}</button>)}</div></div>}{Number(mechanic.encounterId)===16&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='balloon_loot_recovered'&&<p className="rounded border border-emerald-800 p-2 text-emerald-200">Tesouro do balão transferido uma única vez. A reação de Andred e o encerramento da cena ainda exigem adjudicação da Mestra.</p>}{(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&<><p className="text-text-secondary">Se a ação ainda NÃO foi executada, é possível voltar atrás e sair desta cena, registrando uma justificativa. Isso não desfaz consequências reais.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={400} className="field" placeholder="Por que o grupo desistiu antes de começar? (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`official-scene-withdraw:${event.id}`,async()=>{await JourneyService.withdrawUnexecutedOfficialSceneChoice(campaign.id,event.id,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('Retirada anterior à ação registrada. Nenhuma consequência mecânica foi revertida.');})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Desistir ANTES de executar a ação</button></>}
            {event.status==='open'&&Number(mechanic.encounterId)===36&&(()=>{
              const decision=event.result?.officialSceneDecision as Record<string,unknown>|undefined;
              const stage=String(decision?.stage||'');
              const party=characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead');
              const exposureRound=officialExposureRounds[event.id]??1;
              const strikeRound=officialCrystalStrikeRounds[event.id]??1;
              const exposureChecks=(decision?.crystalExposureChecks as Array<{characterId:string;round:number}>|undefined)||[];
              const strikeChecks=(decision?.crystalStrikeChecks as Array<{characterId:string;round:number}>|undefined)||[];
              const hatchling=decision?.hatchlingHatched===true;
              return <div className="mt-2 rounded border border-violet-800/70 p-3 space-y-2">
                <p className="text-violet-100">Abaken e os três lupinos não têm fichas de combate publicadas. A caverna não tem cura definida. Cada rodada perto dos cristais exige uma exposição real à Virulência 9; quebrar um cristal requer pelo menos 4 de dano.</p>
                {['hairless_lupines_met','hairless_cave_search_pending'].includes(stage)&&<button disabled={!!busy} onClick={()=>void act(`hairless-cave:${event.id}`,async()=>{await JourneyService.openOfficialHairlessLupineCave(campaign.id,event.id);setLastRoll('Exploração da caverna subterrânea registrada; nenhuma cura foi presumida.');})} className="rounded border border-violet-700 px-3 py-2 disabled:opacity-50">Explorar a caverna subterrânea próxima</button>}
                {stage==='hairless_cave_open'&&<>
                  <label className="block text-sm">Rodada próxima aos cristais
                    <input aria-label="Rodada de exposição aos cristais" type="number" min={1} step={1} value={exposureRound} onChange={e=>setOfficialExposureRounds(v=>({...v,[event.id]:Math.max(1,Math.trunc(Number(e.target.value)||1))}))} className="field ml-2 w-24"/>
                  </label>
                  <div className="flex flex-wrap gap-2">{party.map(c=>{
                    const alreadyExposed=exposureChecks.some(check=>check.characterId===c.id&&Number(check.round)===exposureRound);
                    return <button key={`exposure:${c.id}`} disabled={!!busy||alreadyExposed} onClick={()=>void act(`hairless-exposure:${event.id}:${c.id}:${exposureRound}`,async()=>{
                      const result=await JourneyService.resolveOfficialEncounterDiseaseExposure(campaign.id,event.id,c.id,'hairless_crystal',exposureRound);
                      setLastRoll(`${c.name}: Virulência 9 ${result.patientRoll?.successes??'—'}×${result.diseaseRoll?.successes??'—'}; ${result.skippedBecauseAlreadySick?'doença já existente preservada':result.infected?'infecção em incubação':'resistiu'}.`);
                    })} className="rounded border border-violet-700 px-3 py-2 disabled:opacity-50">{alreadyExposed?`${c.name}: exposição já registrada na rodada ${exposureRound}`:`${c.name}: rolar Resiliência, rodada ${exposureRound}`}</button>;
                  })}</div>
                  <label className="block text-sm">Rodada do ataque ao cristal
                    <input aria-label="Rodada de ataque ao cristal" type="number" min={1} step={1} value={strikeRound} onChange={e=>setOfficialCrystalStrikeRounds(v=>({...v,[event.id]:Math.max(1,Math.trunc(Number(e.target.value)||1))}))} className="field ml-2 w-24"/>
                  </label>
                  <div className="flex flex-wrap gap-2">{party.filter(c=>CharacterActionRulesEngine.canAct(c)).flatMap(c=>(c.weapons||[]).map((weapon:any,index:number)=>{
                    const alreadyStruck=strikeChecks.some(check=>check.characterId===c.id&&Number(check.round)===strikeRound);
                    return <button key={`crystal:${c.id}:${index}`} disabled={!!busy||alreadyStruck} onClick={()=>void act(`hairless-crystal:${event.id}:${c.id}:${index}:${strikeRound}`,async()=>{
                      const result=await JourneyService.strikeOfficialHairlessDragonCrystal(campaign.id,event.id,c.id,index,strikeRound);
                      setLastRoll(`${c.name}: Luta ${result.roll.successes} sucesso(s), ${result.damage} dano ao cristal${result.crystalBroken?' — cristal rompido e filhote eclodiu':''}.`);
                    })} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">{alreadyStruck?`${c.name}: já atacou nesta rodada`:`${c.name}: atacar com ${String(weapon?.name||weapon?.label||'arma')} (dano ${String(weapon?.damage??weapon?.weaponDamage??'não registrado')})`}</button>;
                  }))}</div>
                </>}
                {hatchling&&<div className="space-y-2 rounded border border-amber-800/70 p-2">
                  <p>Filhote eclodido: Força 4 e Armadura 3; cada semana completa acrescenta +1 a ambos. O crescimento é persistido por semana decorrida.</p>
                  {!decision?.hatchlingBondCharacterId&&<div className="flex flex-wrap gap-2">{party.filter(c=>CharacterActionRulesEngine.canAct(c)).map(c=><button key={`bond:${c.id}`} disabled={!!busy} onClick={()=>void act(`hatchling-bond:${event.id}:${c.id}`,async()=>{const result=await JourneyService.bondOfficialHairlessDragonHatchling(campaign.id,event.id,c.id);setLastRoll(`${c.name}: Adestramento ${result.roll.successes} sucesso(s); ${result.bonded?'vínculo criado':'sem vínculo nesta tentativa'}.`);})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name}: testar Adestramento para vínculo</button>)}</div>}
                </div>}
                {stage==='hairless_cave_open'||stage==='hairless_hatchling_hatched'?<>
                  {hatchling&&!decision?.hatchlingBondAttempt?<button disabled={!!busy} onClick={()=>void act(`hairless-leave-unbonded:${event.id}`,async()=>{await JourneyService.leaveOfficialHairlessLupineCave(campaign.id,event.id,true);setLastRoll('O grupo deixou a caverna sem tentar criar vínculo com o filhote; cura não presumida.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Deixar a caverna sem tentar vínculo</button>:<button disabled={!!busy} onClick={()=>void act(`hairless-leave:${event.id}`,async()=>{await JourneyService.leaveOfficialHairlessLupineCave(campaign.id,event.id);setLastRoll('O grupo deixou a caverna; doença e efeitos registrados foram preservados, sem cura presumida.');})} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Deixar a caverna</button>}
                </>:null}
              </div>;
            })()}
            {event.status==='open'&&Number(mechanic.encounterId)===37&&
              (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='dwarf_toll_pending'&&
              <div className="mt-2 rounded border border-stone-700 p-3 space-y-2"><p>R88 #37: cada aventureira viva paga 3 pratas de sua própria reserva. O pagamento é atômico; se faltar dinheiro para alguém, nenhuma moeda é retirada.</p>
                <button disabled={!!busy} onClick={()=>void act(`dwarf-toll:${event.id}`,async()=>{const result=await JourneyService.payOfficialRoadBlockadeToll(campaign.id,event.id);const saved=result.journey.events.find(item=>item.id===event.id);const decision=saved?.result?.officialSceneDecision as Record<string,unknown>|undefined;setLastRoll(`Pedágio pago: ${String(decision?.tollSilverTotal??0)} pratas no total.`);})} className="rounded border border-stone-600 px-3 py-2 disabled:opacity-50">Pagar 3 pratas por aventureira</button>
              </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===38&&
              (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='blue_plague_contact_pending'&&(()=>{
                const decision=event.result?.officialSceneDecision as Record<string,unknown>;
                const contacted=(decision.bluePlagueContactCharacterIds as string[]|undefined)||[];
                return <div className="mt-2 rounded border border-blue-800 p-3 space-y-2"><p>Linnaya não consegue falar. Registre somente quem teve contato; cada contato real testa Resiliência contra a Praga Azul, Virulência 5.</p>
                  <div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead'&&!contacted.includes(c.id)).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`blue-plague:${event.id}:${c.id}`,async()=>{const result=await JourneyService.resolveOfficialEncounterDiseaseExposure(campaign.id,event.id,c.id,'blue_plague_contact');setLastRoll(`${c.name}: contato com Virulência 5; ${result.skippedBecauseAlreadySick?'doença preexistente preservada':result.infected?'infecção em incubação':'resistiu'}.`);})} className="rounded border border-blue-700 px-3 py-2 disabled:opacity-50">{c.name}: registrar contato e rolar Resiliência</button>)}</div>
                  <button disabled={!!busy} onClick={()=>void act(`blue-plague-leave:${event.id}`,async()=>{await JourneyService.leaveOfficialBluePlagueScene(campaign.id,event.id);setLastRoll('O grupo deixou Linnaya; exposições e infecções reais permanecem registradas.');})} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Deixar Linnaya e prosseguir</button>
                </div>;
              })()}
            {event.status==='open'&&Number(mechanic.encounterId)===39&&
              (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='merchant_ship_contact'&&<div className="mt-2 rounded border border-cyan-800 p-3 space-y-2">
                <p>A tripulação troca histórias e itens; Sertold pode servir jantar se o grupo passar a noite. A R88 não fixa mercadorias, preços ou efeitos de refeição, então registre a adjudicação e use as operações normais de inventário para qualquer transferência.</p>
                <textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="História, troca ou jantar adjudicado (mínimo 12 caracteres)"/>
                <div className="flex flex-wrap gap-2">{([['stories','Registrar troca de histórias'],['trade','Registrar troca de itens adjudicada'],['dinner','Registrar jantar durante a estadia']] as const).map(([action,label])=><button key={action} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`moonapple:${event.id}:${action}`,async()=>{await JourneyService.resolveOfficialMerchantShipContact(campaign.id,event.id,action,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(`${label}; sem criar itens, preços, refeição ou passagem de tempo automáticos.`);})} className="rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">{label}</button>)}
                  <button disabled={!!busy} onClick={()=>void act(`moonapple-depart:${event.id}`,async()=>{await JourneyService.resolveOfficialMerchantShipContact(campaign.id,event.id,'depart');setLastRoll('O grupo retomou a viagem. Meroggha foi persistido escondido no barco, sem revelar o segredo.');})} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Retomar viagem e embarcar o clandestino</button>
                </div>
              </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===40&&
              (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='combat_active'&&
              worldState.encounter?.startedSceneId===event.id&&worldState.encounter.status==='active'&&(()=>{
                const draft=octopusTentacleDrafts[event.id]||{cut:'',total:'',evidence:''};
                return <div className="mt-2 rounded border border-sky-800 p-3 space-y-2"><p>O polvo usa a ficha canônica. A R88 não informa quantos tentáculos possui: a Mestra registra o total adjudicado e quantos foram efetivamente cortados. O encontro não conclui como vitória por Força reduzida; confirme a fuga quando metade dos tentáculos tiver sido cortada.</p>
                  <div className="flex flex-wrap gap-2"><label>Tentáculos cortados<input aria-label="Tentáculos cortados" type="number" min={1} step={1} value={draft.cut} onChange={e=>setOctopusTentacleDrafts(v=>({...v,[event.id]:{...(v[event.id]||{cut:'',total:'',evidence:''}),cut:e.target.value}}))} className="field ml-2 w-24"/></label>
                    <label>Total adjudicado<input aria-label="Total de tentáculos adjudicado" type="number" min={1} step={1} value={draft.total} onChange={e=>setOctopusTentacleDrafts(v=>({...v,[event.id]:{...(v[event.id]||{cut:'',total:'',evidence:''}),total:e.target.value}}))} className="field ml-2 w-24"/></label></div>
                  <textarea aria-label="Evidência da adjudicação do polvo" value={draft.evidence} onChange={e=>setOctopusTentacleDrafts(v=>({...v,[event.id]:{...(v[event.id]||{cut:'',total:'',evidence:''}),evidence:e.target.value}}))} rows={2} maxLength={500} className="field" placeholder="Adjudicação da MdJ sobre os tentáculos cortados (mínimo 8 caracteres)"/>
                  <button disabled={!!busy||!Number.isInteger(Number(draft.cut))||!Number.isInteger(Number(draft.total))||Number(draft.cut)<1||Number(draft.cut)*2<Number(draft.total)||draft.evidence.trim().length<8} onClick={()=>void act(`octopus-retreat:${event.id}`,async()=>{await JourneyService.recordOfficialGiantOctopusRetreat(campaign.id,event.id,Number(draft.cut),Number(draft.total),draft.evidence);setLastRoll(`Polvo Gigante recuou após ${draft.cut}/${draft.total} tentáculos cortados; combate encerrado pela regra R88.`);})} className="rounded border border-sky-700 px-3 py-2 disabled:opacity-50">Confirmar fuga do polvo à metade dos tentáculos</button>
                </div>;
              })()}
            {Number(mechanic.encounterId)===20&&event.status==='open'&&(()=>{
              const decision=event.result?.officialSceneDecision as Record<string,unknown>|undefined;
              const stage=String(decision?.stage||'');
              return stage==='procession_warning'||stage==='procession_undisturbed'?<div className="mt-2 rounded border border-amber-800/70 p-3 space-y-2">
                {stage==='procession_warning'?<><p className="text-amber-100">Os elfos ocultos se revelaram e gritaram: “Vão embora, sacos de carne maldita!”. Se a luta começar, os cinco Ents vivos pousam o corpo e combatem.</p>
                  <button disabled={!!busy} onClick={()=>void act(`funeral-20:${event.id}:leave`,async()=>{await JourneyService.advanceOfficialFuneralProcession(campaign.id,event.id,'leave');setLastRoll('O grupo deixou a procissão após a advertência.');})} className="mr-2 rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Deixar a procissão em paz</button>
                  <button disabled={!!busy} onClick={()=>void act(`funeral-20:${event.id}:attack`,async()=>{await JourneyService.advanceOfficialFuneralProcession(campaign.id,event.id,'attack');setLastRoll('A advertência foi ignorada; o corpo foi pousado e o combate oficial dos cinco Ents está pronto.');})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Atacar os Ents</button></>
                  :<><p className="text-amber-100">O grupo não interferiu. A Mestra pode encerrar a cena ou registrar um presente opcional de fruta, noz ou algo mais útil. O registro do presente não cria nem transfere um item sem adjudicação de inventário.</p>
                    <button disabled={!!busy} onClick={()=>void act(`funeral-20:${event.id}:leave`,async()=>{await JourneyService.advanceOfficialFuneralProcession(campaign.id,event.id,'leave');setLastRoll('O grupo prosseguiu sem receber presente.');})} className="mr-2 rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Prosseguir sem presente</button>
                    <textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Presente adjudicado pela Mestra (12–600 caracteres)"/>
                    <button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`funeral-20:${event.id}:gift`,async()=>{await JourneyService.advanceOfficialFuneralProcession(campaign.id,event.id,'record_gift',encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('Presente registrado conforme a adjudicação da Mestra; nenhum item foi transferido automaticamente.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Registrar presente adjudicado</button></>}
              </div>:null;
            })()}
            {Number(mechanic.encounterId)===19&&event.status==='open'&&(()=>{
              const decision=event.result?.officialSceneDecision as Record<string,unknown>|undefined;
              const stage=String(decision?.stage||'');
              const actions:Array<[ 'approach'|'attack_animals'|'wait_for_leader'|'attack_leader'|'leave_sanctuary',string ]>=
                stage==='pending_external_mechanics'&&decision?.choiceId==='linger'?[['approach','Permitir que mais animais se aproximem'],['leave_sanctuary','Desmontar e deixar o santuário']]:
                stage==='pending_external_mechanics'&&decision?.choiceId==='attack'?[['attack_animals','Registrar que os animais recuaram sem dano'],['leave_sanctuary','Desmontar e deixar o santuário']]:
                stage==='sanctuary_animals_approaching'?[['attack_animals','Atacar os animais que empurram'],['wait_for_leader','Esperar a chegada da Alce Negra'],['leave_sanctuary','Deixar o santuário']]:
                stage==='sanctuary_animals_withdrew'?[['wait_for_leader','Esperar a chegada da Alce Negra'],['leave_sanctuary','Deixar o santuário']]:
                stage==='sanctuary_black_elk_arrived'?[['attack_leader','Atacar a Alce Negra'],['leave_sanctuary','Deixar o santuário']]:[];
              return actions.length||stage==='sanctuary_leader_attacked_pending_combat'?<div className="mt-2 rounded border border-emerald-800/70 p-3 space-y-2">
                <p>O santuário avança sem causar dano ou mover o grupo automaticamente. Depois do ataque à Alce Negra, a quantidade e as fichas animais exigem adjudicação pela tabela oficial p. 126.</p>
                {actions.map(([action,label])=><button key={action} disabled={!!busy} onClick={()=>void act(`sanctuary-19:${event.id}:${action}`,async()=>{await JourneyService.advanceOfficialAnimalSanctuary(campaign.id,event.id,action);setLastRoll(label);})} className="mr-2 rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">{label}</button>)}
                {stage==='sanctuary_leader_attacked_pending_combat'&&<p role="alert" className="text-amber-200">A regra de combate exige ficha e quantidade de cada animal da p. 126. A cena permanece aberta até implementar essa composição sem presumir combatentes.</p>}
              </div>:null;
            })()}
            {Number(mechanic.encounterId)===1&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <button disabled={!!busy} onClick={()=>void act(`blood-mist-start:${event.id}`,async()=>{
               const o=await JourneyService.startOfficialBloodMist(campaign.id,event.id);
               setLastRoll(o.watchRoll?`Vigia: ${o.watchRoll.successes} sucesso(s). ${o.canEscapeBeforeFog?'O grupo pode tentar fugir antes de ser cercado.':'O grupo foi cercado.'}`:'Sem vigia capaz de alertar; o grupo já está cercado.');
             })} className="rounded border border-rose-700 px-3 py-2 disabled:opacity-50">Iniciar Névoa de Sangue · testar vigia</button>}
            {Number(mechanic.encounterId)===1&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='blood_mist_active'&&
             <div className="space-y-2 rounded border border-rose-800/70 p-2">
               <p className="text-rose-200">Névoa de Sangue · rodada {Number((event.result?.officialSceneDecision as Record<string,unknown>).mistRound||0)===0?'prévia':String((event.result?.officialSceneDecision as Record<string,unknown>).mistRound)}. Cada aventureiro preso tenta Movimentação; na névoa também testa Discernimento (falha: -1 Empatia). Não se pode repetir a ação na mesma rodada.</p>
               {((event.result?.officialSceneDecision as Record<string,unknown>).mistActorIds as string[]||[]).map(id=>{
                 const actor=characters.find(c=>c.id===id);
                 const decision=event.result?.officialSceneDecision as Record<string,unknown>;
                 const escaped=(decision.mistEscapedIds as string[]||[]).includes(id);
                 const acted=(decision.mistActedIds as string[]||[]).includes(id);
                 if(!actor)return <p key={id} className="text-red-300">Personagem {id} não encontrada; preservar ocorrência aberta até recuperar o grupo.</p>;
                 return <div key={id} className="flex flex-wrap items-center gap-2"><span>{actor.name}: {escaped?'fora da névoa':acted?'já agiu nesta rodada':actor.brokenAttribute==='empathy'?'Quebrada em Empatia':'presa'}</span>
                   {!escaped&&!acted&&CharacterActionRulesEngine.canAct(actor)&&<button disabled={!!busy} onClick={()=>void act(`blood-mist-escape:${event.id}:${id}`,async()=>{
                     const o=await JourneyService.attemptOfficialBloodMistEscape(campaign.id,event.id,id);
                     setLastRoll(`${actor.name}: Movimentação ${o.movement.successes}; ${o.insight?`Discernimento ${o.insight.successes}; `:''}${o.empathyDamage?'-1 Empatia; ':''}${o.escaped?'escapou':'continua presa'}.`);
                   })} className="border border-rose-700 rounded px-3 py-2 disabled:opacity-50">Rolar fuga e Discernimento</button>}
                   {!escaped&&actor.brokenAttribute==='empathy'&&((decision.mistEscapedIds as string[])||[]).map(helperId=>{
                     const helper=characters.find(c=>c.id===helperId);
                     return helper&&CharacterActionRulesEngine.canAct(helper)?<button key={helperId} disabled={!!busy} onClick={()=>void act(`blood-mist-drag:${event.id}:${id}:${helperId}`,async()=>{
                       await JourneyService.dragOfficialBloodMistVictim(campaign.id,event.id,id,helperId);
                       setLastRoll(`${helper.name} arrastou ${actor.name} para fora; o dano de Empatia continua.`);
                     })} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">{helper.name} resgata</button>:null;
                   })}
                 </div>;
               })}
             </div>}
            {Number(mechanic.encounterId)===1&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='blood_mist_active'&&
             !(event.result?.officialSceneDecision as Record<string,unknown>)?.mistDemonEncounterId&&
             Number((event.result?.officialSceneDecision as Record<string,unknown>)?.mistRound)>=1&&
             <button disabled={!!busy} onClick={()=>void act(`blood-mist-manifest:${event.id}`,async()=>{
               await JourneyService.manifestOfficialBloodMistDemon(campaign.id,event.id);
               setLastRoll('A ameaça se materializou. Resolva o combate vinculado na tela de combate.');
             })} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Avançar ameaça da Névoa de Sangue (decisão da Mestra)</button>}
            {Number(mechanic.encounterId)===1&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='blood_mist_combat_active'&&
             <div className="rounded border border-red-800 p-2 space-y-2"><p>Uma criatura se materializou. O combate está vinculado à jornada; nenhum dano ou vitória é presumido.</p>
              {worldState.encounter?.status==='resolved'&&<button disabled={!!busy} onClick={()=>void act(`blood-mist-demon-finish:${event.id}`,async()=>{
                await JourneyService.finishOfficialBloodMistDemon(campaign.id,event.id);
                setLastRoll('Desfecho registrado. A névoa continua até a fuga das aventureiras restantes.');
              })} className="border border-emerald-700 rounded px-3 py-2 disabled:opacity-50">Confirmar desfecho do combate</button>}
             </div>}
            {Number(mechanic.encounterId)===5&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='bribe'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-amber-800 p-2 space-y-2"><p className="text-amber-100">Escolha um objeto real do inventário para chamar atenção das harpias. Ele não é consumido e a regra não garante êxito automático; a disputa será resolvida no combate.</p>
             {(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.harpyBaitItem&&<p className="text-amber-200">Objeto preparado: {String(((event.result?.officialSceneDecision as Record<string,unknown>).harpyBaitItem as Record<string,unknown>).itemName)}. Se mudou de posse, escolha outro abaixo.</p>}
             {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).flatMap(c=>(['gear','weapons','armor'] as const).flatMap(collection=>(c[collection]||[]).map((item:any,index:number)=><button key={`${c.id}:${collection}:${index}`} disabled={!!busy} onClick={()=>void act(`harpy-bait:${event.id}:${c.id}:${collection}:${index}`,async()=>{
                await JourneyService.prepareOfficialHarpyDistraction(campaign.id,event.id,c.id,collection,index);
                setLastRoll(`${c.name} preparou ${String(item?.name||item?.label)} sem consumi-lo. Nenhum bônus automático concedido.`);
              })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name}: {String(item?.name||item?.label||'item')}</button>)))}
             </div>}
            {/* Atalhos R105 preservados para o encontro #29. O Codex também permite distribuição individual abaixo. */}
            {Number(mechanic.encounterId)===29&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-red-800 p-2 space-y-2">
               <p>Esqueletos: iniciar todos com couro batido ou todos com cota de malha; para composição mista, escolha a quantidade de cotas abaixo.</p>
               {(['leather','mail'] as const).map(armor=><button key={armor} disabled={!!busy}
                 onClick={()=>void act(`skeleton-quick:${event.id}:${armor}`,async()=>{
                   const world=await JourneyService.startOfficialSceneCombat(campaign.id,event.id,undefined,armor);
                   setLastRoll(`Emboscada iniciada: ${world.encounter?.combatants.filter(c=>c.entityId?.startsWith('official-skeleton:')).length} esqueletos; armadura ${armor==='mail'?'cota de malha':'couro batido'}.`);
                 })} className="mr-2 rounded border border-red-700 px-3 py-2 disabled:opacity-50">{armor==='mail'?'Todos com cota':'Todos com couro'}</button>)}
             </div>}
            {(((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics')||
             (Number(mechanic.encounterId)===18&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='dough_captives_returned'))&&
             ((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.pending==='combat'||
              (Number(mechanic.encounterId)===7&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='help')||
              (Number(mechanic.encounterId)===5&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='flee'))&&
             [4,5,7,8,11,12,18,20,21,23,24,25,29,30,33,35,37,40,41,42,43].includes(Number(mechanic.encounterId))&&
             <>{Number(mechanic.encounterId)===29&&<><p className="rounded border border-amber-700 p-2 text-amber-100">R88 p.159: D6+3 esqueletos atacam. Use a ficha típica da p.106 (Força 3, Agilidade 2, Luta 3, Patrulha 3, espada larga enferrujada, elmo fechado). A fonte permite couro batido ou cota de malha, sem definir a distribuição nem os ratings numéricos; escolha quantos dos {Number((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.skeletonCount||0)} usam cota. Os demais usam couro.</p><label className="block text-xs text-amber-200">Esqueletos com cota de malha<input type="number" min={0} max={Number((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.skeletonCount||9)} step={1} value={skeletonChainmailDrafts[event.id]??0} onChange={e=>setSkeletonChainmailDrafts(v=>({...v,[event.id]:Number(e.target.value)}))} className="field ml-2 w-24"/></label></>}{Number(mechanic.encounterId)===30&&<p className="rounded border border-amber-700 p-2 text-amber-100">Teramalda é imune a todo dano. Se for atacada no primeiro encontro, a R88 manda ir direto às regras do terceiro; fuga nessa etapa encerra a cena sem perseguição. A partir do quarto encontro, ela persegue enquanto o grupo carregar ferro. Para matá-la: imobilização confirmada e Potência Formidável (−3), ação lenta, para retirar o espigão.</p>}{[23,35].includes(Number(mechanic.encounterId))&&<label className="block text-xs text-amber-200">Força escolhida para {Number(mechanic.encounterId)===35?'Karg':'Ghormald'} (R88 pp.114–115: 12–16)<select value={trollStrengthDrafts[event.id]??12} onChange={e=>setTrollStrengthDrafts(v=>({...v,[event.id]:Number(e.target.value)}))} className="field ml-2">{[12,13,14,15,16].map(value=><option key={value} value={value}>{value}</option>)}</select></label>}
             {Number(mechanic.encounterId)===41&&(()=>{const d=merchantLoadoutDrafts[event.id]||{ginda:{melee:'',ranged:'',armor:''},aneta:{melee:'',ranged:'',armor:''}};return <div className="space-y-2 rounded border border-amber-800 p-2"><p>Ginda e Aneta usam o Soldado típico R88 p.183. Selecione equipamento dentro das alternativas impressas para cada irmã.</p>{(['ginda','aneta'] as const).map(key=><div key={key} className="grid gap-2 md:grid-cols-4"><b>{key==='ginda'?'Ginda':'Aneta'}</b><select aria-label={`${key} arma corpo a corpo`} value={d[key].melee} onChange={e=>setMerchantLoadoutDrafts(v=>({...v,[event.id]:{...d,[key]:{...d[key],melee:e.target.value}}}))} className="field"><option value="">Arma corpo a corpo</option><option value="broad_sword">Espada larga</option><option value="battle_axe">Machado de batalha</option></select><select aria-label={`${key} arma secundária`} value={d[key].ranged} onChange={e=>setMerchantLoadoutDrafts(v=>({...v,[event.id]:{...d,[key]:{...d[key],ranged:e.target.value}}}))} className="field"><option value="">Segunda arma</option><option value="long_spear">Lança longa</option><option value="long_bow">Arco longo</option><option value="heavy_crossbow">Besta pesada</option></select><select aria-label={`${key} armadura`} value={d[key].armor} onChange={e=>setMerchantLoadoutDrafts(v=>({...v,[event.id]:{...d,[key]:{...d[key],armor:e.target.value}}}))} className="field"><option value="">Armadura</option><option value="leather">Couro</option><option value="chainmail">Cota de malha</option></select></div>)}</div>;})()}
             {Number(mechanic.encounterId)===42&&<div className="space-y-2 rounded border border-amber-800 p-2"><p>R88 p.166: quantidade = aventureiras vivas + D6. A rolagem acontece ao iniciar. Escolha o armamento publicado.</p><select aria-label="Arma dos caçadores sáurios" value={saurianSpearDrafts[event.id]||''} onChange={e=>setSaurianSpearDrafts(v=>({...v,[event.id]:e.target.value as 'trident'|'long_spear'|''}))} className="field"><option value="">Escolha tridente ou lança longa</option><option value="trident">Tridente</option><option value="long_spear">Lança longa</option></select></div>}
             <button disabled={!!busy||(Number(mechanic.encounterId)===5&&(event.result?.officialSceneDecision as Record<string,unknown>)?.choiceId==='bribe'&&!(event.result?.officialSceneDecision as Record<string,unknown>)?.harpyBaitItem)||(Number(mechanic.encounterId)===41&&['ginda','aneta'].some(key=>!merchantLoadoutDrafts[event.id]?.[key as 'ginda'|'aneta']?.melee||!merchantLoadoutDrafts[event.id]?.[key as 'ginda'|'aneta']?.ranged||!merchantLoadoutDrafts[event.id]?.[key as 'ginda'|'aneta']?.armor))||(Number(mechanic.encounterId)===42&&!saurianSpearDrafts[event.id])} onClick={()=>void act(`official-combat-start:${event.id}`,async()=>{
               const sceneId=Number(mechanic.encounterId);
               const merchantSisterLoadouts=merchantLoadoutDrafts[event.id] as {ginda:{melee:'broad_sword'|'battle_axe';ranged:'long_spear'|'long_bow'|'heavy_crossbow';armor:'leather'|'chainmail'};aneta:{melee:'broad_sword'|'battle_axe';ranged:'long_spear'|'long_bow'|'heavy_crossbow';armor:'leather'|'chainmail'}}|undefined;
               const sceneOptions=sceneId===33?{minotaurSide:String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId)==='help_markut'?'markut' as const:'soldiers' as const}:sceneId===41?{merchantSisterLoadouts}:sceneId===42?{saurianSpear:saurianSpearDrafts[event.id]||undefined}:undefined;
               const started=await JourneyService.startOfficialSceneCombat(campaign.id,event.id,undefined,[23,35].includes(sceneId)?(trollStrengthDrafts[event.id]??12):undefined,undefined,sceneId===29?(skeletonChainmailDrafts[event.id]??0):undefined,sceneOptions);
               const decision=(started.journey.events.find(item=>item.id===event.id)?.result?.officialSceneDecision||{}) as Record<string,unknown>;
               setLastRoll(sceneId===7?'Harwa soou a trombeta. Os escravagistas revelaram a emboscada; combate iniciado.':sceneId===37?`A trombeta soou. D6 = ${Number(started.encounter?.officialEncounter?.reinforcementsAtRound||1)-1}; os três reforços chegam no início da rodada ${String(started.encounter?.officialEncounter?.reinforcementsAtRound||'—')}.`:sceneId===40?'Combate oficial do Polvo Gigante iniciado; registre a fuga da R88 após metade dos tentáculos ser cortada.':sceneId===42?`D6 = ${String(decision.saurianCountD6)}; ${String(decision.saurianTotalCount)} caçadores sáurios no combate.`:'Combate oficial iniciado, ligado à ocorrência de jornada. Resolva os turnos na tela de combate.');
             })} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Iniciar combate oficial</button></>}
            {event.status==='open'&&Number(mechanic.encounterId)===41&&(()=>{const d=(event.result?.officialSceneDecision as Record<string,unknown>|undefined)||{};const stock=Number(d.soulBottleStock);return d.stage==='soul_merchant_contact'?<div className="mt-2 rounded border border-cyan-800 p-3 space-y-2"><p>Vergevil cobra 10 pratas por garrafa e aceita itens VALIOSOS. A R88 não determina o estoque nem equivalência de troca. Defina o estoque da cena; a troca exige marcar item realmente valioso e não recebe preço inventado.</p><label>Estoque adjudicado<input type="number" min={0} max={999} step={1} value={soulMerchantStockDrafts[event.id]??0} onChange={e=>setSoulMerchantStockDrafts(v=>({...v,[event.id]:Math.max(0,Math.trunc(Number(e.target.value)||0))}))} className="field ml-2 w-24"/></label><button disabled={!!busy} onClick={()=>void act(`soul-stock:${event.id}`,async()=>{await JourneyService.setOfficialSoulMerchantStock(campaign.id,event.id,soulMerchantStockDrafts[event.id]??0);setLastRoll('Estoque escolhido pela Mestra e salvo; sem quantidade presumida pela R88.');})} className="rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">Registrar estoque da cena</button></div>:d.stage==='soul_merchant_stock_set'?<div className="mt-2 rounded border border-cyan-800 p-3 space-y-2"><p>Estoque restante: {stock}. Cada garrafa custa 10 pratas. Abrir uma garrafa consome o item e deixa uma alma livre; Ressurreição não é simulada por esta tela.</p>{stock>0&&<div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={`soul-buy:${c.id}`} data-testid={`fl-soul-buy-${event.id}-${c.id}`} disabled={!!busy} onClick={()=>void act(`soul-buy:${event.id}:${c.id}`,async()=>{await JourneyService.buyOfficialSoulBottle(campaign.id,event.id,c.id);setLastRoll(`${c.name} pagou 10 pratas e recebeu uma garrafa de alma.`);})} className="rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">{c.name}: comprar garrafa · 10 pratas</button>)}</div>}{stock>0&&<div className="space-y-1"><p>Troca por item VALIOSO/PRECIOSO (equivalência confirmada pela Mestra):</p>{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).flatMap(c=>(['gear','weapons','armor'] as const).flatMap(collection=>(c[collection]||[]).flatMap((item:any,index:number)=>['valuable','precious'].includes(String(item?.treasureCategory||item?.valueCategory||''))?[<button key={`soul-barter:${c.id}:${collection}:${index}`} data-testid={`fl-soul-barter-${event.id}-${c.id}-${collection}-${index}`} disabled={!!busy} onClick={()=>void act(`soul-barter:${event.id}:${c.id}:${collection}:${index}`,async()=>{await JourneyService.barterOfficialSoulBottle(campaign.id,event.id,c.id,collection,index,InventorySelectionSnapshot.capture(item));setLastRoll(`${c.name} trocou ${String(item?.name||item?.label||'item valioso')} por uma garrafa; a equivalência ficou adjudicada pela Mestra.`);})} className="mr-2 rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name}: trocar {String(item?.name||item?.label||'item valioso')}</button>]:[])))}</div>}<div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).flatMap(c=>(c.gear||[]).flatMap((item:any,index:number)=>item?.itemKind==='official_soul_bottle'&&item?.soulBottleState==='sealed'&&item?.sourceEventId===event.id?[<button key={`open-soul:${c.id}:${index}`} data-testid={`fl-soul-open-${event.id}-${c.id}-${index}`} disabled={!!busy} onClick={()=>void act(`open-soul:${event.id}:${c.id}:${index}`,async()=>{const o=await JourneyService.openOfficialSoulBottle(campaign.id,event.id,c.id,index,InventorySelectionSnapshot.capture(item));setLastRoll(`A garrafa foi aberta; alma ${o.soulTokenId} livre, Ressurreição pendente.`);})} className="rounded border border-violet-700 px-3 py-2 disabled:opacity-50">{c.name}: abrir garrafa</button>]:[]))}<button disabled={!!busy} onClick={()=>void act(`leave-soul-merchant:${event.id}`,async()=>{await JourneyService.leaveOfficialSoulMerchant(campaign.id,event.id);setLastRoll('A companhia deixou o veleiro.');})} className="rounded border border-border px-3 py-2 disabled:opacity-50">Deixar o veleiro</button></div></div>:null;})()}
            {event.status==='open'&&Number(mechanic.encounterId)===42&&(()=>{const d=(event.result?.officialSceneDecision as Record<string,unknown>|undefined)||{};return d.stage==='saurian_parley_pending'?<div className="mt-2 rounded border border-emerald-800 p-3 space-y-2"><p>Sheeerz procura porcos grandes e odeia humanos, mas não é especialmente hostil às outras raças. A R88 não obriga ataque durante a conversa. Registre o resultado adjudicado pela Mestra.</p><textarea value={saurianParleyEvidenceDrafts[event.id]||''} onChange={e=>setSaurianParleyEvidenceDrafts(v=>({...v,[event.id]:e.target.value}))} rows={2} maxLength={600} className="field" placeholder="Resultado adjudicado (mínimo 8 caracteres)"/><div className="flex gap-2"><button disabled={!!busy||(saurianParleyEvidenceDrafts[event.id]||'').trim().length<8} onClick={()=>void act(`saurian-parley:${event.id}:peace`,async()=>{await JourneyService.resolveOfficialSaurianParley(campaign.id,event.id,'peaceful',saurianParleyEvidenceDrafts[event.id]||'');setLastRoll('Conversa encerrada conforme adjudicação; sem hostilidade automática.');})} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Encerrar conversa</button><button disabled={!!busy||(saurianParleyEvidenceDrafts[event.id]||'').trim().length<8} onClick={()=>void act(`saurian-parley:${event.id}:attack`,async()=>{await JourneyService.resolveOfficialSaurianParley(campaign.id,event.id,'attack',saurianParleyEvidenceDrafts[event.id]||'');setLastRoll('Escalada para combate registrada; a quantidade será rolada ao iniciar.');})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Adjudicar hostilidade e preparar combate</button></div></div>:null;})()}
            {event.status==='open'&&Number(mechanic.encounterId)===43&&(()=>{const d=(event.result?.officialSceneDecision as Record<string,unknown>|undefined)||{};return d.stage==='burial_rescue_pending'?<div className="mt-2 rounded border border-red-800 p-3 space-y-2"><p>A família Ylendidra (quatro pessoas) está no fosso. A R88 não define prazo, teste nem profundidade. Registre a decisão da Mestra sem inventar rolagem; após vitória sobre os caçadores, a decisão permanece pendente até aqui.</p><textarea value={burialRescueEvidenceDrafts[event.id]||''} onChange={e=>setBurialRescueEvidenceDrafts(v=>({...v,[event.id]:e.target.value}))} rows={2} maxLength={600} className="field" placeholder="Como ocorreu o resgate/desfecho (mínimo 8 caracteres)"/><div className="flex flex-wrap gap-2">{([['rescued','Registrar família resgatada'],['executed','Registrar execução adjudicada'],['withdrawn','Registrar retirada sem resgate']] as const).map(([outcome,label])=><button key={outcome} disabled={!!busy||(burialRescueEvidenceDrafts[event.id]||'').trim().length<8} onClick={()=>void act(`burial-rescue:${event.id}:${outcome}`,async()=>{await JourneyService.resolveOfficialBurialRescue(campaign.id,event.id,outcome,burialRescueEvidenceDrafts[event.id]||'');setLastRoll(`${label}; destino das quatro pessoas salvo sem rolagem inventada.`);})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">{label}</button>)}</div></div>:null;})()}
            {Number(mechanic.encounterId)===3&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='rescue'&&
             <div className="rounded border border-amber-800 p-3 space-y-2"><p>O livro não define o clã da escolta. A Mestra escolhe uma ficha típica oficial; a escolta terá 2× o número de aventureiros.</p>
              {(['Roka','Urhur','Isir'] as const).map(clan=><button key={clan} disabled={!!busy} onClick={()=>void act(`official-orc-start:${event.id}:${clan}`,async()=>{
                await JourneyService.startOfficialSceneCombat(campaign.id,event.id,clan);
                setLastRoll(`Escolta ${clan} em combate oficial, Sargah continua amarrado.`);
              })} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Escolta {clan} · iniciar combate</button>)}
             </div>}
            {Number(mechanic.encounterId)===18&&Number(event.result?.officialEncounterVisitNumber||1)>=3&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='wait_for_melkar'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-amber-800 p-2 space-y-2"><p>A R88 diz que Melkar retorna após algumas horas, sem especificar duração exata. Confirme a passagem desse intervalo narrativo; o relógio da campanha não receberá horas inventadas.</p><button disabled={!!busy} onClick={()=>void act(`dough-return:${event.id}`,async()=>{await JourneyService.resolveOfficialDoughBakerReturn(campaign.id,event.id);setLastRoll('Após algumas horas, Melkar retornou com halflings acorrentados e 200 massas. Combate disponível.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Confirmar retorno de Melkar</button></div>}
            {Number(mechanic.encounterId)===18&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='bake_dough'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-amber-800 p-2 space-y-2"><p>A p. 155 diz que o demônio nas pedras tentará corromper quem assar massa, mas não define teste, alvo ou efeito. Registre a adjudicação real da Mestra; nenhuma corrupção será aplicada automaticamente.</p><textarea value={doughCorruptionDrafts[event.id]||''} onChange={e=>setDoughCorruptionDrafts(cur=>({...cur,[event.id]:e.target.value}))} rows={2} maxLength={800} className="field" placeholder="Adjudicação da Mestra (12–800 caracteres)"/><button disabled={!!busy||(doughCorruptionDrafts[event.id]||'').trim().length<12} onClick={()=>void act(`dough-corruption:${event.id}`,async()=>{await JourneyService.adjudicateOfficialDoughCorruption(campaign.id,event.id,doughCorruptionDrafts[event.id]||'');setDoughCorruptionDrafts(cur=>({...cur,[event.id]:''}));setLastRoll('Tentativa de assar massa e adjudicação da Mestra registradas sem efeitos automáticos.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Registrar adjudicação da corrupção</button></div>}
            {Number(mechanic.encounterId)===18&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='dough_captives_pending'&&
             <div className="rounded border border-amber-800 p-2 space-y-2"><p>Após derrotar as 200 massas, os halflings acorrentados que Melkar trouxe de Pernty aguardam uma decisão. A fonte não determina teste, quantidade de habitantes ou recompensa.</p><div className="flex flex-wrap gap-2"><button disabled={!!busy} onClick={()=>void act(`dough-captives-release:${event.id}`,async()=>{await JourneyService.resolveOfficialDoughCaptives(campaign.id,event.id,'release');setLastRoll('Halflings acorrentados trazidos de Pernty foram libertados.');})} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Libertar os halflings</button><button disabled={!!busy} onClick={()=>void act(`dough-captives-leave:${event.id}`,async()=>{await JourneyService.resolveOfficialDoughCaptives(campaign.id,event.id,'leave');setLastRoll('O grupo deixou os halflings acorrentados no local.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Deixá-los no local</button></div></div>}
            {Number(mechanic.encounterId)===18&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='dough_loot_pending'&&
             <div className="rounded border border-amber-800 p-2 space-y-2"><p>A massa foi derrotada. Se o grupo encontrou e abriu o baú, uma personagem apta pode recolher o diamante de valor oficial 1 ouro.</p>
              {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`dough-diamond:${event.id}:${c.id}`,async()=>{await JourneyService.claimOfficialDemonicDoughDiamond(campaign.id,event.id,c.id);setLastRoll(`${c.name} recolheu o diamante de 1 ouro do baú de Melkar.`);})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Recolher diamante para {c.name}</button>)}
             </div>}
            {(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='in_combat'&&
             <div className="space-y-2"><p className="text-amber-100">Combate manual vinculado: {String((event.result?.officialSceneDecision as Record<string,unknown>).combatEncounterId||'—')}.</p>
             <button disabled={!!busy||worldState.encounter?.status!=='resolved'} onClick={()=>void act(`official-manual-combat-finish:${event.id}`,async()=>{await JourneyService.finishOfficialSceneAfterCombat(campaign.id,event.id);setLastRoll('Combate original validado, com suas consequências preservadas.');})} className="rounded border border-emerald-700 p-2 text-xs disabled:opacity-50">Confirmar resultado do combate e liberar jornada</button></div>}
            {Number(mechanic.encounterId)===5&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='bribe'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='combat_active'&&
             !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.harpyBaitThrownAt&&
             worldState.encounter?.status==='active'&&
             <button disabled={!!busy} onClick={()=>void act(`harpy-throw:${event.id}`,async()=>{
               await JourneyService.throwOfficialHarpyBait(campaign.id,event.id);
               setLastRoll('Objeto arremessado e removido do inventário. A reação das harpias não é automática.');
             })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Arremessar objeto preparado (turno do portador)</button>}
            {Number(mechanic.encounterId)===5&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='combat_active'&&
             !!(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.harpyBaitThrownAt&&
             !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.harpyBaitRecoveredAt&&
             worldState.encounter?.status==='active'&&
             (['Krag','Mag','Serag'].includes(worldState.encounter.combatants[worldState.encounter.turnIndex]?.name||''))&&
             <div className="rounded border border-amber-800 p-2 space-y-2"><p>Turno da harpia: a Mestra escolhe a reação à isca. Apanhar exige confirmação manual de alcance; nenhuma fuga é concedida automaticamente.</p>
              {(['ignore','investigate','pick_up'] as const).map(reaction=><button key={reaction} disabled={!!busy} onClick={()=>void act(`harpy-react:${event.id}:${reaction}`,async()=>{
                await JourneyService.reactOfficialHarpyToBait(campaign.id,event.id,worldState.encounter!.combatants[worldState.encounter!.turnIndex].id,reaction,reaction==='pick_up');
                setLastRoll(reaction==='pick_up'?'Harpia recolheu a isca real, que saiu do chão.':reaction==='investigate'?'Harpia gastou ação investigando a isca.':'Harpia ignorou a isca nesta rodada.');
              })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{reaction==='pick_up'?'Apanhar (alcance confirmado)':reaction==='investigate'?'Investigar (gastar ação)':'Ignorar'}</button>)}
             </div>}
            {Number(mechanic.encounterId)===3&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='sargah_release_pending'&&
             <div className="rounded border border-amber-800 p-3 space-y-2"><p>Escolta Roka derrotada. Sargah continua amarrado até uma decisão explícita.</p>
              {(['release','leave_bound'] as const).map(choice=><button key={choice} disabled={!!busy} onClick={()=>void act(`sargah:${event.id}:${choice}`,async()=>{
                await JourneyService.resolveOfficialSargah(campaign.id,event.id,choice);
                setLastRoll(choice==='release'?'Sargah foi libertado, sem recrutamento automático.':'Sargah permaneceu amarrado.');
              })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{choice==='release'?'Libertar Sargah':'Deixar Sargah amarrado'}</button>)}
             </div>}
            {Number(mechanic.encounterId)===7&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='slaver_captives_pending'&&
             <p className="rounded border border-red-700 p-3 text-red-200">A captura foi registrada nas fichas dos aventureiros dominados pelos escravagistas sobreviventes. As personagens cativas não podem agir livremente. O resgate, a fuga e o destino dos prisioneiros continuam pendentes; não use saída genérica.</p>}
            {Number(mechanic.encounterId)===7&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='slaver_captives_pending'&&
             <div className="rounded border border-red-700 p-3 space-y-2"><p className="text-red-200">Resgate real: só aventureiros livres e aptos, presentes neste hex, podem enfrentar os escravagistas que sobreviveram. Se todos estão cativos, será necessária ajuda externa; nenhuma fuga é simulada.</p>
              <button disabled={!!busy||!characters.some(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)&&!(((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.slaverCapturedCharacterIds as string[]|undefined)||[]).includes(c.id))} onClick={()=>void act(`slaver-rescue:${event.id}`,async()=>{
                await JourneyService.startOfficialSlaverRescue(campaign.id,event.id);
                setLastRoll('Combate de resgate iniciado com os escravagistas sobreviventes.');
              })} className="rounded border border-red-700 p-2 text-xs disabled:opacity-50">Iniciar combate de resgate</button>
             </div>}
            {Number(mechanic.encounterId)===7&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='slaver_rescue_combat_active'&&
             <div className="rounded border border-red-700 p-3 space-y-2"><p>Resolva o combate real dos resgatadores. Os cativos continuam sem poder agir.</p>
              <button disabled={!!busy||worldState.encounter?.status!=='resolved'} onClick={()=>void act(`slaver-rescue-finish:${event.id}`,async()=>{
                await JourneyService.completeOfficialSlaverRescue(campaign.id,event.id);
                setLastRoll('Resgate conciliado: efeitos removidos somente se os escravagistas foram vencidos.');
              })} className="rounded border border-red-700 p-2 text-xs disabled:opacity-50">Conciliar combate do resgate</button>
             </div>}
            {Number(mechanic.encounterId)===11&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='tomb_loot_pending'&&
             <div className="rounded border border-amber-700 p-3 space-y-2"><p>O Cavaleiro da Morte foi derrotado. Os objetos continuam na sepultura até serem recolhidos fisicamente.</p>
              {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`prince-loot:${event.id}:${c.id}`,async()=>{
                const outcome=await JourneyService.claimOfficialForgottenPrinceTreasure(campaign.id,event.id,c.id);
                setLastRoll(`${c.name} recolheu o colar (valor ${outcome.silverValue} pratas), a espada e a cota de malha.`);
              })} className="rounded border border-amber-700 p-2 text-xs disabled:opacity-50">{c.name}: recolher os três objetos</button>)}
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===26&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='escape_fire'&&
             <div className="rounded border border-orange-800 p-3 space-y-2"><p>R88 p.158: cada personagem precisa escapar. A primeira falha sofre ataque de fogo com 6 Dados Base e Dano 1; cada falha seguinte acrescenta 1 Dado Base. A sequência termina quando cada pessoa escapa ou fica Acabada.</p>
              {characters.filter(c=>CampaignPartyService.belongs(campaign,c)).map(c=>{const decision=event.result?.officialSceneDecision as Record<string,unknown>;const progress=(decision.fireEscapeProgress as Record<string,Record<string,unknown>>|undefined)?.[c.id];const state=String(progress?.status||'pending');const canAct=CharacterActionRulesEngine.canAct(c);return <button key={c.id} disabled={!!busy||worldState.encounter?.status==='active'||!canAct||['escaped','broken','dead'].includes(state)} onClick={()=>void act(`wildfire:${event.id}:${c.id}`,async()=>{const result=await JourneyService.resolveOfficialWildfireEscapeAttempt(campaign.id,event.id,c.id);setLastRoll(`${c.name}: Sobrevivência ${result.survivalRoll.successes} sucesso(s)${result.fireRoll?`; ataque de fogo ${result.fireBaseDice} Dados Base, dano ${result.fireDamage}`:''}; ${result.status==='escaped'?'escapou':result.status==='broken'?'ficou Acabada':'ainda precisa tentar escapar'}.${result.completed?' Grupo fora do incêndio.':''}`);})} className="mr-2 rounded border border-orange-700 px-3 py-2 disabled:opacity-50">{c.name} · {state==='escaped'?'escapou':state==='broken'?'Acabada':state==='dead'?'fora de ação':`resolver fuga (falhas: ${String(progress?.attempts||0)})`}</button>})}
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===27&&
             ['comfort','investigate'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId))&&
             (()=>{const decision=event.result?.officialSceneDecision as Record<string,unknown>;const stage=String(decision.stage||'');
               if(decision.choiceId==='comfort'&&stage==='linn_waiting')return <div className="rounded border border-violet-800 p-3 space-y-2"><p>Linn só contará o ocorrido depois do D6 de dias registrado e se continuar com o grupo. Prazo R88: dia {String(decision.linnRevealDay)}; dia atual: {String(campaign.currentDay||1)}.</p>{Number(campaign.currentDay||1)>=Number(decision.linnRevealDay)&&<><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Confirme que o D6 de dias passou (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`linn-time:${event.id}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'linn_d6_days_elapsed',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="rounded border border-violet-700 px-3 py-2 disabled:opacity-50">Registrar passagem do D6</button></>}</div>;
               if(decision.choiceId==='comfort'&&stage==='linn_revelation_pending')return <div className="rounded border border-violet-800 p-3 space-y-2"><p>A revelação está disponível; R88 não fornece ficha, atributos ou estatísticas para Linn.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da permanência de Linn (mínimo 12 caracteres)"/>{(['linn_follows_party','linn_left'] as const).map(outcome=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`linn-follow:${event.id}:${outcome}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(outcome==='linn_follows_party'?'Linn foi registrada como NPC sem ficha inventada e acompanhará o grupo.':'Linn ficou na clareira após a revelação.');})} className="mr-2 rounded border border-violet-700 px-3 py-2 disabled:opacity-50">{outcome==='linn_follows_party'?'Linn acompanha o grupo':'Deixar Linn na clareira'}</button>)}</div>;
               if(decision.choiceId==='investigate')return <div className="rounded border border-violet-800 p-3 space-y-2"><p>Registre o que a MdJ decidiu que a investigação revelou; a entrada não define teste nem recompensa.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da investigação (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`linn-investigate:${event.id}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'linn_clearing_investigated',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="rounded border border-violet-700 px-3 py-2 disabled:opacity-50">Registrar investigação concluída</button></div>;
               return <div className="rounded border border-violet-800 p-3 space-y-2"><p>A R88 exige que Linn seja alimentada para contar sua história; registre a comida oferecida sem presumir transferência automática.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Alimento e cuidado oferecidos (mínimo 12 caracteres)"/>{(stage==='linn_care_pending'?[['linn_fed_and_kept','Confirmar que foi alimentada e ficará com o grupo'],['linn_abandoned','Deixar Linn na clareira']]:[['linn_fed_and_kept','Registrar que Linn foi alimentada e ficará'],['linn_not_fed','Registrar que ainda não foi alimentada'],['linn_abandoned','Deixar Linn na clareira']]).map(([outcome,label])=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`linn-care:${event.id}:${outcome}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id),()=>Math.random());setEncounterResolution(event.id,'');setLastRoll(outcome==='linn_fed_and_kept'?'O prazo D6 foi registrado a partir do dia atual da campanha.':'Decisão de cuidado registrada sem efeitos inventados.');})} className="mr-2 rounded border border-violet-700 px-3 py-2 disabled:opacity-50">{label}</button>)}</div>;
             })()}
            {event.status==='open'&&Number(mechanic.encounterId)===28&&
             ['pending_external_mechanics','cabin_tracks_found'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))&&
             ['search','track'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId))&&
             <div className="rounded border border-stone-700 p-3 space-y-2"><p>A entrada não determina teste de rastreamento nem cria um combate automático. Registre o resultado decidido pela MdJ; a família e os captores não recebem fichas inventadas.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Resultado da busca/rastreamento (mínimo 12 caracteres)"/>{((((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='search')&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage!=='cabin_tracks_found')?[['tracks_found','Encontrar pegadas recentes'],['tracks_lost','Não conseguir seguir os rastros']]:[['captors_tracked','Alcançar os captores e descobrir a mina de prata'],['trail_lost','Perder a trilha']]).map(([outcome,label])=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`cabin-track:${event.id}:${outcome}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(outcome==='captors_tracked'?'A trilha levou aos captores e à necessidade do lorde por escravos para a mina de prata.':'O resultado do rastreamento foi registrado.');})} className="mr-2 rounded border border-stone-600 px-3 py-2 disabled:opacity-50">{label}</button>)}</div>}
            {event.status==='open'&&['combat_active','teramalda_spike_removed'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))&&
             <div className="space-y-2"><p className="text-text-secondary">Combate vinculado à cena. Resolva a batalha pelo motor de combate; nenhuma vitória ou dano será presumido.</p>
             {worldState.encounter?.status==='resolved'&&<button disabled={!!busy} onClick={()=>void act(`official-combat-finish:${event.id}`,async()=>{
               await JourneyService.completeOfficialSceneCombat(campaign.id,event.id);
               setLastRoll('Desfecho do combate real vinculado à ocorrência e jornada liberada.');
             })} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Registrar desfecho do combate</button>}</div>}
            {event.status==='open'&&Number(mechanic.encounterId)===29&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='combat_active'&&
             worldState.encounter?.status==='resolved'&&worldState.encounter.combatants.some(item=>item.side==='enemy'&&item.entityId?.startsWith('official-r88-skeleton:')&&item.status==='active')&&
             <div className="rounded border border-stone-700 p-3 space-y-2"><p>Os esqueletos perseguem enquanto mantêm o grupo à vista. Use Fugir no turno de cada integrante e confirme aqui, como MdJ, quando a linha de visão realmente se romper.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Como os esqueletos perderam o grupo de vista? (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`skeleton-pursuit:${event.id}`,async()=>{await JourneyService.confirmOfficialSkeletonPursuitEnded(campaign.id,event.id,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('A MdJ confirmou a perda da linha de visão; os esqueletos cessaram a perseguição.');})} className="rounded border border-stone-600 px-3 py-2 disabled:opacity-50">Confirmar perda da linha de visão</button></div>}
            {event.status==='open'&&Number(mechanic.encounterId)===30&&
             ['combat_active','teramalda_spike_removed'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))&&
             <div className="rounded border border-red-800 p-3 space-y-2"><p>Teramalda é imune a todo dano. Só pode morrer após imobilização confirmada pela MdJ e teste real de Potência Formidável (−3), ação lenta.</p>
              {!(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.teramaldaImmobilized&&<><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Como a MdJ imobilizou Teramalda? (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`teramalda-immobilize:${event.id}`,async()=>{await JourneyService.recordOfficialTeramaldaImmobilization(campaign.id,event.id,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('Imobilização de Teramalda registrada pela MdJ; a tentativa de retirar o espigão está disponível na ação lenta.');})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Confirmar imobilização pela MdJ</button></>}
              {(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.teramaldaImmobilized===true&&worldState.encounter?.status==='active'&&characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.id===worldState.encounter?.combatants[worldState.encounter.turnIndex]?.entityId).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`teramalda-spike:${event.id}:${c.id}`,async()=>{const result=await JourneyService.attemptOfficialTeramaldaSpikeRemoval(campaign.id,event.id,c.id);setLastRoll(`${c.name}: Potência Formidável (−3) ${result.roll.successes} sucesso(s); ${result.removed?'removeu o espigão e matou Teramalda':'falhou, Teramalda continua imune e ativa'}.`);})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">{c.name}: gastar ação lenta para retirar espigão</button>)}
             </div>}
            {event.status==='open'&&[14,17,18].includes(Number(mechanic.encounterId))&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&(()=>{
              const choices:Record<string,{outcome:string;label:string}>={
                '14:consider_joining':{outcome:'invitation_declined_after_discussion',label:'Recusar convite após conversar'},
                '17:attend':{outcome:'celebration_completed_without_fight',label:'Concluir celebração sem briga'},
                '17:prevent_fight':{outcome:'wedding_fight_prevented',label:'Registrar briga impedida'},
                '18:tea':{outcome:'tea_visit_completed',label:'Concluir visita após o chá'},
                '18:inspect':{outcome:'dough_origin_explained',label:'Registrar explicação de Melkar'},
              };
              const key=`${Number(mechanic.encounterId)}:${String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId||'')}`;
              const choice=choices[key];
              return choice?<div className="rounded border border-teal-800 p-3 space-y-2">
                <p>Registre como a Mestra conduziu a conversa e confirmou este desfecho. Equipamentos, ferimentos, corrupção e combates exigem suas próprias ações.</p>
                <textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Desfecho da interação confirmado pela Mestra (12–600 caracteres)"/>
                <button data-testid={`fl-social-outcome-${event.id}`} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`official-social:${event.id}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,choice.outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(choice.label);})} className="rounded border border-teal-700 px-3 py-2 disabled:opacity-50">{choice.label}</button>
              </div>:null;
             })()}
            {event.status==='open'&&Number(mechanic.encounterId)===21&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='ghost_follow_up_pending'&&
             <div className="rounded border border-cyan-800 p-3 space-y-2"><p>Garmagol continua presente. Escolha falar, atacar ou tentar sair; a descoberta da adaga não encerra a cena.</p>
               {(['speak','attack','withdraw'] as const).map(choice=><button key={choice} disabled={!!busy} onClick={()=>void act('ghost-follow-up:'+event.id+':'+choice,async()=>{await JourneyService.advanceOfficialVengefulGhostScene(campaign.id,event.id,choice);setLastRoll(choice==='speak'?'Conversa aguardando adjudicação da MdJ.':choice==='attack'?'Combate com Garmagol disponível.':'Retirada aguardando adjudicação da MdJ.');})} className="mr-2 rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">{choice==='speak'?'Falar da adaga e da morte':choice==='attack'?'Atacar Garmagol':'Tentar se afastar'}</button>)}
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===21&&
             ((['speak','withdraw'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId))&&
               (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics')||
              (['speak','withdraw'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.followUpChoiceId))&&
               ['ghost_conversation_pending','ghost_withdrawal_pending'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))))&&
             <div className="rounded border border-cyan-800 p-3 space-y-2"><p>Registre a decisão da MdJ. A R88 não determina teste, resultado automático ou recuperação da adaga.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da cena (mínimo 12 caracteres)"/>
               {(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.followUpChoiceId||(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId)==='speak'?<>
                 <button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('ghost-conversation:'+event.id,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'conversation_succeeded',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">A conversa funcionou</button>
                 <button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('ghost-conversation-failed:'+event.id,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'conversation_failed',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">A conversa não funcionou</button></>:<>
                 <button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('ghost-withdraw:'+event.id,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'withdrawal_succeeded',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">O grupo conseguiu se afastar</button>
                 <button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('ghost-withdraw-failed:'+event.id,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'withdrawal_failed',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">O grupo não conseguiu se afastar</button></>)}
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===21&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='observe_dagger'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-cyan-800 p-3 space-y-2"><p>A R88 não define teste para perceber a adaga. Registre a observação da MdJ.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da observação (mínimo 12 caracteres)"/>{(['dagger_spotted','dagger_not_spotted'] as const).map(outcome=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('ghost-observation:'+event.id+':'+outcome,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-cyan-700 px-3 py-2 disabled:opacity-50">{outcome==='dagger_spotted'?'A adaga foi notada':'A adaga não foi notada'}</button>)}</div>}
            {event.status==='open'&&Number(mechanic.encounterId)===22&&
             (((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='hear_request'&&
               (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics')||
              ((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='collect_mist'&&
               (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics')||
              (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='glass_collection_pending')&&
             <div className="rounded border border-sky-800 p-3 space-y-2"><p>A R88 não define o procedimento para coletar a névoa. Registre a decisão da MdJ sem inventar um teste.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação (mínimo 12 caracteres)"/>
               {(((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='hear_request')? [['decline','Recusar o pedido'],['accept','Aceitar e preparar a coleta']]:[['collection_succeeded','Registrar coleta'],['collection_not_performed','Registrar que não houve coleta']]).map(([outcome,label])=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('glassmaker:'+event.id+':'+outcome,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-sky-700 px-3 py-2 disabled:opacity-50">{label}</button>)}
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===22&&
             ['glass_mist_progression','blood_mist_hex_covered'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))&&
             <div className="rounded border border-sky-800 p-3 space-y-2"><p>{(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='glass_mist_progression'?'O vidro quebra após D6 horas: '+String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.glassShatterHoursD6)+' hora(s); a velocidade da progressão não está definida.':'O hex está coberto; a Névoa de Sangue dispersa após D6 semanas: '+String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.bloodMistDispersalWeeksD6)+' semana(s).'}</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Confirmação da passagem do tempo (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('glassmaker-time:'+event.id,async()=>{const breaking=(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='glass_mist_progression';await JourneyService.advanceOfficialGlassmakerTimeline(campaign.id,event.id,breaking?'confirm_d6_hours_elapsed':'confirm_d6_weeks_elapsed',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="rounded border border-sky-700 px-3 py-2 disabled:opacity-50">{(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='glass_mist_progression'?'Confirmar D6 horas transcorridas':'Confirmar D6 semanas transcorridas'}</button></div>}
            {event.status==='open'&&Number(mechanic.encounterId)===32&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='rat_baby_unrescued_pending'&&
             <div className="rounded border border-rose-800 p-3 space-y-2"><p>A R88 diz que o choro cessa após alguns minutos, sem definir um relógio exato. Confirme a passagem narrativa sem alterar o calendário.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da passagem de tempo (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`rats-quiet:${event.id}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'baby_cry_quiet',encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('O choro cessou após o intervalo narrativo confirmado pela MdJ; nenhum número de minutos foi inventado.');})} className="rounded border border-rose-700 px-3 py-2 disabled:opacity-50">Registrar que o choro cessou</button></div>}
            {event.status==='open'&&Number(mechanic.encounterId)===32&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='belinda_response_pending'&&
             <div className="rounded border border-rose-800 p-3 space-y-2"><p>Belinda finge não reconhecer o bebê; se pressionada, ela conta o que aconteceu. Não há teste indicado para essa interação.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Como a MdJ conduziu a conversa (mínimo 12 caracteres)"/>{(['belinda_not_pressed','belinda_pressed'] as const).map(outcome=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`belinda:${event.id}:${outcome}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(outcome==='belinda_pressed'?'Belinda cedeu e contou a origem do bebê; a informação ficou registrada como segredo da MdJ.':'O grupo não pressionou Belinda.');})} className="mr-2 rounded border border-rose-700 px-3 py-2 disabled:opacity-50">{outcome==='belinda_pressed'?'Pressionar; Belinda conta a verdade':'Não pressionar e deixar a cabana'}</button>)}</div>}
            {event.status==='open'&&Number(mechanic.encounterId)===34&&
             ['question_captain','help_escort'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId))&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-slate-700 p-3 space-y-2"><p>A fonte não fixa o tamanho da patrulha nem fichas para a trupe. Registre a decisão da MdJ sem gerar combatentes automaticamente.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da interação/confronto (mínimo 12 caracteres)"/>{(['holmar_released_troupe','villagers_protected','party_withdrew'] as const).map(outcome=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`iron-clown:${event.id}:${outcome}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-slate-600 px-3 py-2 disabled:opacity-50">{outcome==='holmar_released_troupe'?'Holmar libera a trupe':outcome==='villagers_protected'?'A escolta aldeã foi protegida':'O grupo se retirou'}</button>)}</div>}
            {event.status==='open'&&Number(mechanic.encounterId)===35&&
             ['offer_gift','offer_goblin','offer_adventurer','offer_horse'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId))&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-amber-800 p-3 space-y-2"><p>Registre a decisão da MdJ sobre a oferta. A R88 não define um teste para convencer Karg.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da oferta (mínimo 12 caracteres)"/><div className="flex flex-wrap gap-2">{(['troll_offer_accepted','troll_offer_refused'] as const).map(outcome=><button key={outcome} data-testid={`fl-karg-offer-${event.id}-${outcome}`} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`karg-offer:${event.id}:${outcome}`,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(outcome==='troll_offer_accepted'?'Karg aceitou a oferta; presentes e sacrifícios aguardam transferência/registro explícitos.':'Karg recusou a oferta; a cena continua aberta para outra escolha.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{outcome==='troll_offer_accepted'?'Karg aceita a oferta':'Karg recusa a oferta'}</button>)}</div></div>}
            {event.status==='open'&&Number(mechanic.encounterId)===35&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='offer_gift'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='karg_offer_accepted'&&
             <div className="rounded border border-amber-800 p-3 space-y-2"><p>A MdJ confirmou que Karg aceitou o presente. Escolha o objeto real do inventário que será transferido para ele; a anotação deve identificar por que o objeto foi aceito como brilhante.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação do presente aceito (mínimo 12 caracteres)"/><div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead').flatMap(c=>(['gear','weapons','armor'] as const).flatMap(collection=>(c[collection]||[]).map((item:any,index:number)=><button key={`${c.id}:${collection}:${index}`} data-testid={`fl-karg-gift-${event.id}-${c.id}-${collection}-${index}`} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`karg-gift:${event.id}:${c.id}:${collection}:${index}`,async()=>{await JourneyService.offerOfficialKargGift(campaign.id,event.id,c.id,collection,index,encounterResolution(event.id),InventorySelectionSnapshot.capture(item));setEncounterResolution(event.id,'');setLastRoll(`Presente transferido do inventário para Karg.`);})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name}: {String(item?.name||item?.label||'objeto')}</button>)))}</div></div>}
            {event.status==='open'&&Number(mechanic.encounterId)===35&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='karg_sacrifice_pending'&&
             <div className="rounded border border-red-800 p-3 space-y-2"><p>Karg aceitou o sacrifício. Selecione exatamente quem ou qual montaria foi devorada; a confirmação altera o estado persistente.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Registro da decisão da MdJ (mínimo 12 caracteres)"/>{(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='offer_adventurer'?<><select data-testid={`fl-karg-sacrifice-${event.id}`} value={trollSacrificeDrafts[event.id]||''} onChange={e=>setTrollSacrificeDrafts(current=>({...current,[event.id]:e.target.value}))} className="field"><option value="">Escolha a personagem</option>{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead').map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select><button disabled={!!busy||!trollSacrificeDrafts[event.id]||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`karg-devours-person:${event.id}`,async()=>{await JourneyService.resolveOfficialKargSacrifice(campaign.id,event.id,'adventurer',trollSacrificeDrafts[event.id],encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('A personagem escolhida foi devorada por Karg e marcada como morta.');})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Confirmar que Karg devorou a personagem</button></>:<><select data-testid={`fl-karg-sacrifice-${event.id}`} value={trollSacrificeDrafts[event.id]||''} onChange={e=>setTrollSacrificeDrafts(current=>({...current,[event.id]:e.target.value}))} className="field"><option value="">Escolha a montaria</option>{(worldState.journey.mounts||[]).map(m=><option key={m.id} value={m.id}>{m.name}</option>)}</select><button disabled={!!busy||!trollSacrificeDrafts[event.id]||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`karg-devours-horse:${event.id}`,async()=>{await JourneyService.resolveOfficialKargSacrifice(campaign.id,event.id,'horse',trollSacrificeDrafts[event.id],encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('A montaria escolhida foi devorada por Karg e removida da jornada.');})} className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Confirmar que Karg devorou a montaria</button></>}</div>}
            {event.status==='open'&&Number(mechanic.encounterId)===23&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='talk_troll'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-amber-800 p-3 space-y-2"><p>A página não determina o resultado da conversa com Ghormald. Registre a decisão da MdJ.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação (mínimo 12 caracteres)"/>{(['troll_agrees','troll_refuses'] as const).map(outcome=><button key={outcome} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('troll-talk:'+event.id+':'+outcome,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,outcome,encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="mr-2 rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{outcome==='troll_agrees'?'Ghormald aceita a conversa':'Ghormald recusa e luta pelo filhote'}</button>)}</div>}
            {event.status==='open'&&Number(mechanic.encounterId)===25&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='accept_candle_quest'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-emerald-800 p-3 space-y-2"><p>A entrada não define teste para encontrar o filho. Confirme com a MdJ se Gonlab foi localizado; só então o combate pode começar.</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Adjudicação da busca (mínimo 12 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act('candle-search:'+event.id,async()=>{await JourneyService.adjudicateOfficialEncounterFollowUp(campaign.id,event.id,'gonlab_found',encounterResolution(event.id));setEncounterResolution(event.id,'');})} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Confirmar que Gonlab foi localizado</button></div>}
            {event.status==='open'&&Number(mechanic.encounterId)===24&&
             ((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='oker_fate_pending'||((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='free_oker'))&&
             <div className="rounded border border-amber-800 p-3 space-y-2"><p>Oker continua sem ficha própria no Guia R88. Registre apenas se o grupo o liberta ou o deixa com Kurge.</p>
               {(['free_oker','leave_oker'] as const).map(choice=><button key={choice} disabled={!!busy} onClick={()=>void act(`oker-fate:${event.id}:${choice}`,async()=>{await JourneyService.resolveOfficialCursedOgrePrisoner(campaign.id,event.id,choice);setLastRoll(choice==='free_oker'?'Oker foi libertado; nenhum atributo foi presumido.':'Oker foi deixado no saco de Kurge.');})} className="mr-2 rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{choice==='free_oker'?'Libertar Oker':'Deixar Oker com Kurge'}</button>)}
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===25&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='candle_child_rescue_pending'&&
             <div className="rounded border border-emerald-800 p-3 space-y-2"><p>Confirme que o filho foi encontrado, libertado e trazido de volta a Engsle. As dez velas só funcionam depois de mergulhadas em Brilhazul pulverizada.</p>
               <textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={600} className="field" placeholder="Como o filho foi recuperado e voltou? (mínimo 12 caracteres)"/>
               {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead').map(c=><button key={c.id} disabled={!!busy||encounterResolution(event.id).trim().length<12} onClick={()=>void act(`candle-reward:${event.id}:${c.id}`,async()=>{await JourneyService.completeOfficialCandleQuest(campaign.id,event.id,c.id,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll(`Dez velas foram entregues a ${c.name}; continuam sem preparo até o banho em Brilhazul pulverizada.`);})} className="mr-2 rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Confirmar retorno e receber 10 velas · {c.name}</button>)}
             </div>}
            {(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             Number(mechanic.encounterId)===6&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='calm'&&
             <div className="space-y-2"><p>Escolha quem fará o teste real de Empatia + Adestramento.</p><div className="flex flex-wrap gap-2">
             {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&c.status!=='dead'&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`horse-calm:${event.id}:${c.id}`,async()=>{
               const o=await JourneyService.calmOfficialHorse(campaign.id,event.id,c.id);
               setLastRoll(`${c.name}: ${o.roll.successes} sucesso(s). ${o.calmed?'Cavalo acalmado.':'Cavalo continua arisco.'}`);
             })} className="rounded border border-teal-700 px-3 py-2 disabled:opacity-50">{c.name} · Adestramento</button>)}</div></div>}
            {event.status==='open'&&Number(mechanic.encounterId)===10&&(()=>{
              const d=(event.result?.officialSceneDecision||{}) as Record<string,unknown>;
              const profile=d.grelfGeneratedProfile as import('../services/OfficialDemonGenerator').OfficialGeneratedDemon|undefined;
              return <div className="rounded border border-purple-700 p-3 space-y-2">
               {d.stage==='pending_external_mechanics'&&d.choiceId==='confront'&&<button disabled={!!busy} className="rounded border border-purple-700 px-3 py-2" onClick={()=>void act(`grelf-generate:${event.id}`,()=>JourneyService.generateOfficialGrelfProfile(campaign.id,event.id))}>Gerar ficha de Grelf pelas tabelas oficiais</button>}
               {profile&&<><p>Forma: {String(profile.combatant.quickSheet?.form)} · Força {profile.combatant.maxStrength} · Armadura {profile.combatant.armor}. Dados da geração preservados: {profile.dice.map(r=>`${r.label}=D${r.sides}:${r.value}`).join(', ')}.</p><ul>{profile.situationalRules.map((rule,i)=><li key={i}>{rule}</li>)}</ul>{profile.sourceGaps.length>0&&<p role="alert">Lacuna da tabela: {profile.sourceGaps.join(' ')}. Não há rerrolagem automática nem combate liberado.</p>}</>}
               {d.stage==='grelf_profile_generated'&&profile&&!profile.sourceGaps.length&&<button disabled={!!busy} className="rounded border border-red-700 px-3 py-2" onClick={()=>void act(`grelf-start:${event.id}`,()=>JourneyService.startOfficialGrelfCombat(campaign.id,event.id))}>Iniciar combate com Grelf gerado</button>}
               {d.stage==='combat_active'&&worldState.encounter?.status==='resolved'&&!d.grelfSituationalResolutionAt&&<><p>Antes de conciliar, confirme a exposição à fraqueza, contato/veneno, habilidades opcionais e demais efeitos da ficha gerada. A conferência não altera dados, dano ou declara vitória.</p><textarea rows={3} maxLength={1200} className="field" value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} placeholder="Regras situacionais de Grelf e efeitos resolvidos (mínimo 20 caracteres)"/><button disabled={!!busy||encounterResolution(event.id).trim().length<20} className="rounded border border-purple-700 px-3 py-2" onClick={()=>void act(`grelf-rules:${event.id}`,()=>JourneyService.recordOfficialGrelfSituationalResolution(campaign.id,event.id,encounterResolution(event.id)))}>Conferir regras situacionais de Grelf</button></>}
              </div>;
             })()}
            {event.status==='open'&&[2,17].includes(Number(mechanic.encounterId))&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.pending==='combat'&&(()=>{
              const id=Number(mechanic.encounterId),draft=adoptedSocialDrafts[event.id]||{orcs:'',humans:''};
              return <div className="rounded border border-amber-700 p-3 space-y-2"><p>{id===2?'A Mestra pode adotar, explicitamente, o Elfo Inquieto (p.54) para Heldemin e o Orc Roka (p.65) para Alachran. Estes são perfis típicos escolhidos pela mesa; a entrada não publica fichas individuais.':'A Mestra escolhe a quantidade de convidados que entram na briga e confirma os perfis típicos: Orc Roka (p.65) e Soldado humano (p.183). A quantidade não vem da entrada.'}</p>
               {id===17&&<div className="flex gap-2"><label>Convidados orcs<input aria-label="Convidados orcs adjudicados" className="field" type="number" min={1} max={23} value={draft.orcs} onChange={e=>setAdoptedSocialDrafts(v=>({...v,[event.id]:{...draft,orcs:e.target.value}}))}/></label><label>Convidados humanos<input aria-label="Convidados humanos adjudicados" className="field" type="number" min={1} max={23} value={draft.humans} onChange={e=>setAdoptedSocialDrafts(v=>({...v,[event.id]:{...draft,humans:e.target.value}}))}/></label></div>}
               <textarea className="field" rows={2} maxLength={600} value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} placeholder="Confirmação da Mestra sobre os perfis adotados (mínimo 12 caracteres)"/>
               <button disabled={!!busy||encounterResolution(event.id).trim().length<12||(id===17&&(!draft.orcs||!draft.humans))} className="rounded border border-red-700 px-3 py-2" onClick={()=>void act(`adopted-combat:${event.id}`,async()=>{await JourneyService.startOfficialAdoptedSocialCombat(campaign.id,event.id,encounterResolution(event.id),id===17?Number(draft.orcs):undefined,id===17?Number(draft.humans):undefined);setEncounterResolution(event.id,'');})}>Iniciar combate com perfis típicos confirmados</button>
              </div>;
             })()}
            {event.status==='open'&&Number(mechanic.encounterId)===6&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='capture'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-amber-700 p-3 space-y-2"><p>A entrada não determina teste para captura à força. A Mestra deve confirmar como o animal foi contido; ele continuará arisco, sem sucesso de Adestramento presumido.</p>
              <textarea className="field" rows={2} maxLength={600} value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} placeholder="Como o cavalo foi contido? (mínimo 12 caracteres)"/>
              {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy||encounterResolution(event.id).trim().length<12} className="rounded border border-amber-700 px-3 py-2" onClick={()=>void act(`horse-capture:${event.id}:${c.id}`,async()=>{await JourneyService.recordOfficialHorseCapture(campaign.id,event.id,c.id,encounterResolution(event.id));setEncounterResolution(event.id,'');})}>Confirmar contenção · {c.name}</button>)}
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===16&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.choiceId==='help'&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics'&&
             <div className="rounded border border-amber-700 p-3 space-y-2"><p>Registre o reparo e o atendimento da perna separadamente. A fonte não fornece ficha do piloto nem teste específico; este registro não cura automaticamente a lesão.</p>
              <textarea className="field" rows={2} maxLength={600} value={balloonAssistanceDrafts[event.id]?.repair||''} onChange={e=>setBalloonAssistanceDrafts(v=>({...v,[event.id]:{repair:e.target.value,care:v[event.id]?.care||''}}))} placeholder="Como o balão foi reparado? (mínimo 12 caracteres)"/>
              <textarea className="field" rows={2} maxLength={600} value={balloonAssistanceDrafts[event.id]?.care||''} onChange={e=>setBalloonAssistanceDrafts(v=>({...v,[event.id]:{care:e.target.value,repair:v[event.id]?.repair||''}}))} placeholder="Como a perna foi atendida? (mínimo 12 caracteres)"/>
              <button disabled={!!busy||(balloonAssistanceDrafts[event.id]?.repair||'').trim().length<12||(balloonAssistanceDrafts[event.id]?.care||'').trim().length<12} className="rounded border border-amber-700 px-3 py-2" onClick={()=>void act(`balloon-assist:${event.id}`,async()=>{const draft=balloonAssistanceDrafts[event.id];await JourneyService.recordOfficialBalloonAssistance(campaign.id,event.id,draft.repair,draft.care);})}>Registrar reparo e atendimento</button>
             </div>}
            {event.status==='open'&&Number(mechanic.encounterId)===16&&
             (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='balloon_loot_recovered'&&
             <div className="rounded border border-amber-700 p-3 space-y-2"><p>Se Andred não abriu fogo e o grupo já se afastou, registre esse desfecho. Um combate ativo precisa ser resolvido antes.</p>
              <textarea className="field" rows={2} maxLength={600} value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} placeholder="Resposta de Andred e afastamento sem combate (mínimo 12 caracteres)"/>
              <button disabled={!!busy||worldState.encounter?.status==='active'||encounterResolution(event.id).trim().length<12} className="rounded border border-amber-700 px-3 py-2" onClick={()=>void act(`balloon-response:${event.id}`,async()=>{await JourneyService.recordOfficialBalloonPilotResponse(campaign.id,event.id,encounterResolution(event.id));setEncounterResolution(event.id,'');})}>Confirmar que Andred não abriu fogo</button>
             </div>}
            {['horse_test_recorded','horse_capture_recorded'].includes(String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage))&&
             <div className="space-y-2"><p className="text-text-secondary">Teste salvo: {String((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseCalmed===true?'sucesso':'falha')}. Nenhuma posse foi transferida automaticamente.</p>
             {((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseCalmed===true||(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseCaptureConfirmed===true)&&
              !!(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseInsigniaRecoveredAt&&
              !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseValuableRecoveredAt&&
              <div className="space-y-2"><p>Alforje: item VALIOSO carregado (tabela oficial, p. 189). Escolha quem o recebe.</p>
              {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`horse-valuable:${event.id}:${c.id}`,async()=>{
                const o=await JourneyService.recoverOfficialHorseValuable(campaign.id,event.id,c.id);
                setLastRoll(`D66 ${o.treasure.roll}: ${o.treasure.name} transferido para ${c.name}.`);
              })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name} · recolher VALIOSO</button>)}</div>}
             {((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseCalmed===true||(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseCaptureConfirmed===true)&&
              !(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseInsigniaRecoveredAt&&
              <div className="flex flex-wrap gap-2"><p className="w-full text-amber-100">Alforjes: símbolo da família Lavide e um item VALIOSO (sorteio da tabela oficial ainda pendente).</p>
              {characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).map(c=><button key={c.id} disabled={!!busy} onClick={()=>void act(`horse-insignia:${event.id}:${c.id}`,async()=>{
                await JourneyService.retrieveOfficialHorseInsignia(campaign.id,event.id,c.id);
                setLastRoll(`${c.name} recebeu a insígnia real; item valioso e recompensa familiar continuam pendentes.`);
              })} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">{c.name} recolhe a insígnia</button>)}</div>}
             <button disabled={!!busy} onClick={()=>void act(`horse-leave:${event.id}`,async()=>{await JourneyService.finishOfficialHorse(campaign.id,event.id,'leave');setLastRoll('Cavalo deixado no local; teste preservado.');})} className="rounded border border-border px-3 py-2 disabled:opacity-50">Deixar o cavalo</button>
             {((event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseCalmed===true||(event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.horseCaptureConfirmed===true)&&
             <button disabled={!!busy} onClick={()=>void act(`horse-claim:${event.id}`,async()=>{await JourneyService.finishOfficialHorse(campaign.id,event.id,'claim');setLastRoll('Garanhão conduzido como montaria em custódia; a posse definitiva exige devolução à família Lavide.');})} className="rounded border border-emerald-700 px-3 py-2 disabled:opacity-50">Levar cavalo sob guarda (ainda não é propriedade)</button>}</div>}
          </>}</div>}
          {event.status==='open'&&mechanic?.kind==='official_random_encounter'&&(![9,13,15,...OfficialEncounterRulesEngine.specialSceneIds()].includes(Number(mechanic.encounterId)))&&<div className="mt-2 rounded border border-violet-800/60 p-3 space-y-2"><p className="text-violet-200">Resultado oficial #{String(mechanic.encounterId||event.result?.encounterId||'—')} · D66 {String(mechanic.d66||event.result?.d66||'—')} · terreno {String(mechanic.terrain||event.result?.terrain||'—')}.</p><p className="text-text-secondary">{Number(mechanic.encounterId)===15?'Guia do Mestre, p. 154: vinte pedintes famintos e silenciosos. A entrada não determina testes, pagamentos ou efeitos automáticos. Registre a reação do grupo; qualquer doação real de recursos deve ser realizada no inventário, e não simulada por este botão.':`Fonte: Guia do Mestre, p. ${String(mechanic.sourcePage||'a conferir')}. Esta entrada ainda exige arbitragem mecânica usando o livro: a anotação de texto não aplica dados, dano ou efeitos automaticamente. A jornada fica bloqueada até registrar o resultado.`}</p><textarea value={encounterResolution(event.id)} onChange={e=>setEncounterResolution(event.id,e.target.value)} rows={2} maxLength={800} className="field" placeholder="Como o encontro foi resolvido?"/><button disabled={!!busy||!encounterResolution(event.id).trim()} onClick={()=>void act(`official-encounter:${event.id}`,async()=>{await JourneyService.resolveOfficialRandomEncounterEvent(campaign.id,event.id,encounterResolution(event.id));setEncounterResolution(event.id,'');setLastRoll('Encontro oficial resolvido e jornada liberada.');})} className="rounded border border-violet-700 px-3 py-2 disabled:opacity-50">Registrar resolução do encontro</button></div>}
          {event.status==='open'&&mechanic?.kind==='creature_encounter'&&<div className="mt-2 rounded border border-red-800/60 p-3 space-y-2"><p className="text-red-200">A regra manda a MdJ escolher o animal na tabela oficial. Escolha abaixo; o combate será aberto com os atributos do Guia do Mestre.</p><div className="flex flex-wrap gap-2">{JourneyService.officialWildAnimals().map(animal=><button key={animal.id} disabled={!!busy} onClick={()=>void act(`wild-animal:${event.id}:${animal.id}`,async()=>{await JourneyService.startWildAnimalEncounter(campaign.id,event.id,animal.id);setLastRoll(`Animal Selvagem: ${animal.name}; encontro iniciado.`);})} className="border border-red-700 rounded px-3 py-2 disabled:opacity-50">{animal.name}</button>)}</div></div>}
          {event.status==='open'&&mechanic?.kind==='fishing_gear_damaged'&&affected&&<div className="mt-2 rounded border border-amber-800/60 p-3 space-y-2"><p className="text-text-secondary">O equipamento quebrou. Escolha o item de pesca e tente repará-lo com ARTESANATO, ou descarte-o e obtenha outro.</p>{fishingGear.length?<div className="space-y-2">{fishingGear.map(entry=><div key={entry.index} className="flex flex-wrap items-center gap-2"><b>{entry.name}</b><button disabled={!!busy||!activityCharacterId} onClick={()=>void act(`fishing-repair:${event.id}:${entry.index}`,async()=>{const outcome=await JourneyService.resolveFishingGearDamage(campaign.id,event.id,entry.index,activityCharacterId);setLastRoll(outcome.repaired?`${outcome.itemName} reparado com Artesanato.`:outcome.destroyed?`Falha no reparo: ${outcome.itemName} foi destruído.`:`Falha no reparo de ${outcome.itemName}; continua danificado.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Reparar com {characters.find(c=>c.id===activityCharacterId)?.name||'artesã'}</button><button disabled={!!busy} onClick={()=>void act(`fishing-discard:${event.id}:${entry.index}`,async()=>{const name=await JourneyService.discardFishingGear(campaign.id,event.id,entry.index);setLastRoll(`${name} descartado; é necessário outro equipamento para Pescar.`);})} className="border border-border rounded px-3 py-2 disabled:opacity-50">Descartar</button></div>)}</div>:<p className="text-amber-200">Nenhum Anzol e Linha/Rede de Pesca cadastrado na ficha. Cadastre ou obtenha um equipamento de pesca para voltar a Pescar.</p>}</div>}
          {event.status==='open'&&mechanic?.kind==='maritime_overboard'&&<div className="mt-2 flex flex-wrap gap-2"><span className="w-full text-cyan-200">A MdJ escolhe quem caiu na água. O estado de afogamento será iniciado imediatamente.</span>{characters.filter(character=>character.status!=='dead').map(character=><button key={character.id} disabled={!!busy} onClick={()=>void act(`maritime-overboard:${event.id}:${character.id}`,async()=>{await JourneyService.resolveMaritimeOverboard(campaign.id,event.id,character.id,false);setLastRoll(`${character.name} caiu ao mar; fluxo de afogamento iniciado.`);})} className="border border-cyan-700 rounded px-3 py-2 disabled:opacity-50">{character.name} caiu ao mar</button>)}</div>}
          {event.status==='open'&&mechanic?.kind==='maritime_item_overboard'&&<div className="mt-2 space-y-2"><span className="block text-amber-200">Vendaval: a MdJ escolhe um item importante que caiu na água.</span>{characters.filter(character=>character.status!=='dead').map(character=><div key={character.id} className="flex flex-wrap gap-1"><b className="mr-2">{character.name}</b>{(['weapons','armor','gear'] as const).flatMap(collection=>(character[collection]||[]).map((item:any,index:number)=><button key={`${character.id}:${collection}:${index}`} data-testid={`fl-maritime-loss-${event.id}-${character.id}-${collection}-${index}`} disabled={!!busy} onClick={()=>void act(`maritime-item:${event.id}:${character.id}:${collection}:${index}`,async()=>{const outcome=await JourneyService.resolveMaritimeItemOverboard(campaign.id,event.id,character.id,collection,index,InventorySelectionSnapshot.capture(item));setLastRoll(`${outcome.itemName} caiu na água e foi perdido.`);})} className="border border-amber-800 rounded px-2 py-1 disabled:opacity-50">{String(item?.name||item?.label||`${collection} ${index+1}`)}</button>))}</div>)}</div>}
          {event.status==='open'&&mechanic?.kind==='lost'&&<button disabled={!!busy||!activityCharacterId||!!(event.result?.actorId&&event.result.actorId!==activityCharacterId)} onClick={()=>void act(`lost:${event.id}`,async()=>{const outcome=await JourneyService.resolveLostRoute(campaign.id,event.id,activityCharacterId);setLastRoll(outcome.found?'A rota foi encontrada.':'A rota continua perdida; passou mais um Quarto de Dia.');})} className="mt-2 border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Tentar encontrar a rota com a desbravadora</button>}
          {event.status==='open'&&mechanic?.kind==='persistent_complication'&&(()=>{const draft=animalDraft(event.id);const complication=event.persistentComplication;return <div className="mt-2 rounded border border-amber-800/60 p-3 space-y-2"><p className="text-text-secondary">O Mestre registra qual animal acompanha o grupo e define a complicação, sem automatizar consequências que a regra deixa a critério da mesa.</p>{!complication?<><label className="block">Animal<input value={draft.animal} onChange={e=>setAnimalDraft(event.id,'animal',e.target.value)} maxLength={80} className="field mt-1" placeholder="Ex.: corvo, raposa"/></label><label className="block">Complicação definida pelo Mestre<textarea value={draft.problem} onChange={e=>setAnimalDraft(event.id,'problem',e.target.value)} maxLength={500} rows={2} className="field mt-1" placeholder="Que problema o animal persistente causa durante a jornada?"/></label><button disabled={!!busy||!draft.animal.trim()||!draft.problem.trim()} onClick={()=>void act(`animal-complication:start:${event.id}`,async()=>{await JourneyService.startPersistentAnimalComplication(campaign.id,event.id,{animal:draft.animal,problem:draft.problem});setLastRoll(`Acompanhamento de ${draft.animal.trim()} iniciado.`);})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Iniciar acompanhamento</button></>:<><p><b>Animal:</b> {complication.animal}</p><p><b>Complicação:</b> {complication.problem}</p><ul className="list-disc pl-5 text-text-secondary">{complication.history.slice(-5).map((entry,index)=><li key={`${event.id}:animal:${index}`}>{entry.note}</li>)}</ul><label className="block">Novo acontecimento<input value={draft.note} onChange={e=>setAnimalDraft(event.id,'note',e.target.value)} maxLength={500} className="field mt-1" placeholder="Registre como a complicação evoluiu"/></label><button disabled={!!busy||!draft.note.trim()} onClick={()=>void act(`animal-complication:note:${event.id}`,async()=>{await JourneyService.recordPersistentAnimalComplication(campaign.id,event.id,draft.note);setAnimalDraft(event.id,'note','');setLastRoll('A complicação do animal foi atualizada.');})} className="rounded border border-amber-700 px-3 py-2 disabled:opacity-50">Registrar evolução</button><label className="block">Como terminou<input value={draft.resolution} onChange={e=>setAnimalDraft(event.id,'resolution',e.target.value)} maxLength={500} className="field mt-1" placeholder="Resolução definida pela mesa"/></label><button disabled={!!busy||!draft.resolution.trim()} onClick={()=>void act(`animal-complication:end:${event.id}`,async()=>{await JourneyService.resolvePersistentAnimalComplication(campaign.id,event.id,draft.resolution);setLastRoll(`Complicação de ${complication.animal} encerrada.`);})} className="rounded border border-emerald-800 px-3 py-2 disabled:opacity-50">Encerrar acompanhamento</button></>}</div>;})()}
          {event.status==='open'&&mechanic?.kind==='diseased_prey'&&<div className="mt-2 flex flex-wrap gap-2"><span className="w-full text-text-secondary">Registre quem comeu a carne; cada personagem só pode comer uma porção desta presa.</span>{characters.filter(character=>character.status!=='dead'&&!((event.result?.eatenByCharacterIds as string[]|undefined)||[]).includes(character.id)).map(character=><button key={character.id} disabled={!!busy||Boolean(character.diseaseState)||!CharacterActionRulesEngine.canAct(character)} onClick={()=>void act(`diseased-prey:${event.id}:${character.id}`,async()=>{const outcome=await JourneyService.eatDiseasedPrey(campaign.id,event.id,character.id);setLastRoll(`${character.name}: Resiliência ${outcome.patientRoll.successes} × Virulência ${outcome.diseaseRoll.successes}${outcome.infected?'; doença contraída, manifesta após 1 dia':' ; resistiu à doença'}.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">{character.name} comeu a carne doente</button>)}<button disabled={!!busy} onClick={()=>void act(`discard-prey:${event.id}`,async()=>{await JourneyService.discardDiseasedPrey(campaign.id,event.id);setLastRoll('A carne doente restante foi descartada.');})} className="border border-border rounded px-3 py-2 disabled:opacity-50">Descartar carne restante</button></div>}
          {event.status==='open'&&mechanic?.kind==='poisonous_plant'&&affected&&<div className="mt-2 rounded border border-emerald-900/70 p-3 space-y-2"><p className="text-text-secondary">O vegetal coletado só revela o veneno na próxima refeição. A fonte define Potência 3, mas não nomeia o subtipo; registre o subtipo aplicado pela mesa antes da rolagem disputada.</p><div className="flex flex-wrap gap-2">{(['lethal','paralyzing','soporific','hallucinogenic'] as const).map(kind=><button key={kind} disabled={!!busy||Boolean(affected.poisonState?.active)} onClick={()=>void act(`poisonous-vegetable:${event.id}:${kind}`,async()=>{const outcome=await JourneyService.eatPoisonousVegetable(campaign.id,event.id,affected.id,kind);setLastRoll(`${affected.name}: veneno ${kind} P3 · ${outcome.poisonRoll.successes} × Resiliência ${outcome.victimRoll.successes}${outcome.totalEffect?' · efeito total':' · efeito limitado'}.`);})} className="border border-emerald-800 rounded px-3 py-2 disabled:opacity-50">Próxima refeição · {kind}</button>)}<button disabled={!!busy} onClick={()=>void act(`discard-poisonous:${event.id}`,async()=>{await JourneyService.discardPoisonousVegetable(campaign.id,event.id);setLastRoll('Vegetal venenoso descartado.');})} className="border border-border rounded px-3 py-2 disabled:opacity-50">Descartar vegetal</button></div></div>}
          {event.status==='resolved'&&Number(mechanic?.encounterId)===41&&<div data-testid={`fl-soul-after-departure-${event.id}`} className="mt-2 rounded border border-violet-800 p-3 space-y-2"><p className="text-text-secondary">Garrafas lacradas adquiridas no veleiro continuam no inventário depois da partida. Abrir uma libera a alma; Ressurreição continua exigindo resolução própria.</p><div className="flex flex-wrap gap-2">{characters.filter(c=>CampaignPartyService.belongs(campaign,c)&&CharacterActionRulesEngine.canAct(c)).flatMap(c=>(c.gear||[]).flatMap((item:any,index:number)=>item?.itemKind==='official_soul_bottle'&&item?.soulBottleState==='sealed'&&item?.sourceEventId===event.id?[<button key={`open-soul-after:${c.id}:${index}`} data-testid={`fl-soul-open-after-${event.id}-${c.id}-${index}`} disabled={!!busy} onClick={()=>void act(`open-soul-after:${event.id}:${c.id}:${index}`,async()=>{const outcome=await JourneyService.openOfficialSoulBottle(campaign.id,event.id,c.id,index,InventorySelectionSnapshot.capture(item));setLastRoll(`Garrafa aberta após a partida: alma ${outcome.soulTokenId} libertada; Ressurreição pendente.`);})} className="rounded border border-violet-700 px-3 py-2 disabled:opacity-50">{c.name}: abrir garrafa lacrada</button>]:[]))}</div></div>}
          {event.status==='open'&&mechanic?.kind==='hunt_prey_choice'&&affected&&<div className="mt-2 rounded border border-emerald-800/60 p-3 space-y-2"><p className="text-text-secondary">{affected.name} encontrou <b>{String(event.result?.prey||'presa')}</b> (D6 {String(event.result?.preyRoll||'—')}). Cada sucesso extra na busca permite uma rerrolagem, mas não é possível voltar ao resultado anterior.</p><div className="flex flex-wrap gap-2">{Number(event.result?.remainingPreyRerolls||0)>0&&<button disabled={!!busy} onClick={()=>void act(`hunt-reroll:${event.id}`,async()=>{const outcome=await JourneyService.rerollHuntPrey(campaign.id,event.id);setLastRoll(`Nova presa: ${outcome.prey} (D6 ${outcome.preyRoll}); restam ${outcome.remaining} rerrolagem(ns).`);})} className="border border-emerald-700 rounded px-3 py-2 disabled:opacity-50">Rerrolar presa ({String(event.result?.remainingPreyRerolls||0)})</button>}<button disabled={!!busy} onClick={()=>void act(`hunt-keep:${event.id}`,async()=>{const outcome=await JourneyService.resolveHuntPreyChoice(campaign.id,event.id);setLastRoll(outcome.incompatibleTrap?`${outcome.prey} exige arma; a armadilha não produziu presa.`:outcome.encounterStarted?`Falha ao abater ${outcome.prey}; o javali iniciou um encontro.`:outcome.killSuccesses>0?`${outcome.prey} abatido: ${outcome.meat} Carne e ${outcome.pelts} Pele(s).`:`${outcome.prey} escapou sem rendimento.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Manter presa e tentar abater</button></div></div>}
          {event.status==='open'&&mechanic?.kind==='leech_infestation'&&<button disabled={!!busy||!activityCharacterId} onClick={()=>void act(`leeches:${event.id}`,async()=>{const outcome=await JourneyService.resolveLeeches(campaign.id,event.id,activityCharacterId);setLastRoll(`Sanguessugas removidas${outcome.damage?`; vítima sofreu ${outcome.damage} dano adicional de Força`:''}.`);})} className="mt-2 border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Remover sanguessugas com Cura</button>}
          {event.status==='open'&&mechanic?.kind==='lice'&&affected?.liceState&&<button disabled={!!busy||!activityCharacterId} onClick={()=>void act(`lice:${event.id}`,async()=>{const outcome=await JourneyService.cureLice(campaign.id,event.id,activityCharacterId);setLastRoll(outcome.cured?'Piolhos removidos; sono liberado.':'Cura falhou; piolhos continuam causando dano diário e impedindo sono.');})} className="mt-2 border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Tratar piolhos de {affected.name} com Cura</button>}
          {event.status==='open'&&mechanic?.kind==='clothing_damaged'&&<button disabled={!!busy||!activityCharacterId||!!(event.result?.pendingColdCheckCharacterIds as string[]|undefined)?.length} onClick={()=>void act(`clothing:${event.id}`,async()=>{const outcome=await JourneyService.repairTornClothing(campaign.id,event.id,activityCharacterId);setLastRoll(outcome.repaired?'Roupas reparadas.':'Falha no Artesanato; roupas continuam danificadas.');})} className="mt-2 border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Reparar roupas com Artesanato</button>}
          {event.status==='open'&&mechanic?.kind==='trap'&&<button disabled={!!busy||activityCharacterId!==event.result?.actorId} onClick={()=>void act(`trap:${event.id}`,async()=>{const outcome=await JourneyService.escapeHuntingTrap(campaign.id,event.id,activityCharacterId);setLastRoll(outcome.escaped?'A personagem escapou da armadilha.':'A personagem permanece presa.');})} className="mt-2 border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Escapar da armadilha com Movimentação</button>}
          {event.status==='open'&&mechanic?.kind==='gear_damaged'&&affected&&<div className="mt-2 rounded border border-amber-800/60 p-3 space-y-2"><p className="text-text-secondary">A MdJ escolhe qual item de {affected.name} quebrou. O reparo usa Força + ARTESANATO; itens com Bônus de Equipamento quebram em bônus 0 e uma falha no reparo os destrói.</p><div className="flex flex-wrap gap-2">{(['weapons','armor','gear'] as const).flatMap(collection=>(affected[collection]||[]).map((item:any,index:number)=><button key={`${collection}:${index}`} disabled={!!busy||!activityCharacterId} onClick={()=>void act(`broken-item:${event.id}:${collection}:${index}`,async()=>{const outcome=await JourneyService.resolveBrokenJourneyEquipment(campaign.id,event.id,collection,index,activityCharacterId);setLastRoll(outcome.repaired?`${outcome.itemName} foi reparado.`:outcome.destroyed?`${outcome.itemName} foi destruído após a falha no reparo.`:`O reparo de ${outcome.itemName} falhou; o item continua quebrado.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Quebrar e reparar {String(item?.name||item?.label||`${collection} ${index+1}`)}</button>))}</div></div>}
          {event.status==='open'&&mechanic?.kind==='lost_equipment'&&affected&&<div className="mt-2 flex flex-wrap gap-2"><span className="w-full text-text-secondary">Escolha o item que {affected.name} perdeu:</span>{(['weapons','gear',...(mechanic.scope==='random_party_member'?['armor']:[])] as Array<'weapons'|'gear'|'armor'>).flatMap(collection=>(affected[collection]||[]).map((item:any,index:number)=><button key={`${collection}:${index}`} disabled={!!busy} onClick={()=>void act(`lost-item:${event.id}`,async()=>{const outcome=await JourneyService.resolveLostEquipment(campaign.id,event.id,collection,index);setLastRoll(`${affected.name} perdeu ${outcome.itemName}.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Perder {String(item?.name||item?.label||`${collection} ${index+1}`)}</button>))}</div>}
          {event.status==='open'&&mechanic?.kind==='group_attack_and_check'&&((event.result?.pendingGearLossCharacterIds as string[]|undefined)||[]).map(characterId=>{const target=characters.find(character=>character.id===characterId);if(!target)return null;return <div key={characterId} className="mt-2 flex flex-wrap gap-2"><span className="w-full text-text-secondary">{target.name} falhou em salvar o equipamento. Escolha o item consumido pelo fogo:</span>{(['weapons','armor','gear'] as const).flatMap(collection=>(target[collection]||[]).map((item:any,index:number)=><button key={`${collection}:${index}`} disabled={!!busy} onClick={()=>void act(`fire-loss:${event.id}:${characterId}`,async()=>{const outcome=await JourneyService.resolveLostEquipment(campaign.id,event.id,collection,index,characterId);setLastRoll(`${target.name} perdeu ${outcome.itemName} no incêndio.`);})} className="border border-orange-700 rounded px-3 py-2 disabled:opacity-50">Perder {String(item?.name||item?.label||`${collection} ${index+1}`)}</button>))}</div>;})}
          {event.status==='open'&&mechanic?.kind==='group_attack_and_check'&&((event.result?.pendingCriticalInjuryCharacterIds as string[]|undefined)||[]).map(characterId=>{const target=characters.find(character=>character.id===characterId);if(!target)return null;return <div key={`fire-critical:${characterId}`} className="mt-2 flex flex-wrap gap-2"><span className="w-full text-red-200">Força Quebrada em {target.name}: escolha a tabela para resolver o ferimento grave:</span>{(['slash','puncture','blunt'] as const).map(table=><button key={table} disabled={!!busy} onClick={()=>void act(`fire-critical:${event.id}:${characterId}`,async()=>{const outcome=await JourneyService.resolvePendingJourneyCritical(campaign.id,event.id,characterId,table);setLastRoll(`${target.name}: ${outcome.name} · ${table} D66 ${outcome.roll}.`);})} className="border border-red-700 rounded px-3 py-2 disabled:opacity-50">Rolar {table} D66</button>)}</div>;})}{event.status==='open'&&mechanic?.kind==='stuck_fishing_gear'&&affected&&<div className="mt-2 flex flex-wrap gap-2"><span className="w-full text-text-secondary">Escolha o equipamento de pesca preso de {affected.name}:</span>{(affected.gear||[]).map((item:any,index:number)=><button key={index} disabled={!!busy} onClick={()=>void act(`fishing-snag:${event.id}`,async()=>{const outcome=await JourneyService.resolveSnaggedFishingGear(campaign.id,event.id,index);setLastRoll(`${outcome.itemName}: ${outcome.recovered?'recuperado':'perdido'}.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Recuperar {String(item?.name||item?.label||`item ${index+1}`)} com Potência</button>)}</div>}
          {event.status==='open'&&mechanic?.kind==='group_escape'&&<div className="mt-2 flex flex-wrap gap-2"><span className="w-full text-text-secondary">Já escaparam: {escaped.map(id=>characters.find(character=>character.id===id)?.name||id).join(', ')||'ninguém'} · ajudantes já escapados: {Math.min(3,escaped.length)}</span>{(['might',...(Array.isArray(mechanic.attributes)?['movement']:[])] as Array<'might'|'movement'>).map(choice=><button key={choice} disabled={!!busy||!activityCharacterId||escaped.includes(activityCharacterId)} onClick={()=>void act(`escape:${event.id}:${choice}`,async()=>{const outcome=await JourneyService.resolveGroupEscape(campaign.id,event.id,activityCharacterId,choice,Math.min(3,escaped.length));setLastRoll(outcome.allEscaped?'Todo o grupo escapou.':outcome.escaped?'Personagem escapou; faltam as outras.':`Falha: ${outcome.damage} dano(s). Pode tentar novamente.`);})} className="border border-amber-700 rounded px-3 py-2 disabled:opacity-50">Escapar com {choice==='might'?'Potência':'Movimentação'}</button>)}</div>}
        </div>;
      })}</div></details>}
    </div>}
  </section>;
}
