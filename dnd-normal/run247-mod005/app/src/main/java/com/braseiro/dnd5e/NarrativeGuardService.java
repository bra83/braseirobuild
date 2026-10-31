package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Guarda determinístico e sanitizador para assegurar narração de Mestre Humano Imersivo.
 * Conforme documentado no GUIA_IMPLEMENTACAO_NARRACAO_MESTRE_HUMANO_VTT.md.
 */
public final class NarrativeGuardService {
    private NarrativeGuardService() {}

    private static final Pattern UI_LABELS = Pattern.compile(
            "(?im)^\\s*(?:local atual|clima(?: atualizado)?|horário|período|objetivo|status|hex(?:ágono)?|coordenada)\\s*:\\s*.*$");

    private static final Pattern SUGGESTION_HEADERS = Pattern.compile(
            "(?im)^\\s*(?:opções disponíveis|você pode|ações sugeridas)\\s*:\\s*$");

    private static final Pattern SOFTWARE_METALANGUAGE = Pattern.compile(
            "(?i)\\b(?:a cena responde(?: ao último gesto)?|antes que qualquer explicação(?: seja necessária)?|a cena não pede uma resposta|o sistema calculou)[^.!?…]*[.!?…]?");

    private static final Pattern REPETITIVE_CRUTCH = Pattern.compile(
            "(?iu)(?:[,\n;]\\s*|\\s+|^)(?:o que (?:você|o grupo|vocês) faz(?:em)?\\??|o que você decide\\??|como (?:você|vocês) pretendem agir\\??)\\s*$");

    private static final Pattern AI_VOICE = Pattern.compile(
            "(?iu)\\b(?:como uma ia|como inteligência artificial|assistente virtual|prompt|pipeline|json)\\b");

    // Instruções operacionais não pertencem à ficção. Os padrões são deliberadamente
    // específicos: falar de uma missão ou de um mestre dentro do mundo é permitido.
    private static final Pattern INTERNAL_DIRECTIVES = Pattern.compile(
            "(?iu)(?:\\b(?:estado da campanha|dados persistidos|estado persistente|motor b[áa]rbara|motor barbara|"
            + "miss[ãa]o artificialmente acrescentada|missao artificialmente acrescentada|miss[ãa]o artificialmente|"
            + "aus[êe]ncia de um trilho|ausencia de um trilho|o sistema invente|o braseiro n[ãa]o transforma|"
            + "n[úu]meros invis[íi]veis|numeros invisiveis|sem identidades inventadas apenas para encher|"
            + "sem identidades inventadas|esses sinais n[ãa]o s[ãa]o automaticamente pistas|"
            + "o estado persistente realmente o transformou|"
            + "isso [ée] deliberado: conhecimento de cen[áa]rio|isso e deliberado: conhecimento de cenario|"
            + "conhecimento de cen[áa]rio ser[áa] conquistado em jogo|conhecimento de cenario sera conquistado em jogo|"
            + "o mundo n[ãa]o pausa enquanto essa decis[ãa]o|o mundo nao pausa enquanto essa decisao|"
            + "n[ãa]o inventa uma hist[óo]ria secreta para preencher sil[êe]ncio|"
            + "nao inventa uma historia secreta para preencher silencio|"
            + "nenhuma figura recebe import[âa]ncia artificial|nenhuma figura recebe importancia artificial|"
            + "n[ãa]o cria tais efeitos por decora[çc][ãa]o|nao cria tais efeitos por decoracao|"
            + "nenhuma amea[çc]a nova [ée] acrescentada apenas para terminar a introdu[çc][ãa]o|"
            + "nenhuma ameaca nova e acrescentada apenas para terminar a introducao|"
            + "a apresenta[çc][ãa]o n[ãa]o fabrica um novo|a apresentacao nao fabrica um novo|"
            + "sem precisar receber um mapa t[ée]cnico em voz alta|sem precisar receber um mapa tecnico em voz alta|"
            + "nenhum npc novo [ée] criado apenas para servir de guia|nenhum npc novo e criado apenas para servir de guia|"
            + "n[ãa]o porque o mestre decidiu recitar seu verbete|nao porque o mestre decidiu recitar seu verbete|"
            + "impede que uma descri[çc][ãa]o rica vire spoiler|impede que uma descricao rica vire spoiler|"
            + "n[ãa]o inventa enchentes, rios ou passagens inexistentes|nao inventa enchentes, rios ou passagens inexistentes|"
            + "sem acrescentar obst[áa]culos decorativos|sem acrescentar obstaculos decorativos|"
            + "mestre local evita c[óo]digos de mapa|mestre local evita codigos de mapa|"
            + "nenhum rio, lago, ponte ou travessia [ée] acrescentado quando a cartografia n[ãa]o os confirma|"
            + "nenhum rio, lago, ponte ou travessia e acrescentado quando a cartografia nao os confirma|"
            + "em vez de convite para inventar uma emboscada|"
            + "sem precisar abrir um atlas externo|"
            + "n[ãa]o inventa [áa]gua onde n[ãa]o h[áa] curso confirmado|nao inventa agua onde nao ha curso confirmado|"
            + "n[ãa]o transforma chuva comum em enchente|nao transforma chuva comum em enchente|"
            + "n[ãa]o inventa [áa]gua|nao inventa agua|"
            + "pessoas distantes n[ãa]o mudam de plano s[óo] porque a companhia viu o tempo fechar aqui|"
            + "pessoas distantes nao mudam de plano so porque a companhia viu o tempo fechar aqui|"
            + "n[ãa]o uma suposi[çc][ãa]o de que o jogador se lembra de um livro do cen[áa]rio|"
            + "nao uma suposicao de que o jogador se lembra de um livro do cenario|"
            + "o mestre n[ãa]o reescreve o lugar inteiro para parecer novo|o mestre nao reescreve o lugar inteiro para parecer novo|"
            + "a apresenta[çc][ãa]o n[ãa]o cria uma miss[ãa]o nova|a apresentacao nao cria uma missao nova|"
            + "a pr[óo]xima a[çc][ãa]o n[ãa]o [ée] decidida pelo mestre|a proxima acao nao e decidida pelo mestre|"
            + "o mestre preserva o que j[áa] est[áa] estabelecido|o mestre preserva o que ja esta estabelecido|"
            + "nenhum detalhe novo [ée] acrescentado apenas para preencher a descri[çc][ãa]o|"
            + "nenhum detalhe novo e acrescentado apenas para preencher a descricao|"
            + "quando houver uma escolha significativa, ela continua pertencendo aos jogadores|"
            + "o mestre local s[óo] confirma a geometria|o mestre local so confirma a geometria|"
            + "o mestre preserva o ponto da cena em vez de inventar uma resposta|"
            + "pessoas, posi[çc][õo]es e tens[õo]es n[ãa]o s[ãa]o substitu[íi]das por uma cena gen[ée]rica|"
            + "pessoas, posicoes e tensoes nao sao substituidas por uma cena generica|"
            + "o cen[áa]rio oferece detalhes observ[áa]veis em tempo real, sem decidir a pr[óo]xima a[çc][ãa]o pelo grupo|"
            + "o cenario oferece detalhes observaveis em tempo real, sem decidir a proxima acao pelo grupo|"
            + "a cena permanece honesta|autoridade do aplicativo|contexto operacional|"
            + "continu[íi]dade preservada no banco local|host_world_authority|"
            + "scene_state|npc_updates|ponto inicial catalogado desta aventura|"
            + "foco de regras para a abertura)\\b"
            + "|\\bo mestre (?:local|virtual)\\b"
            + "|\\bele estabelece o ch[ãa]o\\b|\\bele estabelece o chao\\b)");

    private static final Pattern EMOTION_HIJACK = Pattern.compile(
            "(?iu)\\b(?:você sente medo|você fica impressionado|você odeia|vocês sentem pavor)\\b");

    public static final class ValidationResult {
        public final boolean valid;
        public final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors == null ? Collections.<String>emptyList() : Collections.unmodifiableList(errors);
        }
    }

    public static boolean containsInternalLeak(String text) {
        return text != null && (INTERNAL_DIRECTIVES.matcher(text).find()
                || AI_VOICE.matcher(text).find());
    }

    public static String sanitize(String text) {
        if (text == null || text.trim().length() == 0) return "";
        String clean = text.replace("\r\n", "\n").replace('\r', '\n');

        // Remove rótulos de interface vazados na prosa
        clean = UI_LABELS.matcher(clean).replaceAll("");
        clean = SUGGESTION_HEADERS.matcher(clean).replaceAll("");

        // Não tente recortar um pedaço de instrução: descarte todo o parágrafo
        // contaminado, preservando parágrafos narrativos e sua pontuação.
        StringBuilder fictionalParagraphs = new StringBuilder();
        for (String paragraph : clean.split("\\n\\s*\\n", -1)) {
            if (containsInternalLeak(paragraph)) continue;
            if (paragraph.trim().length() == 0) continue;
            if (fictionalParagraphs.length() > 0) fictionalParagraphs.append("\n\n");
            fictionalParagraphs.append(paragraph.trim());
        }
        clean = fictionalParagraphs.toString();

        // Remove metalinguagem de software
        clean = SOFTWARE_METALANGUAGE.matcher(clean).replaceAll("");

        // Remove muletas repetitivas como "O que você faz?"
        clean = REPETITIVE_CRUTCH.matcher(clean).replaceAll("");

        // Normaliza espaçamento de parágrafos
        clean = clean.replaceAll("[ \t]+", " ")
                     .replaceAll(" *\n *", "\n")
                     .replaceAll("\n{3,}", "\n\n")
                     .trim();

        return clean;
    }

    public static ValidationResult validate(String text) {
        List<String> errors = new ArrayList<String>();
        if (text == null || text.trim().length() == 0) {
            return new ValidationResult(true, errors);
        }
        if (AI_VOICE.matcher(text).find()) {
            errors.add("Voz de IA/meta detectada.");
        }
        if (INTERNAL_DIRECTIVES.matcher(text).find()) {
            errors.add("Instruções internas do Mestre detectadas no texto público.");
        }
        if (EMOTION_HIJACK.matcher(text).find()) {
            errors.add("Sequestro de emoção do herói detectado.");
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
