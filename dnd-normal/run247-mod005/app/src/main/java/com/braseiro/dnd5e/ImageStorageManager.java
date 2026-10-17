package com.braseiro.dnd5e;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/** Cópia durável, limitada e decodificação amostrada de imagens recebidas. */
public final class ImageStorageManager {
    public static final long MAX_INPUT_BYTES = 24L * 1024L * 1024L;
    private static final long MAX_PIXELS = 48_000_000L;
    private static final int BUFFER = 64 * 1024;

    private ImageStorageManager() {}

    public static Uri importImage(Context context, Uri source, String folder, String baseName, int keepNewest) throws Exception {
        if (context == null || source == null) throw new Exception("Imagem ausente.");
        Context app = context.getApplicationContext();
        ContentResolver resolver = context.getContentResolver();
        String mime = null;
        try { mime = resolver.getType(source); } catch (Exception ignored) {}
        if (mime == null || mime.trim().length() == 0) {
            try { mime = app.getContentResolver().getType(source); } catch (Exception ignored) {}
        }
        if (mime == null || mime.trim().length() == 0) mime = mimeFromUri(source);
        try (InputStream input = open(context, source)) {
            return store(app, input, mime, folder, baseName, keepNewest);
        }
    }

    public static Uri importImage(Context context, InputStream source, String mime, String folder, String baseName, int keepNewest) throws Exception {
        if (context == null || source == null) throw new Exception("Imagem ausente.");
        Context app = context.getApplicationContext();
        try (InputStream input = source instanceof BufferedInputStream ? source : new BufferedInputStream(source)) {
            return store(app, input, mime, folder, baseName, keepNewest);
        }
    }

    private static Uri store(Context app, InputStream input, String mime, String folder, String baseName, int keepNewest) throws Exception {
        if (!input.markSupported()) input = new BufferedInputStream(input);
        String normalizedMime = normalizeMimeOrDetect(input, mime);
        String extension = extensionForMime(normalizedMime);
        File directory = new File(app.getFilesDir(), safe(folder));
        if (!directory.exists() && !directory.mkdirs() && !directory.exists()) throw new Exception("Não foi possível preparar a pasta de imagens.");
        String stem = safe(baseName) + "_" + System.currentTimeMillis();
        File temporary = new File(directory, stem + ".tmp");
        File target = new File(directory, stem + "." + extension);
        long copied = 0L;
        try (FileOutputStream fileOutput = new FileOutputStream(temporary);
             OutputStream output = new BufferedOutputStream(fileOutput)) {
            byte[] buffer = new byte[BUFFER];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count == 0) continue;
                copied += count;
                if (copied > MAX_INPUT_BYTES) throw new Exception("Imagem maior que 24 MB.");
                output.write(buffer, 0, count);
            }
            output.flush();
            fileOutput.getFD().sync();
        } catch (Exception error) {
            if (temporary.exists() && !temporary.delete()) AppLog.w("ImageStorage", "Falha ao remover imagem temporária.");
            throw error;
        }
        validateDimensions(temporary);
        if (!temporary.renameTo(target)) {
            copyFile(temporary, target);
            if (!temporary.delete()) AppLog.w("ImageStorage", "Falha ao limpar arquivo temporário após cópia.");
        }
        cleanup(directory, Math.max(1, keepNewest), target);
        return Uri.fromFile(target);
    }

    public static Bitmap decodeSampled(Context context, Uri uri, int requestedWidth, int requestedHeight) throws Exception {
        if (uri == null) return null;
        int width = Math.max(256, requestedWidth);
        int height = Math.max(256, requestedHeight);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = open(context, uri)) { BitmapFactory.decodeStream(input, null, bounds); }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new Exception("Imagem inválida ou corrompida.");
        int baseSample = sample(bounds.outWidth, bounds.outHeight, width, height);
        for (int attempt = 0; attempt < 3; attempt++) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, baseSample << attempt);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            try (InputStream input = open(context, uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
                if (bitmap != null) return bitmap;
            } catch (OutOfMemoryError memoryError) {
                AppLog.w("ImageStorage", "Memória insuficiente ao decodificar; tentando amostra maior.", memoryError);
                System.gc();
            }
        }
        throw new Exception("Não foi possível decodificar a imagem com segurança.");
    }

    public static boolean isReadable(Context context, Uri uri) {
        try (InputStream input = open(context, uri)) { return input.read() >= 0; }
        catch (Exception error) { return false; }
    }

    /** Remove imagens de cena do formato antigo sem apagar a referência ainda em uso. */
    public static void cleanupLegacySceneImages(Context context, Uri protectedUri) {
        if (context == null) return;
        String protectedPath = protectedUri != null && "file".equalsIgnoreCase(protectedUri.getScheme()) ? protectedUri.getPath() : "";
        File[] files = context.getFilesDir().listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (!file.isFile() || !(name.startsWith("scene_") || name.startsWith("shared_scene_"))) continue;
            if (protectedPath != null && protectedPath.equals(file.getAbsolutePath())) continue;
            if (!file.delete()) AppLog.w("ImageStorage", "Falha ao remover imagem antiga: " + name);
        }
    }

    private static InputStream open(Context context, Uri uri) throws Exception {
        if (context == null || uri == null) throw new Exception("Imagem ausente.");
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            if (path == null) throw new Exception("Caminho da imagem inválido.");
            File f = new File(path);
            for (int i = 0; i < 3; i++) {
                if (f.exists() && f.canRead()) break;
                try { Thread.sleep(120L); } catch (InterruptedException ignored) {}
            }
            return new BufferedInputStream(new FileInputStream(f));
        }
        Exception lastError = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                InputStream input = context.getContentResolver().openInputStream(uri);
                if (input != null) return new BufferedInputStream(input);
            } catch (Exception ex) {
                lastError = ex;
            }
            if (context.getApplicationContext() != null && context.getApplicationContext() != context) {
                try {
                    InputStream input = context.getApplicationContext().getContentResolver().openInputStream(uri);
                    if (input != null) return new BufferedInputStream(input);
                } catch (Exception ex) {
                    lastError = ex;
                }
            }
            try {
                android.content.res.AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                if (afd != null) {
                    InputStream stream = afd.createInputStream();
                    if (stream != null) return new BufferedInputStream(stream);
                }
            } catch (Exception ex) {
                lastError = ex;
            }
            try { Thread.sleep(120L); } catch (InterruptedException ignored) {}
        }
        if (lastError != null) throw lastError;
        throw new Exception("Imagem indisponível.");
    }

    private static String normalizeMimeOrDetect(InputStream input, String mime) throws Exception {
        String declared = mime == null ? "" : mime.toLowerCase(Locale.US).trim();
        input.mark(32);byte[] header=new byte[16];int count=input.read(header);input.reset();
        String detected=detectMime(header,count);
        if(detected.length()==0)throw new Exception("Formato não aceito. Use JPEG, PNG ou WebP.");
        if(declared.startsWith("image/")&&!declared.contains("jpeg")&&!declared.contains("jpg")&&!declared.contains("png")&&!declared.contains("webp"))throw new Exception("Formato não aceito. Use JPEG, PNG ou WebP.");
        if(declared.length()>0&&declared.startsWith("image/")&&!compatibleMime(declared,detected))AppLog.w("ImageStorage","MIME declarado "+declared+" difere do conteúdo "+detected+"; usando o conteúdo real.");
        return detected;
    }

    private static boolean compatibleMime(String declared,String detected){
        if(declared.contains("jpeg")||declared.contains("jpg"))return "image/jpeg".equals(detected);
        if(declared.contains("png"))return "image/png".equals(detected);
        if(declared.contains("webp"))return "image/webp".equals(detected);
        return false;
    }

    private static String detectMime(byte[] header,int count){
        if(count>=8&&(header[0]&255)==0x89&&header[1]=='P'&&header[2]=='N'&&header[3]=='G'&&header[4]==13&&header[5]==10&&header[6]==26&&header[7]==10)return "image/png";
        if(count>=3&&(header[0]&255)==0xff&&(header[1]&255)==0xd8&&(header[2]&255)==0xff)return "image/jpeg";
        if(count>=12&&header[0]=='R'&&header[1]=='I'&&header[2]=='F'&&header[3]=='F'&&header[8]=='W'&&header[9]=='E'&&header[10]=='B'&&header[11]=='P')return "image/webp";
        return "";
    }

    private static void validateDimensions(File file) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            if (file.exists() && !file.delete()) AppLog.w("ImageStorage", "Falha ao remover imagem inválida.");
            throw new Exception("A imagem está corrompida ou não é suportada.");
        }
        if ((long) options.outWidth * (long) options.outHeight > MAX_PIXELS) {
            if (file.exists() && !file.delete()) AppLog.w("ImageStorage", "Falha ao remover imagem com dimensões excessivas.");
            throw new Exception("Imagem com resolução excessiva. Limite: 48 megapixels.");
        }
    }

    private static int sample(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        int sample = 1;
        while (sourceWidth / (sample * 2) >= targetWidth && sourceHeight / (sample * 2) >= targetHeight) sample *= 2;
        return Math.max(1, sample);
    }

    private static String mimeFromUri(Uri uri) {
        String path = uri == null ? "" : String.valueOf(uri.getPath());
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        return extension == null ? "" : MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
    }

    private static String extensionForMime(String mime) {
        if (mime.contains("png")) return "png";
        if (mime.contains("webp")) return "webp";
        return "jpg";
    }

    private static String safe(String value) {
        String out = value == null ? "image" : value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return out.length() == 0 ? "image" : out;
    }

    private static void copyFile(File source, File target) throws Exception {
        try (InputStream input = new BufferedInputStream(new FileInputStream(source));
             FileOutputStream fileOutput = new FileOutputStream(target);
             OutputStream output = new BufferedOutputStream(fileOutput)) {
            byte[] buffer = new byte[BUFFER];
            int count;
            while ((count = input.read(buffer)) >= 0) if (count > 0) output.write(buffer, 0, count);
            output.flush();
            fileOutput.getFD().sync();
        }
    }

    private static void cleanup(File directory, int keepNewest, File protectedFile) {
        File[] files = directory.listFiles();
        if (files == null || files.length <= keepNewest) return;
        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File left, File right) { return Long.compare(right.lastModified(), left.lastModified()); }
        });
        int kept = 0;
        for (File file : files) {
            if (!file.isFile() || file.getName().endsWith(".tmp")) {
                if (file.isFile()) {
                    long age = System.currentTimeMillis() - file.lastModified();
                    if (age > 60_000L && !file.delete()) AppLog.w("ImageStorage", "Falha ao limpar temporário antigo.");
                }
                continue;
            }
            if (file.equals(protectedFile)) { kept++; continue; }
            if (kept < keepNewest) { kept++; continue; }
            if (!file.delete()) AppLog.w("ImageStorage", "Falha ao remover imagem antiga: " + file.getName());
        }
    }
}
