package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class ThirtyThreeSessionSimulationV1315Test {
  private fun dice(pool:Int, successes:Int):List<Int> = List(pool) { index -> if(index<successes) 6 else 1 }

  private fun runSession(seed:Int) {
    val kid=KidState(name="Lasse Svensson",conditions=emptySet(),broken=false,prideAvailable=true,xp=0)
    val group=GroupState(members=listOf(
      GroupMemberState("Lasse Svensson","Informático","kid_portrait_01"),
      GroupMemberState("Anita Holmberg","Criança Popular","kid_portrait_02",setOf(Condition.SCARED,Condition.UPSET)),
      GroupMemberState("Jonas Berg","Jocão","kid_portrait_03"),
      GroupMemberState("Malin Ek","Livro","kid_portrait_04")
    ))
    var state=GameState(kid=kid,group=group,routeHistory=listOf("Casa"))

    fun apply(event:GameEvent) {
      state=TalesReducer.reduce(state,event)
      assertNull("sessão $seed falhou em ${event::class.simpleName}: ${state.lastError}",state.lastError)
    }

    // Trouble comum: alterna Sorte, sucesso direto e Forçar; também exercita Ajuda/Item.
    val investigate=state.kid.testForSkill("Sessão $seed • investigar o fenômeno","Investigação",1)!!
    apply(GameEvent.GmRequestTest(investigate))
    if(seed%2==0) apply(GameEvent.ApplyHelp("Anita Holmberg"))
    if(seed%4==0) apply(GameEvent.ApplyApprovedItem(CanonicalTalesRules.ICONIC_ITEM_BONUS))
    var pool=state.finalPool()
    when(seed%3) {
      0 -> {
        apply(GameEvent.RollResult(dice(pool,0)))
        apply(GameEvent.SpendLuck(dice(pool,1)))
      }
      1 -> {
        apply(GameEvent.RollResult(dice(pool,1)))
        if(seed%5==1) apply(GameEvent.UsePride)
      }
      else -> {
        apply(GameEvent.RollResult(dice(pool,0)))
        apply(GameEvent.PushRoll(Condition.SCARED,dice(pool,1)))
      }
    }
    apply(GameEvent.ClearTest)
    if(Condition.SCARED in state.kid.conditions) apply(GameEvent.HealCondition(Condition.SCARED))

    // Contatos: compra recurso de contato, persiste snapshot e usa como único Item no teste seguinte.
    val contacts=state.kid.testForSkill("Sessão $seed • encontrar um contato","Contatos",1)!!
    apply(GameEvent.GmRequestTest(contacts))
    pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,2)))
    apply(GameEvent.PurchaseBonusEffect("contact_item",null))
    apply(GameEvent.ClearTest)
    state=GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))!!
    assertEquals("sessão $seed perdeu contato no save",1,state.contactItemBonusBank)

    val mechanics=state.kid.testForSkill("Sessão $seed • abrir o painel","Mecânica",1)!!
    apply(GameEvent.GmRequestTest(mechanics))
    apply(GameEvent.ApplyContactItemBonus)
    pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,1)))
    apply(GameEvent.ClearTest)
    assertEquals(0,state.contactItemBonusBank)

    // Liderança/Conforto: cura o alvo, uma Condição extra e uma Condição do líder.
    apply(GameEvent.ApplyCondition(Condition.EXHAUSTED))
    val comfort=state.kid.testForSkill("Sessão $seed • confortar Anita","Liderança",1)!!
      .copy(purpose=TestPurpose.LEADERSHIP_COMFORT,targetKid="Anita Holmberg")
    apply(GameEvent.GmRequestTest(comfort))
    pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,3)))
    apply(GameEvent.ApplyLeadershipComfort(Condition.SCARED))
    apply(GameEvent.PurchaseBonusEffect("lead_more",Condition.UPSET.name))
    apply(GameEvent.PurchaseBonusEffect("lead_self",Condition.EXHAUSTED.name))
    apply(GameEvent.ClearTest)
    assertTrue(state.group.members.first{it.name.startsWith("Anita")}.conditions.isEmpty())
    assertTrue(state.kid.conditions.isEmpty())

    // Preparação de Liderança e distribuição para outro Kid.
    val prepare=state.kid.testForSkill("Sessão $seed • preparar o grupo","Liderança",1)!!
      .copy(purpose=TestPurpose.LEADERSHIP_PREPARE)
    apply(GameEvent.GmRequestTest(prepare))
    pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,2)))
    assertEquals(4,state.leadershipPoolDice)
    apply(GameEvent.AssignLeadershipDice("Anita Holmberg",2))
    assertEquals(2,state.leadershipPoolDice)
    apply(GameEvent.ClearTest)

    // Problema Estendido completo para o Kid local; sem inventar rolagem de outro cliente.
    apply(GameEvent.StartExtendedTrouble(
      stakes="Impedir a máquina da sessão $seed",
      kids=1,
      threatLevel=ThreatLevel.NORMAL,
      scenes=listOf(ExtendedKidScene("Lasse Svensson","Investigação"))
    ))
    apply(GameEvent.BeginExtendedKidScene)
    pool=state.finalPool()
    apply(GameEvent.RollResult(dice(pool,2)))
    apply(GameEvent.FinalizeExtendedKidSceneFromTest)
    apply(GameEvent.EndExtendedTrouble)
    assertFalse(state.extended.active)

    // Inventário real e encerramento completo.
    apply(GameEvent.AddItem(KidItemState("Caderno-$seed","anotações",0,KidItemKind.ORDINARY)))
    if(seed%2==0) apply(GameEvent.AddItem(KidItemState("Detector-$seed","achado no Mistério",2,KidItemKind.MYSTERY)))

    val mode=if(seed%2==0) EndingMode.MYSTERY else EndingMode.SESSION
    apply(GameEvent.OpenEnding(mode))
    apply(GameEvent.SetXpAnswer(0,true))
    apply(GameEvent.SetXpAnswer(1,seed%2==0))
    apply(GameEvent.SetXpAnswer(2,seed%3==0))
    apply(GameEvent.SetXpAnswer(3,false))
    apply(GameEvent.SetXpAnswer(4,seed%5==0))
    apply(GameEvent.ConfirmMysteryEnd)

    if(mode==EndingMode.MYSTERY) {
      apply(GameEvent.SetBirthdayPassed(false))
      if(seed%4==0) apply(GameEvent.PromoteMysteryItemToIconic("Detector-$seed"))
      apply(GameEvent.ConfirmBetweenMysteryReview)
      assertFalse(state.kid.items.any{it.kind==KidItemKind.MYSTERY})
    }

    apply(GameEvent.StartNextSession)
    assertEquals(EndingMode.NONE,state.endingMode)
    assertFalse(state.sessionXpAwarded)
    assertEquals(0,state.informationBonusBank)
    assertEquals(0,state.contactItemBonusBank)
    assertEquals(0,state.leadershipPoolDice)
    assertTrue(state.leadershipAssignments.isEmpty())
    assertTrue(state.grantedSuccesses.isEmpty())
    assertTrue(state.waivedTroubles.isEmpty())
    assertTrue(state.kid.prideAvailable)
  }

  @Test fun session01() = runSession(1)
  @Test fun session02() = runSession(2)
  @Test fun session03() = runSession(3)
  @Test fun session04() = runSession(4)
  @Test fun session05() = runSession(5)
  @Test fun session06() = runSession(6)
  @Test fun session07() = runSession(7)
  @Test fun session08() = runSession(8)
  @Test fun session09() = runSession(9)
  @Test fun session10() = runSession(10)
  @Test fun session11() = runSession(11)
  @Test fun session12() = runSession(12)
  @Test fun session13() = runSession(13)
  @Test fun session14() = runSession(14)
  @Test fun session15() = runSession(15)
  @Test fun session16() = runSession(16)
  @Test fun session17() = runSession(17)
  @Test fun session18() = runSession(18)
  @Test fun session19() = runSession(19)
  @Test fun session20() = runSession(20)
  @Test fun session21() = runSession(21)
  @Test fun session22() = runSession(22)
  @Test fun session23() = runSession(23)
  @Test fun session24() = runSession(24)
  @Test fun session25() = runSession(25)
  @Test fun session26() = runSession(26)
  @Test fun session27() = runSession(27)
  @Test fun session28() = runSession(28)
  @Test fun session29() = runSession(29)
  @Test fun session30() = runSession(30)
  @Test fun session31() = runSession(31)
  @Test fun session32() = runSession(32)
  @Test fun session33() = runSession(33)
}
