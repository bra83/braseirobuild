package com.braseiro.dnd5e;

/** Player/GM command with revision and durable event identity. */
public final class CoreCompanionControlEvent {
    public enum Actor { PLAYER, GM }
    public enum Kind { PROPOSE_ACTION, APPROVE, REJECT, CHANGE_MODE }
    public final String id,characterId,proposalId,summary;
    public final Actor actor;
    public final Kind kind;
    public final long revision;
    public final boolean highImpact;
    public final CoreCompanionControlState.Mode newMode;

    public CoreCompanionControlEvent(String id,String characterId,Actor actor,Kind kind,
                                     long revision,String proposalId,String summary,
                                     boolean highImpact,CoreCompanionControlState.Mode newMode) {
        this.id=id;this.characterId=characterId;this.actor=actor;this.kind=kind;
        this.revision=revision;this.proposalId=proposalId;this.summary=summary;
        this.highImpact=highImpact;this.newMode=newMode;
    }
}
