package com.braseiro.dnd5e;
import org.json.JSONObject;
/** Compatibility facade. All mechanics execute through SupplementRuntimeGateway. */
@Deprecated
public final class CompiledSupplementExecutionEngine{
 private CompiledSupplementExecutionEngine(){}
 public static JSONObject execute(String id,JSONObject in){JSONObject out=SupplementRuntimeGateway.execute(id,in);try{JSONObject result=out.optJSONObject("result");if(result!=null){java.util.Iterator<String> keys=result.keys();while(keys.hasNext()){String k=keys.next();out.put(k,result.opt(k));}}}catch(Exception ignored){}return out;}
}
