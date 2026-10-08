package com.braseiro.dnd5e;

public final class Core2024ConcentrationBoundaryTest {
    static long checks=0;
    static void ok(boolean value) {
        checks++;
        if(!value) throw new AssertionError("check "+checks);
    }
    public static void main(String[] args) {
        for(int damage=1;damage<=5000;damage++) {
            Core2024ConcentrationState s=Core2024ConcentrationState.initial("h");
            s=Core2024ConcentrationRules.apply(s,new Core2024ConcentrationEvent(
                "cast","h",Core2024ConcentrationEvent.Type.START,0,"spell",0,null)).state;
            int dc=Math.max(10,damage/2);
            Core2024ConcentrationResult pass=Core2024ConcentrationRules.apply(s,
                new Core2024ConcentrationEvent("hit","h",
                    Core2024ConcentrationEvent.Type.DAMAGE,1,null,damage,dc));
            ok(pass.accepted()&&pass.saveDc==dc&&pass.state.active());
            Core2024ConcentrationResult fail=Core2024ConcentrationRules.apply(s,
                new Core2024ConcentrationEvent("hit","h",
                    Core2024ConcentrationEvent.Type.DAMAGE,1,null,damage,dc-1));
            ok(fail.accepted()&&!fail.state.active());
            ok(s.revision==1);
        }
        System.out.println("CONCENTRATION_BOUNDARY_PASS checks="+checks);
    }
}
