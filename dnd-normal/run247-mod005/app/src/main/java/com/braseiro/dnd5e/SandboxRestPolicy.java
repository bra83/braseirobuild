package com.braseiro.dnd5e;

/**
 * Mechanical preconditions and non-punitive reminders for campaign rest.
 * Actual resource recovery belongs to the source-bound character engines.
 * This policy never applies exhaustion or invents penalties.
 */
public final class SandboxRestPolicy {
    private SandboxRestPolicy(){}

    public static boolean validLong(boolean requested,double elapsedHours){
        return requested&&Double.isFinite(elapsedHours)&&elapsedHours>=8d;
    }
    public static boolean validShort(boolean requested,double elapsedHours,boolean longRest){
        return requested&&!longRest&&Double.isFinite(elapsedHours)&&elapsedHours>=1d;
    }
    public static double hoursSinceLongRest(double lastCompletion,double afterAbsolute,boolean restCompleted){
        if(restCompleted)return 0d;
        if(!Double.isFinite(lastCompletion)||!Double.isFinite(afterAbsolute))return 0d;
        return Math.max(0d,afterAbsolute-lastCompletion);
    }
    public static boolean recommended(double hours){return Double.isFinite(hours)&&hours>=16d;}
    public static boolean overdue(double hours){return Double.isFinite(hours)&&hours>=24d;}
}