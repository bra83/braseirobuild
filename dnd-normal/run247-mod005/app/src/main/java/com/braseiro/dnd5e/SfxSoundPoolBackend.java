package com.braseiro.dnd5e;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backend de baixa latência para efeitos pontuais.
 * Arquivos curtos são carregados antes do gatilho; MediaPlayer permanece como fallback.
 */
public final class SfxSoundPoolBackend {
    public interface Callback {
        void onAccepted(int streamId);
        void onRejected(String reason);
    }

    private static final int MAX_CACHED = 48;
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SoundPool pool;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<String, Entry>(64, .75f, true);
    private final HashMap<Integer, String> keyBySoundId = new HashMap<Integer, String>();
    private final HashSet<Integer> activeStreams = new HashSet<Integer>();
    private final HashMap<Integer,Float> activeStreamVolumes = new HashMap<Integer,Float>();
    private float outputFactor = 1f;
    private final AudioDiagnosticsRepository diagnostics;
    private volatile String sessionId = "";
    private volatile long generation = 0L;

    private static final class Entry {
        final String key;
        final int soundId;
        boolean loaded;
        boolean failed;
        final ArrayList<Callback> callbacks = new ArrayList<Callback>();
        Entry(String k, int id) { key = k; soundId = id; }
    }

    public SfxSoundPoolBackend(Context value, AudioDiagnosticsRepository repository) {
        context = value.getApplicationContext();
        diagnostics = repository;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attributes).build();
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                ArrayList<Callback> callbacks;
                Entry entry;
                synchronized (SfxSoundPoolBackend.this) {
                    String key = keyBySoundId.get(sampleId);
                    entry = key == null ? null : entries.get(key);
                    if (entry == null) return;
                    entry.loaded = status == 0;
                    entry.failed = status != 0;
                    callbacks = new ArrayList<Callback>(entry.callbacks);
                    entry.callbacks.clear();
                }
                for (Callback callback : callbacks) {
                    if (entry.loaded) callback.onAccepted(0);
                    else callback.onRejected("SoundPool rejeitou o arquivo (status=" + status + ").");
                }
            }
        });
    }

    public synchronized void beginSession(String id) {
        generation++;
        stopActiveStreamsLocked();
        sessionId = id == null ? "" : id;
        diagnostics.event(sessionId, "soundpool-session", "generation", generation);
    }

    public synchronized void cancelSession() {
        generation++;
        stopActiveStreamsLocked();
        sessionId = "";
    }

    public void preload(Uri uri, final Callback callback) {/*BR_JSON_WRAP*/try{
        if (uri == null) { if (callback != null) callback.onRejected("URI ausente."); return; }
        final String key = stableKey(uri);
        Entry existing;
        synchronized (this) {
            existing = entries.get(key);
            if (existing != null) {
                if (existing.loaded) { if (callback != null) callback.onAccepted(0); return; }
                if (existing.failed) { if (callback != null) callback.onRejected("Arquivo marcado como incompatível com SoundPool."); return; }
                if (callback != null) existing.callbacks.add(callback);
                return;
            }
        }
        int soundId;
        synchronized (this) {
            // O listener de carregamento também usa este monitor. Assim ele não
            // pode observar o soundId antes de a entrada correspondente existir.
            soundId = load(uri);
            if (soundId == 0) { if (callback != null) callback.onRejected("Não foi possível pré-carregar no SoundPool."); return; }
            Entry entry = new Entry(key, soundId);
            if (callback != null) entry.callbacks.add(callback);
            entries.put(key, entry);
            keyBySoundId.put(soundId, key);
            trimCacheLocked();
        }
        JSONObject details = new JSONObject();
        try { details.put("uri", uri.toString()).put("backend", "SoundPool").put("soundId", soundId); } catch(Exception ignored){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored);}
        diagnostics.event(sessionId, "preload-start", details);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public void play(final Uri uri, final float volume, final float rate, final int maxDurationMs, final Callback callback) {/*BR_JSON_WRAP*/try{
        if (uri == null) { if (callback != null) callback.onRejected("URI ausente."); return; }
        final long expectedGeneration = generation;
        preload(uri, new Callback() {
            @Override public void onAccepted(int ignored) {/*BR_JSON_WRAP*/try{
                final int streamId;final String playedSession;
                synchronized (SfxSoundPoolBackend.this) {
                    if (expectedGeneration != generation) { if (callback != null) callback.onRejected("Sessão substituída."); return; }
                    Entry entry = entries.get(stableKey(uri));
                    if (entry == null || !entry.loaded) { if (callback != null) callback.onRejected("Amostra ainda não está pronta."); return; }
                    try {
                        float v = clamp(volume);float output = clamp(v * outputFactor);
                        streamId = pool.play(entry.soundId, output, output, 1, 0, Math.max(.5f, Math.min(2f, rate)));
                    } catch (Throwable error) {
                        if (callback != null) callback.onRejected(error.getMessage());return;
                    }
                    if (streamId == 0) { if (callback != null) callback.onRejected("SoundPool devolveu streamId 0."); return; }
                    activeStreams.add(streamId);activeStreamVolumes.put(streamId,clamp(volume));playedSession=sessionId;
                }
                JSONObject details = new JSONObject();
                try { details.put("uri", uri.toString()).put("backend", "SoundPool").put("streamId", streamId); } catch(Exception ignored2){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored2);}
                diagnostics.event(playedSession, "native-sfx-started", details);
                final int stopAfter = Math.max(250, Math.min(14000, maxDurationMs > 0 ? maxDurationMs : 14000));
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        try { pool.stop(streamId); } catch(Throwable ignored3){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored3);}
                        synchronized (SfxSoundPoolBackend.this) { activeStreams.remove(streamId); activeStreamVolumes.remove(streamId); }
                    }
                }, stopAfter);
                if (callback != null) callback.onAccepted(streamId);
            }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
            @Override public void onRejected(String reason) { if (callback != null) callback.onRejected(reason); }
        });
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized boolean hasActiveStreams() { return !activeStreams.isEmpty(); }
    public synchronized int activeStreamCount() { return activeStreams.size(); }

    public synchronized void setMuted(boolean muted) {
        try { if (muted) pool.autoPause(); else { pool.autoResume(); applyOutputFactorLocked(); } } catch(Throwable ignored){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored);}
    }

    public synchronized void setOutputFactor(float factor) {
        outputFactor=clamp(factor);applyOutputFactorLocked();
    }

    private void applyOutputFactorLocked(){
        for(Integer streamId:new ArrayList<Integer>(activeStreams)){
            Float base=activeStreamVolumes.get(streamId);float value=clamp((base==null?1f:base)*outputFactor);
            try{pool.setVolume(streamId,value,value);}catch(Throwable ignored){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored);}
        }
    }

    public synchronized void stopAll() {
        generation++;
        stopActiveStreamsLocked();
    }

    public synchronized void release() {
        generation++;
        stopActiveStreamsLocked();
        entries.clear();
        keyBySoundId.clear();
        try { pool.release(); } catch(Throwable ignored){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored);}
    }

    private void stopActiveStreamsLocked() {
        for (Integer streamId : new ArrayList<Integer>(activeStreams)) {
            try { pool.stop(streamId); } catch(Throwable ignored){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored);}
        }
        activeStreams.clear();
        activeStreamVolumes.clear();
    }

    private int load(Uri uri) {
        String scheme = uri.getScheme();
        try {
            if (scheme == null || "file".equalsIgnoreCase(scheme)) {
                File file = scheme == null ? new File(uri.toString()) : new File(uri.getPath());
                if (!file.isFile()) return 0;
                return pool.load(file.getAbsolutePath(), 1);
            }
            ContentResolver resolver = context.getContentResolver();
            try (AssetFileDescriptor afd = resolver.openAssetFileDescriptor(uri, "r")) {
                if (afd == null) return 0;
                long length = afd.getLength();
                if (length > 0) return pool.load(afd.getFileDescriptor(), afd.getStartOffset(), length, 1);
                return pool.load(afd.getFileDescriptor(), afd.getStartOffset(), 64L * 1024L * 1024L, 1);
            }
        } catch (Throwable error) {
            AppLog.w("SoundPool", "Falha ao carregar " + uri, error);
            return 0;
        }
    }

    private synchronized void trimCacheLocked() {
        while (entries.size() > MAX_CACHED) {
            Map.Entry<String, Entry> first = entries.entrySet().iterator().next();
            entries.remove(first.getKey());
            keyBySoundId.remove(first.getValue().soundId);
            try { pool.unload(first.getValue().soundId); } catch(Throwable ignored){AppLog.ignored("SfxSoundPoolBackend","Falha tolerada; a execução continuou.",ignored);}
        }
    }

    private static String stableKey(Uri uri) { return uri == null ? "" : uri.normalizeScheme().toString(); }
    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
}
