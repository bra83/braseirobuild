package com.braseiro.dnd5e;

import android.content.Context;
import com.braseiro.dnd5e.adventure.AdventureEngine;
import com.braseiro.dnd5e.adventure.AdventureModule;
import com.braseiro.dnd5e.adventure.FaerunAdventureModuleCatalog;
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
 * Simulação de Sessão Real de Jogo Comprovando:
 * 1. Inicialização de campanha e conhecimento contextual do Mestre Gemini.
 * 2. Proteção estrita contra spoilers (Knowledge Firewall).
 * 3. Descoberta dinâmica de segredos pelo jogador durante a partida.
 * 4. Navegação por aventura não linear de FRAiF com exigência de teste manual.
 * 5. Persistência de descobertas e escolhas sobrevivendo a reabertura do app e seedOfficialCanon().
 * 6. Isolamento estrito entre cenários (Faerûn vs Eberron).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class SimulatedGameSessionNarrativeTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "sim_session_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(dbName);
    }

    @Test
    public void testFullGameSessionFlowWithNarrativeAndAdventure() throws Exception {
        // 1. Inicialização do Mestre Orquestrador e Motores
        PartyDirector party = new PartyDirector(db);
        WorldStateEngine world = new WorldStateEngine(db);
        AiQuotaGovernor quota = new AiQuotaGovernor(context);
        MasterOrchestrator orchestrator = new MasterOrchestrator(db, party, world, quota, context);

        NarrativeContentEngine narrative = orchestrator.getNarrativeEngine();
        AdventureEngine adventure = orchestrator.getAdventureEngine();

        assertNotNull("Narrative engine deve estar ativo", narrative);
        assertNotNull("Adventure engine deve estar ativo", adventure);

        // 2. Mestre Gemini monta o contexto para Faerûn (Player View)
        JSONObject faerunPlayerView = narrative.promptKnowledgeContext("forgotten_realms", false);
        assertNotNull("Player View não pode ser nulo", faerunPlayerView);
        assertEquals("forgotten_realms", faerunPlayerView.optString("setting"));

        String playerViewStr = faerunPlayerView.toString();
        // Conhecimento público local deve estar presente
        assertTrue("Mestre deve conhecer Harpistas em Faerûn", playerViewStr.contains("faerun_harpers") || playerViewStr.contains("Harpistas"));

        // 3. Proteção contra spoilers: Segredos NÃO descobertos não vazam para a visão do jogador
        NarrativeContentEngine.Entity crownEntity = narrative.getEntity("faerun_item_crown_of_horns");
        assertNotNull("Entidade da Coroa de Chifres deve existir", crownEntity);
        assertFalse("Coroa de Chifres começa como segredo não descoberto", crownEntity.discovered);
        assertFalse("Segredo oculto não pode aparecer na visão do jogador antes da descoberta",
                playerViewStr.contains("Crown of Horns") && playerViewStr.contains("avatar de Myrkul"));

        // 4. Jogador investiga e descobre a pista da Coroa de Chifres
        boolean discovered = narrative.discoverEntity("faerun_item_crown_of_horns");
        assertTrue("Descoberta deve retornar sucesso", discovered);

        NarrativeContentEngine.Entity discoveredCrown = narrative.getEntity("faerun_item_crown_of_horns");
        assertTrue("Agora a entidade está formalmente descoberta", discoveredCrown.discovered);

        // 5. Início da Aventura de FRAiF: "The Tomb of Damara"
        AdventureModule damaraMod = FaerunAdventureModuleCatalog.byId("fraif_tomb_damara");
        assertNotNull("Módulo de Damara deve existir", damaraMod);

        JSONObject startRes = adventure.loadOrStartAdventure(damaraMod, 1L);
        assertNotNull(startRes);
        assertTrue("Aventura deve iniciar com sucesso", startRes.optBoolean("module_installed", false));

        // Processar ação para avançar (sem auto-roll)
        JSONObject transRes = adventure.processAction("begin", null);
        assertNotNull(transRes);

        // 6. Teste de Persistência: Fechar e Reabrir o Banco de Dados
        db.close();
        AppDb reopenedDb = new AppDb(context, dbName);
        PartyDirector newParty = new PartyDirector(reopenedDb);
        WorldStateEngine newWorld = new WorldStateEngine(reopenedDb);
        AiQuotaGovernor newQuota = new AiQuotaGovernor(context);
        MasterOrchestrator newOrchestrator = new MasterOrchestrator(reopenedDb, newParty, newWorld, newQuota, context);
        NarrativeContentEngine reopenedNarrative = newOrchestrator.getNarrativeEngine();

        // Garantir que seedOfficialCanon() não sobrescreveu o estado vivo
        NarrativeContentEngine.Entity persistedCrown = reopenedNarrative.getEntity("faerun_item_crown_of_horns");
        assertNotNull(persistedCrown);
        assertTrue("A descoberta da Coroa de Chifres DEVE sobreviver a reinicializações", persistedCrown.discovered);

        // 7. Isolamento de Cenários: Visão de Eberron
        JSONObject eberronContext = reopenedNarrative.promptKnowledgeContext("eberron", true);
        String eberronStr = eberronContext.toString();

        assertEquals("eberron", eberronContext.optString("setting"));
        assertTrue("Eberron deve conter Sharn ou Forja", eberronStr.contains("eberron_sharn_city") || eberronStr.contains("Sharn"));
        assertFalse("Eberron NUNCA deve conter Harpistas de Faerûn", eberronStr.contains("faerun_harpers"));
        assertFalse("Eberron NUNCA deve conter Strahd de Ravenloft", eberronStr.contains("ravenloft_strahd"));

        reopenedDb.close();
    }

    @Test
    public void testMasterActionStateJourneyWithMockedAiAndIdempotentPersistence() throws Exception {
        PartyDirector party = new PartyDirector(db);
        WorldStateEngine world = new WorldStateEngine(db);
        AiQuotaGovernor quota = new AiQuotaGovernor(context);
        MasterOrchestrator orchestrator = new MasterOrchestrator(db, party, world, quota, context);
        NarrativeContentEngine narrative = orchestrator.getNarrativeEngine();
        AdventureEngine adventure = orchestrator.getAdventureEngine();

        // 1. Proposta da IA Mockada (MOCK_OK) e Validação de Firewall Mecânico
        JSONObject aiProposal = new JSONObject()
                .put("narration", "O vento gélido uiva nas ameias do castelo arruinado de Damara. Tochas tremeluzem no pátio.")
                .put("lore_context", "MOCK_OK")
                .put("mock_status", "MOCK_OK")
                .put("hp", 999)
                .put("gold", 5000);

        JSONObject gated = orchestrator.gateGeminiGameplayProposal(aiProposal);
        assertNotNull(gated);
        assertTrue("Mutações mecânicas não autorizadas da IA devem ser bloqueadas", gated.optBoolean("mechanical_mutation_blocked"));
        assertFalse("IA não pode mutar estado diretamente", gated.optBoolean("state_mutation_allowed"));
        assertTrue(gated.optJSONArray("blocked_mutations").toString().contains("hp"));
        assertTrue(gated.optJSONArray("blocked_mutations").toString().contains("gold"));
        assertEquals("MOCK_OK", gated.optJSONObject("allowed_narrative").optString("lore_context"));
        assertTrue(gated.optJSONObject("allowed_narrative").has("narration"));

        // 2. Proteção Estrita de Segredos antes da Descoberta
        JSONObject publicPrompt = narrative.promptKnowledgeContext("forgotten_realms", false);
        String publicPromptStr = publicPrompt.toString();
        assertFalse("Segredo de Myth Drannor não pode aparecer sem descoberta",
                publicPromptStr.contains("faerun_cormanthor_myth_drannor") && publicPromptStr.contains("Baalpheros"));

        // 3. Descoberta Canônica do Segredo
        boolean discovered = narrative.discoverEntity("faerun_cormanthor_myth_drannor");
        assertTrue("Descoberta deve ter sucesso", discovered);

        JSONObject playerContextAfter = narrative.promptKnowledgeContext("forgotten_realms", false);
        assertTrue("Após descoberta, o lore deve estar visível", playerContextAfter.toString().contains("faerun_cormanthor_myth_drannor"));

        // 4. Execução da Aventura com Escolhas Ramificadas e Resolução Explícita de Testes
        AdventureModule damaraMod = FaerunAdventureModuleCatalog.byId("fraif_tomb_damara");
        assertNotNull(damaraMod);

        JSONObject startRes = adventure.loadOrStartAdventure(damaraMod, 100L);
        assertTrue(startRes.optBoolean("module_installed"));
        assertEquals("setup", startRes.optString("current_node_id"));

        // Avançar: setup -> scene_1
        JSONObject pBegin = adventure.processAction("begin", new JSONObject().put("target_node_id", "scene_1"));
        assertTrue(pBegin.optBoolean("committed"));
        assertEquals("scene_1", pBegin.optString("current_node_id"));

        // Cena 1: Assalto Direto (scene_1 -> scene_2)
        JSONObject pAssault = adventure.processAction("direct_assault", new JSONObject().put("target_node_id", "scene_2"));
        assertTrue(pAssault.optBoolean("committed"));
        assertEquals("scene_2", pAssault.optString("current_node_id"));

        // Cena 2: Resgate dos Silverstar com Rolagem Explícita Manual (CD 13, roll=14, mod=2 -> total 16)
        JSONObject rollParams = new JSONObject()
                .put("target_node_id", "scene_3")
                .put("roll", 14)
                .put("modifier", 2);
        JSONObject pRescue = adventure.processAction("rescue_direct", rollParams);
        assertTrue(pRescue.optBoolean("committed"));
        assertEquals("scene_3", pRescue.optString("current_node_id"));

        // 5. Retry Idempotente: Repetir a mesma ação não deve corromper nem duplicar estado
        JSONObject pRetry = adventure.processAction("rescue_direct", rollParams);
        // O nó atual já é scene_3, transição já foi consumida
        assertEquals("scene_3", adventure.getActiveState().getCurrentNodeId());

        // 6. Fechar banco, reabrir e verificar persistência transacional completa
        db.close();

        AppDb reopenedDb = new AppDb(context, dbName);
        PartyDirector reopenedParty = new PartyDirector(reopenedDb);
        WorldStateEngine reopenedWorld = new WorldStateEngine(reopenedDb);
        AiQuotaGovernor reopenedQuota = new AiQuotaGovernor(context);
        MasterOrchestrator freshOrchestrator = new MasterOrchestrator(reopenedDb, reopenedParty, reopenedWorld, reopenedQuota, context);

        AdventureEngine freshAdvEngine = freshOrchestrator.getAdventureEngine();
        JSONObject resumeRes = freshAdvEngine.loadOrStartAdventure(damaraMod, 100L);
        assertEquals("scene_3", resumeRes.optString("current_node_id"));

        com.braseiro.dnd5e.adventure.AdventureState restoredState = freshAdvEngine.getActiveState();
        assertNotNull(restoredState);
        assertTrue("Flags devem persistir após reinício", restoredState.hasFlag("damara_ghouls_slain"));
        assertTrue("Flags de resgate devem persistir", restoredState.hasFlag("silverstars_rescued"));
        assertTrue("Chave da tumba obtida deve persistir", restoredState.hasFlag("tomb_key_obtained"));

        // Verificar que entidade descoberta persiste após re-seeding
        NarrativeContentEngine freshNarrative = freshOrchestrator.getNarrativeEngine();
        NarrativeContentEngine.Entity restoredMythDrannor = freshNarrative.getEntity("faerun_cormanthor_myth_drannor");
        assertNotNull(restoredMythDrannor);
        assertTrue("Descoberta de Myth Drannor deve persistir após reabertura", restoredMythDrannor.discovered);

        reopenedDb.close();
    }
}
