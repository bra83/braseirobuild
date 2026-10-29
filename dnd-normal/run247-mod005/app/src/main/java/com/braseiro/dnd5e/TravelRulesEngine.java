package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Autoridade local para viagem e efeitos ambientais.
 *
 * Regras automáticas são separadas de regras opcionais, decisões do Mestre e
 * simulação narrativa. O motor nunca transforma uma sugestão do Diretor
 * Ambiental em penalidade mecânica antes que o fenômeno esteja ativo.
 */
public final class TravelRulesEngine {
    public static final String RULE_RAW="RAW";
    public static final String RULE_OPTIONAL="REGRA_OPCIONAL";
    public static final String RULE_DM="DECISAO_DO_MESTRE";
    public static final String RULE_NARRATIVE="SIMULACAO_NARRATIVA";

    private final AppDb db;
    private final DndRulesEngine dnd=new DndRulesEngine();
    private final MystaraRulesEngine mystara=new MystaraRulesEngine();

    public TravelRulesEngine(AppDb database){db=database;}

    public JSONObject previewNarrativeAction(String action,int explicitHours){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            boolean travel=isTravel(action);
            int hours=Math.max(0,explicitHours);
            out.put("travel_detected",travel);
            out.put("explicit_hours",hours);
            out.put("mechanics_apply",travel&&hours>0);
            out.put("classification",travel&&hours>0?RULE_RAW:RULE_NARRATIVE);
            if(!travel){out.put("reason","A ação não foi identificada como viagem.");return out;}
            if(isMystaraWorld()){
                out.put("mechanics_apply",false);
                out.put("classification",RULE_NARRATIVE);
                out.put("reason","Em Mystara, a posição, a escala do mapa e a fração do dia precisam ser resolvidas pelo comando de viagem do mapa; uma frase narrativa não aplica movimento nem rolagens secretas.");
                out.put("requires_map_segment",true);
                return out;
            }
            if(hours<=0){
                out.put("reason","A duração ou a distância não foi confirmada; nenhuma regra de consumo, marcha prolongada ou clima foi aplicada.");
                out.put("requires_distance_or_time",true);
                return out;
            }
            String configured=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014);
            if(dnd.isHybrid(configured)&&effectiveDndTravelProfile(configured).length()==0){
                out.put("mechanics_apply",false);
                out.put("classification",RULE_OPTIONAL);
                out.put("requires_hybrid_travel_choice",true);
                out.put("reason","O perfil híbrido exige a escolha explícita de D&D 2014 ou D&D 2024 para a viagem. Nenhuma edição foi aplicada silenciosamente.");
                return out;
            }
            JSONObject map=db.getMapState();
            JSONObject hex=db.getMapHex(map.optString("map_package_id"),map.optString("current_hex_id"));
            String terrain=hex.optString("terrain_primary","open");
            String pace=paceFromAction(action);
            return previewDndHours(hours,terrain,pace,hasRoad(hex),db.getSceneState().optString("weather",""),signature(action+"|"+hours+"|"+map.optString("current_hex_id")),isExtendedTravelAction(action));
        }catch(Exception e){try{out.put("error",safe(e.getMessage()));}catch(Exception ignored){AppLog.ignored("TravelRulesEngine","Falha tolerada; a execução continuou.",ignored);}return out;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject previewDndSegment(double distanceKm,String terrain,String requestedPace,boolean road,String weather,String actionKey){return previewDndSegment(distanceKm,terrain,requestedPace,road,weather,actionKey,false);}

    public JSONObject previewDndSegment(double distanceKm,String terrain,String requestedPace,boolean road,String weather,String actionKey,boolean extendedTravel){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            String configured=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014);
            String profile=effectiveDndTravelProfile(configured);
            if(profile.length()==0){
                out.put("system","dnd_hybrid");out.put("rules_profile",configured);out.put("classification",RULE_OPTIONAL);out.put("mechanics_apply",false);
                out.put("requires_hybrid_travel_choice",true);out.put("distance_km",round2(distanceKm));out.put("reason","O perfil híbrido não define sozinho qual edição rege a viagem. Escolha D&D 2014 ou D&D 2024 na configuração da campanha.");return out;
            }
            boolean revised=dnd.is2024(profile);
            String selected=canonicalPace(requestedPace);
            String maximum=revised?maximum2024Pace(terrain,road):"Rápido";
            String effective=slower(selected,maximum);
            double speed=revised?speed2024(effective):speed2014(effective);
            if(!revised&&isDifficult2014(terrain,road))speed*=.5d;
            double hours=distanceKm<=0?0:distanceKm/Math.max(.1d,speed);
            out=previewDndHours(hours,terrain,effective,road,weather,signature(actionKey+"|"+distanceKm+"|"+terrain+"|"+effective+"|extended="+extendedTravel),extendedTravel);
            out.put("distance_km",round2(distanceKm));
            out.put("requested_pace",selected);
            out.put("maximum_pace",maximum);
            out.put("effective_pace",effective);
            out.put("speed_km_h",round2(speed));
            out.put("pace_was_capped",!selected.equals(effective));
            out.put("terrain",terrain);
            out.put("road",road);
            return out;
        }catch(Exception e){try{out.put("error",safe(e.getMessage()));}catch(Exception ignored){AppLog.ignored("TravelRulesEngine","Falha tolerada; a execução continuou.",ignored);}return out;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject previewDndHours(double hours,String terrain,String pace,boolean road,String weather,String signature,boolean extendedTravel)throws Exception{/*BR_JSON_WRAP*/try{
        String configured=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014);
        String profile=effectiveDndTravelProfile(configured);
        if(profile.length()==0)throw new IllegalStateException("Perfil híbrido sem edição de viagem definida");
        boolean revised=dnd.is2024(profile);
        JSONObject state=db.getTravelRulesState();
        JSONObject scene=db.getSceneState();
        JSONObject mapState=db.getMapState();
        String preStateFingerprint=stateFingerprint(state,scene,mapState);
        signature=signature(signature+"|"+preStateFingerprint);
        int day=Math.max(1,scene.optInt("day_number",1));
        double legacyBefore=state.optInt("travel_day",day)==day?state.optDouble("hours_today",0d):0d;
        double beforeSinceRest=Math.max(0d,state.optDouble("hours_since_long_rest",legacyBefore));
        double before=beforeSinceRest;
        double clockHour=scene.optDouble("clock_hour",sceneClockHour(scene.optString("time_of_day","Manhã")));
        TravelSchedule schedule=extendedTravel?TravelSchedule.extended(day,before,Math.max(0d,hours),clockHour):TravelSchedule.normal(day,before,Math.max(0d,hours));
        int arrivalDay=day+(int)Math.floor((clockHour+schedule.elapsedHours)/24d);
        double after=schedule.finalHoursToday;
        double afterSinceRest=extendedTravel?beforeSinceRest+Math.max(0d,hours):after;
        JSONObject out=new JSONObject();
        out.put("system",revised?"dnd_2024":"dnd_2014");
        out.put("rules_profile",profile);
        out.put("travel_detected",true);
        out.put("mechanics_apply",hours>0d);
        out.put("classification",RULE_RAW);
        out.put("hours",round2(hours));
        out.put("movement_hours",round2(hours));
        out.put("elapsed_hours",round2(schedule.elapsedHours));
        out.put("rest_periods",schedule.restPeriods);
        out.put("arrival_day",arrivalDay);
        out.put("extended_travel",extendedTravel);
        out.put("hours_today_before",round2(before));
        out.put("hours_today_after",round2(after));
        out.put("pace",canonicalPace(pace));
        out.put("pace_effects",paceEffects(revised,pace));
        out.put("terrain",terrain);
        out.put("road",road);
        out.put("environmental_effects",environmentalEffects(weather,revised));
        out.put("master_decisions",masterDecisions(weather));
        out.put("sources",dndSources(revised));
        out.put("action_signature",signature);
        out.put("pre_state_fingerprint",preStateFingerprint);
        out.put("idempotent",signature.equals(state.optString("last_signature","")));

        JSONArray saves=new JSONArray();
        if(extendedTravel){
            int firstExtra=(int)Math.floor(Math.max(8d,beforeSinceRest))+1;
            int finalHour=(int)Math.floor(afterSinceRest+1e-9);
            for(int absoluteHour=firstExtra;absoluteHour<=finalHour;absoluteHour++){
                if(absoluteHour<=8)continue;
                int dc=10+(absoluteHour-8);
                appendConstitutionSaves(saves,"marcha_forcada",dc,signature+"|forced|"+absoluteHour,revised,0d);
            }
        }
        appendWeatherSaves(saves,weather,hours,signature,revised);
        out.put("saving_throws",saves);
        out.put("exhaustion_changes",exhaustionChanges(saves));
        JSONObject resourceConsumption=previewResourceConsumption(schedule.elapsedHours,signature+"|resources");
        out.put("resource_consumption",resourceConsumption);
        JSONObject stateCommit=new JSONObject().put("travel_day",arrivalDay).put("hours_today",after).put("hours_since_long_rest",afterSinceRest).put("last_signature",signature).put("active_rules_profile",profile).put("active_pace",canonicalPace(pace)).put("active_pace_effects",out.optJSONObject("pace_effects")).put("active_environmental_effects",out.optJSONArray("environmental_effects"));
        mergeState(stateCommit,resourceConsumption.optJSONObject("_state_commit"));
        out.put("_state_commit",stateCommit);
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject previewMystaraSegment(double distanceKm,String terrain,String requestedMode,boolean road,boolean paved,boolean guide,boolean forage,boolean legacyForcedMarch,int windRoll,String actionKey){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            JSONObject scene=db.getSceneState();
            int day=Math.max(1,scene.optInt("day_number",1));
            JSONObject state=db.getTravelRulesState();
            String mode=resolvedMystaraMode(requestedMode);
            // A marcha forçada de +50% encontrada na Rules Cyclopedia pertence às regras de exércitos,
            // não à viagem comum de aventureiros. O parâmetro legado é ignorado e nunca altera a taxa RAW.
            double rate=mystara.travelKmPerDay(mode,terrain,road,paved,forage,false,windRoll);
            double days=distanceKm/Math.max(.1d,rate);
            double beforeFraction=state.optInt("travel_day",day)==day?state.optDouble("day_fraction",0d):0d;
            double beforeHours=Math.max(0d,Math.min(8d,beforeFraction*8d));
            double movementHours=Math.max(0d,days*8d);
            TravelSchedule schedule=TravelSchedule.normal(day,beforeHours,movementHours);
            int completed=Math.max(0,(int)Math.floor((beforeHours+movementHours+1e-9)/8d)-(int)Math.floor((beforeHours+1e-9)/8d));
            double remainder=Math.max(0d,Math.min(1d,schedule.finalHoursToday/8d));
            String preStateFingerprint=stateFingerprint(state,scene,db.getMapState());
            String sig=signature(actionKey+"|"+distanceKm+"|"+terrain+"|"+mode+"|"+day+"|"+preStateFingerprint);
            JSONObject calendar=mystaraTravelCalendar(day,beforeHours,movementHours);
            JSONArray movementDays=calendar.optJSONArray("movement_days");
            JSONArray campNights=calendar.optJSONArray("camp_nights");
            JSONArray lostChecks=mystaraLostChecks(movementDays,terrain,road||guide||mystara.isWaterMode(mode)||mystara.isAirMode(mode),state,sig);
            JSONObject primaryLost=primaryMystaraLostCheck(lostChecks);
            int lastLostDay=lastMystaraLostCheckDay(lostChecks,state.optInt("last_lost_check_day",0));
            JSONArray encounterChecks=mystaraEncounterChecks(movementDays,campNights,terrain,state,sig);
            int lastDayEncounter=lastMystaraEncounterCheckDay(encounterChecks,"dia",state.optInt("last_day_encounter_check_day",0));
            int lastNightEncounter=lastMystaraEncounterCheckDay(encounterChecks,"noite/acampamento",state.optInt("last_night_encounter_check_day",0));

            out.put("system",MystaraRulesEngine.SYSTEM_ID);
            out.put("rules_profile",MystaraRulesEngine.SYSTEM_LABEL);
            out.put("classification",RULE_RAW);
            out.put("distance_km",round2(distanceKm));
            out.put("mode_requested",requestedMode);
            out.put("mode_resolved",mode);
            out.put("mode_overridden_by_slowest_member",isWalking(requestedMode)&&!mode.equals(requestedMode));
            out.put("terrain",mystara.terrainPt(terrain));
            out.put("rate_km_day",round2(rate));
            out.put("days_for_segment",round3(days));
            out.put("movement_hours",round2(movementHours));
            out.put("elapsed_hours",round2(schedule.elapsedHours));
            out.put("rest_periods",schedule.restPeriods);
            out.put("arrival_day",schedule.finalDay);
            out.put("day_fraction_before",round3(beforeFraction));
            out.put("day_fraction_after",round3(remainder));
            int daysWithoutRest=Math.max(0,state.optInt("mystara_days_without_rest",0))+completed;
            out.put("completed_travel_days",completed);
            out.put("mystara_days_without_rest_after",daysWithoutRest);
            out.put("mystara_long_distance_penalty",mystara.longDistancePenalty(daysWithoutRest));
            out.put("travel_days_touched",movementDays==null?0:movementDays.length());
            out.put("lost_checks",lostChecks);
            out.put("lost_check",primaryLost);
            out.put("encounter_checks",encounterChecks);
            out.put("forage_check",mystaraForageCheck(forage,terrain,sig));
            if(legacyForcedMarch)out.put("ignored_legacy_forced_march",new JSONObject().put("classification",RULE_OPTIONAL).put("applied",false).put("reason","O bônus de +50% da Rules Cyclopedia pertence à marcha forçada de forças militares, não à viagem comum de aventureiros."));
            out.put("sources",new JSONArray().put("Rules Cyclopedia, Capítulo 6 — Viagem por Terra").put("Rules Cyclopedia, Capítulo 7 — Encontros e Evasão"));
            out.put("action_signature",sig);
            out.put("pre_state_fingerprint",preStateFingerprint);
            out.put("idempotent",sig.equals(state.optString("last_signature","")));
            JSONObject resourceConsumption=previewResourceConsumption(schedule.elapsedHours,sig+"|resources");
            out.put("resource_consumption",resourceConsumption);
            JSONObject stateCommit=new JSONObject().put("travel_day",schedule.finalDay).put("day_fraction",remainder).put("mystara_days_without_rest",daysWithoutRest).put("mystara_long_distance_penalty",mystara.longDistancePenalty(daysWithoutRest)).put("last_signature",sig).put("last_lost_check_day",lastLostDay).put("last_day_encounter_check_day",lastDayEncounter).put("last_night_encounter_check_day",lastNightEncounter);
            mergeState(stateCommit,resourceConsumption.optJSONObject("_state_commit"));
            out.put("_state_commit",stateCommit);
            return out;
        }catch(Exception e){try{out.put("error",safe(e.getMessage()));}catch(Exception ignored){AppLog.ignored("TravelRulesEngine","Falha tolerada; a execução continuou.",ignored);}return out;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject mystaraTravelCalendar(int startDay,double beforeHours,double movementHours)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray movementDays=new JSONArray(),campNights=new JSONArray();
        double remaining=Math.max(0d,movementHours),current=Math.max(0d,Math.min(8d,beforeHours));
        int day=startDay,guard=0;
        while(remaining>1e-7&&guard++<10000){
            if(current>=8d-1e-7){campNights.put(day);day++;current=0d;continue;}
            if(!arrayHasInt(movementDays,day))movementDays.put(day);
            double moved=Math.min(remaining,8d-current);remaining-=moved;current+=moved;
            if(remaining>1e-7&&current>=8d-1e-7){campNights.put(day);day++;current=0d;}
        }
        return new JSONObject().put("movement_days",movementDays).put("camp_nights",campNights);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONArray mystaraLostChecks(JSONArray movementDays,String terrain,boolean protectedRoute,JSONObject state,String sig)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray checks=new JSONArray();
        if(movementDays==null)return checks;
        for(int i=0;i<movementDays.length();i++){
            int checkDay=movementDays.optInt(i);
            JSONObject check=mystaraLostCheck(checkDay,terrain,protectedRoute,state,sig+"|travel-day|"+checkDay);
            check.put("day",checkDay);
            checks.put(check);
        }
        return checks;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject primaryMystaraLostCheck(JSONArray checks)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject fallback=new JSONObject().put("required",false).put("reason","O trecho não iniciou um novo dia de viagem.");
        for(int i=0;i<checks.length();i++){JSONObject x=checks.optJSONObject(i);if(x!=null&&x.optBoolean("lost",false))return x;}
        for(int i=0;i<checks.length();i++){JSONObject x=checks.optJSONObject(i);if(x!=null&&x.optBoolean("required",false))return x;}
        return checks.length()>0&&checks.optJSONObject(0)!=null?checks.optJSONObject(0):fallback;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int lastMystaraLostCheckDay(JSONArray checks,int previous){
        int last=previous;
        for(int i=0;i<checks.length();i++){JSONObject x=checks.optJSONObject(i);if(x!=null&&(x.optBoolean("required",false)||x.optBoolean("already_checked_today",false)))last=Math.max(last,x.optInt("day",last));}
        return last;
    }

    public synchronized boolean commit(JSONObject report){/*BR_JSON_WRAP*/try{
        if(report==null)return false;
        try{
            JSONObject currentState=db.getTravelRulesState();
            String reportSignature=report.optString("action_signature","");
            if(reportSignature.length()>0&&reportSignature.equals(currentState.optString("last_signature","")))return true;
            // Preview de recursos pode ser confirmado diretamente, sem assinatura de viagem.
            // Reusar o mesmo relatorio nunca deve descontar agua ou racoes uma segunda vez.
            if(report.optBoolean("resource_time_report",false)&&reportSignature.length()>0&&
                reportSignature.equals(currentState.optString("last_resource_signature","")))return true;
            String expectedPreState=report.optString("pre_state_fingerprint","");
            if(expectedPreState.length()>0&&!expectedPreState.equals(stateFingerprint(currentState,db.getSceneState(),db.getMapState()))){
                db.addAuditEvent("erro_regras_viagem","Relatório de viagem obsoleto","O estado mudou entre a prévia e a confirmação.","{}",report.toString(),false);return false;
            }
            if(report.optBoolean("idempotent",false))return true;
            JSONObject next=report.optJSONObject("_state_commit");
            if(next!=null){JSONObject merged=db.getTravelRulesState();java.util.Iterator<String> keys=next.keys();while(keys.hasNext()){String key=keys.next();merged.put(key,next.opt(key));}db.saveTravelRulesState(merged);}
            JSONObject resourceUse=report.optJSONObject("resource_consumption");if(resourceUse==null&&report.optBoolean("resource_time_report",false))resourceUse=report;
            if(resourceUse!=null&&resourceUse.optBoolean("enabled",false)&&resourceUse.optInt("days",0)>0){
                JSONObject party=db.getPartyState();JSONObject resources=parseObject(party.optString("company_resources_json","{}"));
                resources.put("provisions_days",Math.max(0,resourceUse.optInt("provisions_after",resources.optInt("provisions_days",0))));
                resources.put("water_days",Math.max(0,resourceUse.optInt("water_after",resources.optInt("water_days",0))));
                if(resourceUse.optBoolean("wilem_meal_credit_applied",false))
                    resources.put("wilem_provisions_credit_used_day",resourceUse.optInt("wilem_meal_credit_day"));
                party.put("company_resources_json",resources.toString());db.savePartyState(party);
            }
            JSONArray changes=report.optJSONArray("exhaustion_changes");
            if(changes!=null)for(int i=0;i<changes.length();i++){
                JSONObject change=changes.optJSONObject(i);if(change==null||change.optInt("delta",0)<=0)continue;
                JSONObject hero=db.getCharacter(change.optLong("character_id"));if(hero.length()==0)continue;
                JSONObject sheet=parseObject(hero.optString("sheet_json","{}"));
                int old=Math.max(0,sheet.optInt("exhaustion",0));
                int max=dnd.is2024(db.getCampaign().optString("rules_profile"))?6:6;
                int value=Math.min(max,old+change.optInt("delta",0));
                sheet.put("exhaustion",value);
                sheet.put("exhaustion_rule_profile",db.getCampaign().optString("rules_profile"));
                db.updateCharacterSheet(hero.optLong("id"),sheet.toString());
            }
            db.addAuditEvent("regras_viagem","Regras de viagem aplicadas",summary(report),"{}",report.toString(),true);
            return true;
        }catch(Exception error){try{db.addAuditEvent("erro_regras_viagem","Falha ao aplicar regras de viagem",safe(error.getMessage()),"{}",report.toString(),false);}catch(Exception ignored){AppLog.ignored("TravelRulesEngine","Falha tolerada; a execução continuou.",ignored);}return false;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public void resetDailyTravel(){
        try{resetDailyTravelChecked();}
        catch(Exception error){AppLog.e("TravelRulesEngine","Falha ao reiniciar o estado diário de viagem.",error);}
    }

    public void resetDailyTravelChecked() throws Exception {/*BR_JSON_WRAP*/try{
        JSONObject state=db.getTravelRulesState();
        state.put("hours_today",0d);state.put("hours_since_long_rest",0d);state.put("day_fraction",0d);
        state.put("mystara_days_without_rest",0);state.put("mystara_long_distance_penalty",0);
        state.put("travel_day",Math.max(1,db.getSceneState().optInt("day_number",1)));state.put("last_signature","");
        db.saveTravelRulesState(state);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public String resolvedMystaraMode(String requested){
        if(!isWalking(requested))return requested;
        double slowest=36d;
        List<JSONObject> heroes=db.listActiveCharacters();
        for(JSONObject hero:heroes){JSONObject sheet=parseObject(hero.optString("sheet_json","{}"));int mos=sheet.optInt("encumbrance_mos",hero.optInt("encumbrance_mos",0));slowest=Math.min(slowest,mystara.normalMoveMeters(mos));}
        if(slowest<=18d)return "A pé — carga pesada";
        if(slowest<=27d)return "A pé — carga leve";
        return "A pé — sem carga";
    }

    public JSONObject normalScheduleForMovement(double movementHours){/*BR_JSON_WRAP*/try{JSONObject out=new JSONObject();try{JSONObject scene=db.getSceneState(),state=db.getTravelRulesState();int day=Math.max(1,scene.optInt("day_number",1));double before=Math.max(0d,state.optDouble("hours_since_long_rest",state.optDouble("hours_today",0d)));double clock=scene.optDouble("clock_hour",sceneClockHour(scene.optString("time_of_day","Manhã")));TravelSchedule schedule=TravelSchedule.normal(day,before,Math.max(0d,movementHours));int arrival=day+(int)Math.floor((clock+schedule.elapsedHours)/24d);out.put("movement_hours",round2(movementHours)).put("elapsed_hours",round2(schedule.elapsedHours)).put("rest_periods",schedule.restPeriods).put("arrival_day",arrival).put("hours_today_after",round2(schedule.finalHoursToday));}catch(Exception ignored){AppLog.ignored("TravelRulesEngine","Falha tolerada; a execução continuou.",ignored);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject previewElapsedTimeResources(double elapsedHours,String actionKey){return previewResourceConsumption(Math.max(0d,elapsedHours),signature(actionKey+"|"+elapsedHours+"|resources"));}

    private JSONObject previewResourceConsumption(double elapsedHours,String sig){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            JSONObject party=db.getPartyState();JSONObject resources=parseObject(party.optString("company_resources_json","{}"));JSONObject state=db.getTravelRulesState();
            boolean enabled=resources.optBoolean("track_travel_resources",false);
            double before=Math.max(0d,state.optDouble("resource_elapsed_hours",0d));double total=before+Math.max(0d,elapsedHours);int days=(int)Math.floor(total/24d+1e-9);double after=total-days*24d;
            int provisions=Math.max(0,resources.optInt("provisions_days",0)),water=Math.max(0,resources.optInt("water_days",0));
            // A refeicao compartilhada e uma substituicao de suprimento de UM dia, nao cria racoes.
            // O recibo de uso evita que o mesmo jantar pague mais de um dia de viagem.
            int sceneDay=db.getSceneState().optInt("day_number",1);
            int mealDay=resources.optInt("wilem_provisions_credit_day",0);
            boolean mealCredit=!isMystaraWorld()&&enabled&&days>0&&mealDay==sceneDay
                &&resources.optInt("wilem_provisions_credit_used_day",0)!=mealDay;
            int rationsSpent=Math.max(0,days-(mealCredit?1:0));
            out.put("resource_time_report",true).put("classification",RULE_OPTIONAL)
                .put("enabled",enabled).put("mechanics_apply",enabled&&elapsedHours>0d)
                .put("elapsed_hours",round2(elapsedHours))
                .put("accumulated_hours_before",round2(before))
                .put("accumulated_hours_after",round2(after))
                .put("days",days).put("days_consumed",days)
                .put("provisions_before",provisions).put("provisions_after",Math.max(0,provisions-rationsSpent))
                .put("water_before",water).put("water_after",Math.max(0,water-days))
                .put("insufficient_provisions",enabled&&rationsSpent>provisions)
                .put("insufficient_water",enabled&&days>water)
                .put("wilem_meal_credit_applied",mealCredit)
                .put("wilem_meal_credit_day",mealCredit?mealDay:0)
                .put("provisions_days_consumed",rationsSpent)
                .put("action_signature",sig)
                .put("idempotent",sig.equals(state.optString("last_resource_signature","")));
            if(!enabled)out.put("reason","O controle automático de provisões e água é uma regra de logística opcional e está desativado nos Recursos da Companhia.");
            else out.put("basis","Os campos representam dias de suprimento para toda a companhia; um dia é consumido a cada 24 horas efetivamente decorridas.");
            out.put("_state_commit",enabled?new JSONObject().put("resource_elapsed_hours",after).put("last_resource_signature",sig):new JSONObject());
        }catch(Exception e){try{out.put("error",safe(e.getMessage()));}catch(Exception ignored){AppLog.ignored("TravelRulesEngine","Falha tolerada; a execução continuou.",ignored);}}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static void mergeState(JSONObject target,JSONObject additions)throws Exception{/*BR_JSON_WRAP*/try{if(target==null||additions==null)return;java.util.Iterator<String> keys=additions.keys();while(keys.hasNext()){String key=keys.next();target.put(key,additions.opt(key));}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject environmentalMechanics(String weather){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            if(isMystaraWorld())return out.put("classification",RULE_DM).put("active_only",true).put("mechanics_apply",false).put("rules_profile",MystaraRulesEngine.SYSTEM_LABEL).put("effects",new JSONArray()).put("reason","O Diretor Ambiental não importa efeitos ambientais de D&D 2014/2024 para Mystara. Consequências mecânicas exigem uma regra identificada da Rules Cyclopedia ou decisão explícita do Mestre.");
            String configured=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014);
            String profile=effectiveDndTravelProfile(configured);
            if(profile.length()==0)return out.put("classification",RULE_OPTIONAL).put("active_only",true).put("mechanics_apply",false).put("requires_hybrid_travel_choice",true);
            boolean revised=dnd.is2024(profile);
            out.put("classification",RULE_RAW).put("active_only",true).put("rules_profile",profile).put("effects",environmentalEffects(weather,revised)).put("master_decisions",masterDecisions(weather));
        }catch(Exception ignored){AppLog.ignored("TravelRulesEngine","Falha tolerada; a execução continuou.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject mystaraLostCheck(int day,String terrain,boolean protectedRoute,JSONObject state,String sig)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        if(protectedRoute){out.put("required",false).put("reason","Estrada, trilha, rio, guia confiável ou modo de viagem que dispensa esta verificação.");return out;}
        if(state.optInt("last_lost_check_day",0)==day){out.put("required",false).put("already_checked_today",true);return out;}
        int chance=mystara.lostChanceOnD6(terrain,false);int roll=deterministicDie(sig+"|lost",6);
        out.put("required",true).put("frequency","uma vez no início de cada dia de viagem").put("die","1d6").put("chance","1–"+chance).put("roll",roll).put("lost",roll<=chance).put("direction_roll",roll<=chance?deterministicDie(sig+"|lost-direction",6):0).put("resolution",roll<=chance?"O grupo viaja numa direção diferente da pretendida; não trate como falha sem movimento.":"A direção pretendida foi mantida.").put("master_only_direction",roll<=chance);
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONArray mystaraEncounterChecks(JSONArray movementDays,JSONArray campNights,String terrain,JSONObject state,String sig)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();int chance=mystara.encounterChanceOnD6(terrain);
        if(movementDays!=null)for(int i=0;i<movementDays.length();i++){
            int checkDay=movementDays.optInt(i);
            if(state.optInt("last_day_encounter_check_day",0)==checkDay){out.put(new JSONObject().put("period","dia").put("day",checkDay).put("required",false).put("already_checked_today",true));continue;}
            int roll=deterministicDie(sig+"|enc-day|"+checkDay,6);
            JSONObject check=new JSONObject().put("period","dia").put("day",checkDay).put("required",true).put("frequency","uma vez durante cada dia de viagem").put("die","1d6").put("chance","1–"+chance).put("roll",roll).put("encounter",roll<=chance);
            if(roll<=chance)check.put("procedure",mystaraEncounterProcedure(sig+"|day|"+checkDay,false));out.put(check);
        }
        if(campNights!=null)for(int i=0;i<campNights.length();i++){
            int checkDay=campNights.optInt(i);
            if(state.optInt("last_night_encounter_check_day",0)==checkDay){out.put(new JSONObject().put("period","noite/acampamento").put("day",checkDay).put("required",false).put("already_checked_today",true));continue;}
            int roll=deterministicDie(sig+"|enc-night|"+checkDay,12);
            JSONObject check=new JSONObject().put("period","noite/acampamento").put("day",checkDay).put("required",true).put("frequency","uma vez quando o grupo acampa à noite").put("die","1d12").put("chance","1–"+chance).put("roll",roll).put("encounter",roll<=chance);
            if(roll<=chance)check.put("procedure",mystaraEncounterProcedure(sig+"|night|"+checkDay,true));out.put(check);
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int lastMystaraEncounterCheckDay(JSONArray checks,String period,int previous){
        int last=previous;if(checks==null)return last;
        for(int i=0;i<checks.length();i++){JSONObject x=checks.optJSONObject(i);if(x!=null&&period.equals(x.optString("period"))&&(x.optBoolean("required",false)||x.optBoolean("already_checked_today",false)))last=Math.max(last,x.optInt("day",last));}
        return last;
    }

    private static boolean arrayHasInt(JSONArray values,int target){if(values==null)return false;for(int i=0;i<values.length();i++)if(values.optInt(i,Integer.MIN_VALUE)==target)return true;return false;}

    private JSONObject mystaraEncounterProcedure(String sig,boolean night)throws Exception{/*BR_JSON_WRAP*/try{
        int party=deterministicDie(sig+"|surprise-party",6),enemy=deterministicDie(sig+"|surprise-enemy",6),reaction=deterministicDie(sig+"|react-a",6)+deterministicDie(sig+"|react-b",6);
        boolean partySurprised=party<=2,enemySurprised=enemy<=2,surpriseAffectsDistance=partySurprised||enemySurprised;
        int distance=0;
        String basis;
        if(surpriseAffectsDistance){distance=deterministicDie(sig+"|distance-surprise",4)*9;basis="ao menos um grupo surpreendido: 1d4 × 9 m";}
        else if(night){distance=deterministicDie(sig+"|distance-night",4)*9;basis="sem luz ou visibilidade muito precária: 1d4 × 9 m";}
        else {for(int i=0;i<4;i++)distance+=deterministicDie(sig+"|distance-day|"+i,6);distance*=9;basis="luz do dia clara: 4d6 × 9 m";}
        JSONObject out=new JSONObject().put("surprise_party_roll",party).put("surprise_enemy_roll",enemy).put("surprise_result",mystara.surpriseResult(party,enemy)).put("reaction_2d6",reaction).put("reaction_result",mystaraReaction(reaction)).put("encounter_distance_m",distance).put("distance_basis",basis).put("combat_not_automatic",true).put("creature_and_number","Consultar a tabela oficial do terreno e o Número de Aparições da criatura selecionada.");
        if(partySurprised&&!enemySurprised)out.put("party_awareness_distance_m",Math.max(1,distance/2)).put("enemy_awareness_distance_m",distance);
        else if(enemySurprised&&!partySurprised)out.put("party_awareness_distance_m",distance).put("enemy_awareness_distance_m",Math.max(1,distance/2));
        else out.put("party_awareness_distance_m",distance).put("enemy_awareness_distance_m",distance);
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject mystaraForageCheck(boolean forage,String terrain,String sig)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject().put("attempted",forage);
        if(!forage)return out;
        int chance=mystara.forageChanceOnD6(terrain);int roll=chance<=0?0:deterministicDie(sig+"|forage",6);
        out.put("movement_multiplier","2/3").put("die","1d6").put("chance",chance<=0?"indisponível":"1–"+chance).put("roll",roll).put("success",chance>0&&roll<=chance);
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject paceEffects(boolean revised,String pace)throws Exception{/*BR_JSON_WRAP*/try{
        String p=canonicalPace(pace);JSONObject out=new JSONObject();out.put("classification",RULE_RAW);
        if(revised){
            if("Rápido".equals(p))out.put("disadvantage",new JSONArray().put("Sabedoria (Percepção)").put("Sabedoria (Sobrevivência)").put("Destreza (Furtividade)"));
            else if("Normal".equals(p))out.put("disadvantage",new JSONArray().put("Destreza (Furtividade)"));
            else out.put("advantage",new JSONArray().put("Sabedoria (Percepção)").put("Sabedoria (Sobrevivência)"));
        }else{
            if("Rápido".equals(p))out.put("passive_perception_modifier",-5);
            if("Lento".equals(p))out.put("stealth_while_travelling",true);
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONArray environmentalEffects(String weather,boolean revised)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray effects=new JSONArray();String w=fold(weather);
        if(contains(w,"frio extremo","extreme cold"))effects.put(new JSONObject().put("effect","Frio extremo").put("rule","Salvaguarda de Constituição CD 10 ao fim de cada hora; falha: 1 nível de Exaustão.").put("classification",RULE_RAW));
        if(contains(w,"chuva forte","neve forte","nevasca","heavy rain","heavy snowfall")){
            String rule=revised?"Área levemente obscurecida; Desvantagem em todos os testes de Sabedoria (Percepção). Chuva forte apaga chamas abertas.":"Área levemente obscurecida; Desvantagem em testes de Sabedoria (Percepção) baseados em visão. Chuva forte apaga chamas abertas e também impõe Desvantagem em Percepção baseada em audição.";
            effects.put(new JSONObject().put("effect","Precipitação forte").put("rule",rule).put("classification",RULE_RAW));
        }
        if(contains(w,"vento forte","vendaval","strong wind"))effects.put(new JSONObject().put("effect","Vento forte").put("rule","Desvantagem em ataques à distância com armas; apaga chamas abertas; dispersa névoa; criaturas voadoras precisam pousar ou caem.").put("classification",RULE_RAW));
        if(contains(w,"gelo escorregadio","slippery ice")){
            String rule=revised?"Terreno difícil; salvaguarda de Destreza CD 10 ao entrar ou iniciar o turno, ou fica Caído.":"Terreno difícil; teste de Destreza (Acrobacia) CD 10 ao entrar pela primeira vez num turno ou iniciar o turno, ou fica Caído.";
            effects.put(new JSONObject().put("effect","Gelo escorregadio").put("rule",rule).put("classification",RULE_RAW));
        }
        return effects;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONArray masterDecisions(String weather)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();String w=fold(weather);
        if(contains(w,"nevasca","incendio florestal","forest fire"))out.put(new JSONObject().put("decision","Tratar o fenômeno como obstáculo de jornada, definir solução, atraso e eventual consequência.").put("classification",RULE_DM).put("automatic",false));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void appendWeatherSaves(JSONArray saves,String weather,double hours,String sig,boolean revised)throws Exception{
        String w=fold(weather);if(!contains(w,"frio extremo","extreme cold"))return;
        int whole=(int)Math.floor(hours+1e-9);
        double start=WilemCookbookEffectRules.worldHour(db.getSceneState());
        for(int h=1;h<=whole;h++)
            appendConstitutionSaves(saves,"frio_extremo",10,sig+"|cold|"+h,revised,start+h);
    }

    private void appendConstitutionSaves(JSONArray saves,String cause,int dc,String seed,boolean revised,double atWorldHour)throws Exception{/*BR_JSON_WRAP*/try{
        for(JSONObject hero:db.listActiveCharacters()){
            if("frio_extremo".equals(cause)&&coldProtected(hero,revised,atWorldHour))continue;
            int roll=deterministicDie(seed+"|"+hero.optLong("id"),20);int bonus=constitutionSaveBonus(hero);int total=roll+bonus;
            saves.put(new JSONObject().put("character_id",hero.optLong("id")).put("character",hero.optString("name")).put("cause",cause).put("die",roll).put("constitution_modifier",bonus).put("total",total).put("dc",dc).put("success",total>=dc).put("failure_effect","1 nível de Exaustão").put("classification",RULE_RAW));
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONArray exhaustionChanges(JSONArray saves)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();java.util.LinkedHashMap<Long,JSONObject> map=new java.util.LinkedHashMap<Long,JSONObject>();
        for(int i=0;i<saves.length();i++){JSONObject s=saves.optJSONObject(i);if(s==null||s.optBoolean("success",true))continue;long id=s.optLong("character_id");JSONObject x=map.get(id);if(x==null){x=new JSONObject().put("character_id",id).put("character",s.optString("character")).put("delta",0).put("causes",new JSONArray());map.put(id,x);}x.put("delta",x.optInt("delta")+1);x.optJSONArray("causes").put(s.optString("cause"));}
        for(JSONObject x:map.values())out.put(x);return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private boolean coldProtected(JSONObject hero,boolean revised,double atWorldHour){
        if(WilemCookbookEffectRules.coldResistanceAt(hero,atWorldHour))return true;
        String all=fold(hero.optString("resistances")+" "+hero.optString("immunities")+" "+
            hero.optString("notes")+" "+hero.optString("equipment")+" "+hero.optString("sheet_json"));
        if(contains(all,"resistencia a frio","resistência a frio","imunidade a frio",
            "cold resistance","cold immunity"))return true;
        return !revised&&contains(all,"roupa de frio","roupas de frio","cold weather gear");
    }

    private int constitutionSaveBonus(JSONObject hero){return dnd.savingThrowBonus(hero,"Constituição");}

    private boolean isMystaraWorld(){String profile=db.getActiveMapProfile();return "mystara".equals(profile)||MystaraRulesEngine.SYSTEM_ID.equals(profile);}
    private static boolean isExtendedTravelAction(String action){String a=fold(action);return contains(a,"marcha forcada","marcha prolongada","sem descansar","alem de 8 horas","além de 8 horas","continuamos durante a noite");}
    private static String stateFingerprint(JSONObject state,JSONObject scene,JSONObject map){return state.optInt("travel_day",0)+"|"+round3(state.optDouble("hours_today",0))+"|"+round3(state.optDouble("hours_since_long_rest",0))+"|"+round3(state.optDouble("day_fraction",0))+"|"+scene.optInt("day_number",1)+"|"+round3(scene.optDouble("clock_hour",sceneClockHour(scene.optString("time_of_day","Manhã"))))+"|"+map.optString("current_hex_id","");}

    private String effectiveDndTravelProfile(String configured){
        if(!dnd.isHybrid(configured))return configured;String chosen=db.getTravelRulesState().optString("hybrid_dnd_profile","");
        return DndRulesEngine.PROFILE_2014.equals(chosen)||DndRulesEngine.PROFILE_2024.equals(chosen)?chosen:"";
    }

    private JSONArray dndSources(boolean revised){/*BR_JSON_WRAP*/try{JSONArray a=new JSONArray();if(revised)return a.put("Player’s Handbook 2024 — Ritmo de Viagem").put("Dungeon Master’s Guide 2024 — Travel Terrain, Extended Travel e Environmental Effects");return a.put("Player’s Handbook 2014 — Travel Pace e Forced March").put("Dungeon Master’s Guide 2014 — Wilderness e Environmental Hazards");}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private String maximum2024Pace(String raw,boolean road){
        String t=terrainClass(raw);String max;
        if("mountain".equals(t)||"swamp".equals(t))max="Lento";
        else if("grassland".equals(t))max="Rápido";
        else if("arctic".equals(t))max=partyHasArcticTravelEquipment()?"Rápido":"Normal";
        else max="Normal";
        if(road)max=faster(max);
        if(partyRequiresSlowPace())max="Lento";
        return max;
    }
    private boolean isDifficult2014(String raw,boolean road){
        if(road)return false;
        String t=fold(raw);
        return contains(t,"terreno dificil","difficult terrain","floresta densa","dense forest","vegetacao densa","thick undergrowth","neve profunda","deep snow","entulho","rubble","pantano profundo","deep swamp","encosta ingreme","steep slope");
    }
    private boolean partyHasArcticTravelEquipment(){
        StringBuilder all=new StringBuilder();
        for(JSONObject hero:db.listActiveCharacters())all.append(' ').append(hero.optString("equipment")).append(' ').append(hero.optString("notes")).append(' ').append(hero.optString("sheet_json"));
        JSONObject resources=parseObject(db.getPartyState().optString("company_resources_json","{}"));all.append(' ').append(resources.optString("shared_inventory")).append(' ').append(resources.optString("vehicle"));
        String text=fold(all.toString());return contains(text,"esquis","ski","snowshoe","raquetes de neve","trenó","treno","sled");
    }
    private boolean partyRequiresSlowPace(){
        String profile=effectiveDndTravelProfile(db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014));
        for(JSONObject hero:db.listActiveCharacters()){
            JSONObject sheet=parseObject(hero.optString("sheet_json","{}"));
            if(sheet.optBoolean("speed_reduced_to_half",false)||sheet.optBoolean("travel_speed_reduced_to_half",false)||sheet.optDouble("speed_multiplier",1d)<=.5d)return true;
            if(dnd.is2024(profile)){int exhaustion=Math.max(0,Math.min(6,sheet.optInt("exhaustion",0)));double base=baseSpeedFeet(hero,sheet);if(base>0d&&Math.max(0d,base-exhaustion*5d)<=base/2d)return true;}
            String text=fold(hero.optString("notes")+" "+sheet.optString("conditions")+" "+sheet.optString("notes"));
            if(contains(text,"deslocamento reduzido a metade","speed reduced to half","metade do deslocamento"))return true;
        }
        return false;
    }
    private static double baseSpeedFeet(JSONObject hero,JSONObject sheet){
        String raw=sheet.optString("speed",hero.optString("speed","")).toLowerCase(java.util.Locale.ROOT);java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+(?:[.,]\\d+)?)").matcher(raw);if(!m.find())return 0d;double value;try{value=Double.parseDouble(m.group(1).replace(',','.'));}catch(Exception e){return 0d;}if(raw.contains(" ft")||raw.contains("feet")||raw.contains("pé")||raw.contains("pes")||raw.contains("pés"))return value;if(raw.contains(" m")||raw.contains("metro"))return value*3.280839895d;return 0d;
    }

    private String terrainClass(String raw){String t=fold(raw);if(contains(t,"neve","gelo","geleira","tundra","arctic"))return "arctic";if(contains(t,"montanh"))return "mountain";if(contains(t,"pantano","pântano","swamp","marsh"))return "swamp";if(contains(t,"selva","jungle"))return "jungle";if(contains(t,"floresta","bosque","forest","wood"))return "forest";if(contains(t,"planicie","planície","campo","grassland","open"))return "grassland";if(contains(t,"costa","coast"))return "coastal";if(contains(t,"deserto","desert"))return "desert";if(contains(t,"cidade","urbano","urban"))return "urban";return "other";}

    private static double speed2014(String pace){if("Rápido".equals(canonicalPace(pace)))return 6.4d;if("Lento".equals(canonicalPace(pace)))return 3.2d;return 4.8d;}
    private static double speed2024(String pace){if("Rápido".equals(canonicalPace(pace)))return 6.4d;if("Lento".equals(canonicalPace(pace)))return 3.2d;return 4.8d;}
    private static int sceneClockHour(String label){String n=fold(label);if(n.contains("madrugada"))return 3;if(n.contains("amanhe"))return 6;if(n.contains("manha"))return 9;if(n.contains("meio dia"))return 12;if(n.contains("tarde"))return 15;if(n.contains("entard"))return 18;if(n.contains("noite"))return 21;return 9;}
    private static String paceFromAction(String action){String a=fold(action);if(contains(a,"ritmo rapido","ritmo rápido","marcha rapida","marcha rápida"))return "Rápido";if(contains(a,"ritmo lento","devagar","furtivamente"))return "Lento";return "Normal";}
    private static String canonicalPace(String pace){String p=fold(pace);if(p.contains("rapid"))return "Rápido";if(p.contains("lent"))return "Lento";return "Normal";}
    private static String slower(String a,String b){return rank(a)<=rank(b)?canonicalPace(a):canonicalPace(b);}
    private static String faster(String p){int r=rank(p);return r<=0?"Normal":"Rápido";}
    private static int rank(String p){String x=canonicalPace(p);return "Lento".equals(x)?0:"Normal".equals(x)?1:2;}
    private static boolean isWalking(String mode){return fold(mode).contains("a pe");}
    private static boolean isTravel(String action){String a=fold(action);return contains(a,"viaj","caminh","march","seguimos pela estrada","avancamos para","avançamos para","partimos para","cavalg");}
    private static boolean hasRoad(JSONObject hex){if(hex!=null&&hex.optInt("road_present",0)==1)return true;String x=fold(hex==null?"":hex.optString("roads_json","[]")+" "+hex.optString("terrain_primary",""));return contains(x,"road","estrada","trail","trilha")&&!contains(x,"needsreview true","revisao pendente");}
    private static String mystaraReaction(int roll){if(roll<=3)return "Ataca ou reage de forma hostil";if(roll<=6)return "Agressivo; nova reação com –4 após a resposta dos personagens";if(roll<=9)return "Cauteloso; nova reação após a resposta dos personagens";if(roll<=11)return "Neutro; nova reação com +4 após a resposta dos personagens";return "Amigável";}

    private static final class TravelSchedule{
        final int finalDay;final double finalHoursToday;final double elapsedHours;final int restPeriods;
        TravelSchedule(int d,double h,double elapsed,int rests){finalDay=d;finalHoursToday=h;elapsedHours=elapsed;restPeriods=rests;}
        static TravelSchedule extended(int day,double before,double movement,double clockHour){return new TravelSchedule(day+(int)Math.floor((clockHour+movement)/24d),before+movement,movement,0);}
        static TravelSchedule normal(int day,double before,double movement){double remaining=movement,current=Math.max(0d,Math.min(8d,before)),elapsed=0d;int currentDay=day,rests=0;int guard=0;while(remaining>1e-7&&guard++<10000){double available=Math.max(0d,8d-current);if(available<=1e-7){elapsed+=16d;currentDay++;current=0d;rests++;continue;}double moved=Math.min(remaining,available);remaining-=moved;current+=moved;elapsed+=moved;if(remaining>1e-7&&current>=8d-1e-7){elapsed+=16d;currentDay++;current=0d;rests++;}}return new TravelSchedule(currentDay,current,elapsed,rests);}
    }
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static int deterministicDie(String seed,int sides){byte[] bytes=digest(seed);long n=0;for(int i=0;i<Math.min(8,bytes.length);i++)n=(n<<8)|(bytes[i]&255L);return 1+(int)Math.floorMod(n,sides);}
    private static byte[] digest(String value){try{return MessageDigest.getInstance("SHA-256").digest((value==null?"":value).getBytes(StandardCharsets.UTF_8));}catch(Exception e){return new byte[]{(byte)(value==null?0:value.hashCode())};}}
    private static String signature(String value){byte[] b=digest(value);StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(12,b.length);i++)s.append(String.format(Locale.ROOT,"%02x",b[i]));return s.toString();}
    private static String summary(JSONObject r){return r.optString("rules_profile",r.optString("system"))+" • "+r.optDouble("distance_km",0)+" km • "+r.optDouble("hours",0)+" h • "+(r.optJSONArray("exhaustion_changes")==null?0:r.optJSONArray("exhaustion_changes").length())+" alteração(ões) de Exaustão";}
    private static boolean contains(String text,String... values){for(String v:values)if(text.contains(fold(v)))return true;return false;}
    private static String fold(String value){if(value==null)return "";return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
    private static String safe(String s){return s==null?"":s;}
    private static double round2(double v){return Math.round(v*100d)/100d;}
    private static double round3(double v){return Math.round(v*1000d)/1000d;}
}
