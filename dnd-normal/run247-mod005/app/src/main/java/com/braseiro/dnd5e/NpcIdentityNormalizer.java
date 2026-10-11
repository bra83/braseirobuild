package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Única porta de normalização de identidade de NPC da versão 5.0. */
public final class NpcIdentityNormalizer {
    private static final Pattern NAME_FIELD=Pattern.compile("(?i)(?:\\\"?name\\\"?|\\\"?nome\\\"?)\\s*[:=]\\s*[\\\"']?([^,}\\\"']{2,100})");
    private NpcIdentityNormalizer(){}

    public static String displayName(Object raw){
        if(raw==null)return "";
        if(raw instanceof JSONObject){
            JSONObject o=(JSONObject)raw;
            String n=first(o.optString("name"),o.optString("nome"),o.optString("display_name"),o.optString("npc_name"));
            return clean(n);
        }
        String s=String.valueOf(raw).trim();
        if(s.startsWith("{")||s.toLowerCase(Locale.ROOT).contains("name:" )||s.toLowerCase(Locale.ROOT).contains("name=")){
            try{return displayName(new JSONObject(s));}catch(Exception ignored){AppLog.ignored("NpcIdentityNormalizer","Falha tolerada registrada na consolidação 5.0.",ignored);}
            Matcher m=NAME_FIELD.matcher(s);if(m.find())return clean(m.group(1));
        }
        return clean(s);
    }

    public static boolean isReservedPlayerAlias(String raw){
        String n=key(displayName(raw));
        if(n.length()==0)return true;
        return n.equals("voce")||n.equals("jogador")||n.equals("jogadora")||n.equals("player")||
                n.equals("grupo")||n.equals("companhia")||n.equals("party")||n.equals("heroi")||n.equals("herois")||
                n.equals("aventureiro")||n.equals("aventureiros")||n.equals("eu")||n.equals("nos")||n.equals("eles")||n.equals("elas");
    }

    public static String stableId(String worldProfile,String raw){
        String name=displayName(raw);if(isReservedPlayerAlias(name))return "";
        String seed=(worldProfile==null?"dnd":worldProfile.trim().toLowerCase(Locale.ROOT))+"|"+key(name);
        return "npc-"+UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static JSONObject normalize(JSONObject source,String worldProfile){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();if(source==null)return out;
        try{
            java.util.Iterator<String> it=source.keys();while(it.hasNext()){String k=it.next();out.put(k,source.opt(k));}
            String name=displayName(source);
            if(name.length()==0||isReservedPlayerAlias(name))return new JSONObject();
            out.put("name",name).put("stable_id",stableId(worldProfile,name));
        }catch(Exception e){AppLog.w("NpcIdentityNormalizer","Falha ao normalizar NPC.",e);return new JSONObject();}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static String key(String raw){
        String s=raw==null?"":Normalizer.normalize(raw,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9]+"," ").trim().replaceAll("\\s+"," ");
    }
    private static String clean(String s){
        if(s==null)return "";String x=s.trim().replaceAll("^[\\{\\[\\\"']+|[\\}\\]\\\"']+$","");
        int cut=x.indexOf(',');if(cut>1&&x.substring(0,cut).length()<100)x=x.substring(0,cut);
        x=x.replaceAll("\\s+"," ").trim();return x.length()>100?x.substring(0,100).trim():x;
    }
    private static String first(String... values){for(String v:values)if(v!=null&&!v.trim().isEmpty())return v;return "";}
}
