package com.braseiro.talesvtt.data

import android.util.Base64
import com.braseiro.talesvtt.game.Condition
import com.braseiro.talesvtt.game.KidRelationship
import com.braseiro.talesvtt.game.KidState
import com.braseiro.talesvtt.game.TalesSkillCatalog
import com.braseiro.talesvtt.game.RandomKidFactory
import org.json.JSONArray
import org.json.JSONObject

object KidJsonCodec {
  private const val FORMAT = "braseiro-tales-kid"
  private const val VERSION = 2

  fun encode(kid: KidState): String = JSONObject()
    .put("format", FORMAT)
    .put("version", VERSION)
    .put("profile", "tales_kid")
    .put("kid", JSONObject()
      .put("name", kid.name)
      .put("archetype", kid.archetype)
      .put("portrait_asset", kid.portraitAssetKey)
      .put("portrait_image_base64", kid.portraitImageBase64 ?: JSONObject.NULL)
      .put("description", kid.description)
      .put("problem", kid.problem)
      .put("drive", kid.drive)
      .put("pride", kid.prideText)
      .put("pride_available", kid.prideAvailable)
      .put("anchor", kid.anchorName)
      .put("iconic_item", kid.iconicItemName)
      .put("age", kid.age)
      .put("luck", kid.luck)
      .put("xp", kid.xp)
      .put("broken", kid.broken)
      .put("conditions", JSONArray(kid.conditions.map { it.name }))
      .put("attributes", JSONObject(kid.attributes))
      .put("skills", JSONObject(kid.skills))
      .put("relationships", JSONArray(kid.relationships.map { JSONObject().put("name", it.name).put("description", it.description) }))
    ).toString(2)

  fun decode(text: String): KidState {
    val root = JSONObject(text)
    require(root.optString("format") == FORMAT) { "JSON não é uma ficha BRASEIRO Tales." }
    require(root.optInt("version") in 1..VERSION) { "Versão de ficha não suportada." }
    require(root.optString("profile") == "tales_kid") { "Ficha não pertence ao perfil Tales/Kids." }
    val j = root.getJSONObject("kid")
    val age = j.getInt("age")
    require(age in 10..15) { "Kid deve ter entre 10 e 15 anos." }
    val attrsObj = j.getJSONObject("attributes")
    val canonicalAttrs = listOf("CORPO", "TECNOLOGIA", "CORAÇÃO", "MENTE")
    val attrs = canonicalAttrs.associateWith { key -> attrsObj.optInt(key, -1) }
    require(attrs.values.all { it in 1..5 }) { "Atributos inválidos." }
    require(attrs.values.sum() == age) { "A soma dos Atributos deve acompanhar a idade do Kid." }

    val skillsObj = j.getJSONObject("skills")
    val skills = TalesSkillCatalog.skillToAttribute.keys.associateWith { key -> skillsObj.optInt(key, -1) }
    require(skills.values.all { it in 0..5 }) { "Perícias inválidas." }

    val conditions = buildSet {
      val a = j.optJSONArray("conditions") ?: JSONArray()
      for (i in 0 until a.length()) runCatching { Condition.valueOf(a.getString(i)) }.getOrNull()?.let(::add)
    }
    val relationships = buildList {
      val a = j.optJSONArray("relationships") ?: JSONArray()
      for (i in 0 until a.length()) {
        val r = a.optJSONObject(i) ?: continue
        val name = r.optString("name").trim()
        val desc = r.optString("description").trim()
        if (name.isNotBlank()) add(KidRelationship(name, desc))
      }
    }
    val portraitImageBase64 = j.optString("portrait_image_base64", "").takeIf { it.isNotBlank() && it != "null" }
    require(portraitImageBase64 == null || portraitImageBase64.length <= 1_500_000) { "Imagem do retrato excede o limite permitido." }
    if (portraitImageBase64 != null) {
      require(runCatching { Base64.decode(portraitImageBase64, Base64.DEFAULT) }.getOrNull()?.isNotEmpty() == true) { "Imagem do retrato em Base64 é inválida." }
    }
    val name = j.optString("name").trim()
    require(name.isNotBlank()) { "Nome do Kid é obrigatório." }
    val luck = j.optInt("luck", 15 - age)
    require(luck in 0..(15 - age)) { "Sorte incompatível com a idade." }
    val xp = j.optInt("xp", 0)
    require(xp in 0..10) { "XP do Kid deve permanecer entre 0 e 10." }
    val kid = KidState(
      name = name,
      archetype = j.optString("archetype", "Kid"),
      portraitAssetKey = j.optString("portrait_asset", "tales_type_computer_geek"),
      portraitImageBase64 = portraitImageBase64,
      description = j.optString("description"),
      problem = j.optString("problem"),
      drive = j.optString("drive"),
      prideText = j.optString("pride"),
      anchorName = j.optString("anchor"),
      iconicItemName = j.optString("iconic_item"),
      relationships = relationships,
      age = age,
      luck = luck,
      conditions = conditions,
      broken = j.optBoolean("broken", false),
      prideAvailable = j.optBoolean("pride_available", true),
      xp = xp,
      attributes = attrs,
      skills = skills
    )
    require(RandomKidFactory.validatePersistentState(kid)) { "Estado persistido do Kid viola os contratos Tales/Kids." }
    return kid
  }
}
