from pathlib import Path
import re, sys

repo=Path(__file__).resolve().parent
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.15k-transform.py')}
code=(repo/'tales-1.3.15k-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],sys.argv[1]]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

root=Path(sys.argv[1])
def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def one(text,old,new,label):
    if old not in text: raise SystemExit('missing anchor: '+label)
    return text.replace(old,new,1)

# Source-only development version. No APK/release.
rel='app/build.gradle.kts'; s=read(rel)
s=re.sub(r'versionCode\s*=\s*\d+','versionCode = 20',s,count=1)
s=re.sub(r'versionName\s*=\s*"[^"]+"','versionName = "1.3.16-dev"',s,count=1)
write(rel,s)

profiles=r'''package com.braseiro.talesvtt.game

enum class RulesProfile(val wireName:String) {
  TALES_KIDS("tales_kid"),
  FLOOD_TEENS("flood_teen");

  companion object {
    fun fromWire(value:String?):RulesProfile = when(value?.trim()?.lowercase()) {
      "flood_teen", "flood_teens", "things_from_the_flood", "flood" -> FLOOD_TEENS
      else -> TALES_KIDS
    }
  }
}

data class FloodScarState(
  val text:String,
  val accepted:Boolean=false,
  val usedForSuccess:Boolean=false
)

object RulesProfiles {
  fun motorProfile(profile:RulesProfile)=when(profile){
    RulesProfile.TALES_KIDS -> MotorProfile.TALES_KIDS
    RulesProfile.FLOOD_TEENS -> MotorProfile.FLOOD_TEENS
  }
  fun threatTarget(profile:RulesProfile,kids:Int,level:ThreatLevel):Int = kids * when(profile){
    RulesProfile.TALES_KIDS -> when(level){
      ThreatLevel.NORMAL->2; ThreatLevel.DIFFICULT->3; ThreatLevel.ALMOST_IMPOSSIBLE->4
    }
    RulesProfile.FLOOD_TEENS -> when(level){
      ThreatLevel.NORMAL->3; ThreatLevel.DIFFICULT->4; ThreatLevel.ALMOST_IMPOSSIBLE->5
    }
  }
  fun mechanicalContract(profile:RulesProfile)=when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.localMechanicalContract()
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.localMechanicalContract()
  }
}
'''
write('app/src/main/java/com/braseiro/talesvtt/game/RulesProfile.kt',profiles)

flood=r'''package com.braseiro.talesvtt.game

data class FloodTypeRule(val id:String,val label:String,val keySkills:Set<String>)

object CanonicalFloodRules {
  const val PROFILE="flood_teen"
  const val MIN_AGE=14
  const val MAX_AGE=19
  const val ATTRIBUTE_TOTAL=14
  const val ATTRIBUTE_MIN=1
  const val ATTRIBUTE_MAX=5
  const val CREATION_SKILL_POINTS=10
  const val CREATION_KEY_SKILL_MAX=3
  const val CREATION_OTHER_SKILL_MAX=1
  const val SKILL_MAX=5
  const val XP_SKILL_COST=5
  const val XP_STORAGE_MAX=10
  const val HELP_BONUS=1
  const val ICONIC_ITEM_BONUS=2
  const val ITEM_MIN_BONUS=1
  const val ITEM_MAX_BONUS=3
  const val MIN_NON_BROKEN_POOL=1

  val types=listOf(
    FloodTypeRule("FLOOD-TYPE-001","Hacker",setOf("Programação","Cálculo","Conhecimento")),
    FloodTypeRule("FLOOD-TYPE-002","Jocão",setOf("Força","Movimentação","Liderança")),
    FloodTypeRule("FLOOD-TYPE-003","Lobo Solitário",setOf("Furtividade","Conhecimento","Empatia")),
    FloodTypeRule("FLOOD-TYPE-004","Motorhead",setOf("Mecânica","Cálculo","Charme")),
    FloodTypeRule("FLOOD-TYPE-005","Festeiro",setOf("Movimentação","Contatos","Liderança")),
    FloodTypeRule("FLOOD-TYPE-006","Raver",setOf("Mecânica","Contatos","Charme")),
    FloodTypeRule("FLOOD-TYPE-007","Rocker",setOf("Movimentação","Empatia","Investigação")),
    FloodTypeRule("FLOOD-TYPE-008","Buscador",setOf("Investigação","Empatia","Liderança")),
    FloodTypeRule("FLOOD-TYPE-009","Esnobe",setOf("Charme","Contatos","Conhecimento")),
    FloodTypeRule("FLOOD-TYPE-010","Garoto de Rua",setOf("Força","Furtividade","Investigação"))
  )

  fun type(label:String)=types.firstOrNull{it.label.equals(label.trim(),true)}
  fun validItem(item:KidItemState)=when(item.kind){
    KidItemKind.ORDINARY->item.name.isNotBlank()&&item.bonus==0
    KidItemKind.MYSTERY->item.name.isNotBlank()&&item.bonus in ITEM_MIN_BONUS..ITEM_MAX_BONUS
  }
  fun localMechanicalContract()="""
PROFILE=FLOOD/TEENS. LOCAL ENGINE IS THE ONLY MECHANICAL AUTHORITY; RAG/LLM IS NARRATIVE REFERENCE ONLY.
Teen creation: age 14..19; Attributes Body/Tech/Heart/Mind each 1..5 totaling exactly 14; 10 Skill points; Type key Skills max3, others max1 at creation.
Test = Attribute + Skill + approved Item + information bonus + at most one Help - one die per light Condition; a non-Broken Teen always rolls at least 1 die; every 6 is one success. Broken auto-fails.
There is NO Luck and NO Pride. Shame grants +1 automatic success once per Mystery; when a Mystery spans sessions or in Mystery Landscape it refreshes once per session.
Each time a Teen becomes Broken, the Teen gains a Scar. An unused Scar may grant +1 automatic success once, then it is accepted. After every Scar after the first, removal risk is resolved locally; the sixth Scar inevitably removes the Teen from play.
Iconic Item +2. Other real Items +1..+3 when applicable; ordinary props +0. Non-Iconic Items are normally lost between Mysteries unless one becomes the new Iconic Item.
Conditions: Chateado, Assustado, Exausto, Machucado. Each gives -1 die. Broken auto-fails until valid recovery.
Anchor recovery clears Conditions in a safe close scene. Friction may be resolved only in a safe close group scene; if Trouble occurs, no Conditions are healed.
Extended Trouble: Normal=3x Teens, Extremely Difficult=4x, Almost Impossible=5x, one scene/roll per Teen. Rolled successes count toward Threat and may also buy Skill effects. A Lead preparation scene creates its leadership pool instead of contributing those successes to Threat.
XP: five Flood questions, each Yes=1 XP; 5 XP raises one Skill, max5; max10 stored. Spend XP only outside active play.
Change: Problem, Shame, Iconic Item and Relationships may change. Conditions clear between Mysteries. If enough time passed age may rise by one; at 20 the Teen leaves play.
Never import Tales Luck, Pride, Kid age/attribute-total rules, or Tales 2x/3x/4x Extended Trouble targets.
""".trimIndent()
}
'''
write('app/src/main/java/com/braseiro/talesvtt/game/CanonicalFloodRules.kt',flood)

teen_factory=r'''package com.braseiro.talesvtt.game

import kotlin.random.Random

object RandomTeenFactory {
  private val firstNames=listOf("Mette","Helmi","Sanna","Melissa","Tiffany","Erin","Dennis","Lenny","Ola","Jeremy","Kyle","Seth")
  private val lastNames=listOf("Andersson","Berg","Ek","Holm","Järne","Lind","Nilsson","Svensson","Tannenbaum","Östberg")
  private val anchors=listOf("Eva — professora","Gunnar — mecânico","Lena — irmã mais velha","Mats — vizinho","Louise — tia","Sven — treinador")
  private val problems=listOf("Minha família está se desfazendo.","Eu preciso provar que não sou mais criança.","Estou escondendo algo dos meus amigos.","A escola está desmoronando ao meu redor.","Eu confio na pessoa errada.")
  private val drives=listOf("Descobrir o que aconteceu.","Proteger meus amigos.","Sair desta cidade.","Impedir que isso aconteça de novo.","Encontrar alguém que desapareceu.")
  private val shames=listOf("Eu abandonei alguém quando precisava de mim.","Eu menti sobre algo importante.","Tenho medo de ser igual aos meus pais.","Eu causei algo que não consigo desfazer.","Ninguém sabe do que realmente tenho medo.")
  private val iconic=listOf("Sistema de hacking caseiro","Jaqueta de couro","Câmera VHS","Kit de ferramentas","Walkman modificado","Skate gasto","Telefone portátil velho")
  private val portraits=listOf("kid_portrait_01","kid_portrait_02","kid_portrait_03","kid_portrait_04")

  fun create(random:Random=Random.Default):KidState {
    val type=CanonicalFloodRules.types.random(random)
    val attrs=linkedMapOf("CORPO" to 1,"TECNOLOGIA" to 1,"CORAÇÃO" to 1,"MENTE" to 1)
    var remain=CanonicalFloodRules.ATTRIBUTE_TOTAL-4
    while(remain>0){
      val choices=attrs.keys.filter{(attrs[it]?:1)<CanonicalFloodRules.ATTRIBUTE_MAX}
      val chosen=choices.random(random); attrs[chosen]=(attrs[chosen]?:1)+1; remain--
    }
    val skills=linkedMapOf<String,Int>().apply{TalesSkillCatalog.skillToAttribute.keys.forEach{put(it,0)}}
    var points=CanonicalFloodRules.CREATION_SKILL_POINTS
    while(points>0){
      val choices=skills.keys.filter{skill->(skills[skill]?:0)<if(skill in type.keySkills)3 else 1}
      val chosen=choices.random(random); skills[chosen]=(skills[chosen]?:0)+1; points--
    }
    val age=random.nextInt(CanonicalFloodRules.MIN_AGE,CanonicalFloodRules.MAX_AGE+1)
    return KidState(
      name="${firstNames.random(random)} ${lastNames.random(random)}",
      archetype=type.label,
      portraitAssetKey=portraits.random(random),
      description="Teen de $age anos, ${type.label.lowercase()}, criado pelas regras de Things from the Flood.",
      problem=problems.random(random),
      drive=drives.random(random),
      prideText="",
      shameText=shames.random(random),
      shameAvailable=true,
      birthday="${random.nextInt(1,29).toString().padStart(2,'0')}/${random.nextInt(1,13).toString().padStart(2,'0')}",
      anchorName=anchors.random(random),
      iconicItemName=iconic.random(random),
      hooks=listOf("Um gancho será definido pelo grupo."),
      friction="A tensão do grupo ainda será definida em jogo.",
      relationships=listOf(KidRelationship("Outro Teen do grupo","relação que será definida em jogo"),KidRelationship("NPC importante","relação da vida cotidiana")),
      age=age,
      luck=0,
      conditions=emptySet(),
      broken=false,
      prideAvailable=false,
      xp=0,
      attributes=attrs,
      skills=skills
    )
  }

  fun fromKid(kid:KidState,random:Random=Random.Default):KidState {
    val fresh=create(random)
    return fresh.copy(
      name=kid.name,
      portraitAssetKey=kid.portraitAssetKey,
      portraitImageBase64=kid.portraitImageBase64,
      description=kid.description,
      favoriteSong=kid.favoriteSong,
      relationships=kid.relationships.ifEmpty{fresh.relationships}
    )
  }

  fun validateCreation(teen:KidState):Boolean {
    val type=CanonicalFloodRules.type(teen.archetype)?:return false
    if(teen.age !in CanonicalFloodRules.MIN_AGE..CanonicalFloodRules.MAX_AGE)return false
    if(teen.attributes.keys!=setOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE"))return false
    if(teen.attributes.values.any{it !in 1..5}||teen.attributes.values.sum()!=14)return false
    if(teen.skills.keys!=TalesSkillCatalog.skillToAttribute.keys)return false
    if(teen.skills.values.sum()!=10)return false
    if(teen.skills.any{(skill,value)->value !in 0..if(skill in type.keySkills)3 else 1})return false
    if(teen.luck!=0||teen.prideAvailable)return false
    if(teen.shameText.isBlank())return false
    if(teen.items.any{!CanonicalFloodRules.validItem(it)})return false
    return true
  }

  fun validatePersistentState(teen:KidState):Boolean {
    if(teen.name.isBlank()||teen.age !in 14..19)return false
    if(teen.attributes.keys!=setOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE"))return false
    if(teen.attributes.values.any{it !in 1..5}||teen.attributes.values.sum()!=14)return false
    if(teen.skills.keys!=TalesSkillCatalog.skillToAttribute.keys||teen.skills.values.any{it !in 0..5})return false
    if(teen.luck!=0||teen.xp !in 0..10)return false
    if(teen.items.any{!CanonicalFloodRules.validItem(it)})return false
    return true
  }
}
'''
write('app/src/main/java/com/braseiro/talesvtt/game/RandomTeenFactory.kt',teen_factory)

# Integrate profile and Flood-only fields into the shared state without redesigning screens.
rel='app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt'; s=read(rel)
s=one(s,
'''  val favoriteSong: String = "",
  val items: List<KidItemState> = emptyList(),''',
'''  val favoriteSong: String = "",
  val birthday: String = "",
  val shameText: String = "",
  val shameAvailable: Boolean = false,
  val scars: List<FloodScarState> = emptyList(),
  val hooks: List<String> = emptyList(),
  val friction: String = "",
  val removedFromPlay: Boolean = false,
  val items: List<KidItemState> = emptyList(),''','teen fields')

s=one(s,
'''data class ExtendedTroubleState(
  val active: Boolean = false,''',
'''data class ExtendedTroubleState(
  val rulesProfile: RulesProfile = RulesProfile.TALES_KIDS,
  val active: Boolean = false,''','extended profile')
s=s.replace('''  val target: Int get() = kids * threatLevel.multiplier''','''  val target: Int get() = RulesProfiles.threatTarget(rulesProfile, kids, threatLevel)''',1)

s=one(s,
'''data class GameState(
  val kid: KidState = KidState(),''',
'''data class GameState(
  val rulesProfile: RulesProfile = RulesProfile.TALES_KIDS,
  val kid: KidState = KidState(),''','game profile')
s=one(s,
'''    return (test.basePool() + test.approvedItemBonus + test.informationBonus + leadership + (if (test.helperKid != null) CanonicalTalesRules.HELP_BONUS else 0) - kid.conditionPenalty).coerceAtLeast(0)''',
'''    val raw = test.basePool() + test.approvedItemBonus + test.informationBonus + leadership + (if (test.helperKid != null) CanonicalTalesRules.HELP_BONUS else 0) - kid.conditionPenalty
    return when (rulesProfile) {
      RulesProfile.TALES_KIDS -> raw.coerceAtLeast(0)
      RulesProfile.FLOOD_TEENS -> raw.coerceAtLeast(CanonicalFloodRules.MIN_NON_BROKEN_POOL)
    }''','profile pool')

s=one(s,
'''  data object UsePride : GameEvent
  data class ApplyCondition''',
'''  data object UsePride : GameEvent
  data object UseShame : GameEvent
  data class ApplyCondition''','shame event')

# Flood cannot spend Tales Luck.
s=one(s,
'''      is GameEvent.SpendLuck -> {
        val result = state.testResolution ?: return fail("Role antes de usar Sorte.")''',
'''      is GameEvent.SpendLuck -> {
        if (state.rulesProfile != RulesProfile.TALES_KIDS) return fail("Things from the Flood não usa Pontos de Sorte.")
        val result = state.testResolution ?: return fail("Role antes de usar Sorte.")''','luck firewall')

old_pride='''      GameEvent.UsePride -> {
        val result = state.testResolution ?: return fail("Role antes de usar Orgulho.")
        if (!state.kid.prideAvailable) fail("Orgulho indisponível.")
        else if (result.prideUsed) fail("Orgulho já usado neste teste.")
        else ok(state.copy(
          kid = state.kid.copy(prideAvailable = false),
          testResolution = result.copy(successes = result.successes + 1, prideUsed = true)
        ))
      }

      is GameEvent.ApplyCondition -> {'''
new_pride='''      GameEvent.UsePride -> {
        if (state.rulesProfile != RulesProfile.TALES_KIDS) fail("Things from the Flood usa Vergonha/Cicatrizes, não Orgulho.")
        else {
          val result = state.testResolution ?: return fail("Role antes de usar Orgulho.")
          if (!state.kid.prideAvailable) fail("Orgulho indisponível.")
          else if (result.prideUsed) fail("Orgulho já usado neste teste.")
          else ok(state.copy(
            kid = state.kid.copy(prideAvailable = false),
            testResolution = result.copy(successes = result.successes + 1, prideUsed = true)
          ))
        }
      }

      GameEvent.UseShame -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Vergonha é exclusiva de Things from the Flood.")
        else {
          val result=state.testResolution ?: return fail("Role antes de ativar a Vergonha.")
          if(!state.kid.shameAvailable) fail("Vergonha indisponível nesta sessão/Mistério.")
          else ok(state.copy(kid=state.kid.copy(shameAvailable=false),testResolution=result.copy(successes=result.successes+1)))
        }
      }

      is GameEvent.ApplyCondition -> {'''
s=one(s,old_pride,new_pride,'pride/shame reducer')

# Extended Trouble carries the active profile so Threat targets remain isolated.
s=one(s,
'''        else ok(state.copy(extended = ExtendedTroubleState(
          active = true,''',
'''        else ok(state.copy(extended = ExtendedTroubleState(
          rulesProfile = state.rulesProfile,
          active = true,''','extended start profile')

# Reset session resources by active rules profile.
s=one(s,
'''            kid = state.kid.copy(luck = state.kid.maxLuck, prideAvailable = true),''',
'''            kid = when(state.rulesProfile) {
              RulesProfile.TALES_KIDS -> state.kid.copy(luck = state.kid.maxLuck, prideAvailable = true)
              RulesProfile.FLOOD_TEENS -> state.kid.copy(luck = 0, prideAvailable = false, shameAvailable = true)
            },''','next session resources')
write(rel,s)

# Snapshot v14: rules profile + Flood character fields + Extended Trouble profile.
rel='app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt'; s=read(rel)
s=s.replace('private const val VERSION = 13','private const val VERSION = 14',1)
s=one(s,'line("version", VERSION)','line("version", VERSION)\n    line("rules.profile", state.rulesProfile.name)','snapshot profile encode')
s=one(s,
'''    line("kid.favoriteSong", enc(state.kid.favoriteSong))
    line("kid.items",''',
'''    line("kid.favoriteSong", enc(state.kid.favoriteSong))
    line("kid.birthday", enc(state.kid.birthday))
    line("kid.shameText", enc(state.kid.shameText))
    line("kid.shameAvailable", state.kid.shameAvailable)
    line("kid.scars", state.kid.scars.joinToString("|") { listOf(enc(it.text),it.accepted.toString(),it.usedForSuccess.toString()).joinToString("~") })
    line("kid.hooks", state.kid.hooks.joinToString("|") { enc(it) })
    line("kid.friction", enc(state.kid.friction))
    line("kid.removedFromPlay", state.kid.removedFromPlay)
    line("kid.items",''','snapshot teen encode')
s=one(s,
'''    line("ext.active", ext.active)''',
'''    line("ext.profile", ext.rulesProfile.name)
    line("ext.active", ext.active)''','snapshot ext profile encode')

# Decode teen fields into KidState.
s=one(s,
'''      favoriteSong = if (version >= 10) decoded("kid.favoriteSong") else defaults.favoriteSong,
      items = kidItems,''',
'''      favoriteSong = if (version >= 10) decoded("kid.favoriteSong") else defaults.favoriteSong,
      birthday = if (version >= 14) decoded("kid.birthday") else "",
      shameText = if (version >= 14) decoded("kid.shameText") else "",
      shameAvailable = if (version >= 14) bool("kid.shameAvailable") else false,
      scars = if (version >= 14) map["kid.scars"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
        val p=raw.split('~'); if(p.size>=3) FloodScarState(dec(p[0]),p[1].toBooleanStrictOrNull()?:false,p[2].toBooleanStrictOrNull()?:false) else null
      } else emptyList(),
      hooks = if (version >= 14) map["kid.hooks"].orEmpty().split('|').filter{it.isNotBlank()}.map(::dec) else emptyList(),
      friction = if (version >= 14) decoded("kid.friction") else "",
      removedFromPlay = if (version >= 14) bool("kid.removedFromPlay") else false,
      items = kidItems,''','snapshot teen decode')

# Decode Extended Trouble profile if the current constructor anchor is present.
s=s.replace('''    val ext = ExtendedTroubleState(
      active = bool("ext.active"),''','''    val ext = ExtendedTroubleState(
      rulesProfile = if(version>=14) runCatching{RulesProfile.valueOf(map["ext.profile"].orEmpty())}.getOrDefault(RulesProfile.TALES_KIDS) else RulesProfile.TALES_KIDS,
      active = bool("ext.active"),''',1)

s=one(s,
'''    GameState(
      kid = kid,''',
'''    GameState(
      rulesProfile = if(version>=14) runCatching{RulesProfile.valueOf(map["rules.profile"].orEmpty())}.getOrDefault(RulesProfile.TALES_KIDS) else RulesProfile.TALES_KIDS,
      kid = kid,''','snapshot game profile decode')
write(rel,s)

# Motor packets become profile-aware. A packet can never cross the active rules profile.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt'; s=read(rel)
s=s.replace('enum class MotorProfile { TALES_KIDS }','enum class MotorProfile { TALES_KIDS, FLOOD_TEENS }',1)
old='''    if (packet.profile != MotorProfile.TALES_KIDS) {
      return MotorApplyResult(
        gameState = gameState,
        privateState = privateState,
        routeHint = MotorRouteHint.NONE,
        acceptedActions = 0,
        rejected = listOf("Perfil mecânico incompatível: somente TALES_KIDS é aceito neste projeto.")
      )
    }'''
new='''    val expectedProfile=RulesProfiles.motorProfile(gameState.rulesProfile)
    if (packet.profile != expectedProfile) {
      return MotorApplyResult(
        gameState = gameState,
        privateState = privateState,
        routeHint = MotorRouteHint.NONE,
        acceptedActions = 0,
        rejected = listOf("Perfil mecânico incompatível com a campanha ativa: esperado $expectedProfile.")
      )
    }'''
s=one(s,old,new,'motor profile firewall')
write(rel,s)

# Wire parser accepts an explicit profile; legacy responses remain Tales for backward compatibility.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt'; s=read(rel)
s=one(s,
'''    return MotorParseResult(MotorPacket(actions = parsed), rejected)''',
'''    val profile=when(root.optString("profile").trim().lowercase()) {
      "flood_teen","flood_teens","things_from_the_flood","flood" -> MotorProfile.FLOOD_TEENS
      else -> MotorProfile.TALES_KIDS
    }
    return MotorParseResult(MotorPacket(profile=profile, actions = parsed), rejected)''','wire profile')
write(rel,s)

# Gemini receives only the mechanical contract for the selected profile.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt'; s=read(rel)
s=s.replace('.put("profile", "tales_kid")','.put("profile", state.rulesProfile.wireName)',1)
s=s.replace('.put("active_profile", "tales_kid")','.put("active_profile", state.rulesProfile.wireName)',1)
s=s.replace('.put("mechanical_contract", CanonicalTalesRules.localMechanicalContract())','.put("mechanical_contract", RulesProfiles.mechanicalContract(state.rulesProfile))',1)
# Include Flood resources without exposing any GM-private material.
s=one(s,
'''        .put("prideAvailable", state.kid.prideAvailable)
        .put("iconicItem", state.kid.iconicItemName)''',
'''        .put("prideAvailable", state.kid.prideAvailable)
        .put("shameAvailable", state.kid.shameAvailable)
        .put("shame", state.kid.shameText)
        .put("scars", JSONArray(state.kid.scars.map { JSONObject().put("text",it.text).put("accepted",it.accepted).put("used",it.usedForSuccess) }))
        .put("friction", state.kid.friction)
        .put("iconicItem", state.kid.iconicItemName)''','transport flood visible state')
write(rel,s)

# Unit tests for strict profile isolation and canonical Teen creation.
test=r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FloodProfileFoundationV1316Test {
  private fun req(kid:KidState,skill:String="Investigação")=kid.testForSkill("x",skill,1)!!

  @Test fun teenFactoryIsCanonicalAcrossSeeds(){
    repeat(256){ seed ->
      val teen=RandomTeenFactory.create(Random(seed))
      assertTrue("seed $seed",RandomTeenFactory.validateCreation(teen))
      assertTrue(teen.age in 14..19)
      assertEquals(14,teen.attributes.values.sum())
      assertEquals(10,teen.skills.values.sum())
      assertEquals(0,teen.luck)
      assertFalse(teen.prideAvailable)
      assertTrue(teen.shameText.isNotBlank())
    }
  }

  @Test fun kidToTeenKeepsIdentityButRedistributesMechanics(){
    val kid=RandomKidFactory.create(Random(7))
    val teen=RandomTeenFactory.fromKid(kid,Random(9))
    assertEquals(kid.name,teen.name)
    assertEquals(kid.portraitAssetKey,teen.portraitAssetKey)
    assertEquals(14,teen.attributes.values.sum())
    assertEquals(10,teen.skills.values.sum())
    assertEquals(0,teen.luck)
    assertTrue(RandomTeenFactory.validateCreation(teen))
  }

  @Test fun floodPoolHasOneDieFloorButTalesDoesNot(){
    val base=KidState(conditions=setOf(Condition.UPSET,Condition.SCARED,Condition.EXHAUSTED,Condition.INJURED),attributes=linkedMapOf("CORPO" to 1,"TECNOLOGIA" to 1,"CORAÇÃO" to 1,"MENTE" to 1),skills=linkedMapOf("Furtividade" to 0,"Força" to 0,"Movimentação" to 0,"Mecânica" to 0,"Programação" to 0,"Cálculo" to 0,"Contatos" to 0,"Charme" to 0,"Liderança" to 0,"Investigação" to 0,"Conhecimento" to 0,"Empatia" to 0))
    val test=req(base)
    assertEquals(0,GameState(rulesProfile=RulesProfile.TALES_KIDS,kid=base,activeTest=test).finalPool())
    assertEquals(1,GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=base.copy(luck=0,prideAvailable=false),activeTest=test).finalPool())
  }

  @Test fun floodThreatTargetsAreThreeFourFive(){
    assertEquals(3,RulesProfiles.threatTarget(RulesProfile.FLOOD_TEENS,1,ThreatLevel.NORMAL))
    assertEquals(4,RulesProfiles.threatTarget(RulesProfile.FLOOD_TEENS,1,ThreatLevel.DIFFICULT))
    assertEquals(5,RulesProfiles.threatTarget(RulesProfile.FLOOD_TEENS,1,ThreatLevel.ALMOST_IMPOSSIBLE))
    assertEquals(2,RulesProfiles.threatTarget(RulesProfile.TALES_KIDS,1,ThreatLevel.NORMAL))
  }

  @Test fun floodRejectsLuckAndPrideButAcceptsShame(){
    val teen=RandomTeenFactory.create(Random(3))
    val test=req(teen)
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,activeTest=test)
    val pool=s.finalPool(); s=TalesReducer.reduce(s,GameEvent.RollResult(List(pool){1})); assertNull(s.lastError)
    assertNotNull(TalesReducer.reduce(s,GameEvent.SpendLuck(List(pool){6})).lastError)
    assertNotNull(TalesReducer.reduce(s,GameEvent.UsePride).lastError)
    s=TalesReducer.reduce(s,GameEvent.UseShame); assertNull(s.lastError); assertEquals(1,s.testResolution!!.successes); assertFalse(s.kid.shameAvailable)
  }

  @Test fun snapshotPreservesFloodProfileAndFields(){
    val teen=RandomTeenFactory.create(Random(11)).copy(scars=listOf(FloodScarState("corte na sobrancelha",true,true)),hooks=listOf("Helmi desapareceu"),friction="Dennis não confia em mim")
    val s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,extended=ExtendedTroubleState(rulesProfile=RulesProfile.FLOOD_TEENS,kids=2,threatLevel=ThreatLevel.NORMAL))
    val restored=GameSnapshotCodec.decode(GameSnapshotCodec.encode(s))!!
    assertEquals(RulesProfile.FLOOD_TEENS,restored.rulesProfile)
    assertEquals(teen.shameText,restored.kid.shameText)
    assertEquals(teen.birthday,restored.kid.birthday)
    assertEquals(1,restored.kid.scars.size)
    assertEquals(6,restored.extended.target)
  }

  @Test fun motorFirewallCannotCrossProfiles(){
    val teen=RandomTeenFactory.create(Random(1))
    val state=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen)
    val wrong=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(profile=MotorProfile.TALES_KIDS,actions=listOf(MotorAction.Message("x"))))
    assertEquals(0,wrong.acceptedActions); assertTrue(wrong.rejected.isNotEmpty())
    val right=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(profile=MotorProfile.FLOOD_TEENS,actions=listOf(MotorAction.Message("x"))))
    assertEquals(1,right.acceptedActions)
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/FloodProfileFoundationV1316Test.kt',test)

resume=root/'TALES_RETOMADA_CURRENT.md'
text=resume.read_text(encoding='utf-8') if resume.exists() else '# TALES FROM THE LOOP — RETOMADA ATUAL\n'
text+='''\n## Bloco 1.3.16a-dev — fundação Things from the Flood\n- Perfil mecânico explícito TALES_KIDS / FLOOD_TEENS adicionado ao estado e ao firewall do Mestre.\n- Regras de criação Flood locais: Teens 14–19, 14 pontos de Atributos, 10 de Perícias e 10 Tipos oficiais codificados.\n- RandomTeenFactory e conversão Kid→Teen redistribuem Atributos/Perícias em vez de carregar progressão mecânica antiga.\n- Vergonha, aniversário, Cicatrizes, Hooks e Friction ganharam estado persistível; Sorte/Orgulho são bloqueados no perfil Flood.\n- Pool mínimo de 1 dado e alvos 3x/4x/5x ficam exclusivos de Flood; Tales continua isolado em 2x/3x/4x.\n- Motor/RAG recebe somente o contrato mecânico do perfil ativo.\n- Este bloco é código/teste, sem APK.\n'''
resume.write_text(text,encoding='utf-8')

# Hard static checks.
assert 'FLOOD_TEENS' in read('app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt')
assert 'rulesProfile: RulesProfile' in read('app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt')
assert 'CanonicalFloodRules.MIN_NON_BROKEN_POOL' in read('app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt')
assert 'RulesProfiles.mechanicalContract(state.rulesProfile)' in read('app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt')
print('TALES/FLOOD 1.3.16a profile foundation: OK')
