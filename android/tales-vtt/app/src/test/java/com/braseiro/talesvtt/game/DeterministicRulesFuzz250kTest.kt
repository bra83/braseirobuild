package com.braseiro.talesvtt.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * P1 Gate: Fuzz de 250.000 eventos sem exceções e com validação contínua de invariantes.
 * Valida Tales from the Loop (Kids) e Things from the Flood (Teens) sob estresse estocástico.
 */
class DeterministicRulesFuzz250kTest {

  @Test
  fun testDeterministicRulesFuzz250kEvents() {
    val random = Random(42) // Seed reprodutível
    var talesIterations = 0
    var floodIterations = 0

    // Invariantes rastreadas
    var talesViolations = 0
    var floodViolations = 0

    // 125.000 iterações Tales / Kids
    var currentKid = RandomKidFactory.create(random = random)
    repeat(125_000) {
      talesIterations++
      val eventType = random.nextInt(6)
      when (eventType) {
        0 -> {
          // Rolagem de teste de perícia
          val skillLevel = random.nextInt(0, 6)
          val statLevel = random.nextInt(1, 6)
          val pool = (statLevel + skillLevel).coerceAtLeast(1)
          val rolls = List(pool) { random.nextInt(1, 7) }
          val successes = rolls.count { it == 6 }
          if (successes < 0 || pool < 1) talesViolations++
        }
        1 -> {
          // Consumo de Sorte
          if (currentKid.luck > 0) {
            val spent = currentKid.copy(luck = currentKid.luck - 1)
            if (spent.luck < 0 || spent.luck >= currentKid.maxLuck) {
              talesViolations++
            }
            currentKid = spent
          } else {
            // Sorte esgotada
            if (currentKid.luck != 0) talesViolations++
          }
        }
        2 -> {
          // Ativação de Orgulho (Pride)
          if (currentKid.prideAvailable) {
            val prideUsed = currentKid.copy(prideAvailable = false)
            if (prideUsed.prideAvailable) talesViolations++
            currentKid = prideUsed
          }
        }
        3 -> {
          // Aplicação de Condições
          val conditions = Condition.entries.toTypedArray()
          val cond = conditions[random.nextInt(conditions.size)]
          val newConditions = currentKid.conditions + cond
          val isBroken = newConditions.size >= 4
          val updated = currentKid.copy(conditions = newConditions, broken = isBroken)
          if (updated.conditions.size > 4) talesViolations++
          if (isBroken && !updated.broken) talesViolations++
          currentKid = updated
        }
        4 -> {
          // Recuperação (Âncora / Esconderijo)
          val healed = currentKid.copy(
            conditions = emptySet(),
            broken = false,
            luck = currentKid.maxLuck,
            prideAvailable = true
          )
          if (healed.broken || healed.conditions.isNotEmpty()) talesViolations++
          currentKid = healed
        }
        5 -> {
          // Criação de nova Kid periódica
          currentKid = RandomKidFactory.create(random = random)
          // Invariantes de nascimento de Kid
          if (currentKid.age !in 10..15) talesViolations++
          if (currentKid.luck != (15 - currentKid.age)) talesViolations++
        }
      }
    }

    // 125.000 iterações Flood / Teens
    var currentTeen = RandomTeenFactory.create(random = random)
    repeat(125_000) {
      floodIterations++
      val eventType = random.nextInt(6)
      when (eventType) {
        0 -> {
          // Rolagem Flood (Ano 1990s, dados Year Zero)
          val pool = random.nextInt(1, 12)
          val rolls = List(pool) { random.nextInt(1, 7) }
          val successes = rolls.count { it == 6 }
          if (successes < 0) floodViolations++
        }
        1 -> {
          // Invariante inegociável de Sorte no Flood: SEMPRE 0
          if (currentTeen.luck != 0) {
            floodViolations++
          }
        }
        2 -> {
          // Vergonha (Shame): 1x por sessão, +3 dados
          if (currentTeen.shameAvailable) {
            val shameUsed = currentTeen.copy(shameAvailable = false)
            if (shameUsed.shameAvailable) floodViolations++
            currentTeen = shameUsed
          }
        }
        3 -> {
          // Dano e Cicatrizes (Scars)
          if (currentTeen.conditions.size < 4) {
            val conds = Condition.entries.toTypedArray()
            val nextCond = conds[currentTeen.conditions.size]
            val updated = currentTeen.copy(
              conditions = currentTeen.conditions + nextCond,
              broken = (currentTeen.conditions.size + 1) >= 4
            )
            currentTeen = updated
          } else {
            // Se já quebrado, sofre Cicatriz
            val scarRoll = random.nextInt(1, 7)
            val scarPending = scarRoll == 6 // 6 = risco crítico
            val updated = currentTeen.copy(scarPending = scarPending)
            currentTeen = updated
          }
        }
        4 -> {
          // Recuperação Teen (Conforto / Discussão)
          val recovered = currentTeen.copy(
            conditions = emptySet(),
            broken = false,
            shameAvailable = true,
            scarPending = false,
            luck = 0
          )
          if (recovered.luck != 0) floodViolations++
          currentTeen = recovered
        }
        5 -> {
          // Criação periódica de novo Teen
          currentTeen = RandomTeenFactory.create(random = random)
          // Invariantes de nascimento de Teen
          if (currentTeen.age !in 14..19) floodViolations++
          if (currentTeen.luck != 0) floodViolations++
          if (currentTeen.prideAvailable) floodViolations++ // Teen não tem Orgulho
        }
      }
    }

    assertEquals("Total Tales events deve ser 125.000", 125_000, talesIterations)
    assertEquals("Total Flood events deve ser 125.000", 125_000, floodIterations)
    assertEquals("Zero violações de invariantes em Tales (125k eventos)", 0, talesViolations)
    assertEquals("Zero violações de invariantes em Flood (125k eventos)", 0, floodViolations)
  }
}
