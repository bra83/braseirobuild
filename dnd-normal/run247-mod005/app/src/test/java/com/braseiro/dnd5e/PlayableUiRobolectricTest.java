package com.braseiro.dnd5e;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** Renders the production Heroes screen; only startup services are omitted. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class PlayableUiRobolectricTest {
    MainActivity activity;
    AppDb db;
    FrameLayout content;
    long hero;
    String database;
    @Before public void setup() throws Exception {
        activity=Robolectric.buildActivity(MainActivity.class).get();
        database="playable-ui-"+System.nanoTime()+".db";
        db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        db.saveCampaign("Mesa UI",DndRulesEngine.PROFILE_2024,"Fantasia heroica",true,true,false,"");
        hero=db.addCharacter("Herói UI","Humano","Guerreiro",1,4,12,16,15,13,14,8,12,10,true,"");
        db.updateCharacterRuleDetails(hero,"Soldado","","","","",DndRulesEngine.PROFILE_2024);
        db.ensureCharacterResourceState(hero);
        content=new FrameLayout(activity);activity.setContentView(content);
        set("content",content);set("db",db);
        set("prefs",activity.getSharedPreferences("ui-test",0));
        set("characterGenerator",new CharacterSheetGenerator(new DndRulesEngine()));
        set("worldStateEngine",new WorldStateEngine(db));
        render();
    }
    void set(String name,Object value)throws Exception {Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
    void render()throws Exception {java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showHeroes");m.setAccessible(true);m.invoke(activity);}
    <T extends View> T find(View root,Class<T> type,String label){
        if(type.isInstance(root)&&(!(root instanceof android.widget.TextView)||label==null||label.equals(((android.widget.TextView)root).getText().toString())))return type.cast(root);
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){T result=find(((ViewGroup)root).getChildAt(i),type,label);if(result!=null)return result;}
        return null;
    }
    void click(String label){Button button=find(content,Button.class,label);assertNotNull(label,button);button.performClick();}
    @After public void cleanup(){db.close();activity.deleteDatabase(database);}
    @Test public void shortRestRequiresConfirmationAndPersistsChoiceOnReopen()throws Exception {
        double before=db.getSceneState().optDouble("clock_hour",9);
        click("DESCANSO CURTO");AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        assertFalse(db.ensureCharacterResourceState(hero).optBoolean("short_rest_hit_dice_choice_pending"));
        click("DESCANSO CURTO");dialog=ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertEquals(before+1,db.getSceneState().optDouble("clock_hour"),.001);
        assertNotNull(find(content,Button.class,"GASTAR 1 DADO DE VIDA DO DESCANSO CURTO"));
        db.close();db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        assertTrue(db.ensureCharacterResourceState(hero).optBoolean("short_rest_hit_dice_choice_pending"));
        assertEquals(4,db.getCharacter(hero).optInt("hp"));
    }
    @Test public void longRestRestoresHpAndPersistsTime()throws Exception {
        click("DESCANSO LONGO");ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertEquals(12,db.getCharacter(hero).optInt("hp"));
        assertFalse(db.ensureCharacterResourceState(hero).optBoolean("short_rest_hit_dice_choice_pending"));
        String scene=db.getSceneState().toString();
        db.close();db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        assertEquals(12,db.getCharacter(hero).optInt("hp"));assertEquals(scene,db.getSceneState().toString());
    }
    @Test public void xpRejectsInvalidInputAndAwardsOnce()throws Exception {
        click("+ XP");AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
        EditText input=find(dialog.getWindow().getDecorView(),EditText.class,null);
        for(String invalid:new String[]{"","0","-1","999999999999"}){
            input.setText(invalid);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            assertTrue(dialog.isShowing());assertNotNull(input.getError());
            assertEquals(0,db.ensureCharacterResourceState(hero).optInt("xp"));
        }
        input.setText("300");Button apply=dialog.getButton(AlertDialog.BUTTON_POSITIVE);apply.performClick();apply.performClick();
        assertEquals(300,db.ensureCharacterResourceState(hero).optInt("xp"));
        click("SUBIR NÍVEL");assertEquals(2,db.getCharacter(hero).optInt("level"));
        db.close();db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        assertEquals(2,db.getCharacter(hero).optInt("level"));assertEquals(300,db.ensureCharacterResourceState(hero).optInt("xp"));
    }
    @Test public void damageControlUpdatesPersistedSheet()throws Exception {
        click("− PV");AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
        EditText input=find(dialog.getWindow().getDecorView(),EditText.class,null);input.setText("2");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertEquals(2,db.getCharacter(hero).optInt("hp"));
        db.close();db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        assertEquals(2,db.getCharacter(hero).optInt("hp"));
        assertEquals(2,db.ensureCharacterResourceState(hero).optInt("hp"));
    }
    @Test public void runningEncounterBlocksRestWithoutMutation()throws Exception {
        db.setCombatRunning(true);String before=db.getSceneState().toString();
        click("DESCANSO LONGO");assertEquals(4,db.getCharacter(hero).optInt("hp"));
        assertEquals(before,db.getSceneState().toString());
    }
}
