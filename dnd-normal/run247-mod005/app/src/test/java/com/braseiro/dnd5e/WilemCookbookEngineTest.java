package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import java.util.HashSet;
import static org.junit.Assert.*;

/** Persistence, atomic inventory, party boundaries and 300 recipe validations. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class WilemCookbookEngineTest {
    private Context context;
    private AppDb db;
    private WilemCookbookEngine book;
    private String name;
    private long wilem,kvothe;

    @Before public void prepare()throws Exception{
        context=RuntimeEnvironment.getApplication();name="wilem-cook-"+System.nanoTime()+".db";
        db=new AppDb(context,name);db.setActiveMapProfile("dnd");
        wilem=db.addCharacter("Wilem","Anão","Bárbaro",1,14,14,14,16,10,12,8,11,10,true,"");
        kvothe=db.addCharacter("Kvothe","Tiferino","Mago",1,7,7,12,8,15,12,16,12,13,true,"");
        book=new WilemCookbookEngine(db);
    }
    @After public void cleanup(){if(db!=null)db.close();if(context!=null)context.deleteDatabase(name);}
    private String receipt(){return WilemCookbookEngine.newReceipt();}
    private void join()throws Exception{
        db.upsertPartyMembership(new JSONObject().put("actor_kind","hero").put("character_id",wilem).put("status","active")
            .put("party_role","companion").put("recruitment_type","player_confirmed"));
    }
    @Test public void catalogHasExactly300OriginalValidUniqueRecipes()throws Exception{
        JSONArray all=WilemCookbookCatalog.recipes();assertEquals(300,all.length());assertEquals(300,WilemCookbookCatalog.catalogSize());
        HashSet<String> ids=new HashSet<String>();
        for(int i=0;i<all.length();i++){
            JSONObject r=all.getJSONObject(i);
            assertTrue("duplicate id "+r.optString("id"),ids.add(r.getString("id")));
            assertTrue(r.getInt("servings")>0);assertTrue(r.getInt("minutes")>0);
            assertTrue(r.getInt("dc")>=8);assertTrue(r.optBoolean("original_rule"));
            JSONObject materials=r.getJSONObject("ingredients");JSONArray names=materials.names();assertNotNull(names);
            for(int j=0;j<names.length();j++){
                String id=names.getString(j);
                assertTrue(r.getString("name")+" missing ingredient "+id,WilemCookbookCatalog.ingredientById(id).length()>0);
                assertTrue(materials.getInt(id)>0);
            }
        }
        assertEquals(10,WilemCookbookCatalog.starterIds().length());
    }
    @Test public void onlyWilemMayActWhenInPersistedParty()throws Exception{
        assertTrue(book.ownerExists());assertFalse(book.canCook());
        try{book.acquire("batata",1,"coleta_confirmada",true,receipt());fail("owner away");}
        catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("companhia"));}
        join();assertTrue(book.canCook());
        db.updatePartyMembershipStatus(db.findActivePartyHero(wilem).optLong("id"),"away","","Saiu para estudar");
        assertFalse(book.canCook());assertTrue(book.state().optJSONArray("known").length()>0);
    }
    @Test public void receiptsInventoryAndMealsSurviveReopen()throws Exception{
        join();
        String receipt=receipt();
        book.acquire("batata",2,"coleta_confirmada",true,receipt);
        try{book.acquire("batata",2,"coleta_confirmada",true,receipt);fail("receipt replay");}
        catch(IllegalStateException expected){}
        book.acquire("agua",1,"coleta_confirmada",true,receipt());
        book.acquire("cebola",1,"coleta_confirmada",true,receipt());
        assertEquals(2,book.pantry().optJSONArray("items").getJSONObject(0).optInt("quantity"));
        System.out.println("WILEM_BEFORE_COOK "+book.pantry()+" SHEET "+db.getCharacter(wilem).optString("sheet_json"));
        JSONObject batch=book.cook("com_batata_0",0,true,true,receipt());
        System.out.println("WILEM_AFTER_COOK "+book.pantry()+" SHEET "+db.getCharacter(wilem).optString("sheet_json"));
        assertTrue(batch.optBoolean("completed"));assertEquals(4,batch.optInt("remaining"));
        JSONObject meal=book.serve(batch.getString("id"),wilem,"",true,"Contou uma historia da primeira aula",receipt());
        assertTrue(meal.optBoolean("food"));assertEquals("nenhum",meal.optString("effect_key"));
        System.out.println("WILEM_AFTER_SERVE "+book.pantry()+" SHEET "+db.getCharacter(wilem).optString("sheet_json"));
        assertEquals(1,book.pantry().optJSONArray("items").getJSONObject(0).optInt("quantity"));
        JSONObject before=book.state();assertEquals(1,before.optJSONArray("meals").length());
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");book=new WilemCookbookEngine(db);
        assertTrue(book.canCook());assertEquals(1,book.state().optJSONArray("meals").length());
        assertEquals(1,book.pantry().optJSONArray("items").getJSONObject(0).optInt("quantity"));
        assertEquals(1,db.listJournal().stream().filter(j->j.optString("title").startsWith("Mesa de Wilem")).count());
    }
    @Test public void marketPurchaseDebitsCanonicalGoldAtomically()throws Exception{
        join();
        JSONObject sheet=new JSONObject(db.getCharacter(wilem).optString("sheet_json","{}"));
        sheet.put("inventory_currency_gp",1d);
        db.updateCharacterSheetChecked(wilem,sheet.toString());
        JSONObject bought=book.acquire("batata",2,"mercado_pago",true,receipt());
        assertTrue(bought.optBoolean("money_auto_debited"));
        assertEquals(4,bought.getInt("paid_copper"));
        JSONObject paid=new JSONObject(db.getCharacter(wilem).getString("sheet_json"));
        assertEquals(0.96d,paid.getDouble("inventory_currency_gp"),.00001d);
        int before=book.pantry().optJSONArray("items").getJSONObject(0).getInt("quantity");
        try{book.acquire("queijo",30,"mercado_pago",true,receipt());fail("not enough copper");}
        catch(IllegalStateException expected){}
        assertEquals(before,book.pantry().optJSONArray("items").getJSONObject(0).getInt("quantity"));
        assertEquals(0.96d,new JSONObject(db.getCharacter(wilem).getString("sheet_json"))
            .getDouble("inventory_currency_gp"),.00001d);
        try{book.acquire("carne_aranha",1,"coleta_confirmada",true,receipt());fail("unearned monster meat");}
        catch(IllegalStateException expected){}
    }
    @Test public void monsterHarvestRequiresRealDefeatedCreatureAndCannotDuplicate()throws Exception{
        join();
        long body=db.addCombatant("Javali selvagem",9,0,8,12,"",false);
        java.util.List<Integer> faces=java.util.Collections.singletonList(20);
        Dice3DOverlay.RollResult dice=new Dice3DOverlay.RollResult(
            faces,faces,20,20,"20","","d20","Normal",120,8675309L,"test",
            RollIntegrity.signature("d20",faces,0,8675309L,20));
        JSONObject harvested=book.harvestWithPhysicalRoll(body,"carne_javali",dice,receipt());
        assertTrue(harvested.optBoolean("success"));
        JSONArray current=book.pantry().getJSONArray("items");
        assertEquals(1,current.length());assertEquals("carne_javali",current.getJSONObject(0).getString("cookbook_ingredient_id"));
        assertEquals(1,book.state().getJSONArray("harvests").length());
        try{book.harvestWithPhysicalRoll(body,"carne_javali",dice,receipt());fail("double harvest");}
        catch(IllegalStateException expected){}
        assertEquals(1,book.pantry().getJSONArray("items").length());
        long alive=db.addCombatant("Javali selvagem",7,8,8,12,"",false);
        try{book.harvestWithPhysicalRoll(alive,"carne_javali",dice,receipt());fail("living creature");}
        catch(IllegalStateException expected){}
        assertEquals(1,book.state().getJSONArray("harvests").length());
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");book=new WilemCookbookEngine(db);
        assertEquals(1,book.state().getJSONArray("harvests").length());
        assertEquals(1,book.pantry().getJSONArray("items").length());
    }
    @Test public void riskyMealRejectsTypedSaveAndUsesVerifiedPhysicalRoll()throws Exception{
        join();
        db.upsertPartyMembership(new JSONObject().put("actor_kind","hero").put("character_id",kvothe)
            .put("status","active").put("party_role","companion").put("recruitment_type","player_confirmed"));
        book.recordRumor("mon_javali_0","Cozinheiro",
            "Uma refeicao de javali requer pesquisa e conservacao.",receipt());
        book.confirmDiscovery("mon_javali_0","Receita obtida em pesquisa comprovada",receipt());
        long body=db.addCombatant("Javali selvagem",9,0,8,12,"",false);
        java.util.List<Integer> high=java.util.Collections.singletonList(20);
        Dice3DOverlay.RollResult harvested=new Dice3DOverlay.RollResult(
            high,high,20,20,"20","","d20","Normal",100,85231L,"test",
            RollIntegrity.signature("d20",high,0,85231L,20));
        assertTrue(book.harvestWithPhysicalRoll(body,"carne_javali",harvested,receipt())
            .getBoolean("success"));
        for(String id:new String[]{"sal","agua","batata","cebola"})
            book.acquire(id,1,"coleta_confirmada",true,receipt());
        java.util.List<Integer> low=java.util.Collections.singletonList(1);
        Dice3DOverlay.RollResult badPrep=new Dice3DOverlay.RollResult(
            low,low,1,1,"1","","d20","Normal",100,85232L,"test",
            RollIntegrity.signature("d20",low,0,85232L,1));
        JSONObject prepared=book.cookWithPhysicalRoll("mon_javali_0",badPrep,true,true,receipt());
        assertTrue(prepared.optString("result").contains("risco"));
        try{book.serveWithSave(prepared.getString("id"),kvothe,"",false,
            "Cena",20,receipt());fail("Typed save blocked");}
        catch(IllegalStateException expected){}
        assertEquals(0,book.state().getJSONArray("meals").length());
        Dice3DOverlay.RollResult goodSave=new Dice3DOverlay.RollResult(
            high,high,20,20,"20","","d20","Normal",100,85233L,"test",
            RollIntegrity.signature("d20",high,0,85233L,20));
        JSONObject served=book.serveWithPhysicalSave(prepared.getString("id"),kvothe,"",
            false,"Historia a mesa",goodSave,receipt());
        assertFalse(served.optBoolean("poisoned_after_consumption"));
        assertEquals("physical3d",served.getJSONObject("constitution_save_proof").getString("source"));
        assertEquals(1,book.state().getJSONArray("meals").length());
    }
    @Test public void magicalBaitCannotResolveWithTypedD20AndRecordsPhysicalProof()throws Exception{
        join();
        JSONObject state=book.state();
        state.getJSONArray("baits").put(new JSONObject().put("id","bait-physical-test")
            .put("recipe_id","isca_mel_fantasia").put("status","armada")
            .put("batch_quality","somente_beneficio"));
        db.getWritableDatabase().execSQL(
            "UPDATE wilem_recipe_books SET state_json=? WHERE world_profile=? AND character_id=?",
            new Object[]{state.toString(),"dnd",wilem});
        long target=db.addCombatant("Lobo faminto",9,12,12,12,"",false);
        try{
            book.resolveBait("bait-physical-test",target,true,true,false,1,receipt());
            fail("Typed magical save must be denied");
        }catch(IllegalStateException expected){}
        java.util.List<Integer> faces=java.util.Collections.singletonList(20);
        Dice3DOverlay.RollResult dice=new Dice3DOverlay.RollResult(
            faces,faces,20,20,"20","","d20","Normal",110,85234L,"test",
            RollIntegrity.signature("d20",faces,0,85234L,20));
        JSONObject result=book.resolveBaitWithPhysicalSave("bait-physical-test",target,
            true,true,false,dice,2,true,receipt());
        assertEquals(22,result.getInt("save_total"));
        assertFalse(result.getBoolean("effect_applied"));
        assertEquals("physical3d",result.getJSONObject("save_proof").getString("source"));
        assertEquals("resolvida",result.getString("status"));
    }
    @Test public void noIngredientCreatesNoFreeMealsAndTransactionRollsBack()throws Exception{
        join();int entries=book.state().getJSONArray("cooked").length();
        try{book.cook("com_batata_0",0,true,true,receipt());fail("No potatoes");}
        catch(IllegalStateException expected){}
        assertEquals(entries,book.state().getJSONArray("cooked").length());
        assertEquals(0,book.pantry().getJSONArray("items").length());
    }
    @Test public void fantasyBaitRequiresExplicitConsumptionAndSave()throws Exception{
        join();book.acquire("mel",1,"recompensa_confirmada",true,receipt());
        book.acquire("maça",1,"coleta_confirmada",true,receipt());
        try{book.cook("isca_mel",12,true,true,receipt());fail("manual d20 must be rejected");}
        catch(IllegalStateException expected){}
        java.util.List<Integer> faces=java.util.Collections.singletonList(12);
        Dice3DOverlay.RollResult physical=new Dice3DOverlay.RollResult(
            faces,faces,12,12,"12","","d20","Normal",120,12345L,"test",
            RollIntegrity.signature("d20",faces,0,12345L,12));
        JSONObject bait=book.cookWithPhysicalRoll("isca_mel",physical,true,true,receipt());
        assertEquals("physical3d",book.state().getJSONArray("experiments").getJSONObject(0).getString("roll_origin"));
        JSONObject placed=book.setBait(bait.getString("id"),"Entrada do bosque",receipt());
        long creature=db.addCombatant("Lobo faminto",9,8,8,12,"",false);
        try{book.resolveBait(placed.getString("id"),creature,false,true,false,1,receipt());fail("No ingestion");}
        catch(IllegalStateException expected){}
        assertFalse(book.resolveBait(placed.getString("id"),creature,true,true,false,1,receipt()).optBoolean("effect_applied"));
        try{book.resolveBait(placed.getString("id"),creature,true,true,false,1,receipt());fail("Bait already resolved");}
        catch(IllegalStateException expected){}
    }
}
