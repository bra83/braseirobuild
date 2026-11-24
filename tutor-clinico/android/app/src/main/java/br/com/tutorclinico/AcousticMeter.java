package br.com.tutorclinico;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Low-cost signal measurements on 16 kHz mono signed PCM 16-bit, without
 * keeping or sending raw samples. Aggregates five-second windows.
 *
 * Measurements are acoustics, NOT an emotion, psychiatric or stress classifier.
 * No person attribution is possible in a mono multi-speaker recording.
 */
final class AcousticMeter {
    private static final int SAMPLE_RATE=16000;
    private static final int WINDOW_SAMPLES=SAMPLE_RATE*5;
    private static final int RING=2048;
    private final short[] recent=new short[RING];
    private int ringPos=0,ringCount=0;
    private long frames=0;
    private int inWindow=0;
    private double squared=0;
    private int quiet=0;
    private final ArrayList<String> ready=new ArrayList<>();

    synchronized void accept(byte[] pcm,int length) {
        int size=Math.min(length,pcm.length);
        for(int i=0;i+1<size;i+=2){
            int v=(short)((pcm[i]&255)|((pcm[i+1]&255)<<8));
            squared+=(double)v*v;
            if(Math.abs(v)<500)quiet++;
            recent[ringPos++]=(short)v;
            if(ringPos==RING)ringPos=0;
            if(ringCount<RING)ringCount++;
            inWindow++;
            frames++;
            if(inWindow==WINDOW_SAMPLES)emit();
        }
    }
    synchronized List<String> drain(){
        ArrayList<String> out=new ArrayList<>(ready);
        ready.clear();return out;
    }
    synchronized void finish(){
        if(inWindow>0)emit();
    }
    private void emit(){
        if(inWindow<=0)return;
        double rms=Math.sqrt(squared/inWindow);
        double db=20*Math.log10(Math.max(1,rms)/32768.0);
        double silence=(double)quiet/inWindow;
        Double f0=estimatePitchHz(rms);
        try{
            JSONObject o=new JSONObject();
            o.put("atMs",(frames-inWindow)*1000L/SAMPLE_RATE);
            o.put("durationMs",inWindow*1000L/SAMPLE_RATE);
            o.put("rmsDbFs",Math.round(db*10)/10.0);
            o.put("nearSilenceRatio",Math.round(silence*1000)/1000.0);
            if(f0!=null)o.put("f0EstimateHz",Math.round(f0));
            o.put("source","PCM16 16kHz mono");
            ready.add(o.toString());
        }catch(Exception ignored){}
        inWindow=0;squared=0;quiet=0;
    }

    /**
     * A deliberately conservative approximate fundamental frequency estimate.
     * Search 70-340 Hz using normalized autocorrelation on the newest ~128ms.
     * Ignore low-energy and unvoiced windows, and never report it as emotion.
     */
    private Double estimatePitchHz(double rms){
        if(ringCount<RING||rms<350)return null;
        double best=-1;int lagBest=-1;
        final int n=1024;
        for(int lag=47;lag<=228;lag+=2){
            double cross=0,aa=0,bb=0;
            for(int j=0;j<n;j++){
                int ai=(ringPos-1-j+RING)%RING;
                int bi=(ai-lag+RING)%RING;
                double a=recent[ai],b=recent[bi];
                cross+=a*b;aa+=a*a;bb+=b*b;
            }
            double corr=cross/Math.sqrt(Math.max(1,aa*bb));
            if(corr>best){best=corr;lagBest=lag;}
        }
        if(best<0.68||lagBest<0)return null;
        return (double)SAMPLE_RATE/lagBest;
    }
}
