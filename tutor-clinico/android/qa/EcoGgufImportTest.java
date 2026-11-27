package br.com.tutorclinico;
import java.io.*;
import java.security.MessageDigest;
import java.util.zip.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public final class EcoGgufImportTest {
 static byte[] fixture=new byte[8192];
 static String sha;
 static byte[] zip(String name,byte[] data)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(ZipOutputStream z=new ZipOutputStream(bytes)){
   z.putNextEntry(new ZipEntry("ATTRIBUTION.txt"));z.write("synthetic".getBytes());z.closeEntry();
   z.putNextEntry(new ZipEntry(name));z.write(data);z.closeEntry();
  }
  return bytes.toByteArray();
 }
 static byte[] run(byte[] bytes,long free,BooleanSupplier active)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  long count=EcoGgufImport.copy(new ByteArrayInputStream(bytes),out,sha,fixture.length,fixture.length,128,()->free,active,n->{});
  if(count!=fixture.length)throw new AssertionError("count");return out.toByteArray();
 }
 static void mustFail(byte[] bytes,long free,String expected)throws Exception{
  try{run(bytes,free,()->true);throw new AssertionError("accepted "+expected);}
  catch(EcoGgufImport.Refused e){if(!e.getMessage().contains(expected))throw new AssertionError(e);}
 }
 public static void main(String[] args)throws Exception{
  fixture[0]='G';fixture[1]='G';fixture[2]='U';fixture[3]='F';fixture[500]=7;
  sha=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fixture));
  if(!java.util.Arrays.equals(run(fixture,100000,()->true),fixture))throw new AssertionError("raw");
  if(!java.util.Arrays.equals(run(zip("build/eco-advanced-model/"+EcoGgufImport.ENTRY,fixture),100000,()->true),fixture))throw new AssertionError("zip");
  mustFail("APK!".getBytes(),100000,"não é GGUF");
  mustFail(zip("other.gguf",fixture),100000,"não contém");
  mustFail(java.util.Arrays.copyOf(fixture,100),100000,"incompleto");
  byte[] corrupt=fixture.clone();corrupt[100]=1;mustFail(corrupt,100000,"SHA-256");
  mustFail(fixture,10,"Espaço interno");
  byte[] oversized=java.util.Arrays.copyOf(fixture,fixture.length+1);mustFail(oversized,100000,"acima do tamanho");
  try{run(fixture,100000,()->false);throw new AssertionError("cancel");}catch(CancellationException expected){}
  System.out.println("PASS GGUF raw/ZIP streaming, SHA, size, format, capacity and cancellation");
 }
}
