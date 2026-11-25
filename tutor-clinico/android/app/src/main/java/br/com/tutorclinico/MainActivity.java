package br.com.tutorclinico;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.print.PrintManager;
import android.print.PrintAttributes;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.content.res.Configuration;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    // Clean orphaned temporary recordings only once per app process.
    // Activity recreation must not delete audio still being processed by the old instance.
    private static final java.util.concurrent.atomic.AtomicBoolean audioCleanedThisProcess =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final int MIC_PERMISSION = 903;
    private static final int AUDIO_FILE_CODE = 904;
    private MlSessionEngine ml;
    private final java.util.concurrent.ExecutorService semanticWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final java.util.concurrent.atomic.AtomicBoolean semanticBusy=new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile long semanticEpoch=0;
    private final EcoEmbeddingCache embeddingCache=new EcoEmbeddingCache();
    private volatile String semanticJobId="";
    private String selectedAsrModel="small";
    private String selectedMicEngine="whisper-base";
    private static final int EXPORT_CODE = 901;
    private static final int IMPORT_CODE = 902;
    private WebView web;
    private SecureStore store;
    private SharedPreferences pref;
    private byte[] pendingExport;
    private String pendingPassword;
    private volatile boolean unlocked = false;
    private boolean showingLock = false;
    private long backgroundAt = 0;
    private boolean initialUnlockDone=false;
    private volatile boolean journalWhileLocked=false;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        store = new SecureStore(this);
        // On a new process, purge files abandoned by prior crashes. This must NOT
        // run again on an Activity recreation while a previous worker is still alive.
        if(audioCleanedThisProcess.compareAndSet(false,true)){
            WhisperFileEngine.cleanupAbandonedAudio(this);
            WhisperLiveEngine.clearStaleFiles(this);
            SessionRecorder.cleanAbandoned(this);
        }
        ml = new MlSessionEngine(this,new MlSessionEngine.Events(){
            @Override public void state(String s){jsCallback("nativeMlState",s);}
            @Override public void segment(String s){jsCallback("nativeMlSegment",s);}
            @Override public void segmentDetailed(String s,long startMs,long endMs){segmentWithSpeaker(s,startMs,endMs,"");}
            @Override public void segmentWithSpeaker(String s,long startMs,long endMs,String cluster){
                try {
                    JSONObject v=new JSONObject();
                    v.put("text",s);v.put("startMs",startMs);v.put("endMs",endMs);v.put("cluster",cluster);
                    String jobId = ml.getCurrentJobId();
                    v.put("jobId", jobId);v.put("caseId", ml.getCurrentCaseId());v.put("sessionId", ml.getCurrentSessionId());
                    v.put("id", (jobId != null && !jobId.isEmpty()) ? (jobId + "_" + startMs + "_" + endMs) : "");
                    jsCallback("nativeMlSegmentDetailed",v.toString());
                }catch(Exception e){jsCallback("nativeMlState","ERRO METADADOS DO TRECHO");}
            }
            @Override public void partial(String s){jsCallback("nativeMlPartial",s);}
            @Override public void acoustic(String json){jsCallback("nativeMlAcoustic",json);}
        });
        pref = getSharedPreferences("tutor-lock", MODE_PRIVATE);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(246,248,246));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (unlocked) view.evaluateJavascript("window.appUnlock && window.appUnlock()", null);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return true; }
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) { return true; }
        });
        web.addJavascriptInterface(new Bridge(), "NativeVault");
        web.setVisibility(View.INVISIBLE);
        setContentView(web);
        try (InputStream in = getAssets().open("index.html")) {
            String html = new String(readLimited(in, 1000000), StandardCharsets.UTF_8);
            try (InputStream icon = getAssets().open("eco_logo.png")) {
                String iconUri = "data:image/png;base64," + android.util.Base64.encodeToString(readLimited(icon,100000),android.util.Base64.NO_WRAP);
                html = html.replace("{{ECO_APP_ICON}}", iconUri);
            }
            web.loadDataWithBaseURL("https://app.local/", html, "text/html", "UTF-8", null);
        } catch (Exception e) { showToast("Falha ao carregar aplicativo"); }
        requestUnlock();
    }
    private static byte[] readLimited(InputStream in, int max) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192]; int n;
        while ((n=in.read(chunk)) != -1) {
            if (out.size() + n > max) throw new IllegalArgumentException("Arquivo grande demais");
            out.write(chunk,0,n);
        }
        return out.toByteArray();
    }
    private void showToast(String msg) { main.post(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show()); }
    private EditText passwordInput(String hint) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setSingleLine(true); input.setHint(hint);
        int d = (int)(22 * getResources().getDisplayMetrics().density);
        input.setPadding(d, d, d, d);
        return input;
    }
    private void requestUnlock() {
        if (showingLock || isFinishing()) return;
        showingLock = true; journalWhileLocked=initialUnlockDone&&ml!=null&&ml.isTranscribing(); unlocked = false; semanticEpoch++; embeddingCache.clear(); web.setVisibility(View.INVISIBLE);
        boolean needsCreate = !pref.contains("pin_hash");
        EditText input = passwordInput(needsCreate ? "Crie um PIN com 6 ou mais dígitos" : "Digite seu PIN");
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(needsCreate ? "Proteger o ECO" : "Desbloquear ECO")
            .setMessage(needsCreate ? "Os registros serão criptografados neste aparelho. Guarde seu PIN e faça cópias protegidas." : "Insira o PIN local para abrir os dados clínicos.")
            .setView(input).setCancelable(false)
            .setPositiveButton(needsCreate ? "Criar PIN" : "Desbloquear", null)
            .setNegativeButton("Fechar", (d,w) -> finish()).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pin = input.getText().toString();
            try {
                if (!pin.matches("[0-9]{6,}")) { input.setError("Use pelo menos 6 dígitos"); return; }
                if (System.currentTimeMillis() < pref.getLong("blocked_until", 0)) {
                    showToast("Muitas tentativas. Aguarde 60 segundos."); return;
                }
                if (needsCreate) {
                    String salt = SecureStore.newSalt();
                    pref.edit().putString("pin_salt", salt).putString("pin_hash", SecureStore.hashPin(pin,salt)).apply();
                } else {
                    String expected = pref.getString("pin_hash", "");
                    String actual = SecureStore.hashPin(pin, pref.getString("pin_salt", ""));
                    if (!SecureStore.equalHash(expected, actual)) {
                        int failures = pref.getInt("failed", 0) + 1;
                        SharedPreferences.Editor editor = pref.edit().putInt("failed", failures);
                        if (failures >= 5) editor.putLong("blocked_until", System.currentTimeMillis()+60000).putInt("failed",0);
                        editor.apply(); showToast("PIN incorreto"); return;
                    }
                }
                pref.edit().putInt("failed",0).putLong("blocked_until",0).apply();
                unlocked = true; journalWhileLocked=false; showingLock = false; backgroundAt=0;
                web.setVisibility(View.VISIBLE); dialog.dismiss();
                if(!initialUnlockDone){initialUnlockDone=true;web.evaluateJavascript("window.appUnlock && window.appUnlock()", null);}
                else web.evaluateJavascript("window.nativeSemanticUnlocked && window.nativeSemanticUnlocked()",null);
            } catch (Exception e) { showToast("Falha ao verificar PIN"); }
        }));
        dialog.show();
    }
    // Rotation must NOT destroy the WebView, recorder, or transcription worker.
    // The manifest opts into these configuration changes; onCreate is not called
    // and the existing unlocked session remains in memory.
    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(web!=null)web.requestLayout();
    }

    @Override protected void onPause() {
        // A system file picker, permissions dialog or app switch invokes onPause.
        // Do not cancel a Whisper decoder or release a microphone from the UI thread.
        if(ml!=null)ml.onHostPause();
        backgroundAt = System.currentTimeMillis();
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (unlocked && backgroundAt > 0 && System.currentTimeMillis()-backgroundAt > 120000) requestUnlock();
    }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            embeddingCache.clear();
            TagarelaEngine.Companion.releaseCached();
        }
    }
    @Override protected void onDestroy() { semanticEpoch++; embeddingCache.clear(); semanticWorker.shutdownNow(); if(ml!=null)ml.destroy(); if (web != null) web.destroy(); super.onDestroy(); }
    private void jsCallback(String fn, String arg) {
        main.post(() -> { if (!isFinishing() && !isDestroyed() && web != null) web.evaluateJavascript("window."+fn+"("+JSONObject.quote(arg)+")",null); });
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(code,permissions,grants);
        if(code==MIC_PERMISSION){
            if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED&&unlocked)ml.start(selectedMicEngine);
            else jsCallback("nativeMlState","MICROFONE NEGADO");
        }
    }
    private void beginCapture(){
        if(!unlocked)return;
        if(semanticBusy.get()){jsCallback("nativeMlState","ERRO: análise semântica em andamento");return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_PERMISSION);
            return;
        }
        ml.start(selectedMicEngine);
    }
    private void chooseAudioFile(){
        if(!unlocked){jsCallback("nativeMlState","ERRO ARQUIVO: app bloqueado");return;}
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{
                "audio/mpeg","audio/mp3","audio/wav","audio/x-wav",
                "audio/mp4","audio/x-m4a","audio/flac","audio/x-flac","audio/ogg","audio/aac"
        });
        try{startActivityForResult(intent,AUDIO_FILE_CODE);}
        catch(Exception e){jsCallback("nativeMlState","ERRO ARQUIVO: seletor indisponível");}
    }
    private void openAudioSaf(Uri uri){
        if(!unlocked){jsCallback("nativeMlState","ERRO ARQUIVO: app bloqueado");return;}
        if(semanticBusy.get()){jsCallback("nativeMlState","ERRO: análise semântica em andamento");return;}
        try(Cursor cur=getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){
            if(cur!=null && cur.moveToFirst()){
                int col=cur.getColumnIndex(OpenableColumns.SIZE);
                if(col>=0&&!cur.isNull(col)&&cur.getLong(col)>AudioFileTranscriber.MAX_FILE_BYTES){
                    jsCallback("nativeMlState","ERRO ARQUIVO: tamanho acima de 650 MB");
                    return;
                }
            }
        }catch(Exception ignored){ /* Some providers do not expose length; decoder enforces duration. */ }
        ml.importFile(uri,selectedAsrModel);
    }
    private final class Bridge {
        @JavascriptInterface public void setDiarization(boolean enabled){if(unlocked)ml.setDiarization(enabled);}
        @JavascriptInterface public void clearSemanticCache(){embeddingCache.clear();}
        @JavascriptInterface public String cancelSemantic(String id){
            if(id!=null&&id.equals(semanticJobId)){semanticEpoch++;embeddingCache.clear();return "OK";}
            return "INATIVO";
        }
        @JavascriptInterface public String analyzeSemantic(String json){
            if(!unlocked)return "ECO bloqueado";
            if(ml.isRecording()||ml.isTranscribing())return "Motor de áudio ocupado";
            if(json==null||json.length()>500000)return "Solicitação acima do limite";
            final JSONObject req;
            try{req=new JSONObject(json);}catch(Exception e){return "Solicitação inválida";}
            final String id=req.optString("id");
            if(id.isEmpty())return "Identificador inválido";
            if(!semanticBusy.compareAndSet(false,true))return "Motor ocupado; aguarde o encerramento da execução anterior.";
            semanticJobId=id;
            final long epoch=semanticEpoch;
            try{semanticWorker.execute(()->{
                JSONObject result=new JSONObject();
                java.util.function.BooleanSupplier active=()->unlocked&&semanticEpoch==epoch&&!Thread.currentThread().isInterrupted();
                final long started=android.os.SystemClock.elapsedRealtime();
                final long[] last={0};
                NeuralSemanticEngine.Progress progress=(done,total,stage)->{
                    if(!active.getAsBoolean())return;
                    long now=android.os.SystemClock.elapsedRealtime();
                    if(done>0&&done<total&&now-last[0]<500)return;
                    last[0]=now;
                    try{
                        String data=new JSONObject().put("id",id).put("done",done).put("total",total).put("stage",stage).put("elapsedMs",now-started).toString();
                        main.post(()->{if(unlocked&&semanticEpoch==epoch&&!isDestroyed()&&!isFinishing())web.evaluateJavascript("window.nativeSemanticProgress("+JSONObject.quote(data)+")",null);});
                    }catch(Exception ignored){}
                };
                try{
                    TagarelaEngine.Companion.releaseCached();
                    System.gc();
                    if("generate".equals(req.optString("mode"))){
                        result=LocalLlmEngine.analyze(getApplicationContext(),req,active,progress);
                    }else if("rerank".equals(req.optString("mode"))){
                        int count=req.getJSONArray("rows").length();
                        progress.report(0,count,"Carregando reranker mMARCO");
                        try(RerankEngine engine=new RerankEngine(getApplicationContext(),active)){result=engine.analyze(req,active,progress);}
                    }else if("nli".equals(req.optString("mode"))){
                        int count=req.getJSONArray("pairs").length();
                        if(count<1||count>12)throw new java.io.IOException("Limite NLI");
                        progress.report(0,count,"Carregando BERTimbau NLI");
                        try(NliEngine engine=new NliEngine(getApplicationContext(),active)){result=engine.analyze(req,active,progress);}
                    }else{
                        int count=req.getJSONArray("rows").length();
                        progress.report(0,count,"Carregando modelo ONNX");
                        try(NeuralSemanticEngine engine=new NeuralSemanticEngine(getApplicationContext())){
                            if(active.getAsBoolean())engine.setCache(embeddingCache,embeddingCache.open(req.optString("cacheScope",""),NeuralSemanticEngine.MODEL));
                            if(!active.getAsBoolean())throw new java.util.concurrent.CancellationException();
                            result=engine.analyze(req,active,progress);
                        }
                    }
                }catch(Throwable e){
                    // Only a safe category, never patient text or a stack trace in JS/logs.
                    String detail=e instanceof java.util.concurrent.CancellationException?"Análise cancelada":e instanceof OutOfMemoryError?"Memória insuficiente":e instanceof LinkageError?"Runtime neural incompatível":e instanceof ai.onnxruntime.OrtException?"Falha de inferência ONNX":e instanceof java.io.IOException?"Falha de leitura ou entrada do modelo":"Falha interna do motor";
                    try{result=new JSONObject().put("error",detail+"; nenhuma inferência simulada.");}catch(Exception ignored){}
                }finally{semanticJobId="";semanticBusy.set(false);}
                try{result.put("id",id);}catch(Exception ignored){}
                final String response=result.toString();
                main.post(()->{if(unlocked&&semanticEpoch==epoch&&!isDestroyed()&&!isFinishing())web.evaluateJavascript("window.nativeSemanticResult("+JSONObject.quote(response)+")",null);});
            });}catch(java.util.concurrent.RejectedExecutionException e){semanticJobId="";semanticBusy.set(false);return "Motor encerrado";}
            return "OK";
        }

        @JavascriptInterface public void selectAudioFileWithModel(boolean confirmed,String model){
            if(!unlocked||!confirmed){jsCallback("nativeMlState","AUTORIZAÇÃO NÃO CONFIRMADA");return;}
            main.post(()->{
                selectedAsrModel="vosk".equals(model)?"vosk":"tagarela".equals(model)?"tagarela":"base".equals(model)?"base":"small";
                chooseAudioFile();
            });
        }
        @JavascriptInterface public void selectAudioFile(boolean confirmed){
            if(!unlocked||!confirmed){jsCallback("nativeMlState","AUTORIZAÇÃO NÃO CONFIRMADA");return;}
            main.post(()->chooseAudioFile());
        }
        @JavascriptInterface public void startMlCaptureWithEngine(boolean confirmed,String engine){
            if(!unlocked||!confirmed){jsCallback("nativeMlState","AUTORIZAÇÃO NÃO CONFIRMADA");return;}
            main.post(()->{
                selectedMicEngine="tagarela".equals(engine)?"tagarela":"vosk".equals(engine)?"vosk":
                        "whisper-small".equals(engine)?"whisper-small":"whisper-base";
                beginCapture();
            });
        }
        @JavascriptInterface public void startMlCapture(boolean confirmed){
            if(!unlocked||!confirmed){jsCallback("nativeMlState","AUTORIZAÇÃO NÃO CONFIRMADA");return;}
            main.post(()->beginCapture());
        }
        @JavascriptInterface public void stopMlCapture(){if(unlocked)main.post(()->{if(unlocked)ml.stop();});}
        @JavascriptInterface public void pauseMlTranscription(){if(unlocked)main.post(()->{if(unlocked)ml.pauseTranscription();});}
        @JavascriptInterface public void resumeMlTranscription(){if(unlocked)main.post(()->{if(unlocked)ml.resumeTranscription();});}
        @JavascriptInterface public void discardMlAudio(){if(unlocked)main.post(()->{if(unlocked)ml.discardRemainingAudio();});}

        private String activeJobCaseId = "";

        @JavascriptInterface public boolean setJobContext(String jobId, String caseId, String sessionId) {
            if (!unlocked) {
                jsCallback("nativeMlState", "ERRO: aplicativo bloqueado");
                return false;
            }
            if (jobId == null || jobId.trim().isEmpty() || caseId == null || caseId.trim().isEmpty() || sessionId == null || sessionId.trim().isEmpty()) {
                jsCallback("nativeMlState", "ERRO: parâmetros de contexto incompletos");
                return false;
            }
            if (ml.isTranscribing() || ml.isRecording() || ml.isPaused() || ml.hasPendingAudio()) {
                jsCallback("nativeMlState", "ERRO: operação de áudio ativa ou pausada (gravação/transcrição) com outro contexto");
                return false;
            }
            try {
                store.getDbHelper().createJob(jobId, caseId, sessionId, "", null);
                activeJobCaseId = caseId;
                ml.setJobContext(jobId, caseId, sessionId);
                return true;
            } catch (Exception e) {
                Log.e("MainActivity", "Erro ao inicializar contexto do job: " + e.getMessage(), e);
                jsCallback("nativeMlState", "ERRO CONTEXTO: " + e.getMessage());
                return false;
            }
        }

        @Deprecated
        @JavascriptInterface public boolean setJobContext(String jobId, String caseId) {
            Log.w("MainActivity", "setJobContext chamado sem sessionId obrigatório");
            return false;
        }

        @JavascriptInterface public boolean acknowledgePersistence(String jobId) {
            if (!unlocked) return false;
            if (jobId == null || jobId.trim().isEmpty()) return false;
            return ml.acknowledgePersistence(jobId);
        }

        @JavascriptInterface public boolean confirmSpeaker(String segmentId, String caseId, String sessionId, String newSpeaker) {
            if (!unlocked) return false;
            return store.getDbHelper().confirmSpeaker(segmentId, caseId, sessionId, newSpeaker);
        }

        @JavascriptInterface public boolean confirmSpeaker(String segmentId, String newSpeaker) {
            if (!unlocked) return false;
            return false; // require patient and session scope
        }

        @JavascriptInterface public String getEngineInfo() {
            try {
                JSONObject info = new JSONObject();
                info.put("versionName", "0.23.0-arm64-beta");
                info.put("versionCode", 28);
                info.put("abi", "arm64-v8a");
                info.put("micEngine", selectedMicEngine);
                info.put("tagarelaAvailable", true);
                info.put("whisperAvailable", true);
                info.put("voskAvailable", true);
                info.put("semanticModel", "DistilUSE-multilingual-cased-v2-U8U8");
                info.put("semanticAvailable", true);
                info.put("nliAvailable", NliEngine.available(getApplicationContext()));
                info.put("nliModel", NliEngine.MODEL);
                info.put("nliExperimental", true);
                info.put("rerankerAvailable", RerankEngine.available(getApplicationContext()));
                info.put("rerankerModel", RerankEngine.MODEL);
                info.put("llmAvailable", LocalLlmEngine.available(getApplicationContext()));
                info.put("llmModel", LocalLlmEngine.MODEL);
                info.put("databaseType", "SQLite-Transactional-AES-GCM-v5");
                return info.toString();
            } catch (Exception e) {
                return "{\"versionName\":\"0.23.0-arm64-beta\",\"versionCode\":28}";
            }
        }

        @JavascriptInterface public String listRecoverableJobs() {
            if (!unlocked) return "[]";
            try {
                java.util.List<JSONObject> jobs = store.getDbHelper().listRecoverableJobs();
                JSONArray arr = new JSONArray();
                for (JSONObject j : jobs) { j.remove("audio_path"); arr.put(j); }
                return arr.toString();
            } catch (Exception e) {
                return "{\"__loadError\":\"Falha ao consultar gravações pendentes\"}";
            }
        }

        @JavascriptInterface public boolean resumeJob(String jobId) {
            if (!unlocked || ml == null || semanticBusy.get()) return false;
            if (ml.isRecording() || ml.isTranscribing() || ml.isPaused()) return false;
            JSONObject job = store.getDbHelper().getJob(jobId);
            if (job == null) return false;
            activeJobCaseId = job.optString("case_id");
            return ml.resumeJob(jobId);
        }

        @JavascriptInterface public String savePrivateDiary(String caseId, String json) {
            if (!unlocked) return "Bloqueado";
            try { store.savePrivateDiary(caseId, json); return "OK"; }
            catch (Exception e) { android.util.Log.e("ECO", "Falha ao persistir cofre local", e); return "ERRO COFRE LOCAL: " + e.getClass().getSimpleName() + ": " + e.getMessage(); }
        }

        @JavascriptInterface public String loadPrivateDiary(String caseId) {
            if (!unlocked) return "[]";
            try { return store.readPrivateDiary(caseId); }
            catch (Exception e) { return "{\"__loadError\":\"Falha ao ler diário privado\"}"; }
        }

        @JavascriptInterface public String saveCaseDraft(String caseId, String json) {
            if (!unlocked && (!journalWhileLocked || activeJobCaseId.isEmpty() || !activeJobCaseId.equals(caseId))) return "Bloqueado";
            try { store.saveCaseDraft(caseId, json); return "OK"; }
            catch (Exception e) { return "ERRO: " + e.getMessage(); }
        }

        @JavascriptInterface public String loadCaseDraft(String caseId) {
            if (!unlocked) return "{\"__loadError\":\"Bloqueado\"}";
            try { return store.readCaseDraft(caseId); }
            catch (Exception e) { return "{\"__loadError\":\"Falha ao ler rascunho.\"}"; }
        }

        @JavascriptInterface public String loadState() {
            if (!unlocked) return "{\"__loadError\":\"Bloqueado\"}";
            try { return store.read(); }
            catch (Exception e) { return "{\"__loadError\":\"Falha ao abrir dados criptografados. Não sobrescreva o arquivo.\"}"; }
        }
        @JavascriptInterface public String saveState(String json) {
            if (!unlocked) return "Bloqueado";
            try { store.save(json); return "OK"; }
            catch (Exception e) { return "ERRO: " + e.getMessage(); }
        }
        @JavascriptInterface public String saveDraft(String json) {
            if(!unlocked){
                if(!journalWhileLocked)return "Bloqueado";
                try {
                    JSONObject obj = new JSONObject(json);
                    String draftCase = obj.optString("caseId");
                    if(activeJobCaseId.isEmpty() || !activeJobCaseId.equals(draftCase)) return "Bloqueado: caso não autorizado durante bloqueio";
                } catch(Exception ignored){return "Rascunho inválido";}
            }
            try{store.saveDraft(json);return "OK";}
            catch(Exception e){return "ERRO: "+e.getMessage();}
        }
        @JavascriptInterface public String loadDraft() {
            if(!unlocked)return "{\"__loadError\":\"Bloqueado\"}";
            try{return store.readDraft();}
            catch(Exception e){return "{\"__loadError\":\"Rascunho cifrado não pode ser lido. Não desinstale o ECO.\"}";}
        }
        @JavascriptInterface public void clearDraft() {
            if(!unlocked)return;
            store.clearDraft();
        }

        @JavascriptInterface public void exportBackup(String password) {
            exportBackupWithOptions(password, false);
        }
        @JavascriptInterface public void exportBackupWithOptions(String password, boolean includePrivateDiary) {
            if (!unlocked) return;
            try {
                JSONObject export=new JSONObject(store.read());
                JSONArray cases = export.optJSONArray("cases");
                if (cases != null) for (int i=0; i<cases.length(); i++) {
                    JSONObject singleCase=cases.getJSONObject(i);
                    singleCase.remove("diarioNotas");
                    if (includePrivateDiary) singleCase.put("diarioNotas", new JSONArray(store.readPrivateDiary(singleCase.getString("id"))));
                }
                String draft=store.readDraft();
                if(!draft.isEmpty()){
                    JSONObject journal=new JSONObject(draft);
                    if(!journal.has("__loadError"))export.put("mlDraft",journal);
                }
                pendingExport = SecureStore.encryptBackup(export.toString(), password);
            }
            catch (Exception e) { showToast("Erro no backup: " + e.getMessage()); return; }
            main.post(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "tutor-clinico-backup.psicbackup");
                startActivityForResult(intent, EXPORT_CODE);
            });
        }
        @JavascriptInterface public void importBackup(String password) {
            if (!unlocked) return;
            pendingPassword = password;
            main.post(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, IMPORT_CODE);
            });
        }
        @JavascriptInterface public void printReport(String html) {
            if (!unlocked || html == null || html.trim().isEmpty()) return;
            main.post(() -> {
                if (!unlocked || isFinishing() || isDestroyed()) return;
                WebView printView = new WebView(MainActivity.this);
                printView.getSettings().setBlockNetworkLoads(true);
                printView.getSettings().setJavaScriptEnabled(false);
                printView.getSettings().setAllowFileAccess(false);
                printView.getSettings().setAllowContentAccess(false);
                printView.setWebViewClient(new WebViewClient() {
                    private boolean submitted = false;
                    @Override public void onPageFinished(WebView view, String url) {
                        if (submitted) return;
                        submitted = true;
                        PrintManager manager = (PrintManager)getSystemService(Context.PRINT_SERVICE);
                        if (!unlocked || manager == null || isFinishing() || isDestroyed()) { view.destroy(); return; }
                        PrintAttributes attrs = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build();
                        android.print.PrintDocumentAdapter delegate = view.createPrintDocumentAdapter("Documento ECO");
                        try {
                            manager.print("Documento ECO", new android.print.PrintDocumentAdapter() {
                                @Override public void onStart() { delegate.onStart(); }
                                @Override public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs,
                                        android.os.CancellationSignal cancel, LayoutResultCallback callback, android.os.Bundle extras) {
                                    delegate.onLayout(oldAttrs, newAttrs, cancel, callback, extras);
                                }
                                @Override public void onWrite(android.print.PageRange[] pages, android.os.ParcelFileDescriptor destination,
                                        android.os.CancellationSignal cancel, WriteResultCallback callback) {
                                    delegate.onWrite(pages, destination, cancel, callback);
                                }
                                @Override public void onFinish() {
                                    try { delegate.onFinish(); }
                                    finally { main.post(() -> { view.stopLoading(); view.destroy(); }); }
                                }
                            }, attrs);
                        } catch (RuntimeException e) {
                            view.destroy();
                        }
                    }
                });
                printView.loadDataWithBaseURL("https://app.local/", html, "text/html", "UTF-8", null);
            });
        }
    }
    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code,result,data);
        if (result != RESULT_OK || data == null || data.getData() == null) {
            if (code == EXPORT_CODE) pendingExport = null;
            if (code == IMPORT_CODE) pendingPassword = null;
            if (code == AUDIO_FILE_CODE) jsCallback("nativeMlState","IMPORTAÇÃO CANCELADA");
            return;
        }
        Uri uri = data.getData();
        try {
            if (code == AUDIO_FILE_CODE) {
                openAudioSaf(uri);
            } else if (code == EXPORT_CODE && pendingExport != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IllegalStateException("Destino inacessível");
                    out.write(pendingExport); out.flush();
                }
                showToast("Backup criptografado salvo");
            } else if (code == IMPORT_CODE) {
                byte[] bytes;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IllegalStateException("Arquivo inacessível");
                    bytes = readLimited(in, 50_000_000);
                }
                if (bytes.length > 50_000_000) throw new IllegalArgumentException("Arquivo acima do limite de 50 MB");
                String content = SecureStore.decryptBackup(bytes, pendingPassword == null ? "" : pendingPassword);
                jsCallback("nativeImportResult", content);
            }
        } catch (Exception e) {
            android.util.Log.e("ECO", "Falha de operação local: " + code, e);
            showToast((code == IMPORT_CODE ? "Falha ao abrir backup: " : "Operação não concluída: ") + e.getMessage());
            if (code == AUDIO_FILE_CODE) jsCallback("nativeMlState","ERRO ARQUIVO: " + e.getMessage());
        }
        finally { pendingPassword = null; pendingExport = null; }
    }
}
