package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/**
 * Fronteira determinística entre a proposta do Mestre e a transação persistente.
 * IA propõe; host valida grounding, subset aprovado e base mecânica por mutação.
 */
public final class ConsequenceEngine {
    public static final int PROPOSAL_VERSION=2;
    private static final String[] ARRAYS={"state_updates","npc_updates","npc_cognition_updates","knowledge_updates","quest_updates","faction_updates","guild_updates","clock_updates","world_events","event_resolutions","pending_resolutions","journal_updates","map_updates","tactical_updates"};
    private final AppDb db;
    public ConsequenceEngine(AppDb database){if(database==null)throw new IllegalArgumentException("Banco ausente.");db=database;}

    public JSONObject prepare(JSONObject envelope,JSONObject grounding,String sceneCommitId) throws Exception {
        if(envelope==null)throw new IllegalArgumentException("Envelope de consequências ausente.");
        JSONObject validation=envelope.optJSONObject("_master_validation");
        // fail_closed descreve a POLÍTICA do validador (falhar fechado quando inválido),
        // não o estado atual da resposta. Uma resposta valid=true pode e deve ser aplicada.
        if(validation==null||!validation.optBoolean("valid",false))throw new IllegalStateException("Envelope sem validação final positiva.");
        if(sceneCommitId==null||sceneCommitId.trim().length()<16)throw new IllegalStateException("scene_commit_id ausente.");
        String groundingProblem=GroundingEnvelope.validate(grounding);if(groundingProblem.length()>0)throw new IllegalStateException(groundingProblem);
        JSONObject out=new JSONObject(envelope.toString());
        validateArrays(out);validateCharacterUpdates(out.optJSONArray("state_updates"));validateWorldRefs(out);validateApprovedSubset(out,sceneCommitId);
        if(out.optBoolean("_safe_narration_only",false)&&hasChanges(out))throw new IllegalStateException("Envelope marcado como somente narração tentou persistir consequências.");
        validateMechanicalBases(out,grounding);
        JSONObject proposal=new JSONObject();
        proposal.put("version",PROPOSAL_VERSION).put("scene_commit_id",sceneCommitId).put("world_profile",db.getActiveMapProfile()).put("prepared_at",System.currentTimeMillis())
                .put("grounding_version",grounding.optInt("version",0)).put("grounding_fingerprint",grounding.optString("grounding_fingerprint"))
                .put("mutation_count",mutationCount(out)).put("mechanical",hasMechanicalChanges(out)).put("proposal_hash",CanonicalJson.sha256(stripEphemeral(out)));
        out.put("_scene_commit_id",sceneCommitId).put("_consequence_proposal",proposal).put("_grounding_envelope",new JSONObject(grounding.toString()));
        return out;
    }

    /** Aprovação por categoria pode remover uma categoria, jamais reescrevê-la. */
    private void validateApprovedSubset(JSONObject selected,String commitId)throws Exception{
        JSONObject journal=db.getSceneCommit(commitId);long txId=journal.optLong("transaction_id",0);if(txId<=0)return;
        JSONObject tx=db.getPendingTransaction(txId);if(tx.length()==0)throw new IllegalStateException("A transação original do commit de cena não existe mais.");
        String txStatus=tx.optString("status","").toLowerCase(java.util.Locale.US);if("applied".equals(txStatus))throw new IllegalStateException("A transação consta como aplicada sem evento final canônico; reaplicação automática foi bloqueada.");if("discarded".equals(txStatus)||"rolled_back".equals(txStatus))throw new IllegalStateException("A transação original já é terminal e não pode ser reaplicada.");
        JSONObject staged=new JSONObject(tx.optString("payload_json","{}"));String stagedId=staged.optString("_scene_commit_id","").trim();if(stagedId.length()>0&&!stagedId.equals(commitId))throw new IllegalStateException("A pendência foi vinculada a outro scene_commit_id.");
        for(String key:ARRAYS){JSONArray want=selected.optJSONArray(key),base=staged.optJSONArray(key);if(want==null||want.length()==0)continue;if(base==null||!CanonicalJson.stringify(want).equals(CanonicalJson.stringify(base)))throw new IllegalStateException("A revisão alterou o conteúdo da categoria "+key+"; apenas remover categorias é permitido.");}
        validateObjectSubset(selected,staged,"scene_state",false);validateObjectSubset(selected,staged,"_local_tick",false);validateObjectSubset(selected,staged,"combat",true);
    }
    private static void validateObjectSubset(JSONObject selected,JSONObject staged,String key,boolean noneAllowed){
        JSONObject want=selected.optJSONObject(key);if(want==null||want.length()==0)return;if(noneAllowed&&"none".equalsIgnoreCase(want.optString("action","none")))return;
        JSONObject base=staged.optJSONObject(key);if(base==null||!CanonicalJson.stringify(want).equals(CanonicalJson.stringify(base)))throw new IllegalStateException("A revisão alterou o conteúdo da categoria "+key+"; apenas removê-la é permitido.");
    }

    public boolean hasChanges(JSONObject e){if(e==null)return false;for(String k:ARRAYS){JSONArray a=e.optJSONArray(k);if(a!=null&&a.length()>0)return true;}JSONObject c=e.optJSONObject("combat"),s=e.optJSONObject("scene_state"),t=e.optJSONObject("_local_tick");return c!=null&&!"none".equalsIgnoreCase(c.optString("action","none"))||(s!=null&&s.length()>0)||(t!=null&&t.length()>0);}
    public boolean hasMechanicalChanges(JSONObject e){JSONArray a=e==null?null:e.optJSONArray("state_updates");JSONObject c=e==null?null:e.optJSONObject("combat");return a!=null&&a.length()>0||c!=null&&!"none".equalsIgnoreCase(c.optString("action","none"));}

    private static void validateMechanicalBases(JSONObject e,JSONObject grounding){
        JSONArray states=e.optJSONArray("state_updates");if(states!=null)for(int i=0;i<states.length();i++){JSONObject x=states.optJSONObject(i);if(x!=null&&isMechanicalStateUpdate(x))validateMechanicalBasis(x.optJSONObject("mechanical_basis"),grounding,"state_updates["+i+"]",e);}
        JSONObject combat=e.optJSONObject("combat");if(combat!=null&&!"none".equalsIgnoreCase(combat.optString("action","none")))validateMechanicalBasis(combat.optJSONObject("mechanical_basis"),grounding,"combat",e);
    }
    private static void validateMechanicalBasis(JSONObject basis,JSONObject grounding,String label,JSONObject envelope){
        if(basis==null)throw new IllegalStateException(label+" sem mechanical_basis próprio.");
        String type=normalize(basis.optString("source_type")),evidence=basis.optString("evidence","").trim();if(evidence.length()<4)throw new IllegalStateException(label+" sem evidência mecânica específica.");
        JSONObject calc=basis.optJSONObject("calculation");String resolution=basis.optString("resolution_key","").trim();
        if(type.contains("compiled supplement")||type.contains("suplemento compilado")){
            if(!SupplementContentRegistry.verifyMechanicalBasis(basis))throw new IllegalStateException(label+" usa regra compilada não registrada ou evidência divergente.");
            return;
        }
        if(type.contains("manual")||type.contains("regra")){
            if(!GroundingEnvelope.hasRuleEvidence(grounding))throw new IllegalStateException(label+" usa manual sem evidência de regra recuperada.");
            if(basis.optString("book","").trim().length()==0||basis.optInt("page",0)<=0||basis.optString("excerpt","").trim().length()<12)throw new IllegalStateException(label+" usa manual sem livro/página/trecho verificável.");
            if(calc==null||(!basis.optBoolean("calculation_verified",false)&&calc.optString("operation","").trim().length()==0))throw new IllegalStateException(label+" usa manual sem cálculo normalizado/verificado.");
            return;
        }
        if(type.contains("roll")||type.contains("rolagem")||type.contains("resolved")||type.contains("teste resolvido")){
            if(calc==null||calc.optLong("roll_id",0)<=0||calc.optString("test_key","").trim().length()==0||calc.optString("action_signature","").trim().length()==0)throw new IllegalStateException(label+" não está vinculado a uma rolagem/teste local específico.");return;
        }
        if(type.contains("local engine")||type.contains("regra local")){
            JSONObject tick=envelope==null?null:envelope.optJSONObject("_local_tick");boolean committed=tick!=null&&tick.optBoolean("committed",false);String procedure=basis.optString("procedure_id","").trim();if(!committed&&procedure.length()==0&&resolution.length()==0)throw new IllegalStateException(label+" não identifica o procedimento local autorizado.");return;
        }
        if(type.contains("authorized activity")||type.contains("atividade autorizada")){if(resolution.length()==0)throw new IllegalStateException(label+" sem resolution_key de atividade autorizada.");return;}
        throw new IllegalStateException(label+" usa source_type mecânico não autorizado: "+basis.optString("source_type")+".");
    }
    private static String normalize(String s){return s==null?"":java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(java.util.Locale.US).replace('_',' ').trim();}
    private static boolean isMechanicalStateUpdate(JSONObject u){if(u==null)return false;if(u.has("hp_set")||u.has("hp_delta"))return true;for(String key:new String[]{"add_items","remove_items","condition_add","condition_remove"}){JSONArray a=u.optJSONArray(key);if(a!=null&&a.length()>0)return true;}JSONObject resources=u.optJSONObject("resource_changes");return resources!=null&&resources.length()>0;}
    private static void validateArrays(JSONObject out) throws Exception {for(String k:ARRAYS){Object raw=out.opt(k);if(raw==null){out.put(k,new JSONArray());continue;}if(!(raw instanceof JSONArray))throw new IllegalStateException(k+" deve ser uma lista.");JSONArray a=(JSONArray)raw;if(a.length()>100)throw new IllegalStateException(k+" excedeu 100 mutações numa única cena.");for(int i=0;i<a.length();i++)if(!(a.opt(i) instanceof JSONObject))throw new IllegalStateException(k+" contém item não estruturado.");}}
    private static void validateCharacterUpdates(JSONArray a){if(a==null)return;Set<String> seen=new HashSet<String>();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;String who=x.optString("character",x.optString("name","")).trim().toLowerCase(java.util.Locale.US);if(who.length()==0)throw new IllegalStateException("Alteração de herói sem identidade.");if(!seen.add(who))throw new IllegalStateException("Duas mutações concorrentes para o mesmo herói na mesma cena: "+who+".");}}
    private static void validateWorldRefs(JSONObject e){JSONArray resolutions=e.optJSONArray("event_resolutions");if(resolutions!=null)for(int i=0;i<resolutions.length();i++){JSONObject r=resolutions.optJSONObject(i);if(r!=null&&(r.optLong("id",0)<=0||r.optLong("transaction_id",0)<=0||r.optString("resolution_key","").trim().length()==0))throw new IllegalStateException("Resolução de evento sem correlação completa.");}}
    private static int mutationCount(JSONObject e){int n=0;for(String k:ARRAYS){JSONArray a=e.optJSONArray(k);if(a!=null)n+=a.length();}if(e.optJSONObject("scene_state")!=null&&e.optJSONObject("scene_state").length()>0)n++;if(e.optJSONObject("combat")!=null&&!"none".equalsIgnoreCase(e.optJSONObject("combat").optString("action","none")))n++;return n;}
    private static JSONObject stripEphemeral(JSONObject e){try{JSONObject x=new JSONObject(e.toString());x.remove("_consequence_proposal");x.remove("_grounding_envelope");return x;}catch(Exception ignored){return new JSONObject();}}
}
