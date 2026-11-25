package br.com.tutorclinico;

import android.content.Context;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.SpeakerModel;
import org.json.*;
import java.io.*;
import java.util.Arrays;

/** Real Vosk x-vector model; groups only within one session, no voiceprint persistence. */
final class SegmentDiarizer implements AutoCloseable {
    private Model asr;
    private SpeakerModel speaker;
    private final SpeakerClusters clusters;
    private NeuralOverlapEngine overlap;
    boolean overlapAvailable(){return overlap!=null;}
    SegmentDiarizer(Context c,SpeakerClusters clusters) throws Exception{
        this.clusters=clusters;
        try{
            File root=new File(c.getNoBackupFilesDir(),"eco-speaker-v1");
            File ready=new File(root,"ready");
            if(!ready.isFile()){
                copy(c,"model-pt",new File(root,"asr"));
                copy(c,"model-spk",new File(root,"spk"));
                if(!ready.createNewFile()&&!ready.isFile())throw new IOException("Speaker model installation");
            }
            asr=new Model(new File(root,"asr").getAbsolutePath());
            if(clusters!=null){
                speaker=new SpeakerModel(new File(root,"spk").getAbsolutePath());
                try{overlap=new NeuralOverlapEngine(c);}catch(Exception|LinkageError unavailable){overlap=null;}
            }
        }catch(Exception|LinkageError e){close();throw e;}
    }
    private static void copy(Context c,String path,File out)throws IOException{
        String[] names=c.getAssets().list(path);
        if(names!=null&&names.length>0){
            if(!out.isDirectory()&&!out.mkdirs())throw new IOException("Model directory");
            for(String n:names)copy(c,path+"/"+n,new File(out,n));
        }else{
            try(InputStream in=c.getAssets().open(path);OutputStream os=new FileOutputStream(out)){
                byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)os.write(b,0,n);
            }
        }
    }
    private void detectOverlap(byte[] pcm,SpeakerTimeline timeline,java.util.function.BooleanSupplier active){
        if(overlap==null)return;
        try{overlap.detect(pcm,timeline,active);}
        catch(Exception|LinkageError e){
            timeline.clearOverlap();try{overlap.close();}catch(Exception ignored){}overlap=null;
        }
    }
    /** One Vosk x-vector pass per chunk, using Vosk utterance timestamps, not its transcript. */
    SpeakerTimeline analyze(byte[] pcm,java.util.function.BooleanSupplier active){
        SpeakerTimeline result=new SpeakerTimeline();
        detectOverlap(pcm,result,active);
        try(Recognizer rec=new Recognizer(asr,16000f,speaker)){
            rec.setWords(true);
            for(int i=0;i<pcm.length&&active.getAsBoolean();i+=8000){
                byte[] block=Arrays.copyOfRange(pcm,i,Math.min(i+8000,pcm.length));
                if(rec.acceptWaveForm(block,block.length))append(result,new JSONObject(rec.getResult()),pcm.length/32L);
            }
            if(active.getAsBoolean())append(result,new JSONObject(rec.getFinalResult()),pcm.length/32L);
        }catch(Exception|LinkageError e){result.clearGroups();}
        return result;
    }
    interface SpeechConsumer {void accept(String text,long startMs,long endMs,String cluster);}
    /** Fast alternative: the same Vosk pass produces text, timestamps and optional x-vectors. */
    void transcribe(byte[] pcm,java.util.function.BooleanSupplier active,SpeechConsumer consumer) throws Exception {
        SpeakerTimeline neural=new SpeakerTimeline();detectOverlap(pcm,neural,active);
        try(Recognizer rec=speaker==null?new Recognizer(asr,16000f):new Recognizer(asr,16000f,speaker)){
            rec.setWords(true);
            for(int i=0;i<pcm.length&&active.getAsBoolean();i+=8000){
                byte[] block=Arrays.copyOfRange(pcm,i,Math.min(i+8000,pcm.length));
                try{if(rec.acceptWaveForm(block,block.length))emitSpeech(new JSONObject(rec.getResult()),pcm.length/32L,consumer,neural);}
                finally{Arrays.fill(block,(byte)0);}
            }
            if(active.getAsBoolean())emitSpeech(new JSONObject(rec.getFinalResult()),pcm.length/32L,consumer,neural);
        }
    }
    private void emitSpeech(JSONObject v,long maxMs,SpeechConsumer consumer,SpeakerTimeline neural)throws JSONException{
        String text=v.optString("text","").trim();
        if(text.isEmpty())return;
        JSONArray words=v.optJSONArray("result");
        long start=0,end=maxMs;
        if(words!=null&&words.length()>0){
            start=Math.max(0,Math.round(words.getJSONObject(0).getDouble("start")*1000));
            end=Math.min(maxMs,Math.round(words.getJSONObject(words.length()-1).getDouble("end")*1000));
        }
        if(end<=start)throw new JSONException("Tempos Vosk inválidos");
        consumer.accept(text,start,end,neural.hasOverlap(start,end)?SpeakerTimeline.OVERLAP:clusterFor(v,end-start));
    }
    private String clusterFor(JSONObject v,long duration)throws JSONException {
        JSONArray spk=v.optJSONArray("spk");
        if(clusters!=null&&spk!=null&&spk.length()>=64&&spk.length()<=1024&&v.optInt("spk_frames")>=100&&duration<=15000){
            double[] vector=new double[spk.length()];for(int i=0;i<vector.length;i++)vector[i]=spk.getDouble(i);
            return clusters.assign(vector);
        }
        return "";
    }
    private void append(SpeakerTimeline result,JSONObject v,long maxMs)throws JSONException{
        JSONArray words=v.optJSONArray("result");
        if(words==null||words.length()==0)return;
        long start=Math.max(0,Math.round(words.getJSONObject(0).getDouble("start")*1000));
        long end=Math.min(maxMs,Math.round(words.getJSONObject(words.length()-1).getDouble("end")*1000));
        if(end<=start)return;
        result.add(start,end,result.hasOverlap(start,end)?"":clusterFor(v,end-start));
    }
    @Override public void close(){if(overlap!=null){try{overlap.close();}catch(Exception ignored){}overlap=null;}if(speaker!=null){speaker.close();speaker=null;}if(asr!=null){asr.close();asr=null;}}
}
