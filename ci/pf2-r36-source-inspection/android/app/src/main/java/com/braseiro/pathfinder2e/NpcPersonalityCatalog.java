package com.braseiro.pathfinder2e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Catálogo editorial local de personalidades. A escolha é determinística e
 * nunca substitui uma personalidade que o Mestre já editou. */
public final class NpcPersonalityCatalog {
    public static final String ASSET="npc/npc_personalities_300.json";
    private final JSONArray profiles;
    public NpcPersonalityCatalog(Context context){profiles=load(context);}
    NpcPersonalityCatalog(JSONArray source){profiles=source==null?new JSONArray():source;}
    public int size(){return profiles.length();}

    public JSONObject select(String npcName,JSONObject memory){
        if(profiles.length()==0)return new JSONObject();
        String evidence=normalize(memory==null?"":memory.optString("summary")+" "+memory.optString("visual_description")+" "+memory.optString("goals")+" "+memory.optString("stats_json"));
        List<JSONObject> candidates=new ArrayList<JSONObject>();int best=-1;
        for(int i=0;i<profiles.length();i++){JSONObject p=profiles.optJSONObject(i);if(p==null)continue;int score=score(evidence,p);if(score>best){candidates.clear();best=score;}if(score==best)candidates.add(p);}
        if(candidates.isEmpty())return new JSONObject();
        return compile(candidates.get(Math.floorMod(normalize(npcName).hashCode(),candidates.size())),best);
    }
    public JSONObject initializeIfBlank(String npcName,JSONObject memory,JSONObject cognitive){JSONObject current=parseObject(cognitive==null?"":cognitive.optString("personality_json","{}"));return current.length()>0?current:select(npcName,memory);}

    private static int score(String evidence,JSONObject p){int total=0;String ancestry=normalize(p.optString("ancestralidade")),job=normalize(p.optString("oficio_ou_funcao")),role=normalize(p.optString("papel_populacional"));if(ancestry.length()>2&&contains(evidence,ancestry))total+=20;if(job.length()>2&&contains(evidence,job))total+=16;for(String word:role.split(" "))if(word.length()>4&&contains(evidence,word))total+=4;JSONArray tags=p.optJSONArray("tags");if(tags!=null)for(int i=0;i<tags.length();i++){String tag=normalize(tags.optString(i));if(tag.length()>3&&contains(evidence,tag))total+=3;}return total;}
    private static JSONObject compile(JSONObject p,int matchScore){JSONObject out=new JSONObject();try{out.put("catalog_profile_id",p.optString("id")).put("catalog_source",ASSET).put("catalog_match_score",matchScore);for(String key:new String[]{"identificacao","papel_populacional","ancestralidade","faixa_etaria","classe_social","oficio_ou_funcao","primeira_impressao","visao_de_mundo","modo_de_falar","desejo_atual","medo_ou_limite","vinculo_local","gancho_de_jogo","uso_em_mesa","confianca_inicial","risco_ou_perigo"})if(p.has(key))out.put(key,p.opt(key));for(String key:new String[]{"personalidade","reacoes","gosta_de","nao_gosta_de","camadas_opcionais","tags"})if(p.has(key))out.put(key,p.opt(key));}catch(Exception ignored){AppLog.ignored("NpcPersonalityCatalog","Perfil inválido ignorado.",ignored);}return out;}
    private static JSONArray load(Context context){try{InputStream in=context.getAssets().open(ASSET);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[32768];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();JSONArray list=new JSONObject(out.toString("UTF-8")).optJSONArray("npcs");return list==null?new JSONArray():list;}catch(Exception error){AppLog.e("NpcPersonalityCatalog","Não foi possível carregar o catálogo de NPCs.",error);return new JSONArray();}}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null||raw.trim().length()==0?"{}":raw);}catch(Exception ignored){return new JSONObject();}}
    private static boolean contains(String text,String phrase){return (" "+text+" ").contains(" "+phrase+" ");}
    private static String normalize(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
