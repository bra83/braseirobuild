package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
/** RUN196: classificações explícitas de commit; prévia/bloqueio nunca são gravados. */
public final class CharacterSpellCommitGuard {
    private CharacterSpellCommitGuard(){}
    /** RUN211: resultado imutável de um gasto de espaço da rota PHB 2024.
     * O chamador persiste sheet somente após a ação e os efeitos terem sucesso
     * na mesma transação; um erro deixa a ficha original intocada. */
    public static final class Core2024SlotSpend {
        public final String error;
        public final JSONObject sheet;
        private Core2024SlotSpend(String error,JSONObject sheet){this.error=error;this.sheet=sheet;}
        public boolean allowed(){return error.isEmpty();}
    }
    private static Core2024SlotSpend slotError(String error){return new Core2024SlotSpend(error,null);}
    private static int strictSlotCount(JSONArray array,int index){
        Object value=array.opt(index);
        if(!(value instanceof Number))return -1;
        double number=((Number)value).doubleValue();
        if(!Double.isFinite(number)||number<0||number>Integer.MAX_VALUE||number!=Math.floor(number))return -1;
        return (int)number;
    }
    /** Valida os recursos persistidos sem confiar em optInt, que transforma JSON
     * ausente ou malformado em zero. Arrays têm sempre um índice por círculo 1..9.
     * Não é aplicável às regras 2014, espaços de pacto ou conjuração sem espaço. */
    public static Core2024SlotSpend spendCore2024Slot(String profile,JSONObject before,int slotLevel){
        if(!"2024".equals(profile))return slotError("requires_2024_rules_profile");
        if(slotLevel<1||slotLevel>9)return slotError("invalid_spell_slot_level");
        if(before==null)return slotError("spell_slot_state_missing");
        if(before.has("multiclass_slot_validation_error"))return slotError("invalid_multiclass_slot_ledger");
        JSONArray current=before.optJSONArray("spell_slots_current");
        JSONArray maximum=before.optJSONArray("spell_slots_max");
        if(current==null||maximum==null||current.length()!=9||maximum.length()!=9)
            return slotError("spell_slot_state_missing");
        JSONArray next=new JSONArray();
        for(int i=0;i<9;i++){
            int available=strictSlotCount(current,i),limit=strictSlotCount(maximum,i);
            if(available<0||limit<0||available>limit)return slotError("invalid_spell_slot_state");
            if(i==slotLevel-1){
                if(available==0)return slotError("spell_slot_unavailable");
                next.put(available-1);
            }else next.put(available);
        }
        try{
            JSONObject after=new JSONObject(before.toString());
            after.put("spell_slots_current",next);
            after.put("spell_slot_spent_level",slotLevel);
            return new Core2024SlotSpend("",after);
        }catch(Exception error){return slotError("spell_slot_state_copy_failed");}
    }
    /** Restauração de espaços comuns no descanso longo 2024. A concentração
     * continua sob responsabilidade do executor do efeito correspondente. */
    public static Core2024SlotSpend restoreCore2024Slots(String profile,JSONObject before){
        if(!"2024".equals(profile))return slotError("requires_2024_rules_profile");
        if(before==null)return slotError("spell_slot_state_missing");
        JSONArray current=before.optJSONArray("spell_slots_current");
        JSONArray maximum=before.optJSONArray("spell_slots_max");
        if(current==null||maximum==null||current.length()!=9||maximum.length()!=9)
            return slotError("spell_slot_state_missing");
        JSONArray restored=new JSONArray();
        for(int i=0;i<9;i++){
            int available=strictSlotCount(current,i),limit=strictSlotCount(maximum,i);
            if(available<0||limit<0||available>limit)return slotError("invalid_spell_slot_state");
            restored.put(limit);
        }
        try{
            JSONObject after=new JSONObject(before.toString());
            after.put("spell_slots_current",restored);
            after.remove("spell_slot_spent_level");
            return new Core2024SlotSpend("",after);
        }catch(Exception error){return slotError("spell_slot_state_copy_failed");}
    }
    public static boolean completed(String family,JSONObject s){
        if(s==null)return false;
        if("run149".equals(family))return s.optBoolean("run149_spell_resolved",false)&&!s.optBoolean("run149_spell_replay_ignored",false);
        if("arcana".equals(family))return s.optBoolean("au_spell_resolved",false);
        if("frhof".equals(family))return s.optBoolean("frhof_resolved",false);
        if("eb_homunculus".equals(family))return s.optBoolean("eberron_resolved",false);
        return false;
    }
    public static boolean pendingCommit(String family,JSONObject s){
        if(s==null)return false;
        if("arcana".equals(family))return s.optBoolean("au_spell_cast_committed",false)&&!s.optString("au_pending_spell","").isEmpty();
        if("run149".equals(family))return !s.optString("run149_pending_spell","").isEmpty();
        return false;
    }
    public static boolean pendingCleanup(String family,JSONObject before,JSONObject after){
        if(!"run149".equals(family)||before==null||after==null)return false;
        return before.has("run149_pending_spell")&&!after.has("run149_pending_spell")&&after.optBoolean("run149_spell_cancelled",false);
    }
}
