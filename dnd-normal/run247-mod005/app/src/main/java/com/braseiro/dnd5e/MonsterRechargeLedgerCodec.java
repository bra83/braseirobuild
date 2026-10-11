package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Iterator;

/** G05/RUN232: persistent recharge JSON key compatibility.
 * Read historical aliases case-insensitively and conservatively; normalize
 * them on mutation without dropping unrelated actions. Callers write the
 * resulting resources_json inside their existing combatAtomic transaction.
 */
public final class MonsterRechargeLedgerCodec {
    private MonsterRechargeLedgerCodec() { }
    public static boolean spent(JSONObject ledger,String action) {
        if(ledger==null)return false;
        Iterator<String> keys=ledger.keys();
        while(keys.hasNext()) {
            String stored=keys.next();
            if(MonsterRechargeLedgerRules.sameAction(stored,action)) {
                Object value=ledger.opt(stored);
                // Corrupt spend metadata is NOT a license to use the action.
                // Only explicit false (or its historical string encoding) clears it.
                if(Boolean.FALSE.equals(value)||"false".equalsIgnoreCase(String.valueOf(value)))continue;
                return true;
            }
        }
        return false;
    }
    public static int latestRound(JSONObject ledger,String action) {
        if(ledger==null)return 0;
        int newest=0;Iterator<String> keys=ledger.keys();
        while(keys.hasNext()) {
            String stored=keys.next();
            if(MonsterRechargeLedgerRules.sameAction(stored,action)) {
                Object value=ledger.opt(stored);
                if(!(value instanceof Number))return Integer.MAX_VALUE;
                Number n=(Number)value;
                double d=n.doubleValue();
                if(!Double.isFinite(d)||d<0||d>Integer.MAX_VALUE||Math.rint(d)!=d)
                    return Integer.MAX_VALUE;
                newest=Math.max(newest,n.intValue());
            }
        }
        return newest;
    }
    public static void write(JSONObject ledger,String action,Object value)throws JSONException {
        String canonical=MonsterRechargeLedgerRules.key(action);
        if(ledger==null||canonical.isEmpty())throw new IllegalArgumentException("invalid_recharge_action");
        ArrayList<String> aliases=new ArrayList<String>();
        Iterator<String> keys=ledger.keys();
        while(keys.hasNext()) {
            String stored=keys.next();
            if(MonsterRechargeLedgerRules.sameAction(stored,action))aliases.add(stored);
        }
        for(String alias:aliases)ledger.remove(alias);
        ledger.put(canonical,value);
    }
}
