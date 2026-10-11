package com.braseiro.dnd5e;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

/** Roteador de Atlas sobre o grafo de vizinhos pré-computado do pacote. */
public final class AtlasRouter {
    private AtlasRouter() {}
    private static final class Node { final String id; final double f; Node(String id,double f){this.id=id;this.f=f;} }

    public static List<String> route(AppDb db,String packageId,String from,String to,int maxVisited){/*BR_JSON_WRAP*/try{
        if(db==null||!db.isValidMapHex(packageId,from)||!db.isValidMapHex(packageId,to))return Collections.emptyList();
        if(from.equalsIgnoreCase(to))return new ArrayList<String>();
        final JSONObject target=db.getRawMapHex(packageId,to);final double tx=target.optDouble("center_x"),ty=target.optDouble("center_y");
        PriorityQueue<Node> open=new PriorityQueue<Node>(64,new Comparator<Node>(){public int compare(Node a,Node b){return Double.compare(a.f,b.f);}});
        HashMap<String,Double> g=new HashMap<String,Double>();HashMap<String,String> parent=new HashMap<String,String>();HashSet<String> closed=new HashSet<String>();
        g.put(from,0d);open.add(new Node(from,heuristic(db,packageId,from,tx,ty)));int cap=Math.max(100,Math.min(120000,maxVisited));
        while(!open.isEmpty()&&closed.size()<cap){Node n=open.poll();if(closed.contains(n.id))continue;if(n.id.equalsIgnoreCase(to))return reconstruct(parent,from,to);closed.add(n.id);double base=g.containsKey(n.id)?g.get(n.id):Double.POSITIVE_INFINITY;for(String next:db.getMapHexNeighbors(packageId,n.id)){if(closed.contains(next))continue;double candidate=base+edgeCost(db,packageId,n.id,next);Double old=g.get(next);if(old==null||candidate+1e-9<old){g.put(next,candidate);parent.put(next,n.id);open.add(new Node(next,candidate+heuristic(db,packageId,next,tx,ty)));}}}
        return Collections.emptyList();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public static int distance(AppDb db,String packageId,String from,String to){List<String> r=route(db,packageId,from,to,120000);return from!=null&&from.equalsIgnoreCase(to)?0:(r.isEmpty()?-1:r.size());}
    private static List<String> reconstruct(HashMap<String,String> parent,String from,String to){ArrayList<String> out=new ArrayList<String>();String cur=to;while(cur!=null&&!cur.equalsIgnoreCase(from)){out.add(cur);cur=parent.get(cur);}if(cur==null)return Collections.emptyList();Collections.reverse(out);return out;}
    private static double heuristic(AppDb db,String packageId,String id,double tx,double ty){JSONObject h=db.getRawMapHex(packageId,id);if(h.length()==0)return 0;JSONObject p=db.getMapPackageAny(packageId);JSONObject profile;try{profile=new JSONObject(p.optString("atlas_profile_json","{}"));}catch(Exception e){profile=new JSONObject();}JSONObject grid=profile.optJSONObject("grid");double step=grid==null?0:grid.optDouble("column_step_x",0);if(step<=0)return 0d;return .70d*Math.hypot(h.optDouble("center_x")-tx,h.optDouble("center_y")-ty)/step;}
    private static double edgeCost(AppDb db,String packageId,String a,String b){JSONObject x=db.getRawMapHex(packageId,a),y=db.getRawMapHex(packageId,b);if(x.optInt("road_present",0)==1&&y.optInt("road_present",0)==1)return .72;String terrain=y.optString("terrain_primary","").toLowerCase();if(terrain.contains("mont")||terrain.contains("pantan")||terrain.contains("densa"))return 1.28;if(terrain.contains("arido")||terrain.contains("desert"))return 1.12;return 1.0;}
}
