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
