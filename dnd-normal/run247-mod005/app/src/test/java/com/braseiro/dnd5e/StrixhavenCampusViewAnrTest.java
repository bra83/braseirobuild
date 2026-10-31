package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Bitmap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.*;

/** A map refresh must never decode an image or recycle a shared bitmap on the UI thread. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class StrixhavenCampusViewAnrTest {
    @Test public void repeatedMapRefreshesReturnImmediately() {
        Context context=RuntimeEnvironment.getApplication();
        StrixhavenCampusView view=new StrixhavenCampusView(context);
        long start=System.nanoTime();
        for(int i=0;i<30;i++){
            view.setMapAsset("strixhaven/Strixhaven_map.webp",
                    Collections.<StrixhavenLocationCatalog.Hotspot>emptyList(),0.5f,0.6f);
        }
        long elapsed=(System.nanoTime()-start)/1000000L;
        assertTrue("Image decode on the main thread regressed: "+elapsed+"ms",elapsed<1500L);
    }

    @Test public void staleMapCallbacksCannotOverwriteNewMap() throws Exception {
        StrixhavenCampusView view=new StrixhavenCampusView(RuntimeEnvironment.getApplication());
        view.setMapAsset("strixhaven/Map_A_not_used.webp",
                Collections.<StrixhavenLocationCatalog.Hotspot>emptyList(),0.5f,0.6f);
        Bitmap image=Bitmap.createBitmap(12,12,Bitmap.Config.ARGB_8888);
        try {
            view.acceptDecodedMap("strixhaven/Map_B_not_used.webp",image);
            Field field=StrixhavenCampusView.class.getDeclaredField("bitmap");
            field.setAccessible(true);
            assertNull("Wrong-map decode must never replace active map",field.get(view));
            view.acceptDecodedMap("strixhaven/Map_A_not_used.webp",image);
            assertSame(image,field.get(view));
            view.setMapAsset("strixhaven/Map_C_not_used.webp",
                    Collections.<StrixhavenLocationCatalog.Hotspot>emptyList(),0.5f,0.6f);
            assertNull(field.get(view));
            assertFalse("The shared old map cannot be recycled during view recreation",image.isRecycled());
        } finally {
            image.recycle();
        }
    }
}
