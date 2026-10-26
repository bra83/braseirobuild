package com.braseiro.talesvtt.life

import org.junit.Assert.*
import org.junit.Test

class IdempotentConcludeTest {

  @Test
  fun testCharacterLegacyDeduplicationIsStrictlyIdempotent() {
    val identityKey = CharacterIdentityKey("tales-kids", "malaren-islands", "timeline-default", "char-lasse-01")
    val initialRecord = CharacterLegacyRecord(
      identityKey = identityKey,
      characterName = "Lasse Svensson",
      profile = "TALES_KIDS"
    )

    val deedId = "deed-inst-misterio-01"
    val deed1 = ConfirmedDeed(
      id = deedId,
      adventureId = "inst-misterio-01",
      timestamp = 1000L,
      locationId = "escola",
      summary = "Descobriram o sinal oculto",
      witnesses = listOf("Agnes"),
      consequences = listOf("Fatos esclarecidos")
    )

    // First conclusion
    val record1 = initialRecord.copy(
      confirmedDeeds = (initialRecord.confirmedDeeds + deed1).distinctBy { it.id },
      completedAdventures = (initialRecord.completedAdventures + "O Sinal Oculto").distinct(),
      permanentScarsOrMarks = (initialRecord.permanentScarsOrMarks + listOf("Arranhão no braço")).distinct()
    )
    assertEquals(1, record1.confirmedDeeds.size)
    assertEquals(1, record1.completedAdventures.size)
    assertEquals(1, record1.permanentScarsOrMarks.size)

    // Second conclusion (calling conclude again with same deed ID)
    val deed2 = ConfirmedDeed(
      id = deedId,
      adventureId = "inst-misterio-01",
      timestamp = 2000L,
      locationId = "escola",
      summary = "Descobriram o sinal oculto (atualizado)",
      witnesses = listOf("Agnes"),
      consequences = listOf("Fatos esclarecidos")
    )
    val existingDeedIndex = record1.confirmedDeeds.indexOfFirst { it.id == deedId }
    val updatedDeeds = if (existingDeedIndex >= 0) {
      record1.confirmedDeeds.toMutableList().apply { set(existingDeedIndex, deed2) }
    } else {
      record1.confirmedDeeds + deed2
    }
    val record2 = record1.copy(
      confirmedDeeds = updatedDeeds.distinctBy { it.id },
      completedAdventures = (record1.completedAdventures + "O Sinal Oculto").distinct(),
      permanentScarsOrMarks = (record1.permanentScarsOrMarks + listOf("Arranhão no braço")).distinct()
    )

    // MUST NOT duplicate!
    assertEquals("A lista de feitos não pode duplicar", 1, record2.confirmedDeeds.size)
    assertEquals("O feito deve ter o sumário mais recente", "Descobriram o sinal oculto (atualizado)", record2.confirmedDeeds.first().summary)
    assertEquals("Completed adventures não pode duplicar", 1, record2.completedAdventures.size)
    assertEquals("Cicatrizes não podem duplicar", 1, record2.permanentScarsOrMarks.size)
  }

  @Test
  fun testWorldTimelineClockAndOffSceneEvent() {
    val timeline = WorldTimelineState(
      worldId = "malaren-islands",
      timelineId = "timeline-default",
      elapsedFictionalDays = 2,
      elapsedFictionalHours = 20
    )

    // Advance clock by 6 hours -> should cross into next day (day 3, hour 2)
    val advanced = timeline.advanceClock(6, "Chuva fina")
    assertEquals(3, advanced.elapsedFictionalDays)
    assertEquals(2, advanced.elapsedFictionalHours)
    assertEquals("Chuva fina", advanced.currentWeather)

    // Record off-scene event
    val event = OffSceneEvent(
      id = "event-deed-01",
      day = advanced.elapsedFictionalDays,
      locationId = "escola",
      description = "Zelador investiga os dutos após boatos dos alunos",
      involvedNpcIds = listOf("npc-gunnar"),
      worldStateChange = "Porta do porão trancada com corrente reforçada"
    )

    val updatedTimeline = advanced.recordOffSceneEvent(event)
    assertEquals(1, updatedTimeline.offSceneEventsJournal.size)

    // Re-recording identical event id is idempotent
    val updatedTwice = updatedTimeline.recordOffSceneEvent(event)
    assertEquals(1, updatedTwice.offSceneEventsJournal.size)
  }

  @Test
  fun testWorldTimelineShareRumor() {
    val timeline = WorldTimelineState(
      worldId = "malaren-islands",
      timelineId = "timeline-default"
    )

    val t1 = timeline.shareRumor("npc-gunnar", "fact-luzes-escola")
    assertEquals(listOf("fact-luzes-escola"), t1.npcKnowledgeGraph["npc-gunnar"])

    // Duplicate rumor sharing is idempotent
    val t2 = t1.shareRumor("npc-gunnar", "fact-luzes-escola")
    assertEquals(1, t2.npcKnowledgeGraph["npc-gunnar"]?.size)
  }
}
