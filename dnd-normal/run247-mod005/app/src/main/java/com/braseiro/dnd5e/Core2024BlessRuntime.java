package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** PHB 2024 Bless: persistent slot, targets, concentration and one-minute lifetime.
 * Caller supplies authoritative character, inventory and tactical tokens, then persists
 * the returned sheet and combatant concentration in one transaction. No dice are rolled here.
 * Source: PHB 2024 Bless (level 1, action, 30 ft, V/S/M, 1 minute concentration).
 */
public final class Core2024BlessRuntime {
    public static final String FIELD="core2024_bless";
    private Core2024BlessRuntime(){}
    public static final class Result {
        public final String error,replacedConcentration;
        public final JSONObject sheet;
        private Result(String error,JSONObject sheet,String replaced){
            this.error=error;this.sheet=sheet;this.replacedConcentration=replaced;
        }
        public boolean allowed(){return error.isEmpty();}
    }
    private static Result fail(String error){return new Result(error,null,"");}
    private static Result done(JSONObject sheet,String previous){return new Result("",sheet,previous);}
    private static boolean finite(double number){return !Double.isNaN(number)&&!Double.isInfinite(number);}
    private static boolean materialAvailable(JSONArray items){
        if(items==null)return false;
        for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);
            if(item==null||!item.optBoolean("accessible",false))continue;
            String name=Core2024SpellRules.norm(item.optString("name",""));
            if(!name.contains("holy symbol")&&!name.contains("simbolo sagrado"))continue;
            Object raw=item.opt("value_gp");
            if(raw instanceof Number&&finite(((Number)raw).doubleValue())&&
                ((Number)raw).doubleValue()>=5)return true;
        }
        return false;
    }
    /** request: caster_id, source_class, casting_ability, slot_level, target_ids,
     * voice_available, hand_available, line_of_effect_confirmed, inventory,
     * round, operation_token, existing_concentration. The caller must compute
     * voice/hand conditions and obtain current token positions from the DB. */
    public static Result cast(String profile,JSONObject sheet,JSONObject request,
                              List<Core2024TacticalGeometry.Token> tokens,int width,int height,
                              int characterLevel,List<Core2024CastingEligibility.ClassLevel> classes){
        if(!"2024".equals(profile))return fail("requires_2024_rules_profile");
        if(sheet==null||request==null)return fail("missing_cast_state");
        long casterId=request.optLong("caster_id",0);
        int slot=request.optInt("slot_level",0),round=request.optInt("round",0);
        String source=request.optString("source_class",""),ability=request.optString("casting_ability","");
        String token=request.optString("operation_token","");
        JSONArray chosen=request.optJSONArray("target_ids");
        if(casterId<=0||round<1||round>1000000||
            !CombatActionReceiptGuard.validToken(token))return fail("invalid_cast_identity");
        if(token.equals(sheet.optString("core2024_bless_last_token","")))return fail("replayed_cast");
        if(slot<1||slot>9)return fail("invalid_spell_slot_level");
        Core2024CastingEligibility.Result eligibility=Core2024CastingEligibility.check(
            "bless",characterLevel,classes,ability);
        if(!eligibility.allowed()||!source.equals(eligibility.sourceClass))
            return fail("bless_class_or_ability_unavailable");
        if(!Core2024PreparedSpellLedger.isPrepared(sheet,source,"bless"))
            return fail("bless_not_prepared");
        if(!request.optBoolean("voice_available",false))return fail("verbal_component_unavailable");
        if(!request.optBoolean("hand_available",false))return fail("somatic_component_unavailable");
        if(!materialAvailable(request.optJSONArray("inventory")))return fail("holy_symbol_5gp_missing");
        if(!request.optBoolean("line_of_effect_confirmed",false))return fail("line_of_effect_unconfirmed");
        if(chosen==null||chosen.length()<1||chosen.length()>slot+2)
            return fail("invalid_bless_target_count");
        if(width<1||height<1||width>100||height>100||tokens==null||tokens.isEmpty()||tokens.size()>256)
            return fail("invalid_tactical_scene");
        Map<Long,Core2024TacticalGeometry.Token> byId=new HashMap<Long,Core2024TacticalGeometry.Token>();
        for(Core2024TacticalGeometry.Token t:tokens){
            if(t==null||t.id<=0||!finite(t.x)||!finite(t.y)||t.x<0||t.x>=1||t.y<0||t.y>=1||
                byId.put(t.id,t)!=null)return fail("invalid_tactical_positions");
        }
        Core2024TacticalGeometry.Token caster=byId.get(casterId);
        if(caster==null)return fail("caster_position_missing");
        Set<Long> unique=new HashSet<Long>();
        JSONArray targets=new JSONArray();
        for(int i=0;i<chosen.length();i++){
            Object raw=chosen.opt(i);
            if(!(raw instanceof Number)||((Number)raw).doubleValue()!=((Number)raw).longValue())
                return fail("invalid_bless_target");
            long id=((Number)raw).longValue();
            Core2024TacticalGeometry.Token target=byId.get(id);
            if(id<=0||target==null||!unique.add(id))return fail("invalid_bless_target");
            double distance=Math.hypot((target.x-caster.x)*width,(target.y-caster.y)*height)*5;
            if(distance>30.000001)return fail("bless_target_out_of_range");
            targets.put(id);
        }
        CharacterSpellCommitGuard.Core2024SlotSpend spend=
            CharacterSpellCommitGuard.spendCore2024Slot(profile,sheet,slot);
        if(!spend.allowed())return fail(spend.error);
        try{
            JSONObject active=new JSONObject().put("spell","bless").put("caster_id",casterId)
                .put("cast_round",round).put("duration_rounds",10).put("slot_level",slot)
                .put("cast_id",token).put("targets",targets).put("concentration",true);
            JSONObject after=spend.sheet;
            after.put(FIELD,active).put("core2024_bless_last_token",token);
            String previous=request.optString("existing_concentration","");
            return done(after,previous);
        }catch(Exception error){return fail("bless_state_write_failed");}
    }
    /** Caller invokes this on a caster turn, or after loading a delayed world state. */
    public static Result advanceCasterRound(JSONObject sheet,int round){
        if(sheet==null||round<1)return fail("invalid_round");
        try{
            JSONObject after=new JSONObject(sheet.toString()),active=after.optJSONObject(FIELD);
            if(active==null)return done(after,"");
            int castRound=active.optInt("cast_round",0);
            if(castRound<1||round<castRound)return fail("invalid_bless_timeline");
            if(SpellConcentrationDurationRules.expired("2024",castRound,10,round))after.remove(FIELD);
            return done(after,"");
        }catch(Exception error){return fail("bless_state_copy_failed");}
    }
    public static Result endConcentration(JSONObject sheet){
        if(sheet==null)return fail("missing_cast_state");
        try{JSONObject after=new JSONObject(sheet.toString());after.remove(FIELD);return done(after,"");}
        catch(Exception error){return fail("bless_state_copy_failed");}
    }
    /** No independent bonus can be claimed after expiry, even if the tick was missed. */
    public static int bonusFor(JSONObject sheet,long targetId,int round,String rollKind,int d4){
        if(!"attack".equals(rollKind)&&!"save".equals(rollKind))
            throw new IllegalArgumentException("bless_only_attack_or_save");
        if(d4<1||d4>4)throw new IllegalArgumentException("invalid_bless_d4");
        JSONObject active=sheet==null?null:sheet.optJSONObject(FIELD);
        if(active==null||targetId<=0||round<active.optInt("cast_round",Integer.MAX_VALUE))return 0;
        if(SpellConcentrationDurationRules.expired("2024",active.optInt("cast_round",0),10,round))return 0;
        JSONArray targets=active.optJSONArray("targets");
        if(targets!=null)for(int i=0;i<targets.length();i++){
            Object id=targets.opt(i);
            if(id instanceof Number&&((Number)id).longValue()==targetId)return d4;
        }
        return 0;
    }
    /** Shared 2024 slot restoration plus effect cleanup; caller also clears combatant.concentration. */
    public static Result longRest(String profile,JSONObject sheet){
        CharacterSpellCommitGuard.Core2024SlotSpend restored=
            CharacterSpellCommitGuard.restoreCore2024Slots(profile,sheet);
        if(!restored.allowed())return fail(restored.error);
        restored.sheet.remove(FIELD);
        return done(restored.sheet,"");
    }
}
