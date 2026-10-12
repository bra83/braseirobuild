package com.braseiro.dnd5e.adventure;

import com.braseiro.dnd5e.AppDb;
import com.braseiro.dnd5e.MasterOrchestrator;
import com.braseiro.dnd5e.WorldStateEngine;
import org.json.JSONArray;
import org.json.JSONObject;

/** Coordenador real do Adventure Engine. Não usa PDF/RAG nem fabrica rolagens. */
public final class AdventureEngine implements IAdventureEngine {
    private final AdventureGraphEngine graphEngine;
    private final AdventurePersistenceAdapter persistenceAdapter;
    private final AdventureRulesAdapter rulesAdapter;
    private final AdventureWorldAdapter worldAdapter;
    private final AdventureAiAdapter aiAdapter;
    private AdventureModule activeModule;
    private AdventureState activeState;
    private long activePartyId;

    public AdventureEngine(AppDb database, MasterOrchestrator orchestrator, WorldStateEngine worldEngine){
        graphEngine=new AdventureGraphEngine();persistenceAdapter=new AdventurePersistenceAdapter(database);rulesAdapter=new AdventureRulesAdapter();worldAdapter=new AdventureWorldAdapter(database,worldEngine);aiAdapter=new AdventureAiAdapter(orchestrator);
    }

    public synchronized boolean installModule(AdventureModule module){return persistenceAdapter.saveModule(module);}

    public synchronized JSONObject loadOrStartAdventure(AdventureModule module,long partyId){JSONObject out=new JSONObject();try{
        if(module==null){return error("Módulo de aventura ausente.");}
        if(module.getNode(module.getRootNodeId())==null)return error("Módulo inválido: nó inicial ausente.");
        if(!persistenceAdapter.saveModule(module))return error("Falha ao persistir módulo da aventura.");
        activeModule=module;activePartyId=partyId;
        AdventureState loaded=persistenceAdapter.loadState(module.getId(),partyId);
        JSONObject res;
        if(loaded!=null){activeState=loaded;res=graphEngine.resumeAdventure(module,loaded);}else{AdventureState created=new AdventureState(module.getRootNodeId());
            if(!persistenceAdapter.saveState(module.getId(),partyId,created)){
                activeState=null;
                return error("Falha ao salvar primeira sessão da aventura. Nenhum jogo iniciado.");
            }
            activeState=created;res=graphEngine.resumeAdventure(module,activeState);}
        res.put("module_installed",true).put("current_node_id",activeState!=null?activeState.getCurrentNodeId():"").put("auto_rolls",false);return res;
    }catch(Exception e){return error(e.getMessage());}}

    /** Carrega somente módulo já compilado/instalado. Não cria aventura fictícia de fallback. */
    public synchronized JSONObject loadOrStart(String moduleId){AdventureModule mod=persistenceAdapter.loadModule(moduleId);if(mod==null){JSONObject o=error("Módulo de aventura não instalado: "+moduleId);try{o.put("requires_module_install",true).put("auto_rolls",false);}catch(Exception ignored){}return o;}return loadOrStartAdventure(mod,1L);}

    /**
     * A transition is identified by its action id, NOT only by the target scene.
     * Multiple choices may lead to the same node with different consequences.
     */
    public synchronized JSONObject processAction(String actionId, JSONObject params) {
        if (activeModule == null || activeState == null) return error("Nenhuma aventura ativa.");
        AdventureNode current = activeModule.getNode(activeState.getCurrentNodeId());
        if (current == null) return error("Nó atual inválido.");
        String target = params != null ? params.optString("target_node_id", "") : "";
        AdventureTransition selected = findTransition(current, target, actionId);
        if (target.length() > 0 && selected == null)
            return blocked("Ação ou destino não corresponde a uma transição disponível.");
        if (selected != null) target = selected.getTargetNodeId();
        int modifier = params != null ? params.optInt("modifier", 0) : 0;
        boolean advantage = params != null && params.optBoolean("advantage", false);
        boolean disadvantage = params != null && params.optBoolean("disadvantage", false);
        JSONObject requirement = rulesAdapter.determineCheckRequirement(current, selected);
        String input = actionId != null ? actionId : (params == null ? "" : params.optString("input", ""));
        if (requirement.optBoolean("requires_check", false)) {
            if (params == null || !params.has("roll"))
                return inputRequired(requirement, advantage, disadvantage, false);
            int first = params.optInt("roll", 0);
            if (advantage != disadvantage) {
                if (!params.has("roll_2"))
                    return inputRequired(requirement, advantage, disadvantage, true);
                return processPlayerAction(input, target, first, params.optInt("roll_2", 0),
                        modifier, advantage, disadvantage);
            }
            return processPlayerAction(input, target, first, first, modifier,
                    advantage, disadvantage);
        }
        return processPlayerAction(input, target, 0, 0, modifier, false, false);
    }

    public synchronized JSONObject processPlayerAction(String playerInput, String targetNodeId,
            int naturalRoll, int modifier, boolean advantage, boolean disadvantage) {
        if (advantage != disadvantage) {
            JSONObject req = new JSONObject();
            try { req.put("requires_check", true); } catch (Exception ignored) {}
            return inputRequired(req, advantage, disadvantage, true);
        }
        return processPlayerAction(playerInput, targetNodeId, naturalRoll, naturalRoll,
                modifier, advantage, disadvantage);
    }

    public synchronized JSONObject processPlayerAction(String playerInput, String targetNodeId,
            int firstRoll, int secondRoll, int modifier, boolean advantage, boolean disadvantage) {
        JSONObject result = new JSONObject();
        try {
            if (activeModule == null || activeState == null) return error("Nenhuma aventura ativa.");
            AdventureNode current = activeModule.getNode(activeState.getCurrentNodeId());
            if (current == null) return error("Nó atual inválido.");
            AdventureTransition transition = findTransition(current, targetNodeId, playerInput);
            if (targetNodeId != null && targetNodeId.length() > 0 && transition == null)
                return blocked("A ação informada não corresponde a uma transição deste nó.");
            String target = transition != null ? transition.getTargetNodeId() : "";
            if (transition != null) {
                if (transition.getRequiredFlag().length() > 0
                        && !activeState.hasFlag(transition.getRequiredFlag()))
                    return blocked("Pré-requisito da ação não atendido.");
                if (transition.getRequiredClue().length() > 0
                        && !activeState.getDiscoveredClues().contains(transition.getRequiredClue()))
                    return blocked("Pista necessária ainda não descoberta.");
            }
            JSONObject validationPayload = new JSONObject()
                    .put("target_node_id", target)
                    .put("player_input", playerInput == null ? "" : playerInput);
            JSONObject validation = graphEngine.validateActionContext("PLAYER_ACTION", validationPayload);
            if (!validation.optBoolean("resolved", false))
                return result.put("status", "blocked").put("validation", validation)
                        .put("committed", false).put("auto_rolls", false);

            JSONObject checkReq = rulesAdapter.determineCheckRequirement(current, transition);
            JSONObject rollResolution;
            if (checkReq.optBoolean("requires_check", false)) {
                rollResolution = rulesAdapter.evaluateExplicitRoll(checkReq,
                        firstRoll, secondRoll, modifier, advantage, disadvantage);
                if (!rollResolution.optBoolean("resolved", false))
                    return result.put("status", "requires_input").put("check_requirement", checkReq)
                            .put("roll_resolution", rollResolution).put("committed", false)
                            .put("auto_rolls", false);
            } else {
                rollResolution = new JSONObject().put("resolved", true).put("success", true)
                        .put("requires_input", false).put("auto_rolls", false);
            }

            boolean success = rollResolution.optBoolean("success", true);
            boolean failForward = transition != null && transition.isFailForward();
            boolean shouldAdvance = transition != null && (success || failForward);
            boolean committed = false;
            boolean worldApplied = true;
            AdventureNode resolvedNode = current;
            if (shouldAdvance) {
                // NEVER mutate the active state until SQLite has confirmed the write.
                AdventureState proposed = AdventureState.fromJson(activeState.toJson());
                JSONObject effects = copyObject(transition.getEffects());
                effects.put("mechanical_resolution", rollResolution)
                       .put("transition_id", transition.getId());
                proposed.stageTransition(target, effects);
                if (!proposed.commitTransaction()) return error("Falha ao confirmar a transição.");
                if (!persistenceAdapter.saveState(activeModule.getId(), activePartyId, proposed))
                    return error("Falha ao salvar a transição; o estado anterior foi preservado.");
                activeState = proposed;
                graphEngine.resumeAdventure(activeModule, activeState);
                committed = true;
                resolvedNode = activeModule.getNode(activeState.getCurrentNodeId());
                if (resolvedNode != null)
                    worldApplied = worldAdapter.applyNodeConsequences(resolvedNode, rollResolution);
            }
            JSONObject narration = aiAdapter.buildNarrationPayload(resolvedNode, activeState, rollResolution);
            return result.put("status", committed ? "success" :
                            (checkReq.optBoolean("requires_check", false) && !success
                                    ? "resolved_failure" : "resolved"))
                    .put("committed", committed)
                    .put("advanced", shouldAdvance && committed)
                    .put("current_node_id", activeState.getCurrentNodeId())
                    .put("check_requirement", checkReq).put("roll_resolution", rollResolution)
                    .put("narration_context", narration).put("world_applied", worldApplied)
                    .put("auto_rolls", false);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * Applies a universal command ONLY against a detached state copy. A committed
     * result becomes visible to the UI after a successful SQLite save. A failed
     * save cannot mutate the graph, active state, sheet or current world journal.
     * No AI text is interpreted as a mechanical effect by this coordinator.
     */
    public synchronized JSONObject processUniversal(JSONObject command) {
        if (activeModule == null || activeState == null)
            return error("Nenhuma aventura ativa.");
        if (command == null)
            return error("Comando universal ausente.");
        try {
            AdventureState proposed = AdventureState.fromJson(activeState.toJson());
            JSONObject result = UniversalRpgEngine.process(proposed, command);
            if (result == null) return error("Executor universal não retornou resultado.");
            if (!result.optBoolean("committed", false)) return result;
            if (!persistenceAdapter.saveState(activeModule.getId(), activePartyId, proposed))
                return error("Falha ao persistir o comando universal; nada foi confirmado.");
            activeState = proposed;
            graphEngine.resumeAdventure(activeModule, activeState);
            return result;
        } catch (Exception e) {
            return error("Erro no comando universal: " + e.getMessage());
        }
    }

    /**
     * A proposal is not a resolved action, but its pending state MUST survive
     * restart. Save it atomically before presenting the player's choice.
     */
    public synchronized JSONObject proposeManualAdjudication(JSONObject specification) {
        if (activeModule == null || activeState == null)
            return error("Nenhuma aventura ativa.");
        try {
            AdventureState proposed = AdventureState.fromJson(activeState.toJson());
            JSONObject result = ManualAdjudicationEngine.propose(proposed, specification);
            if (!"requires_manual_result".equals(result.optString("status"))) return result;
            if (!persistenceAdapter.saveState(activeModule.getId(), activePartyId, proposed))
                return error("Falha ao salvar proposta manual; não há teste pendente confirmado.");
            activeState = proposed;
            graphEngine.resumeAdventure(activeModule, activeState);
            return result;
        } catch (Exception e) {
            return error("Erro ao propor teste manual: " + e.getMessage());
        }
    }

    /** Persists a confirmed player choice, with replay and rollback safeguards. */
    public synchronized JSONObject resolveManualAdjudication(JSONObject playerChoice) {
        if (activeModule == null || activeState == null)
            return error("Nenhuma aventura ativa.");
        try {
            AdventureState proposed = AdventureState.fromJson(activeState.toJson());
            JSONObject result = ManualAdjudicationEngine.resolve(proposed, playerChoice);
            if (!"resolved_manual".equals(result.optString("status"))) return result;
            if (!persistenceAdapter.saveState(activeModule.getId(), activePartyId, proposed))
                return error("Falha ao persistir decisão manual; escolha não aplicada.");
            activeState = proposed;
            graphEngine.resumeAdventure(activeModule, activeState);
            return result;
        } catch (Exception e) {
            return error("Erro na decisão manual: " + e.getMessage());
        }
    }

    /**
     * Prefer the exact transition id. If only a destination is provided, accept
     * it exclusively when one choice leads there; otherwise the user must choose.
     */
    private AdventureTransition findTransition(AdventureNode node, String target, String actionId) {
        if (node == null) return null;
        String action = actionId == null ? "" : actionId.trim();
        String destination = target == null ? "" : target.trim();
        if (!action.isEmpty()) {
            for (AdventureTransition option : node.getTransitions()) {
                if (action.equals(option.getId())) {
                    return destination.isEmpty() || destination.equals(option.getTargetNodeId())
                            ? option : null;
                }
            }
        }
        if (destination.isEmpty()) return null;
        AdventureTransition unique = null;
        for (AdventureTransition option : node.getTransitions()) {
            if (!destination.equals(option.getTargetNodeId())) continue;
            if (unique != null) return null;
            unique = option;
        }
        return unique;
    }
    private static JSONObject blocked(String reason) {
        JSONObject out = error(reason);
        try { out.put("status", "blocked").put("committed", false); } catch (Exception ignored) {}
        return out;
    }
    private static JSONObject copyObject(JSONObject src){try{return src==null?new JSONObject():new JSONObject(src.toString());}catch(Exception e){return new JSONObject();}}
    private static JSONObject inputRequired(JSONObject req,boolean adv,boolean dis,boolean second){JSONObject o=new JSONObject();try{o.put("status","requires_input").put("requires_input",true).put("auto_rolls",false).put("input_type",second?"second_d20":"d20").put("advantage",adv&&!dis).put("disadvantage",dis&&!adv).put("check_requirement",req==null?new JSONObject():req);}catch(Exception ignored){}return o;}
    private static JSONObject error(String msg){JSONObject o=new JSONObject();try{o.put("status","error").put("error",msg==null?"Erro desconhecido":msg).put("auto_rolls",false);}catch(Exception ignored){}return o;}

    @Override public JSONObject startAdventure(AdventureModule module,long partyId){return loadOrStartAdventure(module,partyId);}
    @Override public JSONObject validateActionContext(String actionType,JSONObject payload){return graphEngine.validateActionContext(actionType,payload);}
    /**
     * No unchecked graph commit: the same transition, roll and persistence gates
     * used by the normal player-action path are mandatory here as well.
     */
    @Override public synchronized JSONObject commitResolution(JSONObject resolutionResult) {
        if (resolutionResult == null) return error("Resolução da ação ausente.");
        String target = resolutionResult.optString("target_node_id", "");
        String actionId = resolutionResult.optString("transition_id",
                resolutionResult.optString("action_id", ""));
        if (target.isEmpty() && actionId.isEmpty())
            return blocked("Transição e destino não informados.");
        return processAction(actionId, resolutionResult);
    }
    @Override public JSONObject getNarrationContext(){return graphEngine.getNarrationContext();}
    @Override public boolean canTransition(String targetNodeId){return graphEngine.canTransition(targetNodeId);}
    @Override public boolean commitTransaction(){boolean ok=graphEngine.commitTransaction();if(ok&&activeModule!=null&&activeState!=null)persistenceAdapter.saveState(activeModule.getId(),activePartyId,activeState);return ok;}
    @Override public void rollbackTransaction(){graphEngine.rollbackTransaction();}
    public AdventureModule getActiveModule(){return activeModule;} public AdventureState getActiveState(){return activeState;}
}
