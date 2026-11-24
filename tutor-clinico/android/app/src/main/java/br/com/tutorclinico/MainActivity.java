package br.com.tutorclinico;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.print.PrintManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.content.res.Configuration;
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
        showingLock = true; journalWhileLocked=initialUnlockDone&&ml!=null&&ml.isTranscribing(); unlocked = false; semanticEpoch++; web.setVisibility(View.INVISIBLE);
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
    @Override protected void onDestroy() { semanticEpoch++; semanticWorker.shutdownNow(); if(ml!=null)ml.destroy(); if (web != null) web.destroy(); super.onDestroy(); }
    private void jsCallback(String fn, String arg) {
        main.post(() -> { if (!isFinishing()) web.evaluateJavascript("window."+fn+"("+JSONObject.quote(arg)+")",null); });
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
        @JavascriptInterface public void analyzeSemantic(String json){
            if(!unlocked||json==null||json.length()>500000)return;
            final JSONObject req;
            try{req=new JSONObject(json);}catch(Exception e){return;}
            final String id=req.optString("id");
            if(!semanticBusy.compareAndSet(false,true)){
                jsCallback("nativeSemanticResult", "{\"id\":"+JSONObject.quote(id)+",\"error\":\"Motor ocupado\"}");return;
            }
            final long epoch=semanticEpoch;
            try{semanticWorker.execute(()->{
                JSONObject result=new JSONObject();
                try(NeuralSemanticEngine engine=new NeuralSemanticEngine(getApplicationContext())){
                    result=engine.analyze(req,()->unlocked&&semanticEpoch==epoch&&!Thread.currentThread().isInterrupted());
                }catch(Exception|LinkageError e){
                    try{result=new JSONObject().put("error","Modelo indisponível ou análise interrompida; nenhuma inferência simulada.");}catch(Exception ignored){}
                }finally{semanticBusy.set(false);}
                try{result.put("id",id);}catch(Exception ignored){}
                if(unlocked&&semanticEpoch==epoch&&!isDestroyed())jsCallback("nativeSemanticResult",result.toString());
            });}catch(java.util.concurrent.RejectedExecutionException e){semanticBusy.set(false);}
        }

        @JavascriptInterface public void selectAudioFileWithModel(boolean confirmed,String model){
            if(!unlocked||!confirmed){jsCallback("nativeMlState","AUTORIZAÇÃO NÃO CONFIRMADA");return;}
            main.post(()->{
                selectedAsrModel="base".equals(model)?"base":"small";
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
                selectedMicEngine="vosk".equals(engine)?"vosk":
                        "whisper-small".equals(engine)?"whisper-small":"whisper-base";
                beginCapture();
            });
        }
        @JavascriptInterface public void startMlCapture(boolean confirmed){
            if(!unlocked||!confirmed){jsCallback("nativeMlState","AUTORIZAÇÃO NÃO CONFIRMADA");return;}
            main.post(()->beginCapture());
        }
        @JavascriptInterface public void stopMlCapture(){main.post(()->ml.stop());}
        @JavascriptInterface public void pauseMlTranscription(){main.post(()->ml.pauseTranscription());}
        @JavascriptInterface public void resumeMlTranscription(){main.post(()->ml.resumeTranscription());}
        @JavascriptInterface public void discardMlAudio(){main.post(()->ml.discardRemainingAudio());}

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
            if(!unlocked&&!journalWhileLocked)return "Bloqueado";
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
            if (!unlocked) return;
            try {
                JSONObject export=new JSONObject(store.read());
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
            if (!unlocked) return;
            main.post(() -> {
                WebView printView = new WebView(MainActivity.this);
                printView.getSettings().setBlockNetworkLoads(true);
                printView.getSettings().setJavaScriptEnabled(false);
                printView.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, String url) {
                        PrintManager manager = (PrintManager)getSystemService(Context.PRINT_SERVICE);
                        manager.print("Relatório clínico", view.createPrintDocumentAdapter("relatorio"), null);
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
                    bytes = readLimited(in, 12_000_000);
                }
                if (bytes.length > 12_000_000) throw new IllegalArgumentException("Arquivo acima do limite de 12 MB");
                String content = SecureStore.decryptBackup(bytes, pendingPassword == null ? "" : pendingPassword);
                jsCallback("nativeImportResult", content);
            }
        } catch (Exception e) {
            showToast("Operação não concluída: " + e.getMessage());
            if (code == AUDIO_FILE_CODE) jsCallback("nativeMlState","ERRO ARQUIVO: " + e.getMessage());
        }
        finally { pendingPassword = null; pendingExport = null; }
    }
}
