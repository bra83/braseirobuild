package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Registra todas as consequências antes de aplicá-las, permitindo revisão. */
public final class StateTransactionManager {
    private final AppDb db;
    public StateTransactionManager(AppDb database){db=database;}

    public long stage(JSONObject envelope,String narration,String action){
        try{
            JSONObject payload=new JSONObject(envelope==null?"{}":envelope.toString());payload.put("narration",narration==null?"":narration);payload.put("action",action==null?"":action);
            return db.addPendingTransaction("Consequências da cena",summary(payload),payload.toString());
        }catch(Exception e){AppLog.e("StateTransaction","Falha ao preparar transação de estado.",e);return -1;}
    }

    public JSONObject get(long id){return db.getPendingTransaction(id);}
    public void applied(long id){db.setPendingTransactionStatus(id,"applied");}
    public void discarded(long id){db.setPendingTransactionStatus(id,"discarded");}
    /** Marca a transação como falha sem propagar uma segunda exceção durante o rollback. */
    public void failed(long id,String reason){
        if(id<=0)return;
        db.setPendingTransactionStatus(id,"failed");
        String safe=reason==null?"":reason.replaceAll("\\s+"," ").trim();
        if(safe.length()>240)safe=safe.substring(0,240);
        AppLog.w("StateTransaction","Transação "+id+" marcada como falha"+(safe.length()==0?"":": "+safe),null);
    }
    public void attachRollbackSnapshot(long id,String fileName){if(id>0&&fileName!=null&&fileName.length()>0)db.setPendingTransactionRollbackSnapshot(id,fileName);}
    public JSONObject lastApplied(){return db.getLastAppliedTransaction();}
    public String rollbackSnapshotName(JSONObject transaction){try{return new JSONObject(transaction==null?"{}":transaction.optString("payload_json","{}")).optString("_rollback_snapshot");}catch(Exception e){AppLog.w("StateTransaction","Snapshot de reversão inválido.",e);return "";}}
    public void rolledBack(long id){db.markPendingTransactionRolledBack(id);}

    public boolean hasChanges(JSONObject e){if(e==null)return false;return count(e,"state_updates")+count(e,"npc_updates")+count(e,"npc_cognition_updates")+count(e,"knowledge_updates")+count(e,"quest_updates")+count(e,"faction_updates")+count(e,"clock_updates")+count(e,"world_events")+count(e,"event_resolutions")+count(e,"pending_resolutions")+count(e,"journal_updates")+count(e,"map_updates")+count(e,"tactical_updates")>0||nonEmpty(e.optJSONObject("scene_state"))||!"none".equalsIgnoreCase(e.optJSONObject("combat")==null?"none":e.optJSONObject("combat").optString("action","none"))||tickChanges(e.optJSONObject("_local_tick"));}
    public boolean hasMechanicalChanges(JSONObject e){return e!=null&&(count(e,"state_updates")>0||!"none".equalsIgnoreCase(e.optJSONObject("combat")==null?"none":e.optJSONObject("combat").optString("action","none")));}

    public String describe(JSONObject e){
        if(e==null)return "Nenhuma alteração estruturada.";StringBuilder b=new StringBuilder();append(b,count(e,"state_updates"),"alteração(ões) de herói");JSONObject combat=e.optJSONObject("combat");if(combat!=null&&!"none".equalsIgnoreCase(combat.optString("action","none")))appendText(b,"combate: "+combat.optString("action"));if(nonEmpty(e.optJSONObject("scene_state")))appendText(b,"estado da cena");append(b,count(e,"npc_updates"),"memória(s) de NPC");append(b,count(e,"npc_cognition_updates"),"estado(s) mental(is) de NPC");append(b,count(e,"knowledge_updates"),"fato(s) de conhecimento");append(b,count(e,"quest_updates"),"missão(ões)");append(b,count(e,"faction_updates"),"facção(ões)");append(b,count(e,"clock_updates"),"relógio(s)");append(b,count(e,"world_events"),"evento(s) futuro(s)");append(b,count(e,"event_resolutions"),"evento(s) resolvido(s)");append(b,count(e,"pending_resolutions"),"pendência(s) autônoma(s) resolvida(s)");append(b,count(e,"journal_updates"),"registro(s) de diário");append(b,count(e,"map_updates"),"mudança(s) no mapa");append(b,count(e,"tactical_updates"),"movimento(s) no tabuleiro");JSONObject tick=e.optJSONObject("_local_tick");if(tickChanges(tick))appendText(b,"avanço do mundo: "+tick.optInt("hours_advanced",0)+" hora(s)");return b.length()==0?"Nenhuma alteração estruturada.":b.toString();
    }

    private static String summary(JSONObject p){return new StateSummary(p).text;}
    private static int count(JSONObject e,String key){JSONArray a=e.optJSONArray(key);return a==null?0:a.length();}
    private static boolean nonEmpty(JSONObject o){return o!=null&&o.length()>0;}
    private static boolean tickChanges(JSONObject t){if(t==null)return false;JSONObject resources=t.optJSONObject("company_resources_consumed");return t.optInt("hours_advanced",0)>0||count(t,"events_triggered")>0||count(t,"clocks_advanced")>0||(resources!=null&&resources.optInt("days",0)>0);}
    private static void append(StringBuilder b,int count,String label){if(count<=0)return;appendText(b,count+" "+label);}
    private static void appendText(StringBuilder b,String text){if(b.length()>0)b.append(" • ");b.append(text);}
    private static final class StateSummary{final String text;StateSummary(JSONObject p){StringBuilder b=new StringBuilder();append(b,count(p,"state_updates"),"herói(s)");append(b,count(p,"npc_updates")+count(p,"npc_cognition_updates"),"NPC(s)");append(b,count(p,"knowledge_updates"),"fato(s)");append(b,count(p,"quest_updates")+count(p,"faction_updates")+count(p,"clock_updates")+count(p,"world_events")+count(p,"event_resolutions")+count(p,"pending_resolutions"),"mudança(s) no mundo");if(nonEmpty(p.optJSONObject("scene_state")))appendText(b,"cena");JSONObject c=p.optJSONObject("combat");if(c!=null&&!"none".equalsIgnoreCase(c.optString("action","none")))appendText(b,"combate "+c.optString("action"));if(tickChanges(p.optJSONObject("_local_tick")))appendText(b,"tempo/relógios");text=b.length()==0?"Sem mudanças persistentes":b.toString();}}
}
