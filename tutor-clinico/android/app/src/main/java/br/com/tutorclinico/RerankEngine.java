package br.com.tutorclinico;
import android.content.Context;
import ai.onnxruntime.*;
import org.json.*;
import java.io.*;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.concurrent.CancellationException;
final class RerankEngine implements AutoCloseable {
 static final String MODEL="mmarco-mMiniLMv2-L12-H384-int8@6772eee";
 static final String SHA="6e57207c62deb625b5751a376f32ac66109281b0d199a2aec09f2671e066acf5";
 private final RerankTokenizer tokenizer;
 private final OrtEnvironment env=OrtEnvironment.getEnvironment();
 private final OrtSession session;
 static boolean available(Context c){return EcoModelAssets.available(c,"rerank/model.onnx")&&EcoModelAssets.available(c,"rerank/vocab.txt")&&EcoModelAssets.available(c,"rerank/gate.txt");}
 RerankEngine(Context c,BooleanSupplier active)throws Exception{
  if(!available(c))throw new IOException("Reranker não provisionado");
  tokenizer=new RerankTokenizer(new InputStreamReader(c.getAssets().open("rerank/vocab.txt"),StandardCharsets.UTF_8));
  File f=EcoModelAssets.materialize(c,"rerank/model.onnx","eco-reranker-6772eee.onnx",SHA,active);
  if(!active.getAsBoolean())throw new CancellationException();
  try(OrtSession.SessionOptions o=new OrtSession.SessionOptions()){o.setIntraOpNumThreads(2);o.setInterOpNumThreads(1);session=env.createSession(f.getAbsolutePath(),o);}
 }
 JSONObject analyze(JSONObject req,BooleanSupplier active,NeuralSemanticEngine.Progress progress)throws Exception{
  String query=req.getString("query").trim();JSONArray rows=req.getJSONArray("rows");
  if(query.isEmpty()||query.length()>500||rows.length()<1||rows.length()>20)throw new IOException("Consulta inválida");
  List<JSONObject> hits=new ArrayList<>();JSONArray skipped=new JSONArray();
  for(int i=0;i<rows.length();i++){
   if(!active.getAsBoolean())throw new CancellationException();
   String t=rows.getJSONObject(i).getString("text");if(t.length()>12000)throw new IOException("Trecho longo");
   long[] ids;try{ids=tokenizer.pair(query,t);}catch(IllegalArgumentException e){skipped.put(i);continue;}
   long[] mask=new long[ids.length];Arrays.fill(mask,1);long[] shape={1,ids.length};
   try(OnnxTensor a=OnnxTensor.createTensor(env,LongBuffer.wrap(ids),shape);OnnxTensor b=OnnxTensor.createTensor(env,LongBuffer.wrap(mask),shape);OnnxTensor types=OnnxTensor.createTensor(env,LongBuffer.wrap(new long[ids.length]),shape)){
    Map<String,OnnxTensor> input=new HashMap<>();input.put("input_ids",a);input.put("attention_mask",b);if(session.getInputNames().contains("token_type_ids"))input.put("token_type_ids",types);
    try(OrtSession.Result r=session.run(input)){float[][] v=(float[][])r.get(0).getValue();if(v.length!=1||v[0].length!=1||!Float.isFinite(v[0][0]))throw new IOException("Saída inválida");hits.add(new JSONObject().put("i",i).put("score",1/(1+Math.exp(-v[0][0]))));}
   }
   progress.report(i+1,rows.length(),"Reordenando fontes com cross-encoder");
  }
  hits.sort(Comparator.comparingDouble((JSONObject o)->o.optDouble("score")).reversed());
  return new JSONObject().put("model",MODEL).put("count",rows.length()).put("hits",new JSONArray(hits)).put("skipped",skipped);
 }
 @Override public void close()throws OrtException{session.close();}
}
