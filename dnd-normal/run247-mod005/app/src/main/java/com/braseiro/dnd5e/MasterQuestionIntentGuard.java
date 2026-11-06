package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/**
 * User text sent through the free-form action composer is not automatically an
 * action. Direct questions addressed to the Game Master belong in GM_HELP even
 * when the player accidentally leaves the composer in the ACTION mode.
 */
public final class MasterQuestionIntentGuard {
    private MasterQuestionIntentGuard() {}

    public static boolean isConsultation(String raw) {
        if (raw == null) return false;
        String source=raw.trim();
        if (source.isEmpty() || source.length()>800) return false;
        String n=fold(source);
        if (n.isEmpty()) return false;
        // Only natural-language player input, never orchestrator/internal prompts
        // or diegetic statements such as "Pergunto à bibliotecária onde fica...".
        if (n.startsWith("acao voluntaria") || n.startsWith("o jogador confirma")
                || n.startsWith("inicio automatico") || n.startsWith("consulta auxiliar")
                || n.startsWith("contrato narrativo") || n.startsWith("resultado da rolagem")
                || n.startsWith("retomar") || n.startsWith("rolagem registrada"))
            return false;
        if (n.matches("^(pergunto|questiono|indago|abordo|converso|falo|digo|peço|peco) (a|ao|aos|as|com|para) .+"))
            return false;
        if (starts(n,"mestre ","ei mestre ","ola mestre ","duvida ","uma duvida ",
                "qual ","quais ","quem ","onde ","como ","quando ","por que ",
                "porque ","o que ","que teste ","que pericia ","que perícia ",
                "posso ","consigo ","seria possivel ","e possivel ","existe ",
                "existem ","ha algum ","ha alguma ","tem algum ","tem alguma ",
                "preciso fazer ","devo fazer ","saberia me dizer ","poderia me dizer ",
                "qual seria ","quais seriam ","o meu personagem sabe "))
            return true;
        if(source.endsWith("?") && !starts(n,"vou ","tento ","eu tento ","eu ","me aproximo ",
                "uso ","conjuro ","ataco ","pego ","examino ","investigo ","procuro ",
                "busco ","caminho ","entro ","saio ","viajo ","faço ","faco "))
            return true;
        return false;
    }

    private static boolean starts(String input,String... prefixes){
        for(String p:prefixes)if(input.startsWith(p))return true;
        return false;
    }
    private static String fold(String v){
        return Normalizer.normalize(v,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9?]+"," ").trim();
    }
}
