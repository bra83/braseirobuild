package br.com.tutorclinico;

import android.content.Context;
import ai.onnxruntime.*;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Experimental overlapped-speech detection, not source separation or identification.
 * Pyannote segmentation 3.0 (MIT), ONNX export by sherpa-onnx.
 * The seven powerset classes are silence, three local single-speaker classes,
 * and the three two-speaker pairs. Local IDs never become clinical roles.
 */
final class NeuralOverlapEngine implements AutoCloseable {
    static final String MODEL="pyannote-segmentation-3.0-fp32-sherpa";
    static final String SHA="220ad67ca923bef2fa91f2390c786097bf305bceb5e261d4af67b38e938e1079";
    static final String ASSET="overlap/model.onnx";
    static final int SAMPLE_RATE=16000, WINDOW=160000, STEP=80000, RF=991, FRAME=270, FRAMES=589;
    private static final double THRESHOLD=0.65; // experimental, not calibrated probability
    private final OrtEnvironment env=OrtEnvironment.getEnvironment();
    private OrtSession session;
    static boolean available(Context c){return EcoModelAssets.available(c,ASSET);}
    NeuralOverlapEngine(Context c)throws Exception{
        File f=EcoModelAssets.materialize(c,ASSET,"eco-overlap-220ad67c.onnx",SHA,()->true);
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
            options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);
            session=env.createSession(f.getAbsolutePath(),options);
        }
        if(!session.getInputNames().equals(Collections.singleton("x"))||!session.getOutputNames().equals(Collections.singleton("y"))){
            close();throw new IOException("Contrato ONNX de sobreposição incompatível");
        }
    }
    /** Stable normalization for model log probabilities or logits. */
    static double overlapProbability(float[] logits)throws IOException{
        if(logits==null||logits.length!=7)throw new IOException("Classes inválidas");
        double max=-Double.MAX_VALUE;
        for(float v:logits){if(!Float.isFinite(v))throw new IOException("Saída neural inválida");max=Math.max(max,v);}
        double total=0,overlap=0;
        for(int i=0;i<7;i++){double p=Math.exp(logits[i]-max);total+=p;if(i>=4)overlap+=p;}
        return overlap/total;
    }
    void detect(byte[] pcm,SpeakerTimeline timeline,BooleanSupplier active)throws Exception{
        if((pcm.length&1)!=0)throw new IOException("PCM inválido");
        int samples=pcm.length/2;
        if(samples==0)return;
        float[] input=new float[WINDOW];
        long startRun=-1,endRun=-1;
        try{
            for(int start=0;;start+=STEP){
                if(!active.getAsBoolean())throw new CancellationException();
                boolean last=start+WINDOW>=samples;
                Arrays.fill(input,0);
                for(int i=0;i<Math.min(WINDOW,samples-start);i++){
                    int p=2*(start+i);
                    input[i]=(short)((pcm[p]&255)|(pcm[p+1]<<8))/32768f;
                }
                float[][] logits;
                try(OnnxTensor tensor=OnnxTensor.createTensor(env,FloatBuffer.wrap(input),new long[]{1,1,WINDOW});
                    OrtSession.Result result=session.run(Collections.singletonMap("x",tensor))){
                    float[][][] batch=(float[][][])result.get(0).getValue();
                    if(batch.length!=1||batch[0].length!=FRAMES)throw new IOException("Número de quadros inesperado");
                    logits=batch[0];
                }
                int lower=start==0?0:start+STEP/2;
                int upper=last?samples:start+WINDOW-STEP/2;
                for(int i=0;i<logits.length;i++){
                    int center=start+RF/2+i*FRAME;
                    if(center<lower||center>=upper)continue;
                    long a=Math.max(lower,center-FRAME/2)*1000L/SAMPLE_RATE;
                    long b=Math.min(upper,center+FRAME/2)*1000L/SAMPLE_RATE;
                    if(b<=a)continue;
                    if(overlapProbability(logits[i])>=THRESHOLD){
                        if(startRun>=0&&a-endRun>35){
                            if(endRun-startRun>=150)timeline.addOverlap(startRun,endRun);
                            startRun=-1;
                        }
                        if(startRun<0)startRun=a;
                        endRun=b;
                    }else if(startRun>=0){
                        if(endRun-startRun>=150)timeline.addOverlap(startRun,endRun);
                        startRun=-1;endRun=-1;
                    }
                }
                if(last)break;
            }
            if(startRun>=0&&endRun-startRun>=150)timeline.addOverlap(startRun,endRun);
        }finally{Arrays.fill(input,0);}
    }
    @Override public void close()throws OrtException{if(session!=null){session.close();session=null;}}
}
