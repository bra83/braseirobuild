from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_model.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="lib/master/types.ts"
t=read(rel)

anchor="export interface MasterState {\n"
types="""export type SkillSpecialSuccessChoice =
  | 'cancel-failure'
  | 'additional-success'
  | 'information'
  | 'silent'
  | 'hurry'
  | 'influence';

export interface SkillTestSessionRoll {
  characterId: string;
  skill: string;
  roll: unknown;
  success: boolean;
  successIcons: number;
  iconsSpent: number;
  supporterCharacterId?: string;
  supporterSkill?: string;
  supportHopeSpent?: boolean;
  ownHopeSpent?: boolean;
  magicalSuccess?: boolean;
  cancelledFailureBy?: string;
}

export interface SkillTestSession {
  id: string;
  label: string;
  participants: string[];
  requiredSuccesses?: number;
  rolls: Record<string,SkillTestSessionRoll>;
  additionalSuccesses: number;
  informationDiscoveries: number;
  silent: boolean;
  timeMultiplier: number;
  additionalTargetGroups: number;
  effects: Array<{
    choice: SkillSpecialSuccessChoice;
    sourceCharacterId: string;
    targetCharacterId?: string;
    note?: string;
    createdAt: number;
  }>;
  status: 'active'|'resolved';
  createdAt: number;
  updatedAt: number;
}

"""
t=req(t,anchor,types+anchor,"skill-test session types")

t=req(t,
"  pendingMagicalTreasure?: PendingMagicalTreasureDiscovery[];\n",
"  pendingMagicalTreasure?: PendingMagicalTreasureDiscovery[];\n  skillTestSession?: SkillTestSession | null;\n",
"skill-test MasterState field")

cmd_anchor="  | { type: 'resolve_lossoth_safe_refuge_rest'; reason: string }"
extra="""
  | { type: 'start_skill_test_session'; label: string; participants: string[]; requiredSuccesses?: number; reason: string }
  | { type: 'roll_skill_test_session'; targetCharacterId: string; skill: string; supporterCharacterId?: string; supporterSkill?: string; spendHope?: boolean; useMagicalSuccess?: boolean; reason: string }
  | { type: 'spend_skill_success_icon'; sourceCharacterId: string; choice: SkillSpecialSuccessChoice; targetCharacterId?: string; note?: string; reason: string }
  | { type: 'end_skill_test_session'; reason: string }"""
t=req(t,cmd_anchor,cmd_anchor+extra,"skill-test commands")
write(rel,t)

rel="lib/master/state.ts"
t=read(rel)
t=req(
    t,
    "    pendingMagicalTreasure: [],\n",
    "    pendingMagicalTreasure: [],\n    skillTestSession: null,\n",
    "skill-test default",
)
t=req(
    t,
    "    pendingMagicalTreasure: Array.isArray(incoming.pendingMagicalTreasure) ? incoming.pendingMagicalTreasure : [],\n",
    "    pendingMagicalTreasure: Array.isArray(incoming.pendingMagicalTreasure) ? incoming.pendingMagicalTreasure : [],\n    skillTestSession: incoming.skillTestSession || null,\n",
    "skill-test normalization",
)
write(rel,t)

print("PATCH_4945_MODEL_OK")
