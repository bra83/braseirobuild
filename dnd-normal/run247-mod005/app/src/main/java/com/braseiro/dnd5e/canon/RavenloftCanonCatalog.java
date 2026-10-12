package com.braseiro.dnd5e.canon;

import com.braseiro.dnd5e.NarrativeContentEngine.Entity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo canônico estruturado dos Domínios do Pavor de Ravenloft:
 * - Ravenloft: The Horrors Within (RHW)
 * - Astarion's Book of Hungers (ABH)
 *
 * Abrange:
 * - Domínios do Pavor: Barovia, Borca, Darkon, Dementlieu, Falkovnia, Har'Akir, Hazlan, Lamordia, Mordent, Tepest, Valachan, Kalakeri, Sithicus.
 * - Lordes das Trevas (Darklords): Strahd von Zarovich, Ivana Boritsi, Azalin Rex, Saidra d'Honaire, Vladeska Drakov, Ankhtepot, Viktra Mordenheim, Lord Soth, Cazador Szarr.
 * - Caçadores & Ordens: Rudolph van Richten, Ez d'Avenir, Irmãs Weathermay-Foxgrove, Guardiões da Pena (Keepers of the Feather).
 * - Sete Fomes Vampíricas (ABH) & Regras de Terror e Isolamento.
 */
public final class RavenloftCanonCatalog {
    private RavenloftCanonCatalog() {}

    public static List<Entity> all() {
        List<Entity> list = new ArrayList<>();

        // 1. Barovia e Strahd
        Map<String, String> strahdRel = new HashMap<>();
        strahdRel.put("nemesis", "ravenloft_van_richten");
        strahdRel.put("opponents", "ravenloft_keepers_feather");
        strahdRel.put("domain", "ravenloft_barovia_domain");
        list.add(new Entity(
                "ravenloft_strahd", "RHW", "ravenloft", "735 BC", "npc",
                "Conde Strahd von Zarovich", "Count Strahd von Zarovich",
                "O primeiro lorde vampiro e Darklord imortal do vale de Barovia, senhor de Castle Ravenloft.",
                Arrays.asList(
                        "Comanda Barovia com autoridade suprema e controle mágico sobre as criaturas da noite (lobos, morcegos e espectros).",
                        "Prisioneiro eterno de sua maldição pelas Poderes Sombrias (Dark Powers), condenado a nunca possuir a alma reencarnada de Tatyana."
                ),
                Arrays.asList(
                        "O Coração do Pesar (Heart of Sorrow) no castelo absorve ferimentos massivos que seriam infligidos a ele.",
                        "Possui espiões Vistani leais que monitoram todas as carruagens e viajantes que cruzam os portões de névoa de Barovia."
                ),
                Arrays.asList("ravenloft", "strahd", "vampire", "darklord", "barovia", "horror"),
                strahdRel, "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_barovia_domain", "RHW", "ravenloft", "735 BC", "location",
                "Domínio de Barovia", "Domain of Barovia",
                "Vale sombrio cercado por montanhas escarpadas e pinheiros fúnebres, envolto em névoas sufocantes que impedem a saída.",
                Arrays.asList(
                        "Composto pela Vila de Barovia, a cidade fortificada de Vallaki e o vilarejo alpino de Krezk.",
                        "O céu é perpetuamente cinzento e os habitantes vivem sob o terror de noites sem lua."
                ),
                Arrays.asList(
                        "O Templo de Âmbar (Amber Temple) nas montanhas abriga os sarcófagos onde deuses e vestígios banidos sussurram Dádivas Sombrias.",
                        "As águas da piscina sagrada de Krezk preservam a única bênção de bênção solar intocada pelo conde."
                ),
                Arrays.asList("ravenloft", "barovia", "domain_of_dread", "castle_ravenloft", "vallaki"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_castle_ravenloft", "RHW", "ravenloft", "735 BC", "location",
                "Castelo Ravenloft", "Castle Ravenloft",
                "Fortaleza gótica colossal erguida sobre um penhasco de mil pés sobre o vale de Barovia.",
                Arrays.asList(
                        "Lar ancestral dos von Zarovich, repleto de salões assombrados, espirais pontiagudas e catacumbas labirínticas.",
                        "Contém o túmulo de Sergei, a cripta dos pais de Strahd e salões de baile congelados no tempo."
                ),
                Arrays.asList(
                        "Um portal nos calabouços inferiores conecta-se diretamente a sepulcros arcanos esquecidos em Faerûn.",
                        "O Tomo de Strahd, caso decifrado nas bibliotecas do castelo, revela a fraqueza psicológica definitiva do vampiro."
                ),
                Arrays.asList("ravenloft", "castle_ravenloft", "dungeon", "gothic", "vampire_lair"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        // 2. Outros Grandes Domínios do Pavor
        list.add(new Entity(
                "ravenloft_darkon_domain", "RHW", "ravenloft", "735 BC", "location",
                "Domínio de Darkon", "Domain of Darkon",
                "O maior domínio de Ravenloft, um reino de necromancia outrora governado pelo lich supremo Azalin Rex.",
                Arrays.asList(
                        "Após o desaparecimento de Azalin na Hora da Ascensão, o domínio se fraturou em regiões instáveis disputadas por barões mortos-vivos.",
                        "Qualquer forasteiro que permaneça em Darkon por mais de alguns meses perde suas memórias de seu plano de origem."
                ),
                Arrays.asList(
                        "A fortaleza de Castle Avernus guarda a máquina arcana inacabada de Azalin para escapar dos Domínios do Pavor.",
                        "A Ordem Secreta dos Kargat manipula o comércio e espiona os pretendentes ao trono vacante."
                ),
                Arrays.asList("ravenloft", "darkon", "azalin_rex", "necromancy", "memory_loss"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_lamordia_domain", "RHW", "ravenloft", "735 BC", "location",
                "Domínio de Lamordia", "Domain of Lamordia",
                "Região costeira gélida e tempestuosa movida pela ciência amoral, anatomia macabra e tecnologia a vapor.",
                Arrays.asList(
                        "Lar da Baronesa Dra. Viktra Mordenheim, cientista que desafiou a morte criando vida a partir de tecidos reconstruídos.",
                        "Cidades envoltas em fumaça de carvão e hospitais experimentais onde transplantes misteriosos ocorrem sem anestesia."
                ),
                Arrays.asList(
                        "Viktra busca obsessivamente recuperar Elise, sua criação fujona que carrega o Coração Inquebrável (Unbreakable Heart).",
                        "Criaturas de retalhos (Flesh Golems) conscientes patrulham as montanhas geladas buscando abrigo humano."
                ),
                Arrays.asList("ravenloft", "lamordia", "mordenheim", "frankenstein_horror", "flesh_golem"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_dementlieu_domain", "RHW", "ravenloft", "735 BC", "location",
                "Domínio de Dementlieu", "Domain of Dementlieu",
                "Cenário de horror psicológico disfarçado por opulência, bailes de máscaras deslumbrantes e mentiras teatrais.",
                Arrays.asList(
                        "A capital Port-a-Lucine aparenta ser o centro da sofisticação e da alta sociedade iluminada.",
                        "Governada pela Darklord Saidra d'Honaire, aprisionada na compulsão de manter a farsa perpétua do Grande Baile de Máscaras."
                ),
                Arrays.asList(
                        "Aqueles cujas mentiras e fraudes são desmascaradas no baile são transformados em manequins de cera vivos pelos reflexos nos espelhos.",
                        "Os esgotos sob os palácios acolhem carniceiros e cidadãos rejeitados que planejam incendiar as óperas da elite."
                ),
                Arrays.asList("ravenloft", "dementlieu", "psychological_horror", "masquerade", "vanity"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_falkovnia_domain", "RHW", "ravenloft", "735 BC", "location",
                "Domínio de Falkovnia", "Domain of Falkovnia",
                "Reino de terror militarizado e sobrevivência sob cerco perpétuo de hordas infinitas de mortos-vivos famintos.",
                Arrays.asList(
                        "Comandada pela tirana Vladeska Drakov e seus implacáveis soldados Falcões (Talons).",
                        "A cada lua cheia, ondas avassaladoras de zumbis colapsam as paliçadas e os sobreviventes devem queimar cidades inteiras para recuar."
                ),
                Arrays.asList(
                        "Drakov executa sumariamente qualquer refugiado que apresente sintomas de febre de mordida ou fraqueza moral.",
                        "Túneis secretos sob a cidade de Lekar guardam provisões para evacuação exclusiva do alto escalão militar."
                ),
                Arrays.asList("ravenloft", "falkovnia", "zombie_siege", "tyranny", "survival_horror"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // 3. Aliados e Caçadores Notáveis
        list.add(new Entity(
                "ravenloft_van_richten", "RHW", "ravenloft", "735 BC", "npc",
                "Dr. Rudolph van Richten", "Dr. Rudolph van Richten",
                "O mais célebre erudito e caçador de monstros dos Domínios do Pavor, autor de guias sobre vampiros e licantropos.",
                Arrays.asList(
                        "Dedicou sua vida idosa a desvendar os pontos fracos das criaturas sombrias após a trágica perda de seu filho.",
                        "Atua disfarçado como mestre de cerimônias ou herborista itinerante para escapar da vigilância de Strahd."
                ),
                Arrays.asList(
                        "Carrega uma maldição vistani dolorosa: qualquer aliado que se aproxime dele por muito tempo é atraído para um destino funesto.",
                        "Possui um diário cifrado detalhando os segredos de invocação e destruição do Símbolo Sagrado de Ravenkind."
                ),
                Arrays.asList("ravenloft", "van_richten", "monster_hunter", "scholar", "tragedy"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_keepers_feather", "RHW", "ravenloft", "735 BC", "faction",
                "Os Guardiões da Pena", "The Keepers of the Feather",
                "Ordem secreta de corvos-licantropos (wereravens) barovianos que se opõem silenciosamente à tirania de Strahd.",
                Arrays.asList(
                        "Operam através da Estalagem Água Azul (Blue Water Inn) em Vallaki e do vinhedo Mago dos Vinhos (Wizard of Wines).",
                        "Fornecem abrigo, avisos de emboscadas e mensagens urgentes a aventureiros que demonstrem nobreza de coração."
                ),
                Arrays.asList(
                        "As três gemas mágicas que fertilizam as uvas barovianas foram forjadas por druidas ancestrais antes da chegada de Strahd.",
                        "Reconhecem-se mutuamente através de penas negras entrelaçadas em seus cintos e marcas de asas discretas."
                ),
                Arrays.asList("ravenloft", "keepers_of_the_feather", "wereravens", "resistance", "allies"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        // 4. Astarion's Book of Hungers (ABH)
        list.add(new Entity(
                "astarion_cazador_palace", "ABH", "ravenloft", "present", "location",
                "O Palácio Szarr (Cazador Szarr)", "Szarr Palace",
                "Mansão aristocrática monumental em Baldur's Gate e seu reflexo demiplanar no Pavor, centro do clã vampírico de Cazador.",
                Arrays.asList(
                        "Arquitetura nobre opulenta ocultando criptas e salões de sangue nos níveis inferiores.",
                        "Sete mil almas de servos gerados por vampiros foram marcadas ritualmente com runas infernais de sacrifício."
                ),
                Arrays.asList(
                        "O Ritual de Ascensão Vampírica permite a um vampiro andar à luz do dia sem sofrer dano solar e alimentar-se sem limites.",
                        "A taverna Rat's Run funciona como entreposto de coleta de almas e refúgio clandestino de servos rebeldes de Astarion."
                ),
                Arrays.asList("abh", "astarion", "cazador", "vampire_ascension", "szarr_palace"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "astarion_vampiric_hungers", "ABH", "ravenloft", "present", "misc",
                "As Sete Fomes Vampíricas", "The Seven Vampiric Hungers",
                "A doutrina e os apetites incontroláveis que regem os vampiros e dhampirs descritos no Livro das Fomes de Astarion.",
                Arrays.asList(
                        "A Fome não é apenas necessidade física de sangue, mas obsessão voraz por poder, beleza, segredos, carne, medos, almas e controle.",
                        "Heróis dhampirs aprendem a canalizar sua mordida fortalecida (Empowered Bite) para recuperar vigor e inspirar ataques."
                ),
                Arrays.asList(
                        "Vampiros que jejuam por longos períodos entram em frenesi assassino incontrolável atacando aliados e inocentes.",
                        "Certos pactos infernais com diabos colhedores (Harvester Devils) prometem saciar a sede vampírica em troca de almas mortais."
                ),
                Arrays.asList("abh", "hungers", "dhampir", "vampirism", "rules_mechanic"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_borca_domain", "RHW", "ravenloft", "present", "location",
                "Domínio de Borca", "Domain of Borca",
                "Terra de decadência aristocrática, bailes suntuosos e venenos mortais governada por Ivana Boritsi e Ivan Dilisnya.",
                Arrays.asList(
                        "Palácios rococó erguem-se sobre vilas miseráveis onde a cortesia social esconde conspirações de assassinato.",
                        "Ervas venenosas raras são cultivadas nas estufas dos nobres para eliminar rivais de herança."
                ),
                Arrays.asList(
                        "O Beijo de Ivana é um veneno indetectável que transmuta sangue nobre em cristal corrosivo.",
                        "Espiões de Borca infiltram redes de comércio interdomínios buscando receitas de venenos de Thay."
                ),
                Arrays.asList("ravenloft", "borca", "ivana_boritsi", "dilisnya", "poisons", "domain"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "ravenloft_tepest_domain", "RHW", "ravenloft", "present", "location",
                "Domínio de Tepest", "Domain of Tepest",
                "Vale rural aparentemente bucólico assombrado por bruxas feéricas e festivais macabros de sacrifício.",
                Arrays.asList(
                        "A vila de Viktal celebra o dízimo da colheita sob cantos folclóricos e danças em torno da fogueira.",
                        "Florestas sussurrantes são habitadas por criaturas feéricas distorcidas pelas brumas de Ravenloft."
                ),
                Arrays.asList(
                        "Mãe Lorinda devora a juventude dos aldeões oferecidos como tributo pelas próprias famílias.",
                        "Bonecos de palha deixados nas portas durante o solstício servem como âncoras para o despertar de espantalhos assassinos."
                ),
                Arrays.asList("ravenloft", "tepest", "lorinda", "hags", "folk_horror", "domain"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "astarion_cazador_spawn_family", "ABH", "ravenloft", "present", "faction",
                "Progênie de Cazador Szarr", "Cazador's Vampire Spawn",
                "Os 'irmãos' vampíricos de Astarion: Leon, Petras, Aurelia, Dalyria, Violet e Yousen, submetidos à tirania de Cazador.",
                Arrays.asList(
                        "Crias vampíricas mantidas sob servidão estrita através de laços de sangue e crueldade mental.",
                        "Enviados para seduzir almas nos salões nobres de Baldur's Gate para abastecer as masmorras do palácio."
                ),
                Arrays.asList(
                        "Leon e Dalyria conspiram para sabotar os planos de ascensão de Cazador e escapar com vida.",
                        "Cada cria carrega cicatrizes arcanas gravadas nas costas que compõem o círculo do ritual de Mefistófeles."
                ),
                Arrays.asList("abh", "vampire_spawn", "cazador_family", "astarion", "baldurs_gate"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "astarion_rats_run_tavern", "ABH", "ravenloft", "present", "location",
                "Taverna Rat's Run", "Rat's Run Tavern & Inn",
                "Taverna decadente nos becos inferiores onde aventureiros e criaturas da noite negociam segredos.",
                Arrays.asList(
                        "Local de encontro de caçadores de recompensas, ladinos e informantes de Astarion.",
                        "Serve hidromel escuro e ensopados enquanto negócios ilegais são selados nos porões."
                ),
                Arrays.asList(
                        "Um alçapão sob o barril principal conecta diretamente aos esgotos da Cidade Baixa e ao palácio Szarr.",
                        "O taverneiro é um ghoul disfarçado leal a Astarion que recolhe rumores para o Mestre."
                ),
                Arrays.asList("abh", "rats_run", "tavern", "astarion", "safehouse"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        return Collections.unmodifiableList(list);
    }
}
