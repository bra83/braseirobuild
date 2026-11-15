package com.braseiro.dnd5e;

import org.json.JSONObject;

/**
 * Canonical side-effects of a map trip already paid for and resolved by TravelRulesEngine.
 * This gateway NEVER moves the marker, charges resources, rolls dice or creates weather.
 * The confirmed time and environmental response are committed atomically to SQLite.
 * MainActivity owns the outer travel snapshot for resources + atlas rollback.
 */
public final class ConfirmedMapTravelWorldGateway {
    private final AppDb db;
    private final WorldStateEngine clock;
    private final EnvironmentalDirector environment;

    public ConfirmedMapTravelWorldGateway(AppDb database) {
        db=database;
        clock=new WorldStateEngine(database);
        environment=new EnvironmentalDirector(database);
    }

    public JSONObject commit(double elapsedHours, String destination) {
        JSONObject out=new JSONObject();
        try {
            if(!Double.isFinite(elapsedHours)||elapsedHours<=0d)
                return out.put("committed",false).put("error","invalid_elapsed_map_hours");
            JSONObject tick=clock.previewConfirmedMapElapsed(elapsedHours);
            if(tick.optString("error","").length()>0)
                return out.put("committed",false).put("error",tick.optString("error"));
            final JSONObject[] plan={new JSONObject()};
            db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                @Override public void run() throws Exception {
                    if(!clock.commit(tick))
                        throw new IllegalStateException("world_clock_rejected: "+tick.optString("commit_error","unknown"));
                    // The environment must observe the NEW canonical clock and weather.
                    plan[0]=environment.preview("Viajo pelo mapa até "+(destination==null?"o destino":destination));
                    if(plan[0].optString("error","").length()>0)
                        throw new IllegalStateException("environment_rejected: "+plan[0].optString("error"));
                    environment.commitChecked(plan[0],"");
                }
            });
            JSONObject scene=db.getSceneState();
            out.put("committed",true)
               .put("day_number",scene.optInt("day_number",1))
               .put("clock_hour",scene.optDouble("clock_hour",9d))
               .put("weather",scene.optString("weather",""))
               .put("hours_advanced",tick.optDouble("hours_advanced",0d))
               .put("external_travel_resources_handled",true)
               .put("world",tick)
               .put("weather_system",plan[0].optJSONObject("weather_system"));
        } catch(Exception e) {
            try{out.put("committed",false).put("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
            catch(Exception ignored){}
        }
        return out;
    }
}