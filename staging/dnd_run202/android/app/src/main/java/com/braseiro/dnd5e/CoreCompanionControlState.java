package com.braseiro.dnd5e;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Persistent control ownership; all fields are scoped to one character. */
public final class CoreCompanionControlState {
    public enum Mode { PLAYER, GM, GM_CURATED }
    public final String characterId;
    public final Mode mode;
    public final long revision;
    public final Map<String,String> pending;
    public final Set<String> receipts;

    public CoreCompanionControlState(String characterId,Mode mode,long revision,
                                     Map<String,String> pending,Set<String> receipts) {
        if(characterId==null||characterId.trim().isEmpty()||mode==null||revision<0||
           pending==null||receipts==null) throw new IllegalArgumentException("invalid control state");
        this.characterId=characterId;this.mode=mode;this.revision=revision;
        this.pending=Collections.unmodifiableMap(new LinkedHashMap<String,String>(pending));
        this.receipts=Collections.unmodifiableSet(new LinkedHashSet<String>(receipts));
    }
    public static CoreCompanionControlState initial(String characterId,Mode mode) {
        return new CoreCompanionControlState(characterId,mode,0L,
            Collections.<String,String>emptyMap(),Collections.<String>emptySet());
    }
}
