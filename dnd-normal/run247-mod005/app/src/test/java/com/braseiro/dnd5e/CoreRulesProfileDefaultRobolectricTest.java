package com.braseiro.dnd5e;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** New D&D campaigns use the 2024 core books without converting saved campaigns. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class CoreRulesProfileDefaultRobolectricTest {
    @Test public void newCampaignDefaultsTo2024AndSaved2014Stays2014AfterReopen() {
        Context context=RuntimeEnvironment.getApplication();
        String dbName="core_rules_default_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            assertEquals(DndRulesEngine.PROFILE_2024,db.getCampaign().optString("rules_profile"));
            db.saveCampaign("Campanha 2014",DndRulesEngine.PROFILE_2014,
                "Fantasia heroica",true,true,false,"");
            assertEquals(DndRulesEngine.PROFILE_2014,db.getCampaign().optString("rules_profile"));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            assertEquals(DndRulesEngine.PROFILE_2014,db.getCampaign().optString("rules_profile"));
            assertEquals(DndRulesEngine.PROFILE_2014,db.getCampaign().optString("rules_profile"));
        } finally {
            db.close();context.deleteDatabase(dbName);
        }
    }
}
