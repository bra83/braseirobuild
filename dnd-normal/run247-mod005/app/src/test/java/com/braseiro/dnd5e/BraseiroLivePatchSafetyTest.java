package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import java.io.InputStream;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class BraseiroLivePatchSafetyTest {
    private Context app;
    private SharedPreferences preferences;
    @Before public void init(){
        app=RuntimeEnvironment.getApplication();
        preferences=app.getSharedPreferences("braseiro_livepatch_dnd",Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }
    @After public void clear(){
        preferences.edit().clear().commit();
    }
    @Test public void defaultInstallationIsUnchanged(){
        assertEquals("Nenhuma correção incremental aplicada.",BraseiroLivePatch.currentRevision(app));
        assertEquals(-1,preferences.getInt("active",-1));
    }
    @Test public void firstPatchCanAlwaysRollbackToOriginalApkAssets(){
        preferences.edit().putInt("active",42).putInt("previous",-1).commit();
        String result=BraseiroLivePatch.rollback(app);
        assertTrue(result.contains("originais"));
        assertEquals(-1,preferences.getInt("active",-2));
        assertEquals(42,preferences.getInt("previous",-1));
    }
    @Test public void missingPreviousRevisionNeverDisablesActivePatch(){
        preferences.edit().putInt("active",42).putInt("previous",41).commit();
        assertTrue(BraseiroLivePatch.rollback(app).contains("não encontrada"));
        assertEquals(42,preferences.getInt("active",-1));
    }
    @Test public void rejectsTraversalExecutableFilesAndUnapprovedDirectories() throws Exception {
        Method valid=BraseiroLivePatch.class.getDeclaredMethod("validAsset",String.class);
        valid.setAccessible(true);
        assertEquals("dice/d6.png",valid.invoke(null,"dice/d6.png"));
        assertEquals("maps/sample.webp",valid.invoke(null,"maps/sample.webp"));
        for(String bad:new String[]{"../dice/d6.png","dice/../../file.png","app/classes.dex",
           "rules/script.js","data/ficha.json","/etc/passwd","dice/patch.so",
           "dice/a.zip","scripts/logic.py","dice/d 6.png"}){
            try{
                valid.invoke(null,bad);
                fail("Allowed invalid patch path "+bad);
            }catch(java.lang.reflect.InvocationTargetException expected){
                assertTrue(expected.getCause() instanceof IllegalArgumentException);
            }
        }
    }
    @Test public void assetWithoutPatchComesFromSignedApk() throws Exception {
        try(InputStream in=BraseiroLivePatch.openAsset(app,"dice/d6.png")){
            assertEquals(0x89,in.read()); // PNG signature
            assertEquals(0x50,in.read());
            assertEquals(0x4e,in.read());
            assertEquals(0x47,in.read());
        }
    }
}
