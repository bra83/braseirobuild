package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.Locale;

/**
 * Runtime narrativo/sandbox do GAMEPLAY-001.
 *
 * Este engine NÃO resolve mecânica determinística. Ele separa perguntas de ações,
 * mantém continuidade de mundo, produz pedidos explícitos de gateway e bloqueia
 * mutações vindas de narração/Gemini. Rolagens permanecem manuais.
 */
public final class GameplaySandboxRuntimeEngine {
    public static final int STATE_VERSION = 1;
    public static final String SOURCE_FAERUN = "Forgotten Realms: Heroes of Faerûn";
    public static final String SOURCE_EBERRON = "Eberron: Forge of the Artificer";

    private static final String[] FAERUN_REGIONS = {
        "Anauroch","Arcane Empires","Forgotten Lands","Heartlands","Lands of Intrigue",
        "The North","Old Empires","Sword Coast","Trackless Sea","Vilhon Reach","Beyond"
    };
    private static final String[] EBERRON_FRAMEWORKS = {
        "Sharn Inquisitives","Dragonmarked Intrigue","Morgrave Expeditions"
    };
    private static final String[] NARRATIVE_KEYS = {
        "narration","description","dialogue","lore_context","context","justification",
        "suggested_action","proposed_event","rumor","npc_context","clue_context"
    };
    private static final String[] MECHANICAL_KEYS = {
        "hp","pv","hit_points","temp_hp","spell_slot","spell_slots","spell_slots_current",
        "item","inventory","grant_item","condition","conditions","renown","faction_renown",
        "deity_renown","roll","dice","save","saving_throw","resource","resources","cost",
        "gold","gp","position","location","current_location","damage","healing","ac","armor_class"
    };

    private GameplaySandboxRuntimeEngine(){}

    public static JSONObject initialState(){
        JSONObject s=new JSONObject();
        try{
            s.put("gameplay_sandbox_version",STATE_VERSION)
             .put("surface_language","pt-BR")
             .put("auto_rolls",false)
             .put("revision",0)
             .put("current_setting","")
             .put("current_region","")
             .put("current_location","")
             .put("region_context",new JSONObject())
             .put("travel",new JSONObject().put("status","idle"))
             .put("eberron_framework_context",new JSONObject())
             .put("faction_context",new JSONObject())
             .put("deity_context",new JSONObject())
             .put("narrative_flags",new JSONObject())
             .put("relevant_events",new JSONArray())
             .put("last_gateway",new JSONObject())
             .put("last_action",new JSONObject());
        }catch(Exception ignored){}
        return s;
    }

    public static JSONObject ensureState(JSONObject input){
        JSONObject s=copy(input);
        try{
            if(s.optInt("gameplay_sandbox_version",0)<=0)s.put("gameplay_sandbox_version",STATE_VERSION);
            s.put("surface_language","pt-BR").put("auto_rolls",false);
            if(!s.has("revision"))s.put("revision",0);
            ensureObject(s,"region_context");ensureObject(s,"travel");ensureObject(s,"eberron_framework_context");
            ensureObject(s,"faction_context");ensureObject(s,"deity_context");ensureObject(s,"narrative_flags");ensureObject(s,"last_gateway");ensureObject(s,"last_action");
            if(s.optJSONArray("relevant_events")==null)s.put("relevant_events",new JSONArray());
            if(!s.has("current_setting"))s.put("current_setting","");
            if(!s.has("current_region"))s.put("current_region","");
            if(!s.has("current_location"))s.put("current_location","");
        }catch(Exception ignored){}
        return s;
    }

    public static JSONArray faerunRegions(){
        JSONArray out=new JSONArray();
        try{for(String name:FAERUN_REGIONS)out.put(new JSONObject()
            .put("name",name).put("kind","region")
            .put("source",SOURCE_FAERUN).put("source_locator","Chapter 2: A Guide to the Realms — Regions of the Realms")
            .put("gameplay_classification","region_context").put("mechanical_rule_compiled",false));}catch(Exception ignored){}
        return out;
    }

    public static JSONArray eberronFrameworks(){
        JSONArray out=new JSONArray();
        try{for(String name:EBERRON_FRAMEWORKS)out.put(new JSONObject()
            .put("name",name).put("source",SOURCE_EBERRON).put("gameplay_kind","sandbox_framework")
            .put("mechanical_resolution","external_gateway"));}catch(Exception ignored){}
        return out;
    }

    /** Prévia imutável: pergunta e intenção nunca viram ação concluída. */
    public static JSONObject previewInput(JSONObject state,String text,String inputChannel,JSONObject payload){
        JSONObject out=new JSONObject();if(payload==null)payload=new JSONObject();
        try{
            String kind=classifyInput(text,inputChannel);
            out.put("input_kind",kind).put("auto_rolls",false).put("state_mutation_allowed",false).put("preview_only",true);
            if("QUERY".equals(kind))return out.put("response_mode","answer_only").put("world_mutation",false).put("resource_mutation",false);
            boolean requiresTest=payload.optBoolean("requires_test",false);
            if(requiresTest&&!hasManualResult(payload))return out.put("status","pending_manual_result").put("manual_result_required",true).put("gateway_required",true);
            if(requiresTest&&!payload.optBoolean("mechanical_resolution_confirmed",false))return out.put("status","pending_gateway_resolution").put("manual_result_required",false).put("gateway_required",true);
            if("INTENT".equals(kind)&&!payload.optBoolean("action_confirmed",false))return out.put("status","intent_only").put("action_confirmation_required",true);
            return out.put("status","ready_for_confirmed_commit").put("gateway_required",requiresTest);
        }catch(Exception e){return error(out,e);}
    }

    /**
     * Aplica somente continuidade narrativa já resolvida. Se houve teste, o gateway
     * externo deve confirmar a resolução; este engine nunca compara dado/DC.
     */
    public static JSONObject resolveInput(JSONObject state,String text,String inputChannel,JSONObject payload){
        JSONObject before=ensureState(state);JSONObject preview=previewInput(before,text,inputChannel,payload);JSONObject out=copy(before);if(payload==null)payload=new JSONObject();
        try{
            String kind=preview.optString("input_kind","");
            if("QUERY".equals(kind))return result(out,false,"query_no_mutation",preview);
            if(!"ready_for_confirmed_commit".equals(preview.optString("status","")))return result(out,false,preview.optString("status","pending"),preview);
            if(!payload.optBoolean("confirm",false))return result(out,false,"confirmation_required",preview.put("confirm_required",true));
            String outcome=payload.optString("resolved_outcome",payload.optString("outcome","")).trim();
            if(payload.optBoolean("requires_test",false)&&outcome.length()==0)return result(out,false,"resolved_outcome_required",preview.put("resolved_outcome_required",true));
            if("failure".equals(normalize(outcome))||"falha".equals(normalize(outcome)))return result(out,false,"resolved_without_state_change",preview.put("mechanical_outcome","failure"));
            String destination=payload.optString("destination",payload.optString("resolved_location","")).trim();
            if(destination.length()>0)out.put("current_location",destination);
            String setting=payload.optString("setting","").trim();if(setting.length()>0)out.put("current_setting",setting);
            String region=canonical(FAERUN_REGIONS,payload.optString("region",""));if(region.length()>0)out.put("current_region",region);
            JSONObject regionContext=payload.optJSONObject("region_context");if(regionContext!=null)out.put("region_context",copy(regionContext));
            bump(out);out.put("last_action",new JSONObject().put("text",text==null?"":text).put("kind",kind).put("outcome",outcome.length()==0?"confirmed":outcome));
            JSONObject meta=new JSONObject().put("mechanical_outcome",outcome.length()==0?"confirmed":outcome).put("scene_location_after",out.optString("current_location",""));
            return result(out,true,"resolved",meta);
        }catch(Exception e){return errorResult(out,e);}
    }

    public static JSONObject beginTravel(JSONObject state,String origin,String destination,String service,JSONObject payload){
        JSONObject out=ensureState(state);if(payload==null)payload=new JSONObject();
        try{
            String o=safe(origin),d=safe(destination),svc=safe(service);
            JSONObject meta=new JSONObject().put("origin",o).put("destination",d).put("service",svc).put("auto_rolls",false);
            if(o.length()==0||d.length()==0)return result(out,false,"origin_destination_required",meta);
            if(!payload.optBoolean("confirm",false))return result(out,false,"travel_confirmation_required",meta.put("resource_spend_allowed",false).put("destination_change_allowed",false));
            JSONObject travel=new JSONObject().put("origin",o).put("destination",d).put("service",svc).put("status","awaiting_engine_gateway")
                .put("resource_spend_committed",false).put("destination_committed",false);
            out.put("travel",travel);bump(out);
            return result(out,true,"travel_gateway_required",meta.put("gateway_required",true).put("resource_spend_allowed",false).put("destination_change_allowed",false));
        }catch(Exception e){return errorResult(out,e);}
    }

    /** Só conclui a transição depois que o gateway mecânico confirmou seu próprio commit. */
    public static JSONObject completeTravelAfterGateway(JSONObject state,JSONObject gatewayResult,JSONObject payload){
        JSONObject out=ensureState(state);if(gatewayResult==null)gatewayResult=new JSONObject();if(payload==null)payload=new JSONObject();
        try{
            JSONObject travel=out.optJSONObject("travel");if(travel==null||!"awaiting_engine_gateway".equals(travel.optString("status","")))return result(out,false,"travel_not_staged",new JSONObject());
            boolean gatewayResolved=gatewayResult.optBoolean("frhof_resolved",gatewayResult.optBoolean("eberron_resolved",false));
            if(!gatewayResolved)return result(out,false,"mechanical_gateway_not_resolved",new JSONObject().put("destination_change_allowed",false));
            if(!payload.optBoolean("engine_resource_commit_confirmed",false))return result(out,false,"engine_resource_commit_required",new JSONObject().put("destination_change_allowed",false).put("resource_spend_allowed",false));
            travel.put("status","resolved").put("resource_spend_committed",true).put("destination_committed",true);
            out.put("travel",travel).put("current_location",travel.optString("destination",out.optString("current_location","")));
            bump(out);recordLastGateway(out,"travel_service",gatewayResult);
            return result(out,true,"travel_resolved",new JSONObject().put("scene_location_after",out.optString("current_location","")).put("resource_spend_allowed",true));
        }catch(Exception e){return errorResult(out,e);}
    }

    public static JSONObject recordFrameworkGateway(JSONObject state,String framework,JSONObject gatewayResult,JSONObject payload){
        JSONObject out=ensureState(state);if(gatewayResult==null)gatewayResult=new JSONObject();if(payload==null)payload=new JSONObject();
        try{
            if(!gatewayResult.optBoolean("eberron_resolved",false))return result(out,false,"framework_gateway_not_resolved",new JSONObject().put("gateway",copy(gatewayResult)));
            String canonical=canonical(EBERRON_FRAMEWORKS,framework);if(canonical.length()==0)return result(out,false,"framework_unknown",new JSONObject());
            JSONObject ctx=out.optJSONObject("eberron_framework_context");if(ctx==null)ctx=new JSONObject();ctx.put("active",canonical).put("source",SOURCE_EBERRON);
            if(canonical.equals("Sharn Inquisitives")){
                putIfText(ctx,"situation",payload);copyArrayOrObject(ctx,"locations",payload);copyArrayOrObject(ctx,"npcs",payload);copyArrayOrObject(ctx,"clues",payload);
            }else if(canonical.equals("Dragonmarked Intrigue")){
                copyArrayOrObject(ctx,"houses",payload);copyArrayOrObject(ctx,"relations",payload);putIfText(ctx,"reputation_context",payload);copyArrayOrObject(ctx,"consequences",payload);
            }else if(canonical.equals("Morgrave Expeditions")){
                String phase=payload.optString("phase","").trim();if(phase.length()>0&&isExpeditionPhase(phase))ctx.put("phase",canonicalPhase(phase));
                putIfText(ctx,"expedition_id",payload);copyArrayOrObject(ctx,"preparation",payload);copyArrayOrObject(ctx,"exploration",payload);copyArrayOrObject(ctx,"return_state",payload);
            }
            out.put("eberron_framework_context",ctx);bump(out);recordLastGateway(out,"eberron_framework",gatewayResult);
            return result(out,true,"framework_context_persisted",new JSONObject().put("framework",canonical));
        }catch(Exception e){return errorResult(out,e);}
    }

    public static JSONObject recordFactionGateway(JSONObject state,String faction,JSONObject gatewayResult,JSONObject payload){
        JSONObject out=ensureState(state);if(gatewayResult==null)gatewayResult=new JSONObject();if(payload==null)payload=new JSONObject();
        try{
            if(!gatewayResult.optBoolean("frhof_resolved",false))return result(out,false,"faction_gateway_not_resolved",new JSONObject().put("gateway",copy(gatewayResult)));
            JSONObject ctx=out.optJSONObject("faction_context");if(ctx==null)ctx=new JSONObject();JSONObject entry=ctx.optJSONObject(faction);if(entry==null)entry=new JSONObject();
            entry.put("last_event",gatewayResult.optString("frhof_event","")).put("source",SOURCE_FAERUN);
            if(payload.has("relationship"))entry.put("relationship",payload.optString("relationship",""));
            if(payload.has("justification"))entry.put("last_justification",payload.optString("justification",""));
            ctx.put(faction,entry);out.put("faction_context",ctx);bump(out);recordLastGateway(out,"faerun_faction",gatewayResult);
            return result(out,true,"faction_context_persisted",new JSONObject().put("faction",faction));
        }catch(Exception e){return errorResult(out,e);}
    }

    public static JSONObject recordDeityGateway(JSONObject state,String deity,JSONObject gatewayResult,JSONObject payload){
        JSONObject out=ensureState(state);if(gatewayResult==null)gatewayResult=new JSONObject();if(payload==null)payload=new JSONObject();
        try{
            if(!gatewayResult.optBoolean("frhof_resolved",false))return result(out,false,"deity_gateway_not_resolved",new JSONObject().put("gateway",copy(gatewayResult)));
            JSONObject ctx=out.optJSONObject("deity_context");if(ctx==null)ctx=new JSONObject();JSONObject entry=ctx.optJSONObject(deity);if(entry==null)entry=new JSONObject();
            entry.put("last_event",gatewayResult.optString("frhof_event","")).put("source",SOURCE_FAERUN);
            if(payload.has("justification"))entry.put("last_justification",payload.optString("justification",""));
            ctx.put(deity,entry);out.put("deity_context",ctx);bump(out);recordLastGateway(out,"faerun_deity_renown",gatewayResult);
            return result(out,true,"deity_context_persisted",new JSONObject().put("deity",deity));
        }catch(Exception e){return errorResult(out,e);}
    }

    /** Gemini pode propor texto/contexto; chaves mecânicas e de posição são sempre recusadas. */
    public static JSONObject gateGeminiProposal(JSONObject proposal){
        JSONObject out=new JSONObject(),allowed=new JSONObject();JSONArray blocked=new JSONArray();if(proposal==null)proposal=new JSONObject();
        try{
            Iterator<String> it=proposal.keys();while(it.hasNext()){
                String key=it.next();Object value=proposal.opt(key);String normalized=normalizeKey(key);
                if(matchesAny(normalized,MECHANICAL_KEYS)){blocked.put(key);continue;}
                if(matchesAny(normalized,NARRATIVE_KEYS))allowed.put(key,value);
            }
            out.put("allowed_narrative",allowed).put("blocked_mutations",blocked).put("mechanical_mutation_blocked",blocked.length()>0)
               .put("state_mutation_allowed",false).put("auto_rolls",false).put("gateway_required_for_mechanics",true);
        }catch(Exception e){return error(out,e);}
        return out;
    }

    public static JSONObject contextSnapshot(JSONObject state,JSONObject scene){
        JSONObject out=new JSONObject();JSONObject s=ensureState(state);if(scene==null)scene=new JSONObject();
        try{out.put("setting",s.optString("current_setting","")).put("region",s.optString("current_region","")).put("location",s.optString("current_location",scene.optString("location","")))
            .put("day",scene.optInt("day_number",1)).put("clock_hour",scene.optDouble("clock_hour",9d)).put("time_of_day",scene.optString("time_of_day","Manhã"))
            .put("weather",scene.optString("weather","")).put("travel",copy(s.optJSONObject("travel"))).put("framework",copy(s.optJSONObject("eberron_framework_context")))
            .put("factions",copy(s.optJSONObject("faction_context"))).put("deities",copy(s.optJSONObject("deity_context"))).put("auto_rolls",false);
        }catch(Exception e){return error(out,e);}return out;
    }

    public static String classifyInput(String text,String inputChannel){
        String channel=inputChannel==null?"":inputChannel.trim().toUpperCase(Locale.ROOT);String n=normalize(text);
        if("GM_HELP".equals(channel))return "QUERY";
        if(n.endsWith("?")||startsAny(n,"o que ","oque ","consigo ","tem ","ha ","onde ","quem ","como ","posso ","vejo ","estou vendo "))return "QUERY";
        if("PLAYER_ACTION".equals(channel))return "ACTION";
        if(startsAny(n,"quero ","quero tentar ","tento ","vou tentar ","pretendo ","gostaria de "))return "INTENT";
        return "ACTION";
    }

    private static boolean hasManualResult(JSONObject p){return p.has("manual_result")||p.has("manual_total")||p.has("manual_roll")||p.has("manual_check_total")||p.has("manual_save_total")||p.has("manual_attack_total");}
    private static boolean isExpeditionPhase(String p){String n=normalize(p);return n.equals("preparation")||n.equals("preparacao")||n.equals("transit")||n.equals("deslocamento")||n.equals("exploration")||n.equals("exploracao")||n.equals("return")||n.equals("retorno");}
    private static String canonicalPhase(String p){String n=normalize(p);if(n.equals("preparation")||n.equals("preparacao"))return "preparation";if(n.equals("transit")||n.equals("deslocamento"))return "transit";if(n.equals("exploration")||n.equals("exploracao"))return "exploration";return "return";}
    private static void putIfText(JSONObject target,String key,JSONObject source)throws Exception{String v=source.optString(key,"").trim();if(v.length()>0)target.put(key,v);}
    private static void copyArrayOrObject(JSONObject target,String key,JSONObject source)throws Exception{Object v=source.opt(key);if(v instanceof JSONArray)target.put(key,new JSONArray(v.toString()));else if(v instanceof JSONObject)target.put(key,new JSONObject(v.toString()));else if(v!=null&&String.valueOf(v).trim().length()>0)target.put(key,v);}
    private static void recordLastGateway(JSONObject state,String type,JSONObject gateway)throws Exception{state.put("last_gateway",new JSONObject().put("type",type).put("resolved",true).put("status",gateway.optString("frhof_status",gateway.optString("eberron_status","resolved"))).put("event",gateway.optString("frhof_event",gateway.optString("eberron_event",""))));}
    private static void bump(JSONObject s)throws Exception{s.put("revision",Math.max(0,s.optInt("revision",0))+1).put("auto_rolls",false).put("surface_language","pt-BR");}
    private static JSONObject result(JSONObject state,boolean mutated,String status,JSONObject meta){JSONObject out=new JSONObject();try{return out.put("state",state).put("sandbox_mutated",mutated).put("status",status).put("meta",meta==null?new JSONObject():meta).put("auto_rolls",false);}catch(Exception e){return out;}}
    private static JSONObject errorResult(JSONObject state,Exception e){return result(state,false,"error",error(new JSONObject(),e));}
    private static JSONObject error(JSONObject out,Exception e){try{return out.put("error",e==null?"runtime_error":e.getClass().getSimpleName()).put("auto_rolls",false);}catch(Exception ignored){return out;}}
    private static void ensureObject(JSONObject s,String key)throws Exception{if(s.optJSONObject(key)==null)s.put(key,new JSONObject());}
    private static JSONObject copy(JSONObject x){try{return x==null?new JSONObject():new JSONObject(x.toString());}catch(Exception e){return new JSONObject();}}
    private static String safe(String x){return x==null?"":x.trim();}
    private static boolean startsAny(String n,String... prefixes){for(String p:prefixes)if(n.startsWith(normalize(p)))return true;return false;}
    private static boolean matchesAny(String key,String[] values){for(String v:values){String q=normalizeKey(v);if(key.equals(q)||key.startsWith(q+"_")||key.endsWith("_"+q)||key.contains("_"+q+"_"))return true;}return false;}
    private static String canonical(String[] values,String input){String n=normalize(input);for(String v:values)if(normalize(v).equals(n))return v;return "";}
    private static String normalizeKey(String value){return normalize(value).replace(' ','_');}
    private static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replace('’','\'').trim().replaceAll("\\s+"," ");}
}
