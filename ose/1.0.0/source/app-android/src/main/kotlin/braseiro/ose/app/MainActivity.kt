package braseiro.ose.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.room.Room
import androidx.webkit.WebViewAssetLoader
import braseiro.ose.backup.BackupCodec
import braseiro.ose.character.NativeCharacterCreationCoordinator
import braseiro.ose.character.NativeCharacterLoadoutCoordinator
import braseiro.ose.character.CharacterPartyAdministration
import braseiro.ose.character.CharacterControlCoordinator
import braseiro.ose.character.CompanionJudgmentEngine
import braseiro.ose.rag.RuleEvidenceRegistry
import braseiro.ose.model.*
import braseiro.ose.dungeon.DungeonGeneratorV1
import braseiro.ose.hex.GeographicCoherenceValidator
import braseiro.ose.hex.HexKnowledge
import braseiro.ose.hex.HexWorldGeneratorV1
import braseiro.ose.npc.NpcDomain
import braseiro.ose.settlement.SettlementPackages
import braseiro.ose.persistence.api.*
import braseiro.ose.persistence.room.*
import braseiro.ose.referee.RulesRefereeBoundary
import braseiro.ose.rules.api.AttributeAdjustment
import braseiro.ose.rules.api.CharacterCreationResult
import braseiro.ose.rules.api.CreationMethod
import braseiro.ose.rules.api.RuleEvidence
import braseiro.ose.rules.api.RuleTrace
import braseiro.ose.rules.shared.StartingEquipmentCatalog
import braseiro.ose.session.SessionEngine
import braseiro.ose.session.ActionAuthorityPolicy
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    companion object {
        const val LOCAL_APP_ORIGIN = "https://appassets.androidplatform.net/assets/"
        const val START_URL = "${LOCAL_APP_ORIGIN}index.html?screen=session&state=active"
        const val REQUEST_EXPORT_CHARACTER = 4101
        const val REQUEST_IMPORT_CHARACTER = 4102
        const val REQUEST_IMPORT_ADVENTURE = 4103
        val CAMPAIGN_ID = CampaignId("braseiro-ose-primary")
    }

    private lateinit var webView: WebView
    private lateinit var db: BraseiroOseDatabase
    private lateinit var repository: RoomCampaignRepository
    private lateinit var session: SessionEngine
    private lateinit var credentials: GeminiCredentialStore
    private lateinit var runtimeSettings: GeminiRuntimeSettings
    private lateinit var mediaStore: PresentationMediaStore
    private lateinit var gemini: GeminiInteractionsClient
    private lateinit var tts: braseiro.ose.tts.android.AndroidTtsController
    private lateinit var geminiTts: GeminiTtsController
    private lateinit var geminiSupervisor: GeminiBarbaraSupervisor
    private lateinit var adventureStore: BraseiroAdventureStore
    private var playerGuideMode = OSEPlayerGuide.GuideMode.LEARN_PLAYING
    private val manualDice = SecureRandom()
    private val evidenceRegistry = RuleEvidenceRegistry()
    private val creationCoordinator = NativeCharacterCreationCoordinator()
    private val loadoutCoordinator = NativeCharacterLoadoutCoordinator()
    private val io = Executors.newSingleThreadExecutor()
    @Volatile private var pendingCharacterExport: Pair<String, String>? = null

    @Volatile
    var lastBridgeMessage: BridgeEnvelopeMetadata? = null
        private set

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(
            applicationContext,
            BraseiroOseDatabase::class.java,
            "braseiro-ose.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        repository = RoomCampaignRepository(db)
        val guidePrefs = getSharedPreferences("braseiro_ose_player_guide_v1", MODE_PRIVATE)
        val savedGuideMode = guidePrefs.getString("guide_mode", null)
        playerGuideMode = if (savedGuideMode != null) OSEPlayerGuide.GuideMode.fromString(savedGuideMode) else OSEPlayerGuide.GuideMode.LEARN_PLAYING
        credentials = GeminiCredentialStore(applicationContext)
        runtimeSettings = GeminiRuntimeSettings(applicationContext)
        mediaStore = PresentationMediaStore(applicationContext)
        gemini = GeminiInteractionsClient()
        adventureStore = BraseiroAdventureStore(db)
        geminiTts = GeminiTtsController(applicationContext, credentials, runtimeSettings, gemini)
        geminiTts.onState = { res, err ->
            if (tts.mode() == braseiro.ose.tts.android.AndroidTtsController.Mode.GEMINI_TTS) {
                sendTtsState(if (res == GeminiTtsController.Result.STARTING || res == GeminiTtsController.Result.REPLAYING) "PLAYING" else "IDLE", err)
            }
        }
        geminiSupervisor = GeminiBarbaraSupervisor(credentials, runtimeSettings, gemini) { adventureStore.masterContextPrompt(CAMPAIGN_ID.value) }
        session = SessionEngine(repository, RulesRefereeBoundary(), geminiSupervisor)
        tts = braseiro.ose.tts.android.AndroidTtsController(applicationContext)
        tts.onState = { state, error -> sendTtsState(state.name, error) }
        tts.geminiSpeaker = { text ->
            when (val res = geminiTts.playVisibleNarration(text)) {
                GeminiTtsController.Result.MISSING_KEY -> error("GEMINI_TTS_MISSING_KEY")
                GeminiTtsController.Result.ERROR -> error("GEMINI_TTS_ERROR")
                GeminiTtsController.Result.EMPTY_NARRATION -> error("EMPTY_VISIBLE_NARRATION")
                else -> Unit
            }
        }
        tts.geminiStopper = { geminiTts.stop() }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/media/", mediaStore)
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(BridgeEndpoint(), "BraseiroBridge")
        }
        val root = FrameLayout(this)
        root.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
        root.setOnApplyWindowInsetsListener { _, insets ->
            val lp = webView.layoutParams as FrameLayout.LayoutParams
            lp.topMargin = insets.systemWindowInsetTop
            lp.bottomMargin = insets.systemWindowInsetBottom
            lp.leftMargin = insets.systemWindowInsetLeft
            lp.rightMargin = insets.systemWindowInsetRight
            webView.layoutParams = lp
            insets
        }
        root.requestApplyInsets()

        io.execute {
            bootstrapIfMissing()
            saveSharedSceneImage(intent, notifyUi = false)
            runOnUiThread {
                if (!isFinishing && !isDestroyed) webView.loadUrl(START_URL)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        io.execute { saveSharedSceneImage(intent, notifyUi = true) }
    }

    @Deprecated("Deprecated in Android API; retained for document picker compatibility on minSdk 26")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_EXPORT_CHARACTER -> {
                val pending = pendingCharacterExport
                pendingCharacterExport = null
                if (resultCode != RESULT_OK || data?.data == null || pending == null) {
                    send(JSONObject().put("type","CharacterSheetExportResult").put("payload",JSONObject().put("ok",false).put("cancelled",true)))
                    return
                }
                runCatching {
                    contentResolver.openOutputStream(data.data!!, "w")!!.use { it.write(pending.second.toByteArray(Charsets.UTF_8)) }
                }.onSuccess {
                    send(JSONObject().put("type","CharacterSheetExportResult").put("payload",JSONObject().put("ok",true).put("fileName",pending.first)))
                }.onFailure { t ->
                    send(JSONObject().put("type","BridgeError").put("payload",JSONObject().put("message",safeError(t))))
                }
            }
            REQUEST_IMPORT_CHARACTER -> {
                if (resultCode != RESULT_OK || data?.data == null) {
                    send(JSONObject().put("type","CharacterSheetImportResult").put("payload",JSONObject().put("ok",false).put("cancelled",true)))
                    return
                }
                val uri = data.data!!
                io.execute {
                    try {
                        val raw = contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val current = session.loadSession(CAMPAIGN_ID)
                        val imported = importCharacterSheet(current, raw)
                        repository.commit(CAMPAIGN_ID, StateTransition("character-import-${System.currentTimeMillis()}", imported.campaignState))
                        val persisted = session.loadSession(CAMPAIGN_ID)
                        storeSelectedCharacterId(persisted, imported.campaignState.party.characterIds.last())
                        send(JSONObject().put("type","CharacterSheetImportResult").put("payload",JSONObject().put("ok",true).put("viewState",projectForUi(persisted))))
                    } catch (t: Throwable) {
                        send(JSONObject().put("type","BridgeError").put("payload",JSONObject().put("message",safeError(t))))
                    }
                }
            }
            REQUEST_IMPORT_ADVENTURE -> {
                if (resultCode != RESULT_OK || data?.data == null) {
                    send(JSONObject().put("type","AdventureImportResult").put("payload",JSONObject().put("ok",false).put("cancelled",true)))
                    return
                }
                val uri = data.data!!
                io.execute {
                    try {
                        val raw = contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val current = session.loadSession(CAMPAIGN_ID)
                        val res = adventureStore.importJson(raw, scope = current.campaignId.value, expectedWorldId = null)
                        val startLocationId = res.optString("startLocationId")
                        val advWorldId = res.optString("worldId", current.campaignId.value)
                        if (startLocationId.isNotBlank()) {
                            val nextPos = SpatialRef.Scene(advWorldId, startLocationId)
                            val updatedState = current.campaignState.copy(position = current.campaignState.position.copy(primary = nextPos))
                            repository.commit(CAMPAIGN_ID, StateTransition("adventure-start-pos:$startLocationId", updatedState))
                        }
                        val persisted = session.loadSession(CAMPAIGN_ID)
                        send(JSONObject().put("type","AdventureImportResult").put("payload",res.put("viewState",projectForUi(persisted))))
                    } catch (t: Throwable) {
                        send(JSONObject().put("type","BridgeError").put("payload",JSONObject().put("message",safeError(t))))
                    }
                }
            }
        }
    }

    private fun bootstrapIfMissing() {
        when (val loaded = repository.load(CAMPAIGN_ID)) {
            is CampaignLoadResult.NotFound -> {
                val bootstrapOptions = OptionSet(listOf("OPT.MORALE", "OPT.ENCUMBRANCE=BASIC"))
                check(braseiro.ose.rules.api.OptionPolicyContract.resolve(RuleProfile.OSE_ADVANCED_FANTASY, bootstrapOptions).valid)
                val initSeed = kotlin.random.Random.nextLong().toULong()
                val envelope = CampaignEnvelope(
                    campaignId = CAMPAIGN_ID,
                    createdAtMetadata = "bootstrap",
                    ruleProfile = RuleProfile.OSE_ADVANCED_FANTASY,
                    authorityRevision = AuthorityRevision(
                        "OSE_ADVANCED_INTERIM_CANONICAL",
                        "Referee-v1.2+Player",
                        "63ea8b2cbcb32b671a70118862a2ecd73c5f670a031914da0e94e6955acf57a1"
                    ),
                    optionSet = bootstrapOptions,
                    generatorRegistry = GeneratorRegistry(listOf(GeneratorVersion("DUNGEON_GEN", 1), GeneratorVersion("HEX_WORLD_GEN", 1))),
                    assetManifestRevision = AssetManifestRevision(
                        "BRASEIRO_VISUAL_FORGE_ASSET_MANIFEST_V2", "2026-08-31",
                        "769fa3b9893dc8a4108646783740a0caaa9971c6f90fd44d6e9309792e7301e4"
                    ),
                    campaignState = buildWorldBootstrap(RuleProfile.OSE_ADVANCED_FANTASY, initSeed)
                )
                repository.create(envelope)
            }
            is CampaignLoadResult.Loaded -> {
                var repaired = ensureWorldBootstrap(loaded.envelope)
                repaired = repairCanonicalResources(repaired)
                repaired = repairLegacyPresentation(repaired)
                if (repaired != loaded.envelope) {
                    repository.commit(CAMPAIGN_ID, StateTransition("runtime-repair-v2", repaired.campaignState))
                }
            }
            is CampaignLoadResult.ReadFailure -> throw loaded.cause
        }
    }

    private fun buildWorldBootstrap(profile: RuleProfile, customSeed: ULong? = null): CampaignState {
        val worldSeed = customSeed ?: 0x0B5E1AUL
        val dungeonSeed = if (customSeed != null) (customSeed xor 0x0D06E0UL) else 0x0D06E0UL
        val generated = HexWorldGeneratorV1.generate("world-1", worldSeed, profile)
        val coherence = GeographicCoherenceValidator.validate(generated, profile)
        check(coherence.pass) { "HEX_BOOTSTRAP_INVALID:${coherence.errors.joinToString()}" }
        val startCell = generated.cells.firstOrNull { it.poiId != null }
            ?: generated.cells.firstOrNull { it.terrain.name == "PLAINS" || it.terrain.name == "CLEAR" }
            ?: generated.cells.firstOrNull { it.q == 3 && it.r == 2 }
            ?: generated.cells.first()
        val startQ = startCell.q
        val startR = startCell.r
        val world = HexKnowledge.reveal(generated, startQ, startR, 0)
        val dungeon = DungeonGeneratorV1.generate("DUNGEON_001", dungeonSeed, 12)
        check(DungeonGeneratorV1.isReachable(dungeon)) { "DUNGEON_BOOTSTRAP_UNREACHABLE" }
        val settlement = SettlementPackages.canonicalVillage()
        val npcs = listOf(
            NpcDomain.createStable("NPC_INNKEEPER", "Estalajadeiro", SpatialRef.Settlement("SETTLEMENT_001", "INN"), setOf("RUMOR_DUNGEON_001")),
            NpcDomain.createStable("NPC_SMITH", "Ferreiro", SpatialRef.Settlement("SETTLEMENT_001", "SMITH"), setOf("SERVICE_SMITH")),
            NpcDomain.createStable("NPC_PRIEST", "Sacerdote", SpatialRef.Settlement("SETTLEMENT_001", "TEMPLE"), setOf("SERVICE_TEMPLE"))
        ).associateBy { it.npcId }
        val events = listOf(
            WorldEventSnapshot("WORLD_EVENT_RUMOR_001", 6, "RUMOR", "SETTLEMENT_001", "RUMOR_DUNGEON_001"),
            WorldEventSnapshot("WORLD_EVENT_FACTION_001", 18, "NPC_INTENT", "NPC_INNKEEPER", "UPDATE_RUMOR")
        )
        return CampaignState(
            party = PartyState(),
            time = TimeState(turns = 0),
            position = PositionState(SpatialRef.Hex("world-1", startQ, startR)),
            resources = ResourceState(),
            playerKnowledge = PlayerKnowledgeState(listOf("hex:$startQ,$startR")),
            game = GameExtensions(
                session = SessionSnapshot(phase = "ACTIVE"),
                hexWorld = world, dungeon = dungeon, settlement = settlement, npcs = npcs, worldEvents = events,
                rngRootSeed = "42"
            ).canonical()
        )
    }

    private fun ensureWorldBootstrap(envelope: CampaignEnvelope): CampaignEnvelope {
        val game = envelope.campaignState.game
        if (game.hexWorld != null && game.dungeon != null && game.settlement != null && game.npcs.isNotEmpty()) return envelope
        val fresh = buildWorldBootstrap(envelope.ruleProfile)
        val repairedGame = game.copy(
            hexWorld = game.hexWorld ?: fresh.game.hexWorld,
            dungeon = game.dungeon ?: fresh.game.dungeon,
            settlement = game.settlement ?: fresh.game.settlement,
            npcs = if (game.npcs.isEmpty()) fresh.game.npcs else game.npcs,
            worldEvents = if (game.worldEvents.isEmpty()) fresh.game.worldEvents else game.worldEvents,
            revision = game.revision + 1
        ).canonical()
        val knowledge = if (envelope.campaignState.playerKnowledge.knownFactIds.isEmpty()) fresh.playerKnowledge else envelope.campaignState.playerKnowledge
        return envelope.copy(campaignState = envelope.campaignState.copy(game = repairedGame, playerKnowledge = knowledge))
    }

    private fun repairCanonicalResources(envelope: CampaignEnvelope): CampaignEnvelope {
        val game = envelope.campaignState.game
        val items = game.inventory.items.values.filter { it.quantity > 0 && it.ownerId in envelope.campaignState.party.characterIds }
        val rations = items.filter { it.kind == InventoryItemKind.FOOD }.sumOf { it.quantity }
        val torches = items.filter { it.lightSourceKind == LightSourceKind.TORCH }.sumOf { it.quantity }
        val gold = items.filter { it.kind == InventoryItemKind.COIN }.sumOf { it.quantity }
        val old = envelope.campaignState.resources
        val preserved = old.resourceFacts.filterNot {
            it.startsWith("rations:") || it.startsWith("ration:") || it.startsWith("torch:") ||
                it.startsWith("gold:") || it.startsWith("movement:") ||
                it.startsWith("ration_deficit:") || it.startsWith("foodless_days:") ||
                it.startsWith("last_rations_consumed:")
        }
        val repaired = ResourceState(preserved + listOf(
            "rations:$rations",
            "torch:$torches",
            "gold:$gold",
            "movement:${game.expedition.baseMoveMeters}",
            "ration_deficit:${game.expedition.lastRationDeficit}",
            "foodless_days:${game.expedition.foodlessDays}"
        )).canonical()
        return if (repaired == old) envelope else envelope.copy(campaignState = envelope.campaignState.copy(resources = repaired))
    }

    private fun repairLegacyPresentation(envelope: CampaignEnvelope): CampaignEnvelope {
        val game = envelope.campaignState.game
        val old = game.session
        val technical = isTechnicalNarration(old.visibleNarration)
        if (!technical) return envelope
        val cleanedReceipts = old.reactionReceipts.mapValues { (_, receipt) ->
            if (isTechnicalNarration(receipt.narration.orEmpty())) receipt.copy(narration = null) else receipt
        }
        val cleaned = old.copy(
            visibleNarration = "",
            visibleMechanicalFeedback = "",
            journal = old.journal.filterNot(::isTechnicalNarration),
            reactionReceipts = cleanedReceipts,
            pendingNarrationReactionId = null
        )
        val repairedGame = game.copy(session = cleaned, revision = game.revision + 1)
        return envelope.copy(campaignState = envelope.campaignState.copy(game = repairedGame))
    }

    private fun isTechnicalNarration(text: String): Boolean {
        val t = text.uppercase()
        return text.startsWith("Mestre: o grupo declarou:", ignoreCase = true) ||
            listOf("PLAYER_REACTION", "WILD_TRAVEL", "WILD_REST", "RULES_DICE", "CHARACTER_LOADOUT", "POSIÇÃO CANÔNICA", "WORLD-1").any { it in t }
    }

    fun webViewForTest(): WebView = webView

    private inner class BridgeEndpoint {
        @JavascriptInterface
        fun postMessage(raw: String) {
            io.execute {
                try {
                    val meta = BridgeContractValidator.validate(raw)
                    lastBridgeMessage = meta
                    val obj = JSONObject(raw)
                    val payload = obj.getJSONObject("payload")
                    when (meta.type) {
                        "PlayerReaction" -> {
                            val reactionId = payload.optString("reactionId", "").trim()
                            require(reactionId.isNotBlank()) { "PlayerReaction.reactionId is required" }
                            val text = payload.optString("text", "").trim()
                            val mechanicalIntent = payload.optString("mechanicalIntent", "").trim().ifBlank { text }
                            val currentBefore = session.loadSession(CAMPAIGN_ID)
                            val actorId = payload.optString("characterId", "").trim()
                                .ifBlank { selectedCharacterId(currentBefore) ?: error("NO_CHARACTER") }
                            val result = session.submitCharacterAction(
                                CAMPAIGN_ID, reactionId, actorId, text, mechanicalIntent
                            )
                            val current = session.loadSession(CAMPAIGN_ID)
                            val receipt = current.campaignState.game.session.reactionReceipts[reactionId]
                            val diceRollsList = receipt?.diceRolls?.map { d ->
                                JSONObject().put("sides", d.sides).put("value", d.value).put("label", d.label ?: JSONObject.NULL)
                            } ?: emptyList<JSONObject>()
                            val diceRollsJson = JSONArray(diceRollsList)
                            send(
                                JSONObject().put("type", "SessionUpdate").put("payload", JSONObject()
                                    .put("narration", result.narration)
                                    .put("feedback", result.feedback)
                                    .put("committed", true)
                                    .put("mechanicalMutation", result.mechanicalMutation)
                                    .put("ruleTrace", traceJson(result.trace))
                                    .put("stateHash", CanonicalStateHash.sha256(current))
                                    .put("viewState", projectForUi(current))
                                    .put("diceRolls", diceRollsJson)
                                    .put("lifeCauseId", receipt?.lifeCauseId ?: JSONObject.NULL)
                                    .put("lifeTransitionRef", receipt?.lifeTransitionRef ?: JSONObject.NULL)
                                    .put("lifeConsequence", receipt?.lifeConsequence ?: JSONObject.NULL))
                            )
                            // Administrative wage operations do not advance companions or the story.
                            if (ActionAuthorityPolicy.triggersCompanionAutonomy(mechanicalIntent) &&
                                current.campaignState.party.characterIds.any {
                                    CharacterControlCoordinator.mode(current, it) != CharacterControlMode.PLAYER
                                }) {
                                val autonomous = runCatching { advanceMasterCharacters("reaction:$reactionId") }
                                autonomous.onFailure {
                                    android.util.Log.w("BraseiroOSE", "Autonomous party turn deferred; player action remains committed", it)
                                }
                                send(JSONObject().put("type", "MasterActionResult").put("payload", JSONObject()
                                    .put("ok", autonomous.isSuccess)
                                    .put("error", autonomous.exceptionOrNull()?.let(::safeError) ?: JSONObject.NULL)
                                    .put("viewState",
                                        projectForUi(session.loadSession(CAMPAIGN_ID)))))
                            }
                        }

                        "StartAdventure" -> {
                            val result = session.startAdventure(CAMPAIGN_ID)
                            val current = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "SessionUpdate").put("payload", JSONObject()
                                .put("narration", result.narration)
                                .put("feedback", "")
                                .put("committed", true)
                                .put("mechanicalMutation", false)
                                .put("ruleTrace", traceJson(result.trace))
                                .put("stateHash", CanonicalStateHash.sha256(current))
                                .put("viewState", projectForUi(current))))
                            if (current.campaignState.party.characterIds.any {
                                    CharacterControlCoordinator.mode(current, it) != CharacterControlMode.PLAYER
                                }) {
                                val autonomous = runCatching { advanceMasterCharacters("start:") }
                                autonomous.onFailure {
                                    android.util.Log.w("BraseiroOSE", "Master opening actions deferred; opening remains committed", it)
                                }
                                send(JSONObject().put("type", "MasterActionResult").put("payload", JSONObject()
                                    .put("ok", autonomous.isSuccess)
                                    .put("error", autonomous.exceptionOrNull()?.let(::safeError) ?: JSONObject.NULL)
                                    .put("viewState",
                                        projectForUi(session.loadSession(CAMPAIGN_ID)))))
                            }
                        }

                        "GMHelp" -> {
                            val result = session.gmHelp(CAMPAIGN_ID, payload.optString("question", ""))
                            send(JSONObject().put("type", "GMHelpResponse").put("payload", JSONObject()
                                .put("answer", result.answer).put("stateRevision", result.stateRevision)))
                        }

                        "TtsCommand" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            try {
                                when (payload.optString("command")) {
                                    "play" -> tts.playVisibleNarration(current.campaignState.game.session.visibleNarration)
                                    "replay" -> tts.replayVisibleNarration(current.campaignState.game.session.visibleNarration)
                                    "stop" -> tts.stop()
                                    "test" -> tts.playTestPhrase()
                                    else -> error("UNKNOWN_TTS_COMMAND")
                                }
                                sendTtsState(tts.state.name, tts.lastError)
                            } catch (t: Throwable) {
                                sendTtsState("ERROR", safeError(t))
                            }
                        }

                        "TtsSettingsGet" -> sendTtsSettingsState()

                        "TtsSettingsSet" -> {
                            tts.stop()
                            geminiTts.stop()
                            tts.configure(
                                payload.optString("enginePackage", "").takeIf { it.isNotBlank() },
                                payload.optString("voiceName", "").takeIf { it.isNotBlank() },
                                payload.optDouble("speechRate", tts.speechRate().toDouble()).toFloat(),
                                payload.optString("mode", "").takeIf { it.isNotBlank() }
                            )
                            sendTtsSettingsState()
                        }

                        "TtsTest" -> {
                            tts.playTestPhrase()
                            sendTtsState(tts.state.name, tts.lastError)
                        }

                        "AdventureImport" -> {
                            val raw = payload.optString("rawJson", "").trim()
                            if (raw.isNotBlank()) {
                                io.execute {
                                    try {
                                        val current = session.loadSession(CAMPAIGN_ID)
                                        val res = adventureStore.importJson(raw, scope = current.campaignId.value, expectedWorldId = null)
                                        val startLocationId = res.optString("startLocationId")
                                        val advWorldId = res.optString("worldId", current.campaignId.value)
                                        if (startLocationId.isNotBlank()) {
                                            val nextPos = SpatialRef.Scene(advWorldId, startLocationId)
                                            val updatedState = current.campaignState.copy(position = current.campaignState.position.copy(primary = nextPos))
                                            repository.commit(CAMPAIGN_ID, StateTransition("adventure-start-pos:$startLocationId", updatedState))
                                        }
                                        val persisted = session.loadSession(CAMPAIGN_ID)
                                        send(JSONObject().put("type", "AdventureImportResult").put("payload", res.put("viewState", projectForUi(persisted))))
                                    } catch (t: Throwable) {
                                        send(JSONObject().put("type", "BridgeError").put("payload", JSONObject().put("message", safeError(t))))
                                    }
                                }
                            } else {
                                runOnUiThread {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/json"
                                    }
                                    startActivityForResult(intent, REQUEST_IMPORT_ADVENTURE)
                                }
                            }
                        }

                        "AdventureFinish" -> {
                            io.execute {
                                try {
                                    val current = session.loadSession(CAMPAIGN_ID)
                                    val res = adventureStore.finish(scope = current.campaignId.value)
                                    val ok = res.optBoolean("ok", false)
                                    val state = current.campaignState
                                    val currentPos = state.position.primary
                                    val nextState = if (currentPos is SpatialRef.Scene) {
                                        val hexWorld = state.game.hexWorld
                                        val fallbackHex = hexWorld?.let { w ->
                                            val poi = w.cells.firstOrNull { it.poiId != null }
                                            if (poi != null) SpatialRef.Hex(w.worldId, poi.q, poi.r) else null
                                        } ?: SpatialRef.Hex("world-1", 3, 2)
                                        state.copy(position = PositionState(fallbackHex))
                                    } else state
                                    if (nextState != state) {
                                        repository.commit(CAMPAIGN_ID, StateTransition("adventure-finish-exit-scene", nextState))
                                    }
                                    val persisted = session.loadSession(CAMPAIGN_ID)
                                    send(JSONObject().put("type", "AdventureFinishResult").put("payload", JSONObject()
                                        .put("ok", ok)
                                        .put("archivedAdventureId", res.optString("archivedAdventureId", ""))
                                        .put("status", res.optString("status", ""))
                                        .put("viewState", projectForUi(persisted))))
                                } catch (t: Throwable) {
                                    send(JSONObject().put("type", "BridgeError").put("payload", JSONObject().put("message", safeError(t))))
                                }
                            }
                        }

                        "AdventureContextQuery" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "AdventureContextResult").put("payload", adventureStore.getActiveAdventureSummary(current.campaignId.value)))
                        }

                        "LifeStoreSync" -> {
                            io.execute {
                                try {
                                    val scope = payload.optString("scope", CAMPAIGN_ID.value).ifBlank { CAMPAIGN_ID.value }
                                    val stateJson = payload.optString("stateJson", "").trim()
                                    val revision = payload.optLong("revision", 0L)
                                    val day = payload.optInt("day", 0)
                                    if (stateJson.isNotBlank()) {
                                        db.lifeStoreDao().save(
                                            LifeStoreEntity(
                                                scope = scope,
                                                revision = revision,
                                                day = day,
                                                stateJson = stateJson,
                                                updatedAt = System.currentTimeMillis()
                                            )
                                        )
                                        send(JSONObject().put("type", "LifeStoreSyncResult").put("payload", JSONObject().put("ok", true).put("scope", scope)))
                                    }
                                } catch (t: Throwable) {
                                    send(JSONObject().put("type", "BridgeError").put("payload", JSONObject().put("message", safeError(t))))
                                }
                            }
                        }

                        "LifeStoreQuery" -> {
                            io.execute {
                                try {
                                    val scope = payload.optString("scope", CAMPAIGN_ID.value).ifBlank { CAMPAIGN_ID.value }
                                    val entity = db.lifeStoreDao().findByScope(scope)
                                    val json = entity?.stateJson ?: "{}"
                                    send(JSONObject().put("type", "LifeStoreResult").put("payload", JSONObject().put("ok", true).put("scope", scope).put("state", JSONObject(json))))
                                } catch (t: Throwable) {
                                    send(JSONObject().put("type", "BridgeError").put("payload", JSONObject().put("message", safeError(t))))
                                }
                            }
                        }

                        "GeminiSettingsGet" -> sendSettingsState()

                        "GeminiRuntimeSettingsSet" -> {
                            runtimeSettings.update(
                                payload.optString("textModel", "").takeIf { it.isNotBlank() },
                                payload.optString("imageModel", "").takeIf { it.isNotBlank() },
                                payload.optString("ttsModel", "").takeIf { it.isNotBlank() },
                                payload.optString("ttsVoice", "").takeIf { it.isNotBlank() }
                            )
                            sendSettingsState()
                        }

                        "GeminiCredentialSet" -> {
                            val slot = credentialSlot(payload.getString("slot"))
                            credentials.set(slot, payload.getString("key"))
                            sendSettingsState()
                        }

                        "GeminiCredentialDelete" -> {
                            credentials.delete(credentialSlot(payload.getString("slot")))
                            sendSettingsState()
                        }

                        "GeminiCredentialTest" -> {
                            val slot = credentialSlot(payload.getString("slot"))
                            val runtime = runtimeSettings.snapshot()
                            val ok = when (slot) {
                                GeminiCredentialStore.Slot.TEXT_SCENE -> {
                                    val key = credentials.get(slot) ?: error("GEMINI_KEY_MISSING")
                                    gemini.testCredential(key, runtime.textModel)
                                }
                                GeminiCredentialStore.Slot.TTS_AUDIO -> runCatching {
                                    tts.playTestPhrase()
                                    tts.state != braseiro.ose.tts.TtsState.ERROR
                                }.getOrDefault(false)
                            }
                            send(JSONObject().put("type", "GeminiCredentialTestResult").put("payload", JSONObject()
                                .put("slot", slot.wireName).put("ok", ok)))
                        }

                        "ScenePromptShare" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val prompt = buildSceneImagePrompt(current)
                            val sceneId = scenePresentationId(current)
                            launchGeminiWithPrompt(prompt, PresentationMediaStore.Scope.SCENE, sceneId)
                            send(JSONObject().put("type", "ScenePromptShareResult").put("payload", JSONObject()
                                .put("ok", true).put("promptLength", prompt.length).put("sceneId", sceneId)))
                        }

                        "PortraitPromptShare" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val kind = payload.getString("kind")
                            val stableId = payload.getString("id").trim()
                            val scope: PresentationMediaStore.Scope
                            val prompt: String
                            when (kind) {
                                "CHARACTER" -> {
                                    require(current.campaignState.game.characters.containsKey(stableId)) { "UNKNOWN_CHARACTER_ID" }
                                    scope = PresentationMediaStore.Scope.CHARACTER
                                    prompt = buildCharacterPortraitPrompt(current, stableId)
                                }
                                "NPC" -> {
                                    require(current.campaignState.game.npcs.containsKey(stableId)) { "UNKNOWN_NPC_ID" }
                                    scope = PresentationMediaStore.Scope.NPC
                                    prompt = buildNpcPortraitPrompt(current, stableId)
                                }
                                else -> error("UNSUPPORTED_PORTRAIT_KIND")
                            }
                            launchGeminiWithPrompt(prompt, scope, stableId)
                            send(JSONObject().put("type", "PortraitPromptShareResult").put("payload", JSONObject()
                                .put("ok", true).put("scope", scope.name).put("id", stableId).put("promptLength", prompt.length)))
                        }

                        "SceneImageGenerate" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val key = credentials.get(GeminiCredentialStore.Slot.TEXT_SCENE) ?: error("GEMINI_TEXT_SCENE_KEY_MISSING")
                            val blob = gemini.generateSceneImage(key, runtimeSettings.snapshot().imageModel, buildSceneImagePrompt(current))
                            val sceneId = scenePresentationId(current)
                            val url = mediaStore.save(PresentationMediaStore.Scope.SCENE, sceneId, blob.mimeType, blob.data)
                            send(JSONObject().put("type", "SceneImageResult").put("payload", JSONObject()
                                .put("ok", true).put("url", url).put("sceneId", sceneId).put("mechanicalDelta", false)))
                        }

                        "PortraitImageGenerate" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val kind = payload.getString("kind")
                            val stableId = payload.getString("id").trim()
                            val scope: PresentationMediaStore.Scope
                            val prompt: String
                            when (kind) {
                                "CHARACTER" -> {
                                    require(current.campaignState.game.characters.containsKey(stableId)) { "UNKNOWN_CHARACTER_ID" }
                                    scope = PresentationMediaStore.Scope.CHARACTER
                                    prompt = buildCharacterPortraitPrompt(current, stableId)
                                }
                                "NPC" -> {
                                    require(current.campaignState.game.npcs.containsKey(stableId)) { "UNKNOWN_NPC_ID" }
                                    scope = PresentationMediaStore.Scope.NPC
                                    prompt = buildNpcPortraitPrompt(current, stableId)
                                }
                                else -> error("UNSUPPORTED_PORTRAIT_KIND")
                            }
                            val key = credentials.get(GeminiCredentialStore.Slot.TEXT_SCENE) ?: error("GEMINI_TEXT_SCENE_KEY_MISSING")
                            val blob = gemini.generateSceneImage(key, runtimeSettings.snapshot().imageModel, prompt)
                            val url = mediaStore.save(scope, stableId, blob.mimeType, blob.data)
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "PresentationImageSaved").put("payload", JSONObject()
                                .put("ok", true).put("url", url).put("scope", scope.name).put("id", stableId).put("mechanicalDelta", false)
                                .put("viewState", projectForUi(persisted))))
                        }

                        "PresentationImageSave" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val scope = when (payload.getString("scope")) {
                                "SCENE" -> PresentationMediaStore.Scope.SCENE
                                "CHARACTER" -> PresentationMediaStore.Scope.CHARACTER
                                "NPC" -> PresentationMediaStore.Scope.NPC
                                "ITEM" -> PresentationMediaStore.Scope.ITEM
                                else -> error("UNSUPPORTED_PRESENTATION_SCOPE")
                            }
                            val stableId = payload.getString("id").trim()
                            when (scope) {
                                PresentationMediaStore.Scope.SCENE -> require(stableId == scenePresentationId(current)) { "STALE_SCENE_ID" }
                                PresentationMediaStore.Scope.CHARACTER -> require(current.campaignState.game.characters.containsKey(stableId)) { "UNKNOWN_CHARACTER_ID" }
                                PresentationMediaStore.Scope.NPC -> require(current.campaignState.game.npcs.containsKey(stableId)) { "UNKNOWN_NPC_ID" }
                                PresentationMediaStore.Scope.ITEM -> require(current.campaignState.game.inventory.items.containsKey(stableId)) { "UNKNOWN_ITEM_ID" }
                            }
                            val url = mediaStore.saveDataUrl(scope, stableId, payload.getString("dataUrl"))
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "PresentationImageSaved").put("payload", JSONObject()
                                .put("scope", scope.name).put("id", stableId).put("url", url).put("mechanicalDelta", false)
                                .put("viewState", projectForUi(persisted))))
                        }

                        "PresentationImageRemove" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val scope = when (payload.getString("scope")) {
                                "SCENE" -> PresentationMediaStore.Scope.SCENE
                                "CHARACTER" -> PresentationMediaStore.Scope.CHARACTER
                                "NPC" -> PresentationMediaStore.Scope.NPC
                                "ITEM" -> PresentationMediaStore.Scope.ITEM
                                else -> error("UNSUPPORTED_PRESENTATION_SCOPE")
                            }
                            val stableId = payload.getString("id").trim()
                            val removed = mediaStore.remove(scope, stableId)
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "PresentationImageRemoved").put("payload", JSONObject()
                                .put("ok", removed).put("scope", scope.name).put("id", stableId)
                                .put("viewState", projectForUi(persisted))))
                        }

                        "ManualDiceRoll" -> {
                            val sides = payload.optInt("sides", 20)
                            val count = payload.optInt("count", 1)
                            val modifier = payload.optInt("modifier", 0)
                            require(sides in setOf(4,6,8,10,12,20,100)) { "INVALID_DIE" }
                            require(count in 1..20) { "INVALID_DICE_COUNT" }
                            require(modifier in -100..100) { "INVALID_DICE_MODIFIER" }
                            val rolls = List(count) { manualDice.nextInt(sides) + 1 }
                            val total = rolls.sum() + modifier
                            send(JSONObject().put("type", "ManualDiceResult").put("payload", JSONObject()
                                .put("sides", sides).put("count", count).put("modifier", modifier)
                                .put("rolls", JSONArray(rolls)).put("total", total)
                                .put("mechanicalDelta", false)))
                        }

                        "RuleEvidenceQuery" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val ruleId = payload.optString("ruleId", "")
                            val result = evidenceRegistry.lookup(current.ruleProfile, ruleId)
                            send(JSONObject().put("type","RuleEvidenceResult").put("payload",JSONObject()
                                .put("ruleId",ruleId).put("text",result.text).put("trace",traceJson(result.trace))))
                        }

                        "RuleEvidenceList" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val rules = JSONArray(evidenceRegistry.registeredRuleIds().sorted().mapNotNull { id ->
                                val result = evidenceRegistry.lookup(current.ruleProfile, id)
                                if (result.trace.status.name == "MISSING_EVIDENCE" && result.trace.evidence.isEmpty()) null
                                else JSONObject().put("ruleId", id).put("trace", traceJson(result.trace))
                            })
                            send(JSONObject().put("type","RuleEvidenceListResult").put("payload",JSONObject()
                                .put("profile",current.ruleProfile.name).put("rules",rules)))
                        }

                        "ResetAdventure" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            repository.checkpoint(CAMPAIGN_ID, "before-reset-${System.currentTimeMillis()}")
                            val oldState = current.campaignState
                            val partyIds = oldState.party.characterIds
                            val oldGame = oldState.game
                            val keptCharacters = oldGame.characters.filterKeys { it in partyIds }
                            val keptInventory = oldGame.inventory.items.filterValues { it.ownerId in partyIds }
                            val keptLoadouts = oldGame.loadouts.filterKeys { it in partyIds }
                            val newSeed = kotlin.random.Random.nextLong().toULong()
                            val base = buildWorldBootstrap(current.ruleProfile, newSeed)
                            val nextPhase = when {
                                partyIds.isEmpty() -> "ACTIVE"
                                keptLoadouts.values.any { !it.completed } -> "CHARACTER_LOADOUT"
                                else -> "READY_TO_START"
                            }
                            val nextGame = base.game.copy(
                                characters = keptCharacters,
                                characterControlModes = oldGame.characterControlModes.filterKeys { it in partyIds },
                                characterActionProposals = oldGame.characterActionProposals.filterValues {
                                    it.characterId in partyIds && it.status == CharacterActionApproval.EXECUTED
                                },
                                inventory = base.game.inventory.copy(items = keptInventory),
                                loadouts = keptLoadouts,
                                session = SessionSnapshot(phase = nextPhase),
                                revision = oldGame.revision + 1
                            ).canonical()
                            val fresh = repairCanonicalResources(current.copy(
                                createdAtMetadata = "reset-${System.currentTimeMillis()}",
                                campaignState = base.copy(
                                    party = oldState.party,
                                    resources = ResourceState(),
                                    game = nextGame
                                )
                            ))
                            repository.replace(fresh)
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type","ResetAdventureResult").put("payload",JSONObject()
                                .put("ok",true).put("characterPreserved", partyIds.isNotEmpty())
                                .put("stateHash",CanonicalStateHash.sha256(persisted))
                                .put("viewState",projectForUi(persisted))))
                        }

                        "BackupExport" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            // Gemini credentials are in noBackupFilesDir and not part of CampaignEnvelope/BackupCodec.
                            send(JSONObject().put("type","BackupExportResult").put("payload",JSONObject()
                                .put("campaignId",current.campaignId.value).put("backup",BackupCodec.exportCampaign(current, selectedCharacterId(current)))))
                        }

                        "BackupImport" -> {
                            val backupText = payload.getString("backup")
                            val imported = BackupCodec.importCampaign(backupText)
                            require(imported.campaignId == CAMPAIGN_ID) { "Backup campaign id mismatch" }
                            val policy = braseiro.ose.rules.api.OptionPolicyContract.resolve(imported.ruleProfile, imported.optionSet)
                            require(policy.valid) { "Invalid backup option policy: ${policy.errors.joinToString()}" }
                            repository.replace(imported)
                            val current = session.loadSession(CAMPAIGN_ID)
                            val restoredSelection = BackupCodec.selectedCharacterId(backupText)
                                ?.takeIf { it in current.campaignState.party.characterIds }
                            storeSelectedCharacterId(current, restoredSelection)
                            send(JSONObject().put("type","BackupImportResult").put("payload",JSONObject()
                                .put("ok",true).put("campaignId",current.campaignId.value)
                                .put("stateHash",CanonicalStateHash.sha256(current)).put("viewState",projectForUi(current))))
                        }

                        "CharacterCreationRandom" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val commandId = payload.optString("commandId", "").trim()
                                .ifBlank { "legacy-random-${java.util.UUID.randomUUID()}" }
                            val actionId = "RANDOM_CHARACTER:$commandId"
                            val receipt = current.campaignState.game.actionLog.firstOrNull { it.actionId == actionId }
                            val createdCharacter = if (receipt != null) {
                                val existingId = receipt.text.removePrefix("CREATED:")
                                require(existingId in current.campaignState.party.characterIds) {
                                    "CHARACTER_COMMAND_ALREADY_APPLIED"
                                }
                                current.campaignState.game.characters[existingId] ?: error("UNKNOWN_CHARACTER_ID")
                            } else {
                                val (createdEnvelope, character) = createRandomCharacter(current)
                                val nextGame = createdEnvelope.campaignState.game.copy(
                                    actionLog = createdEnvelope.campaignState.game.actionLog +
                                        ActionLogEntry(actionId, "CAMPAIGN_ADMIN",
                                            "CREATED:${character.characterId}", emptyList())
                                ).canonical()
                                repository.commit(CAMPAIGN_ID, StateTransition(
                                    "character-random:$commandId",
                                    createdEnvelope.campaignState.copy(game = nextGame)
                                ))
                                character
                            }
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            storeSelectedCharacterId(persisted, createdCharacter.characterId)
                            send(JSONObject().put("type","CharacterCreationResult").put("payload",JSONObject()
                                .put("ok",true).put("random",true).put("characterId",createdCharacter.characterId).put("name",createdCharacter.name)
                                .put("stateHash",CanonicalStateHash.sha256(persisted)).put("viewState",projectForUi(persisted))))
                        }

                        "CharacterSheetExportRequest" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val character = selectedCharacter(current, payload.optString("characterId", ""))
                            val sheet = characterSheetJson(current, character.characterId)
                            pendingCharacterExport = ("${safeFileName(character.name)}_ficha.json" to sheet)
                            runOnUiThread {
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TITLE, pendingCharacterExport!!.first)
                                }
                                startActivityForResult(intent, REQUEST_EXPORT_CHARACTER)
                            }
                        }

                        "CharacterSheetImportRequest" -> runOnUiThread {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/json"
                            }
                            startActivityForResult(intent, REQUEST_IMPORT_CHARACTER)
                        }

                        "CharacterSheetUpdate" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val character = selectedCharacter(current, payload.optString("characterId", ""))
                            val name = payload.optString("name", "").trim()
                            require(name.isNotBlank() && name.length <= 80) { "INVALID_CHARACTER_NAME" }
                            val game = current.campaignState.game
                            if (character.name != name) {
                                val commandId = payload.optString("commandId", "").trim()
                                    .ifBlank { "legacy-${character.characterId}-${game.revision}" }
                                val updated = character.copy(name = name).canonical()
                                val nextGame = game.copy(
                                    characters = game.characters + (updated.characterId to updated),
                                    revision = game.revision + 1
                                ).canonical()
                                repository.commit(CAMPAIGN_ID, StateTransition(
                                    "character-edit:$commandId",
                                    current.campaignState.copy(game = nextGame)
                                ))
                            }
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type","CharacterSheetUpdateResult").put("payload",JSONObject()
                                .put("ok",true).put("viewState",projectForUi(persisted))))
                        }

                        "CharacterRecreateBeforeStart" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val character = selectedCharacter(current)
                            require(current.campaignState.game.session.visibleNarration.isBlank() &&
                                current.campaignState.time.turns == 0L) { "CHARACTER_RECREATE_AFTER_START" }
                            repository.checkpoint(CAMPAIGN_ID, "before-character-recreate-${System.currentTimeMillis()}")
                            val next = CharacterPartyAdministration.delete(
                                current, character.characterId, "legacy-recreate-${System.currentTimeMillis()}"
                            )
                            repository.commit(CAMPAIGN_ID, StateTransition(
                                "character-recreate:${current.createdAtMetadata}:${current.campaignState.game.revision}",
                                next.campaignState
                            ))
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            storeSelectedCharacterId(persisted, selectedCharacterId(persisted))
                            send(JSONObject().put("type","CharacterRecreateResult").put("payload",JSONObject()
                                .put("ok",true).put("viewState",projectForUi(persisted))))
                        }

                        "CharacterCreationStart" -> {
                            val draftId = payload.optString("draftId", "").trim()
                            require(draftId.isNotBlank()) { "draftId is required" }
                            val current = session.loadSession(CAMPAIGN_ID)
                            val outcome = creationCoordinator.startDraft(current, draftId)
                            if (outcome.created) repository.commit(CAMPAIGN_ID, StateTransition("character-draft:$draftId", outcome.envelope.campaignState))
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            val draft = persisted.campaignState.game.creationDrafts.getValue(draftId)
                            send(JSONObject().put("type","CharacterCreationDraft").put("payload",JSONObject()
                                .put("draftId",draft.draftId).put("profile",draft.profile.name)
                                .put("attributes",JSONObject().put("str",draft.rolledAttributes.str).put("int",draft.rolledAttributes.int)
                                    .put("wis",draft.rolledAttributes.wis).put("dex",draft.rolledAttributes.dex)
                                    .put("con",draft.rolledAttributes.con).put("cha",draft.rolledAttributes.cha))
                                .put("created",outcome.created).put("viewState",projectForUi(persisted))))
                        }

                        "CharacterCreationCancel" -> {
                            val draftId = payload.optString("draftId", "").trim()
                            val current = session.loadSession(CAMPAIGN_ID)
                            val game = current.campaignState.game
                            val newDrafts = if (draftId.isNotBlank()) game.creationDrafts - draftId else emptyMap()
                            val updatedGame = game.copy(creationDrafts = newDrafts)
                            repository.commit(CAMPAIGN_ID, StateTransition("character-draft-cancel", current.campaignState.copy(game = updatedGame)))
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "CharacterCreationCancelResult").put("payload", JSONObject()
                                .put("ok", true).put("viewState", projectForUi(persisted))))
                        }

                        "PlayerGuideModeSet" -> {
                            val modeStr = payload.optString("mode", "LEARN_PLAYING")
                            playerGuideMode = OSEPlayerGuide.GuideMode.fromString(modeStr)
                            getSharedPreferences("braseiro_ose_player_guide_v1", MODE_PRIVATE)
                                .edit().putString("guide_mode", playerGuideMode.wireName).apply()
                            val current = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "PlayerGuideModeResult").put("payload", JSONObject()
                                .put("ok", true).put("mode", playerGuideMode.wireName).put("viewState", projectForUi(current))))
                        }

                        "PlayerGuideQuery" -> {
                            val topicId = payload.optString("topicId", "").trim()
                            val topic = if (topicId.isNotBlank()) OSEPlayerGuide.getTopic(topicId) else null
                            val all = OSEPlayerGuide.getAllTopics()
                            send(JSONObject().put("type", "PlayerGuideQueryResult").put("payload", JSONObject()
                                .put("ok", true)
                                .put("topic", topic?.toJson() ?: JSONObject.NULL)
                                .put("topics", JSONArray(all.map { it.toJson() }))))
                        }

                        "ActionSuggestionsQuery" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val suggestions = OSEPlayerGuide.getActionSuggestions(current)
                            send(JSONObject().put("type", "ActionSuggestionsQueryResult").put("payload", JSONObject()
                                .put("ok", true)
                                .put("suggestions", JSONArray(suggestions.map { it.toJson() }))))
                        }

                        "CharacterCreationCommit" -> {
                            val draftId = payload.optString("draftId", "").trim()
                            val characterId = payload.optString("characterId", "").trim()
                            val name = payload.optString("name", "").trim()
                            val method = CreationMethod.valueOf(payload.getString("method"))
                            val classArray = payload.getJSONArray("classIds")
                            val classIds = (0 until classArray.length()).map { classArray.getString(it) }
                            val raceId = payload.optString("raceId", "").trim().ifBlank { null }
                            val adjustments = buildList {
                                val a = payload.optJSONArray("adjustments")
                                if (a != null) for (i in 0 until a.length()) {
                                    val x = a.getJSONObject(i)
                                    add(AttributeAdjustment(AttributeKey.valueOf(x.getString("decrease")), AttributeKey.valueOf(x.getString("increase"))))
                                }
                            }
                            val current = session.loadSession(CAMPAIGN_ID)
                            require(!current.campaignState.game.characters.containsKey(characterId)) { "CHARACTER_ID_EXISTS" }
                            val outcome = creationCoordinator.commit(current,draftId,characterId,name,method,classIds,raceId,adjustments)
                            when (val result = outcome.result) {
                                is CharacterCreationResult.Created -> {
                                    if (outcome.stateChanged) repository.commit(CAMPAIGN_ID,StateTransition("character-create:$characterId",outcome.envelope.campaignState))
                                    val persisted = session.loadSession(CAMPAIGN_ID)
                                    storeSelectedCharacterId(persisted, result.character.characterId)
                                    send(JSONObject().put("type","CharacterCreationResult").put("payload",JSONObject()
                                        .put("ok",true).put("characterId",result.character.characterId).put("name",result.character.name)
                                        .put("stateHash",CanonicalStateHash.sha256(persisted)).put("viewState",projectForUi(persisted))))
                                }
                                is CharacterCreationResult.Rejected -> send(JSONObject().put("type","CharacterCreationResult").put("payload",JSONObject()
                                    .put("ok",false).put("code",result.code.name).put("detail",result.detail)))
                            }
                        }

                        "UiCommand" -> {
                            val command = payload.optString("command", "").trim()
                            val commandId = payload.optString("commandId", "").trim()
                            require(commandId.isNotBlank()) { "UiCommand.commandId is required" }
                            val characterId = payload.optString("characterId", "").trim()
                            val current = session.loadSession(CAMPAIGN_ID)
                            val result = when (command) {
                                "loadout_purchase" -> loadoutCoordinator.purchase(current, characterId, payload.getString("catalogId"))
                                "loadout_equip" -> loadoutCoordinator.setEquipped(current, characterId, payload.getString("itemId"), payload.optBoolean("equipped", true))
                                "loadout_finish" -> loadoutCoordinator.finish(current, characterId)
                                else -> error("UNKNOWN_UI_COMMAND")
                            }
                            if (result.changed) repository.commit(CAMPAIGN_ID, StateTransition("ui:$commandId", result.envelope.campaignState))
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type","UiCommandResult").put("payload",JSONObject()
                                .put("ok", result.status !in setOf("INVALID_LOADOUT","CATALOG_ITEM_NOT_FOUND","INSUFFICIENT_GOLD","EQUIPMENT_RESTRICTED","TWO_HANDED_WEAPON_WITH_SHIELD"))
                                .put("status", result.status).put("detail", result.detail)
                                .put("stateHash", CanonicalStateHash.sha256(persisted)).put("viewState", projectForUi(persisted))))
                        }

                        "CharacterDelete" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val id = payload.getString("characterId").trim()
                            val commandId = payload.getString("commandId").trim()
                            require(id.isNotBlank() && commandId.isNotBlank()) { "INVALID_CHARACTER_COMMAND" }
                            val old = current.campaignState
                            val character = old.game.characters[id] ?: error("UNKNOWN_CHARACTER_ID")
                            if (id in old.party.characterIds) {
                                repository.checkpoint(CAMPAIGN_ID, "before-delete-$id-${System.currentTimeMillis()}")
                                val updated = CharacterPartyAdministration.delete(current, id, commandId)
                                repository.commit(CAMPAIGN_ID, StateTransition(
                                    "character-delete:$commandId", updated.campaignState
                                ))
                            } else {
                                require("ARCHIVED_DELETED" in character.flags) { "UNKNOWN_CHARACTER_ID" }
                            }
                            val persisted = session.loadSession(CAMPAIGN_ID)
                            storeSelectedCharacterId(persisted, selectedCharacterId(persisted))
                            send(JSONObject().put("type","CharacterDeleteResult").put("payload",JSONObject()
                                .put("ok",true).put("characterId",id)
                                .put("viewState",projectForUi(persisted))))
                        }

                        "CharacterControlSet" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val id = payload.getString("characterId").trim()
                            val mode = CharacterControlMode.valueOf(payload.getString("mode").trim())
                            val next = CharacterControlCoordinator.setMode(current, id, mode)
                            if (next != current) repository.commit(CAMPAIGN_ID, StateTransition(
                                "character-control:" + id + ":" + java.util.UUID.randomUUID(),
                                next.campaignState
                            ))
                            val updated = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "CharacterControlResult").put("payload", JSONObject()
                                .put("ok", true).put("characterId", id).put("mode", mode.name)
                                .put("viewState", projectForUi(updated))))
                        }

                        "MasterActionPropose" -> {
                            val actorId = payload.getString("characterId").trim()
                            val current = session.loadSession(CAMPAIGN_ID)
                            require(CharacterControlCoordinator.mode(current, actorId) != CharacterControlMode.PLAYER) {
                                "CHARACTER_PLAYER_CONTROLLED"
                            }
                            val proposalId = payload.optString("proposalId", "").trim()
                                .ifBlank { "manual:" + java.util.UUID.randomUUID() }
                            val text = payload.getString("text").trim()
                            val intent = payload.optString("mechanicalIntent", "").trim().ifBlank { text }
                            val next = CharacterControlCoordinator.propose(current, proposalId, actorId, text, intent)
                            if (next != current) repository.commit(CAMPAIGN_ID, StateTransition(
                                "master-propose:" + proposalId, next.campaignState
                            ))
                            if (CharacterControlCoordinator.mode(next, actorId) == CharacterControlMode.MASTER) {
                                executeMasterProposal(proposalId)
                            }
                            val updated = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "MasterActionResult").put("payload", JSONObject()
                                .put("ok", true).put("viewState", projectForUi(updated))))
                        }

                        "MasterActionReview" -> {
                            val proposalId = payload.getString("proposalId").trim()
                            val approve = payload.getBoolean("approve")
                            val current = session.loadSession(CAMPAIGN_ID)
                            val editedText = payload.optString("editedText", "").trim().takeIf { it.isNotBlank() }
                            val editedIntent = payload.optString("editedIntent", "").trim().takeIf { it.isNotBlank() }
                            val next = CharacterControlCoordinator.review(
                                current, proposalId, approve, editedText, editedIntent
                            )
                            if (next != current) repository.commit(CAMPAIGN_ID, StateTransition(
                                "master-review:" + proposalId, next.campaignState
                            ))
                            if (approve) executeMasterProposal(proposalId)
                            val updated = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "MasterActionResult").put("payload", JSONObject()
                                .put("ok", true).put("approved", approve)
                                .put("viewState", projectForUi(updated))))
                        }

                        "MasterActionContinue" -> {
                            val only = payload.optString("characterId", "").trim()
                            advanceMasterCharacters(
                                "continue:" + java.util.UUID.randomUUID(),
                                only.takeIf { it.isNotBlank() }
                            )
                            send(JSONObject().put("type", "MasterActionResult").put("payload", JSONObject()
                                .put("ok", true).put("viewState", projectForUi(session.loadSession(CAMPAIGN_ID)))))
                        }

                        "CompanionOpinionsQuery" -> {
                            val before = session.loadSession(CAMPAIGN_ID)
                            val hashBefore = CanonicalStateHash.sha256(before)
                            val opinions = CompanionJudgmentEngine.opinions(before)
                            val after = session.loadSession(CAMPAIGN_ID)
                            check(hashBefore == CanonicalStateHash.sha256(after)) {
                                "COMPANION_OPINION_STATE_MUTATION"
                            }
                            send(JSONObject().put("type", "CompanionOpinionsResult").put("payload",
                                JSONObject().put("readOnly", true).put("mechanicalDelta", false)
                                    .put("stateHash", hashBefore)
                                    .put("opinions", JSONArray(opinions.map { opinion ->
                                        JSONObject().put("characterId", opinion.characterId)
                                            .put("name", opinion.name)
                                            .put("perception", opinion.perception)
                                            .put("hypothesis", opinion.hypothesis)
                                            .put("motivation", opinion.motivation)
                                            .put("reason", opinion.reason)
                                            .put("risk", opinion.risk)
                                            .put("stance", opinion.stance)
                                            .put("suggestion", opinion.suggestedAction)
                                    }))))
                        }

                        "CharacterSelect" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            storeSelectedCharacterId(current, payload.getString("characterId").trim())
                            send(JSONObject().put("type","CharacterSelectionResult").put("payload",JSONObject()
                                .put("ok",true).put("viewState",projectForUi(current))))
                        }

                        "ViewState" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "ViewState").put("payload", projectForUi(current)))
                        }
                    }
                } catch (t: Throwable) {
                    send(JSONObject().put("type", "BridgeError").put("payload", JSONObject()
                        .put("message", safeError(t))))
                }
            }
        }
    }

    /**
     * A bounded Mestre turn after a player action or opening. Barbara chooses
     * among visible, read-only suggestions. Mechanics still flow through the
     * deterministic referee; group movement is never imposed on human players.
     */
    private fun advanceMasterCharacters(turnKey: String, onlyCharacterId: String? = null) {
        val initial = session.loadSession(CAMPAIGN_ID)
        val ids = initial.campaignState.party.characterIds
        if (onlyCharacterId != null) require(onlyCharacterId in ids) { "UNKNOWN_CHARACTER_ID" }
        val candidates = ids.filter { id ->
            (onlyCharacterId == null || id == onlyCharacterId) &&
                CharacterControlCoordinator.mode(initial, id) != CharacterControlMode.PLAYER
        }.take(8)
        for (id in candidates) {
            val current = session.loadSession(CAMPAIGN_ID)
            if (id !in current.campaignState.party.characterIds) continue
            val proposalId = "auto:" + turnKey.hashCode() + ":" + id.hashCode()
            if (current.campaignState.game.characterActionProposals.containsKey(proposalId)) continue
            val name = current.campaignState.game.characters[id]?.name ?: continue
            val judgment = CompanionJudgmentEngine.assess(current, id)
            val suggestions = ContextualDecisionSuggester.suggest(current)
            val options = suggestions.mapIndexed { index, d ->
                (index + 1).toString() + ") " + d.label + ": " + d.reaction
            }.joinToString("\n")
            val answer = runCatching {
                session.gmHelp(CAMPAIGN_ID,
                    "Como o personagem " + name + ", escolha UMA opção pública. " +
                    "Responda primeiro com 1, 2 ou 3, sem inventar regras ou informação secreta:\n" +
                    options).answer
            }.getOrDefault("")
            val selectedNumber = if (answer.startsWith("Posso ajudar") || answer.startsWith("Não consegui"))
                null else answer.trim().firstOrNull()?.digitToIntOrNull()?.takeIf { it in 1..suggestions.size }
            val groupActionPermitted = ids.all { member ->
                member == id || CharacterControlCoordinator.mode(current, member) == CharacterControlMode.MASTER
            }
            // A vulnerable or skeptical actor may disagree/refuse. Neither an LLM
            // choice nor an actor's autonomy authorizes moving another character.
            val chosen = if (judgment.stance in setOf("OBJECT", "CAUTION", "UNCERTAIN")) null
                else suggestions[(selectedNumber ?: 1) - 1]
            val collective = chosen?.mechanicalIntent?.startsWith("WILD_") == true
            val reaction = if (chosen == null || (collective && !groupActionPermitted))
                judgment.suggestedAction else chosen.reaction
            val intent = if (chosen == null || (collective && !groupActionPermitted))
                judgment.mechanicalIntent else (chosen.mechanicalIntent ?: chosen.reaction)
            val next = CharacterControlCoordinator.propose(
                current, proposalId, id, reaction, intent, judgment
            )
            repository.commit(CAMPAIGN_ID, StateTransition(
                "master-propose:" + proposalId, next.campaignState
            ))
            if (CharacterControlCoordinator.mode(next, id) == CharacterControlMode.MASTER) {
                executeMasterProposal(proposalId)
            }
        }
    }

    private fun executeMasterProposal(proposalId: String) {
        val current = session.loadSession(CAMPAIGN_ID)
        val proposal = current.campaignState.game.characterActionProposals[proposalId]
            ?: error("UNKNOWN_PROPOSAL")
        require(proposal.status == CharacterActionApproval.APPROVED ||
            proposal.status == CharacterActionApproval.EXECUTED) { "CHARACTER_CURATOR_APPROVAL_REQUIRED" }
        session.submitCharacterAction(
            CAMPAIGN_ID, "master-action:" + proposalId, proposal.characterId,
            proposal.reactionText, proposal.mechanicalIntent,
            fromMaster = true, approvedProposalId = proposalId
        )
        // The SessionEngine persists the executed proposal and mechanical
        // receipt together. A second commit here would create a crash window.
    }

    private fun traceJson(trace: RuleTrace): JSONObject {
        // Presentation is enriched from the canonical 252-rule evidence registry.
        // This never changes mechanics: it only replaces internal spec references with
        // the actual OSE book/source locations that justify the already-resolved rule.
        val evidence = trace.evidence.flatMap { original ->
            val canonical = evidenceRegistry.lookup(original.profile, original.ruleId).trace.evidence
            if (canonical.isNotEmpty()) canonical else listOf(original)
        }.distinctBy { it.ref() }
        return JSONObject()
            .put("status", trace.status.name)
            .put("note", trace.note)
            .put("evidence", JSONArray(evidence.map { e ->
                JSONObject().put("ruleId", e.ruleId).put("profile", e.profile.name)
                    .put("source", e.source).put("location", e.location)
            }))
    }

    private fun latestRuleTraceJson(current: CampaignEnvelope): JSONObject? {
        val sessionState = current.campaignState.game.session
        val lastAction = current.campaignState.game.actionLog.lastOrNull { it.channel == "PLAYER_REACTION" } ?: return null
        val reactionId = lastAction.actionId.removePrefix("reaction:")
        val receipt = sessionState.reactionReceipts[reactionId] ?: return null
        val evidence = receipt.traceEvidenceRefs.mapNotNull { raw ->
            val p = raw.split('|', limit = 4)
            if (p.size != 4) null else runCatching { RuleEvidence(p[0], RuleProfile.valueOf(p[1]), p[2], p[3]) }.getOrNull()
        }
        val status = runCatching { braseiro.ose.rules.api.EvidenceStatus.valueOf(receipt.traceStatus) }.getOrDefault(braseiro.ose.rules.api.EvidenceStatus.REFEREE_JUDGMENT)
        return traceJson(RuleTrace(status, evidence, receipt.traceNote))
    }

    private fun selectedCharacterId(current: CampaignEnvelope): String? {
        val ids = current.campaignState.party.characterIds
        val saved = getSharedPreferences("ose-character-selection", MODE_PRIVATE).getString(current.campaignId.value, null)
        return saved?.takeIf { it in ids && it in current.campaignState.game.characters } ?: ids.firstOrNull()
    }

    private fun storeSelectedCharacterId(current: CampaignEnvelope, id: String?) {
        val edit = getSharedPreferences("ose-character-selection", MODE_PRIVATE).edit()
        if (id == null) edit.remove(current.campaignId.value)
        else {
            require(id in current.campaignState.party.characterIds &&
                current.campaignState.game.characters.containsKey(id)) { "UNKNOWN_CHARACTER_ID" }
            edit.putString(current.campaignId.value, id)
        }
        require(edit.commit()) { "CHARACTER_SELECTION_SAVE_FAILED" }
    }

    private fun selectedCharacter(current: CampaignEnvelope, requestedId: String? = null): CharacterSnapshot {
        val id = requestedId?.takeIf { it.isNotBlank() } ?: selectedCharacterId(current) ?: error("NO_CHARACTER")
        require(id in current.campaignState.party.characterIds) { "UNKNOWN_CHARACTER_ID" }
        return current.campaignState.game.characters[id] ?: error("UNKNOWN_CHARACTER_ID")
    }

    private fun createRandomCharacter(current: CampaignEnvelope): Pair<CampaignEnvelope, CharacterSnapshot> {
        val draftId = "random-${java.util.UUID.randomUUID()}"
        val draft = creationCoordinator.startDraft(current, draftId)
        val base = draft.envelope
        val candidates = when (current.ruleProfile) {
            RuleProfile.OSE_CLASSIC_FANTASY -> listOf("FIGHTER","CLERIC","THIEF","MAGIC_USER","DWARF","ELF","HALFLING")
            RuleProfile.OSE_ADVANCED_FANTASY -> listOf("FIGHTER","CLERIC","THIEF","MAGIC_USER","RANGER","BARD","DRUID","ACROBAT","BARBARIAN","KNIGHT","PALADIN","ILLUSIONIST","ASSASSIN")
        }.shuffled(kotlin.random.Random(manualDice.nextLong()))
        val method = if (current.ruleProfile == RuleProfile.OSE_CLASSIC_FANTASY) CreationMethod.CLASSIC else CreationMethod.ADVANCED_BASIC
        val names = listOf("Aldren","Brena","Cael","Dora","Edrik","Fenna","Garran","Helga","Ivar","Lysa","Marek","Nora","Orin","Petra","Rurik","Sela")
        val name = names[manualDice.nextInt(names.size)]
        val characterId = "char-${java.util.UUID.randomUUID()}"
        for (classId in candidates) {
            val attempt = creationCoordinator.commit(base, draftId, characterId, name, method, listOf(classId), null, emptyList())
            val created = attempt.result as? CharacterCreationResult.Created ?: continue
            return attempt.envelope to created.character
        }
        error("RANDOM_CHARACTER_NO_VALID_CLASS")
    }

    private fun characterSheetJson(current: CampaignEnvelope, characterId: String): String {
        val c = selectedCharacter(current, characterId)
        val game = current.campaignState.game
        val root = JSONObject().put("schema", "BRASEIRO_OSE_FICHA_V1").put("profile", current.ruleProfile.name)
            .put("character", characterToJson(c))
        mediaStore.url(PresentationMediaStore.Scope.CHARACTER, c.characterId)?.let { url ->
            root.put("portraitUrl", url)
        }
        mediaStore.exportDataUrl(PresentationMediaStore.Scope.CHARACTER, c.characterId)?.let { dataUrl ->
            root.put("portraitDataUrl", dataUrl)
        }
        root.put("inventory", JSONArray(game.inventory.items.values.filter { it.ownerId == c.characterId }.sortedBy { it.itemId }.map(::inventoryItemToJson)))
        game.loadouts[c.characterId]?.let { l ->
            root.put("loadout", JSONObject().put("characterId", l.characterId).put("startingWealthGp", l.startingWealthGp)
                .put("remainingWealthGp", l.remainingWealthGp).put("completed", l.completed).put("sourceRuleId", l.sourceRuleId)
                .put("purchases", JSONObject(l.purchases)))
        }
        return root.toString(2)
    }

    private fun characterToJson(c: CharacterSnapshot): JSONObject = JSONObject()
        .put("characterId", c.characterId).put("name", c.name).put("creationMethod", c.creationMethod).put("raceId", c.raceId)
        .put("classIds", JSONArray(c.classIds)).put("levelByClass", JSONObject(c.levelByClass)).put("xpByClass", JSONObject(c.xpByClass))
        .put("attributes", JSONObject().put("str",c.attributes.str).put("int",c.attributes.int).put("wis",c.attributes.wis).put("dex",c.attributes.dex).put("con",c.attributes.con).put("cha",c.attributes.cha))
        .put("hitPoints", JSONObject().put("currentNumerator",c.hitPoints.currentNumerator).put("maxNumerator",c.hitPoints.maxNumerator).put("denominator",c.hitPoints.denominator))
        .put("armorClassDescending",c.armorClassDescending).put("armorClassAscending",c.armorClassAscending).put("thac0",c.thac0).put("attackBonusAscending",c.attackBonusAscending)
        .put("savingThrows", JSONObject().put("deathPoison",c.savingThrows.deathPoison).put("wands",c.savingThrows.wands).put("paralysisPetrification",c.savingThrows.paralysisPetrification).put("breath",c.savingThrows.breath).put("spellsRodsStaves",c.savingThrows.spellsRodsStaves))
        .put("languages",JSONArray(c.languages)).put("flags",JSONArray(c.flags.toList())).put("evidenceRefs",JSONArray(c.evidenceRefs))
        .put("primeModifierPercentByClass",JSONObject(c.primeModifierPercentByClass)).put("weaponProficiencies",JSONArray(c.weaponProficiencies.toList()))
        .put("weaponSpecialization",c.weaponSpecialization ?: JSONObject.NULL).put("weaponProficiencySlotsTotal",c.weaponProficiencySlotsTotal)
        .put("weaponProficiencySlotsSpent",c.weaponProficiencySlotsSpent).put("secondarySkills",JSONArray(c.secondarySkills))

    private fun inventoryItemToJson(i: InventoryItemSnapshot): JSONObject {
        val out = JSONObject().put("itemId",i.itemId).put("ownerId",i.ownerId).put("catalogId",i.catalogId).put("profile",i.profile.name)
            .put("sourceId",i.sourceId).put("kind",i.kind.name).put("quantity",i.quantity).put("carried",i.carried).put("equipped",i.equipped)
            .put("unitWeightCoins",i.unitWeightCoins).put("armorLoadCategory",i.armorLoadCategory?.name ?: JSONObject.NULL)
            .put("lightSourceKind",i.lightSourceKind?.name ?: JSONObject.NULL)
        i.weapon?.let { w -> out.put("weapon",JSONObject().put("melee",w.melee).put("missile",w.missile).put("slow",w.slow)
            .put("damageDiceCount",w.damageDiceCount).put("damageDieSides",w.damageDieSides).put("damageBonus",w.damageBonus)
            .put("shortRangeMeters",w.shortRangeMeters ?: JSONObject.NULL).put("mediumRangeMeters",w.mediumRangeMeters ?: JSONObject.NULL).put("longRangeMeters",w.longRangeMeters ?: JSONObject.NULL)) }
        return out
    }

    private fun importCharacterSheet(current: CampaignEnvelope, raw: String): CampaignEnvelope {
        val root = JSONObject(raw)
        require(root.getString("schema") == "BRASEIRO_OSE_FICHA_V1") { "INVALID_CHARACTER_SHEET" }
        require(root.getString("profile") == current.ruleProfile.name) { "CHARACTER_PROFILE_MISMATCH" }
        var c = characterFromJson(root.getJSONObject("character"))
        if (current.campaignState.game.characters.containsKey(c.characterId)) {
            val freshId = "${c.characterId}_${System.currentTimeMillis()}"
            c = c.copy(characterId = freshId).canonical()
        }
        val portraitDataUrl = root.optString("portraitDataUrl", "").trim()
        if (portraitDataUrl.isNotBlank()) {
            runCatching {
                mediaStore.saveDataUrl(PresentationMediaStore.Scope.CHARACTER, c.characterId, portraitDataUrl)
            }
        }
        val items = mutableMapOf<String, InventoryItemSnapshot>()
        val arr = root.optJSONArray("inventory") ?: JSONArray()
        for (i in 0 until arr.length()) {
            var item = inventoryItemFromJson(arr.getJSONObject(i))
            require(item.profile == current.ruleProfile) { "CHARACTER_ITEM_PROFILE_MISMATCH" }
            item = item.copy(ownerId = c.characterId)
            if (current.campaignState.game.inventory.items.containsKey(item.itemId) || items.containsKey(item.itemId)) {
                item = item.copy(itemId = "${item.itemId}_${System.currentTimeMillis()}_$i")
            }
            items[item.itemId] = item
        }
        val loadout = root.optJSONObject("loadout")?.let { o ->
            val purchases = mutableMapOf<String,Int>(); val keys=o.optJSONObject("purchases")?.keys(); if(keys!=null) while(keys.hasNext()){ val k=keys.next(); purchases[k]=o.getJSONObject("purchases").getInt(k) }
            CharacterLoadoutSnapshot(c.characterId,o.getInt("startingWealthGp"),o.getInt("remainingWealthGp"),purchases,o.optBoolean("completed",true),o.getString("sourceRuleId"))
        }
        val oldGame = current.campaignState.game
        val partyIds = (current.campaignState.party.characterIds + c.characterId).distinct()
        val beforeAdventure = current.campaignState.time.turns == 0L &&
            oldGame.session.visibleNarration.isBlank()
        val newPhase = when {
            beforeAdventure && loadout?.completed == false -> "CHARACTER_LOADOUT"
            beforeAdventure && current.campaignState.party.characterIds.isEmpty() -> "READY_TO_START"
            else -> oldGame.session.phase
        }
        val inventory = oldGame.inventory.copy(items = oldGame.inventory.items + items).canonical()
        val expedition = braseiro.ose.rules.shared.InventoryRuntimeRules.derivePartyExpedition(
            current.ruleProfile, current.optionSet, partyIds, inventory, oldGame.expedition
        ).expedition
        val newGame = oldGame.copy(
            characters = oldGame.characters + (c.characterId to c),
            inventory = inventory,
            expedition = expedition,
            loadouts = if (loadout == null) oldGame.loadouts else oldGame.loadouts + (c.characterId to loadout),
            session = oldGame.session.copy(phase = newPhase), revision = oldGame.revision + 1
        ).canonical()
        val newState = current.campaignState.copy(party = PartyState(partyIds), game = newGame)
        return current.copy(campaignState = newState)
    }

    private fun characterFromJson(o: JSONObject): CharacterSnapshot {
        val a=o.getJSONObject("attributes"); val hp=o.getJSONObject("hitPoints"); val sv=o.getJSONObject("savingThrows")
        fun strings(name:String):List<String>{ val x=o.optJSONArray(name)?:JSONArray(); return (0 until x.length()).map{x.getString(it)} }
        fun intMap(name:String):Map<String,Int>{ val x=o.getJSONObject(name); return buildMap{ val k=x.keys(); while(k.hasNext()){ val q=k.next(); put(q,x.getInt(q)) } } }
        fun longMap(name:String):Map<String,Long>{ val x=o.getJSONObject(name); return buildMap{ val k=x.keys(); while(k.hasNext()){ val q=k.next(); put(q,x.getLong(q)) } } }
        return CharacterSnapshot(o.getString("characterId"),o.getString("name"),o.getString("creationMethod"),o.getString("raceId"),strings("classIds"),intMap("levelByClass"),longMap("xpByClass"),
            Attributes(a.getInt("str"),a.getInt("int"),a.getInt("wis"),a.getInt("dex"),a.getInt("con"),a.getInt("cha")),
            HitPoints(hp.getInt("currentNumerator"),hp.getInt("maxNumerator"),hp.optInt("denominator",1)),o.getInt("armorClassDescending"),o.getInt("armorClassAscending"),o.getInt("thac0"),o.getInt("attackBonusAscending"),
            SavingThrows(sv.getInt("deathPoison"),sv.getInt("wands"),sv.getInt("paralysisPetrification"),sv.getInt("breath"),sv.getInt("spellsRodsStaves")),
            strings("languages"),strings("flags").toSet(),strings("evidenceRefs"),intMap("primeModifierPercentByClass"),strings("weaponProficiencies").toSet(),
            o.optString("weaponSpecialization","").ifBlank{null},o.optInt("weaponProficiencySlotsTotal",0),o.optInt("weaponProficiencySlotsSpent",0),strings("secondarySkills")).canonical()
    }

    private fun inventoryItemFromJson(o: JSONObject): InventoryItemSnapshot {
        val w=o.optJSONObject("weapon")?.let { x -> WeaponRuntimeSnapshot(x.optBoolean("melee"),x.optBoolean("missile"),x.optBoolean("slow"),x.optInt("damageDiceCount",1),x.optInt("damageDieSides",6),x.optInt("damageBonus",0),
            x.optInt("shortRangeMeters").takeIf{!x.isNull("shortRangeMeters")},x.optInt("mediumRangeMeters").takeIf{!x.isNull("mediumRangeMeters")},x.optInt("longRangeMeters").takeIf{!x.isNull("longRangeMeters")}) }
        return InventoryItemSnapshot(o.getString("itemId"),o.getString("ownerId"),o.getString("catalogId"),RuleProfile.valueOf(o.getString("profile")),o.getString("sourceId"),InventoryItemKind.valueOf(o.getString("kind")),
            o.optInt("quantity",1),o.optBoolean("carried",true),o.optBoolean("equipped",false),o.optInt("unitWeightCoins",0),
            o.optString("armorLoadCategory","").takeIf{it.isNotBlank()}?.let(ArmorLoadCategory::valueOf),o.optString("lightSourceKind","").takeIf{it.isNotBlank()}?.let(LightSourceKind::valueOf),w)
    }

    private fun safeFileName(name:String):String = name.lowercase().replace(Regex("[^a-z0-9_-]+"),"_").trim('_').ifBlank{"personagem"}

    private fun projectForUi(current: CampaignEnvelope): JSONObject {
        val out = AppViewStateProjector.project(current, selectedCharacterId(current))
        val game = current.campaignState.game
        val portraits = JSONObject()
        current.campaignState.party.characterIds.forEach { id -> mediaStore.url(PresentationMediaStore.Scope.CHARACTER, id)?.let { portraits.put(id, it) } }
        val npcPortraits = JSONObject()
        game.npcs.keys.sorted().forEach { id -> mediaStore.url(PresentationMediaStore.Scope.NPC, id)?.let { npcPortraits.put(id, it) } }
        val presentNpcs = game.npcs.values
            .filter { it.spatialRef != null && it.spatialRef == current.campaignState.position.primary }
            .sortedBy { it.name }
        val itemThumbnails = JSONObject()
        game.inventory.items.keys.sorted().forEach { id -> mediaStore.url(PresentationMediaStore.Scope.ITEM, id)?.let { itemThumbnails.put(id, it) } }
        val sceneId = scenePresentationId(current)
        val selectedCharacterId = selectedCharacterId(current)
        val loadout = selectedCharacterId?.let(game.loadouts::get)
        if (selectedCharacterId != null && loadout != null) {
            val ownedByCatalog = game.inventory.items.values.filter { it.ownerId == selectedCharacterId }.groupBy { it.catalogId }
            out.put("loadout", JSONObject()
                .put("characterId", selectedCharacterId)
                .put("startingWealthGp", loadout.startingWealthGp)
                .put("remainingWealthGp", loadout.remainingWealthGp)
                .put("completed", loadout.completed)
                .put("catalog", JSONArray(StartingEquipmentCatalog.all(current.ruleProfile).map { e ->
                    val owned = ownedByCatalog[e.catalogId].orEmpty()
                    JSONObject().put("catalogId", e.catalogId).put("name", e.displayName)
                        .put("priceGp", e.priceGp ?: JSONObject.NULL).put("purchaseQuantity", e.purchaseQuantity)
                        .put("kind", e.kind.name).put("ownedQuantity", owned.sumOf { it.quantity })
                        .put("itemId", owned.firstOrNull()?.itemId ?: JSONObject.NULL)
                        .put("equipped", owned.any { it.equipped })
                }))
            )
        } else out.put("loadout", JSONObject.NULL)
        out.put("sceneDecisions", ContextualDecisionSuggester.asJson(current))
        latestRuleTraceJson(current)?.let { out.put("lastRuleTrace", it) } ?: out.put("lastRuleTrace", JSONObject.NULL)
        out.put("presentation", JSONObject()
            .put("sceneId", sceneId)
            .put("sceneImageUrl", mediaStore.url(PresentationMediaStore.Scope.SCENE, sceneId) ?: JSONObject.NULL)
            .put("characterPortraits", portraits)
            .put("npcPortraits", npcPortraits)
            .put("presentNpcs", JSONArray(presentNpcs.map { npc ->
                JSONObject()
                    .put("npcId", npc.npcId)
                    .put("name", npc.name)
                    .put("portraitUrl", mediaStore.url(PresentationMediaStore.Scope.NPC, npc.npcId) ?: JSONObject.NULL)
            }))
            .put("itemThumbnails", itemThumbnails))
        val geminiState = credentials.statusJson()
        val runtimeState = runtimeSettings.statusJson()
        run { val keys = runtimeState.keys(); while (keys.hasNext()) { val key = keys.next(); geminiState.put(key, runtimeState.get(key)) } }
        geminiState.put("ttsAudioConfigured", tts.isReady())
        geminiState.put("ttsMode", tts.mode().wireName)
        out.put("gemini", geminiState)
        out.put("tts", ttsStatusJson(includeCatalogs = false))
        adventureStore.getActiveAdventureSummary(current.campaignId.value).let {
            if (it.optBoolean("active", false)) out.put("activeAdventure", it)
        }
        db.lifeStoreDao().findByScope(current.campaignId.value)?.let { entity ->
            if (entity.stateJson.isNotBlank()) {
                runCatching { JSONObject(entity.stateJson) }.getOrNull()?.let { out.put("lifeStore", it) }
            }
        }
        out.put("worldStatus", JSONObject()
            .put("active", game.hexWorld != null && game.npcs.isNotEmpty())
            .put("schedulerActive", true)
            .put("knownHexes", game.hexWorld?.cells?.count { it.known || it.visible } ?: 0)
            .put("npcCount", game.npcs.size)
            .put("scheduledEventCount", game.worldEvents.count { !it.resolved })
            .put("resolvedEventCount", game.worldEvents.count { it.resolved })
            .put("weather", game.livingWorld.weather.kind.name)
            .put("weatherChangedAtDay", game.livingWorld.weather.changedAtDay)
            .put("worldPulseCount", game.livingWorld.pulseCount)
            .put("lastWorldTurn", game.livingWorld.lastProcessedTurn)
            .put("npcActivityCount", game.npcs.values.sumOf { npc -> npc.reactionHistory.count { it.startsWith("WORLD_PULSE:") || it.startsWith("WORLD_EVENT:") } })
            .put("revision", game.revision))
        out.put("libraryStatus", JSONObject()
            .put("profile", current.ruleProfile.name)
            .put("authority", current.authorityRevision.authorityId)
            .put("ruleCount", evidenceRegistry.registeredRuleIds().size)
            .put("embedded", true))
        val guidePrefs = getSharedPreferences("braseiro_ose_player_guide_v1", MODE_PRIVATE)
        val firstLaunch = !guidePrefs.contains("guide_mode")
        out.put("playerGuide", JSONObject()
            .put("mode", playerGuideMode.wireName)
            .put("firstLaunch", firstLaunch)
            .put("topics", JSONArray(OSEPlayerGuide.getAllTopics().map { it.toJson() }))
            .put("contextualTopics", JSONArray(OSEPlayerGuide.getContextualHelp(current.campaignState.game.session.phase, current.campaignState.position.primary.javaClass.simpleName).map { it.toJson() }))
            .put("actionSuggestions", JSONArray(OSEPlayerGuide.getActionSuggestions(current).map { it.toJson() }))
        )
        geminiSupervisor.lastReceipt?.let { r ->
            out.put("narrationReceipt", JSONObject()
                .put("provider", r.provider)
                .put("model", r.model ?: JSONObject.NULL)
                .put("status", r.status)
                .put("reason", r.reason ?: JSONObject.NULL))
        }
        return out
    }

    private fun sendSettingsState() {
        val payload = credentials.statusJson()
        val runtime = runtimeSettings.statusJson()
        run { val keys = runtime.keys(); while (keys.hasNext()) { val key = keys.next(); payload.put(key, runtime.get(key)) } }
        payload.put("ttsAudioConfigured", tts.isReady())
        payload.put("ttsMode", tts.mode().wireName)
        send(JSONObject().put("type", "GeminiSettingsState").put("payload", payload))
        sendTtsSettingsState()
    }

    private fun sendTtsState(stateName: String, error: String?) {
        send(JSONObject().put("type", "TtsState").put("payload", ttsStatusJson(includeCatalogs = false)
            .put("state", stateName)
            .put("message", error ?: JSONObject.NULL)
            .put("configured", tts.isReady())))
    }

    private fun sendTtsSettingsState() {
        send(JSONObject().put("type", "TtsSettingsState").put("payload", ttsStatusJson(includeCatalogs = true)))
    }

    private fun ttsStatusJson(includeCatalogs: Boolean): JSONObject {
        val payload = JSONObject()
            .put("state", tts.state.name)
            .put("ready", tts.isReady())
            .put("enginePackage", tts.selectedEnginePackage() ?: JSONObject.NULL)
            .put("voiceName", tts.selectedVoiceName() ?: JSONObject.NULL)
            .put("speechRate", tts.speechRate())
            .put("mode", tts.mode().wireName)
            .put("error", tts.lastError ?: JSONObject.NULL)
        if (includeCatalogs) {
            payload.put("modes", JSONArray(listOf("GEMINI_TTS", "KOKORO_INSTALLED", "ANDROID_TTS", "OFF")))
            payload.put("engines", JSONArray(tts.engineOptions().map { e ->
                JSONObject()
                    .put("packageName", e.packageName)
                    .put("label", e.label)
                    .put("system", e.system)
                    .put("preferredKokoro", e.preferredKokoro)
            }))
            payload.put("voices", JSONArray(tts.voiceOptions().map { v ->
                JSONObject()
                    .put("name", v.name)
                    .put("localeTag", v.localeTag)
                    .put("quality", v.quality)
                    .put("latency", v.latency)
                    .put("networkRequired", v.networkRequired)
            }))
        }
        return payload
    }

    private fun credentialSlot(value: String): GeminiCredentialStore.Slot = GeminiCredentialStore.Slot.values()
        .firstOrNull { it.wireName == value } ?: error("UNKNOWN_GEMINI_CREDENTIAL_SLOT")

    private fun scenePresentationId(current: CampaignEnvelope): String {
        val game = current.campaignState.game
        val source = buildString {
            append(current.campaignId.value).append('|')
            append(current.campaignState.position.primary.toString()).append('|')
            append(game.encounter?.encounterId ?: "NO_ACTIVE_ENCOUNTER")
        }
        return MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun buildSceneImagePrompt(current: CampaignEnvelope): String {
        val state = current.campaignState
        val game = state.game
        val visibleNarration = game.session.visibleNarration.take(6000)
        val visiblePosition = when (val p = state.position.primary) {
            is SpatialRef.Hex -> {
                val cell = game.hexWorld?.cells?.firstOrNull { it.q == p.q && it.r == p.r }
                "ermos em ${cell?.terrain?.name ?: "terreno desconhecido"}; coordenada pública q=${p.q}, r=${p.r}"
            }
            is SpatialRef.Dungeon -> "área de dungeon conhecida ${p.nodeId}"
            is SpatialRef.Settlement -> "local conhecido ${p.anchorId}"
            is SpatialRef.Scene -> "cena conhecida ${p.sceneId}"
        }
        val party = state.party.characterIds.mapNotNull(game.characters::get).joinToString("; ") { c ->
            "${c.name}, ${c.raceId}, ${c.classIds.joinToString("/")}, nível ${c.levelByClass.values.maxOrNull() ?: 1}"
        }.ifBlank { "companhia sem aparência definida" }
        return """
            Gere UMA ilustração da cena atual de uma campanha de Old-School Essentials.

            CENA VISÍVEL AOS JOGADORES:
            $visibleNarration

            CONTEXTO PÚBLICO:
            - Local: $visiblePosition
            - Tempo de jogo: dia ${state.time.days + 1}, turno ${state.time.turns}
            - Clima atual: ${game.livingWorld.weather.kind.name}
            - Personagens presentes: $party

            DIREÇÃO VISUAL:
            fantasia medieval old-school sombria e orgânica; pintura/gravura tradicional de livro de RPG; textura de tinta e papel; composição cinematográfica horizontal 16:9; iluminação coerente com a narração; ambiente dominante, personagens em escala natural; sem HUD, sem molduras, sem texto, sem letras, sem logotipos, sem aparência de videogame moderno.

            REGRAS DE INFORMAÇÃO:
            represente SOMENTE o que já é visível ou explicitamente conhecido pelos jogadores. Não revele inimigos ocultos, passagens secretas, armadilhas, tesouros, pistas escondidas, NPCs ainda não encontrados ou qualquer informação de bastidor. Não invente acontecimentos novos. Preserve exatamente o clima, terreno, horário, posição e personagens descritos acima.

            Entregue apenas a imagem da cena.
        """.trimIndent()
    }

    private fun buildCharacterPortraitPrompt(current: CampaignEnvelope, characterId: String): String {
        val c = current.campaignState.game.characters[characterId] ?: error("UNKNOWN_CHARACTER_ID")
        val name = c.name
        val race = c.raceId
        val classes = c.classIds.joinToString(" / ")
        val level = c.levelByClass.values.maxOrNull() ?: 1
        return """
            Gere UM retrato/token para o herói $name, de Old-School Essentials.

            DADOS PÚBLICOS E FIXOS:
            - Raça: $race
            - Classe: $classes
            - Nível: $level

            DIREÇÃO VISUAL:
            fantasia medieval old-school; ilustração tradicional de livro de RPG, tinta/papel, sem aparência 3D moderna; enquadramento frontal ou três-quartos, cabeça, ombros e parte superior do torso; personagem rigorosamente centralizado; margem segura generosa em todos os lados para recorte circular; fundo simples e discreto; equipamento coerente com a classe; iluminação legível; identidade visual marcante e reutilizável.

            REGRAS:
            sem texto, sem nome escrito, sem moldura, sem HUD, sem logotipo, sem mãos/armas cortadas nas bordas. Não invente poderes, itens mágicos ou informação mecânica. A imagem será persistida como identidade visual deste personagem.

            Entregue somente a imagem, preferencialmente quadrada 1:1.
        """.trimIndent()
    }

    private fun buildNpcPortraitPrompt(current: CampaignEnvelope, npcId: String): String {
        val npc = current.campaignState.game.npcs[npcId] ?: error("UNKNOWN_NPC_ID")
        val npcName = npc.name
        val publicPlace = when (val p = npc.spatialRef) {
            is SpatialRef.Settlement -> p.anchorId
            is SpatialRef.Dungeon -> "masmorra"
            is SpatialRef.Hex -> "ermos"
            is SpatialRef.Scene -> "cena atual"
            null -> "local conhecido"
        }
        return """
            Gere UM retrato/token persistente para o NPC “$npcName” de uma campanha de Old-School Essentials.

            CONTEXTO PÚBLICO:
            - Nome: $npcName
            - Onde foi encontrado: $publicPlace

            DIREÇÃO VISUAL:
            fantasia medieval old-school; pintura/gravura tradicional de livro de RPG, textura de tinta e papel; cabeça, ombros e parte superior do torso; personagem centralizado e inteiramente dentro de uma margem segura para recorte circular; fundo simples; expressão e roupa coerentes com alguém deste cenário. Crie uma aparência distinta que possa permanecer a mesma em cenas futuras.

            REGRAS:
            não revele segredos, facção oculta, estatísticas, tesouros ou intenções não conhecidas. Sem texto, sem nome escrito, sem moldura, sem HUD, sem logotipo. A imagem será usada como identidade visual persistente do NPC.

            Entregue somente a imagem, preferencialmente quadrada 1:1.
        """.trimIndent()
    }

    private fun launchGeminiWithPrompt(prompt: String, scope: PresentationMediaStore.Scope, stableId: String) {
        rememberExternalImageTarget(scope, stableId)
        runOnUiThread {
            val direct = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, prompt)
                setPackage("com.google.android.apps.bard")
            }
            try {
                startActivity(direct)
            } catch (_: ActivityNotFoundException) {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, prompt)
                }
                startActivity(Intent.createChooser(fallback, "Abrir prompt de ilustração"))
            }
        }
    }

    private fun rememberExternalImageTarget(scope: PresentationMediaStore.Scope, stableId: String) {
        getSharedPreferences("braseiro_ose_external_media_v1", MODE_PRIVATE)
            .edit()
            .putString("scope", scope.name)
            .putString("stableId", stableId)
            .apply()
    }

    private fun consumeExternalImageTarget(current: CampaignEnvelope): Pair<PresentationMediaStore.Scope, String> {
        val prefs = getSharedPreferences("braseiro_ose_external_media_v1", MODE_PRIVATE)
        val scope = runCatching {
            PresentationMediaStore.Scope.valueOf(prefs.getString("scope", "").orEmpty())
        }.getOrNull()
        val stableId = prefs.getString("stableId", "").orEmpty()
        prefs.edit().clear().apply()
        if (scope != null && stableId.isNotBlank()) {
            val valid = when (scope) {
                PresentationMediaStore.Scope.SCENE -> true
                PresentationMediaStore.Scope.CHARACTER -> current.campaignState.game.characters.containsKey(stableId)
                PresentationMediaStore.Scope.NPC -> current.campaignState.game.npcs.containsKey(stableId)
                PresentationMediaStore.Scope.ITEM -> current.campaignState.game.inventory.items.containsKey(stableId)
            }
            if (valid) return scope to stableId
        }
        return PresentationMediaStore.Scope.SCENE to scenePresentationId(current)
    }

    @Suppress("DEPRECATION")
    private fun saveSharedSceneImage(incoming: Intent?, notifyUi: Boolean) {
        if (incoming?.action != Intent.ACTION_SEND || incoming.type?.startsWith("image/") != true) return
        val uri = incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        require(bytes.isNotEmpty() && bytes.size <= 20 * 1024 * 1024) { "IMAGE_SIZE_INVALID" }
        val mime = contentResolver.getType(uri)?.takeIf { it in setOf("image/jpeg", "image/png", "image/webp") } ?: "image/jpeg"
        val current = session.loadSession(CAMPAIGN_ID)
        val target = consumeExternalImageTarget(current)
        val scope = target.first
        val stableId = target.second
        val url = mediaStore.save(scope, stableId, mime, bytes)
        if (notifyUi) {
            val type = if (scope == PresentationMediaStore.Scope.SCENE) "SceneImageResult" else "PresentationImageSaved"
            send(JSONObject().put("type", type).put("payload", JSONObject()
                .put("ok", true).put("url", url).put("scope", scope.name).put("id", stableId)
                .put("sceneId", if (scope == PresentationMediaStore.Scope.SCENE) stableId else JSONObject.NULL)
                .put("source", "ANDROID_SHARE").put("mechanicalDelta", false)
                .put("viewState", projectForUi(current))))
        }
    }

    private fun safeError(t: Throwable): String {
        val message = t.message.orEmpty()
        return when {
            message.startsWith("GEMINI_") -> message
            message.startsWith("INVALID_") || message.startsWith("INCOMPATIBLE_") ||
                message.startsWith("EMPTY_") || message.startsWith("MISSING_") ||
                message.startsWith("ENEMY_") || message.startsWith("SECRET_") -> message
            message in setOf("EMPTY_GEMINI_KEY", "GEMINI_KEY_TOO_LONG", "INVALID_GEMINI_MODEL", "INVALID_TTS_VOICE", "UNKNOWN_GEMINI_CREDENTIAL_SLOT", "UNKNOWN_TTS_COMMAND", "EMPTY_VISIBLE_NARRATION", "INVALID_IMAGE_DATA_URL", "IMAGE_SIZE_INVALID", "UNSUPPORTED_IMAGE_TYPE", "UNKNOWN_CHARACTER_ID", "UNKNOWN_NPC_ID", "UNKNOWN_ITEM_ID", "UNSUPPORTED_PRESENTATION_SCOPE", "UNSUPPORTED_PORTRAIT_KIND", "UNKNOWN_UI_COMMAND", "INVALID_DIE", "INVALID_DICE_COUNT", "INVALID_DICE_MODIFIER", "NO_CHARACTER", "CHARACTER_ALREADY_EXISTS", "RANDOM_CHARACTER_NO_VALID_CLASS", "INVALID_CHARACTER_SHEET", "CHARACTER_PROFILE_MISMATCH", "CHARACTER_ID_EXISTS", "CHARACTER_ITEM_PROFILE_MISMATCH", "ITEM_ID_EXISTS", "INVALID_CHARACTER_NAME", "CHARACTER_RECREATE_AFTER_START", "INVALID_CHARACTER_COMMAND", "CHARACTER_COMMAND_ALREADY_APPLIED", "CHARACTER_SELECTION_SAVE_FAILED", "CHARACTER_PLAYER_CONTROLLED", "CHARACTER_MASTER_CONTROLLED", "CHARACTER_CURATOR_APPROVAL_REQUIRED", "CHARACTER_NOT_CURATED", "UNKNOWN_PROPOSAL", "INVALID_PROPOSAL_ID", "INVALID_PROPOSAL_CONTENT", "PROPOSAL_ID_COLLISION", "PROPOSAL_ALREADY_REVIEWED", "CHARACTER_ACTOR_PROTECTED", "GROUP_ACTION_REQUIRES_PLAYER_DECISION") -> message
            else -> t::class.java.simpleName
        }
    }

    private fun send(obj: JSONObject) {
        runOnUiThread {
            if (::webView.isInitialized) {
                val quoted = JSONObject.quote(obj.toString())
                webView.evaluateJavascript("window.BraseiroReceive && window.BraseiroReceive($quoted);", null)
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        if (::geminiTts.isInitialized) geminiTts.shutdown()
        if (::tts.isInitialized) tts.shutdown()
        if (::db.isInitialized) db.close()
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
