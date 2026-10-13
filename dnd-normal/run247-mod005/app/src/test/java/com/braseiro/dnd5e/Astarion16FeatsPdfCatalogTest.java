package com.braseiro.dnd5e;

import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PDF source: Astarion's Book of Hungers, user Drive file, Ch. 1.
 * Checks every named feat against the actual supplied source roster and
 * the three epic boon contracts. Only roster and these contracts are tested.
 */
public final class Astarion16FeatsPdfCatalogTest {
    private static final String[] ORIGIN={"Tireless Reveler","Vampire Hunter","Vampire's Plaything"};
    private static final String[] GENERAL={
            "Bloodlust","Bomber","Cloying Mists","Delicious Pain","Light Bringer",
            "Love Bites","Putrefy","Rebuke","Treacherous Allure","Vampire Touched"
    };
    private static final String[] EPIC={
            "Boon of Blazing Dawn","Boon of Looming Shadows","Boon of Misty Escape"
    };

    @Test public void exactSixteenSourceFeatsAndCategories() {
        JSONArray actual=new AstarionHungersRulesEngine().listVampiricFeats();
        assertEquals("Original PDF lists 3 origin, 10 general, 3 epic boons",16,actual.length());
        Set<String> expected=new HashSet<>();
        for(String name:ORIGIN) expected.add("Origin:"+name);
        for(String name:GENERAL) expected.add("General:"+name);
        for(String name:EPIC) expected.add("Epic Boon:"+name);
        Set<String> found=new HashSet<>();
        for(int i=0;i<actual.length();i++) {
            JSONObject f=actual.optJSONObject(i);
            assertNotNull(f);
            String category=f.optString("category");
            assertTrue("Category missing from source: "+category,
                    "Origin".equals(category)||"General".equals(category)||"Epic Boon".equals(category));
            int level=f.optInt("level_required");
            assertEquals("Incorrect feat prerequisite for "+f.optString("name"),
                    "Origin".equals(category)?1:("General".equals(category)?4:19),level);
            String uniqueKey=category+":"+f.optString("name");
            assertTrue("Duplicate feat: "+uniqueKey,found.add(uniqueKey));
        }
        assertEquals("Mismatch with PDF feats",expected,found);
    }

    @Test public void sourceSpecificEpicBoonsHaveDistinctRealBehaviors() {
        AstarionHungersRulesEngine e=new AstarionHungersRulesEngine();
        JSONObject dawn=e.getEpicBoon("Boon of Blazing Dawn");
        assertEquals(19,dawn.optInt("level_required"));
        assertTrue(dawn.optBoolean("radiant_immunity"));
        assertTrue(dawn.optBoolean("weapon_damage_may_be_radiant"));
        JSONObject shadows=e.getEpicBoon("Boon of Looming Shadows");
        assertEquals(10,shadows.optInt("melee_reach_bonus_on_attack_action_ft"));
        assertTrue(shadows.optBoolean("dodge_as_bonus_action"));
        JSONObject escape=e.getEpicBoon("Boon of Misty Escape");
        assertEquals(1,escape.optInt("instead_hp"));
        assertEquals(20,escape.optInt("fly_speed_ft"));
        assertEquals(10,escape.optInt("healing_each_turn"));
        assertEquals(1,escape.optInt("uses_per_long_rest"));
        assertFalse(e.getEpicBoon("Unpublished Boon").optBoolean("source_verified",true));
    }

    @Test public void noDhampirBiteResourceSpendWithoutDamageOrValidTarget() {
        AstarionHungersRulesEngine e=new AstarionHungersRulesEngine();
        assertFalse(e.resolveVampiricBiteEmpowerment(-1,"humanoid","drain",3,0).optBoolean("resolved"));
        assertFalse(e.resolveVampiricBiteEmpowerment(0,"humanoid","drain",3,0).optBoolean("resolved"));
        assertFalse(e.resolveVampiricBiteEmpowerment(5,"","drain",3,0).optBoolean("resolved"));
        assertFalse(e.resolveVampiricBiteEmpowerment(5,"undead","drain",3,0).optBoolean("resolved"));
        assertFalse(e.resolveVampiricBiteEmpowerment(5,"humanoid","drain",0,0).optBoolean("resolved"));
        assertFalse(e.resolveVampiricBiteEmpowerment(5,"humanoid","drain",3,-1).optBoolean("resolved"));
        JSONObject success=e.resolveVampiricBiteEmpowerment(5,"humanoid","drain",3,0);
        assertTrue(success.optBoolean("resolved"));
        assertEquals(5,success.optInt("healing"));
        assertEquals(1,success.optInt("uses_spent_after"));
    }
}
