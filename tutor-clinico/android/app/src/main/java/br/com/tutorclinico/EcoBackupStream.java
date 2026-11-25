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
    /** Caller owns both streams; they are not closed here. */
    public static long write(InputStream source,OutputStream target,char[] password)throws Exception{
        byte[] header=new byte[HEADER_BYTES];RANDOM.nextBytes(header);
        System.arraycopy(MAGIC,0,header,0,MAGIC.length);
        SecretKeySpec key=key(password,header);
        DataOutputStream out=new DataOutputStream(target);
        byte[] buffer=new byte[FRAME_BYTES];long sequence=0,total=0;
        try{
            out.write(header);
            int count;
            while((count=source.read(buffer))!=-1){
                if(count==0){int value=source.read();if(value<0)break;buffer[0]=(byte)value;count=1;}
                out.writeInt(count);out.write(crypt(Cipher.ENCRYPT_MODE,key,header,sequence++,count,buffer,count));
                total=Math.addExact(total,count);
            }
            out.writeInt(0);out.write(crypt(Cipher.ENCRYPT_MODE,key,header,sequence,0,buffer,0));out.flush();
            return total;
        }finally{Arrays.fill(buffer,(byte)0);}
    }
    /**
     * Only authenticated frames reach the target. A successful return additionally
     * authenticates the final frame and EOF. Target must not commit on failure.
     * The caller defines a resource budget; no allocation depends on archive size.
     */
    public static long read(InputStream source,OutputStream target,char[] password,long maxPlaintext)throws Exception{
        if(maxPlaintext<0)throw new IllegalArgumentException("Limite inválido");
        DataInputStream in=new DataInputStream(source);
        byte[] header=new byte[HEADER_BYTES];in.readFully(header);
        if(!matchesMagic(header))throw new IOException("Formato de backup não reconhecido");
        SecretKeySpec key=key(password,header);
        long sequence=0,total=0;
        while(true){
            int size=in.readInt();
            if(size<0||size>FRAME_BYTES||size>maxPlaintext-total)throw new IOException("Bloco de backup inválido ou limite excedido");
            byte[] encrypted=new byte[size+TAG_BYTES];in.readFully(encrypted);
            byte[] clear=crypt(Cipher.DECRYPT_MODE,key,header,sequence++,size,encrypted,encrypted.length);
            try{
                if(clear.length!=size)throw new SecurityException("Tamanho autenticado divergente");
                if(size==0){
                    if(in.read()!=-1)throw new IOException("Dados após o final autenticado");
                    target.flush();return total;
                }
                target.write(clear);total+=size;
            }finally{Arrays.fill(clear,(byte)0);}
        }
    }
}
