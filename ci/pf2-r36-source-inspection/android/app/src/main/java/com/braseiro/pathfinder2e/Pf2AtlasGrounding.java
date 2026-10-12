package com.braseiro.pathfinder2e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Classifica recortes de mapa sem deixar imagem/lore inventarem geografia. */
public final class Pf2AtlasGrounding {
    public enum Kind { WORLD_MAP, REGIONAL_MAP, CITY_MAP, DUNGEON_MAP, BATTLEMAP, ILLUSTRATION, UNKNOWN }
    private Pf2AtlasGrounding(){}
    public static Kind classify(String caption,String surroundingText,int width,int height){
        String n=norm((caption==null?"":caption)+" "+(surroundingText==null?"":surroundingText));
        if(has(n,"battlemap","battle map","encounter map","grid map","mapa de encontro","mapa tatico"))return Kind.BATTLEMAP;
        if(has(n,"dungeon","level map","floor plan","mapa da masmorra","mapa do nivel","caverna"))return Kind.DUNGEON_MAP;
        if(has(n,"city map","town map","village map","mapa da cidade","mapa da vila"))return Kind.CITY_MAP;
        if(has(n,"regional map","region map","mapa regional","hex map","mapa hex"))return Kind.REGIONAL_MAP;
        if(has(n,"world map","mapa do mundo","map of golarion","golarion map"))return Kind.WORLD_MAP;
        if(has(n,"portrait","illustration","artwork","ilustracao","retrato"))return Kind.ILLUSTRATION;
        if(width>0&&height>0&&Math.max(width,height)/(double)Math.max(1,Math.min(width,height))<1.8&&has(n,"map","mapa","grid","grade"))return Kind.BATTLEMAP;
        return Kind.UNKNOWN;
    }
    public static JSONObject groundedPoi(String name,String kind,String source,int page,double x,double y,boolean confirmed){
        JSONObject o=new JSONObject();
        try{o.put("name",safe(name)).put("kind",safe(kind)).put("source",safe(source)).put("page",Math.max(0,page)).put("x",clamp(x)).put("y",clamp(y)).put("confirmed",confirmed).put("narrative_allowed",confirmed).put("mechanical_authority",false);}catch(Exception ignored){}
        return o;
    }
    public static boolean mayNarrateLocation(JSONObject poi){return poi!=null&&poi.optBoolean("confirmed",false)&&poi.optString("name","").trim().length()>0;}
    private static double clamp(double v){return Math.max(0d,Math.min(1d,v));}
    private static String safe(String v){return v==null?"":v.trim();}
    private static boolean has(String n,String... xs){for(String x:xs)if(n.contains(norm(x)))return true;return false;}
    private static String norm(String s){return Normalizer.normalize((s==null?"":s).toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
}
