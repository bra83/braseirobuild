package com.braseiro.dnd5e;

/** Only EXECUTE authorizes gameplay effects; PENDING must not apply an effect. */
public final class CoreCompanionControlResult {
    public enum Decision { EXECUTE, PENDING, DENIED, CANCELLED, MODE_CHANGED, REPLAY }
    public final CoreCompanionControlState state;
    public final Decision decision;
    public final String error,approvedProposalId;
    public CoreCompanionControlResult(CoreCompanionControlState state,Decision decision,
                                      String error,String approvedProposalId) {
        this.state=state;this.decision=decision;this.error=error;
        this.approvedProposalId=approvedProposalId;
    }
    public boolean accepted() {return error==null;}
}
