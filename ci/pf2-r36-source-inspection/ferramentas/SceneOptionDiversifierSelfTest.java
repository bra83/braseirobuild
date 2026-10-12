package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SceneOptionDiversifierSelfTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static JSONObject option(String action)throws Exception{return new JSONObject().put("label",action).put("description",action).put("action",action);}
    private static boolean contains(JSONArray values,String needle){String n=needle.toLowerCase();for(int i=0;i<values.length();i++){JSONObject value=values.optJSONObject(i);if(value!=null&&value.toString().toLowerCase().contains(n))return true;}return false;}

    public static void main(String[] args)throws Exception{
        JSONArray history=new JSONArray().put("Examinar as pegadas recentes").put("Questionar Mira sobre a chave");
        JSONArray same=new JSONArray().put(option("Examinar as pegadas recentes")).put(option("Questionar Mira sobre a chave"));
        JSONArray deduped=SceneOptionDiversifier.appendHistory(history,same);
        check(deduped.length()==2,"o mesmo par de opções não pode ser duplicado no histórico");

        JSONObject clueScene=new JSONObject().put("clues",new JSONArray().put(new JSONObject().put("description","chave de bronze partida")));
        JSONArray clueOptions=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),"A fechadura foi mexida há pouco.","",clueScene,1);
        check(clueOptions.length()>=4&&clueOptions.length()<=6,"uma cena rica em pista precisa receber quatro a seis escolhas úteis");
        check(contains(clueOptions,"chave de bronze partida"),"a pista recém-projetada precisa aparecer nas opções");

        JSONObject threatScene=new JSONObject().put("threats",new JSONArray().put("sentinela na galeria")).put("objective","resgatar o cartógrafo");
        JSONArray threatOptions=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),"A galeria permanece ocupada.","",threatScene,2);
        check(contains(threatOptions,"sentinela na galeria")||contains(threatOptions,"resgatar o cartógrafo"),"ameaça ou objetivo recém-projetado deve orientar a escolha");

        JSONObject npcScene=new JSONObject().put("present_npcs",new JSONArray().put(new JSONObject().put("name","Mira Valecinza")));
        JSONArray npcOptions=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),"Mira hesita diante do grupo.","",npcScene,3);
        check(contains(npcOptions,"Mira Valecinza"),"NPC recém-projetado deve orientar a escolha");

        JSONArray noContext=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),"A luz azul se apaga quando o sino para de vibrar.","",new JSONObject(),4);
        check(noContext.length()==0,"sem entidade ou objetivo concreto o app deve preservar a ação livre em vez de inventar opções genéricas");

        String debtNarration="Entrego suas notas promissórias antes do meio-dia — declarou o Barão Varian Vance, pressionando o anel de selo contra o lacre. A exigência era entregar o documento original da mina ou encarar a dívida dobrada.";
        JSONObject debtScene=new JSONObject().put("present_npcs",new JSONArray().put(new JSONObject().put("name","Barão Varian Vance").put("role","Barão")));
        JSONArray debtOptions=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),debtNarration,"",debtScene,5);
        check(debtOptions.length()>=4&&debtOptions.length()<=6,"a cobrança com NPC e documento deve produzir quatro a seis escolhas específicas");
        check(contains(debtOptions,"Barão Varian Vance"),"a escolha deve nomear o NPC da cobrança");
        check(contains(debtOptions,"notas promissórias")||contains(debtOptions,"documento"),"a escolha deve tratar o documento da cena");
        check(!contains(debtOptions,"travessia"),"a palavra rio dentro de documento original não pode criar uma travessia inexistente");
        check(!SceneOptionDiversifier.isCompatibleWithScene("Verificar a travessia",debtNarration,debtScene),"uma opção de travessia sem rio ou ponte deve ser rejeitada");



        String tracksNarration="Pegadas frescas cruzam a lama e seguem para o norte. Algumas marcas são profundas, outras leves, e a trilha desaparece por alguns metros sob raízes expostas.";
        JSONObject tracksScene=new JSONObject().put("location","Clareira das raízes");
        JSONArray trackOptions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),tracksNarration,"",tracksScene,7);
        check(trackOptions.length()>=4&&trackOptions.length()<=6,"rastros precisam gerar quatro a seis decisões concretas");
        check(contains(trackOptions,"seguir os rastros"),"rastros devem oferecer avanço direto");
        check(contains(trackOptions,"examinar os rastros"),"rastros devem oferecer análise antes do avanço");
        check(contains(trackOptions,"paralelo")||contains(trackOptions,"segundo conjunto"),"rastros devem oferecer uma abordagem lateral distinta");
        check(!contains(trackOptions,"reavaliar posição e saídas"),"fallback vago não pode reaparecer quando a cena tem rastros concretos");

                String malformedNpc="{NAME:Feitor Kaelen Vane,VISUAL_DESCRIPTION:homem de sobretudo escuro,ROLE:feitor}";
        check("Feitor Kaelen Vane".equals(GeneratedTextSanitizer.scalar(malformedNpc,"name")),
                "o sanitizador deve extrair NAME de objeto pseudo-JSON com chave em maiúsculas");
        String leakedLabel="NEGOCIAR A EXIGÊNCIA COM "+malformedNpc;
        String humanLabel=GeneratedTextSanitizer.humanText(leakedLabel);
        check(humanLabel.contains("Feitor Kaelen Vane"),"o rótulo deve preservar somente o nome humano do NPC");
        check(!humanLabel.contains("VISUAL_DESCRIPTION")&&!humanLabel.contains("{NAME:"),
                "o payload estruturado do NPC não pode aparecer na interface");
        String truncatedHuman=GeneratedTextSanitizer.humanText("NEGOCIAR COM {NAME:Feitor Kaelen Vane,VISUAL_DESCRIPTION:homem alto...");
        check(truncatedHuman.contains("Feitor Kaelen Vane")&&!truncatedHuman.contains("VISUAL_DESCRIPTION"),
                "payload estruturado truncado também deve virar apenas o nome do NPC");

        JSONObject malformedScene=new JSONObject().put("present_npcs",new JSONArray().put(malformedNpc));
        JSONArray malformedSource=new JSONArray().put(new JSONObject()
                .put("label",leakedLabel)
                .put("description","Questionar prazo, autoridade e contrapartida.")
                .put("action","Negociar a exigência com "+malformedNpc+" antes de aceitar o contrato."));
        JSONArray sanitizedNpcOptions=SceneOptionDiversifier.diversify(malformedSource,new JSONArray(),
                "O feitor apresenta um contrato e exige resposta imediata.","",malformedScene,6);
        check(sanitizedNpcOptions.length()>0,"uma opção válida com NPC estruturado deve sobreviver à sanitização");
        check(contains(sanitizedNpcOptions,"Feitor Kaelen Vane"),"a sugestão deve nomear Feitor Kaelen Vane");
        check(!contains(sanitizedNpcOptions,"VISUAL_DESCRIPTION")&&!contains(sanitizedNpcOptions,"{NAME:"),
                "nenhuma sugestão pode exibir campos internos do NPC");

        System.out.println("OK: "+checks+" verificações de sugestões e histórico.");
    }
}
