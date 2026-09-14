package com.braseiro.talesvtt.game

/**
 * Things from the Flood / Teens mechanical core.
 *
 * Mechanical firewall: this file is Flood-only. It intentionally contains no
 * Tales/Kids Luck or Pride state. Source authority is the project canonical
 * ledger (FLD-* rules, baseline TFTL-TFTF-GRCS-003 candidate catalogue).
 */

enum class FloodCondition(val label: String) {
  UPSET("Upset"),
  SCARED("Scared"),
  EXHAUSTED("Exhausted"),
  INJURED("Injured")
}

object FloodSkillCatalog {
  val skillToAttribute: Map<String, String> = linkedMapOf(
    "Sneak" to "BODY",
    "Force" to "BODY",
    "Move" to "BODY",
    "Tinker" to "TECH",
    "Program" to "TECH",
    "Calculate" to "TECH",
    "Contact" to "HEART",
    "Charm" to "HEART",
    "Lead" to "HEART",
    "Investigate" to "MIND",
    "Comprehend" to "MIND",
    "Empathize" to "MIND"
  )

  fun canonicalSkill(name: String): String? =
    skillToAttribute.keys.firstOrNull { it.equals(name.trim(), ignoreCase = true) }

  fun attributeFor(skill: String): String? = canonicalSkill(skill)?.let(skillToAttribute::get)
}

data class FloodTypeDefinition(
  val id: String,
  val label: String,
  val keySkills: Set<String>
)

object FloodTypeCatalog {
  val types: List<FloodTypeDefinition> = listOf(
    FloodTypeDefinition("hacker", "Hacker", setOf("Program", "Calculate", "Comprehend")),
    FloodTypeDefinition("jock", "Jock", setOf("Force", "Move", "Lead")),
    FloodTypeDefinition("lone_wolf", "Lone Wolf", setOf("Sneak", "Comprehend", "Empathize")),
    FloodTypeDefinition("motorhead", "Motorhead", setOf("Tinker", "Calculate", "Charm")),
    FloodTypeDefinition("party_animal", "Party Animal", setOf("Move", "Contact", "Lead")),
    FloodTypeDefinition("raver", "Raver", setOf("Tinker", "Contact", "Charm")),
    FloodTypeDefinition("rocker", "Rocker", setOf("Move", "Empathize", "Investigate")),
    FloodTypeDefinition("seeker", "Seeker", setOf("Investigate", "Empathize", "Lead")),
    FloodTypeDefinition("snob", "Snob", setOf("Charm", "Contact", "Comprehend")),
    FloodTypeDefinition("street_kid", "Street Kid", setOf("Force", "Sneak", "Investigate"))
  )

  fun resolve(idOrLabel: String): FloodTypeDefinition? = types.firstOrNull {
    it.id.equals(idOrLabel.trim(), ignoreCase = true) || it.label.equals(idOrLabel.trim(), ignoreCase = true)
  }
}

data class FloodRelationship(
  val name: String,
  val description: String
)

data class FloodShameState(
  val text: String,
  val lastUsedMystery: String? = null,
  val lastUsedSession: String? = null
) {
  fun isAvailable(mysteryId: String, sessionId: String, multiSessionMystery: Boolean): Boolean =
    if (multiSessionMystery) lastUsedSession != sessionId else lastUsedMystery != mysteryId
}

data class FloodScar(
  val id: String,
  val text: String,
  val accepted: Boolean = false,
  val deathRoll: Int? = null,
  val decisive: Boolean = false
)

data class FloodTeenState(
  val name: String = "Sara",
  val age: Int = 17,
  val typeId: String = "hacker",
  val portraitAssetKey: String = "flood_type_hacker",
  val portraitImageBase64: String? = null,
  val description: String = "Teen Flood",
  val problem: String = "Minha família acha que estou jogando meu futuro fora.",
  val drive: String = "Descobrir o que está usando a velha tecnologia do Loop.",
  val shame: FloodShameState = FloodShameState("Eu abandonei alguém quando mais precisava de mim."),
  val anchorName: String = "Eva, minha tia",
  val iconicItemName: String = "Modem portátil",
  val relationships: List<FloodRelationship> = emptyList(),
  val hooks: List<String> = emptyList(),
  val conditions: Set<FloodCondition> = emptySet(),
  val broken: Boolean = false,
  val scars: List<FloodScar> = emptyList(),
  val removed: Boolean = false,
  val removalPending: Boolean = false,
  val xp: Int = 0,
  val attributes: Map<String, Int> = linkedMapOf(
    "BODY" to 3,
    "TECH" to 5,
    "HEART" to 3,
    "MIND" to 3
  ),
  val skills: Map<String, Int> = linkedMapOf(
    "Sneak" to 0,
    "Force" to 0,
    "Move" to 0,
    "Tinker" to 1,
    "Program" to 3,
    "Calculate" to 3,
    "Contact" to 0,
    "Charm" to 0,
    "Lead" to 0,
    "Investigate" to 1,
    "Comprehend" to 2,
    "Empathize" to 0
  )
) {
  val conditionPenalty: Int get() = conditions.size

  fun creationErrors(): List<String> {
    val errors = mutableListOf<String>()
    if (name.isBlank()) errors += "Nome obrigatório."
    if (age !in 14..19) errors += "Idade Flood deve estar entre 14 e 19."
    val type = FloodTypeCatalog.resolve(typeId)
    if (type == null) errors += "Tipo Flood inválido."

    val requiredAttributes = setOf("BODY", "TECH", "HEART", "MIND")
    if (attributes.keys.map { it.uppercase() }.toSet() != requiredAttributes) {
      errors += "Atributos Flood incompletos."
    } else {
      if (attributes.values.any { it !in 1..5 }) errors += "Atributos Flood devem ficar entre 1 e 5."
      if (attributes.values.sum() != 14) errors += "Criação Flood exige exatamente 14 pontos de Atributos."
    }

    val canonicalSkills = FloodSkillCatalog.skillToAttribute.keys
    if (skills.keys.toSet() != canonicalSkills.toSet()) {
      errors += "Perícias Flood incompletas."
    } else {
      if (skills.values.any { it < 0 }) errors += "Perícia Flood não pode ser negativa."
      if (skills.values.sum() != 10) errors += "Criação Flood exige exatamente 10 pontos de Perícias."
      if (type != null) {
        skills.forEach { (skill, value) ->
          val max = if (skill in type.keySkills) 3 else 1
          if (value > max) errors += "$skill excede o limite inicial $max para ${type.label}."
        }
      }
    }

    if (problem.isBlank()) errors += "Problem obrigatório."
    if (drive.isBlank()) errors += "Drive obrigatório."
    if (shame.text.isBlank()) errors += "Shame obrigatório."
    if (anchorName.isBlank()) errors += "Anchor obrigatório."
    if (iconicItemName.isBlank()) errors += "Iconic Item obrigatório."
    return errors
  }

  fun runtimeErrors(): List<String> {
    val errors = mutableListOf<String>()
    if (age !in 14..19) errors += "Idade Flood inválida."
    if (attributes.values.any { it !in 1..5 } || attributes.values.sum() != 14) errors += "Atributos Flood inválidos."
    if (skills.keys.toSet() != FloodSkillCatalog.skillToAttribute.keys.toSet()) errors += "Perícias Flood incompletas."
    if (skills.values.any { it !in 0..5 }) errors += "Perícia Flood fora do limite 0..5."
    if (xp !in 0..10) errors += "XP Flood deve permanecer entre 0 e 10."
    if (scars.size >= 6 && !removed && !removalPending) errors += "Sexta Scar exige remoção."
    return errors
  }

  fun testForSkill(reason: String, skill: String, requiredSuccesses: Int): FloodTestRequest? {
    val canonical = FloodSkillCatalog.canonicalSkill(skill) ?: return null
    val attribute = FloodSkillCatalog.attributeFor(canonical) ?: return null
    val attributeValue = attributes[attribute] ?: return null
    val skillValue = skills[canonical] ?: return null
    return FloodTestRequest(
      reason = reason,
      attributeName = attribute,
      attributeValue = attributeValue,
      skillName = canonical,
      skillValue = skillValue,
      requiredSuccesses = requiredSuccesses
    )
  }
}

data class FloodGroupState(
  val friction: String = "Nós nos culpamos por ter voltado à instalação."
)

data class FloodSceneChoice(
  val title: String,
  val description: String
)

data class FloodSceneState(
  val title: String = "Depois da Chuva",
  val chapter: String = "MYSTERY",
  val seasonYear: String = "1990s",
  val location: String = "Posto de Serviço",
  val weather: String = "Chuva forte",
  val time: String = "23:17",
  val daylight: String = "Noite",
  val visualAssetKey: String = "scene_flooded_tunnel",
  val importedImageUri: String? = null,
  val narrative: List<String> = emptyList(),
  val choices: List<FloodSceneChoice> = emptyList()
)

data class FloodRevealedState(
  val clues: Set<String> = emptySet(),
  val locations: Set<String> = setOf("Posto de Serviço"),
  val connections: Set<Pair<String, String>> = emptySet()
)

data class FloodTestRequest(
  val reason: String,
  val attributeName: String,
  val attributeValue: Int,
  val skillName: String? = null,
  val skillValue: Int = 0,
  val requiredSuccesses: Int = 1,
  val approvedItemBonus: Int = 0,
  val helperTeen: String? = null,
  val extendedSceneIndex: Int? = null
) {
  fun basePool(): Int = attributeValue + if (skillName == null) 0 else skillValue
}

data class FloodTestResolution(
  val dice: List<Int>,
  val successes: Int,
  val pushed: Boolean = false,
  val shameUsed: Boolean = false,
  val scarUsedId: String? = null,
  val reportedToGm: Boolean = false
)

enum class FloodThreatLevel(val multiplier: Int, val label: String) {
  NORMAL(3, "Normal"),
  EXTREMELY_DIFFICULT(4, "Extremely Difficult"),
  ALMOST_IMPOSSIBLE(5, "Almost Impossible")
}

data class FloodExtendedTeenScene(
  val teen: String,
  val skill: String,
  val successes: Int? = null
)

enum class FloodExtendedOutcome {
  UNRESOLVED,
  OBJECTIVE_COMPLETE,
  COMPROMISE_AVAILABLE,
  COMPLETE_FAILURE,
  EXACT_HALF_REQUIRES_GM
}

data class FloodExtendedTroubleState(
  val active: Boolean = false,
  val stakes: String = "",
  val teens: Int = 4,
  val threatLevel: FloodThreatLevel = FloodThreatLevel.NORMAL,
  val scenes: List<FloodExtendedTeenScene> = emptyList(),
  val acceptedExtraConditions: Int = 0
) {
  val target: Int get() = teens * threatLevel.multiplier
  val allScenesResolved: Boolean get() = scenes.size == teens && scenes.all { it.successes != null }
  val sceneSuccesses: Int get() = scenes.sumOf { it.successes ?: 0 }
  val totalSuccesses: Int get() = sceneSuccesses + acceptedExtraConditions
  val nextUnresolvedIndex: Int? get() = scenes.indexOfFirst { it.successes == null }.takeIf { it >= 0 }

  val outcome: FloodExtendedOutcome
    get() {
      if (!allScenesResolved) return FloodExtendedOutcome.UNRESOLVED
      if (totalSuccesses >= target) return FloodExtendedOutcome.OBJECTIVE_COMPLETE
      val doubled = totalSuccesses * 2
      return when {
        doubled < target -> FloodExtendedOutcome.COMPLETE_FAILURE
        doubled > target -> FloodExtendedOutcome.COMPROMISE_AVAILABLE
        else -> FloodExtendedOutcome.EXACT_HALF_REQUIRES_GM
      }
    }
}

enum class FloodBreakSource { TROUBLE, PUSH, EXTENDED_TROUBLE }

data class FloodState(
  val teen: FloodTeenState = FloodTeenState(),
  val group: FloodGroupState = FloodGroupState(),
  val scene: FloodSceneState = FloodSceneState(),
  val revealed: FloodRevealedState = FloodRevealedState(),
  val lastPlayerAction: String? = null,
  val activeTest: FloodTestRequest? = null,
  val testResolution: FloodTestResolution? = null,
  val extended: FloodExtendedTroubleState = FloodExtendedTroubleState(),
  val lastError: String? = null
) {
  fun finalPool(): Int {
    val test = activeTest ?: return 0
    if (teen.broken || teen.removed) return 0
    val raw = test.basePool() + test.approvedItemBonus + (if (test.helperTeen != null) 1 else 0) - teen.conditionPenalty
    return raw.coerceAtLeast(1)
  }
}

sealed interface FloodEvent {
  data class PlayerActionDeclared(val text: String) : FloodEvent
  data class GmNarration(val text: String, val choices: List<FloodSceneChoice> = emptyList()) : FloodEvent
  data class GmWorldPatch(
    val location: String? = null,
    val weather: String? = null,
    val time: String? = null,
    val daylight: String? = null,
    val visualAssetKey: String? = null
  ) : FloodEvent
  data class SetSceneImportedImage(val uri: String?) : FloodEvent
  data class RevealClue(val id: String) : FloodEvent
  data class RevealLocation(val id: String) : FloodEvent
  data class RevealConnection(val from: String, val to: String) : FloodEvent
  data class GmRequestTest(val request: FloodTestRequest) : FloodEvent
  data object ClearTest : FloodEvent
  data class ApplyHelp(val helperTeen: String) : FloodEvent
  data class ApplyApprovedItem(val bonus: Int) : FloodEvent
  data class RollResult(val dice: List<Int>) : FloodEvent
  data class SetTestResultReported(val reported: Boolean) : FloodEvent
  data class UseShame(val mysteryId: String, val sessionId: String, val multiSessionMystery: Boolean) : FloodEvent
  data class UseScar(val scarId: String) : FloodEvent
  data class PushRoll(
    val condition: FloodCondition?,
    val rerolledNonSixDice: List<Int>,
    val scarTextIfBroken: String = "",
    val deathRollIfRequired: Int? = null
  ) : FloodEvent
  data class ApplyCondition(
    val condition: FloodCondition?,
    val source: FloodBreakSource = FloodBreakSource.TROUBLE,
    val scarTextIfBroken: String = "",
    val deathRollIfRequired: Int? = null
  ) : FloodEvent
  data class BecomeBroken(
    val source: FloodBreakSource,
    val scarText: String,
    val deathRollIfRequired: Int? = null
  ) : FloodEvent
  data object RecoverWithAnchor : FloodEvent
  data object RecoverWithFriction : FloodEvent
  data class LeadRecoverySuccess(val condition: FloodCondition) : FloodEvent
  data class LeadRecoveryFailure(val condition: FloodCondition) : FloodEvent
  data object RecoverBetweenMysteries : FloodEvent
  data class HealAcceptedScar(val scarId: String) : FloodEvent
  data class StartExtendedTrouble(
    val stakes: String,
    val teens: Int,
    val threatLevel: FloodThreatLevel,
    val scenes: List<FloodExtendedTeenScene>
  ) : FloodEvent
  data class ResolveExtendedTeenScene(val index: Int, val successes: Int) : FloodEvent
  data class AcceptExtendedCompromise(val extraConditions: Int) : FloodEvent
  data object ResolveExtendedCompleteFailure : FloodEvent
  data class AwardXp(val amount: Int) : FloodEvent
  data class ImproveSkill(val skill: String) : FloodEvent
}

object FloodReducer {
  private fun addScar(
    teen: FloodTeenState,
    source: FloodBreakSource,
    scarText: String,
    deathRollIfRequired: Int?
  ): Pair<FloodTeenState, String?> {
    if (scarText.isBlank()) return teen to "Nova Scar exige descrição."
    val nextCount = teen.scars.size + 1
    val requiresRoll = nextCount > 1 && nextCount < 6
    if (requiresRoll && deathRollIfRequired !in 1..6) return teen to "Nova Scar exige death roll d6 válido."
    if (!requiresRoll && deathRollIfRequired != null && deathRollIfRequired !in 1..6) return teen to "Death roll inválido."

    val failed = when {
      nextCount >= 6 -> true
      nextCount > 1 -> (deathRollIfRequired ?: 0) < nextCount
      else -> false
    }
    val immediate = failed && source == FloodBreakSource.EXTENDED_TROUBLE
    val scar = FloodScar(
      id = "scar-$nextCount",
      text = scarText.trim(),
      accepted = false,
      deathRoll = if (nextCount > 1 && nextCount < 6) deathRollIfRequired else if (nextCount >= 6) 0 else null,
      decisive = failed
    )
    return teen.copy(
      broken = true,
      scars = teen.scars + scar,
      removed = teen.removed || immediate,
      removalPending = teen.removalPending || (failed && !immediate)
    ) to null
  }

  fun reduce(state: FloodState, event: FloodEvent): FloodState {
    fun fail(message: String) = state.copy(lastError = message)
    fun ok(next: FloodState) = next.copy(lastError = null)

    return when (event) {
      is FloodEvent.PlayerActionDeclared -> {
        if (event.text.isBlank()) fail("A ação precisa ser descrita.")
        else ok(state.copy(lastPlayerAction = event.text.trim(), activeTest = null, testResolution = null))
      }

      is FloodEvent.GmNarration -> {
        if (event.text.isBlank()) fail("Narração do Mestre vazia.")
        else ok(state.copy(scene = state.scene.copy(
          narrative = listOf(event.text.trim()),
          choices = event.choices.filter { it.title.isNotBlank() && it.description.isNotBlank() }.take(3)
        )))
      }

      is FloodEvent.GmWorldPatch -> {
        val nextLocation = event.location?.takeIf { it.isNotBlank() } ?: state.scene.location
        val nextVisual = event.visualAssetKey?.takeIf { it.isNotBlank() } ?: state.scene.visualAssetKey
        val changedScene = nextLocation != state.scene.location || nextVisual != state.scene.visualAssetKey
        ok(state.copy(scene = state.scene.copy(
          location = nextLocation,
          weather = event.weather?.takeIf { it.isNotBlank() } ?: state.scene.weather,
          time = event.time?.takeIf { it.isNotBlank() } ?: state.scene.time,
          daylight = event.daylight?.takeIf { it.isNotBlank() } ?: state.scene.daylight,
          visualAssetKey = nextVisual,
          importedImageUri = if (changedScene) null else state.scene.importedImageUri
        )))
      }

      is FloodEvent.SetSceneImportedImage -> {
        if (event.uri != null && event.uri.isBlank()) fail("URI de imagem vazia.")
        else ok(state.copy(scene = state.scene.copy(importedImageUri = event.uri)))
      }

      is FloodEvent.RevealClue -> {
        if (event.id.isBlank()) fail("Pista inválida.")
        else ok(state.copy(revealed = state.revealed.copy(clues = state.revealed.clues + event.id)))
      }

      is FloodEvent.RevealLocation -> {
        if (event.id.isBlank()) fail("Local inválido.")
        else ok(state.copy(revealed = state.revealed.copy(locations = state.revealed.locations + event.id)))
      }

      is FloodEvent.RevealConnection -> {
        if (event.from !in state.revealed.clues && event.from !in state.revealed.locations) fail("Origem da conexão ainda não foi revelada.")
        else if (event.to !in state.revealed.clues && event.to !in state.revealed.locations) fail("Destino da conexão ainda não foi revelado.")
        else ok(state.copy(revealed = state.revealed.copy(connections = state.revealed.connections + (event.from to event.to))))
      }

      is FloodEvent.GmRequestTest -> {
        val r = event.request
        if (r.attributeValue < 0 || r.skillValue < 0 || r.requiredSuccesses !in 1..3) fail("Teste Flood inválido.")
        else ok(state.copy(activeTest = r, testResolution = null))
      }

      FloodEvent.ClearTest -> ok(state.copy(activeTest = null, testResolution = null))

      is FloodEvent.ApplyHelp -> {
        val test = state.activeTest ?: return fail("Não há teste ativo.")
        if (state.testResolution != null) fail("Ajuda deve ser aplicada antes da rolagem.")
        else if (test.helperTeen != null) fail("Só um Teen pode ajudar.")
        else if (event.helperTeen.isBlank() || event.helperTeen.equals(state.teen.name, ignoreCase = true)) fail("Ajudante inválido.")
        else ok(state.copy(activeTest = test.copy(helperTeen = event.helperTeen)))
      }

      is FloodEvent.ApplyApprovedItem -> {
        val test = state.activeTest ?: return fail("Não há teste ativo.")
        if (state.testResolution != null) fail("Item deve ser aprovado antes da rolagem.")
        else if (event.bonus !in 0..3) fail("Bônus Flood de item inválido.")
        else ok(state.copy(activeTest = test.copy(approvedItemBonus = event.bonus)))
      }

      is FloodEvent.RollResult -> {
        state.activeTest ?: return fail("Não há teste ativo.")
        if (state.teen.broken || state.teen.removed) {
          ok(state.copy(testResolution = FloodTestResolution(emptyList(), 0)))
        } else {
          val expected = state.finalPool()
          if (event.dice.size != expected || event.dice.any { it !in 1..6 }) fail("Resultado de dados inválido para o pool Flood atual.")
          else ok(state.copy(testResolution = FloodTestResolution(event.dice, event.dice.count { it == 6 })))
        }
      }

      is FloodEvent.SetTestResultReported -> {
        val result = state.testResolution ?: return fail("Não há resultado de teste.")
        ok(state.copy(testResolution = result.copy(reportedToGm = event.reported)))
      }

      is FloodEvent.UseShame -> {
        val result = state.testResolution ?: return fail("Role antes de usar Shame.")
        if (result.shameUsed) fail("Shame já foi usado neste teste.")
        else if (!state.teen.shame.isAvailable(event.mysteryId, event.sessionId, event.multiSessionMystery)) fail("Shame indisponível neste período.")
        else {
          val nextShame = if (event.multiSessionMystery) {
            state.teen.shame.copy(lastUsedSession = event.sessionId)
          } else {
            state.teen.shame.copy(lastUsedMystery = event.mysteryId)
          }
          ok(state.copy(
            teen = state.teen.copy(shame = nextShame),
            testResolution = result.copy(successes = result.successes + 1, shameUsed = true)
          ))
        }
      }

      is FloodEvent.UseScar -> {
        val result = state.testResolution ?: return fail("Role antes de usar uma Scar.")
        if (result.scarUsedId != null) fail("Uma Scar já foi usada neste teste.")
        else {
          val idx = state.teen.scars.indexOfFirst { it.id == event.scarId }
          if (idx < 0) fail("Scar inexistente.")
          else if (state.teen.scars[idx].accepted) fail("Scar já aceita não concede novo sucesso automático.")
          else {
            val scars = state.teen.scars.toMutableList()
            scars[idx] = scars[idx].copy(accepted = true)
            ok(state.copy(
              teen = state.teen.copy(scars = scars),
              testResolution = result.copy(successes = result.successes + 1, scarUsedId = event.scarId)
            ))
          }
        }
      }

      is FloodEvent.PushRoll -> {
        val result = state.testResolution ?: return fail("Role antes de Forçar.")
        if (result.pushed) fail("A rolagem Flood só pode ser Forçada uma vez.")
        else if (state.teen.broken || state.teen.removed) fail("Teen Broken/removed não pode Forçar.")
        else {
          val rerollCount = result.dice.count { it != 6 }
          if (event.rerolledNonSixDice.size != rerollCount || event.rerolledNonSixDice.any { it !in 1..6 }) {
            fail("Rerolagem Flood Forçada inválida.")
          } else {
            var nextTeen = state.teen
            if (nextTeen.conditions.size < 4) {
              val c = event.condition ?: return fail("Forçar exige marcar uma Condition.")
              if (c in nextTeen.conditions) return fail("Escolha uma Condition ainda não marcada.")
              nextTeen = nextTeen.copy(conditions = nextTeen.conditions + c)
            } else {
              if (event.condition != null) return fail("As quatro Conditions já estão marcadas; nova Condition torna o Teen Broken.")
              val scarResult = addScar(nextTeen, FloodBreakSource.PUSH, event.scarTextIfBroken, event.deathRollIfRequired)
              if (scarResult.second != null) return fail(scarResult.second!!)
              nextTeen = scarResult.first
            }
            val keptSixes = result.dice.filter { it == 6 }
            val newDice = keptSixes + event.rerolledNonSixDice
            ok(state.copy(
              teen = nextTeen,
              testResolution = result.copy(
                dice = newDice,
                successes = newDice.count { it == 6 } + (if (result.shameUsed) 1 else 0) + (if (result.scarUsedId != null) 1 else 0),
                pushed = true
              )
            ))
          }
        }
      }

      is FloodEvent.ApplyCondition -> {
        if (state.teen.removed) fail("Teen removido do jogo.")
        else if (state.teen.broken) fail("Teen já está Broken.")
        else if (state.teen.conditions.size < 4) {
          val c = event.condition ?: return fail("Escolha a Condition aplicada.")
          if (c in state.teen.conditions) fail("Condition já ativa.")
          else ok(state.copy(teen = state.teen.copy(conditions = state.teen.conditions + c)))
        } else {
          if (event.condition != null) fail("As quatro Conditions já estão marcadas; a próxima torna o Teen Broken.")
          else {
            val scarResult = addScar(state.teen, event.source, event.scarTextIfBroken, event.deathRollIfRequired)
            if (scarResult.second != null) fail(scarResult.second!!)
            else ok(state.copy(teen = scarResult.first))
          }
        }
      }

      is FloodEvent.BecomeBroken -> {
        if (state.teen.broken) fail("Teen já está Broken.")
        else if (state.teen.removed) fail("Teen removido do jogo.")
        else {
          val scarResult = addScar(state.teen, event.source, event.scarText, event.deathRollIfRequired)
          if (scarResult.second != null) fail(scarResult.second!!)
          else ok(state.copy(teen = scarResult.first))
        }
      }

      FloodEvent.RecoverWithAnchor,
      FloodEvent.RecoverWithFriction,
      FloodEvent.RecoverBetweenMysteries -> {
        ok(state.copy(teen = state.teen.copy(conditions = emptySet(), broken = false)))
      }

      is FloodEvent.LeadRecoverySuccess -> {
        if (state.teen.broken) fail("Lead não cura Broken.")
        else if (event.condition !in state.teen.conditions) fail("Condition indicada não está ativa.")
        else ok(state.copy(teen = state.teen.copy(conditions = state.teen.conditions - event.condition)))
      }

      is FloodEvent.LeadRecoveryFailure -> {
        if (state.teen.broken) fail("Teen já está Broken.")
        else if (event.condition in state.teen.conditions) fail("Falha de Lead exige aplicar a mesma Condition ainda livre ao líder.")
        else if (state.teen.conditions.size >= 4) fail("Nova Condition exigiria Broken e Scar; use a transição explícita de Broken.")
        else ok(state.copy(teen = state.teen.copy(conditions = state.teen.conditions + event.condition)))
      }

      is FloodEvent.HealAcceptedScar -> {
        val idx = state.teen.scars.indexOfFirst { it.id == event.scarId }
        if (idx < 0) fail("Scar inexistente.")
        else if (!state.teen.scars[idx].accepted) fail("Somente Scar aceita pode ser curada.")
        else ok(state.copy(teen = state.teen.copy(scars = state.teen.scars.filterIndexed { i, _ -> i != idx })))
      }

      is FloodEvent.StartExtendedTrouble -> {
        if (event.teens < 1 || event.scenes.size != event.teens) fail("Extended Trouble exige uma cena por Teen.")
        else if (event.scenes.any { FloodSkillCatalog.canonicalSkill(it.skill) == null }) fail("Extended Trouble contém Skill inválida.")
        else ok(state.copy(extended = FloodExtendedTroubleState(
          active = true,
          stakes = event.stakes,
          teens = event.teens,
          threatLevel = event.threatLevel,
          scenes = event.scenes
        )))
      }

      is FloodEvent.ResolveExtendedTeenScene -> {
        val ext = state.extended
        if (!ext.active || event.index !in ext.scenes.indices) fail("Cena de Extended Trouble inválida.")
        else if (event.successes < 0) fail("Sucessos inválidos.")
        else if (ext.scenes[event.index].successes != null) fail("Cena de Extended Trouble já resolvida.")
        else {
          val scenes = ext.scenes.toMutableList()
          scenes[event.index] = scenes[event.index].copy(successes = event.successes)
          ok(state.copy(extended = ext.copy(scenes = scenes)))
        }
      }

      is FloodEvent.AcceptExtendedCompromise -> {
        val ext = state.extended
        if (ext.outcome != FloodExtendedOutcome.COMPROMISE_AVAILABLE) fail("Compromisso Flood indisponível neste resultado.")
        else if (event.extraConditions <= 0) fail("Compromisso exige Conditions extras explicitamente aceitas.")
        else ok(state.copy(extended = ext.copy(acceptedExtraConditions = ext.acceptedExtraConditions + event.extraConditions)))
      }

      FloodEvent.ResolveExtendedCompleteFailure -> {
        val ext = state.extended
        if (ext.outcome != FloodExtendedOutcome.COMPLETE_FAILURE) fail("Resultado não é falha completa de Extended Trouble.")
        else fail("Falha completa exige Broken + Scar individual por Teen; resolução deve ocorrer no estado de cada personagem.")
      }

      is FloodEvent.AwardXp -> {
        if (event.amount < 0) fail("XP inválido.")
        else ok(state.copy(teen = state.teen.copy(xp = (state.teen.xp + event.amount).coerceAtMost(10))))
      }

      is FloodEvent.ImproveSkill -> {
        val canonical = FloodSkillCatalog.canonicalSkill(event.skill) ?: return fail("Skill Flood inválida.")
        val current = state.teen.skills[canonical] ?: return fail("Skill ausente na ficha.")
        if (state.teen.xp < 5) fail("São necessários 5 XP.")
        else if (current >= 5) fail("Skill Flood já está no máximo 5.")
        else ok(state.copy(teen = state.teen.copy(
          xp = state.teen.xp - 5,
          skills = state.teen.skills + (canonical to current + 1)
        )))
      }
    }
  }
}
