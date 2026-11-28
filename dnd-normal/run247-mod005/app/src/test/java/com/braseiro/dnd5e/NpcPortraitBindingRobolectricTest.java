package com.braseiro.dnd5e;
import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class NpcPortraitBindingRobolectricTest {
 @Test public void koteNewPortraitOverridesOldSceneTokenAndPreservesBast()throws Exception{
  Context context=RuntimeEnvironment.getApplication();
  String name="kote-portrait-"+System.nanoTime()+".db";
  AppDb db=new AppDb(context,name);
  try{
   db.setActiveMapProfile("dnd");
   db.upsertNpcMemory(new JSONObject().put("name","Kote").put("active",1));
   db.upsertNpcMemory(new JSONObject().put("name","Bast").put("active",1));
   JSONObject kote=db.findNpcMemoryByName("Kote"),bast=db.findNpcMemoryByName("Bast");
   db.setNpcToken(bast.optLong("id"),"file:///bast-approved.png");
   db.setNpcToken(kote.optLong("id"),"file:///kote-old.png");
   // Simulate an installed database predating stable NPC IDs.
   db.getWritableDatabase().execSQL("UPDATE npc_memory_profiles SET stable_id='' WHERE id=?",new Object[]{kote.optLong("id")});
   JSONObject target=NpcPortraitBinding.resolveTarget(db,kote.optLong("id"),"","Kote");
   assertEquals(kote.optLong("id"),target.optLong("id"));
   assertFalse(target.optString("stable_id").isEmpty());
   NpcPortraitBinding.bindVerified(db,target.optLong("id"),"file:///kote-new.png");
   JSONObject fresh=db.findNpcMemoryByName("Kote");
   JSONObject cached=new JSONObject().put("name","Kote").put("token_uri","file:///kote-old.png");
   assertEquals("file:///kote-new.png",NpcPortraitBinding.displayUri(cached,fresh));
   JSONArray cast=new JSONArray().put(cached).put(new JSONObject().put("name","Bast").put("token_uri","file:///bast-approved.png"));
   JSONArray updated=NpcPortraitBinding.refreshCast(cast,fresh,new JSONObject().put("location","Marco do Percurso"));
   assertEquals("file:///kote-new.png",updated.getJSONObject(0).optString("token_uri"));
   assertEquals("file:///bast-approved.png",updated.getJSONObject(1).optString("token_uri"));
   assertEquals("file:///bast-approved.png",db.findNpcMemoryByName("Bast").optString("token_uri"));
   try{NpcPortraitBinding.resolveTarget(db,kote.optLong("id"),fresh.optString("stable_id"),"Bast");fail("Wrong NPC accepted");}catch(Exception expected){}
  }finally{db.close();context.deleteDatabase(name);}
 }
}