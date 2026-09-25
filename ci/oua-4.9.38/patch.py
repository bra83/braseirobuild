from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4938.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

# ---------------------------------------------------------------------------
# 1) APP — Journey is player-driven. Planning a route no longer starts or
# rolls the Journey automatically. Movement consumes only an already-resolved
# March allowance and leaves Events / next March / final rolls pending.
# ---------------------------------------------------------------------------
app_rel="App.tsx"
app=read(app_rel)

start=app.find("  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {")
end=app.find("\n  const handlePartyMoved = async (move: PartyMapMove) => {",start)
if start<0 or end<0:
    raise SystemExit("map discovery handler markers missing")

map_handler="""  const handleMapDiscoveryChanged = async (discovery: MapDiscovery, reason: string) => {
    const base = campaignRef.current;
    if (!base) return;

    const route = Array.isArray(discovery.plannedRouteHexIds) ? discovery.plannedRouteHexIds.filter(Boolean) : [];
    const currentMaster = normalizeMasterState(base);
    const destination = route.at(-1) || currentMaster.journey.destinationHexId || null;

    // IMPORTANT: planning a route is not consent to assign Journey roles or
    // roll dice. Keep the route ready for the explicit Preparation step.
    const synced = {
      ...base,
      masterState: {
        ...currentMaster,
        journey: {
          ...currentMaster.journey,
          plannedRouteHexIds: route,
          destinationHexId: destination,
        },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };

    const committed = await saveCanonicalTransaction({
      campaign: synced,
      mapDiscovery: discovery,
      reason: \`estado do atlas: \${reason}\`,
    });
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: discovery.campaignId, mapId: discovery.mapId, currentHexId: discovery.currentHexId },
    }));
  };
"""
app=app[:start]+map_handler+app[end:]

start=app.find("  const handlePartyMoved = async (move: PartyMapMove) => {")
end=app.find("\n\n  type PreparedSceneAudio",start)
if start<0 or end<0:
    raise SystemExit("party move handler markers missing")

move_handler="""  const handlePartyMoved = async (move: PartyMapMove) => {
    const base = campaignRef.current;
    if (!base) return;

    const beforeMaster = normalizeMasterState(base);
    const initialPlacement = !beforeMaster.position.currentHexId;
    if (!initialPlacement) {
      const activeJourney = ['planned','travelling','paused'].includes(beforeMaster.journey.status);
      const nextRouteHexId = (beforeMaster.journey.plannedRouteHexIds || [])[0] || null;
      if (!activeJourney || !nextRouteHexId) {
        throw new Error('Movimento livre bloqueado: selecione um destino, planeje a rota e faça a Preparação da Jornada.');
      }
      if (move.toHexId !== nextRouteHexId) {
        throw new Error('Movimento bloqueado: a Companhia só pode avançar para o próximo hex da rota planejada.');
      }
      if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) {
        throw new Error('Movimento bloqueado: há um Evento de Jornada aguardando resolução manual.');
      }
      if (beforeMaster.journey.awaitingEndRoll) {
        throw new Error('Movimento bloqueado: a Companhia chegou ao destino e precisa resolver o encerramento da Jornada.');
      }
      if (beforeMaster.journey.stageComplete) {
        throw new Error('Movimento bloqueado: conclua manualmente a etapa atual da Jornada.');
      }
      if (Math.max(0, Number(beforeMaster.journey.marchHexesRemaining) || 0) <= 0) {
        throw new Error('Movimento bloqueado: faça a Jogada de Marcha do Guia antes do próximo hex.');
      }
    }

    const beforeMinutes = Math.max(0, Number(beforeMaster.worldClock.elapsedMinutes) || 0);
    let workingCampaign = base;
    let workingCharacters = characters;
    let workingNpcs = allNpcs;
    let allAudits: any[] = [];

    const mechanical = applyMasterCommands(workingCampaign, workingCharacters, workingNpcs, [{
      type: 'move_party',
      toHexId: move.toHexId,
      locationName: move.locationName,
      distanceKm: move.distanceKm,
      terrain: move.terrain,
      reason: initialPlacement ? 'Posicionamento inicial confirmado pelo jogador.' : 'Avanço de Jornada autorizado pela Jogada de Marcha já resolvida.',
    }]);
    if (mechanical.rejected.length) throw new Error(mechanical.rejected.map(item => item.reason).join(' | '));
    workingCampaign = mechanical.campaign;
    workingCharacters = mechanical.characters;
    workingNpcs = mechanical.npcs;
    allAudits.push(...mechanical.audits);

    const master = normalizeMasterState(workingCampaign);
    const afterMinutes = Math.max(0, Number(master.worldClock.elapsedMinutes) || 0);
    const elapsedMinutes = Math.max(0, afterMinutes - beforeMinutes);
    const elapsedDays = Math.max(0, Math.round(elapsedMinutes / 1440));

    // Do not delete the cast when crossing a hex. The next scene must decide
    // explicitly who remains, leaves or enters. Existing presence is carried
    // into the narration context instead of being zeroed before the Master can
    // make that decision.
    const carriedNpcIds = workingNpcs.filter(npc => Boolean(npc.presentInScene)).map(npc => npc.id);
    workingCampaign = {
      ...workingCampaign,
      currentLocation: move.locationName,
      presentNpcIds: carriedNpcIds,
      masterState: {
        ...master,
        scene: {
          ...master.scene,
          kind: master.activeMode === 'journey' ? 'journey' : 'exploration',
          status: 'active',
          title: master.activeMode === 'journey' ? 'Jornada' : \`Chegada a \${move.locationName}\`,
          objective: master.activeMode === 'journey'
            ? 'Prosseguir somente após resolver as próximas exigências da Jornada'
            : 'Orientar-se no novo local',
          locationName: move.locationName,
          participantHeroIds: [...(workingCampaign.partyCharacterIds || [])],
          participantNpcIds: carriedNpcIds,
          updatedAt: Date.now(),
        },
        updatedAt: Date.now(),
      },
      updatedAt: Date.now(),
    };

    const locationEvent = makeEvent('location_changed', undefined, {
      hexId: move.toHexId, location: move.locationName, terrain: move.terrain, elapsedDays,
      marchRemaining: master.journey.marchHexesRemaining || 0,
      awaitingEventGeneration: Boolean(master.journey.awaitingEventGeneration),
      pendingEvent: master.journey.pendingEvent?.description || null,
      awaitingEndRoll: Boolean(master.journey.awaitingEndRoll),
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
      reason: 'avanço de Jornada, tempo, NPCs e ambiente confirmados atomicamente sem autorresolução',
    });

    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(reconciled.characters);
    setAllNpcs(reconciled.npcs);
    setPresentNpcs(reconciled.npcs.filter(npc => Boolean(npc.presentInScene)));
    setCurrentImage(null);
    window.dispatchEvent(new CustomEvent(MAP_DISCOVERY_CHANGED_EVENT, {
      detail: { campaignId: reconciled.discovery.campaignId, mapId: reconciled.discovery.mapId, currentHexId: reconciled.discovery.currentHexId },
    }));

    const finalMaster = normalizeMasterState(committed);
    const pendingFatigue = Object.entries(finalMaster.journey.travelFatigueByHero || {})
      .reduce((sum, [,value]) => sum + Math.max(0, Number(value) || 0), 0);
    const nextRequirement = finalMaster.journey.pendingEvent
      ? \`Resolver Evento: \${finalMaster.journey.pendingEvent.description}\`
      : finalMaster.journey.awaitingEventGeneration
        ? 'Gerar e resolver manualmente o próximo Evento de Jornada'
        : finalMaster.journey.awaitingEndRoll
          ? 'Resolver as jogadas finais e encerrar a Jornada'
          : Number(finalMaster.journey.marchHexesRemaining || 0) <= 0 && (finalMaster.journey.plannedRouteHexIds || []).length > 0
            ? 'Fazer a próxima Jogada de Marcha do Guia'
            : 'Pode avançar pela Marcha já resolvida';

    const mechanicalSummary = [
      \`Tempo: +\${elapsedMinutes} min (\${elapsedDays} dia(s))\`,
      \`Posição: \${move.toHexId}\`,
      \`Marcha restante: \${finalMaster.journey.marchHexesRemaining || 0}\`,
      \`Rota restante: \${finalMaster.journey.plannedRouteHexIds?.length || 0}\`,
      \`Fadiga de Jornada pendente: \${pendingFatigue}\`,
      \`Próximo passo: \${nextRequirement}\`,
    ].join(' | ');

    setIsStoryLoading(true);
    setStoryError(null);
    setNarrationError(null);
    await processTurn(
      \`[TRECHO DE JORNADA RESOLVIDO ATÉ O LIMITE AUTORIZADO] A Companhia chegou a \${move.locationName}. \${mechanicalSummary}. NÃO faça rolagens, NÃO atribua papéis, NÃO gere/solucione Evento, NÃO faça nova Marcha e NÃO encerre a Jornada automaticamente. Decida explicitamente quais NPCs permanecem, quais saem e quais entram na nova cena; não zere o elenco por padrão. Narre apenas o que já ocorreu e deixe claro o próximo requisito mecânico.\`,
      false,
      committed,
    );
  };"""
app=app[:start]+move_handler+app[end:]
write(app_rel,app)

# ---------------------------------------------------------------------------
# 2) MECHANICS HUB — explicit Journey Preparation, player-selected roles,
# manual March/Event sequence and manual end rolls.
# ---------------------------------------------------------------------------
hub_rel="components/MechanicsHub.tsx"
hub=read(hub_rel)

hub=req(hub,
"  const [mounted,setMounted]=useState(false);",
"""  const [mounted,setMounted]=useState(false);
  const [journeyRoles,setJourneyRoles]=useState<Partial<Record<'guide'|'scout'|'hunter'|'lookout',string>>>(()=>({...masterState.company.journeyRoles}));
  const [endRolls,setEndRolls]=useState<Record<string,{success:boolean;successIcons:number;mountVigour:number}>>({});""",
"journey local state")

hub=req(hub,
"  const planned=masterState.journey.plannedRouteHexIds||[];",
"""  const planned=masterState.journey.plannedRouteHexIds||[];
  const roleDefs=[
    ['guide','Guia','Viagem','Conduz a Companhia e realiza a Jogada de Marcha.'],
    ['scout','Batedor','Exploração','Encontra caminhos, passagens e locais adequados.'],
    ['hunter','Caçador','Caçada','Busca alimento e recursos e pode ser alvo de Eventos.'],
    ['lookout','Vigia','Vigilância','Percebe perigos, emboscadas e ameaças.'],
  ] as const;""",
"journey role definitions")

old_start="""  const startJourney=()=>run([{
    type:'start_journey',destinationHexId:planned.at(-1)||masterState.journey.destinationHexId||null,
    roles:masterState.company.journeyRoles,routeHexIds:planned,mounted,forcedMarch,
    reason:'Jornada iniciada pela rota planejada do Atlas.',
  } as MasterCommand],'iniciar Jornada');"""
new_start="""  const startJourney=()=>{
    const missing=roleDefs.filter(([role])=>!journeyRoles[role]);
    if(missing.length){setMessage('Preparação incompleta: escolha '+missing.map(([,label])=>label).join(', ')+'.');return;}
    void run([{
      type:'start_journey',destinationHexId:planned.at(-1)||masterState.journey.destinationHexId||null,
      roles:journeyRoles,routeHexIds:planned,mounted,forcedMarch,
      reason:'Jornada iniciada após Preparação e escolha explícita dos papéis pelo jogador.',
    } as MasterCommand],'iniciar Jornada');
  };"""
hub=req(hub,old_start,new_start,"manual journey start")

old_finish=re.search(r"  const finishJourney=\(\)=>\{.*?\n  \};",hub,re.S)
if not old_finish: raise SystemExit("finishJourney block missing")
new_finish="""  const rollEndForHero=(hero:Character)=>{
    const roll=rollSkill(hero,'Viagem');
    setEndRolls(current=>({...current,[hero.id]:{
      success:Boolean(roll.success),
      successIcons:Number(roll.tengwarCount)||0,
      mountVigour:mounted?Math.max(0,Number(mountVigourByHero[hero.id])||0):0,
    }}));
    setMessage('Jogada final de '+heroName(hero)+': '+(roll.success?'sucesso':'falha')+'; '+(roll.tengwarCount||0)+' ícone(s).');
  };
  const finishJourney=()=>{
    const party=characters;
    const missing=party.filter(hero=>!endRolls[hero.id]);
    if(missing.length){setMessage('Faça primeiro a jogada final de Viagem de: '+missing.map(heroName).join(', ')+'.');return;}
    void run([{type:'complete_journey',endRolls,reason:'Encerramento da Jornada após todas as jogadas finais feitas pelo jogador.'} as MasterCommand],'encerrar Jornada');
  };"""
hub=hub[:old_finish.start()]+new_finish+hub[old_finish.end():]

prep_anchor="""      <div className="mt-3 grid grid-cols-2 gap-2">
        {masterState.journey.status==='idle'||masterState.journey.status==='completed'
          ?<button disabled={busy||!planned.length} onClick={startJourney} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Iniciar pela rota</button>"""
prep_new="""      {(masterState.journey.status==='idle'||masterState.journey.status==='completed')&&(
        <div className="mt-3 rounded border border-cyan-950/80 bg-black/30 p-3">
          <div className="text-[10px] font-black uppercase tracking-wide text-cyan-300">Preparação da Jornada</div>
          <div className="mt-2 grid gap-2">
            {roleDefs.map(([role,label,skill,description])=><label key={role} className="grid gap-1 rounded border border-slate-800 p-2 text-xs">
              <span className="font-bold text-slate-100">{label} — {skill}</span>
              <span className="text-[10px] leading-relaxed text-slate-400">{description}</span>
              <select value={journeyRoles[role]||''} onChange={e=>setJourneyRoles(current=>({...current,[role]:e.target.value||undefined}))} className="rounded border border-slate-700 bg-black p-2">
                <option value="">Escolher personagem…</option>
                {characters.map(hero=><option key={hero.id} value={hero.id}>{heroName(hero)}</option>)}
              </select>
            </label>)}
          </div>
        </div>
      )}
      <div className="mt-3 grid grid-cols-2 gap-2">
        {masterState.journey.status==='idle'||masterState.journey.status==='completed'
          ?<button disabled={busy||!planned.length||roleDefs.some(([role])=>!journeyRoles[role])} onClick={startJourney} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Confirmar papéis e iniciar</button>"""
hub=req(hub,prep_anchor,prep_new,"journey preparation UI")

old_end="""        {masterState.journey.awaitingEndRoll&&<>
          {Boolean(masterState.journey.mounted)&&<div className="col-span-2 rounded border border-emerald-900/60 p-2"><div className="mb-1 text-[10px] uppercase text-emerald-300">Vigor das montarias</div>{characters.map(hero=><label key={hero.id} className="mb-1 flex items-center justify-between gap-2 text-xs"><span>{heroName(hero)}</span><input type="number" min={0} max={12} value={mountVigourByHero[hero.id]||0} onChange={e=>setMountVigourByHero(current=>({...current,[hero.id]:Math.max(0,Number(e.target.value)||0)}))} className="w-16 rounded bg-black p-1"/></label>)}</div>}
          <button disabled={busy} onClick={finishJourney} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-emerald-200">Encerrar Jornada e aplicar Fadiga restante</button>
        </>}"""
new_end="""        {masterState.journey.awaitingEndRoll&&<>
          {Boolean(masterState.journey.mounted)&&<div className="col-span-2 rounded border border-emerald-900/60 p-2"><div className="mb-1 text-[10px] uppercase text-emerald-300">Vigor das montarias</div>{characters.map(hero=><label key={hero.id} className="mb-1 flex items-center justify-between gap-2 text-xs"><span>{heroName(hero)}</span><input type="number" min={0} max={12} value={mountVigourByHero[hero.id]||0} onChange={e=>setMountVigourByHero(current=>({...current,[hero.id]:Math.max(0,Number(e.target.value)||0)}))} className="w-16 rounded bg-black p-1"/></label>)}</div>}
          <div className="col-span-2 grid gap-2 rounded border border-emerald-900/60 p-2">
            <div className="text-[10px] uppercase text-emerald-300">Jogadas finais de Viagem</div>
            {characters.map(hero=><button key={hero.id} disabled={busy||Boolean(endRolls[hero.id])} onClick={()=>rollEndForHero(hero)} className="rounded border border-emerald-800 px-2 py-2 text-xs disabled:opacity-50">
              {endRolls[hero.id]?heroName(hero)+' · '+(endRolls[hero.id].success?'sucesso':'falha')+' · '+endRolls[hero.id].successIcons+' ícone(s)':'Rolar '+heroName(hero)}
            </button>)}
          </div>
          <button disabled={busy||characters.some(hero=>!endRolls[hero.id])} onClick={finishJourney} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-emerald-200 disabled:opacity-40">Encerrar Jornada e aplicar Fadiga restante</button>
        </>}"""
hub=req(hub,old_end,new_end,"manual final travel rolls")
write(hub_rel,hub)

# ---------------------------------------------------------------------------
# 3) HUD — keep compact but expose collective mechanical Journey state.
# Full active-hero values are added only when source props make them available;
# this patch guarantees the collective state is no longer hidden.
# ---------------------------------------------------------------------------
imm_rel="components/immersive/ImmersiveJourneyView.tsx"
imm=read(imm_rel)
old_status="""        Jornada · rota {props.masterState.journey.plannedRouteHexIds?.length || 0} hex(es) · marcha {props.masterState.journey.marchHexesRemaining || 0} · fadiga pendente {Object.values(props.masterState.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + Math.max(0, Number(value) || 0), 0)} · {props.masterState.journey.pendingEvent || props.masterState.journey.awaitingEventGeneration ? 'Evento' : props.masterState.journey.awaitingEndRoll ? 'Chegada' : 'Em marcha'}"""
new_status="""        <span>Sociedade {Number((props.masterState.company as any).fellowshipPool ?? (props.masterState.company as any).fellowshipPoints ?? 0)}</span>
        <span> · Olho {props.masterState.company.eyeAwareness ?? 0}</span>
        <span> · Rota {props.masterState.journey.plannedRouteHexIds?.length || 0}</span>
        <span> · Marcha {props.masterState.journey.marchHexesRemaining || 0}</span>
        <span> · Fadiga pend. {Object.values(props.masterState.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + Math.max(0, Number(value) || 0), 0)}</span>
        <span> · {props.masterState.journey.pendingEvent ? props.masterState.journey.pendingEvent.description : props.masterState.journey.awaitingEventGeneration ? 'Evento pendente' : props.masterState.journey.awaitingEndRoll ? 'Chegada · jogadas finais' : 'Em marcha'}</span>"""
if old_status in imm:
    imm=imm.replace(old_status,new_status,1)
write(imm_rel,imm)

# ---------------------------------------------------------------------------
# 4) Version + regression gates
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.38"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.37"','"version": "4.9.38"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.37'","APP_VERSION = '4.9.38'",t)
t=re.sub(r"APP_VERSION_CODE = 49037","APP_VERSION_CODE = 49038",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"
t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49037","versionCode = 49038",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.37"','versionName = "4.9.38"',t)
write(gradle_rel,t)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel).replace("index.html?v=49037&immersive=1","index.html?v=49038&immersive=1")
write(main_rel,main)

app=read(app_rel); hub=read(hub_rel); imm=read(imm_rel); main=read(main_rel)
checks={
 "no-auto-role-assignment": "autoJourneyRoles(party)" not in app,
 "no-auto-first-march": "Primeira Jogada de Marcha automática" not in app,
 "no-auto-event-resolution": "resolvido automaticamente pelo motor local" not in app,
 "no-auto-next-march": "Nova Jogada de Marcha automática" not in app,
 "no-auto-finish": "Destino alcançado: encerrar Jornada" not in app,
 "manual-preparation": "Preparação da Jornada" in hub and "Confirmar papéis e iniciar" in hub,
 "manual-role-descriptions": "Conduz a Companhia" in hub and "Percebe perigos" in hub,
 "manual-march": "Jogada de Marcha" in hub and "resolve_journey_march" in hub,
 "manual-event": "Gerar Evento" in hub and "Resolver {event.description}" in hub,
 "manual-end-rolls": "Jogadas finais de Viagem" in hub and "rollEndForHero" in hub,
 "npc-not-cleared-on-move": "workingNpcs = workingNpcs.map(npc => ({ ...npc, presentInScene: false }))" not in app and "setPresentNpcs([])" not in app,
 "npc-carry-context": "carriedNpcIds" in app and "não zere o elenco por padrão" in app,
 "movement-gate": "Movimento livre bloqueado" in app and "marchHexesRemaining" in app,
 "pending-event-stops-move": "há um Evento de Jornada aguardando resolução manual" in app,
 "journey-hud": "Fadiga pend." in imm and "Marcha" in imm,
 "tts-preserved": "kokoroBackendPackages" in main and "Braseiro Kokoro PT-BR · neural" in main,
 "music-preserved": "scene-music-preserved-manual" in app,
 "combat-preserved": "actor.position!.x * 100" in read("lib/narrativeEngine.ts"),
 "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "version": "4.9.38" in read(version_rel) and "49038" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4938_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.38_MANUAL_JOURNEY_NPC_CONTINUITY_OK")
