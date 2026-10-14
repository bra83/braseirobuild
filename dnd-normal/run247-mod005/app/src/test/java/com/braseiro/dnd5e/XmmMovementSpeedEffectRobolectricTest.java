package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmMovementSpeedEffectRobolectricTest {
    private static void position(AppDb db,long id,int x,int y){
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);
        values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void bind(AppDb db,long id,String block)throws Exception{
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        resources.put("block_id",block);ContentValues bind=new ContentValues();
        bind.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",bind,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }
    private static void runSpeedCase(String block,String action,int actorX,int targetX,
        int expectedSpeed,String expiryBoundary)throws Exception{
        Context context=RuntimeEnvironment.getApplication();String name="xmm_speed_"+
            java.util.UUID.randomUUID().toString().replace("-","").substring(0,10)+".db";
        AppDb db=new AppDb(context,name);
        try{
            db.setActiveMapProfile("dnd");long actor=db.addCombatant("Source "+name,20,60,60,14,"",false);
            long target=db.addCombatant("Target "+name,10,40,40,12,"",true);bind(db,actor,block);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues open=new ContentValues();open.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",open,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();position(db,actor,actorX,2);position(db,target,targetX,2);
            JSONObject compiled=null;JSONArray actions=CompiledStatblockEngine.runtimeProfile(block).optJSONArray("actions");
            for(int i=0;actions!=null&&i<actions.length();i++)if(action.equals(actions.optJSONObject(i).optString("name")))compiled=actions.optJSONObject(i);
            assertNotNull(compiled);JSONArray specs=StatblockCombatFlowEngine.damageSpecs(
                compiled.optString("damage"),compiled.optString("damage_type"),"");
            int damage=0;JSONArray typed=new JSONArray();
            for(int i=0;i<specs.length();i++){
                JSONObject spec=specs.getJSONObject(i);CompiledMonsterDamageGuard.Range range=
                    CompiledMonsterDamageGuard.range(spec.optString("expression"),"",false);
                assertNotNull(range);damage+=range.min;
                typed.put(new JSONObject().put("expression",spec.optString("expression"))
                    .put("damage_type",spec.optString("type")).put("damage_total",range.min));
            }
            JSONObject rollPreview=db.previewCompiledMonsterAttackRollMode(actor,target,block,action,0L);
            assertTrue(rollPreview.toString(),rollPreview.optBoolean("monster_roll_preview_valid"));
            int second=rollPreview.optBoolean("second_d20_required",false)?14:0;
            JSONObject hit=typed.length()>1?db.applyCompiledStatblockAttack(actor,target,block,action,
                15,second,typed,0,0L,-1,-1,"ui-monster-speed-"+name.replace(".db", "")):
                db.applyCompiledStatblockAttack(actor,target,block,action,15,second,damage,0,0L,-1,-1,
                    "ui-monster-speed-"+name.replace(".db", ""));
            assertTrue(hit.toString(),hit.optBoolean("statblock_attack_committed"));
            assertTrue(hit.toString(),hit.getJSONObject("resolved").has("movement_speed_transition"));
            assertEquals(target,db.nextTurn().optLong("id"));
            JSONObject targetResources=new JSONObject(db.getCombatant(target).optString("resources_json","{}"));
            assertEquals(expectedSpeed,targetResources.optInt("movement_max_ft"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            targetResources=new JSONObject(db.getCombatant(target).optString("resources_json","{}"));
            int expectedActive=targetResources.optInt("movement_max_ft");
            if("target_turn_end".equals(expiryBoundary)){
                assertEquals(0,XmmMovementSpeedEffectLedger.penalty(new JSONArray(
                    targetResources.optString(XmmMovementSpeedEffectLedger.FIELD,"[]")),target));
                assertEquals(target,db.nextTurn().optLong("id"));
                assertEquals(30,new JSONObject(db.getCombatant(target).optString("resources_json","{}"))
                    .optInt("movement_max_ft"));
            }else{
                assertEquals(expectedSpeed,expectedActive);
                assertEquals(target,db.nextTurn().optLong("id"));
                assertEquals(30,new JSONObject(db.getCombatant(target).optString("resources_json","{}"))
                    .optInt("movement_max_ft"));
            }
        }finally{db.close();context.deleteDatabase(name);}
    }
    @Test public void lacedonBiteReducesMovementUntilItsNextTurnStarts()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_speed_effect_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try{
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("Lacedon Speed Test",20,30,30,12,"",false);
            long target=db.addCombatant("Speed Test Target",10,30,30,10,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id","Lacedon Ghoul");
            ContentValues bind=new ContentValues();bind.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",bind,
                "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues open=new ContentValues();open.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",open,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();position(db,actor,2,2);position(db,target,3,2);

            JSONObject hit=db.applyCompiledStatblockAttack(actor,target,"Lacedon Ghoul","Icy Bite",
                15,0,9,0,0L,-1,-1,"ui-monster-lacedon-speed-hit");
            assertTrue(hit.toString(),hit.optBoolean("statblock_attack_committed"));
            assertEquals(5,hit.getJSONObject("resolved").getJSONObject("movement_speed_transition")
                .optInt("feet"));
            JSONArray persisted=new JSONArray(new JSONObject(db.getCombatant(target)
                .optString("resources_json","{}")).optString(XmmMovementSpeedEffectLedger.FIELD,"[]"));
            assertEquals(5,XmmMovementSpeedEffectLedger.penalty(persisted,target));

            assertEquals(target,db.nextTurn().optLong("id"));
            JSONObject targetResources=new JSONObject(db.getCombatant(target).optString("resources_json","{}"));
            assertEquals(25,targetResources.optInt("movement_max_ft"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            assertEquals(0,XmmMovementSpeedEffectLedger.penalty(new JSONArray(
                new JSONObject(db.getCombatant(target).optString("resources_json","{}"))
                    .optString(XmmMovementSpeedEffectLedger.FIELD,"[]")),target));
            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals(30,new JSONObject(db.getCombatant(target).optString("resources_json","{}"))
                .optInt("movement_max_ft"));
        }finally{db.close();context.deleteDatabase(dbName);}
    }
    @Test public void otherCompiledSpeedRidersUseTheirOwnTurnBoundaries()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        runSpeedCase("Warrior Commander","Longbow",2,3,20,"target_turn_end");
        runSpeedCase("Berserker Commander","Javelin",2,5,25,"source_turn_start");
        runSpeedCase("Merfolk Skirmisher","Ocean Spear",2,5,20,"target_turn_end");
        runSpeedCase("Hobgoblin Warlord","Javelin",2,5,20,"source_turn_start");
        runSpeedCase("Frost Giant","Great Bow",2,5,20,"target_turn_end");
    }
}
