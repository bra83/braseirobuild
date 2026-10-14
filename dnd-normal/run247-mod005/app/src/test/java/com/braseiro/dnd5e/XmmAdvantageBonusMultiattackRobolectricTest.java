package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmAdvantageBonusMultiattackRobolectricTest {
    @Test public void sqliteMultiattackRevalidatesSourceAdvantageForEachLiveComponent()throws Exception {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_multi_adv_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            long goblin=db.addCombatant("Goblin Boss",20,21,21,17,"",false);
            long target=db.addCombatant("Prone Target",10,30,30,1,"prone",true);
            JSONObject resources=new JSONObject(db.getCombatant(goblin).optString("resources_json","{}"));
            resources.put("block_id","Goblin Boss");
            ContentValues binding=new ContentValues();binding.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",binding,
                "id=? AND world_profile=?",new String[]{String.valueOf(goblin),"dnd"}));
            db.setCombatRunning(true);assertEquals(goblin,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();
            position(db,goblin,2,2);position(db,target,3,2);

            JSONArray rolls=new JSONArray();int expectedDamage=0;
            for(int i=0;i<2;i++){
                JSONArray specs=StatblockCombatFlowEngine.damageSpecs("1d6 + 2","slashing","1d4","slashing");
                JSONArray components=new JSONArray();
                for(int j=0;j<specs.length();j++){
                    JSONObject spec=specs.getJSONObject(j);
                    CompiledMonsterDamageGuard.Range range=CompiledMonsterDamageGuard.range(
                        spec.optString("expression"),"",false);
                    assertNotNull(range);expectedDamage+=range.min;
                    components.put(new JSONObject().put("expression",spec.optString("expression"))
                        .put("damage_type",spec.optString("type")).put("damage_total",range.min));
                }
                rolls.put(new JSONObject().put("action","Scimitar").put("target_id",target)
                    .put("natural",15).put("long_range_second_d20",5).put("rider_save_roll",0)
                    .put("damage_components",components));
            }
            JSONObject result=db.applyCompiledStatblockMultiattack(goblin,"Goblin Boss",rolls,
                "ui-monster-multi-goblin-advantage-revalidation");
            assertTrue(result.toString(),result.optBoolean("statblock_multiattack_committed",false));
            JSONArray outcomes=result.getJSONArray("components");assertEquals(2,outcomes.length());
            for(int i=0;i<outcomes.length();i++){
                JSONObject attack=outcomes.getJSONObject(i).getJSONObject("resolved");
                assertTrue(attack.toString(),attack.optBoolean("roll_advantage",false));
                assertEquals("1d4",attack.optString("extra_damage_expression"));
            }
            assertEquals(30-expectedDamage,db.getCombatant(target).optInt("hp"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
}
