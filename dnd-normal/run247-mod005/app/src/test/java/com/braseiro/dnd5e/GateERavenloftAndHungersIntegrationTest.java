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
 * Suite de Homologação Automatizada do GATE E:
 * Ravenloft: The Horrors Within (RHW) + Astarion's Book of Hungers (ABH).
 * Valida:
 * 1. Dádivas Sombrias (Dark Gifts), Viagem pelas Brumas (Mists Travel), Magia nas Brumas e subclasses de RHW.
 * 2. Feats Vampíricos, Fome Vampírica determinística, Epic Boons de ABH com persistência no AppDb.
 * 3. Lore estruturado dos Domínios do Pavor (Baróvia, Castelo Ravenloft) com Knowledge Firewall e segredos de Darklord.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class GateERavenloftAndHungersIntegrationTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "gate_e_test_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(dbName);
    }

    @Test
    public void testRavenloftDarkGiftsMistsAndSubclasses() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Criar Personagem Assombrado (Haunted One) no AppDb
        long charId = db.addCharacter("Ezmerelda D'Avenir", "Human", "Rogue", 5, 38, 38, 16, 12, 18, 14, 14, 16, 10, true, "{}");
        assertTrue("Personagem inserido com sucesso", charId > 0);

        // 2. Descritor e Listagens de Ravenloft
        JSONObject desc = gateway.execute("rhw", "descriptor", new JSONObject());
        assertTrue(desc.optBoolean("executed"));
        JSONObject cov = gateway.execute("rhw", "coverage", new JSONObject());
        assertTrue(cov.optBoolean("executed"));

        JSONObject feats = gateway.execute("rhw", "list_feats", new JSONObject());
        assertTrue(feats.optBoolean("executed"));
        assertTrue("Deve listar Dark Gifts e Origin feats", feats.optJSONArray("feats").length() >= 11);

        JSONObject subclasses = gateway.execute("rhw", "list_subclasses", new JSONObject());
        assertTrue(subclasses.optBoolean("executed"));
        assertTrue(subclasses.optJSONArray("subclasses").length() >= 7);

        // 3. Regras de Viagem pelas Brumas (Mists Travel) e Magia nas Brumas
        JSONObject mistsReq = new JSONObject()
                .put("has_mist_talisman", true)
                .put("destination_domain", "Barovia")
                .put("travel_hours_d6", 4);
        JSONObject mistsRes = gateway.execute("rhw", "mists_travel", mistsReq);
        assertTrue("Viagem com talismã deve ser resolvida determinística", mistsRes.optBoolean("executed"));
        assertEquals("Barovia", mistsRes.getJSONObject("result").optString("destination"));
        assertEquals(4, mistsRes.getJSONObject("result").optInt("hours"));

        JSONObject magicRes = gateway.execute("rhw", "magic_in_mists", new JSONObject().put("effect", "plane_shift"));
        assertTrue(magicRes.optBoolean("executed"));
        assertFalse("Plane Shift não permite escapar de Ravenloft", magicRes.getJSONObject("result").optBoolean("allowed"));

        // 4. Execução de Dádiva Sombria: Sharp Eye
        JSONObject sharpReq = new JSONObject()
                .put("character_id", charId)
                .put("action_type", "search")
                .put("check_succeeded", true);
        JSONObject sharpRes = gateway.execute("rhw", "sharp_eye", sharpReq);
        assertTrue(sharpRes.optBoolean("executed"));
        assertTrue(sharpRes.optBoolean("persisted"));

        // 5. Descanso e Restauração de Usos
        JSONObject restRes = gateway.execute("rhw", "long_rest", new JSONObject().put("character_id", charId));
        assertTrue(restRes.optBoolean("executed"));
        assertTrue(restRes.optBoolean("persisted"));
    }

    @Test
    public void testAstarionHungersVampiricEmpowermentAndBoons() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Criar Dhampir no AppDb com PV reduzido
        long charId = db.addCharacter("Astarion Ancunin", "Dhampir", "Rogue", 6, 20, 45, 16, 10, 18, 14, 12, 14, 16, true, "{}");
        assertTrue("Dhampir inserido com sucesso", charId > 0);

        // 2. Feats Vampíricos e Epic Boons
        JSONObject feats = gateway.execute("abh", "list_feats", new JSONObject());
        assertTrue(feats.optBoolean("executed"));
        JSONArray featList = feats.optJSONArray("feats");
        assertNotNull(featList);
        assertTrue("Deve listar feats vampíricos de ABH", featList.length() >= 10);

        JSONObject boon = gateway.execute("abh", "epic_boon", new JSONObject().put("boon", "Boon of Misty Escape"));
        assertTrue(boon.optBoolean("executed"));
        JSONObject bRes = boon.getJSONObject("result");
        assertTrue(bRes.optBoolean("source_verified"));
        assertEquals(1, bRes.optInt("instead_hp"));

        // 3. Regra Canônica: Blood Frenzy não inventada arbitrariamente (bloqueada conforme contrato oficial)
        JSONObject frenzy = gateway.execute("abh", "blood_frenzy", new JSONObject().put("hunger_level", 2));
        assertFalse("Blood frenzy sem suporte de regra no livro oficial é bloqueada", frenzy.optBoolean("resolved"));

        // 4. Vampiric Bite Empowerment (Drenar PV para cura determinística)
        JSONObject biteReq = new JSONObject()
                .put("character_id", charId)
                .put("piercing_damage", 8)
                .put("target_creature_type", "humanoid")
                .put("choice", "drain");
        JSONObject biteRes = gateway.execute("abh", "dhampir_bite_empower", biteReq);
        assertTrue("Mordida vampírica empoderada deve ser executada", biteRes.optBoolean("executed"));
        assertTrue("Persistência no AppDb realizada", biteRes.optBoolean("persisted"));

        // Validar cura aplicada na ficha
        JSONObject charAfter = db.getCharacter(charId);
        assertEquals(28, charAfter.optInt("hp")); // 20 + 8

        // 5. Long Rest Reset de ABH
        JSONObject resetRes = gateway.execute("abh", "long_rest_reset", new JSONObject().put("character_id", charId));
        assertTrue(resetRes.optBoolean("executed"));
        assertEquals(0, resetRes.optInt("uses_after"));
    }

    @Test
    public void testRavenloftLoreAndDarklordKnowledgeFirewall() throws Exception {
        NarrativeContentEngine loreEngine = new NarrativeContentEngine(db);

        // 1. Entidade de Lore: Castelo Ravenloft em Baróvia
        List<String> facts = new ArrayList<>();
        facts.add("Castelo ancestral de Strahd von Zarovich construído sobre os penhascos de Baróvia.");
        facts.add("As brumas cercam as torres e os cidadãos de Baróvia temem olhar para suas janelas.");

        List<String> secrets = new ArrayList<>();
        secrets.add("O Coração do Pavor nas torres confere pontos de vida adicionais a Strahd.");
        secrets.add("O túmulo de Sergei contém o segredo para romper a barreira do Darklord.");

        NarrativeContentEngine.Entity castleRavenloft = new NarrativeContentEngine.Entity(
                "castle_ravenloft",
                "RHW",
                "ravenloft",
                "735 BC",
                "local",
                "Castelo Ravenloft",
                "Castle Ravenloft",
                "A temida fortaleza do primeiro Lorde das Brumas.",
                facts,
                secrets,
                Collections.singletonList("barovia"),
                Collections.emptyMap(),
                "secreto",
                false
        );
        assertTrue(loreEngine.saveEntity(castleRavenloft));

        // 2. Knowledge Firewall: Jogadores NUNCA recebem segredos de Darklords ou relíquias
        JSONObject playerView = loreEngine.buildPlayerView("ravenloft", Collections.singletonList("castle_ravenloft"));
        assertEquals("player", playerView.optString("role"));
        assertEquals(0, playerView.optJSONArray("entities").length()); // entidade secreta filtrada para o jogador não exposto
        assertTrue(playerView.optInt("omitted_secrets_protected") >= 2);

        // 3. Isolamento Estrito: Ravenloft não deve vazar para Toril ou Eberron
        JSONObject eberronView = loreEngine.buildPlayerView("eberron", Collections.singletonList("castle_ravenloft"));
        assertEquals(0, eberronView.optJSONArray("entities").length());

        // 4. Visão do Mestre de Ravenloft com acesso completo aos segredos do Lorde das Brumas
        JSONObject gmView = loreEngine.buildGmView("ravenloft", Collections.singletonList("castle_ravenloft"));
        assertEquals("gm", gmView.optString("role"));
        assertEquals(1, gmView.optJSONArray("entities").length());
        JSONObject eObj = gmView.optJSONArray("entities").getJSONObject(0);
        assertEquals(2, eObj.optJSONArray("gm_secrets").length());
        assertEquals("Castle Ravenloft", eObj.optString("original_name"));
    }
}
