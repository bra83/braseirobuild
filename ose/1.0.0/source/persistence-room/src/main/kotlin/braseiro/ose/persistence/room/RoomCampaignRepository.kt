package braseiro.ose.persistence.room

import braseiro.ose.model.CampaignEnvelope
import braseiro.ose.model.CampaignId
import braseiro.ose.model.CanonicalStateHash
import braseiro.ose.persistence.api.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomCampaignRepository(
    private val db: BraseiroOseDatabase,
    private val commitFailureInjector: (() -> Unit)? = null,
    private val readFailureInjector: (() -> Unit)? = null
) : CampaignRepository {
    private val dao = db.campaignDao()
    private val json = Json { encodeDefaults = true; explicitNulls = false; classDiscriminator = "kind"; ignoreUnknownKeys = false }

    override fun create(envelope: CampaignEnvelope) = dao.insertCampaign(CampaignEntity(envelope.campaignId.value, json.encodeToString(envelope))).let { }

    override fun replace(envelope: CampaignEnvelope) {
        db.runInTransaction {
            val current = dao.findCampaign(envelope.campaignId.value) ?: error("Campaign not found: ${envelope.campaignId.value}")
            val stateHash = CanonicalStateHash.sha256(envelope)
            val transitionId = "BACKUP_RESTORE:$stateHash"
            val prior = dao.findTransitionAudit(envelope.campaignId.value, transitionId)
            if (prior != null) {
                require(prior.postStateHash.isBlank() || prior.postStateHash == stateHash) { "TRANSITION_ID_COLLISION:$transitionId" }
                return@runInTransaction
            }
            val nextSequence = current.commitSequence + 1
            dao.insertAudit(ActionAuditEntity(campaignId = envelope.campaignId.value, transitionId = transitionId, sequence = nextSequence, postStateHash = stateHash))
            check(dao.updateState(envelope.campaignId.value, json.encodeToString(envelope), nextSequence) == 1)
        }
    }

    override fun load(campaignId: CampaignId): CampaignLoadResult {
        return try {
            readFailureInjector?.invoke()
            val entity = dao.findCampaign(campaignId.value)
                ?: return CampaignLoadResult.NotFound(campaignId)
            CampaignLoadResult.Loaded(json.decodeFromString<CampaignEnvelope>(entity.envelopeJson))
        } catch (t: Throwable) {
            CampaignLoadResult.ReadFailure(campaignId, t)
        }
    }

    override fun commit(campaignId: CampaignId, transition: StateTransition) {
        db.runInTransaction {
            val current = dao.findCampaign(campaignId.value) ?: error("Campaign not found: ${campaignId.value}")
            val envelope = json.decodeFromString<CampaignEnvelope>(current.envelopeJson)
            val updated = envelope.copy(campaignState = transition.updatedState)
            val postStateHash = CanonicalStateHash.sha256(updated)
            val prior = dao.findTransitionAudit(campaignId.value, transition.transitionId)
            if (prior != null) {
                require(prior.postStateHash.isBlank() || prior.postStateHash == postStateHash) { "TRANSITION_ID_COLLISION:${transition.transitionId}" }
                return@runInTransaction
            }
            val nextSequence = current.commitSequence + 1
            dao.insertAudit(ActionAuditEntity(campaignId = campaignId.value, transitionId = transition.transitionId, sequence = nextSequence, postStateHash = postStateHash))
            commitFailureInjector?.invoke()
            check(dao.updateState(campaignId.value, json.encodeToString(updated), nextSequence) == 1)
        }
    }

    override fun checkpoint(campaignId: CampaignId, checkpointId: String) {
        require(checkpointId.isNotBlank())
        db.runInTransaction {
            val current = dao.findCampaign(campaignId.value) ?: error("Campaign not found: ${campaignId.value}")
            val envelope = json.decodeFromString<CampaignEnvelope>(current.envelopeJson)
            dao.insertCheckpoint(CheckpointEntity(
                checkpointId = checkpointId,
                campaignId = campaignId.value,
                commitSequence = current.commitSequence,
                schemaVersion = envelope.schemaVersion,
                profile = envelope.ruleProfile.name,
                authorityRevision = envelope.authorityRevision.revision,
                stateHash = CanonicalStateHash.sha256(envelope),
                envelopeJson = current.envelopeJson
            ))
        }
    }

    override fun listCampaigns(): List<CampaignSummary> = dao.listCampaigns().map { CampaignSummary(CampaignId(it.campaignId), it.archived) }
    override fun archive(campaignId: CampaignId) { check(dao.archive(campaignId.value) == 1) }
}
