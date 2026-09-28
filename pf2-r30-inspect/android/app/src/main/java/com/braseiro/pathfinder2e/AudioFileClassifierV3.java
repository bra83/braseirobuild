package com.braseiro.pathfinder2e;

import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

/** Classificação inicial; o catálogo JSON e o motor semântico refinam depois. */
public final class AudioFileClassifierV3 {
    private AudioFileClassifierV3() { }

    public static JSONObject classify(String name, long size, long durationMs) {
        String n = fold(name);
        String type;
        if (contains(n, "music", "musica", "trilha", "bgm", "score", "theme", "song")) type = "music";
        else if (contains(n, "ambience", "ambiencia", "atmosphere", "soundscape", "weather", "rain", "chuva", "wind", "vento", "forest", "floresta", "tavern", "taverna", "city", "cidade", "cave", "caverna")) type = "ambience";
        else type = "sfx";
        boolean weather = contains(n, "weather", "rain", "chuva", "storm", "tempestade", "wind", "vento", "snow", "neve", "thunder", "trovao");
        boolean loop = !"sfx".equals(type);
        if (durationMs > 0 && durationMs > 14000 && "sfx".equals(type)) type = "unknown";
        JSONObject out = new JSONObject();
        try {
            out.put("type", type);
            out.put("catalog_channel", type);
            out.put("catalog_role", "music".equals(type) ? "music" : (weather ? "climate" : ("ambience".equals(type) ? "base" : "sfx")));
            out.put("loopable", loop);
            out.put("duration_ms", Math.max(0L, durationMs));
            out.put("size", Math.max(0L, size));
            out.put("keywords", keywords(n));
            out.put("variant_group", group(n, type, weather));
        } catch(Exception ignored){AppLog.ignored("AudioFileClassifierV3","Falha tolerada; a execução continuou.",ignored);}
        return out;
    }

    private static String group(String n, String type, boolean weather) {
        if (weather) {
            if (contains(n, "rain", "chuva", "drizzle", "garoa")) return "weather_rain";
            if (contains(n, "storm", "tempestade", "thunder", "trovao")) return "weather_storm";
            if (contains(n, "snow", "neve")) return "weather_snow";
            return "weather_wind";
        }
        if ("music".equals(type)) {
            if (contains(n, "battle", "combat", "tense", "dark")) return "music_tense";
            if (contains(n, "mystery", "misterio", "secret")) return "music_mystery";
            if (contains(n, "calm", "peace", "chill")) return "music_calm";
            return "music_adventure";
        }
        if ("ambience".equals(type)) {
            if (contains(n, "tavern", "taverna", "inn")) return "ambience_tavern";
            if (contains(n, "forest", "floresta", "woods")) return "ambience_forest";
            if (contains(n, "city", "cidade", "town", "village")) return "ambience_town";
            if (contains(n, "cave", "caverna", "dungeon")) return "ambience_underground";
            return "ambience_generic";
        }
        if (contains(n, "knock", "porta", "door")) return "sfx_door";
        if (contains(n, "footstep", "passos", "step")) return "sfx_footsteps";
        if (contains(n, "sword", "blade", "espada")) return "sfx_blade";
        if (contains(n, "anvil", "bigorna", "hammer", "martelo")) return "sfx_anvil";
        if (contains(n, "cart", "wagon", "carroca", "carruagem")) return "sfx_cart";
        return "sfx_generic";
    }

    private static String keywords(String n) { return n.replaceAll("[^a-z0-9]+", ",").replaceAll("^,+|,+$", ""); }
    private static boolean contains(String n, String... values) { for (String value : values) if (n.contains(value)) return true; return false; }
    private static String fold(String value) {
        String n = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim();
    }
}
