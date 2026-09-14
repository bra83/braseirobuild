from pathlib import Path
import re, sys

root=Path(sys.argv[1])
def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,s):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
def one(s,old,new,label):
    if old not in s: raise SystemExit('missing anchor: '+label)
    return s.replace(old,new,1)

# Source-only dev version. No APK/release in this block.
rel='app/build.gradle.kts'; s=read(rel)
s=re.sub(r'versionCode\s*=\s*\d+','versionCode = 19',s,count=1)
s=re.sub(r'versionName\s*=\s*"[^"]+"','versionName = "1.3.15-dev"',s,count=1)
write(rel,s)

canonical='''package com.braseiro.talesvtt.game

data class TalesTypeRule(val id: String, val label: String, val keySkills: Set<String>)

enum class TestPurpose { STANDARD, BUILD_ITEM, PROGRAM_ITEM, LEADERSHIP_PREPARE, LEADERSHIP_COMFORT, QUESTIONING }
enum class BonusEffectKind { NARRATIVE, GIVE_SUCCESS, SAME_TROUBLE_WAIVER, INFORMATION_DIE, ITEM_BONUS_UPGRADE, HEAL_OTHER_CONDITION, HEAL_SELF_CONDITION, CONTACT_ITEM }
data class BonusEffectRule(val id:String,val skill:String,val label:String,val kind:BonusEffectKind,val repeatable:Boolean=true)

object CanonicalTalesRules {
  const val PROFILE="tales_kid"
  const val MIN_AGE=10
  const val MAX_CREATION_AGE=15
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
  const val MYSTERY_ITEM_MIN_BONUS=1
  const val MYSTERY_ITEM_MAX_BONUS=3
  const val INFORMATION_BONUS_MAX=3

  val attributes=listOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE")
  val skillToAttribute:Map<String,String> = linkedMapOf(
    "Furtividade" to "CORPO","Força" to "CORPO","Movimentação" to "CORPO",
    "Mecânica" to "TECNOLOGIA","Programação" to "TECNOLOGIA","Cálculo" to "TECNOLOGIA",
    "Contatos" to "CORAÇÃO","Charme" to "CORAÇÃO","Liderança" to "CORAÇÃO",
    "Investigação" to "MENTE","Conhecimento" to "MENTE","Empatia" to "MENTE")
  val types=listOf(
    TalesTypeRule("TAL-TYPE-001","Livro",setOf("Cálculo","Investigação","Conhecimento")),
    TalesTypeRule("TAL-TYPE-002","Informático",setOf("Cálculo","Programação","Conhecimento")),
    TalesTypeRule("TAL-TYPE-003","Caipira",setOf("Força","Movimentação","Mecânica")),
    TalesTypeRule("TAL-TYPE-004","Jocão",setOf("Força","Movimentação","Contatos")),
    TalesTypeRule("TAL-TYPE-005","Criança Popular",setOf("Contatos","Charme","Liderança")),
    TalesTypeRule("TAL-TYPE-006","Rocker",setOf("Movimentação","Charme","Empatia")),
    TalesTypeRule("TAL-TYPE-007","Encrenqueiro",setOf("Força","Furtividade","Liderança")),
    TalesTypeRule("TAL-TYPE-008","Estranho",setOf("Furtividade","Investigação","Empatia")))
  val mildConditions=setOf(Condition.UPSET,Condition.SCARED,Condition.EXHAUSTED,Condition.INJURED)

  val bonusEffects=listOf(
    BonusEffectRule("sneak_give","Furtividade","Dar 1 sucesso a outro Kid",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("sneak_find","Furtividade","Encontrar algo inesperado ou mais do que procurava",BonusEffectKind.NARRATIVE),
    BonusEffectRule("force_give","Força","Dar 1 sucesso a outro Kid",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("force_impress","Força","Impressionar, assustar ou humilhar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("force_pin","Força","Prender o oponente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("force_take","Força","Tomar algo do oponente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("force_unconscious","Força","Deixar o oponente inconsciente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("force_repeat","Força","Não rolar novamente para exatamente o mesmo Problema",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("force_safe","Força","Evitar dano colateral",BonusEffectKind.NARRATIVE),
    BonusEffectRule("move_give","Movimentação","Dar 1 sucesso a outro Kid",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("move_impress","Movimentação","Impressionar alguém",BonusEffectKind.NARRATIVE),
    BonusEffectRule("move_unnoticed","Movimentação","Ninguém percebe o Kid",BonusEffectKind.NARRATIVE),
    BonusEffectRule("tinker_plus","Mecânica","Aumentar em +1 o Item criado, até +3",BonusEffectKind.ITEM_BONUS_UPGRADE),
    BonusEffectRule("tinker_give","Mecânica","Dar 1 sucesso a outro Kid",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("tinker_repeat","Mecânica","Não rolar novamente para exatamente o mesmo Problema",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("tinker_fast","Mecânica","Fazer rapidamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("tinker_quiet","Mecânica","Fazer silenciosamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("tinker_show","Mecânica","Exibir habilidade",BonusEffectKind.NARRATIVE),
    BonusEffectRule("program_plus","Programação","Aumentar em +1 o Item criado, até +3",BonusEffectKind.ITEM_BONUS_UPGRADE),
    BonusEffectRule("program_give","Programação","Dar 1 sucesso a outro Kid",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("program_repeat","Programação","Não rolar novamente para exatamente o mesmo Problema",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("program_fast","Programação","Fazer rapidamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("program_info","Programação","Obter informação nova ou inesperada",BonusEffectKind.NARRATIVE),
    BonusEffectRule("program_show","Programação","Exibir habilidade",BonusEffectKind.NARRATIVE),
    BonusEffectRule("calculate_question","Cálculo","Pergunta extra e +1 dado de informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("contact_tools","Contatos","O contato tem as ferramentas certas",BonusEffectKind.NARRATIVE),
    BonusEffectRule("contact_heal","Contatos","O contato cura uma Condição",BonusEffectKind.HEAL_SELF_CONDITION),
    BonusEffectRule("contact_people","Contatos","O contato traz mais pessoas para ajudar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("contact_repeat","Contatos","Não rolar para encontrar este contato de novo neste Mistério",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("contact_knows","Contatos","O contato sabe algo importante",BonusEffectKind.NARRATIVE),
    BonusEffectRule("contact_item","Contatos","Usar o contato uma vez como Item +1, até +3",BonusEffectKind.CONTACT_ITEM),
    BonusEffectRule("charm_believe","Charme","A pessoa continua acreditando",BonusEffectKind.NARRATIVE),
    BonusEffectRule("charm_relation","Charme","Criar uma relação duradoura",BonusEffectKind.NARRATIVE),
    BonusEffectRule("charm_risk","Charme","A pessoa corre riscos para ajudar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("charm_convince","Charme","A pessoa tenta convencer outros",BonusEffectKind.NARRATIVE),
    BonusEffectRule("charm_impress","Charme","A pessoa fica assustada, confusa ou impressionada",BonusEffectKind.NARRATIVE),
    BonusEffectRule("charm_love","Charme","A pessoa se apaixona",BonusEffectKind.NARRATIVE),
    BonusEffectRule("lead_more","Liderança","Curar mais uma Condição do Kid confortado",BonusEffectKind.HEAL_OTHER_CONDITION),
    BonusEffectRule("lead_self","Liderança","Curar uma Condição própria",BonusEffectKind.HEAL_SELF_CONDITION),
    BonusEffectRule("investigate_question","Investigação","Pergunta extra e +1 dado de informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("comprehend_info","Conhecimento","Informação extra e +1 dado de informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("empathize_question","Empatia","Pergunta extra e +1 dado de informação",BonusEffectKind.INFORMATION_DIE))

  fun effectsForSkill(skill:String)=bonusEffects.filter{it.skill.equals(skill.trim(),true)}
  fun effect(id:String)=bonusEffects.firstOrNull{it.id==id}
  fun maxLuck(age:Int)=(15-age).coerceAtLeast(0)
  fun validMysteryItemBonus(bonus:Int)=bonus in 1..3
  fun threatTarget(kids:Int,level:ThreatLevel)=kids*level.multiplier
  fun troubleKey(reason:String)=reason.trim().lowercase().replace(Regex("\\s+")," ")
  fun leadershipPool(successes:Int)=successes.coerceIn(0,3)*2
  fun purposeAllowed(skill:String,purpose:TestPurpose)=when(purpose){
    TestPurpose.STANDARD->true
    TestPurpose.BUILD_ITEM->skill.equals("Mecânica",true)
    TestPurpose.PROGRAM_ITEM->skill.equals("Programação",true)
    TestPurpose.LEADERSHIP_PREPARE,TestPurpose.LEADERSHIP_COMFORT->skill.equals("Liderança",true)
    TestPurpose.QUESTIONING->skill in setOf("Cálculo","Investigação","Conhecimento","Empatia")
  }
  fun validItem(item:KidItemState)=when(item.kind){
    KidItemKind.ORDINARY->item.name.isNotBlank()&&item.bonus==0
    KidItemKind.MYSTERY->item.name.isNotBlank()&&validMysteryItemBonus(item.bonus)
  }
  fun localMechanicalContract()="""
PROFILE=TALES/KIDS. LOCAL ENGINE IS THE ONLY MECHANICAL AUTHORITY; RAG/LLM IS NARRATIVE REFERENCE ONLY.
Creation: age 10..15; Attributes 1..5 summing to age; Luck=15-age; 10 Skill points; Type key Skills max3, others max1 at creation.
Test = Attribute + Skill + approved Item + information bonus + at most one Help - one die per light Condition; every 6 is one success.
Luck: after failure, once per roll, reroll non-6. Push: once, mark Condition then reroll non-6. Pride: +1 automatic success.
Iconic Item +2. Mystery Items +1..+3 as stored. Ordinary props +0. Mystery Items remain through Change so one may replace the Iconic Item; all other Mystery Items are then lost.
Each success above the required successes buys one effect from the local Skill effect catalog; no invented effects.
Information effects bank +1 die each, max +3. Leadership preparation gives pool 2/4/6 for 1/2/3+ successes, never for the leader.
Conditions: Chateado, Assustado, Exausto, Machucado. Broken auto-fails until valid recovery. NPCs never roll.
Extended Trouble: target Normal=2x Kids, Difficult=3x, Almost Impossible=4x, one scene/roll per Kid.
XP: each Yes among five questions =1; a present Kid always gets participation XP; 5 XP raises one Skill, max5; max10 stored.
TAL-EDGE-001 pool<=0 for non-Broken remains blocked. Never import Flood minimum die, Shame, Scars or Friction.
""".trimIndent()
}
'''
write('app/src/main/java/com/braseiro/talesvtt/game/CanonicalTalesRules.kt',canonical)

# Core data structures.
rel='app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt'; s=read(rel)
s=one(s,'''data class TestRequest(
  val reason: String,
  val attributeName: String,
  val attributeValue: Int,
  val skillName: String? = null,
  val skillValue: Int = 0,
  val requiredSuccesses: Int = 1,
  val approvedItemBonus: Int = 0,
  val helperKid: String? = null,
  val extendedSceneIndex: Int? = null
) {
  fun basePool(): Int = attributeValue + if (skillName == null) 0 else skillValue
}

data class TestResolution(
  val dice: List<Int>,
  val successes: Int,
  val luckUsed: Boolean = false,
  val pushed: Boolean = false,
  val prideUsed: Boolean = false,
  val reportedToGm: Boolean = false
)''','''data class TestRequest(
  val reason: String,
  val attributeName: String,
  val attributeValue: Int,
  val skillName: String? = null,
  val skillValue: Int = 0,
  val requiredSuccesses: Int = 1,
  val approvedItemBonus: Int = 0,
  val informationBonus: Int = 0,
  val helperKid: String? = null,
  val extendedSceneIndex: Int? = null,
  val purpose: TestPurpose = TestPurpose.STANDARD,
  val targetKid: String? = null
) {
  fun basePool(): Int = attributeValue + if (skillName == null) 0 else skillValue
}

data class BonusEffectPurchase(val effectId: String, val target: String? = null)

data class TestResolution(
  val dice: List<Int>,
  val successes: Int,
  val luckUsed: Boolean = false,
  val pushed: Boolean = false,
  val prideUsed: Boolean = false,
  val reportedToGm: Boolean = false,
  val bonusEffects: List<BonusEffectPurchase> = emptyList(),
  val baseEffectApplied: Boolean = false
)''','test structures')
s=one(s,'''  val endingMode: EndingMode = EndingMode.NONE,
  val change: ChangeState = ChangeState(),
  val lastError: String? = null''','''  val endingMode: EndingMode = EndingMode.NONE,
  val change: ChangeState = ChangeState(),
  val informationBonusBank: Int = 0,
  val leadershipPoolDice: Int = 0,
  val leadershipAssignments: Map<String, Int> = emptyMap(),
  val grantedSuccesses: Map<String, Int> = emptyMap(),
  val waivedTroubles: Set<String> = emptySet(),
  val lastError: String? = null''','game bonus fields')
s=s.replace('test.basePool() + test.approvedItemBonus + (if (test.helperKid != null)','test.basePool() + test.approvedItemBonus + test.informationBonus + (if (test.helperKid != null)',1)
s=one(s,'''  data class ApplyApprovedItem(val bonus: Int) : GameEvent
  data class AddItem(val item: KidItemState) : GameEvent''','''  data class ApplyApprovedItem(val bonus: Int) : GameEvent
  data object ApplyInformationBonus : GameEvent
  data class PurchaseBonusEffect(val effectId: String, val target: String? = null) : GameEvent
  data class AssignLeadershipDice(val kid: String, val dice: Int = 1) : GameEvent
  data class PromoteMysteryItemToIconic(val itemName: String) : GameEvent
  data class AddItem(val item: KidItemState) : GameEvent''','new events')

# Request validation and same-Trouble effect.
s=one(s,'''      is GameEvent.GmRequestTest -> {
        if (event.request.attributeValue < 0 || event.request.skillValue < 0 || event.request.requiredSuccesses !in 1..3) {
          fail("Teste inválido.")
        } else ok(state.copy(activeTest = event.request, testResolution = null))
      }''','''      is GameEvent.GmRequestTest -> {
        val skill = event.request.skillName?.let(TalesSkillCatalog::canonicalSkill)
        val key = CanonicalTalesRules.troubleKey(event.request.reason)
        when {
          event.request.attributeValue < 0 || event.request.skillValue < 0 || event.request.requiredSuccesses !in 1..3 -> fail("Teste inválido.")
          skill == null -> fail("Teste precisa de Perícia canônica Tales/Kids.")
          !CanonicalTalesRules.purposeAllowed(skill, event.request.purpose) -> fail("Finalidade incompatível com a Perícia.")
          key in state.waivedTroubles -> ok(state.copy(activeTest=event.request,testResolution=TestResolution(emptyList(),event.request.requiredSuccesses)))
          else -> ok(state.copy(activeTest=event.request,testResolution=null))
        }
      }''','request test')

# Insert local effects after item approval.
needle='''      is GameEvent.ApplyApprovedItem -> {
        val test = state.activeTest ?: return fail("Não há teste ativo.")
        if (state.testResolution != null) fail("Item deve ser aprovado antes da rolagem.")
        else if (event.bonus !in 0..3) fail("Bônus de item inválido.")
        else ok(state.copy(activeTest = test.copy(approvedItemBonus = event.bonus)))
      }
'''
extra='''
      GameEvent.ApplyInformationBonus -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        when {
          state.testResolution!=null -> fail("Bônus de informação deve ser usado antes da rolagem.")
          state.informationBonusBank<=0 -> fail("Nenhum bônus de informação disponível.")
          test.informationBonus>=3 -> fail("Máximo de +3 dados de informação.")
          else -> ok(state.copy(informationBonusBank=state.informationBonusBank-1,activeTest=test.copy(informationBonus=test.informationBonus+1)))
        }
      }

      is GameEvent.PurchaseBonusEffect -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        val result=state.testResolution ?: return fail("Role antes de comprar efeito.")
        val skill=test.skillName?.let(TalesSkillCatalog::canonicalSkill) ?: return fail("Perícia inválida.")
        val rule=CanonicalTalesRules.effect(event.effectId) ?: return fail("Efeito bônus não canônico.")
        val credits=(result.successes-test.requiredSuccesses-result.bonusEffects.size).coerceAtLeast(0)
        when {
          credits<=0 -> fail("Não há sucessos extras disponíveis.")
          !rule.skill.equals(skill,true) -> fail("Efeito não pertence à Perícia usada.")
          !rule.repeatable && result.bonusEffects.any{it.effectId==rule.id} -> fail("Este efeito não pode repetir.")
          rule.kind==BonusEffectKind.ITEM_BONUS_UPGRADE && test.purpose !in setOf(TestPurpose.BUILD_ITEM,TestPurpose.PROGRAM_ITEM) -> fail("Este efeito exige criação de Item.")
          else -> {
            var next=state
            when(rule.kind){
              BonusEffectKind.GIVE_SUCCESS -> {
                val target=event.target.orEmpty().trim()
                val member=state.group.members.firstOrNull{it.name.equals(target,true)||it.name.substringBefore(' ').equals(target,true)} ?: return fail("Escolha outro Kid do grupo.")
                if(member.name.equals(state.kid.name,true)) return fail("O Kid não pode dar sucesso a si mesmo.")
                next=next.copy(grantedSuccesses=next.grantedSuccesses+(member.name to ((next.grantedSuccesses[member.name]?:0)+1)))
              }
              BonusEffectKind.SAME_TROUBLE_WAIVER -> next=next.copy(waivedTroubles=next.waivedTroubles+CanonicalTalesRules.troubleKey(test.reason))
              BonusEffectKind.INFORMATION_DIE -> {
                if(next.informationBonusBank>=3) return fail("Bônus de informação já está no máximo +3.")
                next=next.copy(informationBonusBank=next.informationBonusBank+1)
              }
              BonusEffectKind.HEAL_SELF_CONDITION -> {
                if(next.kid.broken) return fail("Este efeito não cura Quebrado.")
                val c=event.target?.let{raw->Condition.entries.firstOrNull{it.name.equals(raw,true)||it.label.equals(raw,true)}} ?: return fail("Escolha uma Condição para curar.")
                if(c !in next.kid.conditions) return fail("Condição não está marcada.")
                next=next.copy(kid=next.kid.copy(conditions=next.kid.conditions-c))
              }
              else -> Unit
            }
            ok(next.copy(testResolution=result.copy(bonusEffects=result.bonusEffects+BonusEffectPurchase(rule.id,event.target))))
          }
        }
      }

      is GameEvent.AssignLeadershipDice -> {
        val member=state.group.members.firstOrNull{it.name.equals(event.kid,true)||it.name.substringBefore(' ').equals(event.kid,true)}
        when {
          event.dice<=0 -> fail("Quantidade inválida.")
          member==null -> fail("Kid alvo não pertence ao grupo.")
          member.name.equals(state.kid.name,true) -> fail("O líder não usa o próprio pool.")
          event.dice>state.leadershipPoolDice -> fail("Pool de Liderança insuficiente.")
          else -> ok(state.copy(leadershipPoolDice=state.leadershipPoolDice-event.dice,leadershipAssignments=state.leadershipAssignments+(member.name to ((state.leadershipAssignments[member.name]?:0)+event.dice))))
        }
      }

      is GameEvent.PromoteMysteryItemToIconic -> {
        val item=state.kid.items.firstOrNull{it.kind==KidItemKind.MYSTERY && it.name.equals(event.itemName.trim(),true)}
        when {
          state.endingMode!=EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("Só entre Mistérios.")
          state.change.characterReviewConfirmed -> fail("Mudança já confirmada.")
          item==null -> fail("Item do Mistério não encontrado.")
          else -> ok(state.copy(kid=state.kid.copy(iconicItemName=item.name,items=state.kid.items-item)))
        }
      }
'''
if needle not in s: raise SystemExit('missing item approval block')
s=s.replace(needle,needle+extra,1)

# Leadership prep base result.
s=one(s,'ok(state.copy(testResolution = TestResolution(event.dice, event.dice.count { it == 6 })))','''run {
            val successes=event.dice.count{it==6}
            val pool=if(state.activeTest?.purpose==TestPurpose.LEADERSHIP_PREPARE && successes >= (state.activeTest?.requiredSuccesses ?: 1)) CanonicalTalesRules.leadershipPool(successes) else state.leadershipPoolDice
            ok(state.copy(leadershipPoolDice=pool,leadershipAssignments=if(pool!=state.leadershipPoolDice) emptyMap() else state.leadershipAssignments,testResolution=TestResolution(event.dice,successes)))
          }''','roll result')

# Keep Mystery Items until Change, then discard remaining ones.
s=s.replace(',\n            items = state.kid.items.filterNot { it.kind == KidItemKind.MYSTERY }','',1)
s=s.replace(',\n              items = if (state.endingMode == EndingMode.MYSTERY) state.kid.items.filterNot { it.kind == KidItemKind.MYSTERY } else state.kid.items','',1)
s=one(s,'else -> ok(state.copy(change = state.change.copy(characterReviewConfirmed = true)))','''else -> ok(state.copy(
            kid=state.kid.copy(items=state.kid.items.filterNot{it.kind==KidItemKind.MYSTERY}),
            change=state.change.copy(characterReviewConfirmed=true)
          ))''','change cleanup')
# Participation is automatic if present.
s=s.replace('if (event.index !in 0..4) fail("Pergunta de XP inválida.")','if (event.index !in 0..4) fail("Pergunta de XP inválida.")\n        else if (event.index == 0 && !event.yes) fail("Um Kid presente sempre recebe o XP de participação.")',1)
# Reset Mystery-limited stores.
s=one(s,'''            lastPlayerAction = null
          ))''','''            lastPlayerAction = null,
            informationBonusBank = 0,
            leadershipPoolDice = 0,
            leadershipAssignments = emptyMap(),
            grantedSuccesses = emptyMap(),
            waivedTroubles = emptySet()
          ))''','next session reset')
write(rel,s)

# Snapshot v12.
rel='app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt'; s=read(rel)
s=s.replace('private const val VERSION = 11','private const val VERSION = 12',1)
s=one(s,'      line("test.item", test.approvedItemBonus)\n      line("test.helper",','''      line("test.item", test.approvedItemBonus)
      line("test.info", test.informationBonus)
      line("test.purpose", test.purpose.name)
      line("test.targetKid", test.targetKid?.let(::enc) ?: "")
      line("test.helper",''','snapshot test encode')
s=one(s,'      line("result.reportedToGm", result.reportedToGm)\n    }\n\n    line("reveal.clues",','''      line("result.reportedToGm", result.reportedToGm)
      line("result.baseEffectApplied", result.baseEffectApplied)
      line("result.bonusEffects", result.bonusEffects.joinToString("|") { "${enc(it.effectId)}~${it.target?.let(::enc) ?: ""}" })
    }

    line("bonus.informationBank", state.informationBonusBank)
    line("bonus.leadershipPool", state.leadershipPoolDice)
    line("bonus.leadershipAssignments", state.leadershipAssignments.entries.joinToString("|") { "${enc(it.key)}~${it.value}" })
    line("bonus.grantedSuccesses", state.grantedSuccesses.entries.joinToString("|") { "${enc(it.key)}~${it.value}" })
    line("bonus.waivedTroubles", state.waivedTroubles.joinToString("|") { enc(it) })

    line("reveal.clues",''','snapshot bonus encode')
s=one(s,'''        approvedItemBonus = int("test.item"),
        helperKid = decoded("test.helper").ifBlank { null },
        extendedSceneIndex = if (version >= 6) map["test.extendedSceneIndex"]?.toIntOrNull() else null
      )''','''        approvedItemBonus = int("test.item"),
        informationBonus = if (version >= 12) int("test.info") else 0,
        helperKid = decoded("test.helper").ifBlank { null },
        extendedSceneIndex = if (version >= 6) map["test.extendedSceneIndex"]?.toIntOrNull() else null,
        purpose = if (version >= 12) runCatching { TestPurpose.valueOf(map["test.purpose"].orEmpty()) }.getOrDefault(TestPurpose.STANDARD) else TestPurpose.STANDARD,
        targetKid = if (version >= 12) decoded("test.targetKid").ifBlank { null } else null
      )''','snapshot test decode')
s=one(s,'''        prideUsed = bool("result.prideUsed"),
        reportedToGm = if (version >= 5) bool("result.reportedToGm") else false
      )''','''        prideUsed = bool("result.prideUsed"),
        reportedToGm = if (version >= 5) bool("result.reportedToGm") else false,
        bonusEffects = if (version >= 12) map["result.bonusEffects"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
          val p=raw.split('~',limit=2); if(p.isEmpty()) null else BonusEffectPurchase(dec(p[0]),p.getOrNull(1)?.takeIf{it.isNotBlank()}?.let(::dec))
        } else emptyList(),
        baseEffectApplied = if (version >= 12) bool("result.baseEffectApplied") else false
      )''','snapshot result decode')
s=one(s,'    val scenes = map["ext.scenes"].orEmpty().split(\'|\')','''    fun decodeIntMap(key:String):Map<String,Int> = map[key].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull{raw ->
      val p=raw.split('~',limit=2); p.getOrNull(1)?.toIntOrNull()?.let{dec(p[0]) to it}
    }.toMap()

    val scenes = map["ext.scenes"].orEmpty().split('|')''','snapshot int map helper')
s=one(s,'''      endingMode = endingMode,
      change = change,
      lastError = null''','''      endingMode = endingMode,
      change = change,
      informationBonusBank = if (version >= 12) int("bonus.informationBank") else 0,
      leadershipPoolDice = if (version >= 12) int("bonus.leadershipPool") else 0,
      leadershipAssignments = if (version >= 12) decodeIntMap("bonus.leadershipAssignments") else emptyMap(),
      grantedSuccesses = if (version >= 12) decodeIntMap("bonus.grantedSuccesses") else emptyMap(),
      waivedTroubles = if (version >= 12) map["bonus.waivedTroubles"].orEmpty().split('|').filter{it.isNotBlank()}.map(::dec).toSet() else emptySet(),
      lastError = null''','snapshot GameState decode')
write(rel,s)

# Motor TestRequest purpose + craft enforcement.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt'; s=read(rel)
s=s.replace('data class TestRequest(val skill: String, val requiredSuccesses: Int, val label: String) : MotorAction','''data class TestRequest(val skill:String,val requiredSuccesses:Int,val label:String,val purpose:TestPurpose=TestPurpose.STANDARD,val targetKid:String?=null) : MotorAction''',1)
s=one(s,'''              requiredSuccesses = action.requiredSuccesses
            )
          }''','''              requiredSuccesses = action.requiredSuccesses
            )?.copy(purpose=action.purpose,targetKid=action.targetKid)
          }''','contract purpose copy')
s=one(s,'''        is MotorAction.GiveItem -> {
          if (!CanonicalTalesRules.validItem(action.item)) rejected += "ação[$index] item_add: Item viola bônus/tipo canônico."
          else reduce(GameEvent.AddItem(action.item), "item_add")
        }''','''        is MotorAction.GiveItem -> {
          val test=state.activeTest; val result=state.testResolution
          val creating=test?.purpose in setOf(TestPurpose.BUILD_ITEM,TestPurpose.PROGRAM_ITEM) && result!=null
          val upgrades=result?.bonusEffects?.count{CanonicalTalesRules.effect(it.effectId)?.kind==BonusEffectKind.ITEM_BONUS_UPGRADE} ?: 0
          val expected=(1+upgrades).coerceAtMost(3)
          when {
            !CanonicalTalesRules.validItem(action.item) -> rejected += "ação[$index] item_add: Item viola bônus/tipo canônico."
            creating && (action.item.kind!=KidItemKind.MYSTERY || action.item.bonus!=expected) -> rejected += "ação[$index] item_add: Item criado deve ter bônus local +$expected."
            else -> reduce(GameEvent.AddItem(action.item),"item_add")
          }
        }''','craft firewall')
write(rel,s)

# Wire purpose.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt'; s=read(rel)
s=one(s,'''      if (skill.isEmpty()) null else MotorAction.TestRequest(
        skill = skill,
        requiredSuccesses = a.optInt("required", 1),
        label = a.optString("label", skill)
      )''','''      val purpose=when(a.optString("purpose","standard").lowercase()) {
        "build_item","build","tinker_build" -> TestPurpose.BUILD_ITEM
        "program_item","program_create" -> TestPurpose.PROGRAM_ITEM
        "leadership_prepare","lead_prepare" -> TestPurpose.LEADERSHIP_PREPARE
        "leadership_comfort","comfort" -> TestPurpose.LEADERSHIP_COMFORT
        "questioning","questions" -> TestPurpose.QUESTIONING
        else -> TestPurpose.STANDARD
      }
      if (skill.isEmpty()) null else MotorAction.TestRequest(skill,a.optInt("required",1),a.optString("label",skill),purpose,a.optString("targetKid").trim().ifBlank{null})''','wire purpose')
write(rel,s)

# Transport intent reports purchased effects and purpose; fix recovery schema.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt'; s=read(rel)
s=one(s,'''  val extendedSkill: String? = null,
  val extendedSceneIndex: Int? = null
)''','''  val extendedSkill: String? = null,
  val extendedSceneIndex: Int? = null,
  val bonusEffects: List<String> = emptyList()
)''','intent effects')
s=s.replace('    intent.extendedSceneIndex?.let { intentJson.put("extendedSceneIndex", it) }','    intent.extendedSceneIndex?.let { intentJson.put("extendedSceneIndex", it) }\n    if (intent.bonusEffects.isNotEmpty()) intentJson.put("bonusEffects", JSONArray(intent.bonusEffects))',1)
s=s.replace('.put("extendedSceneIndex", test.extendedSceneIndex)','.put("extendedSceneIndex", test.extendedSceneIndex)\n        .put("purpose", test.purpose.name.lowercase())\n        .put("targetKid", test.targetKid)\n        .put("informationBonus", test.informationBonus)',1)
s=s.replace('- recovery {approved:true|false}','- recovery {approved:true|false,source: anchor|hideout|comfort,condition?}',1)
s=s.replace('- test_request {skill,required,label}','- test_request {skill,required,label,purpose?: standard|build_item|program_item|leadership_prepare|leadership_comfort|questioning,targetKid?}',1)
s=s.replace('- extended_scene_result {kid,successes} — somente para resultado verificado de outro cliente/jogador','- extended_scene_result {kid,successes} — somente para resultado verificado de outro cliente/jogador\n- player_intent.bonusEffects contém somente efeitos já comprados e validados localmente; apenas narre suas consequências, nunca troque/invente efeitos.',1)
write(rel,s)

# ViewModel local controls + effect report.
rel='app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt'; s=read(rel)
s=one(s,'  fun usePride() = dispatch(GameEvent.UsePride)\n  fun setXpAnswer','''  fun usePride() = dispatch(GameEvent.UsePride)
  fun applyInformationBonus() = dispatch(GameEvent.ApplyInformationBonus)
  fun purchaseBonusEffect(effectId:String,target:String?) = dispatch(GameEvent.PurchaseBonusEffect(effectId,target))
  fun assignLeadershipDice(kid:String) = dispatch(GameEvent.AssignLeadershipDice(kid,1))
  fun promoteMysteryItemToIconic(itemName:String) = dispatch(GameEvent.PromoteMysteryItemToIconic(itemName))
  fun setXpAnswer''','VM methods')
s=s.replace('      extendedSceneIndex = extendedIndex\n    ))','      extendedSceneIndex = extendedIndex,\n      bonusEffects = result.bonusEffects.map { it.effectId + (it.target?.let { target -> ":$target" } ?: "") }\n    ))',1)
write(rel,s)

# UI: information and canonical extra-success choices.
rel='app/src/main/java/com/braseiro/talesvtt/screens/TroubleScreen.kt'; s=read(rel)
s=s.replace('import com.braseiro.talesvtt.game.Condition\n','import com.braseiro.talesvtt.game.Condition\nimport com.braseiro.talesvtt.game.CanonicalTalesRules\nimport com.braseiro.talesvtt.game.BonusEffectKind\n',1)
s=one(s,'  onPride: () -> Unit,\n  onSubmitResult: () -> Unit,','''  onPride: () -> Unit,
  onInformationBonus: () -> Unit,
  onPurchaseBonusEffect: (String,String?) -> Unit,
  onSubmitResult: () -> Unit,''','Trouble signature')
s=one(s,'      if (result == null) {\n        PrimaryButton(','''      if (result == null) {
        if (gameState.informationBonusBank > 0 && test.informationBonus < 3) {
          SecondaryButton(text="USAR +1 DADO DE INFORMAÇÃO • ${gameState.informationBonusBank} guardado(s)",onClick=onInformationBonus,modifier=Modifier.fillMaxWidth())
          Spacer(modifier=Modifier.height(7.dp))
        }
        PrimaryButton(''','pre-roll info')
s=s.replace('        RollResultPanel(gameState, onLuck, onPush, onPride)','        RollResultPanel(gameState, onLuck, onPush, onPride)\n        CanonicalBonusEffects(gameState,onPurchaseBonusEffect)',1)
marker='''@Composable
private fun RollTerm(iconRes: Int, label: String, value: String) {'''
bonus='''@Composable
private fun CanonicalBonusEffects(gameState:GameState,onPurchase:(String,String?)->Unit) {
  val test=gameState.activeTest ?: return
  val result=gameState.testResolution ?: return
  val skill=test.skillName?.let{com.braseiro.talesvtt.game.TalesSkillCatalog.canonicalSkill(it)} ?: return
  val credits=(result.successes-test.requiredSuccesses-result.bonusEffects.size).coerceAtLeast(0)
  if(credits<=0) return
  val rules=CanonicalTalesRules.effectsForSkill(skill)
  if(rules.isEmpty()) return
  Spacer(modifier=Modifier.height(10.dp))
  Text("SUCESSOS EXTRAS • $credits PARA GASTAR",style=AppTheme.typography.label.copy(color=AppTheme.colors.accentPurple))
  rules.forEach { rule ->
    val target=when(rule.kind) {
      BonusEffectKind.GIVE_SUCCESS -> gameState.group.members.firstOrNull{!it.name.equals(gameState.kid.name,true)}?.name
      BonusEffectKind.HEAL_SELF_CONDITION -> gameState.kid.conditions.firstOrNull()?.name
      else -> null
    }
    val needsTarget=rule.kind in setOf(BonusEffectKind.GIVE_SUCCESS,BonusEffectKind.HEAL_SELF_CONDITION)
    SecondaryButton(text=rule.label.uppercase(),onClick={onPurchase(rule.id,target)},enabled=!needsTarget||target!=null,modifier=Modifier.fillMaxWidth())
  }
}

@Composable
private fun RollTerm(iconRes: Int, label: String, value: String) {'''
s=one(s,marker,bonus,'bonus UI')
write(rel,s)

# Ending: promote a Mystery Item before the rest are lost.
rel='app/src/main/java/com/braseiro/talesvtt/screens/EndingScreen.kt'; s=read(rel)
s=s.replace('import com.braseiro.talesvtt.game.TalesSkillCatalog\n','import com.braseiro.talesvtt.game.TalesSkillCatalog\nimport com.braseiro.talesvtt.game.KidItemKind\n',1)
s=one(s,'  onConfirmCharacterChanges: () -> Unit,\n  onStartNextSession: () -> Unit,','''  onConfirmCharacterChanges: () -> Unit,
  onPromoteMysteryItem: (String) -> Unit,
  onStartNextSession: () -> Unit,''','Ending signature')
s=one(s,'          OutlinedTextField(changeRelationships, { changeRelationships = it }, label = { Text("RELACIONAMENTOS • Nome — descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 2)\n          Spacer(modifier = Modifier.height(8.dp))','''          OutlinedTextField(changeRelationships, { changeRelationships = it }, label = { Text("RELACIONAMENTOS • Nome — descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
          val mysteryItems=gameState.kid.items.filter{it.kind==KidItemKind.MYSTERY}
          if(mysteryItems.isNotEmpty()) {
            Spacer(modifier=Modifier.height(8.dp))
            Text("ITENS DESTE MISTÉRIO • opcionalmente escolha um novo Item Icônico",style=AppTheme.typography.label.copy(color=AppTheme.colors.accentAmber))
            mysteryItems.forEach { item ->
              SecondaryButton(text="TORNAR ICÔNICO • ${item.name.uppercase()} • +2",onClick={onPromoteMysteryItem(item.name);changeIconic=item.name},modifier=Modifier.fillMaxWidth())
            }
          }
          Spacer(modifier = Modifier.height(8.dp))''','promotion UI')
write(rel,s)

# Navigation wiring.
rel='app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt'; s=read(rel)
s=one(s,'        onPride = gameViewModel::usePride,\n        onSubmitResult = gameViewModel::submitTestResultToMotor,','''        onPride = gameViewModel::usePride,
        onInformationBonus = gameViewModel::applyInformationBonus,
        onPurchaseBonusEffect = gameViewModel::purchaseBonusEffect,
        onSubmitResult = gameViewModel::submitTestResultToMotor,''','Trouble nav')
s=one(s,'        onConfirmCharacterChanges = gameViewModel::confirmBetweenMysteryReview,\n        onStartNextSession = {','''        onConfirmCharacterChanges = gameViewModel::confirmBetweenMysteryReview,
        onPromoteMysteryItem = gameViewModel::promoteMysteryItemToIconic,
        onStartNextSession = {''','Ending nav')
write(rel,s)

# Regression tests for this closure block.
test='''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class SkillEffectsAndItemsV1315Test {
  private fun req(skill:String,purpose:TestPurpose=TestPurpose.STANDARD)=TestRequest("Mesmo Problema",TalesSkillCatalog.attributeFor(skill)!!,3,skill,2,1,purpose=purpose)
  @Test fun allSkillsHaveEffects(){ assertEquals(12,TalesSkillCatalog.skillToAttribute.size); TalesSkillCatalog.skillToAttribute.keys.forEach{assertTrue(CanonicalTalesRules.effectsForSkill(it).isNotEmpty())} }
  @Test fun itemWaitsForChange(){ val i=KidItemState("Detector","",2,KidItemKind.MYSTERY); var s=GameState(kid=KidState(items=listOf(i)),endingMode=EndingMode.MYSTERY); s=TalesReducer.reduce(s,GameEvent.ApplyMysteryEnd(1)); assertTrue(s.kid.items.isNotEmpty()); s=TalesReducer.reduce(s,GameEvent.ConfirmBetweenMysteryReview); assertTrue(s.kid.items.isEmpty()) }
  @Test fun itemCanBecomeIconic(){ val i=KidItemState("Detector","",3,KidItemKind.MYSTERY); var s=GameState(kid=KidState(items=listOf(i)),endingMode=EndingMode.MYSTERY,sessionXpAwarded=true); s=TalesReducer.reduce(s,GameEvent.PromoteMysteryItemToIconic("Detector")); assertNull(s.lastError); assertEquals("Detector",s.kid.iconicItemName); assertTrue(s.kid.items.isEmpty()); assertEquals(2,CanonicalTalesRules.ICONIC_ITEM_BONUS) }
  @Test fun matchingEffectOnly(){ var s=GameState(activeTest=req("Furtividade"),testResolution=TestResolution(listOf(6,6),2)); s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("sneak_find")); assertNull(s.lastError); val bad=TalesReducer.reduce(s.copy(testResolution=TestResolution(listOf(6,6,6),3)),GameEvent.PurchaseBonusEffect("force_pin")); assertNotNull(bad.lastError) }
  @Test fun informationBank(){ var s=GameState(activeTest=req("Cálculo"),testResolution=TestResolution(listOf(6,6),2)); s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("calculate_question")); assertEquals(1,s.informationBonusBank); s=s.copy(activeTest=req("Investigação"),testResolution=null); s=TalesReducer.reduce(s,GameEvent.ApplyInformationBonus); assertEquals(1,s.activeTest!!.informationBonus); assertEquals(0,s.informationBonusBank) }
  @Test fun sameTroubleAutoResolves(){ var s=GameState(activeTest=req("Força"),testResolution=TestResolution(listOf(6,6),2)); s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("force_repeat")); s=TalesReducer.reduce(s.copy(activeTest=null,testResolution=null),GameEvent.GmRequestTest(req("Força"))); assertEquals(1,s.testResolution!!.successes); assertTrue(s.testResolution!!.dice.isEmpty()) }
  @Test fun leadershipPool(){ var s=GameState(activeTest=req("Liderança",TestPurpose.LEADERSHIP_PREPARE)); s=TalesReducer.reduce(s,GameEvent.RollResult(listOf(6,6,3,2))); assertEquals(4,s.leadershipPoolDice) }
  @Test fun itemUpgradeCount(){ var s=GameState(activeTest=req("Mecânica",TestPurpose.BUILD_ITEM),testResolution=TestResolution(listOf(6,6,6),3)); s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("tinker_plus")); s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("tinker_plus")); assertEquals(2,s.testResolution!!.bonusEffects.size) }
  @Test fun participationCannotBeNo(){ assertNotNull(TalesReducer.reduce(GameState(),GameEvent.SetXpAnswer(0,false)).lastError) }
  @Test fun randomCreationStillCanonical(){ repeat(128){assertTrue(RandomKidFactory.validateCreation(RandomKidFactory.create(kotlin.random.Random(it))))} }
  @Test fun snapshotPreservesNewState(){ val s=GameState(informationBonusBank=2,leadershipPoolDice=4,leadershipAssignments=mapOf("Anita Holmberg" to 2),grantedSuccesses=mapOf("Jonas Berg" to 1),waivedTroubles=setOf("x"),activeTest=req("Cálculo").copy(informationBonus=1),testResolution=TestResolution(listOf(6,6),2,bonusEffects=listOf(BonusEffectPurchase("calculate_question")))); val r=GameSnapshotCodec.decode(GameSnapshotCodec.encode(s))!!; assertEquals(2,r.informationBonusBank); assertEquals(4,r.leadershipPoolDice); assertEquals(1,r.testResolution!!.bonusEffects.size); assertTrue("x" in r.waivedTroubles) }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/SkillEffectsAndItemsV1315Test.kt',test)

resume=root/'TALES_RETOMADA_CURRENT.md'; txt=resume.read_text(encoding='utf-8')
txt+='''\n\n## Bloco 1.3.15-dev — Itens e sucessos extras\n- Itens do Mistério permanecem até Mudança; um pode substituir o Item Icônico (+2) antes de os demais serem perdidos.\n- Os efeitos compráveis por sucessos extras das 12 Perícias foram codificados localmente e não dependem de RAG/Gemini.\n- Bônus de informação, dispensa do mesmo Problema, sucesso cedido e pool de Liderança ganharam estado local persistente.\n- Criação por Mecânica/Programação usa Item +1 base e upgrades locais até +3.\n- XP de participação não pode ser negado a um Kid presente.\n- Bloco somente código/teste; nenhum APK publicado.\n'''
resume.write_text(txt,encoding='utf-8')

assert 'versionName = "1.3.15-dev"' in read('app/build.gradle.kts')
assert 'TORNAR ICÔNICO' in read('app/src/main/java/com/braseiro/talesvtt/screens/EndingScreen.kt')
assert 'SUCESSOS EXTRAS' in read('app/src/main/java/com/braseiro/talesvtt/screens/TroubleScreen.kt')
print('TALES 1.3.15-dev clean transform: OK')
