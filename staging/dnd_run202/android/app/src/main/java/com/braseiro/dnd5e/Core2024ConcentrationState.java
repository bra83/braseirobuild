package com.braseiro.dnd5e;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable concentration snapshot; persist with the character and transaction receipt. */
public final class Core2024ConcentrationState {
    public enum End { NONE, REPLACED, FAILED_SAVE, VOLUNTARY, INCAPACITATED, DEAD, EXPIRED }
    public final String casterId, spellId;
    public final long revision;
    public final End lastEnd;
    public final Set<String> processedEvents;

    public Core2024ConcentrationState(String casterId, String spellId, long revision,
                                      End lastEnd, Set<String> processedEvents) {
        if (casterId == null || casterId.trim().isEmpty() || revision < 0 ||
            lastEnd == null || processedEvents == null) {
            throw new IllegalArgumentException("invalid concentration state");
        }
        for (String id : processedEvents) {
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("invalid receipt");
        }
        this.casterId = casterId;
        this.spellId = spellId;
        this.revision = revision;
        this.lastEnd = lastEnd;
        this.processedEvents = Collections.unmodifiableSet(
            new LinkedHashSet<String>(processedEvents));
    }

    public static Core2024ConcentrationState initial(String casterId) {
        return new Core2024ConcentrationState(casterId, null, 0L, End.NONE,
            Collections.<String>emptySet());
    }
    public boolean active() { return spellId != null; }
}
