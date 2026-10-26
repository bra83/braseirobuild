package com.braseiro.talesvtt.life

import org.json.JSONArray
import org.json.JSONObject

/**
 * Universal durable identity key: (systemId, worldId, timelineId, characterId).
 * Preserves character legacy across multiple separate adventures.
 */
data class CharacterIdentityKey(
  val systemId: String,
  val worldId: String,
  val timelineId: String,
  val characterId: String
) {
  fun serialize(): String = "$systemId::$worldId::$timelineId::$characterId"

  companion object {
    fun parse(raw: String): CharacterIdentityKey? {
      val parts = raw.split("::")
      return if (parts.size == 4) CharacterIdentityKey(parts[0], parts[1], parts[2], parts[3]) else null
    }
  }
}

/**
 * Verified confirmed historical biographical record across adventures.
 */
data class CharacterLegacyRecord(
  val identityKey: CharacterIdentityKey,
  val characterName: String,
  val profile: String, // "TALES_KIDS" or "FLOOD_TEENS"
  val confirmedDeeds: List<ConfirmedDeed> = emptyList(),
  val notableFailures: List<ConfirmedDeed> = emptyList(),
  val durableRelationships: Map<String, String> = emptyMap(), // NPC Name -> Relationship status
  val permanentScarsOrMarks: List<String> = emptyList(),
  val hideoutsOrAnchors: List<String> = emptyList(),
  val localReputation: Map<String, String> = emptyMap(), // LocationId/Faction -> Reputation note
  val completedAdventures: List<String> = emptyList(),
  val creationTimestamp: Long = System.currentTimeMillis(),
  val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
  fun addDeed(deed: ConfirmedDeed): CharacterLegacyRecord = copy(
    confirmedDeeds = confirmedDeeds + deed,
    lastUpdatedTimestamp = System.currentTimeMillis()
  )

  fun toJson(): JSONObject {
    val obj = JSONObject()
    obj.put("identityKey", identityKey.serialize())
    obj.put("characterName", characterName)
    obj.put("profile", profile)
    val deedsArr = JSONArray()
    for (d in confirmedDeeds) deedsArr.put(d.toJson())
    obj.put("confirmedDeeds", deedsArr)
    val relObj = JSONObject()
    for ((k, v) in durableRelationships) relObj.put(k, v)
    obj.put("durableRelationships", relObj)
    obj.put("permanentScarsOrMarks", JSONArray(permanentScarsOrMarks))
    obj.put("hideoutsOrAnchors", JSONArray(hideoutsOrAnchors))
    obj.put("completedAdventures", JSONArray(completedAdventures))
    obj.put("creationTimestamp", creationTimestamp)
    obj.put("lastUpdatedTimestamp", lastUpdatedTimestamp)
    return obj
  }

  companion object {
    fun fromJson(obj: JSONObject): CharacterLegacyRecord? {
      val key = CharacterIdentityKey.parse(obj.optString("identityKey")) ?: return null
      val deeds = mutableListOf<ConfirmedDeed>()
      val deedsArr = obj.optJSONArray("confirmedDeeds")
      if (deedsArr != null) {
        for (i in 0 until deedsArr.length()) {
          val dObj = deedsArr.optJSONObject(i) ?: continue
          ConfirmedDeed.fromJson(dObj)?.let { deeds.add(it) }
        }
      }
      val scars = mutableListOf<String>()
      val scarsArr = obj.optJSONArray("permanentScarsOrMarks")
      if (scarsArr != null) {
        for (i in 0 until scarsArr.length()) scars.add(scarsArr.optString(i))
      }
      val hideouts = mutableListOf<String>()
      val hideArr = obj.optJSONArray("hideoutsOrAnchors")
      if (hideArr != null) {
        for (i in 0 until hideArr.length()) hideouts.add(hideArr.optString(i))
      }
      val adventures = mutableListOf<String>()
      val advArr = obj.optJSONArray("completedAdventures")
      if (advArr != null) {
        for (i in 0 until advArr.length()) adventures.add(advArr.optString(i))
      }
      val relMap = mutableMapOf<String, String>()
      val relObj = obj.optJSONObject("durableRelationships")
      if (relObj != null) {
        val keys = relObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          relMap[k] = relObj.optString(k)
        }
      }
      return CharacterLegacyRecord(
        identityKey = key,
        characterName = obj.optString("characterName"),
        profile = obj.optString("profile", "TALES_KIDS"),
        confirmedDeeds = deeds,
        durableRelationships = relMap,
        permanentScarsOrMarks = scars,
        hideoutsOrAnchors = hideouts,
        completedAdventures = adventures,
        creationTimestamp = obj.optLong("creationTimestamp", System.currentTimeMillis()),
        lastUpdatedTimestamp = obj.optLong("lastUpdatedTimestamp", System.currentTimeMillis())
      )
    }
  }
}

data class ConfirmedDeed(
  val id: String,
  val adventureId: String,
  val timestamp: Long,
  val locationId: String,
  val summary: String,
  val witnesses: List<String> = emptyList(),
  val consequences: List<String> = emptyList()
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("adventureId", adventureId)
    put("timestamp", timestamp)
    put("locationId", locationId)
    put("summary", summary)
    put("witnesses", JSONArray(witnesses))
    put("consequences", JSONArray(consequences))
  }

  companion object {
    fun fromJson(obj: JSONObject): ConfirmedDeed? {
      val id = obj.optString("id", "")
      if (id.isEmpty()) return null
      val wit = mutableListOf<String>()
      val witArr = obj.optJSONArray("witnesses")
      if (witArr != null) {
        for (i in 0 until witArr.length()) wit.add(witArr.optString(i))
      }
      val con = mutableListOf<String>()
      val conArr = obj.optJSONArray("consequences")
      if (conArr != null) {
        for (i in 0 until conArr.length()) con.add(conArr.optString(i))
      }
      return ConfirmedDeed(
        id = id,
        adventureId = obj.optString("adventureId", ""),
        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
        locationId = obj.optString("locationId", ""),
        summary = obj.optString("summary", ""),
        witnesses = wit,
        consequences = con
      )
    }
  }
}

/**
 * Biopsychosocial and daily routine persistent life state.
 */
data class CharacterLifeState(
  val stressLevel: Int = 0, // 0 to 5
  val sleepQuality: String = "Normal",
  val dailyRoutineStatus: String = "Escola em dia",
  val currentAnchorStatus: String = "Presente e acolhedora",
  val familyFrictionStatus: String = "Estável",
  val activeLatentPressures: List<String> = emptyList()
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("stressLevel", stressLevel)
    put("sleepQuality", sleepQuality)
    put("dailyRoutineStatus", dailyRoutineStatus)
    put("currentAnchorStatus", currentAnchorStatus)
    put("familyFrictionStatus", familyFrictionStatus)
    put("activeLatentPressures", JSONArray(activeLatentPressures))
  }
}

/**
 * World timeline and living environment state.
 */
data class WorldTimelineState(
  val worldId: String,
  val timelineId: String,
  val seasonYear: String = "Outono de 1986",
  val elapsedFictionalDays: Int = 0,
  val elapsedFictionalHours: Int = 0,
  val currentWeather: String = "Nublado com vento frio",
  val activeFactionMovements: List<String> = emptyList(),
  val offSceneEventsJournal: List<OffSceneEvent> = emptyList(),
  val npcKnowledgeGraph: Map<String, List<String>> = emptyMap() // npcId -> List of known fact IDs
) {
  fun advanceTime(days: Int, newWeather: String? = null): WorldTimelineState = copy(
    elapsedFictionalDays = elapsedFictionalDays + days,
    currentWeather = newWeather ?: currentWeather
  )

  fun advanceClock(hours: Int, newWeather: String? = null): WorldTimelineState {
    val totalHours = elapsedFictionalHours + hours
    val extraDays = totalHours / 24
    val remainingHours = totalHours % 24
    return copy(
      elapsedFictionalDays = elapsedFictionalDays + extraDays,
      elapsedFictionalHours = remainingHours,
      currentWeather = newWeather ?: currentWeather
    )
  }

  fun recordOffSceneEvent(event: OffSceneEvent): WorldTimelineState = copy(
    offSceneEventsJournal = (offSceneEventsJournal + event).distinctBy { it.id }
  )

  fun shareRumor(npcId: String, factId: String): WorldTimelineState {
    val currentFacts = npcKnowledgeGraph[npcId] ?: emptyList()
    if (factId in currentFacts) return this
    val updated = npcKnowledgeGraph.toMutableMap()
    updated[npcId] = (currentFacts + factId).distinct()
    return copy(npcKnowledgeGraph = updated)
  }

  fun toJson(): JSONObject = JSONObject().apply {
    put("worldId", worldId)
    put("timelineId", timelineId)
    put("seasonYear", seasonYear)
    put("elapsedFictionalDays", elapsedFictionalDays)
    put("elapsedFictionalHours", elapsedFictionalHours)
    put("currentWeather", currentWeather)
    put("activeFactionMovements", JSONArray(activeFactionMovements))
    val eventsArr = JSONArray()
    for (e in offSceneEventsJournal) eventsArr.put(e.toJson())
    put("offSceneEventsJournal", eventsArr)
    val kgObj = JSONObject()
    for ((k, v) in npcKnowledgeGraph) kgObj.put(k, JSONArray(v))
    put("npcKnowledgeGraph", kgObj)
  }

  companion object {
    fun fromJson(obj: JSONObject): WorldTimelineState? {
      val worldId = obj.optString("worldId", "")
      val timelineId = obj.optString("timelineId", "")
      if (worldId.isEmpty() || timelineId.isEmpty()) return null
      val events = mutableListOf<OffSceneEvent>()
      val eventsArr = obj.optJSONArray("offSceneEventsJournal")
      if (eventsArr != null) {
        for (i in 0 until eventsArr.length()) {
          val eObj = eventsArr.optJSONObject(i) ?: continue
          OffSceneEvent.fromJson(eObj)?.let { events.add(it) }
        }
      }
      val factions = mutableListOf<String>()
      val facArr = obj.optJSONArray("activeFactionMovements")
      if (facArr != null) {
        for (i in 0 until facArr.length()) factions.add(facArr.optString(i))
      }
      val kg = mutableMapOf<String, List<String>>()
      val kgObj = obj.optJSONObject("npcKnowledgeGraph")
      if (kgObj != null) {
        val keys = kgObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val facts = mutableListOf<String>()
          val fArr = kgObj.optJSONArray(k)
          if (fArr != null) {
            for (j in 0 until fArr.length()) facts.add(fArr.optString(j))
          }
          kg[k] = facts
        }
      }
      return WorldTimelineState(
        worldId = worldId,
        timelineId = timelineId,
        seasonYear = obj.optString("seasonYear", "Outono de 1986"),
        elapsedFictionalDays = obj.optInt("elapsedFictionalDays", 0),
        elapsedFictionalHours = obj.optInt("elapsedFictionalHours", 0),
        currentWeather = obj.optString("currentWeather", "Nublado com vento frio"),
        activeFactionMovements = factions,
        offSceneEventsJournal = events,
        npcKnowledgeGraph = kg
      )
    }
  }
}

data class OffSceneEvent(
  val id: String,
  val day: Int,
  val locationId: String,
  val description: String,
  val involvedNpcIds: List<String> = emptyList(),
  val witnessNpcIds: List<String> = emptyList(),
  val informedNpcIds: List<String> = emptyList(),
  val worldStateChange: String = "",
  val scheduledConsequence: String = "",
  val consequenceTriggerDays: Int = 0,
  val consequenceReason: String = "",
  val timestamp: Long = System.currentTimeMillis()
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("day", day)
    put("locationId", locationId)
    put("description", description)
    put("involvedNpcIds", JSONArray(involvedNpcIds))
    put("witnessNpcIds", JSONArray(witnessNpcIds))
    put("informedNpcIds", JSONArray(informedNpcIds))
    put("worldStateChange", worldStateChange)
    put("scheduledConsequence", scheduledConsequence)
    put("consequenceTriggerDays", consequenceTriggerDays)
    put("consequenceReason", consequenceReason)
    put("timestamp", timestamp)
  }

  companion object {
    fun fromJson(obj: JSONObject): OffSceneEvent? {
      val id = obj.optString("id", "")
      if (id.isEmpty()) return null
      fun readList(key: String): List<String> {
        val arr = obj.optJSONArray(key) ?: return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
          val s = arr.optString(i)
          if (s.isNotBlank()) list.add(s)
        }
        return list
      }
      return OffSceneEvent(
        id = id,
        day = obj.optInt("day", 1),
        locationId = obj.optString("locationId", ""),
        description = obj.optString("description", ""),
        involvedNpcIds = readList("involvedNpcIds"),
        witnessNpcIds = readList("witnessNpcIds"),
        informedNpcIds = readList("informedNpcIds"),
        worldStateChange = obj.optString("worldStateChange", ""),
        scheduledConsequence = obj.optString("scheduledConsequence", ""),
        consequenceTriggerDays = obj.optInt("consequenceTriggerDays", 0),
        consequenceReason = obj.optString("consequenceReason", ""),
        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
      )
    }
  }
}


/**
 * Consequence arc tracking latent consequences caused by player decisions or matrix rolls.
 */
data class ConsequenceArc(
  val id: String,
  val sourceAdventureId: String,
  val title: String,
  val status: String, // "LATENT", "ACTIVE", "RESOLVED"
  val triggerCondition: String,
  val fictionalDelayDays: Int = 3,
  val narrativeOutcome: String
)
