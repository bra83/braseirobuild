package br.com.tutorclinico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded RAM-only embedding reuse. One case or didactic corpus at a time.
 * Exact text digest, model/version and namespace; no patient text retained.
 * Epoch prevents a canceled or locked task from repopulating a cleared cache. */
final class EcoEmbeddingCache {
    static final int LIMIT=2400;
    private final LinkedHashMap<String,float[]> values=new LinkedHashMap<>(256,.75f,true);
    private String namespace="";
    private long epoch;
    synchronized long open(String scope,String model){
        String next=(scope==null||scope.isEmpty()||scope.length()>160)?"":model+"|"+scope;
        if(next.isEmpty()||!next.equals(namespace)){clear();namespace=next;}
        return epoch;
    }
    synchronized void clear(){for(float[] v:values.values())Arrays.fill(v,0);values.clear();namespace="";epoch++;}
    private static String key(String text)throws Exception{
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder(64);for(byte x:hash)b.append(Character.forDigit((x>>>4)&15,16)).append(Character.forDigit(x&15,16));return b.toString();
    }
    synchronized float[] get(long generation,String text)throws Exception{
        if(epoch!=generation||namespace.isEmpty())return null;
        float[] value=values.get(key(text));return value==null?null:value.clone();
    }
    synchronized void put(long generation,String text,float[] vector)throws Exception{
        if(epoch!=generation||namespace.isEmpty()||vector.length!=512)return;
        for(float x:vector)if(!Float.isFinite(x))return;
        float[] old=values.put(key(text),vector.clone());if(old!=null)Arrays.fill(old,0);
        while(values.size()>LIMIT){Map.Entry<String,float[]> first=values.entrySet().iterator().next();Arrays.fill(first.getValue(),0);values.remove(first.getKey());}
    }
    synchronized int size(){return values.size();}
}
