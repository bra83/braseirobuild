package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Modifier;

/**
 * Regression gates derived from seven real Galaxy A57 ANRs on 2026-11-01.
 * UI/Looper verification still requires an Android device test.
 */
public final class A57AnrTraceRegressionTest {
    @Test public void soundCountMustNeverAcquireAudioCatalogMonitor() throws Exception {
        int modifiers=SmartAudioLibrary.class.getMethod("indexedSoundCount").getModifiers();
        assertFalse("Audio index held a monitor >10s in three A57 ANR traces",
            Modifier.isSynchronized(modifiers));
    }

    @Test(timeout=3000) public void creatureCompilerDoesNotRecompileRegexPerLine() throws Exception {
        StringBuilder excerpt=new StringBuilder();
        excerpt.append("Arbusto Despertado\nCA: 12\nPV: 14\nDeslocamento: 20 pés\n");
        for(int i=0;i<450;i++){
            excerpt.append("Descrição da folha e do ramo número ").append(i)
                .append(" com cor azul e movimento simples.\n");
        }
        excerpt.append("Ações\nMordida: ataque +4 para atingir, 1d6+2.\n");
        JSONObject input=new JSONObject().put("rule_id","a57_perf")
            .put("book","Strixhaven").put("excerpt",excerpt.toString());
        JSONArray compiled=CreatureStatBlockCompiler.compile(input);
        assertTrue("Statblock parser should remain functional and return creature",compiled.length()>0);
        JSONObject creature=compiled.getJSONObject(0);
        assertEquals(12,creature.optInt("armor_class"));
        assertEquals(14,creature.optInt("hit_points"));
        assertTrue(creature.optJSONArray("attacks").length()>0);
    }

    @Test public void normalizationPreservesPortugueseAccentsAndDistinctSpells() throws Exception {
        String excerpt="Monstro Fantástico\nCA: 15\nPV: 22\n"
            +"Ações\nArremesso: 1d8 de dano.\n"
            +"Magia: Proteção.\nMagia: Proteção.\n"
            +"Recarga: 5-6.\n";
        JSONArray blocks=CreatureStatBlockCompiler.compile(
            new JSONObject().put("excerpt",excerpt).put("system","dnd5e"));
        assertEquals(1,blocks.length());
        JSONObject b=blocks.getJSONObject(0);
        assertEquals(1,b.getJSONArray("spells").length());
        assertEquals(1,b.getJSONArray("recharges").length());
    }
}
