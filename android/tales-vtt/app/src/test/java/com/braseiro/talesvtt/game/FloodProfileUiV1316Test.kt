package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FloodProfileUiV1316Test {
  private fun source(rel:String):String {
    val a=File("src/main/java/com/braseiro/talesvtt/$rel")
    val b=File("app/src/main/java/com/braseiro/talesvtt/$rel")
    return listOf(a,b).first{it.isFile}.readText()
  }

  @Test fun troubleBranchesShameScarsAwayFromLuckPride(){
    val s=source("screens/TroubleScreen.kt")
    assertTrue(s.contains("RulesProfile.FLOOD_TEENS"))
    assertTrue(s.contains("USAR VERGONHA"))
    assertTrue(s.contains("USAR CICATRIZ"))
    assertTrue(s.contains("!flood && !success && !result.luckUsed"))
    assertTrue(s.contains("!flood && gameState.kid.prideAvailable"))
    assertTrue(s.contains("CanonicalBonusEffects(gameState,onPurchaseBonusEffect)"))
    assertTrue(s.contains("RulesProfiles.effectsForSkill(gameState.rulesProfile,skill)"))
    assertFalse(s.contains("if(!flood) CanonicalBonusEffects"))
  }

  @Test fun characterSheetExposesFloodStateAndPendingScar(){
    val s=source("screens/KidScreen.kt")
    assertTrue(s.contains("FRICTION DO GRUPO"))
    assertTrue(s.contains("REGISTRAR CICATRIZ"))
    assertTrue(s.contains("gameState.kid.shameText"))
    assertTrue(s.contains("gameState.kid.hooks"))
  }

  @Test fun endingHasFloodXpChangeAndAgeBranches(){
    val s=source("screens/EndingScreen.kt")
    assertTrue(s.contains("Você ficou Quebrado e precisou receber uma Cicatriz?"))
    assertTrue(s.contains("FRICTION DO GRUPO"))
    assertTrue(s.contains("GRUPO APROVOU • CURAR CICATRIZ"))
    assertTrue(s.contains("APLICAR +1 ANO"))
    assertTrue(s.contains("Atributos não aumentam por aniversário em Flood"))
  }
}
