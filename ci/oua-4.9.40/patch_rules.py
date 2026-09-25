from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_rules.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Supplement state helpers.
rel="lib/rules/supplementMechanics.ts"
t=read(rel)
insert="""export function houndAvailable(sheet: CharacterSheet): boolean {
  return hasVirtue(sheet,'Cão da Floresta das Trevas') && !Boolean(sheet.other.supplementState?.houndWounded);
}

export function deadlyArcheryMakesPrepareShotSecondary(sheet: CharacterSheet, stance: string|undefined, weaponName: string|undefined): boolean {
  const weapon=norm(weaponName);
  const ordinaryBow=weapon==='arco' || weapon==='bow';
  return Boolean(hasVirtue(sheet,'Arquearia Mortal') && stance==='rearward' && ordinaryBow);
}

export function elvenLightsTargetPower(actor:{power?:number;quickSheet?:Record<string,unknown>}): number | null {
  const direct=Number(actor.power);
  if(Number.isFinite(direct) && direct>0) return Math.max(1,Math.trunc(direct));
  const quick=Number(actor.quickSheet?.power ?? actor.quickSheet?.poder ?? actor.quickSheet?.might);
  return Number.isFinite(quick) && quick>0 ? Math.max(1,Math.trunc(quick)) : null;
}

"""
anchor="export function supplementMaxHopeBonus"
t=req(t,anchor,insert+anchor,"stateful virtue helpers")
t=t.replace(
"  if(hasVirtue(sheet,'Cão da Floresta das Trevas') && context.dogAvailable!==false && (skill==='vigilancia' || skill==='cacada')) dice+=1;",
"  if(houndAvailable(sheet) && context.dogAvailable!==false && (skill==='vigilancia' || skill==='cacada')) dice+=1;",
1)
write(rel,t)

# Combat: Hound counts as another melee hero for Rearward; prepared-shot bonus
# joins Gain Ground and is consumed by command engine after the next ranged attack.
rel="lib/rules/combat.ts"
t=read(rel)
t=req(t,
"export function rearwardAllowed(state:MasterState,actorId:string,smallFolkException=false):{allowed:boolean;reason:string}{",
"export function rearwardAllowed(state:MasterState,actorId:string,smallFolkException=false,companionMeleeCount=0):{allowed:boolean;reason:string}{",
"rearward hound signature")
t=req(t,
"  const meleeOthers=heroes.filter(a=>a.actorId!==actorId&&a.stance!=='rearward').length;",
"  const meleeOthers=heroes.filter(a=>a.actorId!==actorId&&a.stance!=='rearward').length+Math.max(0,Math.trunc(Number(companionMeleeCount)||0));",
"rearward hound count")
t=req(t,
"export function setStance(state:MasterState,characterId:string,stance:CombatStance,smallFolkException=false):MasterState{",
"export function setStance(state:MasterState,characterId:string,stance:CombatStance,smallFolkException=false,companionMeleeCount=0):MasterState{",
"setStance hound signature")
t=req(t,
"    const check=rearwardAllowed(state,characterId,smallFolkException);",
"    const check=rearwardAllowed(state,characterId,smallFolkException,companionMeleeCount);",
"setStance rearward hound")
t=req(t,
"""  const gainGroundNote=(actor.stateNotes||[]).find(note=>note.startsWith('gain-ground:'));
  const gainGround=Math.max(0,Number(gainGroundNote?.split(':')[1])||0);
  const stanceModifier=context.escaping&&actor.stance==='skirmish'?0:heroAttackDiceModifier(actor);
  const count=Math.max(0,rank+stanceModifier+inspiredDice+gainGround);""",
"""  const gainGroundNote=(actor.stateNotes||[]).find(note=>note.startsWith('gain-ground:'));
  const gainGround=Math.max(0,Number(gainGroundNote?.split(':')[1])||0);
  const preparedShotNote=(actor.stateNotes||[]).find(note=>note.startsWith('prepared-shot:'));
  const preparedShot=ranged?Math.max(0,Number(preparedShotNote?.split(':')[1])||0):0;
  const stanceModifier=context.escaping&&actor.stance==='skirmish'?0:heroAttackDiceModifier(actor);
  const count=Math.max(0,rank+stanceModifier+inspiredDice+gainGround+preparedShot);""",
"prepared shot dice")
write(rel,t)

print("PATCH_4940_RULES_OK")
