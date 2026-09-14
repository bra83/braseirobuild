from pathlib import Path
import re, sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])

# Preserve every green block through 1.3.16c.
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16c-transform.py')}
code=(repo/'tales-1.3.16c-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def one(text,old,new,label):
    if old not in text: raise SystemExit('missing anchor: '+label)
    return text.replace(old,new,1)

# -----------------------------------------------------------------------------
# Shared reducer: Flood Change/XP/age must follow the Flood manual, never Tales.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt'; s=read(rel)
s=one(s,
'''    val prideText: String? = null,
    val iconicItemName: String? = null,''',
'''    val prideText: String? = null,
    val shameText: String? = null,
    val iconicItemName: String? = null,''','change shame field')

old='''      is GameEvent.ApplyMysteryEnd -> {
        if (state.sessionXpAwarded) fail("XP desta sessão já foi aplicado.")
        else if (event.xpGained !in 1..5) fail("XP de sessão deve vir das cinco perguntas.")
        else ok(state.copy(
          kid = state.kid.copy(
            xp = (state.kid.xp + event.xpGained).coerceAtMost(10),
            conditions = emptySet(),
            broken = false,
            prideAvailable = true
          ),
          sessionXpAwarded = true,
          endingMode = EndingMode.MYSTERY
        ))
      }'''
new='''      is GameEvent.ApplyMysteryEnd -> {
        if (state.sessionXpAwarded) fail("XP desta sessão já foi aplicado.")
        else if (event.xpGained !in 1..5) fail("XP de sessão deve vir das cinco perguntas.")
        else if (state.rulesProfile == RulesProfile.FLOOD_TEENS && state.kid.scarPending) fail("Registre a Cicatriz pendente antes de encerrar o Mistério.")
        else {
          val nextKid = when(state.rulesProfile) {
            RulesProfile.TALES_KIDS -> state.kid.copy(
              xp=(state.kid.xp+event.xpGained).coerceAtMost(10),conditions=emptySet(),broken=false,prideAvailable=true
            )
            RulesProfile.FLOOD_TEENS -> state.kid.copy(
              xp=(state.kid.xp+event.xpGained).coerceAtMost(10),conditions=emptySet(),broken=false,luck=0,prideAvailable=false
            )
          }
          ok(state.copy(kid=nextKid,sessionXpAwarded=true,endingMode=EndingMode.MYSTERY))
        }
      }'''
s=one(s,old,new,'profile mystery end')

old='''      GameEvent.ConfirmMysteryEnd -> {
        if (state.endingMode == EndingMode.NONE) fail("O encerramento precisa ser aberto pelo Mestre antes de calcular XP.")
        else if (state.xpAnswers.any { it == null }) fail("Responda às cinco perguntas de XP.")
        else if (state.sessionXpAwarded) fail("XP desta sessão já foi aplicado.")
        else {
          val gained = state.xpAnswers.count { it == true }
          if (gained !in 1..5) fail("Ao menos a participação precisa estar registrada para XP.")
          else ok(state.copy(
            kid = state.kid.copy(
              xp = (state.kid.xp + gained).coerceAtMost(10),
              conditions = if (state.endingMode == EndingMode.MYSTERY) emptySet() else state.kid.conditions,
              broken = if (state.endingMode == EndingMode.MYSTERY) false else state.kid.broken,
              prideAvailable = true
            ),
            sessionXpAwarded = true
          ))
        }
      }'''
new='''      GameEvent.ConfirmMysteryEnd -> {
        if (state.endingMode == EndingMode.NONE) fail("O encerramento precisa ser aberto pelo Mestre antes de calcular XP.")
        else if (state.xpAnswers.any { it == null }) fail("Responda às cinco perguntas de XP.")
        else if (state.sessionXpAwarded) fail("XP desta sessão já foi aplicado.")
        else if (state.rulesProfile == RulesProfile.FLOOD_TEENS && state.kid.scarPending) fail("Registre a Cicatriz pendente antes de concluir o XP.")
        else {
          val gained = state.xpAnswers.count { it == true }
          if (gained !in 1..5) fail("Ao menos a participação precisa estar registrada para XP.")
          else {
            val mysteryEnded = state.endingMode == EndingMode.MYSTERY
            val nextKid = when(state.rulesProfile) {
              RulesProfile.TALES_KIDS -> state.kid.copy(
                xp=(state.kid.xp+gained).coerceAtMost(10),
                conditions=if(mysteryEnded) emptySet() else state.kid.conditions,
                broken=if(mysteryEnded) false else state.kid.broken,
                prideAvailable=true
              )
              RulesProfile.FLOOD_TEENS -> state.kid.copy(
                xp=(state.kid.xp+gained).coerceAtMost(10),
                conditions=if(mysteryEnded) emptySet() else state.kid.conditions,
                broken=if(mysteryEnded) false else state.kid.broken,
                luck=0,
                prideAvailable=false
              )
            }
            ok(state.copy(kid=nextKid,sessionXpAwarded=true))
          }
        }
      }'''
s=one(s,old,new,'profile confirm xp')

old='''      is GameEvent.ApplyBirthdayAttribute -> {
        val attr = state.kid.attributes.keys.firstOrNull { it.equals(event.attribute.trim(), ignoreCase = true) }
        when {
          state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("Mudança de aniversário só ocorre entre Mistérios.")
          !state.change.birthdayReviewed || !state.change.birthdayPassed -> fail("Confirme primeiro que o aniversário ocorreu desde o último Mistério.")
          state.change.birthdayApplied -> fail("Mudança de aniversário já aplicada.")
          attr == null -> fail("Atributo inválido.")
          (state.kid.attributes[attr] ?: 0) >= 5 -> fail("Nenhum Atributo pode ultrapassar 5.")
          state.kid.age >= 16 -> fail("Este Kid já chegou aos 16 anos e precisa seguir em frente.")
          else -> {
            val nextAge = state.kid.age + 1
            val nextAttributes = state.kid.attributes.toMutableMap()
            nextAttributes[attr] = (nextAttributes[attr] ?: 0) + 1
            val nextMaxLuck = (15 - nextAge).coerceAtLeast(0)
            ok(state.copy(
              kid = state.kid.copy(
                age = nextAge,
                luck = state.kid.luck.coerceAtMost(nextMaxLuck),
                attributes = nextAttributes
              ),
              change = state.change.copy(
                birthdayApplied = true,
                improvedAttribute = attr,
                newKidRequired = nextAge >= 16
              )
            ))
          }
        }
      }'''
new='''      is GameEvent.ApplyBirthdayAttribute -> {
        val attr = state.kid.attributes.keys.firstOrNull { it.equals(event.attribute.trim(), ignoreCase = true) }
        when {
          state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("Mudança de aniversário só ocorre entre Mistérios.")
          !state.change.birthdayReviewed || !state.change.birthdayPassed -> fail("Confirme primeiro que o aniversário ocorreu desde o último Mistério.")
          state.change.birthdayApplied -> fail("Mudança de aniversário já aplicada.")
          state.rulesProfile == RulesProfile.FLOOD_TEENS -> {
            if (state.kid.age >= 20) fail("Este Teen já deixou a adolescência.")
            else {
              val nextAge=state.kid.age+1
              ok(state.copy(
                kid=state.kid.copy(age=nextAge,removedFromPlay=state.kid.removedFromPlay || nextAge>=20,luck=0,prideAvailable=false),
                change=state.change.copy(birthdayApplied=true,improvedAttribute=null,newKidRequired=nextAge>=20)
              ))
            }
          }
          attr == null -> fail("Atributo inválido.")
          (state.kid.attributes[attr] ?: 0) >= 5 -> fail("Nenhum Atributo pode ultrapassar 5.")
          state.kid.age >= 16 -> fail("Este Kid já chegou aos 16 anos e precisa seguir em frente.")
          else -> {
            val nextAge = state.kid.age + 1
            val nextAttributes = state.kid.attributes.toMutableMap()
            nextAttributes[attr] = (nextAttributes[attr] ?: 0) + 1
            val nextMaxLuck = (15 - nextAge).coerceAtLeast(0)
            ok(state.copy(
              kid = state.kid.copy(age=nextAge,luck=state.kid.luck.coerceAtMost(nextMaxLuck),attributes=nextAttributes),
              change = state.change.copy(birthdayApplied=true,improvedAttribute=attr,newKidRequired=nextAge>=16)
            ))
          }
        }
      }'''
s=one(s,old,new,'profile birthday')

old='''      is GameEvent.ApplyBetweenMysteryChanges -> {
        when {
          state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("Mudanças de personagem só podem ser feitas entre Mistérios.")
          state.change.characterReviewConfirmed -> fail("A revisão de Mudança já foi confirmada.")
          event.problem != null && event.problem.trim().isBlank() -> fail("Problema não pode ficar vazio.")
          event.drive != null && event.drive.trim().isBlank() -> fail("Ímpeto não pode ficar vazio.")
          event.prideText != null && event.prideText.trim().isBlank() -> fail("Orgulho não pode ficar vazio.")
          event.iconicItemName != null && event.iconicItemName.trim().isBlank() -> fail("Item Icônico não pode ficar vazio.")
          event.relationships != null && event.relationships.any { it.name.isBlank() || it.description.isBlank() } -> fail("Relacionamento precisa de pessoa e descrição.")
          else -> ok(state.copy(kid = state.kid.copy(
            problem = event.problem?.trim()?.take(1000) ?: state.kid.problem,
            drive = event.drive?.trim()?.take(1000) ?: state.kid.drive,
            prideText = event.prideText?.trim()?.take(1000) ?: state.kid.prideText,
            iconicItemName = event.iconicItemName?.trim()?.take(180) ?: state.kid.iconicItemName,
            relationships = event.relationships?.take(20) ?: state.kid.relationships
          )))
        }
      }'''
new='''      is GameEvent.ApplyBetweenMysteryChanges -> {
        when {
          state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("Mudanças de personagem só podem ser feitas entre Mistérios.")
          state.change.characterReviewConfirmed -> fail("A revisão de Mudança já foi confirmada.")
          event.problem != null && event.problem.trim().isBlank() -> fail("Problema não pode ficar vazio.")
          event.iconicItemName != null && event.iconicItemName.trim().isBlank() -> fail("Item Icônico não pode ficar vazio.")
          event.relationships != null && event.relationships.any { it.name.isBlank() || it.description.isBlank() } -> fail("Relacionamento precisa de pessoa e descrição.")
          state.rulesProfile == RulesProfile.TALES_KIDS && event.drive != null && event.drive.trim().isBlank() -> fail("Ímpeto não pode ficar vazio.")
          state.rulesProfile == RulesProfile.TALES_KIDS && event.prideText != null && event.prideText.trim().isBlank() -> fail("Orgulho não pode ficar vazio.")
          state.rulesProfile == RulesProfile.FLOOD_TEENS && event.shameText != null && event.shameText.trim().isBlank() -> fail("Vergonha não pode ficar vazia.")
          else -> {
            val next = when(state.rulesProfile) {
              RulesProfile.TALES_KIDS -> state.kid.copy(
                problem=event.problem?.trim()?.take(1000) ?: state.kid.problem,
                drive=event.drive?.trim()?.take(1000) ?: state.kid.drive,
                prideText=event.prideText?.trim()?.take(1000) ?: state.kid.prideText,
                iconicItemName=event.iconicItemName?.trim()?.take(180) ?: state.kid.iconicItemName,
                relationships=event.relationships?.take(20) ?: state.kid.relationships
              )
              RulesProfile.FLOOD_TEENS -> state.kid.copy(
                problem=event.problem?.trim()?.take(1000) ?: state.kid.problem,
                shameText=event.shameText?.trim()?.take(1000) ?: state.kid.shameText,
                iconicItemName=event.iconicItemName?.trim()?.take(180) ?: state.kid.iconicItemName,
                relationships=event.relationships?.take(20) ?: state.kid.relationships,
                luck=0,prideAvailable=false
              )
            }
            ok(state.copy(kid=next))
          }
        }
      }'''
s=one(s,old,new,'profile Change fields')

s=s.replace('''          state.change.newKidRequired -> fail("Aos 16 anos este Kid segue em frente; crie um novo Kid antes de continuar.")''',
'''          state.change.newKidRequired -> fail(if(state.rulesProfile==RulesProfile.FLOOD_TEENS) "Aos 20 anos este Teen segue em frente; crie um novo Teen antes de continuar." else "Aos 16 anos este Kid segue em frente; crie um novo Kid antes de continuar.")''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# Portable character JSON now carries and validates the active rules profile.
# Tales legacy overloads remain compatible with prior code/tests.
# -----------------------------------------------------------------------------
codec=r'''package com.braseiro.talesvtt.data

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
'''
write('app/src/main/java/com/braseiro/talesvtt/data/KidJsonCodec.kt',codec)

# -----------------------------------------------------------------------------
# ViewModel: profile selection is explicit and blocked during an active campaign.
# Character factory, adventure start, import/export all follow the selected profile.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt'; s=read(rel)
s=one(s,
'''  fun usePride() = dispatch(GameEvent.UsePride)
  fun applyInformationBonus()''',
'''  fun usePride() = dispatch(GameEvent.UsePride)
  fun useShame() = dispatch(GameEvent.UseShame)
  fun useScar(index:Int) = dispatch(GameEvent.UseScar(index))
  fun recordFloodScar(text:String) {
    val count=_state.value.kid.scars.size+1
    val roll=if(count in 2..5) Random.nextInt(1,7) else null
    dispatch(GameEvent.RecordFloodScar(text,roll))
  }
  fun healFloodScar(index:Int) = dispatch(GameEvent.HealAcceptedScar(index,true))
  fun setFloodFriction(text:String) = dispatch(GameEvent.SetFriction(text))
  fun resolveFloodFriction(troubleOccurred:Boolean) = dispatch(GameEvent.ResolveFriction(troubleOccurred))
  fun applyInformationBonus()''','Flood ViewModel actions')

s=one(s,
'''    dispatch(GameEvent.ApplyBetweenMysteryChanges(problem, drive, pride, iconic, relationships))''',
'''    val flood=_state.value.rulesProfile==RulesProfile.FLOOD_TEENS
    dispatch(GameEvent.ApplyBetweenMysteryChanges(
      problem=problem,
      drive=if(flood) null else drive,
      prideText=if(flood) null else pride,
      shameText=if(flood) pride else null,
      iconicItemName=iconic,
      relationships=relationships
    ))''','profile Change dispatch')

anchor='''
  fun createRandomKid() {
'''
insert='''
  private fun groupForProfile(profile:RulesProfile,primary:KidState):GroupState {
    val others=List(3){ if(profile==RulesProfile.FLOOD_TEENS) RandomTeenFactory.create() else RandomKidFactory.create() }
    val members=(listOf(primary)+others).map { GroupMemberState(it.name,it.archetype,it.portraitAssetKey,it.conditions,it.broken) }
    return GroupState(
      members=members,
      relationships=emptyList(),
      hideoutName=if(profile==RulesProfile.FLOOD_TEENS) "Ponto de encontro" else "Cabana no bosque",
      hideoutDescription=if(profile==RulesProfile.FLOOD_TEENS) "Um lugar recorrente onde os Teens se encontram; Friction continua sendo uma regra separada." else "O esconderijo do grupo: um lugar seguro que os Kids mantêm em segredo.",
      notes=emptyList()
    )
  }

  fun selectRulesProfile(profile:RulesProfile) {
    if (_adventureStarted.value) {
      _dataStatus.value="Encerre ou reinicie a campanha antes de trocar Tales/Kids por Flood/Teens."
      return
    }
    if (_state.value.rulesProfile==profile) return
    val character=if(profile==RulesProfile.FLOOD_TEENS) RandomTeenFactory.create() else RandomKidFactory.create()
    _state.value=GameState(rulesProfile=profile,kid=character,group=groupForProfile(profile,character))
    gmPrivateState=GmPrivateState()
    persistAutosave()
    _dataStatus.value=if(profile==RulesProfile.FLOOD_TEENS) "Things from the Flood / Teens selecionado." else "Tales from the Loop / Kids selecionado."
  }

  fun createRandomKid() {
'''
s=one(s,anchor,insert,'profile selector insertion')

old='''    val kid = RandomKidFactory.create()
    check(RandomKidFactory.validateCreation(kid)) { "Factory de Kid gerou ficha fora das regras." }'''
new='''    val profile=_state.value.rulesProfile
    val kid=if(profile==RulesProfile.FLOOD_TEENS) RandomTeenFactory.create() else RandomKidFactory.create()
    check(if(profile==RulesProfile.FLOOD_TEENS) RandomTeenFactory.validateCreation(kid) else RandomKidFactory.validateCreation(kid)) { "Factory de personagem gerou ficha fora das regras do perfil ativo." }'''
s=one(s,old,new,'profile random character')
s=s.replace('''    _dataStatus.value = "Kid aleatório criado dentro dos limites de criação de Tales/Kids: ${kid.name}, ${kid.age} anos, ${kid.archetype}."''',
'''    _dataStatus.value = if(profile==RulesProfile.FLOOD_TEENS) "Teen aleatório criado pelas regras Flood/Teens: ${kid.name}, ${kid.age} anos, ${kid.archetype}." else "Kid aleatório criado dentro dos limites de criação de Tales/Kids: ${kid.name}, ${kid.age} anos, ${kid.archetype}."''',1)

# startRandomAdventure: preserve selected profile and use a 1995 scene seed for Flood.
s=one(s,
'''    val seed = AdventureFactory.create(history)
    val nextHistory = (history + seed.signature).joinToString("\\n")''',
'''    val profile=_state.value.rulesProfile
    if(profile==RulesProfile.FLOOD_TEENS && _state.value.kid.removedFromPlay) {
      _dataStatus.value="Este Teen deixou o jogo. Crie um novo Teen antes de iniciar outro Mistério."
      return
    }
    val seed = AdventureFactory.create(history)
    val activeSeed = if(profile==RulesProfile.FLOOD_TEENS) seed.copy(seasonYear="Outono, 1995") else seed
    val nextHistory = (history + "${profile.wireName}|${seed.signature}").joinToString("\\n")''','profile adventure seed')

old='''    val refreshedKid = _state.value.kid.copy(
      luck = _state.value.kid.maxLuck,
      conditions = emptySet(),
      broken = false,
      prideAvailable = true
    )'''
new='''    val refreshedKid = when(profile) {
      RulesProfile.TALES_KIDS -> _state.value.kid.copy(luck=_state.value.kid.maxLuck,conditions=emptySet(),broken=false,prideAvailable=true)
      RulesProfile.FLOOD_TEENS -> _state.value.kid.copy(luck=0,conditions=emptySet(),broken=false,prideAvailable=false,shameAvailable=true,scarPending=false)
    }'''
s=one(s,old,new,'profile adventure refresh')
s=one(s,
'''    _state.value = GameState(
      kid = refreshedKid,''',
'''    _state.value = GameState(
      rulesProfile = profile,
      kid = refreshedKid,''','preserve profile on adventure')
s=s.replace('''        title = seed.title,
        chapter = "NOVA AVENTURA",
        seasonYear = seed.seasonYear,
        location = seed.location,
        weather = seed.weather,
        time = seed.time,
        daylight = seed.daylight,
        visualAssetKey = seed.visualAsset,
        narrative = seed.localOpeningNarrative(),''',
'''        title = activeSeed.title,
        chapter = if(profile==RulesProfile.FLOOD_TEENS) "NOVO MISTÉRIO • FLOOD" else "NOVA AVENTURA",
        seasonYear = activeSeed.seasonYear,
        location = activeSeed.location,
        weather = activeSeed.weather,
        time = activeSeed.time,
        daylight = activeSeed.daylight,
        visualAssetKey = activeSeed.visualAsset,
        narrative = activeSeed.localOpeningNarrative(),''',1)
s=s.replace('''      revealed = RevealedState(clues = emptySet(), locations = setOf(seed.location), connections = emptySet()),
      routeHistory = listOf(seed.location)''',
'''      revealed = RevealedState(clues = emptySet(), locations = setOf(activeSeed.location), connections = emptySet()),
      routeHistory = listOf(activeSeed.location)''',1)
s=s.replace('''      requestMotor(MotorIntent(type = "adventure_start", text = seed.motorBrief()))''','''      requestMotor(MotorIntent(type = "adventure_start", text = activeSeed.motorBrief()))''',1)

s=s.replace('''        val text = KidJsonCodec.encode(_state.value.kid)''','''        val text = KidJsonCodec.encode(_state.value.rulesProfile,_state.value.kid)''',1)
s=s.replace('''        KidJsonCodec.decode(text)''','''        KidJsonCodec.decodeForProfile(text,_state.value.rulesProfile)''',1)
s=s.replace('''        _dataStatus.value = "Ficha ${kid.name} importada e validada."''','''        _dataStatus.value = "Ficha ${kid.name} importada e validada para ${_state.value.rulesProfile.wireName}."''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# Menu keeps the approved shell but exposes the two real rules profiles.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/screens/MenuScreen.kt'; s=read(rel)
s=one(s,'import com.braseiro.talesvtt.game.MotorTransportConfig\n','import com.braseiro.talesvtt.game.MotorTransportConfig\nimport com.braseiro.talesvtt.game.RulesProfile\n','Menu profile import')
s=one(s,
'''  onCreateRandomKid: () -> Unit,
  onContinueAdventure: () -> Unit,''',
'''  onCreateRandomKid: () -> Unit,
  onSelectRulesProfile: (RulesProfile) -> Unit,
  onContinueAdventure: () -> Unit,''','Menu selector callback')
s=one(s,
'''  var ttsMode by remember(motorConfig.ttsMode) { mutableStateOf(motorConfig.ttsMode) }
''',
'''  var ttsMode by remember(motorConfig.ttsMode) { mutableStateOf(motorConfig.ttsMode) }
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
  val profileLabel = if (flood) "Things from the Flood / Teens" else "Tales from the Loop / Kids"
  val characterLabel = if (flood) "TEEN" else "KID"
''','Menu profile locals')
s=s.replace('ContextItemData("menu_profile", "Perfil", "Tales / Kids", ContextIconType.CHARACTERS, true)','ContextItemData("menu_profile", "Perfil", if(flood) "Flood / Teens" else "Tales / Kids", ContextIconType.CHARACTERS, true)',1)

s=one(s,
'''    SectionHeader(title = "COMEÇAR", color = AppTheme.colors.accentCyan)''',
'''    SectionHeader(title = "SISTEMA", color = AppTheme.colors.accentPurple)
    Spacer(Modifier.height(Dimens.spacing8))
    AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = .45f)) {
      Text(profileLabel.uppercase(), style = AppTheme.typography.cardTitle)
      Text("Tales/Kids e Flood/Teens usam contratos mecânicos separados. A troca fica bloqueada durante uma campanha ativa.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      Spacer(Modifier.height(8.dp))
      Row {
        if (!flood) PrimaryButton("TALES • KIDS", onClick = { onSelectRulesProfile(RulesProfile.TALES_KIDS) }, modifier = Modifier.weight(1f))
        else SecondaryButton("TALES • KIDS", onClick = { onSelectRulesProfile(RulesProfile.TALES_KIDS) }, enabled = !adventureStarted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(7.dp))
        if (flood) PrimaryButton("FLOOD • TEENS", onClick = { onSelectRulesProfile(RulesProfile.FLOOD_TEENS) }, modifier = Modifier.weight(1f))
        else SecondaryButton("FLOOD • TEENS", onClick = { onSelectRulesProfile(RulesProfile.FLOOD_TEENS) }, enabled = !adventureStarted, modifier = Modifier.weight(1f))
      }
    }

    Spacer(Modifier.height(Dimens.spacing16))
    SectionHeader(title = "COMEÇAR", color = AppTheme.colors.accentCyan)''','Menu system panel')
s=s.replace('''        "Configure o Motor/voz, crie ou importe seu Kid e só então inicie. Nenhuma aventura é iniciada automaticamente.",''','''        "Configure o Motor/voz, crie ou importe seu ${if(flood) "Teen" else "Kid"} e só então inicie. Nenhuma aventura é iniciada automaticamente.",''',1)
s=s.replace('''      SecondaryButton("CRIAR KID ALEATÓRIO PELAS REGRAS", onClick = onCreateRandomKid, modifier = Modifier.fillMaxWidth())''','''      SecondaryButton("CRIAR $characterLabel ALEATÓRIO PELAS REGRAS", onClick = onCreateRandomKid, modifier = Modifier.fillMaxWidth())''',1)
s=s.replace('''        Text("Tales from the Loop / Kids • ${gameState.group.members.size} Kids • ${gameState.scene.location}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))''','''        Text("$profileLabel • ${gameState.group.members.size} ${if(flood) "Teens" else "Kids"} • ${gameState.scene.location}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))''',1)
s=s.replace('''        Text("Seu Kid atual: ${gameState.kid.name} • ${gameState.kid.age} anos • ${gameState.kid.archetype}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))''','''        Text("Seu ${if(flood) "Teen" else "Kid"} atual: ${gameState.kid.name} • ${gameState.kid.age} anos • ${gameState.kid.archetype}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))''',1)
s=s.replace('''    SectionHeader(title = "FICHA DO KID • JSON", color = AppTheme.colors.accentOlive)''','''    SectionHeader(title = "FICHA DO $characterLabel • JSON", color = AppTheme.colors.accentOlive)''',1)
s=s.replace('''      Text("A importação valida perfil Tales/Kids, idade, Atributos, Perícias, Sorte e campos obrigatórios antes de alterar a campanha.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))''','''      Text(if(flood) "A importação valida Flood/Teens: 14–19 anos, Atributos total 14, Perícias, Vergonha, Cicatrizes e ausência de Sorte/Orgulho." else "A importação valida Tales/Kids, idade, Atributos, Perícias, Sorte e campos obrigatórios antes de alterar a campanha.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))''',1)
write(rel,s)

# Navigation wires the selector without changing the shell/routes.
rel='app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt'; s=read(rel)
s=one(s,
'''        onCreateRandomKid = gameViewModel::createRandomKid,
        onContinueAdventure = { onNavigate(AppDestination.SCENE) }''',
'''        onCreateRandomKid = gameViewModel::createRandomKid,
        onSelectRulesProfile = gameViewModel::selectRulesProfile,
        onContinueAdventure = { onNavigate(AppDestination.SCENE) }''','Nav selector callback')
write(rel,s)

# Regression tests for the new profile lifecycle. UI source assertions intentionally avoid screenshot redesign.
test=r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FloodProfileLifecycleV1316Test {
  private fun ending(teen:KidState,age:Int=teen.age)=GameState(
    rulesProfile=RulesProfile.FLOOD_TEENS,
    kid=teen.copy(age=age),
    endingMode=EndingMode.MYSTERY,
    sessionXpAwarded=true
  )

  @Test fun floodChangeEditsShameButDoesNotSilentlyEditDrive(){
    val teen=RandomTeenFactory.create(Random(31))
    val beforeDrive=teen.drive
    var s=ending(teen)
    s=TalesReducer.reduce(s,GameEvent.ApplyBetweenMysteryChanges(problem="Novo problema",drive="DRIVE INDEVIDO",shameText="Nova vergonha",iconicItemName="Novo item",relationships=teen.relationships))
    assertNull(s.lastError)
    assertEquals(beforeDrive,s.kid.drive)
    assertEquals("Nova vergonha",s.kid.shameText)
    assertEquals("Novo problema",s.kid.problem)
  }

  @Test fun floodBirthdayAddsAgeOnlyAndTwentyLeavesPlay(){
    val teen=RandomTeenFactory.create(Random(32)).copy(age=19)
    var s=ending(teen)
    s=TalesReducer.reduce(s,GameEvent.SetBirthdayPassed(true)); assertNull(s.lastError)
    val attrs=s.kid.attributes
    s=TalesReducer.reduce(s,GameEvent.ApplyBirthdayAttribute("")); assertNull(s.lastError)
    assertEquals(20,s.kid.age)
    assertEquals(attrs,s.kid.attributes)
    assertTrue(s.change.newKidRequired)
    assertTrue(s.kid.removedFromPlay)
  }

  @Test fun pendingFloodScarBlocksXpClosure(){
    val teen=RandomTeenFactory.create(Random(33)).copy(broken=true,scarPending=true)
    val answers=listOf(true,true,false,false,false)
    val s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,endingMode=EndingMode.MYSTERY,xpAnswers=answers)
    val blocked=TalesReducer.reduce(s,GameEvent.ConfirmMysteryEnd)
    assertNotNull(blocked.lastError)
    assertFalse(blocked.sessionXpAwarded)
  }

  @Test fun menuAndCodecExposeExplicitProfileFirewall(){
    val candidates=listOf(File("src/main/java/com/braseiro/talesvtt/screens/MenuScreen.kt"),File("app/src/main/java/com/braseiro/talesvtt/screens/MenuScreen.kt"))
    val menu=candidates.first{it.isFile}.readText()
    assertTrue(menu.contains("FLOOD • TEENS"))
    assertTrue(menu.contains("onSelectRulesProfile"))
    val codecCandidates=listOf(File("src/main/java/com/braseiro/talesvtt/data/KidJsonCodec.kt"),File("app/src/main/java/com/braseiro/talesvtt/data/KidJsonCodec.kt"))
    val codec=codecCandidates.first{it.isFile}.readText()
    assertTrue(codec.contains("decodeForProfile"))
    assertTrue(codec.contains("braseiro-flood-teen"))
    assertTrue(codec.contains("RandomTeenFactory.validatePersistentState"))
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/FloodProfileLifecycleV1316Test.kt',test)

resume=root/'TALES_RETOMADA_CURRENT.md'
text=resume.read_text(encoding='utf-8') if resume.exists() else '# TALES FROM THE LOOP — RETOMADA ATUAL\n'
text+='''\n## Bloco 1.3.16d-dev — seleção Tales/Flood + ciclo de personagem\n- Menu ganhou seletor real TALES/KIDS ↔ FLOOD/TEENS, bloqueado durante campanha ativa.\n- Factory, início de aventura, import/export JSON e Motor preservam o perfil selecionado.\n- Flood Change agora permite Problem/Shame/Iconic Item/Relationships sem importar Drive/Pride de Tales.\n- Flood aniversário aumenta apenas idade; aos 20 o Teen deixa o jogo.\n- Cicatriz pendente bloqueia fechamento de XP antes de ser registrada.\n- Nenhum APK publicado.\n'''
resume.write_text(text,encoding='utf-8')

assert 'onSelectRulesProfile' in read('app/src/main/java/com/braseiro/talesvtt/screens/MenuScreen.kt')
assert 'decodeForProfile' in read('app/src/main/java/com/braseiro/talesvtt/data/KidJsonCodec.kt')
assert 'shameText: String? = null' in read('app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt')
print('TALES/FLOOD 1.3.16d profile selection + character lifecycle: OK')
