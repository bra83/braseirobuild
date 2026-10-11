package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Camada política determinística sobre facções persistentes.
 *
 * Um reino continua sendo uma facção para aproveitar a mesma simulação de
 * recursos, territórios, aliados, inimigos, planos e memória. Esta classe só
 * acrescenta o ciclo de vida político: formação, estabilidade, crise,
 * fragmentação e queda. Não cria números econômicos ou danos de personagem.
 */
public final class RealmLifecycleEngine {
    public static final String ENTITY_REALM="realm";
    private RealmLifecycleEngine(){}

    public static boolean isRealm(JSONObject faction){
        if(faction==null)return false;
        if(ENTITY_REALM.equalsIgnoreCase(faction.optString("entity_type")))return true;
        JSONObject resources=parseObject(faction.optString("resources_json","{}"));
        return resources.optBoolean("realm",false)||resources.optBoolean("is_realm",false);
    }

    /** Facções podem tentar virar estado sem que o motor invente um país do nada. */
    public static boolean claimsStatehood(JSONObject faction){
        if(faction==null)return false;JSONObject resources=parseObject(faction.optString("resources_json","{}"));
        return resources.optBoolean("claims_statehood",false)||resources.optBoolean("statehood_claim",false);
    }

    public static JSONObject previewAfterFactionStep(JSONObject faction,JSONObject action,int day){
        JSONObject out=new JSONObject();
        try{
            if(faction==null||action==null)return out;
            boolean realm=isRealm(faction);boolean claimant=claimsStatehood(faction);
            if(!realm&&!claimant)return out;
            JSONObject state=state(faction,day);
            String prior=state.optString("status","stable");
            evolve(state,faction,action);
            String next=classify(state,realm,claimant);
            state.put("status",next).put("last_transition_day",Math.max(1,day));
            out.put("realm_state_json_after",state);
            if(!realm&&"forming".equals(next)){
                out.put("entity_type_after",ENTITY_REALM).put("founded_day_after",Math.max(1,day));
                state.put("status","stable").put("formed_from_faction",true);out.put("realm_state_json_after",state);
                out.put("political_event",event("Fundação política — "+faction.optString("name"),faction.optString("name")+" consolidou autoridade suficiente para funcionar como um novo reino/estado.",day));
            }else if(realm&&isTerminal(next)&&!isTerminal(prior)){
                out.put("ended_day_after",Math.max(1,day));
                out.put("political_event",event("Queda política — "+faction.optString("name"),faction.optString("name")+" deixou de operar como uma autoridade política coesa. Territórios, população e facções sucessoras continuam existindo no mundo.",day));
            }else if(!next.equals(prior)){
                out.put("political_event",event("Mudança política — "+faction.optString("name"),faction.optString("name")+" passou de "+prior+" para "+next+".",day));
            }
        }catch(Exception e){AppLog.w("RealmLifecycle","Falha ao prever ciclo político.",e);}return out;
    }

    public static JSONObject state(JSONObject faction,int day){
        JSONObject state=parseObject(faction==null?"{}":faction.optString("realm_state_json","{}"));
        try{
            putDefault(state,"legitimacy",55);putDefault(state,"treasury",55);putDefault(state,"security",55);putDefault(state,"food",60);putDefault(state,"cohesion",55);putDefault(state,"unrest",20);putDefault(state,"war_exhaustion",0);
            if(!state.has("status"))state.put("status",isRealm(faction)?"stable":(claimsStatehood(faction)?"claimant":"faction"));
            if(!state.has("since_day"))state.put("since_day",Math.max(1,day));
            normalizeStats(state);
        }catch(Exception e){AppLog.w("RealmLifecycle","Falha ao normalizar estado político.",e);}return state;
    }

    private static void evolve(JSONObject s,JSONObject faction,JSONObject action)throws Exception{
        String context=norm(faction.optString("agenda")+" "+faction.optString("current_plan")+" "+action.optString("step_title")+" "+action.optString("summary"));
        int legitimacy=s.optInt("legitimacy"),treasury=s.optInt("treasury"),security=s.optInt("security"),food=s.optInt("food"),cohesion=s.optInt("cohesion"),unrest=s.optInt("unrest"),war=s.optInt("war_exhaustion");
        if(has(context,"guerra","invas","cerco","batalh","militar","conquista")){treasury-=2;security-=1;war+=3;unrest+=1;}
        if(has(context,"comerc","mercado","tribut","rota","caravana","prosper")){treasury+=2;legitimacy+=1;}
        if(has(context,"fome","escassez","praga","seca","colheita perdida")){food-=4;unrest+=4;legitimacy-=2;}
        if(has(context,"colheita","abastec","celeiro","irrig","alimento")){food+=3;unrest-=1;}
        if(has(context,"revolta","rebeli","golpe","cisao","secess","motim")){cohesion-=5;unrest+=6;legitimacy-=4;security-=2;}
        if(has(context,"alian","tratado","casamento","diplomac","reconcili")){cohesion+=2;legitimacy+=2;unrest-=1;}
        if(has(context,"corrup","tirania","massacre","repress")){legitimacy-=3;unrest+=3;}
        if(has(context,"vitoria","triunf","pacific","estabil")){security+=3;legitimacy+=2;unrest-=2;war=Math.max(0,war-2);}
        if(action.optInt("relationship_after",faction.optInt("relationship",0))<-60)unrest+=1;
        s.put("legitimacy",legitimacy).put("treasury",treasury).put("security",security).put("food",food).put("cohesion",cohesion).put("unrest",unrest).put("war_exhaustion",war);normalizeStats(s);
    }

    private static String classify(JSONObject s,boolean realm,boolean claimant){
        int l=s.optInt("legitimacy"),t=s.optInt("treasury"),sec=s.optInt("security"),food=s.optInt("food"),coh=s.optInt("cohesion"),u=s.optInt("unrest"),w=s.optInt("war_exhaustion");
        if(!realm&&claimant){if(l>=55&&sec>=45&&coh>=50&&u<=45)return "forming";return "claimant";}
        if(l<=8||coh<=8||sec<=5||u>=92)return "fallen";
        if(u>=78&&coh<=32)return "civil_war";
        if(food<=20||t<=15||sec<=22||l<=25||u>=65||w>=80)return "crisis";
        if(l>=75&&t>=70&&sec>=70&&coh>=65&&u<=25)return "ascendant";
        if(w>=50||u>=45||food<=35)return "strained";
        return "stable";
    }

    private static boolean isTerminal(String s){return "fallen".equals(s)||"fragmented".equals(s)||"dissolved".equals(s);}
    private static JSONObject event(String title,String description,int day)throws Exception{
        JSONObject consequence=new JSONObject().put("journal_updates",new JSONArray().put(new JSONObject().put("title",title).put("body",description).put("kind","Política")));
        return new JSONObject().put("title",title).put("description",description).put("due_day",Math.max(1,day)).put("consequence_json",consequence);
    }
    private static void normalizeStats(JSONObject s)throws Exception{for(String k:new String[]{"legitimacy","treasury","security","food","cohesion","unrest","war_exhaustion"})s.put(k,Math.max(0,Math.min(100,s.optInt(k))));}
    private static void putDefault(JSONObject o,String k,int v)throws Exception{if(!o.has(k))o.put(k,v);}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw);}catch(Exception e){return new JSONObject();}}
    private static boolean has(String src,String... values){for(String v:values)if(src.contains(norm(v)))return true;return false;}
    private static String norm(String value){if(value==null)return "";return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}
}
