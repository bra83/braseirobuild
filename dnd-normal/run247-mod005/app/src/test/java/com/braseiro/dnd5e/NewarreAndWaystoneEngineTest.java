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

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Suite de testes completa de conformidade para Newarre e a Pousada Marco do Percurso.
 * Cobre todos os 16 pontos de validação obrigatórios da ordem de implementação.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class NewarreAndWaystoneEngineTest {

    private Context context;
    private AppDb db;
    private String dbName;

    @Before
    public void setup() {
        context = RuntimeEnvironment.getApplication();
        dbName = "test_newarre_" + UUID.randomUUID().toString() + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
        context.deleteDatabase(dbName);
    }

    @Test
    public void testHexagonAndRegionalLocationIntegrity() {
        assertEquals("DO40", NewarreAndWaystoneEngine.HEX_ID);
        assertEquals("DQ36", NewarreAndWaystoneEngine.ROAD_REFERENCE_HEX_ID);
        assertEquals("Vale do Dessarin", NewarreAndWaystoneEngine.REGION_NAME);
        assertEquals("Newarre", NewarreAndWaystoneEngine.SETTLEMENT_NAME);
        assertEquals("Pousada Marco do Percurso", NewarreAndWaystoneEngine.INN_NAME);
    }

    @Test
    public void testIdempotentInstallationAndPoisPresence() throws Exception {
        // Primeira instalação
        NewarreAndWaystoneEngine.install(db);

        // Valida POI de Newarre
        JSONObject newarrePoi = db.getCampaignPoi(NewarreAndWaystoneEngine.POI_NEWARRE_ID);
        assertTrue("Newarre POI deve existir", newarrePoi.length() > 0);
        assertEquals("Newarre", newarrePoi.optString("name"));
        assertEquals("DO40", newarrePoi.optString("hex_id"));
        assertEquals("povoado", newarrePoi.optString("type"));
        assertFalse("Não deve ser canonical do core atlas", newarrePoi.optInt("canonical", 0) == 1);
        assertFalse("Não deve estar oculto", newarrePoi.optInt("hidden", 0) == 1);
        assertEquals("Deve estar descoberto", 1, newarrePoi.optInt("discovered", 0));

        // Valida POI da Pousada Marco do Percurso
        JSONObject innPoi = db.getCampaignPoi(NewarreAndWaystoneEngine.POI_WAYSTONE_INN_ID);
        assertTrue("Pousada POI deve existir", innPoi.length() > 0);
        assertEquals("Pousada Marco do Percurso", innPoi.optString("name"));
        assertEquals("DO40", innPoi.optString("hex_id"));
        assertEquals(NewarreAndWaystoneEngine.POI_NEWARRE_ID, innPoi.optString("parent_poi_id"));

        // Valida Sublocais da Pousada e da Aldeia
        String[] subPois = {
                NewarreAndWaystoneEngine.POI_HALL_ID,
                NewarreAndWaystoneEngine.POI_BAR_ID,
                NewarreAndWaystoneEngine.POI_KITCHEN_ID,
                NewarreAndWaystoneEngine.POI_ROOMS_ID,
                NewarreAndWaystoneEngine.POI_KOTE_ROOMS_ID,
                NewarreAndWaystoneEngine.POI_BAST_ROOMS_ID,
                NewarreAndWaystoneEngine.POI_STABLES_ID,
                NewarreAndWaystoneEngine.POI_BLACKSMITH_ID,
                NewarreAndWaystoneEngine.POI_MILL_ID,
                NewarreAndWaystoneEngine.POI_SQUARE_ID,
                NewarreAndWaystoneEngine.POI_FARMS_ID,
                NewarreAndWaystoneEngine.POI_WOODS_ID
        };
        for (String subId : subPois) {
            JSONObject sub = db.getCampaignPoi(subId);
            assertTrue("Sublocal " + subId + " deve existir", sub.length() > 0);
            assertEquals("DO40", sub.optString("hex_id"));
            assertFalse(sub.optString("parent_poi_id").isEmpty());
        }

        // Segunda instalação (idempotência: não duplica registros)
        NewarreAndWaystoneEngine.install(db);
        List<JSONObject> poisInHex = db.getCampaignPoisForHex("faerun-atlas-2025-ptbr-21600x15390-v2-max", "DO40", true);
        assertEquals("Exatamente 14 POIs (1 aldeia + 1 pousada + 12 sublocais) devem existir em DO40", 14, poisInHex.size());
    }

    @Test
    public void testSettlementRegistration() {
        NewarreAndWaystoneEngine.install(db);

        JSONObject settlement = db.getSettlementForPoi(NewarreAndWaystoneEngine.POI_NEWARRE_ID);
        assertTrue("Assentamento de Newarre deve estar registrado em campaign_settlements", settlement.length() > 0);
        assertEquals(95, settlement.optInt("population"));
        assertEquals("aldeia_pequena", settlement.optString("population_band"));
        assertTrue(settlement.optString("government").contains("Conselho"));
        assertTrue(settlement.optString("economy").contains("Agricultura"));
        assertEquals(55, settlement.optInt("security"));
        assertEquals(50, settlement.optInt("prosperity"));
    }

    @Test
    public void testKoteIdentityAndProtectedSecrets() throws Exception {
        NewarreAndWaystoneEngine.install(db);

        JSONObject kote = db.findNpcMemoryByName("Kote");
        assertTrue("Kote deve estar registrado como NPC persistente", kote.length() > 0);
        assertEquals("DO40", kote.optString("hex_id"));
        assertTrue(kote.optString("last_seen").contains("Marco do Percurso"));

        // Segredos exclusivos do Mestre
        String secrets = kote.optString("secrets");
        assertTrue("Segredo deve registrar Kvothe", secrets.contains("Kvothe"));
        assertTrue("Segredo deve registrar espada Folly ou baú", secrets.contains("Folly") || secrets.contains("baú"));

        // A apresentação comum / summary NÃO deve revelar Kvothe
        String summary = kote.optString("summary");
        assertFalse("Apresentação pública não pode vazar a identidade de Kvothe", summary.toLowerCase().contains("kvothe"));

        // Ficha stats_json compatível com D&D 5e
        JSONObject stats = new JSONObject(kote.optString("stats_json", "{}"));
        assertEquals(11, stats.optInt("ac"));
        assertEquals(14, stats.optInt("hp"));
        assertEquals("vivo", stats.optString("status"));
        assertEquals("Humano", stats.optString("race"));
    }

    @Test
    public void testBastIdentityAndProtectedFeySecrets() {
        NewarreAndWaystoneEngine.install(db);

        JSONObject bast = db.findNpcMemoryByName("Bast");
        assertTrue("Bast deve estar registrado", bast.length() > 0);
        assertEquals("DO40", bast.optString("hex_id"));

        // Segredo de Bast
        String secrets = bast.optString("secrets");
        assertTrue("Segredo deve registrar natureza feérica", secrets.toLowerCase().contains("feérico") || secrets.toLowerCase().contains("fey"));

        // Apresentação pública não deve denunciar sua natureza fey aos aldeões
        String summary = bast.optString("summary");
        assertFalse("Resumo público não deve revelar sua origem sobrenatural", summary.toLowerCase().contains("fauno"));
    }

    @Test
    public void testVillagersIdentitiesAndCognition() {
        NewarreAndWaystoneEngine.install(db);

        String[] residents = {"Oswin Talm", "Mira Venn", "Edda Fen", "Teren Vale", "Linna Arvel"};
        for (String name : residents) {
            JSONObject npc = db.findNpcMemoryByName(name);
            assertTrue("Morador " + name + " deve estar cadastrado", npc.length() > 0);
            assertEquals("DO40", npc.optString("hex_id"));
            assertFalse("Morador deve ter ocupação e resumo", npc.optString("summary").isEmpty());
            assertFalse("Morador deve ter atitude", npc.optString("attitude").isEmpty());
        }
    }

    @Test
    public void testSensoryFirstArrivalNarrationLength() {
        String narration = NewarreAndWaystoneEngine.buildFirstArrivalNarration("outono", "crepúsculo", "névoa suave e brisa fria");
        int length = narration.length();
        assertTrue("A narração de primeira chegada deve ter entre 5.000 e 6.000 caracteres (atual: " + length + ")",
                length >= 5000 && length <= 6000);
        assertFalse("Não deve começar com 'Vocês'", narration.startsWith("Vocês") || narration.startsWith("vocês"));
        assertTrue("Deve citar o silêncio de três partes", narration.contains("silêncio em três partes"));
        assertTrue("Deve citar Kote e o cabelo ruivo", narration.contains("cabelos") && narration.contains("vermelho"));
        assertTrue("Deve citar Bast", narration.contains("Bast") || narration.contains("asa de corvo"));
    }

    @Test
    public void testDynamicContextualNarrationAndVisits() {
        // Primeira visita gera narração longa
        String first = NewarreAndWaystoneEngine.getSceneNarrationForNewarre(db, "inverno", "noite", "neve fina");
        assertTrue("Primeira visita deve gerar narração completa de chegada", first.length() >= 5000 && first.length() <= 6000);

        // Segunda visita gera narração contextual dinâmica
        String second = NewarreAndWaystoneEngine.getSceneNarrationForNewarre(db, "inverno", "noite", "neve fina");
        assertTrue("Segunda visita deve ser dinâmica e concisa", second.length() < 1000);
        assertTrue("Deve mencionar retorno a Newarre e Pousada", second.contains("Newarre") && second.contains("Pousada"));
    }

    @Test
    public void testEconomicServicesAndPrices() {
        JSONObject economy = NewarreAndWaystoneEngine.getEconomicServices();
        assertTrue(economy.has("lodging"));
        assertTrue(economy.has("meals"));
        assertTrue(economy.has("supplies"));

        JSONArray lodging = economy.optJSONArray("lodging");
        assertTrue("Deve conter opções de hospedagem", lodging.length() >= 3);

        JSONArray meals = economy.optJSONArray("meals");
        assertTrue("Deve conter refeições com preços 5e (pc/pp/po)", meals.length() >= 4);
    }

    @Test
    public void testSupplementRegistryIntegration() {
        JSONObject summary = SupplementContentRegistry.catalogSummary();
        JSONArray packs = summary.optJSONArray("compiled_packs");
        boolean found = false;
        for (int i = 0; i < packs.length(); i++) {
            if ("newarre_waystone_inn".equals(packs.optJSONObject(i).optString("id"))) {
                found = true;
                break;
            }
        }
        assertTrue("Newarre deve estar no catálogo de suplementos", found);

        JSONObject match = SupplementContentRegistry.matchAction("vamos descansar na pousada marco do percurso em newarre");
        assertTrue("Deve dar match para menções a Newarre e Marco do Percurso", match.optBoolean("matched", false));
    }

    @Test
    public void testSpreaderEngineSlotsIncludeNewarre() throws Exception {
        NpcWorldSpreaderEngine spreader = new NpcWorldSpreaderEngine(db);
        NewarreAndWaystoneEngine.install(db);

        // Cria um NPC de teste para testar a distribuição
        JSONObject testNpc = new JSONObject()
                .put("name", "Viajante Teste")
                .put("summary", "Viajante procurando uma estalagem calma")
                .put("active", 1)
                .put("stats_json", "{}");
        db.upsertNpcMemory(testNpc);

        JSONObject report = spreader.spreadActiveNpcs();
        assertTrue("Distribuição deve ser bem-sucedida", report.optBoolean("success", false));
    }

    @Test
    public void testMapSearchAndCatalogLookupForNewarreAndWaystone() {
        String packageId = "faerun-atlas-2025-ptbr-21600x15390-v2-max";
        NewarreAndWaystoneEngine.install(db);

        // 1. Busca por "newarre"
        List<JSONObject> resultsNewarre = db.findMapLocationsByQuery(packageId, "newarre", 10);
        assertFalse("Busca por 'newarre' não deve ser vazia", resultsNewarre.isEmpty());
        JSONObject first = resultsNewarre.get(0);
        assertEquals("Newarre", first.optString("name"));
        assertEquals("DO40", first.optString("hex_id"));
        assertEquals("Vale do Dessarin", first.optString("region"));

        // 2. Busca por "marco do percurso"
        List<JSONObject> resultsInn = db.findMapLocationsByQuery(packageId, "marco do percurso", 10);
        assertFalse("Busca por 'marco do percurso' não deve ser vazia", resultsInn.isEmpty());
        boolean foundInn = false;
        for (JSONObject loc : resultsInn) {
            if (loc.optString("name").contains("Marco do Percurso") && "DO40".equals(loc.optString("hex_id"))) {
                foundInn = true;
                break;
            }
        }
        assertTrue("Busca deve encontrar Marco do Percurso em DO40", foundInn);

        // 3. Busca por "kote" (alias)
        List<JSONObject> resultsKote = db.findMapLocationsByQuery(packageId, "kote", 10);
        assertFalse("Busca por 'kote' deve retornar resultados associados", resultsKote.isEmpty());
        assertEquals("DO40", resultsKote.get(0).optString("hex_id"));

        // 4. Locais do Hex DO40
        List<JSONObject> hexLocs = db.getMapLocationsForHex(packageId, "DO40");
        assertFalse("Hex DO40 deve conter locais", hexLocs.isEmpty());
        boolean hasNewarre = false;
        boolean hasWaystone = false;
        for (JSONObject loc : hexLocs) {
            if ("Newarre".equals(loc.optString("name"))) hasNewarre = true;
            if (loc.optString("name").contains("Marco do Percurso")) hasWaystone = true;
        }
        assertTrue("Hex DO40 deve listar Newarre", hasNewarre);
        assertTrue("Hex DO40 deve listar Marco do Percurso", hasWaystone);

        // 5. findConfirmedLocationsByQuery
        List<JSONObject> confirmed = db.findConfirmedLocationsByQuery(packageId, "newarre", 10);
        assertFalse("Locais confirmados devem conter Newarre", confirmed.isEmpty());
        assertEquals("DO40", confirmed.get(0).optString("hex_id"));
    }
}
