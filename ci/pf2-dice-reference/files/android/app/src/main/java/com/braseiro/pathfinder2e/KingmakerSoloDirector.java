package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Camada de leitura do modo Kingmaker Solo.
 *
 * Não resolve regras, não altera Reino e não cria uma segunda autoridade.
 * O objetivo é condensar o estado persistido em sinais jogáveis para a UI e
 * para Barbara, mantendo a administração técnica fora do fluxo principal.
 */
public final class KingmakerSoloDirector {
    private final AppDb db;
    private final KingmakerRepository repository;

    public KingmakerSoloDirector(AppDb database, KingmakerRepository repo){
        if(database==null||repo==null)throw new IllegalArgumentException("Kingmaker Solo sem dependências.");
        db=database;
        repository=repo;
    }

    public JSONObject snapshot(){
        JSONObject out=new JSONObject();
        try{
            JSONObject kingdom=repository.state();
            JSONObject story=db.getStoryState();
            JSONObject scene=db.getSceneState();
            JSONObject map=db.getMapState();
            JSONObject turn=repository.activeTurn();
            JSONObject activeCamp=activeCamp();
            List<JSONObject> companions=repository.companions();
            List<JSONObject> allPendingEvents=repository.events("pending");
            java.util.ArrayList<JSONObject> pendingEvents=new java.util.ArrayList<JSONObject>(),companionEvents=new java.util.ArrayList<JSONObject>(),explorationEvents=new java.util.ArrayList<JSONObject>();
            for(JSONObject event:allPendingEvents){String type=event.optString("event_type");if("companion".equalsIgnoreCase(type)){if(companionEventIsPlayable(event,companions))companionEvents.add(event);}else if("exploration".equalsIgnoreCase(type))explorationEvents.add(event);else pendingEvents.add(event);}
            List<JSONObject> activeWars=repository.warEncounters("active");
            JSONObject kingdomGuide=kingdomTurnGuide(turn,pendingEvents);
            JSONObject companionFocus=companionFocus(companions,companionEvents);
            JSONObject explorationGuide=explorationGuide(map,activeCamp,explorationEvents);
            JSONObject adventureGuide=adventureGuide(story,map,scene,turn,activeCamp,companions,companionEvents,explorationEvents,activeWars,kingdomGuide,explorationGuide);
            JSONObject warGuide=warGuide(activeWars);
            JSONObject campGuide=campGuide(activeCamp,companions);
            JSONObject continuity=continuityCheckpoint(kingdom,story,map,scene,turn,activeCamp,companions,companionEvents,explorationEvents,activeWars,kingdomGuide,explorationGuide);

            JSONArray attention=new JSONArray();
            if(turn.length()>0&&"active".equals(turn.optString("status"))){
                JSONObject resource=parseObject(turn.optString("resource_roll_json","{}"));
                if("UPKEEP".equals(turn.optString("phase"))&&!resource.optBoolean("consumption_resolved")){
                    attention.put(item("kingdom_consumption","O Reino precisa decidir como cobrir o Consumo deste turno.","kingdom"));
                }else{
                    attention.put(item("kingdom_phase","Turno do Reino em "+KingmakerService.phaseLabel(turn.optString("phase"))+".","kingdom"));
                }
            }
            if(pendingEvents.size()>0)attention.put(item("kingdom_event",pendingEvents.size()+" evento(s) do Reino aguardando resolução.","kingdom"));
            if(activeWars.size()>0)attention.put(item("war",activeWars.size()+" batalha(s) em massa em andamento.","warfare"));
            if(explorationEvents.size()>0)attention.put(item("exploration",explorationEvents.size()+" exploração(ões) em cena aguardando resultado.","atlas"));
            if(activeCamp.length()>0){
                String phase=activeCamp.optString("phase","CAMP_ACTIVITIES");
                attention.put(item("camp","Acampamento ativo: "+campPhaseLabel(phase)+".","camp"));
            }

            int availableCompanions=0;
            for(JSONObject companion:companions)if(companion.optInt("available",1)==1)availableCompanions++;

            String hex=map.optString("current_hex_id",scene.optString("hex_id"));
            String weather=scene.optString("weather",map.optString("weather"));
            String period=scene.optString("time_of_day",scene.optString("period"));
            int day=scene.optInt("day_number",1);

            JSONObject world=new JSONObject()
                    .put("hex",hex)
                    .put("day",day)
                    .put("period",period)
                    .put("weather",weather)
                    .put("scene",scene.optString("title",scene.optString("scene_name")));

            JSONObject counts=new JSONObject()
                    .put("claimed_hexes",repository.claimedHexCount())
                    .put("settlements",repository.settlements().size())
                    .put("armies",repository.armies().size())
                    .put("companions",companions.size())
                    .put("available_companions",availableCompanions)
                    .put("pending_events",pendingEvents.size())
                    .put("pending_companion_events",companionEvents.size())
                    .put("pending_exploration_events",explorationEvents.size())
                    .put("active_wars",activeWars.size());

            out.put("enabled",repository.enabled())
                    .put("story_started",story.optInt("started")==1)
                    .put("kingdom_name",kingdom.optString("kingdom_name","Reino em formação"))
                    .put("kingdom_level",kingdom.optInt("level",1))
                    .put("turn_number",kingdom.optInt("turn_number",0))
                    .put("turn_phase",turn.length()>0?turn.optString("phase"):kingdom.optString("turn_phase","COMPLETE"))
                    .put("world",world)
                    .put("counts",counts)
                    .put("attention",attention)
                    .put("active_camp",activeCamp)
                    .put("companions",companionBriefs(companions))
                    .put("companion_focus",companionFocus)
                    .put("exploration_guide",explorationGuide)
                    .put("adventure_guide",adventureGuide)
                    .put("next_action",KingmakerSoloFlowRouter.actionFor(adventureGuide.optString("next_scene")))
                    .put("war_guide",warGuide)
                    .put("camp_guide",campGuide)
                    .put("continuity_checkpoint",continuity)
                    .put("kingdom_turn_guide",kingdomGuide)
                    .put("campaign_flow",campaignFlow(story,map,turn,activeCamp,pendingEvents,companionEvents,explorationEvents,activeWars,companions,kingdomGuide,explorationGuide))
                    .put("play_priority",playPriority(story,turn,activeCamp,pendingEvents,companionEvents,explorationEvents,activeWars));
        }catch(Exception e){
            try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){}
        }
        return out;
    }

    public String masterBrief(){
        JSONObject s=snapshot();
        JSONObject world=s.optJSONObject("world");
        JSONObject counts=s.optJSONObject("counts");
        JSONArray attention=s.optJSONArray("attention");
        StringBuilder b=new StringBuilder();
        b.append("KINGMAKER SOLO — PRIORIDADE DE JOGO: ").append(s.optString("play_priority","aventura")).append(". ");
        if(world!=null){
            if(world.optString("hex").length()>0)b.append("Hex atual ").append(world.optString("hex")).append(". ");
            if(world.optString("weather").length()>0)b.append("Clima ").append(world.optString("weather")).append(". ");
            if(world.optString("period").length()>0)b.append("Período ").append(world.optString("period")).append(". ");
        }
        if(counts!=null)b.append("Companheiros disponíveis ").append(counts.optInt("available_companions")).append("; assentamentos ").append(counts.optInt("settlements")).append("; território ").append(counts.optInt("claimed_hexes")).append(" hexes. ");
        JSONObject guide=s.optJSONObject("kingdom_turn_guide");if(guide!=null&&guide.optBoolean("active"))b.append("Próxima decisão do Reino: ").append(guide.optString("title")).append(". ");
        JSONObject focus=s.optJSONObject("companion_focus");if(focus!=null&&focus.length()>0)b.append("Companheiro em foco: ").append(focus.optString("name")).append(" — ").append(focus.optString("personal_hook",focus.optString("assignment"))).append(". ");
        if(attention!=null&&attention.length()>0){
            b.append("Pendências mecânicas: ");
            for(int i=0;i<attention.length();i++){
                JSONObject item=attention.optJSONObject(i);
                if(item==null)continue;
                if(i>0)b.append(" | ");
                b.append(item.optString("text"));
            }
            b.append(' ');
        }
        b.append("Não interrogue o jogador sobre JSON, RP, CD ou manutenção técnica; traduza o estado em escolhas ficcionais e deixe mutações mecânicas para o motor local.");
        return b.toString();
    }


    private JSONObject kingdomTurnGuide(JSONObject turn,List<JSONObject> events)throws Exception{
        JSONObject guide=new JSONObject();
        if(turn==null||turn.length()==0||!"active".equals(turn.optString("status"))){
            return guide.put("active",false).put("action","start_turn").put("phase","COMPLETE")
                    .put("title","Iniciar o próximo turno do Reino")
                    .put("text","O motor pode rolar os dados de recurso localmente e abrir somente a primeira decisão humana. Dados físicos continuam opcionais.");
        }
        String phase=turn.optString("phase","UPKEEP").toUpperCase(Locale.US);
        List<JSONObject> activities=repository.turnActivities(turn.optLong("id",0));int phaseActivities=0;
        for(JSONObject activity:activities)if(phase.equals(activity.optString("phase")))phaseActivities++;
        guide.put("active",true).put("turn_id",turn.optLong("id")).put("turn_number",turn.optInt("turn_number"))
                .put("phase",phase).put("phase_label",KingmakerService.phaseLabel(phase)).put("activities_done",phaseActivities);
        if("UPKEEP".equals(phase)){
            JSONObject resource=parseObject(turn.optString("resource_roll_json","{}"));
            if(!resource.optBoolean("consumption_resolved"))return guide.put("action","resolve_consumption").put("title","Decidir como cobrir o Consumo")
                    .put("text","Escolha alimento, recursos do Reino ou a consequência de instabilidade; os números ficam no motor local.");
            return guide.put("action","advance_phase").put("title","Concluir Manutenção").put("text","Consumo já resolvido. Avance para Comércio sem preencher outra ficha.");
        }
        if("COMMERCE".equals(phase)||"ACTIVITY".equals(phase)){
            String label=KingmakerService.phaseLabel(phase);
            return guide.put("action","phase_choice").put("title","Continuar "+label)
                    .put("text",phaseActivities+" atividade(s) registrada(s) nesta fase. Resolva outra apenas se a ficção pedir, ou conclua a fase; o app não inventa quantidade obrigatória.");
        }
        if("EVENT".equals(phase)){
            if(events!=null&&!events.isEmpty()){JSONObject event=events.get(0);return guide.put("action","resolve_event").put("event_id",event.optLong("id"))
                    .put("event_title",event.optString("title")).put("title","Resolver evento do Reino")
                    .put("text","Evento pendente: "+event.optString("title")+". Traga a consequência para a cena e aplique somente efeitos confirmados pela fonte.");}
            return guide.put("action","advance_phase").put("title","Concluir fase de Evento e fechar o turno")
                    .put("text","Não há evento pendente. O motor pode concluir o turno e preservar o estado para a volta à aventura.");
        }
        return guide.put("action","advance_phase").put("title","Continuar turno do Reino").put("text","Avance a fase atual usando o estado persistido.");
    }

    /**
     * Pacote de continuidade aventura -> camping/companions -> Reino -> aventura.
     * É deliberadamente read-only: traduz estado persistido em uma próxima cena
     * concreta, sem rolar dados, mover token, resolver evento ou alterar Reino.
     */
    private JSONObject adventureGuide(JSONObject story,JSONObject map,JSONObject scene,JSONObject turn,JSONObject camp,List<JSONObject> companions,List<JSONObject> companionEvents,List<JSONObject> explorationEvents,List<JSONObject> wars,JSONObject kingdomGuide,JSONObject explorationGuide)throws Exception{
        JSONObject out=new JSONObject();
        boolean started=story.optInt("started") == 1, activeTurn=turn!=null&&turn.length()>0&&"active".equals(turn.optString("status"));
        String hex=map.optString("current_hex_id",scene.optString("hex_id"));
        out.put("active",started).put("hex",hex).put("weather",scene.optString("weather",map.optString("weather"))).put("period",scene.optString("time_of_day",scene.optString("period")));
        JSONArray party=new JSONArray();
        for(JSONObject c:companions){if(c.optInt("available",1)!=1)continue;party.put(new JSONObject().put("key",c.optString("companion_key")).put("name",c.optString("name")).put("assignment",c.optString("assignment","reserve")).put("personal_status",c.optString("personal_quest_status","unknown")));}
        out.put("active_companions",party).put("companion_scene_pending",companionEvents!=null&&!companionEvents.isEmpty()).put("exploration_scene_pending",explorationEvents!=null&&!explorationEvents.isEmpty()).put("war_active",wars!=null&&!wars.isEmpty()).put("camp_active",camp!=null&&camp.length()>0);
        if(!started)return out.put("next_scene","adventure_start").put("title","Começar no Atlas").put("text","Abra a primeira cena no estado persistido; Barbara apresenta o problema sem criar mecânica.");
        if(camp!=null&&camp.length()>0){boolean encounter="RANDOM_ENCOUNTER".equals(camp.optString("phase"));return out.put("next_scene",encounter?"camp_encounter":"camp").put("title",encounter?"Resolver encontro do acampamento":"Continuar o acampamento").put("text","O acampamento preserva hex, clima, companions e horas já registradas. Resolva a ficção antes de qualquer avanço.");}
        if(wars!=null&&!wars.isEmpty())return out.put("next_scene","war").put("title","Batalha em andamento").put("text","Volte à batalha persistida; HP, moral e alvos continuam sob a autoridade do motor de guerra.");
        if(explorationEvents!=null&&!explorationEvents.isEmpty())return out.put("next_scene","exploration_resolution").put("title","Fechar a exploração atual").put("text","Conclua a cena no hex de origem e só então confirme reconhecimento ou limpeza.");
        if(companionEvents!=null&&!companionEvents.isEmpty())return out.put("next_scene","companion").put("title","Continuar a cena pessoal").put("text",companionEvents.get(0).optString("title")+" permanece pendente; continue pela ficção sem editar influência ou Reino.");
        if(activeTurn)return out.put("next_scene","kingdom_bridge").put("title",kingdomGuide.optString("title","Continuar o Reino")).put("text",kingdomGuide.optString("text")+" Depois da decisão, retorne à aventura sem abrir manutenção técnica adicional.");
        String next=explorationGuide.optString("next_hex");
        if(next.length()>0)return out.put("next_scene","travel").put("title","Viajar para "+next).put("text","Use a rota já persistida e o motor PF2e existente; companions disponíveis seguem como participantes da cena.");
        return out.put("next_scene","free_exploration").put("title","Explorar e deixar o mundo responder").put("text","Escolha um vizinho canônico ou permaneça no hex atual. Companions podem gerar cenas pessoais organicamente; o próximo turno do Reino só entra quando a ficção alcançar downtime.");
    }

    /** Guias de guerra/camping e checkpoint são somente projeções do estado persistido. */
    private JSONObject warGuide(List<JSONObject> wars)throws Exception{
        JSONObject out=new JSONObject().put("active",wars!=null&&!wars.isEmpty());
        if(wars==null||wars.isEmpty())return out.put("action","return_to_adventure").put("title","Nenhuma batalha ativa");
        JSONObject war=wars.get(0);String active=war.optString("active_army_key");JSONArray roster=parseArray(war.optString("participants_json","[]")),participants=new JSONArray();boolean oppositionStanding=false;
        JSONObject activeArmy=repository.army(active);
        for(int i=0;i<roster.length();i++){Object raw=roster.opt(i);String key=raw instanceof JSONObject?((JSONObject)raw).optString("army_key"):String.valueOf(raw);JSONObject army=repository.army(key);if(army.length()==0)continue;boolean defeated=army.optInt("hp")<=0;participants.put(new JSONObject().put("army_key",key).put("name",army.optString("name")).put("allegiance",army.optString("allegiance")).put("hp",army.optInt("hp")).put("max_hp",army.optInt("max_hp")).put("ac",army.optInt("ac")).put("attack_modifier",army.optInt("attack_modifier")).put("morale_dc",army.optInt("morale_dc")).put("active",key.equals(active)).put("defeated",defeated));if(!key.equals(active)&&!defeated&&(activeArmy.optString("allegiance").length()==0||!activeArmy.optString("allegiance").equals(army.optString("allegiance"))))oppositionStanding=true;}
        return out.put("encounter_key",war.optString("encounter_key")).put("title",war.optString("name","Batalha")).put("battlefield_hex",war.optString("battlefield_hex")).put("round",war.optInt("round_number",1)).put("active_army_key",active).put("active_army",activeArmy.length()==0?new JSONObject():new JSONObject().put("army_key",active).put("name",activeArmy.optString("name")).put("hp",activeArmy.optInt("hp")).put("max_hp",activeArmy.optInt("max_hp")).put("ac",activeArmy.optInt("ac")).put("attack_modifier",activeArmy.optInt("attack_modifier")).put("morale_dc",activeArmy.optInt("morale_dc"))).put("participants",participants).put("opposition_standing",oppositionStanding).put("action",oppositionStanding?"choose_army_action":"confirm_battle_conclusion").put("next_action",oppositionStanding?"Resolver ataque, manobra ou moral":"Retornar à aventura").put("policy","Escolha a ação ficcional do exército ativo; ataque, dano, moral e término continuam nas autoridades mecânicas existentes.");
    }

    private JSONObject campGuide(JSONObject camp,List<JSONObject> companions)throws Exception{
        JSONObject out=new JSONObject().put("active",camp!=null&&camp.length()>0);if(camp==null||camp.length()==0)return out.put("action","camp_when_fiction_requires");
        JSONArray party=new JSONArray();for(JSONObject c:companions)if(c.optInt("available",1)==1)party.put(new JSONObject().put("key",c.optString("companion_key")).put("name",c.optString("name")).put("assignment",c.optString("assignment")));
        String phase=camp.optString("phase","CAMP_ACTIVITIES");boolean blocked="RANDOM_ENCOUNTER".equals(phase);JSONObject encounter=parseObject(camp.optString("encounter_json","{}"));return out.put("camp_key",camp.optString("camp_key")).put("hex",camp.optString("hex_id")).put("day",camp.optInt("day_number",1)).put("weather",camp.optString("weather")).put("zone",camp.optString("zone_name")).put("hours_elapsed",camp.optInt("elapsed_hours")).put("elapsed_hours",camp.optInt("elapsed_hours")).put("phase",phase).put("status",camp.optString("status","active")).put("companions",party).put("encounter_blocking",blocked).put("encounter",blocked?encounter:new JSONObject()).put("action",blocked?"resolve_encounter":"continue_camp_hour").put("next_action",blocked?"Resolver o encontro persistido antes de retomar":"Registrar a próxima hora ou concluir o acampamento").put("policy","Preserve vigias, atividades, clima e horas; encontro interrompe o descanso e deve ser resolvido antes da retomada.");
    }

    private JSONObject continuityCheckpoint(JSONObject kingdom,JSONObject story,JSONObject map,JSONObject scene,JSONObject turn,JSONObject camp,List<JSONObject> companions,List<JSONObject> companionEvents,List<JSONObject> explorationEvents,List<JSONObject> wars,JSONObject kingdomGuide,JSONObject explorationGuide)throws Exception{
        JSONArray activeCompanions=new JSONArray(),companionStates=new JSONArray();
        for(JSONObject c:companions){
            if(c.optInt("available",1)==1)activeCompanions.put(c.optString("companion_key"));
            companionStates.put(new JSONObject().put("companion_key",c.optString("companion_key")).put("available",c.optInt("available",1)).put("assignment",c.optString("assignment","reserve")).put("influence",c.optInt("influence")).put("attitude",c.optString("attitude","indifferent")).put("personal_quest_status",c.optString("personal_quest_status","unknown")).put("personal_quest_notes",c.optString("personal_quest_notes")));
        }
        JSONArray companionEventStates=eventStates(companionEvents),explorationEventStates=eventStates(explorationEvents),warStates=new JSONArray();
        if(wars!=null)for(JSONObject war:wars){
            JSONArray roster=parseArray(war.optString("participants_json","[]")),armyStates=new JSONArray();
            for(int i=0;i<roster.length();i++){Object raw=roster.opt(i);String key=raw instanceof JSONObject?((JSONObject)raw).optString("army_key"):String.valueOf(raw);JSONObject army=repository.army(key);if(army.length()>0)armyStates.put(new JSONObject().put("army_key",key).put("hp",army.optInt("hp")).put("max_hp",army.optInt("max_hp")).put("conditions_json",army.optString("conditions_json","[]")).put("location_hex",army.optString("location_hex")));}
            warStates.put(new JSONObject().put("encounter_key",war.optString("encounter_key")).put("round_number",war.optInt("round_number",1)).put("active_army_key",war.optString("active_army_key")).put("status",war.optString("status")).put("battlefield_hex",war.optString("battlefield_hex")).put("participants_json",roster).put("armies",armyStates));
        }
        JSONObject x=new JSONObject().put("story_started",story.optInt("started")).put("current_hex",map.optString("current_hex_id",scene.optString("hex_id"))).put("destination_hex",map.optString("destination_hex_id")).put("route_json",parseArray(map.optString("route_json","[]"))).put("scene_day",scene.optInt("day_number",1)).put("scene_period",scene.optString("time_of_day",scene.optString("period"))).put("scene_weather",scene.optString("weather",map.optString("weather")))
                .put("kingdom_turn_number",kingdom.optInt("turn_number",0)).put("kingdom_turn_phase",kingdom.optString("turn_phase","COMPLETE")).put("turn_id",turn.optLong("id",0)).put("turn_number",turn.optInt("turn_number",kingdom.optInt("turn_number",0))).put("turn_phase",turn.optString("phase",kingdom.optString("turn_phase","COMPLETE"))).put("turn_status",turn.optString("status",turn.length()>0?"active":"complete")).put("turn_resource_roll_json",parseObject(turn.optString("resource_roll_json","{}")))
                .put("camp_key",camp.optString("camp_key")).put("camp_status",camp.optString("status")).put("camp_phase",camp.optString("phase")).put("camp_hex",camp.optString("hex_id")).put("camp_day",camp.optInt("day_number",0)).put("camp_hours",camp.optInt("elapsed_hours",0)).put("camp_encounter_json",parseObject(camp.optString("encounter_json","{}")))
                .put("active_companions",activeCompanions).put("companion_states",companionStates).put("pending_companion_events",companionEvents==null?0:companionEvents.size()).put("pending_companion_event_states",companionEventStates).put("pending_exploration_events",explorationEvents==null?0:explorationEvents.size()).put("pending_exploration_event_states",explorationEventStates)
                .put("active_wars",wars==null?0:wars.size()).put("active_war_states",warStates).put("kingdom_action",kingdomGuide.optString("action")).put("next_hex",explorationGuide.optString("next_hex"));
        return x.put("restore_contract","Após restore, turno do Reino, camping, companions, eventos, guerras, Atlas e próxima ação devem permanecer semanticamente equivalentes antes de qualquer nova ação do jogador.");
    }

    private static JSONArray eventStates(List<JSONObject> events)throws Exception{
        JSONArray out=new JSONArray();if(events==null)return out;
        for(JSONObject e:events)out.put(new JSONObject().put("event_key",e.optString("event_key")).put("event_type",e.optString("event_type")).put("status",e.optString("status")).put("source_ref",e.optString("source_ref")).put("title",e.optString("title")).put("notes",e.optString("notes")));
        return out;
    }

    private JSONObject companionFocus(List<JSONObject> companions,List<JSONObject> companionEvents)throws Exception{
        JSONObject fallback=new JSONObject();
        if(companionEvents!=null)for(JSONObject event:companionEvents){String source=event.optString("source_ref");if(!source.startsWith("companion:"))continue;String key=source.substring("companion:".length());for(JSONObject c:companions)if(c.optInt("available",1)==1&&key.equals(c.optString("companion_key"))){JSONObject focused=companionBrief(c);focused.put("pending_event_id",event.optLong("id")).put("pending_event_title",event.optString("title"));return focused;}}
        for(JSONObject c:companions){
            if(c.optInt("available",1)!=1)continue;
            if(fallback.length()==0)fallback=companionBrief(c);
            String hook=c.optString("personal_quest_notes").trim(),status=c.optString("personal_quest_status","unknown").toLowerCase(Locale.US);
            if(hook.length()>0&&!"complete".equals(status)&&!"completed".equals(status))return companionBrief(c);
        }
        return fallback;
    }

    private static boolean companionEventIsPlayable(JSONObject event,List<JSONObject> companions){return KingmakerSoloEventPolicy.playable(event,companions);}

    private JSONObject companionBrief(JSONObject c)throws Exception{
        JSONObject x=new JSONObject().put("key",c.optString("companion_key")).put("companion_key",c.optString("companion_key")).put("name",c.optString("name"))
                .put("assignment",c.optString("assignment","reserve")).put("attitude",c.optString("attitude","indifferent"))
                .put("influence",c.optInt("influence")).put("personal_quest_status",c.optString("personal_quest_status","unknown"));
        String quest=c.optString("personal_quest_notes").trim();if(quest.length()>0)x.put("personal_hook",quest);return x;
    }

    private JSONArray companionBriefs(List<JSONObject> companions)throws Exception{
        JSONArray out=new JSONArray();
        for(JSONObject c:companions){
            if(c.optInt("available",1)!=1)continue;
            out.put(companionBrief(c));
        }
        return out;
    }

    private JSONArray campaignFlow(JSONObject story,JSONObject map,JSONObject turn,JSONObject camp,List<JSONObject> events,List<JSONObject> companionEvents,List<JSONObject> explorationEvents,List<JSONObject> wars,List<JSONObject> companions,JSONObject kingdomGuide,JSONObject explorationGuide)throws Exception{
        JSONArray steps=new JSONArray();
        if(story.optInt("started")!=1){steps.put(step("adventure_start","Começar a aventura","Abra a cena no Atlas e deixe a ficção apresentar o primeiro problema.","adventure",false));return steps;}
        if(camp.length()>0){boolean encounter="RANDOM_ENCOUNTER".equals(camp.optString("phase"));steps.put(step(encounter?"camp_encounter":"camp",encounter?"Resolver o encontro do acampamento":"Concluir o acampamento",encounter?"O descanso está pausado por um encontro disparado. Jogue a cena e só depois retome as atividades.":"Resolva atividades, vigias e eventual encontro antes de avançar o dia.","camp",true));}
        if(wars!=null&&!wars.isEmpty())steps.put(step("war","Resolver a guerra em cena","Conduza a batalha ativa por rodadas; o motor local mantém HP, moral e estado dos exércitos.","warfare",true));
        if(events!=null&&!events.isEmpty())steps.put(step("event","Trazer o evento do Reino para a aventura","Apresente a consequência como cena antes de pedir qualquer resolução mecânica.","kingdom",true));
        if(companionEvents!=null&&!companionEvents.isEmpty())steps.put(step("companion_event","Continuar evento pessoal de companion",companionEvents.get(0).optString("title")+" está pendente e deve continuar como cena, não como edição de ficha.","adventure",true));
        if(explorationEvents!=null&&!explorationEvents.isEmpty())steps.put(step("exploration_event","Concluir a exploração em andamento",explorationEvents.get(0).optString("title")+" aguarda o resultado da cena antes de atualizar o estado do hex.","atlas",true));
        int active=0,quest=0;for(JSONObject c:companions){if(c.optInt("available",1)==1){active++;String q=c.optString("personal_quest_status","unknown");if(!"unknown".equals(q)&&!"complete".equals(q)&&!"completed".equals(q))quest++;}}
        if(active>0)steps.put(step("companions","Dar espaço aos companheiros",quest>0?quest+" arco(s) pessoal(is) podem emergir organicamente durante viagem, descanso ou retorno ao Reino.":"Use os companheiros disponíveis como participantes ativos da exploração, não como inventário.","adventure",false));
        if(explorationGuide!=null){String current=explorationGuide.optString("current_hex"),next=explorationGuide.optString("next_hex"),destination=explorationGuide.optString("destination_hex"),status=explorationGuide.optString("current_status","desconhecido");if(next.length()>0)steps.put(step("expedition_travel","Avançar a expedição para "+next,"O destino "+destination+" já está planejado. O movimento continua no motor PF2e existente, um hex por vez.","atlas",false));else if(current.length()>0&&("desconhecido".equals(status)||"reconnoiterado".equals(status)))steps.put(step("explore_current","Explorar o hex "+current,"Jogue a exploração em cena; reconhecimento e limpeza só são persistidos depois de confirmação explícita.","atlas",false));else{String exploreText=current.length()>0?"Escolha um dos vizinhos canônicos de "+current+" e planeje somente o próximo trecho.":"Defina primeiro uma posição válida no Atlas canônico.";steps.put(step("plan_expedition","Explorar pelo Atlas — planejar próximo trecho",exploreText,"atlas",false).put("legacy_key","explore"));}}
        if(turn.length()>0&&"active".equals(turn.optString("status")))steps.put(step("kingdom_turn",kingdomGuide.optString("title","Resolver o Reino quando a ficção pedir"),kingdomGuide.optString("text","Mostre apenas a próxima escolha humana; cálculos permanecem no motor."),"kingdom",false));
        else steps.put(step("kingdom_transition","Retornar ao Reino sem burocracia","Quando a aventura alcançar downtime apropriado, o modo guiado pode iniciar o próximo turno com rolagens locais automáticas e nenhuma ficha administrativa.","kingdom",false));
        return steps;
    }

    private static JSONObject step(String key,String title,String text,String target,boolean urgent)throws Exception{return new JSONObject().put("key",key).put("title",title).put("text",text).put("target",target).put("urgent",urgent);}

    private JSONObject activeCamp(){
        for(JSONObject camp:repository.camps(20))if("active".equals(camp.optString("status")))return camp;
        return new JSONObject();
    }

    private static String playPriority(JSONObject story,JSONObject turn,JSONObject camp,List<JSONObject> events,List<JSONObject> companionEvents,List<JSONObject> explorationEvents,List<JSONObject> wars){
        if(story.optInt("started")!=1)return "começar a aventura";
        if(camp.length()>0&&"RANDOM_ENCOUNTER".equals(camp.optString("phase")))return "resolver o encontro do acampamento";
        if(wars!=null&&!wars.isEmpty())return "resolver a batalha em andamento";
        if(events!=null&&!events.isEmpty())return "integrar o evento do Reino à ficção";
        if(companionEvents!=null&&!companionEvents.isEmpty())return "continuar o evento pessoal do companion";
        if(explorationEvents!=null&&!explorationEvents.isEmpty())return "concluir a exploração do hex atual";
        if(turn.length()>0&&"active".equals(turn.optString("status")))return "continuar a história e resolver a fase do Reino quando ela entrar em cena";
        return "aventura, exploração e companheiros";
    }

    private JSONObject explorationGuide(JSONObject map,JSONObject camp,List<JSONObject> explorationEvents)throws Exception{JSONObject out=new JSONObject();String current=map==null?"":map.optString("current_hex_id").trim().toUpperCase(Locale.US);String destination=map==null?"":map.optString("destination_hex_id").trim().toUpperCase(Locale.US);JSONArray route=parseArray(map==null?"[]":map.optString("route_json","[]"));String next=route.length()>0?route.optString(0,"").trim().toUpperCase(Locale.US):"";boolean pending=explorationEvents!=null&&!explorationEvents.isEmpty();out.put("current_hex",current).put("destination_hex",destination).put("next_hex",next).put("route_length",route.length()).put("position_locked",map!=null&&map.optInt("position_locked",0)==1).put("blocked_by_camp",camp!=null&&camp.length()>0).put("pending_event",pending);if(pending){JSONObject event=explorationEvents.get(0);String source=event.optString("source_ref");String pendingHex=explorationHex(source);out.put("pending_event_id",event.optLong("id",0)).put("pending_event_title",event.optString("title")).put("pending_event_source",source).put("pending_event_hex",pendingHex).put("pending_event_here",pendingHex.length()>0&&pendingHex.equals(current));}JSONObject overlay=current.length()==0?new JSONObject():repository.hex(current);out.put("current_status",hexStatus(overlay)).put("current_reconnoitered",overlay.optInt("reconnoitered",0)==1).put("current_cleared",overlay.optInt("cleared",0)==1);JSONArray neighbors=new JSONArray();if(current.length()>0){try{MapManifestGeometry geometry=MapManifestGeometry.parse(map.optString("manifest_json"));for(String id:geometry.neighbors(current)){JSONObject h=repository.hex(id);neighbors.put(new JSONObject().put("hex_id",id).put("status",hexStatus(h)).put("reconnoitered",h.optInt("reconnoitered",0)==1).put("cleared",h.optInt("cleared",0)==1).put("claimed",h.optInt("claimed",0)==1));}}catch(Exception ignored){out.put("atlas_warning","Manifesto do Atlas indisponível para vizinhança; não inventar hexes.");}}out.put("neighbors",neighbors).put("atlas_grounded",current.length()>0&&neighbors.length()>0);return out;}
    private static String explorationHex(String source){if(source==null)return "";String[] parts=source.split(":");return parts.length>=3&&"exploration".equalsIgnoreCase(parts[0])?parts[1].trim().toUpperCase(Locale.US):"";}
    private static String hexStatus(JSONObject h){if(h==null||h.length()==0)return "desconhecido";if(h.optInt("claimed",0)==1)return "reivindicado";if(h.optInt("cleared",0)==1)return "limpo";if(h.optInt("reconnoitered",0)==1)return "reconnoiterado";return "desconhecido";}

    private static JSONObject item(String key,String text,String target)throws Exception{
        return new JSONObject().put("key",key).put("text",text).put("target",target);
    }

    private static JSONObject parseObject(String raw){
        try{return new JSONObject(raw==null||raw.trim().length()==0?"{}":raw);}catch(Exception ignored){return new JSONObject();}
    }
    private static JSONArray parseArray(String raw){
        try{return new JSONArray(raw==null||raw.trim().length()==0?"[]":raw);}catch(Exception ignored){return new JSONArray();}
    }

    private static String campPhaseLabel(String phase){
        if("RANDOM_ENCOUNTER".equals(phase))return "encontro aleatório";
        if("COMPLETE".equals(phase))return "concluído";
        return "atividades de acampamento";
    }
}
