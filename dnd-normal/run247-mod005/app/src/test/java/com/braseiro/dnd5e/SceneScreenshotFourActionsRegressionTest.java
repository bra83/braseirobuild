package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Regression from an actual Galaxy A57 screenshot on 6.6.58: menu only showed 1. */
public class SceneScreenshotFourActionsRegressionTest {
    static final String LIVE_SCENE =
        "Urzmaktok Grojsh limpa o excesso de terra de uma folha com a ponta do indicador, "+
        "sem retirar a atenção do movimento do arbusto despertado. "+
        "O jovem orc ergue os olhos por um instante e fala em tom calmo, "+
        "batendo suavemente a pá de madeira contra o couro da bota:\n\n"+
        "Urzmaktok Grojsh: — As raízes acompanham a vibração dos passos. "+
        "Se você pretender conhecer o Clube de Horticultura Fantástica mais tarde, "+
        "a estufa fica aberta até o pôr do sol. "+
        "Mas agora as luzes do Hall dos Oráculos já estão pulsando no centro do saguão. "+
        "Daqui a pouco o primeiro grupo de novatos recebe as instruções da caça de orientação.\n\n"+
        "Ele se curva para ajustar uma pequena estaca de apoio junto ao caule da planta, "+
        "deixando o caminho em direção ao grande saguão circular livre.";

    @Test public void mustRenderFourDecisionsInActualOrcGardenerScene() throws Exception {
        JSONObject scene=new JSONObject().put("location","Strixhaven — Jardim")
            .put("objective","Acompanhar as instruções da caça de orientação");
        JSONArray actual=SceneOptionDiversifier.ensureAlways(
            new JSONArray(),new JSONArray(),LIVE_SCENE,"",scene,35);
        assertEquals("ACTUAL SCREENSHOT REGRESSION: "+actual,4,actual.length());
        assertTrue(StoryMomentumAction.isRequest(actual.getJSONObject(0).getString("action")));
        Set<String> types=new HashSet<>();
        boolean npc=false,plant=false,path=false;
        for(int i=1;i<actual.length();i++){
            JSONObject o=actual.getJSONObject(i);
            String action=o.getString("action");
            assertTrue("Visible evidence: "+o,SceneOptionVisibleEvidence.supported(o,LIVE_SCENE));
            assertTrue("Ticket/real click: "+o,SceneActionEvidence.ticketMatches(o,LIVE_SCENE));
            assertTrue("No duplicate category: "+o,types.add(o.optString("action_type")));
            String flat=action.toLowerCase(java.util.Locale.ROOT);
            npc|=flat.contains("urzmaktok")||flat.contains("orc");
            plant|=flat.contains("arbusto")||flat.contains("planta")||flat.contains("raiz");
            path|=flat.contains("saguão")||flat.contains("hall")||flat.contains("orientação");
            assertFalse("No fabricated guard: "+action,flat.contains("guarda"));
            assertFalse("No assumed test success: "+action,flat.contains("tive sucesso"));
        }
        assertTrue("Need observable vegetation: "+actual,plant);
        assertTrue("Need in-scene conversation: "+actual,npc);
        assertTrue("Need forward opportunity: "+actual,path);
    }

    @Test public void noAdjacentActorShallBeStolenFromOldScene() throws Exception {
        String scene="O pátio está silencioso. Uma pequena estaca de apoio está ao lado da planta. "+
            "O caminho para o saguão está livre, mas não há oficial nem guarda aqui.";
        JSONArray actual=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            scene,"",new JSONObject().put("present_npcs_json",
                "[{\\\"name\\\":\\\"Oficial do portão\\\"}]"),36);
        for(int i=0;i<actual.length();i++){
            String action=actual.getJSONObject(i).optString("action","").toLowerCase();
            assertFalse(action,action.contains("oficial"));
            assertFalse(action,action.contains("guarda"));
        }
    }
}
