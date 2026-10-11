package com.braseiro.dnd5e;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Hash estável usado em contratos locais de integridade; não é segredo nem autenticação. */
public final class IntegrityHash {
    private IntegrityHash(){}
    public static String sha256(String value){
        try{
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            byte[] bytes=d.digest((value==null?"":value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(bytes.length*2);
            for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b&0xff));
            return out.toString();
        }catch(Exception e){throw new IllegalStateException("SHA-256 indisponível.",e);}
    }
}
