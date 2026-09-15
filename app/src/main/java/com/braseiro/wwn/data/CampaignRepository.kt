package com.braseiro.wwn.data

import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

class CampaignRepository(private val db: BraseiroDatabase) {
    private val verifiedLoadCache = ConcurrentHashMap<String, CampaignLoadResult.Ok>()
    companion object {
        const val DB_SCHEMA_VERSION = 1
        const val APP_DATA_VERSION = 1
        const val RNG_ALGORITHM = "splitmix64-counter-v1"
        private const val ACTIVE_CAMPAIGN_KEY = "activeCampaignId"
    }

    fun createCampaign(
        campaignId: String,
        displayName: String,
        stateJson: String,
        masterSeed: String,
        rngAlgorithm: String = RNG_ALGORITHM,
        initialSnapshotId: String,
        auditTimeMs: Long,
    ): CommitReceipt {
        val receipt = db.runInTransaction(Callable {
        require(campaignId.isNotBlank()) { "campaignId required" }
        require(displayName.isNotBlank()) { "displayName required" }
        require(stateJson.isNotBlank()) { "stateJson required" }
        require(masterSeed.isNotBlank()) { "masterSeed required" }
        require(rngAlgorithm.isNotBlank()) { "rngAlgorithm required" }
        require(initialSnapshotId.isNotBlank()) { "initialSnapshotId required" }
        val dao = db.dao()
        require(dao.getCampaignMeta(campaignId) == null) { "Campaign already exists" }
        val checksum = sha256Hex(stateJson)
        val meta = CampaignMetaEntity(
            campaignId = campaignId,
            displayName = displayName,
            revision = 0,
            schemaVersion = DB_SCHEMA_VERSION,
            appDataVersion = APP_DATA_VERSION,
            stateChecksum = checksum,
            rngAlgorithm = rngAlgorithm,
            masterSeed = masterSeed,
            updatedAtAudit = auditTimeMs,
        )
        dao.insertCampaignMeta(meta)
        dao.insertCampaignState(CampaignStateEntity(campaignId, 0, stateJson, checksum))
        dao.insertSnapshot(
            CampaignSnapshotEntity(
                snapshotId = initialSnapshotId,
                campaignId = campaignId,
                revision = 0,
                stateJson = stateJson,
                checksum = checksum,
                schemaVersion = DB_SCHEMA_VERSION,
                rngAlgorithm = rngAlgorithm,
                rngCountersJson = "{}",
                lastEventId = null,
                createdAtAudit = auditTimeMs,
            ),
        )
        CommitReceipt(campaignId, -1, 0, "campaign-created", checksum, initialSnapshotId)
        })
        verifiedLoadCache[campaignId] = CampaignLoadResult.Ok(
            campaignId = campaignId,
            displayName = displayName,
            revision = 0,
            stateJson = stateJson,
            checksum = receipt.stateChecksum,
            rngAlgorithm = rngAlgorithm,
            masterSeed = masterSeed,
        )
        return receipt
    }

    fun listCampaigns(): List<CampaignMetaEntity> = db.dao().listCampaignMeta()

    fun loadCampaign(campaignId: String): CampaignLoadResult {
        verifiedLoadCache[campaignId]?.let { return it }
        return try {
            val dao = db.dao()
            val meta = dao.getCampaignMeta(campaignId) ?: return CampaignLoadResult.NotFound
            val state = dao.getCampaignState(campaignId)
                ?: return CampaignLoadResult.Corrupt("campaign_state missing for existing campaign")
            if (state.revision != meta.revision) {
                return CampaignLoadResult.Corrupt("revision mismatch meta=${meta.revision} state=${state.revision}")
            }
            val actual = sha256Hex(state.stateJson)
            if (actual != state.checksum || actual != meta.stateChecksum) {
                return CampaignLoadResult.Corrupt("state checksum mismatch")
            }
            CampaignLoadResult.Ok(
                campaignId = campaignId,
                displayName = meta.displayName,
                revision = meta.revision,
                stateJson = state.stateJson,
                checksum = actual,
                rngAlgorithm = meta.rngAlgorithm,
                masterSeed = meta.masterSeed,
            ).also { verifiedLoadCache[campaignId] = it }
        } catch (t: Throwable) {
            CampaignLoadResult.IoError(t.message ?: t::class.java.simpleName)
        }
    }

    fun getActiveCampaignId(): String? = db.dao().getAppMeta(ACTIVE_CAMPAIGN_KEY)?.metaValue?.takeIf { it.isNotBlank() }

    fun setActiveCampaignId(campaignId: String) {
        require(campaignId.isNotBlank()) { "campaignId required" }
        require(db.dao().getCampaignMeta(campaignId) != null) { "Campaign not found" }
        db.dao().upsertAppMeta(AppMetaEntity(ACTIVE_CAMPAIGN_KEY, campaignId))
    }

    fun commit(request: CommitRequest, auditTimeMs: Long): CommitReceipt {
        val receipt = db.runInTransaction(Callable {
        val dao = db.dao()
        val meta = dao.getCampaignMeta(request.campaignId)
            ?: throw IllegalStateException("Campaign not found")
        val state = dao.getCampaignState(request.campaignId)
            ?: throw IllegalStateException("Campaign state missing")
        if (meta.revision != request.expectedRevision || state.revision != request.expectedRevision) {
            throw RevisionConflictException(
                "Expected revision ${request.expectedRevision}, actual meta=${meta.revision}, state=${state.revision}",
            )
        }
        val newRevision = request.expectedRevision + 1
        val newChecksum = sha256Hex(request.newStateJson)
        dao.insertEvent(
            CampaignEventEntity(
                eventId = request.eventId,
                campaignId = request.campaignId,
                revision = newRevision,
                eventKind = request.eventKind,
                payloadJson = request.eventPayloadJson,
                checksumBefore = state.checksum,
                checksumAfter = newChecksum,
                createdAtAudit = auditTimeMs,
            ),
        )
        dao.updateCampaignState(
            state.copy(revision = newRevision, stateJson = request.newStateJson, checksum = newChecksum),
        )
        dao.updateCampaignMeta(
            meta.copy(revision = newRevision, stateChecksum = newChecksum, updatedAtAudit = auditTimeMs),
        )
        request.snapshotId?.let { snapshotId ->
            dao.insertSnapshot(
                CampaignSnapshotEntity(
                    snapshotId = snapshotId,
                    campaignId = request.campaignId,
                    revision = newRevision,
                    stateJson = request.newStateJson,
                    checksum = newChecksum,
                    schemaVersion = meta.schemaVersion,
                    rngAlgorithm = meta.rngAlgorithm,
                    rngCountersJson = request.rngCountersJson,
                    lastEventId = request.eventId,
                    createdAtAudit = auditTimeMs,
                ),
            )
        }
        CommitReceipt(
            campaignId = request.campaignId,
            previousRevision = request.expectedRevision,
            newRevision = newRevision,
            eventId = request.eventId,
            stateChecksum = newChecksum,
            snapshotId = request.snapshotId,
        )
        })
        val cached = verifiedLoadCache[request.campaignId]
        if (cached != null && cached.revision == request.expectedRevision) {
            verifiedLoadCache[request.campaignId] = cached.copy(
                revision = receipt.newRevision,
                stateJson = request.newStateJson,
                checksum = receipt.stateChecksum,
            )
        } else {
            verifiedLoadCache.remove(request.campaignId)
        }
        return receipt
    }

    fun getEvent(eventId: String): CampaignEventEntity? = db.dao().getEvent(eventId)
    fun latestNarrative(campaignId: String, mechanicalEventId: String): CampaignEventEntity? =
        db.dao().latestNarrativeByPrefix(campaignId, "narrative:${mechanicalEventId}%")
    fun listEvents(campaignId: String): List<CampaignEventEntity> = db.dao().listEvents(campaignId)
    fun listSnapshots(campaignId: String): List<CampaignSnapshotEntity> = db.dao().listSnapshots(campaignId)

    fun appendNarrative(request: NarrativeAppendRequest, auditTimeMs: Long): NarrativeAppendReceipt = db.runInTransaction(Callable {
        val dao = db.dao()
        val existing = dao.getEvent(request.eventId)
        if (existing != null) {
            require(existing.campaignId == request.campaignId) { "Narrative event id collision across campaigns" }
            return@Callable NarrativeAppendReceipt(request.campaignId, existing.revision, existing.eventId, true)
        }
        val parent = dao.getEvent(request.mechanicalEventId)
            ?: throw IllegalStateException("Parent mechanical event not found")
        require(parent.campaignId == request.campaignId) { "Parent mechanical event belongs to different campaign" }
        require(parent.revision == request.mechanicalRevision) {
            "Parent revision mismatch expected=${request.mechanicalRevision} actual=${parent.revision}"
        }
        require(parent.eventKind != "NARRATIVE") { "Narrative cannot parent another narrative in Wave 3" }
        dao.insertEvent(
            CampaignEventEntity(
                eventId = request.eventId,
                campaignId = request.campaignId,
                revision = request.mechanicalRevision,
                eventKind = "NARRATIVE",
                payloadJson = request.eventPayloadJson,
                checksumBefore = parent.checksumAfter,
                checksumAfter = parent.checksumAfter,
                createdAtAudit = auditTimeMs,
            ),
        )
        NarrativeAppendReceipt(request.campaignId, request.mechanicalRevision, request.eventId, false)
    })

    fun restorePortableBackup(payload: PortableRestorePayload, overwrite: Boolean, auditTimeMs: Long): PortableRestoreReceipt {
        val receipt = db.runInTransaction(Callable {
            validateRestorePayload(payload)
            val dao = db.dao()
            val existing = dao.getCampaignMeta(payload.campaignId)
            if (existing != null && !overwrite) throw CampaignRestoreConflictException("TARGET_CAMPAIGN_EXISTS")
            val overwritten = existing != null
            if (overwritten) {
                dao.deleteEvents(payload.campaignId)
                dao.deleteSnapshots(payload.campaignId)
                dao.deleteCampaignState(payload.campaignId)
                dao.deleteCampaignMeta(payload.campaignId)
            }

            val stateChecksum = sha256Hex(payload.stateJson)
            dao.insertCampaignMeta(
                CampaignMetaEntity(
                    campaignId = payload.campaignId,
                    displayName = payload.displayName,
                    revision = payload.revision,
                    schemaVersion = DB_SCHEMA_VERSION,
                    appDataVersion = APP_DATA_VERSION,
                    stateChecksum = stateChecksum,
                    rngAlgorithm = payload.rngAlgorithm,
                    masterSeed = payload.masterSeed,
                    updatedAtAudit = auditTimeMs,
                ),
            )
            dao.insertCampaignState(CampaignStateEntity(payload.campaignId, payload.revision, payload.stateJson, stateChecksum))

            payload.events.forEach { event ->
                dao.insertEvent(
                    CampaignEventEntity(
                        eventId = event.eventId,
                        campaignId = payload.campaignId,
                        revision = event.revision,
                        eventKind = event.eventKind,
                        payloadJson = event.payloadJson,
                        checksumBefore = event.checksumBefore,
                        checksumAfter = event.checksumAfter,
                        createdAtAudit = event.createdAtAudit.takeIf { it > 0L } ?: auditTimeMs,
                    ),
                )
            }
            payload.snapshots.forEach { snapshot ->
                dao.insertSnapshot(
                    CampaignSnapshotEntity(
                        snapshotId = snapshot.snapshotId,
                        campaignId = payload.campaignId,
                        revision = snapshot.revision,
                        stateJson = snapshot.stateJson,
                        checksum = sha256Hex(snapshot.stateJson),
                        schemaVersion = DB_SCHEMA_VERSION,
                        rngAlgorithm = payload.rngAlgorithm,
                        rngCountersJson = snapshot.rngCountersJson,
                        lastEventId = snapshot.lastEventId,
                        createdAtAudit = snapshot.createdAtAudit.takeIf { it > 0L } ?: auditTimeMs,
                    ),
                )
            }
            dao.upsertAppMeta(AppMetaEntity(ACTIVE_CAMPAIGN_KEY, payload.campaignId))
            PortableRestoreReceipt(
                campaignId = payload.campaignId,
                revision = payload.revision,
                stateChecksum = stateChecksum,
                eventsRestored = payload.events.size,
                snapshotsRestored = payload.snapshots.size,
                overwritten = overwritten,
            )
        })
        verifiedLoadCache[payload.campaignId] = CampaignLoadResult.Ok(
            campaignId = payload.campaignId,
            displayName = payload.displayName,
            revision = payload.revision,
            stateJson = payload.stateJson,
            checksum = receipt.stateChecksum,
            rngAlgorithm = payload.rngAlgorithm,
            masterSeed = payload.masterSeed,
        )
        return receipt
    }

    private fun validateRestorePayload(payload: PortableRestorePayload) {
        fun invalid(message: String): Nothing = throw CampaignRestoreValidationException(message)
        if (payload.campaignId.isBlank()) invalid("campaignId required")
        if (payload.displayName.isBlank()) invalid("displayName required")
        if (payload.revision < 0L) invalid("revision must be non-negative")
        if (payload.stateJson.isBlank()) invalid("stateJson required")
        if (payload.masterSeed.isBlank()) invalid("masterSeed required")
        if (payload.rngAlgorithm.isBlank()) invalid("rngAlgorithm required")
        if (payload.snapshots.isEmpty()) invalid("portable backup requires at least one concrete snapshot")

        val eventIds = HashSet<String>()
        payload.events.forEach { event ->
            if (event.eventId.isBlank() || !eventIds.add(event.eventId)) invalid("duplicate or blank eventId")
            if (event.campaignId != payload.campaignId) invalid("event campaignId mismatch")
            if (event.revision < 0L || event.revision > payload.revision) invalid("event revision out of range")
            if (event.eventKind.isBlank()) invalid("eventKind required")
            if (event.checksumAfter.isBlank()) invalid("event checksumAfter required")
        }

        val snapshotIds = HashSet<String>()
        payload.snapshots.forEach { snapshot ->
            if (snapshot.snapshotId.isBlank() || !snapshotIds.add(snapshot.snapshotId)) invalid("duplicate or blank snapshotId")
            if (snapshot.campaignId != payload.campaignId) invalid("snapshot campaignId mismatch")
            if (snapshot.revision < 0L || snapshot.revision > payload.revision) invalid("snapshot revision out of range")
            if (snapshot.stateJson.isBlank()) invalid("snapshot stateJson required")
            val lastEventId = snapshot.lastEventId
            if (lastEventId != null && lastEventId !in eventIds) invalid("snapshot lastEventId missing from events")
        }
        if (payload.snapshots.none { it.revision == payload.revision && sha256Hex(it.stateJson) == sha256Hex(payload.stateJson) }) {
            invalid("portable backup lacks a current-state snapshot")
        }
    }

    private fun sha256Hex(text: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
