package com.braseiro.dnd5e;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Contrato geométrico e identitário de um pacote de Atlas.
 *
 * Regra central: logicalMapId identifica o mundo; mapPackageId identifica apenas
 * a representação visual instalada. Nenhum campo é inferido a partir dos pixels.
 */
public final class AtlasMapProfile {
    public final String schemaVersion;
    public final String logicalMapId;
    public final String mapPackageId;
    public final String atlasNamespace;
    public final String coordinateFrameId;
    public final String worldProfile;
    public final String setting;
    public final String catalogVersion;
    public final String imageFile;
    public final int imageWidth;
    public final int imageHeight;
    public final String gridType;
    public final String orientation;
    public final String offsetLayout;
    public final String hexIdPolicy;
    public final float originX;
    public final float originY;
    public final float sideLength;
    public final float hexWidth;
    public final float hexHeight;
    public final float columnStepX;
    public final float rowStepY;
    public final float oddColumnYOffset;
    public final int columns;
    public final int hexCount;
    public final float distanceMiles;
    public final boolean canonicalReadOnly;
    public final boolean emptyHexValid;
    public final boolean sameFrameUsesUv;
    public final boolean differentFrameRequiresTransform;
    public final int canonicalLocationCount;
    public final int positionedLocationCount;
    public final int multihexAreaCount;
    public final JSONObject raw;

    private AtlasMapProfile(JSONObject root) throws Exception {
        raw = root;
        schemaVersion = root.optString("schemaVersion", "");
        logicalMapId = required(root, "logical_map_id");
        mapPackageId = required(root, "map_package_id");
        atlasNamespace = required(root, "atlas_namespace");
        coordinateFrameId = required(root.optJSONObject("coordinate_system"), "id");
        worldProfile = root.optString("world_profile", "dnd:forgotten_realms");
        setting = root.optString("setting", "");

        JSONObject image = requiredObject(root, "image");
        imageFile = required(image, "file");
        imageWidth = positiveInt(image, "width");
        imageHeight = positiveInt(image, "height");

        JSONObject grid = requiredObject(root, "grid");
        gridType = required(grid, "type");
        orientation = normalizeOrientation(required(grid, "orientation"));
        offsetLayout = required(grid, "offset_layout");
        hexIdPolicy = grid.optString("hex_id_policy", "");
        JSONObject origin = requiredObject(grid, "origin");
        originX = (float) origin.getDouble("x");
        originY = (float) origin.getDouble("y");
        sideLength = positiveFloat(grid, "side_length");
        hexWidth = positiveFloat(grid, "hex_width");
        hexHeight = positiveFloat(grid, "hex_height");
        columnStepX = positiveFloat(grid, "column_step_x");
        rowStepY = positiveFloat(grid, "row_step_y");
        oddColumnYOffset = (float) grid.optDouble("odd_column_y_offset", rowStepY / 2f);
        columns = positiveInt(grid, "columns");
        hexCount = positiveInt(grid, "hex_count");
        JSONObject distance = requiredObject(grid, "distance_per_hex");
        float distanceValue = positiveFloat(distance, "value");
        String unit = distance.optString("unit", "milhas").toLowerCase(Locale.US);
        distanceMiles = unit.contains("km") ? distanceValue / 1.609344f : distanceValue;

        JSONObject catalog = requiredObject(root, "canonical_catalog");
        catalogVersion = required(catalog, "version");
        canonicalReadOnly = catalog.optBoolean("read_only", false);
        canonicalLocationCount = positiveInt(catalog, "locations");
        positionedLocationCount = Math.max(0, catalog.optInt("positioned", 0));
        multihexAreaCount = Math.max(0, catalog.optInt("areas_multihex", 0));

        JSONObject semantics = requiredObject(root, "position_semantics");
        emptyHexValid = semantics.optBoolean("empty_hex_is_valid", false);
        JSONObject compatibility = requiredObject(root, "compatibility");
        sameFrameUsesUv = compatibility.optBoolean("same_coordinate_frame_can_use_uv_directly", false);
        differentFrameRequiresTransform = compatibility.optBoolean("different_coordinate_frame_requires_validated_transform", true);
        validate();
    }

    public static AtlasMapProfile parse(String json) throws Exception {
        if (json == null || json.trim().isEmpty()) throw new Exception("atlas_map_profile_v1.json vazio.");
        return new AtlasMapProfile(new JSONObject(json));
    }

    public static AtlasMapProfile load(File file) throws Exception {
        return parse(readText(file));
    }

    public void validateAgainst(JSONObject manifest, JSONObject index, JSONObject contract) throws Exception {
        if (manifest == null) throw new Exception("map_manifest.json ausente.");
        equal("logical_map_id", logicalMapId, manifest.optString("logical_map_id", ""));
        equal("map_package_id", mapPackageId, manifest.optString("map_package_id", manifest.optString("mapId", "")));
        equal("coordinate_frame_id", coordinateFrameId, manifest.optString("coordinate_frame_id", ""));
        equal("canonical_catalog_version", catalogVersion, manifest.optString("canonical_catalog_version", ""));
        if (index == null) throw new Exception("canonical_catalog_index_v1.json ausente.");
        equal("índice logical_map_id", logicalMapId, index.optString("logical_map_id", ""));
        equal("índice map_package_id", mapPackageId, index.optString("map_package_id", ""));
        equal("índice catalog version", catalogVersion, index.optString("canonical_catalog_version", ""));
        JSONObject counts = requiredObject(index, "counts");
        if (counts.optInt("hexes", -1) != hexCount) throw new Exception("Índice declara quantidade de hexes divergente.");
        if (counts.optInt("locations", -1) != canonicalLocationCount) throw new Exception("Índice declara quantidade de localidades divergente.");
        if (counts.optInt("positionedLocations", -1) != positionedLocationCount) throw new Exception("Índice declara quantidade de localidades posicionadas divergente.");
        JSONObject layers = contract == null ? null : firstObject(contract, "camadas", "layers");
        JSONObject canonical = layers == null ? null : layers.optJSONObject("CANONICAL");
        if (canonical == null) canonical = contract == null ? null : firstObject(contract, "canonicalLayer", "CANONICAL", "canonical");
        boolean readOnly = canonical != null && (canonical.has("mutavel") ? !canonical.optBoolean("mutavel", true) : canonical.optBoolean("read_only", canonical.optBoolean("readOnly", false)));
        if (!readOnly) throw new Exception("Contrato do Atlas não declara CANONICAL somente leitura.");
    }

    public int rowsEven() {
        int base = hexCount / columns;
        int remainder = hexCount % columns;
        return base + (remainder > 0 ? 1 : 0);
    }

    public int rowsOdd() {
        int base = hexCount / columns;
        int remainder = hexCount % columns;
        if (remainder <= 0) return base;
        // Em odd-q, a fração restante ocupa as colunas pares (zero-based).
        return remainder > columns / 2 ? base + 1 : base;
    }

    public float uToX(double u) { return (float) (clamp01(u) * imageWidth); }
    public float vToY(double v) { return (float) (clamp01(v) * imageHeight); }
    public float xToU(double x) { return imageWidth <= 0 ? 0f : (float) clamp01(x / imageWidth); }
    public float yToV(double y) { return imageHeight <= 0 ? 0f : (float) clamp01(y / imageHeight); }

    public String hostWorldProfile() {
        String v = worldProfile.toLowerCase(Locale.US);
        return v.contains("mystara") ? "mystara" : "dnd";
    }

    private void validate() throws Exception {
        if (!"hex".equalsIgnoreCase(gridType)) throw new Exception("AtlasMapProfile exige grid hexagonal.");
        if (!"flat-top".equals(orientation)) throw new Exception("Orientação não suportada pelo Atlas Contract: " + orientation);
        if (!"odd-q".equalsIgnoreCase(offsetLayout)) throw new Exception("Layout esperado odd-q; recebido " + offsetLayout + ".");
        if (logicalMapId.equals(mapPackageId)) throw new Exception("logical_map_id não pode ser confundido com map_package_id.");
        if (!canonicalReadOnly) throw new Exception("Catálogo canônico precisa ser read-only.");
        if (!emptyHexValid) throw new Exception("Contrato deve permitir hex vazio como posição válida.");
        if (!sameFrameUsesUv || !differentFrameRequiresTransform) throw new Exception("Política de portabilidade de coordenadas incompatível.");
        if (positionedLocationCount > canonicalLocationCount) throw new Exception("Contagem canônica inválida.");
    }

    private static JSONObject firstObject(JSONObject root, String... keys) {
        if (root == null) return null;
        for (String key : keys) { JSONObject value = root.optJSONObject(key); if (value != null) return value; }
        return null;
    }
    private static JSONObject requiredObject(JSONObject root, String key) throws Exception {
        JSONObject value = root == null ? null : root.optJSONObject(key);
        if (value == null) throw new Exception("Campo obrigatório ausente em AtlasMapProfile: " + key);
        return value;
    }
    private static String required(JSONObject root, String key) throws Exception {
        String value = root == null ? "" : root.optString(key, "").trim();
        if (value.isEmpty()) throw new Exception("Campo obrigatório ausente em AtlasMapProfile: " + key);
        return value;
    }
    private static int positiveInt(JSONObject root, String key) throws Exception {
        int value = root.optInt(key, 0); if (value <= 0) throw new Exception("Valor inválido para " + key + "."); return value;
    }
    private static float positiveFloat(JSONObject root, String key) throws Exception {
        float value = (float) root.optDouble(key, 0); if (!(value > 0f)) throw new Exception("Valor inválido para " + key + "."); return value;
    }
    private static void equal(String field, String expected, String actual) throws Exception {
        if (!expected.equals(actual)) throw new Exception(field + " divergente: esperado " + expected + ", recebido " + actual + ".");
    }
    private static double clamp01(double value) { return value < 0 ? 0 : (value > 1 ? 1 : value); }
    private static String normalizeOrientation(String value) { String v = value == null ? "" : value.toLowerCase(Locale.US); return v.contains("point") ? "pointy-top" : "flat-top"; }
    private static String readText(File file) throws Exception {
        if (file == null || !file.isFile()) throw new Exception("Arquivo de perfil não encontrado.");
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null) out.append(line).append('\n');
        }
        return out.toString();
    }
}
