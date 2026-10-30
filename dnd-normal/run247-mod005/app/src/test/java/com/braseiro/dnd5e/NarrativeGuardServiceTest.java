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
    @Test
    public void blocksObservedStrixhavenMetaInstructionsWithoutLosingRealScene() {
        String good="O piso do Biblioplex geme sob as botas de Kvothe. Uma pena risca papel do outro lado da sala.";
        String leaked="Quando um novo reino, templo, povo, organização, divindade ou conflito aparecer, ele deverá ganhar contexto no momento em que importar, em vez de ser despejado como uma sequência de nomes. O presente imediato, então, ganha prioridade sobre qualquer enciclopédia. A companhia pode interromper uma rotina. O mundo não pausa enquanto essa decisão é tomada. É nesse ponto que a apresentação termina: não porque o lugar tenha sido explicado por completo, mas porque já existe contexto suficiente para agir sem fingir conhecimento que ninguém recebeu.";
        assertTrue(NarrativeGuardService.containsInstructionLeak(leaked));
        assertEquals(good, NarrativeGuardService.sanitize(good+"\n\n"+leaked));
        assertEquals("", NarrativeGuardService.sanitize(leaked));
        assertFalse(NarrativeGuardService.validate(leaked).valid);
        assertEquals(good, GeneratedTextSanitizer.humanText(good+"\n\n"+leaked));
    }

    @Test
    public void keepsRealDialogueAndSceneEvenWhenGameVocabularyIsUsed() {
        String scene="Kvothe observa os alunos que cruzam o pátio. \""
            +"O mestre de História deve estar na biblioteca\", comenta Wilem. "
            +"Auri segue seu próprio caminho até o jardim.";
        String clean=NarrativeGuardService.sanitize(scene);
        assertEquals(scene, clean);
        assertFalse(NarrativeGuardService.containsInstructionLeak(scene));
    }

}
