package com.braseiro.pathfinder2e;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Importação retomável e atômica da biblioteca de áudio para armazenamento interno. */
public final class AudioLibraryImportWorker extends Worker {
    public static final String UNIQUE_WORK = "braseiro-audio-library-v3";
    public static final String KEY_TREE_URI = "tree_uri";
    public static final String KEY_IMPORTED = "imported";
    public static final String KEY_SKIPPED = "skipped";
    public static final String KEY_FAILED = "failed";
    private static final String CHANNEL = "braseiro_audio_import";
    private static final int NOTIFICATION_ID = 4203;
    private static final int MAX_FILES = 50000;
    private static final int MAX_DEPTH = 32;
    private static final long MAX_SINGLE_AUDIO_BYTES = 512L * 1024L * 1024L;
    private static final long MAX_TOTAL_IMPORT_BYTES = 8L * 1024L * 1024L * 1024L;

    private final Context context;
    private final File audioDir;
    private final File indexFile;
    private final File catalogSource;
    private final File overridesSource;
    private final File checkpointFile;
    private final AudioCatalogDb jobDb;
    private final JSONArray index = new JSONArray();
    private final Set<String> signatures = new HashSet<String>();
    private int imported;
    private int skipped;
    private int failed;
    private int visited;
    private long totalCopiedBytes;

    public AudioLibraryImportWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
        context = appContext.getApplicationContext();
        audioDir = new File(context.getFilesDir(), "audio_library_v3");
        indexFile = new File(context.getFilesDir(), "audio_index_v3.json");
        catalogSource = new File(context.getFilesDir(), "audio_catalog_source_v3.json");
        overridesSource = new File(context.getFilesDir(), "audio_overrides_source_v3.json");
        checkpointFile = new File(context.getFilesDir(), "audio_import_checkpoint_v3.json");
        jobDb = new AudioCatalogDb(context);
    }

    @NonNull @Override public Result doWork() {
        String raw = getInputData().getString(KEY_TREE_URI);
        if (raw == null || raw.trim().isEmpty()) return Result.failure(error("Pasta de áudio ausente."));
        Uri tree = Uri.parse(raw);
        boolean allowed = false;
        for (android.content.UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.isReadPermission() && permission.getUri().equals(tree)) { allowed = true; break; }
        }
        if (!allowed) return Result.failure(error("A permissão persistente da pasta foi perdida."));
        final String jobId=getId().toString();
        try {
            setForegroundAsync(foreground("Preparando a biblioteca sonora…", true)).get();
            prepareDestination(jobId);
            jobDb.updateImportJob(jobId,raw,"running",imported,skipped,failed,"Preparando",null,false);
            DocumentFile root = DocumentFile.fromTreeUri(context, tree);
            if (root == null || !root.exists() || !root.isDirectory()) {
                jobDb.updateImportJob(jobId,raw,"failed",imported,skipped,failed,"", "O Android não conseguiu abrir a pasta.",true);
                return Result.failure(error("O Android não conseguiu abrir a pasta."));
            }
            walk(root, "", 0);
            writeAtomic(indexFile, index.toString().getBytes(StandardCharsets.UTF_8));
            cleanup(checkpointFile);
            jobDb.updateImportJob(jobId,raw,"completed",imported,skipped,failed,"Concluído",null,true);
            setProgressAsync(progress("Concluído", imported, skipped, failed));
            setForegroundAsync(foreground("Biblioteca sonora concluída", false)).get();
            return Result.success(progress("Concluído", imported, skipped, failed));
        } catch (Throwable failure) {
            AppLog.e("AudioImport", "Importação interrompida.", failure);
            String message=failure.getMessage() == null ? "Falha na importação." : failure.getMessage();
            boolean retry=getRunAttemptCount() < 2 && !isStopped();
            jobDb.updateImportJob(jobId,raw,retry?"retry":"failed",imported,skipped,failed,"",message,!retry);
            if (retry) return Result.retry();
            return Result.failure(error(message));
        }
    }

    private void walk(DocumentFile node, String relative, int depth) throws Exception {
        if (isStopped()) throw new InterruptedException("Importação cancelada.");
        if (depth > MAX_DEPTH || visited > MAX_FILES) throw new Exception("A pasta excede o limite de segurança.");
        if (node.isDirectory()) {
            DocumentFile[] children;
            try { children = node.listFiles(); } catch (Throwable error) { failed++; return; }
            for (DocumentFile child : children) walk(child, join(relative, child.getName()), depth + 1);
            return;
        }
        visited++;
        String name = node.getName() == null ? "arquivo" : node.getName();
        String lower = name.toLowerCase(Locale.ROOT);
        try {
            if (lower.equals("biblioteca_sons.json")) {
                copySmallText(node, catalogSource); skipped++; return;
            }
            if (lower.equals("audio_overrides.json")) {
                copySmallText(node, overridesSource); skipped++; return;
            }
            if (lower.endsWith(".zip") || (node.getType() != null && node.getType().contains("zip"))) {
                try (InputStream raw = context.getContentResolver().openInputStream(node.getUri())) {
                    if (raw == null) throw new Exception("ZIP não pôde ser aberto.");
                    importZip(raw);
                }
                return;
            }
            if (!isAudio(name, node.getType())) { skipped++; return; }
            try (InputStream input = context.getContentResolver().openInputStream(node.getUri())) {
                if (input == null) throw new Exception("Arquivo não pôde ser aberto.");
                saveAudio(name, relative, node.getType(), node.length(), input);
            }
        } catch (Throwable error) {
            failed++;
            AppLog.w("AudioImport", "Falha em " + name, error);
        }
        if (visited % 8 == 0) setProgressAsync(progress(name, imported, skipped, failed));
    }

    private void importZip(InputStream source) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(source, 128 * 1024))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (isStopped()) throw new InterruptedException("Importação cancelada.");
                if (entry.isDirectory()) { zip.closeEntry(); continue; }
                visited++;
                if (visited > MAX_FILES) throw new Exception("O ZIP excede o limite de " + MAX_FILES + " arquivos.");
                if (entry.getSize() > MAX_SINGLE_AUDIO_BYTES) throw new Exception("Arquivo de áudio do ZIP excede 512 MB: " + entry.getName());
                String name = new File(entry.getName()).getName();
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.equals("biblioteca_sons.json")) writeAtomic(catalogSource, readLimited(zip, 16 * 1024 * 1024));
                else if (lower.equals("audio_overrides.json")) writeAtomic(overridesSource, readLimited(zip, 16 * 1024 * 1024));
                else if (isAudio(name, "audio/*")) saveAudio(name, entry.getName(), "audio/*", entry.getSize(), zip);
                else skipped++;
                zip.closeEntry();
            }
        }
    }

    private void saveAudio(String originalName, String relative, String mime, long expectedSize, InputStream input) throws Exception {
        if (expectedSize > MAX_SINGLE_AUDIO_BYTES) throw new Exception("Arquivo de áudio excede o limite de 512 MB.");
        ensureSpace(Math.max(0L, expectedSize));
        String normalized = safeFileName(originalName);
        String signature = safe(relative).toLowerCase(Locale.ROOT) + "|" + normalized.toLowerCase(Locale.ROOT) + "|" + Math.max(0L, expectedSize);
        if (!signatures.add(signature)) { skipped++; return; }
        String extension = normalized.contains(".") ? normalized.substring(normalized.lastIndexOf('.') + 1) : "bin";
        String id = "audio_" + digestText(signature).substring(0,24);
        File partial = new File(audioDir, id + "." + extension + ".partial");
        File target = new File(audioDir, id + "." + extension);
        long copied = 0L;
        boolean completed = false;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try {
            cleanup(partial);
            cleanup(target);
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(partial), 128 * 1024)) {
                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (isStopped()) throw new InterruptedException("Importação cancelada.");
                    if (read == 0) continue;
                    copied += read;
                    if (copied > MAX_SINGLE_AUDIO_BYTES) throw new Exception("Arquivo de áudio excede o limite de 512 MB.");
                    if (totalCopiedBytes + copied > MAX_TOTAL_IMPORT_BYTES) throw new Exception("A importação excede o limite de segurança de 8 GB.");
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    if (copied % (16L * 1024L * 1024L) < buffer.length) ensureSpace(0L);
                }
                out.flush();
            }
            try (FileOutputStream sync = new FileOutputStream(partial, true)) { sync.getFD().sync(); }
            if (copied < 32L) throw new Exception("Arquivo de áudio vazio.");
            if (expectedSize > 0L && copied != expectedSize) throw new Exception("Arquivo incompleto: esperado " + expectedSize + " bytes, recebido " + copied + ".");
            if (!partial.renameTo(target)) {
                copyFile(partial, target);
                partial.delete();
            }
            long duration = duration(target);
            JSONObject item = AudioFileClassifierV3.classify(normalized, copied, duration);
            item.put("id", id);
            item.put("uri", Uri.fromFile(target).toString());
            item.put("name", normalized);
            item.put("path", relative == null || relative.isEmpty() ? normalized : relative);
            item.put("mime", mime == null ? "audio/*" : mime);
            item.put("size", copied);
            item.put("modified", System.currentTimeMillis());
            item.put("signature", hex(digest.digest()));
            item.put("source_key", signature);
            index.put(item);
            totalCopiedBytes += copied;
            imported++;
            completed = true;
            if(imported%4==0)writeCheckpoint(getId().toString());
            jobDb.updateImportJob(getId().toString(),getInputData().getString(KEY_TREE_URI),"running",imported,skipped,failed,normalized,null,false);
        } finally {
            cleanup(partial);
            if (!completed) { cleanup(target); signatures.remove(signature); }
        }
    }

    private void prepareDestination(String jobId) throws Exception {
        boolean resumed=false;
        if(checkpointFile.isFile()){
            try{
                JSONObject checkpoint=new JSONObject(new String(readFile(checkpointFile,32*1024*1024),StandardCharsets.UTF_8));
                if(jobId.equals(checkpoint.optString("job_id"))){
                    JSONArray items=checkpoint.optJSONArray("items");
                    if(items!=null)for(int i=0;i<items.length();i++){
                        JSONObject item=items.optJSONObject(i);if(item==null)continue;
                        String uri=item.optString("uri","");File file=uri.startsWith("file:")?new File(Uri.parse(uri).getPath()):null;
                        if(file==null||!file.isFile())continue;
                        index.put(item);totalCopiedBytes+=Math.max(0L,item.optLong("size",0L));String sourceKey=item.optString("source_key","");if(sourceKey.length()>0)signatures.add(sourceKey);
                    }
                    imported=index.length();resumed=true;
                }
            }catch(Throwable ignored){resumed=false;}
        }
        if(!resumed)resetDestination();
        else{
            if(!audioDir.isDirectory()&&!audioDir.mkdirs())throw new Exception("Não foi possível reabrir o armazenamento interno de áudio.");
            File[] partials=audioDir.listFiles();if(partials!=null)for(File file:partials)if(file.getName().endsWith(".partial"))file.delete();
        }
        writeCheckpoint(jobId);
    }

    private void resetDestination() throws Exception {
        if (audioDir.exists()) deleteTree(audioDir);
        if (!audioDir.mkdirs() && !audioDir.isDirectory()) throw new Exception("Não foi possível criar o armazenamento interno de áudio.");
        cleanup(indexFile); cleanup(catalogSource); cleanup(overridesSource);
        cleanup(new File(context.getFilesDir(), "audio_keywords_v3.json"));
        cleanup(checkpointFile);
        signatures.clear();while(index.length()>0)index.remove(index.length()-1);imported=skipped=failed=visited=0;totalCopiedBytes=0L;
    }

    private void writeCheckpoint(String jobId) throws Exception {
        JSONObject checkpoint=new JSONObject();checkpoint.put("job_id",jobId).put("updated_at",System.currentTimeMillis()).put("items",index);
        writeAtomic(checkpointFile,checkpoint.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] readFile(File file,int limit) throws Exception {
        try(InputStream input=new FileInputStream(file)){return readLimited(input,limit);}
    }

    private static String digestText(String value) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");return hex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String safe(String value){return value==null?"":value;}

    private long duration(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Throwable ignored) { return 0L; }
        finally { try { retriever.release(); } catch(Throwable ignored){AppLog.ignored("AudioLibraryImportWorker","Falha tolerada; a execução continuou.",ignored);} }
    }

    private void copySmallText(DocumentFile source, File target) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(source.getUri())) {
            if (input == null) throw new Exception("JSON não pôde ser aberto.");
            writeAtomic(target, readLimited(input, 16 * 1024 * 1024));
        }
    }

    private void ensureSpace(long incoming) throws Exception {
        long required = Math.max(32L * 1024L * 1024L, incoming + 16L * 1024L * 1024L);
        if (context.getFilesDir().getUsableSpace() < required) throw new Exception("Espaço interno insuficiente para copiar a biblioteca.");
    }

    private ForegroundInfo foreground(String text, boolean ongoing) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Importação de áudio", NotificationManager.IMPORTANCE_LOW));
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Braseiro — biblioteca de áudio")
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .setProgress(0, 0, ongoing);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(
                    NOTIFICATION_ID,
                    builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        }
        return new ForegroundInfo(NOTIFICATION_ID, builder.build());
    }

    private static Data progress(String current, int imported, int skipped, int failed) {
        return new Data.Builder().putString("current", current).putInt(KEY_IMPORTED, imported).putInt(KEY_SKIPPED, skipped).putInt(KEY_FAILED, failed).build();
    }
    private static Data error(String message) { return new Data.Builder().putString("error", message).build(); }
    private static boolean isAudio(String name, String mime) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return (mime != null && mime.startsWith("audio/")) || lower.matches(".*\\.(mp3|wav|ogg|m4a|aac|flac|opus|webm)$");
    }
    private static String join(String path, String name) { if (name == null) name = ""; return path == null || path.isEmpty() ? name : path + "/" + name; }
    private static String safeFileName(String name) { return (name == null ? "audio.bin" : name).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").replaceAll("\\s+", " ").trim(); }
    private static void cleanup(File file) { try { if (file.exists()) file.delete(); } catch(Throwable ignored){AppLog.ignored("AudioLibraryImportWorker","Falha tolerada; a execução continuou.",ignored);} }
    private static void deleteTree(File file) { File[] children = file.listFiles(); if (children != null) for (File child : children) { if (child.isDirectory()) deleteTree(child); else child.delete(); } file.delete(); }
    private static void copyFile(File source, File target) throws Exception { try (FileInputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(target)) { byte[] b = new byte[128 * 1024]; int n; while ((n = in.read(b)) >= 0) if (n > 0) out.write(b, 0, n); out.getFD().sync(); } }
    private static byte[] readLimited(InputStream input, int max) throws Exception { ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[32768]; int total = 0, n; while ((n = input.read(b)) >= 0) { if (n == 0) continue; total += n; if (total > max) throw new Exception("JSON excede o limite de segurança."); out.write(b, 0, n); } return out.toByteArray(); }
    private static void writeAtomic(File target, byte[] bytes) throws Exception { File parent = target.getParentFile(); if (parent != null && !parent.exists()) parent.mkdirs(); File temp = new File(target.getAbsolutePath() + ".partial"); try (FileOutputStream out = new FileOutputStream(temp)) { out.write(bytes); out.getFD().sync(); } if (target.exists() && !target.delete()) throw new Exception("Não foi possível substituir " + target.getName()); if (!temp.renameTo(target)) { copyFile(temp, target); temp.delete(); } }
    private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte value : bytes) out.append(String.format(Locale.US, "%02x", value)); return out.toString(); }
}
