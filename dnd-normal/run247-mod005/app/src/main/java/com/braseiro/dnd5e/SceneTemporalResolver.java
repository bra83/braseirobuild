package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Resolve horário, rótulo e iluminação de uma cena a partir de uma única
 * referência canônica. A narração só altera o relógio quando contém um marcador
 * temporal explícito e forte, evitando que lembranças como "a noite anterior"
 * mudem acidentalmente o período atual.
 */
public final class SceneTemporalResolver {
    private SceneTemporalResolver() {}

    public static final class Result {
        public final double hour;
        public final String label;
        public final String lighting;
        public final boolean narrationWasAuthoritative;

        Result(double hour, String label, String lighting, boolean narrationWasAuthoritative) {
            this.hour = normalizeHour(hour);
            this.label = label;
            this.lighting = lighting;
            this.narrationWasAuthoritative = narrationWasAuthoritative;
        }
    }

    public static Result resolve(
            String storedLabel,
            double storedHour,
            boolean hasStoredHour,
            String proposedLabel,
            double proposedHour,
            boolean hasProposedHour,
            String narration,
            String existingLighting) {
        double baseHour;
        if (hasProposedHour && Double.isFinite(proposedHour)) baseHour = normalizeHour(proposedHour);
        else if (hasStoredHour && Double.isFinite(storedHour)) baseHour = normalizeHour(storedHour);
        else if (notBlank(proposedLabel)) baseHour = hourForLabel(proposedLabel);
        else baseHour = hourForLabel(storedLabel);

        double narrativeHour = inferExplicitHour(narration);
        boolean narrationAuthoritative = !Double.isNaN(narrativeHour);
        double resolvedHour = narrationAuthoritative ? narrativeHour : baseHour;
        String label = labelForHour(resolvedHour);
        String lighting = resolveLighting(label, narration, existingLighting);
        return new Result(resolvedHour, label, lighting, narrationAuthoritative);
    }

    /**
     * Descriptions such as "o ar da manhã" or "sob o luar" are ambience,
     * NOT time updates. Only an explicit clock reading or an actual
     * transition can override the persisted time.
     */
    public static double inferExplicitHour(String narration) {
        if(narration==null||narration.trim().isEmpty())return Double.NaN;
        String lower=Normalizer.normalize(narration.toLowerCase(new Locale("pt","BR")),
                Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        java.util.regex.Matcher exact=java.util.regex.Pattern.compile(
                "(?:\\b(?:o\\s+relogio\\s+marca|agora\\s+sao|sao)\\s+)([01]?\\d|2[0-3])(?:\\s*[:h]\\s*([0-5]\\d))?(?:\\s*horas?)?\\b"
        ).matcher(lower);
        double time=Double.NaN;
        while(exact.find()){
            int h=Integer.parseInt(exact.group(1));
            // An isolated "as 3" in prose is not a reliable clock. Require
            // a clock separator or explicit "horas".
            String phrase=exact.group();
            if(phrase.matches(".*(?:[:h]|horas?).*") || lower.contains("agora sao") || lower.contains("o relogio marca"))
                time=h+(exact.group(2)==null?0:Integer.parseInt(exact.group(2)))/60d;
        }
        if(!Double.isNaN(time))return time;
        String text=normalize(narration);
        if(containsAny(text,"ontem","no dia anterior","na noite anterior","na manha anterior","lembrava se"))return Double.NaN;
        if(containsAny(text,"o dia amanhece","o dia amanheceu","ao amanhecer seguinte"))return 6d;
        if(containsAny(text,"anoitece agora","a noite cai","o sol se poe","o sol se pos"))return 18d;
        return Double.NaN;
    }

    public static double hourForLabel(String label) {
        String value = normalize(label);
        if (value.contains("madrugada")) return 3d;
        if (value.contains("amanhe") || value.contains("alvorada")) return 6d;
        if (value.contains("manha")) return 9d;
        if (value.contains("meio dia")) return 12d;
        if (value.contains("tarde") && !value.contains("fim da tarde")) return 15d;
        if (value.contains("entard") || value.contains("crepusc") || value.contains("fim da tarde")) return 18d;
        if (value.contains("noite")) return 21d;
        return 9d;
    }

    public static String labelForHour(double hour) {
        double h = normalizeHour(hour);
        if (h < 5d) return "Madrugada";
        if (h < 8d) return "Amanhecer";
        if (h < 12d) return "Manhã";
        if (h < 14d) return "Meio-dia";
        if (h < 18d) return "Tarde";
        if (h < 20d) return "Entardecer";
        return "Noite";
    }

    public static String resolveLighting(String label, String narration, String existingLighting) {
        String text = normalize(narration);
        String current = existingLighting == null ? "" : existingLighting.trim();
        String currentNormalized = normalize(current);

        if (containsAny(text, "luz dourada da manha", "dourada luz da manha"))
            return "Luz dourada da manhã";
        if (containsAny(text, "luz do amanhecer", "primeiros raios", "alvorada"))
            return "Luz suave do amanhecer";
        if (containsAny(text, "tocha", "tochas", "lareira", "fogueira", "lampiao", "lamparina"))
            return "Luz de fogo";
        if (containsAny(text, "sob o luar", "luz da lua", "luar"))
            return "Luar";

        String normalizedLabel = normalize(label);
        if (current.length() > 0 && !lightingContradicts(currentNormalized, normalizedLabel)) return current;
        if (normalizedLabel.contains("madrugada")) return "Luz baixa da madrugada";
        if (normalizedLabel.contains("amanhe")) return "Luz suave do amanhecer";
        if (normalizedLabel.contains("manha")) return "Luz natural da manhã";
        if (normalizedLabel.contains("meio dia")) return "Luz forte do meio-dia";
        if (normalizedLabel.contains("tarde")) return "Luz natural da tarde";
        if (normalizedLabel.contains("entard")) return "Luz quente do entardecer";
        return "Luz noturna";
    }

    public static boolean isNight(String label) {
        String value = normalize(label);
        return value.contains("noite") || value.contains("madrugada");
    }

    private static boolean lightingContradicts(String lighting, String label) {
        if (lighting.length() == 0) return false;
        if (lighting.contains("fogo") || lighting.contains("tocha") || lighting.contains("lareira") || lighting.contains("lamp"))
            return false;
        if (label.contains("noite") || label.contains("madrugada"))
            return containsAny(lighting, "manha", "amanhe", "meio dia", "tarde", "entard", "sol");
        if (label.contains("manha") || label.contains("amanhe"))
            return containsAny(lighting, "noite", "luar", "lua", "madrugada", "entard");
        if (label.contains("tarde") || label.contains("meio dia"))
            return containsAny(lighting, "noite", "luar", "lua", "madrugada", "amanhe", "manha");
        if (label.contains("entard"))
            return containsAny(lighting, "manha", "amanhe", "meio dia", "noite", "madrugada");
        return false;
    }

    private static Marker choose(Marker current, String text, double hour, int weight, String... phrases) {
        for (String phrase : phrases) {
            int index = text.lastIndexOf(phrase);
            if (index < 0) continue;
            Marker candidate = new Marker(hour, weight, index);
            if (current == null || candidate.weight > current.weight ||
                    (candidate.weight == current.weight && candidate.index > current.index)) current = candidate;
        }
        return current;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static boolean notBlank(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static double normalizeHour(double hour) {
        double value = hour % 24d;
        return value < 0d ? value + 24d : value;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private static final class Marker {
        final double hour;
        final int weight;
        final int index;

        Marker(double hour, int weight, int index) {
            this.hour = hour;
            this.weight = weight;
            this.index = index;
        }
    }
}
