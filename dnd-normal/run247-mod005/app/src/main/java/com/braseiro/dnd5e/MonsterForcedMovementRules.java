package com.braseiro.dnd5e;

import java.util.Locale;

/**
 * RUN237 / G05. Deterministic, manually directed displacement from a compiled
 * saving-throw action. The source action must explicitly contain a single
 * push:Nft or pull:Nft effect; the target's destination is supplied by the
 * human player, not chosen by AI. No action economy or opportunity attack is
 * consumed for forced movement. Unsupported compound effects fail closed.
 *
 * The pure validator is intentionally independent of Android and SQLite.
 */
public final class MonsterForcedMovementRules {
    public static final String SCHEMA="monster_forced_movement_v1";
    private MonsterForcedMovementRules() { }

    public static final class Spec {
        public final String direction;
        public final int maximumFeet;
        public final String accompanyingCondition;
        private Spec(String direction,int maximumFeet,String accompanyingCondition) {
            this.direction=direction;this.maximumFeet=maximumFeet;
            this.accompanyingCondition=accompanyingCondition;
        }
    }

    /** No prose parsing or inference. The two shapes are source-compiled tokens. */
    public static Spec compiled(String effectOnFail,String conditionOnFail) {
        String effect=effectOnFail==null?"":effectOnFail.trim().toLowerCase(Locale.ROOT);
        String condition=conditionOnFail==null?"":conditionOnFail.trim().toLowerCase(Locale.ROOT);
        if(effect.isEmpty()||!effect.matches("(?:push|pull|either):(?:5|10|15|20|25|30|35|40|45|50|55|60)ft(?:;(?:prone|restrained))?"))
            return null;
        int separator=effect.indexOf(':');int suffix=effect.indexOf("ft",separator);
        String direction=effect.substring(0,separator);
        int maximum=Integer.parseInt(effect.substring(separator+1,suffix));
        String rider=effect.indexOf(';')<0?"":effect.substring(effect.indexOf(';')+1);
        if(!rider.isEmpty()&&!rider.equals(condition))return null;
        if(rider.isEmpty()&&!condition.isEmpty())return null;
        return new Spec(direction,maximum,rider);
    }

    public static final class Plan {
        public final String blocked;
        public final int startX,startY,endX,endY,steps,feet;
        private Plan(String blocked,int sx,int sy,int ex,int ey,int steps) {
            this.blocked=blocked;startX=sx;startY=sy;endX=ex;endY=ey;
            this.steps=steps;feet=steps*5;
        }
        public boolean valid(){return blocked.isEmpty();}
    }
    private static Plan blocked(String reason,int sx,int sy,int dx,int dy){
        return new Plan(reason,sx,sy,dx,dy,0);
    }
    /**
     * Checks every traversed cell, not only the destination. Occupancy must
     * include the source creature and all other living combatants; exclude
     * only the moving target. Each step follows one straight 8-way ray and
     * changes distance from the source in the declared direction.
     */
    public static Plan plan(Spec spec,int actorX,int actorY,int targetX,int targetY,
        int destX,int destY,int width,int height,boolean[][] blockedCells,
        boolean[][] occupiedCells) {
        if(spec==null)return blocked("unsupported_compiled_forced_movement",targetX,targetY,destX,destY);
        if(width<1||height<1||width>100||height>100||
            blockedCells==null||occupiedCells==null||
            blockedCells.length!=height||occupiedCells.length!=height)
            return blocked("invalid_tactical_grid",targetX,targetY,destX,destY);
        for(int y=0;y<height;y++)if(blockedCells[y]==null||occupiedCells[y]==null||
            blockedCells[y].length!=width||occupiedCells[y].length!=width)
            return blocked("invalid_tactical_grid",targetX,targetY,destX,destY);
        if(!inside(actorX,actorY,width,height)||!inside(targetX,targetY,width,height)||
            !inside(destX,destY,width,height))
            return blocked("forced_movement_outside_board",targetX,targetY,destX,destY);
        if(actorX==targetX&&actorY==targetY)
            return blocked("source_and_target_overlap",targetX,targetY,destX,destY);
        int deltaX=destX-targetX,deltaY=destY-targetY;
        int steps=Math.max(Math.abs(deltaX),Math.abs(deltaY));
        if(steps<1||steps*5>spec.maximumFeet)
            return blocked("forced_movement_distance_invalid",targetX,targetY,destX,destY);
        if(deltaX!=0&&deltaY!=0&&Math.abs(deltaX)!=Math.abs(deltaY))
            return blocked("forced_movement_not_straight",targetX,targetY,destX,destY);
        int sx=Integer.compare(deltaX,0),sy=Integer.compare(deltaY,0);
        int previous=distance(actorX,actorY,targetX,targetY);
        for(int i=1;i<=steps;i++){
            int x=targetX+i*sx,y=targetY+i*sy;
            if(!inside(x,y,width,height)||(x==actorX&&y==actorY)||
                blockedCells[y][x]||occupiedCells[y][x])
                return blocked("forced_movement_path_blocked",targetX,targetY,destX,destY);
            // RUN245 / G05: diagonal displacement crosses a shared corner.
            // Even if the destination itself is clear, neither orthogonal side
            // cell can be a solid obstacle or an occupied creature cell. This
            // matches the grid's existing no-corner-cut rule and prevents a
            // forced push/pull from passing through walls or creature footprints.
            if(sx!=0&&sy!=0){
                int sideAX=x,sideAY=y-sy;
                int sideBX=x-sx,sideBY=y;
                if(!inside(sideAX,sideAY,width,height)||
                    !inside(sideBX,sideBY,width,height)||
                    blockedCells[sideAY][sideAX]||occupiedCells[sideAY][sideAX]||
                    blockedCells[sideBY][sideBX]||occupiedCells[sideBY][sideBX])
                    return blocked("forced_movement_corner_blocked",targetX,targetY,destX,destY);
            }
            int current=distance(actorX,actorY,x,y);
            boolean wrongDirection="push".equals(spec.direction)?current<=previous:
                "pull".equals(spec.direction)?current>=previous:
                    current==previous;
            if(wrongDirection)
                return blocked("forced_movement_wrong_direction",targetX,targetY,destX,destY);
            previous=current;
        }
        return new Plan("",targetX,targetY,destX,destY,steps);
    }
    private static boolean inside(int x,int y,int w,int h){return x>=0&&y>=0&&x<w&&y<h;}
    private static int distance(int ax,int ay,int bx,int by){return Math.max(Math.abs(ax-bx),Math.abs(ay-by));}
}
