package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Domain rules for an opportunity attack caused by one actual grid step.
 * Positions, hostility, sight, conditions and reaction availability must be
 * read from the live encounter by the transactional caller. Coordinates are
 * five-foot cubes; a footprint is square in the horizontal plane. */
public final class OpportunityAttackRules {
    private OpportunityAttackRules() {}

    public static final class Position {
        public final int x, y, elevation, size;
        public Position(int x, int y, int elevation, int size) {
            if (x < 0 || y < 0 || x > 99 || y > 99 || elevation < -100 ||
                    elevation > 100 || size < 1 || size > 4 ||
                    x + size > 100 || y + size > 100)
                throw new IllegalArgumentException("invalid_footprint");
            this.x=x; this.y=y; this.elevation=elevation; this.size=size;
        }
        public String encode() { return x+","+y+","+elevation+","+size; }
    }

    public static final class Context {
        public final long reactorId, moverId;
        public final Position reactor, before, after;
        public final int reachFeet;
        public final boolean hostile, visibleBefore, reactionAvailable;
        public final boolean voluntary, disengaged, teleported;

        public Context(long reactorId, long moverId, Position reactor,
                Position before, Position after, int reachFeet, boolean hostile,
                boolean visibleBefore, boolean reactionAvailable,
                boolean voluntary, boolean disengaged, boolean teleported) {
            this.reactorId=reactorId; this.moverId=moverId;
            this.reactor=reactor; this.before=before; this.after=after;
            this.reachFeet=reachFeet; this.hostile=hostile;
            this.visibleBefore=visibleBefore; this.reactionAvailable=reactionAvailable;
            this.voluntary=voluntary; this.disengaged=disengaged;
            this.teleported=teleported;
        }
    }

    public static final class Decision {
        public final String blocked;
        public final int beforeFeet, afterFeet;
        private Decision(String blocked, int beforeFeet, int afterFeet) {
            this.blocked=blocked; this.beforeFeet=beforeFeet; this.afterFeet=afterFeet;
        }
        public boolean triggered() { return blocked.isEmpty(); }
    }

    private static Decision fail(String reason) { return new Decision(reason,0,0); }

    /** Closest occupied cubes; diagonal distance follows the existing grid rule. */
    public static int distanceFeet(Position a, Position b) {
        if (a==null || b==null) throw new IllegalArgumentException("missing_footprint");
        int dx=Math.max(0,Math.max(a.x-b.x-b.size+1,b.x-a.x-a.size+1));
        int dy=Math.max(0,Math.max(a.y-b.y-b.size+1,b.y-a.y-a.size+1));
        int dz=Math.abs(a.elevation-b.elevation);
        return 5*Math.max(dz,Math.max(dx,dy));
    }

    /** Trigger occurs immediately before the mover leaves reach. The reactor
     * strikes the mover at its BEFORE position. No trigger is inferred from a
     * final destination without inspecting every traversed step. */
    public static Decision evaluate(Context c) {
        if (c==null || c.reactor==null || c.before==null || c.after==null ||
                c.reactorId<=0 || c.moverId<=0 || c.reactorId==c.moverId ||
                c.reachFeet<5 || c.reachFeet>30 || c.reachFeet%5!=0)
            return fail("invalid_opportunity_context");
        if (c.before.size!=c.after.size ||
                Math.max(Math.abs(c.before.x-c.after.x),
                    Math.max(Math.abs(c.before.y-c.after.y),
                        Math.abs(c.before.elevation-c.after.elevation)))!=1)
            return fail("movement_step_required");
        int before=distanceFeet(c.reactor,c.before), after=distanceFeet(c.reactor,c.after);
        if (before==0 || after==0) return fail("overlapping_footprints");
        if (!c.hostile) return fail("not_hostile");
        if (!c.voluntary || c.teleported) return fail("movement_does_not_provoke");
        if (c.disengaged) return fail("disengaged");
        if (!c.visibleBefore) return fail("reactor_cannot_see_mover");
        if (!c.reactionAvailable) return fail("reaction_unavailable");
        if (before>c.reachFeet || after<=c.reachFeet)
            return fail("did_not_leave_reach");
        return new Decision("",before,after);
    }

    /** Enumerates the same shortest path as TacticalGridMovementRouteRules:
     * eight directions, fixed order, and no corner cutting. The caller still
     * compares the existing route fingerprint against its approved preview. */
    public static List<Position> shortestPath(int width, int height, Position start,
            Position end, boolean[][] blocked) {
        if (width<1 || height<1 || width>100 || height>100 || start==null || end==null ||
                start.size!=1 || end.size!=1 || start.elevation!=end.elevation ||
                start.x>=width || end.x>=width || start.y>=height || end.y>=height ||
                blocked==null || blocked.length!=height) return Collections.emptyList();
        for (boolean[] row:blocked)
            if (row==null || row.length!=width) return Collections.emptyList();
        if (blocked[end.y][end.x] || start.x==end.x && start.y==end.y)
            return Collections.emptyList();
        int[][] prev=new int[height][width];
        for (int y=0;y<height;y++) for (int x=0;x<width;x++) prev[y][x]=-1;
        int[] queue=new int[width*height]; int head=0,tail=0;
        int origin=start.y*width+start.x,goal=end.y*width+end.x;
        queue[tail++]=origin;prev[start.y][start.x]=origin;
        final int[] dx={-1,0,1,-1,1,-1,0,1};
        final int[] dy={-1,-1,-1,0,0,1,1,1};
        while (head<tail) {
            int cell=queue[head++],x=cell%width,y=cell/width;
            if (cell==goal) break;
            for (int i=0;i<8;i++) {
                int nx=x+dx[i],ny=y+dy[i];
                if (nx<0 || ny<0 || nx>=width || ny>=height || blocked[ny][nx] ||
                        prev[ny][nx]>=0) continue;
                if (dx[i]!=0 && dy[i]!=0 && (blocked[y][nx] || blocked[ny][x])) continue;
                prev[ny][nx]=cell;queue[tail++]=ny*width+nx;
            }
        }
        if (prev[end.y][end.x]<0) return Collections.emptyList();
        ArrayList<Position> reverse=new ArrayList<Position>();
        for (int cell=goal;;cell=prev[cell/width][cell%width]) {
            reverse.add(new Position(cell%width,cell/width,start.elevation,1));
            if (cell==origin) break;
        }
        Collections.reverse(reverse);
        return Collections.unmodifiableList(reverse);
    }

    /** Persist this alongside the charged reaction and damage in one SQLite
     * transaction. The movement operation token must already be valid. */
    public static String receiptSource(String movementToken, Context c, int stepIndex) {
        if (!CombatActionReceiptGuard.validToken(movementToken) || c==null ||
                stepIndex<1 || stepIndex>120 || !evaluate(c).triggered())
            throw new IllegalArgumentException("invalid_opportunity_receipt");
        return "opportunity-v1:"+movementToken+":"+stepIndex+":"+
            c.reactorId+":"+c.moverId+":"+c.reachFeet+":"+
            c.reactor.encode()+":"+c.before.encode()+":"+c.after.encode();
    }

    public static boolean validReceipt(String source) {
        if (source==null || !source.startsWith("opportunity-v1:")) return false;
        try {
            String[] p=source.split(":",-1);
            if (p.length!=9) return false;
            Position r=parse(p[6]),b=parse(p[7]),a=parse(p[8]);
            Context c=new Context(Long.parseLong(p[3]),Long.parseLong(p[4]),
                r,b,a,Integer.parseInt(p[5]),true,true,true,true,false,false);
            return source.equals(receiptSource(p[1],c,Integer.parseInt(p[2])));
        } catch (RuntimeException invalid) { return false; }
    }

    private static Position parse(String value) {
        String[] p=value.split(",",-1);
        if (p.length!=4) throw new IllegalArgumentException("invalid_position");
        Position result=new Position(Integer.parseInt(p[0]),Integer.parseInt(p[1]),
            Integer.parseInt(p[2]),Integer.parseInt(p[3]));
        if (!value.equals(result.encode())) throw new IllegalArgumentException("noncanonical_position");
        return result;
    }
}
