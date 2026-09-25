from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text:
        raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Combat UI: Prepare Shot, Mirkwood Hound choice, actor Power, Elven Lights.
# ---------------------------------------------------------------------------
rel="components/CombatRulesPanel.tsx"
t=read(rel)
t=req(t,
"  const [gainSkill,setGainSkill]=useState<'Atletismo'|'Busca'>('Atletismo');\n",
"""  const [gainSkill,setGainSkill]=useState<'Atletismo'|'Busca'>('Atletismo');
  const [actorPower,setActorPower]=useState(1);
  const [lightOwner,setLightOwner]=useState('');
  const [lightTargets,setLightTargets]=useState<string[]>([]);
""","combat stateful UI state")
t=req(t,
"  const strider=masterState.company.rulesMode==='strider';\n",
"""  const strider=masterState.company.rulesMode==='strider';
  const pendingHound=masterState.combat.pendingHoundPiercing;
  const houndHero=pendingHound?characters.find(c=>c.id===pendingHound.targetCharacterId):undefined;
  const lightOwners=characters.filter(c=>heroes.some(a=>a.actorId===c.id)&&hasVirtue(c.sheet,'Luzes Élficas'));
  const activeLight=masterState.combat.elvenLight;
""","combat derived state")

# Put Hound decision and Elven Lights above Strider block.
anchor="""    {strider&&<div className="mt-3 rounded border border-cyan-900/70 bg-cyan-950/15 p-3">"""
extra="""    {pendingHound&&houndHero&&<div className="mt-3 rounded border-2 border-amber-600 bg-amber-950/35 p-3">
      <div className="text-xs font-black uppercase text-amber-100">Cão da Floresta · decisão obrigatória</div>
      <p className="mt-1 text-[11px] text-amber-50/80">Um Golpe Perfurante contra <strong>{houndHero.sheet.identity.name||houndHero.name}</strong> falhou na Proteção. Escolha agora: o cão recebe o Ferimento e cancela o Golpe, ou o Herói recebe o Ferimento.</p>
      <div className="mt-2 grid grid-cols-2 gap-2">
        <button disabled={busy} onClick={()=>void run([{type:'resolve_hound_piercing_choice',targetCharacterId:houndHero.id,choice:'hound',reason:'Jogador escolheu ferir o Cão da Floresta para cancelar o Golpe Perfurante.'} as MasterCommand],'Cão da Floresta recebe o Ferimento')} className="rounded border border-amber-500 px-2 py-2 text-xs">Ferir o cão · cancelar Golpe</button>
        <button disabled={busy} onClick={()=>void run([{type:'resolve_hound_piercing_choice',targetCharacterId:houndHero.id,choice:'hero',reason:'Jogador escolheu aceitar o Ferimento do Herói.'} as MasterCommand],'Herói recebe o Ferimento')} className="rounded border border-red-700 px-2 py-2 text-xs text-red-200">Aceitar Ferimento do Herói</button>
      </div>
    </div>}

    {lightOwners.length>0&&<div className="mt-3 rounded border border-violet-900/70 bg-violet-950/20 p-3">
      <div className="text-xs font-black uppercase text-violet-200">Luzes Élficas</div>
      {activeLight?.active?<div className="mt-2">
        <p className="text-[11px] text-slate-300">Luz ativa por {characters.find(c=>c.id===activeLight.ownerCharacterId)?.sheet.identity.name||'Herói'}. Observadores: {activeLight.targetActorIds.map(id=>nameOf(masterState.combat.actors.find(a=>a.actorId===id)!,characters)).join(', ')}.</p>
        <button disabled={busy} onClick={()=>void run([{type:'extinguish_elven_lights',targetCharacterId:activeLight.ownerCharacterId,reason:'Jogador extinguiu Luzes Élficas.'} as MasterCommand],'Extinguir Luzes Élficas')} className="mt-2 w-full rounded border border-violet-700 px-3 py-2 text-xs">Extinguir luz</button>
      </div>:<>
        <select value={lightOwner} onChange={e=>setLightOwner(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          <option value="">Escolher Herói...</option>{lightOwners.map(c=><option key={c.id} value={c.id}>{c.sheet.identity.name||c.name} · Sabedoria {c.sheet.attributes.wisdom}</option>)}
        </select>
        <div className="mt-2 grid gap-1">{enemies.map(actor=><label key={actor.actorId} className="flex items-center justify-between gap-2 rounded border border-slate-800 px-2 py-1 text-[11px]">
          <span><input type="checkbox" className="mr-2" checked={lightTargets.includes(actor.actorId)} onChange={e=>setLightTargets(cur=>e.target.checked?Array.from(new Set([...cur,actor.actorId])):cur.filter(id=>id!==actor.actorId))}/>{nameOf(actor,characters)}</span>
          <span className="text-slate-500">Poder {actor.power||'?'}</span>
        </label>)}</div>
        <button disabled={busy||!lightOwner||lightTargets.length===0} onClick={()=>void run([{type:'activate_elven_lights',targetCharacterId:lightOwner,targetActorIds:lightTargets,reason:'Jogador gastou Esperança para ativar Luzes Élficas.'} as MasterCommand],'Ativar Luzes Élficas')} className="mt-2 w-full rounded border border-violet-700 px-3 py-2 text-xs disabled:opacity-40">Gastar 1 Esperança · ativar</button>
      </>}
    </div>}

"""
t=req(t,anchor,extra+anchor,"combat virtue panels")

# Prepare Shot in hero attack controls.
anchor="""      <button disabled={busy||!attackerId||!targetId||!weapons.length} onClick={()=>void run([{type:'resolve_hero_attack'"""
prepare="""      <button disabled={busy||!attackerId||!weapons.length} onClick={()=>void run([{type:'prepare_shot',targetCharacterId:attackerId,weaponIndex,reason:'Herói tentou Preparar Tiro com Busca.'} as MasterCommand],'Preparar Tiro')} className="md:col-span-2 rounded border border-sky-700 px-3 py-2 text-xs text-sky-100 disabled:opacity-40">Preparar Tiro · Busca</button>
"""
t=req(t,anchor,prepare+anchor,"Prepare Shot button")

# Power config in adversary controls before resolve attack.
anchor="""        <button disabled={busy||!enemyId||!heroTarget} onClick={()=>void run([{type:'resolve_adversary_attack'"""
power="""        <label className="text-[10px] text-slate-400">Poder do adversário<input type="number" min={1} max={12} value={actorPower} onChange={e=>setActorPower(Math.max(1,Number(e.target.value)||1))} className="mt-1 w-full rounded bg-black p-2"/></label>
        <button disabled={busy||!enemyId} onClick={()=>void run([{type:'set_combat_actor_power',actorId:enemyId,power:actorPower,reason:'Poder do adversário configurado para regras que dependem de Poder.'} as MasterCommand],'Configurar Poder')} className="rounded border border-violet-800 px-2 py-2 text-xs">Salvar Poder</button>
"""
t=req(t,anchor,power+anchor,"Power controls")
write(rel,t)

# ---------------------------------------------------------------------------
# Journey UI: optional Herbal Remedies roll beside the final Travel roll.
# ---------------------------------------------------------------------------
rel="components/JourneyResolutionPanel.tsx"
t=read(rel)
t=req(t,
"import { findSkill, targetNumberForSkill } from '../lib/characterRules';",
"import { findSkill, targetNumberForSkill } from '../lib/characterRules';\nimport { hasVirtue } from '../lib/rules/supplementMechanics';",
"Journey Herbal import")
t=t.replace(
"  onCompleteJourney: () => Promise<void>;",
"  onCompleteJourney: () => Promise<void>;\n  onHerbalRecovery: (characterId: string) => Promise<void>;",
1)

# In each end-roll hero card, display Herbal result/action before closing card.
old="""                {done ? (
                  <div style={{ marginTop: 5, fontSize: 11 }}>{rollText(done.roll)} · Vigor {done.mountVigour || 0}</div>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: '90px 1fr', gap: 6, marginTop: 6 }}>"""
new="""                {done ? (
                  <div style={{ marginTop: 5, fontSize: 11 }}>{rollText(done.roll)} · Vigor {done.mountVigour || 0}</div>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: '90px 1fr', gap: 6, marginTop: 6 }}>"""
if old not in t: raise SystemExit("missing anchor: Journey end roll hero")
# Keep structure then add action after the existing conditional block via a later anchor.
herbal_anchor="""                  </div>
                )}
              </div>"""
herbal_new="""                  </div>
                )}
                {hasVirtue(hero.sheet,'Remédios Herbais') ? (
                  masterState.journey.herbalRecoveryByHero?.[hero.id]
                    ? <div style={{ marginTop:6,fontSize:11,color:'#b7e4c7' }}>Remédios Herbais: {rollText(masterState.journey.herbalRecoveryByHero?.[hero.id]?.roll)}</div>
                    : <button disabled={props.busy} onClick={()=>void props.onHerbalRecovery(hero.id)} style={{...primaryButton,marginTop:6,minHeight:36,background:'#244d39'}}>Rolar Cura · Remédios Herbais</button>
                ) : null}
              </div>"""
t=req(t,herbal_anchor,herbal_new,"Herbal Journey UI")
write(rel,t)

# ---------------------------------------------------------------------------
# Supplement UI: Hound recovery, Beauty of Stars Council, Artificer controls.
# ---------------------------------------------------------------------------
rel="components/SupplementRulesPanel.tsx"
t=read(rel)
t=req(t,
"  const [message,setMessage]=useState('');\n",
"""  const [message,setMessage]=useState('');
  const [artificerItem,setArtificerItem]=useState('');
  const [artificerSkill,setArtificerSkill]=useState<'Ofício'|'História'>('Ofício');
  const [artificerKind,setArtificerKind]=useState<'enchanted-reward'|'wondrous-artefact'>('enchanted-reward');
  const [artificerName,setArtificerName]=useState('Artefato Maravilhoso de Eregion');
""","Supplement stateful UI state")

# Derive all magical items / own weapons.
anchor="  const mode=masterState.company.rulesMode||'standard';\n"
derived="""  const mode=masterState.company.rulesMode||'standard';
  const magicalItems=characters.flatMap(owner=>owner.sheet.inventory.filter(item=>Boolean(item.magicalKind)).map(item=>({owner,item})));
  const ownWeapons=hero?.sheet.inventory.filter(item=>/(weapon|arma|sword|spear|axe|bow|espada|lança|machado|arco)/i.test(item.category+' '+item.name))||[];
"""
t=req(t,anchor,derived,"Supplement derived items")

# Insert stateful virtue controls before closing hero card.
anchor="""        {hasVirtue(hero.sheet,'Poder do Primogênito')&&masterState.activeMode==='combat'&&<button"""
extra="""        {hasVirtue(hero.sheet,'Cão da Floresta das Trevas')&&hero.sheet.other.supplementState?.houndWounded&&hero.sheet.other.supplementState?.houndRecoveryPending&&<button disabled={busy||hero.sheet.other.supplementState?.houndRecoveryFailed} onClick={()=>void run([{type:'resolve_hound_recovery',targetCharacterId:hero.id,reason:'Teste de Cura para recuperação do Cão da Floresta no início da sessão.'} as MasterCommand],'Recuperação do Cão da Floresta')} className="mt-3 w-full rounded border border-emerald-800 px-3 py-2 text-xs disabled:opacity-40">{hero.sheet.other.supplementState?.houndRecoveryFailed?'Cão indisponível até a próxima Fase de Aventura':'Rolar Cura · recuperar Cão da Floresta'}</button>}

        {hasVirtue(hero.sheet,'Beleza das Estrelas')&&masterState.activeMode==='council'&&['introduction','interaction'].includes(masterState.council.status)&&<div className="mt-3 rounded border border-violet-900/60 p-2">
          <div className="text-xs font-black text-violet-100">Beleza das Estrelas · Conselho</div>
          {masterState.council.spokespersonId!==hero.id?<button disabled={busy} onClick={()=>void run([{type:'set_council_spokesperson',targetCharacterId:hero.id,reason:'Jogador definiu este Alto Elfo como porta-voz do Conselho.'} as MasterCommand],'Definir porta-voz')} className="mt-2 w-full rounded border border-violet-700 px-2 py-2 text-xs">Tornar porta-voz</button>:<div className="mt-2 grid grid-cols-2 gap-2">
            <button disabled={busy||Boolean(masterState.council.timeLimitBonus)} onClick={()=>void run([{type:'apply_beauty_of_stars',targetCharacterId:hero.id,choice:'time-limit',reason:'Beleza das Estrelas: aumentar Limite de Tempo do Conselho em 1.'} as MasterCommand],'Beleza das Estrelas · Limite +1')} className="rounded border border-violet-700 px-2 py-2 text-xs disabled:opacity-40">Limite de Tempo +1</button>
            <button disabled={busy||Boolean(masterState.council.memoryVeil)} onClick={()=>void run([{type:'apply_beauty_of_stars',targetCharacterId:hero.id,choice:'memory-veil',reason:'Beleza das Estrelas: Mortais guardarão apenas lembrança vaga.'} as MasterCommand],'Beleza das Estrelas · véu de memória')} className="rounded border border-violet-700 px-2 py-2 text-xs disabled:opacity-40">Véu de memória · magia menor</button>
          </div>}
        </div>}

        {hasVirtue(hero.sheet,'Artífice de Eregion')&&masterState.fellowshipPhase.status==='active'&&<div className="mt-3 rounded border border-cyan-900/60 p-2">
          <div className="text-xs font-black text-cyan-100">Artífice de Eregion</div>
          {magicalItems.length>0&&<div className="mt-2 grid grid-cols-[1fr_auto] gap-2">
            <select value={artificerItem} onChange={e=>setArtificerItem(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-[11px]"><option value="">Item mágico...</option>{magicalItems.map(({owner,item})=><option key={owner.id+'_'+item.id} value={item.id}>{item.name} · {owner.sheet.identity.name||owner.name}{item.magicalIdentified?' · identificado':''}</option>)}</select>
            <select value={artificerSkill} onChange={e=>setArtificerSkill(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2 text-[11px]"><option value="Ofício">Ofício</option><option value="História">História</option></select>
            <button disabled={busy||!artificerItem} onClick={()=>void run([{type:'artificer_study_item',targetCharacterId:hero.id,inventoryItemId:artificerItem,skill:artificerSkill,reason:'Artífice de Eregion estudou um item mágico durante a Fase de Sociedade.'} as MasterCommand],'Estudar item mágico')} className="col-span-2 rounded border border-cyan-700 px-2 py-2 text-xs disabled:opacity-40">Testar {artificerSkill} · identificar item</button>
          </div>}
          {masterState.fellowshipPhase.isYule&&<div className="mt-3 border-t border-slate-800 pt-2">
            <select value={artificerKind} onChange={e=>setArtificerKind(e.target.value as any)} className="w-full rounded border border-slate-700 bg-black p-2 text-[11px]"><option value="enchanted-reward">Adicionar Recompensa Encantada a arma</option><option value="wondrous-artefact">Criar Artefato Maravilhoso</option></select>
            {artificerKind==='enchanted-reward'?<select value={artificerItem} onChange={e=>setArtificerItem(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-[11px]"><option value="">Arma...</option>{ownWeapons.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}</select>:<input value={artificerName} onChange={e=>setArtificerName(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-[11px]" />}
            <button disabled={busy||(artificerKind==='enchanted-reward'?!artificerItem:!artificerName.trim())} onClick={()=>void run([{type:'artificer_yule_craft',targetCharacterId:hero.id,kind:artificerKind,inventoryItemId:artificerKind==='enchanted-reward'?artificerItem:undefined,name:artificerKind==='wondrous-artefact'?artificerName:undefined,reason:'Artífice de Eregion usado durante Yule.'} as MasterCommand],'Artífice de Eregion · Yule')} className="mt-2 w-full rounded border border-cyan-600 px-2 py-2 text-xs disabled:opacity-40">Criar em Yule</button>
          </div>}
        </div>}

"""
t=req(t,anchor,extra+anchor,"Supplement stateful virtue controls")
write(rel,t)

# ---------------------------------------------------------------------------
# App: Journey Herbal handler + prop.
# ---------------------------------------------------------------------------
rel="App.tsx"
t=read(rel)
anchor="""  const handleCompleteJourneyManual = async () => {
    await commitJourneyAction([{type:'player_complete_journey',reason:'Jogador confirmou as rolagens finais e o encerramento da Jornada.'}], 'encerrar Jornada manualmente', 'Encerrar Jornada');
  };"""
new=anchor+"""

  const handleJourneyHerbalRecovery = async (characterId:string) => {
    await commitJourneyAction([{type:'record_journey_herbal_roll',targetCharacterId:characterId,reason:'Remédios Herbais usado no encerramento da Jornada.'}], 'Remédios Herbais no fim da Jornada', 'Remédios Herbais');
  };"""
t=req(t,anchor,new,"App Herbal handler")
t=t.replace(
"            onCompleteJourney={handleCompleteJourneyManual}\n",
"            onCompleteJourney={handleCompleteJourneyManual}\n            onHerbalRecovery={handleJourneyHerbalRecovery}\n",
1)
write(rel,t)

print("PATCH_4940_UI_OK")
