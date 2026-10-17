package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

public class AudioStatusGateTest {
    @Test public void routineSoundDiagnosticsMustNotFloodFooter() {
        AudioStatusGate gate = new AudioStatusGate();
        assertFalse(gate.allow("SFX: Portão enferrujado", 10L));
        assertFalse(gate.allow("SFX não encontrado para vento.", 10L));
        assertFalse(gate.allow("Áudio preparado pelo PLAY: floresta", 10L));
        assertFalse(gate.allow("Ambiência alternada para: floresta", 10L));
        assertFalse(gate.allow("Clima pronto.", 10L));
    }

    @Test public void failuresRemainVisibleButDoNotRepeatEverySceneTick() {
        AudioStatusGate gate = new AudioStatusGate();
        assertTrue(gate.allow("Falha ao tocar clima.", 100L));
        assertFalse(gate.allow("Falha ao tocar clima.", 250L));
        assertFalse(gate.allow("Falha ao tocar clima.", 20000L));
        assertTrue(gate.allow("Falha ao tocar clima.", 20100L));
        assertTrue(gate.allow("Erro na voz: inicialização", 20101L));
        assertFalse(gate.allow("Erro na voz: inicialização", 20102L));
    }

    @Test public void differentRelevantMessagesCanStillBeShown() {
        AudioStatusGate gate = new AudioStatusGate();
        assertTrue(gate.allow("Narração concluída.", 100L));
        assertFalse(gate.allow("Narração concluída.", 105L));
        assertTrue(gate.allow("Fone ou Bluetooth removido; o áudio foi pausado.", 200L));
        assertFalse(gate.allow("  ", 300L));
    }
    @Test public void onlyAudioStatusesAreThrottledOnGameFooter() {
        assertTrue(AudioStatusGate.isAudioStatus("Braseiro Kokoro PT-BR narrando • áudio inteligente sincronizado."));
        assertTrue(AudioStatusGate.isAudioStatus("Voz do Android iniciando."));
        assertTrue(AudioStatusGate.isAudioStatus("Narração concluída."));
        assertTrue(AudioStatusGate.isAudioStatus("SFX não encontrado para vento."));
        assertFalse("Dice rolls are never filtered", AudioStatusGate.isAudioStatus("D20: 19"));
        assertFalse("Game actions are never filtered", AudioStatusGate.isAudioStatus("O orc acertou o herói."));
    }

}
