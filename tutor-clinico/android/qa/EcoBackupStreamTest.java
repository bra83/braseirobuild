package br.com.tutorclinico;
import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;
public final class EcoBackupStreamTest {
    static final char[] PASS="eco-teste-sintetico-2026".toCharArray();
    static final int SIZE=EcoBackupStream.FRAME_BYTES*201+123;
    static final class Pattern extends InputStream {
        int pos=0;
        @Override public int read(){return pos>=SIZE?-1:((pos++*31+7)&255);}
        @Override public int read(byte[] b,int off,int len){
            if(pos>=SIZE)return -1;
            int n=Math.min(len,SIZE-pos);for(int i=0;i<n;i++)b[off+i]=(byte)(pos++*31+7);return n;
        }
    }
    static final class DigestSink extends OutputStream {
        final MessageDigest digest;
        DigestSink()throws Exception{digest=MessageDigest.getInstance("SHA-256");}
        public void write(int v){digest.update((byte)v);}
        public void write(byte[] b,int off,int n){digest.update(b,off,n);}
    }
    interface Check{void run()throws Exception;}
    static void reject(Check action)throws Exception{
        try{action.run();}catch(Exception expected){return;}throw new AssertionError("Arquivo inválido aceito");
    }
    static void check(boolean result){if(!result)throw new AssertionError("Falha na integridade");}
    static byte[] encode(byte[] data)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        EcoBackupStream.write(new ByteArrayInputStream(data),out,PASS);return out.toByteArray();
    }
    static void decode(byte[] data,char[] pass,long limit)throws Exception{
        EcoBackupStream.read(new ByteArrayInputStream(data),new DigestSink(),pass,limit);
    }
    public static void main(String[] args)throws Exception{
        File file=File.createTempFile("eco-backup-fixture",".bin");
        try{
            try(OutputStream out=new BufferedOutputStream(new FileOutputStream(file))){
                check(EcoBackupStream.write(new Pattern(),out,PASS)==SIZE);
            }
            DigestSink expected=new DigestSink();byte[] b=new byte[8192];
            try(InputStream in=new Pattern()){int n;while((n=in.read(b))!=-1)expected.write(b,0,n);}
            DigestSink actual=new DigestSink();
            try(InputStream in=new BufferedInputStream(new FileInputStream(file))){
                check(EcoBackupStream.read(in,actual,PASS,SIZE)==SIZE);
            }
            check(Arrays.equals(expected.digest.digest(),actual.digest.digest()));
        }finally{check(file.delete());}
        byte[] small=encode(new byte[EcoBackupStream.FRAME_BYTES+17]);
        check(EcoBackupStream.matchesMagic(small));
        byte[] corrupt=small.clone();corrupt[80]^=1;
        reject(()->decode(corrupt,PASS,Long.MAX_VALUE));
        byte[] header=small.clone();header[11]^=1;
        reject(()->decode(header,PASS,Long.MAX_VALUE));
        reject(()->decode(Arrays.copyOf(small,small.length-1),PASS,Long.MAX_VALUE));
        reject(()->decode(Arrays.copyOf(small,small.length+1),PASS,Long.MAX_VALUE));
        reject(()->decode(small,"senha-errada-2026".toCharArray(),Long.MAX_VALUE));
        reject(()->decode(small,PASS,3));
        byte[] empty=encode(new byte[0]);decode(empty,PASS,0);
        byte[] frameLength=small.clone();Arrays.fill(frameLength,28,32,(byte)127);
        reject(()->decode(frameLength,PASS,Long.MAX_VALUE));
        // The same plaintext produces independently salted ciphertexts.
        check(!Arrays.equals(empty,encode(new byte[0])));
        System.out.println("PASS backup transport: 50+ MiB under bounded heap, integrity, wrong password, tampering, truncation, trailing bytes, resource limit");
    }
}
