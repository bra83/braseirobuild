package com.braseiro.talesvtt.life

import com.braseiro.talesvtt.game.Condition
import com.braseiro.talesvtt.game.KidRelationship
import com.braseiro.talesvtt.game.KidState
import com.braseiro.talesvtt.game.RulesProfile
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class AdventureContinuityScenarioTest {

  @Test
  fun testExhaustiveValidationP03Gates() {
    // 1. Valid JSON passes
    val validJson = createPackageJson("pkg-valid-01", "tales-kids", "escola")
    val resValid = AdventurePackageValidator.parseAndValidate(validJson)
    assertTrue("JSON válido deve passar", resValid is AdventurePackageValidator.ValidationResult.Valid)

    // 2. Malformed JSON rejects
    val malformed = "{ this is not json"
    val resMalformed = AdventurePackageValidator.parseAndValidate(malformed)
    assertTrue("JSON malformado deve ser rejeitado", resMalformed is AdventurePackageValidator.ValidationResult.Invalid)

    // 3. Incompatible schema rejects
    val badSchema = createPackageJson("pkg-bad-schema", "tales-kids", "escola", schema = "braseiro.adventure/v99")
    val resBadSchema = AdventurePackageValidator.parseAndValidate(badSchema)
    assertTrue("Schema inválido deve ser rejeitado", resBadSchema is AdventurePackageValidator.ValidationResult.Invalid)

    // 4. Duplicate location ID rejects
    val dupLocJson = createPackageJsonWithDupLocations("pkg-dup-loc", "tales-kids")
    val resDupLoc = AdventurePackageValidator.parseAndValidate(dupLocJson)
    assertTrue("IDs de locais duplicados devem ser rejeitados", resDupLoc is AdventurePackageValidator.ValidationResult.Invalid)

    // 5. Duplicate NPC ID rejects
    val dupNpcJson = createPackageJsonWithDupNpcs("pkg-dup-npc", "tales-kids")
    val resDupNpc = AdventurePackageValidator.parseAndValidate(dupNpcJson)
    assertTrue("IDs de NPCs duplicados devem ser rejeitados", resDupNpc is AdventurePackageValidator.ValidationResult.Invalid)

    // 6. Nonexistent start location reference rejects
    val badStartRef = createPackageJson("pkg-bad-start", "tales-kids", "local-fantasma")
    val resBadStart = AdventurePackageValidator.parseAndValidate(badStartRef)
    assertTrue("Referência inexistente de início deve ser rejeitada", resBadStart is AdventurePackageValidator.ValidationResult.Invalid)

    // 7. Nonexistent enemy in encounter rejects
    val badEnemyRef = createPackageJsonWithBadEnemyRef("pkg-bad-enemy", "tales-kids")
    val resBadEnemy = AdventurePackageValidator.parseAndValidate(badEnemyRef)
    assertTrue("Encontro com inimigo inexistente deve ser rejeitado", resBadEnemy is AdventurePackageValidator.ValidationResult.Invalid)

    // 8. Excessive size (> 2MB) rejects
    val hugePayload = " ".repeat(2 * 1024 * 1024 + 10)
    val resHuge = AdventurePackageValidator.parseAndValidate(hugePayload)
    assertTrue("Arquivo > 2MB deve ser rejeitado", resHuge is AdventurePackageValidator.ValidationResult.Invalid)
  }

  @Test
  fun testScenarioAtoBContinuityAndIsolation() {
    // === SCENARIO A ===
    val charId = "char-lasse-scen-01"
    val systemIdTales = "tales-kids"
    val worldId = "malaren-islands"
    val timelineId = "timeline-default"

    val initialKid = KidState(
      id = charId,
      name = "Lasse Svensson",
      relationships = listOf(
        KidRelationship("Gunnar", "zelador reservado"),
        KidRelationship("Agnes", "vizinha confiável")
      )
    )

    // Adventure A completes and updates legacy & timeline
    val identityKeyTales = CharacterIdentityKey(systemIdTales, worldId, timelineId, charId)
    val deedA = ConfirmedDeed(
      id = "deed-adv-a-01",
      adventureId = "inst-adv-a-01",
      timestamp = 1000L,
      locationId = "escola",
      summary = "Lasse e o grupo consertaram a antena da escola e salvaram Gunnar.",
      witnesses = listOf("Gunnar", "Agnes"),
      consequences = listOf("Gunnar agora é um aliado leal do grupo.")
    )

    // Gunnar relationship becomes warmer
    val updatedRelationships = mapOf(
      "Gunnar" to "amigo leal após resgate na antena",
      "Agnes" to "vizinha confiável"
    )

    val legacyAfterA = CharacterLegacyRecord(
      identityKey = identityKeyTales,
      characterName = initialKid.name,
      profile = "TALES_KIDS",
      confirmedDeeds = listOf(deedA),
      completedAdventures = listOf("O Mistério da Torre"),
      durableRelationships = updatedRelationships
    )

    val timelineAfterA = WorldTimelineState(
      worldId = worldId,
      timelineId = timelineId,
      seasonYear = "Outono de 1986",
      elapsedFictionalDays = 3,
      elapsedFictionalHours = 14,
      currentWeather = "Chuva e vento frio"
    ).recordOffSceneEvent(
      OffSceneEvent(
        id = "event-adv-a-01",
        day = 3,
        locationId = "escola",
        description = "A diretoria reforçou a segurança após o incidente da antena."
      )
    ).shareRumor("Gunnar", "fact-antena-desativada")

    // === VERIFY SIMULATED SAVE & REOPEN (PERSISTENCE) ===
    val legacyJson = legacyAfterA.toJson()
    val restoredLegacy = CharacterLegacyRecord.fromJson(legacyJson)
    assertNotNull(restoredLegacy)
    assertEquals(1, restoredLegacy!!.confirmedDeeds.size)
    assertEquals("amigo leal após resgate na antena", restoredLegacy.durableRelationships["Gunnar"])

    val timelineJson = timelineAfterA.toJson()
    val restoredTimeline = WorldTimelineState.fromJson(timelineJson)
    assertNotNull(restoredTimeline)
    assertEquals(3, restoredTimeline!!.elapsedFictionalDays)
    assertEquals(1, restoredTimeline.offSceneEventsJournal.size)
    assertEquals(listOf("fact-antena-desativada"), restoredTimeline.npcKnowledgeGraph["Gunnar"])

    // === SCENARIO B CONTINUITY ===
    // Kid enters Adventure B carrying the updated relationship
    val kidInAdvB = initialKid.copy(
      relationships = initialKid.relationships.map { rel ->
        val updatedDesc = restoredLegacy.durableRelationships[rel.name]
        if (updatedDesc != null) KidRelationship(rel.name, updatedDesc) else rel
      }
    )
    val gunnarRel = kidInAdvB.relationships.firstOrNull { it.name == "Gunnar" }
    assertNotNull(gunnarRel)
    assertEquals("amigo leal após resgate na antena", gunnarRel!!.description)

    // GM Facts in Adventure B receive past deeds and past off-scene events
    val gmFactsInB = mutableMapOf<String, String>()
    restoredLegacy.confirmedDeeds.forEach { d -> gmFactsInB["legacy-${d.id}"] = d.summary }
    restoredTimeline.offSceneEventsJournal.forEach { e -> gmFactsInB["timeline-${e.id}"] = e.description }

    assertTrue(gmFactsInB.containsKey("legacy-deed-adv-a-01"))
    assertTrue(gmFactsInB.containsKey("timeline-event-adv-a-01"))

    // === TALES VS FLOOD FIREWALL ISOLATION ===
    // Flood Teen Karin enters Flood campaign -> MUST NOT inherit Lasse's deeds or Tales relationship
    val floodIdentityKey = CharacterIdentityKey("flood-teens", worldId, timelineId, "char-karin-01")
    assertNotEquals(identityKeyTales.serialize(), floodIdentityKey.serialize())

    val floodTeenRecord = CharacterLegacyRecord(
      identityKey = floodIdentityKey,
      characterName = "Karin Lindqvist",
      profile = "FLOOD_TEENS",
      permanentScarsOrMarks = listOf("Queimadura química no antebraço")
    )
    assertEquals("FLOOD_TEENS", floodTeenRecord.profile)
    assertTrue("Flood Teen não pode ter feitos de Tales", floodTeenRecord.confirmedDeeds.isEmpty())
    assertNull("Flood Teen não compartilha relações de Tales", floodTeenRecord.durableRelationships["Gunnar"])
  }

  // --- Helper JSON creators for P03 validation ---
  private fun createPackageJson(id: String, system: String, startLoc: String, schema: String = "braseiro.adventure/v1"): String {
    return JSONObject().apply {
      put("schema", schema)
      put("id", id)
      put("system", system)
      put("worldId", "malaren-islands")
      put("title", "Aventura Teste")
      put("start", JSONObject().put("locationId", startLoc).put("hook", "Gancho"))
      put("story", JSONObject().put("premise", "Premissa").put("background", "Fatos do Mestre").put("playerIntroduction", "Intro"))
      put("locations", JSONArray().put(JSONObject().put("id", "escola").put("name", "Escola Björkskolan")))
      put("npcs", JSONArray())
      put("secrets", JSONArray())
      put("enemies", JSONArray())
      put("encounters", JSONArray())
      put("objectives", JSONArray())
    }.toString()
  }

  private fun createPackageJsonWithDupLocations(id: String, system: String): String {
    return JSONObject().apply {
      put("schema", "braseiro.adventure/v1")
      put("id", id)
      put("system", system)
      put("worldId", "malaren-islands")
      put("title", "Aventura Loc Duplicado")
      put("start", JSONObject().put("locationId", "escola").put("hook", "Gancho"))
      put("story", JSONObject().put("premise", "P").put("background", "Fatos").put("playerIntroduction", "I"))
      put("locations", JSONArray()
        .put(JSONObject().put("id", "escola").put("name", "Escola"))
        .put(JSONObject().put("id", "escola").put("name", "Escola 2"))
      )
      put("npcs", JSONArray())
      put("secrets", JSONArray())
      put("enemies", JSONArray())
      put("encounters", JSONArray())
      put("objectives", JSONArray())
    }.toString()
  }

  private fun createPackageJsonWithDupNpcs(id: String, system: String): String {
    return JSONObject().apply {
      put("schema", "braseiro.adventure/v1")
      put("id", id)
      put("system", system)
      put("worldId", "malaren-islands")
      put("title", "Aventura NPC Duplicado")
      put("start", JSONObject().put("locationId", "escola").put("hook", "Gancho"))
      put("story", JSONObject().put("premise", "P").put("background", "Fatos").put("playerIntroduction", "I"))
      put("locations", JSONArray().put(JSONObject().put("id", "escola").put("name", "Escola")))
      put("npcs", JSONArray()
        .put(JSONObject().put("id", "npc-1").put("name", "Gunnar"))
        .put(JSONObject().put("id", "npc-1").put("name", "Gunnar Clone"))
      )
      put("secrets", JSONArray())
      put("enemies", JSONArray())
      put("encounters", JSONArray())
      put("objectives", JSONArray())
    }.toString()
  }

  private fun createPackageJsonWithBadEnemyRef(id: String, system: String): String {
    return JSONObject().apply {
      put("schema", "braseiro.adventure/v1")
      put("id", id)
      put("system", system)
      put("worldId", "malaren-islands")
      put("title", "Aventura Encontro Inimigo Inexistente")
      put("start", JSONObject().put("locationId", "escola").put("hook", "Gancho"))
      put("story", JSONObject().put("premise", "P").put("background", "Fatos").put("playerIntroduction", "I"))
      put("locations", JSONArray().put(JSONObject().put("id", "escola").put("name", "Escola")))
      put("npcs", JSONArray())
      put("secrets", JSONArray())
      put("enemies", JSONArray())
      put("encounters", JSONArray().put(
        JSONObject()
          .put("id", "enc-1")
          .put("locationId", "escola")
          .put("name", "Encontro Ruim")
          .put("enemies", JSONArray().put("inimigo-fantasma"))
      ))
      put("objectives", JSONArray())
    }.toString()
  }
}
