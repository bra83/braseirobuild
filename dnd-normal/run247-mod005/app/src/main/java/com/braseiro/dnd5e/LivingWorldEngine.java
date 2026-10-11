package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Simulação de campanha aberta para um único jogador controlar vários heróis.
 * O calendário é canônico; relações, famílias, ocupações e trupe persistem.
 * Nenhum resultado mecânico é aplicado por esta classe sem regra auditável.
 */
public final class LivingWorldEngine {
    public static final int DEFAULT_DAYS_PER_YEAR=365;
    private final AppDb db;

    public LivingWorldEngine(AppDb database){db=database;}

    public JSONObject state(){
        ensureTables();
        JSONObject out=queryOne("SELECT * FROM living_world_state WHERE world_profile=?",new String[]{profile()});
        if(out.length()==0){
            ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("enabled",1);v.put("simulation_mode","equilibrado");v.put("calendar_name","Calendário da campanha");v.put("epoch_year",1);v.put("days_per_year",DEFAULT_DAYS_PER_YEAR);v.put("months_json","[]");v.put("world_notes","");v.put("last_simulated_day",currentDay());v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().insertWithOnConflict("living_world_state",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            out=queryOne("SELECT * FROM living_world_state WHERE world_profile=?",new String[]{profile()});
        }
        return out;
    }

    public void saveSettings(boolean enabled,String mode,String calendarName,int epochYear,int daysPerYear,String notes){
        ensureTables();ContentValues v=new ContentValues();v.put("enabled",enabled?1:0);v.put("simulation_mode",safeMode(mode));v.put("calendar_name",clean(calendarName,"Calendário da campanha"));v.put("epoch_year",Math.max(1,epochYear));v.put("days_per_year",Math.max(1,Math.min(1000,daysPerYear)));v.put("world_notes",notes==null?"":notes.trim());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("living_world_state",v,"world_profile=?",new String[]{profile()});
    }


    public void saveBackgroundSettings(boolean enabled,double gameHoursPerRealDay,int maxCatchUpHours){
        ensureTables();ContentValues v=new ContentValues();v.put("background_simulation_enabled",enabled?1:0);v.put("background_game_hours_per_real_day",Math.max(1d,Math.min(720d,gameHoursPerRealDay)));v.put("background_max_hours",Math.max(1,Math.min(8760,maxCatchUpHours)));v.put("last_background_sync_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("living_world_state",v,"world_profile=?",new String[]{profile()});
    }

    public void markBackgroundSyncNow(){ensureTables();ContentValues v=new ContentValues();v.put("last_background_sync_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("living_world_state",v,"world_profile=?",new String[]{profile()});}

    public JSONObject heroLife(long characterId){
        ensureTables();JSONObject row=queryOne("SELECT * FROM hero_life_profiles WHERE world_profile=? AND character_id=?",new String[]{profile(),String.valueOf(characterId)});
        if(row.length()==0){
            int age=18;ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("character_id",characterId);v.put("birth_day",LivingWorldRulePolicy.birthDayForAge(age,currentDay(),daysPerYear()));v.put("initial_age_years",0);v.put("life_stage","não definido");v.put("residence","");v.put("occupation","Aventureiro");v.put("family_name","");v.put("relationship_status","");v.put("household_json","[]");v.put("goals_json","[]");v.put("status","active");v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().insertWithOnConflict("hero_life_profiles",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            row=queryOne("SELECT * FROM hero_life_profiles WHERE world_profile=? AND character_id=?",new String[]{profile(),String.valueOf(characterId)});
        }
        try{row.put("age_years",ageFor(row));row.put("age_is_narrative_only",true);}catch(Exception ignored){AppLog.ignored("LivingWorldEngine","Não foi possível calcular a idade.",ignored);}
        return row;
    }

    public void saveHeroLife(long characterId,int currentAge,String residence,String occupation,String familyName,String relationshipStatus,String goals){
        heroLife(characterId);ContentValues v=new ContentValues();v.put("birth_day",LivingWorldRulePolicy.birthDayForAge(Math.max(0,currentAge),currentDay(),daysPerYear()));v.put("initial_age_years",0);v.put("life_stage","não definido");v.put("residence",text(residence));v.put("occupation",text(occupation));v.put("family_name",text(familyName));v.put("relationship_status",text(relationshipStatus));v.put("goals_json",new JSONArray().put(text(goals)).toString());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("hero_life_profiles",v,"world_profile=? AND character_id=?",new String[]{profile(),String.valueOf(characterId)});
    }

    public long addRelationship(long characterId,String relatedName,String type,String status,int currentAge,String notes){
        if(relatedName==null||relatedName.trim().length()==0)return -1;ensureTables();ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("character_id",characterId);v.put("related_character_id",0);v.put("related_name",relatedName.trim());v.put("relation_type",clean(type,"vínculo"));v.put("status",clean(status,"ativo"));v.put("established_day",currentDay());boolean known=currentAge>=0;v.put("related_birth_day",known?LivingWorldRulePolicy.birthDayForAge(currentAge,currentDay(),daysPerYear()):0);v.put("related_initial_age_years",0);v.put("age_known",known?1:0);v.put("notes",text(notes));v.put("mechanical_effects_json","{}");v.put("created_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());return db.getWritableDatabase().insert("life_relationships",null,v);
    }

    public List<JSONObject> relationships(long characterId){ensureTables();List<JSONObject> rows=queryList("SELECT * FROM life_relationships WHERE world_profile=? AND character_id=? ORDER BY updated_at DESC,id DESC",new String[]{profile(),String.valueOf(characterId)},100);for(JSONObject row:rows)if(row.optInt("age_known",0)==1)try{row.put("age_years",LivingWorldRulePolicy.ageYears(row.optInt("related_initial_age_years",0),row.optInt("related_birth_day"),currentDay(),daysPerYear()));}catch(Exception ignored){AppLog.ignored("LivingWorldEngine","Não foi possível calcular a idade do vínculo.",ignored);}return rows;}

    public long planActivity(long characterId,String type,String title,String location,int durationDays,String notes){
        if(title==null||title.trim().length()==0)return -1;ensureTables();int duration=Math.max(1,Math.min(36500,durationDays));ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("character_id",characterId);v.put("activity_type",LivingWorldRulePolicy.normalizedActivityType(type));v.put("title",title.trim());v.put("location",text(location));v.put("start_day",currentDay());v.put("duration_days",duration);v.put("status","planejada");v.put("rule_authorized",0);v.put("rule_source","");v.put("rule_page",0);v.put("rule_excerpt","");v.put("check_json","{}");v.put("cost_json","{}");v.put("reward_json","{}");String normalizedType=LivingWorldRulePolicy.normalizedActivityType(type);String family=DowntimeEconomyRulesEngine.familyForActivity(normalizedType);JSONObject procedure=DowntimeEconomyRulesEngine.procedureFor(normalizedType);try{procedure.put("activity_family",family).put("world_time_persists",true);}catch(Exception ignored){}v.put("procedure_json",procedure.toString());v.put("progress_units",0d);v.put("total_units",duration);v.put("check_frequency_days",1);v.put("next_check_day",currentDay());v.put("complications_json","[]");v.put("narrative_result",text(notes));v.put("created_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());return db.getWritableDatabase().insert("life_activities",null,v);
    }

    /** Autoriza uma atividade persistente a usar somente uma regra pré-compilada e registrada. */
    public JSONObject authorizeCompiledSupplementActivity(long activityId,String ruleId){
        ensureTables();JSONObject result=new JSONObject();try{
            JSONObject activity=activity(activityId),rule=SupplementContentRegistry.rule(ruleId);if(activity.length()==0)return result.put("allowed",false).put("reason","Atividade inexistente.");if(rule.length()==0)return result.put("allowed",false).put("reason","Regra compilada inexistente.");
            String type=activity.optString("activity_type","");boolean familyOk=(ruleId.startsWith("animal.")&&"manejo_animal".equals(type))||(ruleId.startsWith("cozinheiro.")&&"culinaria".equals(type));if(!familyOk)return result.put("allowed",false).put("reason","A regra compilada não corresponde ao tipo de atividade.");
            ContentValues v=new ContentValues();v.put("rule_authorized",1);v.put("rule_source",rule.optString("book"));v.put("rule_page",rule.optInt("page"));v.put("rule_excerpt",rule.optString("evidence"));v.put("procedure_json",SupplementContentRegistry.mechanicalBasis(ruleId).toString());v.put("status","em_andamento");v.put("updated_at",System.currentTimeMillis());
            int changed=db.getWritableDatabase().update("life_activities",v,"id=? AND world_profile=?",new String[]{String.valueOf(activityId),profile()});return result.put("allowed",changed==1).put("updated",changed==1).put("rule",rule);
        }catch(Exception failure){try{return result.put("allowed",false).put("updated",false).put("reason",failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage());}catch(Exception ignored){return new JSONObject();}}
    }

    public List<JSONObject> activities(int limit){ensureTables();return queryList("SELECT a.*,c.name AS character_name FROM life_activities a LEFT JOIN characters c ON c.id=a.character_id AND c.world_profile=a.world_profile WHERE a.world_profile=? ORDER BY a.updated_at DESC,a.id DESC",new String[]{profile()},Math.max(1,Math.min(200,limit)));}
    public List<JSONObject> activitiesForSimulation(int limit){ensureTables();return queryList("SELECT a.*,c.name AS character_name FROM life_activities a LEFT JOIN characters c ON c.id=a.character_id AND c.world_profile=a.world_profile WHERE a.world_profile=? AND a.status IN ('planejada','em_andamento','aguardando_rolagem') ORDER BY a.updated_at ASC,a.start_day ASC,a.id ASC",new String[]{profile()},Math.max(1,Math.min(1000,limit)));}
    public JSONObject activity(long activityId){ensureTables();return queryOne("SELECT a.*,c.name AS character_name FROM life_activities a LEFT JOIN characters c ON c.id=a.character_id AND c.world_profile=a.world_profile WHERE a.id=? AND a.world_profile=?",new String[]{String.valueOf(activityId),profile()});}

    public JSONObject authorizeMechanicalOutcome(long activityId,String source,int page,String excerpt,JSONObject check,JSONObject cost,JSONObject reward){
        ensureTables();JSONObject result=new JSONObject();
        try{
            JSONObject activity=activity(activityId);if(activity.length()==0)return result.put("allowed",false).put("updated",false).put("reason","Atividade inexistente.");
            JSONObject safeCost=LivingWorldRulePolicy.sanitizeMechanicalOutcome(profile(),true,source,page,excerpt,cost);
            JSONObject safeReward=LivingWorldRulePolicy.sanitizeMechanicalOutcome(profile(),true,source,page,excerpt,reward);
            JSONObject normalizedCheck=normalizeActivityCheck(check);
            boolean sourceValid=LivingWorldRulePolicy.allowsMechanicalOutcome(profile(),true,source,page,excerpt)&&db.verifyEnabledManualExcerpt(source,page,excerpt);
            boolean outcomeBound=mechanicalOutcomeMatchesExcerpt(excerpt,safeCost,safeReward);
            boolean mechanics=LivingWorldRulePolicy.isMechanicalJson(safeCost)||LivingWorldRulePolicy.isMechanicalJson(safeReward)||normalizedCheck.optBoolean("required",false);
            JSONObject compiled=RuleProcedureCompiler.compile(new JSONObject().put("rule_id","activity:"+activityId+":manual:"+stableId(source+"|"+page+"|"+excerpt)).put("book",source).put("page",page).put("excerpt",excerpt).put("system",profile()),new JSONArray().put("economia_tempo_livre"));
            boolean procedureReady=!mechanics||compiled.optBoolean("executable",false);boolean allowed=sourceValid&&outcomeBound&&procedureReady;
            result.put("allowed",allowed).put("world_profile",profile()).put("source",source==null?"":source).put("page",page).put("outcome_bound_to_excerpt",outcomeBound).put("procedure_compiled",compiled).put("procedure_ready",procedureReady);
            if(!allowed){result.put("reason",!sourceValid?"Fonte habilitada, página ou trecho incompatível com o mundo ativo.":!outcomeBound?"O custo ou recompensa não está demonstrado pelo trecho citado.":"O trecho não forma um procedimento atômico executável para a atividade.");return result;}
            SQLiteDatabase sql=db.getWritableDatabase();sql.beginTransaction();
            try{
                if(normalizedCheck.optBoolean("required",false)){
                    JSONObject binding=activityRollBinding(activity,source,page,excerpt,normalizedCheck);
                    db.bindDiceHistoryChecked(normalizedCheck.optLong("roll_id",0),binding);
                    for(String key:new String[]{"test_key","action_signature","scene_fingerprint","source_rule_id","procedure_id","campaign_fingerprint","target_signature","consequence_key","modifier_source","roll_mode","actor_name","outcome"})normalizedCheck.put(key,binding.optString(key));
                    normalizedCheck.put("actor_character_id",binding.optLong("actor_character_id",0)).put("modifier",binding.optInt("modifier"))
                            .put("natural_roll",binding.optInt("natural_roll")).put("critical",binding.optBoolean("critical")).put("critical_failure",binding.optBoolean("critical_failure"))
                            .put("verified",activityCheckSatisfied(normalizedCheck));
                    if(!normalizedCheck.optBoolean("verified",false))throw new IllegalStateException("A rolagem não pôde ser vinculada exatamente à atividade.");
                }
                ContentValues v=new ContentValues();v.put("rule_authorized",1);v.put("rule_source",source.trim());v.put("rule_page",page);v.put("rule_excerpt",excerpt.trim());v.put("procedure_json",compiled.toString());v.put("check_json",normalizedCheck.toString());v.put("cost_json",safeCost.toString());v.put("reward_json",safeReward.toString());v.put("status","em_andamento");v.put("updated_at",System.currentTimeMillis());
                int changed=sql.update("life_activities",v,"id=? AND world_profile=?",new String[]{String.valueOf(activityId),profile()});if(changed!=1)throw new IllegalStateException("A atividade foi alterada durante a autorização.");
                sql.setTransactionSuccessful();result.put("updated",true).put("check_required",normalizedCheck.optBoolean("required",false));
            }finally{sql.endTransaction();}
            return result;
        }catch(Exception failure){try{return result.put("allowed",false).put("updated",false).put("reason",failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage());}catch(Exception ignored){return new JSONObject();}}
    }

    public JSONObject completeNarrativeActivity(long activityId,String narrative){
        ensureTables();JSONObject before=queryOne("SELECT * FROM life_activities WHERE id=? AND world_profile=?",new String[]{String.valueOf(activityId),profile()});JSONObject out=new JSONObject();
        try{
            if(before.length()==0)return out.put("completed",false).put("reason","Atividade inexistente.");
            JSONObject cost=parseObject(before.optString("cost_json","{}")),reward=parseObject(before.optString("reward_json","{}")),check=parseObject(before.optString("check_json","{}"));
            boolean hasMechanics=LivingWorldRulePolicy.isMechanicalJson(cost)||LivingWorldRulePolicy.isMechanicalJson(reward);
            boolean allowed=!hasMechanics||(LivingWorldRulePolicy.allowsMechanicalOutcome(profile(),before.optInt("rule_authorized",0)==1,before.optString("rule_source"),before.optInt("rule_page"),before.optString("rule_excerpt"))&&db.verifyEnabledManualExcerpt(before.optString("rule_source"),before.optInt("rule_page"),before.optString("rule_excerpt"))&&mechanicalOutcomeMatchesExcerpt(before.optString("rule_excerpt"),cost,reward));
            if(!allowed)return out.put("completed",false).put("reason","Resultado mecânico bloqueado: falta regra válida e vinculada ao resultado.");
            if(hasMechanics&&!activityCheckSatisfied(check))return out.put("completed",false).put("awaiting_roll",true).put("reason","A atividade exige uma rolagem local vinculada antes da aplicação.");
            ContentValues v=new ContentValues();v.put("status",hasMechanics?"aguardando_aplicacao":"concluida");v.put("narrative_result",text(narrative));v.put("updated_at",System.currentTimeMillis());
            int changed=db.getWritableDatabase().update("life_activities",v,"id=? AND world_profile=?",new String[]{String.valueOf(activityId),profile()});if(changed!=1)throw new IllegalStateException("Atividade mudou durante a conclusão.");
            db.addAuditEvent("mundo_vivo","Atividade "+(hasMechanics?"validada":"concluída"),before.optString("title"),before.toString(),queryOne("SELECT * FROM life_activities WHERE id=?",new String[]{String.valueOf(activityId)}).toString(),true);
            return out.put("completed",!hasMechanics).put("awaiting_application",hasMechanics).put("mechanics_applied",false);
        }catch(Exception e){try{return out.put("completed",false).put("reason",e.getMessage());}catch(Exception ignored){return new JSONObject();}}
    }

    /** Aplica uma atividade já documentada e concluída. A operação é idempotente
     * e altera apenas os campos aceitos pela política local do Mundo Vivo. */
    public synchronized JSONObject applyAuthorizedActivityOutcome(long activityId){
        ensureTables();JSONObject out=new JSONObject();SQLiteDatabase sql=db.getWritableDatabase();
        JSONObject activity=queryOne("SELECT * FROM life_activities WHERE id=? AND world_profile=?",new String[]{String.valueOf(activityId),profile()});
        try{
            if(activity.length()==0)return out.put("applied",false).put("reason","Atividade inexistente.");
            if(activity.optLong("applied_at",0)>0||"concluida".equals(activity.optString("status")))return out.put("applied",true).put("idempotent",true).put("resolution",parseObject(activity.optString("resolution_json","{}")));
            if(!"aguardando_aplicacao".equals(activity.optString("status")))return out.put("applied",false).put("reason","A atividade ainda não está pronta para aplicação.");
            boolean authorized=LivingWorldRulePolicy.allowsMechanicalOutcome(profile(),activity.optInt("rule_authorized",0)==1,activity.optString("rule_source"),activity.optInt("rule_page"),activity.optString("rule_excerpt"))&&db.verifyEnabledManualExcerpt(activity.optString("rule_source"),activity.optInt("rule_page"),activity.optString("rule_excerpt"));
            if(!authorized)return out.put("applied",false).put("reason","A fonte documentada não está habilitada para este mundo.");
            JSONObject cost=LivingWorldRulePolicy.sanitizeMechanicalOutcome(profile(),true,activity.optString("rule_source"),activity.optInt("rule_page"),activity.optString("rule_excerpt"),parseObject(activity.optString("cost_json","{}")));
            JSONObject reward=LivingWorldRulePolicy.sanitizeMechanicalOutcome(profile(),true,activity.optString("rule_source"),activity.optInt("rule_page"),activity.optString("rule_excerpt"),parseObject(activity.optString("reward_json","{}")));
            if(!mechanicalOutcomeMatchesExcerpt(activity.optString("rule_excerpt"),cost,reward))return out.put("applied",false).put("reason","O efeito proposto não está demonstrado pelo trecho citado.");
            JSONObject activityCheck=parseObject(activity.optString("check_json","{}"));
            if(!activityCheckSatisfied(activityCheck))return out.put("applied",false).put("reason","A rolagem local exigida ainda não foi vinculada ou verificada.");
            long characterId=activity.optLong("character_id",0);JSONObject hero=db.getCharacter(characterId);if(hero.length()==0)return out.put("applied",false).put("reason","Herói da atividade não foi encontrado.");
            JSONObject before=new JSONObject().put("hero",hero).put("party",db.getPartyState());
            sql.beginTransaction();
            try{
                JSONObject sheet=parseObject(hero.optString("sheet_json","{}"));JSONObject resources=sheet.optJSONObject("resources");if(resources==null)resources=new JSONObject();
                int xp=Math.max(0,sheet.optInt("xp",resources.optInt("xp",0))+sumInt(cost,reward,"xp_delta"));
                int gold=Math.max(0,sheet.optInt("gold",resources.optInt("gold",0))+sumInt(cost,reward,"gold_delta"));
                sheet.put("xp",xp).put("gold",gold);resources.put("xp",xp).put("gold",gold);
                applyGenericResource(resources,cost);applyGenericResource(resources,reward);sheet.put("resources",resources);
                String equipment=hero.optString("equipment");equipment=applyItemChange(equipment,cost);equipment=applyItemChange(equipment,reward);
                db.setCharacterEquipmentAndNotes(characterId,equipment,hero.optString("notes"));db.updateCharacterSheet(characterId,sheet.toString());
                JSONObject party=db.getPartyState();JSONObject company=parseObject(party.optString("company_resources_json","{}"));
                company.put("provisions_days",Math.max(0,company.optInt("provisions_days",0)+sumInt(cost,reward,"provisions_delta")));
                company.put("water_days",Math.max(0,company.optInt("water_days",0)+sumInt(cost,reward,"water_delta")));
                party.put("company_resources_json",company.toString());db.savePartyState(party);
                JSONObject resolution=new JSONObject().put("character_id",characterId).put("character",hero.optString("name")).put("cost",cost).put("reward",reward).put("source",activity.optString("rule_source")).put("page",activity.optInt("rule_page")).put("applied_at",System.currentTimeMillis()).put("hero_after",db.getCharacter(characterId)).put("party_after",db.getPartyState());
                if(activityCheck.optBoolean("required",false))db.consumeDiceHistoryChecked(activityCheck.optLong("roll_id",0),activityCheck.optString("test_key"),activityCheck.optString("consequence_key"));
                ContentValues v=new ContentValues();v.put("status","concluida");v.put("resolution_json",resolution.toString());v.put("applied_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());int changed=sql.update("life_activities",v,"id=? AND world_profile=? AND applied_at=0",new String[]{String.valueOf(activityId),profile()});if(changed!=1)throw new IllegalStateException("A atividade foi alterada durante a aplicação.");
                sql.setTransactionSuccessful();out.put("applied",true).put("resolution",resolution);
            }finally{sql.endTransaction();}
            db.addJournal("Atividade concluída — "+activity.optString("title"),activity.optString("narrative_result")+"\n\nEfeitos aplicados conforme "+activity.optString("rule_source")+", p. "+activity.optInt("rule_page")+".","Mundo Vivo");
            db.addAuditEvent("mundo_vivo","Resultado mecânico aplicado",activity.optString("title"),before.toString(),out.toString(),true);
            return out;
        }catch(Exception e){try{return out.put("applied",false).put("reason",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(Exception ignored){return new JSONObject();}}
    }

    public boolean activityCheckSatisfied(JSONObject check){
        if(check==null||!check.optBoolean("required",false))return true;
        long rollId=check.optLong("roll_id",0);if(rollId<=0)return false;
        JSONObject roll=db.getDiceHistoryById(rollId),hero=db.getCharacter(check.optLong("actor_character_id",0));
        JSONObject calculation=new JSONObject();try{calculation.put("roll_id",rollId).put("roll_formula",check.optString("formula")).put("roll_total",check.optInt("total")).put("test_key",check.optString("test_key")).put("action_signature",check.optString("action_signature")).put("scene_fingerprint",check.optString("scene_fingerprint")).put("source_rule_id",check.optString("source_rule_id"))
                .put("procedure_id",check.optString("procedure_id")).put("campaign_fingerprint",check.optString("campaign_fingerprint")).put("target_signature",check.optString("target_signature")).put("consequence_key",check.optString("consequence_key"))
                .put("modifier",check.optInt("modifier",Integer.MIN_VALUE)).put("modifier_source",check.optString("modifier_source")).put("roll_mode",check.optString("roll_mode")).put("natural_roll",check.optInt("natural_roll",Integer.MIN_VALUE))
                .put("critical",check.optBoolean("critical",false)).put("critical_failure",check.optBoolean("critical_failure",false)).put("outcome",check.optString("outcome"));}catch(Exception failure){AppLog.e("LivingWorldEngine","Falha ao montar vínculo da atividade.",failure);return false;}
        return MechanicalRollBinding.verify(roll,calculation,hero,System.currentTimeMillis()).valid;
    }
    private JSONObject activityRollBinding(JSONObject activity,String source,int page,String excerpt,JSONObject check)throws Exception{
        long activityId=activity.optLong("id",0),characterId=activity.optLong("character_id",0);JSONObject hero=db.getCharacter(characterId);
        JSONObject roll=db.getDiceHistoryById(check.optLong("roll_id",0));if(roll.length()==0)throw new IllegalStateException("A rolagem local da atividade não foi encontrada.");
        String modifierSource=roll.optString("modifier_source").trim(),rollMode=roll.optString("roll_mode").trim();int natural=roll.optInt("natural_roll",0);
        if(modifierSource.length()==0||rollMode.length()==0||natural<=0)throw new IllegalStateException("A rolagem foi criada sem modificador da ficha, modo ou resultado natural e não pode autorizar a atividade.");
        String sourceRule="activity-rule:"+stableId(profile()+"|"+source+"|"+page+"|"+excerpt),procedure=sourceRule+":procedure:activity";
        String campaign=stableId(profile()+"|"+db.getCampaign().optString("name")+"|"+db.getCampaign().optString("rules_profile"));
        String target=stableId("activity-target|"+profile()+"|"+activityId+"|"+characterId),consequence=stableId("activity-consequence|"+profile()+"|"+activityId);
        return new JSONObject().put("formula",check.optString("formula")).put("total",check.optInt("total")).put("test_key","activity:"+profile()+":"+activityId).put("action_signature",stableId(profile()+"|activity|"+activityId+"|"+activity.optString("title")+"|"+sourceRule)).put("scene_fingerprint","living-world:"+profile()+":activity:"+activityId).put("source_rule_id",sourceRule)
                .put("procedure_id",procedure).put("campaign_fingerprint",campaign).put("target_signature",target).put("consequence_key",consequence)
                .put("modifier",roll.optInt("modifier")).put("modifier_source",modifierSource).put("roll_mode",rollMode).put("natural_roll",natural).put("critical",roll.optBoolean("critical",false)).put("critical_failure",roll.optBoolean("critical_failure",false))
                .put("actor_character_id",characterId).put("actor_name",hero.optString("name",activity.optString("character_name"))).put("outcome","activity_resolution");
    }
    private static JSONObject normalizeActivityCheck(JSONObject check)throws Exception{JSONObject out=new JSONObject();boolean required=check!=null&&check.optBoolean("required",false);out.put("required",required);if(!required)return out;out.put("verified",false);out.put("roll_id",Math.max(0,check.optLong("roll_id",0)));if(check.optString("formula").trim().length()>0)out.put("formula",check.optString("formula").trim());if(check.has("total"))out.put("total",check.optInt("total"));for(String key:new String[]{"test_key","action_signature","scene_fingerprint","source_rule_id","procedure_id","campaign_fingerprint","target_signature","consequence_key","modifier_source","roll_mode","actor_name","outcome"})if(check.optString(key).trim().length()>0)out.put(key,check.optString(key).trim());for(String key:new String[]{"modifier","natural_roll"})if(check.has(key))out.put(key,check.optInt(key));for(String key:new String[]{"critical","critical_failure"})if(check.has(key))out.put(key,check.optBoolean(key));if(check.optLong("actor_character_id",0)>0)out.put("actor_character_id",check.optLong("actor_character_id"));return out;}
    private static String stableId(String value){return UUID.nameUUIDFromBytes((value==null?"":value).getBytes(StandardCharsets.UTF_8)).toString();}
    private static boolean mechanicalOutcomeMatchesExcerpt(String excerpt,JSONObject cost,JSONObject reward){String n=normalizeText(excerpt);return outcomeObjectMatches(n,cost)&&outcomeObjectMatches(n,reward);}
    private static boolean outcomeObjectMatches(String excerpt,JSONObject value){if(value==null||value.length()==0)return true;java.util.Iterator<String> keys=value.keys();while(keys.hasNext()){String key=keys.next();Object raw=value.opt(key);if(raw instanceof Number&&Math.abs(((Number)raw).doubleValue())<0.0001d)continue;String required="";if("gold_delta".equals(key))required="ouro moeda moedas po peça peças";else if("xp_delta".equals(key))required="xp experiencia experiência pontos";else if("provisions_delta".equals(key))required="provisao provisões provisoes alimento ração racao";else if("water_delta".equals(key))required="agua água odre";else if("downtime_days".equals(key))required="dia dias tempo atividade";else if("item_add".equals(key)||"item_remove".equals(key)){String item=normalizeText(String.valueOf(raw));if(item.length()>2&&!excerpt.contains(item))return false;continue;}else if("resource_key".equals(key)||"resource_delta".equals(key)){String resource=normalizeText(value.optString("resource_key"));if(resource.length()>2&&!excerpt.contains(resource))return false;continue;}if(required.length()>0){boolean hit=false;for(String token:required.split(" "))if(excerpt.contains(token)){hit=true;break;}if(!hit)return false;}}return true;}
    private static String normalizeText(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}

    private static int sumInt(JSONObject a,JSONObject b,String key){return (int)Math.round((a==null?0d:a.optDouble(key,0d))+(b==null?0d:b.optDouble(key,0d)));}
    private static void applyGenericResource(JSONObject resources,JSONObject change)throws Exception{if(change==null)return;String key=change.optString("resource_key").trim();if(key.length()>0)resources.put(key,Math.max(0,resources.optInt(key,0)+(int)Math.round(change.optDouble("resource_delta",0d))));}
    private static String applyItemChange(String equipment,JSONObject change){String out=equipment==null?"":equipment;String add=change==null?"":change.optString("item_add").trim(),remove=change==null?"":change.optString("item_remove").trim();if(remove.length()>0)out=removeItem(out,remove);if(add.length()>0&&!out.toLowerCase(Locale.ROOT).contains(add.toLowerCase(Locale.ROOT)))out+=(out.trim().length()>0?"\n":"")+"• "+add;return out;}
    private static String removeItem(String equipment,String item){String[] rows=(equipment==null?"":equipment).split("\r?\n");StringBuilder out=new StringBuilder();boolean removed=false;for(String row:rows){String clean=row.replace("•","").trim();if(!removed&&clean.equalsIgnoreCase(item)){removed=true;continue;}if(row.trim().length()>0){if(out.length()>0)out.append('\n');out.append(row.trim());}}return out.toString();}

    public JSONObject troupe(){ensureTables();return queryOne("SELECT * FROM living_troupes WHERE world_profile=? AND active=1 ORDER BY updated_at DESC,id DESC",new String[]{profile()});}
    public long saveTroupe(String name,String mode,String location,String homeBase,JSONArray members,JSONArray itinerary,String notes){
        ensureTables();JSONObject existing=troupe();String itineraryRaw=itinerary==null?"[]":itinerary.toString();ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("name",clean(name,"Companhia itinerante"));v.put("troupe_mode",clean(mode,"itinerante"));v.put("current_location",text(location));v.put("home_base",text(homeBase));v.put("members_json",members==null?"[]":members.toString());v.put("itinerary_json",itineraryRaw);v.put("notes",text(notes));v.put("resources_json","{}");v.put("active",1);v.put("updated_at",System.currentTimeMillis());if(existing.length()>0){long id=existing.optLong("id");if(!itineraryRaw.equals(existing.optString("itinerary_json","[]"))){v.put("itinerary_index",0);v.put("last_travel_day",currentDay());}db.getWritableDatabase().update("living_troupes",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});return id;}v.put("itinerary_index",0);v.put("last_travel_day",currentDay());v.put("created_at",System.currentTimeMillis());return db.getWritableDatabase().insert("living_troupes",null,v);
    }

    public JSONObject previewAdvanceDays(int days){
        JSONObject scene=db.getSceneState(),out=new JSONObject();int safe=Math.max(1,Math.min(36500,days)),before=Math.max(1,scene.optInt("day_number",1));try{JSONObject tick=new WorldStateEngine(db).preview("MUNDO_VIVO_AVANCO_CONFIRMADO: "+safe+" dias");out.put("preview_only",true).put("days_advanced",safe).put("day_before",before).put("day_after",before+safe).put("calendar_before",calendarLabel(before)).put("calendar_after",calendarLabel(before+safe)).put("world_tick",tick).put("activity_mechanics_applied",false).put("warning","Relógios, eventos e recursos opcionais usam o motor canônico. Resultados de atividades continuam aguardando validação individual.");}catch(Exception ignored){AppLog.ignored("LivingWorldEngine","Falha ao preparar avanço.",ignored);}return out;
    }

    public synchronized boolean commitAdvance(JSONObject preview){
        if(preview==null||!preview.optBoolean("preview_only",false)||preview.optBoolean("committed",false))return false;
        try{
            int before=Math.max(1,preview.optInt("day_before",currentDay())),after=Math.max(before,preview.optInt("day_after",before));JSONObject tick=preview.optJSONObject("world_tick");if(tick==null||!new WorldStateEngine(db).commit(tick))return false;
            ContentValues stateValues=new ContentValues();stateValues.put("last_simulated_day",after);stateValues.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("living_world_state",stateValues,"world_profile=?",new String[]{profile()});
            preview.put("preview_only",false).put("committed",true);JSONObject atlasChange=new AtlasSandboxEngine(db).afterWorldAdvance(after);if(atlasChange.length()>0)preview.put("atlas_sandbox_change",atlasChange);db.addAuditEvent("mundo_vivo","Calendário avançado",before+" → "+after,"{}",preview.toString(),true);return true;
        }catch(Exception e){AppLog.e("LivingWorldEngine","Falha ao avançar o calendário.",e);return false;}
    }

    /**
     * Painel operacional do Mundo Vivo. Reúne dados persistentes do simulador
     * para deixar claro o que aconteceu automaticamente, o que deve acontecer
     * em seguida e o que está aguardando revisão humana.
     */
    public JSONObject automationDashboard(int recentLimit,int upcomingLimit){
        ensureTables();JSONObject out=new JSONObject();int day=currentDay();long now=System.currentTimeMillis();
        try{
            JSONObject world=state();out.put("enabled",world.optInt("enabled",1)==1).put("simulation_mode",world.optString("simulation_mode","equilibrado")).put("current_day",day).put("calendar",calendarLabel(day)).put("last_simulated_day",world.optInt("last_simulated_day",day)).put("last_background_sync_at",world.optLong("last_background_sync_at",0)).put("background_enabled",world.optInt("background_simulation_enabled",0)==1).put("generated_at",now);

            JSONArray recent=new JSONArray();
            String recentSql="SELECT title,body AS detail,kind AS source,created_at FROM journal WHERE world_profile=? AND (kind='Mundo Vivo' OR kind='Guildas' OR title LIKE 'Movimento de facção%' OR title LIKE 'Ação fora de cena%' OR title LIKE 'Atividade%' OR title LIKE 'Itinerário%') UNION ALL SELECT title,summary AS detail,category AS source,created_at FROM audit_log WHERE world_profile=? AND category IN ('mundo_vivo','guildas','faccao','npc','viagem','evento') ORDER BY created_at DESC";
            for(JSONObject row:queryList(recentSql,new String[]{profile(),profile()},Math.max(1,Math.min(30,recentLimit))))recent.put(new JSONObject().put("kind","recent").put("title",row.optString("title","Ação automática")).put("detail",row.optString("detail")).put("source",row.optString("source","Mundo Vivo")).put("created_at",row.optLong("created_at",0)));
            out.put("recent_activity",recent);

            ArrayList<JSONObject> upcoming=new ArrayList<JSONObject>();
            for(JSONObject event:queryList("SELECT id,title,description,due_day,due_at,status FROM world_events WHERE world_profile=? AND status IN ('scheduled','awaiting_review') ORDER BY CASE WHEN due_day>0 THEN due_day ELSE 2147483647 END,due_at",new String[]{profile()},40)){
                int due=event.optInt("due_day",0);long dueAt=event.optLong("due_at",0);int priority=("awaiting_review".equals(event.optString("status"))||due>0&&due<=day||dueAt>0&&dueAt<=now)?0:2;
                upcoming.add(dashboardItem("evento",event.optString("title","Evento do mundo"),event.optString("description"),due,dueAt,priority,event.optString("status")));
            }
            for(JSONObject activity:queryList("SELECT a.id,a.title,a.status,a.start_day,a.duration_days,a.next_check_day,a.progress_units,a.total_units,c.name character_name FROM life_activities a LEFT JOIN characters c ON c.id=a.character_id AND c.world_profile=a.world_profile WHERE a.world_profile=? AND a.status NOT IN ('concluida','cancelada') ORDER BY CASE WHEN a.next_check_day>0 THEN a.next_check_day ELSE a.start_day+a.duration_days END,a.updated_at",new String[]{profile()},40)){
                int due=activity.optInt("next_check_day",0);if(due<=0)due=activity.optInt("start_day",day)+Math.max(1,activity.optInt("duration_days",1));
                String detail=clean(activity.optString("character_name"),"Herói")+" • "+formatProgress(activity.optDouble("progress_units",0),activity.optDouble("total_units",Math.max(1,activity.optInt("duration_days",1))));
                int priority=activity.optString("status").startsWith("aguardando")?0:(due<=day?1:3);upcoming.add(dashboardItem("atividade",activity.optString("title","Atividade"),detail,due,0,priority,activity.optString("status")));
            }
            for(JSONObject clock:queryList("SELECT id,name,current_value,max_value,automatic,paused,interval_hours,accumulated_hours,rate_multiplier,trigger_condition FROM world_clocks WHERE world_profile=? AND active=1 ORDER BY updated_at DESC",new String[]{profile()},30)){
                if(clock.optInt("automatic",0)!=1||clock.optInt("paused",0)==1)continue;double rate=Math.max(.01d,clock.optDouble("rate_multiplier",1d)),remaining=Math.max(0d,clock.optDouble("interval_hours",6d)-clock.optDouble("accumulated_hours",0d))/rate;int due=day+(int)Math.ceil(remaining/24d);String detail=clock.optInt("current_value",0)+"/"+Math.max(1,clock.optInt("max_value",4))+" • próximo avanço em "+formatHours(remaining);if(clock.optString("trigger_condition").trim().length()>0)detail+=" • condição: "+clock.optString("trigger_condition").trim();upcoming.add(dashboardItem("relógio",clock.optString("name","Relógio"),detail,due,0,remaining<=0?0:4,"automatico"));
            }
            for(JSONObject faction:queryList("SELECT id,name,current_plan,plan_interval_hours,plan_progress_hours,plan_progress,last_action_day,last_action_summary FROM factions WHERE world_profile=? AND active=1 AND TRIM(COALESCE(current_plan,''))<>'' ORDER BY updated_at DESC",new String[]{profile()},30)){
                double remaining=Math.max(0d,faction.optDouble("plan_interval_hours",24d)-faction.optDouble("plan_progress_hours",0d));int due=day+(int)Math.ceil(remaining/24d);String detail=faction.optString("current_plan")+" • etapa "+Math.max(0,faction.optInt("plan_progress",0)+1)+" • em "+formatHours(remaining);upcoming.add(dashboardItem("facção",faction.optString("name","Facção"),detail,due,0,remaining<=0?1:5,"planejada"));
            }
            for(JSONObject npc:queryList("SELECT npc_name,next_action,urgency,current_location,last_action_day FROM npc_cognitive_state WHERE world_profile=? AND TRIM(COALESCE(next_action,''))<>'' ORDER BY urgency DESC,updated_at DESC",new String[]{profile()},20)){
                int urgency=npc.optInt("urgency",0),due=urgency>=70?day:day+Math.max(1,(70-urgency+19)/20);String detail=npc.optString("next_action")+(npc.optString("current_location").trim().length()>0?" • "+npc.optString("current_location"):"")+" • urgência "+urgency;upcoming.add(dashboardItem("npc",npc.optString("npc_name","NPC"),detail,due,0,urgency>=70?1:6,"agendada"));
            }
            for(JSONObject project:queryList("SELECT p.id,p.title,p.progress,p.goal,p.progress_per_day,p.location,g.name guild_name FROM guild_projects p INNER JOIN guilds g ON g.id=p.guild_id WHERE p.world_profile=? AND p.status='active' ORDER BY p.updated_at DESC",new String[]{profile()},30)){
                double remaining=Math.max(0d,project.optDouble("goal",100d)-project.optDouble("progress",0d)),perDay=Math.max(.01d,project.optDouble("progress_per_day",5d));int due=day+(int)Math.ceil(remaining/perDay);String detail=project.optString("guild_name")+" • "+formatProgress(project.optDouble("progress",0),project.optDouble("goal",100d));if(project.optString("location").trim().length()>0)detail+=" • "+project.optString("location");upcoming.add(dashboardItem("guilda",project.optString("title","Projeto de guilda"),detail,due,0,5,"projeto"));
            }
            for(JSONObject due:queryList("SELECT gm.id,gm.npc_name,gm.character_id,gm.dues_due_day,g.name guild_name,gr.dues_amount,c.name character_name FROM guild_members gm INNER JOIN guilds g ON g.id=gm.guild_id LEFT JOIN guild_ranks gr ON gr.id=gm.rank_id LEFT JOIN characters c ON c.id=gm.character_id WHERE gm.world_profile=? AND gm.status='active' AND gm.dues_due_day>0 ORDER BY gm.dues_due_day",new String[]{profile()},30)){
                String member=due.optString("character_name");if(member.trim().length()==0)member=due.optString("npc_name","Membro");String detail=due.optString("guild_name")+" • mensalidade "+due.optDouble("dues_amount",0);upcoming.add(dashboardItem("guilda",member,detail,due.optInt("dues_due_day"),0,due.optInt("dues_due_day")<=day?0:4,"mensalidade"));
            }
            Collections.sort(upcoming,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){int p=Integer.compare(a.optInt("priority",9),b.optInt("priority",9));if(p!=0)return p;int ad=a.optInt("due_day",Integer.MAX_VALUE),bd=b.optInt("due_day",Integer.MAX_VALUE);if(ad!=bd)return Integer.compare(ad,bd);return a.optString("title").compareToIgnoreCase(b.optString("title"));}});
            JSONArray next=new JSONArray();for(int i=0;i<upcoming.size()&&i<Math.max(1,Math.min(30,upcomingLimit));i++)next.put(upcoming.get(i));out.put("upcoming_actions",next);

            JSONArray pending=new JSONArray();
            for(JSONObject tx:queryList("SELECT id,title,summary,status,created_at,effect_key FROM pending_transactions WHERE world_profile=? AND status IN ('pending','reviewing') ORDER BY created_at DESC",new String[]{profile()},30))pending.put(new JSONObject().put("kind","transação").put("id",tx.optLong("id")).put("title",tx.optString("title","Consequência pendente")).put("detail",tx.optString("summary")).put("status",tx.optString("status")).put("created_at",tx.optLong("created_at")));
            for(JSONObject event:queryList("SELECT id,title,description,status,consequence_status,due_day,created_at FROM world_events WHERE world_profile=? AND (status='awaiting_review' OR consequence_status IN ('awaiting_review','pending_review','blocked')) ORDER BY created_at DESC",new String[]{profile()},20))pending.put(new JSONObject().put("kind","evento").put("id",event.optLong("id")).put("title",event.optString("title","Evento pendente")).put("detail",event.optString("description")).put("status",event.optString("consequence_status",event.optString("status"))).put("due_day",event.optInt("due_day")).put("created_at",event.optLong("created_at")));
            for(JSONObject activity:queryList("SELECT a.id,a.title,a.status,a.updated_at,c.name character_name FROM life_activities a LEFT JOIN characters c ON c.id=a.character_id AND c.world_profile=a.world_profile WHERE a.world_profile=? AND a.status IN ('aguardando_regra','aguardando_aplicacao') ORDER BY a.updated_at DESC",new String[]{profile()},20))pending.put(new JSONObject().put("kind","atividade").put("id",activity.optLong("id")).put("title",activity.optString("title","Atividade pendente")).put("detail",clean(activity.optString("character_name"),"Herói")).put("status",activity.optString("status")).put("created_at",activity.optLong("updated_at")));
            out.put("pending_review",pending).put("recent_count",recent.length()).put("upcoming_count",next.length()).put("pending_count",pending.length());
        }catch(Exception e){try{out.put("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(Exception ignored){AppLog.ignored("LivingWorldEngine","Falha ao montar painel de automação.",ignored);}}
        return out;
    }

    private static JSONObject dashboardItem(String kind,String title,String detail,int dueDay,long dueAt,int priority,String status){JSONObject out=new JSONObject();try{out.put("kind",kind).put("title",title).put("detail",detail).put("due_day",dueDay).put("due_at",dueAt).put("priority",priority).put("status",status);}catch(Exception ignored){AppLog.ignored("LivingWorldEngine","Falha ao montar item do painel.",ignored);}return out;}
    private static String formatHours(double hours){if(hours<=.01d)return "agora";if(hours<24d)return Math.max(1,(int)Math.ceil(hours))+" h";double days=hours/24d;return (int)Math.ceil(days)+" dia(s)";}
    private static String formatProgress(double value,double goal){double safeGoal=Math.max(1d,goal),pct=Math.max(0d,Math.min(100d,value/safeGoal*100d));return String.format(Locale.US,"%.0f/%.0f (%.0f%%)",value,safeGoal,pct);}

    public String compactContext(){
        JSONObject out=new JSONObject();try{
            JSONObject s=state();String mode=s.optString("simulation_mode","equilibrado");out.put("enabled",s.optInt("enabled",1)==1);out.put("mode",mode);out.put("mode_instruction",modeInstruction(mode));out.put("calendar",calendarLabel(currentDay()));out.put("day_number",currentDay());out.put("rule_boundary","Família, idade, trabalho, caça, estudo e trupe são narrativos até uma regra válida do mundo ativo autorizar efeitos mecânicos.");
            JSONArray heroes=new JSONArray();for(JSONObject c:db.listActiveCharacters()){JSONObject life=heroLife(c.optLong("id"));heroes.put(new JSONObject().put("id",c.optLong("id")).put("name",c.optString("name")).put("age_years",life.optInt("age_years")).put("residence",life.optString("residence")).put("occupation",life.optString("occupation")).put("family_name",life.optString("family_name")).put("relationship_status",life.optString("relationship_status")));}
            out.put("hero_lives",heroes);out.put("troupe",troupe());out.put("recent_activities",new JSONArray(activities(12)));
        }catch(Exception e){try{out.put("error",e.getMessage());}catch(Exception ignored){AppLog.ignored("LivingWorldEngine","Falha ao montar contexto.",ignored);}}return out.toString();
    }

    public String calendarLabel(int day){
        JSONObject s=state();int length=Math.max(1,s.optInt("days_per_year",DEFAULT_DAYS_PER_YEAR)),epoch=Math.max(1,s.optInt("epoch_year",1)),safe=Math.max(1,day),year=epoch+(safe-1)/length,dayOfYear=((safe-1)%length)+1;return "Ano "+year+" • dia "+dayOfYear+" ("+s.optString("calendar_name","Calendário da campanha")+")";
    }

    public int daysPerYear(){return Math.max(1,state().optInt("days_per_year",DEFAULT_DAYS_PER_YEAR));}
    public int ageFor(JSONObject life){return LivingWorldRulePolicy.ageYears(life.optInt("initial_age_years",18),life.optInt("birth_day",1),currentDay(),daysPerYear());}
    public int currentDay(){return Math.max(1,db.getSceneState().optInt("day_number",1));}
    private String profile(){return db.getActiveMapProfile();}
    private boolean hasEnabledManualSource(String source){
        String wanted=baseName(source);if(wanted.length()==0)return false;
        for(JSONObject manual:queryList("SELECT title,source,local_uri FROM manuals WHERE enabled=1",null,500)){
            for(String key:new String[]{"title","source","local_uri"}){String candidate=baseName(manual.optString(key));if(wanted.equals(candidate)&&LivingWorldRulePolicy.sourceBelongsToWorld(profile(),candidate))return true;}
        }
        return false;
    }
    private static String baseName(String value){String s=value==null?"":value.trim().replace('\\','/');int slash=s.lastIndexOf('/');return slash>=0?s.substring(slash+1):s;}

    private void ensureTables(){AppDb.ensureLivingWorldTables(db.getWritableDatabase());}
    private JSONObject queryOne(String sql,String[] args){List<JSONObject> rows=queryList(sql,args,1);return rows.isEmpty()?new JSONObject():rows.get(0);}
    private List<JSONObject> queryList(String sql,String[] args,int limit){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();Cursor c=null;try{c=db.getReadableDatabase().rawQuery(sql+(limit>0?" LIMIT "+limit:""),args);while(c.moveToNext()){JSONObject row=new JSONObject();for(int i=0;i<c.getColumnCount();i++){String key=c.getColumnName(i);switch(c.getType(i)){case Cursor.FIELD_TYPE_INTEGER:row.put(key,c.getLong(i));break;case Cursor.FIELD_TYPE_FLOAT:row.put(key,c.getDouble(i));break;case Cursor.FIELD_TYPE_STRING:row.put(key,c.getString(i));break;case Cursor.FIELD_TYPE_BLOB:row.put(key,"");break;default:row.put(key,JSONObject.NULL);}}out.add(row);}}catch(Exception e){AppLog.e("LivingWorldEngine","Consulta do Mundo Vivo falhou.",e);}finally{if(c!=null)c.close();}return out;
    }
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static String modeInstruction(String mode){if("leve".equals(mode))return "Use apenas acontecimentos de vida essenciais.";if("simulacao".equals(mode))return "Acompanhe consequências cotidianas e vínculos com mais detalhe, sem criar mecânicas.";return "Equilibre aventura, passagem do tempo e vida cotidiana.";}
    private static String safeMode(String value){return "leve".equals(value)||"simulacao".equals(value)?value:"equilibrado";}
    private static String clean(String value,String fallback){String s=text(value);return s.length()==0?fallback:s;}
    private static String text(String value){return value==null?"":value.trim();}
}
