package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

/**
 * Mundo persistente e determinístico. O avanço é calculado antes da chamada,
 * mas só é gravado depois que uma resposta válida do Mestre foi aceita.
 * Assim, falhas de rede nunca consomem provisões nem avançam relógios.
 */
public final class WorldStateEngine {
    private final AppDb db;
    private final TravelRulesEngine travelRules;
    private final LivingWorldSimulationEngine livingSimulation;
    private final WorldEventConsequenceEngine eventConsequences;
    public WorldStateEngine(AppDb database){
        db=database;
        travelRules=new TravelRulesEngine(database);
        livingSimulation=new LivingWorldSimulationEngine(database);
        eventConsequences=new WorldEventConsequenceEngine(database);
    }

    public JSONObject preview(String playerAction){
        JSONObject report=new JSONObject();try{
            int requestedHours=inferHours(playerAction);
            JSONObject scene=db.getSceneState();
            int currentDay=Math.max(1,scene.optInt("day_number",1));
            double currentHour=normalizeHour(scene.optDouble("clock_hour",hourFromLabel(scene.optString("time_of_day",""))));
            JSONObject travelPreview=travelRules.previewNarrativeAction(playerAction,requestedHours);
            double elapsedHours=requestedHours;
            if(travelPreview.optBoolean("requires_map_segment",false))elapsedHours=0d;
            else if(travelPreview.optBoolean("mechanics_apply",false))elapsedHours=Math.max(0d,travelPreview.optDouble("elapsed_hours",travelPreview.optDouble("movement_hours",requestedHours)));
            double total=currentHour+elapsedHours;
            int projectedDay=currentDay+(int)Math.floor(total/24d);
            double projectedHour=normalizeHour(total);
            report.put("hours_requested",requestedHours);report.put("hours_advanced",round2(elapsedHours));report.put("long_rest",isLongRest(playerAction));report.put("day_before",currentDay);report.put("day_after",projectedDay);report.put("clock_hour_before",round2(currentHour));report.put("clock_hour_after",round2(projectedHour));report.put("time_before",scene.optString("time_of_day",labelForHour(currentHour)));report.put("time_after",labelForHour(projectedHour));report.put("travel_rules",travelPreview);

            JSONObject resourcePreview=travelPreview.optBoolean("mechanics_apply",false)?travelPreview.optJSONObject("resource_consumption"):travelRules.previewElapsedTimeResources(elapsedHours,"world-time|"+playerAction+"|"+currentDay+"|"+currentHour);report.put("company_resources_consumed",resourcePreview==null?new JSONObject():resourcePreview);

            boolean livingEnabled=new LivingWorldEngine(db).state().optInt("enabled",1)==1;
            report.put("living_world_enabled",livingEnabled);
            JSONArray triggered=new JSONArray();
            if(livingEnabled){
                long projectedNow=System.currentTimeMillis()+(long)Math.floor(Math.max(0d,elapsedHours)*60d*60d*1000d);
                for(JSONObject event:db.listUpcomingWorldEvents(80)){
                    long dueAt=event.optLong("due_at",0);int dueDay=event.optInt("due_day",0);
                    if((dueAt>0&&dueAt<=projectedNow)||(dueDay>0&&dueDay<=projectedDay))triggered.put(event);
                }
            }
            report.put("events_triggered",triggered);
            report.put("event_consequence_preview",eventConsequences.preview(triggered));

            JSONArray advanced=new JSONArray();
            if(livingEnabled&&elapsedHours>0d)for(JSONObject clock:db.listWorldClocks(true,60)){
                if(clock.optInt("automatic",0)!=1)continue;
                if(clock.optInt("paused",0)==1)continue;
                if(!clockConditionMatches(clock,playerAction,scene))continue;
                int before=clock.optInt("current_value",0),max=Math.max(1,clock.optInt("max_value",4));
                double interval=Math.max(1d,clock.optDouble("interval_hours",6d));
                double rate=Math.max(.01d,Math.min(100d,clock.optDouble("rate_multiplier",1d))),accumulated=Math.max(0d,clock.optDouble("accumulated_hours",0d))+elapsedHours*rate;
                int ticks=Math.max(0,(int)Math.floor(accumulated/interval)),tickDelta=clock.optInt("tick_delta",1);if(tickDelta<0&&clock.optInt("regression_allowed",0)!=1)tickDelta=0;
                int delta=ticks*tickDelta;double remainder=accumulated-(ticks*interval);int after=Math.max(0,Math.min(max,before+delta));JSONArray segmentEffects=clockSegmentEffects(clock,before,after);
                if(delta!=0||Math.abs(remainder-clock.optDouble("accumulated_hours",0d))>.001d){
                    advanced.put(new JSONObject().put("id",clock.optLong("id")).put("name",clock.optString("name"))
                            .put("before",before).put("after",after).put("max",max).put("delta",delta)
                            .put("interval_hours",interval).put("rate_multiplier",rate).put("tick_delta",tickDelta).put("accumulated_after",round2(remainder))
                            .put("owner_entity",clock.optString("owner_entity")).put("segment_effects",segmentEffects).put("day",projectedDay).put("completed",after>=max));
                }
            }
            report.put("clocks_advanced",advanced);
            report.put("living_world_simulation",livingSimulation.preview(report));
            report.put("preview_only",true);
        }catch(Exception error){try{report.put("error",error.getMessage()==null?"Falha desconhecida":error.getMessage());}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha tolerada; a execução continuou.",ignored);}}return report;
    }

    public synchronized boolean commit(final JSONObject report){
        if(report==null||report.optBoolean("committed",false))return false;
        if(report.optString("error","").trim().length()>0){
            try{report.put("commit_error","A prévia do mundo contém erro e não pode ser aplicada: "+report.optString("error"));}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha ao registrar prévia inválida.",ignored);}
            return false;
        }
        JSONObject preSimulation=report.optJSONObject("living_world_simulation");
        if(preSimulation!=null&&preSimulation.optString("error","").trim().length()>0){
            try{report.put("commit_error","A simulação do Mundo Vivo contém erro e não pode ser aplicada: "+preSimulation.optString("error"));}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha ao registrar simulação inválida.",ignored);}
            return false;
        }
        try{
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{
                JSONObject travel=report.optJSONObject("travel_rules");
                if(travel!=null&&travel.optBoolean("mechanics_apply",false)&&!travelRules.commit(travel))throw new Exception("As regras de viagem não puderam ser aplicadas.");
                if(travel==null||!travel.optBoolean("mechanics_apply",false)){
                    JSONObject resources=report.optJSONObject("company_resources_consumed");
                    if(resources!=null&&resources.optBoolean("mechanics_apply",false)&&!travelRules.commit(resources))throw new Exception("Os recursos da companhia não puderam ser aplicados.");
                }
                double hours=Math.max(0d,report.optDouble("hours_advanced",0d));
                if(hours>0d){
                    JSONObject scene=db.getSceneState();
                    scene.put("day_number",Math.max(1,report.optInt("day_after",scene.optInt("day_number",1))));
                    scene.put("clock_hour",normalizeHour(report.optDouble("clock_hour_after",scene.optDouble("clock_hour",9d))));
                    scene.put("time_of_day",report.optString("time_after",scene.optString("time_of_day","Manhã")));
                    db.saveSceneStateChecked(scene);
                }

                JSONArray clocks=report.optJSONArray("clocks_advanced");
                if(clocks!=null)for(int i=0;i<clocks.length();i++){
                    JSONObject clock=clocks.optJSONObject(i);
                    if(clock!=null&&clock.optLong("id",0)>0){db.updateWorldClockSimulationChecked(clock.optLong("id"),clock.optInt("after"),clock.optDouble("accumulated_after",0d),clock.optInt("day",report.optInt("day_after",1)));applyClockSegmentEffects(clock);}
                }

                JSONArray events=report.optJSONArray("events_triggered");
                if(events!=null&&!eventConsequences.applyTriggered(events))report.put("event_consequences_pending",true);

                JSONObject simulation=report.optJSONObject("living_world_simulation");
                if(simulation!=null&&!livingSimulation.commit(simulation))throw new Exception("A simulação do Mundo Vivo não pôde ser confirmada.");

                if(report.optBoolean("long_rest",false)){
                    JSONObject travelState=db.getTravelRulesState();double restStart=Math.max(0d,report.optInt("day_before",1)*24d+report.optDouble("clock_hour_before",0d));double lastRest=travelState.optDouble("last_long_rest_start_hour",-100000d);
                    if(restStart-lastRest>=24d){report.put("long_rest_natural_healing",db.applyPf2LongRestNaturalHealing());travelState.put("last_long_rest_start_hour",restStart);db.saveTravelRulesStateChecked(travelState);}else report.put("long_rest_recovery_skipped","PF2e permite este período de descanso apenas uma vez a cada 24 horas.");
                    travelRules.resetDailyTravelChecked();
                }
                db.addAuditEvent("mundo","Avanço local do mundo",summary(report),"{}",report.toString(),true);
            }});
            report.put("preview_only",false);report.put("committed",true);report.remove("commit_error");
            try{report.put("world_6",new WorldSixCoordinator(db).tick());}catch(Exception sixError){AppLog.w("WorldStateEngine","O mundo 6.0 será retomado no próximo ciclo.",sixError);}
            return true;
        }catch(Exception error){
            try{
                JSONObject simulation=report.optJSONObject("living_world_simulation");if(simulation!=null)simulation.put("committed",false).put("preview_only",true);
                report.put("committed",false).put("preview_only",true).put("commit_error",error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());
                db.addAuditEvent("erro_mundo","Falha ao avançar o mundo",String.valueOf(error.getMessage()),"{}",report.toString(),false);
            }catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha ao registrar erro transacional.",ignored);}
            return false;
        }
    }

    public String compactContext(){return compactContext("");}
    public String compactContext(String focus){
        JSONObject out=new JSONObject();try{
            out.put("quests",new JSONArray(db.listQuests("active",16)));out.put("factions_director_only",new JSONArray(db.listFactions(12)));out.put("clocks",new JSONArray(db.listWorldClocks(true,20)));out.put("upcoming_events_director_only",new JSONArray(db.listUpcomingWorldEvents(16)));
            JSONArray visible=new JSONArray(),hidden=new JSONArray();String normalized=normalize(focus);for(JSONObject fact:db.listKnowledgeFacts(80)){
                boolean relevant=isRelevantFact(fact,normalized);String visibility=fact.optString("visibility");if("mestre".equals(visibility)){if(hidden.length()<8&&(relevant||hidden.length()<3))hidden.put(compactFact(fact));}else if(visible.length()<14&&(relevant||visible.length()<6))visible.put(compactFact(fact));
            }
            out.put("visible_knowledge",visible);out.put("director_only_hidden_facts",hidden);out.put("hidden_facts_rule","Use para planejar, nunca revele diretamente antes da condição de revelação.");
        }catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha tolerada; a execução continuou.",ignored);}return fitJson(out,6200);
    }

    private static JSONObject compactFact(JSONObject fact){JSONObject out=new JSONObject();try{out.put("subject",fact.optString("subject"));out.put("content",trim(fact.optString("content"),420));out.put("truth_status",fact.optString("truth_status"));out.put("visibility",fact.optString("visibility"));out.put("known_by_json",fact.optString("known_by_json","[]"));out.put("reveal_condition",trim(fact.optString("reveal_condition"),220));out.put("source",trim(fact.optString("source"),160));}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha tolerada; a execução continuou.",ignored);}return out;}
    private static boolean isRelevantFact(JSONObject fact,String focus){if(focus.length()==0)return false;String hay=normalize(fact.optString("subject")+" "+fact.optString("content")+" "+fact.optString("source"));for(String term:focus.split("\\s+"))if(term.length()>=4&&hay.contains(term))return true;return false;}
    private static String normalize(String value){if(value==null)return "";String n=java.text.Normalizer.normalize(value.toLowerCase(new java.util.Locale("pt","BR")),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static String trim(String value,int max){String s=value==null?"":value.trim();return s.length()<=max?s:s.substring(0,max)+"…";}

    private boolean clockConditionMatches(JSONObject clock,String action,JSONObject scene){
        String raw=clock==null?"":clock.optString("trigger_condition","").trim();if(raw.length()==0)return true;
        String condition=normalize(raw);if(condition.length()==0||"sempre".equals(condition)||"always".equals(condition))return true;
        String actionText=normalize(action),location=normalize(scene==null?"":scene.optString("location")),danger=normalize(scene==null?"":scene.optString("danger"));
        String objective=normalize(scene==null?"":scene.optString("objective")),weather=normalize(scene==null?"":scene.optString("weather"));
        String threats=normalize(scene==null?"":scene.optString("threats_json"));
        if(condition.startsWith("acao "))return actionText.contains(condition.substring(5).trim());
        if(condition.startsWith("action "))return actionText.contains(condition.substring(7).trim());
        if(condition.startsWith("local "))return location.contains(condition.substring(6).trim());
        if(condition.startsWith("location "))return location.contains(condition.substring(9).trim());
        if(condition.startsWith("perigo "))return danger.contains(condition.substring(7).trim())||threats.contains(condition.substring(7).trim());
        String hay=actionText+" "+location+" "+danger+" "+objective+" "+weather+" "+threats;
        for(String alternative:condition.split("\\bor\\b|\\bou\\b|\\|")){String token=alternative.trim();if(token.length()>=3&&hay.contains(token))return true;}
        return false;
    }

    private JSONArray clockSegmentEffects(JSONObject clock,int before,int after)throws Exception{
        JSONArray out=new JSONArray();JSONArray configured=parseArray(clock.optString("segments_json","[]"));int low=Math.min(before,after),high=Math.max(before,after);
        for(int i=0;i<configured.length();i++){JSONObject segment=configured.optJSONObject(i);if(segment==null)continue;int value=segment.optInt("value",segment.optInt("segment",-1));if(value<=low||value>high)continue;JSONObject copy=new JSONObject(segment.toString()).put("clock_id",clock.optLong("id")).put("direction",after>=before?"advance":"regress").put("effect_key","clock:"+clock.optLong("id")+":segment:"+value+":"+(after>=before?"up":"down"));out.put(copy);}
        if(after>=clock.optInt("max_value",4)&&before<clock.optInt("max_value",4)&&clock.optString("consequence").trim().length()>0)out.put(new JSONObject().put("clock_id",clock.optLong("id")).put("value",after).put("title",clock.optString("name")+" concluído").put("narrative",clock.optString("consequence")).put("effect_key","clock:"+clock.optLong("id")+":completion"));
        return out;
    }
    private void applyClockSegmentEffects(JSONObject clock)throws Exception{
        JSONArray effects=clock.optJSONArray("segment_effects");if(effects==null)return;for(int i=0;i<effects.length();i++){JSONObject effect=effects.optJSONObject(i);if(effect==null)continue;String key=effect.optString("effect_key");if(key.length()==0)continue;if(db.hasEffectReceipt(key)){if(key.endsWith(":completion"))db.setWorldClockCompletionReceipt(clock.optLong("id"),key);continue;}JSONObject consequence=effect.optJSONObject("consequence_json");if(consequence==null)consequence=parseObject(effect.optString("consequence_json","{}"));if(consequence.length()==0){consequence=new JSONObject().put("journal_updates",new JSONArray().put(new JSONObject().put("title",effect.optString("title",clock.optString("name"))).put("body",effect.optString("narrative","Segmento do relógio alcançado.")).put("kind","Relógio")));}
            if(eventConsequences.canApplyStructured(consequence)){eventConsequences.applyStructured(consequence,"Relógio — "+clock.optString("name"),key);if(key.endsWith(":completion")&&db.hasEffectReceipt(key))db.setWorldClockCompletionReceipt(clock.optLong("id"),key);}else db.addPendingTransactionUnique("Relógio pendente — "+clock.optString("name"),"A consequência do segmento exige procedimento mecânico e revisão.",new JSONObject().put("clock_id",clock.optLong("id")).put("segment",effect).put("proposed_consequence",consequence).put("requires_master_review",true),key);
        }
    }

    private static boolean isLongRest(String action){String n=action==null?"":action.toLowerCase(Locale.ROOT);return n.contains("descanso longo")||n.contains("durmo")||n.contains("dormimos")||n.contains("passamos a noite");}
    private static int inferHours(String action){
        String n=action==null?"":action.toLowerCase(Locale.ROOT);
        if(n.contains("mundo_vivo_avanco_confirmado:")){
            java.util.regex.Matcher confirmed=java.util.regex.Pattern.compile("mundo_vivo_avanco_confirmado:\\s*(\\d{1,5})\\s*dias").matcher(n);
            if(confirmed.find())try{return Math.min(876000,Integer.parseInt(confirmed.group(1))*24);}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Avanço explícito inválido.",ignored);}
        }
        java.util.regex.Matcher weeks=java.util.regex.Pattern.compile("(\\d{1,3})\\s*semanas?").matcher(n);
        if(weeks.find())try{return Math.min(876000,Integer.parseInt(weeks.group(1))*7*24);}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Semanas inválidas.",ignored);}
        java.util.regex.Matcher days=java.util.regex.Pattern.compile("(\\d{1,5})\\s*dias?").matcher(n);
        if(days.find())try{return Math.min(876000,Integer.parseInt(days.group(1))*24);}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Dias inválidos.",ignored);}
        if(n.contains("descanso longo")||n.contains("durmo")||n.contains("dormimos")||n.contains("passamos a noite"))return 8;
        java.util.regex.Matcher hours=java.util.regex.Pattern.compile("(\\d{1,4})\\s*horas?").matcher(n);
        if(hours.find())try{return Math.min(876000,Integer.parseInt(hours.group(1)));}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Horas inválidas.",ignored);}
        java.util.regex.Matcher minutes=java.util.regex.Pattern.compile("(\\d{1,4})\\s*minutos?").matcher(n);
        if(minutes.find())try{return Integer.parseInt(minutes.group(1))>=30?1:0;}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Minutos inválidos.",ignored);}
        return 0;
    }
    private static double hourFromLabel(String value){String n=value==null?"":value.toLowerCase(Locale.ROOT);if(n.contains("madrugada"))return 3;if(n.contains("amanhe"))return 6;if(n.contains("manhã")||n.contains("manha"))return 9;if(n.contains("meio-dia")||n.contains("meio dia"))return 12;if(n.contains("tarde"))return 15;if(n.contains("entard"))return 18;if(n.contains("noite"))return 21;return 9;}
    private static String labelForHour(double hour){if(hour<5)return "Madrugada";if(hour<8)return "Amanhecer";if(hour<12)return "Manhã";if(hour<14)return "Meio-dia";if(hour<18)return "Tarde";if(hour<20)return "Entardecer";return "Noite";}
    private static double normalizeHour(double hour){double x=hour%24d;if(x<0)x+=24d;return x;}
    private static double round2(double value){return Math.round(value*100d)/100d;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw);}catch(Exception e){return new JSONObject();}}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw);}catch(Exception e){return new JSONArray();}}
    private static String fitJson(JSONObject source,int max){String raw=source.toString();if(raw.length()<=max)return raw;try{JSONObject lean=new JSONObject();lean.put("quests",source.optJSONArray("quests"));lean.put("clocks",source.optJSONArray("clocks"));lean.put("visible_knowledge",source.optJSONArray("visible_knowledge"));lean.put("director_only_hidden_facts",source.optJSONArray("director_only_hidden_facts"));lean.put("hidden_facts_rule",source.optString("hidden_facts_rule"));lean.put("context_reduced",true);raw=lean.toString();if(raw.length()<=max)return raw;return new JSONObject().put("context_reduced",true).put("excerpt",raw.substring(0,Math.min(max-80,raw.length()))).toString();}catch(Exception e){return "{\"context_reduced\":true}";}}
    private static String summary(JSONObject report){return report.optDouble("hours_advanced",0)+" hora(s) • dia "+report.optInt("day_before",1)+" → "+report.optInt("day_after",1)+" • "+(report.optJSONArray("events_triggered")==null?0:report.optJSONArray("events_triggered").length())+" evento(s) • "+(report.optJSONArray("clocks_advanced")==null?0:report.optJSONArray("clocks_advanced").length())+" relógio(s)";}
}
