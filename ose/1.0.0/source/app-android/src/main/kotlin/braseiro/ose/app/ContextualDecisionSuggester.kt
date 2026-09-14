package braseiro.ose.app

import braseiro.ose.model.CampaignEnvelope
import braseiro.ose.model.SpatialRef
import org.json.JSONArray
import org.json.JSONObject

/**
 * Three deterministic, read-only suggestions from player-visible state only.
 * They are reaction text, never commands and never mechanical mutations.
 */
object ContextualDecisionSuggester {
    data class Suggestion(val label: String, val reaction: String, val mechanicalIntent: String? = null)

    fun suggest(envelope: CampaignEnvelope): List<Suggestion> {
        val session = envelope.campaignState.game.session
        val visible = session.visibleNarration.lowercase()
        val phase = session.phase.uppercase()
        val position = envelope.campaignState.position.primary
        return when {
            phase.contains("COMBAT") -> listOf(
                Suggestion("OBSERVAR A AMEAÇA", "Mantemos a formação e observamos a ameaça antes de nos comprometer."),
                Suggestion("BUSCAR COBERTURA", "Procuramos uma posição defensável ao nosso alcance sem avançar às cegas."),
                Suggestion("TENTAR FALAR", "Tentamos comunicação antes de iniciar outra ação hostil.")
            )
            phase.contains("RECOVERY") || visible.contains("silenc") -> listOf(
                Suggestion("VERIFICAR O GRUPO", "Verificamos a condição do grupo e o que está imediatamente ao nosso redor."),
                Suggestion("EXAMINAR A ÁREA", "Examinamos a área visível em busca de sinais ou mudanças recentes."),
                Suggestion("REORGANIZAR", "Reorganizamos a companhia antes de decidir para onde seguir.")
            )
            visible.contains("porta") || visible.contains("corredor") || position is SpatialRef.Dungeon -> listOf(
                Suggestion("EXAMINAR", "Examinamos cuidadosamente a passagem visível sem abri-la nem avançar ainda."),
                Suggestion("ESCUTAR", "Ficamos em silêncio e escutamos antes de agir."),
                Suggestion("RECUAR", "Recuamos até a última posição conhecida como segura.")
            )
            position is SpatialRef.Hex -> hexTravelSuggestions(envelope, position)
            visible.contains("trilha") || visible.contains("caminho") || visible.contains("colina") -> listOf(
                Suggestion("LER O TERRENO", "Examinamos o terreno e sinais visíveis antes de escolher o caminho."),
                Suggestion("SEGUIR COM CAUTELA", "Seguimos pela rota visível com cautela, atentos a mudanças no terreno."),
                Suggestion("BUSCAR VANTAGEM", "Procuramos um ponto visível que permita observar melhor os arredores.")
            )
            else -> listOf(
                Suggestion("OBSERVAR", "Observamos com atenção o que está visível antes de agir."),
                Suggestion("AVANÇAR DEVAGAR", "Avançamos devagar pela opção de caminho já visível."),
                Suggestion("VOLTAR", "Voltamos à última posição conhecida antes de decidir novamente.")
            )
        }.also { check(it.size == 3) }
    }


    private fun hexTravelSuggestions(envelope:CampaignEnvelope, position:SpatialRef.Hex):List<Suggestion> {
        data class D(val key:String,val label:String,val dq:Int,val dr:Int)
        val dirs=listOf(D("E","LESTE",1,0),D("SE","SUDESTE",0,1),D("NE","NORDESTE",1,-1),D("W","OESTE",-1,0),D("SW","SUDOESTE",-1,1),D("NW","NOROESTE",0,-1))
        val cells=envelope.campaignState.game.hexWorld?.cells.orEmpty()
        val valid=dirs.filter { d -> cells.any { it.q==position.q+d.dq && it.r==position.r+d.dr && it.hydrology.name!="SEA_OCEAN" } }
        val first=valid.getOrNull(0) ?: dirs[0]
        val second=valid.getOrNull(1) ?: dirs[1]
        return listOf(
            Suggestion("VIAJAR ${first.label}","Viajamos para ${first.label.lowercase()} com cautela, observando o caminho.","WILD_TRAVEL_DAY:${first.key}"),
            Suggestion("FORRAGEAR ${second.label}","Seguimos para ${second.label.lowercase()} procurando alimento durante a viagem.","WILD_TRAVEL_FORAGE_DAY:${second.key}"),
            Suggestion("DESCANSAR UM DIA","Paramos para descansar um dia inteiro e recuperar as forças.","WILD_REST_DAY")
        )
    }

    fun asJson(envelope: CampaignEnvelope): JSONArray = JSONArray(suggest(envelope).map {
        JSONObject().put("label", it.label).put("reaction", it.reaction).put("mechanicalIntent", it.mechanicalIntent ?: JSONObject.NULL)
    })
}
