package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

/**
 * Multi-system engine behind the real Braseiro AdventureState journal.
 * Loreweaver-inspired neutral outcome envelopes; Tome entity format;
 * separate per-system dice resolvers. No RAG or model writes to sheet.
 * All commits are persisted by AdventureEngine before publication.
 */
public final class UniversalRpgEngine {
    public static final String DND="dnd5e", YZE="forbidden-lands", STAR_WARS="star-wars-ffg";
    private UniversalRpgEngine() {}
    public static boolean supported(String s){return DND.equals(s)||YZE.equals(s)||STAR_WARS.equals(s);}
    public static JSONObject profiles(){
        return new JSONObject().put(DND,"D&D 5e / d20").put(YZE,"Forbidden Lands / Year Zero")
           .put(STAR_WARS,"Star Wars / Edge-AoR");
    }
    /** Mutates a COPY of persisted state; caller saves to SQLite or rejects it. */
    public static JSONObject process(AdventureState adventure,JSONObject command){
        if(adventure==null||command==null)return error("Estado ou comando universal ausente.");
        JSONObject state=adventure.getUniversalRpgState();
        JSONObject entities=state.optJSONObject("entities");if(entities==null)entities=new JSONObject();
        JSONArray history=state.optJSONArray("history");if(history==null)history=new JSONArray();
        JSONObject pending=state.optJSONObject("pending");
        String action=command.optString("action","");
        if("profiles".equals(action))return new JSONObject().put("status","ok").put("profiles",profiles()).put("committed",false);
        if("modules".equals(action))return new JSONObject().put("status","ok")
          .put("committed",false).put("modules",UniversalGithubModules.catalog());
        if("status".equals(action))return new JSONObject().put("status","ok").put("state",state).put("committed",false);
        if("ai_roll_tag".equals(action)){
            String sys=command.optString("system_id","");
            if(!DND.equals(sys))return error("O tag d20 AetherTable não substitui os dados próprios deste sistema.");
            JSONObject parsed=AetherTableRollIntentPort.parseDndRequest(
                command.optString("narration",""),command.optInt("dc",-1));
            if(!"recognized".equals(parsed.optString("status")))return parsed;
            String summary=command.optString("situation","").trim();
            if(summary.isEmpty())return error("Descreva a situação do teste para o jogador.");
            return process(adventure,new JSONObject().put("action","begin_check")
                .put("character_id",command.optString("character_id",""))
                .put("system_id",sys).put("situation",summary)
                .put("on_success",command.optString("on_success","Sucesso confirmado."))
                .put("on_failure",command.optString("on_failure","Fracasso confirmado."))
                .put("spec",parsed.getJSONObject("spec")));
        }
        // Atomic handoff: Gemini narrative and optional pending check are written
        // together inside the AdventureEngine's cloned AdventureState + SQLite save.
        if("accept_ai_scene".equals(action)) {
            if(pending!=null&&pending.length()>0)return error("Resolva o teste pendente antes de avançar o Mestre.");
            String eventId=command.optString("event_id","");
            if(!eventId.matches("[A-Za-z0-9_.-]{8,100}"))return error("Identificador da resposta da IA inválido.");
            JSONArray scenes=state.optJSONArray("scenes");if(scenes==null)scenes=new JSONArray();
            for(int i=0;i<scenes.length();i++){
                JSONObject old=scenes.optJSONObject(i);
                if(old!=null&&eventId.equals(old.optString("event_id")))return error("Resposta do Mestre duplicada.");
            }
            String actor=command.optString("character_id","");
            JSONObject entity=entities.optJSONObject(actor);
            if(entity==null)return error("Registre o personagem antes de chamar o Mestre.");
            String sys=entity.optJSONObject("meta").optString("system","");
            JSONObject parsed=UniversalAiProposalPort.parse(command.optString("raw_response",""),actor,sys);
            if(!"accepted".equals(parsed.optString("status")))return parsed;
            JSONObject proposed=null;
            if(parsed.optBoolean("requires_check",false)){
                JSONObject begin=process(adventure,parsed.getJSONObject("check_command"));
                if(!"requires_roll".equals(begin.optString("status")))return begin;
                proposed=begin.optJSONObject("pending");
                state=adventure.getUniversalRpgState();
            }
            JSONObject entry=new JSONObject().put("event_id",eventId)
                .put("character_id",actor).put("system_id",sys)
                .put("node_id",adventure.getCurrentNodeId())
                .put("narration",parsed.optString("narration"))
                .put("requires_check",parsed.optBoolean("requires_check",false));
            scenes.put(entry);
            // Bound saved AI transcript; historical result receipts remain intact.
            JSONArray recent=new JSONArray();
            for(int i=Math.max(0,scenes.length()-30);i<scenes.length();i++)recent.put(scenes.optJSONObject(i));
            state.put("scenes",recent);adventure.setUniversalRpgState(state);
            JSONObject result=new JSONObject().put("status",proposed==null?"scene_recorded":"requires_roll")
                .put("committed",true).put("scene",entry).put("check_pending",proposed!=null);
            if(proposed!=null)result.put("pending",proposed);
            return result;
        }
        if("register".equals(action)){
            String id=command.optString("character_id",""),sys=command.optString("system_id","");
            if(entities.optJSONObject(id)!=null)return error("Personagem já existe: "+id);
            JSONObject entity;
            try{entity=TomeEntityBridge.create(id,sys,command.optString("name",""),command.optJSONObject("attributes"),command.optJSONObject("resources"));}
            catch(Exception e){return error(e.getMessage());}
            entities.put(id,entity);state.put("entities",entities);adventure.setUniversalRpgState(state);
            return new JSONObject().put("status","registered").put("committed",true).put("entity",entity);
        }
        // Confirmed, all-or-nothing multi-system mutations. AI text NEVER enters this path.
        if("apply_effects".equals(action)){
            if(pending!=null&&pending.length()>0)return error("Resolva o teste pendente antes de alterar o estado.");
            JSONObject result=UniversalEffectTransaction.apply(state,command);
            if(result.optBoolean("committed",false)){
                adventure.setUniversalRpgState(result.getJSONObject("state"));
                result.remove("state"); // state is available via status; no stale reference leaks
            }
            return result;
        }
        // A player may explicitly reject a proposed mechanical consequence.
        // Recording the rejection avoids an infinite confirmation dialog after restart.
        if("decline_effects".equals(action)){
            if(pending!=null&&pending.length()>0)return error("Termine o teste pendente primeiro.");
            String request=command.optString("request_id", ""), actor=command.optString("character_id", "");
            for(int i=0;i<history.length();i++){
                JSONObject h=history.optJSONObject(i);
                if(h!=null&&request.equals(h.optString("request_id"))
                    &&actor.equals(h.optString("character_id"))) {
                    JSONArray offered=h.optJSONArray("proposed_effects");
                    if(offered==null||offered.length()==0 || h.optBoolean("effects_applied",false)
                        ||h.optBoolean("effects_declined",false))return error("Proposta não está aguardando decisão.");
                    h.put("effects_declined",true);
                    state.put("history",history);adventure.setUniversalRpgState(state);
                    return new JSONObject().put("status","effects_declined").put("committed",true)
                        .put("request_id",request);
                }
            }
            return error("Resultado não encontrado para recusa.");
        }
        if("resource".equals(action)){
            String id=command.optString("character_id","");JSONObject entity=entities.optJSONObject(id);
            if(entity==null)return error("Personagem desconhecido.");
            String eventId=command.optString("event_id","");
            if(!eventId.matches("[A-Za-z0-9_.-]{4,100}"))return error("event_id obrigatório (4 a 100 caracteres).");
            for(int i=0;i<history.length();i++){JSONObject h=history.optJSONObject(i);
               if(h!=null&&eventId.equals(h.optString("event_id")))return error("Evento duplicado: "+eventId);}
            Object deltaVal=command.opt("delta");
            if(!(deltaVal instanceof Number)||!Double.isFinite(((Number)deltaVal).doubleValue())
                ||((Number)deltaVal).doubleValue()!=((Number)deltaVal).intValue())
                return error("delta inteiro obrigatório.");
            JSONObject change=TomeEntityBridge.updateResource(entity,command.optString("resource",""),command.optInt("delta",0),command.optInt("expected_revision",-1));
            if(!"resource_updated".equals(change.optString("status")))return change;
            entities.put(id,change.optJSONObject("entity"));state.put("entities",entities);
            JSONObject receipt=new JSONObject().put("event_id",eventId).put("character_id",id)
                    .put("system_id",entity.optJSONObject("meta").optString("system"))
                    .put("resource_change",change.optJSONObject("receipt"));
            history.put(receipt);state.put("history",history);adventure.setUniversalRpgState(state);
            return new JSONObject().put("status","resource_updated").put("committed",true).put("receipt",receipt);
        }
        if("begin_check".equals(action)){
            if(pending!=null&&pending.length()>0)return error("Já existe um teste universal pendente.");
            String id=command.optString("character_id","");JSONObject entity=entities.optJSONObject(id);
            if(entity==null)return error("Personagem desconhecido.");
            String sys=entity.optJSONObject("meta").optString("system","");
            if(!sys.equals(command.optString("system_id",sys)))return error("A ficha pertence a outro sistema.");
            String situation=command.optString("situation","").trim();
            if(situation.isEmpty()||situation.length()>1000)return error("Situação inválida.");
            JSONObject spec=command.optJSONObject("spec");if(spec==null)return error("Faltam dados do teste.");
            if(!DND.equals(sys)&&spec.optJSONObject("dice_pool")==null)return error("Informe dice_pool do sistema selecionado.");
            if(DND.equals(sys)&&(!spec.has("dc")||spec.optInt("dc",-1)<0||spec.optInt("dc",-1)>99))return error("CD de D&D inválida.");
            String idCheck=UUID.randomUUID().toString();
            JSONArray successEffects, failureEffects;
            try {
                successEffects=UniversalEffectProposalPort.sanitize(command.optJSONArray("on_success_effects"));
                failureEffects=UniversalEffectProposalPort.sanitize(command.optJSONArray("on_failure_effects"));
            }catch(IllegalArgumentException e){return error(e.getMessage());}
            pending=new JSONObject().put("request_id",idCheck).put("character_id",id).put("system_id",sys)
               .put("origin_node_id",adventure.getCurrentNodeId()).put("situation",situation)
               .put("spec",new JSONObject(spec.toString())).put("on_success",command.optString("on_success","Sucesso confirmado."))
               .put("on_failure",command.optString("on_failure","Fracasso confirmado."))
               .put("on_success_effects",successEffects).put("on_failure_effects",failureEffects);
            state.put("pending",pending);adventure.setUniversalRpgState(state);
            return new JSONObject().put("status","requires_roll").put("committed",true).put("pending",pending);
        }
        if("resolve_check".equals(action)){
            if(pending==null||pending.length()==0)return error("Não há teste pendente.");
            String checkId=command.optString("request_id","");
            if(!checkId.equals(pending.optString("request_id")))return error("ID não corresponde ao teste pendente.");
            if(!adventure.getCurrentNodeId().equals(pending.optString("origin_node_id")))return error("Teste pertence a outra cena.");
            String sys=pending.optString("system_id"),mode=command.optString("mode","dice");
            JSONObject resolved;
            if("manual".equals(mode)){
                String outcome=command.optString("outcome","");
                if(!"success".equals(outcome)&&!"failure".equals(outcome))return error("Informe success ou failure.");
                boolean success="success".equals(outcome);
                resolved=new JSONObject().put("resolved",true).put("success",success)
                    .put("resolution_mode","player_manual").put("auto_rolls",false)
                    .put("outcome",LoreweaverOutcomePort.create(success?"success":"failure",
                       success?"Sucesso":"Fracasso",success,false,false,success?1:0,null));
            }else if("dice".equals(mode)){
                JSONObject sheet=entities.optJSONObject(pending.optString("character_id"));
                if(sheet==null)return error("A ficha não existe mais.");
                JSONObject supplied=command.optJSONObject("submission");
                if(supplied==null)return error("Forneça os resultados explícitos dos dados.");
                if(YZE.equals(sys))resolved=YearZeroDicePort.resolve(pending.optJSONObject("spec"),supplied);
                else if(STAR_WARS.equals(sys))resolved=StarWarsNarrativeDicePort.resolve(pending.optJSONObject("spec"),supplied);
                else if(DND.equals(sys))resolved=resolveDnd(pending.optJSONObject("spec"),supplied);
                else return error("Sistema não reconhecido.");
                if(!resolved.optBoolean("resolved",false))return error(resolved.optString("error","Rolagem inválida."));
                if(!resolved.has("outcome"))return error("Motor externo não retornou o contrato neutro.");
                resolved.put("resolution_mode","explicit_dice");
            }else return error("Modo de resolução inválido.");
            JSONObject receipt=new JSONObject().put("request_id",checkId).put("character_id",pending.optString("character_id"))
              .put("system_id",sys).put("situation",pending.optString("situation"))
              .put("resolution",resolved).put("consequence",resolved.optBoolean("success",false)
                       ?pending.optString("on_success"):pending.optString("on_failure"))
              .put("effects_applied",false).put("node_id",adventure.getCurrentNodeId())
              .put("proposed_effects",resolved.optBoolean("success",false)
                  ?pending.optJSONArray("on_success_effects"):pending.optJSONArray("on_failure_effects"));
            history.put(receipt);state.put("history",history).put("pending",new JSONObject());adventure.setUniversalRpgState(state);
            return new JSONObject().put("status","resolved").put("committed",true).put("receipt",receipt)
                    .put("narration_context",new JSONObject().put("system_id",sys).put("receipt",receipt)
                    .put("instruction","O teste já foi confirmado. Continue a cena; não role novamente e não altere recursos sem recibo."));
        }
        return error("Comando universal desconhecido: "+action);
    }
    private static JSONObject resolveDnd(JSONObject spec, JSONObject submission){
        int first=submission.optInt("d20",0);int second=submission.optInt("d20_2",first);
        int modifier=spec==null?0:spec.optInt("modifier",0),dc=spec==null?10:spec.optInt("dc",10);
        boolean adv=spec!=null&&spec.optBoolean("advantage",false);
        boolean dis=spec!=null&&spec.optBoolean("disadvantage",false);
        if((adv!=dis)&&!submission.has("d20_2"))return error("Dois resultados d20 obrigatórios para vantagem/desvantagem.");
        JSONObject result=new AdventureRulesAdapter().evaluateExplicitRoll(new JSONObject()
            .put("requires_check",true).put("dc",dc),first,second,modifier,adv,dis);
        if(result.optBoolean("resolved",false)){
            boolean success=result.optBoolean("success",false);
            int total=result.optInt("total", first+modifier);
            result.put("outcome",LoreweaverOutcomePort.create(success?"success":"failure",
                success?"Sucesso":"Fracasso",success,false,false,
                success?1:0,Integer.valueOf(total-dc)));
        }
        return result;
    }
    private static JSONObject error(String message){return new JSONObject().put("status","blocked").put("committed",false).put("error",message);}
}
