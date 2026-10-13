package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Mechanics compared against 5etools book-nf.json, Chapter 2,
 * Environmental Effects; source Git blob 4ca6027aa5b5644948d84d1fd1860b0b55a4b523.
 * These checks are independent expected outcomes derived from the source,
 * not tests that merely repeat runtime registry counts.
 */
public final class NetherilChapter2SourceMechanicsTest {
    private final NetherilsFallRulesEngine engine=new NetherilsFallRulesEngine();

    private JSONObject run(String zone,String action,String key,Object value) throws Exception {
        return engine.applyEnvironmentalEffect(zone,new JSONObject().put("operation",action).put(key,value));
    }

    @Test public void testNf_sec_2_1_dampenedSpellDurationRequiresFullMinute() throws Exception {
        JSONObject shortSpell=run("Dampened Magic Zone","spell_duration","base_duration_seconds",30);
        assertTrue(shortSpell.optBoolean("resolved"));
        assertFalse(shortSpell.optBoolean("halving_applies"));
        assertEquals(30d,shortSpell.optDouble("result_duration_seconds"),0.001d);
        JSONObject longSpell=run("Dampened Magic Zone","spell_duration","base_duration_seconds",60);
        assertTrue(longSpell.optBoolean("halving_applies"));
        assertEquals(30d,longSpell.optDouble("result_duration_seconds"),0.001d);
        JSONObject tenMinutes=run("Dampened Magic Zone","spell_duration","base_duration_seconds",600);
        assertEquals(300d,tenMinutes.optDouble("result_duration_seconds"),0.001d);
        assertFalse(engine.applyEnvironmentalEffect("Dampened Magic Zone",
                new JSONObject().put("operation","spell_duration")).optBoolean("resolved"));
    }

    @Test public void testNf_sec_2_1_empoweredForceDamageDieMinimumThree() throws Exception {
        for(int roll=1;roll<=8;roll++) {
            JSONObject result=engine.applyEnvironmentalEffect("Empowered Force Zone",
                new JSONObject().put("operation","force_damage_die").put("damage_type","Force")
                        .put("die_sides",8).put("rolled_damage_die",roll));
            assertTrue(result.optBoolean("resolved"));
            assertEquals(Math.max(3,roll),result.optInt("adjusted_damage_die"));
            assertFalse(result.optBoolean("auto_rolls",true));
        }
        JSONObject wrongType=engine.applyEnvironmentalEffect("Empowered Force Zone",
                new JSONObject().put("operation","force_damage_die").put("damage_type","Fire")
                        .put("die_sides",8).put("rolled_damage_die",1));
        assertFalse(wrongType.optBoolean("resolved"));
        assertFalse(run("Empowered Force Zone","force_damage_die","rolled_damage_die",1).optBoolean("resolved"));
    }

    @Test public void testNf_sec_2_1_lowGravityJumpAndFallingDamage() throws Exception {
        JSONObject jump=run("Low Gravity Zone","jump_distance","base_jump_distance_ft",12);
        assertTrue(jump.optBoolean("resolved"));
        assertEquals(48d,jump.optDouble("jump_distance_ft"),0.001d);
        assertEquals(2,run("Low Gravity Zone","fall_damage","rolled_fall_damage",5)
                .optInt("fall_damage_after_gravity"));
        assertEquals(3,run("Low Gravity Zone","fall_damage","rolled_fall_damage",6)
                .optInt("fall_damage_after_gravity"));
        assertFalse(run("Low Gravity Zone","fall_damage","rolled_fall_damage",-1)
                .optBoolean("resolved"));
    }

    @Test public void testNf_sec_2_1_telepathyStartsAtEightIntelligence() throws Exception {
        assertEquals(0,run("Communal Telepathy Zone","telepathy","intelligence_score",7)
                .optInt("telepathy_range_ft"));
        assertEquals(30,run("Communal Telepathy Zone","telepathy","intelligence_score",8)
                .optInt("telepathy_range_ft"));
        assertFalse(run("Communal Telepathy Zone","telepathy","intelligence_score",0)
                .optBoolean("resolved"));
    }

    @Test public void testNf_sec_2_1_gatewayDoesNotCommitWorldState() throws Exception {
        OfficialBookRuntimeGateway gateway=new OfficialBookRuntimeGateway(null);
        JSONObject ok=gateway.execute("nf","apply_environmental_effect",new JSONObject()
                .put("name","Dampened Magic Zone").put("operation","spell_duration")
                .put("base_duration_seconds",120));
        assertTrue(ok.optBoolean("executed"));
        assertFalse(ok.optBoolean("persisted",true));
        assertFalse(ok.optBoolean("auto_mutation_allowed",true));
        assertEquals(60d,ok.getJSONObject("result").optDouble("result_duration_seconds"),0.001d);
        JSONObject invalid=gateway.execute("nf","apply_environmental_effect",
                new JSONObject().put("name","Dampened Magic Zone"));
        assertFalse(invalid.optBoolean("executed"));
        assertEquals("blocked",invalid.optString("status"));
    }
}
