package com.braseiro.dnd5e;

import java.util.Iterator;

/** Fail-closed result contract for atomic combat operations.
 * In particular a rejection delivered as a normal JSONObject return must not
 * cause setTransactionSuccessful(). The predicate is JVM-testable without Android.
 */
public final class CombatAtomicCommitGuard {
    private CombatAtomicCommitGuard() { }
    public interface Fields {
        Iterator<String> keys();
        boolean isTrue(String key);
    }
    public static String rejection(Fields fields) {
        if(fields==null)return "missing_result";
        Iterator<String> keys=fields.keys();
        if(keys==null)return "missing_result_keys";
        while(keys.hasNext()) {
            String key=keys.next();
            if(key==null)continue;
            // Namespaced blocks are used by monster, geometry, party, saving
            // throw, movement, healing, and action-economy executors.
            if(key.endsWith("_blocked")||key.endsWith("_error")||"operation_error".equals(key))
                return key;
            if((key.endsWith("_replayed")||"replayed".equals(key)) && fields.isTrue(key))
                return key;
            if("accepted".equals(key) && !fields.isTrue(key))
                return "action_not_accepted";
        }
        return "";
    }
}
