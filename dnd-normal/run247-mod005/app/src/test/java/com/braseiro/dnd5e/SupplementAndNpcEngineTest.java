package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class SupplementAndNpcEngineTest {

    @Test
    public void testFishingRulesEngine() {
        JSONObject descriptor = FishingRulesEngine.descriptor();
        assertEquals("fishing_rules", descriptor.optString("id"));

        JSONArray tackles = FishingRulesEngine.tackleCatalog();
        assertTrue(tackles.length() >= 4);

        JSONArray baits = FishingRulesEngine.baitCatalog();
        assertTrue(baits.length() >= 4);

        JSONArray fish = FishingRulesEngine.fishCatalog();
        assertTrue(fish.length() >= 10);

        JSONObject attempt = FishingRulesEngine.resolveFishingAttempt("Rio", 16, "Vara de Teixo com Carretilha", "Minhoca Comum");
        assertNotNull(attempt);
        assertTrue(attempt.has("success"));
        assertTrue(attempt.has("total_check"));
        assertEquals(18, attempt.optInt("total_check"));
    }

    @Test
    public void testTavernBrothelCatalogEngine() {
        JSONObject desc = TavernBrothelCatalogEngine.descriptor();
        assertEquals("tavern_brothel_rules", desc.optString("id"));

        JSONArray drinks = TavernBrothelCatalogEngine.drinksCatalog();
        assertTrue(drinks.length() >= 6);

        JSONArray services = TavernBrothelCatalogEngine.lodgingAndServices();
        assertTrue(services.length() >= 5);

        JSONArray games = TavernBrothelCatalogEngine.gamesOfChance();
        assertTrue(games.length() >= 3);

        JSONObject est = TavernBrothelCatalogEngine.generateEstablishment("Taverna", "Alta");
        assertNotNull(est);
        assertTrue(est.optString("nome").length() > 0);
        assertTrue(est.has("cardapio_bebidas"));
    }

    @Test
    public void testPeculiarTowersEngine() {
        JSONObject desc = PeculiarTowersEngine.descriptor();
        assertEquals("peculiar_towers_rules", desc.optString("id"));

        JSONArray towers = PeculiarTowersEngine.listTowers();
        assertEquals(12, towers.length());

        JSONObject tower1 = PeculiarTowersEngine.getTower(1);
        assertEquals("Torre do Vidro Cantante", tower1.optString("nome"));

        JSONObject tower12 = PeculiarTowersEngine.getTower(12);
        assertEquals("Torre do Vórtice Abissal", tower12.optString("nome"));
    }

    @Test
    public void testMysticalTattoosEngine() {
        JSONObject desc = MysticalTattoosEngine.descriptor();
        assertEquals("mystical_tattoos_rules", desc.optString("id"));

        JSONArray slots = MysticalTattoosEngine.bodySlots();
        assertTrue(slots.length() >= 5);

        JSONArray tattoos = MysticalTattoosEngine.tattooCatalog();
        assertTrue(tattoos.length() >= 6);

        JSONObject appSuccess = MysticalTattoosEngine.applicationCheck("incomum", 15);
        assertTrue(appSuccess.optBoolean("success"));

        JSONObject appFail = MysticalTattoosEngine.applicationCheck("lendário", 10);
        assertFalse(appFail.optBoolean("success"));
    }

    @Test
    public void testCruelDungeonEngine() {
        JSONObject desc = CruelDungeonEngine.descriptor();
        assertEquals("cruel_dungeons_rules", desc.optString("id"));

        JSONArray traps = CruelDungeonEngine.trapCatalog();
        assertTrue(traps.length() >= 5);

        JSONObject room = CruelDungeonEngine.generateDungeonRoom(3);
        assertNotNull(room);
        assertTrue(room.has("armadilha_instalada"));
        assertTrue(room.has("atmosfera"));
    }

    @Test
    public void testShipRulesEngine() {
        JSONObject desc = ShipRulesEngine.descriptor();
        assertEquals("ship_rules", desc.optString("id"));

        JSONArray vessels = ShipRulesEngine.vesselCatalog();
        assertTrue(vessels.length() >= 5);

        JSONArray maneuvers = ShipRulesEngine.navalManeuvers();
        assertTrue(maneuvers.length() >= 4);

        JSONObject day = ShipRulesEngine.resolveNavalTravelDay("Caravela de Exploração", 1, 15);
        assertTrue(day.optInt("milhas_percorridas") > 0);
    }

    @Test
    public void testEvernuraAndSouthSeaEngines() {
        JSONObject evernuraDesc = EvernuraRegionalEngine.descriptor();
        assertEquals("evernura_regional_rules", evernuraDesc.optString("id"));
        assertTrue(EvernuraRegionalEngine.keyLocations().length() >= 4);
        assertTrue(EvernuraRegionalEngine.factions().length() >= 3);

        JSONObject southSeaDesc = SouthSeaRegionalEngine.descriptor();
        assertEquals("south_sea_rules", southSeaDesc.optString("id"));
        assertTrue(SouthSeaRegionalEngine.archipelagoIslands().length() >= 4);
        assertTrue(SouthSeaRegionalEngine.maritimeFactions().length() >= 3);
    }

    @Test
    public void testMerchantTradingEngine() {
        JSONObject desc = MerchantTradingEngine.descriptor();
        assertEquals("traders_merchants_rules", desc.optString("id"));

        JSONArray profiles = MerchantTradingEngine.merchantProfiles();
        assertTrue(profiles.length() >= 5);

        JSONObject goodHaggle = MerchantTradingEngine.resolveHaggleAttempt(20, 14, 100);
        assertTrue(goodHaggle.optBoolean("sucesso"));
        assertEquals(80, goodHaggle.optInt("preco_final_po"));

        JSONObject badHaggle = MerchantTradingEngine.resolveHaggleAttempt(5, 15, 100);
        assertFalse(badHaggle.optBoolean("sucesso"));
        assertEquals(115, badHaggle.optInt("preco_final_po"));
    }

    @Test
    public void testSupplementContentRegistryMatching() {
        JSONObject summary = SupplementContentRegistry.catalogSummary();
        assertNotNull(summary);
        JSONArray packs = summary.optJSONArray("compiled_packs");
        assertTrue(packs != null && packs.length() >= 10);

        JSONObject matchFishing = SupplementContentRegistry.matchAction("Quero pescar na beira do rio");
        assertTrue(matchFishing.optBoolean("matched"));

        JSONObject matchTavern = SupplementContentRegistry.matchAction("Vou até a taberna pedir uma cerveja");
        assertTrue(matchTavern.optBoolean("matched"));

        JSONObject matchTower = SupplementContentRegistry.matchAction("Explorar a torre peculiar do vidro cantante");
        assertTrue(matchTower.optBoolean("matched"));

        JSONObject matchTattoo = SupplementContentRegistry.matchAction("Aplicar uma tatuagem mística no braço");
        assertTrue(matchTattoo.optBoolean("matched"));

        JSONObject matchShip = SupplementContentRegistry.matchAction("Embarcar no navio em direção a alto mar");
        assertTrue(matchShip.optBoolean("matched"));

        JSONObject matchEvernura = SupplementContentRegistry.matchAction("Viajar pelas matas de evernura");
        assertTrue(matchEvernura.optBoolean("matched"));

        JSONObject matchSouthSea = SupplementContentRegistry.matchAction("Navegar pelo arquipélago de south sea");
        assertTrue(matchSouthSea.optBoolean("matched"));

        JSONObject matchMerchant = SupplementContentRegistry.matchAction("Regatear com o comerciante de poções");
        assertTrue(matchMerchant.optBoolean("matched"));
    }

    @Test
    public void testNpcBookCatalogEngineAndPromptGenerator() {
        String[] books = NpcBookCatalogEngine.getOriginBooks();
        assertTrue(books.length >= 4);

        String[] archetypes = NpcBookCatalogEngine.getArchetypes();
        assertTrue(archetypes.length >= 8);

        String[] ancestries = NpcBookCatalogEngine.getAncestries();
        assertTrue(ancestries.length >= 10);

        String[] quirks = NpcBookCatalogEngine.getQuirks();
        assertTrue(quirks.length >= 15);

        String[] secrets = NpcBookCatalogEngine.getSecrets();
        assertTrue(secrets.length >= 8);

        String[] hooks = NpcBookCatalogEngine.getAdventureHooks();
        assertTrue(hooks.length >= 8);

        JSONObject memory = NpcBookCatalogEngine.buildNpcMemory(
                "Capitão Barba-Rubra",
                "The Game Master's Book of NPCs",
                "Marinheiro, Corsário & Lobo do Mar",
                "Humano",
                "Confortável / Pequeno Burguês",
                "Bucaneiro Corsário",
                "Mastiga incessantemente folhas aromáticas de hortelã",
                "Esconde um mapa de tesouro de South Sea",
                "Pede ajuda para escoltar navio",
                "Capitão Veterano",
                "assets://npc/humano_corsario.png"
        );

        assertNotNull(memory);
        assertEquals("Capitão Barba-Rubra", memory.optString("name"));
        assertEquals(1, memory.optInt("active"));
        assertTrue(memory.optString("visual_description").contains("hortelã"));

        String prompt = NpcGeminiVisualPromptGenerator.buildPrompt(
                "Capitão Barba-Rubra",
                "Humano",
                "Confortável",
                "Bucaneiro Corsário",
                "Cicatriz no olho e capa de veludo",
                "Chapéu de capitão"
        );
        assertNotNull(prompt);
        assertTrue(prompt.contains("Humano"));
        assertTrue(prompt.contains("Bucaneiro Corsário"));
        assertTrue(prompt.contains("token portrait"));
    }
}
