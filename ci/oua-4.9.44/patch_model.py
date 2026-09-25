from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_model.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="lib/db.ts"
t=read(rel)
t=req(t,
"""      magicalQualities?: string[];
      magicalQualitiesUnlocked?: number;
    }>;""",
"""      magicalQualities?: string[];
      magicalQualitiesUnlocked?: number;
      magicalRewards?: Array<{ kind:'common'|'enchanted'; id:string }>;
      magicalRewardsUnlocked?: number;
    }>;""",
"weapon mixed rewards")
t=req(t,
"""    magicalQualities?: string[];
    magicalQualitiesUnlocked?: number;
    magicalIdentified?: boolean;""",
"""    magicalQualities?: string[];
    magicalQualitiesUnlocked?: number;
    magicalRewards?: Array<{ kind:'common'|'enchanted'; id:string }>;
    magicalRewardsUnlocked?: number;
    magicalIdentified?: boolean;""",
"inventory mixed rewards")
write(rel,t)

rel="lib/master/types.ts"
t=read(rel)
t=req(t,
"""  qualities: string[];
  baseItemName?: string;""",
"""  qualities: string[];
  rewards?: Array<{ kind:'common'|'enchanted'; id:string }>;
  baseItemName?: string;""",
"index mixed rewards")
write(rel,t)

print("PATCH_4944_MODEL_OK")
