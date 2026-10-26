package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FloodProfileFoundationV1316Test {
  private fun req(kid:KidState,skill:String="Investigação")=kid.testForSkill("x",skill,1)!!

  @Test fun teenFactoryIsCanonicalAcrossSeeds(){
    repeat(256){ seed ->
      val teen=RandomTeenFactory.create(Random(seed))
      assertTrue("seed $seed",RandomTeenFactory.validateCreation(teen))
      assertTrue(teen.age in 14..19)
      assertEquals(14,teen.attributes.values.sum())
      assertEquals(10,teen.skills.values.sum())
      assertEquals(0,teen.luck)
      assertFalse(teen.prideAvailable)
      assertTrue(teen.shameText.isNotBlank())
    }
  }

  @Test fun kidToTeenKeepsIdentityButRedistributesMechanics(){
    val kid=RandomKidFactory.create(Random(7))
    val teen=RandomTeenFactory.fromKid(kid,Random(9))
    assertEquals(kid.name,teen.name)
    assertEquals(kid.portraitAssetKey,teen.portraitAssetKey)
    assertEquals(14,teen.attributes.values.sum())
    assertEquals(10,teen.skills.values.sum())
    assertEquals(0,teen.luck)
    assertTrue(RandomTeenFactory.validateCreation(teen))
  }

  @Test fun floodPoolHasOneDieFloorButTalesDoesNot(){
    val base=KidState(conditions=setOf(Condition.UPSET,Condition.SCARED,Condition.EXHAUSTED,Condition.INJURED),attributes=linkedMapOf("CORPO" to 1,"TECNOLOGIA" to 1,"CORAÇÃO" to 1,"MENTE" to 1),skills=linkedMapOf("Furtividade" to 0,"Força" to 0,"Movimentação" to 0,"Mecânica" to 0,"Programação" to 0,"Cálculo" to 0,"Contatos" to 0,"Charme" to 0,"Liderança" to 0,"Investigação" to 0,"Conhecimento" to 0,"Empatia" to 0))
    val test=req(base)
    assertEquals(0,GameState(rulesProfile=RulesProfile.TALES_KIDS,kid=base,activeTest=test).finalPool())
    assertEquals(1,GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=base.copy(luck=0,prideAvailable=false),activeTest=test).finalPool())
  }

  @Test fun floodThreatTargetsAreThreeFourFive(){
    assertEquals(3,RulesProfiles.threatTarget(RulesProfile.FLOOD_TEENS,1,ThreatLevel.NORMAL))
    assertEquals(4,RulesProfiles.threatTarget(RulesProfile.FLOOD_TEENS,1,ThreatLevel.DIFFICULT))
    assertEquals(5,RulesProfiles.threatTarget(RulesProfile.FLOOD_TEENS,1,ThreatLevel.ALMOST_IMPOSSIBLE))
    assertEquals(2,RulesProfiles.threatTarget(RulesProfile.TALES_KIDS,1,ThreatLevel.NORMAL))
  }

  @Test fun floodRejectsLuckAndPrideButAcceptsShame(){
    val teen=RandomTeenFactory.create(Random(3))
    val test=req(teen)
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,activeTest=test)
    val pool=s.finalPool(); s=TalesReducer.reduce(s,GameEvent.RollResult(List(pool){1})); assertNull(s.lastError)
    assertNotNull(TalesReducer.reduce(s,GameEvent.SpendLuck(List(pool){6})).lastError)
    assertNotNull(TalesReducer.reduce(s,GameEvent.UsePride).lastError)
    s=TalesReducer.reduce(s,GameEvent.UseShame); assertNull(s.lastError); assertEquals(1,s.testResolution!!.successes); assertFalse(s.kid.shameAvailable)
  }

  @Test fun snapshotPreservesFloodProfileAndFields(){
    val teen=RandomTeenFactory.create(Random(11)).copy(scars=listOf(FloodScarState("corte na sobrancelha",true,true)),hooks=listOf("Helmi desapareceu"),friction="Dennis não confia em mim")
    val s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,extended=ExtendedTroubleState(rulesProfile=RulesProfile.FLOOD_TEENS,kids=2,threatLevel=ThreatLevel.NORMAL))
    val restored=GameSnapshotCodec.decode(GameSnapshotCodec.encode(s))!!
    assertEquals(RulesProfile.FLOOD_TEENS,restored.rulesProfile)
    assertEquals(teen.shameText,restored.kid.shameText)
    assertEquals(teen.birthday,restored.kid.birthday)
    assertEquals(1,restored.kid.scars.size)
    assertEquals("Flood Extended Trouble profile must survive save/load",6,restored.extended.target)
  }

  @Test fun motorFirewallCannotCrossProfiles(){
    val teen=RandomTeenFactory.create(Random(1))
    val state=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen)
    val wrong=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(profile=MotorProfile.TALES_KIDS,actions=listOf(MotorAction.Message("x"))))
    assertEquals(0,wrong.acceptedActions); assertTrue(wrong.rejected.isNotEmpty())
    val right=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(profile=MotorProfile.FLOOD_TEENS,actions=listOf(MotorAction.Message("x"))))
    assertEquals(1,right.acceptedActions)
  }
}
