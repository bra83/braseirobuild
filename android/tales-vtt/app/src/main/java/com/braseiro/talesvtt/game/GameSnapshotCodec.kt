package com.braseiro.talesvtt.game

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Deterministic snapshot codec for player/mechanical state.
 * GM-private memory is deliberately stored by GameViewModel in a separate private snapshot.
 */
object GameSnapshotCodec {
  private const val VERSION = 15

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
    line("rules.profile", state.rulesProfile.name)
    line("kid.id", enc(state.kid.id))
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
    line("kid.favoriteSong", enc(state.kid.favoriteSong))
    line("kid.birthday", enc(state.kid.birthday))
    line("kid.shameText", enc(state.kid.shameText))
    line("kid.shameAvailable", state.kid.shameAvailable)
    line("kid.scars", state.kid.scars.joinToString("|") { listOf(enc(it.text),it.accepted.toString(),it.usedForSuccess.toString()).joinToString("~") })
    line("kid.hooks", state.kid.hooks.joinToString("|") { enc(it) })
    line("kid.friction", enc(state.kid.friction))
    line("kid.removedFromPlay", state.kid.removedFromPlay)
    line("kid.scarPending", state.kid.scarPending)
    line("kid.items", state.kid.items.joinToString("|") { item ->
      listOf(enc(item.name), enc(item.description), item.bonus.toString(), item.kind.name).joinToString("~")
    })
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
    line("change.characterReviewConfirmed", state.change.characterReviewConfirmed)
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
      line("test.info", test.informationBonus)
      line("test.purpose", test.purpose.name)
      line("test.targetKid", test.targetKid?.let(::enc) ?: "")
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
      line("result.baseEffectApplied", result.baseEffectApplied)
      line("result.bonusEffects", result.bonusEffects.joinToString("|") { "${enc(it.effectId)}~${it.target?.let(::enc) ?: ""}" })
    }

    line("bonus.informationBank", state.informationBonusBank)
    line("bonus.leadershipPool", state.leadershipPoolDice)
    line("bonus.leadershipAssignments", state.leadershipAssignments.entries.joinToString("|") { "${enc(it.key)}~${it.value}" })
    line("bonus.grantedSuccesses", state.grantedSuccesses.entries.joinToString("|") { "${enc(it.key)}~${it.value}" })
    line("bonus.waivedTroubles", state.waivedTroubles.joinToString("|") { enc(it) })
    line("bonus.contactItem", state.contactItemBonusBank)

    line("reveal.clues", state.revealed.clues.joinToString("|") { enc(it) })
    line("reveal.locations", state.revealed.locations.joinToString("|") { enc(it) })
    line("route.history", state.routeHistory.joinToString("|") { enc(it) })
    line("reveal.connections", state.revealed.connections.joinToString("|") { (a, b) -> "${enc(a)}~${enc(b)}" })

    val ext = state.extended
    line("ext.profile", ext.rulesProfile.name)
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

    val kidItems = if (version >= 10) map["kid.items"].orEmpty().split('|')
      .filter { it.isNotBlank() }
      .mapNotNull { raw ->
        val parts = raw.split('~', limit = 4)
        if (parts.size != 4) null else {
          val kind = runCatching { KidItemKind.valueOf(parts[3]) }.getOrNull() ?: return@mapNotNull null
          val item = KidItemState(dec(parts[0]), dec(parts[1]), parts[2].toIntOrNull() ?: 0, kind)
          item.takeIf(CanonicalTalesRules::validItem)
        }
      } else emptyList()

    val kid = KidState(
      id = decoded("kid.id").ifBlank { defaults.id },
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
      favoriteSong = if (version >= 10) decoded("kid.favoriteSong") else defaults.favoriteSong,
      birthday = if (version >= 14) decoded("kid.birthday") else "",
      shameText = if (version >= 14) decoded("kid.shameText") else "",
      shameAvailable = if (version >= 14) bool("kid.shameAvailable") else false,
      scars = if (version >= 14) map["kid.scars"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
        val p=raw.split('~'); if(p.size>=3) FloodScarState(dec(p[0]),p[1].toBooleanStrictOrNull()?:false,p[2].toBooleanStrictOrNull()?:false) else null
      } else emptyList(),
      hooks = if (version >= 14) map["kid.hooks"].orEmpty().split('|').filter{it.isNotBlank()}.map(::dec) else emptyList(),
      friction = if (version >= 14) decoded("kid.friction") else "",
      removedFromPlay = if (version >= 14) bool("kid.removedFromPlay") else false,
      scarPending = if (version >= 15) bool("kid.scarPending") else false,
      items = kidItems,
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
        informationBonus = if (version >= 12) int("test.info") else 0,
        helperKid = decoded("test.helper").ifBlank { null },
        extendedSceneIndex = if (version >= 6) map["test.extendedSceneIndex"]?.toIntOrNull() else null,
        purpose = if (version >= 12) runCatching { TestPurpose.valueOf(map["test.purpose"].orEmpty()) }.getOrDefault(TestPurpose.STANDARD) else TestPurpose.STANDARD,
        targetKid = if (version >= 12) decoded("test.targetKid").ifBlank { null } else null
      )
    } else null

    val result = if (bool("result.present")) {
      TestResolution(
        dice = map["result.dice"].orEmpty().split(',').filter { it.isNotBlank() }.map { it.toInt() },
        successes = int("result.successes"),
        luckUsed = bool("result.luckUsed"),
        pushed = bool("result.pushed"),
        prideUsed = bool("result.prideUsed"),
        reportedToGm = if (version >= 5) bool("result.reportedToGm") else false,
        bonusEffects = if (version >= 12) map["result.bonusEffects"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
          val p=raw.split('~',limit=2); if(p.isEmpty()) null else BonusEffectPurchase(dec(p[0]),p.getOrNull(1)?.takeIf{it.isNotBlank()}?.let(::dec))
        } else emptyList(),
        baseEffectApplied = if (version >= 12) bool("result.baseEffectApplied") else false
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
    val routeHistory = if (version >= 9) {
      map["route.history"].orEmpty().split('|').filter { it.isNotBlank() }.map(::dec).ifEmpty { listOf(scene.location) }
    } else listOf(scene.location)

    fun decodeIntMap(key:String):Map<String,Int> = map[key].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull{raw ->
      val p=raw.split('~',limit=2); p.getOrNull(1)?.toIntOrNull()?.let{dec(p[0]) to it}
    }.toMap()

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
      rulesProfile = if (version >= 14) runCatching { RulesProfile.valueOf(map["ext.profile"].orEmpty()) }.getOrDefault(RulesProfile.TALES_KIDS) else RulesProfile.TALES_KIDS,
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
      newKidRequired = bool("change.newKidRequired"),
      characterReviewConfirmed = if (version >= 11) bool("change.characterReviewConfirmed") else false
    ) else ChangeState()

    GameState(
      rulesProfile = if(version>=14) runCatching{RulesProfile.valueOf(map["rules.profile"].orEmpty())}.getOrDefault(RulesProfile.TALES_KIDS) else RulesProfile.TALES_KIDS,
      kid = kid,
      group = group,
      scene = scene,
      lastPlayerAction = decoded("lastAction").ifBlank { null },
      activeTest = test,
      testResolution = result,
      revealed = revealed,
      routeHistory = routeHistory,
      extended = extended,
      xpAnswers = xpAnswers,
      sessionXpAwarded = bool("mysteryXpApplied"),
      endingMode = endingMode,
      change = change,
      informationBonusBank = if (version >= 12) int("bonus.informationBank") else 0,
      leadershipPoolDice = if (version >= 12) int("bonus.leadershipPool") else 0,
      leadershipAssignments = if (version >= 12) decodeIntMap("bonus.leadershipAssignments") else emptyMap(),
      grantedSuccesses = if (version >= 12) decodeIntMap("bonus.grantedSuccesses") else emptyMap(),
      waivedTroubles = if (version >= 12) map["bonus.waivedTroubles"].orEmpty().split('|').filter{it.isNotBlank()}.map(::dec).toSet() else emptySet(),
      contactItemBonusBank = if (version >= 13) int("bonus.contactItem").coerceIn(0,3) else 0,
      lastError = null
    )
  }.getOrNull()
}