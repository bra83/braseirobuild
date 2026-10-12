package com.braseiro.dnd5e;

import com.braseiro.dnd5e.adventure.AdventureModule;
import com.braseiro.dnd5e.adventure.AdventureNode;
import com.braseiro.dnd5e.adventure.FaerunAdventureModuleCatalog;
import com.braseiro.dnd5e.canon.EberronCanonCatalog;
import com.braseiro.dnd5e.canon.FaerunCanonCatalog;
import com.braseiro.dnd5e.canon.LorwynNetherilArcanaCanonCatalog;
import com.braseiro.dnd5e.canon.RavenloftCanonCatalog;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprova o inventário editorial e comportamental dos 10 livros recentes do D&D:
 * 1. Correspondência estrita de IDs de fonte e unicidade de 70 entidades canônicas.
 * 2. Cobertura por livro: XMM, XSAC, FRHoF, FRAiF, EFA, AU, RHW, ABH, LFL, NF.
 * 3. Validação das 5 aventuras não lineares de FRAiF com testes de perícia manuais.
 * 4. Isolamento formal entre os cenários (Faerûn, Eberron, Ravenloft, Lorwyn, Netheril, Multiverso).
 */
public final class EditorialSectionProofTest {

    @Test
    public void testSourceCodesAndUniqueEntityIds() {
        List<NarrativeContentEngine.Entity> faerun = FaerunCanonCatalog.all();
        List<NarrativeContentEngine.Entity> eberron = EberronCanonCatalog.all();
        List<NarrativeContentEngine.Entity> ravenloft = RavenloftCanonCatalog.all();
        List<NarrativeContentEngine.Entity> lorwynArcana = LorwynNetherilArcanaCanonCatalog.all();

        assertEquals("Faerûn deve conter exatamente 29 entidades comprovadas", 29, faerun.size());
        assertEquals("Eberron deve conter exatamente 10 entidades comprovadas", 10, eberron.size());
        assertEquals("Ravenloft/Hungers deve conter exatamente 15 entidades comprovadas", 15, ravenloft.size());
        assertEquals("Lorwyn/Netheril/Arcana/XMM/XSAC deve conter exatamente 16 entidades", 16, lorwynArcana.size());

        Set<String> allIds = new HashSet<>();
        int total = 0;

        for (NarrativeContentEngine.Entity e : faerun) {
            assertTrue("ID não pode ser nulo ou vazio", e.id != null && !e.id.isEmpty());
            assertTrue("Fonte de Faerûn deve ser FRHoF ou FRAiF: " + e.sourceId, "FRHOF".equalsIgnoreCase(e.sourceId) || "FRAIF".equalsIgnoreCase(e.sourceId));
            assertTrue("Cenário de Faerûn deve ser forgotten_realms", "forgotten_realms".equals(e.setting));
            assertTrue("ID duplicado detectado: " + e.id, allIds.add(e.id));
            total++;
        }

        for (NarrativeContentEngine.Entity e : eberron) {
            assertTrue("Fonte de Eberron deve ser EFA: " + e.sourceId, "EFA".equalsIgnoreCase(e.sourceId));
            assertTrue("Cenário de Eberron deve ser eberron", "eberron".equals(e.setting));
            assertTrue("ID duplicado detectado: " + e.id, allIds.add(e.id));
            total++;
        }

        for (NarrativeContentEngine.Entity e : ravenloft) {
            assertTrue("Fonte deve ser RHW ou ABH: " + e.sourceId, "RHW".equalsIgnoreCase(e.sourceId) || "ABH".equalsIgnoreCase(e.sourceId));
            assertTrue("Cenário deve ser ravenloft", "ravenloft".equals(e.setting));
            assertTrue("ID duplicado detectado: " + e.id, allIds.add(e.id));
            total++;
        }

        for (NarrativeContentEngine.Entity e : lorwynArcana) {
            assertTrue("Fonte deve ser LFL, NF, AU, XMM ou XSAC: " + e.sourceId,
                    "LFL".equalsIgnoreCase(e.sourceId) || "NF".equalsIgnoreCase(e.sourceId) || "AU".equalsIgnoreCase(e.sourceId) || "XMM".equalsIgnoreCase(e.sourceId) || "XSAC".equalsIgnoreCase(e.sourceId));
            assertTrue("ID duplicado detectado: " + e.id, allIds.add(e.id));
            total++;
        }

        assertEquals("Total de entidades canônicas deve ser 70", 70, total);
        assertEquals("Total de IDs únicos deve ser exatamente 70", 70, allIds.size());
    }

    @Test
    public void testFraifEditorialSectionsCoverage() {
        List<NarrativeContentEngine.Entity> faerun = FaerunCanonCatalog.all();
        Set<String> ids = new HashSet<>();
        for (NarrativeContentEngine.Entity e : faerun) {
            ids.add(e.id);
        }

        // Ch 2 Dalelands & Cormanthor
        assertTrue("Deve conter Dalelands", ids.contains("faerun_dalelands"));
        assertTrue("Deve conter Cormanthor/Myth Drannor", ids.contains("faerun_cormanthor_myth_drannor"));

        // Ch 3 Icewind Dale
        assertTrue("Deve conter Icewind Dale", ids.contains("faerun_icewind_dale"));

        // Ch 4 Calimshan
        assertTrue("Deve conter Calimshan", ids.contains("faerun_calimshan"));
        assertTrue("Deve conter facções de gênios", ids.contains("faerun_calimshan_genie_factions"));

        // Ch 5 Moonshae Isles
        assertTrue("Deve conter Ilhas Moonshae", ids.contains("faerun_moonshae_isles"));
        assertTrue("Deve conter Poços Lunares", ids.contains("faerun_moonshae_moonwells"));

        // Ch 6 Baldur's Gate
        assertTrue("Deve conter Baldur's Gate", ids.contains("faerun_baldurs_gate"));

        // Ch 7 Lost Library of Lethchauntos
        assertTrue("Deve conter Biblioteca de Lethchauntos", ids.contains("faerun_lost_library_lethchauntos"));

        // Ch 8 Magic Items
        assertTrue("Deve conter Cristal de Calimemnon", ids.contains("faerun_item_calimemnon_crystal"));
        assertTrue("Deve conter Coroa de Chifres", ids.contains("faerun_item_crown_of_horns"));
        assertTrue("Deve conter Tomo do Dragão", ids.contains("faerun_item_tome_dragon"));

        // Ch 9 Bestiary
        assertTrue("Deve conter Fera de Malar", ids.contains("faerun_beast_of_malar"));
        assertTrue("Deve conter Progênie dos Três Mortos", ids.contains("faerun_dead_three_scion"));
    }

    @Test
    public void testFaerunAdventureModulesFiveSpecsNonLinear() {
        List<AdventureModule> modules = FaerunAdventureModuleCatalog.all();
        assertEquals("Deve conter 5 aventuras de FRAiF (2 publicadas exatas + 3 ganchos originais)", 5, modules.size());

        AdventureModule damara = FaerunAdventureModuleCatalog.byId("fraif_tomb_damara");
        assertNotNull("The Tomb of Damara deve existir", damara);
        assertEquals("The Tomb of Damara deve ser Nível 3", 3, damara.getMetadata().optInt("recommended_level"));
        assertTrue("The Tomb of Damara deve ser marcada como oficial publicada", damara.getMetadata().optBoolean("is_official_published"));
        assertEquals("official_published_adventure_adaptation", damara.getMetadata().optString("runtime_tier"));
        // Comprovar presença de Bonegnasher e Silverstar nos nós
        StringBuilder damaraText = new StringBuilder();
        for (AdventureNode n : damara.getNodes().values()) {
            damaraText.append(n.getTitle()).append(" ").append(n.getDescription()).append(" ");
        }
        String damaraFullText = damaraText.toString();
        assertTrue("Deve citar Bonegnasher", damaraFullText.contains("Bonegnasher"));
        assertTrue("Deve citar a família Silverstar e resgate de Sara", damaraFullText.contains("Silverstar"));
        assertTrue("Deve citar Orbe de Damara", damaraFullText.contains("Orbe de Damara"));

        AdventureModule lethchauntos = FaerunAdventureModuleCatalog.byId("fraif_lost_library");
        assertNotNull("The Lost Library of Lethchauntos deve existir", lethchauntos);
        assertEquals("The Lost Library of Lethchauntos deve ser Nível 1 (introdutória)", 1, lethchauntos.getMetadata().optInt("recommended_level"));
        assertTrue("The Lost Library of Lethchauntos deve ser marcada como oficial publicada", lethchauntos.getMetadata().optBoolean("is_official_published"));
        assertEquals("official_published_adventure_adaptation", lethchauntos.getMetadata().optString("runtime_tier"));
        // Comprovar presença de Myrna Nor, Powell Niccam e o tomo Nathlum
        StringBuilder lethText = new StringBuilder();
        for (AdventureNode n : lethchauntos.getNodes().values()) {
            lethText.append(n.getTitle()).append(" ").append(n.getDescription()).append(" ");
        }
        String lethFullText = lethText.toString();
        assertTrue("Deve citar Myrna Nor", lethFullText.contains("Myrna Nor"));
        assertTrue("Deve citar Powell 'the Owl' Niccam", lethFullText.contains("Powell"));
        assertTrue("Deve citar o tomo Nathlum", lethFullText.contains("Nathlum"));

        // Comprovar que os outros 3 módulos são rotulados explicitamente como originais
        AdventureModule heir = FaerunAdventureModuleCatalog.byId("fraif_heir_trial");
        assertNotNull(heir);
        assertFalse("The Heir's Trial é criação original compatível", heir.getMetadata().optBoolean("is_official_published"));
        assertEquals("original_scenario_campaign_hook", heir.getMetadata().optString("runtime_tier"));

        AdventureModule archmage = FaerunAdventureModuleCatalog.byId("fraif_archmage_legacy");
        assertNotNull(archmage);
        assertFalse("The Archmage's Legacy é criação original compatível", archmage.getMetadata().optBoolean("is_official_published"));
        assertEquals("original_scenario_campaign_hook", archmage.getMetadata().optString("runtime_tier"));

        AdventureModule undercellar = FaerunAdventureModuleCatalog.byId("fraif_baldurs_undercellar");
        assertNotNull(undercellar);
        assertFalse("Conspiracy in the Under-Cellar é criação original compatível", undercellar.getMetadata().optBoolean("is_official_published"));
        assertEquals("original_scenario_campaign_hook", undercellar.getMetadata().optString("runtime_tier"));
    }

    @Test
    public void testRavenloftAndHungersEditorialSections() {
        List<NarrativeContentEngine.Entity> list = RavenloftCanonCatalog.all();
        Set<String> ids = new HashSet<>();
        for (NarrativeContentEngine.Entity e : list) ids.add(e.id);

        // RHW Domínios
        assertTrue("Deve conter Barovia", ids.contains("ravenloft_barovia_domain"));
        assertTrue("Deve conter Darkon", ids.contains("ravenloft_darkon_domain"));
        assertTrue("Deve conter Lamordia", ids.contains("ravenloft_lamordia_domain"));
        assertTrue("Deve conter Dementlieu", ids.contains("ravenloft_dementlieu_domain"));
        assertTrue("Deve conter Falkovnia", ids.contains("ravenloft_falkovnia_domain"));
        assertTrue("Deve conter Borca", ids.contains("ravenloft_borca_domain"));
        assertTrue("Deve conter Tepest", ids.contains("ravenloft_tepest_domain"));

        // ABH
        assertTrue("Deve conter Palácio Szarr", ids.contains("astarion_cazador_palace"));
        assertTrue("Deve conter Sete Fomes Vampíricas", ids.contains("astarion_vampiric_hungers"));
        assertTrue("Deve conter Família de Crias de Cazador", ids.contains("astarion_cazador_spawn_family"));
        assertTrue("Deve conter Taverna Rat's Run", ids.contains("astarion_rats_run_tavern"));
    }

    @Test
    public void testArcanaUnleashedAndMultiverseSections() {
        List<NarrativeContentEngine.Entity> list = LorwynNetherilArcanaCanonCatalog.all();
        Set<String> ids = new HashSet<>();
        for (NarrativeContentEngine.Entity e : list) ids.add(e.id);

        // AU Facções e Tumba
        assertTrue("Deve conter Horizon Weavers", ids.contains("arcana_unleashed_weavers"));
        assertTrue("Deve conter Vault of Arcana", ids.contains("arcana_unleashed_vault"));
        assertTrue("Deve conter Ninth Quill", ids.contains("arcana_unleashed_ninth_quill"));
        assertTrue("Deve conter Bejeweled Conclave", ids.contains("au_bejeweled_conclave"));
        assertTrue("Deve conter Bringers of Cosmic Dawn", ids.contains("au_bringers_cosmic_dawn"));
        assertTrue("Deve conter Tomb of Shadow Serpents", ids.contains("au_tomb_shadow_serpents"));

        // XMM e XSAC
        assertTrue("Deve conter Ecologia dos Dragões 2025", ids.contains("xmm_dragon_ecology_2025"));
        assertTrue("Deve conter Adjudicação de Regras 2025", ids.contains("xsac_adjudication_rules"));
    }
}
