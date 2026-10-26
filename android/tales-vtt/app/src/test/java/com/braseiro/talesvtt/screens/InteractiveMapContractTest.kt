package com.braseiro.talesvtt.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InteractiveMapContractTest {
  @Test
  fun mapSourceKeepsInteractiveContracts() {
    val candidates = listOf(
      File("src/main/java/com/braseiro/talesvtt/screens/MapScreen.kt"),
      File("app/src/main/java/com/braseiro/talesvtt/screens/MapScreen.kt")
    )
    val sourceFile = candidates.firstOrNull { it.isFile }
      ?: error("MapScreen.kt não encontrado")
    val source = sourceFile.readText()
    assertTrue(source.contains(".clickable(onClick = onClick)"))
    assertTrue(source.contains("MapControlHotspot"))
    assertTrue(source.contains("SelectedLocationPanel"))
    assertTrue(source.contains("IR PARA"))
    assertTrue(source.contains("MapConnectionsOverlay"))
    assertTrue(source.contains("ANOTAÇÕES DOS KIDS"))
    assertTrue(source.contains("detectTapGestures"))
  }
}
