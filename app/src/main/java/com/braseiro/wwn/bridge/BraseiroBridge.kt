package com.braseiro.wwn.bridge

import android.webkit.JavascriptInterface
import com.braseiro.wwn.data.CampaignLoadResult
import com.braseiro.wwn.data.CampaignRepository
import com.braseiro.wwn.data.CommitRequest
import com.braseiro.wwn.data.NarrativeAppendRequest
import com.braseiro.wwn.data.PortableRestoreEvent
import com.braseiro.wwn.data.PortableRestorePayload
import com.braseiro.wwn.data.PortableRestoreSnapshot
import com.braseiro.wwn.data.CampaignRestoreConflictException
import com.braseiro.wwn.data.CampaignRestoreValidationException
import org.json.JSONArray
import org.json.JSONObject

class BraseiroBridge(
    private val repository: CampaignRepository,
    private val fileActions: NativeFileActions? = null,
    private val geminiRuntime: GeminiRuntime? = null,
    private val ragRuntime: RagRuntime? = null,
    private val ttsRuntime: NativeTtsRuntime? = null,
    private val audioRuntime: NativeAudioRuntime? = null,
    private val diceActions: NativeDiceActions? = null,
) {
    private val CREDENTIAL_KEY = Regex("api[_-]?key|access[_-]?token|refresh[_-]?token|credential|password|authorization", RegexOption.IGNORE_CASE)


    @JavascriptInterface
    fun geminiStatus(): String = try {
        (geminiRuntime?.status() ?: JSONObject().put("status", "UNAVAILABLE").put("configured", false)).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("configured", false).put("reason", t.message).toString()
    }

    @JavascriptInterface
    fun setGeminiApiKey(apiKey: String, model: String): String {
        return try {
        val runtime = geminiRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        runtime.setApiKey(apiKey, model.takeIf { it.isNotBlank() }).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun setGeminiModel(model: String): String {
        return try {
        val runtime = geminiRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        runtime.setModel(model).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun setGeminiImageModel(model: String): String {
        return try {
            val runtime = geminiRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
            runtime.setImageModel(model).toString()
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message).toString()
        }
    }

    @JavascriptInterface
    fun setGeminiVisionEnabled(enabled: Boolean): String {
        return try {
            val runtime = geminiRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
            runtime.setVisionEnabled(enabled).toString()
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message).toString()
        }
    }

    @JavascriptInterface
    fun setGeminiTtsConfig(model: String, voice: String, provider: String): String {
        return try {
            val runtime = geminiRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
            runtime.setTtsConfig(model, voice, provider).toString()
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message).toString()
        }
    }

    @JavascriptInterface
    fun clearGeminiApiKey(): String {
        return try {
        val runtime = geminiRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        runtime.clearApiKey().toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun geminiGenerateText(requestJson: String): String {
        return try {
        val runtime = geminiRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        runtime.generateText(requestJson).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun geminiGenerateTextAsync(requestJson: String): String {
        return try {
            val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
            actions.requestGeminiTextAsync(requestJson)
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message).toString()
        }
    }

    @JavascriptInterface
    fun geminiGenerateImageAsync(requestJson: String): String {
        return try {
            val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
            actions.requestGeminiImageAsync(requestJson)
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message).toString()
        }
    }

    @JavascriptInterface
    fun ragStatus(): String = try {
        (ragRuntime?.status() ?: JSONObject().put("status", "UNAVAILABLE").put("documentCount", 0)).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun listRagDocuments(): String = try {
        (ragRuntime?.listDocuments() ?: JSONObject().put("status", "UNAVAILABLE").put("documents", JSONArray())).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).put("documents", JSONArray()).toString() }

    @JavascriptInterface
    fun ragSearch(requestJson: String): String = try {
        (ragRuntime?.search(requestJson) ?: JSONObject().put("status", "UNAVAILABLE").put("results", JSONArray()).put("mechanicalAuthority", false)).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).put("results", JSONArray()).put("mechanicalAuthority", false).toString() }

    @JavascriptInterface
    fun ragSearchAsync(requestJson: String): String {
        return try {
            val actions = fileActions
            if (actions == null) JSONObject().put("status", "UNAVAILABLE").put("results", JSONArray()).put("mechanicalAuthority", false).toString()
            else actions.requestRagSearchAsync(requestJson)
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message).put("results", JSONArray()).put("mechanicalAuthority", false).toString()
        }
    }

    @JavascriptInterface
    fun removeRagDocument(documentId: String): String = try {
        (ragRuntime?.removeDocument(documentId) ?: JSONObject().put("status", "UNAVAILABLE")).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun openRulesPdf(authorityProfile: String): String {
        return try {
        val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        require(authorityProfile.uppercase() in setOf("WWN_RULEBOOK_AUTHORIZED", "CAMPAIGN_REFERENCE", "GM_PRIVATE_REFERENCE")) { "Invalid authority profile" }
        actions.requestOpenRulesPdf(authorityProfile.uppercase())
        JSONObject().put("status", "PENDING").put("authorityProfile", authorityProfile.uppercase()).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }
    }

    @JavascriptInterface
    fun ttsStatus(): String = try {
        (ttsRuntime?.status() ?: JSONObject().put("status", "UNAVAILABLE").put("ready", false)).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("ready", false).put("reason", t.message).toString() }

    @JavascriptInterface
    fun ttsConfigure(requestJson: String): String {
        return try {
            val runtime = ttsRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
            runtime.configure(requestJson).toString()
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message).toString()
        }
    }

    @JavascriptInterface
    fun ttsSpeak(requestJson: String): String {
        return try {
        val runtime = ttsRuntime ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        runtime.speak(requestJson).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }
    }

    @JavascriptInterface
    fun ttsStop(): String = try {
        (ttsRuntime?.stop() ?: JSONObject().put("status", "UNAVAILABLE")).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun audioStatus(): String = try {
        (audioRuntime?.status() ?: JSONObject().put("status", "UNAVAILABLE").put("ready", false)).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("ready", false).put("reason", t.message).toString() }

    @JavascriptInterface
    fun audioConfigure(requestJson: String): String = try {
        audioRuntime?.configure(requestJson)?.toString() ?: JSONObject().put("status", "UNAVAILABLE").toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun audioApplyScenePlan(planJson: String): String = try {
        audioRuntime?.applyScenePlan(planJson)?.toString() ?: JSONObject().put("status", "UNAVAILABLE").toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun audioSetNarrationActive(active: Boolean): String = try {
        (audioRuntime?.setNarrationActive(active) ?: JSONObject().put("status", "UNAVAILABLE")).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun audioStopAll(): String = try {
        (audioRuntime?.stopAll() ?: JSONObject().put("status", "UNAVAILABLE")).toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun startPhysicalDice(requestJson: String): String {
        return try {
            val actions = diceActions ?: return JSONObject().put("status", "UNAVAILABLE").put("reason", "DADOS_3D_INDISPONIVEIS").toString()
            require(requestJson.length <= 4096) { "Pedido de dados 3D excede o limite" }
            actions.requestPhysicalDice(requestJson)
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message ?: "Falha ao iniciar dados 3D").toString()
        }
    }

    @JavascriptInterface
    fun openSceneImage(): String {
        return try {
        val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        actions.requestOpenSceneImage()
        JSONObject().put("status", "PENDING").toString()
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).toString() }
    }

    @JavascriptInterface
    fun shareText(title: String, text: String): String {
        return try {
            val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
            require(title.length <= 160) { "Título muito longo" }
            require(text.length in 1..12000) { "Texto para compartilhar fora do limite" }
            actions.requestShareText(title.ifBlank { "Braseiro WWN" }, text)
            JSONObject().put("status", "OK").toString()
        } catch (t: Throwable) {
            JSONObject().put("status", "ERROR").put("reason", t.message ?: "Falha ao compartilhar texto").toString()
        }
    }

    @JavascriptInterface
    fun openCharacterJson(): String {
        return try {
        val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        actions.requestOpenCharacterJson()
        JSONObject().put("status", "PENDING").toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun saveCharacterJson(json: String, suggestedName: String): String {
        return try {
        val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        require(json.length <= 2_000_000) { "Character JSON too large" }
        actions.requestSaveCharacterJson(json, suggestedName)
        JSONObject().put("status", "PENDING").toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun openBackupJson(): String {
        return try {
        val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        actions.requestOpenBackupJson()
        JSONObject().put("status", "PENDING").toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun saveBackupJson(json: String, suggestedName: String): String {
        return try {
        val actions = fileActions ?: return JSONObject().put("status", "UNAVAILABLE").toString()
        require(json.length <= 50_000_000) { "Backup JSON too large" }
        actions.requestSaveBackupJson(json, suggestedName)
        JSONObject().put("status", "PENDING").toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }
    }

    @JavascriptInterface
    fun capabilities(): String = JSONObject()
        .put("persistence", "ROOM_SQLITE")
        .put("dbSchemaVersion", CampaignRepository.DB_SCHEMA_VERSION)
        .put("rngAlgorithm", CampaignRepository.RNG_ALGORITHM)
        .put("canonicalStorage", true)
        .put("characterJsonFiles", fileActions != null)
        .put("backupJsonFiles", fileActions != null)
        .put("portableBackupRestore", true)
        .put("activeCampaign", true)
        .put("geminiNative", geminiRuntime != null)
        .put("geminiImageGeneration", geminiRuntime != null && fileActions != null)
        .put("pdfRag", ragRuntime != null)
        .put("ttsNative", ttsRuntime != null)
        .put("audioSession", audioRuntime != null)
        .put("audioProtocol", "barbara-audio-scene-plan-v1")
        .put("sceneImageImport", fileActions != null)
        .put("textShare", fileActions != null)
        .put("physicalDice3d", diceActions != null)
        .put("physicalDiceAuthority", "FACE_SUPERIOR_APOS_ASSENTAMENTO")
        .put("ragMechanicalAuthority", false)
        .put("geminiSecretExposedToJavascript", false)
        .toString()

    @JavascriptInterface
    fun getActiveCampaignId(): String = try {
        JSONObject().put("status", "OK").put("campaignId", repository.getActiveCampaignId()).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }

    @JavascriptInterface
    fun setActiveCampaignId(campaignId: String): String = try {
        repository.setActiveCampaignId(campaignId)
        JSONObject().put("status", "OK").put("campaignId", campaignId).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }

    @JavascriptInterface
    fun listCampaigns(): String = try {
        val items = JSONArray()
        repository.listCampaigns().forEach { meta ->
            items.put(
                JSONObject()
                    .put("campaignId", meta.campaignId)
                    .put("displayName", meta.displayName)
                    .put("revision", meta.revision)
                    .put("checksum", meta.stateChecksum)
                    .put("updatedAtAudit", meta.updatedAtAudit),
            )
        }
        JSONObject().put("status", "OK").put("campaigns", items).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "IO_ERROR").put("reason", t.message).toString()
    }

    @JavascriptInterface
    fun loadCampaign(campaignId: String): String = when (val result = repository.loadCampaign(campaignId)) {
        is CampaignLoadResult.Ok -> JSONObject()
            .put("status", "OK")
            .put("campaignId", result.campaignId)
            .put("displayName", result.displayName)
            .put("revision", result.revision)
            .put("stateJson", result.stateJson)
            .put("checksum", result.checksum)
            .put("rngAlgorithm", result.rngAlgorithm)
            .put("masterSeed", result.masterSeed)
            .toString()
        CampaignLoadResult.NotFound -> JSONObject().put("status", "NOT_FOUND").toString()
        is CampaignLoadResult.Corrupt -> JSONObject().put("status", "CORRUPT").put("reason", result.reason).toString()
        is CampaignLoadResult.IoError -> JSONObject().put("status", "IO_ERROR").put("reason", result.reason).toString()
    }

    @JavascriptInterface
    fun createCampaign(requestJson: String): String = try {
        val request = JSONObject(requestJson)
        val receipt = repository.createCampaign(
                campaignId = request.getString("campaignId"),
                displayName = request.getString("displayName"),
                stateJson = request.getString("stateJson"),
                masterSeed = request.getString("masterSeed"),
                rngAlgorithm = request.optString("rngAlgorithm", CampaignRepository.RNG_ALGORITHM),
                initialSnapshotId = request.getString("initialSnapshotId"),
                auditTimeMs = System.currentTimeMillis(),
            )
        receiptJson(receipt).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }

    @JavascriptInterface
    fun commit(requestJson: String): String = try {
        val request = JSONObject(requestJson)
        val receipt = repository.commit(
                CommitRequest(
                    campaignId = request.getString("campaignId"),
                    expectedRevision = request.getLong("expectedRevision"),
                    eventId = request.getString("eventId"),
                    eventKind = request.getString("eventKind"),
                    eventPayloadJson = request.getString("eventPayloadJson"),
                    newStateJson = request.getString("newStateJson"),
                    snapshotId = request.optString("snapshotId").takeIf { it.isNotBlank() },
                    rngCountersJson = request.optString("rngCountersJson", "{}"),
                ),
                auditTimeMs = System.currentTimeMillis(),
            )
        receiptJson(receipt).toString()
    } catch (t: Throwable) {
        val status = if (t::class.java.simpleName.contains("RevisionConflict")) "REVISION_CONFLICT" else "ERROR"
        JSONObject().put("status", status).put("reason", t.message).toString()
    }

    @JavascriptInterface
    fun appendNarrative(requestJson: String): String = try {
        val request = JSONObject(requestJson)
        val receipt = repository.appendNarrative(
            NarrativeAppendRequest(
                campaignId = request.getString("campaignId"),
                eventId = request.getString("eventId"),
                mechanicalEventId = request.getString("mechanicalEventId"),
                mechanicalRevision = request.getLong("mechanicalRevision"),
                eventPayloadJson = request.getString("eventPayloadJson"),
            ),
            auditTimeMs = System.currentTimeMillis(),
        )
        JSONObject()
            .put("status", "OK")
            .put("campaignId", receipt.campaignId)
            .put("revision", receipt.revision)
            .put("eventId", receipt.eventId)
            .put("idempotent", receipt.idempotent)
            .toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }

    @JavascriptInterface
    fun getEvent(eventId: String): String = try {
        val event = repository.getEvent(eventId)
        if (event == null) JSONObject().put("status", "NOT_FOUND").toString()
        else JSONObject().put("status", "OK").put("event", eventJson(event)).toString()
    } catch (t: Throwable) { JSONObject().put("status", "IO_ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun latestNarrative(campaignId: String, mechanicalEventId: String): String = try {
        val event = repository.latestNarrative(campaignId, mechanicalEventId)
        if (event == null) JSONObject().put("status", "NOT_FOUND").toString()
        else JSONObject().put("status", "OK").put("event", eventJson(event)).toString()
    } catch (t: Throwable) { JSONObject().put("status", "IO_ERROR").put("reason", t.message).toString() }

    @JavascriptInterface
    fun listEvents(campaignId: String): String = try {
        val items = JSONArray()
        repository.listEvents(campaignId).forEach { event ->
            items.put(
                JSONObject()
                    .put("eventId", event.eventId)
                    .put("campaignId", event.campaignId)
                    .put("revision", event.revision)
                    .put("eventKind", event.eventKind)
                    .put("payloadJson", event.payloadJson)
                    .put("checksumBefore", event.checksumBefore)
                    .put("checksumAfter", event.checksumAfter)
                    .put("createdAtAudit", event.createdAtAudit),
            )
        }
        JSONObject().put("status", "OK").put("events", items).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).put("events", JSONArray()).toString()
    }

    @JavascriptInterface
    fun listSnapshots(campaignId: String): String = try {
        val items = JSONArray()
        repository.listSnapshots(campaignId).forEach { snap ->
            items.put(
                JSONObject()
                    .put("snapshotId", snap.snapshotId)
                    .put("campaignId", snap.campaignId)
                    .put("revision", snap.revision)
                    .put("stateJson", snap.stateJson)
                    .put("checksum", snap.checksum)
                    .put("schemaVersion", snap.schemaVersion)
                    .put("rngAlgorithm", snap.rngAlgorithm)
                    .put("rngCountersJson", snap.rngCountersJson)
                    .put("lastEventId", snap.lastEventId)
                    .put("createdAtAudit", snap.createdAtAudit),
            )
        }
        JSONObject().put("status", "OK").put("snapshots", items).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).put("snapshots", JSONArray()).toString()
    }

    @JavascriptInterface
    fun restorePortableBackup(backupJson: String, overwrite: Boolean): String = try {
        require(backupJson.length <= 50_000_000) { "Backup JSON too large" }
        val backup = JSONObject(backupJson)
        require(backup.optInt("backupVersion", -1) == 1) { "Unsupported backup version" }
        require(backup.optString("system") == "WWN") { "Backup is not WWN" }
        rejectCredentialKeys(backup)
        val payload = parsePortableRestorePayload(backup)
        val receipt = repository.restorePortableBackup(payload, overwrite, System.currentTimeMillis())
        JSONObject()
            .put("status", "OK")
            .put("campaignId", receipt.campaignId)
            .put("revision", receipt.revision)
            .put("stateChecksum", receipt.stateChecksum)
            .put("eventsRestored", receipt.eventsRestored)
            .put("snapshotsRestored", receipt.snapshotsRestored)
            .put("overwritten", receipt.overwritten)
            .toString()
    } catch (t: CampaignRestoreConflictException) {
        val reason = t.message ?: "RESTORE_CONFLICT"
        JSONObject().put("status", if (reason == "TARGET_CAMPAIGN_EXISTS") reason else "RESTORE_CONFLICT").put("reason", reason).toString()
    } catch (t: CampaignRestoreValidationException) {
        JSONObject().put("status", "INVALID_BACKUP_CONTENT").put("reason", t.message).toString()
    } catch (t: Throwable) {
        JSONObject().put("status", "ERROR").put("reason", t.message).toString()
    }

    private fun parsePortableRestorePayload(backup: JSONObject): PortableRestorePayload {
        val campaignId = backup.getString("campaignId")
        val revision = backup.getLong("revision")
        val stateJson = backup.optString("stateJson").ifBlank { backup.getJSONObject("state").toString() }
        val state = JSONObject(stateJson)
        require(state.optString("campaignId") == campaignId) { "State campaignId mismatch" }
        require(state.optLong("revision", -1) == revision) { "State revision mismatch" }
        rejectCredentialKeys(state)

        val eventsJson = backup.optJSONArray("events") ?: JSONArray()
        val events = buildList {
            for (i in 0 until eventsJson.length()) {
                val e = eventsJson.getJSONObject(i)
                val payloadJson = e.optString("payloadJson", "{}")
                rejectCredentialJsonString(payloadJson)
                add(
                    PortableRestoreEvent(
                        eventId = e.getString("eventId"),
                        campaignId = e.optString("campaignId", campaignId),
                        revision = e.getLong("revision"),
                        eventKind = e.getString("eventKind"),
                        payloadJson = payloadJson,
                        checksumBefore = if (e.isNull("checksumBefore") || !e.has("checksumBefore")) null else e.getString("checksumBefore"),
                        checksumAfter = e.getString("checksumAfter"),
                        createdAtAudit = e.optLong("createdAtAudit", 0L),
                    ),
                )
            }
        }

        val snapshotsJson = backup.optJSONArray("snapshots") ?: JSONArray()
        val snapshots = buildList {
            for (i in 0 until snapshotsJson.length()) {
                val snap = snapshotsJson.getJSONObject(i)
                val snapStateJson = snap.getString("stateJson")
                rejectCredentialJsonString(snapStateJson)
                add(
                    PortableRestoreSnapshot(
                        snapshotId = snap.getString("snapshotId"),
                        campaignId = snap.optString("campaignId", campaignId),
                        revision = snap.getLong("revision"),
                        stateJson = snapStateJson,
                        rngCountersJson = snap.optString("rngCountersJson", "{}"),
                        lastEventId = if (snap.isNull("lastEventId") || !snap.has("lastEventId")) null else snap.getString("lastEventId"),
                        createdAtAudit = snap.optLong("createdAtAudit", 0L),
                    ),
                )
            }
        }
        return PortableRestorePayload(
            campaignId = campaignId,
            displayName = backup.getString("displayName"),
            revision = revision,
            stateJson = stateJson,
            masterSeed = backup.getString("masterSeed"),
            rngAlgorithm = backup.getString("rngAlgorithm"),
            events = events,
            snapshots = snapshots,
        )
    }

    private fun rejectCredentialJsonString(text: String) {
        runCatching { JSONObject(text) }.getOrNull()?.let { rejectCredentialKeys(it) }
    }

    private fun rejectCredentialKeys(value: Any?) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (CREDENTIAL_KEY.containsMatchIn(key)) throw CampaignRestoreValidationException("Credential-bearing key is forbidden in portable backup")
                    rejectCredentialKeys(value.opt(key))
                }
            }
            is JSONArray -> for (i in 0 until value.length()) rejectCredentialKeys(value.opt(i))
        }
    }

    private fun eventJson(event: com.braseiro.wwn.data.CampaignEventEntity): JSONObject = JSONObject()
        .put("eventId", event.eventId)
        .put("campaignId", event.campaignId)
        .put("revision", event.revision)
        .put("eventKind", event.eventKind)
        .put("payloadJson", event.payloadJson)
        .put("checksumBefore", event.checksumBefore)
        .put("checksumAfter", event.checksumAfter)
        .put("createdAtAudit", event.createdAtAudit)

    private fun receiptJson(receipt: com.braseiro.wwn.data.CommitReceipt): JSONObject = JSONObject()
        .put("status", "OK")
        .put("campaignId", receipt.campaignId)
        .put("previousRevision", receipt.previousRevision)
        .put("newRevision", receipt.newRevision)
        .put("eventId", receipt.eventId)
        .put("stateChecksum", receipt.stateChecksum)
        .put("snapshotId", receipt.snapshotId)
}
