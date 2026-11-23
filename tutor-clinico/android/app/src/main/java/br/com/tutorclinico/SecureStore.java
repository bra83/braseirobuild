package br.com.tutorclinico;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.AtomicFile;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

/** Offline-only AES-GCM vault. No network permission, no automatic Android backup. */
final class SecureStore {
    private static final String KEY_ALIAS = "tutorclinico-v1";
    private static final String FILE_NAME = "vault.aes";
    private static final SecureRandom RNG = new SecureRandom();
    private final Context context;
    SecureStore(Context context) { this.context = context.getApplicationContext(); }

    private SecretKey deviceKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry)entry).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    synchronized String read() throws Exception {
        File file = new File(context.getFilesDir(), FILE_NAME);
        AtomicFile atom = new AtomicFile(file);
        if (!atom.getBaseFile().exists() && !new File(context.getFilesDir(), FILE_NAME + ".bak").exists()) return "{\"version\":1,\"cases\":[],\"appointments\":[]}";
        byte[] bytes;
        try (FileInputStream stream = atom.openRead()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n=stream.read(buffer))!=-1) {
                if (out.size()+n>25000000) throw new IllegalStateException("Base muito grande");
                out.write(buffer,0,n);
            }
            bytes=out.toByteArray();
        }
        if (bytes.length < 30) throw new SecurityException("Arquivo de dados inválido");
        byte[] iv = Arrays.copyOfRange(bytes, 0, 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, deviceKey(), new GCMParameterSpec(128, iv));
        byte[] plaintext = cipher.doFinal(Arrays.copyOfRange(bytes, 12, bytes.length));
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    synchronized void save(String json) throws Exception {
        JSONObject validated = new JSONObject(json);
        if (validated.optInt("version") != 1 || !(validated.opt("cases") instanceof org.json.JSONArray)
                || !(validated.opt("appointments") instanceof org.json.JSONArray))
            throw new IllegalArgumentException("Estrutura de dados inválida");
        // AndroidKeyStore AES-GCM keys with randomized encryption require the provider to generate the IV.
        // Supplying a caller-created IV here raises "Caller-provided IV not permitted" on Android.
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey());
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length != 12) throw new IllegalStateException("IV AES-GCM inesperado");
        byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), FILE_NAME));
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            stream.write(iv); stream.write(encrypted);
            file.finishWrite(stream);
        } catch (Exception ex) {
            if (stream != null) file.failWrite(stream);
            throw ex;
        }
    }

    /**
     * Small independently encrypted autosave journal. Saves do NOT rewrite vault.aes.
     * JSON contains {caseId,session}; the Android Keystore key never leaves device.
     */
    synchronized void saveDraft(String json) throws Exception {
        JSONObject obj = new JSONObject(json);
        if (obj.optString("caseId").trim().isEmpty() || !(obj.opt("session") instanceof JSONObject))
            throw new IllegalArgumentException("Rascunho inválido");
        if (json.length() > 1_000_000) throw new IllegalArgumentException("Rascunho acima do limite");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey());
        byte[] iv=cipher.getIV();
        byte[] encoded=cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),"draft.aes"));
        FileOutputStream out=null;
        try {
            out=file.startWrite();
            out.write(iv);out.write(encoded);
            file.finishWrite(out);
        }catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }

    synchronized String readDraft() throws Exception {
        AtomicFile file=new AtomicFile(new File(context.getFilesDir(),"draft.aes"));
        if(!file.getBaseFile().exists()&&!new File(context.getFilesDir(),"draft.aes.bak").exists())return "";
        try(FileInputStream in=file.openRead()){
            ByteArrayOutputStream buf=new ByteArrayOutputStream();
            byte[] bytes=new byte[8192];int n;
            while((n=in.read(bytes))!=-1){
                if(buf.size()+n>2_000_000)throw new IllegalArgumentException("Rascunho muito grande");
                buf.write(bytes,0,n);
            }
            byte[] enc=buf.toByteArray();
            if(enc.length<30)throw new SecurityException("Rascunho cifrado inválido");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,deviceKey(),new GCMParameterSpec(128,Arrays.copyOfRange(enc,0,12)));
            return new String(cipher.doFinal(Arrays.copyOfRange(enc,12,enc.length)),StandardCharsets.UTF_8);
        }
    }

    synchronized void clearDraft() {
        new File(context.getFilesDir(),"draft.aes").delete();
        new File(context.getFilesDir(),"draft.aes.bak").delete();
        new File(context.getFilesDir(),"draft.aes.new").delete();
    }

    static String hashPin(String pin, String saltBase64) throws Exception {
        byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 180000, 256);
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.encodeToString(key, Base64.NO_WRAP);
        } finally { spec.clearPassword(); }
    }
    static String newSalt() {
        byte[] salt = new byte[16]; RNG.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }
    static boolean equalHash(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
    private static SecretKey passKey(String pass, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, 240000, 256);
        try {
            return new javax.crypto.spec.SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), "AES");
        } finally { spec.clearPassword(); }
    }
    static byte[] encryptBackup(String json, String pass) throws Exception {
        if (pass == null || pass.length() < 10) throw new IllegalArgumentException("Senha do backup: mínimo de 10 caracteres");
        byte[] salt = new byte[16], iv = new byte[12]; RNG.nextBytes(salt); RNG.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, passKey(pass, salt), new GCMParameterSpec(128, iv));
        JSONObject pkg = new JSONObject();
        pkg.put("format", "tutor-clinico-encrypted-backup");
        pkg.put("version", 1);
        pkg.put("kdf", "PBKDF2-HMAC-SHA256");
        pkg.put("iterations", 240000);
        pkg.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
        pkg.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
        pkg.put("ciphertext", Base64.encodeToString(cipher.doFinal(json.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP));
        return pkg.toString(2).getBytes(StandardCharsets.UTF_8);
    }
    static String decryptBackup(byte[] bytes, String pass) throws Exception {
        String input = new String(bytes, StandardCharsets.UTF_8).trim();
        if (input.startsWith("[")) return input; // Legacy backup from original website; UI labels unencrypted.
        JSONObject pkg = new JSONObject(input);
        if (!"tutor-clinico-encrypted-backup".equals(pkg.optString("format")) || pkg.optInt("version") != 1)
            throw new IllegalArgumentException("Formato não reconhecido");
        if (pkg.optInt("iterations") != 240000) throw new IllegalArgumentException("Parâmetros de derivação não suportados");
        byte[] salt = Base64.decode(pkg.getString("salt"), Base64.NO_WRAP);
        byte[] iv = Base64.decode(pkg.getString("iv"), Base64.NO_WRAP);
        if (salt.length != 16 || iv.length != 12) throw new IllegalArgumentException("Backup inválido");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, passKey(pass, salt), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(Base64.decode(pkg.getString("ciphertext"), Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
}
