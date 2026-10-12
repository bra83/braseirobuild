package com.braseiro.pathfinder2e;

import org.json.JSONObject;

/** Exercita classificação estrutural e invalidação por conteúdo de página. */
public final class ManualPageStructureSelfTest {
    private static int checks=0;
    public static void main(String[] args)throws Exception{
        String creature="CAPÍTULO 4 — CRIATURAS\nGoblin\nCA 15\nPV 7\nDeslocamento 9 m\nFOR 8 DES 14 CON 10\nSentidos visão no escuro\nConsulte a tabela anterior.";
        JSONObject block=ManualPageStructureAnalyzer.analyze(creature,42);
        check("creature_block".equals(block.optString("content_kind")),"linhas de estatística formam um bloco de criatura");
        boolean hasTableReference=false;for(int i=0;i<block.optJSONArray("cross_references").length();i++)if("tabela anterior".equalsIgnoreCase(block.optJSONArray("cross_references").optString(i)))hasTableReference=true;
        check(hasTableReference,"referência cruzada é preservada: "+block);
        check(block.optString("section_title").contains("CRIATURAS"),"cabeçalho e seção são preservados");
        check(block.optString("page_hash").length()==64,"página recebe hash SHA-256 do conteúdo");

        String list="ATIVIDADES\n1. Escolha a ferramenta\n2. Pague o custo\n3. Faça o teste";
        JSONObject listed=ManualPageStructureAnalyzer.analyze(list,43);
        check("list".equals(listed.optString("content_kind")),"lista numerada mantém a própria estrutura");
        check(!block.optString("page_hash").equals(ManualPageStructureAnalyzer.analyze(creature+" alterado",42).optString("page_hash")),"alterar o conteúdo invalida o hash da página");

        JSONObject continuation=ManualPageStructureAnalyzer.analyze("E a regra continua até a página seguinte:",44);
        check(continuation.optBoolean("continues_from_previous")&&continuation.optBoolean("continues_on_next"),"continuação entre páginas é marcada nos dois limites");
        System.out.println("OK: "+checks+" cenários de estrutura e invalidação de páginas.");
    }
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
