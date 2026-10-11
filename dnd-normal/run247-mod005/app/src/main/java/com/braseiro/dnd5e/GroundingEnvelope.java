package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Contrato de grounding com separação de autoridade e fingerprint anti-tamper.
 * Inferência nunca substitui regras, lore ou estado de campanha canônicos.
 */
public final class GroundingEnvelope {
    public static final int VERSION=1;
    private static final String[] DOMAINS={"rules","lore","campaign_state","inference"};
    private GroundingEnvelope(){}

    public static JSONObject build(JSONObject rules,JSONObject lore,JSONObject campaignState,JSONObject inference){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            out.put("version",VERSION)
                    .put("rules",CanonicalJson.copyObject(rules))
                    .put("lore",CanonicalJson.copyObject(lore))
                    .put("campaign_state",CanonicalJson.copyObject(campaignState))
                    .put("inference",CanonicalJson.copyObject(inference))
                    .put("authority_order",new JSONArray().put("campaign_state").put("rules").put("lore").put("inference"))
                    .put("inference_may_override_canonical",false)
                    .put("mechanical_truth_source","rules+campaign_state")
                    .put("world_truth_source","campaign_state+lore");
            attachFingerprints(out);
        }catch(Exception ignored){AppLog.ignored("GroundingEnvelope","Falha ao montar grounding determinístico.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static JSONObject attachFingerprints(JSONObject grounding)throws Exception{/*BR_JSON_WRAP*/try{
        if(grounding==null)throw new IllegalArgumentException("GroundingEnvelope ausente.");
        JSONObject domain=new JSONObject();for(String d:DOMAINS)domain.put(d,CanonicalJson.sha256(domainObject(grounding,d)));
        grounding.put("domain_fingerprints",domain);
        grounding.put("canonical_fingerprint",canonicalFingerprint(grounding));
        grounding.put("grounding_fingerprint",fingerprint(grounding));
        return grounding;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static String canonicalFingerprint(JSONObject grounding){/*BR_JSON_WRAP*/try{
        JSONObject c=new JSONObject();try{c.put("rules",domainObject(grounding,"rules")).put("lore",domainObject(grounding,"lore")).put("campaign_state",domainObject(grounding,"campaign_state"));}catch(Exception failure){AppLog.ignored("GroundingEnvelope","Falha ao calcular fingerprint canônico.",failure);}
        return CanonicalJson.sha256(c);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public static String fingerprint(JSONObject grounding){return CanonicalJson.sha256(fingerprintCore(grounding));}

    private static JSONObject fingerprintCore(JSONObject g){/*BR_JSON_WRAP*/try{
        JSONObject c=new JSONObject();try{
            c.put("version",g==null?0:g.optInt("version",0));for(String d:DOMAINS)c.put(d,domainObject(g,d));
            c.put("authority_order",g==null?new JSONArray():CanonicalJson.copyArray(g.optJSONArray("authority_order")))
                    .put("inference_may_override_canonical",g!=null&&g.optBoolean("inference_may_override_canonical",true))
                    .put("mechanical_truth_source",g==null?"":g.optString("mechanical_truth_source"))
                    .put("world_truth_source",g==null?"":g.optString("world_truth_source"));
        }catch(Exception failure){AppLog.ignored("GroundingEnvelope","Falha ao montar núcleo do fingerprint.",failure);}
        return c;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject domainObject(JSONObject g,String key){JSONObject o=g==null?null:g.optJSONObject(key);return o==null?new JSONObject():CanonicalJson.copyObject(o);}

    public static boolean hasRuleEvidence(JSONObject grounding){
        JSONObject r=grounding==null?null:grounding.optJSONObject("rules");if(r==null||r.length()==0)return false;
        String status=r.optString("status",r.optString("retrieval_status","")).toLowerCase(java.util.Locale.US);
        if(status.contains("no_relevant")||status.contains("timeout")||status.contains("incomplete")||status.contains("failed"))return false;
        JSONArray hits=r.optJSONArray("hits"),sources=r.optJSONArray("sources");
        return r.optBoolean("verified",false)||r.optBoolean("rules_verified",false)||(hits!=null&&hits.length()>0)||(sources!=null&&sources.length()>0)||r.optInt("evidence_count",0)>0;
    }

    public static String validate(JSONObject grounding){
        if(grounding==null)return "GroundingEnvelope ausente.";
        if(grounding.optInt("version",0)!=VERSION)return "GroundingEnvelope com versão incompatível.";
        if(grounding.optBoolean("inference_may_override_canonical",true))return "Inferência não pode sobrescrever fontes canônicas.";
        if(grounding.optJSONObject("campaign_state")==null||grounding.optJSONObject("rules")==null||grounding.optJSONObject("lore")==null||grounding.optJSONObject("inference")==null)return "Domínios obrigatórios do grounding ausentes.";
        JSONArray order=grounding.optJSONArray("authority_order");if(order==null||order.length()!=4||!"campaign_state".equals(order.optString(0))||!"rules".equals(order.optString(1))||!"lore".equals(order.optString(2))||!"inference".equals(order.optString(3)))return "Ordem de autoridade do grounding foi alterada.";
        JSONObject fps=grounding.optJSONObject("domain_fingerprints");if(fps==null)return "Fingerprints de domínio ausentes.";
        for(String d:DOMAINS)if(!CanonicalJson.sha256(domainObject(grounding,d)).equals(fps.optString(d)))return "Grounding adulterado no domínio "+d+".";
        if(!canonicalFingerprint(grounding).equals(grounding.optString("canonical_fingerprint")))return "Fingerprint canônico do grounding divergiu.";
        if(!fingerprint(grounding).equals(grounding.optString("grounding_fingerprint")))return "Fingerprint integral do grounding divergiu.";
        return "";
    }
}
