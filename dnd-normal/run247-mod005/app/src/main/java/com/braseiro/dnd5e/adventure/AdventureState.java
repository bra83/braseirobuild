package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/**
 * Estado mutável e persistente da aventura com suporte a contrato completo,
 * commit transacional em duas fases e rollback seguro.
 */
public final class AdventureState {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int CURRENT_MIGRATION_VERSION = 1;

    private int schemaVersion;
    private String status; // "in_progress", "completed", "failed"
    private String currentNodeId;
    private final Set<String> visitedNodes;
    private final Set<String> completedObjectives;
    private final Set<String> failedObjectives;
    private final Set<String> flags;
    private final Set<String> discoveredFacts;
    private final Set<String> discoveredClues;
    private final JSONObject npcStates;
    private final Set<String> triggeredEncounters;
    private final Set<String> consumedRewards;
    private final JSONArray consequences;
    private final JSONArray branchHistory;
    private JSONObject pendingManualAdjudication;
    private JSONArray manualAdjudicationHistory;
    private JSONObject universalRpgState;
    private JSONObject worldTimeReference;
    private int migrationVersion;

    private JSONObject stagedChanges;
    private boolean inTransaction;

    public AdventureState(String startNodeId) {
        this.schemaVersion = CURRENT_SCHEMA_VERSION;
        this.status = "in_progress";
        this.currentNodeId = startNodeId != null ? startNodeId : "start";
        this.visitedNodes = new HashSet<String>();
        this.completedObjectives = new HashSet<String>();
        this.failedObjectives = new HashSet<String>();
        this.flags = new HashSet<String>();
        this.discoveredFacts = new HashSet<String>();
        this.discoveredClues = new HashSet<String>();
        this.npcStates = new JSONObject();
        this.triggeredEncounters = new HashSet<String>();
        this.consumedRewards = new HashSet<String>();
        this.consequences = new JSONArray();
        this.branchHistory = new JSONArray();
        this.pendingManualAdjudication = new JSONObject();
        this.manualAdjudicationHistory = new JSONArray();
        this.universalRpgState = new JSONObject();
        this.worldTimeReference = new JSONObject();
        this.migrationVersion = CURRENT_MIGRATION_VERSION;

        this.stagedChanges = new JSONObject();
        this.inTransaction = false;
        this.visitedNodes.add(this.currentNodeId);
    }

    public synchronized int getSchemaVersion() { return schemaVersion; }
    public synchronized void setSchemaVersion(int v) { this.schemaVersion = v; }

    public synchronized String getStatus() { return status; }
    public synchronized void setStatus(String s) { this.status = s != null ? s : "in_progress"; }

    public synchronized String getCurrentNodeId() { return currentNodeId; }
    public synchronized void setCurrentNodeId(String id) {
        if (id != null && id.length() > 0) {
            this.currentNodeId = id;
            this.visitedNodes.add(id);
        }
    }

    public synchronized Set<String> getVisitedNodes() { return visitedNodes; }
    public synchronized Set<String> getCompletedObjectives() { return completedObjectives; }
    public synchronized Set<String> getFailedObjectives() { return failedObjectives; }
    public synchronized Set<String> getFlags() { return flags; }
    public synchronized Set<String> getDiscoveredFacts() { return discoveredFacts; }
    public synchronized Set<String> getDiscoveredClues() { return discoveredClues; }
    public synchronized JSONObject getNpcStates() { return npcStates; }
    public synchronized Set<String> getTriggeredEncounters() { return triggeredEncounters; }
    public synchronized Set<String> getConsumedRewards() { return consumedRewards; }
    public synchronized JSONArray getConsequences() { return consequences; }
    public synchronized JSONArray getBranchHistory() { return branchHistory; }
    public synchronized JSONObject getPendingManualAdjudication() { return copyJson(pendingManualAdjudication); }
    public synchronized JSONArray getManualAdjudicationHistory() { return copyArray(manualAdjudicationHistory); }
    public synchronized void setPendingManualAdjudication(JSONObject pending) { pendingManualAdjudication = copyJson(pending); }
    public synchronized void appendManualAdjudication(JSONObject resolution) {
        if (resolution != null) manualAdjudicationHistory.put(copyJson(resolution));
    }
    private static JSONObject copyJson(JSONObject value) {
        try { return value == null ? new JSONObject() : new JSONObject(value.toString()); }
        catch (Exception e) { return new JSONObject(); }
    }
    private static JSONArray copyArray(JSONArray value) {
        try { return value == null ? new JSONArray() : new JSONArray(value.toString()); }
        catch (Exception e) { return new JSONArray(); }
    }
    public synchronized JSONObject getUniversalRpgState() { return copyJson(universalRpgState); }
    public synchronized void setUniversalRpgState(JSONObject value) { universalRpgState = copyJson(value); }
    public synchronized JSONObject getWorldTimeReference() { return worldTimeReference; }
    public synchronized int getMigrationVersion() { return migrationVersion; }
    public synchronized void setMigrationVersion(int v) { this.migrationVersion = v; }

    public synchronized void markObjectiveCompleted(String objectiveId) {
        if (objectiveId != null) completedObjectives.add(objectiveId);
    }

    public synchronized void markObjectiveFailed(String objectiveId) {
        if (objectiveId != null) failedObjectives.add(objectiveId);
    }

    public synchronized boolean isObjectiveCompleted(String objectiveId) {
        return objectiveId != null && completedObjectives.contains(objectiveId);
    }

    public synchronized boolean isObjectiveFailed(String objectiveId) {
        return objectiveId != null && failedObjectives.contains(objectiveId);
    }

    public synchronized void addFlag(String flag) {
        if (flag != null) flags.add(flag);
    }

    public synchronized boolean hasFlag(String flag) {
        return flag != null && flags.contains(flag);
    }

    public synchronized void addDiscoveredFact(String fact) {
        if (fact != null) discoveredFacts.add(fact);
    }

    public synchronized void addDiscoveredClue(String clue) {
        if (clue != null) discoveredClues.add(clue);
    }

    public synchronized void setNpcState(String npcId, String state) {
        if (npcId != null) {
            try { npcStates.put(npcId, state != null ? state : "neutral"); } catch (Exception ignored) {}
        }
    }

    public synchronized String getNpcState(String npcId) {
        return npcId != null ? npcStates.optString(npcId, "unknown") : "unknown";
    }

    public synchronized void triggerEncounter(String encounterId) {
        if (encounterId != null) triggeredEncounters.add(encounterId);
    }

    public synchronized boolean isEncounterTriggered(String encounterId) {
        return encounterId != null && triggeredEncounters.contains(encounterId);
    }

    public synchronized void consumeReward(String rewardId) {
        if (rewardId != null) consumedRewards.add(rewardId);
    }

    public synchronized boolean isRewardConsumed(String rewardId) {
        return rewardId != null && consumedRewards.contains(rewardId);
    }

    public synchronized void addConsequence(JSONObject consequence) {
        if (consequence != null) consequences.put(consequence);
    }

    public synchronized void recordBranch(String fromNode, String toNode, String transitionId) {
        try {
            JSONObject b = new JSONObject();
            b.put("from_node", fromNode != null ? fromNode : "");
            b.put("to_node", toNode != null ? toNode : "");
            b.put("transition_id", transitionId != null ? transitionId : "");
            b.put("timestamp", System.currentTimeMillis());
            branchHistory.put(b);
        } catch (Exception ignored) {}
    }

    public synchronized void setWorldTimeReference(int day, double hour, String timeOfDay) {
        try {
            worldTimeReference.put("day_number", day);
            worldTimeReference.put("clock_hour", hour);
            worldTimeReference.put("time_of_day", timeOfDay != null ? timeOfDay : "");
        } catch (Exception ignored) {}
    }

    public synchronized void stageTransition(String nextNodeId, JSONObject payload) {
        this.inTransaction = true;
        this.stagedChanges = new JSONObject();
        try {
            this.stagedChanges.put("target_node_id", nextNodeId);
            this.stagedChanges.put("payload", payload != null ? payload : new JSONObject());
            if (payload != null) this.stagedChanges.put("transition_id", payload.optString("transition_id", ""));
        } catch (Exception ignored) {}
    }

    public synchronized boolean commitTransaction() {
        if (!inTransaction || stagedChanges == null) return false;
        String prevNode = this.currentNodeId;
        String nextNodeId = stagedChanges.optString("target_node_id");
        if (nextNodeId != null && nextNodeId.length() > 0) {
            this.currentNodeId = nextNodeId;
            this.visitedNodes.add(nextNodeId);
            recordBranch(prevNode, nextNodeId, stagedChanges.optString("transition_id", ""));
        }
        JSONObject payload = stagedChanges.optJSONObject("payload");
        if (payload != null) {
            JSONArray addFlags = payload.optJSONArray("add_flags");
            if (addFlags != null) {
                for (int i = 0; i < addFlags.length(); i++) flags.add(addFlags.optString(i));
            }
            JSONArray addClues = payload.optJSONArray("add_clues");
            if (addClues != null) {
                for (int i = 0; i < addClues.length(); i++) discoveredClues.add(addClues.optString(i));
            }
            JSONArray addFacts = payload.optJSONArray("add_facts");
            if (addFacts != null) {
                for (int i = 0; i < addFacts.length(); i++) discoveredFacts.add(addFacts.optString(i));
            }
            JSONArray compObj = payload.optJSONArray("complete_objectives");
            if (compObj != null) {
                for (int i = 0; i < compObj.length(); i++) completedObjectives.add(compObj.optString(i));
            }
            JSONArray failObj = payload.optJSONArray("fail_objectives");
            if (failObj != null) {
                for (int i = 0; i < failObj.length(); i++) failedObjectives.add(failObj.optString(i));
            }
            JSONObject npcMap = payload.optJSONObject("npc_states");
            if (npcMap != null) {
                JSONArray keys = npcMap.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String k = keys.optString(i);
                        setNpcState(k, npcMap.optString(k));
                    }
                }
            }
            String encId = payload.optString("trigger_encounter", "");
            if (encId.length() > 0) triggeredEncounters.add(encId);
            String rewId = payload.optString("consume_reward", "");
            if (rewId.length() > 0) consumedRewards.add(rewId);
            JSONObject cons = payload.optJSONObject("consequence");
            if (cons != null) consequences.put(cons);
        }
        this.stagedChanges = new JSONObject();
        this.inTransaction = false;
        return true;
    }

    public synchronized void rollbackTransaction() {
        this.stagedChanges = new JSONObject();
        this.inTransaction = false;
    }

    public synchronized JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("schema_version", schemaVersion);
            obj.put("status", status);
            obj.put("current_node_id", currentNodeId);

            JSONArray vArr = new JSONArray();
            for (String s : visitedNodes) vArr.put(s);
            obj.put("visited_nodes", vArr);

            JSONArray compArr = new JSONArray();
            for (String s : completedObjectives) compArr.put(s);
            obj.put("completed_objectives", compArr);

            JSONArray failArr = new JSONArray();
            for (String s : failedObjectives) failArr.put(s);
            obj.put("failed_objectives", failArr);

            JSONArray fArr = new JSONArray();
            for (String s : flags) fArr.put(s);
            obj.put("flags", fArr);

            JSONArray factsArr = new JSONArray();
            for (String s : discoveredFacts) factsArr.put(s);
            obj.put("discovered_facts", factsArr);

            JSONArray cluesArr = new JSONArray();
            for (String s : discoveredClues) cluesArr.put(s);
            obj.put("discovered_clues", cluesArr);

            obj.put("npc_states", npcStates != null ? npcStates : new JSONObject());

            JSONArray encArr = new JSONArray();
            for (String s : triggeredEncounters) encArr.put(s);
            obj.put("triggered_encounters", encArr);

            JSONArray rewArr = new JSONArray();
            for (String s : consumedRewards) rewArr.put(s);
            obj.put("consumed_rewards", rewArr);

            obj.put("consequences", consequences != null ? consequences : new JSONArray());
            obj.put("branch_history", branchHistory != null ? branchHistory : new JSONArray());
            obj.put("pending_manual_adjudication", pendingManualAdjudication);
            obj.put("manual_adjudication_history", manualAdjudicationHistory);
            obj.put("universal_rpg_state", universalRpgState);
            obj.put("world_time_reference", worldTimeReference != null ? worldTimeReference : new JSONObject());
            obj.put("migration_version", migrationVersion);
        } catch (Exception ignored) {}
        return obj;
    }

    public static AdventureState fromJson(JSONObject json) {
        if (json == null) return new AdventureState("start");
        AdventureState state = new AdventureState(json.optString("current_node_id", "start"));
        state.schemaVersion = json.optInt("schema_version", CURRENT_SCHEMA_VERSION);
        state.status = json.optString("status", "in_progress");
        state.migrationVersion = json.optInt("migration_version", CURRENT_MIGRATION_VERSION);

        JSONArray vArr = json.optJSONArray("visited_nodes");
        if (vArr != null) {
            for (int i = 0; i < vArr.length(); i++) state.visitedNodes.add(vArr.optString(i));
        }
        JSONArray compArr = json.optJSONArray("completed_objectives");
        if (compArr != null) {
            for (int i = 0; i < compArr.length(); i++) state.completedObjectives.add(compArr.optString(i));
        }
        JSONArray failArr = json.optJSONArray("failed_objectives");
        if (failArr != null) {
            for (int i = 0; i < failArr.length(); i++) state.failedObjectives.add(failArr.optString(i));
        }
        JSONArray fArr = json.optJSONArray("flags");
        if (fArr != null) {
            for (int i = 0; i < fArr.length(); i++) state.flags.add(fArr.optString(i));
        }
        JSONArray factsArr = json.optJSONArray("discovered_facts");
        if (factsArr != null) {
            for (int i = 0; i < factsArr.length(); i++) state.discoveredFacts.add(factsArr.optString(i));
        }
        JSONArray cArr = json.optJSONArray("discovered_clues");
        if (cArr != null) {
            for (int i = 0; i < cArr.length(); i++) state.discoveredClues.add(cArr.optString(i));
        }
        JSONObject npcs = json.optJSONObject("npc_states");
        if (npcs != null) {
            JSONArray names = npcs.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String k = names.optString(i);
                    state.setNpcState(k, npcs.optString(k));
                }
            }
        }
        JSONArray encArr = json.optJSONArray("triggered_encounters");
        if (encArr != null) {
            for (int i = 0; i < encArr.length(); i++) state.triggeredEncounters.add(encArr.optString(i));
        }
        JSONArray rewArr = json.optJSONArray("consumed_rewards");
        if (rewArr != null) {
            for (int i = 0; i < rewArr.length(); i++) state.consumedRewards.add(rewArr.optString(i));
        }
        JSONArray consArr = json.optJSONArray("consequences");
        if (consArr != null) {
            for (int i = 0; i < consArr.length(); i++) state.consequences.put(consArr.optJSONObject(i));
        }
        JSONArray brArr = json.optJSONArray("branch_history");
        if (brArr != null) {
            for (int i = 0; i < brArr.length(); i++) state.branchHistory.put(brArr.optJSONObject(i));
        }
        state.pendingManualAdjudication = copyJson(json.optJSONObject("pending_manual_adjudication"));
        state.manualAdjudicationHistory = copyArray(json.optJSONArray("manual_adjudication_history"));
        state.universalRpgState = copyJson(json.optJSONObject("universal_rpg_state"));
        JSONObject wt = json.optJSONObject("world_time_reference");
        if (wt != null) {
            try { state.worldTimeReference = new JSONObject(wt.toString()); } catch (Exception ignored) {}
        }
        return state;
    }
}
