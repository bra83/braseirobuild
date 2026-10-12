package com.braseiro.dnd5e.adventure;

import org.json.JSONObject;

/** Motor determinístico do grafo. O estado pode ser novo ou retomado da persistência. */
public final class AdventureGraphEngine implements IAdventureEngine {
    private AdventureModule currentModule;
    private AdventureState currentState;

    public AdventureGraphEngine() {}

    @Override public synchronized JSONObject startAdventure(AdventureModule module,long partyId){
        if(module==null)return error("Módulo nulo");
        return attachAdventure(module,new AdventureState(module.getRootNodeId()),"started");
    }

    public synchronized JSONObject resumeAdventure(AdventureModule module,AdventureState persistedState){
        if(module==null)return error("Módulo nulo");
        if(persistedState==null)return startAdventure(module,0L);
        if(module.getNode(persistedState.getCurrentNodeId())==null)return error("O estado salvo aponta para um nó inexistente no módulo atual.");
        return attachAdventure(module,persistedState,"resumed");
    }

    private JSONObject attachAdventure(AdventureModule module,AdventureState state,String status){
        this.currentModule=module;this.currentState=state;JSONObject res=new JSONObject();try{
            AdventureNode node=module.getNode(state.getCurrentNodeId());
            res.put("status",status).put("module_id",module.getId()).put("state",state.toJson());
            if(node!=null)res.put("current_node",node.toJson());
        }catch(Exception ignored){}return res;
    }

    @Override public synchronized JSONObject validateActionContext(String actionType,JSONObject payload){JSONObject res=new JSONObject();try{
        if(currentModule==null||currentState==null)return errorResolution("Nenhuma aventura ativa");
        AdventureNode node=currentModule.getNode(currentState.getCurrentNodeId());if(node==null)return errorResolution("Nó de cena não encontrado");
        String target=payload!=null?payload.optString("target_node_id",""):"";
        if(target.length()>0&&!canTransition(target)){res.put("resolved",false).put("status","blocked").put("error","Transição bloqueada por pré-requisitos");return res;}
        res.put("resolved",true).put("status","valid").put("check_requirement",node.getCheckRequirement());
    }catch(Exception ignored){}return res;}

    @Override public synchronized JSONObject commitResolution(JSONObject resolutionResult){JSONObject res=new JSONObject();try{
        if(currentState==null||resolutionResult==null){res.put("committed",false);return res;}
        String target=resolutionResult.optString("target_node_id","");
        if(target.length()>0){currentState.stageTransition(target,resolutionResult);boolean ok=currentState.commitTransaction();res.put("committed",ok).put("new_node_id",currentState.getCurrentNodeId());}
        else res.put("committed",true);
    }catch(Exception ignored){}return res;}

    @Override public synchronized JSONObject getNarrationContext(){JSONObject res=new JSONObject();try{if(currentModule!=null&&currentState!=null){AdventureNode n=currentModule.getNode(currentState.getCurrentNodeId());if(n!=null)res.put("title",n.getTitle()).put("description",n.getDescription()).put("location_id",n.getLocationId()).put("directives",n.getGmDirectives()).put("state",currentState.toJson());}}catch(Exception ignored){}return res;}

    @Override public synchronized boolean canTransition(String targetNodeId) {
        if (currentModule == null || currentState == null || targetNodeId == null)
            return false;
        AdventureNode node = currentModule.getNode(currentState.getCurrentNodeId());
        if (node == null) return false;
        // Several choices may share a destination. An inaccessible first choice
        // must not hide another valid action leading to that scene.
        for (AdventureTransition option : node.getTransitions()) {
            if (!targetNodeId.equals(option.getTargetNodeId())) continue;
            if (!option.getRequiredFlag().isEmpty()
                    && !currentState.hasFlag(option.getRequiredFlag())) continue;
            if (!option.getRequiredClue().isEmpty()
                    && !currentState.getDiscoveredClues().contains(option.getRequiredClue())) continue;
            return true;
        }
        return false;
    }
    @Override public synchronized boolean commitTransaction(){return currentState!=null&&currentState.commitTransaction();}
    @Override public synchronized void rollbackTransaction(){if(currentState!=null)currentState.rollbackTransaction();}
    public synchronized AdventureState getCurrentState(){return currentState;}

    private static JSONObject error(String msg){JSONObject o=new JSONObject();try{o.put("status","error").put("error",msg);}catch(Exception ignored){}return o;}
    private static JSONObject errorResolution(String msg){JSONObject o=new JSONObject();try{o.put("resolved",false).put("status","acao_nao_suportada").put("error",msg);}catch(Exception ignored){}return o;}
}
