package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Estado persistente de animais companheiros usando a memória canônica de NPC.
 * O animal continua existindo no Mundo Vivo mesmo fora da party.
 */
public final class AnimalCompanionStateEngine {
    private static final String STATE_KEY="animal_companion";
    private final AppDb db;
    private final LivingWorldEngine living;

    public AnimalCompanionStateEngine(AppDb database){db=database;living=new LivingWorldEngine(database);}

    public JSONObject register(long ownerCharacterId,String name,String species,String size,int intelligenceRating,int handlerWisdomScore,boolean domestic,String location)throws Exception{
        if(ownerCharacterId<=0)throw new IllegalArgumentException("Dono do animal inválido.");
        String safeName=clean(name);if(safeName.length()==0)throw new IllegalArgumentException("Animal sem nome.");
        JSONObject previous=db.findNpcMemoryByName(safeName),stats=parse(previous.optString("stats_json","{}"));
        if(previous.length()>0&&!stats.has(STATE_KEY))throw new IllegalStateException("Já existe um NPC não animal com o nome "+safeName+".");
        JSONObject state=stats.optJSONObject(STATE_KEY);if(state==null)state=new JSONObject();
        int wis=Math.max(1,Math.min(30,handlerWisdomScore)),intel=Math.max(1,Math.min(20,intelligenceRating));
        int day=currentDay(),maxLoyalty=AnimalCompanionRulesEngine.maxLoyalty(wis);
        if(!state.has("loyalty"))state.put("loyalty",domestic?AnimalCompanionRulesEngine.startingDomesticLoyalty(wis):0);
        state.put("version",1).put("owner_character_id",ownerCharacterId).put("species",clean(species)).put("size",canonicalSize(size))
                .put("intelligence_rating",intel).put("handler_wisdom_score",wis).put("max_loyalty",maxLoyalty)
                .put("domestic",domestic).put("acclimated",domestic).put("active_companion",true)
                .put("sidekick_level",Math.max(0,state.optInt("sidekick_level",0))).put("companion_life",true).put("defining_role",state.optString("defining_role",""))
                .put("daily_food_lb",AnimalCompanionRulesEngine.dailyFoodPounds(size)).put("daily_water_gallons",AnimalCompanionRulesEngine.dailyWaterGallons(size));
        if(!state.has("tricks"))state.put("tricks",new JSONArray());
        if(!state.has("registered_day"))state.put("registered_day",day);
        if(!state.has("fed_through_day"))state.put("fed_through_day",day);
        if(!state.has("watered_through_day"))state.put("watered_through_day",day);
        state.put("last_location",clean(location));stats.put(STATE_KEY,state);

        JSONObject npc=new JSONObject().put("name",safeName).put("summary",clean(species)+" — companheiro animal")
                .put("relationship","companheiro animal de personagem #"+ownerCharacterId).put("last_seen",clean(location))
                .put("stats_json",stats.toString()).put("active",1);
        db.upsertNpcMemoryChecked(npc);
        ensureRelationship(ownerCharacterId,safeName);
        JSONObject cognition=db.getNpcCognitiveState(safeName);cognition.put("name",safeName).put("emotion","calmo")
                .put("trust",Math.max(-100,Math.min(100,state.optInt("loyalty",0)*5))).put("current_location",clean(location))
                .put("next_action","acompanhar o tratador e reagir conforme treino, lealdade e necessidades");
        db.upsertNpcCognitiveStateChecked(cognition);
        return snapshot(safeName);
    }

    public JSONObject snapshot(String name){
        JSONObject npc=db.findNpcMemoryByName(name),out=new JSONObject();try{
            if(npc.length()==0)return out;JSONObject state=parse(npc.optString("stats_json","{}")).optJSONObject(STATE_KEY);if(state==null)return out;
            out.put("npc_id",npc.optLong("id")).put("name",npc.optString("name")).put("location",npc.optString("last_seen")).put("state",new JSONObject(state.toString()));
            appendCareStatus(out.optJSONObject("state"));
        }catch(Exception ignored){AppLog.ignored("AnimalCompanionStateEngine","Falha ao montar estado do animal.",ignored);}return out;
    }

    public JSONArray snapshotAll(int limit){
        JSONArray out=new JSONArray(),rows=db.listNpcMemories(Math.max(1,Math.min(5000,limit*10)));for(int i=0;i<rows.length()&&out.length()<Math.max(1,limit);i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;JSONObject state=parse(row.optString("stats_json","{}")).optJSONObject(STATE_KEY);if(state==null)continue;
            try{JSONObject item=new JSONObject().put("npc_id",row.optLong("id")).put("name",row.optString("name")).put("location",row.optString("last_seen")).put("state",new JSONObject(state.toString()));appendCareStatus(item.optJSONObject("state"));out.put(item);}catch(Exception ignored){AppLog.ignored("AnimalCompanionStateEngine","Falha ao listar animal.",ignored);}
        }return out;
    }

    public JSONObject configureSidekick(String name,String definingRole,int level)throws Exception{
        JSONObject npc=requireAnimal(name),stats=parse(npc.optString("stats_json","{}")),state=requireState(stats);String role=clean(definingRole);int safeLevel=Math.max(0,Math.min(20,level));
        if(role.length()>0&&!AnimalCompanionRulesEngine.validRole(role))throw new IllegalArgumentException("Papel de companheiro animal não reconhecido.");
        state.put("sidekick_level",safeLevel).put("defining_role",role).put("companion_life",true).put("indomitable_uses",AnimalCompanionRulesEngine.indomitableUses(safeLevel)).put("second_wind_uses",AnimalCompanionRulesEngine.secondWindUses(safeLevel)).put("trick_mastery",AnimalCompanionRulesEngine.trickLimitRemoved(safeLevel));saveAnimal(npc,stats);return snapshot(name);
    }

    public JSONObject feedAndWater(String name,int days)throws Exception{
        JSONObject npc=requireAnimal(name),stats=parse(npc.optString("stats_json","{}")),state=requireState(stats);int day=currentDay(),span=Math.max(1,Math.min(30,days));
        state.put("fed_through_day",Math.max(day,state.optInt("fed_through_day",day))+span-1).put("watered_through_day",Math.max(day,state.optInt("watered_through_day",day))+span-1).put("last_care_day",day);
        saveAnimal(npc,stats);return snapshot(name);
    }

    public JSONObject applyLoyaltyEvent(String name,int d4,boolean gain,String reason)throws Exception{
        JSONObject npc=requireAnimal(name),stats=parse(npc.optString("stats_json","{}")),state=requireState(stats);
        int next=AnimalCompanionRulesEngine.applyLoyaltyEvent(state.optInt("loyalty",0),state.optInt("handler_wisdom_score",10),d4,gain);
        state.put("loyalty",next).put("last_loyalty_event",clean(reason)).put("last_loyalty_day",currentDay());saveAnimal(npc,stats);
        JSONObject cognition=db.getNpcCognitiveState(npc.optString("name"));cognition.put("name",npc.optString("name")).put("trust",Math.max(-100,Math.min(100,next*5)));db.upsertNpcCognitiveStateChecked(cognition);
        return snapshot(name);
    }

    public JSONObject startAcclimation(long ownerCharacterId,String name,boolean captive,String location)throws Exception{
        JSONObject npc=requireOwnedAnimal(ownerCharacterId,name),stats=parse(npc.optString("stats_json","{}")),state=requireState(stats);
        if(state.optBoolean("domestic",false)||state.optBoolean("acclimated",false))return new JSONObject().put("already_acclimated",true).put("animal",snapshot(name));
        int days=captive?AnimalCompanionRulesEngine.captiveAcclimationDowntimeDays():AnimalCompanionRulesEngine.normalAcclimationDowntimeDays();
        long activityId=living.planActivity(ownerCharacterId,"manejo_animal","Aclimatar "+npc.optString("name"),location,days,"animal="+npc.optString("name")+"; rule_id=animal.acclimation");
        JSONObject auth=living.authorizeCompiledSupplementActivity(activityId,"animal.acclimation");
        state.put("acclimation_activity_id",activityId).put("acclimation_captive",captive);saveAnimal(npc,stats);
        return new JSONObject().put("activity_id",activityId).put("days",days).put("authorization",auth).put("animal",snapshot(name));
    }

    public JSONObject completeAcclimation(long ownerCharacterId,String name,long activityId)throws Exception{
        JSONObject npc=requireOwnedAnimal(ownerCharacterId,name),activity=requireMatureActivity(activityId,"animal.acclimation"),stats=parse(npc.optString("stats_json","{}")),state=requireState(stats);
        state.put("acclimated",true).put("loyalty",AnimalCompanionRulesEngine.startingDomesticLoyalty(state.optInt("handler_wisdom_score",10))).put("acclimation_activity_id",0);saveAnimal(npc,stats);
        db.updateLifeActivityProgressChecked(activityId,activity.optDouble("total_units",activity.optInt("duration_days",1)),activity.optDouble("total_units",activity.optInt("duration_days",1)),"concluida","Animal aclimatado pelas regras compiladas do suplemento.",SupplementContentRegistry.mechanicalBasis("animal.acclimation"),true);
        return snapshot(name);
    }

    public JSONObject startTrickTraining(long ownerCharacterId,String name,String trick,String location)throws Exception{
        JSONObject npc=requireOwnedAnimal(ownerCharacterId,name),state=requireState(parse(npc.optString("stats_json","{}")));String safeTrick=clean(trick);if(safeTrick.length()==0)throw new IllegalArgumentException("Truque vazio.");
        if(!state.optBoolean("acclimated",false))throw new IllegalStateException("O animal ainda não está aclimatado para treino.");
        JSONArray tricks=state.optJSONArray("tricks");if(contains(tricks,safeTrick))return new JSONObject().put("already_known",true).put("animal",snapshot(name));
        if(!AnimalCompanionRulesEngine.trickLimitRemoved(state.optInt("sidekick_level",0))&&tricks!=null&&tricks.length()>=AnimalCompanionRulesEngine.maxRememberedTricks(state.optInt("intelligence_rating",1)))throw new IllegalStateException("O animal já atingiu o limite de truques memorizados.");
        long activityId=living.planActivity(ownerCharacterId,"manejo_animal","Treinar "+npc.optString("name")+": "+safeTrick,location,AnimalCompanionRulesEngine.trickTrainingDowntimeDays(),"animal="+npc.optString("name")+"; trick="+safeTrick+"; rule_id=animal.training.trick");
        JSONObject auth=living.authorizeCompiledSupplementActivity(activityId,"animal.training.trick");return new JSONObject().put("activity_id",activityId).put("days",AnimalCompanionRulesEngine.trickTrainingDowntimeDays()).put("authorization",auth).put("animal",snapshot(name));
    }

    public JSONObject completeTrickTraining(long ownerCharacterId,String name,String trick,long activityId)throws Exception{
        JSONObject npc=requireOwnedAnimal(ownerCharacterId,name),activity=requireMatureActivity(activityId,"animal.training.trick"),stats=parse(npc.optString("stats_json","{}")),state=requireState(stats);String safeTrick=clean(trick);
        JSONArray tricks=state.optJSONArray("tricks");if(tricks==null){tricks=new JSONArray();state.put("tricks",tricks);}if(!contains(tricks,safeTrick)){
            if(!AnimalCompanionRulesEngine.trickLimitRemoved(state.optInt("sidekick_level",0))&&tricks.length()>=AnimalCompanionRulesEngine.maxRememberedTricks(state.optInt("intelligence_rating",1)))throw new IllegalStateException("O animal já atingiu o limite de truques memorizados.");tricks.put(safeTrick);
        }
        state.put("last_training_day",currentDay());saveAnimal(npc,stats);
        db.updateLifeActivityProgressChecked(activityId,activity.optDouble("total_units",activity.optInt("duration_days",1)),activity.optDouble("total_units",activity.optInt("duration_days",1)),"concluida","Truque aprendido: "+safeTrick,SupplementContentRegistry.mechanicalBasis("animal.training.trick"),true);
        return snapshot(name);
    }

    private static JSONObject requireState(JSONObject stats){JSONObject state=stats==null?null:stats.optJSONObject(STATE_KEY);if(state==null)throw new IllegalArgumentException("Estado de companheiro animal ausente.");return state;}

    private JSONObject requireMatureActivity(long activityId,String expectedRule)throws Exception{
        JSONObject activity=living.activity(activityId);if(activity.length()==0)throw new IllegalArgumentException("Atividade de treino inexistente.");
        if(activity.optInt("rule_authorized",0)!=1||!AnimalCompanionRulesEngine.BOOK.equals(activity.optString("rule_source")))throw new IllegalStateException("Atividade sem autorização do suplemento animal.");
        JSONObject basis=SupplementContentRegistry.rule(expectedRule);if(!basis.optString("evidence").equals(activity.optString("rule_excerpt")))throw new IllegalStateException("Atividade vinculada a outra regra compilada.");
        int due=activity.optInt("start_day",1)+Math.max(1,activity.optInt("duration_days",1))-1;if(currentDay()<due)throw new IllegalStateException("A atividade ainda não acumulou o tempo necessário.");return activity;
    }

    private JSONObject requireOwnedAnimal(long owner,String name)throws Exception{JSONObject npc=requireAnimal(name),state=requireState(parse(npc.optString("stats_json","{}")));if(state.optLong("owner_character_id",0)!=owner)throw new IllegalStateException("O animal pertence a outro tratador.");return npc;}
    private JSONObject requireAnimal(String name)throws Exception{JSONObject npc=db.findNpcMemoryByName(name);if(npc.length()==0)throw new IllegalArgumentException("Animal não encontrado: "+clean(name));JSONObject stats=parse(npc.optString("stats_json","{}"));if(stats.optJSONObject(STATE_KEY)==null)throw new IllegalArgumentException("O NPC não é um companheiro animal registrado.");return npc;}
    private void saveAnimal(JSONObject npc,JSONObject stats)throws Exception{db.upsertNpcMemoryChecked(new JSONObject().put("name",npc.optString("name")).put("stats_json",stats.toString()));}
    private void ensureRelationship(long owner,String animalName){for(JSONObject r:living.relationships(owner))if(normalize(r.optString("related_name")).equals(normalize(animalName))&&normalize(r.optString("relation_type")).contains("animal"))return;living.addRelationship(owner,animalName,"companheiro animal","ativo",-1,"Vínculo persistente; treino, lealdade e cuidado seguem Animal Handlers Manual.");}
    private void appendCareStatus(JSONObject state)throws Exception{if(state==null)return;int day=currentDay();int sidekickLevel=state.optInt("sidekick_level",0);state.put("care_day",day).put("needs_food",day>state.optInt("fed_through_day",0)).put("needs_water",day>state.optInt("watered_through_day",0)).put("max_tricks",AnimalCompanionRulesEngine.trickLimitRemoved(sidekickLevel)?-1:AnimalCompanionRulesEngine.maxRememberedTricks(state.optInt("intelligence_rating",1))).put("companion_life",true);}
    private int currentDay(){return Math.max(1,db.getSceneState().optInt("day_number",1));}
    private static boolean contains(JSONArray values,String wanted){if(values==null)return false;String n=normalize(wanted);for(int i=0;i<values.length();i++)if(normalize(values.optString(i)).equals(n))return true;return false;}
    private static JSONObject parse(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception ignored){return new JSONObject();}}
    private static String canonicalSize(String raw){String n=normalize(raw);if(n.contains("tiny")||n.contains("miud")||n.contains("minuscul"))return "Tiny";if(n.contains("garg"))return "Gargantuan";if(n.contains("huge")||n.contains("enorme"))return "Huge";if(n.contains("large")||n.contains("grande"))return "Large";if(n.contains("small")||n.contains("pequen"))return "Small";return "Medium";}
    private static String clean(String value){return value==null?"":value.trim().replaceAll("\\s+"," ");}
    private static String normalize(String value){return Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}
}
