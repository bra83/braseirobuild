package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** RUN234: turns-only condition expiry. A duration is authoritative ONLY if
 * declared as an integer condition_duration_rounds on the compiled action.
 * No prose-to-rules inference. All writes remain in AppDb's combatAtomic.
 */
public final class MonsterConditionTimerLedger {
    public static final String SCHEMA="compiled_monster_condition_timer_v1";
    private MonsterConditionTimerLedger() { }
    public static final class Transition {
        public final JSONArray timers;
        public final List<String> expired;
        public final boolean changed;
        Transition(JSONArray t,List<String> e,boolean c) {
            timers=t;expired=Collections.unmodifiableList(new ArrayList<String>(e));changed=c;
        }
    }
    public static int compiledDuration(JSONObject action) {
        if(action==null)return 0;
        // Recovery uses its own explicit, manually rolled end-of-turn flow.
        // Mixing recovery and fixed duration remains forbidden.
        MonsterConditionRecoveryLedger.Spec recovery=MonsterConditionRecoveryLedger.compiled(action);
        if(recovery!=null)return 0;
        if(!action.has("condition_duration_rounds")){
            if(action.has("condition_duration_tick"))
                throw new IllegalArgumentException("condition_tick_without_duration");
            return 0;
        }
        // A declared round count is executable only with its exact turn-start
        // owner. Source-turn expiry is needed for conditions explicitly lasting
        // until the source creature's next turn.
        String tick=action.optString("condition_duration_tick","");
        if(!"target_turn_start".equals(tick)&&!"source_turn_start".equals(tick))
            throw new IllegalArgumentException("condition_duration_trigger_not_compiled");
        Object raw=action.opt("condition_duration_rounds");
        if(!(raw instanceof Number))throw new IllegalArgumentException("condition_duration_not_integer");
        double d=((Number)raw).doubleValue();
        if(!Double.isFinite(d)||d!=Math.floor(d)||d<1||d>100)
            throw new IllegalArgumentException("condition_duration_out_of_range");
        String onFail=action.optString("condition_on_fail","").trim();
        String onHit=action.optString("condition_on_hit","").trim();
        if(onFail.isEmpty()&&onHit.isEmpty()||!onFail.isEmpty()&&!onHit.isEmpty())
            throw new IllegalArgumentException("condition_duration_without_condition");
        return (int)d;
    }
    public static JSONArray parse(String raw) {
        try{
            JSONArray array=new JSONArray(raw==null?"[]":raw);
            for(int i=0;i<array.length();i++){
                JSONObject o=array.optJSONObject(i);
                if(o==null)throw new IllegalArgumentException("invalid_condition_timer_entry");
                if(!o.has("schema"))continue; // pre-existing legacy timers retain their format
                if(MonsterConditionRecoveryLedger.SCHEMA.equals(o.optString("schema",""))){
                    MonsterConditionRecoveryLedger.validate(o);continue;
                }
                if(!SCHEMA.equals(o.optString("schema", ""))||
                   !o.optString("name","").equals(CompiledMonsterActionGuard.canonicalCondition(o.optString("name","")))||
                   o.optString("name","").isEmpty()||
                   !(o.opt("rounds") instanceof Number)||
                   !Double.isFinite(((Number)o.opt("rounds")).doubleValue())||
                   ((Number)o.opt("rounds")).doubleValue()!=Math.floor(((Number)o.opt("rounds")).doubleValue())||
                   o.optInt("rounds",0)<1||o.optInt("rounds",0)>100||
                   o.optLong("source_actor_id",0)<=0||
                   o.optString("source_statblock","").isEmpty()||
                   o.optString("source_action","").isEmpty()||
                   o.optString("source_receipt","").isEmpty()||
                   !supportedTick(o.optString("trigger","target_turn_start")))
                    throw new IllegalArgumentException("invalid_typed_condition_timer");
            }
            return array;
        }catch(IllegalArgumentException bad){throw bad;}
        catch(Exception err){throw new IllegalArgumentException("malformed_condition_timer_json",err);}
    }
    public static JSONArray stage(String rawTimers,MonsterConditionRiderPlan.Result plan,
        int duration,long sourceActor,String statblock,String action,String receipt) throws Exception {
        return stage(rawTimers,plan,duration,sourceActor,statblock,action,receipt,"target_turn_start");
    }
    public static JSONArray stage(String rawTimers,MonsterConditionRiderPlan.Result plan,
        int duration,long sourceActor,String statblock,String action,String receipt,String tick) throws Exception {
        JSONArray existing=parse(rawTimers);
        if(duration==0)return existing;
        if(duration<1||duration>100||sourceActor<=0||
            statblock==null||statblock.trim().isEmpty()||action==null||action.trim().isEmpty()||
            receipt==null||receipt.trim().isEmpty()||!supportedTick(tick))
            throw new IllegalArgumentException("invalid_timed_rider_authority");
        if(plan==null||!plan.valid()||!plan.alreadyPresent.isEmpty())
            throw new IllegalArgumentException("ambiguous_timed_condition_ownership");
        JSONArray result=new JSONArray(existing.toString());
        for(String applied:plan.applied){
            for(int j=0;j<result.length();j++){
                JSONObject timer=result.getJSONObject(j);
                if(ConditionTokenGuard.has(timer.optString("name",""),applied))
                    throw new IllegalArgumentException("condition_timer_ownership_conflict");
            }
            result.put(new JSONObject().put("schema",SCHEMA).put("name",applied)
                .put("rounds",duration).put("source_actor_id",sourceActor)
                .put("trigger",tick)
                .put("source_statblock",statblock).put("source_action",action)
                .put("source_receipt",receipt));
        }
        return result;
    }
    /** Manual condition removal must also remove typed recovery/timer claims.
     * Legacy timer rows are left untouched to preserve their original rules.
     */
    public static JSONArray pruneMissing(String rawTimers,String currentConditions)throws Exception {
        JSONArray timers=parse(rawTimers),kept=new JSONArray();
        for(int i=0;i<timers.length();i++){
            JSONObject timer=timers.getJSONObject(i);
            if(!timer.has("schema")||ConditionTokenGuard.has(currentConditions,timer.optString("name","")))
                kept.put(new JSONObject(timer.toString()));
        }
        return kept;
    }
    /** Tick at the affected combatant's own turn START, as established in AppDb.
     * Duplicate timers are not allowed to end a condition while another is active.
     * Legacy timer format {name,rounds} keeps its existing decrement semantics.
     */
    public static Transition advance(String rawTimers) throws Exception {
        return advanceInternal(rawTimers,0,0,true);
    }
    /** Advance timers on this combatant's row when the active actor starts a
     * turn. The timer owner is either its target (legacy/default) or its source. */
    public static Transition advance(String rawTimers,long targetActorId,long activeActorId) throws Exception {
        return advanceInternal(rawTimers,targetActorId,activeActorId,false);
    }
    private static Transition advanceInternal(String rawTimers,long targetActorId,
        long activeActorId,boolean legacyTargetOnly) throws Exception {
        JSONArray timers=parse(rawTimers),remaining=new JSONArray();
        List<String> candidates=new ArrayList<String>();
        boolean changed=false;
        for(int i=0;i<timers.length();i++){
            JSONObject old=timers.getJSONObject(i);
            JSONObject timer=new JSONObject(old.toString());
            // Recovery saves are resolved manually at the target's turn end;
            // advancing the turn must never silently expire those records.
            if(MonsterConditionRecoveryLedger.SCHEMA.equals(timer.optString("schema",""))){
                remaining.put(timer);continue;
            }
            String tick=timer.optString("trigger","target_turn_start");
            long clockActor="source_turn_start".equals(tick)?timer.optLong("source_actor_id",0):targetActorId;
            boolean due=legacyTargetOnly?"target_turn_start".equals(tick):
                activeActorId>0&&clockActor>0&&clockActor==activeActorId;
            if(!due){
                remaining.put(timer);continue;
            }
            int rounds=timer.optInt("rounds",0);
            if(rounds>0){timer.put("rounds",rounds-1);changed=true;}
            if(timer.optInt("rounds",0)>0)remaining.put(timer);
            else {candidates.add(timer.optString("name",""));changed=true;}
        }
        List<String> expired=new ArrayList<String>();
        for(String candidate:candidates){
            String canonical=CompiledMonsterActionGuard.canonicalCondition(candidate);
            if(canonical.isEmpty())continue;
            boolean stillActive=false;
            for(int j=0;j<remaining.length();j++){
                if(ConditionTokenGuard.has(remaining.getJSONObject(j).optString("name",""),canonical)){
                    stillActive=true;break;
                }
            }
            if(!stillActive&&!expired.contains(canonical))expired.add(canonical);
        }
        return new Transition(remaining,expired,changed);
    }
    private static boolean supportedTick(String tick) {
        return "target_turn_start".equals(tick)||"source_turn_start".equals(tick);
    }
}
