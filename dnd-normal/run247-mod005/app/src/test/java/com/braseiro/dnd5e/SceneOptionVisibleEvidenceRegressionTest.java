package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SceneOptionVisibleEvidenceRegressionTest {
    private static JSONObject option(String label,String action) throws Exception {
        return new JSONObject().put("label",label).put("action",action);
    }
    private static void doesNotMention(JSONArray options,String word){
        for(int i=0;i<options.length();i++){
            JSONObject item=options.optJSONObject(i);
            String shown=(item.optString("label","")+" "+item.optString("action","")).toLowerCase(java.util.Locale.ROOT);
            assertFalse("Hallucinated "+word+" in "+shown,shown.matches("(?s).*\\b"+word+"\\b.*"));
        }
    }
    @Test public void invisibleGuardMustNeverAppearEvenIfDatabaseSaysPresent() throws Exception {
        String narration="O corredor está silencioso. Uma auxiliar deixa o roteiro na mesa e aguarda a resposta de Kvothe.";
        JSONObject stale=new JSONObject().put("location","Biblioplex")
            .put("present_npcs_json","[{\"name\":\"o guarda\"}]");
        JSONArray raw=new JSONArray().put(option("ABORDAR O GUARDA",
            "Pergunto ao guarda se ele autoriza passar."))
            .put(option("OBSERVAR A AUXILIAR","Observo a auxiliar perto da mesa."));
        JSONArray result=SceneOptionDiversifier.ensureAlways(raw,new JSONArray(),narration,
            "Olho a sala",stale,12);
        assertEquals(5,result.length());
        doesNotMention(result,"guarda");
        assertFalse(SceneOptionVisibleEvidence.allSupported(raw,narration));
        assertTrue(SceneOptionVisibleEvidence.allSupported(result,narration));
    }
    @Test public void guardadosDoesNotMeanThereIsAGuard() throws Exception {
        String narration="Os adereços estão guardados no armário. Uma veterana comenta o ensaio.";
        JSONArray raw=new JSONArray().put(option("FALAR COM O GUARDA","Falo com o guarda sobre o armário."));
        JSONArray result=SceneOptionDiversifier.ensureAlways(raw,new JSONArray(),narration,"",new JSONObject(),4);
        assertEquals(5,result.length());
        doesNotMention(result,"guarda");
    }
    @Test public void deniedPresenceIsNotEvidenceOfPresence() throws Exception {
        String narration="A biblioteca está vazia, sem guardas. Os livros estão sobre a mesa.";
        assertFalse(SceneOptionVisibleEvidence.supported(
            option("CHAMAR OS GUARDAS","Chamo os guardas para falar."),narration));
    }
    @Test public void realVisibleGuardCanBeProposed() throws Exception {
        String narration="Dois guardas observam o saguão e aguardam instruções.";
        assertTrue(SceneOptionVisibleEvidence.supported(
            option("CONVERSAR COM OS GUARDAS","Pergunto aos guardas o motivo da espera."),narration));
    }
    @Test public void invisiblePropsAndCharactersInLabelsAreRejected() throws Exception {
        String narration="Uma funcionária anuncia a aula no pátio.";
        assertFalse(SceneOptionVisibleEvidence.supported(
            option("EXAMINAR O MAPA","Olho a funcionária."),narration));
        assertFalse(SceneOptionVisibleEvidence.supported(
            option("FALAR COM O PROFESSOR","Observo o pátio."),narration));
    }
}
