package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sanitizes suggested effects from AI into a bounded, player-reviewable envelope.
 * No model output ever calls apply_effects. Only the UI's explicit confirmation does.
 * The persisted receipt preserves proposals across restarts and invalidates altered lists.
 */
public final class UniversalEffectProposalPort {
    private UniversalEffectProposalPort() {}
    private static boolean key(String s) { return s != null && s.matches("[A-Za-z0-9_.-]{1,80}"); }
    private static Integer integer(Object v) {
        if (!(v instanceof Number)) return null;
        double d = ((Number)v).doubleValue();
        if (!Double.isFinite(d) || d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) return null;
        return Integer.valueOf((int)d);
    }
    public static JSONArray sanitize(JSONArray list) {
        JSONArray output = new JSONArray();
        if (list == null) return output;
        if (list.length() > 6) throw new IllegalArgumentException("Máximo de seis efeitos por resultado.");
        for (int i=0; i<list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) throw new IllegalArgumentException("Efeito não é um objeto.");
            String kind = item.optString("kind", "");
            JSONObject normalized = new JSONObject().put("kind",kind);
            if ("resource".equals(kind)) {
                String k = item.optString("key", ""); Integer delta = integer(item.opt("delta"));
                if (!key(k) || delta == null || delta == 0 || Math.abs((long)delta) > 1000000L)
                    throw new IllegalArgumentException("Efeito de recurso inválido.");
                normalized.put("key",k).put("delta",delta);
            } else if ("item".equals(kind)) {
                String id=item.optString("item_id", ""), name=item.optString("name", "").trim();
                Integer delta=integer(item.opt("delta"));
                if (!key(id) || delta==null || delta==0 || Math.abs((long)delta)>9999L || name.length()>120)
                    throw new IllegalArgumentException("Efeito de item inválido.");
                normalized.put("item_id",id).put("name",name).put("delta",delta);
            } else if ("time".equals(kind)) {
                Integer minutes=integer(item.opt("minutes"));
                if (minutes==null || minutes<1 || minutes>1440)
                    throw new IllegalArgumentException("Avanço de tempo inválido.");
                normalized.put("minutes",minutes);
            } else if ("world_fact".equals(kind)) {
                String id=item.optString("fact_id", ""), value=item.optString("text", "").trim();
                if (!key(id) || value.length()<3 || value.length()>500)
                    throw new IllegalArgumentException("Fato do mundo inválido.");
                normalized.put("fact_id",id).put("text",value);
            } else throw new IllegalArgumentException("Tipo de efeito não permitido: "+kind);
            output.put(normalized);
        }
        return output;
    }
    /** Same normalized semantic payload, independent of JSONObject key iteration order. */
    public static boolean same(JSONArray expected, JSONArray supplied) {
        try {
            JSONArray a=sanitize(expected), b=sanitize(supplied);
            if(a.length()!=b.length()) return false;
            for(int i=0;i<a.length();i++) {
                JSONObject left=a.getJSONObject(i), right=b.getJSONObject(i);
                if(left.length()!=right.length()) return false;
                JSONArray keys=left.names();
                for(int k=0;keys!=null&&k<keys.length();k++){
                    String field=keys.optString(k);
                    if(!left.opt(field).equals(right.opt(field)))return false;
                }
            }
            return true;
        } catch(Exception ignored){return false;}
    }
}
