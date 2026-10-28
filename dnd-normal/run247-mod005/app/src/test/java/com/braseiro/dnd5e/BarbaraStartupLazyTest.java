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
import java.lang.reflect.Field;
import java.util.UUID;
import static org.junit.Assert.*;

/** Regression: full Python/Chaquopy is never initialized while Activity creates the Master. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class BarbaraStartupLazyTest {
    private Context context;
    private AppDb db;
    private String dbName;

    @Before public void setUp() {
        context=RuntimeEnvironment.getApplication();
        dbName="barbara_lazy_"+UUID.randomUUID().toString().substring(0,8)+".db";
        db=new AppDb(context,dbName);
        db.setActiveMapProfile("dnd");
    }

    @After public void tearDown(){
        if(db!=null)db.close();
        if(context!=null)context.deleteDatabase(dbName);
    }

    @Test public void constructingBarbaraDoesNotStartPython() {
        BarbaraSupervisor supervisor=new BarbaraSupervisor(context);
        assertFalse("Python must not start in Barbara constructor",supervisor.isAvailable());
        JSONObject before=supervisor.diagnostic();
        assertFalse("Barbara must remain lazy until background preparation",before.optBoolean("initialized",true));
        assertFalse(supervisor.systemContract("dnd").optBoolean("available",true));
        assertFalse("Reading a diagnostic must not start Python",supervisor.diagnostic().optBoolean("initialized",true));
    }

    @Test public void masterConstructorKeepsRulesAndAdventureWithoutStartingPython() throws Exception {
        MasterOrchestrator master=new MasterOrchestrator(db,new PartyDirector(db),
            new WorldStateEngine(db),new AiQuotaGovernor(context),context);
        assertNotNull("Adventures must remain installed",master.getAdventureEngine());
        assertNotNull("Canonical lore must remain ready",master.getNarrativeEngine().getEntity("faerun_harpers"));
        Field f=MasterOrchestrator.class.getDeclaredField("barbara");
        f.setAccessible(true);
        BarbaraSupervisor supervisor=(BarbaraSupervisor)f.get(master);
        assertNotNull(supervisor);
        assertFalse("Master constructor must not synchronously initialize Chaquopy",
            supervisor.diagnostic().optBoolean("initialized",true));
    }

    @Test public void nullContextFallbackRemainsExplicitAfterFirstUse(){
        BarbaraSupervisor supervisor=new BarbaraSupervisor(null);
        assertFalse(supervisor.diagnostic().optBoolean("initialized",true));
        JSONObject plan=supervisor.plan("Entrar na taverna","normal",true,false,"continuation");
        assertFalse(plan.optBoolean("available",true));
        assertTrue(plan.optBoolean("fallback_java_policy",false));
        assertTrue("First plan must resolve initialization, not silently wait forever",
            supervisor.diagnostic().optBoolean("initialized",false));
    }
}
