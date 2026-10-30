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
            "(?iu)(?:\\b(?:estado da campanha|dados persistidos|motor bárbara|motor barbara|"
            + "missão artificialmente acrescentada|missao artificialmente acrescentada|"
            + "ausência de um trilho|ausencia de um trilho|o sistema invente|"
            + "a cena permanece honesta|autoridade do aplicativo|contexto operacional|"
            + "continu[íi]dade preservada no banco local|host_world_authority|"
            + "scene_state|npc_updates|ponto inicial catalogado desta aventura|"
            + "foco de regras para a abertura)\\b"
            + "|\\bo mestre (?:local|virtual)\\b[^.!?\\n]{0,180}"
            + "\\b(?:move|movimenta|teletransporta|automaticamente)\\b"
            + "|\\bele estabelece o chão\\b)");

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
