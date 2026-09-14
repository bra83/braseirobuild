package braseiro.ose.app

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
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
import braseiro.ose.rules.shared.StartingEquipmentCatalog
import braseiro.ose.session.SessionEngine
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    companion object {
        const val LOCAL_APP_ORIGIN = "https://appassets.androidplatform.net/assets/"
        const val START_URL = "${LOCAL_APP_ORIGIN}index.html?screen=session&state=active"
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
    private lateinit var tts: GeminiTtsController
    private val manualDice = SecureRandom()
    private val evidenceRegistry = RuleEvidenceRegistry()
    private val creationCoordinator = NativeCharacterCreationCoordinator()
    private val loadoutCoordinator = NativeCharacterLoadoutCoordinator()
    private val io = Executors.newSingleThreadExecutor()

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
        ).addMigrations(MIGRATION_1_2).build()
        repository = RoomCampaignRepository(db)
        credentials = GeminiCredentialStore(applicationContext)
        runtimeSettings = GeminiRuntimeSettings(applicationContext)
        mediaStore = PresentationMediaStore(applicationContext)
        gemini = GeminiInteractionsClient()
        session = SessionEngine(repository, RulesRefereeBoundary(), GeminiBarbaraSupervisor(credentials, runtimeSettings, gemini))
        tts = GeminiTtsController(applicationContext, credentials, runtimeSettings, gemini)

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
        setContentView(webView)
        webView.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }
        webView.requestApplyInsets()

        io.execute {
            bootstrapIfMissing()
            runOnUiThread {
                if (!isFinishing && !isDestroyed) webView.loadUrl(START_URL)
            }
        }
    }

    private fun bootstrapIfMissing() {
        when (val loaded = repository.load(CAMPAIGN_ID)) {
            is CampaignLoadResult.NotFound -> {
                val bootstrapOptions = OptionSet(listOf("OPT.MORALE", "OPT.ENCUMBRANCE=BASIC"))
                check(braseiro.ose.rules.api.OptionPolicyContract.resolve(RuleProfile.OSE_ADVANCED_FANTASY, bootstrapOptions).valid)
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
                    campaignState = buildWorldBootstrap(RuleProfile.OSE_ADVANCED_FANTASY)
                )
                repository.create(envelope)
            }
            is CampaignLoadResult.Loaded -> {
                var repaired = ensureWorldBootstrap(loaded.envelope)
                repaired = repairLegacyPresentation(repaired)
                if (repaired != loaded.envelope) {
                    repository.commit(CAMPAIGN_ID, StateTransition("runtime-repair-v2", repaired.campaignState))
                }
            }
            is CampaignLoadResult.ReadFailure -> throw loaded.cause
        }
    }

    private fun buildWorldBootstrap(profile: RuleProfile): CampaignState {
        val worldSeed = 0x0B5E1AUL
        val dungeonSeed = 0x0D06E0UL
        val generated = HexWorldGeneratorV1.generate("world-1", worldSeed, profile)
        val coherence = GeographicCoherenceValidator.validate(generated, profile)
        check(coherence.pass) { "HEX_BOOTSTRAP_INVALID:${coherence.errors.joinToString()}" }
        val world = HexKnowledge.reveal(generated, 3, 2, 1)
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
            position = PositionState(SpatialRef.Hex("world-1", 3, 2)),
            resources = ResourceState(),
            playerKnowledge = PlayerKnowledgeState(listOf("hex:3,2")),
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
                            val result = session.submitPlayerReaction(CAMPAIGN_ID, reactionId, text, mechanicalIntent)
                            val current = session.loadSession(CAMPAIGN_ID)
                            send(
                                JSONObject().put("type", "SessionUpdate").put("payload", JSONObject()
                                    .put("narration", result.narration)
                                    .put("feedback", result.feedback)
                                    .put("committed", true)
                                    .put("mechanicalMutation", result.mechanicalMutation)
                                    .put("stateHash", CanonicalStateHash.sha256(current))
                                    .put("viewState", projectForUi(current)))
                            )
                        }

                        "StartAdventure" -> {
                            val result = session.startAdventure(CAMPAIGN_ID)
                            val current = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type", "SessionUpdate").put("payload", JSONObject()
                                .put("narration", result.narration)
                                .put("feedback", "")
                                .put("committed", true)
                                .put("mechanicalMutation", false)
                                .put("stateHash", CanonicalStateHash.sha256(current))
                                .put("viewState", projectForUi(current))))
                        }

                        "GMHelp" -> {
                            val result = session.gmHelp(CAMPAIGN_ID, payload.optString("question", ""))
                            send(JSONObject().put("type", "GMHelpResponse").put("payload", JSONObject()
                                .put("answer", result.answer).put("stateRevision", result.stateRevision)))
                        }

                        "TtsCommand" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            // The WebView is not trusted to choose what TTS reads. Only canonical visible narration is accepted.
                            val result = when (payload.optString("command")) {
                                "play" -> tts.playVisibleNarration(current.campaignState.game.session.visibleNarration)
                                "replay" -> tts.replayVisibleNarration(current.campaignState.game.session.visibleNarration)
                                "stop" -> tts.stop()
                                else -> error("UNKNOWN_TTS_COMMAND")
                            }
                            send(JSONObject().put("type", "TtsState").put("payload", JSONObject()
                                .put("state", result.name)
                                .put("configured", credentials.has(GeminiCredentialStore.Slot.TTS_AUDIO))))
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
                            val key = credentials.get(slot) ?: error("GEMINI_KEY_MISSING")
                            val runtime = runtimeSettings.snapshot()
                            val ok = when (slot) {
                                GeminiCredentialStore.Slot.TEXT_SCENE -> gemini.testCredential(key, runtime.textModel)
                                GeminiCredentialStore.Slot.TTS_AUDIO -> gemini.testTtsCredential(key, runtime.ttsModel, runtime.ttsVoice)
                            }
                            send(JSONObject().put("type", "GeminiCredentialTestResult").put("payload", JSONObject()
                                .put("slot", slot.wireName).put("ok", ok)))
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

                        "PresentationImageSave" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            val scope = when (payload.getString("scope")) {
                                "CHARACTER" -> PresentationMediaStore.Scope.CHARACTER
                                "ITEM" -> PresentationMediaStore.Scope.ITEM
                                else -> error("UNSUPPORTED_PRESENTATION_SCOPE")
                            }
                            val stableId = payload.getString("id").trim()
                            when (scope) {
                                PresentationMediaStore.Scope.CHARACTER -> require(current.campaignState.game.characters.containsKey(stableId)) { "UNKNOWN_CHARACTER_ID" }
                                PresentationMediaStore.Scope.ITEM -> require(current.campaignState.game.inventory.items.containsKey(stableId)) { "UNKNOWN_ITEM_ID" }
                                else -> error("UNSUPPORTED_PRESENTATION_SCOPE")
                            }
                            val url = mediaStore.saveDataUrl(scope, stableId, payload.getString("dataUrl"))
                            send(JSONObject().put("type", "PresentationImageSaved").put("payload", JSONObject()
                                .put("scope", scope.name).put("id", stableId).put("url", url).put("mechanicalDelta", false)))
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
                                .put("ruleId",ruleId).put("text",result.text).put("status",result.trace.status.name).put("evidenceCount",result.trace.evidence.size)))
                        }

                        "BackupExport" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            // Gemini credentials are in noBackupFilesDir and not part of CampaignEnvelope/BackupCodec.
                            send(JSONObject().put("type","BackupExportResult").put("payload",JSONObject()
                                .put("campaignId",current.campaignId.value).put("backup",BackupCodec.exportCampaign(current))))
                        }

                        "BackupImport" -> {
                            val imported = BackupCodec.importCampaign(payload.getString("backup"))
                            require(imported.campaignId == CAMPAIGN_ID) { "Backup campaign id mismatch" }
                            val policy = braseiro.ose.rules.api.OptionPolicyContract.resolve(imported.ruleProfile, imported.optionSet)
                            require(policy.valid) { "Invalid backup option policy: ${policy.errors.joinToString()}" }
                            repository.replace(imported)
                            val current = session.loadSession(CAMPAIGN_ID)
                            send(JSONObject().put("type","BackupImportResult").put("payload",JSONObject()
                                .put("ok",true).put("campaignId",current.campaignId.value)
                                .put("stateHash",CanonicalStateHash.sha256(current)).put("viewState",projectForUi(current))))
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
                            val outcome = creationCoordinator.commit(current,draftId,characterId,name,method,classIds,raceId,adjustments)
                            when (val result = outcome.result) {
                                is CharacterCreationResult.Created -> {
                                    if (outcome.stateChanged) repository.commit(CAMPAIGN_ID,StateTransition("character-create:$characterId",outcome.envelope.campaignState))
                                    val persisted = session.loadSession(CAMPAIGN_ID)
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

    private fun projectForUi(current: CampaignEnvelope): JSONObject {
        val out = AppViewStateProjector.project(current)
        val game = current.campaignState.game
        val portraits = JSONObject()
        current.campaignState.party.characterIds.forEach { id -> mediaStore.url(PresentationMediaStore.Scope.CHARACTER, id)?.let { portraits.put(id, it) } }
        val itemThumbnails = JSONObject()
        game.inventory.items.keys.sorted().forEach { id -> mediaStore.url(PresentationMediaStore.Scope.ITEM, id)?.let { itemThumbnails.put(id, it) } }
        val sceneId = scenePresentationId(current)
        val selectedCharacterId = current.campaignState.party.characterIds.firstOrNull()
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
        out.put("presentation", JSONObject()
            .put("sceneId", sceneId)
            .put("sceneImageUrl", mediaStore.url(PresentationMediaStore.Scope.SCENE, sceneId) ?: JSONObject.NULL)
            .put("characterPortraits", portraits)
            .put("itemThumbnails", itemThumbnails))
        val geminiState = credentials.statusJson()
        val runtimeState = runtimeSettings.statusJson()
        run { val keys = runtimeState.keys(); while (keys.hasNext()) { val key = keys.next(); geminiState.put(key, runtimeState.get(key)) } }
        out.put("gemini", geminiState)
        out.put("worldStatus", JSONObject()
            .put("active", game.hexWorld != null && game.npcs.isNotEmpty())
            .put("schedulerActive", game.worldEvents.isNotEmpty())
            .put("knownHexes", game.hexWorld?.cells?.count { it.known || it.visible } ?: 0))
        out.put("libraryStatus", JSONObject()
            .put("profile", current.ruleProfile.name)
            .put("authority", current.authorityRevision.authorityId)
            .put("embedded", true))
        return out
    }

    private fun sendSettingsState() {
        val payload = credentials.statusJson()
        val runtime = runtimeSettings.statusJson()
        run { val keys = runtime.keys(); while (keys.hasNext()) { val key = keys.next(); payload.put(key, runtime.get(key)) } }
        send(JSONObject().put("type", "GeminiSettingsState").put("payload", payload))
    }

    private fun credentialSlot(value: String): GeminiCredentialStore.Slot = GeminiCredentialStore.Slot.values()
        .firstOrNull { it.wireName == value } ?: error("UNKNOWN_GEMINI_CREDENTIAL_SLOT")

    private fun scenePresentationId(current: CampaignEnvelope): String {
        val session = current.campaignState.game.session
        val source = buildString {
            append(current.campaignId.value).append('|')
            append(session.phase).append('|').append(session.visibleNarration).append('|')
            append(current.campaignState.position.primary.toString())
        }
        return MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun buildSceneImagePrompt(current: CampaignEnvelope): String {
        val state = current.campaignState
        val visibleNarration = state.game.session.visibleNarration.take(5000)
        val visiblePosition = when (val p = state.position.primary) {
            is SpatialRef.Hex -> "hex conhecido q=${p.q}, r=${p.r}"
            is SpatialRef.Dungeon -> "área de dungeon conhecida ${p.nodeId}"
            is SpatialRef.Settlement -> "local conhecido ${p.anchorId}"
            is SpatialRef.Scene -> "cena conhecida ${p.sceneId}"
        }
        return """
            Crie uma ilustração de cena para um VTT de Old-School Essentials, fantasia medieval sombria e orgânica, pintura/gravura tradicional, sem HUD, sem texto e sem aparência de videogame moderno. A imagem deve representar SOMENTE informações visíveis aos jogadores. Não acrescente inimigos, passagens, tesouros, armadilhas, objetos ocultos ou pistas que não estejam explicitamente descritos. Contexto público: $visiblePosition. Narração visível do Mestre: $visibleNarration. Composição horizontal 4:3, legível em tela móvel.
        """.trimIndent()
    }

    private fun safeError(t: Throwable): String {
        val message = t.message.orEmpty()
        return when {
            message.startsWith("GEMINI_") -> message
            message in setOf("EMPTY_GEMINI_KEY", "GEMINI_KEY_TOO_LONG", "INVALID_GEMINI_MODEL", "INVALID_TTS_VOICE", "UNKNOWN_GEMINI_CREDENTIAL_SLOT", "UNKNOWN_TTS_COMMAND", "EMPTY_VISIBLE_NARRATION", "INVALID_IMAGE_DATA_URL", "IMAGE_SIZE_INVALID", "UNSUPPORTED_IMAGE_TYPE", "UNKNOWN_CHARACTER_ID", "UNKNOWN_ITEM_ID", "UNSUPPORTED_PRESENTATION_SCOPE", "UNKNOWN_UI_COMMAND", "INVALID_DIE", "INVALID_DICE_COUNT", "INVALID_DICE_MODIFIER") -> message
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
        if (::tts.isInitialized) tts.shutdown()
        if (::db.isInitialized) db.close()
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
