package com.braseiro.dnd5e;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persist returned state and approved gameplay effect in the SAME transaction. */
public final class CoreCompanionControlRules {
    private CoreCompanionControlRules() {}
    public static CoreCompanionControlResult apply(CoreCompanionControlState s,
                                                   CoreCompanionControlEvent e) {
        if(s==null||e==null||e.actor==null||e.kind==null||e.id==null||
           e.id.trim().isEmpty()||e.id.length()>128||
           !s.characterId.equals(e.characterId)) return bad(s,"INVALID_EVENT");
        if(s.receipts.contains(e.id))
            return new CoreCompanionControlResult(s,
                CoreCompanionControlResult.Decision.REPLAY,null,null);
        if(s.revision!=e.revision) return bad(s,"STALE_REVISION");
        if(s.revision==Long.MAX_VALUE) return bad(s,"REVISION_OVERFLOW");
        CoreCompanionControlTransitions.Change c=CoreCompanionControlTransitions.resolve(s,e);
        if(c.error!=null) return bad(s,c.error);
        Set<String> receipts=new LinkedHashSet<String>(s.receipts);
        receipts.add(e.id);
        CoreCompanionControlState next=new CoreCompanionControlState(
            s.characterId,c.mode,s.revision+1,c.pending,receipts);
        return new CoreCompanionControlResult(next,c.decision,null,c.approvedId);
    }
    private static CoreCompanionControlResult bad(CoreCompanionControlState s,String error) {
        return new CoreCompanionControlResult(s,CoreCompanionControlResult.Decision.DENIED,error,null);
    }
}
