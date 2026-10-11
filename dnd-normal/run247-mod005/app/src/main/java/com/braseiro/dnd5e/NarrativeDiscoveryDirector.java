package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Decide quando o Mestre deve interromper a rotina e APRESENTAR o mundo.
 *
 * O ledger é por campanha/perfil e mora no SQLite. Uma descoberta só vira
 * "conhecida" depois de uma resposta narrativa aceita. Local, geografia, POI
 * e clima são sinais separados: mudar um relógio do Mundo Vivo não transforma
 * toda fala em changed_return e atravessar um hex não vira falsa cidade nova.
 */
public final class NarrativeDiscoveryDirector {
    private final AppDb db;

    public NarrativeDiscoveryDirector(AppDb database){
        if(database==null)throw new IllegalArgumentException("Banco da descoberta ausente.");
        db=database;
        AppDb.ensureNarrativeDiscoverySchema(db.getWritableDatabase());
    }

    public JSONObject evaluate(boolean narrative,boolean nonAdvancing,JSONObject scene,JSONObject map,JSONObject environmental){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            JSONObject runtime=db.getNarrativeDiscoveryRuntime();
            int day=Math.max(1,scene==null?1:scene.optInt("day_number",1));
            JSONObject signals=signals(scene,map,environmental);
            String locationKey=signals.optString("location_key"),locationImprint=signals.optString("location_imprint");
            String geographyKey=signals.optString("geography_key"),poiKey=signals.optString("poi_key"),weatherKey=signals.optString("weather_key");
            boolean storyStarted=db.getStoryState().optInt("started",0)==1||runtime.optInt("campaign_started",0)==1;
            boolean unknownLocation=locationKey.length()>0&&db.getNarrativeDiscovery("location",locationKey).length()==0;
            boolean unknownPoi=poiKey.length()>0&&db.getNarrativeDiscovery("poi",poiKey).length()==0;
            boolean unknownGeography=geographyKey.length()>0&&db.getNarrativeDiscovery("geography",geographyKey).length()==0;
            boolean weatherTransition=signals.optBoolean("weather_changed",false)&&weatherKey.length()>0&&!weatherKey.equals(runtime.optString("last_weather_key"));
            boolean returnedChanged=false;
            if(locationKey.length()>0&&!unknownLocation){
                String previousPosition=runtime.optString("last_location_key");
                if(previousPosition.length()>0&&!locationKey.equals(previousPosition)){
                    JSONObject known=db.getNarrativeDiscovery("location",locationKey);
                    returnedChanged=known.length()>0&&locationImprint.length()>0&&!locationImprint.equals(known.optString("imprint"));
                }
            }

            JSONArray topics=new JSONArray();
            if(!storyStarted){
                addTopic(topics,"location",locationKey.length()>0);
                addTopic(topics,"poi",poiKey.length()>0);
                addTopic(topics,"geography",geographyKey.length()>0);
                // A abertura precisa situar o clima atual mesmo que ainda não exista uma transição.
                addTopic(topics,"weather",weatherKey.length()>0);
            }else{
                addTopic(topics,"location",unknownLocation&&!signals.optBoolean("primary_is_poi",false));
                addTopic(topics,"poi",unknownPoi);
                addTopic(topics,"geography",unknownGeography);
                addTopic(topics,"weather",weatherTransition);
                addTopic(topics,"changed_location",returnedChanged);
            }

            String occasion="continuation";
            if(narrative&&!nonAdvancing){
                if(!storyStarted)occasion="campaign_opening";
                else if(unknownPoi)occasion="poi_discovery";
                else if(unknownLocation)occasion="first_arrival";
                else if(unknownGeography)occasion="geography_discovery";
                else if(weatherTransition)occasion="weather_transition";
                else if(returnedChanged)occasion="changed_return";
            }
            out.put("occasion",occasion).put("signals",signals).put("day",day).put("campaign_started",storyStarted);
            out.put("presentation_topics",topics).put("requires_large_presentation",requiresLargePresentation(occasion));
            out.put("contract",contract(occasion,signals,topics));
        }catch(Exception failure){
            AppLog.e("NarrativeDiscoveryDirector","Falha ao classificar descoberta narrativa.",failure);
            try{out.put("occasion","continuation").put("presentation_topics",new JSONArray()).put("error",failure.getClass().getSimpleName());}catch(Exception ignored){AppLog.ignored("NarrativeDiscoveryDirector","Falha ao registrar diagnóstico.",ignored);}
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Persiste somente o que já era conhecido ou o que a apresentação aceita realmente cobriu. */
    public void commit(JSONObject decision){/*BR_JSON_WRAP*/try{
        if(decision==null)return;
        try{
            JSONObject signals=decision.optJSONObject("signals");if(signals==null)signals=new JSONObject();
            JSONArray topics=decision.optJSONArray("presentation_topics");if(topics==null)topics=new JSONArray();
            int day=Math.max(1,decision.optInt("day",db.getSceneState().optInt("day_number",1)));
            boolean locationKnown=recordIfAllowed("location",signals,"location_key","location_label","location_imprint",day,topics,"location","changed_location");
            boolean geographyKnown=recordIfAllowed("geography",signals,"geography_key","geography_label","geography_imprint",day,topics,"geography");
            boolean poiKnown=recordIfAllowed("poi",signals,"poi_key","poi_label","poi_imprint",day,topics,"poi");
            boolean weatherKnown=recordIfAllowed("weather",signals,"weather_key","weather_label","weather_imprint",day,topics,"weather");

            JSONObject runtime=db.getNarrativeDiscoveryRuntime();
            runtime.put("campaign_started",1);
            if(locationKnown){copyRuntime(runtime,signals,"last_location_key","location_key");copyRuntime(runtime,signals,"last_location_imprint","location_imprint");}
            else{
                // Marca apenas que o grupo saiu do último local nomeado; não cria uma falsa descoberta por hex.
                String packageId=signals.optString("map_package_id"),hex=signals.optString("hex_id");
                if(hex.length()>0)runtime.put("last_location_key","@position:"+fold(packageId+" "+hex));
            }
            if(geographyKnown)copyRuntime(runtime,signals,"last_geography_key","geography_key");
            if(poiKnown)copyRuntime(runtime,signals,"last_poi_key","poi_key");
            if(weatherKnown)copyRuntime(runtime,signals,"last_weather_key","weather_key");
            db.saveNarrativeDiscoveryRuntime(runtime);
        }catch(Exception failure){AppLog.e("NarrativeDiscoveryDirector","Falha ao persistir descoberta aceita.",failure);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject diagnostic(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{List<JSONObject> known=db.listNarrativeDiscovery();out.put("runtime",db.getNarrativeDiscoveryRuntime()).put("known",new JSONArray(known)).put("known_count",known.size());}
        catch(Exception failure){AppLog.w("NarrativeDiscoveryDirector","Diagnóstico indisponível.",failure);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject signals(JSONObject scene,JSONObject map,JSONObject environmental)throws Exception{/*BR_JSON_WRAP*/try{
        if(scene==null)scene=new JSONObject();if(map==null)map=new JSONObject();if(environmental==null)environmental=new JSONObject();
        JSONObject out=new JSONObject();String packageId=map.optString("map_package_id"),hexId=map.optString("current_hex_id",scene.optString("hex_id"));
        List<JSONObject> locations=(packageId.length()>0&&hexId.length()>0)?db.getMapLocationsForHex(packageId,hexId):Collections.<JSONObject>emptyList();
        String sceneLocation=clean(scene.optString("location"));JSONObject primary=bestLocation(sceneLocation,locations);
        String locationLabel=primary.length()>0?clean(primary.optString("name")):sceneLocation;
        String locationKey=primary.length()>0&&clean(primary.optString("location_id")).length()>0?"id:"+fold(primary.optString("location_id")):(locationLabel.length()>0?"name:"+fold(locationLabel):"");
        String region=primary.length()>0?clean(primary.optString("region")):"";
        boolean primaryPoi=primary.length()>0&&!isSettlement(primary.optString("type"));
        if(locationKey.length()>0){
            out.put("location_key",locationKey).put("location_label",locationLabel).put("location_kind",primary.length()>0?clean(primary.optString("type")):"named_place").put("primary_is_poi",primaryPoi);
            out.put("location_imprint",hash(join(locationLabel,region,scene.optString("danger"),scene.optString("objective"),scene.optString("threats_json"),scene.optString("clues_json"))));
            if(primaryPoi){out.put("poi_key",locationKey).put("poi_label",locationLabel).put("poi_imprint",hash(join(locationLabel,primary.optString("type"),region,primary.optString("tags"))));}
        }

        JSONObject hex=(packageId.length()>0&&hexId.length()>0)?db.getRawMapHex(packageId,hexId):new JSONObject();
        String terrain=clean(hex.optString("terrain_primary")),secondary=clean(hex.optString("terrain_secondary"));
        if(terrain.length()==0){JSONObject terrainPlan=environmental.optJSONObject("terrain_system");if(terrainPlan!=null)terrain=clean(terrainPlan.optString("terrain",terrainPlan.optString("current_terrain")));}
        String geographyLabel=joinHuman(terrain,secondary,region);
        if(geographyLabel.length()>0){String geographyKey="geo:"+fold(geographyLabel);out.put("geography_key",geographyKey).put("geography_label",geographyLabel).put("geography_imprint",hash(join(terrain,secondary,region,hex.optString("roads_json"),hex.optString("waterways_json"),hex.optString("political_json"))));}

        JSONObject weather=environmental.optJSONObject("weather_system");if(weather==null)weather=new JSONObject();
        String display=clean(weather.optString("display_weather",scene.optString("weather"))),event=clean(weather.optString("event")),stage=clean(weather.optString("stage_name"));boolean changed=weather.optBoolean("weather_changed",false);
        if(display.length()>0){String key="weather:"+fold(join(display,event,stage));out.put("weather_key",key).put("weather_label",display).put("weather_changed",changed).put("weather_imprint",hash(join(display,event,stage,weather.optString("aftermath"),String.valueOf(weather.optInt("stage",0)))));}
        out.put("map_package_id",packageId).put("hex_id",hexId);
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONObject bestLocation(String sceneLocation,List<JSONObject> rows){
        JSONObject settlement=new JSONObject(),fallback=new JSONObject();String wanted=fold(sceneLocation);
        for(JSONObject row:rows){
            if(row==null||!confirmed(row))continue;String name=clean(row.optString("name"));if(name.length()==0)continue;
            if(wanted.length()>0&&fold(name).equals(wanted))return row;
            if(isSettlement(row.optString("type"))&&settlement.length()==0)settlement=row;
            if(fallback.length()==0)fallback=row;
        }
        // Se a cena nomeia um lugar que o atlas ainda não resolveu, preserve o nome narrativo sem inventar coordenadas.
        if(sceneLocation.length()>0)return new JSONObject();
        return settlement.length()>0?settlement:fallback;
    }
    private static boolean confirmed(JSONObject row){String status=fold(row.optString("placement_status"));return !status.contains("review")&&!status.contains("approx")&&!status.contains("unassigned");}
    private static boolean isSettlement(String type){String t=fold(type);return t.equals("cidade")||t.equals("vila")||t.equals("aldeia")||t.equals("porto")||t.equals("fortaleza")||t.equals("castelo")||t.equals("metropole")||t.equals("bairro");}

    private boolean recordIfAllowed(String kind,JSONObject s,String keyName,String labelName,String imprintName,int day,JSONArray topics,String...acceptedTopics){
        String key=s.optString(keyName);if(key.length()==0)return false;
        boolean known=db.getNarrativeDiscovery(kind,key).length()>0,covered=false;
        for(int i=0;i<topics.length()&&!covered;i++){String topic=topics.optString(i,"");for(String allowed:acceptedTopics)if(allowed.equals(topic)){covered=true;break;}}
        if(!known&&!covered)return false;
        db.recordNarrativeDiscovery(kind,key,s.optString(labelName),s.optString(imprintName),Math.max(1,day));return true;
    }
    private static void addTopic(JSONArray topics,String topic,boolean condition){/*BR_JSON_WRAP*/try{if(condition&&!containsTopic(topics,topic))topics.put(topic);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean containsTopic(JSONArray topics,String topic){for(int i=0;i<topics.length();i++)if(topic.equals(topics.optString(i)))return true;return false;}
    private static void copyRuntime(JSONObject runtime,JSONObject signals,String target,String source)throws Exception{/*BR_JSON_WRAP*/try{String value=signals.optString(source);if(value.length()>0)runtime.put(target,value);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean requiresLargePresentation(String occasion){return !"continuation".equals(occasion);}
    private static JSONObject contract(String occasion,JSONObject signals,JSONArray topics)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject c=new JSONObject().put("occasion",occasion).put("assume_setting_literacy_zero",true).put("story_before_choices",true).put("presentation_topics",topics);
        if("campaign_opening".equals(occasion))c.put("min_words",1200).put("min_paragraphs",10).put("target_words","1200-2200");
        else if("first_arrival".equals(occasion))c.put("min_words",850).put("min_paragraphs",8).put("target_words","850-1600");
        else if("changed_return".equals(occasion))c.put("min_words",600).put("min_paragraphs",6).put("target_words","600-1200");
        else if("poi_discovery".equals(occasion))c.put("min_words",600).put("min_paragraphs",6).put("target_words","600-1200");
        else if("geography_discovery".equals(occasion))c.put("min_words",500).put("min_paragraphs",5).put("target_words","500-1000");
        else if("weather_transition".equals(occasion))c.put("min_words",350).put("min_paragraphs",4).put("target_words","350-800");
        c.put("focus",signals);return c;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String clean(String s){return s==null?"":s.trim();}
    private static String fold(String s){return Normalizer.normalize(clean(s),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
    private static String join(Object... values){StringBuilder b=new StringBuilder();for(Object v:values){String s=v==null?"":String.valueOf(v).trim();if(s.length()==0)continue;if(b.length()>0)b.append('|');b.append(s);}return b.toString();}
    private static String joinHuman(String... values){StringBuilder b=new StringBuilder();for(String s:values){s=clean(s);if(s.length()==0)continue;if(b.length()>0)b.append(" • ");b.append(s);}return b.toString();}
    private static String hash(String value){try{byte[] d=MessageDigest.getInstance("SHA-256").digest((value==null?"":value).getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format(Locale.US,"%02x",x&255));return b.toString();}catch(Exception e){return Integer.toHexString((value==null?"":value).hashCode());}}
}
