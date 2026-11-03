package com.braseiro.pathfinder2e;

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
    public JSONObject validate(JSONObject sheet, String profile) {
        JSONArray errors = new JSONArray();
        JSONArray warnings = new JSONArray();
        JSONObject normalized;
        try { normalized = new JSONObject(sheet == null ? "{}" : sheet.toString()); }
        catch (Exception e) { normalized = new JSONObject(); }
        try {
            required(normalized, "name", "Nome", errors);
            required(normalized, "species", "Ancestralidade", errors); // chave legada preservada no schema
            required(normalized, "class_name", "Classe", errors);
            required(normalized, "background", "Antecedente", errors);

            String cls = lower(normalized.optString("class_name"));
            String[] pfClasses={"alquimista","bárbaro","barbaro","bardo","bruxa","campeão","campeao","clérigo","clerigo","druida","guerreiro","inventor","investigador","cinético","cinetico","mago","mago de batalha","magus","monge","oráculo","oraculo","patrulheiro","pistoleiro","psíquico","psiquico","ladino","feiticeiro","invocador","espadachim","taumaturgo"};
            boolean classKnown=false; for(String c:pfClasses)if(cls.equals(c)||cls.startsWith(c+" ")||cls.startsWith(c+" — ")){classKnown=true;break;}
            if(cls.length()>0&&!classKnown) warnings.put("Classe fora do catálogo local; valide-a pelo RAG PF2e antes de conceder mecânica.");

            int level = normalized.optInt("level", 1);
            if(level<1||level>20) errors.put("O nível precisa estar entre 1 e 20.");
            level=clamp(level,1,20); normalized.put("level",level);

            JSONObject mods=normalized.optJSONObject("attribute_modifiers");
            if(mods==null)mods=new JSONObject();
            String[] abilities={"str","dex","con","int_score","wis","cha"};
            for(String ability:abilities){
                int raw=normalized.has(ability)?normalized.optInt(ability,0):mods.optInt(ability,0);
                int mod;
                if(raw>=-5&&raw<=10)mod=raw; else if(raw>=1&&raw<=30){mod=Math.floorDiv(raw-10,2);warnings.put("Atributo legado "+ability+" foi migrado de score para modificador PF2e.");} else {mod=0;errors.put(ability+" não é um modificador PF2e nem um score legado válido.");}
                mods.put(ability,mod); normalized.put(ability,mod);
            }
            normalized.put("attribute_modifiers",mods);
            normalized.put("attribute_model","pf2_remaster_modifiers");

            int maxHp=Math.max(1,normalized.optInt("max_hp",normalized.optInt("hp",1)));
            int hp=clamp(normalized.optInt("hp",maxHp),0,maxHp);
            int ac=Math.max(1,normalized.optInt("ac",10));
            normalized.put("max_hp",maxHp).put("hp",hp).put("ac",ac);

            if(normalized.optString("perception_rank").trim().isEmpty()&&normalized.optString("perception_bonus").trim().isEmpty())
                warnings.put("Percepção ainda não foi registrada; iniciativa padrão PF2e depende dela.");
            if(normalized.optString("saving_throws").trim().isEmpty()) warnings.put("Fortitude, Reflexos e Vontade ainda precisam ser confirmados.");
            if(normalized.optString("skills").trim().isEmpty()) warnings.put("Perícias ainda precisam ser confirmadas.");

            JSONObject result=new JSONObject();
            result.put("valid",errors.length()==0).put("profile",Pathfinder2eRulesEngine.PROFILE)
                    .put("errors",errors).put("warnings",warnings).put("normalized",normalized)
                    .put("mechanical_authority","pf2e_remaster").put("checked_at",System.currentTimeMillis());
            return result;
        }catch(Exception e){
            try{return new JSONObject().put("valid",false).put("errors",new JSONArray().put("Falha interna de validação: "+e.getMessage())).put("warnings",warnings).put("normalized",normalized);}catch(Exception ignored){return new JSONObject();}
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

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static String lower(String value) { return value == null ? "" : value.trim().toLowerCase(new Locale("pt","BR")); }
    private static boolean containsKnown(Set<String> values, String candidate) {
        for (String value : values) if (candidate.equals(value) || candidate.startsWith(value + " ")) return true;
        return false;
    }
}
