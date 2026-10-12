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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Teste de Reprodução e Comprovação:
 * Defeito de Persistência do Lore e Sobrescrita do Cânone (NarrativeContentEngine & Backup21).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class NarrativeCanonPersistenceTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "narrative_canon_test_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(dbName);
    }

    @Test
    public void testReSeedingCanonPreservesPlayerDiscoveriesAndCustomData() throws Exception {
        NarrativeContentEngine engine = new NarrativeContentEngine(db);
        engine.seedOfficialCanon();

        // 1. Verificar entidade inicial não descoberta
        NarrativeContentEngine.Entity harpers = engine.getEntity("faerun_harpers");
        assertNotNull(harpers);

        // Escolhemos uma entidade secreta como undermountain
        NarrativeContentEngine.Entity undermountain = engine.getEntity("faerun_undermountain_depths");
        assertNotNull(undermountain);
        assertFalse("Undermountain inicialmente não deve estar descoberta", undermountain.discovered);
        assertEquals("secreto", undermountain.visibility);

        // 2. Jogador descobre Undermountain durante a aventura
        boolean discovered = engine.discoverEntity("faerun_undermountain_depths", "Mapa antigo encontrado na Taverna do Portal Bocejante");
        assertTrue("Descoberta deve ser registrada", discovered);

        NarrativeContentEngine.Entity afterDiscovery = engine.getEntity("faerun_undermountain_depths");
        assertTrue("Deve estar marcada como descoberta", afterDiscovery.discovered);
        assertEquals("grupo", afterDiscovery.visibility);

        // 3. Simula reinício do app / nova inicialização do MasterOrchestrator chamando seedOfficialCanon()
        engine.seedOfficialCanon();

        // 4. Comprovar que a descoberta e a visibilidade NÃO FORAM sobrescritas pelo seed
        NarrativeContentEngine.Entity afterReSeed = engine.getEntity("faerun_undermountain_depths");
        assertNotNull(afterReSeed);
        assertTrue("DEFEITO CRÍTICO: seedOfficialCanon sobrescreveu discovered=true para false!", afterReSeed.discovered);
        assertEquals("grupo", afterReSeed.visibility);
    }

    @Test
    public void testBackup21RoundTripPreservesNarrativeEntitiesAndDiscoveries() throws Exception {
        NarrativeContentEngine engine = new NarrativeContentEngine(db);
        engine.seedOfficialCanon();

        // Descobre uma entidade secreta
        engine.discoverEntity("netheril_xinok_enclave", "Rastros em Anauroch desvendados");
        NarrativeContentEngine.Entity xinok = engine.getEntity("netheril_xinok_enclave");
        assertTrue(xinok.discovered);

        // Exporta Backup21
        JSONObject backup = db.exportAll();
        assertNotNull(backup);
        assertTrue(backup.has("narrativeDiscoveryState"));
        JSONArray discoveries = backup.optJSONArray("narrativeDiscoveryState");
        assertNotNull(discoveries);
        assertTrue(discoveries.length() > 0);

        // Cria novo banco e restaura o backup
        String newDbName = "narrative_restored_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb restoredDb = new AppDb(context, newDbName);
        restoredDb.setActiveMapProfile("dnd");
        restoredDb.importAll(backup);

        NarrativeContentEngine restoredEngine = new NarrativeContentEngine(restoredDb);
        // Mesmo se o Master chamar seedOfficialCanon na inicialização
        restoredEngine.seedOfficialCanon();

        NarrativeContentEngine.Entity restoredXinok = restoredEngine.getEntity("netheril_xinok_enclave");
        assertNotNull("Entidade restaurada deve existir", restoredXinok);
        assertTrue("Descoberta de Xinok deve sobreviver à restauração de Backup21 e re-seed", restoredXinok.discovered);
        assertEquals("grupo", restoredXinok.visibility);

        restoredDb.close();
        context.deleteDatabase(newDbName);
    }

    @Test
    public void reSeedMustNotExposeUndiscoveredEntitiesWhenSecrecyChanges() throws Exception {
        NarrativeContentEngine engine = new NarrativeContentEngine(db);
        String entityId = "regression_visibility_" + UUID.randomUUID().toString().substring(0, 8);
        List<String> ids = Collections.singletonList(entityId);

        NarrativeContentEngine.Entity originallyPublic = new NarrativeContentEngine.Entity(
                entityId, "FRHOF", "forgotten_realms", "present", "lore",
                "Cofre de Segredos", "Hidden Vault", "Rumor conhecido",
                Collections.singletonList("Fato comum"), Collections.singletonList("Segredo de mestre"),
                Collections.<String>emptyList(), Collections.<String, String>emptyMap(), "grupo", false);
        NarrativeContentEngine.Entity classifiedSecret = new NarrativeContentEngine.Entity(
                entityId, "FRHOF", "forgotten_realms", "present", "lore",
                "Cofre de Segredos", "Hidden Vault", "Rumor secreto",
                Collections.singletonList("Novo fato"), Collections.singletonList("Segredo de mestre"),
                Collections.<String>emptyList(), Collections.<String, String>emptyMap(), "secreto", false);

        assertTrue(engine.seedCanonicalEntity(originallyPublic));
        assertEquals(1, engine.buildPlayerView("forgotten_realms", ids)
                .getJSONArray("entities").length());

        // Um novo catálogo torna secreta uma entidade ainda não descoberta.
        assertTrue(engine.seedCanonicalEntity(classifiedSecret));
        assertFalse(engine.getEntity(entityId).discovered);
        assertEquals("secreto", engine.getEntity(entityId).visibility);
        assertEquals(0, engine.buildPlayerView("forgotten_realms", ids)
                .getJSONArray("entities").length());
        assertEquals(1, engine.buildGmView("forgotten_realms", ids)
                .getJSONArray("entities").length());

        // Reclassificação pública futura não deve vazar segredo ainda não descoberto.
        assertTrue(engine.seedCanonicalEntity(originallyPublic));
        assertEquals("secreto", engine.getEntity(entityId).visibility);
        assertEquals(0, engine.buildPlayerView("forgotten_realms", ids)
                .getJSONArray("entities").length());

        // Descoberta genuína deve liberar acesso e resistir a re-seed.
        assertTrue(engine.discoverEntity(entityId, "Descoberto em jogo"));
        assertTrue(engine.seedCanonicalEntity(classifiedSecret));
        assertTrue(engine.getEntity(entityId).discovered);
        assertEquals("grupo", engine.getEntity(entityId).visibility);
        assertEquals(1, engine.buildPlayerView("forgotten_realms", ids)
                .getJSONArray("entities").length());
    }
}
