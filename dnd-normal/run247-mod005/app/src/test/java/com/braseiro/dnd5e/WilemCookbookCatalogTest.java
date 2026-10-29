package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Contratos de dados; nenhum teste presume concessao automatica de poderes RAW. */
public final class WilemCookbookCatalogTest {
    @Test public void totalAndUniqueness() throws Exception {
        JSONArray all=WilemCookbookCatalog.recipes();
        assertEquals(300,all.length());
        assertEquals(300,WilemCookbookCatalog.catalogSize());
        Set<String> ids=new HashSet<>();
        for(int i=0;i<all.length();i++){
            JSONObject r=all.getJSONObject(i);
            assertTrue("id vazio",!r.getString("id").isEmpty());
            assertTrue("id duplicado: "+r.getString("id"),ids.add(r.getString("id")));
            assertTrue("sem preparo",r.getInt("minutes")>0);
            assertTrue("sem porcoes",r.getInt("servings")>0);
            assertTrue("cd invalida",r.getInt("dc")>=8 && r.getInt("dc")<=22);
            assertTrue("efeito nao estruturado",!r.getString("effect_key").isEmpty());
            assertTrue("receita sem ingredientes",r.getJSONObject("ingredients").length()>0);
            assertEquals("braseiro_original",r.getString("source"));
        }
    }
    @Test public void allIngredientsResolveInCanonCatalog() throws Exception {
        JSONArray recipes=WilemCookbookCatalog.recipes();
        for(int i=0;i<recipes.length();i++){
            JSONObject recipe=recipes.getJSONObject(i), ingredients=recipe.getJSONObject("ingredients");
            JSONArray keys=ingredients.names();assertNotNull(keys);
            for(int j=0;j<keys.length();j++){
                String id=keys.getString(j);
                assertTrue("desconhecido: "+id,WilemCookbookCatalog.ingredientById(id).length()>0);
                assertTrue("quantidade invalida: "+recipe.getString("id"),ingredients.getInt(id)>0);
            }
        }
    }
    @Test public void starterRecipesAreRealAndEverydayFoodHasNoCombatBonus() throws Exception {
        JSONArray ids=WilemCookbookCatalog.starterIds();
        assertTrue(ids.length()>=6);
        for(int i=0;i<ids.length();i++)
            assertTrue("entrada inicial inexistente",WilemCookbookCatalog.recipeById(ids.getString(i)).length()>0);
        JSONArray recipes=WilemCookbookCatalog.recipes();
        for(int i=0;i<recipes.length();i++){
            JSONObject r=recipes.getJSONObject(i);
            if("cotidiana".equals(r.getString("type")))assertEquals("alimentacao",r.getString("effect_key"));
        }
    }
    @Test public void sleepingBaitsAreMagicalAndRequireExplicitSaves() throws Exception {
        JSONArray recipes=WilemCookbookCatalog.recipes();int count=0;
        for(int i=0;i<recipes.length();i++){
            JSONObject r=recipes.getJSONObject(i);
            if(!"isca".equals(r.getString("type")))continue;
            count++;assertTrue(r.getBoolean("requires_consumption"));
            if(r.getString("effect_key").matches("isca_(sonolencia|tontura|lentidao)")){
                assertTrue(r.getBoolean("requires_save"));
                JSONObject ingredients=r.getJSONObject("ingredients");JSONArray keys=ingredients.names();
                boolean magical=false;
                for(int j=0;j<keys.length();j++)if("magico".equals(WilemCookbookCatalog.ingredientById(keys.getString(j)).optString("category")))magical=true;
                assertTrue("sedacao sem componente fantastico",magical);
            }
        }
        assertEquals(12,count);
    }
}
