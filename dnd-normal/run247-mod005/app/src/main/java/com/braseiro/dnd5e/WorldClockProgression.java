package com.braseiro.dnd5e;

/** Pure, deterministic clock advancement used by the persistent world tick. */
public final class WorldClockProgression {
    public final int before, after, ticks, tickDelta;
    public final double remainder, interval, rate;
    private WorldClockProgression(int b,int a,int t,int d,double rem,double i,double r){before=b;after=a;ticks=t;tickDelta=d;remainder=rem;interval=i;rate=r;}

    public static WorldClockProgression advance(int current,int maximum,double accumulated,double elapsed,double interval,double rate,int tickDelta,boolean regressionAllowed){
        int before=Math.max(0,current), max=Math.max(1,maximum);
        // Malformed legacy clock state may contain NaN, infinities or an
        // out-of-range elapsed value. Never write a non-finite remainder back
        // into the persistent world; an invalid tick becomes a safe no-op.
        double step=finitePositive(interval)?Math.max(1d,interval):1d;
        // A configured rate of zero was historically clamped to 0.01, not
        // reset to 1.0. Preserve that behavior while rejecting NaN/Infinity.
        double multiplier=Double.isFinite(rate)?Math.max(.01d,Math.min(100d,rate)):1d;
        int delta=tickDelta;
        if(delta<0&&!regressionAllowed)delta=0;
        double stored=finiteNonnegative(accumulated)?accumulated:0d;
        double duration=finiteNonnegative(elapsed)?elapsed:0d;
        double total=stored+duration*multiplier;
        if(!Double.isFinite(total))total=Double.MAX_VALUE;
        // Preserve fractional time and clamp the per-call workload; left-over
        // accumulated time is retained for subsequent catch-up passes.
        int ticks=(int)Math.max(0d,Math.min(1_000_000d,Math.floor(total/step)));
        double remainder=Math.max(0d,total-ticks*step);
        long progressed=(long)before+(long)ticks*delta;
        int after=(int)Math.max(0L,Math.min((long)max,progressed));
        return new WorldClockProgression(before,after,ticks,delta,remainder,step,multiplier);
    }
    private static boolean finitePositive(double n){return Double.isFinite(n)&&n>0d;}
    private static boolean finiteNonnegative(double n){return Double.isFinite(n)&&n>=0d;}
}
