package com.braseiro.talesvtt.data

import android.util.Base64
import com.braseiro.talesvtt.game.Condition
import com.braseiro.talesvtt.game.FloodScarState
import com.braseiro.talesvtt.game.KidItemKind
import com.braseiro.talesvtt.game.KidItemState
import com.braseiro.talesvtt.game.KidRelationship
import com.braseiro.talesvtt.game.KidState
import com.braseiro.talesvtt.game.RandomKidFactory
import com.braseiro.talesvtt.game.RandomTeenFactory
import com.braseiro.talesvtt.game.RulesProfile
import com.braseiro.talesvtt.game.TalesSkillCatalog
import org.json.JSONArray
import org.json.JSONObject

object KidJsonCodec {
  private const val TALES_FORMAT="braseiro-tales-kid"
  private const val FLOOD_FORMAT="braseiro-flood-teen"
  private const val VERSION=3

  fun encode(kid:KidState):String=encode(RulesProfile.TALES_KIDS,kid)

  fun encode(profile:RulesProfile,kid:KidState):String {
    val format=if(profile==RulesProfile.FLOOD_TEENS) FLOOD_FORMAT else TALES_FORMAT
    return JSONObject()
      .put("format",format).put("version",VERSION).put("profile",profile.wireName)
      .put("kid",JSONObject()
        .put("name",kid.name).put("archetype",kid.archetype).put("portrait_asset",kid.portraitAssetKey)
        .put("portrait_image_base64",kid.portraitImageBase64?:JSONObject.NULL).put("description",kid.description)
        .put("problem",kid.problem).put("drive",kid.drive).put("pride",kid.prideText)
        .put("shame",kid.shameText).put("shame_available",kid.shameAvailable).put("birthday",kid.birthday)
        .put("anchor",kid.anchorName).put("iconic_item",kid.iconicItemName).put("favorite_song",kid.favoriteSong)
        .put("age",kid.age).put("luck",kid.luck).put("xp",kid.xp).put("broken",kid.broken)
        .put("pride_available",kid.prideAvailable).put("removed",kid.removedFromPlay).put("scar_pending",kid.scarPending)
        .put("conditions",JSONArray(kid.conditions.map{it.name})).put("attributes",JSONObject(kid.attributes)).put("skills",JSONObject(kid.skills))
        .put("relationships",JSONArray(kid.relationships.map{JSONObject().put("name",it.name).put("description",it.description)}))
        .put("hooks",JSONArray(kid.hooks)).put("friction",kid.friction)
        .put("scars",JSONArray(kid.scars.map{JSONObject().put("text",it.text).put("accepted",it.accepted).put("used",it.usedForSuccess)}))
        .put("items",JSONArray(kid.items.map{JSONObject().put("name",it.name).put("description",it.description).put("bonus",it.bonus).put("kind",it.kind.name)}))
      ).toString(2)
  }

  fun decode(text:String):KidState=decodeForProfile(text,RulesProfile.TALES_KIDS)

  fun decodeForProfile(text:String,expected:RulesProfile):KidState {
    val root=JSONObject(text)
    val profile=RulesProfile.fromWire(root.optString("profile"))
    require(profile==expected){"Ficha pertence a ${profile.wireName}, mas a campanha ativa usa ${expected.wireName}."}
    val expectedFormat=if(profile==RulesProfile.FLOOD_TEENS) FLOOD_FORMAT else TALES_FORMAT
    val format=root.optString("format")
    require(format==expectedFormat || (profile==RulesProfile.TALES_KIDS && format==TALES_FORMAT)){"Formato de ficha incompatível com o perfil ativo."}
    require(root.optInt("version") in 1..VERSION){"Versão de ficha não suportada."}
    val j=root.optJSONObject("kid") ?: error("Ficha sem personagem.")
    val portrait=j.optString("portrait_image_base64","").takeIf{it.isNotBlank()&&it!="null"}
    require(portrait==null||portrait.length<=1_500_000){"Imagem do retrato excede o limite permitido."}
    if(portrait!=null) require(runCatching{Base64.decode(portrait,Base64.DEFAULT)}.getOrNull()?.isNotEmpty()==true){"Imagem Base64 inválida."}

    val attrs=listOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE").associateWith{j.getJSONObject("attributes").optInt(it,-1)}
    val skills=TalesSkillCatalog.skillToAttribute.keys.associateWith{j.getJSONObject("skills").optInt(it,-1)}
    val conditions=buildSet {
      val a=j.optJSONArray("conditions")?:JSONArray(); for(i in 0 until a.length()) runCatching{Condition.valueOf(a.getString(i))}.getOrNull()?.let(::add)
    }
    val relationships=buildList {
      val a=j.optJSONArray("relationships")?:JSONArray(); for(i in 0 until a.length()) { val r=a.optJSONObject(i)?:continue; val n=r.optString("name").trim(); val d=r.optString("description").trim(); if(n.isNotBlank()) add(KidRelationship(n,d)) }
    }
    val hooks=buildList { val a=j.optJSONArray("hooks")?:JSONArray(); for(i in 0 until a.length()) a.optString(i).trim().takeIf{it.isNotBlank()}?.let(::add) }
    val scars=buildList {
      val a=j.optJSONArray("scars")?:JSONArray(); require(a.length()<=6){"Quantidade de Cicatrizes inválida."}; for(i in 0 until a.length()) { val x=a.optJSONObject(i)?:continue; val text=x.optString("text").trim(); require(text.isNotBlank()){"Cicatriz precisa de descrição."}; add(FloodScarState(text,x.optBoolean("accepted"),x.optBoolean("used"))) }
    }
    val items=buildList {
      val a=j.optJSONArray("items")?:JSONArray(); for(i in 0 until a.length()) { val x=a.optJSONObject(i)?:continue; val kind=runCatching{KidItemKind.valueOf(x.optString("kind"))}.getOrNull()?:continue; add(KidItemState(x.optString("name").trim(),x.optString("description").trim(),x.optInt("bonus"),kind)) }
    }
    val age=j.getInt("age")
    val kid=KidState(
      name=j.optString("name").trim(),archetype=j.optString("archetype").trim(),portraitAssetKey=j.optString("portrait_asset","kid_portrait_01"),portraitImageBase64=portrait,
      description=j.optString("description"),problem=j.optString("problem"),drive=j.optString("drive"),prideText=if(profile==RulesProfile.TALES_KIDS) j.optString("pride") else "",
      anchorName=j.optString("anchor"),iconicItemName=j.optString("iconic_item"),favoriteSong=j.optString("favorite_song"),birthday=j.optString("birthday"),
      shameText=if(profile==RulesProfile.FLOOD_TEENS) j.optString("shame") else "",shameAvailable=profile==RulesProfile.FLOOD_TEENS && j.optBoolean("shame_available",true),
      scars=scars,hooks=hooks,friction=if(profile==RulesProfile.FLOOD_TEENS) j.optString("friction") else "",removedFromPlay=j.optBoolean("removed"),scarPending=j.optBoolean("scar_pending"),
      items=items,relationships=relationships,age=age,luck=if(profile==RulesProfile.FLOOD_TEENS) 0 else j.optInt("luck",(15-age).coerceAtLeast(0)),conditions=conditions,broken=j.optBoolean("broken"),
      prideAvailable=profile==RulesProfile.TALES_KIDS && j.optBoolean("pride_available",true),xp=j.optInt("xp"),attributes=attrs,skills=skills
    )
    require(kid.name.isNotBlank()){"Nome obrigatório."}
    when(profile){
      RulesProfile.TALES_KIDS -> require(RandomKidFactory.validatePersistentState(kid)){"Estado persistido viola Tales/Kids."}
      RulesProfile.FLOOD_TEENS -> require(RandomTeenFactory.validatePersistentState(kid)){"Estado persistido viola Flood/Teens."}
    }
    return kid
  }
}
