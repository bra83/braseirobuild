package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

/**
 * RUN236: portable backup21 identity mapping for compiled monster conditions.
 * Combatant primary keys are reallocated on restore. A timed condition must
 * retain its actual source actor and its historical receipts must refer to
 * the new target; leaving old IDs silently points at unrelated creatures.
 * No rules, die rolls, or elapsed time are inferred from a backup.
 */
public final class MonsterConditionBackupCodec {
    private MonsterConditionBackupCodec() { }

    private static long mapped(long oldId,Map<Long,Long> combatants) {
        Long next=combatants==null?null:combatants.get(oldId);
        if(oldId<=0||next==null||next.longValue()<=0)
            throw new IllegalArgumentException("orphan_compiled_condition_combatant_rollback");
        return next.longValue();
    }

    public static String remapTimers(String raw,Map<Long,Long> combatants)throws Exception {
        JSONArray before=MonsterConditionTimerLedger.parse(raw);
        JSONArray after=new JSONArray();
        for(int i=0;i<before.length();i++){
            JSONObject timer=new JSONObject(before.getJSONObject(i).toString());
            String schema=timer.optString("schema","");
            if(MonsterConditionTimerLedger.SCHEMA.equals(schema)||
                MonsterConditionRecoveryLedger.SCHEMA.equals(schema)) {
                timer.put("source_actor_id",mapped(timer.optLong("source_actor_id",0),combatants));
                // Validate the rewritten row as well as the original one.
                MonsterConditionTimerLedger.parse(new JSONArray().put(timer).toString());
            }
            after.put(timer);
        }
        return after.toString();
    }

    /** The turn-expiration receipt is not a combat-v1 key; its final field is
     * the target combatant ID. Preserve encounter UUID, round and turn index.
     */
    public static String remapReceiptKey(String key,Map<Long,Long> combatants) {
        if(key==null||!key.startsWith("monster-condition-turn-v1:")&&
            !key.startsWith("xmm-save-effect-turn-v1:"))return key;
        int last=key.lastIndexOf(':');
        if(last<"monster-condition-turn-v1:".length()||last==key.length()-1)
            throw new IllegalArgumentException("malformed_condition_turn_receipt_rollback");
        try{
            long old=Long.parseLong(key.substring(last+1));
            return key.substring(0,last+1)+mapped(old,combatants);
        }catch(NumberFormatException invalid){
            throw new IllegalArgumentException("malformed_condition_turn_receipt_rollback",invalid);
        }
    }

    /** Remap ONLY typed G05 receipt fields. Other receipt formats retain their
     * existing versioned remappers, rather than a broad and unsafe ID rewrite.
     */
    public static String remapReceiptSource(String source,Map<Long,Long> combatants)throws Exception {
        if(source==null||!source.trim().startsWith("{")||
            !source.contains("monster_condition_")&&
            !source.contains(MonsterForcedMovementRules.SCHEMA)&&
            !source.contains(TacticalGridMovementProofRules.SCHEMA)&&
            !source.contains(XmmSaveEffectLedger.SCHEMA))return source;
        JSONObject original;
        try{original=new JSONObject(source);}catch(Exception invalid){
            if(source.contains("monster_condition_")||source.contains(MonsterForcedMovementRules.SCHEMA)||
                source.contains(TacticalGridMovementProofRules.SCHEMA)||source.contains(XmmSaveEffectLedger.SCHEMA))
                throw new IllegalArgumentException("malformed_condition_receipt_rollback",invalid);
            return source;
        }
        return remapObject(original,combatants).toString();
    }

    private static JSONObject remapObject(JSONObject original,Map<Long,Long> combatants)throws Exception {
        JSONObject out=new JSONObject(original.toString());
        String schema=out.optString("schema","");
        boolean typed="monster_condition_recovery_v1".equals(schema)||
            "monster_condition_turn_v1".equals(schema)||
            "monster_condition_rider_v2".equals(schema)||
            MonsterForcedMovementRules.SCHEMA.equals(schema)||XmmSaveEffectLedger.SCHEMA.equals(schema);
        if(TacticalGridMovementProofRules.SCHEMA.equals(schema)&&out.has("actor_id"))
            out.put("actor_id",mapped(out.optLong("actor_id",0),combatants));
        if(typed){
            if(out.has("target_id"))out.put("target_id",mapped(out.optLong("target_id",0),combatants));
            if("monster_condition_turn_v1".equals(schema)&&out.has("active_actor_id"))
                out.put("active_actor_id",mapped(out.optLong("active_actor_id",0),combatants));
            if(MonsterForcedMovementRules.SCHEMA.equals(schema))
                out.put("source_actor_id",mapped(out.optLong("source_actor_id",0),combatants));
            if(XmmSaveEffectLedger.SCHEMA.equals(schema)&&out.has("source_actor_id"))
                out.put("source_actor_id",mapped(out.optLong("source_actor_id",0),combatants));
            remapTimerArray(out,"timers_before",combatants);
            remapTimerArray(out,"timers_after",combatants);
            remapTimerArray(out,"condition_timers_before",combatants);
            remapTimerArray(out,"condition_timers_after",combatants);
        }
        // Typed rider transitions can be nested in single attacks, saving
        // throws and each multiattack component. Traverse only JSON objects;
        // ordinary strings, roll totals and source descriptions are untouched.
        java.util.Iterator<String> keys=out.keys();
        java.util.ArrayList<String> names=new java.util.ArrayList<String>();
        while(keys.hasNext())names.add(keys.next());
        for(String name:names){
            if(typed&&(name.equals("timers_before")||name.equals("timers_after")||
                name.equals("condition_timers_before")||name.equals("condition_timers_after")))continue;
            Object value=out.opt(name);
            if(value instanceof JSONObject)out.put(name,remapObject((JSONObject)value,combatants));
            else if(value instanceof JSONArray)out.put(name,remapArray((JSONArray)value,combatants));
        }
        return out;
    }

    private static JSONArray remapArray(JSONArray old,Map<Long,Long> combatants)throws Exception {
        JSONArray result=new JSONArray();
        for(int i=0;i<old.length();i++){
            Object value=old.opt(i);
            if(value instanceof JSONObject)result.put(remapObject((JSONObject)value,combatants));
            else if(value instanceof JSONArray)result.put(remapArray((JSONArray)value,combatants));
            else result.put(value==null?JSONObject.NULL:value);
        }
        return result;
    }

    private static void remapTimerArray(JSONObject receipt,String name,
        Map<Long,Long> combatants)throws Exception {
        if(!receipt.has(name))return;
        JSONArray rows=receipt.optJSONArray(name);
        if(rows==null)throw new IllegalArgumentException("invalid_condition_receipt_timers_rollback");
        receipt.put(name,new JSONArray(remapTimers(rows.toString(),combatants)));
    }
}
