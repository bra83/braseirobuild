package com.braseiro.dnd5e;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/**
 * Núcleo cognitivo do Mestre virtual. A IA interpreta e narra; o aplicativo
 * planeja, filtra conhecimento, valida regras, mantém memória e só confirma
 * mudanças após autorização do jogador.
 */
public final class MasterOrchestrator {
    public static final class Plan {
        public final String userPrompt;
        public final String compactContext;
        public final JSONObject localTick;
        public final JSONObject cognitivePlan;
        public final JSONObject npcTick;
        public final JSONObject environmentalPlan;
        public final JSONObject ruleRetrieval;
        public final JSONObject groundingEnvelope;
        public final boolean usedPartyQueue;
        public final JSONArray queuedActionIds;
        public final boolean economy;
        public final boolean narrative;
        public final boolean nonAdvancing;
        public final String narrativeIntent;
        public final String actionHash;
        public final String stateHash;
        public final String cacheKey;
        public final boolean contextReady;
        public final String contextError;
        public final String barbaraStoryOccasion;
        public final String barbaraLocation;
        public final String barbaraImprint;
        public final String inputChannel;
        public final JSONObject discoveryDecision;
        private boolean accepted;
        private boolean committed;
        private boolean fromCache;
        Plan(String prompt,String context,JSONObject tick,JSONObject cognition,JSONObject npcPreview,JSONObject environmentPreview,JSONObject retrieval,JSONObject grounding,boolean queued,boolean eco,boolean narr,boolean frozen,String intent,String aHash,String sHash,String key,boolean ready,String error,String bOccasion,String bLocation,String bImprint,String channel,JSONObject discovery,JSONArray actionIds){queuedActionIds=actionIds==null?new JSONArray():actionIds;userPrompt=prompt;compactContext=context;localTick=tick;cognitivePlan=cognition;npcTick=npcPreview;environmentalPlan=environmentPreview;ruleRetrieval=retrieval==null?new JSONObject():retrieval;groundingEnvelope=grounding==null?new JSONObject():grounding;usedPartyQueue=queued;economy=eco;narrative=narr;nonAdvancing=frozen;narrativeIntent=intent==null?"":intent;actionHash=aHash;stateHash=sHash;cacheKey=key;contextReady=ready;contextError=error==null?"":error;barbaraStoryOccasion=bOccasion==null?"continuation":bOccasion;barbaraLocation=bLocation==null?"":bLocation;barbaraImprint=bImprint==null?"":bImprint;inputChannel=channel==null?"":channel;discoveryDecision=discovery==null?new JSONObject():discovery;}
        public JSONObject getGroundingEnvelope(){return groundingEnvelope;}
    }

    private final AppDb db;
    private final PartyDirector party;
    private final WorldStateEngine world;
    private final AiQuotaGovernor quota;
    private final KnowledgeFirewall firewall;
    private final MasterRulesArbiter rules;
    private final NpcAgencyEngine npcAgency;
    private final GuildEngine guilds;
    private final MasterMemoryEngine memory;
    private final MasterCognitiveEngine cognition;
    private final MasterResponseValidator validator;
    private final EnvironmentalDirector environmentalDirector;
    private final MasterGameContext gameContext;
    private final BarbaraSupervisor barbara;
    private final NarrativeDiscoveryDirector discoveryDirector;
    private final StrixhavenAdventureProfile strixhavenProfile;
    private final com.braseiro.dnd5e.adventure.AdventureEngine adventureEngine;
    private final NarrativeContentEngine narrativeEngine;

    public MasterOrchestrator(AppDb database,PartyDirector partyDirector,WorldStateEngine worldEngine,AiQuotaGovernor governor){
        this(database,partyDirector,worldEngine,governor,null);
    }
    public MasterOrchestrator(AppDb database,PartyDirector partyDirector,WorldStateEngine worldEngine,AiQuotaGovernor governor,Context context){
        db=database;party=partyDirector;world=worldEngine;quota=governor;firewall=new KnowledgeFirewall(db);rules=new MasterRulesArbiter(db);npcAgency=new NpcAgencyEngine(db,context==null?null:new NpcPersonalityCatalog(context));guilds=new GuildEngine(db);memory=new MasterMemoryEngine(db);cognition=new MasterCognitiveEngine(db,party,rules);validator=new MasterResponseValidator(db,firewall,rules,npcAgency);environmentalDirector=new EnvironmentalDirector(db);gameContext=new MasterGameContext(db);discoveryDirector=new NarrativeDiscoveryDirector(db);strixhavenProfile=context==null?null:new StrixhavenAdventureProfile(context);barbara=context==null?null:new BarbaraSupervisor(context);startupMark("MASTER_CORE_CREATED");narrativeEngine=new NarrativeContentEngine(db);try{narrativeEngine.seedOfficialCanon();}catch(Exception ignored){}startupMark("MASTER_CANON_SEEDED");adventureEngine=new com.braseiro.dnd5e.adventure.AdventureEngine(db,this,world);try{for(com.braseiro.dnd5e.adventure.AdventureModule module:com.braseiro.dnd5e.adventure.FaerunAdventureModuleCatalog.all())adventureEngine.installModule(module);for(com.braseiro.dnd5e.adventure.AdventureModule module:com.braseiro.dnd5e.adventure.RavenloftAdventureModuleCatalog.all())adventureEngine.installModule(module);for(com.braseiro.dnd5e.adventure.AdventureModule module:com.braseiro.dnd5e.adventure.RavenloftShortAdventureCatalog.all())adventureEngine.installModule(module);for(com.braseiro.dnd5e.adventure.AdventureModule module:com.braseiro.dnd5e.adventure.LorwynAdventureModuleCatalog.all())adventureEngine.installModule(module);for(com.braseiro.dnd5e.adventure.AdventureModule module:com.braseiro.dnd5e.adventure.EberronAdventureModuleCatalog.all())adventureEngine.installModule(module);for(com.braseiro.dnd5e.adventure.AdventureModule module:com.braseiro.dnd5e.adventure.NetherilAdventureModuleCatalog.all())adventureEngine.installModule(module);}catch(Exception ignored){}startupMark("MASTER_ADVENTURES_REGISTERED");try{TashaCompiledSourceRulesEngine.install();TashaSpellRuntimeEngine.install();TashaArtificerRuntimeEngine.install();TashaSubclassRuntimeEngine.install();CrossBookSubclassRuntimeEngine.install();Core2024EberronCharacterEngine.install();Core2024ClassRuntimeEngine.install();HomebrewClassRuntimeEngine.install();HomebrewClassRuntimeEngine.installRun138();HomebrewSubclassRuntimeEngine.install();VoloSubraceHandbookEngine.install();VoloSubraceRun139Engine.install();VoloSubraceRun140Engine.install();VoloRacialFeatRuntimeEngine.install();Run141SpeciesFeatRuntimeEngine.install();Run141CompactCharacterOptionEngine.install();Run143ForgottenSecretsEngine.install();Run149ForgottenSecretsSpellEngine.install();EberronForgeCompleteEngine.install();ForgottenRealmsHeroesEngine.install();RuneterraClassRuntimeEngine.install();ArcanaUnleashedRun143Engine.install();ArcanaUnleashedRun144Engine.install();ArcanaUnleashedRun145ItemEngine.install();ArcanaUnleashedRun145BestiaryEngine.install();RavenloftBestiaryRuntimeEngine.install();LorwynBestiaryRuntimeEngine.install();ArcanaUnleashedRun147EvolvingItemsEngine.install();TashaItemRuntimeEngine.install();TashaArtifactRuntimeEngine.install();TashaSupplementalRuntimeEngine.install();}catch(Exception ignored){}startupMark("MASTER_RULES_REGISTERED");try{TashaFeatCompiledRulesEngine.install();}catch(Exception ignored){}try{TashaMimicColonyEngine.installStatblocks();}catch(Exception ignored){}try{installSrd51BestiaryBatch();}catch(Exception ignored){}startupMark("MASTER_MONSTERS_REGISTERED");
    }

    /** Start Chaquopy off-main after first UI frame. Mechanics remain installed. */
    public void warmBarbaraAsync(){if(barbara!=null)barbara.warmUpAsync();}

    private static void startupMark(String checkpoint){
        try{DndStartupTrace.mark(checkpoint);}catch(Throwable ignored){/* JVM unit tests lack Android clock */}
    }

    public Plan prepare(String mode,String rawAction,String mapContext,JSONObject audio,JSONObject ruleRetrieval){return prepare(mode,rawAction,mapContext,audio,ruleRetrieval,null);}
    public Plan prepare(String mode,String rawAction,String mapContext,JSONObject audio,JSONObject ruleRetrieval,String inputChannel){
        // Antes de qualquer cognição/tempo/NPC, elimina drift legado entre cena e Atlas.
        // Assim nenhum subsistema recebe uma cidade antiga associada ao hex atual.
        final boolean strixhavenActive=strixhavenProfile!=null&&strixhavenProfile.isActive();
        if(!strixhavenActive)try{AtlasScenePositionReconciler.reconcilePersistedScene(db,"preflight do Mestre");}catch(Exception failure){AppLog.e("MasterOrchestrator","Falha ao reconciliar Atlas antes do planejamento.",failure);}
        final String effectiveMapContext=strixhavenActive?strixhavenProfile.narrativeMapContext():mapContext;
        boolean narrative="Narrador".equals(mode);boolean explicitAction="PLAYER_ACTION".equals(inputChannel);boolean explicitHelp="GM_HELP".equals(inputChannel);BraseiroNarrativeDirector.Intent routedIntent=BraseiroNarrativeDirector.classify(rawAction);boolean nonAdvancing=narrative&&(explicitHelp||(!explicitAction&&BraseiroNarrativeDirector.isNonAdvancing(routedIntent)));boolean queued=narrative&&!nonAdvancing&&party.hasQueuedActions();JSONArray frozenActionIds=queued?party.queuedActionIds():new JSONArray();String action=queued?party.buildActionBatch(rawAction):(rawAction==null?"":rawAction.trim());JSONObject tick=narrative&&!nonAdvancing?world.preview(action):new JSONObject();
        JSONObject effectiveRuleRetrieval=mergeRuleRetrieval(ruleRetrieval,worldRuleRetrieval(tick));
        rules.setActiveRuleRetrieval(effectiveRuleRetrieval);
        JSONObject environmental=narrative&&!nonAdvancing?environmentalDirector.preview(action):new JSONObject();JSONObject environmentalPrompt=narrative&&!nonAdvancing?environmentalDirector.promptView(environmental):new JSONObject();try{if(narrative&&!nonAdvancing)tick.put("environmental_director",environmentalPrompt);}catch(Exception failure){AppLog.e("MasterOrchestrator","Falha ao anexar o plano ambiental.",failure);}JSONObject cognitive=narrative&&!nonAdvancing?cognition.plan(action,tick):new JSONObject();JSONObject npcTick=narrative&&!nonAdvancing?npcAgency.previewOffscreenTick(tick):new JSONObject();JSONObject root=new JSONObject();boolean contextReady=true;String contextError="";String barbaraLocation="",barbaraImprint="",barbaraOccasion="continuation";JSONObject discoveryDecision=new JSONObject();JSONObject groundingEnvelope=new JSONObject();
        try{
            root.put("campaign",compact(db.getCampaign(),new String[]{"name","rules_profile","tone","feats","multiclass","flanking","notes"}));
            JSONObject bus=gameContext.narratorSnapshot(effectiveMapContext,audio);if(strixhavenActive)bus=strixhavenProfile.sanitizeNarratorBus(bus);bus.put("rule_retrieval",compactRuleRetrieval(effectiveRuleRetrieval));JSONObject supplementCapabilities=SupplementContentRegistry.matchAction(action);bus.put("supplement_capabilities",supplementCapabilities);root.put("game_bus",bus).put("human_master_profile",HumanMasterNarrativeProfile.contract());
            root.put("rule_retrieval",compactRuleRetrieval(effectiveRuleRetrieval));root.put("supplement_capabilities",supplementCapabilities);
            JSONObject storyState=db.getStoryState();JSONObject story=compact(storyState,new String[]{"pending_test_json","last_action","updated_at"});story.put("last_narration",tail(storyState.optString("last_narration"),quota.economyMode()?850:1400));story.put("avoid_options",firstJson(parseArray(storyState.optString("options_json","[]")),4));root.put("story",story);
            if(narrative){root.put("narrative_intent",explicitHelp?"ajuda do Mestre":(explicitAction?"ação do jogador":BraseiroNarrativeDirector.intentLabel(routedIntent)));root.put("input_channel",inputChannel==null?"":inputChannel);root.put("fiction_frozen",nonAdvancing);}
            JSONObject sourceScene=AtlasScenePositionReconciler.authoritativeSceneSnapshot(db);if(strixhavenActive)sourceScene=strixhavenProfile.sceneProjection(sourceScene);JSONObject compactSceneState=compactScene(sourceScene);JSONObject compactPartyState=party.compactStateForPrompt(quota.economyMode()?3300:4400);JSONObject compactWorldState=compactWorld(action+" "+effectiveMapContext);
            JSONObject liveMemory=new JSONObject();JSONObject partyBlock=compactPartyState.optJSONObject("party");JSONObject sessionPrefs=partyBlock==null?null:partyBlock.optJSONObject("session_preferences");JSONObject savedNarrativeMemory=sessionPrefs==null?null:sessionPrefs.optJSONObject("braseiro_narrative_memory");if(savedNarrativeMemory!=null)liveMemory=savedNarrativeMemory;
            root.put("scene",compactSceneState);root.put("party",compactPartyState);root.put("control_contract",party.controlContract());root.put("cognitive_plan",cognitive);root.put("rules_frame",rules.rulesFrame(action));JSONObject knowledgeView=firewall.promptView(action);root.put("knowledge",knowledgeView);root.put("npcs",npcAgency.promptView(action,quota.economyMode()?5:8));root.put("guilds",guilds.promptView(action+" "+effectiveMapContext,quota.economyMode()?6:12));root.put("relevant_memories",memory.relevant(action+" "+effectiveMapContext,quota.economyMode()?3:6));root.put("world",compactWorldState);root.put("live_table",LiveTableNarrativeDirector.promptState(root.optJSONObject("campaign"),compactSceneState,compactPartyState,compactWorldState,liveMemory,action));root.put("living_world",new JSONObject(new LivingWorldEngine(db).compactContext()));root.put("combat",db.getCombatState());root.put("combatants",first(db.listCombatants(),quota.economyMode()?8:14));root.put("map",strixhavenActive?strixhavenProfile.mapProjection():compact(db.getMapState(),new String[]{"logical_map_id","map_package_id","coordinate_frame_id","current_hex_id","position_u","position_v","position_migration_status","destination_hex_id","route_json","fog_enabled"}));JSONObject atlasPublic=strixhavenActive?strixhavenProfile.atlasProjection():AtlasContextProjector.publicProjection(db);root.put("atlas_public",atlasPublic);root.put("map_context",tail(effectiveMapContext,quota.economyMode()?900:1700));if(strixhavenActive)root.put("adventure_profile",strixhavenProfile.descriptor()).put("adventure_profile_instruction",strixhavenProfile.masterInstruction());JSONObject academicHistory=strixhavenProfile.academicHistory();if(academicHistory.length()>0){root.put("academic_history",academicHistory);bus.put("academic_history",academicHistory);}root.put("local_tick",tick);root.put("environmental_director",environmentalPrompt);root.put("npc_offscreen_preview",npcTick);root.put("spotlight_hint",party.nextSpotlightHint());root.put("authority","O aplicativo é a autoridade mecânica e persistente. O Gemini não aplica mudanças diretamente.");
            // O áudio permanece compatível, mas não participa do núcleo cognitivo desta versão.
            root.put("audio_compatibility",compactAudio(audio));
            JSONObject bScene=root.optJSONObject("scene");JSONObject bMap=root.optJSONObject("map");
            discoveryDecision=discoveryDirector.evaluate(narrative,nonAdvancing,bScene,bMap,environmentalPrompt);
            JSONObject discoverySignals=discoveryDecision.optJSONObject("signals");
            barbaraLocation=discoverySignals==null?(bScene==null?"":bScene.optString("location","")):discoverySignals.optString("location_label",bScene==null?"":bScene.optString("location",""));
            barbaraImprint=discoverySignals==null?"":discoverySignals.optString("location_imprint","");
            barbaraOccasion=discoveryDecision.optString("occasion","continuation");
            String atlasHex=bMap==null?"":bMap.optString("current_hex_id","").trim();String atlasPackage=bMap==null?"":bMap.optString("map_package_id","").trim();String atlasLogical=bMap==null?"":bMap.optString("logical_map_id","").trim();String atlasFrame=bMap==null?"":bMap.optString("coordinate_frame_id","").trim();String sceneLocation=bScene==null?"":bScene.optString("location","").trim();boolean atlasCanonical=atlasPackage.length()>0&&atlasHex.length()>0;
            JSONObject canonicalPosition=new JSONObject().put("source",atlasCanonical?"atlas":"scene").put("location_id",atlasCanonical?atlasHex:sceneLocation).put("logical_map_id",atlasLogical).put("map_package_id",atlasPackage).put("coordinate_frame_id",atlasFrame).put("hex_id",atlasHex).put("scene_location",sceneLocation);if(bMap!=null&&bMap.has("position_u")&&!bMap.isNull("position_u")&&bMap.has("position_v")&&!bMap.isNull("position_v"))canonicalPosition.put("u",bMap.optDouble("position_u")).put("v",bMap.optDouble("position_v"));root.put("canonical_position",canonicalPosition);
            JSONObject campaignGrounding=new JSONObject().put("campaign",root.optJSONObject("campaign")).put("scene",compactSceneState).put("party",compactPartyState).put("map",root.optJSONObject("map")).put("atlas_public",root.optJSONObject("atlas_public")).put("canonical_position",canonicalPosition).put("living_world",root.optJSONObject("living_world")).put("environmental_director",environmentalPrompt);
            String activeSetting = resolveActiveSetting(db, compactSceneState, canonicalPosition);
            if(narrativeEngine!=null)campaignGrounding.put("narrative_lore",narrativeEngine.promptKnowledgeContext(activeSetting,true));
            JSONObject inferenceGrounding=new JSONObject().put("cognitive_plan",cognitive).put("npc_offscreen_preview",npcTick).put("world_discovery",discoveryDecision);
            groundingEnvelope=GroundingEnvelope.build(compactRuleRetrieval(effectiveRuleRetrieval),knowledgeView,campaignGrounding,inferenceGrounding);
            root.put("grounding_envelope",groundingEnvelope);root.put("barbara_host_contract",BarbaraHostContractV1.descriptor());
            String barbaraSystem="mystara".equalsIgnoreCase(db.getActiveMapProfile())?"mystara":"dnd";
            JSONObject bPlan=barbara==null?new JSONObject().put("story_occasion",barbaraOccasion).put("available",false).put("host_world_authority",true).put("fallback_java_policy",true):barbara.plan(action,"normal",narrative,nonAdvancing,barbaraOccasion,inputChannel,barbaraSystem);
            root.put("world_discovery",discoveryDecision).put("barbara",bPlan).put("barbara_system_contract",barbara==null?new JSONObject().put("available",false):barbara.systemContract(barbaraSystem));
            if(barbara!=null&&barbara.isAvailable()){
                JSONObject bState=new JSONObject();JSONObject bCampaign=root.optJSONObject("campaign");JSONObject bStory=root.optJSONObject("story");
                bState.put("campaign_id",bCampaign==null?("mystara".equals(barbaraSystem)?"braseiro-mystara":"braseiro-dnd"):bCampaign.optString("name","braseiro-dnd"));bState.put("tick",Math.max(0,bStory==null?0:bStory.optInt("turn_index",0))).put("location",canonicalPosition.optString("location_id",barbaraLocation));
                bState.put("facts",new JSONObject().put("last_action",bStory==null?"":bStory.optString("last_action","")).put("last_narration",bStory==null?"":tail(bStory.optString("last_narration",""),1200))).put("world_flags",root.optJSONObject("world")==null?new JSONObject():root.optJSONObject("world"));
                JSONObject atlasForBarbara=root.optJSONObject("atlas_public");JSONObject barbaraNotes=new JSONObject();if(atlasForBarbara!=null)barbaraNotes.put("atlas_public",atlasForBarbara);
                bState.put("npcs",new JSONObject()).put("factions",new JSONObject()).put("rumors",new JSONArray()).put("events",new JSONArray()).put("memory",new JSONArray()).put("clocks",new JSONObject()).put("economy",new JSONObject()).put("weather",environmentalPrompt==null?new JSONObject():environmentalPrompt).put("player_state",compactPartyState).put("scene",bScene==null?new JSONObject():bScene).put("notes",barbaraNotes).put("sites",barbaraSites(atlasForBarbara)).put("public_ledger",new JSONArray()).put("secret_ledger",new JSONArray()).put("discovery",discoveryDecision).put("state_version",0).put("game_time",new JSONObject());
                JSONObject bStateReport=barbara.validateState(bState,barbaraSystem);root.put("barbara_state_validation",bStateReport);if(!bStateReport.optBoolean("valid",false)){contextReady=false;contextError="Motor Barbara rejeitou a projeção CampaignState; a cena foi preservada sem nova geração.";}
            }else root.put("barbara_state_validation",new JSONObject().put("valid",true).put("bypassed",true).put("reason","barbara_unavailable"));
        }catch(Exception failure){contextReady=false;contextError="Falha ao montar o barramento operacional: "+safeError(failure);AppLog.e("MasterOrchestrator",contextError,failure);root=new JSONObject();try{root.put("context_error",true).put("context_error_message",contextError).put("authority","Não gerar narrativa nem mutações: o estado operacional não pôde ser montado.");}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao registrar erro de contexto.",ignored);}}
        String context=fitContext(root,quota.economyMode()?22000:34000);if(context.contains("\"context_error\":true")){contextReady=false;if(contextError.length()==0)contextError="O contexto operacional excedeu o limite seguro ou não pôde ser compactado.";}String actionHash=hash(normalize(action));JSONObject freshStory=db.getStoryState();String storySignature=freshStory.optLong("updated_at",0)+"|"+freshStory.optString("last_action","")+"|"+tail(freshStory.optString("last_narration",""),1800);String stateHash=hash(context+"|story_signature="+hash(storySignature));String cacheKey=hash(mode+"|"+inputChannel+"|"+actionHash+"|"+stateHash);
        String prompt=action+"\n\nUse game_bus como estado operacional completo, live_table como direção de fluxo entre protagonistas/threads e cognitive_plan/environmental_director como planos locais. live_table não autoriza ação, emoção ou conhecimento fora do personagem; apenas organiza foco, cortes, convergência, promessas e pressão causal. supplement_capabilities contém regras de suplementos já compiladas pelo aplicativo e tem prioridade sobre RAG quando matched=true. Para essas regras, use somente rule_ids registrados e mechanical_basis com source_type=compiled_supplement, rule_id, book e evidence exatamente como fornecidos pelo registro; não faça nova busca no PDF para reaprender a mesma mecânica. O campo rule_retrieval foi pesquisado localmente para conteúdo ainda não compilado. Quando não houver pacote compilado aplicável, use somente rule_retrieval.evidence/structured_rules para regras de manual; nunca cite página fora desse pacote. Se não houver regra compilada correspondente e sufficient_for_mechanics=false, continue apenas a ficção e deixe mutações mecânicas vazias. Use living_world somente quando living_world.enabled for verdadeiro; quando falso, não avance agenda, facções, NPCs ou atividades. Envelhecimento, família, relacionamentos, trabalho, caça, estudo e trupe mantêm continuidade narrativa, mas jamais concedem XP, ouro, itens, atributos ou recursos por conta própria. O environmental_director antecipa sinais de clima, terreno, rota e aproximação de locais sem revelar além do perceptível. Quando environmental_director.weather_system trouxer world_consequences, settlement_consequences, trace_consequences, plot_pressure ou aftermath, trate-os como continuidade causal obrigatória da ficção: clima novo precisa ser apresentado quando se torna perceptível; clima ativo muda terreno, circulação, logística e comportamento local; o rescaldo persiste depois que o céu melhora. Nunca faça reset ambiental instantâneo. Nunca invente rio, ponte, enchente ou travessia se hydrology_confirmed=false. plot_pressure pode mudar acesso, timing e legibilidade de pistas, mas NÃO conclui/falha missão, NÃO avança relógio e NÃO muda a verdade canônica de pistas. living_world_pressure só alcança NPCs, facções, atividades e itinerários geograficamente vinculados à área afetada. Respeite rules_boundary: SIMULACAO_NARRATIVA nunca altera ficha; DECISAO_DO_MESTRE nunca é aplicada como fato; REGRA_OPCIONAL exige habilitação; somente RAW confirmado pelo aplicativo produz consequência mecânica. Toda mutação mecânica deve incluir mechanical_basis com source_type e evidence verificável. Citação de manual identifica o procedimento, mas hp_delta, itens, condições e recursos exigem rolagem ou cálculo local. Ao resolver pending_transactions, copie transaction_id e effect_key como resolution_key para event_resolutions ou pending_resolutions, e repita resolution_key na mutação mecânica correspondente. Para manual/regra, use mechanical_basis={source_type:\"manual\",rule_id,evidence,book,page,excerpt,calculation:{operation,roll_id?,roll_formula?,roll_total?,multiplier?,flat_modifier?,modifier_source?,divide_by?,rounding?,fixed_value?,computed_value,resource_deltas?}} e repita exatamente a mesma prova recuperada em sources=[{book,page,excerpt}]. Dados de rolagem precisam existir em game_bus.recent_dice; multiplicadores, modificadores e valores fixos precisam aparecer no procedimento compilado; modifier_source deve apontar para o atributo da ficha e divide_by/rounding só pode ser usado quando a passagem disser metade/terço/divisão. O aplicativo recalculará tudo e rejeitará divergências. Planos autônomos de facção devem reagir ao estado persistente e podem usar plan_steps_json com etapas {title,summary,location,resources,agenda,relationship_delta,next_plan,consequence_json}; consequence_json só pode conter mutações estruturais não mecânicas. Não recalcule regras, relógios, recursos ou ordem causal. Use game_bus.world_6 como camada de direção, nunca como roteiro obrigatório: campaign_director contém tensões emergentes e perguntas dramáticas sem final predeterminado; settlements descreve pressões narrativas locais; history e legacy contêm somente história canônica persistida. Rumores exigem firewall de conhecimento: truth_level, distortion, source_event_key e outros metadados internos são apenas do Mestre; um personagem só pode ouvir ou agir sobre um rumor quando ele estiver explicitamente entre os conhecimentos/rumores daquele personagem ou quando a cena o fizer chegar até ele. Nunca revele automaticamente a verdade objetiva por trás de um rumor. npc_voices é identidade de voz, não autoridade narrativa. tactical_generation fornece geometria visual jogável, mas não cria bônus, cobertura mecânica, dano ou regra sem o motor apropriado. Arcos emergentes podem mudar, desaparecer ou ser resolvidos conforme as ações dos jogadores; jamais force um desfecho para preservar um arco. Narre apenas o que os personagens podem perceber. Proponha mudanças em campos estruturados, que serão revisadas antes de gravar.";
        prompt += "\n\nRespeite control_contract por personagem: manual só age por comando explícito do jogador; companion pode tomar iniciativa motivada dentro da ficha e da informação acessível; curated propõe ações relevantes para aprovação e não as narra como concluídas. Nenhum modo autoriza gasto de recurso limitado sem confirmação.";
        prompt += "\n\nMOTOR BARBARA 1.1.3 É POLÍTICA OBRIGATÓRIA: obedeça o bloco barbara do contexto. input_channel é vinculante: PLAYER_ACTION continua a ficção mesmo contendo pergunta ou fala; GM_HELP congela tempo, posição, NPCs e Mundo Vivo. canonical_position é a única posição operacional; quando source=atlas, jamais narre outro local conflitante com map_package_id/current_hex_id. O Barbara não é um segundo Mestre, não grava estado e não autoriza mecânica; D&D/Mystara no host continuam soberanos.  ALFABETIZAÇÃO DE CENÁRIO É ZERO por padrão: o jogador NÃO leu o livro do mundo e NÃO conhece automaticamente geografia, facções, deuses, povos, guerras, história, instituições ou o estado atual só porque estão no contexto. Na primeira menção relevante de cada nome próprio, ancore naturalmente o que é, onde se encaixa na escala atual e por que importa agora, salvo quando a memória da campanha provar que isso já foi vivido. Em campaign_opening, conte 10-18 parágrafos e normalmente 1200-2200 palavras; em first_arrival, 8-14 parágrafos e 850-1600 palavras; em changed_return, 6-12 parágrafos e 600-1200 palavras; em poi_discovery, 6-12 parágrafos e 600-1200 palavras; em geography_discovery, 5-10 parágrafos e 500-1000 palavras; em weather_transition, 4-8 parágrafos e 350-800 palavras. Se world_discovery.presentation_topics listar mais de um tópico novo, todos precisam ser dramatizados na mesma apresentação antes de serem marcados como conhecidos. Mova a câmera de mundo/região para lugar, pessoas, atividade e problema imediato. Não substitua por briefing, resumo, lista de contexto ou escolhas. Clima, economia, guerras, política e cultura devem aparecer por sinais perceptíveis na ficção. As opções vêm depois da história. O Braseiro continua autoridade mecânica e persistente.";
        if(strixhavenActive)prompt=strixhavenProfile.masterInstruction()+"\n\n"+prompt;
        return new Plan(prompt,context,tick,cognitive,npcTick,environmental,effectiveRuleRetrieval,groundingEnvelope,queued,quota.economyMode(),narrative,nonAdvancing,(explicitHelp?"GM_HELP":(explicitAction?"PLAYER_ACTION":routedIntent.name())),actionHash,stateHash,cacheKey,contextReady,contextError,barbaraOccasion,barbaraLocation,barbaraImprint,inputChannel,discoveryDecision,frozenActionIds);
    }

    public com.braseiro.dnd5e.adventure.AdventureEngine getAdventureEngine(){return adventureEngine;}
    public JSONObject executeOfficialBookRule(String bookId,String actionId,JSONObject input){return rules.executeOfficialBookRule(bookId,actionId,input);}

    private static String resolveActiveSetting(AppDb database, JSONObject scene, JSONObject canonicalPos) {
        if (database == null) return "forgotten_realms";
        String mapProfile = database.getActiveMapProfile();
        if ("mystara".equalsIgnoreCase(mapProfile)) return "mystara";

        // 1. Cena explícita
        if (scene != null) {
            String s = scene.optString("setting", "").trim().toLowerCase(Locale.US);
            if (s.length() > 0) return normalizeSettingName(s);
            String loc = (scene.optString("location", "") + " " + scene.optString("description", "")).toLowerCase(Locale.US);
            if (loc.contains("barovia") || loc.contains("ravenloft") || loc.contains("darkon") || loc.contains("borca") || loc.contains("dementlieu") || loc.contains("szarr")) return "ravenloft";
            if (loc.contains("eberron") || loc.contains("sharn") || loc.contains("khorvaire") || loc.contains("mournland") || loc.contains("cannith")) return "eberron";
            if (loc.contains("lorwyn") || loc.contains("shadowmoor") || loc.contains("aeghith") || loc.contains("aurora")) return "lorwyn";
            if (loc.contains("netheril") || loc.contains("anauroch") || loc.contains("xinok")) return "forgotten_realms";
        }

        // 2. Campanha ativa
        try {
            JSONObject camp = database.getCampaign();
            if (camp != null) {
                String cs = camp.optString("setting", "").trim().toLowerCase(Locale.US);
                if (cs.length() > 0) return normalizeSettingName(cs);
                String notes = camp.optString("notes", "").toLowerCase(Locale.US);
                if (notes.contains("ravenloft") || notes.contains("barovia")) return "ravenloft";
                if (notes.contains("eberron") || notes.contains("khorvaire")) return "eberron";
                if (notes.contains("lorwyn")) return "lorwyn";
            }
        } catch (Exception ignored) {}

        // 3. Posição canônica / Atlas
        if (canonicalPos != null) {
            String posLoc = canonicalPos.optString("scene_location", "").toLowerCase(Locale.US);
            if (posLoc.contains("barovia") || posLoc.contains("ravenloft")) return "ravenloft";
            if (posLoc.contains("eberron") || posLoc.contains("sharn")) return "eberron";
            if (posLoc.contains("aeghith") || posLoc.contains("lorwyn")) return "lorwyn";
        }

        return "forgotten_realms";
    }

    private static String normalizeSettingName(String s) {
        if (s == null) return "forgotten_realms";
        String lower = s.trim().toLowerCase(Locale.US);
        if (lower.contains("faerun") || lower.contains("forgotten") || lower.contains("toril") || lower.contains("dnd")) return "forgotten_realms";
        if (lower.contains("ravenloft") || lower.contains("barovia") || lower.contains("mists")) return "ravenloft";
        if (lower.contains("eberron") || lower.contains("khorvaire")) return "eberron";
        if (lower.contains("lorwyn") || lower.contains("shadowmoor")) return "lorwyn";
        if (lower.contains("multiverse") || lower.contains("planar")) return "multiverse";
        return lower;
    }

    public JSONObject validateEnvelope(JSONObject envelope){return validateEnvelope(envelope,"");}
    public JSONObject validateEnvelope(JSONObject envelope,String action){
        JSONObject candidate=envelope;if(activePlanForValidation!=null&&activePlanForValidation.nonAdvancing)candidate=BraseiroNarrativeDirector.freezeEnvelope(candidate,action,activePlanForValidation.inputChannel);
        JSONObject safe=validator.validate(candidate,action);
        safe=guilds.validateUpdates(safe);
        try{
            String bOccasion=activePlanForValidation==null?"continuation":activePlanForValidation.barbaraStoryOccasion;
            String bSystem="mystara".equalsIgnoreCase(db.getActiveMapProfile())?"mystara":"dnd";JSONObject bReport=barbara==null?new JSONObject().put("valid",true).put("bypassed",true):barbara.validate(safe,action,"normal",bOccasion,activePlanForValidation==null?null:activePlanForValidation.inputChannel,bSystem);
            safe.put("_barbara_validation",bReport);
            if(!bReport.optBoolean("valid",true)){
                JSONObject master=safe.optJSONObject("_master_validation");if(master==null){master=new JSONObject();safe.put("_master_validation",master);}
                master.put("valid",false).put("fail_closed",true).put("barbara_code",bReport.optString("code","barbara_reject"));
                JSONArray warnings=master.optJSONArray("warnings");if(warnings==null){warnings=new JSONArray();master.put("warnings",warnings);}warnings.put("Motor Barbara: "+bReport.optString("code","violação narrativa"));
            }
        }catch(Exception bFailure){AppLog.w("MasterOrchestrator","Falha ao anexar validação Barbara; validações Java foram preservadas.",bFailure);}
        try{
            if(activePlanForValidation!=null&&activePlanForValidation.narrative&&!activePlanForValidation.nonAdvancing){
                String onboardingProblem=WorldOnboardingNarrativeGuard.problem(safe.optString("narration"),activePlanForValidation.barbaraStoryOccasion,activePlanForValidation.discoveryDecision);
                if(onboardingProblem.length()>0){JSONObject master=safe.optJSONObject("_master_validation");if(master==null){master=new JSONObject();safe.put("_master_validation",master);}master.put("valid",false).put("fail_closed",true).put("world_onboarding_problem",onboardingProblem);JSONArray warnings=master.optJSONArray("warnings");if(warnings==null){warnings=new JSONArray();master.put("warnings",warnings);}warnings.put("Apresentação do mundo: "+onboardingProblem);}
            }
        }catch(Exception onboardingFailure){AppLog.e("MasterOrchestrator","Falha ao validar apresentação do mundo; envelope bloqueado.",onboardingFailure);try{JSONObject master=safe.optJSONObject("_master_validation");if(master==null){master=new JSONObject();safe.put("_master_validation",master);}master.put("valid",false).put("fail_closed",true).put("world_onboarding_problem","Falha interna no guarda de descoberta.");}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao fechar envelope de descoberta.",ignored);}}
        try{
            JSONObject report=safe.optJSONObject("_master_validation");JSONObject guildReport=safe.optJSONObject("_guild_validation");boolean valid=report!=null&&report.optBoolean("valid",false)&&(guildReport==null||guildReport.optBoolean("valid",false));if(!valid&&report!=null)report.put("valid",false).put("fail_closed",true);
            if(activePlanForValidation!=null&&valid){if(activePlanForValidation.nonAdvancing){safe=BraseiroNarrativeDirector.freezeEnvelope(safe,action,activePlanForValidation.inputChannel);safe.put("_cognitive_plan",new JSONObject());}else{boolean deferred=safe.optBoolean("_deferred_until_test",false);safe.put("_local_tick",deferred?new JSONObject():activePlanForValidation.localTick);safe.put("_cognitive_plan",activePlanForValidation.cognitivePlan);safe.put("_npc_tick",deferred?new JSONObject():activePlanForValidation.npcTick);}}
            else{stripRejectedStructuredState(safe);}
        }catch(Exception failure){
            AppLog.e("MasterOrchestrator","Falha ao anexar o plano local; envelope bloqueado.",failure);
            try{safe.put("_local_tick",new JSONObject());safe.put("_npc_tick",new JSONObject());safe.put("_master_validation",new JSONObject().put("valid",false).put("fail_closed",true).put("error","Falha ao anexar plano local."));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao fechar envelope sem plano.",ignored);}
        }
        return safe;
    }

    private Plan activePlanForValidation;
    public void setActivePlanForValidation(Plan plan){activePlanForValidation=plan;rules.setActiveRuleRetrieval(plan==null?new JSONObject():plan.ruleRetrieval);}
    public boolean hasStructuredChanges(JSONObject envelope){JSONObject report=envelope==null?null:envelope.optJSONObject("_master_validation");return report!=null&&report.optBoolean("valid",false)&&validator.hasStructuredChanges(envelope);}

    private static void stripRejectedStructuredState(JSONObject envelope){
        if(envelope==null)return;
        try{
            for(String key:new String[]{"state_updates","npc_updates","npc_cognition_updates","knowledge_updates","quest_updates","faction_updates","guild_updates","clock_updates","world_events","event_resolutions","pending_resolutions","journal_updates","map_updates","tactical_updates"})envelope.put(key,new JSONArray());
            envelope.put("scene_state",new JSONObject()).put("combat",new JSONObject().put("action","none")).put("pending_test",new JSONObject()).put("_local_tick",new JSONObject()).put("_npc_tick",new JSONObject()).put("_deferred_until_test",true).put("_safe_narration_only",true);
        }catch(Exception failure){AppLog.e("MasterOrchestrator","Falha ao neutralizar envelope rejeitado; mantendo execução bloqueada.",failure);}
    }

    public synchronized void afterAccepted(Plan plan,String narration,int inputChars,int outputChars){afterAccepted(plan,narration,null,inputChars,outputChars);}
    public synchronized void afterAccepted(Plan plan,String narration,JSONObject envelope,int inputChars,int outputChars){
        if(plan==null)return;boolean discoverySafe=discoveryNarrationAccepted(plan,narration);if(plan.fromCache){if(plan.narrative&&!plan.nonAdvancing){party.updateSpotlightFromNarration(narration);if(discoverySafe)discoveryDirector.commit(plan.discoveryDecision);}if(!plan.accepted&&plan.usedPartyQueue)party.markQueuedNarrated(plan.queuedActionIds);plan.accepted=true;return;}boolean offline=envelope!=null&&envelope.optJSONObject("_offline_master")!=null;if(!offline)quota.recordCall(inputChars,outputChars);if(plan.narrative&&!plan.nonAdvancing){party.updateSpotlightFromNarration(narration);if(discoverySafe)discoveryDirector.commit(plan.discoveryDecision);}if(!plan.accepted){if(plan.usedPartyQueue)party.markQueuedNarrated(plan.queuedActionIds);if(!plan.nonAdvancing)memory.record(plan.userPrompt,narration,plan.cognitivePlan,envelope);plan.accepted=true;}
    }

    private boolean discoveryNarrationAccepted(Plan plan,String narration){
        if(plan==null||!plan.narrative||plan.nonAdvancing)return false;
        try{return WorldOnboardingNarrativeGuard.problem(narration,plan.barbaraStoryOccasion,plan.discoveryDecision).length()==0;}
        catch(Exception failure){AppLog.e("MasterOrchestrator","Falha ao confirmar cobertura da descoberta; conhecimento não será avançado.",failure);return false;}
    }

    public synchronized void afterConsequencesApplied(Plan plan,JSONObject envelope,String narration) throws Exception{
        if(plan==null||plan.committed)return;
        JSONObject validation=envelope==null?null:envelope.optJSONObject("_master_validation");
        if(validation==null||!validation.optBoolean("valid",false))throw new IllegalStateException("As consequências não possuem validação final válida.");
        JSONObject confirmedTick=envelope.optJSONObject("_local_tick");if(confirmedTick==null)confirmedTick=plan.localTick;
        boolean deferred=envelope.optBoolean("_deferred_until_test",false);
        if(confirmedTick!=null&&confirmedTick.length()>0&&!world.commit(confirmedTick))throw new IllegalStateException("O avanço do Mundo Vivo não pôde ser confirmado atomicamente.");
        if(!deferred&&plan.environmentalPlan!=null&&plan.environmentalPlan.length()>0)environmentalDirector.commitChecked(plan.environmentalPlan,narration);
        npcAgency.applyConfirmed(envelope,confirmedTick==null?new JSONObject():confirmedTick,narration);
        applyWorldEnvelope(envelope);
        storeValidationState(envelope,plan);
    }
    public synchronized void markConsequencesCommitted(Plan plan){if(plan!=null)plan.committed=true;}
    public synchronized void markConsequencesRolledBack(Plan plan,JSONObject envelope){
        if(plan!=null){plan.committed=false;resetTickFlags(plan.localTick);}
        if(envelope!=null)resetTickFlags(envelope.optJSONObject("_local_tick"));
    }
    private static void resetTickFlags(JSONObject tick){try{if(tick==null)return;tick.put("committed",false).put("preview_only",true);JSONObject simulation=tick.optJSONObject("living_world_simulation");if(simulation!=null)simulation.put("committed",false).put("preview_only",true);}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao reiniciar marcações após rollback.",ignored);}}
    public synchronized void afterConsequencesDiscarded(Plan plan,JSONObject envelope){if(plan==null)return;try{db.addAuditEvent("mestre","Consequências descartadas","A narração foi preservada; mundo, NPCs e recursos não avançaram.",envelope==null?"{}":envelope.toString(),"{}",true);}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha tolerada; a execução continuou.",ignored);}}

    public String cachedResponse(Plan plan,String model,String mode){if(plan==null||plan.narrative)return "";String cached=db.getMasterCache(hash(plan.cacheKey+"|"+model+"|"+mode),10L*60L*1000L);if(cached.length()>0)plan.fromCache=true;return cached;}
    public void cacheResponse(Plan plan,String model,String mode,String raw){if(plan==null||plan.narrative||raw==null||raw.length()<40||plan.fromCache)return;db.saveMasterCache(hash(plan.cacheKey+"|"+model+"|"+mode),mode,model,plan.actionHash,plan.stateHash,raw);}

    public JSONObject groundingFor(Plan plan){
        try{
            JSONObject rulesGround=compactRuleRetrieval(plan==null?new JSONObject():plan.ruleRetrieval);
            JSONObject lore=firewall.promptView(plan==null?"":plan.userPrompt);
            JSONObject campaignGround=new JSONObject().put("campaign",db.getCampaign()).put("scene",db.getSceneState()).put("party",db.getPartyState()).put("map",db.getMapState()).put("living_world",new JSONObject(new LivingWorldEngine(db).compactContext())).put("environmental_director",environmentalDirector.promptView(environmentalDirector.preview("")));
            JSONObject inference=new JSONObject().put("cognitive_plan",plan==null?new JSONObject():plan.cognitivePlan).put("world_discovery",plan==null?new JSONObject():plan.discoveryDecision);
            return GroundingEnvelope.build(rulesGround,lore,campaignGround,inference);
        }catch(Exception failure){AppLog.e("MasterOrchestrator","Falha ao montar GroundingEnvelope para commit.",failure);return GroundingEnvelope.build(new JSONObject(),new JSONObject(),new JSONObject(),new JSONObject());}
    }

    /** Mechanical supplement actions exposed to the game runtime through the same arbiter used by Barbara. */
    public JSONObject executeCompiledSupplement(String ruleId,JSONObject inputs,JSONObject requestedMutation){return rules.executeCompiledSupplement(ruleId,inputs,requestedMutation);}
    public JSONObject compiledSupplementReadiness(String ruleId,JSONObject inputs){return rules.compiledSupplementReadiness(ruleId,inputs);}

    /** GAMEPLAY-001 sandbox bridge. The Master only delegates validated operations to WorldStateEngine. */
    public JSONObject gameplaySandboxContext(){return world.gameplaySandboxContext();}
    public JSONObject previewGameplayInput(String text,String inputChannel,JSONObject payload){return world.previewGameplayInput(text,inputChannel,payload);}
    public JSONObject commitGameplayInput(String text,String inputChannel,JSONObject payload){return world.commitGameplayInput(text,inputChannel,payload);}
    public JSONObject resolveGameplayEberronFramework(long characterId,String framework,String action,JSONObject payload){return world.resolveGameplayEberronFramework(characterId,framework,action,payload);}
    public JSONObject resolveGameplayFaerunFaction(long characterId,String faction,String action,JSONObject payload){return world.resolveGameplayFaerunFaction(characterId,faction,action,payload);}
    public JSONObject resolveGameplayFaerunDeityRenown(long characterId,String deity,int delta,JSONObject payload){return world.resolveGameplayFaerunDeityRenown(characterId,deity,delta,payload);}
    public JSONObject beginGameplayTravel(String origin,String destination,String service,JSONObject payload){return world.beginGameplayTravel(origin,destination,service,payload);}
    public JSONObject resolveGameplayFaerunTravelService(long characterId,String item,String action,JSONObject payload){return world.resolveGameplayFaerunTravelService(characterId,item,action,payload);}
    public JSONObject gateGeminiGameplayProposal(JSONObject proposal){return world.gateGeminiGameplayProposal(proposal);}

    public JSONObject livingWorldAggregate(){return db.getLivingWorldAggregate();}
    public JSONObject spawnWorldStatblockActor(JSONObject payload,int day){return db.mutateLivingWorldAggregate("population_spawn_statblock",payload==null?"":payload.optString("id"),payload,day);}
    public JSONArray worldActorsAt(String location){JSONObject p=db.getLivingWorldAggregate().optJSONObject("population");return WorldPopulationEngine.at(p,location==null?"":location);}
    public JSONObject worldActor(String id){JSONObject p=db.getLivingWorldAggregate().optJSONObject("population");return WorldPopulationEngine.get(p,id==null?"":id);}
    public JSONObject mutateLivingWorldAggregate(String operation,String entityKey,JSONObject payload,int day){return db.mutateLivingWorldAggregate(operation,entityKey,payload,day);}
    public JSONObject setDowntimeProjectStatusRuntime(String projectId,String status,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("downtime_status",projectId,new JSONObject().put("status",status),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject recordJobShiftRuntime(String projectId,double units,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("job_shift",projectId,new JSONObject().put("units",units),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject createCraftingRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("craft_create",payload==null?"":payload.optString("id"),payload,day);}
    public JSONObject workCraftingRuntime(String projectId,double units,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("craft_work",projectId,new JSONObject().put("units",units),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject resolveCraftingRuntime(String projectId,JSONObject payload,int day){return db.mutateLivingWorldAggregate("craft_resolve",projectId,payload,day);}
    public JSONObject reserveWorldEconomy(double amount,String reason,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("economy_reserve","ledger",new JSONObject().put("amount",amount).put("reason",reason),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject commitWorldEconomy(JSONObject payload,int day){return db.mutateLivingWorldAggregate("economy_commit","ledger",payload,day);}
    public JSONObject releaseWorldEconomyReserve(double amount,String reason,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("economy_release","ledger",new JSONObject().put("amount",amount).put("reason",reason),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject configureCookingRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("cooking_configure","cook",payload,day);}
    public JSONObject restockCookingRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("cooking_restock","cook",payload,day);}
    public JSONObject prepareCookingRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("cooking_prepare","cook",payload,day);}
    public JSONObject feedCookingRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("cooking_feed","cook",payload,day);}
    public JSONObject cookingLeftoversRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("cooking_leftovers","cook",payload,day);}
    public JSONObject applyWorldEconomyPressure(String region,JSONObject event,int day){return db.mutateLivingWorldAggregate("economy",region,event,day);}
    public JSONObject advanceLivingWorldDay(int day){return db.mutateLivingWorldAggregate("advance_day","world",new JSONObject(),day);}
    public JSONObject resolveLivingWorldMission(String missionKey,String status,String summary,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("mission_resolve",missionKey,new JSONObject().put("status",status).put("summary",summary==null?"":summary),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject resolveNpcCommitmentRuntime(String npc,String commitmentKey,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("npc_commitment_resolve",npc,new JSONObject().put("commitment_key",commitmentKey),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject acknowledgeNpcCommitmentRuntime(String npc,String commitmentKey,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("npc_commitment_ack",npc,new JSONObject().put("commitment_key",commitmentKey),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject acknowledgeFactionTreatyRuntime(String faction,String treatyKey,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("faction_treaty_ack",faction,new JSONObject().put("treaty_key",treatyKey),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject acknowledgePoliticalDecreeRuntime(String decreeKey,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("politics_decree_ack","politics",new JSONObject().put("decree_key",decreeKey),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject configureGroupPatronRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("patron_configure","patron",payload,day);}
    public JSONObject patronAssistRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("patron_assist","patron",payload,day);}
    public JSONObject addPatronAssignmentRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("patron_assignment_add",payload==null?"":payload.optString("key"),payload,day);}
    public JSONObject resolvePatronAssignmentRuntime(String key,String status,String summary,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("patron_assignment_resolve",key,new JSONObject().put("status",status).put("summary",summary==null?"":summary),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject createSidekickRuntime(JSONObject payload,int day){return db.mutateLivingWorldAggregate("sidekick_create",payload==null?"":payload.optString("id"),payload,day);}
    public JSONObject levelSidekickRuntime(String id,int hpDieRoll,int constitutionModifier,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("sidekick_level",id,new JSONObject().put("hp_die_roll",hpDieRoll).put("constitution_modifier",constitutionModifier),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject sidekickShortRestRuntime(String id,int day){return db.mutateLivingWorldAggregate("sidekick_short_rest",id,new JSONObject(),day);}
    public JSONObject sidekickLongRestRuntime(String id,int day){return db.mutateLivingWorldAggregate("sidekick_long_rest",id,new JSONObject(),day);}
    public JSONObject sidekickSecondWindRuntime(String id,int d10Roll,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("sidekick_second_wind",id,new JSONObject().put("d10_roll",d10Roll),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject sidekickIndomitableRuntime(String id,int rerollTotal,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("sidekick_indomitable",id,new JSONObject().put("original_failed",true).put("reroll_total",rerollTotal),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject sidekickExpertHelpRuntime(String id,String ally,String target,boolean helpsAttack,double distanceFt,int inspiringRoll,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("sidekick_expert_help",id,new JSONObject().put("ally",ally).put("target",target).put("helps_attack",helpsAttack).put("target_distance_ft",distanceFt).put("inspiring_roll",inspiringRoll),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject sidekickExpertStrikeRuntime(String id,String target,boolean hit,int rolled2d6,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("sidekick_expert_strike",id,new JSONObject().put("target",target).put("attack_hit",hit).put("rolled_2d6",rolled2d6),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject sidekickSpellCastRuntime(String id,int spellLevel,int abilityModifier,boolean cantripDealsDamage,boolean empoweredSchoolMatches,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("sidekick_spell_cast",id,new JSONObject().put("spell_level",spellLevel).put("spellcasting_ability_modifier",abilityModifier).put("cantrip_deals_damage",cantripDealsDamage).put("empowered_school_matches",empoweredSchoolMatches),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject sidekickWarriorProfile(String id){JSONObject s=db.getLivingWorldAggregate().optJSONObject("sidekicks");return TashaCampaignSystemsEngine.warriorProfile(s==null?null:s.optJSONObject(id));}
    public JSONObject sidekickExpertEvasion(String id,boolean incapacitated,boolean saveSuccess,int incomingDamage){JSONObject s=db.getLivingWorldAggregate().optJSONObject("sidekicks");return TashaCampaignSystemsEngine.expertEvasion(s==null?null:s.optJSONObject(id),incapacitated,saveSuccess,incomingDamage);}
    public JSONObject sidekickExpertReliableTalent(String id,boolean proficient,int d20){JSONObject s=db.getLivingWorldAggregate().optJSONObject("sidekicks");return TashaCampaignSystemsEngine.expertReliableTalent(s==null?null:s.optJSONObject(id),proficient,d20);}
    public JSONObject sidekickSpellcasterProfile(String id,int abilityModifier){JSONObject s=db.getLivingWorldAggregate().optJSONObject("sidekicks");return TashaCampaignSystemsEngine.spellcasterProfile(s==null?null:s.optJSONObject(id),abilityModifier);}
    public JSONObject sidekickConcentrationAfterDamage(String id,int damage){JSONObject s=db.getLivingWorldAggregate().optJSONObject("sidekicks");return TashaCampaignSystemsEngine.spellcasterConcentrationAfterDamage(s==null?null:s.optJSONObject(id),damage);}
    public JSONObject startAvalancheRuntime(String id,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("hazard_avalanche_start",id,new JSONObject().put("id",id),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject avalancheImpactRuntime(String id,String victim,int strengthSaveTotal,int rolledDamage,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("hazard_avalanche_impact",id,new JSONObject().put("victim",victim).put("strength_save_total",strengthSaveTotal).put("rolled_damage",rolledDamage),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject stopAvalancheRuntime(String id,int day){return db.mutateLivingWorldAggregate("hazard_avalanche_stop",id,new JSONObject(),day);}
    public JSONObject avalancheEscapeRuntime(String id,String victim,int athleticsTotal,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("hazard_avalanche_escape",id,new JSONObject().put("victim",victim).put("athletics_total",athleticsTotal),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject tashaFallIntoWater(int fallingDamage,int athleticsOrAcrobaticsTotal){return TashaCampaignSystemsEngine.fallIntoWater(fallingDamage,athleticsOrAcrobaticsTotal);}
    public JSONObject tashaFallOntoCreature(int fallingDamage,int targetDexSaveTotal,String fallingSize,String targetSize){return TashaCampaignSystemsEngine.fallOntoCreature(fallingDamage,targetDexSaveTotal,fallingSize,targetSize);}
    public JSONArray tashaNaturalHazardSpellApproximations(){return TashaCampaignSystemsEngine.naturalHazardSpellApproximations();}
    public JSONObject createSpellEquivalentNaturalHazardRuntime(String id,String hazard,String approximateSpell,String location,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("hazard_spell_create",id,new JSONObject().put("id",id).put("hazard",hazard).put("approximate_spell",approximateSpell).put("location",location),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject resolveSpellEquivalentNaturalHazardRuntime(String id,String resolution,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("hazard_spell_resolve",id,new JSONObject().put("resolution",resolution),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject tashaMonsterResearch(String type,double cr,String skill,int total){return TashaCampaignSystemsEngine.monsterResearch(type,cr,skill,total);}
    public JSONObject tashaMonsterNegotiation(String type,int roll,boolean offerMatches){return TashaCampaignSystemsEngine.monsterNegotiation(type,roll,offerMatches);}
    public JSONObject tashaSubclassChangePlan(int classLevel,boolean returning,boolean suddenApproved){return TashaCampaignSystemsEngine.subclassChangePlan(classLevel,returning,suddenApproved);}
    public JSONObject tashaWorldPhenomenaDescriptor(){return TashaWorldPhenomenaEngine.descriptor();}
    public JSONObject createSupernaturalRegionRuntime(String id,String type,String location,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("supernatural_region_create",id,new JSONObject().put("id",id).put("type",type).put("location",location),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject triggerSupernaturalRegionRuntime(String id,String event,int d100,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("supernatural_region_trigger",id,new JSONObject().put("event",event).put("d100",d100),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject acknowledgeSupernaturalRegionRuntime(String id,String resolution,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("supernatural_region_ack",id,new JSONObject().put("resolution",resolution),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject evaluateSupernaturalRegionRuntime(String id,JSONObject context){return LivingWorldPersistenceEngine.evaluateSupernaturalRegion(db.getLivingWorldAggregate(),id,context);}
    public JSONObject startMysticStormRuntime(String id,String type,int durationUnits,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("mystic_storm_start",id,new JSONObject().put("id",id).put("type",type).put("duration_units",durationUnits),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject mysticStormExposureRuntime(String id,String victim,int saveTotal,int rolledDamage,boolean immune,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("mystic_storm_exposure",id,new JSONObject().put("victim",victim).put("save_total",saveTotal).put("rolled_damage",rolledDamage).put("immune",immune),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject stopMagicalPhenomenonRuntime(String id,int day){return db.mutateLivingWorldAggregate("magical_phenomenon_stop",id,new JSONObject(),day);}
    public JSONObject createEmotionalEchoRuntime(String id,String emotion,String location,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("emotional_echo_create",id,new JSONObject().put("id",id).put("emotion",emotion).put("location",location),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject triggerEmotionalEchoRuntime(String id,String creature,boolean expressed,int wisdomSaveTotal,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("emotional_echo_trigger",id,new JSONObject().put("creature",creature).put("emotion_expressed",expressed).put("wisdom_save_total",wisdomSaveTotal),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject createEnchantedFountainRuntime(String id,String location,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("enchanted_fountain_create",id,new JSONObject().put("id",id).put("location",location),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject useEnchantedFountainRuntime(String id,String creature,int d12,int durationRoll,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("enchanted_fountain_use",id,new JSONObject().put("creature",creature).put("d12",d12).put("duration_roll",durationRoll),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject installTashaMimicColonyStatblocks(){return TashaMimicColonyEngine.installStatblocks();}
    public JSONObject createMimicColonyRuntime(String id,String location,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("mimic_colony_create",id,new JSONObject().put("id",id).put("location",location),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject mimicColonyLairActionRuntime(String id,String action,JSONArray targets,JSONObject saveTotals,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("mimic_colony_lair",id,new JSONObject().put("action",action).put("targets",targets==null?new JSONArray():targets).put("save_totals",saveTotals==null?new JSONObject():saveTotals),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject beginMimicColonyBargainRuntime(String id,String offer,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("mimic_colony_bargain",id,new JSONObject().put("offer",offer),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject resolveMimicColonyBargainRuntime(String id,String status,String result,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("mimic_colony_bargain_resolve",id,new JSONObject().put("status",status).put("result",result),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject createMagicalMushroomPatchRuntime(String id,String location,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("magical_mushroom_create",id,new JSONObject().put("id",id).put("location",location),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject consumeMagicalMushroomRuntime(String id,String creature,int d10,int durationRoll,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("magical_mushroom_consume",id,new JSONObject().put("creature",creature).put("d10",d10).put("duration_roll",durationRoll),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject createPrimalFruitPlantRuntime(String id,String location,int weeklyYield,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("primal_fruit_create",id,new JSONObject().put("id",id).put("location",location).put("weekly_yield",weeklyYield),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject harvestPrimalFruitRuntime(String id,int weeklyYield,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("primal_fruit_harvest",id,new JSONObject().put("weekly_yield",weeklyYield),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject consumePrimalFruitRuntime(String id,String creature,int d8,int durationRoll,int charismaSaveTotal,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("primal_fruit_consume",id,new JSONObject().put("creature",creature).put("d8",d8).put("duration_roll",durationRoll).put("charisma_save_total",charismaSaveTotal),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject createSupernaturalRoadRuntime(String id,String from,String to,int keyRoll,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("supernatural_road_create",id,new JSONObject().put("id",id).put("from",from).put("to",to).put("key_roll",keyRoll),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    public JSONObject supernaturalRoadTravelRuntime(String id,double roadDistanceKm,int day){/*BR_JSON_WRAP_106*/try{return db.mutateLivingWorldAggregate("supernatural_road_travel",id,new JSONObject().put("road_distance_km",roadDistanceKm),day);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public JSONArray livingWorldBarbaraProposals(int day){return LivingWorldPersistenceEngine.barbaraProposals(db.getLivingWorldAggregate(),day);}

    public JSONObject compiledContentReadiness(String kind,String name){return rules.compiledContentReadiness(kind,name);}
    public JSONObject executeCompiledContent(String kind,String name,JSONObject context){return rules.executeCompiledContent(kind,name,context);}
    public JSONObject dispatchCompiledPlayerAction(JSONObject action){return CompiledPlayerActionDispatcher.dispatch(rules,action);}
    public JSONObject releaseAuthorityHealth(){return ReleaseAuthorityHealthEngine.report();}
    public JSONObject releaseGate(){return ReleaseGateEngine.evaluate();}
    public JSONObject registerAnimalCompanion(long ownerCharacterId,String name,String species,String size,int intelligenceRating,int handlerWisdomScore,boolean domestic,String location)throws Exception{return new AnimalCompanionStateEngine(db).register(ownerCharacterId,name,species,size,intelligenceRating,handlerWisdomScore,domestic,location);}
    public JSONArray animalCompanions(int limit){return new AnimalCompanionStateEngine(db).snapshotAll(limit);}
    public JSONObject configureAnimalSidekick(String name,String role,int level)throws Exception{return new AnimalCompanionStateEngine(db).configureSidekick(name,role,level);}
    public JSONObject careForAnimal(String name,int days)throws Exception{return new AnimalCompanionStateEngine(db).feedAndWater(name,days);}
    public JSONObject animalLoyaltyEvent(String name,int d4,boolean gain,String reason)throws Exception{return new AnimalCompanionStateEngine(db).applyLoyaltyEvent(name,d4,gain,reason);}
    public JSONObject startAnimalAcclimation(long owner,String name,boolean captive,String location)throws Exception{return new AnimalCompanionStateEngine(db).startAcclimation(owner,name,captive,location);}
    public JSONObject completeAnimalAcclimation(long owner,String name,long activityId)throws Exception{return new AnimalCompanionStateEngine(db).completeAcclimation(owner,name,activityId);}
    public JSONObject startAnimalTrickTraining(long owner,String name,String trick,String location)throws Exception{return new AnimalCompanionStateEngine(db).startTrickTraining(owner,name,trick,location);}
    public JSONObject completeAnimalTrickTraining(long owner,String name,String trick,long activityId)throws Exception{return new AnimalCompanionStateEngine(db).completeTrickTraining(owner,name,trick,activityId);}
    public JSONObject installSrd51BestiaryBatch(){JSONObject out=Srd51BestiaryBatchEngine.install();try{out.put("npc_appendix",Srd51NpcStatblockBatchEngine.install());}catch(Exception ignored){}try{out.put("monster_b_batch",Srd51MonsterBatchBEngine.install());}catch(Exception ignored){}try{out.put("monster_c_batch",Srd51MonsterBatchCEngine.install());}catch(Exception ignored){}try{out.put("faerun_core_batch",Srd51FaerunCoreMonsterEngine.install());}catch(Exception ignored){}return out;}
    public JSONObject installTashaCompiledSourceRules(){return TashaCompiledSourceRulesEngine.install();}
    public JSONObject installRun137CharacterClassRuntimes(){JSONObject o=new JSONObject();try{return o.put("core2024",Core2024ClassRuntimeEngine.install()).put("homebrew_classes",HomebrewClassRuntimeEngine.install()).put("homebrew_subclasses",HomebrewSubclassRuntimeEngine.install()).put("auto_rolls",false);}catch(Exception e){return o;}}
    public JSONObject installRun138CharacterOptions(){JSONObject o=new JSONObject();try{return o.put("homebrew_classes_legacy",HomebrewClassRuntimeEngine.install()).put("xama",HomebrewClassRuntimeEngine.installRun138()).put("homebrew_subclasses",HomebrewSubclassRuntimeEngine.install()).put("volo_subrace_handbook",VoloSubraceHandbookEngine.install()).put("tomo_clerigo_block_a","deduplicated_no_new_class_or_subclass").put("auto_rolls",false);}catch(Exception e){return o;}}
    public JSONObject installRun139VoloRacialFeats(){JSONObject o=new JSONObject();try{return o.put("volo_racial_feats",VoloRacialFeatRuntimeEngine.install()).put("descriptor",VoloRacialFeatRuntimeEngine.descriptor()).put("auto_rolls",false);}catch(Exception e){return o;}}
    public JSONObject resolveCharacterClassAction(long characterId,String action,JSONObject payload){return db.resolveCharacterClassAction(characterId,action,payload);}
    public JSONObject resolveCharacterHomebrewSubclassAction(long characterId,String feature,JSONObject payload){return db.resolveCharacterHomebrewSubclassAction(characterId,feature,payload);}
    public JSONObject forgottenSecretsSpellCatalog(){try{return new JSONObject().put("magias",Run149ForgottenSecretsSpellEngine.allSpells()).put("descriptor",Run149ForgottenSecretsSpellEngine.descriptor()).put("surface_language","pt-BR").put("auto_rolls",false);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject resolveForgottenSecretsSpell(long characterId,String spell,int slotLevel,JSONObject payload){return db.resolveForgottenSecretsSpell(characterId,spell,slotLevel,payload);}
    public JSONObject eberronForgeCatalog(){try{return new JSONObject().put("descriptor",EberronForgeCompleteEngine.descriptor()).put("coverage",EberronForgeCompleteEngine.coverage()).put("subclasses",EberronForgeCompleteEngine.subclasses()).put("species",EberronForgeCompleteEngine.species()).put("backgrounds",EberronForgeCompleteEngine.backgrounds()).put("feats",EberronForgeCompleteEngine.feats()).put("bastions",EberronForgeCompleteEngine.bastions()).put("airships",EberronForgeCompleteEngine.airships()).put("magic_items",EberronForgeCompleteEngine.magicItems()).put("bestiary",EberronForgeCompleteEngine.creatures()).put("surface_language","pt-BR").put("auto_rolls",false);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject heroesFaerunCatalog(){try{return new JSONObject().put("descriptor",ForgottenRealmsHeroesEngine.descriptor()).put("coverage",ForgottenRealmsHeroesEngine.coverage()).put("subclasses",ForgottenRealmsHeroesEngine.subclasses()).put("backgrounds",ForgottenRealmsHeroesEngine.backgrounds()).put("feats",ForgottenRealmsHeroesEngine.feats()).put("spells",ForgottenRealmsHeroesEngine.spells()).put("factions",ForgottenRealmsHeroesEngine.factions()).put("aurora_gear",ForgottenRealmsHeroesEngine.auroraGear()).put("aurora_mounts",ForgottenRealmsHeroesEngine.auroraMounts()).put("aurora_tools",ForgottenRealmsHeroesEngine.auroraTools()).put("aurora_services",ForgottenRealmsHeroesEngine.auroraServices()).put("aurora_vehicles",ForgottenRealmsHeroesEngine.auroraVehicles()).put("surface_language","pt-BR").put("auto_rolls",false);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject resolveEberronArtificerAction(long characterId,String action,JSONObject payload){return db.resolveEberronArtificerAction(characterId,action,payload);}
    public JSONObject resolveEberronSubclassAction(long characterId,String action,JSONObject payload){return db.resolveEberronSubclassAction(characterId,action,payload);}
    public JSONObject resolveEberronSpeciesAction(long characterId,String action,JSONObject payload){return db.resolveEberronSpeciesAction(characterId,action,payload);}
    public JSONObject resolveEberronFeatAction(long characterId,String feat,String action,JSONObject payload){return db.resolveEberronFeatAction(characterId,feat,action,payload);}
    public JSONObject resolveEberronHomunculusSpell(long characterId,int slotLevel,JSONObject payload){return db.resolveEberronHomunculusSpell(characterId,slotLevel,payload);}
    public JSONObject resolveEberronMagicItemAction(long characterId,String item,String action,JSONObject payload){return db.resolveEberronMagicItemAction(characterId,item,action,payload);}
    public JSONObject resolveEberronBastionAction(long characterId,String facility,String action,JSONObject payload){return db.resolveEberronBastionAction(characterId,facility,action,payload);}
    public JSONObject resolveEberronAirshipAction(long characterId,String ship,String action,JSONObject payload){return db.resolveEberronAirshipAction(characterId,ship,action,payload);}
    public JSONObject resolveEberronFrameworkAction(long characterId,String framework,String action,JSONObject payload){return db.resolveEberronFrameworkAction(characterId,framework,action,payload);}
    public JSONObject eberronCreature(String creature,JSONObject context){return db.eberronCreature(creature,context);}
    public JSONObject resolveEberronCreatureAction(String creature,String action,JSONObject payload){return db.resolveEberronCreatureAction(creature,action,payload);}
    public JSONObject resolveArcanaUnleashedSpell(long characterId,String spell,int slotLevel,JSONObject payload){return db.resolveArcanaUnleashedSpell(characterId,spell,slotLevel,payload);}
    public JSONObject resolveArcanaUnleashedBastion(long characterId,String facility,String order,JSONObject payload,int currentDay){return db.resolveArcanaUnleashedBastion(characterId,facility,order,payload,currentDay);}
    public JSONObject applyArcanaUnleashedRenown(long characterId,String faction,int delta){return db.applyArcanaUnleashedRenown(characterId,faction,delta);}
    public JSONObject evolveArcanaUnleashedInventoryItem(long characterId,String itemId,String method,JSONObject payload){return db.evolveArcanaUnleashedInventoryItem(characterId,itemId,method,payload);}
    public JSONObject resolveArcanaUnleashedItemAction(long characterId,String item,String action,JSONObject payload){return db.resolveArcanaUnleashedItemAction(characterId,item,action,payload);}
    public JSONObject resolveArcanaUnleashedItemDawn(long characterId,JSONObject rechargeRolls){return db.resolveArcanaUnleashedItemDawn(characterId,rechargeRolls);}
    public JSONObject tickArcanaUnleashedItemHours(long characterId,int hours){return db.tickArcanaUnleashedItemHours(characterId,hours);}
    public JSONObject arcanaUnleashedItemInventoryRules(long characterId){return db.arcanaUnleashedItemInventoryRules(characterId);}
    public JSONObject arcanaUnleashedMagicItemCatalog(){try{return new JSONObject().put("itens",ArcanaUnleashedRun145ItemEngine.allPrimaryItems()).put("variantes_colar_familiar",ArcanaUnleashedRun145ItemEngine.allNecklaceVariants()).put("grupos_evolutivos",ArcanaUnleashedRun145ItemEngine.allEvolvingGroups()).put("surface_language","pt-BR").put("auto_rolls",false);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject bestiarioArcanaUnleashed(String criatura,JSONObject contexto){return db.arcanaUnleashedCreature(criatura,contexto);}
    public JSONObject resolverAcaoCriaturaArcanaUnleashed(String criatura,String acao,JSONObject payload){return db.resolveArcanaUnleashedCreatureAction(criatura,acao,payload);}
    public JSONObject catalogoBestiarioArcanaUnleashed(){try{return new JSONObject().put("criaturas",ArcanaUnleashedRun145BestiaryEngine.allCreatures()).put("surface_language","pt-BR").put("auto_rolls",false);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject coberturaArcanaUnleashedRun146(){return ArcanaUnleashedRun146ReconciliationEngine.coverage();}
    public JSONObject catalogoItensEvolutivosArcanaUnleashed(){try{return new JSONObject().put("estagios",ArcanaUnleashedRun147EvolvingItemsEngine.allStages()).put("surface_language","pt-BR").put("auto_rolls",false);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject resolverItemEvolutivoArcanaUnleashed(long characterId,String item,String action,JSONObject payload){return db.resolveArcanaUnleashedEvolvingItemAction(characterId,item,action,payload);}
    public JSONObject evoluirItemProntoArcanaUnleashed(long characterId,String itemId,boolean confirmar){return db.evolveArcanaUnleashedReadyMadeItem(characterId,itemId,confirmar);}
    public JSONObject tashaCompiledSourceReadiness(){return TashaCompiledSourceRulesEngine.readiness();}
    public JSONObject installTashaFeatPack(){return TashaFeatCompiledRulesEngine.install();}
    public JSONObject tashaFeatReadiness(){return TashaFeatCompiledRulesEngine.readiness();}
    public JSONObject installSrd51NpcStatblockBatch(){return Srd51NpcStatblockBatchEngine.install();}
    public JSONObject installSrd51MonsterBatchB(){return Srd51MonsterBatchBEngine.install();}
    public JSONObject installSrd51MonsterBatchC(){return Srd51MonsterBatchCEngine.install();}
    public JSONObject installSrd51FaerunCoreMonsterBatch(){return Srd51FaerunCoreMonsterEngine.install();}
    public JSONObject statblockAttack(String id,String action,int d20,int targetAc){return StatblockCombatRuntimeEngine.attack(id,action,d20,targetAc);}
    public JSONObject statblockTraits(String id){return StatblockCombatRuntimeEngine.tacticalTraits(id);}
    public JSONObject statblockTraitPolicy(String id,String traitName,JSONObject context){return StatblockCombatRuntimeEngine.evaluateTrait(id,traitName,context);}
    public JSONObject statblockMovement(String id){return StatblockCombatRuntimeEngine.movement(id);}
    public JSONObject statblockSavingThrowAction(String id,String action,int saveTotal){return StatblockCombatRuntimeEngine.savingThrowAction(id,action,saveTotal);}
    public JSONObject statblockAttackRiderSave(String id,String action,int saveTotal){return StatblockCombatRuntimeEngine.attackRiderSavingThrow(id,action,saveTotal);}
    public JSONObject statblockRechargeCheck(String id,String action,int d6){return StatblockCombatRuntimeEngine.rechargeCheck(id,action,d6);}
    public JSONObject statblockMultiattack(String id){return StatblockCombatRuntimeEngine.multiattack(id);}
    public JSONObject spawnStatblockToken(String token,String id){return StatblockEncounterStateEngine.spawn(token,id);}
    public JSONObject damageStatblockToken(String token,int amount){return StatblockEncounterStateEngine.damage(token,amount);}
    public JSONObject healStatblockToken(String token,int amount){return StatblockEncounterStateEngine.heal(token,amount);}
    public JSONObject setStatblockCondition(String token,String condition,boolean active){return StatblockEncounterStateEngine.condition(token,condition,active);}
    public JSONObject setStatblockCover(String token,String cover){return StatblockEncounterStateEngine.setCover(token,cover);}
    public JSONObject setStatblockTeam(String token,String team){return StatblockEncounterStateEngine.setTeam(token,team);}
    public JSONObject applyTimedStatblockCondition(String token,String condition,int rounds){return StatblockConditionRuntimeEngine.apply(token,condition,rounds);}
    public JSONObject tickStatblockConditions(String token){return StatblockConditionRuntimeEngine.tick(token);}
    public JSONObject rollStatblockRecharge(String token,String action,int d6,int minimum){return StatblockEncounterStateEngine.rechargeRoll(token,action,d6,minimum);}
    public JSONObject statblockEncounterSnapshot(){return StatblockEncounterStateEngine.snapshot();}
    public JSONObject restoreStatblockEncounter(JSONObject snapshot){return StatblockEncounterStateEngine.restore(snapshot);}
    public JSONObject setStatblockInitiative(String token,int total){return StatblockTurnEngine.setInitiative(token,total);}
    public JSONObject startStatblockTurns(){return StatblockTurnEngine.start();}
    public JSONObject nextStatblockTurn(){return StatblockTurnEngine.next();}
    public JSONObject spendStatblockAction(String token,String resource){return StatblockTurnEngine.spend(token,resource);}
    public JSONObject moveStatblockToken(String token,int feet){return StatblockTurnEngine.movement(token,feet);}
    public JSONObject statblockTurnState(){return StatblockTurnEngine.state();}
    public JSONObject nextLivingStatblockTurn(){return StatblockEncounterLifecycleEngine.nextLivingTurn();}
    public JSONObject statblockEncounterStatus(){return StatblockEncounterLifecycleEngine.status();}
    public JSONObject statblockEncounterBundleSnapshot(){return StatblockEncounterLifecycleEngine.snapshot();}
    public JSONObject restoreStatblockEncounterBundle(JSONObject snapshot){return StatblockEncounterLifecycleEngine.restore(snapshot);}


    public String quotaStatus(){return quota.statusPtBr();}
    public String marker(){return quota.systemMarker();}
    public boolean canCall(){return quota.canCall();}
    public boolean economyMode(){return quota.economyMode();}
    public JSONObject quotaDiagnostic(){return quota.diagnostic();}
    public JSONObject diagnostic(){JSONObject out=new JSONObject();try{out.put("cognition",cognition.diagnostic());out.put("knowledge_firewall",firewall.diagnostic());out.put("npc_agency",npcAgency.diagnostic());out.put("guilds",guilds.diagnostic());out.put("memory",memory.diagnostic());out.put("environmental_director",environmentalDirector.promptView(environmentalDirector.preview("")));out.put("quota",quota.diagnostic());out.put("barbara",barbara==null?new JSONObject().put("available",false):barbara.diagnostic());out.put("world_discovery",discoveryDirector.diagnostic());out.put("connection_audit",gameContext.auditConnections());out.put("complete",gameContext.auditConnections().optBoolean("complete",false));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha tolerada; a execução continuou.",ignored);}return out;}

    private JSONObject compactWorld(String focus){JSONObject out=new JSONObject();try{out.put("quests",WorldContextSelector.select(db.listQuests("active",1000),focus,12,"title","objective","stage","stakes"));JSONArray selectedFactions=WorldContextSelector.select(db.listFactions(1000),focus,10,"name","agenda","current_plan","location");JSONArray factions=new JSONArray();for(int i=0;i<selectedFactions.length();i++){JSONObject f=selectedFactions.optJSONObject(i);if(f!=null)factions.put(compact(f,new String[]{"id","name","agenda","current_plan","relationship","location","active"}));}out.put("factions",factions);out.put("guilds",guilds.promptView(focus,12));out.put("clocks",WorldContextSelector.select(db.listWorldClocks(true,1000),focus,14,"name","consequence","owner_entity","trigger_condition"));out.put("events_due_or_upcoming",WorldContextSelector.select(db.listUpcomingWorldEvents(1000),focus,10,"title","description","event_key"));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha tolerada; a execução continuou.",ignored);}return out;}

    private void applyWorldEnvelope(JSONObject envelope) throws Exception{
        if(envelope==null)return;
        JSONArray facts=envelope.optJSONArray("knowledge_updates");if(facts!=null)for(int i=0;i<facts.length();i++){JSONObject f=facts.optJSONObject(i);if(f==null)continue;db.addKnowledgeFactChecked(f.optString("subject"),f.optString("content"),f.optString("truth_status","verdade"),f.optString("visibility","grupo"),jsonArrayText(f.opt("known_by_json")),f.optString("reveal_condition"),f.optString("source","Mestre virtual"));}
        JSONArray quests=envelope.optJSONArray("quest_updates");if(quests!=null)for(int i=0;i<quests.length();i++)applyQuest(quests.optJSONObject(i));
        JSONArray factions=envelope.optJSONArray("faction_updates");if(factions!=null)for(int i=0;i<factions.length();i++)db.applyFactionUpdateChecked(factions.optJSONObject(i));
        guilds.applyUpdates(envelope);
        JSONArray clocks=envelope.optJSONArray("clock_updates");if(clocks!=null)for(int i=0;i<clocks.length();i++)applyClock(clocks.optJSONObject(i));
        JSONArray events=envelope.optJSONArray("world_events");if(events!=null)for(int i=0;i<events.length();i++){JSONObject e=events.optJSONObject(i);if(e==null)continue;db.addWorldEventChecked(e);}
        JSONArray resolutions=envelope.optJSONArray("event_resolutions");if(resolutions!=null)for(int i=0;i<resolutions.length();i++)applyEventResolution(resolutions.optJSONObject(i));
        JSONArray pending=envelope.optJSONArray("pending_resolutions");if(pending!=null)for(int i=0;i<pending.length();i++)applyPendingResolution(pending.optJSONObject(i));
        JSONArray journals=envelope.optJSONArray("journal_updates");if(journals!=null)for(int i=0;i<journals.length();i++){JSONObject j=journals.optJSONObject(i);if(j!=null&&j.optString("body").trim().length()>0){db.addJournalChecked(j.optString("title","Registro do Mestre"),j.optString("body"),j.optString("kind","Mestre"));}}
        JSONArray maps=envelope.optJSONArray("map_updates");if(maps!=null)for(int i=0;i<maps.length();i++)db.applyMapDirectorUpdateChecked(maps.optJSONObject(i));
        JSONArray tactical=envelope.optJSONArray("tactical_updates");if(tactical!=null&&tactical.length()>0){
            String sceneCommitId=envelope.optString("_scene_commit_id","").trim();
            if(sceneCommitId.length()<16)throw new IllegalStateException("Movimento narrado sem vínculo com o commit da cena.");
            for(int i=0;i<tactical.length();i++){
                JSONObject update=tactical.optJSONObject(i);
                if(update==null)throw new IllegalStateException("Atualização tática sem estrutura.");
                NarratedTacticalMovementExecutor.executeChecked(db,update,sceneCommitId,i);
            }
        }
    }

    private void applyEventResolution(JSONObject r) throws Exception{
        if(r==null)return;long eventId=r.optLong("id",0),transactionId=r.optLong("transaction_id",0);String key=r.optString("resolution_key").trim(),status=r.optString("status","resolved");
        JSONObject pending=db.getPendingTransaction(transactionId);JSONObject payload=parseObject(pending.optString("payload_json","{}"));
        String pendingKey=pending.optString("effect_key",payload.optString("_effect_key")).trim();
        if(eventId<=0||transactionId<=0||key.length()==0||pending.length()==0||!"pending".equalsIgnoreCase(pending.optString("status"))||payload.optLong("_world_event_id",0)!=eventId||!key.equals(pendingKey)||!key.equals(payload.optString("_effect_key")))throw new IllegalStateException("Correlação de evento pendente inválida.");
        if("resolved".equalsIgnoreCase(status))db.recordEffectReceipt(key,"master_event_review");
        db.resolveWorldEventAfterReview(eventId,status,r.optString("note"));
        db.resolvePendingEffectChecked(transactionId,"resolved".equalsIgnoreCase(status)?"applied":"cancelled");
    }

    private void applyPendingResolution(JSONObject r) throws Exception{
        if(r==null)return;long id=r.optLong("transaction_id",0);String key=r.optString("resolution_key").trim(),status=r.optString("status","applied");
        JSONObject pending=db.getPendingTransaction(id);JSONObject payload=parseObject(pending.optString("payload_json","{}"));
        if(id<=0||key.length()==0||pending.length()==0||!"pending".equalsIgnoreCase(pending.optString("status"))||!key.equals(pending.optString("effect_key",payload.optString("_effect_key"))))throw new IllegalStateException("Correlação de pendência inválida.");
        if(payload.optLong("_world_event_id",0)>0)throw new IllegalStateException("Eventos devem usar event_resolutions.");
        if("cancelled".equalsIgnoreCase(status)){db.resolvePendingEffectChecked(id,"cancelled");return;}
        JSONObject post=payload.optJSONObject("_post_apply");
        if(post!=null&&post.length()>0){
            if(post.optLong("id",0)>0&&payload.optLong("faction_id",0)>0){db.applyFactionSimulationStepChecked(post);db.addJournalChecked("Movimento de facção — "+post.optString("name"),post.optString("summary"),"Mundo Vivo");}
            else if(payload.optString("npc_name").trim().length()>0){JSONObject state=db.getNpcCognitiveState(payload.optString("npc_name"));state.put("name",payload.optString("npc_name"));state.put("urgency",post.optInt("urgency_after",state.optInt("urgency")));state.put("current_location",post.optString("location",state.optString("current_location")));state.put("last_interaction_summary",post.optString("summary"));state.put("last_action_at",System.currentTimeMillis());state.put("last_action_day",post.optInt("last_action_day"));db.upsertNpcCognitiveStateChecked(state);db.addJournalChecked("Ação fora de cena — "+payload.optString("npc_name"),post.optString("summary"),"Mundo Vivo");}
        }
        db.recordEffectReceipt(key,"master_pending_review");db.resolvePendingEffectChecked(id,"applied");
    }

    private void applyQuest(JSONObject q)throws Exception{db.applyQuestUpdateChecked(q);}
    private void applyClock(JSONObject c)throws Exception{db.applyWorldClockUpdateChecked(c);}
    private void storeValidationState(JSONObject envelope,Plan plan) throws Exception{JSONObject runtime=db.getMasterRuntimeState();runtime.put("last_action_fingerprint",plan.actionHash);runtime.put("last_context_fingerprint",plan.stateHash);runtime.put("last_response_fingerprint",hash(envelope==null?"":envelope.optString("narration")));runtime.put("last_cognitive_plan_json",plan.cognitivePlan.toString());JSONObject report=envelope==null?null:envelope.optJSONObject("_master_validation");runtime.put("validation_warnings_json",report==null?"[]":jsonArrayText(report.opt("warnings")));JSONArray threads=new JSONArray();String threadFocus=plan==null?"":plan.userPrompt;JSONArray threadQuests=WorldContextSelector.select(db.listQuests("active",1000),threadFocus,20,"title","objective","stage","stakes");for(int qi=0;qi<threadQuests.length();qi++){JSONObject q=threadQuests.optJSONObject(qi);if(q!=null)threads.put(new JSONObject().put("type","quest").put("id",q.optLong("id")).put("title",q.optString("title")).put("status",q.optString("status")).put("objective",q.optString("objective")));}JSONArray threadClocks=WorldContextSelector.select(db.listWorldClocks(true,1000),threadFocus,20,"name","consequence","owner_entity","trigger_condition");for(int ci=0;ci<threadClocks.length();ci++){JSONObject c=threadClocks.optJSONObject(ci);if(c!=null)threads.put(new JSONObject().put("type","clock").put("id",c.optLong("id")).put("name",c.optString("name")).put("current",c.optInt("current_value")).put("max",c.optInt("max_value")).put("consequence",c.optString("consequence")));}JSONObject scene=db.getSceneState();JSONArray pending=parseArray(scene.optString("pending_consequences_json","[]"));for(int i=0;i<pending.length()&&i<20;i++)threads.put(new JSONObject().put("type","pending_consequence").put("content",pending.opt(i)));runtime.put("unresolved_threads_json",threads.toString());db.saveMasterRuntimeStateChecked(runtime);}

    private static JSONObject compactScene(JSONObject scene){return compact(scene,new String[]{"day_number","location","hex_id","time_of_day","weather","lighting","danger","objective","present_npcs_json","threats_json","clues_json","pending_consequences_json"});}
    private static JSONObject compact(JSONObject source,String[] keys){JSONObject out=new JSONObject();if(source==null)return out;for(String key:keys)if(source.has(key))try{out.put(key,source.opt(key));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha tolerada; a execução continuou.",ignored);}return out;}
    private static JSONArray first(List<JSONObject> rows,int max){JSONArray out=new JSONArray();if(rows!=null)for(int i=0;i<rows.size()&&i<max;i++)out.put(rows.get(i));return out;}
    private static JSONObject compactAudio(JSONObject source){return compact(source,new String[]{"audio_channels","environment","weather","location","time_of_day","current_ambience","current_music","catalog_sounds"});}
    private static String tail(String value,int max){String s=value==null?"":value;if(s.length()<=max)return s;return "…"+s.substring(s.length()-Math.max(1,max-1));}
    /** Converte a projeção pública do Atlas no formato estrito CampaignState.sites. */
    private static JSONObject barbaraSites(JSONObject atlas){
        JSONObject sites=new JSONObject();if(atlas==null)return sites;
        try{
            JSONObject hex=atlas.optJSONObject("hex");if(hex!=null){String id=hex.optString("hex_id","").trim();if(id.length()>0)sites.put("hex:"+id,new JSONObject(hex.toString()).put("site_kind","hex").put("public",true));}
            JSONArray regions=atlas.optJSONArray("regions");if(regions!=null)for(int i=0;i<regions.length();i++){JSONObject r=regions.optJSONObject(i);if(r==null)continue;String id=r.optString("id","").trim();if(id.length()==0)id="region:"+i;sites.put("region:"+id,new JSONObject(r.toString()).put("site_kind","region").put("public",true));}
            JSONArray pois=atlas.optJSONArray("pois");if(pois!=null)for(int i=0;i<pois.length();i++){JSONObject poi=pois.optJSONObject(i);if(poi==null)continue;String id=poi.optString("id","").trim();if(id.length()==0)continue;sites.put("poi:"+id,new JSONObject(poi.toString()).put("site_kind","poi").put("public",true));}
        }catch(Exception failure){AppLog.e("MasterOrchestrator","Falha ao adaptar sites públicos do Atlas para CampaignState.",failure);}
        return sites;
    }

    private static String fitContext(JSONObject root,int max){String raw=root.toString();if(raw.length()<=max)return raw;try{root.put("map_context",tail(root.optString("map_context"),500));root.put("combatants",firstJson(root.optJSONArray("combatants"),6));root.put("npcs",firstJson(root.optJSONArray("npcs"),4));root.put("relevant_memories",firstJson(root.optJSONArray("relevant_memories"),3));root.put("context_reduced",true);raw=root.toString();if(raw.length()<=max)return raw;compactGameBus(root);raw=root.toString();if(raw.length()<=max)return raw;root.remove("audio_compatibility");root.remove("map_context");raw=root.toString();if(raw.length()<=max)return raw;JSONObject lean=new JSONObject();for(String key:new String[]{"campaign","story","scene","party","control_contract","game_bus","rule_retrieval","cognitive_plan","rules_frame","knowledge","npcs","relevant_memories","world","living_world","combat","map","atlas_public","canonical_position","local_tick","environmental_director","grounding_envelope","barbara_host_contract","world_discovery","barbara","narrative_intent","fiction_frozen","spotlight_hint","authority"})if(root.has(key))lean.put(key,root.opt(key));lean.put("context_reduced",true);raw=lean.toString();if(raw.length()<=max)return raw;lean.put("npcs",firstJson(lean.optJSONArray("npcs"),3));lean.put("relevant_memories",firstJson(lean.optJSONArray("relevant_memories"),2));lean.remove("world");raw=lean.toString();if(raw.length()<=max)return raw;JSONObject emergency=new JSONObject();emergency.put("story",lean.optJSONObject("story"));emergency.put("scene",lean.optJSONObject("scene"));emergency.put("party",lean.optJSONObject("party"));emergency.put("control_contract",lean.optJSONObject("control_contract"));emergency.put("living_world",lean.optJSONObject("living_world"));emergency.put("atlas_public",lean.optJSONObject("atlas_public"));emergency.put("canonical_position",lean.optJSONObject("canonical_position"));emergency.put("cognitive_plan",lean.optJSONObject("cognitive_plan"));emergency.put("rules_frame",lean.optJSONObject("rules_frame"));emergency.put("rule_retrieval",compactRuleRetrieval(lean.optJSONObject("rule_retrieval")));emergency.put("knowledge",firstJson(lean.optJSONArray("knowledge"),8));emergency.put("npcs",firstJson(lean.optJSONArray("npcs"),2));emergency.put("local_tick",lean.optJSONObject("local_tick"));emergency.put("environmental_director",lean.optJSONObject("environmental_director"));emergency.put("grounding_envelope",lean.optJSONObject("grounding_envelope"));emergency.put("barbara_host_contract",lean.optJSONObject("barbara_host_contract"));emergency.put("world_discovery",lean.optJSONObject("world_discovery"));emergency.put("barbara",lean.optJSONObject("barbara"));emergency.put("narrative_intent",lean.optString("narrative_intent"));emergency.put("fiction_frozen",lean.optBoolean("fiction_frozen",false));emergency.put("authority",lean.optString("authority"));emergency.put("game_bus",criticalGameBus(lean.optJSONObject("game_bus"),false));emergency.put("context_reduced",true);raw=emergency.toString();if(raw.length()<=max)return raw;emergency.put("game_bus",criticalGameBus(lean.optJSONObject("game_bus"),true));emergency.put("knowledge",firstJson(lean.optJSONArray("knowledge"),3));emergency.put("npcs",firstJson(lean.optJSONArray("npcs"),1));return enforceContextLimit(emergency,max);}catch(Exception e){try{return new JSONObject().put("game_bus",new JSONObject().put("context_error",true)).put("context_reduced",true).toString();}catch(Exception ignored){return "{\"game_bus\":{\"context_error\":true},\"context_reduced\":true}";}}}
    private static JSONObject worldRuleRetrieval(JSONObject tick){
        if(tick==null)return new JSONObject();JSONObject simulation=tick.optJSONObject("living_world_simulation");if(simulation==null)return new JSONObject();JSONObject retrieval=simulation.optJSONObject("rule_retrieval");return retrieval==null?new JSONObject():retrieval;
    }

    /** Une a consulta da ação com as provas recuperadas para o avanço do Mundo Vivo. */
    private static JSONObject mergeRuleRetrieval(JSONObject primary,JSONObject world){
        JSONObject out=new JSONObject();try{out=new JSONObject(primary==null?"{}":primary.toString());}catch(Exception ignored){out=new JSONObject();}
        try{
            JSONArray evidence=out.optJSONArray("evidence");if(evidence==null)evidence=new JSONArray();
            JSONArray structured=out.optJSONArray("structured_rules");if(structured==null)structured=new JSONArray();
            java.util.HashSet<String> ids=new java.util.HashSet<String>();for(int i=0;i<evidence.length();i++){JSONObject e=evidence.optJSONObject(i);if(e!=null)ids.add(e.optString("rule_id",e.optString("book")+"|"+e.optInt("page")));}
            JSONArray worldEvidence=world==null?null:world.optJSONArray("evidence");if(worldEvidence!=null)for(int i=0;i<worldEvidence.length()&&evidence.length()<16;i++){JSONObject e=worldEvidence.optJSONObject(i);if(e==null)continue;String id=e.optString("rule_id",e.optString("book")+"|"+e.optInt("page"));if(ids.add(id))evidence.put(new JSONObject(e.toString()));}
            JSONArray worldStructured=world==null?null:world.optJSONArray("structured_rules");if(worldStructured!=null)for(int i=0;i<worldStructured.length()&&structured.length()<16;i++){JSONObject r=worldStructured.optJSONObject(i);if(r!=null)structured.put(new JSONObject(r.toString()));}
            boolean worldIncluded=world!=null&&world.length()>0;boolean primaryComplete=out.optBoolean("index_complete",true);boolean worldComplete=!worldIncluded||world.optBoolean("index_complete",true);boolean complete=primaryComplete&&worldComplete;
            int executable=0;for(int i=0;i<structured.length();i++){JSONObject rule=structured.optJSONObject(i);if(rule!=null&&rule.optBoolean("executable",false))executable++;}
            boolean sufficient=complete&&evidence.length()>0&&executable>0;
            out.put("evidence",evidence).put("structured_rules",structured).put("evidence_count",evidence.length()).put("executable_rule_count",executable).put("index_complete",complete).put("sufficient_for_mechanics",sufficient).put("world_simulation_included",worldIncluded);
            if(!complete)out.put("status","index_incomplete");else if(evidence.length()==0)out.put("status","no_relevant_evidence");else if(executable==0)out.put("status","evidence_not_executable");else out.put("status","ready");
        }catch(Exception failure){AppLog.w("MasterOrchestrator","Não foi possível unir as provas do Mundo Vivo; a busca da cena foi preservada.",failure);}
        return out;
    }

    private static JSONObject compactRuleRetrieval(JSONObject source){
        JSONObject out=new JSONObject();if(source==null)return out;
        try{
            for(String key:new String[]{"status","required","world","enabled_manuals","ready_manuals","index_complete","index_missing_manuals","mechanical_candidate","sufficient_for_mechanics","executable_rule_count","instruction","retrieval_version","state_focus","world_simulation_included","index_health"})if(source.has(key))out.put(key,source.opt(key));
            out.put("intents",firstJson(source.optJSONArray("intents"),18));
            out.put("queries",firstJson(source.optJSONArray("queries"),12));
            out.put("evidence",firstJson(source.optJSONArray("evidence"),10));
            out.put("structured_rules",firstJson(source.optJSONArray("structured_rules"),10));
            JSONObject byIntent=source.optJSONObject("rules_by_intent");if(byIntent!=null)out.put("rules_by_intent",byIntent);
        }catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao compactar regras recuperadas.",ignored);}
        return out;
    }

    private static void compactGameBus(JSONObject root){
        try{
            JSONObject bus=root.optJSONObject("game_bus");if(bus==null)return;
            bus.put("npcs",firstJson(bus.optJSONArray("npcs"),16));
            bus.put("knowledge_facts",firstJson(bus.optJSONArray("knowledge_facts"),20));
            bus.put("recent_journal",firstJson(bus.optJSONArray("recent_journal"),5));
            bus.put("recent_dice",firstJson(bus.optJSONArray("recent_dice"),6));
            bus.put("combatants",firstJson(bus.optJSONArray("combatants"),24));
            bus.put("world_events",firstJson(bus.optJSONArray("world_events"),12));
            bus.put("map_context",tail(bus.optString("map_context"),2400));
            bus.put("context_reduced",true);
        }catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao compactar barramento.",ignored);}
    }

    private static JSONObject criticalGameBus(JSONObject source,boolean minimum){
        JSONObject out=new JSONObject();if(source==null)return out;try{
            for(String key:new String[]{"world_profile","campaign","story","scene","combat_state","map","living_world","party_state","travel_rules_state","connection_manifest"})if(source.has(key))out.put(key,source.opt(key));
            out.put("active_heroes_full",compactHeroBus(source.optJSONArray("active_heroes_full"),minimum?3:8,minimum?700:1600));
            out.put("combatants",firstJson(source.optJSONArray("combatants"),minimum?8:18));
            out.put("tactical_tokens",firstJson(source.optJSONArray("tactical_tokens"),minimum?8:18));
            out.put("recent_journal",firstJson(source.optJSONArray("recent_journal"),minimum?2:4));
            out.put("recent_dice",firstJson(source.optJSONArray("recent_dice"),minimum?4:8));
            out.put("npcs",firstJson(source.optJSONArray("npcs"),minimum?5:12));
            out.put("npc_cognition",firstJson(source.optJSONArray("npc_cognition"),minimum?5:12));
            out.put("knowledge_facts",firstJson(source.optJSONArray("knowledge_facts"),minimum?6:14));
            out.put("quests",firstJson(source.optJSONArray("quests"),minimum?5:12));
            out.put("factions",firstJson(source.optJSONArray("factions"),minimum?5:12));
            out.put("world_clocks",firstJson(source.optJSONArray("world_clocks"),minimum?5:12));
            out.put("world_events",firstJson(source.optJSONArray("world_events"),minimum?5:12));
            out.put("pending_transactions",firstJson(source.optJSONArray("pending_transactions"),minimum?5:12));
            out.put("party_actions",firstJson(source.optJSONArray("party_actions"),minimum?4:10));
            out.put("manual_catalog",firstJson(source.optJSONArray("manual_catalog"),minimum?3:8));
            if(!minimum){out.put("map_context",tail(source.optString("map_context"),1200));out.put("audio",source.optJSONObject("audio"));}
            out.put("context_reduced",true);out.put("all_systems_preserved",true);
        }catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao preservar barramento crítico.",ignored);}return out;
    }
    private static JSONArray compactHeroBus(JSONArray heroes,int max,int stringLimit){
        JSONArray out=new JSONArray();if(heroes==null)return out;
        for(int i=0;i<heroes.length()&&out.length()<max;i++){
            JSONObject hero=heroes.optJSONObject(i);if(hero==null)continue;
            try{out.put(deepCompactObject(hero,0,stringLimit,12));}
            catch(Exception ignored){out.put(new JSONObject());}
        }
        return out;
    }

    private static String enforceContextLimit(JSONObject source,int max)throws Exception{
        if(source==null)return "{}";String raw=source.toString();if(raw.length()<=max)return raw;
        int[][] passes={{900,16},{520,10},{280,7},{150,4},{80,2}};
        for(int[] pass:passes){JSONObject compact=deepCompactObject(source,0,pass[0],pass[1]);compact.put("context_reduced",true);compact.put("context_limit",max);raw=compact.toString();if(raw.length()<=max)return raw;source=compact;}
        JSONObject finalContext=new JSONObject();
        for(String key:new String[]{"story","scene","party","cognitive_plan","rules_frame","rule_retrieval","living_world","local_tick","narrative_intent","fiction_frozen","authority"})if(source.has(key))finalContext.put(key,deepCompactValue(source.opt(key),0,96,2));
        JSONObject bus=source.optJSONObject("game_bus");finalContext.put("game_bus",deepCompactObject(criticalGameBus(bus,true),0,96,2));
        finalContext.put("connection_manifest",connectionManifest(bus));finalContext.put("context_reduced",true);finalContext.put("hard_limit_applied",true);
        raw=finalContext.toString();if(raw.length()<=max)return raw;
        JSONObject minimum=new JSONObject();minimum.put("scene",deepCompactValue(source.opt("scene"),0,72,1));minimum.put("rules_frame",deepCompactValue(source.opt("rules_frame"),0,72,1));minimum.put("local_tick",deepCompactValue(source.opt("local_tick"),0,72,1));minimum.put("connection_manifest",connectionManifest(bus));minimum.put("context_reduced",true);minimum.put("hard_limit_applied",true);raw=minimum.toString();
        if(raw.length()>max)throw new IllegalStateException("O contexto mínimo excede o limite seguro.");return raw;
    }

    private static JSONObject connectionManifest(JSONObject bus)throws Exception{
        JSONObject out=new JSONObject();if(bus==null)return out;
        for(String key:new String[]{"active_heroes_full","combatants","tactical_tokens","recent_journal","recent_dice","npcs","npc_cognition","knowledge_facts","quests","factions","world_clocks","world_events","pending_transactions","party_actions","manual_catalog"}){JSONArray a=bus.optJSONArray(key);out.put(key,a==null?0:a.length());}
        for(String key:new String[]{"campaign","story","scene","combat_state","map","living_world","party_state","travel_rules_state"})out.put(key,bus.optJSONObject(key)!=null);
        return out;
    }

    private static JSONObject deepCompactObject(JSONObject source,int depth,int stringLimit,int arrayLimit)throws Exception{
        JSONObject out=new JSONObject();if(source==null)return out;java.util.Iterator<String> keys=source.keys();
        while(keys.hasNext()){String key=keys.next();out.put(key,deepCompactValue(source.opt(key),depth+1,stringLimit,arrayLimit));}
        return out;
    }
    private static Object deepCompactValue(Object value,int depth,int stringLimit,int arrayLimit)throws Exception{
        if(value==null||value==JSONObject.NULL)return JSONObject.NULL;
        if(value instanceof String){String text=(String)value;return text.length()<=stringLimit?text:text.substring(0,Math.max(1,stringLimit-1))+"…";}
        if(depth>6)return String.valueOf(value).substring(0,Math.min(stringLimit,String.valueOf(value).length()));
        if(value instanceof JSONObject)return deepCompactObject((JSONObject)value,depth,stringLimit,arrayLimit);
        if(value instanceof JSONArray){JSONArray src=(JSONArray)value,out=new JSONArray();for(int i=0;i<src.length()&&i<arrayLimit;i++)out.put(deepCompactValue(src.opt(i),depth+1,stringLimit,arrayLimit));return out;}
        return value;
    }

    private static JSONArray firstJson(JSONArray rows,int max){JSONArray out=new JSONArray();if(rows!=null)for(int i=0;i<rows.length()&&i<max;i++)out.put(rows.opt(i));return out;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw==null?"[]":raw);}catch(Exception e){return new JSONArray();}}
    private static String jsonArrayText(Object value){if(value instanceof JSONArray)return value.toString();try{return new JSONArray(String.valueOf(value)).toString();}catch(Exception e){return "[]";}}
    private static String jsonObjectText(Object value){if(value instanceof JSONObject)return value.toString();try{return new JSONObject(String.valueOf(value)).toString();}catch(Exception e){return "{}";}}
    private static String safeError(Throwable failure){String message=failure==null?"":failure.getMessage();if(message==null||message.trim().length()==0)message=failure==null?"erro desconhecido":failure.getClass().getSimpleName();return tail(message.replaceAll("[\r\n]+"," "),240);}
    private static String normalize(String value){return value==null?"":value.replaceAll("\\s+"," ").trim().toLowerCase(java.util.Locale.ROOT);}
    private static String hash(String value){try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] bytes=d.digest((value==null?"":value).getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){return Integer.toHexString((value==null?"":value).hashCode());}}

    public JSONObject resolveStatblockTurnAttack(String attacker,String target,String action,int d20){return StatblockCombatFlowEngine.attack(attacker,target,action,d20);}
    public JSONObject resolveStatblockTurnAttackWithGeometry(String attacker,String target,String action,
        int firstD20,int secondD20,int width,int height,int levels,
        TacticalCreatureGeometryRules.Creature attackerVolume,TacticalCreatureGeometryRules.Creature targetVolume,
        boolean[][][] opaque,int[][][] cover,boolean[][][] occupied,String kind,int reach,int normal,int far,
        boolean targetLocationKnown,boolean targetVisible,int blindsightFeet){
        return StatblockCombatFlowEngine.attackWithGeometry(attacker,target,action,firstD20,secondD20,
            width,height,levels,attackerVolume,targetVolume,opaque,cover,occupied,kind,reach,normal,far,
            targetLocationKnown,targetVisible,blindsightFeet);
    }
    public JSONObject resolveStatblockTurnMultiattack(String attacker,String target,String[] actions,int[] d20s){return StatblockCombatFlowEngine.multiattack(attacker,target,actions,d20s);}
    public JSONObject resolveStatblockLegendaryAction(String attacker,String target,String action,int d20){
        JSONObject res=StatblockCombatFlowEngine.legendaryAction(attacker,target,action,d20);
        if(db!=null&&!res.optBoolean("blocked",false)){
            return db.recordStatblockLegendaryTransaction(attacker,target,action,res);
        }
        return res;
    }
    public JSONObject resolveStatblockLegendaryAction(String attacker,String target,String action,int firstD20,int secondD20){
        JSONObject res=StatblockCombatFlowEngine.legendaryAction(attacker,target,action,firstD20,secondD20);
        if(db!=null&&!res.optBoolean("blocked",false)){
            return db.recordStatblockLegendaryTransaction(attacker,target,action,res);
        }
        return res;
    }
    public JSONObject spendStatblockLegendary(String token,int cost){return StatblockTurnEngine.spendLegendary(token,cost);}
    public JSONObject installRun139VoloHandbook(){JSONObject o=new JSONObject();try{return o.put("racial_feats",VoloRacialFeatRuntimeEngine.install()).put("species_phase2",VoloSubraceRun139Engine.install()).put("legacy_species_phase1",VoloSubraceHandbookEngine.install()).put("book_complete",false).put("auto_rolls",false);}catch(Exception e){return o;}}
    public JSONObject installRun141CharacterOptions(){JSONObject o=new JSONObject();try{return o.put("species_feats_legacy_runeterra",Run141SpeciesFeatRuntimeEngine.install()).put("run141_compact",Run141CompactCharacterOptionEngine.install()).put("forgotten_secrets_run143",Run143ForgottenSecretsEngine.install()).put("runeterra_atirador_base",RuneterraClassRuntimeEngine.install()).put("run148_reconciled",true).put("auto_rolls",false);}catch(Exception e){return o;}}
    public JSONObject installRun148Reconciliation(){JSONObject o=new JSONObject();try{return o.put("run141_compact",Run141CompactCharacterOptionEngine.install()).put("run143_forgotten_secrets",Run143ForgottenSecretsEngine.install()).put("arcana_unleashed_run147",ArcanaUnleashedRun147EvolvingItemsEngine.install()).put("auto_rolls",false).put("reconciled",true);}catch(Exception e){return o;}}
    public JSONObject installRun140VoloRemainder(){JSONObject o=new JSONObject();try{return o.put("species_remainder",VoloSubraceRun140Engine.install()).put("grung_delta_pending",VoloSubraceRun140Engine.pendingSourceDependencies()).put("book_complete",false).put("auto_rolls",false);}catch(Exception e){return o;}}

    public NarrativeContentEngine getNarrativeEngine(){return narrativeEngine;}
}
