package com.braseiro.talesvtt.components

import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.MotorRuntimeStatus
import com.braseiro.talesvtt.game.RulesProfile
import com.braseiro.talesvtt.game.TestRequest
import com.braseiro.talesvtt.game.TestResolution
import com.braseiro.talesvtt.model.AppDestination
import org.junit.Assert.*
import org.junit.Test

class GuidedModeTest {
  @Test fun noKeyMenuShowsGeminiPreparation() {
    val page = GuidedMode.resolve(AppDestination.MENU, "Menu", GuideContext())
    assertTrue(page.next.contains("chave"))
    assertEquals(AppDestination.KID, page.destination)
  }

  @Test fun guidedModeCoversAllBottomRoutes() {
    val routes = AppDestination.values()
    for (route in routes) {
      val page = GuidedMode.resolve(route, route.title, GuideContext(adventureStarted = true))
      assertTrue("Missing $route", page.steps.isNotEmpty())
      assertTrue("Missing $route", page.next.isNotBlank())
    }
  }

  @Test fun guideReadsActualDicePoolAndTargetWithoutRolling() {
    val test = TestRequest("Examinar a máquina", "MENTE", 3, "Investigação", 2, requiredSuccesses = 2)
    val initial = GameState(activeTest = test)
    val copy = initial.copy()
    val guide = GuidedMode.resolve(AppDestination.SCENE, "Teste", GuideContext(game = initial, adventureStarted = true))
    assertTrue(guide.rule.contains("${initial.finalPool()} dados"))
    assertTrue(guide.rule.contains("alvo: 2"))
    assertTrue(guide.next.contains("AJUDA", ignoreCase = true))
    assertEquals(copy, initial) // No state mutation, randomization or GM request.
  }

  @Test fun afterRollShowsRecordedFacesAndDoesNotCallGM() {
    val state = GameState(activeTest = TestRequest("Esconder-se", "CORPO", 3, "Furtividade", 2),
      testResolution = TestResolution(dice = listOf(2,6,4,6), successes = 2))
    val page = GuidedMode.resolve(AppDestination.SCENE, "Trouble", GuideContext(game = state))
    assertTrue(page.rule.contains("2, 6, 4, 6"))
    assertTrue(page.next.contains("ENVIAR RESULTADO"))
  }

  @Test fun whileGeminiBusyGuideExplainsNotToRepeatRoll() {
    val state = GameState(activeTest = TestRequest("Teste", "CORPO", 3),
      testResolution = TestResolution(dice = listOf(6), successes = 1))
    val page = GuidedMode.resolve(AppDestination.SCENE, "Trouble", GuideContext(game = state, motor = MotorRuntimeStatus(busy = true)))
    assertTrue(page.next.contains("Não role novamente"))
  }

  @Test fun floodGuideDoesNotRecommendKidLuck() {
    val flood = GameState(rulesProfile = RulesProfile.FLOOD_TEENS,
      activeTest = TestRequest("Teste", "CORPO", 3), testResolution = TestResolution(listOf(6), 1))
    val page = GuidedMode.resolve(AppDestination.SCENE, "Trouble", GuideContext(game = flood))
    assertTrue(page.rule.contains("Vergonha"))
    assertFalse(page.rule.contains("Sorte"))
  }

  @Test fun groupHelpReflectsRealMemberCount() {
    val state = GameState()
    val page = GuidedMode.resolve(AppDestination.GROUP, "Grupo", GuideContext(game = state))
    assertTrue(page.title.contains("${state.group.members.size}"))
  }

  @Test fun supportsNonBottomRoutes() {
    for (title in listOf("Encerramento", "Clímax", "Biblioteca", "Voz", "Aventuras", "Conexões")) {
      val page = GuidedMode.resolve(AppDestination.SCENE, title, GuideContext())
      assertTrue(title, page.steps.isNotEmpty())
    }
  }

  @Test fun ruleCardModesDoNotMixKidsAndTeens() {
    val kids = GuidedMode.ruleCards(RulesProfile.TALES_KIDS).joinToString { it.second }
    val teens = GuidedMode.ruleCards(RulesProfile.FLOOD_TEENS).joinToString { it.second }
    assertTrue(kids.contains("Sorte"))
    assertFalse(teens.contains("Use Sorte"))
  }
}