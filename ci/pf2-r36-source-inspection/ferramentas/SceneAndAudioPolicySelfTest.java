package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Regressões de sugestões persistentes e da política de espera do Charon. */
public final class SceneAndAudioPolicySelfTest {
    private static int checks=0;

    public static void main(String[] args)throws Exception{
        JSONObject scene=new JSONObject()
                .put("location","Salão do Relógio")
                .put("present_npcs_json",new JSONArray().put(new JSONObject().put("name","Iria" )).toString())
                .put("clues_json",new JSONArray().put("engrenagem marcada").toString())
                .put("threats_json",new JSONArray().put("sentinela de bronze").toString());
        String narration="A sentinela se cala. Iria aponta para uma engrenagem marcada atrás do relógio.";

        JSONArray generated=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),narration,"",scene,8);
        check(generated.length()>=4&&generated.length()<=6,"uma cena narrativa rica recebe quatro a seis escolhas mesmo sem opções remotas");

        JSONArray history=SceneOptionDiversifier.appendHistory(new JSONArray(),generated);
        JSONArray repeatedSource=new JSONArray();
        for(int i=0;i<generated.length();i++)repeatedSource.put(generated.optJSONObject(i));
        JSONArray next=SceneOptionDiversifier.diversify(repeatedSource,history,
                "A engrenagem gira e revela uma passagem estreita.","",scene,9);
        check(next.length()>=3&&next.length()<=6,"o histórico não faz o painel de escolhas desaparecer nem o enche de paráfrases");
        check(!SceneOptionDiversifier.setsAreEquivalent(generated,next),"a cena seguinte não repete imediatamente o mesmo conjunto");

        check(history.toString().startsWith("[")&&history.toString().endsWith("]"),
                "o histórico é serializado como o array usado por estado e backup");
        check(SceneOptionDiversifier.diversify(new JSONArray(),history,"","",new JSONObject(),10).length()==0,
                "sem cena válida não são inventadas sugestões");

        check(NarrationPlaybackPolicy.CHARON_WAIT_STATUS_MS<NarrationPlaybackPolicy.CHARON_LONG_WAIT_STATUS_MS,
                "avisos do Charon são somente marcos progressivos");
        check(NarrationPlaybackPolicy.CHARON_LONG_WAIT_STATUS_MS<NarrationPlaybackPolicy.CHARON_READ_TIMEOUT_MS,
                "nenhum aviso de espera antecipa o timeout real");
        check(NarrationPlaybackPolicy.CHARON_READ_TIMEOUT_MS>=60_000,
                "Charon mantém pelo menos sessenta segundos de leitura");
        String prompt=NarrationPlaybackPolicy.charonPrompt("A porta se abre.");
        check(prompt.contains("TRANSCRIÇÃO")&&prompt.endsWith("A porta se abre."),
                "direção de voz e transcrição permanecem separadas");

        System.out.println("OK: "+checks+" cenários de sugestões e política Charon.");
    }

    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
