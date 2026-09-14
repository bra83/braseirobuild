package com.braseiro.talesvtt.game

enum class Condition(val label: String) {
  UPSET("Chateado"),
  SCARED("Assustado"),
  EXHAUSTED("Exausto"),
  INJURED("Machucado")
}

object TalesSkillCatalog {
  val skillToAttribute: Map<String, String> = linkedMapOf(
    "Furtividade" to "CORPO",
    "Força" to "CORPO",
    "Movimentação" to "CORPO",
    "Mecânica" to "TECNOLOGIA",
    "Programação" to "TECNOLOGIA",
    "Cálculo" to "TECNOLOGIA",
    "Contatos" to "CORAÇÃO",
    "Charme" to "CORAÇÃO",
    "Liderança" to "CORAÇÃO",
    "Investigação" to "MENTE",
    "Conhecimento" to "MENTE",
    "Empatia" to "MENTE"
  )

  fun canonicalSkill(name: String): String? = skillToAttribute.keys.firstOrNull { it.equals(name.trim(), ignoreCase = true) }
  fun attributeFor(skill: String): String? = canonicalSkill(skill)?.let(skillToAttribute::get)
}

data class KidRelationship(
  val name: String,
  val description: String
)

data class KidState(
  val name: String = "Lasse Svensson",
  val archetype: String = "Informático",
  val portraitAssetKey: String = "tales_type_computer_geek",
  val portraitImageBase64: String? = null,
  val description: String = "Curioso, observador e sempre desmontando alguma coisa.",
  val problem: String = "Meu pai desapareceu há dois anos e ninguém fala sobre isso.",
  val drive: String = "Saiba a verdade.",
  val prideText: String = "Sou bom com máquinas.",
  val anchorName: String = "Agnes — vizinha.",
  val iconicItemName: String = "Chave inglesa antiga",
  val relationships: List<KidRelationship> = listOf(
    KidRelationship("Jonas", "melhor amigo"),
    KidRelationship("Malin", "colega da escola"),
    KidRelationship("Agnes", "vizinha; confia em mim")
  ),
  val age: Int = 12,
  val luck: Int = 3,
  val conditions: Set<Condition> = setOf(Condition.SCARED),
  val broken: Boolean = false,
  val prideAvailable: Boolean = true,
  val xp: Int = 3,
  val attributes: Map<String, Int> = linkedMapOf(
    "CORPO" to 3,
    "TECNOLOGIA" to 4,
    "CORAÇÃO" to 3,
    "MENTE" to 2
  ),
  val skills: Map<String, Int> = linkedMapOf(
    "Furtividade" to 3,
    "Força" to 2,
    "Movimentação" to 3,
    "Mecânica" to 4,
    "Programação" to 3,
    "Cálculo" to 3,
    "Contatos" to 2,
    "Charme" to 2,
    "Liderança" to 2,
    "Investigação" to 2,
    "Conhecimento" to 3,
    "Empatia" to 3
  )
) {
  val maxLuck: Int get() = 15 - age
  val conditionPenalty: Int get() = conditions.size

  fun testForSkill(reason: String, skill: String, requiredSuccesses: Int): TestRequest? {
    val canonical = TalesSkillCatalog.canonicalSkill(skill) ?: return null
    val attribute = TalesSkillCatalog.attributeFor(canonical) ?: return null
    val attributeValue = attributes[attribute] ?: return null
    val skillValue = skills[canonical] ?: return null
    return TestRequest(
      reason = reason,
      attributeName = attribute,
      attributeValue = attributeValue,
      skillName = canonical.uppercase(),
      skillValue = skillValue,
      requiredSuccesses = requiredSuccesses
    )
  }
}

data class SceneChoice(
  val title: String,
  val description: String
)

data class GroupMemberState(
  val name: String,
  val archetype: String,
  val portraitAssetKey: String,
  val conditions: Set<Condition> = emptySet(),
  val broken: Boolean = false
)

data class GroupRelationshipState(
  val left: String,
  val relation: String,
  val right: String = ""
)

data class GroupState(
  val members: List<GroupMemberState> = listOf(
    GroupMemberState("Lasse Svensson", "Informático", "kid_portrait_01", conditions = setOf(Condition.SCARED)),
    GroupMemberState("Anita Holmberg", "Popular", "kid_portrait_02"),
    GroupMemberState("Jonas Berg", "Esportista", "kid_portrait_03"),
    GroupMemberState("Malin Ek", "Caxias", "kid_portrait_04")
  ),
  val relationships: List<GroupRelationshipState> = listOf(
    GroupRelationshipState("Lasse", "confia em", "Anita"),
    GroupRelationshipState("Jonas", "protege", "Malin"),
    GroupRelationshipState("Malin", "provoca", "Lasse"),
    GroupRelationshipState("Anita", "tenta manter o grupo unido")
  ),
  val hideoutName: String = "Cabana no bosque",
  val hideoutDescription: String = "O esconderijo do grupo: um lugar seguro que os Kids mantêm em segredo.",
  val notes: List<String> = listOf(
    "A fita encontrada no porão precisa ser revista.",
    "Há luzes estranhas próximas ao lago.",
    "Agnes ouviu um ruído metálico na floresta."
  )
)

data class SceneState(
  val title: String = "Ecos no Lago",
  val chapter: String = "CAPÍTULO 1",
  val seasonYear: String = "Primavera, 1986",
  val location: String = "Portão de Serviço",
  val weather: String = "Chuva forte",
  val time: String = "17:42",
  val daylight: String = "Crepúsculo",
  val visualAssetKey: String = "scene_loop_gate",
  val importedImageUri: String? = null,
  val narrative: List<String> = listOf(
    "A tarde cinzenta cai sobre Björkäsen. A chuva bate na cerca do portão de serviço da Loop Industries, e as luzes alaranjadas das torres refletem nas poças do asfalto.",
    "Do outro lado, um caminhão de manutenção permanece parado. Ninguém por perto. Só o vento, a chuva e um zumbido baixo que parece vir debaixo da terra."
  ),
  val choices: List<SceneChoice> = listOf(
    SceneChoice("OBSERVAR ANTES", "Parar e prestar atenção no que há além do portão sem se expor."),
    SceneChoice("IR ATÉ O PORTÃO", "Aproximar-se da entrada para examinar melhor o acesso.")
  )
)

data class TestRequest(
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
)

data class RevealedState(
  val clues: Set<String> = setOf("Foto das luzes no lago", "Fita cassete com ruído metálico", "Bilhete rasgado"),
  val locations: Set<String> = setOf("Portão de Serviço", "Casa de Lasse", "Escola Björkskolan", "Esconderijo"),
  val connections: Set<Pair<String, String>> = setOf("Fita cassete com ruído metálico" to "Portão de Serviço")
)

enum class ThreatLevel(val multiplier: Int, val label: String) {
  NORMAL(2, "Normal"), DIFFICULT(3, "Difícil"), ALMOST_IMPOSSIBLE(4, "Quase impossível")
}

data class ExtendedKidScene(
  val kid: String,
  val skill: String,
  val successes: Int? = null
)

data class CompromiseMark(
  val kid: String,
  val condition: Condition? // null means the next Condition makes this Kid Broken
)

enum class EndingMode { NONE, SESSION, MYSTERY }

data class ChangeState(
  val birthdayReviewed: Boolean = false,
  val birthdayPassed: Boolean = false,
  val birthdayApplied: Boolean = false,
  val improvedAttribute: String? = null,
  val newKidRequired: Boolean = false
)

data class ExtendedTroubleState(
  val active: Boolean = false,
  val stakes: String = "",
  val kids: Int = 4,
  val threatLevel: ThreatLevel = ThreatLevel.DIFFICULT,
  val scenes: List<ExtendedKidScene> = emptyList(),
  val acceptedExtraConditions: Int = 0,
  val compromiseMarks: List<CompromiseMark> = emptyList(),
  val compromiseDeclined: Boolean = false
) {
  val target: Int get() = kids * threatLevel.multiplier
  val resolvedScenes: Int get() = scenes.count { it.successes != null }
  val totalSuccesses: Int get() = scenes.sumOf { it.successes ?: 0 } + acceptedExtraConditions
  val allScenesResolved: Boolean get() = scenes.size == kids && scenes.all { it.successes != null }
  val nextUnresolvedIndex: Int? get() = scenes.indexOfFirst { it.successes == null }.takeIf { it >= 0 }
  val compromiseAvailable: Boolean get() = allScenesResolved && !compromiseDeclined && totalSuccesses >= (target + 1) / 2 && totalSuccesses < target
  val fullSuccess: Boolean get() = allScenesResolved && totalSuccesses >= target
  val completeFailure: Boolean get() = allScenesResolved && totalSuccesses * 2 < target
  val declinedCompromiseFailure: Boolean get() = allScenesResolved && compromiseDeclined && totalSuccesses < target
}

data class GameState(
  val kid: KidState = KidState(),
  val group: GroupState = GroupState(),
  val scene: SceneState = SceneState(),
  val lastPlayerAction: String? = null,
  val activeTest: TestRequest? = null,
  val testResolution: TestResolution? = null,
  val revealed: RevealedState = RevealedState(),
  val routeHistory: List<String> = emptyList(),
  val extended: ExtendedTroubleState = ExtendedTroubleState(),
  val xpAnswers: List<Boolean?> = List(5) { null },
  val sessionXpAwarded: Boolean = false,
  val endingMode: EndingMode = EndingMode.NONE,
  val change: ChangeState = ChangeState(),
  val lastError: String? = null
) {
  fun finalPool(): Int {
    val test = activeTest ?: return 0
    if (kid.broken) return 0
    return (test.basePool() + test.approvedItemBonus + (if (test.helperKid != null) 1 else 0) - kid.conditionPenalty).coerceAtLeast(0)
  }
}

sealed interface GameEvent {
  data class PlayerActionDeclared(val text: String) : GameEvent
  data class GmNarration(val text: String, val choices: List<SceneChoice> = emptyList()) : GameEvent
  data class GmWorldPatch(
    val location: String? = null,
    val weather: String? = null,
    val time: String? = null,
    val daylight: String? = null,
    val visualAssetKey: String? = null
  ) : GameEvent
  data class GmRequestTest(val request: TestRequest) : GameEvent
  data class SetSceneImportedImage(val uri: String?) : GameEvent
  data object ClearTest : GameEvent
  data class ApplyHelp(val helperKid: String) : GameEvent
  data class ApplyApprovedItem(val bonus: Int) : GameEvent
  data class RollResult(val dice: List<Int>) : GameEvent
  data class SetTestResultReported(val reported: Boolean) : GameEvent
  data class SpendLuck(val rerolledFailedDice: List<Int>) : GameEvent
  data class PushRoll(val condition: Condition?, val rerolledNonSixDice: List<Int>) : GameEvent
  data object UsePride : GameEvent
  data class ApplyCondition(val condition: Condition) : GameEvent
  data object BecomeBroken : GameEvent
  data object HealAllConditions : GameEvent
  data class RevealClue(val id: String) : GameEvent
  data class RevealLocation(val id: String) : GameEvent
  data class RevealConnection(val from: String, val to: String) : GameEvent
  data class StartExtendedTrouble(
    val stakes: String,
    val kids: Int,
    val threatLevel: ThreatLevel,
    val scenes: List<ExtendedKidScene>
  ) : GameEvent
  data object BeginExtendedKidScene : GameEvent
  data object FinalizeExtendedKidSceneFromTest : GameEvent
  data class ResolveExtendedKidScene(val index: Int, val successes: Int) : GameEvent
  data class AcceptCompromise(val marks: List<CompromiseMark>) : GameEvent
  data object DeclineCompromise : GameEvent
  data object EndExtendedTrouble : GameEvent
  data class SetXpAnswer(val index: Int, val yes: Boolean) : GameEvent
  data class AwardXp(val amount: Int) : GameEvent
  data class OpenEnding(val mode: EndingMode) : GameEvent
  data class ApplyMysteryEnd(val xpGained: Int) : GameEvent
  data object ConfirmMysteryEnd : GameEvent
  data class SetBirthdayPassed(val passed: Boolean) : GameEvent
  data class ApplyBirthdayAttribute(val attribute: String) : GameEvent
  data object StartNextSession : GameEvent
  data class ImproveSkill(val skill: String) : GameEvent
}

object TalesReducer {
  fun reduce(state: GameState, event: GameEvent): GameState {
    fun fail(message: String) = state.copy(lastError = message)
    fun syncActiveKid(next: GameState): GameState {
      val members = next.group.members.map { member ->
        if (member.name.equals(next.kid.name, ignoreCase = true)) {
          member.copy(conditions = next.kid.conditions, broken = next.kid.broken)
        } else member
      }
      return next.copy(group = next.group.copy(members = members))
    }
    fun ok(next: GameState) = syncActiveKid(next).copy(lastError = null)

    return when (event) {
      is GameEvent.PlayerActionDeclared -> {
        if (event.text.isBlank()) fail("A ação precisa ser descrita.")
        else ok(state.copy(lastPlayerAction = event.text, activeTest = null, testResolution = null))
      }

      is GameEvent.GmNarration -> {
        if (event.text.isBlank()) fail("Narração do Mestre vazia.")
        else ok(state.copy(scene = state.scene.copy(
          narrative = listOf(event.text.trim()),
          choices = event.choices.take(3)
        )))
      }

      is GameEvent.GmWorldPatch -> {
        val nextLocation = event.location?.takeIf { it.isNotBlank() } ?: state.scene.location
        val nextVisual = event.visualAssetKey?.takeIf { it.isNotBlank() } ?: state.scene.visualAssetKey
        val locationChanged = !nextLocation.equals(state.scene.location, ignoreCase = true)
        val nextRouteHistory = if (locationChanged) {
          val base = state.routeHistory.ifEmpty { listOf(state.scene.location) }
          (base + nextLocation)
            .fold(mutableListOf<String>()) { acc, location ->
              if (acc.lastOrNull()?.equals(location, ignoreCase = true) != true) acc += location
              acc
            }
            .takeLast(24)
        } else {
          state.routeHistory.ifEmpty { listOf(state.scene.location) }
        }
        val changedScene = locationChanged || nextVisual != state.scene.visualAssetKey
        ok(state.copy(
          scene = state.scene.copy(
            location = nextLocation,
            weather = event.weather?.takeIf { it.isNotBlank() } ?: state.scene.weather,
            time = event.time?.takeIf { it.isNotBlank() } ?: state.scene.time,
            daylight = event.daylight?.takeIf { it.isNotBlank() } ?: state.scene.daylight,
            visualAssetKey = nextVisual,
            importedImageUri = if (changedScene) null else state.scene.importedImageUri
          ),
          routeHistory = nextRouteHistory
        ))
      }

      is GameEvent.SetSceneImportedImage -> {
        if (event.uri != null && event.uri.isBlank()) fail("URI de imagem vazia.")
        else ok(state.copy(scene = state.scene.copy(importedImageUri = event.uri)))
      }

      is GameEvent.GmRequestTest -> {
        if (event.request.attributeValue < 0 || event.request.skillValue < 0 || event.request.requiredSuccesses !in 1..3) {
          fail("Teste inválido.")
        } else ok(state.copy(activeTest = event.request, testResolution = null))
      }

      GameEvent.ClearTest -> ok(state.copy(activeTest = null, testResolution = null))

      is GameEvent.ApplyHelp -> {
        val test = state.activeTest ?: return fail("Não há teste ativo.")
        if (state.testResolution != null) fail("Ajuda deve ser aplicada antes da rolagem.")
        else if (test.helperKid != null) fail("Só um Kid pode ajudar.")
        else if (event.helperKid.isBlank() || event.helperKid == state.kid.name) fail("Ajudante inválido.")
        else ok(state.copy(activeTest = test.copy(helperKid = event.helperKid)))
      }

      is GameEvent.ApplyApprovedItem -> {
        val test = state.activeTest ?: return fail("Não há teste ativo.")
        if (state.testResolution != null) fail("Item deve ser aprovado antes da rolagem.")
        else if (event.bonus !in 0..3) fail("Bônus de item inválido.")
        else ok(state.copy(activeTest = test.copy(approvedItemBonus = event.bonus)))
      }

      is GameEvent.RollResult -> {
        state.activeTest ?: return fail("Não há teste ativo.")
        if (state.kid.broken) {
          ok(state.copy(testResolution = TestResolution(emptyList(), 0)))
        } else {
          val expected = state.finalPool()
          if (expected <= 0) {
            fail("TAL-EDGE-001: o pool Tales chegou a 0 ou menos. A regra de mínimo 1 dado é exclusiva de Flood/Teens e não pode ser importada; o Mestre precisa resolver este caso sem inventar uma regra Tales.")
          } else if (event.dice.size != expected || event.dice.any { it !in 1..6 }) {
            fail("Resultado de dados inválido para o pool atual.")
          } else {
            ok(state.copy(testResolution = TestResolution(event.dice, event.dice.count { it == 6 })))
          }
        }
      }

      is GameEvent.SetTestResultReported -> {
        val result = state.testResolution ?: return fail("Não há resultado de teste para enviar ao Mestre.")
        ok(state.copy(testResolution = result.copy(reportedToGm = event.reported)))
      }

      is GameEvent.SpendLuck -> {
        val result = state.testResolution ?: return fail("Role antes de usar Sorte.")
        if (result.successes >= (state.activeTest?.requiredSuccesses ?: 1)) fail("Sorte só está disponível após falha.")
        else if (result.luckUsed) fail("Só 1 Ponto de Sorte por rolagem.")
        else if (state.kid.luck <= 0) fail("Sem Pontos de Sorte.")
        else {
          val failedCount = result.dice.count { it != 6 }
          if (event.rerolledFailedDice.size != failedCount || event.rerolledFailedDice.any { it !in 1..6 }) {
            fail("Rerolagem de Sorte inválida.")
          } else {
            val keptSixes = result.dice.filter { it == 6 }
            val newDice = keptSixes + event.rerolledFailedDice
            ok(state.copy(
              kid = state.kid.copy(luck = state.kid.luck - 1),
              testResolution = result.copy(
                dice = newDice,
                successes = newDice.count { it == 6 } + if (result.prideUsed) 1 else 0,
                luckUsed = true
              )
            ))
          }
        }
      }

      is GameEvent.PushRoll -> {
        val result = state.testResolution ?: return fail("Role antes de Forçar.")
        if (result.pushed) fail("A rolagem só pode ser Forçada uma vez.")
        else if (state.kid.broken) fail("Kid Quebrado falha automaticamente.")
        else {
          val rerollCount = result.dice.count { it != 6 }
          if (event.rerolledNonSixDice.size != rerollCount || event.rerolledNonSixDice.any { it !in 1..6 }) {
            fail("Rerolagem Forçada inválida.")
          } else {
            val nextKid = when {
              event.condition != null && event.condition in state.kid.conditions ->
                return fail("Escolha uma Condição ainda não marcada para Forçar.")
              event.condition != null && state.kid.conditions.size < 4 ->
                state.kid.copy(conditions = state.kid.conditions + event.condition)
              event.condition == null && state.kid.conditions.size == 4 ->
                state.kid.copy(broken = true)
              event.condition == null ->
                return fail("Escolha qual Condição marcar para Forçar.")
              else ->
                return fail("Com as quatro Condições leves marcadas, Forçar torna o Kid Quebrado.")
            }
            val keptSixes = result.dice.filter { it == 6 }
            val newDice = keptSixes + event.rerolledNonSixDice
            ok(state.copy(
              kid = nextKid,
              testResolution = result.copy(
                dice = newDice,
                successes = newDice.count { it == 6 } + if (result.prideUsed) 1 else 0,
                pushed = true
              )
            ))
          }
        }
      }

      GameEvent.UsePride -> {
        val result = state.testResolution ?: return fail("Role antes de usar Orgulho.")
        if (!state.kid.prideAvailable) fail("Orgulho indisponível.")
        else if (result.prideUsed) fail("Orgulho já usado neste teste.")
        else ok(state.copy(
          kid = state.kid.copy(prideAvailable = false),
          testResolution = result.copy(successes = result.successes + 1, prideUsed = true)
        ))
      }

      is GameEvent.ApplyCondition -> {
        if (state.kid.broken) fail("Kid já está Quebrado.")
        else if (event.condition in state.kid.conditions) fail("Condição já ativa.")
        else if (state.kid.conditions.size >= 4) ok(state.copy(kid = state.kid.copy(broken = true)))
        else ok(state.copy(kid = state.kid.copy(conditions = state.kid.conditions + event.condition)))
      }

      GameEvent.BecomeBroken -> if (state.kid.broken) fail("Kid já está Quebrado.") else ok(state.copy(kid = state.kid.copy(broken = true)))

      GameEvent.HealAllConditions -> ok(state.copy(kid = state.kid.copy(conditions = emptySet(), broken = false)))

      is GameEvent.RevealClue -> if (event.id.isBlank()) fail("Pista inválida.") else ok(state.copy(revealed = state.revealed.copy(clues = state.revealed.clues + event.id)))
      is GameEvent.RevealLocation -> if (event.id.isBlank()) fail("Local inválido.") else ok(state.copy(revealed = state.revealed.copy(locations = state.revealed.locations + event.id)))
      is GameEvent.RevealConnection -> {
        if (event.from !in state.revealed.clues && event.from !in state.revealed.locations) fail("Origem da conexão ainda não foi revelada.")
        else if (event.to !in state.revealed.clues && event.to !in state.revealed.locations) fail("Destino da conexão ainda não foi revelado.")
        else ok(state.copy(revealed = state.revealed.copy(connections = state.revealed.connections + (event.from to event.to))))
      }

      is GameEvent.StartExtendedTrouble -> {
        if (event.kids < 1 || event.scenes.size != event.kids) fail("Problema Estendido precisa de uma cena por Kid.")
        else if (event.scenes.any { TalesSkillCatalog.canonicalSkill(it.skill) == null }) fail("Problema Estendido contém Perícia inválida.")
        else ok(state.copy(extended = ExtendedTroubleState(
          active = true,
          stakes = event.stakes,
          kids = event.kids,
          threatLevel = event.threatLevel,
          scenes = event.scenes
        )))
      }

      GameEvent.BeginExtendedKidScene -> {
        val ext = state.extended
        val index = ext.nextUnresolvedIndex
        if (!ext.active) fail("Não há Problema Estendido ativo.")
        else if (index == null) fail("Todas as cenas do Problema Estendido já foram resolvidas.")
        else if (state.activeTest != null) fail("Já existe um teste ativo.")
        else {
          val scene = ext.scenes[index]
          val activeKidMatches = scene.kid.equals(state.kid.name, ignoreCase = true) ||
            scene.kid.equals(state.kid.name.substringBefore(' '), ignoreCase = true)
          if (!activeKidMatches) fail("A próxima cena pertence a ${scene.kid}; este cliente não inventa a rolagem de outro Kid.")
          else {
            val request = state.kid.testForSkill(
              reason = "Problema Estendido • ${scene.skill}",
              skill = scene.skill,
              requiredSuccesses = 1
            )?.copy(extendedSceneIndex = index)
            if (request == null) fail("Perícia da cena do Problema Estendido não existe na ficha do Kid.")
            else ok(state.copy(activeTest = request, testResolution = null))
          }
        }
      }

      GameEvent.FinalizeExtendedKidSceneFromTest -> {
        val test = state.activeTest ?: return fail("Não há teste ativo para finalizar.")
        val result = state.testResolution ?: return fail("O teste ainda não foi rolado.")
        val index = test.extendedSceneIndex ?: return fail("O teste ativo não pertence a um Problema Estendido.")
        val ext = state.extended
        if (!ext.active || index !in ext.scenes.indices) fail("Cena de Problema Estendido inválida.")
        else if (ext.scenes[index].successes != null) ok(state)
        else {
          val next = ext.scenes.toMutableList()
          next[index] = next[index].copy(successes = result.successes)
          ok(state.copy(extended = ext.copy(scenes = next)))
        }
      }

      is GameEvent.ResolveExtendedKidScene -> {
        val ext = state.extended
        if (!ext.active) fail("Não há Problema Estendido ativo.")
        else if (event.index !in ext.scenes.indices || event.successes < 0) fail("Resultado de cena inválido.")
        else if (ext.scenes[event.index].successes != null) fail("Cada Kid rola uma vez.")
        else {
          val next = ext.scenes.toMutableList()
          next[event.index] = next[event.index].copy(successes = event.successes)
          ok(state.copy(extended = ext.copy(scenes = next)))
        }
      }

      is GameEvent.AcceptCompromise -> {
        val ext = state.extended
        if (!ext.compromiseAvailable) fail("Compromisso não está disponível.")
        else if (event.marks.isEmpty()) fail("É preciso aceitar ao menos uma Condição extra.")
        else {
          val needed = (ext.target - ext.totalSuccesses).coerceAtLeast(0)
          if (event.marks.size != needed) fail("O compromisso exige exatamente $needed Condição(ões) extra(s).")
          else {
            var nextGroup = state.group
            var nextKid = state.kid
            val applied = mutableListOf<CompromiseMark>()
            var validationError: String? = null

            for (mark in event.marks) {
              if (validationError != null) break
              val index = nextGroup.members.indexOfFirst {
                it.name.equals(mark.kid, ignoreCase = true) || it.name.substringBefore(' ').equals(mark.kid, ignoreCase = true)
              }
              if (index < 0) {
                validationError = "Kid do compromisso não pertence ao grupo."
                break
              }
              val member = nextGroup.members[index]
              if (member.broken) {
                validationError = "${member.name} já está Quebrado."
                break
              }
              val updated = when {
                mark.condition != null && mark.condition in member.conditions -> {
                  validationError = "${member.name} já possui a Condição ${mark.condition.label}."
                  member
                }
                mark.condition != null && member.conditions.size < 4 ->
                  member.copy(conditions = member.conditions + mark.condition)
                mark.condition == null && member.conditions.size == 4 ->
                  member.copy(broken = true)
                mark.condition == null -> {
                  validationError = "${member.name} só pode ficar Quebrado após as quatro Condições leves."
                  member
                }
                else -> {
                  validationError = "${member.name} precisa ficar Quebrado para aceitar outra Condição."
                  member
                }
              }
              if (validationError == null) {
                val members = nextGroup.members.toMutableList()
                members[index] = updated
                nextGroup = nextGroup.copy(members = members)
                if (updated.name.equals(nextKid.name, ignoreCase = true)) {
                  nextKid = nextKid.copy(conditions = updated.conditions, broken = updated.broken)
                }
                applied += mark.copy(kid = updated.name)
              }
            }

            if (validationError != null) fail(validationError)
            else ok(state.copy(
              kid = nextKid,
              group = nextGroup,
              extended = ext.copy(
                acceptedExtraConditions = ext.acceptedExtraConditions + applied.size,
                compromiseMarks = ext.compromiseMarks + applied
              )
            ))
          }
        }
      }

      GameEvent.DeclineCompromise -> {
        val ext = state.extended
        if (!ext.compromiseAvailable) fail("Não há compromisso disponível para recusar.")
        else ok(state.copy(extended = ext.copy(compromiseDeclined = true)))
      }

      GameEvent.EndExtendedTrouble -> {
        val ext = state.extended
        if (!ext.active) fail("Não há Problema Estendido ativo.")
        else if (!ext.allScenesResolved) fail("O Problema Estendido só termina após todas as cenas.")
        else if (ext.compromiseAvailable) fail("O grupo precisa aceitar ou recusar o compromisso antes de encerrar.")
        else ok(state.copy(extended = ext.copy(active = false), activeTest = null, testResolution = null))
      }

      is GameEvent.SetXpAnswer -> {
        if (event.index !in 0..4) fail("Pergunta de XP inválida.")
        else {
          val answers = state.xpAnswers.toMutableList()
          answers[event.index] = event.yes
          ok(state.copy(xpAnswers = answers))
        }
      }

      is GameEvent.AwardXp -> if (event.amount < 0) fail("XP inválido.") else ok(state.copy(kid = state.kid.copy(xp = (state.kid.xp + event.amount).coerceAtMost(10))))

      is GameEvent.OpenEnding -> {
        if (event.mode == EndingMode.NONE) fail("Tipo de encerramento inválido.")
        else ok(state.copy(
          endingMode = event.mode,
          xpAnswers = List(5) { null },
          sessionXpAwarded = false,
          change = if (event.mode == EndingMode.MYSTERY) ChangeState() else state.change
        ))
      }

      is GameEvent.ApplyMysteryEnd -> {
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
      }

      GameEvent.ConfirmMysteryEnd -> {
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
      }

      is GameEvent.SetBirthdayPassed -> {
        if (state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded) {
          fail("Aniversário só é revisado após encerrar um Mistério.")
        } else if (state.change.birthdayApplied && state.change.birthdayReviewed) {
          fail("A revisão de aniversário deste encerramento já foi concluída.")
        } else ok(state.copy(change = state.change.copy(
          birthdayReviewed = true,
          birthdayPassed = event.passed,
          birthdayApplied = !event.passed,
          improvedAttribute = null
        )))
      }

      is GameEvent.ApplyBirthdayAttribute -> {
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
      }

      GameEvent.StartNextSession -> {
        when {
          !state.sessionXpAwarded -> fail("Conclua o XP da sessão antes de continuar.")
          state.endingMode == EndingMode.MYSTERY && !state.change.birthdayReviewed -> fail("Confirme se houve aniversário antes do próximo Mistério.")
          state.endingMode == EndingMode.MYSTERY && state.change.birthdayPassed && !state.change.birthdayApplied -> fail("Aplique a mudança de aniversário antes de continuar.")
          state.change.newKidRequired -> fail("Aos 16 anos este Kid segue em frente; crie um novo Kid antes de continuar.")
          else -> ok(state.copy(
            kid = state.kid.copy(luck = state.kid.maxLuck, prideAvailable = true),
            xpAnswers = List(5) { null },
            sessionXpAwarded = false,
            endingMode = EndingMode.NONE,
            change = ChangeState(),
            activeTest = null,
            testResolution = null,
            lastPlayerAction = null
          ))
        }
      }

      is GameEvent.ImproveSkill -> {
        val canonical = TalesSkillCatalog.canonicalSkill(event.skill)
        when {
          !state.sessionXpAwarded -> fail("XP só pode ser gasto fora da sessão; conclua o XP da sessão primeiro.")
          canonical == null -> fail("Perícia inválida para Tales/Kids.")
          state.kid.xp < 5 -> fail("Aprimorar Perícia custa 5 XP.")
          (state.kid.skills[canonical] ?: 0) >= 5 -> fail("Perícia já atingiu o máximo 5.")
          else -> {
            val nextSkills = state.kid.skills.toMutableMap()
            nextSkills[canonical] = (nextSkills[canonical] ?: 0) + 1
            ok(state.copy(kid = state.kid.copy(xp = state.kid.xp - 5, skills = nextSkills)))
          }
        }
      }
    }
  }
}
