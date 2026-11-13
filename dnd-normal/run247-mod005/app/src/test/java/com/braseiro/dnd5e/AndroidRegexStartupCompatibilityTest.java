package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Startup-regression coverage for Android's ICU regex incompatibility.
 * Android does not accept the Java-only inline (?U) flag. The workflow
 * additionally scans all production Java sources and rejects (?U)/(?u).
 */
public final class AndroidRegexStartupCompatibilityTest {
    @Test public void namedNpcActionIsRecognizedWithoutAndroidUnsupportedFlag() throws Exception {
        String scene="Lyra conversa no Biblioplex com os estudantes e apresenta o calendário.";
        assertTrue(SceneActionEvidence.namedReferencesExist(scene,
            "Pergunto a Lyra sobre o calendário do Biblioplex."));
        assertFalse(SceneActionEvidence.namedReferencesExist(scene,
            "Pergunto a Varissa sobre o calendário do Biblioplex."));

        JSONObject choice=new JSONObject()
            .put("type","CONVERSAR").put("action_type","CONVERSAR")
            .put("label","CONVERSAR • PERGUNTAR A LYRA")
            .put("action","Pergunto a Lyra sobre o calendário do Biblioplex.");
        JSONObject safe=SceneActionEvidence.bind(choice,scene);
        assertTrue("The startup action gate must accept a scene-bound action",
            SceneActionEvidence.ticketMatches(safe,scene));
    }

    @Test public void dialogueInterlocutorsStillWorkWithoutInlineUnicodeFlags() {
        String scene="Aisla solta uma risada, atende Kvothe e diz: — Conte-me.";
        assertEquals("Aisla",SceneDialogueDecisionPlanner.namedInterlocutor(scene));
        assertEquals("Lyra",SceneDialogueDecisionPlanner.namedInterlocutor(
            "Lyra: — As inscrições começam agora."));
    }
}
