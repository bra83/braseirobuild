package com.braseiro.dnd5e;

import android.content.Context;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Independent data-driven handoff cases: nonadvancement, reference and SQLite.
 * These offline tests are NOT real Gemini narrative quality certification.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class FictionTurnIntentContractRobolectricTest {
    private Context context;
    private AppDb db;
    private String filename;
    private static final String NPC="Urzmaktok Grojsh";
    @Before public void start()throws Exception{
        context=RuntimeEnvironment.getApplication();
        filename="fiction_handoff_"+UUID.randomUUID().toString().substring(0,8)+".db";
        db=new AppDb(context,filename);
        db.setActiveMapProfile("dnd");
        JSONObject scene=db.getSceneState();
        scene.put("weather","Chuvisco").put("clock_hour",10.25)
             .put("day_number",2).put("location","Jardim de Strixhaven")
             .put("lighting","manhã clara")
             .put("present_npcs_json",new JSONArray().put(new JSONObject()
                 .put("id","npc-urzmaktok")
                 .put("name",NPC)
                 .put("visual_description","Orc jovem com avental de jardinagem.")
                 .put("role","jardineiro")).toString());
        db.saveSceneState(scene);
    }
    @After public void stop(){if(db!=null)db.close();if(context!=null&&filename!=null)context.deleteDatabase(filename);}
    private JSONObject scene(){return db.getSceneState();}
    private void observe(String text){FictionTurnIntent.Kind kind=FictionTurnIntent.classify(text);
        assertTrue(text,kind==FictionTurnIntent.Kind.FICTION_OBSERVATION
                    ||kind==FictionTurnIntent.Kind.AMBIGUOUS_REFERENCE);
        assertTrue(text,FictionTurnIntent.freezesWorld(kind,text,scene()));}

    @Test public void T01_threeWeatherAndTimeQuestionsFreeze() {
        double before=scene().optDouble("clock_hour");
        observe("Como está o tempo?");
        observe("Que horas são?");
        observe("Está chovendo?");
        assertEquals(before,scene().optDouble("clock_hour"),.0001);
        assertEquals("Chuvisco",scene().optString("weather"));
    }
    @Test public void T02_descriptionClothingVisibleArmamentAreFiction() {
        observe("Como essa pessoa é?");observe("O que está vestindo?");
        observe("Está armado?");observe("Posso ver alguma arma?");
    }
    @Test public void T03_unknownVisibleWeaponIsNotAssertedAbsent(){
        JSONArray persons=NpcPerceptionLedger.publicOnstage(db,scene(),"","party");
        assertEquals(1,persons.length());
        JSONObject person=persons.optJSONObject(0);
        assertFalse(person.optBoolean("visibility_of_weapons_established"));
        assertEquals("",person.optString("visible_weapons"));
        assertFalse(person.optBoolean("is_identified_by_observer"));
        assertFalse(person.toString().contains("secrets"));
        String resolver=FictionTurnIntent.contract("Como essa pessoa é?",scene(),"PLAYER_ACTION").toString();
        assertFalse("Unknown NPC name leaked through referent",resolver.toLowerCase().contains("urzmaktok"));
        assertFalse("Unknown NPC role leaked through referent",resolver.toLowerCase().contains("jardineiro"));
        assertFalse("Internal ID leaked through referent",resolver.toLowerCase().contains("npc-urzmaktok"));
    }
    @Test public void T04_spatialVisibilityDoesNotInventExit()throws Exception{
        observe("O que vejo à minha esquerda?");
        assertTrue(FictionTurnDirective.forTurn("O que vejo à minha esquerda?",scene(),"PLAYER_ACTION").contains("Não invente"));
        JSONObject dark=new JSONObject(scene().toString()).put("lighting","escuridão total");
        JSONObject hidden=NpcPerceptionLedger.publicOnstage(db,dark,"","party").optJSONObject(0);
        assertTrue(hidden.optBoolean("perception_limited_by_darkness"));
        assertEquals("",hidden.optString("visible_description"));
        assertEquals("",hidden.optString("visible_weapons"));
        assertFalse(hidden.optBoolean("visibility_of_weapons_established"));
    }
    @Test public void T05_twoVisibleNpcsDemandClarification() throws Exception{
        JSONObject s=scene();s.put("present_npcs_json",new JSONArray()
          .put(new JSONObject().put("name","Aisla"))
          .put(new JSONObject().put("name","Urzmaktok Grojsh")).toString());
        JSONObject r=FictionTurnIntent.contract("Como essa pessoa é?",s,"PLAYER_ACTION");
        assertEquals("AMBIGUOUS_REFERENCE",r.optString("kind"));
        assertTrue(r.optBoolean("non_advancing"));
        assertTrue(r.optJSONObject("referent").optBoolean("ambiguous"));
    }
    @Test public void T05b_anonymousPersonRemainsObservableWithoutInventedName() throws Exception {
        JSONObject s=scene();
        s.put("present_npcs_json",new JSONArray().put(new JSONObject()
            .put("id","anonymous-visitor").put("name","")
            .put("visual_description","Uma figura de capa azul junto ao arco.")).toString());
        JSONObject result=FictionTurnIntent.contract("Como essa pessoa é?",s,"PLAYER_ACTION");
        assertEquals("FICTION_OBSERVATION",result.optString("kind"));
        assertTrue(result.optBoolean("non_advancing"));
        JSONObject referent=result.optJSONObject("referent");
        assertFalse("Visible anonymous person must resolve",referent.optBoolean("unresolved"));
        assertEquals(1,referent.optJSONArray("candidates").length());
        assertFalse("Private ID must not be in observation",referent.toString().contains("anonymous-visitor"));
        assertEquals("Uma figura de capa azul junto ao arco.",
            referent.optJSONObject("target").optString("visible_description"));
    }
    @Test public void T06_speechToExistingNpcDoesNotBecomeHelp(){
        assertEquals(FictionTurnIntent.Kind.CHARACTER_SPEECH,
            FictionTurnIntent.classify("Pergunto a ela por que está me olhando."));
        assertTrue(FictionTurnDirective.forTurn("Pergunto a ela por que está me olhando.",scene(),"PLAYER_ACTION")
            .contains("discurso direto"));
    }
    @Test public void T07_activeSearchIsNotFreePerception(){
        assertEquals(FictionTurnIntent.Kind.CHARACTER_ACTION,
            FictionTurnIntent.classify("Examino sob o casaco para procurar arma escondida."));
        assertEquals(FictionTurnIntent.Kind.CHARACTER_ACTION,
            FictionTurnIntent.classify("Quero fazer um teste para desvendar a pista."));
    }
    @Test public void T08_rulesHelpInMainFieldFreezes(){
        JSONObject c=FictionTurnIntent.contract("Qual regra determina a Percepção passiva?",scene(),"PLAYER_ACTION");
        assertEquals("RULES_HELP",c.optString("kind"));assertTrue(c.optBoolean("non_advancing"));
        assertFalse("Main-field rules don't require switching pane",
            MainActivity.shouldRouteNarratorQuestionToMaster("Qual regra determina a Percepção passiva?"));
    }
    @Test public void T09_orderedRainConditionTrueFalseUnknown() throws Exception{
        String q="Como está o tempo? Se chover, procuro abrigo.";
        assertEquals(FictionTurnIntent.Kind.MIXED_CONDITIONAL,FictionTurnIntent.classify(q));
        assertEquals(FictionTurnIntent.Condition.TRUE,FictionTurnIntent.condition(q,scene()));
        assertFalse(FictionTurnIntent.contract(q,scene(),"PLAYER_ACTION").optBoolean("non_advancing"));
        JSONObject clear=new JSONObject(scene().toString()).put("weather","Céu limpo");
        assertEquals(FictionTurnIntent.Condition.FALSE,FictionTurnIntent.condition(q,clear));
        assertTrue(FictionTurnIntent.contract(q,clear,"PLAYER_ACTION").optBoolean("non_advancing"));
        JSONObject unreported=new JSONObject(scene().toString()).put("weather","");
        assertEquals(FictionTurnIntent.Condition.UNKNOWN,FictionTurnIntent.condition(q,unreported));
        assertTrue(FictionTurnIntent.contract(q,unreported,"PLAYER_ACTION").optBoolean("non_advancing"));
    }
    @Test public void T10_firstAndReopenedNpcPresentationAreStable() throws Exception{
        assertEquals("",NpcPerceptionLedger.publicOnstage(db,scene(),"","party")
            .optJSONObject(0).optString("name_known_to_observer"));
        NpcPerceptionLedger.commitAcceptedPresentation(db,scene(),
            "Urzmaktok Grojsh ajusta um ramo diante de você.","party");
        assertEquals(NPC,NpcPerceptionLedger.publicOnstage(db,scene(),"","party")
            .optJSONObject(0).optString("name_known_to_observer"));
        db.close();db=new AppDb(context,filename);db.setActiveMapProfile("dnd");
        assertEquals(NPC,NpcPerceptionLedger.publicOnstage(db,scene(),"","party")
            .optJSONObject(0).optString("name_known_to_observer"));
        assertEquals("",NpcPerceptionLedger.publicOnstage(db,scene(),"","other_hero")
            .optJSONObject(0).optString("name_known_to_observer"));
    }
    @Test public void T11_locationDiscoveryIsSeparateFromNpcRegistration(){
        assertEquals(0,db.getNarrativeDiscovery("location","secret_place").length());
        assertEquals(0,db.getNarrativeDiscovery("npc_observed","party|npc-urzmaktok").length());
    }
    @Test public void T12_fourActionsGroundedToAcceptedScene() throws Exception {
        String beat=SceneScreenshotFourActionsRegressionTest.LIVE_SCENE;
        JSONArray actions=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            beat,"",new JSONObject().put("location","Jardim de Strixhaven"),35);
        assertEquals(4,actions.length());
        assertTrue(StoryMomentumAction.isRequest(actions.optJSONObject(0).optString("action")));
    }
    @Test public void T13_perceptionDoesNotRebindSavedOptions(){
        String beat=SceneScreenshotFourActionsRegressionTest.LIVE_SCENE;
        JSONArray choices=SceneOptionDiversifier.ensureAlways(new JSONArray(),new JSONArray(),
            beat,"",new JSONObject(),35);
        observe("O que está vestindo?");
        assertEquals(choices.toString(),StoryMomentumAction.ensureVisible(choices,beat).toString());
    }
    @Test public void T14_staleActionTicketCannotClickNewScene(){
        String beat=SceneScreenshotFourActionsRegressionTest.LIVE_SCENE;
        JSONArray cards=StoryMomentumAction.ensureVisible(new JSONArray(),beat);
        assertFalse(SceneActionEvidence.ticketMatches(cards.optJSONObject(0),
            "Um mago desconhecido surge em outra cidade."));
    }
    @Test public void T15_pureIntentDoesNotTickOrQueue(){
        int day=scene().optInt("day_number");
        for(int i=0;i<6;i++)observe("Como essa pessoa é?");
        assertEquals(day,scene().optInt("day_number"));
    }
    @Test public void T16_nullOrIncompleteInputFailsClosed(){
        assertEquals(FictionTurnIntent.Kind.CHARACTER_ACTION,FictionTurnIntent.classify(null));
        assertEquals(FictionTurnIntent.Condition.UNKNOWN,
            FictionTurnIntent.condition("Como está o tempo? Se chover, procuro abrigo.",new JSONObject()));
    }
    @Test public void T17_groundingHasStableIdButNoHiddenName() {
        JSONObject o=NpcPerceptionLedger.publicOnstage(db,scene(),"","party").optJSONObject(0);
        assertTrue(o.optString("stable_ref").startsWith("npc_"));
        assertFalse("Unknown NPC name must not appear even inside the stable ID",
            o.optString("stable_ref").toLowerCase().contains("urzmaktok"));
        assertFalse("Internal role must not leak",o.has("role"));
        assertEquals("",o.optString("name_known_to_observer"));
    }
    @Test public void T18_combatAppearanceQuestionIsFree() {
        observe("Está armado?");
        assertTrue(FictionTurnDirective.forTurn("Está armado?",scene(),"PLAYER_ACTION").contains("combate"));
    }
    @Test public void T19_pendingTestIsNotResolvedByPerception() throws Exception {
        JSONObject test=new JSONObject().put("required",true).put("skill","Investigação");
        observe("Como essa pessoa é?");
        assertTrue(test.optBoolean("required"));
        assertEquals("Investigação",test.optString("skill"));
    }
    @Test public void T20_fortyTurnOfflineReplayPreservesClockNpcAndKnowledge() throws Exception{
        final String[] queries={"Como está o tempo?","Como essa pessoa é?","O que está vestindo?",
          "Está armado?","O que vejo à minha esquerda?","Posso ver alguma arma?"};
        JSONObject initial=scene();int day=initial.optInt("day_number");
        double hour=initial.optDouble("clock_hour");
        for(int i=0;i<40;i++){
            if(i%5==4){
                // Real deterministic, explicit-world tick; NOT an observation.
                JSONObject tick=new WorldStateEngine(db).preview(
                    "Passo 3 horas escrevendo notas e estudando a região");
                assertEquals("",tick.optString("error"));
                assertTrue(new WorldStateEngine(db).commit(tick));
            }else{
                String q=queries[i%queries.length];
                assertTrue(FictionTurnIntent.freezesWorld(FictionTurnIntent.classify(q),q,scene()));
                assertFalse(FictionTurnIntent.classify(q)==FictionTurnIntent.Kind.RULES_HELP);
            }
            JSONObject now=scene();
            assertEquals("Location drift on "+i,"Jardim de Strixhaven",now.optString("location"));
            if(i%10==9){
                db.close();db=new AppDb(context,filename);db.setActiveMapProfile("dnd");
                assertTrue(db.quickIntegrityCheck());
            }
            System.out.println("REPLAY_TURN "+(i+1)+" MODE="+(i%5==4?"HOST_TICK":"FICTION_OBSERVATION")
                +" DAY="+now.optInt("day_number")+" CLOCK="+now.optDouble("clock_hour"));
        }
        assertEquals(day+1,scene().optInt("day_number"));
        assertEquals(hour,scene().optDouble("clock_hour"),.002);
        assertTrue(db.quickIntegrityCheck());
    }
}
