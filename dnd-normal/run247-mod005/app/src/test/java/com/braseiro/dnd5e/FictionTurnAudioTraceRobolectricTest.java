package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class FictionTurnAudioTraceRobolectricTest {
    Context app;SharedPreferences prefs;
    @Before public void setup(){
        app=RuntimeEnvironment.getApplication();
        prefs=app.getSharedPreferences("fiction-turn-qa-test",Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        FictionTurnTrace.setEnabled(app,prefs,false);
    }
    @After public void teardown()throws Exception{
        FictionTurnTrace.setEnabled(app,prefs,false);
        AppExecutors.serial().submit(()->{}).get(10,TimeUnit.SECONDS);
        prefs.edit().clear().commit();
    }
    @Test public void observedReplyIsTheOnlyTextSelectedForAudio(){
        String scene="Um orc aproxima-se da estufa.";
        String answer="Você consegue ver um avental e uma pá de madeira.";
        assertEquals(answer,FictionTurnAudio.select(scene,answer));
        assertTrue(FictionTurnAudio.observation(answer,answer));
        assertFalse(FictionTurnAudio.observation(scene,answer));
        assertEquals(scene,FictionTurnAudio.select(scene,""));
    }
    @Test public void diagnosticsDefaultOffNeverCreatesFile()throws Exception{
        File f=FictionTurnTrace.destination(app);if(f.exists())assertTrue(f.delete());
        FictionTurnTrace.append(app,prefs,"request",new JSONObject().put("request_id",31));
        AppExecutors.serial().submit(()->{}).get(10,TimeUnit.SECONDS);
        assertFalse(f.exists());
    }
    @Test public void userOptInAndOptOutRedactApiKeyAndDelete()throws Exception{
        FictionTurnTrace.setEnabled(app,prefs,true);
        assertTrue(FictionTurnTrace.isEnabled(prefs));
        String secret="AIzaabcdefghijklmnopqrstuvwxyzABC";
        FictionTurnTrace.append(app,prefs,"raw_model_response",new JSONObject()
            .put("request_id",42)
            .put("raw_response","Uma parede tem uma inscrição. "+secret)
            .put("access_token","Bearer mySuperSecretTokenFromSomewhere"));
        AppExecutors.serial().submit(()->{}).get(10,TimeUnit.SECONDS);
        File f=FictionTurnTrace.destination(app);
        assertTrue(f.exists());
        String data=new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8);
        assertTrue(data.contains("raw_model_response"));
        assertTrue(data.contains("request_id"));
        assertFalse(data.contains(secret));
        assertFalse(data.contains("mySuperSecretTokenFromSomewhere"));
        assertTrue(new JSONObject(data.trim()).optBoolean("local_only"));
        FictionTurnTrace.setEnabled(app,prefs,false);
        assertFalse(f.exists());
    }
}
