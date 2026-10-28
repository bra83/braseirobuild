package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import static org.junit.Assert.*;

/**
 * No generation success until the same fields survive SQL write and 3 reopen
 * cycles. The random generator can never replace a pre-existing hero silently.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class Character2024RandomPersistenceTest {
    @Test public void newlyGeneratedHumanAndMonkSurviveThreeSqliteReopens() throws Exception{
        Context app=RuntimeEnvironment.getApplication();
        for(String sp:new String[]{"Humano","Orc"}){
            String dbName="random2024-"+sp+"-"+System.nanoTime()+".db";
            AppDb db=null;
            try{
                CharacterSheetGenerator generator=new CharacterSheetGenerator(new DndRulesEngine());
                JSONObject character=null;
                for(int attempt=0;attempt<100;attempt++){
                    JSONObject sample=generator.randomComplete(DndRulesEngine.PROFILE_2024,
                        new String[]{sp},new ArrayList<JSONObject>());
                    if(sample.optBoolean("generation_valid",false)){
                        character=sample;
                        break;
                    }
                }
                assertNotNull("No legal 2024 random character for "+sp,character);
                String name=character.optString("name");
                String beforeInventory=character.optString("inventory");
                String beforeEquipment=character.optString("equipment");
                String beforeLedger=character.optJSONArray("inventory_ledger").toString();
                JSONObject randomPersona=character.optJSONObject("personality_profile");
                assertTrue(character.toString(),BraseiroRandomPersonalityEngine.validateCompleteRandom(randomPersona));
                String beforePersona=randomPersona.toString();
                String beforeOrigin=character.optString("origin_ability_bonus");
                JSONObject beforePurchase=character.getJSONObject("ability_purchase");
                JSONArray beforeFeats=character.optJSONArray("selected_origin_feats");
                int[] abilities={character.getInt("str"),character.getInt("dex"),
                    character.getInt("con"),character.getInt("int_score"),
                    character.getInt("wis"),character.getInt("cha")};
                db=new AppDb(app,dbName);
                db.setActiveMapProfile("dnd");
                JSONObject receipt=db.saveCharacterSheetAtomic(0,false,character,"","manual");
                assertTrue(receipt.toString(),receipt.optBoolean("character_saved"));
                long id=receipt.getLong("character_id");
                assertTrue(id>0);
                for(int iteration=0;iteration<3;iteration++){
                    db.close();db=new AppDb(app,dbName);db.setActiveMapProfile("dnd");
                    JSONObject row=db.getCharacter(id);
                    assertEquals(id,row.getLong("id"));
                    JSONObject stored=new JSONObject(row.getString("sheet_json"));
                    assertEquals(sp,stored.optString("species"));
                    assertEquals(name,stored.optString("name"));
                    assertEquals(beforeInventory,stored.optString("inventory"));
                    assertEquals(beforeEquipment,stored.optString("equipment"));
                    assertEquals(beforeLedger,stored.optJSONArray("inventory_ledger").toString());
                    assertEquals(beforePersona,stored.getJSONObject("personality_profile").toString());
                    assertEquals(39,stored.getJSONObject("personality_profile").getJSONArray("traits").length());
                    assertEquals(beforeOrigin,stored.optString("origin_ability_bonus"));
                    assertEquals(beforePurchase.getJSONObject("background_bonus").toString(),
                        stored.getJSONObject("ability_purchase").getJSONObject("background_bonus").toString());
                    assertEquals(beforeFeats.toString(),stored.optJSONArray("selected_origin_feats").toString());
                    String[] keys={"str","dex","con","int_score","wis","cha"};
                    for(int i=0;i<keys.length;i++)assertEquals(keys[i],abilities[i],stored.getInt(keys[i]));
                    assertFalse(stored.toString(),stored.optBoolean("origin_ability_choice_pending",true));
                    assertFalse(stored.toString(),stored.optBoolean("human_extra_origin_feat_pending",false));
                    assertFalse(stored.toString(),stored.optString("features").contains("Urdunnir"));
                    assertFalse(stored.toString(),stored.optJSONObject("species_resources")!=null &&
                        stored.optJSONObject("species_resources").has("urdunnir_stone_shape"));
                    JSONObject saved=db.saveCharacterSheetAtomic(id,true,stored,"","manual");
                    assertTrue(saved.toString(),saved.optBoolean("character_saved"));
                    assertEquals(id,saved.getLong("character_id"));
                    assertEquals(1,db.listCharacters().size());
                }
            }finally{
                if(db!=null)db.close();
                app.deleteDatabase(dbName);
            }
        }
    }
}
