package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression tests for real Tome -> per-system dice -> Loreweaver -> AdventureState. */
public class UniversalGithubModulesIntegrationTest {
    private static JSONObject op(String action) throws Exception {return new JSONObject().put("action",action);}
    private static JSONArray faces(int... v) throws Exception {JSONArray a=new JSONArray();for(int i:v)a.put(i);return a;}
    private static JSONObject run(AdventureState s,JSONObject cmd,String expected) throws Exception {
        JSONObject o=UniversalRpgEngine.process(s,cmd);
        assertEquals("unexpected: "+o,expected,o.optString("status"));return o;
    }
    private static JSONObject register(AdventureState s,String id,String system) throws Exception {return run(s,op("register")
        .put("character_id",id).put("system_id",system).put("name","Hero "+id)
        .put("attributes",new JSONObject().put("strength",3))
        .put("resources",new JSONObject().put("health",new JSONObject().put("current",4).put("maximum",6))),"registered");}
    private static JSONObject begin(AdventureState s,String id,String system,JSONObject spec) throws Exception {return run(s,op("begin_check")
        .put("character_id",id).put("system_id",system).put("situation","Ação sob risco")
        .put("on_success","Sucesso no mundo").put("on_failure","Falha no mundo").put("spec",spec),"requires_roll");}
    private static JSONObject finish(AdventureState s,JSONObject pending,JSONObject submission) throws Exception {return run(s,op("resolve_check")
        .put("request_id",pending.getString("request_id")).put("submission",submission),"resolved");}

    private static JSONObject proposedScene(String kind,JSONArray success,JSONArray failure) throws Exception {
        JSONObject spec="forbidden-lands".equals(kind)
            ? new JSONObject().put("dice_pool",new JSONObject().put("attribute",2).put("skill",1).put("gear",0))
            : new JSONObject().put("dc",12).put("modifier",1);
        return new JSONObject().put("narration","Uma passagem instável exige uma decisão rápida do grupo.")
            .put("check",new JSONObject().put("required",true)
                .put("situation","Cruzar a passagem sob risco")
                .put("on_success","Atravessa com segurança")
                .put("on_failure","Escorrega e sofre as consequências")
                .put("spec",spec)
                .put("on_success_effects",success).put("on_failure_effects",failure));
    }
    private static JSONObject proposedEffect(String key,int delta) throws Exception {
        return new JSONObject().put("kind","resource").put("key",key).put("delta",delta);
    }
    @Test public void confirmedAiProposalUpdatesFichaInventarioTempoWorldAndSurvivesReload() throws Exception {
        AdventureState campaign=new AdventureState("live");register(campaign,"scout5","forbidden-lands");
        JSONArray success=new JSONArray().put(proposedEffect("health",-1))
            .put(new JSONObject().put("kind","item").put("item_id","rope").put("name","Corda").put("delta",1))
            .put(new JSONObject().put("kind","time").put("minutes",20))
            .put(new JSONObject().put("kind","world_fact").put("fact_id","bridge.005").put("text","A ponte agora está danificada."));
        JSONObject incoming=proposedScene("forbidden-lands",success,new JSONArray());
        JSONObject pending=run(campaign,op("accept_ai_scene").put("event_id","event.005.intro")
            .put("character_id","scout5").put("raw_response",incoming.toString()),"requires_roll").getJSONObject("pending");
        // The model's proposal must NOT mutate the world or the sheet.
        assertEquals(4,campaign.getUniversalRpgState().getJSONObject("entities").getJSONObject("scout5")
            .getJSONObject("resources").getJSONObject("health").optInt("current"));
        JSONObject confirmed=run(campaign,op("resolve_check").put("request_id",pending.getString("request_id"))
            .put("mode","manual").put("outcome","success"),"resolved").getJSONObject("receipt");
        assertEquals(4,confirmed.getJSONArray("proposed_effects").length());
        assertEquals(4,campaign.getUniversalRpgState().getJSONObject("entities").getJSONObject("scout5")
            .getJSONObject("resources").getJSONObject("health").optInt("current"));
        AdventureState reopened=AdventureState.fromJson(new JSONObject(campaign.toJson().toString()));
        JSONObject applied=run(reopened,op("apply_effects").put("event_id","approval.005.001")
            .put("character_id","scout5").put("request_id",confirmed.getString("request_id"))
            .put("expected_revision",0).put("effects",success),"effects_applied");
        assertTrue(applied.getBoolean("committed"));
        AdventureState restored=AdventureState.fromJson(new JSONObject(reopened.toJson().toString()));
        JSONObject state=restored.getUniversalRpgState();
        assertEquals(3,state.getJSONObject("entities").getJSONObject("scout5")
            .getJSONObject("resources").getJSONObject("health").optInt("current"));
        assertEquals(1,state.getJSONObject("entities").getJSONObject("scout5").getJSONObject("inventory")
            .getJSONArray("items").getJSONObject(0).optInt("quantity"));
        assertEquals(20,state.optInt("elapsed_minutes"));
        assertEquals("bridge.005",state.getJSONArray("world_facts").getJSONObject(0).optString("id"));
        assertTrue(state.getJSONArray("history").getJSONObject(0).optBoolean("effects_applied"));
        assertEquals("blocked",UniversalRpgEngine.process(restored,op("apply_effects")
            .put("event_id","approval.005.002").put("character_id","scout5")
            .put("request_id",confirmed.getString("request_id"))
            .put("expected_revision",2).put("effects",success)).optString("status"));
    }
    @Test public void declinedEffectsStayDeclinedAfterSessionReload() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"scout6","forbidden-lands");
        JSONObject pending=run(s,op("accept_ai_scene").put("event_id","event.005.decline")
            .put("character_id","scout6").put("raw_response",proposedScene("forbidden-lands",
                new JSONArray(),new JSONArray().put(proposedEffect("health",-2))).toString()),"requires_roll").getJSONObject("pending");
        JSONObject receipt=run(s,op("resolve_check").put("request_id",pending.getString("request_id"))
            .put("mode","manual").put("outcome","failure"),"resolved").getJSONObject("receipt");
        run(s,op("decline_effects").put("character_id","scout6")
            .put("request_id",receipt.getString("request_id")),"effects_declined");
        AdventureState copy=AdventureState.fromJson(new JSONObject(s.toJson().toString()));
        assertTrue(copy.getUniversalRpgState().getJSONArray("history").getJSONObject(0).optBoolean("effects_declined"));
        assertEquals("blocked",UniversalRpgEngine.process(copy,op("apply_effects").put("event_id","denied.005.1")
            .put("character_id","scout6").put("request_id",receipt.getString("request_id"))
            .put("expected_revision",0).put("effects",new JSONArray().put(proposedEffect("health",-2)))).optString("status"));
        assertEquals(4,copy.getUniversalRpgState().getJSONObject("entities").getJSONObject("scout6")
            .getJSONObject("resources").getJSONObject("health").optInt("current"));
    }
    @Test public void unapprovedMutationOfAiProposalIsRejectedAtomically() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"hero7","dnd5e");
        JSONArray proposal=new JSONArray().put(proposedEffect("health",-1))
            .put(new JSONObject().put("kind","time").put("minutes",5));
        JSONObject p=run(s,op("accept_ai_scene").put("event_id","event.005.reject")
            .put("character_id","hero7")
            .put("raw_response",proposedScene("dnd5e",proposal,new JSONArray()).toString()),"requires_roll")
            .getJSONObject("pending");
        JSONObject rec=run(s,op("resolve_check").put("request_id",p.optString("request_id"))
            .put("mode","manual").put("outcome","success"),"resolved").getJSONObject("receipt");
        JSONArray fake=new JSONArray().put(proposedEffect("health",-3))
            .put(new JSONObject().put("kind","time").put("minutes",5));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("apply_effects")
            .put("event_id","fraud.005.001").put("character_id","hero7")
            .put("request_id",rec.getString("request_id"))
            .put("expected_revision",0).put("effects",fake)).optString("status"));
        JSONObject unchanged=s.getUniversalRpgState();
        assertEquals(4,unchanged.getJSONObject("entities").getJSONObject("hero7")
            .getJSONObject("resources").getJSONObject("health").optInt("current"));
        assertEquals(0,unchanged.optInt("elapsed_minutes"));
        assertFalse(unchanged.getJSONArray("history").getJSONObject(0).optBoolean("effects_applied"));
    }
    @Test public void malformedAiEffectsDoNotRegisterSceneOrPendingCheck() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"hero8","dnd5e");
        JSONObject invalid=proposedScene("dnd5e",new JSONArray().put(proposedEffect("health",1)),new JSONArray());
        // Fractional value must not be truncated by the parser.
        invalid.getJSONObject("check").put("on_success_effects",new JSONArray()
            .put(new JSONObject().put("kind","resource").put("key","health").put("delta",1.5)));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("accept_ai_scene")
            .put("event_id","event.005.invalid").put("character_id","hero8")
            .put("raw_response",invalid.toString())).optString("status"));
        JSONObject unchanged=s.getUniversalRpgState();
        assertEquals(0,unchanged.optJSONObject("pending")==null?0:unchanged.optJSONObject("pending").length());
        assertEquals(0,unchanged.optJSONArray("scenes")==null?0:unchanged.optJSONArray("scenes").length());
    }
    @Test public void missingResourceAbortsEntireConfirmedBatch() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"hero9","dnd5e");
        JSONArray effects=new JSONArray().put(new JSONObject().put("kind","time").put("minutes",40))
            .put(proposedEffect("unknown_resource",-1));
        JSONObject p=run(s,op("accept_ai_scene").put("event_id","event.005.unknown")
            .put("character_id","hero9").put("raw_response",proposedScene("dnd5e",effects,new JSONArray()).toString()),"requires_roll")
            .getJSONObject("pending");
        JSONObject receipt=run(s,op("resolve_check").put("request_id",p.optString("request_id"))
            .put("mode","manual").put("outcome","success"),"resolved").getJSONObject("receipt");
        assertEquals("blocked",UniversalRpgEngine.process(s,op("apply_effects")
            .put("event_id","apply.005.unknown").put("character_id","hero9")
            .put("request_id",receipt.optString("request_id")).put("expected_revision",0)
            .put("effects",effects)).optString("status"));
        JSONObject unchanged=s.getUniversalRpgState();
        assertEquals(0,unchanged.optInt("elapsed_minutes"));
        assertEquals(4,unchanged.getJSONObject("entities").getJSONObject("hero9")
            .getJSONObject("resources").getJSONObject("health").optInt("current"));
        assertEquals(1,unchanged.getJSONArray("history").length());
        assertFalse(unchanged.getJSONArray("history").getJSONObject(0).optBoolean("effects_applied"));
    }

    @Test public void starWarsRollKeepsOnlyTheChosenOutcomeProposal() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"pilot005","star-wars-ffg");
        JSONArray yes=new JSONArray().put(new JSONObject().put("kind","world_fact")
            .put("fact_id","escape.005").put("text","A nave escapou do bloqueio."));
        JSONArray no=new JSONArray().put(new JSONObject().put("kind","time").put("minutes",15));
        JSONObject ai=proposedScene("dnd5e",yes,no);
        ai.getJSONObject("check").put("spec",new JSONObject().put("dice_pool",new JSONObject()
            .put("ability",1).put("difficulty",1)));
        JSONObject check=run(s,op("accept_ai_scene").put("event_id","scene.005.sw")
            .put("character_id","pilot005").put("raw_response",ai.toString()),"requires_roll")
            .getJSONObject("pending");
        // Ability face 1 = blank; Difficulty face 3 = two failures.
        JSONObject resolution=run(s,op("resolve_check")
            .put("request_id",check.getString("request_id"))
            .put("submission",new JSONObject().put("dice",new JSONArray()
                .put(new JSONObject().put("die","ability").put("face",1))
                .put(new JSONObject().put("die","difficulty").put("face",3)))),"resolved");
        JSONObject receipt=resolution.getJSONObject("receipt");
        assertFalse(receipt.getJSONObject("resolution").getBoolean("success"));
        assertEquals("time",receipt.getJSONArray("proposed_effects").getJSONObject(0).optString("kind"));
        run(s,op("apply_effects").put("event_id","approved.005.sw")
            .put("character_id","pilot005").put("request_id",receipt.optString("request_id"))
            .put("expected_revision",0).put("effects",no),"effects_applied");
        assertEquals(15,s.getUniversalRpgState().optInt("elapsed_minutes"));
        assertEquals(0,s.getUniversalRpgState().optJSONArray("world_facts")==null?0:
            s.getUniversalRpgState().optJSONArray("world_facts").length());
    }
    @Test public void aiCannotHideNonArrayConsequences() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"actor005","dnd5e");
        JSONObject ai=proposedScene("dnd5e",new JSONArray(),new JSONArray());
        ai.getJSONObject("check").put("on_failure_effects",new JSONObject()
            .put("kind","resource").put("delta",-99999));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("accept_ai_scene")
            .put("event_id","scene.005.hidden").put("character_id","actor005")
            .put("raw_response",ai.toString())).optString("status"));
        assertEquals(0,s.getUniversalRpgState().optJSONArray("scenes")==null?0:
            s.getUniversalRpgState().optJSONArray("scenes").length());
    }
    @Test public void tomeYearZeroLoreweaverAreOnePersistentPipeline() throws Exception {
        AdventureState s=new AdventureState("cena-1");
        JSONObject entity=register(s,"yze","forbidden-lands").getJSONObject("entity");
        assertEquals("entity",entity.getJSONObject("tome").getString("format"));
        assertEquals("forbidden-lands",entity.getJSONObject("meta").getString("system"));
        JSONObject p=begin(s,"yze","forbidden-lands",new JSONObject().put("dice_pool",new JSONObject()
            .put("attribute",2).put("skill",1).put("gear",1))).getJSONObject("pending");
        JSONObject submission=new JSONObject().put("dice",new JSONObject()
            .put("attribute",faces(6,2)).put("skill",faces(3)).put("gear",faces(1)));
        JSONObject receipt=finish(s,p,submission).getJSONObject("receipt");
        JSONObject outcome=receipt.getJSONObject("resolution").getJSONObject("outcome");
        assertTrue(outcome.getBoolean("success")); assertEquals(1,outcome.optInt("tier"));
        assertFalse(receipt.getBoolean("effects_applied"));
        AdventureState restored=AdventureState.fromJson(new JSONObject(s.toJson().toString()));
        assertEquals(1,restored.getUniversalRpgState().getJSONArray("history").length());
        assertEquals("blocked",UniversalRpgEngine.process(restored,op("resolve_check")
           .put("request_id",p.getString("request_id")).put("mode","manual").put("outcome","failure")).getString("status"));
    }
    @Test public void starWarsNarrativeFacesCancelCorrectlyAndNeverMixPools() throws Exception {
        AdventureState s=new AdventureState("hangar");register(s,"pilot","star-wars-ffg");
        JSONObject p=begin(s,"pilot","star-wars-ffg",new JSONObject().put("dice_pool",new JSONObject()
            .put("ability",1).put("difficulty",1))).getJSONObject("pending");
        JSONObject bad=new JSONObject().put("dice",new JSONArray()
            .put(new JSONObject().put("die","ability").put("face",11))
            .put(new JSONObject().put("die","difficulty").put("face",2)));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("resolve_check")
            .put("request_id",p.getString("request_id")).put("submission",bad)).getString("status"));
        JSONObject good=new JSONObject().put("dice",new JSONArray()
            .put(new JSONObject().put("die","ability").put("face",4))
            .put(new JSONObject().put("die","difficulty").put("face",2)));
        JSONObject resolved=finish(s,p,good).getJSONObject("receipt").getJSONObject("resolution");
        assertEquals(1,resolved.optInt("net_success")); assertTrue(resolved.getJSONObject("outcome").getBoolean("success"));
        assertEquals(1,s.getUniversalRpgState().getJSONArray("history").length());
        JSONObject force=StarWarsNarrativeDicePort.resolve(new JSONObject().put("check_kind","force")
            .put("dice_pool",new JSONObject().put("force",1)),new JSONObject()
            .put("dice",new JSONArray().put(new JSONObject().put("die","force").put("face",10))));
        assertTrue(force.getBoolean("force_pool"));assertEquals(2,force.optInt("light_side"));
    }
    @Test public void yearZeroPushCountsConsequencesButDoesNotApplyThemSilently() throws Exception {
        JSONObject spec=new JSONObject().put("dice_pool",new JSONObject().put("attribute",2).put("skill",1).put("gear",1));
        JSONObject result=YearZeroDicePort.resolve(spec,new JSONObject()
            .put("dice",new JSONObject().put("attribute",faces(1,2))
            .put("skill",faces(3)).put("gear",faces(1)))
            .put("push",new JSONObject().put("attribute",faces(1)).put("skill",faces(6)).put("gear",faces())));
        assertTrue(result.optBoolean("resolved"));
        assertEquals(2,result.optInt("attribute_damage_from_push"));
        assertEquals(1,result.optInt("gear_damage_from_push"));
        assertEquals(1,result.optInt("successes"));
        assertFalse(result.getBoolean("effects_applied"));
    }
    @Test public void staleSceneIsRejectedAndResourceIsBoundedWithReceipt() throws Exception {
        AdventureState s=new AdventureState("ruina");register(s,"hero","dnd5e");
        JSONObject pending=run(s,op("ai_roll_tag").put("character_id","hero")
            .put("system_id","dnd5e").put("narration","Teste: [[ROLL:1d20+2]]")
            .put("dc",12).put("situation","Fenda aberta"),"requires_roll").getJSONObject("pending");
        s.setCurrentNodeId("outra-cena");
        assertEquals("blocked",UniversalRpgEngine.process(s,op("resolve_check").put("request_id",pending.getString("request_id"))
            .put("mode","manual").put("outcome","success")).getString("status"));
        s.setCurrentNodeId("ruina");
        JSONObject r=run(s,op("resolve_check").put("request_id",pending.getString("request_id"))
            .put("mode","manual").put("outcome","success"),"resolved");
        assertTrue(r.getJSONObject("receipt").getJSONObject("resolution").getJSONObject("outcome").getBoolean("success"));
        JSONObject update=run(s,op("resource").put("event_id","unique-0001").put("character_id","hero")
            .put("resource","health").put("delta",-2).put("expected_revision",0),"resource_updated");
        assertEquals(2,update.getJSONObject("receipt").getJSONObject("resource_change").optInt("after"));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("resource").put("event_id","unique-0001")
            .put("character_id","hero").put("resource","health").put("delta",-2)
            .put("expected_revision",1)).getString("status"));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("resource").put("event_id","fractional-0001")
            .put("character_id","hero").put("resource","health").put("delta",0.75)
            .put("expected_revision",1)).getString("status"));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("resource").put("event_id","unique-0002")
            .put("character_id","hero").put("resource","health").put("delta",-2)
            .put("expected_revision",0)).getString("status"));
        AdventureState loaded=AdventureState.fromJson(new JSONObject(s.toJson().toString()));
        assertEquals(2,loaded.getUniversalRpgState().getJSONObject("entities").getJSONObject("hero")
            .getJSONObject("resources").getJSONObject("health").optInt("current"));
        assertEquals(2,loaded.getUniversalRpgState().getJSONArray("history").length());
    }
    @Test public void dndExplicitD20AlsoProducesTheSameLoreweaverEnvelope() throws Exception {
        AdventureState state=new AdventureState("torre");register(state,"knight","dnd5e");
        JSONObject pending=begin(state,"knight","dnd5e",new JSONObject()
             .put("dc",15).put("modifier",3)).getJSONObject("pending");
        JSONObject resolved=finish(state,pending,new JSONObject().put("d20",14));
        JSONObject outcome=resolved.getJSONObject("receipt").getJSONObject("resolution").getJSONObject("outcome");
        assertTrue(outcome.getBoolean("success"));assertEquals(2,outcome.optInt("margin"));
    }
    @Test public void fourUpstreamModulesRegisteredAndNoImplicitStateEdits() throws Exception {
        JSONArray modules=UniversalGithubModules.catalog(); assertEquals(5,modules.length());
        for(int i=0;i<5;i++)assertEquals("MIT",modules.getJSONObject(i).getString("license"));
        assertEquals("blocked",AetherTableRollIntentPort.parseDndRequest("[[ROLL:999d20]]",12).getString("status"));
        assertEquals("blocked",AetherTableRollIntentPort.parseDndRequest("[[ROLL:1d20]] [[ROLL:1d20]]",12).getString("status"));
        assertEquals("blocked",UniversalRpgEngine.process(new AdventureState("start"),op("register")
            .put("character_id","fake").put("system_id","untrusted-rules")
            .put("name","invasor")).getString("status"));
    }
    @Test public void aiNarrationAndYearZeroCheckShareOneAtomicState() throws Exception {
        AdventureState s=new AdventureState("live"); register(s,"scout","forbidden-lands");
        JSONObject ai=new JSONObject().put("narration","Uma ponte instável range diante dos viajantes.")
            .put("check",new JSONObject().put("required",true)
            .put("situation","Atravessar a ponte instável")
            .put("on_success","Chega ao outro lado")
            .put("on_failure","Fica preso na ponte")
            .put("spec",new JSONObject().put("dice_pool",new JSONObject()
                .put("attribute",2).put("skill",1).put("gear",0))));
        JSONObject scene=run(s,op("accept_ai_scene").put("event_id","scene-yze-001")
            .put("character_id","scout").put("raw_response",ai.toString()),"requires_roll");
        assertTrue(scene.getBoolean("committed"));
        assertEquals(1,s.getUniversalRpgState().getJSONArray("scenes").length());
        JSONObject pending=scene.getJSONObject("pending");
        assertEquals("blocked", UniversalRpgEngine.process(s,op("accept_ai_scene")
           .put("event_id","scene-yze-002").put("character_id","scout")
           .put("raw_response",ai.toString())).getString("status"));
        JSONObject resolved=run(s,op("resolve_check").put("request_id",pending.getString("request_id"))
            .put("mode","manual").put("outcome","success"),"resolved");
        assertTrue(resolved.getJSONObject("receipt").getJSONObject("resolution").getBoolean("success"));
        AdventureState persisted=AdventureState.fromJson(new JSONObject(s.toJson().toString()));
        assertEquals(1,persisted.getUniversalRpgState().getJSONArray("history").length());
        JSONObject continued=run(persisted,op("accept_ai_scene")
            .put("event_id","scene-yze-002").put("character_id","scout")
            .put("raw_response",new JSONObject().put("narration","Você alcança a margem e ouve uma voz nas árvores.").toString()),"scene_recorded");
        assertFalse(continued.optBoolean("check_pending"));
        assertEquals(2,persisted.getUniversalRpgState().getJSONArray("scenes").length());
    }

    @Test public void starWarsAiNarrativeAndDiceShareTomeAndLoreweaver() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"pilot-ai","star-wars-ffg");
        JSONObject ai=new JSONObject().put("narration","O destróier imperial fecha o corredor de fuga.")
            .put("check",new JSONObject().put("required",true)
                .put("situation","Passar pelo bloqueio estelar")
                .put("on_success","Escapa do bloqueio")
                .put("on_failure","É interceptado")
                .put("spec",new JSONObject().put("dice_pool",new JSONObject().put("ability",1).put("difficulty",1))));
        JSONObject pending=run(s,op("accept_ai_scene").put("event_id","scene-star-001")
            .put("character_id","pilot-ai").put("raw_response",ai.toString()),"requires_roll").getJSONObject("pending");
        JSONObject result=run(s,op("resolve_check")
            .put("request_id",pending.getString("request_id"))
            .put("submission",new JSONObject().put("dice",new JSONArray()
                .put(new JSONObject().put("die","ability").put("face",4))
                .put(new JSONObject().put("die","difficulty").put("face",2)))),"resolved");
        assertEquals(1,result.getJSONObject("receipt").getJSONObject("resolution").optInt("net_success"));
    }

    @Test public void untrustedAiCannotApplyHitPointsOrChangeSystem() throws Exception {
        AdventureState s=new AdventureState("live");register(s,"hero-ai","dnd5e");
        JSONObject request=new JSONObject().put("narration","Um ogro golpeia sua armadura. Você recua.")
            .put("hp",-500).put("check",new JSONObject().put("required",true)
                .put("situation","Resistir ao golpe")
                .put("on_success","Permanece de pé").put("on_failure","É derrubado")
                .put("spec",new JSONObject().put("dc",15).put("modifier",2)));
        JSONObject result=run(s,op("accept_ai_scene").put("event_id","scene-dnd-001")
            .put("character_id","hero-ai").put("raw_response",request.toString()),"requires_roll");
        assertEquals(4,s.getUniversalRpgState().getJSONObject("entities")
            .getJSONObject("hero-ai").getJSONObject("resources").getJSONObject("health").optInt("current"));
        assertEquals("blocked",UniversalRpgEngine.process(s,op("accept_ai_scene")
            .put("event_id","scene-dnd-002").put("character_id","hero-ai")
            .put("raw_response","{broken json")).getString("status"));
        AdventureState q=new AdventureState("live");register(q,"pilot","star-wars-ffg");
        JSONObject wrong=new JSONObject().put("narration","O piloto tenta fugir da patrulha espacial.")
            .put("check",new JSONObject().put("required",true)
                .put("situation","Testar pilotagem").put("on_success","Escapa")
                .put("on_failure","Cai").put("spec",new JSONObject().put("dice_pool",new JSONObject().put("gear",2))));
        assertEquals("blocked",UniversalRpgEngine.process(q,op("accept_ai_scene")
            .put("event_id","scene-star-999").put("character_id","pilot")
            .put("raw_response",wrong.toString())).getString("status"));
        assertEquals(0,q.getUniversalRpgState().optJSONArray("scenes")==null?0:
            q.getUniversalRpgState().optJSONArray("scenes").length());
    }


    @Test public void twoResourcesInventoryTimeAndWorldFactAreAtomicAndPersisted() throws Exception {
        AdventureState state=new AdventureState("crossroad");
        register(state,"yze-merchant",UniversalRpgEngine.YZE);
        JSONObject effect=op("apply_effects").put("event_id","evt.bundle.0001")
            .put("character_id","yze-merchant").put("expected_revision",0)
            .put("effects",new JSONArray()
                .put(new JSONObject().put("kind","resource").put("key","health").put("delta",-2))
                .put(new JSONObject().put("kind","item").put("item_id","torch").put("name","Tocha").put("delta",2))
                .put(new JSONObject().put("kind","time").put("minutes",35))
                .put(new JSONObject().put("kind","world_fact").put("fact_id","bridge.001")
                    .put("text","A ponte a oeste está quebrada.")));
        JSONObject accepted=run(state,effect,"effects_applied");
        assertEquals(4,accepted.getJSONObject("receipt").getJSONArray("effects").length());
        AdventureState persisted=AdventureState.fromJson(new JSONObject(state.toJson().toString()));
        JSONObject result=persisted.getUniversalRpgState();
        JSONObject hero=result.getJSONObject("entities").getJSONObject("yze-merchant");
        assertEquals(2,hero.getJSONObject("resources").getJSONObject("health").optInt("current"));
        assertEquals(2,hero.getJSONObject("inventory").getJSONArray("items").getJSONObject(0).optInt("quantity"));
        assertEquals(35,result.optInt("elapsed_minutes"));
        assertEquals(1,result.getJSONArray("world_facts").length());
        assertEquals("blocked",UniversalRpgEngine.process(persisted,effect).getString("status"));
        assertEquals(1,persisted.getUniversalRpgState().getJSONArray("history").length());
    }
    @Test public void aBadEffectRollsBackEveryOtherMutation() throws Exception {
        AdventureState s=new AdventureState("cave");register(s,"hero-effects",UniversalRpgEngine.DND);
        String before=s.getUniversalRpgState().getJSONObject("entities").getJSONObject("hero-effects")
            .getJSONObject("resources").getJSONObject("health").optString("current");
        JSONObject fail=UniversalRpgEngine.process(s,op("apply_effects").put("event_id","bad.sequence.1")
            .put("character_id","hero-effects").put("expected_revision",0)
            .put("effects",new JSONArray()
                .put(new JSONObject().put("kind","resource").put("key","health").put("delta",-2))
                .put(new JSONObject().put("kind","item").put("item_id","fake").put("delta",-1))));
        assertEquals("blocked",fail.getString("status"));
        JSONObject state=s.getUniversalRpgState();
        assertEquals(before,state.getJSONObject("entities").getJSONObject("hero-effects")
            .getJSONObject("resources").getJSONObject("health").optString("current"));
        assertEquals(0,state.optInt("elapsed_minutes"));
        assertEquals(0,state.optJSONArray("history")==null?0:state.optJSONArray("history").length());
    }
    @Test public void effectsCannotBeCommittedWithoutPlayerApprovalOrAgainstStaleSheet() throws Exception {
        AdventureState s=new AdventureState("camp");register(s,"a","dnd5e");
        JSONObject cmd=op("apply_effects").put("event_id","event.approval.001")
            .put("character_id","a").put("expected_revision",0)
            .put("effects",new JSONArray().put(new JSONObject().put("kind","time").put("minutes",10)));
        run(s,cmd,"effects_applied");
        JSONObject stale=op("apply_effects").put("event_id","event.approval.002")
            .put("character_id","a").put("expected_revision",99)
            .put("effects",new JSONArray().put(new JSONObject().put("kind","item")
                .put("item_id","dagger").put("name","Adaga").put("delta",1)));
        assertEquals("blocked",UniversalRpgEngine.process(s,stale).getString("status"));
        JSONObject unknown=op("apply_effects").put("event_id","event.approval.003")
            .put("character_id","a").put("expected_revision",0)
            .put("effects",new JSONArray().put(new JSONObject().put("kind","inject")));
        assertEquals("blocked",UniversalRpgEngine.process(s,unknown).getString("status"));
        assertEquals(10,s.getUniversalRpgState().optInt("elapsed_minutes"));
    }
    @Test public void aTestCanHaveExactlyOnePlayerConfirmedEffectBundle() throws Exception {
        AdventureState state=new AdventureState("bridge");register(state,"p","dnd5e");
        JSONObject check=begin(state,"p","dnd5e",new JSONObject().put("dc",12))
            .getJSONObject("pending");
        run(state,op("resolve_check").put("request_id",check.getString("request_id"))
            .put("mode","manual").put("outcome","success"),"resolved");
        JSONObject applied=run(state,op("apply_effects").put("event_id","event.bridge.01")
            .put("character_id","p").put("expected_revision",0)
            .put("request_id",check.getString("request_id"))
            .put("effects",new JSONArray().put(new JSONObject().put("kind","world_fact")
                .put("fact_id","bridge.safe").put("text","Ponte atravessada."))),"effects_applied");
        assertEquals(check.getString("request_id"),applied.getJSONObject("receipt").getString("request_id"));
        assertTrue(state.getUniversalRpgState().getJSONArray("history").getJSONObject(0).getBoolean("effects_applied"));
        assertEquals("blocked",UniversalRpgEngine.process(state,op("apply_effects")
            .put("event_id","event.bridge.02").put("character_id","p")
            .put("expected_revision",0).put("request_id",check.getString("request_id"))
            .put("effects",new JSONArray().put(new JSONObject().put("kind","time").put("minutes",5)))).getString("status"));
    }

    @Test public void tomeRejectsFractionalOrUnknownResourceValues() throws Exception {
        AdventureState s=new AdventureState("new");
        JSONObject fractional=UniversalRpgEngine.process(s,op("register").put("character_id","invalid")
            .put("system_id","dnd5e").put("name","Teste")
            .put("resources",new JSONObject().put("hp",new JSONObject()
                .put("current",2.5).put("maximum",10))));
        assertEquals("blocked",fractional.getString("status"));
        assertEquals(0,s.getUniversalRpgState().optJSONObject("entities")==null?0
            :s.getUniversalRpgState().optJSONObject("entities").length());
        JSONObject valid=UniversalRpgEngine.process(s,op("register").put("character_id","valid")
            .put("system_id","dnd5e").put("name","Aventureiro")
            .put("resources",new JSONObject().put("hp",new JSONObject()
                .put("current",4).put("maximum",10))));
        assertEquals("registered",valid.getString("status"));
    }

}
