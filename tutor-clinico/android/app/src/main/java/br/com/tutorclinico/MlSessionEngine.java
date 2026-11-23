package br.com.tutorclinico;

import android.app.Activity;
import android.net.Uri;
import android.view.WindowManager;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import org.vosk.android.RecognitionListener;

/**
 * On-device speech recognition with Vosk PT-BR. Live microphone or imported audio,
 * never recording or copying raw audio into an app file.
 */
final class MlSessionEngine implements RecognitionListener {
    interface Events { void state(String s); void segment(String s); void partial(String s); }
    private final Activity activity;
    private final Events events;
    private Model model;
    private SpeechService speech;
    private Recognizer rec;
    private Uri pendingFile;
    private Thread fileWorker;
    private boolean wanted, preparing, importing, destroyed;
    private int generation;
    private long lastPartialAt;

    MlSessionEngine(Activity activity, Events events) {
        this.activity=activity;this.events=events;
    }

    synchronized void start() {
        if (busy() || destroyed) { events.state("OPERAÇÃO EM ANDAMENTO"); return; }
        wanted=true;
        if (model != null) { beginMicrophone(); return; }
        prepareModel();
    }

    synchronized void importFile(Uri uri) {
        if (busy() || destroyed) { events.state("OPERAÇÃO EM ANDAMENTO"); return; }
        if (uri == null) { events.state("ERRO ARQUIVO: seleção inválida"); return; }
        wanted=true;pendingFile=uri;
        beginFile();
    }

    private boolean busy() { return wanted || preparing || importing || speech != null; }

    private synchronized void prepareModel() {
        preparing=true;
        events.state("PREPARANDO MODELO OFFLINE");
        StorageService.unpack(activity, "model-pt", "modelo-pt", loaded -> {
            synchronized (MlSessionEngine.this) {
                preparing=false;
                if (!wanted || destroyed) { loaded.close(); return; }
                model=loaded;
                if (pendingFile != null) beginFile(); else beginMicrophone();
            }
        }, err -> {
            synchronized (MlSessionEngine.this) {
                preparing=false; wanted=false; pendingFile=null;
            }
            events.state("ERRO MODELO: " + err.getMessage());
        });
    }

    private synchronized void beginMicrophone() {
        if (!wanted || model == null || destroyed) return;
        try {
            rec=new Recognizer(model,16000.0f);
            speech=new SpeechService(rec,16000.0f);
            if (!speech.startListening(this))
                throw new IllegalStateException("Falha ao iniciar captura");
            activity.runOnUiThread(() ->
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            events.state("CAPTURANDO · VOSK PT-BR");
        } catch (Exception e) {
            stop();
            events.state("ERRO MICROFONE: " + e.getMessage());
        }
    }

    private synchronized void beginFile() {
        if (!wanted || pendingFile == null || destroyed) return;
        final Uri selected=pendingFile;
        pendingFile=null;
        final int mine=++generation;
        importing=true;
        events.state("IMPORTANDO ÁUDIO · SEM CÓPIA LOCAL");
        activity.runOnUiThread(() ->
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        fileWorker=new Thread(() -> {
            try {
                String mime=activity.getContentResolver().getType(selected);
                if (mime!=null && (mime.equals("audio/mp4")||mime.contains("m4a")||mime.contains("aac"))) {
                    // The free Whisper decoder cannot open M4A/AAC. State this, do not pretend it used Whisper.
                    events.state("ERRO ARQUIVO: para Whisper escolha MP3, WAV ou FLAC. M4A/AAC ainda não suportado.");
                } else {
                    WhisperFileEngine.transcribe(activity.getApplicationContext(), selected,
                            new WhisperFileEngine.Listener() {
                                @Override public boolean isActive() {
                                    synchronized (MlSessionEngine.this) {
                                        return wanted && !destroyed && mine == generation;
                                    }
                                }
                                @Override public void state(String text) {
                                    if (isActive()) events.state(text);
                                }
                                @Override public void segment(String text) {
                                    if (isActive()) events.segment(text);
                                }
                            });
                }
            } catch (Exception e) {
                synchronized (MlSessionEngine.this) {
                    if (mine == generation && wanted && !destroyed)
                        events.state("ERRO ARQUIVO: " + (e.getMessage() == null ? "falha ao decodificar" : e.getMessage()));
                }
            } finally {
                synchronized (MlSessionEngine.this) {
                    if (mine == generation) wanted=false;
                    importing=false;fileWorker=null;
                    if (destroyed && model != null) {
                        try { model.close(); } catch (Exception ignored) {}
                        model=null;
                    }
                }
                activity.runOnUiThread(() ->
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            }
        }, "eco-audio-file-import");
        fileWorker.start();
    }

    synchronized void stop() {
        boolean wasActive = wanted || importing || preparing || speech != null;
        wanted=false;pendingFile=null;generation++;
        if (fileWorker != null) fileWorker.interrupt();
        if (speech != null) {
            try { speech.stop(); } catch(Exception ignored) {}
            try { speech.shutdown(); } catch(Exception ignored) {}
            speech=null;
        }
        if (rec != null) {
            try { rec.close(); } catch(Exception ignored) {}
            rec=null;
        }
        activity.runOnUiThread(() ->
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        if (wasActive) events.state("PARADO · ÁUDIO NÃO ARMAZENADO");
    }

    synchronized void destroy() {
        destroyed=true;
        stop();
        if (!importing && model != null) {
            try { model.close(); } catch (Exception ignored) {}
            model=null;
        }
    }

    private static String extract(String json,String field) {
        try { return new JSONObject(json).optString(field,"").trim(); }
        catch(Exception e) {return "";}
    }
    @Override public void onResult(String data) { emit(data); }
    @Override public void onFinalResult(String data) { emit(data); }
    private void emit(String data) {
        String s=extract(data,"text");
        if (!s.isEmpty()) events.segment(s);
    }
    @Override public void onPartialResult(String data) {
        long t=System.currentTimeMillis();
        if(t-lastPartialAt<850)return;
        lastPartialAt=t;
        events.partial(extract(data,"partial"));
    }
    @Override public void onError(Exception e) {
        events.state("ERRO RECONHECIMENTO: " + e.getMessage());
        stop();
    }
    @Override public void onTimeout() { stop(); }
}
