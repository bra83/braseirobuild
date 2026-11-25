package br.com.tutorclinico;
import android.content.Context;
import ai.onnxruntime.*;
import org.json.*;
import java.io.*;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Experimental local Portuguese NLI. No diagnosis, credibility score or automatic clinical conclusion. */
final class NliEngine implements AutoCloseable {
    static final String MODEL="bertimbau-large-inferbr-nli-q8@6c9d7e7";
    static final String SHA="fc4f10b69a842b6a314195822d370e4c9380776051be38978e2ec45f0425d50b";
    static final long SIZE=336269909L;
    private final Map<String,Integer> vocab=new HashMap<>();
    private final OrtEnvironment env=OrtEnvironment.getEnvironment();
    private OrtSession session;
    static boolean available(Context c){
        try(android.content.res.AssetFileDescriptor fd=c.getAssets().openFd("nli/model.onnx");InputStream v=c.getAssets().open("nli/vocab.txt");InputStream gate=c.getAssets().open("nli/gate.txt")){return fd.getLength()==SIZE&&v.read()!=-1&&gate.read()!=-1;}catch(IOException e){return false;}
    }
    private static String hash(File f)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}
        StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
    }
    NliEngine(Context c,BooleanSupplier active)throws Exception{
        if(!available(c))throw new IOException("Modelo NLI não provisionado");
        try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getAssets().open("nli/vocab.txt"),StandardCharsets.UTF_8))){String line;int i=0;while((line=r.readLine())!=null)vocab.put(line,i++);}
        if(vocab.size()!=29794||!Integer.valueOf(101).equals(vocab.get("[CLS]"))||!Integer.valueOf(102).equals(vocab.get("[SEP]"))||!Integer.valueOf(100).equals(vocab.get("[UNK]")))throw new IOException("Vocabulário incompatível");
        File f=new File(c.getFilesDir(),"eco-nli-6c9d7e7.onnx");
        if(!f.isFile()||f.length()!=SIZE||!SHA.equals(hash(f))){
            File tmp=new File(f.getPath()+".tmp");
            try{try(InputStream in=c.getAssets().open("nli/model.onnx");OutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(!active.getAsBoolean())throw new CancellationException();out.write(b,0,n);}}if(tmp.length()!=SIZE||!SHA.equals(hash(tmp))||!tmp.renameTo(f))throw new IOException("Integridade NLI");}finally{tmp.delete();}
        }
        if(!active.getAsBoolean())throw new CancellationException();
        try(OrtSession.SessionOptions opts=new OrtSession.SessionOptions()){opts.setIntraOpNumThreads(2);opts.setInterOpNumThreads(1);session=env.createSession(f.getAbsolutePath(),opts);}
    }
    private double[] predict(String a,String b,BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new CancellationException();
        long[][] p=NliWordPiece.pair(a,b,vocab);long[] shape={1,p[0].length};
        try(OnnxTensor ids=OnnxTensor.createTensor(env,LongBuffer.wrap(p[0]),shape);OnnxTensor mask=OnnxTensor.createTensor(env,LongBuffer.wrap(p[1]),shape);OnnxTensor types=OnnxTensor.createTensor(env,LongBuffer.wrap(p[2]),shape)){
            Map<String,OnnxTensor> input=new HashMap<>();input.put("input_ids",ids);input.put("attention_mask",mask);if(session.getInputNames().contains("token_type_ids"))input.put("token_type_ids",types);
            try(OrtSession.Result r=session.run(input)){float[][] logits=(float[][])r.get(0).getValue();if(logits.length!=1||logits[0].length!=3)throw new IOException("Saída NLI inválida");double max=Math.max(logits[0][0],Math.max(logits[0][1],logits[0][2]));double[] scores=new double[3];double sum=0;for(int i=0;i<3;i++){scores[i]=Math.exp(logits[0][i]-max);sum+=scores[i];}if(!Double.isFinite(sum)||sum<=0)throw new IOException("Logits inválidos");for(int i=0;i<3;i++)scores[i]/=sum;return scores;}
        }
    }
    JSONObject analyze(JSONObject req,BooleanSupplier active,NeuralSemanticEngine.Progress progress)throws Exception{
        JSONArray pairs=req.getJSONArray("pairs");if(pairs.length()<1||pairs.length()>12)throw new IOException("Limite de 12 pares");
        JSONArray result=new JSONArray();
        for(int i=0;i<pairs.length();i++){
            JSONObject p=pairs.getJSONObject(i);String a=p.getString("a").trim(),b=p.getString("b").trim();if(a.length()>3000||b.length()>3000)throw new IOException("Trecho excede limite");
            double[] ab=predict(a,b,active),ba=predict(b,a,active);
            result.put(new JSONObject().put("i",i).put("ab",new JSONArray(ab)).put("ba",new JSONArray(ba)));
            progress.report(i+1,pairs.length(),"Comparação textual bidirecional");
        }
        return new JSONObject().put("model",MODEL).put("labels",new JSONArray(List.of("contradiction","entailment","neutral"))).put("comparisons",result);
    }
    @Override public void close()throws OrtException{if(session!=null)session.close();}
}
