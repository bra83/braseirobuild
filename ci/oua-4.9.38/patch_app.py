from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_app.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

def req(text, old, new, label):
    if old not in text:
        raise SystemExit("missing anchor: " + label)
    return text.replace(old, new, 1)

rel = "App.tsx"
app = read(rel)

app = req(
    app,
    "import { MechanicsHub } from './components/MechanicsHub';",
    """import { MechanicsHub } from './components/MechanicsHub';
import { JourneyResolutionPanel } from './components/JourneyResolutionPanel';
import { MechanicalStatusStrip } from './components/MechanicalStatusStrip';
import { attachResolutionSummary } from './lib/actionResolution';""",
    "UI imports",
)

old_roll = """function rollJourneySkill(character: Character, skillName: string, extraDice = 0) {
  const skill = findSkill(character.sheet, skillName);
  const mode = shadowAtLimit(character.sheet) ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal';
"""
new_roll = """function rollJourneySkill(character: Character, skillName: string, extraDice = 0, forcedMode?: 'favoured'|'normal'|'ill-favoured') {
  const skill = findSkill(character.sheet, skillName);
  const mode = forcedMode || (shadowAtLimit(character.sheet) ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal');
"""
app = req(app, old_roll, new_roll, "Journey roll mode")

auto_start = app.index("function autoJourneyRoles(party: Character[]):")
auto_end = app.index("\n\nfunction rollJourneySkill", auto_start)
app = app[:auto_start] + app[auto_end + 2:]

helper_anchor = "function classifyJourneyRegion(region: string, tags: string[] = []): 'border'|'wild'|'dark' {"
eye_helper = """function calculateInitialEyeAwareness(party: Character[]): number {
  let base = 0;
  for (const character of party) {
    const culture = String(character.sheet.identity.culture || character.culture || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').toLowerCase();
    if (/high elf|alto elfo/.test(culture)) base = Math.max(base, 3);
    else if (/dunedain|patrulheiro do norte|ranger of the north|elf|elfo/.test(culture)) base = Math.max(base, 2);
    else if (/dwarf|anao/.test(culture)) base = Math.max(base, 1);
  }
  let value = base;
  for (const character of party) {
    if (Math.max(0, Number(character.sheet.attributes.valor) || 0) >= 4) value += 1;
    const rewardText = String(character.sheet.other.rewards || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').toLowerCase();
    const inventoryText = (character.sheet.inventory || []).map(item => String(item.name || '') + ' ' + String(item.category || '') + ' ' + String(item.description || '')).join(' ').normalize('NFD').replace(/[\\u0300-\\u036f]/g, '').toLowerCase();
    const famousMatches = (rewardText + ' ' + inventoryText).match(/arma famosa|armadura famosa|famous weapon|famous armour|famous armor/g) || [];
    value += famousMatches.length;
  }
  return Math.max(0, value);
}

"""
app = req(app, helper_anchor, eye_helper + helper_anchor, "Eye initial helper")

# Generic player/world mechanical actions also receive a before/after resolution summary.
start = app.index("  const handleWorldCommands = async (commands: MasterCommand[], reason: string): Promise<void> => {")
end = app.index("\n\n  const handleMapDiscoveryChanged", start)
world_handler = """  const handleWorldCommands = async (commands: MasterCommand[], reason: string): Promise<void> => {
    const base = campaignRef.current;
    if (!base) throw new Error('Nenhuma crônica ativa.');
    const mechanical = applyMasterCommands(base, characters, allNpcs, commands);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
    const summarized = attachResolutionSummary(base, mechanical.campaign, characters, mechanical.characters, reason);
    const committed = await saveMasterTransaction(summarized, mechanical.characters, mechanical.audits, reason, mechanical.npcs);
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
    setPresentNpcs(mechanical.npcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
  };"""
app = app[:start] + world_handler + app[end:]

manual_handlers = """
  const commitJourneyAction = async (commands: MasterCommand[], reason: string, actionLabel: string) => {
    const base = campaignRef.current;
    if (!base) throw new Error('Nenhuma crônica ativa.');
    const mechanical = applyMasterCommands(base, characters, allNpcs, commands);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
    const summarized = attachResolutionSummary(base, mechanical.campaign, characters, mechanical.characters, actionLabel);
    const committed = await saveMasterTransaction(summarized, mechanical.characters, mechanical.audits, reason, mechanical.npcs);
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
    setPresentNpcs(mechanical.npcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
    return committed;
  };

  const handleSetEyeAwarenessEnabled = async (enabled: boolean) => {
    const base = campaignRef.current;
    if (!base) return;
    const partyIds = new Set(base.partyCharacterIds || []);
    const party = characters.filter(character => partyIds.has(character.id));
    const initial = calculateInitialEyeAwareness(party);
    await commitJourneyAction([{
      type: 'set_eye_awareness_rule',
      enabled,
      initial,
      huntThreshold: 16,
      reason: enabled ? 'Jogador ativou a regra opcional Vigilância do Olho.' : 'Jogador desativou a regra opcional Vigilância do Olho.',
    }], 'preferência da regra opcional Vigilância do Olho', enabled ? 'Ativar Vigilância do Olho' : 'Desativar Vigilância do Olho');
  };

  const handleAdjustEyeAwareness = async (amount: number, reason: string) => {
    await commitJourneyAction([{
      type: 'adjust_eye_awareness',
      amount,
      source: amount >= 3 ? 'magic-powerful' : amount === 2 ? 'magic-major' : 'magic-minor',
      reason,
    }], 'ajuste de Vigilância do Olho', reason);
  };

  const handleJourneyStartManual = async (roles: Partial<Record<JourneyRole,string>>, mounted: boolean, forcedMarch: boolean) => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    const route = (master.journey.plannedRouteHexIds || []).filter(Boolean);
    await commitJourneyAction([{
      type: 'player_start_journey',
      destinationHexId: route.at(-1) || master.journey.destinationHexId,
      roles,
      routeHexIds: route,
      mounted,
      forcedMarch,
      reason: 'Papéis e opções de Jornada escolhidos explicitamente pelo jogador.',
    }], 'preparação manual da Jornada', 'Preparar a Jornada');
  };

  const handleJourneyRollManual = async (
    kind: 'march'|'event'|'end',
    characterId: string,
    skill: string,
    extraDice = 0,
    forcedMode?: 'favoured'|'normal'|'ill-favoured',
    mountVigour = 0,
  ) => {
    const base = campaignRef.current;
    if (!base) return;
    const hero = characters.find(character => character.id === characterId);
    if (!hero) throw new Error('Herói da rolagem não encontrado.');
    const roll = rollJourneySkill(hero, skill, extraDice, forcedMode);
    const commands: MasterCommand[] = [{
      type: 'player_record_journey_roll',
      kind,
      targetCharacterId: hero.id,
      skill,
      targetNumber: targetNumberForSkill(hero.sheet, skill),
      roll,
      mountVigour,
      reason: skill + ' rolada manualmente pelo jogador.',
    }];
    const master = normalizeMasterState(base);
    const eyeRolled = roll.featResult === 11 || (Array.isArray(roll.featResults) && roll.featResults.includes(11));
    if (master.company.eyeAwarenessEnabled && master.activeMode !== 'combat' && eyeRolled) {
      commands.push({
        type: 'adjust_eye_awareness',
        amount: 1,
        source: 'eye-roll',
        reason: '+1: uma rolagem do jogador fora de combate mostrou o Olho de Sauron.',
      });
    }
    await commitJourneyAction(commands, 'rolagem manual de Jornada', 'Rolar ' + skill + ' · ' + (hero.sheet.identity.name || hero.name));
  };

  const handleApplyJourneyMarch = async () => {
    await commitJourneyAction([{ type: 'player_confirm_journey_march', reason: 'Resultado da Jogada de Marcha confirmado pelo jogador.' }], 'aplicar Marcha confirmada', 'Confirmar Jogada de Marcha');
  };

  const handleGenerateJourneyEventManual = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    const discovery = await loadMapDiscovery(base.id).catch(() => null);
    const manifest = await loadRuntimeMapManifest().catch(() => null);
    const currentHexId = discovery?.currentHexId || master.position.currentHexId;
    const currentHex = currentHexId ? manifest?.hexes.find(hex => hex.id === currentHexId) : undefined;
    const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
    const targetDie = secureRoll(1, 6);
    const featResults = journeyFeatResults(region);
    await commitJourneyAction([{
      type: 'player_generate_journey_event',
      region,
      targetDie,
      featResults,
      terrain: currentHex?.terrain,
      reason: 'Jogador rolou explicitamente o alvo e o Dado de Façanha do Evento de Jornada.',
    }], 'gerar Evento de Jornada manualmente', 'Rolar Evento de Jornada');
  };

  const handleApplyJourneyEvent = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const event = normalizeMasterState(base).journey.pendingEvent;
    if (!event) throw new Error('Nenhum Evento aguarda confirmação.');
    await commitJourneyAction([{ type: 'player_confirm_journey_event', reason: 'Consequência do Evento confirmada pelo jogador.' }], 'resolver Evento de Jornada confirmado', 'Resolver ' + event.description);
  };

  const handleContinueJourneyStage = async () => {
    await commitJourneyAction([{ type: 'player_continue_journey_stage', reason: 'Jogador confirmou a continuação para a próxima etapa.' }], 'continuar etapa de Jornada', 'Continuar Jornada');
  };

  const handleCompleteJourneyManual = async () => {
    await commitJourneyAction([{ type: 'player_complete_journey', reason: 'Jogador confirmou as rolagens finais e o encerramento da Jornada.' }], 'encerrar Jornada manualmente', 'Encerrar Jornada');
  };

  const handleOpenJourneyEncounter = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const beforeSceneId = base.history.at(-1)?.id || '';
    setIsStoryLoading(true);
    setStoryError(null);
    await processTurn('[ENCONTRO FORTUITO DA JORNADA] O Evento foi resolvido com sucesso. Apresente agora um encontro favorável e contextual ao local. Crie ou reutilize NPCs concretos com upsert_npc e marque explicitamente quem está presente com set_present_npcs. Não resolva outra Marcha, outro Evento ou movimento.', false, base);
    const after = campaignRef.current;
    if (!after) return;
    const sceneAdvanced = (after.history.at(-1)?.id || '') !== beforeSceneId;
    if (sceneAdvanced && (after.presentNpcIds || []).length > 0) {
      await commitJourneyAction([{ type: 'player_acknowledge_journey_encounter', reason: 'NPCs do Encontro Fortuito foram apresentados na cena.' }], 'confirmar Encontro Fortuito apresentado', 'Encontro Fortuito apresentado');
    }
  };

  const handleOpenEyeRevelation = async () => {
    const base = campaignRef.current;
    if (!base) return;
    setIsStoryLoading(true);
    setStoryError(null);
    await processTurn('[EPISÓDIO DE REVELAÇÃO] A Vigilância do Olho alcançou o Limiar de Caça. Apresente um episódio de Revelação coerente com a situação atual, sem escolher silenciosamente recursos ou decisões dos jogadores. A consequência deve surgir naturalmente da ficção.', false, base);
    const after = campaignRef.current;
    if (after && normalizeMasterState(after).company.eyeRevelationPending) {
      await commitJourneyAction([{ type: 'resolve_eye_revelation', reason: 'Episódio de Revelação apresentado pelo Mestre.' }], 'resolver episódio de Revelação', 'Episódio de Revelação');
    }
  };

"""
map_anchor = "  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {"
app = req(app, map_anchor, manual_handlers + map_anchor, "manual handlers")

# Planning a route only stores the route. It never assigns roles or rolls March.
start = app.index("  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {")
end = app.index("\n  const handlePartyRested = async", start)
map_handler = """  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {
    const base = campaignRef.current;
    if (!base) return;
    const masterState = normalizeMasterState(base);
    const route = Array.isArray(discovery.plannedRouteHexIds) ? discovery.plannedRouteHexIds.filter(Boolean) : [];
    const synced: CampaignState = {
      ...base,
      masterState: {
        ...masterState,
        journey: {
          ...masterState.journey,
          plannedRouteHexIds: route,
          destinationHexId: route.at(-1) || masterState.journey.destinationHexId,
        },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };
    const committed = await saveCanonicalTransaction({ campaign: synced, mapDiscovery: discovery, reason: 'estado do atlas: ' + reason });
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: discovery.campaignId, mapId: discovery.mapId, currentHexId: discovery.currentHexId },
    }));
  };
"""
app = app[:start] + map_handler + app[end:]

# Rest receives the same visible resolution summary.
start = app.index("  const handlePartyRested = async (kind: 'short' | 'long') => {")
end = app.index("\n  const handlePartyMoved = async", start)
rest_handler = """  const handlePartyRested = async (kind: 'short' | 'long') => {
    const base = campaignRef.current;
    if (!base) return;
    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'resolve_rest',
      kind,
      reason: kind === 'short' ? 'Descanso Curto resolvido pelo mapa.' : 'Descanso Prolongado resolvido pelo mapa.',
      source: 'O Um Anel 2e — Descanso',
    }]);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' '));
    const summarized = attachResolutionSummary(base, mechanical.campaign, characters, mechanical.characters, kind === 'short' ? 'Descanso Curto' : 'Descanso Prolongado');
    const committed = await saveMasterTransaction(summarized, mechanical.characters, mechanical.audits, kind === 'short' ? 'descanso curto' : 'descanso prolongado', mechanical.npcs);
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
  };
"""
app = app[:start] + rest_handler + app[end:]

# Movement consumes only an already authorized March hex. It never resolves the next rule step.
start = app.index("  const handlePartyMoved = async (move: PartyMapMove) => {")
end = app.index("\n\n  type PreparedSceneAudio", start)
move_handler = """  const handlePartyMoved = async (move: PartyMapMove) => {
    const base = campaignRef.current;
    if (!base) return;

    const beforeMaster = normalizeMasterState(base);
    const initialPlacement = !beforeMaster.position.currentHexId;
    if (!initialPlacement) {
      const activeJourney = ['planned', 'travelling', 'paused'].includes(beforeMaster.journey.status);
      const nextRouteHexId = (beforeMaster.journey.plannedRouteHexIds || [])[0] || null;
      if (!activeJourney || !nextRouteHexId) throw new Error('Movimento livre bloqueado: selecione um destino e use Planejar rota. Depois avance a Companhia pela Jornada.');
      if (move.toHexId !== nextRouteHexId) throw new Error('Movimento bloqueado: a Companhia só pode avançar para o próximo hex da rota planejada.');
      if (beforeMaster.journey.pendingRoll) throw new Error('Movimento bloqueado: confirme primeiro a rolagem de Jornada exibida.');
      if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) throw new Error('Movimento bloqueado: resolva primeiro o Evento de Jornada atual.');
      if (beforeMaster.journey.awaitingEndRoll) throw new Error('Movimento bloqueado: a Companhia alcançou o destino e precisa encerrar a Jornada.');
      if (beforeMaster.journey.stageComplete) throw new Error('Movimento bloqueado: conclua a etapa atual da Jornada antes de avançar.');
      if (Math.max(0, Number(beforeMaster.journey.marchHexesRemaining) || 0) <= 0) throw new Error('Movimento bloqueado: a Jogada de Marcha do Guia precisa ser resolvida antes do próximo hex.');
    }

    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'move_party',
      toHexId: move.toHexId,
      locationName: move.locationName,
      distanceKm: move.distanceKm,
      terrain: move.terrain,
      reason: 'Movimento confirmado no Atlas pelo jogador.',
    }]);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));

    const manifest = await loadRuntimeMapManifest().catch(() => null);
    const currentHex = manifest?.hexes.find(hex => hex.id === move.toHexId);
    const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
    let movedCampaign = mechanical.campaign;
    if (normalizeMasterState(movedCampaign).company.eyeAwarenessEnabled) {
      const movedMaster = normalizeMasterState(movedCampaign);
      const threshold = region === 'border' ? 18 : region === 'dark' ? 14 : 16;
      movedCampaign = {
        ...movedCampaign,
        masterState: {
          ...movedMaster,
          company: { ...movedMaster.company, eyeHuntThreshold: threshold },
          updatedAt: Date.now(),
        },
        updatedAt: Date.now(),
      };
    }

    const locationEvent = makeEvent('location_changed', undefined, { hexId: move.toHexId, location: move.locationName, terrain: move.terrain });
    movedCampaign = { ...movedCampaign, currentLocation: move.locationName, events: [...(movedCampaign.events || []), locationEvent], updatedAt: Date.now() };
    const reconciled = reconcileCanonicalState(movedCampaign, mechanical.characters, mechanical.npcs, move.discovery, { mapWins: false, preservePresenceUnion: true });
    const summarized = attachResolutionSummary(base, reconciled.campaign, characters, reconciled.characters, 'Viajar para ' + move.locationName);
    const environmentAdvance = await prepareEnvironmentAdvance(summarized, locationEvent.id);
    const committed = await saveCanonicalTransaction({
      campaign: summarized,
      characters: reconciled.characters,
      npcs: reconciled.npcs,
      audits: mechanical.audits,
      mapDiscovery: reconciled.discovery,
      environmentState: environmentAdvance.state,
      autonomousEvents: environmentAdvance.autonomousEvents,
      campaignEvents: [locationEvent],
      reason: 'movimento de Jornada confirmado sem automação de rolagens',
    });

    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(reconciled.characters);
    setAllNpcs(reconciled.npcs);
    setPresentNpcs(reconciled.npcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
    setCurrentImage(null);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: reconciled.discovery.campaignId, mapId: reconciled.discovery.mapId, currentHexId: reconciled.discovery.currentHexId },
    }));

    setIsStoryLoading(true);
    setStoryError(null);
    setNarrationError(null);
    await processTurn(
      '[MOVIMENTO DE JORNADA JÁ APLICADO] A Companhia chegou a ' + move.locationName + '. O movimento, o tempo e a Marcha já foram aplicados localmente. NÃO faça Jogada de Marcha, NÃO gere nem resolva Evento de Jornada e NÃO encerre a Jornada. Reavalie explicitamente os NPCs: preserve quem plausivelmente viajou ou pertence ao novo local; retire somente quem de fato saiu; introduza NPCs apenas quando a ficção atual justificar.',
      false,
      committed,
    );
  };
"""
app = app[:start] + move_handler + app[end:]

# Player rolls outside combat feed the optional Eye and get a visible summary.
start = app.index("  const handleSendRollToMaster = async (roll: RollResult) => {")
end = app.index("\n\n  const handleResolvePendingRoll = async () => {", start)
roll_handler = """  const handleSendRollToMaster = async (roll: RollResult) => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    let workingCampaign = base;
    let workingCharacters = characters;
    let workingNpcs = allNpcs;
    let audits: any[] = [];

    const eyeRolled = roll.featResult === 11 || (Array.isArray(roll.featResults) && roll.featResults.includes(11));
    if (master.company.eyeAwarenessEnabled && master.activeMode !== 'combat' && eyeRolled) {
      const mechanical = applyMasterCommands(base, characters, allNpcs, [{
        type: 'adjust_eye_awareness',
        amount: 1,
        source: 'eye-roll',
        reason: '+1: uma rolagem do jogador fora de combate mostrou o Olho de Sauron.',
      }]);
      if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
      workingCampaign = mechanical.campaign;
      workingCharacters = mechanical.characters;
      workingNpcs = mechanical.npcs;
      audits = mechanical.audits;
    }

    const updated: CampaignState = {
      ...workingCampaign,
      pendingDiceRoll: {
        notation: roll.notation,
        results: roll.results,
        modifier: roll.modifier,
        total: roll.total,
        timestamp: roll.timestamp,
        type: roll.type,
        featResult: roll.featResult,
        featResults: roll.featResults,
        rollMode: roll.rollMode,
        successResults: roll.successResults,
        targetNumber: roll.targetNumber,
        skillName: roll.skillName || workingCampaign.pendingTest?.skill,
        success: roll.success,
        successLevel: roll.successLevel,
        tengwarCount: roll.tengwarCount,
        characterId: roll.characterId || workingCampaign.activeCharacterId || undefined,
        characterName: roll.characterName || workingCampaign.characterSheet.identity.name,
      },
      updatedAt: Date.now(),
    };
    const summarized = attachResolutionSummary(base, updated, characters, workingCharacters, 'Rolar ' + String(roll.skillName || workingCampaign.pendingTest?.skill || 'dados'));
    const persisted = await saveMasterTransaction(summarized, workingCharacters, audits, 'resultado da rolagem aguardando confirmação', workingNpcs);
    campaignRef.current = persisted;
    setCampaign(persisted);
    setCharacters(workingCharacters);
    setAllNpcs(workingNpcs);
    setActiveTab('jogo');
    setIsStoryLoading(false);
    window.setTimeout(() => document.getElementById('pending-action-test')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 80);
  };"""
app = app[:start] + roll_handler + app[end:]

# Normal action resolution persists before/after deltas. Movement already stored its own summary.
app = req(
    app,
    """      const updated: CampaignState = {
        ...rememberedCampaign,""",
    """      let updated: CampaignState = {
        ...rememberedCampaign,""",
    "mutable turn result",
)
summary_anchor = """      if (updated.masterState && narrativeIntent.advancesFiction) {
        updated.masterState = {
          ...updated.masterState,
          memory: { ...updated.masterState.memory, campaignSummary: memory.campaignSummary },
          updatedAt: Date.now(),
        };
      }

      const finalMapDiscovery"""
summary_replacement = """      if (updated.masterState && narrativeIntent.advancesFiction) {
        updated.masterState = {
          ...updated.masterState,
          memory: { ...updated.masterState.memory, campaignSummary: memory.campaignSummary },
          updatedAt: Date.now(),
        };
      }
      if (narrativeIntent.advancesFiction && !userAction.startsWith('[MOVIMENTO DE JORNADA JÁ APLICADO]')) {
        updated = attachResolutionSummary(base, updated, characters, mechanicallyUpdatedCharacters, userAction);
      }

      const finalMapDiscovery"""
app = req(app, summary_anchor, summary_replacement, "turn summary")

# Add HUD + explicit Journey decision panel around the existing game view.
game_start = app.index("      case 'jogo': return <GameView")
game_end = app.index("\n      case 'mestre':", game_start)
game_case = app[game_start:game_end]
prefix = "      case 'jogo': return "
if not game_case.startswith(prefix):
    raise SystemExit("unexpected GameView case")
game_node = game_case[len(prefix):].strip()
if game_node.endswith(";"):
    game_node = game_node[:-1]
new_game_case = """      case 'jogo': return (
        <>
          """ + game_node + """
          <MechanicalStatusStrip masterState={masterState} characters={partyCharacters} activeCharacterId={campaign.activeCharacterId} immersive={immersiveJourneyMode} />
          <JourneyResolutionPanel
            masterState={masterState}
            characters={partyCharacters}
            busy={isStoryLoading}
            onSetEyeEnabled={handleSetEyeAwarenessEnabled}
            onStartJourney={handleJourneyStartManual}
            onRollJourney={handleJourneyRollManual}
            onApplyMarch={handleApplyJourneyMarch}
            onGenerateEvent={handleGenerateJourneyEventManual}
            onApplyEvent={handleApplyJourneyEvent}
            onContinueStage={handleContinueJourneyStage}
            onCompleteJourney={handleCompleteJourneyManual}
            onOpenEncounter={handleOpenJourneyEncounter}
            onOpenEyeRevelation={handleOpenEyeRevelation}
            onAdjustEye={handleAdjustEyeAwareness}
          />
        </>
      );"""
app = app[:game_start] + new_game_case + app[game_end:]

write(rel, app)
print("PATCH_4938_APP_OK")
