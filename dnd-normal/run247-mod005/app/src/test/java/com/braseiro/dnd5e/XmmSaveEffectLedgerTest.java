package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real JSON serialization; no mock of the effect executor. */
public final class XmmSaveEffectLedgerTest {
    private static final long TARGET=2;
    private static JSONObject action(String block)throws Exception {
        return new JSONObject().put("name","Slowing Breath").put("category","action")
            .put("save_ability","con").put("save_dc",XmmSaveEffectLedger.dc(block))
            .put("save_effect_kind",XmmSaveEffectLedger.SLOW)
            .put("save_effect_duration","target_next_turn_end")
            .put("target_area_shape","cone").put("target_range_ft",XmmSaveEffectLedger.cone(block))
            .put("manual_rider_resolution_required",false).put("manual_unmodeled_source_effect_required",false);
    }
    private static JSONObject resources()throws Exception {
        JSONObject r=new JSONObject().put("action_available",true).put("bonus_action_available",true);
        r.put(XmmSaveEffectLedger.FIELD,XmmSaveEffectLedger.stage(r,1,TARGET,"Adult Copper Dragon",
            action("Adult Copper Dragon"),"ui-monster-save-slow-test","enc-test",1,0));return r;
    }
    private interface Work { void run()throws Exception; }
    private static void rejected(Work work)throws Exception {
        boolean failed=false;try{work.run();}catch(IllegalArgumentException expected){failed=true;}
        assertTrue(failed);
    }
    @Test public void fourSourceVariantsAndNoOtherCreature()throws Exception {
        String[] blocks={"Adult Copper Dragon","Ancient Copper Dragon","Copper Dragon Wyrmling","Young Copper Dragon"};
        int[] dcs={18,22,11,14},cones={60,90,15,30};
        for(int i=0;i<blocks.length;i++){
            assertEquals(dcs[i],XmmSaveEffectLedger.dc(blocks[i]));assertEquals(cones[i],XmmSaveEffectLedger.cone(blocks[i]));
            assertTrue(XmmSaveEffectLedger.compiled(blocks[i],action(blocks[i])));
        }
        assertFalse(XmmSaveEffectLedger.compiled("Adult Brass Dragon",action(blocks[0])));
        assertFalse(XmmSaveEffectLedger.compiled("Dracolich",action(blocks[0])));
    }
    @Test public void changedSourceFieldsAreNotExecutable()throws Exception {
        JSONObject a=action("Adult Copper Dragon");a.put("save_dc",19);
        assertFalse(XmmSaveEffectLedger.compiled("Adult Copper Dragon",a));
        a=action("Adult Copper Dragon");a.put("save_dc",18.5);
        assertFalse(XmmSaveEffectLedger.compiled("Adult Copper Dragon",a));
        a=action("Adult Copper Dragon");a.put("recharge","5-6");
        assertFalse(XmmSaveEffectLedger.compiled("Adult Copper Dragon",a));
        a=action("Adult Copper Dragon");a.put("condition_on_fail","slowed");
        assertFalse(XmmSaveEffectLedger.compiled("Adult Copper Dragon",a));
        a=action("Adult Copper Dragon");a.put("save_effect_duration","one_minute");
        assertFalse(XmmSaveEffectLedger.compiled("Adult Copper Dragon",a));
    }
    @Test public void slowsSpeedAndBlocksOnlySourceDeclaredEconomy()throws Exception {
        JSONObject r=resources();assertEquals(15,XmmSaveEffectLedger.speed(r,TARGET,30));
        assertEquals(12,XmmSaveEffectLedger.speed(r,TARGET,25));
        assertEquals("slowing_breath_no_reactions",XmmSaveEffectLedger.spendBlock(r,TARGET,"reaction"));
        assertEquals("",XmmSaveEffectLedger.spendBlock(r,TARGET,"action"));
        assertEquals("",XmmSaveEffectLedger.spendBlock(r,TARGET,"bonus_action"));
        r.put("action_available",false);
        assertEquals("slowing_breath_action_or_bonus",XmmSaveEffectLedger.spendBlock(r,TARGET,"bonus_action"));
        r.put("action_available",true).put("bonus_action_available",false);
        assertEquals("slowing_breath_action_or_bonus",XmmSaveEffectLedger.spendBlock(r,TARGET,"action"));
    }
    @Test public void effectExpiresAtEndOfNextTargetTurn()throws Exception {
        JSONObject r=resources();assertEquals(0,XmmSaveEffectLedger.advance(r,TARGET,"enc-test",1,1).length());
        r.put(XmmSaveEffectLedger.FIELD,new JSONArray());
        assertEquals(30,XmmSaveEffectLedger.speed(r,TARGET,30));
        assertEquals("",XmmSaveEffectLedger.spendBlock(r,TARGET,"reaction"));
    }
    @Test public void currentTargetTurnEndDoesNotExpireNextTurnEffect()throws Exception {
        JSONObject r=resources();
        assertEquals(1,XmmSaveEffectLedger.advance(r,TARGET,"enc-test",1,0).length());
        assertEquals(0,XmmSaveEffectLedger.advance(r,TARGET,"enc-test",2,0).length());
    }
    @Test public void expirationReplayIsIdempotent()throws Exception {
        JSONObject r=resources();r.put(XmmSaveEffectLedger.FIELD,XmmSaveEffectLedger.advance(r,TARGET,"enc-test",1,1));
        assertEquals(0,XmmSaveEffectLedger.advance(r,TARGET,"enc-test",1,1).length());
    }
    @Test public void activationReplayRejectsWithoutMutating()throws Exception {
        final JSONObject r=resources();final String before=r.toString();
        rejected(new Work(){public void run()throws Exception {XmmSaveEffectLedger.stage(r,1,TARGET,
            "Adult Copper Dragon",action("Adult Copper Dragon"),"ui-monster-save-slow-test","enc-test",1,0);}});
        assertEquals(before,r.toString());
    }
    @Test public void overlappingOriginsDoNotQuarterSpeedOrExpireTogether()throws Exception {
        JSONObject r=resources();r.put(XmmSaveEffectLedger.FIELD,XmmSaveEffectLedger.stage(r,3,TARGET,
            "Young Copper Dragon",action("Young Copper Dragon"),"ui-monster-save-other-test","enc-test",1,1));
        assertEquals(15,XmmSaveEffectLedger.speed(r,TARGET,30));
        r.put(XmmSaveEffectLedger.FIELD,XmmSaveEffectLedger.advance(r,TARGET,"enc-test",1,1));
        assertEquals(1,XmmSaveEffectLedger.read(r,TARGET).length());assertEquals(15,XmmSaveEffectLedger.speed(r,TARGET,30));
    }
    @Test public void serializationPreservesOwnershipAndRestrictions()throws Exception {
        JSONObject reopened=new JSONObject(resources().toString());
        assertEquals(15,XmmSaveEffectLedger.speed(reopened,TARGET,30));
        assertEquals(1L,XmmSaveEffectLedger.read(reopened,TARGET).getJSONObject(0).getLong("source_actor_id"));
    }
    @Test public void backupRemapsBothIdsAndPreservesDuration()throws Exception {
        JSONObject r=resources();java.util.Map<Long,Long> ids=new java.util.HashMap<Long,Long>();ids.put(1L,101L);ids.put(2L,102L);
        XmmSaveEffectLedger.remap(r,TARGET,ids);
        JSONObject row=XmmSaveEffectLedger.read(r,102).getJSONObject(0);
        assertEquals(101L,row.getLong("source_actor_id"));assertEquals(102L,row.getLong("target_id"));
        assertEquals("enc-test",row.getString("encounter_id"));
        assertEquals(0,XmmSaveEffectLedger.advance(r,102,"enc-test",1,1).length());
    }
    @Test public void orphanBackupAndWrongEncounterReject()throws Exception {
        final JSONObject r=resources();final java.util.Map<Long,Long> ids=new java.util.HashMap<Long,Long>();ids.put(2L,102L);
        rejected(new Work(){public void run()throws Exception {XmmSaveEffectLedger.remap(r,TARGET,ids);}});
        rejected(new Work(){public void run()throws Exception {XmmSaveEffectLedger.advance(r,TARGET,"wrong-encounter",1,1);}});
    }
    @Test public void corruptPersistedStateFailsClosed()throws Exception {
        final JSONObject r=resources();XmmSaveEffectLedger.read(r,TARGET).getJSONObject(0).put("applied_round",1.5);
        rejected(new Work(){public void run()throws Exception {XmmSaveEffectLedger.speed(r,TARGET,30);}});
        final JSONObject wrong=new JSONObject().put(XmmSaveEffectLedger.FIELD,"[]");
        rejected(new Work(){public void run()throws Exception {XmmSaveEffectLedger.read(wrong,TARGET);}});
    }
    @Test public void actualInstalledProfilesResolveSavesAndKeepOtherBreathsManual()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        for(String block:new String[]{"Adult Copper Dragon","Ancient Copper Dragon","Copper Dragon Wyrmling","Young Copper Dragon"}) {
            JSONObject failed=StatblockCombatRuntimeEngine.savingThrowAction(block,"Slowing Breath",1);
            assertTrue(failed.optBoolean("resolved"));assertFalse(failed.optBoolean("save_success"));
            assertEquals(XmmSaveEffectLedger.SLOW,failed.optString("save_effect_kind"));
            assertEquals("",failed.optString("condition_on_fail"));assertEquals("",failed.optString("recharge"));
            JSONObject saved=StatblockCombatRuntimeEngine.savingThrowAction(block,"Slowing Breath",XmmSaveEffectLedger.dc(block));
            assertTrue(saved.optBoolean("save_success"));assertEquals("",saved.optString("save_effect_kind"));
            for(int low:new int[]{0,-4}){
                JSONObject lowSave=StatblockCombatRuntimeEngine.savingThrowAction(block,"Slowing Breath",low);
                assertTrue(lowSave.optBoolean("resolved"));assertFalse(lowSave.optBoolean("save_success"));
            }
        }
        assertEquals("manual_source_action_resolution_required",StatblockCombatRuntimeEngine.savingThrowAction(
            "Adult Brass Dragon","Sleep Breath",1).optString("reason"));
        assertEquals("manual_source_action_resolution_required",StatblockCombatRuntimeEngine.savingThrowAction(
            "Adult Silver Dragon","Paralyzing Breath",1).optString("reason"));
        assertEquals("manual_source_action_resolution_required",StatblockCombatRuntimeEngine.savingThrowAction(
            "Adult Gold Dragon","Weakening Breath",1).optString("reason"));
    }
    @Test public void nestedReceiptsRemapEffectOwnersAndTargetKeys()throws Exception {
        JSONObject proof=new JSONObject().put("schema",XmmSaveEffectLedger.SCHEMA).put("target_id",TARGET)
            .put("effects_after",XmmSaveEffectLedger.read(resources(),TARGET));
        JSONObject outer=new JSONObject().put("resolved",new JSONObject().put("save_effect_transition",proof));
        java.util.Map<Long,Long> ids=new java.util.HashMap<Long,Long>();ids.put(1L,101L);ids.put(2L,102L);
        JSONObject rewritten=new JSONObject(MonsterConditionBackupCodec.remapReceiptSource(outer.toString(),ids))
            .getJSONObject("resolved").getJSONObject("save_effect_transition");
        assertEquals(102L,rewritten.getLong("target_id"));
        assertEquals(101L,rewritten.getJSONArray("effects_after").getJSONObject(0).getLong("source_actor_id"));
        assertEquals("xmm-save-effect-turn-v1:enc-test:1:1:102",
            MonsterConditionBackupCodec.remapReceiptKey("xmm-save-effect-turn-v1:enc-test:1:1:2",ids));
    }
}
