package com.braseiro.talesvtt.game

import org.json.JSONArray
import org.json.JSONObject

data class MotorParseResult(
  val packet: MotorPacket,
  val rejectedWireActions: List<String>
)

/**
 * Compatibility parser for the proven BRASEIRO GM JSON action format used by the previous
 * functional baseline. Parsing does not imply trust: parsed actions still pass through
 * MotorBarbaraFirewall before touching GameState.
 */
object MotorBarbaraWireAdapter {
  fun parse(raw: String): MotorParseResult {
    val root = parseRoot(raw)
    val actionsJson = when {
      root.optJSONArray("actions") != null -> root.getJSONArray("actions")
      root.optJSONObject("actions") != null -> JSONArray().put(root.getJSONObject("actions"))
      root.optJSONObject("action") != null -> JSONArray().put(root.getJSONObject("action"))
      root.optString("type").isNotBlank() -> JSONArray().put(root)
      else -> JSONArray()
    }
    val parsed = mutableListOf<MotorAction>()
    val rejected = mutableListOf<String>()

    for (i in 0 until actionsJson.length()) {
      val a = actionsJson.optJSONObject(i)
      if (a == null) {
        rejected += "wire[$i]: ação não é objeto."
        continue
      }
      val type = a.optString("type").trim().ifBlank { a.optString("action").trim() }
      val action = runCatching { parseAction(type, a) }.getOrNull()
      if (action == null) rejected += "wire[$i]: ação '$type' inválida ou não suportada."
      else parsed += action
    }

    return MotorParseResult(MotorPacket(actions = parsed), rejected)
  }


  private fun parseRoot(raw: String): JSONObject {
    val trimmed = raw.trim()
    require(trimmed.isNotEmpty()) { "O Mestre respondeu de forma incompleta." }
    val unfenced = Regex("^```(?:json)?\\s*([\\s\\S]*?)\\s*```$", RegexOption.IGNORE_CASE)
      .find(trimmed)?.groupValues?.getOrNull(1)?.trim() ?: trimmed
    val first = unfenced.indexOf('{')
    val last = unfenced.lastIndexOf('}')
    val candidate = if (first >= 0 && last > first) unfenced.substring(first, last + 1) else unfenced
    val normalized = normalizeJson(candidate)
    return runCatching { JSONObject(normalized) }.getOrElse {
      val salvaged = salvageActions(normalized)
      if (salvaged.length() == 0) throw IllegalArgumentException("O Mestre respondeu de forma incompleta.")
      JSONObject().put("actions", salvaged)
    }
  }

  private fun normalizeJson(raw: String): String = raw
    .replace(Regex(",\\s*([}\\]])"), "$1")
    .replace(Regex("\"action\"\\s*:"), "\"type\":")

  private fun salvageActions(raw: String): JSONArray {
    val result = JSONArray()
    val actionsAt = raw.indexOf("\"actions\"")
    val arrayAt = if (actionsAt >= 0) raw.indexOf('[', actionsAt) else -1
    if (arrayAt < 0) return result
    var depth = 0
    var inString = false
    var escaped = false
    var startObject = -1
    for (i in arrayAt + 1 until raw.length) {
      val c = raw[i]
      if (inString) {
        if (escaped) escaped = false
        else if (c == '\\') escaped = true
        else if (c == '"') inString = false
        continue
      }
      if (c == '"') { inString = true; continue }
      if (c == '{') {
        if (depth == 0) startObject = i
        depth++
      } else if (c == '}' && depth > 0) {
        depth--
        if (depth == 0 && startObject >= 0) {
          val objText = normalizeJson(raw.substring(startObject, i + 1))
          runCatching { JSONObject(objText) }.getOrNull()?.let { result.put(it) }
          startObject = -1
        }
      } else if (c == ']' && depth == 0) break
    }
    return result
  }

  private fun parseAction(type: String, a: JSONObject): MotorAction? {
    return when (type) {
    "narration" -> {
      val text = a.optString("text").trim()
      if (text.isEmpty()) null else MotorAction.Narration(text, parseChoices(a.opt("choices") ?: a.opt("options")))
    }

    "scene_visual" -> a.optString("asset").takeIf { it.isNotBlank() }?.let {
      MotorAction.WorldPatch(visualAssetKey = it.removeSuffix(".png"))
    }

    "world_patch" -> {
      val p = a.optJSONObject("patch") ?: JSONObject()
      MotorAction.WorldPatch(
        location = p.optStringOrNull("location"),
        weather = p.optStringOrNull("weather"),
        time = p.optStringOrNull("time"),
        daylight = p.optStringOrNull("daylight")
      )
    }

    "test_request" -> {
      val skill = a.optString("skill").trim()
      val purpose=when(a.optString("purpose","standard").lowercase()) {
        "build_item","build","tinker_build" -> TestPurpose.BUILD_ITEM
        "program_item","program_create" -> TestPurpose.PROGRAM_ITEM
        "leadership_prepare","lead_prepare" -> TestPurpose.LEADERSHIP_PREPARE
        "leadership_comfort","comfort" -> TestPurpose.LEADERSHIP_COMFORT
        "questioning","questions" -> TestPurpose.QUESTIONING
        else -> TestPurpose.STANDARD
      }
      if (skill.isEmpty()) null else MotorAction.TestRequest(skill,a.optInt("required",1),a.optString("label",skill),purpose,a.optString("targetKid").trim().ifBlank{null})
    }

    "test_clear" -> MotorAction.TestClear

    "help_approve" -> a.optString("helperKid").takeIf { it.isNotBlank() }?.let(MotorAction::ApproveHelp)
    "iconic_item_approve" -> MotorAction.ApproveIconicItem
    "mystery_item_approve", "item_approve" -> {
      val item = a.optString("item", a.optString("name")).trim()
      val bonus = a.optInt("bonus", -1)
      if (item.isBlank() || bonus !in 1..3) null else MotorAction.ApproveMysteryItem(item, bonus)
    }
    "item_add" -> {
      val item = a.optJSONObject("item") ?: a
      val name = item.optString("name").trim()
      val description = item.optString("description").trim()
      val bonus = item.optInt("bonus", 0)
      val kind = when (item.optString("kind").lowercase()) {
        "mystery" -> KidItemKind.MYSTERY
        else -> KidItemKind.ORDINARY
      }
      if (name.isBlank()) null else MotorAction.GiveItem(KidItemState(name, description, bonus, kind))
    }
    "item_remove" -> a.optString("item", a.optString("name")).takeIf { it.isNotBlank() }?.let(MotorAction::RemoveItem)

    "condition" -> {
      if (a.optBoolean("broken", false)) MotorAction.BreakKid
      else when (a.optString("key").lowercase()) {
        "upset", "chateado" -> MotorAction.ApplyCondition(Condition.UPSET)
        "scared", "assustado" -> MotorAction.ApplyCondition(Condition.SCARED)
        "exhausted", "exausto" -> MotorAction.ApplyCondition(Condition.EXHAUSTED)
        "injured", "machucado", "ferido" -> MotorAction.ApplyCondition(Condition.INJURED)
        else -> null
      }
    }

    "recovery" -> {
      val kind = when (a.optString("source", a.optString("kind")).lowercase()) {
        "anchor", "ancora", "âncora" -> RecoveryKind.ANCHOR
        "hideout", "esconderijo" -> RecoveryKind.HIDEOUT
        "comfort", "conforto", "skill" -> RecoveryKind.COMFORT
        else -> null
      }
      val condition = when (a.optString("condition", a.optString("key")).lowercase()) {
        "upset", "chateado" -> Condition.UPSET
        "scared", "assustado" -> Condition.SCARED
        "exhausted", "exausto" -> Condition.EXHAUSTED
        "injured", "machucado", "ferido" -> Condition.INJURED
        else -> null
      }
      MotorAction.Recovery(a.optBoolean("approved", false), kind, condition)
    }

    "secret_clue_add" -> {
      val c = a.optJSONObject("secretClue") ?: a.optJSONObject("clue") ?: return null
      val id = c.optString("id").trim()
      val title = c.optString("title").trim()
      if (id.isEmpty() || title.isEmpty()) null else MotorAction.SecretClueAdd(
        PrivateClue(id, title, c.optString("text"), c.optString("location"))
      )
    }

    "secret_connection_add" -> {
      val c = a.optJSONObject("secretConnection") ?: return null
      val id = c.optString("id").trim()
      val from = c.optString("a").trim()
      val to = c.optString("b").trim()
      if (id.isEmpty() || from.isEmpty() || to.isEmpty()) null
      else MotorAction.SecretConnectionAdd(PrivateConnection(id, from, to))
    }

    "secret_location_add" -> {
      val l = a.optJSONObject("secretLocation") ?: return null
      val id = l.optString("id").trim()
      val name = l.optString("name").trim()
      if (id.isEmpty() || name.isEmpty()) null else MotorAction.SecretLocationAdd(PrivateLocation(id, name))
    }

    "secret_npc_upsert", "secret_npc_add" -> {
      val n=a.optJSONObject("secretNpc") ?: a.optJSONObject("npc") ?: return null
      val id=n.optString("id").trim(); val name=n.optString("name").trim()
      if(id.isEmpty()||name.isEmpty()) null else MotorAction.SecretNpcUpsert(PrivateNpc(id,name,n.optString("role"),n.optString("description"),n.optString("location"),n.optString("status","active")))
    }

    "secret_world_fact_upsert", "secret_fact_upsert" -> {
      val f=a.optJSONObject("secretWorldFact") ?: a.optJSONObject("fact") ?: return null
      val id=f.optString("id").trim(); val subject=f.optString("subject").trim(); val value=f.optString("value").trim()
      if(id.isEmpty()||subject.isEmpty()||value.isEmpty()) null else MotorAction.SecretWorldFactUpsert(PrivateWorldFact(id,subject,value,f.optString("scope","campaign")))
    }

    "secret_countdown_upsert", "secret_countdown_add" -> {
      val c=a.optJSONObject("secretCountdown") ?: a.optJSONObject("countdown") ?: return null
      val id=c.optString("id").trim(); val title=c.optString("title").trim()
      if(id.isEmpty()||title.isEmpty()) null else MotorAction.SecretCountdownUpsert(PrivateCountdown(id,title,c.optInt("stage",0),c.optInt("maxStage",4),c.optString("consequence")))
    }

    "reveal_clue" -> a.optString("clueId").takeIf { it.isNotBlank() }?.let(MotorAction::RevealClue)
    "reveal_connection" -> a.optString("connectionId").takeIf { it.isNotBlank() }?.let(MotorAction::RevealConnection)
    "reveal_location" -> a.optString("locationId").takeIf { it.isNotBlank() }?.let(MotorAction::RevealLocation)

    "extended_patch" -> {
      val patch = a.optJSONObject("patch") ?: return null
      if (!patch.optBoolean("active", true)) MotorAction.EndExtendedTrouble else parseExtendedPatch(patch)
    }
    "extended_scene_result" -> {
      val kid = a.optString("kid").trim()
      val successes = a.optInt("successes", -1)
      if (kid.isEmpty() || successes < 0) null else MotorAction.ExtendedSceneResult(kid, successes)
    }

    "session_end" -> MotorAction.SessionEnd
    "mystery_end" -> MotorAction.MysteryEnd
    "message" -> MotorAction.Message(a.optString("text"))

    else -> null
    }
  }

  private fun parseExtendedPatch(p: JSONObject): MotorAction.StartExtendedTrouble? {
    val stakes = p.optString("stakes").trim()
    val scenesJson = p.optJSONArray("scenes") ?: return null
    val scenes = buildList {
      for (i in 0 until scenesJson.length()) {
        val s = scenesJson.optJSONObject(i) ?: continue
        val kid = s.optString("kid").trim()
        val skill = s.optString("skill").trim()
        if (kid.isNotEmpty() && skill.isNotEmpty()) add(kid to skill)
      }
    }
    if (stakes.isEmpty() || scenes.isEmpty()) return null

    val kids = p.optInt("kids", scenes.size).coerceAtLeast(1)
    val target = p.optInt("threat", 0)
    val named = p.optString("threatLevel", p.optString("level")).lowercase()
    val level = when {
      named.contains("quase") || named.contains("almost") -> ThreatLevel.ALMOST_IMPOSSIBLE
      named.contains("dif") || named.contains("difficult") -> ThreatLevel.DIFFICULT
      named.contains("normal") -> ThreatLevel.NORMAL
      target > 0 && target >= kids * 4 -> ThreatLevel.ALMOST_IMPOSSIBLE
      target > 0 && target >= kids * 3 -> ThreatLevel.DIFFICULT
      else -> ThreatLevel.NORMAL
    }
    return MotorAction.StartExtendedTrouble(stakes, level, scenes)
  }

  private fun parseChoices(value: Any?): List<SceneChoice> {
    val arr = value as? JSONArray ?: return emptyList()
    val result = mutableListOf<SceneChoice>()
    for (i in 0 until minOf(arr.length(), 3)) {
      val item = arr.opt(i)
      val choice = when (item) {
        is JSONArray -> {
          val title = item.optString(0).trim()
          val description = item.optString(1).trim()
          if (title.isNotEmpty() && description.isNotEmpty()) SceneChoice(title, description) else null
        }
        is JSONObject -> {
          val title = item.optString("title", item.optString("titulo", item.optString("label"))).trim()
          val description = item.optString("description", item.optString("descricao", item.optString("text"))).trim()
          if (title.isNotEmpty() && description.isNotEmpty()) SceneChoice(title, description) else null
        }
        else -> null
      }
      if (choice != null) result += choice
    }
    return result
  }

  private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
}
