package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Source-bound movement-speed reductions with explicit turn-boundary expiry. */
public final class XmmMovementSpeedEffectLedger {
    public static final String SCHEMA="xmm_movement_speed_effect_v1";
    public static final String FIELD="xmm_movement_speed_effects";
    private XmmMovementSpeedEffectLedger() { }

    public static final class Spec {
        public final int feet;
        public final String expiresOn;
        private Spec(int feet,String expiresOn){this.feet=feet;this.expiresOn=expiresOn;}
    }
    public static Spec compiled(JSONObject action){
        if(action==null)return null;
        int feet=action.optInt("speed_reduction_ft",0);
        String tick=action.optString("speed_reduction_duration_tick","");
        if(feet<5||feet>30||feet%5!=0||
            !("source_turn_start".equals(tick)||"target_turn_end".equals(tick)))return null;
        return new Spec(feet,tick);
    }
    public static final class Transition {
        public final JSONArray effects,expired;
        public final String blocked;
        private Transition(JSONArray effects,JSONArray expired,String blocked){
            this.effects=effects;this.expired=expired;this.blocked=blocked;
        }
        public boolean valid(){return blocked.isEmpty();}
        public boolean changed(){return expired.length()>0;}
    }
    private static JSONArray copy(JSONArray input)throws Exception{
        JSONArray out=new JSONArray();
        if(input==null)return out;
        if(input.length()>100)return null;
        for(int i=0;i<input.length();i++){
            JSONObject row=input.optJSONObject(i);
            if(row==null||!SCHEMA.equals(row.optString("schema",""))||
                row.optLong("source_actor_id",0)<=0||row.optLong("target_id",0)<=0||
                row.optInt("feet",0)<5||row.optInt("feet",0)>30||row.optInt("feet",0)%5!=0||
                row.optInt("applied_round",0)<1||row.optInt("applied_turn_index",-1)<0||
                row.optString("expires_on","").length()==0||row.optString("effect_key","").length()==0||
                row.optString("source_statblock","").length()==0||row.optString("source_action","").length()==0)
                return null;
            out.put(new JSONObject(row.toString()));
        }
        return out;
    }
    private static boolean laterThan(JSONObject row,int round,int turnIndex){
        int appliedRound=row.optInt("applied_round",0),appliedTurn=row.optInt("applied_turn_index",-1);
        return round>appliedRound||(round==appliedRound&&turnIndex>appliedTurn);
    }
    public static Transition apply(JSONArray persisted,Spec spec,long sourceId,long targetId,
        String block,String action,String effectKey,int round,int turnIndex){
        try{
            JSONArray effects=copy(persisted);
            if(effects==null)return new Transition(new JSONArray(),new JSONArray(),"invalid_speed_effect_ledger");
            if(spec==null||sourceId<=0||targetId<=0||block==null||block.trim().isEmpty()||
                action==null||action.trim().isEmpty()||effectKey==null||effectKey.trim().isEmpty()||
                round<1||turnIndex<0||turnIndex>1000||effects.length()>=100)
                return new Transition(effects,new JSONArray(),"invalid_speed_effect_input");
            for(int i=0;i<effects.length();i++)if(effectKey.equals(effects.getJSONObject(i).optString("effect_key")))
                return new Transition(effects,new JSONArray(),"speed_effect_replayed");
            effects.put(new JSONObject().put("schema",SCHEMA).put("effect_key",effectKey)
                .put("source_actor_id",sourceId).put("target_id",targetId).put("feet",spec.feet)
                .put("expires_on",spec.expiresOn).put("applied_round",round)
                .put("applied_turn_index",turnIndex).put("source_statblock",block)
                .put("source_action",action));
            return new Transition(effects,new JSONArray(),"");
        }catch(Exception e){return new Transition(new JSONArray(),new JSONArray(),"invalid_speed_effect_input");}
    }
    public static Transition advance(JSONArray persisted,long boundaryActorId,String boundary,
        int round,int turnIndex){
        try{
            JSONArray effects=copy(persisted),next=new JSONArray(),expired=new JSONArray();
            if(effects==null||boundaryActorId<=0||round<1||turnIndex<0||turnIndex>1000||
                !("source_turn_start".equals(boundary)||"target_turn_end".equals(boundary)))
                return new Transition(new JSONArray(),new JSONArray(),"invalid_speed_effect_boundary");
            for(int i=0;i<effects.length();i++){
                JSONObject row=effects.getJSONObject(i);
                long owner="source_turn_start".equals(boundary)?row.optLong("source_actor_id"):
                    row.optLong("target_id");
                boolean remove=owner==boundaryActorId&&boundary.equals(row.optString("expires_on"))&&
                    laterThan(row,round,turnIndex);
                if(remove)expired.put(row);else next.put(row);
            }
            return new Transition(next,expired,"");
        }catch(Exception e){return new Transition(new JSONArray(),new JSONArray(),"invalid_speed_effect_boundary");}
    }
    public static int penalty(JSONArray persisted,long targetId){
        try{
            JSONArray effects=copy(persisted);if(effects==null||targetId<=0)return -1;
            int total=0;for(int i=0;i<effects.length();i++){
                JSONObject row=effects.optJSONObject(i);
                if(row!=null&&row.optLong("target_id")==targetId)total+=row.optInt("feet",0);
            }
            return Math.min(120,total);
        }catch(Exception e){return -1;}
    }
}
