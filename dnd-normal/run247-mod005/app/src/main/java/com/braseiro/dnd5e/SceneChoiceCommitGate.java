package com.braseiro.dnd5e;

/**
 * Strict commit gate: three grounded choices plus one contextual initiative.
 * A bad model response is not a playable scene. A rules consultation, a
 * nonadvancing observation, and a pending die roll are explicitly exempt.
 */
public final class SceneChoiceCommitGate {
    private SceneChoiceCommitGate(){}
    public static boolean canCommit(boolean narrative,boolean nonAdvancing,
                                    boolean awaitingRoll,int proposedOptions){
        if(!narrative||nonAdvancing||awaitingRoll)return true;
        return proposedOptions==4;
    }
    public static String reason(int actual){
        return "ESCOLHAS: O Mestre ofereceu "+actual+
          " ações em vez de quatro verificáveis. Cena anterior preservada; "+
          "nenhum avanço de história, tempo ou missão foi autorizado. "+
          "Tente novamente para regenerar ações ancoradas no cenário.";
    }
}
