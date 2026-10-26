package com.braseiro.talesvtt.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RouteHistoryTest {
  @Test
  fun worldPatchRecordsOnlyRealLocationChanges() {
    val start = GameState(
      scene = SceneState(location = "Escola Björkskolan"),
      routeHistory = listOf("Escola Björkskolan")
    )
    val moved = TalesReducer.reduce(start, GameEvent.GmWorldPatch(location = "Praça Central"))
    assertEquals(listOf("Escola Björkskolan", "Praça Central"), moved.routeHistory)

    val weatherOnly = TalesReducer.reduce(moved, GameEvent.GmWorldPatch(weather = "Neve leve"))
    assertEquals(moved.routeHistory, weatherOnly.routeHistory)
  }

  @Test
  fun snapshotRoundTripPreservesRoute() {
    val state = GameState(
      scene = SceneState(location = "Loop Industries"),
      routeHistory = listOf("Escola Björkskolan", "Praça Central", "Loop Industries")
    )
    val decoded = GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))
    assertNotNull(decoded)
    assertEquals(state.routeHistory, decoded!!.routeHistory)
  }
}
