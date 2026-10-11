package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Segredos locais protegidos pelo Android Keystore.
 *
 * A partir da 3.8.13, o material cifrado fica em um arquivo de preferências
 * separado, explicitamente excluído dos backups do Android. Valores legados
 * são migrados sem expor o texto puro em disco.
 */
public final class SecureSecretStore {
    private static final String KEY_ALIAS="braseiro_local_secrets_v1";
    private static final String PREFS_NAME="secure_secrets";
    private static final String PREFIX="secure_";
    private final SharedPreferences prefs;
    private final SharedPreferences legacyPrefs;

    public SecureSecretStore(Context context,SharedPreferences legacy){
        Context app=context.getApplicationContext();
        prefs=app.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        legacyPrefs=legacy;
        migrateEncryptedLegacyValues();
    }

    public boolean put(String name,String value){/*BR_JSON_WRAP*/try{
        String safeName=normalizeName(name);
        if(safeName.length()==0)return false;
        try{
            if(value==null||value.length()==0){
                boolean ok=prefs.edit().remove(PREFIX+safeName).commit();
                if(legacyPrefs!=null)legacyPrefs.edit().remove(PREFIX+safeName).remove(safeName).apply();
                return ok;
            }
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
            byte[] iv=cipher.getIV(),encrypted=cipher.doFinal(value.getBytes("UTF-8"));
            ByteBuffer data=ByteBuffer.allocate(4+iv.length+encrypted.length);data.putInt(iv.length);data.put(iv);data.put(encrypted);
            boolean ok=prefs.edit().putString(PREFIX+safeName,Base64.encodeToString(data.array(),Base64.NO_WRAP)).commit();
            if(ok&&legacyPrefs!=null)legacyPrefs.edit().remove(PREFIX+safeName).remove(safeName).apply();
            return ok;
        }catch(Exception e){AppLog.e("SecureSecretStore","Falha ao proteger o segredo '"+safeName+"'.",e);return false;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public String get(String name){
        String safeName=normalizeName(name);if(safeName.length()==0)return "";
        try{
            String raw=prefs.getString(PREFIX+safeName,"");
            if(raw.length()==0&&legacyPrefs!=null){
                String legacy=legacyPrefs.getString(PREFIX+safeName,"");
                if(legacy.length()>0&&prefs.edit().putString(PREFIX+safeName,legacy).commit()){
                    legacyPrefs.edit().remove(PREFIX+safeName).apply();raw=legacy;
                }
            }
            if(raw.length()==0)return "";
            ByteBuffer data=ByteBuffer.wrap(Base64.decode(raw,Base64.NO_WRAP));
            if(data.remaining()<5)return "";
            int ivLength=data.getInt();
            if(ivLength<12||ivLength>32||data.remaining()<=ivLength)return "";
            byte[] iv=new byte[ivLength];data.get(iv);byte[] encrypted=new byte[data.remaining()];data.get(encrypted);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
            return new String(cipher.doFinal(encrypted),"UTF-8");
        }catch(Exception e){AppLog.w("SecureSecretStore","O segredo '"+safeName+"' não pôde ser lido; nenhum valor vazio será gravado por causa disso.",e);return "";}
    }

    public boolean has(String name){return get(name).length()>0;}

    public void migratePlaintext(String name){
        if(legacyPrefs==null)return;String safeName=normalizeName(name);if(safeName.length()==0)return;
        String plain=legacyPrefs.getString(safeName,"");
        if(plain.length()>0&&put(safeName,plain))legacyPrefs.edit().remove(safeName).apply();
    }

    /** Exercita geração, cifra, leitura e remoção sem tocar na chave do usuário. */
    public boolean selfTest(){
        String name="__selftest";String value="ok-"+System.nanoTime();
        boolean stored=put(name,value);String read=stored?get(name):"";boolean removed=put(name,"");
        return stored&&value.equals(read)&&removed;
    }

    private void migrateEncryptedLegacyValues(){
        if(legacyPrefs==null)return;
        try{
            SharedPreferences.Editor add=prefs.edit(),remove=legacyPrefs.edit();boolean changed=false;
            for(Map.Entry<String,?> entry:legacyPrefs.getAll().entrySet()){
                String key=entry.getKey();Object value=entry.getValue();
                if(key==null||!key.startsWith(PREFIX)||!(value instanceof String)||((String)value).length()==0)continue;
                if(!prefs.contains(key))add.putString(key,(String)value);
                remove.remove(key);changed=true;
            }
            if(changed&&add.commit())remove.apply();
        }catch(Exception e){AppLog.w("SecureSecretStore","Migração de segredos cifrados legados não concluída.",e);}
    }

    private static String normalizeName(String name){return name==null?"":name.trim().replaceAll("[^A-Za-z0-9_.-]","_");}

    private SecretKey key()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        java.security.Key existing=store.getKey(KEY_ALIAS,null);if(existing instanceof SecretKey)return (SecretKey)existing;
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
}
