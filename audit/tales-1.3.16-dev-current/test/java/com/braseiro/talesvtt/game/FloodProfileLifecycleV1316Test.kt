package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FloodProfileLifecycleV1316Test {
  private fun ending(teen:KidState,age:Int=teen.age)=GameState(
    rulesProfile=RulesProfile.FLOOD_TEENS,
    kid=teen.copy(age=age),
    endingMode=EndingMode.MYSTERY,
    sessionXpAwarded=true
  )

  @Test fun floodChangeEditsShameButDoesNotSilentlyEditDrive(){
    val teen=RandomTeenFactory.create(Random(31))
    val beforeDrive=teen.drive
    var s=ending(teen)
    s=TalesReducer.reduce(s,GameEvent.ApplyBetweenMysteryChanges(problem="Novo problema",drive="DRIVE INDEVIDO",shameText="Nova vergonha",iconicItemName="Novo item",relationships=teen.relationships))
    assertNull(s.lastError)
    assertEquals(beforeDrive,s.kid.drive)
    assertEquals("Nova vergonha",s.kid.shameText)
    assertEquals("Novo problema",s.kid.problem)
  }

  @Test fun floodBirthdayAddsAgeOnlyAndTwentyLeavesPlay(){
    val teen=RandomTeenFactory.create(Random(32)).copy(age=19)
    var s=ending(teen)
    s=TalesReducer.reduce(s,GameEvent.SetBirthdayPassed(true)); assertNull(s.lastError)
    val attrs=s.kid.attributes
    s=TalesReducer.reduce(s,GameEvent.ApplyBirthdayAttribute("")); assertNull(s.lastError)
    assertEquals(20,s.kid.age)
    assertEquals(attrs,s.kid.attributes)
    assertTrue(s.change.newKidRequired)
    assertTrue(s.kid.removedFromPlay)
  }

  @Test fun pendingFloodScarBlocksXpClosure(){
    val teen=RandomTeenFactory.create(Random(33)).copy(broken=true,scarPending=true)
    val answers=listOf(true,true,false,false,false)
    val s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,endingMode=EndingMode.MYSTERY,xpAnswers=answers)
    val blocked=TalesReducer.reduce(s,GameEvent.ConfirmMysteryEnd)
    assertNotNull(blocked.lastError)
    assertFalse(blocked.sessionXpAwarded)
  }

  @Test fun menuAndCodecExposeExplicitProfileFirewall(){
    val candidates=listOf(File("src/main/java/com/braseiro/talesvtt/screens/MenuScreen.kt"),File("app/src/main/java/com/braseiro/talesvtt/screens/MenuScreen.kt"))
    val menu=candidates.first{it.isFile}.readText()
    assertTrue(menu.contains("FLOOD • TEENS"))
    assertTrue(menu.contains("onSelectRulesProfile"))
    val codecCandidates=listOf(File("src/main/java/com/braseiro/talesvtt/data/KidJsonCodec.kt"),File("app/src/main/java/com/braseiro/talesvtt/data/KidJsonCodec.kt"))
    val codec=codecCandidates.first{it.isFile}.readText()
    assertTrue(codec.contains("decodeForProfile"))
    assertTrue(codec.contains("braseiro-flood-teen"))
    assertTrue(codec.contains("RandomTeenFactory.validatePersistentState"))
  }
}
