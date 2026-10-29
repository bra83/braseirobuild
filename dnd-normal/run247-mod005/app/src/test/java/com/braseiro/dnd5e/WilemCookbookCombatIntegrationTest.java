package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import static org.junit.Assert.*;

/** Integração temporal de efeitos opcionais na regra efetiva de dano. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public final class WilemCookbookCombatIntegrationTest {
    private Context context;
    private AppDb db;
    private String database;
    private long hero,combatant;

    @Before public void init()throws Exception{
        context=RuntimeEnvironment.getApplication();
        database="w-integrate-"+System.nanoTime()+".db";
        db=new AppDb(context,database);db.setActiveMapProfile("dnd");
        hero=db.addCharacter("Heroi Teste","Humano","Guerreiro",1,15,15,12,12,14,12,10,10,10,true,"");
        combatant=db.addCombatantWithVisual("Heroi Teste",10,15,15,13,"",true,"character",hero,"");
        assertEquals("character",db.getCombatant(combatant).optString("source_kind"));
        assertEquals(hero,db.getCombatant(combatant).optLong("source_id"));
    }
    @After public void close(){if(db!=null)db.close();if(context!=null)context.deleteDatabase(database);}

    private void effect(String key,double end)throws Exception{
        JSONObject ch=db.getCharacter(hero);
        JSONObject sheet=new JSONObject(ch.optString("sheet_json","{}"));
        sheet.put("meal_effects",new JSONArray().put(new JSONObject()
            .put("receipt","wilem-test-proof").put("effect_key",key)
            .put("effect_value",1).put("ends_world_hour",end)));
        db.updateCharacterSheetChecked(hero,sheet.toString());
    }

    @Test public void coldResistanceReducesDamageOnlyWhileMealActive()throws Exception{
        double now=WilemCookbookEffectRules.worldHour(db.getSceneState());
        effect("resistencia_frio",now+2d);
        assertTrue(WilemCookbookEffectRules.coldResistance(db.getCharacter(hero),db.getSceneState()));
        JSONObject first=db.applyCombatantDamage(combatant,9,"frio",false);
        assertEquals(4,first.optInt("applied_damage",-1)); // Resistência reduz dano pela metade, arredondando para baixo.
        effect("resistencia_frio",now-1d);
        assertFalse(WilemCookbookEffectRules.coldResistance(db.getCharacter(hero),db.getSceneState()));
        JSONObject second=db.applyCombatantDamage(combatant,9,"cold",false);
        assertEquals(9,second.optInt("applied_damage",-1));
    }

    @Test public void concentrationBonusUsesWorldTimeAndDoesNotLeakIntoOtherEffect()throws Exception{
        double now=WilemCookbookEffectRules.worldHour(db.getSceneState());
        effect("concentracao",now+1d);
        assertEquals(1,WilemCookbookEffectRules.concentrationSaveBonus(db.getCharacter(hero),db.getSceneState()));
        assertFalse(WilemCookbookEffectRules.coldResistance(db.getCharacter(hero),db.getSceneState()));
        effect("concentracao",now-1d);
        assertEquals(0,WilemCookbookEffectRules.concentrationSaveBonus(db.getCharacter(hero),db.getSceneState()));
    }
}
