package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * User acceptance contract: no playable scene can produce fewer than four
 * distinct contextual actions. The optional initiative is the fifth entry.
 */
public final class FourSceneActionsDropdownRegressionTest {
    private static final String[] SCENES = {
        "A luz atravessa o Biblioplex. Calouros se reúnem perto dos balcões de atendimento, enquanto veteranos leem folhetos sobre atividades estudantis.",
        "Uma assistente de triagem convoca os calouros para a orientação e confere os nomes numa prancheta.",
        "A bibliotecária fecha um livro e diz que conhece o caminho até a sala das apresentações.",
        "Dois estudantes ensaiam falas perto do palco. Uma veterana segura um mapa da galeria.",
        "As portas da taverna rangem quando o mercador se aproxima do balcão.",
        "Os guardas observam a ponte enquanto uma caravana se aproxima.",
        "O vento sopra sobre a estrada.",
        "Uma breve pausa.",
        "Uma auxiliar anuncia o começo da orientação.",
        "A chuva cai.",
        "O pergaminho lacrado está sobre uma mesa, diante da escadaria.",
        "O grupo aguarda."
    };

    @Test public void minimumFourDistinctActionsAcrossQuietSocialAndExplorationScenes() throws Exception {
        for (int turn=0;turn<SCENES.length;turn++) {
            String narration=SCENES[turn];
            JSONObject scene=new JSONObject();
            JSONArray result=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
                narration,"Olho ao redor",scene,turn+1);
            assertEquals("Four contextual choices plus one momentum in scene "+turn+" : "+result,
                    5,result.length());
            assertTrue("Initiative is always explicit",StoryMomentumAction.isRequest(result.getJSONObject(0).getString("action")));
            Set<String> distinct = new HashSet<>();
            for(int i=1;i<result.length();i++) {
                JSONObject choice=result.getJSONObject(i);
                String label=choice.optString("label"),action=choice.optString("action");
                assertFalse("No empty action",action.trim().isEmpty());
                assertFalse("No generic label",label.matches("(?iu)^(agir|continuar|observar|investigar)$"));
                assertTrue("Action must reference narrated beat: "+action+" / "+narration,
                    SceneOptionDiversifier.isRelevantToCurrentBeat(action,narration));
                assertTrue("Action must be grounded in the presented scene: "+action+" / "+narration,
                    SceneOptionDiversifier.isGroundedInScene(action,narration,scene));
                assertTrue("Each turn has different options",distinct.add(action.toLowerCase(java.util.Locale.ROOT)));
                assertFalse("No autoplay embedded in contextual action",StoryMomentumAction.isRequest(action));
            }
        }
    }

    @Test public void excessiveOrDuplicateModelSuggestionsAreClampedAndFilled() throws Exception {
        String sceneText="Os estudantes conversam junto ao painel de avisos da biblioteca.";
        JSONArray raw=new JSONArray();
        for(int i=0;i<8;i++)raw.put(new JSONObject()
            .put("label","CONVERSAR COM OS ESTUDANTES")
            .put("action","Pergunto aos estudantes sobre o painel de avisos."));
        JSONArray result=SceneOptionDiversifier.ensureAlways(raw,new JSONArray(),sceneText,
            "Entro na biblioteca",new JSONObject(),2);
        assertEquals(5,result.length());
        assertTrue(StoryMomentumAction.isRequest(result.getJSONObject(0).getString("action")));
    }

    @Test public void inWorldMomentumIsUniqueAndDoesNotActForKvothe() {
        String narration="Uma bibliotecária se aproxima de Kvothe para falar sobre as aulas.";
        JSONArray a=StoryMomentumAction.ensureVisible(new JSONArray(),narration);
        JSONArray b=StoryMomentumAction.ensureVisible(a,narration);
        assertEquals(1,a.length());
        assertEquals(1,b.length());
        assertEquals("ESCUTAR A BIBLIOTECÁRIA",b.optJSONObject(0).optString("label"));
        assertEquals(0d,SceneActionClock.minutesFor(StoryMomentumAction.ACTION),0.0001);
        assertFalse(StoryMomentumAction.instruction(true).contains("teletransportá-lo para"));
    }

    @Test public void noOptionsBeforeNarrationIsPresent() {
        JSONArray empty=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),"","",new JSONObject(),0);
        assertEquals(0,empty.length());
    }

    @Test public void pinpointTwoRealReportedEdgeScenes() throws Exception {
        String[] texts={
            "O roteiro encadernado em couro azul traz anotações nas margens sobre um duelo de réplicas e uma nota dobrada indicando que os adereços estão guardados no armário ao lado da escada.",
            "A Guilda de RPG Live-Action prepara armadilhas de papelão para uma encenação. Uma funcionária orienta inscrições dos recém-chegados."
        };
        for(String narration:texts){
            java.lang.reflect.Method extract=ContextualSceneActionFactory.class.getDeclaredMethod("sparseAnchor",String.class);
            extract.setAccessible(true);
            String anchor=(String)extract.invoke(null,narration);
            String sample="Permaneço atento a "+anchor+" e procuro perceber o que muda, sem antecipar um resultado.";
            JSONObject scene=new JSONObject().put("location","Strixhaven • Biblioplex").put("objective","Investigar os acontecimentos em Strixhaven • Biblioplex");
            JSONArray raw=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),narration,"Olho ao redor",scene,1);
            JSONArray filled=ContextualSceneActionFactory.complete(raw,narration,scene,1);
            assertEquals("FOCUS="+anchor+" sampleGrounded="+SceneOptionDiversifier.isGroundedInScene(sample,narration,scene)
                +" sampleCurrent="+SceneOptionDiversifier.isRelevantToCurrentBeat(sample,narration)
                +" raw="+raw+" filled="+filled,4,filled.length());
        }
    }
}
