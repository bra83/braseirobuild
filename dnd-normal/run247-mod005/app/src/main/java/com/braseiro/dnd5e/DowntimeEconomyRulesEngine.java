package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Núcleo compilado de downtime/economia. O motor organiza estado, tempo, custos e
 * pré-condições; resultados mecânicos continuam bloqueados até existir regra autorizada.
 */
public final class DowntimeEconomyRulesEngine {
    public static final String PACK="Braseiro Sandbox — Downtime & Economy Core";
    private DowntimeEconomyRulesEngine(){}

    public static JSONObject descriptor(){JSONObject o=new JSONObject();try{o.put("id","downtime_economy_core").put("book",PACK).put("status","compiled")
            .put("domains",new JSONArray().put("trabalho").put("crafting").put("comercio").put("pesquisa").put("estudo").put("administracao").put("vida_social"))
            .put("capabilities",new JSONArray().put("projetos persistentes").put("ledger econômico").put("reservas de recursos").put("prazos").put("pausa/retomada").put("gate mecânico"))
            .put("principle","tempo e estado persistem; ouro, itens, XP e recursos exigem regra mecânica autorizada");}catch(Exception ignored){}return o;}

    public static String familyForActivity(String type){if(type==null)return "";if(type.equals("trabalho"))return "job";if(type.equals("artesanato"))return "craft";if(type.equals("comercio"))return "trade";if(type.equals("pesquisa"))return "research";if(type.equals("estudo"))return "training";if(type.equals("administracao"))return "administration";if(type.equals("vida_social"))return "social";return "";}

    public static JSONObject procedureFor(String type){JSONObject o=new JSONObject();String family=familyForActivity(type);try{o.put("family",family).put("known",family.length()>0).put("persistent",true).put("time_advances",true).put("mechanical_gate","authorized_rule_required");
        JSONArray phases=new JSONArray().put("planned").put("active").put("paused").put("completed").put("cancelled");o.put("phases",phases);
        if("craft".equals(family))o.put("tracks",new JSONArray().put("recipe_or_blueprint").put("materials").put("tools").put("work_units").put("quality").put("output"));
        else if("trade".equals(family))o.put("tracks",new JSONArray().put("market").put("capital_reserved").put("counterparty").put("transaction").put("profit_or_loss"));
        else if("job".equals(family))o.put("tracks",new JSONArray().put("employer").put("role").put("attendance").put("wage_due"));
        else if("research".equals(family))o.put("tracks",new JSONArray().put("question").put("sources").put("progress").put("discoveries"));
        else if("training".equals(family))o.put("tracks",new JSONArray().put("subject").put("teacher").put("progress").put("qualification"));
        else if("administration".equals(family))o.put("tracks",new JSONArray().put("asset").put("staff").put("expenses").put("income").put("incidents"));
        else if("social".equals(family))o.put("tracks",new JSONArray().put("contacts").put("relationship_changes").put("commitments").put("reputation"));
    }catch(Exception ignored){}return o;}

    public static JSONObject validateMutation(JSONObject mutation, JSONObject mechanicalBasis){JSONObject out=new JSONObject();try{
        boolean mutatesEconomy=mutation!=null&&(mutation.optDouble("gold_delta",0)!=0||mutation.optDouble("resource_delta",0)!=0||mutation.optString("item_grant").trim().length()>0||mutation.optInt("xp_delta",0)!=0);
        boolean authorized=!mutatesEconomy||SupplementContentRegistry.verifyMechanicalBasis(mechanicalBasis);
        out.put("mutates_mechanics",mutatesEconomy).put("authorized",authorized).put("time_state_allowed",true)
           .put("reason",authorized?"ok":"Mutação econômica/mecânica bloqueada sem regra compilada autorizada.");
    }catch(Exception ignored){}return out;}
}
