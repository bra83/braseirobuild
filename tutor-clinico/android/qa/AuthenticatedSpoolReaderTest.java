package br.com.tutorclinico;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Executes the production reader, not a clone of its verification algorithm. */
public final class AuthenticatedSpoolReaderTest {
    private static final byte[] KEY = new byte[32]; // fictitious deterministic fixture only
    private static void le(ByteArrayOutputStream out,long v,int n){for(int i=0;i<n;i++)out.write((int)(v>>>(8*i))&255);}
    private static void frame(ByteArrayOutputStream out,long i,byte[] data,String aad,int nonceId)throws Exception{
        le(out,i,4);le(out,data.length,4);byte[] nonce=new byte[12];nonce[0]=(byte)nonceId;out.write(nonce);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(KEY,"AES"),new GCMParameterSpec(128,nonce));
        c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));out.write(c.doFinal(data));
    }
    private static byte[] fixture(boolean complete)throws Exception{
        ByteArrayOutputStream o=new ByteArrayOutputStream();o.write(new byte[]{'E','C','O','2'});le(o,2,4);le(o,8192,4);o.write(new byte[12]);le(o,complete?16384:0,8);
        byte[] a=new byte[8192],b=new byte[8192];Arrays.fill(a,(byte)1);Arrays.fill(b,(byte)2);
        frame(o,0,a,"ECO2:0:8192",1);frame(o,1,b,"ECO2:1:8192",2);
        if(complete)frame(o,0xffffffffL,"ECO2_AUTHENTICATED_EOF".getBytes(StandardCharsets.UTF_8),"ECO2:EOF:4294967295:2:16384",3);
        return o.toByteArray();
    }
    private static int read(byte[] source,byte[] key,boolean partial,ByteArrayOutputStream out)throws Exception{return AuthenticatedSpoolReader.read(new ByteArrayInputStream(source),key,()->true,partial,(pcm,n)->out.write(pcm,0,n));}
    private static void require(boolean b,String label){if(!b)throw new AssertionError(label);System.out.println("PASS "+label);}
    private static void reject(byte[] source,byte[] key,boolean partial,String label)throws Exception{boolean failed=false;try{read(source,key,partial,new ByteArrayOutputStream());}catch(IOException|SecurityException|javax.crypto.AEADBadTagException e){failed=true;}require(failed,label);}
    public static void main(String[] args)throws Exception{
        byte[] full=fixture(true), incomplete=fixture(false);ByteArrayOutputStream pcm=new ByteArrayOutputStream();
        require(read(full,KEY,false,pcm)==2&&pcm.size()==16384&&pcm.toByteArray()[0]==1&&pcm.toByteArray()[8192]==2,"real reader roundtrip two frames");
        reject(incomplete,KEY,false,"incomplete capture cannot authorize full EOF");
        pcm.reset();require(read(incomplete,KEY,true,pcm)==-2&&pcm.size()==16384,"explicit interrupted recovery yields negative proof and exact prefix");
        pcm.reset();require(read(Arrays.copyOf(incomplete,incomplete.length-10),KEY,true,pcm)==-1&&pcm.size()==8192,"torn frame excluded, authenticated first frame recovered");
        byte[] tamper=full.clone();tamper[45]^=1;reject(tamper,KEY,true,"authentication failure cannot be bypassed by recovery mode");
        byte[] wrong=KEY.clone();wrong[0]=7;reject(full,wrong,false,"wrong key rejected");
        byte[] trailing=Arrays.copyOf(full,full.length+1);reject(trailing,KEY,true,"bytes after authenticated EOF rejected");
        byte[] modified=full.clone();modified[24]=0;modified[25]=0;reject(modified,KEY,true,"altered final length rejected");
        require(AuthenticatedSpoolReader.read(new ByteArrayInputStream(full),KEY,()->false,false,(a,n)->{throw new AssertionError();})==0,"cancelled decode has no completion proof");
    }
}
