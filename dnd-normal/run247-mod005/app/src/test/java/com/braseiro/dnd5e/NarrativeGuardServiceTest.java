package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarrativeGuardServiceTest {

    @Test
    public void stripsUiLabelsAndHeadersFromProse() {
        String raw = "Local atual: Margem do Pântano\n"
                + "Clima atualizado: Chuva e neblina\n"
                + "Status: Em marcha\n"
                + "A água escorre fria pela grama alta enquanto a companhia avança.\n"
                + "Opções disponíveis:\n"
                + "Hex 1412: 5 km";
        String clean = NarrativeGuardService.sanitize(raw);
        assertFalse(clean.contains("Local atual"));
        assertFalse(clean.contains("Clima atualizado"));
        assertFalse(clean.contains("Status:"));
        assertFalse(clean.contains("Opções disponíveis"));
        assertTrue(clean.contains("A água escorre fria pela grama alta enquanto a companhia avança."));
    }

    @Test
    public void stripsSoftwareMetalanguageFromProse() {
        String raw = "A cena responde ao último gesto dos heróis. Theron firma o escudo de aço. Antes que qualquer explicação seja necessária, a porta da cripta estala.";
        String clean = NarrativeGuardService.sanitize(raw);
        assertFalse(clean.contains("A cena responde"));
        assertFalse(clean.contains("Antes que qualquer explicação"));
        assertTrue(clean.contains("Theron firma o escudo de aço."));
    }

    @Test
    public void stripsRepetitiveConversationalCrutchesAtEnd() {
        String raw = "Um rangido pesado estala sob as tábuas do assoalho e o cheiro doce de carniça invade a sala. O que você faz?";
        String clean = NarrativeGuardService.sanitize(raw);
        assertFalse(clean.contains("O que você faz"));
        assertTrue(clean.endsWith("invade a sala."));

        String raw2 = "A bruma cobre a estrada diante do grupo. O que vocês fazem?";
        String clean2 = NarrativeGuardService.sanitize(raw2);
        assertFalse(clean2.contains("O que vocês fazem"));
        assertTrue(clean2.endsWith("diante do grupo."));
    }

    @Test
    public void validatesAiVoiceAndEmotionHijack() {
        NarrativeGuardService.ValidationResult res1 = NarrativeGuardService.validate("Como uma IA, posso simular o clima da montanha.");
        assertFalse(res1.valid);
        assertTrue(res1.errors.get(0).contains("Voz de IA"));

        NarrativeGuardService.ValidationResult res2 = NarrativeGuardService.validate("Ao encarar a fera no breu, você sente medo e recua.");
        assertFalse(res2.valid);
        assertTrue(res2.errors.get(0).contains("Sequestro de emoção"));

        NarrativeGuardService.ValidationResult resClean = NarrativeGuardService.validate("O vento frio sopra sobre o capuz de Theron, e o estalo da madeira ressoa no corredor escuro.");
        assertTrue(resClean.valid);
        assertTrue(resClean.errors.isEmpty());
    }


    @Test
    public void blocksLeakedMasterInstructionsFromStrixhavenWithoutDestroyingTheScene() {
        String raw = "Os estudantes de Witherbloom cuidam das mudas raras no jardim.\n\n"
                + "O Mestre local não move ninguém automaticamente para o centro do conflito. "
                + "Ele estabelece o chão e as distâncias para a próxima ação.\n\n"
                + "Não existe uma missão artificialmente acrescentada só para dar direção a esta abertura. "
                + "Se o estado da campanha ainda não definiu um objetivo específico, a cena permanece honesta. "
                + "A companhia pode primeiro descobrir o que o lugar oferece. "
                + "Isso evita que o sistema invente uma urgência nos dados persistidos.\n\n"
                + "Um aprendiz atravessa o gramado carregando livros debaixo do braço.";
        assertTrue(NarrativeGuardService.containsInternalLeak(raw));
        String clean = NarrativeGuardService.sanitize(raw);
        assertTrue(clean.contains("Os estudantes de Witherbloom"));
        assertTrue(clean.contains("Um aprendiz atravessa"));
        assertFalse(clean.contains("Mestre local"));
        assertFalse(clean.contains("missão artificialmente"));
        assertFalse(clean.contains("dados persistidos"));
        String uiNarration = GeneratedTextSanitizer.narrative(raw);
        assertTrue(uiNarration.contains("Os estudantes de Witherbloom"));
        assertFalse(uiNarration.contains("Mestre local"));
        assertFalse(uiNarration.contains("dados persistidos"));
        assertFalse(NarrativeGuardService.validate(raw).valid);
        assertTrue(NarrativeGuardService.validate(clean).valid);
    }

    @Test
    public void doesNotMistakeOrdinaryInWorldMasterOrMissionTalkForMetadata() {
        String narration = "O mestre de Witherbloom espera ao lado da torre. "
                + "A missão dos estudantes é catalogar as mudas antes do anoitecer.";
        assertFalse(NarrativeGuardService.containsInternalLeak(narration));
        assertEquals(narration, NarrativeGuardService.sanitize(narration));
    }

    @Test
    public void dropsEntireAnswerWhenItIsOnlyInternalPolicyText() {
        String raw = "Se o estado da campanha ainda não definiu um objetivo específico, "
                + "a cena permanece honesta sobre isso.";
        assertTrue(NarrativeGuardService.containsInternalLeak(raw));
        assertEquals("", NarrativeGuardService.sanitize(raw));
    }

    @Test
    public void humanMasterNarrativeProfileEmbodiments() {
        JSONObject contract = HumanMasterNarrativeProfile.contract();
        assertTrue(contract.optBoolean("fiction_before_mechanics", false));
        assertTrue(contract.optBoolean("sensorial_framing", false));
        assertTrue(contract.optBoolean("hero_physical_anchoring", false));
        assertTrue(contract.optBoolean("continuous_world", false));
        assertTrue(contract.optBoolean("dynamic_zoom", false));
        assertTrue(contract.optBoolean("organic_agency", false));
        assertTrue(contract.optBoolean("zero_ai_metalanguage", false));

        String instruction = HumanMasterNarrativeProfile.systemInstruction();
        assertTrue(instruction.contains("ENQUADRAMENTO SENSORIAL, CLIMA E TERRENO (PILAR 1)"));
        assertTrue(instruction.contains("ANCORAGEM FÍSICA DOS HERÓIS (PILAR 2 - OBRIGATÓRIO)"));
        assertTrue(instruction.contains("O MUNDO EM MOVIMENTO CONTÍNUO (PILAR 3)"));
        assertTrue(instruction.contains("DINÂMICA DE CÂMERA - ZOOM CONSCIENTE (PILAR 4)"));
        assertTrue(instruction.contains("AGÊNCIA E DEVOLUÇÃO ORGÂNICA (PILAR 5 - ZERO MULETAS)"));
    }
}
