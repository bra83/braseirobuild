package com.braseiro.dnd5e;

import java.util.Locale;

/** Máquina de estados pura do journal idempotente de cena. */
public final class SceneCommitStateMachine {
    public static final String PREPARED="PREPARED";
    public static final String COMMITTING="COMMITTING";
    public static final String COMMITTED="COMMITTED";
    public static final String DISCARDED="DISCARDED";
    private SceneCommitStateMachine(){}

    public static String normalize(String value){String s=value==null?"":value.trim().toUpperCase(Locale.US);return isKnown(s)?s:PREPARED;}
    public static boolean isKnown(String s){return PREPARED.equals(s)||COMMITTING.equals(s)||COMMITTED.equals(s)||DISCARDED.equals(s);}
    public static boolean terminal(String s){s=normalize(s);return COMMITTED.equals(s)||DISCARDED.equals(s);}
    public static boolean canTransition(String from,String to){
        from=normalize(from);to=normalize(to);
        if(from.equals(to))return true;
        if(PREPARED.equals(from))return COMMITTING.equals(to)||DISCARDED.equals(to);
        if(COMMITTING.equals(from))return COMMITTED.equals(to)||PREPARED.equals(to);
        return false;
    }
    /** Processo morto antes do commit SQLite: somente PREPARED pode sobreviver. */
    public static String recoverAfterProcessDeath(String persisted){String s=normalize(persisted);return COMMITTING.equals(s)?PREPARED:s;}
}
