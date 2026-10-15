package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read-only, source-pinned resolution of XMM attack condition choices.
 * It records the exact player/die decision as a proposal; AppDb/UI integration
 * remains deliberately separate while the protected combat-flow files are busy.
 */
public final class Xmm2025ConditionChoiceEngine {
    public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
    private static final java.util.Set<String> INPUT_KEYS=new java.util.HashSet<String>(
        java.util.Arrays.asList("monster","name","action","source_pointer",
            "source_blob_sha1","attack_hit","chosen_condition","d4_roll","target_size"));
    private static final Spec[] SPECS={
        new Spec("Death Slaad","Chaos Blade","/monster/119/action/1/entries/0",
            "charmed, frightened, poisoned, incapacitated","d4",1,"source_turn_start",""),
        new Spec("Gray Slaad","Chaos Claw","/monster/223/action/1/entries/0",
            "charmed, frightened, poisoned, incapacitated","d4",1,"source_turn_start",""),
        new Spec("Green Slaad","Chaos Staff","/monster/226/action/1/entries/0",
            "charmed, frightened, poisoned, incapacitated","d4",1,"source_turn_start",""),
        new Spec("Githzerai Psion","Psychic Warp","/monster/205/action/1/entries/0",
            "charmed, prone","player_choice",0,"","large"),
        new Spec("Pixie","Faerie Dust","/monster/347/action/0/entries/0",
            "charmed, poisoned","player_choice",1,"source_turn_start",""),
        new Spec("Pixie Wonderbringer","Faerie Dust","/monster/348/action/1/entries/0",
            "charmed, poisoned","player_choice",1,"source_turn_start","")
    };
    private Xmm2025ConditionChoiceEngine() { }

    private static final class Spec {
        final String monster,action,pointer,choices,mode,tick,maxSize;
        final int rounds;
        Spec(String monster,String action,String pointer,String choices,String mode,
            int rounds,String tick,String maxSize){
            this.monster=monster;this.action=action;this.pointer=pointer;this.choices=choices;
            this.mode=mode;this.rounds=rounds;this.tick=tick;this.maxSize=maxSize;
        }
    }
    private static Spec find(String monster,String action){
        for(Spec s:SPECS)if(s.monster.equals(monster)&&s.action.equals(action))return s;
        return null;
    }
    public static JSONObject profile(String monster,String action){
        Spec s=find(monster,action);if(s==null)return new JSONObject();
        try{return new JSONObject().put("resolved",true).put("source","XMM")
            .put("source_blob_sha1",SOURCE_BLOB).put("monster",s.monster)
            .put("action",s.action).put("source_pointer",s.pointer)
            .put("choice_mode",s.mode).put("condition_choices",s.choices)
            .put("duration_rounds",s.rounds).put("duration_tick",s.tick)
            .put("maximum_target_size",s.maxSize)
            .put("choice_semantics","Githzerai Psion".equals(s.monster)?
                "charmed:source_turn_start;prone:stands_up":"shared_expiry")
            .put("application_integrated",false).put("requires_host_commit",true);
        }catch(Exception ignored){return new JSONObject();}
    }
    public static JSONObject resolve(JSONObject input){
        if(input==null)return blocked("request_required");
        java.util.Iterator<String> keys=input.keys();
        while(keys.hasNext())if(!INPUT_KEYS.contains(keys.next()))
            return blocked("unrecognized_condition_choice_input");
        String monster=input.optString("monster",""),action=input.optString("action","");
        if(input.has("name")&&!monster.equals(input.optString("name","")))
            return blocked("cross_monster_input_rejected");
        Spec s=find(monster,action);if(s==null)return blocked("unverified_condition_choice");
        if(!SOURCE_BLOB.equals(input.optString("source_blob_sha1","")))
            return blocked("source_blob_pin_mismatch");
        if(!s.pointer.equals(input.optString("source_pointer","")))
            return blocked("source_pointer_mismatch");
        JSONObject compiled=findCompiled(s);
        if(compiled==null||!s.choices.equals(compiled.optString("condition_choice_on_hit",""))||
            !compiled.optBoolean("source_bound",false)||
            compiled.optBoolean("manual_unmodeled_source_effect_required",false)!=true||
            compiled.optBoolean("manual_rider_resolution_required",false)!=true||
            compiled.optInt("condition_duration_rounds",0)!=s.rounds||
            !s.tick.equals(compiled.optString("condition_duration_tick",""))||
            !s.maxSize.equals(compiled.optString("condition_choice_max_target_size","")))
            return blocked("compiled_choice_contract_mismatch");
        Object rawHit=input.opt("attack_hit");
        if(!(rawHit instanceof Boolean))return blocked("explicit_attack_hit_required");
        if(rawHit.equals(Boolean.FALSE)&&(input.has("chosen_condition")||input.has("d4_roll")
                ||input.has("target_size")))return blocked("decision_not_allowed_on_miss");
        JSONObject out=new JSONObject();
        try{
            out.put("resolved",true).put("reason",rawHit.equals(Boolean.FALSE)?
                "attack_missed_no_condition":"attack_hit_choice_proposed")
                .put("monster",s.monster).put("action",s.action)
                .put("source_pointer",s.pointer).put("source_blob_sha1",SOURCE_BLOB)
                .put("attack_hit",rawHit).put("effect_applied",false)
                .put("persisted",false).put("auto_rolls",false)
                .put("auto_mutation_allowed",false).put("requires_host_commit",true)
                .put("application_integrated",false).put("manual_source_flag_preserved",true);
            if(!rawHit.equals(Boolean.TRUE))return out.put("proposed_condition","");
            String condition;
            if("d4".equals(s.mode)){
                if(input.has("chosen_condition")||!(input.opt("d4_roll") instanceof Number))
                    return blocked("explicit_d4_roll_required");
                Object value=input.opt("d4_roll");double d=((Number)value).doubleValue();
                if(d!=Math.floor(d)||d<1||d>4)return blocked("d4_roll_out_of_range");
                condition=s.choices.split(", ")[(int)d-1];out.put("d4_roll",(int)d);
            }else{
                if(input.has("d4_roll"))return blocked("d4_roll_not_allowed_for_choice");
                condition=input.optString("chosen_condition","").trim().toLowerCase(java.util.Locale.ROOT);
                boolean allowed=false;for(String option:s.choices.split(", "))if(option.equals(condition))allowed=true;
                if(!allowed)return blocked("condition_choice_not_declared");
                if("prone".equals(condition)){
                    String size=input.optString("target_size","");int rank=sizeRank(size);
                    if(rank<0)return blocked("explicit_target_size_required");
                    if(rank>sizeRank("large"))return blocked("target_exceeds_source_size_limit");
                    out.put("target_size",size);
                }
            }
            int durationRounds=s.rounds;
            String durationTick=s.tick;
            if("Githzerai Psion".equals(s.monster)){
                durationRounds="charmed".equals(condition)?1:0;
                durationTick="charmed".equals(condition)?"source_turn_start":"";
            }
            out.put("proposed_condition",condition).put("choice_mode",s.mode)
                .put("duration_rounds",durationRounds).put("duration_tick",durationTick)
                .put("ends_when_stands_up","prone".equals(condition))
                .put("condition_applied",false);
            return out;
        }catch(Exception failure){return blocked("condition_choice_resolution_error");}
    }
    private static JSONObject findCompiled(Spec s){
        JSONObject block=CompiledStatblockEngine.get(s.monster);
        JSONArray actions=block.optJSONArray("actions");
        if(actions==null)return null;
        for(int i=0;i<actions.length();i++){
            JSONObject a=actions.optJSONObject(i);
            if(a!=null&&s.action.equals(a.optString("name","")))return a;
        }
        return null;
    }
    private static int sizeRank(String size){
        String[] sizes={"tiny","small","medium","large","huge","gargantuan"};
        for(int i=0;i<sizes.length;i++)if(sizes[i].equalsIgnoreCase(size))return i;
        return -1;
    }
    private static JSONObject blocked(String reason){
        try{return new JSONObject().put("resolved",false).put("reason",reason)
            .put("effect_applied",false).put("persisted",false)
            .put("auto_rolls",false).put("auto_mutation_allowed",false);
        }catch(Exception ignored){return new JSONObject();}
    }
}
