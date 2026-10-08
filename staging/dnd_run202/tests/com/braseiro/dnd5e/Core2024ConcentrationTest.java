package com.braseiro.dnd5e;

public final class Core2024ConcentrationTest {
    static int checks=0;
    static void ok(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check "+checks);
    }
    public static void main(String[] args) {
        Core2024ConcentrationState initial=Core2024ConcentrationState.initial("hero");
        Core2024ConcentrationEvent start=new Core2024ConcentrationEvent(
            "a","hero",Core2024ConcentrationEvent.Type.START,0,"bless",0,null);
        Core2024ConcentrationResult a=Core2024ConcentrationRules.apply(initial,start);
        ok(a.accepted()&&a.state.active()&&a.state.revision==1);
        ok(Core2024ConcentrationRules.apply(a.state,start).replay);
        Core2024ConcentrationResult b=Core2024ConcentrationRules.apply(a.state,
            new Core2024ConcentrationEvent("b","hero",
                Core2024ConcentrationEvent.Type.DAMAGE,1,null,22,10));
        ok(b.accepted()&&b.saveDc==11&&!b.state.active());
        ok(b.ended==Core2024ConcentrationState.End.FAILED_SAVE);
        System.out.println("CONCENTRATION_PASS "+checks);
    }
}
