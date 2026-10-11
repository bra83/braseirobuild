package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

/** Abstract settlement/region simulation. Values are narrative pressures, never hero modifiers. */
public final class LivingSettlementEngine {
    private final AppDb db;
    public LivingSettlementEngine(AppDb database){db=database;AppDb.ensureSixZeroSchema(db.getWritableDatabase());}

    public synchronized int seedFromAtlas(){/*BR_JSON_WRAP*/try{
        int added=0;String profile=db.getActiveMapProfile();JSONObject map=db.getMapState();String pack=map.optString("map_package_id");if(pack.isEmpty())return 0;
        List<JSONObject> locs=db.queryList("SELECT * FROM map_locations WHERE package_id=? AND name<>'' ORDER BY name",new String[]{pack});
        for(JSONObject l:locs){String type=l.optString("type").toLowerCase(Locale.ROOT);if(!isSettlement(type))continue;String key=(l.optString("location_id").isEmpty()?normalize(l.optString("name")):l.optString("location_id"));if(db.queryOne("SELECT id FROM settlement_states WHERE world_profile=? AND settlement_key=?",new String[]{profile,key}).length()>0)continue;ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("settlement_key",key);v.put("name",l.optString("name"));v.put("kind",l.optString("type","local"));v.put("region",l.optString("region"));v.put("hex_id",l.optString("hex_id"));int base=45+Math.abs(key.hashCode()%16);v.put("prosperity",base);v.put("security",45+Math.abs((key.hashCode()/7)%21));v.put("scarcity",15+Math.abs((key.hashCode()/11)%21));v.put("unrest",8+Math.abs((key.hashCode()/13)%18));v.put("criminality",10+Math.abs((key.hashCode()/17)%20));v.put("political_pressure",12+Math.abs((key.hashCode()/19)%20));v.put("trade",40+Math.abs((key.hashCode()/23)%31));v.put("faith_influence",20+Math.abs((key.hashCode()/29)%31));v.put("last_simulated_day",Math.max(1,db.getSceneState().optInt("day_number",1)));v.put("updated_at",System.currentTimeMillis());if(db.getWritableDatabase().insertWithOnConflict("settlement_states",null,v,SQLiteDatabase.CONFLICT_IGNORE)>0)added++;}
        return added;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized JSONObject simulateToDay(int targetDay){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();int changed=0,thresholdEvents=0;try{
            seedFromAtlas();String profile=db.getActiveMapProfile();
            for(JSONObject settlement:list(200)){
                int last=Math.max(1,settlement.optInt("last_simulated_day",1));int days=Math.max(0,Math.min(180,targetDay-last));if(days<=0)continue;
                JSONObject pressure=pressureFor(settlement);
                int prosperity=clamp(settlement.optInt("prosperity",50)+step(pressure.optInt("prosperity_delta"),days));
                int security=clamp(settlement.optInt("security",50)+step(pressure.optInt("security_delta"),days));
                int scarcity=clamp(settlement.optInt("scarcity",20)+step(pressure.optInt("scarcity_delta"),days));
                int unrest=clamp(settlement.optInt("unrest",10)+step(pressure.optInt("unrest_delta"),days));
                int crime=clamp(settlement.optInt("criminality",15)+step(pressure.optInt("crime_delta"),days));
                int politics=clamp(settlement.optInt("political_pressure",15)+step(pressure.optInt("political_delta"),days));
                int trade=clamp(settlement.optInt("trade",50)+step(pressure.optInt("trade_delta"),days));
                int faith=clamp(settlement.optInt("faith_influence",30)+step(pressure.optInt("faith_delta"),days));
                ContentValues v=new ContentValues();v.put("prosperity",prosperity);v.put("security",security);v.put("scarcity",scarcity);v.put("unrest",unrest);v.put("criminality",crime);v.put("political_pressure",politics);v.put("trade",trade);v.put("faith_influence",faith);v.put("last_simulated_day",targetDay);v.put("updated_at",System.currentTimeMillis());
                db.getWritableDatabase().update("settlement_states",v,"id=? AND world_profile=?",new String[]{String.valueOf(settlement.optLong("id")),profile});
                thresholdEvents+=recordThresholdCrossings(settlement,prosperity,security,scarcity,unrest,crime,politics,trade,faith,targetDay);changed++;
            }
            out.put("changed",changed).put("threshold_events",thresholdEvents).put("target_day",targetDay).put("settlements",listJson(40));
        }catch(Exception e){try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){AppLog.ignored("LivingSettlementEngine","Falha tolerada na camada 6.0.",ignored);}}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject pressureFor(JSONObject s){/*BR_JSON_WRAP*/try{JSONObject p=new JSONObject();try{
        String region=s.optString("region"),name=s.optString("name");int negative=0,positive=0,factionPressure=0,faithSignals=0,tradeSignals=0;
        for(JSONObject event:db.listWorldEvents(80)){String t=(event.optString("title")+" "+event.optString("summary")+" "+event.optString("location")).toLowerCase(Locale.ROOT);if((!region.isEmpty()&&t.contains(region.toLowerCase(Locale.ROOT)))||t.contains(name.toLowerCase(Locale.ROOT))){if(t.matches(".*(guerra|ataque|fome|praga|roubo|cerco|crise|incêndio|incendio|motim|rebeli).*"))negative++;if(t.matches(".*(feira|aliança|alianca|comércio|comercio|vitória|vitoria|colheita|acordo|festival).*"))positive++;if(t.matches(".*(templo|igreja|culto|fé|fe|peregrin).*"))faithSignals++;if(t.matches(".*(caravana|mercado|porto|rota|comércio|comercio).*"))tradeSignals++;}}
        for(JSONObject faction:db.listFactions(50)){String t=(faction.optString("territories_json")+" "+faction.optString("current_plan")+" "+faction.optString("name")).toLowerCase(Locale.ROOT);if((!region.isEmpty()&&t.contains(region.toLowerCase(Locale.ROOT)))||t.contains(name.toLowerCase(Locale.ROOT)))factionPressure++;}
        p.put("prosperity_delta",Integer.compare(positive+tradeSignals,negative+factionPressure));
        p.put("security_delta",negative+factionPressure>positive?-1:(positive>negative+factionPressure?1:0));
        p.put("scarcity_delta",negative>positive?1:(positive>negative?-1:0));
        p.put("unrest_delta",negative+factionPressure>0?1:(positive>1?-1:0));
        p.put("crime_delta",negative>1?1:(positive>2?-1:0));
        p.put("political_delta",factionPressure>0?1:(positive>negative?-1:0));
        p.put("trade_delta",tradeSignals+positive>negative?1:(negative>0?-1:0));
        p.put("faith_delta",faithSignals>0?1:0);
    }catch(Exception ignored){AppLog.ignored("LivingSettlementEngine","Falha tolerada na camada 6.0.",ignored);}return p;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int recordThresholdCrossings(JSONObject before,int prosperity,int security,int scarcity,int unrest,int crime,int politics,int trade,int faith,int day){int added=0;String key=before.optString("settlement_key"),name=before.optString("name","Assentamento");added+=threshold(before.optInt("scarcity",20),scarcity,65,true,key,name,"escassez","A escassez passou a dominar o cotidiano de "+name+".",day);added+=threshold(before.optInt("unrest",10),unrest,65,true,key,name,"agitação","A agitação social atingiu um ponto crítico em "+name+".",day);added+=threshold(before.optInt("criminality",15),crime,65,true,key,name,"criminalidade","A criminalidade tornou-se uma força pública relevante em "+name+".",day);added+=threshold(before.optInt("political_pressure",15),politics,65,true,key,name,"pressão política","A disputa política em "+name+" entrou em uma fase de forte pressão.",day);added+=threshold(before.optInt("security",50),security,35,false,key,name,"insegurança","A segurança de "+name+" caiu a um nível preocupante.",day);added+=threshold(before.optInt("prosperity",50),prosperity,35,false,key,name,"declínio","A prosperidade de "+name+" entrou em declínio perceptível.",day);added+=threshold(before.optInt("trade",50),trade,70,true,key,name,"comércio","O comércio de "+name+" entrou em uma fase de expansão marcante.",day);added+=threshold(before.optInt("faith_influence",30),faith,70,true,key,name,"fé","A influência religiosa tornou-se uma das forças centrais de "+name+".",day);return added;}
    private int threshold(int oldValue,int newValue,int threshold,boolean rising,String settlementKey,String settlementName,String tag,String statement,int day){boolean crossed=rising?(oldValue<threshold&&newValue>=threshold):(oldValue>threshold&&newValue<=threshold);if(!crossed)return 0;String eventKey="settlement:"+settlementKey+":"+tag.replace(' ','_')+":"+day;try{db.addJournalFact("Mudança em "+settlementName,statement,"Mundo Vivo",eventKey,"settlement_simulation");return 1;}catch(Exception ignored){return 0;}}

    public List<JSONObject> list(int limit){return db.queryList("SELECT * FROM settlement_states WHERE world_profile=? ORDER BY unrest DESC,scarcity DESC,name LIMIT "+Math.max(1,Math.min(1000,limit)),new String[]{db.getActiveMapProfile()});}
    public JSONArray listJson(int limit){return new JSONArray(list(limit));}
    public JSONObject getByLocation(String location){if(location==null||location.trim().isEmpty())return new JSONObject();return db.queryOne("SELECT * FROM settlement_states WHERE world_profile=? AND (name=? COLLATE NOCASE OR settlement_key=?) LIMIT 1",new String[]{db.getActiveMapProfile(),location.trim(),normalize(location)});}
    public JSONObject context(){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{o.put("current",getByLocation(db.getSceneState().optString("location"))).put("pressured",listJson(20)).put("rule_boundary","Prosperidade, segurança, escassez e agitação são estados narrativos do assentamento; não concedem bônus/penalidades de ficha sem regra.");}catch(Exception ignored){AppLog.ignored("LivingSettlementEngine","Falha tolerada na camada 6.0.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean isSettlement(String t){return t.matches(".*(city|cidade|town|vila|village|aldeia|porto|port|fort|fortaleza|castle|castelo|capital|settlement|assentamento|temple|templo|keep).*" );}
    private static String normalize(String s){return (s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}0-9]+","-").replaceAll("^-|-$",""));}
    private static int clamp(int x){return Math.max(0,Math.min(100,x));}
    private static int step(int delta,int days){if(delta==0)return 0;return delta*Math.max(1,Math.min(8,(days+6)/7));}
}
