package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.UUID;
import static org.junit.Assert.*;

/**
 * Extended, offline campaign integration QA. No Gemini API call is made:
 * generated envelopes are adversarial fixtures, not evidence of a live model.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class PostStrixhavenLongCampaignRobolectricTest {
    private Context context;
    private AppDb db;
    private String dbName;

    @Before public void before() throws Exception {
        context=RuntimeEnvironment.getApplication();
        dbName="sandbox_stress_"+UUID.randomUUID().toString().substring(0,8)+".db";
        db=new AppDb(context,dbName);
        db.setActiveMapProfile("dnd");
        JSONObject scene=db.getSceneState();
        scene.put("day_number",1).put("clock_hour",8d)
             .put("time_of_day","Manhã").put("location","Yartar")
             .put("weather","Céu limpo");
        db.saveSceneState(scene);
    }

    @After public void after() {
        if(db!=null)db.close();
        if(context!=null&&dbName!=null)context.deleteDatabase(dbName);
    }

    @Test public void thirtyTwoActionsSpanFourDaysWithoutDoubleTimeOrAiClockAuthority() throws Exception {
        double absoluteHour=8d;
        for(int turn=0;turn<32;turn++){
            WorldStateEngine world=new WorldStateEngine(db);
            JSONObject inquiry=world.preview("Posso descansar, ou preciso procurar um abrigo?");
            assertEquals("Player question advanced time",0d,inquiry.optDouble("hours_advanced"),.001d);

            JSONObject tick=world.preview("Passo 3 horas escrevendo notas e estudando a região");
            assertEquals("Preview error, turn "+turn+": "+tick.optString("error"),
                    "",tick.optString("error"));
            assertEquals("Duration changed on turn "+turn,3d,tick.optDouble("hours_advanced"),.001d);
            assertTrue("Commit failed on turn "+turn+": "+tick.optString("commit_error"),world.commit(tick));
            assertFalse("Same world tick committed twice on turn "+turn,world.commit(tick));
            absoluteHour+=3d;

            JSONObject scene=db.getSceneState();
            assertEquals("Lost day on turn "+turn,1+(int)Math.floor(absoluteHour/24d),
                    scene.optInt("day_number"));
            assertEquals("Clock divergence on turn "+turn,absoluteHour%24d,
                    scene.optDouble("clock_hour"),.001d);
            if(turn%8==0){
                JSONObject maliciousEnvelope=new JSONObject().put("scene_state",
                    new JSONObject().put("day_number",999).put("clock_hour",23d)
                        .put("time_of_day","Noite").put("location","Yartar"));
                new SceneStateEngine(db).apply(maliciousEnvelope,
                        "O narrador afirma que se passaram cem dias sem ação do jogador.",
                        new JSONObject());
                assertEquals("AI invented world day",1+(int)Math.floor(absoluteHour/24d),
                        db.getSceneState().optInt("day_number"));
                assertEquals("AI invented world time",absoluteHour%24d,
                        db.getSceneState().optDouble("clock_hour"),.001d);
            }
            if(turn%8==7){
                assertTrue("SQLite failed integrity check",db.quickIntegrityCheck());
                db.close();
                db=new AppDb(context,dbName);
                db.setActiveMapProfile("dnd");
                assertEquals("Reopening lost campaign hour",absoluteHour%24d,
                        db.getSceneState().optDouble("clock_hour"),.001d);
                assertEquals("Reopening lost campaign day",1+(int)Math.floor(absoluteHour/24d),
                        db.getSceneState().optInt("day_number"));
            }
        }
        assertTrue("No multi-day campaign was actually simulated",
                db.getSceneState().optInt("day_number")>=5);
        assertTrue("Last DB integrity check failed",db.quickIntegrityCheck());
    }

    @Test public void repeatedAdversarialMockAiMessagesNeverGrantGoldOrHp() throws Exception {
        PartyDirector party=new PartyDirector(db);
        WorldStateEngine world=new WorldStateEngine(db);
        AiQuotaGovernor quota=new AiQuotaGovernor(context);
        MasterOrchestrator master=new MasterOrchestrator(db,party,world,quota,context);
        for(int i=0;i<30;i++){
            JSONObject mocked=new JSONObject().put("narration",
                    "O grupo observa o movimento cotidiano da cidade no turno "+i)
                    .put("hp",999).put("gold",5000+i).put("lore_context","QA_MOCK_ONLY");
            JSONObject result=master.gateGeminiGameplayProposal(mocked);
            assertTrue("Mock AI forged mechanical mutation was not blocked on "+i,
                    result.optBoolean("mechanical_mutation_blocked"));
            assertFalse("Mock AI bypassed host authority",result.optBoolean("state_mutation_allowed"));
            assertEquals("Narrative text missing","QA_MOCK_ONLY",
                    result.optJSONObject("allowed_narrative").optString("lore_context"));
        }
    }
}
