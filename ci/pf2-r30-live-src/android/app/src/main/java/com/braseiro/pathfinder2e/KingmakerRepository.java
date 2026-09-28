package com.braseiro.pathfinder2e;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistência Kingmaker no AppDb canônico, sempre isolada por world_profile. */
public final class KingmakerRepository {
    public static final int SCHEMA_VERSION=1;
    private static final String[] BACKUP_KEYS={"kingmakerSkills","kingmakerLeaders","kingmakerFeatures","kingmakerHexes","kingmakerSettlements","kingmakerStructures","kingmakerTurns","kingmakerTurnActivities","kingmakerEvents","kingmakerArmies","kingmakerWarEncounters","kingmakerCompanions","kingmakerCamps","kingmakerTradeAgreements"};
    private static final String[] BACKUP_TABLES={"kingmaker_skills","kingmaker_leaders","kingmaker_features","kingmaker_hexes","kingmaker_settlements","kingmaker_structures","kingmaker_turns","kingmaker_turn_activities","kingmaker_events","kingmaker_armies","kingmaker_war_encounters","kingmaker_companions","kingmaker_camps","kingmaker_trade_agreements"};
    private final AppDb db;
    private final KingmakerRulesEngine rules;

    public KingmakerRepository(AppDb database){
        if(database==null)throw new IllegalArgumentException("AppDb ausente.");
        db=database;rules=new KingmakerRulesEngine();ensureSchema(db.getWritableDatabase());ensureProfile();
    }

    /** Migração idempotente. Nenhuma tabela oficial do Atlas é alterada. */
    public static void ensureSchema(SQLiteDatabase sql){
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_state (world_profile TEXT PRIMARY KEY, enabled INTEGER NOT NULL DEFAULT 0, campaign_mode TEXT NOT NULL DEFAULT 'standard', kingdom_name TEXT NOT NULL DEFAULT '', capital_name TEXT NOT NULL DEFAULT '', charter TEXT NOT NULL DEFAULT '', heartland TEXT NOT NULL DEFAULT '', government TEXT NOT NULL DEFAULT '', level INTEGER NOT NULL DEFAULT 1, xp INTEGER NOT NULL DEFAULT 0, size INTEGER NOT NULL DEFAULT 1, culture_score INTEGER NOT NULL DEFAULT 10, economy_score INTEGER NOT NULL DEFAULT 10, loyalty_score INTEGER NOT NULL DEFAULT 10, stability_score INTEGER NOT NULL DEFAULT 10, fame INTEGER NOT NULL DEFAULT 0, infamy INTEGER NOT NULL DEFAULT 0, unrest INTEGER NOT NULL DEFAULT 0, corruption INTEGER NOT NULL DEFAULT 0, crime INTEGER NOT NULL DEFAULT 0, decay INTEGER NOT NULL DEFAULT 0, strife INTEGER NOT NULL DEFAULT 0, ruin_threshold INTEGER NOT NULL DEFAULT 10, resource_points INTEGER NOT NULL DEFAULT 0, resource_die_count INTEGER NOT NULL DEFAULT 1, resource_die_size INTEGER NOT NULL DEFAULT 4, food INTEGER NOT NULL DEFAULT 0, lumber INTEGER NOT NULL DEFAULT 0, luxuries INTEGER NOT NULL DEFAULT 0, ore INTEGER NOT NULL DEFAULT 0, stone INTEGER NOT NULL DEFAULT 0, consumption INTEGER NOT NULL DEFAULT 0, turn_number INTEGER NOT NULL DEFAULT 0, turn_phase TEXT NOT NULL DEFAULT 'COMPLETE', active_turn_id INTEGER NOT NULL DEFAULT 0, control_dc_modifier INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        ensureColumn(sql,"kingmaker_state","party_level_cap","INTEGER NOT NULL DEFAULT 1");
        ensureColumn(sql,"kingmaker_state","reputation_mode","TEXT NOT NULL DEFAULT 'fame'");
        ensureColumn(sql,"kingmaker_state","consumption_modifier","INTEGER NOT NULL DEFAULT 0");
        for(String ruin:new String[]{"corruption","crime","decay","strife"}){ensureColumn(sql,"kingmaker_state",ruin+"_threshold","INTEGER NOT NULL DEFAULT 10");ensureColumn(sql,"kingmaker_state",ruin+"_penalty","INTEGER NOT NULL DEFAULT 0");}
        ensureColumn(sql,"kingmaker_state","event_dc","INTEGER NOT NULL DEFAULT 16");
        ensureColumn(sql,"kingmaker_state","anarchy","INTEGER NOT NULL DEFAULT 0");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_skills (world_profile TEXT NOT NULL, skill_key TEXT NOT NULL, skill_name TEXT NOT NULL, ability_key TEXT NOT NULL, rank TEXT NOT NULL DEFAULT 'untrained', item_bonus INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', updated_at INTEGER NOT NULL, PRIMARY KEY(world_profile,skill_key))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_leaders (world_profile TEXT NOT NULL, role_key TEXT NOT NULL, role_name TEXT NOT NULL, entity_kind TEXT NOT NULL DEFAULT '', entity_id INTEGER NOT NULL DEFAULT 0, leader_name TEXT NOT NULL DEFAULT '', invested INTEGER NOT NULL DEFAULT 0, vacant INTEGER NOT NULL DEFAULT 1, vacancy_penalty INTEGER NOT NULL DEFAULT 1, notes TEXT NOT NULL DEFAULT '', updated_at INTEGER NOT NULL, PRIMARY KEY(world_profile,role_key))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_features (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, feature_key TEXT NOT NULL, feature_kind TEXT NOT NULL DEFAULT 'kingdom_feat', name TEXT NOT NULL, level INTEGER NOT NULL DEFAULT 1, status TEXT NOT NULL DEFAULT 'active', effects_json TEXT NOT NULL DEFAULT '{}', source_ref TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,feature_key))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_hexes (world_profile TEXT NOT NULL, map_id TEXT NOT NULL, map_package_id TEXT NOT NULL DEFAULT '', hex_id TEXT NOT NULL, reconnoitered INTEGER NOT NULL DEFAULT 0, cleared INTEGER NOT NULL DEFAULT 0, claimed INTEGER NOT NULL DEFAULT 0, terrain TEXT NOT NULL DEFAULT '', improvement TEXT NOT NULL DEFAULT '', road INTEGER NOT NULL DEFAULT 0, farmland INTEGER NOT NULL DEFAULT 0, resource_site TEXT NOT NULL DEFAULT '', claimed_turn INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', updated_at INTEGER NOT NULL, PRIMARY KEY(world_profile,map_id,hex_id))");
        ensureColumn(sql,"kingmaker_hexes","landmark","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_hexes","refuge","INTEGER NOT NULL DEFAULT 0");
        sql.execSQL("CREATE INDEX IF NOT EXISTS idx_kingmaker_hexes ON kingmaker_hexes(world_profile,claimed,reconnoitered,hex_id)");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_settlements (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, settlement_key TEXT NOT NULL, poi_id TEXT NOT NULL DEFAULT '', name TEXT NOT NULL, type TEXT NOT NULL DEFAULT 'village', level INTEGER NOT NULL DEFAULT 1, hex_id TEXT NOT NULL, influence INTEGER NOT NULL DEFAULT 1, population INTEGER NOT NULL DEFAULT 0, consumption INTEGER NOT NULL DEFAULT 0, grid_json TEXT NOT NULL DEFAULT '{}', storage_json TEXT NOT NULL DEFAULT '{}', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,settlement_key))");
        ensureColumn(sql,"kingmaker_settlements","developed_blocks","INTEGER NOT NULL DEFAULT 1");
        ensureColumn(sql,"kingmaker_settlements","occupied_lots","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_settlements","residential_lots","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_settlements","overcrowded","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_settlements","max_item_bonus","INTEGER NOT NULL DEFAULT 1");
        sql.execSQL("CREATE INDEX IF NOT EXISTS idx_kingmaker_settlements ON kingmaker_settlements(world_profile,hex_id,name)");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_structures (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, settlement_key TEXT NOT NULL, structure_key TEXT NOT NULL, name TEXT NOT NULL, count INTEGER NOT NULL DEFAULT 1, lots INTEGER NOT NULL DEFAULT 1, status TEXT NOT NULL DEFAULT 'built', construction_rp INTEGER NOT NULL DEFAULT 0, commodity_costs_json TEXT NOT NULL DEFAULT '{}', effects_json TEXT NOT NULL DEFAULT '{}', source_ref TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,settlement_key,structure_key))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_turns (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, turn_number INTEGER NOT NULL, phase TEXT NOT NULL DEFAULT 'UPKEEP', status TEXT NOT NULL DEFAULT 'active', resource_roll_json TEXT NOT NULL DEFAULT '{}', snapshot_before_json TEXT NOT NULL DEFAULT '{}', snapshot_after_json TEXT NOT NULL DEFAULT '{}', started_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0, UNIQUE(world_profile,turn_number))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_turn_activities (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, turn_id INTEGER NOT NULL, phase TEXT NOT NULL, activity_key TEXT NOT NULL, title TEXT NOT NULL, actor_role TEXT NOT NULL DEFAULT '', skill_key TEXT NOT NULL DEFAULT '', natural_roll INTEGER NOT NULL DEFAULT 0, modifier INTEGER NOT NULL DEFAULT 0, dc INTEGER NOT NULL DEFAULT 0, degree TEXT NOT NULL DEFAULT '', costs_json TEXT NOT NULL DEFAULT '{}', effects_json TEXT NOT NULL DEFAULT '{}', source_ref TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, FOREIGN KEY(turn_id) REFERENCES kingmaker_turns(id) ON DELETE CASCADE)");
        sql.execSQL("CREATE INDEX IF NOT EXISTS idx_kingmaker_activities ON kingmaker_turn_activities(world_profile,turn_id,phase,id)");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_events (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, turn_id INTEGER NOT NULL DEFAULT 0, event_key TEXT NOT NULL, title TEXT NOT NULL, event_type TEXT NOT NULL DEFAULT 'kingdom', status TEXT NOT NULL DEFAULT 'pending', skill_key TEXT NOT NULL DEFAULT '', dc INTEGER NOT NULL DEFAULT 0, resolution_json TEXT NOT NULL DEFAULT '{}', source_ref TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, resolved_at INTEGER NOT NULL DEFAULT 0, UNIQUE(world_profile,event_key))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_armies (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, army_key TEXT NOT NULL, name TEXT NOT NULL, army_type TEXT NOT NULL DEFAULT 'infantry', allegiance TEXT NOT NULL DEFAULT 'kingdom', level INTEGER NOT NULL DEFAULT 1, size INTEGER NOT NULL DEFAULT 1, ac INTEGER NOT NULL DEFAULT 15, maneuver_modifier INTEGER NOT NULL DEFAULT 0, maneuver_dc INTEGER NOT NULL DEFAULT 15, morale_modifier INTEGER NOT NULL DEFAULT 0, morale_dc INTEGER NOT NULL DEFAULT 15, attack_modifier INTEGER NOT NULL DEFAULT 0, hp INTEGER NOT NULL DEFAULT 4, max_hp INTEGER NOT NULL DEFAULT 4, rout_threshold INTEGER NOT NULL DEFAULT 1, ammunition INTEGER NOT NULL DEFAULT 0, supplies INTEGER NOT NULL DEFAULT 0, commander_name TEXT NOT NULL DEFAULT '', commander_id INTEGER NOT NULL DEFAULT 0, location_hex TEXT NOT NULL DEFAULT '', conditions_json TEXT NOT NULL DEFAULT '[]', tactics_json TEXT NOT NULL DEFAULT '[]', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,army_key))");
        ensureColumn(sql,"kingmaker_armies","consumption","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_armies","scouting","INTEGER NOT NULL DEFAULT 0");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_war_encounters (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, encounter_key TEXT NOT NULL, name TEXT NOT NULL, round_number INTEGER NOT NULL DEFAULT 1, active_army_key TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'active', battlefield_hex TEXT NOT NULL DEFAULT '', participants_json TEXT NOT NULL DEFAULT '[]', log_json TEXT NOT NULL DEFAULT '[]', started_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,encounter_key))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_companions (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, companion_key TEXT NOT NULL, npc_id INTEGER NOT NULL DEFAULT 0, name TEXT NOT NULL, influence INTEGER NOT NULL DEFAULT 0, attitude TEXT NOT NULL DEFAULT 'indifferent', assignment TEXT NOT NULL DEFAULT 'reserve', leadership_role TEXT NOT NULL DEFAULT '', available INTEGER NOT NULL DEFAULT 1, personal_quest_status TEXT NOT NULL DEFAULT 'unknown', personal_quest_notes TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,companion_key))");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_camps (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, camp_key TEXT NOT NULL, hex_id TEXT NOT NULL DEFAULT '', day_number INTEGER NOT NULL DEFAULT 1, status TEXT NOT NULL DEFAULT 'active', safety INTEGER NOT NULL DEFAULT 50, comfort INTEGER NOT NULL DEFAULT 50, meals INTEGER NOT NULL DEFAULT 0, watch_json TEXT NOT NULL DEFAULT '[]', activities_json TEXT NOT NULL DEFAULT '[]', weather TEXT NOT NULL DEFAULT '', encounter_json TEXT NOT NULL DEFAULT '{}', result_json TEXT NOT NULL DEFAULT '{}', notes TEXT NOT NULL DEFAULT '', started_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0, UNIQUE(world_profile,camp_key))");
        ensureColumn(sql,"kingmaker_camps","zone_name","TEXT NOT NULL DEFAULT ''");
        ensureColumn(sql,"kingmaker_camps","zone_dc","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_camps","encounter_dc","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_camps","current_encounter_dc","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_camps","phase","TEXT NOT NULL DEFAULT 'PREPARE_CAMP'");
        ensureColumn(sql,"kingmaker_camps","prepare_degree","TEXT NOT NULL DEFAULT ''");
        ensureColumn(sql,"kingmaker_camps","reused","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_camps","elapsed_hours","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_camps","special_ingredients","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_camps","encounter_checks_json","TEXT NOT NULL DEFAULT '[]'");
        ensureColumn(sql,"kingmaker_companions","unlocked_rewards_json","TEXT NOT NULL DEFAULT '[]'");
        ensureColumn(sql,"kingmaker_camps","step","INTEGER NOT NULL DEFAULT 1");
        ensureColumn(sql,"kingmaker_camps","terrain","TEXT NOT NULL DEFAULT ''");
        ensureColumn(sql,"kingmaker_camps","basic_ingredients","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(sql,"kingmaker_camps","meal_json","TEXT NOT NULL DEFAULT '{}'");
        ensureColumn(sql,"kingmaker_camps","buffs_json","TEXT NOT NULL DEFAULT '[]'");
        ensureColumn(sql,"kingmaker_camps","rest_json","TEXT NOT NULL DEFAULT '{}'");
        sql.execSQL("CREATE TABLE IF NOT EXISTS kingmaker_trade_agreements (id INTEGER PRIMARY KEY AUTOINCREMENT, world_profile TEXT NOT NULL, agreement_key TEXT NOT NULL, origin_settlement_key TEXT NOT NULL DEFAULT '', destination_name TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'proposed', route_json TEXT NOT NULL DEFAULT '[]', commodities_json TEXT NOT NULL DEFAULT '{}', benefit_json TEXT NOT NULL DEFAULT '{}', source_ref TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(world_profile,agreement_key))");
        KingmakerCampaignImporter.ensureSchema(sql);
    }

    private void ensureProfile(){
        SQLiteDatabase sql=db.getWritableDatabase();long now=System.currentTimeMillis();ContentValues state=new ContentValues();state.put("world_profile",profile());state.put("created_at",now);state.put("updated_at",now);sql.insertWithOnConflict("kingmaker_state",null,state,SQLiteDatabase.CONFLICT_IGNORE);
        JSONArray catalog=rules.skillCatalog();for(int i=0;i<catalog.length();i++){JSONObject skill=catalog.optJSONObject(i);if(skill==null)continue;ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("skill_key",skill.optString("key"));v.put("skill_name",skill.optString("name"));v.put("ability_key",skill.optString("ability"));v.put("updated_at",now);sql.insertWithOnConflict("kingmaker_skills",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
        for(String role:KingmakerRulesEngine.LEADERSHIP_ROLES){ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("role_key",key(role));v.put("role_name",role);v.put("updated_at",now);sql.insertWithOnConflict("kingmaker_leaders",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
        ensureDefaultCompanions();
    }

    public JSONObject state(){ensureProfile();return db.queryOne("SELECT * FROM kingmaker_state WHERE world_profile=?",new String[]{profile()});}
    public boolean enabled(){return state().optInt("enabled",0)==1;}

    public JSONObject configure(JSONObject spec)throws Exception{
        JSONObject before=state(),source=spec==null?new JSONObject():spec;ContentValues v=new ContentValues();
        for(String field:new String[]{"kingdom_name","capital_name","charter","heartland","government","campaign_mode","reputation_mode","notes"})if(source.has(field))v.put(field,clean(source.optString(field)));
        for(String field:new String[]{"level","party_level_cap","xp","size","culture_score","economy_score","loyalty_score","stability_score","fame","infamy","unrest","corruption","crime","decay","strife","ruin_threshold","corruption_threshold","crime_threshold","decay_threshold","strife_threshold","corruption_penalty","crime_penalty","decay_penalty","strife_penalty","resource_points","food","lumber","luxuries","ore","stone","consumption","consumption_modifier","control_dc_modifier"})if(source.has(field))v.put(field,boundedStateValue(field,source.optInt(field)));
        if(source.has("party_level_cap")){int nextLevel=source.has("level")?boundedStateValue("level",source.optInt("level")):before.optInt("level",1);v.put("party_level_cap",Math.max(nextLevel,boundedStateValue("party_level_cap",source.optInt("party_level_cap"))));}
        if(source.has("enabled"))v.put("enabled",source.optBoolean("enabled")||source.optInt("enabled",0)==1?1:0);
        v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});syncResourceDice();JSONObject after=state();audit("Reino configurado",after.optString("kingdom_name","Reino sem nome"),before,after);return after;
    }

    public JSONObject adjustState(JSONObject deltas,String reason)throws Exception{
        JSONObject before=state(),delta=deltas==null?new JSONObject():deltas,next=new JSONObject(before.toString());
        for(String field:new String[]{"resource_points","food","lumber","luxuries","ore","stone","fame","infamy","unrest"})if(delta.has(field))next.put(field,Math.max(0,before.optInt(field)+delta.optInt(field)));
        if(delta.has("consumption_modifier"))next.put("consumption_modifier",Math.max(-1000,Math.min(1000,before.optInt("consumption_modifier")+delta.optInt("consumption_modifier"))));
        if(delta.has("xp"))next.put("xp",Math.max(0,before.optInt("xp")+delta.optInt("xp")));
        for(String ruin:new String[]{"corruption","crime","decay","strife"})if(delta.has(ruin)){int points=Math.max(0,before.optInt(ruin)+delta.optInt(ruin)),threshold=Math.max(1,before.optInt(ruin+"_threshold",before.optInt("ruin_threshold",10))),penalty=Math.max(0,before.optInt(ruin+"_penalty"));while(points>=threshold){points-=threshold;penalty++;}next.put(ruin,points).put(ruin+"_penalty",penalty);}
        if(delta.has("size"))next.put("size",Math.max(1,before.optInt("size",1)+delta.optInt("size")));
        configure(next);int spent=Math.max(0,before.optInt("resource_points")-next.optInt("resource_points"));if(spent>0)recordRpSpent(spent);JSONObject after=state();audit("Recursos do Reino ajustados",clean(reason),before,after);return after;
    }

    public List<JSONObject> skills(){return db.queryList("SELECT * FROM kingmaker_skills WHERE world_profile=? ORDER BY skill_name COLLATE NOCASE",new String[]{profile()});}
    public JSONObject skill(String key){return db.queryOne("SELECT * FROM kingmaker_skills WHERE world_profile=? AND skill_key=?",new String[]{profile(),key(key)});}
    public void setSkill(String skillKey,String rank,int itemBonus,String notes){if(!rules.validSkill(skillKey))throw new IllegalArgumentException("Perícia de Reino inválida.");String nextRank=rank(rank);int level=state().optInt("level",1);if(("expert".equals(nextRank)&&level<3)||("master".equals(nextRank)&&level<7)||("legendary".equals(nextRank)&&level<15))throw new IllegalArgumentException("A graduação escolhida ainda não está disponível no nível do Reino.");if(level<3&&"trained".equals(nextRank)&&!"trained".equals(skill(skillKey).optString("rank"))){int trained=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_skills WHERE world_profile=? AND rank!='untrained'",new String[]{profile()}).optInt("total");if(trained>=6)throw new IllegalStateException("No 1º nível o Reino pode iniciar treinado em no máximo seis perícias.");}ContentValues v=new ContentValues();v.put("rank",nextRank);v.put("item_bonus",Math.max(-10,Math.min(20,itemBonus)));v.put("notes",clean(notes));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_skills",v,"world_profile=? AND skill_key=?",new String[]{profile(),key(skillKey)});}
    public List<JSONObject> leaders(){return db.queryList("SELECT * FROM kingmaker_leaders WHERE world_profile=? ORDER BY role_name",new String[]{profile()});}
    public int vacantLeadershipRoles(){JSONObject x=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_leaders WHERE world_profile=? AND vacant=1",new String[]{profile()});return x.optInt("total",0);}
    public void assignLeader(String role,String kind,long entityId,String name,boolean invested,String notes){if(!rules.validLeadershipRole(role))throw new IllegalArgumentException("Cargo de liderança inválido.");String roleKey=key(role),leader=clean(name),entityKind=clean(kind);long id=leader.length()==0?0:Math.max(0,entityId);JSONObject old=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),roleKey});if(id>0){JSONObject duplicate=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key!=? AND entity_kind=? AND entity_id=? AND vacant=0",new String[]{profile(),roleKey,entityKind,String.valueOf(id)});if(duplicate.length()>0)throw new IllegalStateException("A mesma pessoa só pode ocupar um cargo de liderança.");}if(leader.length()>0&&invested&&old.optInt("invested")==0){int count=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_leaders WHERE world_profile=? AND invested=1",new String[]{profile()}).optInt("total");if(count>=4)throw new IllegalStateException("Escolha no máximo quatro cargos investidos.");}
        if("companion".equals(entityKind)||leader.length()>0){
            JSONObject comp=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND (name=? OR companion_key=?)",new String[]{profile(),leader,leader});
            if(comp.length()>0&&comp.optInt("available",1)!=1){
                throw new IllegalStateException("O companheiro "+comp.optString("name")+" está indisponível para assumir cargo de liderança.");
            }
        }
        ContentValues v=new ContentValues();v.put("entity_kind",leader.length()==0?"":entityKind);v.put("entity_id",id);v.put("leader_name",leader);v.put("invested",leader.length()>0&&invested?1:0);v.put("vacant",leader.length()==0?1:0);v.put("notes",clean(notes));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_leaders",v,"world_profile=? AND role_key=?",new String[]{profile(),roleKey});if(vacantLeadershipRoles()==0)grantMilestoneOnce("all_leadership_roles",60,"Todos os oito cargos preenchidos");audit("Liderança do Reino atualizada",role+": "+(leader.length()==0?"vago":leader),old,db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),roleKey}));}

    public JSONObject upsertFeature(String featureKey,String kind,String name,int level,String effectsJson,String sourceRef,String notes)throws Exception{String featureName=clean(name);if(featureName.length()==0)throw new IllegalArgumentException("Nome do traço ou talento ausente.");String featureKind=key(kind).length()==0?"kingdom_feat":key(kind),k=clean(featureKey);if(k.length()==0)k=featureKind+":"+key(featureName);ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("feature_kind",featureKind);v.put("name",featureName);v.put("level",Math.max(1,Math.min(20,level)));v.put("effects_json",jsonObject(effectsJson).toString());v.put("source_ref",clean(sourceRef));v.put("notes",clean(notes));v.put("updated_at",now);JSONObject existing=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),k});if(existing.length()==0){v.put("world_profile",profile());v.put("feature_key",k);v.put("created_at",now);db.getWritableDatabase().insertOrThrow("kingmaker_features",null,v);}else db.getWritableDatabase().update("kingmaker_features",v,"world_profile=? AND feature_key=?",new String[]{profile(),k});return db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),k});}
    public void setFeatureStatus(String featureKey,String status){String next=key(status),featureKeyClean=clean(featureKey);if(!"active".equals(next)&&!"inactive".equals(next)&&!"completed".equals(next))throw new IllegalArgumentException("Estado de traço inválido.");ContentValues v=new ContentValues();v.put("status",next);v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_features",v,"world_profile=? AND feature_key=?",new String[]{profile(),featureKeyClean});JSONObject feature=db.queryOne("SELECT feature_kind FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),featureKeyClean});if("active".equals(next)&&"diplomatic_relation".equals(feature.optString("feature_kind")))grantMilestoneOnce("first_diplomatic_relation",60,"Primeiras relações diplomáticas");}
    public List<JSONObject> features(String kind){String filter=key(kind);return db.queryList("SELECT * FROM kingmaker_features WHERE world_profile=? AND (?='' OR feature_kind=?) ORDER BY level,name",new String[]{profile(),filter,filter});}

    public JSONObject controlSummary(){
        JSONObject s=state(),out=new JSONObject();
        try{
            int vacancies=vacantLeadershipRoles(),rulerVacant=roleVacant("ruler")?1:0;
            int unrest=s.optInt("unrest",0);
            boolean anarchy=rules.isAnarchy(unrest)||s.optInt("anarchy",0)==1;
            out.put("control_dc",rules.controlDc(s.optInt("level",1),s.optInt("size",1),unrest,rulerVacant,s.optInt("control_dc_modifier")))
               .put("vacant_roles",vacancies).put("ruler_vacant",rulerVacant==1).put("size_category",rules.sizeCategory(s.optInt("size",1)))
               .put("size_control_modifier",rules.sizeControlModifier(s.optInt("size",1))).put("commodity_storage",rules.commodityStorage(s.optInt("size",1)))
               .put("unrest_penalty",rules.unrestPenalty(unrest)).put("anarchy",anarchy).put("event_dc",s.optInt("event_dc",16))
               .put("invested_leadership_bonus",rules.investedLeadershipBonus(s.optInt("level",1)));
            JSONObject ruin=new JSONObject(),thresholds=new JSONObject();
            for(String field:new String[]{"corruption","crime","decay","strife"}){ruin.put(field,s.optInt(field+"_penalty"));thresholds.put(field,s.optInt(field+"_threshold",10));}
            out.put("ruin_penalties",ruin).put("ruin_thresholds",thresholds).put("vacancy_penalties",vacancySummary());
        }catch(Exception ignored){}
        return out;
    }

    public JSONObject startTurn(JSONArray resourceRolls)throws Exception{return startTurn(resourceRolls,0);}
    public JSONObject startTurn(JSONArray resourceRolls,int rulerVacancyUnrest)throws Exception{
        requireEnabled();JSONObject s=state();if(s.optLong("active_turn_id",0)>0){JSONObject active=activeTurn();if(active.length()>0&&"active".equals(active.optString("status")))return active;}final boolean rulerVacant=roleVacant("ruler");if(rulerVacant&&(rulerVacancyUnrest<1||rulerVacancyUnrest>4))throw new IllegalArgumentException("Com o cargo de Governante vago, informe o resultado de 1d4 Unrest.");
        final JSONObject[] result={new JSONObject()};final int vacancyUnrest=rulerVacant?rulerVacancyUnrest:0;db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{JSONObject before=state();int number=before.optInt("turn_number",0)+1;JSONObject dice=rules.resourceDice(before.optInt("level",1),before.optInt("size",1)),consumption=consumptionSummary();JSONArray rolls=resourceRolls==null?new JSONArray():resourceRolls;int total=0,count=dice.optInt("count",5),die=dice.optInt("die",4),overcrowded=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_settlements WHERE world_profile=? AND overcrowded=1",new String[]{profile()}).optInt("total",0);if(rolls.length()!=count)throw new IllegalArgumentException("O turno exige exatamente "+count+" resultados de d"+die+".");for(int i=0;i<rolls.length();i++){int roll=rolls.optInt(i,0);if(roll<1||roll>die)throw new IllegalArgumentException("Resultado de dado de recurso fora da faixa.");total+=roll;}JSONObject resource=new JSONObject().put("formula",dice.optString("formula")).put("rolls",rolls).put("total",total).put("entered",true).put("ruler_vacancy_unrest",vacancyUnrest).put("overcrowded_unrest",overcrowded).put("consumption",consumption).put("consumption_resolved",consumption.optInt("total")==0).put("spent_rp",0);ContentValues turn=new ContentValues();turn.put("world_profile",profile());turn.put("turn_number",number);turn.put("phase","UPKEEP");turn.put("status","active");turn.put("resource_roll_json",resource.toString());turn.put("snapshot_before_json",before.toString());turn.put("started_at",System.currentTimeMillis());long id=db.getWritableDatabase().insertOrThrow("kingmaker_turns",null,turn);ContentValues state=new ContentValues();state.put("turn_number",number);state.put("turn_phase","UPKEEP");state.put("active_turn_id",id);state.put("resource_points",total);state.put("consumption",consumption.optInt("total"));int nextUnrest=before.optInt("unrest")+vacancyUnrest+overcrowded;state.put("unrest",nextUnrest);state.put("anarchy",rules.isAnarchy(nextUnrest)?1:0);state.put("fame",before.optInt("fame",0));state.put("infamy",before.optInt("infamy",0));state.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",state,"world_profile=?",new String[]{profile()});result[0]=activeTurn();}});audit("Turno do Reino iniciado","Turno "+result[0].optInt("turn_number"),new JSONObject(),result[0]);return result[0];
    }

    public JSONObject consumptionSummary(){JSONObject out=new JSONObject();try{int settlements=db.queryOne("SELECT COALESCE(SUM(consumption),0) AS total FROM kingmaker_settlements WHERE world_profile=?",new String[]{profile()}).optInt("total",0),army=db.queryOne("SELECT COALESCE(SUM(consumption),0) AS total FROM kingmaker_armies WHERE world_profile=?",new String[]{profile()}).optInt("total",0),farmland=influencedFarmlandCount(),modifier=state().optInt("consumption_modifier",0),total=Math.max(0,settlements+army-farmland+modifier);out.put("settlements",settlements).put("armies",army).put("influenced_farmland",farmland).put("modifier",modifier).put("total",total);}catch(Exception e){try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){}}return out;}

    public JSONObject resolveConsumption(String payment,int unrestRoll)throws Exception{requireEnabled();JSONObject turn=activeTurn();if(turn.length()==0||!"UPKEEP".equals(turn.optString("phase")))throw new IllegalStateException("O Consumo só pode ser resolvido na fase de Manutenção.");JSONObject resource=jsonObject(turn.optString("resource_roll_json","{}"));if(resource.optBoolean("consumption_resolved"))throw new IllegalStateException("O Consumo deste turno já foi resolvido.");JSONObject amount=resource.optJSONObject("consumption");int total=amount==null?state().optInt("consumption",0):amount.optInt("total",0);String method=key(payment);if(total==0)method="none";JSONObject before=state();ContentValues stateValues=new ContentValues();int rpSpent=0;if("food".equals(method)){if(before.optInt("food")<total)throw new IllegalStateException("Alimento insuficiente para pagar o Consumo.");stateValues.put("food",before.optInt("food")-total);}else if("rp".equals(method)){rpSpent=total*5;if(before.optInt("resource_points")<rpSpent)throw new IllegalStateException("RP insuficiente: são necessários "+rpSpent+" RP.");stateValues.put("resource_points",before.optInt("resource_points")-rpSpent);}else if("unrest".equals(method)){if(unrestRoll<1||unrestRoll>4)throw new IllegalArgumentException("Informe o resultado de 1d4 Unrest.");stateValues.put("unrest",before.optInt("unrest")+unrestRoll);}else if(!"none".equals(method))throw new IllegalArgumentException("Forma de pagamento do Consumo inválida.");stateValues.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",stateValues,"world_profile=?",new String[]{profile()});int spent=resource.optInt("spent_rp",0)+rpSpent;resource.put("consumption_resolved",true).put("consumption_payment",method).put("consumption_unrest",("unrest".equals(method)?unrestRoll:0)).put("spent_rp",spent);ContentValues turnValues=new ContentValues();turnValues.put("resource_roll_json",resource.toString());db.getWritableDatabase().update("kingmaker_turns",turnValues,"id=? AND world_profile=?",new String[]{String.valueOf(turn.optLong("id")),profile()});if(spent>=100)grantMilestoneOnce("spent_100_rp_turn",80,"100 RP gastos em um turno");audit("Consumo do Reino resolvido",method+" • "+total,before,state());return resource;}

    public JSONObject activeTurn(){JSONObject s=state();long id=s.optLong("active_turn_id",0);return id<=0?new JSONObject():db.queryOne("SELECT * FROM kingmaker_turns WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});}
    public JSONObject advancePhase()throws Exception{
        requireEnabled();JSONObject turn=activeTurn();if(turn.length()==0||!"active".equals(turn.optString("status")))throw new IllegalStateException("Não há turno de Reino ativo.");if("UPKEEP".equals(turn.optString("phase"))&&!jsonObject(turn.optString("resource_roll_json","{}")).optBoolean("consumption_resolved"))throw new IllegalStateException("Resolva o Consumo antes de concluir a Manutenção.");String next=rules.nextPhase(turn.optString("phase"));if("COMPLETE".equals(next))return completeTurn();ContentValues t=new ContentValues();t.put("phase",next);db.getWritableDatabase().update("kingmaker_turns",t,"id=? AND world_profile=?",new String[]{String.valueOf(turn.optLong("id")),profile()});ContentValues s=new ContentValues();s.put("turn_phase",next);s.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",s,"world_profile=?",new String[]{profile()});audit("Fase do Reino avançada",next,turn,activeTurn());return activeTurn();
    }

    public JSONObject completeTurn()throws Exception{
        final JSONObject turn=activeTurn();if(turn.length()==0)throw new IllegalStateException("Não há turno ativo.");final JSONObject[] out={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{JSONObject before=state();int surplus=Math.min(120,Math.max(0,before.optInt("resource_points"))),xp=before.optInt("xp")+surplus,level=before.optInt("level",1),cap=Math.max(1,before.optInt("party_level_cap",1));boolean gained=level<20&&level<cap&&xp>=1000;if(gained){xp-=1000;level++;db.getWritableDatabase().execSQL("UPDATE kingmaker_armies SET level=CASE WHEN level<20 THEN level+1 ELSE 20 END, updated_at=? WHERE world_profile=?",new Object[]{System.currentTimeMillis(),profile()});}ContentValues s=new ContentValues();s.put("turn_phase","COMPLETE");s.put("active_turn_id",0);s.put("resource_points",0);s.put("xp",xp);s.put("level",level);s.put("fame",0);s.put("infamy",0);JSONObject dice=rules.resourceDice(level,before.optInt("size",1));s.put("resource_die_count",dice.optInt("count"));s.put("resource_die_size",dice.optInt("die"));s.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",s,"world_profile=?",new String[]{profile()});JSONObject after=state();ContentValues t=new ContentValues();t.put("phase","COMPLETE");t.put("status","complete");t.put("snapshot_after_json",after.toString());t.put("completed_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_turns",t,"id=? AND world_profile=?",new String[]{String.valueOf(turn.optLong("id")),profile()});out[0]=db.queryOne("SELECT * FROM kingmaker_turns WHERE id=? AND world_profile=?",new String[]{String.valueOf(turn.optLong("id")),profile()});out[0].put("surplus_rp_xp",surplus).put("level_gained",gained).put("kingdom_level",level);}});audit("Turno do Reino concluído","Turno "+turn.optInt("turn_number"),turn,out[0]);return out[0];
    }

    public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes)throws Exception{
        return recordCheck(activityKey,title,role,skillKey,natural,dc,sourceRef,notes,false);
    }

    public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)throws Exception{
        requireEnabled();JSONObject turn=activeTurn();if(turn.length()==0)throw new IllegalStateException("Inicie um turno de Reino antes de registrar uma atividade.");JSONObject s=state(),skill=skill(skillKey);if(skill.length()==0)throw new IllegalArgumentException("Perícia de Reino inválida.");String ability=skill.optString("ability_key"),activity=key(activityKey);
        if((rules.isAnarchy(s.optInt("unrest"))||s.optInt("anarchy",0)==1)&&!rules.isRestorativeActivity(activity))
            throw new IllegalStateException("O Reino está em Anarquia (Unrest >= 20). Nenhuma atividade de Reino pode ser realizada, exceto Acalmar Agitação e atividades restauradoras.");
        int score=s.optInt(ability+"_score",10),leadership=investedAbilityBonus(ability),vacancy=vacancyPenalty(ability,activity),ruin=s.optInt(ruinForAbility(ability)+"_penalty"),unrest=rules.unrestPenalty(s.optInt("unrest"));int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin,s.optInt("unrest"));int resolvedDc=dc>0?dc:controlSummary().optInt("control_dc");JSONObject result=rules.resolveKingdomCheck(skillKey,natural,modifier,resolvedDc);result.put("modifier_breakdown",new JSONObject().put("ability",rules.abilityModifier(score)).put("proficiency",new Pathfinder2eRulesEngine().proficiency(s.optInt("level",1),skill.optString("rank"))).put("item",skill.optInt("item_bonus")).put("invested_leadership",leadership).put("vacancy",vacancy).put("ruin",-ruin).put("unrest",-unrest));String activityTitle=clean(title).length()==0?KingmakerRulesEngine.skillLabel(skillKey):clean(title);ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("turn_id",turn.optLong("id"));v.put("phase",turn.optString("phase"));v.put("activity_key",activity.length()==0?"kingdom_check":activity);v.put("title",activityTitle);v.put("actor_role",clean(role));v.put("skill_key",key(skillKey));v.put("natural_roll",result.optInt("natural_roll"));v.put("modifier",modifier);v.put("dc",resolvedDc);v.put("degree",result.optString("degree"));v.put("effects_json",result.toString());v.put("source_ref",clean(sourceRef));v.put("notes",clean(notes));v.put("created_at",System.currentTimeMillis());long id=db.getWritableDatabase().insertOrThrow("kingmaker_turn_activities",null,v);result.put("activity_id",id).put("turn_id",turn.optLong("id"));if(awardReputationOnCrit&&"CRITICAL_SUCCESS".equals(result.optString("degree")))gainReputationPoint();audit("Teste de Reino resolvido",activityTitle+" — "+result.optString("degree_pt"),new JSONObject(),result);return result;
    }

    public List<JSONObject> turnActivities(long turnId){return db.queryList("SELECT * FROM kingmaker_turn_activities WHERE world_profile=? AND turn_id=? ORDER BY id",new String[]{profile(),String.valueOf(turnId)});}
    public List<JSONObject> turns(int limit){return db.queryList("SELECT * FROM kingmaker_turns WHERE world_profile=? ORDER BY turn_number DESC LIMIT "+Math.max(1,Math.min(100,limit)),new String[]{profile()});}

    public JSONObject reconnoiterHex(String hexId,String notes)throws Exception{return setHex(hexId,true,null,null,notes);}
    public JSONObject clearHex(String hexId,String notes)throws Exception{JSONObject current=hex(hexId);if(current.length()==0||current.optInt("reconnoitered",0)!=1)throw new IllegalStateException("O hex precisa ser reconnoiterado antes de ser limpo.");return setHex(hexId,null,true,null,notes);}
    public JSONObject initializeFoundingHex(String hexId,String notes)throws Exception{requireEnabled();if(claimedHexCount()>0)throw new IllegalStateException("O hex fundador só pode ser definido antes da primeira reivindicação.");JSONObject out=setHex(hexId,true,true,true,notes);ContentValues v=new ContentValues();v.put("size",1);JSONObject dice=rules.resourceDice(state().optInt("level",1),1);v.put("resource_die_count",dice.optInt("count"));v.put("resource_die_size",dice.optInt("die"));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});return out;}
    public JSONObject claimHex(String hexId,String notes)throws Exception{
        requireEnabled();final JSONObject current=hex(hexId);if(current.length()==0||current.optInt("reconnoitered",0)!=1||current.optInt("cleared",0)!=1)throw new IllegalStateException("O hex precisa estar reconnoiterado e limpo antes da reivindicação.");if(current.optInt("claimed",0)==1)return current;final int claimedBefore=claimedHexCount();if(claimedBefore>0&&!adjacentToClaimed(hexId))throw new IllegalStateException("O hex reivindicado precisa ser adjacente ao território atual.");JSONObject s=state();if(s.optInt("resource_points",0)<1)throw new IllegalStateException("Reivindicar o hex custa 1 RP e o Reino não possui esse recurso.");final JSONObject[] out={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{boolean firstClaim=current.optInt("claimed_turn",0)==0,foundingHex=claimedBefore==0&&state().optInt("turn_number",0)==0;setHex(current.optString("hex_id"),null,null,true,notes);JSONObject before=state();int nextSize=foundingHex?Math.max(1,before.optInt("size",1)):before.optInt("size",1)+1;ContentValues v=new ContentValues();v.put("resource_points",before.optInt("resource_points")-1);v.put("size",nextSize);if(firstClaim&&!foundingHex)v.put("xp",before.optInt("xp")+10);JSONObject dice=rules.resourceDice(before.optInt("level",1),nextSize);v.put("resource_die_count",dice.optInt("count"));v.put("resource_die_size",dice.optInt("die"));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});recordRpSpent(1);if(firstClaim&&!foundingHex){if(nextSize==10)grantMilestoneOnce("kingdom_size_10",40,"Reino alcançou Tamanho 10");else if(nextSize==25)grantMilestoneOnce("kingdom_size_25",60,"Reino alcançou Tamanho 25");else if(nextSize==50)grantMilestoneOnce("kingdom_size_50",80,"Reino alcançou Tamanho 50");else if(nextSize==100)grantMilestoneOnce("kingdom_size_100",120,"Reino alcançou Tamanho 100");}out[0]=hex(current.optString("hex_id"));if(out[0].optInt("landmark")==1)grantMilestoneOnce("first_landmark",40,"Primeiro Landmark reivindicado");if(out[0].optInt("refuge")==1)grantMilestoneOnce("first_refuge",40,"Primeiro Refuge reivindicado");}});audit("Hex reivindicado",out[0].optString("hex_id")+" • custo 1 RP",current,out[0]);return out[0];
    }
    public JSONObject relinquishHex(String hexId)throws Exception{JSONObject current=hex(hexId);if(current.length()==0||current.optInt("claimed")!=1)return current;ContentValues v=new ContentValues();v.put("claimed",0);v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_hexes",v,"world_profile=? AND map_id=? AND hex_id=?",new String[]{profile(),current.optString("map_id"),current.optString("hex_id")});JSONObject s=state();int nextSize=Math.max(1,s.optInt("size",1)-1);JSONObject dice=rules.resourceDice(s.optInt("level",1),nextSize);ContentValues change=new ContentValues();change.put("size",nextSize);change.put("resource_die_count",dice.optInt("count"));change.put("resource_die_size",dice.optInt("die"));change.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",change,"world_profile=?",new String[]{profile()});return hex(hexId);}
    public JSONObject improveHex(String hexId,String improvement,boolean road,boolean farmland,boolean landmark,boolean refuge,String resourceSite,String notes)throws Exception{JSONObject current=hex(hexId);if(current.length()==0||current.optInt("claimed")!=1)throw new IllegalStateException("Somente hex reivindicado recebe melhoria.");ContentValues v=new ContentValues();v.put("improvement",clean(improvement));v.put("road",road?1:0);v.put("farmland",farmland?1:0);v.put("landmark",landmark?1:0);v.put("refuge",refuge?1:0);v.put("resource_site",clean(resourceSite));v.put("notes",clean(notes));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_hexes",v,"world_profile=? AND map_id=? AND hex_id=?",new String[]{profile(),current.optString("map_id"),current.optString("hex_id")});if(landmark)grantMilestoneOnce("first_landmark",40,"Primeiro Landmark reivindicado");if(refuge)grantMilestoneOnce("first_refuge",40,"Primeiro Refuge reivindicado");JSONObject out=hex(hexId);audit("Melhoria territorial atualizada",hexId+" • "+clean(improvement),current,out);return out;}
    public JSONObject hex(String hexId){JSONObject map=db.getMapState();return db.queryOne("SELECT * FROM kingmaker_hexes WHERE world_profile=? AND map_id=? AND hex_id=?",new String[]{profile(),logicalMapId(map),clean(hexId).toUpperCase(Locale.US)});}
    public List<JSONObject> hexes(){return db.queryList("SELECT * FROM kingmaker_hexes WHERE world_profile=? ORDER BY claimed DESC,reconnoitered DESC,hex_id",new String[]{profile()});}
    public List<JSONObject> hexesForMap(String mapId){return db.queryList("SELECT * FROM kingmaker_hexes WHERE world_profile=? AND map_id=? ORDER BY claimed DESC,reconnoitered DESC,hex_id",new String[]{profile(),clean(mapId)});}
    public int claimedHexCount(){return db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_hexes WHERE world_profile=? AND claimed=1",new String[]{profile()}).optInt("total",0);}

    private JSONObject setHex(String hexId,Boolean reconnoitered,Boolean cleared,Boolean claimed,String notes)throws Exception{
        JSONObject map=db.getMapState();String packageId=map.optString("map_package_id"),mapId=logicalMapId(map),hex=clean(hexId).toUpperCase(Locale.US);if(packageId.length()==0)throw new IllegalStateException("Ative um Atlas antes de administrar hexes Kingmaker.");JSONObject atlas=db.getMapHex(packageId,hex);if(atlas.length()==0)throw new IllegalArgumentException("Hex "+hex+" não existe no Atlas ativo.");JSONObject old=this.hex(hex);ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("map_id",mapId);v.put("map_package_id",packageId);v.put("hex_id",hex);v.put("terrain",atlas.optString("terrain_primary"));v.put("reconnoitered",reconnoitered==null?old.optInt("reconnoitered",0):(reconnoitered?1:0));v.put("cleared",cleared==null?old.optInt("cleared",0):(cleared?1:0));v.put("claimed",claimed==null?old.optInt("claimed",0):(claimed?1:0));v.put("claimed_turn",claimed!=null&&claimed?Math.max(1,state().optInt("turn_number",0)):old.optInt("claimed_turn",0));v.put("improvement",old.optString("improvement"));v.put("road",old.optInt("road"));v.put("farmland",old.optInt("farmland"));v.put("landmark",old.optInt("landmark"));v.put("refuge",old.optInt("refuge"));v.put("resource_site",old.optString("resource_site"));v.put("notes",clean(notes).length()>0?clean(notes):old.optString("notes"));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().insertWithOnConflict("kingmaker_hexes",null,v,SQLiteDatabase.CONFLICT_REPLACE);JSONObject out=this.hex(hex);audit("Hex Kingmaker atualizado",hex,old,out);return out;
    }

    private boolean adjacentToClaimed(String target){JSONObject map=db.getMapState();Set<String> neighbors=new HashSet<String>();try{MapManifestGeometry g=MapManifestGeometry.parse(map.optString("manifest_json"));neighbors.addAll(g.neighbors(target));}catch(Exception ignored){int[] p=MapManifestGeometry.parseHexId(target);if(p!=null){int c=p[0],r=p[1];int[][] off=(c&1)==0?new int[][]{{0,-1},{0,1},{-1,-1},{-1,0},{1,-1},{1,0}}:new int[][]{{0,-1},{0,1},{-1,0},{-1,1},{1,0},{1,1}};for(int[] o:off)neighbors.add(MapManifestGeometry.excelColumn(c+o[0])+(r+o[1]+1));}}for(JSONObject row:hexes())if(row.optInt("claimed") ==1&&neighbors.contains(row.optString("hex_id")))return true;return false;}

    public JSONObject addSettlement(String name,String type,String hexId,String poiId,String notes)throws Exception{return addSettlement("",name,type,hexId,poiId,notes);}
    public JSONObject addSettlement(String requestedKey,String name,String type,String hexId,String poiId,String notes)throws Exception{
        requireEnabled();JSONObject territory=hex(hexId);if(territory.length()==0||territory.optInt("claimed")!=1||territory.optInt("cleared")!=1)throw new IllegalStateException("O assentamento exige um hex reivindicado e limpo.");if(territory.optInt("farmland")==1)throw new IllegalStateException("Um assentamento não pode ser fundado em hex de Farmland.");if(db.queryOne("SELECT id FROM kingmaker_settlements WHERE world_profile=? AND hex_id=?",new String[]{profile(),clean(hexId).toUpperCase(Locale.US)}).length()>0)throw new IllegalStateException("Este hex já possui um assentamento.");String settlementName=clean(name);if(settlementName.length()==0)throw new IllegalArgumentException("Nome do assentamento ausente.");String settlementType="village";String k=clean(requestedKey);if(k.length()==0)k="settlement_"+UUID.randomUUID().toString().replace("-","");JSONObject limits=rules.settlementLimits(settlementType);ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("world_profile",profile());v.put("settlement_key",k);v.put("poi_id",clean(poiId));v.put("name",settlementName);v.put("type",settlementType);v.put("level",1);v.put("hex_id",clean(hexId).toUpperCase(Locale.US));v.put("influence",0);v.put("population",0);v.put("consumption",limits.optInt("consumption",1));v.put("developed_blocks",1);v.put("occupied_lots",0);v.put("residential_lots",0);v.put("overcrowded",0);v.put("max_item_bonus",limits.optInt("max_item_bonus",1));v.put("notes",clean(notes));v.put("created_at",now);v.put("updated_at",now);long id=db.getWritableDatabase().insertOrThrow("kingmaker_settlements",null,v);grantSettlementMilestone(settlementType);JSONObject out=db.queryOne("SELECT * FROM kingmaker_settlements WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});audit("Assentamento fundado",settlementName+" • "+hexId,new JSONObject(),out);return out;
    }
    public void linkSettlementPoi(String settlementKey,String poiId){ContentValues v=new ContentValues();v.put("poi_id",clean(poiId));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_settlements",v,"world_profile=? AND settlement_key=?",new String[]{profile(),clean(settlementKey)});}
    public void updateSettlement(long id,String type,int level,int population,int consumption,int developedBlocks,int occupiedLots,int residentialLots,String gridJson,String storageJson,String notes)throws Exception{JSONObject old=db.queryOne("SELECT * FROM kingmaker_settlements WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});if(old.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");String settlementType=key(type),oldType=key(old.optString("type"));JSONObject limits=rules.settlementLimits(settlementType);if(state().optInt("level",1)<limits.optInt("minimum_kingdom_level",1))throw new IllegalStateException("O nível do Reino ainda não permite esse tipo de assentamento.");int oldRank=settlementRank(oldType),nextRank=settlementRank(settlementType);if(nextRank>oldRank+1)throw new IllegalStateException("A categoria do assentamento só pode avançar uma etapa por vez.");if(nextRank>oldRank&&old.optInt("overcrowded",0)==1)throw new IllegalStateException("Resolva a superlotação antes de expandir o assentamento.");if(nextRank==2&&old.optInt("occupied_lots",0)<4)throw new IllegalStateException("A aldeia precisa preencher seus quatro lotes antes de virar vila.");if(nextRank==3&&(old.optInt("developed_blocks",1)<4||old.optInt("occupied_lots",0)<8))throw new IllegalStateException("A vila precisa de quatro blocos e ao menos oito lotes ocupados antes de virar cidade.");if(nextRank==4&&(old.optInt("developed_blocks",1)<9||old.optInt("occupied_lots",0)<18))throw new IllegalStateException("A cidade precisa de nove blocos e ao menos dezoito lotes ocupados antes de virar metrópole.");int minBlocks=limits.optInt("min_blocks",1),maxBlocks=limits.optInt("max_blocks",20),blocks=Math.max(minBlocks,developedBlocks);if(maxBlocks>0)blocks=Math.min(maxBlocks,blocks);blocks=Math.min(Integer.MAX_VALUE/4,blocks);int occupied=Math.max(0,Math.min(blocks*4,occupiedLots)),residential=Math.max(0,Math.min(occupied,residentialLots)),overcrowded=occupied>0&&residential<blocks?1:0;ContentValues v=new ContentValues();v.put("type",settlementType);v.put("level",Math.max(limits.optInt("min_level",1),Math.min(limits.optInt("max_level",20),level)));v.put("population",Math.max(0,population));v.put("consumption",Math.max(0,consumption));v.put("influence",rules.settlementInfluence(settlementType));v.put("developed_blocks",blocks);v.put("occupied_lots",occupied);v.put("residential_lots",residential);v.put("overcrowded",overcrowded);v.put("max_item_bonus",limits.optInt("max_item_bonus",1));v.put("grid_json",jsonObject(gridJson).toString());v.put("storage_json",jsonObject(storageJson).toString());v.put("notes",clean(notes));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_settlements",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});if(!settlementType.equals(oldType))grantSettlementMilestone(settlementType);}
    public List<JSONObject> settlements(){return db.queryList("SELECT * FROM kingmaker_settlements WHERE world_profile=? ORDER BY name COLLATE NOCASE",new String[]{profile()});}
    public JSONObject settlement(String key){return db.queryOne("SELECT * FROM kingmaker_settlements WHERE world_profile=? AND settlement_key=?",new String[]{profile(),clean(key)});}
    public JSONObject addStructure(String settlementKey,String name,int count,int lots,int rpCost,String commoditiesJson,String effectsJson,String sourceRef,String notes)throws Exception{JSONObject town=settlement(settlementKey);if(town.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");String structureName=clean(name);if(structureName.length()==0)throw new IllegalArgumentException("Nome da estrutura ausente.");JSONObject costs=jsonObject(commoditiesJson),effects=jsonObject(effectsJson);String k=key(structureName);if(k.length()==0)k="structure_"+UUID.randomUUID().toString().replace("-","");int nextCount=Math.max(1,count),nextLots=Math.max(0,lots);JSONObject existing=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),settlementKey,k});int occupiedWithout=structureLotTotal(settlementKey)-existing.optInt("count",0)*existing.optInt("lots",0),predicted=occupiedWithout+nextCount*nextLots,capacity=Math.max(1,town.optInt("developed_blocks",1))*4;if(predicted>capacity)throw new IllegalStateException("A estrutura excede os "+capacity+" lotes disponíveis nos blocos desenvolvidos.");ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("world_profile",profile());v.put("settlement_key",settlementKey);v.put("structure_key",k);v.put("name",structureName);v.put("count",nextCount);v.put("lots",nextLots);v.put("construction_rp",Math.max(0,rpCost));v.put("commodity_costs_json",costs.toString());v.put("effects_json",effects.toString());v.put("source_ref",clean(sourceRef));v.put("notes",clean(notes));v.put("created_at",existing.length()==0?now:existing.optLong("created_at",now));v.put("updated_at",now);db.getWritableDatabase().insertWithOnConflict("kingmaker_structures",null,v,SQLiteDatabase.CONFLICT_REPLACE);refreshSettlementOccupancy(settlementKey);return db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),settlementKey,k});}
    public List<JSONObject> structures(String settlementKey){return db.queryList("SELECT * FROM kingmaker_structures WHERE world_profile=? AND (?='' OR settlement_key=?) ORDER BY settlement_key,name",new String[]{profile(),clean(settlementKey),clean(settlementKey)});}

    public JSONObject addEvent(String title,String type,String skill,int dc,String sourceRef,String notes)throws Exception{String name=clean(title);if(name.length()==0)throw new IllegalArgumentException("Título do evento ausente.");ContentValues v=new ContentValues();long now=System.currentTimeMillis();String eventKey="event_"+UUID.randomUUID().toString().replace("-","");v.put("world_profile",profile());v.put("turn_id",activeTurn().optLong("id",0));v.put("event_key",eventKey);v.put("title",name);v.put("event_type",clean(type));v.put("skill_key",key(skill));v.put("dc",Math.max(0,dc));v.put("source_ref",clean(sourceRef));v.put("notes",clean(notes));v.put("created_at",now);long id=db.getWritableDatabase().insertOrThrow("kingmaker_events",null,v);return db.queryOne("SELECT * FROM kingmaker_events WHERE id=?",new String[]{String.valueOf(id)});}
    public void resolveEvent(long id,JSONObject resolution)throws Exception{JSONObject old=db.queryOne("SELECT * FROM kingmaker_events WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});if(old.length()==0)throw new IllegalArgumentException("Evento inexistente.");if("resolved".equals(old.optString("status")))throw new IllegalStateException("Este evento já foi resolvido.");JSONObject result=resolution==null?new JSONObject():new JSONObject(resolution.toString());int award="random".equals(key(old.optString("event_type")))||"aleatorio".equals(key(old.optString("event_type")))?30:Math.max(0,result.optInt("kingdom_xp",0));result.put("kingdom_xp_awarded",award);ContentValues v=new ContentValues();v.put("status","resolved");v.put("resolution_json",result.toString());v.put("resolved_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_events",v,"id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});if(award>0)grantKingdomXp(award,"Evento: "+old.optString("title"));}
    public List<JSONObject> events(String status){String filter=clean(status);return db.queryList("SELECT * FROM kingmaker_events WHERE world_profile=? AND (?='' OR status=?) ORDER BY created_at DESC",new String[]{profile(),filter,filter});}

    public JSONObject addArmy(JSONObject spec)throws Exception{JSONObject x=spec==null?new JSONObject():spec;String name=clean(x.optString("name"));if(name.length()==0)throw new IllegalArgumentException("Nome do exército ausente.");String armyKey=clean(x.optString("army_key"));if(armyKey.length()==0)armyKey="army_"+UUID.randomUUID().toString().replace("-","");ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("world_profile",profile());v.put("army_key",armyKey);v.put("name",name);for(String field:new String[]{"army_type","allegiance","commander_name","location_hex","notes"})v.put(field,clean(x.optString(field)));for(String field:new String[]{"level","size","ac","maneuver_modifier","maneuver_dc","morale_modifier","morale_dc","attack_modifier","hp","max_hp","rout_threshold","ammunition","supplies","consumption","commander_id","scouting"})v.put(field,Math.max(field.contains("modifier")?-50:0,x.optInt(field,armyDefault(field))));v.put("conditions_json",jsonArray(x.optString("conditions_json","[]")).toString());v.put("tactics_json",jsonArray(x.optString("tactics_json","[]")).toString());JSONObject existing=army(armyKey);v.put("created_at",existing.length()==0?now:existing.optLong("created_at",now));v.put("updated_at",now);db.getWritableDatabase().insertWithOnConflict("kingmaker_armies",null,v,SQLiteDatabase.CONFLICT_REPLACE);return army(armyKey);}
    public JSONObject army(String key){return db.queryOne("SELECT * FROM kingmaker_armies WHERE world_profile=? AND army_key=?",new String[]{profile(),clean(key)});}
    public List<JSONObject> armies(){return db.queryList("SELECT * FROM kingmaker_armies WHERE world_profile=? ORDER BY allegiance,name",new String[]{profile()});}
    public JSONObject resolveArmyAction(String armyKey,String action,int natural,int dc,int attackIndex,boolean agile)throws Exception{JSONObject army=army(armyKey);if(army.length()==0)throw new IllegalArgumentException("Exército inexistente.");String kind=key(action);int mod=kind.contains("morale")||kind.contains("moral")?army.optInt("morale_modifier"):kind.contains("maneuver")||kind.contains("manobra")?army.optInt("maneuver_modifier"):army.optInt("attack_modifier");int target=dc>0?dc:(kind.contains("morale")||kind.contains("moral")?army.optInt("morale_dc"):army.optInt("maneuver_dc"));return rules.resolveArmyCheck(action,natural,mod,target,attackIndex,agile);}
    public JSONObject damageArmy(String armyKey,int hits,int weakness,int resistance)throws Exception{JSONObject before=army(armyKey);if(before.length()==0)throw new IllegalArgumentException("Exército inexistente.");JSONObject result=rules.applyArmyDamage(before.optInt("hp"),before.optInt("max_hp"),hits,weakness,resistance,before.optInt("rout_threshold"));ContentValues v=new ContentValues();v.put("hp",result.optInt("hp"));JSONArray conditions=jsonArray(before.optString("conditions_json","[]"));if(result.optBoolean("defeated")&&!contains(conditions,"defeated"))conditions.put("defeated");v.put("conditions_json",conditions.toString());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_armies",v,"world_profile=? AND army_key=?",new String[]{profile(),armyKey});result.put("army",army(armyKey));audit("Dano de exército aplicado",before.optString("name")+" • "+result.optInt("damage"),before,army(armyKey));return result;}

    public JSONObject resolveWarAttack(String encounterKey,String attackerKey,String targetKey,int natural,int attackIndex,boolean agile,int weakness,int resistance)throws Exception{
        JSONObject encounter=db.queryOne("SELECT * FROM kingmaker_war_encounters WHERE world_profile=? AND encounter_key=?",new String[]{profile(),clean(encounterKey)});
        if(encounter.length()==0||!"active".equals(encounter.optString("status")))throw new IllegalStateException("Não há batalha ativa para este ataque.");
        JSONArray participants=jsonArray(encounter.optString("participants_json","[]"));Set<String> roster=new HashSet<String>();
        for(int i=0;i<participants.length();i++){Object entry=participants.opt(i);String k=entry instanceof JSONObject?clean(((JSONObject)entry).optString("army_key")):clean(String.valueOf(entry));if(k.length()>0)roster.add(k);}
        String attackerClean=clean(attackerKey),targetClean=clean(targetKey);if(attackerClean.equals(targetClean))throw new IllegalArgumentException("Escolha um exército-alvo diferente do atacante.");
        String activeArmy=clean(encounter.optString("active_army_key"));if(activeArmy.length()>0&&!activeArmy.equals(attackerClean))throw new IllegalStateException("Somente o exército ativo pode agir nesta rodada.");
        if(!roster.contains(attackerClean)||!roster.contains(targetClean))throw new IllegalArgumentException("Atacante e alvo precisam participar da batalha.");
        JSONObject attacker=army(attackerClean),target=army(targetClean);if(attacker.length()==0||target.length()==0)throw new IllegalArgumentException("Exército atacante ou alvo inexistente.");
        if(attacker.optInt("hp")<=0)throw new IllegalStateException("O exército atacante já foi derrotado.");if(target.optInt("hp")<=0)throw new IllegalStateException("O exército-alvo já foi derrotado.");
        JSONObject check=resolveArmyAction(attackerClean,"attack",natural,target.optInt("ac"),Math.max(1,attackIndex),agile);int hits=Math.max(0,check.optInt("hits"));
        JSONObject damage=hits>0?damageArmy(targetClean,hits,Math.max(0,weakness),Math.max(0,resistance)):new JSONObject().put("damage",0).put("hp",target.optInt("hp")).put("defeated",false).put("army",target);
        JSONObject targetAfter=army(targetClean);String attackerAllegiance=attacker.optString("allegiance");boolean oppositionStanding=false;
        for(String k:roster){if(k.equals(attackerClean))continue;JSONObject other=army(k);if(other.length()==0||other.optInt("hp")<=0)continue;if(attackerAllegiance.length()==0||!attackerAllegiance.equals(other.optString("allegiance"))){oppositionStanding=true;break;}}
        String nextStatus=oppositionStanding?"active":"complete";JSONObject logEntry=new JSONObject().put("kind","attack").put("attacker_army_key",attackerClean).put("target_army_key",targetClean)
                .put("natural",natural).put("dc",target.optInt("ac")).put("attack_index",Math.max(1,attackIndex)).put("agile",agile).put("hits",hits).put("damage",damage.optInt("damage"))
                .put("target_hp",targetAfter.optInt("hp")).put("target_defeated",targetAfter.optInt("hp")<=0);
        JSONObject updated=advanceWarEncounter(encounter.optString("encounter_key"),encounter.optInt("round_number",1),attackerClean,nextStatus,logEntry);
        return new JSONObject().put("check",check).put("damage",damage).put("target",targetAfter).put("encounter",updated).put("battle_complete","complete".equals(nextStatus));
    }

    public KingmakerWarfareEngine warfare(){return rules.warfare();}

    public JSONObject updateArmyConditions(String armyKey,JSONArray conditions)throws Exception{
        JSONObject before=army(armyKey);if(before.length()==0)throw new IllegalArgumentException("Exército inexistente.");
        ContentValues v=new ContentValues();
        JSONArray arr=conditions==null?new JSONArray():conditions;
        v.put("conditions_json",arr.toString());
        v.put("updated_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_armies",v,"world_profile=? AND army_key=?",new String[]{profile(),clean(armyKey)});
        JSONObject after=army(armyKey);
        audit("Condições do exército atualizadas",after.optString("name"),before,after);
        return after;
    }

    public JSONObject setArmyTactics(String armyKey,JSONArray tactics)throws Exception{
        JSONObject before=army(armyKey);if(before.length()==0)throw new IllegalArgumentException("Exército inexistente.");
        ContentValues v=new ContentValues();
        JSONArray arr=tactics==null?new JSONArray():tactics;
        v.put("tactics_json",arr.toString());
        v.put("updated_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_armies",v,"world_profile=? AND army_key=?",new String[]{profile(),clean(armyKey)});
        JSONObject after=army(armyKey);
        audit("Táticas do exército atualizadas",after.optString("name"),before,after);
        return after;
    }

    public JSONObject checkArmyDestruction(String armyKey,int flatCheckNatural)throws Exception{
        JSONObject before=army(armyKey);if(before.length()==0)throw new IllegalArgumentException("Exército inexistente.");
        JSONObject result=warfare().checkArmyDestruction(before,flatCheckNatural);
        JSONObject updatedArmy=result.optJSONObject("army");
        if(updatedArmy!=null){
            ContentValues v=new ContentValues();
            v.put("conditions_json",updatedArmy.optString("conditions_json","[]"));
            v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_armies",v,"world_profile=? AND army_key=?",new String[]{profile(),clean(armyKey)});
        }
        JSONObject after=army(armyKey);
        result.put("army",after);
        audit("Teste de destruição de exército",after.optString("name")+" • "+(result.optBoolean("destroyed")?"Destruído":"Sobreviveu"),before,after);
        return result;
    }

    public JSONObject applyPostBattleRecovery(String armyKey,int naturalRoll,int dc)throws Exception{
        JSONObject before=army(armyKey);if(before.length()==0)throw new IllegalArgumentException("Exército inexistente.");
        JSONObject result=warfare().postBattleRecovery(before,naturalRoll,dc);
        JSONObject updatedArmy=result.optJSONObject("army");
        if(updatedArmy!=null){
            ContentValues v=new ContentValues();
            v.put("hp",updatedArmy.optInt("hp"));
            v.put("conditions_json",updatedArmy.optString("conditions_json","[]"));
            v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_armies",v,"world_profile=? AND army_key=?",new String[]{profile(),clean(armyKey)});
        }
        JSONObject after=army(armyKey);
        result.put("army",after);
        audit("Recuperação pós-batalha de exército",after.optString("name")+" • PV "+after.optInt("hp")+"/"+after.optInt("max_hp"),before,after);
        return result;
    }

    public JSONObject calculateArmyCasualties(String armyKey){
        JSONObject target=army(armyKey);if(target.length()==0)throw new IllegalArgumentException("Exército inexistente.");
        return warfare().calculateCasualties(target);
    }

    public JSONObject reinforceArmy(String armyKey,int hpToRestore,int ammunition,int supplies,int rpCost)throws Exception{
        JSONObject before=army(armyKey);if(before.length()==0)throw new IllegalArgumentException("Exército inexistente.");
        if(rpCost>0){
            JSONObject kingdomState=state();
            int currentRp=kingdomState.optInt("resource_points",0);
            if(currentRp<rpCost)throw new IllegalStateException("Pontos de Recursos insuficientes para reforço ("+currentRp+" < "+rpCost+").");
            ContentValues sv=new ContentValues();
            sv.put("resource_points",currentRp-rpCost);
            sv.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_state",sv,"world_profile=?",new String[]{profile()});
            recordRpSpent(rpCost);
        }
        JSONObject result=warfare().reinforceArmy(before,hpToRestore,ammunition,supplies,rpCost);
        JSONObject updatedArmy=result.optJSONObject("army");
        if(updatedArmy!=null){
            ContentValues v=new ContentValues();
            v.put("hp",updatedArmy.optInt("hp"));
            v.put("ammunition",updatedArmy.optInt("ammunition"));
            v.put("supplies",updatedArmy.optInt("supplies"));
            v.put("conditions_json",updatedArmy.optString("conditions_json","[]"));
            v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_armies",v,"world_profile=? AND army_key=?",new String[]{profile(),clean(armyKey)});
        }
        JSONObject after=army(armyKey);
        result.put("army",after);
        audit("Exército reforçado",after.optString("name")+" • PV "+after.optInt("hp")+"/"+after.optInt("max_hp"),before,after);
        return result;
    }

    public JSONObject resolveWarfareAction(String encounterKey,String actorKey,String targetKey,String action,int naturalRoll,JSONObject options)throws Exception{
        JSONObject encounter=db.queryOne("SELECT * FROM kingmaker_war_encounters WHERE world_profile=? AND encounter_key=?",new String[]{profile(),clean(encounterKey)});
        if(encounter.length()==0||!"active".equals(encounter.optString("status")))throw new IllegalStateException("Não há batalha ativa para esta ação militar.");
        JSONArray participants=jsonArray(encounter.optString("participants_json","[]"));Set<String> roster=new HashSet<String>();
        for(int i=0;i<participants.length();i++){Object entry=participants.opt(i);String k=entry instanceof JSONObject?clean(((JSONObject)entry).optString("army_key")):clean(String.valueOf(entry));if(k.length()>0)roster.add(k);}
        String actorClean=clean(actorKey),targetClean=clean(targetKey);
        if(!roster.contains(actorClean))throw new IllegalArgumentException("O exército atuante deve participar da batalha.");
        String activeArmy=clean(encounter.optString("active_army_key"));
        if(activeArmy.length()>0&&!activeArmy.equals(actorClean))throw new IllegalStateException("Somente o exército ativo pode agir nesta rodada.");
        JSONObject actor=army(actorClean);
        if(actor.length()==0)throw new IllegalArgumentException("Exército atuante inexistente.");
        JSONObject target=targetClean.length()>0?army(targetClean):null;
        if(targetClean.length()>0&&(target==null||target.length()==0))throw new IllegalArgumentException("Exército-alvo inexistente.");

        JSONObject result=warfare().resolveWarAction(action,actor,target,naturalRoll,options);

        JSONObject actorAfter=result.optJSONObject("actor");
        if(actorAfter!=null){
            ContentValues va=new ContentValues();
            va.put("hp",actorAfter.optInt("hp"));
            va.put("ammunition",actorAfter.optInt("ammunition"));
            va.put("supplies",actorAfter.optInt("supplies"));
            va.put("conditions_json",actorAfter.optString("conditions_json","[]"));
            va.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_armies",va,"world_profile=? AND army_key=?",new String[]{profile(),actorClean});
        }

        JSONObject targetAfter=result.optJSONObject("target");
        if(targetAfter!=null&&targetClean.length()>0){
            ContentValues vt=new ContentValues();
            vt.put("hp",targetAfter.optInt("hp"));
            vt.put("ammunition",targetAfter.optInt("ammunition"));
            vt.put("supplies",targetAfter.optInt("supplies"));
            vt.put("conditions_json",targetAfter.optString("conditions_json","[]"));
            vt.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_armies",vt,"world_profile=? AND army_key=?",new String[]{profile(),targetClean});
        }

        boolean oppositionStanding=false;
        String actorAllegiance=actor.optString("allegiance");
        for(String k:roster){
            if(k.equals(actorClean))continue;
            JSONObject other=army(k);
            if(other.length()==0||other.optInt("hp")<=0||rules.warfare().hasCondition(other,KingmakerWarfareEngine.COND_DEFEATED)||rules.warfare().hasCondition(other,KingmakerWarfareEngine.COND_DESTROYED))continue;
            if(actorAllegiance.length()==0||!actorAllegiance.equals(other.optString("allegiance"))){
                oppositionStanding=true;break;
            }
        }

        String nextStatus=oppositionStanding?"active":"complete";
        JSONObject logEntry=new JSONObject()
                .put("kind",action)
                .put("action_pt",result.optString("action_pt"))
                .put("actor_army_key",actorClean)
                .put("target_army_key",targetClean)
                .put("natural",naturalRoll)
                .put("hits",result.optInt("hits",0))
                .put("damage",result.optInt("damage",0))
                .put("narrative",result.optString("narrative"))
                .put("opposition_standing",oppositionStanding);

        JSONObject updatedEncounter=advanceWarEncounter(encounter.optString("encounter_key"),encounter.optInt("round_number",1),actorClean,nextStatus,logEntry);
        result.put("encounter",updatedEncounter);
        result.put("battle_complete","complete".equals(nextStatus));
        result.put("actor",army(actorClean));
        if(targetClean.length()>0)result.put("target",army(targetClean));
        return result;
    }

    public JSONObject startWarEncounter(String name,String battlefieldHex,JSONArray participants,String activeArmyKey)throws Exception{
        requireEnabled();String encounterName=clean(name);if(encounterName.length()==0)throw new IllegalArgumentException("Nome da batalha ausente.");JSONArray roster=participants==null?new JSONArray():new JSONArray(participants.toString());if(roster.length()<1)throw new IllegalArgumentException("Selecione ao menos um exército participante.");Set<String> unique=new HashSet<String>();for(int i=0;i<roster.length();i++){Object entry=roster.opt(i);String armyKey=entry instanceof JSONObject?clean(((JSONObject)entry).optString("army_key")):clean(String.valueOf(entry));if(armyKey.length()==0||army(armyKey).length()==0)throw new IllegalArgumentException("Exército participante inexistente: "+armyKey);if(!unique.add(armyKey))throw new IllegalArgumentException("Exército duplicado na batalha: "+armyKey);}
        String active=clean(activeArmyKey);if(active.length()==0)active=String.valueOf(roster.opt(0));if(!unique.contains(active))throw new IllegalArgumentException("O exército ativo deve participar da batalha.");String encounterKey="war_"+UUID.randomUUID().toString().replace("-","");long now=System.currentTimeMillis();JSONArray log=new JSONArray().put(new JSONObject().put("round",1).put("kind","start").put("active_army_key",active).put("at",now));ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("encounter_key",encounterKey);v.put("name",encounterName);v.put("round_number",1);v.put("active_army_key",active);v.put("battlefield_hex",clean(battlefieldHex).toUpperCase(Locale.US));v.put("participants_json",roster.toString());v.put("log_json",log.toString());v.put("started_at",now);v.put("updated_at",now);long id=db.getWritableDatabase().insertOrThrow("kingmaker_war_encounters",null,v);JSONObject out=db.queryOne("SELECT * FROM kingmaker_war_encounters WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});audit("Batalha iniciada",encounterName+" • "+out.optString("battlefield_hex"),new JSONObject(),out);return out;
    }
    public List<JSONObject> warEncounters(String status){String filter=clean(status);return db.queryList("SELECT * FROM kingmaker_war_encounters WHERE world_profile=? AND (?='' OR status=?) ORDER BY updated_at DESC",new String[]{profile(),filter,filter});}
    public JSONObject advanceWarEncounter(String encounterKey,int nextRound,String activeArmyKey,String status,JSONObject logEntry)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_war_encounters WHERE world_profile=? AND encounter_key=?",new String[]{profile(),clean(encounterKey)});
        if(before.length()==0)throw new IllegalArgumentException("Batalha inexistente.");
        JSONArray participants=jsonArray(before.optString("participants_json","[]"));
        String active=clean(activeArmyKey);if(active.length()==0)active=before.optString("active_army_key");
        boolean found=false;for(int i=0;i<participants.length();i++){Object entry=participants.opt(i);String key=entry instanceof JSONObject?((JSONObject)entry).optString("army_key"):String.valueOf(entry);if(active.equals(key)){found=true;break;}}
        if(!found)throw new IllegalArgumentException("O exército ativo deve participar da batalha.");
        String nextStatus=key(status);if(!"active".equals(nextStatus)&&!"complete".equals(nextStatus)&&!"cancelled".equals(nextStatus))throw new IllegalArgumentException("Estado de batalha inválido.");
        int round=Math.max(before.optInt("round_number",1),nextRound);JSONArray log=jsonArray(before.optString("log_json","[]"));JSONObject entry=logEntry==null?new JSONObject():new JSONObject(logEntry.toString());
        if("complete".equals(nextStatus)&&hasStandingOpposition(participants,active)){String kind=entry.optString("kind");if(!"automatic_complete".equals(kind)&&!"opposition_defeated".equals(kind))throw new IllegalStateException("A batalha ainda possui oposição viva; registre uma retirada/cessar-fogo ou resolva a próxima rodada.");}
        entry.put("round",round).put("active_army_key",active).put("status",nextStatus).put("at",System.currentTimeMillis());log.put(entry);ContentValues v=new ContentValues();v.put("round_number",round);v.put("active_army_key",active);v.put("status",nextStatus);v.put("log_json",log.toString());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_war_encounters",v,"world_profile=? AND encounter_key=?",new String[]{profile(),clean(encounterKey)});JSONObject after=db.queryOne("SELECT * FROM kingmaker_war_encounters WHERE world_profile=? AND encounter_key=?",new String[]{profile(),clean(encounterKey)});audit("Batalha atualizada",after.optString("name")+" • rodada "+round,before,after);return after;
    }

    private boolean hasStandingOpposition(JSONArray participants,String activeKey){JSONObject active=army(activeKey);for(int i=0;i<participants.length();i++){Object raw=participants.opt(i);String key=raw instanceof JSONObject?((JSONObject)raw).optString("army_key"):String.valueOf(raw);if(key.equals(activeKey))continue;JSONObject other=army(key);if(other.length()>0&&other.optInt("hp")>0&&(active.optString("allegiance").length()==0||!active.optString("allegiance").equals(other.optString("allegiance"))))return true;}return false;}

    public JSONObject companion(String companionKey){return db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});}

    public void ensureDefaultCompanions(){
        JSONArray catalog=rules.companionCatalog();long now=System.currentTimeMillis();
        for(int i=0;i<catalog.length();i++){
            JSONObject prof=catalog.optJSONObject(i);if(prof==null)continue;
            String k=prof.optString("companion_key");
            JSONObject exist=db.queryOne("SELECT id FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),k});
            if(exist.length()==0){
                ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("companion_key",k);v.put("npc_id",0);
                v.put("name",prof.optString("name"));v.put("influence",0);v.put("attitude",prof.optString("initial_attitude","indifferent"));
                v.put("assignment","reserve");v.put("leadership_role",prof.optString("leadership_role",""));v.put("available",1);
                v.put("personal_quest_status","not_started");v.put("personal_quest_notes",prof.optString("personal_quest",""));
                v.put("unlocked_rewards_json","[]");v.put("notes","Companheiro oficial das Terras Roubadas.");v.put("created_at",now);v.put("updated_at",now);
                db.getWritableDatabase().insertWithOnConflict("kingmaker_companions",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    public JSONObject addCompanion(String name,long npcId,String assignment,String notes)throws Exception{
        String n=clean(name);if(n.length()==0)throw new IllegalArgumentException("Nome do companheiro ausente.");
        String canon=KingmakerRulesEngine.canonicalCompanionKey(n);
        JSONObject prof=rules.companionProfile(canon);
        String companionKey=prof.length()>0?prof.optString("companion_key"):("companion_"+UUID.randomUUID().toString().replace("-",""));
        JSONObject existing=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),companionKey});
        if(existing.length()>0)return existing;
        ContentValues v=new ContentValues();long now=System.currentTimeMillis();
        v.put("world_profile",profile());v.put("companion_key",companionKey);v.put("npc_id",Math.max(0,npcId));
        v.put("name",prof.length()>0?prof.optString("name"):n);v.put("influence",0);
        v.put("attitude",prof.length()>0?prof.optString("initial_attitude","indifferent"):"indifferent");
        v.put("assignment",clean(assignment).length()==0?"reserve":clean(assignment));
        v.put("leadership_role",prof.length()>0?prof.optString("leadership_role",""):"");v.put("available",1);
        v.put("personal_quest_status","not_started");v.put("personal_quest_notes",prof.length()>0?prof.optString("personal_quest",""):"");
        v.put("unlocked_rewards_json","[]");v.put("notes",clean(notes));v.put("created_at",now);v.put("updated_at",now);
        long id=db.getWritableDatabase().insertOrThrow("kingmaker_companions",null,v);
        return db.queryOne("SELECT * FROM kingmaker_companions WHERE id=?",new String[]{String.valueOf(id)});
    }

    public JSONObject adjustCompanionInfluence(String companionKey,int delta,String notes)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});
        if(before.length()==0)throw new IllegalArgumentException("Companheiro inexistente.");
        if(before.optInt("available",1)!=1)throw new IllegalStateException("O companheiro está indisponível.");
        JSONArray alreadyUnlocked=jsonArray(before.optString("unlocked_rewards_json","[]"));
        JSONObject result=rules.companionInfluence(clean(companionKey),before.optInt("influence"),delta,alreadyUnlocked);
        ContentValues v=new ContentValues();
        v.put("influence",result.optInt("influence"));
        String newAttitude=result.optString("attitude");
        if(newAttitude!=null&&!newAttitude.isEmpty())v.put("attitude",newAttitude);
        v.put("unlocked_rewards_json",result.optJSONArray("all_unlocked_rewards").toString());
        String prevNotes=clean(before.optString("notes"));
        JSONArray newly=result.optJSONArray("newly_unlocked_rewards");
        StringBuilder sb=new StringBuilder();
        if(clean(notes).length()>0)sb.append(clean(notes));
        if(newly!=null&&newly.length()>0){
            for(int i=0;i<newly.length();i++){
                JSONObject r=newly.optJSONObject(i);
                if(r!=null){
                    if(sb.length()>0)sb.append(" | ");
                    sb.append("Recompensa: ").append(r.optString("reward_title")).append(" (").append(r.optString("description")).append(")");
                }
            }
        }
        if(sb.length()>0){
            v.put("notes",prevNotes.length()==0?sb.toString():(prevNotes+" | "+sb.toString()));
        }
        v.put("updated_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_companions",v,"world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});
        JSONObject after=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});
        audit("Influência de companheiro ajustada",after.optString("name")+" • "+after.optString("attitude")+" ("+after.optInt("influence")+")",before,after);
        return after;
    }

    public void updateCompanion(String companionKey,String assignment,String leadershipRole,String attitude,boolean available,String questStatus,String questNotes,String notes){
        String nextAssignment=clean(assignment);
        String nextRole=clean(leadershipRole);
        if(!available){
            if("active".equalsIgnoreCase(nextAssignment))nextAssignment="reserve";
            if(nextRole.length()>0)nextRole="";
            try{
                db.getWritableDatabase().execSQL("UPDATE kingmaker_leaders SET vacant=1, leader_name='', entity_kind='', entity_id=0, invested=0 WHERE world_profile=? AND (leader_name=(SELECT name FROM kingmaker_companions WHERE world_profile=? AND companion_key=?) OR (entity_kind='companion' AND entity_id=(SELECT id FROM kingmaker_companions WHERE world_profile=? AND companion_key=?)))",new String[]{profile(),profile(),clean(companionKey),profile(),clean(companionKey)});
            }catch(Exception ignored){}
        }
        ContentValues v=new ContentValues();v.put("assignment",nextAssignment);v.put("leadership_role",nextRole);
        if(clean(attitude).length()>0)v.put("attitude",clean(attitude));
        v.put("available",available?1:0);
        v.put("personal_quest_status",clean(questStatus));v.put("personal_quest_notes",clean(questNotes));
        v.put("notes",clean(notes));v.put("updated_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_companions",v,"world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});
    }
    public JSONObject recordCompanionQuestProgress(String companionKey,String status,String progressNotes)throws Exception{JSONObject before=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});if(before.length()==0)throw new IllegalArgumentException("Companheiro inexistente.");String next=key(status);if("active".equals(next)||"started".equals(next)||"progressed".equals(next))next="in_progress";if("completed".equals(next))next="complete";if(!"in_progress".equals(next)&&!"complete".equals(next))throw new IllegalArgumentException("Estado de arco pessoal inválido.");ContentValues v=new ContentValues();v.put("personal_quest_status",next);String note=clean(progressNotes);if(note.length()>0){String previous=clean(before.optString("notes"));v.put("notes",previous.length()==0?("Arco pessoal: "+note):(previous+" | Arco pessoal: "+note));}v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_companions",v,"world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});JSONObject after=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});audit("Arco pessoal de companheiro atualizado",after.optString("name")+" • "+next,before,after);return after;}
    public JSONObject pendingCompanionEvent(String companionKey){return db.queryOne("SELECT * FROM kingmaker_events WHERE world_profile=? AND status='pending' AND event_type='companion' AND source_ref=? ORDER BY created_at LIMIT 1",new String[]{profile(),"companion:"+clean(companionKey)});}
    public JSONObject ensureCompanionPersonalEvent(String companionKey)throws Exception{JSONObject companion=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});if(companion.length()==0)throw new IllegalArgumentException("Companheiro inexistente.");if(companion.optInt("available",1)!=1)return new JSONObject();String status=key(companion.optString("personal_quest_status"));if("complete".equals(status)||"completed".equals(status))return new JSONObject();JSONObject pending=pendingCompanionEvent(companionKey);if(pending.length()>0)return pending;String hook=clean(companion.optString("personal_quest_notes"));String notes=hook.length()>0?hook:("Dar espaço a "+companion.optString("name")+" dentro da situação atual, sem fabricar efeito mecânico.");return addEvent("Evento pessoal — "+companion.optString("name"),"companion","",0,"companion:"+clean(companionKey),notes);}
    public JSONObject pendingExplorationEvent(String hexId){String hex=clean(hexId).toUpperCase(Locale.US);return db.queryOne("SELECT * FROM kingmaker_events WHERE world_profile=? AND status='pending' AND event_type='exploration' AND source_ref LIKE ? ORDER BY created_at LIMIT 1",new String[]{profile(),"exploration:"+hex+":%"});}
    public JSONObject ensureExplorationEvent(String hexId)throws Exception{requireEnabled();String hex=clean(hexId).toUpperCase(Locale.US);if(hex.length()==0)throw new IllegalArgumentException("Hex de exploração ausente.");JSONObject map=db.getMapState();if(!hex.equals(map.optString("current_hex_id").trim().toUpperCase(Locale.US)))throw new IllegalStateException("A exploração guiada só pode ser iniciada no hex atual.");String packageId=map.optString("map_package_id");if(packageId.length()==0||db.getMapHex(packageId,hex).length()==0)throw new IllegalStateException("O hex atual não existe no Atlas ativo.");JSONObject pending=pendingExplorationEvent(hex);if(pending.length()>0)return pending;JSONObject overlay=this.hex(hex);String stage=overlay.optInt("reconnoitered",0)==1?(overlay.optInt("cleared",0)==1?"complete":"clear"):"reconnoiter";if("complete".equals(stage))return new JSONObject();String title="reconnoiter".equals(stage)?("Reconhecimento — "+hex):("Ameaças do hex — "+hex);String notes="reconnoiter".equals(stage)?("Explore "+hex+" em cena e registre o reconhecimento somente depois de a exploração acontecer."):("Confronte ou resolva as ameaças de "+hex+" em cena; marcar o hex como limpo exige confirmação explícita depois da cena.");return addEvent(title,"exploration","",0,"exploration:"+hex+":"+stage,notes);}
    public JSONObject resolveExplorationEvent(String hexId,String resolutionNotes)throws Exception{String hex=clean(hexId).toUpperCase(Locale.US);JSONObject pending=pendingExplorationEvent(hex);if(pending.length()==0)throw new IllegalStateException("Não há exploração pendente neste hex.");JSONObject map=db.getMapState();if(!hex.equals(map.optString("current_hex_id").trim().toUpperCase(Locale.US)))throw new IllegalStateException("Retorne ao hex da exploração antes de registrar o resultado.");String source=pending.optString("source_ref"),stage=source.endsWith(":clear")?"clear":"reconnoiter";JSONObject after;if("clear".equals(stage))after=clearHex(hex,resolutionNotes);else after=reconnoiterHex(hex,resolutionNotes);resolveEvent(pending.optLong("id"),new JSONObject().put("hex_id",hex).put("exploration_stage",stage).put("notes",clean(resolutionNotes)).put("kingdom_xp",0));return after;}
    public List<JSONObject> companions(){return db.queryList("SELECT * FROM kingmaker_companions WHERE world_profile=? ORDER BY available DESC,name",new String[]{profile()});}

    public JSONObject startCamp(String hexId,int day,String zoneName,int zoneDc,int encounterDc,String prepareDegree,boolean reused,int specialIngredients,String weather,String notes)throws Exception{
        JSONObject active=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND status='active' ORDER BY started_at DESC LIMIT 1",new String[]{profile()});
        if(active.length()>0)throw new IllegalStateException("Já existe um acampamento ativo; conclua ou retome-o antes de iniciar outro.");
        String campKey="camp_"+UUID.randomUUID().toString().replace("-","");
        JSONObject zone=rules.campZone(zoneName);
        int zDc=zoneDc>0?zoneDc:zone.optInt("zone_dc",16);
        int encDc=encounterDc>0?encounterDc:zone.optInt("encounter_dc",14);
        ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("camp_key",campKey);v.put("hex_id",clean(hexId));v.put("day_number",Math.max(1,day));
        v.put("zone_name",clean(zoneName).length()>0?clean(zoneName):zone.optString("name","Zonaverde"));v.put("zone_dc",zDc);v.put("encounter_dc",encDc);v.put("current_encounter_dc",encDc);
        v.put("phase","CAMP_ACTIVITIES");v.put("step",2);v.put("prepare_degree",clean(prepareDegree));v.put("reused",reused?1:0);
        v.put("basic_ingredients",Math.max(0,zDc));v.put("special_ingredients",Math.max(0,specialIngredients));
        v.put("weather",clean(weather));v.put("notes",clean(notes));v.put("started_at",System.currentTimeMillis());
        long id=db.getWritableDatabase().insertOrThrow("kingmaker_camps",null,v);
        return db.queryOne("SELECT * FROM kingmaker_camps WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});
    }

    public JSONObject prepareCamp(String campKey,int naturalRoll,int modifier,boolean reused)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        if(before.length()==0)throw new IllegalArgumentException("Acampamento inexistente.");
        int zDc=before.optInt("zone_dc",16);
        JSONObject res=rules.prepareCamp(naturalRoll,modifier,zDc,reused);
        ContentValues v=new ContentValues();v.put("prepare_degree",res.optString("degree"));
        int bonusEnc=res.optInt("encounter_dc_bonus",0);
        v.put("current_encounter_dc",before.optInt("encounter_dc")+bonusEnc);
        v.put("step",res.optBoolean("activities_allowed",true)?3:5);
        v.put("phase",res.optBoolean("immediate_encounter_check")?"RANDOM_ENCOUNTER":"CAMP_ACTIVITIES");
        String prev=clean(before.optString("notes"));
        v.put("notes",prev.length()==0?res.optString("description"):(prev+" | "+res.optString("description")));
        db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        return db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
    }

    public JSONObject recordCampActivity(String campKey,String companionKey,String activityKey,int naturalRoll,int modifier)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        if(before.length()==0)throw new IllegalArgumentException("Acampamento inexistente.");
        if(!"active".equals(before.optString("status")))throw new IllegalStateException("O acampamento já foi concluído.");
        if("RANDOM_ENCOUNTER".equals(before.optString("phase")))throw new IllegalStateException("Resolva o encontro aleatório antes de prosseguir com atividades de acampamento.");
        String actorName="Líder";
        if(companionKey!=null&&companionKey.trim().length()>0){
            JSONObject comp=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});
            if(comp.length()>0){
                if(comp.optInt("available",1)!=1)throw new IllegalStateException("O companheiro "+comp.optString("name")+" está indisponível para atividades de acampamento.");
                actorName=comp.optString("name");
            }
        }
        JSONObject res=rules.resolveCampingActivity(activityKey,actorName,naturalRoll,modifier,before.optInt("zone_dc",16),before.optInt("zone_level",2),before);
        JSONArray activities=jsonArray(before.optString("activities_json","[]"));activities.put(res);
        int curEncDc=Math.max(0,before.optInt("current_encounter_dc",before.optInt("encounter_dc")));
        curEncDc=Math.max(0,curEncDc+res.optInt("encounter_dc_delta",0));
        int elapsed=before.optInt("elapsed_hours")+res.optInt("duration_hours",2);
        int basic=before.optInt("basic_ingredients")+res.optInt("basic_ingredients_gained",0);
        int special=before.optInt("special_ingredients")+res.optInt("special_ingredients_gained",0);
        ContentValues v=new ContentValues();v.put("activities_json",activities.toString());v.put("elapsed_hours",elapsed);
        v.put("basic_ingredients",basic);v.put("special_ingredients",special);v.put("current_encounter_dc",Math.max(0,curEncDc-1));v.put("step",3);
        db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        return db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
    }

    public JSONObject cookCampMeal(String campKey,String cookCompanionKey,String recipeKey,int naturalRoll,int modifier,String dinersCompanionKeysJson)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        if(before.length()==0)throw new IllegalArgumentException("Acampamento inexistente.");
        if(!"active".equals(before.optString("status")))throw new IllegalStateException("O acampamento já foi concluído.");
        if("RANDOM_ENCOUNTER".equals(before.optString("phase")))throw new IllegalStateException("Resolva o encontro antes de cozinhar.");
        String cookName="Cozinheiro";
        if(cookCompanionKey!=null&&cookCompanionKey.trim().length()>0){
            JSONObject comp=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(cookCompanionKey)});
            if(comp.length()>0){
                if(comp.optInt("available",1)!=1)throw new IllegalStateException("O companheiro "+comp.optString("name")+" está indisponível para cozinhar.");
                cookName=comp.optString("name");
            }
        }
        JSONArray diners=jsonArray(dinersCompanionKeysJson);int dinerCount=Math.max(1,diners.length());
        JSONObject cookRes=rules.resolveCookingMeal(recipeKey,cookName,naturalRoll,modifier,dinerCount,before.optInt("basic_ingredients"),before.optInt("special_ingredients"),diners);
        if(!cookRes.optBoolean("success",true)&&cookRes.has("error")){throw new IllegalStateException(cookRes.optString("error"));}
        ContentValues v=new ContentValues();v.put("basic_ingredients",cookRes.optInt("remaining_basic",before.optInt("basic_ingredients")));
        v.put("special_ingredients",cookRes.optInt("remaining_special",before.optInt("special_ingredients")));
        v.put("meal_json",cookRes.toString());v.put("buffs_json",cookRes.optJSONArray("buffs_24h")!=null?cookRes.optJSONArray("buffs_24h").toString():"[]");
        v.put("step",4);v.put("phase","MEALS_AND_COOKING");
        db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        return db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
    }

    public JSONObject resolveSleepAndRest(String campKey,int partySize,int level,int conModifier,String comfort)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        if(before.length()==0)throw new IllegalArgumentException("Acampamento inexistente.");
        if(!"active".equals(before.optString("status")))throw new IllegalStateException("O acampamento já foi concluído.");
        if("RANDOM_ENCOUNTER".equals(before.optString("phase")))throw new IllegalStateException("Resolva o encontro antes de repousar.");
        JSONObject restRes=rules.resolveSleepAndRest(partySize,level,conModifier,comfort,new JSONArray(),before.optInt("current_encounter_dc"),before.optInt("encounter_dc"),before.optInt("elapsed_hours"));
        ContentValues v=new ContentValues();v.put("rest_json",restRes.toString());v.put("step",5);v.put("phase","SLEEP_AND_REST");
        db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        return db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
    }

    public JSONObject recordCampHour(String campKey,String activityJson,int encounterRoll)throws Exception{JSONObject before=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});if(before.length()==0)throw new IllegalArgumentException("Acampamento inexistente.");if(!"active".equals(before.optString("status")))throw new IllegalStateException("O acampamento já foi concluído.");if("RANDOM_ENCOUNTER".equals(before.optString("phase")))throw new IllegalStateException("Resolva o encontro aleatório antes de registrar outra hora do acampamento.");if(encounterRoll<1||encounterRoll>20)throw new IllegalArgumentException("O teste simples de encontro exige resultado natural de 1 a 20.");JSONObject activity=jsonObject(activityJson);JSONArray activities=jsonArray(before.optString("activities_json","[]"));activities.put(activity);int dc=Math.max(0,before.optInt("current_encounter_dc",before.optInt("encounter_dc"))),hour=before.optInt("elapsed_hours")+1;boolean triggered=dc>0&&encounterRoll>=dc;JSONArray checks=jsonArray(before.optString("encounter_checks_json","[]"));checks.put(new JSONObject().put("hour",hour).put("roll",encounterRoll).put("dc",dc).put("triggered",triggered));ContentValues v=new ContentValues();v.put("activities_json",activities.toString());v.put("encounter_checks_json",checks.toString());v.put("elapsed_hours",hour);v.put("current_encounter_dc",Math.max(0,dc-1));v.put("phase",triggered?"RANDOM_ENCOUNTER":"CAMP_ACTIVITIES");if(triggered)v.put("encounter_json",new JSONObject().put("triggered",true).put("resolved",false).put("hour",hour).put("roll",encounterRoll).put("dc",dc).toString());db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});return db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});}
    public JSONObject resumeCampAfterEncounter(String campKey,String resolutionNotes)throws Exception{JSONObject before=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});if(before.length()==0)throw new IllegalArgumentException("Acampamento inexistente.");if(!"active".equals(before.optString("status")))throw new IllegalStateException("O acampamento já foi concluído.");if(!"RANDOM_ENCOUNTER".equals(before.optString("phase")))throw new IllegalStateException("Não há encontro aleatório pendente neste acampamento.");JSONObject encounter=jsonObject(before.optString("encounter_json","{}"));if(!encounter.optBoolean("triggered"))throw new IllegalStateException("O acampamento não possui encontro disparado.");encounter.put("resolved",true).put("resolution_notes",clean(resolutionNotes)).put("resolved_at",System.currentTimeMillis());ContentValues v=new ContentValues();v.put("encounter_json",encounter.toString());v.put("phase","CAMP_ACTIVITIES");db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});JSONObject after=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});audit("Encontro de acampamento resolvido",after.optString("hex_id")+" • "+clean(resolutionNotes),before,after);return after;}

    public JSONObject completeCamp(String campKey,String watchJson,String activitiesJson,String encounterJson,String resultNotes)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        if(before.length()==0)throw new IllegalArgumentException("Acampamento inexistente.");
        if("RANDOM_ENCOUNTER".equals(before.optString("phase")))throw new IllegalStateException("Resolva o encontro aleatório antes de concluir o acampamento.");
        JSONArray actArray=activitiesJson!=null&&activitiesJson.length()>0?jsonArray(activitiesJson):jsonArray(before.optString("activities_json","[]"));
        JSONObject mealObj=jsonObject(before.optString("meal_json","{}"));
        JSONObject restObj=jsonObject(before.optString("rest_json","{}"));
        JSONObject prepObj=new JSONObject().put("prepare_degree",before.optString("prepare_degree"));
        JSONObject finalEffects=rules.aggregateCampEffects(prepObj,actArray,mealObj,restObj);
        if(clean(resultNotes).length()>0){finalEffects.put("notes",clean(resultNotes));}
        ContentValues v=new ContentValues();
        v.put("watch_json",jsonArray(watchJson).toString());v.put("activities_json",actArray.toString());
        v.put("encounter_json",jsonObject(encounterJson).toString());v.put("result_json",finalEffects.toString());
        v.put("phase","COMPLETE");v.put("status","complete");v.put("step",5);v.put("completed_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
        return db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
    }
    public List<JSONObject> camps(int limit){return db.queryList("SELECT * FROM kingmaker_camps WHERE world_profile=? ORDER BY started_at DESC LIMIT "+Math.max(1,Math.min(100,limit)),new String[]{profile()});}

    public JSONObject addTradeAgreement(String origin,String destination,String status,String routeJson,String commoditiesJson,String benefitJson,String sourceRef,String notes)throws Exception{String name=clean(destination),nextStatus=key(status);if(name.length()==0)throw new IllegalArgumentException("Destino comercial ausente.");if(!"proposed".equals(nextStatus)&&!"active".equals(nextStatus)&&!"suspended".equals(nextStatus)&&!"ended".equals(nextStatus))throw new IllegalArgumentException("Estado do acordo comercial inválido.");String agreementKey="trade_"+UUID.randomUUID().toString().replace("-","");ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("world_profile",profile());v.put("agreement_key",agreementKey);v.put("origin_settlement_key",clean(origin));v.put("destination_name",name);v.put("status",nextStatus);v.put("route_json",jsonArray(routeJson).toString());v.put("commodities_json",jsonObject(commoditiesJson).toString());v.put("benefit_json",jsonObject(benefitJson).toString());v.put("source_ref",clean(sourceRef));v.put("notes",clean(notes));v.put("created_at",now);v.put("updated_at",now);long id=db.getWritableDatabase().insertOrThrow("kingmaker_trade_agreements",null,v);if("active".equals(nextStatus))grantMilestoneOnce("first_trade_agreement",80,"Primeiro acordo comercial");return db.queryOne("SELECT * FROM kingmaker_trade_agreements WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),profile()});}
    public List<JSONObject> tradeAgreements(){return db.queryList("SELECT * FROM kingmaker_trade_agreements WHERE world_profile=? ORDER BY updated_at DESC",new String[]{profile()});}

    public void applyStructureSkillBonuses() {
        Map<String, Integer> bestBonuses = new HashMap<String, Integer>();
        for (JSONObject town : settlements()) {
            int townMaxBonus = town.optInt("max_item_bonus", 1);
            List<JSONObject> townStructures = structures(town.optString("settlement_key"));
            for (JSONObject st : townStructures) {
                String skey = st.optString("structure_key");
                JSONObject def = rules.structure(skey);
                if (def == null) continue;
                JSONObject bonuses = def.optJSONObject("skill_bonuses");
                if (bonuses != null) {
                    Iterator<String> it = bonuses.keys();
                    while (it.hasNext()) {
                        String skill = it.next();
                        int rawBonus = bonuses.optInt(skill, 0);
                        int effectiveBonus = Math.min(rawBonus, townMaxBonus);
                        Integer currentBest = bestBonuses.get(skill);
                        if (currentBest == null || effectiveBonus > currentBest) {
                            bestBonuses.put(skill, effectiveBonus);
                        }
                    }
                }
            }
        }
        long now = System.currentTimeMillis();
        for (JSONObject sk : skills()) {
            String skillKey = sk.optString("skill_key");
            int bonus = bestBonuses.containsKey(skillKey) ? bestBonuses.get(skillKey) : 0;
            ContentValues v = new ContentValues();
            v.put("item_bonus", bonus);
            v.put("updated_at", now);
            db.getWritableDatabase().update("kingmaker_skills", v, "world_profile=? AND skill_key=?", new String[]{profile(), skillKey});
        }
    }

    public JSONObject buildStructure(final String settlementKey, final String structureKey, final String notes) throws Exception {
        requireEnabled();
        final JSONObject town = settlement(settlementKey);
        if (town.length() == 0) throw new IllegalArgumentException("Assentamento inexistente.");
        final JSONObject def = rules.structure(structureKey);
        if (def == null) throw new IllegalArgumentException("Estrutura não encontrada no catálogo: " + structureKey);

        final JSONObject s = state();
        if (s.optInt("level", 1) < def.optInt("level", 1)) {
            throw new IllegalStateException("O Reino está no nível " + s.optInt("level", 1) + ", mas a estrutura exige nível " + def.optInt("level", 1) + ".");
        }
        int townRank = settlementRank(town.optString("type"));
        int minRank = settlementRank(def.optString("min_settlement_type", "village"));
        if (townRank < minRank) {
            throw new IllegalStateException("O assentamento (" + town.optString("type") + ") não possui o porte mínimo exigido (" + def.optString("min_settlement_type") + ").");
        }
        String prereq = def.optString("prerequisites", "");
        if (prereq.length() > 0) {
            JSONObject existingPrereq = db.queryOne("SELECT id FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?", new String[]{profile(), clean(settlementKey), prereq});
            if (existingPrereq.length() == 0) {
                JSONObject prereqDef = rules.structure(prereq);
                String prereqName = prereqDef != null ? prereqDef.optString("name", prereq) : prereq;
                throw new IllegalStateException("A construção exige que o assentamento já possua: " + prereqName + ".");
            }
        }

        int lotCost = def.optInt("lots", 1);
        int currentLots = structureLotTotal(settlementKey);
        int capacity = Math.max(1, town.optInt("developed_blocks", 1)) * 4;
        if (currentLots + lotCost > capacity) {
            throw new IllegalStateException("Espaço insuficiente no assentamento: exige " + lotCost + " lotes, mas apenas " + (capacity - currentLots) + " estão livres de " + capacity + ".");
        }

        final int rpCost = def.optInt("rp", 0);
        final int lumberCost = def.optInt("lumber", 0);
        final int stoneCost = def.optInt("stone", 0);
        final int oreCost = def.optInt("ore", 0);
        final int luxuriesCost = def.optInt("luxuries", 0);

        if (s.optInt("resource_points", 0) < rpCost) {
            throw new IllegalStateException("RP insuficiente para construir " + def.optString("name") + " (" + s.optInt("resource_points", 0) + " < " + rpCost + ").");
        }
        if (s.optInt("lumber", 0) < lumberCost) {
            throw new IllegalStateException("Madeira insuficiente (" + s.optInt("lumber", 0) + " < " + lumberCost + ").");
        }
        if (s.optInt("stone", 0) < stoneCost) {
            throw new IllegalStateException("Pedra insuficiente (" + s.optInt("stone", 0) + " < " + stoneCost + ").");
        }
        if (s.optInt("ore", 0) < oreCost) {
            throw new IllegalStateException("Minério insuficiente (" + s.optInt("ore", 0) + " < " + oreCost + ").");
        }
        if (s.optInt("luxuries", 0) < luxuriesCost) {
            throw new IllegalStateException("Artigos de Luxo insuficientes (" + s.optInt("luxuries", 0) + " < " + luxuriesCost + ").");
        }

        final JSONObject[] out = {new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction() {
            public void run() throws Exception {
                ContentValues sv = new ContentValues();
                sv.put("resource_points", s.optInt("resource_points") - rpCost);
                sv.put("lumber", s.optInt("lumber") - lumberCost);
                sv.put("stone", s.optInt("stone") - stoneCost);
                sv.put("ore", s.optInt("ore") - oreCost);
                sv.put("luxuries", s.optInt("luxuries") - luxuriesCost);
                int unrestRed = def.optInt("unrest_reduction", 0);
                if (unrestRed > 0) {
                    int nextUnrest = Math.max(0, s.optInt("unrest") - unrestRed);
                    sv.put("unrest", nextUnrest);
                    sv.put("anarchy", rules.isAnarchy(nextUnrest) ? 1 : 0);
                }
                sv.put("updated_at", System.currentTimeMillis());
                db.getWritableDatabase().update("kingmaker_state", sv, "world_profile=?", new String[]{profile()});
                recordRpSpent(rpCost);

                String k = def.optString("key", structureKey);
                JSONObject existing = db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?", new String[]{profile(), clean(settlementKey), k});
                long now = System.currentTimeMillis();
                ContentValues strV = new ContentValues();
                strV.put("world_profile", profile());
                strV.put("settlement_key", clean(settlementKey));
                strV.put("structure_key", k);
                strV.put("name", def.optString("name"));
                strV.put("lots", def.optInt("lots", 1));
                strV.put("construction_rp", rpCost);
                JSONObject commCosts = new JSONObject().put("lumber", lumberCost).put("stone", stoneCost).put("ore", oreCost).put("luxuries", luxuriesCost);
                strV.put("commodity_costs_json", commCosts.toString());
                JSONObject effects = new JSONObject().put("residential_lots", def.optInt("residential_lots", 0)).put("skill_bonuses", def.optJSONObject("skill_bonuses"));
                strV.put("effects_json", effects.toString());
                strV.put("source_ref", "KingmakerRulesEngine");
                strV.put("notes", clean(notes));
                strV.put("updated_at", now);
                if (existing.length() > 0) {
                    strV.put("count", existing.optInt("count", 1) + 1);
                    strV.put("created_at", existing.optLong("created_at", now));
                } else {
                    strV.put("count", 1);
                    strV.put("created_at", now);
                }
                db.getWritableDatabase().insertWithOnConflict("kingmaker_structures", null, strV, SQLiteDatabase.CONFLICT_REPLACE);

                refreshSettlementOccupancy(settlementKey);
                applyStructureSkillBonuses();
                out[0] = db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?", new String[]{profile(), clean(settlementKey), k});
            }
        });
        audit("Estrutura construída", def.optString("name") + " em " + town.optString("name"), new JSONObject(), out[0]);
        return out[0];
    }

    public JSONObject demolishStructure(final String settlementKey, final String structureKey, final String notes) throws Exception {
        requireEnabled();
        final JSONObject town = settlement(settlementKey);
        if (town.length() == 0) throw new IllegalArgumentException("Assentamento inexistente.");
        final JSONObject existing = db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?", new String[]{profile(), clean(settlementKey), key(structureKey)});
        if (existing.length() == 0 || existing.optInt("count", 0) <= 0) {
            throw new IllegalStateException("A estrutura especificada não existe neste assentamento.");
        }
        final JSONObject[] out = {new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction() {
            public void run() throws Exception {
                int count = existing.optInt("count", 1);
                if (count > 1) {
                    ContentValues v = new ContentValues();
                    v.put("count", count - 1);
                    v.put("updated_at", System.currentTimeMillis());
                    db.getWritableDatabase().update("kingmaker_structures", v, "id=?", new String[]{String.valueOf(existing.optLong("id"))});
                    out[0] = db.queryOne("SELECT * FROM kingmaker_structures WHERE id=?", new String[]{String.valueOf(existing.optLong("id"))});
                } else {
                    db.getWritableDatabase().delete("kingmaker_structures", "id=?", new String[]{String.valueOf(existing.optLong("id"))});
                    out[0] = new JSONObject().put("demolished", true).put("structure_key", structureKey);
                }
                refreshSettlementOccupancy(settlementKey);
                applyStructureSkillBonuses();
            }
        });
        audit("Estrutura demolida", existing.optString("name") + " em " + town.optString("name"), existing, out[0]);
        return out[0];
    }

    public JSONObject repairStructure(String settlementKey, String structureKey, String notes) throws Exception {
        requireEnabled();
        JSONObject town = settlement(settlementKey);
        if (town.length() == 0) throw new IllegalArgumentException("Assentamento inexistente.");
        JSONObject existing = db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?", new String[]{profile(), clean(settlementKey), key(structureKey)});
        if (existing.length() == 0) throw new IllegalStateException("A estrutura especificada não existe neste assentamento.");
        JSONObject def = rules.structure(structureKey);
        int halfRp = def != null ? Math.max(1, def.optInt("rp", 0) / 2) : 1;
        JSONObject s = state();
        if (s.optInt("resource_points", 0) < halfRp) {
            throw new IllegalStateException("RP insuficiente para reparar (" + s.optInt("resource_points", 0) + " < " + halfRp + ").");
        }
        ContentValues sv = new ContentValues();
        sv.put("resource_points", s.optInt("resource_points") - halfRp);
        sv.put("updated_at", System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_state", sv, "world_profile=?", new String[]{profile()});
        recordRpSpent(halfRp);
        ContentValues v = new ContentValues();
        v.put("notes", (clean(existing.optString("notes")) + " [Reparado] " + clean(notes)).trim());
        v.put("updated_at", System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_structures", v, "id=?", new String[]{String.valueOf(existing.optLong("id"))});
        JSONObject after = db.queryOne("SELECT * FROM kingmaker_structures WHERE id=?", new String[]{String.valueOf(existing.optLong("id"))});
        audit("Estrutura reparada", after.optString("name") + " em " + town.optString("name"), existing, after);
        return after;
    }

    public JSONObject upgradeSettlement(String settlementKey, String nextType) throws Exception {
        requireEnabled();
        JSONObject town = settlement(settlementKey);
        if (town.length() == 0) throw new IllegalArgumentException("Assentamento inexistente.");
        String targetType = key(nextType);
        updateSettlement(town.optLong("id"), targetType, town.optInt("level", 1) + 1,
                town.optInt("population", 0), town.optInt("consumption", 1),
                town.optInt("developed_blocks", 1), town.optInt("occupied_lots", 0),
                town.optInt("residential_lots", 0), town.optString("grid_json", "{}"),
                town.optString("storage_json", "{}"), "Melhoria de assentamento para " + targetType);
        applyStructureSkillBonuses();
        return settlement(settlementKey);
    }

    public JSONObject resolveEventPhaseCheck(int flatCheckRoll) throws Exception {
        requireEnabled();
        if (flatCheckRoll < 1 || flatCheckRoll > 20) {
            throw new IllegalArgumentException("O teste simples exige resultado de 1 a 20.");
        }
        JSONObject s = state();
        int currentDc = s.optInt("event_dc", KingmakerRulesEngine.BASE_EVENT_DC);
        boolean eventOccurred = rules.checkEventOccurs(flatCheckRoll, currentDc);
        int nextDc = rules.nextEventDc(currentDc, eventOccurred);

        ContentValues v = new ContentValues();
        v.put("event_dc", nextDc);
        v.put("updated_at", System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_state", v, "world_profile=?", new String[]{profile()});

        JSONObject out = new JSONObject();
        out.put("roll", flatCheckRoll);
        out.put("current_dc", currentDc);
        out.put("event_occurred", eventOccurred);
        out.put("next_dc", nextDc);

        if (eventOccurred) {
            JSONArray cat = rules.eventsCatalog();
            int index = Math.abs(flatCheckRoll - 1) % cat.length();
            JSONObject evDef = cat.optJSONObject(index);
            if (evDef != null) {
                int controlDc = controlSummary().optInt("control_dc", 15);
                JSONObject createdEvent = addEvent(evDef.optString("title"), evDef.optString("event_type"),
                        evDef.optString("skill"), controlDc, "event:" + evDef.optString("key"),
                        evDef.optString("description"));
                out.put("event", createdEvent);
                out.put("event_definition", evDef);
            }
        }
        audit("Fase de Eventos verificada", eventOccurred ? "Evento disparado (CD " + currentDc + ")" : "Nenhum evento (Próxima CD: " + nextDc + ")", s, state());
        return out;
    }

    public JSONObject resolveKingdomEvent(long eventId, int naturalRoll, String resolvingSkillKey, String notes) throws Exception {
        requireEnabled();
        JSONObject ev = db.queryOne("SELECT * FROM kingmaker_events WHERE id=? AND world_profile=?", new String[]{String.valueOf(eventId), profile()});
        if (ev.length() == 0) throw new IllegalArgumentException("Evento inexistente.");
        if ("resolved".equals(ev.optString("status"))) throw new IllegalStateException("Este evento já foi resolvido.");

        String skillKey = clean(resolvingSkillKey).length() > 0 ? clean(resolvingSkillKey) : ev.optString("skill_key", "defense");
        int targetDc = ev.optInt("dc") > 0 ? ev.optInt("dc") : controlSummary().optInt("control_dc", 15);

        JSONObject checkResult = recordCheck("resolve_event", ev.optString("title"), "Liderança", skillKey, naturalRoll, targetDc, ev.optString("source_ref"), notes, false);
        String degree = checkResult.optString("degree");
        String evType = ev.optString("event_type", "harmful");
        boolean beneficial = "beneficial".equalsIgnoreCase(evType);

        JSONObject resolution = new JSONObject();
        resolution.put("degree", degree);
        resolution.put("degree_pt", checkResult.optString("degree_pt"));
        resolution.put("natural_roll", naturalRoll);
        resolution.put("dc", targetDc);
        resolution.put("skill_key", skillKey);
        resolution.put("notes", clean(notes));

        JSONObject stateChanges = new JSONObject();
        int xpAward = 30;

        if ("CRITICAL_SUCCESS".equals(degree)) {
            if (beneficial) {
                xpAward = 40;
                stateChanges.put("resource_points", 2);
                resolution.put("outcome_pt", "Sucesso Crítico: O evento benéfico floresce com vigor extraordinário (+2 RP, +40 XP).");
            } else {
                xpAward = 40;
                stateChanges.put("unrest", -1);
                resolution.put("outcome_pt", "Sucesso Crítico: A ameaça foi neutralizada de forma exemplar (-1 Unrest, +40 XP).");
            }
        } else if ("SUCCESS".equals(degree)) {
            if (beneficial) {
                stateChanges.put("resource_points", 1);
                resolution.put("outcome_pt", "Sucesso: O evento benéfico produz vantagens sólidas para o reino (+1 RP, +30 XP).");
            } else {
                resolution.put("outcome_pt", "Sucesso: A ameaça foi contida sem causar danos ao Reino (+30 XP).");
            }
        } else if ("FAILURE".equals(degree)) {
            if (beneficial) {
                resolution.put("outcome_pt", "Falha: O evento benéfico dissipou-se sem render proveito.");
            } else {
                stateChanges.put("unrest", 1);
                resolution.put("outcome_pt", "Falha: A ameaça gerou instabilidade e desgaste na população (+1 Unrest).");
            }
        } else {
            if (beneficial) {
                stateChanges.put("unrest", 1);
                resolution.put("outcome_pt", "Falha Crítica: As expectativas frustradas geraram descontentamento público (+1 Unrest).");
            } else {
                stateChanges.put("unrest", 2);
                resolution.put("outcome_pt", "Falha Crítica: Desastre descontrolado abala as fundações do Reino (+2 Unrest).");
            }
        }

        if (stateChanges.length() > 0) {
            adjustState(stateChanges, "Resolução do evento: " + ev.optString("title"));
        }

        resolution.put("kingdom_xp", xpAward);
        resolveEvent(eventId, resolution);

        JSONObject resolvedEv = db.queryOne("SELECT * FROM kingmaker_events WHERE id=? AND world_profile=?", new String[]{String.valueOf(eventId), profile()});
        return new JSONObject().put("event", resolvedEv).put("check", checkResult).put("resolution", resolution);
    }

    public JSONObject resolveNewLeadership(String role, String entityKind, long entityId, String name, boolean invested, String notes) throws Exception {
        requireEnabled();
        assignLeader(role, entityKind, entityId, name, invested, notes);
        return db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?", new String[]{profile(), key(role)});
    }

    public JSONObject resolveProclamation(int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "arts";
        JSONObject check = recordCheck("proclamation", "Proclamação", "Governante", skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -2), "Proclamação inspiradora (Sucesso Crítico)");
            out.put("outcome_pt", "Sucesso Crítico: Proclamação inspiradora acalma o povo (-2 Unrest).");
        } else if ("SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -1), "Proclamação bem recebida (Sucesso)");
            out.put("outcome_pt", "Sucesso: Proclamação esclarecedora tranquiliza os ânimos (-1 Unrest).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: A proclamação teve pouco eco na população.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Proclamação mal interpretada (Falha Crítica)");
            out.put("outcome_pt", "Falha Crítica: Declaração desastrada causa descontentamento (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveRestAndRelax(int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "arts";
        JSONObject check = recordCheck("rest_and_relax", "Descansar e Relaxar", "Liderança", skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -2), "Descanso restaurador dos líderes (Sucesso Crítico)");
            out.put("outcome_pt", "Sucesso Crítico: Festividades e descanso pleno renovam a corte (-2 Unrest).");
        } else if ("SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -1), "Descanso dos líderes (Sucesso)");
            out.put("outcome_pt", "Sucesso: Tempo de lazer dissipa o estresse acumulado (-1 Unrest).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: O descanso não foi suficiente para aliviar as tensões políticas.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Descanso mal visto pelo povo (Falha Crítica)");
            out.put("outcome_pt", "Falha Crítica: O povo ressente-se da indolência aparente da liderança (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveSupernaturalSolution(int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "magic";
        JSONObject check = recordCheck("supernatural_solution", "Solução Sobrenatural", "Magíster", skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -2), "Intervenção sobrenatural triunfante (Sucesso Crítico)");
            out.put("outcome_pt", "Sucesso Crítico: Prodígios e presságios abençoam a comunidade (-2 Unrest).");
        } else if ("SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -1), "Solução sobrenatural eficaz (Sucesso)");
            out.put("outcome_pt", "Sucesso: Rituais arcanos ou preces aplacam a inquietação (-1 Unrest).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: As forças sobrenaturais mantêm-se em silêncio.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Reveses mágicos alarmam a corte (Falha Crítica)");
            out.put("outcome_pt", "Falha Crítica: Agouros terríveis ou retorno arcano causam pânico (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveTakeCharge(int naturalRoll, String role, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "statecraft";
        String leaderRole = clean(role).length() > 0 ? clean(role) : "Governante";
        JSONObject check = recordCheck("take_charge", "Assumir o Controle", leaderRole, skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            out.put("circumstance_bonus", 2);
            out.put("outcome_pt", "Sucesso Crítico: Liderança impecável concede +2 de bônus de circunstância no próximo teste de atividade.");
        } else if ("SUCCESS".equals(degree)) {
            out.put("circumstance_bonus", 1);
            out.put("outcome_pt", "Sucesso: Comando resoluto concede +1 de bônus de circunstância no próximo teste de atividade.");
        } else if ("FAILURE".equals(degree)) {
            out.put("circumstance_bonus", 0);
            out.put("outcome_pt", "Falha: A liderança não conseguiu inspirar eficiência extra.");
        } else {
            out.put("circumstance_bonus", -1);
            out.put("outcome_pt", "Falha Crítica: Microgerenciamento gera atritos; -1 de penalidade no próximo teste de atividade.");
        }
        return out;
    }

    public JSONObject resolveSendDiplomaticEnvoy(int naturalRoll, String targetNation, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "politics";
        JSONObject check = recordCheck("send_diplomatic_envoy", "Enviar Enviado Diplomático para " + clean(targetNation), "Emissário", skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            grantKingdomXp(40, "Missão diplomática brilhante");
            out.put("outcome_pt", "Sucesso Crítico: Recepção calorosa e aliança promissora firmada (+40 XP).");
        } else if ("SUCCESS".equals(degree)) {
            grantKingdomXp(20, "Missão diplomática bem-sucedida");
            out.put("outcome_pt", "Sucesso: O enviado é recebido com cordialidade e estabelece diálogo amigável (+20 XP).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: O interlocutor mantém cautela diplomática sem avanços.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Gafes diplomáticas geram desconforto (Falha Crítica)");
            out.put("outcome_pt", "Falha Crítica: Incidente diplomático cria embaraço e tensão nas fronteiras (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolvePledgeOfFealty(int naturalRoll, String targetCommunity, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "politics";
        JSONObject check = recordCheck("pledge_of_fealty", "Juramento de Fidelidade: " + clean(targetCommunity), "Governante", skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            grantKingdomXp(40, "Comunidade acolhe juramento de fidelidade");
            out.put("outcome_pt", "Sucesso Crítico: A comunidade acolhe com entusiasmo a suserania do Reino (+40 XP).");
        } else if ("SUCCESS".equals(degree)) {
            grantKingdomXp(20, "Termos de união aceitos");
            out.put("outcome_pt", "Sucesso: Termos de cooperação e proteção recíproca são aceitos (+20 XP).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: A comunidade prefere manter sua autonomia.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Proposta de suserania soa ameaçadora (Falha Crítica)");
            out.put("outcome_pt", "Falha Crítica: A proposta é recebida como tirania arrogante (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveRequestForeignAid(int naturalRoll, String allyNation, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "trade";
        JSONObject check = recordCheck("request_foreign_aid", "Solicitar Ajuda Externa a " + clean(allyNation), "Conselheiro", skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", 4), "Auxílio generoso de aliados");
            out.put("outcome_pt", "Sucesso Crítico: O aliado responde com extrema generosidade (+4 RP).");
        } else if ("SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", 2), "Auxílio modesto de aliados");
            out.put("outcome_pt", "Sucesso: O aliado despacha suprimentos e fundos emergenciais (+2 RP).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: O aliado recusa o pedido por limitações orçamentárias.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Pedido soa desesperado ou inconveniente (Falha Crítica)");
            out.put("outcome_pt", "Falha Crítica: A corte aliada ofende-se e a fraqueza do Reino é exposta (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveQuellUnrest(int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "politics";
        JSONObject check = recordCheck("quell_unrest", "Acalmar Agitação", "Governante", skill, naturalRoll, 0, "Leadership Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            JSONObject s = state();
            int nextUnrest = Math.max(0, s.optInt("unrest") - 2);
            adjustState(new JSONObject().put("unrest", -2), "Acalmar Agitação (Sucesso Crítico)");
            boolean anarchyNow = rules.isAnarchy(nextUnrest);
            out.put("anarchy", anarchyNow);
            out.put("outcome_pt", "Sucesso Crítico: Revoltas pacificadas eficazmente (-2 Unrest)." + (anarchyNow ? "" : " A Anarquia foi debelada!"));
        } else if ("SUCCESS".equals(degree)) {
            JSONObject s = state();
            int nextUnrest = Math.max(0, s.optInt("unrest") - 1);
            adjustState(new JSONObject().put("unrest", -1), "Acalmar Agitação (Sucesso)");
            boolean anarchyNow = rules.isAnarchy(nextUnrest);
            out.put("anarchy", anarchyNow);
            out.put("outcome_pt", "Sucesso: Discursos e concessões contêm os protestos (-1 Unrest)." + (anarchyNow ? "" : " A Anarquia foi debelada!"));
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: A agitação popular persiste.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Acalmar Agitação desastrado (Falha Crítica)");
            out.put("outcome_pt", "Falha Crítica: Conflitos com a guarda intensificam a revolta (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveClaimHex(String hexId, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "wilderness";
        JSONObject check = recordCheck("claim_hex", "Reivindicar Hex " + clean(hexId), "Guardião", skill, naturalRoll, 0, "Region Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            JSONObject claimed = claimHex(hexId, notes);
            out.put("hex", claimed);
            out.put("outcome_pt", "Sucesso: O hex " + hexId + " foi incorporado soberanamente ao Reino.");
        } else if ("FAILURE".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Tentativa frustrada de reivindicação de hex");
            out.put("outcome_pt", "Falha: Expedição de posse frustrada; o custo de 1 RP foi consumido.");
        } else {
            adjustState(new JSONObject().put("resource_points", -1).put("unrest", 1), "Desastre na expedição de reivindicação");
            out.put("outcome_pt", "Falha Crítica: Colonos e pioneiros emboscados ou dispersados; -1 RP e +1 Unrest.");
        }
        return out;
    }

    public JSONObject resolveAbandonHex(String hexId, String notes) throws Exception {
        requireEnabled();
        JSONObject abandoned = relinquishHex(hexId);
        JSONObject s = state();
        if (s.optInt("unrest", 0) > 0) {
            adjustState(new JSONObject().put("unrest", -1), "Redução de fronteiras alivia encargos do Reino");
        }
        audit("Hex abandonado", clean(hexId), abandoned, hex(hexId));
        return new JSONObject().put("hex", abandoned).put("outcome_pt", "Hex " + hexId + " abandonado; fronteira desmobilizada.");
    }

    public JSONObject resolveClearHex(String hexId, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "wilderness";
        JSONObject check = recordCheck("clear_hex", "Limpar Hex " + clean(hexId), "Guardião", skill, naturalRoll, 0, "Region Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            JSONObject cleared = clearHex(hexId, notes);
            out.put("hex", cleared);
            out.put("outcome_pt", "Sucesso: O hex " + hexId + " foi totalmente limpo e pacificado.");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: Ameaças residuais impedem considerar o hex limpo.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Guarnição rechaçada no hex");
            out.put("outcome_pt", "Falha Crítica: Monstros ou bandidos infligem baixas severas (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveReconnoiterHex(String hexId, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "exploration";
        JSONObject check = recordCheck("reconnoiter_hex", "Reconhecer Hex " + clean(hexId), "Guardião", skill, naturalRoll, 0, "Region Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            JSONObject recon = reconnoiterHex(hexId, notes);
            out.put("hex", recon);
            out.put("outcome_pt", "Sucesso: O hex " + hexId + " foi devidamente explorado e mapeado.");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: Terreno confuso ou intempéries impediram a conclusão do reconhecimento.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Batedores perdidos no hex");
            out.put("outcome_pt", "Falha Crítica: Patrulha de batedores perdida ou dizimada (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveBuildRoads(String hexId, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        JSONObject h = hex(hexId);
        if (h.length() == 0 || h.optInt("claimed") != 1) throw new IllegalStateException("Estradas só podem ser construídas em hex reivindicado.");
        JSONObject s = state();
        if (s.optInt("resource_points", 0) < 1) throw new IllegalStateException("Construir estradas exige 1 RP.");
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "engineering";
        JSONObject check = recordCheck("build_roads", "Construir Estradas em " + clean(hexId), "Vice-rei", skill, naturalRoll, 0, "Region Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Construção de estradas");
            JSONObject improved = improveHex(hexId, "Estradas", true, false, false, false, "", notes);
            out.put("hex", improved);
            out.put("outcome_pt", "Sucesso: Malha viária estabelecida no hex " + hexId + ".");
        } else if ("FAILURE".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Tentativa frustrada de construção de estradas");
            out.put("outcome_pt", "Falha: Dificuldades de terraplanagem atrasam a obra; 1 RP consumido.");
        } else {
            adjustState(new JSONObject().put("resource_points", -1).put("unrest", 1), "Desmoronamento em obras de estradas");
            out.put("outcome_pt", "Falha Crítica: Acidente estrutural destrói suprimentos; 1 RP consumido e +1 Unrest.");
        }
        return out;
    }

    public JSONObject resolveIrrigate(String hexId, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        JSONObject h = hex(hexId);
        if (h.length() == 0 || h.optInt("claimed") != 1) throw new IllegalStateException("Irrigação só pode ser feita em hex reivindicado.");
        if (h.optInt("farmland") == 1) throw new IllegalStateException("O hex já possui terras cultivadas.");
        JSONObject s = state();
        if (s.optInt("resource_points", 0) < 1) throw new IllegalStateException("Irrigar exige 1 RP.");
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "agriculture";
        JSONObject check = recordCheck("irrigate", "Irrigar Hex " + clean(hexId), "Vice-rei", skill, naturalRoll, 0, "Region Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Abertura de canais de irrigação");
            JSONObject improved = improveHex(hexId, "Farmland", false, true, false, false, "", notes);
            out.put("hex", improved);
            out.put("outcome_pt", "Sucesso: Canais abertos e terras cultivadas estabelecidas no hex " + hexId + ".");
        } else if ("FAILURE".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Tentativa frustrada de irrigação");
            out.put("outcome_pt", "Falha: O solo não reteve os diques adequadamente; 1 RP consumido.");
        } else {
            adjustState(new JSONObject().put("resource_points", -1).put("unrest", 1), "Inundação desastrada em irrigação");
            out.put("outcome_pt", "Falha Crítica: Rompimento de diques alaga terras férteis; 1 RP consumido e +1 Unrest.");
        }
        return out;
    }

    public JSONObject resolveEstablishSettlement(String hexId, String settlementName, String notes) throws Exception {
        requireEnabled();
        JSONObject created = addSettlement(settlementName, "village", hexId, "", notes);
        return new JSONObject().put("settlement", created).put("outcome_pt", "Aldeia " + settlementName + " fundada no hex " + hexId + ".");
    }

    public JSONObject resolveWorkSites(String hexId, String siteType, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        JSONObject h = hex(hexId);
        if (h.length() == 0 || h.optInt("claimed") != 1) throw new IllegalStateException("Canteiros exigem hex reivindicado.");
        JSONObject s = state();
        if (s.optInt("resource_points", 0) < 1) throw new IllegalStateException("Canteiro de trabalho exige 1 RP.");
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "industry";
        JSONObject check = recordCheck("work_sites", "Canteiro de Trabalho: " + clean(siteType), "Tesoureiro", skill, naturalRoll, 0, "Region Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Desenvolvimento de canteiro de trabalho");
            JSONObject improved = improveHex(hexId, clean(siteType), false, false, false, false, clean(siteType), notes);
            out.put("hex", improved);
            out.put("outcome_pt", "Sucesso: Canteiro de trabalho (" + siteType + ") instalado com produtividade.");
        } else if ("FAILURE".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Canteiro improdutivo");
            out.put("outcome_pt", "Falha: Veio fraco ou desapropriação difícil; 1 RP consumido.");
        } else {
            adjustState(new JSONObject().put("resource_points", -1).put("unrest", 1), "Acidente grave no canteiro");
            out.put("outcome_pt", "Falha Crítica: Desmoronamento ou greve operária no canteiro; 1 RP consumido e +1 Unrest.");
        }
        return out;
    }

    public JSONObject resolveFortifyHex(String hexId, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        JSONObject h = hex(hexId);
        if (h.length() == 0 || h.optInt("claimed") != 1) throw new IllegalStateException("Fortificação exige hex reivindicado.");
        JSONObject s = state();
        if (s.optInt("resource_points", 0) < 2) throw new IllegalStateException("Fortificar exige 2 RP.");
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "defense";
        JSONObject check = recordCheck("fortify_hex", "Fortificar Hex " + clean(hexId), "General", skill, naturalRoll, 0, "Region Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -2), "Construção de fortificação");
            JSONObject improved = improveHex(hexId, "Fortificação", false, false, false, true, "", notes);
            out.put("hex", improved);
            out.put("outcome_pt", "Sucesso: Paliçadas e postos de vigia construídos no hex " + hexId + ".");
        } else if ("FAILURE".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", -1), "Atraso nas defesas");
            out.put("outcome_pt", "Falha: Obra atrasada por falta de alinhamento; 1 RP consumido.");
        } else {
            adjustState(new JSONObject().put("resource_points", -2).put("unrest", 1), "Desastre em fortificação");
            out.put("outcome_pt", "Falha Crítica: Colapso de muralhas de madeira durante a montagem; 2 RP consumidos e +1 Unrest.");
        }
        return out;
    }

    public JSONObject resolveBuildStructure(String settlementKey, String structureKey, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "engineering";
        JSONObject check = recordCheck("build_structure", "Construir Estrutura: " + clean(structureKey), "Vice-rei", skill, naturalRoll, 0, "Civic Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            JSONObject built = buildStructure(settlementKey, structureKey, notes);
            out.put("structure", built);
            out.put("outcome_pt", "Sucesso: Estrutura concluída com maestria técnica.");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: Obras emperradas por trâmites ou condições meteorológicas; materiais poupados.");
        } else {
            adjustState(new JSONObject().put("resource_points", -1), "Desperdício de material em obra");
            out.put("outcome_pt", "Falha Crítica: Erro grosseiro de fundação; 1 RP em materiais foi perdido.");
        }
        return out;
    }

    public JSONObject resolveDemolish(String settlementKey, String structureKey, String notes) throws Exception {
        requireEnabled();
        JSONObject res = demolishStructure(settlementKey, structureKey, notes);
        return new JSONObject().put("result", res).put("outcome_pt", "Estrutura demolida com segurança; lotes liberados.");
    }

    public JSONObject resolveRepairStructure(String settlementKey, String structureKey, String notes) throws Exception {
        requireEnabled();
        JSONObject res = repairStructure(settlementKey, structureKey, notes);
        return new JSONObject().put("structure", res).put("outcome_pt", "Estrutura reparada e devolvida à plena operacionalidade.");
    }

    public JSONObject resolveUpgradeSettlement(String settlementKey, String nextType, String notes) throws Exception {
        requireEnabled();
        JSONObject upgraded = upgradeSettlement(settlementKey, nextType);
        return new JSONObject().put("settlement", upgraded).put("outcome_pt", "Assentamento promovido à categoria " + clean(nextType) + ".");
    }

    public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "trade";
        JSONObject check = recordCheck("collect_taxes", "Coletar Impostos", "Tesoureiro", skill, naturalRoll, 0, "Commerce Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", 2), "Arrecadação fiscal excepcional");
            out.put("outcome_pt", "Sucesso Crítico: Comércio aquecido e alfândegas eficientes geram superávit (+2 RP).");
        } else if ("SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", 1), "Arrecadação fiscal regular");
            out.put("outcome_pt", "Sucesso: Impostos e tributos recolhidos conforme o previsto (+1 RP).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: Sonegação ou circulação tímida de moeda impedem ganhos fiscais extras.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Revolta popular contra impostos");
            out.put("outcome_pt", "Falha Crítica: Coletores truculentos geram ressentimento e motins (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveImproveLifestyle(int naturalRoll, String skillKey, int rpInvested, String notes) throws Exception {
        requireEnabled();
        int rp = Math.max(1, rpInvested);
        JSONObject s = state();
        if (s.optInt("resource_points", 0) < rp) throw new IllegalStateException("RP insuficiente para melhorar estilo de vida (" + s.optInt("resource_points", 0) + " < " + rp + ").");
        adjustState(new JSONObject().put("resource_points", -rp), "Investimento em estilo de vida e festivais");
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "arts";
        JSONObject check = recordCheck("improve_lifestyle", "Melhorar Estilo de Vida", "Conselheiro", skill, naturalRoll, 0, "Commerce Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -2), "Festivais e serviços públicos excelentes");
            out.put("outcome_pt", "Sucesso Crítico: O povo celebra banquetes e bem-estar comunitário (-2 Unrest).");
        } else if ("SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("unrest", -1), "Melhoria do bem-estar geral");
            out.put("outcome_pt", "Sucesso: O padrão de vida melhora sensivelmente (-1 Unrest).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: Os gastos apenas mantiveram a rotina sem empolgar os cidadãos.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Esbanjamento na corte gera críticas");
            out.put("outcome_pt", "Falha Crítica: Gastos luxuosos escandalizam os mais pobres (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveTapTreasury(int rpAmount, String notes) throws Exception {
        requireEnabled();
        int amount = Math.max(1, rpAmount);
        JSONObject stateChanges = new JSONObject().put("resource_points", amount).put("crime", 2);
        adjustState(stateChanges, "Recurso ao tesouro emergencial");
        return new JSONObject().put("resource_points_gained", amount).put("crime_points", 2).put("outcome_pt", "Fundos emergenciais sacados (" + amount + " RP). Práticas orçamentárias obscuras elevam Crime.");
    }

    public JSONObject resolveTradeCommodities(String buyOrSell, String commodity, int amount, String notes) throws Exception {
        requireEnabled();
        String action = "buy".equalsIgnoreCase(buyOrSell) ? "buy" : "sell";
        String comm = key(commodity);
        if (!"food".equals(comm) && !"lumber".equals(comm) && !"luxuries".equals(comm) && !"ore".equals(comm) && !"stone".equals(comm)) {
            throw new IllegalArgumentException("Mercadoria inválida: " + commodity);
        }
        int qty = Math.max(1, amount);
        JSONObject s = state();
        int maxStorage = rules.commodityStorage(s.optInt("size", 1));
        if ("buy".equals(action)) {
            if (s.optInt("resource_points", 0) < qty) throw new IllegalStateException("RP insuficiente para compra de commodities.");
            int currentComm = s.optInt(comm, 0);
            if (currentComm + qty > maxStorage) throw new IllegalStateException("Capacidade máxima de armazenamento excedida (" + (currentComm + qty) + " > " + maxStorage + ").");
            adjustState(new JSONObject().put("resource_points", -qty).put(comm, qty), "Compra comercial de " + comm);
            return new JSONObject().put("action", "buy").put("commodity", comm).put("amount", qty).put("outcome_pt", "Compradas " + qty + " unidades de " + comm + " por " + qty + " RP.");
        } else {
            int currentComm = s.optInt(comm, 0);
            if (currentComm < qty) throw new IllegalStateException("Estoque insuficiente da mercadoria " + comm + " (" + currentComm + " < " + qty + ").");
            int rpGain = Math.max(1, qty / 2);
            adjustState(new JSONObject().put(comm, -qty).put("resource_points", rpGain), "Venda comercial de " + comm);
            return new JSONObject().put("action", "sell").put("commodity", comm).put("amount", qty).put("rp_gained", rpGain).put("outcome_pt", "Vendidas " + qty + " unidades de " + comm + " por " + rpGain + " RP.");
        }
    }

    public JSONObject resolveManageTradeAgreements(int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "trade";
        JSONObject check = recordCheck("manage_trade_agreements", "Gerenciar Acordos Comerciais", "Tesoureiro", skill, naturalRoll, 0, "Commerce Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", 2), "Fiscalização mercantil impecável");
            out.put("outcome_pt", "Sucesso Crítico: Rotas comerciais operam com eficiência máxima (+2 RP).");
        } else if ("SUCCESS".equals(degree)) {
            adjustState(new JSONObject().put("resource_points", 1), "Cobrança regular de dividendos");
            out.put("outcome_pt", "Sucesso: Tarifas e dividendos mercantis recolhidos com êxito (+1 RP).");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: Fretes e atrasos logísticos consomem as margens comerciais.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Quebra contratual em rota comercial");
            out.put("outcome_pt", "Falha Crítica: Rompimento ou litígio com mercadores associados (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveEstablishTradeAgreement(String originSettlementKey, String destinationName, int naturalRoll, String skillKey, String notes) throws Exception {
        requireEnabled();
        String skill = clean(skillKey).length() > 0 ? key(skillKey) : "trade";
        JSONObject check = recordCheck("establish_trade_agreement", "Estabelecer Acordo Comercial com " + clean(destinationName), "Tesoureiro", skill, naturalRoll, 0, "Commerce Activity", notes, false);
        String degree = check.optString("degree");
        JSONObject out = new JSONObject().put("check", check);
        if ("CRITICAL_SUCCESS".equals(degree) || "SUCCESS".equals(degree)) {
            JSONObject created = addTradeAgreement(originSettlementKey, destinationName, "active", "[]", "{}", "{}", "Tratado Bilateral", notes);
            out.put("trade_agreement", created);
            out.put("outcome_pt", "Sucesso: Tratado comercial bilateral firmado com " + destinationName + ".");
        } else if ("FAILURE".equals(degree)) {
            out.put("outcome_pt", "Falha: Negociações estagnadas; os termos propostos não convenceram.");
        } else {
            adjustState(new JSONObject().put("unrest", 1), "Negociação mercantil desastrosa");
            out.put("outcome_pt", "Falha Crítica: Proposta considerada abusiva gera mal-estar e boicotes (+1 Unrest).");
        }
        return out;
    }

    public JSONObject resolveCapitalInvestment(int goldAmount, String notes) throws Exception {
        requireEnabled();
        int gold = Math.max(0, goldAmount);
        JSONObject s = state();
        int lvl = Math.max(1, s.optInt("level", 1));
        int costPerRp = 50 * lvl;
        int rpGained = gold / costPerRp;
        if (rpGained < 1) throw new IllegalArgumentException("Para o nível " + lvl + " do Reino, cada 1 RP exige " + costPerRp + " PO (fornecido: " + gold + " PO).");
        adjustState(new JSONObject().put("resource_points", rpGained), "Investimento de Capital dos Aventureiros: " + gold + " PO");
        return new JSONObject().put("gold_invested", gold).put("rp_gained", rpGained).put("cost_per_rp", costPerRp).put("outcome_pt", "Investidos " + gold + " PO gerando " + rpGained + " RP para o Reino.");
    }

    public JSONObject resolveKingdomActivity(String activityKey, JSONObject params) throws Exception {
        requireEnabled();
        String k = key(activityKey);
        JSONObject p = params == null ? new JSONObject() : params;
        int nat = p.optInt("natural_roll", 10);
        String skill = p.optString("skill_key", "");
        String notes = p.optString("notes", "");

        if ("new_leadership".equals(k)) {
            return resolveNewLeadership(p.optString("role"), p.optString("entity_kind"), p.optLong("entity_id", 0), p.optString("name"), p.optBoolean("invested", false), notes);
        } else if ("proclamation".equals(k)) {
            return resolveProclamation(nat, skill, notes);
        } else if ("rest_and_relax".equals(k)) {
            return resolveRestAndRelax(nat, skill, notes);
        } else if ("supernatural_solution".equals(k)) {
            return resolveSupernaturalSolution(nat, skill, notes);
        } else if ("take_charge".equals(k)) {
            return resolveTakeCharge(nat, p.optString("role"), skill, notes);
        } else if ("send_diplomatic_envoy".equals(k)) {
            return resolveSendDiplomaticEnvoy(nat, p.optString("target_nation"), skill, notes);
        } else if ("pledge_of_fealty".equals(k)) {
            return resolvePledgeOfFealty(nat, p.optString("target_community"), skill, notes);
        } else if ("request_foreign_aid".equals(k)) {
            return resolveRequestForeignAid(nat, p.optString("ally_nation"), skill, notes);
        } else if ("quell_unrest".equals(k)) {
            return resolveQuellUnrest(nat, skill, notes);
        } else if ("claim_hex".equals(k)) {
            return resolveClaimHex(p.optString("hex_id"), nat, skill, notes);
        } else if ("abandon_hex".equals(k)) {
            return resolveAbandonHex(p.optString("hex_id"), notes);
        } else if ("clear_hex".equals(k)) {
            return resolveClearHex(p.optString("hex_id"), nat, skill, notes);
        } else if ("reconnoiter_hex".equals(k)) {
            return resolveReconnoiterHex(p.optString("hex_id"), nat, skill, notes);
        } else if ("build_roads".equals(k)) {
            return resolveBuildRoads(p.optString("hex_id"), nat, skill, notes);
        } else if ("irrigate".equals(k)) {
            return resolveIrrigate(p.optString("hex_id"), nat, skill, notes);
        } else if ("establish_settlement".equals(k)) {
            return resolveEstablishSettlement(p.optString("hex_id"), p.optString("settlement_name"), notes);
        } else if ("work_sites".equals(k)) {
            return resolveWorkSites(p.optString("hex_id"), p.optString("site_type"), nat, skill, notes);
        } else if ("fortify_hex".equals(k)) {
            return resolveFortifyHex(p.optString("hex_id"), nat, skill, notes);
        } else if ("build_structure".equals(k)) {
            return resolveBuildStructure(p.optString("settlement_key"), p.optString("structure_key"), nat, skill, notes);
        } else if ("demolish".equals(k)) {
            return resolveDemolish(p.optString("settlement_key"), p.optString("structure_key"), notes);
        } else if ("repair_structure".equals(k)) {
            return resolveRepairStructure(p.optString("settlement_key"), p.optString("structure_key"), notes);
        } else if ("upgrade_settlement".equals(k)) {
            return resolveUpgradeSettlement(p.optString("settlement_key"), p.optString("next_type"), notes);
        } else if ("collect_taxes".equals(k)) {
            return resolveCollectTaxes(nat, skill, notes);
        } else if ("improve_lifestyle".equals(k)) {
            return resolveImproveLifestyle(nat, skill, p.optInt("rp_invested", 1), notes);
        } else if ("tap_treasury".equals(k)) {
            return resolveTapTreasury(p.optInt("rp_amount", 2), notes);
        } else if ("trade_commodities".equals(k)) {
            return resolveTradeCommodities(p.optString("action"), p.optString("commodity"), p.optInt("amount", 1), notes);
        } else if ("manage_trade_agreements".equals(k)) {
            return resolveManageTradeAgreements(nat, skill, notes);
        } else if ("establish_trade_agreement".equals(k)) {
            return resolveEstablishTradeAgreement(p.optString("origin_settlement_key"), p.optString("destination_name"), nat, skill, notes);
        } else if ("capital_investment".equals(k)) {
            return resolveCapitalInvestment(p.optInt("gold_amount", 50), notes);
        }

        throw new IllegalArgumentException("Atividade de Reino não reconhecida: " + activityKey);
    }

    public JSONObject compactContext(){JSONObject out=new JSONObject();try{JSONObject s=state();out.put("enabled",s.optInt("enabled")==1).put("authority",rules.authority()).put("state",s).put("control",controlSummary()).put("skills",new JSONArray(skills())).put("leaders",new JSONArray(leaders())).put("features",new JSONArray(features(""))).put("territory",new JSONArray(hexes())).put("settlements",new JSONArray(settlements())).put("structures",new JSONArray(structures(""))).put("active_turn",activeTurn()).put("turn_activities",new JSONArray(turnActivities(activeTurn().optLong("id",0)))).put("events",new JSONArray(events("pending"))).put("armies",new JSONArray(armies())).put("war_encounters",new JSONArray(warEncounters("active"))).put("companions",new JSONArray(companions())).put("recent_camps",new JSONArray(camps(3))).put("trade_agreements",new JSONArray(tradeAgreements())).put("rules_boundary","O banco e KingmakerRulesEngine são autoridade. A IA não altera Reino, exércitos, companheiros nem acampamento diretamente.");}catch(Exception e){try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){}}return out;}

    public JSONObject exportJson()throws Exception{JSONObject out=new JSONObject();out.put("kingmakerState",state());for(int i=0;i<BACKUP_KEYS.length;i++)out.put(BACKUP_KEYS[i],new JSONArray(db.queryList("SELECT * FROM "+BACKUP_TABLES[i]+" WHERE world_profile=? ORDER BY "+orderColumn(BACKUP_TABLES[i]),new String[]{profile()})));return out;}
    public void restoreJson(JSONObject root,String targetProfile)throws Exception{
        if(root==null)return;String world=targetProfile==null?profile():targetProfile;ensureSchema(db.getWritableDatabase());SQLiteDatabase sql=db.getWritableDatabase();for(int i=BACKUP_TABLES.length-1;i>=0;i--)sql.delete(BACKUP_TABLES[i],"world_profile=?",new String[]{world});sql.delete("kingmaker_state","world_profile=?",new String[]{world});JSONObject state=root.optJSONObject("kingmakerState");if(state!=null){ContentValues v=values(state);v.put("world_profile",world);v.put("updated_at",System.currentTimeMillis());if(!v.containsKey("created_at"))v.put("created_at",System.currentTimeMillis());sql.insertOrThrow("kingmaker_state",null,v);}for(int i=0;i<BACKUP_KEYS.length;i++){JSONArray rows=root.optJSONArray(BACKUP_KEYS[i]);if(rows==null)continue;for(int j=0;j<rows.length();j++){JSONObject row=rows.optJSONObject(j);if(row==null)continue;ContentValues v=values(row);v.put("world_profile",world);sql.insertOrThrow(BACKUP_TABLES[i],null,v);}}ensureProfile();
    }

    /** Valida o agregado Kingmaker ainda dentro da transação de restore do AppDb. */
    public JSONObject validateRestoreAgainst(JSONObject root)throws Exception{
        JSONObject report=new JSONObject();JSONArray problems=new JSONArray();JSONObject counts=new JSONObject();JSONObject actual=exportJson();
        if(root==null){
            problems.put("Root de backup nulo");
            report.put("healthy",false).put("problems",problems).put("counts",counts);
            return report;
        }
        JSONObject expectedState=root.optJSONObject("kingmakerState"),actualState=actual.optJSONObject("kingmakerState");
        if(expectedState!=null){
            String[] textFields={"kingdom_name","capital_name","campaign_mode","turn_phase","charter","heartland","government","reputation_mode"};
            for(String field:textFields)if(expectedState.has(field)&&!expectedState.optString(field).equals(actualState==null?"":actualState.optString(field)))problems.put("state."+field+" divergiu");
            String[] intFields={"enabled","level","xp","size","turn_number","active_turn_id","resource_points","unrest","corruption","crime","decay","strife","fame","infamy","food","lumber","luxuries","ore","stone","consumption","resource_die_count","resource_die_size","anarchy","event_dc","party_level_cap","consumption_modifier","control_dc_modifier","ruin_threshold","corruption_threshold","crime_threshold","decay_threshold","strife_threshold","corruption_penalty","crime_penalty","decay_penalty","strife_penalty"};
            for(String field:intFields)if(expectedState.has(field)&&expectedState.optLong(field)!= (actualState==null?0:actualState.optLong(field)))problems.put("state."+field+" divergiu");
        }
        for(int i=0;i<BACKUP_KEYS.length;i++){
            if(root.has(BACKUP_KEYS[i]) && root.optJSONArray(BACKUP_KEYS[i])==null){
                problems.put(BACKUP_KEYS[i]+" deve ser um array");
            }
            JSONArray expected=root.optJSONArray(BACKUP_KEYS[i]),got=actual.optJSONArray(BACKUP_KEYS[i]);
            int expectedCount=expected==null?0:expected.length(),actualCount=got==null?0:got.length();
            counts.put(BACKUP_KEYS[i],new JSONObject().put("expected",expectedCount).put("actual",actualCount));
            if(expectedCount!=actualCount)problems.put(BACKUP_KEYS[i]+" esperava "+expectedCount+" e restaurou "+actualCount);
        }
        validateSemanticRows(root,actual,"kingmakerSkills","skill_key",new String[]{"skill_key","skill_name","ability_key","rank","item_bonus","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerLeaders","role_key",new String[]{"role_key","role_name","entity_kind","entity_id","leader_name","invested","vacant","vacancy_penalty","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerFeatures","feature_key",new String[]{"feature_key","feature_kind","name","level","status","effects_json","source_ref","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerHexes","hex_id",new String[]{"hex_id","map_id","map_package_id","reconnoitered","cleared","claimed","terrain","improvement","road","farmland","landmark","refuge","resource_site","claimed_turn","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerSettlements","settlement_key",new String[]{"settlement_key","poi_id","name","type","level","hex_id","influence","population","consumption","developed_blocks","occupied_lots","residential_lots","overcrowded","max_item_bonus","grid_json","storage_json","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerStructures","structure_key",new String[]{"settlement_key","structure_key","name","count","lots","status","construction_rp","commodity_costs_json","effects_json","source_ref","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerTurns","turn_number",new String[]{"turn_number","phase","status","resource_roll_json","snapshot_before_json","snapshot_after_json"},problems);
        validateSemanticRows(root,actual,"kingmakerTurnActivities","id",new String[]{"turn_id","phase","activity_key","title","actor_role","skill_key","natural_roll","modifier","dc","degree","costs_json","effects_json","source_ref","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerEvents","event_key",new String[]{"event_key","title","event_type","status","skill_key","dc","resolution_json","source_ref","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerArmies","army_key",new String[]{"army_key","name","army_type","allegiance","level","size","ac","maneuver_modifier","maneuver_dc","morale_modifier","morale_dc","attack_modifier","hp","max_hp","rout_threshold","ammunition","supplies","consumption","commander_name","location_hex","conditions_json","tactics_json","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerWarEncounters","encounter_key",new String[]{"encounter_key","name","round_number","active_army_key","status","battlefield_hex","participants_json","log_json"},problems);
        validateSemanticRows(root,actual,"kingmakerCompanions","companion_key",new String[]{"companion_key","name","influence","attitude","assignment","leadership_role","available","personal_quest_status","personal_quest_notes","notes"},problems);
        validateSemanticRows(root,actual,"kingmakerCamps","camp_key",new String[]{"camp_key","hex_id","day_number","status","safety","comfort","meals","watch_json","activities_json","weather","encounter_json","result_json","notes","zone_name","zone_dc","encounter_dc","current_encounter_dc","phase","prepare_degree","reused","elapsed_hours","special_ingredients","encounter_checks_json"},problems);
        validateSemanticRows(root,actual,"kingmakerTradeAgreements","agreement_key",new String[]{"agreement_key","origin_settlement_key","destination_name","status","route_json","commodities_json","benefit_json","source_ref","notes"},problems);

        // Incorpora a auditoria fail-closed do estado de campanha v22
        JSONObject campaignAudit=KingmakerCampaignImporter.validateCampaignRestore(root);
        if(campaignAudit!=null){
            if(!campaignAudit.optBoolean("healthy",false)){
                JSONArray campaignErrors=campaignAudit.optJSONArray("errors");
                if(campaignErrors!=null&&campaignErrors.length()>0){
                    for(int c=0;c<campaignErrors.length();c++){
                        problems.put("campaign: "+campaignErrors.optString(c));
                    }
                }else{
                    problems.put("campaign: auditoria de campanha falhou");
                }
            }
            report.put("campaign_audit",campaignAudit);
        }

        report.put("healthy",problems.length()==0).put("problems",problems).put("counts",counts).put("active_turn",activeTurn()).put("state",state());
        return report;
    }

    public static boolean importCampaignChapter(SQLiteDatabase db, String worldProfile, JSONObject chapterJson) throws Exception {
        return KingmakerCampaignImporter.importChapter(db, worldProfile, chapterJson);
    }

    public static boolean isChapterImported(SQLiteDatabase db, String worldProfile, String chapterKey) {
        return KingmakerCampaignImporter.isChapterImported(db, worldProfile, chapterKey);
    }

    public static JSONObject exportCampaignState(SQLiteDatabase db, String worldProfile) throws Exception {
        return KingmakerCampaignImporter.exportCampaignState(db, worldProfile);
    }

    public static JSONObject validateCampaignRestore(JSONObject backupRoot) {
        return KingmakerCampaignImporter.validateCampaignRestore(backupRoot);
    }

    public boolean importCampaignChapter(JSONObject chapterJson) throws Exception {
        return importCampaignChapter(db.getWritableDatabase(), profile(), chapterJson);
    }

    public boolean isChapterImported(String chapterKey) {
        return isChapterImported(db.getWritableDatabase(), profile(), chapterKey);
    }

    public JSONObject exportCampaignState() throws Exception {
        return exportCampaignState(db.getWritableDatabase(), profile());
    }

    private static void validateSemanticRows(JSONObject expectedRoot,JSONObject actualRoot,String key,String identity,String[] fields,JSONArray problems)throws Exception{
        JSONArray expected=expectedRoot==null?null:expectedRoot.optJSONArray(key),actual=actualRoot==null?null:actualRoot.optJSONArray(key);if(expected==null)expected=new JSONArray();if(actual==null)actual=new JSONArray();
        for(int i=0;i<expected.length();i++){JSONObject e=expected.optJSONObject(i);if(e==null)continue;String id=String.valueOf(e.opt(identity));JSONObject a=findSemanticRow(actual,identity,id,e);if(a.length()==0){problems.put(key+" perdeu registro "+identity+"="+id);continue;}for(String field:fields)if(e.has(field)&&!semanticValueEquals(e.opt(field),a.opt(field))){problems.put(key+"["+id+"]."+field+" divergiu");break;}}
    }

    private static JSONObject findSemanticRow(JSONArray rows,String identity,String id,JSONObject expected){
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);
            if(row==null)continue;
            if(id!=null&&id.length()>0&&!"null".equals(id)&&id.equals(String.valueOf(row.opt(identity))))return row;
            if((id==null||id.length()==0||"null".equals(id)||"0".equals(id))&&expected!=null){
                if(expected.has("activity_key")&&expected.optString("activity_key").equals(row.optString("activity_key")))return row;
            }
        }
        return new JSONObject();
    }

    private static boolean semanticValueEquals(Object a,Object b){
        if(a==b)return true;if(a==null||b==null||a==JSONObject.NULL||b==JSONObject.NULL)return false;if(a instanceof Number&&b instanceof Number)return Double.compare(((Number)a).doubleValue(),((Number)b).doubleValue())==0;
        String as=String.valueOf(a),bs=String.valueOf(b);if(as.equals(bs))return true;
        try{if((as.startsWith("{")&&bs.startsWith("{")))return jsonSemanticEquals(new JSONObject(as),new JSONObject(bs));if(as.startsWith("[")&&bs.startsWith("["))return jsonSemanticEquals(new JSONArray(as),new JSONArray(bs));}catch(Exception ignored){}return false;
    }
    private static boolean jsonSemanticEquals(Object a,Object b)throws Exception{if(a instanceof Number&&b instanceof Number)return Double.compare(((Number)a).doubleValue(),((Number)b).doubleValue())==0;if(a instanceof JSONObject&&b instanceof JSONObject){JSONObject x=(JSONObject)a,y=(JSONObject)b;if(x.length()!=y.length())return false;Iterator<String> keys=x.keys();while(keys.hasNext()){String k=keys.next();if(!y.has(k)||!jsonSemanticEquals(x.opt(k),y.opt(k)))return false;}return true;}if(a instanceof JSONArray&&b instanceof JSONArray){JSONArray x=(JSONArray)a,y=(JSONArray)b;if(x.length()!=y.length())return false;for(int i=0;i<x.length();i++)if(!jsonSemanticEquals(x.opt(i),y.opt(i)))return false;return true;}return semanticValueEquals(a,b);}

    private void syncResourceDice(){JSONObject s=state(),dice=rules.resourceDice(s.optInt("level",1),s.optInt("size",1));ContentValues v=new ContentValues();v.put("resource_die_count",dice.optInt("count"));v.put("resource_die_size",dice.optInt("die"));db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});}
    private void requireEnabled(){if(!enabled())throw new IllegalStateException("Ative e configure o modo Kingmaker primeiro.");}
    private String profile(){return db.getActiveMapProfile();}
    private static String logicalMapId(JSONObject map){String value=map==null?"":map.optString("logical_map_id").trim();return value.length()>0?value:map.optString("map_package_id");}
    private void audit(String title,String detail,JSONObject before,JSONObject after){try{db.addAuditEvent("kingmaker",title,detail,before==null?"{}":before.toString(),after==null?"{}":after.toString(),true);}catch(Exception e){AppLog.w("Kingmaker","Falha ao registrar auditoria Kingmaker.",e);}}
    private static int boundedStateValue(String field,int value){if("level".equals(field)||"party_level_cap".equals(field))return Math.max(1,Math.min(20,value));if("xp".equals(field))return Math.max(0,Math.min(1000000,value));if("size".equals(field)||"ruin_threshold".equals(field)||field.endsWith("_threshold"))return Math.max(1,value);if(field.endsWith("_score"))return Math.max(1,Math.min(30,value));if("control_dc_modifier".equals(field))return Math.max(-20,Math.min(20,value));if("consumption_modifier".equals(field))return Math.max(-1000,Math.min(1000,value));return Math.max(0,value);}
    private static String rank(String rank){String k=key(rank);if(k.startsWith("legend")||k.startsWith("lend"))return "legendary";if(k.startsWith("master")||k.startsWith("mestre"))return "master";if(k.startsWith("expert")||k.startsWith("especial"))return "expert";if(k.startsWith("train")||k.startsWith("trein"))return "trained";return "untrained";}
    private static int armyDefault(String field){if("level".equals(field)||"size".equals(field)||"rout_threshold".equals(field))return 1;if("ac".equals(field)||field.endsWith("_dc"))return 15;if("hp".equals(field)||"max_hp".equals(field))return 4;return 0;}
    private static int settlementRank(String type){String k=key(type);if("metropolis".equals(k)||"metropole".equals(k))return 4;if("city".equals(k)||"cidade".equals(k))return 3;if("town".equals(k)||"vila".equals(k))return 2;return 1;}
    private int structureLotTotal(String settlementKey){return db.queryOne("SELECT COALESCE(SUM(count*lots),0) AS total FROM kingmaker_structures WHERE world_profile=? AND settlement_key=?",new String[]{profile(),clean(settlementKey)}).optInt("total",0);}
    private void refreshSettlementOccupancy(String settlementKey){JSONObject town=settlement(settlementKey);int occupied=structureLotTotal(settlementKey),residential=0;for(JSONObject structure:structures(settlementKey)){try{JSONObject effects=jsonObject(structure.optString("effects_json","{}"));residential+=Math.max(0,effects.optInt("residential_lots",0))*Math.max(1,structure.optInt("count",1));}catch(Exception ignored){}}int blocks=Math.max(1,town.optInt("developed_blocks",1));ContentValues v=new ContentValues();v.put("occupied_lots",Math.min(blocks*4,occupied));v.put("residential_lots",Math.min(occupied,residential));v.put("overcrowded",occupied>0&&residential<blocks?1:0);v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_settlements",v,"world_profile=? AND settlement_key=?",new String[]{profile(),clean(settlementKey)});}
    private int influencedFarmlandCount(){int count=0;for(JSONObject hex:hexes())if(hex.optInt("claimed")==1&&hex.optInt("farmland")==1){boolean influenced=false;for(JSONObject settlement:settlements())if(hexDistance(hex.optString("hex_id"),settlement.optString("hex_id"))<=Math.max(0,settlement.optInt("influence"))){influenced=true;break;}if(influenced)count++;}return count;}
    private static int hexDistance(String a,String b){int[] ac=MapManifestGeometry.parseHexId(a),bc=MapManifestGeometry.parseHexId(b);if(ac==null||bc==null)return Integer.MAX_VALUE;int aq=ac[0],ar=ac[1]-(ac[0]-(ac[0]&1))/2,bq=bc[0],br=bc[1]-(bc[0]-(bc[0]&1))/2,ax=aq,az=ar,ay=-ax-az,bx=bq,bz=br,by=-bx-bz;return (Math.abs(ax-bx)+Math.abs(ay-by)+Math.abs(az-bz))/2;}
    private static String orderColumn(String table){if("kingmaker_state".equals(table))return "world_profile";if(table.equals("kingmaker_skills"))return "skill_key";if(table.equals("kingmaker_leaders"))return "role_key";if(table.equals("kingmaker_features"))return "feature_key";if(table.equals("kingmaker_hexes"))return "hex_id";if(table.equals("kingmaker_turns"))return "turn_number";return table.contains("settlements")?"settlement_key":table.contains("structures")?"id":table.contains("companions")?"id":table.contains("armies")?"id":table.contains("camps")?"id":"id";}
    private boolean roleVacant(String role){return db.queryOne("SELECT vacant FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),key(role)}).optInt("vacant",1)==1;}
    private int investedAbilityBonus(String ability){for(JSONObject leader:leaders())if(leader.optInt("vacant",1)==0&&leader.optInt("invested")==1&&key(ability).equals(rules.roleAbility(leader.optString("role_key"))))return rules.investedLeadershipBonus(state().optInt("level",1));return 0;}
    private int vacancyPenalty(String ability,String activity){int penalty=roleVacant("ruler")?-1:0;String a=key(ability),kind=key(activity);if("culture".equals(a)&&roleVacant("counselor"))penalty--;if("loyalty".equals(a)&&roleVacant("emissary"))penalty--;if("economy".equals(a)&&roleVacant("treasurer"))penalty--;if("stability".equals(a)&&roleVacant("viceroy"))penalty--;if("warfare".equals(kind)){if(roleVacant("general"))penalty-=4;if(roleVacant("magister"))penalty-=4;}if("region".equals(kind)&&roleVacant("warden"))penalty-=4;return penalty;}
    private JSONObject vacancySummary(){JSONObject out=new JSONObject();try{out.put("ruler",roleVacant("ruler")?"-1 em todos os testes; +2 CD de Controle; +1d4 Unrest no início do turno":"—").put("counselor",roleVacant("counselor")?"-1 em testes de Cultura":"—").put("general",roleVacant("general")?"-4 em atividades de Guerra":"—").put("emissary",roleVacant("emissary")?"-1 em testes de Lealdade":"—").put("magister",roleVacant("magister")?"-4 em atividades de Guerra":"—").put("treasurer",roleVacant("treasurer")?"-1 em testes de Economia":"—").put("viceroy",roleVacant("viceroy")?"-1 em testes de Estabilidade":"—").put("warden",roleVacant("warden")?"-4 em atividades de Região":"—");}catch(Exception ignored){}return out;}
    private static String ruinForAbility(String ability){String a=key(ability);if("culture".equals(a))return "corruption";if("economy".equals(a))return "crime";if("loyalty".equals(a))return "strife";return "decay";}
    private void gainReputationPoint(){JSONObject s=state();String field="infamy".equals(s.optString("reputation_mode"))?"infamy":"fame";int cap=s.optInt("level",1)>=20?4:3;ContentValues v=new ContentValues();v.put(field,Math.min(cap,s.optInt(field)+1));v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});}
    private void grantSettlementMilestone(String type){String k=key(type);if("metropolis".equals(k)||"metropole".equals(k))grantMilestoneOnce("first_metropolis",120,"Primeira metrópole");else if("city".equals(k)||"cidade".equals(k))grantMilestoneOnce("first_city",80,"Primeira cidade");else if("town".equals(k)||"vila".equals(k))grantMilestoneOnce("first_town",60,"Primeira vila");else grantMilestoneOnce("first_village",40,"Primeira aldeia");}
    private void grantKingdomXp(int xp,String reason){if(xp<=0)return;JSONObject current=state();ContentValues state=new ContentValues();state.put("xp",current.optInt("xp")+xp);state.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_state",state,"world_profile=?",new String[]{profile()});audit("XP do Reino concedido",clean(reason)+" • "+xp+" XP",current,state());}
    private void recordRpSpent(int amount){if(amount<=0)return;JSONObject turn=activeTurn();if(turn.length()==0||!"active".equals(turn.optString("status")))return;try{JSONObject resource=jsonObject(turn.optString("resource_roll_json","{}"));int spent=resource.optInt("spent_rp",0)+amount;resource.put("spent_rp",spent);ContentValues v=new ContentValues();v.put("resource_roll_json",resource.toString());db.getWritableDatabase().update("kingmaker_turns",v,"id=? AND world_profile=?",new String[]{String.valueOf(turn.optLong("id")),profile()});if(spent>=100)grantMilestoneOnce("spent_100_rp_turn",80,"100 RP gastos em um turno");}catch(Exception e){AppLog.w("Kingmaker","Falha ao registrar RP gasto no turno.",e);}}
    private void grantMilestoneOnce(String milestoneKey,int xp,String name){String featureKey="milestone:"+key(milestoneKey);if(db.queryOne("SELECT id FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),featureKey}).length()>0)return;long now=System.currentTimeMillis();ContentValues feature=new ContentValues();feature.put("world_profile",profile());feature.put("feature_key",featureKey);feature.put("feature_kind","milestone");feature.put("name",clean(name));feature.put("level",state().optInt("level",1));feature.put("status","completed");try{feature.put("effects_json",new JSONObject().put("xp",Math.max(0,xp)).toString());}catch(Exception ignored){feature.put("effects_json","{}");}feature.put("source_ref","Kingmaker Adventure Path — Kingdom XP milestones");feature.put("notes","Concedido automaticamente uma única vez.");feature.put("created_at",now);feature.put("updated_at",now);db.getWritableDatabase().insertOrThrow("kingmaker_features",null,feature);ContentValues state=new ContentValues();state.put("xp",this.state().optInt("xp")+Math.max(0,xp));state.put("updated_at",now);db.getWritableDatabase().update("kingmaker_state",state,"world_profile=?",new String[]{profile()});}
    private static void ensureColumn(SQLiteDatabase sql,String table,String column,String definition){Cursor cursor=null;try{cursor=sql.rawQuery("PRAGMA table_info("+table+")",null);int nameIndex=cursor.getColumnIndex("name");while(cursor.moveToNext())if(column.equals(cursor.getString(nameIndex)))return;}finally{if(cursor!=null)cursor.close();}sql.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}
    private static ContentValues values(JSONObject source){ContentValues out=new ContentValues();if(source==null)return out;Iterator<String> keys=source.keys();while(keys.hasNext()){String key=keys.next();Object value=source.opt(key);if(value==null||value==JSONObject.NULL)out.putNull(key);else if(value instanceof Boolean)out.put(key,(Boolean)value?1:0);else if(value instanceof Integer)out.put(key,(Integer)value);else if(value instanceof Long)out.put(key,(Long)value);else if(value instanceof Number)out.put(key,((Number)value).doubleValue());else out.put(key,String.valueOf(value));}return out;}
    private static JSONObject jsonObject(String raw){try{return new JSONObject(raw==null||raw.trim().length()==0?"{}":raw);}catch(Exception e){throw new IllegalArgumentException("Objeto JSON inválido.");}}
    private static JSONArray jsonArray(String raw){try{return new JSONArray(raw==null||raw.trim().length()==0?"[]":raw);}catch(Exception e){throw new IllegalArgumentException("Array JSON inválido.");}}
    private static boolean contains(JSONArray a,String value){if(a==null)return false;for(int i=0;i<a.length();i++)if(value.equalsIgnoreCase(a.optString(i)))return true;return false;}
    private static int clamp100(int value){return Math.max(0,Math.min(100,value));}
    private static String clean(String value){return value==null?"":value.replaceAll("[\\r\\n\\t]+"," ").replaceAll("\\s+"," ").trim();}
    private static String key(String value){String s=Normalizer.normalize(clean(value).toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");return s;}
}
