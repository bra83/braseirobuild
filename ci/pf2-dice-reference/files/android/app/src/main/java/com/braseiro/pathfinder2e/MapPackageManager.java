package com.braseiro.pathfinder2e;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Importa pacotes de mapa sem OCR. O manifesto define os nomes e a geometria.
 * Esta build aceita somente pacotes cartográficos do perfil Pathfinder 2e.
 */
public final class MapPackageManager {
    public interface Callback {
        void onProgress(String stage, int current, int total);
        void onSuccess(ImportResult result);
        void onError(String message);
    }

    public static final class ImportResult {
        public final String packageId;
        public final String mapId;
        public final String worldProfile;
        public final String label;
        public final String imageUri;
        public final String overlayUri;
        public final String vectorOverlayUri;
        public final String hexIndexUri;
        public final String overlayMode;
        public final String manifestJson;
        public final String currentHexId;
        public final float tokenX;
        public final float tokenY;
        public final int hexCount;
        public final int locationCount;

        ImportResult(String packageId, String mapId, String worldProfile, String label, String imageUri,
                     String overlayUri, String vectorOverlayUri, String hexIndexUri, String overlayMode,
                     String manifestJson, String currentHexId, float tokenX, float tokenY,
                     int hexCount, int locationCount) {
            this.packageId = packageId;
            this.mapId = mapId;
            this.worldProfile = worldProfile;
            this.label = label;
            this.imageUri = imageUri;
            this.overlayUri = overlayUri;
            this.vectorOverlayUri = vectorOverlayUri;
            this.hexIndexUri = hexIndexUri;
            this.overlayMode = overlayMode;
            this.manifestJson = manifestJson;
            this.currentHexId = currentHexId;
            this.tokenX = tokenX;
            this.tokenY = tokenY;
            this.hexCount = hexCount;
            this.locationCount = locationCount;
        }
    }

    private static final int BATCH_SIZE = 750;
    private static final int MAX_ZIP_ENTRIES = 5000;
    private static final int READY_PACKAGES_TO_KEEP = 2;
    private static final long MAX_ENTRY_BYTES = 350L * 1024L * 1024L;
    private static final long MAX_PACKAGE_BYTES = 1200L * 1024L * 1024L;

    private final Context context;
    private final AppDb appDb;

    public MapPackageManager(Context context, AppDb appDb) {
        this.context = context.getApplicationContext();
        this.appDb = appDb;
    }

    public void importZip(final Uri uri, final Callback callback) {
        AppExecutors.serial().execute(new Runnable() {
            @Override public void run() {
                File staging = null;
                File finalDirectory = null;
                String packageId = "";
                String stage = "abrir o ZIP";
                try {
                    progress(callback, "Abrindo pacote ZIP", 0, 9);
                    staging = new File(context.getFilesDir(), "maps/staging_" + System.currentTimeMillis());
                    if (!staging.mkdirs() && !staging.isDirectory()) throw new Exception("Não foi possível criar a pasta temporária do mapa.");
                    List<String> extracted = extractAll(uri, staging, callback);

                    stage = "localizar o manifesto";
                    File manifestFile = findManifest(staging, extracted);
                    requireFile(manifestFile, "manifesto do mapa");
                    String manifestJson = readText(manifestFile);
                    JSONObject manifestRoot = new JSONObject(manifestJson);
                    MapManifestGeometry geometry = MapManifestGeometry.parse(manifestJson);
                    PackageSpec spec = PackageSpec.from(manifestRoot, geometry, staging);

                    stage = "validar o mapa-base";
                    progress(callback, "Validando mapa-base", 1, 9);
                    requireFile(spec.mapFile, spec.mapName);
                    validateImage(spec.mapFile, spec.mapName, Math.round(geometry.getImageWidth()), Math.round(geometry.getImageHeight()));

                    stage = "validar as camadas";
                    progress(callback, "Validando camadas vetorial e raster", 2, 9);
                    if (spec.rasterOverlayFile != null && spec.rasterOverlayFile.isFile())
                        validateImage(spec.rasterOverlayFile, spec.rasterOverlayFile.getName(), Math.round(geometry.getImageWidth()), Math.round(geometry.getImageHeight()));
                    if (spec.regionsOverlayFile != null && spec.regionsOverlayFile.isFile()) validateImage(spec.regionsOverlayFile, spec.regionsOverlayFile.getName(), Math.round(geometry.getImageWidth()), Math.round(geometry.getImageHeight()));
                    if (spec.politicalOverlayFile != null && spec.politicalOverlayFile.isFile()) validateImage(spec.politicalOverlayFile, spec.politicalOverlayFile.getName(), Math.round(geometry.getImageWidth()), Math.round(geometry.getImageHeight()));
                    if (spec.vectorOverlayFile != null) requireFile(spec.vectorOverlayFile, spec.vectorOverlayFile.getName());
                    if (spec.vectorOverlayGzipFile != null) requireFile(spec.vectorOverlayGzipFile, spec.vectorOverlayGzipFile.getName());

                    stage = "validar a geometria";
                    progress(callback, "Validando geometria e hexId", 3, 9);
                    verifyGeometry(geometry);
                    if (spec.hexIndexFile != null) {
                        requireFile(spec.hexIndexFile, spec.hexIndexFile.getName());
                        validateHexIndex(spec.hexIndexFile, geometry, callback);
                    }

                    stage = "preparar o pacote interno";
                    String mapId = geometry.getMapId().length() > 0 ? geometry.getMapId() : "mapa-" + System.currentTimeMillis();
                    String worldProfile = geometry.getWorldProfile();
                    JSONObject previous = appDb.getMapStateForProfile(worldProfile);
                    JSONObject previousPackage = appDb.getMapPackageAny(previous.optString("map_package_id"));
                    String previousLogicalMapId = previous.optString("logical_map_id", previousPackage.optString("map_id"));
                    String logicalMapId=geometry.getLogicalMapId().length()>0?geometry.getLogicalMapId():mapId;
                    boolean sameMap = sameLogicalMapFamily(logicalMapId,previousLogicalMapId)||sameLogicalMapFamily(mapId,previousLogicalMapId);
                    boolean coordinateFrameChanged=previousPackage.optInt("image_width",0)>0&&(previousPackage.optInt("image_width",0)!=Math.round(geometry.getImageWidth())||previousPackage.optInt("image_height",0)!=Math.round(geometry.getImageHeight()));
                    packageId = mapId + "@" + System.currentTimeMillis();
                    finalDirectory = new File(context.getFilesDir(), "maps/" + safeName(mapId) + "_" + System.currentTimeMillis());
                    if (finalDirectory.exists()) deleteDirectory(finalDirectory);
                    if (!staging.renameTo(finalDirectory)) {
                        copyDirectory(staging, finalDirectory);
                        deleteDirectory(staging);
                    }
                    staging = null;
                    spec = spec.rebase(finalDirectory);
                    manifestFile = new File(finalDirectory, manifestFile.getName());

                    String imageUri = Uri.fromFile(spec.mapFile).toString();
                    String overlayUri = spec.rasterOverlayFile != null && spec.rasterOverlayFile.isFile() ? Uri.fromFile(spec.rasterOverlayFile).toString() : "";
                    String regionsOverlayUri = spec.regionsOverlayFile != null && spec.regionsOverlayFile.isFile() ? Uri.fromFile(spec.regionsOverlayFile).toString() : "";
                    String politicalOverlayUri = spec.politicalOverlayFile != null && spec.politicalOverlayFile.isFile() ? Uri.fromFile(spec.politicalOverlayFile).toString() : "";
                    String vectorUri = spec.vectorOverlayFile != null && spec.vectorOverlayFile.isFile() ? Uri.fromFile(spec.vectorOverlayFile).toString() : "";
                    String vectorGzipUri = spec.vectorOverlayGzipFile != null && spec.vectorOverlayGzipFile.isFile() ? Uri.fromFile(spec.vectorOverlayGzipFile).toString() : "";
                    String indexUri = spec.hexIndexFile != null && spec.hexIndexFile.isFile() ? Uri.fromFile(spec.hexIndexFile).toString() : "";
                    String preferred = geometry.getPreferredOverlayMode() == null ? "" : geometry.getPreferredOverlayMode().toLowerCase(Locale.US);
                    String overlayMode;
                    if ((preferred.contains("svg") || preferred.contains("vector")) && (vectorUri.length() > 0 || vectorGzipUri.length() > 0)) overlayMode = "vector";
                    else if (preferred.contains("raster") && overlayUri.length() > 0) overlayMode = "raster";
                    else overlayMode = "dynamic";

                    SQLiteDatabase db = appDb.getWritableDatabase();
                    ContentValues packageValues = new ContentValues();
                    packageValues.put("package_id", packageId);
                    packageValues.put("map_id", mapId);
                    packageValues.put("world_profile", worldProfile);
                    packageValues.put("label", geometry.getLabel());
                    packageValues.put("image_uri", imageUri);
                    packageValues.put("overlay_uri", overlayUri);
                    packageValues.put("regions_overlay_uri", regionsOverlayUri);
                    packageValues.put("political_overlay_uri", politicalOverlayUri);
                    packageValues.put("vector_overlay_uri", vectorUri);
                    packageValues.put("vector_overlay_gzip_uri", vectorGzipUri);
                    packageValues.put("hex_index_uri", indexUri);
                    packageValues.put("overlay_mode", overlayMode);
                    packageValues.put("manifest_json", manifestJson);
                    packageValues.put("source_index_json", spec.sourceIndexFile != null && spec.sourceIndexFile.isFile() ? readText(spec.sourceIndexFile) : "{}");
                    packageValues.put("image_width", Math.round(geometry.getImageWidth()));
                    packageValues.put("image_height", Math.round(geometry.getImageHeight()));
                    packageValues.put("distance_miles", geometry.getHexScaleMiles());
                    packageValues.put("distance_km", geometry.getHexScaleKm());
                    packageValues.put("min_label_zoom", geometry.getMinLabelZoom());
                    packageValues.put("imported_at", System.currentTimeMillis());
                    packageValues.put("ready", 0);
                    db.insertOrThrow("map_packages", null, packageValues);

                    stage = "importar o atlas de hexes";
                    progress(callback, "Importando atlas de hexes", 4, 9);
                    requireFile(spec.atlasFile, spec.atlasName);
                    int hexCount = importAtlas(spec.atlasFile, packageId, geometry, callback);
                    if (hexCount != geometry.size()) throw new Exception("Atlas incompleto: " + hexCount + " de " + geometry.size() + " hexes.");

                    stage = "importar o catálogo de locais";
                    progress(callback, "Importando catálogo de locais", 5, 9);
                    requireFile(spec.locationsFile, spec.locationsName);
                    int locationCount = importLocations(spec.locationsFile, packageId, callback);

                    stage = "importar nações e regiões políticas";
                    progress(callback, "Importando nações e regiões políticas", 6, 9);
                    int nationCount = 0;
                    if (spec.nationsFile != null && spec.nationsFile.isFile()) nationCount = importNations(spec.nationsFile, packageId);
                    if (spec.regionProfilesFile != null && spec.regionProfilesFile.isFile()) importRegionProfiles(spec.regionProfilesFile, packageId);

                    stage = "cruzar atlas, índice, locais e política";
                    progress(callback, "Validando cruzamento por hexId", 7, 9);
                    validateDatabaseSamples(packageId, geometry);
                    ContentValues ready = new ContentValues();
                    ready.put("hex_count", hexCount);
                    ready.put("location_count", locationCount);
                    ready.put("nation_count", nationCount);
                    ready.put("ready", 1);
                    db.update("map_packages", ready, "package_id=?", new String[]{packageId});

                    if(sameMap&&!logicalMapId.equals(previousLogicalMapId))appDb.copyMapUserData(previousLogicalMapId,logicalMapId);
                    MapManifestGeometry.ManifestHex initial = geometry.nearest(geometry.getImageWidth() / 2f, geometry.getImageHeight() / 2f);
                    if (initial == null) initial = geometry.byId("0.00");
                    if(sameMap&&geometry.byId(previous.optString("current_hex_id"))!=null)initial=geometry.byId(previous.optString("current_hex_id"));
                    String currentHexId = initial == null ? "" : initial.id;
                    float tokenX = initial == null ? .5f : initial.centerX / geometry.getImageWidth();
                    float tokenY = initial == null ? .5f : initial.centerY / geometry.getImageHeight();
                    String reveals = new JSONObject().put("hexIds", new JSONArray().put(currentHexId)).put("points", new JSONArray()).toString();
                    boolean overlayVisible=true;float savedScale=0f,savedOx=0f,savedOy=0f;
                    if(sameMap){
                        JSONObject oldReveals=new JSONObject(previous.optString("reveals_json","{}"));JSONArray oldIds=oldReveals.optJSONArray("hexIds"),keptIds=new JSONArray();if(oldIds!=null)for(int i=0;i<oldIds.length();i++){String id=oldIds.optString(i);if(geometry.byId(id)!=null)keptIds.put(id);}if(currentHexId.length()>0&&!containsJsonString(keptIds,currentHexId))keptIds.put(currentHexId);reveals=new JSONObject().put("hexIds",keptIds).put("points",coordinateFrameChanged?new JSONArray():(oldReveals.optJSONArray("points")==null?new JSONArray():oldReveals.optJSONArray("points"))).toString();overlayVisible=previous.optInt("overlay_visible",1)==1;if(!coordinateFrameChanged){savedScale=(float)previous.optDouble("scale",0);savedOx=(float)previous.optDouble("offset_x",0);savedOy=(float)previous.optDouble("offset_y",0);}
                    }
                    appDb.saveMapPackageStateForProfile(worldProfile, imageUri, overlayUri, manifestJson, packageId, currentHexId, overlayVisible, reveals,
                            "pointy-top".equals(geometry.getOrientation()) ? "Hexagonal pointy-top" : "Hexagonal flat-top", geometry.getHexWidth(), previous.optInt("fog_enabled") == 1,
                            tokenX, tokenY, savedScale, savedOx, savedOy);
                    if(sameMap){String destination=geometry.byId(previous.optString("destination_hex_id"))==null?"":previous.optString("destination_hex_id");JSONArray oldRoute=new JSONArray(previous.optString("route_json","[]")),keptRoute=new JSONArray();for(int i=0;i<oldRoute.length();i++){String id=oldRoute.optString(i);if(geometry.byId(id)!=null)keptRoute.put(id);}appDb.saveMapTravelStateForProfile(worldProfile,previous.optInt("position_locked",0)==1,destination,keptRoute.toString(),previous.optString("route_owner","Jogador"));}
                    else appDb.saveMapTravelStateForProfile(worldProfile,false,"","[]","Jogador");
                    appDb.saveMapRenderModeForProfile(worldProfile, overlayMode, geometry.getMinLabelZoom());
                    pruneObsoletePackages(worldProfile, packageId, READY_PACKAGES_TO_KEEP);

                    progress(callback, "Pacote de mapa pronto", 9, 9);
                    if (callback != null) callback.onSuccess(new ImportResult(packageId, mapId, worldProfile, geometry.getLabel(), imageUri,
                            overlayUri, vectorUri.length() > 0 ? vectorUri : vectorGzipUri, indexUri, overlayMode,
                            manifestJson, currentHexId, tokenX, tokenY, hexCount, locationCount));
                } catch (Exception e) {
                    AppLog.e("MapPackage", "Falha ao " + stage + ".", e);
                    if (packageId.length() > 0) try { appDb.deleteMapPackage(packageId); } catch (Exception cleanupError) { AppLog.w("MapPackage", "Falha ao remover pacote incompleto.", cleanupError); }
                    if (staging != null) deleteDirectory(staging);
                    if (finalDirectory != null) deleteDirectory(finalDirectory);
                    String detail = e.getMessage() == null ? e.toString() : e.getMessage();
                    if (callback != null) callback.onError("Falha ao " + stage + ": " + detail);
                }
            }
        });
    }

    private static boolean containsJsonString(JSONArray array,String wanted){if(array==null||wanted==null)return false;for(int i=0;i<array.length();i++)if(wanted.equals(array.optString(i)))return true;return false;}

    private List<String> extractAll(Uri uri, File staging, Callback callback) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        InputStream opened;
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            if (path == null || path.trim().isEmpty()) throw new Exception("O caminho local do ZIP é inválido.");
            opened = new FileInputStream(new File(path));
        } else opened = resolver.openInputStream(uri);
        if (opened == null) throw new Exception("O ZIP não pôde ser aberto.");

        ArrayList<String> found = new ArrayList<>();
        HashMap<String, Boolean> names = new HashMap<>();
        byte[] buffer = new byte[65536];
        long packageBytes = 0;
        int entries = 0;
        try (InputStream source = opened;
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(source))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                try {
                    if (entry.isDirectory()) continue;
                    entries++;
                    if (entries > MAX_ZIP_ENTRIES) throw new Exception("O ZIP contém arquivos demais para ser processado com segurança.");
                    String basename = new File(entry.getName().replace('\\', '/')).getName();
                    if (basename.length() == 0 || basename.equals(".") || basename.equals("..")) continue;
                    if (names.containsKey(basename)) throw new Exception("O ZIP contém dois arquivos com o mesmo nome: " + basename);
                    File output = new File(staging, basename);
                    long written = 0;
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
                        int count;
                        while ((count = zip.read(buffer)) > 0) {
                            written += count;
                            packageBytes += count;
                            if (written > MAX_ENTRY_BYTES) throw new Exception("Arquivo excessivamente grande no pacote: " + basename);
                            if (packageBytes > MAX_PACKAGE_BYTES) throw new Exception("O pacote ultrapassa o limite seguro de 1,2 GB.");
                            out.write(buffer, 0, count);
                        }
                        out.flush();
                    } catch (Exception e) {
                        if (output.exists() && !output.delete()) AppLog.w("MapPackage", "Não foi possível apagar extração parcial: " + output.getName());
                        throw e;
                    }
                    names.put(basename, true);
                    found.add(basename);
                    progress(callback, "Extraindo " + basename, found.size(), Math.max(1, found.size()));
                } finally { zip.closeEntry(); }
            }
        }
        if (found.isEmpty()) throw new Exception("O ZIP está vazio.");
        return found;
    }

    private File findManifest(File staging, List<String> names) throws Exception {
        ArrayList<String> candidates = new ArrayList<>();
        for (String name : names) if (name.toLowerCase(Locale.US).startsWith("map_manifest") && name.toLowerCase(Locale.US).endsWith(".json")) candidates.add(name);
        if (candidates.isEmpty()) throw new Exception("Nenhum map_manifest*.json foi encontrado.");
        Collections.sort(candidates, new Comparator<String>() {
            @Override public int compare(String a, String b) {
                boolean av2 = a.toLowerCase(Locale.US).contains("v2");
                boolean bv2 = b.toLowerCase(Locale.US).contains("v2");
                if (av2 != bv2) return av2 ? -1 : 1;
                return a.compareToIgnoreCase(b);
            }
        });
        return new File(staging, candidates.get(0));
    }

    private void validateHexIndex(File file, MapManifestGeometry geometry, Callback callback) throws Exception {
        JsonReader reader = new JsonReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), StandardCharsets.UTF_8));
        reader.setLenient(true);
        int count = 0;
        String mapId = "";
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("mapId".equals(name)) mapId = reader.nextString();
            else if ("count".equals(name)) {
                int declared = reader.nextInt();
                if (declared != geometry.size()) throw new Exception("hex_index declara " + declared + " hexes, mas o manifesto gera " + geometry.size() + ".");
            } else if ("hexes".equals(name)) {
                reader.beginObject();
                while (reader.hasNext()) {
                    String hexId = reader.nextName();
                    Object raw = readJsonValue(reader);
                    if (!(raw instanceof JSONObject)) continue;
                    JSONObject value = (JSONObject) raw;
                    MapManifestGeometry.ManifestHex expected = geometry.byId(hexId);
                    if (expected == null) throw new Exception("hex_index contém ID inexistente no manifesto: " + hexId);
                    double x = value.optDouble("x", Double.NaN);
                    double y = value.optDouble("y", Double.NaN);
                    int c = value.optInt("c", expected.column);
                    int r = value.optInt("r", expected.row);
                    if (Math.abs(expected.centerX - x) > .01 || Math.abs(expected.centerY - y) > .01 || expected.column != c || expected.row != r)
                        throw new Exception("Centro divergente em " + hexId + ".");
                    count++;
                    if (count % 5000 == 0) progress(callback, "Validando índice: " + count + " / " + geometry.size(), count, geometry.size());
                }
                reader.endObject();
            } else reader.skipValue();
        }
        reader.endObject();
        reader.close();
        if (mapId.length() > 0 && geometry.getMapId().length() > 0 && !mapId.equals(geometry.getMapId())) throw new Exception("hex_index pertence ao mapa " + mapId + ".");
        if (count != geometry.size()) throw new Exception("hex_index incompleto: " + count + " de " + geometry.size() + " hexes.");
    }

    private int importAtlas(File file, String packageId, MapManifestGeometry geometry, Callback callback) throws Exception {
        int expected=geometry==null?0:geometry.size();
        importMetaregions(file,packageId);
        JsonReader reader = new JsonReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), StandardCharsets.UTF_8));
        reader.setLenient(true);
        int count = 0;
        ArrayList<AtlasRow> batch = new ArrayList<>(BATCH_SIZE);
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (!"hexes".equals(name)) { reader.skipValue(); continue; }
            reader.beginObject();
            while (reader.hasNext()) {
                String hexId = reader.nextName();
                Object raw;
                try { raw = readJsonValue(reader); }
                catch (Exception parseError) { throw new Exception("Atlas inválido no hex " + hexId + ": " + parseError.getMessage(), parseError); }
                if (!(raw instanceof JSONObject)) continue;
                batch.add(new AtlasRow(hexId, (JSONObject) raw));
                count++;
                if (batch.size() >= BATCH_SIZE) {
                    insertAtlasBatch(packageId, batch, geometry);
                    batch.clear();
                    progress(callback, "Atlas: " + count + " / " + expected + " hexes", count, expected);
                }
            }
            reader.endObject();
        }
        reader.endObject();
        reader.close();
        if (!batch.isEmpty()) insertAtlasBatch(packageId, batch, geometry);
        progress(callback, "Atlas: " + count + " / " + expected + " hexes", count, expected);
        return count;
    }

    private void importMetaregions(File file,String packageId)throws Exception{
        JSONObject root=new JSONObject(readText(file));JSONArray regions=root.optJSONArray("metaregions");if(regions==null)return;SQLiteDatabase db=appDb.getWritableDatabase();db.beginTransaction();try{for(int i=0;i<regions.length();i++){JSONObject r=regions.optJSONObject(i);if(r==null)continue;String id=r.optString("id","").trim();if(id.isEmpty())continue;ContentValues v=new ContentValues();v.put("package_id",packageId);v.put("metaregion_id",id);v.put("name_en",r.optString("nameEn",id));v.put("name_ptbr",r.optString("namePtBr",r.optString("nameEn",id)));v.put("polygon_json",r.optJSONArray("polygon")==null?"[]":r.optJSONArray("polygon").toString());v.put("source_text",r.optString("source",""));v.put("raw_json",r.toString());db.insertWithOnConflict("map_metaregions",null,v,SQLiteDatabase.CONFLICT_REPLACE);}db.setTransactionSuccessful();}finally{db.endTransaction();}
    }

    private void insertAtlasBatch(String packageId, List<AtlasRow> rows, MapManifestGeometry geometry) throws Exception {
        SQLiteDatabase db = appDb.getWritableDatabase();
        db.beginTransaction();
        try {
            for (AtlasRow row : rows) {
                JSONObject value = row.value;
                JSONObject center = value.optJSONObject("center");
                JSONObject terrain = value.optJSONObject("terrain");
                JSONObject review = value.optJSONObject("review");
                JSONArray locationIds = value.optJSONArray("locationIds");
                ContentValues values = new ContentValues();
                values.put("package_id", packageId);
                values.put("hex_id", row.hexId);
                MapManifestGeometry.ManifestHex runtimeHex=geometry==null?null:geometry.byId(row.hexId);
                values.put("center_x", runtimeHex==null?(center == null ? 0 : center.optDouble("x")):runtimeHex.centerX);
                values.put("center_y", runtimeHex==null?(center == null ? 0 : center.optDouble("y")):runtimeHex.centerY);
                values.put("terrain_primary", terrain == null || terrain.isNull("primary") ? "" : terrain.optString("primary"));
                values.put("terrain_secondary", terrain == null || terrain.optJSONArray("secondary") == null ? "[]" : terrain.optJSONArray("secondary").toString());
                String classification = terrain == null ? "" : terrain.optString("classificationStatus", terrain.optString("method",""));
                values.put("terrain_confidence", terrain == null ? 0 : terrain.has("confidence") ? terrain.optDouble("confidence") : (classification.contains("automatic") ? .80d : 1d));
                values.put("terrain_method", classification);
                values.put("metaregion_id", value.optString("metaregionId",""));
                values.put("roads_json", value.optJSONArray("roads") == null ? "[]" : value.optJSONArray("roads").toString());
                values.put("waterways_json", value.optJSONArray("waterways") == null ? "[]" : value.optJSONArray("waterways").toString());
                values.put("political_json", value.optJSONObject("political") == null ? (value.optJSONArray("political") == null ? "{}" : value.optJSONArray("political").toString()) : value.optJSONObject("political").toString());
                values.put("review_status", review == null ? "" : review.optString("status"));
                values.put("review_reasons", review == null ? "[]" : (review.optJSONArray("reasons") != null ? review.optJSONArray("reasons").toString() : (review.optJSONArray("notes") != null ? review.optJSONArray("notes").toString() : "[]")));
                values.put("location_ids", locationIds == null ? "[]" : locationIds.toString());
                JSONArray nearby=value.optJSONArray("nearbyLocationIds");
                JSONObject canonical=value.optJSONObject("canonicalContext");
                if((nearby==null||nearby.length()==0)&&canonical!=null)nearby=canonical.optJSONArray("nearbyPlacedLocationIds");
                values.put("nearby_location_ids", nearby == null ? "[]" : nearby.toString());
                values.put("raw_json", value.toString());
                db.insertOrThrow("map_hex_atlas", null, values);
                if (locationIds != null) for (int i = 0; i < locationIds.length(); i++) {
                    String locationId = locationIds.optString(i, "");
                    if (locationId.length() == 0) continue;
                    ContentValues join = new ContentValues();
                    join.put("package_id", packageId);
                    join.put("hex_id", row.hexId);
                    join.put("location_id", locationId);
                    db.insertWithOnConflict("map_hex_locations", null, join, SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private int importLocations(File file, String packageId, Callback callback) throws Exception {
        JsonReader reader = new JsonReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), StandardCharsets.UTF_8));
        reader.setLenient(true);
        int count = 0;
        ArrayList<JSONObject> batch = new ArrayList<>(BATCH_SIZE);
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (!"locations".equals(name) && !"locais".equals(name)) { reader.skipValue(); continue; }
            reader.beginArray();
            while (reader.hasNext()) {
                Object raw = readJsonValue(reader);
                if (!(raw instanceof JSONObject)) continue;
                batch.add((JSONObject) raw);
                count++;
                if (batch.size() >= BATCH_SIZE) { insertLocationBatch(packageId, batch); batch.clear(); progress(callback, "Locais: " + count, count, Math.max(1, count)); }
            }
            reader.endArray();
        }
        reader.endObject();
        reader.close();
        if (!batch.isEmpty()) insertLocationBatch(packageId, batch);
        progress(callback, "Locais: " + count, count, Math.max(1, count));
        return count;
    }

    private void insertLocationBatch(String packageId, List<JSONObject> rows) throws Exception {
        SQLiteDatabase db = appDb.getWritableDatabase();
        db.beginTransaction();
        try {
            for (JSONObject location : rows) {
                String id = location.optString("id", "").trim();
                if (id.length() == 0) continue;
                String hexId = location.isNull("hexId") ? "" : location.optString("hexId", "").trim();
                JSONObject placement = location.optJSONObject("placement");
                String status = placement == null ? location.optString("placementStatus",location.optString("status", "")) : placement.optString("status", location.optString("placementStatus",location.optString("status", "")));
                String locationName=location.optString("name",location.optString("nome",""));
                String locationType=location.optString("type",location.optString("tipo",""));
                String locationRegion=location.optString("region",location.optString("areaOrNation",""));
                if(locationRegion.length()==0){JSONArray regions=location.optJSONArray("regiao");if(regions==null)regions=location.optJSONArray("regions");if(regions!=null){StringBuilder rb=new StringBuilder();for(int ri=0;ri<regions.length();ri++){String rv=regions.optString(ri,"");if(rv.length()>0){if(rb.length()>0)rb.append("; ");rb.append(rv);}}locationRegion=rb.toString();}else locationRegion=location.optString("regiao","");}
                ContentValues values = new ContentValues();
                values.put("package_id", packageId);
                values.put("location_id", id);
                values.put("name", locationName);
                values.put("type", locationType);
                values.put("hex_id", hexId);
                values.put("region", locationRegion);
                values.put("metaregion_id", location.optString("metaregionId",""));
                values.put("area_or_nation", location.optString("areaOrNation",locationRegion));
                values.put("canonical_tier", location.optString("canonicalTier",""));
                values.put("official_5e", location.optBoolean("official5e") ? 1 : 0);
                values.put("scope", location.optString("scope"));
                values.put("placement_method", placement == null ? "" : placement.optString("method"));
                values.put("placement_confidence", placement == null ? (hexId.length() > 0 ? 1d : 0d) : placement.optDouble("confidence"));
                values.put("placement_status", status);
                values.put("source_ids", location.optJSONArray("sources") != null ? location.optJSONArray("sources").toString() : (location.optJSONArray("sourceIds") == null ? "[]" : location.optJSONArray("sourceIds").toString()));
                values.put("tags", location.optJSONArray("tags") == null ? "[]" : location.optJSONArray("tags").toString());
                values.put("raw_json", location.toString());
                db.insertWithOnConflict("map_locations", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                if (hexId.length() > 0) {
                    ContentValues join = new ContentValues();
                    join.put("package_id", packageId);
                    join.put("hex_id", hexId);
                    join.put("location_id", id);
                    db.insertWithOnConflict("map_hex_locations", null, join, SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private int importNations(File file,String packageId)throws Exception{
        JSONObject root=new JSONObject(readText(file));JSONArray nations=root.optJSONArray("nations");if(nations==null)return 0;SQLiteDatabase db=appDb.getWritableDatabase();db.beginTransaction();int count=0;try{for(int i=0;i<nations.length();i++){JSONObject n=nations.optJSONObject(i);if(n==null)continue;String id=n.optString("id","").trim();if(id.isEmpty())continue;ContentValues v=new ContentValues();v.put("package_id",packageId);v.put("nation_id",id);v.put("name_en",n.optString("nameEn",""));v.put("name_ptbr",n.optString("namePtBr",n.optString("nameEn","")));v.put("polygon_json",n.optJSONArray("polygon")==null?"[]":n.optJSONArray("polygon").toString());v.put("hex_ids_json",n.optJSONArray("hexIds")==null?"[]":n.optJSONArray("hexIds").toString());v.put("raw_json",n.toString());db.insertWithOnConflict("map_nations",null,v,SQLiteDatabase.CONFLICT_REPLACE);count++;}db.setTransactionSuccessful();}finally{db.endTransaction();}return count;
    }

    private void validateDatabaseSamples(String packageId, MapManifestGeometry geometry) throws Exception {
        ArrayList<String> ids=new ArrayList<String>();
        if(geometry.byId("0.00")!=null)ids.add("0.00");
        if(geometry.byId("11.09")!=null)ids.add("11.09");
        MapManifestGeometry.ManifestHex center=geometry.nearest(geometry.getImageWidth()/2f,geometry.getImageHeight()/2f);if(center!=null&&!ids.contains(center.id))ids.add(center.id);
        if(ids.isEmpty()&&!geometry.all().isEmpty())ids.add(geometry.all().get(0).id);
        for(String id:ids){MapManifestGeometry.ManifestHex expected=geometry.byId(id);JSONObject row=appDb.getRawMapHex(packageId,id);if(expected==null||row.length()==0)throw new Exception("Cruzamento ausente no hex "+id+".");if(Math.abs(row.optDouble("center_x")-expected.centerX)>.011||Math.abs(row.optDouble("center_y")-expected.centerY)>.011)throw new Exception("Centro do atlas divergente no hex "+id+".");}
    }

    private void validateImage(File file, String label, int expectedWidth, int expectedHeight) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) throw new Exception(label + " não é uma imagem válida.");
        if (expectedWidth > 0 && expectedHeight > 0 && (options.outWidth != expectedWidth || options.outHeight != expectedHeight))
            throw new Exception(label + " precisa ter " + expectedWidth + "×" + expectedHeight + "; encontrado " + options.outWidth + "×" + options.outHeight + ".");
    }

    private void verifyGeometry(MapManifestGeometry geometry) throws Exception {
        if(!"pf2".equals(geometry.getWorldProfile()))throw new Exception("Perfil cartográfico não é Pathfinder 2e.");
        if(geometry.size()<=0)throw new Exception("Geometria sem hexes.");
        if(geometry.getImageWidth()<=0||geometry.getImageHeight()<=0)throw new Exception("Dimensões cartográficas inválidas.");
        if(geometry.getStepX()<=0||geometry.getStepY()<=0)throw new Exception("Passo da grade inválido.");
        if(geometry.getMapId().contains("pathfinder2e-inner-sea")){
            if(geometry.size()!=3300||geometry.getColumns()!=55||geometry.getRows()!=60||!"flat-top".equals(geometry.getOrientation()))throw new Exception("Geometria Inner Sea inesperada.");
            String mapId=geometry.getMapId();boolean v6="pathfinder2e-inner-sea-4270x5388-canonical-v6-grid-native-hires-compat".equals(mapId),v5="pathfinder2e-inner-sea-2135x2720-canonical-v5-max-compat".equals(mapId);
            if(!v6&&!v5)throw new Exception("Revisão cartográfica Inner Sea não reconhecida: "+mapId+".");
            float expectedWidth=v6?4270f:2135f,expectedHeight=v6?5388f:2720f;
            if(Math.abs(geometry.getImageWidth()-expectedWidth)>.5f||Math.abs(geometry.getImageHeight()-expectedHeight)>.5f)throw new Exception("Dimensões Inner Sea divergentes: esperado "+Math.round(expectedWidth)+"×"+Math.round(expectedHeight)+", manifesto "+Math.round(geometry.getImageWidth())+"×"+Math.round(geometry.getImageHeight())+".");
            if(v6&&(!"pathfinder:golarion:inner_sea".equals(geometry.getLogicalMapId())||!"pf2e:golarion:inner_sea:shieldwulf_hexmap_rectified_v1".equals(geometry.getCoordinateFrameId())))throw new Exception("Identidade ou quadro de coordenadas do Atlas V6 divergente.");
            if(v6&&(Math.abs(geometry.getOriginX()-25f)>.02f||Math.abs(geometry.getOriginY()-48f)>.02f||Math.abs(geometry.getStepX()-77.7f)>.02f||Math.abs(geometry.getStepY()-89.72023f)>.02f))throw new Exception("Calibração geométrica do Atlas V6 divergente.");
            MapManifestGeometry.ManifestHex h=geometry.byId("11.09");
            if(h==null)throw new Exception("Hex canônico 11.09 ausente.");
            float expectedX=geometry.getOriginX()+11f*geometry.getStepX();
            float expectedY=geometry.getOriginY()+9f*geometry.getStepY()+geometry.getAlternateColumnOffsetY();
            if(Math.abs(h.centerX-expectedX)>.02f||Math.abs(h.centerY-expectedY)>.02f)throw new Exception("Snap canônico Inner Sea divergiu do próprio manifesto em 11.09.");
            MapManifestGeometry.ManifestHex last=geometry.byId("54.59");
            if(last==null||last.centerX<0||last.centerY<0||last.centerX>geometry.getImageWidth()+1f||last.centerY>geometry.getImageHeight()+1f)throw new Exception("Extensão da grade Inner Sea ultrapassa a imagem.");
        }
        MapManifestGeometry.ManifestHex first=geometry.all().get(0),near=geometry.nearest(first.centerX,first.centerY);if(near==null||!first.id.equals(near.id))throw new Exception("Snap por coordenadas falhou no primeiro hex.");
        MapManifestGeometry.ManifestHex middle=geometry.all().get(geometry.size()/2),nearMiddle=geometry.nearest(middle.centerX,middle.centerY);if(nearMiddle==null||!middle.id.equals(nearMiddle.id))throw new Exception("Snap por coordenadas falhou no hex intermediário.");
    }

    private static Object readJsonValue(JsonReader reader) throws Exception {
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JSONObject object = new JSONObject();
                reader.beginObject();
                while (reader.hasNext()) object.put(reader.nextName(), readJsonValue(reader));
                reader.endObject();
                return object;
            case BEGIN_ARRAY:
                JSONArray array = new JSONArray();
                reader.beginArray();
                while (reader.hasNext()) array.put(readJsonValue(reader));
                reader.endArray();
                return array;
            case STRING: return reader.nextString();
            case NUMBER:
                String number = reader.nextString();
                try { return number.indexOf('.') >= 0 || number.indexOf('e') >= 0 || number.indexOf('E') >= 0 ? Double.parseDouble(number) : Long.parseLong(number); }
                catch (Exception e) { return number; }
            case BOOLEAN: return reader.nextBoolean();
            case NULL: reader.nextNull(); return "";
            default: reader.skipValue(); return "";
        }
    }

    private void pruneObsoletePackages(String worldProfile, String currentPackageId, int keep) {
        try {
            List<JSONObject> packages = appDb.listMapPackages(worldProfile);
            int retainedReady = 0;
            for (JSONObject row : packages) {
                String id = row.optString("package_id", "");
                boolean ready = row.optInt("ready", 0) == 1;
                if (id.equals(currentPackageId)) { retainedReady++; continue; }
                if (ready && retainedReady < Math.max(1, keep)) { retainedReady++; continue; }
                File directory = packageDirectory(row.optString("image_uri", ""));
                appDb.deleteMapPackage(id);
                if (directory != null) safeDeleteMapDirectory(directory);
                AppLog.i("MapPackage", (ready ? "Pacote antigo removido: " : "Pacote inválido removido: ") + id);
            }
        } catch (Exception e) { AppLog.w("MapPackage", "Não foi possível limpar pacotes antigos de mapa.", e); }
    }

    private static File packageDirectory(String imageUri) {
        try {
            String path = Uri.parse(imageUri).getPath();
            File image = path == null ? null : new File(path);
            return image == null ? null : image.getParentFile();
        } catch (Exception e) { return null; }
    }

    private void safeDeleteMapDirectory(File directory) {
        try {
            File root = new File(context.getFilesDir(), "maps").getCanonicalFile();
            File candidate = directory.getCanonicalFile();
            String prefix = root.getPath() + File.separator;
            if (!candidate.getPath().startsWith(prefix)) {
                AppLog.w("MapPackage", "Limpeza bloqueada fora da pasta de mapas: " + candidate);
                return;
            }
            deleteDirectory(candidate);
        } catch (Exception e) { AppLog.w("MapPackage", "Falha ao remover diretório de mapa antigo.", e); }
    }

    private static String readText(File file) throws Exception {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
        }
        return text.toString();
    }

    private static boolean sameLogicalMapFamily(String a,String b){
        if(a==null||b==null)return false;String x=a.trim(),y=b.trim();if(x.equals(y))return true;
        String logical="pathfinder:golarion:inner_sea";boolean xi=logical.equals(x)||x.startsWith("pathfinder2e-inner-sea-");boolean yi=logical.equals(y)||y.startsWith("pathfinder2e-inner-sea-");
        return xi&&yi;
    }

    private void importRegionProfiles(File file,String packageId)throws Exception{
        JSONObject root=new JSONObject(readText(file));JSONObject regions=root.optJSONObject("regions");if(regions==null)return;
        SQLiteDatabase db=appDb.getWritableDatabase();db.beginTransaction();try{
            JSONArray names=regions.names();if(names!=null)for(int i=0;i<names.length();i++){
                String id=names.optString(i,"");JSONObject profile=regions.optJSONObject(id);if(id.isEmpty()||profile==null)continue;
                ContentValues v=new ContentValues();v.put("package_id",packageId);v.put("metaregion_id",id);v.put("name_en",profile.optString("nameEn",id));v.put("name_ptbr",profile.optString("namePtBr",profile.optString("nameEn",id)));v.put("raw_json",profile.toString());
                int updated=db.update("map_metaregions",v,"package_id=? AND metaregion_id=?",new String[]{packageId,id});
                if(updated==0){v.put("polygon_json","[]");v.put("source_text","LOWG2019");db.insertWithOnConflict("map_metaregions",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void requireFile(File file, String name) throws Exception { if (file == null || !file.isFile() || file.length() == 0) throw new Exception("Arquivo ausente ou vazio: " + name); }
    private static void progress(Callback callback, String stage, int current, int total) { if (callback != null) callback.onProgress(stage, current, total); }
    private static String safeName(String value) { return value == null ? "mapa" : value.replaceAll("[^A-Za-z0-9._-]", "_"); }

    private static void copyDirectory(File source, File target) throws Exception {
        if (!target.mkdirs() && !target.isDirectory()) throw new Exception("Não foi possível criar a pasta final do mapa.");
        File[] files = source.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[65536];
        for (File file : files) {
            if (file.isDirectory()) copyDirectory(file, new File(target, file.getName()));
            else {
                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                     BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(target, file.getName())))) {
                    int count;
                    while ((count = in.read(buffer)) > 0) out.write(buffer, 0, count);
                    out.flush();
                }
            }
        }
    }

    private static void deleteDirectory(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) { File[] children = file.listFiles(); if (children != null) for (File child : children) deleteDirectory(child); }
        file.delete();
    }

    private static final class AtlasRow {
        final String hexId;
        final JSONObject value;
        AtlasRow(String hexId, JSONObject value) { this.hexId = hexId; this.value = value; }
    }

    private static final class PackageSpec {
        final File directory;final String mapName,atlasName,locationsName,nationsName,regionProfilesName,sourceIndexName,rasterName,regionsName,politicalName,vectorName,vectorGzipName,indexName;
        final File mapFile,atlasFile,locationsFile,nationsFile,regionProfilesFile,sourceIndexFile,rasterOverlayFile,regionsOverlayFile,politicalOverlayFile,vectorOverlayFile,vectorOverlayGzipFile,hexIndexFile;
        PackageSpec(File d,String map,String atlas,String loc,String nations,String regionProfiles,String sourceIndex,String raster,String regions,String political,String vector,String vg,String index){directory=d;mapName=map;atlasName=atlas;locationsName=loc;nationsName=nations;regionProfilesName=regionProfiles;sourceIndexName=sourceIndex;rasterName=raster;regionsName=regions;politicalName=political;vectorName=vector;vectorGzipName=vg;indexName=index;mapFile=file(d,map);atlasFile=file(d,atlas);locationsFile=file(d,loc);nationsFile=file(d,nations);regionProfilesFile=file(d,regionProfiles);sourceIndexFile=file(d,sourceIndex);rasterOverlayFile=file(d,raster);regionsOverlayFile=file(d,regions);politicalOverlayFile=file(d,political);vectorOverlayFile=file(d,vector);vectorOverlayGzipFile=file(d,vg);hexIndexFile=file(d,index);}
        PackageSpec rebase(File d){return new PackageSpec(d,mapName,atlasName,locationsName,nationsName,regionProfilesName,sourceIndexName,rasterName,regionsName,politicalName,vectorName,vectorGzipName,indexName);}
        static PackageSpec from(JSONObject root,MapManifestGeometry geometry,File directory)throws Exception{
            JSONObject source=root.optJSONObject("sourceMap"),image=root.optJSONObject("image"),data=root.optJSONObject("dataFiles"),overlays=root.optJSONObject("overlays");
            String map=source==null?"":source.optString("filename","");if(map.isEmpty()&&image!=null)map=image.optString("expectedFilename","");if(map.isEmpty())map=existing(directory,"map.png");
            String atlas=data==null?"":data.optString("hexAtlas",data.optString("terrainAtlas",""));if(atlas.isEmpty())atlas=existing(directory,"pathfinder2e_inner_sea_hex_atlas_v1.json");
            String loc=data==null?"":data.optString("locations","");if(loc.isEmpty())loc=existing(directory,"pathfinder2e_inner_sea_locations_v1.json");
            String nations=data==null?"":data.optString("nations","");if(nations.isEmpty())nations=existingOptional(directory,"pathfinder2e_inner_sea_nations_v1.json");
            String regionProfiles=data==null?"":data.optString("regionProfiles","");if(regionProfiles.isEmpty())regionProfiles=existingOptional(directory,"pathfinder2e_inner_sea_region_profiles_v1.json");
            String sourceIndex=data==null?"":data.optString("sourceIndex","");if(sourceIndex.isEmpty())sourceIndex=existingOptional(directory,"pathfinder2e_inner_sea_source_index_v1.json");
            String raster=overlays==null?"":overlays.optString("hexIdsOverlay","");if(raster.isEmpty())raster=existingOptional(directory,"hex_ids_overlay.png");
            String regions=overlays==null?"":overlays.optString("regionsOverlayPtBr","");if(regions.isEmpty())regions=existingOptional(directory,"map_regions_ptbr_overlay.png");
            String political=overlays==null?"":overlays.optString("nationLabelsPtBr","");if(political.isEmpty())political=existingOptional(directory,"nation_labels_ptbr_overlay.png");
            String vector="",vg="",index=data==null?"":data.optString("hexIndex","");
            return new PackageSpec(directory,map,atlas,loc,nations,regionProfiles,sourceIndex,raster,regions,political,vector,vg,index);
        }
        private static File file(File d,String name){return name==null||name.trim().isEmpty()?null:new File(d,new File(name).getName());}
        private static String existing(File d,String...names)throws Exception{for(String n:names)if(new File(d,n).isFile())return n;throw new Exception("Arquivo esperado não encontrado: "+java.util.Arrays.toString(names));}
        private static String existingOptional(File d,String...names){for(String n:names)if(new File(d,n).isFile())return n;return "";}
    }

}
