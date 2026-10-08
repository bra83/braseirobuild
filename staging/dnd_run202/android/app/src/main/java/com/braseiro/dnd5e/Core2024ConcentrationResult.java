package com.braseiro.dnd5e;

/** Immutable result of a concentration event. */
public final class Core2024ConcentrationResult {
    public final Core2024ConcentrationState state;
    public final String error;
    public final int saveDc;
    public final boolean savePassed,replay;
    public final Core2024ConcentrationState.End ended;
    public Core2024ConcentrationResult(Core2024ConcentrationState state,String error,
                                       int saveDc,boolean savePassed,boolean replay,
                                       Core2024ConcentrationState.End ended) {
        this.state=state;this.error=error;this.saveDc=saveDc;
        this.savePassed=savePassed;this.replay=replay;this.ended=ended;
    }
    public boolean accepted() {return error==null;}
}
