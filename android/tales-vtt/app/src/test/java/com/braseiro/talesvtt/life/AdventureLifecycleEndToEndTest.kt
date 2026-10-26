package com.braseiro.talesvtt.life

import com.braseiro.talesvtt.game.AdventureFactory
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class AdventureLifecycleEndToEndTest {

  @Test
  fun testProceduralAdventureGenerationAndValidation() {
    val rng = Random(42)
    val kidsPkg = AdventureFactory.createProceduralAdventurePackage("TALES_KIDS", random = rng)
    assertEquals("tales-kids", kidsPkg.system)
    assertTrue("Aventura kids deve ser identificada como Tales/Kids", kidsPkg.isTalesKids())
    assertFalse(kidsPkg.isFloodTeens())
    assertTrue(kidsPkg.locations.isNotEmpty())
    assertEquals(kidsPkg.start.locationId, "escola")
    assertNotNull(kidsPkg.story.background)
    assertTrue(kidsPkg.story.background.isNotBlank())

    val floodPkg = AdventureFactory.createProceduralAdventurePackage("FLOOD_TEENS", random = rng)
    assertEquals("flood-teens", floodPkg.system)
    assertTrue("Aventura flood deve ser identificada como Flood/Teens", floodPkg.isFloodTeens())
    assertFalse(floodPkg.isTalesKids())
  }

  @Test
  fun testAdventureInstanceSerializationRoundTrip() {
    val original = AdventureInstance(
      instanceId = "inst-test-1234",
      packageId = "pkg-sample-001",
      worldId = "malaren-islands",
      timelineId = "timeline-default",
      startedAtTimestamp = 1600000000L,
      currentLocationId = "escola",
      discoveredClues = listOf("clue-1", "clue-2"),
      revealedSecretIds = setOf("sec-alpha"),
      completedObjectiveIds = setOf("obj-1"),
      triggeredEncounterIds = setOf("enc-1"),
      flags = mapOf("generator_disabled" to true),
      notes = listOf("Fita cassete ouvida"),
      activeConsequenceArcs = listOf("arc-reputation"),
      concluded = true,
      outcomeSummary = "Mistério resolvido com sucesso."
    )

    val json = original.toJson()
    val restored = AdventureInstance.fromJson(json)

    assertEquals(original.instanceId, restored.instanceId)
    assertEquals(original.packageId, restored.packageId)
    assertEquals(original.worldId, restored.worldId)
    assertEquals(original.currentLocationId, restored.currentLocationId)
    assertEquals(original.discoveredClues, restored.discoveredClues)
    assertEquals(original.revealedSecretIds, restored.revealedSecretIds)
    assertEquals(original.completedObjectiveIds, restored.completedObjectiveIds)
    assertEquals(original.flags, restored.flags)
    assertEquals(original.concluded, restored.concluded)
    assertEquals(original.outcomeSummary, restored.outcomeSummary)
  }

  @Test
  fun testCharacterLegacyRecordRoundTripWithDeeds() {
    val key = CharacterIdentityKey("tales-kids", "malaren-islands", "timeline-1", "lasse")
    val deed1 = ConfirmedDeed(
      id = "deed-01",
      adventureId = "adv-01",
      timestamp = 1000L,
      locationId = "escola",
      summary = "Consertou a fita do rádio",
      witnesses = listOf("Agnes"),
      consequences = listOf("Reputação elevada")
    )
    val deed2 = ConfirmedDeed(
      id = "deed-02",
      adventureId = "adv-02",
      timestamp = 2000L,
      locationId = "ferro-velho",
      summary = "Desativou a sonda desgovernada",
      witnesses = listOf("Jonas"),
      consequences = listOf("Sonda guardada")
    )

    val legacy = CharacterLegacyRecord(
      identityKey = key,
      characterName = "Lasse Svensson",
      profile = "TALES_KIDS",
      confirmedDeeds = listOf(deed1, deed2),
      durableRelationships = mapOf("Jonas" to "melhor amigo", "Agnes" to "confia em mim"),
      permanentScarsOrMarks = listOf("Cicatriz de solda na mão"),
      hideoutsOrAnchors = listOf("Casa da árvore"),
      completedAdventures = listOf("adv-01", "adv-02")
    )

    val json = legacy.toJson()
    val restored = CharacterLegacyRecord.fromJson(json)

    assertNotNull(restored)
    assertEquals(legacy.characterName, restored!!.characterName)
    assertEquals(legacy.profile, restored.profile)
    assertEquals(legacy.identityKey.serialize(), restored.identityKey.serialize())
    assertEquals(2, restored.confirmedDeeds.size)
    assertEquals("deed-01", restored.confirmedDeeds[0].id)
    assertEquals("deed-02", restored.confirmedDeeds[1].id)
    assertEquals("melhor amigo", restored.durableRelationships["Jonas"])
    assertEquals(1, restored.permanentScarsOrMarks.size)
    assertEquals(2, restored.completedAdventures.size)
  }
}
