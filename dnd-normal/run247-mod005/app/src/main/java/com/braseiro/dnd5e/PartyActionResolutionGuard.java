package com.braseiro.dnd5e;

import java.util.Locale;
/** Pure JVM authority for queued player declarations. Narration alone is never execution. */
public final class PartyActionResolutionGuard {
    private PartyActionResolutionGuard() {}
    public static boolean transitionAllowed(String current,String requested){
        if(!"narrated".equals(current))return false;
        return "resolved".equals(requested)||"retry".equals(requested)||"cancelled".equals(requested)||"narrative_only".equals(requested);
    }
    public static boolean receiptAllowed(String key,String source,long receiptAt,long actionAt,boolean exists){
        if(!exists||key==null||key.trim().length()<8||source==null||source.trim().isEmpty())return false;
        String s=source.toLowerCase(Locale.ROOT),k=key.toLowerCase(Locale.ROOT);
        if(s.startsWith("migra")||s.contains("legacy_ring_migrated")||k.startsWith("party-action:"))return false;
        return receiptAt>0&&actionAt>0&&receiptAt>=actionAt;
    }
    public static boolean dismissalAllowed(String note){return note!=null&&note.trim().length()>=8;}
    public static boolean narrativeOnlyAllowed(String intent,String note){
        // Combat, movement and rest cannot be declared mechanically completed without receipts.
        if("combate".equals(intent)||"movimento".equals(intent)||"descanso".equals(intent)||"curated_approved".equals(intent))return false;
        return dismissalAllowed(note);
    }
}
