package com.braseiro.dnd5e;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** RUN209: deterministic grid-based attack legality for the ordinary single-weapon
 * executor. A square (including diagonals) represents 5 ft, as in RUN208.
 * World/scene queries remain in AppDb; this class is Android-independent.
 * Unsupported features (flight, verticality, unusual senses, large footprints,
 * thrown improvised weapons, opportunity attacks) must use dedicated executors.
 */
public final class TacticalAttackGeometryRules {
    private TacticalAttackGeometryRules() {}
    public static final class Result {
        public final String blocked, fingerprint;
        public final int distanceFeet, geometryCoverBonus;
        public final boolean longRangeDisadvantage;
        private Result(String reason,String fp,int distance,int cover,boolean disadvantage){
            blocked=reason;fingerprint=fp;distanceFeet=distance;geometryCoverBonus=cover;
            longRangeDisadvantage=disadvantage;
        }
        public boolean legal(){return blocked.isEmpty();}
    }
    private static Result fail(String reason){return new Result(reason,"",0,0,false);}
    public static boolean validProfile(String kind,int reach,int normal,int far){
        return "melee".equals(kind)?reach>=5&&reach<=30&&reach%5==0&&normal==0&&far==0:
            "ranged".equals(kind)&&reach==0&&normal>=5&&normal<=600&&normal%5==0&&
                far>=normal&&far<=1200&&far%5==0;
    }
    public static boolean validReceipt(String value){
        if(value==null)return false;
        String[] p=value.split(":",-1);
        if(p.length!=5||!"attackgeom-v1".equals(p[0])||!p[1].matches("[0-9a-f]{1,8}"))return false;
        try{int dist=Integer.parseInt(p[2]),cover=Integer.parseInt(p[3]),longRange=Integer.parseInt(p[4]);
            return dist>=5&&dist<=1200&&dist%5==0&&(cover==0||cover==2||cover==5)&&
                (longRange==0||longRange==1);
        }catch(NumberFormatException e){return false;}
    }
    public static String receipt(Result r){
        if(r==null||!r.legal())throw new IllegalArgumentException("invalid_geometry_receipt");
        return "attackgeom-v1:"+r.fingerprint+":"+r.distanceFeet+":"+r.geometryCoverBonus+":"+
            (r.longRangeDisadvantage?1:0);
    }
    private static boolean bounds(int w,int h,int x,int y){return x>=0&&y>=0&&x<w&&y<h;}
    /** Opaque cells block, intervening creatures/partial cover confer cover.
     * Cover entries: 0=no cover, 2=half, 5=three quarters. Full cover belongs
     * in the opaque array and is never converted to a mere AC penalty.
     */
    public static Result evaluate(int width,int height,int ax,int ay,int tx,int ty,
            boolean[][] opaque,int[][] cover,boolean[][] occupied,String kind,int reach,int normal,int far){
        if(width<1||height<1||width>100||height>100||!bounds(width,height,ax,ay)||
           !bounds(width,height,tx,ty)||(ax==tx&&ay==ty)||
           !validProfile(kind,reach,normal,far)||opaque==null||cover==null||occupied==null||
           opaque.length!=height||cover.length!=height||occupied.length!=height)return fail("invalid_attack_grid_or_weapon");
        for(int y=0;y<height;y++)if(opaque[y]==null||cover[y]==null||occupied[y]==null||
                opaque[y].length!=width||cover[y].length!=width||occupied[y].length!=width)return fail("invalid_attack_grid_shape");
        int distance=5*Math.max(Math.abs(tx-ax),Math.abs(ty-ay));
        if(opaque[ty][tx])return fail("target_in_opaque_cell");
        if(opaque[ay][ax])return fail("attacker_in_opaque_cell");
        if("melee".equals(kind)&&distance>reach)return fail("target_out_of_reach");
        if("ranged".equals(kind)&&distance>far)return fail("target_out_of_range");
        boolean disadvantage="ranged".equals(kind)&&distance>normal;
        int coverBonus=0;
        // Exact centre-to-centre voxel traversal (supercover at tied boundary).
        // A corner grazing an opaque square cannot be used to shoot through a wall.
        int dx=tx-ax,dy=ty-ay,x=ax,y=ay,stepX=Integer.compare(dx,0),stepY=Integer.compare(dy,0);
        double absDx=Math.abs((double)dx),absDy=Math.abs((double)dy);
        double tMaxX=dx==0?Double.POSITIVE_INFINITY:0.5/absDx;
        double tMaxY=dy==0?Double.POSITIVE_INFINITY:0.5/absDy;
        double deltaX=dx==0?Double.POSITIVE_INFINITY:1/absDx;
        double deltaY=dy==0?Double.POSITIVE_INFINITY:1/absDy;
        int guard=0;
        while(x!=tx||y!=ty){
            if(++guard>width+height+4)return fail("invalid_geometry_ray");
            if(Math.abs(tMaxX-tMaxY)<1e-11){
                // At a corner boundary, both side squares obstruct vision.
                if(!bounds(width,height,x+stepX,y)||!bounds(width,height,x,y+stepY))return fail("invalid_geometry_ray");
                if(opaque[y][x+stepX]||opaque[y+stepY][x])return fail("line_of_sight_blocked");
                if(!(x+stepX==tx&&y==ty)){
                    coverBonus=Math.max(coverBonus,cover[y][x+stepX]);
                    if(occupied[y][x+stepX])coverBonus=Math.max(coverBonus,2);
                }
                if(!(x==tx&&y+stepY==ty)){
                    coverBonus=Math.max(coverBonus,cover[y+stepY][x]);
                    if(occupied[y+stepY][x])coverBonus=Math.max(coverBonus,2);
                }
                x+=stepX;y+=stepY;tMaxX+=deltaX;tMaxY+=deltaY;
            }else if(tMaxX<tMaxY){x+=stepX;tMaxX+=deltaX;}
            else{y+=stepY;tMaxY+=deltaY;}
            if(!bounds(width,height,x,y))return fail("invalid_geometry_ray");
            if(x==tx&&y==ty)break;
            if(opaque[y][x])return fail("line_of_sight_blocked");
            coverBonus=Math.max(coverBonus,cover[y][x]);
            if(occupied[y][x])coverBonus=Math.max(coverBonus,2);
        }
        if(coverBonus!=0&&coverBonus!=2&&coverBonus!=5)return fail("invalid_cover_value");
        CRC32 crc=new CRC32();StringBuilder snapshot=new StringBuilder(width*height*3+100);
        snapshot.append("attack-grid-v1|").append(width).append('|').append(height).append('|')
            .append(ax).append('|').append(ay).append('|').append(tx).append('|').append(ty)
            .append('|').append(kind).append('|').append(reach).append('|').append(normal).append('|').append(far).append('|');
        for(int yy=0;yy<height;yy++)for(int xx=0;xx<width;xx++){
            int c=cover[yy][xx];if(c!=0&&c!=2&&c!=5)return fail("invalid_cover_value");
            snapshot.append(opaque[yy][xx]?'X':'.').append(c==5?'3':c==2?'H':'.').append(occupied[yy][xx]?'C':'.');
        }
        byte[] data=snapshot.toString().getBytes(StandardCharsets.UTF_8);crc.update(data,0,data.length);
        return new Result("",Long.toHexString(crc.getValue()),distance,coverBonus,disadvantage);
    }
}
