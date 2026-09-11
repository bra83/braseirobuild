package com.braseiro.talesvtt.game

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Deterministic snapshot codec for player/mechanical state.
 * GM-private memory is deliberately stored by GameViewModel in a separate private snapshot.
 */
object GameSnapshotCodec {
  private const val VERSION = 8

  private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
  private fun dec(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

  private fun encodeMap(map: Map<String, Int>): String = map.entries.joinToString("|") { (k, v) -> "${enc(k)}~$v" }
  private fun decodeMap(raw: String): Map<String, Int> = raw.split('|').filter { it.isNotBlank() }.mapNotNull { item ->
    val parts = item.split('~', limit = 2)
    if (parts.size == 2) parts[1].toIntOrNull()?.let { dec(parts[0]) to it } else null
  }.toMap()

  fun encode(state: GameState): String = buildString {
    fun line(key: String, value: Any?) { append(key).append('=').append(value ?: "").append('\n') }
    line("version", VERSION)
    line("kid.name", enc(state.kid.name))
    line("kid.archetype", enc(state.kid.archetype))
    line("kid.portrait", enc(state.kid.portraitAssetKey))
    line("kid.portraitImageBase64", state.kid.portraitImageBase64?.let(::enc) ?: "")
    line("kid.description", enc(state.kid.description))
    line("kid.problem", enc(state.kid.problem))
    line("kid.drive", enc(state.kid.drive))
    line("kid.prideText", enc(state.kid.prideText))
    line("kid.anchor", enc(state.kid.anchorName))
    line("kid.iconicItem", enc(state.kid.iconicItemName))
    line("kid.relationships", state.kid.relationships.joinToString("|") { "${enc(it.name)}~${enc(it.description)}" })
    line("kid.age", state.kid.age)
    line("kid.luck", state.kid.luck)
    line("kid.conditions", state.kid.conditions.joinToString(",") { it.name })
    line("kid.broken", state.kid.broken)
    line("kid.pride", state.kid.prideAvailable)
    line("kid.xp", state.kid.xp)
    line("kid.attributes", encodeMap(state.kid.attributes))
    line("kid.skills", encodeMap(state.kid.skills))
    line("lastAction", state.lastPlayerAction?.let(::enc) ?: "")
    line("mysteryXpApplied", state.sessionXpAwarded)
    line("ending.mode", state.endingMode.name)
    line("change.birthdayReviewed", state.change.birthdayReviewed)
    line("change.birthdayPassed", state.change.birthdayPassed)
    line("change.birthdayApplied", state.change.birthdayApplied)
    line("change.improvedAttribute", state.change.improvedAttribute?.let(::enc) ?: "")
    line("change.newKidRequired", state.change.newKidRequired)
    line("xp.answers", state.xpAnswers.joinToString(",") { when (it) { true -> "1"; false -> "0"; null -> "_" } })

    val scene = state.scene
    line("scene.title", enc(scene.title))
    line("scene.chapter", enc(scene.chapter))
    line("scene.seasonYear", enc(scene.seasonYear))
    line("scene.location", enc(scene.location))
    line("scene.weather", enc(scene.weather))
    line("scene.time", enc(scene.time))
    line("scene.daylight", enc(scene.daylight))
    line("scene.visual", enc(scene.visualAssetKey))
    line("scene.importedImageUri", scene.importedImageUri?.let(::enc) ?: "")
    line("scene.narrative", scene.narrative.joinToString("|") { enc(it) })
    line("scene.choices", scene.choices.joinToString("|") { "${enc(it.title)}~${enc(it.description)}" })

    val group = state.group
    line("group.members", group.members.joinToString("|") {
      listOf(
        enc(it.name),
        enc(it.archetype),
        enc(it.portraitAssetKey),
        it.conditions.joinToString(",") { condition -> condition.name },
        it.broken.toString()
      ).joinToString("~")
    })
    line("group.relationships", group.relationships.joinToString("|") { listOf(enc(it.left), enc(it.relation), enc(it.right)).joinToString("~") })
    line("group.hideoutName", enc(group.hideoutName))
    line("group.hideoutDescription", enc(group.hideoutDescription))
    line("group.notes", group.notes.joinToString("|") { enc(it) })

    val test = state.activeTest
    line("test.present", test != null)
    if (test != null) {
      line("test.reason", enc(test.reason))
      line("test.attrName", enc(test.attributeName))
      line("test.attrValue", test.attributeValue)
      line("test.skillName", test.skillName?.let(::enc) ?: "")
      line("test.skillValue", test.skillValue)
      line("test.required", test.requiredSuccesses)
      line("test.item", test.approvedItemBonus)
      line("test.helper", test.helperKid?.let(::enc) ?: "")
      line("test.extendedSceneIndex", test.extendedSceneIndex ?: "")
    }

    val result = state.testResolution
    line("result.present", result != null)
    if (result != null) {
      line("result.dice", result.dice.joinToString(","))
      line("result.successes", result.successes)
      line("result.luckUsed", result.luckUsed)
      line("result.pushed", result.pushed)
      line("result.prideUsed", result.prideUsed)
      line("result.reportedToGm", result.reportedToGm)
    }

    line("reveal.clues", state.revealed.clues.joinToString("|") { enc(it) })
    line("reveal.locations", state.revealed.locations.joinToString("|") { enc(it) })
    line("reveal.connections", state.revealed.connections.joinToString("|") { (a, b) -> "${enc(a)}~${enc(b)}" })

    val ext = state.extended
    line("ext.active", ext.active)
    line("ext.stakes", enc(ext.stakes))
    line("ext.kids", ext.kids)
    line("ext.threat", ext.threatLevel.name)
    line("ext.extraConditions", ext.acceptedExtraConditions)
    line("ext.compromiseDeclined", ext.compromiseDeclined)
    line("ext.compromiseMarks", ext.compromiseMarks.joinToString("|") { mark ->
      "${enc(mark.kid)}~${mark.condition?.name ?: "_BROKEN_"}"
    })
    line("ext.scenes", ext.scenes.joinToString("|") { sceneItem ->
      listOf(enc(sceneItem.kid), enc(sceneItem.skill), sceneItem.successes?.toString() ?: "").joinToString("~")
    })
  }

  fun decode(text: String): GameState? = runCatching {
    val map = text.lineSequence()
      .filter { it.contains('=') }
      .associate { line -> line.substringBefore('=') to line.substringAfter('=') }

    val version = map["version"]?.toIntOrNull() ?: return null
    if (version !in 1..VERSION) return null

    fun bool(key: String, default: Boolean = false) = map[key]?.toBooleanStrictOrNull() ?: default
    fun int(key: String, default: Int = 0) = map[key]?.toIntOrNull() ?: default
    fun decoded(key: String): String = map[key]?.takeIf { it.isNotEmpty() }?.let(::dec).orEmpty()

    val defaults = KidState()
    val conditions = map["kid.conditions"].orEmpty()
      .split(',')
      .filter { it.isNotBlank() }
      .mapNotNull { runCatching { Condition.valueOf(it) }.getOrNull() }
      .toSet()

    val kidRelationships = if (version >= 3) map["kid.relationships"].orEmpty().split('|')
      .filter { it.isNotBlank() }
      .mapNotNull { raw ->
        val parts = raw.split('~', limit = 2)
        if (parts.size == 2) KidRelationship(dec(parts[0]), dec(parts[1])) else null
      }.ifEmpty { defaults.relationships } else defaults.relationships

    val kid = KidState(
      name = decoded("kid.name").ifBlank { defaults.name },
      archetype = if (version >= 3) decoded("kid.archetype").ifBlank { defaults.archetype } else defaults.archetype,
      portraitAssetKey = if (version >= 3) decoded("kid.portrait").ifBlank { defaults.portraitAssetKey } else defaults.portraitAssetKey,
      portraitImageBase64 = if (version >= 8) decoded("kid.portraitImageBase64").ifBlank { null } else null,
      description = if (version >= 3) decoded("kid.description").ifBlank { defaults.description } else defaults.description,
      problem = if (version >= 3) decoded("kid.problem").ifBlank { defaults.problem } else defaults.problem,
      drive = if (version >= 3) decoded("kid.drive").ifBlank { defaults.drive } else defaults.drive,
      prideText = if (version >= 3) decoded("kid.prideText").ifBlank { defaults.prideText } else defaults.prideText,
      anchorName = if (version >= 3) decoded("kid.anchor").ifBlank { defaults.anchorName } else defaults.anchorName,
      iconicItemName = if (version >= 3) decoded("kid.iconicItem").ifBlank { defaults.iconicItemName } else defaults.iconicItemName,
      relationships = kidRelationships,
      age = int("kid.age", 12),
      luck = int("kid.luck", 3),
      conditions = conditions,
      broken = bool("kid.broken"),
      prideAvailable = bool("kid.pride", true),
      xp = int("kid.xp", 3),
      attributes = decodeMap(map["kid.attributes"].orEmpty()).ifEmpty { defaults.attributes },
      skills = decodeMap(map["kid.skills"].orEmpty()).ifEmpty { defaults.skills }
    )

    val defaultScene = SceneState()
    val narrative = map["scene.narrative"].orEmpty().split('|').filter { it.isNotBlank() }.map(::dec)
    val choices = map["scene.choices"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val parts = raw.split('~', limit = 2)
      if (parts.size == 2) SceneChoice(dec(parts[0]), dec(parts[1])) else null
    }
    val scene = if (version >= 2) SceneState(
      title = decoded("scene.title").ifBlank { defaultScene.title },
      chapter = decoded("scene.chapter").ifBlank { defaultScene.chapter },
      seasonYear = if (version >= 3) decoded("scene.seasonYear").ifBlank { defaultScene.seasonYear } else defaultScene.seasonYear,
      location = decoded("scene.location").ifBlank { defaultScene.location },
      weather = decoded("scene.weather").ifBlank { defaultScene.weather },
      time = decoded("scene.time").ifBlank { defaultScene.time },
      daylight = decoded("scene.daylight").ifBlank { defaultScene.daylight },
      visualAssetKey = decoded("scene.visual").ifBlank { defaultScene.visualAssetKey },
      importedImageUri = if (version >= 7) decoded("scene.importedImageUri").ifBlank { null } else null,
      narrative = if (map.containsKey("scene.narrative")) narrative else defaultScene.narrative,
      choices = if (map.containsKey("scene.choices")) choices else defaultScene.choices
    ) else defaultScene

    val defaultGroup = GroupState()
    val group = if (version >= 3) {
      val members = map["group.members"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
        val parts = raw.split('~')
        if (parts.size >= 3) {
          val memberConditions = if (version >= 4 && parts.size >= 4) {
            parts[3].split(',').filter { it.isNotBlank() }.mapNotNull { name ->
              runCatching { Condition.valueOf(name) }.getOrNull()
            }.toSet()
          } else emptySet()
          val memberBroken = if (version >= 4 && parts.size >= 5) parts[4].toBooleanStrictOrNull() ?: false else false
          GroupMemberState(dec(parts[0]), dec(parts[1]), dec(parts[2]), memberConditions, memberBroken)
        } else null
      }.ifEmpty { defaultGroup.members }
      val relationships = map["group.relationships"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
        val parts = raw.split('~')
        if (parts.size >= 3) GroupRelationshipState(dec(parts[0]), dec(parts[1]), dec(parts[2])) else null
      }.ifEmpty { defaultGroup.relationships }
      GroupState(
        members = members,
        relationships = relationships,
        hideoutName = decoded("group.hideoutName").ifBlank { defaultGroup.hideoutName },
        hideoutDescription = decoded("group.hideoutDescription").ifBlank { defaultGroup.hideoutDescription },
        notes = map["group.notes"].orEmpty().split('|').filter { it.isNotBlank() }.map(::dec).ifEmpty { defaultGroup.notes }
      )
    } else defaultGroup

    val test = if (bool("test.present")) {
      TestRequest(
        reason = decoded("test.reason"),
        attributeName = decoded("test.attrName"),
        attributeValue = int("test.attrValue"),
        skillName = decoded("test.skillName").ifBlank { null },
        skillValue = int("test.skillValue"),
        requiredSuccesses = int("test.required", 1),
        approvedItemBonus = int("test.item"),
        helperKid = decoded("test.helper").ifBlank { null },
        extendedSceneIndex = if (version >= 6) map["test.extendedSceneIndex"]?.toIntOrNull() else null
      )
    } else null

    val result = if (bool("result.present")) {
      TestResolution(
        dice = map["result.dice"].orEmpty().split(',').filter { it.isNotBlank() }.map { it.toInt() },
        successes = int("result.successes"),
        luckUsed = bool("result.luckUsed"),
        pushed = bool("result.pushed"),
        prideUsed = bool("result.prideUsed"),
        reportedToGm = if (version >= 5) bool("result.reportedToGm") else false
      )
    } else null

    fun decodeSet(key: String): Set<String> = map[key].orEmpty().split('|')
      .filter { it.isNotBlank() }.map(::dec).toSet()

    val connections = map["reveal.connections"].orEmpty().split('|')
      .filter { it.isNotBlank() }
      .mapNotNull { raw ->
        val parts = raw.split('~', limit = 2)
        if (parts.size == 2) dec(parts[0]) to dec(parts[1]) else null
      }.toSet()

    val revealed = RevealedState(
      clues = decodeSet("reveal.clues"),
      locations = decodeSet("reveal.locations"),
      connections = connections
    )

    val scenes = map["ext.scenes"].orEmpty().split('|')
      .filter { it.isNotBlank() }
      .mapNotNull { raw ->
        val parts = raw.split('~')
        if (parts.size >= 3) ExtendedKidScene(dec(parts[0]), dec(parts[1]), parts[2].toIntOrNull()) else null
      }

    val compromiseMarks = if (version >= 4) map["ext.compromiseMarks"].orEmpty().split('|')
      .filter { it.isNotBlank() }
      .mapNotNull { raw ->
        val parts = raw.split('~', limit = 2)
        if (parts.size != 2) null else {
          val condition = if (parts[1] == "_BROKEN_") null else runCatching { Condition.valueOf(parts[1]) }.getOrNull()
          if (parts[1] != "_BROKEN_" && condition == null) null else CompromiseMark(dec(parts[0]), condition)
        }
      } else emptyList()

    val threat = runCatching { ThreatLevel.valueOf(map["ext.threat"].orEmpty()) }.getOrDefault(ThreatLevel.DIFFICULT)
    val extended = ExtendedTroubleState(
      active = bool("ext.active"),
      stakes = decoded("ext.stakes"),
      kids = int("ext.kids", 4),
      threatLevel = threat,
      scenes = scenes,
      acceptedExtraConditions = int("ext.extraConditions"),
      compromiseMarks = compromiseMarks,
      compromiseDeclined = if (version >= 6) bool("ext.compromiseDeclined") else false
    )

    val xpAnswers = if (version >= 2) map["xp.answers"].orEmpty().split(',').take(5).map {
      when (it) { "1" -> true; "0" -> false; else -> null }
    }.let { it + List((5 - it.size).coerceAtLeast(0)) { null } } else List(5) { null }

    val endingMode = if (version >= 6) runCatching { EndingMode.valueOf(map["ending.mode"].orEmpty()) }.getOrDefault(EndingMode.NONE) else EndingMode.NONE
    val change = if (version >= 6) ChangeState(
      birthdayReviewed = bool("change.birthdayReviewed"),
      birthdayPassed = bool("change.birthdayPassed"),
      birthdayApplied = bool("change.birthdayApplied"),
      improvedAttribute = decoded("change.improvedAttribute").ifBlank { null },
      newKidRequired = bool("change.newKidRequired")
    ) else ChangeState()

    GameState(
      kid = kid,
      group = group,
      scene = scene,
      lastPlayerAction = decoded("lastAction").ifBlank { null },
      activeTest = test,
      testResolution = result,
      revealed = revealed,
      extended = extended,
      xpAnswers = xpAnswers,
      sessionXpAwarded = bool("mysteryXpApplied"),
      endingMode = endingMode,
      change = change,
      lastError = null
    )
  }.getOrNull()
}
