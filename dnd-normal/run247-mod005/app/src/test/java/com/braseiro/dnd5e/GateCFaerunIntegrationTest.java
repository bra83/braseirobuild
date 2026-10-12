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
 * Suite de Homologação Automatizada do GATE C:
 * Forgotten Realms: Heroes of Faerûn (FRHoF) + Adventures in Faerûn (FRAiF).
 * Valida:
 * 1. Opções de Personagem de Faerûn (Subclasses, Backgrounds, Feats, Spells, Círculo Mágico, Catálogo de Aurora).
 * 2. Destinos Épicos e Rulings Canônicos de Aventuras em Faerûn.
 * 3. Lore estruturado do cenário Forgotten Realms com Knowledge Firewall e módulo narrativo no AdventureEngine.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class GateCFaerunIntegrationTest {

    private String dbName;
    private AppDb db;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbName = "gate_c_test_" + UUID.randomUUID().toString().substring(0, 8) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null) context.deleteDatabase(dbName);
    }

    @Test
    public void testFrhofMechanicsAndCharacterLifecycle() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Criar personagem no AppDb
        long charId = db.addCharacter("Aragorn de Cormyr", "Human", "Fighter", 3, 28, 28, 16, 16, 12, 14, 10, 12, 10, true, "{}");
        assertTrue("Personagem deve ser inserido", charId > 0);

        // 2. Aplicar Background canônico (Harper)
        JSONObject bgReq = new JSONObject()
                .put("character_id", charId)
                .put("background", "Harper")
                .put("level", 3);
        JSONObject bgRes = gateway.execute("frhof", "apply_background", bgReq);
        assertTrue("Background Harper deve ser aplicado", bgRes.optBoolean("executed"));
        assertTrue("Deve persistir no AppDb", bgRes.optBoolean("persisted"));

        // 3. Aplicar Subclasse de Faerûn (Banneret)
        JSONObject subReq = new JSONObject()
                .put("character_id", charId)
                .put("subclass", "Banneret")
                .put("level", 3);
        JSONObject subRes = gateway.execute("frhof", "apply_subclass", subReq);
        assertTrue("Subclasse Banneret deve ser aplicada", subRes.optBoolean("executed"));

        // 4. Aplicar Feat de Faerûn (Harper Agent)
        JSONObject featReq = new JSONObject()
                .put("character_id", charId)
                .put("feat", "Harper Agent")
                .put("level", 3);
        JSONObject featRes = gateway.execute("frhof", "apply_feat", featReq);
        assertTrue("Feat Harper Agent deve ser aplicada", featRes.optBoolean("executed"));

        // 5. Consultar Spells canônicas de Faerûn
        JSONObject spell1 = gateway.execute("frhof", "spell_info", new JSONObject().put("spell", "Blade of Disaster"));
        assertTrue(spell1.optBoolean("executed"));
        JSONObject s1Result = spell1.getJSONObject("result");
        assertEquals("Blade of Disaster", s1Result.optString("name"));
        assertEquals(9, s1Result.optInt("level"));
        assertEquals("Conjuration", s1Result.optString("school"));

        JSONObject spell2 = gateway.execute("frhof", "spell_info", new JSONObject().put("spell", "Death Armor"));
        assertTrue(spell2.optBoolean("executed"));
        JSONObject s2Result = spell2.getJSONObject("result");
        assertEquals("Death Armor", s2Result.optString("name"));
        assertEquals(2, s2Result.optInt("level"));
        assertTrue(s2Result.optBoolean("death_save_advantage"));

        // 6. Consultar Catálogo de Aurora
        JSONObject aurora = gateway.execute("frhof", "aurora_info", new JSONObject().put("item", "Genie Robe"));
        assertTrue(aurora.optBoolean("executed"));
        JSONObject aResult = aurora.getJSONObject("result");
        assertEquals("Genie Robe", aResult.optString("name"));
        assertEquals("Calimshan Wares", aResult.optString("catalog"));

        // 7. Descanso Longo e Reset de Recursos
        JSONObject lrRes = gateway.execute("frhof", "long_rest", new JSONObject().put("character_id", charId));
        assertTrue(lrRes.optBoolean("executed"));
        assertTrue(lrRes.optBoolean("persisted"));

        // 8. Validação da persistência recarregando a ficha do SQLite
        JSONObject reloaded = db.getCharacter(charId);
        String sheetStr = reloaded.optString("sheet_json", "{}");
        JSONObject sheet = new JSONObject(sheetStr);
        assertEquals("Harper", sheet.optString("background"));
        assertEquals("Banneret", sheet.optString("subclass_name"));
        assertTrue(sheet.optBoolean("frhof_background_runtime"));
        assertTrue(sheet.optBoolean("frhof_subclass_runtime"));
    }

    @Test
    public void testFraifEpicDestinies() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(db);

        // 1. Destino Épico: Heir to the Throne (Nível 4 e Nível 17)
        JSONObject heirLvl4 = gateway.execute("fraif", "epic_destiny",
                new JSONObject().put("destiny", "Heir to the Throne").put("level", 4));
        assertTrue(heirLvl4.optBoolean("executed"));
        JSONObject hResult4 = heirLvl4.getJSONObject("result");
        assertTrue(hResult4.optBoolean("source_verified"));
        JSONObject m4 = hResult4.getJSONObject("current_milestone");
        assertEquals(4, m4.optInt("level"));
        assertEquals("Skill Expert feat", m4.optString("benefit"));

        JSONObject heirLvl17 = gateway.execute("fraif", "epic_destiny",
                new JSONObject().put("destiny", "Heir to the Throne").put("level", 17));
        JSONObject hResult17 = heirLvl17.getJSONObject("result");
        JSONObject m17 = hResult17.getJSONObject("current_milestone");
        assertEquals(17, m17.optInt("level"));
        assertTrue(m17.optString("benefit").contains("Foresight"));

        // 2. Destino Épico: Pupil of the Archmage (Nível 5)
        JSONObject pupilLvl5 = gateway.execute("fraif", "epic_destiny",
                new JSONObject().put("destiny", "Pupil of the Archmage").put("level", 5));
        JSONObject pResult5 = pupilLvl5.getJSONObject("result");
        JSONObject pm5 = pResult5.getJSONObject("current_milestone");
        assertEquals(5, pm5.optInt("level"));
        assertEquals("Archmage spellbook", pm5.optString("benefit"));

        // 3. Destino não registrado no livro (Fail-Closed)
        JSONObject nonExistent = gateway.execute("fraif", "epic_destiny",
                new JSONObject().put("destiny", "Destino_Inventado_Sem_Fonte").put("level", 20));
        assertFalse(nonExistent.getJSONObject("result").optBoolean("source_verified"));
        assertEquals("example_not_defined_by_source", nonExistent.getJSONObject("result").optString("status"));

        // 4. Perigo regional não universalizado (Fail-Closed)
        JSONObject hazard = gateway.execute("fraif", "regional_hazard",
                new JSONObject().put("region", "Anauroch").put("survival_roll", 15).put("con_save_roll", 14));
        assertFalse(hazard.getJSONObject("result").optBoolean("resolved"));
        assertEquals("no_universal_hazard_rule_in_source", hazard.getJSONObject("result").optString("status"));
    }

    @Test
    public void testFaerunLoreAndAdventureModule() throws Exception {
        NarrativeContentEngine loreEngine = new NarrativeContentEngine(db);

        // 1. Entidade de Lore: Elminster Aumar com Knowledge Firewall
        List<String> facts = new ArrayList<>();
        facts.add("Elminster é o Sábio de Shadowdale e Escolhido de Mystra.");
        facts.add("Viveu por mais de mil anos servindo à Trama.");

        List<String> secrets = new ArrayList<>();
        secrets.add("Guarda no refúgio uma centelha pura de Magia Prateada pré-Karsus.");
        secrets.add("Mantém comunicação direta com o Conselho Secreto dos Harpers.");

        NarrativeContentEngine.Entity elminster = new NarrativeContentEngine.Entity(
                "elminster_aumar",
                "FRHoF",
                "forgotten_realms",
                "1492 DR",
                "npc",
                "Elminster Aumar",
                "Elminster Aumar",
                "O lendário Arquimago de Shadowdale.",
                facts,
                secrets,
                Collections.singletonList("harpers"),
                Collections.emptyMap(),
                "secreto",
                false
        );
        assertTrue(loreEngine.saveEntity(elminster));

        // 2. Visão do Jogador protege estritamente os segredos do Arquimago
        JSONObject pView = loreEngine.buildPlayerView("forgotten_realms", Collections.singletonList("elminster_aumar"));
        assertEquals(0, pView.optJSONArray("entities").length());
        assertTrue(pView.optInt("omitted_secrets_protected") >= 2);

        // 3. Módulo de Aventura em Faerûn no AdventureEngine
        AdventureTransition tCoast = new AdventureTransition("t_coast", "node_daggerford", "Viajar para Daggerford", "", "", false, null, null);
        AdventureNode startNode = new AdventureNode(
                "node_waterdeep",
                "Taverna do Portal Bocejante",
                "Em Águas Profundas, o poço central desce rumo a Undermountain.",
                "Durnan observa os aventureiros em silêncio.",
                "waterdeep_yawning_portal",
                Collections.singletonList("Durnan"),
                Collections.singletonList(tCoast),
                null,
                null
        );

        AdventureNode daggerfordNode = new AdventureNode(
                "node_daggerford",
                "Portões de Daggerford",
                "Uma vila fortificada na Costa da Espada sob chuva constante.",
                "Guardas locais pedem identificação.",
                "sword_coast_daggerford",
                Collections.<String>emptyList(),
                Collections.<AdventureTransition>emptyList(),
                null,
                null
        );

        Map<String, AdventureNode> nodes = new HashMap<>();
        nodes.put(startNode.getId(), startNode);
        nodes.put(daggerfordNode.getId(), daggerfordNode);

        AdventureModule swordCoastMod = new AdventureModule(
                "mod_faerun_sword_coast",
                "Sombras da Costa da Espada",
                "1.0.0",
                "node_waterdeep",
                nodes,
                new JSONObject().put("setting", "forgotten_realms")
        );

        AdventureEngine advEngine = new AdventureEngine(db, null, null);
        JSONObject session = advEngine.loadOrStartAdventure(swordCoastMod, 777L);
        assertNotNull(session);

        // Avança de Waterdeep para Daggerford
        JSONObject moveRes = advEngine.processAction("t_coast", new JSONObject().put("target_node_id", "node_daggerford"));
        assertTrue(moveRes.optBoolean("committed"));
        assertEquals("node_daggerford", moveRes.optString("current_node_id"));

        // Reinicia AdventureEngine e valida persistência da posição na Costa da Espada
        AdventureEngine reloadedEngine = new AdventureEngine(db, null, null);
        JSONObject resumed = reloadedEngine.loadOrStartAdventure(swordCoastMod, 777L);
        String resumedId = resumed.optJSONObject("current_node") != null
                ? resumed.optJSONObject("current_node").optString("id")
                : resumed.optJSONObject("state").optString("current_node_id");
        assertEquals("node_daggerford", resumedId);
    }
}
