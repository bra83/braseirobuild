package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Testes puros das regras opcionais de refeicao sem Android runtime. */
public final class WilemCookbookEffectRulesTest {
    private static JSONObject hero(String key,int value,double expiration)throws Exception{
        JSONObject effect=new JSONObject().put("effect_key",key).put("effect_value",value)
            .put("ends_world_hour",expiration);
        JSONObject sheet=new JSONObject().put("meal_effects",new JSONArray().put(effect));
        return new JSONObject().put("name","Auri").put("sheet_json",sheet.toString());
    }
    private static JSONObject scene(int day,int hour)throws Exception{
        return new JSONObject().put("day_number",day).put("clock_hour",hour);
    }
    @Test public void bonusExistsOnlyForRelevantSkillBeforeExpiration()throws Exception{
        JSONObject h=hero("sobrevivencia",3,36d);
        assertEquals(3,WilemCookbookEffectRules.skillBonus(h,"Sabedoria","Sobrevivência",scene(2,9)));
        assertEquals(3,WilemCookbookEffectRules.skillBonus(h,"Wisdom","Survival",scene(2,9)));
        assertEquals(0,WilemCookbookEffectRules.skillBonus(h,"Sabedoria","Percepção",scene(2,9)));
        assertEquals(0,WilemCookbookEffectRules.skillBonus(h,"Sabedoria","Sobrevivência",scene(2,12)));
    }
    @Test public void onlyDeclaredColdAndConcentrationFlags()throws Exception{
        JSONObject cold=hero("resistencia_frio",2,42d);
        assertTrue(WilemCookbookEffectRules.coldResistance(cold,scene(2,12)));
        assertFalse(WilemCookbookEffectRules.coldResistance(cold,scene(3,12)));
        JSONObject concentration=hero("concentracao",4,50d);
        assertEquals(4,WilemCookbookEffectRules.concentrationSaveBonus(concentration,scene(2,9)));
        assertEquals(0,WilemCookbookEffectRules.concentrationSaveBonus(concentration,scene(4,9)));
    }
    @Test public void missingSheetCannotGrantBonus()throws Exception{
        assertEquals(0,WilemCookbookEffectRules.skillBonus(new JSONObject(),"wis","survival",scene(1,8)));
        assertFalse(WilemCookbookEffectRules.coldResistance(new JSONObject(),scene(1,8)));
    }
}
