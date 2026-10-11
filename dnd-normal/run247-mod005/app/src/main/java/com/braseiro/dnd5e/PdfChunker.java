package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Prepara cópias temporárias de PDFs grandes para ingestão no Gemini File Search.
 *
 * O arquivo original nunca é alterado. Para PDFs acima do limite por documento, as
 * páginas são copiadas para PDFs derivados, com alta resolução e sem mudar a ordem.
 * Em Android 15/API 35+, o texto extraído pelo renderer também é incluído de forma
 * praticamente invisível em cada página, melhorando a busca e preservando a página.
 */
public final class PdfChunker {
    public static final long GEMINI_LIMIT_BYTES = 100L * 1024L * 1024L;
    public static final long SAFE_UPLOAD_BYTES = 94L * 1024L * 1024L;
    private static final long TARGET_PART_BYTES = 78L * 1024L * 1024L;
    private static final int DEFAULT_LONG_SIDE = 2400;
    private static final int[] SINGLE_PAGE_LONG_SIDES = new int[]{2400, 2100, 1800, 1500, 1200};
    private static final long MAX_BITMAP_PIXELS = 6_200_000L;

    public interface Progress {
        void onProgress(String message);
    }

    public static final class Part {
        public int number;
        public int total;
        public int startPage;
        public int endPage;
        public int pageCount;
        public long sizeBytes;
        public File file;
        public boolean temporary;
        public String mode;
        public String sourceHash;

        public String rangeLabel() {
            return startPage == endPage ? "página " + startPage : "páginas " + startPage + "–" + endPage;
        }
    }

    private PdfChunker() {}

    public static List<Part> prepare(Context context, File source, long manualId, Progress progress) throws Exception {
        if (source == null || !source.isFile() || source.length() <= 0) {
            throw new Exception("O PDF não foi encontrado ou está vazio.");
        }
        String hash = sha256(source);
        int pageCount = readPageCount(source);
        if (pageCount <= 0) throw new Exception("O PDF não contém páginas legíveis.");

        if (source.length() <= SAFE_UPLOAD_BYTES) {
            Part part = new Part();
            part.number = 1;
            part.total = 1;
            part.startPage = 1;
            part.endPage = pageCount;
            part.pageCount = pageCount;
            part.sizeBytes = source.length();
            part.file = source;
            part.temporary = false;
            part.mode = "original";
            part.sourceHash = hash;
            return Collections.singletonList(part);
        }

        File root = new File(context.getCacheDir(), "rag_pdf_parts/manual_" + manualId + "_" + hash.substring(0, 12));
        if (!root.mkdirs() && !root.isDirectory()) throw new Exception("Não foi possível criar a área temporária para dividir o PDF.");

        if (progress != null) progress.onProgress("PDF grande detectado: preparando divisão automática sem alterar o original...");
        ArrayList<Part> parts = loadManifest(root, hash, pageCount);
        int estimatedPages = estimatePagesPerPart(pageCount, source.length());
        int startIndex = parts.size() == 0 ? 0 : parts.get(parts.size() - 1).endPage;
        if (startIndex > 0 && progress != null) progress.onProgress("Retomando a divisão a partir da página " + (startIndex + 1) + "...");
        while (startIndex < pageCount) {
            int endIndex = Math.min(pageCount - 1, startIndex + estimatedPages - 1);
            Part part = buildRangeWithLimit(source, root, parts.size() + 1, startIndex, endIndex, progress);
            parts.add(part);
            writeManifest(root, hash, pageCount, parts, false);
            int actualPages = part.endPage - part.startPage + 1;
            if (part.sizeBytes > 0) {
                double ratio = (double) TARGET_PART_BYTES / (double) part.sizeBytes;
                int adapted = (int) Math.floor(actualPages * Math.max(0.55d, Math.min(1.75d, ratio)));
                estimatedPages = Math.max(1, Math.min(220, adapted));
            }
            startIndex = part.endPage; // original page is 1-based; next zero-based index equals endPage
        }

        for (int i = 0; i < parts.size(); i++) {
            Part p = parts.get(i);
            p.number = i + 1;
            p.total = parts.size();
            p.sourceHash = hash;
        }
        writeManifest(root, hash, pageCount, parts, true);
        if (progress != null) progress.onProgress("Divisão concluída: " + parts.size() + " partes, " + pageCount + " páginas preservadas.");
        return parts;
    }

    /** Recria apenas uma faixa já conhecida, útil ao retomar uma parte interrompida. */
    public static Part recreateRange(Context context, File source, long manualId, String sourceHash, int number, int total, int startPage, int endPage, Progress progress) throws Exception {
        String hash = sourceHash == null || sourceHash.length() < 8 ? sha256(source) : sourceHash;
        File root = new File(context.getCacheDir(), "rag_pdf_parts/manual_" + manualId + "_" + hash.substring(0, Math.min(12, hash.length())));
        if (!root.exists() && !root.mkdirs()) throw new Exception("Não foi possível recriar a parte temporária do PDF.");
        Part p = buildRangeWithLimit(source, root, number, Math.max(0, startPage - 1), Math.max(0, endPage - 1), progress);
        p.number = number;
        p.total = total;
        p.sourceHash = hash;
        return p;
    }

    public static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] buffer = new byte[128 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
        } finally {
            in.close();
        }
        byte[] bytes = digest.digest();
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b & 0xff));
        return out.toString();
    }

    private static int readPageCount(File source) throws Exception {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = null;
        try {
            renderer = new PdfRenderer(pfd);
            return renderer.getPageCount();
        } catch (SecurityException e) {
            throw new Exception("O PDF é protegido por senha e não pode ser dividido automaticamente.");
        } catch (Exception e) {
            throw new Exception("O PDF está corrompido ou não pôde ser aberto para divisão: " + safeMessage(e));
        } finally {
            if (renderer != null) try { renderer.close(); } catch(Exception ignored){AppLog.ignored("PdfChunker","Falha tolerada; a execução continuou.",ignored);}
            else try { pfd.close(); } catch(Exception ignored){AppLog.ignored("PdfChunker","Falha tolerada; a execução continuou.",ignored);}
        }
    }

    private static int estimatePagesPerPart(int pageCount, long sourceBytes) {
        if (sourceBytes <= 0) return Math.min(80, pageCount);
        int pages = (int) Math.floor((double) pageCount * (double) TARGET_PART_BYTES / (double) sourceBytes);
        return Math.max(1, Math.min(180, pages));
    }

    private static Part buildRangeWithLimit(File source, File root, int partNumber, int startIndex, int requestedEndIndex, Progress progress) throws Exception {
        int lowEnd = startIndex;
        int highEnd = requestedEndIndex;
        Part best = null;
        boolean firstAttempt = true;
        while (lowEnd <= highEnd) {
            int candidateEnd = firstAttempt ? highEnd : (lowEnd + highEnd) / 2;
            firstAttempt = false;
            File candidate = new File(root, String.format(Locale.US, "parte_%03d_p%05d-%05d.pdf", partNumber, startIndex + 1, candidateEnd + 1));
            if (progress != null) progress.onProgress("Dividindo PDF: páginas " + (startIndex + 1) + "–" + (candidateEnd + 1) + "...");
            renderRange(source, candidate, startIndex, candidateEnd, DEFAULT_LONG_SIDE, progress);
            long bytes = candidate.length();
            if (bytes <= SAFE_UPLOAD_BYTES) {
                if (best != null && best.file != null && best.file.exists()) best.file.delete();
                best = toPart(candidate, startIndex, candidateEnd, bytes, "cópia visual de alta resolução");
                lowEnd = candidateEnd + 1;
                if (candidateEnd == requestedEndIndex) break;
            } else {
                candidate.delete();
                highEnd = candidateEnd - 1;
            }
        }

        if (best == null) {
            // Uma única página muito pesada: reduz apenas a cópia de indexação, nunca o PDF original.
            for (int longSide : SINGLE_PAGE_LONG_SIDES) {
                File candidate = new File(root, String.format(Locale.US, "parte_%03d_p%05d.pdf", partNumber, startIndex + 1));
                if (progress != null) progress.onProgress("Preparando a página " + (startIndex + 1) + " para a biblioteca...");
                renderRange(source, candidate, startIndex, startIndex, longSide, progress);
                if (candidate.length() <= SAFE_UPLOAD_BYTES) {
                    best = toPart(candidate, startIndex, startIndex, candidate.length(), "cópia visual adaptativa de alta resolução");
                    break;
                }
                candidate.delete();
            }
        }
        if (best == null) throw new Exception("Uma única página continua acima do limite de 100 MB. O PDF original foi preservado, mas essa página precisa ser salva novamente pelo editor do PDF.");
        return best;
    }

    private static Part toPart(File file, int startIndex, int endIndex, long bytes, String mode) {
        Part p = new Part();
        p.startPage = startIndex + 1;
        p.endPage = endIndex + 1;
        p.pageCount = endIndex - startIndex + 1;
        p.sizeBytes = bytes;
        p.file = file;
        p.temporary = true;
        p.mode = mode;
        return p;
    }

    private static void renderRange(File source, File destination, int startIndex, int endIndex, int maxLongSide, Progress progress) throws Exception {
        if (destination.exists()) destination.delete();
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = null;
        PdfDocument out = new PdfDocument();
        try {
            renderer = new PdfRenderer(pfd);
            if (startIndex < 0 || endIndex >= renderer.getPageCount() || endIndex < startIndex) throw new Exception("Faixa de páginas inválida.");
            for (int index = startIndex; index <= endIndex; index++) {
                PdfRenderer.Page page = renderer.openPage(index);
                Bitmap bitmap = null;
                try {
                    int srcW = Math.max(1, page.getWidth());
                    int srcH = Math.max(1, page.getHeight());
                    double scaleBySide = (double) maxLongSide / (double) Math.max(srcW, srcH);
                    double scaleByPixels = Math.sqrt((double) MAX_BITMAP_PIXELS / (double) ((long) srcW * (long) srcH));
                    double scale = Math.max(0.20d, Math.min(scaleBySide, scaleByPixels));
                    int bitmapW = Math.max(320, (int) Math.round(srcW * scale));
                    int bitmapH = Math.max(320, (int) Math.round(srcH * scale));
                    bitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888);
                    Canvas bitmapCanvas = new Canvas(bitmap);
                    bitmapCanvas.drawColor(Color.WHITE);
                    Matrix matrix = new Matrix();
                    matrix.setScale((float) bitmapW / srcW, (float) bitmapH / srcH);
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);

                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(srcW, srcH, index - startIndex + 1).create();
                    PdfDocument.Page target = out.startPage(info);
                    Canvas canvas = target.getCanvas();
                    Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
                    canvas.drawBitmap(bitmap, null, new Rect(0, 0, srcW, srcH), imagePaint);
                    addHiddenSearchText(page, canvas, srcW, srcH);
                    out.finishPage(target);
                } finally {
                    if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
                    page.close();
                }
                if (progress != null && ((index - startIndex) % 6 == 0 || index == endIndex)) {
                    progress.onProgress("Copiando páginas " + (startIndex + 1) + "–" + (endIndex + 1) + ": " + (index - startIndex + 1) + " de " + (endIndex - startIndex + 1));
                }
            }
            FileOutputStream fileOut = new FileOutputStream(destination);
            try { out.writeTo(fileOut); } finally { fileOut.close(); }
        } catch (OutOfMemoryError oom) {
            destination.delete();
            throw new Exception("O aparelho ficou sem memória ao preparar uma página. A tarefa foi interrompida com segurança e pode ser retomada.");
        } finally {
            try { out.close(); } catch(Exception ignored){AppLog.ignored("PdfChunker","Falha tolerada; a execução continuou.",ignored);}
            if (renderer != null) try { renderer.close(); } catch(Exception ignored){AppLog.ignored("PdfChunker","Falha tolerada; a execução continuou.",ignored);}
            else try { pfd.close(); } catch(Exception ignored){AppLog.ignored("PdfChunker","Falha tolerada; a execução continuou.",ignored);}
        }
        if (!destination.isFile() || destination.length() <= 0) throw new Exception("A parte temporária do PDF não pôde ser criada.");
    }

    /** API 35+: adiciona o texto da página com opacidade mínima para facilitar a recuperação sem alterar a aparência. */
    private static void addHiddenSearchText(PdfRenderer.Page page, Canvas canvas, int pageWidth, int pageHeight) {
        if (Build.VERSION.SDK_INT < 35) return;
        try {
            Method getTextContents = page.getClass().getMethod("getTextContents");
            Object value = getTextContents.invoke(page);
            if (!(value instanceof List)) return;
            StringBuilder text = new StringBuilder();
            for (Object item : (List<?>) value) {
                if (item == null) continue;
                Method getText = item.getClass().getMethod("getText");
                Object raw = getText.invoke(item);
                if (raw != null) {
                    String clean = String.valueOf(raw).replaceAll("\\s+", " ").trim();
                    if (clean.length() > 0) text.append(clean).append(' ');
                }
            }
            String all = text.toString().trim();
            if (all.length() == 0) return;
            Paint hidden = new Paint(Paint.ANTI_ALIAS_FLAG);
            hidden.setColor(Color.WHITE);
            hidden.setAlpha(1);
            hidden.setTextSize(1.15f);
            int maxChars = Math.max(80, Math.min(700, pageWidth));
            float y = Math.max(1f, pageHeight - 1.5f);
            for (int start = 0; start < all.length() && start < 30000; start += maxChars) {
                int end = Math.min(all.length(), start + maxChars);
                canvas.drawText(all.substring(start, end), 0.5f, y, hidden);
                y -= 1.2f;
                if (y < 0.5f) break;
            }
        } catch (Throwable ignored) {
            // A cópia visual continua válida e o Gemini pode aplicar OCR.
        }
    }

    private static ArrayList<Part> loadManifest(File root, String sourceHash, int pageCount) {
        ArrayList<Part> out = new ArrayList<Part>();
        File manifest = new File(root, "partes.json");
        if (!manifest.isFile()) return out;
        try {
            FileInputStream in = new FileInputStream(manifest);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream((int)Math.min(manifest.length(),64L*1024L));
            byte[] data=new byte[16384];int read,total=0;
            try{while((read=in.read(data))>0){total+=read;if(total>2*1024*1024)throw new Exception("Manifesto de partes excedeu 2 MB.");bytes.write(data,0,read);}}finally{in.close();}
            if (total <= 0) return out;
            JSONObject rootJson = new JSONObject(new String(bytes.toByteArray(), "UTF-8"));
            if (!sourceHash.equals(rootJson.optString("sourceHash")) || pageCount != rootJson.optInt("pageCount")) return out;
            JSONArray rows = rootJson.optJSONArray("parts");if(rows == null) return out;
            int expectedStart = 1;
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);if(row == null) break;
                File file = new File(root, row.optString("file"));
                int startPage = row.optInt("startPage"), endPage = row.optInt("endPage");
                if (!file.isFile() || file.length() <= 0 || file.length() > SAFE_UPLOAD_BYTES || startPage != expectedStart || endPage < startPage) break;
                Part part = new Part();part.number=i+1;part.total=rows.length();part.startPage=startPage;part.endPage=endPage;part.pageCount=endPage-startPage+1;part.sizeBytes=file.length();part.file=file;part.temporary=true;part.mode=row.optString("mode","cópia visual de alta resolução");part.sourceHash=sourceHash;out.add(part);expectedStart=endPage+1;
            }
        } catch (Exception ignored) { out.clear(); }
        return out;
    }

    private static void writeManifest(File root,String sourceHash,int pageCount,List<Part> parts,boolean complete) {/*BR_JSON_WRAP*/try{
        File temp = new File(root,"partes.json.tmp"), target = new File(root,"partes.json");
        try {
            JSONArray rows=new JSONArray();for(Part p:parts){if(p==null||p.file==null)continue;rows.put(new JSONObject().put("number",p.number).put("startPage",p.startPage).put("endPage",p.endPage).put("sizeBytes",p.sizeBytes).put("file",p.file.getName()).put("mode",p.mode==null?"":p.mode));}
            JSONObject json=new JSONObject().put("sourceHash",sourceHash).put("pageCount",pageCount).put("complete",complete).put("updatedAt",System.currentTimeMillis()).put("parts",rows);
            FileOutputStream out=new FileOutputStream(temp);out.write(json.toString().getBytes("UTF-8"));out.flush();out.close();if(target.exists())target.delete();if(!temp.renameTo(target)){FileInputStream in=new FileInputStream(temp);FileOutputStream copy=new FileOutputStream(target);byte[] b=new byte[8192];int n;while((n=in.read(b))>0)copy.write(b,0,n);in.close();copy.close();temp.delete();}
        }catch(Exception ignored){temp.delete();}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static void cleanupStale(Context context,long maxAgeMillis) {
        if(context==null)return;File base=new File(context.getCacheDir(),"rag_pdf_parts");File[] dirs=base.listFiles();if(dirs==null)return;long cutoff=System.currentTimeMillis()-Math.max(24L*60L*60L*1000L,maxAgeMillis);
        for(File dir:dirs){if(dir==null||!dir.isDirectory())continue;File manifest=new File(dir,"partes.json");long last=manifest.isFile()?manifest.lastModified():dir.lastModified();if(last>0&&last<cutoff)deleteRecursively(dir);}
    }

    public static void cleanupParts(List<Part> parts) {
        if (parts == null) return;
        Set<File> parents = new HashSet<File>();
        for (Part p : parts) if (p != null && p.temporary && p.file != null) {
            File parent = p.file.getParentFile();if(parent!=null)parents.add(parent);p.file.delete();
        }
        for(File parent:parents)deleteRecursively(parent);
    }

    public static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        file.delete();
    }

    private static String safeMessage(Throwable t) {
        String m = t == null ? "" : t.getMessage();
        return m == null || m.trim().length() == 0 ? "erro desconhecido" : m.trim();
    }
}
