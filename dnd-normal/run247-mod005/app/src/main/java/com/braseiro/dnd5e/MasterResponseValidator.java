package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Validação estrutural final sem pedir uma segunda resposta ao Gemini. */
public final class MasterResponseValidator {
    private final AppDb db;
    private final KnowledgeFirewall firewall;
    private final MasterRulesArbiter rules;
    private final NpcAgencyEngine npcs;
    public MasterResponseValidator(AppDb database,KnowledgeFirewall knowledgeFirewall,MasterRulesArbiter arbiter,NpcAgencyEngine npcEngine){db=database;firewall=knowledgeFirewall;rules=arbiter;npcs=npcEngine;}

    public JSONObject validate(JSONObject envelope,String action){/*BR_JSON_WRAP*/try{
        if(envelope==null)envelope=new JSONObject();
        JSONArray warnings=new JSONArray();
        try{
            envelope=MasterProtocolV1.enforce(envelope);
            if(envelope.optBoolean("protocol_rejected",false))warnings.put("Protocolo estrutural incompatível; a narração foi preservada sem mutações.");
            normalizeCore(envelope,warnings);
            envelope=rules.validate(envelope,action);
            requireValid(envelope.optJSONObject("_rules_report"),"validador mecânico");
            envelope=npcs.validateUpdates(envelope);
            requireValid(envelope.optJSONObject("_npc_validation"),"validador de NPCs");
            envelope=firewall.sanitizeEnvelope(envelope,action);
            requireValid(envelope.optJSONObject("_firewall_report"),"firewall de conhecimento");
            validateNpcUpdates(envelope,warnings);
            validateWorldUpdates(envelope,warnings);
            // O mapa precisa ser validado antes da cena: a cena é projetada contra
            // o último current_hex_id realmente aceito, nunca contra uma proposta inválida.
            validateIntegrationUpdates(envelope,warnings);
            validateScene(envelope,warnings);
            validateOptions(envelope,warnings);
            JSONObject report=new JSONObject();report.put("warnings",warnings);
            appendNestedWarnings(report,envelope.optJSONObject("_rules_report"));
            appendNestedWarnings(report,envelope.optJSONObject("_firewall_report"));
            appendNestedWarnings(report,envelope.optJSONObject("_npc_validation"));
            report.put("valid",true);report.put("fail_closed",true);
            envelope.put("_master_validation",report);
        }catch(Exception failure){
            AppLog.e("MasterResponseValidator","Resposta do Mestre bloqueada pela validação fechada.",failure);
            try{
                clearStructuredMutations(envelope);
                warnings.put("Nenhuma consequência foi mantida: "+failure.getClass().getSimpleName());
                envelope.put("_master_validation",new JSONObject().put("warnings",warnings).put("valid",false).put("fail_closed",true).put("error",String.valueOf(failure.getMessage())));
            }catch(Exception ignored){AppLog.ignored("MasterResponseValidator","Falha ao fechar o envelope inválido.",ignored);}
        }
        return envelope;
    }catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}

    public boolean hasStructuredChanges(JSONObject env){if(env==null)return false;return length(env,"state_updates")>0||length(env,"npc_updates")>0||length(env,"npc_cognition_updates")>0||length(env,"knowledge_updates")>0||length(env,"quest_updates")>0||length(env,"faction_updates")>0||length(env,"clock_updates")>0||length(env,"world_events")>0||length(env,"event_resolutions")>0||length(env,"pending_resolutions")>0||length(env,"journal_updates")>0||length(env,"map_updates")>0||length(env,"tactical_updates")>0||nonEmpty(env.optJSONObject("scene_state"))||!"none".equalsIgnoreCase(env.optJSONObject("combat")==null?"none":env.optJSONObject("combat").optString("action","none"))||localTickChanges(env.optJSONObject("_local_tick"));}

    private void normalizeCore(JSONObject e,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{
        String rawNarration=e.optString("narration","");
        if(rawNarration.trim().length()==0){
            e.put("narration","A cena aguarda a ação da companhia.");
        }else{
            boolean internalLeak=NarrativeGuardService.containsInternalLeak(rawNarration);
            String sanitizedNarration=NarrativeGuardService.sanitize(rawNarration);
            if(!sanitizedNarration.equals(rawNarration))e.put("narration",sanitizedNarration);
            if(internalLeak){
                // Uma resposta contaminada jamais pode avançar a campanha.
                if(sanitizedNarration.trim().length()==0)
                    e.put("narration","A narração foi bloqueada por conter instruções internas. Nenhuma ação foi aplicada; tente novamente.");
                e.put("options",new JSONArray());
                throw new IllegalStateException("Vazamento de instruções do Mestre na resposta pública.");
            }
            NarrativeGuardService.ValidationResult vRes=NarrativeGuardService.validate(sanitizedNarration);
            if(!vRes.valid){for(String err:vRes.errors)warnings.put("Guard Narrativo: "+err);}
        }
        if(e.optJSONArray("options")==null)e.put("options",new JSONArray());if(e.optJSONArray("state_updates")==null)e.put("state_updates",new JSONArray());if(e.optJSONArray("npc_updates")==null)e.put("npc_updates",new JSONArray());if(e.optJSONArray("npc_cognition_updates")==null)e.put("npc_cognition_updates",new JSONArray());if(e.optJSONArray("knowledge_updates")==null)e.put("knowledge_updates",new JSONArray());if(e.optJSONArray("quest_updates")==null)e.put("quest_updates",new JSONArray());if(e.optJSONArray("faction_updates")==null)e.put("faction_updates",new JSONArray());if(e.optJSONArray("clock_updates")==null)e.put("clock_updates",new JSONArray());if(e.optJSONArray("world_events")==null)e.put("world_events",new JSONArray());if(e.optJSONArray("event_resolutions")==null)e.put("event_resolutions",new JSONArray());if(e.optJSONArray("pending_resolutions")==null)e.put("pending_resolutions",new JSONArray());if(e.optJSONArray("journal_updates")==null)e.put("journal_updates",new JSONArray());if(e.optJSONArray("map_updates")==null)e.put("map_updates",new JSONArray());if(e.optJSONArray("tactical_updates")==null)e.put("tactical_updates",new JSONArray());if(e.optJSONObject("combat")==null)e.put("combat",new JSONObject().put("action","none"));}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}
    private void validateScene(JSONObject e,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{JSONObject scene=e.optJSONObject("scene_state");if(scene==null)return;JSONObject current=db.getSceneState();int currentDay=Math.max(1,current.optInt("day_number",1));if(scene.has("day_number")&&scene.optInt("day_number",currentDay)<currentDay){scene.put("day_number",currentDay);warnings.put("O dia da campanha não pôde retroceder.");}AtlasScenePositionReconciler.reconcileProposal(db,e,current,scene,warnings);JSONObject delta=new JSONObject();String[] fields={"day_number","location","hex_id","time_of_day","weather","lighting","danger","objective","present_npcs","threats","clues","pending_consequences"};for(String f:fields){if(!scene.has(f)||scene.isNull(f))continue;Object value=scene.opt(f);String currentKey=f;if(f.equals("present_npcs")||f.equals("threats")||f.equals("clues")||f.equals("pending_consequences"))currentKey=f+"_json";String before=current.optString(currentKey,"");String after=value instanceof JSONArray?value.toString():String.valueOf(value);if(value instanceof String&&after.trim().length()==0)continue;if(f.equals("day_number")){if(scene.optInt(f,currentDay)!=currentDay)delta.put(f,scene.optInt(f));}else if(!normalizeJsonText(before).equals(normalizeJsonText(after)))delta.put(f,value);}e.put("scene_state",delta);}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}
    private void validateNpcUpdates(JSONObject e,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{JSONArray source=e.optJSONArray("npc_updates"),clean=new JSONArray();if(source==null)return;for(int i=0;i<source.length();i++){JSONObject n=NpcIdentityNormalizer.normalize(source.optJSONObject(i),db.getActiveMapProfile());if(n.length()==0){warnings.put("Uma identidade reservada ou inválida foi rejeitada como NPC.");continue;}JSONObject previous=db.findNpcMemoryByName(n.optString("name"));JSONObject x=new JSONObject();x.put("name",n.optString("name")).put("stable_id",n.optString("stable_id"));String[] fields={"summary","goals","attitude","relationship","knowledge","debts","last_seen","visual_description","stats_json","hex_id","voice_profile"};for(String f:fields)if(n.has(f)){Object value=n.opt(f);String before=previous.optString(f,"");String after=value==null?"":String.valueOf(value);if(!normalizeJsonText(before).equals(normalizeJsonText(after)))x.put(f,value);}if(x.length()>2||previous.length()==0)clean.put(x);}e.put("npc_updates",clean);}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}
    private void validateWorldUpdates(JSONObject e,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{
        e.put("quest_updates",filterNamed(e.optJSONArray("quest_updates"),new String[]{"id","title","status","stage","objective","stakes","progress_json","owner_character_id","faction_id"},30));
        JSONArray factions=filterNamed(e.optJSONArray("faction_updates"),new String[]{"id","name","agenda","resources","relationship","current_plan","location","active","plan_progress","plan_interval_hours","plan_progress_hours","plan_steps_json","plan_cycle","last_action_summary","objectives_json","resources_json","territories_json","relations_json","allies_json","enemies_json","information_json","costs_json","risks_json","success_conditions_json","failure_conditions_json","memory_json"},30);sanitizeFactionPlans(factions,warnings);e.put("faction_updates",factions);
        e.put("clock_updates",filterNamed(e.optJSONArray("clock_updates"),new String[]{"id","name","current_value","max_value","consequence","automatic","active","interval_hours","accumulated_hours","trigger_condition","faction_id","paused","rate_multiplier","tick_delta","regression_allowed","owner_entity","segments_json"},30));
        e.put("world_events",filterNamed(e.optJSONArray("world_events"),new String[]{"title","description","due_at","due_day","trigger_json","procedure_json","consequence_json"},30));

        JSONArray resolutions=new JSONArray(),raw=e.optJSONArray("event_resolutions");
        if(raw!=null)for(int i=0;i<raw.length()&&resolutions.length()<30;i++){
            JSONObject r=raw.optJSONObject(i);if(r==null||r.optLong("id",0)<=0)continue;
            JSONObject existing=db.getWorldEvent(r.optLong("id"));
            if(existing.length()==0||!"awaiting_review".equalsIgnoreCase(existing.optString("status"))){warnings.put("Somente eventos pendentes de revisão podem ser encerrados pelo Mestre.");continue;}
            String status=r.optString("status","resolved").toLowerCase(Locale.ROOT);
            if(!"resolved".equals(status)&&!"cancelled".equals(status)){warnings.put("Resolução de evento com estado inválido foi ignorada.");continue;}
            long transactionId=r.optLong("transaction_id",0);String resolutionKey=r.optString("resolution_key").trim();
            JSONObject pending=db.getPendingTransaction(transactionId);
            JSONObject payload=parseObject(pending.optString("payload_json","{}"));
            boolean correlated=transactionId>0&&"pending".equalsIgnoreCase(pending.optString("status"))&&payload.optLong("_world_event_id",0)==existing.optLong("id")&&resolutionKey.length()>0&&resolutionKey.equals(payload.optString("_effect_key"));
            if(!correlated){warnings.put("A resolução de "+existing.optString("title")+" não corresponde à sua pendência original e foi bloqueada.");continue;}
            if("resolved".equals(status)&&!envelopeHasVerifiedMechanicsForKey(e,resolutionKey)){warnings.put("O evento "+existing.optString("title")+" permaneceu pendente porque sua consequência não foi verificada com a chave correta.");continue;}
            resolutions.put(new JSONObject().put("id",existing.optLong("id")).put("transaction_id",transactionId).put("resolution_key",resolutionKey).put("status",status).put("note",trim(r.optString("note"),500)));
        }
        e.put("event_resolutions",resolutions);
        validatePendingResolutions(e,warnings);
    }catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}

    private void sanitizeFactionPlans(JSONArray factions,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{
        if(factions==null)return;
        for(int i=0;i<factions.length();i++){
            JSONObject faction=factions.optJSONObject(i);if(faction==null||!faction.has("plan_steps_json"))continue;
            Object raw=faction.opt("plan_steps_json");
            try{
                JSONArray source=raw instanceof JSONArray?(JSONArray)raw:new JSONArray(String.valueOf(raw));
                JSONArray clean=new JSONArray();
                for(int j=0;j<source.length()&&clean.length()<24;j++){
                    Object value=source.opt(j);
                    if(value instanceof String){String summary=trim(String.valueOf(value),400);if(summary.length()>0)clean.put(summary);continue;}
                    JSONObject step=source.optJSONObject(j);if(step==null)continue;
                    JSONObject x=new JSONObject();
                    for(String key:new String[]{"title","summary","agenda","resources","location","current_plan","next_plan","next_action","journal_title","journal_body"})if(step.has(key)&&step.optString(key).trim().length()>0)x.put(key,trim(step.optString(key),key.startsWith("journal_")?1200:500));
                    for(String key:new String[]{"relationship_delta","resource_delta"})if(step.has(key))x.put(key,Math.max(-1000,Math.min(1000,step.optInt(key))));
                    Object consequence=step.has("consequence_json")?step.opt("consequence_json"):step.opt("consequence");
                    if(consequence!=null&&consequence!=JSONObject.NULL){JSONObject parsed=consequence instanceof JSONObject?(JSONObject)consequence:parseObject(String.valueOf(consequence));if(parsed.length()>0)x.put("consequence_json",parsed);}
                    if(x.length()>0)clean.put(x);
                }
                faction.put("plan_steps_json",clean);
            }catch(Exception invalid){faction.remove("plan_steps_json");warnings.put("Um plano de facção inválido foi removido antes da revisão.");}
        }
    }catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}

    private void validatePendingResolutions(JSONObject e,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray clean=new JSONArray(),raw=e.optJSONArray("pending_resolutions");
        if(raw!=null)for(int i=0;i<raw.length()&&clean.length()<40;i++){
            JSONObject r=raw.optJSONObject(i);if(r==null)continue;
            long id=r.optLong("transaction_id",r.optLong("id",0));String key=r.optString("resolution_key").trim();
            JSONObject pending=db.getPendingTransaction(id);JSONObject payload=parseObject(pending.optString("payload_json","{}"));
            if(id<=0||pending.length()==0||!"pending".equalsIgnoreCase(pending.optString("status"))||key.length()==0||!key.equals(pending.optString("effect_key",payload.optString("_effect_key")))){warnings.put("Uma resolução pendente sem correlação exata foi ignorada.");continue;}
            String status=r.optString("status","applied").toLowerCase(Locale.ROOT);if(!"applied".equals(status)&&!"cancelled".equals(status)){warnings.put("Estado inválido de resolução pendente foi ignorado.");continue;}
            if("applied".equals(status)&&!envelopeHasVerifiedMechanicsForKey(e,key)){warnings.put("A pendência "+id+" continua aberta porque nenhum efeito mecânico verificado usa sua chave.");continue;}
            clean.put(new JSONObject().put("transaction_id",id).put("resolution_key",key).put("status",status).put("note",trim(r.optString("note"),500)));
        }
        e.put("pending_resolutions",clean);
    }catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}

    private static boolean envelopeHasVerifiedMechanicsForKey(JSONObject e,String key){
        if(e==null||key==null||key.trim().length()==0)return false;String wanted=key.trim();
        JSONArray updates=e.optJSONArray("state_updates");if(updates!=null)for(int i=0;i<updates.length();i++){JSONObject u=updates.optJSONObject(i);if(u==null)continue;JSONObject basis=u.optJSONObject("mechanical_basis");if(wanted.equals(u.optString("resolution_key"))||basis!=null&&wanted.equals(basis.optString("resolution_key")))return true;}
        JSONObject combat=e.optJSONObject("combat");if(combat!=null&&!"none".equalsIgnoreCase(combat.optString("action","none"))){JSONObject basis=combat.optJSONObject("mechanical_basis");if(wanted.equals(combat.optString("resolution_key"))||basis!=null&&wanted.equals(basis.optString("resolution_key")))return true;}
        return false;
    }

    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}

    private JSONArray filterNamed(JSONArray source,String[] fields,int max)throws Exception{/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();if(source==null)return out;for(int i=0;i<source.length()&&out.length()<max;i++){JSONObject row=source.optJSONObject(i);if(row==null)continue;boolean hasIdentity=row.optLong("id",0)>0||row.optString("name").trim().length()>0||row.optString("title").trim().length()>0;if(!hasIdentity)continue;JSONObject x=new JSONObject();for(String f:fields)if(row.has(f))x.put(f,row.opt(f));out.put(x);}return out;}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}
    private void validateIntegrationUpdates(JSONObject e,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray journals=new JSONArray(),source=e.optJSONArray("journal_updates");
        if(source!=null)for(int i=0;i<source.length()&&journals.length()<20;i++){
            JSONObject j=source.optJSONObject(i);if(j==null)continue;String body=trim(j.optString("body"),2400);if(body.length()==0)continue;
            journals.put(new JSONObject().put("title",trim(j.optString("title","Registro do Mestre"),120)).put("body",body).put("kind",trim(j.optString("kind","Mestre"),60)));
        }
        e.put("journal_updates",journals);

        JSONArray maps=new JSONArray();source=e.optJSONArray("map_updates");
        if(source!=null)for(int i=0;i<source.length()&&maps.length()<8;i++){
            JSONObject m=source.optJSONObject(i);if(m==null)continue;JSONObject x=new JSONObject();JSONObject mapState=db.getMapState();String packageId=mapState.optString("map_package_id","");
            for(String key:new String[]{"current_hex_id","destination_hex_id"})if(m.has(key)&&m.optString(key).trim().length()>0){String hex=trim(m.optString(key),120);if(packageId.length()==0||db.getRawMapHex(packageId,hex).length()>0)x.put(key,hex);else warnings.put("O hex "+hex+" não existe no atlas ativo e foi ignorado.");}
            if(m.has("route_json")){Object route=m.opt("route_json");try{JSONArray proposed=route instanceof JSONArray?(JSONArray)route:new JSONArray(String.valueOf(route));JSONArray valid=new JSONArray();for(int r=0;r<proposed.length()&&valid.length()<500;r++){String hex=proposed.optString(r).trim();if(hex.length()==0)continue;if(packageId.length()==0||db.getRawMapHex(packageId,hex).length()>0)valid.put(hex);else warnings.put("Um hex inexistente foi removido da rota: "+hex);}x.put("route_json",valid);}catch(Exception ignored){warnings.put("Uma rota inválida foi ignorada.");}}
            if(m.has("reveals_json")){Object reveals=m.opt("reveals_json");try{x.put("reveals_json",reveals instanceof JSONObject?reveals:new JSONObject(String.valueOf(reveals)));}catch(Exception ignored){warnings.put("Uma revelação de mapa inválida foi ignorada.");}}
            if(m.has("position_locked"))x.put("position_locked",m.optBoolean("position_locked"));if(m.has("overlay_visible"))x.put("overlay_visible",m.optBoolean("overlay_visible"));
            if(x.length()>0)maps.put(x);
        }
        e.put("map_updates",maps);

        JSONArray tactical=new JSONArray();source=e.optJSONArray("tactical_updates");
        if(source!=null)for(int i=0;i<source.length()&&tactical.length()<40;i++){
            JSONObject t=source.optJSONObject(i);if(t==null)continue;long id=t.optLong("combatant_id",0);String name=trim(t.optString("name"),100);if(id<=0&&name.length()==0)continue;
            if(!t.has("x")||!t.has("y")||!(t.opt("x") instanceof Number)||!(t.opt("y") instanceof Number))
                throw new IllegalArgumentException("Movimento tático exige destino x/y numérico explícito.");
            double xValue=t.optDouble("x",Double.NaN),yValue=t.optDouble("y",Double.NaN);
            if(Double.isNaN(xValue)||Double.isInfinite(xValue)||Double.isNaN(yValue)||Double.isInfinite(yValue)||
                    xValue<0d||xValue>=1d||yValue<0d||yValue>=1d)
                throw new IllegalArgumentException("Destino tático fora dos limites normalizados; movimento bloqueado.");
            JSONObject existing=id>0?findCombatantById(id):db.findCombatantByName(name);if(existing.length()==0){warnings.put("Um movimento tático foi ignorado porque o combatente não existe no combate atual.");continue;}
            if(id>0&&name.length()>0&&!existing.optString("name").equalsIgnoreCase(name))
                throw new IllegalArgumentException("Identidade e nome do movimento tático divergiram.");
            JSONObject x=new JSONObject();x.put("combatant_id",existing.optLong("id"));x.put("name",existing.optString("name"));x.put("x",xValue);x.put("y",yValue);tactical.put(x);
        }
        e.put("tactical_updates",tactical);
    }catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}

    private static void requireValid(JSONObject report,String component)throws Exception{if(report==null||!report.optBoolean("valid",false))throw new IllegalStateException(component+" não confirmou uma validação segura.");}
    private static void clearStructuredMutations(JSONObject e)throws Exception{/*BR_JSON_WRAP*/try{for(String key:new String[]{"state_updates","npc_updates","npc_cognition_updates","knowledge_updates","quest_updates","faction_updates","guild_updates","clock_updates","world_events","event_resolutions","pending_resolutions","journal_updates","map_updates","tactical_updates"})e.put(key,new JSONArray());e.put("combat",new JSONObject().put("action","none"));e.put("scene_state",new JSONObject());e.put("options",new JSONArray());e.put("_local_tick",new JSONObject());e.put("_deferred_until_test",true);}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}

    private JSONObject findCombatantById(long id){if(id<=0)return new JSONObject();for(JSONObject row:db.listCombatants())if(row.optLong("id")==id)return row;return new JSONObject();}
    private void validateOptions(JSONObject e,JSONArray warnings)throws Exception{/*BR_JSON_WRAP*/try{JSONArray source=e.optJSONArray("options"),out=new JSONArray();if(source!=null)for(int i=0;i<source.length()&&out.length()<2;i++){JSONObject o=source.optJSONObject(i);if(o==null)continue;String action=o.optString("action",o.optString("description","")).trim();if(action.length()==0)continue;JSONObject x=new JSONObject();x.put("label",trim(o.optString("label","Sugestão"),80));x.put("description",trim(o.optString("description"),180));x.put("action",trim(action,300));if(o.optJSONObject("test")!=null)x.put("test",o.optJSONObject("test"));out.put(x);}e.put("options",out);}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}
    private static boolean localTickChanges(JSONObject tick){if(tick==null)return false;return tick.optInt("hours_advanced",0)>0||length(tick,"events_triggered")>0||length(tick,"clocks_advanced")>0||(tick.optJSONObject("company_resources_consumed")!=null&&tick.optJSONObject("company_resources_consumed").optInt("days",0)>0);}
    private static boolean nonEmpty(JSONObject o){return o!=null&&o.length()>0;}
    private static int length(JSONObject o,String key){JSONArray a=o==null?null:o.optJSONArray(key);return a==null?0:a.length();}
    private static JSONArray filterWarnings(JSONObject report){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();if(report==null)return out;JSONArray w=report.optJSONArray("warnings");if(w!=null)for(int i=0;i<w.length();i++)out.put(w.opt(i));return out;}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}
    private static void appendNestedWarnings(JSONObject report,JSONObject nested)throws Exception{/*BR_JSON_WRAP*/try{JSONArray target=report.optJSONArray("warnings");if(target==null){target=new JSONArray();report.put("warnings",target);}JSONArray source=filterWarnings(nested);for(int i=0;i<source.length();i++)target.put(source.opt(i));}catch(RuntimeException __brRuntimeFailure106){throw __brRuntimeFailure106;}catch(Exception __brCheckedFailure106){throw new IllegalStateException(__brCheckedFailure106);}}
    private static String normalizeJsonText(String value){if(value==null)return "";return value.replaceAll("\\s+","").replace("\"","").trim();}
    private static String trim(String v,int max){String s=v==null?"":v.trim();return s.length()<=max?s:s.substring(0,max)+"…";}
}
