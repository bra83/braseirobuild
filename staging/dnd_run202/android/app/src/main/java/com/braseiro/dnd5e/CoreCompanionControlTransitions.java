package com.braseiro.dnd5e;

import java.util.LinkedHashMap;
import java.util.Map;

/** Authority checks and pending approval transitions, without persistence. */
public final class CoreCompanionControlTransitions {
    private CoreCompanionControlTransitions() {}
    public static final class Change {
        public final CoreCompanionControlState.Mode mode;
        public final Map<String,String> pending;
        public final CoreCompanionControlResult.Decision decision;
        public final String error,approvedId;
        Change(CoreCompanionControlState.Mode mode,Map<String,String> pending,
               CoreCompanionControlResult.Decision decision,String error,String approvedId) {
            this.mode=mode;this.pending=pending;this.decision=decision;
            this.error=error;this.approvedId=approvedId;
        }
    }
    public static Change resolve(CoreCompanionControlState s,CoreCompanionControlEvent e) {
        Map<String,String> pending=new LinkedHashMap<String,String>(s.pending);
        CoreCompanionControlState.Mode mode=s.mode;
        CoreCompanionControlResult.Decision decision;
        String approved=null;
        switch(e.kind) {
            case CHANGE_MODE:
                if(e.actor!=CoreCompanionControlEvent.Actor.PLAYER||e.newMode==null)
                    return bad(s,"PLAYER_ONLY_MODE_CHANGE");
                mode=e.newMode;pending.clear();
                decision=CoreCompanionControlResult.Decision.MODE_CHANGED;break;
            case PROPOSE_ACTION:
                if(e.actor==CoreCompanionControlEvent.Actor.GM &&
                   mode==CoreCompanionControlState.Mode.PLAYER) return bad(s,"PLAYER_CONTROLLED");
                if(e.actor==CoreCompanionControlEvent.Actor.PLAYER &&
                   mode==CoreCompanionControlState.Mode.GM) return bad(s,"GM_CONTROLLED");
                if(e.actor==CoreCompanionControlEvent.Actor.GM && e.highImpact &&
                   mode==CoreCompanionControlState.Mode.GM_CURATED) {
                    if(e.summary==null||e.summary.trim().isEmpty())
                        return bad(s,"PROPOSAL_SUMMARY_REQUIRED");
                    pending.put(e.id,e.summary);
                    decision=CoreCompanionControlResult.Decision.PENDING;
                } else decision=CoreCompanionControlResult.Decision.EXECUTE;
                break;
            case APPROVE: case REJECT:
                if(e.actor!=CoreCompanionControlEvent.Actor.PLAYER)
                    return bad(s,"PLAYER_APPROVAL_REQUIRED");
                if(e.proposalId==null||!pending.containsKey(e.proposalId))
                    return bad(s,"UNKNOWN_PROPOSAL");
                pending.remove(e.proposalId);approved=e.proposalId;
                decision=e.kind==CoreCompanionControlEvent.Kind.APPROVE?
                    CoreCompanionControlResult.Decision.EXECUTE:
                    CoreCompanionControlResult.Decision.CANCELLED;break;
            default:return bad(s,"UNSUPPORTED_EVENT");
        }
        return new Change(mode,pending,decision,null,approved);
    }
    private static Change bad(CoreCompanionControlState s,String error) {
        return new Change(s.mode,s.pending,CoreCompanionControlResult.Decision.DENIED,error,null);
    }
}
