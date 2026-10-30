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
    private void seedIngredient(String ingredientId,int count)throws Exception{
        JSONObject sheet=new JSONObject(db.getCharacter(wilem).optString("sheet_json","{}"));
        JSONArray inv=sheet.optJSONArray("inventory_ledger");
        if(inv==null)inv=new JSONArray();
        JSONObject ing=WilemCookbookCatalog.ingredientById(ingredientId);
        assertTrue("unknown test ingredient "+ingredientId,ing.length()>0);
        inv.put(new JSONObject().put("id","wilem_ing_"+ingredientId)
            .put("name",ing.optString("name")).put("cookbook_ingredient_id",ingredientId)
            .put("quantity",count));
        sheet.put("inventory_ledger",inv)
            .put("inventory_load",InventoryLedgerEngine.totals(inv,db.getCharacter(wilem).optInt("str",10),false));
        db.updateCharacterSheetChecked(wilem,sheet.toString());
    }

    private void openMarket(String place,int day,String revision,int potato,int onion,int water,int cheese)throws Exception{
        JSONArray stock=new JSONArray();
        for(Object[] row:new Object[][]{{"batata",potato,2},{"cebola",onion,2},{"agua",water,0},{"queijo",cheese,5}})
            if((int)row[1]>0)stock.put(new JSONObject().put("ingredient_id",row[0])
                .put("quantity",row[1]).put("price_cp",row[2]));
        db.upsertNpcMemory(new JSONObject().put("name","Mina Mercadora")
            .put("active",1).put("stats_json",new JSONObject().put("revision",revision)
                .put("wilem_market_stock",stock).toString()));
        db.saveSceneState(new JSONObject().put("day_number",day).put("clock_hour",12)
            .put("location",place)
            .put("present_npcs_json",new JSONArray().put("Mina Mercadora").toString()));
    }
    private void forageNode(String place,int day,int stock)throws Exception{
        JSONObject node=new JSONObject().put("type","wilem_forage_node")
            .put("id","stryx-garden-1").put("ingredient_id","batata")
            .put("location",place).put("quantity",stock).put("revision","one-harvest");
        db.saveSceneState(new JSONObject().put("day_number",day).put("clock_hour",13)
            .put("location",place).put("clues_json",new JSONArray().put(node).toString()));
    }
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
        openMarket("Mercado de Strixhaven",1,"opening-stock",10,6,6,0);
        JSONObject paySheet=new JSONObject(db.getCharacter(wilem).optString("sheet_json","{}"));
        paySheet.put("inventory_currency_gp",1d);db.updateCharacterSheetChecked(wilem,paySheet.toString());
        String receipt=receipt();
        book.acquire("batata",2,"mercado_pago",true,receipt);
        try{book.acquire("batata",2,"mercado_pago",true,receipt);fail("receipt replay");}
        catch(IllegalStateException expected){}
        book.acquire("agua",1,"mercado_pago",true,receipt());
        book.acquire("cebola",1,"mercado_pago",true,receipt());
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
        openMarket("Mercado de Strixhaven",1,"opening-stock",10,6,0,30);
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
    @Test public void unconsciousCreatureCannotBeHarvestedAndNumberedTokenUsesSameSpecies()throws Exception{
        join();
        long creature=db.addCombatant("Javali selvagem #2",9,0,8,12,"",false);
        db.updateCombatantAdvancedChecked(creature,0,"",false,0,0,"","[]",
            "","","",new JSONObject().put("stable",true).toString());
        java.util.List<Integer> faces=java.util.Collections.singletonList(20);
        Dice3DOverlay.RollResult dice=new Dice3DOverlay.RollResult(
            faces,faces,20,20,"20","","d20","Normal",100,95512L,"test",
            RollIntegrity.signature("d20",faces,0,95512L,20));
        try{book.harvestWithPhysicalRoll(creature,"carne_javali",dice,receipt());
            fail("Creature still stabilized");}
        catch(IllegalStateException expected){}
        db.updateCombatantAdvancedChecked(creature,0,"",false,0,0,"","[]",
            "","","",new JSONObject().put("stable",false).toString());
        assertTrue(book.harvestWithPhysicalRoll(creature,"carne_javali",dice,receipt())
            .getBoolean("success"));
    }
    @Test public void contextualRumorOpensRealDiscoveryWithoutUnlockingEverything()throws Exception{
        join();
        db.saveSceneState(new JSONObject().put("day_number",1)
            .put("location","Mercado de Strixhaven"));
        JSONObject view=book.masterView();
        JSONArray offers=view.getJSONArray("rumor_candidates");
        assertTrue(offers.length()>0);
        String recipeId=offers.getJSONObject(0).getString("recipe_id");
        assertFalse(book.state().getJSONArray("known").toString().contains("\""+recipeId+"\""));
        book.recordRumor(recipeId,"Cozinheiro da taverna",
            "Ouvi a descricao de um preparo incomum que vale investigar.",receipt());
        book.confirmDiscovery(recipeId,
            "Pesquisei a receita e reproduzi as instrucoes em uma bancada de cozinha.",receipt());
        assertTrue(book.state().getJSONArray("known").toString().contains("\""+recipeId+"\""));
        assertEquals(11,book.state().getJSONArray("known").length());
    }
    @Test public void marketRequiresWorldPlaceAndEnforcesStock()throws Exception{
        join();
        JSONObject sheet=new JSONObject(db.getCharacter(wilem).optString("sheet_json","{}"));
        sheet.put("inventory_currency_gp",100d);
        db.updateCharacterSheetChecked(wilem,sheet.toString());
        try{book.acquire("cebola",1,"mercado_pago",true,receipt());fail("No vendor in scene");}
        catch(IllegalStateException expected){}
        openMarket("Feira de Halruaa",4,"initial-stock",0,12,0,0);
        assertEquals(12,book.acquire("cebola",12,"mercado_pago",true,receipt()).getInt("quantity"));
        try{book.acquire("cebola",1,"mercado_pago",true,receipt());fail("No infinite market stock");}
        catch(IllegalStateException expected){}
        openMarket("Feira de Halruaa",5,"initial-stock",0,12,0,0);
        try{book.acquire("cebola",1,"mercado_pago",true,receipt());fail("No auto-restock next day");}
        catch(IllegalStateException expected){}
        openMarket("Feira de Halruaa",5,"new-shipment-2",0,4,0,0);
        assertTrue(book.acquire("cebola",1,"mercado_pago",true,receipt()).getBoolean("money_auto_debited"));
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");book=new WilemCookbookEngine(db);
        assertEquals(2,book.state().getJSONArray("purchases").length());
    }
    @Test public void forageUsesPhysicalD20AndEnvironmentalAvailabilityWithoutDuplicates()throws Exception{
        join();
        db.saveSceneState(new JSONObject().put("day_number",1).put("clock_hour",13)
            .put("location","Horta na escola de Strixhaven"));
        assertEquals(0,book.forageableIngredients().length());
        forageNode("Horta na escola de Strixhaven",1,5);
        JSONArray available=book.forageableIngredients();assertTrue(available.length()>0);
        boolean potato=false;
        for(int i=0;i<available.length();i++)
            if("batata".equals(available.getJSONObject(i).optString("ingredient_id")))potato=true;
        assertTrue(potato);
        java.util.List<Integer> faces=java.util.Collections.singletonList(20);
        Dice3DOverlay.RollResult d20=new Dice3DOverlay.RollResult(
            faces,faces,20,20,"20","","d20","Normal",100,93433L,"test",
            RollIntegrity.signature("d20",faces,0,93433L,20));
        JSONObject harvested=book.forageWithPhysicalRoll("batata",d20,receipt());
        assertTrue(harvested.getBoolean("success"));assertTrue(harvested.getInt("quantity")>0);
        try{book.forageWithPhysicalRoll("batata",d20,receipt());fail("Duplicate same day resource");}
        catch(IllegalStateException expected){}
        forageNode("Horta na escola de Strixhaven",2,5);
        assertTrue(book.forageWithPhysicalRoll("batata",d20,receipt()).getBoolean("success"));
        try{book.forageWithPhysicalRoll("sal",d20,receipt());fail("No sal extraction from garden");}
        catch(IllegalStateException expected){}
    }
    @Test public void appliedWorldEventGrantsArcaneIngredientExactlyOnce()throws Exception{
        join();
        JSONObject reward=new JSONObject().put("wilem_ingredient",
            new JSONObject().put("ingredient_id","essencia_feerica")
                .put("quantity",2).put("recipient_character_id",wilem));
        long eventId=db.addWorldEventStructured(new JSONObject().put("title","Tesouro feerico identificado")
            .put("description","Exploracao concluida")
            .put("consequence_json",reward));
        assertTrue(eventId>0);
        assertEquals(0,book.worldRewardOffers().length());
        db.markWorldEventConsequence(eventId,true,"");
        assertEquals(1,book.worldRewardOffers().length());
        JSONObject claimed=book.claimWorldReward(eventId,receipt());
        assertEquals("essencia_feerica",claimed.getString("ingredient_id"));
        assertEquals(0,book.worldRewardOffers().length());
        try{book.claimWorldReward(eventId,receipt());fail("Claim replay");}
        catch(IllegalStateException expected){}
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");book=new WilemCookbookEngine(db);
        assertEquals(0,book.worldRewardOffers().length());
        assertEquals(1,book.pantry().getJSONArray("items").length());
        assertEquals(2,book.pantry().getJSONArray("items").getJSONObject(0).getInt("quantity"));
    }
    @Test public void noManualFreeIngredientCreationEvenWithConfirmedCheckbox()throws Exception{
        join();
        for(String src:new String[]{"coleta_confirmada","recompensa_confirmada"}){
            try{book.acquire("batata",1,src,true,receipt());fail("Free creation: "+src);}
            catch(IllegalStateException expected){}
        }
        assertEquals(0,book.pantry().getJSONArray("items").length());
        db.saveSceneState(new JSONObject().put("day_number",1).put("clock_hour",12)
            .put("location","Mercado de Strixhaven"));
        assertEquals(0,book.forageableIngredients().length());
    }
    @Test public void twoActiveWilemsDoNotSilentlyReassignTheRecipeBook()throws Exception{
        join();
        long duplicate=db.addCharacter("Willem","Anão","Guerreiro",1,8,8,12,12,12,10,10,10,10,true,"");
        db.upsertPartyMembership(new JSONObject().put("actor_kind","hero").put("character_id",duplicate)
            .put("status","active").put("party_role","companion").put("recruitment_type","player_confirmed"));
        assertEquals(0,book.ownerId());
        assertFalse(book.canCook());
    }

    @Test public void sharedMealCreditsOneDayOfProvisionsOnlyOnceWithoutCreatingRations()throws Exception{
        join();
        JSONObject party=db.getPartyState();
        JSONObject resources=new JSONObject().put("track_travel_resources",true)
            .put("provisions_days",3).put("water_days",3)
            .put("wilem_provisions_credit_day",1);
        party.put("company_resources_json",resources.toString());db.savePartyState(party);
        TravelRulesEngine travel=new TravelRulesEngine(db);
        JSONObject first=travel.previewElapsedTimeResources(24d,"wilem-meal-travel");
        assertTrue(first.optBoolean("wilem_meal_credit_applied"));
        assertEquals(3,first.optInt("provisions_after"));
        assertEquals(2,first.optInt("water_after"));
        assertTrue(travel.commit(first));
        JSONObject after=new JSONObject(db.getPartyState().optString("company_resources_json","{}"));
        assertEquals(1,after.getInt("wilem_provisions_credit_used_day"));
        assertEquals(3,after.getInt("provisions_days"));
        assertEquals(2,after.getInt("water_days"));
        // A mesma confirmacao repetida nao pode descontar a mesma agua.
        assertTrue(travel.commit(first));
        JSONObject replay=new JSONObject(db.getPartyState().optString("company_resources_json","{}"));
        assertEquals(3,replay.getInt("provisions_days"));
        assertEquals(2,replay.getInt("water_days"));
        JSONObject second=travel.previewElapsedTimeResources(24d,"wilem-new-travel");
        assertFalse(second.optBoolean("wilem_meal_credit_applied"));
        assertEquals(2,second.optInt("provisions_after"));
        assertEquals(1,second.optInt("water_after"));
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
            seedIngredient(id,1);
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
    @Test public void knownConditionImmunityOverridesUnsafeBaitSelection()throws Exception{
        join();
        JSONObject st=book.state();
        st.getJSONArray("baits").put(new JSONObject().put("id","bait-immune")
            .put("recipe_id","isca_mel_fantasia")
            .put("status","armada").put("batch_quality","somente_beneficio"));
        db.getWritableDatabase().execSQL(
            "UPDATE wilem_recipe_books SET state_json=? WHERE world_profile=? AND character_id=?",
            new Object[]{st.toString(),"dnd",wilem});
        long target=db.addCombatant("Lobo insone",9,12,12,12,"",false);
        db.updateCombatantAdvancedChecked(target,0,"",false,0,0,"","[]",
            "","","",new JSONObject().put("condition_immunities","unconscious").toString());
        java.util.List<Integer> low=java.util.Collections.singletonList(1);
        Dice3DOverlay.RollResult roll=new Dice3DOverlay.RollResult(
            low,low,1,1,"1","","d20","Normal",100,124331L,"test",
            RollIntegrity.signature("d20",low,0,124331L,1));
        book.registerMonsterConstitution(target,10,0,receipt());
        JSONObject resolved=book.resolveBaitWithPhysicalSave("bait-immune",target,
            true,true,false,roll,999,false,receipt());
        assertTrue(resolved.optBoolean("immunity_auto_detected"));
        assertFalse(resolved.optBoolean("effect_applied"));
        assertEquals("",resolved.optString("applied_condition"));
    }
    @Test public void everyMonsterIngredientHasUniqueAnatomicalCut()throws Exception{
        JSONArray ingredients=WilemCookbookCatalog.ingredients();
        java.util.HashSet<String> cuts=new java.util.HashSet<String>();int count=0;
        for(int i=0;i<ingredients.length();i++){
            JSONObject ing=ingredients.getJSONObject(i);
            if(!"monstro".equals(ing.optString("category")))continue;
            JSONObject anatomy=ing.optJSONObject("anatomy");assertNotNull(anatomy);
            assertTrue(anatomy.optString("cut").length()>5);
            assertTrue(anatomy.optString("warning").length()>12);
            assertTrue(cuts.add(anatomy.optString("cut")));
            count++;
        }
        assertEquals(24,count);
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
        book.registerMonsterConstitution(target,14,0,receipt());
        JSONObject result=book.resolveBaitWithPhysicalSave("bait-physical-test",target,
            true,true,false,dice,999,false,receipt());
        assertEquals(22,result.getInt("save_total"));
        assertFalse(result.getBoolean("effect_applied"));
        assertEquals("physical3d",result.getJSONObject("save_proof").getString("source"));
        assertEquals("resolvida",result.getString("status"));
    }
    @Test public void npcStatsAutoCalculateConstitutionWithoutTypedBonus()throws Exception{
        join();
        db.upsertNpcMemory(new JSONObject().put("name","Crocodilo do Rio")
            .put("active",1).put("stats_json",new JSONObject().put("con",16).toString()));
        long npcId=db.findNpcMemoryByName("Crocodilo do Rio").getLong("id");
        long target=db.addCombatantWithVisual("Crocodilo do Rio",10,25,25,13,
            "",false,"npc",npcId,"");
        JSONObject profile=book.monsterSaveInfo(target);
        assertTrue(profile.optBoolean("known"));
        assertEquals("npc_canonical_stats",profile.optString("authority"));
        assertEquals(3,profile.optInt("bonus"));
        try{book.registerMonsterConstitution(target,25,9,receipt());fail("Cannot overwrite NPC");}
        catch(IllegalStateException expected){}
    }
    @Test public void unknownMonsterNeedsOnePersistentAbilityScoreAndCannotForgeRollBonus()throws Exception{
        join();
        long target=db.addCombatant("Batedor sem ficha",7,14,14,11,"",false);
        assertFalse(book.monsterSaveInfo(target).optBoolean("known"));
        try{book.registerMonsterConstitution(target,0,0,receipt());fail("Invalid D&D score");}
        catch(IllegalArgumentException expected){}
        assertEquals(3,book.registerMonsterConstitution(target,16,0,receipt()).getInt("bonus"));
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");
        book=new WilemCookbookEngine(db);
        assertEquals(3,book.monsterSaveInfo(target).getInt("bonus"));
        assertEquals("master_confirmed_profile",book.monsterSaveInfo(target).getString("authority"));
    }
    @Test public void proseMarketCannotInventStockOrChargeMoney()throws Exception{
        join();
        db.saveSceneState(new JSONObject().put("location","Mercado de Strixhaven, loja e feira"));
        assertEquals(0,book.marketOffers().length());
        try{book.acquire("batata",1,"mercado_pago",true,receipt());fail("No stock");}
        catch(IllegalStateException expected){}
        assertEquals(0,book.pantry().getJSONArray("items").length());
    }
    @Test public void vendorPurchasesPersistStockAndRollbackInvalidAmount()throws Exception{
        join();openMarket("Loja de Mina",2,"shipment-01",4,0,0,0);
        JSONObject sheet=new JSONObject(db.getCharacter(wilem).optString("sheet_json","{}"));
        sheet.put("inventory_currency_gp",1d);db.updateCharacterSheetChecked(wilem,sheet.toString());
        JSONObject bought=book.buyFromMarket("batata",3,book.marketOffers().getJSONObject(0)
            .getString("source_key"),receipt());
        assertEquals(6,bought.getInt("paid_copper"));
        assertEquals(1,book.marketOffers().getJSONObject(0).getInt("quantity"));
        try{book.buyFromMarket("batata",2,bought.getString("source_key"),receipt());fail("Not enough stock");}
        catch(IllegalStateException expected){}
        assertEquals(.94d,new JSONObject(db.getCharacter(wilem).getString("sheet_json"))
            .getDouble("inventory_currency_gp"),.0001);
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");book=new WilemCookbookEngine(db);
        assertEquals(1,book.marketOffers().getJSONObject(0).getInt("quantity"));
        assertEquals(3,book.pantry().getJSONArray("items").getJSONObject(0).getInt("quantity"));
    }
    @Test public void finiteForageNodeCannotDuplicateAcrossDayIfStockIsExhausted()throws Exception{
        join();forageNode("Horta ao lado da escola",1,1);
        java.util.List<Integer> faces=java.util.Collections.singletonList(20);
        Dice3DOverlay.RollResult d20=new Dice3DOverlay.RollResult(
            faces,faces,20,20,"20","","d20","Normal",100,2468L,"test",
            RollIntegrity.signature("d20",faces,0,2468L,20));
        assertEquals(1,book.forageWithPhysicalRoll("batata",d20,receipt()).getInt("quantity"));
        assertEquals(0,book.forageableIngredients().length());
        forageNode("Horta ao lado da escola",2,1);
        assertEquals(0,book.forageableIngredients().length());
    }
    private void prepareSyntheticBait(String id,String recipe,String scene)throws Exception{
        JSONObject st=book.state();
        st.getJSONArray("baits").put(new JSONObject().put("id",id)
            .put("recipe_id",recipe).put("status","armada")
            .put("scene",scene).put("batch_quality","somente_beneficio"));
        db.getWritableDatabase().execSQL(
            "UPDATE wilem_recipe_books SET state_json=? WHERE world_profile=? AND character_id=?",
            new Object[]{st.toString(),"dnd",wilem});
    }
    private Dice3DOverlay.RollResult d20(int face,long seed)throws Exception{
        java.util.List<Integer> f=java.util.Collections.singletonList(face);
        return new Dice3DOverlay.RollResult(f,f,face,face,String.valueOf(face),"","d20","Normal",
            110,seed,"test",RollIntegrity.signature("d20",f,0,seed,face));
    }
    @Test public void aValidNegativePhysicalConstitutionTotalIsNotRejected()throws Exception{
        join();prepareSyntheticBait("bait-neg-save","isca_mel_fantasia","");
        long creature=db.addCombatant("Rato muito debilitado",10,8,8,10,"",false);
        db.updateCombatantAdvancedChecked(creature,0,"",false,0,0,"","[]",
            "","","unconscious","{}");
        assertEquals(-5,book.registerMonsterConstitution(creature,1,0,receipt()).getInt("bonus"));
        JSONObject resolved=book.resolveBaitWithPhysicalSave("bait-neg-save",creature,true,true,
            false,d20(1,98761L),999,false,receipt());
        assertEquals(-4,resolved.getInt("save_total"));
        assertTrue(resolved.getBoolean("immunity_auto_detected"));
        assertFalse(resolved.getBoolean("effect_applied"));
        assertEquals(-4,resolved.getJSONObject("save_proof").getInt("total"));
    }
    @Test public void allCanonicalImmunityOriginsApplyToBaits()throws Exception{
        join();prepareSyntheticBait("bait-native-immunity","isca_mel_fantasia","");
        long nativeCreature=db.addCombatant("Criatura imune",10,8,8,10,"",false);
        db.updateCombatantAdvancedChecked(nativeCreature,0,"",false,0,0,"","[]",
            "","","inconsciente","{}");
        book.registerMonsterConstitution(nativeCreature,10,0,receipt());
        JSONObject nativeResult=book.resolveBaitWithPhysicalSave("bait-native-immunity",
            nativeCreature,true,true,false,d20(1,67342L),0,false,receipt());
        assertTrue("Top level combatant immunity not respected",nativeResult.optBoolean("immunity_auto_detected"));
        assertFalse(nativeResult.optBoolean("effect_applied"));

        prepareSyntheticBait("bait-npc-immunity","isca_mel_fantasia","");
        db.upsertNpcMemory(new JSONObject().put("name","Guardiao ancestral")
            .put("active",1).put("stats_json",new JSONObject().put("con",12)
                .put("condition_immunities",new JSONArray().put("unconscious")).toString()));
        long npcId=db.findNpcMemoryByName("Guardiao ancestral").getLong("id");
        long npc=db.addCombatantWithVisual("Guardiao ancestral",10,20,20,12,"",
            false,"npc",npcId,"");
        JSONObject npcResult=book.resolveBaitWithPhysicalSave("bait-npc-immunity",npc,
            true,true,false,d20(1,67343L),0,false,receipt());
        assertTrue("NPC declared condition immunity not respected",npcResult.optBoolean("immunity_auto_detected"));
        assertEquals("npc_canonical_stats",npcResult.getJSONObject("save_proof").getString("bonus_authority"));
    }
    @Test public void explicitImmunityPhrasesAreAcceptedButNegatedOnesAreNot()throws Exception{
        join();
        prepareSyntheticBait("bait-immune-prose","isca_mel_fantasia","");
        long creature=db.addCombatant("Aranha imune",9,10,10,10,"",false);
        db.updateCombatantAdvancedChecked(creature,0,"",false,0,0,"","[]",
            "","","immune to unconscious","{}");
        book.registerMonsterConstitution(creature,10,0,receipt());
        JSONObject result=book.resolveBaitWithPhysicalSave("bait-immune-prose",
            creature,true,true,false,d20(1,72512L),0,false,receipt());
        assertTrue(result.optBoolean("immunity_auto_detected"));
        assertFalse(result.optBoolean("effect_applied"));
        // Negated descriptions cannot be interpreted as condition immunity.
        prepareSyntheticBait("bait-not-immune","isca_mel_fantasia","");
        String persistedMechanics=db.getCombatant(creature).optString("resources_json","{}");
        db.updateCombatantAdvancedChecked(creature,0,"",false,0,0,"","[]",
            "","","not immune to sleep",persistedMechanics);
        try{book.resolveBaitWithPhysicalSave("bait-not-immune",creature,true,true,
            false,d20(1,72513L),0,false,receipt());
            fail("Without immunity an actual active encounter is needed for the effect");}
        catch(IllegalStateException expected){
            assertTrue(expected.getMessage().contains("encontro ativo"));
        }
        assertFalse("Failed resolution must not consume bait",
            book.state().getJSONArray("baits").getJSONObject(1).optBoolean("consumed"));
    }
    @Test public void baitCannotHitDifferentSceneOrDeadCreature()throws Exception{
        join();
        db.saveSceneState(new JSONObject().put("location","Floresta do Vale"));
        prepareSyntheticBait("bait-far-away","isca_mel","Marco do Percurso");
        long creature=db.addCombatant("Lobo",10,8,8,10,"",false);
        try{book.resolveBait("bait-far-away",creature,true,true,false,0,receipt());
            fail("A scene-bound bait cannot hit target in a different scene");}
        catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("cena"));}
        assertEquals("armada",book.state().getJSONArray("baits").getJSONObject(0).getString("status"));
        prepareSyntheticBait("bait-dead-target","isca_mel","Floresta do Vale");
        long dead=db.addCombatant("Cadaver",10,0,8,10,"",false);
        try{book.resolveBait("bait-dead-target",dead,true,true,false,0,receipt());
            fail("Dead creature cannot ingest bait");}
        catch(IllegalArgumentException expected){}
    }
    @Test public void sameHexDifferentPoiCannotSellGoods()throws Exception{
        JSONObject actualScene=new JSONObject().put("location","Pousada Marco do Percurso");
        JSONObject unrelatedShop=new JSONObject().put("location_id","merchant-01")
            .put("name","Mercado de Red Larch");
        assertFalse(WilemWorldSupply.atCurrentPointOfSale(actualScene,unrelatedShop));
        assertFalse(WilemWorldSupply.atCurrentPointOfSale(
            new JSONObject().put("location","Mercado"),unrelatedShop));
        assertTrue(WilemWorldSupply.atCurrentPointOfSale(
            new JSONObject().put("location","Mercado de Red Larch"),unrelatedShop));
        assertTrue(WilemWorldSupply.atCurrentPointOfSale(
            new JSONObject().put("location","merchant-01"),unrelatedShop));
        assertFalse(WilemWorldSupply.atCurrentPointOfSale(
            new JSONObject().put("location",""),unrelatedShop));
    }
    @Test public void forageNodeIsBoundToThePlaceWhereItWasFound()throws Exception{
        join();forageNode("Horta de Strixhaven",3,3);
        assertTrue(book.forageableIngredients().length()>0);
        JSONObject elsewhere=db.getSceneState();
        elsewhere.put("location","Quarto na pousada");
        // Even if clue data leaks across scene transitions, it cannot grant ingredients.
        db.saveSceneState(elsewhere);
        assertEquals(0,book.forageableIngredients().length());
    }
    @Test public void receiptsCannotReplayAfterSlidingWindowOrDatabaseReopen()throws Exception{
        join();
        String first="wilem-receipt-unique-first-"+System.nanoTime();
        book.addNote("Primeira anotacao","Historico da cozinha",first);
        for(int i=0;i<131;i++)book.addNote("Anotacao "+i,
            "Registro persistente "+i,"wilem-receipt-unique-"+i+"-"+name);
        assertFalse("Old receipt is outside the 120-element display cache",
            book.state().getJSONArray("receipts").toString().contains(first));
        try{book.addNote("Nota duplicada","Nunca duplicar",first);
            fail("Sliding window must not defeat unique durable ledger");}
        catch(IllegalStateException expected){}
        int before=book.state().getJSONArray("notes").length();
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");book=new WilemCookbookEngine(db);
        try{book.addNote("Nota apos reabrir","Nao repetir recibo",first);
            fail("Reopening must not reset durable receipt ledger");}
        catch(IllegalStateException expected){}
        assertEquals(before,book.state().getJSONArray("notes").length());
    }
    @Test public void legacyWindowReceiptsMigrateWithoutLosingPreviousState()throws Exception{
        join();
        JSONObject old=book.state();
        old.put("durable_receipts_v2",false);
        String legacy="wilem-legacy-locked-receipt";
        old.getJSONArray("receipts").put(legacy);
        db.getWritableDatabase().execSQL(
            "UPDATE wilem_recipe_books SET state_json=? WHERE world_profile=? AND character_id=?",
            new Object[]{old.toString(),"dnd",wilem});
        assertTrue(book.state().optBoolean("durable_receipts_v2"));
        try{book.addNote("Reuso legado","Nao pode repetir",legacy);
            fail("Old receipt must migrate to SQLite ledger");}
        catch(IllegalStateException expected){}
        assertEquals(old.getJSONArray("notes").length(),book.state().getJSONArray("notes").length());
    }
    @Test public void expiringBaitPreservesUnrelatedCombatResourcesAndPrunesOwnTimer()throws Exception{
        join();
        long enemy=db.addCombatant("Lobo em combate",8,12,12,10,"",false);
        JSONObject resources=new JSONObject().put("wilem_bait_source","bait-expire")
            .put("concentration_save_pending",true)
            .put("concentration_save_queue",new JSONArray().put("magic"));
        JSONArray timers=new JSONArray().put(new JSONObject()
            .put("schema",MonsterConditionTimerLedger.SCHEMA)
            .put("name","inconsciente").put("rounds",1)
            .put("source_actor_id",wilem).put("trigger","target_turn_start")
            .put("source_statblock","wilem_cookbook")
            .put("source_action","isca_sonolencia").put("source_receipt","wilem-owned-test"));
        db.updateCombatantAdvancedChecked(enemy,0,"",false,0,0,
            "inconsciente,paralisado",timers.toString(),"","","",resources.toString());
        // Simulate pending check after previous combat effects; manual condition
        // removal must invalidate it rather than resurrecting stale resources.
        db.getWritableDatabase().execSQL(
            "UPDATE combatants SET resources_json=? WHERE id=?",
            new Object[]{resources.toString(),enemy});
        JSONObject state=book.state();
        state.getJSONArray("baits").put(new JSONObject().put("id","bait-expire")
            .put("status","resolvida").put("effect_applied",true)
            .put("target_id",enemy).put("applied_condition","inconsciente")
            .put("ends_world_hour",-1).put("ends_combat_round",100000));
        db.getWritableDatabase().execSQL(
            "UPDATE wilem_recipe_books SET state_json=? WHERE world_profile=? AND character_id=?",
            new Object[]{state.toString(),"dnd",wilem});
        assertEquals("expirada",book.state().getJSONArray("baits").getJSONObject(0).getString("status"));
        JSONObject after=db.getCombatant(enemy),res=new JSONObject(after.getString("resources_json"));
        assertEquals("paralisado",after.optString("conditions"));
        assertFalse(res.has("wilem_bait_source"));
        assertFalse(res.has("concentration_save_pending"));
        assertFalse(res.has("concentration_save_queue"));
        assertEquals(0,new JSONArray(after.getString("condition_timers_json")).length());
    }
    @Test public void forageRollCannotSwitchToAnotherSceneOrRestockRevision()throws Exception{
        join();
        forageNode("Campo de nabos",1,5);
        JSONObject selected=book.forageableIngredients().getJSONObject(0);
        assertEquals("batata",selected.optString("ingredient_id"));
        db.saveSceneState(new JSONObject().put("location","Bosque distante")
            .put("day_number",1).put("clues_json",new JSONArray().put(new JSONObject()
                .put("type","wilem_forage_node").put("id","bosque-outro-recurso")
                .put("ingredient_id","batata").put("location","Bosque distante")
                .put("revision","first").put("quantity",3)).toString()));
        try{book.forageWithPhysicalRoll("batata",selected.optString("source_key"),
            selected.optString("stock_revision"),d20(20,82631L),receipt());
            fail("Old resource roll must not become harvest in another place");}
        catch(IllegalStateException expected){}
        assertEquals(0,book.pantry().getJSONArray("items").length());
    }
    @Test public void noIngredientCreatesNoFreeMealsAndTransactionRollsBack()throws Exception{
        join();int entries=book.state().getJSONArray("cooked").length();
        try{book.cook("com_batata_0",0,true,true,receipt());fail("No potatoes");}
        catch(IllegalStateException expected){}
        assertEquals(entries,book.state().getJSONArray("cooked").length());
        assertEquals(0,book.pantry().getJSONArray("items").length());
    }
    @Test public void fantasyBaitRequiresExplicitConsumptionAndSave()throws Exception{
        join();seedIngredient("mel",1);seedIngredient("maça",1);
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
