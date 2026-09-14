package com.braseiro.talesvtt.game

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
