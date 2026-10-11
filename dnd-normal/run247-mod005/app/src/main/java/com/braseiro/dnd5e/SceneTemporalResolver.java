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
        if (hasProposedHour) baseHour = normalizeHour(proposedHour);
        else if (notBlank(proposedLabel)) baseHour = hourForLabel(proposedLabel);
        else if (hasStoredHour) baseHour = normalizeHour(storedHour);
        else baseHour = hourForLabel(storedLabel);

        double narrativeHour = inferExplicitHour(narration);
        boolean narrationAuthoritative = !Double.isNaN(narrativeHour);
        double resolvedHour = narrationAuthoritative ? narrativeHour : baseHour;
        String label = labelForHour(resolvedHour);
        String lighting = resolveLighting(label, narration, existingLighting);
        return new Result(resolvedHour, label, lighting, narrationAuthoritative);
    }

    public static double inferExplicitHour(String narration) {
        String text = normalize(narration);
        if (text.length() == 0) return Double.NaN;
        Marker best = null;

        best = choose(best, text, 3d, 9,
                "em plena madrugada", "durante a madrugada", "na madrugada silenciosa");
        best = choose(best, text, 3d, 6, "na madrugada", "antes do amanhecer");

        best = choose(best, text, 6d, 10,
                "luz dourada do amanhecer", "luz do amanhecer", "ao amanhecer");
        best = choose(best, text, 6d, 9,
                "o dia amanhece", "o dia amanheceu", "amanheceu", "alvorada");
        best = choose(best, text, 6d, 8,
                "primeiros raios", "sol nascente", "nascer do sol");

        best = choose(best, text, 9d, 12,
                "luz dourada da manha", "dourada luz da manha");
        best = choose(best, text, 9d, 10,
                "luz da manha", "sol da manha", "claridade da manha", "luz matinal");
        best = choose(best, text, 9d, 8,
                "pela manha", "durante a manha", "nesta manha", "esta manha", "na manha seguinte");
        best = choose(best, text, 9d, 7, "na manha", "manha clara");

        best = choose(best, text, 12d, 10,
                "ao meio dia", "meio dia", "sol a pino");

        best = choose(best, text, 15d, 9,
                "luz da tarde", "sol da tarde", "durante a tarde", "pela tarde");
        best = choose(best, text, 15d, 7, "nesta tarde", "na tarde");

        best = choose(best, text, 18d, 11,
                "luz do entardecer", "ao entardecer", "por do sol");
        best = choose(best, text, 18d, 9,
                "o sol se poe", "o sol se pos", "crepusculo", "fim da tarde");

        best = choose(best, text, 21d, 11,
                "a noite cai", "anoitece", "anoiteceu", "em plena noite");
        best = choose(best, text, 21d, 10,
                "durante a noite", "sob o luar", "ceu estrelado", "luz da lua");
        best = choose(best, text, 21d, 8,
                "nesta noite", "naquela noite", "noite cerrada");

        return best == null ? Double.NaN : best.hour;
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
