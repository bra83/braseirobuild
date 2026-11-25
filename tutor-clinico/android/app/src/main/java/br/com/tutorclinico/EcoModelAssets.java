package br.com.tutorclinico;
import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.concurrent.CancellationException;
final class EcoModelAssets {
 static boolean available(Context c,String path){try(android.content.res.AssetFileDescriptor f=c.getAssets().openFd(path)){return f.getLength()>0;}catch(IOException e){return false;}}
 static String digest(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();}
 static File materialize(Context c,String asset,String name,String sha,BooleanSupplier active)throws Exception{
  long size;try(android.content.res.AssetFileDescriptor fd=c.getAssets().openFd(asset)){size=fd.getLength();}
  File f=new File(c.getFilesDir(),name);if(f.isFile()&&f.length()==size&&sha.equals(digest(f)))return f;
  File tmp=new File(f.getPath()+".tmp");
  try{try(InputStream in=c.getAssets().open(asset);OutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(!active.getAsBoolean())throw new CancellationException();out.write(b,0,n);}}
   if(tmp.length()!=size||!sha.equals(digest(tmp))||!tmp.renameTo(f))throw new IOException("Integridade de modelo");
   return f;
  }finally{tmp.delete();}
 }
}
