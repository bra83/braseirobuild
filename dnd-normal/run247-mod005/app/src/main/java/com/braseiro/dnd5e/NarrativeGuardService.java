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


    /*
     * Gate for instructional prose accidentally emitted by the model as story.
     * Detection is paragraph-scoped: a legitimate character saying "o mundo"
     * or "personagens" is not enough to trigger it. Separate internal context
     * from what a character could have seen or heard.
     */
    private static final Pattern DIRECTIVE_LEAK = Pattern.compile(
        "(?iu)(?:quando um novo reino,?\\s*templo,?\\s*povo|"
        +"dever[aá] ganhar contexto no momento em que importar|"
        +"ser despejado como uma sequ[eê]ncia de nomes|"
        +"o presente imediato,? ent[aã]o,? ganha prioridade|"
        +"a companhia pode interromper uma rotina|"
        +"o mundo n[aã]o pausa enquanto essa decis[aã]o|"
        +"[eé] nesse ponto que a apresenta[cç][aã]o termina|"
        +"sem fingir conhecimento que ningu[eé]m recebeu|"
        +"contexto suficiente para agir sem fingir conhecimento|"
        +"foco de regras para a abertura|"
        +"refer[eê]ncia do cat[aá]logo:|"
        +"ponto inicial catalogado desta aventura|"
        +"scene_state\\.present_npcs|host_world_authority|duplicate_gemini_call)"
    );

    private static final Pattern RULE_META = Pattern.compile(
        "(?iu)(?:^|[.!?]\\s+)(?:o mestre deve|a narra[cç][aã]o deve|"
        +"a cena deve|n[aã]o revele|n[aã]o antecipe|n[aã]o force|"
        +"n[aã]o invente|deve ser apresentado|dever[aá] aparecer|"
        +"contextualize |descreva primeiro|priorize o presente imediato)"
    );

    public static boolean containsInstructionLeak(String text) {
        if(text==null||text.trim().isEmpty())return false;
        // Strong signatures from real incident reports.
        if(DIRECTIVE_LEAK.matcher(text).find())return true;
        // A weaker imperative alone isn't proof; require another instruction
        // signal or a clear rule-like imperative, not normal dialogue.
        String lower=text.toLowerCase(java.util.Locale.ROOT);
        return RULE_META.matcher(text).find()
            && (lower.contains("jogador")||lower.contains("narrativa")
                ||lower.contains("campanha")||lower.contains("personagem")
                ||lower.contains("mestre")||lower.contains("roteiro"));
    }

    public static String removeInstructionLeaks(String text) {
        if(text==null||text.trim().isEmpty())return "";
        String[] paragraphs=text.replace("\r\n","\n").replace('\r','\n')
            .split("\\n\\s*\\n",-1);
        StringBuilder kept=new StringBuilder();
        for(String paragraph:paragraphs){
            if(paragraph.trim().isEmpty()||containsInstructionLeak(paragraph))continue;
            if(kept.length()>0)kept.append("\n\n");
            kept.append(paragraph.trim());
        }
        return kept.toString();
    }

    public static String sanitize(String text) {
        if (text == null || text.trim().length() == 0) return "";
        String clean = removeInstructionLeaks(text);

        // Remove rótulos de interface vazados na prosa
        clean = UI_LABELS.matcher(clean).replaceAll("");
        clean = SUGGESTION_HEADERS.matcher(clean).replaceAll("");

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
        if (containsInstructionLeak(text)) {errors.add("Diretrizes internas vazaram para a narração.");}
        if (AI_VOICE.matcher(text).find()) {
            errors.add("Voz de IA/meta detectada.");
        }
        if (EMOTION_HIJACK.matcher(text).find()) {
            errors.add("Sequestro de emoção do herói detectado.");
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
