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
}
