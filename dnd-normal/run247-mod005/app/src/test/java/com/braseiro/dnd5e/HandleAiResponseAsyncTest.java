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

import static org.junit.Assert.assertEquals;
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
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * The asynchronous adapter must not silently drop an otherwise safe
     * real-world scene. Keep the full rejection report in the assertion.
     */
    @Test
    public void simpleInWorldScenePassesStrictMasterValidation() throws Exception {
        JSONObject fixture=new JSONObject()
            .put("protocol_version",MasterProtocolV1.VERSION)
            .put("narration","No pátio, a chuva fina escurece as pedras antigas. Um mercador fecha sua barraca sob o toldo.")
            .put("options",new JSONArray());
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("masterOrchestrator");
        field.setAccessible(true);
        MasterOrchestrator core=(MasterOrchestrator)field.get(activity);
        assertNotNull("MainActivity must construct the real master arbiter",core);
        JSONObject checked=core.validateEnvelope(fixture,"Observo o pátio");
        JSONObject verdict=checked.optJSONObject("_master_validation");
        assertNotNull("The arbiter must report a verdict: "+checked,verdict);
        assertTrue("Safe scene rejected by strict master gate: "+verdict+" / "+checked.optJSONObject("_rules_report"),verdict.optBoolean("valid",false));
    }

    @Test
    public void testHandleAiResponseExecutesAsynchronouslyAndAppliesToUi() throws Exception {
        JSONObject envelope = new JSONObject().put("protocol_version",MasterProtocolV1.VERSION);
        envelope.put("narration", "No pátio, a chuva fina escurece as pedras antigas. Um mercador fecha sua barraca sob o toldo.");
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
                activity.lastNarration.contains("a chuva fina escurece"));

        // Check that story was persisted to database
        JSONObject story = activity.db.getStoryState();
        assertNotNull(story);
        assertTrue("Persistência SQLite da narrativa deve estar consistente",
                story.optString("last_narration", "").contains("a chuva fina escurece"));

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
    @Test
    public void testConcurrentStaleResponseFinishingLastDoesNotOverwriteDatabaseOrUi() throws Exception {
        // Geração inicial
        long genA = ++activity.aiRequestGeneration;

        JSONObject envA = new JSONObject().put("protocol_version",MasterProtocolV1.VERSION);
        envA.put("narration", "Uma carroça vermelha atravessa o pátio e desaparece atrás do muro.");
        envA.put("visual_prompt", "Visual A obsoleto");
        envA.put("options", new JSONArray());
        envA.put("_master_validation", new JSONObject().put("valid", true));

        JSONObject envB = new JSONObject().put("protocol_version",MasterProtocolV1.VERSION);
        envB.put("narration", "O sino da torre soa três vezes enquanto a chuva engrossa diante das barracas do pátio.");
        envB.put("visual_prompt", "Visual B legítimo");
        envB.put("options", new JSONArray());
        envB.put("_master_validation", new JSONObject().put("valid", true));

        final java.util.concurrent.CountDownLatch aStarted = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch bFinished = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch aFinished = new java.util.concurrent.CountDownLatch(1);

        // Thread para executar a resposta A
        Thread threadA = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Resposta A inicia o processamento com genA
                    aStarted.countDown();
                    // Aguarda a resposta B substituir A e terminar o processamento
                    bFinished.await();
                    // Resposta A tenta processar até o fim, terminando POR ÚLTIMO
                    activity.processAiResponseInBackground(envA.toString(), "Observo a carroça", genA);
                } catch (Exception ignored) {
                } finally {
                    aFinished.countDown();
                }
            }
        });

        threadA.start();
        aStarted.await();

        // Resposta B substitui A (incrementa geração e processa primeiro)
        long genB = ++activity.aiRequestGeneration;
        activity.processAiResponseInBackground(envB.toString(), "Observo o pátio", genB);

        // Notifica thread A que B terminou
        bFinished.countDown();

        // Aguarda A terminar por último
        aFinished.await();
        threadA.join();

        // Processa eventos da fila UI
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // 1. Verificação da UI: Somente B pode estar na UI
        assertNotNull(activity.lastNarration);
        assertTrue("A UI deve refletir exclusivamente a resposta B",
                activity.lastNarration.contains("O sino da torre soa três vezes"));
        assertFalse("A UI NUNCA pode ser contaminada pela resposta obsoleta A",
                activity.lastNarration.contains("Uma carroça vermelha atravessa o pátio"));

        // 2. Verificação do Banco de Dados SQLite: Somente B pode estar persistido no AppDb
        JSONObject story = activity.db.getStoryState();
        assertNotNull(story);
        String persistedNarration = story.optString("last_narration", "");
        assertTrue("O banco de dados deve conter exclusivamente a narrativa da resposta B",
                persistedNarration.contains("O sino da torre soa três vezes"));
        assertFalse("O banco de dados NUNCA pode ter sido sobrescrito pela resposta obsoleta A",
                persistedNarration.contains("Uma carroça vermelha atravessa o pátio"));
        assertEquals("A última ação persistida deve ser a da resposta B",
                "Observo o pátio", story.optString("last_action", ""));
    }
}