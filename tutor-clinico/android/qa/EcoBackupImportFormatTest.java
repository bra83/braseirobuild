package br.com.tutorclinico;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class EcoBackupImportFormatTest {
 static final char[] PASS="test-password-123".toCharArray();
 static byte[] envelope(int magic)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(EcoBackupStream.Writer w=new EcoBackupStream.Writer(bytes,PASS)){new DataOutputStream(w).writeInt(magic);w.finish();}
  return bytes.toByteArray();
 }
 static void check(byte[] bytes,char[] pass,EcoBackupImportFormat.Kind expected)throws Exception{
  if(EcoBackupImportFormat.detect(new ByteArrayInputStream(bytes),pass)!=expected)throw new AssertionError(expected);
 }
 public static void main(String[] args)throws Exception{
  check(" { \"version\":1 }".getBytes(StandardCharsets.UTF_8),new char[0],EcoBackupImportFormat.Kind.LEGACY_JSON);
  check("\uFEFF []".getBytes(StandardCharsets.UTF_8),new char[0],EcoBackupImportFormat.Kind.LEGACY_JSON);
  byte[] integral=envelope(0x45564c31);
  check(integral,PASS,EcoBackupImportFormat.Kind.INTEGRAL);
  check(envelope(0x45415431),PASS,EcoBackupImportFormat.Kind.ATTACHMENTS);
  try{check(integral,"wrong-password-123".toCharArray(),EcoBackupImportFormat.Kind.INTEGRAL);throw new AssertionError("wrong password");}catch(IOException expected){}
  try{check("GGUF".getBytes(StandardCharsets.UTF_8),PASS,EcoBackupImportFormat.Kind.INTEGRAL);throw new AssertionError("wrong format");}catch(IOException expected){}
  System.out.println("PASS backup content routing, encrypted header, wrong password, BOM and wrong file");
 }
}
