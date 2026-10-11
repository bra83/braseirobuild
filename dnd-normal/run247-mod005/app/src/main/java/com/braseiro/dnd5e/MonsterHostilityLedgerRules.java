package com.braseiro.dnd5e;

import java.util.Arrays;

/** G05/RUN230. Explicit directed hostility, never inferred from names, faction or is_player.
 * The caller resolves IDs against the active SQLite world before committing.
 * Immutable sorted IDs make a saved declaration deterministic and replay-safe. */
public final class MonsterHostilityLedgerRules {
    public static final int MAX_HOSTILES=128;
    private MonsterHostilityLedgerRules() { }
    public static long[] canonical(long actorId,long[] declared) {
        if(actorId<=0||declared==null||declared.length>MAX_HOSTILES)
            throw new IllegalArgumentException("invalid_hostility_ledger_size_or_actor");
        long[] result=declared.clone();
        Arrays.sort(result);
        for(int i=0;i<result.length;i++) {
            if(result[i]<=0||result[i]==actorId)
                throw new IllegalArgumentException("invalid_hostility_ledger_id");
            if(i>0&&result[i]==result[i-1])
                throw new IllegalArgumentException("duplicate_hostility_ledger_id");
        }
        return result;
    }
    public static boolean contains(long[] canonicalIds,long otherId) {
        return canonicalIds!=null&&otherId>0&&Arrays.binarySearch(canonicalIds,otherId)>=0;
    }
    public static boolean identical(long[] first,long[] second) {
        return Arrays.equals(first,second);
    }
}
