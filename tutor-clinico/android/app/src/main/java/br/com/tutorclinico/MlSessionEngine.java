package br.com.tutorclinico;

import android.app.Activity;
import android.net.Uri;
import java.io.File;
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
    interface Events {
       void state(String s); void segment(String s); void partial(String s);
       void segmentDetailed(String text,long startMs,long endMs);
       default void segmentWithSpeaker(String text,long startMs,long endMs,String cluster){segmentDetailed(text,startMs,endMs);}
       void acoustic(String measurementJson);
    }
    private final Activity activity;
    private final Events events;
    private Model model;
    private SpeechService speech;
    private Recognizer rec;
    // The old simultaneous Whisper mic path is intentionally retired.
    private WhisperLiveEngine live;
    private SessionRecorder recorder;
    private File recordedFile;
    private Uri retainedUri;
    private volatile boolean pauseRequested, paused, discarding;
    private volatile long resumeFromMs;
    private volatile boolean recordingRequested;
    private volatile boolean deferRecognitionUntilResume;
    private Uri pendingFile;
    private String fileModelName="small";
    private Thread fileWorker;
    private boolean wanted, preparing, importing, destroyed;
    private boolean backgroundAllowed;
    private boolean diarizationEnabled=true;
    private final SpeakerClusters speakerClusters=new SpeakerClusters();
    synchronized void setDiarization(boolean enabled){if(!busy())diarizationEnabled=enabled;}
    private int generation;
    private long lastPartialAt;

    MlSessionEngine(Activity activity, Events events) {
        this.activity=activity;this.events=events;
    }

    synchronized void start() { start("whisper-base"); }
    synchronized void start(String engine) {
        if (busy() || destroyed) { events.state("OPERAÇÃO EM ANDAMENTO"); return; }
        speakerClusters.clear();
        wanted=true;
        recordingRequested=true;
        pauseRequested=false;paused=false;discarding=false;resumeFromMs=0L;
        deferRecognitionUntilResume=false;
        fileModelName="tagarela".equals(engine)?"tagarela":"whisper-base".equals(engine)?"base":"small";
        recorder=new SessionRecorder(activity.getApplicationContext(),new SessionRecorder.Events(){
            @Override public void status(String s){if(!destroyed)events.state(s);}
            @Override public void acoustic(String json){
                if(!destroyed)events.acoustic(json);
            }
            @Override public void error(String s){
                synchronized(MlSessionEngine.this){wanted=false;recordingRequested=false;recorder=null;}
                if(!destroyed)events.state(s);
            }
            @Override public void ready(File file,long durationMs){
                synchronized(MlSessionEngine.this){
                    recordingRequested=false;recorder=null;
                    if(destroyed||discarding){file.delete();return;}
                    recordedFile=file;
                    retainedUri=Uri.fromFile(file);
                    pendingFile=retainedUri;
                    if(deferRecognitionUntilResume){
                        paused=true;
                        events.state("PAUSADO · GRAVAÇÃO LOCAL FINALIZADA · RETOME PARA TRANSCREVER");
                    }else{
                        events.state("GRAVAÇÃO FINALIZADA · "+(durationMs/1000)+"s · INICIANDO MOTOR OFFLINE");
                        beginFile();
                    }
                }
            }
        });
        activity.runOnUiThread(()->
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        recorder.start();
    }

    private synchronized void beginWhisperMicrophone(String modelName) {
        if(!wanted||destroyed)return;
        final WhisperLiveEngine[] holder=new WhisperLiveEngine[1];
        final WhisperLiveEngine worker=new WhisperLiveEngine(activity.getApplicationContext(),
                new WhisperLiveEngine.Listener() {
                    @Override public void state(String text) {
                        if(!destroyed)events.state(text);
                    }
                    @Override public void segment(String text,long startMs,long endMs) {
                        if(!destroyed)events.segmentDetailed(text,startMs,endMs);
                    }
                    @Override public void finished() {
                        synchronized(MlSessionEngine.this) {
                            if(live==holder[0]){live=null;wanted=false;}
                        }
                        activity.runOnUiThread(() ->
                            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
                    }
                });
        holder[0]=worker;
        live=worker;
        activity.runOnUiThread(() ->
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        worker.start(modelName);
    }

    synchronized void importFile(Uri uri) { importFile(uri,"small"); }
    synchronized void importFile(Uri uri,String modelName) {
        if (busy() || destroyed) { events.state("OPERAÇÃO EM ANDAMENTO"); return; }
        if (uri == null) { events.state("ERRO ARQUIVO: seleção inválida"); return; }
        fileModelName = "tagarela".equals(modelName)?"tagarela":"base".equals(modelName)?"base":"small";
        speakerClusters.clear();
        wanted=true;retainedUri=uri;pendingFile=uri;
        resumeFromMs=0L;pauseRequested=false;paused=false;discarding=false;
        beginFile();
    }

    private boolean busy() { return wanted || preparing || importing || paused || recordingRequested ||
           speech != null || (live!=null&&live.isBusy()) || retainedUri!=null; }

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
        if (!wanted || retainedUri == null || destroyed || importing) return;
        final Uri selected=retainedUri;
        final String selectedModel=fileModelName;
        final long fromMs=resumeFromMs;
        pendingFile=null;
        paused=false;pauseRequested=false;
        final int mine=++generation;
        importing=true;
        backgroundAllowed=TranscriptionService.begin(activity.getApplicationContext(),this::pauseTranscription);
        final SpeakerClusters clusters=diarizationEnabled?speakerClusters:null;
        events.state(fromMs>0
                ? "RETOMANDO TRANSCRIÇÃO · A PARTIR DE "+(fromMs/1000)+"s"
                : "INICIANDO TRANSCRIÇÃO OFFLINE · AGUARDE O PRIMEIRO TRECHO");
        activity.runOnUiThread(() ->
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        fileWorker=new Thread(() -> {
            boolean success=false;
            try {
                success=WhisperFileEngine.transcribe(activity.getApplicationContext(),selected,
                        new WhisperFileEngine.Listener() {
                            @Override public boolean isActive() {
                                synchronized(MlSessionEngine.this) {
                                    return wanted && !pauseRequested && !discarding && !destroyed && mine==generation;
                                }
                            }
                            @Override public void state(String text) {
                                synchronized(MlSessionEngine.this) {
                                    if(!destroyed && mine==generation && !discarding)
                                        events.state(text);
                                }
                            }
                            @Override public void segment(String text,long startMs,long endMs) {
                                if(isActive())events.segmentDetailed(text,startMs,endMs);
                            }
                            @Override public void segmentWithSpeaker(String text,long startMs,long endMs,String cluster){
                                if(isActive())events.segmentWithSpeaker(text,startMs,endMs,cluster);
                            }
                            @Override public void acoustic(String json){
                                if(isActive())events.acoustic(json);
                            }
                            @Override public void progress(long processedMs) {
                                synchronized(MlSessionEngine.this){
                                    if(mine==generation && processedMs>resumeFromMs)
                                        resumeFromMs=processedMs;
                                }
                            }
                        },selectedModel,fromMs,clusters);
            } catch(Throwable e) {
                synchronized(MlSessionEngine.this){
                    if(!destroyed && mine==generation && !pauseRequested && !discarding)
                        events.state("ERRO TRANSCRIÇÃO: "+(e.getMessage()==null?"falha interna":e.getMessage()));
                }
            } finally {
                synchronized(MlSessionEngine.this) {
                    if(mine==generation){
                        importing=false;fileWorker=null;
                        if(backgroundAllowed)TranscriptionService.end(activity.getApplicationContext());
                        backgroundAllowed=false;
                        if(pauseRequested && wanted && !discarding && !destroyed) {
                            paused=true;pauseRequested=false;
                            events.state("PAUSADO · "+(resumeFromMs/1000)+"s PROCESSADOS · REVISE OU RETOME");
                        } else if (success && !discarding && !destroyed) {
                            wanted=false;paused=false;retainedUri=null;resumeFromMs=0;
                            deleteOwnedRecording();speakerClusters.clear();
                            events.state("CONCLUÍDO · TRANSCRIÇÃO DISPONÍVEL NA LINHA DO TEMPO");
                        } else if(discarding || destroyed) {
                            wanted=false;paused=false;retainedUri=null;resumeFromMs=0L;
                            deleteOwnedRecording();
                            if(!destroyed)events.state("PARADO · TEXTO PARCIAL PRESERVADO");
                        } else {
                            // A failure must not automatically erase recoverable audio.
                            wanted=false;paused=true;
                            events.state("FALHA NA TRANSCRIÇÃO · ÁUDIO TEMPORÁRIO RETIDO PARA RETOMADA OU DESCARTE");
                        }
                    }
                }
                activity.runOnUiThread(() ->
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            }
        },"eco-whisper-transcription");
        fileWorker.start();
    }

    private void deleteOwnedRecording(){
        if(recordedFile!=null){
            File file=recordedFile;recordedFile=null;
            if(file.exists()&&!file.delete()&&!destroyed)
                events.state("AVISO · NÃO FOI POSSÍVEL ELIMINAR O ÁUDIO TEMPORÁRIO");
        }
    }

    synchronized void pauseTranscription(){
        if(importing && wanted && !pauseRequested){
            pauseRequested=true;
            events.state("PAUSANDO · FINALIZANDO TRECHO EM PROCESSAMENTO");
        }else if(paused)events.state("PAUSADO · REVISE OS TRECHOS E TOQUE EM RETOMAR");
    }
    synchronized void resumeTranscription(){
        if(destroyed || importing || recordingRequested){
            events.state("OPERAÇÃO EM ANDAMENTO");return;
        }
        if(!paused||retainedUri==null){events.state("NÃO EXISTE TRANSCRIÇÃO PAUSADA");return;}
        wanted=true;pauseRequested=false;discarding=false;deferRecognitionUntilResume=false;
        beginFile();
    }
    synchronized void discardRemainingAudio(){
        if(recordingRequested&&recorder!=null){
            discarding=true;recorder.finish();events.state("ENCERRANDO GRAVAÇÃO · DESCARTANDO ÁUDIO");return;
        }
        if(importing){
            discarding=true;wanted=false;pauseRequested=true;
            events.state("CANCELANDO PROCESSAMENTO · PRESERVANDO TEXTO RECONHECIDO");
            return; // Never delete an open audio file under a native decoder.
        }
        wanted=false;paused=false;pauseRequested=false;discarding=false;retainedUri=null;
        resumeFromMs=0L;deleteOwnedRecording();speakerClusters.clear();
        events.state("PARADO · TEXTO PARCIAL PRESERVADO");
    }
    synchronized boolean isTranscribing(){return importing;}
    synchronized void onHostPause(){
        if(recordingRequested&&recorder!=null){
            // No foreground service: never keep an undisclosed recording running
            // behind the lock screen. Preserve the WAV, then require explicit resume.
            deferRecognitionUntilResume=true;
            recorder.finish();
            events.state("APP EM SEGUNDO PLANO · FINALIZANDO GRAVAÇÃO");
        }else if(importing && !pauseRequested && !backgroundAllowed){
            // If Android refused foreground processing, preserve the existing pause fallback.
            pauseTranscription();
        }
    }

    synchronized void stop() {
        if(recordingRequested && recorder!=null){
            recorder.finish(); // asynchronous finalize; starts Whisper when WAV is closed
            return;
        }
        if(importing){pauseTranscription();return;}
        if(paused){events.state("PAUSADO · RETOMAR OU DESCARTAR ÁUDIO RESTANTE");return;}
        boolean wasActive = wanted || preparing || speech != null;
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
        if(backgroundAllowed)TranscriptionService.end(activity.getApplicationContext());
        backgroundAllowed=false;
        discarding=true;wanted=false;pauseRequested=true;
        if(recorder!=null)recorder.finish();
        if(live!=null){live.destroy();live=null;}
        if(!importing)deleteOwnedRecording();
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
