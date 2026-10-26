package com.braseiro.talesvtt.game

/** Official Tales/Kids supplements supported as local mechanics/content-data.
 *  The LLM/RAG may reference a ruleId but never supplies authoritative numbers.
 */
enum class OfficialSupplement(val label: String) {
  NONE("Tales Base"),
  THEY_GROW_UP_SO_FAST("They Grow Up So Fast"),
  OUR_FRIENDS_THE_MACHINES("Nossas Amigas as Máquinas")
}

enum class TgufAlienStage { NONE, EGG, HATCHLING, DEVELOPING, MATURE }

data class HometownHackState(
  val city: String = "",
  val area: String = "",
  val mapReference: String = "",
  val loopLayout: String = "",
  val agency: String = "",
  val institutions: List<String> = emptyList(),
  val summary: String = ""
)

data class SupplementCampaignState(
  val active: OfficialSupplement = OfficialSupplement.NONE,
  val mysteryId: String = "",
  val tgufStage: TgufAlienStage = TgufAlienStage.NONE,
  val imprintedKids: Set<String> = emptySet(),
  val oncePerMysteryUsed: Set<String> = emptySet(),
  val tgufTrustPenalty: Int = 0,
  val tgufExtendedEscalationSteps: Int = 0,
  val countdowns: Map<String, Int> = emptyMap(),
  val chippedCharacters: Set<String> = emptySet(),
  val disabledChips: Set<String> = emptySet(),
  val savedBonusDice: Map<String, Int> = emptyMap(),
  val permanentBonusDice: Map<String, Int> = emptyMap(),
  val hometownHack: HometownHackState = HometownHackState()
) {
  fun startMystery(id: String): SupplementCampaignState = copy(
    mysteryId = id,
    oncePerMysteryUsed = emptySet()
  )
}

data class SupplementRuleDefinition(
  val id: String,
  val supplement: OfficialSupplement,
  val requiredSuccesses: Int? = null,
  val bonusDice: Int = 0,
  val automaticSuccesses: Int = 0,
  val extendedMultiplier: Int? = null,
  val allowedSkills: Set<String> = emptySet(),
  val oncePerMystery: Boolean = false,
  val description: String
)

object OfficialSupplementRules {
  // IDs are stable runtime contracts. They deliberately encode only mechanics present in official sources.
  const val TGUF_BOND_HEAL = "TGUF_BOND_HEAL"
  const val TGUF_DRAIN_DEVICE = "TGUF_DRAIN_DEVICE"
  const val TGUF_STATIC_SHOCK = "TGUF_STATIC_SHOCK"
  const val TGUF_REACTOR_MOVE_2 = "TGUF_REACTOR_MOVE_2"
  const val TGUF_REACTOR_CHARM_2 = "TGUF_REACTOR_CHARM_2"
  const val TGUF_PARENT_LOOP_CHARM_BONUS_2 = "TGUF_PARENT_LOOP_CHARM_BONUS_2"
  const val TGUF_LAB_SNEAK_2 = "TGUF_LAB_SNEAK_2"
  const val TGUF_TUNNEL_SNEAK_3 = "TGUF_TUNNEL_SNEAK_3"
  const val TGUF_SECURITY_SNEAK_CHARM_2 = "TGUF_SECURITY_SNEAK_CHARM_2"
  const val TGUF_NEODYMIUM_FORCE_4 = "TGUF_NEODYMIUM_FORCE_4"
  const val TGUF_FREE_TO_CHOOSE_BONUS_1 = "TGUF_FREE_TO_CHOOSE_BONUS_1"
  const val TGUF_REGAIN_TRUST_CHARM_2 = "TGUF_REGAIN_TRUST_CHARM_2"
  const val TGUF_ESCAPE_EXTENDED_2X = "TGUF_ESCAPE_EXTENDED_2X"
  const val TGUF_BULLIES_EXTENDED_2X = "TGUF_BULLIES_EXTENDED_2X"
  const val TGUF_DANIEL_EXTENDED_2X = "TGUF_DANIEL_EXTENDED_2X"
  const val TGUF_TRAVEL_EXTENDED_2X = "TGUF_TRAVEL_EXTENDED_2X"
  const val TGUF_OTHER_KIDS_EXTENDED_3X = "TGUF_OTHER_KIDS_EXTENDED_3X"
  const val TGUF_AGENTS_EXTENDED_2X = "TGUF_AGENTS_EXTENDED_2X"
  const val TGUF_RIFT_EXTENDED_2X = "TGUF_RIFT_EXTENDED_2X"
  const val TGUF_RIFT_NO_DANIEL_EXTENDED_3X = "TGUF_RIFT_NO_DANIEL_EXTENDED_3X"
  const val TGUF_RIFT_REMINDER_AUTO_2 = "TGUF_RIFT_REMINDER_AUTO_2"
  const val TGUF_APPEAL_EXTENDED_3X = "TGUF_APPEAL_EXTENDED_3X"
  const val TGUF_FORCE_REGIS_EXTENDED_3X = "TGUF_FORCE_REGIS_EXTENDED_3X"

  const val OFM_HACK_TOY_CHIP_2 = "OFM_HACK_TOY_CHIP_2"
  const val OFM_LARGE_MACHINE_GROUP_3 = "OFM_LARGE_MACHINE_GROUP_3"
  const val OFM_MACHINE_HACKERS_PER_CHIP_10 = "OFM_MACHINE_HACKERS_PER_CHIP_10"
  const val OFM_CHIP_REACTIVATE_D6_6 = "OFM_CHIP_REACTIVATE_D6_6"
  const val OFM_DESTRUCTION_EXTENDED_4X = "OFM_DESTRUCTION_EXTENDED_4X"
  const val OFM_DESTRUCTION_WITH_B_EXTENDED_3X = "OFM_DESTRUCTION_WITH_B_EXTENDED_3X"
  const val OFM_CAPTURE_EXTENDED_2X = "OFM_CAPTURE_EXTENDED_2X"
  const val OFM_PATROL_PATTERN_BONUS_1 = "OFM_PATROL_PATTERN_BONUS_1"
  const val OFM_MAYHEM_EXTENDED_3X = "OFM_MAYHEM_EXTENDED_3X"
  const val OFM_MAYHEM_ALLIES_EXTENDED_2X = "OFM_MAYHEM_ALLIES_EXTENDED_2X"
  const val OFM_MUMMY_STAIRS_2 = "OFM_MUMMY_STAIRS_2"
  const val OFM_DIM9_EXTENDED_3X = "OFM_DIM9_EXTENDED_3X"
  const val OFM_DIM9_ALLIES_EXTENDED_2X = "OFM_DIM9_ALLIES_EXTENDED_2X"
  const val OFM_NPC_SPECIAL_2 = "OFM_NPC_SPECIAL_2"
  const val OFM_COMMUNICATOR_PERMANENT_CAP_3 = "OFM_COMMUNICATOR_PERMANENT_CAP_3"

  private fun skills(vararg names: String): Set<String> = names.mapNotNull(TalesSkillCatalog::canonicalSkill).toSet()

  val definitions: Map<String, SupplementRuleDefinition> = listOf(
    SupplementRuleDefinition(TGUF_BOND_HEAL, OfficialSupplement.THEY_GROW_UP_SO_FAST, oncePerMystery = true, description = "Kid imprinted remove uma Condition por Mistério ao narrar a conexão."),
    SupplementRuleDefinition(TGUF_DRAIN_DEVICE, OfficialSupplement.THEY_GROW_UP_SO_FAST, oncePerMystery = true, description = "Kid imprinted drena um aparelho eletrônico portátil por Mistério."),
    SupplementRuleDefinition(TGUF_STATIC_SHOCK, OfficialSupplement.THEY_GROW_UP_SO_FAST, bonusDice = 2, oncePerMystery = true, description = "Choque estático do vínculo: +2 dados em uma rolagem apropriada, uma vez por Mistério."),
    SupplementRuleDefinition(TGUF_REACTOR_MOVE_2, OfficialSupplement.THEY_GROW_UP_SO_FAST, requiredSuccesses = 2, allowedSkills = skills("Movimentação"), description = "Entrada no reator sem se ferir/ser pego."),
    SupplementRuleDefinition(TGUF_REACTOR_CHARM_2, OfficialSupplement.THEY_GROW_UP_SO_FAST, requiredSuccesses = 2, allowedSkills = skills("Charme"), description = "Convencer segurança a permitir entrada."),
    SupplementRuleDefinition(TGUF_PARENT_LOOP_CHARM_BONUS_2, OfficialSupplement.THEY_GROW_UP_SO_FAST, bonusDice = 2, allowedSkills = skills("Charme"), description = "Citar pai/mãe que trabalha no Loop concede +2 dados no Charme da entrada."),
    SupplementRuleDefinition(TGUF_LAB_SNEAK_2, OfficialSupplement.THEY_GROW_UP_SO_FAST, requiredSuccesses = 2, allowedSkills = skills("Furtividade"), description = "Furtividade pela instalação/laboratórios."),
    SupplementRuleDefinition(TGUF_TUNNEL_SNEAK_3, OfficialSupplement.THEY_GROW_UP_SO_FAST, requiredSuccesses = 3, allowedSkills = skills("Furtividade"), description = "Entrar furtivamente nos túneis de serviço."),
    SupplementRuleDefinition(TGUF_SECURITY_SNEAK_CHARM_2, OfficialSupplement.THEY_GROW_UP_SO_FAST, requiredSuccesses = 2, allowedSkills = skills("Furtividade", "Charme"), description = "Segurança reforçada no Loop em estação posterior."),
    SupplementRuleDefinition(TGUF_NEODYMIUM_FORCE_4, OfficialSupplement.THEY_GROW_UP_SO_FAST, requiredSuccesses = 4, allowedSkills = skills("Força"), description = "Mover disco de neodímio grande exige 4 sucessos em Força."),
    SupplementRuleDefinition(TGUF_FREE_TO_CHOOSE_BONUS_1, OfficialSupplement.THEY_GROW_UP_SO_FAST, bonusDice = 1, description = "Uso direto do cargueiro Free to Choose em ações contra as outras crianças: +1 dado."),
    SupplementRuleDefinition(TGUF_REGAIN_TRUST_CHARM_2, OfficialSupplement.THEY_GROW_UP_SO_FAST, requiredSuccesses = 2, allowedSkills = skills("Charme"), description = "Recuperar a confiança da criatura no Mistério seguinte."),
    SupplementRuleDefinition(TGUF_ESCAPE_EXTENDED_2X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 2, description = "Fuga dos túneis/ReGIS: Problema Estendido normal 2×Kids."),
    SupplementRuleDefinition(TGUF_BULLIES_EXTENDED_2X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 2, description = "Lidar com valentões: Problema Estendido normal 2×Kids."),
    SupplementRuleDefinition(TGUF_DANIEL_EXTENDED_2X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 2, description = "Escapar/lidar com Daniel: Problema Estendido normal 2×Kids."),
    SupplementRuleDefinition(TGUF_TRAVEL_EXTENDED_2X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 2, description = "Mover a criatura sem ser notado: Problema Estendido normal 2×Kids."),
    SupplementRuleDefinition(TGUF_OTHER_KIDS_EXTENDED_3X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 3, description = "Remover o outro grupo da península: Problema Estendido difícil 3×Kids."),
    SupplementRuleDefinition(TGUF_AGENTS_EXTENDED_2X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 2, description = "Fazer agentes deixarem o local: Problema Estendido normal 2×Kids."),
    SupplementRuleDefinition(TGUF_RIFT_EXTENDED_2X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 2, description = "Operar equipamento para abrir o rift com Daniel presente: Problema Estendido 2×Kids."),
    SupplementRuleDefinition(TGUF_RIFT_NO_DANIEL_EXTENDED_3X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 3, description = "Operar equipamento para abrir o rift sem Daniel presente: Problema Estendido 3×Kids."),
    SupplementRuleDefinition(TGUF_RIFT_REMINDER_AUTO_2, OfficialSupplement.THEY_GROW_UP_SO_FAST, automaticSuccesses = 2, oncePerMystery = true, description = "Lembrar Daniel das flutuações de energia: 2 sucessos automáticos, sem rolagem."),
    SupplementRuleDefinition(TGUF_APPEAL_EXTENDED_3X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 3, description = "Apelar à simpatia de ReGIS: Problema Estendido 3×Kids."),
    SupplementRuleDefinition(TGUF_FORCE_REGIS_EXTENDED_3X, OfficialSupplement.THEY_GROW_UP_SO_FAST, extendedMultiplier = 3, description = "Barricar/forçar ReGIS para fora: Problema Estendido 3×Kids."),

    SupplementRuleDefinition(OFM_HACK_TOY_CHIP_2, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, requiredSuccesses = 2, allowedSkills = skills("Programação"), description = "Hackear chip de um brinquedo individual: 2 sucessos."),
    SupplementRuleDefinition(OFM_LARGE_MACHINE_GROUP_3, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, requiredSuccesses = 3, description = "Grupo grande (10+) de Máquinas é problema de 3 sucessos."),
    SupplementRuleDefinition(OFM_DESTRUCTION_EXTENDED_4X, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, extendedMultiplier = 4, description = "Destruição da fábrica sem ajuda de B: 4×Kids."),
    SupplementRuleDefinition(OFM_DESTRUCTION_WITH_B_EXTENDED_3X, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, extendedMultiplier = 3, description = "Destruição da fábrica com ajuda de B: 3×Kids."),
    SupplementRuleDefinition(OFM_CAPTURE_EXTENDED_2X, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, extendedMultiplier = 2, description = "Plano de A para capturar/chipar: ameaça normal 2×Kids."),
    SupplementRuleDefinition(OFM_PATROL_PATTERN_BONUS_1, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, bonusDice = 1, allowedSkills = skills("Furtividade", "Movimentação"), description = "Padrão de patrulha descoberto concede +1 ao teste subsequente permitido."),
    SupplementRuleDefinition(OFM_MAYHEM_EXTENDED_3X, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, extendedMultiplier = 3, description = "Clímax de Horror Movie Mayhem sem ajuda: 3×Kids."),
    SupplementRuleDefinition(OFM_MAYHEM_ALLIES_EXTENDED_2X, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, extendedMultiplier = 2, description = "Com ajuda externa adequada: ameaça reduzida a 2×Kids."),
    SupplementRuleDefinition(OFM_MUMMY_STAIRS_2, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, requiredSuccesses = 2, allowedSkills = skills("Conhecimento", "Investigação"), description = "Descobrir a identidade/local da múmia: 2 sucessos."),
    SupplementRuleDefinition(OFM_DIM9_EXTENDED_3X, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, extendedMultiplier = 3, description = "Clímax DIM-9 sem ajuda: 3×Kids."),
    SupplementRuleDefinition(OFM_DIM9_ALLIES_EXTENDED_2X, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, extendedMultiplier = 2, description = "Clímax DIM-9 com a ajuda prevista pela aventura: reduz para Normal, 2×Kids."),
    SupplementRuleDefinition(OFM_NPC_SPECIAL_2, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, requiredSuccesses = 2, description = "Atributo Especial 2 de NPC: requer 2 sucessos quando diretamente aplicável."),
    SupplementRuleDefinition(OFM_COMMUNICATOR_PERMANENT_CAP_3, OfficialSupplement.OUR_FRIENDS_THE_MACHINES, description = "Sucessos adicionais no comunicador podem virar bônus permanente até +3.")
  ).associateBy { it.id }

  fun rule(id: String): SupplementRuleDefinition? = definitions[id]

  fun isRuleActive(state: SupplementCampaignState, ruleId: String): Boolean =
    rule(ruleId)?.supplement == state.active

  fun canUseOncePerMystery(state: SupplementCampaignState, ruleId: String): Boolean {
    val r = rule(ruleId) ?: return false
    return !r.oncePerMystery || ruleId !in state.oncePerMysteryUsed
  }

  fun markUsed(state: SupplementCampaignState, ruleId: String): SupplementCampaignState =
    if (rule(ruleId)?.oncePerMystery == true) state.copy(oncePerMysteryUsed = state.oncePerMysteryUsed + ruleId) else state

  fun requiredSuccesses(state: SupplementCampaignState, ruleId: String, skill: String?): Int? {
    val r = rule(ruleId) ?: return null
    if (r.supplement != state.active) return null
    val canonical = skill?.let(TalesSkillCatalog::canonicalSkill)
    if (r.allowedSkills.isNotEmpty() && canonical !in r.allowedSkills) return null
    val base = r.requiredSuccesses ?: return null
    return if (state.active == OfficialSupplement.THEY_GROW_UP_SO_FAST && state.tgufTrustPenalty > 0 && canonical == "Charme") {
      base + state.tgufTrustPenalty
    } else base
  }

  fun bonusDice(state: SupplementCampaignState, ruleId: String, skill: String?): Int? {
    val r = rule(ruleId) ?: return null
    if (r.supplement != state.active || !canUseOncePerMystery(state, ruleId)) return null
    val canonical = skill?.let(TalesSkillCatalog::canonicalSkill)
    if (r.allowedSkills.isNotEmpty() && canonical !in r.allowedSkills) return null
    return r.bonusDice.takeIf { it > 0 }
  }

  fun automaticSuccesses(state: SupplementCampaignState, ruleId: String): Int? {
    val r = rule(ruleId) ?: return null
    if (r.supplement != state.active || !canUseOncePerMystery(state, ruleId)) return null
    return r.automaticSuccesses.takeIf { it > 0 }
  }

  fun extendedThreatLevel(state: SupplementCampaignState, ruleId: String): ThreatLevel? {
    val r = rule(ruleId) ?: return null
    if (r.supplement != state.active) return null
    var mult = r.extendedMultiplier ?: return null
    if (state.active == OfficialSupplement.THEY_GROW_UP_SO_FAST && state.tgufExtendedEscalationSteps > 0) {
      mult = (mult + state.tgufExtendedEscalationSteps).coerceAtMost(4)
    }
    return when (mult) { 2 -> ThreatLevel.NORMAL; 3 -> ThreatLevel.DIFFICULT; 4 -> ThreatLevel.ALMOST_IMPOSSIBLE; else -> null }
  }

  fun chipReactivates(nearDeceiverCount: Int, d6: Int): Boolean = nearDeceiverCount >= 10 && d6 == 6
  fun machinesNeededToHackChips(chips: Int): Int = chips.coerceAtLeast(0) * 10
  fun capPermanentBonus(value: Int): Int = value.coerceIn(0, 3)
}
