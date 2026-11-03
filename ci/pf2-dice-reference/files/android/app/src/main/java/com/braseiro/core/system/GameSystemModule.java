package com.braseiro.core.system;

import org.json.JSONObject;

/**
 * Contrato entre o Motor Barbara e qualquer sistema de RPG.
 * O motor cuida de mundo vivo, narrativa, memoria, RAG, cenas, tokens e saves.
 * O modulo decide apenas autoridade mecanica e identidade especifica do sistema.
 */
public interface GameSystemModule {
    String id();
    String displayName();
    String rulesProfile();
    String campaignIsolationKey();
    String rulesAuthorityPrompt();
    String narrativeDirective();
    String visualIdentityDirective();
    boolean acceptsRuleSource(String titleOrProfile);
    JSONObject validateCharacter(JSONObject sheet);
    JSONObject resolveCheck(int naturalRoll, int modifier, int dc);
    JSONObject turnBudget(int actionsUsed, boolean reactionUsed, boolean quickened, boolean slowed, int stunned);
}
