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

    public synchronized JSONObject loadOrStartAdventure(AdventureModule module,long partyId) {
        // Never expose the previous campaign under a different module/party.
        graphEngine.clearSession();
        activeModule = null;
        activeState = null;
        activePartyId = 0L;
        try {
            if (module == null) return error("Módulo de aventura ausente.");
            if (module.getNode(module.getRootNodeId()) == null)
                return error("Módulo inválido: nó inicial ausente.");

            // Read and validate first. A damaged existing save must not even
            // replace the previously installed module/version in SQLite.
            AdventureState loaded = persistenceAdapter.loadStateForStart(module.getId(), partyId);
            if (loaded != null) {
                String node = loaded.getCurrentNodeId();
                String status = loaded.getStatus();
                boolean terminal = ("completed".equals(node) || "failed".equals(node))
                        && node.equals(status);
                if (module.getNode(node) == null && !terminal)
                    return error("Save incompatível com o grafo atual; módulo e save preservados.");
            }
            if (!persistenceAdapter.saveModule(module))
                return error("Falha ao persistir módulo da aventura.");

            activeModule = module;
            activePartyId = partyId;
            JSONObject result;
            if (loaded != null) {
                result = graphEngine.resumeAdventure(module, loaded);
                if ("error".equals(result.optString("status"))) {
                    activeModule = null;
                    return error("Save existente incompatível com o grafo atual; não sobrescrever.");
                }
                activeState = loaded;
            } else {
                AdventureState created = new AdventureState(module.getRootNodeId());
                if (!persistenceAdapter.saveState(module.getId(), partyId, created)) {
                    activeModule = null;
                    return error("Falha ao salvar primeira sessão da aventura.");
                }
                activeState = created;
                result = graphEngine.resumeAdventure(module, activeState);
            }
            result.put("module_installed", true)
                    .put("current_node_id", activeState.getCurrentNodeId())
                    .put("auto_rolls", false);
            return result;
        } catch (Exception e) {
            graphEngine.clearSession();
            activeModule = null;
            activeState = null;
            activePartyId = 0L;
            return error("Falha ao carregar aventura sem alterar o save: " + e.getMessage());
        }
    }

    /** Carrega somente módulo já compilado/instalado. Não cria aventura fictícia de fallback. */
    public synchronized JSONObject loadOrStart(String moduleId){AdventureModule mod=persistenceAdapter.loadModule(moduleId);if(mod==null){JSONObject o=error("Módulo de aventura não instalado: "+moduleId);try{o.put("requires_module_install",true).put("auto_rolls",false);}catch(Exception ignored){}return o;}return loadOrStartAdventure(mod,1L);}

    /**
     * Ação de catálogo: actionId é ID da transição, NUNCA apenas texto narrado.
     * O destino pode ser omitido (inferido pelo ID) ou deve coincidir exatamente.
     */
    public synchronized JSONObject processAction(String actionId, JSONObject params) {
        if (activeModule == null || activeState == null) return error("Nenhuma aventura ativa.");
        AdventureNode current = activeModule.getNode(activeState.getCurrentNodeId());
        String target = params == null ? "" : params.optString("target_node_id", "");
        AdventureTransition selected = findTransition(current, actionId, target, true);
        if (selected == null) return blocked("Transição inexistente ou ação e destino divergentes.");
        if (!prerequisitesSatisfied(selected)) return blocked("Pré-requisitos da ação não atendidos.");
        JSONObject req = rulesAdapter.determineCheckRequirement(current, selected);
        String input = params == null ? actionId : params.optString("input", actionId);
        int modifier = params == null ? 0 : params.optInt("modifier", 0);
        boolean advantage = params != null && params.optBoolean("advantage", false);
        boolean disadvantage = params != null && params.optBoolean("disadvantage", false);
        if (req.optBoolean("requires_check", false)) {
            if (params == null || !params.has("roll")) return inputRequired(req, advantage, disadvantage, false);
            int first = params.optInt("roll", 0);
            if (advantage != disadvantage) {
                if (!params.has("roll_2")) return inputRequired(req, advantage, disadvantage, true);
                return resolvePlayerAction(actionId, input, selected.getTargetNodeId(),
                        first, params.optInt("roll_2", 0), modifier, advantage, disadvantage);
            }
            return resolvePlayerAction(actionId, input, selected.getTargetNodeId(),
                    first, first, modifier, advantage, disadvantage);
        }
        return resolvePlayerAction(actionId, input, selected.getTargetNodeId(),
                0, 0, modifier, false, false);
    }

    public synchronized JSONObject processPlayerAction(String playerInput, String targetNodeId,
            int naturalRoll, int modifier, boolean advantage, boolean disadvantage) {
        if (advantage != disadvantage) {
            JSONObject requirement = new JSONObject();
            try { requirement.put("requires_check", true); } catch (Exception ignored) {}
            return inputRequired(requirement, advantage, disadvantage, true);
        }
        return resolvePlayerAction(null, playerInput, targetNodeId,
                naturalRoll, naturalRoll, modifier, advantage, disadvantage);
    }

    public synchronized JSONObject processPlayerAction(String playerInput, String targetNodeId,
            int firstRoll, int secondRoll, int modifier, boolean advantage, boolean disadvantage) {
        return resolvePlayerAction(null, playerInput, targetNodeId,
                firstRoll, secondRoll, modifier, advantage, disadvantage);
    }

    private JSONObject resolvePlayerAction(String explicitTransitionId, String playerInput,
            String targetNodeId, int firstRoll, int secondRoll, int modifier,
            boolean advantage, boolean disadvantage) {
        try {
            if (activeModule == null || activeState == null) return error("Nenhuma aventura ativa.");
            if (!"in_progress".equals(activeState.getStatus())) return blocked("Aventura encerrada.");
            AdventureNode current = activeModule.getNode(activeState.getCurrentNodeId());
            if (current == null) return blocked("Nó atual inválido.");
            // API antiga aceita destino sem ID apenas quando esse destino é inequívoco.
            // A API de catálogo exige ID explícito para distinguir rotas com mesmo destino.
            AdventureTransition selected = findTransition(current,
                    explicitTransitionId == null ? playerInput : explicitTransitionId,
                    targetNodeId, explicitTransitionId != null);
            if (selected == null) return blocked("Ação ambígua, inexistente ou destino divergente.");
            if (!prerequisitesSatisfied(selected)) return blocked("Pré-requisitos da transição não atendidos.");
            String nextNodeId = selected.getTargetNodeId();
            if (nextNodeId.length() == 0 ||
                    (activeModule.getNode(nextNodeId) == null && !"completed".equals(nextNodeId)
                            && !"failed".equals(nextNodeId))) {
                return blocked("Destino da transição não existe no módulo.");
            }
            JSONObject check = rulesAdapter.determineCheckRequirement(current, selected);
            JSONObject roll;
            if (check.optBoolean("requires_check", false)) {
                roll = rulesAdapter.evaluateExplicitRoll(check, firstRoll, secondRoll, modifier,
                        advantage, disadvantage);
                if (!roll.optBoolean("resolved", false)) {
                    return new JSONObject().put("status", "requires_input").put("committed", false)
                            .put("check_requirement", check).put("roll_resolution", roll)
                            .put("auto_rolls", false);
                }
            } else {
                roll = new JSONObject().put("resolved", true).put("success", true)
                        .put("requires_input", false).put("auto_rolls", false);
            }
            boolean success = roll.optBoolean("success", true);
            if (!success && !selected.isFailForward()) {
                return new JSONObject().put("status", "resolved_failure").put("committed", false)
                        .put("advanced", false).put("current_node_id", activeState.getCurrentNodeId())
                        .put("check_requirement", check).put("roll_resolution", roll)
                        .put("auto_rolls", false);
            }

            // Commit em duas fases: nunca alterar activeState ou grafo ANTES do SQLite.
            AdventureState proposed = AdventureState.fromJson(activeState.toJson());
            JSONObject effects = copyObject(selected.getEffects());
            effects.put("mechanical_resolution", roll).put("transition_id", selected.getId());
            proposed.stageTransition(nextNodeId, effects);
            if (!proposed.commitTransaction()) return error("Falha ao preparar transição da aventura.");
            if ("completed".equals(nextNodeId) || "failed".equals(nextNodeId))
                proposed.setStatus(nextNodeId);
            AdventureNode resolvedNode = activeModule.getNode(nextNodeId);
            String locationId = resolvedNode == null ? "" : resolvedNode.getLocationId();
            if (!persistenceAdapter.saveTransitionWithLocation(
                    activeModule.getId(), activePartyId, proposed, locationId)) {
                return error("Falha ao salvar escolha e local no SQLite; nenhuma alteração foi aplicada.");
            }
            activeState = proposed;
            graphEngine.resumeAdventure(activeModule, activeState);
            boolean worldSynced = resolvedNode == null ||
                    worldAdapter.applyNodeConsequences(resolvedNode, roll);
            JSONObject narration = aiAdapter.buildNarrationPayload(resolvedNode, activeState, roll);
            return new JSONObject().put("status", "success").put("committed", true)
                    .put("advanced", true).put("transition_id", selected.getId())
                    .put("current_node_id", activeState.getCurrentNodeId())
                    .put("check_requirement", check).put("roll_resolution", roll)
                    .put("world_sync_ok", worldSynced).put("narration_context", narration)
                    .put("auto_rolls", false);
        } catch (Exception e) {
            return error("Não foi possível executar a escolha: " + e.getMessage());
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

    private static AdventureTransition findTransition(AdventureNode node, String actionId,
            String target, boolean requireId) {
        if (node == null) return null;
        String id = actionId == null ? "" : actionId.trim();
        String destination = target == null ? "" : target.trim();
        if (!id.isEmpty()) {
            for (AdventureTransition t : node.getTransitions()) {
                if (id.equals(t.getId()))
                    return destination.isEmpty() || destination.equals(t.getTargetNodeId()) ? t : null;
            }
        }
        if (requireId || destination.isEmpty()) return null;
        AdventureTransition sole = null;
        for (AdventureTransition t : node.getTransitions()) {
            if (destination.equals(t.getTargetNodeId())) {
                if (sole != null) return null; // Destino ambíguo: exigir transition_id.
                sole = t;
            }
        }
        return sole;
    }

    private boolean prerequisitesSatisfied(AdventureTransition t) {
        return t != null && (t.getRequiredFlag().isEmpty() ||
                activeState.hasFlag(t.getRequiredFlag())) &&
                (t.getRequiredClue().isEmpty() ||
                activeState.getDiscoveredClues().contains(t.getRequiredClue()));
    }

    private static JSONObject blocked(String reason) {
        JSONObject out = error(reason);
        try { out.put("status", "blocked").put("committed", false).put("advanced", false); }
        catch (Exception ignored) {}
        return out;
    }
    private static JSONObject copyObject(JSONObject src){try{return src==null?new JSONObject():new JSONObject(src.toString());}catch(Exception e){return new JSONObject();}}
    private static JSONObject inputRequired(JSONObject req,boolean adv,boolean dis,boolean second){JSONObject o=new JSONObject();try{o.put("status","requires_input").put("requires_input",true).put("auto_rolls",false).put("input_type",second?"second_d20":"d20").put("advantage",adv&&!dis).put("disadvantage",dis&&!adv).put("check_requirement",req==null?new JSONObject():req);}catch(Exception ignored){}return o;}
    private static JSONObject error(String msg){JSONObject o=new JSONObject();try{o.put("status","error").put("error",msg==null?"Erro desconhecido":msg).put("auto_rolls",false);}catch(Exception ignored){}return o;}

    @Override public JSONObject startAdventure(AdventureModule module,long partyId){return loadOrStartAdventure(module,partyId);}
    @Override public JSONObject validateActionContext(String actionType,JSONObject payload){return graphEngine.validateActionContext(actionType,payload);}
    @Override public synchronized JSONObject commitResolution(JSONObject resolutionResult) {
        if (resolutionResult == null) return error("Resolução ausente.");
        String actionId = resolutionResult.optString("action_id",
                resolutionResult.optString("transition_id", ""));
        if (actionId.isEmpty()) return blocked("Confirmação exige ID da ação e validação de regras.");
        // Não aceitar efeitos, sucesso ou resultados mecânicos autodeclarados neste canal.
        JSONObject params = new JSONObject();
        try {
            params.put("target_node_id", resolutionResult.optString("target_node_id", ""));
            if (resolutionResult.has("roll")) params.put("roll", resolutionResult.optInt("roll"));
            if (resolutionResult.has("roll_2")) params.put("roll_2", resolutionResult.optInt("roll_2"));
            params.put("modifier", resolutionResult.optInt("modifier", 0));
            params.put("advantage", resolutionResult.optBoolean("advantage", false));
            params.put("disadvantage", resolutionResult.optBoolean("disadvantage", false));
            params.put("input", resolutionResult.optString("player_input", actionId));
        } catch (Exception e) { return error("Argumentos de resolução inválidos."); }
        return processAction(actionId, params);
    }
    @Override public JSONObject getNarrationContext(){return graphEngine.getNarrationContext();}
    @Override public boolean canTransition(String targetNodeId){return graphEngine.canTransition(targetNodeId);}
    @Override public synchronized boolean commitTransaction(){return false; /* Escrita direta sem ação validada é proibida. */}
    @Override public void rollbackTransaction(){graphEngine.rollbackTransaction();}
    public AdventureModule getActiveModule(){return activeModule;} public AdventureState getActiveState(){return activeState;}
}
