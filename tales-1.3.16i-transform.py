from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16h-transform.py')}
code=(repo/'tales-1.3.16h-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')

# Deterministic end-to-end Flood simulation. Each session starts from a valid
# random Teen and exercises the same local engine surfaces a real long campaign
# repeatedly hits: Trouble, Shame, Bonus Effects, Extended Trouble, Scar, Push,
# Friction recovery, inventory, snapshot, XP/Change and next-session reset.
test=r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ThirtyThreeFloodSessionSimulationV1316Test {
  private val skills=listOf("Furtividade","Força","Movimentação","Mecânica","Programação","Cálculo","Contatos","Charme","Liderança","Investigação","Conhecimento","Empatia")
  private fun dice(pool:Int,successes:Int):List<Int> = List(pool){ if(it<successes) 6 else 1 }
  private fun bestSkill(kid:KidState):String = skills.maxBy { skill ->
    val r=kid.testForSkill("stress",skill,1)!!
    r.attributeValue+r.skillValue
  }

  private fun runSession(seed:Int) {
    val generated=RandomTeenFactory.create(Random(seed))
    assertTrue("seed $seed inválido",RandomTeenFactory.validatePersistentState(generated))
    val teen=generated.copy(
      conditions=emptySet(),
      broken=false,
      scarPending=false,
      removedFromPlay=false,
      luck=0,
      prideAvailable=false,
      shameAvailable=true,
      scars=listOf(FloodScarState("Cicatriz de stress $seed"))
    )
    val group=GroupState(members=listOf(GroupMemberState(teen.name,teen.archetype,teen.portraitAssetKey)))
    var state=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,group=group,routeHistory=listOf("Início-$seed"))

    fun apply(event:GameEvent) {
      state=TalesReducer.reduce(state,event)
      assertNull("sessão Flood $seed falhou em ${event::class.simpleName}: ${state.lastError}",state.lastError)
    }

    assertEquals(0,state.kid.luck)
    assertFalse(state.kid.prideAvailable)
    assertTrue(state.kid.shameAvailable)
    assertNotNull(TalesReducer.reduce(state,GameEvent.UsePride).lastError)
    assertNotNull(TalesReducer.reduce(state,GameEvent.SpendLuck(emptyList())).lastError)

    // Investigação: um sucesso rolado + Vergonha gera um sucesso extra que é
    // gasto no efeito Flood de informação. Isso também prova que a Vergonha
    // não pode ser usada novamente na mesma sessão.
    val investigate=state.kid.testForSkill("Sessão $seed • investigar","Investigação",1)!!
    apply(GameEvent.GmRequestTest(investigate))
    var pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,1)))
    apply(GameEvent.UseShame)
    assertEquals(2,state.testResolution!!.successes)
    assertNotNull(TalesReducer.reduce(state,GameEvent.UseShame).lastError)
    apply(GameEvent.PurchaseBonusEffect("flood_investigate_info",null))
    assertEquals(1,state.informationBonusBank)
    apply(GameEvent.ClearTest)

    // Problema Estendido Flood normal: 3x Teens. Com um Teen o alvo é 3.
    // O melhor pool sempre tem pelo menos 4 dados porque a criação Flood
    // distribui 14 pontos entre quatro Atributos antes mesmo das Perícias.
    val skill=bestSkill(state.kid)
    apply(GameEvent.StartExtendedTrouble(
      stakes="Fechar a ameaça Flood $seed",
      kids=1,
      threatLevel=ThreatLevel.NORMAL,
      scenes=listOf(ExtendedKidScene(state.kid.name,skill))
    ))
    assertEquals(3,state.extended.target)
    apply(GameEvent.BeginExtendedKidScene)
    apply(GameEvent.ApplyInformationBonus)
    assertEquals(0,state.informationBonusBank)
    pool=state.finalPool()
    assertTrue("pool estendido insuficiente no seed $seed",pool>=3)
    apply(GameEvent.RollResult(dice(pool,3)))
    apply(GameEvent.FinalizeExtendedKidSceneFromTest)
    assertEquals(3,state.extended.totalSuccesses)
    apply(GameEvent.EndExtendedTrouble)
    assertFalse(state.extended.active)

    // Uma Cicatriz concede exatamente um sucesso automático e torna-se aceita.
    val scarTest=state.kid.testForSkill("Sessão $seed • enfrentar lembrança",skill,1)!!
    apply(GameEvent.GmRequestTest(scarTest))
    pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,0)))
    apply(GameEvent.UseScar(0))
    assertEquals(1,state.testResolution!!.successes)
    assertTrue(state.kid.scars[0].accepted)
    assertTrue(state.kid.scars[0].usedForSuccess)
    assertNotNull(TalesReducer.reduce(state,GameEvent.UseScar(0)).lastError)
    apply(GameEvent.ClearTest)

    // Forçar marca uma Condição e rerrola todos os não-6 uma única vez.
    val pushTest=state.kid.testForSkill("Sessão $seed • insistir",skill,1)!!
    apply(GameEvent.GmRequestTest(pushTest))
    pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,0)))
    apply(GameEvent.PushRoll(Condition.UPSET,dice(pool,1)))
    assertTrue(Condition.UPSET in state.kid.conditions)
    assertTrue(state.testResolution!!.pushed)
    assertNotNull(TalesReducer.reduce(state,GameEvent.PushRoll(Condition.SCARED,dice(pool,1))).lastError)
    apply(GameEvent.ClearTest)

    // Friction só cura via perfil Flood e por decisão validada do Motor.
    val packet=MotorPacket(MotorProfile.FLOOD_TEENS,listOf(MotorAction.Recovery(true,RecoveryKind.FRICTION,null)))
    val recovered=MotorBarbaraFirewall.apply(state,GmPrivateState(),packet)
    assertTrue("firewall Friction rejeitou seed $seed: ${recovered.rejected}",recovered.rejected.isEmpty())
    state=recovered.gameState
    assertTrue(state.kid.conditions.isEmpty())
    assertFalse(state.kid.broken)

    // Item real + snapshot acumulativo antes do encerramento.
    apply(GameEvent.AddItem(KidItemState("Achado-$seed","Item encontrado durante o Mistério",2,KidItemKind.MYSTERY)))
    assertTrue(state.kid.items.any{it.name=="Achado-$seed"})
    state=GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))!!
    assertEquals(RulesProfile.FLOOD_TEENS,state.rulesProfile)
    assertTrue(state.kid.scars.single().accepted)
    assertEquals(0,state.kid.luck)
    assertFalse(state.kid.prideAvailable)

    // XP e Change Flood. A Cicatriz aceita pode ser curada após a sessão com
    // aprovação do grupo; Item de Mistério não promovido desaparece na revisão.
    apply(GameEvent.OpenEnding(EndingMode.MYSTERY))
    apply(GameEvent.SetXpAnswer(0,true))
    apply(GameEvent.SetXpAnswer(1,seed%2==0))
    apply(GameEvent.SetXpAnswer(2,seed%3==0))
    apply(GameEvent.SetXpAnswer(3,seed%5==0))
    apply(GameEvent.SetXpAnswer(4,seed%7==0))
    apply(GameEvent.ConfirmMysteryEnd)
    assertTrue(state.sessionXpAwarded)
    apply(GameEvent.HealAcceptedScar(0,true))
    assertTrue(state.kid.scars.isEmpty())
    apply(GameEvent.SetBirthdayPassed(false))
    apply(GameEvent.ConfirmBetweenMysteryReview)
    assertFalse(state.kid.items.any{it.kind==KidItemKind.MYSTERY})
    apply(GameEvent.StartNextSession)

    assertEquals(EndingMode.NONE,state.endingMode)
    assertFalse(state.sessionXpAwarded)
    assertEquals(RulesProfile.FLOOD_TEENS,state.rulesProfile)
    assertEquals(0,state.kid.luck)
    assertFalse(state.kid.prideAvailable)
    assertTrue(state.kid.shameAvailable)
    assertEquals(0,state.informationBonusBank)
    assertEquals(0,state.contactItemBonusBank)
    assertEquals(0,state.leadershipPoolDice)
    assertTrue(state.leadershipAssignments.isEmpty())
    assertTrue(state.grantedSuccesses.isEmpty())
    assertTrue(state.waivedTroubles.isEmpty())
  }

  @Test fun floodSession01()=runSession(1)
  @Test fun floodSession02()=runSession(2)
  @Test fun floodSession03()=runSession(3)
  @Test fun floodSession04()=runSession(4)
  @Test fun floodSession05()=runSession(5)
  @Test fun floodSession06()=runSession(6)
  @Test fun floodSession07()=runSession(7)
  @Test fun floodSession08()=runSession(8)
  @Test fun floodSession09()=runSession(9)
  @Test fun floodSession10()=runSession(10)
  @Test fun floodSession11()=runSession(11)
  @Test fun floodSession12()=runSession(12)
  @Test fun floodSession13()=runSession(13)
  @Test fun floodSession14()=runSession(14)
  @Test fun floodSession15()=runSession(15)
  @Test fun floodSession16()=runSession(16)
  @Test fun floodSession17()=runSession(17)
  @Test fun floodSession18()=runSession(18)
  @Test fun floodSession19()=runSession(19)
  @Test fun floodSession20()=runSession(20)
  @Test fun floodSession21()=runSession(21)
  @Test fun floodSession22()=runSession(22)
  @Test fun floodSession23()=runSession(23)
  @Test fun floodSession24()=runSession(24)
  @Test fun floodSession25()=runSession(25)
  @Test fun floodSession26()=runSession(26)
  @Test fun floodSession27()=runSession(27)
  @Test fun floodSession28()=runSession(28)
  @Test fun floodSession29()=runSession(29)
  @Test fun floodSession30()=runSession(30)
  @Test fun floodSession31()=runSession(31)
  @Test fun floodSession32()=runSession(32)
  @Test fun floodSession33()=runSession(33)
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/ThirtyThreeFloodSessionSimulationV1316Test.kt',test)
print('TALES/FLOOD 1.3.16i 33-session Flood simulation: OK')
