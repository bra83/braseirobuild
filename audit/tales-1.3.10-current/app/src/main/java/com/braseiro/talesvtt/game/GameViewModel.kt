package com.braseiro.talesvtt.game

import android.app.Application
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.braseiro.talesvtt.data.KidJsonCodec
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
      extendedSceneIndex = extendedIndex
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
  fun startNextSession() = dispatch(GameEvent.StartNextSession)


  fun createRandomKid() {
    val kid = RandomKidFactory.create()
    check(RandomKidFactory.validateCreation(kid)) { "Factory de Kid gerou ficha fora das regras." }
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
    _dataStatus.value = "Kid aleatório criado dentro dos limites de criação de Tales/Kids: ${kid.name}, ${kid.age} anos, ${kid.archetype}."
  }

  fun startRandomAdventure() {
    if (_motorStatus.value.busy) return
    val history = prefs.getString(KEY_ADVENTURE_HISTORY, "").orEmpty()
      .split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val seed = AdventureFactory.create(history)
    val nextHistory = (history + seed.signature).joinToString("\n")
    prefs.edit().putString(KEY_ADVENTURE_HISTORY, nextHistory).apply()

    val refreshedKid = _state.value.kid.copy(
      luck = _state.value.kid.maxLuck,
      conditions = emptySet(),
      broken = false,
      prideAvailable = true
    )
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
      kid = refreshedKid,
      group = _state.value.group.copy(members = refreshedMembers),
      scene = SceneState(
        title = seed.title,
        chapter = "NOVA AVENTURA",
        seasonYear = seed.seasonYear,
        location = seed.location,
        weather = seed.weather,
        time = seed.time,
        daylight = seed.daylight,
        visualAssetKey = seed.visualAsset,
        narrative = seed.localOpeningNarrative(),
        choices = SceneChoiceDefaults.standard()
      ),
      revealed = RevealedState(clues = emptySet(), locations = setOf(seed.location), connections = emptySet())
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
      requestMotor(MotorIntent(type = "adventure_start", text = seed.motorBrief()))
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
        val text = KidJsonCodec.encode(_state.value.kid)
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
        KidJsonCodec.decode(text)
      }.onSuccess { kid ->
        val members = _state.value.group.members.toMutableList()
        if (members.isNotEmpty()) members[0] = members[0].copy(
          name = kid.name, archetype = kid.archetype, portraitAssetKey = kid.portraitAssetKey,
          conditions = kid.conditions, broken = kid.broken
        )
        _state.value = _state.value.copy(kid = kid, group = _state.value.group.copy(members = members), activeTest = null, testResolution = null, lastError = null)
        persistAutosave()
        _dataStatus.value = "Ficha ${kid.name} importada e validada."
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
  }
}
