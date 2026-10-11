package com.braseiro.dnd5e.adventure;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Untrusted Gemini JSON -> explicit, validated proposal. Never lets model content
 * mutate resources, inventories, timestamps, positions or turn counters.
 * Builds on the Loreweaver semantic check contract and the AetherTable intention
 * versus execution separation, while retaining the Braseiro SQLite journal.
 */
public final class UniversalAiProposalPort {
    private UniversalAiProposalPort() {}

    public static JSONObject parse(String raw, String actorId, String systemId) {
        if (raw == null || raw.length() > 16000 || raw.length() < 2)
            return blocked("Resposta da IA ausente ou muito grande.");
        if (!UniversalRpgEngine.supported(systemId) || actorId == null || !actorId.matches("[A-Za-z0-9_.-]{1,80}"))
            return blocked("Personagem ou sistema inválido.");
        try {
            String clean = raw.trim();
            if (clean.startsWith("```")) {
                int endLine=clean.indexOf('\n');
                if(endLine<0 || !clean.endsWith("```")) return blocked("JSON cercado por markdown incompleto.");
                clean=clean.substring(endLine+1,clean.length()-3).trim();
            }
            JSONObject data=new JSONObject(clean);
            String narration=data.optString("narration", "").trim();
            if(narration.length()<10 || narration.length()>7000)
                return blocked("Narração precisa ter entre 10 e 7000 caracteres.");
            JSONObject check=data.optJSONObject("check");
            if(check==null || !check.optBoolean("required",false))
                return new JSONObject().put("status","accepted").put("narration",narration).put("requires_check",false);
            String situation=check.optString("situation","").trim();
            String yes=check.optString("on_success","").trim();
            String no=check.optString("on_failure","").trim();
            if(situation.length()<5 || situation.length()>1000 || yes.length()<3 || yes.length()>1200
                || no.length()<3 || no.length()>1200)
                return blocked("Teste da IA sem situação e consequências delimitadas.");
            JSONObject spec=check.optJSONObject("spec");
            if(spec==null)return blocked("Teste da IA sem especificação da rolagem.");
            if(UniversalRpgEngine.DND.equals(systemId)) {
                int dc=spec.optInt("dc",-1);
                int modifier=spec.optInt("modifier",0);
                if(!spec.has("dc") || dc<0 || dc>99 || modifier < -40 || modifier > 40)
                    return blocked("CD ou modificador d20 inválidos.");
            } else {
                JSONObject pool=spec.optJSONObject("dice_pool");
                if(pool==null || pool.length()==0) return blocked("Pool de dados não informado.");
                int total=0;
                org.json.JSONArray keys=pool.names();
                for(int i=0;keys!=null&&i<keys.length();i++) {
                    String key=keys.optString(i);
                    Object value=pool.opt(key);
                    if(!(value instanceof Number)||((Number)value).doubleValue()!=((Number)value).intValue())
                        return blocked("Quantidade de dados não inteira.");
                    int count=((Number)value).intValue();
                    if(count<0 || count>20)return blocked("Quantidade de dados fora do limite.");
                    boolean allowed=UniversalRpgEngine.YZE.equals(systemId)
                        ? ("attribute".equals(key)||"skill".equals(key)||"gear".equals(key)||"negative".equals(key))
                        : ("ability".equals(key)||"proficiency".equals(key)||"boost".equals(key)||"difficulty".equals(key)||"challenge".equals(key)||"setback".equals(key)||"force".equals(key));
                    if(!allowed)return blocked("Dado não pertence ao sistema: "+key);
                    total+=count;
                }
                if(total<1 || total>40)return blocked("Pool de dados vazio ou excessivo.");
            }
            if ((check.has("on_success_effects") && check.optJSONArray("on_success_effects")==null)
                || (check.has("on_failure_effects") && check.optJSONArray("on_failure_effects")==null))
                return blocked("Propostas de efeitos precisam ser arrays JSON.");
            JSONArray successEffects, failureEffects;
            try {
                successEffects=UniversalEffectProposalPort.sanitize(check.optJSONArray("on_success_effects"));
                failureEffects=UniversalEffectProposalPort.sanitize(check.optJSONArray("on_failure_effects"));
            } catch (IllegalArgumentException ex) { return blocked(ex.getMessage()); }
            // The LLM may only SUGGEST these effects: player approval is always required.
            JSONObject command=new JSONObject().put("action","begin_check")
                .put("character_id",actorId).put("system_id",systemId)
                .put("situation",situation).put("on_success",yes).put("on_failure",no)
                .put("spec",new JSONObject(spec.toString()))
                .put("on_success_effects",successEffects).put("on_failure_effects",failureEffects);
            return new JSONObject().put("status","accepted").put("narration",narration)
                .put("requires_check",true).put("check_command",command);
        } catch(Exception ignored) {
            return blocked("Gemini não devolveu JSON válido; nenhuma alteração de campanha foi aceita.");
        }
    }
    private static JSONObject blocked(String error) {
        try {
            return new JSONObject().put("status", "blocked").put("error", error);
        } catch (RuntimeException invalid) {
            throw invalid;
        } catch (Exception invalidJson) {
            throw new IllegalStateException("Falha ao estruturar recusa da IA", invalidJson);
        }
    }
}
