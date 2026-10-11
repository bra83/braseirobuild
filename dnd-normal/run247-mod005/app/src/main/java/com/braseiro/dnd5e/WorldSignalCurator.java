package com.braseiro.dnd5e;

/** Deterministic priority policy for selecting a living-world signal. */
public final class WorldSignalCurator {
    private WorldSignalCurator() {}

    /** Relevance outranks urgency when an otherwise unrelated signal is hot. */
    public static int priority(int pressure, int relevance) {
        int p = Math.max(0, Math.min(100, pressure));
        int r = Math.max(0, Math.min(35, relevance));
        return r * 2 + p;
    }
}
