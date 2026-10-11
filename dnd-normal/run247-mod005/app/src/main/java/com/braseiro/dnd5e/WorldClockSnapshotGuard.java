package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

/** Deterministic snapshot for world-clock preview authorization.
 * Ignores timestamps and SQL order; compares only rule-driving fields. */
public final class WorldClockSnapshotGuard {
    private WorldClockSnapshotGuard() {}
    public static String digest(List<JSONObject> clocks) {
        List<String> entries=new ArrayList<String>();
        if(clocks!=null)for(JSONObject clock:clocks){
            if(clock==null)continue;
            StringBuilder b=new StringBuilder();
            String[] fields={"id","active","automatic","paused","current_value","max_value",
                "accumulated_hours","interval_hours","rate_multiplier","tick_delta","regression_allowed",
                "condition_json","conditions_json","condition","owner_entity","name","consequence"};
            for(String field:fields)b.append(field).append('=').append(
                CanonicalJson.stringify(clock.opt(field))).append('|');
            entries.add(b.toString());
        }
        Collections.sort(entries);
        return IntegrityHash.sha256(entries.toString());
    }
}
