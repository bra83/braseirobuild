package com.braseiro.dnd5e.e2e;

import android.content.ContentValues;
import android.content.Context;
import com.braseiro.dnd5e.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Real SQLite gateways; no adventure supplement, AI adjudication or fake monster. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class Core2024PlayableSessionRobolectricTest {
    private static JSONObject hero() throws Exception {
        return new JSONObject().put("name","Lia").put("species","Human")
            .put("class_name","Cleric").put("level",1).put("hp",10).put("max_hp",10)
            .put("ac",16).put("str",14).put("dex",10).put("con",14)
            .put("int_score",10).put("wis",16).put("cha",10)
            .put("skills","Insight, Medicine").put("spells","Bless")
            .put("inventory_ledger",new JSONArray().put(new JSONObject()
                .put("id","core-mace").put("name","Mace").put("quantity",1).put("equipped",true)));
    }
    private static JSONObject resources(AppDb db,long id) throws Exception {
        return new JSONObject(db.getCombatant(id).getString("resources_json"));
    }
    private static void position(AppDb db,long id,int x,int y,JSONObject scene) {
        ContentValues v=new ContentValues();
        v.put("x",(x+0.5)/scene.optInt("width_cells"));
        v.put("y",(y+0.5)/scene.optInt("height_cells"));
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }

    @Test public void coreSessionSurvivesReopenAndRepeatedBackupImport() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        String name="core_session_"+System.nanoTime()+".db";
        AppDb db=AppDbTestHelper.createSandbox(context,name);
        try {
            db.setActiveMapProfile("dnd");
            db.saveCampaign("Núcleo 2024",DndRulesEngine.PROFILE_2024,"Sandbox",true,true,false,"");
            // The real creation gateway must inherit the campaign edition.
            JSONObject created=db.saveCharacterSheetAtomic(0,false,hero(),"","manual");
            assertTrue(created.toString(),created.optBoolean("character_saved"));
            long character=created.getLong("character_id");
            assertEquals(DndRulesEngine.PROFILE_2024,db.getCharacter(character).getString("rules_source"));
            assertEquals(2,db.ensureCharacterResourceState(character).getJSONArray("spell_slots_current").getInt(0));
            assertTrue(db.configureEquippedWeaponAttack(character,"core-mace",4,"1d6+2","contundente","melee",5,0,0));
            WorldStateEngine world=new WorldStateEngine(db);
            JSONObject exploration=world.commitGameplayInput("Exploro a clareira","PLAYER_ACTION",
                new JSONObject().put("confirm",true).put("destination","Clareira de teste"));
            assertTrue(exploration.toString(),exploration.optBoolean("persisted"));
            String sandbox=world.gameplaySandboxState().toString();
            JSONObject pending=world.commitGameplayInput("Examino um selo complexo","PLAYER_ACTION",
                new JSONObject().put("confirm",true).put("requires_test",true));
            assertFalse(pending.optBoolean("sandbox_mutated"));
            assertEquals("pending_manual_result",pending.optString("status"));
            assertEquals(sandbox,world.gameplaySandboxState().toString());
            JSONObject assisted=world.commitGameplayInput("Examino um selo complexo","PLAYER_ACTION",
                new JSONObject().put("confirm",true).put("requires_test",true)
                    .put("manual_total",17).put("mechanical_resolution_confirmed",true)
                    .put("resolved_outcome","success"));
            assertTrue(assisted.toString(),assisted.optBoolean("persisted"));
            assertEquals(2,db.ensureCharacterResourceState(character).getJSONArray("spell_slots_current").getInt(0));

            Xmm2025CompiledBestiaryEngine.install();
            long actor=db.addCombatantWithVisual("Lia",20,10,10,16,"",true,"character",character,"");
            JSONObject spawn=db.addCompiledStatblockCombatant("Goblin Warrior","Goblin de teste",10,"ui-monster-spawn-core-session");
            assertTrue(spawn.toString(),spawn.optBoolean("statblock_spawn_committed"));
            long monster=spawn.getLong("combatant_id");
            assertEquals("Monster Manual (2025)",resources(db,monster).getString("statblock_source"));
            assertEquals(10,db.getCombatant(monster).getInt("hp"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();
            position(db,actor,2,2,scene);position(db,monster,3,2,scene);
            assertEquals(1,db.getCombatant(actor).getInt("active_turn"));
            int movement=resources(db,actor).getInt("movement_remaining_ft");
            JSONObject moved=db.moveCombatant(actor,5,"ui-move-core-session");
            assertFalse(moved.toString(),moved.has("movement_blocked"));
            assertEquals(movement-5,resources(db,actor).getInt("movement_remaining_ft"));
            long action=db.addPartyAction(character,"Lia","Atacar","combate");
            db.markPartyActionsNarrated(new JSONArray().put(action));
            JSONObject preview=db.previewTacticalWeaponAttack(actor,monster,"core-mace");
            assertTrue(preview.toString(),preview.optBoolean("attack_geometry_valid"));
            JSONObject strike=db.applySingleWeaponAttackForPartyAction(action,actor,monster,15,4,5,
                "contundente","ui-weapon2-core-session","core-mace","1d6+2",
                preview.optInt("effective_ac"),preview.optString("attack_geometry_fingerprint"),"Normal");
            assertTrue(strike.toString(),strike.optBoolean("party_action_resolved"));
            assertEquals(5,db.getCombatant(monster).getInt("hp"));
            JSONObject replay=db.applySingleWeaponAttackForPartyAction(action,actor,monster,15,4,5,
                "contundente","ui-weapon2-core-session","core-mace","1d6+2",
                preview.optInt("effective_ac"),preview.optString("attack_geometry_fingerprint"),"Normal");
            assertTrue(replay.toString(),replay.has("party_attack_blocked"));
            assertEquals(5,db.getCombatant(monster).getInt("hp"));
            db.spendCharacterSpellSlot(character,1,"core-session-slot");
            assertEquals(1,db.ensureCharacterResourceState(character).getJSONArray("spell_slots_current").getInt(0));
            assertEquals(monster,db.nextTurn().getLong("id"));
            JSONObject retaliation=db.applyCompiledStatblockAttack(monster,actor,"Goblin Warrior",
                "Scimitar",15,5,0,"ui-monster-core-retaliation");
            assertTrue(retaliation.toString(),retaliation.optBoolean("statblock_attack_committed"));
            assertEquals(5,db.getCombatant(actor).getInt("hp"));
            assertEquals(5,db.getCharacter(character).getInt("hp"));
            JSONObject secondAttack=db.applyCompiledStatblockAttack(monster,actor,"Goblin Warrior",
                "Scimitar",15,5,0,"ui-monster-core-second-attack");
            assertTrue(secondAttack.toString(),secondAttack.has("statblock_attack_blocked"));
            assertEquals(5,db.getCharacter(character).getInt("hp"));
            db.updateConditions(monster,"Caído"); // Mestre confirms the condition; no invented ability.
            String conditions=db.getCombatant(monster).getString("conditions");
            int remainingMovement=resources(db,actor).getInt("movement_remaining_ft");
            int turn=db.getCombatState().getInt("turn_index");
            db.close();db=AppDbTestHelper.createSandbox(context,name);db.setActiveMapProfile("dnd");
            assertEquals(turn,db.getCombatState().getInt("turn_index"));
            assertEquals(5,db.getCharacter(character).getInt("hp"));
            assertEquals(remainingMovement,resources(db,actor).getInt("movement_remaining_ft"));
            assertEquals(conditions,db.getCombatant(monster).getString("conditions"));
            assertEquals(5,db.getCombatant(monster).getInt("hp"));
            assertEquals("Clareira de teste",db.getSceneState().getString("location"));
            JSONObject backup=db.exportAll();
            assertEquals(21,backup.getInt("version"));
            assertEquals("",Backup21IntegrityEnvelope.check(backup));
            for(int i=0;i<2;i++) {
                db.importAll(backup);
                assertEquals(1,db.listCharacters().size());assertEquals(2,db.listCombatants().size());
                character=db.findCharacterByName("Lia").getLong("id");
                actor=db.findCombatantByName("Lia").getLong("id");
                monster=db.findCombatantByName("Goblin de teste").getLong("id");
                assertEquals(5,db.getCombatant(monster).getInt("hp"));
                assertEquals(conditions,db.getCombatant(monster).getString("conditions"));
                assertEquals(remainingMovement,resources(db,actor).getInt("movement_remaining_ft"));
                assertEquals(1,db.ensureCharacterResourceState(character).getJSONArray("spell_slots_current").getInt(0));
                assertEquals("Clareira de teste",new WorldStateEngine(db).gameplaySandboxState().getString("current_location"));
            }
            db.setCombatRunning(false);
            db.setCharacterHp(character,4);
            world=new WorldStateEngine(db);
            JSONObject rest=world.preview("descanso curto");
            assertTrue(rest.toString(),world.commit(rest));
            assertEquals(4,db.getCharacter(character).getInt("hp"));
            JSONObject healed=db.applyCharacterShortRestHitDice(character,1,6);
            assertFalse(healed.toString(),healed.has("short_rest_hit_dice_blocked"));
            assertEquals(10,db.getCharacter(character).getInt("hp"));
            rest=world.preview("descanso longo");
            assertTrue(rest.toString(),world.commit(rest));
            assertEquals(0,db.ensureCharacterResourceState(character).getInt("hit_dice_spent"));
            assertEquals(2,db.ensureCharacterResourceState(character).getJSONArray("spell_slots_current").getInt(0));
            // XP is deliberately not inferred from a wounded, undefeated creature.
            assertEquals(0,db.ensureCharacterResourceState(character).optInt("xp",0));
            // An explicit DM award for a resolved objective uses the existing XP gateway.
            db.addJournal("Objetivo resolvido","Mestre concede 50 XP pela recuperação do selo.","progression");
            assertEquals(50,db.addCharacterXp(character,50).getInt("xp"));
            assertEquals(1,db.getCharacter(character).getInt("level"));
            backup=db.exportAll();db.importAll(backup);db.importAll(backup);
            db.close();db=AppDbTestHelper.createSandbox(context,name);db.setActiveMapProfile("dnd");
            character=db.findCharacterByName("Lia").getLong("id");
            assertEquals(1,db.listCharacters().size());
            assertEquals(1,db.listJournal().size());
            assertEquals(50,db.ensureCharacterResourceState(character).getInt("xp"));
            assertEquals(0,db.ensureCharacterResourceState(character).getInt("hit_dice_spent"));
            assertEquals(2,db.ensureCharacterResourceState(character).getJSONArray("spell_slots_current").getInt(0));
        } finally {db.close();context.deleteDatabase(name);}
    }

    @Test public void editingWithoutSourcePreservesSaved2014HeroIn2024Campaign() throws Exception {
        Context context=RuntimeEnvironment.getApplication();String name="legacy_core_"+System.nanoTime()+".db";
        AppDb db=AppDbTestHelper.createSandbox(context,name);
        try {
            db.setActiveMapProfile("dnd");
            JSONObject old=hero().put("rules_source",DndRulesEngine.PROFILE_2014);
            long id=db.saveCharacterSheetAtomic(0,false,old,"","manual").getLong("character_id");
            old.remove("rules_source");
            assertTrue(db.saveCharacterSheetAtomic(id,true,old,"","manual").optBoolean("character_saved"));
            assertEquals(DndRulesEngine.PROFILE_2014,db.getCharacter(id).getString("rules_source"));
            assertEquals(DndRulesEngine.PROFILE_2014,db.ensureCharacterResourceState(id).getString("rules_profile"));
        } finally {db.close();context.deleteDatabase(name);}
    }
}
