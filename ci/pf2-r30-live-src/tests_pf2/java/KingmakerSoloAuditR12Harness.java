package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class KingmakerSoloAuditR12Harness {
    private static void require(boolean c,String m){if(!c)throw new IllegalStateException(m);}

    private static JSONObject checkpoint() throws Exception {
        return new JSONObject()
                .put("story_started",1)
                .put("current_hex","10.10")
                .put("destination_hex","11.10")
                .put("route_json",new JSONArray().put("11.10"))
                .put("scene_day",44)
                .put("scene_period","Noite")
                .put("scene_weather","chuva")
                .put("kingdom_turn_number",12)
                .put("kingdom_turn_phase","ACTIVITY")
                .put("turn_id",77)
                .put("turn_number",12)
                .put("turn_phase","ACTIVITY")
                .put("turn_status","active")
                .put("turn_resource_roll_json",new JSONObject().put("rp",12).put("consumption_resolved",true))
                .put("camp_key","camp_1")
                .put("camp_status","active")
                .put("camp_phase","RANDOM_ENCOUNTER")
                .put("camp_hex","10.10")
                .put("camp_day",44)
                .put("camp_hours",3)
                .put("camp_encounter_json",new JSONObject().put("triggered",true).put("resolved",false).put("roll",1))
                .put("active_companions",new JSONArray().put("linzi"))
                .put("companion_states",new JSONArray().put(new JSONObject().put("companion_key","linzi").put("available",1).put("assignment","camping").put("influence",4).put("attitude","friendly").put("personal_quest_status","in_progress").put("personal_quest_notes","hook")))
                .put("pending_companion_events",1)
                .put("pending_companion_event_states",new JSONArray().put(new JSONObject().put("event_key","event_linzi").put("event_type","companion").put("status","pending").put("source_ref","companion:linzi").put("title","Linzi").put("notes","hook")))
                .put("pending_exploration_events",1)
                .put("pending_exploration_event_states",new JSONArray().put(new JSONObject().put("event_key","event_hex").put("event_type","exploration").put("status","pending").put("source_ref","exploration:10.10:clear").put("title","Hex").put("notes","clear")))
                .put("active_wars",1)
                .put("active_war_states",new JSONArray().put(new JSONObject().put("encounter_key","war_1").put("round_number",2).put("active_army_key","army_a").put("status","active").put("battlefield_hex","10.10").put("participants_json",new JSONArray().put("army_a").put("army_b")).put("armies",new JSONArray().put(new JSONObject().put("army_key","army_a").put("hp",4).put("max_hp",4).put("conditions_json","[]").put("location_hex","10.10")).put(new JSONObject().put("army_key","army_b").put("hp",2).put("max_hp",4).put("conditions_json","[]").put("location_hex","10.10")))))
                .put("kingdom_action","phase_choice")
                .put("next_hex","11.10");
    }

    private static JSONObject clone(JSONObject x)throws Exception{return new JSONObject(x.toString());}
    private static void mustDiffer(JSONObject base,JSONObject changed,String field)throws Exception{
        JSONObject r=KingmakerContinuityContract.compare(base,changed);
        require(!r.optBoolean("ok",true),"divergência não detectada: "+field);
        boolean found=false;JSONArray d=r.optJSONArray("differences");for(int i=0;i<d.length();i++)if(field.equals(d.optString(i)))found=true;
        require(found,"campo não apareceu nas diferenças: "+field+" => "+r);
    }

    public static void main(String[] args)throws Exception{
        JSONObject base=checkpoint(),same=clone(base).put("turn_id",999);
        same.optJSONObject("camp_encounter_json").put("roll",1.0d);
        require(KingmakerContinuityContract.equivalent(base,same),"IDs físicos e 1/1.0 deveriam ser equivalentes");

        JSONObject x=clone(base);x.put("turn_phase","EVENT");mustDiffer(base,x,"turn_phase");
        x=clone(base);x.put("camp_hours",99);mustDiffer(base,x,"camp_hours");
        x=clone(base);x.optJSONArray("active_war_states").optJSONObject(0).put("round_number",99);mustDiffer(base,x,"active_war_states");
        x=clone(base);x.optJSONArray("companion_states").optJSONObject(0).put("influence",99);mustDiffer(base,x,"companion_states");
        x=clone(base);x.optJSONArray("pending_companion_event_states").optJSONObject(0).put("source_ref","companion:wrong");mustDiffer(base,x,"pending_companion_event_states");

        List<JSONObject> companions=new ArrayList<JSONObject>();
        companions.add(new JSONObject().put("companion_key","a").put("available",0));
        companions.add(new JSONObject().put("companion_key","b").put("available",1));
        List<JSONObject> events=new ArrayList<JSONObject>();
        events.add(new JSONObject().put("id",1).put("event_type","companion").put("source_ref","companion:a"));
        events.add(new JSONObject().put("id",2).put("event_type","companion").put("source_ref","companion:b"));
        JSONObject chosen=KingmakerSoloEventPolicy.choose(events,companions,1);
        require(chosen.optLong("id")==2,"evento de companion indisponível não pode vencer a seleção");
        require("b".equals(KingmakerSoloEventPolicy.companionKey(new JSONObject().put("key","b"))),"alias key deve permanecer compatível");
        require("b".equals(KingmakerSoloEventPolicy.companionKey(new JSONObject().put("companion_key","b"))),"companion_key deve ser canônico");

        System.out.println("KINGMAKER_SOLO_AUDIT_R12_PASS continuity=5 companion_policy=3");
    }
}
