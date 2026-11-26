package com.braseiro.dnd5e;
import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class AtlasBundledImportRobolectricTest {
    @Test public void indexIdentitiesRespectValidatedContract(){
        assertTrue(MapPackageManager.indexIdentityMatches("faerun-v2","forgotten_realms:faerun","faerun-v2","legacy-index"));
        assertTrue(MapPackageManager.indexIdentityMatches("legacy-index","forgotten_realms:faerun","faerun-v2","legacy-index"));
        assertFalse(MapPackageManager.indexIdentityMatches("mystara","forgotten_realms:faerun","faerun-v2","legacy-index"));
    }
    @Test public void profileReferenceMatchesFlattenedZipExtraction() throws Exception {
        File dir=java.nio.file.Files.createTempDirectory("atlas-profile").toFile();
        File profile=new File(dir,"atlas_map_profile_v1.json");
        try {
            assertTrue(profile.createNewFile());
            org.json.JSONObject root=new org.json.JSONObject().put("dataFiles",
                new org.json.JSONObject().put("atlasMapProfile","metadata/atlas_map_profile_v1.json"));
            assertEquals(profile,MapPackageManager.resolveAtlasProfileFile(dir,root));
        } finally { profile.delete();dir.delete(); }
    }
    @Test public void realBundledAtlasImportsAndReopens() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String dbName="atlas-real-"+System.nanoTime()+".db";
        AppDb db=new AppDb(ctx,dbName);
        try {
            db.setActiveMapProfile("dnd");
            File dir=new File("src/main/assets/maps");
            if(!dir.isDirectory())dir=new File("app/src/main/assets/maps");
            File[] files=dir.listFiles((d,n)->n.startsWith("PACOTE_MAPA_FAERUN_2025_PTBR_MAX_V")&&n.endsWith(".zip"));
            assertNotNull("Bundled Atlas directory "+dir.getAbsolutePath(),files);
            assertTrue("Bundled Atlas missing",files.length>0);
            java.util.Arrays.sort(files,java.util.Comparator.comparing(File::getName));
            File zip=files[files.length-1];
            CountDownLatch done=new CountDownLatch(1);
            AtomicReference<String> failure=new AtomicReference<>();
            AtomicReference<MapPackageManager.ImportResult> result=new AtomicReference<>();
            new MapPackageManager(ctx,db).importZip(Uri.fromFile(zip),new MapPackageManager.Callback(){
                public void onProgress(String stage,int current,int total){if(current==total)System.out.println("ATLAS_REAL "+stage);}
                public void onSuccess(MapPackageManager.ImportResult r){result.set(r);done.countDown();}
                public void onError(String e){failure.set(e);done.countDown();}
            });
            assertTrue("Bundled Atlas timed out",done.await(8,TimeUnit.MINUTES));
            assertNull(failure.get(),failure.get());
            assertNotNull(result.get());
            String id=result.get().packageId;
            assertTrue("Atlas ausente ou não reconhecido: "+id+" / "+db.getMapPackageAny(id).optInt("atlas_ready",-1),db.isAtlasContractPackage(id));
            assertEquals(id,db.getMapState().optString("map_package_id"));
            assertTrue(db.isValidMapHex(id,"DO40"));
            db.close();
            db=new AppDb(ctx,dbName);db.setActiveMapProfile("dnd");
            assertTrue(db.isAtlasContractPackage(id));
            assertTrue(db.isValidMapHex(id,"DO40"));
        } finally {db.close();ctx.deleteDatabase(dbName);}
    }
}
