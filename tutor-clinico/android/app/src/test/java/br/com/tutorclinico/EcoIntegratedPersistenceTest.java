package br.com.tutorclinico;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class EcoIntegratedPersistenceTest {
    private static JSONObject caseWith(String id,String text) throws Exception {
        String sid=id+"-s";
        JSONObject source=new JSONObject().put("scope","manual").put("sessionId",sid)
            .put("sessionDate","2026-11-25").put("quote",text).put("source","nota clínica manual").put("atMs",JSONObject.NULL);
        JSONObject session=new JSONObject().put("id",sid).put("origem","manual").put("data","2026-11-25").put("hora","09:00").put("texto",text);
        return new JSONObject().put("id",id).put("nome","Fictício "+id)
            .put("sessoes",new JSONArray().put(session))
            .put("anamnese",new JSONObject().put("padroesSono",text))
            .put("ecoReviewRecords",new JSONArray().put(new JSONObject(source.toString()).put("id",id+"-ref").put("type","anamnese").put("key","padroesSono")))
            .put("ecoAiArchive",new JSONArray().put(new JSONObject().put("id",id+"-ai").put("reviewed",true).put("scope","manual").put("text",text).put("sources",new JSONArray().put(source))))
            .put("ecoQuestions",new JSONArray().put(new JSONObject().put("id",id+"-q").put("text",text).put("status","respondida").put("history",new JSONArray().put(new JSONObject().put("status","aberta")))))
            .put("ecoNetwork",new JSONArray().put(new JSONObject().put("id",id+"-net").put("name",text).put("kind","amizade")))
            .put("sonhosList",new JSONArray().put(new JSONObject().put("id",id+"-dream").put("sessionId",sid).put("texto",text)))
            .put("genograma",new JSONObject().put("membros",new JSONArray()).put("relacoes",new JSONArray()))
            .put("ecoPreparation",new JSONObject().put("goals",text))
            .put("ecoVisitClosure",new JSONObject().put(sid,new JSONObject().put("summary",text).put("status","revisado").put("history",new JSONArray())));
    }
    private static JSONObject byId(JSONObject root,String id) throws Exception {
        JSONArray a=root.getJSONArray("cases");
        for(int i=0;i<a.length();i++)if(id.equals(a.getJSONObject(i).getString("id")))return a.getJSONObject(i);
        throw new AssertionError("Case missing "+id);
    }
    @Test public void newModulesSurviveReopenAndBackupWithoutCrossCaseLeak() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        SecureStore store=new SecureStore(context,key);
        JSONObject a=caseWith("integrated-a","MARCADOR_PRIVADO_A_7351"),b=caseWith("integrated-b","MARCADOR_PRIVADO_B_9264");
        JSONObject root=new JSONObject().put("version",1).put("uiTheme","dark").put("cases",new JSONArray().put(a).put(b)).put("appointments",new JSONArray());
        store.save(root.toString());
        try(Cursor rows=store.getDbHelper().getReadableDatabase().query("_pacientes",new String[]{"payload_enc"},null,null,null,null,null)){
            while(rows.moveToNext()){
                String raw=new String(rows.getBlob(0),StandardCharsets.UTF_8);
                assertFalse(raw.contains("MARCADOR_PRIVADO_A_7351"));assertFalse(raw.contains("MARCADOR_PRIVADO_B_9264"));
            }
        }
        store.getDbHelper().close();
        store=new SecureStore(context,key);
        JSONObject restored=new JSONObject(store.read());
        assertEquals("dark",restored.getString("uiTheme"));
        for(JSONObject expected:new JSONObject[]{a,b}){
            JSONObject actual=byId(restored,expected.getString("id"));
            for(String field:new String[]{"anamnese","ecoReviewRecords","ecoAiArchive","ecoQuestions","ecoNetwork","sonhosList","genograma","ecoPreparation","ecoVisitClosure"})
                assertEquals(field,expected.get(field).toString(),actual.get(field).toString());
            assertEquals(expected.getJSONArray("sessoes").getJSONObject(0).getString("texto"),actual.getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        }
        assertFalse(byId(restored,"integrated-a").toString().contains("MARCADOR_PRIVADO_B_9264"));
        assertFalse(byId(restored,"integrated-b").toString().contains("MARCADOR_PRIVADO_A_7351"));
        byte[] backup=SecureStore.encryptBackup(restored.toString(),"eco-fixture-2026");
        assertEquals(restored.toString(),SecureStore.decryptBackup(backup,"eco-fixture-2026"));
        JSONObject edited=byId(restored,"integrated-a");
        edited.getJSONArray("ecoAiArchive").getJSONObject(0).put("text","REVISADO_A");
        store.save(restored.toString());
        store.getDbHelper().close();store=new SecureStore(context,key);
        JSONObject reread=new JSONObject(store.read());
        assertEquals("REVISADO_A",byId(reread,"integrated-a").getJSONArray("ecoAiArchive").getJSONObject(0).getString("text"));
        assertEquals(b.getJSONArray("ecoAiArchive").toString(),byId(reread,"integrated-b").getJSONArray("ecoAiArchive").toString());
        try(Cursor c=store.getDbHelper().getReadableDatabase().rawQuery("PRAGMA foreign_key_check",null)){assertEquals(0,c.getCount());}
        store.getDbHelper().close();
    }

    @Test public void scaleOneHundredCasesTenThousandSessions() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecureStore store=new SecureStore(context,new SecretKeySpec(new byte[32],"AES"));
        JSONArray cases=new JSONArray();
        for(int i=0;i<100;i++){
            String id="scale-"+i;
            JSONArray sessions=new JSONArray();
            for(int j=0;j<100;j++){
                sessions.put(new JSONObject().put("id",id+"-s"+j).put("data","2026-11-25")
                    .put("hora","10:00").put("origem","manual").put("texto","Nota fictícia "+id+" / "+j));
            }
            cases.put(new JSONObject().put("id",id).put("nome","Fictício "+i).put("sessoes",sessions));
        }
        JSONObject root=new JSONObject().put("version",1).put("cases",cases).put("appointments",new JSONArray());
        long start=System.nanoTime();
        store.save(root.toString());
        long saved=System.nanoTime();
        store.getDbHelper().close();
        store=new SecureStore(context,new SecretKeySpec(new byte[32],"AES"));
        JSONObject result=new JSONObject(store.read());
        long read=System.nanoTime();
        int total=0;
        for(int i=0;i<100;i++){
            JSONObject c=byId(result,"scale-"+i);
            JSONArray sessions=c.getJSONArray("sessoes");assertEquals(100,sessions.length());
            java.util.HashSet<String> ids=new java.util.HashSet<>();
            for(int j=0;j<sessions.length();j++){
                JSONObject s=sessions.getJSONObject(j);
                assertTrue(s.getString("id").startsWith("scale-"+i+"-s"));
                assertTrue(s.getString("texto").startsWith("Nota fictícia scale-"+i+" / "));
                assertTrue(ids.add(s.getString("id")));total++;
            }
        }
        assertEquals(10000,total);
        try(Cursor cursor=store.getDbHelper().getReadableDatabase().rawQuery("PRAGMA foreign_key_check",null)){assertEquals(0,cursor.getCount());}
        System.out.println("ECO_SCALE_HOST cases=100 sessions="+total+" save_ms="+(saved-start)/1000000+" reopen_read_ms="+(read-saved)/1000000+" bytes="+result.toString().getBytes(StandardCharsets.UTF_8).length);
        store.getDbHelper().close();
    }

    @Test public void batchedSegmentsPreserveOrderSpeakerAndPatientIsolation() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        SecureStore store=new SecureStore(context,key);
        JSONObject a=caseWith("batch-a",""), b=caseWith("batch-b","");
        a.getJSONArray("sessoes").put(new JSONObject().put("id","batch-a-manual")
            .put("data","2026-11-25").put("origem","manual").put("texto","Nota independente"));
        store.save(new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b))
            .put("appointments",new JSONArray()).toString());
        EcoDatabaseHelper db=store.getDbHelper();
        db.createJob("batch-job-a","batch-a","batch-a-s","fixture-a",new byte[32]);
        db.createJob("batch-job-b","batch-b","batch-b-s","fixture-b",new byte[32]);
        db.updateJobState("batch-job-a","PROCESSING","");
        db.updateJobState("batch-job-b","PROCESSING","");
        assertTrue(db.commitSegment("batch-job-a","batch-a","batch-a-s",200,300,"falante2","segundo",null));
        assertTrue(db.commitSegment("batch-job-a","batch-a","batch-a-s",0,100,"falante1","primeiro",null));
        assertTrue(db.commitSegment("batch-job-b","batch-b","batch-b-s",0,100,"falante1","exclusivo B",null));
        assertFalse(db.commitSegment("batch-job-a","batch-b","batch-b-s",300,400,"falante1","vazamento",null));
        db.close();
        store=new SecureStore(context,key);
        JSONObject restored=new JSONObject(store.read());
        JSONObject patientA=byId(restored,"batch-a"),patientB=byId(restored,"batch-b");
        JSONArray sessions=patientA.getJSONArray("sessoes");
        JSONObject audio=null,manual=null;
        for(int i=0;i<sessions.length();i++){
            JSONObject session=sessions.getJSONObject(i);
            if("batch-a-s".equals(session.getString("id")))audio=session;
            if("batch-a-manual".equals(session.getString("id")))manual=session;
        }
        assertNotNull(audio);assertNotNull(manual);
        JSONArray segments=audio.getJSONArray("segmentos");
        assertEquals(2,segments.length());
        assertEquals("primeiro",segments.getJSONObject(0).getString("text"));
        assertEquals("segundo",segments.getJSONObject(1).getString("text"));
        assertEquals("falante1",segments.getJSONObject(0).getString("cluster"));
        assertEquals("desconhecido",segments.getJSONObject(0).getString("speaker"));
        assertEquals("primeiro segundo",audio.getString("texto"));
        assertEquals("Nota independente",manual.getString("texto"));
        assertFalse(manual.has("segmentos"));
        assertFalse(patientA.toString().contains("exclusivo B"));
        assertFalse(patientB.toString().contains("primeiro"));
        assertEquals("exclusivo B",patientB.getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        store.getDbHelper().close();
    }

    private static byte[] sessionCiphertext(SecureStore store,String id) {
        try(Cursor c=store.getDbHelper().getReadableDatabase().query("sessoes",
                new String[]{"payload_enc"},"id = ?",new String[]{id},null,null,null)){
            assertTrue(c.moveToFirst());return c.getBlob(0);
        }
    }

    @Test public void unchangedSessionRetainsCiphertextAcrossEditsAndReopen() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        SecureStore store=new SecureStore(context,key);
        JSONObject a=caseWith("incremental-a","primeira nota"),b=caseWith("incremental-b","segunda nota");
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b))
            .put("appointments",new JSONArray());
        store.save(root.toString());
        byte[] originalA=sessionCiphertext(store,"incremental-a-s");
        byte[] originalB=sessionCiphertext(store,"incremental-b-s");
        a.put("nome","Paciente fictício renomeado");
        store.save(root.toString());
        assertArrayEquals(originalA,sessionCiphertext(store,"incremental-a-s"));
        assertArrayEquals(originalB,sessionCiphertext(store,"incremental-b-s"));
        store.getDbHelper().close();store=new SecureStore(context,key);
        a.getJSONArray("sessoes").getJSONObject(0).put("texto","nota revisada");
        store.save(root.toString());
        assertFalse(java.util.Arrays.equals(originalA,sessionCiphertext(store,"incremental-a-s")));
        assertArrayEquals(originalB,sessionCiphertext(store,"incremental-b-s"));
        JSONObject restored=new JSONObject(store.read());
        assertEquals("nota revisada",byId(restored,"incremental-a").getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        assertEquals("segunda nota",byId(restored,"incremental-b").getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        store.getDbHelper().close();
    }

    @Test public void duplicateIdsRejectedAndImportedTextSurvivesReopen() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        SecureStore store=new SecureStore(context,key);
        String transcript="Paciente: Texto fictício com acentos.\\nTerapeuta: Pergunta fictícia.";
        JSONObject a=caseWith("import-a","nota original"),b=caseWith("import-b","outro caso");
        JSONObject session=a.getJSONArray("sessoes").getJSONObject(0);
        session.put("origem","texto").put("mlTranscricao",transcript).put("importRolesConfirmed",true)
            .put("mlSegments",new JSONArray().put(new JSONObject().put("text","Texto fictício com acentos.").put("speaker","paciente").put("startMs",JSONObject.NULL)));
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b)).put("appointments",new JSONArray());
        store.save(root.toString());
        JSONObject bad=new JSONObject(root.toString());
        bad.getJSONArray("cases").getJSONObject(0).put("nome","NÃO GRAVAR");
        bad.getJSONArray("cases").getJSONObject(1).getJSONArray("sessoes").getJSONObject(0).put("id","import-a-s");
        try { store.save(bad.toString()); fail("duplicate session ID accepted"); } catch(IllegalArgumentException expected) {}
        store.getDbHelper().close();store=new SecureStore(context,key);
        JSONObject read=new JSONObject(store.read());
        assertEquals(a.getString("nome"),byId(read,"import-a").getString("nome"));
        JSONObject restored=byId(read,"import-a").getJSONArray("sessoes").getJSONObject(0);
        assertEquals(transcript,restored.getString("mlTranscricao"));
        assertEquals("texto",restored.getString("origem"));
        assertTrue(restored.getJSONArray("mlSegments").getJSONObject(0).isNull("startMs"));
        assertEquals("paciente",restored.getJSONArray("mlSegments").getJSONObject(0).getString("speaker"));
        assertEquals("outro caso",byId(read,"import-b").getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        byte[] backup=SecureStore.encryptBackup(read.toString(),"eco-fixture-2026");
        assertEquals(read.toString(),SecureStore.decryptBackup(backup,"eco-fixture-2026"));
        try(Cursor c=store.getDbHelper().getReadableDatabase().rawQuery("PRAGMA integrity_check",null)){assertTrue(c.moveToFirst());assertEquals("ok",c.getString(0));}
        store.getDbHelper().close();
    }

    @Test public void perCaseDraftAndIncrementalHistorySurviveReopen() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        SecureStore store=new SecureStore(context,key);
        JSONObject a=caseWith("recover-a","PRIMEIRA"),b=caseWith("recover-b","SEGUNDA");
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b)).put("appointments",new JSONArray());
        store.save(root.toString());
        JSONObject draftA=new JSONObject().put("caseId","recover-a").put("session",new JSONObject().put("id","draft-a").put("texto","RASCUNHO_A"));
        JSONObject draftB=new JSONObject().put("caseId","recover-b").put("session",new JSONObject().put("id","draft-b").put("texto","RASCUNHO_B"));
        store.saveFormDraft("recover-a",draftA.toString());store.saveFormDraft("recover-b",draftB.toString());
        store.clearFormDraft("recover-a","wrong");
        assertEquals(draftA.toString(),store.readFormDraft("recover-a"));
        a.getJSONArray("sessoes").getJSONObject(0).put("texto","ALTERADO");
        store.save(root.toString());store.getDbHelper().close();
        store=new SecureStore(context,key);
        assertEquals(draftA.toString(),store.readFormDraft("recover-a"));assertEquals(draftB.toString(),store.readFormDraft("recover-b"));
        JSONArray revisions=new JSONArray(store.sessionVersions("recover-a","recover-a-s"));
        assertEquals(1,revisions.length());
        long seq=revisions.getJSONObject(0).getLong("id");
        assertEquals("PRIMEIRA",new JSONObject(store.sessionVersion("recover-a","recover-a-s",seq)).getString("texto"));
        assertEquals(0,new JSONArray(store.sessionVersions("recover-b","recover-b-s")).length());
        try{store.sessionVersion("recover-b","recover-a-s",seq);fail("cross-case history");}catch(IllegalArgumentException expected){}
        store.clearFormDraft("recover-a","draft-a");assertEquals("",store.readFormDraft("recover-a"));assertEquals(draftB.toString(),store.readFormDraft("recover-b"));
        assertEquals("ALTERADO",byId(new JSONObject(store.read()),"recover-a").getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        store.getDbHelper().close();
    }

    @Test public void encryptedAttachmentChunksAndCheckpointRecovery() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        SecureStore store=new SecureStore(context,key);
        JSONObject a=caseWith("attach-a","ANTES"),b=caseWith("attach-b","OUTRO");
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b)).put("appointments",new JSONArray());
        store.save(root.toString());
        byte[] pdf=new byte[205_000];byte[] head="%PDF-1.4\nMARCADOR_SINTETICO".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(head,0,pdf,0,head.length);
        JSONObject meta=store.addAttachment("attach-a","Documento fictício","Documento recebido",pdf);
        String id=meta.getString("id");
        assertEquals(1,store.listAttachments("attach-a").length());assertEquals(0,store.listAttachments("attach-b").length());
        try{store.attachmentBytes("attach-b",id);fail("cross-case attachment");}catch(IllegalArgumentException expected){}
        try(Cursor c=store.getDbHelper().getReadableDatabase().query("eco_attachment_chunks",new String[]{"data_enc"},"id=?",new String[]{id},null,null,null)){
            assertEquals(3,c.getCount());while(c.moveToNext())assertFalse(new String(c.getBlob(0),StandardCharsets.UTF_8).contains("MARCADOR_SINTETICO"));
        }
        assertArrayEquals(pdf,store.attachmentBytes("attach-a",id));
        try{store.addAttachment("attach-a","Executável","Outro",new byte[20]);fail("unknown format");}catch(java.io.IOException expected){}
        store.checkpoint(true);
        JSONArray slots=store.checkpointList();assertTrue(slots.length()>=1);
        int slot=slots.getJSONObject(0).getInt("slot");
        JSONObject checkpoint=new JSONObject(store.readCheckpoint(slot));assertEquals("ANTES",byId(checkpoint,"attach-a").getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        a.getJSONArray("sessoes").getJSONObject(0).put("texto","DEPOIS");
        store.save(root.toString());store.getDbHelper().close();store=new SecureStore(context,key);
        assertArrayEquals(pdf,store.attachmentBytes("attach-a",id));
        JSONArray bundle=store.exportAttachments();assertEquals(1,bundle.length());
        JSONObject archived=new JSONObject(store.read()).put("attachmentBundle",bundle);
        String decoded=SecureStore.decryptBackup(SecureStore.encryptBackup(archived.toString(),"eco-fixture-2026"),"eco-fixture-2026");
        assertEquals(archived.toString(),decoded);
        store.save(decoded);assertEquals(1,store.listAttachments("attach-a").length());
        JSONObject bad=new JSONObject(decoded);
        bad.getJSONArray("cases").getJSONObject(0).put("nome","NÃO SALVAR");
        bad.getJSONArray("attachmentBundle").getJSONObject(0).getJSONObject("meta").put("sha256","invalid");
        try{store.save(bad.toString());fail("invalid attachment accepted");}catch(SecurityException expected){}
        assertEquals(a.getString("nome"),byId(new JSONObject(store.read()),"attach-a").getString("nome"));
        assertArrayEquals(pdf,store.attachmentBytes("attach-a",id));
        store.getDbHelper().close();
    }

    @Test public void largeEncryptedRecordsBeyondCursorWindowSurviveReopenAndVersionRead() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        SecureStore store=new SecureStore(context,key);
        String large="História fictícia com acentuação. ".repeat(100000);
        JSONObject a=caseWith("large-a","nota");
        JSONObject session=a.getJSONArray("sessoes").getJSONObject(0);
        session.put("origem","texto").put("mlTranscricao",large);
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(a)).put("appointments",new JSONArray());
        store.save(root.toString());
        JSONObject draft=new JSONObject().put("caseId","large-a").put("session",new JSONObject().put("id","large-draft").put("texto",large));
        store.saveFormDraft("large-a",draft.toString());
        store.getDbHelper().close();store=new SecureStore(context,key);
        JSONObject read=new JSONObject(store.read());
        assertEquals(large,byId(read,"large-a").getJSONArray("sessoes").getJSONObject(0).getString("mlTranscricao"));
        assertEquals(draft.toString(),store.readFormDraft("large-a"));
        session.put("mlTranscricao","TEXTO RETIFICADO");
        store.save(root.toString());
        JSONArray versions=new JSONArray(store.sessionVersions("large-a","large-a-s"));
        assertEquals(1,versions.length());
        assertEquals(large,new JSONObject(store.sessionVersion("large-a","large-a-s",versions.getJSONObject(0).getLong("id"))).getString("mlTranscricao"));
        store.getDbHelper().close();
    }

    @Test public void portableRecoveryReencryptsAcrossDevicesAndRejectsConflictingDraftAtomically() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        byte[] k1=new byte[32],k2=new byte[32];java.util.Arrays.fill(k2,(byte)42);
        SecureStore store=new SecureStore(context,new SecretKeySpec(k1,"AES"));
        JSONObject a=caseWith("portable-a","VERSAO ANTERIOR"),b=caseWith("portable-b","NOTA B");
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b)).put("appointments",new JSONArray());
        store.save(root.toString());
        JSONObject draft=new JSONObject().put("caseId","portable-a").put("session",new JSONObject().put("id","portable-draft").put("texto","RASCUNHO REMOTO"));
        store.saveFormDraft("portable-a",draft.toString());
        a.getJSONArray("sessoes").getJSONObject(0).put("texto","VERSAO ATUAL");
        store.save(root.toString());
        JSONObject backup=store.exportPortableSnapshot(false);
        assertEquals(1,backup.getJSONObject("nativeRecoveryBundle").getJSONArray("drafts").length());
        assertEquals(1,backup.getJSONObject("nativeRecoveryBundle").getJSONArray("revisions").length());
        assertFalse(byId(backup,"portable-a").has("diarioNotas"));
        byte[] envelope=SecureStore.encryptBackup(backup.toString(),"eco-portable-fixture");
        store.getDbHelper().close();
        assertTrue(context.deleteDatabase("eco_vault.db"));
        store=new SecureStore(context,new SecretKeySpec(k2,"AES"));
        String restored=SecureStore.decryptBackup(envelope,"eco-portable-fixture");
        store.save(restored);
        assertEquals(draft.toString(),store.readFormDraft("portable-a"));
        assertEquals("",store.readFormDraft("portable-b"));
        JSONArray versions=new JSONArray(store.sessionVersions("portable-a","portable-a-s"));
        assertEquals(1,versions.length());
        assertEquals("VERSAO ANTERIOR",new JSONObject(store.sessionVersion("portable-a","portable-a-s",versions.getJSONObject(0).getLong("id"))).getString("texto"));
        assertFalse(new JSONObject(store.read()).has("nativeRecoveryBundle"));
        store.save(restored);
        assertEquals(1,new JSONArray(store.sessionVersions("portable-a","portable-a-s")).length());
        JSONObject divergent=new JSONObject(draft.toString());
        divergent.getJSONObject("session").put("texto","RASCUNHO LOCAL");
        store.saveFormDraft("portable-a",divergent.toString());
        JSONObject attempted=new JSONObject(restored);
        byId(attempted,"portable-b").put("nome","NAO GRAVAR");
        try{store.save(attempted.toString());fail("conflicting draft accepted");}catch(IllegalArgumentException expected){}
        assertEquals(divergent.toString(),store.readFormDraft("portable-a"));
        assertEquals(b.getString("nome"),byId(new JSONObject(store.read()),"portable-b").getString("nome"));
        assertEquals(1,new JSONArray(store.sessionVersions("portable-a","portable-a-s")).length());
        store.getDbHelper().close();
    }

    @Test public void encryptedEditorDraftsSurviveReopenAndPortableRestoreWithoutBecomingRecords() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        byte[] k=new byte[32];SecureStore store=new SecureStore(context,new SecretKeySpec(k,"AES"));
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(caseWith("editor-a","nota"))).put("appointments",new JSONArray());
        store.save(root.toString());
        JSONObject w=new JSONObject().put("slot","wizard").put("step",2).put("value",new JSONObject().put("id","uncommitted").put("nome","RASCUNHO SINTETICO"));
        JSONObject a=new JSONObject().put("slot","appointment").put("value",new JSONObject().put("id","unsaved-appt").put("caseId","editor-a"));
        store.saveEditorDraft("wizard",w.toString());store.saveEditorDraft("appointment",a.toString());
        store.getDbHelper().close();store=new SecureStore(context,new SecretKeySpec(k,"AES"));
        assertEquals(w.toString(),store.readEditorDraft("wizard"));assertEquals(a.toString(),store.readEditorDraft("appointment"));
        assertEquals(1,new JSONObject(store.read()).getJSONArray("cases").length());assertEquals(0,new JSONObject(store.read()).getJSONArray("appointments").length());
        store.clearEditorDraft("wizard","wrong");assertEquals(w.toString(),store.readEditorDraft("wizard"));
        JSONObject another=new JSONObject(w.toString());another.getJSONObject("value").put("id","another");
        try{store.saveEditorDraft("wizard",another.toString());fail("overwritten draft");}catch(IllegalArgumentException expected){}
        JSONObject portable=store.exportPortableSnapshot(false);assertEquals(2,portable.getJSONObject("nativeRecoveryBundle").getJSONArray("editors").length());
        store.getDbHelper().close();assertTrue(context.deleteDatabase("eco_vault.db"));
        k[0]=9;store=new SecureStore(context,new SecretKeySpec(k,"AES"));store.save(portable.toString());
        assertEquals(w.toString(),store.readEditorDraft("wizard"));assertEquals(a.toString(),store.readEditorDraft("appointment"));
        store.clearEditorDraft("wizard","uncommitted");assertEquals("",store.readEditorDraft("wizard"));assertEquals(a.toString(),store.readEditorDraft("appointment"));
        store.getDbHelper().close();
    }

    @Test public void authenticatedDecoderCheckpointSurvivesReopenAndRejectsWrongKeyOrModel() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        byte[] master=new byte[32],audio=new byte[32];audio[0]=7;
        SecureStore store=new SecureStore(context,new SecretKeySpec(master,"AES"));
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(caseWith("decoder-a","nota"))).put("appointments",new JSONArray());
        store.save(root.toString());
        EcoDatabaseHelper db=store.getDbHelper();
        db.createJob("decoder-job","decoder-a","decoder-a-s","fixture",audio);
        assertTrue(db.commitSegment("decoder-job","decoder-a","decoder-a-s",0,1000,"falante1","primeiro",null));
        SpeakerClusters clusters=new SpeakerClusters();clusters.assign(new double[]{1,0,0});
        java.util.Set<String> prefix=db.verifiedPrefixIds("decoder-job",25000);
        db.saveDecoderCheckpoint("decoder-job","small",25000,prefix,clusters.checkpoint(),true);
        assertTrue(db.commitSegment("decoder-job","decoder-a","decoder-a-s",26000,27000,"falante1","segundo",null));
        store.getDbHelper().close();store=new SecureStore(context,new SecretKeySpec(master,"AES"));db=store.getDbHelper();
        JSONObject cp=db.readDecoderCheckpoint("decoder-job","small",audio);
        assertEquals(25000,cp.getLong("offset"));assertEquals(1,cp.getInt("count"));assertEquals(prefix,db.verifiedPrefixIds("decoder-job",25000));
        assertEquals(clusters.checkpoint(),cp.getString("speakers"));
        try{db.readDecoderCheckpoint("decoder-job","small",new byte[32]);fail("wrong key");}catch(java.security.GeneralSecurityException expected){}
        try{db.readDecoderCheckpoint("decoder-job","base",audio);fail("wrong model");}catch(SecurityException expected){}
        assertTrue(db.commitSegment("decoder-job","decoder-a","decoder-a-s",26000,27000,"falante1","segundo",null));
        db.saveDecoderCheckpoint("decoder-job","small",50000,db.verifiedPrefixIds("decoder-job",50000),clusters.checkpoint(),true);
        assertTrue(db.markJobDecoded("decoder-job",2,2));assertTrue(db.acknowledgePersistence("decoder-job"));
        assertTrue(db.clearJobAudioKey("decoder-job"));assertNull(db.readDecoderCheckpoint("decoder-job","small",audio));
        store.getDbHelper().close();
    }

    @Test public void clinicalDraftsStayIsolatedAndRestoreWithoutChangingApprovedRecord() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        byte[] k=new byte[32];SecureStore store=new SecureStore(context,new SecretKeySpec(k,"AES"));
        JSONObject a=caseWith("clinical-a","ORIGINAL A"),b=caseWith("clinical-b","ORIGINAL B");
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b)).put("appointments",new JSONArray());
        store.save(root.toString());
        JSONObject draft=new JSONObject().put("caseId","clinical-a").put("kind","anamnese").put("id","draft-1")
            .put("base",new JSONObject().put("padroesSono","ORIGINAL A")).put("fields",new JSONObject().put("padroesSono","NÃO APROVADO"));
        store.saveClinicalDraft("clinical-a","anamnese",draft.toString());
        assertEquals("",store.readClinicalDraft("clinical-b","anamnese"));assertEquals("",store.readClinicalDraft("clinical-a","enquadre"));
        store.getDbHelper().close();store=new SecureStore(context,new SecretKeySpec(k,"AES"));
        assertEquals(draft.toString(),store.readClinicalDraft("clinical-a","anamnese"));
        assertEquals("ORIGINAL A",byId(new JSONObject(store.read()),"clinical-a").getJSONObject("anamnese").getString("padroesSono"));
        // A stale or foreign draft ID must fail closed without deleting the saved draft.
        try {
            store.clearClinicalDraft("clinical-a","anamnese","wrong");
            fail("stale draft ID accepted");
        } catch (SecurityException expected) {
            assertEquals("Rascunho divergente",expected.getMessage());
        }
        assertEquals(draft.toString(),store.readClinicalDraft("clinical-a","anamnese"));
        JSONObject conflicting=new JSONObject(draft.toString()).put("id","wrong");
        try {
            store.saveClinicalDraft("clinical-a","anamnese",conflicting.toString());
            fail("foreign draft ID overwrote existing draft");
        } catch (IllegalArgumentException expected) {
            assertEquals("Rascunho divergente",expected.getMessage());
        }
        assertEquals(draft.toString(),store.readClinicalDraft("clinical-a","anamnese"));
        JSONObject portable=store.exportPortableSnapshot(false);assertEquals(1,portable.getJSONObject("nativeRecoveryBundle").getJSONArray("clinical").length());
        store.getDbHelper().close();assertTrue(context.deleteDatabase("eco_vault.db"));k[0]=11;
        store=new SecureStore(context,new SecretKeySpec(k,"AES"));store.save(portable.toString());
        assertEquals(draft.toString(),store.readClinicalDraft("clinical-a","anamnese"));
        store.clearClinicalDraft("clinical-a","anamnese","draft-1");assertEquals("",store.readClinicalDraft("clinical-a","anamnese"));
        assertEquals("ORIGINAL B",byId(new JSONObject(store.read()),"clinical-b").getJSONObject("anamnese").getString("padroesSono"));
        store.getDbHelper().close();
    }
    @Test public void largeAttachmentArchiveRestoresAtomicallyBeyondLegacyLimit() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecretKeySpec key=new SecretKeySpec(new byte[32],"AES");
        char[] password="eco-fixture-archive-2026".toCharArray();
        SecureStore store=new SecureStore(context,key);
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(caseWith("archive-a","FICTICIO"))).put("appointments",new JSONArray());
        store.save(root.toString());
        java.util.List<JSONObject> metas=new java.util.ArrayList<>();
        byte[] pdf=new byte[6_100_000];
        byte[] header="%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(header,0,pdf,0,header.length);
        for(int n=0;n<4;n++){
            pdf[pdf.length-1]=(byte)n;
            metas.add(store.addAttachment("archive-a","Sintético "+n,"Teste",pdf));
        }
        try{store.exportPortableSnapshot(false);fail("legacy attachment budget bypassed");}catch(java.io.IOException expected){}
        JSONObject clinical=store.exportPortableSnapshot(false,false);
        assertEquals(0,clinical.getJSONArray("attachmentBundle").length());
        java.io.File archive=java.io.File.createTempFile("eco-archive-test",".bin",context.getCacheDir());
        try{
            try(java.io.OutputStream out=new java.io.FileOutputStream(archive)){
                assertEquals(4,store.exportAttachmentArchive(out,password,()->true));
            }
            assertTrue(archive.length()>24_000_000);
            store.getDbHelper().close();assertTrue(context.deleteDatabase("eco_vault.db"));
            key=new SecretKeySpec(new byte[]{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},"AES");
            store=new SecureStore(context,key);store.save(clinical.toString());
            // Tamper only the authenticated terminal tag: earlier records must roll back.
            int original;
            try(java.io.RandomAccessFile raf=new java.io.RandomAccessFile(archive,"rw")){
                raf.seek(raf.length()-1);original=raf.read();raf.seek(raf.length()-1);raf.write(original^1);
            }
            try(java.io.InputStream in=new java.io.FileInputStream(archive)){
                try{store.restoreAttachmentArchive(in,password,()->true);fail("corrupt archive accepted");}catch(java.io.IOException expected){}
            }
            assertEquals(0,store.listAttachments("archive-a").length());
            try(java.io.RandomAccessFile raf=new java.io.RandomAccessFile(archive,"rw")){
                raf.seek(raf.length()-1);raf.write(original);
            }
            try(java.io.InputStream in=new java.io.FileInputStream(archive)){
                assertEquals(4,store.restoreAttachmentArchive(in,password,()->true));
            }
            assertEquals(4,store.listAttachments("archive-a").length());
            for(int n=0;n<metas.size();n++){
                pdf[pdf.length-1]=(byte)n;
                assertArrayEquals(pdf,store.attachmentBytes("archive-a",metas.get(n).getString("id")));
            }
            try(java.io.InputStream in=new java.io.FileInputStream(archive)){
                assertEquals(0,store.restoreAttachmentArchive(in,password,()->true));
            }
            assertEquals(4,store.listAttachments("archive-a").length());
        }finally{
            java.util.Arrays.fill(pdf,(byte)0);java.util.Arrays.fill(password,(char)0);
            store.getDbHelper().close();archive.delete();
        }
    }

    @Test public void fullVaultArchiveExceeds50MbRestoresAllTablesAndRejectsNonemptyDestination() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        char[] password="eco-fixture-full-vault".toCharArray();
        SecureStore store=new SecureStore(context,new SecretKeySpec(new byte[32],"AES"));
        JSONObject root=new JSONObject().put("version",1).put("uiTheme","dark")
            .put("cases",new JSONArray().put(caseWith("full-a","ORIGINAL"))).put("appointments",new JSONArray());
        store.save(root.toString());
        JSONObject draft=new JSONObject().put("caseId","full-a").put("session",new JSONObject().put("id","full-draft").put("texto","RASCUNHO FICTICIO"));
        store.saveFormDraft("full-a",draft.toString());
        root.getJSONArray("cases").getJSONObject(0).getJSONArray("sessoes").getJSONObject(0).put("texto","REVISADO");
        store.save(root.toString());
        EcoDatabaseHelper helper=store.getDbHelper();
        helper.createJob("full-job","full-a","full-a-s","fixture-audio",new byte[32]);
        assertTrue(helper.commitSegment("full-job","full-a","full-a-s",0,1000,"sobreposicao","TRECHO FICTICIO",null));
        assertTrue(helper.markJobDecoded("full-job",1,1));assertTrue(helper.acknowledgePersistence("full-job"));
        assertTrue(helper.clearJobAudioKey("full-job"));
        store.savePrivateDiary("full-a",new JSONArray().put(new JSONObject().put("id","diary-test").put("data","2026-11-25").put("texto","DIARIO_FICTICIO")).toString());
        byte[] pdf=new byte[6_100_000];System.arraycopy("%PDF-1.7".getBytes(StandardCharsets.US_ASCII),0,pdf,0,8);
        JSONObject attachment=null;
        for(int n=0;n<5;n++){pdf[pdf.length-1]=(byte)n;attachment=store.addAttachment("full-a","Sintético "+n,"Teste",pdf);}
        final String attachmentId=attachment.getString("id");
        // Add independent large synthetic sessions without constructing one huge JSON snapshot.
        SQLiteDatabase db=store.getDbHelper().getWritableDatabase();
        char[] chars=new char[1_000_000];java.util.Arrays.fill(chars,'x');String text=new String(chars);
        for(int n=0;n<24;n++){
            String id="full-large-"+n;
            JSONObject session=new JSONObject().put("id",id).put("data","2026-11-25").put("texto",text).put("origem","manual");
            android.content.ContentValues v=new android.content.ContentValues();v.put("id",id);v.put("case_id","full-a");v.put("data","2026-11-25");
            v.put("origem","manual");v.put("created_at",1);v.put("updated_at",1);v.put("payload_enc",store.getDbHelper().encrypt(session.toString()));
            db.insertOrThrow("sessoes",null,v);
        }
        java.io.File archive=java.io.File.createTempFile("eco-full-vault-test",".bin",context.getCacheDir());
        try{
            try(java.io.OutputStream out=new java.io.FileOutputStream(archive)){assertTrue(store.exportFullVault(out,password,false,()->true)>30);}
            assertTrue(archive.length()>50_000_000);
            try(java.io.InputStream in=new java.io.FileInputStream(archive)){
                try{store.restoreFullVault(in,password,()->true);fail("populated vault accepted");}catch(SecurityException expected){}
            }
            assertEquals(5,store.listAttachments("full-a").length());
            store.getDbHelper().close();assertTrue(context.deleteDatabase("eco_vault.db"));
            byte[] destinationKey=new byte[32];destinationKey[0]=1;
            store=new SecureStore(context,new SecretKeySpec(destinationKey,"AES"));
            int original;
            try(java.io.RandomAccessFile raf=new java.io.RandomAccessFile(archive,"rw")){
                raf.seek(raf.length()-1);original=raf.read();raf.seek(raf.length()-1);raf.write(original^1);
            }
            try(java.io.InputStream in=new java.io.FileInputStream(archive)){
                try{store.restoreFullVault(in,password,()->true);fail("tampered archive accepted");}catch(java.io.IOException expected){}
            }
            try(Cursor c=store.getDbHelper().getReadableDatabase().rawQuery("SELECT count(*) FROM _pacientes",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
            try(java.io.RandomAccessFile raf=new java.io.RandomAccessFile(archive,"rw")){raf.seek(raf.length()-1);raf.write(original);}
            try(java.io.InputStream in=new java.io.FileInputStream(archive)){assertTrue(store.restoreFullVault(in,password,()->true)>30);}
            assertEquals(5,store.listAttachments("full-a").length());
            assertArrayEquals(pdf,store.attachmentBytes("full-a",attachmentId));
            assertEquals("[]",store.readPrivateDiary("full-a"));
            assertEquals(draft.toString(),store.readFormDraft("full-a"));
            JSONArray versions=new JSONArray(store.sessionVersions("full-a","full-a-s"));
            assertEquals(1,versions.length());
            assertEquals("ORIGINAL",new JSONObject(store.sessionVersion("full-a","full-a-s",versions.getJSONObject(0).getLong("id"))).getString("texto"));
            assertTrue(store.getDbHelper().isJobCommitted("full-job"));
            try(Cursor c=store.getDbHelper().getReadableDatabase().rawQuery("SELECT speaker,text_enc FROM segmentos WHERE job_id='full-job'",null)){
                assertTrue(c.moveToFirst());assertEquals("sobreposicao",c.getString(0));assertEquals("TRECHO FICTICIO",store.getDbHelper().decrypt(c.getBlob(1)));
            }
            try(Cursor c=store.getDbHelper().getReadableDatabase().rawQuery("SELECT count(*) FROM sessoes WHERE case_id='full-a'",null)){assertTrue(c.moveToFirst());assertEquals(25,c.getInt(0));}
            try(Cursor c=store.getDbHelper().getReadableDatabase().rawQuery("SELECT payload_enc FROM sessoes WHERE id='full-large-23'",null)){
                assertTrue(c.moveToFirst());assertEquals(text,new JSONObject(store.getDbHelper().decrypt(c.getBlob(0))).getString("texto"));
            }
        }finally{java.util.Arrays.fill(password,(char)0);java.util.Arrays.fill(pdf,(byte)0);store.getDbHelper().close();archive.delete();}
    }

    @Test public void nativeKeysetPagesStayScopedAndBounded() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecureStore store=new SecureStore(context,new SecretKeySpec(new byte[32],"AES"));
        JSONArray cases=new JSONArray();
        for(int n=0;n<23;n++)cases.put(caseWith(String.format(java.util.Locale.ROOT,"page-%02d",n),"FICTICIO"));
        JSONObject first=cases.getJSONObject(0);
        String longText="";char[] chars=new char[32001];java.util.Arrays.fill(chars,'x');longText=new String(chars);
        JSONArray sessions=first.getJSONArray("sessoes");
        for(int n=0;n<22;n++)sessions.put(new JSONObject().put("id",String.format(java.util.Locale.ROOT,"page-session-%02d",n)).put("data","2026-11-25").put("origem","manual").put("texto",longText));
        store.save(new JSONObject().put("version",1).put("cases",cases).put("appointments",new JSONArray()).toString());
        JSONObject page=store.casePage("");assertEquals(20,page.getJSONArray("rows").length());
        JSONObject tail=store.casePage(page.getString("next"));assertEquals(3,tail.getJSONArray("rows").length());assertEquals("",tail.getString("next"));
        JSONObject sp=store.sessionPage("page-00","");assertEquals(20,sp.getJSONArray("rows").length());
        assertEquals(3,store.sessionPage("page-00",sp.getString("next")).getJSONArray("rows").length());
        JSONObject text=store.sessionTextPage("page-00","page-session-00","texto",0);
        assertEquals(16000,text.getString("text").length());assertEquals(16000,text.getInt("next"));
        assertEquals(1,store.sessionTextPage("page-00","page-session-00","texto",32000).getString("text").length());
        try{store.sessionTextPage("page-01","page-session-00","texto",0);fail("cross-case text");}catch(SecurityException expected){}
        EcoDatabaseHelper helper=store.getDbHelper();helper.createJob("page-job","page-00","page-00-s","",null);
        for(int n=0;n<22;n++)assertTrue(helper.commitSegment("page-job","page-00","page-00-s",n*1000,n*1000+500,"falante1","TRECHO "+n,null));
        JSONObject seg=store.segmentPage("page-00","page-00-s","");assertEquals(20,seg.getJSONArray("rows").length());
        assertEquals(2,store.segmentPage("page-00","page-00-s",seg.getString("next")).getJSONArray("rows").length());
        try{store.segmentPage("page-01","page-00-s","");fail("cross-case segments");}catch(SecurityException expected){}
        store.getDbHelper().close();
    }
    @Test public void integralLocalCheckpointReexportsExactOldSnapshotAndPreservesPreviousOnCancel() throws Exception {
        Context context=RuntimeEnvironment.getApplication();byte[] firstKey=new byte[32];
        SecureStore store=new SecureStore(context,new SecretKeySpec(firstKey,"AES"));
        JSONObject c=caseWith("local-full","ANTES");
        JSONObject root=new JSONObject().put("version",1).put("cases",new JSONArray().put(c)).put("appointments",new JSONArray());
        store.save(root.toString());
        store.savePrivateDiary("local-full",new JSONArray().put(new JSONObject().put("id","local-diary").put("texto","PRIVADO")).toString());
        byte[] pdf="%PDF-1.7 ficticio".getBytes(StandardCharsets.UTF_8);
        JSONObject meta=store.addAttachment("local-full","Teste","Teste",pdf);
        assertTrue(store.fullCheckpoint(true,()->true));JSONArray rows=store.fullCheckpointList();assertEquals(1,rows.length());
        int slot=rows.getJSONObject(0).getInt("slot");
        assertFalse(store.fullCheckpoint(false,()->true));
        c.getJSONArray("sessoes").getJSONObject(0).put("texto","DEPOIS");store.save(root.toString());
        try{store.fullCheckpoint(true,()->false);fail("cancelled checkpoint");}catch(java.io.IOException expected){}
        assertEquals(1,store.fullCheckpointList().length());
        java.util.concurrent.atomic.AtomicInteger checks=new java.util.concurrent.atomic.AtomicInteger();
        try{store.fullCheckpoint(true,()->checks.incrementAndGet()<4);fail("mid-write cancellation accepted");}catch(java.io.IOException expected){}
        assertTrue(checks.get()>=4);assertEquals(1,store.fullCheckpointList().length());
        assertTrue(store.fullCheckpoint(true,()->true));assertEquals(2,store.fullCheckpointList().length());
        char[] password="eco-checkpoint-fixture".toCharArray();
        java.io.ByteArrayOutputStream portable=new java.io.ByteArrayOutputStream();store.exportFullCheckpoint(slot,portable,password,()->true);
        store.getDbHelper().close();assertTrue(context.deleteDatabase("eco_vault.db"));
        byte[] secondKey=new byte[32];secondKey[0]=1;
        store=new SecureStore(context,new SecretKeySpec(secondKey,"AES"));
        store.restoreFullVault(new java.io.ByteArrayInputStream(portable.toByteArray()),password,()->true);
        assertEquals("ANTES",byId(new JSONObject(store.read()),"local-full").getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        assertArrayEquals(pdf,store.attachmentBytes("local-full",meta.getString("id")));
        assertEquals("[]",store.readPrivateDiary("local-full"));
        store.getDbHelper().close();java.util.Arrays.fill(password,(char)0);
        for(int localSlot=2;localSlot<=3;localSlot++)new java.io.File(context.getFilesDir(),"eco-full-checkpoint-"+localSlot+".aes").delete();
    }

    @Test public void scopedPageEditingPreservesOriginalAndHistoryAndRejectsStaleWrites() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        SecureStore store=new SecureStore(context,new SecretKeySpec(new byte[32],"AES"));
        JSONObject a=caseWith("edit-page-a","ANTES"),b=caseWith("edit-page-b","OUTRO");
        JSONObject session=a.getJSONArray("sessoes").getJSONObject(0);
        char[] chars=new char[32001];java.util.Arrays.fill(chars,'x');String original=new String(chars);
        session.put("texto",original).put("mlTranscricao","ORIGINAL IMUTAVEL").put("mlRevisada","REVISADA");
        store.save(new JSONObject().put("version",1).put("cases",new JSONArray().put(a).put(b)).put("appointments",new JSONArray()).toString());
        JSONObject page=store.sessionTextPage("edit-page-a","edit-page-a-s","texto",16000);
        String rev=page.getString("revision");
        JSONObject updated=store.editSessionTextPage("edit-page-a","edit-page-a-s","texto",16000,page.getInt("end"),rev,"NOVO");
        assertNotEquals(rev,updated.getString("revision"));
        JSONObject result=byId(new JSONObject(store.read()),"edit-page-a").getJSONArray("sessoes").getJSONObject(0);
        assertEquals(original.substring(0,16000)+"NOVO"+original.substring(32000),result.getString("texto"));
        assertEquals("ORIGINAL IMUTAVEL",result.getString("mlTranscricao"));assertEquals("REVISADA",result.getString("mlRevisada"));
        assertEquals("OUTRO",byId(new JSONObject(store.read()),"edit-page-b").getJSONArray("sessoes").getJSONObject(0).getString("texto"));
        try{store.editSessionTextPage("edit-page-a","edit-page-a-s","texto",0,1,rev,"X");fail("stale edit accepted");}catch(SecurityException expected){}
        try{store.editSessionTextPage("edit-page-b","edit-page-a-s","texto",0,1,updated.getString("revision"),"X");fail("cross-case edit accepted");}catch(SecurityException expected){}
        try{store.editSessionTextPage("edit-page-a","edit-page-a-s","mlTranscricao",0,1,updated.getString("revision"),"X");fail("raw transcript changed");}catch(IllegalArgumentException expected){}
        JSONArray versions=new JSONArray(store.sessionVersions("edit-page-a","edit-page-a-s"));
        assertEquals(1,versions.length());
        assertEquals(original,new JSONObject(store.sessionVersion("edit-page-a","edit-page-a-s",versions.getJSONObject(0).getLong("id"))).getString("texto"));
        store.getDbHelper().close();
    }

}
