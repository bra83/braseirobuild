package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * RUN224: spell-slot authority for class-level histories. Does not infer prepared
 * spells, does not merge Pact Magic into Spellcasting, and never writes SQLite.
 *
 * All classes in a history must be known. Malformed/partial histories fail closed
 * so a preview cannot silently replace the character's remaining spell slots.
 */
public final class MulticlassSpellSlotRules {
    private MulticlassSpellSlotRules() {}

    public static final class Result {
        public final String error;
        public final boolean applicable;
        public final int casterLevel;
        public final int[] slots;
        public final int pactLevel;
        public final int classCount;
        private Result(String error,boolean applicable,int casterLevel,int[] slots,int pactLevel,int classCount){
            this.error=error;this.applicable=applicable;this.casterLevel=casterLevel;
            this.slots=slots;this.pactLevel=pactLevel;this.classCount=classCount;
        }
        public boolean valid(){return error.isEmpty();}
    }
    private static Result error(String why){return new Result(why,false,0,new int[9],0,0);}

    public static Result evaluate(JSONObject sheet,String primaryClass,int characterLevel,String profile){
        if(characterLevel<1||characterLevel>20)return error("invalid_character_level");
        boolean revised="2024".equals(DndRulesEngine.canonicalProfile(profile)) ||
                DndRulesEngine.PROFILE_2024.equals(DndRulesEngine.canonicalProfile(profile));
        JSONArray levels=sheet==null?null:sheet.optJSONArray("class_levels");
        boolean explicit=sheet!=null&&sheet.has("class_levels");
        if(explicit&&(levels==null||levels.length()==0))return error("invalid_class_levels_ledger");
        if(!explicit){
            levels=new JSONArray();
            try{levels.put(new JSONObject().put("class_name",primaryClass).put("level",characterLevel));}
            catch(Exception ex){return error("class_levels_unavailable");}
        }
        Set<String> seen=new HashSet<String>();
        int total=0,combined=0,pact=0,casters=0;
        boolean multi=levels.length()>1;
        for(int i=0;i<levels.length();i++){
            JSONObject row=levels.optJSONObject(i);
            if(row==null)return error("invalid_class_level_entry");
            String name=canonical(row.optString("class_name",row.optString("class","")));
            int lvl=row.optInt("level",0);
            if(name.isEmpty())return explicit?error("uncompiled_class_progression"):
                    new Result("",false,0,new int[9],0,1);
            if(lvl<1||lvl>20||!seen.add(name))return error("invalid_or_duplicate_class_level");
            total+=lvl;
            if("warlock".equals(name)){pact=lvl;continue;}
            if("bard".equals(name)||"cleric".equals(name)||"druid".equals(name)||
                    "sorcerer".equals(name)||"wizard".equals(name)){
                combined+=lvl;casters++;continue;
            }
            if("artificer".equals(name)){
                combined+=(lvl+1)/2;casters++;continue;
            }
            if("paladin".equals(name)||"ranger".equals(name)){
                int contribution=revised?(lvl+1)/2:lvl/2;
                if(contribution>0){combined+=contribution;casters++;}
                continue;
            }
            if("fighter".equals(name)||"rogue".equals(name)){
                String subclass=row.optString("subclass_name",row.optString("subclass",""));
                if(subclass.isEmpty()&&!multi&&sheet!=null)
                    subclass=sheet.optString("subclass_name",sheet.optString("subclass",""));
                String sub=norm(subclass);
                boolean third=("fighter".equals(name)&&(sub.contains("eldritch knight")||sub.contains("cavaleiro arcano")))||
                    ("rogue".equals(name)&&(sub.contains("arcane trickster")||sub.contains("trapaceiro arcano")));
                if(third&&lvl>=3){
                    combined+=multi?(revised?(lvl+2)/3:lvl/3):(lvl+2)/3;
                    casters++;
                }
            }
        }
        if(total!=characterLevel)return error("class_levels_do_not_sum_to_character_level");
        if(combined>20)return error("invalid_effective_caster_level");
        int[] max=new int[9];
        if(combined>0){
            // The canonical table is shared with DndRulesEngine; no copied slot chart.
            JSONArray table=new DndRulesEngine().fullCasterSlots(combined);
            if(table==null||table.length()!=9)return error("canonical_slot_table_unavailable");
            for(int i=0;i<9;i++)max[i]=table.optInt(i,0);
        }
        return new Result("",casters>0||pact>0,combined,max,pact,levels.length());
    }

    /** Returns a copy. Invalid resource ledgers are not repaired by resetting slots. */
    public static JSONObject reconcile(JSONObject input,String primaryClass,int level,String profile){
        JSONObject copy;
        try{copy=input==null?new JSONObject():new JSONObject(input.toString());}
        catch(Exception ex){return input==null?new JSONObject():input;}
        Result result=evaluate(copy,primaryClass,level,profile);
        try{
            if(!result.valid())return copy.put("multiclass_slot_validation_error",result.error);
            copy.remove("multiclass_slot_validation_error");
            if(!result.applicable)return copy;
            // Validate both independent pools BEFORE mutating either one. A broken
            // Pact Magic ledger must not leave a half-reconciled shared slot pool.
            JSONArray oldMax=copy.optJSONArray("spell_slots_max"),oldCurrent=copy.optJSONArray("spell_slots_current");
            JSONObject oldPact=copy.optJSONObject("pact_magic");
            if(result.casterLevel>0){
                if((oldMax==null)!=(oldCurrent==null))
                    return copy.put("multiclass_slot_validation_error","incomplete_existing_slot_ledger");
                if(oldMax!=null&&(!validSlots(oldMax)||!validSlots(oldCurrent)||!withinMaximum(oldCurrent,oldMax)))
                    return copy.put("multiclass_slot_validation_error","invalid_existing_slot_ledger");
            }
            if(result.pactLevel>0&&oldPact!=null&&(!oldPact.has("current")||
                oldPact.optInt("current",-1)<0||oldPact.optInt("current")>oldPact.optInt("max",-1)))
                return copy.put("multiclass_slot_validation_error","invalid_existing_pact_ledger");
            if(result.casterLevel>0){
                JSONArray max=new JSONArray(),current=new JSONArray();
                for(int i=0;i<9;i++){
                    int cap=result.slots[i];max.put(cap);
                    int remaining=oldMax==null?cap:
                        Math.max(0,Math.min(cap,cap-(oldMax.optInt(i)-oldCurrent.optInt(i))));
                    current.put(remaining);
                }
                copy.put("spell_slots_max",max).put("spell_slots_current",current)
                    .put("multiclass_effective_caster_level",result.casterLevel);
            }
            if(result.pactLevel>0){
                int lvl=result.pactLevel;
                int cap=lvl==1?1:lvl<=10?2:lvl<=16?3:4;
                int circle=lvl<=2?1:lvl<=4?2:lvl<=6?3:lvl<=8?4:5;
                int remaining=oldPact==null?cap:Math.max(0,Math.min(cap,
                    cap-(oldPact.optInt("max")-oldPact.optInt("current"))));
                copy.put("pact_magic",new JSONObject().put("max",cap).put("current",remaining)
                    .put("slot_level",circle).put("recharge","short_or_long_rest"));
            }
            return copy;
        }catch(Exception ex){
            try{return copy.put("multiclass_slot_validation_error","slot_reconciliation_failed");}
            catch(Exception ignored){return copy;}
        }
    }
    private static boolean withinMaximum(JSONArray current,JSONArray maximum){
        for(int i=0;i<9;i++)if(current.optInt(i)>maximum.optInt(i))return false;
        return true;
    }
    private static boolean validSlots(JSONArray arr){
        if(arr==null||arr.length()!=9)return false;
        for(int i=0;i<9;i++){
            Object value=arr.opt(i);
            if(!(value instanceof Number))return false;
            double number=((Number)value).doubleValue();
            if(!Double.isFinite(number)||number<0||number>99||number!=Math.floor(number))return false;
        }
        return true;
    }
    private static String canonical(String value){
        String n=norm(value);
        if(n.startsWith("artificer")||n.startsWith("artifice"))return "artificer";
        if(n.startsWith("barbar"))return "barbarian";
        if(n.startsWith("guerreiro")||n.startsWith("fighter"))return "fighter";
        if(n.startsWith("paladino")||n.startsWith("paladin"))return "paladin";
        if(n.startsWith("patrulheiro")||n.startsWith("ranger")||n.startsWith("guardiao"))return "ranger";
        if(n.startsWith("ladino")||n.startsWith("rogue"))return "rogue";
        if(n.startsWith("mago")||n.startsWith("wizard"))return "wizard";
        if(n.startsWith("clerigo")||n.startsWith("cleric"))return "cleric";
        if(n.startsWith("druida")||n.startsWith("druid"))return "druid";
        if(n.startsWith("monge")||n.startsWith("monk"))return "monk";
        if(n.startsWith("bardo")||n.startsWith("bard"))return "bard";
        if(n.startsWith("feiticeiro")||n.startsWith("sorcerer"))return "sorcerer";
        if(n.startsWith("bruxo")||n.startsWith("warlock"))return "warlock";
        return "";
    }
    private static String norm(String s){return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
        .replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}
}
