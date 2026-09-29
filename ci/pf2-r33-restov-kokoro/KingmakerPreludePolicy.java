package com.braseiro.pathfinder2e;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;

/**
 * Contrato do prólogo de Kingmaker.
 *
 * A aleatoriedade existe dentro de Restov e nunca decide pelo jogador quando
 * a campanha principal começa. A ida à mansão de Lady Jamandi precisa nascer
 * de uma ação explícita do jogador (ou do botão equivalente na UI).
 */
public final class KingmakerPreludePolicy {
    public static final String PHASE_RESTOV_ARRIVAL = "restov_arrival";
    public static final String PHASE_RESTOV_SANDBOX = "restov_sandbox";
    public static final String PHASE_JAMANDI_MANSION = "jamandi_mansion";
    public static final String PHASE_CHARTER_RECEIVED = "charter_received";
    public static final String PHASE_STOLEN_LANDS_EXPEDITION = "stolen_lands_expedition";

    private static final String[] ARRIVAL_MODES = {
        "a pé, misturados ao fluxo de viajantes que alcança a cidade",
        "a cavalo, depois de uma jornada que deixou poeira ou lama nas roupas e arreios",
        "em uma carroça de mercadores, como passageiros pagos ou convidados de ocasião",
        "numa carroça de carga, ocultos entre fardos ou sob o feno por uma razão coerente com a história",
        "junto de uma pequena caravana que se desfaz ao alcançar os acessos urbanos",
        "em companhia temporária de viajantes encontrados na estrada, que seguem destinos diferentes ao entrar na cidade"
    };

    private static final String[] CITY_PRESSURES = {
        "o acesso está congestionado por carroças, animais, carregadores e guardas conferindo quem entra",
        "uma pequena discussão entre viajantes e autoridades cria um atraso visível no acesso à cidade",
        "mercadores disputam passagem e espaço enquanto trabalhadores tentam manter o fluxo andando",
        "um acontecimento cotidiano nas imediações do acesso chama atenção sem obrigar os heróis a se envolver",
        "um rumor passa de boca em boca entre recém-chegados, mas ninguém parece ter a história inteira",
        "a mudança de turno da guarda altera o ritmo da entrada e faz alguns viajantes procurarem outra fila"
    };

    private static final String[] WEATHER = {
        "céu limpo e ar frio",
        "garoa fina e persistente",
        "chuva moderada empurrada pelo vento",
        "nuvens baixas e ameaça de chuva",
        "vento seco e céu parcialmente encoberto",
        "neblina rala se desfazendo aos poucos",
        "tempo firme depois de uma chuva recente",
        "frio cortante sob um céu claro"
    };

    private static final String[] DAYPART = {
        "Amanhecer", "Manhã", "Meio-dia", "Tarde", "Entardecer", "Noite"
    };
    private static final double[] HOURS = {6d, 9d, 12d, 15d, 18d, 21d};
    private static final String[] LIGHTING = {
        "Primeira luz do amanhecer", "Luz da manhã", "Luz alta do meio-dia",
        "Luz da tarde", "Luz baixa do entardecer", "Luz noturna de tochas, janelas e lua"
    };

    private KingmakerPreludePolicy() {}

    public static boolean isRestovPhase(String phase) {
        String p = safe(phase);
        return PHASE_RESTOV_ARRIVAL.equals(p) || PHASE_RESTOV_SANDBOX.equals(p);
    }

    public static boolean isMansionPhase(String phase) {
        String p = safe(phase);
        return PHASE_JAMANDI_MANSION.equals(p) || PHASE_CHARTER_RECEIVED.equals(p);
    }

    public static boolean isPreExpeditionPhase(String phase) {
        return isRestovPhase(phase) || isMansionPhase(phase);
    }

    public static boolean isMansionIntent(String raw) {
        String n = norm(raw);
        boolean target = n.contains("jamandi") || n.contains("aldori");
        if (!target) return false;
        if (containsAny(n, "nao vou", "nao vamos", "nao quero ir", "nao iremos", "nao irei",
                "ainda nao", "depois eu vou", "depois vamos", "nao entrar", "nao visit")) return false;
        return containsAny(n, "vou ", "vamos ", "ir ", "indo ", "seguir ", "sigo ", "dirigir",
                "caminh", "entrar", "entro ", "visitar", "visito", "comparecer", "aceito o convite",
                "aceitamos o convite", "chegar", "procuro", "procurar", "rumo a", "rumo à");
    }

    public static boolean isCharterAcceptanceIntent(String raw) {
        String n = norm(raw);
        boolean target = containsAny(n, "carta", "charter", "missao", "expedicao", "terras roubadas");
        if (!target) return false;
        if (containsAny(n, "nao aceito", "nao assino", "recuso", "nao concordo", "nao topo")) return false;
        return containsAny(n, "aceito", "aceitamos", "assino", "assinamos", "concordo", "concordamos",
                "topo", "aceitar", "assinar");
    }

    public static boolean isExpeditionDepartureIntent(String raw) {
        String n = norm(raw);
        boolean target = containsAny(n, "terras roubadas", "stolen lands", "oleg", "posto de oleg",
                "expedicao", "fronteira ao sul", "fronteira sul");
        if (!target) return false;
        if (containsAny(n, "nao vamos", "nao vou", "nao partir", "nao saio", "ainda nao", "ficamos em restov")) return false;
        return containsAny(n, "partir", "partimos", "parto", "viajar", "viajamos", "seguir", "seguimos",
                "sair", "saimos", "deixar restov", "rumo a", "rumo à", "vamos para", "vou para",
                "iniciar a jornada", "comecar a jornada");
    }

    public static String arrivalDirective(long seed) {
        Random random = new Random(seed ^ 0x4b494e474d414b45L);
        String mode = ARRIVAL_MODES[Math.floorMod(random.nextInt(), ARRIVAL_MODES.length)];
        String pressure = CITY_PRESSURES[Math.floorMod(random.nextInt(), CITY_PRESSURES.length)];
        return "A PRIMEIRA CENA DEVE MOSTRAR OS PERSONAGENS CHEGANDO A RESTOV. Nesta execução, use como ponto de partida: "
                + mode + "; " + pressure + ". Isso é uma variação escolhida para esta campanha, não uma fórmula permanente. "
                + "A chegada pode variar entre campanhas de muitas outras formas coerentes. Não trate exemplos como carroça, feno, cavalo ou caminhada como uma lista fechada.";
    }

    public static String randomWeather(long seed) {
        Random random = new Random(seed ^ 0x57454154484552L);
        return WEATHER[Math.floorMod(random.nextInt(), WEATHER.length)];
    }

    public static String randomDaypart(long seed) {
        Random random = new Random(seed ^ 0x44415950415254L);
        return DAYPART[Math.floorMod(random.nextInt(), DAYPART.length)];
    }

    public static double hourForDaypart(String daypart) {
        String d = safe(daypart);
        for (int i = 0; i < DAYPART.length; i++) if (DAYPART[i].equals(d)) return HOURS[i];
        return 9d;
    }

    public static String lightingForDaypart(String daypart) {
        String d = safe(daypart);
        for (int i = 0; i < DAYPART.length; i++) if (DAYPART[i].equals(d)) return LIGHTING[i];
        return "Luz da manhã";
    }

    public static String directiveForPhase(String phase) {
        String p = safe(phase);
        if (PHASE_RESTOV_ARRIVAL.equals(p) || PHASE_RESTOV_SANDBOX.equals(p)) {
            return "KINGMAKER — RESTOV SANDBOX: a campanha principal ainda NÃO começou. Restov deve funcionar como cidade viva: NPCs, rumores, pequenos conflitos e acontecimentos podem surgir organicamente. "
                    + "Lady Jamandi Aldori e sua convocação podem existir como informação do mundo, mas NÃO transporte o grupo para a mansão, NÃO aceite a missão por eles e NÃO ative Oleg/Terras Roubadas. "
                    + "A mansão só entra em cena quando o jogador decidir explicitamente ir até lá.";
        }
        if (PHASE_JAMANDI_MANSION.equals(p)) {
            return "KINGMAKER — MANSÃO DE LADY JAMANDI: o jogador escolheu entrar no arco principal. Conduza a sequência canônica em cenas, sem resumo apressado: recepção e encontro dos convocados, banquete e interações, repouso oferecido aos hóspedes, ataque noturno à mansão e suas consequências, reencontro com Lady Jamandi e concessão das cartas/missões. "
                    + "Não pule diretamente para Oleg e não trate acontecimentos futuros como já resolvidos. A agência e as mecânicas PF2e continuam valendo em cada cena.";
        }
        if (PHASE_CHARTER_RECEIVED.equals(p)) {
            return "KINGMAKER — CARTA RECEBIDA: a missão principal foi aceita, mas o grupo ainda está em Restov até decidir partir. Permita despedidas, preparação, compras, conversas e outros atos naturais. "
                    + "Não teleporte para as Terras Roubadas. Só mude para a expedição quando houver intenção explícita de partir.";
        }
        if (PHASE_STOLEN_LANDS_EXPEDITION.equals(p)) {
            return "KINGMAKER — EXPEDIÇÃO ATIVA: o grupo deixou Restov rumo às Terras Roubadas. A partir daqui use a ordem persistida de capítulos, missões e pré-requisitos de Kingmaker. "
                    + "O Posto de Oleg é o primeiro grande ponto de apoio do arco das Terras Roubadas, mas viagem, encontros e decisões devem acontecer naturalmente em vez de serem resumidos como teleporte.";
        }
        return "";
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String norm(String value) {
        String n = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return n;
    }
}
