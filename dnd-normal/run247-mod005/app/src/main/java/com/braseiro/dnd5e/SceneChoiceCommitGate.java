package com.braseiro.dnd5e;

/**
 * Commit safety is not an action-card quantity gate. Suggested actions are
 * optional player guidance, not a precondition to apply a validated narrative.
 *
 * The upstream scene option pipeline has already rejected unsupported model
 * choices and provides a validated, voluntary momentum option whenever a
 * narrated scene exists. The app must never discard a valid Gemini response,
 * consume another API call, or repeat a player's declared action just because
 * fewer than three non-momentum choices could be grounded in sparse narration.
 *
 * Remain fail-closed when zero valid choices exist, or the supplied result
 * violates the maximum four-card UI contract. Rule/roll/snapshot integrity
 * checks continue to operate separately and must not be bypassed here.
 */
public final class SceneChoiceCommitGate {
    private SceneChoiceCommitGate(){}
    public static boolean canCommit(boolean narrative,boolean nonAdvancing,
                                    boolean awaitingRoll,int proposedOptions){
        if(!narrative||nonAdvancing||awaitingRoll)return true;
        return proposedOptions>=1 && proposedOptions<=4;
    }
    public static String reason(int actual){
        return "ESCOLHAS: nenhuma opção validada para a cena (total="+actual+"). "
            +"O Mestre preservou a ação e não autorizou avanço. "
            +"Esta condição não deve ocorrer apenas porque foram geradas "
            +"menos de quatro sugestões.";
    }
}
