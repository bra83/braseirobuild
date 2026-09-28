package com.braseiro.pathfinder2e;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/** Cache Charon v7: pipeline, voz, idioma, velocidade, modelo, estilo e texto compõem a chave. */
public final class TtsCache {
    private static final String CACHE_VERSION="tts-v8";
    private static final String LANGUAGE="pt-BR";
    private static final String SPEED="1.0";
    private static final String STYLE=NarrationPlaybackPolicy.STYLE_ID;
    private static final int MAX_ENTRIES=72;
    private static final long MAX_BYTES=180L*1024L*1024L;
    private final File root;

    public TtsCache(Context context){
        root=new File(context.getFilesDir(),"tts_cache_v8");
        if(!root.exists())root.mkdirs();
        cleanupPartials();
    }

    public File get(String model,String voice,String text){return get(model,voice,text,"");}

    public File get(String model,String voice,String text,String direction){
        File f=new File(root,key(model,voice,text,direction)+".wav");
        if(f.isFile()&&f.length()>128){f.setLastModified(System.currentTimeMillis());return f;}
        return null;
    }

    public File store(String model,String voice,String text,File source)throws Exception{return store(model,voice,text,"",source);}

    public File store(String model,String voice,String text,String direction,File source)throws Exception{
        File dest=new File(root,key(model,voice,text,direction)+".wav");if(dest.equals(source))return dest;
        File temp=new File(root,dest.getName()+".partial");
        try(FileInputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(temp)){
            byte[] b=new byte[65536];int n;while((n=in.read(b))>0)out.write(b,0,n);out.getFD().sync();
        }
        if(dest.exists()&&!dest.delete())throw new Exception("Não foi possível substituir a voz em cache.");
        if(!temp.renameTo(dest)){copy(temp,dest);temp.delete();}
        dest.setLastModified(System.currentTimeMillis());prune();return dest;
    }

    public long bytes(){long total=0;File[] fs=root.listFiles();if(fs!=null)for(File f:fs)if(!f.getName().endsWith(".partial"))total+=f.length();return total;}
    public int entries(){File[] fs=root.listFiles();if(fs==null)return 0;int n=0;for(File f:fs)if(f.isFile()&&!f.getName().endsWith(".partial"))n++;return n;}
    public void clear(){File[] fs=root.listFiles();if(fs!=null)for(File f:fs)f.delete();}

    private void prune(){
        File[] fs=root.listFiles();if(fs==null)return;
        Arrays.sort(fs,new Comparator<File>(){public int compare(File a,File b){return Long.compare(a.lastModified(),b.lastModified());}});
        long total=bytes();int count=entries();
        for(File f:fs){
            if(f.getName().endsWith(".partial")){f.delete();continue;}
            if(total<=MAX_BYTES&&count<=MAX_ENTRIES)break;
            long len=f.length();if(f.delete()){total-=len;count--;}
        }
    }

    private void cleanupPartials(){File[] fs=root.listFiles();if(fs!=null)for(File f:fs)if(f.getName().endsWith(".partial"))f.delete();}
    private static void copy(File source,File target)throws Exception{try(FileInputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(target)){byte[] b=new byte[65536];int n;while((n=in.read(b))>0)out.write(b,0,n);out.getFD().sync();}}
    private static String key(String model,String voice,String text,String direction){
        String normalized=Normalizer.normalize(text==null?"":text,Normalizer.Form.NFC).replaceAll("\\s+"," ").trim();
        String directed=Normalizer.normalize(direction==null?"":direction,Normalizer.Form.NFC).replaceAll("\\s+"," ").trim();
        String material=CACHE_VERSION+"|"+(voice==null?"Charon":voice)+"|"+LANGUAGE+"|"+SPEED+"|"+(model==null?"":model)+"|"+STYLE+"|"+directed+"|"+normalized;
        try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] h=d.digest(material.getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format(Locale.US,"%02x",x));return b.toString();}catch(Exception e){return Integer.toHexString(material.hashCode());}
    }
}
