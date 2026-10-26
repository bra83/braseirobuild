package com.braseiro.talesvtt.game

import android.app.Application
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.braseiro.talesvtt.data.KidJsonCodec
import com.braseiro.talesvtt.life.AdventureRuntimeProgress
import com.braseiro.talesvtt.life.AdventurePackage
import com.braseiro.talesvtt.life.AdventureRepository
import com.braseiro.talesvtt.life.OffSceneEvent
import com.braseiro.talesvtt.life.WorldTimelineState
import com.braseiro.talesvtt.life.AdventurePackageValidator
import com.braseiro.talesvtt.life.PersistentLifeEngine
import com.braseiro.talesvtt.life.CharacterIdentityKey
import com.braseiro.talesvtt.life.CharacterLegacyRecord
import com.braseiro.talesvtt.life.ConfirmedDeed
import com.braseiro.talesvtt.data.ManualLibraryRepository
import com.braseiro.talesvtt.data.ManualLibraryStatus
import com.braseiro.talesvtt.data.PortableCampaignBackup
import com.braseiro.talesvtt.data.SceneImageExchange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MotorRuntimeStatus(
  val firewallReady: Boolean = true,
  val transportConnected: Boolean = false,
  val busy: Boolean = false,
  val lastAcceptedActions: Int = 0,
  val lastRejectedActions: Int = 0,
  val lastMessage: String = "Firewall de eventos pronto; transporte do Mestre ainda não conectado."
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

  val adventureRepo by lazy { AdventureRepository(getApplication()) }
  val lifeEngine by lazy { PersistentLifeEngine(getApplication()) }
  private val prefs = application.getSharedPreferences("tftl_game_state", 0)

  private val restoredAutosave = (prefs.getString(KEY_AUTOSAVE, null) ?: prefs.getString(KEY_AUTOSAVE_V1, null))?.let(GameSnapshotCodec::decode)
  private val _state = MutableStateFlow(restoredAutosave ?: GameState())
  val state: StateFlow<GameState> = _state.asStateFlow()

  // GM-private facts are intentionally not exposed to Compose/player UI.
  private var gmPrivateState: GmPrivateState = prefs.getString(KEY_GM_AUTOSAVE, null)
    ?.let(GmPrivateSnapshotCodec::decode)
    ?: GmPrivateState()

  private val _hasManualSave = MutableStateFlow(prefs.contains(KEY_MANUAL_SAVE) || prefs.contains(KEY_MANUAL_SAVE_V1))
  val hasManualSave: StateFlow<Boolean> = _hasManualSave.asStateFlow()

  private val _motorStatus = MutableStateFlow(MotorRuntimeStatus())
  val motorStatus: StateFlow<MotorRuntimeStatus> = _motorStatus.asStateFlow()

  private val _motorConfig = MutableStateFlow(MotorTransportConfig(false, false, ""))
  val motorConfig: StateFlow<MotorTransportConfig> = _motorConfig.asStateFlow()

  private val _motorRoute = MutableStateFlow(MotorRouteHint.NONE)
  val motorRoute: StateFlow<MotorRouteHint> = _motorRoute.asStateFlow()

  private val motorTransport = MotorBarbaraTransport(application)
  private val portableBackup = PortableCampaignBackup(application)
  private val manualLibrary = ManualLibraryRepository(application)
  private val _dataStatus = MutableStateFlow("Dados locais prontos.")
  val dataStatus: StateFlow<String> = _dataStatus.asStateFlow()
  private val _manualLibraryStatus = MutableStateFlow(ManualLibraryStatus(manuals = manualLibrary.listManuals()))
  val manualLibraryStatus: StateFlow<ManualLibraryStatus> = _manualLibraryStatus.asStateFlow()
  private val _adventureStarted = MutableStateFlow(prefs.getBoolean(KEY_ADVENTURE_STARTED, false))
  val adventureStarted: StateFlow<Boolean> = _adventureStarted.asStateFlow()
  private var currentAdventureInstanceId: String? = prefs.getString(KEY_ACTIVE_INSTANCE_ID, null)

  init {
    _motorConfig.value = motorTransport.currentConfig()
    if (_motorConfig.value.apiKeyConfigured) {
      _motorStatus.value = _motorStatus.value.copy(lastMessage = "Firewall pronto; configuração do Motor disponível.")
    }
  }

  fun dispatch(event: GameEvent) {
    _state.value = TalesReducer.reduce(_state.value, event)
    persistAutosave()
  }

  fun declareAction(text: String) = dispatch(GameEvent.PlayerActionDeclared(text))

  fun submitPlayerIntent(text: String) {
    dispatch(GameEvent.PlayerActionDeclared(text))
    if (_state.value.lastError == null) requestMotor(MotorIntent(type = "free_action", text = text))
  }

  fun submitTravelIntent(text: String) {
    dispatch(GameEvent.PlayerActionDeclared(text))
    if (_state.value.lastError == null) requestMotor(MotorIntent(type = "travel_request", text = text))
  }

  fun submitTestResultToMotor() {
    val test = _state.value.activeTest ?: return
    val result = _state.value.testResolution ?: return
    if (result.reportedToGm || _motorStatus.value.busy) return

    val extendedIndex = test.extendedSceneIndex
    if (extendedIndex != null) {
      val extScene = _state.value.extended.scenes.getOrNull(extendedIndex) ?: return
      if (extScene.successes == null) {
        dispatch(GameEvent.FinalizeExtendedKidSceneFromTest)
        if (_state.value.lastError != null) return
      }
    }

    if (!_motorConfig.value.apiKeyConfigured) {
      markMotorTransportOffline("Resultado pronto, mas a chave Gemini não está configurada para enviar ao Mestre.")
      return
    }
    dispatch(GameEvent.SetTestResultReported(true))
    val extScene = extendedIndex?.let { _state.value.extended.scenes.getOrNull(it) }
    requestMotor(MotorIntent(
      type = if (extendedIndex != null) "extended_test_result" else "test_result",
      successes = result.successes,
      requiredSuccesses = test.requiredSuccesses,
      extendedKid = extScene?.kid,
      extendedSkill = extScene?.skill,
      extendedSceneIndex = extendedIndex,
      bonusEffects = result.bonusEffects.map { it.effectId + (it.target?.let { target -> ":$target" } ?: "") }
    ))
  }

  /**
   * Single entry point for every authoritative response produced by Motor Barbara.
   * Transport/network code must call this method rather than dispatching GameEvents itself.
   */
  fun applyMotorPacket(packet: MotorPacket, wireRejected: List<String> = emptyList()): MotorApplyResult {
    val result = MotorBarbaraFirewall.apply(_state.value, gmPrivateState, packet)
    _state.value = result.gameState
    gmPrivateState = result.privateState
    _motorRoute.value = result.routeHint
    // Persist imported encounter/objective progress through a local whitelist.
    if (result.acceptedActions > 0 && result.rejected.none { it.contains("Perfil mecânico incompatível") }) {
      val instanceId = currentAdventureInstanceId
      val inst = instanceId?.let(adventureRepo::loadInstance)
      val pkg = inst?.let { adventureRepo.getPackage(it.packageId) }
      if (inst != null && pkg != null) {
        val sceneLoc = pkg.locations.firstOrNull { it.name == result.gameState.scene.location }?.id
          ?: inst.currentLocationId
        val current = inst.copy(currentLocationId = sceneLoc,
          discoveredClues = result.gameState.revealed.clues.toList())
        val updated = AdventureRuntimeProgress.apply(current, pkg, packet)
        if (updated != inst) adventureRepo.saveInstance(updated)
      }
    }
    val rejectedCount = result.rejected.size + wireRejected.size
    _motorStatus.value = MotorRuntimeStatus(
      firewallReady = true,
      transportConnected = true,
      busy = false,
      lastAcceptedActions = result.acceptedActions,
      lastRejectedActions = rejectedCount,
      lastMessage = if (rejectedCount == 0) {
        "Resposta do Mestre validada: ${result.acceptedActions} evento(s) aceito(s)."
      } else {
        "Resposta filtrada: ${result.acceptedActions} aceito(s), $rejectedCount rejeitado(s)."
      }
    )
    persistAutosave()
    return result
  }

  fun markMotorTransportOffline(message: String = "Transporte do Mestre indisponível.") {
    _motorStatus.value = _motorStatus.value.copy(transportConnected = false, busy = false, lastMessage = message)
  }

  private fun requestMotor(intent: MotorIntent, retryIncomplete: Boolean = true) {
    if (_motorStatus.value.busy) return
    if (!motorTransport.hasApiKey()) {
      markMotorTransportOffline("Motor Barbara pronto, mas a chave Gemini não está configurada nesta build.")
      return
    }
    _motorStatus.value = _motorStatus.value.copy(transportConnected = true, busy = true, lastMessage = "Mestre pensando…")
    val stateSnapshot = _state.value
    val privateSnapshot = gmPrivateState
    viewModelScope.launch {
      motorTransport.request(intent, stateSnapshot, privateSnapshot)
        .onSuccess { transport ->
          applyMotorPacket(transport.parsed.packet, transport.parsed.rejectedWireActions)
        }
        .onFailure { error ->
          if (intent.type == "test_result" && _state.value.testResolution?.reportedToGm == true) {
            dispatch(GameEvent.SetTestResultReported(false))
          }
          val rawMessage = error.message.orEmpty()
          if (retryIncomplete && rawMessage.contains("respondeu de forma incompleta", ignoreCase = true)) {
            _motorStatus.value = _motorStatus.value.copy(busy = false, lastMessage = "O Mestre está reorganizando a resposta…")
            requestMotor(intent, retryIncomplete = false)
          } else {
            val safeMessage = if (rawMessage.contains('{') || rawMessage.length > 220 || rawMessage.contains("character ", ignoreCase = true)) {
              "O Mestre não conseguiu concluir a resposta. Tente a ação novamente."
            } else rawMessage.ifBlank { "Falha ao acionar o Motor Barbara." }
            markMotorTransportOffline(safeMessage)
            _dataStatus.value = safeMessage
          }
        }
    }
  }

  fun consumeMotorRoute() {
    _motorRoute.value = MotorRouteHint.NONE
  }

  fun submitHelpProposal() {
    val test = _state.value.activeTest ?: return
    if (_state.value.testResolution != null || test.helperKid != null) return
    requestMotor(MotorIntent(type = "help_request", text = "Quero pedir ajuda de um Kid do grupo que possa contribuir de forma plausível com esta ação."))
  }

  fun submitIconicItemProposal() {
    val test = _state.value.activeTest ?: return
    if (_state.value.testResolution != null || test.approvedItemBonus != 0) return
    requestMotor(MotorIntent(type = "item_proposal", text = "Proponho usar meu Item Icônico: ${_state.value.kid.iconicItemName}."))
  }

  fun submitMysteryItemProposal(itemName: String) {
    val test = _state.value.activeTest ?: return
    if (_state.value.testResolution != null || test.approvedItemBonus != 0) return
    val item = _state.value.kid.items.firstOrNull { it.name.equals(itemName.trim(), ignoreCase = true) } ?: return
    if (item.kind != KidItemKind.MYSTERY || !RulesProfiles.validItem(_state.value.rulesProfile,item)) {
      _dataStatus.value = "Este objeto não possui bônus mecânico de Item no perfil ativo."
      return
    }
    requestMotor(MotorIntent(type = "item_proposal", text = "Proponho usar o Item '${item.name}', cujo bônus registrado no estado é +${item.bonus}."))
  }

  fun roll() {
    val current = _state.value
    val count = current.finalPool()
    if (!current.kid.broken && current.activeTest != null && count <= 0) {
      dispatch(GameEvent.RollResult(emptyList()))
      _dataStatus.value = currentStateErrorOr("Pool Tales 0: caso bloqueado pelo firewall Tales/Flood.")
      return
    }
    val dice = List(count) { Random.nextInt(1, 7) }
    dispatch(GameEvent.RollResult(dice))
  }

  private fun currentStateErrorOr(fallback: String): String = _state.value.lastError?.takeIf { it.isNotBlank() } ?: fallback

  fun spendLuck() {
    val result = _state.value.testResolution ?: return
    val rerolled = List(result.dice.count { it != 6 }) { Random.nextInt(1, 7) }
    dispatch(GameEvent.SpendLuck(rerolled))
  }

  fun push(condition: Condition?) {
    val result = _state.value.testResolution ?: return
    val rerolled = List(result.dice.count { it != 6 }) { Random.nextInt(1, 7) }
    dispatch(GameEvent.PushRoll(condition, rerolled))
  }

  fun usePride() = dispatch(GameEvent.UsePride)
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
  fun applyInformationBonus() = dispatch(GameEvent.ApplyInformationBonus)
  fun applyContactItemBonus() = dispatch(GameEvent.ApplyContactItemBonus)
  fun applyLeadershipComfort(condition:Condition) = dispatch(GameEvent.ApplyLeadershipComfort(condition))
  fun purchaseBonusEffect(effectId:String,target:String?) = dispatch(GameEvent.PurchaseBonusEffect(effectId,target))
  fun assignLeadershipDice(kid:String) = dispatch(GameEvent.AssignLeadershipDice(kid,1))
  fun promoteMysteryItemToIconic(itemName:String) = dispatch(GameEvent.PromoteMysteryItemToIconic(itemName))
  fun setXpAnswer(index: Int, yes: Boolean) = dispatch(GameEvent.SetXpAnswer(index, yes))
  fun confirmMysteryEnd() = dispatch(GameEvent.ConfirmMysteryEnd)
  fun improveSkill(skill: String) = dispatch(GameEvent.ImproveSkill(skill))
  fun startExtendedKidScene(): Boolean {
    val current = _state.value
    val index = current.extended.nextUnresolvedIndex ?: return false
    if (current.activeTest?.extendedSceneIndex == index) return true
    dispatch(GameEvent.BeginExtendedKidScene)
    return _state.value.lastError == null && _state.value.activeTest?.extendedSceneIndex == index
  }

  fun acceptExtendedCompromise(marks: List<CompromiseMark>) {
    dispatch(GameEvent.AcceptCompromise(marks))
    if (_state.value.lastError == null) requestMotor(MotorIntent(type = "extended_compromise_accepted"))
  }

  fun declineExtendedCompromise() {
    dispatch(GameEvent.DeclineCompromise)
    if (_state.value.lastError == null) requestMotor(MotorIntent(type = "extended_compromise_declined"))
  }

  fun setBirthdayPassed(passed: Boolean) = dispatch(GameEvent.SetBirthdayPassed(passed))
  fun applyBirthdayAttribute(attribute: String) = dispatch(GameEvent.ApplyBirthdayAttribute(attribute))

  fun saveBetweenMysteryChanges(problem: String, drive: String, pride: String, iconic: String, relationshipsText: String) {
    val relationships = relationshipsText.lineSequence()
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .mapNotNull { line ->
        val separator = when {
          " — " in line -> " — "
          " - " in line -> " - "
          else -> null
        }
        if (separator == null) null else {
          val name = line.substringBefore(separator).trim()
          val description = line.substringAfter(separator).trim()
          if (name.isBlank() || description.isBlank()) null else KidRelationship(name, description)
        }
      }.toList()
    if (relationships.isEmpty()) {
      _dataStatus.value = "Relacionamentos: use uma linha por pessoa no formato Nome — descrição."
      return
    }
    val flood=_state.value.rulesProfile==RulesProfile.FLOOD_TEENS
    dispatch(GameEvent.ApplyBetweenMysteryChanges(
      problem=problem,
      drive=if(flood) null else drive,
      prideText=if(flood) null else pride,
      shameText=if(flood) pride else null,
      iconicItemName=iconic,
      relationships=relationships
    ))
  }

  fun confirmBetweenMysteryReview() = dispatch(GameEvent.ConfirmBetweenMysteryReview)
  fun startNextSession() = dispatch(GameEvent.StartNextSession)


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
    val profile=_state.value.rulesProfile
    val kid=if(profile==RulesProfile.FLOOD_TEENS) RandomTeenFactory.create() else RandomKidFactory.create()
    check(if(profile==RulesProfile.FLOOD_TEENS) RandomTeenFactory.validateCreation(kid) else RandomKidFactory.validateCreation(kid)) { "Factory de personagem gerou ficha fora das regras do perfil ativo." }
    val members = _state.value.group.members.toMutableList()
    if (members.isEmpty()) {
      members += GroupMemberState(kid.name, kid.archetype, kid.portraitAssetKey)
    } else {
      members[0] = members[0].copy(
        name = kid.name,
        archetype = kid.archetype,
        portraitAssetKey = kid.portraitAssetKey,
        conditions = emptySet(),
        broken = false
      )
    }
    _state.value = _state.value.copy(
      kid = kid,
      group = _state.value.group.copy(members = members),
      activeTest = null,
      testResolution = null,
      lastError = null
    )
    persistAutosave()
    _dataStatus.value = if(profile==RulesProfile.FLOOD_TEENS) "Teen aleatório criado pelas regras Flood/Teens: ${kid.name}, ${kid.age} anos, ${kid.archetype}." else "Kid aleatório criado dentro dos limites de criação de Tales/Kids: ${kid.name}, ${kid.age} anos, ${kid.archetype}."
  }


  fun startAdventureFromPackage(pkg: AdventurePackage) {
    if (_motorStatus.value.busy) return
    val targetProfile = if (pkg.isFloodTeens()) RulesProfile.FLOOD_TEENS else RulesProfile.TALES_KIDS
    _state.value = _state.value.copy(rulesProfile = targetProfile)

    val systemId = if (targetProfile == RulesProfile.FLOOD_TEENS) "flood-teens" else "tales-kids"
    val worldId = pkg.worldId.ifBlank { "malaren-islands" }
    val timelineId = "timeline-default"
    val currentKid = _state.value.kid
    val charId = currentKid.id.ifBlank { currentKid.name.lowercase().replace(" ", "-") }

    val identityKey = CharacterIdentityKey(
      systemId = systemId,
      worldId = worldId,
      timelineId = timelineId,
      characterId = charId
    )

    // Load existing character legacy and living world timeline (Scenario A -> B Continuity)
    val existingLegacy = adventureRepo.loadLegacy(identityKey)
    val existingTimeline = adventureRepo.loadTimeline(worldId, timelineId)

    // Merge persistent relationships from previous adventures
    val mergedRelationships = if (existingLegacy != null && existingLegacy.durableRelationships.isNotEmpty()) {
      val relMap = currentKid.relationships.associate { it.name to it.description }.toMutableMap()
      existingLegacy.durableRelationships.forEach { (name, desc) -> relMap[name] = desc }
      relMap.map { KidRelationship(it.key, it.value) }
    } else {
      currentKid.relationships
    }

    val refreshedKid = when (targetProfile) {
      RulesProfile.TALES_KIDS -> currentKid.copy(
        luck = currentKid.maxLuck,
        conditions = emptySet(),
        broken = false,
        prideAvailable = true,
        relationships = mergedRelationships
      )
      RulesProfile.FLOOD_TEENS -> currentKid.copy(
        luck = 0,
        conditions = emptySet(),
        broken = false,
        prideAvailable = false,
        shameAvailable = true,
        scarPending = false,
        relationships = mergedRelationships
      )
    }

    val locName = pkg.locations.firstOrNull { it.id == pkg.start.locationId }?.name ?: pkg.start.locationId
    val startNarrative = listOf(
      "=== ${pkg.title.uppercase()} ===",
      pkg.story.playerIntroduction.ifEmpty { pkg.story.premise },
      "Local inicial: $locName. O que vocês fazem a seguir?"
    )

    val season = existingTimeline?.seasonYear ?: if (targetProfile == RulesProfile.FLOOD_TEENS) "Outono de 1995" else "Outono de 1986"
    val weather = existingTimeline?.currentWeather ?: "Nublado com vento frio"

    _state.value = GameState(
      rulesProfile = targetProfile,
      kid = refreshedKid,
      group = _state.value.group,
      scene = SceneState(
        title = pkg.title,
        chapter = if (targetProfile == RulesProfile.FLOOD_TEENS) "MISTÉRIO IMPORTADO • FLOOD" else "MISTÉRIO IMPORTADO • TALES",
        seasonYear = season,
        location = locName,
        weather = weather,
        time = "16:30",
        daylight = "Crepúsculo",
        visualAssetKey = "approved_scene_outono_loop",
        narrative = startNarrative,
        choices = SceneChoiceDefaults.standard()
      ),
      revealed = RevealedState(
        clues = emptySet(),
        locations = setOf(locName),
        connections = emptySet()
      ),
      routeHistory = listOf(locName)
    )

    // Store GM-only secrets and world timeline knowledge in private state
    val gmFacts = pkg.secrets.filter { it.visibility.equals("gm_only", ignoreCase = true) }
      .associate { it.id to PrivateWorldFact(id = it.id, subject = "secret", value = it.truth, scope = "adventure") }
      .toMutableMap()

    // Structured goals/encounter IDs for the GM; the local engine validates each ID.
    pkg.objectives.forEach { obj ->
      gmFacts["objective-${obj.id}"] = PrivateWorldFact(
        id = "objective-${obj.id}", subject = "active_objective",
        value = "ID=${obj.id}; ${obj.description}; opcional=${obj.optional}", scope = "adventure")
    }
    pkg.encounters.forEach { enc ->
      gmFacts["encounter-${enc.id}"] = PrivateWorldFact(
        id = "encounter-${enc.id}", subject = "available_encounter",
        value = "ID=${enc.id}; local=${enc.locationId}; alternativas=${enc.alternatives.joinToString("; ")}", scope = "adventure")
    }

    // Add facts from existing legacy deeds
    existingLegacy?.confirmedDeeds?.forEach { d ->
      gmFacts["legacy-${d.id}"] = PrivateWorldFact(id = "legacy-${d.id}", subject = "deed", value = d.summary, scope = "world")
    }

    // Add facts from living world timeline events
    existingTimeline?.offSceneEventsJournal?.forEach { e ->
      gmFacts["timeline-${e.id}"] = PrivateWorldFact(id = "timeline-${e.id}", subject = "event", value = e.description, scope = "world")
    }

    val gmLocations = pkg.locations.associate { it.id to PrivateLocation(id = it.id, name = it.name) }
    val gmNpcs = pkg.npcs.associate { npc ->
      val defaultLoc = npc.schedule.firstOrNull()?.locationId ?: ""
      val desc = npc.goals.joinToString("; ")
      npc.id to PrivateNpc(id = npc.id, name = npc.name, role = npc.role, description = desc, location = defaultLoc)
    }
    gmPrivateState = GmPrivateState(
      locations = gmLocations,
      npcs = gmNpcs,
      worldFacts = gmFacts
    )

    // Create and save active adventure playthrough instance
    val instance = adventureRepo.createInstance(pkg, timelineId = timelineId)
    currentAdventureInstanceId = instance.instanceId
    prefs.edit().putString(KEY_ACTIVE_INSTANCE_ID, instance.instanceId).apply()

    // Persist and navigate
    _adventureStarted.value = true
    prefs.edit().putBoolean(KEY_ADVENTURE_STARTED, true).apply()
    _dataStatus.value = "Mistério '${pkg.title}' iniciado com sucesso."
    persistAutosave()
    _motorRoute.value = MotorRouteHint.SCENE
    // Imported mysteries must request their first real Gemini narration too.
    if (motorTransport.hasApiKey()) {
      requestMotor(MotorIntent(type = "adventure_start", text =
        "Abra '${pkg.title}' em '${locName}' para o jogador. Premissa pública: ${pkg.story.playerIntroduction.ifBlank { pkg.story.premise }}. " +
        "Gancho: ${pkg.start.hook}. Produza uma abertura longa, sensorial, três ações e preserve todos os segredos."))
    } else {
      markMotorTransportOffline("Mistério importado iniciado com texto local: configure a chave Gemini para gerar a abertura.")
    }
  }

  fun retryOpeningNarration() {
    if (_motorStatus.value.busy) return
    requestMotor(MotorIntent(type = "adventure_start", text =
      "Gere AGORA a abertura longa do mistério '${_state.value.scene.title}' situado em '${_state.value.scene.location}'. " +
      "Descreva 5 sentidos, NPCs presentes, o clima e termine com três opções sem revelar segredos."))
  }

  suspend fun generateAdventureWithAi(
    profile: RulesProfile,
    onSuccess: (AdventurePackage, Boolean) -> Unit,
    onError: (String) -> Unit = {}
  ) {
    if (!motorTransport.hasApiKey()) {
      val fallbackPkg = AdventureFactory.createProceduralAdventurePackage(profile.name)
      adventureRepo.savePackage(fallbackPkg)
      onSuccess(fallbackPkg, false)
      return
    }

    try {
      val result = motorTransport.generateAdventurePackage(profile)
      if (result.isSuccess) {
        val jsonText = result.getOrThrow()
        val validation = AdventurePackageValidator.parseAndValidate(jsonText)
        if (validation is AdventurePackageValidator.ValidationResult.Valid) {
          adventureRepo.savePackage(validation.adventurePackage)
          onSuccess(validation.adventurePackage, true)
          return
        }
      }
      val fallbackPkg = AdventureFactory.createProceduralAdventurePackage(profile.name)
      adventureRepo.savePackage(fallbackPkg)
      onSuccess(fallbackPkg, false)
    } catch (e: Exception) {
      val fallbackPkg = AdventureFactory.createProceduralAdventurePackage(profile.name)
      adventureRepo.savePackage(fallbackPkg)
      onSuccess(fallbackPkg, false)
    }
  }

  fun concludeCurrentAdventure(summary: String = "Mistério concluído com sucesso") {
    val currentId = currentAdventureInstanceId ?: prefs.getString(KEY_ACTIVE_INSTANCE_ID, null)
    if (currentId != null && adventureRepo.loadInstance(currentId)?.concluded == true) {
      _adventureStarted.value = false
      prefs.edit().putBoolean(KEY_ADVENTURE_STARTED, false).apply()
      _dataStatus.value = "Esse mistério já foi concluído; legado e relógio preservados sem duplicação."
      return
    }
    _adventureStarted.value = false
    prefs.edit().putBoolean(KEY_ADVENTURE_STARTED, false).apply()

    // 1. Update and save AdventureInstance
    val activeInstId = currentAdventureInstanceId ?: prefs.getString(KEY_ACTIVE_INSTANCE_ID, null)
    if (activeInstId != null) {
      val inst = adventureRepo.loadInstance(activeInstId)
      if (inst != null) {
        val concludedInst = inst.copy(
          concluded = true,
          outcomeSummary = summary,
          discoveredClues = _state.value.revealed.clues.toList()
        )
        adventureRepo.saveInstance(concludedInst)
      }
    }

    // 2. Build and save CharacterLegacyRecord with ConfirmedDeed (Idempotent)
    val currentKid = _state.value.kid
    val currentProfileStr = if (_state.value.rulesProfile == RulesProfile.FLOOD_TEENS) "FLOOD_TEENS" else "TALES_KIDS"
    val systemId = if (_state.value.rulesProfile == RulesProfile.FLOOD_TEENS) "flood-teens" else "tales-kids"
    val activeInstance = activeInstId?.let { adventureRepo.loadInstance(it) }
    val worldId = activeInstance?.worldId?.ifBlank { "malaren-islands" } ?: "malaren-islands"
    val timelineId = activeInstance?.timelineId?.ifBlank { "timeline-default" } ?: "timeline-default"
    val charId = currentKid.id.ifBlank { currentKid.name.lowercase().replace(" ", "-") }

    val identityKey = CharacterIdentityKey(
      systemId = systemId,
      worldId = worldId,
      timelineId = timelineId,
      characterId = charId
    )

    val existingLegacy = adventureRepo.loadLegacy(identityKey) ?: CharacterLegacyRecord(
      identityKey = identityKey,
      characterName = currentKid.name,
      profile = currentProfileStr
    )

    // Deterministic IDEMPOTENT deed id
    val deedId = "deed-${activeInstId ?: _state.value.scene.title.hashCode().toUInt().toString(16)}"
    val newDeed = ConfirmedDeed(
      id = deedId,
      adventureId = activeInstId ?: "adventure-completed",
      timestamp = System.currentTimeMillis(),
      locationId = _state.value.scene.location,
      summary = summary,
      witnesses = listOfNotNull(currentKid.anchorName.takeIf { it.isNotBlank() }),
      consequences = listOf("Investigação concluída")
    )

    val existingDeedIndex = existingLegacy.confirmedDeeds.indexOfFirst { it.id == deedId }
    val updatedDeeds = if (existingDeedIndex >= 0) {
      existingLegacy.confirmedDeeds.toMutableList().apply { set(existingDeedIndex, newDeed) }
    } else {
      existingLegacy.confirmedDeeds + newDeed
    }

    val relMap = existingLegacy.durableRelationships.toMutableMap()
    currentKid.relationships.forEach { r ->
      relMap[r.name] = r.description
    }

    val scarList = (existingLegacy.permanentScarsOrMarks + currentKid.scars.map { it.text }).distinct()

    val updatedLegacy = existingLegacy.copy(
      confirmedDeeds = updatedDeeds.distinctBy { it.id },
      completedAdventures = (existingLegacy.completedAdventures + (_state.value.scene.title)).distinct(),
      durableRelationships = relMap,
      permanentScarsOrMarks = scarList,
      lastUpdatedTimestamp = System.currentTimeMillis()
    )
    adventureRepo.saveLegacy(updatedLegacy)

    // Update Living World Timeline (B08)
    val currentTimeline = adventureRepo.loadTimeline(worldId, timelineId) ?: WorldTimelineState(
      worldId = worldId,
      timelineId = timelineId,
      seasonYear = _state.value.scene.weather.ifBlank { "Outono de 1986" },
      currentWeather = _state.value.scene.weather
    )
    val offSceneEvent = OffSceneEvent(
      id = "event-${deedId}",
      day = currentTimeline.elapsedFictionalDays + 1,
      locationId = _state.value.scene.location,
      description = "Conclusão do mistério '${_state.value.scene.title}': $summary",
      involvedNpcIds = listOfNotNull(currentKid.anchorName.takeIf { it.isNotBlank() }),
      worldStateChange = "Mistério ${_state.value.scene.title} finalizado com sucesso.",
      scheduledConsequence = "Repercussão da conclusão no bairro de Björkäsen",
      consequenceTriggerDays = 3,
      consequenceReason = "Resolução dos fatos anômalos no local"
    )
    val updatedTimeline = currentTimeline
      .advanceClock(4)
      .recordOffSceneEvent(offSceneEvent)
    adventureRepo.saveTimeline(updatedTimeline)

    // 3. Sample a life event via lifeEngine
    val outcome = lifeEngine.sampleLifeEvent(
      profile = currentProfileStr,
      turnCount = _state.value.routeHistory.size,
      currentLocation = _state.value.scene.location
    )
    _dataStatus.value = "Mistério encerrado. Legado e linha do tempo do mundo persistidos. Desdobramento de vida: ${outcome.manifestation.description}"
    persistAutosave()
  }

  fun startRandomAdventure() {
    if (_motorStatus.value.busy) return
    val history = prefs.getString(KEY_ADVENTURE_HISTORY, "").orEmpty()
      .split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val profile=_state.value.rulesProfile
    if(profile==RulesProfile.FLOOD_TEENS && _state.value.kid.removedFromPlay) {
      _dataStatus.value="Este Teen deixou o jogo. Crie um novo Teen antes de iniciar outro Mistério."
      return
    }
    val seed = AdventureFactory.create(history)
    val activeSeed = if(profile==RulesProfile.FLOOD_TEENS) seed.copy(seasonYear="Outono, 1995") else seed
    val nextHistory = (history + "${profile.wireName}|${seed.signature}").joinToString("\n")
    prefs.edit().putString(KEY_ADVENTURE_HISTORY, nextHistory).apply()

    val refreshedKid = when(profile) {
      RulesProfile.TALES_KIDS -> _state.value.kid.copy(luck=_state.value.kid.maxLuck,conditions=emptySet(),broken=false,prideAvailable=true)
      RulesProfile.FLOOD_TEENS -> _state.value.kid.copy(luck=0,conditions=emptySet(),broken=false,prideAvailable=false,shameAvailable=true,scarPending=false)
    }
    val refreshedMembers = _state.value.group.members.map { member ->
      if (member.name.equals(_state.value.kid.name, ignoreCase = true)) {
        member.copy(
          name = refreshedKid.name,
          archetype = refreshedKid.archetype,
          portraitAssetKey = refreshedKid.portraitAssetKey,
          conditions = emptySet(),
          broken = false
        )
      } else member.copy(conditions = emptySet(), broken = false)
    }
    _state.value = GameState(
      rulesProfile = profile,
      kid = refreshedKid,
      group = _state.value.group.copy(members = refreshedMembers),
      scene = SceneState(
        title = activeSeed.title,
        chapter = if(profile==RulesProfile.FLOOD_TEENS) "NOVO MISTÉRIO • FLOOD" else "NOVA AVENTURA",
        seasonYear = activeSeed.seasonYear,
        location = activeSeed.location,
        weather = activeSeed.weather,
        time = activeSeed.time,
        daylight = activeSeed.daylight,
        visualAssetKey = activeSeed.visualAsset,
        narrative = activeSeed.localOpeningNarrative(),
        choices = SceneChoiceDefaults.standard()
      ),
      revealed = RevealedState(clues = emptySet(), locations = setOf(activeSeed.location), connections = emptySet()),
      routeHistory = listOf(activeSeed.location)
    )
    gmPrivateState = GmPrivateState()

    // The player's explicit tap is the authoritative start. Gemini enriches the opening,
    // but a network/model failure can no longer trap the user on MENU.
    _adventureStarted.value = true
    prefs.edit().putBoolean(KEY_ADVENTURE_STARTED, true).apply()
    _dataStatus.value = if (motorTransport.hasApiKey()) {
      "Aventura criada. O Mestre está enriquecendo a cena inicial…"
    } else {
      "Aventura criada em modo local. Configure a chave Gemini para o Mestre continuar a narrar."
    }
    persistAutosave()
    _motorRoute.value = MotorRouteHint.SCENE

    if (motorTransport.hasApiKey()) {
      requestMotor(MotorIntent(type = "adventure_start", text = activeSeed.motorBrief()))
    } else {
      markMotorTransportOffline("Aventura iniciada; Motor Barbara offline até uma chave Gemini ser configurada.")
    }
  }

  fun saveMotorConfig(apiKey: String?, model: String, ttsModel: String, ttsVoice: String, ttsMode: String) {
    motorTransport.saveConfig(apiKey, model, ttsModel, ttsVoice, ttsMode)
    _motorConfig.value = motorTransport.currentConfig()
    _motorStatus.value = _motorStatus.value.copy(
      transportConnected = false,
      busy = false,
      lastMessage = if (_motorConfig.value.apiKeyConfigured) {
        "Configuração salva. A próxima ação validará a conexão com o Mestre."
      } else {
        "Modelo salvo, mas ainda falta configurar a chave Gemini."
      }
    )
  }

  fun clearMotorLocalApiKey() {
    motorTransport.clearLocalApiKey()
    _motorConfig.value = motorTransport.currentConfig()
    _motorStatus.value = _motorStatus.value.copy(
      transportConnected = false,
      busy = false,
      lastMessage = if (_motorConfig.value.apiKeyConfigured) {
        "Override local removido; usando a chave injetada pela build."
      } else {
        "Override local removido; nenhuma chave Gemini disponível nesta build."
      }
    )
  }

  fun exportCampaignBackup(uri: Uri) {
    viewModelScope.launch {
      runCatching { portableBackup.write(uri, _state.value, gmPrivateState) }
        .onSuccess { _dataStatus.value = "Backup portátil exportado e verificado." }
        .onFailure { _dataStatus.value = "Falha no backup: ${it.message}" }
    }
  }

  fun importCampaignBackup(uri: Uri) {
    viewModelScope.launch {
      runCatching { portableBackup.read(uri) }
        .onSuccess { payload ->
          _state.value = payload.game
          gmPrivateState = payload.gmPrivate
          _adventureStarted.value = true
          prefs.edit().putBoolean(KEY_ADVENTURE_STARTED, true).apply()
          persistAutosave()
          _dataStatus.value = "Backup restaurado com estado público + privado do Mestre."
        }
        .onFailure { _dataStatus.value = "Backup rejeitado: ${it.message}" }
    }
  }

  fun configureAutomaticBackup(treeUri: Uri) {
    viewModelScope.launch {
      runCatching {
        portableBackup.configureAutomaticDirectory(treeUri)
        portableBackup.createAutomaticBackup(_state.value, gmPrivateState)
      }.onSuccess { name -> _dataStatus.value = "Backup automático configurado${name?.let { ": $it" } ?: "."}" }
        .onFailure { _dataStatus.value = "Falha ao configurar backup automático: ${it.message}" }
    }
  }

  fun automaticBackupConfigured(): Boolean = portableBackup.automaticDirectory() != null

  fun setKidPortrait(uri: Uri) {
    viewModelScope.launch {
      runCatching {
        val resolver = getApplication<Application>().contentResolver
        val original = resolver.openInputStream(uri).use { input ->
          requireNotNull(input) { "Imagem não pôde ser aberta." }
          requireNotNull(BitmapFactory.decodeStream(input)) { "Formato de imagem inválido." }
        }
        val maxSide = 512
        val scale = minOf(1f, maxSide.toFloat() / maxOf(original.width, original.height).toFloat())
        val normalized = if (scale < 1f) {
          Bitmap.createScaledBitmap(original, (original.width * scale).toInt().coerceAtLeast(1), (original.height * scale).toInt().coerceAtLeast(1), true)
        } else original
        val bytes = ByteArrayOutputStream().use { out ->
          require(normalized.compress(Bitmap.CompressFormat.JPEG, 86, out)) { "Falha ao preparar retrato." }
          out.toByteArray()
        }
        require(bytes.size <= 1_000_000) { "Imagem do retrato ficou grande demais." }
        Base64.encodeToString(bytes, Base64.NO_WRAP)
      }.onSuccess { encoded ->
        _state.value = _state.value.copy(kid = _state.value.kid.copy(portraitImageBase64 = encoded))
        persistAutosave()
        _dataStatus.value = "Retrato do Kid atualizado."
      }.onFailure { _dataStatus.value = "Falha ao trocar retrato: ${it.message}" }
    }
  }

  fun exportKid(uri: Uri) {
    viewModelScope.launch {
      runCatching {
        val text = KidJsonCodec.encode(_state.value.rulesProfile,_state.value.kid)
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8).use { out ->
          requireNotNull(out) { "Destino da ficha indisponível." }
          out.write(text); out.flush()
        }
      }.onSuccess { _dataStatus.value = "Ficha do Kid exportada em JSON." }
        .onFailure { _dataStatus.value = "Falha ao exportar ficha: ${it.message}" }
    }
  }

  fun importKid(uri: Uri) {
    viewModelScope.launch {
      runCatching {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { input ->
          requireNotNull(input) { "Ficha não pôde ser aberta." }
          input.readText()
        }
        KidJsonCodec.decodeForProfile(text,_state.value.rulesProfile)
      }.onSuccess { kid ->
        val members = _state.value.group.members.toMutableList()
        if (members.isNotEmpty()) members[0] = members[0].copy(
          name = kid.name, archetype = kid.archetype, portraitAssetKey = kid.portraitAssetKey,
          conditions = kid.conditions, broken = kid.broken
        )
        _state.value = _state.value.copy(kid = kid, group = _state.value.group.copy(members = members), activeTest = null, testResolution = null, lastError = null)
        persistAutosave()
        _dataStatus.value = "Ficha ${kid.name} importada e validada para ${_state.value.rulesProfile.wireName}."
      }.onFailure { _dataStatus.value = "Ficha rejeitada: ${it.message}" }
    }
  }

  fun importManuals(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _manualLibraryStatus.value = _manualLibraryStatus.value.copy(busy = true, progress = "Preparando indexação…", lastError = null)
      var failure: Throwable? = null
      for (uri in uris) {
        runCatching {
          manualLibrary.importAndIndex(uri) { message ->
            _manualLibraryStatus.value = _manualLibraryStatus.value.copy(busy = true, progress = message)
          }
        }.onFailure { failure = it; return@onFailure }
        if (failure != null) break
      }
      _manualLibraryStatus.value = ManualLibraryStatus(
        manuals = manualLibrary.listManuals(), busy = false,
        progress = if (failure == null) "Indexação concluída. PDFs permanecem locais no aparelho." else "Indexação interrompida.",
        results = _manualLibraryStatus.value.results,
        lastError = failure?.message
      )
    }
  }

  fun searchManuals(query: String) {
    viewModelScope.launch {
      val hits = runCatching { manualLibrary.search(query, 12) }.getOrElse { emptyList() }
      _manualLibraryStatus.value = _manualLibraryStatus.value.copy(results = hits, progress = if (query.isBlank()) "Digite uma busca." else "${hits.size} resultado(s) locais.")
    }
  }

  fun removeManual(id: String) {
    manualLibrary.removeManual(id)
    _manualLibraryStatus.value = _manualLibraryStatus.value.copy(manuals = manualLibrary.listManuals(), results = _manualLibraryStatus.value.results.filterNot { it.manualId == id })
  }

  fun consumeSharedSceneImage(token: String): Boolean {
    return runCatching {
      val uri = SceneImageExchange.acceptImportedToken(getApplication(), token, _state.value)
      dispatch(GameEvent.SetSceneImportedImage(uri))
      _dataStatus.value = "Imagem vinculada à cena atual."
      true
    }.getOrElse {
      _dataStatus.value = "Imagem não vinculada: ${it.message}"
      false
    }
  }

  fun saveNow() {
    prefs.edit()
      .putString(KEY_MANUAL_SAVE, GameSnapshotCodec.encode(_state.value))
      .putString(KEY_GM_MANUAL_SAVE, GmPrivateSnapshotCodec.encode(gmPrivateState))
      .putBoolean(KEY_MANUAL_SAVE_ADVENTURE, _adventureStarted.value)
      .apply()
    _hasManualSave.value = true
  }

  fun loadManualSave(): Boolean {
    val text = prefs.getString(KEY_MANUAL_SAVE, null) ?: prefs.getString(KEY_MANUAL_SAVE_V1, null) ?: return false
    val restored = GameSnapshotCodec.decode(text) ?: return false
    val restoredPrivate = prefs.getString(KEY_GM_MANUAL_SAVE, null)?.let(GmPrivateSnapshotCodec::decode) ?: GmPrivateState()
    _state.value = restored
    gmPrivateState = restoredPrivate
    val restoredAdventureStarted = prefs.getBoolean(KEY_MANUAL_SAVE_ADVENTURE, true)
    _adventureStarted.value = restoredAdventureStarted
    prefs.edit().putBoolean(KEY_ADVENTURE_STARTED, restoredAdventureStarted).apply()
    persistAutosave()
    return true
  }

  private fun persistAutosave() {
    prefs.edit()
      .putString(KEY_AUTOSAVE, GameSnapshotCodec.encode(_state.value))
      .putString(KEY_GM_AUTOSAVE, GmPrivateSnapshotCodec.encode(gmPrivateState))
      .apply()
    if (portableBackup.shouldAutoBackup()) {
      val game = _state.value
      val gm = gmPrivateState
      viewModelScope.launch {
        runCatching { portableBackup.createAutomaticBackup(game, gm) }
          .onSuccess { name -> if (name != null) _dataStatus.value = "Autosave local + backup portátil: $name" }
          .onFailure { _dataStatus.value = "Autosave local OK; backup portátil falhou: ${it.message}" }
      }
    }
  }

  companion object {
    private const val KEY_AUTOSAVE = "autosave_v2"
    private const val KEY_MANUAL_SAVE = "manual_save_v2"
    private const val KEY_GM_AUTOSAVE = "gm_autosave_v1"
    private const val KEY_GM_MANUAL_SAVE = "gm_manual_save_v1"
    private const val KEY_AUTOSAVE_V1 = "autosave_v1"
    private const val KEY_MANUAL_SAVE_V1 = "manual_save_v1"
    private const val KEY_MANUAL_SAVE_ADVENTURE = "manual_save_adventure_v1"
    private const val KEY_ADVENTURE_STARTED = "adventure_started_v1"
    private const val KEY_ADVENTURE_HISTORY = "adventure_history_v1"
    private const val KEY_ACTIVE_INSTANCE_ID = "active_instance_id_v1"
  }
}