package com.braseiro.dnd5e;

public final class CoreCompanionControlTest {
    static int checks=0;
    static void ok(boolean b) {checks++;if(!b) throw new AssertionError("check "+checks);}
    static CoreCompanionControlEvent event(String id,CoreCompanionControlEvent.Actor actor,
        CoreCompanionControlEvent.Kind kind,long revision,String proposal,boolean high) {
        return new CoreCompanionControlEvent(id,"hero",actor,kind,revision,proposal,
            "Use expensive item",high,null);
    }
    public static void main(String[] args) {
        CoreCompanionControlState s=CoreCompanionControlState.initial("hero",
            CoreCompanionControlState.Mode.GM_CURATED);
        CoreCompanionControlResult p=CoreCompanionControlRules.apply(s,
            event("proposal",CoreCompanionControlEvent.Actor.GM,
                CoreCompanionControlEvent.Kind.PROPOSE_ACTION,0,null,true));
        ok(p.accepted()&&p.decision==CoreCompanionControlResult.Decision.PENDING);
        ok(p.state.pending.containsKey("proposal"));
        CoreCompanionControlResult unauthorized=CoreCompanionControlRules.apply(p.state,
            event("approval",CoreCompanionControlEvent.Actor.GM,
                CoreCompanionControlEvent.Kind.APPROVE,1,"proposal",false));
        ok(!unauthorized.accepted()&&unauthorized.state.revision==1);
        CoreCompanionControlResult approved=CoreCompanionControlRules.apply(p.state,
            event("approval",CoreCompanionControlEvent.Actor.PLAYER,
                CoreCompanionControlEvent.Kind.APPROVE,1,"proposal",false));
        ok(approved.accepted()&&approved.decision==CoreCompanionControlResult.Decision.EXECUTE);
        ok(approved.state.pending.isEmpty());
        ok(CoreCompanionControlRules.apply(approved.state,
            event("approval",CoreCompanionControlEvent.Actor.PLAYER,
                CoreCompanionControlEvent.Kind.APPROVE,1,"proposal",false)).decision==
                CoreCompanionControlResult.Decision.REPLAY);
        System.out.println("COMPANION_CONTROL_PASS checks="+checks);
    }
}
