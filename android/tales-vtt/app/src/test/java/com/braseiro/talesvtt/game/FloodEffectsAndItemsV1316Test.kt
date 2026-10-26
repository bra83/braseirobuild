package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FloodEffectsAndItemsV1316Test {
  private fun teenState():GameState = GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=RandomTeenFactory.create(Random(88)))
  private fun request(state:GameState,skill:String,purpose:TestPurpose=TestPurpose.STANDARD):TestRequest =
    state.kid.testForSkill("mesmo trouble",skill,1)!!.copy(purpose=purpose)

  @Test fun floodAndTalesEffectIdsAreMutuallyExclusive(){
    assertNull(RulesProfiles.effect(RulesProfile.FLOOD_TEENS,"force_pin"))
    assertNotNull(RulesProfiles.effect(RulesProfile.FLOOD_TEENS,"flood_force_pin"))
    assertNull(RulesProfiles.effect(RulesProfile.TALES_KIDS,"flood_force_pin"))
  }

  @Test fun floodCalculateExtraSuccessBanksInformationDie(){
    var s=teenState()
    s=s.copy(activeTest=request(s,"Cálculo"),testResolution=TestResolution(listOf(6,6),2))
    s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("flood_calculate_info"))
    assertNull(s.lastError); assertEquals(1,s.informationBonusBank)
  }

  @Test fun floodForceWaiverAutoResolvesExactSameTrouble(){
    var s=teenState()
    val r=request(s,"Força")
    s=s.copy(activeTest=r,testResolution=TestResolution(listOf(6,6),2))
    s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("flood_force_repeat")); assertNull(s.lastError)
    s=TalesReducer.reduce(s,GameEvent.ClearTest)
    s=TalesReducer.reduce(s,GameEvent.GmRequestTest(r))
    assertNull(s.lastError); assertNotNull(s.testResolution); assertEquals(1,s.testResolution!!.successes)
  }

  @Test fun floodRealItemsRequirePlusOneToPlusThree(){
    var s=teenState()
    s=TalesReducer.reduce(s,GameEvent.AddItem(KidItemState("scanner","",1,KidItemKind.MYSTERY)))
    assertNull(s.lastError); assertEquals(1,s.kid.items.size)
    val bad=TalesReducer.reduce(s,GameEvent.AddItem(KidItemState("zero","",0,KidItemKind.MYSTERY)))
    assertNotNull(bad.lastError)
  }

  @Test fun replacingIconicThenConfirmingChangeLosesOtherFloodItems(){
    val teen=RandomTeenFactory.create(Random(89)).copy(items=listOf(
      KidItemState("scanner","",1,KidItemKind.MYSTERY),KidItemState("drone","",3,KidItemKind.MYSTERY)))
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,endingMode=EndingMode.MYSTERY,sessionXpAwarded=true)
    s=TalesReducer.reduce(s,GameEvent.PromoteMysteryItemToIconic("scanner")); assertNull(s.lastError)
    assertEquals("scanner",s.kid.iconicItemName); assertEquals(1,s.kid.items.size)
    s=TalesReducer.reduce(s,GameEvent.ConfirmBetweenMysteryReview); assertNull(s.lastError)
    assertTrue(s.kid.items.isEmpty())
    assertEquals(CanonicalFloodRules.ICONIC_ITEM_BONUS,2)
  }

  @Test fun floodLeadershipPrepareUsesTwoFourSixPool(){
    var s=teenState(); s=s.copy(activeTest=request(s,"Liderança",TestPurpose.LEADERSHIP_PREPARE))
    val dice=List(s.finalPool()){if(it<2)6 else 2}
    s=TalesReducer.reduce(s,GameEvent.RollResult(dice))
    assertNull(s.lastError); assertEquals(4,s.leadershipPoolDice)
  }

  @Test fun sourceFilesRouteEffectsAndItemsThroughRulesProfiles(){
    fun src(rel:String):String {
      val a=File("src/main/java/com/braseiro/talesvtt/$rel"); val b=File("app/src/main/java/com/braseiro/talesvtt/$rel")
      return listOf(a,b).first{it.isFile}.readText()
    }
    val core=src("game/TalesCore.kt"); val motor=src("game/MotorBarbaraContract.kt"); val ui=src("screens/TroubleScreen.kt")
    assertTrue(core.contains("RulesProfiles.effect(state.rulesProfile"))
    assertTrue(core.contains("RulesProfiles.validItem(state.rulesProfile"))
    assertTrue(motor.contains("RulesProfiles.validItem(state.rulesProfile"))
    assertTrue(motor.contains("RulesProfiles.effect(state.rulesProfile"))
    assertTrue(ui.contains("RulesProfiles.effectsForSkill(gameState.rulesProfile"))
    assertFalse(ui.contains("if(!flood) CanonicalBonusEffects"))
  }
}
