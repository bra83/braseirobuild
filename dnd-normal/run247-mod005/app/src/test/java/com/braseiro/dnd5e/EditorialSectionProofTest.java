package com.braseiro.dnd5e;

import com.braseiro.dnd5e.adventure.AdventureModule;
import com.braseiro.dnd5e.adventure.AdventureNode;
import com.braseiro.dnd5e.adventure.AdventureTransition;
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

        assertEquals("Faerûn deve conter exatamente 45 entidades comprovadas", 45, faerun.size());
        assertEquals("Eberron deve conter exatamente 22 entidades comprovadas", 22, eberron.size());
        assertEquals("Ravenloft/Hungers deve conter exatamente 25 entidades comprovadas", 25, ravenloft.size());
        assertEquals("Lorwyn/Netheril/Arcana/XMM/XSAC deve conter exatamente 20 entidades", 20, lorwynArcana.size());

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

        assertEquals("Total de entidades canônicas deve ser 112", 112, total);
        assertEquals("Total de IDs únicos deve ser exatamente 112", 112, allIds.size());
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
        assertTrue("Deve conter Shadowdale", ids.contains("faerun_shadowdale"));
        assertTrue("Deve conter Cormanthor/Myth Drannor", ids.contains("faerun_cormanthor_myth_drannor"));

        // Ch 3 Icewind Dale
        assertTrue("Deve conter Icewind Dale", ids.contains("faerun_icewind_dale"));
        assertTrue("Deve conter Drizzt Do'Urden", ids.contains("faerun_drizzt_dourden"));

        // Ch 4 Calimshan
        assertTrue("Deve conter Calimshan", ids.contains("faerun_calimshan"));
        assertTrue("Deve conter facções de gênios", ids.contains("faerun_calimshan_genie_factions"));

        // Ch 5 Moonshae Isles
        assertTrue("Deve conter Ilhas Moonshae", ids.contains("faerun_moonshae_isles"));
        assertTrue("Deve conter Poços Lunares", ids.contains("faerun_moonshae_moonwells"));

        // Ch 6 Baldur's Gate, Neverwinter & Silverymoon
        assertTrue("Deve conter Baldur's Gate", ids.contains("faerun_baldurs_gate"));
        assertTrue("Deve conter Neverwinter", ids.contains("faerun_neverwinter"));
        assertTrue("Deve conter Silverymoon", ids.contains("faerun_silverymoon"));
        assertTrue("Deve conter Thay", ids.contains("faerun_thay"));

        // Ch 7 Lost Library of Lethchauntos
        assertTrue("Deve conter Biblioteca de Lethchauntos", ids.contains("faerun_lost_library_lethchauntos"));
        assertTrue("Deve conter Myrna Nor", ids.contains("faerun_myrna_nor"));
        assertTrue("Deve conter Powell the Owl Niccam", ids.contains("faerun_powell_the_owl_niccam"));

        // Protagonistas & Antagonistas de Aventuras
        assertTrue("Deve conter Lady Khara Sulwood", ids.contains("faerun_khara_sulwood"));
        assertTrue("Deve conter Bonegnasher", ids.contains("faerun_bonegnasher"));
        assertTrue("Deve conter Szass Tam", ids.contains("faerun_szass_tam"));

        // Ch 8 Magic Items & Relíquias de Aventuras
        assertTrue("Deve conter Cristal de Calimemnon", ids.contains("faerun_item_calimemnon_crystal"));
        assertTrue("Deve conter Coroa de Chifres", ids.contains("faerun_item_crown_of_horns"));
        assertTrue("Deve conter Tomo do Dragão", ids.contains("faerun_item_tome_dragon"));
        assertTrue("Deve conter Orbe de Damara", ids.contains("faerun_item_orb_of_damara"));
        assertTrue("Deve conter Grimório Nathlum", ids.contains("faerun_item_nathlum_grimoire"));

        // Ch 9 Bestiary
        assertTrue("Deve conter Fera de Malar", ids.contains("faerun_beast_of_malar"));
        assertTrue("Deve conter Progênie dos Três Mortos", ids.contains("faerun_dead_three_scion"));

        // Panteão Maior
        assertTrue("Deve conter Lathander", ids.contains("faerun_deity_lathander"));
        assertTrue("Deve conter Kelemvor", ids.contains("faerun_deity_kelemvor"));
        assertTrue("Deve conter Ilmater", ids.contains("faerun_deity_ilmater"));
        assertTrue("Deve conter Bane", ids.contains("faerun_deity_bane"));
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
        assertTrue("Deve conter Har'Akir", ids.contains("ravenloft_harakir_domain"));
        assertTrue("Deve conter Hazlan", ids.contains("ravenloft_hazlan_domain"));
        assertTrue("Deve conter Kalakeri", ids.contains("ravenloft_kalakeri_domain"));
        assertTrue("Deve conter Valachan", ids.contains("ravenloft_valachan_domain"));
        assertTrue("Deve conter Sithicus", ids.contains("ravenloft_sithicus_domain"));

        // RHW Entidades Cósmicas e Relíquias
        assertTrue("Deve conter Poderes Sombrios", ids.contains("ravenloft_dark_powers"));
        assertTrue("Deve conter Símbolo Sagrado de Ravenkind", ids.contains("ravenloft_item_holy_symbol_ravenkind"));
        assertTrue("Deve conter Espada Solar", ids.contains("ravenloft_item_sunsword"));

        // ABH
        assertTrue("Deve conter Palácio Szarr", ids.contains("astarion_cazador_palace"));
        assertTrue("Deve conter Sete Fomes Vampíricas", ids.contains("astarion_vampiric_hungers"));
        assertTrue("Deve conter Família de Crias de Cazador", ids.contains("astarion_cazador_spawn_family"));
        assertTrue("Deve conter Taverna Rat's Run", ids.contains("astarion_rats_run_tavern"));
        assertTrue("Deve conter Lorde Cazador Szarr NPC", ids.contains("astarion_cazador_szarr_npc"));
        assertTrue("Deve conter Mordida Empoderada", ids.contains("astarion_dhampir_empowered_bite"));
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
        assertTrue("Deve conter Magias Vivas do Cataclisma", ids.contains("au_living_spells_cataclysm"));
        assertTrue("Deve conter Relíquias de Evolução Mágica", ids.contains("au_evolving_items_relics"));
        assertTrue("Deve conter Penas de Escrita Arcana", ids.contains("au_item_dictation_quills"));
        assertTrue("Deve conter Lente de Cronos", ids.contains("au_relic_chronos_lens"));

        // XMM e XSAC
        assertTrue("Deve conter Ecologia dos Dragões 2025", ids.contains("xmm_dragon_ecology_2025"));
        assertTrue("Deve conter Adjudicação de Regras 2025", ids.contains("xsac_adjudication_rules"));
    }

    @Test
    public void testEberronEditorialCoverage() {
        List<NarrativeContentEngine.Entity> list = EberronCanonCatalog.all();
        Set<String> ids = new HashSet<>();
        for (NarrativeContentEngine.Entity e : list) ids.add(e.id);

        // Casas Dracossinaladas
        assertTrue("Deve conter Casa Cannith", ids.contains("eberron_house_cannith"));
        assertTrue("Deve conter Casa Lyrandar", ids.contains("eberron_house_lyrandar"));
        assertTrue("Deve conter Casa Orien", ids.contains("eberron_house_orien"));
        assertTrue("Deve conter Casa Deneith", ids.contains("eberron_house_deneith"));
        assertTrue("Deve conter Casa Kundarak", ids.contains("eberron_house_kundarak"));
        assertTrue("Deve conter Casa Sivis", ids.contains("eberron_house_sivis"));
        assertTrue("Deve conter Casa Jorasco", ids.contains("eberron_house_jorasco"));
        assertTrue("Deve conter Casa Ghallanda", ids.contains("eberron_house_ghallanda"));
        assertTrue("Deve conter Casa Tharashk", ids.contains("eberron_house_tharashk"));
        assertTrue("Deve conter Casa Vadalis", ids.contains("eberron_house_vadalis"));
        assertTrue("Deve conter Casa Medani", ids.contains("eberron_house_medani"));
        assertTrue("Deve conter Casas Phiarlan & Thuranni", ids.contains("eberron_house_phiarlan_thuranni"));

        // Locais, Itens e Monstros de Eberron
        assertTrue("Deve conter Sharn", ids.contains("eberron_sharn_city"));
        assertTrue("Deve conter Mournland", ids.contains("eberron_mournland_fog"));
        assertTrue("Deve conter Forjas de Eston", ids.contains("eberron_eston_creation_forge"));
        assertTrue("Deve conter Universidade Morgrave", ids.contains("eberron_morgrave_university"));
        assertTrue("Deve conter Senhor das Lâminas", ids.contains("eberron_lord_of_blades"));
        assertTrue("Deve conter Profecia Dracônica", ids.contains("eberron_draconic_prophecy"));
        assertTrue("Deve conter Fragmentos de Dragão", ids.contains("eberron_item_dragonshards"));
        assertTrue("Deve conter Argonnessen", ids.contains("eberron_argonnessen"));
        assertTrue("Deve conter Droaam", ids.contains("eberron_droaam"));
        assertTrue("Deve conter Colosso Forjado da Guerra", ids.contains("eberron_warforged_colossus"));
    }

    @Test
    public void testAdventuresHaveRichBranchingNodesAndConcreteEffects() {
        List<AdventureModule> allModules = FaerunAdventureModuleCatalog.all();
        assertEquals(5, allModules.size());

        for (AdventureModule mod : allModules) {
            assertNotNull(mod.getId());
            assertNotNull(mod.getTitle());
            assertTrue("Cada módulo deve ter pelo menos 4 nós", mod.getNodes().size() >= 4);

            int branchingNodesCount = 0;
            for (AdventureNode node : mod.getNodes().values()) {
                assertNotNull("Diretivas não podem ser nulas", node.getGmDirectives());
                assertNotNull("ID do nó não pode ser nulo", node.getId());

                if (!"completed".equalsIgnoreCase(node.getId()) && !"scene_4".equalsIgnoreCase(node.getId())) {
                    assertFalse("Nó não-terminal deve conter transições", node.getTransitions().isEmpty());
                }

                if (node.getTransitions().size() >= 2) {
                    branchingNodesCount++;
                    // Comprovar que as transições oferecem efeitos concretos distintos
                    AdventureTransition t1 = node.getTransitions().get(0);
                    AdventureTransition t2 = node.getTransitions().get(1);
                    assertNotNull("Efeitos da transição 1 não podem ser nulos", t1.getEffects());
                    assertNotNull("Efeitos da transição 2 não podem ser nulos", t2.getEffects());
                    assertNotEquals("IDs das transições devem ser distintos", t1.getId(), t2.getId());
                }

                if (node.getPresentNpcIds() != null) {
                    for (String npcId : node.getPresentNpcIds()) {
                        assertNotNull("ID de NPC presente não pode ser nulo", npcId);
                        assertFalse("ID de NPC presente não pode ser vazio", npcId.trim().isEmpty());
                    }
                }
            }

            assertTrue("Módulo " + mod.getId() + " deve possuir nós com opções ramificadas", branchingNodesCount >= 2);
        }
    }

    @Test
    public void testTombOfDamaraBranchingStateDivergence() {
        AdventureModule damara = FaerunAdventureModuleCatalog.byId("fraif_tomb_damara");
        assertNotNull(damara);

        // Caminho A: Assalto direto, resgate furtivo, destruição do espectro, entrega a Lady Sulwood
        com.braseiro.dnd5e.adventure.AdventureState pathA = new com.braseiro.dnd5e.adventure.AdventureState(damara.getRootNodeId());
        AdventureNode setupNode = damara.getNode("setup");
        AdventureTransition beginTr = setupNode.getTransitions().get(0);
        pathA.stageTransition("scene_1", beginTr.getEffects());
        pathA.commitTransaction();

        AdventureNode sc1 = damara.getNode("scene_1");
        AdventureTransition assaultTr = sc1.getTransitions().get(0); // direct_assault
        assertEquals("direct_assault", assaultTr.getId());
        pathA.stageTransition("scene_2", assaultTr.getEffects());
        pathA.commitTransaction();

        AdventureNode sc2 = damara.getNode("scene_2");
        AdventureTransition rescueTr = sc2.getTransitions().get(0); // rescue_direct
        assertEquals("rescue_direct", rescueTr.getId());
        pathA.stageTransition("scene_3", rescueTr.getEffects());
        pathA.commitTransaction();

        AdventureNode sc3 = damara.getNode("scene_3");
        AdventureTransition slayTr = sc3.getTransitions().get(1); // slay_specter
        assertEquals("slay_specter", slayTr.getId());
        pathA.stageTransition("scene_4", slayTr.getEffects());
        pathA.commitTransaction();

        AdventureNode sc4 = damara.getNode("scene_4");
        AdventureTransition cleanseTr = sc4.getTransitions().get(0); // cleanse_crypt
        assertEquals("cleanse_crypt", cleanseTr.getId());
        pathA.stageTransition("completed", cleanseTr.getEffects());
        pathA.commitTransaction();

        // Caminho B: Infiltração furtiva, suborno do capitão, banimento espectral, posse privada do orbe
        com.braseiro.dnd5e.adventure.AdventureState pathB = new com.braseiro.dnd5e.adventure.AdventureState(damara.getRootNodeId());
        pathB.stageTransition("scene_1", beginTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition stealthTr = sc1.getTransitions().get(1); // stealth_bypass
        assertEquals("stealth_bypass", stealthTr.getId());
        pathB.stageTransition("scene_2", stealthTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition parleyTr = sc2.getTransitions().get(1); // parley_hobgoblins
        assertEquals("parley_hobgoblins", parleyTr.getId());
        pathB.stageTransition("scene_3", parleyTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition banishTr = sc3.getTransitions().get(0); // banish_specter
        assertEquals("banish_specter", banishTr.getId());
        pathB.stageTransition("scene_4", banishTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition claimTr = sc4.getTransitions().get(1); // claim_orb_for_party
        assertEquals("claim_orb_for_party", claimTr.getId());
        pathB.stageTransition("completed", claimTr.getEffects());
        pathB.commitTransaction();

        // Comprovar divergência total e concreta de consequências nos dois caminhos
        assertTrue("Caminho A matou ghouls", pathA.hasFlag("damara_ghouls_slain"));
        assertFalse("Caminho B não matou ghouls", pathB.hasFlag("damara_ghouls_slain"));
        assertTrue("Caminho B evadiu ghouls", pathB.hasFlag("damara_ghouls_bypassed"));

        assertTrue("Caminho A resgatou diretamente", pathA.hasFlag("silverstars_rescued"));
        assertTrue("Caminho B subornou capitão", pathB.hasFlag("silverstars_ransomed"));

        assertTrue("Caminho A destruiu Bonegnasher", pathA.hasFlag("bonegnasher_destroyed"));
        assertTrue("Caminho B baniu Bonegnasher", pathB.hasFlag("bonegnasher_banished"));

        assertTrue("Caminho A purificou a tumba para Sulwood", pathA.hasFlag("damara_crypt_cleansed"));
        assertTrue("Caminho B reivindicou o orbe para o grupo", pathB.hasFlag("damara_orb_claimed_by_party"));
    }

    @Test
    public void testLostLibraryBranchingConsequencesDivergence() {
        AdventureModule library = FaerunAdventureModuleCatalog.byId("fraif_lost_library");
        assertNotNull(library);

        // Caminho A: Rastreamento cuidadoso, gazua silenciosa, negociação com Powell, doação a Myrna Nor
        com.braseiro.dnd5e.adventure.AdventureState pathA = new com.braseiro.dnd5e.adventure.AdventureState(library.getRootNodeId());
        AdventureTransition begin = library.getNode("setup").getTransitions().get(0);
        pathA.stageTransition("scene_1", begin.getEffects());
        pathA.commitTransaction();

        AdventureTransition trackTr = library.getNode("scene_1").getTransitions().get(0); // track_powell_trail
        pathA.stageTransition("scene_2", trackTr.getEffects());
        pathA.commitTransaction();

        AdventureTransition pickTr = library.getNode("scene_2").getTransitions().get(0); // pick_lock_quietly
        pathA.stageTransition("scene_3", pickTr.getEffects());
        pathA.commitTransaction();

        AdventureTransition negoTr = library.getNode("scene_3").getTransitions().get(0); // negotiate_powell
        pathA.stageTransition("scene_4", negoTr.getEffects());
        pathA.commitTransaction();

        AdventureTransition deliverTr = library.getNode("scene_4").getTransitions().get(0); // deliver_to_myrna
        pathA.stageTransition("completed", deliverTr.getEffects());
        pathA.commitTransaction();

        // Caminho B: Marcha rápida, arrombamento forçado, combate frontal, venda ao mercado negro
        com.braseiro.dnd5e.adventure.AdventureState pathB = new com.braseiro.dnd5e.adventure.AdventureState(library.getRootNodeId());
        pathB.stageTransition("scene_1", begin.getEffects());
        pathB.commitTransaction();

        AdventureTransition marchTr = library.getNode("scene_1").getTransitions().get(1); // hasty_march
        pathB.stageTransition("scene_2", marchTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition forceTr = library.getNode("scene_2").getTransitions().get(1); // force_door
        pathB.stageTransition("scene_3", forceTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition combatTr = library.getNode("scene_3").getTransitions().get(1); // combat_powell
        pathB.stageTransition("scene_4", combatTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition blackMarketTr = library.getNode("scene_4").getTransitions().get(1); // keep_poison_secrets
        pathB.stageTransition("completed", blackMarketTr.getEffects());
        pathB.commitTransaction();

        // Comprovar divergência
        assertTrue(pathA.hasFlag("powell_trail_tracked"));
        assertTrue(pathB.hasFlag("hasty_march_taken"));

        assertTrue(pathA.hasFlag("library_door_picked"));
        assertTrue(pathB.hasFlag("library_door_broken"));

        assertTrue(pathA.hasFlag("powell_surrendered"));
        assertTrue(pathB.hasFlag("powell_defeated_in_battle"));

        assertTrue(pathA.hasFlag("nathlum_given_to_myrna"));
        assertTrue(pathB.hasFlag("nathlum_kept_secret"));
    }

    @Test
    public void testHeirTrialBranchingConsequences() {
        AdventureModule heir = FaerunAdventureModuleCatalog.byId("fraif_heir_trial");
        assertNotNull(heir);

        // Caminho A: Endosso formal dos lordes, confronto direto com assassinos, selo verificado, coroação
        com.braseiro.dnd5e.adventure.AdventureState pathA = new com.braseiro.dnd5e.adventure.AdventureState(heir.getRootNodeId());
        AdventureTransition begin = heir.getNode("setup").getTransitions().get(0);
        pathA.stageTransition("scene_1", begin.getEffects());
        pathA.commitTransaction();

        AdventureTransition adv1 = heir.getNode("scene_1").getTransitions().get(0); // advance_1
        pathA.stageTransition("scene_2", adv1.getEffects());
        pathA.commitTransaction();

        AdventureTransition adv2 = heir.getNode("scene_2").getTransitions().get(0); // advance_2
        pathA.stageTransition("scene_3", adv2.getEffects());
        pathA.commitTransaction();

        AdventureTransition sealTr = heir.getNode("scene_3").getTransitions().get(0); // seal_verified
        pathA.stageTransition("scene_4", sealTr.getEffects());
        pathA.commitTransaction();

        AdventureTransition crownTr = heir.getNode("scene_4").getTransitions().get(0); // coronate_heir
        pathA.stageTransition("completed", crownTr.getEffects());
        pathA.commitTransaction();

        // Caminho B: Investigação secreta, pulo diplomático (scene_2 -> scene_4), exílio com os Harpistas
        com.braseiro.dnd5e.adventure.AdventureState pathB = new com.braseiro.dnd5e.adventure.AdventureState(heir.getRootNodeId());
        pathB.stageTransition("scene_1", begin.getEffects());
        pathB.commitTransaction();

        AdventureTransition covertTr = heir.getNode("scene_1").getTransitions().get(1); // covert_investigation
        pathB.stageTransition("scene_2", covertTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition diploTr = heir.getNode("scene_2").getTransitions().get(1); // diplomatic_branch -> scene_4
        assertEquals("scene_4", diploTr.getTargetNodeId());
        pathB.stageTransition("scene_4", diploTr.getEffects());
        pathB.commitTransaction();

        AdventureTransition exileTr = heir.getNode("scene_4").getTransitions().get(1); // exile_and_protect
        pathB.stageTransition("completed", exileTr.getEffects());
        pathB.commitTransaction();

        // Comprovar divergência total
        assertTrue(pathA.hasFlag("heir_claimed_openly"));
        assertTrue(pathB.hasFlag("heir_hidden_identity"));

        assertTrue(pathA.hasFlag("zhents_routed"));
        assertTrue(pathB.hasFlag("dock_ambush_evaded"));

        assertTrue(pathA.hasFlag("heir_crowned"));
        assertTrue(pathB.hasFlag("heir_secretly_exiled"));
    }
}
