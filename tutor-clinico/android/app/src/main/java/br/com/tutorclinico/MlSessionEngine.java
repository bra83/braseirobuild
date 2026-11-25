package br.com.tutorclinico;

import android.app.Activity;
import android.net.Uri;
import java.io.File;
import android.view.WindowManager;
import android.util.Log;
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
    private final android.content.Context context;
    private volatile java.lang.ref.WeakReference<Activity> host=new java.lang.ref.WeakReference<>(null);
    private volatile Events listener;
    private volatile String lastState="PARADO";
    private final Events events=new Events(){
        public void state(String s){lastState=s;Events e=listener;if(e!=null)e.state(s);}
        public void segment(String s){Events e=listener;if(e!=null)e.segment(s);}
        public void partial(String s){Events e=listener;if(e!=null)e.partial(s);}
        public void segmentDetailed(String s,long start,long end){Events e=listener;if(e!=null)e.segmentDetailed(s,start,end);}
        public void segmentWithSpeaker(String s,long start,long end,String cluster){Events e=listener;if(e!=null)e.segmentWithSpeaker(s,start,end,cluster);}
        public void acoustic(String s){Events e=listener;if(e!=null)e.acoustic(s);}
    };
    private final SecureStore store;
    private final java.util.Set<String> emittedIds = new java.util.HashSet<>();
    private Model model;
    private SpeechService speech;
    private Recognizer rec;
    // The old simultaneous Whisper mic path is intentionally retired.
    private WhisperLiveEngine live;
    private SessionRecorder recorder;
    private File recordedFile;
    private byte[] recordedEphemeralKey;
    private Uri retainedUri;
    private volatile boolean pauseRequested, paused, discarding;
    private volatile long resumeFromMs;
    private volatile boolean recordingRequested;
    private boolean recoveryMode;
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

    MlSessionEngine(android.content.Context context) {
        this.context=context.getApplicationContext();this.store=new SecureStore(this.context);
    }
    synchronized void attach(Activity activity,Events events){host=new java.lang.ref.WeakReference<>(activity);listener=events;}
    synchronized void detach(Activity activity){
        if(host.get()!=activity)return;
        onHostPause();listener=null;host.clear();
    }
    private void keepScreen(boolean enabled){
        Activity a=host.get();if(a==null)return;
        a.runOnUiThread(()->{if(host.get()!=a||a.isDestroyed())return;
            if(enabled)a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
    }
    synchronized String activeSnapshot(){
        if(!busy()&& !hasPendingAudio())return "{}";
        try{return new JSONObject().put("jobId",currentJobId).put("caseId",currentCaseId)
            .put("sessionId",currentSessionId).put("status",lastState).put("processing",importing)
            .put("paused",paused).put("recording",recordingRequested).put("model",fileModelName).toString();}
        catch(Exception e){return "{}";}
    }

    synchronized void start() { start("whisper-base"); }
    synchronized void start(String engine) {
        if (busy() || destroyed || hasPendingAudio() || currentJobId.isEmpty()) { events.state("ERRO · CONTEXTO INDISPONÍVEL OU OPERAÇÃO EM ANDAMENTO"); return; }
        speakerClusters.clear();
        wanted=true;
        recordingRequested=true;
        recoveryMode=false;
        sessionEmittedSegments=0;
        lastChunksProcessed=0;
        pauseRequested=false;paused=false;discarding=false;userExplicitDiscard=false;resumeFromMs=0L;
        deferRecognitionUntilResume=false;
        fileModelName="vosk".equals(engine)?"vosk":"tagarela".equals(engine)?"tagarela":"whisper-base".equals(engine)?"base":"small";
        recorder=new SessionRecorder(context,new SessionRecorder.Events(){
            @Override public void status(String s){if(!destroyed)events.state(s);}
            @Override public void onStarted(File file, byte[] ephemeralKey) throws Exception {
                synchronized(MlSessionEngine.this) {
                    if (currentJobId.isEmpty()) throw new IllegalStateException("Job obrigatório antes de gravar");
                    EcoDatabaseHelper dbHelper = store.getDbHelper();
                    dbHelper.saveAppConfig("asr:" + currentJobId, fileModelName);
                    if (!dbHelper.updateJobAudio(currentJobId, file.getAbsolutePath(), ephemeralKey)) {
                        throw new IllegalStateException("Não foi possível persistir chave e áudio antes de gravar");
                    }
                    recordedFile = file;
                    recordedEphemeralKey = ephemeralKey;
                    setActiveEphemeralKey(ephemeralKey);
                    retainedUri=Uri.fromFile(file);
                    pendingFile=retainedUri;
                    beginFile(recorder.boundary);
                }
            }
            @Override public void acoustic(String json){
                if(!destroyed)events.acoustic(json);
            }
            @Override public void error(String s){
                synchronized(MlSessionEngine.this){wanted=false;recordingRequested=false;recorder=null;}
                if(!destroyed)events.state(s);
            }
            @Override public void failed(File file, byte[] ephemeralKey, String message){
                synchronized(MlSessionEngine.this){
                    recordingRequested=false;recorder=null;
                    if(importing && file!=null && file.isFile()){
                        recoveryMode=true;
                        events.state("FINALIZANDO GRAVAÇÃO · CAPTURA INTERROMPIDA · RECUPERANDO TRECHOS");
                        return;
                    }
                    wanted=false;
                    if(file!=null && file.isFile()){
                        recordedFile=file;recordedEphemeralKey=ephemeralKey;
                        retainedUri=Uri.fromFile(file);paused=true;recoveryMode=true;
                        events.state("FALHA NA TRANSCRIÇÃO · CAPTURA INTERROMPIDA · RETOME PARA RECUPERAR TRECHOS");
                    }else{
                        recordedFile=null;recordedEphemeralKey=null;setActiveEphemeralKey(null);
                        try{store.getDbHelper().updateJobState(currentJobId,"FAILED_EMPTY",null);store.getDbHelper().clearJobAudioKey(currentJobId);}catch(Exception e){Log.e("MlSessionEngine","Limpeza pendente",e);}
                        events.state(message);
                    }
                }
            }
            @Override public void ready(File file,long durationMs,byte[] ephemeralKey){
                synchronized(MlSessionEngine.this){
                    recordingRequested=false;recorder=null;
                    if(userExplicitDiscard){recordedFile=file;recordedEphemeralKey=ephemeralKey;if(!importing){deleteOwnedRecording();wanted=false;}return;}
                    recordedFile=file;
                    recordedEphemeralKey=ephemeralKey;
                    setActiveEphemeralKey(ephemeralKey);
                    // Audio path and key were durably registered before AudioRecord started.
                    retainedUri=Uri.fromFile(file);
                    pendingFile=retainedUri;
                    if(importing){
                        events.state("GRAVAÇÃO FINALIZADA · "+(durationMs/1000)+"s · CONCLUINDO TRANSCRIÇÃO");
                        return;
                    }
                    restoreDecoderPrefix();
                    if(deferRecognitionUntilResume || destroyed){
                        paused=true;
                        events.state("PAUSADO · GRAVAÇÃO LOCAL FINALIZADA · RETOME PARA TRANSCREVER");
                    }else{
                        events.state("GRAVAÇÃO FINALIZADA · "+(durationMs/1000)+"s · INICIANDO MOTOR OFFLINE");
                        beginFile();
                    }
                }
            }
        });
        keepScreen(true);
        recorder.start();
    }

    private synchronized void beginWhisperMicrophone(String modelName) {
        if(!wanted||destroyed)return;
        final WhisperLiveEngine[] holder=new WhisperLiveEngine[1];
        final WhisperLiveEngine worker=new WhisperLiveEngine(context,
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
                        keepScreen(false);
                    }
                });
        holder[0]=worker;
        live=worker;
        keepScreen(true);
        worker.start(modelName);
    }

    synchronized void importFile(Uri uri) { importFile(uri,"small"); }
    synchronized void importFile(Uri uri,String modelName) {
        if (busy() || destroyed || hasPendingAudio() || currentJobId.isEmpty()) { events.state("ERRO · CONTEXTO INDISPONÍVEL OU OPERAÇÃO EM ANDAMENTO"); return; }
        if (uri == null) { events.state("ERRO ARQUIVO: seleção inválida"); return; }
        recoveryMode=false;
        fileModelName = "vosk".equals(modelName)?"vosk":"tagarela".equals(modelName)?"tagarela":"base".equals(modelName)?"base":"small";
        setActiveEphemeralKey(null);
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
        StorageService.unpack(context, "model-pt", "modelo-pt", loaded -> {
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
            keepScreen(true);
            events.state("CAPTURANDO · VOSK PT-BR");
        } catch (Exception e) {
            stop();
            events.state("ERRO MICROFONE: " + e.getMessage());
        }
    }

    private synchronized void beginFile(){beginFile(null);}
    private synchronized void beginFile(LiveSpoolInput.Boundary liveBoundary) {
        if (!wanted || retainedUri == null || destroyed || importing) return;
        final Uri selected=retainedUri;
        final String selectedModel=fileModelName;
        final long fromMs=resumeFromMs;
        final byte[] key=recordedEphemeralKey==null?null:recordedEphemeralKey.clone();
        final boolean allowInterrupted=recoveryMode;
        pendingFile=null;
        paused=false;pauseRequested=false;
        final int mine=++generation;
        importing=true;
        backgroundAllowed=TranscriptionService.begin(context,this::pauseTranscription);
        final SpeakerClusters clusters=diarizationEnabled?speakerClusters:null;
        events.state(fromMs>0
                ? "RETOMANDO TRANSCRIÇÃO · A PARTIR DE "+(fromMs/1000)+"s"
                : "INICIANDO TRANSCRIÇÃO OFFLINE · AGUARDE O PRIMEIRO TRECHO");
        keepScreen(true);
        fileWorker=new Thread(() -> {
            boolean success=false;
            try {
                WhisperFileEngine.Listener callback=new WhisperFileEngine.Listener() {
                            @Override public boolean isActive() {
                                synchronized(MlSessionEngine.this) {
                                    return wanted && !pauseRequested && !discarding && !destroyed && mine==generation;
                                }
                            }
                            @Override public void state(String text) {
                                synchronized(MlSessionEngine.this) {
                                    if(!destroyed && mine==generation && !discarding)
                                        events.state(recordingRequested?"GRAVANDO · "+text:text);
                                }
                            }
                            @Override public void segment(String text,long startMs,long endMs) {
                                synchronized(MlSessionEngine.this){ if(isActive()){
                                    boolean committed = commitSegmentToDb(text, startMs, endMs, "desconhecido");
                                    if (!committed) {
                                        events.state("ERRO DE PERSISTÊNCIA NATIVA · ÁUDIO RETIDO PARA RECUPERAÇÃO");
                                        pauseTranscription();
                                        return;
                                    }
                                    events.segmentDetailed(text,startMs,endMs);
                                }}
                            }
                            @Override public void segmentWithSpeaker(String text,long startMs,long endMs,String cluster){
                                synchronized(MlSessionEngine.this){ if(isActive()){
                                    boolean committed = commitSegmentToDb(text, startMs, endMs, cluster);
                                    if (!committed) {
                                        events.state("ERRO DE PERSISTÊNCIA NATIVA · ÁUDIO RETIDO PARA RECUPERAÇÃO");
                                        pauseTranscription();
                                        return;
                                    }
                                    events.segmentWithSpeaker(text,startMs,endMs,cluster);
                                }}
                            }
                            @Override public void acoustic(String json){
                                if(isActive()&&liveBoundary==null)events.acoustic(json);
                            }
                            @Override public void chunksProcessed(int count) {
                                synchronized(MlSessionEngine.this) {
                                    lastChunksProcessed = count;
                                }
                            }
                            @Override public void progress(long processedMs) {
                                synchronized(MlSessionEngine.this){
                                    // Advance only after a complete chunk and successful segment commits.
                                    if(mine==generation && wanted && !pauseRequested && !discarding && !destroyed && processedMs>resumeFromMs){
                                        if(recordedFile!=null&&recordedEphemeralKey!=null&&selected.equals(Uri.fromFile(recordedFile))){
                                            if(processedMs%50!=0)return;
                                            try{
                                                store.getDbHelper().saveDecoderCheckpoint(currentJobId,selectedModel,processedMs,emittedIds,speakerClusters.checkpoint(),diarizationEnabled);
                                            }catch(Exception e){
                                                events.state("FALHA AO CONFIRMAR CHECKPOINT · ÁUDIO RETIDO");
                                                pauseTranscription();return;
                                            }
                                        }
                                        resumeFromMs=processedMs;
                                    }
                                }
                            }
                        };
                success=liveBoundary==null
                    ?WhisperFileEngine.transcribe(context,selected,callback,selectedModel,fromMs,clusters,key,allowInterrupted)
                    :WhisperFileEngine.transcribeLive(context,selected,callback,selectedModel,clusters,key,liveBoundary);
            } catch(Throwable e) {
                synchronized(MlSessionEngine.this){
                    if(!destroyed && mine==generation && !pauseRequested && !discarding)
                        events.state("ERRO TRANSCRIÇÃO: "+(e.getMessage()==null?"falha interna":e.getMessage()));
                }
            } finally {
                if(key!=null)java.util.Arrays.fill(key,(byte)0);
                synchronized(MlSessionEngine.this) {
                    if(mine==generation){
                        importing=false;fileWorker=null;
                        if(backgroundAllowed)TranscriptionService.end(context);
                        backgroundAllowed=false;
                        if(recordingRequested&&liveBoundary!=null){
                            // Capture continues independently after inference failure. The closed
                            // spool will be replayed from the last verified boundary when ready.
                            pauseRequested=false;
                            events.state("GRAVANDO · TRANSCRIÇÃO INCREMENTAL INTERROMPIDA · ÁUDIO PRESERVADO");
                            return;
                        }
                        if(pauseRequested && wanted && !discarding && !destroyed) {
                            paused=true;pauseRequested=false;
                            events.state("PAUSADO · "+(resumeFromMs/1000)+"s PROCESSADOS · REVISE OU RETOME");
                        } else if (success && lastChunksProcessed < 0 && !discarding && !destroyed) {
                            wanted=false;paused=true;
                            // The verified prefix has been durably stored. No complete-EOF ACK.
                            events.state("FALHA NA TRANSCRIÇÃO · GRAVAÇÃO INCOMPLETA · TRECHOS RECUPERADOS; REVISE E DESCARTE O ÁUDIO RESTANTE");
                        } else if (success && !discarding && !destroyed) {
                            // P0-01/P0-02: Mark job as DECODED_VERIFIED in database using verified counts!
                            boolean markOk = false;
                            try {
                                EcoDatabaseHelper dbHelper = store.getDbHelper();
                                markOk = dbHelper.markJobDecoded(currentJobId, emittedIds.size(), lastChunksProcessed)
                                        && dbHelper.acknowledgePersistence(currentJobId);
                            } catch (Exception e) { Log.e("MlSessionEngine", "Falha de persistência final", e); }
                            if (!markOk) {
                                wanted=false;paused=true;
                                events.state("FALHA NA TRANSCRIÇÃO · CONFIRMAÇÃO NATIVA PENDENTE · ÁUDIO RETIDO");
                            } else {
                                wanted=false;paused=false;retainedUri=null;resumeFromMs=0;
                                persistenceAckReceived=true;
                                deleteOwnedRecording();
                                events.state("CONCLUÍDO · TRANSCRIÇÃO DISPONÍVEL NA LINHA DO TEMPO");
                            }
                            speakerClusters.clear();
                        } else if(discarding) {
                            wanted=false;paused=false;retainedUri=null;resumeFromMs=0L;
                            deleteOwnedRecording();
                            if(!destroyed)events.state("PARADO · TEXTO PARCIAL PRESERVADO");
                        } else if(destroyed) {
                            wanted=false;paused=false;retainedUri=null;
                            // Activity destroyed: do NOT delete unpersisted recording! Retain for recovery!
                        } else {
                            // A failure must not automatically erase recoverable audio.
                            wanted=false;paused=true;
                            events.state("FALHA NA TRANSCRIÇÃO · ÁUDIO TEMPORÁRIO RETIDO PARA RETOMADA OU DESCARTE");
                        }
                    }
                }
                if(!recordingRequested)keepScreen(false);
            }
        },"eco-whisper-transcription");
        fileWorker.start();
    }

    public static byte[] getActiveEphemeralKey() { return SessionRecorder.getActiveEphemeralKey(); }
    public static void setActiveEphemeralKey(byte[] key) { SessionRecorder.setActiveEphemeralKey(key); }

    private volatile boolean persistenceAckReceived = false;
    private volatile boolean userExplicitDiscard = false;
    private volatile int sessionEmittedSegments = 0;
    private volatile int lastChunksProcessed = 0;
    private volatile String currentJobId = "";
    private volatile String currentCaseId = "";
    private volatile String currentSessionId = "";

    public synchronized String getCurrentJobId() { return currentJobId; }

    private boolean isJobCommitted(String jobId) {
        if (jobId == null || jobId.isEmpty()) return false;
        try {
            EcoDatabaseHelper dbHelper = store.getDbHelper();
            return dbHelper.isJobCommitted(jobId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean commitSegmentToDb(String text, long startMs, long endMs, String speaker) {
        if (currentJobId.isEmpty() || currentCaseId.isEmpty() || currentSessionId.isEmpty()) return false;
        try {
            EcoDatabaseHelper dbHelper = store.getDbHelper();
            String checksum = EcoDatabaseHelper.computeChecksum(text);
            boolean ok = dbHelper.commitSegment(currentJobId, currentCaseId, currentSessionId, startMs, endMs, speaker, text, checksum);
            if (ok) {
                emittedIds.add(currentJobId + "_" + startMs + "_" + endMs);
                sessionEmittedSegments = emittedIds.size();
            }
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    synchronized void setJobContext(String jobId, String caseId) {
        setJobContext(jobId, caseId, "");
    }

    synchronized void setJobContext(String jobId, String caseId, String sessionId) {
        this.currentJobId = jobId == null ? "" : jobId;
        this.currentCaseId = caseId == null ? "" : caseId;
        this.currentSessionId = sessionId == null ? "" : sessionId;
        this.sessionEmittedSegments = 0;
        this.emittedIds.clear();
        this.userExplicitDiscard = false;
        this.lastChunksProcessed = 0;
        this.persistenceAckReceived = false;
        TranscriptionService.registerJob(jobId, caseId, sessionId);
    }

    synchronized boolean acknowledgePersistence(String jobId) {
        if (jobId == null || jobId.trim().isEmpty() || !jobId.equals(currentJobId)) {
            return false;
        }
        EcoDatabaseHelper dbHelper = store.getDbHelper();
        boolean ok = dbHelper.acknowledgePersistence(jobId);
        if (ok) {
            persistenceAckReceived = true;
            if (!wanted && !paused && retainedUri == null && recordedFile != null) {
                deleteOwnedRecording();
            }
        }
        return ok;
    }

    private void deleteOwnedRecording(){
        if(!userExplicitDiscard && !persistenceAckReceived){
            return;
        }
        if(currentJobId != null && !currentJobId.isEmpty() && !userExplicitDiscard){
            if(!isJobCommitted(currentJobId)){
                return; // Prohibit audio deletion until durable confirmation of all segments and verified EOF
            }
        }
        if (currentJobId != null && !currentJobId.isEmpty()) {
            try {
                EcoDatabaseHelper dbHelper = store.getDbHelper();
                if (userExplicitDiscard) dbHelper.updateJobState(currentJobId, "DISCARDED", null);
                if (!dbHelper.clearJobAudioKey(currentJobId)) {
                    events.state("FALHA · LIMPEZA DE CHAVE PENDENTE"); return;
                }
            } catch (Exception e) { events.state("FALHA · LIMPEZA DE CHAVE PENDENTE"); return; }
        }
        if(recordedFile!=null){
            File file=recordedFile;recordedFile=null;
            byte[] key=recordedEphemeralKey;recordedEphemeralKey=null;
            setActiveEphemeralKey(null);
            SessionRecorder.wipeAndClean(file, key);
            file.delete();
        }
    }

    synchronized boolean resumeJob(String jobId) {
        if (busy() || destroyed || jobId == null || jobId.isEmpty() || (recordedFile != null && !jobId.equals(currentJobId))) return false;
        try {
            EcoDatabaseHelper dbHelper = store.getDbHelper();
            JSONObject job = dbHelper.getJob(jobId);
            if (job == null || !java.util.Arrays.asList("PROCESSING","PAUSED","DECODED_VERIFIED").contains(job.optString("state"))) return false;
            String path = job.optString("audio_path");
            if (path == null || path.isEmpty()) return false;
            File file = new File(path);
            if (!SessionRecorder.isOwnedSpool(context, file) || !file.isFile() || file.length() == 0) return false;
            byte[] key = dbHelper.getJobKey(jobId);
            if (key == null) return false;

            String savedModel = dbHelper.getAppConfig("asr:" + jobId);
            if (!java.util.Arrays.asList("tagarela","base","small","vosk").contains(savedModel)) savedModel = "tagarela";
            setJobContext(jobId, job.optString("case_id"), job.optString("session_id"));
            fileModelName = savedModel;
            recoveryMode = true;
            dbHelper.updateJobState(jobId, "PROCESSING", null);
            recordedFile = file;
            recordedEphemeralKey = key;
            setActiveEphemeralKey(key);
            retainedUri = Uri.fromFile(file);
            pendingFile = retainedUri;
            wanted = true;
            paused = false;
            // Never use the last word timestamp as a decoder boundary.
            restoreDecoderPrefix();
            beginFile();
            return true;
        } catch (Exception e) {
            Log.e("MlSessionEngine", "Erro ao retomar job: " + jobId, e);
            return false;
        }
    }

    private void restoreDecoderPrefix(){
        resumeFromMs=0L;emittedIds.clear();sessionEmittedSegments=0;speakerClusters.clear();
        if(recordedFile==null||recordedEphemeralKey==null||retainedUri==null||!retainedUri.equals(Uri.fromFile(recordedFile)))return;
        try{
            EcoDatabaseHelper db=store.getDbHelper();
            JSONObject cp=db.readDecoderCheckpoint(currentJobId,fileModelName,recordedEphemeralKey);
            if(cp==null)return;
            speakerClusters.restore(cp.getString("speakers"));
            diarizationEnabled=cp.getBoolean("diarization");
            resumeFromMs=cp.getLong("offset");
            emittedIds.addAll(db.verifiedPrefixIds(currentJobId,resumeFromMs));
            sessionEmittedSegments=emittedIds.size();
        }catch(Exception e){
            resumeFromMs=0L;emittedIds.clear();sessionEmittedSegments=0;speakerClusters.clear();
            events.state("CHECKPOINT INDISPONÍVEL · REPLAY SEGURO DO ÁUDIO");
        }
    }

    synchronized void pauseTranscription(){
        if(importing && wanted && !pauseRequested){
            pauseRequested=true;
            events.state(recordingRequested?"GRAVANDO · PAUSANDO TRANSCRIÇÃO INCREMENTAL":"PAUSANDO · FINALIZANDO TRECHO EM PROCESSAMENTO");
        }else if(paused)events.state("PAUSADO · REVISE OS TRECHOS E TOQUE EM RETOMAR");
    }
    synchronized void resumeTranscription(){
        if(destroyed || importing || recordingRequested){
            events.state("OPERAÇÃO EM ANDAMENTO");return;
        }
        if(!paused||retainedUri==null){events.state("NÃO EXISTE TRANSCRIÇÃO PAUSADA");return;}
        wanted=true;pauseRequested=false;discarding=false;userExplicitDiscard=false;deferRecognitionUntilResume=false;
        restoreDecoderPrefix();
        lastChunksProcessed=0;
        beginFile();
    }
    // Explicitly park an owned encrypted recording after the decoder stops.
    // The SQLite job/key remain intact; the next patient gets an entirely new context.
    synchronized boolean parkPausedJob(){
        if(!paused||importing||recordingRequested||recorder!=null||recordedFile==null
                ||recordedEphemeralKey==null||currentJobId.isEmpty())return false;
        try{
            EcoDatabaseHelper db=store.getDbHelper();
            JSONObject job=db.getJob(currentJobId);
            if(job==null||!currentCaseId.equals(job.optString("case_id"))
                    ||!currentSessionId.equals(job.optString("session_id"))
                    ||!recordedFile.getAbsolutePath().equals(job.optString("audio_path"))
                    ||!recordedFile.isFile()||!SessionRecorder.isOwnedSpool(context,recordedFile))return false;
            byte[] persisted=db.getJobKey(currentJobId);
            if(persisted==null)return false;
            boolean same=java.security.MessageDigest.isEqual(persisted,recordedEphemeralKey);
            java.util.Arrays.fill(persisted,(byte)0);
            if(!same)return false;
            db.updateJobState(currentJobId,"PAUSED",null);
            if(!"PAUSED".equals(db.getJob(currentJobId).optString("state")))return false;
            generation++;wanted=false;paused=false;pauseRequested=false;discarding=false;
            retainedUri=null;pendingFile=null;recordedFile=null;
            java.util.Arrays.fill(recordedEphemeralKey,(byte)0);recordedEphemeralKey=null;setActiveEphemeralKey(null);
            resumeFromMs=0;emittedIds.clear();speakerClusters.clear();sessionEmittedSegments=0;lastChunksProcessed=0;
            currentJobId="";currentCaseId="";currentSessionId="";
            TranscriptionService.clearJob();keepScreen(false);
            events.state("PARADO · TEXTO PARCIAL PRESERVADO · ÁUDIO GUARDADO NA FILA");
            return true;
        }catch(Exception e){return false;}
    }
    synchronized void discardRemainingAudio(){
        userExplicitDiscard = true;
        discarding = true;
        if(recordingRequested&&recorder!=null){
            recorder.finish();events.state("ENCERRANDO GRAVAÇÃO · DESCARTANDO ÁUDIO");return;
        }
        if(importing){
            wanted=false;pauseRequested=true;
            events.state("CANCELANDO PROCESSAMENTO · PRESERVANDO TEXTO RECONHECIDO");
            return; // Never delete an open audio file under a native decoder.
        }
        wanted=false;paused=false;pauseRequested=false;discarding=false;retainedUri=null;
        resumeFromMs=0L;deleteOwnedRecording();speakerClusters.clear();
        events.state("PARADO · TEXTO PARCIAL PRESERVADO");
    }
    synchronized boolean hasPendingAudio(){return recordedFile != null;}
    synchronized String getCurrentCaseId(){return currentCaseId;}
    synchronized String getCurrentSessionId(){return currentSessionId;}
    synchronized boolean isTranscribing(){return importing;}
    synchronized boolean isRecording(){return recordingRequested || (recorder != null);}
    synchronized boolean isPaused(){return paused;}
    synchronized void onHostPause(){
        if(recordingRequested&&recorder!=null){
            // No foreground service: never keep an undisclosed recording running
            // behind the lock screen. Preserve the WAV, then require explicit resume.
            deferRecognitionUntilResume=true;
            recorder.finish();
            if(importing)pauseRequested=true;
            events.state("APP EM SEGUNDO PLANO · FINALIZANDO GRAVAÇÃO");
        }else if(importing && !pauseRequested && !backgroundAllowed){
            // If Android refused foreground processing, preserve the existing pause fallback.
            pauseTranscription();
        }
    }

    synchronized void stop() {
        if(recordingRequested && recorder!=null){
            recorder.finish(); // seals authenticated EOF; incremental decoder drains remaining frames
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
        keepScreen(false);
        if (wasActive) events.state("PARADO · ÁUDIO NÃO ARMAZENADO");
    }

    synchronized void destroy() {
        destroyed=true;
        if(backgroundAllowed)TranscriptionService.end(context);
        backgroundAllowed=false;
        wanted=false;pauseRequested=true;
        if(recorder!=null)recorder.finish();
        if(live!=null){live.destroy();live=null;}
        // P0-03: Activity destroy must NEVER delete uncommitted recordings!
        if(!importing && isJobCommitted(currentJobId))deleteOwnedRecording();
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
