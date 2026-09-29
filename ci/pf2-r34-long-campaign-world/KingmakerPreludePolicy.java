package com.braseiro.pathfinder2e;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;

/** Contrato persistente do prólogo Kingmaker: Restov sandbox antes da mansão. */
public final class KingmakerPreludePolicy {
    public static final String PHASE_RESTOV_ARRIVAL="restov_arrival";
    public static final String PHASE_RESTOV_SANDBOX="restov_sandbox";
    public static final String PHASE_JAMANDI_MANSION="jamandi_mansion";
    public static final String PHASE_CHARTER_RECEIVED="charter_received";
    public static final String PHASE_STOLEN_LANDS_EXPEDITION="stolen_lands_expedition";

    private static final String[] ARRIVAL_MODES={
        "a pé, misturados ao fluxo de viajantes que alcança a cidade",
        "a cavalo, depois de uma jornada que deixou poeira ou lama nas roupas e arreios",
        "em uma carroça de mercadores, como passageiros pagos ou convidados de ocasião",
        "numa carroça de carga, ocultos entre fardos ou sob o feno por uma razão coerente com a história",
        "junto de uma pequena caravana que se desfaz ao alcançar os acessos urbanos",
        "em companhia temporária de viajantes encontrados na estrada, que seguem destinos diferentes ao entrar na cidade"
    };
    private static final String[] CITY_PRESSURES={
        "o acesso está congestionado por carroças, animais, carregadores e guardas conferindo quem entra",
        "uma pequena discussão entre viajantes e autoridades cria um atraso visível no acesso à cidade",
        "mercadores disputam passagem e espaço enquanto trabalhadores tentam manter o fluxo andando",
        "um acontecimento cotidiano nas imediações do acesso chama atenção sem obrigar os heróis a se envolver",
        "um rumor passa de boca em boca entre recém-chegados, mas ninguém parece ter a história inteira",
        "a mudança de turno da guarda altera o ritmo da entrada e faz alguns viajantes procurarem outra fila"
    };
    private static final String[] WEATHER={"céu limpo e ar frio","garoa fina e persistente","chuva moderada empurrada pelo vento","nuvens baixas e ameaça de chuva","vento seco e céu parcialmente encoberto","neblina rala se desfazendo aos poucos","tempo firme depois de uma chuva recente","frio cortante sob um céu claro"};
    private static final String[] DAYPART={"Amanhecer","Manhã","Meio-dia","Tarde","Entardecer","Noite"};
    private static final double[] HOURS={6d,9d,12d,15d,18d,21d};
    private static final String[] LIGHTING={"Primeira luz do amanhecer","Luz da manhã","Luz alta do meio-dia","Luz da tarde","Luz baixa do entardecer","Luz noturna de tochas, janelas e lua"};

    private KingmakerPreludePolicy(){}

    public static boolean isRestovPhase(String phase){String p=safe(phase);return PHASE_RESTOV_ARRIVAL.equals(p)||PHASE_RESTOV_SANDBOX.equals(p);}
    public static boolean isMansionPhase(String phase){String p=safe(phase);return PHASE_JAMANDI_MANSION.equals(p)||PHASE_CHARTER_RECEIVED.equals(p);}
    public static boolean isPreExpeditionPhase(String phase){return isRestovPhase(phase)||isMansionPhase(phase);}

    public static boolean isMansionIntent(String raw){
        String n=norm(raw);
        boolean target=n.contains("jamandi")||n.contains("aldori")||n.contains("mansao");
        if(!target)return false;
        if(negatesMovement(n))return false;
        return containsAny(n,"vou ","vamos ","ir ","indo ","seguir ","sigo ","dirigir","caminh","entrar","entro ","visitar","visito","comparecer","aceito o convite","aceitamos o convite","chegar","procuro","procurar","rumo a");
    }

    public static boolean isCharterAcceptanceIntent(String raw){
        String n=norm(raw);boolean target=containsAny(n,"carta","charter","missao","expedicao","terras roubadas");
        if(!target||containsAny(n,"nao aceito","nao assino","recuso","nao concordo","nao topo","nunca aceito","jamais aceito"))return false;
        return containsAny(n,"aceito","aceitamos","assino","assinamos","concordo","concordamos","topo","aceitar","assinar");
    }

    public static boolean isExpeditionDepartureIntent(String raw){
        String n=norm(raw);boolean target=containsAny(n,"terras roubadas","stolen lands","oleg","posto de oleg","expedicao","fronteira ao sul","fronteira sul");
        if(!target||negatesMovement(n)||containsAny(n,"ficamos em restov","ficarei em restov","ficaremos em restov"))return false;
        return containsAny(n,"partir","partimos","parto","viajar","viajamos","seguir","seguimos","sair","saimos","deixar restov","rumo a","vamos para","vou para","iniciar a jornada","comecar a jornada");
    }

    private static boolean negatesMovement(String n){
        if(containsAny(n,"nao vou","nao vamos","nao quero ir","nao queremos ir","nao iremos","nao irei","ainda nao","nao entrar","nao entro","nao visit","nao seguir","nao parto","nao partimos","nao sair","nao saio","nunca vou","jamais vou","nunca iremos","jamais iremos"))return true;
        String compact=" "+n+" ";
        String[] neg={" nao "," nunca "," jamais "};
        String[] move={" vou "," vamos "," ir "," entrar "," visitar "," seguir "," partir "," sair "," rumo "};
        for(String a:neg)for(String b:move){int i=compact.indexOf(a);int j=compact.indexOf(b);if(i>=0&&j>=0&&Math.abs(i-j)<=40)return true;}
        return false;
    }

    public static String arrivalDirective(long seed){
        Random r=new Random(seed^0x4b494e474d414b45L);
        String mode=ARRIVAL_MODES[Math.floorMod(r.nextInt(),ARRIVAL_MODES.length)];
        String pressure=CITY_PRESSURES[Math.floorMod(r.nextInt(),CITY_PRESSURES.length)];
        return "A PRIMEIRA CENA DEVE MOSTRAR OS PERSONAGENS CHEGANDO A RESTOV. Nesta execução, use como ponto de partida: "+mode+"; "+pressure+". Isso é uma variação escolhida para esta campanha, não uma fórmula permanente. A chegada pode variar entre campanhas de muitas outras formas coerentes. Não trate exemplos como carroça, feno, cavalo ou caminhada como uma lista fechada.";
    }
    public static String randomWeather(long seed){Random r=new Random(seed^0x57454154484552L);return WEATHER[Math.floorMod(r.nextInt(),WEATHER.length)];}
    public static String randomDaypart(long seed){Random r=new Random(seed^0x44415950415254L);return DAYPART[Math.floorMod(r.nextInt(),DAYPART.length)];}
    public static double hourForDaypart(String d){String x=safe(d);for(int i=0;i<DAYPART.length;i++)if(DAYPART[i].toLowerCase(Locale.ROOT).equals(x))return HOURS[i];return 9d;}
    public static String lightingForDaypart(String d){String x=safe(d);for(int i=0;i<DAYPART.length;i++)if(DAYPART[i].toLowerCase(Locale.ROOT).equals(x))return LIGHTING[i];return "Luz da manhã";}

    public static String directiveForPhase(String phase){
        String p=safe(phase);
        if(PHASE_RESTOV_ARRIVAL.equals(p)||PHASE_RESTOV_SANDBOX.equals(p))return "KINGMAKER — RESTOV SANDBOX: a campanha principal ainda NÃO começou. Restov funciona como cidade viva, com NPCs, rumores, conflitos e acontecimentos orgânicos. Lady Jamandi Aldori e sua convocação podem existir como informação do mundo, mas NÃO transporte o grupo para a mansão, NÃO aceite a missão por eles e NÃO ative Oleg/Terras Roubadas. A mansão só entra em cena quando o jogador decidir explicitamente ir até lá.";
        if(PHASE_JAMANDI_MANSION.equals(p))return "KINGMAKER — MANSÃO DE LADY JAMANDI: o jogador escolheu entrar no arco principal. Conduza a sequência canônica em cenas, sem resumo apressado; não pule diretamente para Oleg e preserve agência e mecânicas PF2e.";
        if(PHASE_CHARTER_RECEIVED.equals(p))return "KINGMAKER — CARTA RECEBIDA: a missão principal foi aceita, mas o grupo ainda está em Restov até decidir partir. Permita preparação, despedidas, compras e conversas. Não teleporte para as Terras Roubadas.";
        if(PHASE_STOLEN_LANDS_EXPEDITION.equals(p))return "KINGMAKER — EXPEDIÇÃO ATIVA: o grupo deixou Restov rumo às Terras Roubadas. A partir daqui use a ordem persistida de capítulos, missões e pré-requisitos; o Posto de Oleg é ponto de apoio, não teleporte.";
        return "";
    }

    private static boolean containsAny(String text,String... values){for(String v:values)if(text.contains(v))return true;return false;}
    private static String safe(String v){return v==null?"":v.trim().toLowerCase(Locale.ROOT);}
    private static String norm(String v){
        String s=v==null?"":v;
        s=s.replace('á','a').replace('à','a').replace('â','a').replace('ã','a').replace('ä','a')
           .replace('é','e').replace('è','e').replace('ê','e').replace('ë','e')
           .replace('í','i').replace('ì','i').replace('î','i').replace('ï','i')
           .replace('ó','o').replace('ò','o').replace('ô','o').replace('õ','o').replace('ö','o')
           .replace('ú','u').replace('ù','u').replace('û','u').replace('ü','u')
           .replace('ç','c').replace('Á','A').replace('À','A').replace('Â','A').replace('Ã','A')
           .replace('É','E').replace('Ê','E').replace('Í','I').replace('Ó','O').replace('Ô','O').replace('Õ','O').replace('Ú','U').replace('Ç','C');
        return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();
    }
}
