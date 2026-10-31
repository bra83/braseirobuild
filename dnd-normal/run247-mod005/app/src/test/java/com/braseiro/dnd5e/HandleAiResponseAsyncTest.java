package com.braseiro.dnd5e;

import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes de integração para a execução assíncrona do handleAiResponse.
 * Verifica que processamento, validação e persistência ocorrem fora da thread principal
 * e postam na UI sem travar o looper principal nem quebrar a consistência SQLite.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class HandleAiResponseAsyncTest {

    private ActivityController<MainActivity> controller;
    private MainActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        activity = controller.get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.pause().stop().destroy();
        }
    }

    @Test
    public void testHandleAiResponseExecutesAsynchronouslyAndAppliesToUi() throws Exception {
        JSONObject envelope = new JSONObject();
        envelope.put("narration", "Kvothe ajusta o manto de viagem enquanto a chuva leve molha as pedras do pátio central de Baldur's Gate.");
        envelope.put("visual_prompt", "Pátio de pedras antigas, chuva fina, arquitetura gótica de Baldur's Gate");
        JSONArray options = new JSONArray();
        JSONObject opt1 = new JSONObject().put("label", "Entrar na estalagem").put("action", "Entrar na estalagem");
        options.put(opt1);
        envelope.put("options", options);
        JSONObject validation = new JSONObject().put("valid", true);
        envelope.put("_master_validation", validation);

        // Trigger handleAiResponse
        activity.handleAiResponse(envelope.toString(), "Observar o pátio");

        // Wait for background AppExecutors.io() task to complete and post to UI
        long deadline = System.currentTimeMillis() + 8000L;
        while (System.currentTimeMillis() < deadline && (activity.lastNarration == null || activity.lastNarration.isEmpty())) {
            Thread.sleep(50);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }

        // Final idle pass to ensure all UI callbacks processed
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Check that narration was updated in activity
        assertNotNull(activity.lastNarration);
        assertTrue("A narração deve ter sido aplicada na UI",
                activity.lastNarration.contains("Kvothe ajusta o manto"));

        // Check that story was persisted to database
        JSONObject story = activity.db.getStoryState();
        assertNotNull(story);
        assertTrue("Persistência SQLite da narrativa deve estar consistente",
                story.optString("last_narration", "").contains("Kvothe ajusta o manto"));

        // Check footer status
        assertNotNull(activity.statusText);
        assertNotNull(activity.statusText.getText());
        assertTrue("Status deve estar visível e preenchido",
                activity.statusText.getText().length() > 0);
    }

    @Test
    public void testOutdatedGenerationDoesNotOverwriteActiveUiState() throws Exception {
        JSONObject validEnv = new JSONObject();
        validEnv.put("narration", "Cena atual válida e mais recente.");
        validEnv.put("_master_validation", new JSONObject().put("valid", true));

        // Advance generation
        long currentGen = ++activity.aiRequestGeneration;

        // An older response arrives with a stale generation (currentGen - 1)
        JSONObject staleEnv = new JSONObject();
        staleEnv.put("narration", "Cena atrasada que não deve aparecer na tela.");
        staleEnv.put("_master_validation", new JSONObject().put("valid", true));

        activity.processAiResponseInBackground(staleEnv.toString(), "Ação atrasada", currentGen - 1);
        Thread.sleep(200);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Stale narration must NOT overwrite active narration
        assertFalse("Narração atrasada não pode sobrescrever a UI",
                activity.lastNarration != null && activity.lastNarration.contains("Cena atrasada que não deve aparecer"));
    }
}
