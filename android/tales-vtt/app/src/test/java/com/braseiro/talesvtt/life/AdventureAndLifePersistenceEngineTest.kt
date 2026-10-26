package com.braseiro.talesvtt.life

import org.junit.Assert.*
import org.junit.Test

class AdventureAndLifePersistenceEngineTest {

  private val validTalesAdventureJson = """
    {
      "schema": "braseiro.adventure/v1",
      "id": "tales-sinal-escola-001",
      "system": "tales-from-the-loop",
      "worldId": "malaren-islands",
      "title": "O Sinal sob a Escola",
      "author": "Antigravity",
      "description": "Uma interferência eletromagnética estranha afeta os aparelhos dos alunos.",
      "tags": ["escola", "loop", "anomalia"],
      "start": { "locationId": "loc-escola", "hook": "O sino de intervalo tocou com uma frequência metálica distorcida." },
      "story": {
        "premise": "Investigar a origem da pulsação subterrânea sob o ginásio da escola.",
        "background": "Um transformador experimental da Loop foi abandonado num túnel de manutenção nos anos 70.",
        "playerIntroduction": "Segunda-feira nublada. As lâmpadas fluorescentes do corredor piscam num padrão compassado."
      },
      "locations": [
        { "id": "loc-escola", "name": "Escola Primária", "description": "Prédio de tijolos vermelhos com piso encerado." },
        { "id": "loc-ginasio", "name": "Ginásio e Porão", "description": "Cheiro de poeira e borracha velha." },
        { "id": "loc-tunel", "name": "Túnel de Manutenção", "description": "Tubulações grossas cobertas por condensação morna." }
      ],
      "npcs": [
        { "id": "npc-zelador", "name": "Lars", "role": "Zelador da Escola", "goals": ["Manter os alunos longe do porão"], "secretIds": ["sec-chave-porao"] },
        { "id": "npc-professora", "name": "Karin", "role": "Professora de Ciências", "goals": ["Entender o padrão de rádio"], "secretIds": [] }
      ],
      "secrets": [
        { "id": "sec-chave-porao", "visibility": "gm_only", "truth": "Lars ouviu vozes pelo duto e trancou o acesso por medo." }
      ],
      "enemies": [
        { "id": "ene-automato", "name": "Sonda de Reparo Desgovernada", "kind": "maquina", "rulesRef": { "source": "official-loop", "statblockId": "repair-drone" }, "combatOptional": true }
      ],
      "encounters": [
        { "id": "enc-porao", "locationId": "loc-ginasio", "enemies": ["ene-automato"], "alternatives": ["fuga", "desativar-energia", "bloquear-porta"] }
      ],
      "objectives": [
        { "id": "obj-investigar-sino", "description": "Descobrir o que causa o zumbido no sino", "optional": false },
        { "id": "obj-desligar-fonte", "description": "Neutralizar a pulsação sem chamar a polícia", "optional": false }
      ],
      "worldConsequences": [
        { "id": "cons-escola-segura", "condition": "host_event:fonte-desligada", "effects": ["alarme-cessa", "zelador-grato"] }
      ]
    }
  """.trimIndent()

  @Test
  fun testAdventurePackageValidator_ValidPackage() {
    val result = AdventurePackageValidator.parseAndValidate(validTalesAdventureJson)
    assertTrue("O pacote válido deveria ser aceito", result is AdventurePackageValidator.ValidationResult.Valid)
    val pkg = (result as AdventurePackageValidator.ValidationResult.Valid).adventurePackage
    assertEquals("tales-sinal-escola-001", pkg.id)
    assertEquals("O Sinal sob a Escola", pkg.title)
    assertTrue("Deveria ser identificado como Tales/Kids", pkg.isTalesKids())
    assertFalse("Não deveria ser Flood", pkg.isFloodTeens())
    assertEquals(3, pkg.locations.size)
    assertEquals(2, pkg.npcs.size)
    assertEquals(1, pkg.secrets.size)
    assertEquals("gm_only", pkg.secrets[0].visibility)
    assertEquals(1, pkg.enemies.size)
    assertEquals(1, pkg.encounters.size)
    assertEquals(2, pkg.objectives.size)
  }

  @Test
  fun testAdventurePackageValidator_RejectsInvalidSchema() {
    val invalidSchemaJson = validTalesAdventureJson.replace("braseiro.adventure/v1", "schema.invalido/v99")
    val result = AdventurePackageValidator.parseAndValidate(invalidSchemaJson)
    assertTrue("Deveria rejeitar schema inválido", result is AdventurePackageValidator.ValidationResult.Invalid)
    val errors = (result as AdventurePackageValidator.ValidationResult.Invalid).errors
    assertTrue(errors.any { it.contains("Schema incompatível") })
  }

  @Test
  fun testAdventurePackageValidator_RejectsMissingStartLocation() {
    val missingStartLocJson = validTalesAdventureJson.replace(""""locationId": "loc-escola"""", """"locationId": "loc-inexistente"""")
    val result = AdventurePackageValidator.parseAndValidate(missingStartLocJson)
    assertTrue("Deveria rejeitar start.locationId inexistente", result is AdventurePackageValidator.ValidationResult.Invalid)
    val errors = (result as AdventurePackageValidator.ValidationResult.Invalid).errors
    assertTrue(errors.any { it.contains("loc-inexistente") })
  }

  @Test
  fun testAdventurePackageValidator_RejectsDuplicateIds() {
    val dupLocationJson = validTalesAdventureJson.replace(
      """"id": "loc-ginasio"""",
      """"id": "loc-escola""""
    )
    val result = AdventurePackageValidator.parseAndValidate(dupLocationJson)
    assertTrue("Deveria rejeitar ID de local duplicado", result is AdventurePackageValidator.ValidationResult.Invalid)
    val errors = (result as AdventurePackageValidator.ValidationResult.Invalid).errors
    assertTrue(errors.any { it.contains("duplicado") })
  }

  @Test
  fun testAdventurePackageValidator_RejectsEncounterWithMissingEnemy() {
    val missingEnemyJson = validTalesAdventureJson.replace(
      """"enemies": ["ene-automato"]""",
      """"enemies": ["ene-fantasma-inexistente"]"""
    )
    val result = AdventurePackageValidator.parseAndValidate(missingEnemyJson)
    assertTrue("Deveria rejeitar encontro com inimigo inexistente", result is AdventurePackageValidator.ValidationResult.Invalid)
    val errors = (result as AdventurePackageValidator.ValidationResult.Invalid).errors
    assertTrue(errors.any { it.contains("ene-fantasma-inexistente") })
  }

  @Test
  fun testCharacterIdentityKey_SerializationRoundTrip() {
    val key = CharacterIdentityKey(
      systemId = "tales-from-the-loop",
      worldId = "malaren-islands",
      timelineId = "timeline-1986",
      characterId = "kid-olof-001"
    )
    val serialized = key.serialize()
    val parsed = CharacterIdentityKey.parse(serialized)
    assertNotNull(parsed)
    assertEquals(key, parsed)
  }

  @Test
  fun testCharacterLegacyRecord_PersistenceAcrossAdventures() {
    val key = CharacterIdentityKey("tales", "malaren", "timeline-1", "kid-01")
    var legacy = CharacterLegacyRecord(
      identityKey = key,
      characterName = "Olof",
      profile = "TALES_KIDS",
      durableRelationships = mapOf("Karin (Professora)" to "Confiança conquistada")
    )

    // Complete first mystery
    val deed1 = ConfirmedDeed(
      id = "deed-01",
      adventureId = "adv-sinal",
      timestamp = 1000L,
      locationId = "loc-ginasio",
      summary = "Desconectou o módulo elétrico do ginásio sem causar curto-circuito.",
      witnesses = listOf("Lars (Zelador)")
    )
    legacy = legacy.addDeed(deed1)
    legacy = legacy.copy(completedAdventures = listOf("adv-sinal"))

    assertEquals(1, legacy.confirmedDeeds.size)
    assertEquals(1, legacy.completedAdventures.size)
    assertEquals("Olof", legacy.characterName)

    // Verify JSON serialization of Legacy
    val json = legacy.toJson()
    assertEquals("Olof", json.getString("characterName"))
    assertEquals(1, json.getJSONArray("confirmedDeeds").length())
    assertEquals(1, json.getJSONArray("completedAdventures").length())
  }

  @Test
  fun testWorldTimelineState_AdvanceTimeAndOffSceneJournal() {
    var timeline = WorldTimelineState(
      worldId = "malaren-islands",
      timelineId = "timeline-1986",
      seasonYear = "Outono de 1986",
      currentWeather = "Chuva leve"
    )

    timeline = timeline.advanceTime(3, "Neblina espessa")
    assertEquals(3, timeline.elapsedFictionalDays)
    assertEquals("Neblina espessa", timeline.currentWeather)

    val event = OffSceneEvent(
      id = "evt-01",
      day = 3,
      locationId = "torre-norte",
      description = "Caminhões da Riksenergi transportaram caixas seladas durante a madrugada."
    )
    timeline = timeline.recordOffSceneEvent(event)
    assertEquals(1, timeline.offSceneEventsJournal.size)
    assertEquals("torre-norte", timeline.offSceneEventsJournal[0].locationId)
  }
}
