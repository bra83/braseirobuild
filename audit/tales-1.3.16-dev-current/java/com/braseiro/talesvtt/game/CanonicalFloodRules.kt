package com.braseiro.talesvtt.game

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
