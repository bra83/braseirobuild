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

  val bonusEffects=listOf(
    BonusEffectRule("flood_sneak_give","Furtividade","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_sneak_find","Furtividade","Encontrar algo inesperado ou mais do que procurava",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_give","Força","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_force_impress","Força","Impressionar, assustar ou humilhar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_pin","Força","Prender o oponente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_take","Força","Tomar algo do oponente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_unconscious","Força","Deixar o oponente inconsciente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_repeat","Força","Não rolar novamente para exatamente o mesmo Trouble",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_move_give","Movimentação","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_move_impress","Movimentação","Impressionar alguém",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_move_hidden","Movimentação","Permanecer sem ser detectado",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_tinker_upgrade","Mecânica","Aumentar em +1 o Item criado, até +3",BonusEffectKind.ITEM_BONUS_UPGRADE),
    BonusEffectRule("flood_tinker_give","Mecânica","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_tinker_repeat","Mecânica","Não rolar novamente para exatamente o mesmo Trouble",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_tinker_fast","Mecânica","Fazer rapidamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_tinker_quiet","Mecânica","Fazer silenciosamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_tinker_show","Mecânica","Exibir habilidade",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_program_upgrade","Programação","Aumentar em +1 o Item criado, até +3",BonusEffectKind.ITEM_BONUS_UPGRADE),
    BonusEffectRule("flood_program_give","Programação","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_program_repeat","Programação","Não rolar novamente para exatamente o mesmo Trouble",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_program_fast","Programação","Fazer rapidamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_program_info","Programação","Obter informação nova ou inesperada",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_program_show","Programação","Exibir habilidade",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_calculate_info","Cálculo","Pergunta extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("flood_contact_tools","Contatos","O contato tem as ferramentas certas",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_contact_heal","Contatos","O contato cura uma Condição",BonusEffectKind.HEAL_SELF_CONDITION),
    BonusEffectRule("flood_contact_people","Contatos","O contato traz mais pessoas para ajudar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_contact_repeat","Contatos","Não rolar para encontrar este contato de novo neste Mistério",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_contact_knows","Contatos","O contato sabe algo importante",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_contact_item","Contatos","Usar o contato uma vez como Item +1, até +3",BonusEffectKind.CONTACT_ITEM),
    BonusEffectRule("flood_charm_believe","Charme","A pessoa continua acreditando",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_relation","Charme","Criar uma relação duradoura",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_risk","Charme","A pessoa corre riscos para ajudar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_convince","Charme","A pessoa tenta convencer outros",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_impress","Charme","A pessoa fica assustada, confusa ou impressionada",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_love","Charme","A pessoa se apaixona",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_lead_more","Liderança","Curar mais uma Condição do Teen confortado",BonusEffectKind.HEAL_OTHER_CONDITION),
    BonusEffectRule("flood_lead_self","Liderança","Curar uma Condição própria",BonusEffectKind.HEAL_SELF_CONDITION),
    BonusEffectRule("flood_investigate_info","Investigação","Pergunta extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("flood_comprehend_info","Conhecimento","Informação extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("flood_empathize_info","Empatia","Pergunta extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE)
  )

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

  fun effectsForSkill(skill:String)=bonusEffects.filter{it.skill.equals(skill.trim(),true)}
  fun effect(id:String)=bonusEffects.firstOrNull{it.id==id}
  fun troubleKey(reason:String)=reason.trim().lowercase().replace(Regex("\\s+")," ")
  fun leadershipPool(successes:Int)=successes.coerceIn(0,3)*2
  fun purposeAllowed(skill:String,purpose:TestPurpose)=when(purpose){
    TestPurpose.STANDARD->true
    TestPurpose.BUILD_ITEM->skill.equals("Mecânica",true)
    TestPurpose.PROGRAM_ITEM->skill.equals("Programação",true)
    TestPurpose.LEADERSHIP_PREPARE,TestPurpose.LEADERSHIP_COMFORT->skill.equals("Liderança",true)
    TestPurpose.QUESTIONING->skill in setOf("Cálculo","Investigação","Conhecimento","Empatia")
  }
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
Each time a Teen becomes Broken, a Scar becomes mandatory before recovery. An unused Scar may grant +1 automatic success once, then it is accepted. After every Scar after the first, roll 1d6: if lower than the number of Scars the Teen is removed from play; the sixth Scar inevitably removes the Teen.
Iconic Item +2. Other real Items +1..+3 when applicable; ordinary props +0. Non-Iconic Items are lost between Mysteries unless one becomes the new Iconic Item; replacement Iconic Item is always +2 mechanically.
Each success above the required successes may buy one effect from the Flood Skill effect catalog; the same source-listed effect may be bought repeatedly unless its fiction is inherently one-shot. No Tales-only effect ID is valid in Flood.
Information effects bank +1 die each, max +3. Lead preparation converts 1/2/3+ successes to a 2/4/6 die pool for other Teens; Lead comfort heals one Condition on success and its source-listed bonus effects may heal an additional target Condition or one of the leader's own.
Conditions: Chateado, Assustado, Exausto, Machucado. Each gives -1 die. Broken auto-fails until valid recovery.
Anchor recovery clears Conditions in a safe close scene. Friction may be resolved only in a safe close group scene; if Trouble occurs, no Conditions are healed.
Extended Trouble: Normal=3x Teens, Extremely Difficult=4x, Almost Impossible=5x, one scene/roll per Teen. Rolled successes count toward Threat and may also buy Skill effects. A Lead preparation scene creates its leadership pool instead of contributing those successes to Threat.
XP: five Flood questions, each Yes=1 XP; 5 XP raises one Skill, max5; max10 stored. Spend XP only outside active play.
Change: Problem, Shame, Iconic Item and Relationships may change. Conditions clear between Mysteries. If enough time passed age may rise by one; at 20 the Teen leaves play.
Never import Tales Luck, Pride, Kid age/attribute-total rules, or Tales 2x/3x/4x Extended Trouble targets.
""".trimIndent()
}
