from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Combat UI: adversary Attribute Level/Banes, Heavy stand-up, Cleaving follow-up.
# ---------------------------------------------------------------------------
rel="components/CombatRulesPanel.tsx"
t=read(rel)
t=req(t,
"""  const [actorPower,setActorPower]=useState(1);
  const [lightOwner,setLightOwner]=useState('');""",
"""  const [actorPower,setActorPower]=useState(1);
  const [actorAttributeLevel,setActorAttributeLevel]=useState(1);
  const [actorBanes,setActorBanes]=useState<string[]>([]);
  const [cleaveTargetByHero,setCleaveTargetByHero]=useState<Record<string,string>>({});
  const [lightOwner,setLightOwner]=useState('');""",
"combat magical gear UI state")

t=req(t,
"""  const activeLight=masterState.combat.elvenLight;
""",
"""  const activeLight=masterState.combat.elvenLight;
  const cleavingReady=Object.entries(masterState.combat.cleavingReadyByHero||{}).filter((entry):entry is [string,NonNullable<typeof entry[1]>]=>Boolean(entry[1]));
  const knockedDown=actors.filter(actor=>(actor.stateNotes||[]).includes('knocked-down'));
  const baneOptions=['Orcs','Trolls','Wolves','Evil Men','Undead','Spiders'] as const;
""",
"combat magical gear derived state")

# Keep local metadata in sync when choosing a different enemy.
t=req(t,
"""        <select value={enemyId} onChange={e=>setEnemyId(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-xs">{enemies.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>""",
"""        <select value={enemyId} onChange={e=>{const id=e.target.value;setEnemyId(id);const chosen=enemies.find(a=>a.actorId===id);setActorPower(Math.max(1,Number(chosen?.power)||1));setActorAttributeLevel(Math.max(1,Number(chosen?.attributeLevel)||1));setActorBanes([...(chosen?.baneTags||[])]);}} className="rounded border border-slate-700 bg-black p-2 text-xs">{enemies.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>""",
"enemy metadata sync")

anchor="""        <button disabled={busy||!enemyId} onClick={()=>void run([{type:'set_combat_actor_power',actorId:enemyId,power:actorPower,reason:'Poder do adversário configurado para regras que dependem de Poder.'} as MasterCommand],'Configurar Poder')} className="rounded border border-violet-800 px-2 py-2 text-xs">Salvar Poder</button>
"""
extra="""        <label className="text-[10px] text-slate-400">Nível de Atributo<input type="number" min={1} max={12} value={actorAttributeLevel} onChange={e=>setActorAttributeLevel(Math.max(1,Number(e.target.value)||1))} className="mt-1 w-full rounded bg-black p-2"/></label>
        <button disabled={busy||!enemyId} onClick={()=>void run([{type:'set_combat_actor_attribute_level',actorId:enemyId,attributeLevel:actorAttributeLevel,reason:'Nível de Atributo configurado para Pesada e outras regras de equipamento famoso.'} as MasterCommand],'Configurar Nível de Atributo')} className="rounded border border-violet-800 px-2 py-2 text-xs">Salvar Nível</button>
        <div className="col-span-2 rounded border border-slate-800 p-2">
          <div className="text-[10px] text-slate-400">Categorias de Flagelo do adversário</div>
          <div className="mt-1 flex flex-wrap gap-2">{baneOptions.map(tag=><label key={tag} className="text-[10px]"><input className="mr-1" type="checkbox" checked={actorBanes.includes(tag)} onChange={e=>setActorBanes(cur=>e.target.checked?Array.from(new Set([...cur,tag])):cur.filter(value=>value!==tag))}/>{tag}</label>)}</div>
          <button disabled={busy||!enemyId} onClick={()=>void run([{type:'set_combat_actor_banes',actorId:enemyId,baneTags:actorBanes as any,reason:'Categorias de Flagelo configuradas explicitamente para efeitos de Tesouro Mágico.'} as MasterCommand],'Salvar categorias de Flagelo')} className="mt-2 w-full rounded border border-yellow-800 px-2 py-2 text-[10px]">Salvar Flagelos</button>
        </div>
"""
t=req(t,anchor,anchor+extra,"adversary magical gear metadata controls")

# Insert pending immediate actions before First Aid.
anchor="""    <div className="mt-4 border-t border-slate-800 pt-3">
      <h3 className="text-xs font-bold uppercase text-emerald-200">Primeiros Socorros</h3>"""
extra="""    {(cleavingReady.length>0||knockedDown.length>0)&&<div className="mt-4 border-t border-slate-800 pt-3">
      <h3 className="text-xs font-bold uppercase text-yellow-200">Efeitos de Equipamento Famoso pendentes</h3>
      {cleavingReady.map(([heroId,ready])=>{
        const actor=actors.find(a=>a.actorId===heroId);
        const valid=(actor?.engagedWith||[]).map(id=>actors.find(a=>a.actorId===id)).filter((a):a is CombatActorState=>Boolean(a&&!a.defeated));
        const current=cleaveTargetByHero[heroId]||valid[0]?.actorId||'';
        return <div key={heroId} className="mt-2 rounded border border-yellow-900/60 p-2 text-[11px]">
          <strong>Perfuradora · {nameOf(actor!,characters)}</strong>
          <p className="mt-1 text-slate-400">O ataque que derrotou o primeiro inimigo liberou um ataque imediato contra outro adversário já engajado.</p>
          <div className="mt-2 grid grid-cols-[1fr_auto] gap-2">
            <select value={current} onChange={e=>setCleaveTargetByHero(cur=>({...cur,[heroId]:e.target.value}))} className="rounded border border-slate-700 bg-black p-2">{valid.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>
            <button disabled={busy||!current} onClick={()=>void run([{type:'resolve_cleaving_attack',attackerId:heroId,targetActorId:current,reason:'Ataque imediato concedido por Perfuradora.'} as MasterCommand],'Perfuradora · ataque imediato')} className="rounded border border-yellow-700 px-3 py-2 disabled:opacity-40">Atacar</button>
          </div>
        </div>;
      })}
      {knockedDown.map(actor=><div key={actor.actorId} className="mt-2 flex items-center justify-between gap-2 rounded border border-orange-900/60 p-2 text-[11px]">
        <span><strong>{nameOf(actor,characters)}</strong> está derrubado por Pesada e deve gastar a próxima ação principal para levantar.</span>
        <button disabled={busy} onClick={()=>void run([{type:'stand_up_combat_actor',actorId:actor.actorId,reason:'Combatente gastou a ação principal para se levantar após Pesada.'} as MasterCommand],'Levantar após Pesada')} className="rounded border border-orange-700 px-2 py-2">Levantar</button>
      </div>)}
    </div>}

"""
t=req(t,anchor,extra+anchor,"Cleaving Heavy UI")
write(rel,t)

# ---------------------------------------------------------------------------
# Treasure Index: cursed-item design + runtime controls.
# ---------------------------------------------------------------------------
rel="components/TreasureIndexPanel.tsx"
t=read(rel)
t=req(t,
"import { ENCHANTED_QUALITIES, type Bane, type MagicalOrigin, type MagicalTreasureKind } from '../lib/rules/magicalTreasure';",
"import { CURSES, ENCHANTED_QUALITIES, type Bane, type CurseId, type MagicalOrigin, type MagicalTreasureKind } from '../lib/rules/magicalTreasure';",
"curse UI import")
t=req(t,
"""  const [selectedBanes,setSelectedBanes]=useState<Bane[]>([]);
  const [rating,setRating]""",
"""  const [selectedBanes,setSelectedBanes]=useState<Bane[]>([]);
  const [curse,setCurse]=useState<''|CurseId>('');
  const [curseTrigger,setCurseTrigger]=useState('');
  const [curseRemoval,setCurseRemoval]=useState('');
  const [curseWeakening,setCurseWeakening]=useState<Record<string,'strength'|'heart'|'wits'>>({});
  const [curseHunter,setCurseHunter]=useState<Record<string,string>>({});
  const [curseRevealed,setCurseRevealed]=useState<Record<string,boolean>>({});
  const [curseOwnerPresent,setCurseOwnerPresent]=useState<Record<string,boolean>>({});
  const [rating,setRating]""",
"curse UI state")

t=req(t,
"""      baseItemName:kind==='famous-war-gear'?baseItem.trim():undefined,
      discovered:false,claimed:false,""",
"""      baseItemName:kind==='famous-war-gear'?baseItem.trim():undefined,
      curse:curse||undefined,
      curseTrigger:curse?curseTrigger.trim()||undefined:undefined,
      curseRemoval:curse?curseRemoval.trim()||undefined:undefined,
      discovered:false,claimed:false,""",
"Treasure Index curse fields")

# Curse design fields before Add button.
anchor="""    </div>
    <button disabled={busy||!name.trim()||!destined}"""
extra="""      <div className="rounded border border-red-950/70 p-2 md:col-span-2">
        <div className="text-[10px] font-black uppercase text-red-300">Maldição opcional do item</div>
        <select value={curse} onChange={e=>setCurse(e.target.value as any)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="">Sem Maldição</option>{CURSES.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}</select>
        {curse&&<div className="mt-2 grid gap-2 md:grid-cols-2">
          <input value={curseTrigger} onChange={e=>setCurseTrigger(e.target.value)} placeholder="Circunstância que ativa a Maldição" className="rounded border border-slate-700 bg-black p-2 text-[10px]"/>
          <input value={curseRemoval} onChange={e=>setCurseRemoval(e.target.value)} placeholder="Como a Maldição pode ser desfeita" className="rounded border border-slate-700 bg-black p-2 text-[10px]"/>
        </div>}
      </div>
    </div>
    <button disabled={busy||!name.trim()||!destined}"""
t=req(t,anchor,extra,"curse design controls")

# Magical runtime items.
t=req(t,
"""  const famousItems=useMemo(()=>characters.flatMap(hero=>hero.sheet.inventory.filter(item=>item.magicalKind==='famous-war-gear').map(item=>({hero,item}))),[characters]);""",
"""  const famousItems=useMemo(()=>characters.flatMap(hero=>hero.sheet.inventory.filter(item=>item.magicalKind==='famous-war-gear').map(item=>({hero,item}))),[characters]);
  const cursedItems=useMemo(()=>characters.flatMap(hero=>hero.sheet.inventory.filter(item=>Boolean(item.magicalCurse)).map(item=>({hero,item}))),[characters]);""",
"cursed items derived")

anchor="""    {famousItems.length>0&&<div className="mt-4 border-t border-slate-800 pt-3">"""
curses_ui="""    {cursedItems.length>0&&<div className="mt-4 border-t border-red-950/70 pt-3">
      <div className="text-xs font-black text-red-200">Itens Amaldiçoados</div>
      <p className="mt-1 text-[10px] text-slate-400">O Historiador ativa a Maldição quando sua circunstância ocorre. Remover é permanente e deve ser usado apenas após a aventura/condição definida para quebrá-la.</p>
      <div className="mt-2 space-y-2">{cursedItems.map(({hero,item})=>{
        const active=Boolean(item.magicalCurseActive);
        const weakening=curseWeakening[item.id]||'strength';
        const hunted=curseHunter[item.id]??item.magicalCurseHunterTag??'';
        const revealed=curseRevealed[item.id]??Boolean(item.magicalCurseRevealed);
        const ownerPresent=curseOwnerPresent[item.id]??Boolean(item.magicalCurseOwnerPresent);
        return <div key={item.id} className="rounded border border-red-950/70 p-2 text-[10px]">
          <div><strong>{item.name}</strong> · {hero.sheet.identity.name||hero.name} · {CURSES.find(c=>c.id===item.magicalCurse)?.name||item.magicalCurse} · {active?'ATIVA':'inativa'}</div>
          {item.magicalCurseTrigger&&<div className="mt-1 text-slate-500">Gatilho: {item.magicalCurseTrigger}</div>}
          {item.magicalCurseRemoval&&<div className="text-slate-500">Remoção: {item.magicalCurseRemoval}</div>}
          {item.magicalCurse==='weakening'&&<select value={weakening} onChange={e=>setCurseWeakening(cur=>({...cur,[item.id]:e.target.value as any}))} className="mt-2 w-full rounded border border-slate-700 bg-black p-1"><option value="strength">Força</option><option value="heart">Coração</option><option value="wits">Astúcia</option></select>}
          {item.magicalCurse==='hunted'&&<input value={hunted} onChange={e=>setCurseHunter(cur=>({...cur,[item.id]:e.target.value}))} placeholder="Tipo de inimigo que detecta o item" className="mt-2 w-full rounded border border-slate-700 bg-black p-1"/>}
          {item.magicalCurse==='darkening'&&<label className="mt-2 flex items-center gap-2"><input type="checkbox" checked={revealed} onChange={e=>setCurseRevealed(cur=>({...cur,[item.id]:e.target.checked}))}/> Item revelado · Escurecer aplica −1d às jogadas apropriadas</label>}
          {item.magicalCurse==='ownership'&&<label className="mt-2 flex items-center gap-2"><input type="checkbox" checked={ownerPresent} onChange={e=>setCurseOwnerPresent(cur=>({...cur,[item.id]:e.target.checked}))}/> Proprietário verdadeiro presente · características especiais ficam ineficazes</label>}
          <div className="mt-2 grid grid-cols-2 gap-2">
            <button disabled={busy} onClick={()=>void run([{type:'set_magical_curse_state',targetCharacterId:hero.id,inventoryItemId:item.id,active:!active,revealed,ownerPresent,weakeningAttribute:item.magicalCurse==='weakening'?weakening:undefined,hunterTag:item.magicalCurse==='hunted'?hunted:undefined,reason:active?'Historiador desativou a Maldição porque sua condição deixou de se aplicar.':'Circunstância da Maldição ocorreu; efeito ativado.'} as MasterCommand],active?'desativar Maldição':'ativar Maldição')} className="rounded border border-red-800 px-2 py-2">{active?'Desativar efeito':'Ativar Maldição'}</button>
            <button disabled={busy} onClick={()=>void run([{type:'remove_magical_curse',targetCharacterId:hero.id,inventoryItemId:item.id,reason:'Condição narrativa para desfazer a Maldição foi cumprida.'} as MasterCommand],'remover Maldição permanentemente')} className="rounded border border-emerald-900 px-2 py-2 text-emerald-200">Quebrar Maldição</button>
          </div>
          {item.magicalCurseWeaknessFlaw&&<div className="mt-2 text-red-300">Falha temporária: {item.magicalCurseWeaknessFlaw}</div>}
        </div>;
      })}</div>
    </div>}

"""
t=req(t,anchor,curses_ui+anchor,"curse runtime controls")
write(rel,t)

print("PATCH_4942_UI_OK")
