package com.braseiro.dnd5e;

public final class CoreCompanionModesTest {
    static int checks=0;
    static void ok(boolean b) {checks++;if(!b) throw new AssertionError("check "+checks);}
    public static void main(String[] args) {
        for(CoreCompanionControlState.Mode mode:CoreCompanionControlState.Mode.values()) {
            CoreCompanionControlState s=CoreCompanionControlState.initial("c",mode);
            for(CoreCompanionControlEvent.Actor actor:CoreCompanionControlEvent.Actor.values()) {
                CoreCompanionControlEvent e=new CoreCompanionControlEvent(
                    "action","c",actor,CoreCompanionControlEvent.Kind.PROPOSE_ACTION,0,null,
                    "Talk to innkeeper",false,null);
                CoreCompanionControlResult r=CoreCompanionControlRules.apply(s,e);
                boolean allowed=mode==CoreCompanionControlState.Mode.GM_CURATED||
                    (mode==CoreCompanionControlState.Mode.GM&&actor==CoreCompanionControlEvent.Actor.GM)||
                    (mode==CoreCompanionControlState.Mode.PLAYER&&actor==CoreCompanionControlEvent.Actor.PLAYER);
                ok(r.accepted()==allowed);
                ok((r.decision==CoreCompanionControlResult.Decision.EXECUTE)==allowed);
            }
        }
        System.out.println("COMPANION_MODES_PASS checks="+checks);
    }
}
