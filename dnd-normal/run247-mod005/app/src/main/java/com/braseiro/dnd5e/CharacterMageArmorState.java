package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Explicit 2024 Mage Armor state. An incidental sentence in spells/history is
 * not proof of a cast, and read/save/reopen must NEVER spend spell slots.
 * AC is projected into sheet.ac, the numeric value consumed by combat/SQLite.
 */
public final class CharacterMageArmorState {
    private CharacterMageArmorState(){}
    private static final String KEY="mage_armor_effect";
    private static int modifier(int score){return Math.floorDiv(score-10,2);}
    private static boolean wearingArmor(JSONObject sheet){
        String equipped=sheet.optString("equipped_armor","").trim();
        return !equipped.isEmpty() && !equipped.equalsIgnoreCase("nenhuma") &&
            !equipped.equalsIgnoreCase("none");
    }
    public static int mageArmorAc(JSONObject sheet){
        return 13+modifier(sheet.optInt("dex",10));
    }
    private static boolean hasMageArmor(JSONObject sheet){
        String spells=sheet.optString("spells","").toLowerCase(Locale.ROOT);
        return spells.contains("armadura arcana")||spells.contains("mage armor");
    }

    /** Activate only on an explicit, confirmed successful cast. */
    public static JSONObject activate(JSONObject sheet,long worldMinute,boolean slotAlreadySpent){
        if(sheet==null)return failure("Ficha ausente");
        if(!Character2024SheetReconciler.applies(sheet))return failure("Perfil 2024 necessário");
        if(!hasMageArmor(sheet))return failure("Armadura Arcana não consta na ficha");
        if(wearingArmor(sheet))return failure("Armadura vestida impede Armadura Arcana");
        if(worldMinute<0)return failure("Minuto de mundo inválido");
        JSONObject current=sheet.optJSONObject(KEY);
        if(current!=null&&current.optBoolean("active",false))return failure("Efeito já ativo");
        JSONArray slots=sheet.optJSONArray("spell_slots_current");
        if(!slotAlreadySpent && (slots==null||slots.length()<1||slots.optInt(0,0)<1))
            return failure("Nenhum espaço de primeiro círculo disponível");
        try{
            // All validation before state mutation.
            int oldAc=sheet.optInt("ac",10);
            if(!slotAlreadySpent){
                JSONArray replacement=new JSONArray(slots.toString());
                replacement.put(0,slots.optInt(0)-1);
                sheet.put("spell_slots_current",replacement);
            }
            JSONObject effect=new JSONObject()
                .put("active",true).put("source","Armadura Arcana")
                .put("duration_minutes",480)
                .put("started_world_minute",worldMinute)
                .put("expires_world_minute",worldMinute+480)
                .put("slot_spent_by_activation",!slotAlreadySpent)
                .put("concentration",false)
                .put("ac_before_effect",oldAc)
                .put("projected_ac",Math.max(oldAc,mageArmorAc(sheet)));
            sheet.put(KEY,effect);
            sheet.put("ac",effect.getInt("projected_ac"));
            sheet.remove("mage_armor_confirmation_pending");
            return new JSONObject().put("ok",true).put("effective_ac",sheet.optInt("ac"))
                .put("slot_consumed",!slotAlreadySpent);
        }catch(Exception error){return failure("Falha ao ativar: "+error.getMessage());}
    }
    private static JSONObject failure(String reason){
        try{return new JSONObject().put("ok",false).put("reason",reason);}
        catch(Exception ignored){return new JSONObject();}
    }

    /** End early (dispel, don armor), or after its eight-hour world duration. */
    public static void end(JSONObject sheet){
        if(sheet==null)return;
        JSONObject effect=sheet.optJSONObject(KEY);
        if(effect==null||!effect.optBoolean("active",false))return;
        try{
            int projected=effect.optInt("projected_ac",-1);
            // Do not erase subsequent independent AC adjustments.
            if(sheet.optInt("ac",-1)==projected)
                sheet.put("ac",effect.optInt("ac_before_effect",sheet.optInt("ac",10)));
            effect.put("active",false);
            sheet.put(KEY,effect);
        }catch(Exception error){throw new IllegalStateException("Falha ao encerrar Armadura Arcana.",error);}
    }
    public static void advanceTo(JSONObject sheet,long worldMinute){
        JSONObject effect=sheet==null?null:sheet.optJSONObject(KEY);
        if(effect==null||!effect.optBoolean("active",false))return;
        if(wearingArmor(sheet)||worldMinute>=effect.optLong("expires_world_minute",Long.MAX_VALUE))
            end(sheet);
    }
    public static JSONObject reconcile(JSONObject sheet){
        if(sheet==null || !Character2024SheetReconciler.applies(sheet))return sheet;
        try{
            JSONObject effect=sheet.optJSONObject(KEY);
            if(effect!=null&&effect.optBoolean("active",false)){
                if(wearingArmor(sheet)){end(sheet);return sheet;}
                int original=effect.optInt("ac_before_effect",sheet.optInt("ac",10));
                int target=Math.max(original,mageArmorAc(sheet));
                effect.put("projected_ac",target);
                sheet.put("ac",target);
            }else if(effect==null &&
                sheet.optString("spells","").toLowerCase(Locale.ROOT).contains("armadura arcana ativa")){
                // Legacy free text is ambiguous: ask rather than invent a cast
                // or silently spend one of the player's slots.
                sheet.put("mage_armor_confirmation_pending",true);
            }
        }catch(Exception e){throw new IllegalStateException("Falha no estado de Armadura Arcana.",e);}
        return sheet;
    }
}
