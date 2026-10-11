package com.braseiro.dnd5e;

/**
 * RUN202: durable combat receipts stored in effect_receipts, scoped by world and
 * combatant. This pure class can be tested without Android or SQLite.
 */
public final class CombatPersistentReceiptKey {
    private CombatPersistentReceiptKey() {}

    public static String forCombatant(long combatantId, String operationToken) {
        if (combatantId <= 0 || !CombatActionReceiptGuard.validToken(operationToken)) {
            throw new IllegalArgumentException("invalid_persistent_combat_receipt");
        }
        return "combat-v1:" + combatantId + ":" + operationToken;
    }

    /** Map a combatant-scoped receipt to the new id during backup restore. */
    public static String remap(String key, java.util.Map<Long,Long> combatantIds) {
        if (key == null) return null;
        if (key.startsWith("combat-v1-migrated:")) {
            if (combatantIds == null) return null;
            try {
                long oldId = Long.parseLong(key.substring("combat-v1-migrated:".length()));
                Long newId = combatantIds.get(oldId);
                return newId == null || newId.longValue() <= 0 ? null :
                    "combat-v1-migrated:" + newId.longValue();
            } catch (NumberFormatException invalid) { return null; }
        }
        if (!key.startsWith("combat-v1:")) return key;
        if (combatantIds == null) return null;
        int begin = "combat-v1:".length();
        int colon = key.indexOf(':', begin);
        if (colon <= begin || colon >= key.length() - 1) return null;
        try {
            long oldId = Long.parseLong(key.substring(begin, colon));
            Long newId = combatantIds.get(oldId);
            if (newId == null || newId.longValue() <= 0) return null;
            return forCombatant(newId.longValue(), key.substring(colon + 1));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    public static String replayDecision(String persistedSignature, String requestedSignature) {
        return CombatActionReceiptGuard.replayDecision(persistedSignature, requestedSignature);
    }
}
