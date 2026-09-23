from pathlib import Path
import json, re, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4924.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# APP: connect canonical Journey mechanics directly to the Atlas.
# ---------------------------------------------------------------------------
app_rel="App.tsx"
app=read(app_rel)

app=req(app,
"import { RollResult } from './lib/dice';",
"import { RollResult, rollOneRing, secureRoll } from './lib/dice';",
"App dice import")

app=req(app,
"import type { RuleAnswer, MasterCommand, CompanyState } from './lib/master/types';",
"import type { RuleAnswer, MasterCommand, CompanyState, JourneyRole } from './lib/master/types';",
"App JourneyRole import")

if "from './lib/characterRules'" not in app:
    app=req(app,
    "import { createSkillTest, shouldSuppressRoutineTest } from './lib/rules/tests';",
    "import { createSkillTest, shouldSuppressRoutineTest } from './lib/rules/tests';\nimport { findSkill, targetNumberForSkill } from './lib/characterRules';",
    "App characterRules import")

if "shadowAtLimit" not in app.split("\n")[:120]:
    anchor="import { normalizeEquipmentMechanics } from './lib/rules/equipment';"
    app=req(app,anchor,anchor+"\nimport { shadowAtLimit } from './lib/rules/shadow';","App shadow import")

helper_anchor="function immersiveJourneyUiEnabled(): boolean {"
helpers="""const JOURNEY_ROLE_SKILL: Record<JourneyRole,string> = {
  guide: 'Viagem', scout: 'Exploração', hunter: 'Caçada', lookout: 'Vigilância',
};

function journeySkillRank(character: Character, skillName: string): number {
  return Math.max(0, Number(findSkill(character.sheet, skillName)?.rank) || 0);
}

function autoJourneyRoles(party: Character[]): Partial<Record<JourneyRole,string>> {
  if (!party.length) return {};
  const best = (skill: string) => [...party].sort((a,b) => journeySkillRank(b, skill) - journeySkillRank(a, skill))[0]?.id;
  return {
    guide: best('Viagem'),
    scout: best('Exploração'),
    hunter: best('Caçada'),
    lookout: best('Vigilância'),
  };
}

function rollJourneySkill(character: Character, skillName: string, extraDice = 0) {
  const skill = findSkill(character.sheet, skillName);
  const mode = shadowAtLimit(character.sheet) ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal';
  return rollOneRing(
    Math.max(0, Number(skill?.rank) || 0) + Math.trunc(extraDice),
    0,
    targetNumberForSkill(character.sheet, skillName),
    skillName,
    mode,
    Boolean(character.sheet.attributes.weary),
    Boolean(character.sheet.attributes.miserable),
  );
}

function classifyJourneyRegion(region: string, tags: string[] = []): 'border'|'wild'|'dark' {
  const text = (region + ' ' + tags.join(' ')).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
  if (/dark|shadow|sombr|mordor|angmar|barrow|tumular/.test(text)) return 'dark';
  if (/border|fronteira|shire|condado|bree|bri|rivendell|valfenda|lindon/.test(text)) return 'border';
  return 'wild';
}

function journeyFeatResults(region: 'border'|'wild'|'dark'): number[] {
  const count = region === 'wild' ? 1 : 2;
  return Array.from({length:count},()=>secureRoll(1,12));
}

"""
app=req(app,helper_anchor,helpers+helper_anchor,"Journey helpers")

# Replace whole map-discovery handler with auto-start + first March.
start=app.index("  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {")
end=app.index("\n  const handlePartyMoved = async (move: PartyMapMove) => {",start)
if start<0 or end<0: raise SystemExit("map discovery handler markers missing")
map_handler="""  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {
    const base = campaignRef.current;
    if (!base) return;

    const route = Array.isArray(discovery.plannedRouteHexIds) ? discovery.plannedRouteHexIds.filter(Boolean) : [];
    const partyIds = new Set(base.partyCharacterIds || []);
    const party = characters.filter(character => partyIds.has(character.id));
    let workingCampaign = base;
    let workingCharacters = characters;
    let workingNpcs = allNpcs;
    let audits: any[] = [];

    const currentMaster = normalizeMasterState(base);
    const destination = route.at(-1) || null;
    const shouldStartJourney = route.length > 0 && party.length > 0 && (
      currentMaster.journey.status === 'idle'
      || currentMaster.journey.status === 'completed'
      || currentMaster.journey.destinationHexId !== destination
    );

    if (shouldStartJourney) {
      const roles = autoJourneyRoles(party);
      const guide = party.find(character => character.id === roles.guide) || party[0];
      const march = rollJourneySkill(guide, 'Viagem');
      const mechanical = applyMasterCommands(base, characters, allNpcs, [
        {
          type: 'start_journey',
          destinationHexId: destination,
          roles,
          routeHexIds: route,
          mounted: false,
          forcedMarch: false,
          reason: 'Rota planejada no Atlas: Jornada iniciada automaticamente.',
        },
        {
          type: 'resolve_journey_march',
          success: Boolean(march.success),
          successIcons: Math.max(0, Number(march.tengwarCount) || 0),
          roll: march,
          reason: 'Primeira Jogada de Marcha automática do Guia ao iniciar a Jornada.',
        },
      ]);
      if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
      workingCampaign = mechanical.campaign;
      workingCharacters = mechanical.characters;
      workingNpcs = mechanical.npcs;
      audits = mechanical.audits;
    } else {
      const masterState = normalizeMasterState(base);
      workingCampaign = {
        ...base,
        masterState: {
          ...masterState,
          journey: {
            ...masterState.journey,
            plannedRouteHexIds: route,
            destinationHexId: destination || masterState.journey.destinationHexId,
          },
          updatedAt: Date.now(),
        },
        updatedAt: Date.now(),
      };
    }

    const committed = await saveCanonicalTransaction({
      campaign: workingCampaign,
      characters: workingCharacters,
      npcs: workingNpcs,
      audits,
      mapDiscovery: discovery,
      reason: `estado do atlas: ${reason}`,
    });
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(workingCharacters);
    setAllNpcs(workingNpcs);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: discovery.campaignId, mapId: discovery.mapId, currentHexId: discovery.currentHexId },
    }));
  };
"""
app=app[:start]+map_handler+app[end:]

# Replace movement handler: mechanical cascade + stale NPC clearing + narration.
start=app.index("  const handlePartyMoved = async (move: PartyMapMove) => {")
end=app.index("\n\n  type PreparedSceneAudio",start)
if start<0 or end<0: raise SystemExit("party move handler markers missing")
move_handler="""  const handlePartyMoved = async (move: PartyMapMove) => {
    const base = campaignRef.current;
    if (!base) return;

    const beforeMaster = normalizeMasterState(base);
    const beforeMinutes = Math.max(0, Number(beforeMaster.worldClock.elapsedMinutes) || 0);
    let workingCampaign = base;
    let workingCharacters = characters;
    let workingNpcs = allNpcs;
    let allAudits: any[] = [];
    const journeyNotes: string[] = [];

    const applyCommands = (commands: MasterCommand[]) => {
      const mechanical = applyMasterCommands(workingCampaign, workingCharacters, workingNpcs, commands);
      if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
      workingCampaign = mechanical.campaign;
      workingCharacters = mechanical.characters;
      workingNpcs = mechanical.npcs;
      allAudits.push(...mechanical.audits);
    };

    applyCommands([{
      type: 'move_party',
      toHexId: move.toHexId,
      locationName: move.locationName,
      distanceKm: move.distanceKm,
      terrain: move.terrain,
      reason: 'Movimento confirmado no Atlas pelo jogador.',
    }]);

    const manifest = await loadRuntimeMapManifest().catch(() => null);
    const currentHex = manifest?.hexes.find(hex => hex.id === move.toHexId);
    let master = normalizeMasterState(workingCampaign);

    // When a March reaches its Event point, resolve the complete canonical Event
    // immediately so moving on the map can never bypass Journey mechanics.
    if (master.journey.awaitingEventGeneration) {
      const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
      applyCommands([{
        type: 'generate_journey_event',
        region,
        targetDie: secureRoll(1,6),
        featResults: journeyFeatResults(region),
        terrain: move.terrain,
        reason: 'Evento de Jornada disparado no hex alcançado pela Jogada de Marcha.',
      }]);
      master = normalizeMasterState(workingCampaign);
      const event = master.journey.pendingEvent;
      if (event) {
        const targetId = master.company.journeyRoles[event.targetRole];
        const target = workingCharacters.find(character => character.id === targetId);
        if (!target) throw new Error('O Evento de Jornada apontou um papel sem Herói atribuído.');
        const roll = rollJourneySkill(target, event.skill, Number(event.successDiceModifier || 0));
        journeyNotes.push(`${event.description}: ${target.sheet.identity.name || target.name} — ${roll.success ? 'sucesso' : 'falha'}`);
        applyCommands([{
          type: 'resolve_journey_event',
          targetCharacterId: target.id,
          success: Boolean(roll.success),
          successIcons: Math.max(0, Number(roll.tengwarCount) || 0),
          roll,
          reason: `Evento de Jornada ${event.description} resolvido automaticamente pelo motor local.`,
        }]);
      }
    }

    master = normalizeMasterState(workingCampaign);

    // Dangerous-area stage or 20-hex stage: continue automatically once its
    // mandatory events are clear.
    if (master.journey.stageComplete && !master.journey.pendingEvent && !master.journey.awaitingEventGeneration) {
      applyCommands([{ type: 'continue_journey_stage', reason: 'Etapa de 20 hexes concluída; iniciar próxima etapa.' }]);
      master = normalizeMasterState(workingCampaign);
    }

    // After every Event (and at a fresh stage), the Guide immediately makes the
    // next March roll as required by the Journey sequence.
    if (
      ['planned','travelling','paused'].includes(master.journey.status)
      && !master.journey.pendingEvent
      && !master.journey.awaitingEventGeneration
      && !master.journey.awaitingEndRoll
      && !master.journey.stageComplete
      && (master.journey.plannedRouteHexIds || []).length > 0
      && Math.max(0, Number(master.journey.marchHexesRemaining) || 0) <= 0
    ) {
      const guideId = master.company.journeyRoles.guide;
      const guide = workingCharacters.find(character => character.id === guideId);
      if (!guide) throw new Error('A Jornada perdeu o Guia. Reconfigure a Companhia.');
      const march = rollJourneySkill(guide, 'Viagem');
      applyCommands([{
        type: 'resolve_journey_march',
        success: Boolean(march.success),
        successIcons: Math.max(0, Number(march.tengwarCount) || 0),
        roll: march,
        reason: 'Nova Jogada de Marcha automática após Evento/etapa.',
      }]);
      journeyNotes.push(`Marcha do Guia: ${march.success ? 'sucesso' : 'falha'} (${Math.max(0, Number(march.tengwarCount) || 0)} ícone(s))`);
    }

    master = normalizeMasterState(workingCampaign);

    // Reaching the destination closes the Journey and applies remaining Travel
    // Fatigue through the mandatory final Travel rolls.
    if (master.journey.awaitingEndRoll) {
      const endRolls: Record<string,{success:boolean;successIcons:number;mountVigour?:number}> = {};
      for (const hero of workingCharacters.filter(character => (workingCampaign.partyCharacterIds || []).includes(character.id))) {
        const roll = rollJourneySkill(hero, 'Viagem');
        endRolls[hero.id] = {
          success: Boolean(roll.success),
          successIcons: Math.max(0, Number(roll.tengwarCount) || 0),
          mountVigour: 0,
        };
      }
      applyCommands([{ type: 'complete_journey', endRolls, reason: 'Destino alcançado: encerrar Jornada e aplicar Fadiga de Viagem restante.' }]);
      journeyNotes.push('Destino alcançado; Fadiga de Viagem final foi calculada.');
    }

    master = normalizeMasterState(workingCampaign);
    const afterMinutes = Math.max(0, Number(master.worldClock.elapsedMinutes) || 0);
    const elapsedDays = Math.max(0, Math.round((afterMinutes - beforeMinutes) / 1440));

    // NPC presence belongs to a place/scene. Crossing a hex invalidates the old
    // cast before the new scene is generated.
    workingNpcs = workingNpcs.map(npc => ({ ...npc, presentInScene: false }));
    workingCampaign = {
      ...workingCampaign,
      currentLocation: move.locationName,
      presentNpcIds: [],
      masterState: {
        ...master,
        scene: {
          ...master.scene,
          kind: master.activeMode === 'journey' ? 'journey' : 'exploration',
          status: 'active',
          title: master.activeMode === 'journey' ? 'Jornada' : `Chegada a ${move.locationName}`,
          objective: master.activeMode === 'journey' ? 'Prosseguir pela rota e enfrentar os Eventos da Jornada' : 'Orientar-se no novo local',
          locationName: move.locationName,
          participantHeroIds: [...(workingCampaign.partyCharacterIds || [])],
          participantNpcIds: [],
          updatedAt: Date.now(),
        },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };

    const locationEvent = makeEvent('location_changed', undefined, {
      hexId: move.toHexId, location: move.locationName, terrain: move.terrain,
      elapsedDays, journeyNotes,
    });
    const dayEvent = elapsedDays > 0
      ? makeEvent('journey_day_completed', undefined, { days: elapsedDays, totalJourneyDays: master.journey.day || elapsedDays, hexId: move.toHexId })
      : null;
    workingCampaign = {
      ...workingCampaign,
      events: [...(workingCampaign.events || []), locationEvent, ...(dayEvent ? [dayEvent] : [])],
    };

    const reconciled = reconcileCanonicalState(workingCampaign, workingCharacters, workingNpcs, move.discovery, { mapWins: false });
    const environmentAdvance = await prepareEnvironmentAdvance(reconciled.campaign, locationEvent.id);
    const committed = await saveCanonicalTransaction({
      campaign: reconciled.campaign,
      characters: reconciled.characters,
      npcs: reconciled.npcs,
      audits: allAudits,
      mapDiscovery: reconciled.discovery,
      environmentState: environmentAdvance.state,
      autonomousEvents: environmentAdvance.autonomousEvents,
      campaignEvents: [locationEvent, ...(dayEvent ? [dayEvent] : [])],
      reason: 'movimento, Jornada, tempo, NPCs e ambiente confirmados atomicamente',
    });

    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(reconciled.characters);
    setAllNpcs(reconciled.npcs);
    setPresentNpcs([]);
    setCurrentImage(null);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: reconciled.discovery.campaignId, mapId: reconciled.discovery.mapId, currentHexId: reconciled.discovery.currentHexId },
    }));

    const finalMaster = normalizeMasterState(committed);
    const residualFatigue = reconciled.characters
      .filter(character => (committed.partyCharacterIds || []).includes(character.id))
      .filter(character => Math.max(0, Number(character.sheet.attributes.fatigue) || 0) > 0)
      .map(character => `${character.sheet.identity.name || character.name}: ${Math.max(0, Number(character.sheet.attributes.fatigue) || 0)}`);

    const mechanicalSummary = [
      `${elapsedDays} dia(s) transcorrido(s) neste deslocamento`,
      `calendário: ano ${finalMaster.worldClock.year}, dia ${finalMaster.worldClock.day}, estação ${finalMaster.worldClock.season}, ${String(finalMaster.worldClock.hour).padStart(2,'0')}:${String(finalMaster.worldClock.minute).padStart(2,'0')}`,
      ...journeyNotes,
      residualFatigue.length ? `Fadiga residual: ${residualFatigue.join('; ')}. Ela só reduz com Descanso Prolongado em refúgio protegido e seguro.` : 'Sem Fadiga residual aplicada às fichas.',
    ].join(' | ');

    // Every actual map move becomes a fresh GM scene. Movement is already
    // canonical: Gemini narrates it, never decides or replays the movement.
    setIsStoryLoading(true);
    setStoryError(null);
    setNarrationError(null);
    await processTurn(
      `[TRECHO DE JORNADA JÁ RESOLVIDO PELO MOTOR] A Companhia acabou de chegar a ${move.locationName}. ${mechanicalSummary}. Não mova a Companhia novamente. Não preserve NPCs do hex anterior. Narre o trecho percorrido, a passagem real do tempo, qualquer Evento de Jornada resolvido e a chegada ao novo hex. Se a Jornada terminou e houver Fadiga residual, deixe claro que a recuperação exige refúgio seguro.`,
      false,
      committed,
    );
  };
"""
app=app[:start]+move_handler+app[end:]

# Add calendar hard contract to every narrative generation.
needle="""Comandos rejeitados: ${JSON.stringify(mechanical.rejected)}
NPCs presentes validados:"""
replacement="""Calendário canônico: ano ${finalClock.year}; dia ${finalClock.day}; estação ${finalClock.season}; horário ${String(finalClock.hour).padStart(2,'0')}:${String(finalClock.minute).padStart(2,'0')}. REGRA DURA: nunca mencione outra estação, outro dia ou outro período do dia.
Comandos rejeitados: ${JSON.stringify(mechanical.rejected)}
NPCs presentes validados:"""
app=req(app,needle,replacement,"calendar narrative contract")
write(app_rel,app)

# ---------------------------------------------------------------------------
# Immersive header: show actual day number + clock, not generic "Dia".
# ---------------------------------------------------------------------------
imm_rel="components/immersive/ImmersiveJourneyView.tsx"
imm=read(imm_rel)
old="""          <Hud icon={resolveImmersiveAsset(assets, 'hud.time.' + clock.key)} text={clock.label} />
          <span>{seasonLabel(props.masterState.worldClock.season)}</span>"""
new="""          <Hud icon={resolveImmersiveAsset(assets, 'hud.time.' + clock.key)} text={`${clock.label} · Dia ${props.masterState.worldClock.day} · ${String(props.masterState.worldClock.hour).padStart(2,'0')}:${String(props.masterState.worldClock.minute).padStart(2,'0')}`} />
          <span>{seasonLabel(props.masterState.worldClock.season)}</span>"""
imm=req(imm,old,new,"immersive day header")

# Small journey status line below header so mechanical progression is visible.
header_end="""      </div>
    </header>"""
status="""      </div>
    </header>
    {props.masterState.activeMode === 'journey' && (
      <div style={{ position: 'absolute', top: 'calc(' + safeTop + ' + 66px)', left: 12, right: 12, zIndex: 79, pointerEvents: 'none', textAlign: 'center', fontSize: 10, color: '#e6d5b1', opacity: .82 }}>
        Jornada · {props.masterState.journey.day || 0} dia(s) acumulados · Marcha: {props.masterState.journey.marchHexesRemaining || 0} hex(es)
      </div>
    )}"""
imm=req(imm,header_end,status,"immersive journey status")
write(imm_rel,imm)

# ---------------------------------------------------------------------------
# Version 4.9.24
# ---------------------------------------------------------------------------
pkg=root/"package.json"; data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.24"; pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8"); t=re.sub(r'"version":\s*"4\.9\.23"','"version": "4.9.24"',t,count=2); lock.write_text(t,encoding="utf-8")
version_rel="lib/version.ts"; version=read(version_rel); version=re.sub(r"APP_VERSION = '4\.9\.23'","APP_VERSION = '4.9.24'",version); version=re.sub(r"APP_VERSION_CODE = 49023","APP_VERSION_CODE = 49024",version); write(version_rel,version)
gradle_rel="android/app/build.gradle.kts"; gradle=read(gradle_rel); gradle=re.sub(r"versionCode\s*=\s*49023","versionCode = 49024",gradle); gradle=re.sub(r'versionName\s*=\s*"4\.9\.23"','versionName = "4.9.24"',gradle); write(gradle_rel,gradle)
main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"; main=read(main_rel).replace("index.html?v=49023&immersive=1","index.html?v=49024&immersive=1"); write(main_rel,main)

# Hard gates
whole="\n".join(read(rel) for rel in ["App.tsx","components/immersive/ImmersiveJourneyView.tsx","lib/rules/journey.ts","lib/master/commands.ts"])
checks={
 "route-auto-start":"Rota planejada no Atlas: Jornada iniciada automaticamente." in whole,
 "auto-first-march":"Primeira Jogada de Marcha automática" in whole,
 "event-auto-resolve":"Evento de Jornada disparado no hex" in whole and "resolve_journey_event" in whole,
 "march-after-event":"Nova Jogada de Marcha automática após Evento/etapa." in whole,
 "auto-end-journey":"Destino alcançado: encerrar Jornada" in whole,
 "time-visible":"Dia ${props.masterState.worldClock.day}" in whole,
 "calendar-contract":"Calendário canônico: ano" in whole and "nunca mencione outra estação" in whole,
 "npc-clear":"presentNpcIds: []" in whole and "presentInScene: false" in whole,
 "fresh-scene":"TRECHO DE JORNADA JÁ RESOLVIDO PELO MOTOR" in whole,
 "day-event":"journey_day_completed" in whole,
 "residual-fatigue":"Fadiga residual" in whole and "refúgio protegido e seguro" in whole,
 "journey-status":"Marcha: {props.masterState.journey.marchHexesRemaining || 0} hex(es)" in whole,
 "full-4923-kept":"resolveJourneyMarch" in whole and "generateJourneyEvent" in whole and "resolveJourneyEvent" in whole,
 "db": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "package":"com.braseiro.oumanel" in read(gradle_rel),
 "version":"4.9.24" in read(version_rel) and "49024" in read(gradle_rel),
}
for name,ok in checks.items(): print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4924_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.24_ATLAS_JOURNEY_INTEGRATION_OK")
