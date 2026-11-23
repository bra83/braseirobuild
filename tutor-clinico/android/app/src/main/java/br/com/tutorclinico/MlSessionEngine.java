package br.com.tutorclinico;
import android.app.Activity;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import org.vosk.android.RecognitionListener;
/** Vosk runs in memory. Raw audio is never saved to disk, uploaded, or backed up. */
final class MlSessionEngine implements RecognitionListener {
 interface Events { void state(String s); void segment(String s); void partial(String s); }
 private final Activity activity; private final Events events;
 private Model model; private SpeechService speech; private Recognizer rec;
 private boolean wanted,preparing;private long lastPartialAt;
 MlSessionEngine(Activity activity,Events events){this.activity=activity;this.events=events;}
 synchronized void start(){
  if(wanted||speech!=null||preparing){events.state("JÁ EM EXECUÇÃO");return;}
  wanted=true;
  if(model!=null){begin();return;}
  preparing=true;events.state("PREPARANDO MODELO OFFLINE");
  StorageService.unpack(activity,"model-pt","modelo-pt",loaded->{
   synchronized(MlSessionEngine.this){preparing=false;if(!wanted){loaded.close();return;}model=loaded;begin();}
  },err->{synchronized(MlSessionEngine.this){preparing=false;wanted=false;}events.state("ERRO MODELO: "+err.getMessage());});
 }
 private synchronized void begin(){
  if(!wanted||model==null)return;
  try{
   rec=new Recognizer(model,16000.0f);
   speech=new SpeechService(rec,16000.0f);
   if(!speech.startListening(this))throw new IllegalStateException("Falha ao iniciar captura");
   activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   events.state("CAPTURANDO · VOSK PT-BR");
  }catch(Exception e){stop();events.state("ERRO: "+e.getMessage());}
 }
 synchronized void stop(){
  wanted=false;
  if(speech!=null){try{speech.stop();}catch(Exception ignored){}try{speech.shutdown();}catch(Exception ignored){}speech=null;}
  if(rec!=null){try{rec.close();}catch(Exception ignored){}rec=null;}
  activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  events.state("PARADO · ÁUDIO NÃO ARMAZENADO");
 }
 void destroy(){stop();if(model!=null){try{model.close();}catch(Exception ignored){}model=null;}}
 private static String extract(String json,String field){try{return new JSONObject(json).optString(field,"").trim();}catch(Exception e){return "";}}
 @Override public void onResult(String data){emit(data);}
 @Override public void onFinalResult(String data){emit(data);}
 private void emit(String data){String s=extract(data,"text");if(!s.isEmpty())events.segment(s);}
 @Override public void onPartialResult(String data){long t=System.currentTimeMillis();if(t-lastPartialAt<850)return;lastPartialAt=t;events.partial(extract(data,"partial"));}
 @Override public void onError(Exception e){events.state("ERRO RECONHECIMENTO: "+e.getMessage());stop();}
 @Override public void onTimeout(){stop();}
}
