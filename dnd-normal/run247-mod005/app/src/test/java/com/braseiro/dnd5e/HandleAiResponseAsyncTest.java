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
    @Test
    public void testConcurrentStaleResponseFinishingLastDoesNotOverwriteDatabaseOrUi() throws Exception {
        // Geração inicial
        long genA = ++activity.aiRequestGeneration;

        JSONObject envA = new JSONObject();
        envA.put("narration", "RESPOSTA_OBSOLETA_A: Este texto nunca deve aparecer no banco nem na UI.");
        envA.put("visual_prompt", "Visual A obsoleto");
        envA.put("options", new JSONArray());
        envA.put("_master_validation", new JSONObject().put("valid", true));

        JSONObject envB = new JSONObject();
        envB.put("narration", "RESPOSTA_LEGITIMA_B: Este texto DEVE estar salvo no banco e visível na UI.");
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
                    activity.processAiResponseInBackground(envA.toString(), "Ação A", genA);
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
        activity.processAiResponseInBackground(envB.toString(), "Ação B", genB);

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
                activity.lastNarration.contains("RESPOSTA_LEGITIMA_B"));
        assertFalse("A UI NUNCA pode ser contaminada pela resposta obsoleta A",
                activity.lastNarration.contains("RESPOSTA_OBSOLETA_A"));

        // 2. Verificação do Banco de Dados SQLite: Somente B pode estar persistido no AppDb
        JSONObject story = activity.db.getStoryState();
        assertNotNull(story);
        String persistedNarration = story.optString("last_narration", "");
        assertTrue("O banco de dados deve conter exclusivamente a narrativa da resposta B",
                persistedNarration.contains("RESPOSTA_LEGITIMA_B"));
        assertFalse("O banco de dados NUNCA pode ter sido sobrescrito pela resposta obsoleta A",
                persistedNarration.contains("RESPOSTA_OBSOLETA_A"));
        assertEquals("A última ação persistida deve ser a da resposta B",
                "Ação B", story.optString("last_action", ""));
    }

    @Test
    public void testPreparedOldResponseFinishingAfterNewerCommitCannotMutateStory() throws Exception {
        final java.util.concurrent.CountDownLatch aPrepared = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch resumeA = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<Throwable> aError = new java.util.concurrent.atomic.AtomicReference<>();
        final JSONObject a = new JSONObject().put("narration", "A_ANTIGA: não pode modificar a campanha.")
                .put("options", new JSONArray());
        final JSONObject b = new JSONObject().put("narration", "B_ATUAL: o pátio de Strixhaven recebe os alunos.")
                .put("options", new JSONArray());
        final long genA = activity.advanceAiRequestGeneration();
        activity.preCommitObserverForTests = (generation, action) -> {
            if (generation == genA) {
                aPrepared.countDown();
                try {
                    if (!resumeA.await(25, java.util.concurrent.TimeUnit.SECONDS))
                        throw new AssertionError("A commit gate was not released");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interrupted pre-commit gate", ex);
                }
            }
        };
        Thread workerA = new Thread(() -> {
            try {
                activity.processAiResponseInBackground(a.toString(), "Ação A", genA);
            } catch (Throwable error) {
                aError.set(error);
            }
        }, "prepared-stale-reply-A");
        workerA.start();
        try {
            assertTrue("A reached pre-commit fence before B", aPrepared.await(25, java.util.concurrent.TimeUnit.SECONDS));
            final long genB = activity.advanceAiRequestGeneration();
            activity.processAiResponseInBackground(b.toString(), "Ação B", genB);
        } finally {
            resumeA.countDown();
            workerA.join(25000L);
            activity.preCommitObserverForTests = null;
        }
        assertFalse("A thread must terminate", workerA.isAlive());
        assertTrue("A must not crash", aError.get() == null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        String persisted = activity.db.getStoryState().optString("last_narration", "");
        assertTrue("Only B may reach SQLite", persisted.contains("B_ATUAL"));
        assertFalse("A cannot reach SQLite", persisted.contains("A_ANTIGA"));
        assertTrue("UI reflects B", activity.lastNarration.contains("B_ATUAL"));
        assertFalse("UI cannot show A", activity.lastNarration.contains("A_ANTIGA"));
    }
}