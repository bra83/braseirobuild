#!/usr/bin/env python3
from pathlib import Path
import shutil
import sys

ROOT = Path(sys.argv[1]).resolve()
HERE = Path(__file__).resolve().parent
JAVA = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"

def read(path):
    return Path(path).read_text(encoding="utf-8")

def write(path, text):
    Path(path).write_text(text, encoding="utf-8")

def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f"anchor missing: {label}")
    return text.replace(old, new, 1)

def replace_between(text, start, end, replacement, label):
    a = text.find(start)
    if a < 0:
        raise RuntimeError(f"start anchor missing: {label}")
    b = text.find(end, a)
    if b < 0:
        raise RuntimeError(f"end anchor missing: {label}")
    return text[:a] + replacement + text[b:]

# ---------------------------------------------------------------------------
# 1) New deterministic prelude policy + Forbidden Lands-style chunker
# ---------------------------------------------------------------------------
shutil.copy2(HERE / "KingmakerPreludePolicy.java", JAVA / "KingmakerPreludePolicy.java")
shutil.copy2(HERE / "NarrationChunker.java", JAVA / "NarrationChunker.java")

# ---------------------------------------------------------------------------
# 2) Persistent campaign phase in the existing Kingmaker authority
# ---------------------------------------------------------------------------
repo_path = JAVA / "KingmakerRepository.java"
repo = read(repo_path)
repo = replace_once(
    repo,
    '        ensureColumn(sql,"kingmaker_state","anarchy","INTEGER NOT NULL DEFAULT 0");',
    '        ensureColumn(sql,"kingmaker_state","anarchy","INTEGER NOT NULL DEFAULT 0");\n'
    '        ensureColumn(sql,"kingmaker_state","campaign_story_phase","TEXT NOT NULL DEFAULT \'restov_arrival\'");\n'
    '        ensureColumn(sql,"kingmaker_state","main_adventure_started","INTEGER NOT NULL DEFAULT 0");',
    "kingmaker_state prelude columns",
)
repo_methods = r'''
    public String campaignStoryPhase(){
        String phase=state().optString("campaign_story_phase",KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL).trim().toLowerCase(Locale.ROOT);
        if(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(phase)
                ||KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX.equals(phase)
                ||KingmakerPreludePolicy.PHASE_JAMANDI_MANSION.equals(phase)
                ||KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)
                ||KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION.equals(phase))return phase;
        return KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL;
    }

    public boolean mainAdventureStarted(){return state().optInt("main_adventure_started",0)==1;}

    public JSONObject resetCampaignPrelude(){
        return setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL,false);
    }

    public JSONObject setCampaignStoryPhase(String phase,boolean mainStarted){
        String next=phase==null?"":phase.trim().toLowerCase(Locale.ROOT);
        if(!(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(next)
                ||KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX.equals(next)
                ||KingmakerPreludePolicy.PHASE_JAMANDI_MANSION.equals(next)
                ||KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(next)
                ||KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION.equals(next)))
            throw new IllegalArgumentException("Fase narrativa Kingmaker inválida: "+phase);
        ContentValues v=new ContentValues();
        v.put("campaign_story_phase",next);
        v.put("main_adventure_started",mainStarted?1:0);
        v.put("updated_at",System.currentTimeMillis());
        db.getWritableDatabase().update("kingmaker_state",v,"world_profile=?",new String[]{profile()});
        return state();
    }

'''
repo = replace_once(
    repo,
    '    public boolean enabled(){return state().optInt("enabled",0)==1;}\n\n',
    '    public boolean enabled(){return state().optInt("enabled",0)==1;}\n\n' + repo_methods,
    "repository campaign phase methods",
)
write(repo_path, repo)

# ---------------------------------------------------------------------------
# 3) Solo router understands Restov and mansion scenes
# ---------------------------------------------------------------------------
router_path = JAVA / "KingmakerSoloFlowRouter.java"
router = read(router_path)
router = replace_once(
    router,
    '    public static final String START_ADVENTURE = "start_adventure";',
    '    public static final String START_ADVENTURE = "start_adventure";\n'
    '    public static final String RESTOV_SANDBOX = "restov_sandbox";\n'
    '    public static final String JAMANDI_MANSION = "jamandi_mansion";\n'
    '    public static final String GO_JAMANDI_MANSION = "go_jamandi_mansion";',
    "router prelude constants",
)
router = replace_once(
    router,
    '        if ("adventure_start".equals(scene)) return START_ADVENTURE;',
    '        if ("adventure_start".equals(scene)) return START_ADVENTURE;\n'
    '        if ("restov_sandbox".equals(scene)) return RESTOV_SANDBOX;\n'
    '        if ("jamandi_mansion".equals(scene)) return JAMANDI_MANSION;\n'
    '        if ("go_jamandi_mansion".equals(scene)) return GO_JAMANDI_MANSION;',
    "router prelude mappings",
)
write(router_path, router)

# ---------------------------------------------------------------------------
# 4) Solo director: Restov sandbox before the canonical Stolen Lands chapters
# ---------------------------------------------------------------------------
solo_path = JAVA / "KingmakerSoloDirector.java"
solo = read(solo_path)

adventure_method = r'''    private JSONObject adventureGuide(JSONObject story,JSONObject map,JSONObject scene,JSONObject turn,JSONObject camp,List<JSONObject> companions,List<JSONObject> companionEvents,List<JSONObject> explorationEvents,List<JSONObject> wars,JSONObject kingdomGuide,JSONObject explorationGuide)throws Exception{
        JSONObject out=new JSONObject();
        String phase=repository.campaignStoryPhase();
        boolean started=story.optInt("started")==1, activeTurn=turn!=null&&turn.length()>0&&"active".equals(turn.optString("status"));
        String hex=map.optString("current_hex_id",scene.optString("hex_id"));
        out.put("active",started).put("hex",hex).put("location",scene.optString("location"))
                .put("weather",scene.optString("weather",map.optString("weather")))
                .put("period",scene.optString("time_of_day",scene.optString("period")))
                .put("campaign_story_phase",phase)
                .put("main_adventure_started",repository.mainAdventureStarted());

        JSONArray party=new JSONArray();
        for(JSONObject c:companions){if(c.optInt("available",1)!=1)continue;party.put(new JSONObject().put("key",c.optString("companion_key")).put("name",c.optString("name")).put("assignment",c.optString("assignment","reserve")).put("personal_status",c.optString("personal_quest_status","unknown")));}
        out.put("active_companions",party).put("companion_scene_pending",companionEvents!=null&&!companionEvents.isEmpty()).put("exploration_scene_pending",explorationEvents!=null&&!explorationEvents.isEmpty()).put("war_active",wars!=null&&!wars.isEmpty()).put("camp_active",camp!=null&&camp.length()>0);

        if(KingmakerPreludePolicy.isRestovPhase(phase)){
            return out.put("next_scene","restov_sandbox")
                    .put("title",KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(phase)?"Chegar a Restov":"Viver Restov antes da convocação")
                    .put("text","A campanha principal ainda não foi iniciada. Restov permanece sandbox; NPCs e acontecimentos podem emergir, mas a mansão de Lady Jamandi só começa quando o jogador decidir ir até lá.");
        }
        if(KingmakerPreludePolicy.isMansionPhase(phase)){
            return out.put("next_scene","jamandi_mansion")
                    .put("title",KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)?"Preparar a partida de Restov":"Continuar a Convocação de Heróis")
                    .put("text",KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)
                            ?"A carta foi aceita, mas o grupo ainda está em Restov. Preparação e despedidas podem acontecer antes da partida."
                            :"O arco principal está ativo dentro da mansão de Lady Jamandi. Jogue seus acontecimentos em ordem e não salte diretamente para Oleg.");
        }

        if(!started)return out.put("next_scene","adventure_start").put("title","Retomar a expedição Kingmaker").put("text","A fase da expedição está ativa; retome o estado persistido sem criar um novo começo.");
        if(camp!=null&&camp.length()>0){boolean encounter="RANDOM_ENCOUNTER".equals(camp.optString("phase"));return out.put("next_scene",encounter?"camp_encounter":"camp").put("title",encounter?"Resolver encontro do acampamento":"Continuar o acampamento").put("text","O acampamento preserva hex, clima, companions e horas já registradas. Resolva a ficção antes de qualquer avanço.");}
        if(wars!=null&&!wars.isEmpty())return out.put("next_scene","war").put("title","Batalha em andamento").put("text","Volte à batalha persistida; HP, moral e alvos continuam sob a autoridade do motor de guerra.");
        if(explorationEvents!=null&&!explorationEvents.isEmpty())return out.put("next_scene","exploration_resolution").put("title","Fechar a exploração atual").put("text","Conclua a cena no hex de origem e só então confirme reconhecimento ou limpeza.");
        if(companionEvents!=null&&!companionEvents.isEmpty())return out.put("next_scene","companion").put("title","Continuar a cena pessoal").put("text",companionEvents.get(0).optString("title")+" permanece pendente; continue pela ficção sem editar influência ou Reino.");
        if(activeTurn)return out.put("next_scene","kingdom_bridge").put("title",kingdomGuide.optString("title","Continuar o Reino")).put("text",kingdomGuide.optString("text")+" Depois da decisão, retorne à aventura sem abrir manutenção técnica adicional.");
        String next=explorationGuide.optString("next_hex");
        if(next.length()>0)return out.put("next_scene","travel").put("title","Viajar para "+next).put("text","Use a rota já persistida e o motor PF2e existente; companions disponíveis seguem como participantes da cena.");
        return out.put("next_scene","free_exploration").put("title","Explorar e deixar o mundo responder").put("text","Escolha um vizinho canônico ou permaneça no hex atual. Companions podem gerar cenas pessoais organicamente; o próximo turno do Reino só entra quando a ficção alcançar downtime.");
    }

'''
solo = replace_between(
    solo,
    '    private JSONObject adventureGuide(',
    '    private JSONObject warGuide(',
    adventure_method,
    "solo adventureGuide",
)

campaign_flow_method = r'''    private JSONArray campaignFlow(JSONObject story,JSONObject map,JSONObject turn,JSONObject camp,List<JSONObject> events,List<JSONObject> companionEvents,List<JSONObject> explorationEvents,List<JSONObject> wars,List<JSONObject> companions,JSONObject kingdomGuide,JSONObject explorationGuide)throws Exception{
        JSONArray steps=new JSONArray();
        String phase=repository.campaignStoryPhase();
        if(story.optInt("started")!=1){
            steps.put(step("adventure_start","Chegar a Restov","A primeira cena mostra os personagens entrando em Restov; forma de chegada, horário, clima e incidentes variam de maneira coerente.","adventure",false));
            return steps;
        }
        if(KingmakerPreludePolicy.isRestovPhase(phase)){
            steps.put(step("restov_sandbox","Continuar vivendo Restov","Explore a cidade, converse, investigue, trabalhe ou siga acontecimentos emergentes. A campanha principal ainda não foi ativada.","adventure",false));
            steps.put(step("go_jamandi_mansion","Ir à mansão de Lady Jamandi","Esta escolha inicia o roteiro principal de Kingmaker. Nada força o grupo a fazê-la antes de você decidir.","adventure",false));
            return steps;
        }
        if(KingmakerPreludePolicy.isMansionPhase(phase)){
            steps.put(step("jamandi_mansion",KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)?"Preparar a saída de Restov":"Continuar a aventura na mansão de Lady Jamandi",
                    KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED.equals(phase)
                            ?"A missão está aceita. Jogue o que ainda quiser em Restov e parta somente quando decidir."
                            :"Continue a sequência canônica da Convocação de Heróis em cenas; não pule acontecimentos nem teleporte para Oleg.","adventure",true));
            return steps;
        }
        if(camp.length()>0){boolean encounter="RANDOM_ENCOUNTER".equals(camp.optString("phase"));steps.put(step(encounter?"camp_encounter":"camp",encounter?"Resolver o encontro do acampamento":"Concluir o acampamento",encounter?"O descanso está pausado por um encontro disparado. Jogue a cena e só depois retome as atividades.":"Resolva atividades, vigias e eventual encontro antes de avançar o dia.","camp",true));}
        if(wars!=null&&!wars.isEmpty())steps.put(step("war","Resolver a guerra em cena","Conduza a batalha ativa por rodadas; o motor local mantém HP, moral e estado dos exércitos.","warfare",true));
        if(events!=null&&!events.isEmpty())steps.put(step("event","Trazer o evento do Reino para a aventura","Apresente a consequência como cena antes de pedir qualquer resolução mecânica.","kingdom",true));
        if(companionEvents!=null&&!companionEvents.isEmpty())steps.put(step("companion_event","Continuar evento pessoal de companion",companionEvents.get(0).optString("title")+" está pendente e deve continuar como cena, não como edição de ficha.","adventure",true));
        if(explorationEvents!=null&&!explorationEvents.isEmpty())steps.put(step("exploration_event","Concluir a exploração em andamento",explorationEvents.get(0).optString("title")+" aguarda o resultado da cena antes de atualizar o estado do hex.","atlas",true));
        int active=0,quest=0;for(JSONObject c:companions){if(c.optInt("available",1)==1){active++;String q=c.optString("personal_quest_status","unknown");if(!"unknown".equals(q)&&!"complete".equals(q)&&!"completed".equals(q))quest++;}}
        if(active>0)steps.put(step("companions","Dar espaço aos companheiros",quest>0?quest+" arco(s) pessoal(is) podem emergir organicamente durante viagem, descanso ou retorno ao Reino.":"Use os companheiros disponíveis como participantes ativos da exploração, não como inventário.","adventure",false));
        if(explorationGuide!=null){String current=explorationGuide.optString("current_hex"),next=explorationGuide.optString("next_hex"),destination=explorationGuide.optString("destination_hex"),status=explorationGuide.optString("current_status","desconhecido");if(next.length()>0)steps.put(step("expedition_travel","Avançar a expedição para "+next,"O destino "+destination+" já está planejado. O movimento continua no motor PF2e existente, um hex por vez.","atlas",false));else if(current.length()>0&&("desconhecido".equals(status)||"reconnoiterado".equals(status)))steps.put(step("explore_current","Explorar o hex "+current,"Jogue a exploração em cena; reconhecimento e limpeza só são persistidos depois de confirmação explícita.","atlas",false));else{String exploreText=current.length()>0?"Escolha um dos vizinhos canônicos de "+current+" e planeje somente o próximo trecho.":"O Atlas das Terras Roubadas aguarda a chegada narrativa ao primeiro ponto canônico; não invente teleporte.";steps.put(step("plan_expedition","Expedição às Terras Roubadas",exploreText,"atlas",false).put("legacy_key","explore"));}}
        if(turn.length()>0&&"active".equals(turn.optString("status")))steps.put(step("kingdom_turn",kingdomGuide.optString("title","Resolver o Reino quando a ficção pedir"),kingdomGuide.optString("text","Mostre apenas a próxima escolha humana; cálculos permanecem no motor."),"kingdom",false));
        else steps.put(step("kingdom_transition","Retornar ao Reino sem burocracia","Quando a aventura alcançar downtime apropriado, o modo guiado pode iniciar o próximo turno com rolagens locais automáticas e nenhuma ficha administrativa.","kingdom",false));
        return steps;
    }

'''
solo = replace_between(
    solo,
    '    private JSONArray campaignFlow(',
    '    private static JSONObject step(',
    campaign_flow_method,
    "solo campaignFlow",
)

prelude_guidance = r'''
            String storyPhase = repository.campaignStoryPhase();
            if (KingmakerPreludePolicy.isRestovPhase(storyPhase)) {
                out.put("active_chapter", 0)
                   .put("current_stage", storyPhase)
                   .put("active_quests", new JSONArray())
                   .put("recommended_scene", "restov_sandbox")
                   .put("scene_action", KingmakerSoloFlowRouter.RESTOV_SANDBOX)
                   .put("guidance_text", "Restov está em modo sandbox. A mansão de Lady Jamandi é um caminho disponível, não uma cena automática.")
                   .put("chapter_title", "Prólogo — Restov")
                   .put("world_profile", profile);
                return out;
            }
            if (KingmakerPreludePolicy.isMansionPhase(storyPhase)) {
                out.put("active_chapter", 0)
                   .put("current_stage", storyPhase)
                   .put("active_quests", new JSONArray())
                   .put("recommended_scene", "jamandi_mansion")
                   .put("scene_action", KingmakerSoloFlowRouter.JAMANDI_MANSION)
                   .put("guidance_text", KingmakerPreludePolicy.directiveForPhase(storyPhase))
                   .put("chapter_title", "Convocação de Heróis — Mansão de Lady Jamandi")
                   .put("world_profile", profile);
                return out;
            }

'''
profile_anchor = '''            String profile = (worldProfile == null || worldProfile.trim().length() == 0)
                    ? (db != null ? db.getActiveMapProfile() : "pf2")
                    : worldProfile.trim();

'''
solo = replace_once(solo, profile_anchor, profile_anchor + prelude_guidance, "campaign guidance prelude gate")
solo = solo.replace("iniciar a aventura no posto comercial de Oleg.", "mostrar a chegada a Restov e manter a mansão de Lady Jamandi como escolha do jogador.")
write(solo_path, solo)

# ---------------------------------------------------------------------------
# 5) MainActivity: opening anchored in Restov, explicit mansion trigger,
#    no Stolen Lands teleport, and 5–6k-character major opening.
# ---------------------------------------------------------------------------
main_path = JAVA / "MainActivity.java"
main = read(main_path)

new_adventure_block = r'''    private void beginAdventure(final boolean reset){
        if(adventureStartRunning||aiBusy){toast("A abertura já está sendo preparada.");return;}
        if(db.listActiveCharacters().isEmpty()){toast("Ative ao menos um personagem antes de começar.");navigate("Personagens");return;}
        if(reset){safety.beforeDestructiveChange("começar uma nova aventura");db.clearStoryState();lastNarration="";lastVisualPrompt="";currentOptions=new JSONArray();pendingTest=new JSONObject();sharedImageUri=null;prefs.edit().remove(PREF_SCENE_IMAGE_URI).remove(PREF_SCENE_IMAGE_FINGERPRINT).remove(PREF_PENDING_IMAGE_FINGERPRINT).remove(PREF_RECENT_SCENE_OPTIONS).apply();db.saveSceneOptionHistory("[]");db.addAuditEvent("campanha","Nova aventura iniciada","História anterior preservada em snapshot","{}","{}",true);if(kingmakerRepository!=null)kingmakerRepository.resetCampaignPrelude();}
        launchAdventure(reset,"");
    }

    private void launchAdventure(final boolean reset,final String requestedHex){
        adventureStartRunning=true;setStatus("Preparando a primeira cena...");
        try{
            String phase=kingmakerRepository==null?KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL:kingmakerRepository.campaignStoryPhase();
            final boolean restovPrelude=kingmakerRepository!=null&&KingmakerPreludePolicy.isPreExpeditionPhase(phase);
            final JSONObject startingPoint;
            if(restovPrelude)startingPoint=prepareKingmakerRestovStart(reset);
            else startingPoint=prepareAdventureStartingPoint(requestedHex);
            final String startingHex=startingPoint.optString("hex_id","");
            if(!restovPrelude){if(reset)resetSceneClock();else initializeSceneClock();}
            final JSONObject campaign=db.getCampaign();
            final String world=activeWorldProfile();
            final String tone=campaign.optString("tone","aventura");
            final String campaignName=campaign.optString("name","Kingmaker");
            final long openingSeed=System.currentTimeMillis()^((long)campaignName.hashCode()<<17)^world.hashCode();
            final String openingInstruction;
            if(restovPrelude)openingInstruction=KingmakerPreludePolicy.arrivalDirective(openingSeed);
            else openingInstruction=AdventureOpeningPlanner.choose(world,campaignName,tone,startingHex,prefs.getString("recent_opening_plans","[]"),prefs.getString("recent_opening_texts","[]"),openingSeed).instruction;

            final String opening;
            if(restovPrelude){
                opening="INÍCIO AUTOMÁTICO — PRIMEIRA CENA NARRATIVA DE KINGMAKER. "
                        +"A âncora obrigatória é RESTOV, em Rostland/Brevoy: a primeira cena mostra os personagens CHEGANDO À CIDADE. "
                        +openingInstruction+" Horário e clima já foram sorteados e estão no scene_state; respeite-os. "
                        +"Depois da entrada, Restov é SANDBOX. NPCs, rumores, trabalhos, pequenos conflitos e acontecimentos podem surgir organicamente e variar entre campanhas. "
                        +"A mansão de Lady Jamandi Aldori pode existir como destino conhecido, convite, rumor ou possibilidade, mas NÃO leve os personagens até lá, NÃO aceite a convocação por eles e NÃO comece a campanha principal até o jogador escolher explicitamente ir à mansão. "
                        +"Não mencione Oleg nem coloque o grupo nas Terras Roubadas nesta abertura. Não transforme a cidade em menu de opções ou tutorial. "
                        +"Esta é uma CENA PRINCIPAL: escreva aproximadamente 5.000 a 6.000 caracteres de prosa PT-BR, em parágrafos substanciais, com câmera narrativa, sensorialidade funcional, pessoas agindo e espaço jogável. "
                        +"A chegada precisa ser concreta e variar naturalmente; exemplos como esconder-se sob feno, vir a cavalo ou chegar a pé são apenas exemplos, nunca uma lista fechada. "
                        +"Preserve a agência: descreva estímulos e circunstâncias, não sentimentos ou decisões voluntárias dos personagens. "
                        +"Todo personagem ou grupo visível, falante ou agente deve constar em scene_state.present_npcs; NPCs nomeados/persistentes também entram em npc_updates. "
                        +"Termine em um ponto real de decisão dentro de Restov e ofereça 4 a 6 escolhas ligadas ao que aconteceu, podendo incluir — sem obrigar — seguir o assunto da convocação de Lady Jamandi.";
            }else{
                opening="INÍCIO AUTOMÁTICO — PRIMEIRA CENA NARRATIVA. Comece a aventura agora como ficção de mesa, não como explicação de regras, tutorial, relatório do aplicativo ou resposta sobre o funcionamento do sistema. "
                        +"A abertura deve ser LONGA, IMERSIVA, JOGÁVEL e significativa desde o primeiro parágrafo: situe quem, onde, o que está acontecendo, o que mudou ou pressiona a cena e por que o personagem precisa prestar atenção. "
                        +openingInstruction+" O estado autoritativo, o Atlas, a campanha, os personagens, os NPCs e o Mundo Vivo já estão no contexto interno do Mestre; use esses fatos sem mencioná-los como estrutura técnica. "
                        +"Todo personagem ou grupo visível, falante ou agente deve constar em scene_state.present_npcs. NPCs nomeados, persistentes ou individualmente relevantes também devem constar em npc_updates, com name, role, present=true e visual_description concreta. "
                        +"Escreva aproximadamente 5.000 a 6.000 caracteres em 6 a 8 parágrafos substanciais. Termine num ponto de decisão concreto e ofereça de 4 a 6 escolhas diretamente ligadas ao que acabou de acontecer.";
            }

            adventureStartupCoordinator.phase(AdventureStartupCoordinator.Phase.NARRATIVE,"Solicitando a primeira cena narrativa");
            ui.post(new Runnable(){public void run(){
                adventureStartRunning=false;lastAiUsedRag=false;lastAiManuals=new JSONArray();lastRagCitations=new JSONArray();activeRuleRetrieval=new JSONObject();
                prefs.edit().remove("last_rag_citations").remove("last_manual_context").apply();
                setStatus(restovPrelude?"Restov • escrevendo a chegada dos personagens…":(startingHex.length()>0?"Atlas pronto • escrevendo a primeira cena…":"Aventura iniciada sem Atlas • escrevendo a primeira cena…"));
                sendAi("Narrador",opening,"PLAYER_ACTION");
            }});
        }catch(Exception error){adventureStartRunning=false;adventureStartupCoordinator.phase(AdventureStartupCoordinator.Phase.BLOCKED,"Falha de abertura: "+safeMessage(error));toast("Abertura: "+safeMessage(error));}
    }

    private JSONObject prepareKingmakerRestovStart(boolean reset)throws Exception{
        if(kingmakerRepository!=null&&(reset||!KingmakerPreludePolicy.isPreExpeditionPhase(kingmakerRepository.campaignStoryPhase())))
            kingmakerRepository.resetCampaignPrelude();
        JSONObject map=db.getMapState();
        String packageId=map.optString("map_package_id","");
        if(packageId.length()>0&&db.isMapPackageUsable(packageId)){
            db.saveMapPackageState(map.optString("image_uri"),map.optString("overlay_uri"),map.optString("manifest_json"),packageId,"",
                    map.optInt("overlay_visible",1)==1,map.optString("reveals_json","{}"),map.optString("grid_type","Quadrada"),
                    (float)map.optDouble("grid_size",64),map.optInt("fog_enabled",0)==1,(float)map.optDouble("token_x",.5),
                    (float)map.optDouble("token_y",.5),(float)map.optDouble("scale",1),(float)map.optDouble("offset_x",0),(float)map.optDouble("offset_y",0));
            db.saveMapTravelState(false,"","[]","Kingmaker — prólogo em Restov");
        }
        long seed=System.nanoTime()^System.currentTimeMillis()^activeWorldProfile().hashCode();
        String daypart=KingmakerPreludePolicy.randomDaypart(seed);
        JSONObject scene=new JSONObject();
        scene.put("day_number",1);
        scene.put("location","Restov, Rostland, Brevoy");
        scene.put("hex_id","");
        scene.put("time_of_day",daypart);
        scene.put("clock_hour",KingmakerPreludePolicy.hourForDaypart(daypart));
        scene.put("weather",KingmakerPreludePolicy.randomWeather(seed));
        scene.put("lighting",KingmakerPreludePolicy.lightingForDaypart(daypart));
        scene.put("danger","");
        scene.put("objective","Entrar em Restov e decidir livremente o que fazer na cidade.");
        scene.put("present_npcs",new JSONArray());
        scene.put("threats",new JSONArray());
        scene.put("clues",new JSONArray());
        scene.put("pending_consequences",new JSONArray());
        db.saveSceneState(scene);
        db.addAuditEvent("kingmaker_prologo","Chegada preparada em Restov",daypart+" • "+scene.optString("weather"),"{}",scene.toString(),true);
        return new JSONObject().put("hex_id","").put("start_location_name","Restov, Rostland, Brevoy").put("atlas_optional",true).put("map_grounded",false).put("prelude","restov");
    }

    private String prepareKingmakerPreludeAction(String raw){
        if(kingmakerRepository==null)return raw==null?"":raw;
        String text=raw==null?"":raw;
        try{
            String phase=kingmakerRepository.campaignStoryPhase();
            if(KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(phase)&&db.getStoryState().optInt("started",0)==1){
                kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX,false);
                phase=KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX;
            }
            if(KingmakerPreludePolicy.isRestovPhase(phase)&&KingmakerPreludePolicy.isMansionIntent(text)){
                kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_JAMANDI_MANSION,true);
                JSONObject scene=db.getSceneState();
                scene.put("location","Restov — caminho para a Mansão de Lady Jamandi Aldori");
                scene.put("hex_id","");
                scene.put("objective","Chegar à mansão de Lady Jamandi e responder à Convocação de Heróis.");
                db.saveSceneState(scene);
                db.addAuditEvent("kingmaker_prologo","Campanha principal iniciada","Jogador escolheu ir à mansão de Lady Jamandi","{}",scene.toString(),true);
                return text+"\n\n"+KingmakerPreludePolicy.directiveForPhase(KingmakerPreludePolicy.PHASE_JAMANDI_MANSION);
            }
            if(KingmakerPreludePolicy.isMansionPhase(phase)){
                if(KingmakerPreludePolicy.isExpeditionDepartureIntent(text)){
                    kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION,true);
                    prepareStolenLandsDepartureAtlas();
                    return text+"\n\n"+KingmakerPreludePolicy.directiveForPhase(KingmakerPreludePolicy.PHASE_STOLEN_LANDS_EXPEDITION);
                }
                if(KingmakerPreludePolicy.PHASE_JAMANDI_MANSION.equals(phase)&&KingmakerPreludePolicy.isCharterAcceptanceIntent(text)){
                    kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED,true);
                    phase=KingmakerPreludePolicy.PHASE_CHARTER_RECEIVED;
                }
            }
            String directive=KingmakerPreludePolicy.directiveForPhase(phase);
            return directive.length()==0?text:(text+"\n\n"+directive);
        }catch(Exception error){AppLog.w("KingmakerPrelude","Falha ao atualizar fase narrativa; preservando ação original.",error);return text;}
    }

    private void prepareStolenLandsDepartureAtlas(){
        try{
            JSONObject map=db.getMapState();String packageId=map.optString("map_package_id","");
            if(packageId.length()>0&&db.isMapPackageUsable(packageId)){
                JSONObject oleg=db.describeCatalogedAdventureStart(packageId,"H_OLEG");
                if(oleg.length()>0)db.saveMapTravelState(false,"H_OLEG","[]","Kingmaker — expedição às Terras Roubadas");
            }
            JSONObject scene=db.getSceneState();
            scene.put("location","Estrada ao sul de Restov — rumo às Terras Roubadas");
            scene.put("hex_id","");
            scene.put("objective","Viajar às Terras Roubadas; o Atlas só fixa H_OLEG quando a chegada for estabelecida na ficção.");
            db.saveSceneState(scene);
            db.addAuditEvent("kingmaker_prologo","Expedição iniciada","Restov → Terras Roubadas; H_OLEG é destino canônico, não teleporte","{}",scene.toString(),true);
        }catch(Exception error){AppLog.w("KingmakerPrelude","Não foi possível preparar o destino do Atlas; a ficção continuará sem teleporte.",error);}
    }

    private void startJamandiMansionFromUi(){
        navigate("Mestre IA");
        sendAi("Narrador","O jogador decide ir agora à mansão de Lady Jamandi Aldori para responder à convocação. Narre a aproximação e a entrada naturalmente; não resuma acontecimentos futuros.","PLAYER_ACTION");
    }

'''
main = replace_between(
    main,
    '    private void beginAdventure(final boolean reset){',
    '    private boolean isEligibleAdventureStartHex',
    new_adventure_block,
    "MainActivity adventure opening",
)

old_run = '    private void runKingmakerSoloStep(JSONObject step){String key=step==null?"":step.optString("key");if("camp".equals(key)){showKingmakerActiveCampOrCamps();return;}if("camp_encounter".equals(key)){playKingmakerPendingCampEncounter();return;}if("war".equals(key)){showKingmakerActiveWarOrArmies();return;}if("event".equals(key)){showKingmakerEventsDialog();return;}if("companion_event".equals(key)||"companion".equals(key)){playPendingKingmakerCompanionEvent();return;}if("companions".equals(key)){showKingmakerCompanionFocus();return;}if("exploration_event".equals(key)){showKingmakerExplorationGuide();return;}if("explore".equals(key)||"explore_current".equals(key)||"plan_expedition".equals(key)||"expedition_travel".equals(key)||"travel".equals(key)||"free_exploration".equals(key)){showKingmakerExplorationGuide();return;}if("kingdom_turn".equals(key)||"kingdom_transition".equals(key)){runGuidedKingdomDecision();return;}if("adventure_start".equals(key)){beginAdventure(false);return;}navigate("Mestre IA");}'
new_run = r'''    private void runKingmakerSoloStep(JSONObject step){
        String key=step==null?"":step.optString("key");
        if("go_jamandi_mansion".equals(key)){startJamandiMansionFromUi();return;}
        if("restov_sandbox".equals(key)){navigate("Mestre IA");sendAi("Narrador","Continue a vida sandbox em Restov a partir do estado persistido. Faça o mundo se mover naturalmente; não leve o grupo à mansão de Lady Jamandi sem escolha explícita do jogador.","PLAYER_ACTION");return;}
        if("jamandi_mansion".equals(key)){navigate("Mestre IA");sendAi("Narrador","Continue a sequência atual da Convocação de Heróis na mansão de Lady Jamandi a partir do ponto exato persistido; não salte para Oleg ou para um acontecimento futuro.","PLAYER_ACTION");return;}
        if("camp".equals(key)){showKingmakerActiveCampOrCamps();return;}
        if("camp_encounter".equals(key)){playKingmakerPendingCampEncounter();return;}
        if("war".equals(key)){showKingmakerActiveWarOrArmies();return;}
        if("event".equals(key)){showKingmakerEventsDialog();return;}
        if("companion_event".equals(key)||"companion".equals(key)){playPendingKingmakerCompanionEvent();return;}
        if("companions".equals(key)){showKingmakerCompanionFocus();return;}
        if("exploration_event".equals(key)){showKingmakerExplorationGuide();return;}
        if("explore".equals(key)||"explore_current".equals(key)||"plan_expedition".equals(key)||"expedition_travel".equals(key)||"travel".equals(key)||"free_exploration".equals(key)){showKingmakerExplorationGuide();return;}
        if("kingdom_turn".equals(key)||"kingdom_transition".equals(key)){runGuidedKingdomDecision();return;}
        if("adventure_start".equals(key)){beginAdventure(false);return;}
        navigate("Mestre IA");
    }'''
main = replace_once(main, old_run, new_run, "Kingmaker Solo UI prelude steps")

main = replace_once(
    main,
    '        final String requestedText=userText==null?"":userText;',
    '        String preparedText=userText==null?"":userText;\n'
    '        final boolean rawOpening=RuleRetrievalEngine.isNarrativeOpeningRequest(preparedText);\n'
    '        if("Narrador".equals(mode)&&!"GM_HELP".equals(inputChannel)&&!rawOpening)preparedText=prepareKingmakerPreludeAction(preparedText);\n'
    '        final String requestedText=preparedText;',
    "sendAi prelude transition",
)

old_persist = '    private void persistStory(String action){db.saveStoryState(lastNarration.length()>0,lastNarration,lastVisualPrompt,currentOptions==null?"[]":currentOptions.toString(),pendingTest==null||pendingTest.length()==0?"":pendingTest.toString(),action==null?"":action);}'
new_persist = r'''    private void persistStory(String action){
        db.saveStoryState(lastNarration.length()>0,lastNarration,lastVisualPrompt,currentOptions==null?"[]":currentOptions.toString(),pendingTest==null||pendingTest.length()==0?"":pendingTest.toString(),action==null?"":action);
        try{
            if(kingmakerRepository!=null&&lastNarration.length()>0&&KingmakerPreludePolicy.PHASE_RESTOV_ARRIVAL.equals(kingmakerRepository.campaignStoryPhase()))
                kingmakerRepository.setCampaignStoryPhase(KingmakerPreludePolicy.PHASE_RESTOV_SANDBOX,false);
        }catch(Exception error){AppLog.w("KingmakerPrelude","Não foi possível avançar chegada → sandbox.",error);}
    }'''
main = replace_once(main, old_persist, new_persist, "persist Restov sandbox transition")

# Opening repair/length gates: character count, not word quota.
main = replace_once(main, '            final boolean needsExpansion=words<650||paragraphs<6;', '            final boolean needsExpansion=draft.length()<4800||paragraphs<6;', "opening char gate")
main = replace_once(main, '                final int missingWords=Math.max(260,720-words);', '                final int missingChars=Math.max(900,5200-draft.length());', "opening missing chars")
main = replace_once(
    main,
    '                        ?"CENA JÁ ESCRITA (NÃO REPITA):\\n"+draft+"\\n\\nCONTINUE EXATAMENTE DEPOIS DA ÚLTIMA FRASE COM CERCA DE "+Math.min(450,missingWords+120)+" PALAVRAS NOVAS, em 2 a 4 parágrafos. A continuação deve aprofundar presença, espaço, tensão, relações, sinais observáveis e consequências em curso; não faça resumo nem catálogo. Feche a cena completa em uma decisão concreta. Em options, devolva 4 a 6 ações diferentes que só façam sentido por causa de fatos presentes nesta cena completa."',
    '                        ?"CENA JÁ ESCRITA (NÃO REPITA):\\n"+draft+"\\n\\nCONTINUE EXATAMENTE DEPOIS DA ÚLTIMA FRASE COM CERCA DE "+Math.min(2200,missingChars+350)+" CARACTERES NOVOS, em 2 a 4 parágrafos. A cena principal completa deve ficar aproximadamente na faixa de 5.000 a 6.000 caracteres. A continuação deve aprofundar presença, espaço, tensão, relações, sinais observáveis e consequências em curso; não faça resumo nem catálogo. Feche a cena completa em uma decisão concreta. Em options, devolva 4 a 6 ações diferentes que só façam sentido por causa de fatos presentes nesta cena completa."',
    "opening continuation chars",
)
main = replace_once(
    main,
    '                        :"RASCUNHO RECEBIDO DO GEMINI:\\n"+draft+"\\n\\nREESCREVA/EXPANDA PARA 650 A 900 PALAVRAS EM 6 A 8 PARÁGRAFOS SUBSTANCIAIS. Nos primeiros 100 termos deixe claros protagonista/foco, local, situação atual, pressão e por que isso importa. Use detalhes sensoriais com função, geografia acionável, pessoas/forças que possam reagir e consequências em curso. Não transforme ferramentas, materiais, arquitetura ou rotina em catálogo técnico. Termine em uma decisão concreta do jogador. Em options, devolva 4 a 6 ações específicas, distintas e diretamente ancoradas nessa decisão; não use opções genéricas. Não mencione estrutura de JSON, fontes, regras ou software.";',
    '                        :"RASCUNHO RECEBIDO DO GEMINI:\\n"+draft+"\\n\\nREESCREVA/EXPANDA ESTA CENA PRINCIPAL PARA APROXIMADAMENTE 5.000 A 6.000 CARACTERES, EM 6 A 8 PARÁGRAFOS SUBSTANCIAIS. Nos primeiros parágrafos deixe claros protagonista/foco, local, situação atual, pressão e por que isso importa. Use detalhes sensoriais com função, geografia acionável, pessoas/forças que possam reagir e consequências em curso. Não transforme ferramentas, materiais, arquitetura ou rotina em catálogo técnico. Termine em uma decisão concreta do jogador. Em options, devolva 4 a 6 ações específicas, distintas e diretamente ancoradas nessa decisão; não use opções genéricas. Não mencione estrutura de JSON, fontes, regras ou software.";',
    "opening rewrite chars",
)
main = main.replace(
    'ABERTURA JOGÁVEL: 6 a 8 parágrafos substanciais, normalmente 650 a 900 palavras.',
    'ABERTURA JOGÁVEL: cena principal com 6 a 8 parágrafos substanciais, normalmente cerca de 5.000 a 6.000 caracteres.'
)
main = main.replace(
    'Escreva 6 a 8 parágrafos substanciais, normalmente entre 650 e 900 palavras.',
    'Escreva 6 a 8 parágrafos substanciais, normalmente em torno de 5.000 a 6.000 caracteres.'
)

# Settings text must describe the actual new two-layer Android/Kokoro pipeline.
main = main.replace(
    'Braseiro Kokoro PT-BR: blocos 250–650 (alvo 450), primeiro bloco QUEUE_FLUSH, seguintes QUEUE_ADD e avanço somente após onDone real. O motor mantém OfflineTts/prewarm/streaming internamente; o VTT não usa watchdog neural.',
    'Braseiro Kokoro PT-BR: segmentação em duas camadas como no Forbidden Lands — capítulo inicial até ~320 caracteres, seguintes até ~680; envio Android em segmentos de ~150 (corte natural a partir de ~90), sem cortar palavras. Primeiro segmento QUEUE_FLUSH, seguintes QUEUE_ADD, avanço somente após onDone real. O VTT usa android.speech.tts.TextToSpeech e não acessa o modelo interno do motor.'
)
write(main_path, main)

# ---------------------------------------------------------------------------
# 6) Canonical narrative profile: major scenes may be 5–6k characters
# ---------------------------------------------------------------------------
profile_path = JAVA / "HumanGmNarrativeProfile.java"
profile = read(profile_path)
profile = replace_once(
    profile,
    '        p.append("o necessário para criar presença, sem transformar o jogador em espectador por tempo excessivo.\\n");',
    '        p.append("o necessário para criar presença, sem transformar o jogador em espectador por tempo excessivo.\\n");\n'
    '        p.append("Nas CENAS PRINCIPAIS — especialmente abertura de aventura, grande chegada, revelação, clímax ou acontecimento equivalente — o alvo usual do usuário é aproximadamente 5.000 a 6.000 caracteres quando a cena sustenta essa densidade. Não aplique essa extensão a cenas rotineiras, combate batida a batida ou transições menores.\\n");',
    "human GM major scene length",
)
write(profile_path, profile)

# ---------------------------------------------------------------------------
# 7) Version
# ---------------------------------------------------------------------------
version_path = ROOT / "version.properties"
version = read(version_path)
version = version.replace("VERSION_NAME=9.0.33", "VERSION_NAME=9.0.34")
version = version.replace("VERSION_CODE=90033", "VERSION_CODE=90034")
write(version_path, version)

print("PF2 R33 Restov/Kokoro patch applied successfully.")
