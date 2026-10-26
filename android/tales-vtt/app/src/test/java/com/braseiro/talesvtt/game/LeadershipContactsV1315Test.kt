package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class LeadershipContactsV1315Test {
  private fun groupWithTarget(vararg targetConditions:Condition, broken:Boolean=false):GroupState = GroupState(members=listOf(
    GroupMemberState("Lasse Svensson","Informático","kid_portrait_01"),
    GroupMemberState("Anita Holmberg","Criança Popular","kid_portrait_02",targetConditions.toSet(),broken)
  ))

  private fun comfortState(successes:Int=3, targetConditions:Set<Condition> = setOf(Condition.SCARED,Condition.UPSET)):GameState {
    val kid=KidState(name="Lasse Svensson",conditions=setOf(Condition.EXHAUSTED))
    val test=kid.testForSkill("Confortar Anita","Liderança",1)!!.copy(purpose=TestPurpose.LEADERSHIP_COMFORT,targetKid="Anita Holmberg")
    return GameState(kid=kid,group=groupWithTarget(*targetConditions.toTypedArray()),activeTest=test,testResolution=TestResolution(List(successes){6},successes))
  }

  @Test fun leadershipComfortHealsChosenTargetCondition() {
    val state=comfortState()
    val next=TalesReducer.reduce(state,GameEvent.ApplyLeadershipComfort(Condition.SCARED))
    assertNull(next.lastError)
    assertTrue(next.testResolution!!.baseEffectApplied)
    val anita=next.group.members.first{it.name.startsWith("Anita")}
    assertFalse(Condition.SCARED in anita.conditions)
    assertTrue(Condition.UPSET in anita.conditions)
  }

  @Test fun leadershipExtraCanHealAnotherTargetCondition() {
    val base=TalesReducer.reduce(comfortState(),GameEvent.ApplyLeadershipComfort(Condition.SCARED))
    val next=TalesReducer.reduce(base,GameEvent.PurchaseBonusEffect("lead_more",Condition.UPSET.name))
    assertNull(next.lastError)
    val anita=next.group.members.first{it.name.startsWith("Anita")}
    assertTrue(anita.conditions.isEmpty())
  }

  @Test fun leadershipExtraCanHealLeader() {
    val base=TalesReducer.reduce(comfortState(),GameEvent.ApplyLeadershipComfort(Condition.SCARED))
    val next=TalesReducer.reduce(base,GameEvent.PurchaseBonusEffect("lead_self",Condition.EXHAUSTED.name))
    assertNull(next.lastError)
    assertFalse(Condition.EXHAUSTED in next.kid.conditions)
  }

  @Test fun comfortNeverHealsBrokenTarget() {
    val kid=KidState(name="Lasse Svensson")
    val request=kid.testForSkill("Confortar Anita","Liderança",1)!!.copy(purpose=TestPurpose.LEADERSHIP_COMFORT,targetKid="Anita Holmberg")
    val state=GameState(kid=kid,group=groupWithTarget(Condition.SCARED,broken=true))
    val next=TalesReducer.reduce(state,GameEvent.GmRequestTest(request))
    assertNotNull(next.lastError)
  }

  @Test fun contactBuildsOneUseItemUpToThree() {
    val kid=KidState(conditions=emptySet())
    val contactTest=kid.testForSkill("Encontrar técnico","Contatos",1)!!
    var state=GameState(kid=kid,activeTest=contactTest,testResolution=TestResolution(listOf(6,6,6,6),4))
    repeat(3) { state=TalesReducer.reduce(state,GameEvent.PurchaseBonusEffect("contact_item",null)); assertNull(state.lastError) }
    assertEquals(3,state.contactItemBonusBank)
    state=TalesReducer.reduce(state,GameEvent.ClearTest)
    val nextTest=kid.testForSkill("Abrir painel","Mecânica",1)!!
    state=TalesReducer.reduce(state,GameEvent.GmRequestTest(nextTest))
    state=TalesReducer.reduce(state,GameEvent.ApplyContactItemBonus)
    assertNull(state.lastError)
    assertEquals(3,state.activeTest!!.approvedItemBonus)
    assertEquals(0,state.contactItemBonusBank)
  }

  @Test fun contactCannotStackWithAnotherItem() {
    val kid=KidState(conditions=emptySet())
    val test=kid.testForSkill("Abrir painel","Mecânica",1)!!.copy(approvedItemBonus=2)
    val state=GameState(kid=kid,activeTest=test,contactItemBonusBank=2)
    val next=TalesReducer.reduce(state,GameEvent.ApplyContactItemBonus)
    assertNotNull(next.lastError)
    assertEquals(2,next.contactItemBonusBank)
  }

  @Test fun leadershipAssignmentAddsDiceThenIsConsumed() {
    val kid=KidState(name="Anita Holmberg",conditions=emptySet())
    val test=kid.testForSkill("Convencer adulto","Charme",1)!!
    var state=GameState(kid=kid,activeTest=test,leadershipAssignments=mapOf("Anita Holmberg" to 2))
    val expected=test.basePool()+2
    assertEquals(expected,state.finalPool())
    state=TalesReducer.reduce(state,GameEvent.RollResult(List(expected){1}))
    assertNull(state.lastError)
    assertFalse(state.leadershipAssignments.keys.any{it.equals("Anita Holmberg",true)})
  }

  @Test fun snapshotPersistsContactAndLeadershipResources() {
    val state=GameState(contactItemBonusBank=2,leadershipPoolDice=4,leadershipAssignments=mapOf("Anita Holmberg" to 2))
    val decoded=GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))!!
    assertEquals(2,decoded.contactItemBonusBank)
    assertEquals(4,decoded.leadershipPoolDice)
    assertEquals(2,decoded.leadershipAssignments["Anita Holmberg"])
  }
}
