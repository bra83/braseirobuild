package com.braseiro.dnd5e;

import android.content.Context;
import com.braseiro.dnd5e.adventure.AdventureEngine;
import com.braseiro.dnd5e.adventure.AdventureModule;
import com.braseiro.dnd5e.adventure.AdventureNode;
import com.braseiro.dnd5e.adventure.AdventureTransition;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Suite de Homologação Automatizada do GATE A: Fundação & Infraestrutura de Conteúdo.
 * Valida:
 * 1. Ciclo de Entidades de Lore & Knowledge Firewall (Isolamento por cenário, proteção estrita de segredos de GM).
 * 2. OfficialBookRuntimeGateway unificado para os 10 livros oficiais (Fail-closed, contratos canônicos e persistência).
 * 3. AdventureEngine (Instalação declarativa, navegação não linear e persistência transacional de grafo em SQLite).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class GateAFoundationIntegrationTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "gate_a_test_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
        if (context != null) {
            context.deleteDatabase(dbName);
        }
    }

    @Test
    public void testPillar1_LoreEntityAndKnowledgeFirewall() throws Exception {
        NarrativeContentEngine engine = new NarrativeContentEngine(db);

        List<String> facts = new ArrayList<>();
        facts.add("Candlekeep guarda a maior coleção de tomos das Terras Centrais.");
        facts.add("A entrada requer a doação de um tomo raro não catalogado.");

        List<String> secrets = new ArrayList<>();
        secrets.add("Sob as catacumbas existe um Mythal corrompido de Netheril.");
        secrets.add("O primeiro escriba é um doppelganger a serviço dos Zhentarim.");

        NarrativeContentEngine.Entity secretVault = new NarrativeContentEngine.Entity(
                "candlekeep_secret_vault",
                "FRHoF",
                "forgotten_realms",
                "1492 DR",
                "local",
                "Câmara Oculta de Candlekeep",
                "Candlekeep Secret Vault",
                "Uma câmara subterrânea esquecida protegida por glifos ancestrais.",
                facts,
                secrets,
                Collections.singletonList("candlekeep"),
                Collections.singletonMap("ligado_a", "netheril"),
                "secreto",
                false
        );

        // 1. Salvar entidade
        boolean saved = engine.saveEntity(secretVault);
        assertTrue("Entidade deve ser salva com sucesso no SQLite", saved);

        // 2. Consulta direta
        NarrativeContentEngine.Entity retrieved = engine.getEntity("candlekeep_secret_vault");
        assertNotNull(retrieved);
        assertEquals("candlekeep_secret_vault", retrieved.id);
        assertEquals(2, retrieved.verifiedFacts.size());
        assertEquals(2, retrieved.gmSecrets.size());
        assertFalse(retrieved.discovered);

        // 3. Visão do Jogador (Entidade secreta ainda não descoberta: deve ser estritamente omitida)
        List<String> ids = Collections.singletonList("candlekeep_secret_vault");
        JSONObject playerView = engine.buildPlayerView("forgotten_realms", ids);
        assertEquals("player", playerView.optString("role"));
        assertEquals("gm_secrets_firewalled", playerView.optString("security_status"));
        assertEquals(0, playerView.optJSONArray("entities").length());
        assertTrue("Segredos de GM devem ser contabilizados como protegidos",
                playerView.optInt("omitted_secrets_protected", 0) > 0);

        // 4. Visão do Mestre (Deve conter acesso integral aos segredos)
        JSONObject gmView = engine.buildGmView("forgotten_realms", ids);
        assertEquals("gm", gmView.optString("role"));
        assertEquals("full_gm_access", gmView.optString("security_status"));
        assertEquals(1, gmView.optJSONArray("entities").length());
        JSONObject gmEntity = gmView.optJSONArray("entities").getJSONObject(0);
        assertEquals(2, gmEntity.optJSONArray("gm_secrets").length());

        // 5. Descoberta in-game
        boolean discovered = engine.discoverEntity("candlekeep_secret_vault", "Descoberta pelo Ladino");
        assertTrue("Descoberta deve ser registrada", discovered);

        // 6. Visão do Jogador após descoberta: entidade revelada, fatos visíveis, mas GM SECRETS NUNCA VAZAM
        JSONObject playerViewAfter = engine.buildPlayerView("forgotten_realms", ids);
        assertEquals(1, playerViewAfter.optJSONArray("entities").length());
        JSONObject revealed = playerViewAfter.optJSONArray("entities").getJSONObject(0);
        assertEquals("Câmara Oculta de Candlekeep", revealed.optString("name"));
        assertEquals(2, revealed.optJSONArray("verified_facts").length());
        assertFalse("gm_secrets NUNCA devem estar presentes no objeto do jogador", revealed.has("gm_secrets"));

        // 7. Sincronização com knowledge_facts do AppDb
        List<JSONObject> kFacts = db.listKnowledgeFacts(50);
        boolean foundInKnowledge = false;
        for (JSONObject kf : kFacts) {
            String sub = kf.optString("subject", kf.optString("topic"));
            if ("Câmara Oculta de Candlekeep".equals(sub)) {
                foundInKnowledge = true;
                assertEquals("verdade", kf.optString("truth_status", kf.optString("status")));
                assertEquals("grupo", kf.optString("visibility"));
                break;
            }
        }
        assertTrue("Fato descoberto deve ser sincronizado com knowledge_facts", foundInKnowledge);

        // 8. Isolamento de cenário (Entidade de Eberron não vaza em cenário Faerûn)
        NarrativeContentEngine.Entity sharnTower = new NarrativeContentEngine.Entity(
                "sharn_lightning_spire",
                "EFA",
                "eberron",
                "998 YK",
                "local",
                "Pináculo do Relâmpago",
                "Lightning Spire",
                "Uma torre de atracamento em Sharn.",
                Collections.singletonList("Pertence à Casa Cannith"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                "grupo",
                true
        );
        engine.saveEntity(sharnTower);
        JSONObject crossSettingView = engine.buildPlayerView("forgotten_realms", Collections.singletonList("sharn_lightning_spire"));
        assertEquals("Entidade de Eberron não pode aparecer em Forgotten Realms", 0, crossSettingView.optJSONArray("entities").length());
    }

    @Test
    public void testPillar2_OfficialBookRuntimeGatewayTenBooksAndLore() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Fail-closed com livro não cadastrado
        JSONObject unknown = gateway.execute("unregistered_book_xyz", "action", new JSONObject());
        assertEquals("blocked", unknown.optString("status"));
        assertEquals("official_book_not_registered", unknown.optString("reason"));

        // 2. Livro 1: FRHoF
        JSONObject frhofDesc = gateway.execute("frhof", "descriptor", new JSONObject());
        assertTrue("FRHoF descriptor deve executar", frhofDesc.optBoolean("executed"));
        JSONObject aurora = gateway.execute("frhof", "aurora_info", new JSONObject().put("item", "Desert Clothing"));
        assertTrue("Aurora info deve executar", aurora.optBoolean("executed"));

        // 3. Livro 2: EFA
        JSONObject efaCov = gateway.execute("efa", "coverage", new JSONObject());
        assertTrue("EFA coverage deve executar", efaCov.optBoolean("executed"));
        JSONObject airship = gateway.execute("efa", "airship_info", new JSONObject().put("ship", "Lyrandar Skyskiff"));
        assertTrue("Airship info deve executar", airship.optBoolean("executed"));

        // 4. Livro 3: AU
        JSONObject auCov = gateway.execute("au", "coverage", new JSONObject());
        assertTrue("AU coverage deve executar", auCov.optBoolean("executed"));

        // 5. Livro 4: XMM
        JSONObject xmmCov = gateway.execute("xmm", "coverage", new JSONObject());
        assertTrue("XMM coverage deve executar", xmmCov.optBoolean("executed"));

        // 6. Livro 5: XSAC
        JSONObject xsacCov = gateway.execute("xsac", "coverage", new JSONObject());
        assertTrue("XSAC coverage deve executar", xsacCov.optBoolean("executed"));

        // 7. Livro 6: FRAiF
        JSONObject fraifDest = gateway.execute("fraif", "list_example_destinies", new JSONObject());
        assertTrue("FRAiF destinies deve executar", fraifDest.optBoolean("executed"));

        // 8. Livro 7: RHW
        JSONObject rhwDesc = gateway.execute("rhw", "descriptor", new JSONObject());
        assertTrue("RHW descriptor deve executar", rhwDesc.optBoolean("executed"));

        // 9. Livro 8: ABH
        JSONObject abhFeats = gateway.execute("abh", "list_feats", new JSONObject());
        assertTrue("ABH list_feats deve executar", abhFeats.optBoolean("executed"));

        // 10. Livro 9: LFL
        JSONObject lflDesc = gateway.execute("lfl", "descriptor", new JSONObject());
        assertTrue("LFL descriptor deve executar", lflDesc.optBoolean("executed"));

        // 11. Livro 10: NF
        JSONObject nfCov = gateway.execute("nf", "coverage", new JSONObject());
        assertTrue("NF coverage deve executar", nfCov.optBoolean("executed"));

        // 12. Operações de Lore pelo Gateway
        JSONObject loreEntity = new JSONObject()
                .put("id", "netheril_enclave_loros")
                .put("source_id", "NF")
                .put("setting", "forgotten_realms")
                .put("era", "-339 DR")
                .put("type", "local")
                .put("name", "Enclave de Loros")
                .put("original_name", "Loros Enclave")
                .put("summary", "Um enclave flutuante antes da Queda.")
                .put("verified_facts", new JSONArray().put("Queda causada pela Loucura de Karsus"))
                .put("gm_secrets", new JSONArray().put("Um artefato sombrio continua intacto na cratera"))
                .put("tags", new JSONArray().put("netheril"))
                .put("relations", new JSONObject())
                .put("visibility", "grupo")
                .put("discovered", true);

        JSONObject saveRes = gateway.execute("lore", "lore_save", new JSONObject().put("entity", loreEntity));
        assertTrue("Lore save via gateway deve executar", saveRes.optBoolean("executed"));
        assertTrue("Lore save via gateway deve persistir", saveRes.optBoolean("persisted"));

        JSONObject getRes = gateway.execute("lore", "lore_get", new JSONObject().put("id", "netheril_enclave_loros"));
        assertTrue("Lore get via gateway deve executar", getRes.optBoolean("executed"));
        assertTrue("Lore get deve encontrar a entidade", getRes.optBoolean("found"));
        assertEquals("Enclave de Loros", getRes.getJSONObject("entity").optString("name"));
    }

    @Test
    public void testPillar3_AdventureEnginePersistenceAndGraphTraversal() throws Exception {
        AdventureEngine engine = new AdventureEngine(db, null, null);

        // 1. Criar grafo de aventura não-linear
        AdventureTransition t1 = new AdventureTransition("t1", "node_armory", "Avançar para o Arsenal", "", "", false, null, null);
        List<AdventureTransition> rootTransitions = Collections.singletonList(t1);

        AdventureNode rootNode = new AdventureNode(
                "node_entrance",
                "Entrada da Cripta",
                "Uma antecâmara de pedra fria com duas passagens.",
                "Descrever o ar gelado e ecos distantes.",
                "crypt_dungeon",
                Collections.<String>emptyList(),
                rootTransitions,
                null,
                null
        );

        AdventureNode armoryNode = new AdventureNode(
                "node_armory",
                "Arsenal Esquecido",
                "Armas antigas enferrujadas cobrem as paredes.",
                "Se procurarem, encontram uma espada curta gravada com runas.",
                "crypt_armory",
                Collections.<String>emptyList(),
                Collections.<AdventureTransition>emptyList(),
                null,
                null
        );

        Map<String, AdventureNode> nodes = new HashMap<>();
        nodes.put(rootNode.getId(), rootNode);
        nodes.put(armoryNode.getId(), armoryNode);

        AdventureModule module = new AdventureModule(
                "mod_gate_a_crypt",
                "O Mistério da Cripta Esquecida",
                "1.0.0",
                "node_entrance",
                nodes,
                new JSONObject().put("author", "Braseiro D&D").put("tier", 1)
        );

        // 2. Instalar e iniciar aventura para o grupo 42L
        JSONObject session = engine.loadOrStartAdventure(module, 42L);
        assertNotNull(session);
        String currentNode = session.optJSONObject("current_node") != null
                ? session.optJSONObject("current_node").optString("id")
                : session.optJSONObject("state").optString("current_node_id");
        assertEquals("node_entrance", currentNode);

        // 3. Processar avanço de nó via ação do jogador
        JSONObject actionParams = new JSONObject()
                .put("target_node_id", "node_armory")
                .put("input", "Avançar para o Arsenal");
        JSONObject advanceResult = engine.processAction("t1", actionParams);
        assertTrue("Ação de avanço deve ser bem-sucedida", advanceResult.optBoolean("committed", false));
        assertEquals("node_armory", advanceResult.optString("current_node_id"));

        // 4. Reinicialização de runtime (Simula fechar o app e reabrir)
        AdventureEngine reloadedEngine = new AdventureEngine(db, null, null);
        JSONObject resumed = reloadedEngine.loadOrStartAdventure(module, 42L);

        // 5. Validar que o nó ativo retomado é exatamente node_armory do SQLite
        String resumedNode = resumed.optJSONObject("current_node") != null
                ? resumed.optJSONObject("current_node").optString("id")
                : resumed.optJSONObject("state").optString("current_node_id");
        assertEquals("Sessão retomada deve manter o nó após persistência real",
                "node_armory", resumedNode);
    }
}
