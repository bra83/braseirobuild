package com.braseiro.talesvtt.game

enum class Condition(val label: String) {
  UPSET("Chateado"),
  SCARED("Assustado"),
  EXHAUSTED("Exausto"),
  INJURED("Machucado")
}

object TalesSkillCatalog {
  val skillToAttribute: Map<String, String> = CanonicalTalesRules.skillToAttribute

  fun canonicalSkill(name: String): String? = skillToAttribute.keys.firstOrNull { it.equals(name.trim(), ignoreCase = true) }
  fun attributeFor(skill: String): String? = canonicalSkill(skill)?.let(skillToAttribute::get)
}

data class KidRelationship(
  val name: String,
  val description: String
)

enum class KidItemKind { ORDINARY, MYSTERY }

data class KidItemState(
  val name: String,
  val description: String = "",
  val bonus: Int = 0,
  val kind: KidItemKind = KidItemKind.ORDINARY
)

data class KidState(
  val id: String = "char-" + java.util.UUID.randomUUID().toString().replace("-", "").take(8),
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
  val favoriteSong: String = "",
  val birthday: String = "",
  val shameText: String = "",
  val shameAvailable: Boolean = false,
  val scars: List<FloodScarState> = emptyList(),
  val hooks: List<String> = emptyList(),
  val friction: String = "",
  val removedFromPlay: Boolean = false,
  val scarPending: Boolean = false,
  val items: List<KidItemState> = emptyList(),
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
  val maxLuck: Int get() = CanonicalTalesRules.maxLuck(age)
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
  val newKidRequired: Boolean = false,
  val characterReviewConfirmed: Boolean = false
)

data class ExtendedTroubleState(
  val rulesProfile: RulesProfile = RulesProfile.TALES_KIDS,
  val active: Boolean = false,
  val stakes: String = "",
  val kids: Int = 4,
  val threatLevel: ThreatLevel = ThreatLevel.DIFFICULT,
  val scenes: List<ExtendedKidScene> = emptyList(),
  val acceptedExtraConditions: Int = 0,
  val compromiseMarks: List<CompromiseMark> = emptyList(),
  val compromiseDeclined: Boolean = false
) {
  val target: Int get() = RulesProfiles.threatTarget(rulesProfile, kids, threatLevel)
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
  val rulesProfile: RulesProfile = RulesProfile.TALES_KIDS,
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
  val informationBonusBank: Int = 0,
  val leadershipPoolDice: Int = 0,
  val leadershipAssignments: Map<String, Int> = emptyMap(),
  val grantedSuccesses: Map<String, Int> = emptyMap(),
  val waivedTroubles: Set<String> = emptySet(),
  val contactItemBonusBank: Int = 0,
  val lastError: String? = null
) {
  fun finalPool(): Int {
    val test = activeTest ?: return 0
    if (kid.broken) return 0
    val leadership = leadershipAssignments.entries.firstOrNull { it.key.equals(kid.name, ignoreCase = true) }?.value ?: 0
    val raw = test.basePool() + test.approvedItemBonus + test.informationBonus + leadership + (if (test.helperKid != null) CanonicalTalesRules.HELP_BONUS else 0) - kid.conditionPenalty
    return when (rulesProfile) {
      RulesProfile.TALES_KIDS -> raw.coerceAtLeast(0)
      RulesProfile.FLOOD_TEENS -> raw.coerceAtLeast(CanonicalFloodRules.MIN_NON_BROKEN_POOL)
    }
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
  data object ApplyInformationBonus : GameEvent
  data object ApplyContactItemBonus : GameEvent
  data class PurchaseBonusEffect(val effectId: String, val target: String? = null) : GameEvent
  data class ApplyLeadershipComfort(val condition: Condition) : GameEvent
  data class AssignLeadershipDice(val kid: String, val dice: Int = 1) : GameEvent
  data class PromoteMysteryItemToIconic(val itemName: String) : GameEvent
  data class AddItem(val item: KidItemState) : GameEvent
  data class RemoveItem(val name: String) : GameEvent
  data class HealCondition(val condition: Condition) : GameEvent
  data class RollResult(val dice: List<Int>) : GameEvent
  data class SetTestResultReported(val reported: Boolean) : GameEvent
  data class SpendLuck(val rerolledFailedDice: List<Int>) : GameEvent
  data class PushRoll(val condition: Condition?, val rerolledNonSixDice: List<Int>) : GameEvent
  data object UsePride : GameEvent
  data object UseShame : GameEvent
  data class RecordFloodScar(val text: String, val survivalRoll: Int? = null) : GameEvent
  data class UseScar(val index: Int) : GameEvent
  data class HealAcceptedScar(val index: Int, val groupApproved: Boolean) : GameEvent
  data class SetFriction(val text: String) : GameEvent
  data class ResolveFriction(val troubleOccurred: Boolean) : GameEvent
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
  data class ApplyBetweenMysteryChanges(
    val problem: String? = null,
    val drive: String? = null,
    val prideText: String? = null,
    val iconicItemName: String? = null,
    val relationships: List<KidRelationship>? = null,
    val shameText: String? = null
  ) : GameEvent
  data object ConfirmBetweenMysteryReview : GameEvent
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
        val skill = event.request.skillName?.let(TalesSkillCatalog::canonicalSkill)
        val key = RulesProfiles.troubleKey(state.rulesProfile,event.request.reason)
        when {
          event.request.attributeValue < 0 || event.request.skillValue < 0 || event.request.requiredSuccesses !in 1..3 -> fail("Teste inválido.")
          skill == null -> fail("Teste precisa de Perícia canônica Tales/Kids.")
          !RulesProfiles.purposeAllowed(state.rulesProfile,skill,event.request.purpose) -> fail("Finalidade incompatível com a Perícia no perfil ativo.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && event.request.targetKid.isNullOrBlank() -> fail("Conforto por Liderança exige um Kid alvo.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.none { member -> event.request.targetKid?.let { target -> member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true) } == true } -> fail("Kid alvo do conforto não pertence ao grupo.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.any { member -> event.request.targetKid?.let { target -> (member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true)) && member.name.equals(state.kid.name,true) } == true } -> fail("Liderança conforta outro Kid, não o próprio líder.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.firstOrNull { member -> event.request.targetKid?.let { target -> member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true) } == true }?.broken == true -> fail("Conforto por Liderança não cura Kid Quebrado.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.firstOrNull { member -> event.request.targetKid?.let { target -> member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true) } == true }?.conditions?.isEmpty() == true -> fail("Kid alvo não possui Condição para confortar.")
          key in state.waivedTroubles -> ok(state.copy(activeTest=event.request,testResolution=TestResolution(emptyList(),event.request.requiredSuccesses)))
          else -> ok(state.copy(activeTest=event.request,testResolution=null))
        }
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
        else if (test.approvedItemBonus != 0) fail("Só um Item pode fornecer bônus ao teste.")
        else if (event.bonus !in 0..3) fail("Bônus de item inválido.")
        else ok(state.copy(activeTest = test.copy(approvedItemBonus = event.bonus)))
      }

      GameEvent.ApplyInformationBonus -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        when {
          state.testResolution!=null -> fail("Bônus de informação deve ser usado antes da rolagem.")
          state.informationBonusBank<=0 -> fail("Nenhum bônus de informação disponível.")
          test.informationBonus>=3 -> fail("Máximo de +3 dados de informação.")
          else -> ok(state.copy(informationBonusBank=state.informationBonusBank-1,activeTest=test.copy(informationBonus=test.informationBonus+1)))
        }
      }

      GameEvent.ApplyContactItemBonus -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        when {
          state.testResolution!=null -> fail("O contato deve ser usado antes da rolagem.")
          state.contactItemBonusBank !in 1..3 -> fail("Nenhum bônus de contato disponível.")
          test.approvedItemBonus!=0 -> fail("Só um Item pode fornecer bônus ao teste.")
          else -> ok(state.copy(contactItemBonusBank=0,activeTest=test.copy(approvedItemBonus=state.contactItemBonusBank)))
        }
      }

      is GameEvent.PurchaseBonusEffect -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        val result=state.testResolution ?: return fail("Role antes de comprar efeito.")
        val skill=test.skillName?.let(TalesSkillCatalog::canonicalSkill) ?: return fail("Perícia inválida.")
        val rule=RulesProfiles.effect(state.rulesProfile,event.effectId) ?: return fail("Efeito bônus não canônico para o perfil ativo.")
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
                if(member.name.equals(state.kid.name,true)) return fail(if(state.rulesProfile==RulesProfile.FLOOD_TEENS) "O Teen não pode dar sucesso a si mesmo." else "O Kid não pode dar sucesso a si mesmo.")
                next=next.copy(grantedSuccesses=next.grantedSuccesses+(member.name to ((next.grantedSuccesses[member.name]?:0)+1)))
              }
              BonusEffectKind.SAME_TROUBLE_WAIVER -> next=next.copy(waivedTroubles=next.waivedTroubles+RulesProfiles.troubleKey(state.rulesProfile,test.reason))
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
              BonusEffectKind.HEAL_OTHER_CONDITION -> {
                if(test.purpose!=TestPurpose.LEADERSHIP_COMFORT || !result.baseEffectApplied) return fail("Cure primeiro a Condição básica do Kid confortado.")
                val targetName=test.targetKid.orEmpty()
                val index=next.group.members.indexOfFirst{it.name.equals(targetName,true)||it.name.substringBefore(' ').equals(targetName,true)}
                if(index<0) return fail("Kid confortado não pertence ao grupo.")
                val member=next.group.members[index]
                if(member.broken) return fail("Conforto por Liderança não cura Kid Quebrado.")
                val c=event.target?.let{raw->Condition.entries.firstOrNull{it.name.equals(raw,true)||it.label.equals(raw,true)}} ?: return fail("Escolha uma Condição do Kid confortado.")
                if(c !in member.conditions) return fail("Condição não está marcada no Kid confortado.")
                val members=next.group.members.toMutableList(); members[index]=member.copy(conditions=member.conditions-c)
                next=next.copy(group=next.group.copy(members=members))
              }
              BonusEffectKind.CONTACT_ITEM -> {
                if(next.contactItemBonusBank>=3) return fail("O contato já está preparado como Item +3.")
                next=next.copy(contactItemBonusBank=next.contactItemBonusBank+1)
              }
              else -> Unit
            }
            ok(next.copy(testResolution=result.copy(bonusEffects=result.bonusEffects+BonusEffectPurchase(rule.id,event.target))))
          }
        }
      }

      is GameEvent.ApplyLeadershipComfort -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        val result=state.testResolution ?: return fail("Role Liderança antes de aplicar conforto.")
        val targetName=test.targetKid.orEmpty()
        val index=state.group.members.indexOfFirst{it.name.equals(targetName,true)||it.name.substringBefore(' ').equals(targetName,true)}
        when {
          test.purpose!=TestPurpose.LEADERSHIP_COMFORT -> fail("O teste ativo não é de conforto por Liderança.")
          result.successes<test.requiredSuccesses -> fail("Conforto exige sucesso no teste de Liderança.")
          result.baseEffectApplied -> fail("A Condição básica do conforto já foi curada.")
          index<0 -> fail("Kid confortado não pertence ao grupo.")
          state.group.members[index].name.equals(state.kid.name,true) -> fail("O líder não conforta a si mesmo com este efeito.")
          state.group.members[index].broken -> fail("Conforto por Liderança não cura Kid Quebrado.")
          event.condition !in state.group.members[index].conditions -> fail("Condição não está marcada no Kid confortado.")
          else -> {
            val members=state.group.members.toMutableList()
            val member=members[index]
            members[index]=member.copy(conditions=member.conditions-event.condition)
            ok(state.copy(group=state.group.copy(members=members),testResolution=result.copy(baseEffectApplied=true)))
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

      is GameEvent.AddItem -> {
        val item = event.item.copy(name = event.item.name.trim(), description = event.item.description.trim())
        when {
          !RulesProfiles.validItem(state.rulesProfile,item) -> fail("Item viola o contrato do perfil ativo.")
          state.kid.items.any { it.name.equals(item.name, ignoreCase = true) } -> fail("O Kid já possui este Item.")
          else -> ok(state.copy(kid = state.kid.copy(items = state.kid.items + item)))
        }
      }

      is GameEvent.RemoveItem -> {
        val item = state.kid.items.firstOrNull { it.name.equals(event.name.trim(), ignoreCase = true) }
        if (item == null) fail("Item não pertence ao Kid.")
        else ok(state.copy(kid = state.kid.copy(items = state.kid.items - item)))
      }

      is GameEvent.HealCondition -> {
        when {
          state.kid.broken -> fail("Recuperação por conforto não cura um Kid Quebrado.")
          event.condition !in state.kid.conditions -> fail("Condição não está marcada.")
          else -> ok(state.copy(kid = state.kid.copy(conditions = state.kid.conditions - event.condition)))
        }
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
            run {
            val successes=event.dice.count{it==6}
            val pool=if(state.activeTest?.purpose==TestPurpose.LEADERSHIP_PREPARE && successes >= (state.activeTest?.requiredSuccesses ?: 1)) RulesProfiles.leadershipPool(state.rulesProfile,successes) else state.leadershipPoolDice
            val assignedKey=state.leadershipAssignments.keys.firstOrNull{it.equals(state.kid.name,true)}
            val remainingAssignments=if(assignedKey!=null) state.leadershipAssignments-assignedKey else state.leadershipAssignments
            ok(state.copy(
              leadershipPoolDice=pool,
              leadershipAssignments=if(pool!=state.leadershipPoolDice) emptyMap() else remainingAssignments,
              testResolution=TestResolution(event.dice,successes)
            ))
          }
          }
        }
      }

      is GameEvent.SetTestResultReported -> {
        val result = state.testResolution ?: return fail("Não há resultado de teste para enviar ao Mestre.")
        ok(state.copy(testResolution = result.copy(reportedToGm = event.reported)))
      }

      is GameEvent.SpendLuck -> {
        if (state.rulesProfile != RulesProfile.TALES_KIDS) return fail("Things from the Flood não usa Pontos de Sorte.")
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
                state.kid.copy(broken = true, scarPending = state.rulesProfile == RulesProfile.FLOOD_TEENS || state.kid.scarPending)
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

      is GameEvent.RecordFloodScar -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Cicatrizes são exclusivas de Things from the Flood.")
        else if (!state.kid.broken || !state.kid.scarPending) fail("Não há Cicatriz pendente para registrar.")
        else if (event.text.trim().isBlank()) fail("Descreva a Cicatriz em uma frase.")
        else {
          val nextCount = state.kid.scars.size + 1
          val roll = event.survivalRoll
          val invalidRoll = nextCount in 2..5 && roll !in 1..6
          if (invalidRoll) fail("Da segunda à quinta Cicatriz é obrigatório rolar 1d6 para permanência do Teen.")
          else {
            val removed = nextCount >= 6 || (nextCount >= 2 && (roll ?: 6) < nextCount)
            val scar = FloodScarState(event.text.trim().take(500), accepted=false, usedForSuccess=false)
            ok(state.copy(kid=state.kid.copy(
              scars=state.kid.scars+scar,
              scarPending=false,
              removedFromPlay=removed
            )))
          }
        }
      }

      is GameEvent.UseScar -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Cicatrizes são exclusivas de Things from the Flood.")
        else {
          val result=state.testResolution ?: return fail("Role antes de ativar uma Cicatriz.")
          val scar=state.kid.scars.getOrNull(event.index) ?: return fail("Cicatriz inválida.")
          if (scar.usedForSuccess || scar.accepted) fail("Esta Cicatriz já foi aceita e não pode conceder outro sucesso.")
          else {
            val scars=state.kid.scars.toMutableList()
            scars[event.index]=scar.copy(accepted=true,usedForSuccess=true)
            ok(state.copy(kid=state.kid.copy(scars=scars),testResolution=result.copy(successes=result.successes+1)))
          }
        }
      }

      is GameEvent.HealAcceptedScar -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Cicatrizes são exclusivas de Things from the Flood.")
        else if (!state.sessionXpAwarded) fail("Cicatrizes só são curadas após a sessão.")
        else if (!event.groupApproved) fail("A cura da Cicatriz precisa ser aprovada pelo grupo após ação heroica/sacrificial ou cena cotidiana transformadora.")
        else {
          val scar=state.kid.scars.getOrNull(event.index) ?: return fail("Cicatriz inválida.")
          if (!scar.accepted || !scar.usedForSuccess) fail("Só uma Cicatriz aceita, já usada para um sucesso automático, pode ser curada.")
          else ok(state.copy(kid=state.kid.copy(scars=state.kid.scars.filterIndexed{i,_->i!=event.index})))
        }
      }

      is GameEvent.SetFriction -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Friction é exclusiva de Things from the Flood.")
        else if (!state.sessionXpAwarded && state.kid.friction.isNotBlank()) fail("Friction só pode mudar entre sessões.")
        else if (event.text.trim().isBlank()) fail("Friction não pode ficar vazia.")
        else ok(state.copy(kid=state.kid.copy(friction=event.text.trim().take(1000))))
      }

      is GameEvent.ResolveFriction -> {
        if (state.rulesProfile != RulesProfile.FLOOD_TEENS) fail("Friction é exclusiva de Things from the Flood.")
        else if (state.kid.scarPending) fail("Registre primeiro a Cicatriz causada por ficar Quebrado.")
        else if (event.troubleOccurred) ok(state) // Trouble na própria cena cancela toda a cura.
        else ok(state.copy(kid=state.kid.copy(conditions=emptySet(),broken=false)))
      }

      is GameEvent.ApplyCondition -> {
        if (state.kid.broken) fail("Kid já está Quebrado.")
        else if (event.condition in state.kid.conditions) fail("Condição já ativa.")
        else if (state.kid.conditions.size >= 4) ok(state.copy(kid = state.kid.copy(
          broken = true,
          scarPending = state.rulesProfile == RulesProfile.FLOOD_TEENS || state.kid.scarPending
        )))
        else ok(state.copy(kid = state.kid.copy(conditions = state.kid.conditions + event.condition)))
      }

      GameEvent.BecomeBroken -> if (state.kid.broken) fail("Kid já está Quebrado.") else ok(state.copy(kid = state.kid.copy(
        broken = true,
        scarPending = state.rulesProfile == RulesProfile.FLOOD_TEENS || state.kid.scarPending
      )))

      GameEvent.HealAllConditions -> {
        if (state.rulesProfile == RulesProfile.FLOOD_TEENS && state.kid.scarPending) fail("Registre primeiro a Cicatriz causada por ficar Quebrado.")
        else ok(state.copy(kid = state.kid.copy(conditions = emptySet(), broken = false)))
      }

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
          rulesProfile = state.rulesProfile,
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
          val threatSuccesses = if (test.purpose == TestPurpose.LEADERSHIP_PREPARE) 0 else result.successes
          next[index] = next[index].copy(successes = threatSuccesses)
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
                  val newlyBroken = updated.broken && !nextKid.broken
                  nextKid = nextKid.copy(
                    conditions = updated.conditions,
                    broken = updated.broken,
                    scarPending = nextKid.scarPending || (state.rulesProfile == RulesProfile.FLOOD_TEENS && newlyBroken)
                  )
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
        else if (state.rulesProfile == RulesProfile.FLOOD_TEENS && ext.completeFailure) {
          val members=state.group.members.map { it.copy(broken=true) }
          ok(state.copy(
            kid=state.kid.copy(broken=true,scarPending=true),
            group=state.group.copy(members=members),
            extended=ext.copy(active=false),
            activeTest=null,
            testResolution=null
          ))
        } else ok(state.copy(extended = ext.copy(active = false), activeTest = null, testResolution = null))
      }

      is GameEvent.SetXpAnswer -> {
        if (event.index !in 0..4) fail("Pergunta de XP inválida.")
        else if (event.index == 0 && !event.yes) fail("Um Kid presente sempre recebe o XP de participação.")
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
      }

      GameEvent.ConfirmMysteryEnd -> {
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
      }

      is GameEvent.ApplyBetweenMysteryChanges -> {
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
      }

      GameEvent.ConfirmBetweenMysteryReview -> {
        when {
          state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("A revisão de Mudança só é confirmada entre Mistérios.")
          state.change.characterReviewConfirmed -> ok(state)
          else -> ok(state.copy(
            kid=state.kid.copy(items=state.kid.items.filterNot{it.kind==KidItemKind.MYSTERY}),
            change=state.change.copy(characterReviewConfirmed=true)
          ))
        }
      }

      GameEvent.StartNextSession -> {
        when {
          !state.sessionXpAwarded -> fail("Conclua o XP da sessão antes de continuar.")
          state.endingMode == EndingMode.MYSTERY && !state.change.characterReviewConfirmed -> fail("Confirme a revisão de Mudança antes do próximo Mistério.")
          state.endingMode == EndingMode.MYSTERY && !state.change.birthdayReviewed -> fail("Confirme se houve aniversário antes do próximo Mistério.")
          state.endingMode == EndingMode.MYSTERY && state.change.birthdayPassed && !state.change.birthdayApplied -> fail("Aplique a mudança de aniversário antes de continuar.")
          state.change.newKidRequired -> fail(if(state.rulesProfile==RulesProfile.FLOOD_TEENS) "Aos 20 anos este Teen segue em frente; crie um novo Teen antes de continuar." else "Aos 16 anos este Kid segue em frente; crie um novo Kid antes de continuar.")
          else -> ok(state.copy(
            kid = when(state.rulesProfile) {
              RulesProfile.TALES_KIDS -> state.kid.copy(luck = state.kid.maxLuck, prideAvailable = true)
              RulesProfile.FLOOD_TEENS -> state.kid.copy(luck = 0, prideAvailable = false, shameAvailable = true)
            },
            xpAnswers = List(5) { null },
            sessionXpAwarded = false,
            endingMode = EndingMode.NONE,
            change = ChangeState(),
            activeTest = null,
            testResolution = null,
            lastPlayerAction = null,
            informationBonusBank = 0,
            leadershipPoolDice = 0,
            leadershipAssignments = emptyMap(),
            grantedSuccesses = emptyMap(),
            waivedTroubles = emptySet(),
            contactItemBonusBank = 0
          ))
        }
      }

      is GameEvent.ImproveSkill -> {
        val canonical = TalesSkillCatalog.canonicalSkill(event.skill)
        when {
          !state.sessionXpAwarded -> fail("XP só pode ser gasto fora da sessão; conclua o XP da sessão primeiro.")
          canonical == null -> fail("Perícia inválida para o perfil ativo.")
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
