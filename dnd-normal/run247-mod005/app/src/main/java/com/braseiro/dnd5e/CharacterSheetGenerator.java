package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Gera fichas completas e coerentes sem depender da IA. A IA pode enriquecer os
 * campos narrativos, mas os números básicos permanecem determinísticos.
 */
public final class CharacterSheetGenerator {
    private static final Locale PT = new Locale("pt", "BR");
    private final DndRulesEngine rules;
    private final Random random;
    private final Map<String,ArrayList<String>> selectionBags = new HashMap<String,ArrayList<String>>();

    private static final String[] CLASSES = {
            "Bárbaro", "Bardo", "Bruxo", "Clérigo", "Druida", "Feiticeiro",
            "Guerreiro", "Ladino", "Mago", "Monge", "Paladino", "Patrulheiro", "Cozinheiro",
            "Caçador de Sangue", "Necromante", "Cronomancer", "Cavaleiro da Morte"
    };
    private static final String[] BACKGROUNDS = {
            "Acólito", "Artesão de Guilda", "Artista", "Charlatão", "Criminoso",
            "Eremita", "Forasteiro", "Herói do Povo", "Marinheiro", "Nobre",
            "Órfão", "Sábio", "Soldado"
    };
    private static final String[] BACKGROUNDS_2024 = {
            "Acólito", "Andarilho", "Artesão", "Artista", "Charlatão", "Criminoso",
            "Eremita", "Escriba", "Fazendeiro", "Guarda", "Guia", "Marinheiro",
            "Mercador", "Nobre", "Sábio", "Soldado"
    };
    private static final String[] ALIGNMENTS = {
            "Leal e Bom", "Neutro e Bom", "Caótico e Bom", "Leal e Neutro",
            "Neutro", "Caótico e Neutro"
    };
    private static final String[] ORIGINS = {
            "Aldeia rural", "Bairro pobre", "Cidade mercantil", "Fronteira selvagem",
            "Templo ou mosteiro", "Corte nobre", "Comunidade itinerante", "Colônia militar",
            "Porto movimentado", "Fortaleza subterrânea", "Bosque ancestral"
    };
    private static final String[] CAREERS = {
            "Milícia e guerra", "Erudição e magia", "Templo e peregrinação", "Crime e submundo",
            "Comércio e ofício", "Mar e estradas", "Caça e exploração", "Corte e diplomacia",
            "Mineração e engenharia", "Curandeirismo e animais"
    };

    public CharacterSheetGenerator(DndRulesEngine rules) {
        this.rules = rules == null ? new DndRulesEngine() : rules;
        this.random = new Random();
    }

    public String rollName(String species) { return randomName(species); }
    public String rollNickname(String cls) { return randomNickname(cls); }
    public String rollTitle(String cls) { return randomTitle(cls); }

    /**
     * Gera somente um campo da ficha, mantendo espécie, classe, antecedente,
     * nível e perfil de regras atuais. É a opção "Aleatório local" do botão de
     * cada campo e não depende de rede ou de IA.
     */
    public String randomField(String key, JSONObject current) {
        JSONObject context = current == null ? new JSONObject() : current;
        String normalized = key == null ? "" : key.trim();
        try {
            String profile = context.optString("rules_source", "5e 2024");
            String species = context.optString("species", "Humano");
            String cls = baseClass(context.optString("class_name", "Guerreiro"));
            String background = context.optString("background", "Soldado");
            int level = clamp(context.optInt("level", 1), 1, 20);

            if ("name".equals(normalized)) return randomName(species);
            if ("nickname".equals(normalized)) return randomNickname(cls);
            if ("title".equals(normalized)) return randomTitle(cls);
            if ("subclass".equals(normalized)) return randomSubclass(cls, level, profile, random);
            if ("level".equals(normalized)) return String.valueOf(1 + random.nextInt(20));
            if (isAbilityKey(normalized)) return String.valueOf(rollAbilityScore(random));
            if ("extra_notes".equals(normalized)) return pick(new String[]{
                    "Uma testemunha reconhece o personagem por um feito que ele preferia esquecer.",
                    "Um objeto aparentemente comum reage à presença do personagem.",
                    "Uma promessa antiga volta a ser cobrada no pior momento possível.",
                    "O contato envia um aviso incompleto, acompanhado por um símbolo desconhecido.",
                    "O rival oferece uma trégua que parece boa demais para ser sincera."
            });

            JSONObject generated = isMechanicalField(normalized)
                    ? mechanicsFromContext(context, profile, species, cls, background, level)
                    : baseComplete(profile, species, cls, background, level);
            generated.put("life_origin", context.optString("life_origin", generated.optString("life_origin")));
            generated.put("life_career", context.optString("life_career", generated.optString("life_career")));
            generated.put("life_terms", context.optInt("life_terms", generated.optInt("life_terms", 2)));
            enrichNarrative(generated, true);
            if ("life_path".equals(normalized)) return generateLifePath(
                    generated.optString("life_origin"), generated.optString("life_career"),
                    generated.optInt("life_terms", 2), species, cls, background);
            if ("hp".equals(normalized)) normalized = "max_hp";
            Object value = generated.opt(normalized);
            return value == null ? "" : GeneratedTextSanitizer.scalar(String.valueOf(value), normalized);
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Converte uma consulta Mythic em um valor mecanicamente legal. Os termos
     * influenciam escolhas e rolagens, mas nunca substituem regras por texto
     * livre em atributos, defesas, perícias, equipamentos ou magia.
     */
    public String oracleRuleField(String key, JSONObject current, String wordA, String wordB, int rollA, int rollB) {
        JSONObject context = current == null ? new JSONObject() : current;
        String normalized = key == null ? "" : key.trim();
        long seed = oracleSeed(normalized, wordA, wordB, rollA, rollB);
        Random oracleRandom = new Random(seed);
        try {
            String profile = context.optString("rules_source", "5e 2024");
            String species = context.optString("species", "Humano");
            String cls = baseClass(context.optString("class_name", "Guerreiro"));
            String background = context.optString("background", "Soldado");
            int level = clamp(context.optInt("level", 1), 1, 20);
            if ("subclass".equals(normalized)) return randomSubclass(cls, level, profile, oracleRandom);
            if ("level".equals(normalized)) return String.valueOf(1 + oracleRandom.nextInt(20));
            if (isAbilityKey(normalized)) return String.valueOf(rollAbilityScore(oracleRandom));

            JSONObject generated = mechanicsFromContext(context, profile, species, cls, background, level);
            if ("hp".equals(normalized)) normalized = "max_hp";
            Object value = generated.opt(normalized);
            return value == null ? "" : GeneratedTextSanitizer.scalar(String.valueOf(value), normalized);
        } catch (Exception ignored) {
            return randomField(normalized, context);
        }
    }

    /** Seleciona uma opção legal usando os dois resultados do Mythic como semente. */
    public int oracleChoiceIndex(int optionCount, String field, String wordA, String wordB, int rollA, int rollB) {
        if (optionCount <= 1) return 0;
        long seed = oracleSeed(field, wordA, wordB, rollA, rollB);
        return new Random(seed).nextInt(optionCount);
    }

    private long oracleSeed(String field, String wordA, String wordB, int rollA, int rollB) {
        String material = String.valueOf(field) + "|" + String.valueOf(wordA) + "|" +
                String.valueOf(wordB) + "|" + rollA + "|" + rollB;
        long seed = 1125899906842597L;
        for (int i = 0; i < material.length(); i++) seed = 31L * seed + material.charAt(i);
        return seed;
    }

    private boolean isAbilityKey(String key) {
        return "str".equals(key) || "dex".equals(key) || "con".equals(key) ||
                "int_score".equals(key) || "wis".equals(key) || "cha".equals(key);
    }

    private boolean isMechanicalField(String key) {
        for (String candidate : new String[]{"hp","max_hp","ac","speed","initiative","proficiency_bonus","hit_dice",
                "saving_throws","skills","passive_senses","languages","proficiencies","tools","carrying_capacity",
                "attacks","features","spellcasting","spells","inventory","equipment"}) if (candidate.equals(key)) return true;
        return false;
    }

    private JSONObject mechanicsFromContext(JSONObject context, String profile, String species, String cls, String background, int level) throws Exception {
        JSONObject generated = new JSONObject();
        generated.put("rules_source", profile);generated.put("species", species);generated.put("class_name", cls);
        generated.put("background", background);generated.put("level", level);
        int[] fallback = classScores(cls);
        generated.put("str", context.optInt("str", fallback[0]));generated.put("dex", context.optInt("dex", fallback[1]));
        generated.put("con", context.optInt("con", fallback[2]));generated.put("int_score", context.optInt("int_score", fallback[3]));
        generated.put("wis", context.optInt("wis", fallback[4]));generated.put("cha", context.optInt("cha", fallback[5]));
        applyMechanics(generated);
        return generated;
    }

    private int rollAbilityScore(Random source) {
        int total = 0, lowest = 7;
        for (int i = 0; i < 4; i++) {
            int die = source.nextInt(6) + 1;
            total += die;
            lowest = Math.min(lowest, die);
        }
        return total - lowest;
    }

    private String randomSubclass(String cls, int level,String profile, Random source) {
        String c = norm(cls);
        boolean revised=rules.is2024(profile);int minimum=revised?3:(c.contains("clerigo")||c.contains("bruxo")||c.contains("feiticeiro")?1:(c.contains("druida")||c.contains("mago")?2:3));if(level<minimum)return "";
        String[] options;
        if (c.contains("barbar")) options = revised?new String[]{"Berserker", "Coração Selvagem", "Árvore do Mundo", "Fanático"}:new String[]{"Berserker","Guerreiro Totêmico"};
        else if (c.contains("bardo")) options = revised?new String[]{"Colégio da Dança", "Colégio do Glamour", "Colégio do Conhecimento", "Colégio do Valor"}:new String[]{"Colégio do Conhecimento","Colégio do Valor"};
        else if (c.contains("bruxo")) options = revised?new String[]{"Patrono Arquifada", "Patrono Celestial", "Patrono Corruptor", "Patrono Grande Antigo"}:new String[]{"Arquifada","Corruptor","Grande Antigo"};
        else if (c.contains("clerigo")) options = new String[]{"Domínio da Guerra", "Domínio da Luz", "Domínio da Trapaça", "Domínio da Vida"};
        else if (c.contains("druida")) options = revised?new String[]{"Círculo da Lua", "Círculo da Terra", "Círculo das Estrelas", "Círculo do Mar"}:new String[]{"Círculo da Terra","Círculo da Lua"};
        else if (c.contains("feiticeiro")) options = revised?new String[]{"Feitiçaria Aberrante", "Feitiçaria Dracônica", "Feitiçaria do Caos Mecânico", "Magia Selvagem"}:new String[]{"Linhagem Dracônica","Magia Selvagem"};
        else if (c.contains("ladino")) options = revised?new String[]{"Trapaceiro Arcano", "Assassino", "Alma-Faca", "Ladrão"}:new String[]{"Trapaceiro Arcano","Assassino","Ladrão"};
        else if (c.contains("guerreiro")) options = revised?new String[]{"Mestre de Batalha", "Campeão", "Cavaleiro Arcano", "Guerreiro Psiônico"}:new String[]{"Mestre de Batalha","Campeão","Cavaleiro Arcano"};
        else if (c.contains("mago")) options = revised?new String[]{"Abjurador", "Adivinho", "Evocador", "Ilusionista"}:new String[]{"Abjuração","Adivinhação","Conjuração","Encantamento","Evocação","Ilusão","Necromancia","Transmutação"};
        else if (c.contains("monge")) options = revised?new String[]{"Guerreiro da Misericórdia", "Guerreiro da Mão Aberta", "Guerreiro das Sombras", "Guerreiro dos Elementos"}:new String[]{"Caminho da Mão Aberta","Caminho da Sombra","Caminho dos Quatro Elementos"};
        else if (c.contains("paladino")) options = revised?new String[]{"Juramento da Devoção", "Juramento da Glória", "Juramento da Vingança", "Juramento dos Anciões"}:new String[]{"Juramento da Devoção","Juramento da Vingança","Juramento dos Anciões"};
        else if (c.contains("patrulheiro")) options = revised?new String[]{"Andarilho Feérico", "Caçador", "Mestre das Feras", "Perseguidor Sombrio"}:new String[]{"Caçador","Mestre das Feras"};
        else if (c.contains("cozinheiro")) options = new String[]{"Sous Chef","Sargento Alimentar","Mestre-Cuca","Mestre Cervejeiro","Amassador de Monstros"};
        else options = new String[0];
        JSONArray supplemental=CharacterOptionCatalogEngine.subclassesFor(cls);if(supplemental.length()>0)options=mergeCompiledSubclassOptions(cls,options,supplemental);
        return options.length==0?"":options[source.nextInt(options.length)];
    }

    private String[] mergeSubclassOptions(String[] base,JSONArray extra){ArrayList<String> out=new ArrayList<String>();for(String x:base)if(x!=null&&!out.contains(x))out.add(x);for(int i=0;i<extra.length();i++){String x=extra.optString(i);if(x!=null&&x.length()>0&&!out.contains(x))out.add(x);}return out.toArray(new String[0]);}
    private String[] mergeCompiledSubclassOptions(String cls,String[] base,JSONArray extra){ArrayList<String> out=new ArrayList<String>();for(String x:base)if(x!=null&&!out.contains(x))out.add(x);for(int i=0;i<extra.length();i++){String x=extra.optString(i);if(x==null||x.trim().isEmpty()||out.contains(x))continue;JSONObject gate=CharacterOptionCatalogEngine.validateSubclass(cls,x);if(gate.optBoolean("selection_allowed",false)&&gate.optBoolean("mechanics_compiled",false))out.add(x);}return out.toArray(new String[0]);}

    public String[] selectableClasses(String profile){
        ArrayList<String> out=new ArrayList<String>();
        for(String c:CLASSES){
            if(c==null||containsNormalized(out,c))continue;
            JSONObject meta=CharacterOptionCatalogEngine.lookupClass(c);
            boolean core=!HomebrewClassRuntimeEngine.lookup(c).optBoolean("known",false);
            boolean compiledCatalog=meta.optBoolean("known",false)&&(meta.optBoolean("mechanics_compiled",false)||meta.optBoolean("base_class_mechanics_compiled",false));
            boolean compiledHomebrew=HomebrewClassRuntimeEngine.supports(c);
            if(core||compiledCatalog||compiledHomebrew||SupplementContentRegistry.isKnownClass(c))out.add(c);
        }
        JSONArray extra=CharacterOptionCatalogEngine.allSupplementClasses();for(int i=0;i<extra.length();i++){String c=extra.optString(i);JSONObject meta=CharacterOptionCatalogEngine.lookupClass(c);if(meta.optBoolean("known",false)&&(meta.optBoolean("mechanics_compiled",false)||meta.optBoolean("base_class_mechanics_compiled",false))&&!containsNormalized(out,c))out.add(c);}
        if(!containsNormalized(out,"Artífice"))out.add("Artífice");
        return out.toArray(new String[0]);
    }

    public JSONObject recomputeMechanics(JSONObject source){
        JSONObject o;try{o=source==null?new JSONObject():new JSONObject(source.toString());applyMechanics(o);return ensureComplete(o);}catch(Exception e){return source==null?new JSONObject():source;}
    }

    /** Extrai os campos estruturados de uma história de vida já rolada. */
    public JSONObject lifeDetailsFromPath(String lifePath) {
        JSONObject out = new JSONObject();
        try {
            if (lifePath == null) return out;
            for (String raw : lifePath.split("\n")) {
                String line = raw.trim();
                putLifeLine(out, line, "Contato:", "contact");
                putLifeLine(out, line, "Rival:", "rival");
                putLifeLine(out, line, "Dívida ou compromisso:", "debt");
                putLifeLine(out, line, "Motivação:", "motivation");
                putLifeLine(out, line, "Entrada na classe:", "class_entry");
            }
        } catch(Exception ignored){AppLog.ignored("CharacterSheetGenerator","Falha tolerada; a execução continuou.",ignored);}
        return out;
    }

    private void putLifeLine(JSONObject out, String line, String prefix, String key) throws Exception {
        if (line.regionMatches(true, 0, prefix, 0, prefix.length())) {
            String value = line.substring(prefix.length()).trim();
            if (value.endsWith(".")) value = value.substring(0, value.length() - 1).trim();
            if (!value.isEmpty()) out.put(key, value);
        }
    }

    public JSONObject randomComplete(String profile, String[] speciesOptions, List<JSONObject> existing) {
        // New fully automatic creation is 2024-only. Legacy profiles remain
        // readable/editable; never silently migrate an existing sheet.
        if(!DndRulesEngine.PROFILE_2024.equals(DndRulesEngine.canonicalProfile(profile))){
            JSONObject blocked=new JSONObject();
            try{
                blocked.put("rules_source",DndRulesEngine.canonicalProfile(profile));
                blocked.put("generation_status","blocked");
                blocked.put("generation_valid",false);
                blocked.put("generation_blocked_reason",
                    "Novas fichas aleatórias aceitam apenas D&D 2024. " +
                    "Fichas 2014 e híbridas existentes continuam preservadas.");
            }catch(org.json.JSONException ignored){}
            return blocked;
        }
        // Rejected candidates must NEVER reach the sheet editor. Some random
        // combinations have no legal unchosen proficiency (e.g. Elf Keen Senses
        // after all three choices were already granted by class/background).
        // Reroll the entire candidate, then fail closed if no legal combination
        // could be certified. Never grant a substitute outside the PHB options.
        JSONObject last=new JSONObject();
        int maxAttempts=rules.is2024(profile)?8:1;
        for(int attempt=0;attempt<maxAttempts;attempt++){
            last=randomCompleteAttempt(profile,speciesOptions,existing);
            if(last.optBoolean("generation_valid",false))return last;
        }
        return last;
    }

    private JSONObject randomCompleteAttempt(String profile, String[] speciesOptions,
                                             List<JSONObject> existing) {
        try {
            String[] legalSpecies=CharacterRandom2024Rules.legalSpecies(profile,speciesOptions);
            String[] legalClasses=CharacterRandom2024Rules.legalClasses(profile,selectableClasses(profile));
            if(legalSpecies.length==0||legalClasses.length==0)
                throw new IllegalStateException("Não há espécie ou classe com mecânicas certificadas nesse perfil.");
            String species = chooseBalanced(legalSpecies, existing, "species");
            String cls = chooseBalanced(legalClasses, existing, "class_name");
            String background = chooseBalanced(rules.is2024(profile)?Dnd2024RulePackCatalog.backgroundPool():BACKGROUNDS, existing, "background");
            JSONObject sheet = baseComplete(profile, species, cls, background, 1,true);
            // Trace the source catalog for newly generated 2024 sheets only; do not migrate saves.
            sheet.put("dnd2024_rulepack_revision",Dnd2024RulePackCatalog.revision());
            String subclass=rules.is2024(profile)?"":randomSubclass(cls,1,profile,random);if(subclass.length()>0){sheet.put("class_name",cls+" — "+subclass);sheet.put("features",sheet.optString("features")+"\nSubclasse de nível 1: "+subclass+".");CrossBookSubclassRuntimeEngine.applyToSheet(sheet);}
            sheet.put("name", randomName(species));
            sheet.put("nickname", chance(58) ? randomNickname(cls) : "");
            sheet.put("title", chance(38) ? randomTitle(cls) : "");
            sheet.put("alignment", pick(ALIGNMENTS));
            sheet.put("life_origin", pick(ORIGINS));
            sheet.put("life_career", pick(CAREERS));
            sheet.put("life_terms", 2 + random.nextInt(3));
            enrichNarrative(sheet, true);
            sheet.put("life_path", generateLifePath(
                    sheet.optString("life_origin"), sheet.optString("life_career"),
                    sheet.optInt("life_terms", 2), species, cls, background));
            syncLifeDetails(sheet, true);
            sheet.put("history", mergeHistory(sheet.optString("history"), sheet.optString("life_path")));
            // One coherent, editable personality archetype for all 39 dimensions.
            // Never affects spells, skills, items or mechanical statistics.
            BraseiroRandomPersonalityEngine.applyToNewRandom(sheet,random);
            JSONObject assembled=ensureComplete(sheet);
            return CharacterRandom2024Rules.certify(
                CharacterRandom2024Rules.finalizeNew2024(assembled,random));
        } catch (Exception e) {
            JSONObject blocked=new JSONObject();
            try{
                blocked.put("generation_status","blocked").put("generation_valid",false)
                    .put("generation_blocked_reason",String.valueOf(e.getMessage()));
            }catch(Exception ignored){}
            return blocked;
        }
    }

    private String[] backgroundPool2014(){
        JSONArray x=CrossBookCharacterBackgroundEngine.allBackgrounds();String[] out=new String[BACKGROUNDS.length+x.length()];System.arraycopy(BACKGROUNDS,0,out,0,BACKGROUNDS.length);for(int i=0;i<x.length();i++)out[BACKGROUNDS.length+i]=x.optString(i);return out;
    }

    public JSONObject baseComplete(String profile, String species, String cls, String background, int level) {
        return baseComplete(profile,species,cls,background,level,false);
    }

    private JSONObject baseComplete(String profile, String species, String cls,
                                    String background, int level,boolean fullyRandom) {
        JSONObject o = new JSONObject();
        try {
            level = clamp(level, 1, 20);
            o.put("rules_source", profile == null ? "5e 2014" : profile);
            o.put("species", cleanChoice(species, "Humano"));
            o.put("class_name", cleanChoice(cls, "Guerreiro"));
            o.put("background", cleanChoice(background, "Soldado"));
            o.put("level", level);
            o.put("alignment", "Neutro e Bom");
            int[] scores = classScores(o.optString("class_name"));
            o.put("str", scores[0]); o.put("dex", scores[1]); o.put("con", scores[2]);
            o.put("int_score", scores[3]); o.put("wis", scores[4]); o.put("cha", scores[5]);
            applyOriginAbilityBonuses(o);
            if(fullyRandom&&rules.is2024(o.optString("rules_source")))
                CharacterRandom2024Rules.chooseBackgroundAbilitiesBeforeMechanics(o,random);
            applyMechanics(o);
            o.put("life_origin", "Cidade mercantil");
            o.put("life_career", careerForClass(o.optString("class_name")));
            o.put("life_terms", 2);
            enrichNarrative(o, false);
            o.put("life_path", generateLifePath(o.optString("life_origin"), o.optString("life_career"), 2,
                    o.optString("species"), o.optString("class_name"), o.optString("background")));
            syncLifeDetails(o, true);
            o.put("history", mergeHistory(o.optString("history"), o.optString("life_path")));
        } catch(Exception ignored){AppLog.ignored("CharacterSheetGenerator","Falha tolerada; a execução continuou.",ignored);}
        return ensureComplete(o);
    }

    public JSONObject fromLegacy(JSONObject character) {
        if (character == null) return new JSONObject();
        CharacterIdentityCoherence.repairLegacyRow(character);
        JSONObject out = new JSONObject();
        try {
            String raw = character.optString("sheet_json", "").trim();
            if (raw.startsWith("{")) out = new JSONObject(raw);
        } catch (Exception ignored) { out = new JSONObject(); }
        try {
            // A tabela principal é a fonte de verdade para estado mutável. Assim,
            // dano, cura, itens e edições feitas pelo Mestre aparecem imediatamente
            // também na ficha completa, mesmo que sheet_json tenha sido salvo antes.
            out.put("id", character.optLong("id"));
            out.put("name", character.optString("name", out.optString("name")));
            out.put("species", character.optString("species", out.optString("species")));
            out.put("class_name", character.optString("class_name", out.optString("class_name")));
            out.put("background", character.optString("background", out.optString("background")));
            out.put("rules_source", character.optString("rules_source", out.optString("rules_source")));
            out.put("level", character.optInt("level", out.optInt("level", 1)));
            out.put("hp", character.optInt("hp", out.optInt("hp", 1)));
            out.put("max_hp", character.optInt("max_hp", out.optInt("max_hp", 1)));
            out.put("ac", character.optInt("ac", out.optInt("ac", 10)));
            out.put("str", character.optInt("str", out.optInt("str", 10)));
            out.put("dex", character.optInt("dex", out.optInt("dex", 10)));
            out.put("con", character.optInt("con", out.optInt("con", 10)));
            out.put("int_score", character.optInt("int_score", out.optInt("int_score", 10)));
            out.put("wis", character.optInt("wis", out.optInt("wis", 10)));
            out.put("cha", character.optInt("cha", out.optInt("cha", 10)));
            out.put("active", character.optInt("active", 1));

            String visual = character.optString("visual_description").trim();
            if (!visual.isEmpty() && out.optString("appearance").trim().isEmpty()) out.put("appearance", visual);
            if (!visual.isEmpty() && out.optString("visual_description").trim().isEmpty()) out.put("visual_description", visual);
            copyIfMissing(out, "proficiencies", character.optString("proficiencies"));

            // A structured sheet may have a newer inventory than the legacy
            // equipment column (including customized quantities and unlisted
            // items). Never replace it during edit/reopen. An explicitly empty
            // inventory is also authoritative: it must not resurrect old gear.
            String equipment = character.optString("equipment").trim();
            if (out.has("inventory")) {
                out.put("equipment", out.optString("inventory", ""));
            } else if (out.has("equipment")) {
                out.put("inventory", out.optString("equipment", ""));
            } else if (!equipment.isEmpty()) {
                out.put("inventory", equipment);
                out.put("equipment", equipment);
            }
            copyIfMissing(out, "spells", character.optString("spells"));

            String notes = character.optString("notes");
            out.put("campaign_notes", notes);
            if (out.optString("history").trim().isEmpty()) out.put("history", removeLifePath(notes));
            if (out.optString("life_path").trim().isEmpty() && notes.contains("HISTÓRIA DE VIDA")) {
                int at = notes.indexOf("HISTÓRIA DE VIDA"); out.put("life_path", notes.substring(at));
            }
            String masterNotes = extractMasterNotes(notes);
            if (!masterNotes.isEmpty()) {
                String extra = out.optString("extra_notes").trim();
                if (!extra.contains(masterNotes)) out.put("extra_notes", extra + (extra.isEmpty()?"":"\n") + masterNotes);
            }
            if (out.optString("alignment").trim().isEmpty()) out.put("alignment", "Neutro");
            if (out.optString("life_origin").trim().isEmpty()) out.put("life_origin", "Cidade mercantil");
            if (out.optString("life_career").trim().isEmpty()) out.put("life_career", careerForClass(out.optString("class_name")));
            if (!out.has("life_terms")) out.put("life_terms", 2);
            applyMechanicsIfMissing(out); enrichNarrativeIfMissing(out);
        } catch(Exception ignored){AppLog.ignored("CharacterSheetGenerator","Falha tolerada; a execução continuou.",ignored);}
        return ensureComplete(out);
    }

    private String removeLifePath(String notes) { if (notes == null) return ""; int at=notes.indexOf("HISTÓRIA DE VIDA"); return at<0?notes.trim():notes.substring(0,at).trim(); }
    private String extractMasterNotes(String notes) {
        if (notes == null || notes.trim().isEmpty()) return "";StringBuilder b=new StringBuilder();
        for (String line : notes.split("\n")) if (line.trim().startsWith("[Mestre]")) { if (b.length()>0)b.append('\n');b.append(line.trim()); }
        return b.toString();
    }

    public JSONObject mergeNarrativeAi(JSONObject base, JSONObject ai) {
        if (base == null) base = new JSONObject();
        if (ai == null) return ensureComplete(base);
        String[] allowed = {"name", "nickname", "title", "alignment", "concept", "role", "appearance",
                "personality", "ideal", "bond", "fear", "flaw", "history", "mannerisms", "quotes",
                "contact", "rival", "debt", "motivation", "class_entry", "level_up"};
        try {
            for (String key : allowed) {
                String value = GeneratedTextSanitizer.scalar(ai.optString(key), key);
                if (!value.isEmpty()) base.put(key, value);
            }
            // Estes campos jamais podem ser trocados silenciosamente pela IA.
            sanitizeNarrativeFields(base);
        } catch(Exception ignored){AppLog.ignored("CharacterSheetGenerator","Falha tolerada; a execução continuou.",ignored);}
        return ensureComplete(base);
    }

    public JSONObject ensureComplete(JSONObject o) {
        if (o == null) o = new JSONObject();
        try {
            if (o.optString("species").trim().isEmpty()) o.put("species", "Humano");
            if (o.optString("class_name").trim().isEmpty()) o.put("class_name", "Guerreiro");
            if (o.optString("background").trim().isEmpty()) o.put("background", "Soldado");
            if (o.optInt("level", 0) < 1) o.put("level", 1);
            applyMechanicsIfMissing(o);
            if(!o.optBoolean("life_path_pending",false))enrichNarrativeIfMissing(o);
            putDefault(o, "name", randomName(o.optString("species")));
            putDefault(o, "alignment", "Neutro e Bom");
            if(!o.optBoolean("life_path_pending",false)) {
            putDefault(o, "life_origin", "Cidade mercantil");
            putDefault(o, "life_career", careerForClass(o.optString("class_name")));
            if (o.optInt("life_terms", 0) < 1) o.put("life_terms", 2);
            }
            if(!o.optBoolean("life_path_pending",false)) {
            if (o.optString("life_path").trim().isEmpty()) o.put("life_path", generateLifePath(
                    o.optString("life_origin"), o.optString("life_career"), o.optInt("life_terms", 2),
                    o.optString("species"), o.optString("class_name"), o.optString("background")));
            syncLifeDetails(o, false);
            o.put("history", mergeHistory(o.optString("history"), o.optString("life_path")));
            }
            sanitizeNarrativeFields(o);
            putDefault(o, "equipment", o.optString("inventory"));
        } catch(Exception ignored){AppLog.ignored("CharacterSheetGenerator","Falha tolerada; a execução continuou.",ignored);}
        return Character2024SheetReconciler.reconcile(o);
    }

    private void syncLifeDetails(JSONObject o, boolean overwrite) throws Exception {
        JSONObject details = lifeDetailsFromPath(o.optString("life_path"));
        for (String key : new String[]{"contact","rival","debt","motivation","class_entry"}) {
            String value = details.optString(key).trim();
            if (!value.isEmpty() && (overwrite || o.optString(key).trim().isEmpty())) o.put(key, value);
        }
    }

    public String generateLifePath(String origin, String career, int terms, String species, String cls, String background) {
        String[] events = {
                "sobreviveu a uma emboscada e aprendeu a desconfiar de rotas fáceis",
                "protegeu uma comunidade quando ninguém mais assumiu a responsabilidade",
                "falhou diante de uma escolha moral e ainda procura reparar o dano",
                "descobriu uma pista ligada a uma ruína que ninguém acredita existir",
                "foi acusado injustamente e precisou abandonar um lar",
                "salvou a vida de alguém influente, criando uma obrigação recíproca",
                "testemunhou magia perigosa e carrega uma lembrança impossível",
                "participou de uma expedição que voltou com menos da metade do grupo",
                "recusou uma ordem cruel e ganhou um inimigo",
                "encontrou um mapa incompleto e preservou uma parte dele",
                "negociou a paz entre rivais e descobriu que alguém lucrava com a guerra",
                "perdeu um objeto de família e segue a única pista que restou"
        };
        String[] contacts = {
                "uma curandeira itinerante", "um escriba de reputação duvidosa", "uma capitã mercenária",
                "um sacerdote pragmático", "uma contrabandista bem relacionada", "um caçador das fronteiras",
                "uma artesã anã", "um mensageiro da corte", "uma cartógrafa obcecada", "um tratador de animais"
        };
        String[] rivals = {
                "um antigo superior humilhado", "um herdeiro que perdeu prestígio", "um criminoso que cobra uma dívida",
                "um inquisidor convencido de sua culpa", "um explorador que deseja o mesmo segredo", "uma companhia rival",
                "um oficial que falsificou o relatório da missão", "uma comerciante que se sente traída"
        };
        String[] motives = {
                "provar que seu nome vale mais que sua origem", "quitar uma dívida que ameaça pessoas inocentes",
                "encontrar alguém desaparecido", "descobrir a verdade sobre a expedição perdida",
                "construir um legado que sobreviva à guerra", "impedir que o rival encontre primeiro um segredo antigo",
                "proteger a comunidade que lhe deu abrigo", "recuperar algo que pertence ao seu povo"
        };
        String culture = speciesCulture(species);
        StringBuilder b = new StringBuilder("HISTÓRIA DE VIDA — ").append(origin).append(" / ").append(career).append('\n');
        b.append("Origem: ").append(speciesOriginSentence(species, origin, culture)).append(".\n");
        ArrayList<String> pool = new ArrayList<String>(); Collections.addAll(pool, events); Collections.shuffle(pool, random);
        for (int i = 1; i <= clamp(terms, 1, 4); i++) b.append("Termo ").append(i).append(": ").append(pool.get((i - 1) % pool.size())).append(".\n");
        b.append("Contato: ").append(pick(contacts)).append(".\n");
        b.append("Rival: ").append(pick(rivals)).append(".\n");
        b.append("Dívida ou compromisso: deve um favor que pode ser cobrado durante a campanha.\n");
        b.append("Motivação: ").append(pick(motives)).append(".\n");
        b.append("Entrada na classe: as experiências anteriores culminaram no caminho de ").append(cls).append(".\n");
        b.append("Antecedente: ").append(background).append(".\n");
        b.append("Nota mecânica: estes eventos são ganchos narrativos; benefícios de regra vêm apenas de espécie, antecedente, classe, talentos e manuais habilitados.");
        return sanitizeSpeciesCoherence(b.toString(), species);
    }

    public String sanitizeSpeciesCoherence(String text, String species) {
        if (text == null) return "";
        String selectedSpecies = cleanChoice(species, "Humano");
        String identity = speciesIdentityAdjective(selectedSpecies);
        String roots = speciesRootAdjective(selectedSpecies);
        String community = speciesCommunity(selectedSpecies);
        String out = text;

        // Corrige frases gravadas por versões antigas ou devolvidas pela IA.
        out = out.replaceAll("(?iu)cresceu aprendendo a sobreviver como\\s+(humano|humana|anão|anã|elfo|elfa|halfling|gnomo|gnoma|golias|orc|orque|tiefling|aasimar|draconato|draconata)",
                "cresceu entre " + java.util.regex.Matcher.quoteReplacement(community));
        out = out.replaceAll("(?iu)cresceu entre costumes\\s+(humanos|humanas|anões|anãs|élficos|élficas|halflings|gnômicos|gnômicas|golias|orcs|tieflings|celestiais|dracônicos|dracônicas)",
                "cresceu entre " + java.util.regex.Matcher.quoteReplacement(community));
        out = out.replaceAll("(?iu)(suas?|as) raízes\\s+(humanas|anãs|élficas|halflings|gnômicas|golias|orcs|tieflings|celestiais|dracônicas)",
                "$1 raízes " + java.util.regex.Matcher.quoteReplacement(roots));
        out = out.replaceAll("(?iu)(identidade|herança|linhagem)\\s+(humana|anã|élfica|halfling|gnômica|golias|orc|tiefling|celestial|dracônica|meio-élfica|meio-orc)",
                "$1 " + java.util.regex.Matcher.quoteReplacement(identity));

        // Linhas de origem que afirmem outra espécie são substituídas por uma frase inequívoca.
        String[] lines = out.split("\n", -1);
        String selectedNorm = canonicalSpecies(selectedSpecies);
        String[] foreign = {"humano","anao","elfo","halfling","gnomo","golias","orc","tiefling","aasimar","draconato","meio elfo","meio orc"};
        for (int i = 0; i < lines.length; i++) {
            String lineNorm = norm(lines[i]);
            if (!(lineNorm.startsWith("origem ") || lineNorm.startsWith("historia ") || lineNorm.startsWith("aparencia "))) continue;
            boolean conflict = false;
            for (String label : foreign) {
                String canonical = canonicalSpecies(label);
                if (canonical.equals(selectedNorm)) continue;
                if (containsWholePhrase(lineNorm, canonical)) { conflict = true; break; }
            }
            if (conflict && lineNorm.startsWith("origem ")) lines[i] = "Origem: cresceu entre " + community + ", preservando sua identidade " + identity + ".";
        }
        return joinLines(lines);
    }

    private void sanitizeNarrativeFields(JSONObject o) throws Exception {
        String species = o.optString("species", "Humano");
        String[] fields = {"name","nickname","title","alignment","concept","role","appearance","visual_description","personality","ideal","bond","fear","flaw","history","mannerisms","quotes","contact","rival","debt","motivation","class_entry","life_path","level_up","extra_notes"};
        for (String field : fields) if (o.has(field)) {
            String clean=GeneratedTextSanitizer.scalar(o.optString(field),field);
            o.put(field, sanitizeSpeciesCoherence(clean, species));
        }
        o.put("visual_description", o.optString("appearance"));
    }

    private String canonicalSpecies(String value) {
        String n = norm(value);
        if (n.contains("meio elfo")) return "meio elfo";
        if (n.contains("meio orc")) return "meio orc";
        if (n.contains("anao")) return "anao";
        if (n.contains("draconato")) return "draconato";
        if (n.contains("halfling")) return "halfling";
        if (n.contains("gnomo")) return "gnomo";
        if (n.contains("golias")) return "golias";
        if (n.contains("tiefling")) return "tiefling";
        if (n.contains("aasimar")) return "aasimar";
        if (n.contains("elfo")) return "elfo";
        if (n.contains("orc")) return "orc";
        return "humano";
    }
    private boolean containsWholePhrase(String text,String phrase) { return (" " + text + " ").contains(" " + phrase + " "); }

    private String speciesCommunity(String species) {
        String n = norm(species);
        if (n.contains("meio elfo")) return "costumes humanos e élficos, construindo uma identidade própria";
        if (n.contains("meio orc")) return "heranças humanas e orcs, construindo uma identidade própria";
        if (n.contains("anao")) return "tradições anãs de ofício, memória e palavra dada";
        if (n.contains("elfo")) return "costumes élficos de observação, memória e ligação com o mundo";
        if (n.contains("halfling")) return "costumes halflings de hospitalidade, coragem e comunidade";
        if (n.contains("draconato")) return "tradições dracônicas de honra, linhagem e responsabilidade";
        if (n.contains("gnomo")) return "tradições gnômicas de curiosidade, invenção e trabalho minucioso";
        if (n.contains("golias")) return "tradições golias de responsabilidade, resistência e cooperação";
        if (n.equals("orc") || n.contains("meio orc")) return "costumes orcs de perseverança, família e força responsável";
        if (n.contains("tiefling")) return "uma comunidade tiefling marcada por independência e solidariedade";
        if (n.contains("aasimar")) return "uma comunidade ligada à sua herança celestial sem perder a própria vontade";
        return "costumes humanos diversos, adaptação e vínculos comunitários";
    }

    private String joinLines(String[] lines) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lines.length; i++) { if (i > 0) b.append('\n'); b.append(lines[i]); }
        return b.toString();
    }

    private void applyMechanics(JSONObject o) throws Exception {
        String cls = o.optString("class_name"); int level = clamp(o.optInt("level", 1), 1, 20);
        int con = o.optInt("con", 10), dex = o.optInt("dex", 10), wis = o.optInt("wis", 10);
        int maxHp = rules.suggestedMaxHp(cls, level, con);
        String inventory=inventoryFor(cls,o.optString("background"),o.optString("rules_source"));
        o.put("max_hp", maxHp); o.put("hp", maxHp); o.put("ac", rules.armorClassFromEquipment(inventory,cls,dex,con,wis));
        o.put("speed", speciesSpeed(o.optString("species"),o.optString("rules_source")));
        o.put("initiative", signed(rules.modifier(dex)));
        o.put("proficiency_bonus", signed(rules.proficiencyBonus(level)));
        o.put("hit_dice", level + "d" + rules.classHitDie(cls));
        o.put("saving_throws", savingThrows(cls, o));
        String skills = skillsFor(cls, o.optString("background"), o);
        o.put("skills", skills); o.put("proficiencies", proficienciesFor(cls));
        o.put("passive_senses", passiveSenses(o, skills));
        o.put("languages", languagesFor(o.optString("species"),o.optString("rules_source")));
        o.put("tools", toolsFor(o.optString("background"), cls,o.optString("rules_source")));
        if(rules.is2024(o.optString("rules_source")))o.put("origin_feat",Core2024EberronCharacterEngine.originFeatForBackground(o.optString("background")));
        o.put("carrying_capacity", Math.max(1, o.optInt("str", 10) * 7) + " kg (aprox.)");
        o.put("attacks", attacksFor(cls, o));
        String features=featuresFor(cls, o.optString("species"), level,o.optString("rules_source"));if(rules.is2024(o.optString("rules_source")))features+="\nTalento de Origem: "+o.optString("origin_feat")+".";
        o.put("features",features);
        VoloSubraceHandbookEngine.applySpeciesMechanics(o);VoloSubraceRun139Engine.applySpeciesMechanics(o);VoloSubraceRun140Engine.applySpeciesMechanics(o);if(Run141CompactCharacterOptionEngine.isSpecies(o.optString("species")))Run141CompactCharacterOptionEngine.applySpeciesMechanics(o);else Run141SpeciesFeatRuntimeEngine.applySpeciesMechanics(o);
        CrossBookCharacterOptionEngine.applySpeciesMechanics(o);
        MultiverseCharacterOptionEngine.applySpeciesMechanics(o);
        Core2024EberronCharacterEngine.apply2024SpeciesMechanics(o);
        Core2024EberronCharacterEngine.applyDragonmarkMechanics(o);
        Core2024EberronCharacterEngine.prepare2024Background(o);
        o.put("spellcasting", spellcastingFor(cls, o));
        o.put("spells", spellsFor(cls, level,o.optString("rules_source")));
        o.put("inventory", inventory);
        CrossBookCharacterBackgroundEngine.applyBackgroundMechanics(o);
        Core2024EberronCharacterEngine.applyHouseAgentBackground(o);
        CrossBookSubclassRuntimeEngine.applyToSheet(o);
        HomebrewSubclassRuntimeEngine.applyToSheet(o);Run141CompactCharacterOptionEngine.applySubclassMechanics(o);Run143ForgottenSecretsEngine.applyPatronMechanics(o);
        o.put("equipment", o.optString("inventory",inventory));
        JSONObject structured=CharacterResourceStateEngine.ensure(o,cls,level,o.optString("rules_source"));
        for(String key:new String[]{"hit_dice_max","hit_dice_spent","spell_slots_max","spell_slots_current","pact_magic","class_resources","inventory_ledger","inventory_ledger_status","resource_state_version","class_rules_source","class_rules_runtime","class_rules_profile","class_rules_auto_rolls","class_choices_pending","rage_damage_bonus","weapon_masteries_known","wild_shape_forms_known","wild_shape_max_cr","wild_shape_flight_allowed","sneak_attack_dice","cunning_strike_available","martial_arts_die","unarmored_movement_bonus_m","crimson_rite_die","blood_curses_known","source_hit_die_declared","source_first_level_hp","source_later_hp","prepared_spells_formula","spellbook_points_initial","spellbook_points_per_level_after_first","spellcasting_ability","creature_type_note","homebrew_class","subclass_name","subclass_class","subclass_source","subclass_runtime","subclass_mechanics_compiled","subclass_choices_pending","subclass_resources"})if(structured.has(key))o.put(key,structured.opt(key));
        o.put("progression",DndProgressionEngine.state(o,level));
        Character2024SheetReconciler.reconcile(o);
    }

    private void applyMechanicsIfMissing(JSONObject o) throws Exception {
        if (!o.has("str") || o.optInt("str", 0) < 3) {
            int[] s = classScores(o.optString("class_name"));
            o.put("str", s[0]); o.put("dex", s[1]); o.put("con", s[2]); o.put("int_score", s[3]); o.put("wis", s[4]); o.put("cha", s[5]);
        }
        JSONObject generated = new JSONObject();
        generated.put("species", o.optString("species")); generated.put("class_name", o.optString("class_name"));
        generated.put("background", o.optString("background")); generated.put("rules_source", o.optString("rules_source"));
        generated.put("level", o.optInt("level", 1));
        for (String k : new String[]{"str","dex","con","int_score","wis","cha"}) generated.put(k, o.optInt(k, 10));
        applyMechanics(generated);
        String[] keys = {"hp","max_hp","ac","speed","initiative","proficiency_bonus","hit_dice","saving_throws","skills",
                "proficiencies","passive_senses","languages","tools","carrying_capacity","attacks","features","spellcasting",
                "spells","inventory","equipment","hit_dice_max","hit_dice_spent","spell_slots_max","spell_slots_current","pact_magic","class_resources","inventory_ledger","inventory_ledger_status","resource_state_version","progression","species_source","species_source_page","species_mechanics_runtime","species_choice_pending","species_choices_complete","creature_type","size_options","darkvision_m","fly_speed","swim_speed","climb_speed","natural_armor_ac","resistances","immunities","condition_immunities","magic_resistance","pack_tactics","sunlight_sensitivity","powerful_build","carrying_capacity_multiplier","species_skill_proficiencies","saving_throw_advantage_abilities","condition_save_advantage","integrated_protection_ac_bonus","does_not_need_food_drink_breathe","magic_sleep_immune","telepathy_range_formula_m","dream_effect_immune","shapechanger","shifting_recharge","shifting_temp_hp_formula","shifting_ac_bonus","shifting_extra_temp_hp","shifting_speed_bonus_m","shifting_bite_damage","shifting_wisdom_check_advantage","shifting_blocks_enemy_attack_advantage_within_m","background_source","background_source_page","background_mechanics_runtime","background_choice_pending","background_choices_complete","background_skill_proficiencies","background_feature","expanded_spell_list","guild","guild_spell_list_available","legal_authority","boros_garrison_access","boros_weekly_salary_gp","false_identity_available","ravnica_city_travel_multiplier","gruul_refuge_party_capacity","izzet_infrastructure_knowledge","orzhov_leverage","rakdos_fearsome_reputation","selesnya_shelter_access","selesnya_free_care_components_required","simic_researcher_network","strixhaven_college","granted_feats","feat_choice_pending","feat_choices_complete","strixhaven_initiate_college_locked","origin_ability_choice_pending","origin_ability_allowed","origin_feat","origin_feat_choice_pending","human_extra_origin_feat_pending","selected_origin_feats","dragonmark","dragonmark_source","dragonmark_runtime","dragonmark_choice_pending","dragonmark_choices_complete","house_agent_house_pending","dragonmarked_house","subclass_name","subclass_class","subclass_source","subclass_source_locator","subclass_runtime","subclass_profile","subclass_choices_pending","subclass_mechanics_compiled","subclass_resources","subclass_active_effects","subclass_last_feature"};
        for (String k : keys) if (!o.has(k) || String.valueOf(o.opt(k)).trim().isEmpty() || (o.opt(k) instanceof Number && o.optInt(k, 0) <= 0)) o.put(k, generated.opt(k));
        MultiverseCharacterOptionEngine.applySpeciesMechanics(o);
        Core2024EberronCharacterEngine.apply2024SpeciesMechanics(o);
        Core2024EberronCharacterEngine.applyDragonmarkMechanics(o);
        Core2024EberronCharacterEngine.prepare2024Background(o);
        Core2024EberronCharacterEngine.applyHouseAgentBackground(o);
    }

    private void enrichNarrative(JSONObject o, boolean moreRandom) throws Exception {
        String species = o.optString("species"), cls = o.optString("class_name"), bg = o.optString("background");
        String occupation = roleForClass(cls);
        o.put("role", occupation);
        o.put("concept", conceptFor(species, cls, bg));
        o.put("appearance", appearanceFor(species, cls));
        o.put("visual_description", o.optString("appearance"));
        o.put("personality", pick(new String[]{"Prático, atento e protetor, mas possui um humor inesperadamente seco.", "Curioso e gentil, escuta antes de agir e guarda rancor de crueldades gratuitas.", "Teatral quando está seguro e silencioso quando o perigo é real.", "Metódico, leal aos companheiros e incapaz de ignorar um mistério mal explicado.", "Otimista por escolha, mesmo carregando lembranças difíceis."}));
        o.put("ideal", pick(new String[]{"Responsabilidade — poder sem consequências é apenas abuso bem vestido.", "Liberdade — ninguém deve ser reduzido ao papel que lhe deram.", "Conhecimento — perguntas honestas valem mais que certezas confortáveis.", "Proteção — força serve para guardar quem não consegue se defender.", "Lealdade — uma promessa só vale quando cumpri-la custa alguma coisa."}));
        o.put("bond", pick(new String[]{"Carrega a lembrança de uma comunidade que jurou proteger.", "Procura uma pessoa desaparecida cuja última mensagem nunca foi explicada.", "Guarda um objeto de família que parece ligado a um segredo antigo.", "Deve a vida a um mentor que agora precisa de ajuda.", "Prometeu reparar um erro que destruiu a confiança de alguém."}));
        o.put("fear", pick(new String[]{"Teme falhar quando alguém vulnerável depende de sua decisão.", "Teme descobrir que o segredo que procura é pior do que a mentira que o esconde.", "Teme perder o controle e ferir alguém que jurou proteger.", "Teme ser lembrado apenas pelo pior erro de sua vida.", "Teme que seu rival conheça uma verdade que nem seus aliados sabem."}));
        o.put("flaw", pick(new String[]{"Não consegue abandonar uma pista, mesmo quando deveria recuar.", "Confunde coragem com responsabilidade e tenta resolver tudo sozinho.", "Perdoa os outros com facilidade, mas nunca a si mesmo.", "Quando percebe uma injustiça, perde toda diplomacia.", "Aceita desafios absurdos quando alguém duvida de sua competência."}));
        o.put("contact", pick(new String[]{"Uma curandeira itinerante que cobra favores em vez de moedas.", "Um escriba de reputação duvidosa que conhece arquivos proibidos.", "Uma capitã mercenária que respeita competência e palavra dada.", "Uma artista aposentada que mantém esconderijos em várias cidades.", "Um tratador de animais que conhece trilhas ignoradas pelos mapas."}));
        o.put("rival", pick(new String[]{"Um inquisidor convencido de que o personagem esconde algo perigoso.", "Uma companhia rival que lucra com o fracasso de sua missão.", "Um antigo superior que considera a deserção uma traição pessoal.", "Uma exploradora que procura o mesmo segredo e aceita métodos cruéis.", "Um credor influente que prefere controlar a dívida a recebê-la."}));
        o.put("debt", pick(new String[]{"Deve um favor que pode ser cobrado durante a campanha.", "Prometeu devolver um objeto ou documento à família de alguém desaparecido.", "Sua fuga só foi possível porque outra pessoa assumiu a culpa.", "Aceitou proteger uma comunidade que agora corre perigo.", "Possui uma obrigação legal ou religiosa que entra em conflito com a aventura."}));
        o.put("motivation", pick(new String[]{"Descobrir a verdade antes que o rival a transforme em arma.", "Proteger a comunidade que lhe deu abrigo.", "Reparar uma escolha que ainda causa sofrimento.", "Construir um legado que sobreviva à guerra.", "Recuperar algo que pertence ao seu povo."}));
        o.put("class_entry", "As experiências anteriores culminaram no caminho de "+cls+".");
        o.put("history", historyFor(species, cls, bg));
        o.put("mannerisms", pick(new String[]{"Conta objetos quando está nervoso; alisa o cabo da arma antes de mentir.", "Fala baixo com animais; repete a última palavra de uma pergunta difícil.", "Faz anotações em pedaços de papel; sempre verifica saídas e janelas.", "Transforma conselhos em provérbios; guarda pequenos presentes de cada lugar visitado.", "Muda de postura quando entra em perigo e nunca dá as costas para uma porta aberta."}));
        o.put("quotes", pick(new String[]{"‘Podemos fazer isso do jeito fácil. Ainda não decidi qual é.’", "‘Toda ruína foi um lar antes de virar aviso.’", "‘Não prometo vitória. Prometo não abandonar ninguém.’", "‘Tenho um plano. A parte preocupante é que ele faz sentido.’", "‘Pergunte de novo quando estivermos fora do alcance das flechas.’"}));
        o.put("level_up", levelUpFor(cls));
        if (moreRandom && chance(25)) o.put("title", randomTitle(cls));
    }

    private void enrichNarrativeIfMissing(JSONObject o) throws Exception {
        JSONObject g = new JSONObject();
        g.put("species", o.optString("species")); g.put("class_name", o.optString("class_name")); g.put("background", o.optString("background"));
        enrichNarrative(g, false);
        String[] keys = {"role","concept","appearance","visual_description","personality","ideal","bond","fear","flaw","contact","rival","debt","motivation","class_entry","history","mannerisms","quotes","level_up"};
        for (String k : keys) if (o.optString(k).trim().isEmpty()) o.put(k, g.optString(k));
    }

    private int[] classScores(String cls) {
        String c = norm(cls);
        int[] values = {15, 14, 13, 12, 10, 8};
        int[] order;
        if (c.contains("necromante")) order = new int[]{3,2,4,1,5,0};
        else if (c.contains("cronomancer")) order = new int[]{4,2,1,3,5,0};
        else if (c.contains("cavaleiro da morte")) order = new int[]{2,0,5,1,4,3};
        else if (c.contains("cacador de sangue")) order = new int[]{0,4,2,1,3,5};
        else if (c.equals("xama")) order = new int[]{4,2,3,1,5,0};
        else if (c.contains("mago") || c.contains("artifice")) order = new int[]{3,1,2,4,5,0};
        else if (c.contains("feiticeiro") || c.contains("bardo") || c.contains("bruxo")) order = new int[]{5,1,2,4,3,0};
        else if (c.contains("clerigo") || c.contains("druida")) order = new int[]{4,2,1,5,3,0};
        else if (c.contains("ladino")) order = new int[]{1,2,3,4,5,0};
        else if (c.contains("monge") || c.contains("patrulheiro")) order = new int[]{1,4,2,0,3,5};
        else if (c.contains("paladino")) order = new int[]{0,5,2,1,4,3};
        else if (c.contains("barbar")) order = new int[]{0,2,1,4,5,3};
        else if (c.contains("cozinheiro")) order = new int[]{2,1,5,4,0,3};
        else order = new int[]{0,2,1,4,5,3};
        int[] scores = new int[6];
        for (int i = 0; i < order.length; i++) scores[order[i]] = values[i];
        // Pequena variedade: troca apenas atributos secundários, preservando os dois principais.
        if (chance(50)) { int a = order[2], b = order[3]; int t = scores[a]; scores[a] = scores[b]; scores[b] = t; }
        return scores;
    }

    private void applyOriginAbilityBonuses(JSONObject o) throws Exception {
        if(rules.is2024(o.optString("rules_source"))){
            // RUN136: PHB 2024 exige escolha do jogador. Não otimizar nem distribuir +2/+1 automaticamente.
            o.put("origin_ability_choice_pending",true);
            o.put("origin_ability_allowed",Core2024EberronCharacterEngine.allowedBackgroundAbilities(o.optString("background")));
            o.put("origin_ability_bonus","Escolha manual pendente: +2 e +1 em dois atributos diferentes permitidos pelo antecedente 2024");
            return;
        }
        if(VoloSubraceHandbookEngine.apply2014AbilityBonuses(o)) return;if(VoloSubraceRun139Engine.apply2014AbilityBonuses(o)) return;if(VoloSubraceRun140Engine.apply2014AbilityBonuses(o)) return;if(Run141CompactCharacterOptionEngine.apply2014AbilityBonuses(o)) return;if(Run141SpeciesFeatRuntimeEngine.apply2014AbilityBonuses(o)) return;
        if(Core2024EberronCharacterEngine.applyEberronDragonmarkAbilityBonuses(o)) return;
        if(MultiverseCharacterOptionEngine.apply2014AbilityBonuses(o)) return;
        if(CrossBookCharacterOptionEngine.apply2014AbilityBonuses(o)) return;
        String s=norm(o.optString("species"));
        if(s.equals("humano"))for(int i=0;i<6;i++)addAbility(o,i,1);
        else if(s.contains("meio elfo")){addAbility(o,5,2);addBestOtherAbilities(o,new int[]{0,1,2,3,4},2,1);}
        else if(s.contains("meio orc")){addAbility(o,0,2);addAbility(o,2,1);}
        else if(s.contains("anao")){addAbility(o,2,2);addAbility(o,4,1);}
        else if(s.contains("elfo")){addAbility(o,1,2);addAbility(o,3,1);}
        else if(s.contains("halfling")){addAbility(o,1,2);addAbility(o,5,1);}
        else if(s.contains("draconato")){addAbility(o,0,2);addAbility(o,5,1);}
        else if(s.contains("gnomo")){addAbility(o,3,2);addAbility(o,2,1);}
        else if(s.contains("tiefling")){addAbility(o,5,2);addAbility(o,3,1);}
        o.put("origin_ability_bonus","Bônus de espécie do perfil 2014 aplicado antes do jogo");
    }

    private int[] backgroundAbilityIndexes(String background){
        String b=norm(background);
        if(b.contains("acolito"))return new int[]{3,4,5};
        if(b.contains("andarilho"))return new int[]{1,4,5};
        if(b.equals("artesao"))return new int[]{0,1,3};
        if(b.contains("artista"))return new int[]{0,1,5};
        if(b.contains("charlatao"))return new int[]{1,2,5};
        if(b.contains("criminoso"))return new int[]{1,2,3};
        if(b.contains("eremita"))return new int[]{2,4,5};
        if(b.contains("escriba"))return new int[]{1,3,4};
        if(b.contains("fazendeiro"))return new int[]{0,2,4};
        if(b.contains("guarda"))return new int[]{0,3,4};
        if(b.equals("guia"))return new int[]{1,2,4};
        if(b.contains("marinheiro"))return new int[]{0,1,4};
        if(b.contains("mercador"))return new int[]{2,3,5};
        if(b.contains("nobre"))return new int[]{0,3,5};
        if(b.contains("sabio"))return new int[]{2,3,4};
        return new int[]{0,1,2};
    }
    private String backgroundAbilityNames(String background){int[] a=backgroundAbilityIndexes(background);String[] n={"Força","Destreza","Constituição","Inteligência","Sabedoria","Carisma"};return n[a[0]]+", "+n[a[1]]+" e "+n[a[2]];}
    private int abilityAt(JSONObject o,int i){String[] k={"str","dex","con","int_score","wis","cha"};return o.optInt(k[i],10);}
    private void addAbility(JSONObject o,int i,int amount)throws Exception{String[] k={"str","dex","con","int_score","wis","cha"};o.put(k[i],Math.min(20,o.optInt(k[i],10)+amount));}
    private void addBestOtherAbilities(JSONObject o,int[] allowed,int count,int amount)throws Exception{ArrayList<Integer> pool=new ArrayList<Integer>();for(int i:allowed)pool.add(i);Collections.sort(pool,new java.util.Comparator<Integer>(){public int compare(Integer a,Integer b){return Integer.compare(abilityAt(o,b),abilityAt(o,a));}});for(int i=0;i<Math.min(count,pool.size());i++)addAbility(o,pool.get(i),amount);}

    private String chooseBalanced(String[] options, List<JSONObject> existing, String field) {
        if (options == null || options.length == 0) return "";
        Map<String,Integer> counts = new HashMap<String,Integer>();
        for (String option : options) counts.put(norm(option), 0);
        if (existing != null) for (JSONObject c : existing) {
            String value = c.optString(field);
            if ("class_name".equals(field)) value = baseClass(value);
            String n = norm(value);
            if (counts.containsKey(n)) counts.put(n, counts.get(n) + 1);
        }
        int min = Integer.MAX_VALUE;
        for (Integer n : counts.values()) min = Math.min(min, n);
        ArrayList<String> candidates = new ArrayList<String>();
        for (String option : options) if (counts.get(norm(option)) == min) candidates.add(option);
        if (candidates.isEmpty()) Collections.addAll(candidates, options);

        // Sacola embaralhada: mesmo clicando várias vezes antes de salvar, percorre
        // todas as opções menos usadas antes de repetir uma espécie/classe/antecedente.
        StringBuilder keyBuilder = new StringBuilder(field);
        for (String option : options) keyBuilder.append('|').append(norm(option));
        String bagKey = keyBuilder.toString();
        ArrayList<String> bag = selectionBags.get(bagKey);
        if (bag == null) { bag = new ArrayList<String>(); selectionBags.put(bagKey, bag); }
        for (int i = bag.size() - 1; i >= 0; i--) if (!containsNormalized(candidates, bag.get(i))) bag.remove(i);
        if (bag.isEmpty()) { bag.addAll(candidates); Collections.shuffle(bag, random); }
        return bag.remove(0);
    }

    private boolean containsNormalized(List<String> values, String target) {
        String n = norm(target); for (String value : values) if (norm(value).equals(n)) return true; return false;
    }

    private String speciesOriginSentence(String species, String origin, String culture) {
        String s = species == null ? "Humano" : species;
        String n = norm(s), place = origin == null || origin.trim().isEmpty() ? "uma comunidade viajante" : origin.toLowerCase(PT);
        if (n.contains("meio elfo")) return "nasceu em " + place + " e aprendeu a transitar entre comunidades humanas e élficas sem reduzir sua identidade a nenhuma delas";
        if (n.contains("meio orc")) return "nasceu em " + place + " e construiu uma identidade própria entre heranças humanas e orcs, sem aceitar que outros decidissem quem deveria ser";
        if (n.contains("anao")) return "nasceu em " + place + " e foi criado entre tradições anãs de ofício, memória e palavra dada";
        if (n.contains("elfo")) return "nasceu em " + place + " e cresceu entre costumes élficos, aprendendo a observar mudanças que outros ignoravam";
        if (n.contains("halfling")) return "nasceu em " + place + " e cresceu numa comunidade halfling onde hospitalidade e coragem caminhavam juntas";
        if (n.contains("draconato")) return "nasceu em " + place + " e aprendeu desde cedo a honrar sua linhagem dracônica sem ser prisioneiro dela";
        if (n.contains("gnomo")) return "nasceu em " + place + " e foi criado entre curiosidade gnômica, histórias improváveis e trabalho minucioso";
        if (n.contains("golias")) return "nasceu em " + place + " e cresceu numa comunidade golias que media grandeza por responsabilidade, não por força";
        if (n.equals("orc")) return "nasceu em " + place + " e aprendeu a carregar com orgulho sua herança orc, apesar dos preconceitos de forasteiros";
        if (n.contains("tiefling")) return "nasceu em " + place + " e transformou a desconfiança alheia em atenção, independência e humor afiado";
        if (n.contains("aasimar")) return "nasceu em " + place + " e cresceu tentando separar sua própria vontade das expectativas ligadas à sua herança celestial";
        if(CrossBookCharacterOptionEngine.isKnownSpecies(s)) return "nasceu em " + place + " e cresceu entre tradições de sua própria espécie ("+s+"), preservando vínculos e identidade";
        return "nasceu em " + place + " e cresceu entre costumes humanos diversos, aprendendo a adaptar-se sem perder a própria identidade";
    }

    private String speciesCulture(String species) { return speciesIdentityAdjective(species); }
    private String speciesRootAdjective(String species) {
        String n = norm(species);
        if (n.contains("meio elfo")) return "humanas e élficas";
        if (n.contains("meio orc")) return "humanas e orcs";
        if (n.contains("anao")) return "anãs";
        if (n.contains("elfo")) return "élficas";
        if (n.contains("halfling")) return "halflings";
        if (n.contains("draconato")) return "dracônicas";
        if (n.contains("gnomo")) return "gnômicas";
        if (n.contains("golias")) return "golias";
        if (n.equals("orc")) return "orcs";
        if (n.contains("tiefling")) return "tieflings";
        if (n.contains("aasimar")) return "celestiais";
        if(CrossBookCharacterOptionEngine.isKnownSpecies(species)) return species;
        return "humanas";
    }
    private String speciesIdentityAdjective(String species) {
        String n = norm(species);
        if (n.contains("meio elfo")) return "meio-élfica";
        if (n.contains("meio orc")) return "meio-orc";
        if (n.contains("anao")) return "anã";
        if (n.contains("elfo")) return "élfica";
        if (n.contains("halfling")) return "halfling";
        if (n.contains("draconato")) return "dracônica";
        if (n.contains("gnomo")) return "gnômica";
        if (n.contains("golias")) return "golias";
        if (n.equals("orc")) return "orc";
        if (n.contains("tiefling")) return "tiefling";
        if (n.contains("aasimar")) return "celestial";
        if(CrossBookCharacterOptionEngine.isKnownSpecies(species)) return species;
        return "humana";
    }

    private String speciesSpeed(String species,String profile) {
        String volo=VoloSubraceHandbookEngine.speedOverride(species);if(volo.length()>0)return volo;String volo139=VoloSubraceRun139Engine.speedOverride(species);if(volo139.length()>0)return volo139;String volo140=VoloSubraceRun140Engine.speedOverride(species);if(volo140.length()>0)return volo140;String r141=Run141SpeciesFeatRuntimeEngine.speedOverride(species);if(r141.length()>0)return r141;String multi=MultiverseCharacterOptionEngine.speedOverride(species);if(multi.length()>0)return multi;String cross=CrossBookCharacterOptionEngine.speedOverride(species,profile);if(cross.length()>0)return cross;
        String n = norm(species);
        if(!rules.is2024(profile)&&(n.contains("anao")||n.contains("halfling")||n.contains("gnomo")))return "7,5 m";
        if (n.contains("elfo da floresta")) return "10,5 m";
        if (n.contains("golias")) return "10,5 m";
        return "9 m";
    }

    private String savingThrows(String cls, JSONObject o) {
        String c = norm(cls);String first = "Força", second = "Constituição";
        if (c.contains("bardo")) { first="Destreza"; second="Carisma"; }
        else if (c.contains("bruxo") || c.contains("feiticeiro") || c.contains("paladino")) { first="Sabedoria"; second="Carisma"; }
        else if (c.contains("clerigo")) { first="Sabedoria"; second="Carisma"; }
        else if (c.contains("druida")) { first="Inteligência"; second="Sabedoria"; }
        else if (c.equals("xama")) { first="Sabedoria"; second="Carisma"; }
        else if (c.contains("ladino")) { first="Destreza"; second="Inteligência"; }
        else if (c.contains("mago")) { first="Inteligência"; second="Sabedoria"; }
        else if (c.contains("monge") || c.contains("patrulheiro")) { first="Força"; second="Destreza"; }
        else if (c.contains("cozinheiro")) { first="Constituição"; second="Carisma"; }
        else if (c.contains("cacador de sangue")) { first="Força"; second="Sabedoria"; }
        else if (c.contains("necromante") || c.contains("cronomancer")) { first="Inteligência"; second="Sabedoria"; }
        else if (c.contains("cavaleiro da morte")) { first="Constituição"; second="Carisma"; }
        int pb = rules.proficiencyBonus(o.optInt("level",1));String[] abilities={"Força","Destreza","Constituição","Inteligência","Sabedoria","Carisma"};StringBuilder out=new StringBuilder();
        for(String ability:abilities){boolean proficient=ability.equals(first)||ability.equals(second);int bonus=abilityMod(o,ability)+(proficient?pb:0);if(out.length()>0)out.append(" • ");out.append(ability).append(' ').append(signed(bonus));if(proficient)out.append(" ★");}
        return out.toString();
    }

    private String skillsFor(String cls, String bg, JSONObject o) {
        String c = norm(cls);ArrayList<String> proficient = new ArrayList<String>();ArrayList<String> expertise = new ArrayList<String>();Collections.addAll(proficient,backgroundSkills(bg,rules.is2024(o.optString("rules_source"))));
        String[] pool=classSkillPool(c);int count=c.contains("ladino")?4:(c.contains("bardo")||c.contains("patrulheiro")||c.contains("cozinheiro")?3:2);ArrayList<String> classChosen=new ArrayList<String>();
        for(String skill:pool)if(!containsNormalized(proficient,skill)&&classChosen.size()<count){proficient.add(skill);classChosen.add(skill);}
        if(c.contains("ladino"))for(int i=0;i<Math.min(2,classChosen.size());i++)expertise.add(classChosen.get(i));
        int pb = rules.proficiencyBonus(o.optInt("level",1));String[] all={"Acrobacia","Adestrar Animais","Arcanismo","Atletismo","Atuação","Enganação","Furtividade","História","Intimidação","Intuição","Investigação","Medicina","Natureza","Percepção","Persuasão","Prestidigitação","Religião","Sobrevivência"};StringBuilder out=new StringBuilder();
        for(String skill:all){boolean expert=containsNormalized(expertise,skill);boolean prof=expert||containsNormalized(proficient,skill);int bonus=abilityMod(o,skillAbility(skill))+(prof?pb:0)+(expert?pb:0);if(out.length()>0)out.append(" • ");out.append(skill).append(' ').append(signed(bonus));if(expert)out.append(" ◈");else if(prof)out.append(" ★");}
        return out.toString();
    }

    private String[] backgroundSkills(String background,boolean revised){
        String b=norm(background);
        if(b.contains("acolito"))return new String[]{"Intuição","Religião"};
        if(b.contains("andarilho"))return new String[]{"Furtividade","Intuição"};
        if(revised&&b.equals("artesao"))return new String[]{"Investigação","Persuasão"};
        if(b.contains("artesao"))return new String[]{"Intuição","Persuasão"};
        if(b.contains("artista"))return new String[]{"Acrobacia","Atuação"};
        if(b.contains("charlatao"))return new String[]{"Enganação","Prestidigitação"};
        if(b.contains("criminoso"))return revised?new String[]{"Furtividade","Prestidigitação"}:new String[]{"Enganação","Furtividade"};
        if(b.contains("eremita"))return new String[]{"Medicina","Religião"};
        if(b.contains("escriba"))return new String[]{"Investigação","Percepção"};
        if(b.contains("fazendeiro"))return new String[]{"Adestrar Animais","Natureza"};
        if(b.contains("guarda"))return new String[]{"Atletismo","Percepção"};
        if(b.equals("guia"))return new String[]{"Furtividade","Sobrevivência"};
        if(b.contains("marinheiro"))return revised?new String[]{"Acrobacia","Percepção"}:new String[]{"Atletismo","Percepção"};
        if(b.contains("mercador"))return new String[]{"Adestrar Animais","Persuasão"};
        if(b.contains("nobre"))return new String[]{"História","Persuasão"};
        if(b.contains("sabio"))return new String[]{"Arcanismo","História"};
        if(b.contains("soldado"))return new String[]{"Atletismo","Intimidação"};
        if(b.contains("forasteiro"))return new String[]{"Atletismo","Sobrevivência"};
        if(b.contains("heroi do povo"))return new String[]{"Adestrar Animais","Sobrevivência"};
        if(b.contains("orfao"))return new String[]{"Furtividade","Prestidigitação"};
        return new String[0];
    }
    private String[] classSkillPool(String c){
        if(c.contains("barbar"))return new String[]{"Atletismo","Percepção","Sobrevivência","Intimidação","Natureza","Adestrar Animais"};
        if(c.contains("bardo"))return new String[]{"Persuasão","Atuação","Enganação","Intuição","Percepção","Acrobacia","Arcanismo","Atletismo","Furtividade","História","Intimidação","Investigação","Medicina","Natureza","Prestidigitação","Religião","Sobrevivência","Adestrar Animais"};
        if(c.contains("clerigo"))return new String[]{"Intuição","Religião","Medicina","História","Persuasão"};
        if(c.contains("druida"))return new String[]{"Natureza","Sobrevivência","Percepção","Intuição","Medicina","Religião","Arcanismo","Adestrar Animais"};
        if(c.contains("guerreiro"))return new String[]{"Atletismo","Percepção","Intimidação","Acrobacia","História","Intuição","Sobrevivência","Adestrar Animais"};
        if(c.contains("ladino"))return new String[]{"Furtividade","Prestidigitação","Investigação","Percepção","Acrobacia","Atletismo","Enganação","Intimidação","Intuição","Atuação","Persuasão"};
        if(c.contains("mago"))return new String[]{"Arcanismo","Investigação","História","Intuição","Medicina","Religião"};
        if(c.contains("monge"))return new String[]{"Acrobacia","Atletismo","Furtividade","Intuição","História","Religião"};
        if(c.contains("paladino"))return new String[]{"Atletismo","Persuasão","Intuição","Intimidação","Medicina","Religião"};
        if(c.contains("patrulheiro"))return new String[]{"Sobrevivência","Percepção","Furtividade","Natureza","Atletismo","Intuição","Investigação","Adestrar Animais"};
        if(c.contains("feiticeiro"))return new String[]{"Arcanismo","Persuasão","Enganação","Intimidação","Intuição","Religião"};
        if(c.contains("cozinheiro"))return new String[]{"Adestrar Animais","Arcanismo","Atletismo","Medicina","Natureza","Atuação","Persuasão","Prestidigitação","Sobrevivência"};
        if(c.contains("cacador de sangue"))return new String[]{"Atletismo","Acrobacia","Arcanismo","Intuição","Investigação","Sobrevivência"};
        if(c.contains("necromante"))return new String[]{"Arcanismo","Religião"};
        if(c.contains("cronomancer"))return new String[]{"Arcanismo","História","Intuição","Investigação","Percepção","Persuasão","Religião"};
        if(c.contains("cavaleiro da morte"))return new String[]{"Arcanismo","Atletismo","História","Intimidação","Medicina","Religião"};
        if(c.equals("xama"))return new String[]{"Adestrar Animais","Intuição","Medicina","Natureza","Percepção","Religião","Sobrevivência"};
        return new String[]{"Arcanismo","Investigação","História","Intimidação","Enganação","Natureza","Religião"};
    }

    private void addUnique(List<String> values,String value){if(!containsNormalized(values,value))values.add(value);}
    private int skillRank(String skills,String name){
        if(skills==null||skills.trim().isEmpty())return 0;String needle=norm(name);
        for(String part:skills.split("[•\n]")){String n=norm(part);if(!n.startsWith(needle+" ")&&!n.equals(needle))continue;if(part.contains("◈")||n.contains("especializacao"))return 2;if(part.contains("★")||n.contains("proficiente"))return 1;return 0;}
        return 0;
    }

    private String passiveSenses(JSONObject o, String skills) {
        int pb = rules.proficiencyBonus(o.optInt("level",1));
        int per = 10 + rules.modifier(o.optInt("wis",10)) + skillRank(skills,"Percepção")*pb;
        int inv = 10 + rules.modifier(o.optInt("int_score",10)) + skillRank(skills,"Investigação")*pb;
        int ins = 10 + rules.modifier(o.optInt("wis",10)) + skillRank(skills,"Intuição")*pb;
        return "Percepção "+per+" • Investigação "+inv+" • Intuição "+ins;
    }

    private String languagesFor(String species,String profile) {
        if(rules.is2024(profile))return "Comum e dois idiomas padrão escolhidos para a origem (2024)";
        String n = norm(species);
        if (n.contains("anao")) return "Comum, Anão e um idioma adicional coerente com o antecedente";
        if (n.contains("elfo")) return "Comum, Élfico e um idioma adicional coerente com o antecedente";
        if (n.contains("halfling")) return "Comum, Halfling e um idioma adicional coerente com o antecedente";
        if (n.contains("draconato")) return "Comum, Dracônico e um idioma adicional coerente com o antecedente";
        if (n.contains("gnomo")) return "Comum, Gnômico e um idioma adicional coerente com o antecedente";
        if (n.equals("orc") || n.contains("meio orc")) return "Comum, Orc e um idioma adicional coerente com o antecedente";
        if (n.contains("tiefling")) return "Comum, Infernal e um idioma adicional coerente com o antecedente";
        if (n.contains("aasimar")) return "Comum, Celestial e um idioma adicional coerente com o antecedente";
        return "Comum e dois idiomas coerentes com o antecedente";
    }

    private String toolsFor(String background, String cls,String profile) {
        String b = norm(background), c = norm(cls);
        if(rules.is2024(profile)){
            if(b.contains("acolito")||b.contains("escriba")||b.contains("sabio"))return "Suprimentos de Calígrafo";
            if(b.contains("andarilho")||b.contains("criminoso"))return "Ferramentas de Ladrão";
            if(b.equals("artesao"))return "Um tipo de Ferramentas de Artesão";
            if(b.contains("artista"))return "Um Instrumento Musical";
            if(b.contains("charlatao"))return "Kit de Falsificação";
            if(b.contains("eremita"))return "Kit de Herbalismo";
            if(b.contains("fazendeiro"))return "Ferramentas de Carpinteiro";
            if(b.contains("guarda")||b.contains("nobre")||b.contains("soldado"))return "Um Kit de Jogos";
            if(b.equals("guia"))return "Ferramentas de Cartógrafo";
            if(b.contains("marinheiro")||b.contains("mercador"))return "Ferramentas de Navegador";
        }
        if (c.contains("ladino") || b.contains("criminoso")) return "Ferramentas de ladrão";
        if (b.contains("artesao")) return "Ferramentas de artesão adequadas ao ofício";
        if (b.contains("artista") || c.contains("bardo")) return "Um instrumento musical e ferramentas de apresentação";
        if (b.contains("marinheiro")) return "Ferramentas de navegador e veículos aquáticos";
        if (b.contains("soldado")) return "Jogo de dados ou cartas e veículos terrestres";
        if (b.contains("charlatao")) return "Kit de falsificação e kit de disfarce";
        if(c.equals("xama"))return "Kit de herbalismo";
        return "Ferramenta coerente com o antecedente";
    }

    private String backgroundFeat2024(String background){
        String b=norm(background);
        if(b.contains("acolito"))return "Iniciado em Magia (Clérigo)";
        if(b.contains("andarilho")||b.contains("mercador"))return "Sortudo";
        if(b.equals("artesao"))return "Artifista";
        if(b.contains("artista"))return "Músico";
        if(b.contains("charlatao")||b.contains("escriba")||b.contains("nobre"))return "Habilidoso";
        if(b.contains("criminoso")||b.contains("guarda"))return "Alerta";
        if(b.contains("eremita"))return "Curandeiro";
        if(b.contains("fazendeiro"))return "Vigoroso";
        if(b.equals("guia"))return "Iniciado em Magia (Druida)";
        if(b.contains("marinheiro"))return "Valentão de Taverna";
        if(b.contains("sabio"))return "Iniciado em Magia (Mago)";
        return "Atacante Selvagem";
    }

    private String proficienciesFor(String cls) {
        String c = norm(cls);
        if (c.contains("barbar")) return "Armaduras leves e médias, escudos, armas simples e marciais";
        if (c.contains("guerreiro") || c.contains("paladino")) return "Todas as armaduras, escudos, armas simples e marciais";
        if (c.contains("ladino")) return "Armaduras leves, armas simples, armas finesse selecionadas, ferramentas de ladrão";
        if (c.contains("mago") || c.contains("feiticeiro")) return "Adagas, bordões, bestas leves e focos de conjuração permitidos";
        if (c.contains("clerigo")) return "Armaduras leves e médias, escudos, armas simples, símbolo sagrado";
        if (c.contains("bardo")) return "Armaduras leves, armas simples, instrumentos musicais";
        if (c.contains("cozinheiro")) return "Armaduras leves e médias; armas simples, besta leve, espada curta, espada longa e rapieira; utensílios de cozinheiro";
        if (c.contains("cacador de sangue")) return "Armaduras leves e médias, escudos, armas simples e marciais, suprimentos de alquimista";
        if (c.contains("necromante")) return "Sem armadura; armas simples e foice; símbolo profano";
        if (c.contains("cronomancer")) return "Armaduras leves, escudos, adagas, bordões e espadas curtas, kit de jogos";
        if (c.contains("cavaleiro da morte")) return "Armaduras leves e médias; armas corpo a corpo simples e marciais; sem armas à distância/escudos; ferramentas de ferraria";
        return "Proficiências de classe e antecedente conforme os manuais habilitados";
    }

    private String attacksFor(String cls, JSONObject o) {
        int pb = rules.proficiencyBonus(o.optInt("level",1)); String c = norm(cls);
        int str = rules.modifier(o.optInt("str",10)), dex = rules.modifier(o.optInt("dex",10));
        boolean revised=rules.is2024(o.optString("rules_source"));String unarmed=revised?"Desarmado | "+signed(str+pb)+" / CD "+(8+str+pb)+" | dano, agarrar ou empurrar | corpo a corpo":"Desarmado | "+signed(str+pb)+" | 1"+signed(str)+" contundente | corpo a corpo";
        if (c.contains("barbar")) return "Machado grande | "+signed(str+pb)+" | 1d12"+signed(str)+" cortante | corpo a corpo"+(revised?", maestria da arma":"")+"\nMachadinha | "+signed(str+pb)+" | 1d6"+signed(str)+" cortante | 6/18 m\n"+unarmed;
        if (c.contains("ladino")) return "Arco curto | "+signed(dex+pb)+" | 1d6"+signed(dex)+" perfurante | 24/96 m\nEspada curta | "+signed(dex+pb)+" | 1d6"+signed(dex)+" perfurante | finesse, leve\nAdaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | 6/18 m, finesse, leve\nAtaque Furtivo | — | +1d6 uma vez por turno | condição atendida";
        if (revised && c.contains("bardo")) return "Adaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | 6/18 m, finesse, leve\nZombaria Viciosa | CD "+(8+spellMod(o,cls)+pb)+" | conforme magia | alcance da magia";
        if (!revised && c.contains("bardo")) return "Florete | "+signed(dex+pb)+" | 1d8"+signed(dex)+" perfurante | finesse\nArco curto | "+signed(dex+pb)+" | 1d6"+signed(dex)+" perfurante | 24/96 m\nZombaria Viciosa | CD "+(8+spellMod(o,cls)+pb)+" | efeito da magia";
        if (c.contains("clerigo")) return "Maça | "+signed(str+pb)+" | 1d6"+signed(str)+" contundente | corpo a corpo\n"+unarmed;
        if (revised && c.contains("druida")) return "Foice | "+signed(str+pb)+" | 1d4"+signed(str)+" cortante | leve\nBordão | "+signed(str+pb)+" | 1d6"+signed(str)+" contundente | versátil 1d8\n"+unarmed;
        if (!revised && c.contains("druida")) return "Cimitarra | "+signed(dex+pb)+" | 1d6"+signed(dex)+" cortante | finesse\n"+unarmed;
        if (c.contains("guerreiro")) {
            if(revised)return "Espada grande | "+signed(str+pb)+" | 2d6"+signed(str)+" cortante | pesada, duas mãos\nMangual | "+signed(str+pb)+" | 1d8"+signed(str)+" contundente | corpo a corpo\nAzagaia | "+signed(str+pb)+" | 1d6"+signed(str)+" perfurante | 9/36 m\n"+unarmed;
            return "Espada longa | "+signed(str+pb)+" | 1d8"+signed(str)+" cortante | versátil\nArco curto | "+signed(dex+pb)+" | 1d6"+signed(dex)+" perfurante | 24/96 m\n"+unarmed;
        }
        if (c.contains("paladino")) return "Espada longa | "+signed(str+pb)+" | 1d8"+signed(str)+" cortante | versátil\nAzagaia | "+signed(str+pb)+" | 1d6"+signed(str)+" perfurante | 9/36 m\n"+unarmed;
        if (c.contains("patrulheiro")) return "Cimitarra | "+signed(dex+pb)+" | 1d6"+signed(dex)+" cortante | finesse, leve\nEspada curta | "+signed(dex+pb)+" | 1d6"+signed(dex)+" perfurante | finesse, leve\nArco longo | "+signed(dex+pb)+" | 1d8"+signed(dex)+" perfurante | 45/180 m\n"+unarmed;
        if (c.contains("monge")) return revised?Character2024SheetConsistencyGuard.monkAttacks(o):"Lança | "+signed(str+pb)+" | 1d6"+signed(str)+" perfurante | versátil\nAdaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | finesse, leve\n"+unarmed;
        if (revised && c.contains("feiticeiro")) return "Lança | "+signed(str+pb)+" | 1d6"+signed(str)+" perfurante | versátil\nAdaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | finesse, leve\nTruque de ataque | "+signed(spellMod(o,cls)+pb)+" | conforme magia | alcance da magia";
        if (revised && c.contains("bruxo")) return "Foice | "+signed(str+pb)+" | 1d4"+signed(str)+" cortante | leve\nAdaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | finesse, leve\nTruque de ataque | "+signed(spellMod(o,cls)+pb)+" | conforme magia | alcance da magia";
        if (revised && c.contains("mago")) return "Bordão | "+signed(str+pb)+" | 1d6"+signed(str)+" contundente | versátil 1d8\nAdaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | finesse, leve\nTruque de ataque | "+signed(spellMod(o,cls)+pb)+" | conforme magia | alcance da magia";
        if (!revised && (c.contains("mago") || c.contains("feiticeiro") || c.contains("bruxo"))) return "Adaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | 6/18 m\nTruque de ataque | "+signed(spellMod(o,cls)+pb)+" | conforme magia | alcance da magia";
        if (c.contains("cozinheiro")) return "Espada longa | "+signed(str+pb)+" | 1d8"+signed(str)+" cortante | versátil\nBesta leve | "+signed(dex+pb)+" | 1d8"+signed(dex)+" perfurante | 24/96 m\nPetisco | CD "+CookingRulesEngine.snackSaveDc(pb,rules.modifier(o.optInt("con",10)))+" | efeito conforme petisco compilado | toque";
        if(c.contains("cacador de sangue"))return "Arma marcial | "+signed(Math.max(str,dex)+pb)+" | conforme arma + Ritual Vermelho | ritual exige ativação explícita";
        if(c.contains("cavaleiro da morte"))return "Arma corpo a corpo rúnica | "+signed(str+pb)+" | conforme arma | Forja Rúnica\nFunil da Morte | CD "+(8+pb+rules.modifier(o.optInt("con",10)))+" | 2d8/3d8 necrótico | salvaguarda manual";
        if(c.contains("necromante")||c.contains("cronomancer"))return "Adaga | "+signed(dex+pb)+" | 1d4"+signed(dex)+" perfurante | finesse\nAtaque mágico | "+signed(spellMod(o,cls)+pb)+" | conforme magia | rolagem manual";
        if(c.equals("xama"))return "Bordão | "+signed(str+pb)+" | 1d6"+signed(str)+" contundente | versátil\nLança | "+signed(str+pb)+" | 1d6"+signed(str)+" perfurante | versátil\nAtaque mágico | "+signed(spellMod(o,cls)+pb)+" | conforme magia | rolagem manual";
        return unarmed;
    }

    private String featuresFor(String cls, String species, int level,String profile) {
        String c = norm(cls);boolean revised=rules.is2024(profile);StringBuilder b = new StringBuilder();
        if (c.contains("barbar")) b.append("Fúria; Defesa sem Armadura").append(revised?"; Maestria de Armas":"");
        else if (c.contains("ladino")) b.append("Ataque Furtivo; Especialização; Gíria de Ladrão").append(revised?"; Maestria de Armas":"");
        else if (c.contains("bardo")) b.append("Inspiração de Bardo; Conjuração; recursos do antecedente");
        else if (c.contains("guerreiro")) b.append("Retomar o Fôlego; Estilo de Luta").append(revised?"; Maestria de Armas":"");
        else if (c.contains("mago")) b.append("Conjuração; Recuperação Arcana; Livro de Magias");
        else if (c.contains("clerigo")) b.append("Conjuração Divina; Canalizar Divindade quando disponível");
        else if (c.contains("monge")) b.append("Artes Marciais; Defesa sem Armadura").append(level>=2?(revised?"; Foco do Monge; Movimento sem Armadura; Metabolismo Sobrenatural":"; Ki; Movimento sem Armadura"):"");
        else if (c.contains("cozinheiro")) b.append("Bolsa de Cozinheiro; Petiscos; Temperos Aromáticos; pacote compilado Cozinheiro ativo");
        else if (c.contains("cacador de sangue")) b.append("Perdição do Caçador; Ritual Vermelho").append(level>=2?"; Sangue Maldito":"").append(level>=3?"; Ordem do Caçador de Sangue":"");
        else if (c.contains("necromante")) b.append("Conjuração; Recuperação de Magia Negra; Canalizar Necromancia").append(level>=2?"; Caminho de Necromante":"");
        else if (c.contains("cronomancer")) b.append("Conjuração; Linguística Eterna").append(level>=2?"; Roda do Tempo":"").append(level>=3?"; Jornada Perpétua":"");
        else if (c.contains("cavaleiro da morte")) b.append("Funil da Morte; Forja Rúnica; Renegado").append(level>=2?"; Conjuração; Estilo de Luta":"").append(level>=3?"; Origem Sombria":"");
        else if(c.equals("xama")) b.append("Conjuração; Ritualista; Sentido Espiritual").append(level>=2?"; Guia Espiritual; Bênçãos Espirituais":"").append(level>=3?"; Chamado para o Vidente":"").append(level>=10?"; Defesa Espiritual":"").append(level>=17?"; Um com os Espíritos":"").append(level>=20?"; Visão Espiritual":"");
        else b.append("Recursos de classe de nível ").append(level).append(" conforme os manuais habilitados");
        b.append("\nTraços de espécie: ").append(species).append(" — visão, deslocamento, resistências e demais traços conforme o perfil de regras ativo.");
        return b.toString();
    }

    private String spellcastingFor(String cls, JSONObject o) {
        String c = norm(cls); if (!isCaster(c)) return "Não possui conjuração no nível atual.";
        if(o.optInt("level",1)<2&&!rules.is2024(o.optString("rules_source"))&&(c.contains("paladino")||c.contains("patrulheiro")))return "Não possui conjuração no nível 1 no perfil 2014.";
        int mod = spellMod(o, cls), pb = rules.proficiencyBonus(o.optInt("level",1));
        String ability = (c.contains("mago") || c.contains("artifice") || c.contains("necromante")) ? "Inteligência" : (c.contains("clerigo") || c.contains("druida") || c.contains("patrulheiro") || c.contains("guardiao") || c.contains("cronomancer") || c.equals("xama")) ? "Sabedoria" : c.contains("cavaleiro da morte") ? "Constituição" : "Carisma";
        return ability+" • CD "+(8+mod+pb)+" para resistir • "+signed(mod+pb)+" em ataques mágicos";
    }

    private String spellsFor(String cls, int level,String profile) {
        String c = norm(cls); if (!isCaster(c)) return "Sem magias; consulte os recursos de classe.";
        if(level>1)return "As magias de níveis superiores exigem escolhas do jogador confirmadas pelo livro ativo; a criação automática não inventa uma progressão.";
        if (c.contains("bardo")) return "Espaços: 2 de 1º círculo\nTruques: Zombaria Viciosa, Ilusão Menor\nPreparadas: Palavra Curativa, Sussurros Dissonantes, Enfeitiçar Pessoa, Risada Nefasta";
        if (c.contains("mago")) return "Espaços: 2 de 1º círculo\nTruques: Raio de Fogo, Mãos Mágicas, Prestidigitação\nLivro: Armadura Arcana, Escudo, Mísseis Mágicos, Sono, Detectar Magia, Queda Suave";
        if (c.contains("clerigo")) return "Espaços: 2 de 1º círculo\nTruques: Chama Sagrada, Orientação, Taumaturgia\nPreparadas: Curar Ferimentos, Palavra Curativa, Bênção, Santuário";
        if (c.contains("druida")) return "Espaços: 2 de 1º círculo\nTruques: Druidismo, Bordão Místico\nPreparadas: Curar Ferimentos, Fogo das Fadas, Enredar, Falar com Animais";
        if (c.contains("bruxo")) return "Espaços de pacto: 1 de 1º círculo\nTruques: Rajada Mística, Mãos Mágicas\nConhecidas: Repreensão Infernal, Bruxaria";
        if (c.contains("feiticeiro")) return "Espaços: 2 de 1º círculo\nTruques: Raio de Fogo, Prestidigitação, Mãos Mágicas, Luz\nConhecidas: Escudo, Orbe Cromática";
        if (c.contains("paladino")) return rules.is2024(profile)?"Espaços: 2 de 1º círculo\nPreparadas: Bênção, Curar Ferimentos":"Conjuração começa no nível 2 no perfil 2014; recursos atuais são marciais.";
        if (c.contains("patrulheiro")) return rules.is2024(profile)?"Espaços: 2 de 1º círculo\nPreparadas: Marca do Caçador, Curar Ferimentos":"Conjuração começa no nível 2 no perfil 2014; recursos atuais são marciais.";
        return "Magias e espaços conforme a classe, o nível e os manuais habilitados.";
    }

    private String inventoryFor(String cls, String background, String profile) {
        String c = norm(cls); String base;
        if (rules.is2024(profile)) {
            // Pacote A oficial das regras revisadas de 2024. O antecedente continua listado
            // separadamente porque também fornece seu próprio equipamento/moedas.
            if (c.contains("barbar")) base="Machado grande, 4 machadinhas, mochila de explorador, 15 PO";
            else if (c.contains("bardo")) base="Armadura de couro, 2 adagas, instrumento musical à escolha, mochila de artista, 19 PO";
            else if (c.contains("clerigo")) base="Camisão de malha, escudo, maça, símbolo sagrado, pacote de sacerdote, 7 PO";
            else if (c.contains("druida")) base="Armadura de couro, escudo, foice, foco druídico (bordão), mochila de explorador, kit de herbalismo, 9 PO";
            else if (c.contains("guerreiro")) base="Cota de malha, espada grande, mangual, 8 azagaias, mochila de explorador de masmorras, 4 PO";
            else if (c.contains("monge")) base="Lança, 5 adagas, ferramentas de artesão ou instrumento musical à escolha, mochila de explorador, 11 PO";
            else if (c.contains("paladino")) base="Cota de malha, escudo, espada longa, 6 azagaias, símbolo sagrado, pacote de sacerdote, 9 PO";
            else if (c.contains("patrulheiro")) base="Armadura de couro batido, cimitarra, espada curta, arco longo, 20 flechas, aljava, foco druídico (raminho de visco), mochila de explorador, 7 PO";
            else if (c.contains("ladino")) base="Armadura de couro, 2 adagas, espada curta, arco curto, 20 flechas, aljava, ferramentas de ladrão, mochila de assaltante, 8 PO";
            else if (c.contains("feiticeiro")) base="Lança, 2 adagas, foco arcano (cristal), mochila de explorador de masmorras, 28 PO";
            else if (c.contains("bruxo")) base="Armadura de couro, foice, 2 adagas, foco arcano (orbe), livro de conhecimento oculto, mochila de estudioso, 15 PO";
            else if (c.contains("mago")) base="2 adagas, foco arcano (bordão), robe, livro de magias, mochila de estudioso, 5 PO";
            else if (c.contains("cozinheiro")) base="Equipamento inicial da classe caseira Cozinheiro conforme o pacote compilado ativo";
            else if(c.contains("cacador de sangue"))base="Arma marcial ou duas simples; besta leve + 20 virotes ou besta de mão; couro batido ou brunea; pacote de explorador; suprimentos de alquimista";
            else if(c.contains("necromante"))base="Armas simples/foice, símbolo profano e equipamento conforme a fonte HB Necromante";
            else if(c.contains("cronomancer"))base="Armadura leve, escudo, adaga/bordão/espada curta, kit de jogos e equipamento conforme HB Cronomancer";
            else if(c.contains("cavaleiro da morte"))base="Duas armas corpo a corpo simples ou marciais; armadura de couro; pacote de explorador; adaga; 4 azagaias; ferramentas de ferraria";
            else if(c.equals("xama"))base="Clava ou lança; bordão; armadura média ou leve; escudo; kit de herbalismo; foco druídico; pacote de explorador";
            else base="Equipamento inicial da classe confirmado pelo perfil ativo";
        } else {
            if (c.contains("mago")) base="Livro de magias, foco arcano, adaga, mochila de estudioso";
            else if (c.contains("ladino")) base="Armadura de couro, espada curta, 2 adagas, arco curto, 20 flechas, ferramentas de ladrão, mochila de assaltante";
            else if (c.contains("clerigo")) base="Brunea, escudo, maça, símbolo sagrado, pacote de sacerdote";
            else if (c.contains("barbar")) base="Machado grande, 4 machadinhas, mochila de explorador, corda de 15 m, rações e cantil";
            else if (c.contains("bardo")) base="Armadura de couro, florete, arco curto, instrumento musical, mochila de artista";
            else if (c.contains("druida")) base="Armadura de couro, escudo de madeira, cimitarra, foco druídico, mochila de explorador";
            else if (c.contains("guerreiro")||c.contains("paladino")) base="Cota de malha, escudo, espada longa, arma à distância, mochila de aventureiro";
            else if (c.contains("patrulheiro")) base="Brunea, duas espadas curtas, arco longo, 20 flechas, mochila de explorador";
            else if (c.contains("monge")) base="Sem armadura, lança, 5 adagas, mochila de aventureiro";
            else if (c.contains("bruxo")) base="Armadura de couro, foco arcano, arma simples, 2 adagas, mochila de estudioso";
            else if (c.contains("feiticeiro")) base="Sem armadura, foco arcano, duas adagas, mochila de explorador";
            else if (c.contains("cozinheiro")) base="Espada longa ou besta leve com 20 virotes; armadura de couro ou brunea; duas adagas ou dois martelos leves; pacote de explorador ou estudioso; utensílios e Bolsa de Cozinheiro";
            else if(c.contains("cacador de sangue"))base="Arma marcial ou duas simples; besta leve + 20 virotes ou besta de mão; couro batido ou brunea; pacote de explorador; suprimentos de alquimista";
            else if(c.contains("necromante"))base="Armas simples/foice, símbolo profano e equipamento conforme a fonte HB Necromante";
            else if(c.contains("cronomancer"))base="Armadura leve, escudo, adaga/bordão/espada curta, kit de jogos e equipamento conforme HB Cronomancer";
            else if(c.contains("cavaleiro da morte"))base="Duas armas corpo a corpo simples ou marciais; armadura de couro; pacote de explorador; adaga; 4 azagaias; ferramentas de ferraria";
            else if(c.equals("xama"))base="Clava ou lança; bordão; armadura média ou leve; escudo; kit de herbalismo; foco druídico; pacote de explorador";
            else base="Equipamento inicial da classe confirmado pelo perfil ativo";
        }
        return base+"\nAntecedente "+background+": equipamento e moedas conforme o antecedente do perfil ativo.";
    }

    private String roleForClass(String cls) {
        String c=norm(cls);
        if(c.contains("barbar")||c.contains("guerreiro")||c.contains("paladino"))return "Linha de frente";
        if(c.contains("ladino")||c.contains("patrulheiro"))return "Exploração, perícias e dano preciso";
        if(c.contains("bardo")||c.contains("clerigo")||c.contains("druida")||c.contains("cozinheiro"))return "Suporte, controle e interação";
        if(c.contains("mago")||c.contains("feiticeiro")||c.contains("bruxo")||c.contains("necromante")||c.contains("cronomancer"))return "Magia, controle e conhecimento";
        if(c.contains("cacador de sangue")||c.contains("cavaleiro da morte"))return "Linha de frente e recursos sobrenaturais";
        return "Versátil";
    }

    private String conceptFor(String species,String cls,String bg) {
        String[] hooks={
                "um antigo profissional que transformou um fracasso em juramento",
                "uma pessoa comum que descobriu ser muito boa em situações impossíveis",
                "um especialista desacreditado que procura uma prova capaz de limpar seu nome",
                "um protetor relutante acompanhado por um segredo que insiste em reaparecer",
                "um viajante gentil cuja paciência termina quando alguém ameaça inocentes"
        };
        return species+" • "+cls+" • "+bg+" — "+pick(hooks)+".";
    }

    private String appearanceFor(String species,String cls) {
        String n=norm(species); String stature;
        if(n.contains("anao"))stature="baixa estatura, constituição robusta";
        else if(n.contains("halfling")||n.contains("gnomo"))stature="pequena estatura, movimentos vivos";
        else if(n.contains("golias"))stature="estatura muito alta, presença imponente";
        else stature="estatura mediana, postura marcada pela vida de aventureiro";
        String[] hair={"cabelos negros encaracolados","cabelos ruivos curtos","cabelos loiros presos em tranças","cabeça raspada com tatuagens discretas","cabelos castanhos compridos","cabelos grisalhos precoces"};
        String[] skin={"pele escura","pele morena","pele clara com sardas","pele bronzeada pelo sol","pele oliva","pele marcada por cicatrizes antigas"};
        return species+", "+stature+", "+pick(skin)+", "+pick(hair)+", olhos atentos; veste equipamento coerente com a classe "+cls+" e mantém a mesma aparência entre cenas.";
    }

    private String historyFor(String species,String cls,String bg) {
        return "Cresceu numa comunidade coerente com sua identidade "+species+" e trabalhou como "+bg.toLowerCase(PT)+" antes de seguir o caminho de "+cls+". Um acontecimento mal resolvido levou essa pessoa a partir, carregando uma promessa, um contato útil e um rival que ainda conhece seus pontos fracos.";
    }

    private String levelUpFor(String cls) {
        return "Ao subir de nível, aplicar automaticamente PV, bônus de proficiência, recursos, espaços de magia e escolhas permitidas para "+cls+". Subclasse somente no nível previsto pelo perfil de regras e pelos manuais habilitados.";
    }

    private String careerForClass(String cls) {
        String c=norm(cls);
        if(c.contains("mago")||c.contains("artifice")||c.contains("feiticeiro"))return "Erudição e magia";
        if(c.contains("clerigo")||c.contains("druida")||c.contains("paladino"))return "Templo e peregrinação";
        if(c.contains("ladino")||c.contains("bruxo"))return "Crime e submundo";
        if(c.contains("bardo"))return "Corte e diplomacia";
        if(c.contains("patrulheiro"))return "Caça e exploração";
        if(c.contains("cozinheiro"))return "Comércio e ofício";
        return "Milícia e guerra";
    }

    private String randomName(String species) {
        String n=norm(species);
        if(n.contains("meio elfo"))return pick(new String[]{"Maelis Avelar","Cael Folha-Rocha","Iria Valeclaro","Tarian Monteverde","Nara Vento-Curto","Daren Lua-Parda"});
        if(n.contains("meio orc"))return pick(new String[]{"Ruka Monteiro","Dorn Vale-Forte","Shura Nascimento","Mogar Pedra-Cruz","Vola Ferraz","Karg Moura"});
        if(n.contains("anao"))return pick(new String[]{"Dagna Ferroclaro","Borin Sete-Pregos","Kelda Pedrafria","Thrain Cobre-Fino","Mara Escudo-Rubro","Orik Barba-de-Giz"});
        if(n.contains("elfo"))return pick(new String[]{"Aelar Folha-Lenta","Naivara Luz-do-Musgo","Thamior Vento-Pálido","Ilyrana Mel-da-Mata","Soveliss Ramo-Negro","Lethariel Chuva-Clara"});
        if(n.contains("halfling"))return pick(new String[]{"Pipa Três-Bolsos","Tobin Pé-de-Chá","Milo Duas-Colheres","Nessa Pulo-Curto","Rudi Bom-Barril","Lina Sete-Botões"});
        if(n.contains("draconato"))return pick(new String[]{"Arjhan Vharax","Balasar Kethend","Rhogar Nemeia","Akra Sora","Kriv Torinn","Surina Medrash"});
        if(n.contains("gnomo"))return pick(new String[]{"Nissa Engrenafina","Boddynock Faísca","Tana Parafuso","Wrenn Três-Lupas","Orla Pavio-Azul","Fizban Miúdo"});
        if(n.contains("golias"))return pick(new String[]{"Kavaki Aguenta-Trovão","Nalla Passo-de-Pedra","Eglath Quebra-Névoa","Vimak Marca-do-Gelo","Thava Guarda-Cume","Gauthak Eco-Forte"});
        if(n.equals("orc")||n.contains("meio orc"))return pick(new String[]{"Ruka Mão-Calma","Dorn Quebra-Corrente","Shura Olho-de-Ferro","Mogar Três-Juramentos","Vola Cinza-Clara","Karg Riso-Torto"});
        if(n.contains("tiefling"))return pick(new String[]{"Véspera Cinza","Misericórdia Rubra","Kael Vira-Chave","Nix Última-Luz","Ravel Sem-Sombra","Íris Brasa-Fria"});
        if(n.contains("aasimar"))return pick(new String[]{"Seren Alvorada","Mikael Luz-Pálida","Asha Voto-Claro","Cael Vigília","Neria Sete-Estrelas","Tamar Sol-Quieto"});
        return pick(new String[]{"Bruna Vale","Caio Monteiro","Iara Nascimento","Davi Rocha","Luzia Ferraz","Bento Cruz","Maya Avelar","Ravi Moura","Joana Teles","Noah Farias"});
    }

    private String randomNickname(String cls) {
        String c=norm(cls);
        if(c.contains("barbar")||c.contains("guerreiro"))return pick(new String[]{"Bate-Ponto","Escudo Torto","Última Porta","Martelo Calmo"});
        if(c.contains("ladino"))return pick(new String[]{"Sussurro","Passo Curto","Duas Chaves","Sem Pegadas"});
        if(c.contains("mago")||c.contains("feiticeiro"))return pick(new String[]{"Pavio Azul","Margem Riscada","Sete Sílabas","Olho de Giz"});
        if(c.contains("bardo"))return pick(new String[]{"Três-Bolsos","Voz Emprestada","Último Refrão","Santo de Feira"});
        return pick(new String[]{"Mão Firme","Mapa Dobrado","Bom Sinal","Copo Vazio"});
    }

    private String randomTitle(String cls) { return pick(new String[]{"a Cartógrafa", "o Campeão", "a Guardiã", "o Peregrino", "a Investigadora", "o Sem-Bandeira", "a Voz da Estrada", "o Último Aprendiz"}); }

    private int abilityMod(JSONObject o,String ability){return rules.modifier(abilityScore(o,ability));}
    private int abilityScore(JSONObject o,String ability){String a=norm(ability);if(a.startsWith("for"))return o.optInt("str",10);if(a.startsWith("des"))return o.optInt("dex",10);if(a.startsWith("con"))return o.optInt("con",10);if(a.startsWith("int"))return o.optInt("int_score",10);if(a.startsWith("sab"))return o.optInt("wis",10);return o.optInt("cha",10);}
    private String skillAbility(String skill){String s=norm(skill);if(s.contains("atletismo"))return "Força";if(s.contains("acrobacia")||s.contains("furtividade")||s.contains("prestidigitacao"))return "Destreza";if(s.contains("arcanismo")||s.contains("historia")||s.contains("investigacao")||s.contains("natureza")||s.contains("religiao"))return "Inteligência";if(s.contains("adestrar")||s.contains("intuicao")||s.contains("medicina")||s.contains("percepcao")||s.contains("sobrevivencia"))return "Sabedoria";return "Carisma";}
    private int spellMod(JSONObject o,String cls){String c=norm(cls);if(c.contains("mago")||c.contains("artifice")||c.contains("necromante"))return rules.modifier(o.optInt("int_score",10));if(c.contains("clerigo")||c.contains("druida")||c.contains("patrulheiro")||c.contains("guardiao")||c.contains("cronomancer"))return rules.modifier(o.optInt("wis",10));if(c.contains("cavaleiro da morte"))return rules.modifier(o.optInt("con",10));return rules.modifier(o.optInt("cha",10));}
    private boolean isCaster(String c){return c.contains("mago")||c.contains("feiticeiro")||c.contains("bruxo")||c.contains("bardo")||c.contains("clerigo")||c.contains("druida")||c.contains("paladino")||c.contains("patrulheiro")||c.contains("guardiao")||c.contains("artifice")||c.contains("necromante")||c.contains("cronomancer")||c.contains("cavaleiro da morte")||c.equals("xama");}
    private String mergeHistory(String history,String life){String h=history==null?"":history.trim(),l=life==null?"":life.trim();if(h.contains("HISTÓRIA DE VIDA"))return h;if(h.isEmpty())return l;if(l.isEmpty())return h;return h+"\n\n"+l;}
    private String baseClass(String full){if(full==null)return "";int i=full.indexOf(" — ");if(i<0)i=full.indexOf(" - ");return i>0?full.substring(0,i).trim():full.trim();}
    private String signed(int value){return value>=0?"+"+value:String.valueOf(value);}
    private String cleanChoice(String value,String fallback){return value==null||value.trim().isEmpty()?fallback:value.trim();}
    private int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private boolean chance(int percent){return random.nextInt(100)<percent;}
    private String pick(String[] values){return values[random.nextInt(values.length)];}
    private static String norm(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(PT),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private void putDefault(JSONObject o,String key,String value)throws Exception{if(o.optString(key).trim().isEmpty())o.put(key,value==null?"":value);}
    private void copyIfMissing(JSONObject o,String key,Object value)throws Exception{if(!o.has(key)||o.isNull(key)||String.valueOf(o.opt(key)).trim().isEmpty())o.put(key,value);}

    /** Pattern.quote sem exigir import de Pattern em todas as chamadas. */
    private static final class PatternEscaper { static String quote(String s){ return java.util.regex.Pattern.quote(s); } }
}
