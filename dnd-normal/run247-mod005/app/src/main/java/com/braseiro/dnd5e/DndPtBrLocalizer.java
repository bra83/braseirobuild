package com.braseiro.dnd5e;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normaliza fichas importadas para PT-BR sem alterar nomes próprios livres.
 * A tradução é aplicada apenas aos campos mecânicos da ficha.
 */
public final class DndPtBrLocalizer {
    private static final LinkedHashMap<String,String> TERMS = new LinkedHashMap<String,String>();

    static {
        // Cabeçalhos e blocos mecânicos.
        add("Volo's Complete Subrace Handbook", "Manual Completo de Sub-raças de Volo");
        add("Volo Subrace Handbook", "Manual de Sub-raças de Volo");
        add("Subrace Handbook", "Manual de Sub-raças");
        add("INVENTORY AND EQUIPMENT", "INVENTÁRIO E EQUIPAMENTO");
        add("PREPARED SPELLS", "MAGIAS PREPARADAS");
        add("SPELLS PREPARED", "MAGIAS PREPARADAS");
        add("SPELLS KNOWN", "MAGIAS CONHECIDAS");
        add("PASSIVE SENSES", "SENTIDOS PASSIVOS");
        add("SAVING THROWS", "SALVAGUARDAS");
        add("SPELLCASTING", "CONJURAÇÃO");
        add("PROFICIENCIES", "PROFICIÊNCIAS");
        add("CANTRIPS", "TRUQUES");
        add("CANTRIP", "TRUQUE");
        add("FEATURES AND TRAITS", "RECURSOS E TRAÇOS");
        add("FEATURES", "RECURSOS E TRAÇOS");
        add("ATTACKS", "ATAQUES");
        add("SKILLS", "PERÍCIAS");
        add("LANGUAGES", "IDIOMAS");
        add("TOOLS", "FERRAMENTAS");

        // Magias mais frequentes no D&D 5e e no catálogo do aplicativo.
        add("Tasha's Hideous Laughter", "Riso Histérico de Tasha");
        add("Tasha’s Hideous Laughter", "Riso Histérico de Tasha");
        add("Protection from Evil and Good", "Proteção contra o Bem e o Mal");
        add("Pass without Trace", "Passar sem Deixar Rastros");
        add("Dissonant Whispers", "Sussurros Dissonantes");
        add("Vicious Mockery", "Zombaria Viciosa");
        add("Spiritual Weapon", "Arma Espiritual");
        add("Armor of Agathys", "Armadura de Agathys");
        add("Absorb Elements", "Absorver Elementos");
        add("Speak with Animals", "Falar com Animais");
        add("Hunter's Mark", "Marca do Caçador");
        add("Hunter’s Mark", "Marca do Caçador");
        add("Shield of Faith", "Escudo da Fé");
        add("Minor Illusion", "Ilusão Menor");
        add("Healing Word", "Palavra Curativa");
        add("Charm Person", "Enfeitiçar Pessoa");
        add("Faerie Fire", "Fogo das Fadas");
        add("Thunderwave", "Onda Trovejante");
        add("Fire Bolt", "Raio de Fogo");
        add("Mage Hand", "Mãos Mágicas");
        add("Magic Missile", "Mísseis Mágicos");
        add("Mage Armor", "Armadura Arcana");
        add("Detect Magic", "Detectar Magia");
        add("Feather Fall", "Queda Suave");
        add("Sacred Flame", "Chama Sagrada");
        add("Cure Wounds", "Curar Ferimentos");
        add("Produce Flame", "Produzir Chamas");
        add("Thorn Whip", "Chicote de Espinhos");
        add("Fog Cloud", "Nuvem de Névoa");
        add("Eldritch Blast", "Rajada Mística");
        add("Hellish Rebuke", "Repreensão Infernal");
        add("Misty Step", "Passo Nebuloso");
        add("Goodberry", "Bom Fruto");
        add("Longstrider", "Passos Longos");
        add("Prestidigitation", "Prestidigitação");
        add("Thaumaturgy", "Taumaturgia");
        add("Guidance", "Orientação");
        add("Bless", "Bênção");
        add("Command", "Comando");
        add("Entangle", "Constrição");
        add("Moonbeam", "Raio Lunar");
        add("Hex", "Bruxaria");
        add("Darkness", "Escuridão");
        add("Invisibility", "Invisibilidade");
        add("Suggestion", "Sugestão");
        add("Sleep", "Sono");
        add("Web", "Teia");
        add("Shield", "Escudo Arcano");
        add("Aid", "Auxílio");
        add("Mending", "Consertar");
        add("Identify", "Identificação");
        add("Sanctuary", "Santuário");

        // Classes, espécies e antecedentes em fichas externas.
        add("Guild Artisan", "Artesão de Guilda");
        add("Folk Hero", "Herói do Povo");
        add("Half-Elf", "Meio-Elfo");
        add("Half-Orc", "Meio-Orc");
        add("Dragonborn", "Draconato");
        add("Artificer", "Artífice");
        add("Barbarian", "Bárbaro");
        add("Sorcerer", "Feiticeiro");
        add("Warlock", "Bruxo");
        add("Fighter", "Guerreiro");
        add("Ranger", "Patrulheiro");
        add("Paladin", "Paladino");
        add("Cleric", "Clérigo");
        add("Wizard", "Mago");
        add("Rogue", "Ladino");
        add("Druid", "Druida");
        add("Monk", "Monge");
        add("Bard", "Bardo");
        add("Human", "Humano");
        add("Dwarf", "Anão");
        add("Elf", "Elfo");
        add("Gnome", "Gnomo");
        add("Soldier", "Soldado");
        add("Entertainer", "Artista");
        add("Criminal", "Criminoso");
        add("Acolyte", "Acólito");
        add("Outlander", "Forasteiro");
        add("Charlatan", "Charlatão");
        add("Hermit", "Eremita");
        add("Sailor", "Marinheiro");
        add("Noble", "Nobre");
        add("Sage", "Sábio");
        add("Urchin", "Órfão");

        // Alinhamentos e equipamentos comuns em fichas importadas.
        add("Lawful Good", "Leal e Bom");
        add("Neutral Good", "Neutro e Bom");
        add("Chaotic Good", "Caótico e Bom");
        add("Lawful Neutral", "Leal e Neutro");
        add("True Neutral", "Neutro");
        add("Chaotic Neutral", "Caótico e Neutro");
        add("Lawful Evil", "Leal e Mau");
        add("Neutral Evil", "Neutro e Mau");
        add("Chaotic Evil", "Caótico e Mau");
        add("Leather Armor", "Armadura de couro");
        add("Studded Leather", "Couro batido");
        add("Chain Mail", "Cota de malha");
        add("Shortbow", "Arco curto");
        add("Longbow", "Arco longo");
        add("Crossbow", "Besta");
        add("Dagger", "Adaga");
        add("Rapier", "Rapieira");
        add("Longsword", "Espada longa");
        add("Shortsword", "Espada curta");
        add("Quarterstaff", "Bordão");
        add("Backpack", "Mochila");
        add("Rations", "Rações de viagem");

        // Termos mecânicos em descrições importadas.
        add("temporary hit points", "pontos de vida temporários");
        add("hit points", "pontos de vida");
        add("bonus action", "ação bônus");
        add("saving throw", "salvaguarda");
        add("attack roll", "jogada de ataque");
        add("spell attack", "ataque mágico");
        add("Wisdom", "Sabedoria");
        add("Dexterity", "Destreza");
        add("Strength", "Força");
        add("Constitution", "Constituição");
        add("Intelligence", "Inteligência");
        add("Charisma", "Carisma");
        add("concentration", "concentração");
        add("reaction", "reação");
        add("disadvantage", "desvantagem");
        add("advantage", "vantagem");
        add("humanoid", "humanoide");
        add("psychic damage", "dano psíquico");
        add("radiant damage", "dano radiante");
        add("fire damage", "dano de fogo");
        add("thunder damage", "dano trovejante");
        add("force damage", "dano de energia");
        add("target", "alvo");
        add("range", "alcance");
        add("action", "ação");
    }

    private DndPtBrLocalizer() {}

    private static void add(String source, String translated) {/*BR_JSON_WRAP*/try{
        TERMS.put(source, translated);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static String localizeText(String input) {
        if (input == null || input.length() == 0) return input == null ? "" : input;
        String out = input;
        for (Map.Entry<String,String> entry : TERMS.entrySet()) {
            String source = entry.getKey();
            Pattern p = Pattern.compile("(?i)(?<![\\p{L}\\p{N}])" + Pattern.quote(source) + "(?![\\p{L}\\p{N}])");
            out = p.matcher(out).replaceAll(Matcher.quoteReplacement(entry.getValue()));
        }
        return out;
    }

    public static JSONObject localizeCharacterSheet(JSONObject source) {/*BR_JSON_WRAP*/try{
        if (source == null) return new JSONObject();
        JSONObject out;
        try { out = new JSONObject(source.toString()); }
        catch (Exception e) { out = source; }

        String[] fields = {
                "species", "class_name", "background", "alignment", "role",
                "saving_throws", "skills", "passive_senses", "languages",
                "proficiencies", "tools", "carrying_capacity", "attacks",
                "features", "spellcasting", "spells", "inventory", "equipment",
                "level_up", "extra_notes"
        };
        for (String field : fields) {
            if (out.has(field)) {
                try { out.put(field, localizeText(out.optString(field))); }
                catch(Exception ignored){AppLog.ignored("DndPtBrLocalizer","Falha tolerada; a execução continuou.",ignored);}
            }
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
