package br.com.tutorclinico;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import javax.crypto.spec.SecretKeySpec;

/**
 * Offline-only AES-GCM vault with relational ACID SQLite database.
 * No network permission, no automatic Android backup.
 */
final class SecureStore {
    private static final String KEY_ALIAS = "tutorclinico-v1";
    private static final String FILE_NAME = "vault.aes";
    private static final SecureRandom RNG = new SecureRandom();
    private final Context context;
    private final EcoDatabaseHelper dbHelper;

    SecureStore(Context context) {
        this(context, null);
    }

    EcoDatabaseHelper getDbHelper() {
        return dbHelper;
    }

    private final SecretKey injectedTestKey;

    // Package-private injection for tests only; production never falls back to a volatile key.
    SecureStore(Context context, SecretKey testKey) {
        this.context = context.getApplicationContext();
        this.injectedTestKey = testKey;
        this.dbHelper = new EcoDatabaseHelper(this.context, this);
    }

    synchronized SecretKey deviceKey() throws Exception {
        if (injectedTestKey != null) return injectedTestKey;
        synchronized (SecureStore.class) {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(KEY_ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build());
            return generator.generateKey();
        }
    }

    synchronized String read() throws Exception {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        JSONArray casesArray = new JSONArray();

        try (Cursor c = db.query("_pacientes", null, null, null, null, null, "created_at ASC")) {
            while (c.moveToNext()) {
                String caseId = c.getString(c.getColumnIndexOrThrow("id"));
                byte[] blob = c.getBlob(c.getColumnIndexOrThrow("payload_enc"));
                JSONObject caseObj = (blob != null && blob.length > 0)
                        ? new JSONObject(dbHelper.decrypt(blob))
                        : new JSONObject();
                caseObj.put("id", caseId);

                // Load sessions for this patient
                JSONArray sessoes = new JSONArray();
                try (Cursor sc = db.query("sessoes", null, "case_id = ?",
                        new String[]{caseId}, null, null, "created_at ASC")) {
                    while (sc.moveToNext()) {
                        String sId = sc.getString(sc.getColumnIndexOrThrow("id"));
                        byte[] sBlob = sc.getBlob(sc.getColumnIndexOrThrow("payload_enc"));
                        JSONObject sObj = (sBlob != null && sBlob.length > 0)
                                 ? new JSONObject(dbHelper.decrypt(sBlob))
                                : new JSONObject();
                        sObj.put("id", sId);
                        sObj.put("data", sc.getString(sc.getColumnIndexOrThrow("data")));
                        int hourCol = sc.getColumnIndexOrThrow("hora");
                        if (!sObj.has("hora") && !sc.isNull(hourCol)) sObj.put("hora", sc.getString(hourCol));
                        sObj.put("origem", sc.getString(sc.getColumnIndexOrThrow("origem")));

                        // Read committed segments from relational table segmentos
                        JSONArray segs = new JSONArray();
                        StringBuilder fullText = new StringBuilder();
                        try (Cursor segCursor = db.query("segmentos", null, "session_id = ? AND case_id = ?",
                                new String[]{sId, caseId}, null, null, "start_ms ASC")) {
                            while (segCursor.moveToNext()) {
                                JSONObject seg = new JSONObject();
                                seg.put("id", segCursor.getString(segCursor.getColumnIndexOrThrow("id")));
                                seg.put("startMs", segCursor.getLong(segCursor.getColumnIndexOrThrow("start_ms")));
                                seg.put("endMs", segCursor.getLong(segCursor.getColumnIndexOrThrow("end_ms")));
                                String role = segCursor.getString(segCursor.getColumnIndexOrThrow("speaker"));
                                seg.put("cluster", role.startsWith("falante") ? role : "");
                                seg.put("speaker", role.startsWith("falante") ? "desconhecido" : role);
                                seg.put("manualSpeaker", segCursor.getInt(segCursor.getColumnIndexOrThrow("speaker_confirmed")) == 1);
                                String segText = dbHelper.decrypt(segCursor.getBlob(segCursor.getColumnIndexOrThrow("text_enc")));
                                seg.put("text", segText);
                                segs.put(seg);
                                if (fullText.length() > 0) fullText.append(" ");
                                fullText.append(segText);
                            }
                        }
                        if (segs.length() > 0) {
                            sObj.put("segmentos", segs);
                            sObj.put("mlSegments", segs);
                            if (!sObj.has("mlTranscricao") || sObj.optString("mlTranscricao").isEmpty()) {
                                sObj.put("mlTranscricao", fullText.toString());
                            }
                            if (!sObj.has("texto") || sObj.optString("texto").isEmpty()) {
                                sObj.put("texto", fullText.toString());
                            }
                        }
                        sessoes.put(sObj);
                    }
                }
                caseObj.put("sessoes", sessoes);
                // STRICTLY REMOVE any private diary notes from public patient record:
                caseObj.remove("diarioNotas");
                casesArray.put(caseObj);
            }
        }

        JSONArray apptsArray = new JSONArray();
        try (Cursor c = db.query("agenda", null, null, null, null, null, "data ASC, hora ASC")) {
            while (c.moveToNext()) {
                byte[] blob = c.getBlob(c.getColumnIndexOrThrow("payload_enc"));
                if (blob != null && blob.length > 0) {
                    apptsArray.put(new JSONObject(dbHelper.decrypt(blob)));
                }
            }
        }

        JSONObject root = new JSONObject();
        // Reconstruct root metadata (theme, activeEngine, settings, etc.) from configuracoes unconditionally
        try (Cursor cfgCursor = db.query("configuracoes", new String[]{"valor_enc"}, "chave = ?", new String[]{"app_state_meta"}, null, null, null)) {
            if (cfgCursor.moveToFirst()) {
                byte[] blob = cfgCursor.getBlob(0);
                if (blob != null && blob.length > 0) {
                    JSONObject meta = new JSONObject(dbHelper.decrypt(blob));
                    java.util.Iterator<String> keys = meta.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        root.put(k, meta.get(k));
                    }
                }
            }
        } catch (Exception ignored) {}
        if (!root.has("version")) root.put("version", 1);
        root.put("cases", casesArray);
        root.put("appointments", apptsArray);

        // One-time legacy migration check
        boolean migrated = false;
        try (Cursor c = db.query("configuracoes", new String[]{"chave"}, "chave = ?", new String[]{"db_migrated"}, null, null, null)) {
            migrated = c.moveToFirst();
        } catch (Exception ignored) {}
        if (!migrated && casesArray.length() == 0) {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (file.exists() || new File(context.getFilesDir(), FILE_NAME + ".bak").exists()) {
                String legacy = readLegacyVault();
                if (!legacy.isEmpty()) {
                    save(legacy);
                    ContentValues mcv = new ContentValues();
                    mcv.put("chave", "db_migrated");
                    mcv.put("valor_enc", dbHelper.encrypt("true"));
                    mcv.put("updated_at", System.currentTimeMillis());
                    db.insertWithOnConflict("configuracoes", null, mcv, SQLiteDatabase.CONFLICT_REPLACE);
                    if (file.exists()) file.renameTo(new File(context.getFilesDir(), FILE_NAME + ".migrated"));
                    return read();
                }
            }
            ContentValues mcv = new ContentValues();
            mcv.put("chave", "db_migrated");
            mcv.put("valor_enc", dbHelper.encrypt("true"));
            mcv.put("updated_at", System.currentTimeMillis());
            db.insertWithOnConflict("configuracoes", null, mcv, SQLiteDatabase.CONFLICT_REPLACE);
        }

        return root.toString();
    }

    synchronized void save(String json) throws Exception {
        JSONObject validated = new JSONObject(json);
        if (validated.optInt("version") < 1 || !(validated.opt("cases") instanceof JSONArray)
                || !(validated.opt("appointments") instanceof JSONArray))
            throw new IllegalArgumentException("Estrutura de dados inválida");

        JSONArray cases = validated.getJSONArray("cases");
        JSONArray appts = validated.getJSONArray("appointments");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Save root configurations to preserve preferences, active engine and UI state
            JSONObject rootMeta = new JSONObject(json);
            rootMeta.remove("cases");
            rootMeta.remove("appointments");
            ContentValues cfgCv = new ContentValues();
            cfgCv.put("chave", "app_state_meta");
            cfgCv.put("valor_enc", dbHelper.encrypt(rootMeta.toString()));
            cfgCv.put("updated_at", System.currentTimeMillis());
            int cfgUpdated = db.update("configuracoes", cfgCv, "chave = ?", new String[]{"app_state_meta"});
            if (cfgUpdated == 0) {
                db.insertWithOnConflict("configuracoes", null, cfgCv, SQLiteDatabase.CONFLICT_REPLACE);
            }

            for (int i = 0; i < cases.length(); i++) {
                JSONObject cObj = cases.getJSONObject(i);
                String caseId = cObj.getString("id");

                // Strictly segregate private diary
                JSONArray legacyDiary = cObj.optJSONArray("diarioNotas");
                cObj.remove("diarioNotas");

                JSONArray sessoes = cObj.optJSONArray("sessoes");
                JSONObject payload = new JSONObject(cObj.toString());
                payload.remove("sessoes");

                ContentValues cv = new ContentValues();
                cv.put("id", caseId);
                cv.put("created_at", cObj.optLong("created_at", System.currentTimeMillis()));
                cv.put("updated_at", System.currentTimeMillis());
                cv.put("payload_enc", dbHelper.encrypt(payload.toString()));

                // Atomic update in place to PREVENT cascade wipeout of child entities
                int updated = db.update("_pacientes", cv, "id = ?", new String[]{caseId});
                if (updated == 0) {
                    db.insertOrThrow("_pacientes", null, cv);
                }

                if (legacyDiary != null && legacyDiary.length() > 0 && "[]".equals(dbHelper.readPrivateDiary(caseId))) {
                    dbHelper.savePrivateDiary(caseId, legacyDiary.toString());
                }
                if (sessoes != null) {
                    for (int s = 0; s < sessoes.length(); s++) {
                        JSONObject sObj = sessoes.getJSONObject(s);
                        String sId = sObj.optString("id", java.util.UUID.randomUUID().toString());
                        String sData = sObj.optString("data", "");
                        String sOrigem = sObj.optString("origem", "audio");

                        ContentValues scv = new ContentValues();
                        scv.put("id", sId);
                        scv.put("case_id", caseId);
                        scv.put("data", sData);
                        if (sObj.has("hora")) scv.put("hora", sObj.optString("hora"));
                        scv.put("origem", sOrigem);
                        scv.put("created_at", sObj.optLong("created_at", System.currentTimeMillis()));
                        scv.put("updated_at", System.currentTimeMillis());
                        scv.put("payload_enc", dbHelper.encrypt(sObj.toString()));

                        // Atomic update in place for sessions
                        int sUpdated = db.update("sessoes", scv, "id = ? AND case_id = ?", new String[]{sId, caseId});
                        if (sUpdated == 0) {
                            db.insertOrThrow("sessoes", null, scv);
                        }
                    }
                }
            }

            db.delete("agenda", null, null);
            for (int i = 0; i < appts.length(); i++) {
                JSONObject aObj = appts.getJSONObject(i);
                ContentValues acv = new ContentValues();
                acv.put("id", aObj.optString("id", java.util.UUID.randomUUID().toString()));
                acv.put("case_id", aObj.optString("caseId", ""));
                acv.put("data", aObj.optString("data", ""));
                acv.put("hora", aObj.optString("hora", ""));
                acv.put("created_at", System.currentTimeMillis());
                acv.put("payload_enc", dbHelper.encrypt(aObj.toString()));
                db.insertWithOnConflict("agenda", null, acv, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private synchronized String readLegacyVault() throws Exception {
        File file = new File(context.getFilesDir(), FILE_NAME);
        AtomicFile atom = new AtomicFile(file);
        if (!atom.getBaseFile().exists() && !new File(context.getFilesDir(), FILE_NAME + ".bak").exists()) return "";
        byte[] bytes;
        try (FileInputStream stream = atom.openRead()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n = stream.read(buffer)) != -1) {
                if (out.size() + n > 100000000) throw new IllegalStateException("Base muito grande");
                out.write(buffer, 0, n);
            }
            bytes = out.toByteArray();
        }
        if (bytes.length < 30) return "";
        byte[] iv = Arrays.copyOfRange(bytes, 0, 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, deviceKey(), new GCMParameterSpec(128, iv));
        byte[] plaintext = cipher.doFinal(Arrays.copyOfRange(bytes, 12, bytes.length));
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private synchronized void saveLegacyVault(String json) throws Exception {
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

    synchronized void savePrivateDiary(String caseId, String json) throws Exception {
        dbHelper.savePrivateDiary(caseId, json);
    }

    synchronized String readPrivateDiary(String caseId) throws Exception {
        return dbHelper.readPrivateDiary(caseId);
    }

    synchronized void saveCaseDraft(String caseId, String json) throws Exception {
        dbHelper.saveCaseDraft(caseId, json);
    }

    synchronized String readCaseDraft(String caseId) throws Exception {
        return dbHelper.readCaseDraft(caseId);
    }

    synchronized void clearCaseDraft(String caseId) {
        dbHelper.clearCaseDraft(caseId);
    }

    synchronized void saveDraft(String json) throws Exception {
        JSONObject obj = new JSONObject(json);
        if (obj.optString("caseId").trim().isEmpty() || !(obj.opt("session") instanceof JSONObject))
            throw new IllegalArgumentException("Rascunho inválido");
        if (json.length() > 50_000_000) throw new IllegalArgumentException("Rascunho acima do limite");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey());
        byte[] iv = cipher.getIV();
        byte[] encoded = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), "draft.aes"));
        FileOutputStream out = null;
        try {
            out = file.startWrite();
            out.write(iv); out.write(encoded);
            file.finishWrite(out);
        } catch (Exception e) { if (out != null) file.failWrite(out); throw e; }
        saveCaseDraft(obj.getString("caseId"), json);
    }

    synchronized String readDraft() throws Exception {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), "draft.aes"));
        if (!file.getBaseFile().exists() && !new File(context.getFilesDir(), "draft.aes.bak").exists()) return "";
        try (FileInputStream in = file.openRead()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] bytes = new byte[8192]; int n;
            while ((n = in.read(bytes)) != -1) {
                if (buf.size() + n > 50_000_000) throw new IllegalArgumentException("Rascunho muito grande");
                buf.write(bytes, 0, n);
            }
            byte[] enc = buf.toByteArray();
            if (enc.length < 30) throw new SecurityException("Rascunho cifrado inválido");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deviceKey(), new GCMParameterSpec(128, Arrays.copyOfRange(enc, 0, 12)));
            return new String(cipher.doFinal(Arrays.copyOfRange(enc, 12, enc.length)), StandardCharsets.UTF_8);
        }
    }

    synchronized void clearDraft() {
        new AtomicFile(new File(context.getFilesDir(), "draft.aes")).delete();
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
            return new SecretKeySpec(
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
        if (input.startsWith("[")) return input; // Legacy backup
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
