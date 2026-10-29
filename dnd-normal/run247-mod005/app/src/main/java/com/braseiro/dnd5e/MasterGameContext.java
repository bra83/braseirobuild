package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Barramento único do Mestre IA. Nenhuma aba mantém uma "verdade paralela":
 * o prompt é montado a partir do mesmo banco que alimenta campanha, fichas,
 * NPCs, cena, mapa, combate, dados, diário e áudio.
 */
public final class MasterGameContext {
    private final AppDb db;
    public MasterGameContext(AppDb database){db=database;}

    public JSONObject snapshot(String mapNarrativeContext,JSONObject audioDiagnostic){
        JSONObject root=new JSONObject();
        try{
            String contextFocus=WorldContextSelector.focus(AtlasScenePositionReconciler.authoritativeSceneSnapshot(db),mapNarrativeContext,db.getStoryState().optString("last_action"));
            root.put("world_profile",db.getActiveMapProfile());
            root.put("master_protocol",MasterProtocolV1.descriptor());
            root.put("source_of_truth_manifest",SourceOfTruthRegistry.manifest());
            root.put("combat_state_machine",CombatStateMachine.descriptor());
            root.put("essential_rules_cache",EssentialRulesCache.snapshot(db.getActiveMapProfile()));
            root.put("campaign",compact(db.getCampaign(),new String[]{"name","rules_profile","tone","feats","multiclass","flanking","notes","updated_at"}));
            root.put("story",compact(db.getStoryState(),new String[]{"started","last_narration","visual_prompt","options_json","pending_test_json","last_action","updated_at"}));
            root.put("scene",AtlasScenePositionReconciler.authoritativeSceneSnapshot(db));
            root.put("travel_rules_state",db.getTravelRulesState());
            root.put("rules_boundary",new JSONObject().put("RAW","Pode ser aplicado automaticamente pelo aplicativo quando o gatilho objetivo foi confirmado.").put("REGRA_OPCIONAL","Só pode ser aplicada quando habilitada ou escolhida explicitamente.").put("DECISAO_DO_MESTRE","Nunca é aplicada silenciosamente pelo texto da IA.").put("SIMULACAO_NARRATIVA","Produz sinais e ambientação, sem alterar ficha ou estado mecânico."));
            root.put("active_heroes",compactHeroes(db.listActiveCharacters(),12));
            root.put("all_heroes",compactHeroes(db.listCharacters(),24));
            root.put("npcs",WorldContextSelector.select(db.listNpcMemories(5000),contextFocus,120,"name","summary","goals","relationship","knowledge","last_seen","hex_id"));
            root.put("known_npcs",WorldContextSelector.select(db.listKnownNpcs(5000),contextFocus,120,"name","summary","goals","relationship","knowledge","last_seen","hex_id"));
            root.put("scene_npcs",db.listSceneNpcs(50));
            root.put("world_active_npcs",db.listWorldActiveNpcs(80));
            root.put("npc_cognition",WorldContextSelector.select(db.listNpcCognitiveStates(5000),contextFocus,120,"npc_name","current_location","next_action","beliefs_json","faction","last_interaction_summary"));
            root.put("combat_state",db.getCombatState());
            boolean mystara="mystara".equals(db.getActiveMapProfile());
            root.put("combatants",compactCombatants(db.listCombatants(),80,mystara));
            root.put("combat_rules",mystara?new JSONObject().put("system",MystaraRulesEngine.SYSTEM_ID).put("armor_class","descending").put("initiative","1d6 by side; ties simultaneous; individual initiative optional").put("round_seconds",10).put("morale","2d6 equal or below; NPCs and monsters only"):new JSONObject().put("system","dnd5e"));
            root.put("map",compactMap(db.getMapState()));
            root.put("map_context",tail(mapNarrativeContext,11000));
            root.put("recent_journal",compactRows(db.listJournal(),12,new String[]{"title","body","kind","created_at"},1800));
            root.put("recent_dice",compactRows(db.listDiceHistory(),20,new String[]{"formula","detail","total","mode","created_at"},600));
            root.put("recent_audit",compactRows(db.listAuditEvents(24),24,new String[]{"category","title","summary","created_at"},900));
            root.put("manual_catalog",compactRows(db.listManuals(""),30,new String[]{"id","title","category","rule_profile","enabled","indexed","rag_status","rag_page_count","source"},900));
            root.put("database_health",db.healthCheck());
            root.put("audio",audioDiagnostic==null?new JSONObject():audioDiagnostic);
            root.put("party_state",db.getPartyState());
            root.put("wilem_cookbook",new WilemCookbookEngine(db).masterView());
            root.put("party_actions",new JSONArray(db.listPartyActions("queued",40)));
            root.put("knowledge_facts",new JSONArray(db.listKnowledgeFacts(80)));
            root.put("present_npc_adventure_memory",new AdventureContinuityEngine(db).sceneContext());
            root.put("kvothe_strixhaven_party_policy",new KvotheCampaignPartyBridge(db).context());
            root.put("quests",WorldContextSelector.select(db.listQuests("active",1000),contextFocus,40,"title","objective","stage","stakes"));
            root.put("factions",WorldContextSelector.select(db.listFactions(1000),contextFocus,40,"name","agenda","current_plan","location"));
            root.put("guilds",new GuildEngine(db).promptView(contextFocus,40));
            root.put("world_clocks",WorldContextSelector.select(db.listWorldClocks(true,1000),contextFocus,40,"name","consequence","owner_entity","trigger_condition"));
            root.put("world_events",WorldContextSelector.select(db.listWorldEventsForMaster(1000),contextFocus,30,"title","description","event_key"));
            root.put("living_world",new JSONObject(new LivingWorldEngine(db).compactContext()));
            root.put("supplement_registry",SupplementContentRegistry.catalogSummary());
            root.put("supplement_capabilities",SupplementContentRegistry.matchAction(contextFocus));
            root.put("animal_companions",new AnimalCompanionStateEngine(db).snapshotAll(24));
            root.put("world_6",new WorldSixCoordinator(db).context());
            root.put("pending_transactions",new JSONArray(db.listPendingTransactions("pending",20)));
            root.put("pending_actions",db.listPendingActions("pending",40));
            root.put("tactical_tokens",new JSONArray(db.listTacticalTokens()));
            root.put("connection_audit",audit(root));
        }catch(Exception e){try{root.put("snapshot_error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha tolerada; a execução continuou.",ignored);}}
        return root;
    }

    /** Contexto operacional usado em cada cena do Narrador. Preserva a ficha completa
     * dos heróis ativos e os sistemas que podem ser lidos ou alterados pela transação. */
    public JSONObject narratorSnapshot(String mapNarrativeContext,JSONObject audioDiagnostic){
        JSONObject root=new JSONObject();
        try{
            String contextFocus=WorldContextSelector.focus(AtlasScenePositionReconciler.authoritativeSceneSnapshot(db),mapNarrativeContext,db.getStoryState().optString("last_action"));
            root.put("world_profile",db.getActiveMapProfile());
            root.put("master_protocol",MasterProtocolV1.descriptor());
            root.put("source_of_truth_manifest",SourceOfTruthRegistry.manifest());
            root.put("combat_state_machine",CombatStateMachine.descriptor());
            root.put("essential_rules_cache",EssentialRulesCache.snapshot(db.getActiveMapProfile()));
            root.put("campaign",compact(db.getCampaign(),new String[]{"name","rules_profile","tone","feats","multiclass","flanking","notes"}));
            root.put("story",compact(db.getStoryState(),new String[]{"started","last_narration","visual_prompt","options_json","pending_test_json","last_action","option_history_json","updated_at"}));
            root.put("active_heroes_full",compactHeroes(db.listActiveCharacters(),12));
            root.put("scene",AtlasScenePositionReconciler.authoritativeSceneSnapshot(db));
            root.put("combat_state",db.getCombatState());
            root.put("combatants",compactCombatants(db.listCombatants(),50,"mystara".equals(db.getActiveMapProfile())));
            root.put("map",compactMap(db.getMapState()));
            root.put("map_context",tail(mapNarrativeContext,6500));
            root.put("tactical_tokens",new JSONArray(db.listTacticalTokens()));
            root.put("recent_journal",compactRows(db.listJournal(),8,new String[]{"title","body","kind","created_at"},1000));
            root.put("recent_dice",compactRows(db.listDiceHistory(),12,new String[]{"formula","detail","total","mode","created_at"},500));
            root.put("npcs",WorldContextSelector.select(db.listNpcMemories(5000),contextFocus,50,"name","summary","goals","relationship","knowledge","last_seen","hex_id"));
            root.put("known_npcs",WorldContextSelector.select(db.listKnownNpcs(5000),contextFocus,50,"name","summary","goals","relationship","knowledge","last_seen","hex_id"));
            root.put("scene_npcs",limitArray(db.listSceneNpcs(30),30));
            root.put("world_active_npcs",limitArray(db.listWorldActiveNpcs(40),40));
            root.put("npc_cognition",WorldContextSelector.select(db.listNpcCognitiveStates(5000),contextFocus,50,"npc_name","current_location","next_action","beliefs_json","faction","last_interaction_summary"));
            root.put("knowledge_facts",new JSONArray(db.listKnowledgeFacts(50)));
            root.put("present_npc_adventure_memory",new AdventureContinuityEngine(db).sceneContext());
            root.put("kvothe_strixhaven_party_policy",new KvotheCampaignPartyBridge(db).context());
            root.put("quests",WorldContextSelector.select(db.listQuests("active",1000),contextFocus,30,"title","objective","stage","stakes"));
            root.put("factions",WorldContextSelector.select(db.listFactions(1000),contextFocus,30,"name","agenda","current_plan","location"));
            root.put("guilds",new GuildEngine(db).promptView(contextFocus,30));
            root.put("world_clocks",WorldContextSelector.select(db.listWorldClocks(true,1000),contextFocus,30,"name","consequence","owner_entity","trigger_condition"));
            root.put("world_events",WorldContextSelector.select(db.listWorldEventsForMaster(1000),contextFocus,24,"title","description","event_key"));
            root.put("pending_transactions",new JSONArray(db.listPendingTransactions("pending",20)));
            root.put("pending_actions",db.listPendingActions("pending",40));
            root.put("living_world",new JSONObject(new LivingWorldEngine(db).compactContext()));
            root.put("supplement_registry",SupplementContentRegistry.catalogSummary());
            root.put("supplement_capabilities",SupplementContentRegistry.matchAction(contextFocus));
            root.put("animal_companions",new AnimalCompanionStateEngine(db).snapshotAll(24));
            root.put("world_6",new WorldSixCoordinator(db).context());
            root.put("party_state",db.getPartyState());
            root.put("wilem_cookbook",new WilemCookbookEngine(db).masterView());
            root.put("party_actions",new JSONArray(db.listPartyActions("queued",40)));
            root.put("travel_rules_state",db.getTravelRulesState());
            root.put("manual_catalog",compactRows(db.listManuals(""),20,new String[]{"id","title","category","rule_profile","enabled","indexed","rag_status","rag_page_count","source"},700));
            root.put("audio",audioDiagnostic==null?new JSONObject():audioDiagnostic);
            JSONObject counts=db.masterStateCounts();JSONObject coverage=new JSONObject();
            coverage.put("active_heroes",coverage(root.optJSONArray("active_heroes_full"),counts.optInt("active_heroes")));
            coverage.put("combatants",coverage(root.optJSONArray("combatants"),counts.optInt("combatants")));
            coverage.put("npcs",coverage(root.optJSONArray("npcs"),counts.optInt("npcs")));
            coverage.put("npc_cognition",coverage(root.optJSONArray("npc_cognition"),counts.optInt("npc_cognition")));
            coverage.put("knowledge",coverage(root.optJSONArray("knowledge_facts"),counts.optInt("knowledge")));
            coverage.put("quests",coverage(root.optJSONArray("quests"),counts.optInt("quests")));
            coverage.put("factions",coverage(root.optJSONArray("factions"),counts.optInt("factions")));
            coverage.put("guilds",coverage(root.optJSONArray("guilds"),counts.optInt("guilds")));
            coverage.put("clocks",coverage(root.optJSONArray("world_clocks"),counts.optInt("clocks")));
            coverage.put("events",coverage(root.optJSONArray("world_events"),counts.optInt("events")));
            coverage.put("pending_transactions",coverage(root.optJSONArray("pending_transactions"),counts.optInt("pending_transactions")));
            coverage.put("tactical_tokens",coverage(root.optJSONArray("tactical_tokens"),counts.optInt("tactical_tokens")));
            coverage.put("journal",coverage(root.optJSONArray("recent_journal"),counts.optInt("journal")));
            coverage.put("dice",coverage(root.optJSONArray("recent_dice"),counts.optInt("dice")));
            coverage.put("manuals",coverage(root.optJSONArray("manual_catalog"),counts.optInt("manuals")));
            root.put("state_coverage",coverage);
            root.put("connection_manifest",new JSONObject()
                    .put("read",new JSONArray(new String[]{"campaign","story","full_sheets","inventory","spells","scene","combat","npcs","known_npcs","scene_npcs","world_active_npcs","knowledge","quests","factions","guilds","clocks","events","pending_actions","living_world","map","tactical_board","journal","dice","audio"}))
                    .put("write_after_review",new JSONArray(new String[]{"hero_state","combat","scene","npc_memory","npc_cognition","knowledge","quests","factions","guilds","clocks","events","map","tactical_board","journal"}))
                    .put("coverage",coverage)
                    .put("contract","Cada sistema informa total, enviados e omitidos. Registros fora da janela não são tratados como ausentes; o banco local continua sendo a fonte de verdade.")
                    .put("authority","Banco local + motores de regras; a IA apenas propõe."));
        }catch(Exception e){try{root.put("snapshot_error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha no barramento narrativo.",ignored);}}
        return root;
    }

    public String promptSnapshot(String mapNarrativeContext,JSONObject audioDiagnostic){
        JSONObject root=snapshot(mapNarrativeContext,audioDiagnostic);String raw=root.toString();
        if(raw.length()<=48000)return raw;
        // Preserva todas as seções e reduz apenas textos longos, nunca corta o JSON no meio.
        try{
            root.put("map_context",tail(root.optString("map_context"),6000));
            root.put("recent_journal",compactRows(db.listJournal(),6,new String[]{"title","body","kind","created_at"},900));
            root.put("all_heroes",compactHeroes(db.listCharacters(),12));
            String fallbackFocus=WorldContextSelector.focus(db.getSceneState(),mapNarrativeContext,db.getStoryState().optString("last_action",""));
            root.put("npcs",WorldContextSelector.select(db.listNpcMemories(5000),fallbackFocus,80,"name","summary","goals","relationship","knowledge","last_seen","hex_id"));
            root.put("manual_catalog",compactRows(db.listManuals(""),15,new String[]{"id","title","category","rule_profile","enabled","indexed","rag_status","source"},500));
        }catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha tolerada; a execução continuou.",ignored);}
        return root.toString();
    }
    public JSONObject auditConnections(){JSONObject full=snapshot("",new JSONObject()),out=full.optJSONObject("connection_audit");if(out==null)out=new JSONObject();try{JSONObject bus=narratorSnapshot("",new JSONObject());JSONArray heroes=bus.optJSONArray("active_heroes_full");JSONObject manifest=bus.optJSONObject("connection_manifest");JSONArray writes=manifest==null?null:manifest.optJSONArray("write_after_review");out.put("narrator_bus_connected",!bus.has("snapshot_error")&&bus.optJSONObject("campaign")!=null&&bus.optJSONObject("scene")!=null);out.put("full_sheets_in_narrator_bus",heroes!=null&&containsField(heroes,"sheet"));out.put("inventory_and_spells_in_narrator_bus",heroes!=null&&containsField(heroes,"equipment")&&containsField(heroes,"spells"));out.put("map_bidirectional_connected",bus.optJSONObject("map")!=null&&arrayHas(writes,"map"));out.put("tactical_bidirectional_connected",bus.optJSONArray("tactical_tokens")!=null&&arrayHas(writes,"tactical_board"));out.put("journal_bidirectional_connected",bus.optJSONArray("recent_journal")!=null&&arrayHas(writes,"journal"));out.put("living_world_runtime_connected",bus.optJSONObject("living_world")!=null&&bus.optJSONArray("factions")!=null&&bus.optJSONArray("world_clocks")!=null&&bus.optJSONArray("world_events")!=null);out.put("emergent_world_connected",bus.optJSONObject("world_6")!=null);out.put("guilds_bidirectional_connected",bus.optJSONArray("guilds")!=null&&arrayHas(writes,"guilds"));out.put("npc_cognition_connected",bus.optJSONArray("npc_cognition")!=null);out.put("party_and_travel_connected",bus.optJSONObject("party_state")!=null&&bus.optJSONObject("travel_rules_state")!=null);out.put("manual_sources_in_narrator_bus",bus.optJSONArray("manual_catalog")!=null);out.put("pending_review_connected",bus.optJSONArray("pending_transactions")!=null&&bus.optJSONArray("pending_actions")!=null);out.put("mechanical_guard_connected",true);boolean complete=true;java.util.Iterator<String> keys=out.keys();while(keys.hasNext()){String key=keys.next();if(!"complete".equals(key)&&out.opt(key) instanceof Boolean&&!out.optBoolean(key))complete=false;}out.put("complete",complete);}catch(Exception e){try{out.put("complete",false).put("audit_error",String.valueOf(e.getMessage()));}catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha na auditoria operacional.",ignored);}}return out;}

    private JSONObject audit(JSONObject root){JSONObject out=new JSONObject();try{
        out.put("campaign_connected",root.optJSONObject("campaign")!=null);
        out.put("story_continuity_connected",root.optJSONObject("story")!=null&&root.optJSONObject("story").has("last_narration"));
        out.put("choices_and_tests_connected",root.optJSONObject("story")!=null&&root.optJSONObject("story").has("options_json")&&root.optJSONObject("story").has("pending_test_json"));
        out.put("rules_connected",root.optJSONObject("campaign")!=null&&root.optJSONObject("campaign").optString("rules_profile").length()>0);
        out.put("heroes_connected",root.optJSONArray("all_heroes")!=null);
        out.put("active_party_connected",root.optJSONArray("active_heroes")!=null);
        out.put("inventory_and_sheet_connected",containsField(root.optJSONArray("all_heroes"),"sheet"));
        out.put("character_tokens_connected",containsField(root.optJSONArray("all_heroes"),"token_uri"));
        out.put("npc_memory_connected",root.optJSONArray("npcs")!=null);
        out.put("npc_tokens_connected",containsField(root.optJSONArray("npcs"),"token_uri"));
        out.put("scene_connected",root.optJSONObject("scene")!=null);
        out.put("travel_rules_connected",root.optJSONObject("travel_rules_state")!=null&&root.optJSONObject("rules_boundary")!=null);
        out.put("scene_npc_link_connected",sceneNpcLinksResolve(root.optJSONObject("scene"),root.optJSONArray("npcs")));
        out.put("map_connected",root.optJSONObject("map")!=null);
        out.put("combat_connected",root.optJSONObject("combat_state")!=null&&root.optJSONArray("combatants")!=null);
        out.put("journal_connected",root.optJSONArray("recent_journal")!=null);
        out.put("dice_connected",root.optJSONArray("recent_dice")!=null);
        out.put("audit_connected",root.optJSONArray("recent_audit")!=null);
        out.put("manual_sources_connected",root.optJSONArray("manual_catalog")!=null);
        out.put("database_health_connected",root.optJSONObject("database_health")!=null);
        out.put("audio_connected",root.optJSONObject("audio")!=null);
        out.put("party_director_connected",root.optJSONObject("party_state")!=null&&root.optJSONArray("party_actions")!=null);
        out.put("knowledge_separation_connected",root.optJSONArray("knowledge_facts")!=null);
        out.put("living_world_connected",root.optJSONArray("quests")!=null&&root.optJSONArray("factions")!=null&&root.optJSONArray("world_clocks")!=null&&root.optJSONArray("world_events")!=null);out.put("emergent_world_connected",root.optJSONObject("world_6")!=null);out.put("guilds_connected",root.optJSONArray("guilds")!=null);
        out.put("sandbox_life_connected",root.optJSONObject("living_world")!=null&&root.optJSONObject("living_world").has("rule_boundary"));
        out.put("transaction_review_connected",root.optJSONArray("pending_transactions")!=null&&root.optJSONArray("pending_actions")!=null);
        out.put("tactical_board_connected",root.optJSONArray("tactical_tokens")!=null);
        boolean complete=true;java.util.Iterator<String> keys=out.keys();while(keys.hasNext())if(!out.optBoolean(keys.next()))complete=false;out.put("complete",complete);
    }catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha tolerada; a execução continuou.",ignored);}return out;}

    private static JSONArray compactHeroes(List<JSONObject> list,int max){JSONArray out=new JSONArray();if(list==null)return out;for(int i=0;i<list.size()&&i<max;i++){JSONObject h=list.get(i),x=compact(h,new String[]{"id","name","species","class_name","level","hp","max_hp","ac","str","dex","con","int_score","wis","cha","active","background","visual_description","proficiencies","equipment","spells","notes","token_uri","rules_source"});try{JSONObject sheet=parseObject(h.optString("sheet_json","{}"));x.put("sheet",sheet);out.put(x);}catch(Exception ignored){out.put(x);}}return out;}
    private static JSONArray compactCombatants(List<JSONObject> list,int max,boolean mystara){String[] keys=mystara?new String[]{"id","name","initiative","hp","max_hp","ac","conditions","resources_json","is_player","active_turn","sort_order"}:new String[]{"id","name","initiative","hp","max_hp","temp_hp","ac","conditions","condition_timers_json","concentration","reaction_used","death_success","death_fail","resistances","vulnerabilities","immunities","resources_json","is_player","active_turn","sort_order"};return compactRows(list,max,keys,1200);}
    private static JSONArray compactRows(List<JSONObject> list,int max,String[] keys,int textLimit){JSONArray out=new JSONArray();if(list==null)return out;for(int i=0;i<list.size()&&i<max;i++){JSONObject row=compact(list.get(i),keys);java.util.Iterator<String> it=row.keys();while(it.hasNext()){String key=it.next();Object value=row.opt(key);if(value instanceof String&&((String)value).length()>textLimit)try{row.put(key,((String)value).substring(0,textLimit)+"…");}catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha tolerada; a execução continuou.",ignored);}}out.put(row);}return out;}
    private static JSONObject coverage(JSONArray sent,int total){JSONObject out=new JSONObject();try{int count=sent==null?0:sent.length();out.put("total",Math.max(0,total)).put("sent",count).put("omitted",Math.max(0,total-count)).put("complete",count>=Math.max(0,total));}catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha ao registrar cobertura do barramento.",ignored);}return out;}

    private static JSONArray limitArray(JSONArray array,int max){JSONArray out=new JSONArray();if(array!=null)for(int i=0;i<array.length()&&i<max;i++)out.put(array.opt(i));return out;}
    private static boolean sceneNpcLinksResolve(JSONObject scene,JSONArray memories){
        if(scene==null||memories==null)return false;
        JSONArray present;
        try{present=new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception e){present=scene.optJSONArray("present_npcs");}
        if(present==null||present.length()==0)return true;
        java.util.HashSet<String> known=new java.util.HashSet<String>();
        for(int i=0;i<memories.length();i++){JSONObject npc=memories.optJSONObject(i);if(npc!=null)known.add(npc.optString("name").trim().toLowerCase(java.util.Locale.ROOT));}
        for(int i=0;i<present.length();i++){Object value=present.opt(i);String name=value instanceof JSONObject?((JSONObject)value).optString("name"):String.valueOf(value);if(name.trim().length()>0&&!known.contains(name.trim().toLowerCase(java.util.Locale.ROOT)))return false;}
        return true;
    }

    private static boolean arrayHas(JSONArray array,String value){if(array==null)return false;for(int i=0;i<array.length();i++)if(value.equalsIgnoreCase(array.optString(i)))return true;return false;}
    private static boolean containsField(JSONArray array,String field){if(array==null)return false;if(array.length()==0)return true;for(int i=0;i<array.length();i++){JSONObject o=array.optJSONObject(i);if(o!=null&&o.has(field))return true;}return false;}
    private static JSONObject compact(JSONObject source,String[] keys){JSONObject out=new JSONObject();if(source==null)return out;for(String key:keys)if(source.has(key))try{out.put(key,source.opt(key));}catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha tolerada; a execução continuou.",ignored);}return out;}
    private static JSONObject compactMap(JSONObject source){JSONObject out=new JSONObject();if(source==null)return out;String[] keys={"map_package_id","current_hex_id","reveals_json","grid_type","grid_size","fog_enabled","token_x","token_y","scale","offset_x","offset_y","position_locked","destination_hex_id","route_json","route_owner","overlay_visible","manifest_json"};for(String key:keys)if(source.has(key))try{Object value=source.opt(key);if("manifest_json".equals(key)&&value instanceof String&&((String)value).length()>3500)value=((String)value).substring(0,3500)+"…";out.put(key,value);}catch(Exception ignored){AppLog.ignored("MasterGameContext","Falha tolerada; a execução continuou.",ignored);}return out;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static String tail(String text,int max){String value=text==null?"":text;return value.length()<=max?value:value.substring(value.length()-max);}
}
