package com.braseiro.dnd5e;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Importa pacotes de mapa sem OCR. O manifesto define os nomes e a geometria.
 * O mapa de D&D legado e o pacote Mystara V2 usam o mesmo fluxo.
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

    /**
     * Localiza a versão mais alta do pacote Faerûn Atlas Contract embarcado.
     * O código não conhece V3/V5: a versão pertence ao pacote, não ao engine.
     */
    public static String latestBundledFaerunAsset(Context context) throws Exception {
        if(context==null)throw new Exception("Contexto ausente ao localizar Atlas de Faerûn.");
        String[] names=context.getAssets().list("maps");
        if(names==null)names=new String[0];
        Pattern pattern=Pattern.compile("^PACOTE_MAPA_FAERUN_2025_PTBR_MAX_V(\\d+)(?:_[A-Z0-9_]+)?\\.zip$",Pattern.CASE_INSENSITIVE);
        int bestVersion=-1;String best="";
        for(String name:names){Matcher m=pattern.matcher(name==null?"":name);if(!m.matches())continue;int v;try{v=Integer.parseInt(m.group(1));}catch(Exception ignored){continue;}if(v>bestVersion){bestVersion=v;best=name;}}
        if(best.length()==0)throw new Exception("Nenhum pacote versionado PACOTE_MAPA_FAERUN_2025_PTBR_MAX_V*.zip foi encontrado nos assets.");
        return "maps/"+best;
    }

    public void importZip(final Uri uri, final Callback callback) {/*BR_JSON_WRAP*/try{
        AppExecutors.serial().execute(new Runnable() {
            @Override public void run() {/*BR_JSON_WRAP*/try{
                File staging = null;
                File finalDirectory = null;
                String packageId = "";
                String stage = "abrir o ZIP";
                try {
                    progress(callback, "Abrindo pacote ZIP", 0, 8);
                    staging = new File(context.getFilesDir(), "maps/staging_" + System.currentTimeMillis());
                    if (!staging.mkdirs() && !staging.isDirectory()) throw new Exception("Não foi possível criar a pasta temporária do mapa.");
                    List<String> extracted = extractAll(uri, staging, callback);

                    stage = "localizar o manifesto";
                    File manifestFile = findManifest(staging, extracted);
                    requireFile(manifestFile, "manifesto do mapa");
                    String manifestJson = readText(manifestFile);
                    JSONObject manifestRoot = new JSONObject(manifestJson);
                    File profileFile = new File(staging, manifestRoot.optJSONObject("dataFiles") == null ? "atlas_map_profile_v1.json" : manifestRoot.optJSONObject("dataFiles").optString("atlasMapProfile", "atlas_map_profile_v1.json"));
                    AtlasMapProfile atlasProfile = profileFile.isFile() ? AtlasMapProfile.load(profileFile) : null;
                    File contractFile = new File(staging, "atlas_contract_v1.json");
                    File indexContractFile = new File(staging, "canonical_catalog_index_v1.json");
                    File migrationsFile = new File(staging, "map_migration_profiles_v1.json");
                    JSONObject atlasContract = contractFile.isFile() ? new JSONObject(readText(contractFile)) : new JSONObject();
                    JSONObject canonicalIndex = indexContractFile.isFile() ? new JSONObject(readText(indexContractFile)) : new JSONObject();
                    JSONObject migrationProfiles = migrationsFile.isFile() ? new JSONObject(readText(migrationsFile)) : new JSONObject();
                    if (atlasProfile != null) atlasProfile.validateAgainst(manifestRoot, canonicalIndex, atlasContract);
                    // Atlas Contract: não gere centros/topologia a partir da matemática do grid.
                    // A geometria exata só é construída DEPOIS da importação, lendo center_x/center_y/neighbors_json do dataset.
                    MapManifestGeometry geometry = atlasProfile == null ? MapManifestGeometry.parse(manifestJson) : null;
                    PackageSpec spec = PackageSpec.from(manifestRoot, geometry, atlasProfile, staging);

                    stage = "validar o mapa-base";
                    progress(callback, "Validando mapa-base", 1, 8);
                    requireFile(spec.mapFile, spec.mapName);
                    validateImage(spec.mapFile, spec.mapName, atlasProfile != null ? atlasProfile.imageWidth : Math.round(geometry.getImageWidth()), atlasProfile != null ? atlasProfile.imageHeight : Math.round(geometry.getImageHeight()));

                    stage = "validar as camadas";
                    progress(callback, "Validando camadas vetorial e raster", 2, 8);
                    if (spec.rasterOverlayFile != null && spec.rasterOverlayFile.isFile())
                        validateImage(spec.rasterOverlayFile, spec.rasterOverlayFile.getName(), atlasProfile != null ? atlasProfile.imageWidth : Math.round(geometry.getImageWidth()), atlasProfile != null ? atlasProfile.imageHeight : Math.round(geometry.getImageHeight()));
                    if (spec.vectorOverlayFile != null) requireFile(spec.vectorOverlayFile, spec.vectorOverlayFile.getName());
                    if (spec.vectorOverlayGzipFile != null) requireFile(spec.vectorOverlayGzipFile, spec.vectorOverlayGzipFile.getName());

                    stage = "validar a geometria";
                    progress(callback, "Validando geometria e hexId", 3, 8);
                    if (atlasProfile == null) {
                        verifyGeometry(geometry);
                        if (spec.hexIndexFile != null) {
                            requireFile(spec.hexIndexFile, spec.hexIndexFile.getName());
                            validateHexIndex(spec.hexIndexFile, geometry, callback);
                        }
                    }

                    stage = "preparar o pacote interno";
                    String mapId = atlasProfile != null ? atlasProfile.logicalMapId : (geometry.getMapId().length() > 0 ? geometry.getMapId() : "mapa-" + System.currentTimeMillis());
                    String worldProfile = atlasProfile != null ? atlasProfile.hostWorldProfile() : geometry.getWorldProfile();
                    if (!"dnd".equalsIgnoreCase(worldProfile)) throw new IllegalArgumentException("Este projeto aceita apenas mapas do perfil D&D; Mystara está excluído.");
                    JSONObject previous = appDb.getMapStateForProfile(worldProfile);
                    JSONObject previousPackage = appDb.getMapPackageAny(previous.optString("map_package_id"));
                    String previousLogicalMapId = previous.optString("logical_map_id", previousPackage.optString("logical_map_id", previousPackage.optString("map_id")));
                    boolean isFaerunMap = mapId.toLowerCase(Locale.US).contains("faerun") || "forgotten_realms:faerun".equalsIgnoreCase(mapId) || "dnd".equalsIgnoreCase(worldProfile);
                    boolean isPrevFaerun = previousLogicalMapId.toLowerCase(Locale.US).contains("faerun") || "forgotten_realms:faerun".equalsIgnoreCase(previousLogicalMapId) || "dnd".equalsIgnoreCase(worldProfile);
                    boolean sameMap = mapId.equals(previousLogicalMapId)
                            || (isFaerunMap && isPrevFaerun)
                            || (atlasProfile != null && AtlasMigrationEngine.sourceDeclaredForTarget(migrationProfiles, previousPackage.optString("map_package_id",previousPackage.optString("package_id",previous.optString("map_package_id",""))), previous.optString("coordinate_frame_id",previousPackage.optString("coordinate_frame_id","")), atlasProfile.mapPackageId, atlasProfile.coordinateFrameId))
                            || (previous.optString("current_hex_id", "").trim().length() > 0 && geometry != null && geometry.byId(previous.optString("current_hex_id")) != null);
                    packageId = atlasProfile != null ? atlasProfile.mapPackageId : mapId + "@" + System.currentTimeMillis();
                    if (atlasProfile != null && appDb.getMapPackageAny(packageId).length() > 0) appDb.deleteMapPackage(packageId);
                    finalDirectory = new File(context.getFilesDir(), "maps/" + safeName(packageId) + "_" + System.currentTimeMillis());
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
                    String vectorUri = spec.vectorOverlayFile != null && spec.vectorOverlayFile.isFile() ? Uri.fromFile(spec.vectorOverlayFile).toString() : "";
                    String vectorGzipUri = spec.vectorOverlayGzipFile != null && spec.vectorOverlayGzipFile.isFile() ? Uri.fromFile(spec.vectorOverlayGzipFile).toString() : "";
                    String indexUri = spec.hexIndexFile != null && spec.hexIndexFile.isFile() ? Uri.fromFile(spec.hexIndexFile).toString() : "";
                    String preferred = atlasProfile != null ? "" : (geometry.getPreferredOverlayMode() == null ? "" : geometry.getPreferredOverlayMode().toLowerCase(Locale.US));
                    String overlayMode;
                    if ((preferred.contains("svg") || preferred.contains("vector")) && (vectorUri.length() > 0 || vectorGzipUri.length() > 0)) overlayMode = "vector";
                    else if (preferred.contains("raster") && overlayUri.length() > 0) overlayMode = "raster";
                    else overlayMode = "dynamic";

                    String logicalMapId = atlasProfile != null ? atlasProfile.logicalMapId : (isFaerunMap ? "forgotten_realms:faerun" : mapId);
                    SQLiteDatabase db = appDb.getWritableDatabase();
                    ContentValues packageValues = new ContentValues();
                    packageValues.put("package_id", packageId);
                    packageValues.put("map_id", mapId);
                    packageValues.put("logical_map_id", logicalMapId);
                    packageValues.put("map_package_id", atlasProfile == null ? packageId : atlasProfile.mapPackageId);
                    packageValues.put("atlas_namespace", atlasProfile == null ? "" : atlasProfile.atlasNamespace);
                    packageValues.put("coordinate_frame_id", atlasProfile == null ? "" : atlasProfile.coordinateFrameId);
                    packageValues.put("canonical_catalog_version", atlasProfile == null ? "" : atlasProfile.catalogVersion);
                    packageValues.put("atlas_profile_json", atlasProfile == null ? "{}" : atlasProfile.raw.toString());
                    packageValues.put("atlas_contract_json", atlasContract.toString());
                    packageValues.put("canonical_index_json", canonicalIndex.toString());
                    packageValues.put("migration_profiles_json", migrationProfiles.toString());
                    packageValues.put("atlas_ready", 0);
                    packageValues.put("world_profile", worldProfile);
                    packageValues.put("label", atlasProfile != null ? (atlasProfile.setting.length()>0?atlasProfile.setting+" — Atlas":atlasProfile.logicalMapId) : geometry.getLabel());
                    packageValues.put("image_uri", imageUri);
                    packageValues.put("overlay_uri", overlayUri);
                    packageValues.put("vector_overlay_uri", vectorUri);
                    packageValues.put("vector_overlay_gzip_uri", vectorGzipUri);
                    packageValues.put("hex_index_uri", indexUri);
                    packageValues.put("overlay_mode", overlayMode);
                    packageValues.put("manifest_json", manifestJson);
                    packageValues.put("image_width", atlasProfile != null ? atlasProfile.imageWidth : Math.round(geometry.getImageWidth()));
                    packageValues.put("image_height", atlasProfile != null ? atlasProfile.imageHeight : Math.round(geometry.getImageHeight()));
                    packageValues.put("distance_miles", atlasProfile != null ? atlasProfile.distanceMiles : geometry.getHexScaleMiles());
                    packageValues.put("distance_km", atlasProfile != null ? atlasProfile.distanceMiles*1.609344f : geometry.getHexScaleKm());
                    packageValues.put("min_label_zoom", atlasProfile != null ? 1.6f : geometry.getMinLabelZoom());
                    packageValues.put("imported_at", System.currentTimeMillis());
                    packageValues.put("ready", 0);
                    db.insertOrThrow("map_packages", null, packageValues);

                    stage = "importar o atlas de hexes";
                    progress(callback, "Importando atlas de hexes", 4, 8);
                    requireFile(spec.atlasFile, spec.atlasName);
                    int expectedHexCount = atlasProfile != null ? atlasProfile.hexCount : geometry.size();
                    int hexCount = importAtlas(spec.atlasFile, packageId, expectedHexCount, callback);
                    if (hexCount != expectedHexCount) throw new Exception("Atlas incompleto: " + hexCount + " de " + expectedHexCount + " hexes.");
                    if (atlasProfile != null) {
                        geometry = MapManifestGeometry.fromAtlasDatabase(appDb, packageId, atlasProfile);
                        verifyGeometry(geometry);
                        if (spec.hexIndexFile != null) { requireFile(spec.hexIndexFile, spec.hexIndexFile.getName()); validateHexIndex(spec.hexIndexFile, geometry, callback); }
                    }

                    stage = "importar o catálogo de locais";
                    progress(callback, "Importando catálogo de locais", 5, 8);
                    requireFile(spec.locationsFile, spec.locationsName);
                    int locationCount = importLocations(spec.locationsFile, packageId, callback);
                    if (spec.areasFile != null && spec.areasFile.isFile()) {
                        stage = "importar áreas";
                        importAreas(spec.areasFile, packageId);
                    }
                    if (spec.markersFile != null && spec.markersFile.isFile()) {
                        stage = "importar evidência cartográfica";
                        importMarkers(spec.markersFile, packageId);
                    }

                    stage = "cruzar atlas, índice e locais";
                    progress(callback, "Validando cruzamento por hexId", 6, 8);
                    validateDatabaseSamples(packageId, geometry);
                    ContentValues ready = new ContentValues();
                    ready.put("hex_count", hexCount);
                    ready.put("location_count", locationCount);
                    ready.put("ready", 1);
                    ready.put("atlas_ready", atlasProfile == null ? 0 : 1);
                    db.update("map_packages", ready, "package_id=?", new String[]{packageId});

                    AtlasMigrationEngine.PositionResolution resolved = atlasProfile == null ? null : AtlasMigrationEngine.resolveCampaignPosition(appDb, previous, previousPackage, atlasProfile, migrationProfiles, packageId, geometry);
                    MapManifestGeometry.ManifestHex initial = null;
                    if (atlasProfile == null) {
                        String prevHex = previous.optString("current_hex_id", "").trim();
                        if (prevHex.length() > 0 && geometry.byId(prevHex) != null) {
                            initial = geometry.byId(prevHex);
                        } else {
                            initial = geometry.nearest(geometry.getImageWidth() / 2f, geometry.getImageHeight() / 2f);
                            if (initial == null) initial = geometry.byId("A1");
                        }
                    } else if (resolved != null && resolved.hexId.length() > 0) initial = geometry.byId(resolved.hexId);
                    // Atlas Contract: câmera centralizada NÃO cria posição de campanha. Só restaura posição validada.
                    String currentHexId = atlasProfile != null ? (resolved == null ? "" : resolved.hexId) : (initial == null ? "" : initial.id);
                    float tokenX = resolved != null && resolved.hasPosition ? (float)resolved.u : (atlasProfile != null ? .5f : (initial == null ? .5f : initial.centerX / geometry.getImageWidth()));
                    float tokenY = resolved != null && resolved.hasPosition ? (float)resolved.v : (atlasProfile != null ? .5f : (initial == null ? .5f : initial.centerY / geometry.getImageHeight()));
                    String reveals = new JSONObject().put("hexIds", currentHexId.length() == 0 ? new JSONArray() : new JSONArray().put(currentHexId)).put("points", new JSONArray()).toString();
                    boolean overlayVisible=true;float savedScale=0f,savedOx=0f,savedOy=0f;
                    if(sameMap){
                        JSONObject oldReveals=new JSONObject(previous.optString("reveals_json","{}"));JSONArray oldIds=oldReveals.optJSONArray("hexIds"),keptIds=new JSONArray();if(oldIds!=null&&atlasProfile==null)for(int i=0;i<oldIds.length();i++){String id=oldIds.optString(i);if(geometry.byId(id)!=null)keptIds.put(id);}if(currentHexId.length()>0&&!containsJsonString(keptIds,currentHexId))keptIds.put(currentHexId);reveals=new JSONObject().put("hexIds",keptIds).put("points",new JSONArray()).toString();overlayVisible=previous.optInt("overlay_visible",1)==1;savedScale=(float)previous.optDouble("scale",0);savedOx=(float)previous.optDouble("offset_x",0);savedOy=(float)previous.optDouble("offset_y",0);
                    }
                    appDb.saveMapPackageStateForProfile(worldProfile, imageUri, overlayUri, manifestJson, packageId, currentHexId, overlayVisible, reveals,
                            "pointy-top".equals(geometry.getOrientation()) ? "Hexagonal pointy-top" : "Hexagonal flat-top", geometry.getHexWidth(), previous.optInt("fog_enabled") == 1,
                            tokenX, tokenY, savedScale, savedOx, savedOy);
                    if (atlasProfile != null) {
                        appDb.saveAtlasPositionForProfile(worldProfile, packageId, currentHexId, tokenX, tokenY, atlasProfile.coordinateFrameId, resolved == null ? "ready" : resolved.status, resolved == null ? "{}" : resolved.legacyJson);
                        JSONObject poiMigration = AtlasMigrationEngine.migrateCampaignPois(appDb, atlasProfile, migrationProfiles, packageId);
                        appDb.addAuditEvent("atlas_migration", "CampaignPoi reconciliados pelo contrato do pacote", poiMigration.toString(), "{}", poiMigration.toString(), true);
                    }
                    if(sameMap && (resolved == null || "ready".equals(resolved.status) || "ready_identity_reconciled".equals(resolved.status))){String destination=atlasProfile==null&&geometry.byId(previous.optString("destination_hex_id"))!=null?previous.optString("destination_hex_id"):"";appDb.saveMapTravelStateForProfile(worldProfile,previous.optInt("position_locked",0)==1&&currentHexId.length()>0,destination,previous.optString("route_json","[]"),previous.optString("route_owner","Jogador"));}
                    else appDb.saveMapTravelStateForProfile(worldProfile,false,"","[]","Jogador");
                    appDb.saveMapRenderModeForProfile(worldProfile, overlayMode, atlasProfile != null ? 1.6f : geometry.getMinLabelZoom());
                    pruneObsoletePackages(worldProfile, packageId, READY_PACKAGES_TO_KEEP);

                    progress(callback, "Pacote de mapa pronto", 8, 8);
                    try {
                        NewarreAndWaystoneEngine.install(appDb);
                    } catch (Exception e) {
                        AppLog.w("MapPackage", "Falha tolerada ao instalar Newarre no pacote de mapa.", e);
                    }
                    // The first post-Strixhaven arrival may have been deferred
                    // until the 2025 Atlas finished indexing. A city record and
                    // valid token center are required: no fabricated placement.
                    if ("dnd".equalsIgnoreCase(worldProfile)) {
                        try {
                            android.content.SharedPreferences settings =
                                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                            if (StrixhavenFaerunArrivalEngine.pending(settings)
                                    && !StrixhavenAdventureProfile.isActive(context)) {
                                StrixhavenFaerunArrivalEngine.applyPending(appDb, settings);
                            }
                        } catch (Exception arrivalError) {
                            // A valid imported atlas must NOT be deleted merely
                            // because a separate campaign arrival is pending.
                            AppLog.e("StrixhavenArrival",
                                "A chegada a Yartar sera revalidada na proxima abertura.", arrivalError);
                        }
                    }
                    if (callback != null) callback.onSuccess(new ImportResult(packageId, mapId, worldProfile, atlasProfile != null ? (atlasProfile.setting.length()>0?atlasProfile.setting+" — Atlas":atlasProfile.logicalMapId) : geometry.getLabel(), imageUri,
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
            }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
        });
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static boolean containsJsonString(JSONArray array,String wanted){if(array==null||wanted==null)return false;for(int i=0;i<array.length();i++)if(wanted.equals(array.optString(i)))return true;return false;}

    private List<String> extractAll(Uri uri, File staging, Callback callback) throws Exception {/*BR_JSON_WRAP*/try{
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
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

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

    private int importAtlas(File file, String packageId, int expected, Callback callback) throws Exception {
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
                    insertAtlasBatch(packageId, batch);
                    batch.clear();
                    progress(callback, "Atlas: " + count + " / " + expected + " hexes", count, expected);
                }
            }
            reader.endObject();
        }
        reader.endObject();
        reader.close();
        if (!batch.isEmpty()) insertAtlasBatch(packageId, batch);
        progress(callback, "Atlas: " + count + " / " + expected + " hexes", count, expected);
        return count;
    }

    private void insertAtlasBatch(String packageId, List<AtlasRow> rows) throws Exception {/*BR_JSON_WRAP*/try{
        SQLiteDatabase db = appDb.getWritableDatabase();
        db.beginTransaction();
        try {
            for (AtlasRow row : rows) {
                JSONObject value = row.value;
                JSONObject center = value.optJSONObject("center");
                JSONObject normalized = value.optJSONObject("centerNormalized");
                JSONObject terrain = value.optJSONObject("terrain");
                if (terrain == null) terrain = value.optJSONObject("visualTerrain");
                JSONObject review = value.optJSONObject("review");
                JSONArray locationIds = value.optJSONArray("locationIds");
                JSONArray areaIds = value.optJSONArray("areaIds");
                JSONArray markerIds = value.optJSONArray("cartographicMarkerIds");
                JSONObject neighbors = value.optJSONObject("neighbors");
                JSONObject road = value.optJSONObject("road");
                ContentValues values = new ContentValues();
                values.put("package_id", packageId);
                values.put("hex_id", row.hexId);
                values.put("center_x", center == null ? 0 : center.optDouble("x"));
                values.put("center_y", center == null ? 0 : center.optDouble("y"));
                values.put("center_u", normalized == null ? null : normalized.optDouble("u"));
                values.put("center_v", normalized == null ? null : normalized.optDouble("v"));
                values.put("terrain_primary", terrain == null ? "" : terrain.optString("primary", terrain.optString("classe", "")));
                values.put("terrain_secondary", terrain == null || terrain.optJSONArray("secondary") == null ? "[]" : terrain.optJSONArray("secondary").toString());
                values.put("terrain_confidence", terrain == null ? 0 : terrain.optDouble("confidence", terrain.optDouble("confianca", 0)));
                values.put("terrain_method", terrain == null ? "" : terrain.optString("method", terrain.optString("metodo", "")));
                JSONArray roads=value.optJSONArray("roads");
                if(roads==null&&road!=null){roads=new JSONArray();roads.put(road);}
                values.put("roads_json", roads == null ? "[]" : roads.toString());
                values.put("road_present", road != null && road.optBoolean("presente", false) ? 1 : 0);
                values.put("neighbors_json", neighbors == null ? "{}" : neighbors.toString());
                values.put("area_ids", areaIds == null ? "[]" : areaIds.toString());
                values.put("marker_ids", markerIds == null ? "[]" : markerIds.toString());
                values.put("waterways_json", value.optJSONArray("waterways") == null ? "[]" : value.optJSONArray("waterways").toString());
                values.put("political_json", value.optJSONObject("political") == null ? (value.optJSONArray("political") == null ? "{}" : value.optJSONArray("political").toString()) : value.optJSONObject("political").toString());
                values.put("review_status", review == null ? "" : review.optString("status"));
                values.put("review_reasons", review == null || review.optJSONArray("reasons") == null ? "[]" : review.optJSONArray("reasons").toString());
                values.put("location_ids", locationIds == null ? "[]" : locationIds.toString());
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
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

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

    private void insertLocationBatch(String packageId, List<JSONObject> rows) throws Exception {/*BR_JSON_WRAP*/try{
        SQLiteDatabase db = appDb.getWritableDatabase();
        db.beginTransaction();
        try {
            for (JSONObject location : rows) {
                String id = location.optString("id", "").trim();
                if (id.length() == 0) continue;
                JSONObject packagePlacement = location.optJSONObject("packagePlacement");
                JSONObject canonicalPosition = location.optJSONObject("canonicalPosition");
                JSONObject placement = location.optJSONObject("placement");
                String hexId = location.isNull("hexId") ? "" : location.optString("hexId", "").trim();
                if (hexId.length() == 0 && packagePlacement != null) hexId = packagePlacement.optString("hex_id", "").trim();
                if (hexId.length() == 0 && placement != null && "posicionado".equalsIgnoreCase(placement.optString("status", ""))) hexId = placement.optString("hexId", "").trim();
                String status = placement == null ? location.optString("status", hexId.length()>0?"posicionado":"nao_posicionado") : placement.optString("status", location.optString("status", ""));
                boolean contractLocation = location.has("canonicalPosition") || location.has("packagePlacement") || location.has("logical_map_id");
                if (contractLocation && (canonicalPosition == null || packagePlacement == null)) hexId = ""; // Atlas Contract: sem as duas provas, não inventar posição.
                String locationName=location.optString("name",location.optString("nome",""));
                String locationType=location.optString("type",location.optString("tipo",""));
                String locationRegion=location.optString("region","");
                if(locationRegion.length()==0){JSONArray regions=location.optJSONArray("regioes");if(regions==null)regions=location.optJSONArray("regiao");if(regions==null)regions=location.optJSONArray("regions");if(regions!=null){StringBuilder rb=new StringBuilder();for(int ri=0;ri<regions.length();ri++){String rv=regions.optString(ri,"");if(rv.length()>0){if(rb.length()>0)rb.append("; ");rb.append(rv);}}locationRegion=rb.toString();}else locationRegion=location.optString("regiao","");}
                JSONObject norm = placement != null ? placement.optJSONObject("normalizado") : null;
                ContentValues values = new ContentValues();
                values.put("package_id", packageId);
                values.put("logical_map_id", location.optString("logical_map_id", ""));
                values.put("coordinate_frame_id", canonicalPosition == null ? "" : canonicalPosition.optString("coordinate_frame_id", ""));
                if(canonicalPosition!=null){values.put("position_u",canonicalPosition.optDouble("u"));values.put("position_v",canonicalPosition.optDouble("v"));}
                else if(norm!=null){values.put("position_u",norm.optDouble("u"));values.put("position_v",norm.optDouble("v"));}
                values.put("canonical",1);
                values.put("package_placement_json",packagePlacement==null?"{}":packagePlacement.toString());
                values.put("location_id", id);
                values.put("name", locationName);
                values.put("type", locationType);
                values.put("hex_id", hexId);
                values.put("region", locationRegion);
                values.put("official_5e", location.optBoolean("official5e") ? 1 : 0);
                values.put("scope", location.optString("scope"));
                values.put("placement_method", placement == null ? (canonicalPosition==null?"":canonicalPosition.optString("evidenceMethod","")) : placement.optString("method",placement.optString("metodo","")));
                values.put("placement_confidence", canonicalPosition == null ? (placement == null ? 0d : placement.optDouble("confidence",placement.optDouble("confianca",0d))) : canonicalPosition.optDouble("confidence",0d));
                values.put("placement_status", status);
                values.put("source_ids", location.optJSONArray("sourceIds") == null ? "[]" : location.optJSONArray("sourceIds").toString());
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
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int importAreas(File file,String packageId) throws Exception {/*BR_JSON_WRAP*/try{
        if(file==null||!file.isFile())throw new Exception("faerun_2025_areas_multihex_v1.json ausente.");JSONObject root=new JSONObject(readText(file));JSONArray areas=root.optJSONArray("areas");if(areas==null)throw new Exception("Dataset de áreas inválido.");SQLiteDatabase db=appDb.getWritableDatabase();db.beginTransaction();try{for(int i=0;i<areas.length();i++){JSONObject area=areas.optJSONObject(i);if(area==null)continue;String id=area.optString("id","");if(id.length()==0)continue;ContentValues a=new ContentValues();a.put("package_id",packageId);a.put("area_id",id);a.put("name",area.optString("nome",area.optString("name","")));a.put("type",area.optString("tipo",area.optString("type","")));a.put("confidence",area.optDouble("confianca",area.optDouble("confidence",0)));a.put("raw_json",area.toString());db.insertWithOnConflict("map_areas",null,a,SQLiteDatabase.CONFLICT_REPLACE);JSONArray hexes=area.optJSONArray("hexIds");if(hexes!=null)for(int h=0;h<hexes.length();h++){String hex=hexes.optString(h,"");if(hex.length()==0)continue;ContentValues j=new ContentValues();j.put("package_id",packageId);j.put("area_id",id);j.put("hex_id",hex);db.insertWithOnConflict("map_area_hex",null,j,SQLiteDatabase.CONFLICT_IGNORE);}}db.setTransactionSuccessful();}finally{db.endTransaction();}return areas.length();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private int importMarkers(File file,String packageId) throws Exception {/*BR_JSON_WRAP*/try{
        if(file==null||!file.isFile())throw new Exception("faerun_2025_cartographic_markers_v1.json ausente.");JSONObject root=new JSONObject(readText(file));JSONArray markers=root.optJSONArray("markers");if(markers==null)throw new Exception("Dataset de marcadores inválido.");SQLiteDatabase db=appDb.getWritableDatabase();db.beginTransaction();try{for(int i=0;i<markers.length();i++){JSONObject marker=markers.optJSONObject(i);if(marker==null)continue;String id=marker.optString("id","");if(id.length()==0)continue;JSONObject n=marker.optJSONObject("normalizado");ContentValues v=new ContentValues();v.put("package_id",packageId);v.put("marker_id",id);v.put("marker_type",marker.optString("tipo","marcador_cartografico"));v.put("hex_id",marker.optString("hexId",""));if(n!=null){v.put("position_u",n.optDouble("u"));v.put("position_v",n.optDouble("v"));}v.put("status",marker.optString("status","anonymous"));v.put("raw_json",marker.toString());db.insertWithOnConflict("map_cartographic_markers",null,v,SQLiteDatabase.CONFLICT_REPLACE);}db.setTransactionSuccessful();}finally{db.endTransaction();}return markers.length();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void validateDatabaseSamples(String packageId, MapManifestGeometry geometry) throws Exception {
        MapManifestGeometry.ManifestHex center = geometry.nearest(geometry.getImageWidth()/2f, geometry.getImageHeight()/2f);
        String[] ids = new String[]{"A1", "B1", center == null ? "A1" : center.id};
        for (String id : ids) {
            MapManifestGeometry.ManifestHex expected = geometry.byId(id);
            JSONObject row = appDb.getRawMapHex(packageId, id);
            if (expected == null || row.length() == 0) throw new Exception("Cruzamento ausente no hex " + id + ".");
            if (Math.abs(row.optDouble("center_x") - expected.centerX) > .01 || Math.abs(row.optDouble("center_y") - expected.centerY) > .01)
                throw new Exception("Centro do atlas divergente no hex " + id + ".");
        }
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
        MapManifestGeometry.ManifestHex a1 = geometry.byId("A1");
        if (a1 == null) throw new Exception("Geometria sem A1.");
        MapManifestGeometry.ManifestHex nearA1 = geometry.nearest(a1.centerX, a1.centerY);
        if (nearA1 == null || !"A1".equals(nearA1.id)) throw new Exception("Snap por coordenadas falhou em A1.");
        if ("mystara".equals(geometry.getWorldProfile())) {
            MapManifestGeometry.ManifestHex b1 = geometry.byId("B1");
            if (geometry.size() != 71726 || geometry.getColumns() != 297 || geometry.getRowsEven() != 242 || geometry.getRowsOdd() != 241)
                throw new Exception("Geometria Mystara inesperada: " + geometry.size() + " hexes, " + geometry.getColumns() + " colunas.");
            if (Math.abs(a1.centerX - 4f) > .01f || Math.abs(a1.centerY - 4f) > .01f) throw new Exception("A1 de Mystara não está em (4,4).");
            if (b1 == null || Math.abs(b1.centerX - 31f) > .01f || Math.abs(b1.centerY - 19f) > .01f) throw new Exception("B1 de Mystara não está em (31,19).");
            MapManifestGeometry.ManifestHex nearB1 = geometry.nearest(31f, 19f);
            if (nearB1 == null || !"B1".equals(nearB1.id)) throw new Exception("Snap por coordenadas falhou em B1.");
        }
    }

    private static Object readJsonValue(JsonReader reader) throws Exception {/*BR_JSON_WRAP*/try{
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
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

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
        final File directory;
        final String mapName;
        final String atlasName;
        final String locationsName;
        final String rasterName;
        final String vectorName;
        final String vectorGzipName;
        final String indexName;
        final String areasName;
        final String markersName;
        final File mapFile;
        final File atlasFile;
        final File locationsFile;
        final File rasterOverlayFile;
        final File vectorOverlayFile;
        final File vectorOverlayGzipFile;
        final File hexIndexFile;
        final File areasFile;
        final File markersFile;

        PackageSpec(File directory, String mapName, String atlasName, String locationsName, String rasterName,
                    String vectorName, String vectorGzipName, String indexName, String areasName, String markersName) {
            this.directory = directory;
            this.mapName = mapName;
            this.atlasName = atlasName;
            this.locationsName = locationsName;
            this.rasterName = rasterName;
            this.vectorName = vectorName;
            this.vectorGzipName = vectorGzipName;
            this.indexName = indexName;
            this.areasName = areasName;
            this.markersName = markersName;
            mapFile = file(directory, mapName);
            atlasFile = file(directory, atlasName);
            locationsFile = file(directory, locationsName);
            rasterOverlayFile = file(directory, rasterName);
            vectorOverlayFile = file(directory, vectorName);
            vectorOverlayGzipFile = file(directory, vectorGzipName);
            hexIndexFile = file(directory, indexName);
            areasFile = file(directory, areasName);
            markersFile = file(directory, markersName);
        }

        PackageSpec rebase(File directory) { return new PackageSpec(directory, mapName, atlasName, locationsName, rasterName, vectorName, vectorGzipName, indexName, areasName, markersName); }

        static PackageSpec from(JSONObject root, MapManifestGeometry geometry, AtlasMapProfile atlasProfile, File directory) throws Exception {
            JSONObject sourceMap = root.optJSONObject("sourceMap");
            JSONObject dataFiles = root.optJSONObject("dataFiles");
            String map = sourceMap == null ? "" : sourceMap.optString("filename", "");
            String raster = sourceMap == null ? "" : sourceMap.optString("numberOverlay", "");
            String vector = "";
            String vectorGzip = "";
            JSONArray layers = root.optJSONArray("layers");
            if (layers != null) for (int i = 0; i < layers.length(); i++) {
                JSONObject layer = layers.optJSONObject(i);
                if (layer == null) continue;
                String type = layer.optString("type", "").toLowerCase(Locale.US);
                if (map.length() == 0 && "image".equals(type)) map = layer.optString("file", "");
                if (type.contains("svg")) { vector = layer.optString("file", vector); vectorGzip = layer.optString("compressedFile", vectorGzip); }
                if (type.contains("image-overlay")) raster = layer.optString("file", raster);
            }
            if (map.length() == 0 && atlasProfile != null) map = atlasProfile.imageFile;
            if (map.length() == 0) map = existing(directory, "map.jpg", "map.png", "map_mystara_known_world_8014x7235.png");
            String atlas = dataFiles == null ? "" : dataFiles.optString("hexAtlas", dataFiles.optString("terrainAtlas", ""));
            String locations = dataFiles == null ? "" : dataFiles.optString("locations", "");
            String index = dataFiles == null ? "" : dataFiles.optString("hexIndex", "");
            String areas = dataFiles == null ? "" : dataFiles.optString("areas", "");
            String markers = dataFiles == null ? "" : dataFiles.optString("cartographicMarkers", "");
            if (atlasProfile != null) {
                if (atlas.length() == 0) throw new Exception("Atlas Contract sem dataFiles.hexAtlas/terrainAtlas declarado.");
                if (locations.length() == 0) throw new Exception("Atlas Contract sem dataFiles.locations declarado.");
            } else {
                if (atlas.length() == 0) atlas = existing(directory, "mystara_hex_terrain_atlas.json");
                if (locations.length() == 0) locations = existing(directory, "mystara_locations_starter.json");
            }
            if (raster.length() == 0) raster = existingOptional(directory, "hex_ids_overlay.png", "hex_ids_overlay_transparente.png");
            if (vector.length() == 0) vector = existingOptional(directory, "hex_ids_overlay_vector.svg");
            if (vectorGzip.length() == 0) vectorGzip = existingOptional(directory, "hex_ids_overlay_vector.svgz");
            if (index.length() == 0 && atlasProfile == null && geometry != null && "mystara".equals(geometry.getWorldProfile())) index = existing(directory, "hex_index_mystara.json");
            return new PackageSpec(directory, map, atlas, locations, raster, vector, vectorGzip, index, areas, markers);
        }

        private static File file(File dir, String name) { return name == null || name.trim().length() == 0 ? null : new File(dir, new File(name).getName()); }
        private static String existing(File dir, String... names) throws Exception { for (String name : names) if (new File(dir, name).isFile()) return name; throw new Exception("Arquivo esperado não encontrado: " + java.util.Arrays.toString(names)); }
        private static String existingOptional(File dir, String... names) { for (String name : names) if (new File(dir, name).isFile()) return name; return ""; }
    }
}
