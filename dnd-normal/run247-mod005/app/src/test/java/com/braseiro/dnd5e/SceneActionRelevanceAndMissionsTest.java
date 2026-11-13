package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Context quality and durable mission lifecycle, without fabricated facts. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public final class SceneActionRelevanceAndMissionsTest {
    private Context context;
    private AppDb db;
    private String name;

    @Before public void open(){
        context=RuntimeEnvironment.getApplication();
        name="quests-relevance-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(context,name);
        db.setActiveMapProfile("dnd");
    }

    @After public void close(){
        if(db!=null)db.close();
        context.deleteDatabase(name);
    }

    private static JSONObject option(String type,String action)throws Exception{
        return new JSONObject().put("type",type).put("action_type",type)
            .put("label",type+" • "+action).put("action",action);
    }

    @Test public void incidentalNotebookCannotTakeThreeDifferentCategories()throws Exception{
        JSONObject scene=new JSONObject();
        JSONArray shown=new JSONArray();
        JSONObject look=option("OBSERVAR","Examino o caderno deixado sobre a mesa.");
        JSONObject read=option("AGIR","Leio o caderno sobre a mesa antes de responder.");
        JSONObject investigate=option("INVESTIGAR","Investigo o caderno sobre a mesa.");
        JSONObject talk=option("CONVERSAR","Pergunto à professora sobre as inscrições.");
        JSONObject act=option("EXPLORAR","Aproximo-me da mesa de inscrições mencionada na cena.");
        ArrayList<JSONObject> candidates=new ArrayList<>();
        candidates.add(look);candidates.add(read);candidates.add(investigate);
        candidates.add(talk);candidates.add(act);
        String narration="A professora explica as inscrições do campus e pede uma resposta. "
            +"Uma mesa de inscrições foi preparada. Um caderno fica sobre outra mesa.";
        List<JSONObject> ranked=SceneActionRelevance.order(candidates,narration,"",scene);
        assertTrue("A conversation about registration outranks an incidental notebook",
            ranked.indexOf(talk)<ranked.indexOf(look));
        assertFalse("The notebook has one focal slot, not one slot per verb",
            SceneActionRelevance.diverse(read,new JSONArray().put(look),scene,narration));
        assertFalse(SceneActionRelevance.diverse(investigate,
            new JSONArray().put(look),scene,narration));
        assertTrue(SceneActionRelevance.diverse(talk,new JSONArray().put(look),scene,narration));
    }

    @Test public void activeObjectiveOutranksBackgroundProps()throws Exception{
        JSONObject scene=new JSONObject().put("active_quests",new JSONArray().put(
            new JSONObject().put("title","Inscrição na Guilda")
                .put("objective","Conversar com a funcionária sobre a inscrição da Guilda")
                .put("stage","Inscrições abertas")));
        JSONObject notebook=option("OBSERVAR","Observo o caderno deixado na mesa.");
        JSONObject goal=option("CONVERSAR","Converso com a funcionária sobre a inscrição da Guilda.");
        assertTrue(SceneActionRelevance.goalMatch(goal,scene));
        assertFalse(SceneActionRelevance.goalMatch(notebook,scene));
        ArrayList<JSONObject> list=new ArrayList<>();list.add(notebook);list.add(goal);
        assertEquals(goal,SceneActionRelevance.order(list,
            "Há um caderno na mesa. A funcionária recebe inscrições na Guilda.",
            "",scene).get(0));
    }

    @Test public void speakingNpcAndLiveObjectiveBeatIncidentalNotebook()throws Exception{
        JSONObject scene=new JSONObject().put("objective",
            "Investigar o desaparecimento de uma aluna")
            .put("active_quests",new JSONArray().put(
                new JSONObject().put("title","Aluna desaparecida")
                    .put("objective","Descobrir onde a aluna desaparecida foi vista")));
        JSONObject notebook=option("OBSERVAR",
            "Examino as páginas do caderno sobre a mesa.");
        JSONObject speaker=option("CONVERSAR",
            "Pergunto a Aisla sobre o desaparecimento da aluna.");
        JSONObject objective=option("INVESTIGAR",
            "Investigo o desaparecimento da aluna com as testemunhas presentes.");
        List<JSONObject> options=new ArrayList<>();
        options.add(notebook);
        options.add(speaker);
        options.add(objective);
        List<JSONObject> chosen=SceneActionRelevance.order(options,
            "Aisla: — A aluna desapareceu ontem. Aisla espera nossa resposta. "
                +"Há um caderno esquecido sobre uma mesa.",
            "",scene);
        assertNotEquals("An incidental notebook must not be the top option",
            notebook,chosen.get(0));
        assertTrue("A scene-level goal should outrank incidental scenery",
            chosen.indexOf(objective)<chosen.indexOf(notebook));
        assertTrue("The speaking NPC should outrank incidental scenery",
            chosen.indexOf(speaker)<chosen.indexOf(notebook));
    }

    @Test public void activeAndCompletedQuestsRemainAfterDatabaseReopen()throws Exception{
        long first=db.addQuest("Inscrições na Guilda","Falar com a funcionária",
            "A vaga extracurricular",0);
        long second=db.addQuest("Investigar o Jardim","Conhecer o motivo do chamado",
            "A rotina em Strixhaven",0);
        assertEquals(2,db.listQuests("active",20).size());
        db.updateQuestStatus(first,"completed","Inscrição concluída pelo jogador");
        assertEquals(1,db.listQuests("active",20).size());
        assertEquals("Investigar o Jardim",db.listQuests("active",20).get(0).optString("title"));
        assertEquals("Inscrições na Guilda",
            db.listQuests("completed",20).get(0).optString("title"));
        db.close();
        db=new AppDb(context,name);
        db.setActiveMapProfile("dnd");
        assertEquals(1,db.listQuests("completed",20).size());
        assertEquals(1,db.listQuests("active",20).size());
        assertEquals("Inscrição concluída pelo jogador",
            db.listQuests("completed",20).get(0).optString("stage"));
        db.updateQuestStatus(first,"active","Retomada pelo jogador");
        assertEquals(2,db.listQuests("active",20).size());
        assertTrue(second>0);
    }
}
