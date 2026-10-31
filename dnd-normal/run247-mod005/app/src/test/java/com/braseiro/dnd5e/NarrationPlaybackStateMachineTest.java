package com.braseiro.dnd5e;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarrationPlaybackStateMachineTest {

    @Test
    public void testFullPlaybackCycleCharon() {
        NarrationPlaybackStateMachine sm = new NarrationPlaybackStateMachine();
        List<String> chunks = Arrays.asList(
                "O sol se ergue sobre as torres do Biblioplex.",
                "Estudantes caminham apressados entre os canteiros arcanos.",
                "Uma brisa suave agita as capas de viagem."
        );

        sm.startSession(101L, 1L, chunks, true, NarrationPlaybackStateMachine.VoiceSource.CHARON);
        assertEquals(NarrationPlaybackStateMachine.State.PREPARING, sm.getState());
        assertEquals(NarrationPlaybackStateMachine.VoiceSource.CHARON, sm.getActiveVoiceSource());
        assertEquals(3, sm.getTotalChunks());
        assertEquals(0, sm.getFirstUnheardChunkIndex());

        // Chunk 0 synthesis requested and completed
        sm.onSynthesisRequested(0, 1L);
        assertEquals(NarrationPlaybackStateMachine.State.SYNTHESIZING, sm.getState());

        sm.onChunkSynthesized(0, 1L);
        assertEquals(NarrationPlaybackStateMachine.State.CHUNK_READY, sm.getState());

        // Chunk 0 playback starts
        sm.onChunkPlaybackStarted(0, 1L);
        assertEquals(NarrationPlaybackStateMachine.State.PLAYING, sm.getState());
        assertEquals(0, sm.getStartedPlaybackIndex());
        assertEquals(-1, sm.getCompletedPlaybackIndex());

        // Chunk 1 synthesized in parallel
        sm.onSynthesisRequested(1, 1L);
        sm.onChunkSynthesized(1, 1L);

        // Chunk 0 playback completes
        sm.onChunkPlaybackCompleted(0, 1L);
        assertEquals(0, sm.getCompletedPlaybackIndex());
        assertEquals(1, sm.getFirstUnheardChunkIndex());

        // Chunk 1 playback starts and completes
        sm.onChunkPlaybackStarted(1, 1L);
        sm.onChunkPlaybackCompleted(1, 1L);
        assertEquals(1, sm.getCompletedPlaybackIndex());
        assertEquals(2, sm.getFirstUnheardChunkIndex());

        // Chunk 2 playback starts and completes
        sm.onChunkPlaybackStarted(2, 1L);
        sm.onChunkPlaybackCompleted(2, 1L);

        assertEquals(NarrationPlaybackStateMachine.State.COMPLETED, sm.getState());
        assertEquals(NarrationPlaybackStateMachine.VoiceSource.NONE, sm.getActiveVoiceSource());
    }

    @Test
    public void testFallbackResumesFromFirstUnheardChunkWithoutRestartingOrSkipping() {
        NarrationPlaybackStateMachine sm = new NarrationPlaybackStateMachine();
        List<String> chunks = Arrays.asList(
                "Bloco zero já foi ouvido pelo jogador.",
                "Bloco um também terminou de tocar.",
                "Bloco dois falhou ao sintetizar no Charon.",
                "Bloco três deve ser lido pelo motor nativo."
        );

        sm.startSession(202L, 2L, chunks, true, NarrationPlaybackStateMachine.VoiceSource.CHARON);

        // Chunks 0 and 1 play completely
        sm.onChunkPlaybackStarted(0, 2L);
        sm.onChunkPlaybackCompleted(0, 2L);
        sm.onChunkPlaybackStarted(1, 2L);
        sm.onChunkPlaybackCompleted(1, 2L);

        // Chunk 2 fails synthesis with 429 quota
        sm.onSynthesisFailed(2, 2L, "HTTP 429 Too Many Requests", 429, true);
        assertEquals(NarrationPlaybackStateMachine.State.FALLBACK_PENDING, sm.getState());
        assertTrue(sm.isQuotaError());

        // The first unheard chunk must be index 2!
        assertEquals(2, sm.getFirstUnheardChunkIndex());
        List<String> unheard = sm.getUnheardChunks();
        assertEquals(2, unheard.size());
        assertEquals("Bloco dois falhou ao sintetizar no Charon.", unheard.get(0));
        assertEquals("Bloco três deve ser lido pelo motor nativo.", unheard.get(1));

        // Start fallback to Kokoro / Android TTS
        sm.onFallbackStarted(NarrationPlaybackStateMachine.VoiceSource.KOKORO, 2L);
        assertEquals(NarrationPlaybackStateMachine.State.FALLBACK_PLAYING, sm.getState());
        assertEquals(NarrationPlaybackStateMachine.VoiceSource.KOKORO, sm.getActiveVoiceSource());
        assertEquals("Braseiro Kokoro PT-BR", sm.getActiveVoiceLabel());
    }

    @Test
    public void testCancelImmediatelyStopsSession() {
        NarrationPlaybackStateMachine sm = new NarrationPlaybackStateMachine();
        List<String> chunks = Arrays.asList("A", "B", "C");
        sm.startSession(303L, 3L, chunks, true, NarrationPlaybackStateMachine.VoiceSource.CHARON);
        sm.onChunkPlaybackStarted(0, 3L);

        sm.onCancel();
        assertEquals(NarrationPlaybackStateMachine.State.CANCELLED, sm.getState());
        assertEquals(NarrationPlaybackStateMachine.VoiceSource.NONE, sm.getActiveVoiceSource());
        assertTrue(sm.isTerminal());

        // Callbacks from previous session are ignored after cancel
        sm.onChunkPlaybackCompleted(0, 3L);
        assertEquals(NarrationPlaybackStateMachine.State.CANCELLED, sm.getState());
    }
}
