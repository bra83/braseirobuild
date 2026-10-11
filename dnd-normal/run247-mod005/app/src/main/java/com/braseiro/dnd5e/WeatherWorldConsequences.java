package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Ponte determinística entre clima, ficção e Mundo Vivo.
 *
 * Regras de autoridade:
 * - clima pode alterar acesso, timing, circulação, rastros e agendas;
 * - clima NÃO conclui/falha missão, NÃO avança relógio e NÃO inventa hidrologia;
 * - pressão só alcança entidades geograficamente vinculadas à área afetada;
 * - rescaldo persiste depois que o fenômeno termina.
 */
public final class WeatherWorldConsequences {
    private final AppDb db;

    public WeatherWorldConsequences(AppDb database){db=database;}

    public JSONObject enrich(JSONObject weather,JSONObject map,JSONObject scene,JSONObject currentHex,JSONObject nextHex,JSONArray nearbyPlaces){/*BR_JSON_WRAP*/try{
        JSONObject out=cloneObject(weather);
        try{
            boolean hydrology=confirmedHydrology(currentHex,nextHex);
            JSONObject scope=buildScope(map,scene,nearbyPlaces);
            int stage=out.optInt("stage",0);
            String event=out.optString("event","");
            String aftermath=out.optString("aftermath","");
            boolean active=stage>=3;
            boolean severe=active&&isSevereEvent(event);

            out.put("hydrology_confirmed",hydrology);
            out.put("geographic_scope",scope);
            out.put("world_consequences",worldConsequences(event,stage,aftermath,hydrology,scope));
            out.put("settlement_consequences",settlementConsequences(event,stage,aftermath,scope));
            out.put("trace_consequences",traceConsequences(event,stage,aftermath,scene));
            out.put("plot_pressure",plotPressure(event,stage,aftermath,scope,scene));
            out.put("narrative_weather_instruction",out.optBoolean("narrative_beat_required",false)?
                    "A mudança do tempo precisa aparecer organicamente nesta cena por sinais sensoriais e por pelo menos uma consequência concreta no ambiente/comportamento; não trate clima como legenda de HUD.":
                    "Mantenha continuidade do clima e do rescaldo sem repetir descrição meteorológica sem mudança relevante.");
            out.put("living_world_pressure",new JSONObject()
                    .put("active",active||aftermath.length()>0)
                    .put("severe",severe)
                    .put("event",event)
                    .put("stage",stage)
                    .put("aftermath",aftermath)
                    .put("delay_days",severe?1:0)
                    .put("scope",scope)
                    .put("policy","Atrasar somente compromissos externos geograficamente vinculados; nunca congelar o mundo inteiro."));
            out.put("authority_guard",new JSONObject()
                    .put("may_change_access",true)
                    .put("may_change_timing",true)
                    .put("may_change_npc_routes",true)
                    .put("may_change_trace_legibility",true)
                    .put("may_fail_quest",false)
                    .put("may_complete_quest",false)
                    .put("may_advance_clock",false)
                    .put("may_invent_water",false));
        }catch(Exception e){
            try{out.put("consequence_error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(Exception ignored){AppLog.ignored("WeatherWorldConsequences","Falha ao registrar erro.",ignored);}
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Aplica pressão climática à prévia do Mundo Vivo, sem gravar nada. */
    public void applyToLivingWorldPreview(JSONObject preview){/*BR_JSON_WRAP*/try{
        if(preview==null)return;
        try{
            JSONObject pressure=currentPressure();
            preview.put("weather_pressure",pressure);
            if(!pressure.optBoolean("severe",false))return;
            JSONObject scope=pressure.optJSONObject("scope");
            int delay=Math.max(1,pressure.optInt("delay_days",1));
            markDelays(preview.optJSONArray("faction_actions"),"faction",scope,delay,pressure);
            markDelays(preview.optJSONArray("npc_actions"),"npc",scope,delay,pressure);
            markDelays(preview.optJSONArray("activity_updates"),"activity",scope,delay,pressure);
            markDelays(preview.optJSONArray("troupe_updates"),"troupe",scope,delay,pressure);
        }catch(Exception e){AppLog.w("WeatherWorldConsequences","Pressão climática do Mundo Vivo ignorada com segurança.",e);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject currentPressure(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            JSONObject state=db.getEnvironmentalDirectorState();
            JSONObject weather=state.optJSONObject("weather_system");if(weather==null)weather=new JSONObject();
            JSONObject map=db.getMapState(),scene=db.getSceneState();
            JSONObject scope=weather.optJSONObject("geographic_scope");
            if(scope==null||scope.length()==0)scope=buildScope(map,scene,new JSONArray());
            int stage=weather.optInt("stage",0);String event=weather.optString("event","");String aftermath=weather.optString("aftermath","");
            boolean severe=stage>=3&&isSevereEvent(event);
            out.put("active",stage>0||aftermath.length()>0).put("severe",severe).put("event",event).put("stage",stage)
                    .put("stage_name",weather.optString("stage_name","")).put("aftermath",aftermath)
                    .put("delay_days",severe?1:0).put("scope",scope)
                    .put("source","environmental_director");
        }catch(Exception e){try{out.put("active",false).put("severe",false);}catch(Exception ignored){AppLog.ignored("WeatherWorldConsequences","Falha tolerada.",ignored);}}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void markDelays(JSONArray actions,String kind,JSONObject scope,int delay,JSONObject pressure)throws Exception{/*BR_JSON_WRAP*/try{
        if(actions==null)return;
        for(int i=0;i<actions.length();i++){
            JSONObject action=actions.optJSONObject(i);if(action==null||action.optBoolean("pressure_only",false))continue;
            String location=actionLocation(action,kind);
            String description=actionDescription(action,kind);
            if(!isOutdoorCommitment(description))continue;
            if(!locationInScope(location,action,scope))continue;
            action.put("weather_delayed",true)
                    .put("weather_delay_days",delay)
                    .put("weather_event",pressure.optString("event"))
                    .put("weather_delay_reason","Clima severo torna este compromisso externo imprudente ou logisticamente inviável na área afetada.")
                    .put("weather_scope",scope);
            if("npc".equals(kind)){
                int due=action.optInt("last_action_day",0);
                action.put("weather_delay_until_day",Math.max(1,due+delay));
                action.put("urgency_after",Math.min(100,Math.max(action.optInt("urgency_before",0),action.optInt("urgency_after",0))+8));
                action.put("summary",action.optString("name","NPC")+" precisou adiar "+plainAction(action.optString("action","seu compromisso"))+" por causa de "+humanEvent(pressure.optString("event"))+".");
            }else if("activity".equals(kind)){
                action.put("weather_delay_until_day",Math.max(1,db.getSceneState().optInt("day_number",1)+delay));
                action.put("narrative",action.optString("title","Atividade")+" foi interrompida pelo clima severo; o progresso não é recuperado magicamente no dia seguinte.");
            }else if("troupe".equals(kind)){
                action.put("summary","A etapa de viagem foi adiada por "+humanEvent(pressure.optString("event"))+" na rota confirmada.");
            }else if("faction".equals(kind)){
                action.put("summary",action.optString("name","A facção")+" adiou "+action.optString("step_title","uma etapa externa")+" devido ao clima severo na área.");
            }
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONArray plotPressure(String event,int stage,String aftermath,JSONObject scope,JSONObject scene){/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();
        if(stage<2&&aftermath.length()==0)return out;
        try{
            for(JSONObject q:db.listQuests("active",80)){
                String hay=q.optString("title")+" "+q.optString("objective")+" "+q.optString("stakes")+" "+q.optString("stage")+" "+q.optString("progress_json");
                if(!textTouchesScope(hay,scope))continue;
                out.put(new JSONObject().put("kind","quest").put("id",q.optLong("id")).put("title",q.optString("title"))
                        .put("effect","O clima pode mudar acesso, logística ou timing desta missão; não a conclua nem a faça falhar automaticamente.")
                        .put("mechanical_mutation_allowed",false));
            }
            for(JSONObject c:db.listWorldClocks(true,80)){
                String hay=c.optString("name")+" "+c.optString("consequence")+" "+c.optString("owner_entity")+" "+c.optString("trigger_condition");
                if(!textTouchesScope(hay,scope))continue;
                out.put(new JSONObject().put("kind","world_clock").put("id",c.optLong("id")).put("title",c.optString("name"))
                        .put("effect","O clima pode mudar a representação ficcional do relógio, mas esta camada não altera seus segmentos ou valor.")
                        .put("mechanical_mutation_allowed",false));
            }
            JSONArray clues=parseArray(scene.optString("clues_json","[]"));
            if(clues.length()>0){
                out.put(new JSONObject().put("kind","scene_clues").put("count",clues.length())
                        .put("effect",traceEffect(event,stage,aftermath))
                        .put("truth_mutation_allowed",false));
            }
        }catch(Exception e){AppLog.w("WeatherWorldConsequences","Pressão de trama parcialmente indisponível.",e);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray worldConsequences(String event,int stage,String aftermath,boolean hydrology,JSONObject scope)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();
        if(stage<=0&&aftermath.length()==0)return out;
        String e=fold(event+" "+aftermath);
        if(contains(e,"chuva","tempestade")){
            out.put(effect("terrain","Solo exposto fica úmido, escorregadio e depois lamacento; isso persiste no rescaldo.",true));
            out.put(effect("traces","Chuva degrada rastros antigos e cria pegadas/rodados novos onde houver lama.",true));
            if(hydrology)out.put(effect("hydrology","Água já confirmada no Atlas pode subir, acelerar ou dificultar travessias; não presumir inundação automática.",true));
        }else if(contains(e,"nevasca","neve")){
            out.put(effect("terrain","Neve acumulada encobre referências e continua alterando deslocamento depois da nevasca.",true));
            out.put(effect("traces","Rastros antigos podem ser cobertos; rastros novos ficam muito visíveis na neve recente.",true));
        }else if(contains(e,"areia")){
            out.put(effect("terrain","Areia e poeira redesenham superfícies expostas e podem soterrar marcas rasas.",true));
            out.put(effect("traces","Rastros frágeis somem; movimento posterior pode produzir marcas novas nas deposições.",true));
        }else if(contains(e,"nevoa","névoa")){
            out.put(effect("visibility","Visibilidade curta altera aproximações, encontros e navegação sem mudar a geografia real.",false));
        }
        if(stage>=3)out.put(effect("logistics","Viagens, patrulhas, entregas, caravanas, caçadas e expedições locais podem atrasar.",false));
        if(aftermath.length()>0)out.put(effect("aftermath",aftermath,true));
        out.put(effect("scope","Consequências diretas ficam limitadas à área geográfica confirmada: "+scope.optString("label","área atual")+".",false));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray settlementConsequences(String event,int stage,String aftermath,JSONObject scope)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();if(stage<2&&aftermath.length()==0)return out;
        if(scope.optJSONArray("settlements")!=null&&scope.optJSONArray("settlements").length()>0){
            if(stage>=3){
                out.put("Vendedores e serviços externos podem recolher mercadorias, fechar bancas ou operar de forma reduzida.");
                out.put("Pessoas tendem a se concentrar em estalagens, pórticos, templos, mercados cobertos e outros abrigos já existentes.");
                out.put("Patrulhas e mensageiros podem mudar rota, frequência ou horário; isso altera quem pode ser encontrado e quando.");
                out.put("Entregas e caravanas locais podem acumular atraso, gerando filas, escassez localizada ou oportunidades narrativas sem inventar preços mecânicos.");
            }
            if(aftermath.length()>0)out.put("A rotina urbana retoma gradualmente; atrasos e danos leves não desaparecem no instante em que o céu abre.");
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray traceConsequences(String event,int stage,String aftermath,JSONObject scene)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();if(stage<2&&aftermath.length()==0)return out;
        JSONArray clues=parseArray(scene.optString("clues_json","[]"));
        if(clues.length()==0)return out;
        out.put(traceEffect(event,stage,aftermath));
        out.put("O clima pode alterar legibilidade/preservação, nunca a verdade canônica da pista e nunca revela segredo automaticamente.");
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String traceEffect(String event,int stage,String aftermath){
        String e=fold(event+" "+aftermath);
        if(contains(e,"chuva","tempestade"))return "Rastros expostos podem borrar ou ser lavados; lama posterior registra movimentos novos com clareza.";
        if(contains(e,"neve","nevasca"))return "Neve pode cobrir vestígios anteriores e registrar com grande nitidez quem passou depois.";
        if(contains(e,"areia"))return "Poeira/areia pode cobrir vestígios frágeis e, depois, registrar perturbações novas.";
        if(contains(e,"nevoa","névoa"))return "Névoa reduz observação à distância, mas não apaga fisicamente uma pista protegida.";
        return "O rescaldo pode alterar a leitura física da cena sem mudar o que realmente aconteceu.";
    }

    private static JSONObject buildScope(JSONObject map,JSONObject scene,JSONArray nearbyPlaces)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject scope=new JSONObject();JSONArray hexes=new JSONArray(),settlements=new JSONArray();
        String current=map==null?"":map.optString("current_hex_id",scene==null?"":scene.optString("hex_id",""));
        if(current.length()>0)putUnique(hexes,current);
        if(map!=null){JSONArray route=parseArray(map.optString("route_json","[]"));for(int i=0;i<route.length()&&i<2;i++){Object raw=route.opt(i);String h=raw instanceof JSONObject?((JSONObject)raw).optString("hex_id",((JSONObject)raw).optString("id")):String.valueOf(raw==null?"":raw);if(h.length()>0)putUnique(hexes,h);}}
        if(scene!=null&&scene.optString("location","").trim().length()>0)putUnique(settlements,scene.optString("location").trim());
        if(nearbyPlaces!=null)for(int i=0;i<nearbyPlaces.length();i++){JSONObject p=nearbyPlaces.optJSONObject(i);if(p==null)continue;if(p.optInt("route_steps",99)>1)continue;String n=p.optString("name","").trim();if(n.length()>0)putUnique(settlements,n);}
        String label=settlements.length()>0?settlements.optString(0):(current.length()>0?"hex "+current:"área atual");
        scope.put("current_hex",current).put("hexes",hexes).put("settlements",settlements).put("label",label).put("radius_route_steps",1);
        return scope;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static boolean confirmedHydrologyForDirector(JSONObject a,JSONObject b){return confirmedHydrology(a,b);}
    private static boolean confirmedHydrology(JSONObject a,JSONObject b){return hexHasWater(a)||hexHasWater(b);}
    private static boolean hexHasWater(JSONObject h){
        if(h==null)return false;
        String waterways=h.optString("waterways_json","").trim();
        if(waterways.length()>2&&!"null".equalsIgnoreCase(waterways)&&!"[]".equals(waterways)&&!"{}".equals(waterways))return true;
        if(h.optBoolean("river_crossing",false)||h.optInt("river_crossing",0)==1||h.optBoolean("water",false)||h.optInt("water",0)==1)return true;
        String terrain=fold(h.optString("terrain_primary","")+" "+h.optString("terrain","")+" "+h.optString("name",""));
        return WeatherCausalityKernel.terrainImpliesWater(terrain);
    }

    private static boolean locationInScope(String location,JSONObject action,JSONObject scope){
        if(scope==null)return false;
        String loc=fold(location);
        JSONArray hexes=scope.optJSONArray("hexes");
        String from=fold(action.optString("from_hex",action.optString("from",""))),to=fold(action.optString("to_hex",action.optString("to","")));
        if(hexes!=null)for(int i=0;i<hexes.length();i++){String h=fold(hexes.optString(i));if(h.length()>0&&(h.equals(from)||h.equals(to)||loc.contains(h)))return true;}
        JSONArray settlements=scope.optJSONArray("settlements");
        if(settlements!=null)for(int i=0;i<settlements.length();i++){String s=fold(settlements.optString(i));if(s.length()>2&&loc.contains(s))return true;}
        String current=fold(scope.optString("current_hex",""));
        return current.length()>0&&(from.equals(current)||to.equals(current));
    }

    private static boolean textTouchesScope(String text,JSONObject scope){
        String hay=fold(text);if(hay.length()==0)return false;
        JSONArray settlements=scope.optJSONArray("settlements");if(settlements!=null)for(int i=0;i<settlements.length();i++){String s=fold(settlements.optString(i));if(s.length()>2&&hay.contains(s))return true;}
        JSONArray hexes=scope.optJSONArray("hexes");if(hexes!=null)for(int i=0;i<hexes.length();i++){String h=fold(hexes.optString(i));if(h.length()>1&&hay.contains(h))return true;}
        return false;
    }

    private static boolean isOutdoorCommitment(String text){return WeatherCausalityKernel.isOutdoorCommitment(text);}

    private static String actionLocation(JSONObject a,String kind){
        if("faction".equals(kind))return a.optString("location_after","");
        if("npc".equals(kind))return a.optString("location","");
        if("activity".equals(kind))return a.optString("location",a.optString("narrative",""));
        if("troupe".equals(kind))return a.optString("from_hex",a.optString("from",""))+" "+a.optString("to_hex",a.optString("to",""));
        return "";
    }
    private static String actionDescription(JSONObject a,String kind){return a.optString("action","")+" "+a.optString("summary","")+" "+a.optString("step_title","")+" "+a.optString("title","")+" "+a.optString("narrative","")+" "+a.optString("rule_query","");}

    private static boolean isSevereEvent(String event){return WeatherCausalityKernel.isSevereEvent(event);}
    private static String humanEvent(String e){return e==null||e.trim().length()==0?"o mau tempo":e.trim().toLowerCase(new Locale("pt","BR"));}
    private static String plainAction(String a){return a==null||a.trim().length()==0?"o compromisso":a.trim();}
    private static JSONObject effect(String kind,String text,boolean persistent)throws Exception{/*BR_JSON_WRAP*/try{return new JSONObject().put("kind",kind).put("text",text).put("persists_after_weather",persistent);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject cloneObject(JSONObject in){try{return in==null?new JSONObject():new JSONObject(in.toString());}catch(Exception e){return new JSONObject();}}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw==null?"[]":raw);}catch(Exception e){return new JSONArray();}}
    private static void putUnique(JSONArray a,String value){/*BR_JSON_WRAP*/try{if(value==null||value.trim().length()==0)return;for(int i=0;i<a.length();i++)if(value.equalsIgnoreCase(a.optString(i)))return;a.put(value);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean contains(String text,String... terms){String t=fold(text);for(String term:terms)if(t.contains(fold(term)))return true;return false;}
    private static String fold(String value){if(value==null)return "";String s=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return s.replaceAll("[^a-z0-9]+"," ").trim();}
}
