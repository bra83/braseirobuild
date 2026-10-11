package com.braseiro.dnd5e;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** Deterministic 5-foot voxel attack geometry for large footprints and height.
 * The caller supplies the live scene, creature volumes and sight facts. */
public final class TacticalCreatureGeometryRules {
    private TacticalCreatureGeometryRules() {}
    public static final class Creature {
        public final int x,y,z,width,height;
        public Creature(int x,int y,int z,int width,int height){
            if(x<0||y<0||z<0||width<1||width>4||height<1||height>4)
                throw new IllegalArgumentException("invalid_creature_volume");
            this.x=x;this.y=y;this.z=z;this.width=width;this.height=height;
        }
    }
    public static final class Result {
        public final String blocked,fingerprint;
        public final int distanceFeet,coverBonus;
        public final boolean longRangeDisadvantage,unseenDisadvantage,usedBlindsight;
        private Result(String blocked,String fingerprint,int distance,int cover,
                boolean longRange,boolean unseen,boolean blind){
            this.blocked=blocked;this.fingerprint=fingerprint;distanceFeet=distance;
            coverBonus=cover;longRangeDisadvantage=longRange;
            unseenDisadvantage=unseen;usedBlindsight=blind;
        }
        public boolean legal(){return blocked.isEmpty();}
    }
    private static Result fail(String reason){return new Result(reason,"",0,0,false,false,false);}
    private static boolean overlaps(Creature a,Creature b){
        return a.x<b.x+b.width&&b.x<a.x+a.width&&a.y<b.y+b.width&&b.y<a.y+a.width&&
            a.z<b.z+b.height&&b.z<a.z+a.height;
    }
    private static int gap(int a,int alen,int b,int blen){
        return Math.max(0,Math.max(a-(b+blen-1),b-(a+alen-1)));
    }
    public static int closestDistanceFeet(Creature a,Creature b){
        if(a==null||b==null)throw new IllegalArgumentException("missing_creature");
        return 5*Math.max(gap(a.x,a.width,b.x,b.width),
            Math.max(gap(a.y,a.width,b.y,b.width),gap(a.z,a.height,b.z,b.height)));
    }
    private static boolean inBounds(int w,int h,int d,int x,int y,int z){
        return x>=0&&y>=0&&z>=0&&x<w&&y<h&&z<d;
    }
    private static boolean volumeInside(Creature c,int w,int h,int d,boolean[][][] opaque){
        if(c.x+c.width>w||c.y+c.width>h||c.z+c.height>d)return false;
        for(int z=c.z;z<c.z+c.height;z++)for(int y=c.y;y<c.y+c.width;y++)
            for(int x=c.x;x<c.x+c.width;x++)if(opaque[z][y][x])return false;
        return true;
    }
    private static final class Ray {
        final boolean clear;final int cover;
        Ray(boolean clear,int cover){this.clear=clear;this.cover=cover;}
    }
    /** Supercover voxel traversal: every cell touched at an edge/corner is
     * inspected, so a diagonal cannot tunnel through an opaque corner. */
    private static Ray ray(int w,int h,int d,int ax,int ay,int az,int tx,int ty,int tz,
            boolean[][][] opaque,int[][][] cover,boolean[][][] occupied){
        int x=ax,y=ay,z=az,dx=tx-ax,dy=ty-ay,dz=tz-az;
        int sx=Integer.compare(dx,0),sy=Integer.compare(dy,0),sz=Integer.compare(dz,0);
        double mx=dx==0?Double.POSITIVE_INFINITY:.5/Math.abs((double)dx),
            my=dy==0?Double.POSITIVE_INFINITY:.5/Math.abs((double)dy),
            mz=dz==0?Double.POSITIVE_INFINITY:.5/Math.abs((double)dz);
        double ix=dx==0?Double.POSITIVE_INFINITY:1d/Math.abs((double)dx),
            iy=dy==0?Double.POSITIVE_INFINITY:1d/Math.abs((double)dy),
            iz=dz==0?Double.POSITIVE_INFINITY:1d/Math.abs((double)dz);
        int bonus=0,guard=0;
        while(x!=tx||y!=ty||z!=tz){
            if(++guard>w+h+d+4)return new Ray(false,0);
            double min=Math.min(mx,Math.min(my,mz));
            boolean stepX=Math.abs(mx-min)<1e-11,stepY=Math.abs(my-min)<1e-11,
                stepZ=Math.abs(mz-min)<1e-11;
            for(int mask=1;mask<8;mask++){
                if((mask&1)!=0&&!stepX||(mask&2)!=0&&!stepY||(mask&4)!=0&&!stepZ)continue;
                int nx=x+((mask&1)!=0?sx:0),ny=y+((mask&2)!=0?sy:0),
                    nz=z+((mask&4)!=0?sz:0);
                if(!inBounds(w,h,d,nx,ny,nz)||opaque[nz][ny][nx])return new Ray(false,0);
                if(nx==tx&&ny==ty&&nz==tz)continue;
                bonus=Math.max(bonus,cover[nz][ny][nx]);
                if(occupied[nz][ny][nx])bonus=Math.max(bonus,2);
            }
            if(stepX){x+=sx;mx+=ix;}if(stepY){y+=sy;my+=iy;}if(stepZ){z+=sz;mz+=iz;}
        }
        return new Ray(true,bonus);
    }
    public static Result evaluate(int width,int height,int levels,Creature attacker,Creature target,
            boolean[][][] opaque,int[][][] cover,boolean[][][] occupied,
            String kind,int reach,int normal,int far,boolean targetLocationKnown,
            boolean targetVisible,int blindsightFeet){
        if(width<1||width>100||height<1||height>100||levels<1||levels>20||
            (long)width*height*levels>250000||attacker==null||target==null||
            opaque==null||cover==null||occupied==null||opaque.length!=levels||
            cover.length!=levels||occupied.length!=levels||
            !TacticalAttackGeometryRules.validProfile(kind,reach,normal,far)||
            blindsightFeet<0||blindsightFeet>120||blindsightFeet%5!=0)
            return fail("invalid_creature_grid_or_weapon");
        for(int z=0;z<levels;z++){
            if(opaque[z]==null||cover[z]==null||occupied[z]==null||
                opaque[z].length!=height||cover[z].length!=height||occupied[z].length!=height)
                return fail("invalid_creature_grid_shape");
            for(int y=0;y<height;y++){
                if(opaque[z][y]==null||cover[z][y]==null||occupied[z][y]==null||
                    opaque[z][y].length!=width||cover[z][y].length!=width||
                    occupied[z][y].length!=width)return fail("invalid_creature_grid_shape");
                for(int x=0;x<width;x++)if(cover[z][y][x]!=0&&cover[z][y][x]!=2&&
                    cover[z][y][x]!=5)return fail("invalid_cover_value");
            }
        }
        if(!volumeInside(attacker,width,height,levels,opaque)||
            !volumeInside(target,width,height,levels,opaque)||overlaps(attacker,target))
            return fail("invalid_or_overlapping_creatures");
        if(!targetLocationKnown)return fail("target_location_unknown");
        int closest=closestDistanceFeet(attacker,target);
        if("melee".equals(kind)&&closest>reach)return fail("target_out_of_reach");
        if("ranged".equals(kind)&&closest>far)return fail("target_out_of_range");
        int bestCover=6,bestDistance=Integer.MAX_VALUE;boolean bestBlind=false;
        for(int az=attacker.z;az<attacker.z+attacker.height;az++)
        for(int ay=attacker.y;ay<attacker.y+attacker.width;ay++)
        for(int ax=attacker.x;ax<attacker.x+attacker.width;ax++)
        for(int tz=target.z;tz<target.z+target.height;tz++)
        for(int ty=target.y;ty<target.y+target.width;ty++)
        for(int tx=target.x;tx<target.x+target.width;tx++){
            int distance=5*Math.max(Math.abs(ax-tx),Math.max(Math.abs(ay-ty),Math.abs(az-tz)));
            if(distance==0||"melee".equals(kind)&&distance>reach||
                "ranged".equals(kind)&&distance>far)continue;
            Ray ray=ray(width,height,levels,ax,ay,az,tx,ty,tz,opaque,cover,occupied);
            if(!ray.clear)continue;
            if(ray.cover<bestCover||ray.cover==bestCover&&distance<bestDistance){
                bestCover=ray.cover;bestDistance=distance;
                bestBlind=!targetVisible&&blindsightFeet>=distance;
            }
        }
        if(bestDistance==Integer.MAX_VALUE)return fail("line_of_sight_blocked");
        CRC32 crc=new CRC32();StringBuilder snapshot=new StringBuilder(128+width*height*levels*3);
        snapshot.append("creature-grid-v1|").append(width).append('|').append(height).append('|').append(levels)
            .append('|').append(attacker.x).append(',').append(attacker.y).append(',').append(attacker.z)
            .append(',').append(attacker.width).append(',').append(attacker.height)
            .append('|').append(target.x).append(',').append(target.y).append(',').append(target.z)
            .append(',').append(target.width).append(',').append(target.height)
            .append('|').append(kind).append('|').append(reach).append('|').append(normal).append('|')
            .append(far).append('|').append(targetVisible).append('|').append(blindsightFeet).append('|');
        for(int z=0;z<levels;z++)for(int y=0;y<height;y++)for(int x=0;x<width;x++)
            snapshot.append(opaque[z][y][x]?'X':'.').append(cover[z][y][x]==5?'3':
                cover[z][y][x]==2?'H':'.').append(occupied[z][y][x]?'C':'.');
        byte[] data=snapshot.toString().getBytes(StandardCharsets.UTF_8);crc.update(data,0,data.length);
        boolean longRange="ranged".equals(kind)&&bestDistance>normal;
        boolean unseen=!targetVisible&&!bestBlind;
        return new Result("",Long.toHexString(crc.getValue()),bestDistance,bestCover,
            longRange,unseen,bestBlind);
    }
    public static String receipt(Result result){
        if(result==null||!result.legal())throw new IllegalArgumentException("invalid_creature_geometry");
        return "creaturegeom-v1:"+result.fingerprint+":"+result.distanceFeet+":"+
            result.coverBonus+":"+(result.longRangeDisadvantage?1:0)+":"+
            (result.unseenDisadvantage?1:0)+":"+(result.usedBlindsight?1:0);
    }
    public static boolean validReceipt(String raw){
        if(raw==null)return false;
        String[] p=raw.split(":",-1);
        if(p.length!=7||!"creaturegeom-v1".equals(p[0])||
            !p[1].matches("[0-9a-f]{1,8}"))return false;
        try{
            int distance=Integer.parseInt(p[2]),cover=Integer.parseInt(p[3]);
            if(distance<5||distance>1200||distance%5!=0||
                !(cover==0||cover==2||cover==5))return false;
            for(int i=4;i<=6;i++)if(!("0".equals(p[i])||"1".equals(p[i])))return false;
            return !("1".equals(p[5])&&"1".equals(p[6]));
        }catch(NumberFormatException invalid){return false;}
    }
}
