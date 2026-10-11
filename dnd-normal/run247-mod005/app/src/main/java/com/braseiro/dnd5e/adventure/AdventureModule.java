package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulo de aventura declarativo e imutável.
 * Não depende de PDF ou RAG em tempo de execução.
 */
public final class AdventureModule {
    private final String id;
    private final String title;
    private final String version;
    private final String rootNodeId;
    private final Map<String, AdventureNode> nodes;
    private final JSONObject metadata;

    public AdventureModule(String id, String title, String version, String rootNodeId, Map<String, AdventureNode> nodes, JSONObject metadata) {
        this.id = id != null ? id : "unknown_module";
        this.title = title != null ? title : "Aventura sem título";
        this.version = version != null ? version : "1.0.0";
        this.rootNodeId = rootNodeId;
        this.nodes = nodes != null ? Collections.unmodifiableMap(new HashMap<String, AdventureNode>(nodes)) : Collections.<String, AdventureNode>emptyMap();
        this.metadata = metadata != null ? metadata : new JSONObject();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getVersion() { return version; }
    public String getRootNodeId() { return rootNodeId; }
    public AdventureNode getNode(String nodeId) { return nodes.get(nodeId); }
    public Map<String, AdventureNode> getNodes() { return nodes; }
    public JSONObject getMetadata() { return metadata; }

    public static AdventureModule fromJson(JSONObject json) {
        if (json == null) return new AdventureModule("empty", "Vazio", "1.0", "root", null, null);
        String id = json.optString("id", "mod_default");
        String title = json.optString("title", "Aventura");
        String version = json.optString("version", "1.0");
        String rootNodeId = json.optString("root_node_id", "scene_start");
        
        Map<String, AdventureNode> nodeMap = new HashMap<String, AdventureNode>();
        JSONArray nodesArray = json.optJSONArray("nodes");
        if (nodesArray != null) {
            for (int i = 0; i < nodesArray.length(); i++) {
                JSONObject nObj = nodesArray.optJSONObject(i);
                if (nObj != null) {
                    AdventureNode node = AdventureNode.fromJson(nObj);
                    nodeMap.put(node.getId(), node);
                }
            }
        }
        return new AdventureModule(id, title, version, rootNodeId, nodeMap, json.optJSONObject("metadata"));
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("version", version);
            obj.put("root_node_id", rootNodeId);
            JSONArray arr = new JSONArray();
            for (AdventureNode n : nodes.values()) {
                arr.put(n.toJson());
            }
            obj.put("nodes", arr);
            obj.put("metadata", metadata);
        } catch (Exception ignored) {}
        return obj;
    }

    public static AdventureModule createSampleModule(String id, String title) {
        Map<String, AdventureNode> nodes = new HashMap<String, AdventureNode>();
        AdventureNode root = new AdventureNode("start", "Inicio da Aventura", "Voce se encontra na encruzilhada de Faerun.", "descreva os arredores", "loc_crossroads", null, null, null, null);
        nodes.put("start", root);
        return new AdventureModule(id, title, "1.0.0", "start", nodes, new JSONObject());
    }
}
