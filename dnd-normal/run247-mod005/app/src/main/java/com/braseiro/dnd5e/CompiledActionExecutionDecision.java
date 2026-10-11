package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Typed boundary between compiled execution and the later gameplay transaction. */
public final class CompiledActionExecutionDecision {
    public enum Stage { COMPILED_EXECUTION_READY, GAMEPLAY_RESOLUTION_REQUIRED }

    public final Stage stage;
    public final String authority;
    public final boolean effectApplied;
    public final boolean commitRequired;

    private CompiledActionExecutionDecision(Stage stage, String authority) {
        this.stage = stage;
        this.authority = authority;
        this.effectApplied = false;
        this.commitRequired = true;
    }

    /** Creates a decision only for a successful compiled execution result. */
    public static CompiledActionExecutionDecision from(JSONObject result) {
        if (result == null || !result.optBoolean("executed", false)) return null;
        Stage stage = result.optBoolean("requires_gameplay_resolution", false)
                ? Stage.GAMEPLAY_RESOLUTION_REQUIRED : Stage.COMPILED_EXECUTION_READY;
        String authority = result.optString("authority", "compiled_runtime").trim();
        if (authority.isEmpty()) authority = "compiled_runtime";
        return new CompiledActionExecutionDecision(stage, authority);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("execution_stage", stage.name().toLowerCase(java.util.Locale.ROOT))
                    .put("effect_applied", effectApplied)
                    .put("commit_required", commitRequired)
                    .put("authority", authority);
        } catch (Exception ignored) { }
        return json;
    }
}
