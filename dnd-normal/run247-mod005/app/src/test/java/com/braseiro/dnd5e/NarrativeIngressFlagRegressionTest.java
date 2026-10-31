package com.braseiro.dnd5e;

import java.lang.reflect.Method;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class NarrativeIngressFlagRegressionTest {
    private static JSONObject parse(MainActivity activity,String json) throws Exception {
        Method m=MainActivity.class.getDeclaredMethod("parseAiEnvelope",String.class);
        m.setAccessible(true);
        return (JSONObject)m.invoke(activity,json);
    }

    @Test public void rawLeakedNarrativeIsMarkedBeforeAnySanitization() throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            JSONObject source=new JSONObject().put("narration",
                "Auri desvia o olhar.\n\nO mundo não pausa enquanto essa decisão é tomada.")
                .put("options",new org.json.JSONArray().put(new JSONObject().put("action","Avançar")));
            JSONObject env=parse(controller.get(),source.toString());
            assertTrue("Leak provenance must survive parsing",env.optBoolean("_narration_leak_detected",false));
            assertEquals(1,env.optJSONArray("options").length());
        }finally{controller.pause().stop().destroy();}
    }

    @Test public void ordinaryQuotedNpcSpeechRemainsUnchangedAndSafe() throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            String dialogue="Auri pergunta: \"Quer conversar?\"\nWilem responde: \"Claro.\"";
            JSONObject env=parse(controller.get(),new JSONObject().put("narration",dialogue).toString());
            assertFalse(env.optBoolean("_narration_leak_detected",false));
            assertEquals(dialogue,env.optString("narration"));
        }finally{controller.pause().stop().destroy();}
    }
}
