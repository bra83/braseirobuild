package braseiro.ose.map

import braseiro.ose.model.SpatialRef
import kotlin.math.abs

object HexGeometryV1 {
    const val VERSION=1
    const val ASSET_CANVAS_WIDTH=131
    const val ASSET_CANVAS_HEIGHT=144
    const val GEOMETRIC_WIDTH=112
    const val GEOMETRIC_HEIGHT=128
    const val CENTER_X=66
    const val CENTER_Y=72
    const val COLUMN_STEP_X=112
    const val ROW_STEP_Y=96
    const val ODD_ROW_OFFSET_X=56
    const val ORIENTATION="POINTY_TOP"

    data class Point(val x:Int,val y:Int)
    fun projectAxial(q:Int,r:Int):Point {
        // axial -> odd-row offset visual coordinates without mutating canonical q,r
        val col=q + (r - (r and 1))/2
        return Point(col*COLUMN_STEP_X + if((r and 1)!=0) ODD_ROW_OFFSET_X else 0, r*ROW_STEP_Y)
    }
    fun noManualNudge():Boolean=true
}

data class Axial(val q:Int,val r:Int) {
    fun neighbors():List<Axial> = listOf(Axial(q+1,r),Axial(q+1,r-1),Axial(q,r-1),Axial(q-1,r),Axial(q-1,r+1),Axial(q,r+1))
    fun distance(other:Axial):Int {
        val dq=q-other.q; val dr=r-other.r; val ds=(q+r)-(other.q+other.r)
        return (abs(dq)+abs(dr)+abs(ds))/2
    }
}

object PositionAuthority {
    fun sameCanonicalPosition(a:SpatialRef,b:SpatialRef):Boolean=a==b
}
