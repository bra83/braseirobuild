package braseiro.ose.dungeon

import braseiro.ose.model.*
import braseiro.ose.rng.*

object DungeonGeneratorV1 {
    const val VERSION=1
    fun generate(dungeonId:String, seed:ULong, roomCount:Int=12):DungeonSnapshot {
        require(roomCount>=2)
        val rng=NamedRngStreams.fromRootSeed(seed)
        val nodes=(0 until roomCount).map { i ->
            DungeonNodeSnapshot("R%02d".format(i),if(i==0)"ENTRANCE" else if(i==roomCount-1)"DEEP_ROOM" else "ROOM", secret=i>1 && rng.draw(RngStreamId.DUNGEON_GEN)%11u==0u)
        }
        val edges=mutableListOf<DungeonEdgeSnapshot>()
        for(i in 1 until roomCount){
            val parent=(rng.draw(RngStreamId.DUNGEON_GEN)%i.toUInt()).toInt()
            edges += DungeonEdgeSnapshot(nodes[parent].nodeId,nodes[i].nodeId,if(rng.draw(RngStreamId.DUNGEON_GEN)%5u==0u)"DOOR" else "PASSAGE",secret=rng.draw(RngStreamId.DUNGEON_GEN)%13u==0u)
        }
        repeat(minOf(3,roomCount/4)){
            val a=(rng.draw(RngStreamId.DUNGEON_GEN)%roomCount.toUInt()).toInt(); val b=(rng.draw(RngStreamId.DUNGEON_GEN)%roomCount.toUInt()).toInt()
            if(a!=b && edges.none{(it.from==nodes[a].nodeId&&it.to==nodes[b].nodeId)||(it.from==nodes[b].nodeId&&it.to==nodes[a].nodeId)})
                edges += DungeonEdgeSnapshot(nodes[a].nodeId,nodes[b].nodeId,"PASSAGE",false)
        }
        return DungeonSnapshot(dungeonId,VERSION,seed,nodes.first().nodeId,nodes,edges,knownNodeIds=setOf(nodes.first().nodeId),visibleNodeIds=setOf(nodes.first().nodeId)).canonical()
    }

    fun isReachable(d:DungeonSnapshot):Boolean {
        val adj=d.nodes.associate{it.nodeId to mutableSetOf<String>()}.toMutableMap()
        d.edges.forEach{e->adj.getValue(e.from)+=e.to;adj.getValue(e.to)+=e.from}
        val seen=mutableSetOf(d.entranceNodeId); val q=ArrayDeque<String>();q+=d.entranceNodeId
        while(q.isNotEmpty()){for(n in adj.getValue(q.removeFirst()))if(seen.add(n))q+=n}
        return seen.size==d.nodes.size
    }
}
