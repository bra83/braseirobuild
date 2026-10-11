package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Player-approved, atomic multi-system operations over Tome and the universal world.
 * Does NOT accept model output directly. UniversalRpgEngine calls this only for the
 * explicit apply_effects command; AdventureEngine persists the whole proposed
 * AdventureState before publishing it to the active session.
 */
public final class UniversalEffectTransaction {
    private UniversalEffectTransaction() {}

    private static JSONObject blocked(String msg){return new JSONObject().put("status","blocked")
            .put("committed",false).put("error",msg);}
    private static boolean key(String s){return s!=null && s.matches("[A-Za-z0-9_.-]{1,80}");}
    private static Integer exactInt(Object n){
        if(!(n instanceof Number))return null;
        double d=((Number)n).doubleValue();
        if(!Double.isFinite(d)||d!=Math.rint(d)||d>Integer.MAX_VALUE||d<Integer.MIN_VALUE)return null;
        return Integer.valueOf((int)d);
    }
    private static JSONObject item(JSONArray items,String id){
        for(int i=0;i<items.length();i++){
            JSONObject candidate=items.optJSONObject(i);
            if(candidate!=null&&id.equals(candidate.optString("id")))return candidate;
        }
        return null;
    }
    private static JSONObject fact(JSONArray facts,String id){
        for(int i=0;i<facts.length();i++){
            JSONObject f=facts.optJSONObject(i);
            if(f!=null&&id.equals(f.optString("id")))return f;
        }
        return null;
    }

    public static JSONObject apply(JSONObject originalState,JSONObject command){
        if(originalState==null||command==null)return blocked("Estado ou comando ausente.");
        String actor=command.optString("character_id", "");
        String event=command.optString("event_id", "");
        if(!key(actor)||!event.matches("[A-Za-z0-9_.-]{8,100}"))
            return blocked("Personagem ou identificador do evento inválido.");
        JSONArray effects=command.optJSONArray("effects");
        if(effects==null||effects.length()<1||effects.length()>12)
            return blocked("Informe de 1 a 12 efeitos confirmados pelo jogador.");
        JSONArray history=originalState.optJSONArray("history");
        if(history!=null){
            for(int i=0;i<history.length();i++){
                JSONObject prior=history.optJSONObject(i);
                if(prior!=null&&event.equals(prior.optString("event_id")))
                    return blocked("Este evento já foi confirmado.");
            }
        }
        JSONObject state=new JSONObject(originalState.toString());
        JSONObject entities=state.optJSONObject("entities");
        JSONObject entity=entities==null?null:entities.optJSONObject(actor);
        if(entity==null)return blocked("Personagem não registrado nesta campanha.");
        int revision=entity.optInt("braseiro_revision",-1);
        if(revision<0||revision!=command.optInt("expected_revision",-2))
            return blocked("Revisão desatualizada. Reabra a ficha antes de confirmar.");
        JSONArray receipts=new JSONArray();
        for(int i=0;i<effects.length();i++){
            JSONObject effect=effects.optJSONObject(i);
            if(effect==null)return blocked("Efeito "+(i+1)+" não é um objeto.");
            String kind=effect.optString("kind", "");
            if("resource".equals(kind)){
                String r=effect.optString("key", "");
                Integer delta=exactInt(effect.opt("delta"));
                if(!key(r)||delta==null)return blocked("Recurso ou delta inválido.");
                JSONObject updated=TomeEntityBridge.updateResource(entity,r,delta.intValue(),revision);
                if(!"resource_updated".equals(updated.optString("status")))
                    return blocked(updated.optString("error","Alteração de recurso não permitida."));
                entity=updated.optJSONObject("entity");revision=entity.optInt("braseiro_revision",-1);
                receipts.put(new JSONObject().put("kind","resource").put("result",updated.optJSONObject("receipt")));
            }else if("item".equals(kind)){
                String id=effect.optString("item_id", "");
                Integer delta=exactInt(effect.opt("delta"));
                if(!key(id)||delta==null||delta.intValue()==0||Math.abs((long)delta.intValue())>9999)
                    return blocked("ID de item ou quantidade inválidos.");
                JSONObject inventory=entity.optJSONObject("inventory");
                if(inventory==null){inventory=new JSONObject();entity.put("inventory",inventory);}
                JSONArray items=inventory.optJSONArray("items");
                if(items==null){items=new JSONArray();inventory.put("items",items);}
                JSONObject existing=item(items,id);
                int before=existing==null?0:existing.optInt("quantity",0);
                long after=(long)before+delta.intValue();
                if(after<0||after>9999)return blocked("Quantidade de item fora de 0..9999.");
                String name=effect.optString("name", "").trim();
                if(existing==null){
                    if(delta.intValue()<=0||name.isEmpty()||name.length()>120)
                        return blocked("Item novo exige nome e quantidade positiva.");
                    existing=new JSONObject().put("id",id).put("name",name).put("quantity",(int)after);
                    items.put(existing);
                }else{
                    existing.put("quantity",(int)after);
                }
                revision++;entity.put("braseiro_revision",revision);
                receipts.put(new JSONObject().put("kind","item").put("item_id",id)
                    .put("name",existing.optString("name")).put("before",before).put("after",(int)after)
                    .put("delta",delta.intValue()));
            }else if("time".equals(kind)){
                Integer minutes=exactInt(effect.opt("minutes"));
                if(minutes==null||minutes.intValue()<1||minutes.intValue()>1440)
                    return blocked("Tempo deve avançar entre 1 e 1440 minutos.");
                long old=state.optInt("elapsed_minutes",0);
                long updated=old+minutes.intValue();
                if(updated>1000000000L)return blocked("Relógio ultrapassou limite de campanha.");
                state.put("elapsed_minutes",(int)updated);
                receipts.put(new JSONObject().put("kind","time").put("before",old)
                    .put("after",updated).put("minutes",minutes.intValue()));
            }else if("world_fact".equals(kind)){
                String id=effect.optString("fact_id", "");
                String content=effect.optString("text", "").trim();
                if(!key(id)||content.length()<3||content.length()>500)
                    return blocked("ID ou texto do fato do mundo inválido.");
                JSONArray facts=state.optJSONArray("world_facts");
                if(facts==null){facts=new JSONArray();state.put("world_facts",facts);}
                if(fact(facts,id)!=null)return blocked("Fato do mundo já existente: "+id);
                if(facts.length()>=500)return blocked("Limite de fatos do mundo atingido.");
                facts.put(new JSONObject().put("id",id).put("text",content)
                    .put("confirmed_by","player").put("event_id",event));
                receipts.put(new JSONObject().put("kind","world_fact").put("fact_id",id));
            }else{return blocked("Tipo de efeito não autorizado: "+kind);}
        }
        entities.put(actor,entity);
        state.put("entities",entities);
        JSONArray all=state.optJSONArray("history");if(all==null)all=new JSONArray();
        JSONObject receipt=new JSONObject().put("event_id",event).put("character_id",actor)
            .put("system_id",entity.optJSONObject("meta").optString("system"))
            .put("confirmed_by","player").put("effects_applied",true)
            .put("revision_before",command.optInt("expected_revision"))
            .put("revision_after",revision).put("effects",receipts);
        String request=command.optString("request_id", "");
        if(!request.isEmpty()){
            JSONObject resolved=null;
            for(int i=0;i<all.length();i++){
                JSONObject h=all.optJSONObject(i);
                if(h!=null&&request.equals(h.optString("request_id"))){resolved=h;break;}
            }
            if(resolved==null||!actor.equals(resolved.optString("character_id")))
                return blocked("Resultado do teste não encontrado para este personagem.");
            if(resolved.optBoolean("effects_applied",false) || resolved.optBoolean("effects_declined",false))
                return blocked("A consequência deste teste já foi decidida.");
            JSONArray offered=resolved.optJSONArray("proposed_effects");
            if(offered!=null && offered.length()>0 && !UniversalEffectProposalPort.same(offered,effects))
                return blocked("A proposta foi alterada. Confirme os efeitos originalmente apresentados ou recuse a proposta.");
            resolved.put("effects_applied",true).put("effects_event_id",event);
            receipt.put("request_id",request);
        }
        all.put(receipt);state.put("history",all);
        return new JSONObject().put("status","effects_applied").put("committed",true)
            .put("state",state).put("receipt",receipt);
    }
}
