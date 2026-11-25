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
}
