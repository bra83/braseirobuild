package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** User's actual 09:45 Strixhaven scene: a revealed destination is not a waiting prompt. */
public final class RevealedClueActionGroundingRegressionTest {
    private static final String CLUE =
        "Ao aproximar a folha da luz e alinhar os traços de tinta com a textura áspera do pergaminho, "
      + "o padrão oculto se revela com clareza. As ranhuras cinzentas formam um diagrama em miniatura "
      + "da nave central do Hall dos Oráculos. As letras maiúsculas de cada verso formam as coordenadas "
      + "de uma rosa dos ventos estilizada, apontando a interseção exata entre a entrada do Café do "
      + "Biblioplex e o mural de raízes que adorna a parede oeste. Abaixo dessa marcação, uma sequência "
      + "micrográfica gravada no selo de cera indica que o próximo elemento da caça de boas-vindas "
      + "exige a identificação de um ingrediente específico usado no preparo do prato diário da cantina. "
      + "Nenhum dos calouros parece ter notado o detalhe impresso na margem inferior. "
      + "A pista está decifrada, e o caminho para o próximo nó da orientação se abre.";

    @Test public void revealedLeadSupersedesGenericOrientationAndInventedDiary() throws Exception {
        JSONArray offered=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            CLUE,"Observei o pergaminho",new JSONObject().put("location","Hall dos Oráculos"),35);
        assertEquals(offered.toString(),4,offered.length());
        JSONObject momentum=offered.getJSONObject(0);
        assertTrue(StoryMomentumAction.isRequest(momentum.getString("action")));
        assertTrue(momentum.getString("label"),momentum.getString("label").contains("MURAL"));
        assertTrue(momentum.getString("action"),momentum.getString("action").contains("Café do Biblioplex"));
        assertTrue(momentum.getString("action"),momentum.getString("action").contains("mural de raízes"));
        assertFalse(momentum.getString("label").contains("AGUARDAR"));
        assertEquals(0d,SceneActionClock.minutesFor(momentum.getString("action")),0.0001);
        assertTrue(StoryMomentumAction.instruction(true,momentum.getString("action"))
            .contains("NÃO pressupor que ela foi encontrada"));

        String[] types={"OBSERVAR","AGIR","DESPISTAR"};
        String[] anchors={"MICROGRAFIA","CALOUROS","COORDENADAS"};
        for(int i=1;i<4;i++){
            JSONObject option=offered.getJSONObject(i);
            assertEquals(types[i-1],option.getString("action_type"));
            assertTrue(option.getString("label"),option.getString("label").contains(anchors[i-1]));
            assertTrue(SceneOptionVisibleEvidence.supported(option,CLUE));
            assertFalse(option.getString("action").contains("próxima instrução"));
        }
        assertFalse("A daily meal is not a notebook: "+offered,
            offered.toString().toLowerCase().contains("diário"));
        assertEquals(offered.toString(),StoryMomentumAction.ensureVisible(offered,CLUE).toString());
    }

    @Test public void diaryAdjectiveIsNotEvidenceOfAPhysicalJournal() throws Exception {
        assertFalse(SceneOptionVisibleEvidence.diaryNounPresent(
            "A cantina oferece um prato diário com ingredientes da horta."));
        assertTrue(SceneOptionVisibleEvidence.diaryNounPresent(
            "Sobre a mesa há um diário antigo de viagem."));
        JSONObject fake=new JSONObject().put("label","OBSERVAR O DIÁRIO")
            .put("action","Examino o diário para encontrar a pista.");
        assertFalse(SceneOptionVisibleEvidence.supported(fake,CLUE));
    }

    @Test public void doNotFollowInvisibleDestinationOrResolvePuzzleByClick() throws Exception {
        String withoutTarget="A pista está decifrada. O texto fala de um ingrediente "
            +"presente no prato diário, mas não informa onde procurar.";
        assertFalse(SceneDiscoveredLead.available(withoutTarget));
        assertEquals(StoryMomentumAction.ACTION,StoryMomentumAction.option(withoutTarget).optString("action"));
        assertFalse(StoryMomentumAction.isRequest("Falo com os calouros."));
        JSONArray result=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            withoutTarget,"",new JSONObject(),13);
        assertFalse(result.toString(),result.toString().contains("IR AO PONTO INDICADO JUNTO AO MURAL"));
    }
}
