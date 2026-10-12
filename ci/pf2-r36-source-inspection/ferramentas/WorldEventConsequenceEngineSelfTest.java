package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public final class WorldEventConsequenceEngineSelfTest {
    private static int checks=0;
    public static void main(String[] args)throws Exception{
        AppDb db=new AppDb();WorldEventConsequenceEngine engine=new WorldEventConsequenceEngine(db);
        JSONObject consequence=new JSONObject()
                .put("knowledge_updates",new JSONArray().put(new JSONObject().put("subject","ponte").put("content","A ponte caiu.")))
                .put("quest_updates",new JSONArray().put(new JSONObject().put("title","Encontrar outra rota")))
                .put("journal_updates",new JSONArray().put(new JSONObject().put("title","Ponte caída").put("body","A travessia foi interrompida.")));
        JSONObject event=event(1,"event:ponte","Ponte caída",consequence);db.putEvent(event);db.failQuestUpdate=true;
        boolean failed=false;try{engine.applyTriggered(new JSONArray().put(event));}catch(Exception expected){failed=true;}
        check(failed,"falha intermediária é propagada");
        check(db.effects.isEmpty()&&db.receipts.isEmpty()&&db.audits.isEmpty(),"falha intermediária reverte efeitos, recibo e auditoria");
        check("scheduled".equals(db.getWorldEvent(1).optString("status")),"evento permanece retomável depois do rollback");

        db.failQuestUpdate=false;
        check(engine.applyTriggered(new JSONArray().put(event)),"retentativa estruturada conclui");
        int applied=db.effects.size(),audits=db.audits.size();
        check(applied==3&&db.receipts.contains("world_event:event:ponte")&&"resolved".equals(db.getWorldEvent(1).optString("status")),"efeitos, recibo e estado são confirmados juntos");
        check(engine.applyTriggered(new JSONArray().put(event))&&db.effects.size()==applied&&db.audits.size()==audits,"repetição não reaplica consequência");

        JSONObject blocked=event(2,"event:dano","Dano pendente",new JSONObject().put("damage",4));db.putEvent(blocked);
        check(!engine.applyTriggered(new JSONArray().put(blocked)),"efeito mecânico vai para revisão");
        int pending=db.pending.size(),blockedAudits=db.audits.size();
        check(pending==1&&"awaiting_review".equals(db.getWorldEvent(2).optString("status")),"revisão possui uma pendência correlacionada");
        check(!engine.applyTriggered(new JSONArray().put(blocked))&&db.pending.size()==pending&&db.audits.size()==blockedAudits,"evento em revisão não recria pendência nem auditoria");

        JSONObject invalid=event(3,"","Evento sem identidade",new JSONObject().put("journal_updates",new JSONArray().put(new JSONObject().put("body","não aplicar"))));db.putEvent(invalid);
        boolean refused=false;try{engine.applyTriggered(new JSONArray().put(invalid));}catch(Exception expected){refused=true;}
        check(refused,"evento sem event_key falha fechado");

        JSONObject autonomous=new JSONObject().put("journal_updates",new JSONArray().put(new JSONObject().put("title","Plano").put("body","Etapa concluída.")));
        int before=db.effects.size();engine.applyStructured(autonomous,"Facção","faction-plan:1");engine.applyStructured(autonomous,"Facção","faction-plan:1");
        check(db.effects.size()==before+1&&db.receipts.contains("faction-plan:1"),"efeito autônomo também é idempotente");
        System.out.println("OK: "+checks+" cenários transacionais e idempotentes de eventos.");
    }

    private static JSONObject event(long id,String key,String title,JSONObject consequence)throws Exception{
        String tx=key.length()==0?"":"world_event:"+key;
        return new JSONObject().put("id",id).put("event_key",key).put("transaction_key",tx).put("title",title).put("description",title)
                .put("status","scheduled").put("consequence_status","pending").put("consequence_json",consequence.toString());
    }
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
