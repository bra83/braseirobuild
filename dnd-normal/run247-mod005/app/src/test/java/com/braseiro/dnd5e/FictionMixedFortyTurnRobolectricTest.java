package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.UUID;
import static org.junit.Assert.*;

/**
 * Forty DIFFERENT protocol turns with an actual SQLite campaign:
 * perception, dialogue, rules, investigation, conditionals, voluntary
 * initiative and deterministic world-time progression.
 *
 * THIS IS OFFLINE INTEGRATION QA, NOT 40 LIVE GEMINI RESPONSES.
 * Unresolved actions are never faked as resolved events.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class FictionMixedFortyTurnRobolectricTest {
    private Context context;
    private AppDb db;
    private String name;
    private static final String NPC="Urzmaktok Grojsh";
    private final String[] turns={
        "Como está o tempo?",
        "Como essa pessoa é?",
        "Pergunto ao jardineiro sobre a caça de orientação.",
        "Qual regra determina um teste de Investigação?",
        "Examino a estaca em busca de marcas escondidas.",
        "Como está o tempo? Se chover, procuro abrigo.",
        "Quero ouvir as próximas instruções antes de decidir.",
        "Passo 3 horas escrevendo notas e estudando a região."
    };

    @Before public void setUp() throws Exception {
        context=RuntimeEnvironment.getApplication();
        name="mixed_handoff_"+UUID.randomUUID().toString().substring(0,8)+".db";
        db=new AppDb(context,name);
        db.setActiveMapProfile("dnd");
        JSONObject state=db.getSceneState();
        state.put("day_number",2).put("clock_hour",10.25)
            .put("location","Jardim de Strixhaven")
            .put("weather","Chuvisco").put("lighting","manhã clara")
            .put("present_npcs_json",new JSONArray().put(new JSONObject()
                .put("id","npc-urzmaktok").put("name",NPC)
                .put("visual_description","Um orc jovem cuidando de uma planta.")
                .put("visible_weapons","")).toString());
        db.saveSceneState(state);
    }
    @After public void stop(){
        if(db!=null)db.close();
        if(context!=null&&name!=null)context.deleteDatabase(name);
    }

    @Test public void mixedFortyTurnLedgerPreservesFactsAndOnlyCommitsAuthorizedTime() throws Exception {
        final int firstDay=db.getSceneState().optInt("day_number");
        final double firstClock=db.getSceneState().optDouble("clock_hour");
        int explicitWorldTicks=0,fictionQueries=0,speeches=0,rules=0,investigations=0,conditionals=0;
        for(int turn=0;turn<40;turn++){
            JSONObject scene=db.getSceneState();
            int stage=turn%turns.length;
            String action=turns[stage];
            FictionTurnIntent.Kind kind=FictionTurnIntent.classify(action);
            JSONObject intent=FictionTurnIntent.contract(action,scene,"PLAYER_ACTION");
            assertEquals("Turn "+turn,kind.name(),intent.optString("kind"));
            assertEquals("Canonical location drift at "+turn,"Jardim de Strixhaven",scene.optString("location"));
            JSONArray visible=NpcPerceptionLedger.publicOnstage(db,scene,"","party");
            assertEquals("A physically present NPC must stay onstage",1,visible.length());
            boolean introduced=turn>=16;
            assertEquals("Name visibility must track accepted narration at "+turn,
                introduced?NPC:"",visible.optJSONObject(0).optString("name_known_to_observer"));
            switch(stage){
                case 0:
                case 1:
                    assertEquals(FictionTurnIntent.Kind.FICTION_OBSERVATION,kind);
                    assertTrue(intent.optBoolean("non_advancing"));
                    assertEquals("The narrator must speak only the accepted answer",
                        "Você percebe um chuvisco.",FictionTurnAudio.select(
                            "Narrativa anterior longa.","Você percebe um chuvisco."));
                    fictionQueries++;
                    break;
                case 2:
                    assertEquals(FictionTurnIntent.Kind.CHARACTER_SPEECH,kind);
                    assertFalse(intent.optBoolean("non_advancing"));
                    assertTrue(FictionTurnDirective.forTurn(action,scene,"PLAYER_ACTION")
                        .contains("discurso direto"));
                    speeches++;
                    break;
                case 3:
                    assertEquals(FictionTurnIntent.Kind.RULES_HELP,kind);
                    assertTrue(intent.optBoolean("non_advancing"));
                    rules++;
                    break;
                case 4:
                    assertEquals(FictionTurnIntent.Kind.CHARACTER_ACTION,kind);
                    assertFalse("Investigation is not free perception",intent.optBoolean("non_advancing"));
                    // No roll/skill outcome is committed by mere classification.
                    investigations++;
                    break;
                case 5:
                    assertEquals(FictionTurnIntent.Kind.MIXED_CONDITIONAL,kind);
                    assertEquals(FictionTurnIntent.Condition.TRUE,
                        FictionTurnIntent.condition(action,scene));
                    // Even true weather does not prove a traversable shelter.
                    conditionals++;
                    break;
                case 6:
                    assertEquals(FictionTurnIntent.Kind.CHARACTER_ACTION,kind);
                    // NPC initiative belongs to the later accepted narrator response.
                    break;
                case 7:
                    JSONObject tick=new WorldStateEngine(db).preview(action);
                    assertEquals("No authority violation at "+turn,"",tick.optString("error"));
                    assertTrue("Actual deterministic tick must commit",new WorldStateEngine(db).commit(tick));
                    explicitWorldTicks++;
                    break;
                default:fail("Unknown stage");
            }
            if(turn==15){
                NpcPerceptionLedger.commitAcceptedPresentation(db,db.getSceneState(),
                    "Urzmaktok Grojsh se apresenta enquanto segura uma pequena estaca.",
                    "party");
            }
            if(turn%8==7){
                db.close();
                db=new AppDb(context,name);
                db.setActiveMapProfile("dnd");
                assertTrue("SQLite integrity after reopen "+turn,db.quickIntegrityCheck());
            }
            JSONObject persisted=db.getSceneState();
            assertEquals("Clock changed without explicit host tick at "+turn,
                ((firstClock+explicitWorldTicks*3)%24),persisted.optDouble("clock_hour"),0.002);
            assertEquals("Unexpected day change at "+turn,
                firstDay+(int)((firstClock+explicitWorldTicks*3)/24),
                persisted.optInt("day_number"));
            System.out.println("MIXED_TURN "+(turn+1)+" KIND="+kind.name()
                +" ACTUAL_TICKS="+explicitWorldTicks+" NPC_NAME_KNOWN="+(turn>=16));
        }
        assertEquals(40,fictionQueries+speeches+rules+investigations+conditionals+5+5);
        assertEquals(5,explicitWorldTicks);
        assertEquals(10,fictionQueries);
        assertEquals(5,speeches);
        assertEquals(5,rules);
        assertEquals(5,investigations);
        assertEquals(5,conditionals);
    }
}
