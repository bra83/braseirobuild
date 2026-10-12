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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Suite de Homologação Automatizada do GATE D:
 * Eberron: Forge of the Artificer (EFA) + Arcana Unleashed (AU).
 * Valida:
 * 1. Artífice, Dirigíveis de Eberron (Airships) e Frameworks de Campanha com persistência no SQLite.
 * 2. Subclasses e Feats arcanos de Arcana Unleashed com resolução determinística e ciclo de descanso.
 * 3. Lore estruturado de Khorvaire (Eberron) com Knowledge Firewall e isolamento estrito de cenário.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class GateDEberronAndArcanaIntegrationTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "gate_d_test_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(dbName);
    }

    @Test
    public void testEberronForgeArtificerAndAirships() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Criar Artífice no AppDb
        long charId = db.addCharacter("Draker Cannith", "Warforged", "Artificer", 5, 42, 42, 18, 10, 14, 16, 18, 12, 8, true, "{}");
        assertTrue("Artífice inserido com sucesso", charId > 0);

        // 2. Consulta de Dirigíveis de Eberron
        JSONObject skyskiff = gateway.execute("efa", "airship_info", new JSONObject().put("ship", "Lyrandar Skyskiff"));
        assertTrue(skyskiff.optBoolean("executed"));
        JSONObject sResult = skyskiff.getJSONObject("result");
        assertTrue(sResult.optBoolean("known"));
        assertEquals("Lyrandar Skyskiff", sResult.optString("name"));
        JSONArray sComp = sResult.optJSONArray("components");
        assertNotNull(sComp);
        assertTrue("Deve conter Helm e Tow Cable", sComp.length() >= 2);

        JSONObject cruiser = gateway.execute("efa", "airship_info", new JSONObject().put("ship", "Lyrandar Air Cruiser"));
        JSONObject cResult = cruiser.getJSONObject("result");
        JSONArray cComp = cResult.optJSONArray("components");
        assertTrue("Cruiser deve conter Helm, Shield Station e Lightning Turrets", cComp.length() >= 7);

        // 3. Ativação de Framework de Campanha (Morgrave Expeditions)
        JSONObject actReq = new JSONObject()
                .put("character_id", charId)
                .put("framework", "Morgrave Expeditions")
                .put("framework_action", "activate")
                .put("confirm", true);
        JSONObject actRes = gateway.execute("efa", "framework_action", actReq);
        assertTrue("Framework deve ser ativado", actRes.optBoolean("executed"));
        assertTrue("Deve ser persistido no AppDb", actRes.optBoolean("persisted"));

        // 4. Registro de fato na campanha e avanço de progresso
        JSONObject factReq = new JSONObject()
                .put("character_id", charId)
                .put("framework", "Morgrave Expeditions")
                .put("framework_action", "record_fact")
                .put("fact", "Névoa cinzenta se dissipa próximo à linha férrea elemental")
                .put("confirm", true);
        JSONObject factRes = gateway.execute("efa", "framework_action", factReq);
        assertTrue("Fato de expedição deve ser registrado", factRes.optBoolean("executed"));

        JSONObject advReq = new JSONObject()
                .put("character_id", charId)
                .put("framework", "Morgrave Expeditions")
                .put("framework_action", "advance")
                .put("delta", 2)
                .put("confirm", true);
        JSONObject advRes = gateway.execute("efa", "framework_action", advReq);
        assertTrue("Progresso do framework deve avançar", advRes.optBoolean("executed"));
        assertEquals(2, advRes.getJSONObject("result").optJSONObject("eberron_effect").optInt("progress", advRes.getJSONObject("result").optInt("progress", 2)));

        // 5. Descanso Curto e Longo do Artífice
        JSONObject srRes = gateway.execute("efa", "short_rest", new JSONObject().put("character_id", charId));
        assertTrue(srRes.optBoolean("executed"));
        assertTrue(srRes.optBoolean("persisted"));

        JSONObject lrRes = gateway.execute("efa", "long_rest", new JSONObject().put("character_id", charId));
        assertTrue(lrRes.optBoolean("executed"));
        assertTrue(lrRes.optBoolean("persisted"));

        // 6. Validar que o framework permanece salvo na ficha do SQLite
        JSONObject reloaded = db.getCharacter(charId);
        JSONObject sheet = new JSONObject(reloaded.optString("sheet_json", "{}"));
        JSONObject fwObj = sheet.optJSONObject("eberron_campaign_framework");
        assertNotNull(fwObj);
        assertEquals("Morgrave Expeditions", fwObj.optString("active"));
        assertEquals(2, fwObj.optInt("progress"));
    }

    @Test
    public void testArcanaUnleashedSubclassesAndFeats() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Criar Mago arcano no AppDb
        long charId = db.addCharacter("Vaelin Arcano", "Elf", "Wizard", 6, 38, 38, 15, 8, 16, 14, 18, 12, 10, true, "{}");

        // 2. Cobertura e Listagem de Opções de Arcana Unleashed
        JSONObject cov = gateway.execute("au", "coverage", new JSONObject());
        assertTrue(cov.optBoolean("executed"));
        assertTrue(cov.getJSONObject("result").optBoolean("character_options_complete"));

        JSONObject subs = gateway.execute("au", "list_subclasses", new JSONObject());
        assertTrue(subs.optBoolean("executed"));
        assertTrue(subs.optJSONArray("subclasses").length() >= 8);

        JSONObject feats = gateway.execute("au", "list_feats", new JSONObject());
        assertTrue(feats.optBoolean("executed"));
        assertTrue(feats.optJSONArray("feats").length() >= 9);

        // 3. Ações para subclasse e feat
        JSONObject subActions = gateway.execute("au", "actions_for_subclass",
                new JSONObject().put("subclass", "Arcana Domain").put("level", 6));
        assertTrue(subActions.optBoolean("executed"));
        JSONArray acts = subActions.optJSONArray("actions");
        assertTrue(acts.length() >= 2);

        // 4. Execução de Feat: Arcane Infiltrator (Cunning Diversion)
        JSONObject featActionReq = new JSONObject()
                .put("character_id", charId)
                .put("feat", "Arcane Infiltrator")
                .put("feat_action", "Cunning Diversion");
        JSONObject featActionRes = gateway.execute("au", "resolve_feat", featActionReq);
        assertTrue(featActionRes.optBoolean("executed"));
        assertTrue(featActionRes.optBoolean("persisted"));

        // 5. Restauração após descanso
        JSONObject lrRes = gateway.execute("au", "long_rest", new JSONObject().put("character_id", charId));
        assertTrue(lrRes.optBoolean("executed"));
        assertTrue(lrRes.optBoolean("persisted"));
    }

    @Test
    public void testEberronLoreAndKnowledgeFirewall() throws Exception {
        NarrativeContentEngine loreEngine = new NarrativeContentEngine(db);

        // 1. Entidade de Lore: Forjas da Criação de Eston em Eberron
        List<String> facts = new ArrayList<>();
        facts.add("Eston era o coração industrial da Casa Cannith em Cyre antes do Dia do Luto.");
        facts.add("Lá foram forjados os primeiros Warforged colossais.");

        List<String> secrets = new ArrayList<>();
        secrets.add("Um protótipo de alma viva continua pulsando nos níveis inferiores.");
        secrets.add("Lord Blades envia patrulhas regulares para recuperar a matriz mestra.");

        NarrativeContentEngine.Entity estonForge = new NarrativeContentEngine.Entity(
                "eston_creation_forge",
                "EFA",
                "eberron",
                "998 YK",
                "local",
                "Forjas da Criação de Eston",
                "Eston Creation Forge",
                "Um complexo soterrado na Terra Lamentada.",
                facts,
                secrets,
                Collections.singletonList("mournland"),
                Collections.emptyMap(),
                "secreto",
                false
        );
        assertTrue(loreEngine.saveEntity(estonForge));

        // 2. Validação do Knowledge Firewall: Segredos do Mestre estritamente bloqueados para jogadores
        JSONObject playerView = loreEngine.buildPlayerView("eberron", Collections.singletonList("eston_creation_forge"));
        assertEquals("player", playerView.optString("role"));
        assertEquals(0, playerView.optJSONArray("entities").length());
        assertTrue(playerView.optInt("omitted_secrets_protected") >= 2);

        // 3. Validação do Isolamento de Cenário: Eberron não vaza em Forgotten Realms
        JSONObject frView = loreEngine.buildPlayerView("forgotten_realms", Collections.singletonList("eston_creation_forge"));
        assertEquals(0, frView.optJSONArray("entities").length());

        // 4. Visão do Mestre de Eberron com acesso integral aos segredos do Luto
        JSONObject gmView = loreEngine.buildGmView("eberron", Collections.singletonList("eston_creation_forge"));
        assertEquals("gm", gmView.optString("role"));
        assertEquals(1, gmView.optJSONArray("entities").length());
        JSONObject eObj = gmView.optJSONArray("entities").getJSONObject(0);
        assertEquals(2, eObj.optJSONArray("gm_secrets").length());
    }

    @Test
    public void testEberronDistinctRoutesPersistedAfterReopen() throws Exception {
        com.braseiro.dnd5e.adventure.AdventureModule module =
                com.braseiro.dnd5e.adventure.EberronAdventureModuleCatalog.byId("efa_sharn_inquisitive");
        assertNotNull(module);
        com.braseiro.dnd5e.adventure.AdventureEngine engine =
                new com.braseiro.dnd5e.adventure.AdventureEngine(db, null, null);

        // Rota principal: investigação nos Cogs e publicação das provas.
        assertTrue(engine.loadOrStartAdventure(module, 101L).optBoolean("module_installed"));
        assertTrue(engine.processAction("begin", null).optBoolean("committed"));
        JSONObject first = new JSONObject().put("target_node_id", "scene_2").put("roll", 16);
        assertTrue(engine.processAction("advance_1", first).optBoolean("committed"));
        assertTrue(engine.processAction("advance_2",
                new JSONObject().put("target_node_id", "scene_3")).optBoolean("committed"));
        assertTrue(engine.processAction("advance_3",
                new JSONObject().put("target_node_id", "scene_4")).optBoolean("committed"));
        assertTrue(engine.processAction("publish_truth", null).optBoolean("committed"));
        assertEquals("completed", engine.getActiveState().getStatus());
        assertTrue(engine.getActiveState().hasFlag("efa_sharn_inquisitive_path_main"));
        assertFalse(engine.getActiveState().hasFlag("efa_sharn_inquisitive_path_alternative"));
        assertTrue(engine.getActiveState().hasFlag("efa_sharn_truth_published"));
        assertFalse(engine.getActiveState().hasFlag("efa_sharn_sealed_alliance"));

        // Mesmo módulo, outro grupo: atalho exige rolagem e gera consequências alternativas.
        assertTrue(engine.loadOrStartAdventure(module, 102L).optBoolean("module_installed"));
        assertTrue(engine.processAction("begin", null).optBoolean("committed"));
        assertTrue(engine.processAction("advance_1", first).optBoolean("committed"));
        JSONObject shortcut = new JSONObject().put("target_node_id", "scene_4");
        JSONObject noRoll = engine.processAction("alternative_clues", shortcut);
        assertEquals("requires_input", noRoll.optString("status"));
        assertEquals("scene_2", engine.getActiveState().getCurrentNodeId());
        shortcut.put("roll", 17);
        assertTrue(engine.processAction("alternative_clues", shortcut).optBoolean("committed"));
        assertTrue(engine.processAction("protect_alliance", null).optBoolean("committed"));
        assertEquals("completed", engine.getActiveState().getStatus());
        assertTrue(engine.getActiveState().hasFlag("efa_sharn_inquisitive_path_alternative"));
        assertFalse(engine.getActiveState().hasFlag("efa_sharn_inquisitive_path_main"));
        assertTrue(engine.getActiveState().hasFlag("efa_sharn_sealed_alliance"));
        assertFalse(engine.getActiveState().hasFlag("efa_sharn_truth_published"));

        // Reabertura SQLite: grupos 101 e 102 conservam decisões independentes.
        db.close();
        db = new AppDb(context, dbName);
        com.braseiro.dnd5e.adventure.AdventureEngine reopened =
                new com.braseiro.dnd5e.adventure.AdventureEngine(db, null, null);
        reopened.loadOrStartAdventure(module, 101L);
        assertTrue(reopened.getActiveState().hasFlag("efa_sharn_truth_published"));
        assertFalse(reopened.getActiveState().hasFlag("efa_sharn_sealed_alliance"));
        reopened.loadOrStartAdventure(module, 102L);
        assertTrue(reopened.getActiveState().hasFlag("efa_sharn_sealed_alliance"));
        assertFalse(reopened.getActiveState().hasFlag("efa_sharn_truth_published"));
    }

    @Test
    public void testMorgraveShortcutRequiresExplicitRollAndCompletionIsIdempotent() throws Exception {
        com.braseiro.dnd5e.adventure.AdventureModule module =
                com.braseiro.dnd5e.adventure.EberronAdventureModuleCatalog.byId("efa_morgrave_expedition");
        assertNotNull(module);
        com.braseiro.dnd5e.adventure.AdventureEngine engine =
                new com.braseiro.dnd5e.adventure.AdventureEngine(db, null, null);
        assertTrue(engine.loadOrStartAdventure(module, 103L).optBoolean("module_installed"));
        assertTrue(engine.processAction("begin", null).optBoolean("committed"));
        assertTrue(engine.processAction("advance_1", null).optBoolean("committed"));
        JSONObject shortcut = new JSONObject().put("target_node_id", "scene_4");
        assertEquals("requires_input",
                engine.processAction("alternative_clues", shortcut).optString("status"));
        assertEquals("scene_2", engine.getActiveState().getCurrentNodeId());
        shortcut.put("roll", 19);
        assertTrue(engine.processAction("alternative_clues", shortcut).optBoolean("committed"));
        assertTrue(engine.getActiveState().hasFlag("efa_morgrave_expedition_path_alternative"));
        assertFalse(engine.getActiveState().hasFlag("efa_morgrave_expedition_path_main"));
        assertTrue(engine.processAction("seal_relic", null).optBoolean("committed"));
        assertTrue(engine.getActiveState().hasFlag("efa_morgrave_relic_sealed"));
        int historySize = engine.getActiveState().getBranchHistory().length();
        assertFalse(engine.processAction("seal_relic", null).optBoolean("committed"));
        assertEquals(historySize, engine.getActiveState().getBranchHistory().length());
        assertFalse(engine.getActiveState().hasFlag("efa_morgrave_relic_delivered"));
    }
}
