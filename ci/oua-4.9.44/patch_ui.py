from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="components/TreasureIndexPanel.tsx"
t=read(rel)
t=req(
    t,
    "import { CURSES, ENCHANTED_QUALITIES, type Bane, type CurseId, type MagicalOrigin, type MagicalTreasureKind } from '../lib/rules/magicalTreasure';",
    "import { COMMON_REWARDS, CURSES, ENCHANTED_QUALITIES, type Bane, type CurseId, type FamousRewardRef, type MagicalOrigin, type MagicalTreasureKind } from '../lib/rules/magicalTreasure';",
    "mixed Reward UI imports",
)
t=req(t,
"""  const [qualities,setQualities]=useState<string[]>([]);
  const [selectedBanes,setSelectedBanes]=useState<Bane[]>([]);""",
"""  const [qualities,setQualities]=useState<string[]>([]);
  const [rewardSequence,setRewardSequence]=useState<FamousRewardRef[]>([]);
  const [selectedBanes,setSelectedBanes]=useState<Bane[]>([]);""",
"mixed Reward UI state")
t=t.replace(
"setKind(e.target.value as MagicalTreasureKind);setBlessings([]);setQualities([]);setSelectedBanes([]);",
"setKind(e.target.value as MagicalTreasureKind);setBlessings([]);setQualities([]);setRewardSequence([]);setSelectedBanes([]);",
1)

t=req(t,
"""      qualities:kind==='famous-war-gear'?qualities:[],
      baseItemName:kind==='famous-war-gear'?baseItem.trim():undefined,""",
"""      qualities:kind==='famous-war-gear'?rewardSequence.filter(ref=>ref.kind==='enchanted').map(ref=>ref.id):[],
      rewards:kind==='famous-war-gear'?[...rewardSequence]:undefined,
      baseItemName:kind==='famous-war-gear'?baseItem.trim():undefined,""",
"mixed Reward Index payload")

old="""        <div className="rounded border border-slate-800 p-2 md:col-span-2">
          <div className="text-[10px] text-slate-500">Qualidades · 1 a 3 · ordem de despertar</div>
          <div className="mt-1 grid grid-cols-2 gap-1">{ENCHANTED_QUALITIES.map(q=><label key={q.id} className="flex items-center gap-1 text-[9px]"><input type="checkbox" checked={qualities.includes(q.id)} disabled={!qualities.includes(q.id)&&qualities.length>=3} onChange={e=>setQualities(cur=>e.target.checked?[...cur,q.id]:cur.filter(x=>x!==q.id))}/>{q.name}</label>)}</div>
        </div>"""
new="""        <div className="rounded border border-slate-800 p-2 md:col-span-2">
          <div className="text-[10px] font-black text-slate-300">Recompensas da peça · 1 a 3 · ordem de despertar</div>
          <p className="mt-1 text-[9px] text-slate-500">Pode misturar Recompensas comuns e Encantadas. A primeira fica ativa quando o item é encontrado; as demais despertam exatamente nesta ordem.</p>
          <div className="mt-2 space-y-2">{rewardSequence.map((ref,index)=><div key={String(index)+'_'+ref.kind+'_'+ref.id} className="grid grid-cols-[32px_90px_1fr_auto] items-center gap-1">
            <span className="text-[10px] text-amber-300">{index+1}º</span>
            <select value={ref.kind} onChange={e=>setRewardSequence(cur=>cur.map((value,i)=>i===index?{kind:e.target.value as 'common'|'enchanted',id:e.target.value==='common'?'Afiada':'superior-keen'}:value))} className="rounded border border-slate-700 bg-black p-1 text-[9px]"><option value="common">Comum</option><option value="enchanted">Encantada</option></select>
            <select value={ref.id} onChange={e=>setRewardSequence(cur=>cur.map((value,i)=>i===index?{...value,id:e.target.value}:value))} className="rounded border border-slate-700 bg-black p-1 text-[9px]">
              {ref.kind==='common'
                ? COMMON_REWARDS.map(reward=><option key={reward.id} value={reward.id}>{reward.id}</option>)
                : ENCHANTED_QUALITIES.map(reward=><option key={reward.id} value={reward.id}>{reward.name}</option>)}
            </select>
            <button type="button" onClick={()=>setRewardSequence(cur=>cur.filter((_,i)=>i!==index))} className="rounded border border-red-950 px-2 py-1 text-[9px]">×</button>
          </div>)}</div>
          <div className="mt-2 grid grid-cols-2 gap-2">
            <button type="button" disabled={rewardSequence.length>=3} onClick={()=>setRewardSequence(cur=>[...cur,{kind:'common',id:'Afiada'}])} className="rounded border border-slate-700 px-2 py-1 text-[9px] disabled:opacity-40">+ Comum</button>
            <button type="button" disabled={rewardSequence.length>=3} onClick={()=>setRewardSequence(cur=>[...cur,{kind:'enchanted',id:'superior-keen'}])} className="rounded border border-amber-900 px-2 py-1 text-[9px] disabled:opacity-40">+ Encantada</button>
          </div>
        </div>"""
t=req(t,old,new,"ordered mixed Reward designer")

t=t.replace(
"<strong>{item.name}</strong> · {hero.sheet.identity.name||hero.name} · {item.magicalQualitiesUnlocked||0}/{item.magicalQualities?.length||0}",
"<strong>{item.name}</strong> · {hero.sheet.identity.name||hero.name} · {item.magicalRewardsUnlocked ?? item.magicalQualitiesUnlocked ?? 0}/{item.magicalRewards?.length || item.magicalQualities?.length || 0}",
1)
t=t.replace(
"disabled={busy||(item.magicalQualitiesUnlocked||0)>=(item.magicalQualities?.length||0)}",
"disabled={busy||(item.magicalRewardsUnlocked ?? item.magicalQualitiesUnlocked ?? 0)>=(item.magicalRewards?.length || item.magicalQualities?.length || 0)}",
1)

write(rel,t)
print("PATCH_4944_UI_OK")
