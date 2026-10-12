package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Separa latência de RAG da ficção. Uma consulta lenta nunca congela narração;
 * apenas mutações mecânicas que dependem de regra ficam pendentes até haver evidência.
 */
public final class Pf2RagContinuityGate {
    public static final long DEFAULT_SOFT_BUDGET_MS=850L;
    private Pf2RagContinuityGate(){}

    public static JSONObject plan(boolean mechanical, long elapsedMs, JSONArray evidence){
        JSONObject out=new JSONObject();
        try{
            boolean grounded=evidence!=null&&evidence.length()>0;
            boolean budgetExceeded=elapsedMs>DEFAULT_SOFT_BUDGET_MS;
            out.put("fiction_may_continue",true);
            out.put("mechanics_may_commit",!mechanical||grounded);
            out.put("rag_deferred",budgetExceeded&&!grounded);
            out.put("requires_followup_retrieval",mechanical&&!grounded);
            out.put("policy", mechanical&&!grounded
                    ? "Continue a cena sem inventar números; retenha somente a mutação mecânica até a regra ser verificada."
                    : "A ficção e o estado autorizado podem prosseguir.");
        }catch(Exception ignored){}
        return out;
    }
}
