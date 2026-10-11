package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Seleciona o recorte do Mundo Vivo enviado ao Mestre por relevância de cena.
 * O banco continua guardando todas as entidades; este seletor só decide quais
 * cabem no prompt, evitando que "os últimos N" apaguem missões/NPCs antigos
 * porém diretamente relacionados ao local, objetivo ou ação atual.
 */
public final class WorldContextSelector {
    private WorldContextSelector(){}

    public static JSONArray select(List<JSONObject> rows,String focus,int max,String... keys){/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();if(rows==null||max<=0)return out;
        List<Scored> scored=new ArrayList<Scored>();Set<String> tokens=tokens(focus);String normalizedFocus=normalize(focus);
        int ordinal=0;
        for(JSONObject row:rows){if(row==null)continue;scored.add(new Scored(row,score(row,tokens,normalizedFocus,keys),ordinal++));}
        Collections.sort(scored,new Comparator<Scored>(){public int compare(Scored a,Scored b){
            int s=Double.compare(b.score,a.score);if(s!=0)return s;
            long au=a.row.optLong("updated_at",a.row.optLong("created_at",0)),bu=b.row.optLong("updated_at",b.row.optLong("created_at",0));
            int u=Long.compare(bu,au);return u!=0?u:Integer.compare(a.ordinal,b.ordinal);
        }});
        for(int i=0;i<scored.size()&&i<max;i++)out.put(scored.get(i).row);return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static JSONArray select(JSONArray rows,String focus,int max,String... keys){
        List<JSONObject> list=new ArrayList<JSONObject>();if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null)list.add(row);}return select(list,focus,max,keys);
    }

    public static String focus(JSONObject scene,String mapContext,String action){
        StringBuilder b=new StringBuilder();if(action!=null)b.append(action).append(' ');if(mapContext!=null)b.append(mapContext).append(' ');
        if(scene!=null){for(String key:new String[]{"location","hex_id","world_region","objective","weather","danger","present_npcs_json","threats_json","clues_json"})if(scene.has(key))b.append(String.valueOf(scene.opt(key))).append(' ');}return b.toString();
    }

    private static double score(JSONObject row,Set<String> focusTokens,String normalizedFocus,String... keys){
        StringBuilder b=new StringBuilder();if(keys!=null&&keys.length>0){for(String key:keys)if(row.has(key))b.append(String.valueOf(row.opt(key))).append(' ');}else{for(String key:new String[]{"name","npc_name","title","objective","stage","stakes","agenda","current_plan","location","hex_id","description","consequence","faction","current_location","next_action","last_interaction_summary","headquarters"})if(row.has(key))b.append(String.valueOf(row.opt(key))).append(' ');}
        String text=normalize(b.toString());Set<String> rowTokens=tokens(text);double score=0d;
        if(!normalizedFocus.isEmpty()&&!text.isEmpty()&&(normalizedFocus.contains(text)||text.contains(normalizedFocus)))score+=120d;
        for(String token:focusTokens)if(token.length()>=3&&rowTokens.contains(token))score+=token.length()>=7?12d:7d;
        String status=normalize(row.optString("status"));if("active".equals(status)||"accepted".equals(status)||"open".equals(status)||"em_andamento".equals(status))score+=4d;
        if(row.optInt("active",1)==1)score+=1d;
        score+=Math.max(0,Math.min(100,row.optInt("urgency",0)))/40d;
        score+=Math.max(0,Math.min(100,row.optInt("significance",0)))/50d;
        return score;
    }

    private static Set<String> tokens(String raw){Set<String> out=new HashSet<String>();String n=normalize(raw);if(n.isEmpty())return out;for(String p:n.split(" +"))if(p.length()>=3&&!STOP.contains(p))out.add(p);return out;}
    private static String normalize(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static final Set<String> STOP=new HashSet<String>();static{Collections.addAll(STOP,"que","com","para","uma","por","dos","das","and","the","de","do","da","em","no","na","ao","as","os","um","uma","seu","sua","sem","mais","mundo","cena");}
    private static final class Scored{final JSONObject row;final double score;final int ordinal;Scored(JSONObject r,double s,int o){row=r;score=s;ordinal=o;}}
}
