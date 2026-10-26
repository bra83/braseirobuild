package com.braseiro.talesvtt.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveKidsDiceFlowTest {
  @Test fun helpFromExistingMemberChangesPoolBeforeRoll() {
    var state = GameState(kid = KidState(name = "Lasse Svensson"),
      activeTest = TestRequest(reason = "Investigar", attributeName = "MENTE", attributeValue = 2, skillName = "INVESTIGAÇÃO", skillValue = 2, requiredSuccesses = 1))
    val before = state.finalPool()
    state = TalesReducer.reduce(state, GameEvent.ApplyHelp("Anita Holmberg"))
    assertNull(state.lastError)
    assertEquals("Anita Holmberg", state.activeTest?.helperKid)
    assertEquals(before + 1, state.finalPool())
    val duplicate = TalesReducer.reduce(state, GameEvent.ApplyHelp("Jonas Berg"))
    assertEquals("Anita Holmberg", duplicate.activeTest?.helperKid)
    assertNotNull(duplicate.lastError)
  }

  @Test fun characterIdSurvivesSnapshotAndRollResultStaysAuthoritative() {
    val source = GameState(kid = KidState(id="char-real-active-id",name="Lasse Svensson"),
      activeTest = TestRequest(reason="Teste", attributeName="CORPO",attributeValue=3,requiredSuccesses=1),
      testResolution = TestResolution(listOf(2, 6, 3), 1))
    val restored = GameSnapshotCodec.decode(GameSnapshotCodec.encode(source))!!
    assertEquals("char-real-active-id", restored.kid.id)
    assertEquals(listOf(2,6,3), restored.testResolution?.dice)
    assertTrue(restored.group.members.size == 4)
  }
}