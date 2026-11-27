package br.com.tutorclinico;

import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Bounded streaming import of the official raw GGUF or GitHub ZIP.
 * Nothing is extracted by pathname. Only the expected model bytes are copied.
 */
final class EcoGgufImport {
 static final String ENTRY="qwen2.5-1.5b-instruct-q4_k_m.gguf";
 static final long MIN=1_000_000_001L, MAX=1_200_000_000L, RESERVE=128L*1024*1024;
 static final class Refused extends IOException {Refused(String message){super(message);}}
 private static void check(BooleanSupplier active){if(!active.getAsBoolean())throw new CancellationException();}
 static long copy(InputStream raw,OutputStream out,String sha,long min,long max,long reserve,LongSupplier free,BooleanSupplier active,LongConsumer progress)throws Exception{
  check(active);
  BufferedInputStream input=new BufferedInputStream(raw,65536);
  input.mark(4);byte[] signature=firstFour(input);input.reset();
  if(signature.length!=4)throw new Refused("Arquivo vazio ou incompleto.");
  if(signature[0]=='P'&&signature[1]=='K'&&signature[2]==3&&signature[3]==4){
   try(ZipInputStream zip=new ZipInputStream(input)){
    ZipEntry entry;int entries=0;long metadata=0;byte[] skip=new byte[8192];
    while((entry=zip.getNextEntry())!=null){
     check(active);if(++entries>32)throw new Refused("ZIP contém entradas demais.");
     String name=entry.getName().replace('\\','/');String basename=name.substring(name.lastIndexOf('/')+1);
     if(!entry.isDirectory()&&ENTRY.equalsIgnoreCase(basename)){
      long copied=copyModel(zip,out,sha,min,max,reserve,free,active,progress);
      zip.closeEntry();return copied;
     }
     int n;while((n=zip.read(skip))!=-1){check(active);metadata+=n;if(metadata>2L*1024*1024)throw new Refused("ZIP não corresponde ao pacote oficial de IA.");}
     zip.closeEntry();
    }
    throw new Refused("O ZIP não contém o GGUF oficial Qwen 1.5B Q4_K_M.");
   }catch(java.util.zip.ZipException e){throw new Refused("ZIP inválido ou download incompleto.");}
  }
  return copyModel(input,out,sha,min,max,reserve,free,active,progress);
 }
 private static byte[] firstFour(InputStream in)throws IOException{byte[] b=new byte[4];int pos=0;while(pos<4){int n=in.read(b,pos,4-pos);if(n<0)break;if(n>0)pos+=n;}return pos==4?b:java.util.Arrays.copyOf(b,pos);}
 private static long copyModel(InputStream in,OutputStream out,String sha,long min,long max,long reserve,LongSupplier free,BooleanSupplier active,LongConsumer progress)throws Exception{
  check(active);
  byte[] header=firstFour(in);
  if(header.length!=4||header[0]!='G'||header[1]!='G'||header[2]!='U'||header[3]!='F')throw new Refused("Arquivo não é GGUF. Selecione o GGUF ou o ZIP oficial do Qwen, não o APK.");
  if(free.getAsLong()<min+reserve)throw new Refused("Espaço interno insuficiente. Libere pelo menos 1,4 GB além do arquivo baixado.");
  MessageDigest md=MessageDigest.getInstance("SHA-256");md.update(header);out.write(header);
  long count=4;byte[] buffer=new byte[1024*1024];int n;
  while((n=in.read(buffer))!=-1){
   check(active);if(n==0)continue;
   if(count>max-n)throw new Refused("GGUF acima do tamanho permitido.");
   if(free.getAsLong()<reserve+n)throw new Refused("Espaço interno insuficiente durante a cópia.");
   md.update(buffer,0,n);out.write(buffer,0,n);count+=n;progress.accept(count);
  }
  check(active);
  StringBuilder hex=new StringBuilder();for(byte b:md.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
  if(count<min)throw new Refused("GGUF incompleto ou modelo diferente do Qwen 1.5B.");
  if(!sha.equals(hex.toString()))throw new Refused("SHA-256 diferente do modelo oficial. Baixe novamente o pacote Qwen 1.5B Q4_K_M.");
  return count;
 }
}
