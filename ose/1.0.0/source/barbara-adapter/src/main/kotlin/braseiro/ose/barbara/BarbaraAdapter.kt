package braseiro.ose.barbara

import braseiro.ose.model.CampaignEnvelope
import braseiro.ose.rules.api.RuleTrace

interface BarbaraSupervisorPort {
    fun narrate(committed: CampaignEnvelope, playerReaction: String, mechanicalFeedback: String, trace: RuleTrace): String
    fun help(readOnly: CampaignEnvelope, question: String): String
}

/**
 * Deterministic local Barbara adapter used when no external language service is configured.
 * It is presentation/orchestration only: it receives already-resolved state and cannot return mutations.
 */
class DeterministicBarbaraSupervisor : BarbaraSupervisorPort {
    override fun narrate(committed: CampaignEnvelope, playerReaction: String, mechanicalFeedback: String, trace: RuleTrace): String {
        val place=when(val p=committed.campaignState.position.primary){
            is braseiro.ose.model.SpatialRef.Dungeon -> "na masmorra ${p.spatialEntityId}, setor ${p.nodeId}"
            is braseiro.ose.model.SpatialRef.Hex -> "no hex ${p.q},${p.r} de ${p.spatialEntityId}"
            is braseiro.ose.model.SpatialRef.Settlement -> "em ${p.spatialEntityId}, ${p.anchorId}"
            is braseiro.ose.model.SpatialRef.Scene -> "na cena ${p.sceneId}"
        }
        val consequence=if(mechanicalFeedback.isBlank()) "Nenhuma consequência mecânica foi aplicada." else mechanicalFeedback
        return "Mestre: o grupo declarou: “${playerReaction.trim()}”. A posição canônica permanece $place. $consequence"
    }
    override fun help(readOnly: CampaignEnvelope, question: String): String =
        "GM_HELP: consulta somente leitura sobre “${question.trim()}”. Perfil ativo: ${readOnly.ruleProfile}. Nenhum tempo, posição, NPC, recurso ou RNG foi alterado."
}
