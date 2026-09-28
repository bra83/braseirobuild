package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mantém códigos cartográficos na interface do mapa, mas os transforma em
 * referências naturais dentro da prosa narrativa.
 */
public final class NarrativeLocationNaturalizer {
    private static final Pattern HEX_ID = Pattern.compile("(?<![A-Za-z0-9])([A-Z]{1,3}[0-9]{1,4})(?![A-Za-z0-9])");
    private static final Pattern GENERIC_AT_CURRENT = Pattern.compile("(?iu)\\b(?:no|neste|nesse)\\s+(?:hex(?:ágono)?)(?:\\s+[A-Z]{1,3}[0-9]{1,4})?(?:\\s+(?:nesta|nessa)\\s+área)?\\b");
    private static final Pattern GENERIC_FROM_CURRENT = Pattern.compile("(?iu)\\b(?:do|deste|desse)\\s+(?:hex(?:ágono)?)(?:\\s+[A-Z]{1,3}[0-9]{1,4})?\\b");
    private static final Pattern GENERIC_SUBJECT = Pattern.compile("(?iu)\\b(?:o|este|esse)\\s+(?:hex(?:ágono)?)(?:\\s+[A-Z]{1,3}[0-9]{1,4})?\\b");
    private static final Pattern GENERIC_BARE = Pattern.compile("(?iu)\\bhex(?:ágono)?(?:\\s+[A-Z]{1,3}[0-9]{1,4})?\\b");

    private NarrativeLocationNaturalizer() { }

    public static String naturalize(String text, AppDb db) {
        if (text == null || text.length() == 0 || db == null) return text == null ? "" : text;
        JSONObject state = db.getMapState();
        String packageId = state.optString("map_package_id", "");
        String currentId = state.optString("current_hex_id", "");
        JSONObject current = packageId.length() == 0 || currentId.length() == 0 ? new JSONObject() : db.getMapHex(packageId, currentId);
        CurrentReference reference = currentReference(db, packageId, currentId, current);

        String normalized = replace(GENERIC_AT_CURRENT, text, reference.at);
        normalized = replace(GENERIC_FROM_CURRENT, normalized, reference.from);
        normalized = replace(GENERIC_SUBJECT, normalized, reference.subject);
        normalized = replace(GENERIC_BARE, normalized, reference.bare);

        if (packageId.length() == 0) return finish(normalized);
        Matcher matcher = HEX_ID.matcher(normalized);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String id = matcher.group(1);
            if (isNonMapContext(normalized, matcher.start())) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(id));
                continue;
            }
            if (id.equalsIgnoreCase(currentId)) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(reference.bare));
                continue;
            }
            JSONObject target = db.getMapHex(packageId, id);
            if (target.length() == 0) {
                // Um código desconhecido nunca deve aparecer como geografia na prosa.
                matcher.appendReplacement(out, Matcher.quoteReplacement("a região indicada"));
                continue;
            }
            String replacement = describeTarget(id, target, currentId, current, state, db, packageId);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return finish(out.toString());
    }

    /** Texto curto que pode ser entregue ao prompt sem expor o código do mapa. */
    public static String currentNarrativeLabel(AppDb db) {
        if (db == null) return "esta região";
        JSONObject state = db.getMapState();
        String packageId = state.optString("map_package_id", "");
        String currentId = state.optString("current_hex_id", "");
        JSONObject current = packageId.length() == 0 || currentId.length() == 0 ? new JSONObject() : db.getMapHex(packageId, currentId);
        return currentReference(db, packageId, currentId, current).subject;
    }

    private static CurrentReference currentReference(AppDb db, String packageId, String currentId, JSONObject current) {
        String named = confirmedLocationName(db, packageId, currentId);
        if (named.length() > 0) return CurrentReference.proper(named);

        // A anotação explícita do jogador tem precedência sobre inferências do
        // atlas e sobre o local sugerido por uma cena gerada.
        if (packageId.length() > 0 && currentId.length() > 0) {
            JSONObject note = db.getMapHexNote(packageId, currentId);
            String title = cleanNarrativeLocation(note.optString("title", note.optString("note_title", "")));
            if (title.length() > 0) return CurrentReference.proper(title);
        }

        String political = politicalLabel(current.optString("political_json", ""));
        if (political.length() > 0) return CurrentReference.region(political);

        String terrain = reliableTerrainLabel(current);
        if (terrain.length() > 0) return CurrentReference.terrain(terrain);

        // Sem mapa ativo, a localização da cena ainda é uma referência útil.
        // Com mapa ativo e atlas não confirmado (como EH92), ela não pode
        // sobrepor o contexto cartográfico e inventar geografia.
        if (packageId.length() == 0 || currentId.length() == 0) {
            JSONObject scene = db.getSceneState();
            String sceneLocation = cleanNarrativeLocation(scene.optString("location", ""));
            if (sceneLocation.length() > 0) return CurrentReference.place(sceneLocation);
        }
        return CurrentReference.fallback();
    }

    private static String confirmedLocationName(AppDb db, String packageId, String id) {
        if (packageId == null || packageId.length() == 0 || id == null || id.length() == 0) return "";
        List<JSONObject> locations = db.getMapLocationsForHex(packageId, id);
        for (JSONObject location : locations) {
            String status = location.optString("placement_status", "").toLowerCase(Locale.ROOT);
            if (status.contains("review") || status.contains("approx") || status.contains("unassigned")) continue;
            String name = cleanNarrativeLocation(location.optString("name", ""));
            if (name.length() > 0) return name;
        }
        return "";
    }

    private static String describeTarget(String id, JSONObject target, String currentId, JSONObject current,
                                         JSONObject state, AppDb db, String packageId) {
        String named = confirmedLocationName(db, packageId, id);
        String terrain = reliableTerrainLabel(target);
        if (current.length() == 0) return named.length() > 0 ? named : terrain.length() > 0 ? articleFor(terrain) + terrain : "a região indicada";
        double dx = target.optDouble("center_x") - current.optDouble("center_x");
        double dy = target.optDouble("center_y") - current.optDouble("center_y");
        String direction = direction(dx, dy);
        double grid = Math.max(1d, state.optDouble("grid_size", 64d));
        int steps = Math.max(1, (int)Math.round(Math.hypot(dx, dy) / grid));
        String base = named.length() > 0 ? named : terrain.length() > 0 ? articleFor(terrain) + terrain : "a região";
        if (steps <= 1) return base + " ao " + direction;
        return base + ", cerca de " + steps + " trechos de viagem ao " + direction;
    }

    private static String terrainLabel(JSONObject target) { return reliableTerrainLabel(target); }

    private static String reliableTerrainLabel(JSONObject target) {
        if (target == null || target.length() == 0) return "";
        String status = target.optString("review_status", "").toLowerCase(Locale.ROOT);
        double confidence = target.optDouble("terrain_confidence", -1d);
        String raw = target.optString("terrain_primary", target.optString("terrain", ""));
        if (raw == null) return "";
        String n = raw.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
        if (n.length() == 0 || n.equals("null") || n.equals("unknown") || n.equals("unconfirmed") || n.equals("área") || n.equals("area")) return "";
        if ((status.contains("needs_review") || status.contains("unconfirmed")) && confidence <= 0d) return "";
        if (n.contains("forest") || n.contains("wood") || n.contains("floresta")) return "floresta";
        if (n.contains("plain") || n.contains("grass") || n.contains("field") || n.contains("open") || n.contains("planície") || n.contains("planicie")) return "planície";
        if (n.contains("hill") || n.contains("colina")) return "colinas";
        if (n.contains("mount") || n.contains("montanha")) return "montanha";
        if (n.contains("swamp") || n.contains("marsh") || n.contains("pântano") || n.contains("pantano")) return "pântano";
        if (n.contains("desert") || n.contains("deserto")) return "deserto";
        if (n.contains("road") || n.contains("estrada")) return "estrada";
        if (n.contains("river") || n.contains("rio")) return "rio";
        if (n.contains("lake") || n.contains("lago")) return "lago";
        if (n.contains("sea") || n.contains("ocean") || n.contains("water") || n.contains("mar") || n.contains("água") || n.contains("agua")) return "costa";
        return n.length() <= 32 ? n : "";
    }

    private static String politicalLabel(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.length() == 0 || value.equals("{}") || value.equals("[]") || value.equalsIgnoreCase("null")) return "";
        try {
            Object parsed = value.startsWith("[") ? new JSONArray(value) : new JSONObject(value);
            return findPoliticalName(parsed, 0);
        } catch (Exception ignored) {
            return cleanNarrativeLocation(value);
        }
    }

    private static String findPoliticalName(Object value, int depth) {
        if (value == null || depth > 3) return "";
        if (value instanceof String) return cleanNarrativeLocation(String.valueOf(value));
        if (value instanceof JSONArray) {
            JSONArray a = (JSONArray)value;
            for (int i=0;i<a.length();i++) { String found=findPoliticalName(a.opt(i),depth+1);if(found.length()>0)return found; }
            return "";
        }
        if (value instanceof JSONObject) {
            JSONObject o=(JSONObject)value;
            String[] keys={"name","nome","region","regiao","região","realm","kingdom","nation","territory","label"};
            for(String key:keys){String found=cleanNarrativeLocation(o.optString(key,""));if(found.length()>0)return found;}
            java.util.Iterator<String> it=o.keys();while(it.hasNext()){String found=findPoliticalName(o.opt(it.next()),depth+1);if(found.length()>0)return found;}
        }
        return "";
    }

    private static String cleanNarrativeLocation(String raw) {
        if (raw == null) return "";
        String value=PortugueseNarrationSanitizer.sanitize(raw).replaceAll("\\s+"," ").trim();
        String folded=value.toLowerCase(Locale.ROOT);
        if (value.length()<2 || folded.equals("null") || folded.equals("área") || folded.equals("area") || folded.equals("local") || folded.equals("local não definido") || folded.equals("não definido")) return "";
        if (folded.matches(".*\\bhex(?:ágono)?\\b.*") || folded.matches("[a-z]{1,3}[0-9]{1,4}")) return "";
        return value.length()>80?value.substring(0,80).trim():value;
    }

    private static boolean isNonMapContext(String text, int start) {
        int from = Math.max(0, start - 48);
        String before = text.substring(from, start).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return before.matches(".*\\b(sala|quarto|tabela|pagina|página|capitulo|capítulo|item|codigo|código|arquivo|modelo)\\s*$")
                || before.matches(".*\\b(dado|role|rolagem|teste com)\\s+(um |o )?$");
    }

    private static String articleFor(String terrain) {
        String n = terrain.toLowerCase(Locale.ROOT);
        if (n.equals("montanha") || n.equals("estrada") || n.equals("floresta") || n.equals("planície") || n.equals("costa")) return "na ";
        if (n.equals("colinas")) return "nas ";
        return "no ";
    }

    private static String direction(double dx, double dy) {
        double angle = Math.atan2(-dy, dx);
        double octant = Math.round(8d * angle / (2d * Math.PI) + 8d) % 8d;
        switch ((int)octant) {
            case 0: return "leste";
            case 1: return "nordeste";
            case 2: return "norte";
            case 3: return "noroeste";
            case 4: return "oeste";
            case 5: return "sudoeste";
            case 6: return "sul";
            default: return "sudeste";
        }
    }

    private static String replace(Pattern pattern,String text,String replacement){
        Matcher matcher=pattern.matcher(text);StringBuffer out=new StringBuffer();while(matcher.find())matcher.appendReplacement(out,Matcher.quoteReplacement(replacement));matcher.appendTail(out);return out.toString();
    }

    private static String finish(String value){
        return PortugueseNarrationSanitizer.sanitize(value).replaceAll("(?iu)\\b(?:nesta|nessa) região nesta área\\b","nesta região").replaceAll(" {2,}"," ").trim();
    }

    private static final class CurrentReference {
        final String at,from,subject,bare;
        CurrentReference(String at,String from,String subject,String bare){this.at=at;this.from=from;this.subject=subject;this.bare=bare;}
        static CurrentReference proper(String name){return new CurrentReference("em "+name,"de "+name,name,name);}
        static CurrentReference region(String name){return new CurrentReference("na região de "+name,"da região de "+name,"a região de "+name,"região de "+name);}
        static CurrentReference terrain(String terrain){
            String article=articleFor(terrain);String at=("nas ".equals(article)?"nestas ":"na ".equals(article)?"nesta ":"neste ")+terrain;
            String from=("nas ".equals(article)?"destas ":"na ".equals(article)?"desta ":"deste ")+terrain;
            String subject=("nas ".equals(article)?"estas ":"na ".equals(article)?"esta ":"este ")+terrain;
            return new CurrentReference(at,from,subject,terrain);
        }
        static CurrentReference place(String value){
            String lower=value.toLowerCase(Locale.ROOT);
            if(lower.startsWith("na ")||lower.startsWith("no ")||lower.startsWith("nas ")||lower.startsWith("nos ")||lower.startsWith("em ")||lower.startsWith("nesta ")||lower.startsWith("neste "))return new CurrentReference(value,"desse local",value.replaceFirst("(?iu)^(na|no|nas|nos|em)\\s+",""),value.replaceFirst("(?iu)^(na|no|nas|nos|em|nesta|neste)\\s+",""));
            if(lower.startsWith("a "))return new CurrentReference("na "+value.substring(2),"da "+value.substring(2),value,value.substring(2));
            if(lower.startsWith("o "))return new CurrentReference("no "+value.substring(2),"do "+value.substring(2),value,value.substring(2));
            return proper(value);
        }
        static CurrentReference fallback(){return new CurrentReference("nesta região","desta região","esta região","região");}
    }
}
