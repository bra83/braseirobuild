package br.com.tutorclinico;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Versioned portable backup transport. Each bounded frame is AES-256-GCM
 * authenticated with the header, sequence and length as AAD. The authenticated
 * empty final frame and required EOF detect truncation and appended data.
 * Callers must stage or transactionally restore data until read() succeeds.
 * No plaintext temporary files are required by this transport.
 */
public final class EcoBackupStream {
    private static final byte[] MAGIC={'E','C','O','B','K','P',2,0};
    public static final int FRAME_BYTES=256*1024;
    private static final int TAG_BYTES=16, ITERATIONS=240000;
    private static final int HEADER_BYTES=MAGIC.length+16+4;
    private static final SecureRandom RANDOM=new SecureRandom();
    private EcoBackupStream(){}

    private static SecretKeySpec key(char[] password,byte[] header)throws Exception{
        if(password==null||password.length<10)throw new IllegalArgumentException("Senha do backup: mínimo de 10 caracteres");
        byte[] salt=Arrays.copyOfRange(header,MAGIC.length,MAGIC.length+16);
        PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);
        byte[] raw=null;
        try{
            raw=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            if(raw==null||raw.length!=32)throw new SecurityException("Chave de backup inválida");
            return new SecretKeySpec(raw,"AES");
        }finally{spec.clearPassword();Arrays.fill(salt,(byte)0);if(raw!=null)Arrays.fill(raw,(byte)0);}
    }
    private static byte[] crypt(int mode,SecretKeySpec key,byte[] header,long sequence,int size,byte[] input,int count)throws Exception{
        if(sequence<0)throw new IOException("Sequência excedida");
        byte[] nonce=ByteBuffer.allocate(12).put(header,HEADER_BYTES-4,4).putLong(sequence).array();
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode,key,new GCMParameterSpec(128,nonce));
        cipher.updateAAD(header);
        cipher.updateAAD(ByteBuffer.allocate(12).putLong(sequence).putInt(size).array());
        return cipher.doFinal(input,0,count);
    }
    public static boolean matchesMagic(byte[] bytes){
        return bytes!=null&&bytes.length>=MAGIC.length&&Arrays.equals(MAGIC,Arrays.copyOf(bytes,MAGIC.length));
    }
    /** Explicit finish authenticates successful completion; close alone never does. */
    public static final class Writer extends OutputStream {
        private final DataOutputStream out;
        private final byte[] header=new byte[HEADER_BYTES], buffer=new byte[FRAME_BYTES];
        private final SecretKeySpec key;
        private int used=0;
        private long sequence=0,total=0;
        private boolean closed=false,finished=false,failed=false;
        public Writer(OutputStream target,char[] password)throws Exception{
            RANDOM.nextBytes(header);System.arraycopy(MAGIC,0,header,0,MAGIC.length);
            key=key(password,header);out=new DataOutputStream(target);out.write(header);
        }
        private void check()throws IOException{if(closed||finished||failed)throw new IOException("Backup encerrado");}
        private void frame()throws IOException{
            try{
                out.writeInt(used);out.write(crypt(Cipher.ENCRYPT_MODE,key,header,sequence++,used,buffer,used));
                total=Math.addExact(total,used);Arrays.fill(buffer,0,used,(byte)0);used=0;
            }catch(Exception e){failed=true;throw new IOException("Falha ao cifrar bloco",e);}
        }
        @Override public void write(int b)throws IOException{check();buffer[used++]=(byte)b;if(used==buffer.length)frame();}
        @Override public void write(byte[] b,int off,int len)throws IOException{
            check();if(off<0||len<0||off>b.length-len)throw new IndexOutOfBoundsException();
            while(len>0){int n=Math.min(len,buffer.length-used);System.arraycopy(b,off,buffer,used,n);used+=n;off+=n;len-=n;if(used==buffer.length)frame();}
        }
        public long finish()throws IOException{
            check();if(used>0)frame();
            try{frame();out.flush();finished=true;return total;}
            catch(IOException e){failed=true;throw e;}
        }
        @Override public void close(){closed=true;Arrays.fill(buffer,(byte)0);}
    }
    /** Reader authenticates EOF only when consumed through its terminal frame. */
    public static final class Reader extends InputStream {
        private final DataInputStream in;
        private final byte[] header=new byte[HEADER_BYTES];
        private final SecretKeySpec key;
        private final long maxPlaintext;
        private byte[] clear=new byte[0];
        private int position=0;
        private long sequence=0,total=0;
        private boolean ended=false,closed=false,failed=false;
        public Reader(InputStream source,char[] password,long maxPlaintext)throws Exception{
            if(maxPlaintext<0)throw new IllegalArgumentException("Limite inválido");
            this.maxPlaintext=maxPlaintext;in=new DataInputStream(source);in.readFully(header);
            if(!matchesMagic(header))throw new IOException("Formato de backup não reconhecido");
            key=key(password,header);
        }
        private boolean next()throws IOException{
            if(closed||failed)throw new IOException("Backup encerrado");
            if(position<clear.length)return true;
            Arrays.fill(clear,(byte)0);clear=new byte[0];position=0;
            if(ended)return false;
            try{
                int size=in.readInt();
                if(size<0||size>FRAME_BYTES||size>maxPlaintext-total)throw new IOException("Bloco de backup inválido ou limite excedido");
                byte[] encrypted=new byte[size+TAG_BYTES];in.readFully(encrypted);
                clear=crypt(Cipher.DECRYPT_MODE,key,header,sequence++,size,encrypted,encrypted.length);
                if(clear.length!=size)throw new SecurityException("Tamanho autenticado divergente");
                if(size==0){if(in.read()!=-1)throw new IOException("Dados após o final autenticado");ended=true;return false;}
                total+=size;return true;
            }catch(Exception e){failed=true;Arrays.fill(clear,(byte)0);throw new IOException("Backup inválido, senha incorreta ou arquivo incompleto",e);}
        }
        @Override public int read()throws IOException{return next()?clear[position++]&255:-1;}
        @Override public int read(byte[] b,int off,int len)throws IOException{
            if(off<0||len<0||off>b.length-len)throw new IndexOutOfBoundsException();if(len==0)return 0;
            if(!next())return -1;int n=Math.min(len,clear.length-position);
            System.arraycopy(clear,position,b,off,n);position+=n;return n;
        }
        @Override public void close(){closed=true;Arrays.fill(clear,(byte)0);}
    }
    /** Caller owns both underlying streams. */
    public static long write(InputStream source,OutputStream target,char[] password)throws Exception{
        byte[] buffer=new byte[FRAME_BYTES];
        try(Writer out=new Writer(target,password)){
            int n;while((n=source.read(buffer))!=-1){
                if(n==0){int value=source.read();if(value<0)break;out.write(value);}
                else out.write(buffer,0,n);
            }
            return out.finish();
        }finally{Arrays.fill(buffer,(byte)0);}
    }
    public static long read(InputStream source,OutputStream target,char[] password,long maxPlaintext)throws Exception{
        byte[] buffer=new byte[FRAME_BYTES];long total=0;
        try(Reader in=new Reader(source,password,maxPlaintext)){
            int n;while((n=in.read(buffer))!=-1){target.write(buffer,0,n);total+=n;}
            target.flush();return total;
        }finally{Arrays.fill(buffer,(byte)0);}
    }
}
