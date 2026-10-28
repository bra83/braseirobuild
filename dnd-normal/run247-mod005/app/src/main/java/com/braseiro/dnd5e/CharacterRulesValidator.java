package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Segunda barreira mecânica: a IA pode enriquecer narrativa, porém não consegue
 * salvar uma ficha com valores básicos fora das regras habilitadas.
 */
public final class CharacterRulesValidator {
    private static final Set<String> CLASSES = new HashSet<String>(Arrays.asList(
            "bárbaro","barbaro","bardo","bruxo","clérigo","clerigo","druida","feiticeiro",
            "guerreiro","ladino","mago","monge","paladino","patrulheiro","artificer","artífice","artifice"));
    private static final Set<String> SPECIES = new HashSet<String>(Arrays.asList(
            "anão","anao","elfo","halfling","humano","gnomo","draconato","meio-elfo",
            "meio-orc","tiefling","tiferino","pequenino","golias","aasimar","goliath","orc","owlin"));
    private static final Set<String> BACKGROUNDS_2014 = new HashSet<String>(Arrays.asList(
            "acólito","artesão de guilda","artista","charlatão","criminoso","eremita",
            "forasteiro","herói do povo","marinheiro","nobre","órfão","sábio","soldado","estudante de lorehold","estudante de prismari","estudante de quandrix","estudante de silverquill","estudante de witherbloom"));
    private static final Set<String> BACKGROUNDS_2024 = new HashSet<String>(Arrays.asList(
            "acólito","andarilho","artesão","artista","charlatão","criminoso","eremita",
            "escriba","fazendeiro","guarda","guia","marinheiro","mercador","nobre","sábio","soldado"));

    public JSONObject validate(JSONObject sheet, String profile) {
        JSONArray errors = new JSONArray();
        JSONArray warnings = new JSONArray();
        JSONObject normalized;
        try { normalized = new JSONObject(sheet == null ? "{}" : sheet.toString()); }
        catch (Exception e) { normalized = new JSONObject(); }
        try {
            required(normalized, "name", "Nome", errors);
            required(normalized, "species", "Espécie", errors);
            required(normalized, "class_name", "Classe", errors);
            String rawClass = normalized.optString("class_name");
            String baseClassName = baseClassName(rawClass);
            String cls = lower(baseClassName);
            String species = lower(normalized.optString("species"));
            JSONObject classMeta = CharacterOptionCatalogEngine.lookupClass(baseClassName);
            boolean compiledSupplementClass = classMeta.optBoolean("known", false)
                    && (classMeta.optBoolean("mechanics_compiled", false) || classMeta.optBoolean("base_class_mechanics_compiled", false));
            boolean compiledHomebrewClass = HomebrewClassRuntimeEngine.supports(baseClassName);
            if (cls.length() > 0 && !containsKnown(CLASSES, cls) && !compiledSupplementClass && !compiledHomebrewClass && !SupplementContentRegistry.isKnownClass(cls))
                errors.put("Classe não reconhecida pelo núcleo ou pelos suplementos compilados: " + baseClassName + ".");
            if (species.length() > 0 && !containsKnown(SPECIES, species)
                    && !CharacterOptionCatalogEngine.lookupSpecies(normalized.optString("species")).optBoolean("known",false)
                    && !EberronForgeCompleteEngine.supportsSpecies(normalized.optString("species")))
                errors.put("Espécie não reconhecida pelo núcleo nem pelos suplementos compilados: " + normalized.optString("species") + ".");
            int level = clamp(normalized.optInt("level", 1), 1, 20);
            if (level != normalized.optInt("level", 1)) errors.put("O nível precisa estar entre 1 e 20.");
            normalized.put("level", level);
            String[] abilities = {"str","dex","con","int_score","wis","cha"};
            for (String ability : abilities) {
                int original = normalized.optInt(ability, 10);
                int safe = clamp(original, 1, 30);
                if (original != safe) errors.put(ability + " deve estar entre 1 e 30.");
                normalized.put(ability, safe);
            }
            int maxHp = Math.max(1, normalized.optInt("max_hp", normalized.optInt("hp", 1)));
            int hp = clamp(normalized.optInt("hp", maxHp), 0, maxHp);
            int ac = clamp(normalized.optInt("ac", 10), 1, 30);
            normalized.put("max_hp", maxHp);
            normalized.put("hp", hp);
            normalized.put("ac", ac);
            if (normalized.optString("saving_throws").trim().length() == 0)
                warnings.put("Salvaguardas ainda não foram confirmadas pelo seletor.");
            if (normalized.optString("skills").trim().length() == 0)
                warnings.put("Perícias ainda não foram confirmadas pelo seletor.");
            boolean revised=profile!=null&&(profile.contains("2024")||profile.startsWith("5.5"));String background=lower(normalized.optString("background"));
            if(revised){
                // For imported legacy sheets, migrate deterministically before validating.
                // For already-structured sheets, a mismatching rendered number is a
                // broken projection and must not be silently committed.
                boolean hadSkills=normalized.optJSONArray("skill_proficiencies")!=null;
                boolean hadSaves=normalized.optJSONArray("saving_throw_proficiencies")!=null;
                String oldSkills=normalized.optString("skills","");
                String oldSaves=normalized.optString("saving_throws","");
                JSONObject repaired=Character2024SheetReconciler.reconcile(new JSONObject(normalized.toString()));
                if(hadSkills&&!oldSkills.equals(repaired.optString("skills")))
                    errors.put("Perícias inconsistentes com as proficiências estruturadas.");
                if(hadSaves&&!oldSaves.equals(repaired.optString("saving_throws"))){
                    if(Character2024ClassContract.isGeneratedLevelOne(normalized) &&
                            Character2024ClassContract.hasCorrectLevelOneSaves(repaired))
                        warnings.put("Salvaguardas de ficha aleatória legada reparadas de acordo com a classe 2024.");
                    else
                        errors.put("Salvaguardas inconsistentes com as proficiências estruturadas.");
                }
                if(!hadSkills||!hadSaves)
                    warnings.put("Ficha legada reconciliada com proficiências estruturadas antes do salvamento.");
                normalized=repaired;
                // Never certify internally consistent but CLASS-INCOMPATIBLE saves.
                // This independent rules gate catches the Sorcerer WIS/CHA regression.
                if(level==1 && !Character2024ClassContract.hasCorrectLevelOneSaves(normalized))
                    errors.put("Proficiências em salvaguardas incompatíveis com a classe inicial 2024.");
                String coreClass=baseClassName(normalized.optString("class_name"));
                if(("Feiticeiro".equalsIgnoreCase(coreClass)||"Mago".equalsIgnoreCase(coreClass)) &&
                        level==1 && !lower(normalized.optString("proficiencies")).contains("armas simples"))
                    errors.put("Proficiência em armas incompleta para a classe 2024: requer armas simples.");
                if(!normalized.optString("species_mechanics_coherence_error","").isEmpty())
                    errors.put(normalized.optString("species_mechanics_coherence_error"));
                if(normalized.optBoolean("species_narrative_review_required",false))
                    warnings.put("Biografia menciona uma espécie anterior. Revise a narrativa antes de jogar.");
                int unresolved=normalized.optInt("creation_missing_choice_count",0);
                if(unresolved>0)
                    warnings.put("Criação INCOMPLETA: "+unresolved+" escolhas mecânicas pendentes: "+
                        normalized.optJSONArray("creation_missing_choices").toString());
                if(normalized.optBoolean("inventory_review_required",false))
                    warnings.put("Inventário textual preservado. Confirme a posse dos itens antes de converter para o inventário estruturado.");
                if(normalized.optBoolean("mage_armor_confirmation_pending",false))
                    warnings.put("O texto diz Armadura Arcana ativa, mas a conjuração não foi confirmada; CA de combate mantida.");
            }
            boolean compiledBackground=CharacterOptionCatalogEngine.lookupBackground(normalized.optString("background")).optBoolean("known",false)||EberronForgeCompleteEngine.supportsBackground(normalized.optString("background"))||ForgottenRealmsHeroesEngine.supportsBackground(normalized.optString("background"));
            if(revised&&!BACKGROUNDS_2024.contains(background)&&!compiledBackground)errors.put("O antecedente não pertence à lista revisada nem aos suplementos compilados.");
            if(!revised&&!BACKGROUNDS_2014.contains(background)&&!compiledBackground)errors.put("O antecedente não pertence à lista básica nem aos suplementos compilados.");
            String rawSubclass=subclassName(rawClass);String base=lower(baseClassName);boolean hasSubclass=rawSubclass.length()>0;
            int subclassLevel=subclassMinimumLevel(baseClassName,profile);
            if(hasSubclass&&level<subclassLevel)errors.put("A subclasse foi registrada antes do nível permitido neste perfil.");
            if(hasSubclass&&level>=subclassLevel&&!isCoreSubclassFor(baseClassName,rawSubclass)){
                JSONObject subMeta=CharacterOptionCatalogEngine.validateSubclass(baseClassName,rawSubclass);
                if(!subMeta.optBoolean("selection_allowed",false))errors.put("A subclasse não pertence à classe selecionada ou não está disponível: "+rawSubclass+".");
                else if(!subMeta.optBoolean("mechanics_compiled",false))errors.put("A subclasse existe, mas suas mecânicas ainda não estão compiladas para uso: "+rawSubclass+".");
            }
            if(!revised&&level>=subclassLevel&&!hasSubclass&&(base.contains("clérigo")||base.contains("clerigo")||base.contains("bruxo")||base.contains("feiticeiro")))errors.put("Esta classe de 2014 precisa registrar sua subclasse no nível 1.");
            JSONObject result = new JSONObject();
            result.put("valid", errors.length() == 0);
            result.put("profile", profile == null ? "" : profile);
            result.put("errors", errors);
            result.put("warnings", warnings);
            result.put("normalized", normalized);
            result.put("checked_at", System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            try {
                return new JSONObject().put("valid", false)
                        .put("errors", new JSONArray().put("Falha interna de validação: " + e.getMessage()))
                        .put("warnings", warnings).put("normalized", normalized);
            } catch (Exception ignored) { return new JSONObject(); }
        }
    }

    public String citationContract() {
        return "Para qualquer afirmação de regra ou material sorteado, inclua sources como uma lista JSON. "
                + "Cada fonte deve ter book, edition, language, chapter_or_table, printed_page, pdf_page, "
                + "original_term e uma explicação curta em português. Se nenhum manual foi consultado, use "
                + "sources:[] e marque rules_verified:false. Nunca invente páginas.";
    }

    public JSONObject auditAiAnswer(String mode, JSONObject envelope, JSONArray manuals) {
        JSONObject out = new JSONObject();
        try {
            JSONArray sources = envelope == null ? null : envelope.optJSONArray("sources");
            if (sources == null) sources = new JSONArray();
            boolean rulesMode = "Regras RAW/RAI".equals(mode) || "Sortear dos livros".equals(mode);
            JSONArray checked = new JSONArray();
            int confirmed = 0;
            for (int i = 0; i < sources.length(); i++) {
                JSONObject source = sources.optJSONObject(i);
                if (source == null) continue;
                String book = source.optString("book").trim();
                boolean complete = book.length() > 0
                        && source.optString("chapter_or_table").trim().length() > 0
                        && source.optString("printed_page").trim().length() > 0
                        && source.optString("pdf_page").trim().length() > 0;
                boolean supplied = isSuppliedManual(book, manuals);
                JSONObject item = new JSONObject(source.toString());
                item.put("complete", complete);
                item.put("matched_manual", supplied);
                item.put("confirmed", complete && supplied);
                if (complete && supplied) confirmed++;
                checked.put(item);
            }
            boolean modelClaim = envelope != null && envelope.optBoolean("rules_verified", false);
            boolean verified = !rulesMode || (modelClaim && checked.length() > 0 && confirmed == checked.length());
            out.put("mode", mode);
            out.put("sources", checked);
            out.put("manuals_sent", manuals == null ? new JSONArray() : manuals);
            out.put("verified", verified);
            out.put("source_count", checked.length());
            out.put("confirmed_source_count", confirmed);
            String warning = "";
            if (rulesMode && checked.length() == 0)
                warning = "A resposta não apresentou referência verificável; trate-a como interpretação.";
            else if (rulesMode && confirmed != checked.length())
                warning = "Uma ou mais referências estão incompletas ou não correspondem aos manuais realmente enviados à IA.";
            else if (rulesMode && !modelClaim)
                warning = "A própria resposta não confirmou que as regras foram verificadas no material consultado.";
            out.put("warning", warning);
            out.put("created_at", System.currentTimeMillis());
        } catch(Exception ignored){AppLog.ignored("CharacterRulesValidator","Falha tolerada; a execução continuou.",ignored);}
        return out;
    }

    private static boolean isSuppliedManual(String book, JSONArray manuals) {
        if (book == null || manuals == null) return false;
        String candidate = normalizeTitle(book);
        if (candidate.length() < 3) return false;
        for (int i = 0; i < manuals.length(); i++) {
            JSONObject manual = manuals.optJSONObject(i);
            if (manual == null) continue;
            String title = normalizeTitle(manual.optString("title"));
            if (title.length() < 3) continue;
            if (title.equals(candidate) || title.contains(candidate) || candidate.contains(title)) return true;
        }
        return false;
    }

    private static String normalizeTitle(String value) {
        String text = lower(value);
        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return text;
    }

    private static void required(JSONObject o, String key, String label, JSONArray errors) {
        if (o.optString(key).trim().length() == 0) errors.put(label + " é obrigatório.");
    }

    private static String baseClassName(String value) {
        String raw=value==null?"":value.trim();int dash=raw.indexOf(" — ");if(dash<0)dash=raw.indexOf(" - ");return dash>0?raw.substring(0,dash).trim():raw;
    }
    private static String subclassName(String value) {
        String raw=value==null?"":value.trim();int dash=raw.indexOf(" — ");int size=3;if(dash<0){dash=raw.indexOf(" - ");size=3;}return dash>0?raw.substring(dash+size).trim():"";
    }
    private static int subclassMinimumLevel(String cls,String profile) {
        String c=fold(cls);boolean revised=profile!=null&&(profile.contains("2024")||profile.startsWith("5.5"));
        if(revised)return 3;
        if(c.contains("clerigo")||c.contains("bruxo")||c.contains("feiticeiro"))return 1;
        if(c.contains("druida")||c.contains("mago"))return 2;
        return 3;
    }
    private static boolean isCoreSubclassFor(String cls,String subclass) {
        String c=fold(cls),s=fold(subclass);
        if(c.contains("barbar"))return oneOf(s,"berserker","coracao selvagem","arvore do mundo","fanatico","guerreiro totemico");
        if(c.contains("bardo"))return oneOf(s,"colegio da danca","colegio do glamour","colegio do conhecimento","colegio do valor");
        if(c.contains("bruxo"))return oneOf(s,"patrono arquifada","patrono celestial","patrono corruptor","patrono grande antigo","arquifada","corruptor","grande antigo");
        if(c.contains("clerigo"))return oneOf(s,"dominio da guerra","dominio da luz","dominio da trapaca","dominio da vida");
        if(c.contains("druida"))return oneOf(s,"circulo da lua","circulo da terra","circulo das estrelas","circulo do mar");
        if(c.contains("feiticeiro"))return oneOf(s,"feiticaria aberrante","feiticaria draconica","feiticaria do caos mecanico","magia selvagem","linhagem draconica");
        if(c.contains("ladino"))return oneOf(s,"trapaceiro arcano","assassino","alma faca","ladrao");
        if(c.contains("guerreiro"))return oneOf(s,"mestre de batalha","campeao","cavaleiro arcano","guerreiro psionico");
        if(c.contains("mago"))return oneOf(s,"abjurador","adivinho","evocador","ilusionista","abjuracao","adivinhacao","conjuracao","encantamento","evocacao","ilusao","necromancia","transmutacao");
        if(c.contains("monge"))return oneOf(s,"guerreiro da misericordia","guerreiro da mao aberta","guerreiro das sombras","guerreiro dos elementos","caminho da mao aberta","caminho da sombra","caminho dos quatro elementos");
        if(c.contains("paladino"))return oneOf(s,"juramento da devocao","juramento da gloria","juramento da vinganca","juramento dos ancioes");
        if(c.contains("patrulheiro"))return oneOf(s,"andarilho feerico","cacador","mestre das feras","perseguidor sombrio");
        return false;
    }
    private static boolean oneOf(String value,String... allowed){for(String x:allowed)if(value.equals(x))return true;return false;}
    private static String fold(String value){return java.text.Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static String lower(String value) { return value == null ? "" : value.trim().toLowerCase(new Locale("pt","BR")); }
    private static boolean containsKnown(Set<String> values, String candidate) {
        for (String value : values) if (candidate.equals(value) || candidate.startsWith(value + " ")) return true;
        return false;
    }
}
