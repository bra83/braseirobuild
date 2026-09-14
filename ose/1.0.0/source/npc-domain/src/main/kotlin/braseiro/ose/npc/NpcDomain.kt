package braseiro.ose.npc

import braseiro.ose.model.*

object NpcDomain {
    private fun legacy(ref: SpatialRef): String = when (ref) {
        is SpatialRef.Hex -> "${ref.spatialEntityId}:${ref.q}:${ref.r}"
        is SpatialRef.Dungeon -> "${ref.spatialEntityId}:${ref.nodeId}"
        is SpatialRef.Settlement -> "${ref.spatialEntityId}:${ref.anchorId}"
        is SpatialRef.Scene -> "${ref.spatialEntityId}:${ref.sceneId}"
    }

    fun createStable(id:String,name:String,spatialRef:SpatialRef,knownFacts:Set<String> = emptySet()) =
        NpcSnapshot(id,name,legacy(spatialRef),spatialRef,knownFacts)

    /** Legacy overload for pre-v2 fixtures/imports. New production code must provide a typed SpatialRef. */
    fun createStable(id:String,name:String,locationRef:String,knownFacts:Set<String> = emptySet()) =
        NpcSnapshot(id,name,locationRef,null,knownFacts)

    fun move(game:GameExtensions,npcId:String,newLocation:SpatialRef):GameExtensions {
        val npc=game.npcs[npcId] ?: error("NPC_NOT_FOUND:$npcId")
        return game.copy(npcs=game.npcs+(npcId to npc.copy(locationRef=legacy(newLocation), spatialRef=newLocation)),revision=game.revision+1).canonical()
    }

    /** Legacy move path for imported snapshots that have not yet resolved a typed spatial reference. */
    fun move(game:GameExtensions,npcId:String,newLocation:String):GameExtensions {
        val npc=game.npcs[npcId] ?: error("NPC_NOT_FOUND:$npcId")
        return game.copy(npcs=game.npcs+(npcId to npc.copy(locationRef=newLocation, spatialRef=null)),revision=game.revision+1).canonical()
    }

    fun remember(game:GameExtensions,npcId:String,summary:String):GameExtensions {
        val npc=game.npcs[npcId] ?: error("NPC_NOT_FOUND:$npcId")
        val updated=npc.copy(memorySummaries=(npc.memorySummaries+summary).takeLast(20))
        return game.copy(npcs=game.npcs+(npcId to updated),revision=game.revision+1).canonical()
    }
    fun projectPlausibleKnowledge(game:GameExtensions,npcId:String,requestedFactIds:Set<String>):Set<String> {
        val npc=game.npcs[npcId] ?: return emptySet()
        return requestedFactIds intersect npc.knownFactIds
    }
}
