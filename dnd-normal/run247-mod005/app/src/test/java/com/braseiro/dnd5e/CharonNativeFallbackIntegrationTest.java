package com.braseiro.dnd5e;

import android.os.Looper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowTextToSpeech;
import java.util.Locale;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes de integração do pipeline de áudio real do MainActivity.
 * Verifica que fallback de Charon para Android TTS / Kokoro:
 * 1. Não cancela a máquina de estados antes do handover.
 * 2. Não perde a geração da sessão ativa.
 * 3. Transiciona para FALLBACK_PLAYING sem repetir nem cortar trechos.
 * 4. Conclui com sucesso quando o motor de voz alternativo termina.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class CharonNativeFallbackIntegrationTest {

    private ActivityController<MainActivity> controller;
    private MainActivity activity;

    @Before
    public void setUp() {
        ShadowTextToSpeech.addLanguageAvailability(new Locale("pt", "BR"));
        ShadowTextToSpeech.addLanguageAvailability(new Locale("pt"));
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
        ShadowTextToSpeech.reset();
    }

    @Test
    public void testImmediateFallbackPreservesStateMachineAndTransitionsToFallbackPlaying() throws Exception {
        long session = activity.charonNarrationGeneration;
        List<String> chunks = Arrays.asList(
                "A companhia alcança as portas do Biblioplex sob a luz da manhã.",
                "Transeuntes observam a chegada enquanto guardas mantêm a vigília."
        );
        activity.narrationStateMachine.startSession(session, session, chunks, true, NarrationPlaybackStateMachine.VoiceSource.CHARON);
        assertEquals(NarrationPlaybackStateMachine.State.PREPARING, activity.narrationStateMachine.getState());

        // Charon fails on initial attempt with 429 quota exhaustion
        activity.narrationStateMachine.onSynthesisFailed(0, session, "HTTP 429 Resource Exhausted", 429, true);
        assertEquals(NarrationPlaybackStateMachine.State.FALLBACK_PENDING, activity.narrationStateMachine.getState());

        // Trigger real fallbackToNativeNarration from MainActivity
        activity.fallbackToNativeNarration("A companhia alcança as portas do Biblioplex sob a luz da manhã. Transeuntes observam a chegada enquanto guardas mantêm a vigília.", session, "429 Quota");
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Critical verification: state machine must NOT be CANCELLED!
        assertNotEquals("Máquina de estados NÃO pode ser cancelada pelo fallback",
                NarrationPlaybackStateMachine.State.CANCELLED, activity.narrationStateMachine.getState());
        assertEquals("Máquina de estados deve estar em FALLBACK_PLAYING",
                NarrationPlaybackStateMachine.State.FALLBACK_PLAYING, activity.narrationStateMachine.getState());
        assertNotEquals(NarrationPlaybackStateMachine.VoiceSource.CHARON, activity.narrationStateMachine.getActiveVoiceSource());
        assertTrue("activeFallbackSession deve rastrear a sessão ativa", activity.activeFallbackSession == session);

        // Native voice finishes reading
        completeWithNativeUtteranceCallbacks();

        assertEquals("Máquina de estados deve concluir com sucesso",
                NarrationPlaybackStateMachine.State.COMPLETED, activity.narrationStateMachine.getState());
        assertEquals(-1L, activity.activeFallbackSession);
    }

    @Test
    public void testLateFallbackPreservesPlayedPortionAndContinuesOnlyUnheardRemainder() throws Exception {
        long session = activity.charonNarrationGeneration;
        List<String> chunks = Arrays.asList(
                "Primeiro trecho ouvido com sucesso na voz Charon.",
                "Segundo trecho falha de conexão na API Gemini.",
                "Terceiro trecho deve continuar na voz nativa."
        );
        activity.narrationStateMachine.startSession(session, session, chunks, true, NarrationPlaybackStateMachine.VoiceSource.CHARON);

        // Chunk 0 is synthesized, starts, and completes
        activity.narrationStateMachine.onSynthesisRequested(0, session);
        activity.narrationStateMachine.onChunkSynthesized(0, session);
        activity.narrationStateMachine.onChunkPlaybackStarted(0, session);
        activity.narrationStateMachine.onChunkPlaybackCompleted(0, session);
        assertEquals(0, activity.narrationStateMachine.getCompletedPlaybackIndex());

        // Chunk 1 fails synthesis
        activity.narrationStateMachine.onSynthesisFailed(1, session, "500 Internal Error", 500, false);
        assertEquals(NarrationPlaybackStateMachine.State.FALLBACK_PENDING, activity.narrationStateMachine.getState());

        // Verify unheard calculation before fallback call
        assertEquals(1, activity.narrationStateMachine.getFirstUnheardChunkIndex());
        String unheardRemainder = activity.narrationStateMachine.getUnheardRemainingText();
        assertFalse("Não deve incluir bloco 0 que já foi ouvido", unheardRemainder.contains("Primeiro trecho"));
        assertTrue("Deve conter bloco 1", unheardRemainder.contains("Segundo trecho"));
        assertTrue("Deve conter bloco 2", unheardRemainder.contains("Terceiro trecho"));

        // Late handover runner executes
        activity.waitForCharonQueueThenContinueNative(unheardRemainder, session, activity.activeAudioSession, System.currentTimeMillis() + 1000L);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertNotEquals(NarrationPlaybackStateMachine.State.CANCELLED, activity.narrationStateMachine.getState());
        assertEquals(NarrationPlaybackStateMachine.State.FALLBACK_PLAYING, activity.narrationStateMachine.getState());
        assertEquals(session, activity.activeFallbackSession);

        // Finishing native voice marks total session completed
        completeWithNativeUtteranceCallbacks();

        assertEquals(NarrationPlaybackStateMachine.State.COMPLETED, activity.narrationStateMachine.getState());
    }

    @Test
    public void testExplicitStopCancelsStateMachineAndAudioQueue() {
        long session = activity.charonNarrationGeneration;
        List<String> chunks = Arrays.asList("Trecho 1", "Trecho 2");
        activity.narrationStateMachine.startSession(session, session, chunks, true, NarrationPlaybackStateMachine.VoiceSource.CHARON);

        activity.cancelNarrationAudio();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertEquals(NarrationPlaybackStateMachine.State.CANCELLED, activity.narrationStateMachine.getState());
        assertEquals(-1L, activity.activeFallbackSession);
    }
    @Test
    public void testFallbackWhenNativeTtsNotYetInitializedResumesOriginalSessionWithoutCancelingOrRepeating() throws Exception {
        // 1. Simulate TTS engine NOT yet initialized
        activity.nativeTtsReady = false;
        activity.nativeTtsLanguageReady = false;

        long session = ++activity.charonNarrationGeneration;
        List<String> chunks = Arrays.asList(
                "Primeiro trecho ouvido perfeitamente com a voz Charon.",
                "Segundo trecho que sofreu falha de conexão na API.",
                "Terceiro trecho a ser continuado pela voz nativa sem perda de estado."
        );
        activity.narrationStateMachine.startSession(session, session, chunks, true, NarrationPlaybackStateMachine.VoiceSource.CHARON);

        // Chunk 0 finishes playing with Charon
        activity.narrationStateMachine.onSynthesisRequested(0, session);
        activity.narrationStateMachine.onChunkSynthesized(0, session);
        activity.narrationStateMachine.onChunkPlaybackStarted(0, session);
        activity.narrationStateMachine.onChunkPlaybackCompleted(0, session);

        // Chunk 1 fails synthesis -> triggers fallback
        activity.narrationStateMachine.onSynthesisFailed(1, session, "HTTP 503 Backend Unavailable", 503, false);
        assertEquals(NarrationPlaybackStateMachine.State.FALLBACK_PENDING, activity.narrationStateMachine.getState());

        String unheard = activity.narrationStateMachine.getUnheardRemainingText();
        assertFalse("Não deve incluir trecho 0", unheard.contains("Primeiro trecho"));
        assertTrue("Deve incluir trecho 1", unheard.contains("Segundo trecho"));
        assertTrue("Deve incluir trecho 2", unheard.contains("Terceiro trecho"));

        // Trigger fallback while nativeTts is not ready
        activity.waitForCharonQueueThenContinueNative(unheard, session, activity.activeAudioSession, System.currentTimeMillis() + 1000L);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Verify state is FALLBACK_PLAYING and session is preserved in activeFallbackSession
        assertEquals(NarrationPlaybackStateMachine.State.FALLBACK_PLAYING, activity.narrationStateMachine.getState());
        assertEquals(session, activity.activeFallbackSession);
        assertEquals(session, activity.charonNarrationGeneration);
        assertFalse("pendingRequestedNativeNarration deve guardar o texto pendente", activity.pendingRequestedNativeNarration.isEmpty());

        // 2. Now simulate Native TTS completing initialization (onInit SUCCESS)
        if (activity.nativeTts == null) {
            activity.nativeTts = new android.speech.tts.TextToSpeech(activity, null);
        }
        activity.nativeTtsReady = true;
        activity.nativeTtsLanguageReady = true;
        activity.handleNativeTtsInit(activity.nativeTtsInitGeneration, activity.nativeTtsRequestedEnginePackage, android.speech.tts.TextToSpeech.SUCCESS);

        // CRITICAL: verify that initialization resumed the original session WITHOUT cancelNarrationAudio!
        assertNotEquals("Máquina de estados NÃO pode ser cancelada pelo callback de inicialização",
                NarrationPlaybackStateMachine.State.CANCELLED, activity.narrationStateMachine.getState());
        assertEquals("Geração da sessão original deve ser preservada", session, activity.charonNarrationGeneration);
        assertEquals("activeFallbackSession deve manter a sessão original", session, activity.activeFallbackSession);

        // Exercise the listener actually registered on Android's TextToSpeech engine.
        // Calling finishNativeNarration() directly would incorrectly mark a silent
        // or interrupted playback as complete, hiding skipped utterances.
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNotNull("O listener Android TTS real deve estar registrado", activity.nativeUtteranceProgressListener);
        java.lang.reflect.Field queueField = MainActivity.class.getDeclaredField("nativeNarrationQueue");
        queueField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> voiceQueue = (List<String>)queueField.get(activity);
        if (!voiceQueue.isEmpty()) {
            String queuedText = String.join(" ", voiceQueue);
            assertFalse("Nenhum trecho ouvido pode ser repetido", queuedText.contains("Primeiro trecho ouvido"));
            assertTrue("O segundo trecho precisa estar na fila", queuedText.contains("Segundo trecho"));
            assertTrue("O terceiro trecho precisa estar na fila", queuedText.contains("Terceiro trecho"));
            completeWithNativeUtteranceCallbacks();
        } else {
            // Robolectric's TTS shadow can invoke all onDone callbacks itself
            // when idle() flushes the looper: this is a legitimate completion.
            assertEquals("Queue may be empty only after real onDone callbacks",
                    NarrationPlaybackStateMachine.State.COMPLETED,
                    activity.narrationStateMachine.getState());
        }
        String actuallySpoken = String.join(" ", activity.nativeVoiceCompletedUtterances);
        assertFalse("O bloco já ouvido em Charon não pode ser repetido", actuallySpoken.contains("Primeiro trecho ouvido"));
        assertTrue("O segundo bloco precisa ter passado pelo onDone", actuallySpoken.contains("Segundo trecho"));
        assertTrue("O terceiro bloco precisa ter passado pelo onDone", actuallySpoken.contains("Terceiro trecho"));
        assertEquals("Cada trecho enfileirado precisa de confirmação onDone",
                activity.nativeVoiceExpectedUtterances, activity.nativeVoiceCompletedUtterances.size());

        assertEquals("Máquina de estados deve concluir com sucesso",
                NarrationPlaybackStateMachine.State.COMPLETED, activity.narrationStateMachine.getState());
        assertEquals(-1L, activity.activeFallbackSession);
        assertEquals("Geração original mantida após conclusão", session, activity.charonNarrationGeneration);
        assertTrue("pendingRequestedNativeNarration deve ter sido consumido", activity.pendingRequestedNativeNarration.isEmpty());
    }

    private void completeWithNativeUtteranceCallbacks() throws Exception {
        if (activity.narrationStateMachine.getState() == NarrationPlaybackStateMachine.State.COMPLETED) return;
        if (!activity.nativeTtsReady || !activity.nativeTtsLanguageReady ||
                activity.nativeUtteranceProgressListener == null) {
            if (activity.nativeTts == null)
                activity.nativeTts = new android.speech.tts.TextToSpeech(activity, null);
            activity.nativeTtsReady = true;
            activity.nativeTtsLanguageReady = true;
            activity.handleNativeTtsInit(activity.nativeTtsInitGeneration,
                    activity.nativeTtsRequestedEnginePackage, android.speech.tts.TextToSpeech.SUCCESS);
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        if (activity.narrationStateMachine.getState() == NarrationPlaybackStateMachine.State.COMPLETED) return;
        assertNotNull("Native listener must be registered", activity.nativeUtteranceProgressListener);
        java.lang.reflect.Field queueField = MainActivity.class.getDeclaredField("nativeNarrationQueue");
        queueField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>)queueField.get(activity);
        assertFalse("Native queue must be available before its onDone callbacks", chunks.isEmpty());
        java.lang.reflect.Field idField = MainActivity.class.getDeclaredField("nativeNarrationSession");
        idField.setAccessible(true);
        long nativeId = idField.getLong(activity);
        int count = chunks.size();
        for (int i = 0; i < count; i++) {
            String id = "narracao_" + nativeId + "_" + i;
            activity.nativeUtteranceProgressListener.onStart(id);
            activity.nativeUtteranceProgressListener.onDone(id);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
        }
    }
}

