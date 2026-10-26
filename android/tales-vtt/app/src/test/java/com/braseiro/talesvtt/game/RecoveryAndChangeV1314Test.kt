package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class RecoveryAndChangeV1314Test {
  @Test fun comfortHealsExactlyOneMildConditionAndNeverBroken() {
    val base=GameState(kid=KidState(conditions=setOf(Condition.UPSET,Condition.SCARED)))
    val healed=MotorBarbaraFirewall.apply(base,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.COMFORT,Condition.UPSET))))
    assertEquals(setOf(Condition.SCARED),healed.gameState.kid.conditions)
    val broken=base.copy(kid=base.kid.copy(broken=true))
    val rejected=MotorBarbaraFirewall.apply(broken,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.COMFORT,Condition.UPSET))))
    assertTrue(rejected.rejected.isNotEmpty())
  }

  @Test fun anchorAndQualifiedHideoutHealAll() {
    val sick=KidState(conditions=setOf(Condition.UPSET,Condition.INJURED),broken=true)
    val anchor=MotorBarbaraFirewall.apply(GameState(kid=sick),GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.ANCHOR))))
    assertTrue(anchor.gameState.kid.conditions.isEmpty()); assertFalse(anchor.gameState.kid.broken)
    val hideout=MotorBarbaraFirewall.apply(GameState(kid=sick),GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.HIDEOUT))))
    assertTrue(hideout.gameState.kid.conditions.isEmpty()); assertFalse(hideout.gameState.kid.broken)
  }

  @Test fun genericRecoveryIsRejected() {
    val state=GameState(kid=KidState(conditions=setOf(Condition.UPSET)))
    val result=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,null,null))))
    assertTrue(result.rejected.isNotEmpty()); assertEquals(setOf(Condition.UPSET),result.gameState.kid.conditions)
  }

  @Test fun talesChangeAllowsOnlyBetweenMysteriesAndRequiresConfirmation() {
    val relationship=KidRelationship("Sara","minha amiga mais próxima")
    var state=GameState(endingMode=EndingMode.MYSTERY,sessionXpAwarded=true)
    state=TalesReducer.reduce(state,GameEvent.ApplyBetweenMysteryChanges("Novo problema","Novo ímpeto","Novo orgulho","Novo item",listOf(relationship)))
    assertNull(state.lastError); assertEquals("Novo problema",state.kid.problem); assertEquals("Novo ímpeto",state.kid.drive)
    state=TalesReducer.reduce(state,GameEvent.ConfirmBetweenMysteryReview)
    assertTrue(state.change.characterReviewConfirmed)
  }

  @Test fun activePlayCannotEditChangeFields() {
    val state=TalesReducer.reduce(GameState(),GameEvent.ApplyBetweenMysteryChanges(problem="hack"))
    assertNotNull(state.lastError)
  }

  @Test fun nextMysteryRequiresChangeReview() {
    var state=GameState(endingMode=EndingMode.MYSTERY,sessionXpAwarded=true,change=ChangeState(birthdayReviewed=true,birthdayApplied=true))
    val blocked=TalesReducer.reduce(state,GameEvent.StartNextSession)
    assertNotNull(blocked.lastError)
    state=TalesReducer.reduce(state,GameEvent.ConfirmBetweenMysteryReview)
    val allowed=TalesReducer.reduce(state,GameEvent.StartNextSession)
    assertNull(allowed.lastError); assertEquals(EndingMode.NONE,allowed.endingMode)
  }
}
