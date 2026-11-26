package com.braseiro.dnd5e;

import android.content.Context;
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

/** Regression for 6.6.68: fast startup, safe retry and permanent world memory. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class DndStartupContinuityPerformanceRobolectricTest {
    private Context ctx;
    private AppDb db;
    private String filename;

    @Before public void setup(){
        ctx=RuntimeEnvironment.getApplication();
        filename="dnd-startup-6668-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(ctx,filename);
        db.setActiveMapProfile("dnd");
    }

    @After public void tearDown(){
        if(db!=null)db.close();
        ctx.deleteDatabase(filename);
    }

    @Test public void retryKeyIgnoresDisplayAndLocationCorrectionsButChangesWithAcceptedNarration() throws Exception {
        JSONObject story=new JSONObject().put("started",1)
            .put("last_narration","Kote permanece atrás do balcão.");
        String stable=ConfirmedSceneRetryIdentity.of("dnd",story);
        assertEquals(stable,ConfirmedSceneRetryIdentity.of("dnd",
            new JSONObject().put("started",1)
                .put("last_narration","Kote permanece atrás do balcão.")
                .put("scene_location","Newarre")
                .put("campaign_title","Fugitivos de Strixhaven")));
        assertNotEquals(stable,ConfirmedSceneRetryIdentity.of("dnd",
            new JSONObject().put("started",1)
                .put("last_narration","Kote entrega o pão após a ação de Kvothe.")));
        assertNotEquals(stable,ConfirmedSceneRetryIdentity.of("mystara",story));
    }

    @Test public void existingLocalSceneOpeningUsesTheFastNonTickRulePath() {
        String anchored="INÍCIO AUTOMÁTICO — CONTINUAÇÃO DA CENA EXISTENTE. "+
            "A companhia já está na Pousada Marco do Percurso.";
        assertTrue(RuleRetrievalEngine.isNarrativeOpeningRequest(anchored));
        assertTrue(RuleRetrievalEngine.isNarrativeOpeningRequest(
            "INÍCIO AUTOMÁTICO — ABERTURA DE HISTÓRIA."));
        assertFalse(RuleRetrievalEngine.isNarrativeOpeningRequest(
            "Kvothe bate discretamente a caneca na mesa."));
    }

    @Test public void boundedMasterJournalNeverDeletesChronicles() throws Exception {
        for(int i=0;i<145;i++)
            db.addJournal("Cena "+i,"Kvothe e os amigos continuam no mundo.","Narrador");
        assertEquals(145,db.listJournal().size());
        assertTrue(db.listJournalForMasterContext().size()<=160);
        assertTrue(db.listJournalForMasterContext().size()>=100);
        assertTrue(db.listJournalForMasterContext().get(0)
            .optString("title","").contains("144"));
        assertEquals(145,db.listJournal().size());
    }
}
