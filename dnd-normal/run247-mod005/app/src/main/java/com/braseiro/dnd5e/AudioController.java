package com.braseiro.dnd5e;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.json.JSONObject;

/**
 * Mesa de áudio em cinco canais independentes: voz, SFX, ambiência-base, clima e música.
 *
 * O controlador não decide quais arquivos representam a cena. Ele somente garante:
 * - música e ambiência em loop com crossfade;
 * - voz sequencial sem apagar arquivos persistentes de cache;
 * - SFX sobrepostos, limitados e sem interromper os demais canais;
 * - cancelamento por geração, para callbacks de uma reprodução antiga não vazarem;
 * - ducking suave enquanto Charon ou a voz nativa estiverem narrando.
 */
public final class AudioController {
    public interface Status { void onStatus(String text); }
    public interface TimedVoiceStart { void onStart(int durationMs); }
    public interface PlaybackFailure { void onFailure(String channel, String uri, String label); }

    private static final int MAX_SFX = 6;
    /** Um efeito pontual não pode ser reaberto em cascata por callbacks duplicados. */
    private static final long SFX_DUPLICATE_WINDOW_MS = 2400L;
    /** Proteção final contra arquivos longos ou corrompidos classificados como SFX. */
    private static final int MAX_ONE_SHOT_MS = 14000;
    private static final int SFX_HARD_LIMIT_MS = MAX_ONE_SHOT_MS;
    private static final int DEFAULT_CROSSFADE_MS = 3000;
    private static final int DUCK_FADE_MS = 420;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener focusListener;
    private AudioFocusRequest audioFocusRequest;
    private final BroadcastReceiver noisyReceiver;
    private boolean noisyReceiverRegistered=false;
    private final AudioDiagnosticsRepository diagnostics;
    private final SfxSoundPoolBackend soundPool;
    private String playbackSessionId = "";
    private long playbackGeneration = 0L;

    private MediaPlayer ambience;
    private MediaPlayer climate;
    private MediaPlayer music;
    private MediaPlayer voice;
    private String ambienceUri = "";
    private String climateUri = "";
    private String musicUri = "";
    private boolean ambienceReady = true;
    private boolean climateReady = true;
    private boolean musicReady = true;

    private final LinkedHashMap<MediaPlayer,String> sfxPlayers = new LinkedHashMap<MediaPlayer,String>();
    private final IdentityHashMap<MediaPlayer,Float> playerVolumes = new IdentityHashMap<MediaPlayer,Float>();
    private final IdentityHashMap<MediaPlayer,String> sfxKeyByPlayer = new IdentityHashMap<MediaPlayer,String>();
    private final HashSet<String> activeSfxKeys = new HashSet<String>();
    private final HashSet<String> pendingSfxKeys = new HashSet<String>();
    private final HashMap<String,Long> recentSfxAt = new HashMap<String,Long>();

    private float ambienceVol = .35f;
    private float climateVol = .28f;
    private float musicVol = .14f;
    private float sfxVol = .80f;
    private float voiceVol = 1f;

    private final Queue<VoiceClip> voiceQueue = new LinkedList<VoiceClip>();
    private VoiceClip currentVoiceClip;
    private boolean voiceInputOpen = false;

    private Status status;
    private PlaybackFailure playbackFailure;
    private long ambienceGeneration = 0L;
    private long climateGeneration = 0L;
    private long musicGeneration = 0L;
    private long sfxGeneration = 0L;
    private long voiceGeneration = 0L;
    private long duckGeneration = 0L;
    private boolean ducked = false;
    private boolean focusHeld = false;
    private boolean focusSuppressed = false;

    public AudioController(Context value) {
        context = value.getApplicationContext();
        diagnostics = new AudioDiagnosticsRepository(context);
        soundPool = new SfxSoundPoolBackend(context, diagnostics);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        focusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override public void onAudioFocusChange(int change) {
                synchronized (AudioController.this) {
                    if (change == AudioManager.AUDIOFOCUS_LOSS) {
                        focusSuppressed = true;
                        focusHeld = false;
                        pauseSafe(ambience);
                        pauseSafe(climate);
                        pauseSafe(music);
                        stopVoice();
                        stopSfx();
                        soundPool.stopAll();
                    } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        focusSuppressed = true;
                        pauseSafe(voice);
                        soundPool.setMuted(true);
                        setVolumeSafe(ambience, effectiveAmbienceVolume() * .25f);
                        setVolumeSafe(climate, effectiveClimateVolume() * .25f);
                        setVolumeSafe(music, effectiveMusicVolume() * .18f);
                    } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        soundPool.setMuted(false);soundPool.setOutputFactor(.20f);
                        setVolumeSafe(ambience, effectiveAmbienceVolume() * .20f);
                        setVolumeSafe(climate, effectiveClimateVolume() * .20f);
                        setVolumeSafe(music, effectiveMusicVolume() * .14f);
                    } else if (change == AudioManager.AUDIOFOCUS_GAIN) {
                        focusSuppressed = false;
                        resumeSafe(ambience);
                        resumeSafe(climate);
                        resumeSafe(music);
                        resumeSafe(voice);
                        soundPool.setOutputFactor(1f);soundPool.setMuted(false);
                        applyContinuousVolumesSmooth(300);
                    }
                }
            }
        };
        if(Build.VERSION.SDK_INT>=26){
            AudioAttributes focusAttributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            audioFocusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(focusAttributes).setAcceptsDelayedFocusGain(false).setWillPauseWhenDucked(false).setOnAudioFocusChangeListener(focusListener,handler).build();
        }
        noisyReceiver=new BroadcastReceiver(){@Override public void onReceive(Context ignored,Intent intent){
            if(intent==null||!AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))return;
            synchronized(AudioController.this){pauseSafe(ambience);pauseSafe(climate);pauseSafe(music);stopVoice();stopSfx();soundPool.stopAll();soundPool.setOutputFactor(1f);soundPool.setMuted(false);abandonFocus();diagnostics.event(playbackSessionId,"audio-becoming-noisy",new JSONObject());say("Fone ou Bluetooth removido; o áudio foi pausado com segurança.");}
        }};
        try{IntentFilter filter=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);if(android.os.Build.VERSION.SDK_INT>=33)context.registerReceiver(noisyReceiver,filter,Context.RECEIVER_NOT_EXPORTED);else context.registerReceiver(noisyReceiver,filter);noisyReceiverRegistered=true;}catch(Throwable error){AppLog.w("AudioController","Receiver de retirada de fone indisponível.",error);}
    }

    public synchronized void setStatus(Status value) { status = value; }
    public synchronized void setPlaybackFailure(PlaybackFailure value) { playbackFailure = value; }

    public synchronized void setVolumes(float ambienceValue, float climateValue, float musicValue, float sfxValue, float voiceValue) {
        ambienceVol = clamp(ambienceValue);
        climateVol = clamp(climateValue);
        musicVol = clamp(musicValue);
        sfxVol = clamp(sfxValue);
        voiceVol = clamp(voiceValue);
        applyContinuousVolumesSmooth(220);
        for (MediaPlayer player : new ArrayList<MediaPlayer>(sfxPlayers.keySet())) {
            setVolumeSafe(player, sfxVol);
        }
        setVolumeSafe(voice, voiceVol);
    }

    public synchronized void setVolumes(float ambienceValue, float musicValue, float sfxValue, float voiceValue) {
        setVolumes(ambienceValue, ambienceValue * .58f, musicValue, sfxValue, voiceValue);
    }

    public synchronized void playAmbience(Uri uri) { playAmbience(uri, DEFAULT_CROSSFADE_MS); }

    public synchronized void playAmbience(Uri uri, int transitionMs) {
        if (uri == null) return;
        String value = uri.toString();
        if (value.equals(ambienceUri) && ambience != null) {
            ambienceReady = true;
            return;
        }
        requestFocus();
        MediaPlayer old = ambience;
        String previousUri = ambienceUri;
        long generation = ++ambienceGeneration;
        ambienceUri = value;
        ambienceReady = false;
        MediaPlayer created = createLoop(uri, "Ambiência", generation, true, old, previousUri, clampTransition(transitionMs));
        if (created != null) {
            ambience = created;
        } else {
            ambience = old;
            ambienceUri = previousUri;
            ambienceReady = old != null || previousUri.length() == 0;
        }
    }

    /** Camada climática independente da ambiência-base. */
    public synchronized void playClimate(Uri uri) { playClimate(uri, DEFAULT_CROSSFADE_MS); }

    public synchronized void playClimate(Uri uri, int transitionMs) {
        if (uri == null) return;
        String value = uri.toString();
        if (value.equals(climateUri) && climate != null) {
            climateReady = true;
            return;
        }
        requestFocus();
        MediaPlayer old = climate;
        String previousUri = climateUri;
        long generation = ++climateGeneration;
        climateUri = value;
        climateReady = false;
        MediaPlayer created = createClimateLoop(uri, generation, old, previousUri, clampTransition(transitionMs));
        if (created != null) {
            climate = created;
        } else {
            climate = old;
            climateUri = previousUri;
            climateReady = old != null || previousUri.length() == 0;
        }
    }

    public synchronized void playMusic(Uri uri) { playMusic(uri, DEFAULT_CROSSFADE_MS); }

    public synchronized void playMusic(Uri uri, int transitionMs) {
        if (uri == null) return;
        String value = uri.toString();
        if (value.equals(musicUri) && music != null) {
            musicReady = true;
            return;
        }
        requestFocus();
        MediaPlayer old = music;
        String previousUri = musicUri;
        long generation = ++musicGeneration;
        musicUri = value;
        musicReady = false;
        MediaPlayer created = createLoop(uri, "Música", generation, false, old, previousUri, clampTransition(transitionMs));
        if (created != null) {
            music = created;
        } else {
            music = old;
            musicUri = previousUri;
            musicReady = old != null || previousUri.length() == 0;
        }
    }

    public synchronized boolean bedsReady() { return ambienceReady && climateReady && musicReady; }

    public void playSfx(Uri uri) { playSfx(uri, "", 1f, 0); }
    public void playSfx(Uri uri, String label) { playSfx(uri, label, 1f, 0); }
    public void playSfx(Uri uri, String label, float gain) { playSfx(uri, label, gain, 0); }

    /**
     * Reproduz um efeito pontual. maxDurationMs evita que um arquivo longo,
     * classificado como efeito por fallback, vire uma segunda ambiência.
     */
    public void playSfx(final Uri uri, final String requestedLabel, final float gain, final int maxDurationMs) {/*BR_JSON_WRAP*/try{
        if (uri == null) return;
        final String label = requestedLabel == null || requestedLabel.trim().length() == 0 ? "efeito" : requestedLabel.trim();
        final String key = sfxKey(uri, label);
        final long now = System.currentTimeMillis();
        synchronized (this) {
            Long recent = recentSfxAt.get(key);
            if (activeSfxKeys.contains(key) || pendingSfxKeys.contains(key) || (recent != null && now - recent < SFX_DUPLICATE_WINDOW_MS)) return;
            pendingSfxKeys.add(key);
        }
        requestFocus();
        final int limit = Math.max(350, Math.min(14000, maxDurationMs > 0 ? maxDurationMs : SFX_HARD_LIMIT_MS));
        final long expectedPlaybackGeneration;
        final String expectedSessionId;
        synchronized(this){expectedPlaybackGeneration=playbackGeneration;expectedSessionId=playbackSessionId;}
        diagnostics.event(expectedSessionId, "native-play-request", "uri", uri.toString());
        soundPool.play(uri, clamp(sfxVol * clamp(gain)), 1f, limit, new SfxSoundPoolBackend.Callback() {
            @Override public void onAccepted(int streamId) {/*BR_JSON_WRAP*/try{
                synchronized (AudioController.this) {
                    pendingSfxKeys.remove(key);
                    if(expectedPlaybackGeneration!=playbackGeneration||!expectedSessionId.equals(playbackSessionId))return;
                    activeSfxKeys.add(key);
                    recentSfxAt.put(key, System.currentTimeMillis());
                }
                say("SFX: " + label);
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        synchronized (AudioController.this) { activeSfxKeys.remove(key); abandonFocusIfIdle(); }
                    }
                }, limit + 120L);
            }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
            @Override public void onRejected(final String reason) {
                synchronized(AudioController.this){pendingSfxKeys.remove(key);if(expectedPlaybackGeneration!=playbackGeneration||!expectedSessionId.equals(playbackSessionId))return;}
                diagnostics.event(expectedSessionId, "native-play-rejected", "reason", reason);
                handler.post(new Runnable() {
                    @Override public void run() {
                        synchronized(AudioController.this){if(expectedPlaybackGeneration!=playbackGeneration||!expectedSessionId.equals(playbackSessionId))return;}
                        playSfxMediaPlayer(uri, label, gain, maxDurationMs);
                    }
                });
            }
        });
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public void preloadSfx(final Uri uri, String label) {
        if (uri == null) return;
        final long expectedGeneration;final String expectedSession;
        synchronized(this){expectedGeneration=playbackGeneration;expectedSession=playbackSessionId;}
        soundPool.preload(uri, new SfxSoundPoolBackend.Callback() {
            @Override public void onAccepted(int streamId) { synchronized(AudioController.this){if(expectedGeneration!=playbackGeneration||!expectedSession.equals(playbackSessionId))return;} diagnostics.event(expectedSession, "preload-complete", "uri", uri.toString()); }
            @Override public void onRejected(String reason) { synchronized(AudioController.this){if(expectedGeneration!=playbackGeneration||!expectedSession.equals(playbackSessionId))return;} diagnostics.event(expectedSession, "preload-failed", "reason", reason); }
        });
    }

    public synchronized void beginPlaybackSession(String sessionId) {
        playbackGeneration++;
        playbackSessionId = sessionId == null ? "" : sessionId;
        soundPool.beginSession(playbackSessionId);
    }

    public synchronized void endPlaybackSession() {
        playbackGeneration++;
        pendingSfxKeys.clear();
        soundPool.cancelSession();
        playbackSessionId = "";
    }

    private synchronized void playSfxMediaPlayer(final Uri uri, final String requestedLabel, final float gain, final int maxDurationMs) {/*BR_JSON_WRAP*/try{
        if (uri == null) return;
        final String label = cleanLabel(requestedLabel, uri);
        final String sfxKey = sfxKey(uri, label);
        final long now = System.currentTimeMillis();
        pruneRecentSfx(now);
        Long recent = recentSfxAt.get(sfxKey);
        if (activeSfxKeys.contains(sfxKey) || (recent != null && now - recent < SFX_DUPLICATE_WINDOW_MS)) {
            AppLog.i("AudioController", "SFX duplicado bloqueado: " + label);
            return;
        }
        activeSfxKeys.add(sfxKey);
        recentSfxAt.put(sfxKey, now);
        requestFocus();
        trimSfxPool();
        final long generation = sfxGeneration;
        MediaPlayer created = null;
        try {
            created = new MediaPlayer();
            final MediaPlayer player = created;
            sfxPlayers.put(player, label);
            sfxKeyByPlayer.put(player, sfxKey);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build());
            player.setDataSource(context, uri);
            player.setLooping(false);
            setVolumeSafe(player, clamp(sfxVol * clamp(gain)));
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(final MediaPlayer mp) {
                    synchronized (AudioController.this) {
                        if (generation != sfxGeneration || !sfxPlayers.containsKey(mp)) {
                            releaseSfx(mp);
                            return;
                        }
                        try {
                            if (!focusSuppressed) mp.start();
                            say("SFX: " + label);
                            // Mesmo quando o catálogo não informa duração, um SFX pontual
                            // recebe limite rígido. Isso impede loops acidentais e arquivos
                            // enormes classificados como efeito de travarem a mesa de áudio.
                            final int requestedLimit = maxDurationMs > 0 ? maxDurationMs : SFX_HARD_LIMIT_MS;
                            final int limit = Math.max(350, Math.min(SFX_HARD_LIMIT_MS, requestedLimit));
                            handler.postDelayed(new Runnable() {
                                @Override public void run() {
                                    synchronized (AudioController.this) {
                                        if (generation == sfxGeneration && sfxPlayers.containsKey(mp)) {
                                            AppLog.w("AudioController", "SFX encerrado pelo limite de segurança: " + label);
                                            releaseSfx(mp);
                                            abandonFocusIfIdle();
                                        }
                                    }
                                }
                            }, limit);
                        } catch (Exception e) {
                            AppLog.e("AudioController","Falha ao iniciar SFX "+label+".",e);
                            releaseSfx(mp);
                            say("Falha ao iniciar SFX: " + label); notifyFailure("sfx", uri, label);
                        }
                    }
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    synchronized (AudioController.this) {
                        releaseSfx(mp);
                        abandonFocusIfIdle();
                    }
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    synchronized (AudioController.this) {
                        releaseSfx(mp);
                        abandonFocusIfIdle();
                    }
                    AppLog.w("AudioController","MediaPlayer rejeitou SFX "+label+" (what="+what+", extra="+extra+").");
                    say("Falha ao tocar SFX: " + label); notifyFailure("sfx", uri, label);
                    return true;
                }
            });
            player.prepareAsync();
        } catch (Exception e) {
            AppLog.e("AudioController","Erro ao preparar SFX "+label+".",e);
            if (created != null) releaseSfx(created); else activeSfxKeys.remove(sfxKey);
            abandonFocusIfIdle();
            say("Erro de SFX: " + safe(e)); notifyFailure("sfx", uri, label);
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized void playSfxSolo(Uri uri, String requestedLabel) {
        stopSfx();
        playSfx(uri, requestedLabel, 1f, 0);
    }

    private MediaPlayer createLoop(final Uri uri, final String label, final long generation,
                                   final boolean ambienceChannel, final MediaPlayer old,
                                   final String previousUri, final int transitionMs) {
        MediaPlayer created = null;
        try {
            created = new MediaPlayer();
            final MediaPlayer player = created;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build());
            player.setDataSource(context, uri);
            player.setLooping(true);
            setVolumeSafe(player, 0f);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    synchronized (AudioController.this) {
                        boolean valid = ambienceChannel
                                ? generation == ambienceGeneration && mp == ambience
                                : generation == musicGeneration && mp == music;
                        if (!valid) {
                            release(mp);
                            return;
                        }
                        try {
                            if (!focusSuppressed) mp.start();
                            if (ambienceChannel) ambienceReady = true;
                            else musicReady = true;
                            crossFade(old, mp, ambienceChannel, generation, transitionMs);
                            say(label + " pronta.");
                        } catch (Exception e) {
                            AppLog.e("AudioController","Falha ao iniciar "+label.toLowerCase()+".",e);
                            restorePreviousLoop(ambienceChannel, mp, old, previousUri);
                            say("A nova " + label.toLowerCase() + " falhou; a faixa anterior foi preservada.");
                        }
                    }
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    synchronized (AudioController.this) {
                        restorePreviousLoop(ambienceChannel, mp, old, previousUri);
                        abandonFocusIfIdle();
                    }
                    AppLog.w("AudioController","MediaPlayer rejeitou "+label.toLowerCase()+" (what="+what+", extra="+extra+").");
                    say("Falha ao tocar " + label.toLowerCase() + "."); notifyFailure(ambienceChannel?"ambience":"music", uri, label);
                    return true;
                }
            });
            player.prepareAsync();
            return player;
        } catch (Exception e) {
            AppLog.e("AudioController","Erro ao preparar "+label.toLowerCase()+".",e);
            release(created);
            if (ambienceChannel) ambienceReady = old != null || previousUri == null || previousUri.length() == 0;
            else musicReady = old != null || previousUri == null || previousUri.length() == 0;
            say("Erro de " + label.toLowerCase() + ": " + safe(e)); notifyFailure(ambienceChannel?"ambience":"music", uri, label);
            return null;
        }
    }

    private MediaPlayer createClimateLoop(final Uri uri, final long generation,
                                          final MediaPlayer old, final String previousUri,
                                          final int transitionMs) {
        MediaPlayer created = null;
        try {
            created = new MediaPlayer();
            final MediaPlayer player = created;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build());
            player.setDataSource(context, uri);
            player.setLooping(true);
            setVolumeSafe(player, 0f);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    synchronized (AudioController.this) {
                        if (generation != climateGeneration || mp != climate) {
                            release(mp);
                            return;
                        }
                        try {
                            if (!focusSuppressed) mp.start();
                            climateReady = true;
                            crossFadeClimate(old, mp, generation, transitionMs);
                            say("Clima pronto.");
                        } catch (Exception e) {
                            AppLog.e("AudioController","Falha ao iniciar camada climática.",e);
                            restorePreviousClimate(mp, old, previousUri);
                            say("A nova camada climática falhou; a anterior foi preservada.");
                        }
                    }
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    synchronized (AudioController.this) {
                        restorePreviousClimate(mp, old, previousUri);
                        abandonFocusIfIdle();
                    }
                    AppLog.w("AudioController","MediaPlayer rejeitou clima (what="+what+", extra="+extra+").");
                    say("Falha ao tocar clima."); notifyFailure("climate", uri, "Clima");
                    return true;
                }
            });
            player.prepareAsync();
            return player;
        } catch (Exception e) {
            AppLog.e("AudioController","Erro ao preparar camada climática.",e);
            release(created);
            climateReady = old != null || previousUri == null || previousUri.length() == 0;
            say("Erro de clima: " + safe(e)); notifyFailure("climate", uri, "Clima");
            return null;
        }
    }

    private void restorePreviousClimate(MediaPlayer failed, MediaPlayer old, String previousUri) {
        release(failed);
        if (failed == climate) {
            climate = old;
            climateUri = previousUri == null ? "" : previousUri;
        }
        climateReady = true;
        if (old != null) {
            try { if (!focusSuppressed && !old.isPlaying()) old.start(); } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
            applyContinuousVolumesSmooth(250);
        }
    }

    private void crossFadeClimate(final MediaPlayer old, final MediaPlayer next,
                                  final long generation, final int durationMs) {
        final int steps = Math.max(12, Math.min(60, durationMs / 60));
        final long interval = Math.max(20L, durationMs / (long) steps);
        final float oldStart = readVolumeEstimate(old, effectiveClimateVolume());
        for (int i = 1; i <= steps; i++) {
            final int step = i;
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    synchronized (AudioController.this) {
                        if (generation != climateGeneration || next != climate) return;
                        float p = step / (float) steps;
                        setVolumeSafe(next, effectiveClimateVolume() * smoothStep(p));
                        if (old != null) setVolumeSafe(old, oldStart * (1f - smoothStep(p)));
                        if (step == steps && old != null) release(old);
                    }
                }
            }, interval * i);
        }
    }

    private void restorePreviousLoop(boolean ambienceChannel, MediaPlayer failed, MediaPlayer old, String previousUri) {
        release(failed);
        if (ambienceChannel) {
            if (failed == ambience) {
                ambience = old;
                ambienceUri = previousUri == null ? "" : previousUri;
            }
            ambienceReady = true;
        } else {
            if (failed == music) {
                music = old;
                musicUri = previousUri == null ? "" : previousUri;
            }
            musicReady = true;
        }
        if (old != null) {
            try {
                if (!focusSuppressed && !old.isPlaying()) old.start();
            } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
            applyContinuousVolumesSmooth(250);
        }
    }

    private void crossFade(final MediaPlayer old, final MediaPlayer next, final boolean ambienceChannel,
                           final long generation, final int durationMs) {
        final int steps = Math.max(12, Math.min(60, durationMs / 60));
        final long interval = Math.max(20L, durationMs / (long) steps);
        final float oldStart = readVolumeEstimate(old, currentChannelVolume(ambienceChannel));
        for (int i = 1; i <= steps; i++) {
            final int step = i;
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    synchronized (AudioController.this) {
                        boolean valid = ambienceChannel
                                ? generation == ambienceGeneration && next == ambience
                                : generation == musicGeneration && next == music;
                        if (!valid) return;
                        float p = step / (float) steps;
                        float target = ambienceChannel ? effectiveAmbienceVolume() : effectiveMusicVolume();
                        setVolumeSafe(next, target * smoothStep(p));
                        if (old != null) setVolumeSafe(old, oldStart * (1f - smoothStep(p)));
                        if (step == steps && old != null) release(old);
                    }
                }
            }, interval * i);
        }
    }

    /**
     * Abre uma sessão de voz produtora/consumidora. A fila pode ficar
     * temporariamente vazia enquanto o próximo bloco Charon é sintetizado sem
     * encerrar a narração nem restaurar os volumes prematuramente.
     */
    public synchronized void beginVoiceSession() {
        stopVoiceInternal(false);
        voiceGeneration++;
        voiceInputOpen = true;
        requestFocus();
    }

    public synchronized void endVoiceSessionInput() {
        voiceInputOpen = false;
        if (voice == null && voiceQueue.isEmpty()) finishVoiceSession();
    }

    public synchronized void enqueueVoice(File file) {
        enqueueTimedVoice(file, true, null, null);
    }

    public synchronized void enqueueVoice(File file, Runnable onComplete) {
        enqueueTimedVoice(file, true, null, onComplete);
    }

    public synchronized void enqueueVoice(File file, final Runnable onStart, Runnable onComplete) {
        enqueueTimedVoice(file, true, onStart == null ? null : new TimedVoiceStart() {
            @Override public void onStart(int durationMs) { onStart.run(); }
        }, onComplete);
    }

    public synchronized void enqueueTimedVoice(File file, TimedVoiceStart onStart, Runnable onComplete) {
        enqueueTimedVoice(file, true, onStart, onComplete);
    }

    public synchronized void enqueueTimedVoice(File file, boolean deleteAfterPlayback,
                                               TimedVoiceStart onStart, Runnable onComplete) {
        if (file == null || !file.isFile()) return;
        requestFocus();
        voiceQueue.offer(new VoiceClip(file, deleteAfterPlayback, onStart, onComplete, voiceGeneration));
        if (voice == null) {
            setDucked(true);
            playNextVoice();
        }
    }

    private synchronized void playNextVoice() {
        final VoiceClip clip = voiceQueue.poll();
        if (clip == null) {
            voice = null;
            currentVoiceClip = null;
            if (!voiceInputOpen) finishVoiceSession();
            return;
        }
        if (clip.generation != voiceGeneration) {
            deleteIfOwned(clip);
            playNextVoice();
            return;
        }
        currentVoiceClip = clip;
        MediaPlayer created = null;
        try {
            created = new MediaPlayer();
            final MediaPlayer player = created;
            voice = player;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build());
            player.setDataSource(clip.file.getAbsolutePath());
            setVolumeSafe(player, voiceVol);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    synchronized (AudioController.this) {
                        if (clip.generation != voiceGeneration || mp != voice) {
                            release(mp);
                            deleteIfOwned(clip);
                            return;
                        }
                        int duration = 1;
                        try { duration = Math.max(1, mp.getDuration()); } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
                        try {
                            if (!focusSuppressed) mp.start();
                            if (clip.onStart != null) clip.onStart.onStart(duration);
                            say("Narrando • música e ambiência ao fundo.");
                        } catch (Exception e) {
                            AppLog.e("AudioController","Falha ao iniciar bloco de voz.",e);
                            handleVoiceFinished(mp, clip, false);
                        }
                    }
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    synchronized (AudioController.this) { handleVoiceFinished(mp, clip, true); }
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    AppLog.w("AudioController","MediaPlayer rejeitou bloco de voz (what="+what+", extra="+extra+").");
                    synchronized (AudioController.this) { handleVoiceFinished(mp, clip, false); }
                    return true;
                }
            });
            player.prepareAsync();
        } catch (Exception e) {
            AppLog.e("AudioController","Erro ao preparar bloco de voz.",e);
            if (created != null) release(created);
            if (voice == created) voice = null;
            deleteIfOwned(clip);
            say("Erro na voz: " + safe(e));
            playNextVoice();
        }
    }

    private void handleVoiceFinished(MediaPlayer player, VoiceClip clip, boolean success) {
        release(player);
        if (voice == player) voice = null;
        currentVoiceClip = null;
        deleteIfOwned(clip);
        if (clip.onComplete != null) handler.post(clip.onComplete);
        if (!success) say("Um bloco de voz falhou; a reprodução continuará.");
        playNextVoice();
    }

    private void finishVoiceSession() {
        voiceInputOpen = false;
        setDucked(false);
        say("Narração concluída.");
        abandonFocusIfIdle();
    }

    private void deleteIfOwned(VoiceClip clip) {
        if (clip != null && clip.deleteAfterPlayback) {
            try { if (clip.file.exists() && !clip.file.delete()) AppLog.w("AudioController","Arquivo temporário de voz não pôde ser removido: "+clip.file.getName()); } catch (Exception e) { AppLog.w("AudioController","Falha ao remover arquivo temporário de voz.",e); }
        }
    }

    private void setDucked(boolean value) {
        if (ducked == value) return;
        ducked = value;
        applyContinuousVolumesSmooth(DUCK_FADE_MS);
    }

    public synchronized void setExternalVoiceActive(boolean active) {
        if (active) requestFocus();
        setDucked(active);
        if (!active) abandonFocusIfIdle();
    }

    private float effectiveAmbienceVolume() { return ambienceVol * (ducked ? .58f : 1f); }
    private float effectiveClimateVolume() { return climateVol; }
    private float effectiveMusicVolume() { return musicVol * (ducked ? .34f : 1f); }
    private float currentChannelVolume(boolean ambienceChannel) {
        return ambienceChannel ? effectiveAmbienceVolume() : effectiveMusicVolume();
    }

    private void applyContinuousVolumesSmooth(int durationMs) {
        final long generation = ++duckGeneration;
        final MediaPlayer a = ambience;
        final MediaPlayer c = climate;
        final MediaPlayer m = music;
        final float aStart = readVolumeEstimate(a, effectiveAmbienceVolume());
        final float cStart = readVolumeEstimate(c, effectiveClimateVolume());
        final float mStart = readVolumeEstimate(m, effectiveMusicVolume());
        final float aTarget = effectiveAmbienceVolume();
        final float cTarget = effectiveClimateVolume();
        final float mTarget = effectiveMusicVolume();
        final int steps = Math.max(4, Math.min(24, durationMs / 35));
        final long interval = Math.max(20L, durationMs / (long) steps);
        for (int i = 1; i <= steps; i++) {
            final int step = i;
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    synchronized (AudioController.this) {
                        if (generation != duckGeneration) return;
                        float p = smoothStep(step / (float) steps);
                        setVolumeSafe(a, lerp(aStart, aTarget, p));
                        setVolumeSafe(c, lerp(cStart, cTarget, p));
                        setVolumeSafe(m, lerp(mStart, mTarget, p));
                    }
                }
            }, interval * i);
        }
    }

    public synchronized void stopVoice() { stopVoiceInternal(true); }

    private void stopVoiceInternal(boolean incrementGeneration) {
        if (incrementGeneration) voiceGeneration++;
        voiceInputOpen = false;
        for (VoiceClip clip : new ArrayList<VoiceClip>(voiceQueue)) deleteIfOwned(clip);
        voiceQueue.clear();
        release(voice);
        voice = null;
        deleteIfOwned(currentVoiceClip);
        currentVoiceClip = null;
        setDucked(false);
        abandonFocusIfIdle();
    }

    public synchronized void stopSfx() {
        soundPool.stopAll();
        sfxGeneration++;
        for (MediaPlayer player : new ArrayList<MediaPlayer>(sfxPlayers.keySet())) releaseSfx(player);
        sfxPlayers.clear();
        sfxKeyByPlayer.clear();
        activeSfxKeys.clear();
        pendingSfxKeys.clear();
        recentSfxAt.clear();
        abandonFocusIfIdle();
    }

    public synchronized void stopAmbience() {
        final MediaPlayer old = ambience;
        ambienceGeneration++;
        ambience = null;
        ambienceUri = "";
        ambienceReady = true;
        fadeOutAndRelease(old, 750);
        abandonFocusIfIdleDelayed();
    }

    public synchronized void stopClimate() {
        final MediaPlayer old = climate;
        climateGeneration++;
        climate = null;
        climateUri = "";
        climateReady = true;
        fadeOutAndRelease(old, 750);
        abandonFocusIfIdleDelayed();
    }

    public synchronized void stopMusic() {
        final MediaPlayer old = music;
        musicGeneration++;
        music = null;
        musicUri = "";
        musicReady = true;
        fadeOutAndRelease(old, 750);
        abandonFocusIfIdleDelayed();
    }

    public synchronized void stopAll() {
        endPlaybackSession();
        ambienceGeneration++;
        climateGeneration++;
        musicGeneration++;
        sfxGeneration++;
        voiceGeneration++;
        duckGeneration++;
        release(ambience);
        release(climate);
        release(music);
        ambience = null;
        climate = null;
        music = null;
        ambienceUri = "";
        climateUri = "";
        musicUri = "";
        ambienceReady = true;
        climateReady = true;
        musicReady = true;
        for (MediaPlayer player : new ArrayList<MediaPlayer>(sfxPlayers.keySet())) releaseSfx(player);
        sfxPlayers.clear();
        sfxKeyByPlayer.clear();
        activeSfxKeys.clear();
        pendingSfxKeys.clear();
        recentSfxAt.clear();
        for (VoiceClip clip : new ArrayList<VoiceClip>(voiceQueue)) deleteIfOwned(clip);
        voiceQueue.clear();
        release(voice);
        voice = null;
        deleteIfOwned(currentVoiceClip);
        currentVoiceClip = null;
        voiceInputOpen = false;
        ducked = false;
        abandonFocus();
        say("Todos os sons foram interrompidos.");
    }

    public synchronized void shutdown(){
        stopAll();
        handler.removeCallbacksAndMessages(null);
        try{soundPool.release();}catch(Throwable ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
        if(noisyReceiverRegistered){try{context.unregisterReceiver(noisyReceiver);}catch(Throwable ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}noisyReceiverRegistered=false;}
    }

    public synchronized String currentAmbienceUri() { return ambienceUri == null ? "" : ambienceUri; }
    public synchronized String currentClimateUri() { return climateUri == null ? "" : climateUri; }
    public synchronized String currentMusicUri() { return musicUri == null ? "" : musicUri; }
    public synchronized boolean hasAmbience() { return ambience != null || ambienceUri.length() > 0; }
    public synchronized boolean hasClimate() { return climate != null || climateUri.length() > 0; }
    public synchronized boolean hasMusic() { return music != null || musicUri.length() > 0; }
    public synchronized boolean isVoiceActive() { return voice != null || !voiceQueue.isEmpty() || voiceInputOpen; }

    public synchronized String activeChannels() {
        StringBuilder b = new StringBuilder();
        if (ambience != null) b.append("Ambiência");
        if (climate != null) {
            if (b.length() > 0) b.append(" • ");
            b.append("Clima");
        }
        if (music != null) {
            if (b.length() > 0) b.append(" • ");
            b.append("Música");
        }
        int nativeSfx=soundPool.activeStreamCount();
        int totalSfx=sfxPlayers.size()+nativeSfx;
        if (totalSfx>0) {
            if (b.length() > 0) b.append(" • ");
            b.append("SFX ×").append(totalSfx);
        }
        if (isVoiceActive()) {
            if (b.length() > 0) b.append(" • ");
            b.append("Voz");
        }
        return b.length() == 0 ? "Nenhum canal tocando" : b.toString();
    }

    private void fadeOutAndRelease(final MediaPlayer player, int durationMs) {
        if (player == null) return;
        final float start = readVolumeEstimate(player, 1f);
        final int steps = 12;
        final long interval = Math.max(20, durationMs / steps);
        for (int i = 1; i <= steps; i++) {
            final int step = i;
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    float p = 1f - step / (float) steps;
                    setVolumeSafe(player, Math.max(0f, start * p));
                    if (step == steps) release(player);
                }
            }, interval * i);
        }
    }

    private void trimSfxPool() {
        while (sfxPlayers.size() >= MAX_SFX) {
            Map.Entry<MediaPlayer,String> first = sfxPlayers.entrySet().iterator().next();
            releaseSfx(first.getKey());
        }
    }

    private void releaseSfx(MediaPlayer player) {/*BR_JSON_WRAP*/try{
        sfxPlayers.remove(player);
        String key = sfxKeyByPlayer.remove(player);
        if (key != null) {
            activeSfxKeys.remove(key);
            recentSfxAt.put(key, System.currentTimeMillis());
        }
        release(player);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String sfxKey(Uri uri, String label) {
        String raw = (uri == null ? "" : uri.toString()) + "|" + (label == null ? "" : label);
        return raw.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private void pruneRecentSfx(long now) {
        for (String key : new ArrayList<String>(recentSfxAt.keySet())) {
            Long at = recentSfxAt.get(key);
            if (at == null || now - at > SFX_DUPLICATE_WINDOW_MS * 4L) recentSfxAt.remove(key);
        }
    }

    private void pauseSafe(MediaPlayer player) {
        try { if (player != null && player.isPlaying()) player.pause(); } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
    }

    private void resumeSafe(MediaPlayer player) {
        try { if (player != null && !player.isPlaying()) player.start(); } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
    }

    private void setVolumeSafe(MediaPlayer player, float value) {/*BR_JSON_WRAP*/try{
        if (player == null) return;
        try {
            float use = clamp(value);
            player.setVolume(use, use);
            playerVolumes.put(player, use);
        } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private float readVolumeEstimate(MediaPlayer player, float fallback) {
        if (player == null) return 0f;
        Float known = playerVolumes.get(player);
        return known == null ? clamp(fallback) : clamp(known);
    }

    private void release(MediaPlayer player) {
        if (player == null) return;
        playerVolumes.remove(player);
        try {
            player.setOnPreparedListener(null);
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            try { player.stop(); } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
            player.reset();
            player.release();
        } catch(Exception ignored){AppLog.ignored("AudioController","Falha tolerada; a execução continuou.",ignored);}
    }

    @SuppressWarnings("deprecation")
    private void requestFocus() {
        if (audioManager == null || focusHeld) return;
        try {
            int result;
            if(Build.VERSION.SDK_INT>=26&&audioFocusRequest!=null)result=audioManager.requestAudioFocus(audioFocusRequest);
            else result=audioManager.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if(focusHeld){focusSuppressed=false;soundPool.setOutputFactor(1f);soundPool.setMuted(false);resumeSafe(ambience);resumeSafe(climate);resumeSafe(music);applyContinuousVolumesSmooth(220);}
        } catch (Exception error) { AppLog.w("AudioFocus","Não foi possível solicitar foco de áudio.",error); }
    }

    @SuppressWarnings("deprecation")
    private void abandonFocus() {
        if (audioManager == null || !focusHeld) return;
        try {
            if(Build.VERSION.SDK_INT>=26&&audioFocusRequest!=null)audioManager.abandonAudioFocusRequest(audioFocusRequest);
            else audioManager.abandonAudioFocus(focusListener);
        } catch (Exception error) { AppLog.w("AudioFocus","Não foi possível abandonar o foco de áudio.",error); }
        focusHeld = false;
        focusSuppressed = false;
    }

    private void abandonFocusIfIdle() {
        if (ambience == null && climate == null && music == null && voice == null
                && voiceQueue.isEmpty() && !voiceInputOpen && sfxPlayers.isEmpty() && !soundPool.hasActiveStreams()) {
            abandonFocus();
        }
    }

    private void abandonFocusIfIdleDelayed() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                synchronized (AudioController.this) { abandonFocusIfIdle(); }
            }
        }, 820L);
    }

    private static int clampTransition(int value) { return Math.max(600, Math.min(8000, value)); }
    private static float smoothStep(float value) {
        float x = clamp(value);
        return x * x * (3f - 2f * x);
    }
    private static float lerp(float a, float b, float p) { return a + (b - a) * p; }
    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    private static String safe(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
    private static String cleanLabel(String label, Uri uri) {
        String value = label == null ? "" : label.trim();
        if (value.length() == 0 && uri != null) {
            value = uri.getLastPathSegment();
            if (value == null) value = "";
        }
        value = value.replace('\n', ' ').replace('\r', ' ');
        if (value.length() > 72) value = value.substring(0, 69) + "...";
        return value.length() == 0 ? "arquivo selecionado" : value;
    }
    private void notifyFailure(String channel, Uri uri, String label) {
        PlaybackFailure listener;
        synchronized (this) { listener = playbackFailure; }
        if (listener != null) {
            try { listener.onFailure(channel == null ? "" : channel, uri == null ? "" : uri.toString(), label == null ? "" : label); } catch (Exception e) { AppLog.w("AudioController","Callback de falha de reprodução lançou exceção.",e); }
        }
    }

    private final AudioStatusGate footerStatusGate = new AudioStatusGate();
    private void say(String text) {
        if (status == null || !footerStatusGate.allow(text, android.os.SystemClock.elapsedRealtime())) return;
        try { status.onStatus(text); }
        catch (Exception e) { AppLog.w("AudioController","Callback de status de áudio falhou.",e); }
    }

    private static final class VoiceClip {
        final File file;
        final boolean deleteAfterPlayback;
        final TimedVoiceStart onStart;
        final Runnable onComplete;
        final long generation;
        VoiceClip(File f, boolean delete, TimedVoiceStart start, Runnable complete, long gen) {
            file = f;
            deleteAfterPlayback = delete;
            onStart = start;
            onComplete = complete;
            generation = gen;
        }
    }
}
