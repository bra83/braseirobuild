package br.com.tutorclinico;
import android.content.Context;
import ai.onnxruntime.*;
import java.io.IOException;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** True waveform separation, Conv-TasNet Libri2Mix sepnoisy 16 kHz FP32. */
final class NeuralVoiceSeparator implements AutoCloseable {
    static final String SHA="3fa85c7bb2537ced4bb7acb2ea91255c7d6b222fd3d0fae5088e8a3999a963fd";
    static final String ASSET="separation/model.onnx";
    private final OrtEnvironment env=OrtEnvironment.getEnvironment();
    private OrtSession session;
    NeuralVoiceSeparator(Context c)throws Exception{
        File f=EcoModelAssets.materialize(c,ASSET,"eco-separation-3fa85c7b.onnx",SHA,()->true);
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
            options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);session=env.createSession(f.getAbsolutePath(),options);
        }
        if(!session.getInputNames().equals(Collections.singleton("mixture"))||session.getOutputNames().size()!=1){close();throw new IOException("Contrato de separação inválido");}
    }
    // Reject zero-variance, nearly silent and constant DC input before ONNX.
    static boolean usableInput(byte[] pcm){
        if(pcm==null||pcm.length<3200||(pcm.length&1)!=0||pcm.length>160000)return false;
        double sum=0,squares=0;int n=pcm.length/2;
        for(int i=0;i<n;i++){
            double v=(short)((pcm[2*i]&255)|(pcm[2*i+1]<<8))/32768.0;
            sum+=v;squares+=v*v;
        }
        return Math.max(0,squares/n-(sum/n)*(sum/n))>=1e-6;
    }
    byte[][] separate(byte[] pcm,BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new CancellationException();
        if(pcm.length<3200||(pcm.length&1)!=0||pcm.length>160000)throw new IOException("Janela inválida");
        byte[][] output=new byte[2][];
        if(!usableInput(pcm))return output;
        int n=pcm.length/2,padded=((n+319)/320)*320;float[] x=new float[padded];
        try{
            for(int i=0;i<n;i++)x[i]=(short)((pcm[2*i]&255)|(pcm[2*i+1]<<8))/32768f;
            try(OnnxTensor tensor=OnnxTensor.createTensor(env,FloatBuffer.wrap(x),new long[]{1,padded});OrtSession.Result result=session.run(Collections.singletonMap("mixture",tensor))){
                float[][][] y=(float[][][])result.get(0).getValue();
                if(y.length!=1||y[0].length!=2||y[0][0].length<padded||y[0][1].length<padded)throw new IOException("Saída incompatível");
                try{
                    for(int k=0;k<2;k++){
                        double energy=0;float peak=0;
                        for(int i=0;i<n;i++){float v=y[0][k][i];if(!Float.isFinite(v))throw new IOException("Saída não finita");energy+=(double)v*v;peak=Math.max(peak,Math.abs(v));}
                        if(Math.sqrt(energy/n)<0.001||peak<0.005f)continue;
                        float gain=Math.min(4f,0.95f/peak);byte[] wave=new byte[n*2];
                        for(int i=0;i<n;i++){int v=Math.round(Math.max(-1f,Math.min(0.99997f,y[0][k][i]*gain))*32768f);wave[2*i]=(byte)v;wave[2*i+1]=(byte)(v>>8);}
                        output[k]=wave;
                    }
                }finally{for(float[] row:y[0])Arrays.fill(row,0);}
            }
            if(!active.getAsBoolean())throw new CancellationException();return output;
        }catch(Exception e){for(byte[] row:output)if(row!=null)Arrays.fill(row,(byte)0);throw e;}
        finally{Arrays.fill(x,0);}
    }
    @Override public void close()throws OrtException{if(session!=null){session.close();session=null;}}
}
