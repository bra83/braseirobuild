package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Viagem e ambiente do projeto Pathfinder 2e.
 *
 * O motor calcula apenas geometria/tempo determinístico. Condições, testes,
 * dano, fadiga ou outros efeitos de regra só entram por procedimento PF2e
 * recuperado e verificado; não existe fallback para outro sistema.
 */
public final class TravelRulesEngine {
    public static final String RULE_RAW="PF2E_VERIFIED";
    public static final String RULE_NARRATIVE="NARRATIVE_ONLY";
    public static final String RULE_OPTIONAL="PF2E_OPTIONAL";
    public static final String RULE_GM="PF2E_GM_VERIFIED";
    private final AppDb db;

    public TravelRulesEngine(AppDb database){db=database;}

    public JSONObject previewNarrativeAction(String action,double requestedHours){
        JSONObject out=new JSONObject();
        try{
            String n=norm(action);boolean travel=isTravel(n);double hours=Math.max(0d,requestedHours);
            out.put("system","pathfinder_2e").put("rules_profile",Pathfinder2eRulesEngine.PROFILE).put("travel_detected",travel);
            if(!travel){out.put("mechanics_apply",false).put("classification",RULE_NARRATIVE).put("reason","A ação não foi identificada como deslocamento.");return out;}
            JSONObject map=db.getMapState();String packageId=map.optString("map_package_id","").trim();String hexId=map.optString("current_hex_id","").trim();
            if(packageId.isEmpty()||hexId.isEmpty()){
                out.put("mechanics_apply",false).put("classification",RULE_NARRATIVE).put("elapsed_hours",hours).put("movement_hours",hours).put("hours",hours);
                out.put("cartographic_grounding",false).put("reason","Tempo narrativo preservado sem inventar posição, distância, terreno ou rota.");
                return out;
            }
            JSONObject hex=db.getMapHex(packageId,hexId);String terrain=hex.optString("terrain_primary","open");String pace=paceFromAction(n);
            double distance=distanceForHours(hours,canonicalPf2Pace(pace),terrain,hasRoad(hex));
            return previewPf2Segment(distance,terrain,pace,hasRoad(hex),db.getSceneState().optString("weather",""),signature(action+"|"+hours+"|"+hexId));
        }catch(Exception e){try{out.put("system","pathfinder_2e").put("mechanics_apply",false).put("classification",RULE_NARRATIVE).put("error",safe(e.getMessage()));}catch(Exception ignored){}return out;}
    }

    /** Estimativa cartográfica PF2e. Não inventa efeitos mecânicos adicionais. */
    public JSONObject previewPf2Segment(double distanceKm,String terrain,String requestedPace,boolean road,String weather,String actionKey){
        JSONObject out=new JSONObject();
        try{
            String pace=canonicalPf2Pace(requestedPace);double distance=Math.max(0d,distanceKm);double speed=pf2SpeedKmH(pace,terrain,road);double hours=hoursForDistance(distance,pace,terrain,road);
            out.put("system","pathfinder_2e").put("rules_profile",Pathfinder2eRulesEngine.PROFILE).put("classification",RULE_RAW).put("mechanics_apply",hours>0d);
            out.put("distance_km",round2(distance)).put("movement_hours",round2(hours)).put("hours",round2(hours)).put("elapsed_hours",round2(hours)).put("rest_periods",0);
            out.put("pace",pace).put("terrain",safe(terrain)).put("road",road).put("speed_km_h",round2(speed)).put("party_speed_ft",partyTravelSpeedFeet()).put("hustle_limit_minutes",round2(partyHustleLimitHours()*60d)).put("weather",safe(weather));
            out.put("action_signature",signature(actionKey+"|pf2|"+distance+"|"+terrain+"|"+pace));
            out.put("cartographic_grounding",true).put("rules_note","Estimativa de deslocamento do Atlas. Efeitos mecânicos adicionais exigem uma regra Pathfinder 2e explicitamente recuperada e verificada.");
            out.put("saving_throws",new JSONArray()).put("environmental_effects",new JSONArray()).put("condition_changes",new JSONArray()).put("resource_consumption",new JSONObject().put("mechanics_apply",false));
        }catch(Exception e){try{out.put("system","pathfinder_2e").put("mechanics_apply",false).put("error",safe(e.getMessage()));}catch(Exception ignored){}}
        return out;
    }

    public JSONObject normalScheduleForMovement(double movementHours){
        JSONObject o=new JSONObject();try{double h=Math.max(0d,movementHours);o.put("movement_hours",round2(h)).put("elapsed_hours",round2(h)).put("rest_periods",0).put("system","pathfinder_2e");}catch(Exception ignored){}return o;
    }

    /** Passagem de tempo não consome provisões por inferência. */
    public JSONObject previewElapsedTimeResources(double elapsedHours,String actionKey){
        JSONObject o=new JSONObject();try{o.put("system","pathfinder_2e").put("classification",RULE_NARRATIVE).put("mechanics_apply",false).put("elapsed_hours",round2(Math.max(0d,elapsedHours))).put("action_signature",signature(actionKey)).put("reason","Consumo de recursos depende do estado da campanha e de procedimento PF2e verificado; nenhum consumo foi inventado.");}catch(Exception ignored){}return o;
    }

    /** Clima influencia a ficção; mecânica automática é fail-closed sem regra recuperada. */
    public JSONObject environmentalMechanics(String weather){
        JSONObject o=new JSONObject();try{o.put("system","pathfinder_2e").put("weather",safe(weather)).put("classification",RULE_NARRATIVE).put("mechanics_apply",false).put("effects",new JSONArray()).put("requires_verified_pf2_rule",true).put("reason","O clima pode alterar descrição, visibilidade ficcional e decisões do Mundo Vivo, mas não cria condição, dano, penalidade ou teste sem regra PF2e verificada.");}catch(Exception ignored){}return o;
    }

    /** Confirma somente relatórios produzidos pelo caminho Pathfinder. */
    public synchronized boolean commit(JSONObject report){
        if(report==null||report.optBoolean("committed",false))return false;
        String system=norm(report.optString("system",""));if(!system.equals("pathfinder_2e")&&!system.equals("pf2e_remaster"))return false;
        if(report.optJSONArray("exhaustion_changes")!=null&&report.optJSONArray("exhaustion_changes").length()>0)return false;
        try{
            JSONObject state=db.getTravelRulesState();
            state.put("system","pathfinder_2e").put("rules_profile",Pathfinder2eRulesEngine.PROFILE);
            state.put("last_signature",report.optString("action_signature",""));
            state.put("last_distance_km",report.optDouble("distance_km",0d));
            state.put("last_movement_hours",report.optDouble("movement_hours",report.optDouble("hours",0d)));
            state.put("last_elapsed_hours",report.optDouble("elapsed_hours",report.optDouble("hours",0d)));
            state.put("last_pace",report.optString("pace","Normal"));state.put("last_terrain",report.optString("terrain",""));
            state.put("last_committed_at",System.currentTimeMillis());
            db.saveTravelRulesStateChecked(state);report.put("committed",true);return true;
        }catch(Exception error){AppLog.w("TravelRulesEngine","Viagem PF2e não persistida.",error);return false;}
    }

    public void resetDailyTravel(){try{resetDailyTravelChecked();}catch(Exception error){AppLog.w("TravelRulesEngine","Não foi possível limpar o estado diário PF2e.",error);}}
    public void resetDailyTravelChecked()throws Exception{JSONObject state=db.getTravelRulesState();state.put("system","pathfinder_2e").put("hours_since_long_rest",0d).put("last_reset_at",System.currentTimeMillis());db.saveTravelRulesStateChecked(state);}

    /** Compatibilidade de migração: chamadas antigas são bloqueadas e nunca aplicam mecânica. */

    private String canonicalPf2Pace(String requested){String n=norm(requested);if(n.contains("rap")||n.contains("hustle"))return "Rápido";if(n.contains("caut")||n.contains("lento")||n.contains("slow"))return "Cauteloso";return "Normal";}
    private String paceFromAction(String n){if(n.contains("corr")||n.contains("rap"))return "Rápido";if(n.contains("caut")||n.contains("devagar")||n.contains("explor"))return "Cauteloso";return "Normal";}
    private double pf2SpeedKmH(String pace,String terrain,boolean road){double base=(partyTravelSpeedFeet()/10d)*1.609344d;String n=norm(terrain);boolean greater=n.contains("greater")||n.contains("maior")||n.contains("extrem")||n.contains("severe");boolean difficult=greater||n.contains("mount")||n.contains("montanh")||n.contains("swamp")||n.contains("pant")||n.contains("dense")||n.contains("densa")||n.contains("snow")||n.contains("neve");if(difficult&&!road)base*=greater?(1d/3d):.5d;if("Rápido".equals(pace))base*=2d;else if("Cauteloso".equals(pace))base*=.5d;return base;}
    private double hoursForDistance(double distance,String pace,String terrain,boolean road){double normal=pf2SpeedKmH("Normal",terrain,road);if(!"Rápido".equals(pace))return distance/Math.max(.1d,pf2SpeedKmH(pace,terrain,road));double limit=partyHustleLimitHours(),fast=normal*2d,fastDistance=fast*limit;if(distance<=fastDistance)return distance/Math.max(.1d,fast);return limit+(distance-fastDistance)/Math.max(.1d,normal);}
    private double distanceForHours(double hours,String pace,String terrain,boolean road){double h=Math.max(0d,hours),normal=pf2SpeedKmH("Normal",terrain,road);if(!"Rápido".equals(pace))return h*pf2SpeedKmH(pace,terrain,road);double limit=Math.min(h,partyHustleLimitHours());return limit*normal*2d+Math.max(0d,h-limit)*normal;}
    private int partyTravelSpeedFeet(){int slowest=Integer.MAX_VALUE;for(JSONObject hero:db.listActiveCharacters()){int speed=parseSpeedFeet(hero);if(speed>0)slowest=Math.min(slowest,speed);}return slowest==Integer.MAX_VALUE?25:slowest;}
    private double partyHustleLimitHours(){int lowest=Integer.MAX_VALUE;Pathfinder2eRulesEngine rules=new Pathfinder2eRulesEngine();for(JSONObject hero:db.listActiveCharacters())lowest=Math.min(lowest,rules.attributeModifier(hero,"Constituição"));if(lowest==Integer.MAX_VALUE)lowest=0;return Math.max(10,lowest*10)/60d;}
    private int parseSpeedFeet(JSONObject hero){try{JSONObject sheet=hero.opt("sheet_json") instanceof JSONObject?(JSONObject)hero.opt("sheet_json"):new JSONObject(hero.optString("sheet_json","{}"));if(sheet.has("speed_ft"))return Math.max(5,sheet.optInt("speed_ft",25));String raw=sheet.optString("speed",hero.optString("speed",""));java.util.regex.Matcher m=java.util.regex.Pattern.compile("([0-9]+(?:[.,][0-9]+)?)").matcher(raw);if(!m.find())return 25;double value=Double.parseDouble(m.group(1).replace(',','.'));String n=norm(raw);if(n.contains(" m")||n.endsWith("m")||n.contains("metro")){double feet=value*3.280839895d;return Math.max(5,(int)(Math.round(feet/5d)*5));}return Math.max(5,(int)Math.round(value));}catch(Exception ignored){return 25;}}
    private static boolean hasRoad(JSONObject hex){if(hex==null)return false;try{JSONArray roads=new JSONArray(hex.optString("roads_json","[]"));for(int i=0;i<roads.length();i++){JSONObject r=roads.optJSONObject(i);if(r!=null&&!r.optBoolean("needsReview",false))return true;}}catch(Exception ignored){}return false;}
    private static boolean isTravel(String n){return contains(n,"viaj","seguir viagem","caminh","marchar","desloc","partir para","ir ate","ir para","atravess","percorrer","avancar");}
    private static boolean contains(String n,String... terms){for(String t:terms)if(n.contains(norm(t)))return true;return false;}
    private static String signature(String s){return Integer.toHexString((s==null?"":s).hashCode());}
    private static double round2(double v){return Math.round(v*100d)/100d;}
    private static String safe(String v){return v==null?"":v;}
    private static String norm(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_+\\- ]"," ").replaceAll("\\s+"," ").trim();}
}
