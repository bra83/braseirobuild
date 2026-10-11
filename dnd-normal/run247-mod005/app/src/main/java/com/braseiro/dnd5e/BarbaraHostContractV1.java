package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Fronteira versionada e fail-closed entre Braseiro D&D e Motor Barbara. */
public final class BarbaraHostContractV1 {
    public static final String ID="braseiro.barbara.host";
    public static final int VERSION=1;
    public static final String STATE_AUTHORITY="sqlite_host";
    public static final String MUTATION_PROTOCOL="proposal_validate_commit";
    private static final String[] READS={"campaign","scene","party","characters","combat","map","rules","lore","campaign_memory","living_world","weather","discovery","dice_history"};
    private static final String[] PROPOSALS={"state_updates","combat","scene_state","npc_updates","npc_cognition_updates","knowledge_updates","quest_updates","faction_updates","guild_updates","clock_updates","world_events","event_resolutions","pending_resolutions","map_updates","tactical_updates","journal_updates"};
    private BarbaraHostContractV1(){}

    public static JSONObject descriptor(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();try{
            out.put("contract_id",ID).put("contract_version",VERSION).put("state_authority",STATE_AUTHORITY).put("mutation_protocol",MUTATION_PROTOCOL)
                    .put("scene_commit_id_required",true).put("idempotent_scene_commits",true).put("grounding_envelope_version",GroundingEnvelope.VERSION)
                    .put("roll_result_authority","host_physical_faces").put("direct_database_write",false).put("world_event_ledger",true).put("failure_policy","fail_closed")
                    .put("read_surfaces",array(READS)).put("proposal_surfaces",array(PROPOSALS));
        }catch(Exception ignored){AppLog.ignored("BarbaraHostContractV1","Falha ao montar contrato determinístico.",ignored);}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONArray array(String[] xs){/*BR_JSON_WRAP*/try{JSONArray a=new JSONArray();for(String x:xs)a.put(x);return a;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static String problem(JSONObject host){
        if(host==null)return "Contrato Barbara ausente.";
        if(!ID.equals(host.optString("contract_id")))return "contract_id incompatível.";
        if(host.optInt("contract_version",0)!=VERSION)return "Versão do contrato Barbara incompatível.";
        if(!STATE_AUTHORITY.equals(host.optString("state_authority")))return "Autoridade persistente não pertence ao host.";
        if(!MUTATION_PROTOCOL.equals(host.optString("mutation_protocol")))return "Protocolo de mutação incompatível.";
        if(!host.optBoolean("scene_commit_id_required",false)||!host.optBoolean("idempotent_scene_commits",false))return "Idempotência de cena não foi garantida.";
        if(host.optInt("grounding_envelope_version",0)!=GroundingEnvelope.VERSION)return "Versão de grounding incompatível.";
        if(!"host_physical_faces".equals(host.optString("roll_result_authority")))return "Autoridade dos dados físicos incompatível.";
        if(host.optBoolean("direct_database_write",true))return "Barbara não pode receber escrita direta no banco.";
        if(!host.optBoolean("world_event_ledger",false))return "Ledger canônico de mundo é obrigatório.";
        if(!"fail_closed".equals(host.optString("failure_policy")))return "Política de falha deve ser fail_closed.";
        String p=surfaceProblem(host.optJSONArray("read_surfaces"),READS,"leitura");if(p.length()>0)return p;
        return surfaceProblem(host.optJSONArray("proposal_surfaces"),PROPOSALS,"proposta");
    }

    /** Confere o requisito declarado pelo Barbara contra o contrato real do host. */
    public static String compatibilityProblem(JSONObject host,JSONObject required){
        String p=problem(host);if(p.length()>0)return p;if(required==null)return "Motor Barbara não declarou host_contract_required.";
        if(!ID.equals(required.optString("contract_id"))||required.optInt("contract_version",0)!=VERSION)return "Motor Barbara requer contrato host incompatível.";
        for(String k:new String[]{"state_authority","mutation_protocol","roll_result_authority","failure_policy"}){String want=required.optString(k,"");if(want.length()>0&&!want.equals(host.optString(k,"")))return "Motor Barbara requer "+k+" incompatível.";}
        for(String k:new String[]{"scene_commit_id_required","idempotent_scene_commits","world_event_ledger"})if(required.optBoolean(k,false)&&!host.optBoolean(k,false))return "Motor Barbara requer "+k+".";
        if(required.optBoolean("direct_database_write",false))return "Motor Barbara solicitou escrita direta proibida.";
        int gv=required.optInt("grounding_envelope_version",GroundingEnvelope.VERSION);if(gv!=host.optInt("grounding_envelope_version"))return "Motor Barbara requer grounding incompatível.";
        return "";
    }
    private static String surfaceProblem(JSONArray actual,String[] required,String label){
        if(actual==null)return "Superfícies de "+label+" ausentes.";Set<String> seen=new HashSet<String>();for(int i=0;i<actual.length();i++){String x=actual.optString(i,"");if(x.length()==0||!seen.add(x))return "Superfície de "+label+" inválida/duplicada.";}
        for(String x:required)if(!seen.contains(x))return "Superfície obrigatória de "+label+" ausente: "+x+".";
        if(seen.size()!=required.length)return "Superfície desconhecida no contrato de "+label+".";return "";
    }
}
