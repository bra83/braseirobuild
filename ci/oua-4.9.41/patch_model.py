from pathlib import Path
import shutil, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_model.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

templates=Path(__file__).resolve().parent/"templates"
for src_rel,dst_rel in [
    ("lib/characterPaths.ts","lib/characterPaths.ts"),
    ("lib/rules/magicalTreasure.ts","lib/rules/magicalTreasure.ts"),
]:
    src=templates/src_rel; dst=root/dst_rel; dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(src,dst)

# Character sheet: persistent source trace and full magical-treasure state.
rel="lib/db.ts"
t=read(rel)
t=req(t,
"""      active: boolean;
    }>;""",
"""      active: boolean;
      magicalBaseItemName?: string;
      treasureIndexEntryId?: string;
      magicalQualities?: string[];
      magicalQualitiesUnlocked?: number;
    }>;""",
"combat weapon magical metadata")
t=req(t,
"""    supplementState?: {
      houndWounded?: boolean;""",
"""    characterPath?: {
      backgroundId: string;
      importantEventId: string;
      importantEventFeat: number;
      tnImprovementAttribute?: 'strength' | 'heart' | 'wits';
      addedFavouredSkills?: string[];
      removedFavouredSkills?: string[];
    };
    supplementState?: {
      houndWounded?: boolean;""","character path metadata")
t=req(t,
"""    magicalKind?: 'wondrous-artefact' | 'wondrous-item' | 'enchanted-reward';
    magicalQualities?: string[];
    magicalIdentified?: boolean;
    craftsmanship?: string;""",
"""    magicalBaseItemName?: string;
    magicalKind?: 'magnificent-artefact' | 'wondrous-item' | 'famous-war-gear' | 'enchanted-reward';
    magicalBlessings?: string[];
    magicalOrigin?: 'Elven' | 'Dwarven' | 'Numenorean';
    magicalBanes?: string[];
    magicalQualities?: string[];
    magicalQualitiesUnlocked?: number;
    magicalIdentified?: boolean;
    magicalCurse?: string;
    magicalCurseActive?: boolean;
    magicalCurseTrigger?: string;
    magicalCurseRemoval?: string;
    treasureIndexEntryId?: string;
    destinedCharacterId?: string;
    craftsmanship?: string;""","magical treasure inventory")
write(rel,t)

# Master state: campaign Treasure Index + unresolved discoveries.
rel="lib/master/types.ts"
t=read(rel)
t=req(t,
"  elrondItemsIdentified?: boolean;\n",
"""  elrondItemsIdentified?: boolean;
  characterPathFellowshipDelta?: number;
  characterPathEyeDelta?: number;
""",
"Character Paths company deltas")
master_anchor="export interface MasterState {\n"
treasure_types="""export interface TreasureIndexStateEntry {
  id: string;
  destinedCharacterId: string;
  name: string;
  kind: 'magnificent-artefact' | 'wondrous-item' | 'famous-war-gear';
  blessings: string[];
  origin?: 'Elven' | 'Dwarven' | 'Numenorean';
  banes?: string[];
  qualities: string[];
  baseItemName?: string;
  curse?: string;
  curseTrigger?: string;
  curseRemoval?: string;
  notes?: string;
  discovered?: boolean;
  claimed?: boolean;
}

export interface PendingMagicalTreasureDiscovery {
  id: string;
  hoardRating: 'lesser' | 'greater' | 'marvellous';
  featResult: number;
  foundOnEye: boolean;
  natureDie: number;
  kind: 'magnificent-artefact' | 'wondrous-item' | 'famous-war-gear';
  greedShadow: number;
  treasureIndexEntryId?: string;
  createdAt: number;
}

"""
t=req(t,master_anchor,treasure_types+master_anchor,"Treasure state types")
t=req(t,
"  currentSessionId: string;\n",
"""  currentSessionId: string;
  treasureIndex?: TreasureIndexStateEntry[];
  pendingMagicalTreasure?: PendingMagicalTreasureDiscovery[];
""","Master treasure state")

# Commands.
anchor="  | { type: 'artificer_yule_craft'; targetCharacterId: string; kind: 'enchanted-reward' | 'wondrous-artefact'; inventoryItemId?: string; name?: string; reason: string }"
extra="""
  | { type: 'add_treasure_index_entry'; entry: TreasureIndexStateEntry; reason: string }
  | { type: 'update_treasure_index_entry'; entry: TreasureIndexStateEntry; reason: string }
  | { type: 'roll_magical_treasure'; rating: 'lesser' | 'greater' | 'marvellous'; reason: string }
  | { type: 'claim_magical_treasure'; discoveryId: string; treasureIndexEntryId: string; targetCharacterId: string; reason: string }
  | { type: 'identify_magical_item'; targetCharacterId: string; inventoryItemId: string; reason: string }
  | { type: 'unlock_famous_quality'; targetCharacterId: string; inventoryItemId: string; source: 'valor' | 'treasury'; sacrificedInventoryItemId?: string; reason: string }
  | { type: 'remove_magical_curse'; targetCharacterId: string; inventoryItemId: string; reason: string }"""
t=req(t,anchor,anchor+extra,"Treasure commands")
write(rel,t)

# Defaults + old-save normalization.
rel="lib/master/state.ts"
t=read(rel)
t=req(
    t,
    "    currentSessionId: `session_${campaign.id}_${now}`,\n",
    "    currentSessionId: `session_${campaign.id}_${now}`,\n    treasureIndex: [],\n    pendingMagicalTreasure: [],\n",
    "Treasure defaults",
)
t=req(
    t,
    "    schemaVersion: 3,\n    updatedAt: incoming.updatedAt || Date.now(),",
    "    treasureIndex: Array.isArray(incoming.treasureIndex) ? incoming.treasureIndex : [],\n    pendingMagicalTreasure: Array.isArray(incoming.pendingMagicalTreasure) ? incoming.pendingMagicalTreasure : [],\n    schemaVersion: 3,\n    updatedAt: incoming.updatedAt || Date.now(),",
    "Treasure normalization",
)
write(rel,t)

print("PATCH_4941_MODEL_OK")
