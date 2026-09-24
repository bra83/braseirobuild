from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4930.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

# ---------------------------------------------------------------------------
# 1) HARD CANONICAL MOVEMENT GATE IN APP
# After initial placement, the party may only move through the next hex of an
# active planned Journey. This prevents direct adjacent-hex hopping from
# bypassing March rolls, Events, elapsed days and Journey fatigue.
# ---------------------------------------------------------------------------
app_rel="App.tsx"
app=read(app_rel)

old="""    const beforeMaster = normalizeMasterState(base);
    const beforeMinutes = Math.max(0, Number(beforeMaster.worldClock.elapsedMinutes) || 0);"""
new="""    const beforeMaster = normalizeMasterState(base);
    const initialPlacement = !beforeMaster.position.currentHexId;
    if (!initialPlacement) {
      const activeJourney = ['planned','travelling','paused'].includes(beforeMaster.journey.status);
      const nextRouteHexId = (beforeMaster.journey.plannedRouteHexIds || [])[0] || null;
      if (!activeJourney || !nextRouteHexId) {
        throw new Error('Movimento livre bloqueado: selecione um destino e use Planejar rota. Depois avance a Companhia pela Jornada.');
      }
      if (move.toHexId !== nextRouteHexId) {
        throw new Error('Movimento bloqueado: a Companhia só pode avançar para o próximo hex da rota planejada.');
      }
      if (beforeMaster.journey.pendingEvent || beforeMaster.journey.awaitingEventGeneration) {
        throw new Error('Movimento bloqueado: resolva primeiro o Evento de Jornada atual.');
      }
      if (beforeMaster.journey.awaitingEndRoll) {
        throw new Error('Movimento bloqueado: a Companhia alcançou o destino e precisa encerrar a Jornada.');
      }
      if (beforeMaster.journey.stageComplete) {
        throw new Error('Movimento bloqueado: conclua a etapa atual da Jornada antes de avançar.');
      }
      if (Math.max(0, Number(beforeMaster.journey.marchHexesRemaining) || 0) <= 0) {
        throw new Error('Movimento bloqueado: a Jogada de Marcha do Guia precisa ser resolvida antes do próximo hex.');
      }
    }
    const beforeMinutes = Math.max(0, Number(beforeMaster.worldClock.elapsedMinutes) || 0);"""
app=req(app,old,new,"canonical movement guard")
write(app_rel,app)

# ---------------------------------------------------------------------------
# 2) MAP UX: remove free movement after initial placement, force route/Journey,
# and add a local in-flight lock so repeated taps cannot queue several moves.
# ---------------------------------------------------------------------------
map_rel="components/EriadorMap.tsx"
mp=read(map_rel)

state_anchor="  const [isImportingPackage, setIsImportingPackage] = useState(false);"
if "const [isMovePending" not in mp:
    mp=req(mp,state_anchor,state_anchor+"\n  const [isMovePending, setIsMovePending] = useState(false);","move pending state")

move_start="""  const moveToHex = async (target: EriadorHex) => {
    if (!target || !target.playable || !discovery) return;
    const current = discovery.currentHexId ? byId.get(discovery.currentHexId) : null;
    """
move_new="""  const moveToHex = async (target: EriadorHex) => {
    if (!target || !target.playable || !discovery) return;
    if (isMovePending) {
      setError('A Jornada ainda está resolvendo o deslocamento anterior.');
      return;
    }
    const current = discovery.currentHexId ? byId.get(discovery.currentHexId) : null;
    if (current) {
      const expectedNext = discovery.plannedRouteHexIds?.[0] || null;
      if (!expectedNext) {
        setError('Movimento livre bloqueado: selecione o destino e toque em “Planejar rota”.');
        return;
      }
      if (target.id !== expectedNext) {
        setError('Este não é o próximo hex da rota. A Companhia só avança pela Jornada planejada.');
        return;
      }
    }
    """
mp=req(mp,move_start,move_new,"moveToHex route gate")

old_try="""    try {
      await onPartyMoved({ toHexId: target.id, locationName, terrain: target.terrain, discovery: updated });
      setDiscovery(updated);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'O movimento não pôde ser gravado de forma atômica.');
      return;
    }"""
new_try="""    setIsMovePending(true);
    try {
      await onPartyMoved({ toHexId: target.id, locationName, terrain: target.terrain, discovery: updated });
      setDiscovery(updated);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'O movimento não pôde ser gravado de forma atômica.');
      return;
    } finally {
      setIsMovePending(false);
    }"""
mp=req(mp,old_try,new_try,"moveToHex in-flight lock")

old_group="""  const moveGroup = async () => {
    if (!selected) return;
    await moveToHex(selected);
  };"""
new_group="""  const moveGroup = async () => {
    if (!selected) return;
    if (discovery?.currentHexId) {
      setError('Movimento livre bloqueado: escolha o destino, planeje a rota e avance pela Jornada.');
      return;
    }
    await moveToHex(selected);
  };"""
mp=req(mp,old_group,new_group,"direct move disabled after placement")

# Immersive selected-hex controls: position once, then route-only travel.
old_immersive="""            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 7, marginTop: 9 }}>
              <button type="button" onClick={() => void moveGroup()} disabled={!selected.playable} style={{ minHeight: 44, borderRadius: 8, border: '1px solid #8a5f2f', background: '#5a3a1d', color: '#fff0cd', fontWeight: 800, opacity: selected.playable ? 1 : .45 }}>
                {discovery?.currentHexId ? 'Mover aqui' : 'Posicionar'}
              </button>
              <button type="button" onClick={() => void planRouteToSelected()} disabled={!discovery?.currentHexId || discovery.currentHexId === selected.id} style={{ minHeight: 44, borderRadius: 8, border: '1px solid rgba(195,151,85,.5)', background: 'rgba(255,255,255,.06)', color: '#ead9b7', fontWeight: 700, opacity: !discovery?.currentHexId || discovery.currentHexId === selected.id ? .4 : 1 }}>
                Planejar rota
              </button>
            </div>
            {Boolean(discovery?.plannedRouteHexIds?.length) && (
              <button type="button" onClick={() => void advancePlannedRoute()} style={{ width: '100%', minHeight: 46, marginTop: 7, borderRadius: 8, border: '1px solid rgba(82,184,203,.72)', background: 'rgba(18,86,104,.72)', color: '#e6fbff', fontWeight: 900 }}>
                Avançar 1 hex pela rota
              </button>
            )}
            {discovery?.currentHexId && (
              <div style={{ marginTop: 7, fontSize: 10, lineHeight: 1.35, opacity: .72 }}>
                Destino distante: Planejar rota → Avançar 1 hex pela rota.
              </div>
            )}"""
new_immersive="""            {!discovery?.currentHexId ? (
              <button type="button" onClick={() => void moveGroup()} disabled={!selected.playable || isMovePending} style={{ width: '100%', minHeight: 46, marginTop: 9, borderRadius: 8, border: '1px solid #8a5f2f', background: '#5a3a1d', color: '#fff0cd', fontWeight: 900, opacity: selected.playable && !isMovePending ? 1 : .45 }}>
                {isMovePending ? 'Posicionando…' : 'Posicionar Companhia'}
              </button>
            ) : (
              <button type="button" onClick={() => void planRouteToSelected()} disabled={discovery.currentHexId === selected.id || isMovePending} style={{ width: '100%', minHeight: 46, marginTop: 9, borderRadius: 8, border: '1px solid rgba(195,151,85,.5)', background: 'rgba(255,255,255,.06)', color: '#ead9b7', fontWeight: 800, opacity: discovery.currentHexId === selected.id || isMovePending ? .4 : 1 }}>
                Planejar rota até aqui
              </button>
            )}
            {Boolean(discovery?.plannedRouteHexIds?.length) && (
              <button type="button" onClick={() => void advancePlannedRoute()} disabled={isMovePending} style={{ width: '100%', minHeight: 48, marginTop: 7, borderRadius: 8, border: '1px solid rgba(82,184,203,.72)', background: 'rgba(18,86,104,.72)', color: '#e6fbff', fontWeight: 900, opacity: isMovePending ? .55 : 1 }}>
                {isMovePending ? 'Resolvendo Jornada…' : 'Avançar Jornada · próximo hex'}
              </button>
            )}
            {discovery?.currentHexId && (
              <div style={{ marginTop: 7, fontSize: 10, lineHeight: 1.35, opacity: .78 }}>
                Movimento livre bloqueado. Escolha um destino → Planejar rota → Avançar Jornada.
              </div>
            )}"""
mp=req(mp,old_immersive,new_immersive,"immersive route-only controls")

# Classic map: position button exists only before the first canonical placement.
old_classic="""                <button 
                  onClick={moveGroup} 
                  disabled={!selected.playable} 
                  className="flex items-center justify-center gap-2 rounded border border-red-800 bg-red-950/70 px-2 py-2 font-bold text-red-200 disabled:opacity-40 hover:bg-red-900/80 transition-colors"
                >
                  <Footprints className="h-3.5 w-3.5" />
                  {discovery?.currentHexId ? 'Mover aqui' : 'Posicionar'}
                </button>"""
new_classic="""                {!discovery?.currentHexId && (
                  <button 
                    onClick={moveGroup} 
                    disabled={!selected.playable || isMovePending} 
                    className="col-span-2 flex items-center justify-center gap-2 rounded border border-red-800 bg-red-950/70 px-2 py-2 font-bold text-red-200 disabled:opacity-40 hover:bg-red-900/80 transition-colors"
                  >
                    <Footprints className="h-3.5 w-3.5" />
                    {isMovePending ? 'Posicionando…' : 'Posicionar Companhia'}
                  </button>
                )}"""
mp=req(mp,old_classic,new_classic,"classic free-move button removal")

# Disable route advance while an earlier step is still resolving.
mp=mp.replace(
    """<button onClick={() => void advancePlannedRoute()} className="col-span-2 flex items-center justify-center gap-1.5 rounded border border-cyan-500 bg-cyan-900/60 px-2 py-2 font-black text-cyan-50 hover:bg-cyan-800/70">
                    <Footprints className="h-3.5 w-3.5" /> Avançar 1 hex pela rota
                  </button>""",
    """<button onClick={() => void advancePlannedRoute()} disabled={isMovePending} className="col-span-2 flex items-center justify-center gap-1.5 rounded border border-cyan-500 bg-cyan-900/60 px-2 py-2 font-black text-cyan-50 disabled:opacity-50 hover:bg-cyan-800/70">
                    <Footprints className="h-3.5 w-3.5" /> {isMovePending ? 'Resolvendo Jornada…' : 'Avançar Jornada · próximo hex'}
                  </button>""",
    1
)

# The route is now a formal Journey. Do not expose a visual "clear future route"
# that silently desynchronizes the canonical Journey state.
clear_block="""                {Boolean(discovery?.plannedRouteHexIds?.length) && (
                  <button onClick={() => void clearPlannedRoute()} className="col-span-2 rounded border border-cyan-950 px-2 py-1.5 text-cyan-500 hover:bg-cyan-950/30">
                    Limpar somente a rota futura
                  </button>
                )}"""
if clear_block in mp:
    mp=mp.replace(clear_block,"""                {Boolean(discovery?.plannedRouteHexIds?.length) && (
                  <div className="col-span-2 rounded border border-cyan-950/70 bg-cyan-950/15 px-2 py-1.5 text-[9px] leading-relaxed text-cyan-400">
                    Jornada ativa: a rota não pode ser apagada como simples marcação de mapa. Avance até o destino.
                  </div>
                )}""",1)

write(map_rel,mp)

# ---------------------------------------------------------------------------
# 3) IMMERSIVE HUD: expose Journey progress, not only calendar day.
# ---------------------------------------------------------------------------
imm_rel="components/immersive/ImmersiveJourneyView.tsx"
imm=read(imm_rel)
old_status="""        Jornada · {props.masterState.journey.day || 0} dia(s) acumulados · Marcha: {props.masterState.journey.marchHexesRemaining || 0} hex(es)"""
new_status="""        Jornada · rota {props.masterState.journey.plannedRouteHexIds?.length || 0} hex(es) · marcha {props.masterState.journey.marchHexesRemaining || 0} · fadiga pendente {Object.values(props.masterState.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + Math.max(0, Number(value) || 0), 0)} · {props.masterState.journey.pendingEvent || props.masterState.journey.awaitingEventGeneration ? 'Evento' : props.masterState.journey.awaitingEndRoll ? 'Chegada' : 'Em marcha'}"""
imm=req(imm,old_status,new_status,"immersive journey HUD")
write(imm_rel,imm)

# ---------------------------------------------------------------------------
# VERSION 4.9.30
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.30"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.29"','"version": "4.9.30"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.29'","APP_VERSION = '4.9.30'",t)
t=re.sub(r"APP_VERSION_CODE = 49029","APP_VERSION_CODE = 49030",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"
t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49029","versionCode = 49030",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.29"','versionName = "4.9.30"',t)
write(gradle_rel,t)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(main_rel).replace("index.html?v=49029&immersive=1","index.html?v=49030&immersive=1")
write(main_rel,t)

# ---------------------------------------------------------------------------
# GATES
# ---------------------------------------------------------------------------
app=read(app_rel); mp=read(map_rel); imm=read(imm_rel); main=read(main_rel)
checks={
    "canonical-route-gate": "Movimento livre bloqueado: selecione um destino e use Planejar rota" in app,
    "canonical-next-hex-gate": "a Companhia só pode avançar para o próximo hex da rota planejada" in app,
    "canonical-march-gate": "Jogada de Marcha do Guia precisa ser resolvida" in app,
    "map-no-free-move-label": "'Mover aqui'" not in mp and "? 'Mover aqui'" not in mp,
    "map-initial-position-only": "Posicionar Companhia" in mp,
    "map-route-only-guidance": "Movimento livre bloqueado. Escolha um destino" in mp,
    "map-double-tap-lock": "isMovePending" in mp and "Resolvendo Jornada…" in mp,
    "map-next-route-enforced": "target.id !== expectedNext" in mp,
    "map-clear-route-desync-removed": "Jornada ativa: a rota não pode ser apagada" in mp,
    "journey-hud-route": "Jornada · rota {props.masterState.journey.plannedRouteHexIds?.length || 0}" in imm,
    "journey-hud-fatigue": "fadiga pendente" in imm,
    "journey-mechanics-preserved": "resolve_journey_march" in app and "resolve_journey_event" in app and "complete_journey" in app,
    "tts-voice-fix-preserved": "this@MainActivity.selectTtsVoice(voiceName)" in main,
    "tts-preference-preserved": "braseiro.narrationVoiceBackend" in read("components/SettingsView.tsx"),
    "rest-handler-preserved": "const handlePartyRested = async (kind: 'short' | 'long') => {" in app,
    "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
    "version": "4.9.30" in read(version_rel) and "49030" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4930_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.30_ROUTE_LOCK_AND_JOURNEY_HUD_OK")
