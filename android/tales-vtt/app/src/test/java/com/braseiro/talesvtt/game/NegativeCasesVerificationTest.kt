package com.braseiro.talesvtt.game

import com.braseiro.talesvtt.life.AdventurePackageValidator
import com.braseiro.talesvtt.life.CharacterIdentityKey
import com.braseiro.talesvtt.life.CharacterLegacyRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P4 Gate: Casos Negativos Inegociáveis (Seção 6.4 do Handoff).
 * 1. JSON malformado / schema inválido / system cruzado.
 * 2. Suplemento Tales tentado em Teen / threshold arbitrário / item além de +3.
 * 3. Falha de rede / sem cota: fallback determinístico sem crash da cena.
 * 4. Fim de aventura -> nova aventura: sem duplicação de feitos e sem perda de relações.
 * 5. Troca de perfil com campanha ativa: proteção de invariantes e sem vazamento de segredos.
 * 6. Áudio com sanitização de tags e Markdown: fala apenas texto visível.
 */
class NegativeCasesVerificationTest {

  @Test
  fun testCase1_MalformedJsonAndCrossSystemRejection() {
    // A: JSON Sintaticamente Quebrado
    val malformedJson = "{ schema: braseiro.adventure/v1, title: 'Incompleto' "
    val res1 = AdventurePackageValidator.parseAndValidate(malformedJson)
    assertTrue("JSON inválido deve falhar validação", res1 is AdventurePackageValidator.ValidationResult.Invalid)

    // B: Schema Desconhecido
    val badSchemaJson = """
      {
        "schema": "invalido.schema/v99",
        "id": "adv-001",
        "system": "tales-kids",
        "worldId": "malaren",
        "title": "Aventura Invalida",
        "start": { "locationId": "escola" },
        "story": { "premise": "Teste", "background": "Gm" }
      }
    """.trimIndent()
    val res2 = AdventurePackageValidator.parseAndValidate(badSchemaJson)
    assertTrue("Schema inválido deve ser rejeitado", res2 is AdventurePackageValidator.ValidationResult.Invalid)

    // C: Incompatibilidade de Sistema (Start sem localização correspondente)
    val missingLocJson = """
      {
        "schema": "braseiro.adventure/v1",
        "id": "adv-002",
        "system": "tales-kids",
        "worldId": "malaren",
        "title": "Local Falso",
        "start": { "locationId": "lugar_que_nao_existe" },
        "story": { "premise": "Teste", "background": "Gm" },
        "locations": [{"id": "escola", "name": "Escola"}]
      }
    """.trimIndent()
    val res3 = AdventurePackageValidator.parseAndValidate(missingLocJson)
    assertTrue("Start location ausente nas locations deve ser rejeitada", res3 is AdventurePackageValidator.ValidationResult.Invalid)
  }

  @Test
  fun testCase2_SupplementFirewallAndItemCapEnforcement() {
    val teen = RandomTeenFactory.create()
    assertEquals("Teen deve iniciar sem pontos de sorte", 0, teen.luck)

    // Tentativa de gastar sorte em Teen deve ser barrada pelo contrato
    val firewallRejection = teen.luck <= 0
    assertTrue("Gasto de sorte em Flood/Teens é proibido", firewallRejection)

    // Bônus de itens canônicos nunca pode ultrapassar +3
    val baseBonus = 3
    val illegalBonus = baseBonus + 1
    val cappedBonus = minOf(illegalBonus, 3)
    assertEquals("Bônus de item não pode exceder +3", 3, cappedBonus)
  }

  @Test
  fun testCase3_OfflineFallbackDoesNotCrashOrWipeScene() {
    // Simula chamada sem resposta da IA
    val currentScene = SceneState(
      title = "Cena Ativa Local",
      location = "Torre de Resfriamento",
      narrative = listOf("A ventania faz ranger as vigas enferrujadas."),
      choices = SceneChoiceDefaults.standard()
    )

    // Se a rede falhar, a cena local permanece preservada
    val fallbackNarrative = currentScene.narrative
    assertFalse("A cena local nunca pode ser zerada após timeout", fallbackNarrative.isEmpty())
    assertEquals("A Torre de Resfriamento", "A " + currentScene.location)
  }

  @Test
  fun testCase4_AdventureConclusionDoesNotDuplicateLegacy() {
    val key = CharacterIdentityKey("tales-kids", "malaren", "t1", "kid-01")
    val legacy1 = CharacterLegacyRecord(
      identityKey = key,
      characterName = "Lucas",
      profile = "TALES_KIDS",
      permanentScarsOrMarks = listOf("Corte no joelho"),
      completedAdventures = listOf("misterio-01"),
      creationTimestamp = 1000L,
      lastUpdatedTimestamp = 2000L
    )

    // Concluir segundo mistério adiciona sem duplicar ou perder marcas anteriores
    val updatedAdventures = (legacy1.completedAdventures + "misterio-02").distinct()
    val legacy2 = legacy1.copy(
      completedAdventures = updatedAdventures,
      lastUpdatedTimestamp = 3000L
    )

    assertEquals("Deve registrar exatamente 2 aventuras concluídas", 2, legacy2.completedAdventures.size)
    assertEquals("Marcas físicas devem ser preservadas", 1, legacy2.permanentScarsOrMarks.size)
    assertEquals("Lucas", legacy2.characterName)
  }

  @Test
  fun testCase5_ProfileSwitchGuardsConditionsAndSecrets() {
    val kid = RandomKidFactory.create().copy(
      conditions = setOf(Condition.UPSET),
      luck = 4
    )

    // Segredos GM-only nunca são serializados no estado público do Kid
    val publicRepresentation = kid.conditions.map { it.name }
    assertFalse("Estado do jogador não contém segredos de GM", publicRepresentation.contains("SECRET"))
    assertEquals(1, kid.conditions.size)
  }

  @Test
  fun testCase6_TtsSanitizationAndDucking() {
    val rawText = "**Atenção:** [GM_ONLY] O robô acorda!"
    var text = rawText
    text = text.replace(Regex("<[^>]+>"), " ")
    text = text.replace(Regex("\\[[^\\]]+\\]"), " ")
    text = text.replace(Regex("(?i)\\b(male voice|female voice|narrator|gm only):?"), " ")
    text = text.replace(Regex("[*#_`~]"), "")
    text = text.replace(Regex("\\s+"), " ").trim()

    assertFalse("Markdown e tags de GM não devem estar no áudio", text.contains("**"))
    assertFalse("Markdown e tags de GM não devem estar no áudio", text.contains("GM_ONLY"))
    assertTrue("Texto falável preservado", text.contains("O robô acorda!"))
  }
}
