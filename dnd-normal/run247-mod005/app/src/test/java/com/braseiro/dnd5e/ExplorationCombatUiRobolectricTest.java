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
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Real production controls, real core registry and SQLite. Startup/network services omitted. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class ExplorationCombatUiRobolectricTest {
    MainActivity activity; AppDb db; FrameLayout content; String database; AlertDialog dialog;
    JSONObject registry;
    @Before public void setup()throws Exception {
        activity=Robolectric.buildActivity(MainActivity.class).get();
        database="combat-ui-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        content=new FrameLayout(activity);activity.setContentView(content);
        set("content",content);set("db",db);set("prefs",activity.getSharedPreferences(database,0));
        set("rulesEngine",new DndRulesEngine());set("partyDirector",new PartyDirector(db));
        set("worldStateEngine",new WorldStateEngine(db));set("safety",new CampaignSafetyManager(activity,db));
        registry=CompiledStatblockEngine.snapshot();Xmm2025CompiledBestiaryEngine.install();render();
    }
    @After public void cleanup(){if(dialog!=null)dialog.dismiss();db.close();activity.deleteDatabase(database);CompiledStatblockEngine.restoreReplacing(registry);}
    void set(String field,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(field);f.setAccessible(true);f.set(activity,value);}
    void render()throws Exception{Method m=MainActivity.class.getDeclaredMethod("showCombat");m.setAccessible(true);m.invoke(activity);shadowOf(Looper.getMainLooper()).idle();}
    void reopen()throws Exception{db.close();db=new AppDb(activity,database);db.setActiveMapProfile("dnd");set("db",db);set("partyDirector",new PartyDirector(db));set("safety",new CampaignSafetyManager(activity,db));}
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
    long spawn()throws Exception{
        open("+ MONSTRO DE BLOCO COMPILADO");select(find(dialog.getWindow().getDecorView(),Spinner.class,null),"Aarakocra Skirmisher");
        write("Iniciativa final (d20 + bônus)","16");apply().performClick();assertFalse(dialog.isShowing());
        assertEquals(1,db.listCombatants().size());return db.listCombatants().get(0).optLong("id");
    }
    @Test public void xmmPickerExcludesEveryNonCoreSourceAndCancelDoesNotStart(){
        open("+ MONSTRO DE BLOCO COMPILADO");Spinner blocks=find(dialog.getWindow().getDecorView(),Spinner.class,null);
        assertEquals(503,blocks.getCount());
        for(int i=0;i<blocks.getCount();i++)assertEquals("Monster Manual (2025)",CompiledStatblockEngine.get(blocks.getItemAtPosition(i).toString()).optString("source_book"));
        cancel();assertTrue(db.listCombatants().isEmpty());assertEquals(0,db.getCombatState().optInt("running"));
    }
    @Test public void xmmSpawnStartsRealTurnAndCannotRepeatAfterConfirmation()throws Exception{
        open("+ MONSTRO DE BLOCO COMPILADO");select(find(dialog.getWindow().getDecorView(),Spinner.class,null),"Aarakocra Skirmisher");
        for(String invalid:new String[]{"","oops","101","-21","999999999999"}){write("Iniciativa final (d20 + bônus)",invalid);apply().performClick();assertTrue(dialog.isShowing());assertTrue(db.listCombatants().isEmpty());}
        write("Iniciativa final (d20 + bônus)","16");Button add=apply();add.performClick();add.performClick();
        assertEquals(1,db.listCombatants().size());long id=db.listCombatants().get(0).optLong("id");
        assertEquals(1,db.getCombatState().optInt("running"));assertEquals(1,db.getCombatant(id).optInt("active_turn"));
        JSONObject block=CompiledStatblockEngine.get("Aarakocra Skirmisher");assertEquals(block.optInt("hit_points"),db.getCombatant(id).optInt("hp"));
        reopen();assertEquals(16,db.getCombatant(id).optInt("initiative"));assertEquals("Aarakocra Skirmisher",new JSONObject(db.getCombatant(id).optString("resources_json")).optString("block_id"));
    }
    @Test public void manualCreatureRejectsInvalidVitalsThenStartsOnce()throws Exception{
        open("+ CRIATURA MANUAL");write("Iniciativa final (d20 + bônus)","9");
        for(String invalid:new String[]{"","0","-1","x","999999999999"}){write("PV",invalid);apply().performClick();assertTrue(dialog.isShowing());assertTrue(db.listCombatants().isEmpty());assertNotNull(field("PV").getError());}
        write("PV","20");write("CA","x");apply().performClick();assertTrue(db.listCombatants().isEmpty());
        write("CA","14");Button add=apply();add.performClick();add.performClick();assertEquals(1,db.listCombatants().size());
        reopen();assertEquals(20,db.listCombatants().get(0).optInt("hp"));assertEquals(14,db.listCombatants().get(0).optInt("ac"));
    }
    @Test public void damageRejectsInvalidAndConsumesTemporaryHpOnlyOnce()throws Exception{
        long id=spawn();JSONObject c=db.getCombatant(id);
        db.updateCombatantAdvanced(id,3,"",false,0,0,"","[]","","","",c.optString("resources_json"));render();
        int hp=c.optInt("hp");open("DANO");
        for(String invalid:new String[]{"","0","-1","x","999999999999"}){write("Dano",invalid);apply().performClick();assertTrue(dialog.isShowing());assertEquals(hp,db.getCombatant(id).optInt("hp"));assertEquals(3,db.getCombatant(id).optInt("temp_hp"));}
        write("Dano","5");Button damage=apply();damage.performClick();damage.performClick();
        assertEquals(hp-2,db.getCombatant(id).optInt("hp"));assertEquals(0,db.getCombatant(id).optInt("temp_hp"));
        reopen();assertEquals(hp-2,db.getCombatant(id).optInt("hp"));
    }
    @Test public void cancellingDamagePreservesAllVitals()throws Exception{long id=spawn();String before=db.getCombatant(id).toString();open("DANO");write("Dano","7");cancel();assertEquals(before,db.getCombatant(id).toString());}
    @Test public void timedConditionIsActiveAndPersistsUnknownResources()throws Exception{
        long id=spawn();open("AVANÇADO");write("Nova condição com duração (opcional)","Envenenado");write("Duração em rodadas","2");apply().performClick();assertFalse(dialog.isShowing());
        reopen();JSONObject c=db.getCombatant(id);assertTrue(ConditionRulesEngine.has(c.optString("conditions"),"Envenenado"));assertEquals(2,new JSONArray(c.optString("condition_timers_json")).getJSONObject(0).optInt("rounds"));assertEquals("Aarakocra Skirmisher",new JSONObject(c.optString("resources_json")).optString("block_id"));
    }
    @Test public void invalidDurationAndCancelLeaveStateUntouched()throws Exception{
        long id=spawn();String before=db.getCombatant(id).toString();open("AVANÇADO");write("Nova condição com duração (opcional)","Envenenado");write("Duração em rodadas","0");apply().performClick();assertTrue(dialog.isShowing());assertEquals(before,db.getCombatant(id).toString());cancel();assertEquals(before,db.getCombatant(id).toString());
    }
    @Test public void staleAdvancedEditorCannotEraseDamageOrTurnResources()throws Exception{
        long id=spawn();open("AVANÇADO");write("PV temporários","10");db.applyCombatantDamage(id,1,"",false,false,"ui-test-stale-damage");String live=db.getCombatant(id).toString();apply().performClick();assertTrue(dialog.isShowing());assertEquals(live,db.getCombatant(id).toString());
    }
    @Test public void manualDiceRejectWrongFacesAndPersistOneAuditableRoll()throws Exception{
        spawn();open("INFORMAR DADOS MANUAIS");
        for(String invalid:new String[]{"","0","21","1,2","x"}){write("Faces naturais",invalid);apply().performClick();assertTrue(dialog.isShowing());assertTrue(db.listDiceHistory().isEmpty());}
        write("Faces naturais","17");Button register=apply();register.performClick();register.performClick();assertEquals(1,db.listDiceHistory().size());
        reopen();JSONObject roll=db.listDiceHistory().get(0);assertEquals(17,roll.optInt("total"));assertEquals("[17]",roll.optString("rolls_json"));assertEquals("manual_user",roll.optString("render_mode"));
    }
    @Test public void cancelledManualDiceDoesNotChangeCombatOrHistory()throws Exception{long id=spawn();String before=db.getCombatant(id).toString();open("INFORMAR DADOS MANUAIS");write("Faces naturais","20");cancel();assertTrue(db.listDiceHistory().isEmpty());assertEquals(before,db.getCombatant(id).toString());}
    @Test public void nextTurnAdvancesRoundAndResetsSpentAction()throws Exception{long id=spawn();click("AÇÃO");assertFalse(new JSONObject(db.getCombatant(id).optString("resources_json")).optBoolean("action_available"));int round=db.getCombatState().optInt("round_no");click("PRÓXIMO TURNO");assertEquals(round+1,db.getCombatState().optInt("round_no"));assertTrue(new JSONObject(db.getCombatant(id).optString("resources_json")).optBoolean("action_available"));reopen();assertEquals(round+1,db.getCombatState().optInt("round_no"));}
    @Test public void encounterEndRequiresConfirmationAndPersistsClearState()throws Exception{long id=spawn();open("ENCERRAR");cancel();assertEquals(1,db.getCombatState().optInt("running"));assertTrue(db.getCombatant(id).length()>0);open("ENCERRAR");apply().performClick();assertTrue(db.listCombatants().isEmpty());reopen();assertEquals(0,db.getCombatState().optInt("running"));assertTrue(db.listCombatants().isEmpty());}
    @Test public void emptyEncounterCannotAdvanceTurn()throws Exception{int round=db.getCombatState().optInt("round_no");click("PRÓXIMO TURNO");assertEquals(round,db.getCombatState().optInt("round_no"));assertEquals(0,db.getCombatState().optInt("running"));}
    void clearBoard()throws Exception{
        JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
        android.content.ContentValues clear=new android.content.ContentValues();clear.put("obstacles_json","[]");
        assertEquals(1,db.getWritableDatabase().update("tactical_scenes",clear,"world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
        db.ensureTacticalTokensForCombat();
    }
    void position(long id,int x,int y){
        android.content.ContentValues values=new android.content.ContentValues();values.put("x",(x+.5)/12d);values.put("y",(y+.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,"world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void movementCancelInvalidAndCommitUseRealGridBudget()throws Exception{
        long id=spawn();clearBoard();position(id,2,2);render();
        int budget=new JSONObject(db.getCombatant(id).optString("resources_json")).optInt("movement_remaining_ft");
        open("MOVER");write("Destino: coluna (começando em 0)","3");write("Destino: linha (começando em 0)","2");cancel();
        assertEquals(2,db.tacticalGridPosition(id).optInt("cell_x"));
        open("MOVER");write("Destino: coluna (começando em 0)","999");apply().performClick();assertTrue(dialog.isShowing());assertEquals(2,db.tacticalGridPosition(id).optInt("cell_x"));
        write("Destino: coluna (começando em 0)","3");write("Destino: linha (começando em 0)","2");apply().performClick();assertFalse(dialog.isShowing());
        assertEquals(3,db.tacticalGridPosition(id).optInt("cell_x"));assertEquals(budget-5,new JSONObject(db.getCombatant(id).optString("resources_json")).optInt("movement_remaining_ft"));
        reopen();assertEquals(3,db.tacticalGridPosition(id).optInt("cell_x"));assertEquals(2,db.tacticalGridPosition(id).optInt("cell_y"));
    }
    @Test public void compiledAttackUsesSourceActionManualDiceAndPersistsDamageOnce()throws Exception{
        long actor=spawn();long target=db.addCombatant("Alvo de teste",5,30,30,12,"",true);clearBoard();position(actor,2,2);position(target,3,2);render();
        open("ATAQUE DE MONSTRO • BLOCO COMPILADO");
        write("Primeiro d20 natural (1–20)","0");apply().performClick();assertTrue(dialog.isShowing());assertEquals(30,db.getCombatant(target).optInt("hp"));
        write("Primeiro d20 natural (1–20)","15");write("Total de dano","3");
        Button attack=apply();attack.performClick();attack.performClick();assertFalse(dialog.isShowing());
        assertEquals(27,db.getCombatant(target).optInt("hp"));
        assertFalse(new JSONObject(db.getCombatant(actor).optString("resources_json")).optBoolean("action_available"));
        reopen();assertEquals(27,db.getCombatant(target).optInt("hp"));
    }
    @Test public void cancellingCompiledAttackDoesNotSpendActionOrDamage()throws Exception{
        long actor=spawn();long target=db.addCombatant("Alvo de teste",5,30,30,12,"",true);clearBoard();position(actor,2,2);position(target,3,2);render();
        String before=db.getCombatant(actor).toString();open("ATAQUE DE MONSTRO • BLOCO COMPILADO");write("Primeiro d20 natural (1–20)","15");write("Total de dano","3");cancel();
        assertEquals(30,db.getCombatant(target).optInt("hp"));assertEquals(before,db.getCombatant(actor).toString());
    }

    @Test public void returningFromHeroSheetsRebuildsCombatFromPersistedState()throws Exception{
        long id=spawn();
        set("characterGenerator",new CharacterSheetGenerator(new DndRulesEngine()));
        set("currentScreen","Combate");render();View oldCombat=content.getChildAt(0);
        click("FICHAS DOS HERÓIS");assertNotNull(find(content,Button.class,"+ CRIAR PERSONAGEM"));
        db.applyCombatantDamage(id,1,"",false,false,"ui-test-navigation-damage");
        Method nav=MainActivity.class.getDeclaredMethod("navigate",String.class);nav.setAccessible(true);nav.invoke(activity,"Combate");shadowOf(Looper.getMainLooper()).idle();
        assertNotSame(oldCombat,content.getChildAt(0));assertNotNull(find(content,Button.class,"DANO"));
        JSONObject c=db.getCombatant(id);assertNotNull(find(content,TextView.class,"PV "+c.optInt("hp")+"/"+c.optInt("max_hp")+" • CA "+c.optInt("effective_ac",c.optInt("ac"))));
    }

    @Test public void manualMixedDiceUsesFormulaOrderAndAddsModifierOnce()throws Exception{
        spawn();EditText formula=find(content,EditText.class,"d20+5");assertNotNull(formula);formula.setText("2d6+1d8+3");
        open("INFORMAR DADOS MANUAIS");write("Faces naturais","2,5,8");apply().performClick();
        assertEquals(18,db.listDiceHistory().get(0).optInt("total"));assertEquals("[2,5,8]",db.listDiceHistory().get(0).optString("rolls_json"));
    }
    @Test public void manualAdvantageRequiresTwoFacesAndUsesHigherNatural()throws Exception{
        spawn();EditText formula=find(content,EditText.class,"d20+5");assertNotNull(formula);formula.setText("d20+4");
        Spinner mode=find((View)formula.getParent(),Spinner.class,null);assertNotNull(mode);select(mode,"Vantagem");
        open("INFORMAR DADOS MANUAIS");write("Faces naturais","7");apply().performClick();assertTrue(dialog.isShowing());assertTrue(db.listDiceHistory().isEmpty());
        write("Faces naturais","7,16");apply().performClick();JSONObject roll=db.listDiceHistory().get(0);
        assertEquals(20,roll.optInt("total"));assertEquals(16,roll.optInt("natural_roll"));assertEquals("Vantagem",roll.optString("roll_mode"));
    }

}
