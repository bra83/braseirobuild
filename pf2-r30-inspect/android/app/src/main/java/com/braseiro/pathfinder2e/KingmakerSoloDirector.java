package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String phase=camp.optString("phase","CAMP_ACTIVITIES");boolean blocked="RANDOM_ENCOUNTER".equals(phase);JSONObject encounter=parseObject(camp.optString("encounter_json","{}"));
        int step=camp.optInt("step",1);
        String stepName="Seleção de Local";
        if(step==2)stepName="Preparar Acampamento";
        else if(step==3)stepName="Atividades de Acampamento";
        else if(step==4)stepName="Refeições Especiais e Culinária";
        else if(step==5)stepName="Dormir e Descansar";
        JSONObject meal=parseObject(camp.optString("meal_json","{}"));
        JSONArray buffs=parseArray(camp.optString("buffs_json","[]"));
        JSONObject rest=parseObject(camp.optString("rest_json","{}"));
        return out.put("camp_key",camp.optString("camp_key")).put("hex",camp.optString("hex_id")).put("day",camp.optInt("day_number",1)).put("weather",camp.optString("weather")).put("zone",camp.optString("zone_name")).put("hours_elapsed",camp.optInt("elapsed_hours")).put("elapsed_hours",camp.optInt("elapsed_hours")).put("phase",phase).put("step",step).put("step_name",stepName).put("status",camp.optString("status","active")).put("companions",party).put("encounter_blocking",blocked).put("encounter",blocked?encounter:new JSONObject()).put("meal",meal).put("active_buffs",buffs).put("rest",rest).put("action",blocked?"resolve_encounter":"continue_camp_hour").put("next_action",blocked?"Resolver o encontro persistido antes de retomar":"Registrar a próxima hora ou concluir o acampamento").put("policy","Preserve vigias, atividades, clima e horas; encontro interrompe o descanso e deve ser resolvido antes da retomada.");
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
                .put("attitude_label",KingmakerRulesEngine.attitudeLabel(c.optString("attitude","indifferent")))
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
        if("PREPARE_CAMP".equals(phase))return "preparar acampamento";
        if("CAMP_ACTIVITIES".equals(phase))return "atividades de acampamento";
        if("MEALS_AND_COOKING".equals(phase))return "refeições e culinária";
        if("SLEEP_AND_REST".equals(phase))return "dormir e descansar";
        if("COMPLETE".equals(phase))return "concluído";
        return "atividades de acampamento";
    }

    /**
     * Retorna a orientação da campanha ativa baseada no estado SQLite persistido.
     * Totalmente read-only, determinístico e em 100% pt-BR.
     */
    public JSONObject getActiveCampaignGuidance() {
        return getActiveCampaignGuidance(db != null ? db.getActiveMapProfile() : "pf2");
    }

    public JSONObject getActiveCampaignGuidance(String worldProfile) {
        JSONObject out = new JSONObject();
        try {
            String profile = (worldProfile == null || worldProfile.trim().length() == 0)
                    ? (db != null ? db.getActiveMapProfile() : "pf2")
                    : worldProfile.trim();

            List<JSONObject> activeQuestsList = db != null ? db.queryList(
                    "SELECT * FROM quests WHERE world_profile=? AND status IN ('active', 'in_progress', 'em_andamento') ORDER BY id ASC",
                    new String[]{profile}) : new ArrayList<JSONObject>();
            JSONArray activeQuests = new JSONArray();
            for (JSONObject q : activeQuestsList) {
                activeQuests.put(q);
            }

            int activeChapter = 1;
            String chapterKey = "";
            String chapterTitle = "";

            if (!activeQuestsList.isEmpty()) {
                JSONObject firstQuest = activeQuestsList.get(0);
                chapterKey = firstQuest.optString("chapter_key", "");
                activeChapter = resolveChapterNumber(profile, chapterKey);
            } else {
                List<JSONObject> unstartedQuests = db != null ? db.queryList(
                        "SELECT * FROM quests WHERE world_profile=? AND status IN ('unstarted', 'pending', 'pendente') ORDER BY id ASC LIMIT 1",
                        new String[]{profile}) : new ArrayList<JSONObject>();
                if (!unstartedQuests.isEmpty()) {
                    chapterKey = unstartedQuests.get(0).optString("chapter_key", "");
                    activeChapter = resolveChapterNumber(profile, chapterKey);
                } else if (db != null) {
                    JSONObject importedCh = db.queryOne(
                            "SELECT chapter_number, chapter_key, title FROM kingmaker_campaign_chapters WHERE world_profile=? ORDER BY chapter_number ASC LIMIT 1",
                            new String[]{profile});
                    if (importedCh.length() > 0) {
                        activeChapter = importedCh.optInt("chapter_number", 1);
                        chapterKey = importedCh.optString("chapter_key", "");
                        chapterTitle = importedCh.optString("title", "");
                    }
                }
            }

            if (chapterTitle.length() == 0 && chapterKey.length() > 0 && db != null) {
                JSONObject chInfo = db.queryOne(
                        "SELECT title, chapter_number FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?",
                        new String[]{profile, chapterKey});
                if (chInfo.length() > 0) {
                    chapterTitle = chInfo.optString("title", "");
                    if (chInfo.has("chapter_number")) {
                        activeChapter = chInfo.optInt("chapter_number", activeChapter);
                    }
                }
            }
            if (chapterTitle.length() == 0) {
                chapterTitle = defaultChapterTitle(activeChapter);
            }

            String currentStage = "";
            if (!activeQuestsList.isEmpty()) {
                currentStage = activeQuestsList.get(0).optString("stage", "").trim();
                if (currentStage.length() == 0) {
                    currentStage = activeQuestsList.get(0).optString("objective", "").trim();
                }
            }
            if (currentStage.length() == 0) {
                currentStage = defaultChapterStage(activeChapter);
            }

            JSONObject snap = snapshot();
            JSONObject advGuide = snap.optJSONObject("adventure_guide");
            String recommendedScene = advGuide != null ? advGuide.optString("next_scene", "free_exploration") : "free_exploration";
            String sceneAction = KingmakerSoloFlowRouter.actionFor(recommendedScene);

            String guidanceText = buildGuidanceText(activeChapter, chapterTitle, currentStage, activeQuestsList, recommendedScene, sceneAction);

            out.put("active_chapter", activeChapter)
               .put("current_stage", currentStage)
               .put("active_quests", activeQuests)
               .put("recommended_scene", recommendedScene)
               .put("scene_action", sceneAction)
               .put("guidance_text", guidanceText)
               .put("chapter_title", chapterTitle)
               .put("world_profile", profile);

        } catch (Exception e) {
            try {
                out.put("error", String.valueOf(e.getMessage()));
                out.put("active_chapter", 1);
                out.put("current_stage", "exploracao_inicial");
                out.put("active_quests", new JSONArray());
                out.put("recommended_scene", "free_exploration");
                out.put("scene_action", KingmakerSoloFlowRouter.FREE_EXPLORATION);
                out.put("guidance_text", "Explore as Terras Roubadas e estabeleça sua base nas fronteiras.");
            } catch (Exception ignored) {}
        }
        return out;
    }

    public JSONArray getAvailableRumors(int currentChapter) {
        return getAvailableRumors(db != null ? db.getActiveMapProfile() : "pf2", currentChapter);
    }

    public JSONArray getAvailableRumors(String worldProfile, int currentChapter) {
        JSONArray out = new JSONArray();
        try {
            String profile = (worldProfile == null || worldProfile.trim().length() == 0)
                    ? (db != null ? db.getActiveMapProfile() : "pf2")
                    : worldProfile.trim();

            List<CanonicalRumor> catalog = getCanonicalRumorsCatalog(currentChapter);
            for (CanonicalRumor r : catalog) {
                boolean resolved = isRumorResolved(profile, r);
                JSONObject item = new JSONObject();
                item.put("rumor_id", r.rumorId);
                item.put("chapter", r.chapter);
                item.put("source_poi", r.sourcePoi);
                item.put("text", r.text);
                item.put("resolved", resolved);
                out.put(item);
            }

            if (db != null) {
                List<JSONObject> dbRumors = db.queryList(
                        "SELECT * FROM rumors WHERE world_profile=? ORDER BY id ASC",
                        new String[]{profile});
                for (JSONObject dbr : dbRumors) {
                    String rKey = dbr.optString("rumor_key", "");
                    boolean alreadyPresent = false;
                    for (CanonicalRumor cr : catalog) {
                        if (cr.rumorId.equalsIgnoreCase(rKey)) {
                            alreadyPresent = true;
                            break;
                        }
                    }
                    if (alreadyPresent) continue;

                    int ch = dbr.optInt("chapter", currentChapter > 0 ? currentChapter : 1);
                    if (currentChapter > 0 && ch != currentChapter && dbr.has("chapter")) {
                        continue;
                    }

                    String status = dbr.optString("status", "circulating");
                    boolean resolved = "resolved".equalsIgnoreCase(status) || "faded".equalsIgnoreCase(status) || "concluded".equalsIgnoreCase(status);
                    String sourcePoi = dbr.optString("origin_location", dbr.optString("source_poi", "poi_stolen_lands"));

                    JSONObject item = new JSONObject();
                    item.put("rumor_id", rKey.length() > 0 ? rKey : "rumor_db_" + dbr.optLong("id"));
                    item.put("chapter", ch);
                    item.put("source_poi", sourcePoi);
                    item.put("text", dbr.optString("statement", dbr.optString("text", "")));
                    item.put("resolved", resolved);
                    out.put(item);
                }
            }

        } catch (Exception ignored) {}
        return out;
    }

    private int resolveChapterNumber(String profile, String chapterKey) {
        if (chapterKey == null || chapterKey.trim().length() == 0) return 1;
        if (db != null) {
            JSONObject ch = db.queryOne(
                    "SELECT chapter_number FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?",
                    new String[]{profile, chapterKey.trim()});
            if (ch.length() > 0 && ch.has("chapter_number")) {
                return ch.optInt("chapter_number", 1);
            }
        }
        Matcher m = Pattern.compile("chapter_0?(\\d+)").matcher(chapterKey);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return 1;
    }

    private static String defaultChapterTitle(int chapter) {
        switch (chapter) {
            case 1: return "Terras Roubadas - Posto Avançado de Oleg";
            case 2: return "O Lorde Cervo e a Fundação da Baronia";
            case 3: return "Rios Correm Vermelhos";
            case 4: return "O Culto da Florada";
            case 5: return "O Desaparecimento de Varnhold";
            case 6: return "Sangue por Sangue";
            case 7: return "Guerra dos Reis do Rio";
            case 8: return "Aqueles que Espreitam Abaixo";
            case 9: return "O Som de Mil Gritos";
            case 10: return "A Maldição do Rei Lanterna";
            case 11: return "Atlas Regional das Terras Roubadas";
            case 12: return "Histórias e Missões dos Companheiros";
            default: return "Campanha Kingmaker - Capítulo " + chapter;
        }
    }

    private static String defaultChapterStage(int chapter) {
        switch (chapter) {
            case 1: return "exploracao_das_terras_roubadas";
            case 2: return "confronto_com_lorde_cervo";
            case 3: return "investigacao_da_ameaca_troll";
            case 4: return "contencao_da_florada_feerica";
            case 5: return "investigacao_do_desaparecimento";
            case 6: return "confronto_com_senhores_dos_tigres";
            case 7: return "disputa_politica_em_pitax";
            case 8: return "incursao_as_terras_negras";
            case 9: return "infiltracao_na_casa_no_limiar_do_tempo";
            case 10: return "confronto_com_rei_lanterna";
            case 11: return "mapeamento_dos_hexes";
            case 12: return "resolucao_dos_arcos_pessoais";
            default: return "exploracao_ativa";
        }
    }

    private static String buildGuidanceText(int chapter, String chapterTitle, String stage, List<JSONObject> activeQuests, String recommendedScene, String sceneAction) {
        StringBuilder sb = new StringBuilder();
        sb.append("Capítulo ").append(chapter).append(": ").append(chapterTitle).append(". ");
        if (activeQuests != null && !activeQuests.isEmpty()) {
            JSONObject q = activeQuests.get(0);
            sb.append("Missão ativa prioritária: ").append(q.optString("title")).append(". ");
            String obj = q.optString("objective", "").trim();
            if (obj.length() > 0) {
                sb.append("Objetivo: ").append(obj).append(" ");
            }
            if (activeQuests.size() > 1) {
                sb.append("(").append(activeQuests.size()).append(" missões ativas no total). ");
            }
        } else {
            sb.append("Nenhuma missão principal em andamento. Explore hexágonos desconhecidos, ouça rumores locais ou visite entrepostos para descobrir novos ganchos. ");
        }
        sb.append("Estágio: ").append(stage).append(". ");
        sb.append("Próxima cena sugerida: ");
        if ("travel".equals(recommendedScene)) {
            sb.append("viagem pelo Atlas em direção ao objetivo.");
        } else if ("camp".equals(recommendedScene)) {
            sb.append("descanso e atividades de acampamento.");
        } else if ("camp_encounter".equals(recommendedScene)) {
            sb.append("resolver o encontro repentino no acampamento antes do descanso.");
        } else if ("war".equals(recommendedScene)) {
            sb.append("conduzir a batalha de exércitos em andamento.");
        } else if ("companion".equals(recommendedScene)) {
            sb.append("cena narrativa de companheiro.");
        } else if ("exploration_resolution".equals(recommendedScene)) {
            sb.append("concluir o reconhecimento ou combate no hexágono atual.");
        } else if ("kingdom_bridge".equals(recommendedScene)) {
            sb.append("decisão pendente no turno do Reino.");
        } else if ("adventure_start".equals(recommendedScene)) {
            sb.append("iniciar a aventura no posto comercial de Oleg.");
        } else {
            sb.append("exploração livre dos Ermos das Terras Roubadas.");
        }
        return sb.toString();
    }

    private boolean isRumorResolved(String profile, CanonicalRumor r) {
        if (db == null) return false;
        try {
            JSONObject dbRumor = db.queryOne(
                    "SELECT status FROM rumors WHERE world_profile=? AND (rumor_key=? OR rumor_key=?)",
                    new String[]{profile, r.rumorId, "rumor:" + r.rumorId});
            if (dbRumor.length() > 0) {
                String st = dbRumor.optString("status", "");
                if ("resolved".equalsIgnoreCase(st) || "faded".equalsIgnoreCase(st) || "concluded".equalsIgnoreCase(st)) {
                    return true;
                }
            }

            if (r.associatedQuestKey != null && r.associatedQuestKey.length() > 0) {
                JSONObject quest = db.queryOne(
                        "SELECT status FROM quests WHERE world_profile=? AND (quest_key=? OR quest_key LIKE ?)",
                        new String[]{profile, r.associatedQuestKey, "%" + r.associatedQuestKey + "%"});
                if (quest.length() > 0 && "completed".equalsIgnoreCase(quest.optString("status"))) {
                    return true;
                }
            }

            if (repository != null) {
                JSONObject poi = db.queryOne(
                        "SELECT hex_id FROM campaign_pois WHERE world_profile=? AND (poi_key=? OR poi_id=? OR poi_id=?)",
                        new String[]{profile, r.sourcePoi, r.sourcePoi, profile + ":" + r.sourcePoi});
                if (poi.length() > 0) {
                    String hexId = poi.optString("hex_id", "");
                    if (hexId.length() > 0) {
                        JSONObject hex = repository.hex(hexId);
                        if (hex.length() > 0 && hex.optInt("cleared", 0) == 1) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static final class CanonicalRumor {
        final String rumorId;
        final int chapter;
        final String sourcePoi;
        final String text;
        final String associatedQuestKey;

        CanonicalRumor(String rumorId, int chapter, String sourcePoi, String text, String associatedQuestKey) {
            this.rumorId = rumorId;
            this.chapter = chapter;
            this.sourcePoi = sourcePoi;
            this.text = text;
            this.associatedQuestKey = associatedQuestKey;
        }
    }

    private static List<CanonicalRumor> getCanonicalRumorsCatalog(int targetChapter) {
        List<CanonicalRumor> all = new ArrayList<CanonicalRumor>();

        // Capítulo 1: Terras Roubadas
        all.add(new CanonicalRumor("rumor_ch1_01", 1, "poi_oleg", "Bandoleiros liderados por Happs Bydon têm extorquido o entreposto de Oleg e ameaçado colonos.", "q_oleg"));
        all.add(new CanonicalRumor("rumor_ch1_02", 1, "poi_bokken_hut", "O ermitão Bokken precisa de amoras-espinhosas frescas colhidas em uma ravina para suas poções alquímicas.", "q_bokken"));
        all.add(new CanonicalRumor("rumor_ch1_03", 1, "poi_tuskgutter_lair", "Um javali monstruoso chamado Presa-de-Javali assola as florestas ao sul do Vau do Espinheiro.", "q_tuskgutter"));
        all.add(new CanonicalRumor("rumor_ch1_04", 1, "poi_kressle_camp", "A bandoleira Kressle comanda um acampamento fortificado escondido nas curvas do Rio Thorn.", "q_kressle"));

        // Capítulo 2: Lorde Cervo e Fundação da Baronia
        all.add(new CanonicalRumor("rumor_ch2_01", 2, "poi_old_sycamore", "Goblins e ácaros travam uma guerra sangrenta nas galerias subterrâneas do Sicômoro Antigo.", "q_sycamore"));
        all.add(new CanonicalRumor("rumor_ch2_02", 2, "poi_temple_of_elk", "Uma fonte sagrada corrompida no Templo do Alce aguarda purificação espiritual.", "q_temple_elk"));
        all.add(new CanonicalRumor("rumor_ch2_03", 2, "poi_candlemere_island", "Fogos-fátuos misteriosos dançam sobre a torre em ruínas na Ilha de Candlemere.", "q_candlemere"));
        all.add(new CanonicalRumor("rumor_ch2_04", 2, "poi_stag_lord_fort", "O temido Lorde Cervo governa bandoleiros com punho de ferro em um forte de paliçada no lago.", "q_stag_lord"));

        // Capítulo 3: Rios Correm Vermelhos
        all.add(new CanonicalRumor("rumor_ch3_01", 3, "poi_hargulka_stronghold", "Trolls da fortaleza de Hargulka desenvolveram resistência a fogo e ameaçam o Reino.", "q_hargulka"));
        all.add(new CanonicalRumor("rumor_ch3_02", 3, "poi_ford_jubilost", "O estudioso gnomo Jubilost Narthropple está retido com sua carruagem após ataque fluvial.", "q_jubilost"));
        all.add(new CanonicalRumor("rumor_ch3_03", 3, "poi_swamp_witch_elga", "A bruxa do pântano Elga negocia segredos ancestrais em troca de favores bizarros.", "q_swamp_witch"));

        // Capítulo 4: Culto da Florada
        all.add(new CanonicalRumor("rumor_ch4_01", 4, "poi_womb_of_lamashtu", "Criaturas aberrantes nascidas no Útero de Lamashtu espalham contaminação e medo.", "q_womb_lamashtu"));
        all.add(new CanonicalRumor("rumor_ch4_02", 4, "poi_bloom_spring", "A florada feérica provoca mutações e desordem mágica em assentamentos próximos.", "q_bloom"));

        // Capítulo 5: O Desaparecimento de Varnhold
        all.add(new CanonicalRumor("rumor_ch5_01", 5, "poi_varnhold_abandoned", "A colônia inteira de Varnhold desapareceu misteriosamente, deixando edifícios intactos.", "q_varnhold"));
        all.add(new CanonicalRumor("rumor_ch5_02", 5, "poi_nomen_centaurs", "Centauros Nomen vigiam as estepes orientais e reagem com hostilidade a invasões de terras sagradas.", "q_centaurs"));
        all.add(new CanonicalRumor("rumor_ch5_03", 5, "poi_vordakai_tomb", "Uma tumba ciclópea guarda o necromante Vordakai e jarros de almas de tempos imemoriais.", "q_vordakai"));

        // Capítulo 6: Sangue por Sangue
        all.add(new CanonicalRumor("rumor_ch6_01", 6, "poi_armag_tomb", "Armag, campeão dos Senhores dos Tigres, busca a tumba ancestral e a espada negra Ovinrbaane.", "q_armag"));
        all.add(new CanonicalRumor("rumor_ch6_02", 6, "poi_tiger_lords_camp", "Tribos bárbaras reunidas em acampamentos ao oeste preparam saques coordenados.", "q_tiger_lords"));

        // Capítulo 7: Guerra dos Reis do Rio
        all.add(new CanonicalRumor("rumor_ch7_01", 7, "poi_rushlight_tournament", "O Rei Castruccio Irovetti convocou governantes para o festival de torneios de Rushlight em Pitax.", "q_rushlight"));
        all.add(new CanonicalRumor("rumor_ch7_02", 7, "poi_whiterose_abbey", "A Abadia de Whiterose esconde manuscritos heréticos e relíquias de dinastias caídas.", "q_whiterose"));

        // Capítulo 8: Aqueles que Espreitam Abaixo
        all.add(new CanonicalRumor("rumor_ch8_01", 8, "poi_darklands_passage", "Entradas secretas nas colinas conduzem às Terras Negras e horrores subterrâneos desconhecidos.", "q_darklands"));
        all.add(new CanonicalRumor("rumor_ch8_02", 8, "poi_planar_anchor", "Distorções no tecido planar enfraquecem as defesas do Reino nas fronteiras feéricas.", "q_planar_anchor"));

        // Capítulo 9: O Som de Mil Gritos
        all.add(new CanonicalRumor("rumor_ch9_01", 9, "poi_house_edge_time", "A lendária Casa no Limiar do Tempo foi localizada além dos véus temporais da floresta.", "q_house_edge_time"));
        all.add(new CanonicalRumor("rumor_ch9_02", 9, "poi_briar_sanctum", "A lâmina Briar é procurada como a chave essencial para superar as ilusões de Nyrissa.", "q_briar"));

        // Capítulo 10: A Maldição do Rei Lanterna
        all.add(new CanonicalRumor("rumor_ch10_01", 10, "poi_lantern_king_manifestation", "O Rei Lanterna manipula o destino das Terras Roubadas como uma peça em seu jogo primordial.", "q_lantern_king"));
        all.add(new CanonicalRumor("rumor_ch10_02", 10, "poi_stolen_lands_planar_rift", "Fendas planares colapsam a realidade e exigem mobilização máxima das forças do Reino.", "q_planar_rift"));

        // Capítulo 11: Atlas Regional e Hex POIs
        all.add(new CanonicalRumor("rumor_ch11_01", 11, "poi_hex_narlmarches_ruins", "Ruínas esquecidas nos Ermos Verdejantes ocultam altares antigos e marcos de exploração.", "q_atlas_narlmarches"));
        all.add(new CanonicalRumor("rumor_ch11_02", 11, "poi_hex_kamelands_cache", "Esconderijos de contrabandistas nas colinas de Kamelands guardam suprimentos de valor.", "q_atlas_kamelands"));

        // Capítulo 12: Histórias e Missões dos Companheiros
        all.add(new CanonicalRumor("rumor_ch12_01", 12, "poi_companion_amiri", "Amiri procura uma oportunidade de desafiar feras lendárias para redimir a honra de sua lâmina.", "q_companion_amiri"));
        all.add(new CanonicalRumor("rumor_ch12_02", 12, "poi_companion_linzi", "Linzi reúne histórias comoventes dos colonos para publicar o grande romance das Terras Roubadas.", "q_companion_linzi"));
        all.add(new CanonicalRumor("rumor_ch12_03", 12, "poi_companion_valerie", "Enviados da Ordem da Rosa Eterna buscam contato formal com Valerie nas fronteiras da Baronia.", "q_companion_valerie"));

        if (targetChapter <= 0) {
            return all;
        }

        List<CanonicalRumor> filtered = new ArrayList<CanonicalRumor>();
        for (CanonicalRumor r : all) {
            if (r.chapter == targetChapter) {
                filtered.add(r);
            }
        }
        return filtered;
    }
}
