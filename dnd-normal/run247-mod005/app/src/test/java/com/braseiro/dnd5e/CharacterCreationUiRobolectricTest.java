package com.braseiro.dnd5e;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Production controls and SQLite, without network/startup services or mocked rule engines. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class CharacterCreationUiRobolectricTest {
    MainActivity activity;
    AppDb db;
    FrameLayout content;
    String database;
    AlertDialog form;
    @Before public void setup() throws Exception {
        activity=Robolectric.buildActivity(MainActivity.class).get();
        database="creation-ui-"+System.nanoTime()+".db";
        db=new AppDb(activity,database);db.setActiveMapProfile("dnd");
        assertEquals(DndRulesEngine.PROFILE_2024,db.getCampaign().optString("rules_profile"));
        content=new FrameLayout(activity);activity.setContentView(content);
        set("content",content);set("db",db);set("prefs",activity.getSharedPreferences(database,0));
        set("rulesEngine",new DndRulesEngine());
        set("characterGenerator",new CharacterSheetGenerator(new DndRulesEngine()));
        set("characterRulesValidator",new CharacterRulesValidator());
        set("safety",new CampaignSafetyManager(activity,db));
        set("worldStateEngine",new WorldStateEngine(db));
        render();
    }
    void set(String name,Object value)throws Exception {Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
    void render()throws Exception {java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showHeroes");m.setAccessible(true);m.invoke(activity);}
    @After public void cleanup(){if(form!=null)form.dismiss();db.close();activity.deleteDatabase(database);}
    View find(View root,String label,boolean description){
        CharSequence value=description?root.getContentDescription():root instanceof TextView?((TextView)root).getText():null;
        if(value!=null&&label.contentEquals(value))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View result=find(((ViewGroup)root).getChildAt(i),label,description);if(result!=null)return result;}
        return null;
    }
    View control(String label){View v=find(form.getWindow().getDecorView(),label,true);assertNotNull(label,v);return v;}
    void click(String label){View v=find(content,label,false);assertNotNull(label,v);assertTrue(v.performClick());shadowOf(Looper.getMainLooper()).idle();}
    void open(){click("+ CRIAR PERSONAGEM");form=ShadowAlertDialog.getLatestAlertDialog();assertTrue(form.isShowing());}
    void named(String name){
        control("Editar Nome").performClick();AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();
        menu.getListView().performItemClick(menu.getListView().getChildAt(2),2,2);
        AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();
        EditText input=firstEdit(editor.getWindow().getDecorView());assertNotNull(input);input.setText(name);
        editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }
    EditText firstEdit(View root){if(root instanceof EditText&&root.getVisibility()==View.VISIBLE)return (EditText)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){EditText e=firstEdit(((ViewGroup)root).getChildAt(i));if(e!=null)return e;}return null;}
    void select(String label,String choice){Spinner spinner=(Spinner)control(label);for(int i=0;i<spinner.getCount();i++)if(choice.equals(spinner.getItemAtPosition(i).toString())){spinner.setSelection(i);shadowOf(Looper.getMainLooper()).idle();return;}fail(choice);}
    void level(int level){control("Selecionar Nível").performClick();AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();int index=level+2; // current value + random + oracle + levels 1..20; level 1 is deduplicated
        for(int i=0;i<picker.getListView().getCount();i++)if(String.valueOf(level).equals(picker.getListView().getItemAtPosition(i).toString())){index=i;break;}
        picker.getListView().performItemClick(picker.getListView().getChildAt(index),index,index);shadowOf(Looper.getMainLooper()).idle();}
    void save(){form.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();}
    JSONObject only(){assertEquals(1,db.listCharacters().size());return db.listCharacters().get(0);}
    JSONObject sheet()throws Exception{return new JSONObject(only().optString("sheet_json"));}
    void reopenDb()throws Exception {db.close();db=new AppDb(activity,database);db.setActiveMapProfile("dnd");set("db",db);set("safety",new CampaignSafetyManager(activity,db));set("worldStateEngine",new WorldStateEngine(db));}
    AlertDialog picker(String label){control("Selecionar "+label).performClick();AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(form,picker);return picker;}
    void toggle(AlertDialog picker,int index){ListView list=picker.getListView();list.performItemClick(list.getChildAt(index),index,index);}

    @Test public void create2024SaveReopenAndEditName()throws Exception {
        open();named("Brasa");select("CLASSE","Mago");select("ESPÉCIE","Humano");
        save();assertFalse(form.isShowing());assertEquals("Brasa",only().optString("name"));
        assertEquals(DndRulesEngine.PROFILE_2024,only().optString("rules_source"));
        long id=only().optLong("id");reopenDb();render();click("EDITAR");form=ShadowAlertDialog.getLatestAlertDialog();named("Brasa revisada");save();
        assertEquals("Edit must preserve character identity",id,only().optLong("id"));
        assertEquals("Editing name after DB reopen must persist; saved="+only().optString("name"),"Brasa revisada",only().optString("name"));
    }
    @Test public void cancelCreationDoesNotPersist(){open();named("Descartada");form.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertTrue(db.listCharacters().isEmpty());}
    @Test public void blankNameBlocksSave(){open();named("");save();assertTrue(form.isShowing());assertTrue(db.listCharacters().isEmpty());}
    @Test public void zeroMaximumHpBlocksSave(){open();named("Inválida");View minus=control("Diminuir PV máximo");for(int i=0;i<100;i++)minus.performClick();save();assertTrue(form.isShowing());assertTrue(db.listCharacters().isEmpty());}
    @Test public void doubleSaveDoesNotCreateDuplicate(){open();named("Única");Button save=form.getButton(AlertDialog.BUTTON_POSITIVE);save.performClick();save.performClick();only();}
    @Test public void zeroHpIsNotHealedBySave()throws Exception {open();named("Caída");View minus=control("Diminuir PV atual");for(int i=0;i<100;i++)minus.performClick();save();assertEquals(0,only().optInt("hp"));reopenDb();assertEquals(0,only().optInt("hp"));}
    @Test public void manualAbilitySurvivesIdentityChanges()throws Exception {
        open();named("Atributos");control("Aumentar FOR").performClick();select("ESPÉCIE","Humano");select("ANTECEDENTE","Sábio");save();
        assertEquals(16,only().optInt("str"));
    }
    @Test public void skillDiamondSurvivesLevelChangeAndPersists()throws Exception {
        open();named("Perita");control("Especialidade em Percepção").performClick();level(5);save();
        assertEquals(5,only().optInt("level"));assertTrue(sheet().optString("skills").matches("(?s).*Percepção[^•]*◈.*"));
        reopenDb();assertTrue(sheet().optString("skills").contains("◈"));
    }
    @Test public void inventoryRequiresApplyAndSurvivesLevelChange()throws Exception {
        open();named("Mochileira");AlertDialog picker=picker("INVENTÁRIO E EQUIPAMENTO");toggle(picker,0);picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();level(2);save();
        assertTrue(sheet().optString("inventory").contains("Mochila"));reopenDb();assertTrue(sheet().optString("inventory").contains("Mochila"));
    }
    @Test public void cancellingInventoryDoesNotReplaceStartingEquipment()throws Exception {
        open();named("Equipamento");AlertDialog picker=picker("INVENTÁRIO E EQUIPAMENTO");toggle(picker,18);picker.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();save();
        assertFalse(sheet().optString("inventory").contains("Poção de cura"));
    }
    @Test public void spellsRequireApplyAndSurviveSpeciesChange()throws Exception {
        open();named("Arcana");select("CLASSE","Mago");AlertDialog picker=picker("TRUQUES E MAGIAS");
        for(int i=0;i<picker.getListView().getCount();i++)if(picker.getListView().isItemChecked(i))toggle(picker,i);
        toggle(picker,0);picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();select("ESPÉCIE","Humano");level(2);save();
        assertTrue(sheet().optString("spells").contains("Raio de Fogo"));assertFalse(sheet().optString("spells").contains("Teia"));
        reopenDb();assertTrue(sheet().optString("spells").contains("Raio de Fogo"));
    }
    @Test public void legacy2014EditIn2024CampaignKeepsEditionAndDamage()throws Exception {
        long id=db.addCharacter("Legado","Meio-elfo","Guerreiro",1,0,12,16,15,13,14,8,12,10,true,"");
        db.updateCharacterRuleDetails(id,"Soldado","","","","",DndRulesEngine.PROFILE_2014);
        render();click("EDITAR");form=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Meio-elfo",((Spinner)control("ESPÉCIE")).getSelectedItem());
        named("Legado editado");save();assertFalse(form.isShowing());reopenDb();
        assertEquals("2014 legacy edition must survive 2024 campaign edit",DndRulesEngine.PROFILE_2014,only().optString("rules_source"));
        assertEquals("Editing a fallen 2014 character must not silently heal it",0,only().optInt("hp"));
        assertEquals("2014-only legacy species must not be rewritten","Meio-elfo",only().optString("species"));
        assertEquals(DndRulesEngine.PROFILE_2024,db.getCampaign().optString("rules_profile"));
    }
    AlertDialog originPicker(){
        AlertDialog menu=picker("OPÇÕES 2024 / EBERRON");
        menu.getListView().performItemClick(menu.getListView().getChildAt(0),0,0);
        return ShadowAlertDialog.getLatestAlertDialog();
    }
    @Test public void duplicateOriginAbilitiesKeepDialogOpenWithoutApplying()throws Exception {
        open();named("Origem inválida");AlertDialog picker=originPicker();
        Spinner plus2=(Spinner)find(picker.getWindow().getDecorView(),"Atributo +2 do antecedente",true);
        Spinner plus1=(Spinner)find(picker.getWindow().getDecorView(),"Atributo +1 do antecedente",true);
        plus1.setSelection(plus2.getSelectedItemPosition());picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertTrue(picker.isShowing());assertTrue(db.listCharacters().isEmpty());
        picker.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();save();assertEquals(15,only().optInt("str"));
    }
    @Test public void originBonusIsExplicitAndDoesNotStackWhenReopened()throws Exception {
        open();named("Origem confirmada");AlertDialog picker=originPicker();
        Button applyOrigin=picker.getButton(AlertDialog.BUTTON_POSITIVE);
        applyOrigin.performClick();assertFalse(picker.isShowing());
        applyOrigin.performClick(); // A retained button must not stack the accepted bonus.
        picker=originPicker();picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();save();
        JSONObject choices=new JSONObject(sheet().optString("feat_selection_json")).getJSONObject("run136").getJSONObject("origin_ability");
        assertTrue(choices.optBoolean("applied_to_editor"));assertEquals(17,only().optInt("str"));
        reopenDb();render();click("EDITAR");form=ShadowAlertDialog.getLatestAlertDialog();
        picker=originPicker();picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();save();assertEquals(17,only().optInt("str"));
    }
    @Test public void changingBackgroundRemovesOldBonusAndChoices()throws Exception {
        open();named("Outra origem");AlertDialog picker=originPicker();picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        select("ANTECEDENTE","Sábio");save();assertEquals(15,only().optInt("str"));
        JSONObject choices=new JSONObject(sheet().optString("feat_selection_json")).getJSONObject("run136");
        assertFalse(choices.has("origin_ability"));reopenDb();assertEquals(15,only().optInt("str"));
    }
    @Test public void inventoryPickerPreservesExistingQuantitiesAndUnlistedItems()throws Exception {
        open();named("Inventário durável");save();JSONObject row=only();JSONObject stored=sheet();
        String inventory="Mochila | QTD 3 | PESO 2,5 kg | Geral\nLivro pessoal | QTD 1 | PESO 1 kg | Geral";
        stored.put("inventory",inventory).put("equipment",inventory);db.updateCharacterSheet(row.optLong("id"),stored.toString());
        render();click("EDITAR");form=ShadowAlertDialog.getLatestAlertDialog();
        AlertDialog picker=picker("INVENTÁRIO E EQUIPAMENTO");picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();save();
        assertTrue(sheet().optString("inventory").contains("Mochila | QTD 3"));
        assertTrue(sheet().optString("inventory").contains("Livro pessoal"));
        reopenDb();assertTrue(sheet().optString("inventory").contains("Livro pessoal"));
    }

    @Test public void newCampaignScreenSaves2024AndCharacterUsesThatProfile()throws Exception {
        set("drive",new DriveService(activity));
        java.lang.reflect.Method screen=MainActivity.class.getDeclaredMethod("showCampaign");screen.setAccessible(true);screen.invoke(activity);
        Spinner profile=(Spinner)find(content,"Perfil de regras da campanha",true);assertNotNull(profile);
        assertEquals(DndRulesEngine.PROFILE_2024,profile.getSelectedItem());
        firstEdit(content).setText("Campanha pela interface");click("SALVAR CONFIGURAÇÃO");
        reopenDb();assertEquals("Campanha pela interface",db.getCampaign().optString("name"));
        assertEquals(DndRulesEngine.PROFILE_2024,db.getCampaign().optString("rules_profile"));
        render();open();named("Primeiro herói");save();assertEquals(DndRulesEngine.PROFILE_2024,only().optString("rules_source"));
    }

    @Test public void unavailableSubclassLabelDoesNotBecomeASubclass()throws Exception {
        open();named("Sem subclasse");control("Selecionar Subclasse (somente no nível permitido)").performClick();
        AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();ListView list=picker.getListView();int index=-1;
        for(int i=0;i<list.getCount();i++)if(list.getItemAtPosition(i).toString().startsWith("Disponível a partir")){index=i;break;}
        assertTrue(index>=0);list.performItemClick(list.getChildAt(index),index,index);save();
        assertFalse(form.isShowing());assertEquals("Guerreiro",only().optString("class_name"));
    }

}
