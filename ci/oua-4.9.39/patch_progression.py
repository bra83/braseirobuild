from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_progression.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Wisdom-acquired supplement virtues must change derived maxima immediately.
rel="lib/rules/progression.ts"
t=read(rel)
t=req(t,
"""  if(v==='confianca'){ next.attributes.hopeMax+=2; next.attributes.hopeCurrent=Math.min(next.attributes.hopeMax,next.attributes.hopeCurrent+2); }
  if(v==='resiliencia'){ next.attributes.enduranceMax+=2; next.attributes.enduranceCurrent=Math.min(next.attributes.enduranceMax,next.attributes.enduranceCurrent+2); }
  if(v==='destreza') next.combat.parry+=1;""",
"""  if(v==='confianca'){ next.attributes.hopeMax+=2; next.attributes.hopeCurrent=Math.min(next.attributes.hopeMax,next.attributes.hopeCurrent+2); }
  if(v==='resiliencia'){ next.attributes.enduranceMax+=2; next.attributes.enduranceCurrent=Math.min(next.attributes.enduranceMax,next.attributes.enduranceCurrent+2); }
  if(v==='beleza das estrelas'||v==='musica de estancamento'){ next.attributes.hopeMax+=1; next.attributes.hopeCurrent=Math.min(next.attributes.hopeMax,next.attributes.hopeCurrent+1); }
  if(v==='irmao para os ursos'){ next.attributes.enduranceMax+=1; next.attributes.enduranceCurrent=Math.min(next.attributes.enduranceMax,next.attributes.enduranceCurrent+1); }
  if(v==='destreza') next.combat.parry+=1;""",
"supplement virtue derived stats")
write(rel,t)

# Fellowship state tracks whether Spiritual Recovery was already resolved.
rel="lib/master/types.ts"
t=read(rel)
t=req(t,
"""  undertakingCountByHero?: Record<string, number>;
}""",
"""  undertakingCountByHero?: Record<string, number>;
  spiritualRecoveryApplied?: boolean;
}""",
"spiritual recovery state")
anchor="  | { type: 'heal_shadow_scar'; targetCharacterId: string; reason: string }"
t=req(t,anchor,anchor+"\n  | { type: 'resolve_spiritual_recovery'; amount: 1 | 2 | 3; reason: string }","spiritual recovery command")
write(rel,t)

rel="lib/master/state.ts"
t=read(rel)
t=t.replace(
"fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {}, isYule: false, skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {} },",
"fellowshipPhase: { status: 'idle', durationDays: 0, undertakings: {}, isYule: false, skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {}, spiritualRecoveryApplied: false },"
)
write(rel,t)

# Engine: explicit 1–3 Shadow recovery; High Elves can only benefit at Yule.
rel="lib/master/commands.ts"
cmd=read(rel)
cmd=cmd.replace(
"nextState.fellowshipPhase = { ...nextState.fellowshipPhase, isYule: Boolean(command.yule), skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {}, undertakings: {} };",
"nextState.fellowshipPhase = { ...nextState.fellowshipPhase, isYule: Boolean(command.yule), skillAdvances: {}, combatAdvances: {}, valorWisdomAdvance: {}, undertakingCountByHero: {}, undertakings: {}, spiritualRecoveryApplied: false };"
)
anchor="    case 'heal_shadow_scar': {"
case_block="""    case 'resolve_spiritual_recovery': {
      if (nextState.fellowshipPhase.status !== 'active') throw new Error('Recuperação Espiritual exige uma Fase de Sociedade ativa.');
      if (nextState.fellowshipPhase.spiritualRecoveryApplied) throw new Error('A Recuperação Espiritual já foi aplicada nesta Fase de Sociedade.');
      const amount = Math.max(1, Math.min(3, Math.trunc(Number(command.amount) || 1)));
      for (const characterId of partyIds) updateHero(characterId, sheet => {
        const culture = String(sheet.identity.culture || '').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'').toLowerCase();
        const highElf = /alto elfo|high elf/.test(culture);
        if (highElf && !nextState.fellowshipPhase.isYule) return sheet;
        return adjustShadowCanonical(sheet, -amount);
      });
      nextState.fellowshipPhase = { ...nextState.fellowshipPhase, spiritualRecoveryApplied: true };
      break;
    }

"""
cmd=req(cmd,anchor,case_block+anchor,"spiritual recovery case")
write(rel,cmd)

print("PATCH_4939_PROGRESSION_OK")
