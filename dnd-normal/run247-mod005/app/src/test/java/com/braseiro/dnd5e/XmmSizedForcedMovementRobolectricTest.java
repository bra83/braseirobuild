package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Source size limits govern on-hit push/pull without suppressing legal damage. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmSizedForcedMovementRobolectricTest {
    private static void bind(AppDb db,long id,String block)throws Exception{
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        resources.put("block_id",block);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }
    private static void position(AppDb db,long id,int x,int y)throws Exception{
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void runCase(String actorBlock,String action,String targetBlock,
        int actorX,int targetX,int destinationX,int damage,boolean expectedMove,boolean unknownSize,String token)throws Exception{
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_size_force_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,9)+".db";
        java.io.File directory=context.getDatabasePath(dbName).getParentFile();
        assertNotNull(directory);assertTrue(directory.isDirectory()||directory.mkdirs());
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("Source "+token,20,150,150,15,"",false);
            target=db.addCombatant("Target "+token,10,100,100,10,"",unknownSize);
            bind(db,actor,actorBlock);if(!unknownSize)bind(db,target,targetBlock);
            if(!unknownSize){
                JSONObject targetResources=new JSONObject(db.getCombatant(target).optString("resources_json","{}"));
                assertEquals(targetBlock,targetResources.optString("block_id"));
                assertEquals("0",String.valueOf(db.getCombatant(target).optInt("is_player",0)));
                java.lang.reflect.Method sizeMethod=AppDb.class.getDeclaredMethod("verifiedCombatantSizeCategory",JSONObject.class);
                sizeMethod.setAccessible(true);
                assertEquals(CompiledStatblockEngine.get(targetBlock).optString("size_category"),
                    sizeMethod.invoke(db,db.getCombatant(target)));
            }
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues open=new ContentValues();open.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",open,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();position(db,actor,actorX,2);position(db,target,targetX,2);
            org.json.JSONArray typedDamage=null;
            {
                JSONObject compiledAction=null;org.json.JSONArray actions=CompiledStatblockEngine
                    .runtimeProfile(actorBlock).optJSONArray("actions");
                for(int i=0;actions!=null&&i<actions.length();i++){
                    JSONObject candidate=actions.optJSONObject(i);
                    if(candidate!=null&&action.equals(candidate.optString("name")))compiledAction=candidate;
                }
                assertNotNull(compiledAction);
                org.json.JSONArray specs=StatblockCombatFlowEngine.damageSpecs(
                    compiledAction.optString("damage"),compiledAction.optString("damage_type"),"");
                if(specs.length()>1){
                    typedDamage=new org.json.JSONArray();damage=0;
                    for(int i=0;i<specs.length();i++){
                        JSONObject spec=specs.getJSONObject(i);
                        CompiledMonsterDamageGuard.Range range=CompiledMonsterDamageGuard.range(
                            spec.optString("expression"),"",false);
                        assertNotNull(range);damage+=range.min;
                        typedDamage.put(new JSONObject().put("expression",spec.optString("expression"))
                            .put("damage_type",spec.optString("type")).put("damage_total",range.min));
                    }
                }
            }
            JSONObject applied=typedDamage==null?db.applyCompiledStatblockAttack(actor,target,actorBlock,action,
                18,0,damage,0,0L,expectedMove?destinationX:-1,expectedMove?2:-1,"ui-monster-size-"+token):
                db.applyCompiledStatblockAttack(actor,target,actorBlock,action,18,0,typedDamage,0,0L,
                    expectedMove?destinationX:-1,expectedMove?2:-1,"ui-monster-size-"+token);
            if(unknownSize){
                assertEquals("target_size_not_verified",applied.optString("statblock_attack_blocked"));
                assertEquals(100,db.getCombatant(target).optInt("hp"));
                assertEquals(targetX,db.tacticalGridPosition(target).optInt("cell_x"));
                return;
            }
            assertTrue(applied.toString(),applied.optBoolean("statblock_attack_committed"));
            assertEquals(100-damage,db.getCombatant(target).optInt("hp"));
            JSONObject resolved=applied.getJSONObject("resolved");
            if(expectedMove){
                assertEquals(destinationX,db.tacticalGridPosition(target).optInt("cell_x"));
                assertTrue(resolved.has("on_hit_forced_movement_transition"));
            }else{
                assertEquals(targetX,db.tacticalGridPosition(target).optInt("cell_x"));
                assertEquals("target_size_exceeds_movement_limit",
                    resolved.optString("on_hit_forced_movement_skipped"));
                assertEquals(CompiledStatblockEngine.get(targetBlock).optString("size_category"),
                    resolved.optString("target_size_category"));
                assertFalse(resolved.has("on_hit_forced_movement_transition"));
            }
            if("Balor".equals(actorBlock)||"Goristro".equals(actorBlock))
                assertEquals(expectedMove,
                    ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }

    @Test public void merrowHarpoonPullsEligibleTargetAndSatyrHoovesHonorsSizeCap()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        assertEquals("Large",CompiledStatblockEngine.get("Merrow").optString("size_category"));
        assertEquals("Medium",CompiledStatblockEngine.get("Satyr").optString("size_category"));
        runCase("Merrow","Harpoon","Satyr",2,6,4,6,true,false,"merrow-pull");
        runCase("Satyr","Hooves","Satyr",3,4,6,4,true,false,"satyr-push");
        runCase("Satyr","Hooves","Merrow",3,4,-1,4,false,false,"satyr-large-target");
        runCase("Satyr","Hooves","",3,4,6,4,false,true,"satyr-unknown-size");
        runCase("Tough Boss","Warhammer","Satyr",3,4,6,5,true,false,"tough-boss-push");
        runCase("Goristro","Brutal Gore","Satyr",3,4,6,13,true,false,"goristro-push-prone");
        runCase("Goristro","Brutal Gore","Colossus",3,4,-1,13,false,false,"goristro-size-cap");
        runCase("Balor","Flame Whip","Merrow",2,6,4,20,true,false,"balor-pull-prone");
        runCase("Balor","Flame Whip","Colossus",2,4,-1,20,false,false,"balor-size-cap");
        runCase("Shambling Mound","Charged Tendril","Satyr",3,5,4,7,true,false,"shambling-mound-pull");
    }

    @Test public void hookHorrorChoosesTowardOrAwayAndHonorsItsLargeTargetLimit()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        runCase("Hook Horror","Hook","Satyr",2,4,3,6,true,false,"hook-horror-toward");
        runCase("Hook Horror","Hook","Satyr",2,4,5,6,true,false,"hook-horror-away");
        runCase("Hook Horror","Hook","Colossus",2,4,-1,6,false,false,"hook-horror-size-cap");
    }
}
