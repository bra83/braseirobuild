package com.braseiro.dnd5e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class Core2024SubclassRobolectricTest {

    private static String databaseName() {
        return "subc_" + Long.toHexString(System.nanoTime() & 0xFFFFFL) + ".db";
    }

    @Test
    public void exactlyFortyEightOfficialPhb2024SubclassesAcrossTwelveClasses() {
        assertEquals("Exatamente 48 subclasses oficiais no PHB 2024", 48,
            Core2024SubclassRuntimeEngine.totalSubclassesCount());

        String[] coreClasses = {
            "Bárbaro", "Bardo", "Clérigo", "Druida",
            "Guerreiro", "Monge", "Paladino", "Guardião",
            "Ladino", "Feiticeiro", "Bruxo", "Mago"
        };

        for (String className : coreClasses) {
            List<Core2024SubclassRuntimeEngine.SubclassDef> list =
                Core2024SubclassRuntimeEngine.getSubclassesForClass(className);
            assertEquals("Classe " + className + " deve ter exatamente 4 subclasses oficiais no PHB 2024",
                4, list.size());
            for (Core2024SubclassRuntimeEngine.SubclassDef sub : list) {
                assertNotNull(sub.name);
                assertNotNull(sub.nameEn);
                assertNotNull(sub.locator);
                assertTrue("Subclasse " + sub.name + " deve ter ao menos 2 features definidas",
                    sub.features.size() >= 2);
                boolean hasLevel3 = false;
                for (Core2024SubclassRuntimeEngine.SubclassFeature feat : sub.features) {
                    if (feat.level == 3) hasLevel3 = true;
                }
                assertTrue("Subclasse " + sub.name + " deve ter obrigatoriamente recurso no nível 3",
                    hasLevel3);
            }
        }
    }

    @Test
    public void levelGateThreeIsRequiredFor2024Subclasses() {
        assertFalse("Nível 1 não pode ter subclasse no PHB 2024",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(1));
        assertFalse("Nível 2 não pode ter subclasse no PHB 2024",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(2));
        assertTrue("Nível 3 desbloqueia subclasse no PHB 2024",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(3));
        assertTrue("Nível 5 mantém subclasse válida",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(5));
        assertTrue("Nível 20 mantém subclasse válida",
            Core2024SubclassRuntimeEngine.isSubclassLevelValid(20));
    }

    @Test
    public void lookupByPtBrAndEnglishNamesBothResolve() {
        Core2024SubclassRuntimeEngine.SubclassDef pt =
            Core2024SubclassRuntimeEngine.getSubclass("Mestre da Batalha");
        assertNotNull(pt);
        assertEquals("Guerreiro", pt.className);

        Core2024SubclassRuntimeEngine.SubclassDef en =
            Core2024SubclassRuntimeEngine.getSubclass("Battle Master");
        assertNotNull(en);
        assertEquals(pt.name, en.name);

        Core2024SubclassRuntimeEngine.SubclassDef champion =
            Core2024SubclassRuntimeEngine.getSubclass("Campeão");
        assertNotNull(champion);
        assertEquals(3, champion.features.get(0).level);
        assertEquals("improved_critical", champion.features.get(0).id);
    }

    @Test
    public void persistedCharacterSubclassIntegrationAndReopen() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String dbName = databaseName();
        AppDb db = new AppDb(context, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long charId = db.addCharacter(
                "Kaelen", "Humano", "Guerreiro", 3, 28, 28, 18,
                16, 12, 14, 10, 12, 10, true, "Soldado");

            // Associa a subclasse oficial Campeão no sheet_json
            JSONObject ch = db.getCharacter(charId);
            JSONObject sheet = new JSONObject(ch.optString("sheet_json", "{}"));
            sheet.put("subclass_2024", "Campeão");
            sheet.put("crit_threshold", 19);
            db.updateCharacterSheet(charId, sheet.toString());

            // Reabertura
            db.close();
            db = new AppDb(context, dbName);
            db.setActiveMapProfile("dnd");

            JSONObject reopened = db.getCharacter(charId);
            JSONObject reopenedSheet = new JSONObject(reopened.optString("sheet_json", "{}"));
            assertEquals("Campeão", reopenedSheet.optString("subclass_2024"));
            assertEquals(19, reopenedSheet.optInt("crit_threshold"));
        } finally {
            db.close();
            context.deleteDatabase(dbName);
        }
    }
}
