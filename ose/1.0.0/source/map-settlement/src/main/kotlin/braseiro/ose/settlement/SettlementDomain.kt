package braseiro.ose.settlement

import braseiro.ose.model.*

object SettlementPackages {
    fun canonicalVillage(id:String="SETTLEMENT_001")=SettlementSnapshot(
        settlementId=id,packageVersion=1,
        locations=listOf(
            SettlementLocationSnapshot("GATE","Portão",true,setOf("EXIT")),
            SettlementLocationSnapshot("INN","Estalagem",true,setOf("SERVICE","SOCIAL")),
            SettlementLocationSnapshot("TEMPLE","Templo",false,setOf("SERVICE")),
            SettlementLocationSnapshot("SMITH","Ferreiro",false,setOf("SERVICE")),
            SettlementLocationSnapshot("MARKET","Mercado",true,setOf("SERVICE"))
        ),
        exits=mapOf("GATE" to "HEXCRAWL")
    ).canonical()
}

object SettlementService {
    fun discover(state:CampaignState,locationId:String):CampaignState {
        val s=state.game.settlement ?: error("NO_SETTLEMENT")
        check(s.locations.any{it.locationId==locationId})
        val next=s.copy(locations=s.locations.map{if(it.locationId==locationId)it.copy(discovered=true)else it}).canonical()
        return state.copy(game=state.game.copy(settlement=next,revision=state.game.revision+1))
    }
    fun enter(state:CampaignState,locationId:String):CampaignState {
        val s=state.game.settlement ?: error("NO_SETTLEMENT")
        val loc=s.locations.firstOrNull{it.locationId==locationId&&it.discovered} ?: error("LOCATION_NOT_DISCOVERED")
        return state.copy(position=PositionState(SpatialRef.Settlement(s.settlementId,loc.locationId)),game=state.game.copy(revision=state.game.revision+1))
    }
}
