package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** XMM usage gates reject forged resource counts and coerced recharge results. */
public final class XmmSpellUsageStrictInputTest {
    @Test public void dailyAndRestUsageMustBeRealIntegersWithinPinnedLimits() throws Exception {
        JSONArray names=Xmm2025CompiledBestiaryEngine.catalog().getJSONArray("names");
        int verified=0;
        for(int i=0;i<names.length();i++) {
            String monster=names.getString(i);
            JSONObject profile=Xmm2025SpellUsageIndex.options(monster);
            if(!profile.optBoolean("resolved"))continue;
            JSONArray entries=profile.getJSONArray("entries");
            for(int j=0;j<entries.length();j++) {
                JSONObject entry=entries.getJSONObject(j);
                for(String usage:new String[]{"daily","restLong","legendary"}) {
                    JSONObject limits=entry.getJSONObject(usage);
                    for(java.util.Iterator<String> keys=limits.keys();keys.hasNext();) {
                        String key=keys.next();
                        int max=Integer.parseInt(key.endsWith("e")?key.substring(0,key.length()-1):key);
                        JSONArray spells=limits.getJSONArray(key);
                        for(int k=0;k<spells.length();k++) {
                            String spell=spells.getString(k);
                            JSONObject request=new JSONObject().put("monster",monster).put("spell",spell);
                            JSONObject valid=Xmm2025SpellUsageIndex.check(
                                request.put("uses_remaining",max));
                            assertTrue(monster+" / "+spell+": "+valid,valid.optBoolean("resolved"));
                            assertTrue(monster+" / "+spell,valid.optBoolean("may_attempt"));
                            for(Object invalid:new Object[]{max+1,-1,"1",1.0,Boolean.TRUE}) {
                                JSONObject blocked=Xmm2025SpellUsageIndex.check(
                                    request.put("uses_remaining",invalid));
                                assertFalse(monster+" / "+spell+" / "+invalid+": "+blocked,
                                    blocked.optBoolean("resolved"));
                                assertEquals("invalid_remaining_uses",blocked.optString("reason"));
                            }
                            verified++;
                        }
                    }
                }
            }
        }
        assertTrue("Expected source-limited XMM spells",verified>0);
    }

    @Test public void rechargeRequiresExplicitBooleanWithoutHiddenDice() throws Exception {
        JSONObject request=new JSONObject().put("monster","Pit Fiend").put("spell","Fireball");
        JSONObject pending=Xmm2025SpellUsageIndex.check(request);
        assertTrue(pending.toString(),pending.optBoolean("resolved"));
        assertEquals("manual_recharge_status_required",pending.optString("status"));
        assertTrue(pending.isNull("may_attempt"));
        for(Object invalid:new Object[]{"true",1,JSONObject.NULL}) {
            JSONObject blocked=Xmm2025SpellUsageIndex.check(
                request.put("recharge_ready",invalid));
            assertFalse(blocked.toString(),blocked.optBoolean("resolved"));
            assertEquals("invalid_recharge_ready_type",blocked.optString("reason"));
        }
        JSONObject ready=Xmm2025SpellUsageIndex.check(request.put("recharge_ready",true));
        assertTrue(ready.toString(),ready.optBoolean("may_attempt"));
        assertFalse(ready.optBoolean("auto_rolls"));
        assertFalse(ready.optBoolean("auto_mutation_allowed"));
        assertFalse(ready.optBoolean("spell_effects_applied"));
        JSONObject spent=Xmm2025SpellUsageIndex.check(request.put("recharge_ready",false));
        assertFalse(spent.optBoolean("may_attempt"));
    }
}
