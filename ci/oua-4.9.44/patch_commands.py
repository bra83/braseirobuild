from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_commands.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="lib/master/commands.ts"
cmd=read(rel)

cmd=req(
    cmd,
    "import { allowedBanes, blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, magicalBlessingDice, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
    "import { allowedBanes, blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, magicalBlessingDice, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries, validateFamousRewardSequence } from '../rules/magicalTreasure';",
    "mixed Reward import",
)

# Add/update Treasure Index validation.
cmd=req(cmd,
"""        const qualities=Array.from(new Set((entry.qualities||[]).map(String).filter(Boolean)));
        if(qualities.length<1||qualities.length>3) throw new Error('Arma/Armadura Famosa exige de 1 a 3 qualidades, na ordem de desbloqueio.');
        entry.qualities=qualities;
        entry.blessings=[];
        if(!entry.origin) throw new Error('Arma/Armadura Famosa exige Origem.');
        const banes=Array.from(new Set((entry.banes||[]).map(String).filter(Boolean)));
        if(banes.length>maxBanes(entry.origin)) throw new Error('Quantidade de Flagelos excede o permitido pela Origem.');
        const legal=new Set(allowedBanes(entry.origin));
        if(banes.some(value=>!legal.has(value as any))) throw new Error('Há Flagelo incompatível com a Origem do item.');
        entry.banes=banes;""",
"""        if(!entry.origin) throw new Error('Arma/Armadura Famosa exige Origem.');
        const banes=Array.from(new Set((entry.banes||[]).map(String).filter(Boolean)));
        if(banes.length>maxBanes(entry.origin)) throw new Error('Quantidade de Flagelos excede o permitido pela Origem.');
        const legal=new Set(allowedBanes(entry.origin));
        if(banes.some(value=>!legal.has(value as any))) throw new Error('Há Flagelo incompatível com a Origem do item.');
        entry.banes=banes;
        const rewards=(entry.rewards?.length?entry.rewards:(entry.qualities||[]).map(id=>({kind:'enchanted' as const,id:String(id)}))).map(ref=>({kind:ref.kind,id:String(ref.id)}));
        const validation=validateFamousRewardSequence(rewards,entry.origin,entry.baseItemName||'',banes as any);
        if(!validation.valid) throw new Error(validation.reason);
        entry.rewards=rewards;
        entry.qualities=rewards.filter(ref=>ref.kind==='enchanted').map(ref=>ref.id);
        entry.blessings=[];""",
"add mixed Reward validation")

cmd=req(cmd,
"""        replacement.qualities=Array.from(new Set((replacement.qualities||[]).map(String).filter(Boolean)));
        if(replacement.qualities.length<1||replacement.qualities.length>3) throw new Error('Arma/Armadura Famosa exige de 1 a 3 qualidades.');
        if(!replacement.origin) throw new Error('Arma/Armadura Famosa exige Origem.');
        replacement.banes=Array.from(new Set((replacement.banes||[]).map(String).filter(Boolean)));
        const legal=new Set(allowedBanes(replacement.origin));
        if(replacement.banes.length>maxBanes(replacement.origin)||replacement.banes.some(value=>!legal.has(value as any))) throw new Error('Flagelos incompatíveis com a Origem.');
        replacement.blessings=[];""",
"""        if(!replacement.origin) throw new Error('Arma/Armadura Famosa exige Origem.');
        replacement.banes=Array.from(new Set((replacement.banes||[]).map(String).filter(Boolean)));
        const legal=new Set(allowedBanes(replacement.origin));
        if(replacement.banes.length>maxBanes(replacement.origin)||replacement.banes.some(value=>!legal.has(value as any))) throw new Error('Flagelos incompatíveis com a Origem.');
        const rewards=(replacement.rewards?.length?replacement.rewards:(replacement.qualities||[]).map(id=>({kind:'enchanted' as const,id:String(id)}))).map(ref=>({kind:ref.kind,id:String(ref.id)}));
        const validation=validateFamousRewardSequence(rewards,replacement.origin,replacement.baseItemName||'',replacement.banes as any);
        if(!validation.valid) throw new Error(validation.reason);
        replacement.rewards=rewards;
        replacement.qualities=rewards.filter(ref=>ref.kind==='enchanted').map(ref=>ref.id);
        replacement.blessings=[];""",
"update mixed Reward validation")

# Claim copies full ordered sequence and awakens exactly the first listed Reward.
cmd=req(cmd,
"""        magicalQualities:[...(entry.qualities||[])],
        magicalQualitiesUnlocked:entry.kind==='famous-war-gear'?1:0,
        magicalIdentified:identified,""",
"""        magicalQualities:[...(entry.qualities||[])],
        magicalQualitiesUnlocked:entry.kind==='famous-war-gear' ? ((entry.rewards?.[0]?.kind==='enchanted'||!entry.rewards?.length)?1:0) : 0,
        magicalRewards:entry.kind==='famous-war-gear'
          ? [...(entry.rewards?.length?entry.rewards:(entry.qualities||[]).map(id=>({kind:'enchanted' as const,id:String(id)})))]
          : undefined,
        magicalRewardsUnlocked:entry.kind==='famous-war-gear'?1:0,
        magicalIdentified:identified,""",
"claim mixed Reward sequence")

cmd=req(cmd,
"""              magicalQualities:[...(entry.qualities||[])],
              magicalQualitiesUnlocked:1,
            }];""",
"""              magicalQualities:[...(entry.qualities||[])],
              magicalQualitiesUnlocked:(entry.rewards?.[0]?.kind==='enchanted'||!entry.rewards?.length)?1:0,
              magicalRewards:[...(entry.rewards?.length?entry.rewards:(entry.qualities||[]).map(id=>({kind:'enchanted' as const,id:String(id)})))],
              magicalRewardsUnlocked:1,
            }];""",
"weapon mixed Reward sequence")

# Treasury awakening counts the unified ordered sequence.
cmd=req(cmd,
"""      const total=(item.magicalQualities||[]).length;
      const unlocked=Math.max(0,Number(item.magicalQualitiesUnlocked)||0);
      if(unlocked>=total) throw new Error('Todas as Qualidades deste item já foram ativadas.');""",
"""      const total=(item.magicalRewards||[]).length || (item.magicalQualities||[]).length;
      const unlocked=Math.max(0,Number(item.magicalRewardsUnlocked ?? item.magicalQualitiesUnlocked)||0);
      if(unlocked>=total) throw new Error('Todas as Qualidades deste item já foram ativadas.');""",
"mixed Reward unlock total")

cmd=req(cmd,
"""          famous.magicalQualitiesUnlocked=Math.min((famous.magicalQualities||[]).length,Math.max(0,Number(famous.magicalQualitiesUnlocked)||0)+rewardTags.length);
          next.inventory=next.inventory.filter(entry=>entry.id!==sacrificed.id);
          next.combat.weapons=(next.combat.weapons||[])
            .filter(weapon=>weapon.name!==sacrificed.name)
            .map(weapon=>weapon.treasureIndexEntryId===famous.treasureIndexEntryId?{...weapon,magicalQualitiesUnlocked:famous.magicalQualitiesUnlocked}:weapon);""",
"""          const totalRewards=(famous.magicalRewards||[]).length || (famous.magicalQualities||[]).length;
          const currentRewards=Math.max(0,Number(famous.magicalRewardsUnlocked ?? famous.magicalQualitiesUnlocked)||0);
          famous.magicalRewardsUnlocked=Math.min(totalRewards,currentRewards+rewardTags.length);
          if(!famous.magicalRewards?.length) famous.magicalQualitiesUnlocked=famous.magicalRewardsUnlocked;
          next.inventory=next.inventory.filter(entry=>entry.id!==sacrificed.id);
          next.combat.weapons=(next.combat.weapons||[])
            .filter(weapon=>weapon.name!==sacrificed.name)
            .map(weapon=>weapon.treasureIndexEntryId===famous.treasureIndexEntryId?{
              ...weapon,
              magicalRewardsUnlocked:famous.magicalRewardsUnlocked,
              magicalQualitiesUnlocked:famous.magicalRewards?.length?weapon.magicalQualitiesUnlocked:famous.magicalQualitiesUnlocked,
            }:weapon);""",
"treasury mixed Reward awakening")

write(rel,cmd)
print("PATCH_4944_COMMANDS_OK")
