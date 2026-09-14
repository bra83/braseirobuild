package braseiro.ose.hex

import braseiro.ose.map.Axial
import braseiro.ose.map.HexGeometryV1
import braseiro.ose.model.*
import braseiro.ose.rng.NamedRngStreams
import braseiro.ose.rng.RngStreamId
import kotlin.math.abs

object HexWorldGeneratorV1 {
    const val VERSION=1
    fun generate(worldId:String,seed:ULong,profile:RuleProfile,width:Int=12,height:Int=10):HexWorldSnapshot {
        require(width>=6&&height>=6)
        val rng=NamedRngStreams.fromRootSeed(seed)
        val mountainQ=(width/2 + (rng.draw(RngStreamId.HEX_WORLD_GEN)%3u).toInt()-1).coerceIn(1,width-2)
        val riverQ=(mountainQ + (rng.draw(RngStreamId.HEX_WORLD_GEN)%3u).toInt()-1).coerceIn(1,width-2)
        val poiSlots=setOf(
            (1 to height/2) to "POI_WEST",
            ((width-2) to height/2) to "POI_EAST",
            (riverQ to (height-3)) to "POI_RIVER"
        ).toMap()
        val cells=mutableListOf<HexCellSnapshot>()
        for(r in 0 until height) for(q in 0 until width){
            val sea=r==height-1
            val river=q==riverQ && r in 1 until height-1
            val terrain=when {
                r<=1 && abs(q-mountainQ)<=1 -> TerrainKind.MOUNTAINS
                r<=3 && abs(q-mountainQ)<=2 -> TerrainKind.HILLS
                q<width/3 && r in 2..(height-3) -> TerrainKind.FOREST
                q>=(width*2/3) && r in 2..(height-4) -> TerrainKind.DESERT
                river && r>=height-4 -> TerrainKind.SWAMP
                profile==RuleProfile.OSE_ADVANCED_FANTASY && q==width-2 && r==1 -> TerrainKind.BARREN_ADV
                profile==RuleProfile.OSE_ADVANCED_FANTASY && q==width-3 && r==1 -> TerrainKind.BROKEN_LANDS_ADV
                r==height/2 && q in (width/3)..(width*2/3) -> TerrainKind.JUNGLE
                else -> TerrainKind.CLEAR_GRASSLANDS
            }
            val hydro=when{sea->HydrologyKind.SEA_OCEAN;river->HydrologyKind.LAKE_RIVER;else->HydrologyKind.NONE}
            val road=r==height/2 && q in 1 until width-1
            val trail=q==mountainQ && r in 2..height/2
            cells+=HexCellSnapshot(q,r,terrain,hydro,road,trail,poiSlots[q to r])
        }
        return HexWorldSnapshot(worldId,VERSION,HexGeometryV1.VERSION,seed,width,height,cells).canonical()
    }
}

data class CoherenceReport(val pass:Boolean,val errors:List<String>)

object GeographicCoherenceValidator {
    fun validate(world:HexWorldSnapshot,profile:RuleProfile):CoherenceReport {
        val errors=mutableListOf<String>()
        if(world.geometryVersion!=HexGeometryV1.VERSION) errors+="GEOMETRY_VERSION"
        if(world.cells.size!=world.width*world.height) errors+="CELL_COUNT"
        if(world.cells.map{it.q to it.r}.toSet().size!=world.cells.size) errors+="DUPLICATE_COORDINATES"
        if(profile==RuleProfile.OSE_CLASSIC_FANTASY && world.cells.any{it.terrain in setOf(TerrainKind.BARREN_ADV,TerrainKind.BROKEN_LANDS_ADV)}) errors+="ADVANCED_TERRAIN_IN_CLASSIC"
        val sea=world.cells.filter{it.hydrology==HydrologyKind.SEA_OCEAN}.map{Axial(it.q,it.r)}.toSet()
        if(sea.isEmpty()||!connected(sea)) errors+="SEA_NOT_CONNECTED"
        val river=world.cells.filter{it.hydrology==HydrologyKind.LAKE_RIVER}.map{Axial(it.q,it.r)}.toSet()
        if(river.isEmpty()||!connected(river)) errors+="RIVER_BROKEN"
        if(river.isNotEmpty() && sea.isNotEmpty() && river.none{r->sea.any{s->r.distance(s)==1}}) errors+="RIVER_NO_SEA_ENDPOINT"
        val roads=world.cells.filter{it.road}.map{Axial(it.q,it.r)}.toSet(); if(roads.isNotEmpty()&&!connected(roads))errors+="ROAD_BROKEN"
        val trails=world.cells.filter{it.trail}.map{Axial(it.q,it.r)}.toSet(); if(trails.isNotEmpty()&&!connected(trails))errors+="TRAIL_BROKEN"
        return CoherenceReport(errors.isEmpty(),errors)
    }
    private fun connected(points:Set<Axial>):Boolean {
        if(points.isEmpty())return true
        val seen=mutableSetOf(points.first());val q=ArrayDeque<Axial>();q+=points.first()
        while(q.isNotEmpty()){for(n in q.removeFirst().neighbors())if(n in points&&seen.add(n))q+=n}
        return seen.size==points.size
    }
}
