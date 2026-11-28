package com.braseiro.dnd5e;

import android.content.SharedPreferences;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class MasterActionDurabilityRobolectricTest {
 @Test public void longActionSurvivesActivityReplacementBeforeAnyNetworkCall()throws Exception{
  MainActivity first=Robolectric.buildActivity(MainActivity.class).get();
  MainActivity reopened=Robolectric.buildActivity(MainActivity.class).get();
  String name="turn-durability-"+System.nanoTime();
  SharedPreferences prefs=first.getSharedPreferences(name,0);
  AppDb db=new AppDb(first,name+".db");
  try{
   db.setActiveMapProfile("dnd");
   MasterPreparationFailureUiRobolectricTest.set(first,"prefs",prefs);
   MasterPreparationFailureUiRobolectricTest.set(first,"db",db);
   MasterPreparationFailureUiRobolectricTest.set(reopened,"prefs",reopened.getSharedPreferences(name,0));
   MasterPreparationFailureUiRobolectricTest.set(reopened,"db",db);
   Method persist=MainActivity.class.getDeclaredMethod("persistMasterActionBeforeSend",String.class,String.class,String.class);
   persist.setAccessible(true);
   StringBuilder b=new StringBuilder("Kvothe: ");
   for(int i=0;i<300;i++)b.append("Examino a situação, pergunto a Kote e descrevo os detalhes. ");
   String action=b.toString();
   String before=db.getStoryState().toString();
   assertEquals(Boolean.TRUE,persist.invoke(first,"Narrador",action,"PLAYER_ACTION"));
   assertEquals(action,reopened.getSharedPreferences(name,0).getString("run188_gemini_failed_action",""));
   assertTrue(reopened.getSharedPreferences(name,0).getBoolean("run188_gemini_failed_pending",false));
   assertEquals(Boolean.TRUE,persist.invoke(reopened,"Narrador",action,"PLAYER_ACTION"));
   assertEquals(before,db.getStoryState().toString());
   Method clear=MainActivity.class.getDeclaredMethod("clearFailedGeminiRequest");
   clear.setAccessible(true);clear.invoke(reopened);
   assertFalse(prefs.getBoolean("run188_gemini_failed_pending",false));
  }finally{db.close();first.deleteDatabase(name+".db");prefs.edit().clear().commit();}
 }
}