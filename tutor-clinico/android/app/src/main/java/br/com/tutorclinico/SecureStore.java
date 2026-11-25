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
                        seg.put("cluster", role.startsWith("falante") ? role : "");
                        seg.put("speaker", role.startsWith("falante") ? "desconhecido" : role);
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

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ensureRevisionTable(db);
            // Save root configurations to preserve preferences, active engine and UI state
            JSONObject rootMeta = new JSONObject(json);
            rootMeta.remove("cases");
            rootMeta.remove("appointments");
            rootMeta.remove("attachmentBundle");
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
