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

    // Keep large ciphertexts outside Android CursorWindow's per-row size limit.
    private static final int INLINE_BLOB=262144, MAX_RECORD_BLOB=100_000_000;
    private static String smallBlob(String column){return "CASE WHEN length("+column+")<="+INLINE_BLOB+" THEN "+column+" ELSE NULL END AS "+column;}
    private static byte[] readLargeBlob(SQLiteDatabase db,String table,String column,String where,String[] args) throws Exception {
        int length;
        try(Cursor c=db.query(table,new String[]{"length("+column+")"},where,args,null,null,null)){
            if(!c.moveToFirst())return null;length=c.getInt(0);
        }
        if(length<0||length>MAX_RECORD_BLOB)throw new java.io.IOException("Registro acima do limite de recuperação");
        ByteArrayOutputStream out=new ByteArrayOutputStream(length);
        for(int offset=0;offset<length;offset+=INLINE_BLOB){
            int count=Math.min(INLINE_BLOB,length-offset);
            try(Cursor c=db.query(table,new String[]{"substr("+column+","+(offset+1)+","+count+")"},where,args,null,null,null)){
                if(!c.moveToFirst())throw new java.io.IOException("Registro removido durante leitura");
                byte[] part=c.getBlob(0);if(part==null||part.length!=count)throw new SecurityException("Registro cifrado incompleto");out.write(part);
            }
        }
        return out.toByteArray();
    }
    private static byte[] readBoundedBlob(SQLiteDatabase db,Cursor cursor,String table,String column,String where,String[] args) throws Exception {
        byte[] inline=cursor.getBlob(cursor.getColumnIndexOrThrow(column));return inline!=null?inline:readLargeBlob(db,table,column,where,args);
    }
    synchronized String read() throws Exception {
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        if(db.isReadOnly())return readSnapshot(db);
        db.beginTransactionNonExclusive();
        try{String data=readSnapshot(db);db.setTransactionSuccessful();return data;}finally{db.endTransaction();}
    }
    private String readSnapshot(SQLiteDatabase db) throws Exception {
        JSONArray casesArray = new JSONArray();

        try (Cursor c = db.query("_pacientes", new String[]{"id",smallBlob("payload_enc")}, null, null, null, null, "created_at ASC")) {
            while (c.moveToNext()) {
                String caseId = c.getString(c.getColumnIndexOrThrow("id"));
                byte[] blob = readBoundedBlob(db,c,"_pacientes","payload_enc","id=?",new String[]{caseId});
                JSONObject caseObj = (blob != null && blob.length > 0)
                        ? new JSONObject(dbHelper.decrypt(blob))
                        : new JSONObject();
                caseObj.put("id", caseId);

                // Scope the segment lookup to this patient, including the session
                // identifier in the grouping so no other patient can contribute text.
                java.util.Map<String, JSONArray> segmentsBySession = new java.util.HashMap<>();
                try (Cursor segCursor = db.query("segmentos", null, "case_id = ?",
                        new String[]{caseId}, null, null, "session_id ASC, start_ms ASC, id ASC")) {
                    while (segCursor.moveToNext()) {
                        String sessionId = segCursor.getString(segCursor.getColumnIndexOrThrow("session_id"));
                        JSONObject seg = new JSONObject();
                        seg.put("id", segCursor.getString(segCursor.getColumnIndexOrThrow("id")));
                        seg.put("startMs", segCursor.getLong(segCursor.getColumnIndexOrThrow("start_ms")));
                        seg.put("endMs", segCursor.getLong(segCursor.getColumnIndexOrThrow("end_ms")));
                        String role = segCursor.getString(segCursor.getColumnIndexOrThrow("speaker"));
                        boolean anonymous=role.matches("falante[1-4]")||SpeakerTimeline.OVERLAP.equals(role);
                        seg.put("cluster", anonymous ? role : "");
                        seg.put("speaker", anonymous ? "desconhecido" : role);
                        seg.put("manualSpeaker", segCursor.getInt(segCursor.getColumnIndexOrThrow("speaker_confirmed")) == 1);
                        seg.put("text", dbHelper.decrypt(segCursor.getBlob(segCursor.getColumnIndexOrThrow("text_enc"))));
                        JSONArray sessionSegments = segmentsBySession.get(sessionId);
                        if (sessionSegments == null) {
                            sessionSegments = new JSONArray();
                            segmentsBySession.put(sessionId, sessionSegments);
                        }
                        sessionSegments.put(seg);
                    }
                }

                // Load sessions for this patient
                JSONArray sessoes = new JSONArray();
                try (Cursor sc = db.query("sessoes", new String[]{"id","data","hora","origem",smallBlob("payload_enc")}, "case_id = ?",
                        new String[]{caseId}, null, null, "created_at ASC")) {
                    while (sc.moveToNext()) {
                        String sId = sc.getString(sc.getColumnIndexOrThrow("id"));
                        byte[] sBlob = readBoundedBlob(db,sc,"sessoes","payload_enc","case_id=? AND id=?",new String[]{caseId,sId});
                        JSONObject sObj = (sBlob != null && sBlob.length > 0)
                                 ? new JSONObject(dbHelper.decrypt(sBlob))
                                : new JSONObject();
                        sObj.put("id", sId);
                        sObj.put("data", sc.getString(sc.getColumnIndexOrThrow("data")));
                        int hourCol = sc.getColumnIndexOrThrow("hora");
                        if (!sObj.has("hora") && !sc.isNull(hourCol)) sObj.put("hora", sc.getString(hourCol));
                        sObj.put("origem", sc.getString(sc.getColumnIndexOrThrow("origem")));

                        // A single ordered segment query per patient avoids one SQLite
                        // cursor per session (10,000 cursors in a large manual history).
                        JSONArray segs = segmentsBySession.get(sId);
                        if (segs == null) segs = new JSONArray();
                        StringBuilder fullText = new StringBuilder();
                        for (int j = 0; j < segs.length(); j++) {
                            if (fullText.length() > 0) fullText.append(" ");
                            fullText.append(segs.getJSONObject(j).getString("text"));
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
        try (Cursor c = db.query("agenda", new String[]{"id",smallBlob("payload_enc")}, null, null, null, null, "data ASC, hora ASC")) {
            while (c.moveToNext()) {
                byte[] blob = readBoundedBlob(db,c,"agenda","payload_enc","id=?",new String[]{c.getString(0)});
                if (blob != null && blob.length > 0) {
                    apptsArray.put(new JSONObject(dbHelper.decrypt(blob)));
                }
            }
        }

        JSONObject root = new JSONObject();
        // Reconstruct root metadata (theme, activeEngine, settings, etc.) from configuracoes unconditionally
        try (Cursor cfgCursor = db.query("configuracoes", new String[]{smallBlob("valor_enc")}, "chave = ?", new String[]{"app_state_meta"}, null, null, null)) {
            if (cfgCursor.moveToFirst()) {
                byte[] blob = readBoundedBlob(db,cfgCursor,"configuracoes","valor_enc","chave=?",new String[]{"app_state_meta"});
                if (blob != null && blob.length > 0) {
                    JSONObject meta = new JSONObject(dbHelper.decrypt(blob));
                    java.util.Iterator<String> keys = meta.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        root.put(k, meta.get(k));
                    }
                }
            }
        }
        if (!root.has("version")) root.put("version", 1);
        root.put("cases", casesArray);
        root.put("appointments", apptsArray);

        // One-time legacy migration check
        boolean migrated = false;
        try (Cursor c = db.query("configuracoes", new String[]{"chave"}, "chave = ?", new String[]{"db_migrated"}, null, null, null)) {
            migrated = c.moveToFirst();
        }
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

    synchronized JSONArray readReminderAgenda() throws Exception {
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        JSONArray rows=new JSONArray();
        try(Cursor c=db.query("agenda",new String[]{"id",smallBlob("payload_enc")},null,null,null,null,"data ASC,hora ASC")){
            while(c.moveToNext()){
                JSONObject a=new JSONObject(dbHelper.decrypt(readBoundedBlob(db,c,"agenda","payload_enc","id=?",new String[]{c.getString(0)})));
                rows.put(new JSONObject().put("data",a.optString("data")).put("hora",a.optString("hora")).put("status",a.optString("status")));
            }
        }
        return rows;
    }

    private static String requiredId(JSONObject object, java.util.Set<String> seen, String kind) throws Exception {
        Object raw = object.opt("id");
        if (!(raw instanceof String) || ((String)raw).trim().isEmpty() || !seen.add((String)raw))
            throw new IllegalArgumentException("Identificador ausente ou duplicado: " + kind);
        return (String)raw;
    }

    private static void validateSnapshot(JSONArray cases, JSONArray appts) throws Exception {
        java.util.Set<String> caseIds = new java.util.HashSet<>();
        java.util.Set<String> sessionIds = new java.util.HashSet<>();
        java.util.Set<String> appointmentIds = new java.util.HashSet<>();
        for (int i=0;i<cases.length();i++) {
            JSONObject c = cases.getJSONObject(i);
            requiredId(c,caseIds,"caso");
            if(c.has("sessoes") && !(c.opt("sessoes") instanceof JSONArray))
                throw new IllegalArgumentException("Lista de sessões inválida");
            JSONArray sessions = c.optJSONArray("sessoes");
            if(sessions != null) for(int j=0;j<sessions.length();j++)
                requiredId(sessions.getJSONObject(j),sessionIds,"sessão");
        }
        for(int i=0;i<appts.length();i++) requiredId(appts.getJSONObject(i),appointmentIds,"agenda");
    }

    // Separate encrypted, durable form journal. Never overwrites the audio recovery journal.
    synchronized void saveFormDraft(String caseId, String json) throws Exception {
        if(caseId == null || caseId.trim().isEmpty() || caseId.length()>200 || json.length()>5_000_000)
            throw new IllegalArgumentException("Rascunho de formulário inválido");
        JSONObject obj=new JSONObject(json);
        if(!caseId.equals(obj.optString("caseId")) || !(obj.opt("session") instanceof JSONObject))
            throw new IllegalArgumentException("Rascunho de formulário inválido");
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            try(Cursor c=db.query("_pacientes",new String[]{"id"},"id=?",new String[]{caseId},null,null,null)){
                if(!c.moveToFirst())throw new IllegalArgumentException("Caso inexistente");
            }
            ContentValues cv=new ContentValues();
            cv.put("chave","form_draft:"+caseId);cv.put("valor_enc",dbHelper.encrypt(json));cv.put("updated_at",System.currentTimeMillis());
            int updated=db.update("configuracoes",cv,"chave=?",new String[]{"form_draft:"+caseId});
            if(updated==0)db.insertOrThrow("configuracoes",null,cv);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    synchronized String readFormDraft(String caseId) throws Exception {
        SQLiteDatabase db=dbHelper.getReadableDatabase();try(Cursor c=db.query("configuracoes",new String[]{smallBlob("valor_enc")},"chave=?",new String[]{"form_draft:"+caseId},null,null,null)){
            return c.moveToFirst()?dbHelper.decrypt(readBoundedBlob(db,c,"configuracoes","valor_enc","chave=?",new String[]{"form_draft:"+caseId})):"";
        }
    }
    synchronized void clearFormDraft(String caseId,String sessionId) throws Exception {
        String json=readFormDraft(caseId);
        if(!json.isEmpty()&&sessionId.equals(new JSONObject(json).getJSONObject("session").optString("id")))
            dbHelper.getWritableDatabase().delete("configuracoes","chave=?",new String[]{"form_draft:"+caseId});
    }
    private static void ensureRevisionTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS eco_revisions (seq INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, case_id TEXT NOT NULL, entity_id TEXT NOT NULL, captured_at INTEGER NOT NULL, payload_enc BLOB NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS eco_revisions_case_idx ON eco_revisions(case_id,kind,entity_id,seq)");
    }
    private static void retainRevision(SQLiteDatabase db,String kind,String caseId,String entityId,byte[] previous){
        if(previous==null||previous.length==0)return;
        ContentValues cv=new ContentValues();cv.put("kind",kind);cv.put("case_id",caseId);cv.put("entity_id",entityId);
        cv.put("captured_at",System.currentTimeMillis());cv.put("payload_enc",previous);db.insertOrThrow("eco_revisions",null,cv);
    }
    synchronized String sessionVersions(String caseId,String sessionId) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureRevisionTable(db);
        JSONArray versions=new JSONArray();
        try(Cursor c=db.query("eco_revisions",new String[]{"seq","captured_at"},"kind=? AND case_id=? AND entity_id=?",new String[]{"session",caseId,sessionId},null,null,"seq DESC")){
            while(c.moveToNext())versions.put(new JSONObject().put("id",c.getLong(0)).put("date",c.getLong(1)));
        }
        return versions.toString();
    }
    synchronized String sessionVersion(String caseId,String sessionId,long seq) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureRevisionTable(db);
        try(Cursor c=db.query("eco_revisions",new String[]{smallBlob("payload_enc")},"kind=? AND case_id=? AND entity_id=? AND seq=?",new String[]{"session",caseId,sessionId,Long.toString(seq)},null,null,null)){
            if(!c.moveToFirst())throw new IllegalArgumentException("Versão indisponível");
            return dbHelper.decrypt(readBoundedBlob(db,c,"eco_revisions","payload_enc","kind=? AND case_id=? AND entity_id=? AND seq=?",new String[]{"session",caseId,sessionId,Long.toString(seq)}));
        }
    }


    private AtomicFile checkpointFile(int slot){
        if(slot<0||slot>1)throw new IllegalArgumentException("Checkpoint inválido");
        return new AtomicFile(new File(context.getFilesDir(),"eco-checkpoint-"+slot+".aes"));
    }
    synchronized void checkpoint(boolean force) throws Exception {
        long now=System.currentTimeMillis();
        File a=checkpointFile(0).getBaseFile(),b=checkpointFile(1).getBaseFile();
        long last=Math.max(a.lastModified(),b.lastModified());
        if(!force&&last>0&&now-last<24L*60*60*1000)return;
        int slot=a.lastModified()<=b.lastModified()?0:1;
        JSONObject pkg=new JSONObject().put("capturedAt",now).put("state",new JSONObject(read()));
        byte[] encrypted=dbHelper.encrypt(pkg.toString());
        if(encrypted.length>100_000_000)throw new java.io.IOException("Checkpoint acima do limite de recuperação");
        AtomicFile file=checkpointFile(slot);FileOutputStream out=null;
        try{out=file.startWrite();out.write(encrypted);file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
    synchronized String readCheckpoint(int slot) throws Exception {
        AtomicFile file=checkpointFile(slot);
        try(FileInputStream in=file.openRead()){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[65536];int n;
            while((n=in.read(buffer))!=-1){if(out.size()+n>100_000_000)throw new java.io.IOException("Checkpoint acima do limite");out.write(buffer,0,n);}
            JSONObject pkg=new JSONObject(dbHelper.decrypt(out.toByteArray()));
            return pkg.getJSONObject("state").toString();
        }
    }
    synchronized JSONArray checkpointList() throws Exception {
        JSONArray checkpoints=new JSONArray();
        for(int i=0;i<2;i++){File f=checkpointFile(i).getBaseFile();if(f.exists())checkpoints.put(new JSONObject().put("slot",i).put("date",f.lastModified()).put("bytes",f.length()));}
        return checkpoints;
    }
    synchronized String diagnostics() throws Exception {
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        JSONObject result=new JSONObject();
        try(Cursor c=db.rawQuery("PRAGMA quick_check",null)){result.put("integrity",c.moveToFirst()&&"ok".equals(c.getString(0))?"ok":"erro");}
        try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){result.put("foreignKeysOk",!c.moveToFirst());}
        for(String table:new String[]{"_pacientes","sessoes","segmentos","agenda"}){
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){if(c.moveToFirst())result.put(table,c.getLong(0));}
        }
        return result.put("checkpoints",checkpointList()).put("freeBytes",context.getFilesDir().getUsableSpace()).toString();
    }


    private static final int ATTACHMENT_LIMIT=8_000_000, ATTACHMENT_CHUNK=96_000;
    private static void ensureAttachments(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS eco_attachments(id TEXT PRIMARY KEY NOT NULL,case_id TEXT NOT NULL REFERENCES _pacientes(id) ON DELETE RESTRICT,meta_enc BLOB NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS eco_attachments_case ON eco_attachments(case_id,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS eco_attachment_chunks(id TEXT NOT NULL REFERENCES eco_attachments(id) ON DELETE RESTRICT,part INTEGER NOT NULL,data_enc BLOB NOT NULL,PRIMARY KEY(id,part))");
    }
    private static String attachmentMime(byte[] data) throws Exception {
        if(data==null||data.length<8||data.length>ATTACHMENT_LIMIT)throw new java.io.IOException("Tamanho de anexo inválido");
        if(data[0]=='%'&&data[1]=='P'&&data[2]=='D'&&data[3]=='F'&&data[4]=='-')return "application/pdf";
        if((data[0]&255)==137&&data[1]=='P'&&data[2]=='N'&&data[3]=='G'&&data[4]==13&&data[5]==10&&data[6]==26&&data[7]==10)return "image/png";
        if((data[0]&255)==255&&(data[1]&255)==216&&(data[2]&255)==255)return "image/jpeg";
        throw new java.io.IOException("Somente PDF, PNG ou JPEG são aceitos");
    }
    private static String sha256(byte[] data) throws Exception {
        StringBuilder s=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(data))s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return s.toString();
    }
    synchronized JSONObject addAttachment(String caseId,String title,String classification,byte[] data) throws Exception {
        if(title==null||title.trim().isEmpty()||title.length()>200||classification==null||classification.length()>100)throw new IllegalArgumentException("Metadados inválidos");
        JSONObject meta=new JSONObject().put("id",java.util.UUID.randomUUID().toString()).put("caseId",caseId)
            .put("title",title).put("classification",classification).put("mime",attachmentMime(data)).put("sha256",sha256(data))
            .put("bytes",data.length).put("createdAt",System.currentTimeMillis()).put("source","Arquivo local autorizado");
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);db.beginTransaction();
        try{insertAttachment(db,meta,data);db.setTransactionSuccessful();return meta;}finally{db.endTransaction();}
    }
    private void insertAttachment(SQLiteDatabase db,JSONObject meta,byte[] data) throws Exception {
        String id=meta.getString("id"),caseId=meta.getString("caseId");
        if(id.isEmpty()||!attachmentMime(data).equals(meta.getString("mime"))||data.length!=meta.getInt("bytes")||!sha256(data).equals(meta.getString("sha256")))
            throw new SecurityException("Integridade do anexo inválida");
        ContentValues v=new ContentValues();v.put("id",id);v.put("case_id",caseId);v.put("meta_enc",dbHelper.encrypt(meta.toString()));v.put("created_at",meta.getLong("createdAt"));db.insertOrThrow("eco_attachments",null,v);
        for(int start=0,part=0;start<data.length;start+=ATTACHMENT_CHUNK,part++){
            byte[] chunk=Arrays.copyOfRange(data,start,Math.min(data.length,start+ATTACHMENT_CHUNK));
            ContentValues cv=new ContentValues();cv.put("id",id);cv.put("part",part);cv.put("data_enc",dbHelper.encrypt(Base64.encodeToString(chunk,Base64.NO_WRAP)));db.insertOrThrow("eco_attachment_chunks",null,cv);Arrays.fill(chunk,(byte)0);
        }
    }
    synchronized JSONArray listAttachments(String caseId) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);JSONArray result=new JSONArray();
        try(Cursor c=db.query("eco_attachments",new String[]{"meta_enc"},"case_id=?",new String[]{caseId},null,null,"created_at DESC")){
            while(c.moveToNext())result.put(new JSONObject(dbHelper.decrypt(c.getBlob(0))));
        }
        return result;
    }
    synchronized JSONObject attachmentMeta(String caseId,String id) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);
        try(Cursor c=db.query("eco_attachments",new String[]{"meta_enc"},"case_id=? AND id=?",new String[]{caseId,id},null,null,null)){
            if(!c.moveToFirst())throw new IllegalArgumentException("Anexo não encontrado");
            return new JSONObject(dbHelper.decrypt(c.getBlob(0)));
        }
    }
    synchronized byte[] attachmentBytes(String caseId,String id) throws Exception {
        JSONObject meta=attachmentMeta(caseId,id);SQLiteDatabase db=dbHelper.getReadableDatabase();ByteArrayOutputStream out=new ByteArrayOutputStream();int part=0;
        try(Cursor c=db.query("eco_attachment_chunks",new String[]{"part","data_enc"},"id=?",new String[]{id},null,null,"part ASC")){
            while(c.moveToNext()){
                if(c.getInt(0)!=part++)throw new SecurityException("Bloco de anexo ausente");
                byte[] decoded=Base64.decode(dbHelper.decrypt(c.getBlob(1)),Base64.NO_WRAP);
                if(out.size()+decoded.length>ATTACHMENT_LIMIT)throw new SecurityException("Anexo acima do limite");
                out.write(decoded);Arrays.fill(decoded,(byte)0);
            }
        }
        byte[] data=out.toByteArray();
        if(data.length!=meta.getInt("bytes")||!sha256(data).equals(meta.getString("sha256")))throw new SecurityException("Anexo não autenticado");
        return data;
    }
    synchronized JSONArray exportAttachments() throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);JSONArray rows=new JSONArray();long total=0;
        try(Cursor c=db.query("eco_attachments",new String[]{"id","case_id","meta_enc"},null,null,null,null,"created_at ASC")){
            while(c.moveToNext()){
                JSONObject meta=new JSONObject(dbHelper.decrypt(c.getBlob(2)));
                total+=meta.getLong("bytes");if(total>24_000_000)throw new java.io.IOException("Anexos acima do limite de exportação conjunta");
                byte[] data=attachmentBytes(c.getString(1),c.getString(0));rows.put(new JSONObject().put("meta",meta).put("data",Base64.encodeToString(data,Base64.NO_WRAP)));Arrays.fill(data,(byte)0);
            }
        }return rows;
    }
    private void restoreAttachments(SQLiteDatabase db,JSONArray rows) throws Exception {
        if(rows==null)return;ensureAttachments(db);long total=0;java.util.Set<String> seen=new java.util.HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i),meta=row.getJSONObject("meta");String id=meta.getString("id"),caseId=meta.getString("caseId");
            if(!seen.add(id))throw new IllegalArgumentException("Anexo duplicado");
            byte[] data=Base64.decode(row.getString("data"),Base64.NO_WRAP);total+=data.length;
            try{
                if(total>24_000_000||data.length!=meta.getInt("bytes")||!sha256(data).equals(meta.getString("sha256")))throw new SecurityException("Anexo inválido");
                try(Cursor c=db.query("eco_attachments",new String[]{"case_id","meta_enc"},"id=?",new String[]{id},null,null,null)){
                    if(c.moveToFirst()){
                        if(!caseId.equals(c.getString(0))||!meta.getString("sha256").equals(new JSONObject(dbHelper.decrypt(c.getBlob(1))).getString("sha256")))throw new SecurityException("Conflito de anexo");
                        continue;
                    }
                }
                insertAttachment(db,meta,data);
            }finally{Arrays.fill(data,(byte)0);}
        }
    }

    // A native, single-snapshot portable vault archive. Fixed schema, no arbitrary
    // SQL from the file. Device-bound encrypted cells are decrypted and re-encrypted
    // under the destination key. Audio spools and device credentials never travel.
    private static final int VAULT_MAGIC=0x45564c31;
    private static final long VAULT_MAX=8L*1024*1024*1024;
    private static final int VAULT_FIELD_MAX=MAX_RECORD_BLOB;
    private static final class VaultTable {
        final String table,encrypted,where,order;final String[] cols,keys;
        VaultTable(String table,String encrypted,String where,String order,String cols,String keys){
            this.table=table;this.encrypted=encrypted;this.where=where;this.order=order;
            this.cols=cols.split(",");this.keys=keys.split(",");
        }
    }
    private static final VaultTable[] VAULT_TABLES={
        new VaultTable("_pacientes","payload_enc",null,"id","id,created_at,updated_at,payload_enc","id"),
        new VaultTable("sessoes","payload_enc",null,"id","id,case_id,data,hora,origem,created_at,updated_at,payload_enc","id"),
        new VaultTable("processamento_jobs","",null,"job_id","job_id,case_id,session_id,audio_path,state,total_segments,committed_segments,checkpoint_offset_ms,created_at,updated_at,error_message,eof_verified","job_id"),
        new VaultTable("segmentos","text_enc",null,"id","id,job_id,session_id,case_id,speaker,speaker_confirmed,start_ms,end_ms,text_enc,checksum,status,created_at","id"),
        new VaultTable("agenda","payload_enc",null,"id","id,case_id,data,hora,created_at,payload_enc","id"),
        new VaultTable("rascunhos","draft_enc",null,"case_id","case_id,session_id,updated_at,draft_enc","case_id"),
        new VaultTable("diario_privado","entry_enc",null,"id","id,case_id,data,categoria,created_at,entry_enc","id"),
        new VaultTable("configuracoes","valor_enc","chave='app_state_meta' OR chave GLOB 'form_draft:*' OR chave GLOB 'editor_draft:*' OR chave GLOB 'clinical_draft:*' OR chave GLOB 'asr:*'","chave","chave,valor_enc,updated_at","chave"),
        new VaultTable("eco_revisions","payload_enc",null,"seq","seq,kind,case_id,entity_id,captured_at,payload_enc","seq"),
        new VaultTable("eco_attachments","meta_enc",null,"id","id,case_id,meta_enc,created_at","id"),
        new VaultTable("eco_attachment_chunks","data_enc",null,"id,part","id,part,data_enc","id,part")
    };
    private static boolean portableConfig(String key,boolean diary){
        return "app_state_meta".equals(key)||key.startsWith("form_draft:")||
            key.equals("editor_draft:wizard")||key.equals("editor_draft:appointment")||
            (key.startsWith("clinical_draft:")&&(diary||!key.startsWith("clinical_draft:res_diary:")))||
            key.startsWith("asr:");
    }
    private static String rowWhere(VaultTable t){
        return String.join("=? AND ",t.keys)+"=?";
    }
    private static String[] rowArgs(Cursor c,VaultTable t){
        String[] args=new String[t.keys.length];
        for(int i=0;i<args.length;i++)args[i]=c.getString(c.getColumnIndexOrThrow(t.keys[i]));
        return args;
    }
    private static void writeVaultBytes(java.io.DataOutputStream out,byte[] bytes)throws Exception{
        try{if(bytes.length>VAULT_FIELD_MAX)throw new java.io.IOException("Registro acima do limite individual");out.writeInt(bytes.length);out.write(bytes);}
        finally{Arrays.fill(bytes,(byte)0);}
    }
    private static String readVaultText(java.io.DataInputStream in)throws Exception{
        int length=in.readInt();
        if(length<0||length>VAULT_FIELD_MAX)throw new java.io.IOException("Campo de arquivo inválido");
        byte[] bytes=new byte[length];
        try{in.readFully(bytes);return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
        finally{Arrays.fill(bytes,(byte)0);}
    }
    private static boolean any(SQLiteDatabase db,String table,String where){
        try(Cursor c=db.query(table,new String[]{"1"},where,null,null,null,null,"1")){return c.moveToFirst();}
    }
    private void requireNoPendingAudio(SQLiteDatabase db)throws Exception{
        if(any(db,"processamento_jobs","audio_key_enc IS NOT NULL OR audio_path<>''")||!readDraft().isEmpty())
            throw new java.io.IOException("Finalize ou descarte a gravação e o rascunho de áudio antes do backup integral");
    }
    synchronized int exportFullVault(java.io.OutputStream target,char[] password,boolean diary,java.util.function.BooleanSupplier active)throws Exception{
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);ensureRevisionTable(db);
        db.beginTransaction();
        try(EcoBackupStream.Writer writer=new EcoBackupStream.Writer(target,password,VAULT_MAX)){
            requireNoPendingAudio(db);
            java.io.DataOutputStream out=new java.io.DataOutputStream(writer);
            out.writeInt(VAULT_MAGIC);out.writeBoolean(diary);
            int count=0;
            for(int index=0;index<VAULT_TABLES.length;index++){
                VaultTable t=VAULT_TABLES[index];out.writeInt(index);
                String[] projection=t.cols.clone();
                for(int i=0;i<projection.length;i++)if(projection[i].equals(t.encrypted))projection[i]=smallBlob(t.encrypted);
                if(diary||!t.table.equals("diario_privado")){
                    try(Cursor c=db.query(t.table,projection,t.where,null,null,null,t.order)){
                        while(c.moveToNext()){
                            checkArchiveActive(active);
                            if(t.table.equals("configuracoes")&&!portableConfig(c.getString(0),diary))continue;
                            out.writeByte(1);
                            for(int i=0;i<t.cols.length;i++){
                                String col=t.cols[i];
                                if(col.equals(t.encrypted)){
                                    byte[] enc=readBoundedBlob(db,c,t.table,col,rowWhere(t),rowArgs(c,t));
                                    if(enc==null)throw new SecurityException("Campo cifrado ausente");
                                    out.writeByte(3);
                                    writeVaultBytes(out,dbHelper.decrypt(enc).getBytes(StandardCharsets.UTF_8));
                                }else if(c.isNull(i)){out.writeByte(0);}
                                else if(c.getType(i)==Cursor.FIELD_TYPE_INTEGER){out.writeByte(1);out.writeLong(c.getLong(i));}
                                else if(c.getType(i)==Cursor.FIELD_TYPE_STRING){out.writeByte(2);writeVaultBytes(out,c.getString(i).getBytes(StandardCharsets.UTF_8));}
                                else throw new SecurityException("Tipo não portátil");
                            }
                            count=Math.addExact(count,1);
                        }
                    }
                }
                out.writeByte(0);
            }
            out.writeInt(count);checkArchiveActive(active);writer.finish();db.setTransactionSuccessful();return count;
        }finally{db.endTransaction();}
    }
    // Refuse to merge an integral recovery into any populated vault.
    // Ordinary legacy backup retains its separate non-destructive merge path.
    private void requireEmptyVault(SQLiteDatabase db)throws Exception{
        requireNoPendingAudio(db);
        for(VaultTable t:VAULT_TABLES){
            if(t.table.equals("configuracoes")){
                if(any(db,t.table,"chave<>'db_migrated' AND chave<>'app_state_meta'"))
                    throw new SecurityException("Cofre contém rascunhos ou configurações existentes");
                try(Cursor c=db.query(t.table,new String[]{smallBlob("valor_enc")},"chave='app_state_meta'",null,null,null,null)){
                    if(c.moveToFirst()){
                        JSONObject meta=new JSONObject(dbHelper.decrypt(readBoundedBlob(db,c,t.table,"valor_enc","chave=?",new String[]{"app_state_meta"})));
                        for(String k:new String[]{"ecoFinance","ecoQuestionTemplates","ecoPersonalLibrary","ecoDocTemplates"})
                            if(meta.has(k)&&(!(meta.opt(k) instanceof JSONArray)||meta.getJSONArray(k).length()>0))
                                throw new SecurityException("Cofre contém registros administrativos");
                    }
                }
            }else if(any(db,t.table,null))throw new SecurityException("Restauração integral exige um cofre vazio");
        }
    }
    private static void validatePortableRow(SQLiteDatabase db,VaultTable t,ContentValues v,String clear,boolean diary)throws Exception{
        for(String key:t.keys)if(v.getAsString(key)==null||v.getAsString(key).isEmpty())
            throw new SecurityException("Identificador ausente");
        String caseId=v.getAsString("case_id");
        if(caseId!=null&&!caseId.isEmpty()&&!caseExists(db,caseId))throw new SecurityException("Caso não localizado");
        switch(t.table){
            case "_pacientes":case "sessoes":case "agenda":case "eco_attachments":{
                JSONObject data=new JSONObject(clear);
                if(!v.getAsString("id").equals(data.optString("id")))throw new SecurityException("Identificador de conteúdo divergente");
                if(data.has("caseId")&&caseId!=null&&!caseId.equals(data.optString("caseId")))throw new SecurityException("Propriedade de registro divergente");
                if(t.table.equals("_pacientes")&&data.has("diarioNotas"))throw new SecurityException("Diário fora do cofre segregado");
                break;
            }
            case "segmentos":{
                Long a=v.getAsLong("start_ms"),b=v.getAsLong("end_ms");
                if(a==null||b==null||a<0||b<a||!EcoDatabaseHelper.computeChecksum(clear).equals(v.getAsString("checksum")))
                    throw new SecurityException("Trecho inconsistente");
                break;
            }
            case "rascunhos":{
                JSONObject data=new JSONObject(clear);
                if(data.has("caseId")&&!caseId.equals(data.optString("caseId")))throw new SecurityException("Rascunho de outro caso");
                break;
            }
            case "diario_privado":
                if(!diary)throw new SecurityException("Diário sem autorização");
                new JSONObject(clear);break;
            case "eco_revisions":{
                JSONObject data=new JSONObject(clear);String kind=v.getAsString("kind"),id=v.getAsString("entity_id");
                if(!("session".equals(kind)||"agenda".equals(kind))||id==null||!id.equals(data.optString("id")))
                    throw new SecurityException("Histórico incompatível");
                if("agenda".equals(kind)&&!caseId.equals(data.optString("caseId")))throw new SecurityException("Histórico de outro caso");
                if("session".equals(kind))try(Cursor c=db.query("sessoes",new String[]{"case_id"},"id=?",new String[]{id},null,null,null)){
                    if(c.moveToFirst()&&!caseId.equals(c.getString(0)))throw new SecurityException("Histórico de outro caso");
                }
                break;
            }
            case "configuracoes":{
                String key=v.getAsString("chave");
                if(!portableConfig(key,diary))throw new SecurityException("Configuração não portátil");
                if(key.equals("app_state_meta")){
                    JSONObject data=new JSONObject(clear);
                    if(data.has("cases")||data.has("appointments")||data.has("attachmentBundle")||data.has("nativeRecoveryBundle"))
                        throw new SecurityException("Metadados de raiz inválidos");
                }else if(key.startsWith("form_draft:")){
                    JSONObject data=new JSONObject(clear);String id=data.getString("caseId");
                    if(!key.equals("form_draft:"+id)||!caseExists(db,id)||!(data.opt("session") instanceof JSONObject))
                        throw new SecurityException("Rascunho inválido");
                }else if(key.startsWith("editor_draft:")){
                    String slot=key.substring("editor_draft:".length());JSONObject data=validateEditor(slot,clear);
                    if("appointment".equals(slot)&&!caseExists(db,data.getJSONObject("value").getString("caseId")))
                        throw new SecurityException("Consulta sem caso");
                }else if(key.startsWith("clinical_draft:")){
                    JSONObject data=new JSONObject(clear);String id=data.getString("caseId"),kind=data.getString("kind");
                    if(!key.equals(clinicalDraftKey(id,kind))||(!"__eco_admin__".equals(id)&&!caseExists(db,id))||
                        data.optString("id").isEmpty()||!(data.opt("fields") instanceof JSONObject)||!(data.opt("base") instanceof JSONObject))
                        throw new SecurityException("Rascunho clínico inválido");
                }
                break;
            }
            default:break;
        }
    }
    synchronized int restoreFullVault(java.io.InputStream source,char[] password,java.util.function.BooleanSupplier active)throws Exception{
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);ensureRevisionTable(db);
        db.beginTransaction();
        try(EcoBackupStream.Reader reader=new EcoBackupStream.Reader(source,password,VAULT_MAX)){
            requireEmptyVault(db);
            java.io.DataInputStream in=new java.io.DataInputStream(reader);
            if(in.readInt()!=VAULT_MAGIC)throw new SecurityException("Arquivo não é backup integral ECO");
            boolean diary=in.readBoolean();int count=0;
            db.delete("configuracoes","chave=?",new String[]{"app_state_meta"});
            for(int index=0;index<VAULT_TABLES.length;index++){
                VaultTable t=VAULT_TABLES[index];
                if(in.readInt()!=index)throw new SecurityException("Ordem de tabelas inválida");
                while(true){
                    checkArchiveActive(active);int marker=in.readUnsignedByte();if(marker==0)break;
                    if(marker!=1||(!diary&&t.table.equals("diario_privado")))throw new SecurityException("Registro inválido");
                    ContentValues values=new ContentValues();String clear=null;
                    for(String col:t.cols){
                        int type=in.readUnsignedByte();
                        if(col.equals(t.encrypted)){
                            if(type!=3)throw new SecurityException("Campo cifrado inválido");
                            clear=readVaultText(in);values.put(col,encryptRecord(clear));
                        }else if(type==0)values.putNull(col);
                        else if(type==1)values.put(col,in.readLong());
                        else if(type==2)values.put(col,readVaultText(in));
                        else throw new SecurityException("Tipo não reconhecido");
                    }
                    if(t.table.equals("configuracoes")){
                        String k=values.getAsString("chave");
                        if(k==null||!portableConfig(k,diary))throw new SecurityException("Configuração não portátil");
                    }
                    if(t.table.equals("processamento_jobs")&&!"".equals(values.getAsString("audio_path")))throw new SecurityException("Caminho de áudio não portátil");
                    validatePortableRow(db,t,values,clear,diary);
                    db.insertOrThrow(t.table,null,values);
                    count=Math.addExact(count,1);
                }
            }
            if(in.readInt()!=count||in.read()!=-1)throw new SecurityException("Final do backup divergente");
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){if(c.moveToFirst())throw new SecurityException("Referência inconsistente");}
            try(Cursor c=db.query("eco_attachments",new String[]{"id","case_id"},null,null,null,null,"id")){
                while(c.moveToNext()){checkArchiveActive(active);byte[] bytes=attachmentBytes(c.getString(1),c.getString(0));Arrays.fill(bytes,(byte)0);}
            }
            ContentValues migrated=new ContentValues();migrated.put("chave","db_migrated");migrated.put("valor_enc",dbHelper.encrypt("true"));migrated.put("updated_at",System.currentTimeMillis());
            db.insertWithOnConflict("configuracoes",null,migrated,SQLiteDatabase.CONFLICT_REPLACE);
            checkArchiveActive(active);db.setTransactionSuccessful();return count;
        }finally{db.endTransaction();}
    }

    // Companion archive for large attachment collections. Case records must
    // already exist on restore; no existing attachment is overwritten.
    private static final int ATTACHMENT_ARCHIVE_MAGIC=0x45415431, ARCHIVE_META_MAX=65536;
    private static final long ATTACHMENT_ARCHIVE_MAX=8L*1024*1024*1024;
    private static void checkArchiveActive(java.util.function.BooleanSupplier active)throws java.io.IOException{
        if(!active.getAsBoolean())throw new java.io.IOException("Operação cancelada");
    }
    synchronized int exportAttachmentArchive(java.io.OutputStream target,char[] password,java.util.function.BooleanSupplier active)throws Exception{
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);db.beginTransaction();
        try(EcoBackupStream.Writer writer=new EcoBackupStream.Writer(target,password)){
            java.io.DataOutputStream out=new java.io.DataOutputStream(writer);
            out.writeInt(ATTACHMENT_ARCHIVE_MAGIC);long total=8;int count=0;
            try(Cursor c=db.query("eco_attachments",new String[]{"id","case_id","meta_enc"},null,null,null,null,"id ASC")){
                while(c.moveToNext()){
                    checkArchiveActive(active);
                    byte[] meta=dbHelper.decrypt(c.getBlob(2)).getBytes(StandardCharsets.UTF_8);
                    byte[] data=null;
                    try{
                        if(meta.length<1||meta.length>ARCHIVE_META_MAX)throw new java.io.IOException("Metadados acima do limite");
                        data=attachmentBytes(c.getString(1),c.getString(0));
                        total+=8L+meta.length+data.length;
                        if(total>ATTACHMENT_ARCHIVE_MAX)throw new java.io.IOException("Arquivo acima de 8 GiB");
                        out.writeInt(meta.length);out.write(meta);out.writeInt(data.length);out.write(data);count++;
                    }finally{Arrays.fill(meta,(byte)0);if(data!=null)Arrays.fill(data,(byte)0);}
                }
            }
            checkArchiveActive(active);out.writeInt(0);writer.finish();db.setTransactionSuccessful();return count;
        }finally{db.endTransaction();}
    }
    synchronized int restoreAttachmentArchive(java.io.InputStream source,char[] password,java.util.function.BooleanSupplier active)throws Exception{
        SQLiteDatabase db=dbHelper.getWritableDatabase();ensureAttachments(db);db.beginTransaction();
        try(EcoBackupStream.Reader reader=new EcoBackupStream.Reader(source,password,ATTACHMENT_ARCHIVE_MAX)){
            java.io.DataInputStream in=new java.io.DataInputStream(reader);
            if(in.readInt()!=ATTACHMENT_ARCHIVE_MAGIC)throw new SecurityException("Arquivo não é backup de anexos ECO");
            int count=0;String previous=null;
            while(true){
                checkArchiveActive(active);int n=in.readInt();
                if(n==0)break;
                if(n<0||n>ARCHIVE_META_MAX)throw new SecurityException("Metadados inválidos");
                byte[] metaBytes=new byte[n],data=null;
                try{
                    in.readFully(metaBytes);
                    JSONObject meta=new JSONObject(new String(metaBytes,StandardCharsets.UTF_8));
                    String id=meta.getString("id"),caseId=meta.getString("caseId");
                    if(id.isEmpty()||id.length()>200||caseId.isEmpty()||caseId.length()>200||
                       (previous!=null&&previous.compareTo(id)>=0))throw new SecurityException("ID de anexo inválido ou duplicado");
                    previous=id;
                    if(!caseExists(db,caseId))throw new SecurityException("Caso ausente: restaure primeiro o backup clínico");
                    int size=in.readInt();
                    if(size<8||size>ATTACHMENT_LIMIT||size!=meta.getInt("bytes"))throw new SecurityException("Tamanho inválido");
                    data=new byte[size];in.readFully(data);
                    if(!attachmentMime(data).equals(meta.getString("mime"))||!sha256(data).equals(meta.getString("sha256")))throw new SecurityException("Anexo corrompido");
                    boolean exists=false;
                    try(Cursor c=db.query("eco_attachments",new String[]{"case_id","meta_enc"},"id=?",new String[]{id},null,null,null)){
                        if(c.moveToFirst()){
                            if(!caseId.equals(c.getString(0))||!meta.getString("sha256").equals(new JSONObject(dbHelper.decrypt(c.getBlob(1))).getString("sha256")))throw new SecurityException("Conflito de anexo");
                            exists=true;
                        }
                    }
                    if(exists){byte[] local=attachmentBytes(caseId,id);Arrays.fill(local,(byte)0);}
                    else {insertAttachment(db,meta,data);count++;}
                }finally{Arrays.fill(metaBytes,(byte)0);if(data!=null)Arrays.fill(data,(byte)0);}
            }
            if(in.read()!=-1)throw new SecurityException("Dados após o final do arquivo");
            checkArchiveActive(active);db.setTransactionSuccessful();return count;
        }finally{db.endTransaction();}
    }

    // Capture all portable SQLite data at the same transaction boundary.
    // Private diary remains opt-in and is not copied into recovery metadata.
    synchronized JSONObject exportPortableSnapshot(boolean includePrivateDiary) throws Exception {
        return exportPortableSnapshot(includePrivateDiary,true);
    }
    synchronized JSONObject exportPortableSnapshot(boolean includePrivateDiary,boolean includeAttachments) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        db.beginTransaction();
        try{
            JSONObject export=new JSONObject(readSnapshot(db));
            JSONArray cases=export.getJSONArray("cases");
            for(int i=0;i<cases.length();i++){
                JSONObject single=cases.getJSONObject(i);single.remove("diarioNotas");
                if(includePrivateDiary)single.put("diarioNotas",new JSONArray(readPrivateDiary(single.getString("id"))));
            }
            String draft=readDraft();
            if(!draft.isEmpty()){
                JSONObject journal=new JSONObject(draft);
                if(journal.has("__loadError"))throw new java.io.IOException("Rascunho de áudio indisponível; backup não criado");
                export.put("mlDraft",journal);
            }
            export.put("attachmentBundle",includeAttachments?exportAttachments():new JSONArray());
            export.put("nativeRecoveryBundle",exportNativeRecovery(includePrivateDiary));
            db.setTransactionSuccessful();
            return export;
        }finally{db.endTransaction();}
    }

    // Separate slots for incomplete registrations and appointment forms.
    // They never enter the clinical record until explicitly saved.
    private static String editorKey(String slot) {
        if(!"wizard".equals(slot)&&!"appointment".equals(slot))
            throw new IllegalArgumentException("Editor inválido");
        return "editor_draft:"+slot;
    }
    private static JSONObject validateEditor(String slot,String json) throws Exception {
        editorKey(slot);
        if(json==null||json.length()>2_000_000)throw new IllegalArgumentException("Rascunho acima do limite");
        JSONObject data=new JSONObject(json),value=data.getJSONObject("value");
        if(!slot.equals(data.optString("slot"))||value.optString("id").trim().isEmpty())
            throw new IllegalArgumentException("Rascunho inválido");
        if("wizard".equals(slot)&&(data.optInt("step",-1)<0||data.optInt("step",-1)>3))
            throw new IllegalArgumentException("Etapa inválida");
        if("appointment".equals(slot)&&value.optString("caseId").isEmpty())
            throw new IllegalArgumentException("Consulta sem caso");
        return data;
    }
    synchronized void saveEditorDraft(String slot,String json) throws Exception {
        JSONObject data=validateEditor(slot,json);
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        db.beginTransaction();
        try{
            if("appointment".equals(slot)&&!caseExists(db,data.getJSONObject("value").getString("caseId")))
                throw new IllegalArgumentException("Caso inexistente");
            String key=editorKey(slot);
            String old=readEditorDraft(slot);
            if(!old.isEmpty()&&!new JSONObject(old).getJSONObject("value").getString("id").equals(data.getJSONObject("value").getString("id")))
                throw new IllegalArgumentException("Outro rascunho aguarda recuperação");
            ContentValues cv=new ContentValues();cv.put("chave",key);cv.put("valor_enc",encryptRecord(json));
            cv.put("updated_at",System.currentTimeMillis());
            if(db.update("configuracoes",cv,"chave=?",new String[]{key})==0)db.insertOrThrow("configuracoes",null,cv);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    synchronized String readEditorDraft(String slot) throws Exception {
        String key=editorKey(slot);
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        try(Cursor c=db.query("configuracoes",new String[]{smallBlob("valor_enc")},"chave=?",new String[]{key},null,null,null)){
            return c.moveToFirst()?dbHelper.decrypt(readBoundedBlob(db,c,"configuracoes","valor_enc","chave=?",new String[]{key})):"";
        }
    }
    synchronized void clearEditorDraft(String slot,String id) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();db.beginTransaction();
        try{
            String old=readEditorDraft(slot);
            if(!old.isEmpty()&&id.equals(new JSONObject(old).getJSONObject("value").getString("id")))
                db.delete("configuracoes","chave=?",new String[]{editorKey(slot)});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static String clinicalDraftKey(String caseId,String kind){
        if(caseId==null||caseId.isEmpty()||caseId.length()>200||(!java.util.Arrays.asList("anamnese","enquadre","sonhos","genograma","cronologia").contains(kind)&&!kind.matches("res_(ficha|rede|prep|post|question|qedit|axis|hypo|interv|relation|safety|diary|custom|finance|template|personal|doctemplate|aiarchive|attachment)(_[A-Za-z0-9_-]{1,100})?")))
            throw new IllegalArgumentException("Formulário clínico inválido");
        boolean admin=java.util.Arrays.asList("res_finance","res_template","res_personal","res_doctemplate").contains(kind);
        if(admin!="__eco_admin__".equals(caseId))throw new IllegalArgumentException("Escopo de rascunho inválido");
        return "clinical_draft:"+kind+":"+caseId;
    }
    synchronized String readClinicalDraft(String caseId,String kind) throws Exception {
        String key=clinicalDraftKey(caseId,kind);SQLiteDatabase db=dbHelper.getReadableDatabase();
        try(Cursor c=db.query("configuracoes",new String[]{smallBlob("valor_enc")},"chave=?",new String[]{key},null,null,null)){
            return c.moveToFirst()?dbHelper.decrypt(readBoundedBlob(db,c,"configuracoes","valor_enc","chave=?",new String[]{key})):"";
        }
    }
    synchronized void saveClinicalDraft(String caseId,String kind,String raw) throws Exception {
        String key=clinicalDraftKey(caseId,kind);
        if(raw==null||raw.length()>2_000_000)throw new IllegalArgumentException("Rascunho acima do limite");
        JSONObject data=new JSONObject(raw),fields=data.getJSONObject("fields"),base=data.getJSONObject("base");
        if(!caseId.equals(data.optString("caseId"))||!kind.equals(data.optString("kind"))||data.optString("id").isEmpty()
                ||fields.length()==0||fields.length()>150||base.length()!=fields.length())throw new IllegalArgumentException("Rascunho inválido");
        java.util.Iterator<String> keys=fields.keys();
        while(keys.hasNext()){
            String field=keys.next();
            if(!field.matches("[A-Za-z0-9_]{1,100}")||!(fields.opt(field) instanceof String)||!(base.opt(field) instanceof String))
                throw new IllegalArgumentException("Campo inválido");
        }
        SQLiteDatabase db=dbHelper.getWritableDatabase();db.beginTransaction();
        try{
            if(!"__eco_admin__".equals(caseId)&&!caseExists(db,caseId))throw new IllegalArgumentException("Caso inexistente");
            String old=readClinicalDraft(caseId,kind);
            if(!old.isEmpty()&&!data.getString("id").equals(new JSONObject(old).getString("id")))throw new IllegalArgumentException("Rascunho divergente");
            ContentValues cv=new ContentValues();cv.put("chave",key);cv.put("valor_enc",encryptRecord(raw));cv.put("updated_at",System.currentTimeMillis());
            if(db.update("configuracoes",cv,"chave=?",new String[]{key})==0)db.insertOrThrow("configuracoes",null,cv);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    synchronized void clearClinicalDraft(String caseId,String kind,String id) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();db.beginTransaction();
        try{
            String old=readClinicalDraft(caseId,kind);
            if(!old.isEmpty()){
                if(!id.equals(new JSONObject(old).getString("id")))throw new SecurityException("Rascunho divergente");
                db.delete("configuracoes","chave=?",new String[]{clinicalDraftKey(caseId,kind)});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    // Portable backup stores decrypted journal data only inside the password-encrypted envelope.
    // Device-bound ciphertext must never be exported as a portable record.
    synchronized JSONObject exportNativeRecovery() throws Exception {return exportNativeRecovery(false);}
    synchronized JSONObject exportNativeRecovery(boolean includePrivateDiary) throws Exception {
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ensureRevisionTable(db);
            JSONObject bundle=new JSONObject().put("version",1);
            JSONArray drafts=new JSONArray(), revisions=new JSONArray();
            try(Cursor c=db.query("configuracoes",new String[]{"chave",smallBlob("valor_enc")},
                    "chave LIKE ?",new String[]{"form_draft:%"},null,null,"chave ASC")){
                while(c.moveToNext()){
                    String key=c.getString(0),caseId=key.substring("form_draft:".length());
                    String payload=dbHelper.decrypt(readBoundedBlob(db,c,"configuracoes","valor_enc","chave=?",new String[]{key}));
                    drafts.put(new JSONObject().put("caseId",caseId).put("payload",new JSONObject(payload)));
                }
            }
            try(Cursor c=db.query("eco_revisions",new String[]{"seq","kind","case_id","entity_id","captured_at",smallBlob("payload_enc")},null,null,null,null,"seq ASC")){
                while(c.moveToNext()){
                    String payload=dbHelper.decrypt(readBoundedBlob(db,c,"eco_revisions","payload_enc","seq=?",new String[]{Long.toString(c.getLong(0))}));
                    revisions.put(new JSONObject().put("kind",c.getString(1)).put("caseId",c.getString(2))
                        .put("entityId",c.getString(3)).put("capturedAt",c.getLong(4)).put("payload",new JSONObject(payload)));
                }
            }
            JSONArray editors=new JSONArray();
            for(String slot:new String[]{"wizard","appointment"}){String editor=readEditorDraft(slot);if(!editor.isEmpty())editors.put(new JSONObject(editor));}
            JSONArray clinical=new JSONArray();
            try(Cursor c=db.query("configuracoes",new String[]{"chave",smallBlob("valor_enc")},"chave LIKE ?",new String[]{"clinical_draft:%"},null,null,"chave ASC")){
                while(c.moveToNext()){
                    String k=c.getString(0);
                    if(k.startsWith("clinical_draft:res_diary:")&&!includePrivateDiary)continue;
                    clinical.put(new JSONObject(dbHelper.decrypt(readBoundedBlob(db,c,"configuracoes","valor_enc","chave=?",new String[]{k}))));
                }
            }
            bundle.put("drafts",drafts).put("revisions",revisions).put("editors",editors).put("clinical",clinical);
            db.setTransactionSuccessful();
            return bundle;
        }finally{db.endTransaction();}
    }

    private static boolean caseExists(SQLiteDatabase db,String caseId) {
        try(Cursor c=db.query("_pacientes",new String[]{"id"},"id=?",new String[]{caseId},null,null,null)){
            return c.moveToFirst();
        }
    }

    // Called inside save()'s transaction, after patient and session rows have been merged.
    // A conflicting draft aborts the entire import instead of destroying either version.
    private void restoreNativeRecovery(SQLiteDatabase db,JSONObject bundle) throws Exception {
        if(bundle==null)return;
        if(bundle.optInt("version")!=1)throw new IllegalArgumentException("Versão do histórico de recuperação incompatível");
        JSONArray drafts=bundle.getJSONArray("drafts"), revisions=bundle.getJSONArray("revisions");
        java.util.Set<String> seenDrafts=new java.util.HashSet<>();
        for(int i=0;i<drafts.length();i++){
            JSONObject row=drafts.getJSONObject(i),payload=row.getJSONObject("payload");
            String caseId=row.getString("caseId"),key="form_draft:"+caseId;
            if(!seenDrafts.add(caseId)||!caseExists(db,caseId)||!caseId.equals(payload.optString("caseId"))
                    ||!(payload.opt("session") instanceof JSONObject)||payload.toString().length()>5_000_000)
                throw new IllegalArgumentException("Rascunho de recuperação inválido");
            String data=payload.toString();
            try(Cursor c=db.query("configuracoes",new String[]{smallBlob("valor_enc")},"chave=?",new String[]{key},null,null,null)){
                if(c.moveToFirst()){
                    String old=dbHelper.decrypt(readBoundedBlob(db,c,"configuracoes","valor_enc","chave=?",new String[]{key}));
                    if(!old.equals(data))throw new IllegalArgumentException("Há rascunho local diferente. Finalize ou exporte o rascunho antes de mesclar.");
                    continue;
                }
            }
            ContentValues cv=new ContentValues();cv.put("chave",key);cv.put("valor_enc",encryptRecord(data));
            cv.put("updated_at",System.currentTimeMillis());db.insertOrThrow("configuracoes",null,cv);
        }
        JSONArray editors=bundle.optJSONArray("editors");
        if(bundle.has("editors")&&editors==null)throw new IllegalArgumentException("Editores inválidos");
        java.util.Set<String> slots=new java.util.HashSet<>();
        if(editors!=null)for(int i=0;i<editors.length();i++){
            JSONObject editor=editors.getJSONObject(i);String slot=editor.getString("slot");
            if(!slots.add(slot))throw new IllegalArgumentException("Editor duplicado");
            String old=readEditorDraft(slot);
            if(!old.isEmpty()&&!old.equals(editor.toString()))throw new IllegalArgumentException("Rascunho de editor local divergente");
            if(old.isEmpty())saveEditorDraft(slot,editor.toString());
        }
        JSONArray clinical=bundle.optJSONArray("clinical");
        if(bundle.has("clinical")&&clinical==null)throw new IllegalArgumentException("Rascunhos clínicos inválidos");
        java.util.Set<String> seenClinical=new java.util.HashSet<>();
        if(clinical!=null)for(int i=0;i<clinical.length();i++){
            JSONObject item=clinical.getJSONObject(i);String caseId=item.getString("caseId"),kind=item.getString("kind");
            if(!seenClinical.add(clinicalDraftKey(caseId,kind)))throw new IllegalArgumentException("Rascunho clínico duplicado");
            String old=readClinicalDraft(caseId,kind);
            if(!old.isEmpty()&&!old.equals(item.toString()))throw new IllegalArgumentException("Rascunho clínico local divergente");
            if(old.isEmpty())saveClinicalDraft(caseId,kind,item.toString());
        }
        for(int i=0;i<revisions.length();i++){
            JSONObject row=revisions.getJSONObject(i),payload=row.getJSONObject("payload");
            String kind=row.getString("kind"),caseId=row.getString("caseId"),entityId=row.getString("entityId");
            long captured=row.getLong("capturedAt");
            if((!"session".equals(kind)&&!"agenda".equals(kind))||!caseExists(db,caseId)
                    ||entityId.isEmpty()||!entityId.equals(payload.optString("id"))||captured<=0)
                throw new IllegalArgumentException("Revisão de recuperação inválida");
            if("session".equals(kind)){
                try(Cursor c=db.query("sessoes",new String[]{"case_id"},"id=?",new String[]{entityId},null,null,null)){
                    if(c.moveToFirst()&&!caseId.equals(c.getString(0)))throw new SecurityException("Histórico de sessão pertence a outro caso");
                }
            }else if(!caseId.equals(payload.optString("caseId")))throw new SecurityException("Histórico de agenda pertence a outro caso");
            String data=payload.toString();boolean exists=false;
            try(Cursor c=db.query("eco_revisions",new String[]{"seq",smallBlob("payload_enc")},
                    "kind=? AND case_id=? AND entity_id=? AND captured_at=?",new String[]{kind,caseId,entityId,Long.toString(captured)},null,null,null)){
                while(c.moveToNext()){
                    String old=dbHelper.decrypt(readBoundedBlob(db,c,"eco_revisions","payload_enc","seq=?",new String[]{Long.toString(c.getLong(0))}));
                    if(old.equals(data)){exists=true;break;}
                }
            }
            if(!exists){
                ContentValues cv=new ContentValues();cv.put("kind",kind);cv.put("case_id",caseId);cv.put("entity_id",entityId);
                cv.put("captured_at",captured);cv.put("payload_enc",encryptRecord(data));db.insertOrThrow("eco_revisions",null,cv);
            }
        }
    }

    private byte[] encryptRecord(String payload) throws Exception {
        byte[] encrypted=dbHelper.encrypt(payload);
        if(encrypted.length>MAX_RECORD_BLOB)throw new java.io.IOException("Registro excede o limite de leitura; alteração não gravada");
        return encrypted;
    }
    synchronized void save(String json) throws Exception {
        JSONObject validated = new JSONObject(json);
        if (validated.optInt("version") < 1 || !(validated.opt("cases") instanceof JSONArray)
                || !(validated.opt("appointments") instanceof JSONArray))
            throw new IllegalArgumentException("Estrutura de dados inválida");

        JSONArray cases = validated.getJSONArray("cases");
        JSONArray appts = validated.getJSONArray("appointments");
        validateSnapshot(cases, appts);
        if(validated.has("attachmentBundle")&&!(validated.opt("attachmentBundle") instanceof JSONArray))throw new IllegalArgumentException("Anexos inválidos");

        if(validated.has("nativeRecoveryBundle")&&!(validated.opt("nativeRecoveryBundle") instanceof JSONObject))throw new IllegalArgumentException("Histórico de recuperação inválido");
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ensureRevisionTable(db);
            // Save root configurations to preserve preferences, active engine and UI state
            JSONObject rootMeta = new JSONObject(json);
            rootMeta.remove("cases");
            rootMeta.remove("appointments");
            rootMeta.remove("attachmentBundle");
            rootMeta.remove("nativeRecoveryBundle");
            ContentValues cfgCv = new ContentValues();
            cfgCv.put("chave", "app_state_meta");
            cfgCv.put("valor_enc", encryptRecord(rootMeta.toString()));
            cfgCv.put("updated_at", System.currentTimeMillis());
            int cfgUpdated = db.update("configuracoes", cfgCv, "chave = ?", new String[]{"app_state_meta"});
            if (cfgUpdated == 0) {
                db.insertOrThrow("configuracoes", null, cfgCv);
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
                cv.put("payload_enc", encryptRecord(payload.toString()));

                // Atomic update in place to PREVENT cascade wipeout of child entities
                int updated = db.update("_pacientes", cv, "id = ?", new String[]{caseId});
                if (updated == 0) {
                    db.insertOrThrow("_pacientes", null, cv);
                }

                if (legacyDiary != null && legacyDiary.length() > 0 && "[]".equals(dbHelper.readPrivateDiary(caseId))) {
                    dbHelper.savePrivateDiary(caseId, legacyDiary.toString());
                }
                if (sessoes != null) {
                    // Compare authenticated existing payloads in one scoped query.
                    // Unchanged sessions retain their ciphertext and timestamps, avoiding
                    // WAL amplification when only a patient field or another session changes.
                    java.util.Map<String, byte[]> existingSessions = new java.util.HashMap<>();
                    try (Cursor existing = db.query("sessoes", new String[]{"id", smallBlob("payload_enc")},
                            "case_id = ?", new String[]{caseId}, null, null, null)) {
                        while (existing.moveToNext()) {
                            existingSessions.put(existing.getString(0), existing.getBlob(1));
                        }
                    }
                    for (int s = 0; s < sessoes.length(); s++) {
                        JSONObject sObj = sessoes.getJSONObject(s);
                        String sId = sObj.getString("id");
                        String sData = sObj.optString("data", "");
                        String sOrigem = sObj.optString("origem", "audio");

                        String sessionPayload = sObj.toString();
                        byte[] existingPayload = existingSessions.get(sId);
                        if(existingPayload==null&&existingSessions.containsKey(sId))existingPayload=readLargeBlob(db,"sessoes","payload_enc","case_id=? AND id=?",new String[]{caseId,sId});
                        if (existingPayload != null && sessionPayload.equals(dbHelper.decrypt(existingPayload))) {
                            continue;
                        }
                        retainRevision(db,"session",caseId,sId,existingPayload);

                        ContentValues scv = new ContentValues();
                        scv.put("id", sId);
                        scv.put("case_id", caseId);
                        scv.put("data", sData);
                        if (sObj.has("hora")) scv.put("hora", sObj.optString("hora"));
                        scv.put("origem", sOrigem);
                        scv.put("created_at", sObj.optLong("created_at", System.currentTimeMillis()));
                        scv.put("updated_at", System.currentTimeMillis());
                        scv.put("payload_enc", encryptRecord(sessionPayload));

                        // Atomic update in place for sessions
                        int sUpdated = db.update("sessoes", scv, "id = ? AND case_id = ?", new String[]{sId, caseId});
                        if (sUpdated == 0) {
                            db.insertOrThrow("sessoes", null, scv);
                        }
                    }
                }
            }

            // Preserve prior appointments transactionally when their payload is modified or removed.
            java.util.Map<String,byte[]> oldAgenda=new java.util.HashMap<>();
            java.util.Map<String,String> oldAgendaCase=new java.util.HashMap<>();
            try(Cursor c=db.query("agenda",new String[]{"id","case_id",smallBlob("payload_enc")},null,null,null,null,null)){
                while(c.moveToNext()){oldAgenda.put(c.getString(0),readBoundedBlob(db,c,"agenda","payload_enc","id=?",new String[]{c.getString(0)}));oldAgendaCase.put(c.getString(0),c.getString(1));}
            }
            java.util.Set<String> nextAgenda=new java.util.HashSet<>();
            for(int i=0;i<appts.length();i++){
                JSONObject a=appts.getJSONObject(i);String aid=a.getString("id");nextAgenda.add(aid);
                byte[] old=oldAgenda.get(aid);if(old!=null&&!a.toString().equals(dbHelper.decrypt(old)))retainRevision(db,"agenda",oldAgendaCase.get(aid),aid,old);
            }
            for(String aid:oldAgenda.keySet())if(!nextAgenda.contains(aid))retainRevision(db,"agenda",oldAgendaCase.get(aid),aid,oldAgenda.get(aid));
            db.delete("agenda", null, null);
            for (int i = 0; i < appts.length(); i++) {
                JSONObject aObj = appts.getJSONObject(i);
                ContentValues acv = new ContentValues();
                acv.put("id", aObj.getString("id"));
                acv.put("case_id", aObj.optString("caseId", ""));
                acv.put("data", aObj.optString("data", ""));
                acv.put("hora", aObj.optString("hora", ""));
                acv.put("created_at", System.currentTimeMillis());
                acv.put("payload_enc", encryptRecord(aObj.toString()));
                db.insertOrThrow("agenda", null, acv);
            }

            restoreAttachments(db,validated.optJSONArray("attachmentBundle"));
            restoreNativeRecovery(db,validated.optJSONObject("nativeRecoveryBundle"));
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
        if (pass == null || pass.isEmpty()) throw new IllegalArgumentException("Senha do backup não informada");
        PBEKeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, 240000, 256);
        byte[] derived = null;
        try {
            try {
                derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                if (derived == null || derived.length != 32) throw new java.security.InvalidKeyException("PBKDF2 retornou chave inválida");
            } catch (java.security.GeneralSecurityException providerFailure) {
                // Same standard, same parameters and password encoding; no weaker key or ephemeral vault fallback.
                derived = pbkdf2Utf8(pass, salt, 240000);
            }
            return new SecretKeySpec(derived, "AES");
        } finally { spec.clearPassword(); if (derived != null) Arrays.fill(derived, (byte)0); }
    }

    static byte[] pbkdf2Utf8(String pass, byte[] salt, int iterations) throws Exception {
        if(pass == null || pass.isEmpty() || iterations < 1) throw new IllegalArgumentException("PBKDF2 inválido");
        byte[] password=pass.getBytes(StandardCharsets.UTF_8);
        byte[] first=Arrays.copyOf(salt,salt.length+4);
        first[first.length-1]=1;
        byte[] u=null,result=null;
        try {
            javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(password,"HmacSHA256"));
            u=mac.doFinal(first);result=u.clone();
            for(int i=1;i<iterations;i++){
                byte[] next=mac.doFinal(u);Arrays.fill(u,(byte)0);u=next;
                for(int j=0;j<result.length;j++) result[j]^=u[j];
            }
            return result;
        }finally{Arrays.fill(password,(byte)0);Arrays.fill(first,(byte)0);if(u!=null)Arrays.fill(u,(byte)0);}
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
        if (pkg.optInt("version") == 1 && pkg.opt("cases") instanceof JSONArray && !pkg.has("format")) return input;
        if (!"tutor-clinico-encrypted-backup".equals(pkg.optString("format")) || pkg.optInt("version") != 1)
            throw new IllegalArgumentException("Formato não reconhecido");
        if (pass == null || pass.isEmpty()) throw new IllegalArgumentException("Informe a senha original do backup criptografado antes de selecionar o arquivo.");
        if (pkg.optInt("iterations") != 240000) throw new IllegalArgumentException("Parâmetros de derivação não suportados");
        byte[] salt = Base64.decode(pkg.getString("salt"), Base64.NO_WRAP);
        byte[] iv = Base64.decode(pkg.getString("iv"), Base64.NO_WRAP);
        if (salt.length != 16 || iv.length != 12) throw new IllegalArgumentException("Backup inválido");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, passKey(pass, salt), new GCMParameterSpec(128, iv));
        try { return new String(cipher.doFinal(Base64.decode(pkg.getString("ciphertext"), Base64.NO_WRAP)), StandardCharsets.UTF_8); }
        catch (javax.crypto.AEADBadTagException e) { throw new SecurityException("Senha incorreta ou backup alterado/corrompido. Nenhum dado foi importado.", e); }
    }
}
