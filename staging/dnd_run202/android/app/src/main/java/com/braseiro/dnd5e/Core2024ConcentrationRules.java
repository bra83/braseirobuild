package com.braseiro.dnd5e;

import java.util.LinkedHashSet;
import java.util.Set;

/** Pure reducer; caller persists this with the triggering event in one transaction. */
public final class Core2024ConcentrationRules {
    private Core2024ConcentrationRules() {}
    public static Core2024ConcentrationResult apply(Core2024ConcentrationState s,
                                                    Core2024ConcentrationEvent e) {
        if(s==null||e==null||e.type==null||e.id==null||e.id.trim().isEmpty()||
           e.id.length()>128||!s.casterId.equals(e.caster)) return bad(s,"INVALID_EVENT");
        if(s.processedEvents.contains(e.id))
            return new Core2024ConcentrationResult(s,null,0,false,true,
                Core2024ConcentrationState.End.NONE);
        if(s.revision!=e.revision) return bad(s,"STALE_REVISION");
        if(s.revision==Long.MAX_VALUE) return bad(s,"REVISION_OVERFLOW");
        Core2024ConcentrationTransitions.Change c=Core2024ConcentrationTransitions.resolve(s,e);
        if(c.error!=null) return bad(s,c.error);
        Set<String> receipts=new LinkedHashSet<String>(s.processedEvents);
        receipts.add(e.id);
        Core2024ConcentrationState.End last=c.ended==Core2024ConcentrationState.End.NONE?
            s.lastEnd:c.ended;
        if(e.type==Core2024ConcentrationEvent.Type.START)
            last=Core2024ConcentrationState.End.NONE;
        Core2024ConcentrationState next=new Core2024ConcentrationState(
            s.casterId,c.spell,s.revision+1,last,receipts);
        return new Core2024ConcentrationResult(next,null,c.dc,c.passed,false,c.ended);
    }
    private static Core2024ConcentrationResult bad(Core2024ConcentrationState s,String error) {
        return new Core2024ConcentrationResult(s,error,0,false,false,
            Core2024ConcentrationState.End.NONE);
    }
}
