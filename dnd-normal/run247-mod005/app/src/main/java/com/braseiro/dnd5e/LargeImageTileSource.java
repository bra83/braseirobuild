package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.Closeable;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Renderizador em mosaicos para mapas muito grandes.
 *
 * Mantém uma prévia leve para o mapa inteiro e, conforme o usuário amplia,
 * decodifica somente os trechos visíveis diretamente do PNG original. Assim
 * o mapa-fonte nunca é reduzido definitivamente a uma prévia fixa e os
 * nomes, estradas e símbolos voltam a ficar nítidos no zoom, sem exigir um
 * bitmap ARGB de centenas de megabytes na memória.
 */
final class LargeImageTileSource implements Closeable {
    private static final int DECODED_TILE_SIZE = 1024;
    private static final float MAX_DECODED_PIXEL_ON_SCREEN = 1.15f;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object decoderLock = new Object();
    private final Set<String> pending = new HashSet<>();
    private final LruCache<String, Bitmap> cache;

    private InputStream decoderStream;
    private BitmapRegionDecoder decoder;
    private int sourceWidth;
    private int sourceHeight;
    private int previewSample = 1;
    private int generation;
    private boolean permanentlyClosed;
    private volatile long lastDecodeErrorLogAt;

    LargeImageTileSource(Context context, int cacheMegabytes) {
        this.context = context.getApplicationContext();
        final int maxKb = Math.max(8, cacheMegabytes) * 1024;
        cache = new LruCache<String, Bitmap>(maxKb) {
            @Override protected int sizeOf(String key, Bitmap value) {
                return Math.max(1, value.getAllocationByteCount() / 1024);
            }

            @Override protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (oldValue != null && oldValue != newValue && !oldValue.isRecycled()) oldValue.recycle();
            }
        };
    }

    boolean open(Uri uri, int width, int height, int sample) {
        resetDecoder();
        if (uri == null || width <= 0 || height <= 0) return false;
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            if (stream == null) return false;
            BitmapRegionDecoder opened = BitmapRegionDecoder.newInstance(stream, false);
            if (opened == null) {
                stream.close();
                return false;
            }
            synchronized (decoderLock) {
                decoderStream = stream;
                decoder = opened;
                sourceWidth = width;
                sourceHeight = height;
                previewSample = Math.max(1, sample);
            }
            return true;
        } catch (Throwable error) {
            AppLog.w("LargeImageTileSource","Não foi possível abrir o mapa em mosaicos: "+uri,error);
            resetDecoder();
            return false;
        }
    }

    boolean isReady() {
        synchronized (decoderLock) {
            return decoder != null && !decoder.isRecycled() && sourceWidth > 0 && sourceHeight > 0;
        }
    }

    /**
     * Desenha mosaicos de maior resolução sobre a prévia já desenhada.
     * O canvas deve estar no mesmo sistema de coordenadas da prévia.
     */
    void draw(Canvas canvas, int previewWidth, int previewHeight, float viewScale, Paint paint, Runnable invalidator) {
        if (!isReady() || previewWidth <= 0 || previewHeight <= 0 || viewScale <= 0f) return;

        final float sourcePixelToScreen = viewScale * previewWidth / (float) sourceWidth;
        int desiredSample = chooseSample(sourcePixelToScreen);
        if (desiredSample >= previewSample) return; // a prévia já tem detalhe suficiente neste zoom

        Rect clip = canvas.getClipBounds();
        float previewLeft = clamp(clip.left, 0, previewWidth);
        float previewTop = clamp(clip.top, 0, previewHeight);
        float previewRight = clamp(clip.right, 0, previewWidth);
        float previewBottom = clamp(clip.bottom, 0, previewHeight);
        if (previewRight <= previewLeft || previewBottom <= previewTop) return;

        int sourceLeft = clampInt((int) Math.floor(previewLeft * sourceWidth / previewWidth), 0, sourceWidth);
        int sourceTop = clampInt((int) Math.floor(previewTop * sourceHeight / previewHeight), 0, sourceHeight);
        int sourceRight = clampInt((int) Math.ceil(previewRight * sourceWidth / previewWidth), 0, sourceWidth);
        int sourceBottom = clampInt((int) Math.ceil(previewBottom * sourceHeight / previewHeight), 0, sourceHeight);

        int tileSpan = DECODED_TILE_SIZE * desiredSample;
        int firstTileX = Math.max(0, sourceLeft / tileSpan);
        int firstTileY = Math.max(0, sourceTop / tileSpan);
        int lastTileX = Math.max(firstTileX, (Math.max(sourceLeft, sourceRight - 1)) / tileSpan);
        int lastTileY = Math.max(firstTileY, (Math.max(sourceTop, sourceBottom - 1)) / tileSpan);

        for (int tileY = firstTileY; tileY <= lastTileY; tileY++) {
            for (int tileX = firstTileX; tileX <= lastTileX; tileX++) {
                int left = tileX * tileSpan;
                int top = tileY * tileSpan;
                int right = Math.min(sourceWidth, left + tileSpan);
                int bottom = Math.min(sourceHeight, top + tileSpan);
                if (right <= left || bottom <= top) continue;

                String key = desiredSample + ":" + tileX + ":" + tileY;
                Bitmap tile = cache.get(key);
                Rect sourceRegion = new Rect(left, top, right, bottom);
                RectF destination = new RectF(
                        left * previewWidth / (float) sourceWidth,
                        top * previewHeight / (float) sourceHeight,
                        right * previewWidth / (float) sourceWidth,
                        bottom * previewHeight / (float) sourceHeight
                );
                if (tile != null && !tile.isRecycled()) {
                    canvas.drawBitmap(tile, null, destination, paint);
                } else {
                    requestTile(key, sourceRegion, desiredSample, invalidator);
                }
            }
        }
    }

    private int chooseSample(float sourcePixelToScreen) {
        int sample = 1;
        while (sample * 2 <= previewSample
                && (sample * 2) * sourcePixelToScreen <= MAX_DECODED_PIXEL_ON_SCREEN) {
            sample *= 2;
        }
        return Math.max(1, sample);
    }

    private void requestTile(final String key, final Rect region, final int sample, final Runnable invalidator) {/*BR_JSON_WRAP*/try{
        final int requestGeneration;
        synchronized (pending) {
            if (permanentlyClosed || pending.contains(key)) return;
            pending.add(key);
            requestGeneration = generation;
        }

        try {
            executor.execute(new Runnable() {
            @Override public void run() {/*BR_JSON_WRAP*/try{
                Bitmap decoded = null;
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = sample;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    options.inDither = true;
                    synchronized (decoderLock) {
                        if (decoder != null && !decoder.isRecycled() && requestGeneration == generation) {
                            decoded = decoder.decodeRegion(region, options);
                        }
                    }
                } catch (Throwable error) {
                    logDecodeFailure(error);
                    decoded = null;
                }

                final Bitmap result = decoded;
                mainHandler.post(new Runnable() {
                    @Override public void run() {/*BR_JSON_WRAP*/try{
                        synchronized (pending) { pending.remove(key); }
                        if (result == null) return;
                        if (permanentlyClosed || requestGeneration != generation) {
                            if (!result.isRecycled()) result.recycle();
                            return;
                        }
                        cache.put(key, result);
                        if (invalidator != null) try { invalidator.run(); } catch (Exception error) { AppLog.w("LargeImageTileSource","Falha ao redesenhar mosaico do mapa.",error); }
                    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
                });
            }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
            });
        } catch (RuntimeException rejected) {
            synchronized (pending) { pending.remove(key); }
            if (!permanentlyClosed) AppLog.w("LargeImageTileSource","Fila de mosaicos recusou uma decodificação.",rejected);
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void logDecodeFailure(Throwable error) {
        long now=System.currentTimeMillis();
        if(now-lastDecodeErrorLogAt<10000L)return;
        lastDecodeErrorLogAt=now;
        AppLog.w("LargeImageTileSource","Falha ao decodificar um mosaico; a prévia continuará visível.",error);
    }

    private void resetDecoder() {
        generation++;
        synchronized (pending) { pending.clear(); }
        cache.evictAll();
        synchronized (decoderLock) {
            if (decoder != null && !decoder.isRecycled()) decoder.recycle();
            decoder = null;
            if (decoderStream != null) {
                try { decoderStream.close(); } catch(Exception ignored){AppLog.ignored("LargeImageTileSource","Falha tolerada; a execução continuou.",ignored);}
            }
            decoderStream = null;
            sourceWidth = 0;
            sourceHeight = 0;
            previewSample = 1;
        }
    }

    /** Libera somente mosaicos decodificados; o decoder permanece pronto para recarregá-los. */
    void trimMemory() {
        generation++;
        synchronized (pending) { pending.clear(); }
        cache.evictAll();
    }

    @Override public void close() {
        permanentlyClosed = true;
        resetDecoder();
        executor.shutdownNow();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
