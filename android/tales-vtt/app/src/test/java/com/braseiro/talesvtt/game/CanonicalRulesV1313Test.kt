package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CanonicalRulesV1313Test {
  @Test fun canonicalCatalogIsLocalAndCompleteForCoreCharacterMath() {
    assertEquals(setOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE"), CanonicalTalesRules.attributes.toSet())
    assertEquals(12, CanonicalTalesRules.skillToAttribute.size)
    assertEquals(8, CanonicalTalesRules.types.size)
    assertEquals(3, CanonicalTalesRules.maxLuck(12))
    assertEquals(1, CanonicalTalesRules.HELP_BONUS)
    assertEquals(2, CanonicalTalesRules.ICONIC_ITEM_BONUS)
    assertEquals(5, CanonicalTalesRules.XP_SKILL_COST)
    assertEquals(10, CanonicalTalesRules.XP_STORAGE_MAX)
  }

  @Test fun eachMildConditionRemovesExactlyOneDieAndBrokenAutoFails() {
    val req = TestRequest("x","CORPO",3,"FORÇA",2,1)
    val base = GameState(kid = KidState(conditions=emptySet(), broken=false), activeTest=req)
    assertEquals(5, base.finalPool())
    val conditioned = base.copy(kid=base.kid.copy(conditions=setOf(Condition.UPSET, Condition.SCARED)))
    assertEquals(3, conditioned.finalPool())
    assertEquals(0, conditioned.copy(kid=conditioned.kid.copy(broken=true)).finalPool())
  }

  @Test fun zeroPoolDoesNotBorrowFloodMinimumDie() {
    val state=GameState(kid=KidState(conditions=setOf(Condition.UPSET,Condition.SCARED,Condition.EXHAUSTED,Condition.INJURED)), activeTest=TestRequest("x","CORPO",2,null,0,1))
    val result=TalesReducer.reduce(state, GameEvent.RollResult(emptyList()))
    assertTrue(result.lastError.orEmpty().contains("TAL-EDGE-001"))
  }

  @Test fun actualMysteryItemUsesOnlyItsStoredBonus() {
    val item=KidItemState("Detector improvisado","Montado durante o Mistério",3,KidItemKind.MYSTERY)
    var state=GameState(kid=KidState(items=listOf(item)), activeTest=TestRequest("x","TECNOLOGIA",3,"MECÂNICA",2,1))
    val good=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.ApproveMysteryItem(item.name,3))))
    assertEquals(3,good.gameState.activeTest?.approvedItemBonus)
    assertTrue(good.rejected.isEmpty())
    val bad=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.ApproveMysteryItem(item.name,2))))
    assertEquals(0,bad.gameState.activeTest?.approvedItemBonus)
    assertTrue(bad.rejected.isNotEmpty())
  }

  @Test fun ordinaryObjectNeverGetsAutomaticBonus() {
    val item=KidItemState("Caderno","anotações",0,KidItemKind.ORDINARY)
    val state=GameState(kid=KidState(items=listOf(item)), activeTest=TestRequest("x","MENTE",3,"INVESTIGAÇÃO",2,1))
    val result=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.ApproveMysteryItem(item.name,1))))
    assertEquals(0,result.gameState.activeTest?.approvedItemBonus)
    assertTrue(result.rejected.isNotEmpty())
  }

  @Test fun itemAcquisitionAndRemovalAreEngineOwned() {
    val mystery=KidItemState("Chave codificada","abre o painel",2,KidItemKind.MYSTERY)
    val added=MotorBarbaraFirewall.apply(GameState(),GmPrivateState(),MotorPacket(actions=listOf(MotorAction.GiveItem(mystery))))
    assertEquals(mystery,added.gameState.kid.items.single())
    val removed=MotorBarbaraFirewall.apply(added.gameState,added.privateState,MotorPacket(actions=listOf(MotorAction.RemoveItem(mystery.name))))
    assertTrue(removed.gameState.kid.items.isEmpty())
  }

  @Test fun snapshotRoundTripPreservesInventoryAndFavoriteSong() {
    val item=KidItemState("Mapa transparente","marcado em vermelho",1,KidItemKind.MYSTERY)
    val state=GameState(kid=KidState(favoriteSong="Take On Me",items=listOf(item)))
    val decoded=GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))!!
    assertEquals("Take On Me",decoded.kid.favoriteSong)
    assertEquals(item,decoded.kid.items.single())
  }

  @Test fun mysteryBoundaryKeepsMysteryItemsUntilChangeThenDropsRemaining() {
    val ordinary=KidItemState("Caderno","",0,KidItemKind.ORDINARY)
    val mystery=KidItemState("Detector","",2,KidItemKind.MYSTERY)
    val state=GameState(kid=KidState(items=listOf(ordinary,mystery)),endingMode=EndingMode.MYSTERY,xpAnswers=listOf(true,false,false,false,false))
    val ended=TalesReducer.reduce(state,GameEvent.ConfirmMysteryEnd)
    assertNull(ended.lastError)
    assertTrue(ended.kid.items.any{it.kind==KidItemKind.MYSTERY})
    val changed=TalesReducer.reduce(ended,GameEvent.ConfirmBetweenMysteryReview)
    assertNull(changed.lastError)
    assertTrue(changed.kid.items.any{it.kind==KidItemKind.ORDINARY})
    assertFalse(changed.kid.items.any{it.kind==KidItemKind.MYSTERY})
  }

  @Test fun locationsRemainDynamicVisibleStateNotHardcodedRuleCatalog() {
    var state=GameState(revealed=RevealedState(clues=emptySet(),locations=emptySet(),connections=emptySet()))
    state=TalesReducer.reduce(state,GameEvent.RevealLocation("Estação meteorológica abandonada"))
    assertTrue("Estação meteorológica abandonada" in state.revealed.locations)
  }

  @Test fun transportExplicitlySeparatesMechanicalAuthorityFromOptionalReferenceContext() {
    val candidates=listOf(File("src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt"),File("app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt"))
    val src=candidates.first { it.isFile }.readText()
    assertTrue(src.contains("mechanical_contract"))
    assertTrue(src.contains("RulesProfiles.mechanicalContract(state.rulesProfile)"))
    assertTrue(src.contains("reference_context"))
    assertTrue(src.contains("NUNCA use estes trechos para decidir ou alterar regras"))
    assertFalse(src.contains("rag_instruction"))
  }
}
