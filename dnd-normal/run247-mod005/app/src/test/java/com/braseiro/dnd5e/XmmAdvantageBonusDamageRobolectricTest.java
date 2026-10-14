package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmAdvantageBonusDamageRobolectricTest {
    @Test public void sixSourceAttacksAddBonusDiceOnlyWhenAttackHadAdvantage() {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        String[][] cases={
            {"Goblin Boss","Scimitar","1d6 + 2","1d4","slashing"},
            {"Goblin Boss","Shortbow","1d6 + 2","1d4","piercing"},
            {"Goblin Warrior","Scimitar","1d6 + 2","1d4","slashing"},
            {"Goblin Warrior","Shortbow","1d6 + 2","1d4","piercing"},
            {"Scout Captain","Shortsword","1d6 + 3","3d6","piercing"},
            {"Scout Captain","Longbow","1d8 + 3","3d6","piercing"},
        };
        for(String[] row:cases){
            JSONObject ordinary=StatblockCombatRuntimeEngine.attack(
                row[0],row[1],15,1,20,40,false,false);
            assertTrue(row[0]+"/"+row[1],ordinary.optBoolean("resolved",false));
            assertEquals(row[2],ordinary.optString("damage_expression"));
            assertEquals("",ordinary.optString("extra_damage_expression"));

            JSONObject advantaged=StatblockCombatRuntimeEngine.attack(
                row[0],row[1],15,1,20,40,false,true);
            assertTrue(row[0]+"/"+row[1],advantaged.optBoolean("resolved",false));
            assertEquals(row[2],advantaged.optString("damage_expression"));
            assertEquals(row[3],advantaged.optString("extra_damage_expression"));
            assertEquals(row[4],advantaged.optString("extra_damage_type"));
            assertTrue(advantaged.optBoolean("attack_had_advantage",false));
        }
    }

    @Test public void advantageBonusActionsAreNoLongerBlockedAsUntyped() {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        for(String[] action:new String[][]{
            {"Goblin Boss","Scimitar"},{"Goblin Boss","Shortbow"},
            {"Goblin Warrior","Scimitar"},{"Goblin Warrior","Shortbow"},
            {"Scout Captain","Shortsword"},{"Scout Captain","Longbow"}}){
            JSONObject profile=CompiledStatblockEngine.runtimeProfile(action[0]);
            JSONObject compiled=null;
            for(int i=0;i<profile.optJSONArray("actions").length();i++){
                JSONObject candidate=profile.optJSONArray("actions").optJSONObject(i);
                if(candidate!=null&&action[1].equals(candidate.optString("name")))compiled=candidate;
            }
            assertTrue(action[0]+"/"+action[1],compiled!=null);
            assertFalse(action[0]+"/"+action[1],compiled.optBoolean(
                "manual_unmodeled_source_effect_required",false));
        }
    }

    @Test public void databaseAttackUsesLiveRollModeForConditionalBonusDamage()throws Exception {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_adv_damage_"+Long.toHexString(System.nanoTime())+".db";
        java.io.File directory=context.getDatabasePath(dbName).getParentFile();
        assertNotNull(directory);assertTrue(directory.isDirectory()||directory.mkdirs());
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            long goblin=db.addCombatant("Goblin Boss",20,21,21,17,"",false);
            long target=db.addCombatant("Prone Target",10,30,30,10,"prone",true);
            JSONObject resources=new JSONObject(db.getCombatant(goblin).optString("resources_json","{}"));
            resources.put("block_id","Goblin Boss");
            ContentValues binding=new ContentValues();binding.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",binding,
                "id=? AND world_profile=?",new String[]{String.valueOf(goblin),"dnd"}));
            db.setCombatRunning(true);assertEquals(goblin,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();
            position(db,goblin,2,2);position(db,target,3,2);

            JSONObject action=null;
            JSONArray actions=CompiledStatblockEngine.runtimeProfile("Goblin Boss").optJSONArray("actions");
            for(int i=0;actions!=null&&i<actions.length();i++){
                JSONObject row=actions.optJSONObject(i);
                if(row!=null&&"Scimitar".equals(row.optString("name")))action=row;
            }
            assertNotNull(action);
            JSONArray specs=StatblockCombatFlowEngine.damageSpecs(action.optString("damage"),
                action.optString("damage_type"),action.optString("extra_damage_if_attack_advantage"),
                action.optString("extra_damage_if_attack_advantage_type"));
            JSONArray rolls=new JSONArray();int expectedTotal=0;
            for(int i=0;i<specs.length();i++){
                JSONObject spec=specs.getJSONObject(i);
                CompiledMonsterDamageGuard.Range range=CompiledMonsterDamageGuard.range(
                    spec.optString("expression"),"",false);
                assertNotNull(range);expectedTotal+=range.min;
                rolls.put(new JSONObject().put("expression",spec.optString("expression"))
                    .put("damage_type",spec.optString("type")).put("damage_total",range.min));
            }
            JSONObject result=db.applyCompiledStatblockAttack(goblin,target,"Goblin Boss","Scimitar",
                15,10,rolls,0,0L,-1,-1,"ui-monster-goblin-advantage-bonus");
            assertTrue(result.toString(),result.optBoolean("statblock_attack_committed",false));
            JSONObject resolved=result.getJSONObject("resolved");
            assertTrue(resolved.toString(),resolved.optBoolean("attack_had_advantage",false));
            assertEquals("1d4",resolved.optString("extra_damage_expression"));
            assertEquals(expectedTotal,result.optInt("damage_total"));
            assertEquals(30-expectedTotal,db.getCombatant(target).optInt("hp"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
}
