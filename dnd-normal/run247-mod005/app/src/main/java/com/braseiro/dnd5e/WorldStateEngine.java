package com.braseiro.dnd5e;

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

    public JSONObject preview(String playerAction){/*BR_JSON_WRAP*/try{
        JSONObject report=new JSONObject();try{
            int requestedHours=inferHours(playerAction);
            JSONObject scene=db.getSceneState();
            int currentDay=Math.max(1,scene.optInt("day_number",1));
            double currentHour=normalizeHour(scene.optDouble("clock_hour",hourFromLabel(scene.optString("time_of_day",""))));
            String previewFingerprint=worldTickFingerprint(scene);
            String clocksFingerprint=WorldClockSnapshotGuard.digest(db.listWorldClocks(true,1000));
            JSONObject travelPreview=travelRules.previewNarrativeAction(playerAction,requestedHours);
            double elapsedHours=requestedHours;
            if(travelPreview.optBoolean("requires_map_segment",false))elapsedHours=0d;
            else if(travelPreview.optBoolean("mechanics_apply",false))elapsedHours=Math.max(0d,travelPreview.optDouble("elapsed_hours",travelPreview.optDouble("movement_hours",requestedHours)));
            double total=currentHour+elapsedHours;
            int projectedDay=currentDay+(int)Math.floor(total/24d);
            double projectedHour=normalizeHour(total);
            report.put("world_tick_fingerprint",previewFingerprint);report.put("world_clocks_fingerprint",clocksFingerprint);report.put("hours_requested",requestedHours);report.put("hours_advanced",round2(elapsedHours));report.put("long_rest",isLongRest(playerAction));report.put("short_rest",isShortRest(playerAction));if(report.optBoolean("long_rest",false))report.put("long_rest_benefits",longRestPreview(currentDay,currentHour));report.put("day_before",currentDay);report.put("day_after",projectedDay);report.put("clock_hour_before",round2(currentHour));report.put("clock_hour_after",round2(projectedHour));report.put("time_before",scene.optString("time_of_day",labelForHour(currentHour)));report.put("time_after",labelForHour(projectedHour));report.put("travel_rules",travelPreview);

            JSONObject resourcePreview=travelPreview.optBoolean("mechanics_apply",false)?travelPreview.optJSONObject("resource_consumption"):travelRules.previewElapsedTimeResources(elapsedHours,"world-time|"+playerAction+"|"+currentDay+"|"+currentHour);report.put("company_resources_consumed",resourcePreview==null?new JSONObject():resourcePreview);

            boolean livingEnabled=new LivingWorldEngine(db).state().optInt("enabled",1)==1;
            report.put("living_world_enabled",livingEnabled);
            JSONArray triggered=new JSONArray();
            if(livingEnabled){
                long projectedNow=System.currentTimeMillis()+(long)Math.floor(Math.max(0d,elapsedHours)*60d*60d*1000d);
                for(JSONObject event:db.listUpcomingWorldEvents(1000)){
                    long dueAt=event.optLong("due_at",0);int dueDay=event.optInt("due_day",0);
                    if((dueAt>0&&dueAt<=projectedNow)||(dueDay>0&&dueDay<=projectedDay))triggered.put(event);
                }
            }
            report.put("events_triggered",triggered);
            report.put("event_consequence_preview",eventConsequences.preview(triggered));

            JSONArray advanced=new JSONArray();
            if(livingEnabled&&elapsedHours>0d)for(JSONObject clock:db.listWorldClocks(true,1000)){
                if(clock.optInt("automatic",0)!=1)continue;
                if(clock.optInt("paused",0)==1)continue;
                if(!clockConditionMatches(clock,playerAction,scene))continue;
                int before=clock.optInt("current_value",0),max=Math.max(1,clock.optInt("max_value",4));
                WorldClockProgression progression=WorldClockProgression.advance(before,max,clock.optDouble("accumulated_hours",0d),elapsedHours,clock.optDouble("interval_hours",6d),clock.optDouble("rate_multiplier",1d),clock.optInt("tick_delta",1),clock.optInt("regression_allowed",0)==1);
                int ticks=progression.ticks,delta=ticks*progression.tickDelta;double remainder=progression.remainder;int after=progression.after;JSONArray segmentEffects=clockSegmentEffects(clock,before,after);
                if(delta!=0||Math.abs(remainder-clock.optDouble("accumulated_hours",0d))>.001d){
                    advanced.put(new JSONObject().put("id",clock.optLong("id")).put("name",clock.optString("name"))
                            .put("before",before).put("after",after).put("max",max).put("delta",delta)
                            .put("interval_hours",progression.interval).put("rate_multiplier",progression.rate).put("tick_delta",progression.tickDelta).put("accumulated_after",round2(remainder))
                            .put("owner_entity",clock.optString("owner_entity")).put("segment_effects",segmentEffects).put("day",projectedDay).put("completed",after>=max));
                }
            }
            report.put("clocks_advanced",advanced);
            report.put("living_world_simulation",livingSimulation.preview(report));
            report.put("preview_only",true).put("world_tick_payload_fingerprint",WorldTickFingerprint.payload(report));
        }catch(Exception error){try{report.put("error",error.getMessage()==null?"Falha desconhecida":error.getMessage());}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha tolerada; a execução continuou.",ignored);}}return report;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized boolean commit(final JSONObject report){/*BR_JSON_WRAP*/try{
        if(report==null)return false;
        if(!report.optBoolean("preview_only",false)||report.optBoolean("committed",false))return false;
        final String expected=report.optString("world_tick_fingerprint","").trim();
        final String expectedClocks=report.optString("world_clocks_fingerprint","").trim();
        final String expectedPayload=report.optString("world_tick_payload_fingerprint","").trim();
        String initialRejection=WorldTickCommitGuard.rejection(report.optBoolean("preview_only",false),report.optBoolean("committed",false),expectedPayload,WorldTickFingerprint.payload(report),expected,worldTickFingerprint(db.getSceneState()),expectedClocks,WorldClockSnapshotGuard.digest(db.listWorldClocks(true,1000)));
        if(!initialRejection.isEmpty()){
            try{report.put("commit_error",commitRejectionMessage(initialRejection));}catch(Exception ignored){}
            return false;
        }
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
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{/*BR_JSON_WRAP*/try{
                // A previous check outside SQL is not sufficient: another
                // thread may mutate the world between preview and beginTransaction.
                String transactionRejection=WorldTickCommitGuard.rejection(report.optBoolean("preview_only",false),report.optBoolean("committed",false),expectedPayload,WorldTickFingerprint.payload(report),expected,worldTickFingerprint(db.getSceneState()),expectedClocks,WorldClockSnapshotGuard.digest(db.listWorldClocks(true,1000)));
                if(!transactionRejection.isEmpty())throw new IllegalStateException(commitRejectionMessage(transactionRejection));
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
                    double absoluteAfter=(Math.max(1,scene.optInt("day_number",1))-1)*24d+scene.optDouble("clock_hour",0d);
                    report.put("timed_character_recoveries",db.resolveTimedCharacterRecoveries(absoluteAfter));
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

                if(report.optBoolean("long_rest",false)){applyLongRestBenefits(report);travelRules.resetDailyTravelChecked();}
                else if(report.optBoolean("short_rest",false))applyShortRestBenefits(report);
                db.addAuditEvent("mundo","Avanço local do mundo",summary(report),"{}",report.toString(),true);
            }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});
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
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private String worldTickFingerprint(JSONObject scene){
        JSONObject snapshot=scene==null?new JSONObject():scene;
        int day=Math.max(1,snapshot.optInt("day_number",1));
        double hour=normalizeHour(snapshot.optDouble("clock_hour",hourFromLabel(snapshot.optString("time_of_day",""))));
        String hex=snapshot.optString("hex_id",snapshot.optString("current_hex_id"));
        return WorldTickFingerprint.of(db.getActiveMapProfile(),day,hour,snapshot.optString("location"),hex,CanonicalJson.stringify(snapshot));
    }

    private static String commitRejectionMessage(String reason){
        if("world_tick_payload_invalid_or_changed".equals(reason))return "A prévia do mundo está ausente ou foi alterada depois de gerada.";
        if("world_tick_scene_stale".equals(reason))return "A cena mudou desde a prévia; o avanço foi descartado para evitar replay sobre estado obsoleto.";
        if("world_tick_clocks_stale".equals(reason))return "Os relógios mudaram desde a prévia; avanço descartado.";
        if("world_tick_already_committed".equals(reason))return "Esta prévia do mundo já foi aplicada.";
        return "A prévia do mundo não está autorizada para commit.";
    }


    // ---------- GAMEPLAY-001: sandbox persistente Faerûn + Eberron ----------
    /** Estado sandbox salvo dentro das preferências da companhia, sem alterar schema. */
    public JSONObject gameplaySandboxState(){/*BR_JSON_WRAP*/try{
        JSONObject party=db.getPartyState();JSONObject prefs=parseObject(party.optString("session_preferences_json","{}"));JSONObject state=prefs.optJSONObject("gameplay_sandbox");
        return GameplaySandboxRuntimeEngine.ensureState(state==null?new JSONObject():state);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Contexto confirmado para Mestre/Gemini; não concede autoridade mecânica ao modelo. */
    public JSONObject gameplaySandboxContext(){/*BR_JSON_WRAP*/try{return GameplaySandboxRuntimeEngine.contextSnapshot(gameplaySandboxState(),db.getSceneState());}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Prévia de pergunta/intenção/ação. Nunca persiste e nunca rola dados. */
    public JSONObject previewGameplayInput(String text,String inputChannel,JSONObject payload){/*BR_JSON_WRAP*/try{
        JSONObject out=GameplaySandboxRuntimeEngine.previewInput(gameplaySandboxState(),text,inputChannel,payload==null?new JSONObject():payload);
        out.put("world_context",gameplaySandboxContext());return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * Comita somente continuidade narrativa já confirmada/resolvida. Testes manuais
     * exigem confirmação do gateway mecânico; a camada GAMEPLAY não decide sucesso.
     */
    public synchronized JSONObject commitGameplayInput(String text,String inputChannel,JSONObject payload){/*BR_JSON_WRAP*/try{
        final JSONObject result=GameplaySandboxRuntimeEngine.resolveInput(gameplaySandboxState(),text,inputChannel,payload==null?new JSONObject():payload);
        if(!result.optBoolean("sandbox_mutated",false))return result;
        final JSONObject next=result.optJSONObject("state");if(next==null)return result.put("sandbox_mutated",false).put("status","state_missing");
        try{db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{/*BR_JSON_WRAP*/try{
            saveGameplaySandboxStateChecked(next);
            String location=result.optJSONObject("meta")==null?"":result.optJSONObject("meta").optString("scene_location_after","").trim();
            if(location.length()>0){JSONObject scene=db.getSceneState();scene.put("location",location);db.saveSceneStateChecked(scene);}
            db.addAuditEvent("gameplay_sandbox","Ação sandbox resolvida",result.optString("status","resolved"),"{}",result.toString(),true);
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});}
        catch(Exception e){return result.put("sandbox_mutated",false).put("status","persistence_error").put("persistence_error",String.valueOf(e.getMessage()));}
        return result.put("persisted",true);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Estagia viagem sem custo nem troca de destino; ambos aguardam gateway ENGINE. */
    public synchronized JSONObject beginGameplayTravel(String origin,String destination,String service,JSONObject payload){/*BR_JSON_WRAP*/try{
        final JSONObject result=GameplaySandboxRuntimeEngine.beginTravel(gameplaySandboxState(),origin,destination,service,payload==null?new JSONObject():payload);
        if(result.optBoolean("sandbox_mutated",false))try{saveGameplaySandboxStateChecked(result.optJSONObject("state"));result.put("persisted",true);}catch(Exception e){result.put("sandbox_mutated",false).put("status","persistence_error").put("persistence_error",String.valueOf(e.getMessage()));}
        return result;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * Resolve serviço de Faerûn pelo gateway existente do ENGINE. A transição de local
     * só ocorre quando todas as entradas de recurso já foram validadas pelo ENGINE.
     * Gateway, recurso/estado do personagem, sandbox e cena ficam na mesma transação.
     */
    public synchronized JSONObject resolveGameplayFaerunTravelService(final long characterId,final String item,final String action,JSONObject payload){/*BR_JSON_WRAP*/try{
        final JSONObject p=payload==null?new JSONObject():payload;final JSONObject before=gameplaySandboxState();
        JSONObject travel=before.optJSONObject("travel");if(travel==null||!"awaiting_engine_gateway".equals(travel.optString("status","")))return new JSONObject().put("state",before).put("sandbox_mutated",false).put("status","travel_not_staged").put("auto_rolls",false);
        if(!p.optBoolean("confirm",false))return new JSONObject().put("state",before).put("sandbox_mutated",false).put("status","travel_confirmation_required").put("resource_spend_allowed",false).put("destination_change_allowed",false).put("auto_rolls",false);
        if(!p.optBoolean("engine_resource_commit_confirmed",false))return new JSONObject().put("state",before).put("sandbox_mutated",false).put("status","engine_resource_commit_required").put("resource_spend_allowed",false).put("destination_change_allowed",false).put("auto_rolls",false);
        final JSONObject[] holder=new JSONObject[1];
        try{db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{/*BR_JSON_WRAP*/try{
            JSONObject gateway=db.resolveHeroesAuroraAction(characterId,item,action,p);
            JSONObject result=GameplaySandboxRuntimeEngine.completeTravelAfterGateway(before,gateway,p).put("mechanical_gateway",gateway);holder[0]=result;
            if(!result.optBoolean("sandbox_mutated",false))throw new GameplayGatewayPending();
            JSONObject next=result.optJSONObject("state");saveGameplaySandboxStateChecked(next);
            String location=result.optJSONObject("meta")==null?"":result.optJSONObject("meta").optString("scene_location_after","").trim();
            if(location.length()>0){JSONObject scene=db.getSceneState();scene.put("location",location);db.saveSceneStateChecked(scene);}
            db.addAuditEvent("gameplay_travel","Viagem de Faerûn resolvida",item+" / "+action,"{}",result.toString(),true);
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});}
        catch(GameplayGatewayPending pending){return holder[0]==null?new JSONObject().put("state",before).put("sandbox_mutated",false).put("status","mechanical_gateway_not_resolved").put("auto_rolls",false):holder[0].put("persisted",false);}
        catch(Exception e){return (holder[0]==null?new JSONObject().put("state",before):holder[0]).put("sandbox_mutated",false).put("status","persistence_error").put("persistence_error",String.valueOf(e.getMessage())).put("auto_rolls",false);}
        return holder[0].put("persisted",true);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Frameworks Eberron usam o gateway persistente real já existente no AppDb. */
    public synchronized JSONObject resolveGameplayEberronFramework(final long characterId,final String framework,final String action,JSONObject payload){/*BR_JSON_WRAP*/try{
        final JSONObject p=payload==null?new JSONObject():payload;final JSONObject before=gameplaySandboxState();final JSONObject[] holder=new JSONObject[1];
        try{db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{/*BR_JSON_WRAP*/try{
            JSONObject gateway=db.resolveEberronFrameworkAction(characterId,framework,action,p);
            JSONObject result=GameplaySandboxRuntimeEngine.recordFrameworkGateway(before,framework,gateway,p).put("mechanical_gateway",gateway);holder[0]=result;
            if(!result.optBoolean("sandbox_mutated",false))throw new GameplayGatewayPending();
            saveGameplaySandboxStateChecked(result.optJSONObject("state"));db.addAuditEvent("gameplay_eberron","Framework Eberron resolvido",framework+" / "+action,"{}",result.toString(),true);
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});}
        catch(GameplayGatewayPending pending){return holder[0]==null?new JSONObject().put("state",before).put("sandbox_mutated",false).put("status","framework_gateway_not_resolved").put("auto_rolls",false):holder[0].put("persisted",false);}
        catch(Exception e){return (holder[0]==null?new JSONObject().put("state",before):holder[0]).put("sandbox_mutated",false).put("status","persistence_error").put("persistence_error",String.valueOf(e.getMessage())).put("auto_rolls",false);}
        return holder[0].put("persisted",true);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Relação/renome de facção permanece exclusivamente GM-confirmed no gateway Heroes. */
    public synchronized JSONObject resolveGameplayFaerunFaction(final long characterId,final String faction,final String action,JSONObject payload){/*BR_JSON_WRAP*/try{
        final JSONObject p=payload==null?new JSONObject():payload;final JSONObject before=gameplaySandboxState();final JSONObject[] holder=new JSONObject[1];
        try{db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{/*BR_JSON_WRAP*/try{
            JSONObject gateway=db.resolveHeroesFactionAction(characterId,faction,action,p);
            JSONObject result=GameplaySandboxRuntimeEngine.recordFactionGateway(before,faction,gateway,p).put("mechanical_gateway",gateway);holder[0]=result;
            if(!result.optBoolean("sandbox_mutated",false))throw new GameplayGatewayPending();
            saveGameplaySandboxStateChecked(result.optJSONObject("state"));db.addAuditEvent("gameplay_faction","Contexto de facção resolvido",faction+" / "+action,"{}",result.toString(),true);
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});}
        catch(GameplayGatewayPending pending){return holder[0]==null?new JSONObject().put("state",before).put("sandbox_mutated",false).put("status","faction_gateway_not_resolved").put("auto_rolls",false):holder[0].put("persisted",false);}
        catch(Exception e){return (holder[0]==null?new JSONObject().put("state",before):holder[0]).put("sandbox_mutated",false).put("status","persistence_error").put("persistence_error",String.valueOf(e.getMessage())).put("auto_rolls",false);}
        return holder[0].put("persisted",true);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Renome de divindade: GAMEPLAY fornece contexto; ENGINE só altera com confirmação GM. */
    public synchronized JSONObject resolveGameplayFaerunDeityRenown(final long characterId,final String deity,final int delta,JSONObject payload){/*BR_JSON_WRAP*/try{
        final JSONObject p=payload==null?new JSONObject():payload;final JSONObject before=gameplaySandboxState();final JSONObject[] holder=new JSONObject[1];
        try{db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run() throws Exception{/*BR_JSON_WRAP*/try{
            JSONObject gateway=db.resolveHeroesDeityRenown(characterId,deity,delta,p);
            JSONObject result=GameplaySandboxRuntimeEngine.recordDeityGateway(before,deity,gateway,p).put("mechanical_gateway",gateway);holder[0]=result;
            if(!result.optBoolean("sandbox_mutated",false))throw new GameplayGatewayPending();
            saveGameplaySandboxStateChecked(result.optJSONObject("state"));db.addAuditEvent("gameplay_deity","Renome de divindade resolvido",deity,"{}",result.toString(),true);
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});}
        catch(GameplayGatewayPending pending){return holder[0]==null?new JSONObject().put("state",before).put("sandbox_mutated",false).put("status","deity_gateway_not_resolved").put("auto_rolls",false):holder[0].put("persisted",false);}
        catch(Exception e){return (holder[0]==null?new JSONObject().put("state",before):holder[0]).put("sandbox_mutated",false).put("status","persistence_error").put("persistence_error",String.valueOf(e.getMessage())).put("auto_rolls",false);}
        return holder[0].put("persisted",true);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Narrativa do Gemini é filtrada; esta função nunca salva estado. */
    public JSONObject gateGeminiGameplayProposal(JSONObject proposal){return GameplaySandboxRuntimeEngine.gateGeminiProposal(proposal);}

    public JSONArray faerunGameplayRegions(){return GameplaySandboxRuntimeEngine.faerunRegions();}
    public JSONArray eberronGameplayFrameworks(){return GameplaySandboxRuntimeEngine.eberronFrameworks();}
    /** Inventário acionável sem converter lore puro em regra. */
    public JSONObject faerunGameplayInventory(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();out.put("source",GameplaySandboxRuntimeEngine.SOURCE_FAERUN).put("regions",faerunGameplayRegions())
            .put("factions",ForgottenRealmsHeroesEngine.factions()).put("travel_services",ForgottenRealmsHeroesEngine.auroraServices())
            .put("mounts",ForgottenRealmsHeroesEngine.auroraMounts()).put("vehicles",ForgottenRealmsHeroesEngine.auroraVehicles())
            .put("windskiff",ForgottenRealmsHeroesEngine.auroraInfo("Windskiff"))
            .put("classification",new JSONObject().put("regions","estado_sandbox/lore_context").put("factions","faccao").put("travel_services","viagem/servico/economia").put("mounts","viagem/economia").put("vehicles","viagem/economia"))
            .put("fine_atlas_status","pending_source_exception_audit").put("scripted_adventure",false).put("auto_rolls",false);return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void saveGameplaySandboxStateChecked(JSONObject state)throws Exception{
        if(state==null)throw new IllegalArgumentException("Estado sandbox ausente.");
        JSONObject party=db.getPartyState();JSONObject prefs=parseObject(party.optString("session_preferences_json","{}"));
        JSONObject safe=GameplaySandboxRuntimeEngine.ensureState(state);int revision=safe.optInt("revision",0);prefs.put("gameplay_sandbox",safe);party.put("session_preferences_json",prefs.toString());db.savePartyStateChecked(party);
        JSONObject afterParty=db.getPartyState();JSONObject afterPrefs=parseObject(afterParty.optString("session_preferences_json","{}"));JSONObject after=afterPrefs.optJSONObject("gameplay_sandbox");
        if(after==null||after.optInt("revision",-1)!=revision)throw new IllegalStateException("O estado sandbox não foi persistido integralmente.");
    }

    private static final class GameplayGatewayPending extends RuntimeException { private static final long serialVersionUID=1L; }

    public String compactContext(){return compactContext("");}
    public String compactContext(String focus){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();try{
            String contextFocus=focus+" "+db.getSceneState().optString("location")+" "+db.getSceneState().optString("objective");out.put("quests",WorldContextSelector.select(db.listQuests("active",1000),contextFocus,16,"title","objective","stage","stakes"));out.put("factions_director_only",WorldContextSelector.select(db.listFactions(1000),contextFocus,12,"name","agenda","current_plan","location"));out.put("clocks",WorldContextSelector.select(db.listWorldClocks(true,1000),contextFocus,20,"name","consequence","owner_entity"));out.put("upcoming_events_director_only",WorldContextSelector.select(db.listUpcomingWorldEvents(1000),contextFocus,16,"title","description","event_key"));
            JSONArray visible=new JSONArray(),hidden=new JSONArray();String normalized=normalize(focus);for(JSONObject fact:db.listKnowledgeFacts(80)){
                boolean relevant=isRelevantFact(fact,normalized);String visibility=fact.optString("visibility");if("mestre".equals(visibility)){if(hidden.length()<8&&(relevant||hidden.length()<3))hidden.put(compactFact(fact));}else if(visible.length()<14&&(relevant||visible.length()<6))visible.put(compactFact(fact));
            }
            out.put("visible_knowledge",visible);out.put("director_only_hidden_facts",hidden);out.put("hidden_facts_rule","Use para planejar, nunca revele diretamente antes da condição de revelação.");
        }catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha tolerada; a execução continuou.",ignored);}return fitJson(out,6200);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONObject compactFact(JSONObject fact){/*BR_JSON_WRAP*/try{JSONObject out=new JSONObject();try{out.put("subject",fact.optString("subject"));out.put("content",trim(fact.optString("content"),420));out.put("truth_status",fact.optString("truth_status"));out.put("visibility",fact.optString("visibility"));out.put("known_by_json",fact.optString("known_by_json","[]"));out.put("reveal_condition",trim(fact.optString("reveal_condition"),220));out.put("source",trim(fact.optString("source"),160));}catch(Exception ignored){AppLog.ignored("WorldStateEngine","Falha tolerada; a execução continuou.",ignored);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
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

    private JSONArray clockSegmentEffects(JSONObject clock,int before,int after)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();JSONArray configured=parseArray(clock.optString("segments_json","[]"));int low=Math.min(before,after),high=Math.max(before,after);
        for(int i=0;i<configured.length();i++){JSONObject segment=configured.optJSONObject(i);if(segment==null)continue;int value=segment.optInt("value",segment.optInt("segment",-1));if(value<=low||value>high)continue;JSONObject copy=new JSONObject(segment.toString()).put("clock_id",clock.optLong("id")).put("direction",after>=before?"advance":"regress").put("effect_key","clock:"+clock.optLong("id")+":segment:"+value+":"+(after>=before?"up":"down"));out.put(copy);}
        if(after>=clock.optInt("max_value",4)&&before<clock.optInt("max_value",4)&&clock.optString("consequence").trim().length()>0)out.put(new JSONObject().put("clock_id",clock.optLong("id")).put("value",after).put("title",clock.optString("name")+" concluído").put("narrative",clock.optString("consequence")).put("effect_key","clock:"+clock.optLong("id")+":completion"));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private void applyClockSegmentEffects(JSONObject clock)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray effects=clock.optJSONArray("segment_effects");if(effects==null)return;for(int i=0;i<effects.length();i++){JSONObject effect=effects.optJSONObject(i);if(effect==null)continue;String key=effect.optString("effect_key");if(key.length()==0)continue;if(db.hasEffectReceipt(key)){if(key.endsWith(":completion"))db.setWorldClockCompletionReceipt(clock.optLong("id"),key);continue;}JSONObject consequence=effect.optJSONObject("consequence_json");if(consequence==null)consequence=parseObject(effect.optString("consequence_json","{}"));if(consequence.length()==0){consequence=new JSONObject().put("journal_updates",new JSONArray().put(new JSONObject().put("title",effect.optString("title",clock.optString("name"))).put("body",effect.optString("narrative","Segmento do relógio alcançado.")).put("kind","Relógio")));}
            if(eventConsequences.canApplyStructured(consequence)){eventConsequences.applyStructured(consequence,"Relógio — "+clock.optString("name"),key);if(key.endsWith(":completion")&&db.hasEffectReceipt(key))db.setWorldClockCompletionReceipt(clock.optLong("id"),key);}else db.addPendingTransactionUnique("Relógio pendente — "+clock.optString("name"),"A consequência do segmento exige procedimento mecânico e revisão.",new JSONObject().put("clock_id",clock.optLong("id")).put("segment",effect).put("proposed_consequence",consequence).put("requires_master_review",true),key);
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject longRestPreview(int day,double hour)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();JSONObject state=db.getTravelRulesState();double absoluteStart=(Math.max(1,day)-1)*24d+normalizeHour(hour),lastCompletion=state.optDouble("last_long_rest_completed_absolute_hour",Double.NEGATIVE_INFINITY);boolean timeEligible=!Double.isFinite(lastCompletion)||absoluteStart-lastCompletion>=16d;JSONArray heroes=new JSONArray();
        for(JSONObject ch:db.listActiveCharacters()){JSONObject sheet=parseObject(ch.optString("sheet_json","{}"));int hp=ch.optInt("hp",sheet.optInt("hp",0));heroes.put(new JSONObject().put("id",ch.optLong("id")).put("name",ch.optString("name")).put("hp",hp).put("eligible",timeEligible&&hp>0));}
        return out.put("time_eligible",timeEligible).put("requires_positive_hp",true).put("last_completion_absolute_hour",Double.isFinite(lastCompletion)?lastCompletion:JSONObject.NULL).put("characters",heroes);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void applyLongRestBenefits(JSONObject report)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject preview=report.optJSONObject("long_rest_benefits");if(preview==null||!preview.optBoolean("time_eligible",false)){report.put("long_rest_mechanical_benefits_applied",false).put("long_rest_blocked_reason","rest_frequency");return;}
        DndRulesEngine rules=new DndRulesEngine();String campaignProfile=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014);JSONArray applied=new JSONArray(),blocked=new JSONArray();
        for(JSONObject ch:db.listActiveCharacters()){long id=ch.optLong("id",0);JSONObject sheet=parseObject(ch.optString("sheet_json","{}"));int hp=ch.optInt("hp",sheet.optInt("hp",0));if(id<=0||hp<=0){blocked.put(new JSONObject().put("id",id).put("name",ch.optString("name")).put("reason","requires_positive_hp"));continue;}
            int max=Math.max(1,ch.optInt("max_hp",sheet.optInt("max_hp",1)));db.setCharacterHpChecked(id,max);db.setCharacterTempHpChecked(id,0);JSONObject latest=db.getCharacter(id),next=parseObject(latest.optString("sheet_json","{}"));
            String profile=latest.optString("rules_source","").trim();if(profile.length()==0)profile=next.optString("rules_source",next.optString("rules_profile",campaignProfile)).trim();if(profile.length()==0)profile=campaignProfile;if(rules.is2024(profile)){int ex=Math.max(0,Math.min(6,next.optInt("exhaustion",0)));next.put("exhaustion",Math.max(0,ex-1));}
            next.put("hp",max).put("max_hp",max).put("temp_hp",0);next=CharacterResourceStateEngine.longRest(next,latest.optString("class_name"),latest.optInt("level",1),profile);MultiverseCharacterOptionEngine.longRest(next);XanatharRacialFeatRuntimeEngine.longRest(next);CrossBookSubclassRuntimeEngine.longRest(next);CrossBookFeatRuntimeEngine.onLongRest(next);Core2024EberronCharacterEngine.onLongRest(next);VoloSubraceHandbookEngine.longRest(next);VoloSubraceRun139Engine.longRest(next);VoloSubraceRun140Engine.longRest(next);VoloRacialFeatRuntimeEngine.longRest(next);Run141SpeciesFeatRuntimeEngine.longRest(next);Run141CompactCharacterOptionEngine.longRest(next);Run143ForgottenSecretsEngine.longRest(next);Run149ForgottenSecretsSpellEngine.longRest(next);ArcanaUnleashedRun143Engine.longRest(next);if(RuneterraClassRuntimeEngine.supports(latest.optString("class_name")))RuneterraClassRuntimeEngine.longRest(next,latest.optInt("level",1));next.put("short_rest_hit_dice_choice_pending",false);
            if(rules.is2024(profile)){
                java.util.List<Core2024PreparedSpellLedger.ClassEntry> spellClasses=
                    Core2024PreparedSpellLedger.classes(latest,next);
                if(!spellClasses.isEmpty())Core2024PreparedSpellLedger.openEvent(next,spellClasses,
                    "long_rest",report.optInt("day_after",1)+":"+
                    report.optDouble("clock_hour_after",0d));
            }
            db.updateCharacterSheetChecked(id,next.toString());applied.put(new JSONObject().put("id",id).put("name",latest.optString("name")).put("hp_after",max).put("exhaustion_after",next.optInt("exhaustion",0)).put("hit_dice_spent_after",next.optInt("hit_dice_spent",0)).put("spell_slots_current",next.optJSONArray("spell_slots_current")));
        }
        JSONObject state=db.getTravelRulesState();double completed=(Math.max(1,report.optInt("day_after",1))-1)*24d+normalizeHour(report.optDouble("clock_hour_after",0d));state.put("last_long_rest_completed_absolute_hour",completed);db.saveTravelRulesStateChecked(state);
        report.put("long_rest_mechanical_benefits_applied",true).put("long_rest_characters_applied",applied).put("long_rest_characters_blocked",blocked).put("long_rest_partial_resources_pending",false);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void applyShortRestBenefits(JSONObject report)throws Exception{/*BR_JSON_WRAP*/try{JSONArray applied=new JSONArray();String campaignProfile=db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014);for(JSONObject ch:db.listActiveCharacters()){long id=ch.optLong("id",0);if(id<=0)continue;JSONObject sheet=parseObject(ch.optString("sheet_json","{}"));String profile=ch.optString("rules_source",sheet.optString("rules_source",campaignProfile));JSONObject next=CharacterResourceStateEngine.shortRest(sheet,ch.optString("class_name"),ch.optInt("level",1),profile);XanatharRacialFeatRuntimeEngine.shortRest(next);CrossBookSubclassRuntimeEngine.shortRest(next);Core2024EberronCharacterEngine.onShortRest(next);VoloSubraceHandbookEngine.shortRest(next);VoloSubraceRun139Engine.shortRest(next);VoloSubraceRun140Engine.shortRest(next);VoloRacialFeatRuntimeEngine.shortRest(next);Run141SpeciesFeatRuntimeEngine.shortRest(next);Run141CompactCharacterOptionEngine.shortRest(next);Run143ForgottenSecretsEngine.shortRest(next);Run149ForgottenSecretsSpellEngine.shortRest(next);ArcanaUnleashedRun143Engine.shortRest(next);if(RuneterraClassRuntimeEngine.supports(ch.optString("class_name")))RuneterraClassRuntimeEngine.shortRest(next,ch.optInt("level",1));
            if(new DndRulesEngine().is2024(profile)){
                java.util.List<Core2024PreparedSpellLedger.ClassEntry> spellClasses=
                    Core2024PreparedSpellLedger.classes(ch,next);
                if(!spellClasses.isEmpty())Core2024PreparedSpellLedger.openEvent(next,spellClasses,
                    "short_rest",report.optInt("day_after",1)+":"+
                    report.optDouble("clock_hour_after",0d));
            }
            db.updateCharacterSheetChecked(id,next.toString());applied.put(new JSONObject().put("id",id).put("name",ch.optString("name")).put("available_hit_dice",Math.max(0,next.optInt("hit_dice_max",ch.optInt("level",1))-next.optInt("hit_dice_spent",0))).put("hit_dice_choice_pending",next.optBoolean("short_rest_hit_dice_choice_pending",false)));}report.put("short_rest_resources_applied",true).put("short_rest_characters",applied);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static boolean isShortRest(String action){String n=action==null?"":action.toLowerCase(Locale.ROOT);return n.contains("descanso curto");}
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
        if(n.contains("descanso curto"))return 1;
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
    private static String fitJson(JSONObject source,int max){/*BR_JSON_WRAP*/try{String raw=source.toString();if(raw.length()<=max)return raw;try{JSONObject lean=new JSONObject();lean.put("quests",source.optJSONArray("quests"));lean.put("clocks",source.optJSONArray("clocks"));lean.put("visible_knowledge",source.optJSONArray("visible_knowledge"));lean.put("director_only_hidden_facts",source.optJSONArray("director_only_hidden_facts"));lean.put("hidden_facts_rule",source.optString("hidden_facts_rule"));lean.put("context_reduced",true);raw=lean.toString();if(raw.length()<=max)return raw;return new JSONObject().put("context_reduced",true).put("excerpt",raw.substring(0,Math.min(max-80,raw.length()))).toString();}catch(Exception e){return "{\"context_reduced\":true}";}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String summary(JSONObject report){return report.optDouble("hours_advanced",0)+" hora(s) • dia "+report.optInt("day_before",1)+" → "+report.optInt("day_after",1)+" • "+(report.optJSONArray("events_triggered")==null?0:report.optJSONArray("events_triggered").length())+" evento(s) • "+(report.optJSONArray("clocks_advanced")==null?0:report.optJSONArray("clocks_advanced").length())+" relógio(s)";}
}
