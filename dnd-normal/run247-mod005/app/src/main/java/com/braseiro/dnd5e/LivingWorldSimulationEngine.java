package com.braseiro.dnd5e;

import android.content.ContentValues;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

/**
 * Simulador local e determinístico do Mundo Vivo.
 *
 * O motor nunca chama IA, nunca grava durante a prévia e só confirma mudanças
 * depois que a transação da cena foi aprovada pelo jogador. Ele transforma
 * agenda, urgência, planos de facção, atividades e itinerários em ações reais
 * e auditáveis, sem inventar recompensas mecânicas.
 */
public final class LivingWorldSimulationEngine {
    private final AppDb db;
    private final LivingWorldEngine living;
    private final WorldAutonomyPlanner autonomy;
    private final GuildEngine guilds;
    private final WorldSimulationBudget budget=new WorldSimulationBudget();

    public LivingWorldSimulationEngine(AppDb database){db=database;living=new LivingWorldEngine(database);autonomy=new WorldAutonomyPlanner(database);guilds=new GuildEngine(database);}

    public JSONObject preview(JSONObject worldTick){
        JSONObject out=new JSONObject();
        try{
            JSONObject settings=living.state();
            boolean enabled=settings.optInt("enabled",1)==1;
            double hours=Math.max(0d,worldTick==null?0d:worldTick.optDouble("hours_advanced",0d));
            int dayBefore=Math.max(1,worldTick==null?living.currentDay():worldTick.optInt("day_before",living.currentDay()));
            int dayAfter=Math.max(dayBefore,worldTick==null?dayBefore:worldTick.optInt("day_after",dayBefore));
            out.put("enabled",enabled).put("hours",hours).put("day_before",dayBefore).put("day_after",dayAfter).put("preview_only",true);
            if(!enabled||hours<=0d){
                out.put("faction_actions",new JSONArray()).put("npc_actions",new JSONArray()).put("activity_updates",new JSONArray()).put("troupe_updates",new JSONArray()).put("guild_simulation",new JSONObject().put("actions",new JSONArray()).put("preview_only",true));
                return out;
            }
            boolean mystara="mystara".equals(db.getActiveMapProfile());
            double clockBefore=worldTick==null?0d:worldTick.optDouble("clock_hour_before",0d);
            double worldHourBefore=Math.max(0d,(dayBefore-1)*24d+clockBefore),worldHourAfter=worldHourBefore+hours;
            out.put("world_hour_before",worldHourBefore).put("world_hour_after",worldHourAfter);
            RuleRetrievalEngine retrievalEngine=new RuleRetrievalEngine(db);
            // O orçamento limita ENTIDADES, não corta ações depois que o tempo já foi calculado.
            // Assim nenhum NPC/facção perde horas só por estar depois na fila.
            JSONArray factionAll=previewFactions(worldHourBefore,worldHourAfter,dayAfter,retrievalEngine,mystara);
            JSONArray npcAll=previewNpcs(worldHourBefore,worldHourAfter,dayBefore,dayAfter,retrievalEngine,mystara);
            JSONArray activityAll=previewActivities(dayAfter);
            JSONArray troupeAll=previewTroupe(hours,dayBefore,dayAfter,retrievalEngine,mystara);
            JSONObject guildPreview=guilds.previewTick(worldHourBefore,worldHourAfter,dayAfter,budget.guilds);JSONArray guildAll=guildPreview.optJSONArray("actions");if(guildAll==null)guildAll=new JSONArray();
            JSONArray factionActions=factionAll,npcActions=npcAll,activityUpdates=budget.trim(activityAll,budget.activities),troupeUpdates=budget.trim(troupeAll,4);
            out.put("faction_actions",factionActions);
            out.put("npc_actions",npcActions);
            out.put("activity_updates",activityUpdates);
            out.put("troupe_updates",troupeUpdates);
            out.put("guild_simulation",guildPreview);
            new WeatherWorldConsequences(db).applyToLivingWorldPreview(out);
            out.put("simulation_budget",budget.metadata(factionAll.length(),npcAll.length(),activityAll.length(),troupeAll.length(),guildAll.length()));
            RuleRetrievalEngine.Result worldRules=retrievalEngine.retrieveForWorld(worldTick,factionActions,npcActions,activityUpdates,mystara);
            attachRuleSupport(retrievalEngine,factionActions,"faction",mystara);
            attachRuleSupport(retrievalEngine,npcActions,"npc",mystara);
            attachRuleSupport(retrievalEngine,activityUpdates,"activity",mystara);
            attachRuleSupport(retrievalEngine,troupeUpdates,"travel",mystara);
            out.put("rule_retrieval",worldRules.bundle);
        }catch(Exception e){
            try{out.put("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(Exception ignored){AppLog.ignored("LivingWorldSimulation","Falha ao registrar erro.",ignored);}
        }
        return out;
    }

    public synchronized boolean commit(final JSONObject preview){
        if(preview==null||!preview.optBoolean("preview_only",false)||preview.optBoolean("committed",false))return false;
        if(preview.optString("error","").trim().length()>0){
            try{preview.put("commit_error","Prévia inválida: "+preview.optString("error"));}catch(Exception ignored){AppLog.ignored("LivingWorldSimulation","Falha ao registrar prévia inválida.",ignored);}
            return false;
        }
        if(!preview.optBoolean("enabled",false)){
            try{preview.put("preview_only",false).put("committed",true);}catch(Exception ignored){AppLog.ignored("LivingWorldSimulation","Falha ao confirmar simulação desativada.",ignored);}
            return true;
        }
        try{
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{
                applyFactions(preview.optJSONArray("faction_actions"));
                applyNpcs(preview.optJSONArray("npc_actions"));
                applyActivities(preview.optJSONArray("activity_updates"));
                applyTroupe(preview.optJSONArray("troupe_updates"));
                guilds.applyTick(preview.optJSONObject("guild_simulation"));
                ContentValues state=new ContentValues();state.put("last_background_sync_at",System.currentTimeMillis());state.put("last_simulated_day",Math.max(1,preview.optInt("day_after",living.currentDay())));state.put("updated_at",System.currentTimeMillis());
                int changed=db.getWritableDatabase().update("living_world_state",state,"world_profile=?",new String[]{db.getActiveMapProfile()});
                if(changed!=1)throw new IllegalStateException("O estado do Mundo Vivo não foi persistido.");
                db.addAuditEvent("mundo_vivo","Simulação autônoma confirmada",summary(preview),"{}",preview.toString(),true);
            }});
            preview.put("preview_only",false).put("committed",true);preview.remove("commit_error");
            return true;
        }catch(Exception e){
            try{preview.put("committed",false).put("preview_only",true).put("commit_error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(Exception ignored){AppLog.ignored("LivingWorldSimulation","Falha ao registrar commit.",ignored);}
            AppLog.e("LivingWorldSimulation","Falha ao confirmar o Mundo Vivo; a transação foi revertida.",e);
            return false;
        }
    }

    private JSONArray previewFactions(double worldHourBefore,double worldHourAfter,int dayAfter,RuleRetrievalEngine retrievalEngine,boolean mystara)throws Exception{
        JSONArray out=new JSONArray();
        for(JSONObject faction:db.listFactionsForSimulation(budget.factions)){
            if(faction.optInt("active",1)!=1)continue;
            double cursor=faction.optDouble("simulation_cursor_hours",-1d);double effectiveHours=Math.max(0d,worldHourAfter-(cursor<0d?worldHourBefore:cursor));
            String plan=faction.optString("current_plan").trim();JSONArray configured=parseArray(faction.optString("plan_steps_json","[]"));
            int storedProgress=Math.max(0,faction.optInt("plan_progress",0));boolean storedCycle=faction.optInt("plan_cycle",1)==1;
            boolean finishedNonCyclic=configured.length()>0&&!storedCycle&&storedProgress>=configured.length();JSONObject autonomousPlan=new JSONObject();
            boolean mustCreatePlan=WorldAutonomyPlanner.needsNewFactionPlan(plan,configured.length(),storedProgress,storedCycle);int before=storedProgress;
            if(mustCreatePlan){
                JSONObject planningSeed=new JSONObject().put("name",faction.optString("name")).put("agenda",faction.optString("agenda")).put("resources",faction.optString("resources")).put("location",faction.optString("location")).put("current_plan",plan).put("rule_query","planejamento de facção agenda recursos domínio reação moral "+faction.optString("agenda"));
                RuleRetrievalEngine.Result planningRules=retrievalEngine.retrieveForWorldAction("faction_planning",planningSeed,mystara);autonomousPlan=autonomy.factionPlan(faction,dayAfter,planningRules.bundle);
                if(plan.length()==0||finishedNonCyclic)plan=autonomousPlan.optString("current_plan").trim();JSONArray generatedSteps=autonomousPlan.optJSONArray("plan_steps_json");if(generatedSteps!=null&&generatedSteps.length()>0)configured=generatedSteps;before=0;
            }
            double accumulated=Math.max(0d,faction.optDouble("plan_progress_hours",0d))+effectiveHours;
            if(plan.length()==0||configured.length()==0){out.put(new JSONObject().put("id",faction.optLong("id")).put("name",faction.optString("name")).put("cursor_only",true).put("progress_after",storedProgress).put("progress_hours_after",accumulated).put("simulation_cursor_hours_after",worldHourAfter));continue;}
            boolean generatedPlan=autonomousPlan.length()>0;double interval=Math.max(1d,generatedPlan?autonomousPlan.optDouble("plan_interval_hours",24d):faction.optDouble("plan_interval_hours",24d));int due=(int)Math.floor(accumulated/interval);
            boolean cycle=generatedPlan?autonomousPlan.optInt("plan_cycle",0)==1:storedCycle;String nextEffectKey=factionEffectKey(faction.optLong("id"),plan,before+1);
            if(due<=0||db.hasOpenPendingEffect(nextEffectKey)){out.put(new JSONObject().put("id",faction.optLong("id")).put("name",faction.optString("name")).put("cursor_only",true).put("progress_after",storedProgress).put("progress_hours_after",accumulated).put("simulation_cursor_hours_after",worldHourAfter));continue;}
            int executable=due;if(configured.length()>0&&!cycle)executable=Math.min(due,Math.max(0,configured.length()-before));executable=Math.min(executable,budget.stepsPerFaction);
            if(executable<=0){out.put(new JSONObject().put("id",faction.optLong("id")).put("name",faction.optString("name")).put("cursor_only",true).put("progress_after",storedProgress).put("progress_hours_after",accumulated).put("simulation_cursor_hours_after",worldHourAfter));continue;}
            int relationship=faction.optInt("relationship",0);String location=faction.optString("location"),resources=faction.optString("resources"),agenda=faction.optString("agenda");
            // O estado político precisa acumular dentro do mesmo tick: uma sequência de
            // guerra/rebelião/comércio não pode recalcular cada etapa a partir do snapshot antigo.
            JSONObject realmFaction=new JSONObject(faction.toString());
            for(int stepNo=0;stepNo<executable;stepNo++){
                int absolute=before+stepNo+1;JSONObject configuredStep=new JSONObject();if(configured.length()>0){int index=cycle?(absolute-1)%configured.length():absolute-1;configuredStep=configured.optJSONObject(index);if(configuredStep==null)configuredStep=new JSONObject().put("summary",String.valueOf(configured.opt(index)));}
                if(configuredStep.has("location"))location=configuredStep.optString("location",location);if(configuredStep.has("resources"))resources=configuredStep.optString("resources",resources);if(configuredStep.has("agenda"))agenda=configuredStep.optString("agenda",agenda);
                if(configuredStep.has("relationship"))relationship=Math.max(-100,Math.min(100,configuredStep.optInt("relationship",relationship)));else relationship=Math.max(-100,Math.min(100,relationship+configuredStep.optInt("relationship_delta",0)));
                String title=configuredStep.optString("title","Etapa "+absolute),summary=configuredStep.optString("summary").trim();if(summary.length()==0)summary=faction.optString("name")+" executou "+title.toLowerCase(new Locale("pt","BR"))+" do plano: "+plan+".";
                JSONObject action=new JSONObject().put("id",faction.optLong("id")).put("name",faction.optString("name")).put("plan",plan).put("step_title",title).put("steps",1).put("progress_before",absolute-1).put("progress_after",absolute)
                        // Retém backlog além do orçamento: subtrai só o que realmente executou.
                        .put("progress_hours_after",round2(Math.max(0d,accumulated-((stepNo+1)*interval)))).put("simulation_cursor_hours_after",worldHourAfter)
                        .put("day",dayAfter).put("location_after",location).put("resources_after",resources).put("agenda_after",agenda).put("relationship_after",relationship).put("summary",summary)
                        .put("rule_query",configuredStep.optString("rule_query",title+" "+summary+" facção domínio reação moral recursos")).put("effect_key",factionEffectKey(faction.optLong("id"),plan,absolute)).put("consequence_json",configuredStep.optJSONObject("consequence_json")!=null?configuredStep.optJSONObject("consequence_json"):parseObject(configuredStep.optString("consequence_json","{}")));
                if(generatedPlan&&stepNo==0)action.put("current_plan_after",plan).put("plan_steps_json_after",configured).put("plan_interval_hours_after",interval).put("plan_cycle_after",autonomousPlan.optInt("plan_cycle",0));else if(faction.optString("current_plan").trim().length()==0)action.put("current_plan_after",plan);if(!cycle&&absolute>=configured.length()&&configured.length()>0)action.put("current_plan_after",configuredStep.optString("next_plan",""));
                JSONObject realmDelta=RealmLifecycleEngine.previewAfterFactionStep(realmFaction,action,dayAfter);if(realmDelta.length()>0){
                    for(String key:new String[]{"entity_type_after","founded_day_after","ended_day_after","predecessor_after","successor_after"})if(realmDelta.has(key))action.put(key,realmDelta.opt(key));
                    if(realmDelta.has("realm_state_json_after"))action.put("realm_state_json_after",realmDelta.optJSONObject("realm_state_json_after"));
                    // Alimenta o próximo passo do mesmo tick com o resultado político atual.
                    if(action.has("entity_type_after"))realmFaction.put("entity_type",action.optString("entity_type_after"));
                    if(action.has("realm_state_json_after"))realmFaction.put("realm_state_json",action.optJSONObject("realm_state_json_after").toString());
                    if(action.has("founded_day_after"))realmFaction.put("founded_day",action.optInt("founded_day_after"));
                    if(action.has("ended_day_after"))realmFaction.put("ended_day",action.optInt("ended_day_after"));
                    realmFaction.put("current_plan",plan).put("agenda",agenda).put("relationship",relationship);
                    JSONObject political=realmDelta.optJSONObject("political_event");if(political!=null){JSONObject consequence=action.optJSONObject("consequence_json");if(consequence==null)consequence=new JSONObject();JSONArray events=consequence.optJSONArray("world_events");if(events==null)events=new JSONArray();events.put(political);consequence.put("world_events",events);action.put("consequence_json",consequence);}
                }
                out.put(action);if(!new WorldEventConsequenceEngine(db).canApplyStructured(action.optJSONObject("consequence_json")))break;
            }
        }return out;
    }

    private JSONArray previewNpcs(double worldHourBefore,double worldHourAfter,int dayBefore,int dayAfter,RuleRetrievalEngine retrievalEngine,boolean mystara)throws Exception{
        JSONArray out=new JSONArray();final int maxSchedulesPerNpc=8;int activeNpcs=0;
        // Registros históricos inativos não devem ocupar as vagas da simulação.
        for(JSONObject state:db.listNpcCognitiveStatesForSimulation(5000)){
            String name=state.optString("npc_name").trim();if(name.length()==0)continue;
            // A cognição pode sobreviver a uma morte ou desativação da memória.
            // Esse registro histórico não deve continuar produzindo ações.
            JSONObject memory=db.findNpcMemoryByName(name);
            if(memory.length()==0||memory.optInt("active",1)!=1)continue;
            if(activeNpcs++>=budget.npcs)break;
            double cursorHours=state.optDouble("simulation_cursor_hours",-1d);double effectiveHours=Math.max(0d,worldHourAfter-(cursorHours<0d?worldHourBefore:cursorHours));
            int cursorDay=state.optInt("simulation_cursor_day",0);int scheduleFrom=cursorDay>0?Math.min(cursorDay,dayAfter):dayBefore;
            int before=Math.max(0,state.optInt("urgency",0)),after=Math.min(100,before+Math.max(effectiveHours>0d?1:0,(int)Math.floor(effectiveHours/2d)));JSONArray schedules=findDueSchedules(state.optString("schedule_json","[]"),scheduleFrom,dayAfter);
            String next=state.optString("next_action").trim();int lastDay=Math.max(0,state.optInt("last_action_day",0)),runningUrgency=after,handledSchedules=0;boolean backlog=false;
            for(int i=0;i<schedules.length();i++){
                JSONObject scheduled=schedules.optJSONObject(i);if(scheduled==null)continue;int dueDay=scheduled.optInt("_due_day",dayAfter),scheduleIndex=scheduled.optInt("_schedule_index",i);String effectKey="npc:"+normalizeKey(name)+":day:"+dueDay+":schedule:"+scheduleIndex;
                if(db.hasEffectReceipt(effectKey)||db.hasOpenPendingEffect(effectKey))continue;if(handledSchedules>=maxSchedulesPerNpc){backlog=true;break;}
                String action=scheduled.optString("action",next),location=scheduled.optString("location",state.optString("current_location"));JSONObject update=new JSONObject().put("name",name).put("action",action.length()==0?"prosseguiu com seus assuntos":action).put("location",location).put("urgency_before",runningUrgency).put("urgency_after",Math.max(0,runningUrgency-35)).put("last_action_day",dueDay).put("scheduled",true).put("effect_key",effectKey).put("summary",scheduled.optString("summary",name+" "+pastTense(action)+locationSuffix(location)+".")).put("schedule_entry",scheduled).put("rule_query",scheduled.optString("rule_query","NPC agenda reação moral "+action+" "+location)).put("consequence_json",scheduled.optJSONObject("consequence_json")!=null?scheduled.optJSONObject("consequence_json"):parseObject(scheduled.optString("consequence_json","{}")));
                out.put(update);runningUrgency=Math.max(0,runningUrgency-35);handledSchedules++;
            }
            if(next.length()==0&&after>=50&&dayAfter>lastDay){JSONObject planningSeed=new JSONObject(state.toString()).put("rule_query","decisão de NPC agenda reação moral crenças facção localização "+name+" "+state.optString("beliefs_json")+" "+state.optString("current_location"));RuleRetrievalEngine.Result planningRules=retrievalEngine.retrieveForWorldAction("npc_planning",planningSeed,mystara);JSONObject generated=autonomy.npcAction(state,dayAfter,planningRules.bundle);next=generated.optString("action").trim();if(next.length()>0)state.put("_autonomous_action",generated);}
            boolean urgencyDue=handledSchedules==0&&next.length()>0&&after>=50&&dayAfter>lastDay;String autonomousKey="npc:"+normalizeKey(name)+":day:"+dayAfter+":autonomous";
            if(urgencyDue&&!db.hasEffectReceipt(autonomousKey)&&!db.hasOpenPendingEffect(autonomousKey)){JSONObject generated=state.optJSONObject("_autonomous_action");out.put(new JSONObject().put("name",name).put("action",next).put("location",generated==null?state.optString("current_location"):generated.optString("location",state.optString("current_location"))).put("urgency_before",runningUrgency).put("urgency_after",Math.max(0,runningUrgency-35)).put("last_action_day",dayAfter).put("scheduled",false).put("autonomous",generated!=null).put("effect_key",autonomousKey).put("summary",generated==null?name+" "+pastTense(next)+locationSuffix(state.optString("current_location"))+".":generated.optString("summary")).put("rule_query",generated==null?"NPC agenda reação moral "+next:generated.optString("rule_query","NPC agenda reação moral "+next)).put("consequence_json",generated==null?new JSONObject():generated.optJSONObject("consequence_json")));runningUrgency=Math.max(0,runningUrgency-35);}
            // Heartbeat sempre persiste o tempo visto pela entidade, mesmo sem ação narrativa.
            out.put(new JSONObject().put("name",name).put("pressure_only",true).put("urgency_before",before).put("urgency_after",runningUrgency).put("simulation_cursor_hours_after",worldHourAfter).put("simulation_cursor_day_after",backlog?scheduleFrom:dayAfter));
        }return out;
    }

    private JSONArray previewActivities(int dayAfter)throws Exception{
        JSONArray out=new JSONArray();
        List<JSONObject> activities=living.activitiesForSimulation(1000);
        for(JSONObject activity:activities){
            String status=activity.optString("status","planejada");
            if(!("planejada".equals(status)||"em_andamento".equals(status)||"aguardando_rolagem".equals(status)))continue;
            int start=Math.max(1,activity.optInt("start_day",1)),duration=Math.max(1,activity.optInt("duration_days",1));double total=Math.max(1d,activity.optDouble("total_units",duration)),before=Math.max(0d,Math.min(total,activity.optDouble("progress_units",0d))),earned=Math.max(0,dayAfter-start);double after=Math.max(before,Math.min(total,earned));boolean finished=after>=total-1e-7;
            JSONObject cost=parseObject(activity.optString("cost_json","{}")),reward=parseObject(activity.optString("reward_json","{}"));
            boolean mechanics=LivingWorldRulePolicy.isMechanicalJson(cost)||LivingWorldRulePolicy.isMechanicalJson(reward);
            boolean authorized=activity.optInt("rule_authorized",0)==1;
            JSONObject check=parseObject(activity.optString("check_json","{}"));
            boolean checkSatisfied=living.activityCheckSatisfied(check);
            String nextStatus=!finished?"em_andamento":(!mechanics?"concluida":(!authorized?"aguardando_regra":(checkSatisfied?"aguardando_aplicacao":"aguardando_rolagem")));
            String narrative=activity.optString("narrative_result").trim();
            if(narrative.length()==0)narrative=finished?activity.optString("character_name","O personagem")+" concluiu "+activity.optString("title")+" em "+activity.optString("location","seu destino")+".":activity.optString("character_name","O personagem")+" avançou "+round2(after)+" de "+round2(total)+" unidades em "+activity.optString("title")+".";
            out.put(new JSONObject().put("id",activity.optLong("id")).put("character_id",activity.optLong("character_id"))
                    .put("character_name",activity.optString("character_name")).put("title",activity.optString("title"))
                    .put("location",activity.optString("location")).put("start_day",start).put("duration_days",duration)
                    .put("status_before",status).put("status_after",nextStatus).put("mechanical",mechanics)
                    .put("progress_before",before).put("progress_after",after).put("total_units",total).put("partial_progress",!finished).put("finished",finished)
                    .put("authorized",authorized).put("rule_query","atividade tempo livre pré-requisitos duração custo teste CD frequência sucesso falha progresso complicações recompensa "+activity.optString("title")+" "+activity.optString("location")).put("narrative",narrative));
        }
        return out;
    }

    private JSONArray previewTroupe(double availableHours,int dayBefore,int dayAfter,RuleRetrievalEngine retrievalEngine,boolean mystara)throws Exception{
        JSONArray out=new JSONArray();JSONObject troupe=living.troupe();if(troupe.length()==0||availableHours<=0d)return out;
        JSONArray itinerary=parseArray(troupe.optString("itinerary_json","[]"));if(itinerary.length()==0)return out;
        int index=Math.max(0,Math.min(itinerary.length(),troupe.optInt("itinerary_index",0)));if(index>=itinerary.length())return out;
        Object raw=itinerary.opt(index);JSONObject stop=raw instanceof JSONObject?(JSONObject)raw:new JSONObject().put("location",String.valueOf(raw==null?"":raw));
        String destination=stop.optString("location",stop.optString("name","")).trim();if(destination.length()==0)return out;
        int explicitDay=stop.optInt("day",stop.optInt("due_day",0));if(explicitDay>dayAfter)return out;
        String current=troupe.optString("current_location");
        JSONObject saved=parseObject(troupe.optString("travel_progress_json","{}"));
        if(saved.optInt("itinerary_index",index)!=index||!destination.equals(saved.optString("destination",destination)))saved=new JSONObject();
        double distance=stop.optDouble("distance_km",0d);
        JSONArray route=stop.optJSONArray("route_hexes");
        if(route!=null&&route.length()>1)return previewHexRouteStop(troupe,stop,saved,route,index,current,destination,availableHours,dayAfter,mystara);
        if(distance<=0d&&route!=null&&route.length()>1){JSONObject map=db.getMapState();JSONObject pack=db.getMapPackageAny(map.optString("map_package_id"));distance=(route.length()-1)*Math.max(0d,pack.optDouble("distance_km",0d));}
        if(distance<=0d){out.put(new JSONObject().put("id",troupe.optLong("id")).put("name",troupe.optString("name")).put("from",current).put("to",destination).put("day",dayAfter).put("next_index",index).put("measurable",false).put("mechanical",true).put("hard_block",true).put("mechanics_blocked",true).put("mechanics_block_reason","O trecho não informa distance_km nem route_hexes com escala real; a companhia não foi teleportada.").put("rule_query","viagem distância terreno ritmo de "+current+" para "+destination));return out;}
        JSONObject map=db.getMapState();JSONObject hex=db.getMapHex(map.optString("map_package_id"),map.optString("current_hex_id"));
        String terrain=stop.optString("terrain",hex.optString("terrain_primary","open"));String pace=stop.optString("pace","Normal");boolean road=stop.optBoolean("road",hex.optInt("road",0)==1);String weather=stop.optString("weather",db.getSceneState().optString("weather",""));
        TravelRulesEngine travel=new TravelRulesEngine(db);String actionKey="living-troupe:"+troupe.optLong("id")+":"+index+":"+current+":"+destination;
        JSONObject full=mystara?travel.previewMystaraSegment(distance,terrain,stop.optString("mode","a pé"),road,stop.optBoolean("paved",false),stop.optBoolean("guide",false),stop.optBoolean("forage",false),false,stop.optInt("wind_roll",0),actionKey):travel.previewDndSegment(distance,terrain,pace,road,weather,actionKey,stop.optBoolean("extended_travel",false));
        if(full.optString("error","").length()>0||!full.optBoolean("mechanics_apply",true)){out.put(new JSONObject().put("id",troupe.optLong("id")).put("name",troupe.optString("name")).put("from",current).put("to",destination).put("next_index",index).put("measurable",true).put("mechanical",true).put("hard_block",true).put("mechanics_blocked",true).put("mechanics_block_reason",full.optString("reason",full.optString("error","As regras de viagem não puderam calcular o trecho."))).put("travel_report",full).put("rule_query","viagem deslocamento terreno ritmo marcha provisões "+terrain));return out;}
        double totalHours=full.optDouble("movement_hours",full.optDouble("hours",0d));if(totalHours<=0d)throw new IllegalStateException("As regras de viagem não produziram duração positiva.");
        JSONObject progress=TravelProgressEngine.advance(totalHours,saved.optDouble("completed_hours",0d),availableHours);double movedHours=progress.optDouble("hours_advanced",0d);double movedDistance=distance*(movedHours/totalHours);
        JSONObject applied=mystara?travel.previewMystaraSegment(movedDistance,terrain,stop.optString("mode","a pé"),road,stop.optBoolean("paved",false),stop.optBoolean("guide",false),stop.optBoolean("forage",false),false,stop.optInt("wind_roll",0),actionKey+":partial:"+progress.optDouble("completed_hours_after")):travel.previewDndSegment(movedDistance,terrain,pace,road,weather,actionKey+":partial:"+progress.optDouble("completed_hours_after"),stop.optBoolean("extended_travel",false));
        JSONObject nextProgress=new JSONObject().put("itinerary_index",index).put("destination",destination).put("distance_km",distance).put("completed_hours",progress.optDouble("completed_hours_after")).put("total_hours",totalHours).put("updated_at",System.currentTimeMillis());
        boolean completed=progress.optBoolean("completed",false);if(completed)nextProgress=new JSONObject();
        JSONObject step=new JSONObject().put("id",troupe.optLong("id")).put("name",troupe.optString("name")).put("from",current).put("to",destination).put("day",dayAfter).put("next_index",completed?index+1:index).put("measurable",true).put("completed",completed).put("distance_km",distance).put("distance_advanced_km",Math.round(movedDistance*100d)/100d).put("progress",progress).put("travel_progress_after",nextProgress).put("travel_report",applied).put("mechanical",true).put("mechanical_basis","local_travel_engine").put("rule_query","viagem deslocamento terreno ritmo marcha provisões "+terrain+" de "+current+" para "+destination);
        out.put(step);return out;
    }

    /** Avança exatamente uma aresta por prévia. Isso preserva a ordem dos
     * hexágonos e permite que o relatório do trecho seja confirmado contra o
     * estado transacional real antes de iniciar o próximo. */
    private JSONArray previewHexRouteStop(JSONObject troupe,JSONObject stop,JSONObject saved,JSONArray route,int itineraryIndex,String current,String destination,double availableHours,int dayAfter,boolean mystara)throws Exception{
        JSONArray out=new JSONArray();JSONObject map=db.getMapState();String packageId=map.optString("map_package_id");JSONObject pack=db.getMapPackageAny(packageId);double scaleKm=stop.optDouble("hex_distance_km",pack.optDouble("distance_km",0d));
        if(packageId.length()==0||scaleKm<=0d)return out.put(blockedTravelStep(troupe,current,destination,itineraryIndex,"A rota tem hexágonos, mas o atlas ativo não fornece package_id e escala real em quilômetros."));
        int routeIndex=Math.max(0,Math.min(route.length()-1,saved.optInt("route_index",0)));String fromHex=routeHexId(route.opt(routeIndex)),toHex=routeHexId(route.opt(Math.min(route.length()-1,routeIndex+1)));
        if(fromHex.length()==0||toHex.length()==0||fromHex.equals(toHex))return out.put(blockedTravelStep(troupe,current,destination,itineraryIndex,"A próxima aresta da rota contém identificador de hexágono inválido ou repetido."));
        JSONObject fromAtlas=db.getMapHex(packageId,fromHex),toAtlas=db.getMapHex(packageId,toHex);if(fromAtlas.length()==0||toAtlas.length()==0)return out.put(blockedTravelStep(troupe,current,destination,itineraryIndex,"Um dos hexágonos da próxima aresta não existe no atlas ativo: "+fromHex+" → "+toHex+"."));
        JSONArray segments=stop.optJSONArray("hex_segments");JSONObject segment=segments!=null&&routeIndex<segments.length()&&segments.optJSONObject(routeIndex)!=null?segments.optJSONObject(routeIndex):new JSONObject();
        double distance=segment.optDouble("distance_km",scaleKm);String terrain=segment.optString("terrain",toAtlas.optString("terrain_primary","open"));String roads=segment.optString("roads_json",toAtlas.optString("roads_json","[]"));boolean road=segment.optBoolean("road",roads.trim().length()>2&&!"[]".equals(roads.trim()));
        String waterways=segment.optString("waterways_json",toAtlas.optString("waterways_json","[]"));boolean riverCrossing=segment.optBoolean("river_crossing",false);String crossing=segment.optString("crossing_mode","").trim();
        if(riverCrossing&&crossing.length()==0)return out.put(blockedTravelStep(troupe,current,destination,itineraryIndex,"O trecho "+fromHex+" → "+toHex+" cruza água, mas não informa ponte, vau, barco ou outro procedimento de travessia."));
        String pace=segment.optString("pace",stop.optString("pace","Normal")),weather=segment.optString("weather",stop.optString("weather",db.getSceneState().optString("weather","")));double elevation=segment.optDouble("elevation_change_m",0d);
        TravelRulesEngine travel=new TravelRulesEngine(db);String actionKey="living-troupe:"+troupe.optLong("id")+":"+itineraryIndex+":hex:"+routeIndex+":"+fromHex+":"+toHex;
        JSONObject full=mystara?travel.previewMystaraSegment(distance,terrain,segment.optString("mode",stop.optString("mode","a pé")),road,segment.optBoolean("paved",stop.optBoolean("paved",false)),segment.optBoolean("guide",stop.optBoolean("guide",false)),segment.optBoolean("forage",stop.optBoolean("forage",false)),false,segment.optInt("wind_roll",stop.optInt("wind_roll",0)),actionKey):travel.previewDndSegment(distance,terrain,pace,road,weather,actionKey,segment.optBoolean("extended_travel",stop.optBoolean("extended_travel",false)));
        if(full.optString("error").length()>0||!full.optBoolean("mechanics_apply",true))return out.put(blockedTravelStep(troupe,current,destination,itineraryIndex,full.optString("reason",full.optString("error","O motor não calculou a aresta da rota."))).put("travel_report",full));
        double totalHours=full.optDouble("movement_hours",full.optDouble("hours",0d));if(totalHours<=0d)throw new IllegalStateException("A aresta da rota não produziu duração positiva.");
        double completedBefore=saved.optInt("route_index",routeIndex)==routeIndex?saved.optDouble("completed_hours",0d):0d;JSONObject progress=TravelProgressEngine.advance(totalHours,completedBefore,availableHours);double movedDistance=distance*progress.optDouble("hours_advanced")/totalHours;
        JSONObject applied=mystara?travel.previewMystaraSegment(movedDistance,terrain,segment.optString("mode",stop.optString("mode","a pé")),road,segment.optBoolean("paved",stop.optBoolean("paved",false)),segment.optBoolean("guide",stop.optBoolean("guide",false)),segment.optBoolean("forage",stop.optBoolean("forage",false)),false,segment.optInt("wind_roll",stop.optInt("wind_roll",0)),actionKey+":partial:"+progress.optDouble("completed_hours_after")):travel.previewDndSegment(movedDistance,terrain,pace,road,weather,actionKey+":partial:"+progress.optDouble("completed_hours_after"),segment.optBoolean("extended_travel",stop.optBoolean("extended_travel",false)));
        boolean edgeCompleted=progress.optBoolean("completed",false);int nextRouteIndex=edgeCompleted?routeIndex+1:routeIndex;boolean journeyCompleted=edgeCompleted&&nextRouteIndex>=route.length()-1;
        JSONObject context=new JSONObject().put("from_hex",fromHex).put("to_hex",toHex).put("terrain",terrain).put("road",road).put("waterways_json",waterways).put("river_crossing",riverCrossing).put("crossing_mode",crossing).put("elevation_change_m",elevation).put("weather",weather).put("pace",pace);applied.put("hex_segment",context);
        JSONObject nextProgress=journeyCompleted?new JSONObject():new JSONObject().put("itinerary_index",itineraryIndex).put("destination",destination).put("route_index",nextRouteIndex).put("route_current_hex",edgeCompleted?toHex:fromHex).put("completed_hours",edgeCompleted?0d:progress.optDouble("completed_hours_after")).put("segment_total_hours",totalHours).put("updated_at",System.currentTimeMillis());
        JSONObject step=new JSONObject().put("id",troupe.optLong("id")).put("name",troupe.optString("name")).put("from",current).put("to",destination).put("from_hex",fromHex).put("to_hex",toHex).put("route_index_before",routeIndex).put("route_index_after",nextRouteIndex).put("route_length",route.length()).put("day",dayAfter).put("next_index",journeyCompleted?itineraryIndex+1:itineraryIndex).put("measurable",true).put("completed",journeyCompleted).put("route_segment_completed",edgeCompleted).put("distance_km",distance).put("distance_advanced_km",round2(movedDistance)).put("progress",progress).put("unused_hours",progress.optDouble("unused_hours")).put("travel_progress_after",nextProgress).put("travel_report",applied).put("segment_context",context).put("mechanical",true).put("mechanical_basis","local_travel_engine_hex_by_hex").put("rule_query","viagem hexágono terreno estrada elevação rio travessia clima ritmo provisões água fadiga perder-se encontros "+terrain+" "+fromHex+" "+toHex);
        out.put(step);return out;
    }
    private static JSONObject blockedTravelStep(JSONObject troupe,String current,String destination,int index,String reason)throws Exception{return new JSONObject().put("id",troupe.optLong("id")).put("name",troupe.optString("name")).put("from",current).put("to",destination).put("next_index",index).put("measurable",false).put("mechanical",true).put("hard_block",true).put("mechanics_blocked",true).put("mechanics_block_reason",reason).put("rule_query","viagem hexágono distância terreno travessia");}
    private static String routeHexId(Object raw){if(raw instanceof JSONObject)return ((JSONObject)raw).optString("hex_id",((JSONObject)raw).optString("id")).trim();return raw==null?"":String.valueOf(raw).trim();}

    private void applyFactions(JSONArray actions)throws Exception{
        if(actions==null)return;
        WorldEventConsequenceEngine consequences=new WorldEventConsequenceEngine(db);
        Set<Long> blocked=new HashSet<Long>();
        for(int i=0;i<actions.length();i++){
            JSONObject a=actions.optJSONObject(i);if(a==null)continue;long factionId=a.optLong("id",0);if(blocked.contains(factionId))continue;
            if(a.optBoolean("cursor_only",false)){db.applyFactionSimulationStepChecked(a);continue;}
            if(a.optBoolean("weather_delayed",false)){
                db.addJournalChecked("Clima e facção — "+a.optString("name"),a.optString("summary",a.optString("weather_delay_reason")),"Mundo Vivo");
                db.addAuditEvent("clima","Etapa de facção adiada",a.optString("weather_delay_reason"),"{}",a.toString(),true);
                continue;
            }
            JSONObject effect=a.optJSONObject("consequence_json");if(effect==null)effect=new JSONObject();
            String effectKey=a.optString("effect_key",factionEffectKey(factionId,a.optString("plan"),a.optInt("progress_after",0)));
            if(a.optBoolean("mechanics_blocked",false)){
                JSONObject pending=new JSONObject().put("faction_id",factionId).put("faction_name",a.optString("name")).put("plan_step",a.optString("step_title")).put("proposed_consequence",effect).put("rule_support",a.optJSONArray("rule_support")!=null?a.optJSONArray("rule_support"):new JSONArray()).put("rule_constraints",a.optJSONArray("rule_constraints")!=null?a.optJSONArray("rule_constraints"):new JSONArray()).put("_post_apply",a).put("requires_master_review",true).put("block_reason",a.optString("mechanics_block_reason"));
                db.addPendingTransactionUnique("Plano de facção pendente — "+a.optString("name"),a.optString("mechanics_block_reason","A etapa exige regra verificável."),pending,effectKey);blocked.add(factionId);continue;
            }
            if(effect.length()>0&&!consequences.canApplyStructured(effect)){
                JSONObject pending=new JSONObject().put("faction_id",factionId).put("faction_name",a.optString("name")).put("plan_step",a.optString("step_title")).put("proposed_consequence",effect).put("rule_support",a.optJSONArray("rule_support")!=null?a.optJSONArray("rule_support"):new JSONArray()).put("_post_apply",a).put("requires_master_review",true);
                db.addPendingTransactionUnique("Plano de facção pendente — "+a.optString("name"),"A etapa contém efeito mecânico e exige regra verificável.",pending,effectKey);
                db.addAuditEvent("mundo_vivo","Efeito de facção enviado à revisão",a.optString("summary"),a.toString(),pending.toString(),false);
                blocked.add(factionId);
                continue;
            }
            if(effect.length()>0)consequences.applyStructured(effect,"Plano de facção — "+a.optString("name"),effectKey);
            db.applyFactionSimulationStepChecked(a);db.recordEffectReceipt(effectKey,"living_world_faction");
            db.addJournalChecked("Movimento de facção — "+a.optString("name"),a.optString("summary"),"Mundo Vivo");
        }
    }

    private void applyNpcs(JSONArray actions)throws Exception{
        if(actions==null)return;
        WorldEventConsequenceEngine consequences=new WorldEventConsequenceEngine(db);
        for(int i=0;i<actions.length();i++){
            JSONObject a=actions.optJSONObject(i);if(a==null)continue;
            JSONObject memory=db.findNpcMemoryByName(a.optString("name"));
            if(memory.length()==0||memory.optInt("active",1)!=1)throw new IllegalStateException("NPC inativo ou sem memória na confirmação: "+a.optString("name"));
            JSONObject state=db.getNpcCognitiveState(a.optString("name"));state.put("name",a.optString("name"));
            if(a.optBoolean("weather_delayed",false)){
                state.put("urgency",Math.min(100,a.optInt("urgency_after",Math.max(0,state.optInt("urgency",0)+8))));
                if(a.optBoolean("scheduled",false))delayNpcSchedule(state,a);
                state.put("last_interaction_summary",a.optString("summary",a.optString("weather_delay_reason")));
                db.upsertNpcCognitiveStateChecked(state);String delayedKey=a.optString("effect_key","");if(delayedKey.length()>0)db.recordEffectReceipt(delayedKey,"living_world_npc_weather_delay");
                db.addJournalChecked("Clima e agenda — "+a.optString("name"),a.optString("summary",a.optString("weather_delay_reason")),"Mundo Vivo");
                continue;
            }
            if(a.optBoolean("pressure_only",false)){
                state.put("urgency",a.optInt("urgency_after",state.optInt("urgency")));if(a.has("simulation_cursor_hours_after"))state.put("simulation_cursor_hours",a.optDouble("simulation_cursor_hours_after"));if(a.has("simulation_cursor_day_after"))state.put("simulation_cursor_day",a.optInt("simulation_cursor_day_after"));db.upsertNpcCognitiveStateChecked(state);continue;
            }
            JSONObject effect=a.optJSONObject("consequence_json");if(effect==null)effect=new JSONObject();
            String effectKey=a.optString("effect_key","npc:"+normalizeKey(a.optString("name"))+":day:"+a.optInt("last_action_day",0));
            if(a.optBoolean("mechanics_blocked",false)){
                JSONObject pending=new JSONObject().put("npc_name",a.optString("name")).put("scheduled_action",a.optString("action")).put("proposed_consequence",effect).put("rule_support",a.optJSONArray("rule_support")!=null?a.optJSONArray("rule_support"):new JSONArray()).put("rule_constraints",a.optJSONArray("rule_constraints")!=null?a.optJSONArray("rule_constraints"):new JSONArray()).put("_post_apply",a).put("requires_master_review",true).put("block_reason",a.optString("mechanics_block_reason"));
                db.addPendingTransactionUnique("Ação de NPC pendente — "+a.optString("name"),a.optString("mechanics_block_reason","A agenda exige regra verificável."),pending,effectKey);continue;
            }
            if(effect.length()>0&&!consequences.canApplyStructured(effect)){
                JSONObject pending=new JSONObject().put("npc_name",a.optString("name")).put("scheduled_action",a.optString("action")).put("proposed_consequence",effect).put("rule_support",a.optJSONArray("rule_support")!=null?a.optJSONArray("rule_support"):new JSONArray()).put("_post_apply",a).put("requires_master_review",true);
                db.addPendingTransactionUnique("Ação de NPC pendente — "+a.optString("name"),"A agenda contém efeito mecânico e exige regra verificável.",pending,effectKey);
                db.addAuditEvent("mundo_vivo","Ação de NPC enviada à revisão",a.optString("summary"),a.toString(),pending.toString(),false);
                continue;
            }
            if(effect.length()>0)consequences.applyStructured(effect,"Agenda de NPC — "+a.optString("name"),effectKey);
            state.put("urgency",a.optInt("urgency_after",state.optInt("urgency")));
            String location=a.optString("location",state.optString("current_location")).trim();
            if(location.length()==0)location=memory.optString("last_seen");
            state.put("current_location",location);
            if(a.optBoolean("scheduled",false)&&a.optJSONObject("schedule_entry")!=null&&a.optJSONObject("schedule_entry").optBoolean("_weather_delayed_resume",false))clearNpcScheduleDelay(state,a);
            state.put("last_interaction_summary",a.optString("summary"));state.put("last_action_at",System.currentTimeMillis());state.put("last_action_day",a.optInt("last_action_day"));
            // current_location é conhecimento do Mestre. last_seen na memória
            // continua sendo a última posição observada pelos personagens.
            db.addJournalChecked("Ação fora de cena — "+a.optString("name"),a.optString("summary"),"Mundo Vivo");db.upsertNpcCognitiveStateChecked(state);
            if(effectKey.length()>0)db.recordEffectReceipt(effectKey,"living_world_npc");
        }
    }

    private void applyActivities(JSONArray updates)throws Exception{
        if(updates==null)return;
        for(int i=0;i<updates.length();i++){
            JSONObject a=updates.optJSONObject(i);if(a==null)continue;
            if(a.optBoolean("weather_delayed",false)){
                int delay=Math.max(1,a.optInt("weather_delay_days",1));
                ContentValues hold=new ContentValues();hold.put("start_day",Math.max(1,a.optInt("start_day",1)+delay));hold.put("updated_at",System.currentTimeMillis());
                if(db.getWritableDatabase().update("life_activities",hold,"id=? AND world_profile=?",new String[]{String.valueOf(a.optLong("id")),db.getActiveMapProfile()})!=1)throw new IllegalStateException("O adiamento climático da atividade não foi persistido.");
                db.addJournalChecked("Clima e atividade — "+a.optString("title"),a.optString("narrative",a.optString("weather_delay_reason")),"Mundo Vivo");
                continue;
            }
            String status=a.optString("status_after");
            if(a.optBoolean("mechanical",false)&&a.optBoolean("mechanics_blocked",false))status="aguardando_regra";
            JSONObject resolution=new JSONObject().put("source","living_world_simulation").put("mechanical",a.optBoolean("mechanical")).put("authorized",a.optBoolean("authorized")).put("manual_evidence_ready",a.optBoolean("manual_evidence_ready",false)).put("rule_support",a.optJSONArray("rule_support")!=null?a.optJSONArray("rule_support"):new JSONArray());
            if(a.optBoolean("mechanics_blocked",false))resolution.put("blocked",true).put("reason",a.optString("mechanics_block_reason"));
            db.updateLifeActivityProgressChecked(a.optLong("id"),a.optDouble("progress_after",0d),a.optDouble("total_units",1d),status,a.optString("narrative"),resolution,false);
            db.addJournalChecked("Atividade — "+a.optString("title"),a.optString("narrative")+" Estado: "+status.replace('_',' ')+".","Mundo Vivo");
        }
    }

    private void applyTroupe(JSONArray updates)throws Exception{
        if(updates==null||updates.length()==0)return;
        TravelRulesEngine travel=new TravelRulesEngine(db);
        for(int i=0;i<updates.length();i++){
            JSONObject step=updates.optJSONObject(i);if(step==null)continue;
            if(step.optBoolean("weather_delayed",false)){
                db.addJournalChecked("Clima e itinerário — "+step.optString("name"),step.optString("summary",step.optString("weather_delay_reason")),"Mundo Vivo");
                continue;
            }
            if(step.optBoolean("mechanics_blocked",false)||!step.optBoolean("manual_evidence_ready",false)){
                String key="troupe:"+step.optLong("id")+":index:"+step.optInt("next_index",0)+":to:"+normalizeKey(step.optString("to"));
                JSONObject pending=new JSONObject().put("troupe_id",step.optLong("id")).put("travel_step",step).put("rule_support",step.optJSONArray("rule_support")!=null?step.optJSONArray("rule_support"):new JSONArray()).put("rule_constraints",step.optJSONArray("rule_constraints")!=null?step.optJSONArray("rule_constraints"):new JSONArray()).put("requires_master_review",true);
                db.addPendingTransactionUnique("Viagem pendente — "+step.optString("name"),step.optString("mechanics_block_reason","A etapa de viagem não possui procedimento mecânico verificável nos manuais indexados."),pending,key);return;
            }
            JSONObject report=step.optJSONObject("travel_report");if(report==null||report.length()==0)throw new IllegalStateException("Relatório mecânico da viagem ausente.");
            if(!travel.commit(report))throw new IllegalStateException("As regras de viagem rejeitaram o relatório parcial do trecho.");
            ContentValues v=new ContentValues();v.put("travel_progress_json",step.optJSONObject("travel_progress_after")==null?"{}":step.optJSONObject("travel_progress_after").toString());
            if(step.optBoolean("route_segment_completed",false)){
                JSONObject map=db.getMapState();String toHex=step.optString("to_hex");if(db.getMapHex(map.optString("map_package_id"),toHex).length()==0)throw new IllegalStateException("O hexágono concluído deixou de existir no atlas antes da confirmação.");
                db.applyMapDirectorUpdate(new JSONObject().put("current_hex_id",toHex).put("position_locked",true));
            }
            if(step.optBoolean("completed",false)){v.put("current_location",step.optString("to"));v.put("itinerary_index",step.optInt("next_index",0));v.put("last_travel_day",step.optInt("day",living.currentDay()));}
            v.put("updated_at",System.currentTimeMillis());int changed=db.getWritableDatabase().update("living_troupes",v,"id=? AND world_profile=?",new String[]{String.valueOf(step.optLong("id")),db.getActiveMapProfile()});if(changed!=1)throw new IllegalStateException("O progresso do itinerário não foi persistido.");
            String body=step.optBoolean("completed",false)?"A companhia concluiu o trecho de "+step.optString("from")+" para "+step.optString("to")+".":(step.optBoolean("route_segment_completed",false)?"A companhia concluiu a aresta "+step.optString("from_hex")+" → "+step.optString("to_hex")+"; o destino do itinerário ainda não foi alcançado.":"A companhia avançou "+step.optDouble("distance_advanced_km",0d)+" km no trecho entre "+step.optString("from")+" e "+step.optString("to")+"; a localização final ainda não mudou.");
            db.addJournalChecked("Itinerário — "+step.optString("name"),body,"Mundo Vivo");
        }
    }

    /**
     * Liga cada ação autônoma às passagens recuperadas no mesmo avanço.
     * Ações narrativas podem prosseguir sem prova; qualquer consequência
     * mecânica continua obrigatoriamente no fluxo de revisão do Mestre.
     */
    private void attachRuleSupport(RuleRetrievalEngine retrievalEngine,JSONArray actions,String kind,boolean mystara)throws Exception{
        if(actions==null)return;
        for(int i=0;i<actions.length();i++){
            JSONObject action=actions.optJSONObject(i);if(action==null)continue;
            RuleRetrievalEngine.Result result=retrievalEngine.retrieveForWorldAction(kind,action,mystara);JSONObject bundle=result.bundle;
            JSONArray evidence=bundle.optJSONArray("evidence");JSONArray support=new JSONArray();if(evidence!=null)for(int j=0;j<evidence.length()&&j<4;j++)support.put(evidence.opt(j));
            JSONArray structured=bundle.optJSONArray("structured_rules");if(structured==null)structured=new JSONArray();
            action.put("rule_support",support).put("rule_constraints",compactWorldRules(structured,6)).put("rule_support_kind",kind)
                    .put("manual_evidence_ready",bundle.optBoolean("sufficient_for_mechanics",false)).put("rule_retrieval_status",bundle.optString("status"));
            JSONObject consequence=action.optJSONObject("consequence_json");
            boolean mechanical=action.optBoolean("mechanical",false)||LivingWorldRulePolicy.isMechanicalJson(consequence);
            if(action.optBoolean("hard_block",false)){action.put("mechanics_blocked",true);}
            else if(mechanical&&!bundle.optBoolean("sufficient_for_mechanics",false))action.put("mechanics_blocked",true).put("mechanics_block_reason",bundle.optString("instruction","Nenhuma regra pertinente e executável foi recuperada para esta ação."));
            else action.remove("mechanics_blocked");
        }
    }

    private static JSONArray compactWorldRules(JSONArray rules,int max)throws Exception{
        JSONArray out=new JSONArray();if(rules==null)return out;
        for(int i=0;i<rules.length()&&out.length()<max;i++){
            JSONObject rule=rules.optJSONObject(i);if(rule==null)continue;JSONObject compact=new JSONObject();
            for(String key:new String[]{"rule_id","intents","atomic_procedures","mechanic_keys","ambiguity_flags","checks","effects","conditions","triggers","exceptions","procedure_steps","action_economy","durations","ranges","targets","success_clauses","failure_clauses","confidence","executable","requires_local_roll","source"})if(rule.has(key))compact.put(key,rule.opt(key));out.put(compact);
        }
        return out;
    }

    private static String normalizeText(String value){if(value==null)return "";return java.text.Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}

    private static void delayNpcSchedule(JSONObject state,JSONObject action)throws Exception{
        JSONObject due=action.optJSONObject("schedule_entry");if(due==null)return;
        int index=due.optInt("_schedule_index",-1);if(index<0)return;
        JSONArray schedule=parseArray(state.optString("schedule_json","[]"));if(index>=schedule.length())return;
        JSONObject entry=schedule.optJSONObject(index);if(entry==null)return;
        int original=Math.max(1,due.optInt("_due_day",action.optInt("last_action_day",1)));
        int until=Math.max(original+1,action.optInt("weather_delay_until_day",original+1));
        entry.put("weather_delayed_due_day",original).put("weather_delay_until_day",until).put("weather_delay_count",Math.max(0,entry.optInt("weather_delay_count",0))+1);
        state.put("schedule_json",schedule.toString());
    }
    private static void clearNpcScheduleDelay(JSONObject state,JSONObject action)throws Exception{
        JSONObject due=action.optJSONObject("schedule_entry");if(due==null)return;
        int index=due.optInt("_schedule_index",-1);if(index<0)return;
        JSONArray schedule=parseArray(state.optString("schedule_json","[]"));if(index>=schedule.length())return;
        JSONObject entry=schedule.optJSONObject(index);if(entry==null)return;
        entry.remove("weather_delayed_due_day");entry.remove("weather_delay_until_day");entry.remove("weather_delay_count");state.put("schedule_json",schedule.toString());
    }

    private static JSONArray findDueSchedules(String raw,int dayBefore,int dayAfter){
        JSONArray out=new JSONArray(),schedule=parseArray(raw);Set<String> seen=new HashSet<String>();
        for(int i=0;i<schedule.length()&&out.length()<4096;i++){
            JSONObject entry=schedule.optJSONObject(i);if(entry==null)continue;
            int delayedOriginal=Math.max(0,entry.optInt("weather_delayed_due_day",0));
            int delayedUntil=Math.max(0,entry.optInt("weather_delay_until_day",0));
            if(delayedUntil>0&&delayedUntil>dayBefore&&delayedUntil<=dayAfter){
                try{addDueSchedule(out,seen,new JSONObject(entry.toString()).put("_weather_delayed_resume",true).put("_weather_original_due_day",delayedOriginal),i,delayedUntil);}catch(Exception ignored){AppLog.ignored("LivingWorldSimulation","Agenda climática inválida ignorada.",ignored);}
            }
            int day=entry.optInt("day",entry.optInt("due_day",0));
            if(day>dayBefore&&day<=dayAfter&&day!=delayedOriginal)addDueSchedule(out,seen,entry,i,day);
            int repeat=Math.max(0,entry.optInt("repeat_days",0)),start=Math.max(1,entry.optInt("start_day",day));
            if(repeat>0){
                int first=start;if(first<=dayBefore){int jumps=(dayBefore-first)/repeat+1;first+=jumps*repeat;}
                for(int due=first;due<=dayAfter&&out.length()<4096;due+=repeat)if(due!=delayedOriginal)addDueSchedule(out,seen,entry,i,due);
            }
        }
        return out;
    }

    private static void addDueSchedule(JSONArray out,Set<String> seen,JSONObject entry,int index,int due){
        String key=index+"|"+due;if(!seen.add(key))return;
        try{out.put(new JSONObject(entry.toString()).put("_due_day",due).put("_schedule_index",index));}
        catch(Exception error){AppLog.w("LivingWorldSimulation","Agenda inválida ignorada.",error);}
    }
    private static String factionEffectKey(long factionId,String plan,int step){
        String normalized=normalizeKey(plan);if(normalized.length()>42)normalized=normalized.substring(0,42);
        String fingerprint=Integer.toHexString((plan==null?"":plan.trim().toLowerCase(Locale.ROOT)).hashCode());
        return "faction:"+factionId+":plan:"+(normalized.length()==0?"sem-plano":normalized)+":"+fingerprint+":step:"+Math.max(0,step);
    }
    private static String normalizeKey(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");}
    private static String pastTense(String action){String a=action==null?"":action.trim();if(a.length()==0)return "prosseguiu com seus assuntos";String lower=a.toLowerCase(new Locale("pt","BR"));if(lower.startsWith("vai "))return a.substring(4);if(lower.startsWith("irá "))return a.substring(4);if(lower.startsWith("pretende "))return a.substring(9);return a;}
    private static String locationSuffix(String location){return location==null||location.trim().length()==0?"":" em "+location.trim();}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw==null?"[]":raw);}catch(Exception e){return new JSONArray();}}
    private static double round2(double value){return Math.round(value*100d)/100d;}
    private static String summary(JSONObject p){return length(p,"faction_actions")+" ação(ões) de facção • "+length(p,"npc_actions")+" NPC(s) • "+length(p,"activity_updates")+" atividade(s) • "+length(p,"troupe_updates")+" etapa(s) de itinerário • "+weatherDelayedCount(p)+" adiamento(s) climático(s)";}
    private static int weatherDelayedCount(JSONObject p){int n=0;for(String k:new String[]{"faction_actions","npc_actions","activity_updates","troupe_updates"}){JSONArray a=p==null?null:p.optJSONArray(k);if(a==null)continue;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&x.optBoolean("weather_delayed",false))n++;}}return n;}
    private static int length(JSONObject p,String key){JSONArray a=p==null?null:p.optJSONArray(key);return a==null?0:a.length();}
}
