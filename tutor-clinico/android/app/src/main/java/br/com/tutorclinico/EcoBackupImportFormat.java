package br.com.tutorclinico;
import java.io.*;
final class EcoBackupImportFormat {
 enum Kind { LEGACY_JSON, INTEGRAL, ATTACHMENTS }
 static Kind detect(InputStream source,char[] password)throws Exception{
  BufferedInputStream in=new BufferedInputStream(source,8192);
  in.mark(8);byte[] prefix=new byte[8];int pos=0;
  while(pos<prefix.length){int n=in.read(prefix,pos,prefix.length-pos);if(n<0)break;if(n>0)pos+=n;}
  in.reset();
  if(EcoBackupStream.matchesMagic(prefix)){
   if(password==null||password.length<10)throw new IllegalArgumentException("Informe a senha original do backup integral (mínimo de 10 caracteres).");
   try(EcoBackupStream.Reader reader=new EcoBackupStream.Reader(in,password,8L*1024*1024*1024)){
    int kind=new DataInputStream(reader).readInt();
    if(kind==0x45564c31)return Kind.INTEGRAL;
    if(kind==0x45415431)return Kind.ATTACHMENTS;
    throw new IOException("Tipo de backup ECO não reconhecido.");
   }
  }
  int ch=in.read();
  if(ch==0xef){if(in.read()!=0xbb||in.read()!=0xbf)throw new IOException("Formato de backup não reconhecido.");ch=in.read();}
  int blanks=0;while(ch==' '||ch=='\t'||ch=='\r'||ch=='\n'){if(++blanks>8192)throw new IOException("Formato de backup não reconhecido.");ch=in.read();}
  if(ch=='{'||ch=='[')return Kind.LEGACY_JSON;
  throw new IOException("Formato de backup não reconhecido. Selecione .ecobkp, .psicbackup ou .json.");
 }
}
