package com.braseiro.talesvtt.game

import com.braseiro.talesvtt.life.AdventurePackageValidator
import org.junit.Assert.*
import org.junit.Test

class AiAdventureGenerationTest {

  @Test
  fun testValidAdventurePackageSchemaV1Passes() {
    val validJson = """
      {
        "schema": "braseiro.adventure/v1",
        "id": "gemini-test-12345",
        "system": "tales-kids",
        "worldId": "malaren-islands",
        "title": "O Mistério da Torre Oculta",
        "author": "Mestre Bárbara (Gemini)",
        "description": "Uma transmissão misteriosa atrai os garotos para a torre antiga.",
        "tags": ["ia", "investigacao", "tales"],
        "start": {
          "locationId": "escola",
          "hook": "Um sinal estranho interfere nos walkie-talkies durante o recreio."
        },
        "story": {
          "premise": "Transmissões de voz sem origem conhecida no canal 7.",
          "background": "Técnicos da Loop esqueceram um retransmissor ligado.",
          "playerIntroduction": "A tarde fria de outono é interrompida por estática ruidosa."
        },
        "locations": [
          { "id": "escola", "name": "Escola Björkskolan", "description": "Pátio e corredores.", "tags": ["urbano"] },
          { "id": "torre", "name": "Torre de Transmissão", "description": "Antena enferrujada na colina.", "tags": ["misterio"] }
        ],
        "npcs": [
          {
            "id": "npc-zelador",
            "name": "Gunnar",
            "role": "Zelador da escola",
            "description": "Homem sério e calado.",
            "goals": ["Evitar acidentes com os alunos"],
            "secretIds": ["sec-1"]
          }
        ],
        "secrets": [
          { "id": "sec-1", "truth": "Gunnar ouviu conversas cifradas de madrugada.", "visibility": "gm_only" }
        ],
        "enemies": [],
        "encounters": [],
        "objectives": [
          { "id": "obj-1", "description": "Localizar o retransmissor e desativar o sinal.", "required": true }
        ]
      }
    """.trimIndent()

    val result = AdventurePackageValidator.parseAndValidate(validJson)
    assertTrue("O schema gerado pela IA deve ser válido", result is AdventurePackageValidator.ValidationResult.Valid)
    val pkg = (result as AdventurePackageValidator.ValidationResult.Valid).adventurePackage
    assertEquals("braseiro.adventure/v1", pkg.schema)
    assertEquals("gemini-test-12345", pkg.id)
    assertEquals("O Mistério da Torre Oculta", pkg.title)
    assertEquals("escola", pkg.start.locationId)
    assertEquals(2, pkg.locations.size)
    assertEquals(1, pkg.npcs.size)
  }

  @Test
  fun testInvalidAdventurePackageRejected() {
    val invalidJson = """
      {
        "schema": "invalido",
        "id": "bad-id"
      }
    """.trimIndent()

    val result = AdventurePackageValidator.parseAndValidate(invalidJson)
    assertTrue("Schema inválido deve ser rejeitado", result is AdventurePackageValidator.ValidationResult.Invalid)
  }

  @Test
  fun testProceduralOfflineFallbackIsValidPackage() {
    val talesPkg = AdventureFactory.createProceduralAdventurePackage("TALES_KIDS")
    assertEquals("braseiro.adventure/v1", talesPkg.schema)
    assertEquals("tales-kids", talesPkg.system)
    assertFalse(talesPkg.isFloodTeens())
    assertTrue(talesPkg.locations.isNotEmpty())
    assertTrue(talesPkg.npcs.isNotEmpty())

    val floodPkg = AdventureFactory.createProceduralAdventurePackage("FLOOD_TEENS")
    assertEquals("braseiro.adventure/v1", floodPkg.schema)
    assertEquals("flood-teens", floodPkg.system)
    assertTrue(floodPkg.isFloodTeens())
    assertTrue(floodPkg.locations.isNotEmpty())
    assertTrue(floodPkg.npcs.isNotEmpty())
  }
}
