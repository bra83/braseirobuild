package com.braseiro.dnd5e;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class PortraitStorageDurabilityRobolectricTest {
 @Test public void newPortraitDoesNotEvictOlderNpcPortrait()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  Bitmap b=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);
  ByteArrayOutputStream png=new ByteArrayOutputStream();
  assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,png));b.recycle();
  byte[] bytes=png.toByteArray();
  String suffix=Long.toString(System.nanoTime());
  Uri bast=ImageStorageManager.importImage(context,new ByteArrayInputStream(bytes),"image/png","npc_avatars","bast_"+suffix,1);
  Uri kote=ImageStorageManager.importImage(context,new ByteArrayInputStream(bytes),"image/png","npc_avatars","kote_"+suffix,1);
  assertTrue(ImageStorageManager.isReadable(context,bast));
  assertTrue(ImageStorageManager.isReadable(context,kote));
  assertNotEquals(bast,kote);
 }
}