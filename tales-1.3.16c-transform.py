from pathlib import Path
import re, sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])

# Preserve the green 1.3.16b profile foundation.
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16b-transform.py')}
code=(repo/'tales-1.3.16b-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def one(text,old,new,label):
    if old not in text: raise SystemExit('missing anchor: '+label)
    return text.replace(old,new,1)

rel='app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt'; s=read(rel)

# A Flood Teen that becomes Broken must record a Scar before recovery can erase Broken.
s=one(s,
'''  val removedFromPlay: Boolean = false,
  val items: List<KidItemState> = emptyList(),''',
'''  val removedFromPlay: Boolean = false,
  val scarPending: Boolean = false,
  val items: List<KidItemState> = emptyList(),''','scar pending field')

s=one(s,
'''  data object UseShame : GameEvent
  data class ApplyCondition(val condition: Condition) : GameEvent''',
'''  data object UseShame : GameEvent
  data class RecordFloodScar(val text: String, val survivalRoll: Int? = null) : GameEvent
  data class UseScar(val index: Int) : GameEvent
  data class HealAcceptedScar(val index: Int, val groupApproved: Boolean) : GameEvent
  data class SetFriction(val text: String) : GameEvent
  data class ResolveFriction(val troubleOccurred: Boolean) : GameEvent
  data class ApplyCondition(val condition: Condition) : GameEvent''','Flood events')

# Pushing into Broken creates a pending Scar in Flood.
s=one(s,
'''              event.condition == null && state.kid.conditions.size == 4 ->
                state.kid.copy(broken = true)''',
'''              event.condition == null && state.kid.conditions.size == 4 ->
                state.kid.copy(broken = true, scarPending = state.rulesProfile == RulesProfile.FLOOD_TEENS || state.kid.scarPending)''','push broken scar')

# Insert Flood Scar/Friction mechanics after Shame and before generic Conditions.
anchor='''      is GameEvent.ApplyCondition -> {
        if (state.kid.broken) fail("Kid já está Quebrado.")
        else if (event.condition in state.kid.conditions) fail("Condição já ativa.")
        else if (state.kid.conditions.size >= 4) ok(state.copy(kid = state.kid.copy(broken = true)))
        else ok(state.copy(kid = state.kid.copy(conditions = state.kid.conditions + event.condition)))
      }

      GameEvent.BecomeBroken -> if (state.kid.broken) fail("Kid já está Quebrado.") else ok(state.copy(kid = state.kid.copy(broken = true)))

      GameEvent.HealAllConditions -> ok(state.copy(kid = state.kid.copy(conditions = emptySet(), broken = false)))'''
replacement='''      is GameEvent.RecordFloodScar -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Cicatrizes são exclusivas de Things from the Flood.")
        else if (!state.kid.broken || !state.kid.scarPending) fail("Não há Cicatriz pendente para registrar.")
        else if (event.text.trim().isBlank()) fail("Descreva a Cicatriz em uma frase.")
        else {
          val nextCount = state.kid.scars.size + 1
          val roll = event.survivalRoll
          val invalidRoll = nextCount in 2..5 && roll !in 1..6
          if (invalidRoll) fail("Da segunda à quinta Cicatriz é obrigatório rolar 1d6 para permanência do Teen.")
          else {
            val removed = nextCount >= 6 || (nextCount >= 2 && (roll ?: 6) < nextCount)
            val scar = FloodScarState(event.text.trim().take(500), accepted=false, usedForSuccess=false)
            ok(state.copy(kid=state.kid.copy(
              scars=state.kid.scars+scar,
              scarPending=false,
              removedFromPlay=removed
            )))
          }
        }
      }

      is GameEvent.UseScar -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Cicatrizes são exclusivas de Things from the Flood.")
        else {
          val result=state.testResolution ?: return fail("Role antes de ativar uma Cicatriz.")
          val scar=state.kid.scars.getOrNull(event.index) ?: return fail("Cicatriz inválida.")
          if (scar.usedForSuccess || scar.accepted) fail("Esta Cicatriz já foi aceita e não pode conceder outro sucesso.")
          else {
            val scars=state.kid.scars.toMutableList()
            scars[event.index]=scar.copy(accepted=true,usedForSuccess=true)
            ok(state.copy(kid=state.kid.copy(scars=scars),testResolution=result.copy(successes=result.successes+1)))
          }
        }
      }

      is GameEvent.HealAcceptedScar -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Cicatrizes são exclusivas de Things from the Flood.")
        else if (!state.sessionXpAwarded) fail("Cicatrizes só são curadas após a sessão.")
        else if (!event.groupApproved) fail("A cura da Cicatriz precisa ser aprovada pelo grupo após ação heroica/sacrificial ou cena cotidiana transformadora.")
        else {
          val scar=state.kid.scars.getOrNull(event.index) ?: return fail("Cicatriz inválida.")
          if (!scar.accepted || !scar.usedForSuccess) fail("Só uma Cicatriz aceita, já usada para um sucesso automático, pode ser curada.")
          else ok(state.copy(kid=state.kid.copy(scars=state.kid.scars.filterIndexed{i,_->i!=event.index})))
        }
      }

      is GameEvent.SetFriction -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Friction é exclusiva de Things from the Flood.")
        else if (!state.sessionXpAwarded && state.kid.friction.isNotBlank()) fail("Friction só pode mudar entre sessões.")
        else if (event.text.trim().isBlank()) fail("Friction não pode ficar vazia.")
        else ok(state.copy(kid=state.kid.copy(friction=event.text.trim().take(1000))))
      }

      is GameEvent.ResolveFriction -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Friction é exclusiva de Things from the Flood.")
        else if (state.kid.scarPending) fail("Registre primeiro a Cicatriz causada por ficar Quebrado.")
        else if (event.troubleOccurred) ok(state) // Trouble na própria cena cancela toda a cura.
        else ok(state.copy(kid=state.kid.copy(conditions=emptySet(),broken=false)))
      }

      is GameEvent.ApplyCondition -> {
        if (state.kid.broken) fail("Kid já está Quebrado.")
        else if (event.condition in state.kid.conditions) fail("Condição já ativa.")
        else if (state.kid.conditions.size >= 4) ok(state.copy(kid = state.kid.copy(
          broken = true,
          scarPending = state.rulesProfile == RulesProfile.FLOOD_TEENS || state.kid.scarPending
        )))
        else ok(state.copy(kid = state.kid.copy(conditions = state.kid.conditions + event.condition)))
      }

      GameEvent.BecomeBroken -> if (state.kid.broken) fail("Kid já está Quebrado.") else ok(state.copy(kid = state.kid.copy(
        broken = true,
        scarPending = state.rulesProfile == RulesProfile.FLOOD_TEENS || state.kid.scarPending
      )))

      GameEvent.HealAllConditions -> {
        if (state.rulesProfile == RulesProfile.FLOOD_TEENS && state.kid.scarPending) fail("Registre primeiro a Cicatriz causada por ficar Quebrado.")
        else ok(state.copy(kid = state.kid.copy(conditions = emptySet(), broken = false)))
      }'''
s=one(s,anchor,replacement,'Flood scar/friction reducer')

# In Extended Trouble, a Lead preparation scene creates the pool and contributes zero Threat successes.
s=one(s,
'''          val next = ext.scenes.toMutableList()
          next[index] = next[index].copy(successes = result.successes)
          ok(state.copy(extended = ext.copy(scenes = next)))''',
'''          val next = ext.scenes.toMutableList()
          val threatSuccesses = if (test.purpose == TestPurpose.LEADERSHIP_PREPARE) 0 else result.successes
          next[index] = next[index].copy(successes = threatSuccesses)
          ok(state.copy(extended = ext.copy(scenes = next)))''','Lead does not count threat')

# If a compromise makes the active Flood Teen Broken, a Scar becomes pending.
s=one(s,
'''                  nextKid = nextKid.copy(conditions = updated.conditions, broken = updated.broken)''',
'''                  val newlyBroken = updated.broken && !nextKid.broken
                  nextKid = nextKid.copy(
                    conditions = updated.conditions,
                    broken = updated.broken,
                    scarPending = nextKid.scarPending || (state.rulesProfile == RulesProfile.FLOOD_TEENS && newlyBroken)
                  )''','compromise scar pending')

# Complete Flood failure breaks every Teen; the active Teen must record a Scar.
old='''      GameEvent.EndExtendedTrouble -> {
        val ext = state.extended
        if (!ext.active) fail("Não há Problema Estendido ativo.")
        else if (!ext.allScenesResolved) fail("O Problema Estendido só termina após todas as cenas.")
        else if (ext.compromiseAvailable) fail("O grupo precisa aceitar ou recusar o compromisso antes de encerrar.")
        else ok(state.copy(extended = ext.copy(active = false), activeTest = null, testResolution = null))
      }'''
new='''      GameEvent.EndExtendedTrouble -> {
        val ext = state.extended
        if (!ext.active) fail("Não há Problema Estendido ativo.")
        else if (!ext.allScenesResolved) fail("O Problema Estendido só termina após todas as cenas.")
        else if (ext.compromiseAvailable) fail("O grupo precisa aceitar ou recusar o compromisso antes de encerrar.")
        else if (state.rulesProfile == RulesProfile.FLOOD_TEENS && ext.completeFailure) {
          val members=state.group.members.map { it.copy(broken=true) }
          ok(state.copy(
            kid=state.kid.copy(broken=true,scarPending=true),
            group=state.group.copy(members=members),
            extended=ext.copy(active=false),
            activeTest=null,
            testResolution=null
          ))
        } else ok(state.copy(extended = ext.copy(active = false), activeTest = null, testResolution = null))
      }'''
s=one(s,old,new,'Flood extended full failure')
write(rel,s)

# Snapshot v15 persists the pending-Scar gate.
rel='app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt'; s=read(rel)
s=s.replace('private const val VERSION = 14','private const val VERSION = 15',1)
s=one(s,
'''    line("kid.removedFromPlay", state.kid.removedFromPlay)
    line("kid.items",''',
'''    line("kid.removedFromPlay", state.kid.removedFromPlay)
    line("kid.scarPending", state.kid.scarPending)
    line("kid.items",''','scar pending encode')
s=one(s,
'''      removedFromPlay = if (version >= 14) bool("kid.removedFromPlay") else false,
      items = kidItems,''',
'''      removedFromPlay = if (version >= 14) bool("kid.removedFromPlay") else false,
      scarPending = if (version >= 15) bool("kid.scarPending") else false,
      items = kidItems,''','scar pending decode')
write(rel,s)

# Recovery firewall: Flood has Anchor/Friction, not the Kids' Hideout recovery shortcut.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt'; s=read(rel)
old='''            RecoveryKind.HIDEOUT -> {
              if (state.group.members.size < 2) rejected += "ação[$index] recovery_hideout: exige ao menos 2 Kids na cena qualificada."
              else if (reduce(GameEvent.HealAllConditions, "recovery_hideout")) raiseRoute(MotorRouteHint.SCENE)
            }'''
new='''            RecoveryKind.HIDEOUT -> {
              if (state.rulesProfile == RulesProfile.FLOOD_TEENS) rejected += "ação[$index] recovery_hideout: Things from the Flood usa Âncora ou resolução de Friction, não a recuperação de Esconderijo dos Kids."
              else if (state.group.members.size < 2) rejected += "ação[$index] recovery_hideout: exige ao menos 2 Kids na cena qualificada."
              else if (reduce(GameEvent.HealAllConditions, "recovery_hideout")) raiseRoute(MotorRouteHint.SCENE)
            }'''
s=one(s,old,new,'Flood hideout block')
write(rel,s)

# Update Flood contract with deterministic gates implemented in this block.
rel='app/src/main/java/com/braseiro/talesvtt/game/CanonicalFloodRules.kt'; s=read(rel)
s=s.replace('''Each time a Teen becomes Broken, the Teen gains a Scar. An unused Scar may grant +1 automatic success once, then it is accepted. After every Scar after the first, removal risk is resolved locally; the sixth Scar inevitably removes the Teen from play.''',
'''Each time a Teen becomes Broken, a Scar becomes mandatory before recovery. An unused Scar may grant +1 automatic success once, then it is accepted. After every Scar after the first, roll 1d6: if lower than the number of Scars the Teen is removed from play; the sixth Scar inevitably removes the Teen.''',1)
write(rel,s)

# Tests: Scar lifecycle, Friction, Extended Trouble and profile isolation.
test=r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FloodCoreMechanicsV1316Test {
  private fun teen(seed:Int=1)=RandomTeenFactory.create(Random(seed))
  private fun req(k:KidState,skill:String="Investigação",purpose:TestPurpose=TestPurpose.STANDARD)=k.testForSkill("x",skill,1)!!.copy(purpose=purpose)

  @Test fun fifthConditionBreaksAndRequiresScarBeforeHealing(){
    val k=teen().copy(conditions=setOf(Condition.UPSET,Condition.SCARED,Condition.EXHAUSTED,Condition.INJURED))
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=k)
    s=TalesReducer.reduce(s,GameEvent.BecomeBroken); assertTrue(s.kid.broken); assertTrue(s.kid.scarPending)
    val blocked=TalesReducer.reduce(s,GameEvent.HealAllConditions); assertNotNull(blocked.lastError)
    s=TalesReducer.reduce(s,GameEvent.RecordFloodScar("Quase morri no túnel")); assertNull(s.lastError); assertEquals(1,s.kid.scars.size); assertFalse(s.kid.scarPending)
    s=TalesReducer.reduce(s,GameEvent.HealAllConditions); assertNull(s.lastError); assertFalse(s.kid.broken); assertTrue(s.kid.conditions.isEmpty())
  }

  @Test fun laterScarNeedsSurvivalRollAndCanRemoveTeen(){
    val first=FloodScarState("primeira",accepted=false,usedForSuccess=false)
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen(2).copy(broken=true,scarPending=true,scars=listOf(first)))
    assertNotNull(TalesReducer.reduce(s,GameEvent.RecordFloodScar("segunda")).lastError)
    s=TalesReducer.reduce(s,GameEvent.RecordFloodScar("segunda",1)); assertNull(s.lastError); assertTrue(s.kid.removedFromPlay)
  }

  @Test fun sixthScarAlwaysRemovesTeen(){
    val old=List(5){FloodScarState("scar-$it",accepted=true,usedForSuccess=true)}
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen(3).copy(broken=true,scarPending=true,scars=old))
    s=TalesReducer.reduce(s,GameEvent.RecordFloodScar("sexta",6)); assertNull(s.lastError); assertTrue(s.kid.removedFromPlay); assertEquals(6,s.kid.scars.size)
  }

  @Test fun scarGivesAutomaticSuccessOnceAndBecomesAccepted(){
    val k=teen(4).copy(scars=listOf(FloodScarState("medo de água")))
    val test=req(k); var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=k,activeTest=test)
    s=TalesReducer.reduce(s,GameEvent.RollResult(List(s.finalPool()){1})); assertEquals(0,s.testResolution!!.successes)
    s=TalesReducer.reduce(s,GameEvent.UseScar(0)); assertNull(s.lastError); assertEquals(1,s.testResolution!!.successes); assertTrue(s.kid.scars[0].accepted)
    assertNotNull(TalesReducer.reduce(s,GameEvent.UseScar(0)).lastError)
  }

  @Test fun acceptedScarCanOnlyHealAfterSessionWithGroupApproval(){
    val k=teen(5).copy(scars=listOf(FloodScarState("cicatriz",accepted=true,usedForSuccess=true)))
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=k)
    assertNotNull(TalesReducer.reduce(s,GameEvent.HealAcceptedScar(0,true)).lastError)
    s=s.copy(sessionXpAwarded=true)
    assertNotNull(TalesReducer.reduce(s,GameEvent.HealAcceptedScar(0,false)).lastError)
    s=TalesReducer.reduce(s,GameEvent.HealAcceptedScar(0,true)); assertNull(s.lastError); assertTrue(s.kid.scars.isEmpty())
  }

  @Test fun frictionRecoveryHealsOnlyWhenNoTroubleOccurred(){
    val k=teen(6).copy(conditions=setOf(Condition.UPSET,Condition.SCARED),broken=true,scarPending=false,friction="ciúmes")
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=k)
    val noHeal=TalesReducer.reduce(s,GameEvent.ResolveFriction(true)); assertEquals(2,noHeal.kid.conditions.size); assertTrue(noHeal.kid.broken)
    s=TalesReducer.reduce(s,GameEvent.ResolveFriction(false)); assertTrue(s.kid.conditions.isEmpty()); assertFalse(s.kid.broken)
  }

  @Test fun leadSceneCreatesPoolButAddsZeroThreat(){
    val k=teen(7)
    val scenes=listOf(ExtendedKidScene(k.name,"Liderança"))
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=k)
    s=TalesReducer.reduce(s,GameEvent.StartExtendedTrouble("x",1,ThreatLevel.NORMAL,scenes)); assertNull(s.lastError)
    s=TalesReducer.reduce(s,GameEvent.BeginExtendedKidScene); assertNull(s.lastError)
    s=s.copy(activeTest=s.activeTest!!.copy(purpose=TestPurpose.LEADERSHIP_PREPARE))
    val pool=s.finalPool(); val dice=List(pool){if(it<2)6 else 1}
    s=TalesReducer.reduce(s,GameEvent.RollResult(dice)); assertEquals(4,s.leadershipPoolDice)
    s=TalesReducer.reduce(s,GameEvent.FinalizeExtendedKidSceneFromTest); assertEquals(0,s.extended.totalSuccesses)
  }

  @Test fun floodCompleteExtendedFailureBreaksActiveTeenAndNeedsScar(){
    val k=teen(8)
    val ext=ExtendedTroubleState(rulesProfile=RulesProfile.FLOOD_TEENS,active=true,kids=1,threatLevel=ThreatLevel.NORMAL,scenes=listOf(ExtendedKidScene(k.name,"Investigação",0)))
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=k,extended=ext)
    s=TalesReducer.reduce(s,GameEvent.EndExtendedTrouble); assertNull(s.lastError); assertTrue(s.kid.broken); assertTrue(s.kid.scarPending); assertTrue(s.group.members.all{it.broken})
  }

  @Test fun talesDoesNotAcquireFloodScarWhenBroken(){
    var s=GameState(rulesProfile=RulesProfile.TALES_KIDS,kid=KidState(conditions=setOf(Condition.UPSET,Condition.SCARED,Condition.EXHAUSTED,Condition.INJURED)))
    s=TalesReducer.reduce(s,GameEvent.BecomeBroken); assertTrue(s.kid.broken); assertFalse(s.kid.scarPending); assertTrue(s.kid.scars.isEmpty())
  }

  @Test fun snapshotPreservesPendingScar(){
    val s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen(10).copy(broken=true,scarPending=true))
    val r=GameSnapshotCodec.decode(GameSnapshotCodec.encode(s))!!; assertTrue(r.kid.scarPending); assertEquals(RulesProfile.FLOOD_TEENS,r.rulesProfile)
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/FloodCoreMechanicsV1316Test.kt',test)

resume=root/'TALES_RETOMADA_CURRENT.md'
text=resume.read_text(encoding='utf-8') if resume.exists() else '# TALES FROM THE LOOP — RETOMADA ATUAL\n'
text+='''\n## Bloco 1.3.16c-dev — Cicatrizes, Friction e Problema Estendido Flood\n- Ficar Quebrado em Flood agora exige registrar uma Cicatriz antes de recuperação.\n- Cicatriz concede um único sucesso automático; ao ser usada torna-se aceita e pode ser curada após sessão com aprovação do grupo.\n- Da 2ª à 5ª Cicatriz há teste local de permanência; a 6ª remove o Teen automaticamente.\n- Friction pode curar Condições em cena segura/intimista; qualquer Trouble na mesma cena cancela toda a cura.\n- Problema Estendido Flood usa 3x/4x/5x; cena de Liderança cria pool e soma zero ao Threat; falha completa deixa todos Quebrados e exige Cicatriz do Teen ativo.\n- Recuperação por Esconderijo permanece exclusiva de Tales/Kids.\n- Nenhum APK publicado.\n'''
resume.write_text(text,encoding='utf-8')

assert 'RecordFloodScar' in read('app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt')
assert 'threatSuccesses = if (test.purpose == TestPurpose.LEADERSHIP_PREPARE) 0' in read('app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt')
assert 'recovery_hideout: Things from the Flood' in read('app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt')
print('TALES/FLOOD 1.3.16c core mechanics: OK')
