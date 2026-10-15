package com.braseiro.dnd5e;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Real production controls, real core registry and SQLite. Startup/network services omitted. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class SessionContinuityUiRobolectricTest {
    MainActivity activity; AppDb db; FrameLayout content; String database; AlertDialog dialog;
    JSONObject registry;
    @Before public void setup()throws Exception {
        activity=Robolectric.buildActivity(MainActivity.class).get();
        database="combat-ui-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        content=new FrameLayout(activity);activity.setContentView(content);
        set("content",content);set("db",db);set("prefs",activity.getSharedPreferences(database,0));
        set("ui",new android.os.Handler(Looper.getMainLooper()));
        set("characterGenerator",new CharacterSheetGenerator(new DndRulesEngine()));
        set("rulesEngine",new DndRulesEngine());set("partyDirector",new PartyDirector(db));
        set("worldStateEngine",new WorldStateEngine(db));set("safety",new CampaignSafetyManager(activity,db));
        registry=CompiledStatblockEngine.snapshot();Xmm2025CompiledBestiaryEngine.install();render();
    }
    @After public void cleanup(){if(dialog!=null)dialog.dismiss();db.close();activity.deleteDatabase(database);CompiledStatblockEngine.restoreReplacing(registry);}
    void set(String field,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(field);f.setAccessible(true);f.set(activity,value);}
    void render()throws Exception{Method m=MainActivity.class.getDeclaredMethod("showHeroes");m.setAccessible(true);m.invoke(activity);shadowOf(Looper.getMainLooper()).idle();}
    void reopen()throws Exception{db.close();db=new AppDb(activity,database);db.setActiveMapProfile("dnd");set("db",db);set("partyDirector",new PartyDirector(db));set("safety",new CampaignSafetyManager(activity,db));set("worldStateEngine",new WorldStateEngine(db));}
    <T extends View>T find(View root,Class<T> type,String label){
        if(type.isInstance(root)&&(label==null||(root instanceof EditText?label.contentEquals(((EditText)root).getHint()):root instanceof TextView&&label.contentEquals(((TextView)root).getText()))))return type.cast(root);
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){T v=find(((ViewGroup)root).getChildAt(i),type,label);if(v!=null)return v;}return null;
    }
    void click(String label){Button b=find(content,Button.class,label);assertNotNull(label,b);b.performClick();shadowOf(Looper.getMainLooper()).idle();}
    void open(String label){click(label);dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());}
    EditText field(String label){EditText f=find(dialog.getWindow().getDecorView(),EditText.class,label);assertNotNull(label,f);return f;}
    void write(String label,String value){field(label).setText(value);}
    Button apply(){return dialog.getButton(AlertDialog.BUTTON_POSITIVE);}
    void cancel(){dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();}
    void select(Spinner spinner,String value){for(int i=0;i<spinner.getCount();i++)if(value.equals(spinner.getItemAtPosition(i).toString())){spinner.setSelection(i);shadowOf(Looper.getMainLooper()).idle();return;}fail(value);}
    long hero()throws Exception{
        long id=db.addCharacter("Continuidade","Humano","Guerreiro",1,4,12,16,15,13,14,8,12,10,true,"");
        db.updateCharacterRuleDetails(id,"Soldado","","","","",DndRulesEngine.PROFILE_2024);
        db.ensureCharacterResourceState(id);render();return id;
    }
    @Test public void healingRejectsInvalidAndAppliesOnlyOnceAfterReopen()throws Exception{
        long id=hero();open("+ PV");
        for(String bad:new String[]{"","0","-2","x","999999999999"}){
            find(dialog.getWindow().getDecorView(),EditText.class,null).setText(bad);apply().performClick();
            assertTrue(dialog.isShowing());assertEquals(4,db.getCharacter(id).optInt("hp"));
        }
        find(dialog.getWindow().getDecorView(),EditText.class,null).setText("2");Button save=apply();save.performClick();save.performClick();
        reopen();assertEquals(6,db.getCharacter(id).optInt("hp"));
    }
    @Test public void cancelledDamagePreservesCharacter()throws Exception{
        long id=hero();String before=db.getCharacter(id).toString();open("− PV");cancel();
        reopen();assertEquals(before,db.getCharacter(id).toString());
    }
    @Test public void xpValidatesAndCannotRepeatConfirmation()throws Exception{
        long id=hero();open("+ XP");EditText amount=find(dialog.getWindow().getDecorView(),EditText.class,null);
        amount.setText("-300");apply().performClick();assertTrue(dialog.isShowing());
        amount.setText("300");Button save=apply();save.performClick();save.performClick();
        reopen();JSONObject sheet=new JSONObject(db.getCharacter(id).optString("sheet_json"));
        assertEquals(300,sheet.optInt("xp"));render();assertNotNull(find(content,Button.class,"SUBIR NÍVEL"));
    }
    @Test public void cancelledRestDoesNotChangeTimeOrResources()throws Exception{
        long id=hero();String before=db.getCharacter(id).toString(),time=db.getTravelRulesState().toString();
        open("DESCANSO CURTO");cancel();reopen();assertEquals(before,db.getCharacter(id).toString());assertEquals(time,db.getTravelRulesState().toString());
    }
    @Test public void restRejectsPartyChangedAfterPreview()throws Exception{
        long id=hero();open("DESCANSO LONGO");db.adjustCharacterHp(id,-1);String time=db.getTravelRulesState().toString();
        apply().performClick();reopen();assertEquals(3,db.getCharacter(id).optInt("hp"));assertEquals(time,db.getTravelRulesState().toString());
    }
    @Test public void shortRestPersistsChoiceAndCannotRepeat()throws Exception{
        long id=hero();open("DESCANSO CURTO");Button rest=apply();rest.performClick();String time=db.getTravelRulesState().toString();rest.performClick();
        reopen();assertEquals(time,db.getTravelRulesState().toString());
        assertTrue(new JSONObject(db.getCharacter(id).optString("sheet_json")).optBoolean("short_rest_hit_dice_choice_pending"));
    }
    @Test public void backup21RoundTripRestoresHeroAndResourceState()throws Exception{
        long id=hero();db.addCharacterXp(id,300);JSONObject backup=db.exportAll();BackupContractValidator.requireValid(backup);
        String saved=db.getCharacter(id).optString("sheet_json");db.adjustCharacterHp(id,-3);db.addCharacterXp(id,50);
        db.importAll(backup);reopen();assertEquals(4,db.getCharacter(id).optInt("hp"));assertEquals(saved,db.getCharacter(id).optString("sheet_json"));
    }
    @Test public void levelUpOldButtonCannotApplyTwoEarnedLevels()throws Exception{
        long id=hero();db.addCharacterXp(id,900);render();
        Button level=find(content,Button.class,"SUBIR NÍVEL");assertNotNull(level);
        level.performClick();level.performClick();reopen();assertEquals(2,db.getCharacter(id).optInt("level"));
        render();click("SUBIR NÍVEL");reopen();assertEquals(3,db.getCharacter(id).optInt("level"));
    }
    @Test public void longRestRecoversHpAndPersistsOnlyOneTimeAdvance()throws Exception{
        long id=hero();open("DESCANSO LONGO");Button rest=apply();rest.performClick();
        String time=db.getTravelRulesState().toString();rest.performClick();reopen();
        assertEquals(12,db.getCharacter(id).optInt("hp"));assertEquals(time,db.getTravelRulesState().toString());
        assertFalse(new JSONObject(db.getCharacter(id).optString("sheet_json")).optBoolean("short_rest_hit_dice_choice_pending"));
    }

}
