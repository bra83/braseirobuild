package com.braseiro.dnd5e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;
import android.util.LruCache;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Decodes Strixhaven campus maps OFF the Android UI thread.
 * Multiple view rebuilds share a bounded cache and a single in-flight decode.
 * Never recycle on eviction: views may still be drawing an evicted bitmap.
 */
final class StrixhavenMapBitmapCache {
    private static final int MAX_CACHE_BYTES = 32 * 1024 * 1024;
    private static final int MAX_DECODE_DIMENSION = 3072;
    private static final Object LOCK = new Object();
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        @Override protected int sizeOf(String path, Bitmap value) {
            return value == null ? 0 : value.getByteCount();
        }
    };
    private static final Map<String, ArrayList<WeakReference<StrixhavenCampusView>>> WAITING =
            new HashMap<String, ArrayList<WeakReference<StrixhavenCampusView>>>();
    private static final ExecutorService DECODE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "braseiro-strixhaven-map-decode");
        t.setDaemon(true);
        return t;
    });

    private StrixhavenMapBitmapCache() {}

    static void request(Context context, String asset, StrixhavenCampusView view) {
        if (context == null || asset == null || asset.isEmpty() || view == null) return;
        final Bitmap cached;
        synchronized (LOCK) {
            cached = CACHE.get(asset);
            if (cached == null) {
                ArrayList<WeakReference<StrixhavenCampusView>> clients = WAITING.get(asset);
                if (clients != null) {
                    clients.add(new WeakReference<StrixhavenCampusView>(view));
                    return;
                }
                clients = new ArrayList<WeakReference<StrixhavenCampusView>>();
                clients.add(new WeakReference<StrixhavenCampusView>(view));
                WAITING.put(asset, clients);
            }
        }
        if (cached != null) {
            view.acceptDecodedMap(asset, cached);
            return;
        }
        final Context app = context.getApplicationContext();
        DECODE.execute(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Bitmap loaded = null;
            try {
                loaded = decode(app, asset);
            } catch (Throwable error) {
                AppLog.w("StrixhavenMap", "Falha ao carregar mapa fora da thread principal: " + asset, error);
            }
            final ArrayList<WeakReference<StrixhavenCampusView>> clients;
            synchronized (LOCK) {
                if (loaded != null) CACHE.put(asset, loaded);
                clients = WAITING.remove(asset);
            }
            if (clients == null) return;
            final Bitmap ready = loaded;
            for (WeakReference<StrixhavenCampusView> ref : clients) {
                StrixhavenCampusView target = ref.get();
                if (target != null) target.post(() -> target.acceptDecodedMap(asset, ready));
            }
        });
    }

    private static Bitmap decode(Context app, String path) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = app.getAssets().open(path)) {
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_DECODE_DIMENSION) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        options.inSampleSize = sample;
        try (InputStream stream = app.getAssets().open(path)) {
            return BitmapFactory.decodeStream(stream, null, options);
        } catch (OutOfMemoryError oom) {
            options.inSampleSize = Math.max(2, sample * 2);
            try (InputStream stream = app.getAssets().open(path)) {
                return BitmapFactory.decodeStream(stream, null, options);
            }
        }
    }
}
