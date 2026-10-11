package com.braseiro.dnd5e.adventure;

import org.json.JSONObject;

/**
 * Interface canônica de execução e resolução do Adventure Engine.
 */
public interface IAdventureEngine {
    JSONObject startAdventure(AdventureModule module, long partyId);
    JSONObject validateActionContext(String actionType, JSONObject payload);
    JSONObject commitResolution(JSONObject resolutionResult);
    JSONObject getNarrationContext();
    boolean canTransition(String targetNodeId);
    boolean commitTransaction();
    void rollbackTransaction();
}
