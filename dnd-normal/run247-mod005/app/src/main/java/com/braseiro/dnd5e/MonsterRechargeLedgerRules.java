package com.braseiro.dnd5e;

import java.util.Locale;

/** G05/RUN232: stable, case-independent identifiers for statblock recharge
 * actions. Lookup in the statblock runtime is already case-insensitive;
 * persisted spend/roll keys must use the same identity to prevent bypass.
 * The original display name and authorized action lookup remain unchanged.
 */
public final class MonsterRechargeLedgerRules {
    private MonsterRechargeLedgerRules() { }
    public static String key(String action) {
        return action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
    }
    public static boolean sameAction(String stored, String requested) {
        String key=key(requested);
        return !key.isEmpty() && key.equals(key(stored));
    }
}
