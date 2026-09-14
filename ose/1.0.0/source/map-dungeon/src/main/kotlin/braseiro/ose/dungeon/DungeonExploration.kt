package braseiro.ose.dungeon

import braseiro.ose.model.*

sealed class DungeonMoveResult {
    data class Moved(val state:CampaignState,val revealed:Set<String>):DungeonMoveResult()
    data class Rejected(val reason:String):DungeonMoveResult()
}

object DungeonExploration {
    fun move(state:CampaignState,targetNodeId:String):DungeonMoveResult {
        val d=state.game.dungeon ?: return DungeonMoveResult.Rejected("NO_DUNGEON_STATE")
        val pos=state.position.primary as? SpatialRef.Dungeon ?: return DungeonMoveResult.Rejected("POSITION_NOT_DUNGEON")
        if(pos.spatialEntityId!=d.dungeonId) return DungeonMoveResult.Rejected("POSITION_DUNGEON_MISMATCH")
        val edge=d.edges.firstOrNull{(it.from==pos.nodeId&&it.to==targetNodeId)||(it.to==pos.nodeId&&it.from==targetNodeId)} ?: return DungeonMoveResult.Rejected("NOT_ADJACENT")
        if(edge.secret && targetNodeId !in d.knownNodeIds) return DungeonMoveResult.Rejected("SECRET_EDGE_UNKNOWN")
        val target=d.nodes.firstOrNull{it.nodeId==targetNodeId} ?: return DungeonMoveResult.Rejected("UNKNOWN_NODE")
        if(target.secret && targetNodeId !in d.knownNodeIds) return DungeonMoveResult.Rejected("SECRET_NODE_UNKNOWN")
        val neighbors=d.edges.filter{it.from==targetNodeId||it.to==targetNodeId}.filterNot{it.secret}.map{if(it.from==targetNodeId)it.to else it.from}.toSet()
        val known=d.knownNodeIds+targetNodeId+neighbors
        val nextD=d.copy(knownNodeIds=known,visibleNodeIds=setOf(targetNodeId)+neighbors).canonical()
        val newTurns=state.time.turns+1
        val crossedHours=newTurns/6-state.time.turns/6
        val newHours=state.time.hours+crossedHours
        val next=state.copy(
            position=PositionState(SpatialRef.Dungeon(d.dungeonId,targetNodeId)),
            time=state.time.copy(turns=newTurns,hours=newHours,days=state.time.days+(newHours/24-state.time.hours/24)),
            game=state.game.copy(dungeon=nextD,revision=state.game.revision+1)
        )
        return DungeonMoveResult.Moved(next,known-d.knownNodeIds)
    }
}
