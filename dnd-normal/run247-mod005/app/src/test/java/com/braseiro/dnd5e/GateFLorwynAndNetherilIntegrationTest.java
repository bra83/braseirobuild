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
 * Suite de Homologação Automatizada do GATE F:
 * Lorwyn: First Light (LFL) + Netheril's Fall (NF).
 * Valida:
 * 1. Espécies, Antecedentes, Talentos e Travessia de Reinos (Eventide/Morningtide) de Lorwyn com persistência de estado.
 * 2. Efeitos Ambientais Arcanos, Perigos (Hazards escalonados por tier) e Itens de Mythallar de Netheril's Fall.
 * 3. Lore estruturado de Lorwyn e Netheril Ancestral com Knowledge Firewall e isolamento estrito de eras/cenários.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class GateFLorwynAndNetherilIntegrationTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "gate_f_test_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(dbName);
    }

    @Test
    public void testLorwynFirstLightSpeciesAndRealmCrossing() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Criar Kithkin de Lorwyn no AppDb
        long charId = db.addCharacter("Kithkin Cenn", "Kithkin", "Fighter", 4, 34, 34, 17, 14, 16, 15, 10, 12, 10, true, "{}");
        assertTrue("Personagem de Lorwyn criado", charId > 0);

        // 2. Descritor e Listagens oficiais de LFL
        JSONObject desc = gateway.execute("lfl", "descriptor", new JSONObject());
        assertTrue(desc.optBoolean("executed"));
        JSONObject cov = gateway.execute("lfl", "coverage", new JSONObject());
        assertTrue(cov.optBoolean("executed"));

        JSONObject speciesList = gateway.execute("lfl", "list_species", new JSONObject());
        assertTrue(speciesList.optBoolean("executed"));
        assertTrue("Deve listar 7 espécies de Lorwyn/Shadowmoor", speciesList.optJSONArray("species").length() >= 7);

        JSONObject bgList = gateway.execute("lfl", "list_backgrounds", new JSONObject());
        assertTrue(bgList.optBoolean("executed"));
        assertTrue(bgList.optJSONArray("backgrounds").length() >= 2);

        JSONObject items = gateway.execute("lfl", "list_items", new JSONObject());
        assertTrue(items.optBoolean("executed"));
        assertTrue(items.optJSONArray("items").length() >= 2);

        // 3. Regra de Travessia entre Reinos: Lorwyn -> Shadowmoor com Relicário da Aurora
        JSONObject crossReq = new JSONObject()
                .put("character_id", charId)
                .put("from_realm", "lorwyn")
                .put("to_realm", "shadowmoor")
                .put("native_realm", "lorwyn")
                .put("wearing_reliquary_dawn", true);
        JSONObject crossRes = gateway.execute("lfl", "realm_crossing", crossReq);
        assertTrue("Travessia resolvida", crossRes.optBoolean("executed"));
        assertTrue("Relicário da Aurora previne transformação drástica da Aurora", crossRes.getJSONObject("result").optBoolean("transformation_prevented"));

        // 4. Ciclo de descanso de LFL
        JSONObject lrRes = gateway.execute("lfl", "long_rest", new JSONObject().put("character_id", charId));
        assertTrue(lrRes.optBoolean("executed"));
        assertTrue(lrRes.optBoolean("persisted"));
    }

    @Test
    public void testNetherilsFallEnvironmentalEffectsAndHazards() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Cobertura de Netheril's Fall
        JSONObject cov = gateway.execute("nf", "coverage", new JSONObject());
        assertTrue(cov.optBoolean("executed"));
        JSONObject covRes = cov.getJSONObject("result");
        assertTrue(covRes.optBoolean("runtime_complete"));
        assertEquals(6, covRes.optInt("environmental_effects"));
        assertEquals(5, covRes.optInt("hazards"));
        assertEquals(8, covRes.optInt("magic_items"));

        // 2. Consulta de Efeitos Ambientais Arcanos
        JSONObject effects = gateway.execute("nf", "list_environmental_effects", new JSONObject());
        assertTrue(effects.optBoolean("executed"));
        assertEquals(6, effects.optJSONArray("effects").length());

        JSONObject dampenedZone = gateway.execute("nf", "environmental_effect", new JSONObject().put("name", "Dampened Magic Zone"));
        assertTrue(dampenedZone.optBoolean("executed"));
        JSONObject dzRes = dampenedZone.getJSONObject("result");
        assertTrue(dzRes.optBoolean("found"));
        assertTrue("Em Dampened Magic Zone os ataques com magias têm desvantagem", dzRes.optBoolean("spell_attacks_disadvantage"));

        // 3. Perigos de Netheril escalonados por tier
        JSONObject hazards = gateway.execute("nf", "list_hazards", new JSONObject());
        assertTrue(hazards.optBoolean("executed"));
        assertEquals(5, hazards.optJSONArray("hazards").length());

        JSONObject shadowbarkTier1 = gateway.execute("nf", "hazard", new JSONObject().put("name", "Shadowbark").put("level", 3));
        assertTrue(shadowbarkTier1.optBoolean("executed"));
        assertEquals(12, shadowbarkTier1.getJSONObject("result").optInt("dc"));
        assertEquals("1d10 Necrotic", shadowbarkTier1.getJSONObject("result").optString("damage"));

        JSONObject shadowbarkTier4 = gateway.execute("nf", "hazard", new JSONObject().put("name", "Shadowbark").put("level", 18));
        assertTrue(shadowbarkTier4.optBoolean("executed"));
        assertEquals(18, shadowbarkTier4.getJSONObject("result").optInt("dc"));
        assertEquals("10d10 Necrotic", shadowbarkTier4.getJSONObject("result").optString("damage"));

        // 4. Itens Mágicos de Mythallar
        JSONObject mythCloak = gateway.execute("nf", "item", new JSONObject().put("name", "Mythallar Cloak"));
        assertTrue(mythCloak.optBoolean("executed"));
        JSONObject mcRes = mythCloak.getJSONObject("result");
        assertTrue(mcRes.optBoolean("found"));
        assertEquals(10, mcRes.optInt("charges"));
        assertEquals(30, mcRes.optInt("fly_speed_ft"));
    }

    @Test
    public void testLorwynAndNetherilLoreFirewall() throws Exception {
        NarrativeContentEngine loreEngine = new NarrativeContentEngine(db);

        // 1. Entidade de Lorwyn: O Círculo de Aurora
        List<String> lflFacts = new ArrayList<>();
        lflFacts.add("Lorwyn é um plano fada onde o sol nunca se põe na Grande Aurora.");
        lflFacts.add("Os Flamekin mantêm acesa a chama espiritual dos ancestrais.");
        List<String> lflSecrets = new ArrayList<>();
        lflSecrets.add("A Grande Aurora oculta a aproximação periódica de Shadowmoor.");

        NarrativeContentEngine.Entity lorwynCircle = new NarrativeContentEngine.Entity(
                "lorwyn_aurora_circle",
                "LFL",
                "lorwyn",
                "dawn_era",
                "local",
                "Círculo da Grande Aurora",
                "Great Aurora Circle",
                "O nexus de transição entre Lorwyn e Shadowmoor.",
                lflFacts,
                lflSecrets,
                Collections.singletonList("aurora"),
                Collections.emptyMap(),
                "secreto",
                false
        );
        assertTrue(loreEngine.saveEntity(lorwynCircle));

        // 2. Entidade de Netheril Ancestral: Enclave de Xinok
        List<String> nfFacts = new ArrayList<>();
        nfFacts.add("Xinok era uma cidade flutuante netherense erguida por um mythallar central.");
        List<String> nfSecrets = new ArrayList<>();
        nfSecrets.add("O mythallar de Xinok continua instável nos subterrâneos de Anauroch.");

        NarrativeContentEngine.Entity xinokEnclave = new NarrativeContentEngine.Entity(
                "netheril_xinok_enclave",
                "NF",
                "forgotten_realms",
                "ancient_netheril",
                "local",
                "Enclave de Xinok",
                "Xinok Enclave",
                "Uma ruína flutuante que despencou durante a Queda de Netheril.",
                nfFacts,
                nfSecrets,
                Collections.singletonList("anauroch"),
                Collections.emptyMap(),
                "secreto",
                false
        );
        assertTrue(loreEngine.saveEntity(xinokEnclave));

        // 3. Knowledge Firewall: segredos ocultos para jogadores
        JSONObject playerViewLfl = loreEngine.buildPlayerView("lorwyn", Collections.singletonList("lorwyn_aurora_circle"));
        assertEquals(0, playerViewLfl.optJSONArray("entities").length());
        assertTrue(playerViewLfl.optInt("omitted_secrets_protected") >= 1);

        // 4. Isolamento Estrito: Cenário Lorwyn não acessa ruínas de Netheril em Toril
        JSONObject playerViewCross = loreEngine.buildPlayerView("lorwyn", Collections.singletonList("netheril_xinok_enclave"));
        assertEquals(0, playerViewCross.optJSONArray("entities").length());

        // 5. Visão do Mestre de Netheril com acesso completo aos segredos do Mythallar
        JSONObject gmViewNf = loreEngine.buildGmView("forgotten_realms", Collections.singletonList("netheril_xinok_enclave"));
        assertEquals("gm", gmViewNf.optString("role"));
        assertEquals(1, gmViewNf.optJSONArray("entities").length());
        JSONObject eObj = gmViewNf.optJSONArray("entities").getJSONObject(0);
        assertEquals(1, eObj.optJSONArray("gm_secrets").length());
        assertEquals("Xinok Enclave", eObj.optString("original_name"));
    }
}
