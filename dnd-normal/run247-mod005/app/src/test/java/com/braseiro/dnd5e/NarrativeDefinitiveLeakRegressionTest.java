package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes de regressão definitivos contra vazamento de metainstruções na narrativa.
 * Invoca os métodos produtores reais (OfflineMasterEngine, completePresentationWithBarbara, etc.)
 * e testa o firewall contra todas as frases vazadas das capturas.
 */
public class NarrativeDefinitiveLeakRegressionTest {

    private static final String[] LEAKED_PHRASES = {
            "O Braseiro não transforma essas relações em números invisíveis",
            "sem identidades inventadas apenas para encher a narrativa",
            "Esses sinais não são automaticamente pistas",
            "o estado persistente realmente o transformou em acontecimento",
            "Isso é deliberado: conhecimento de cenário será conquistado em jogo",
            "O Mestre local não move ninguém automaticamente",
            "Não existe uma missão artificialmente acrescentada",
            "O mundo não pausa enquanto essa decisão é tomada",
            "o Mestre local não inventa uma história secreta para preencher silêncio",
            "Nenhuma figura recebe importância artificial apenas para fabricar um guia do cenário",
            "se não houver, o Mestre não cria tais efeitos por decoração",
            "Nenhuma ameaça nova é acrescentada apenas para terminar a introdução com ação",
            "se existe um objetivo, ele permanece o que a campanha já registrou; a apresentação não fabrica um novo",
            "sem precisar receber um mapa técnico em voz alta",
            "Nenhum NPC novo é criado apenas para servir de guia",
            "não porque o Mestre decidiu recitar seu verbete",
            "Essa separação impede que uma descrição rica vire spoiler e preserva o valor da investigação",
            "Ele não inventa enchentes, rios ou passagens inexistentes",
            "sem acrescentar obstáculos decorativos",
            "O Mestre local evita códigos de mapa",
            "Nenhum rio, lago, ponte ou travessia é acrescentado quando a cartografia não os confirma",
            "em vez de convite para inventar uma emboscada",
            "sem precisar abrir um atlas externo",
            "O Mestre não inventa água onde não há curso confirmado e não transforma chuva comum em enchente por dramatização",
            "Pessoas distantes não mudam de plano só porque a companhia viu o tempo fechar aqui",
            "não uma suposição de que o jogador se lembra de um livro do cenário",
            "O Mestre não reescreve o lugar inteiro para parecer novo",
            "A apresentação não cria uma missão nova",
            "e a próxima ação não é decidida pelo Mestre",
            "O Mestre local preserva o que já está estabelecido e mantém desconhecido o que ainda não foi investigado",
            "O Mestre local só confirma a geometria já estabelecida",
            "o Mestre preserva o ponto da cena em vez de inventar uma resposta que altere o mundo",
            "pessoas, posições e tensões não são substituídas por uma cena genérica",
            "O cenário oferece detalhes observáveis em tempo real, sem decidir a próxima ação pelo grupo",
            "quando houver uma escolha significativa, ela continua pertencendo aos jogadores"
    };

    @Test
    public void testGuardFlagsEveryLiteralLeakedPhrase() {
        for (String phrase : LEAKED_PHRASES) {
            assertTrue("Deveria detectar vazamento na frase: " + phrase,
                    NarrativeGuardService.containsInternalLeak(phrase));
            NarrativeGuardService.ValidationResult v = NarrativeGuardService.validate(phrase);
            assertFalse("Validação deveria reprovar frase contaminada: " + phrase, v.valid);
        }
    }

    @Test
    public void testLegitimateInWorldFictionIsNotBlocked() {
        String[] legitimateTexts = {
                "O mestre ferreiro examina o aço da espada com atenção e entrega a lâmina para Theron.",
                "O grão-mestre da ordem acolhe os noviços no pátio do monastério.",
                "A missão recebida do lorde era escoltar a caravana até as portas da cidade.",
                "No centro da biblioteca, o mapa antigo de pergaminho mostrava os vales e as montanhas de Faerûn.",
                "O guerreiro estudou as regras do torneio cravadas na tábua de carvalho.",
                "As regras da guilda exigem discrição entre os aprendizes que circulam pelo mercado.",
                "Uma missão perigosa aguarda os aventureiros além das colinas sombrias."
        };
        for (String text : legitimateTexts) {
            assertFalse("Não deveria dar falso positivo em ficção legítima: " + text,
                    NarrativeGuardService.containsInternalLeak(text));
            NarrativeGuardService.ValidationResult v = NarrativeGuardService.validate(text);
            assertTrue("Ficção legítima deve ser aceita: " + text, v.valid);
        }
    }

    @Test
    public void testCompletePresentationWithBarbaraProducesNoLeakedPhrasesForAnyOccasion() throws Exception {
        OfflineMasterEngine engine = new OfflineMasterEngine();
        String[] occasions = {
                "campaign_opening",
                "first_arrival",
                "changed_return",
                "poi_discovery",
                "geography_discovery",
                "weather_transition"
        };

        JSONObject scene = new JSONObject();
        scene.put("location", "Biblioplex");
        scene.put("region", "Strixhaven");
        scene.put("geography", "jardins amplos e galerias de pedra arcana");
        scene.put("poi", "Átrio dos Fundadores");
        scene.put("weather_description", "manhã amena de primavera com vento leve");
        scene.put("danger_description", "vigilância redobrada nos corredores centrais");
        scene.put("objective", "encontrar a sala de orientação acadêmica");
        JSONArray npcs = new JSONArray();
        npcs.put(new JSONObject().put("name", "Brelga"));
        scene.put("present_npcs", npcs);

        for (String occ : occasions) {
            MasterOrchestrator.Plan plan = MasterOrchestrator.Plan.forTest("Entrar em Strixhaven", "{}", occ, "PLAYER_ACTION");
            String narration = OfflineMasterEngine.completePresentationWithBarbara("", occ, plan, scene);
            assertTrue("Apresentação em " + occ + " não pode ser vazia", narration.length() > 0);
            assertContainsNoLeakedPhrases("completePresentationWithBarbara(" + occ + ")", narration);
            assertFalse("NarrativeGuard não pode acusar vazamento em " + occ, NarrativeGuardService.containsInternalLeak(narration));
            assertTrue("WorldOnboardingNarrativeGuard deve aceitar a apresentação em " + occ,
                    WorldOnboardingNarrativeGuard.problem(narration, occ, null).isEmpty());
        }
    }

    @Test
    public void testOfflineMasterEngineNarrativeGeneratesNoLeakedPhrases() throws Exception {
        OfflineMasterEngine engine = new OfflineMasterEngine();

        JSONObject contextObj = new JSONObject();
        JSONObject bus = new JSONObject();
        JSONObject scene = new JSONObject();
        scene.put("location", "Portão de Baldur");
        scene.put("weather", "garoa fria");
        scene.put("danger", "patrulha dos Punhos Flamejantes desconfiada");
        scene.put("objective", "entrar na Cidade Baixa");
        bus.put("scene", scene);

        JSONArray heroes = new JSONArray();
        heroes.put(new JSONObject().put("name", "Kvothe").put("armor", "capa de viagem escura").put("equipment", "alaúde e espada"));
        bus.put("active_heroes_full", heroes);

        JSONArray npcs = new JSONArray();
        npcs.put(new JSONObject().put("name", "Wilem"));
        bus.put("npcs", npcs);
        contextObj.put("game_bus", bus);

        MasterOrchestrator.Plan plan = MasterOrchestrator.Plan.forTest("Avanço pelo portão", contextObj.toString(), "continuation", "PLAYER_ACTION");

        // Test normal action
        JSONObject result = engine.generate("Narrador", "Avanço pelo portão observando os guardas.", plan, new ArrayList<JSONObject>());
        String narration = result.optString("narration", "");
        assertTrue(narration.length() > 0);
        assertContainsNoLeakedPhrases("OfflineMaster.generate(Narrador)", narration);
        assertFalse(NarrativeGuardService.containsInternalLeak(narration));

        // Test clarification (SPATIAL_CLARIFICATION)
        JSONObject spatialResult = engine.generate("Narrador", "Onde fica a torre dos guardas em relação à entrada?", plan, new ArrayList<JSONObject>());
        String spatialNarration = spatialResult.optString("narration", "");
        assertTrue(spatialNarration.length() > 0);
        assertContainsNoLeakedPhrases("OfflineMaster.clarification(SPATIAL)", spatialNarration);
        assertFalse(NarrativeGuardService.containsInternalLeak(spatialNarration));

        // Test clarification (PLANNING)
        JSONObject planResult = engine.generate("Narrador", "Talvez seja melhor esperar a troca de guarda.", plan, new ArrayList<JSONObject>());
        String planNarration = planResult.optString("narration", "");
        assertTrue(planNarration.length() > 0);
        assertContainsNoLeakedPhrases("OfflineMaster.clarification(PLANNING)", planNarration);
        assertFalse(NarrativeGuardService.containsInternalLeak(planNarration));

        // Test opening presentation
        MasterOrchestrator.Plan openingPlan = MasterOrchestrator.Plan.forTest("INÍCIO AUTOMÁTICO", contextObj.toString(), "campaign_opening", "PLAYER_ACTION");
        JSONObject openingResult = engine.generate("Narrador", "INÍCIO AUTOMÁTICO", openingPlan, new ArrayList<JSONObject>());
        String openingNarration = openingResult.optString("narration", "");
        assertTrue(openingNarration.length() > 0);
        assertContainsNoLeakedPhrases("OfflineMaster.generate(campaign_opening)", openingNarration);
        assertFalse(NarrativeGuardService.containsInternalLeak(openingNarration));
    }

    @Test
    public void testSanitizerRemovesContaminatedParagraphsWithoutDestroyingCleanFiction() {
        String contaminated = "O vento sopra forte sobre os telhados da cidade alta.\n\n"
                + "O Braseiro não transforma essas relações em números invisíveis nem informa ao jogador fatos que os personagens não poderiam perceber.\n\n"
                + "Theron ajusta a alça do escudo e observa o movimento na praça central.";

        String sanitized = GeneratedTextSanitizer.narrative(contaminated);
        assertTrue("Deve preservar parágrafo limpo inicial", sanitized.contains("O vento sopra forte"));
        assertTrue("Deve preservar parágrafo limpo final", sanitized.contains("Theron ajusta a alça"));
        assertFalse("Deve remover comentário operacional do Braseiro", sanitized.contains("números invisíveis"));
        assertFalse("Deve remover citação ao Braseiro", sanitized.contains("Braseiro"));
        assertFalse(NarrativeGuardService.containsInternalLeak(sanitized));
    }

    private static void assertContainsNoLeakedPhrases(String context, String text) {
        String lower = text.toLowerCase();
        for (String phrase : LEAKED_PHRASES) {
            String pLower = phrase.toLowerCase();
            assertFalse("Vazamento encontrado em [" + context + "]: " + phrase + "\nTexto completo:\n" + text,
                    lower.contains(pLower));
        }
    }
}
