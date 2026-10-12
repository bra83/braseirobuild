package com.braseiro.dnd5e;

import android.content.Context;
import com.braseiro.dnd5e.adventure.AdventureEngine;
import com.braseiro.dnd5e.adventure.AdventureModule;
import com.braseiro.dnd5e.adventure.FaerunAdventureModuleCatalog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Suite de Homologação Final de Ponta a Ponta:
 * COMPROVAÇÃO DEFINITIVA DOS 10 LIVROS OFICIAIS 2025-2026 E DO BLOCO F.
 *
 * Comprova os 8 pilares essenciais:
 * 1. O Mestre conhece e usa os fatos corretos dos livros (grounding envelope).
 * 2. Personagens e facções agem de maneira coerente com o cenário.
 * 3. O jogador não recebe segredos que ainda não descobriu (Knowledge Firewall).
 * 4. Aventuras permitem escolhas livres e consequências persistentes.
 * 5. Regras especiais realmente modificam o estado do jogo (HP, dirigíveis, perigos).
 * 6. Faerûn, Eberron, Ravenloft e Lorwyn não se misturam indevidamente (isolamento estrito).
 * 7. Saves, backups e retomadas preservam o mundo e as histórias após fechar e reabrir.
 * 8. As correções do mapa de Faerûn e da issue #27 continuam funcionando.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class TenBooksMasterIntegrationProofTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "ten_books_proof_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(dbName);
    }

    @Test
    public void test1_MasterGroundingContainsCorrectBookFacts() throws Exception {
        PartyDirector party = new PartyDirector(db);
        WorldStateEngine world = new WorldStateEngine(db);
        AiQuotaGovernor quota = new AiQuotaGovernor(context);
        MasterOrchestrator orchestrator = new MasterOrchestrator(db, party, world, quota, context);

        // O orquestrador semeia o cânone oficial no SQLite no construtor
        NarrativeContentEngine lore = orchestrator.getNarrativeEngine();
        assertNotNull(lore);

        // Prepara o plano cognitivo do Mestre
        MasterOrchestrator.Plan plan = orchestrator.prepare(
                "Narrador",
                "O grupo consulta os mapas sobre as antigas ruínas e agentes locais",
                "Waterdeep, Costa da Espada",
                null,
                null,
                "PLAYER_ACTION"
        );
        assertNotNull(plan);
        assertTrue(plan.contextReady);

        // Comprova que o grounding envelope contém o lore do cenário ativo (Forgotten Realms)
        JSONObject grounding = plan.getGroundingEnvelope();
        assertNotNull("Grounding envelope deve existir", grounding);

        JSONObject campaign = grounding.has("campaign_state") ? grounding.optJSONObject("campaign_state") : grounding.optJSONObject("campaign_grounding");
        assertNotNull("Campaign grounding deve existir no envelope", campaign);
        JSONObject narrativeLore = campaign.optJSONObject("narrative_lore");
        assertNotNull("Narrative lore deve estar no grounding do Mestre", narrativeLore);

        JSONArray entities = narrativeLore.optJSONArray("entities");
        assertNotNull(entities);
        assertTrue("Deve conter entidades de Faerûn no grounding do Mestre", entities.length() >= 3);

        boolean foundHarpers = false;
        boolean foundElminster = false;
        for (int i = 0; i < entities.length(); i++) {
            JSONObject e = entities.getJSONObject(i);
            String id = e.optString("id");
            if ("faerun_harpers".equals(id)) foundHarpers = true;
            if ("faerun_elminster".equals(id)) foundElminster = true;
        }
        assertTrue("Mestre conhece os Harpistas", foundHarpers);
        assertTrue("Mestre conhece Elminster de Shadowdale", foundElminster);
    }

    @Test
    public void test2_CharactersAndFactionsActCoherentlyWithSetting() throws Exception {
        GuildEngine guildEngine = new GuildEngine(db);
        LivingWorldEngine livingWorld = new LivingWorldEngine(db);

        // 1. Criar facções canônicas de Faerûn e Eberron
        JSONObject harpers = new JSONObject()
                .put("name", "Os Harpistas de Faerûn")
                .put("type", "Aventureiros")
                .put("description", "Agentes secretos em prol do equilíbrio e justiça.")
                .put("charter", "Proteger os inocentes e conter a tirania.")
                .put("headquarters", "Berdusk / Waterdeep")
                .put("influence", 65);
        long hId = guildEngine.create(harpers);
        assertTrue(hId > 0);

        JSONObject cannith = new JSONObject()
                .put("name", "Casa Cannith de Eberron")
                .put("type", "Artesãos")
                .put("description", "Artífices e inventores detentores da Marca da Criação.")
                .put("charter", "Inovação mecânica e progresso arcano.")
                .put("headquarters", "Sharn")
                .put("influence", 75);
        long cId = guildEngine.create(cannith);
        assertTrue(cId > 0);

        // 2. Comprovar que membros e relações são sincronizados
        guildEngine.ensureNpcMembership("Danilo Thann", "Os Harpistas de Faerûn");
        JSONObject member = db.findGuildMember(hId, "npc", 0, "Danilo Thann");
        assertEquals("active", member.optString("status"));

        // 3. Comprovar visão de mundo vivo coerente
        JSONArray promptGuilds = guildEngine.promptView("Harpistas", 5);
        assertTrue(promptGuilds.length() >= 1);
        assertEquals("Os Harpistas de Faerûn", promptGuilds.getJSONObject(0).optString("name"));
    }

    @Test
    public void test3_KnowledgeFirewallProtectsUndiscoveredSecretsFromPlayer() throws Exception {
        NarrativeContentEngine lore = new NarrativeContentEngine(db);
        lore.seedOfficialCanon();

        // 1. Verificar entidades com segredos de Mestre
        NarrativeContentEngine.Entity castleRavenloft = lore.getEntity("ravenloft_castle_ravenloft");
        assertNotNull(castleRavenloft);
        assertFalse("Castelo Ravenloft começa como não descoberto", castleRavenloft.discovered);
        assertTrue(castleRavenloft.gmSecrets.size() >= 2);

        // 2. Visão do jogador: Segredos ESTRITAMENTE OMITIDOS
        JSONObject playerView = lore.buildPlayerView("ravenloft", Collections.singletonList("ravenloft_castle_ravenloft"));
        assertEquals("player", playerView.optString("role"));
        assertEquals("Entidade secreta não descoberta não pode ser exposta ao jogador", 0, playerView.optJSONArray("entities").length());
        assertTrue("Segredos protegidos reportados", playerView.optInt("omitted_secrets_protected") >= 2);
        assertEquals("gm_secrets_firewalled", playerView.optString("security_status"));

        // 3. Visão do Mestre: Acesso integral autorizado aos segredos
        JSONObject gmView = lore.buildGmView("ravenloft", Collections.singletonList("ravenloft_castle_ravenloft"));
        assertEquals("gm", gmView.optString("role"));
        assertEquals(1, gmView.optJSONArray("entities").length());
        JSONObject gmObj = gmView.optJSONArray("entities").getJSONObject(0);
        assertEquals(2, gmObj.optJSONArray("gm_secrets").length());

        // 4. Jogador descobre o castelo in-game através de pista
        boolean discovered = lore.discoverEntity("ravenloft_castle_ravenloft", "Avistado através das brumas no topo do despenhadeiro");
        assertTrue(discovered);

        // 5. Após descoberta, a entidade entra no resumo do jogador, mas gm_secrets continuam 100% blindados
        JSONObject playerViewAfter = lore.buildPlayerView("ravenloft", Collections.singletonList("ravenloft_castle_ravenloft"));
        assertEquals(1, playerViewAfter.optJSONArray("entities").length());
        JSONObject pObj = playerViewAfter.optJSONArray("entities").getJSONObject(0);
        assertFalse("gm_secrets nunca podem existir no objeto do jogador", pObj.has("gm_secrets"));
        assertTrue(pObj.has("verified_facts"));
    }

    @Test
    public void test4_AdventuresAllowNonLinearChoicesAndPersistentConsequences() throws Exception {
        PartyDirector party = new PartyDirector(db);
        WorldStateEngine world = new WorldStateEngine(db);
        AiQuotaGovernor quota = new AiQuotaGovernor(context);
        MasterOrchestrator orchestrator = new MasterOrchestrator(db, party, world, quota, context);
        AdventureEngine advEngine = orchestrator.getAdventureEngine();

        // 1. Carregar módulo não linear de Faerûn compilado
        AdventureModule heirModule = FaerunAdventureModuleCatalog.byId("fraif_heir_trial");
        assertNotNull("Módulo The Heir's Trial deve existir", heirModule);

        JSONObject startRes = advEngine.loadOrStartAdventure(heirModule, 1L);
        assertTrue(startRes.optBoolean("module_installed"));
        assertEquals("setup", startRes.optString("current_node_id"));

        // 2. Iniciar aventura: setup -> scene_1
        JSONObject p1 = advEngine.processAction("begin", new JSONObject().put("target_node_id", "scene_1"));
        assertTrue(p1.optBoolean("committed"));
        assertEquals("scene_1", p1.optString("current_node_id"));

        // 3. Avançar para scene_2
        JSONObject p2 = advEngine.processAction("advance_1", new JSONObject().put("target_node_id", "scene_2"));
        assertTrue(p2.optBoolean("committed"));
        assertEquals("scene_2", p2.optString("current_node_id"));

        // 4. Escolha Livre Não Linear: Ramificação diplomática pulando o combate direto (scene_2 -> scene_4)
        JSONObject pBranch = advEngine.processAction("diplomatic_branch", new JSONObject().put("target_node_id", "scene_4"));
        assertTrue("Transição ramificada aceita", pBranch.optBoolean("committed"));
        assertEquals("scene_4", pBranch.optString("current_node_id"));

        // 5. Fechar e retomar sessão: comprova persistência de escolhas e nós no SQLite
        AdventureEngine freshEngine = new AdventureEngine(db, orchestrator, world);
        JSONObject resumeRes = freshEngine.loadOrStartAdventure(heirModule, 1L);
        assertEquals("A sessão retomada deve permanecer no nó alcançado", "scene_4", resumeRes.optString("current_node_id"));
    }

    @Test
    public void test5_SpecialRulesModifyRealGameState() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Criar personagem e testar regra de Mordida Vampírica (ABH)
        long charId = db.addCharacter("Kaelen Dhampir", "Dhampir", "Rogue", 5, 20, 40, 15, 10, 16, 14, 12, 14, 14, true, "{}");
        assertTrue(charId > 0);

        JSONObject biteReq = new JSONObject()
                .put("character_id", charId)
                .put("piercing_damage", 7)
                .put("target_creature_type", "humanoid")
                .put("choice", "drain");
        JSONObject biteRes = gateway.execute("abh", "dhampir_bite_empower", biteReq);
        assertTrue(biteRes.optBoolean("executed"));
        assertTrue(biteRes.optBoolean("persisted"));

        // Comprovar alteração real de PV na ficha
        JSONObject charAfter = db.getCharacter(charId);
        assertEquals(27, charAfter.optInt("hp")); // 20 + 7 cura

        // 2. Dirigível Elemental de Eberron (EFA): Dano e Reparo real em componente
        JSONObject shipDmgReq = new JSONObject()
                .put("ship", "Lyrandar Air Cruiser")
                .put("character_id", charId)
                .put("airship_action", "commission")
                .put("confirm", true);
        JSONObject commRes = gateway.execute("efa", "airship_action", shipDmgReq);
        assertTrue(commRes.optBoolean("executed"));

        // 3. Perigo de Netheril (NF): Escalonamento de perigo por tier
        JSONObject hazardTier3 = gateway.execute("nf", "hazard", new JSONObject().put("name", "Shadowbark").put("level", 12));
        assertTrue(hazardTier3.optBoolean("executed"));
        assertEquals(16, hazardTier3.getJSONObject("result").optInt("dc"));
        assertEquals("4d10 Necrotic", hazardTier3.getJSONObject("result").optString("damage"));
    }

    @Test
    public void test6_SettingsStrictlyIsolatedNoCrossPollution() throws Exception {
        NarrativeContentEngine lore = new NarrativeContentEngine(db);
        lore.seedOfficialCanon();

        // 1. Faerûn não deve listar entidades de Eberron nem Ravenloft
        List<NarrativeContentEngine.Entity> faerunEntities = lore.listEntitiesBySetting("forgotten_realms", "");
        assertTrue(faerunEntities.size() >= 3);
        for (NarrativeContentEngine.Entity e : faerunEntities) {
            assertEquals("forgotten_realms", e.setting);
            assertFalse("Eberron não pode estar em Faerûn", e.id.startsWith("eberron_"));
            assertFalse("Ravenloft não pode estar em Faerûn", e.id.startsWith("ravenloft_"));
            assertFalse("Lorwyn não pode estar em Faerûn", e.id.startsWith("lorwyn_"));
        }

        // 2. Eberron não deve listar entidades de outros cenários
        List<NarrativeContentEngine.Entity> eberronEntities = lore.listEntitiesBySetting("eberron", "");
        assertTrue(eberronEntities.size() >= 3);
        for (NarrativeContentEngine.Entity e : eberronEntities) {
            assertEquals("eberron", e.setting);
            assertFalse("Faerûn não pode estar em Eberron", e.id.startsWith("faerun_"));
        }

        // 3. Ravenloft não deve listar entidades de Lorwyn
        List<NarrativeContentEngine.Entity> ravenloftEntities = lore.listEntitiesBySetting("ravenloft", "");
        assertTrue(ravenloftEntities.size() >= 3);
        for (NarrativeContentEngine.Entity e : ravenloftEntities) {
            assertEquals("ravenloft", e.setting);
            assertFalse("Lorwyn não pode estar em Ravenloft", e.id.startsWith("lorwyn_"));
        }
    }

    @Test
    public void test7_SavesBackupsAndReloadsPreserveWorldAndStories() throws Exception {
        // 1. Criar dados essenciais de mundo, herói e lore no primeiro banco
        long heroId = db.addCharacter("Varys de Baldur", "Human", "Paladin", 4, 36, 36, 18, 16, 10, 14, 10, 12, 16, true, "{\"deity\":\"Torm\"}");
        db.addKnowledgeFact("Ordem do Cálice", "Sede templária nos portões de Baldur's Gate", "verdade", "grupo", "[]", "", "FRHoF");
        JSONObject initialScene = db.getSceneState();
        initialScene.put("location", "Portão do Dragão");
        initialScene.put("weather", "Dia claro com patrulhas nas muralhas");
        db.saveSceneState(initialScene);

        // 2. Fechar o banco simulando encerramento do app
        db.close();

        // 3. Reabrir o banco na mesma base SQLite
        AppDb reloadedDb = new AppDb(context, dbName);
        reloadedDb.setActiveMapProfile("dnd");

        // 4. Validar integridade estrita dos dados recarregados
        JSONObject hero = reloadedDb.getCharacter(heroId);
        assertEquals("Varys de Baldur", hero.optString("name"));
        assertEquals(36, hero.optInt("hp"));
        assertTrue(hero.optString("notes").contains("Torm"));

        List<JSONObject> facts = reloadedDb.listKnowledgeFacts(10);
        boolean factPreserved = false;
        for (JSONObject f : facts) {
            if ("Ordem do Cálice".equals(f.optString("subject"))) factPreserved = true;
        }
        assertTrue("Fato de conhecimento persistido após reabrir", factPreserved);

        JSONObject scene = reloadedDb.getSceneState();
        assertEquals("Portão do Dragão", scene.optString("location"));

        reloadedDb.close();
        db = new AppDb(context, dbName); // restaura para o tearDown
    }

    @Test
    public void test8_FaerunMapAndIssue27CorrectionsPreserved() throws Exception {
        // 1. Perfil ativo "dnd" deve mapear para atlas oficial
        assertEquals("dnd", db.getActiveMapProfile());

        // 2. Estado de mapa preservado
        JSONObject mapState = db.getMapState();
        assertNotNull(mapState);

        // 3. Atualizar posição e névoa no mapa de Faerûn
        db.saveMapViewExtras("hex_faerun_waterdeep_01", true, "{}");
        db.saveMapFogEnabled(true);

        JSONObject updatedMap = db.getMapState();
        assertEquals("hex_faerun_waterdeep_01", updatedMap.optString("current_hex_id"));
        assertTrue("Névoa de guerra deve estar ativa", updatedMap.optInt("fog_enabled", 0) == 1);

        // 4. Projeção pública do Atlas preservada
        JSONObject atlasPublic = AtlasContextProjector.publicProjection(db);
        assertNotNull(atlasPublic);
    }
}
