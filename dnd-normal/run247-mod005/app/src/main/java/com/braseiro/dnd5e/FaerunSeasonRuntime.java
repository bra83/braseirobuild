package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Read-only, demand-driven season overlay for canonical Faerun places.
 *
 * A season is NOT a rule engine or a completed quest. It cannot move tokens,
 * spawn NPCs, grant rewards, change clock/HP/XP, roll a test, force a hook,
 * authorize a combatant, or imply that an event already took place.
 *
 * Locations must be resolved in the INSTALLED canonical Atlas; no guessed
 * hex, coordinates, or map geometry are ever accepted. Mystara and the
 * Strixhaven/Arcavios profile are explicitly excluded.
 */
public final class FaerunSeasonRuntime {
    public static final String ASSET =
        "faerun_seasons/temporada_01_estrada_longa.json";
    public static final String SCHEMA = "braseiro.faerun.season/1";
    private static volatile JSONObject cached;
    private FaerunSeasonRuntime() {}

    public static boolean eligible(String worldProfile, boolean strixhavenActive) {
        return !strixhavenActive && "dnd".equalsIgnoreCase(
            worldProfile == null ? "" : worldProfile.trim());
    }

    public static JSONObject loadAsset(Context context) throws Exception {
        if (context == null) throw new IllegalStateException("Android application context unavailable");
        JSONObject value = cached;
        if (value == null) {
            synchronized (FaerunSeasonRuntime.class) {
                value = cached;
                if (value == null) {
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(context.getAssets().open(ASSET),
                                StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            body.append(line).append('\n');
                            if (body.length() > 250_000)
                                throw new IllegalArgumentException("Season asset too large");
                        }
                    }
                    value = new JSONObject(body.toString());
                    validate(value);
                    cached = value;
                }
            }
        }
        return new JSONObject(value.toString()); // callers cannot mutate cached data
    }

    public static void validate(JSONObject pack) throws Exception {
        if (pack == null || !SCHEMA.equals(pack.optString("schema")))
            throw new IllegalArgumentException("Invalid Faerun season schema");
        if (!"dnd".equals(pack.optString("world_profile"))
                || !"dnd5e-2024".equals(pack.optString("rules_profile")))
            throw new IllegalArgumentException("World/rules firewall violated");
        if (!"open_sandbox".equals(pack.optString("adventure_mode")))
            throw new IllegalArgumentException("Season must remain optional");
        Set<String> canonicalPlaces = new HashSet<>();
        JSONArray places = requiredArray(pack, "places");
        for (int i = 0; i < places.length(); i++) {
            JSONObject place = places.getJSONObject(i);
            String id = requiredId(place, "id"), name = requiredId(place, "name");
            if (!place.optBoolean("canonical_place") || !canonicalPlaces.add(id))
                throw new IllegalArgumentException("Duplicate or noncanonical atlas anchor: " + id);
            if (!"exact_catalog_match_required".equals(place.optString("atlas_resolution")))
                throw new IllegalArgumentException("Unsafe atlas resolution for " + name);
        }
        String start = pack.optString("starting_city_id");
        if (!canonicalPlaces.contains(start))
            throw new IllegalArgumentException("Unknown starting city");
        JSONObject startContract = pack.getJSONObject("start_hex_requirement");
        if (!"Yartar".equals(startContract.optString("city"))
                || !"DM28".equals(startContract.optString("hex"))
                || !startContract.optBoolean("must_match_installed_atlas"))
            throw new IllegalArgumentException("Unverified initial atlas anchor");

        for (String section : new String[] {
                "factions", "arcs", "adventures", "encounters",
                "iconic_locations", "clock_templates" }) {
            JSONArray entries = requiredArray(pack, section);
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < entries.length(); i++) {
                JSONObject item = entries.getJSONObject(i);
                String id = requiredId(item, "id");
                if (!seen.add(id))
                    throw new IllegalArgumentException("Duplicate " + section + " ID: " + id);
                if (item.has("anchor")
                        && !canonicalPlaces.contains(item.optString("anchor")))
                    throw new IllegalArgumentException("Unrecognized atlas anchor: " + id);
                JSONArray anchors = item.optJSONArray("anchors");
                if (anchors != null) for (int j = 0; j < anchors.length(); j++)
                    if (!canonicalPlaces.contains(anchors.optString(j)))
                        throw new IllegalArgumentException("Unknown regional anchor: " + id);
            }
        }
        JSONObject generator = pack.getJSONObject("generator");
        for (String category : new String[] {
                "objective", "locale", "antagonist",
                "support", "complication", "resolution" }) {
            JSONArray rows = generator.getJSONArray(category);
            if (rows.length() != 6)
                throw new IllegalArgumentException("D6 subcategory count: " + category);
            for (int i = 0; i < 6; i++) {
                JSONArray choices = rows.getJSONArray(i);
                if (choices.length() != 6)
                    throw new IllegalArgumentException("D6 value count: " + category);
                for (int j = 0; j < 6; j++)
                    if (choices.optString(j).trim().isEmpty())
                        throw new IllegalArgumentException("Empty generator entry");
            }
        }
    }

    private static JSONArray requiredArray(JSONObject obj, String name) throws Exception {
        JSONArray array = obj.getJSONArray(name);
        if (array.length() == 0) throw new IllegalArgumentException("Empty season section: " + name);
        return array;
    }

    private static String requiredId(JSONObject obj, String field) throws Exception {
        String s = obj.getString(field).trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Missing season key: " + field);
        return s;
    }

    private static String fold(String value) {
        String normalized = Normalizer.normalize(
            value == null ? "" : value.toLowerCase(new Locale("pt", "BR")),
            Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
            .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static boolean explicitlyAsksForLeads(String focus) {
        String f = fold(focus);
        if (f.isEmpty()) return false;
        for (String word : new String[] {
                "trabalho", "contrato", "rumor", "boato", "missao",
                "aventura", "investigar", "investigacao", "emprego",
                "oferta", "gancho", "o que fazer", "acontecendo",
                "oportunidade", "procurar servico", "quadro de avisos" })
            if (f.contains(word)) return true;
        return false;
    }

    /**
     * Pure preview. It NEVER registers a quest, inserts characters, grants
     * facts, or publishes GM secrets. Intent is required, even at a verified
     * canonical location, so stale/repeated hooks cannot haunt every scene.
     */
    public static JSONObject previewForPlace(JSONObject pack, String placeId,
                                              String focus, long seed) throws Exception {
        validate(pack);
        JSONObject out = new JSONObject()
            .put("season_id", pack.getString("id"))
            .put("rule_source", "host_dnd2024")
            .put("no_state_mutation", true)
            .put("optional", true)
            .put("status", "on_demand_only");
        if (!explicitlyAsksForLeads(focus)) return out;

        boolean known = false;
        JSONArray places = pack.getJSONArray("places");
        for (int i = 0; i < places.length(); i++)
            if (placeId.equals(places.getJSONObject(i).optString("id"))) known = true;
        if (!known) return out.put("status", "place_not_in_season");

        List<JSONObject> matching = new ArrayList<>();
        JSONArray adventures = pack.getJSONArray("adventures");
        for (int i = 0; i < adventures.length(); i++) {
            JSONObject entry = adventures.getJSONObject(i);
            if (placeId.equals(entry.optString("anchor"))) matching.add(entry);
        }
        if (matching.isEmpty()) return out.put("status", "no_local_lead");
        Random draw = new Random(seed ^ (placeId.hashCode() * 31L));
        int start = draw.nextInt(matching.size());
        JSONArray leads = new JSONArray();
        for (int i = 0; i < Math.min(2, matching.size()); i++) {
            JSONObject item = matching.get((start + i) % matching.size());
            leads.put(new JSONObject()
                .put("id", item.getString("id"))
                .put("title", item.getString("title"))
                .put("hook_for_director_only", item.optString("hook"))
                .put("kind", item.optString("kind"))
                .put("canonical_anchor", placeId)
                .put("not_yet_discovered", true)
                .put("offer_not_accepted", true));
        }
        return out.put("status", "optional_candidates")
            .put("leads_director_only", leads)
            .put("director_contract",
                "São possibilidades de mundo, nunca fatos já observados. Apresente apenas " +
                "se houver oportunidade e evidência na cena. Não invente NPC presente, " +
                "resultado mecânico, missão aceita ou testemunho.");
    }

    /** Auditable six-category generator: two D6 rolls per category. */
    public static JSONObject generate(JSONObject pack, long seed) throws Exception {
        validate(pack);
        Random dice = new Random(seed);
        JSONObject result = new JSONObject()
            .put("season_id", pack.getString("id"))
            .put("type", "premise_preview_only")
            .put("no_gameplay_commit", true);
        JSONArray receipts = new JSONArray();
        JSONObject choices = new JSONObject();
        JSONObject tables = pack.getJSONObject("generator");
        for (String category : new String[] {
                "objective", "locale", "antagonist", "support",
                "complication", "resolution" }) {
            int a = dice.nextInt(6), b = dice.nextInt(6);
            String value = tables.getJSONArray(category).getJSONArray(a).getString(b);
            choices.put(category, value);
            receipts.put(new JSONObject().put("category", category)
                .put("subcategory_d6", a + 1).put("entry_d6", b + 1));
        }
        return result.put("choices", choices).put("dice_receipts", receipts)
            .put("director_contract",
                "Gerador cria somente premissa. Combine com região verificada, " +
                "evidência persistida e regras do host; não declare um evento ocorrido.");
    }

    /**
     * Fail-closed adapter into the existing compact Master context.
     * The Atlas is canonical; the season overlay never changes its catalog.
     */
    public static JSONObject context(AppDb db, String focus) {
        try {
            if (db == null || !eligible(db.getActiveMapProfile(),
                    db.isStrixhavenAdventureActive()) || !explicitlyAsksForLeads(focus))
                return new JSONObject();
            Context app = BraseiroDndApplication.currentContext();
            if (app == null) return new JSONObject();
            JSONObject map = db.getMapState();
            String packId = map.optString("map_package_id", "");
            String hex = map.optString("current_hex_id", "");
            if (packId.isEmpty() || hex.isEmpty()
                    || !db.isMapPackageUsable(packId)
                    || !db.hasAtlasContractMetadata(packId)
                    || !db.isValidMapHex(packId, hex))
                return new JSONObject();
            JSONObject scene = db.getSceneState();
            String sceneHex = scene.optString("hex_id", "");
            if (!sceneHex.isEmpty() && !sceneHex.equalsIgnoreCase(hex))
                return new JSONObject();

            JSONObject season = loadAsset(app);
            JSONArray listed = season.getJSONArray("places");
            List<JSONObject> atHex = db.getMapLocationsForHex(packId, hex);
            for (JSONObject atlasPlace : atHex) {
                if (atlasPlace == null
                        || atlasPlace.optString("placement_status", "").contains("review"))
                    continue;
                String name = atlasPlace.optString("name", "");
                if (!hex.equalsIgnoreCase(atlasPlace.optString("hex_id", "")))
                    continue;
                for (int i = 0; i < listed.length(); i++) {
                    JSONObject location = listed.getJSONObject(i);
                    if (!fold(name).equals(fold(location.optString("name"))))
                        continue;
                    String sceneName = fold(scene.optString("location", ""));
                    // Reject a scene explicitly naming a different canonical town.
                    if (!sceneName.isEmpty()) {
                        boolean contradicts = false;
                        for (int j = 0; j < listed.length(); j++) {
                            String other = fold(listed.getJSONObject(j).optString("name", ""));
                            if (!other.equals(fold(name)) && sceneName.equals(other)) {
                                contradicts = true;
                                break;
                            }
                        }
                        if (contradicts) return new JSONObject();
                    }
                    int day = Math.max(1, scene.optInt("day_number", 1));
                    long salt = (((long) day) << 32) ^ hex.hashCode();
                    JSONObject lead = previewForPlace(season,
                        location.getString("id"), focus, salt);
                    return "optional_candidates".equals(lead.optString("status"))
                        ? lead : new JSONObject();
                }
            }
        } catch (Exception error) {
            AppLog.w("FaerunSeasonRuntime", "Season overlay unavailable; no core mutation", error);
        }
        return new JSONObject();
    }
}
