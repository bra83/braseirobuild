package braseiro.ose.world

import braseiro.ose.model.*

object WorldEventScheduler {
    fun advance(game:GameExtensions,fromTurn:Long,toTurn:Long):GameExtensions {
        require(toTurn>=fromTurn)
        if(toTurn==fromTurn)return game
        var npcs=game.npcs
        val events=game.worldEvents.map { event ->
            if(!event.resolved && event.dueTurn in (fromTurn+1)..toTurn){
                val npc=npcs[event.targetId]
                if(npc!=null){
                    npcs=npcs+(npc.npcId to npc.copy(
                        reactionHistory=npc.reactionHistory+"WORLD_EVENT:${event.eventId}:${event.kind}",
                        memorySummaries=(npc.memorySummaries+event.payload).takeLast(20),
                        consequenceFlags=npc.consequenceFlags+"EVENT_${event.kind}"
                    ))
                }
                event.copy(resolved=true)
            } else event
        }
        return game.copy(npcs=npcs,worldEvents=events,revision=game.revision+1).canonical()
    }
}
