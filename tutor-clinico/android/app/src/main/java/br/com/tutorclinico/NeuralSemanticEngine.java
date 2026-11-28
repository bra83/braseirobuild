package br.com.tutorclinico;

import android.content.Context;
import ai.onnxruntime.*;
import org.json.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Local sentence embeddings. No transcript or embeddings written to disk. */
final class NeuralSemanticEngine implements AutoCloseable {
    static final String MODEL="distiluse-base-multilingual-cased-v2@bfe45d0-u8u8-v2";
    static final int MAX_ROWS=1200, MAX_TOKENS=128, DIM=512;
    private final Map<String,Integer> vocab=new HashMap<>();
    private final float[] weights=new float[512*768], bias=new float[512];
    private final OrtEnvironment env=OrtEnvironment.getEnvironment();
    private OrtSession session;
    private EcoEmbeddingCache sharedCache;
    private long cacheGeneration;
    private int reused,computed;
    void setCache(EcoEmbeddingCache cache,long generation){sharedCache=cache;cacheGeneration=generation;}
    private float[] cachedVector(String text,BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new CancellationException();
        float[] v=sharedCache==null?null:sharedCache.get(cacheGeneration,text);
        if(v!=null){reused++;return v;}
        v=vector(text,active);computed++;
        if(active.getAsBoolean()&&sharedCache!=null)sharedCache.put(cacheGeneration,text,v);
        return v;
    }
    NeuralSemanticEngine(Context context) throws Exception {
        try(BufferedReader in=new BufferedReader(new InputStreamReader(context.getAssets().open("semantic/vocab.txt"),StandardCharsets.UTF_8))){
            String line;int i=0;while((line=in.readLine())!=null)vocab.put(line,i++);
        }
        for(String s:new String[]{"[UNK]","[CLS]","[SEP]","[PAD]"})if(!vocab.containsKey(s))throw new IOException("Vocabulário inválido");
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(context.getAssets().open("semantic/dense.bin")))){
            for(int i=0;i<weights.length;i++)weights[i]=Float.intBitsToFloat(Integer.reverseBytes(in.readInt()));
            for(int i=0;i<bias.length;i++)bias[i]=Float.intBitsToFloat(Integer.reverseBytes(in.readInt()));
            if(in.read()!=-1)throw new IOException("Dimensão inválida");
        }
        File file=new File(context.getFilesDir(),"eco-semantic-bfe45d0-u8u8-v2.onnx");
        long expected;try(android.content.res.AssetFileDescriptor fd=context.getAssets().openFd("semantic/model.onnx")){expected=fd.getLength();}
        if(!file.isFile()||file.length()!=expected){
            File tmp=new File(file.getPath()+".tmp");
            try(InputStream in=context.getAssets().open("semantic/model.onnx");OutputStream out=new FileOutputStream(tmp)){
                byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            }
            if(!tmp.renameTo(file)){tmp.delete();throw new IOException("Modelo não instalado");}
        }
        try(OrtSession.SessionOptions opts=new OrtSession.SessionOptions()){
            opts.setIntraOpNumThreads(Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors())));opts.setInterOpNumThreads(1);
            session=env.createSession(file.getAbsolutePath(),opts);
        }
    }
    private static boolean punctuation(int cp){
        int t=Character.getType(cp);
        return (cp>=33&&cp<=47)||(cp>=58&&cp<=64)||(cp>=91&&cp<=96)||(cp>=123&&cp<=126)||
            t==Character.CONNECTOR_PUNCTUATION||t==Character.DASH_PUNCTUATION||t==Character.START_PUNCTUATION||
            t==Character.END_PUNCTUATION||t==Character.INITIAL_QUOTE_PUNCTUATION||t==Character.FINAL_QUOTE_PUNCTUATION||t==Character.OTHER_PUNCTUATION;
    }
    private List<Integer> tokens(String text){
        StringBuilder clean=new StringBuilder();
        text.codePoints().forEach(cp->{
            if(cp==0||cp==0xfffd)return;
            if(Character.isWhitespace(cp)||Character.isSpaceChar(cp)){clean.append(' ');return;}
            int type=Character.getType(cp);
            if(type==Character.CONTROL||type==Character.FORMAT)return;
            if(punctuation(cp)||Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN)
                clean.append(' ').appendCodePoint(cp).append(' ');
            else clean.appendCodePoint(cp);
        });
        List<Integer> out=new ArrayList<>();
        for(String word:java.text.Normalizer.normalize(clean.toString(),java.text.Normalizer.Form.NFC).trim().split("\\s+")){
            if(word.isEmpty())continue;
            if(word.codePointCount(0,word.length())>100){out.add(vocab.get("[UNK]"));continue;}
            ArrayList<Integer> part=new ArrayList<>();
            int start=0;
            while(start<word.length()){
                int end=word.length();Integer found=null;
                while(end>start){
                    found=vocab.get((start==0?"":"##")+word.substring(start,end));
                    if(found!=null)break;
                    end=word.offsetByCodePoints(end,-1);
                }
                if(found==null){part.clear();part.add(vocab.get("[UNK]"));break;}
                part.add(found);start=end;
            }
            out.addAll(part);
        }
        return out;
    }
    private float[] embed(List<Integer> ids,int start,int end) throws Exception{
        int n=end-start+2;long[] input=new long[n],mask=new long[n];
        input[0]=vocab.get("[CLS]");input[n-1]=vocab.get("[SEP]");
        for(int i=start;i<end;i++)input[i-start+1]=ids.get(i);
        Arrays.fill(mask,1);
        try(OnnxTensor a=OnnxTensor.createTensor(env,LongBuffer.wrap(input),new long[]{1,n});
            OnnxTensor b=OnnxTensor.createTensor(env,LongBuffer.wrap(mask),new long[]{1,n});
            OrtSession.Result result=session.run(Map.of("input_ids",a,"attention_mask",b))){
            float[][][] tensor=(float[][][])result.get(0).getValue();
            if(tensor.length!=1||tensor[0].length!=n||tensor[0][0].length!=768)throw new IOException("Saída incompatível");
            float[] mean=new float[768];
            for(float[] row:tensor[0])for(int j=0;j<768;j++)mean[j]+=row[j]/n;
            float[] v=new float[DIM];
            for(int i=0;i<DIM;i++){double sum=bias[i];for(int j=0;j<768;j++)sum+=weights[i*768+j]*mean[j];v[i]=(float)Math.tanh(sum);}
            normalize(v);return v;
        }
    }
    private static void normalize(float[] v) throws IOException{
        double norm=0;for(float f:v)norm+=f*f;
        if(!Double.isFinite(norm)||norm<1e-12)throw new IOException("Vetor inválido");
        norm=Math.sqrt(norm);for(int i=0;i<v.length;i++)v[i]/=norm;
    }
    private float[] vector(String text,BooleanSupplier active) throws Exception{
        List<Integer> ids=tokens(text);if(ids.isEmpty())throw new IOException("Texto vazio");
        float[] v=new float[DIM];
        for(int i=0;i<ids.size();i+=MAX_TOKENS-2){
            if(!active.getAsBoolean())throw new CancellationException();
            int end=Math.min(ids.size(),i+MAX_TOKENS-2);float[] part=embed(ids,i,end);
            for(int j=0;j<DIM;j++)v[j]+=part[j]*(end-i);
        }
        normalize(v);return v;
    }
    private static double cosine(float[] a,float[] b){double s=0;for(int i=0;i<DIM;i++)s+=a[i]*b[i];return Math.max(-1,Math.min(1,s));}
    interface Progress {void report(int done,int total,String stage);}
    JSONObject analyze(JSONObject request,BooleanSupplier active,Progress progress) throws Exception{
        reused=0;computed=0;
        JSONArray rows=request.getJSONArray("rows");String query=request.optString("query","").trim();
        String scope=request.optString("scope","paciente");
        boolean longitudinal=request.optBoolean("longitudinal",false);
        boolean personal=request.optBoolean("personal",false);
        if(personal&&(!scope.equals("revisado")||!request.optString("cacheScope","").isEmpty()))throw new IOException("Material pessoal exige escopo revisado sem cache persistente");
        if(longitudinal&&!scope.equals("paciente")&&!personal)throw new IOException("Histórico exige falas do paciente");
        if(!Set.of("paciente","terapeuta","desconhecido","todos","revisado").contains(scope))throw new IOException("Filtro inválido");
        for(int i=0;i<rows.length();i++){
            if(longitudinal&&rows.getJSONObject(i).optString("sessionId").isEmpty())throw new IOException("Sessão de origem ausente");
            String role=rows.getJSONObject(i).optString("speaker","desconhecido");
            if(!Set.of("paciente","terapeuta","desconhecido").contains(role)||(!scope.equals("todos")&&!role.equals(scope.equals("revisado")?"desconhecido":scope)))throw new IOException("Trecho fora do filtro de falantes");
        }
        if(rows.length()==0||rows.length()>MAX_ROWS||query.length()>500)throw new IOException("Limite de análise: 1–1200 trechos e consulta de até 500 caracteres");
        JSONArray queries=request.optJSONArray("queries");
        if(queries!=null){
            if(queries.length()<1||queries.length()>40)throw new IOException("Limite de 40 consultas");
            // One embedding per distinct text, one model load, bounded top hits per query.
            Set<String> ids=new HashSet<>();
            List<String> keys=new ArrayList<>();
            List<float[]> qv=new ArrayList<>();
            List<List<JSONObject>> results=new ArrayList<>();
            for(int k=0;k<queries.length();k++){
                if(!active.getAsBoolean())throw new CancellationException();
                JSONObject item=queries.getJSONObject(k);
                String key=item.getString("key"),text=item.getString("text").trim();
                if(key.isEmpty()||key.length()>80||!ids.add(key)||text.isEmpty()||text.length()>500)throw new IOException("Consulta inválida");
                keys.add(key);qv.add(cachedVector(text,active));results.add(new ArrayList<>());
                progress.report(0,rows.length(),"Preparando consultas "+(k+1)+"/"+queries.length());
            }
            Map<String,float[]> cache=new HashMap<>();
            for(int i=0;i<rows.length();i++){
                if(!active.getAsBoolean())throw new CancellationException();
                String text=rows.getJSONObject(i).getString("text");
                if(text.length()>12000)throw new IOException("Trecho muito longo");
                float[] v=cache.get(text);
                if(v==null){v=cachedVector(text,active);cache.put(text,v);}
                for(int k=0;k<qv.size();k++){
                    double score=cosine(qv.get(k),v);
                    if(!SemanticRanking.relevant(score,true))continue;
                    List<JSONObject> list=results.get(k);
                    // Rows arrive in index order. Equal scores keep earlier sources.
                    // Avoid JSON allocation and a full sort for candidates below the current top five.
                    int position=0;
                    while(position<list.size() && list.get(position).optDouble("score")>=score)position++;
                    if(position>=5)continue;
                    list.add(position,new JSONObject().put("i",i).put("score",score));
                    if(list.size()>5)list.remove(5);
                }
                progress.report(i+1,rows.length(),"Relacionando trechos aos temas");
            }
            JSONArray grouped=new JSONArray();
            for(int k=0;k<keys.size();k++)grouped.put(new JSONObject().put("key",keys.get(k)).put("hits",new JSONArray(results.get(k))));
            return new JSONObject().put("model",MODEL).put("count",rows.length()).put("dimensions",DIM)
                .put("cacheReused",reused).put("cacheComputed",computed).put("scope",scope).put("queryMin",SemanticRanking.QUERY_MIN).put("groups",grouped);
        }
        progress.report(0,rows.length(),"Preparando consulta");
        float[] q=query.isEmpty()?null:cachedVector(query,active);
        Map<String,float[]> memo=new HashMap<>();
        progress.report(0,rows.length(),"Analisando trechos");
        List<float[]> vs=new ArrayList<>();List<JSONObject> hits=new ArrayList<>();List<JSONObject> pairs=new ArrayList<>();
        for(int i=0;i<rows.length();i++){
            if(!active.getAsBoolean())throw new CancellationException();
            String text=rows.getJSONObject(i).getString("text");
            if(text.length()>12000)throw new IOException("Trecho muito longo: divida antes de analisar");
            float[] v=memo.get(text);
            if(v==null){v=cachedVector(text,active);memo.put(text,v);}
            if(q!=null){double score=cosine(q,v);if(SemanticRanking.relevant(score,true))hits.add(new JSONObject().put("i",i).put("score",score));}
            double best=-2;int match=-1;
            JSONObject row=rows.getJSONObject(i);
            if(!request.optBoolean("retrievalOnly",false)&&SemanticRanking.eligible(text))for(int j=0;j<vs.size();j++){
                JSONObject prior=rows.getJSONObject(j);
                if(longitudinal&&row.optString("sessionId").equals(prior.optString("sessionId")))continue;
                if(!SemanticRanking.eligible(prior.getString("text"))||(!longitudinal&&!SemanticRanking.canCompare(row.optString("speaker","desconhecido"),prior.optString("speaker","desconhecido"),row.isNull("atMs")?null:row.optLong("atMs"),prior.isNull("atMs")?null:prior.optLong("atMs"),i,j)))continue;
                double score=cosine(v,vs.get(j));if(score>best){best=score;match=j;}
            }
            if(match>=0&&SemanticRanking.relevant(best,false))pairs.add(new JSONObject().put("i",i).put("j",match).put("score",best));
            vs.add(v);
            progress.report(i+1,rows.length(),"Analisando trechos");
        }
        progress.report(rows.length(),rows.length(),"Organizando resultados");
        Comparator<JSONObject> desc=Comparator.comparingDouble((JSONObject o)->o.optDouble("score")).reversed();
        hits.sort(desc);pairs.sort(desc);
        List<JSONObject> diverse=new ArrayList<>();int[] usage=new int[rows.length()];
        for(JSONObject pair:pairs){if(diverse.size()>=20)break;if(SemanticRanking.acceptPair(pair.getInt("i"),pair.getInt("j"),usage))diverse.add(pair);}
        return new JSONObject().put("model",MODEL).put("count",rows.length()).put("dimensions",DIM)
          .put("hits",new JSONArray(hits.subList(0,Math.min(20,hits.size()))))
          .put("cacheReused",reused).put("cacheComputed",computed).put("scope",scope).put("queryMin",SemanticRanking.QUERY_MIN).put("recurrenceMin",SemanticRanking.RECURRENCE_MIN)
          .put("pairs",new JSONArray(diverse));
    }
    @Override public void close() throws OrtException {if(session!=null)session.close();}
}
