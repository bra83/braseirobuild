package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Suite de Homologação Automatizada do GATE B:
 * Monster Manual 2025 (XMM) + Sage Advice Compendium 2025 (XSAC).
 * Valida:
 * 1. Instalação e catálogo completo dos 503 monstros compilados do XMM (sem dependência de PDF/RAG).
 * 2. Perfil e resolução manual de statblocks (sem geração oculta de rolagens RNG).
 * 3. Compilação e adjudicação dos 163 rulings canônicos do Sage Advice Compendium 2025.
 * 4. Operação fail-closed e integração com OfficialBookRuntimeGateway.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class GateBMonstersAndRulingsTest {

    @Test
    public void testXmmCompiledBestiaryCatalogAndInstall() throws Exception {
        // 1. Instalação do Bestiário XMM
        JSONObject installResult = Xmm2025CompiledBestiaryEngine.install();
        assertNotNull(installResult);
        assertEquals("XMM", installResult.optString("source", installResult.optString("source_id")));
        int installed = installResult.optInt("statblocks_installed", 0);
        assertEquals("Deve instalar exatamente os 503 monstros canônicos", 503, installed);
        assertEquals(0, installResult.optInt("blocked", -1));

        // 2. Cobertura do Bestiário
        JSONObject cov = Xmm2025CompiledBestiaryEngine.coverage();
        assertEquals(503, cov.optInt("expected"));
        assertEquals(503, cov.optInt("installed", cov.optInt("registered")));
        assertFalse("Não deve permitir auto-rolls", cov.optBoolean("auto_rolls", true));

        // 3. Catálogo de Monstros
        JSONObject catalog = Xmm2025CompiledBestiaryEngine.catalog();
        JSONArray ids = catalog.optJSONArray("names");
        assertNotNull(ids);
        assertEquals(503, ids.length());

        // 4. Verificação de criatura com CR baixo (Aarakocra Skirmisher - CR 0.25)
        JSONObject skirmisher = Xmm2025CompiledBestiaryEngine.statblock("Aarakocra Skirmisher");
        assertTrue(skirmisher.optBoolean("resolved"));
        JSONObject sData = skirmisher.getJSONObject("result");
        assertEquals(12, sData.optInt("armor_class"));
        assertEquals(11, sData.optInt("hit_points"));
        assertEquals(0.25, sData.optDouble("challenge_rating", 0), 0.01);

        // 5. Verificação de criatura com CR intermediário (Aboleth - CR 10)
        JSONObject aboleth = Xmm2025CompiledBestiaryEngine.statblock("Aboleth");
        assertTrue(aboleth.optBoolean("resolved"));
        JSONObject aData = aboleth.getJSONObject("result");
        assertEquals(17, aData.optInt("armor_class"));
        assertEquals(150, aData.optInt("hit_points"));
        JSONArray traits = aData.optJSONArray("traits");
        assertTrue("Aboleth deve possuir traits como Amphibious ou Mucus Cloud", traits != null && traits.length() >= 3);

        // 6. Verificação de criatura lendária (Adult Red Dragon - CR 17)
        JSONObject redDragon = Xmm2025CompiledBestiaryEngine.statblock("Adult Red Dragon");
        assertTrue(redDragon.optBoolean("resolved"));
        JSONObject rData = redDragon.getJSONObject("result");
        assertEquals(19, rData.optInt("armor_class"));
        assertEquals(256, rData.optInt("hit_points"));
        JSONArray legendary = rData.optJSONArray("special_actions");
        assertTrue("Dragão Vermelho Adulto deve possuir ações especiais ou lendárias compiladas", legendary != null && legendary.length() >= 2);

        // 7. Busca de criatura inexistente (Fail-Closed)
        JSONObject unknown = Xmm2025CompiledBestiaryEngine.statblock("Monstro_Inexistente_XYZ");
        assertFalse(unknown.optBoolean("resolved"));
    }

    @Test
    public void testSageAdviceCompendium2025Rulings() throws Exception {
        SageAdvice2025RulesEngine engine = new SageAdvice2025RulesEngine();

        // 1. Cobertura Geral
        JSONObject cov = engine.coverage();
        assertEquals("xsac", cov.optString("source_id"));
        assertEquals(163, cov.optInt("source_rulings"));
        assertEquals(163, cov.optInt("classified_rulings"));
        assertFalse("Não deve exigir RAG/LLM no caminho de regras", cov.optBoolean("rag_required", true));
        assertFalse("Não deve permitir auto rolls", cov.optBoolean("auto_rolls", true));

        // 2. Categorias Oficiais
        JSONArray cats = engine.categories();
        assertTrue("Deve cobrir pelo menos 10 categorias oficiais", cats.length() >= 10);

        // 3. Ruling Determinístico: D20 Test
        JSONObject d20Test = engine.resolve("d20_test_types", new JSONObject());
        assertTrue(d20Test.optBoolean("resolved"));
        assertEquals("resolved", d20Test.optString("status"));
        assertEquals("DETERMINISTIC", d20Test.optString("resolution_mode"));
        assertEquals("ability_checks", d20Test.optString("category"));

        // 4. Ruling Determinístico: Critical Check
        JSONObject critCheck = engine.resolve("ability_check_critical", new JSONObject());
        assertTrue(critCheck.optBoolean("resolved"));
        assertFalse(critCheck.optBoolean("critical"));

        // 5. Ruling Manual DM (onde o livro orienta decisão do narrador)
        JSONObject shortRest = engine.resolve("short_rest_daily_limit", new JSONObject());
        assertTrue(shortRest.optBoolean("resolved"));
        assertEquals("manual_dm", shortRest.optString("status"));
        assertEquals("MANUAL_DM", shortRest.optString("resolution_mode"));

        // 6. Ruling Inexistente (Fail-Closed)
        JSONObject blocked = engine.resolve("regra_inventada_sem_fonte", new JSONObject());
        assertFalse(blocked.optBoolean("resolved"));
        assertEquals("blocked", blocked.optString("status"));
        assertEquals("xsac_rule_not_registered", blocked.optString("reason"));
    }

    @Test
    public void testGatewayIntegrationBlockB() throws Exception {
        OfficialBookRuntimeGateway gateway = new OfficialBookRuntimeGateway(null);

        // 1. Gateway despacha XMM catalog
        JSONObject xmmCat = gateway.execute("xmm", "catalog", new JSONObject());
        assertTrue(xmmCat.optBoolean("executed"));
        JSONObject catResult = xmmCat.getJSONObject("result");
        assertEquals(503, catResult.optJSONArray("names").length());

        // 2. Gateway despacha XMM statblock específico
        JSONObject xmmStat = gateway.execute("xmm", "statblock", new JSONObject().put("name", "Allosaurus"));
        assertTrue(xmmStat.optBoolean("executed"));
        JSONObject allosaurusResult = xmmStat.getJSONObject("result");
        assertTrue(allosaurusResult.optBoolean("resolved"));
        JSONObject profile = allosaurusResult.getJSONObject("result");
        assertEquals("Allosaurus", profile.optString("id"));

        // 3. Gateway despacha XSAC ruling
        JSONObject xsacRule = gateway.execute("xsac", "ruling", new JSONObject().put("rule_id", "level_cap"));
        assertTrue(xsacRule.optBoolean("executed"));
        JSONObject capResult = xsacRule.getJSONObject("result");
        assertEquals(20, capResult.optInt("max_level"));

        // 4. Gateway bloqueia ação desconhecida em XMM e XSAC
        JSONObject xmmBlocked = gateway.execute("xmm", "unknown_action", new JSONObject());
        assertEquals("blocked", xmmBlocked.optString("status"));
        JSONObject xsacBlocked = gateway.execute("xsac", "unknown_action", new JSONObject());
        assertEquals("blocked", xsacBlocked.optString("status"));
    }
}
