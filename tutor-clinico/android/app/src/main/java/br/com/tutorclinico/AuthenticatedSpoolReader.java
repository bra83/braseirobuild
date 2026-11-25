package br.com.tutorclinico;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Production ECO2 reader. A positive result means a verified terminal trailer;
 * a negative result means an explicitly requested authenticated prefix of an
 * interrupted capture. Zero means cancellation. No partial path is a full EOF. */
final class AuthenticatedSpoolReader {
    interface Consumer { void accept(byte[] pcm, int length) throws Exception; }
    private static final int CHUNK = 8192;
    private static final long MAX_BYTES = 120L * 60L * 32000L;
    private static final byte[] EOF_TOKEN = "ECO2_AUTHENTICATED_EOF".getBytes(StandardCharsets.UTF_8);

    private static long le(DataInputStream in, int bytes) throws Exception {
        long n=0;
        for(int i=0;i<bytes;i++) n|=(long)in.readUnsignedByte()<<(8*i);
        return n;
    }
    private static int interrupted(boolean allow, int chunks) throws EOFException {
        if(allow && chunks>0) return -chunks;
        throw new EOFException("Gravação sem trailer final autenticado");
    }
    static int read(InputStream source, byte[] key, BooleanSupplier active,
                    boolean allowInterrupted, Consumer receiver) throws Exception {
        return readInternal(source,key,active,allowInterrupted,receiver,null);
    }
    static int readLive(LiveSpoolInput source,byte[] key,BooleanSupplier active,Consumer receiver) throws Exception{
        return readInternal(new java.io.BufferedInputStream(source),key,active,true,receiver,source);
    }
    private static int readInternal(InputStream source,byte[] key,BooleanSupplier active,
                    boolean allowInterrupted,Consumer receiver,LiveSpoolInput live) throws Exception{
        if(key==null||key.length!=32) throw new SecurityException("Chave de áudio inválida");
        DataInputStream in=new DataInputStream(source);
        byte[] magic=new byte[4];in.readFully(magic);
        if(!Arrays.equals(magic,new byte[]{'E','C','O','2'})||le(in,4)!=2||le(in,4)!=CHUNK)
            throw new SecurityException("Cabeçalho ECO2 inválido");
        byte[] reserved=new byte[12];in.readFully(reserved);
        if(!Arrays.equals(reserved,new byte[12]))throw new SecurityException("Cabeçalho reservado adulterado");
        long expected=le(in,8);
        if(expected<0||expected>MAX_BYTES||(expected&1)!=0)throw new SecurityException("Comprimento ECO2 inválido");
        int chunks=0;long bytes=0;
        while(active.getAsBoolean()){
            long index;int length;byte[] nonce=new byte[12], ciphertext;
            try {
                index=le(in,4);long len=le(in,4);
                if(index==0xffffffffL){if(len!=EOF_TOKEN.length)throw new SecurityException("Trailer inválido");}
                else if(index!=chunks||len<=0||len>CHUNK||(len&1)!=0||bytes+len>MAX_BYTES)
                    throw new SecurityException("Bloco ECO2 inválido");
                length=(int)len;ciphertext=new byte[length+16];
                in.readFully(nonce);in.readFully(ciphertext);
            }catch(EOFException e){return interrupted(allowInterrupted,chunks);}
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));
            String aad=index==0xffffffffL?"ECO2:EOF:"+index+":"+chunks+":"+bytes:"ECO2:"+index+":"+length;
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] plain=cipher.doFinal(ciphertext); // bad authentication is never salvageable as an EOF
            try {
                if(index==0xffffffffL){
                    if(!Arrays.equals(plain,EOF_TOKEN)||bytes!=(live==null?expected:live.finalHeaderLength())||chunks==0||in.read()!=-1)
                        throw new SecurityException("Final ECO2 adulterado");
                    return active.getAsBoolean()?chunks:0;
                }
                if(plain.length!=length)throw new SecurityException("Bloco incompleto");
                if(!active.getAsBoolean())return 0;
                receiver.accept(plain,length);
                chunks++;bytes+=length;
            }finally{Arrays.fill(plain,(byte)0);}
        }
        return 0;
    }
}
