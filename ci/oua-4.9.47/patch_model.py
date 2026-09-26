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

rel="lib/master/types.ts"
t=read(rel)

interfaces=r"""
export interface CompanyMountState {
  id: string;
  sourceCharacterId: string;
  riderCharacterId?: string;
  vigour: 1|2|3|4;
  description: string;
  treasureLoad: number;
  updatedAt: number;
}

export interface PatronMissionState {
  id: string;
  patronId: 'balin'|'bilbo'|'cirdan'|'gandalf'|'gilraen'|'tom-goldberry';
  die: number;
  prompt: string;
  status: 'generated'|'accepted'|'completed'|'abandoned';
  generatedAt: number;
  acceptedAt?: number;
  completedAt?: number;
  acceptMilestoneAwarded?: boolean;
  completeMilestoneAwarded?: boolean;
}

export interface SkillChallengeFailureState {
  targetCharacterId: string;
  risk: 'standard'|'dangerous'|'foolhardy';
  rollId: string;
  createdAt: number;
}

export interface SkillChallengeState {
  id: string;
  objective: string;
  difficulty: 'simple'|'arduous'|'challenging';
  resistance: 3|6|9;
  timeMode: 'short'|'enough'|'plenty'|'none';
  timeLimit: number|null;
  risk: 'standard'|'dangerous'|'foolhardy';
  successes: number;
  attempts: number;
  status: 'idle'|'active'|'success'|'failed'|'abandoned';
  pendingFailure?: SkillChallengeFailureState|null;
  log: Array<{
    id:string;
    characterId:string;
    skill:string;
    success:boolean;
    tengwar:number;
    createdAt:number;
    note?:string;
  }>;
  startedAt: number;
  resolvedAt?: number;
}

"""
anchor="export type MasterCommand ="
if interfaces.strip() not in t:
    t=req(t,anchor,interfaces+anchor,"core47 interfaces")

t=req(
    t,
    "  elrondItemsIdentified?: boolean;\n",
    """  elrondItemsIdentified?: boolean;
  mounts?: CompanyMountState[];
  basePatronFellowshipBonus?: number;
  patronMission?: PatronMissionState|null;
  patronRumourPending?: boolean;
  patronInterventionPending?: boolean;
  patronGandalfShadowFavouredHeroId?: string;
""",
    "company core47 state",
)

t=req(
    t,
    "  pendingEvent?: {\n",
    "  oldArnor?: boolean;\n  pendingEvent?: {\n",
    "journey old Arnor flag",
)

t=req(
    t,
    "  currentSessionId: string;\n  updatedAt: number;",
    "  currentSessionId: string;\n  skillChallenge?: SkillChallengeState;\n  updatedAt: number;",
    "master skill challenge state",
)

command_anchor="  | { type: 'create_heir_character'; targetCharacterId: string; inheritedItemIds?: string[]; reason: string }"
extra=r"""
  | { type: 'sync_company_mounts'; reason: string }
  | { type: 'assign_mount'; mountId: string; riderCharacterId?: string; reason: string }
  | { type: 'set_mount_treasure_load'; mountId: string; treasureLoad: number; reason: string }
  | { type: 'start_skill_challenge'; objective: string; difficulty: 'simple'|'arduous'|'challenging'; timeMode: 'short'|'enough'|'plenty'|'none'; risk: 'standard'|'dangerous'|'foolhardy'; reason: string }
  | { type: 'roll_skill_challenge'; targetCharacterId: string; skill: string; spendHope?: boolean; inspired?: boolean; reason: string }
  | { type: 'resolve_skill_challenge_failure'; targetCharacterId: string; consequence: 'delay'|'misfortune'|'disaster'|'moderate-damage'|'severe-damage'|'grave-damage'; note?: string; reason: string }
  | { type: 'abandon_skill_challenge'; reason: string }
  | { type: 'generate_patron_mission'; die?: number; reason: string }
  | { type: 'accept_patron_mission'; reason: string }
  | { type: 'complete_patron_mission'; reason: string }
  | { type: 'abandon_patron_mission'; reason: string }
  | { type: 'resolve_surprise'; mode: 'heroes-ambushed'|'heroes-ambush'; automatic?: boolean; participantHeroIds?: string[]; reason: string }
  | { type: 'resolve_brawl_attack'; attackerId: string; targetActorId: string; weapon: 'unarmed'|'dagger'|'cudgel'|'club'|'improvised'; spendHope?: boolean; inspired?: boolean; reason: string }
  | { type: 'use_patron_advantage'; kind: 'balin-combat-favoured'|'gandalf-shadow-favoured'|'tom-intervention'; targetCharacterId?: string; confirmInTomLand?: boolean; reason: string }
  | { type: 'set_journey_old_arnor'; value: boolean; reason: string }"""
t=req(t,command_anchor,command_anchor+extra,"core47 commands")
write(rel,t)

print("PATCH_4947_MODEL_OK")
