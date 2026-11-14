package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Conservative elapsed-time estimates for completed sandbox actions.
 * Only the host can commit the returned duration, after a valid master response.
 * This policy never settles checks, grants items or moves map tokens.
 */
public final class SandboxActionDuration {
    private SandboxActionDuration() {}

    public static final class Estimate {
        public final double hours;
        public final String category;
        public final String source;
        private Estimate(double hours, String category) {
            this.hours = hours;
            this.category = category;
            this.source = hours > 0 ? "sandbox_action_estimate" : "unresolved_or_nonadvancing";
        }
    }

    public static Estimate estimate(String action) {
        String text = normalize(action);
        if (text.isEmpty() || isNonAdvancing(text, action)) return none();
        // Travel is measured by TravelRulesEngine from map distance/time; do not guess it here.
        if (contains(text, "viajo", "viajamos", "viagem", "caminho ate", "caminhamos ate", "sigo ate", "vou ate", "desloco ate", "marchar", "marcha por", "atravesso a regiao")) return none();
        if (contains(text, "procuro trabalho", "busco trabalho", "procurar emprego", "procuro emprego", "busco emprego", "procuro contrato", "busco contrato", "quadro de avisos", "ofertas de trabalho", "procuro um bico", "alguem que esteja contratando", "quem esteja contratando", "contratando aventureiros")) return new Estimate(1d, "procurar_trabalho");
        if (contains(text, "cacar", "caco", "caca na", "caca por", "pesc", "forrage", "procurar comida", "busco alimento", "busco agua", "recolho alimento", "rastreio animais")) return new Estimate(2d, "caca_coleta");
        if (contains(text, "trabalho como", "trabalho na", "trabalho no", "faço um turno", "faco um turno", "cumpro o turno", "cumpro meu turno", "presto servico", "presto servicos")) return new Estimate(4d, "jornada_trabalho");
        if (contains(text, "patrulho", "patrulhamos", "faco patrulha", "faço patrulha", "vigio a", "vigio o", "montamos guarda")) return new Estimate(2d, "patrulha");
        if (contains(text, "exploro", "exploramos", "investigo", "investigamos", "vasculho", "vasculhamos", "rastreio", "sigo as pistas", "procuro pistas", "procuro rastros", "examino os arredores", "inspeciono", "revisto o")) return new Estimate(1d,"exploracao_investigacao");
        if (contains(text, "colho", "coletamos", "coletar", "recolho", "recolhemos", "corto lenha", "busco lenha")) return new Estimate(1d, "coleta");
        if (contains(text, "compro", "compramos", "vendo meus", "vendo as", "vendo os", "negoceio", "negocio o", "negocio a", "barganho")) return new Estimate(.5d, "comercio");
        if (contains(text, "converso com", "conversamos com", "pergunto a", "pergunto ao", "pergunto para", "entrevisto", "interrogo")) return new Estimate(.25d, "conversa");
        if (contains(text, "observo", "observamos", "examino", "examinar", "olho em volta", "procuro sinais", "escuto atentamente")) return new Estimate(.10d, "observacao");
        return none();
    }

    private static boolean isNonAdvancing(String normalized, String original) {
        if (original != null && original.indexOf('?') >= 0) return true;
        if (contains(normalized, "nao quero", "nao vou", "nao faco", "nao pretendo", "sem fazer", "e se eu", "o que acontece", "quanto tempo", "qual seria", "posso fazer", "poderia", "quais sao", "me explique", "consulte as regras")) return true;
        return normalized.startsWith("mestre ") || normalized.startsWith("pergunta ") || normalized.startsWith("consultar ");
    }

    private static boolean contains(String s, String... phrases) {
        String padded = " " + s + " ";
        for (String phrase : phrases) {
            String normalized = normalize(phrase);
            if (padded.contains(" " + normalized + " ")) return true;
            // Explicit verb roots intended only for activities, e.g., pescar/pesco/pescamos.
            if ((phrase.equals("pesc") || phrase.equals("forrage")) && Pattern.compile("\\b"+phrase+"[a-z]*\\b").matcher(s).find()) return true;
        }
        return false;
    }
    private static String normalize(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }
    private static Estimate none() { return new Estimate(0d, "none"); }
}