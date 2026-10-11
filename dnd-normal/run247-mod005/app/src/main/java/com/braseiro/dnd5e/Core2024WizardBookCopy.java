package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** PHB 2024 p.149: copying a newly found Wizard spell: 50 gp and 2 in-world
 * hours per spell level. The game's clock, not a user checkbox, unlocks the
 * pending project. Call start/finish only inside AppDb.characterAtomic.
 * No game state changes for quotes and failed attempts. */
public final class Core2024WizardBookCopy {
    private Core2024WizardBookCopy(){}
    public static final String PENDING="wizard_copy_project";
    public static final String RECEIPTS="wizard_copy_receipts";
    public static final int MAX_RECEIPTS=64;
    private static JSONObject failed(String reason){
        JSONObject r=new JSONObject();try{r.put("wizard_copy_error",reason);}catch(Exception ignored){}return r;
    }
    private static JSONObject wizard(JSONObject sheet){
        JSONObject l=sheet==null?null:sheet.optJSONObject(Core2024PreparedSpellLedger.FIELD);
        JSONObject cs=l==null?null:l.optJSONObject("classes");
        return cs==null?null:cs.optJSONObject("wizard");
    }
    private static boolean contains(JSONArray a,String spell){
        if(a!=null)for(int i=0;i<a.length();i++)if(spell.equals(a.optString(i)))return true;
        return false;
    }
    private static boolean validToken(String token){return token!=null&&token.length()>=8&&token.length()<=90&&
        token.matches("[A-Za-z0-9_.:-]+");}
    public static JSONObject quote(JSONObject sheet,int wizardLevel,String spellName,double hour){
        Core2024SpellRules.Spell spell=Core2024SpellRules.find(spellName);
        JSONObject entry=wizard(sheet);
        if(entry==null||!entry.optBoolean("configured",false))return failed("wizard_spellbook_not_configured");
        if(spell==null||!"wizard".equals(Core2024CastingEligibility.baseClass("wizard"))||
            !Core2024CastingEligibility.listed(spell.key,"wizard")||spell.level<1)
            return failed("spell_not_eligible_for_wizard_copy");
        if(wizardLevel<1||wizardLevel>20||spell.level>Core2024PreparedSpellRules.maxCircle("wizard",wizardLevel))
            return failed("wizard_cannot_prepare_this_spell_level");
        if(contains(entry.optJSONArray("spellbook"),spell.key))return failed("spell_already_in_book");
        if(entry.optJSONObject(PENDING)!=null)return failed("wizard_copy_already_in_progress");
        if(!Double.isFinite(hour)||hour<0||hour>1e9d)return failed("invalid_world_clock");
        long cost=50L*spell.level; double hours=2d*spell.level;
        if(!sheet.has("gold")||sheet.optInt("gold",-1)<cost)return failed("insufficient_recorded_gold");
        try{return new JSONObject().put("wizard_copy_eligible",true).put("spell",spell.key)
            .put("spell_level",spell.level).put("cost_gp",cost).put("hours_required",hours)
            .put("started_at_hour",hour).put("ready_at_hour",hour+hours)
            .put("revision",entry.optInt("revision",0));}
        catch(Exception e){return failed("quote_failed");}
    }
    public static JSONObject start(JSONObject sheet,int wizardLevel,String spellName,double hour,
        String token,boolean sourceConfirmed,int expectedRevision){
        if(!validToken(token))return failed("invalid_operation_token");
        if(!sourceConfirmed)return failed("found_spell_source_not_confirmed");
        JSONObject preview=quote(sheet,wizardLevel,spellName,hour);
        if(!preview.optBoolean("wizard_copy_eligible",false))return preview;
        JSONObject entry=wizard(sheet);
        if(entry.optInt("revision",0)!=expectedRevision)return failed("stale_spellbook_revision");
        try{
            JSONObject project=new JSONObject().put("token",token).put("spell",preview.optString("spell"))
                .put("cost_gp",preview.optInt("cost_gp")).put("started_at_hour",hour)
                .put("ready_at_hour",preview.optDouble("ready_at_hour"))
                .put("wizard_level",wizardLevel).put("entry_revision",expectedRevision+1)
                .put("book_snapshot",entry.optJSONArray("spellbook")==null?"[]":entry.optJSONArray("spellbook").toString())
                .put("hours_required",2d*preview.optInt("spell_level"))
                .put("studied_hours",0d).put("last_study_hour",hour)
                .put("study_receipts",new JSONArray());
            entry.put(PENDING,project).put("revision",expectedRevision+1);
            return new JSONObject().put("wizard_copy_started",true).put("project",new JSONObject(project.toString()));
        }catch(Exception e){return failed("start_failed");}
    }
    public static JSONObject pending(JSONObject sheet,double hour){
        JSONObject e=wizard(sheet),p=e==null?null:e.optJSONObject(PENDING);
        if(p==null)return failed("wizard_copy_not_started");
        if(!Double.isFinite(hour)||hour<0)return failed("invalid_world_clock");
        try{return new JSONObject(p.toString()).put("wizard_copy_pending",true)
            .put("hours_remaining",Math.max(0d,p.optDouble("hours_required",Double.MAX_VALUE)-
                p.optDouble("studied_hours",0d)))
            .put("elapsed_hours",Math.max(0d,hour-p.optDouble("started_at_hour",hour)));}
        catch(Exception x){return failed("pending_failed");}
    }
    /** Only explicitly recorded, dedicated study counts. Passive clock movement
     * does not copy the spell. Human confirmation is a necessary precondition;
     * a unique receipt prevents accidental double allocation of hours. */
    public static JSONObject recordStudy(JSONObject sheet,double hour,double studiedHours,
        String projectToken,String studyToken,boolean dedicatedConfirmed){
        if(!validToken(studyToken)||!validToken(projectToken))return failed("invalid_study_token");
        if(!dedicatedConfirmed)return failed("dedicated_study_not_confirmed");
        JSONObject e=wizard(sheet),p=e==null?null:e.optJSONObject(PENDING);
        if(p==null)return failed("wizard_copy_not_started");
        if(!projectToken.equals(p.optString("token")))return failed("wrong_copy_operation_token");
        JSONArray receipts=p.optJSONArray("study_receipts");
        if(receipts!=null)for(int i=0;i<receipts.length();i++){
            if(studyToken.equals(receipts.optString(i)))return failed("study_operation_already_recorded");
        }
        double last=p.optDouble("last_study_hour",Double.NaN);
        double accumulated=p.optDouble("studied_hours",0d);
        double need=p.optDouble("hours_required",Double.NaN);
        if(!Double.isFinite(hour)||!Double.isFinite(last)||!Double.isFinite(studiedHours)||
            !Double.isFinite(need)||studiedHours<=0d||hour+1e-6d<last||hour>1e9d)
            return failed("invalid_world_time_or_study_hours");
        if(studiedHours>hour-last+1e-6d)return failed("more_study_than_elapsed_world_time");
        if(accumulated+studiedHours>need+1e-6d)return failed("study_hours_exceed_copy_requirement");
        try{
            JSONArray next=receipts==null?new JSONArray():new JSONArray(receipts.toString());
            next.put(studyToken);
            // This world-clock interval is consumed even if only part was spent
            // studying. It cannot be used again for the same project.
            p.put("studied_hours",accumulated+studiedHours)
                .put("last_study_hour",hour).put("study_receipts",next);
            return new JSONObject().put("wizard_copy_study_recorded",true)
                .put("studied_hours",accumulated+studiedHours)
                .put("hours_remaining",Math.max(0d,need-accumulated-studiedHours));
        }catch(Exception problem){return failed("study_record_failed");}
    }
    public static JSONObject cancel(JSONObject sheet,String projectToken){
        JSONObject e=wizard(sheet),p=e==null?null:e.optJSONObject(PENDING);
        if(p==null)return failed("wizard_copy_not_started");
        if(!validToken(projectToken)||!projectToken.equals(p.optString("token")))
            return failed("wrong_copy_operation_token");
        try{
            e.remove(PENDING);e.put("revision",e.optInt("revision",0)+1);
            return new JSONObject().put("wizard_copy_cancelled",true);
        }catch(Exception problem){return failed("copy_cancel_failed");}
    }
    public static JSONObject finish(JSONObject sheet,int wizardLevel,double hour,String token){
        if(!validToken(token))return failed("invalid_operation_token");
        JSONObject e=wizard(sheet),p=e==null?null:e.optJSONObject(PENDING);
        JSONArray receipts=sheet==null?null:sheet.optJSONArray(RECEIPTS);
        if(receipts!=null)for(int i=0;i<receipts.length();i++){
            JSONObject r=receipts.optJSONObject(i);
            if(r!=null&&token.equals(r.optString("token"))){
                return failed("copy_operation_already_committed"); // duplicate is not a second spend
            }
        }
        if(p==null)return failed("wizard_copy_not_started");
        if(!token.equals(p.optString("token")))return failed("wrong_copy_operation_token");
        if(!Double.isFinite(hour)||hour<0||
            p.optDouble("studied_hours",0d)+1e-6d<p.optDouble("hours_required",Double.MAX_VALUE))
            return failed("dedicated_study_time_incomplete");
        // Preparation/rest windows can legitimately change the class revision
        // while the Wizard studies. The acquired book, not preparation,
        // is the state that must remain identical to the start snapshot.
        JSONArray currentBook=e.optJSONArray("spellbook");
        String currentBookStamp=currentBook==null?"[]":currentBook.toString();
        if(!currentBookStamp.equals(p.optString("book_snapshot","missing")))
            return failed("spellbook_changed_during_copy");
        Core2024SpellRules.Spell spell=Core2024SpellRules.find(p.optString("spell"));
        if(spell==null||!Core2024CastingEligibility.listed(spell.key,"wizard")||
            spell.level>Core2024PreparedSpellRules.maxCircle("wizard",wizardLevel))
            return failed("spell_not_eligible_anymore");
        JSONArray book=e.optJSONArray("spellbook");
        if(contains(book,spell.key))return failed("spell_already_in_book");
        int gold=sheet.optInt("gold",-1),cost=p.optInt("cost_gp",-1);
        if(cost!=spell.level*50||gold<cost)return failed("gold_or_cost_changed");
        try{
            JSONArray nextBook=book==null?new JSONArray():new JSONArray(book.toString());
            nextBook.put(spell.key);
            JSONArray nextReceipts=receipts==null?new JSONArray():new JSONArray(receipts.toString());
            nextReceipts.put(new JSONObject().put("token",token).put("spell",spell.key)
                .put("cost_gp",cost).put("completed_at_hour",hour));
            while(nextReceipts.length()>MAX_RECEIPTS){JSONArray reduced=new JSONArray();
                for(int i=1;i<nextReceipts.length();i++)reduced.put(nextReceipts.opt(i));nextReceipts=reduced;}
            // All sheet mutations occur after complete preflight and JSON allocations.
            sheet.put("gold",gold-cost).put(RECEIPTS,nextReceipts);
            e.put("spellbook",nextBook).put("revision",e.optInt("revision",0)+1);
            e.remove(PENDING);
            return new JSONObject().put("wizard_copy_committed",true).put("spell",spell.key)
                .put("gold_remaining",gold-cost).put("gold_spent",cost)
                .put("world_hours_spent",2*spell.level);
        }catch(Exception err){return failed("copy_finish_failed");}
    }
}
