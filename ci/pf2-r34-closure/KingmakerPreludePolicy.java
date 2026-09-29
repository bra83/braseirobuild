package com.braseiro.pathfinder2e;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;

/**
 * Contrato narrativo persistente do prólogo de Kingmaker.
 * Restov é sandbox até escolha explícita do jogador. Dentro da mansão,
 * a progressão é sequencial e não permite saltar marcos canônicos.
 */
public final class KingmakerPreludePolicy {
    public static final String PHASE_RESTOV_ARRIVAL = "restov_arrival";
    public static final String PHASE_RESTOV_SANDBOX = "restov_sandbox";

    public static final String PHASE_JAMANDI_APPROACH = "jamandi_approach";
    /** Alias de compatibilidade com a R33. */
    public static final String PHASE_JAMANDI_MANSION = PHASE_JAMANDI_APPROACH;
    public static final String PHASE_JAMANDI_RECEPTION = "jamandi_reception";
    public static final String PHASE_JAMANDI_FEAST = "jamandi_feast";
    public static final String PHASE_JAMANDI_GUEST_QUARTERS = "jamandi_guest_quarters";
    public static final String PHASE_JAMANDI_ATTACK = "jamandi_attack";
    public static final String PHASE_JAMANDI_AFTER_ATTACK = "jamandi_after_attack";
    public static final String PHASE_JAMANDI_CHARTER = "jamandi_charter";

    public static final String PHASE_CHARTER_RECEIVED = "charter_received";
    public static final String PHASE_STOLEN_LANDS_EXPEDITION = "stolen_lands_expedition";

    private static final String[] ARRIVAL_MODES = {
        "a pé, misturados ao fluxo de viajantes que alcança a cidade",
        "a cavalo, depois de uma jornada que deixou poeira ou lama nas roupas e arreios",
        "em uma carroça de mercadores, como passageiros pagos ou convidados de ocasião",
        "numa carroça de carga, ocultos entre fardos ou sob o feno por uma razão coerente com a história",
        "junto de uma pequena caravana que se desfaz ao alcançar os acessos urbanos",
        "em companhia temporária de viajantes encontrados na estrada, que seguem destinos diferentes ao entrar na cidade",
        "numa carroça de fazenda que segue para os mercados, sem que isso obrigue os heróis a conhecer o condutor",
        "pela estrada principal depois de uma noite ao relento, alcançando os primeiros bairros junto com outros recém-chegados"
    };

    private static final String[] CITY_PRESSURES = {
        "o acesso está congestionado por carroças, animais, carregadores e guardas conferindo quem entra",
        "uma pequena discussão entre viajantes e autoridades cria um atraso visível no acesso à cidade",
        "mercadores disputam passagem e espaço enquanto trabalhadores tentam manter o fluxo andando",
        "um acontecimento cotidiano nas imediações do acesso chama atenção sem obrigar os heróis a se envolver",
        "um rumor passa de boca em boca entre recém-chegados, mas ninguém parece ter a história inteira",
        "a mudança de turno da guarda altera o ritmo da entrada e faz alguns viajantes procurarem outra fila",
        "uma entrega atrasada reúne cocheiros e comerciantes em torno de uma discussão que pode ou não interessar aos heróis",
        "uma patrulha atravessa o fluxo da rua e força pedestres e montarias a abrirem passagem"
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

    public static boolean isKnownPhase(String phase) {
        String p = safe(phase);
        return PHASE_RESTOV_ARRIVAL.equals(p)
                || PHASE_RESTOV_SANDBOX.equals(p)
                || PHASE_JAMANDI_APPROACH.equals(p)
                || PHASE_JAMANDI_RECEPTION.equals(p)
                || PHASE_JAMANDI_FEAST.equals(p)
                || PHASE_JAMANDI_GUEST_QUARTERS.equals(p)
                || PHASE_JAMANDI_ATTACK.equals(p)
                || PHASE_JAMANDI_AFTER_ATTACK.equals(p)
                || PHASE_JAMANDI_CHARTER.equals(p)
                || PHASE_CHARTER_RECEIVED.equals(p)
                || PHASE_STOLEN_LANDS_EXPEDITION.equals(p);
    }

    public static boolean isRestovPhase(String phase) {
        String p = safe(phase);
        return PHASE_RESTOV_ARRIVAL.equals(p) || PHASE_RESTOV_SANDBOX.equals(p);
    }

    public static boolean isMansionPhase(String phase) {
        String p = safe(phase);
        return PHASE_JAMANDI_APPROACH.equals(p)
                || PHASE_JAMANDI_RECEPTION.equals(p)
                || PHASE_JAMANDI_FEAST.equals(p)
                || PHASE_JAMANDI_GUEST_QUARTERS.equals(p)
                || PHASE_JAMANDI_ATTACK.equals(p)
                || PHASE_JAMANDI_AFTER_ATTACK.equals(p)
                || PHASE_JAMANDI_CHARTER.equals(p)
                || PHASE_CHARTER_RECEIVED.equals(p);
    }

    public static boolean isPreExpeditionPhase(String phase) {
        return isRestovPhase(phase) || isMansionPhase(phase);
    }

    public static boolean isMajorScenePhase(String phase) {
        String p = safe(phase);
        return PHASE_RESTOV_ARRIVAL.equals(p)
                || PHASE_JAMANDI_APPROACH.equals(p)
                || PHASE_JAMANDI_RECEPTION.equals(p)
                || PHASE_JAMANDI_FEAST.equals(p)
                || PHASE_JAMANDI_ATTACK.equals(p)
                || PHASE_JAMANDI_AFTER_ATTACK.equals(p)
                || PHASE_JAMANDI_CHARTER.equals(p);
    }

    public static boolean isMansionIntent(String raw) {
        String n = norm(raw);
        boolean target = n.contains("jamandi") || n.contains("aldori") || n.contains("mansao") || n.contains("convocacao");
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
                "expedicao", "fronteira ao sul", "fronteira sul", "deixar restov");
        if (!target) return false;
        if (containsAny(n, "nao vamos", "nao vou", "nao partir", "nao saio", "ainda nao", "ficamos em restov")) return false;
        return containsAny(n, "partir", "partimos", "parto", "viajar", "viajamos", "seguir", "seguimos",
                "sair", "saimos", "deixar restov", "rumo a", "rumo à", "vamos para", "vou para",
                "iniciar a jornada", "comecar a jornada");
    }

    /**
     * Ação livre nunca salta mais de um marco. Carta e partida exigem ações
     * específicas e não são inferidas de uma menção casual.
     */
    public static String nextMansionPhaseForAction(String currentPhase, String raw) {
        String p = safe(currentPhase);
        String n = norm(raw);
        if (PHASE_JAMANDI_APPROACH.equals(p)) {
            if (containsAny(n, "entr", "cheg", "aproxim", "portao", "porta", "recepc", "apresent", "continu")) return PHASE_JAMANDI_RECEPTION;
            return p;
        }
        if (PHASE_JAMANDI_RECEPTION.equals(p)) {
            if (containsAny(n, "banquet", "jantar", "refeic", "comer", "salao", "festa", "continu")) return PHASE_JAMANDI_FEAST;
            return p;
        }
        if (PHASE_JAMANDI_FEAST.equals(p)) {
            if (containsAny(n, "quarto", "aposent", "retirar", "dorm", "descans", "repous", "continu")) return PHASE_JAMANDI_GUEST_QUARTERS;
            return p;
        }
        if (PHASE_JAMANDI_GUEST_QUARTERS.equals(p)) {
            if (containsAny(n, "dorm", "descans", "repous", "noite", "continu")) return PHASE_JAMANDI_ATTACK;
            return p;
        }
        if (PHASE_JAMANDI_ATTACK.equals(p)) {
            if (containsAny(n, "ataque acabou", "ataque terminou", "inimigos derrotados", "ameaça terminou",
                    "ameaca terminou", "combate terminou", "combate acabou", "area segura", "área segura",
                    "procurar jamandi", "encontrar jamandi")) return PHASE_JAMANDI_AFTER_ATTACK;
            return p;
        }
        if (PHASE_JAMANDI_AFTER_ATTACK.equals(p)) {
            if (containsAny(n, "jamandi", "reun", "amanhec", "carta", "missao", "continu")) return PHASE_JAMANDI_CHARTER;
            return p;
        }
        if (PHASE_JAMANDI_CHARTER.equals(p)) {
            return isCharterAcceptanceIntent(raw) ? PHASE_CHARTER_RECEIVED : p;
        }
        if (PHASE_CHARTER_RECEIVED.equals(p)) {
            return isExpeditionDepartureIntent(raw) ? PHASE_STOLEN_LANDS_EXPEDITION : p;
        }
        return p;
    }

    /** Botão de continuidade avança somente marcos que não exigem resolução mecânica ou aceite. */
    public static String nextMansionPhaseForContinue(String currentPhase) {
        String p = safe(currentPhase);
        if (PHASE_JAMANDI_APPROACH.equals(p)) return PHASE_JAMANDI_RECEPTION;
        if (PHASE_JAMANDI_RECEPTION.equals(p)) return PHASE_JAMANDI_FEAST;
        if (PHASE_JAMANDI_FEAST.equals(p)) return PHASE_JAMANDI_GUEST_QUARTERS;
        if (PHASE_JAMANDI_GUEST_QUARTERS.equals(p)) return PHASE_JAMANDI_ATTACK;
        if (PHASE_JAMANDI_AFTER_ATTACK.equals(p)) return PHASE_JAMANDI_CHARTER;
        return p;
    }

    public static String arrivalDirective(long seed) {
        Random random = new Random(seed ^ 0x4b494e474d414b45L);
        String mode = ARRIVAL_MODES[Math.floorMod(random.nextInt(), ARRIVAL_MODES.length)];
        String pressure = CITY_PRESSURES[Math.floorMod(random.nextInt(), CITY_PRESSURES.length)];
        return "A PRIMEIRA CENA DEVE MOSTRAR OS PERSONAGENS CHEGANDO A RESTOV. Nesta campanha, use como ponto de partida: "
                + mode + "; " + pressure + ". Isso é apenas a circunstância desta chegada. A cidade deve reagir organicamente, "
                + "com NPCs e acontecimentos que podem variar e persistir; os exemplos não formam uma lista fechada.";
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
        String d = daypart == null ? "" : daypart.trim();
        for (int i = 0; i < DAYPART.length; i++) if (DAYPART[i].equals(d)) return HOURS[i];
        return 9d;
    }

    public static String lightingForDaypart(String daypart) {
        String d = daypart == null ? "" : daypart.trim();
        for (int i = 0; i < DAYPART.length; i++) if (DAYPART[i].equals(d)) return LIGHTING[i];
        return "Luz da manhã";
    }

    public static String directiveForPhase(String phase) {
        String p = safe(phase);
        if (PHASE_RESTOV_ARRIVAL.equals(p) || PHASE_RESTOV_SANDBOX.equals(p)) {
            return "KINGMAKER — RESTOV SANDBOX: a campanha principal ainda NÃO começou. Restov é uma cidade viva. "
                    + "NPCs, rumores, trabalhos, pequenos conflitos e acontecimentos podem surgir de modo orgânico e devem persistir quando se tornarem relevantes. "
                    + "Lady Jamandi Aldori e a convocação podem existir no mundo, mas NÃO transporte o grupo para a mansão, NÃO aceite missão por eles e NÃO ative Oleg/Terras Roubadas.";
        }
        if (PHASE_JAMANDI_APPROACH.equals(p)) {
            return "KINGMAKER — APROXIMAÇÃO À MANSÃO: o jogador escolheu iniciar o arco principal. Narre o deslocamento por Restov, a chegada à propriedade e o limiar da recepção. Não resuma o banquete, o ataque ou a carta.";
        }
        if (PHASE_JAMANDI_RECEPTION.equals(p)) {
            return "KINGMAKER — RECEPÇÃO DOS CONVOCADOS: apresente a recepção e os presentes de forma jogável. O próximo marco é o banquete; não pule para o repouso, ataque ou carta.";
        }
        if (PHASE_JAMANDI_FEAST.equals(p)) {
            return "KINGMAKER — BANQUETE: conduza o banquete e as interações como cena. Preserve agência e oportunidades de conversa. Não inicie a carta nem teleporte para as Terras Roubadas.";
        }
        if (PHASE_JAMANDI_GUEST_QUARTERS.equals(p)) {
            return "KINGMAKER — HÓSPEDES NA MANSÃO: depois do banquete, o grupo pode se recolher, conversar ou agir antes do repouso. O ataque noturno só começa quando a ficção realmente chega a esse ponto.";
        }
        if (PHASE_JAMANDI_ATTACK.equals(p)) {
            return "KINGMAKER — ATAQUE À MANSÃO: trate o ataque como cena e encontros reais de PF2e. Não considere inimigos vencidos sem resolução. A progressão fica BLOQUEADA neste marco até o ataque/combate estar efetivamente encerrado.";
        }
        if (PHASE_JAMANDI_AFTER_ATTACK.equals(p)) {
            return "KINGMAKER — CONSEQUÊNCIAS DO ATAQUE: mostre os danos, sobreviventes, reencontros e consequências antes da reunião que entrega as cartas. Não pule direto para Oleg.";
        }
        if (PHASE_JAMANDI_CHARTER.equals(p)) {
            return "KINGMAKER — CARTA DE EXPLORAÇÃO: Lady Jamandi apresenta a missão/carta. O jogo NÃO aceita a carta pelo jogador. Permaneça neste marco até uma aceitação explícita.";
        }
        if (PHASE_CHARTER_RECEIVED.equals(p)) {
            return "KINGMAKER — CARTA RECEBIDA: a missão foi aceita, mas o grupo continua em Restov. Permita compras, despedidas, preparação e outras ações. A expedição só começa quando o jogador decidir partir explicitamente.";
        }
        if (PHASE_STOLEN_LANDS_EXPEDITION.equals(p)) {
            return "KINGMAKER — EXPEDIÇÃO ATIVA: o grupo deixou Restov rumo às Terras Roubadas. A partir daqui use a ordem persistida de capítulos, missões e pré-requisitos. O Posto de Oleg é destino inicial, não teleporte.";
        }
        return "";
    }

    public static String titleForPhase(String phase) {
        String p = safe(phase);
        if (PHASE_RESTOV_ARRIVAL.equals(p)) return "Chegada a Restov";
        if (PHASE_RESTOV_SANDBOX.equals(p)) return "Restov — sandbox";
        if (PHASE_JAMANDI_APPROACH.equals(p)) return "A caminho da Mansão Aldori";
        if (PHASE_JAMANDI_RECEPTION.equals(p)) return "Convocação de Heróis — recepção";
        if (PHASE_JAMANDI_FEAST.equals(p)) return "Convocação de Heróis — banquete";
        if (PHASE_JAMANDI_GUEST_QUARTERS.equals(p)) return "Convocação de Heróis — hóspedes";
        if (PHASE_JAMANDI_ATTACK.equals(p)) return "Convocação de Heróis — ataque";
        if (PHASE_JAMANDI_AFTER_ATTACK.equals(p)) return "Convocação de Heróis — consequências";
        if (PHASE_JAMANDI_CHARTER.equals(p)) return "Convocação de Heróis — carta";
        if (PHASE_CHARTER_RECEIVED.equals(p)) return "Preparação em Restov";
        return "Expedição às Terras Roubadas";
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(norm(value))) return true;
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String norm(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
