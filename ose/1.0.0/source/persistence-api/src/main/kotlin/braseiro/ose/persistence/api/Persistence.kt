package braseiro.ose.persistence.api

import braseiro.ose.model.CampaignEnvelope
import braseiro.ose.model.CampaignId
import braseiro.ose.model.CampaignState

data class StateTransition(val transitionId: String, val updatedState: CampaignState)
data class CampaignSummary(val campaignId: CampaignId, val archived: Boolean)
sealed class CampaignLoadResult {
    data class Loaded(val envelope: CampaignEnvelope) : CampaignLoadResult()
    data class NotFound(val campaignId: CampaignId) : CampaignLoadResult()
    data class ReadFailure(val campaignId: CampaignId, val cause: Throwable) : CampaignLoadResult()
}
interface CampaignRepository {
    fun create(envelope: CampaignEnvelope)
    fun load(campaignId: CampaignId): CampaignLoadResult
    fun replace(envelope: CampaignEnvelope)
    fun commit(campaignId: CampaignId, transition: StateTransition)
    fun checkpoint(campaignId: CampaignId, checkpointId: String)
    fun listCampaigns(): List<CampaignSummary>
    fun archive(campaignId: CampaignId)
}
