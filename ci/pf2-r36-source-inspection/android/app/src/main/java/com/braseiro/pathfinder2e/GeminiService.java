package com.braseiro.pathfinder2e;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GeminiService {
    public interface TextCallback { void onSuccess(String text); void onError(String error); }
    public interface StreamingTextCallback { void onPartial(String text); void onSuccess(String text); void onError(String error); }
    public interface GroundedStreamingTextCallback { void onPartial(String text); void onSuccess(String text, JSONArray citations); void onError(String error); }
    public interface RagProgress { void onProgress(String message); }
    public interface FileCallback { void onSuccess(File file, String mime); void onError(String error); }
    public interface UploadCallback { void onSuccess(String fileUri, String fileName, long expiresAt); void onError(String error); }
    public static final class SpeechRequest {
        private volatile boolean cancelled;
        private volatile HttpURLConnection connection;
        public void cancel(){cancelled=true;HttpURLConnection current=connection;if(current!=null)current.disconnect();}
        public boolean isCancelled(){return cancelled;}
        private void bind(HttpURLConnection value)throws SpeechCancelledException{if(cancelled){if(value!=null)value.disconnect();throw new SpeechCancelledException();}connection=value;}
        private void unbind(HttpURLConnection value){if(connection==value)connection=null;}
        private void requireActive()throws SpeechCancelledException{if(cancelled)throw new SpeechCancelledException();}
    }
    private enum Kind { TEXT, IMAGE, TTS }
    private static final long TTS_PRIMARY_CIRCUIT_MS=0L;
    private static final String PREF_TTS_PRIMARY_CIRCUIT_UNTIL="tts_primary_circuit_until";
    private static final String PREF_LAST_TEXT_ERROR="last_text_error";
    private static final String PREF_LAST_TTS_ERROR="last_tts_error";
    private final Context context;
    private final SharedPreferences settings;

    public GeminiService(Context context){
        this.context=context.getApplicationContext();
        this.settings=this.context.getSharedPreferences("settings",Context.MODE_PRIVATE);
    }

    public void generateText(final String apiKey,final String model,final String system,final String prompt,final TextCallback cb){
        generateTextWithFiles(apiKey,model,system,prompt,new ArrayList<String>(),cb);
    }

    public void generateTextWithFiles(final String apiKey,final String model,final String system,final String prompt,final List<String> fileUris,final TextCallback cb){
        AppExecutors.network().execute(new Runnable(){public void run(){
            try{
                ensureKey(apiKey);
                final JSONObject body=buildTextBody(system,prompt,fileUris);final boolean singleBudgetCall=isSingleBudgetCall(system);
                List<String> candidates=budgetCandidates(resolveCandidates(apiKey,model,Kind.TEXT),singleBudgetCall);
                ArrayList<String> errors=new ArrayList<String>();
                for(int i=0;i<candidates.size();i++){
                    String candidate=candidates.get(i);
                    try{
                        JSONObject response=postWithRetry(candidate,apiKey,body,singleBudgetCall?0:2);
                        String text=extractText(response);
                        if(text.length()==0)throw new Exception(apiError(response,"A IA não retornou texto."));
                        rememberWorkingModel("last_text_model_ok",candidate);
                        settings.edit().remove(PREF_LAST_TEXT_ERROR).apply();
                        cb.onSuccess(text);return;
                    }catch(AuthException e){throw e;
                    }catch(Exception e){errors.add(candidate+": "+shortError(e));}
                }
                throw new Exception("Nenhum modelo de texto disponível respondeu. "+joinErrors(errors));
            }catch(Exception e){String failure=clean(e);settings.edit().putString(PREF_LAST_TEXT_ERROR,failure).apply();cb.onError(failure);}
        }});
    }

    /** Entrega os fragmentos assim que o Gemini os produz. Isso reduz o tempo percebido
     * sem criar uma segunda chamada nem consumir uma segunda resposta. */
    public void generateTextWithFilesStreaming(final String apiKey,final String model,final String system,final String prompt,final List<String> fileUris,final StreamingTextCallback cb){
        AppExecutors.network().execute(new Runnable(){public void run(){
            try{
                ensureKey(apiKey);final JSONObject body=buildTextBody(system,prompt,fileUris);final boolean singleBudgetCall=isSingleBudgetCall(system);
                List<String> candidates=budgetCandidates(resolveCandidates(apiKey,model,Kind.TEXT),singleBudgetCall);ArrayList<String> errors=new ArrayList<String>();
                for(int i=0;i<candidates.size();i++){
                    String candidate=candidates.get(i);
                    try{
                        String text=streamTextWithRetry(candidate,apiKey,body,cb,singleBudgetCall?1:2);
                        if(text.length()==0)throw new Exception("A IA não retornou texto.");
                        rememberWorkingModel("last_text_model_ok",candidate);settings.edit().remove(PREF_LAST_TEXT_ERROR).apply();cb.onSuccess(text);return;
                    }catch(AuthException e){throw e;}catch(Exception e){errors.add(candidate+": "+shortError(e));}
                }
                throw new Exception("Nenhum modelo de texto disponível respondeu. "+joinErrors(errors));
            }catch(Exception e){String failure=clean(e);settings.edit().putString(PREF_LAST_TEXT_ERROR,failure).apply();cb.onError(failure);}
        }});
    }


    private static boolean isSingleBudgetCall(String system){return system!=null&&(system.contains("BRAS_MASTER_ORCHESTRATED")||system.contains("BRAS_SINGLE_CALL"));}
    private static List<String> budgetCandidates(List<String> candidates,boolean single){if(!single||candidates==null||candidates.size()<=3)return candidates;ArrayList<String> failover=new ArrayList<String>();for(int i=0;i<candidates.size()&&failover.size()<3;i++)failover.add(candidates.get(i));return failover;}

    private JSONObject buildTextBody(String system,String prompt,List<String> fileUris)throws Exception{
        JSONObject body=new JSONObject();
        JSONArray contents=new JSONArray(); JSONObject user=new JSONObject(); user.put("role","user");
        JSONArray parts=new JSONArray();
        if(fileUris!=null)for(int i=0;i<fileUris.size();i++){
            String uri=fileUris.get(i);
            if(uri!=null&&uri.length()>0)parts.put(new JSONObject().put("fileData",new JSONObject().put("mimeType","application/pdf").put("fileUri",uri)));
        }
        parts.put(new JSONObject().put("text",prompt)); user.put("parts",parts); contents.put(user); body.put("contents",contents);
        if(system!=null&&system.trim().length()>0){JSONObject si=new JSONObject();si.put("parts",new JSONArray().put(new JSONObject().put("text",system)));body.put("systemInstruction",si);}
        boolean fastNarration=system!=null&&system.contains("BRAS_FAST_NARRATION");boolean economy=system!=null&&system.contains("BRAS_QUOTA_ECONOMY");boolean critical=system!=null&&system.contains("BRAS_QUOTA_CRITICAL");JSONObject config=new JSONObject();config.put("temperature",fastNarration?0.58:0.64);config.put("topP",fastNarration?0.88:0.90);config.put("candidateCount",1);config.put("maxOutputTokens",critical?1800:(economy?2600:(fastNarration?3600:4600)));config.put("responseMimeType","application/json");body.put("generationConfig",config);
        return body;
    }


    /**
     * Mantém uma biblioteca RAG persistente. O conteúdo indexado no File Search Store
     * continua disponível entre cenas; o PDF inteiro não precisa ser reenviado.
     */
    public String ensureFileSearchStore(String apiKey,String existingStore)throws Exception{
        return ensureFileSearchStore(apiKey,existingStore,"Braseiro Pathfinder 2e — Biblioteca persistente do Mestre");
    }

    public String ensureFileSearchStore(String apiKey,String existingStore,String displayName)throws Exception{
        ensureKey(apiKey);
        if(existingStore!=null&&existingStore.startsWith("fileSearchStores/")){
            try{JSONObject current=get("https://generativelanguage.googleapis.com/v1beta/"+existingStore,apiKey);if(current.optString("name").length()>0)return current.optString("name");}catch(Exception ignored){AppLog.ignored("GeminiService","Falha tolerada; a execução continuou.",ignored);}
        }
        String label=displayName==null||displayName.trim().length()==0?"Braseiro Pathfinder 2e — Biblioteca persistente do Mestre":displayName.trim();
        JSONObject body=new JSONObject().put("displayName",label);
        JSONObject created=postRaw("https://generativelanguage.googleapis.com/v1beta/fileSearchStores",apiKey,body,60000);
        String name=created.optString("name");if(!name.startsWith("fileSearchStores/"))throw new Exception("O Gemini não criou a Biblioteca do Mestre.");return name;
    }

    public void deleteFileSearchStore(String apiKey,String storeName)throws Exception{
        if(storeName==null||!storeName.startsWith("fileSearchStores/"))return;
        HttpURLConnection c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/"+storeName+"?force=true").openConnection();
        c.setRequestMethod("DELETE");c.setRequestProperty("x-goog-api-key",apiKey.trim());c.setConnectTimeout(30000);c.setReadTimeout(60000);parseAndDisconnect(c);
    }

    public void deleteFileSearchDocument(String apiKey,String documentName)throws Exception{
        if(documentName==null||!documentName.contains("/documents/"))return;
        HttpURLConnection c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/"+documentName+"?force=true").openConnection();
        c.setRequestMethod("DELETE");c.setRequestProperty("x-goog-api-key",apiKey.trim());c.setConnectTimeout(30000);c.setReadTimeout(60000);parseAndDisconnect(c);
    }

    /**
     * Indexa no File Search Store. Tenta o upload direto e, caso o serviço de ingestão
     * esteja instável, usa automaticamente o fluxo mais resiliente Files API -> importFile.
     */
    public String uploadPdfToFileSearchStore(String apiKey,String storeName,File file,String displayName,JSONObject metadata,RagProgress progress)throws Exception{
        Exception directFailure=null;
        try{return uploadPdfDirectToFileSearchStore(apiKey,storeName,file,displayName,metadata,progress);}
        catch(Exception e){directFailure=e;if(progress!=null)progress.onProgress("Upload direto indisponível; tentando importação resiliente de "+displayName+"...");}
        try{return uploadPdfViaFileServiceAndImport(apiKey,storeName,file,displayName,metadata,progress);}
        catch(Exception fallback){
            throw new Exception(friendlyRagFailure(displayName,directFailure,fallback));
        }
    }

    /** Upload direto para o File Search Store, com metadados que retornam junto das citações. */
    private String uploadPdfDirectToFileSearchStore(String apiKey,String storeName,File file,String displayName,JSONObject metadata,RagProgress progress)throws Exception{
        ensureKey(apiKey);if(storeName==null||!storeName.startsWith("fileSearchStores/"))throw new Exception("Biblioteca RAG ainda não foi criada.");
        if(file==null||!file.exists())throw new Exception("Arquivo PDF não encontrado.");
        if(file.length()>100L*1024L*1024L)throw new Exception("Este PDF ultrapassa o limite de 100 MB da biblioteca RAG.");
        if(progress!=null)progress.onProgress("Iniciando indexação de "+displayName+"...");
        URL startUrl=new URL("https://generativelanguage.googleapis.com/upload/v1beta/"+storeName+":uploadToFileSearchStore");
        HttpURLConnection start=(HttpURLConnection)startUrl.openConnection();start.setRequestMethod("POST");start.setDoOutput(true);
        start.setRequestProperty("x-goog-api-key",apiKey.trim());start.setRequestProperty("X-Goog-Upload-Protocol","resumable");start.setRequestProperty("X-Goog-Upload-Command","start");
        start.setRequestProperty("X-Goog-Upload-Header-Content-Length",String.valueOf(file.length()));start.setRequestProperty("X-Goog-Upload-Header-Content-Type","application/pdf");
        start.setRequestProperty("Content-Type","application/json; charset=UTF-8");start.setConnectTimeout(30000);start.setReadTimeout(60000);
        JSONObject meta=new JSONObject().put("displayName",displayName).put("mimeType","application/pdf");
        JSONArray custom=new JSONArray();if(metadata!=null){java.util.Iterator<String> keys=metadata.keys();while(keys.hasNext()){String k=keys.next();Object value=metadata.opt(k);JSONObject item=new JSONObject().put("key",k);if(value instanceof Number)item.put("numericValue",((Number)value).doubleValue());else item.put("stringValue",String.valueOf(value));custom.put(item);}}
        if(custom.length()>0)meta.put("customMetadata",custom);
        OutputStream mo=start.getOutputStream();mo.write(meta.toString().getBytes("UTF-8"));mo.close();int sc=start.getResponseCode();String startText=read(sc>=200&&sc<300?start.getInputStream():start.getErrorStream());if(sc<200||sc>=300)throw new Exception("Falha ao iniciar a indexação: "+startText);
        String uploadUrl=start.getHeaderField("X-Goog-Upload-URL");start.disconnect();if(uploadUrl==null||uploadUrl.length()==0)throw new Exception("O Gemini não retornou a URL de indexação.");
        if(progress!=null)progress.onProgress("Enviando "+displayName+" para a Biblioteca do Mestre...");
        HttpURLConnection up=(HttpURLConnection)new URL(uploadUrl).openConnection();up.setRequestMethod("POST");up.setDoOutput(true);up.setRequestProperty("X-Goog-Upload-Offset","0");up.setRequestProperty("X-Goog-Upload-Command","upload, finalize");up.setRequestProperty("Content-Type","application/pdf");up.setFixedLengthStreamingMode(file.length());up.setConnectTimeout(30000);up.setReadTimeout(300000);
        FileInputStream fin=new FileInputStream(file);OutputStream uo=up.getOutputStream();byte[] buf=new byte[65536];int n;long sent=0,lastNotice=0;while((n=fin.read(buf))>0){uo.write(buf,0,n);sent+=n;if(progress!=null&&sent-lastNotice>4L*1024L*1024L){lastNotice=sent;progress.onProgress("Enviando "+displayName+": "+Math.min(99,(sent*100/Math.max(1,file.length())))+"%");}}fin.close();uo.close();
        int uc=up.getResponseCode();String ut=read(uc>=200&&uc<300?up.getInputStream():up.getErrorStream());up.disconnect();if(uc<200||uc>=300)throw new Exception("Falha ao enviar o PDF para o RAG: "+ut);JSONObject op=new JSONObject(ut);String opName=op.optString("name");if(opName.length()==0)throw new Exception("A indexação não retornou uma operação válida.");
        for(int tries=0;tries<240;tries++){
            JSONObject state=get("https://generativelanguage.googleapis.com/v1beta/"+opName,apiKey);
            if(state.optBoolean("done",false)){
                JSONObject error=state.optJSONObject("error");if(error!=null)throw new Exception(error.optString("message","O Gemini não conseguiu indexar o PDF."));
                JSONObject response=state.optJSONObject("response");String document=findDocumentName(response);if(document.length()==0)document=findDocumentByDisplayName(apiKey,storeName,displayName);
                if(progress!=null)progress.onProgress(displayName+" pronto para consulta.");return document;
            }
            if(progress!=null&&tries%4==0)progress.onProgress("Extraindo, dividindo e indexando "+displayName+"...");Thread.sleep(3000);
        }
        throw new Exception("A indexação demorou mais que o esperado. Tente novamente sem apagar os outros livros.");
    }

    /** Fluxo de contingência recomendado: envia ao Files API e importa no File Search Store. */
    private String uploadPdfViaFileServiceAndImport(String apiKey,String storeName,File file,String displayName,JSONObject metadata,RagProgress progress)throws Exception{
        ensureKey(apiKey);if(storeName==null||!storeName.startsWith("fileSearchStores/"))throw new Exception("Biblioteca RAG ainda não foi criada.");
        if(file==null||!file.exists())throw new Exception("Arquivo PDF não encontrado.");
        if(file.length()>100L*1024L*1024L)throw new Exception("Este PDF ultrapassa o limite de 100 MB da biblioteca RAG.");
        String fileName="";
        try{
            if(progress!=null)progress.onProgress("Enviando "+displayName+" pelo canal alternativo...");
            JSONObject uploaded=uploadFileServiceSync(apiKey,file,displayName,progress);fileName=uploaded.optString("name");
            if(fileName.length()==0)throw new Exception("O Files API não retornou o nome do arquivo.");
            JSONObject body=new JSONObject().put("fileName",fileName);
            JSONArray custom=metadataArray(metadata);if(custom.length()>0)body.put("customMetadata",custom);
            if(progress!=null)progress.onProgress("Importando "+displayName+" para a Biblioteca do Mestre...");
            JSONObject op=postRaw("https://generativelanguage.googleapis.com/v1beta/"+storeName+":importFile",apiKey,body,60000);
            String opName=op.optString("name");if(opName.length()==0)throw new Exception("A importação não retornou uma operação válida.");
            String document=waitForFileSearchOperation(apiKey,storeName,displayName,opName,progress);
            if(document.length()==0)throw new Exception("O documento foi importado, mas não pôde ser localizado na biblioteca.");
            return document;
        }finally{if(fileName.length()>0)try{deleteFileServiceFile(apiKey,fileName);}catch(Exception ignored){AppLog.ignored("GeminiService","Falha tolerada; a execução continuou.",ignored);}}
    }

    private JSONObject uploadFileServiceSync(String apiKey,File file,String displayName,RagProgress progress)throws Exception{
        URL startUrl=new URL("https://generativelanguage.googleapis.com/upload/v1beta/files");HttpURLConnection start=(HttpURLConnection)startUrl.openConnection();start.setRequestMethod("POST");start.setDoOutput(true);
        start.setRequestProperty("x-goog-api-key",apiKey.trim());start.setRequestProperty("X-Goog-Upload-Protocol","resumable");start.setRequestProperty("X-Goog-Upload-Command","start");
        start.setRequestProperty("X-Goog-Upload-Header-Content-Length",String.valueOf(file.length()));start.setRequestProperty("X-Goog-Upload-Header-Content-Type","application/pdf");start.setRequestProperty("Content-Type","application/json; charset=UTF-8");start.setConnectTimeout(30000);start.setReadTimeout(60000);
        byte[] meta=new JSONObject().put("file",new JSONObject().put("displayName",displayName)).toString().getBytes("UTF-8");OutputStream mo=start.getOutputStream();mo.write(meta);mo.close();int sc=start.getResponseCode();String st=read(sc>=200&&sc<300?start.getInputStream():start.getErrorStream());if(sc<200||sc>=300)throw new Exception("Falha ao iniciar upload alternativo: "+st);
        String uploadUrl=start.getHeaderField("X-Goog-Upload-URL");start.disconnect();if(uploadUrl==null||uploadUrl.length()==0)throw new Exception("O Files API não retornou a URL de upload.");
        HttpURLConnection up=(HttpURLConnection)new URL(uploadUrl).openConnection();up.setRequestMethod("POST");up.setDoOutput(true);up.setRequestProperty("X-Goog-Upload-Offset","0");up.setRequestProperty("X-Goog-Upload-Command","upload, finalize");up.setRequestProperty("Content-Type","application/pdf");up.setFixedLengthStreamingMode(file.length());up.setConnectTimeout(30000);up.setReadTimeout(300000);
        FileInputStream fin=new FileInputStream(file);OutputStream out=up.getOutputStream();byte[] buf=new byte[65536];int n;long sent=0,last=0;while((n=fin.read(buf))>0){out.write(buf,0,n);sent+=n;if(progress!=null&&sent-last>4L*1024L*1024L){last=sent;progress.onProgress("Enviando "+displayName+": "+Math.min(99,(sent*100/Math.max(1,file.length())))+"%");}}fin.close();out.close();
        int uc=up.getResponseCode();String ut=read(uc>=200&&uc<300?up.getInputStream():up.getErrorStream());up.disconnect();if(uc<200||uc>=300)throw new Exception("Falha no upload alternativo: "+ut);JSONObject response=new JSONObject(ut);JSONObject f=response.optJSONObject("file");if(f==null)f=response;String name=f.optString("name");
        if(name.length()==0)throw new Exception("Upload alternativo concluído sem referência do arquivo.");
        for(int tries=0;tries<80;tries++){JSONObject state=getFile(apiKey,name);String value=state.optString("state");if("ACTIVE".equals(value)||value.length()==0)return state;if("FAILED".equals(value))throw new Exception("O Files API não conseguiu processar o PDF.");Thread.sleep(1500);}
        return f;
    }

    private JSONArray metadataArray(JSONObject metadata)throws Exception{JSONArray custom=new JSONArray();if(metadata==null)return custom;java.util.Iterator<String> keys=metadata.keys();while(keys.hasNext()){String k=keys.next();Object value=metadata.opt(k);JSONObject item=new JSONObject().put("key",k);if(value instanceof Number)item.put("numericValue",((Number)value).doubleValue());else item.put("stringValue",String.valueOf(value));custom.put(item);}return custom;}

    private String waitForFileSearchOperation(String apiKey,String storeName,String displayName,String opName,RagProgress progress)throws Exception{
        for(int tries=0;tries<240;tries++){JSONObject state=get("https://generativelanguage.googleapis.com/v1beta/"+opName,apiKey);if(state.optBoolean("done",false)){JSONObject error=state.optJSONObject("error");if(error!=null)throw new Exception(error.optString("message","O Gemini não conseguiu indexar o PDF."));String document=findDocumentName(state.optJSONObject("response"));if(document.length()==0)document=findDocumentByDisplayName(apiKey,storeName,displayName);if(progress!=null)progress.onProgress(displayName+" pronto para consulta.");return document;}if(progress!=null&&tries%4==0)progress.onProgress("Extraindo, dividindo e indexando "+displayName+"...");Thread.sleep(3000);}throw new Exception("A indexação demorou mais que o esperado.");
    }

    private void deleteFileServiceFile(String apiKey,String fileName)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/"+fileName).openConnection();c.setRequestMethod("DELETE");c.setRequestProperty("x-goog-api-key",apiKey.trim());c.setConnectTimeout(20000);c.setReadTimeout(60000);parseAndDisconnect(c);}

    private String findDocumentName(JSONObject json){if(json==null)return "";String name=json.optString("name");if(name.contains("/documents/"))return name;JSONObject document=json.optJSONObject("document");if(document!=null&&document.optString("name").contains("/documents/"))return document.optString("name");java.util.Iterator<String> keys=json.keys();while(keys.hasNext()){Object value=json.opt(keys.next());if(value instanceof JSONObject){String nested=findDocumentName((JSONObject)value);if(nested.length()>0)return nested;}}return "";}
    private String findDocumentByDisplayName(String key,String storeName,String displayName)throws Exception{String page="";for(int loops=0;loops<20;loops++){String endpoint="https://generativelanguage.googleapis.com/v1beta/"+storeName+"/documents?pageSize=20"+(page.length()>0?"&pageToken="+URLEncoder.encode(page,"UTF-8"):"");JSONObject result=get(endpoint,key);JSONArray docs=result.optJSONArray("documents");if(docs!=null)for(int i=0;i<docs.length();i++){JSONObject d=docs.optJSONObject(i);if(d!=null&&displayName.equals(d.optString("displayName")))return d.optString("name");}page=result.optString("nextPageToken");if(page.length()==0)break;}return "";}

    /** Geração de cena com recuperação autônoma dos trechos relevantes e citações estruturadas. */
    public void generateTextWithFileSearchStreaming(final String apiKey,final String model,final String system,final String prompt,final String storeName,final GroundedStreamingTextCallback cb){
        AppExecutors.network().execute(new Runnable(){public void run(){try{
            ensureKey(apiKey);if(storeName==null||!storeName.startsWith("fileSearchStores/"))throw new Exception("Biblioteca RAG indisponível.");
            final JSONObject body=buildFileSearchBody(system,prompt,storeName);List<String> candidates=resolveCandidates(apiKey,model,Kind.TEXT);ArrayList<String> errors=new ArrayList<String>();
            for(int i=0;i<candidates.size();i++){String candidate=candidates.get(i);try{JSONArray citations=new JSONArray();String text=streamGroundedText(candidate,apiKey,body,cb,citations);if(text.length()==0)throw new Exception("A IA não retornou texto.");rememberWorkingModel("last_text_model_ok",candidate);cb.onSuccess(text,citations);return;}catch(AuthException e){throw e;}catch(Exception e){errors.add(candidate+": "+shortError(e));}}
            throw new Exception("Nenhum modelo compatível com a Biblioteca do Mestre respondeu. "+joinErrors(errors));
        }catch(Exception e){cb.onError(clean(e));}}});
    }

    private JSONObject buildFileSearchBody(String system,String prompt,String storeName)throws Exception{
        JSONObject body=new JSONObject();JSONObject user=new JSONObject().put("role","user").put("parts",new JSONArray().put(new JSONObject().put("text",prompt)));body.put("contents",new JSONArray().put(user));
        if(system!=null&&system.trim().length()>0)body.put("systemInstruction",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",system))));
        body.put("tools",new JSONArray().put(new JSONObject().put("fileSearch",new JSONObject().put("fileSearchStoreNames",new JSONArray().put(storeName)))));
        body.put("generationConfig",new JSONObject().put("temperature",0.68).put("topP",0.90).put("maxOutputTokens",6144).put("responseMimeType","application/json"));return body;
    }

    private String streamGroundedText(String model,String key,JSONObject body,GroundedStreamingTextCallback cb,JSONArray citations)throws Exception{
        String endpoint="https://generativelanguage.googleapis.com/v1beta/models/"+normalizeModel(model)+":streamGenerateContent?alt=sse";HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("Accept","text/event-stream");c.setRequestProperty("x-goog-api-key",key.trim());c.setConnectTimeout(15000);c.setReadTimeout(60000);
        try{
            try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));out.flush();}
            int status=c.getResponseCode();if(status<200||status>=300){String error=read(c.getErrorStream());JSONObject parsed;try{parsed=new JSONObject(error);}catch(Exception ignored){parsed=new JSONObject().put("raw",error);}if(status==401||status==403)throw new AuthException(apiError(parsed,error));throw new HttpStatusException(status,"Gemini HTTP "+status+": "+apiError(parsed,error));}
            StringBuilder event=new StringBuilder(),full=new StringBuilder();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"))){String line;while((line=reader.readLine())!=null){if(line.length()==0){if(event.length()>0){appendGroundedEvent(event.toString(),full,cb,citations);event.setLength(0);}continue;}if(line.startsWith("data:")){String data=line.substring(5).trim();if("[DONE]".equals(data))break;if(event.length()>0)event.append('\n');event.append(data);}}}
            if(event.length()>0)appendGroundedEvent(event.toString(),full,cb,citations);return full.toString().trim();
        }finally{c.disconnect();}
    }
    private void appendGroundedEvent(String data,StringBuilder full,GroundedStreamingTextCallback cb,JSONArray citations)throws Exception{if(data==null||data.trim().length()==0)return;JSONObject event=new JSONObject(data);collectGrounding(event,citations);String chunk=extractText(event);if(chunk.length()==0)return;String merged=mergeStreamText(full.toString(),chunk);full.setLength(0);full.append(merged);cb.onPartial(merged);}
    private void collectGrounding(JSONObject event,JSONArray citations)throws Exception{
        JSONArray candidates=event.optJSONArray("candidates");if(candidates==null)return;for(int i=0;i<candidates.length();i++){JSONObject candidate=candidates.optJSONObject(i);JSONObject gm=candidate==null?null:candidate.optJSONObject("groundingMetadata");if(gm==null)continue;JSONArray chunks=gm.optJSONArray("groundingChunks");if(chunks==null)continue;for(int j=0;j<chunks.length();j++){JSONObject rc=chunks.optJSONObject(j)==null?null:chunks.optJSONObject(j).optJSONObject("retrievedContext");if(rc==null)continue;JSONObject citation=new JSONObject().put("book",rc.optString("title","Material consultado")).put("title",rc.optString("title","Material consultado")).put("page",rc.optInt("pageNumber",0)).put("pdf_page",rc.optInt("pageNumber",0)).put("uri",rc.optString("uri")).put("excerpt",compactExcerpt(rc.optString("text"))).put("confirmed",true).put("retrieval","Gemini File Search");JSONArray md=rc.optJSONArray("customMetadata");if(md!=null)for(int k=0;k<md.length();k++){JSONObject item=md.optJSONObject(k);if(item==null)continue;String key=item.optString("key");Object value=item.has("stringValue")?item.optString("stringValue"):item.opt("numericValue");if(key.length()>0&&value!=null)citation.put(key,value);}String dedupe=citation.optString("uri")+"|"+citation.optInt("page")+"|"+citation.optString("excerpt");boolean exists=false;for(int k=0;k<citations.length();k++)if(dedupe.equals(citations.optJSONObject(k).optString("_key")))exists=true;if(!exists){citation.put("_key",dedupe);citations.put(citation);}}}}
    private String compactExcerpt(String text){if(text==null)return "";String clean=text.replaceAll("\\s+"," ").trim();return clean.length()>420?clean.substring(0,417)+"...":clean;}

    public void uploadPdf(final String apiKey,final File file,final String displayName,final UploadCallback cb){
        AppExecutors.network().execute(new Runnable(){public void run(){
            try{
                ensureKey(apiKey);if(file==null||!file.exists())throw new Exception("Arquivo PDF não encontrado.");if(file.length()>512L*1024L*1024L)throw new Exception("PDF maior que 512 MB.");
                URL startUrl=new URL("https://generativelanguage.googleapis.com/upload/v1beta/files");HttpURLConnection start=(HttpURLConnection)startUrl.openConnection();start.setRequestMethod("POST");start.setDoOutput(true);start.setRequestProperty("x-goog-api-key",apiKey.trim());start.setRequestProperty("X-Goog-Upload-Protocol","resumable");start.setRequestProperty("X-Goog-Upload-Command","start");start.setRequestProperty("X-Goog-Upload-Header-Content-Length",String.valueOf(file.length()));start.setRequestProperty("X-Goog-Upload-Header-Content-Type","application/pdf");start.setRequestProperty("Content-Type","application/json; charset=UTF-8");start.setConnectTimeout(30000);start.setReadTimeout(60000);
                byte[] meta=new JSONObject().put("file",new JSONObject().put("displayName",displayName)).toString().getBytes("UTF-8");OutputStream mo=start.getOutputStream();mo.write(meta);mo.close();int sc=start.getResponseCode();String startText=read(sc>=200&&sc<300?start.getInputStream():start.getErrorStream());if(sc<200||sc>=300)throw new Exception("Falha ao iniciar upload: "+startText);String uploadUrl=start.getHeaderField("X-Goog-Upload-URL");start.disconnect();if(uploadUrl==null||uploadUrl.length()==0)throw new Exception("Gemini não retornou a URL de upload.");
                HttpURLConnection up=(HttpURLConnection)new URL(uploadUrl).openConnection();up.setRequestMethod("POST");up.setDoOutput(true);up.setRequestProperty("X-Goog-Upload-Offset","0");up.setRequestProperty("X-Goog-Upload-Command","upload, finalize");up.setRequestProperty("Content-Type","application/pdf");up.setFixedLengthStreamingMode(file.length());up.setConnectTimeout(30000);up.setReadTimeout(300000);FileInputStream fin=new FileInputStream(file);OutputStream uo=up.getOutputStream();byte[] buf=new byte[65536];int n;while((n=fin.read(buf))>0)uo.write(buf,0,n);fin.close();uo.close();int uc=up.getResponseCode();String ut=read(uc>=200&&uc<300?up.getInputStream():up.getErrorStream());up.disconnect();if(uc<200||uc>=300)throw new Exception("Falha no upload do PDF: "+ut);JSONObject uj=new JSONObject(ut);JSONObject fo=uj.optJSONObject("file");if(fo==null)fo=uj;String name=fo.optString("name"),uri=fo.optString("uri");if(name.length()==0||uri.length()==0)throw new Exception("Upload concluído sem referência do arquivo.");
                for(int tries=0;tries<40;tries++){JSONObject state=getFile(apiKey,name);String st=state.optString("state");if("ACTIVE".equals(st)||st.length()==0){uri=state.optString("uri",uri);cb.onSuccess(uri,name,System.currentTimeMillis()+47L*60L*60L*1000L);return;}if("FAILED".equals(st))throw new Exception("O Gemini não conseguiu processar o PDF.");Thread.sleep(3000);}
                cb.onSuccess(uri,name,System.currentTimeMillis()+46L*60L*60L*1000L);
            }catch(Exception e){cb.onError(clean(e));}
        }});
    }

    public void generateImage(final String apiKey,final String model,final String prompt,final FileCallback cb){
        AppExecutors.network().execute(new Runnable(){public void run(){
            try{
                ensureKey(apiKey);
                JSONObject body=new JSONObject(); JSONArray parts=new JSONArray();parts.put(new JSONObject().put("text",prompt));
                JSONObject content=new JSONObject();content.put("role","user");content.put("parts",parts);body.put("contents",new JSONArray().put(content));
                JSONObject cfg=new JSONObject();cfg.put("responseModalities",new JSONArray().put("TEXT").put("IMAGE"));body.put("generationConfig",cfg);
                List<String> candidates=resolveCandidates(apiKey,model,Kind.IMAGE);ArrayList<String> errors=new ArrayList<String>();
                for(int m=0;m<candidates.size();m++){
                    String candidate=candidates.get(m);
                    try{
                        JSONObject response=postWithRetry(candidate,apiKey,body,1);
                        JSONArray c=response.optJSONArray("candidates");
                        if(c!=null)for(int i=0;i<c.length();i++){
                            JSONObject cont=c.getJSONObject(i).optJSONObject("content");JSONArray ps=cont==null?null:cont.optJSONArray("parts");if(ps==null)continue;
                            for(int j=0;j<ps.length();j++){JSONObject p=ps.getJSONObject(j);JSONObject inline=p.optJSONObject("inlineData");if(inline==null)inline=p.optJSONObject("inline_data");if(inline!=null&&inline.optString("data").length()>0){String mime=inline.optString("mimeType",inline.optString("mime_type","image/png"));byte[] bytes=Base64.decode(inline.optString("data"),Base64.DEFAULT);if(bytes.length>ImageStorageManager.MAX_INPUT_BYTES)throw new Exception("Imagem gerada acima do limite seguro de 24 MB.");String ext=mime.contains("jpeg")?"jpg":mime.contains("webp")?"webp":"png";File temp=new File(context.getCacheDir(),"scene_generated_"+System.currentTimeMillis()+"."+ext);FileOutputStream generated=new FileOutputStream(temp);generated.write(bytes);generated.flush();generated.getFD().sync();generated.close();Uri durable=ImageStorageManager.importImage(context,Uri.fromFile(temp),"scene_images","generated_scene",12);if(!temp.delete())AppLog.w("GeminiImage","Falha ao remover imagem temporária.");File f=new File(durable.getPath());rememberWorkingModel("last_image_model_ok",candidate);cb.onSuccess(f,mime);return;}}
                        }
                        throw new Exception(apiError(response,"O modelo não retornou uma imagem."));
                    }catch(AuthException e){throw e;}catch(Exception e){errors.add(candidate+": "+shortError(e));}
                }
                throw new Exception("Nenhum modelo de imagem disponível respondeu. "+joinErrors(errors));
            }catch(Exception e){cb.onError(clean(e));}
        }});
    }

    public SpeechRequest synthesizeSpeech(final String apiKey,final String model,final String voice,final String text,final FileCallback cb){
        return synthesizeSpeech(apiKey,"",model,voice,text,new JSONObject(),cb);
    }

    public SpeechRequest synthesizeSpeech(final String primaryApiKey,final String fallbackApiKey,final String model,final String voice,final String text,final FileCallback cb){
        return synthesizeSpeech(primaryApiKey,fallbackApiKey,model,voice,text,new JSONObject(),cb);
    }

    /**
     * TTS com direção de performance explícita, chave principal e alternativa.
     * A principal recebe uma repetição apenas para falhas transitórias; 429 não
     * é repetido em rajada e passa imediatamente ao próximo modelo compatível.
     */
    public SpeechRequest synthesizeSpeech(final String primaryApiKey,final String fallbackApiKey,final String model,final String voice,final String text,final JSONObject direction,final FileCallback cb){
        final SpeechRequest request=new SpeechRequest();
        AppExecutors.network().execute(new Runnable(){public void run(){
            try{
                request.requireActive();
                String primary=primaryApiKey==null?"":primaryApiKey.trim();
                String fallback=fallbackApiKey==null?"":fallbackApiKey.trim();
                if(primary.length()<20&&fallback.length()<20)throw new Exception("Configure ao menos uma chave da API Gemini em Configurações.");
                String ttsPrompt=NarrationPlaybackPolicy.charonPrompt(text,direction);
                JSONObject body=new JSONObject();JSONObject content=new JSONObject();content.put("role","user");content.put("parts",new JSONArray().put(new JSONObject().put("text",ttsPrompt)));body.put("contents",new JSONArray().put(content));
                JSONObject prebuilt=new JSONObject().put("voiceName",voice==null||voice.length()==0?"Charon":voice);
                JSONObject voiceConfig=new JSONObject().put("prebuiltVoiceConfig",prebuilt);
                JSONObject speechConfig=new JSONObject().put("voiceConfig",voiceConfig);
                JSONObject cfg=new JSONObject().put("responseModalities",new JSONArray().put("AUDIO")).put("speechConfig",speechConfig);body.put("generationConfig",cfg);
                long now=System.currentTimeMillis();long circuitUntil=settings.getLong(PREF_TTS_PRIMARY_CIRCUIT_UNTIL,0L);
                ArrayList<String> keys=new ArrayList<String>();ArrayList<String> labels=new ArrayList<String>();
                if(primary.length()>=20&&now>=circuitUntil){keys.add(primary);labels.add("principal");}
                if(fallback.length()>=20&&!fallback.equals(primary)){keys.add(fallback);labels.add("alternativa");}
                if(keys.isEmpty()){
                    if(primary.length()>=20&&fallback.length()<20)throw new Exception("A chave principal atingiu o limite e está em espera por alguns minutos. A voz Android continua disponível.");
                    throw new Exception("Nenhuma chave TTS disponível neste momento.");
                }
                ArrayList<String> errors=new ArrayList<String>();
                for(int keyIndex=0;keyIndex<keys.size();keyIndex++){
                    String selectedKey=keys.get(keyIndex),keyLabel=labels.get(keyIndex);
                    List<String> candidates=resolveCandidates(selectedKey,model,Kind.TTS);
                    boolean quotaSeenOnKey=false;
                    for(int i=0;i<candidates.size();i++){
                        request.requireActive();
                        String candidate=candidates.get(i);
                        try{
                            // 2.5 e 3.1 usam o mesmo generateContent já validado pelo Android.
                            // Evitamos misturar dois transportes (generateContent/Interactions) dentro da
                            // mesma sessão de voz, o que dificultava diagnosticar limite e formato de áudio.
                            JSONObject response=postTtsWithRetry(candidate,selectedKey,body,1,request);
                            AudioData ad=extractAudio(response);
                            if(ad==null)throw new Exception(apiError(response,"O modelo não retornou áudio."));
                            if(audioLooksPrematurelyTruncated(ad,GeminiTtsMarkupDirector.stripTags(text)))throw new Exception("O Gemini TTS devolveu áudio curto demais para esta transcrição; tentando outro modelo compatível.");
                            request.requireActive();
                            File wav=new File(context.getCacheDir(),"tts_"+System.currentTimeMillis()+".wav");writeWav(wav,ad.bytes,ad.sampleRate);rememberWorkingModel("last_tts_model_ok",candidate);
                            settings.edit().remove(PREF_LAST_TTS_ERROR).apply();
                            if(request.isCancelled()){if(!wav.delete())AppLog.w("GeminiTTS","Falha ao remover áudio de uma solicitação cancelada.");return;}
                            if("principal".equals(keyLabel))settings.edit().remove(PREF_TTS_PRIMARY_CIRCUIT_UNTIL).apply();
                            cb.onSuccess(wav,"audio/wav");return;
                        }catch(AuthException e){
                            // 401 significa chave inválida: outro modelo não conserta a mesma chave.
                            errors.add(keyLabel+"/"+candidate+": "+shortError(e));break;
                        }catch(Exception e){
                            // 403/429 podem ser específicos do modelo (preview sem acesso, cota zero,
                            // limite por modelo). Tente o próximo TTS com a MESMA chave antes de
                            // abandonar para outra chave ou para a voz Android.
                            errors.add(keyLabel+"/"+candidate+": "+shortError(e));
                            if(isQuotaFailure(e))quotaSeenOnKey=true;
                        }
                    }
                    if("principal".equals(keyLabel)&&quotaSeenOnKey&&TTS_PRIMARY_CIRCUIT_MS>0L){
                        settings.edit().putLong(PREF_TTS_PRIMARY_CIRCUIT_UNTIL,System.currentTimeMillis()+TTS_PRIMARY_CIRCUIT_MS).commit();
                    }
                }
                throw new Exception("Nenhum modelo TTS disponível respondeu. "+joinErrors(errors));
            }catch(Exception e){if(!request.isCancelled()&&!(e instanceof SpeechCancelledException)){String failure=clean(e);settings.edit().putString(PREF_LAST_TTS_ERROR,failure).apply();cb.onError(failure);}}
        }});
        return request;
    }


    /** Gemini 3.1 TTS é fallback automático quando o 2.5 sofre limite específico do modelo. */
    private boolean isInteractionsTtsModel(String model){
        String value=normalizeModel(model).toLowerCase(Locale.US);
        return value.contains("gemini-3.1")&&value.contains("tts");
    }

    private AudioData synthesizeWithInteractions(String model,String key,String ttsPrompt,String voice,SpeechRequest request)throws Exception{
        JSONObject body=new JSONObject();
        body.put("model",normalizeModel(model));
        body.put("input",ttsPrompt);
        body.put("response_format",new JSONObject().put("type","audio"));
        String selectedVoice=voice==null||voice.trim().length()==0?"Charon":voice.trim();
        JSONObject speech=new JSONObject().put("voice",selectedVoice);
        body.put("generation_config",new JSONObject().put("speech_config",new JSONArray().put(speech)));
        JSONObject response=postInteractionsWithRetry(key,body,NarrationPlaybackPolicy.CHARON_RECOVERABLE_RETRIES,request);
        AudioData audio=extractInteractionAudio(response);
        if(audio==null||audio.bytes==null||audio.bytes.length==0)throw new Exception(apiError(response,"O Gemini 3.1 TTS não retornou áudio PCM."));
        return audio;
    }

    private JSONObject postInteractionsWithRetry(String key,JSONObject body,int retries,SpeechRequest request)throws Exception{
        int attempts=0;
        while(true){
            if(request!=null)request.requireActive();
            try{return postInteraction(key,body,request);}catch(HttpStatusException e){
                if(e.status==401)throw new AuthException(e.getMessage());
                boolean recoverable=e.status==429||e.status==500||e.status==502||e.status==503||e.status==504;
                if(recoverable&&attempts<retries){attempts++;sleepRetry(900L+1000L*attempts,request);continue;}
                throw e;
            }catch(java.net.SocketTimeoutException e){
                if(request!=null)request.requireActive();if(attempts<retries){attempts++;sleepRetry(900L+1000L*attempts,request);continue;}throw e;
            }catch(java.io.IOException e){
                if(request!=null)request.requireActive();if(attempts<retries){attempts++;sleepRetry(900L+1000L*attempts,request);continue;}throw e;
            }
        }
    }

    private JSONObject postInteraction(String key,JSONObject body,SpeechRequest request)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/interactions").openConnection();
        if(request!=null)request.bind(c);
        c.setRequestMethod("POST");c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("x-goog-api-key",key.trim());
        c.setRequestProperty("Api-Revision","2026-05-20");
        c.setConnectTimeout(NarrationPlaybackPolicy.CHARON_CONNECT_TIMEOUT_MS);c.setReadTimeout(NarrationPlaybackPolicy.CHARON_READ_TIMEOUT_MS);
        try{
            try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));out.flush();}
            if(request!=null)request.requireActive();
            return parseResponse(c);
        }finally{c.disconnect();if(request!=null)request.unbind(c);}
    }

    private AudioData extractInteractionAudio(JSONObject response){
        if(response==null)return null;
        JSONObject direct=response.optJSONObject("output_audio");if(direct==null)direct=response.optJSONObject("outputAudio");
        AudioData data=audioFromObject(direct);if(data!=null)return data;
        return findAudioRecursively(response,0);
    }

    private AudioData findAudioRecursively(Object value,int depth){
        if(value==null||depth>10)return null;
        if(value instanceof JSONObject){
            JSONObject object=(JSONObject)value;AudioData direct=audioFromObject(object);if(direct!=null)return direct;
            java.util.Iterator<String> keys=object.keys();
            while(keys.hasNext()){String key=keys.next();AudioData found=findAudioRecursively(object.opt(key),depth+1);if(found!=null)return found;}
        }else if(value instanceof JSONArray){
            JSONArray array=(JSONArray)value;for(int i=0;i<array.length();i++){AudioData found=findAudioRecursively(array.opt(i),depth+1);if(found!=null)return found;}
        }
        return null;
    }

    private AudioData audioFromObject(JSONObject object){
        if(object==null)return null;
        String encoded=object.optString("data","");
        if(encoded.length()==0){JSONObject audio=object.optJSONObject("audio");if(audio!=null)return audioFromObject(audio);return null;}
        String type=object.optString("type",object.optString("mime_type",object.optString("mimeType",""))).toLowerCase(Locale.US);
        boolean audioLike=type.length()==0||type.contains("audio")||object.has("sample_rate")||object.has("sampleRate");
        if(!audioLike)return null;
        try{
            int rate=object.optInt("sample_rate",object.optInt("sampleRate",24000));
            String mime=object.optString("mime_type",object.optString("mimeType",""));
            int pos=mime.indexOf("rate=");if(pos>=0){String raw=mime.substring(pos+5).replaceAll("[^0-9].*$","");try{rate=Integer.parseInt(raw);}catch(Exception ignored){AppLog.ignored("GeminiService","Taxa TTS inválida; usando 24 kHz.",ignored);}}
            return new AudioData(Base64.decode(encoded,Base64.DEFAULT),rate<=0?24000:rate);
        }catch(Exception error){AppLog.w("GeminiTTS","Áudio Interactions inválido.",error);return null;}
    }

    private static boolean isQuotaFailure(Exception error){
        if(error instanceof HttpStatusException&&((HttpStatusException)error).status==429)return true;
        String value=clean(error).toLowerCase(Locale.ROOT);
        return value.contains("429")||value.contains("quota")||value.contains("resource_exhausted")||value.contains("rate limit");
    }

    public List<String> listAvailableTextModels(String apiKey)throws Exception{return resolveCandidates(apiKey,"",Kind.TEXT);}
    public String getLastWorkingTextModel(){return settings.getString("last_text_model_ok","");}
    public String getLastWorkingImageModel(){return settings.getString("last_image_model_ok","");}
    public String getLastWorkingTtsModel(){return settings.getString("last_tts_model_ok","");}
    public String getLastTextError(){return settings.getString(PREF_LAST_TEXT_ERROR,"");}
    public String getLastTtsError(){return settings.getString(PREF_LAST_TTS_ERROR,"");}
    public long ttsPrimaryCircuitRemainingMs(){return Math.max(0L,settings.getLong(PREF_TTS_PRIMARY_CIRCUIT_UNTIL,0L)-System.currentTimeMillis());}
    public void resetTtsPrimaryCircuit(){settings.edit().remove(PREF_TTS_PRIMARY_CIRCUIT_UNTIL).commit();}

    private List<String> resolveCandidates(String apiKey,String configured,Kind kind)throws Exception{
        LinkedHashSet<String> ordered=new LinkedHashSet<String>();
        String remembered=settings.getString(kind==Kind.TEXT?"last_text_model_ok":kind==Kind.IMAGE?"last_image_model_ok":"last_tts_model_ok","");
        String rememberedClean=normalizeModel(remembered),configuredClean=normalizeModel(configured);
        if(kind==Kind.TTS){rememberedClean=canonicalTtsModel(rememberedClean);configuredClean=canonicalTtsModel(configuredClean);}
        if(kind==Kind.TEXT){
            for(String candidate:Pf2GeminiResiliencePolicy.textCandidates(configuredClean,rememberedClean))
                if(!isLegacyModel(candidate))ordered.add(candidate);
        } else if(kind==Kind.TTS){
            // Depois que um fallback funcionou, mantenha-o na frente nos blocos seguintes da cena.
            // Isso evita bater novamente no mesmo preview com 429 a cada pedaço de narração.
            boolean default25="gemini-2.5-flash-preview-tts".equalsIgnoreCase(configuredClean);
            if(default25&&rememberedClean.length()>0)ordered.add(rememberedClean);
            if(configuredClean.length()>0)ordered.add(configuredClean);
            if(!default25&&rememberedClean.length()>0)ordered.add(rememberedClean);
            ordered.add("gemini-2.5-flash-preview-tts");
            ordered.add("gemini-3.1-flash-tts-preview");
        } else {
            if(configuredClean.length()>0&&!isLegacyModel(configuredClean))ordered.add(configuredClean);
            if(rememberedClean.length()>0&&!isLegacyModel(rememberedClean))ordered.add(rememberedClean);
            ordered.add("gemini-3.1-flash-image");ordered.add("gemini-3-pro-image");
        }

        // Não chama models.list em toda cena. Essa consulta extra era feita antes de cada
        // geração e aumentava sensivelmente o tempo de resposta. Os candidatos são tentados
        // diretamente; o último modelo que funcionou permanece em primeiro lugar.
        ArrayList<String> result=new ArrayList<String>();result.addAll(ordered);
        if(result.isEmpty())throw new Exception("Nenhum modelo compatível foi configurado para "+kind.name().toLowerCase(Locale.US)+".");
        return result;
    }


    private static String canonicalTtsModel(String model){
        String value=normalizeModel(model);
        // Mantém o ID escolhido. O 3.1 preview é um fallback compatível do mesmo
        // generateContent; aliases antigos são apenas normalizados.
        if(value.length()==0)return "gemini-2.5-flash-preview-tts";
        if("gemini-3.1-flash-tts".equalsIgnoreCase(value))return "gemini-3.1-flash-tts-preview";
        return value;
    }

    private List<String> listModels(String key,Kind kind)throws Exception{
        ArrayList<String> out=new ArrayList<String>();String page="";
        do{
            String endpoint="https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000"+(page.length()>0?"&pageToken="+URLEncoder.encode(page,"UTF-8"):"");
            JSONObject json=get(endpoint,key);JSONArray models=json.optJSONArray("models");
            if(models!=null)for(int i=0;i<models.length();i++){
                JSONObject m=models.optJSONObject(i);if(m==null)continue;String name=normalizeModel(m.optString("name"));if(name.length()==0)continue;
                JSONArray methods=m.optJSONArray("supportedGenerationMethods");boolean gen=false;if(methods!=null)for(int j=0;j<methods.length();j++)if("generateContent".equals(methods.optString(j)))gen=true;if(!gen)continue;
                String low=name.toLowerCase(Locale.US);
                boolean match;
                if(kind==Kind.IMAGE)match=low.contains("image")&&!low.contains("embedding");
                else if(kind==Kind.TTS)match=low.contains("tts");
                else match=!low.contains("image")&&!low.contains("tts")&&!low.contains("live")&&!low.contains("embedding")&&!low.contains("veo")&&!low.contains("imagen")&&(low.contains("flash")||low.contains("pro"));
                if(match&&!out.contains(name))out.add(name);
            }
            page=json.optString("nextPageToken");
        }while(page.length()>0);
        return out;
    }

    /** Retries transient streaming failures only while no visible text has been emitted. */
    private String streamTextWithRetry(String model,String key,JSONObject body,final StreamingTextCallback cb,int retries)throws Exception{
        int attempts=0;
        while(true){
            final int[] emitted={0};
            StreamingTextCallback tracking=new StreamingTextCallback(){
                public void onPartial(String text){emitted[0]=Math.max(emitted[0],text==null?0:text.length());cb.onPartial(text);}
                public void onSuccess(String text){}
                public void onError(String error){}
            };
            try{return streamText(model,key,body,tracking);}catch(Exception e){
                if(emitted[0]>0)throw e;
                if(!isTransientTextFailure(e)||attempts>=retries)throw e;
                attempts++;sleepRetry(700L+(attempts*900L),null);
            }
        }
    }

    private static boolean isTransientTextFailure(Exception e){
        if(e instanceof HttpStatusException){int s=((HttpStatusException)e).status;return s==429||s==500||s==502||s==503||s==504;}
        return e instanceof java.net.SocketTimeoutException||e instanceof java.io.IOException;
    }

    private String streamText(String model,String key,JSONObject body,StreamingTextCallback cb)throws Exception{
        String endpoint="https://generativelanguage.googleapis.com/v1beta/models/"+normalizeModel(model)+":streamGenerateContent?alt=sse";
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("Accept","text/event-stream");c.setRequestProperty("x-goog-api-key",key.trim());c.setConnectTimeout(15000);c.setReadTimeout(90000);
        try{
            try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));out.flush();}
            int status=c.getResponseCode();if(status<200||status>=300){String error=read(c.getErrorStream());JSONObject parsed;try{parsed=new JSONObject(error);}catch(Exception ignored){parsed=new JSONObject().put("raw",error);}if(status==401||status==403)throw new AuthException(apiError(parsed,error));throw new HttpStatusException(status,"Gemini HTTP "+status+": "+apiError(parsed,error));}
            StringBuilder event=new StringBuilder(),full=new StringBuilder();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"))){String line;
                while((line=reader.readLine())!=null){
                    if(line.length()==0){if(event.length()>0){appendStreamEvent(event.toString(),full,cb);event.setLength(0);}continue;}
                    if(line.startsWith("data:")){String data=line.substring(5).trim();if("[DONE]".equals(data))break;if(event.length()>0)event.append('\n');event.append(data);}
                }
            }
            if(event.length()>0)appendStreamEvent(event.toString(),full,cb);return full.toString().trim();
        }finally{c.disconnect();}
    }

    private void appendStreamEvent(String data,StringBuilder full,StreamingTextCallback cb)throws Exception{
        if(data==null||data.trim().length()==0)return;JSONObject event=new JSONObject(data);String chunk=extractText(event);if(chunk.length()==0)return;String merged=mergeStreamText(full.toString(),chunk);full.setLength(0);full.append(merged);cb.onPartial(merged);
    }

    /** Alguns modelos enviam fragmentos incrementais; outros podem repetir o prefixo. */
    private static String mergeStreamText(String current,String chunk){
        return PortugueseNarrationSanitizer.mergeStreamingText(current,chunk);
    }

    /** Retry TTS sem repetir 429: quota/rate-limit deve cair imediatamente no próximo modelo. */
    private JSONObject postTtsWithRetry(String model,String key,JSONObject body,int retries,SpeechRequest request)throws Exception{
        int attempts=0;
        while(true){
            if(request!=null)request.requireActive();
            try{return post(modelEndpoint(model),key,body,request);}catch(HttpStatusException e){
                if(e.status==401)throw new AuthException(e.getMessage());
                if(e.status==429)throw e;
                boolean recoverable=e.status==500||e.status==502||e.status==503||e.status==504;
                if(recoverable&&attempts<retries){attempts++;sleepRetry(850L+900L*attempts,request);continue;}
                throw e;
            }catch(java.net.SocketTimeoutException e){
                if(request!=null)request.requireActive();if(attempts<retries){attempts++;sleepRetry(850L+900L*attempts,request);continue;}throw e;
            }catch(java.io.IOException e){
                if(request!=null)request.requireActive();if(attempts<retries){attempts++;sleepRetry(850L+900L*attempts,request);continue;}throw e;
            }
        }
    }

    private JSONObject postWithRetry(String model,String key,JSONObject body,int retries)throws Exception{
        return postWithRetry(model,key,body,retries,null);
    }

    private JSONObject postWithRetry(String model,String key,JSONObject body,int retries,SpeechRequest request)throws Exception{
        int attempts=0;
        while(true){
            if(request!=null)request.requireActive();
            try{return post(modelEndpoint(model),key,body,request);}catch(HttpStatusException e){
                if(e.status==401)throw new AuthException(e.getMessage());
                boolean recoverable=e.status==429||e.status==500||e.status==502||e.status==503||e.status==504;
                if(recoverable&&attempts<retries){attempts++;sleepRetry(650L+900L*attempts,request);continue;}
                throw e;
            }catch(java.net.SocketTimeoutException e){
                if(request!=null)request.requireActive();if(attempts<retries){attempts++;sleepRetry(650L+900L*attempts,request);continue;}throw e;
            }catch(java.io.IOException e){
                if(request!=null)request.requireActive();if(attempts<retries){attempts++;sleepRetry(650L+900L*attempts,request);continue;}throw e;
            }
        }
    }

    private JSONObject getFile(String key,String name)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/"+name).openConnection();c.setRequestMethod("GET");c.setRequestProperty("x-goog-api-key",key.trim());c.setConnectTimeout(20000);c.setReadTimeout(60000);return parseAndDisconnect(c);}
    private JSONObject get(String endpoint,String key)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("GET");c.setRequestProperty("x-goog-api-key",key.trim());c.setRequestProperty("Accept","application/json");c.setConnectTimeout(20000);c.setReadTimeout(60000);return parseAndDisconnect(c);}

    private String modelEndpoint(String model){String m=normalizeModel(model);if(m.length()==0)m="gemini-3.5-flash";return "https://generativelanguage.googleapis.com/v1beta/models/"+m+":generateContent";}
    private static String normalizeModel(String model){String m=model==null?"":model.trim();if(m.startsWith("models/"))m=m.substring(7);return m;}
    private boolean isLegacyModel(String model){String m=model==null?"":model.toLowerCase(Locale.US);return m.startsWith("gemini-2.0")||m.startsWith("gemini-2.5")||m.contains("preview-05-")||m.contains("preview-09-");}

    private JSONObject postRaw(String endpoint,String key,JSONObject body,int readTimeout)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("x-goog-api-key",key.trim());c.setConnectTimeout(30000);c.setReadTimeout(readTimeout);try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}return parseAndDisconnect(c);}

    private JSONObject post(String endpoint,String key,JSONObject body)throws Exception{
        return post(endpoint,key,body,null);
    }

    private JSONObject post(String endpoint,String key,JSONObject body,SpeechRequest request)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("x-goog-api-key",key.trim());c.setConnectTimeout(15000);c.setReadTimeout(90000);
        if(request!=null){request.bind(c);c.setConnectTimeout(NarrationPlaybackPolicy.CHARON_CONNECT_TIMEOUT_MS);c.setReadTimeout(NarrationPlaybackPolicy.CHARON_READ_TIMEOUT_MS);}
        try{
            try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));out.flush();}
            if(request!=null)request.requireActive();
            return parseResponse(c);
        }finally{c.disconnect();if(request!=null)request.unbind(c);}
    }

    private static void sleepRetry(long delay,SpeechRequest request)throws Exception{
        long remaining=Math.max(0L,delay);
        while(remaining>0L){
            if(request!=null)request.requireActive();
            long step=Math.min(200L,remaining);Thread.sleep(step);remaining-=step;
        }
    }

    private JSONObject parseAndDisconnect(HttpURLConnection c)throws Exception{try{return parseResponse(c);}finally{if(c!=null)c.disconnect();}}

    private JSONObject parseResponse(HttpURLConnection c)throws Exception{
        int status=c.getResponseCode();String text=read(status>=200&&status<300?c.getInputStream():c.getErrorStream());JSONObject result;try{result=new JSONObject(text);}catch(Exception e){result=new JSONObject().put("raw",text);}if(status<200||status>=300)throw new HttpStatusException(status,"Gemini HTTP "+status+": "+apiError(result,text));return result;
    }

    /**
     * Preserva exatamente os espaços enviados nos deltas do streaming.
     *
     * Remover o espaço inicial de cada fragmento fazia "a floresta" chegar como
     * "afloresta" quando o servidor dividia a resposta entre as duas palavras.
     * O resultado completo é aparado apenas ao encerrar a resposta.
     */
    private String extractText(JSONObject response){StringBuilder b=new StringBuilder();JSONArray c=response.optJSONArray("candidates");if(c==null)return "";for(int i=0;i<c.length();i++){JSONObject content=c.optJSONObject(i)==null?null:c.optJSONObject(i).optJSONObject("content");JSONArray parts=content==null?null:content.optJSONArray("parts");if(parts==null)continue;for(int j=0;j<parts.length();j++){String t=parts.optJSONObject(j)==null?"":parts.optJSONObject(j).optString("text");if(t.length()>0){if(b.length()>0)b.append("\n");b.append(t);}}}return b.toString();}

    private AudioData extractAudio(JSONObject response){JSONArray c=response.optJSONArray("candidates");if(c==null)return null;for(int i=0;i<c.length();i++){JSONObject content=c.optJSONObject(i)==null?null:c.optJSONObject(i).optJSONObject("content");JSONArray parts=content==null?null:content.optJSONArray("parts");if(parts==null)continue;for(int j=0;j<parts.length();j++){JSONObject p=parts.optJSONObject(j);if(p==null)continue;JSONObject in=p.optJSONObject("inlineData");if(in==null)in=p.optJSONObject("inline_data");if(in!=null&&in.optString("data").length()>0){String mime=in.optString("mimeType",in.optString("mime_type","audio/L16;rate=24000"));int rate=24000;int pos=mime.indexOf("rate=");if(pos>=0){String r=mime.substring(pos+5).replaceAll("[^0-9].*$","");try{rate=Integer.parseInt(r);}catch(Exception ignored){AppLog.ignored("GeminiService","Falha tolerada; a execução continuou.",ignored);}}return new AudioData(Base64.decode(in.optString("data"),Base64.DEFAULT),rate);}}}return null;}

    private static void writeWav(File file,byte[] pcm,int sampleRate)throws Exception{FileOutputStream out=new FileOutputStream(file);int dataLen=pcm.length;int total=dataLen+36;int byteRate=sampleRate*2;out.write(new byte[]{'R','I','F','F'});writeIntLE(out,total);out.write(new byte[]{'W','A','V','E','f','m','t',' '});writeIntLE(out,16);writeShortLE(out,1);writeShortLE(out,1);writeIntLE(out,sampleRate);writeIntLE(out,byteRate);writeShortLE(out,2);writeShortLE(out,16);out.write(new byte[]{'d','a','t','a'});writeIntLE(out,dataLen);out.write(pcm);out.close();}
    private static void writeIntLE(FileOutputStream o,int v)throws Exception{o.write(v&255);o.write((v>>8)&255);o.write((v>>16)&255);o.write((v>>24)&255);}private static void writeShortLE(FileOutputStream o,int v)throws Exception{o.write(v&255);o.write((v>>8)&255);}
    private static String read(InputStream in)throws Exception{if(in==null)return "";BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line).append('\n');r.close();return b.toString();}

    /** Converte respostas técnicas da API em mensagens úteis e integralmente em PT-BR. */
    private static String friendlyRagFailure(String displayName,Exception direct,Exception fallback){
        String raw=(shortError(direct)+" "+shortError(fallback)).toLowerCase(Locale.ROOT);
        if(raw.contains("max_tokens_per_chunk")||raw.contains("maxtokensperchunk")||raw.contains("chunking_config")||raw.contains("chunkingconfig"))
            return "Não foi possível dividir este PDF para indexação. A versão atual usa a divisão automática do Gemini; tente novamente. Os outros livros não foram apagados.";
        if(raw.contains("429")||raw.contains("resource_exhausted")||raw.contains("quota"))
            return "O limite temporário de indexação do Gemini foi atingido. Aguarde alguns minutos e toque em INDEXAR AGORA. Os livros já prontos continuam disponíveis.";
        if(raw.contains("401")||raw.contains("403")||raw.contains("permission_denied")||raw.contains("api key"))
            return "A chave do Gemini não foi autorizada para indexar arquivos. Confira a chave em Configurações e tente novamente.";
        if(raw.contains("100 mb")||raw.contains("too large")||raw.contains("request entity too large")||raw.contains("413"))
            return "Este PDF ultrapassa o limite aceito pela Biblioteca do Mestre. Divida ou reduza o arquivo e tente novamente.";
        if(raw.contains("invalid argument")||raw.contains("invalid_argument")||raw.contains("http 400"))
            return "O Gemini recusou este PDF por formato ou conteúdo inválido. Confirme que o arquivo abre normalmente e tente reindexar. Os demais manuais não foram afetados.";
        if(raw.contains("timeout")||raw.contains("timed out")||raw.contains("demorou mais"))
            return "A indexação demorou mais que o esperado. Toque em INDEXAR AGORA novamente; o aplicativo continuará do ponto seguro.";
        return "Não foi possível indexar "+displayName+". Tente novamente. Os outros livros e a campanha permanecem preservados.";
    }

    private static String apiError(JSONObject json,String fallback){JSONObject e=json.optJSONObject("error");if(e!=null)return e.optString("message",fallback);String block=json.optString("promptFeedback");return block.length()>0?block:fallback;}
    private static void ensureKey(String key)throws Exception{if(key==null||key.trim().length()<20)throw new Exception("Configure sua chave da API Gemini em Configurações.");}
    private void rememberWorkingModel(String key,String model){settings.edit().putString(key,model).apply();}
    private static String shortError(Exception e){String s=clean(e).replaceAll("\\s+"," ").trim();return s.length()>180?s.substring(0,177)+"...":s;}
    private static String joinErrors(List<String> errors){StringBuilder b=new StringBuilder();int start=Math.max(0,errors.size()-3);for(int i=start;i<errors.size();i++){if(b.length()>0)b.append(" | ");b.append(errors.get(i));}return b.toString();}
    private static String clean(Exception e){return e.getMessage()==null?e.toString():e.getMessage();}
    private static boolean audioLooksPrematurelyTruncated(AudioData audio,String transcript){
        if(audio==null||audio.bytes==null||audio.sampleRate<=0)return true;
        String spoken=GeminiTtsMarkupDirector.stripTags(transcript==null?"":transcript).trim();
        if(spoken.length()<260)return false;
        double seconds=audio.bytes.length/(audio.sampleRate*2.0d); // PCM 16-bit mono
        // Só rejeita cortes grosseiros (por exemplo, áudio da primeira frase para um bloco inteiro).
        // Margem larga para não confundir fala rápida com truncamento.
        double conservativeMinimum=Math.max(3.5d,spoken.length()/62.0d);
        return seconds<conservativeMinimum;
    }

    private static class AudioData{final byte[] bytes;final int sampleRate;AudioData(byte[] b,int r){bytes=b;sampleRate=r;}}
    private static class HttpStatusException extends Exception{final int status;HttpStatusException(int status,String message){super(message);this.status=status;}}
    private static class AuthException extends Exception{AuthException(String message){super(message);}}
    private static class SpeechCancelledException extends Exception{SpeechCancelledException(){super("Solicitação de voz cancelada.");}}

    public static List<String> chunkText(String text,int max){ArrayList<String> out=new ArrayList<String>();if(text==null)return out;String[] sentences=text.trim().split("(?<=[.!?…])\\s+");StringBuilder b=new StringBuilder();for(int i=0;i<sentences.length;i++){String s=sentences[i].trim();if(s.length()==0)continue;if(b.length()>0&&b.length()+1+s.length()>max){out.add(b.toString());b.setLength(0);}if(s.length()>max){if(b.length()>0){out.add(b.toString());b.setLength(0);}int p=0;while(p<s.length()){int end=Math.min(s.length(),p+max);out.add(s.substring(p,end));p=end;}}else{if(b.length()>0)b.append(' ');b.append(s);}}if(b.length()>0)out.add(b.toString());return out;}
}
