package com.braseiro.talesvtt.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationTextChunkerTest {
  @Test
  fun longNarrativeIsSplitWithoutOversizedChunks() {
    val text = List(900) { "chuva vento bicicletas escola mistério" }.joinToString(" ") + "."
    val chunks = NarrationTextChunker.split(text, 1000)
    assertTrue(chunks.size > 1)
    assertTrue(chunks.all { it.length <= 1000 })
    assertEquals(text.replace(Regex("\\s+"), " ").trim(), chunks.joinToString(" ").replace(Regex("\\s+"), " ").trim())
  }
}
