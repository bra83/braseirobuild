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
            speaker=new SpeakerModel(new File(root,"spk").getAbsolutePath());
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
    String classify(byte[] pcm,long startMs,long endMs){
        long duration=endMs-startMs;
        if(duration<2500||duration>15000)return "";
        int start=(int)Math.max(0,Math.min(pcm.length,startMs*32L)),end=(int)Math.max(0,Math.min(pcm.length,endMs*32L));
        if(end-start<80000)return "";
        try(Recognizer rec=new Recognizer(asr,16000f,speaker)){
            JSONObject best=null;
            for(int i=start;i<end;i+=8000){
                byte[] block=Arrays.copyOfRange(pcm,i,Math.min(i+8000,end));
                if(rec.acceptWaveForm(block,block.length)){
                    JSONObject v=new JSONObject(rec.getResult());
                    if(v.has("spk")&&(best==null||v.optInt("spk_frames")>best.optInt("spk_frames")))best=v;
                }
            }
            JSONObject v=new JSONObject(rec.getFinalResult());
            if(v.has("spk")&&(best==null||v.optInt("spk_frames")>best.optInt("spk_frames")))best=v;
            if(best==null||best.optInt("spk_frames")<180)return "";
            JSONArray spk=best.getJSONArray("spk");
            if(spk.length()<64||spk.length()>1024)return "";
            double[] vector=new double[spk.length()];for(int i=0;i<vector.length;i++)vector[i]=spk.getDouble(i);
            return clusters.assign(vector);
        }catch(Exception|LinkageError e){return "";}
    }
    @Override public void close(){if(speaker!=null){speaker.close();speaker=null;}if(asr!=null){asr.close();asr=null;}}
}
