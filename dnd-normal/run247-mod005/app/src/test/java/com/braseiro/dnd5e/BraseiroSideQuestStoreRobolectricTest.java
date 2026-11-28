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
import static org.junit.Assert.*;

/** Real Android/SQLite regression for independent D&D 2024 sidequest packages. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class BraseiroSideQuestStoreRobolectricTest {
    private Context context;
    private AppDb db;
    private String filename;

    @Before public void setup(){
        context=RuntimeEnvironment.getApplication();
        filename="dnd-sidequests-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(context,filename);
        db.setActiveMapProfile("dnd");
    }
    @After public void teardown(){
        if(db!=null)db.close();
        context.deleteDatabase(filename);
    }
    private JSONObject sample(String id,String title) throws Exception {
        JSONObject entry=new JSONObject().put("id","tres_pedras").put("name","Estalagem das Três Pedras")
                .put("parentPlaceId","bryn_shander").put("atlasHex",JSONObject.NULL);
        JSONArray locs=new JSONArray()
            .put(new JSONObject().put("id","bryn_shander").put("name","Bryn Shander").put("atlasHex",JSONObject.NULL))
            .put(entry)
            .put(new JSONObject().put("id","luskan").put("name","Luskan").put("atlasHex",JSONObject.NULL));
        return new JSONObject()
            .put("schema","braseiro.adventure/v1")
            .put("id",id).put("title",title).put("system","dnd5e-2024")
            .put("worldId","faerun-campanha-principal").put("timelineId","principal")
            .put("start",new JSONObject().put("locationId","tres_pedras")
                .put("hook","Rumores sobre expedições perdidas e riquezas desconhecidas circulam na estalagem.")
                .put("notAccepted",true).put("noForcedTravel",true))
            .put("story",new JSONObject().put("premise","Caverna remota")
                .put("background","SECRETO: O oráculo comunica eventos da Terra.")
                .put("oralHistory","Entre as montanhas, comenta-se sobre vozes que nenhum guia confirma."))
            .put("locations",locs)
            .put("npcs",new JSONArray()).put("objectives",new JSONArray()).put("encounters",new JSONArray())
            .put("secrets",new JSONArray().put(new JSONObject().put("id","s01")
                .put("visibility","gm_only").put("truth","SEGREDO_QUE_NAO_PODE_VAZAR")))
            .put("sideMissionsPrivate",new JSONArray().put(new JSONObject()
                .put("id","m05").put("place","luskan")
                .put("hook","Um armazém entrega comida para um destinatário desconhecido.")));
    }
    @Test public void importingSeveralDoesNotReplaceMainCampaign() throws Exception {
        BraseiroAdventureStore main=new BraseiroAdventureStore(db);
        JSONObject principal=sample("main.quest","Aventura principal");
        main.importJson(principal.toString());
        BraseiroSideQuestStore side=new BraseiroSideQuestStore(db);
        assertTrue(side.importJson(sample("quest.one","Primeira secundária").toString()).optBoolean("ok"));
        assertTrue(side.importJson(sample("quest.two","Segunda secundária").toString()).optBoolean("ok"));
        assertEquals(2,side.listInstalled().length());
        assertEquals("main.quest",main.masterContext().optString("activeId"));
        assertEquals(2,db.queryOne("SELECT COUNT(*) AS n FROM braseiro_sidequest_modules",
                new String[]{}).optInt("n"));
        assertEquals(0,db.listQuests("active",100).size()); // no automatic acceptance
    }
    @Test public void onlyConfirmedRegionAndPlaceMayOfferHooks() throws Exception {
        JSONObject pkg=sample("local.quest","Regional");
        assertEquals(0,BraseiroSideQuestStore.eligibleRumors(pkg,"Waterdeep","").length());
        assertEquals(0,BraseiroSideQuestStore.eligibleRumors(pkg,"Esta região","").length());
        JSONArray city=BraseiroSideQuestStore.eligibleRumors(pkg,"Bryn Shander","");
        assertEquals(1,city.length());
        assertEquals("regional",city.getJSONObject(0).optString("id"));
        assertFalse(city.toString().contains("SEGREDO_QUE_NAO_PODE_VAZAR"));
        JSONArray inn=BraseiroSideQuestStore.eligibleRumors(pkg,"Estalagem das Três Pedras","");
        assertEquals("opening",inn.getJSONObject(0).optString("id"));
        JSONArray harbor=BraseiroSideQuestStore.eligibleRumors(pkg,"Luskan","");
        assertEquals(1,harbor.length());
        assertEquals("side:m05",harbor.getJSONObject(0).optString("id"));
        assertFalse(harbor.toString().contains("oráculo"));
        assertFalse(harbor.toString().contains("SECRETO:"));
    }
    @Test public void idempotentImportPauseAndExplicitRumorArePersisted() throws Exception {
        BraseiroSideQuestStore side=new BraseiroSideQuestStore(db);
        JSONObject pkg=sample("quest.one","Primeira secundária");
        side.importJson(pkg.toString());
        assertTrue(side.importJson(pkg.toString()).optBoolean("alreadyInstalled"));
        side.setEnabled("quest.one",false);
        assertFalse(side.listInstalled().getJSONObject(0).getBoolean("enabled"));
        assertFalse(side.recordConfirmedRumor("quest.one","regional","conversa","Bryn Shander",3));
        side.setEnabled("quest.one",true);
        assertFalse(side.recordConfirmedRumor("quest.one","regional","conversa","Águas Profundas",3));
        assertTrue(side.recordConfirmedRumor("quest.one","regional","conversa","Bryn Shander",3));
        assertFalse(side.recordConfirmedRumor("quest.one","regional","conversa","Bryn Shander",3));
        db.close();db=new AppDb(context,filename);db.setActiveMapProfile("dnd");
        assertEquals(1,new BraseiroSideQuestStore(db).listInstalled().length());
        assertEquals(1,db.queryOne("SELECT COUNT(*) AS n FROM braseiro_sidequest_rumors",
                new String[]{}).optInt("n"));
    }
    @Test public void fullBackupRoundTripKeepsAllModulesAndNoSpoilerLeak() throws Exception {
        BraseiroSideQuestStore side=new BraseiroSideQuestStore(db);
        side.importJson(sample("q.one","Primeira").toString());
        side.importJson(sample("q.two","Segunda").toString());
        side.setEnabled("q.two",false);
        JSONObject context=side.masterContext();
        assertFalse(context.toString().contains("SEGREDO_QUE_NAO_PODE_VAZAR"));
        assertFalse(context.toString().contains("SECRETO:"));
        JSONObject root=new JSONObject();
        new BraseiroAdventureStore(db).attachPortableBackup(root);
        assertEquals(2,root.getJSONArray("braseiroSideQuestModules").length());
        side.remove("q.one");side.remove("q.two");
        assertEquals(0,side.listInstalled().length());
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            @Override public void run()throws Exception {
                new BraseiroAdventureStore(db).restorePortableBackup(db.getWritableDatabase(),root,"dnd");
            }
        });
        JSONArray restored=side.listInstalled();
        assertEquals(2,restored.length());
        assertFalse(restored.getJSONObject(1).optBoolean("enabled"));
    }
    @Test public void followingIsExplicitAndDoesNotTouchWorldState() throws Exception {
        BraseiroAdventureStore main=new BraseiroAdventureStore(db);
        main.importJson(sample("principal","Campanha principal").toString());
        BraseiroSideQuestStore side=new BraseiroSideQuestStore(db);
        side.importJson(sample("optional","Missão secundária").toString());
        side.setFollowing("optional",true);
        assertEquals("following",side.listInstalled().getJSONObject(0).optString("state"));
        assertEquals("principal",main.masterContext().optString("activeId"));
        assertEquals(0,db.listQuests("active",100).size());
        side.setFollowing("optional",false);
        assertEquals("unaccepted",side.listInstalled().getJSONObject(0).optString("state"));
    }
    @Test public void playableLocalMaterialIsGatedAndRemainsPrivate() throws Exception {
        JSONObject pkg=sample("local","Aventura");
        pkg.getJSONArray("encounters").put(new JSONObject().put("id","e01")
            .put("locationId","luskan").put("type","social")
            .put("description","Uma cobrança inesperada nas docas.")
            .put("trigger","Somente quando o host confirmar a localização."));
        pkg.put("clues",new JSONArray().put(new JSONObject()
            .put("id","c01").put("locationId","luskan")
            .put("description","Um documento tem assinatura incompleta.")
            .put("revealCondition","investigação efetivamente realizada")));
        pkg.getJSONArray("npcs").put(new JSONObject().put("id","npc01")
            .put("name","Lena Fen").put("publicRole","vigia").put("appearance","sobretudo azul")
            .put("secret","SEGREDO_QUE_NAO_PODE_VAZAR")
            .put("places",new JSONArray().put("luskan")));
        JSONObject far=BraseiroSideQuestStore.localSceneMaterial(pkg,"Waterdeep","");
        assertEquals(0,far.getJSONArray("optional_encounters").length());
        assertEquals(0,far.getJSONArray("conditional_clues_private").length());
        JSONObject near=BraseiroSideQuestStore.localSceneMaterial(pkg,"Luskan","");
        assertEquals(1,near.getJSONArray("optional_encounters").length());
        assertEquals(1,near.getJSONArray("conditional_clues_private").length());
        assertEquals(1,near.getJSONArray("possible_npcs_NOT_spawned").length());
        assertFalse(near.toString().contains("SEGREDO_QUE_NAO_PODE_VAZAR"));
    }
    @Test public void legacyStrixhavenPrincipalCannotBlockIndependentFaerunModule() throws Exception {
        BraseiroAdventureStore main=new BraseiroAdventureStore(db);
        JSONObject principal=sample("strixhaven.fugitivos","Os Quatro de Strixhaven - Fugitivos")
            .put("worldId","arcavios-strixhaven").put("timelineId","academia");
        main.importJson(principal.toString());
        BraseiroSideQuestStore side=new BraseiroSideQuestStore(db);
        JSONObject installed=side.importJson(sample("eco.entre.mundos","O Eco entre os Mundos").toString());
        assertTrue(installed.optBoolean("ok"));
        assertFalse(installed.optBoolean("started",true));
        assertEquals(1,side.listInstalled().length());
        assertEquals("strixhaven.fugitivos",main.masterContext().optString("activeId"));
        assertEquals("arcavios-strixhaven",main.masterContext().optString("worldId"));
        assertEquals("academia",main.masterContext().optString("timelineId"));
        JSONObject row=db.queryOne("SELECT world_id,timeline_id,state FROM braseiro_sidequest_modules WHERE module_id=?",
            new String[]{"eco.entre.mundos"});
        assertEquals("faerun-campanha-principal",row.optString("world_id"));
        assertEquals("principal",row.optString("timeline_id"));
        assertEquals("unaccepted",row.optString("state"));
        assertEquals(0,db.listQuests("active",100).size());
    }

    @Test public void fugitivesFromStrixhavenAreAlreadyInFaerunAndCanImportOptionalModule() throws Exception {
        // Campaign TITLE mentions Strixhaven, but the party is already in Faerun.
        // The main adventure may retain its own original world/timeline identifiers.
        BraseiroAdventureStore main=new BraseiroAdventureStore(db);
        JSONObject fugitives=sample("quatro.fugitivos","Os Quatro de Strixhaven - Fugitivos")
            .put("worldId","faerun-fugitivos").put("timelineId","fuga-em-faerun");
        main.importJson(fugitives.toString());
        BraseiroSideQuestStore side=new BraseiroSideQuestStore(db);
        assertEquals("dnd",db.getActiveMapProfile());
        JSONObject result=side.importJson(sample("eco.entre.mundos","O Eco entre os Mundos").toString());
        assertTrue(result.optBoolean("ok"));
        assertFalse(result.optBoolean("started",true));
        assertEquals(1,side.listInstalled().length());
        JSONObject preserved=main.masterContext();
        assertEquals("quatro.fugitivos",preserved.optString("activeId"));
        assertEquals("faerun-fugitivos",preserved.optString("worldId"));
        assertEquals("fuga-em-faerun",preserved.optString("timelineId"));
        assertEquals(0,db.listQuests("active",100).size());
    }

    @Test public void optionalPackageFromAnotherWorldStillRejected() throws Exception {
        BraseiroAdventureStore main=new BraseiroAdventureStore(db);
        main.importJson(sample("principal","Principal").toString());
        JSONObject other=sample("eberron.mod","Outro mundo").put("worldId","eberron-campanha");
        try {
            new BraseiroSideQuestStore(db).importJson(other.toString());
            fail("Other-world module should never enter the Faerun sidequest library");
        }catch(IllegalArgumentException expected){
            assertTrue(expected.getMessage().contains("Faerûn"));
        }
        assertEquals(0,new BraseiroSideQuestStore(db).listInstalled().length());
        assertEquals("principal",main.masterContext().optString("activeId"));
    }
    @Test public void remoteSidequestCannotReplaceKnownInnSceneOrKote() throws Exception {
        JSONObject scene=db.getSceneState();
        scene.put("location","Pousada Marco do Percurso");
        scene.put("hex_id","DO40");
        scene.put("present_npcs_json",new JSONArray().put(
            new JSONObject().put("name","Kote").put("role","estalajadeiro")
                .put("present",true)).toString());
        db.saveSceneState(scene);
        JSONObject before=db.getSceneState();
        JSONObject mapBefore=db.getMapState();
        BraseiroSideQuestStore optional=new BraseiroSideQuestStore(db);
        // This module's own start is an entirely DIFFERENT inn in the north.
        JSONObject distant=sample("echo.north","O Eco entre os Mundos");
        assertEquals("tres_pedras",distant.getJSONObject("start").getString("locationId"));
        assertTrue(optional.importJson(distant.toString()).optBoolean("ok"));
        JSONObject after=db.getSceneState();
        assertEquals(before.optString("location"),after.optString("location"));
        assertEquals(before.optString("hex_id"),after.optString("hex_id"));
        assertTrue(after.optString("present_npcs_json").contains("Kote"));
        assertEquals(before.optString("present_npcs_json"),after.optString("present_npcs_json"));
        assertEquals(mapBefore.toString(),db.getMapState().toString());
        assertEquals(0,optional.masterContext().optJSONArray("optional_modules_nearby").length());
        assertEquals(0,db.listQuests("active",100).size());
    }

}
