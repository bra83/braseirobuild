package com.braseiro.talesvtt.game

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
