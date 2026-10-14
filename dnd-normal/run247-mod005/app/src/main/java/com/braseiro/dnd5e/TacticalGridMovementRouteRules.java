package com.braseiro.dnd5e;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.zip.CRC32;

/** G08 RUN243: deterministic weighted 5-foot grid route over persisted terrain.
 * A cell is difficult ONLY when the scene explicitly declares it. The GM may
 * separately mark the whole proposed route as difficult. No inferred terrain
 * tags, dice rolls, creature overlap or direct SQLite writes occur here.
 * Every chosen step is returned to the opportunity-reaction executor.
 */
public final class TacticalGridMovementRouteRules {
    private TacticalGridMovementRouteRules() {}
    public static final int MAX_DIM=100;
    public static final class Cell {
        public final int x,y;
        private Cell(int x,int y){this.x=x;this.y=y;}
    }
    public static final class Route {
        public final String error,fingerprint;
        public final int startX,startY,endX,endY,steps,traveledFeet,budgetFeet,difficultSteps;
        public final List<Cell> cells;
        private Route(String e,String fp,int sx,int sy,int tx,int ty,int steps,int budget,int difficult,
                      List<Cell> path){
            error=e;fingerprint=fp;startX=sx;startY=sy;endX=tx;endY=ty;
            this.steps=steps;traveledFeet=steps*5;budgetFeet=budget;difficultSteps=difficult;
            cells=Collections.unmodifiableList(new ArrayList<Cell>(path));
        }
        public boolean ok(){return error.isEmpty();}
    }
    private static Route fail(String reason){return new Route(reason,"",-1,-1,-1,-1,0,0,0,
        Collections.<Cell>emptyList());}
    private static boolean finite(double n){return !Double.isNaN(n)&&!Double.isInfinite(n);}
    private static boolean valid(boolean[][] grid,int w,int h){
        if(grid==null||grid.length!=h)return false;
        for(boolean[] row:grid)if(row==null||row.length!=w)return false;
        return true;
    }
    private static final class Node implements Comparable<Node> {
        final int id,cost,steps;
        Node(int id,int cost,int steps){this.id=id;this.cost=cost;this.steps=steps;}
        public int compareTo(Node other){
            if(cost!=other.cost)return Integer.compare(cost,other.cost);
            if(steps!=other.steps)return Integer.compare(steps,other.steps);
            return Integer.compare(id,other.id);
        }
    }
    private static String signature(int w,int h,int sx,int sy,int ex,int ey,
        boolean[][] blocked,boolean[][] difficult,boolean gmDifficult,boolean crawl,
        List<Cell> path){
        CRC32 crc=new CRC32();StringBuilder encoded=new StringBuilder(80+w*h*2+path.size()*8);
        encoded.append("grid-v2|").append(w).append('|').append(h).append('|')
            .append(sx).append('|').append(sy).append('|').append(ex).append('|').append(ey)
            .append('|').append(gmDifficult).append('|').append(crawl).append('|');
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)encoded.append(blocked[y][x]?'1':'0')
            .append(difficult[y][x]?'D':'.');
        encoded.append('|');
        for(Cell c:path)encoded.append(c.x).append(',').append(c.y).append(';');
        byte[] bytes=encoded.toString().getBytes(StandardCharsets.UTF_8);
        crc.update(bytes,0,bytes.length);return Long.toHexString(crc.getValue());
    }
    /** Backward-compatible entrypoint: a scene without explicit difficult cells. */
    public static Route plan(int width,int height,double normalizedX,double normalizedY,
        int targetX,int targetY,boolean[][] blocked,boolean difficult,boolean crawling){
        if(width<1||height<1||width>MAX_DIM||height>MAX_DIM)return fail("invalid_grid");
        return plan(width,height,normalizedX,normalizedY,targetX,targetY,blocked,
            new boolean[height][width],difficult,crawling);
    }
    /** Dijkstra minimizes real movement cost, then step count, then cell index.
     * Entering a declared difficult cell adds five feet. Crawling adds five
     * feet per step; GM whole-route difficult terrain does not stack with cell
     * difficult terrain. The origin is never charged as a destination cell. */
    public static Route plan(int width,int height,double normalizedX,double normalizedY,
        int targetX,int targetY,boolean[][] blocked,boolean[][] difficultCells,
        boolean gmDifficult,boolean crawling){
        if(width<1||height<1||width>MAX_DIM||height>MAX_DIM||
            !valid(blocked,width,height)||!valid(difficultCells,width,height))return fail("invalid_grid");
        if(!finite(normalizedX)||!finite(normalizedY)||normalizedX<0||normalizedX>=1||
            normalizedY<0||normalizedY>=1)return fail("invalid_start_position");
        if(targetX<0||targetX>=width||targetY<0||targetY>=height)return fail("destination_out_of_bounds");
        int sx=(int)Math.floor(normalizedX*width),sy=(int)Math.floor(normalizedY*height);
        if(sx==targetX&&sy==targetY)return fail("destination_is_origin");
        if(blocked[targetY][targetX])return fail("destination_occupied_or_blocked");
        // When a requested horizontal or vertical corridor is clear, keep the
        // selected route straight. Equal-cost Dijkstra ties otherwise favored
        // a bent route, making source rules such as XMM charges impossible to
        // execute even when the player selected an unobstructed straight path.
        List<Cell> straightCells=null;
        int straightCost=0,straightDifficult=0;
        if(sx==targetX||sy==targetY){
            int dx=Integer.compare(targetX,sx),dy=Integer.compare(targetY,sy);
            int count=Math.max(Math.abs(targetX-sx),Math.abs(targetY-sy));
            List<Cell> direct=new ArrayList<Cell>();direct.add(new Cell(sx,sy));
            boolean clear=true;
            for(int step=1;step<=count;step++){
                int x=sx+dx*step,y=sy+dy*step;
                if(blocked[y][x]){clear=false;break;}
                boolean difficult=gmDifficult||difficultCells[y][x];
                if(difficult)straightDifficult++;
                straightCost+=5*(1+(difficult?1:0)+(crawling?1:0));
                direct.add(new Cell(x,y));
            }
            if(clear)straightCells=direct;
        }
        int n=width*height,start=sy*width+sx,goal=targetY*width+targetX;
        int[] cost=new int[n],steps=new int[n],prev=new int[n];
        Arrays.fill(cost,Integer.MAX_VALUE);Arrays.fill(steps,Integer.MAX_VALUE);
        Arrays.fill(prev,-1);cost[start]=0;steps[start]=0;prev[start]=start;
        PriorityQueue<Node> queue=new PriorityQueue<Node>();queue.add(new Node(start,0,0));
        final int[] dx={-1,0,1,-1,1,-1,0,1},dy={-1,-1,-1,0,0,1,1,1};
        while(!queue.isEmpty()){
            Node now=queue.remove();int x=now.id%width,y=now.id/width;
            if(now.cost!=cost[now.id]||now.steps!=steps[now.id])continue;
            if(now.id==goal)break;
            for(int i=0;i<8;i++){
                int nx=x+dx[i],ny=y+dy[i];
                if(nx<0||ny<0||nx>=width||ny>=height||blocked[ny][nx])continue;
                if(dx[i]!=0&&dy[i]!=0&&(blocked[y][nx]||blocked[ny][x]))continue;
                int cell=ny*width+nx;
                int extra=gmDifficult||difficultCells[ny][nx]?1:0;
                int nextCost=now.cost+5*(1+extra+(crawling?1:0));
                int nextSteps=now.steps+1;
                if(nextCost<cost[cell]||nextCost==cost[cell]&&nextSteps<steps[cell]){
                    cost[cell]=nextCost;steps[cell]=nextSteps;prev[cell]=now.id;
                    queue.add(new Node(cell,nextCost,nextSteps));
                }
            }
        }
        if(prev[goal]<0)return fail("no_traversable_route");
        if(steps[goal]<1||steps[goal]>120||cost[goal]>600)return fail("budget_exceeds_safe_limit");
        List<Cell> reverse=new ArrayList<Cell>();int difficultSteps=0;
        for(int id=goal;;id=prev[id]){
            int x=id%width,y=id/width;reverse.add(new Cell(x,y));
            if(id==start)break;
            if(gmDifficult||difficultCells[y][x])difficultSteps++;
        }
        Collections.reverse(reverse);
        // Keep an unobstructed straight corridor on equal-cost routes. Never
        // let this preference override a cheaper weighted path through terrain.
        if(straightCells!=null&&straightCost==cost[goal])return new Route("",
            signature(width,height,sx,sy,targetX,targetY,blocked,difficultCells,
                gmDifficult,crawling,straightCells),sx,sy,targetX,targetY,
            straightCells.size()-1,straightCost,straightDifficult,straightCells);
        return new Route("",signature(width,height,sx,sy,targetX,targetY,blocked,difficultCells,
            gmDifficult,crawling,reverse),sx,sy,targetX,targetY,steps[goal],cost[goal],
            difficultSteps,reverse);
    }
    public static String receipt(String sceneKey,Route route){
        if(route==null||!route.ok()||sceneKey==null||sceneKey.isEmpty())
            throw new IllegalArgumentException("invalid_tactical_movement_receipt");
        return "gridpos-v1:"+Integer.toHexString(sceneKey.hashCode())+":"+route.fingerprint+":"+
            route.startX+":"+route.startY+":"+route.endX+":"+route.endY+":"+route.budgetFeet;
    }
    public static boolean validReceipt(String raw,int expectedCost){
        if(raw==null||!raw.startsWith("gridpos-v1:"))return false;
        String[] p=raw.split(":",-1);
        if(p.length!=8||!p[1].matches("[0-9a-f]{1,8}")||
            !p[2].matches("[0-9a-f]{1,8}"))return false;
        try{
            int x=Integer.parseInt(p[3]),y=Integer.parseInt(p[4]);
            int tx=Integer.parseInt(p[5]),ty=Integer.parseInt(p[6]);
            int spent=Integer.parseInt(p[7]);
            return x>=0&&y>=0&&tx>=0&&ty>=0&&(x!=tx||y!=ty)&&spent==expectedCost;
        }catch(NumberFormatException ex){return false;}
    }
}
