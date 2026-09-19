package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Diretor Ambiental V2.
 *
 * Planeja localmente clima, transições de terreno, influência de povoados,
 * tráfego, fauna, oportunidades, riscos e camadas sonoras. O planejamento é
 * determinístico, usa apenas dados persistidos no aparelho e mantém memória
 * própria para impedir repetição. Nenhuma chamada adicional ao Gemini é feita.
 */
public final class EnvironmentalDirector {
    private static final int ROUTE_HORIZON = 5;
    private static final int RECENT_LIMIT = 18;
    private final AppDb db;
    private final TravelRulesEngine travelRules;

    public EnvironmentalDirector(AppDb database){db=database;travelRules=new TravelRulesEngine(database);}

    public JSONObject preview(String playerAction){
        JSONObject out=new JSONObject();
        try{
            JSONObject map=db.getMapState();
            JSONObject scene=db.getSceneState();
            JSONObject memory=db.getEnvironmentalDirectorState();

            String packageId=map.optString("map_package_id","");
            String current=map.optString("current_hex_id",scene.optString("hex_id",""));
            JSONArray route=parseArray(map.optString("route_json","[]"));
            JSONArray horizon=routeHorizon(route,current,ROUTE_HORIZON);
            JSONArray hexes=loadHexes(packageId,horizon);
            JSONObject currentHex=hexAt(hexes,0);
            JSONObject nextHex=hexAt(hexes,1);

            String terrain=terrainLabel(currentHex);
            String nextTerrain=terrainLabel(nextHex);
            String time=scene.optString("time_of_day","Manhã");
            int day=Math.max(1,scene.optInt("day_number",1));
            boolean travelling=isTravel(playerAction)||(horizon.length()>1&&isMovementIntent(playerAction));
            String actionSignature=signature(safe(playerAction)+"|"+current+"|"+day+"|"+time);
            boolean progression=travelling&&!actionSignature.equals(memory.optString("last_action_signature",""));
            boolean outdoors=travelling||!isIndoor(scene,playerAction);
            boolean road=hasRoad(currentHex,nextHex);

            JSONArray nearbyPlaces=collectLocations(packageId,horizon);
            JSONObject approach=bestApproach(nearbyPlaces);
            JSONObject approachPlan=buildApproachPlan(approach,memory,time,road,travelling);
            JSONObject weatherPlan=buildWeatherPlan(memory,scene.optString("weather",""),terrain,nextTerrain,current,packageId,day,time,progression,outdoors);
            JSONObject terrainPlan=buildTerrainPlan(terrain,nextTerrain,time,currentHex,nextHex);
            JSONObject encounterPlan=buildEncounterPlan(memory,approachPlan,weatherPlan,terrainPlan,road,time,travelling,current,day);

            JSONArray signs=new JSONArray();
            appendStrings(signs,weatherPlan.optJSONArray("perceptible_signs"));
            appendStrings(signs,approachPlan.optJSONArray("perceptible_signs"));
            appendStrings(signs,terrainPlan.optJSONArray("perceptible_signs"));
            if(isNight(time))putUnique(signs,"À noite, luzes, fogueiras e sons carregados pelo vento chegam antes dos detalhes visuais.");
            JSONArray freshSigns=filterRecent(signs,memory.optJSONArray("recent_signatures"),6);

            JSONArray hazards=new JSONArray();
            appendStrings(hazards,weatherPlan.optJSONArray("hazards"));
            appendStrings(hazards,terrainPlan.optJSONArray("hazards"));

            JSONArray opportunities=new JSONArray();
            appendStrings(opportunities,weatherPlan.optJSONArray("player_options"));
            appendStrings(opportunities,terrainPlan.optJSONArray("opportunities"));

            JSONObject audio=buildAudioPlan(terrain,nextTerrain,time,road,approachPlan,weatherPlan,terrainPlan);
            JSONObject commit=buildCommitState(memory,current,day,time,progression,actionSignature,approachPlan,weatherPlan,freshSigns,encounterPlan,audio);

            out.put("source","local_deterministic_v2");
            out.put("requires_gemini",false);
            out.put("autonomous",true);
            out.put("current_hex",current);
            out.put("route_horizon",horizon);
            out.put("terrain",terrain);
            out.put("next_terrain",nextTerrain);
            out.put("terrain_transition",terrainPlan);
            out.put("travelling",travelling);
            out.put("outdoors",outdoors);
            out.put("road_present",road);
            out.put("weather_system",weatherPlan);
            out.put("official_mechanics_preview",travelRules.environmentalMechanics(weatherPlan.optInt("stage",0)>=3?weatherPlan.optString("display_weather",""):""));
            out.put("forecast_weather",weatherPlan.optString("display_weather"));
            out.put("nearby_places",nearbyPlaces);
            out.put("settlement_influence",approachPlan);
            out.put("perceptible_signs",freshSigns);
            out.put("travel_hazards",hazards);
            out.put("resource_opportunities",opportunities);
            out.put("encounter_plan",encounterPlan);
            out.put("suggested_encounters",encounterPlan.optJSONArray("surfaced"));
            out.put("audio_plan",audio);
            out.put("rules_boundary",new JSONObject().put("weather_forecast",TravelRulesEngine.RULE_NARRATIVE).put("settlement_approach",TravelRulesEngine.RULE_NARRATIVE).put("encounter_seeds",TravelRulesEngine.RULE_NARRATIVE).put("active_environmental_effects",TravelRulesEngine.RULE_RAW).put("obstacle_consequences",TravelRulesEngine.RULE_GM));
            out.put("director_instruction",
                    "Use somente sinais proporcionais à distância e ao estágio. " +
                    "Não revele detalhes que ainda não seriam perceptíveis. " +
                    "Encontros são sementes opcionais: só acontecem quando a ficção e a ação do grupo os ativarem. " +
                    "Fenômenos extremos devem oferecer aviso e possibilidade real de reação.");
            out.put("_state_commit",commit);
        }catch(Exception failure){
            try{out.put("source","local_deterministic_v2").put("requires_gemini",false).put("error","Diretor ambiental indisponível nesta cena: "+safe(failure.getMessage()));}catch(Exception ignored){AppLog.ignored("EnvironmentalDirector","Falha tolerada; a execução continuou.",ignored);}
        }
        return out;
    }

    /** Versão segura para contexto da IA, sem campos internos de persistência. */
    public JSONObject promptView(JSONObject preview){
        if(preview==null)return new JSONObject();
        try{
            JSONObject copy=new JSONObject(preview.toString());
            copy.remove("_state_commit");
            return copy;
        }catch(Exception e){return new JSONObject();}
    }

    /**
     * Confirma a memória ambiental somente depois que a cena foi aceita.
     * Também aplica clima já ativo à cena, sem transformar um mero prognóstico
     * em fato antes da hora.
     */
    public void commit(JSONObject preview,String narration){
        try{commitChecked(preview,narration);}
        catch(Exception failure){AppLog.e("EnvironmentalDirector","Falha ao confirmar o estado ambiental.",failure);}
    }

    /** Variante usada pela transação do Mestre: qualquer falha deve abortar tudo. */
    public void commitChecked(JSONObject preview,String narration) throws Exception{
        if(preview==null)return;
        JSONObject next=preview.optJSONObject("_state_commit");
        if(next==null||next.length()==0)return;
        JSONObject before=db.getEnvironmentalDirectorState();
        db.saveEnvironmentalDirectorStateChecked(next);

        JSONObject sceneUpdate=next.optJSONObject("scene_update");
        if(sceneUpdate!=null&&sceneUpdate.optBoolean("apply",false)){
            JSONObject scene=db.getSceneState();
            String oldWeather=scene.optString("weather","");
            String newWeather=sceneUpdate.optString("weather",oldWeather);
            if(newWeather.length()>0&&!fold(newWeather).equals(fold(oldWeather))){
                scene.put("weather",newWeather);
                db.saveSceneStateChecked(scene);
                db.addAuditEvent("ambiente","Clima avançou automaticamente",
                        oldWeather.length()==0?newWeather:oldWeather+" → "+newWeather,
                        new JSONObject().put("weather",oldWeather).toString(),
                        new JSONObject().put("weather",newWeather).put("source","Diretor Ambiental V2").toString(),true);
            }
        }

        String oldApproach=before.optString("last_approach_stage","");
        String newApproach=next.optString("last_approach_stage","");
        String oldKey=before.optString("last_approach_key","");
        String newKey=next.optString("last_approach_key","");
        if(newKey.length()>0&&(!newKey.equals(oldKey)||!newApproach.equals(oldApproach))){
            db.addAuditEvent("ambiente","Influência de local atualizada",
                    next.optString("last_approach_label","Aproximação detectada")+" • "+newApproach,
                    before.toString(),next.toString(),false);
        }
    }

    public JSONObject mergeAudioPlan(JSONObject aiPlan,String playerAction){
        JSONObject local=preview(playerAction).optJSONObject("audio_plan");
        JSONObject merged=new JSONObject();
        try{
            if(local!=null)copy(local,merged);
            if(aiPlan!=null){
                Iterator<String> it=aiPlan.keys();
                while(it.hasNext()){
                    String key=it.next();
                    Object value=aiPlan.opt(key);
                    if(value==null||value==JSONObject.NULL)continue;
                    if("cues".equals(key)){
                        merged.put("cues",mergeObjectArrays(merged.optJSONArray("cues"),value instanceof JSONArray?(JSONArray)value:null,"event","phrase"));
                    }else if("ambient_layers".equals(key)){
                        merged.put("ambient_layers",mergeObjectArrays(merged.optJSONArray("ambient_layers"),value instanceof JSONArray?(JSONArray)value:null,"kind","query"));
                    }else if("soundscape_hints".equals(key)){
                        String a=merged.optString(key,""),b=String.valueOf(value);
                        merged.put(key,joinNonEmpty(a,b));
                    }else if(value instanceof String&&((String)value).trim().length()==0){
                        // vazio não apaga o plano local
                    }else if(value instanceof JSONArray&&((JSONArray)value).length()==0){
                        // lista vazia não apaga camadas locais
                    }else{
                        merged.put(key,value);
                    }
                }
            }
            merged.put("director_local",true);
            merged.put("director_version","2");
        }catch(Exception ignored){AppLog.ignored("EnvironmentalDirector","Falha tolerada; a execução continuou.",ignored);}
        return merged;
    }

    private JSONObject buildWeatherPlan(JSONObject memory,String existing,String terrain,String nextTerrain,String current,String packageId,int day,String time,boolean travelling,boolean outdoors)throws Exception{
        String climate=climateProfile(terrain+" "+nextTerrain);
        JSONObject previous=memory.optJSONObject("weather_system");
        if(previous==null)previous=new JSONObject();

        String existingFold=fold(existing);
        boolean directorExisting=previous.optString("event","").length()>0&&existingFold.equals(fold(previous.optString("display_weather","")));
        String forcedEvent=directorExisting?"":eventFromExisting(existingFold);
        int seed=positiveHash(packageId+"|"+current+"|"+day+"|"+timeBucket(time)+"|"+climate);
        String event=directorExisting?previous.optString("event"):forcedEvent.length()>0?forcedEvent:eventForClimate(climate,seed);
        int probability=eventProbability(climate);
        boolean eventSelected=directorExisting||forcedEvent.length()>0||(event.length()>0&&seed%100<probability);

        int stage=0;
        int activeBeats=0;
        boolean clearedEvent=false;
        if(forcedEvent.length()>0){
            stage=3;
        }else if(eventSelected){
            boolean same=event.equals(previous.optString("event"))&&climate.equals(previous.optString("climate"));
            if(same){
                stage=Math.max(1,previous.optInt("stage",1));
                activeBeats=Math.max(0,previous.optInt("active_beats",0));
                if(travelling&&outdoors){
                    if(stage<3)stage++;
                    else if(stage==3){
                        activeBeats++;
                        if(activeBeats>=3)stage=4;
                    }else if(stage==4)stage=5;
                }
            }else stage=1;
        }

        if(stage>=5){
            eventSelected=false;
            event="";
            stage=0;
            activeBeats=0;
            clearedEvent=true;
        }

        String baselineInput=directorExisting?"":existing;
        String display=eventSelected?weatherLabel(event,stage):baselineWeather(baselineInput,climate,seed);
        JSONArray signs=weatherSigns(event,stage,climate);
        JSONArray hazards=weatherHazards(event,stage);
        JSONArray options=weatherOptions(event,stage);
        JSONObject system=new JSONObject();
        system.put("climate_profile",climate);
        system.put("event",event);
        system.put("stage",stage);
        system.put("stage_name",weatherStageName(stage));
        system.put("probability_percent",eventSelected?probability:0);
        system.put("eta_travel_steps",eventSelected&&stage<3?3-stage:0);
        system.put("display_weather",display);
        system.put("perceptible_signs",signs);
        system.put("hazards",hazards);
        system.put("player_options",options);
        system.put("active_beats",activeBeats);
        system.put("automatic_scene_update",outdoors&&((stage>=3&&stage<=4)||clearedEvent));
        system.put("scene_weather",display);
        system.put("seed",seed);
        JSONObject metrics=weatherMetrics(climate,event,stage,seed,time,display);java.util.Iterator<String> metricKeys=metrics.keys();while(metricKeys.hasNext()){String key=metricKeys.next();system.put(key,metrics.opt(key));}
        system.put("classification",stage>=3?TravelRulesEngine.RULE_RAW:TravelRulesEngine.RULE_NARRATIVE);
        system.put("mechanics_active",stage>=3);
        system.put("rule","O estágio só avança com deslocamento/tempo confirmado; prognóstico não vira fato instantaneamente. Efeitos mecânicos só existem no estágio ativo e são resolvidos pelo TravelRulesEngine.");
        return system;
    }

    private JSONObject buildApproachPlan(JSONObject place,JSONObject memory,String time,boolean road,boolean travelling)throws Exception{
        JSONObject out=new JSONObject();
        if(place==null||place.length()==0){
            out.put("active",false).put("perceptible_signs",new JSONArray());
            return out;
        }
        int steps=Math.max(0,place.optInt("route_steps",ROUTE_HORIZON+1));
        String key=locationKey(place);
        String name=place.optString("name","um local habitado");
        String type=locationType(place);
        String stage=approachStage(steps);
        int previousSteps=memory.optInt("last_approach_steps",99);
        boolean departing=key.equals(memory.optString("last_approach_key"))&&steps>previousSteps;
        if(departing)stage="afastando";

        JSONArray signs=new JSONArray();
        boolean urban=isUrban(place);
        boolean port=isPort(place);
        boolean village=isVillage(place);

        if(departing){
            signs.put("Os sons, as luzes e o movimento de "+name+" começam a ficar para trás.");
        }else if(steps>=4){
            signs.put(road?"O padrão do tráfego indica um centro habitado mais adiante.":"Marcas de passagem sugerem ocupação humana além do horizonte imediato.");
        }else if(steps==3){
            signs.put("Carroças, pegadas e viajantes passam a convergir na mesma direção.");
        }else if(steps==2){
            signs.put("Fumaça, rodas e vozes ocasionais anunciam "+name+" antes que suas construções apareçam.");
        }else if(steps==1){
            signs.put("Os arredores de "+name+" já alcançam a rota: cercas, campos, tráfego local e caminhos secundários.");
        }else{
            signs.put(isNight(time)?"Luzes, guardas e ruídos identificam a entrada de "+name+".":"A estrada desemboca diretamente em "+name+".");
        }
        if(port&&steps<=2)signs.put("O cheiro de sal, aves costeiras e atividade de carga denunciam a proximidade do porto.");
        if(village&&steps<=1)signs.put("Animais domésticos, hortas e gente trabalhando substituem gradualmente o ermo.");
        if(urban&&steps<=1)signs.put("Mensageiros, mercadores, patrulhas e carroças de abastecimento tornam-se parte natural da estrada.");

        int influence=Math.max(0,100-steps*20);
        out.put("active",true);
        out.put("location_key",key);
        out.put("name",name);
        out.put("type",type);
        out.put("route_steps",steps);
        out.put("stage",stage);
        out.put("departing",departing);
        out.put("urban",urban);
        out.put("port",port);
        out.put("village",village);
        out.put("influence_percent",influence);
        out.put("perceptible_signs",signs);
        out.put("audio_query",approachAudioQuery(place,steps,time));
        out.put("narrative_rule","A distância controla o detalhe: influência remota, fluxo na rota, sinais distantes, arredores e entrada.");
        return out;
    }

    private JSONObject buildTerrainPlan(String terrain,String nextTerrain,String time,JSONObject currentHex,JSONObject nextHex)throws Exception{
        JSONObject out=new JSONObject();
        String climate=climateProfile(terrain+" "+nextTerrain);
        boolean transition=nextTerrain.length()>0&&!fold(nextTerrain).equals(fold(terrain));
        JSONArray signs=new JSONArray();
        JSONArray hazards=new JSONArray();
        JSONArray opportunities=new JSONArray();
        JSONArray soundHints=new JSONArray();

        if(transition)signs.put("A vegetação, o solo, a umidade e os sons começam a mudar antes da fronteira do próximo terreno.");
        if("polar".equals(climate)){
            signs.put("A neve abafa sons distantes; rastros recentes se destacam com facilidade.");
            hazards.put("frio, visibilidade reduzida e perda de orientação quando o vento aumenta");
            opportunities.put("examinar rastros, procurar abrigo contra o vento ou preparar proteção contra frio");
            soundHints.put("vento frio neve passos abafados");
        }else if("montanha".equals(climate)){
            signs.put("Ecos, aves de altitude e pedras soltas revelam a geometria do caminho.");
            hazards.put("desníveis, pedras soltas e mudanças rápidas de tempo");
            opportunities.put("buscar terreno elevado para observar a rota ou localizar passagem protegida");
            soundHints.put("vento montanha corvos ecos");
        }else if("pantano".equals(climate)){
            signs.put("Insetos, água rasa e vegetação encharcada tornam cada desvio audível.");
            hazards.put("solo falso, água contaminada, névoa e deslocamento lento");
            opportunities.put("seguir terreno firme, procurar plantas úteis ou rastrear passagem recente na lama");
            soundHints.put("pantano insetos sapos agua lama");
        }else if("deserto".equals(climate)){
            signs.put("O vento redesenha rastros e a distância parece menor do que realmente é.");
            hazards.put("calor, desidratação, areia e referências visuais enganosas");
            opportunities.put("viajar nas horas mais frescas, proteger água ou seguir marcos resistentes ao vento");
            soundHints.put("deserto vento areia");
        }else if("costa".equals(climate)){
            signs.put("Aves, umidade e cheiro de sal anunciam água antes de ela ficar visível.");
            hazards.put("vento lateral, maré, terreno escorregadio e tempestades costeiras");
            opportunities.put("observar embarcações, procurar abrigo de vento ou aproveitar rotas de pescadores");
            soundHints.put("ondas gaivotas vento costa");
        }else if("floresta".equals(climate)){
            signs.put("Mudanças no canto das aves e no movimento das folhas denunciam presença e horário.");
            hazards.put("visibilidade curta, raízes, trilhas falsas e queda de galhos");
            opportunities.put("forragear, procurar rastros, água ou sinais de caçadores e lenhadores");
            soundHints.put(isNight(time)?"floresta noite corujas folhas":"floresta aves folhas riacho");
        }else{
            signs.put("O vento, a altura da vegetação e os rastros na estrada indicam movimento recente.");
            hazards.put("exposição ao clima e poucos abrigos naturais");
            opportunities.put("observar a distância, escolher acampamento ou identificar tráfego pela poeira");
            soundHints.put(isNight(time)?"campos noite grilos vento":"campos vento aves estrada");
        }

        out.put("current",terrain);
        out.put("next",nextTerrain);
        out.put("transitioning",transition);
        out.put("climate_profile",climate);
        out.put("perceptible_signs",signs);
        out.put("hazards",hazards);
        out.put("opportunities",opportunities);
        out.put("sound_hints",soundHints);
        out.put("roads",roadCount(currentHex)+roadCount(nextHex));
        return out;
    }

    private JSONObject buildEncounterPlan(JSONObject memory,JSONObject approach,JSONObject weather,JSONObject terrain,boolean road,String time,boolean travelling,String current,int day)throws Exception{
        JSONArray candidates=new JSONArray();
        int pressure=travelling?12:3;
        int steps=approach.optInt("route_steps",99);
        if(road)pressure+=12;
        if(approach.optBoolean("active",false))pressure+=Math.max(0,35-steps*6);
        if(isNight(time))pressure-=8;
        if(weather.optInt("stage",0)>=3)pressure+=14;
        pressure=Math.max(0,Math.min(70,pressure));

        if(approach.optBoolean("urban",false)){
            String name=approach.optString("name","o povoado");
            addCandidate(candidates,"social","mercadores e carroças de abastecimento vindos de "+name,
                    "se o grupo continuar pela estrada e demonstrar interesse no tráfego",70,"mercadores|"+name);
            addCandidate(candidates,"informação","um mensageiro ou viajante trazendo notícias recentes de "+name,
                    "se o grupo abordar alguém ou procurar informações",62,"mensageiro|"+name);
            addCandidate(candidates,"autoridade","patrulha, cobrador ou guia local nas aproximações de "+name,
                    "se a rota alcançar os arredores, uma ponte, portão ou posto de controle",48,"patrulha|"+name);
        }else if(approach.optBoolean("village",false)){
            String name=approach.optString("name","a vila");
            addCandidate(candidates,"cotidiano","camponeses, pastores ou uma carroça local ligada a "+name,
                    "se o grupo permanecer na estrada durante o período de trabalho",58,"moradores|"+name);
        }
        if(weather.optInt("stage",0)>=2){
            addCandidate(candidates,"clima","viajantes procurando abrigo antes da piora do tempo",
                    "se o grupo avançar sem procurar proteção",66,"abrigo|"+weather.optString("event"));
        }

        String climate=terrain.optString("climate_profile","");
        if("floresta".equals(climate))addCandidate(candidates,"fauna","animais silenciam ou mudam de direção diante de algo adiante","se o grupo observar rastros e comportamento da mata",42,"fauna|floresta");
        if("montanha".equals(climate))addCandidate(candidates,"viagem","um carregador, caçador ou peregrino conhecedor da passagem","se o caminho estreitar ou surgir uma bifurcação",38,"viajante|montanha");
        if("deserto".equals(climate))addCandidate(candidates,"viagem","rastros de uma caravana ou batedores procurando água","se o grupo investigar sinais preservados do vento",40,"caravana|deserto");
        if("pantano".equals(climate))addCandidate(candidates,"exploração","sinais de alguém usando um caminho firme oculto pela vegetação","se o grupo procurar rota segura",36,"trilha|pantano");

        JSONArray recent=memory.optJSONArray("recent_signatures");
        JSONArray eligible=new JSONArray();
        for(int i=0;i<candidates.length();i++){
            JSONObject c=candidates.optJSONObject(i);if(c==null)continue;
            if(!containsSignature(recent,c.optString("signature")))eligible.put(c);
        }

        int seed=positiveHash(current+"|"+day+"|"+timeBucket(time)+"|"+eligible.toString());
        boolean surface=travelling&&eligible.length()>0&&seed%100<pressure;
        JSONArray surfaced=new JSONArray();
        if(surface){
            JSONObject chosen=weightedPick(eligible,seed);
            if(chosen!=null)surfaced.put(chosen);
        }

        JSONObject out=new JSONObject();
        out.put("pressure_percent",pressure);
        out.put("should_surface",surface);
        out.put("candidates",eligible);
        out.put("surfaced",surfaced);
        out.put("forced",false);
        out.put("combat_default",false);
        out.put("rule","Uma semente só vira encontro quando a ação do grupo, o horário e a ficção ativarem seu gatilho.");
        return out;
    }

    private JSONObject buildAudioPlan(String terrain,String nextTerrain,String time,boolean road,JSONObject approach,JSONObject weather,JSONObject terrainPlan)throws Exception{
        JSONObject audio=new JSONObject();
        String environment=terrain.length()>0?terrain:"estrada e ermos";
        if(nextTerrain.length()>0&&!fold(nextTerrain).equals(fold(terrain)))environment+=" em transição para "+nextTerrain;
        String location=approach.optBoolean("active",false)?"influência de "+approach.optString("name","um povoado"):"hex atual";
        String mood=isNight(time)?"misteriosa":"exploração";
        if(weather.optInt("stage",0)>=2)mood="tensa";

        JSONArray layers=new JSONArray();
        JSONArray hints=terrainPlan.optJSONArray("sound_hints");
        StringBuilder hintText=new StringBuilder();
        if(hints!=null)for(int i=0;i<hints.length();i++)appendHint(hintText,hints.optString(i));
        if(hintText.length()>0)layers.put(new JSONObject().put("kind","terrain").put("query",hintText.toString()).put("gain",0.72).put("continuous",true));

        if(weather.optInt("stage",0)>0){
            String query=weatherAudioQuery(weather.optString("event"),weather.optInt("stage",0));
            appendHint(hintText,query);
            layers.put(new JSONObject().put("kind","weather").put("query",query).put("gain",weather.optInt("stage",0)>=3?0.9:0.48).put("continuous",true));
        }
        if(approach.optBoolean("active",false)){
            String query=approach.optString("audio_query");
            appendHint(hintText,query);
            double gain=Math.max(.18,Math.min(.78,approach.optInt("influence_percent",0)/100.0));
            layers.put(new JSONObject().put("kind","settlement").put("query",query).put("gain",gain).put("continuous",true).put("distance_steps",approach.optInt("route_steps",99)));
        }
        if(road){
            appendHint(hintText,"estrada passos rodas ocasionais");
            layers.put(new JSONObject().put("kind","road").put("query","estrada passos rodas ocasionais").put("gain",0.28).put("continuous",true));
        }

        audio.put("environment",environment);
        audio.put("weather",weather.optString("display_weather","clima neutro"));
        audio.put("location",location);
        audio.put("time_of_day",time);
        audio.put("music_mood",mood);
        audio.put("transition_ms",weather.optInt("stage",0)>=2?5600:approach.optBoolean("active",false)?4800:3400);
        audio.put("soundscape_hints",hintText.toString());
        audio.put("ambient_layers",layers);
        audio.put("cues",new JSONArray());
        audio.put("sfx_policy","Som pontual só toca quando o evento estiver realmente presente na narração; camadas distantes usam ambiência.");
        return audio;
    }

    private JSONObject buildCommitState(JSONObject memory,String current,int day,String time,boolean progression,String actionSignature,JSONObject approach,JSONObject weather,JSONArray signs,JSONObject encounters,JSONObject audio)throws Exception{
        JSONObject next=new JSONObject(memory.toString());
        next.put("last_hex_id",current);
        next.put("last_day",day);
        next.put("last_time_of_day",time);
        next.put("travel_beats",Math.max(0,memory.optInt("travel_beats",0))+(progression?1:0));
        next.put("last_action_signature",actionSignature);
        next.put("weather_system",new JSONObject()
                .put("event",weather.optString("event"))
                .put("climate",weather.optString("climate_profile"))
                .put("stage",weather.optInt("stage"))
                .put("active_beats",weather.optInt("active_beats"))
                .put("display_weather",weather.optString("display_weather")));
        if(approach.optBoolean("active",false)){
            next.put("last_approach_key",approach.optString("location_key"));
            next.put("last_approach_steps",approach.optInt("route_steps",99));
            next.put("last_approach_stage",approach.optString("stage"));
            next.put("last_approach_label",approach.optString("name"));
        }else{
            next.put("last_approach_key","");
            next.put("last_approach_steps",99);
            next.put("last_approach_stage","");
            next.put("last_approach_label","");
        }

        JSONArray recent=copyArray(memory.optJSONArray("recent_signatures"));
        for(int i=0;i<signs.length();i++)pushRecent(recent,signature(signs.optString(i)));
        JSONArray surfaced=encounters.optJSONArray("surfaced");
        if(surfaced!=null)for(int i=0;i<surfaced.length();i++){JSONObject e=surfaced.optJSONObject(i);if(e!=null)pushRecent(recent,e.optString("signature"));}
        next.put("recent_signatures",trimRecent(recent,RECENT_LIMIT));

        JSONArray audioRecent=copyArray(memory.optJSONArray("recent_audio_signatures"));
        JSONArray layers=audio.optJSONArray("ambient_layers");
        if(layers!=null)for(int i=0;i<layers.length();i++){JSONObject l=layers.optJSONObject(i);if(l!=null)pushRecent(audioRecent,signature(l.optString("kind")+"|"+l.optString("query")));}
        next.put("recent_audio_signatures",trimRecent(audioRecent,12));

        JSONObject sceneUpdate=new JSONObject();
        sceneUpdate.put("apply",weather.optBoolean("automatic_scene_update",false)&&progression);
        sceneUpdate.put("weather",weather.optString("scene_weather"));
        next.put("scene_update",sceneUpdate);
        next.put("last_preview_signature",signature(current+"|"+day+"|"+time+"|"+weather.optString("display_weather")+"|"+approach.optString("stage")));
        next.put("updated_at",System.currentTimeMillis());
        return next;
    }

    private JSONArray collectLocations(String packageId,JSONArray horizon){
        JSONArray out=new JSONArray();
        if(packageId==null||packageId.length()==0)return out;
        for(int step=0;step<horizon.length();step++)appendLocations(out,packageId,horizon.optString(step),step);
        return out;
    }

    private void appendLocations(JSONArray out,String packageId,String hexId,int steps){
        if(packageId==null||packageId.length()==0||hexId==null||hexId.length()==0)return;
        try{
            List<JSONObject> rows=db.getMapLocationsForHex(packageId,hexId);
            for(JSONObject row:rows){
                String status=fold(row.optString("placement_status",""));
                if(status.contains("review")||status.contains("approx")||status.contains("unassigned")||status.contains("incerto"))continue;
                JSONObject item=new JSONObject(row.toString());
                item.put("route_steps",steps);
                item.put("hex_id",hexId);
                out.put(item);
            }
        }catch(Exception ignored){AppLog.ignored("EnvironmentalDirector","Falha tolerada; a execução continuou.",ignored);}
    }

    private JSONArray loadHexes(String packageId,JSONArray horizon){
        JSONArray out=new JSONArray();
        for(int i=0;i<horizon.length();i++){
            String id=horizon.optString(i);
            JSONObject h=packageId.length()==0||id.length()==0?new JSONObject():db.getMapHex(packageId,id);
            try{h.put("_hex_id",id).put("_route_steps",i);}catch(Exception ignored){AppLog.ignored("EnvironmentalDirector","Falha tolerada; a execução continuou.",ignored);}
            out.put(h);
        }
        return out;
    }

    private static JSONObject bestApproach(JSONArray places){
        JSONObject best=new JSONObject();
        int bestScore=-99999;
        for(int i=0;i<places.length();i++){
            JSONObject p=places.optJSONObject(i);if(p==null)continue;
            int steps=p.optInt("route_steps",99);
            int score=(isUrban(p)?1000:isVillage(p)?760:isPort(p)?900:260)-steps*120+placeImportance(p);
            if(score>bestScore){bestScore=score;try{best=new JSONObject(p.toString());}catch(Exception ignored){AppLog.ignored("EnvironmentalDirector","Falha tolerada; a execução continuou.",ignored);}}
        }
        return best;
    }

    private static int placeImportance(JSONObject place){
        int score=0;
        String n=fold(place.optString("importance","")+" "+place.optString("type","")+" "+place.optString("category",""));
        if(contains(n,"capital","metropole","metropolis","major","grande"))score+=100;
        if(contains(n,"city","cidade","porto","port"))score+=60;
        if(place.optInt("population",0)>10000)score+=80;
        return score;
    }

    private static boolean isUrban(JSONObject place){
        String n=fold(locationType(place)+" "+place.optString("name",""));
        return contains(n,"city","cidade","town","capital","metropole","metropolis","urbano","urban");
    }
    private static boolean isVillage(JSONObject place){
        String n=fold(locationType(place));
        return contains(n,"village","aldeia","hamlet","vilarejo","vila");
    }
    private static boolean isPort(JSONObject place){
        String n=fold(locationType(place)+" "+place.optString("name",""));
        return contains(n,"porto","port","harbor","harbour","doca","dock");
    }
    private static String locationType(JSONObject place){
        return firstNonEmpty(place.optString("type"),place.optString("location_type"),place.optString("category"),place.optString("kind"),"local");
    }
    private static String locationKey(JSONObject place){
        return firstNonEmpty(place.optString("location_id"),place.optString("id"),place.optString("slug"),signature(place.optString("name")+"|"+locationType(place)));
    }
    private static String approachStage(int steps){
        if(steps<=0)return "entrada_ou_interior";
        if(steps==1)return "arredores";
        if(steps==2)return "sinais_distantes";
        if(steps==3)return "fluxo_na_rota";
        return "influencia_remota";
    }
    private static String approachAudioQuery(JSONObject place,int steps,String time){
        String base=isPort(place)?"porto distante gaivotas carga marinheiros":isUrban(place)?"cidade medieval distante vozes rodas sinos":isVillage(place)?"vila distante animais trabalho carroca":"ocupacao humana distante";
        if(steps<=1)base+=" trafego proximo";
        else if(steps>=4)base+=" muito distante esparso";
        if(isNight(time))base+=" noite fogueiras poucas vozes";
        return base;
    }

    private static String climateProfile(String text){
        String n=fold(text);
        if(contains(n,"neve","nevado","tundra","glac","ice","snow","polar","gelad"))return "polar";
        if(contains(n,"montanha","mountain","highland","serra","pico","alpino"))return "montanha";
        if(contains(n,"pantano","swamp","marsh","brejo","mangue"))return "pantano";
        if(contains(n,"deserto","desert","duna","arido","árido","badland"))return "deserto";
        if(contains(n,"costa","coast","mar","ocean","praia","shore","litoral","ilha"))return "costa";
        if(contains(n,"floresta","forest","bosque","woodland","selva","jungle"))return "floresta";
        return "temperado";
    }
    private static String eventFromExisting(String weather){
        if(contains(weather,"nevasca","blizzard"))return "nevasca";
        if(contains(weather,"tempestade de areia","sandstorm"))return "tempestade de areia";
        if(contains(weather,"tempestade","storm","trovoada"))return "tempestade";
        if(contains(weather,"chuva forte","temporal"))return "chuva forte";
        if(contains(weather,"nevoa densa","névoa densa","fog"))return "névoa densa";
        return "";
    }
    private static String eventForClimate(String climate,int seed){
        if("polar".equals(climate))return "nevasca";
        if("montanha".equals(climate))return seed%2==0?"tempestade de altitude":"vento de altitude";
        if("pantano".equals(climate))return seed%2==0?"chuva forte":"névoa densa";
        if("deserto".equals(climate))return "tempestade de areia";
        if("costa".equals(climate))return seed%2==0?"tempestade costeira":"chuva forte";
        if("floresta".equals(climate))return "tempestade";
        return seed%3==0?"tempestade":"chuva forte";
    }
    private static int eventProbability(String climate){
        if("polar".equals(climate))return 34;
        if("montanha".equals(climate))return 28;
        if("pantano".equals(climate))return 38;
        if("deserto".equals(climate))return 23;
        if("costa".equals(climate))return 31;
        if("floresta".equals(climate))return 24;
        return 18;
    }
    private static JSONObject weatherMetrics(String climate,String event,int stage,int seed,String time,String display)throws Exception{
        int baseTemp;if("polar".equals(climate))baseTemp=-12;else if("montanha".equals(climate))baseTemp=7;else if("pantano".equals(climate))baseTemp=23;else if("deserto".equals(climate))baseTemp=32;else if("costa".equals(climate))baseTemp=20;else if("floresta".equals(climate))baseTemp=17;else baseTemp=18;
        String bucket=timeBucket(time);if("noite".equals(bucket)||"madrugada".equals(bucket))baseTemp-=5;else if("tarde".equals(bucket))baseTemp+=3;baseTemp+=(seed%7)-3;
        boolean wet=contains(event,"chuva","tempestade","nevasca","nevoa","névoa");boolean sand=contains(event,"areia");
        if(stage>=3&&contains(event,"nevasca"))baseTemp-=7;if(stage>=3&&sand)baseTemp+=2;
        int humidity;if("pantano".equals(climate))humidity=78;else if("costa".equals(climate))humidity=68;else if("floresta".equals(climate))humidity=62;else if("polar".equals(climate))humidity=58;else if("deserto".equals(climate))humidity=24;else humidity=48;
        if(wet)humidity=Math.max(humidity,stage>=3?88:stage==2?78:68);if(sand)humidity=Math.min(humidity,28);humidity=Math.max(12,Math.min(98,humidity+(seed%9)-4));
        int pressure=1018+(seed%7)-3;if(stage==1)pressure-=3;else if(stage==2)pressure-=8;else if(stage==3)pressure-=15;else if(stage==4)pressure-=7;if(sand)pressure+=4;pressure=Math.max(975,Math.min(1035,pressure));
        int wind=5+(seed%13);if(stage==1)wind+=6;else if(stage==2)wind+=14;else if(stage==3)wind+=28;else if(stage==4)wind+=16;if(contains(event,"vento de altitude"))wind+=18;wind=Math.max(0,Math.min(95,wind));
        String[] directions={"Norte","Nordeste","Leste","Sudeste","Sul","Sudoeste","Oeste","Noroeste"};String direction=directions[(seed/7)%directions.length];
        double nextHours=Math.round((stage==0?4.0+(seed%42)/10.0:stage==1?2.0+(seed%24)/10.0:stage==2?1.0+(seed%16)/10.0:stage==3?2.0+(seed%32)/10.0:1.5+(seed%20)/10.0)*10d)/10d;
        String forecast=stage==0?"Estável":stage==1?capitalize(event)+" possível":stage==2?capitalize(event)+" provável":stage==3?capitalize(event)+" continua":capitalize(event)+" enfraquecendo";
        return new JSONObject().put("temperature_c",baseTemp).put("humidity_percent",humidity).put("pressure_hpa",pressure).put("wind_kmh",wind).put("wind_direction",direction).put("next_transition_hours",nextHours).put("forecast",forecast).put("state_consistent",true).put("display_locale","pt-BR").put("display_summary",display);
    }

    private static String baselineWeather(String existing,String climate,int seed){
        if(existing!=null&&existing.trim().length()>0&&!contains(fold(existing),"indefinido","neutro"))return existing.trim();
        if("polar".equals(climate))return seed%3==0?"Neve leve e vento frio":"Frio seco com neve acumulada";
        if("montanha".equals(climate))return "Vento irregular e temperatura variável";
        if("pantano".equals(climate))return "Umidade alta e névoa localizada";
        if("deserto".equals(climate))return "Tempo seco e vento quente";
        if("costa".equals(climate))return "Vento úmido e nuvens móveis";
        if("floresta".equals(climate))return "Tempo ameno sob a cobertura das árvores";
        return "Tempo estável";
    }
    private static String weatherLabel(String event,int stage){
        if(stage<=1)return capitalize(event)+" possível; primeiros indícios";
        if(stage==2)return capitalize(event)+" se formando";
        if(stage==3)return capitalize(event)+" ativa";
        if(stage==4)return capitalize(event)+" perdendo força";
        return capitalize(event);
    }
    private static String weatherStageName(int stage){
        switch(stage){
            case 1:return "indicios";
            case 2:return "formando";
            case 3:return "ativo";
            case 4:return "cedendo";
            default:return "estavel";
        }
    }
    private static JSONArray weatherSigns(String event,int stage,String climate){
        JSONArray a=new JSONArray();
        if(stage<=0)return a;
        if("nevasca".equals(event)){
            if(stage==1)a.put("O vento muda de direção, a temperatura cai e flocos secos atravessam a rota.");
            else if(stage==2)a.put("A visibilidade começa a fechar e a neve corre rente ao chão em rajadas.");
            else if(stage==3)a.put("A nevasca domina a rota, apaga referências e cobre rastros rapidamente.");
            else a.put("As rajadas perdem força, mas a neve recém-acumulada ainda altera o caminho.");
        }else if("tempestade de areia".equals(event)){
            if(stage==1)a.put("Uma faixa opaca cresce no horizonte e o vento traz grãos de areia mais grossos.");
            else if(stage==2)a.put("O céu perde contraste e a areia começa a varrer o solo em lâminas baixas.");
            else if(stage==3)a.put("A tempestade de areia reduz a visão e apaga referências da rota.");
            else a.put("A poeira começa a baixar, deixando dunas e rastros redesenhados.");
        }else if(contains(event,"nevoa","névoa")){
            a.put(stage<3?"A umidade engrossa e bancos de névoa ocupam as partes baixas.":"A névoa densa encurta a visão e distorce distâncias e sons.");
        }else if(contains(event,"tempestade","chuva")){
            if(stage==1)a.put("O vento esfria, nuvens fecham o céu e o cheiro de chuva chega antes das primeiras gotas.");
            else if(stage==2)a.put("A chuva começa a pontuar o caminho e trovões distantes marcam a aproximação do temporal.");
            else if(stage==3)a.put("Chuva e vento dominam o ambiente, tornando o terreno e a comunicação mais difíceis.");
            else a.put("A chuva enfraquece, mas água, lama e galhos caídos permanecem.");
        }else if("montanha".equals(climate)){
            a.put("O vento muda bruscamente e nuvens passam a se prender às elevações.");
        }
        return a;
    }
    private static JSONArray weatherHazards(String event,int stage){
        JSONArray a=new JSONArray();
        if(stage<2)return a;
        if("nevasca".equals(event))a.put("frio intenso, orientação difícil, fadiga e estrada encoberta");
        else if("tempestade de areia".equals(event))a.put("visibilidade quase nula, areia no equipamento e risco de perder a rota");
        else if(contains(event,"nevoa","névoa"))a.put("visibilidade curta, distâncias enganosas e encontros percebidos tarde");
        else if(contains(event,"tempestade","chuva"))a.put("lama, terreno escorregadio, cursos d'água cheios e ruído que mascara aproximações");
        return a;
    }
    private static JSONArray weatherOptions(String event,int stage){
        JSONArray a=new JSONArray();
        if(stage<=0)return a;
        if(stage<3){
            a.put("procurar abrigo antes da piora");
            a.put("reduzir o ritmo, mudar a rota ou preparar equipamento adequado");
        }else{
            a.put("interromper a marcha em local protegido");
            a.put("aceitar avanço mais lento e maior risco para manter a rota");
        }
        return a;
    }
    private static String weatherAudioQuery(String event,int stage){
        if("nevasca".equals(event))return stage>=3?"nevasca vento forte neve intensa":"vento frio neve distante rajadas";
        if("tempestade de areia".equals(event))return stage>=3?"tempestade areia vento forte":"vento areia distante";
        if(contains(event,"nevoa","névoa"))return "nevoa umidade gotas vento baixo";
        if(contains(event,"tempestade","chuva"))return stage>=3?"chuva forte vento trovao":"chuva distante vento nuvens";
        return event;
    }

    private static void addCandidate(JSONArray out,String kind,String summary,String trigger,int weight,String sig)throws Exception{
        out.put(new JSONObject().put("kind",kind).put("summary",summary).put("trigger",trigger).put("weight",weight).put("signature",signature(sig)).put("forced",false));
    }
    private static JSONObject weightedPick(JSONArray candidates,int seed){
        int total=0;
        for(int i=0;i<candidates.length();i++){JSONObject c=candidates.optJSONObject(i);if(c!=null)total+=Math.max(1,c.optInt("weight",1));}
        if(total<=0)return null;
        int target=seed%total;
        for(int i=0;i<candidates.length();i++){
            JSONObject c=candidates.optJSONObject(i);if(c==null)continue;
            target-=Math.max(1,c.optInt("weight",1));
            if(target<0)return c;
        }
        return candidates.optJSONObject(0);
    }

    private static JSONArray routeHorizon(JSONArray route,String current,int max){
        JSONArray out=new JSONArray();
        if(current!=null&&current.length()>0)out.put(current);
        int start=-1;
        for(int i=0;i<route.length();i++)if(current!=null&&current.equalsIgnoreCase(route.optString(i))){start=i+1;break;}
        if(start<0)start=0;
        for(int i=start;i<route.length()&&out.length()<max+1;i++){
            String id=route.optString(i,"").trim();
            if(id.length()==0||containsText(out,id))continue;
            out.put(id);
        }
        return out;
    }
    private static JSONObject hexAt(JSONArray hexes,int index){JSONObject h=hexes.optJSONObject(index);return h==null?new JSONObject():h;}
    private static String terrainLabel(JSONObject hex){
        if(hex==null)return "";
        return firstNonEmpty(hex.optString("terrain_primary"),hex.optString("terrain"),hex.optString("biome"),hex.optString("terrain_secondary")).replace('_',' ').trim();
    }
    private static boolean isTravel(String action){
        String n=fold(action);
        return contains(n,"viaj","march","caminh","seguir","rota","estrada","partimos","avanc","prossegu","desloc","rumo","ir para","vamos para");
    }
    private static boolean isMovementIntent(String action){
        String n=fold(action);
        if(n.length()==0)return false;
        return !contains(n,"parar","acampar","descans","esperar","observar parado","ficar");
    }
    private static boolean isIndoor(JSONObject scene,String action){
        String n=fold(scene.optString("location")+" "+action);
        return contains(n,"interior","sala","quarto","taverna","estalagem","casa","castelo","torre","templo","caverna","masmorra","subterraneo","loja","oficina")&&!contains(n,"sair","partir","estrada","viajar");
    }
    private static boolean isNight(String time){
        String n=fold(time);
        return contains(n,"noite","madrugada","entardecer","crepusculo");
    }
    private static String timeBucket(String time){
        String n=fold(time);
        if(contains(n,"madrugada"))return "madrugada";
        if(contains(n,"manha","amanhecer"))return "manha";
        if(contains(n,"tarde"))return "tarde";
        if(contains(n,"entardecer","crepusculo"))return "crepusculo";
        if(contains(n,"noite"))return "noite";
        return n;
    }
    private static boolean hasRoad(JSONObject a,JSONObject b){return roadCount(a)>0||roadCount(b)>0;}
    private static int roadCount(JSONObject h){
        if(h==null)return 0;
        Object value=h.opt("roads_json");
        if(value instanceof JSONArray)return ((JSONArray)value).length();
        try{return new JSONArray(String.valueOf(value==null?"[]":value)).length();}catch(Exception ignored){AppLog.ignored("EnvironmentalDirector","Falha tolerada; a execução continuou.",ignored);}
        String n=fold(h.optString("roads","")+" "+h.optString("road",""));
        return n.length()>0?1:0;
    }

    private static JSONArray filterRecent(JSONArray values,JSONArray recent,int max){
        JSONArray out=new JSONArray();
        for(int i=0;i<values.length()&&out.length()<max;i++){
            String value=values.optString(i,"").trim();
            if(value.length()==0)continue;
            String sig=signature(value);
            if(!containsSignature(recent,sig))out.put(value);
        }
        if(out.length()==0&&values.length()>0)out.put(values.optString(0));
        return out;
    }
    private static boolean containsSignature(JSONArray values,String sig){
        if(values==null||sig==null||sig.length()==0)return false;
        for(int i=0;i<values.length();i++)if(sig.equals(values.optString(i)))return true;
        return false;
    }
    private static void pushRecent(JSONArray values,String sig){
        if(sig==null||sig.length()==0||containsSignature(values,sig))return;
        values.put(sig);
    }
    private static JSONArray trimRecent(JSONArray values,int max){
        JSONArray out=new JSONArray();
        int start=Math.max(0,values.length()-Math.max(1,max));
        for(int i=start;i<values.length();i++)out.put(values.opt(i));
        return out;
    }
    private static JSONArray copyArray(JSONArray source){
        try{return source==null?new JSONArray():new JSONArray(source.toString());}catch(Exception e){return new JSONArray();}
    }
    private static void appendStrings(JSONArray target,JSONArray source){
        if(source==null)return;
        for(int i=0;i<source.length();i++)putUnique(target,source.optString(i));
    }
    private static void putUnique(JSONArray target,String value){
        if(value==null||value.trim().length()==0)return;
        String sig=signature(value);
        for(int i=0;i<target.length();i++)if(sig.equals(signature(target.optString(i))))return;
        target.put(value.trim());
    }
    private static void appendHint(StringBuilder b,String value){
        if(value==null||value.trim().length()==0)return;
        if(b.length()>0)b.append(' ');
        b.append(value.trim());
    }
    private static String joinNonEmpty(String a,String b){
        if(a==null||a.trim().length()==0)return b==null?"":b.trim();
        if(b==null||b.trim().length()==0)return a.trim();
        return a.trim()+" "+b.trim();
    }
    private static JSONArray mergeObjectArrays(JSONArray a,JSONArray b,String k1,String k2){
        JSONArray out=new JSONArray();
        appendObjects(out,a,k1,k2);
        appendObjects(out,b,k1,k2);
        return out;
    }
    private static void appendObjects(JSONArray out,JSONArray source,String k1,String k2){
        if(source==null)return;
        for(int i=0;i<source.length();i++){
            JSONObject item=source.optJSONObject(i);if(item==null)continue;
            String sig=signature(item.optString(k1)+"|"+item.optString(k2));
            boolean exists=false;
            for(int j=0;j<out.length();j++){JSONObject old=out.optJSONObject(j);if(old!=null&&sig.equals(signature(old.optString(k1)+"|"+old.optString(k2)))){exists=true;break;}}
            if(!exists)out.put(item);
        }
    }

    private static boolean containsText(JSONArray values,String target){
        for(int i=0;i<values.length();i++)if(target.equalsIgnoreCase(values.optString(i)))return true;
        return false;
    }
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw==null?"[]":raw);}catch(Exception e){return new JSONArray();}}
    private static void copy(JSONObject from,JSONObject to)throws Exception{Iterator<String> it=from.keys();while(it.hasNext()){String k=it.next();to.put(k,from.opt(k));}}
    private static boolean contains(String text,String... values){String n=fold(text);for(String v:values)if(n.contains(fold(v)))return true;return false;}
    private static String firstNonEmpty(String... values){for(String v:values)if(v!=null&&v.trim().length()>0)return v.trim();return "";}
    private static int positiveHash(String value){int h=(value==null?"":value).hashCode();return h==Integer.MIN_VALUE?0:Math.abs(h);}
    private static String signature(String value){return Integer.toHexString(fold(value).hashCode());}
    private static String capitalize(String value){if(value==null||value.length()==0)return "";return Character.toUpperCase(value.charAt(0))+value.substring(1);}
    private static String safe(String value){return value==null?"":value;}
    private static String fold(String value){
        if(value==null)return "";
        return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();
    }
}
