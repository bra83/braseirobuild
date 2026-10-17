package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Source-bound non-condition save effects. Dice and area membership come from the DM.
 * Stored in combatant resources so SQLite, snapshots and hero synchronization share state. */
public final class XmmSaveEffectLedger {
    public static final String FIELD="xmm_save_effects";
    public static final String SCHEMA="xmm_save_effect_v1";
    public static final String SLOW="slowing_breath";
    private XmmSaveEffectLedger() { }

    public static int dc(String block) {
        if("Adult Copper Dragon".equals(block))return 18;
        if("Ancient Copper Dragon".equals(block))return 22;
        if("Copper Dragon Wyrmling".equals(block))return 11;
        if("Young Copper Dragon".equals(block))return 14;
        return 0;
    }
    public static int cone(String block) {
        if("Adult Copper Dragon".equals(block))return 60;
        if("Ancient Copper Dragon".equals(block))return 90;
        if("Copper Dragon Wyrmling".equals(block))return 15;
        if("Young Copper Dragon".equals(block))return 30;
        return 0;
    }
    public static boolean compiled(String block,JSONObject action) {
        return dc(block)>0&&action!=null&&"Slowing Breath".equals(action.optString("name"))&&
            "action".equals(action.optString("category"))&&
            integer(action,"save_dc",dc(block),dc(block))&&"con".equals(action.optString("save_ability"))&&
            SLOW.equals(action.optString("save_effect_kind"))&&
            "target_next_turn_end".equals(action.optString("save_effect_duration"))&&
            integer(action,"target_range_ft",cone(block),cone(block))&&
            "cone".equals(action.optString("target_area_shape"))&&
            action.optString("condition_on_fail","").isEmpty()&&
            action.optString("effect_on_fail","").isEmpty()&&
            action.optString("damage","").isEmpty()&&
            action.optString("recharge","").isEmpty()&&
            !action.optBoolean("manual_unmodeled_source_effect_required",true)&&
            !action.optBoolean("manual_rider_resolution_required",true);
    }
    public static JSONArray read(JSONObject resources,long target) throws Exception {
        if(resources==null||target<=0)throw new IllegalArgumentException("invalid_save_effect_owner");
        Object raw=resources.opt(FIELD);
        if(raw==null)return new JSONArray();
        if(!(raw instanceof JSONArray))throw new IllegalArgumentException("invalid_save_effect_array");
        JSONArray rows=(JSONArray)raw;
        if(rows.length()>100)throw new IllegalArgumentException("save_effect_limit");
        java.util.HashSet<String> receipts=new java.util.HashSet<String>();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.optJSONObject(i);
            if(row==null||!SCHEMA.equals(row.optString("schema"))||
                !SLOW.equals(row.optString("kind"))||
                dc(row.optString("source_statblock"))==0||
                !"Slowing Breath".equals(row.optString("source_action"))||
                !integer(row,"source_actor_id",1,Long.MAX_VALUE)||row.optLong("source_actor_id")==target||
                !integer(row,"target_id",target,target)||
                !integer(row,"applied_round",1,Integer.MAX_VALUE)||
                !integer(row,"applied_turn_index",0,1000)||
                row.optString("encounter_id").isEmpty()||
                !CombatActionReceiptGuard.validToken(row.optString("source_receipt"))||
                !receipts.add(row.optString("source_receipt")))
                throw new IllegalArgumentException("invalid_save_effect_ledger");
        }
        return rows;
    }
    private static boolean integer(JSONObject row,String key,long min,long max) {
        Object value=row.opt(key);
        if(!(value instanceof Number))return false;
        double number=((Number)value).doubleValue();
        return Double.isFinite(number)&&number==Math.floor(number)&&number>=min&&number<=max;
    }
    public static boolean slowed(JSONObject resources,long target) throws Exception {
        return read(resources,target).length()>0;
    }
    public static String spendBlock(JSONObject resources,long target,String cost) throws Exception {
        if(!slowed(resources,target))return "";
        if("reaction".equals(cost))return "slowing_breath_no_reactions";
        if("action".equals(cost)&&!resources.optBoolean("bonus_action_available",true)||
            "bonus_action".equals(cost)&&!resources.optBoolean("action_available",true))
            return "slowing_breath_action_or_bonus";
        return "";
    }
    public static int speed(JSONObject resources,long target,int speed) throws Exception {
        if(speed<0)throw new IllegalArgumentException("invalid_speed");
        return slowed(resources,target)?speed/2:speed;
    }
    public static JSONArray stage(JSONObject resources,long source,long target,String block,
        JSONObject action,String receipt,String encounter,int round,int turnIndex) throws Exception {
        JSONArray existing=read(resources,target);
        if(source<=0||source==target||!compiled(block,action)||
            !CombatActionReceiptGuard.validToken(receipt)||encounter==null||encounter.isEmpty()||
            round<1||turnIndex<0||turnIndex>1000||existing.length()>=100)
            throw new IllegalArgumentException("invalid_save_effect_activation");
        for(int i=0;i<existing.length();i++)if(receipt.equals(existing.getJSONObject(i).optString("source_receipt")))
            throw new IllegalArgumentException("save_effect_replayed");
        JSONArray result=new JSONArray(existing.toString());
        result.put(new JSONObject().put("schema",SCHEMA).put("kind",SLOW)
            .put("source_actor_id",source).put("target_id",target).put("source_statblock",block)
            .put("source_action","Slowing Breath").put("source_receipt",receipt)
            .put("encounter_id",encounter).put("applied_round",round)
            .put("applied_turn_index",turnIndex));
        read(new JSONObject().put(FIELD,result),target);
        return result;
    }
    /** End of the NEXT target turn; application during its current turn does not expire there. */
    public static JSONArray advance(JSONObject resources,long target,String encounter,int round,int turnIndex)
        throws Exception {
        if(encounter==null||encounter.isEmpty()||round<1||turnIndex<0||turnIndex>1000)
            throw new IllegalArgumentException("invalid_save_effect_boundary");
        JSONArray rows=read(resources,target),result=new JSONArray();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i);
            if(!encounter.equals(row.optString("encounter_id")))
                throw new IllegalArgumentException("save_effect_encounter_mismatch");
            boolean later=round>row.optInt("applied_round")||
                round==row.optInt("applied_round")&&turnIndex>row.optInt("applied_turn_index");
            if(!later)result.put(new JSONObject(row.toString()));
        }
        return result;
    }
    public static void remap(JSONObject resources,long oldTarget,java.util.Map<Long,Long> ids) throws Exception {
        JSONArray old=read(resources,oldTarget),next=new JSONArray();
        Long target=ids.get(oldTarget);
        if(target==null||target<=0)throw new IllegalArgumentException("orphan_save_effect_target");
        for(int i=0;i<old.length();i++) {
            JSONObject row=new JSONObject(old.getJSONObject(i).toString());
            Long source=ids.get(row.optLong("source_actor_id"));
            if(source==null||source<=0)throw new IllegalArgumentException("orphan_save_effect_source");
            row.put("source_actor_id",source).put("target_id",target);next.put(row);
        }
        resources.put(FIELD,next);read(resources,target);
    }
}
