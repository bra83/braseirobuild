package com.braseiro.pathfinder2e;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

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
        public final boolean usedPartyQueue;
        public final boolean economy;
        public final boolean narrative;
        public final boolean nonAdvancing;
        public final String narrativeIntent;
        public final String inputChannel;
        public final String actionHash;
        public final String stateHash;
        public final String cacheKey;
        public final boolean contextReady;
        public final String contextError;
        public final String barbaraStoryOccasion;
        public final String barbaraLocation;
        public final String barbaraImprint;
        private boolean accepted;
        private boolean committed;
        private boolean fromCache;
        Plan(String prompt,String context,JSONObject tick,JSONObject cognition,JSONObject npcPreview,JSONObject environmentPreview,JSONObject retrieval,boolean queued,boolean eco,boolean narr,boolean frozen,String intent,String channel,String aHash,String sHash,String key,boolean ready,String error,String bOccasion,String bLocation,String bImprint){userPrompt=prompt;compactContext=context;localTick=tick;cognitivePlan=cognition;npcTick=npcPreview;environmentalPlan=environmentPreview;ruleRetrieval=retrieval==null?new JSONObject():retrieval;usedPartyQueue=queued;economy=eco;narrative=narr;nonAdvancing=frozen;narrativeIntent=intent==null?"":intent;inputChannel=channel==null?"":channel;actionHash=aHash;stateHash=sHash;cacheKey=key;contextReady=ready;contextError=error==null?"":error;barbaraStoryOccasion=bOccasion==null?"continuation":bOccasion;barbaraLocation=bLocation==null?"":bLocation;barbaraImprint=bImprint==null?"":bImprint;}
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

    public MasterOrchestrator(AppDb database,PartyDirector partyDirector,WorldStateEngine worldEngine,AiQuotaGovernor governor){
        this(database,partyDirector,worldEngine,governor,null);
    }
    public MasterOrchestrator(AppDb database,PartyDirector partyDirector,WorldStateEngine worldEngine,AiQuotaGovernor governor,Context context){
        db=database;party=partyDirector;world=worldEngine;quota=governor;firewall=new KnowledgeFirewall(db);rules=new MasterRulesArbiter(db);npcAgency=new NpcAgencyEngine(db,context==null?null:new NpcPersonalityCatalog(context));guilds=new GuildEngine(db);memory=new MasterMemoryEngine(db);cognition=new MasterCognitiveEngine(db,party,rules);validator=new MasterResponseValidator(db,firewall,rules,npcAgency);environmentalDirector=new EnvironmentalDirector(db);gameContext=new MasterGameContext(db);barbara=context==null?null:new BarbaraSupervisor(context);
    }

    public Plan prepare(String mode,String rawAction,String mapContext,JSONObject audio,JSONObject ruleRetrieval){return prepare(mode,rawAction,mapContext,audio,ruleRetrieval,null);}

    public Plan prepare(String mode,String rawAction,String mapContext,JSONObject audio,JSONObject ruleRetrieval,String inputChannel){
        if(inputChannel!=null&&!"PLAYER_ACTION".equals(inputChannel)&&!"GM_HELP".equals(inputChannel))throw new IllegalArgumentException("invalid_input_channel");
        boolean narrative="Narrador".equals(mode);boolean explicitAction="PLAYER_ACTION".equals(inputChannel);boolean explicitHelp="GM_HELP".equals(inputChannel);BraseiroNarrativeDirector.Intent routedIntent=BraseiroNarrativeDirector.classify(rawAction);boolean nonAdvancing=narrative&&(explicitHelp||(!explicitAction&&BraseiroNarrativeDirector.isNonAdvancing(routedIntent)));boolean queued=narrative&&!nonAdvancing&&party.hasQueuedActions();String action=queued?party.buildActionBatch(rawAction):(rawAction==null?"":rawAction.trim());boolean opening=RuleRetrievalEngine.isNarrativeOpeningRequest(action);JSONObject tick=narrative&&!nonAdvancing?world.preview(action):new JSONObject();
        JSONObject effectiveRuleRetrieval=opening?openingNarrativeRuleIsolation():mergeRuleRetrieval(ruleRetrieval,worldRuleRetrieval(tick));
        rules.setActiveRuleRetrieval(effectiveRuleRetrieval);
        JSONObject environmental=narrative&&!nonAdvancing?environmentalDirector.preview(action):new JSONObject();JSONObject environmentalPrompt=narrative&&!nonAdvancing?environmentalDirector.promptView(environmental):new JSONObject();try{if(narrative&&!nonAdvancing)tick.put("environmental_director",environmentalPrompt);}catch(Exception failure){AppLog.e("MasterOrchestrator","Falha ao anexar o plano ambiental.",failure);}JSONObject cognitive=narrative&&!nonAdvancing?cognition.plan(action,tick):new JSONObject();JSONObject npcTick=narrative&&!nonAdvancing?npcAgency.previewOffscreenTick(tick):new JSONObject();JSONObject root=new JSONObject();boolean contextReady=true;String contextError="";String barbaraLocation="",barbaraImprint="",barbaraOccasion="continuation";
        try{
            root.put("campaign",compact(db.getCampaign(),new String[]{"name","rules_profile","tone","feats","multiclass","flanking","notes"}));
            JSONObject bus=gameContext.narratorSnapshot(mapContext,audio);bus.put("rule_retrieval",compactRuleRetrieval(effectiveRuleRetrieval));root.put("game_bus",bus);
            root.put("rule_retrieval",compactRuleRetrieval(effectiveRuleRetrieval));
            JSONObject storyState=db.getStoryState();JSONObject story=compact(storyState,new String[]{"pending_test_json","last_action","updated_at"});story.put("last_narration",tail(storyState.optString("last_narration"),quota.economyMode()?850:1400));story.put("avoid_options",firstJson(parseArray(storyState.optString("options_json","[]")),4));root.put("story",story);
            if(narrative){root.put("narrative_intent",explicitAction?"player_action":(explicitHelp?"gm_help":BraseiroNarrativeDirector.intentLabel(routedIntent)));root.put("fiction_frozen",nonAdvancing);root.put("input_channel",inputChannel==null?JSONObject.NULL:inputChannel);}
            JSONObject compactSceneState=compactScene(db.getSceneState());JSONObject compactPartyState=party.compactStateForPrompt(quota.economyMode()?3300:4400);JSONObject compactWorldState=compactWorld();
            JSONObject liveMemory=new JSONObject();JSONObject partyBlock=compactPartyState.optJSONObject("party");JSONObject sessionPrefs=partyBlock==null?null:partyBlock.optJSONObject("session_preferences");JSONObject savedNarrativeMemory=sessionPrefs==null?null:sessionPrefs.optJSONObject("braseiro_narrative_memory");if(savedNarrativeMemory!=null)liveMemory=savedNarrativeMemory;
            CampaignPoiService campaignPois=new CampaignPoiService(db);JSONObject livingWorld=new JSONObject(new LivingWorldEngine(db).compactContext());int upcomingBeat=Math.max(1,db.getMasterRuntimeState().optInt("beat_no",0)+1);JSONObject worldBeat=WorldBeatDirector.preview(action,compactSceneState,compactWorldState,livingWorld,environmentalPrompt,npcTick,liveMemory,upcomingBeat,opening,nonAdvancing);JSONObject narrativePacing=NarrativePacingDirector.plan(action,cognitive.optString("scene_mode",db.getMasterRuntimeState().optString("scene_mode","exploracao")),compactSceneState,upcomingBeat,quota.economyMode(),worldBeat,opening,nonAdvancing);
            root.put("campaign_pois_public",campaignPois.playerProjection());root.put("campaign_pois_private",campaignPois.masterProjection());root.put("scene",compactSceneState);root.put("scene_entity_grounding",SceneEntityGrounding.promptPolicy(db,compactSceneState));root.put("party",compactPartyState);root.put("cognitive_plan",cognitive);root.put("rules_frame",rules.rulesFrame(action));root.put("knowledge",firewall.promptView(action));root.put("npcs",npcAgency.promptView(action,quota.economyMode()?5:8));root.put("guilds",guilds.promptView(quota.economyMode()?6:12));root.put("relevant_memories",memory.relevant(action+" "+mapContext,quota.economyMode()?3:6));root.put("world",compactWorldState);root.put("live_table",LiveTableNarrativeDirector.promptState(root.optJSONObject("campaign"),compactSceneState,compactPartyState,compactWorldState,liveMemory,action));root.put("living_world",livingWorld);root.put("world_beat",worldBeat);root.put("narrative_pacing",narrativePacing);root.put("kingmaker",bus.optJSONObject("kingmaker"));root.put("combat",db.getCombatState());root.put("combatants",first(db.listCombatants(),quota.economyMode()?8:14));JSONObject canonicalMap=db.getMapState();root.put("map",compact(canonicalMap,new String[]{"map_package_id","current_hex_id","destination_hex_id","route_json","fog_enabled"}));JSONObject canonicalPosition=new JSONObject();String canonicalPackage=canonicalMap.optString("map_package_id","").trim(),canonicalHex=canonicalMap.optString("current_hex_id","").trim(),canonicalSceneLocation=compactSceneState.optString("location","").trim();if(canonicalPackage.length()>0&&canonicalHex.length()>0)canonicalPosition.put("source","atlas").put("map_package_id",canonicalPackage).put("hex_id",canonicalHex);else if(canonicalSceneLocation.length()>0)canonicalPosition.put("source","scene").put("location",canonicalSceneLocation);else canonicalPosition.put("source","unresolved");root.put("canonical_position",canonicalPosition);root.put("map_context",tail(mapContext,quota.economyMode()?900:1700));root.put("local_tick",tick);root.put("environmental_director",environmentalPrompt);root.put("npc_offscreen_preview",npcTick);root.put("spotlight_hint",party.nextSpotlightHint());root.put("authority","O aplicativo é a autoridade mecânica e persistente. O Gemini não aplica mudanças diretamente.");
            // O áudio permanece compatível, mas não participa do núcleo cognitivo desta versão.
            root.put("audio_compatibility",compactAudio(audio));
            JSONObject bScene=root.optJSONObject("scene");JSONObject bMap=root.optJSONObject("map");JSONObject bCanonical=root.optJSONObject("canonical_position");
            if(bCanonical!=null&&"atlas".equals(bCanonical.optString("source")))barbaraLocation=bCanonical.optString("map_package_id")+"#"+bCanonical.optString("hex_id");else barbaraLocation=bCanonical==null?"":bCanonical.optString("location",bScene==null?"":bScene.optString("location",bMap==null?"":bMap.optString("current_hex_id","")));
            JSONObject bImprint=new JSONObject().put("scene",bScene==null?new JSONObject():bScene).put("world",root.optJSONObject("world")).put("living_world",root.optJSONObject("living_world")).put("kingmaker",root.optJSONObject("kingmaker")).put("environment",root.optJSONObject("environmental_director")).put("campaign_pois_public",root.optJSONObject("campaign_pois_public")).put("campaign_pois_private",root.optJSONObject("campaign_pois_private"));
            barbaraImprint=hash(bImprint.toString());
            JSONObject bPlan=barbara==null?new JSONObject():barbara.plan(action,"normal",narrative,nonAdvancing,barbaraLocation,barbaraImprint,inputChannel);
            barbaraOccasion=bPlan.optString("story_occasion","continuation");root.put("barbara",bPlan);
        }catch(Exception failure){contextReady=false;contextError="Falha ao montar o barramento operacional: "+safeError(failure);AppLog.e("MasterOrchestrator",contextError,failure);root=new JSONObject();try{root.put("context_error",true).put("context_error_message",contextError).put("authority","Não gerar narrativa nem mutações: o estado operacional não pôde ser montado.");}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao registrar erro de contexto.",ignored);}}
        String context=fitContext(root,quota.economyMode()?22000:34000);if(context.contains("\"context_error\":true")){contextReady=false;if(contextError.length()==0)contextError="O contexto operacional excedeu o limite seguro ou não pôde ser compactado.";}String actionHash=hash(normalize(action));JSONObject freshStory=db.getStoryState();String storySignature=freshStory.optLong("updated_at",0)+"|"+freshStory.optString("last_action","")+"|"+tail(freshStory.optString("last_narration",""),1800);String stateHash=hash(context+"|story_signature="+hash(storySignature));String cacheKey=hash(mode+"|"+(inputChannel==null?"legacy":inputChannel)+"|"+actionHash+"|"+stateHash);
        String prompt=action+"\n\n"+SceneEntityGrounding.promptDirective(db,db.getSceneState())+"Use game_bus como estado operacional completo, live_table como direção de fluxo entre protagonistas/threads e cognitive_plan/environmental_director como planos locais. live_table não autoriza ação, emoção ou conhecimento fora do personagem; apenas organiza foco, cortes, convergência, promessas e pressão causal. O campo rule_retrieval registra a política local de regras desta interação: pode representar uma consulta nova, uma prova reutilizada do cache da cena ou not_required_fiction quando nenhuma busca mecânica era necessária. Para qualquer regra, procedimento ou estatística mecânica, use somente rule_retrieval.evidence/structured_rules quando houver prova; nunca cite página fora desse pacote. Se sufficient_for_mechanics=false, continue apenas a ficção e deixe mutações mecânicas vazias. Não force busca ou explicação de regra em uma batida puramente ficcional. Use living_world somente quando living_world.enabled for verdadeiro; quando falso, não avance agenda, facções, NPCs ou atividades. Envelhecimento, família, relacionamentos, trabalho, caça, estudo e trupe mantêm continuidade narrativa, mas jamais concedem XP, ouro, itens, atributos ou recursos por conta própria. O environmental_director antecipa sinais de clima, terreno, rota e aproximação de locais sem revelar além do perceptível. Quando world_beat.active=true, faça o mundo mover-se por uma batida perceptível independente da ação do jogador, derivada somente dos sinais persistidos fornecidos; world_beat.private_causality nunca deve ser revelado diretamente. Quando false, não invente movimento distante por obrigação. narrative_pacing define a função e uma faixa flexível de tamanho: varie de fato o comprimento entre cenas e nunca trate min_words/max_words como quota. Respeite rules_boundary: SIMULACAO_NARRATIVA nunca altera ficha; DECISAO_DO_MESTRE nunca é aplicada como fato; REGRA_OPCIONAL exige habilitação; somente RAW confirmado pelo aplicativo produz consequência mecânica. Toda mutação mecânica deve incluir mechanical_basis com source_type e evidence verificável. Citação de manual identifica o procedimento, mas hp_delta, itens, condições e recursos exigem rolagem ou cálculo local. Ao resolver pending_transactions, copie transaction_id e effect_key como resolution_key para event_resolutions ou pending_resolutions, e repita resolution_key na mutação mecânica correspondente. Para manual/regra, use mechanical_basis={source_type:\"manual\",rule_id,evidence,book,page,excerpt,calculation:{operation,roll_id?,roll_formula?,roll_total?,multiplier?,flat_modifier?,modifier_source?,divide_by?,rounding?,fixed_value?,computed_value,resource_deltas?}} e repita exatamente a mesma prova recuperada em sources=[{book,page,excerpt}]. Dados de rolagem precisam existir em game_bus.recent_dice; multiplicadores, modificadores e valores fixos precisam aparecer no procedimento compilado; modifier_source deve apontar para o atributo da ficha e divide_by/rounding só pode ser usado quando a passagem disser metade/terço/divisão. O aplicativo recalculará tudo e rejeitará divergências. Planos autônomos de facção devem reagir ao estado persistente e podem usar plan_steps_json com etapas {title,summary,location,resources,agenda,relationship_delta,next_plan,consequence_json}; consequence_json só pode conter mutações estruturais não mecânicas. Não recalcule regras, relógios, recursos ou ordem causal. Use game_bus.world_6 como camada de direção, nunca como roteiro obrigatório: campaign_director contém tensões emergentes e perguntas dramáticas sem final predeterminado; settlements descreve pressões narrativas locais; history e legacy contêm somente história canônica persistida. Rumores exigem firewall de conhecimento: truth_level, distortion, source_event_key e outros metadados internos são apenas do Mestre; um personagem só pode ouvir ou agir sobre um rumor quando ele estiver explicitamente entre os conhecimentos/rumores daquele personagem ou quando a cena o fizer chegar até ele. Nunca revele automaticamente a verdade objetiva por trás de um rumor. npc_voices é identidade de voz, não autoridade narrativa. tactical_generation fornece geometria visual jogável, mas não cria bônus, cobertura mecânica, dano ou regra sem o motor apropriado. Arcos emergentes podem mudar, desaparecer ou ser resolvidos conforme as ações dos jogadores; jamais force um desfecho para preservar um arco. Narre apenas o que os personagens podem perceber. Proponha mudanças em campos estruturados, que serão revisadas antes de gravar.";
        prompt += "\n\nKINGMAKER: quando kingmaker.enabled=true, use o bloco kingmaker como continuidade factual do Reino, território, liderança, assentamentos, turnos, eventos, exércitos, companheiros e acampamentos. Não invente RP, commodities, Unrest, Ruin, CD, bônus, dano de exército, influência ou resultado de atividade. A interface e KingmakerRulesEngine são a única autoridade de mutação; a IA apenas narra o estado confirmado e pode sugerir uma próxima ação sem gravá-la.\n\nPOIs DINÂMICOS: campaign_pois_public pode ser usado normalmente para coerência, retorno, viagem e Mundo Vivo. campaign_pois_private é conhecimento EXCLUSIVO do Mestre: nunca o revele, nomeie, indique ou transforme em pista sem uma descoberta ficcional válida; ele não existe no contexto público do jogador. Esses POIs são conteúdo da campanha, nunca geografia oficial de Golarion.\n\nMOTOR BARBARA 1.1.2 É POLÍTICA OBRIGATÓRIA: obedeça o bloco barbara do contexto. O jogador NÃO conhece automaticamente o estado atual do mundo. Em campaign_opening, first_arrival ou changed_return, conte primeiro uma cena completa e vivida; não substitua por briefing, resumo, lista de contexto ou escolhas. Clima, economia, guerras, política e cultura devem aparecer por sinais perceptíveis na ficção. As opções vêm depois da história. O Braseiro continua autoridade mecânica e persistente. input_channel é vinculante: PLAYER_ACTION continua a ficção mesmo contendo pergunta ou fala dirigida a NPC; GM_HELP congela tempo, posição, NPCs, relógios, memória e Mundo Vivo. canonical_position é a posição espacial autoritativa: quando houver Atlas configurado, o hex canônico vence qualquer inferência narrativa.";
        return new Plan(prompt,context,tick,cognitive,npcTick,environmental,effectiveRuleRetrieval,queued,quota.economyMode(),narrative,nonAdvancing,explicitAction?"PLAYER_ACTION":(explicitHelp?"GM_HELP":routedIntent.name()),inputChannel,actionHash,stateHash,cacheKey,contextReady,contextError,barbaraOccasion,barbaraLocation,barbaraImprint);
    }

    public JSONObject validateEnvelope(JSONObject envelope){return validateEnvelope(envelope,"");}
    public JSONObject validateEnvelope(JSONObject envelope,String action){
        JSONObject candidate=envelope;if(activePlanForValidation!=null&&activePlanForValidation.nonAdvancing)candidate=BraseiroNarrativeDirector.freezeEnvelope(candidate,action);
        JSONObject safe=validator.validate(candidate,action);
        safe=guilds.validateUpdates(safe);
        try{
            String bOccasion=activePlanForValidation==null?"continuation":activePlanForValidation.barbaraStoryOccasion;
            JSONObject bReport=barbara==null?new JSONObject().put("valid",true).put("bypassed",true):barbara.validate(safe,action,"normal",bOccasion,activePlanForValidation==null?null:activePlanForValidation.inputChannel);
            safe.put("_barbara_validation",bReport);
            if(!bReport.optBoolean("valid",true)){
                JSONObject master=safe.optJSONObject("_master_validation");if(master==null){master=new JSONObject();safe.put("_master_validation",master);}
                master.put("valid",false).put("fail_closed",true).put("barbara_code",bReport.optString("code","barbara_reject"));
                JSONArray warnings=master.optJSONArray("warnings");if(warnings==null){warnings=new JSONArray();master.put("warnings",warnings);}warnings.put("Motor Barbara: "+bReport.optString("code","violação narrativa"));
            }
        }catch(Exception bFailure){AppLog.w("MasterOrchestrator","Falha ao anexar validação Barbara; validações Java foram preservadas.",bFailure);}
        try{
            JSONObject report=safe.optJSONObject("_master_validation");JSONObject guildReport=safe.optJSONObject("_guild_validation");boolean valid=report!=null&&report.optBoolean("valid",false)&&(guildReport==null||guildReport.optBoolean("valid",false));if(!valid&&report!=null)report.put("valid",false).put("fail_closed",true);
            if(activePlanForValidation!=null&&valid){if(activePlanForValidation.nonAdvancing){safe=BraseiroNarrativeDirector.freezeEnvelope(safe,action);safe.put("_cognitive_plan",new JSONObject());}else{boolean deferred=safe.optBoolean("_deferred_until_test",false);safe.put("_local_tick",deferred?new JSONObject():activePlanForValidation.localTick);safe.put("_cognitive_plan",activePlanForValidation.cognitivePlan);safe.put("_npc_tick",deferred?new JSONObject():activePlanForValidation.npcTick);}}
            else{safe.put("_local_tick",new JSONObject());safe.put("_npc_tick",new JSONObject());}
        }catch(Exception failure){
            AppLog.e("MasterOrchestrator","Falha ao anexar o plano local; envelope bloqueado.",failure);
            try{safe.put("_local_tick",new JSONObject());safe.put("_npc_tick",new JSONObject());safe.put("_master_validation",new JSONObject().put("valid",false).put("fail_closed",true).put("error","Falha ao anexar plano local."));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao fechar envelope sem plano.",ignored);}
        }
        return safe;
    }

    private Plan activePlanForValidation;
    public void setActivePlanForValidation(Plan plan){activePlanForValidation=plan;rules.setActiveRuleRetrieval(plan==null?new JSONObject():plan.ruleRetrieval);}
    public boolean hasStructuredChanges(JSONObject envelope){return validator.hasStructuredChanges(envelope);}

    public synchronized void afterAccepted(Plan plan,String narration,int inputChars,int outputChars){afterAccepted(plan,narration,null,inputChars,outputChars);}
    public synchronized void afterAccepted(Plan plan,String narration,JSONObject envelope,int inputChars,int outputChars){
        if(plan==null)return;if(plan.fromCache){if(plan.narrative&&!plan.nonAdvancing)party.updateSpotlightFromNarration(narration);if(!plan.accepted&&plan.usedPartyQueue)party.markQueuedResolved();plan.accepted=true;return;}boolean offline=envelope!=null&&envelope.optJSONObject("_offline_master")!=null;if(!offline)quota.recordCall(inputChars,outputChars);if(plan.narrative&&!plan.nonAdvancing){party.updateSpotlightFromNarration(narration);if(barbara!=null)barbara.markExperienced(plan.barbaraLocation,plan.barbaraImprint);}if(!plan.accepted){if(plan.usedPartyQueue)party.markQueuedResolved();if(!plan.nonAdvancing)memory.record(plan.userPrompt,narration,plan.cognitivePlan,envelope);plan.accepted=true;}
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

    public String quotaStatus(){return quota.statusPtBr();}
    public String marker(){return quota.systemMarker();}
    public boolean canCall(){return quota.canCall();}
    public boolean economyMode(){return quota.economyMode();}
    public JSONObject quotaDiagnostic(){return quota.diagnostic();}
    public JSONObject diagnostic(){JSONObject out=new JSONObject();try{out.put("cognition",cognition.diagnostic());out.put("knowledge_firewall",firewall.diagnostic());out.put("npc_agency",npcAgency.diagnostic());out.put("guilds",guilds.diagnostic());out.put("memory",memory.diagnostic());out.put("environmental_director",environmentalDirector.promptView(environmentalDirector.preview("")));out.put("quota",quota.diagnostic());out.put("barbara",barbara==null?new JSONObject().put("available",false):barbara.diagnostic());out.put("connection_audit",gameContext.auditConnections());out.put("complete",gameContext.auditConnections().optBoolean("complete",false));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha tolerada; a execução continuou.",ignored);}return out;}

    private JSONObject compactWorld(){JSONObject out=new JSONObject();try{out.put("quests",new JSONArray(db.listQuests("active",12)));JSONArray factions=new JSONArray();for(JSONObject f:db.listFactions(10)){JSONObject x=compact(f,new String[]{"id","name","agenda","relationship","location","active"});factions.put(x);}out.put("factions",factions);out.put("guilds",guilds.promptView(12));out.put("clocks",new JSONArray(db.listWorldClocks(true,14)));out.put("events_due_or_upcoming",new JSONArray(db.listUpcomingWorldEvents(10)));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha tolerada; a execução continuou.",ignored);}return out;}

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
        JSONArray tactical=envelope.optJSONArray("tactical_updates");if(tactical!=null)for(int i=0;i<tactical.length();i++)db.applyTacticalDirectorUpdateChecked(tactical.optJSONObject(i));
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
    private void storeValidationState(JSONObject envelope,Plan plan) throws Exception{JSONObject runtime=db.getMasterRuntimeState();runtime.put("last_action_fingerprint",plan.actionHash);runtime.put("last_context_fingerprint",plan.stateHash);runtime.put("last_response_fingerprint",hash(envelope==null?"":envelope.optString("narration")));runtime.put("last_cognitive_plan_json",plan.cognitivePlan.toString());JSONObject report=envelope==null?null:envelope.optJSONObject("_master_validation");runtime.put("validation_warnings_json",report==null?"[]":jsonArrayText(report.opt("warnings")));JSONArray threads=new JSONArray();for(JSONObject q:db.listQuests("active",20))threads.put(new JSONObject().put("type","quest").put("id",q.optLong("id")).put("title",q.optString("title")).put("status",q.optString("status")).put("objective",q.optString("objective")));for(JSONObject c:db.listWorldClocks(true,20))threads.put(new JSONObject().put("type","clock").put("id",c.optLong("id")).put("name",c.optString("name")).put("current",c.optInt("current_value")).put("max",c.optInt("max_value")).put("consequence",c.optString("consequence")));JSONObject scene=db.getSceneState();JSONArray pending=parseArray(scene.optString("pending_consequences_json","[]"));for(int i=0;i<pending.length()&&i<20;i++)threads.put(new JSONObject().put("type","pending_consequence").put("content",pending.opt(i)));runtime.put("unresolved_threads_json",threads.toString());db.saveMasterRuntimeStateChecked(runtime);}

    private static JSONObject compactScene(JSONObject scene){return compact(scene,new String[]{"day_number","location","hex_id","time_of_day","weather","lighting","danger","objective","present_npcs_json","threats_json","clues_json","pending_consequences_json"});}
    private static JSONObject compact(JSONObject source,String[] keys){JSONObject out=new JSONObject();if(source==null)return out;for(String key:keys)if(source.has(key))try{out.put(key,source.opt(key));}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha tolerada; a execução continuou.",ignored);}return out;}
    private static JSONArray first(List<JSONObject> rows,int max){JSONArray out=new JSONArray();if(rows!=null)for(int i=0;i<rows.size()&&i<max;i++)out.put(rows.get(i));return out;}
    private static JSONObject compactAudio(JSONObject source){return compact(source,new String[]{"audio_channels","environment","weather","location","time_of_day","current_ambience","current_music","catalog_sounds"});}
    private static String tail(String value,int max){String s=value==null?"":value;if(s.length()<=max)return s;return "…"+s.substring(s.length()-Math.max(1,max-1));}
    private static String fitContext(JSONObject root,int max){String raw=root.toString();if(raw.length()<=max)return raw;try{root.put("map_context",tail(root.optString("map_context"),500));root.put("combatants",firstJson(root.optJSONArray("combatants"),6));root.put("npcs",firstJson(root.optJSONArray("npcs"),4));root.put("relevant_memories",firstJson(root.optJSONArray("relevant_memories"),3));root.put("context_reduced",true);raw=root.toString();if(raw.length()<=max)return raw;compactGameBus(root);raw=root.toString();if(raw.length()<=max)return raw;root.remove("audio_compatibility");root.remove("map_context");raw=root.toString();if(raw.length()<=max)return raw;JSONObject lean=new JSONObject();for(String key:new String[]{"campaign","story","scene","party","game_bus","rule_retrieval","cognitive_plan","rules_frame","knowledge","npcs","relevant_memories","world","living_world","world_beat","narrative_pacing","kingmaker","combat","map","local_tick","narrative_intent","input_channel","fiction_frozen","canonical_position","campaign_pois_public","campaign_pois_private","scene_entity_grounding","spotlight_hint","authority"})if(root.has(key))lean.put(key,root.opt(key));lean.put("context_reduced",true);raw=lean.toString();if(raw.length()<=max)return raw;lean.put("npcs",firstJson(lean.optJSONArray("npcs"),3));lean.put("relevant_memories",firstJson(lean.optJSONArray("relevant_memories"),2));lean.remove("world");raw=lean.toString();if(raw.length()<=max)return raw;JSONObject emergency=new JSONObject();emergency.put("story",lean.optJSONObject("story"));emergency.put("scene",lean.optJSONObject("scene"));emergency.put("party",lean.optJSONObject("party"));emergency.put("living_world",lean.optJSONObject("living_world"));emergency.put("world_beat",lean.optJSONObject("world_beat"));emergency.put("narrative_pacing",lean.optJSONObject("narrative_pacing"));emergency.put("kingmaker",lean.optJSONObject("kingmaker"));emergency.put("cognitive_plan",lean.optJSONObject("cognitive_plan"));emergency.put("rules_frame",lean.optJSONObject("rules_frame"));emergency.put("rule_retrieval",compactRuleRetrieval(lean.optJSONObject("rule_retrieval")));emergency.put("knowledge",firstJson(lean.optJSONArray("knowledge"),8));emergency.put("npcs",firstJson(lean.optJSONArray("npcs"),2));emergency.put("local_tick",lean.optJSONObject("local_tick"));emergency.put("narrative_intent",lean.optString("narrative_intent"));emergency.put("input_channel",lean.opt("input_channel"));emergency.put("canonical_position",lean.optJSONObject("canonical_position"));emergency.put("fiction_frozen",lean.optBoolean("fiction_frozen",false));emergency.put("authority",lean.optString("authority"));emergency.put("game_bus",criticalGameBus(lean.optJSONObject("game_bus"),false));emergency.put("context_reduced",true);raw=emergency.toString();if(raw.length()<=max)return raw;emergency.put("game_bus",criticalGameBus(lean.optJSONObject("game_bus"),true));emergency.put("knowledge",firstJson(lean.optJSONArray("knowledge"),3));emergency.put("npcs",firstJson(lean.optJSONArray("npcs"),1));return enforceContextLimit(emergency,max);}catch(Exception e){try{return new JSONObject().put("game_bus",new JSONObject().put("context_error",true)).put("context_reduced",true).toString();}catch(Exception ignored){return "{\"game_bus\":{\"context_error\":true},\"context_reduced\":true}";}}}
    private static JSONObject openingNarrativeRuleIsolation(){
        JSONObject out=new JSONObject();try{out.put("required",false).put("status","opening_narrative_direct").put("world","pathfinder_2e").put("mechanical_candidate",false).put("opening_narrative",true).put("opening_no_rag",true).put("evidence",new JSONArray()).put("structured_rules",new JSONArray()).put("evidence_count",0).put("sufficient_for_mechanics",false).put("mechanics_blocked",true).put("instruction","A primeira cena é narrativa; regras serão recuperadas somente quando uma ação real exigir mecânica.");}catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao isolar regras da abertura.",ignored);}return out;
    }

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
            String primaryStatus=out.optString("status","");String worldStatus=world==null?"":world.optString("status","");
            boolean worldIncluded=world!=null&&world.length()>0&&!"not_required_fiction".equals(worldStatus)&&(world.optBoolean("required",false)||(world.optJSONArray("evidence")!=null&&world.optJSONArray("evidence").length()>0));
            boolean primaryComplete=out.optBoolean("index_complete",true);boolean worldComplete=!worldIncluded||world.optBoolean("index_complete",true);boolean complete=primaryComplete&&worldComplete;
            int executable=0;for(int i=0;i<structured.length();i++){JSONObject rule=structured.optJSONObject(i);if(rule!=null&&rule.optBoolean("executable",false))executable++;}
            boolean sufficient=complete&&evidence.length()>0&&executable>0;
            out.put("evidence",evidence).put("structured_rules",structured).put("evidence_count",evidence.length()).put("executable_rule_count",executable).put("index_complete",complete).put("sufficient_for_mechanics",sufficient).put("world_simulation_included",worldIncluded);
            if(!complete)out.put("status","index_incomplete");
            else if(evidence.length()==0&&("not_required_fiction".equals(primaryStatus)||"scene_rule_cache_hit".equals(primaryStatus)))out.put("status",primaryStatus);
            else if(evidence.length()==0)out.put("status","no_relevant_evidence");
            else if("scene_rule_cache_hit".equals(primaryStatus))out.put("status","scene_rule_cache_hit");
            else if(executable==0)out.put("status","evidence_not_executable");
            else out.put("status","ready");
        }catch(Exception failure){AppLog.w("MasterOrchestrator","Não foi possível unir as provas do Mundo Vivo; a busca da cena foi preservada.",failure);}
        return out;
    }

    private static JSONObject compactRuleRetrieval(JSONObject source){
        JSONObject out=new JSONObject();if(source==null)return out;
        try{
            for(String key:new String[]{"status","required","world","enabled_manuals","ready_manuals","index_complete","index_missing_manuals","mechanical_candidate","sufficient_for_mechanics","executable_rule_count","instruction","retrieval_version","state_focus","world_simulation_included","index_health","cache_hit","cache_scope","cache_origin_status"})if(source.has(key))out.put(key,source.opt(key));
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
            JSONObject kingmaker=bus.optJSONObject("kingmaker");if(kingmaker!=null){kingmaker.put("territory",firstJson(kingmaker.optJSONArray("territory"),24));kingmaker.put("structures",firstJson(kingmaker.optJSONArray("structures"),24));kingmaker.put("turn_activities",firstJson(kingmaker.optJSONArray("turn_activities"),16));kingmaker.put("events",firstJson(kingmaker.optJSONArray("events"),12));}
            bus.put("map_context",tail(bus.optString("map_context"),2400));
            bus.put("context_reduced",true);
        }catch(Exception ignored){AppLog.ignored("MasterOrchestrator","Falha ao compactar barramento.",ignored);}
    }

    private static JSONObject criticalGameBus(JSONObject source,boolean minimum){
        JSONObject out=new JSONObject();if(source==null)return out;try{
            for(String key:new String[]{"world_profile","campaign","story","scene","combat_state","map","living_world","kingmaker","party_state","travel_rules_state","connection_manifest"})if(source.has(key))out.put(key,source.opt(key));
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
        for(String key:new String[]{"story","scene","party","cognitive_plan","rules_frame","rule_retrieval","living_world","kingmaker","local_tick","narrative_intent","input_channel","fiction_frozen","canonical_position","authority"})if(source.has(key))finalContext.put(key,deepCompactValue(source.opt(key),0,96,2));
        JSONObject bus=source.optJSONObject("game_bus");finalContext.put("game_bus",deepCompactObject(criticalGameBus(bus,true),0,96,2));
        finalContext.put("connection_manifest",connectionManifest(bus));finalContext.put("context_reduced",true);finalContext.put("hard_limit_applied",true);
        raw=finalContext.toString();if(raw.length()<=max)return raw;
        JSONObject minimum=new JSONObject();minimum.put("scene",deepCompactValue(source.opt("scene"),0,72,1));minimum.put("rules_frame",deepCompactValue(source.opt("rules_frame"),0,72,1));minimum.put("local_tick",deepCompactValue(source.opt("local_tick"),0,72,1));minimum.put("connection_manifest",connectionManifest(bus));minimum.put("context_reduced",true);minimum.put("hard_limit_applied",true);raw=minimum.toString();
        if(raw.length()>max)throw new IllegalStateException("O contexto mínimo excede o limite seguro.");return raw;
    }

    private static JSONObject connectionManifest(JSONObject bus)throws Exception{
        JSONObject out=new JSONObject();if(bus==null)return out;
        for(String key:new String[]{"active_heroes_full","combatants","tactical_tokens","recent_journal","recent_dice","npcs","npc_cognition","knowledge_facts","quests","factions","world_clocks","world_events","pending_transactions","party_actions","manual_catalog"}){JSONArray a=bus.optJSONArray(key);out.put(key,a==null?0:a.length());}
        for(String key:new String[]{"campaign","story","scene","combat_state","map","living_world","kingmaker","party_state","travel_rules_state"})out.put(key,bus.optJSONObject(key)!=null);
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
}
