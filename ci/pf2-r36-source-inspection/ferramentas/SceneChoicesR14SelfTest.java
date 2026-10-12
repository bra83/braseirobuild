package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Regressão R14: escolhas Solo precisam ser numerosas o bastante e ancoradas na cena. */
public final class SceneChoicesR14SelfTest {
    private static int checks=0;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static boolean contains(JSONArray a,String needle){String n=needle.toLowerCase();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.toString().toLowerCase().contains(n))return true;}return false;}
    private static void grounded(JSONArray a,String narration,JSONObject scene){for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);check(o!=null,"opção nula");String action=o.optString("action",o.optString("description",o.optString("label","")));check(SceneOptionDiversifier.isGroundedInScene(action,narration,scene),"opção sem âncora: "+action);check(o.optString("label").length()>=8,"rótulo curto demais");check(o.optString("description").length()>=30,"descrição não explica método/risco suficiente");}}

    public static void main(String[] args)throws Exception{
        String tracks="Pegadas frescas cruzam a lama e seguem para o norte. Algumas marcas são profundas, outras leves, e a trilha desaparece por alguns metros sob raízes expostas.";
        JSONObject trackScene=new JSONObject().put("location","Clareira das raízes");
        JSONArray trackOptions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),tracks,"",trackScene,12);
        check(trackOptions.length()>=4&&trackOptions.length()<=6,"rastros devem gerar 4–6 escolhas");
        check(contains(trackOptions,"SEGUIR OS RASTROS"),"faltou seguir a pista diretamente");
        check(contains(trackOptions,"EXAMINAR OS RASTROS"),"faltou examinar antes de avançar");
        check(contains(trackOptions,"PARALELO")||contains(trackOptions,"SEGUNDO CONJUNTO"),"faltou abordagem lateral");
        check(contains(trackOptions,"INTERCEPTAÇÃO")||contains(trackOptions,"ROTA DE RETORNO"),"faltou decisão de posicionamento/segurança");
        check(!contains(trackOptions,"REAVALIAR POSIÇÃO E SAÍDAS"),"fallback vago reapareceu");
        grounded(trackOptions,tracks,trackScene);

        String debt="O Barão Varian Vance apresenta notas promissórias seladas e exige uma resposta antes do meio-dia.";
        JSONObject debtScene=new JSONObject().put("present_npcs",new JSONArray().put(new JSONObject().put("name","Barão Varian Vance")));
        JSONArray debtOptions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),debt,"",debtScene,13);
        check(debtOptions.length()>=4&&debtOptions.length()<=6,"cobrança deve gerar 4–6 escolhas");
        check(contains(debtOptions,"BARÃO VARIAN VANCE"),"NPC precisa aparecer nas escolhas sociais");
        check(contains(debtOptions,"NOTAS PROMISSÓRIAS")||contains(debtOptions,"PROVA"),"documento/prova precisa aparecer nas escolhas");
        check(contains(debtOptions,"CONDIÇÃO ALTERNATIVA")||contains(debtOptions,"RECUSAR"),"faltou decisão social com consequência");
        grounded(debtOptions,debt,debtScene);

        String threat="A sentinela de bronze ocupa a galeria e cobre a única aproximação visível ao cartógrafo preso.";
        JSONObject threatScene=new JSONObject().put("threats",new JSONArray().put("sentinela de bronze")).put("objective","resgatar o cartógrafo").put("location","galeria");
        JSONArray threatOptions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),threat,"",threatScene,14);
        check(threatOptions.length()>=4&&threatOptions.length()<=6,"ameaça+objetivo deve gerar 4–6 escolhas");
        check(contains(threatOptions,"SENTINELA DE BRONZE"),"ameaça precisa orientar as escolhas");
        grounded(threatOptions,threat,threatScene);


        String decree="O decreto de fundação da região de Nidal exige braços dispostos. Barbara de Vetro estende o documento e exige resposta antes do meio-dia. Dois guardas vigiam a entrada enquanto ela aguarda.";
        JSONObject decreeScene=new JSONObject().put("location","Nidal").put("present_npcs",new JSONArray()
                .put(new JSONObject().put("name","Barbara de Vetro").put("role","representante da Casa Vetro"))
                .put(new JSONObject().put("name","Guardas").put("group",true).put("anonymous",true).put("transient",true).put("count",2)));
        JSONArray decreeOptions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),decree,"",decreeScene,16);
        check(decreeOptions.length()>=4&&decreeOptions.length()<=6,"decreto com NPC e guardas deve gerar 4–6 decisões concretas");
        check(contains(decreeOptions,"BARBARA DE VETRO"),"Barbara precisa orientar a decisão social");
        check(contains(decreeOptions,"DECRETO")||contains(decreeOptions,"AUTORIDADE"),"o decreto/autoridade precisa aparecer nas escolhas");
        check(contains(decreeOptions,"GUARDAS")||contains(decreeOptions,"RECUSAR")||contains(decreeOptions,"CONDIÇÃO ALTERNATIVA"),"faltou reação lateral, recusa ou contraproposta");
        check(!contains(decreeOptions,"TESTAR A CONSEQUÊNCIA DA AÇÃO"),"ação genérica de testar consequência reapareceu");
        check(!contains(decreeOptions,"O QUE MUDOU"),"pergunta genérica sobre o que mudou reapareceu");
        grounded(decreeOptions,decree,decreeScene);

        JSONArray empty=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),"A luz muda lentamente.","",new JSONObject(),15);
        check(empty.length()==0,"sem âncora concreta é melhor manter ação livre do que inventar card genérico");

        System.out.println("OK: "+checks+" verificações R14 de escolhas contextuais 4–6.");
    }
}
