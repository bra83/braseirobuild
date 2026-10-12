package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public final class NarratedNpcExtractorSelfTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static JSONObject find(JSONArray values,String name){for(int i=0;i<values.length();i++){JSONObject value=values.optJSONObject(i);if(value!=null&&name.equals(value.optString("name")))return value;}return null;}
    private static boolean has(JSONArray values,String name){return find(values,name)!=null;}
    public static void main(String[] args){
        String scene="Entrego suas notas promissórias antes do meio-dia — declarou o Barão Varian Vance, pressionando o anel de selo. Ao lado, Mira Valecinza diz que o documento foi alterado.";
        JSONArray values=NarratedNpcExtractor.extract(scene,12);
        check(has(values,"Barão Varian Vance"),"título e nome no meio da frase precisam ser extraídos");
        check(has(values,"Mira Valecinza"),"personagem nomeado em ação precisa ser extraído");
        JSONObject baron=find(values,"Barão Varian Vance");
        check(baron!=null&&"Barão".equals(baron.optString("role")),"o título deve alimentar o papel visual");
        check(baron!=null&&baron.optBoolean("present",false),"NPC extraído deve ser marcado como presente");
        check(NarratedNpcExtractor.extract("O documento original foi lacrado.",12).length()==0,"objetos e início de frase não podem virar NPCs");
        check(NarratedNpcExtractor.extract("Você diz que seguirá pela floresta.",12).length()==0,"Você nunca pode ser criado como NPC");
        check(NarratedNpcExtractor.extract("Jogador responde com cautela.",12).length()==0,"Jogador nunca pode ser criado como NPC");
        check(NarratedNpcExtractor.extract("Kaelen responde com cautela.",12).length()==1,"um nome real continua sendo extraído");

        String screenshot="O vento matinal raspa sobre o cascalho seco das Charnecas Eternas, empurrando o odor acre de salmoura velha e escamas secas diretamente para o aglomerado de viajantes parados junto ao poço da bifurcação. Os dois corredores avançam com os peixes balançando em riste, o raspar rítmico das botas pesadas contra a terra batida sobressaindo-se ao burburinho dos transeuntes que recuam de imediato. Um velho encostado ao bebedouro ergue o cajado para bloquear a passagem.";
        JSONArray cast=NarratedNpcExtractor.extract(screenshot,12);
        JSONObject runners=find(cast,"Corredores");
        JSONObject old=find(cast,"Velho");
        JSONObject travelers=find(cast,"Viajantes");
        JSONObject passers=find(cast,"Transeuntes");
        check(runners!=null,"os dois corredores ativos da cena precisam aparecer no elenco visual");
        check(runners!=null&&runners.optInt("count",0)==2,"quantidade explícita dos corredores precisa ser preservada");
        check(runners!=null&&runners.optBoolean("transient",false),"figurante anônimo deve ser transitório, não memória permanente");
        check(old!=null,"o velho que ergue o cajado precisa aparecer no elenco visual");
        check(old!=null&&old.optBoolean("anonymous",false),"velho sem nome próprio deve ser marcado como anônimo");
        check(travelers!=null&&travelers.optBoolean("group",false),"aglomerado de viajantes deve aparecer como grupo de cena");
        check(passers!=null&&passers.optBoolean("group",false),"transeuntes que reagem à ação devem aparecer como grupo de cena");
        check(!has(cast,"Vento")&&!has(cast,"Peixes")&&!has(cast,"Botas"),"objetos e ambiente não podem virar participantes");
        JSONArray namedRole=NarratedNpcExtractor.extract("O mercador Kaelen responde com cautela.",12);
        check(has(namedRole,"Kaelen")&&!has(namedRole,"Mercador"),"papel genérico seguido de nome próprio não pode duplicar o mesmo NPC");
        java.util.HashSet<String> excluded=new java.util.HashSet<String>();excluded.add("nidal");
        String grounding="O decreto de fundação e patrulha da região de Nidal exige braços dispostos. A Coroa não financia vagas-lúmens, anuncia Barbara de Vetro. Dois guardas vigiam a entrada enquanto ela estende o decreto.";
        JSONArray grounded=NarratedNpcExtractor.extract(grounding,12,excluded);
        check(!has(grounded,"Nidal"),"o nome do local não pode virar NPC por aparecer antes de um verbo");
        check(has(grounded,"Barbara de Vetro"),"atribuição invertida deve reconhecer o NPC nomeado depois do verbo");
        JSONObject guards=find(grounded,"Guardas");
        check(guards!=null&&guards.optBoolean("transient",false)&&guards.optInt("count",0)==2,"guardas em primeiro plano devem aparecer como grupo transitório");
        System.out.println("OK: "+checks+" verificações de extração de NPC narrado.");
    }
}
