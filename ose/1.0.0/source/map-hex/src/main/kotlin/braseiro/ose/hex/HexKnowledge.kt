package braseiro.ose.hex

import braseiro.ose.model.*

object HexKnowledge {
    fun reveal(world:HexWorldSnapshot,q:Int,r:Int,radius:Int=1):HexWorldSnapshot {
        val target=world.cells.firstOrNull{it.q==q&&it.r==r} ?: return world
        val visible=world.cells.filter{ kotlin.math.abs(it.q-target.q)+kotlin.math.abs(it.r-target.r)<=radius*2 }.map{it.q to it.r}.toSet()
        return world.copy(cells=world.cells.map { c ->
            val key=c.q to c.r
            c.copy(known=c.known||key in visible,visible=key in visible)
        }).canonical()
    }
    fun inspect(world:HexWorldSnapshot,q:Int,r:Int):HexCellSnapshot? = world.cells.firstOrNull{it.q==q&&it.r==r&&it.known}
}
