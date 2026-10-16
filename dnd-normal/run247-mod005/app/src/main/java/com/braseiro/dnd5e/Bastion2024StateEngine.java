package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/**
 * Live, deterministic Bastion state reducer for the DMG 2024 event table.
 * Its output is the SAME state AppDb stores atomically in a character sheet,
 * so Backup21 exports, restores and three SQLite reopens preserve consequences.
 * A duplicate token never double-credits GP, defenders, guest protection or
 * facilities. Dice belong to the event-resolution engine; not to this reducer.
 */
public final class Bastion2024StateEngine {
    private Bastion2024StateEngine() { }

    public static JSONObject initialize(int initialGoldGp, int defenders, JSONArray facilityIds) {
        JSONObject out=new JSONObject();
        try {
            if (initialGoldGp < 0 || initialGoldGp > 100000000 || defenders < 0 || defenders > 100000)
                return blocked("invalid_initial_resources");
            if (facilityIds==null || facilityIds.length()>100)
                return blocked("facility_list_required_or_too_large");
            JSONObject facilities=new JSONObject();
            Set<String> names=new HashSet<>();
            for(int i=0;i<facilityIds.length();i++){
                Object raw=facilityIds.opt(i);
                if(!(raw instanceof String))return blocked("invalid_facility_id");
                String name=((String)raw).trim();
                if(name.isEmpty()||name.length()>100||!names.add(name.toLowerCase(java.util.Locale.ROOT)))
                    return blocked("invalid_or_duplicate_facility_id");
                facilities.put(name,new JSONObject().put("unavailable_turn",0));
            }
            return out.put("status","ok").put("schema","bastion2024_v1")
                .put("gold_gp",initialGoldGp).put("defenders",defenders)
                .put("turn_number",0).put("facilities",facilities)
                .put("event_history",new JSONArray()).put("pending_treasure",new JSONArray())
                .put("refugees",0).put("friendly_monster_protection",0);
        }catch(Exception error){return blocked("initial_state_invalid");}
    }

    public static JSONObject advanceTurn(JSONObject old,String token) {
        JSONObject state=cloneState(old);
        if(!"ok".equals(state.optString("status")))return state;
        if(!validToken(token))return blocked("invalid_operation_token");
        if(usedToken(state,token))return blocked("already_resolved_or_replayed");
        try {
            int n=state.getInt("turn_number");
            if(n>=100000)return blocked("bastion_turn_overflow");
            state.put("turn_number",n+1);
            appendHistory(state,new JSONObject().put("token",token)
                .put("kind","bastion_turn").put("turn",n+1));
            return state;
        }catch(Exception e){return blocked("turn_resolution_failed");}
    }

    public static JSONObject apply(JSONObject old,JSONObject event,String token) {
        JSONObject state=cloneState(old);
        if(!"ok".equals(state.optString("status")))return state;
        if(!validToken(token))return blocked("invalid_operation_token");
        if(usedToken(state,token))return blocked("already_resolved_or_replayed");
        if(event==null || !"ok".equals(event.optString("status")) ||
                !event.optBoolean("event_resolved",false) ||
                !event.optBoolean("requires_dm_confirmation",false) ||
                event.optBoolean("persisted_to_world",true))
            return blocked("source_resolved_event_required");
        try {
            int gold=state.getInt("gold_gp");
            int defenders=state.getInt("defenders");
            long nextGold=(long)gold+event.optInt("gold_delta_gp",0);
            long nextDefenders=(long)defenders+event.optInt("defenders_delta",0);
            if(nextGold<0)return blocked("bastion_treasury_insufficient");
            if(nextGold>100000000 || nextDefenders<0 || nextDefenders>100000)
                return blocked("bastion_resources_out_of_range");
            JSONObject facilities=state.getJSONObject("facilities");
            if(event.optBoolean("shut_down_facility_next_turn",false)){
                String id=event.optString("facility_id","").trim();
                if(!facilities.has(id))return blocked("affected_facility_not_registered");
                JSONObject facility=facilities.optJSONObject(id);
                if(facility==null)return blocked("facility_state_invalid");
                facility.put("unavailable_turn",state.getInt("turn_number")+1);
            }
            int refugees=state.optInt("refugees",0)+event.optInt("refugee_count",0);
            if(refugees>100000)return blocked("refugee_capacity_tracking_overflow");
            JSONArray treasures=state.getJSONArray("pending_treasure");
            if(event.optBoolean("treasure_table_roll_required",false)){
                treasures.put(new JSONObject().put("source_event_roll",event.optInt("d100_roll"))
                    .put("treasure_table",event.optString("treasure_table"))
                    .put("second_roll",event.optInt("treasure_d100"))
                    .put("table_result_pending",true));
            }
            state.put("gold_gp",(int)nextGold).put("defenders",(int)nextDefenders)
                .put("refugees",refugees)
                .put("friendly_monster_protection",
                    state.optInt("friendly_monster_protection",0)+
                    (event.optBoolean("friendly_monster_protects_next_bastion_attack",false)?1:0));
            appendHistory(state,new JSONObject().put("token",token)
                .put("kind","bastion_event").put("turn",state.getInt("turn_number"))
                .put("roll",event.optInt("d100_roll"))
                .put("event",event.optString("event"))
                .put("source_book",event.optString("book"))
                .put("source_page",event.optInt("page"))
                .put("delta_gold",event.optInt("gold_delta_gp"))
                .put("delta_defenders",event.optInt("defenders_delta")));
            return state;
        }catch(Exception e){return blocked("bastion_state_transition_failed");}
    }

    private static void appendHistory(JSONObject state,JSONObject event)throws Exception {
        JSONArray history=state.getJSONArray("event_history");
        if(history.length()>=10000)throw new IllegalStateException("bastion_history_limit");
        history.put(event);
    }

    private static JSONObject cloneState(JSONObject raw) {
        try{
            if(raw==null || !"bastion2024_v1".equals(raw.optString("schema")))
                return blocked("bastion_not_configured");
            JSONObject clone=new JSONObject(raw.toString());
            if(!clone.has("facilities")||!clone.has("event_history")||
                    !clone.has("pending_treasure")||!clone.has("gold_gp")||!clone.has("defenders"))
                return blocked("bastion_state_incomplete");
            return clone.put("status","ok");
        }catch(Exception e){return blocked("bastion_state_invalid");}
    }

    private static boolean usedToken(JSONObject state,String token) {
        JSONArray log=state.optJSONArray("event_history");
        if(log==null)return false;
        for(int i=0;i<log.length();i++){
            JSONObject entry=log.optJSONObject(i);
            if(entry!=null && token.equals(entry.optString("token")))return true;
        }
        return false;
    }
    private static boolean validToken(String token) {
        return token!=null && token.startsWith("ui-bastion-") &&
            token.length()>=14 && token.length()<=96 &&
            CombatActionReceiptGuard.validToken(token);
    }
    private static JSONObject blocked(String reason) {
        JSONObject o=new JSONObject();
        try{o.put("status","blocked").put("bastion_blocked",reason)
            .put("requires_resolution",true).put("persisted_to_world",false);
        }catch(Exception ignored){}
        return o;
    }
}
