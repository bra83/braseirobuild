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
 // In-process verification memo: immutable app-private model files only, never patient data.
 private static final java.util.Map<String,String> verified=new java.util.HashMap<>();
 private static String fingerprint(File f,String sha)throws Exception{
  android.system.StructStat st=android.system.Os.stat(f.getAbsolutePath());
  return sha+"|"+st.st_dev+"|"+st.st_ino+"|"+st.st_size+"|"+st.st_mtime+"|"+st.st_ctime;
 }
 private static String digestActive(File f,BooleanSupplier active)throws Exception{
  MessageDigest d=MessageDigest.getInstance("SHA-256");
  try(InputStream in=new FileInputStream(f)){byte[] b=new byte[262144];int n;while((n=in.read(b))!=-1){if(!active.getAsBoolean())throw new CancellationException();d.update(b,0,n);}}
  StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();
 }
 static synchronized File verifyInstalled(File file,String sha,BooleanSupplier active)throws Exception{
  if(!active.getAsBoolean())throw new CancellationException();
  if(!file.isFile())throw new IOException("Modelo não instalado");
  String key=file.getCanonicalPath(),before=fingerprint(file,sha);
  if(before.equals(verified.get(key)))return file;
  verified.remove(key);
  if(!sha.equals(digestActive(file,active))||!before.equals(fingerprint(file,sha)))throw new IOException("Integridade de modelo instalado");
  verified.put(key,before);return file;
 }
 static synchronized File materialize(Context c,String asset,String name,String sha,BooleanSupplier active)throws Exception{
  long size;try(android.content.res.AssetFileDescriptor fd=c.getAssets().openFd(asset)){size=fd.getLength();}
  if(!active.getAsBoolean())throw new CancellationException();
  File f=new File(c.getFilesDir(),name);String key=f.getCanonicalPath();
  if(f.isFile()&&f.length()==size){
   String before=fingerprint(f,sha);
   if(before.equals(verified.get(key)))return f;
   if(sha.equals(digestActive(f,active))&&before.equals(fingerprint(f,sha))){verified.put(key,before);return f;}
  }
  verified.remove(key);
  File tmp=new File(f.getPath()+".tmp");
  try{try(InputStream in=c.getAssets().open(asset);OutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[262144];int n;while((n=in.read(b))!=-1){if(!active.getAsBoolean())throw new CancellationException();out.write(b,0,n);}}
   if(tmp.length()!=size||!sha.equals(digestActive(tmp,active))||!active.getAsBoolean()||!tmp.renameTo(f))throw new IOException("Integridade de modelo");
   verified.put(key,fingerprint(f,sha));return f;
  }finally{tmp.delete();}
 }
}
