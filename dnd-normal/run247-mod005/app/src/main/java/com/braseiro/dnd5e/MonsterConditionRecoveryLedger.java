package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Source-bound end-of-target-turn saving throws. Never rolls dice itself. */
public final class MonsterConditionRecoveryLedger {
    public static final String SCHEMA="compiled_monster_condition_recovery_v1";
    private MonsterConditionRecoveryLedger() { }

    public static final class Spec {
        public final int dc;
        public final String ability,condition;
        private Spec(int dc,String ability,String condition){
            this.dc=dc;this.ability=ability;this.condition=condition;
        }
    }
    public static Spec compiled(JSONObject action) {
        if(action==null)return null;
        boolean any=action.has("condition_recovery_save_dc")||
            action.has("condition_recovery_save_ability")||action.has("condition_recovery_trigger");
        if(!any)return null;
        if(action.has("condition_duration_rounds")||action.has("condition_duration_tick"))
            throw new IllegalArgumentException("combined_timing_and_recovery_unsupported");
        Object raw=action.opt("condition_recovery_save_dc");
        if(!(raw instanceof Number))throw new IllegalArgumentException("recovery_dc_not_integer");
        double d=((Number)raw).doubleValue();
        if(!Double.isFinite(d)||d!=Math.floor(d)||d<1||d>30)
            throw new IllegalArgumentException("recovery_dc_out_of_range");
        String ability=action.optString("condition_recovery_save_ability","").trim().toLowerCase(java.util.Locale.ROOT);
        if(!ability.matches("str|dex|con|int|wis|cha"))
            throw new IllegalArgumentException("invalid_recovery_save_ability");
        if(!"target_turn_end".equals(action.optString("condition_recovery_trigger","")))
            throw new IllegalArgumentException("invalid_recovery_trigger");
        String rawCondition=action.optString("condition_on_fail","").trim();
        String condition=CompiledMonsterActionGuard.canonicalCondition(rawCondition);
        if(condition.isEmpty()||rawCondition.contains(",")||rawCondition.contains(";")||
            rawCondition.matches(".*[0-9].*"))
            throw new IllegalArgumentException("recovery_requires_single_compiled_condition");
        return new Spec((int)d,ability,condition);
    }

    public static void validate(JSONObject timer) {
        Object rawDc=timer==null?null:timer.opt("save_dc");
        Object rawRound=timer==null?null:timer.opt("last_attempt_round");
        if(timer==null||!SCHEMA.equals(timer.optString("schema",""))||
            !timer.optString("name","").equals(CompiledMonsterActionGuard.canonicalCondition(timer.optString("name","")))||
            timer.optString("name","").isEmpty()||
            !(rawDc instanceof Number)||!Double.isFinite(((Number)rawDc).doubleValue())||
            ((Number)rawDc).doubleValue()!=Math.floor(((Number)rawDc).doubleValue())||
            timer.optInt("save_dc",0)<1||timer.optInt("save_dc",0)>30||
            !timer.optString("save_ability","").matches("str|dex|con|int|wis|cha")||
            !"target_turn_end".equals(timer.optString("trigger",""))||
            timer.optLong("source_actor_id",0)<=0||
            timer.optString("source_statblock","").isEmpty()||
            timer.optString("source_action","").isEmpty()||
            timer.optString("source_receipt","").isEmpty()||
            !(rawRound instanceof Number)||!Double.isFinite(((Number)rawRound).doubleValue())||
            ((Number)rawRound).doubleValue()!=Math.floor(((Number)rawRound).doubleValue())||
            timer.optInt("last_attempt_round",-1)<-1||timer.optInt("last_attempt_round",-1)==0||
            timer.optString("last_attempt_encounter","").length()>120||
            (timer.optInt("last_attempt_round",-1)>0)==timer.optString("last_attempt_encounter","").isEmpty()||
            timer.has("rounds"))
            throw new IllegalArgumentException("invalid_recovery_timer");
    }

    public static JSONArray stage(JSONArray existing,MonsterConditionRiderPlan.Result plan,
        Spec spec,long sourceActor,String statblock,String action,String receipt)throws Exception {
        if(spec==null)return existing;
        if(plan==null||!plan.valid()||!plan.alreadyPresent.isEmpty()||
            sourceActor<=0||statblock==null||statblock.trim().isEmpty()||
            action==null||action.trim().isEmpty()||receipt==null||receipt.trim().isEmpty())
            throw new IllegalArgumentException("ambiguous_recovery_ownership");
        JSONArray result=new JSONArray(existing.toString());
        for(String applied:plan.applied){
            if(!spec.condition.equals(applied))
                throw new IllegalArgumentException("recovery_condition_mismatch");
            for(int i=0;i<result.length();i++)if(ConditionTokenGuard.has(
                result.getJSONObject(i).optString("name",""),applied))
                throw new IllegalArgumentException("recovery_condition_ownership_conflict");
            JSONObject timer=new JSONObject().put("schema",SCHEMA).put("name",applied)
                .put("save_dc",spec.dc).put("save_ability",spec.ability)
                .put("trigger","target_turn_end").put("source_actor_id",sourceActor)
                .put("source_statblock",statblock).put("source_action",action)
                .put("source_receipt",receipt).put("last_attempt_round",-1)
                .put("last_attempt_encounter","");
            validate(timer);result.put(timer);
        }
        return result;
    }

    public static final class Resolution {
        public final JSONArray timers;
        public final boolean success,alreadyAttempted,found;
        public final int dc;
        public final String ability,condition;
        private Resolution(JSONArray timers,boolean success,boolean attempted,boolean found,
            int dc,String ability,String condition){
            this.timers=timers;this.success=success;this.alreadyAttempted=attempted;
            this.found=found;this.dc=dc;this.ability=ability;this.condition=condition;
        }
    }
    public static Resolution resolve(String rawTimers,String requested,int saveTotal,
        String encounterId,int round)throws Exception {
        JSONArray timers=MonsterConditionTimerLedger.parse(rawTimers);
        String rawRequested=requested==null?"":requested.trim();
        String condition=CompiledMonsterActionGuard.canonicalCondition(rawRequested);
        if(rawRequested.matches(".*[0-9].*")||rawRequested.contains(",")||rawRequested.contains(";")||
            condition.isEmpty()||saveTotal<1||saveTotal>100||
            encounterId==null||encounterId.isEmpty()||round<1)
            throw new IllegalArgumentException("invalid_recovery_roll_authority");
        JSONArray result=new JSONArray();boolean found=false,success=false,attempted=false;
        int dc=0;String ability="";
        for(int i=0;i<timers.length();i++){
            JSONObject old=timers.getJSONObject(i),timer=new JSONObject(old.toString());
            if(SCHEMA.equals(timer.optString("schema",""))&&condition.equals(timer.optString("name",""))){
                if(found)throw new IllegalArgumentException("duplicate_recovery_condition");
                found=true;validate(timer);dc=timer.optInt("save_dc");ability=timer.optString("save_ability");
                attempted=encounterId.equals(timer.optString("last_attempt_encounter",""))&&
                    round==timer.optInt("last_attempt_round",-1);
                if(attempted)throw new IllegalArgumentException("recovery_already_attempted_this_turn");
                success=saveTotal>=dc;
                if(success)continue;
                timer.put("last_attempt_encounter",encounterId).put("last_attempt_round",round);
            }
            result.put(timer);
        }
        if(!found)throw new IllegalArgumentException("recovery_condition_not_found");
        return new Resolution(result,success,attempted,true,dc,ability,condition);
    }
    public static List<String> due(String rawTimers,String encounterId,int round)throws Exception {
        List<String> result=new ArrayList<String>();
        if(round<1||encounterId==null||encounterId.isEmpty())return result;
        JSONArray timers=MonsterConditionTimerLedger.parse(rawTimers);
        for(int i=0;i<timers.length();i++){
            JSONObject timer=timers.getJSONObject(i);
            if(!SCHEMA.equals(timer.optString("schema","")))continue;
            validate(timer);
            if(!(round==timer.optInt("last_attempt_round",-1)&&
                encounterId.equals(timer.optString("last_attempt_encounter",""))))
                result.add(timer.optString("name"));
        }
        return result;
    }
}
