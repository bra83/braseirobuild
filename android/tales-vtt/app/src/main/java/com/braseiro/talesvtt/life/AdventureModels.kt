package com.braseiro.talesvtt.life

import org.json.JSONArray
import org.json.JSONObject

/**
 * Universal Adventure Package Specification (braseiro.adventure/v1 & v2 compatible).
 * Represents an immutable imported or authored adventure definition.
 */
data class AdventurePackage(
  val schema: String,
  val id: String,
  val system: String,
  val worldId: String,
  val title: String,
  val author: String = "",
  val description: String = "",
  val tags: List<String> = emptyList(),
  val recommendedParty: RecommendedParty? = null,
  val start: AdventureStart,
  val story: AdventureStory,
  val locations: List<AdventureLocation> = emptyList(),
  val npcs: List<AdventureNpc> = emptyList(),
  val secrets: List<AdventureSecret> = emptyList(),
  val enemies: List<AdventureEnemy> = emptyList(),
  val encounters: List<AdventureEncounter> = emptyList(),
  val objectives: List<AdventureObjective> = emptyList(),
  val worldConsequences: List<WorldConsequence> = emptyList(),
  val rawJson: String = ""
) {
  fun isTalesKids(): Boolean = system.contains("tales", ignoreCase = true) || system.contains("kids", ignoreCase = true)
  fun isFloodTeens(): Boolean = system.contains("flood", ignoreCase = true) || system.contains("teens", ignoreCase = true)
}

data class RecommendedParty(
  val levelMin: Int = 1,
  val levelMax: Int = 5,
  val sizeMin: Int = 1,
  val sizeMax: Int = 5
)

data class AdventureStart(
  val locationId: String,
  val hook: String = ""
)

data class AdventureStory(
  val premise: String,
  val background: String = "",
  val playerIntroduction: String = "",
  val themes: List<String> = emptyList(),
  val possibleEndings: List<String> = emptyList()
)

data class AdventureLocation(
  val id: String,
  val name: String,
  val regionId: String = "",
  val description: String = "",
  val mapReference: String? = null
)

data class AdventureNpc(
  val id: String,
  val name: String,
  val role: String = "",
  val goals: List<String> = emptyList(),
  val secretIds: List<String> = emptyList(),
  val schedule: List<NpcScheduleEntry> = emptyList(),
  val relationships: List<String> = emptyList()
)

data class NpcScheduleEntry(
  val period: String,
  val locationId: String
)

data class AdventureSecret(
  val id: String,
  val visibility: String, // "gm_only" or "public"
  val truth: String,
  val revealFlag: String? = null
)

data class AdventureEnemy(
  val id: String,
  val name: String,
  val kind: String = "",
  val statblockId: String = "",
  val rulesSource: String = "",
  val personality: String = "",
  val goals: List<String> = emptyList(),
  val combatOptional: Boolean = true
)

data class AdventureEncounter(
  val id: String,
  val locationId: String,
  val enemies: List<String> = emptyList(),
  val triggerFlag: String? = null,
  val alternatives: List<String> = emptyList()
)

data class AdventureObjective(
  val id: String,
  val description: String,
  val optional: Boolean = false
)

data class WorldConsequence(
  val id: String,
  val condition: String,
  val effects: List<String> = emptyList()
)

/**
 * Mutable state of an active adventure playthrough instance.
 */
data class AdventureInstance(
  val instanceId: String,
  val packageId: String,
  val worldId: String,
  val timelineId: String,
  val startedAtTimestamp: Long,
  val currentLocationId: String,
  val discoveredClues: List<String> = emptyList(),
  val revealedSecretIds: Set<String> = emptySet(),
  val completedObjectiveIds: Set<String> = emptySet(),
  val triggeredEncounterIds: Set<String> = emptySet(),
  val flags: Map<String, Boolean> = emptyMap(),
  val notes: List<String> = emptyList(),
  val activeConsequenceArcs: List<String> = emptyList(),
  val concluded: Boolean = false,
  val outcomeSummary: String = ""
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("instanceId", instanceId)
    put("packageId", packageId)
    put("worldId", worldId)
    put("timelineId", timelineId)
    put("startedAtTimestamp", startedAtTimestamp)
    put("currentLocationId", currentLocationId)
    put("discoveredClues", JSONArray(discoveredClues))
    put("revealedSecretIds", JSONArray(revealedSecretIds))
    put("completedObjectiveIds", JSONArray(completedObjectiveIds))
    put("triggeredEncounterIds", JSONArray(triggeredEncounterIds))
    val flagsObj = JSONObject()
    flags.forEach { (k, v) -> flagsObj.put(k, v) }
    put("flags", flagsObj)
    put("notes", JSONArray(notes))
    put("activeConsequenceArcs", JSONArray(activeConsequenceArcs))
    put("concluded", concluded)
    put("outcomeSummary", outcomeSummary)
  }

  companion object {
    fun fromJson(obj: JSONObject): AdventureInstance {
      val clues = mutableListOf<String>()
      obj.optJSONArray("discoveredClues")?.let { arr ->
        for (i in 0 until arr.length()) clues.add(arr.optString(i))
      }
      val secrets = mutableSetOf<String>()
      obj.optJSONArray("revealedSecretIds")?.let { arr ->
        for (i in 0 until arr.length()) secrets.add(arr.optString(i))
      }
      val objectives = mutableSetOf<String>()
      obj.optJSONArray("completedObjectiveIds")?.let { arr ->
        for (i in 0 until arr.length()) objectives.add(arr.optString(i))
      }
      val encounters = mutableSetOf<String>()
      obj.optJSONArray("triggeredEncounterIds")?.let { arr ->
        for (i in 0 until arr.length()) encounters.add(arr.optString(i))
      }
      val flagsMap = mutableMapOf<String, Boolean>()
      obj.optJSONObject("flags")?.let { flagsObj ->
        val keys = flagsObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          flagsMap[k] = flagsObj.optBoolean(k)
        }
      }
      val notesList = mutableListOf<String>()
      obj.optJSONArray("notes")?.let { arr ->
        for (i in 0 until arr.length()) notesList.add(arr.optString(i))
      }
      val arcs = mutableListOf<String>()
      obj.optJSONArray("activeConsequenceArcs")?.let { arr ->
        for (i in 0 until arr.length()) arcs.add(arr.optString(i))
      }
      return AdventureInstance(
        instanceId = obj.optString("instanceId"),
        packageId = obj.optString("packageId"),
        worldId = obj.optString("worldId"),
        timelineId = obj.optString("timelineId"),
        startedAtTimestamp = obj.optLong("startedAtTimestamp"),
        currentLocationId = obj.optString("currentLocationId"),
        discoveredClues = clues,
        revealedSecretIds = secrets,
        completedObjectiveIds = objectives,
        triggeredEncounterIds = encounters,
        flags = flagsMap,
        notes = notesList,
        activeConsequenceArcs = arcs,
        concluded = obj.optBoolean("concluded"),
        outcomeSummary = obj.optString("outcomeSummary")
      )
    }
  }
}

/**
 * Validator and JSON Parser for Adventure Packages.
 */
object AdventurePackageValidator {

  sealed class ValidationResult {
    data class Valid(val adventurePackage: AdventurePackage) : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
  }

  fun parseAndValidate(jsonString: String): ValidationResult {
    if (jsonString.length > 2 * 1024 * 1024) {
      return ValidationResult.Invalid(listOf("Arquivo excede o limite máximo permitido de 2 MB."))
    }
    val errors = mutableListOf<String>()
    val root = try {
      JSONObject(jsonString)
    } catch (e: Exception) {
      return ValidationResult.Invalid(listOf("JSON malformado: ${e.message}"))
    }

    val schema = root.optString("schema", "").trim()
    if (schema != "braseiro.adventure/v1" && schema != "braseiro.adventure/v2") {
      errors.add("Schema incompatível: '$schema'. Esperado 'braseiro.adventure/v1' ou 'v2'.")
    }

    val id = root.optString("id", "").trim()
    if (id.isEmpty()) {
      errors.add("Campo 'id' da aventura é obrigatório.")
    } else if (!Regex("^[a-zA-Z0-9_-]{3,64}$").matches(id)) {
      errors.add("ID '$id' inválido: deve conter entre 3 e 64 caracteres alfanuméricos, hífen ou sublinhado.")
    }

    val system = root.optString("system", "").trim()
    val validSystems = setOf("tales-kids", "flood-teens", "tales-from-the-loop", "things-from-the-flood")
    if (system.isEmpty()) {
      errors.add("Campo 'system' é obrigatório.")
    } else if (!validSystems.contains(system.lowercase())) {
      errors.add("Sistema desconhecido: '$system'. Sistemas autorizados: tales-kids, flood-teens, tales-from-the-loop, things-from-the-flood.")
    }

    val worldId = root.optString("worldId", "").trim()
    if (worldId.isEmpty()) errors.add("Campo 'worldId' é obrigatório.")

    val title = root.optString("title", "").trim()
    if (title.isEmpty()) errors.add("Campo 'title' é obrigatório.")

    val startObj = root.optJSONObject("start")
    val startLocationId = startObj?.optString("locationId", "")?.trim() ?: ""
    if (startLocationId.isEmpty()) {
      errors.add("start.locationId é obrigatório.")
    }

    val storyObj = root.optJSONObject("story")
    val premise = storyObj?.optString("premise", "")?.trim() ?: ""
    val background = storyObj?.optString("background", "")?.trim() ?: ""
    if (premise.isEmpty()) {
      errors.add("story.premise é obrigatório.")
    }
    if (background.isEmpty()) {
      errors.add("Campo obrigatório 'story.background' ausente ou vazio.")
    }

    // Parse Locations
    val locationsArr = root.optJSONArray("locations")
    val locationList = mutableListOf<AdventureLocation>()
    val locationIds = mutableSetOf<String>()
    if (locationsArr != null) {
      for (i in 0 until locationsArr.length()) {
        val loc = locationsArr.optJSONObject(i) ?: continue
        val locId = loc.optString("id", "").trim()
        val locName = loc.optString("name", "").trim()
        if (locId.isEmpty() || locName.isEmpty()) {
          errors.add("Local na posição $i deve conter 'id' e 'name'.")
        } else if (!locationIds.add(locId)) {
          errors.add("ID de local duplicado: '$locId'.")
        }
        locationList.add(
          AdventureLocation(
            id = locId,
            name = locName,
            regionId = loc.optString("regionId", ""),
            description = loc.optString("description", "")
          )
        )
      }
    }

    if (startLocationId.isNotEmpty() && !locationIds.contains(startLocationId)) {
      errors.add("start.locationId '$startLocationId' não existe na lista de locations.")
    }

    // Parse Secrets
    val secretsArr = root.optJSONArray("secrets")
    val secretList = mutableListOf<AdventureSecret>()
    val secretIds = mutableSetOf<String>()
    if (secretsArr != null) {
      for (i in 0 until secretsArr.length()) {
        val sec = secretsArr.optJSONObject(i) ?: continue
        val secId = sec.optString("id", "").trim()
        val vis = sec.optString("visibility", "gm_only").trim()
        val truth = sec.optString("truth", "").trim()
        if (secId.isEmpty()) errors.add("Segredo na posição $i sem 'id'.")
        if (!secretIds.add(secId)) errors.add("ID de segredo duplicado: '$secId'.")
        if (vis != "gm_only" && vis != "public") {
          errors.add("Visibilidade de segredo '$secId' inválida: '$vis'. Deve ser 'gm_only' ou 'public'.")
        }
        secretList.add(
          AdventureSecret(
            id = secId,
            visibility = vis,
            truth = truth,
            revealFlag = sec.optJSONObject("revealWhen")?.optString("flag")
          )
        )
      }
    }

    // Parse NPCs
    val npcsArr = root.optJSONArray("npcs")
    val npcList = mutableListOf<AdventureNpc>()
    val npcIds = mutableSetOf<String>()
    if (npcsArr != null) {
      for (i in 0 until npcsArr.length()) {
        val npc = npcsArr.optJSONObject(i) ?: continue
        val npcId = npc.optString("id", "").trim()
        val name = npc.optString("name", "").trim()
        if (npcId.isEmpty()) errors.add("NPC na posição $i sem 'id'.")
        if (!npcIds.add(npcId)) errors.add("ID de NPC duplicado: '$npcId'.")
        npcList.add(
          AdventureNpc(
            id = npcId,
            name = name,
            role = npc.optString("role", ""),
            goals = jsonArrayToList(npc.optJSONArray("goals")),
            secretIds = jsonArrayToList(npc.optJSONArray("secretIds"))
          )
        )
      }
    }

    // Parse Enemies
    val enemiesArr = root.optJSONArray("enemies")
    val enemyList = mutableListOf<AdventureEnemy>()
    val enemyIds = mutableSetOf<String>()
    if (enemiesArr != null) {
      for (i in 0 until enemiesArr.length()) {
        val enemy = enemiesArr.optJSONObject(i) ?: continue
        val enemyId = enemy.optString("id", "").trim()
        val name = enemy.optString("name", "").trim()
        if (enemyId.isEmpty()) errors.add("Inimigo na posição $i sem 'id'.")
        if (!enemyIds.add(enemyId)) errors.add("ID de inimigo duplicado: '$enemyId'.")
        val rulesRef = enemy.optJSONObject("rulesRef")
        val rulesSource = rulesRef?.optString("source", "")?.trim() ?: ""
        if (rulesSource.isEmpty()) {
          errors.add("Inimigo '$enemyId' requer 'rulesRef.source' válido.")
        }
        enemyList.add(
          AdventureEnemy(
            id = enemyId,
            name = name,
            kind = enemy.optString("kind", ""),
            statblockId = rulesRef?.optString("statblockId", "") ?: "",
            rulesSource = rulesRef?.optString("source", "") ?: "",
            personality = enemy.optString("personality", ""),
            goals = jsonArrayToList(enemy.optJSONArray("goals")),
            combatOptional = enemy.optBoolean("combatOptional", true)
          )
        )
      }
    }

    // Parse Encounters
    val encountersArr = root.optJSONArray("encounters")
    val encounterList = mutableListOf<AdventureEncounter>()
    if (encountersArr != null) {
      for (i in 0 until encountersArr.length()) {
        val enc = encountersArr.optJSONObject(i) ?: continue
        val encId = enc.optString("id", "").trim()
        val encLocId = enc.optString("locationId", "").trim()
        if (encLocId.isNotEmpty() && !locationIds.contains(encLocId)) {
          errors.add("Encontro '$encId' referencia locationId inexistente '$encLocId'.")
        }
        val encEnemies = jsonArrayToList(enc.optJSONArray("enemies"))
        for (e in encEnemies) {
          if (!enemyIds.contains(e)) {
            errors.add("Encontro '$encId' referencia inimigo inexistente '$e'.")
          }
        }
        encounterList.add(
          AdventureEncounter(
            id = encId,
            locationId = encLocId,
            enemies = encEnemies,
            triggerFlag = enc.optJSONObject("trigger")?.optString("flag"),
            alternatives = jsonArrayToList(enc.optJSONArray("alternatives"))
          )
        )
      }
    }

    // Parse Objectives
    val objArr = root.optJSONArray("objectives")
    val objectiveList = mutableListOf<AdventureObjective>()
    if (objArr != null) {
      for (i in 0 until objArr.length()) {
        val obj = objArr.optJSONObject(i) ?: continue
        val objId = obj.optString("id", "").trim()
        val desc = obj.optString("description", "").trim()
        objectiveList.add(
          AdventureObjective(
            id = objId,
            description = desc,
            optional = obj.optBoolean("optional", false)
          )
        )
      }
    }

    // Parse World Consequences
    val wcArr = root.optJSONArray("worldConsequences")
    val wcList = mutableListOf<WorldConsequence>()
    if (wcArr != null) {
      for (i in 0 until wcArr.length()) {
        val wc = wcArr.optJSONObject(i) ?: continue
        wcList.add(
          WorldConsequence(
            id = wc.optString("id", ""),
            condition = wc.optString("condition", ""),
            effects = jsonArrayToList(wc.optJSONArray("effects"))
          )
        )
      }
    }

    if (errors.isNotEmpty()) {
      return ValidationResult.Invalid(errors)
    }

    val pkg = AdventurePackage(
      schema = schema,
      id = id,
      system = system,
      worldId = worldId,
      title = title,
      author = root.optString("author", ""),
      description = root.optString("description", ""),
      tags = jsonArrayToList(root.optJSONArray("tags")),
      start = AdventureStart(
        locationId = startLocationId,
        hook = startObj?.optString("hook", "") ?: ""
      ),
      story = AdventureStory(
        premise = premise,
        background = storyObj?.optString("background", "") ?: "",
        playerIntroduction = storyObj?.optString("playerIntroduction", "") ?: "",
        themes = jsonArrayToList(storyObj?.optJSONArray("themes")),
        possibleEndings = jsonArrayToList(storyObj?.optJSONArray("possibleEndings"))
      ),
      locations = locationList,
      npcs = npcList,
      secrets = secretList,
      enemies = enemyList,
      encounters = encounterList,
      objectives = objectiveList,
      worldConsequences = wcList,
      rawJson = jsonString
    )

    return ValidationResult.Valid(pkg)
  }

  private fun jsonArrayToList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) {
      val s = arr.optString(i, "").trim()
      if (s.isNotEmpty()) list.add(s)
    }
    return list
  }
}
