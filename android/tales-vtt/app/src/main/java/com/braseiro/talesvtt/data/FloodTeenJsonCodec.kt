package com.braseiro.talesvtt.data

import android.util.Base64
import com.braseiro.talesvtt.game.FloodCondition
import com.braseiro.talesvtt.game.FloodRelationship
import com.braseiro.talesvtt.game.FloodScar
import com.braseiro.talesvtt.game.FloodShameState
import com.braseiro.talesvtt.game.FloodSkillCatalog
import com.braseiro.talesvtt.game.FloodTeenState
import com.braseiro.talesvtt.game.FloodTypeCatalog
import org.json.JSONArray
import org.json.JSONObject

/** Portable character-sheet codec for the mechanically isolated Flood/Teens profile. */
object FloodTeenJsonCodec {
  private const val FORMAT = "braseiro-flood-teen"
  private const val VERSION = 1

  fun encode(teen: FloodTeenState): String = JSONObject()
    .put("format", FORMAT)
    .put("version", VERSION)
    .put("profile", "flood_teen")
    .put("teen", JSONObject()
      .put("name", teen.name)
      .put("age", teen.age)
      .put("type", teen.typeId)
      .put("portrait_asset", teen.portraitAssetKey)
      .put("portrait_image_base64", teen.portraitImageBase64 ?: JSONObject.NULL)
      .put("description", teen.description)
      .put("problem", teen.problem)
      .put("drive", teen.drive)
      .put("shame", JSONObject()
        .put("text", teen.shame.text)
        .put("last_used_mystery", teen.shame.lastUsedMystery ?: JSONObject.NULL)
        .put("last_used_session", teen.shame.lastUsedSession ?: JSONObject.NULL)
      )
      .put("anchor", teen.anchorName)
      .put("iconic_item", teen.iconicItemName)
      .put("xp", teen.xp)
      .put("broken", teen.broken)
      .put("removed", teen.removed)
      .put("removal_pending", teen.removalPending)
      .put("conditions", JSONArray(teen.conditions.map { it.name }))
      .put("attributes", JSONObject(teen.attributes))
      .put("skills", JSONObject(teen.skills))
      .put("relationships", JSONArray(teen.relationships.map {
        JSONObject().put("name", it.name).put("description", it.description)
      }))
      .put("hooks", JSONArray(teen.hooks))
      .put("scars", JSONArray(teen.scars.map {
        JSONObject()
          .put("id", it.id)
          .put("text", it.text)
          .put("accepted", it.accepted)
          .put("death_roll", it.deathRoll ?: JSONObject.NULL)
          .put("decisive", it.decisive)
      }))
    ).toString(2)

  fun decode(text: String): FloodTeenState {
    val root = JSONObject(text)
    require(root.optString("format") == FORMAT) { "JSON não é uma ficha BRASEIRO Flood." }
    require(root.optInt("version") in 1..VERSION) { "Versão de ficha Flood não suportada." }
    require(root.optString("profile") == "flood_teen") { "Ficha não pertence ao perfil Flood/Teens." }
    val j = root.getJSONObject("teen")
    require(!j.has("luck") && !j.has("pride") && !j.has("pride_available")) {
      "Ficha Flood contaminada por recursos Tales/Kids."
    }

    val name = j.optString("name").trim()
    require(name.isNotBlank()) { "Nome do Teen é obrigatório." }
    val age = j.getInt("age")
    require(age in 14..19) { "Teen deve ter entre 14 e 19 anos." }
    val typeId = j.optString("type").trim()
    require(FloodTypeCatalog.resolve(typeId) != null) { "Tipo Flood inválido." }

    val attrsObj = j.getJSONObject("attributes")
    val canonicalAttrs = listOf("BODY", "TECH", "HEART", "MIND")
    val attrs = canonicalAttrs.associateWith { key -> attrsObj.optInt(key, -1) }
    require(attrs.values.all { it in 1..5 } && attrs.values.sum() == 14) {
      "Atributos Flood inválidos: quatro Atributos 1..5, soma 14."
    }

    val skillsObj = j.getJSONObject("skills")
    val skills = FloodSkillCatalog.skillToAttribute.keys.associateWith { key -> skillsObj.optInt(key, -1) }
    require(skills.values.all { it in 0..5 }) { "Skills Flood inválidas." }

    val conditions = buildSet {
      val a = j.optJSONArray("conditions") ?: JSONArray()
      for (i in 0 until a.length()) runCatching { FloodCondition.valueOf(a.getString(i)) }.getOrNull()?.let(::add)
    }

    val relationships = buildList {
      val a = j.optJSONArray("relationships") ?: JSONArray()
      for (i in 0 until a.length()) {
        val r = a.optJSONObject(i) ?: continue
        val n = r.optString("name").trim()
        if (n.isNotBlank()) add(FloodRelationship(n, r.optString("description").trim()))
      }
    }

    val hooks = buildList {
      val a = j.optJSONArray("hooks") ?: JSONArray()
      for (i in 0 until a.length()) a.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
    }

    val scars = buildList {
      val a = j.optJSONArray("scars") ?: JSONArray()
      require(a.length() <= 6) { "Quantidade de Scars inválida." }
      for (i in 0 until a.length()) {
        val s = a.optJSONObject(i) ?: continue
        val id = s.optString("id").trim().ifBlank { "scar-${i + 1}" }
        val scarText = s.optString("text").trim()
        require(scarText.isNotBlank()) { "Scar precisa de descrição." }
        val deathRoll = if (s.isNull("death_roll")) null else s.optInt("death_roll", -1).also {
          require(it in 0..6) { "death_roll de Scar inválido." }
        }
        add(FloodScar(
          id = id,
          text = scarText,
          accepted = s.optBoolean("accepted", false),
          deathRoll = deathRoll,
          decisive = s.optBoolean("decisive", false)
        ))
      }
    }

    val shameObj = j.optJSONObject("shame") ?: error("Shame obrigatório.")
    val shameText = shameObj.optString("text").trim()
    require(shameText.isNotBlank()) { "Shame obrigatório." }
    val shame = FloodShameState(
      text = shameText,
      lastUsedMystery = shameObj.optString("last_used_mystery", "").takeIf { it.isNotBlank() && it != "null" },
      lastUsedSession = shameObj.optString("last_used_session", "").takeIf { it.isNotBlank() && it != "null" }
    )

    val portraitImageBase64 = j.optString("portrait_image_base64", "").takeIf { it.isNotBlank() && it != "null" }
    require(portraitImageBase64 == null || portraitImageBase64.length <= 1_500_000) {
      "Imagem do retrato excede o limite permitido."
    }
    if (portraitImageBase64 != null) {
      require(runCatching { Base64.decode(portraitImageBase64, Base64.DEFAULT) }.getOrNull()?.isNotEmpty() == true) {
        "Imagem do retrato em Base64 é inválida."
      }
    }

    val teen = FloodTeenState(
      name = name,
      age = age,
      typeId = typeId,
      portraitAssetKey = j.optString("portrait_asset", "flood_type_hacker"),
      portraitImageBase64 = portraitImageBase64,
      description = j.optString("description"),
      problem = j.optString("problem"),
      drive = j.optString("drive"),
      shame = shame,
      anchorName = j.optString("anchor"),
      iconicItemName = j.optString("iconic_item"),
      relationships = relationships,
      hooks = hooks,
      conditions = conditions,
      broken = j.optBoolean("broken", false),
      scars = scars,
      removed = j.optBoolean("removed", false),
      removalPending = j.optBoolean("removal_pending", false),
      xp = j.optInt("xp", 0),
      attributes = attrs,
      skills = skills
    )
    require(teen.runtimeErrors().isEmpty()) { teen.runtimeErrors().joinToString(" ") }
    return teen
  }
}
