package com.braseiro.pathfinder2e;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Consulta local das tabelas de personagem do Mythic GME 2e.
 *
 * O PDF completo continua disponível como manual, mas as tabelas usadas pelos
 * botões de dado são armazenadas em JSON para que funcionem offline e sem OCR.
 */
public final class MythicOracle {
    private final Random random = new Random();
    private final JSONObject tables;
    private final Map<String,String> labels = new HashMap<String,String>();
    private final Map<String,String> originalLabels = new HashMap<String,String>();
    private final Map<String,Integer> printedPages = new HashMap<String,Integer>();
    private final Map<String,Integer> pdfPages = new HashMap<String,Integer>();
    private final Map<String,Integer> quickPrintedPages = new HashMap<String,Integer>();
    private final Map<String,Integer> quickPdfPages = new HashMap<String,Integer>();

    public MythicOracle(Context context) {
        JSONObject loaded = new JSONObject();
        try {
            InputStream in = context.getAssets().open("mythic/mythic_character_oracles.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) > 0) out.write(buffer, 0, count);
            in.close();
            JSONObject root = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            loaded = root.optJSONObject("tables");
            if (loaded == null) loaded = new JSONObject();
        } catch(Exception ignored){AppLog.ignored("MythicOracle","Falha tolerada; a execução continuou.",ignored);}
        tables = loaded;
        labels.put("characters", "Personagens");
        labels.put("character_actions_combat", "Ações em combate");
        labels.put("character_actions_general", "Ações gerais");
        labels.put("character_appearance", "Aparência");
        labels.put("character_background", "Passado");
        labels.put("character_conversations", "Conversas");
        labels.put("character_descriptors", "Descritores");
        labels.put("character_identity", "Identidade");
        labels.put("character_motivations", "Motivações");
        labels.put("character_personality", "Personalidade");
        labels.put("character_skills", "Habilidades");
        labels.put("character_traits_flaws", "Traços e defeitos");
        registerReference("characters", "Characters", 49, 50, 202, 203);
        registerReference("character_actions_combat", "Character Actions, Combat", 89, 90, 203, 204);
        registerReference("character_actions_general", "Character Actions, General", 89, 90, 203, 204);
        registerReference("character_appearance", "Character Appearance", 89, 90, 203, 204);
        registerReference("character_background", "Character Background", 90, 91, 204, 205);
        registerReference("character_conversations", "Character Conversations", 90, 91, 204, 205);
        registerReference("character_descriptors", "Character Descriptors", 90, 91, 204, 205);
        registerReference("character_identity", "Character Identity", 91, 92, 205, 206);
        registerReference("character_motivations", "Character Motivations", 91, 92, 205, 206);
        registerReference("character_personality", "Character Personality", 91, 92, 205, 206);
        registerReference("character_skills", "Character Skills", 92, 93, 206, 207);
        registerReference("character_traits_flaws", "Character Traits & Flaws", 92, 93, 206, 207);
    }

    private void registerReference(String key,String original,int printed,int pdf,int quickPrinted,int quickPdf) {
        originalLabels.put(key,original);printedPages.put(key,printed);pdfPages.put(key,pdf);
        quickPrintedPages.put(key,quickPrinted);quickPdfPages.put(key,quickPdf);
    }

    public boolean isReady() { return tables != null && tables.length() > 0; }

    public int rollD100() { return random.nextInt(100) + 1; }

    public String lookup(String tableKey, int roll) {
        JSONArray table = tables.optJSONArray(tableKey);
        if (table == null || table.length() == 0) return "Sem resultado";
        int index = Math.max(1, Math.min(100, roll)) - 1;
        if (index >= table.length()) index = table.length() - 1;
        return table.optString(index, "Sem resultado");
    }

    public String label(String tableKey) {
        String label = labels.get(tableKey);
        return label == null ? tableKey : label;
    }

    public String originalLabel(String tableKey) {
        String label = originalLabels.get(tableKey);
        return label == null ? tableKey : label;
    }

    public String sourceTitle() { return "Mythic Game Master Emulator, Second Edition"; }

    public String sourceReference(String tableKey) {
        Integer printed=printedPages.get(tableKey),pdf=pdfPages.get(tableKey),quickPrinted=quickPrintedPages.get(tableKey),quickPdf=quickPdfPages.get(tableKey);
        if(printed==null||pdf==null)return sourceTitle()+" — "+originalLabel(tableKey);
        return sourceTitle()+" — Meaning Tables: Elements — "+originalLabel(tableKey)+
                "; página impressa "+printed+" (página "+pdf+" do PDF)"+
                (quickPrinted==null||quickPdf==null?"":"; Quick Reference, página impressa "+quickPrinted+" (página "+quickPdf+" do PDF)");
    }

    public Result consult(String tableA, int rollA, String tableB, int rollB) {
        return new Result(tableA, clamp(rollA), lookup(tableA, rollA), tableB, clamp(rollB), lookup(tableB, rollB));
    }

    private int clamp(int value) { return Math.max(1, Math.min(100, value)); }

    public static final class Result {
        public final String tableA;
        public final int rollA;
        public final String wordA;
        public final String tableB;
        public final int rollB;
        public final String wordB;

        Result(String tableA, int rollA, String wordA, String tableB, int rollB, String wordB) {
            this.tableA = tableA;
            this.rollA = rollA;
            this.wordA = wordA;
            this.tableB = tableB;
            this.rollB = rollB;
            this.wordB = wordB;
        }

        public String compact() { return wordA + " / " + wordB; }
    }
}
