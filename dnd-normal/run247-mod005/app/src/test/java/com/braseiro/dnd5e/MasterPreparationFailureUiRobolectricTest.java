package com.braseiro.dnd5e;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Actual Activity failure route: the action remains retryable and the game state does not advance. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class MasterPreparationFailureUiRobolectricTest {
    static void set(MainActivity a,String field,Object value) throws Exception {
        Field f=MainActivity.class.getDeclaredField(field);
        f.setAccessible(true);f.set(a,value);
    }
    static Object get(MainActivity a,String field) throws Exception {
        Field f=MainActivity.class.getDeclaredField(field);
        f.setAccessible(true);return f.get(a);
    }

    @Test public void preGeminiFailureUnlocksUiWithoutInventingAnyScene() throws Exception {
        MainActivity a=Robolectric.buildActivity(MainActivity.class).get();
        String name="pre-gemini-timeout-"+System.nanoTime()+".db";
        AppDb db=new AppDb(a,name);
        try{
            db.setActiveMapProfile("dnd");
            String beforeScene=db.getSceneState().toString();
            String beforeStory=db.getStoryState().toString();
            SharedPreferences pref=a.getSharedPreferences("pre-gemini-safe-retry-"+System.nanoTime(),0);
            TextView status=new TextView(a);
            set(a,"prefs",pref);set(a,"db",db);
            set(a,"ui",new Handler(Looper.getMainLooper()));
            set(a,"statusText",status);
            set(a,"currentScreen","Diagnóstico");
            set(a,"showingGeminiFailureDialog",true); // avoid an incidental modal in this pure-state test
            set(a,"aiBusy",true);
            Method fail=MainActivity.class.getDeclaredMethod("markMasterPreparationFailed",
                String.class,String.class,String.class,String.class);
            fail.setAccessible(true);
            fail.invoke(a,"Narrador","Pergunto a Urzmaktok sobre a caça de orientação",
                "PLAYER_ACTION","Tempo limite de 30 s na montagem do contexto");
            assertFalse("Stuck busy input must be released",(Boolean)get(a,"aiBusy"));
            assertTrue("Not an invented Gemini failure",status.getText().toString().contains("contexto do Mestre"));
            assertTrue("Still retryable",pref.getBoolean("run188_gemini_failed_pending",false));
            assertTrue("Accurate failure class",pref.getBoolean("run188_gemini_failed_context_failure",false));
            assertFalse("No Gemini was called",pref.getBoolean("run188_gemini_failed_api_failure",true));
            assertEquals("Original action is preserved exactly",
                "Pergunto a Urzmaktok sobre a caça de orientação",
                pref.getString("run188_gemini_failed_action",""));
            assertEquals("Retried turn must use PLAY action channel","PLAYER_ACTION",
                pref.getString("run188_gemini_failed_channel",""));
            assertEquals("World clock, position and context must not mutate",beforeScene,db.getSceneState().toString());
            assertEquals("Nothing narrated or journalled",beforeStory,db.getStoryState().toString());
        }finally{db.close();a.deleteDatabase(name);a.finish();}
    }
}
