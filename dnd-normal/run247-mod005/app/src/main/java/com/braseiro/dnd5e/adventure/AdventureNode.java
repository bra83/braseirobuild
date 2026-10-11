package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nó imutável de cena do grafo da aventura.
 */
public final class AdventureNode {
    private final String id;
    private final String title;
    private final String description;
    private final String gmDirectives;
    private final String locationId;
    private final List<String> presentNpcIds;
    private final List<AdventureTransition> transitions;
    private final JSONObject checkRequirement;
    private final JSONObject rewards;

    public AdventureNode(String id, String title, String description, String gmDirectives, String locationId,
                         List<String> presentNpcIds, List<AdventureTransition> transitions,
                         JSONObject checkRequirement, JSONObject rewards) {
        this.id = id != null ? id : "unknown_node";
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.gmDirectives = gmDirectives != null ? gmDirectives : "";
        this.locationId = locationId != null ? locationId : "default_location";
        this.presentNpcIds = presentNpcIds != null ? Collections.unmodifiableList(new ArrayList<String>(presentNpcIds)) : Collections.<String>emptyList();
        this.transitions = transitions != null ? Collections.unmodifiableList(new ArrayList<AdventureTransition>(transitions)) : Collections.<AdventureTransition>emptyList();
        this.checkRequirement = checkRequirement != null ? checkRequirement : new JSONObject();
        this.rewards = rewards != null ? rewards : new JSONObject();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getGmDirectives() { return gmDirectives; }
    public String getLocationId() { return locationId; }
    public List<String> getPresentNpcIds() { return presentNpcIds; }
    public List<AdventureTransition> getTransitions() { return transitions; }
    public JSONObject getCheckRequirement() { return checkRequirement; }
    public JSONObject getRewards() { return rewards; }

    public static AdventureNode fromJson(JSONObject json) {
        if (json == null) return new AdventureNode("empty", "", "", "", "", null, null, null, null);
        String id = json.optString("id", "node_default");
        String title = json.optString("title", "");
        String desc = json.optString("description", "");
        String directives = json.optString("gm_directives", "");
        String loc = json.optString("location_id", "");
        
        List<String> npcs = new ArrayList<String>();
        JSONArray npcsArr = json.optJSONArray("present_npc_ids");
        if (npcsArr != null) {
            for (int i = 0; i < npcsArr.length(); i++) npcs.add(npcsArr.optString(i));
        }

        List<AdventureTransition> trans = new ArrayList<AdventureTransition>();
        JSONArray transArr = json.optJSONArray("transitions");
        if (transArr != null) {
            for (int i = 0; i < transArr.length(); i++) {
                JSONObject tObj = transArr.optJSONObject(i);
                if (tObj != null) trans.add(AdventureTransition.fromJson(tObj));
            }
        }

        return new AdventureNode(id, title, desc, directives, loc, npcs, trans, json.optJSONObject("check_requirement"), json.optJSONObject("rewards"));
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("description", description);
            obj.put("gm_directives", gmDirectives);
            obj.put("location_id", locationId);
            obj.put("present_npc_ids", new JSONArray(presentNpcIds));
            JSONArray tArr = new JSONArray();
            for (AdventureTransition t : transitions) tArr.put(t.toJson());
            obj.put("transitions", tArr);
            obj.put("check_requirement", checkRequirement);
            obj.put("rewards", rewards);
        } catch (Exception ignored) {}
        return obj;
    }
}
