package com.braseiro.pathfinder2e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Direção narrativa canônica derivada do PERFIL TÉCNICO — MESTRE HUMANO IMERSIVO.
 *
 * Esta classe NÃO resolve regras e NÃO altera estado. Ela transforma o estado já
 * resolvido pelos motores em instruções de câmera, intensidade, foco, ritmo e
 * ponto de interrupção para a voz do Mestre.
 */
public final class HumanMasterNarrativeProfile {
    public static final String ASSET = "narrative/PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md";
    private final Context context;
    private volatile String canonicalSource;

    public HumanMasterNarrativeProfile(Context context) {
        this.context = context == null ? null : context.getApplicationContext();
    }

    public JSONObject direction(String playerAction, JSONObject gameBusRoot, JSONObject barbaraPlan) {
        JSONObject out = new JSONObject();
        try {
            String action = clean(playerAction);
            String occasion = barbaraPlan == null ? "" : clean(barbaraPlan.optString("story_occasion"));
            JSONObject scene = gameBusRoot == null ? null : gameBusRoot.optJSONObject("scene");
            JSONObject world = gameBusRoot == null ? null : gameBusRoot.optJSONObject("world");
            JSONObject environmental = gameBusRoot == null ? null : gameBusRoot.optJSONObject("environmental_director");
            String sceneType = classify(action, occasion, scene, world, environmental);
            int intensity = intensity(sceneType, action, occasion);

            out.put("profile_id", "mestre_humano_imersivo_v1");
            out.put("role", "DIRECAO_NARRATIVA");
            out.put("source_asset", ASSET);
            out.put("source_loaded", canonicalText().length() > 0);
            out.put("source_sha256", sha256(canonicalText()));
            out.put("scene_type", sceneType);
            out.put("narrative_intensity", intensity);
            out.put("camera", cameraFor(sceneType));
            out.put("density", densityFor(intensity));
            out.put("focus", focusFor(sceneType));
            out.put("player_last_action", tail(action, 900));
            out.put("pipeline", "PLAYER INPUT -> INTENT PARSER -> WORLD STATE -> RULES ENGINE -> EVENT RESULT -> NARRATIVE DIRECTOR -> GM VOICE -> PLAYER");
            out.put("truth_priority", new JSONArray()
                    .put("estado mecânico real")
                    .put("regras implementadas")
                    .put("dados persistentes")
                    .put("cenário canônico")
                    .put("RAG")
                    .put("improvisação narrativa"));
            out.put("directives", directives(sceneType, intensity));
            out.put("short_memory_required", new JSONArray()
                    .put("últimas ações")
                    .put("NPCs ativos")
                    .put("objetos manipulados")
                    .put("tópico atual")
                    .put("ameaça")
                    .put("pergunta do jogador")
                    .put("posição"));
            out.put("mechanical_bridge", "ficção -> necessidade real de teste -> motor mecânico -> resultado -> ficção");
            out.put("stop_rule", "Narrar somente até o próximo ponto de decisão real e parar.");
            out.put("agency_rule", "Nunca decidir ação, intenção, fala ou emoção do personagem controlado pelo JOGADOR. Heróis marcados MESTRE são exceção explícita de controle, mas suas consequências mecânicas continuam obrigatoriamente no motor PF2e.");
            out.put("quality_gate", new JSONArray()
                    .put("presença")
                    .put("continuidade")
                    .put("concretude")
                    .put("agência")
                    .put("estado")
                    .put("mecânica")
                    .put("ritmo")
                    .put("mistério")
                    .put("sensorial")
                    .put("reatividade"));
        } catch (Exception e) {
            try { out.put("error", String.valueOf(e.getMessage())); } catch (Exception ignored) {}
        }
        return out;
    }

    private JSONArray directives(String sceneType, int intensity) {
        JSONArray a = new JSONArray();
        a.put("Conduza uma cena interativa em tempo real; não escreva romance, relatório, HUD ou log.");
        a.put("A ficção vem antes da mecânica e volta imediatamente depois do resultado mecânico.");
        a.put("Responda especificamente ao último ato do jogador e mostre a reação do mundo.");
        a.put("Use detalhes sensoriais seletivos; não transforme a descrição em inventário.");
        a.put("Não force sentimentos do personagem; mostre estímulos externos.");
        a.put("Revele informação progressivamente e preserve segredos não descobertos.");
        a.put("Não repita automaticamente 'o que você faz?'; pare naturalmente quando houver decisão real.");
        a.put("Não verbalize números e estado já visíveis na UI salvo quando se manifestarem na ficção.");
        a.put("NPCs existem e agem; não são telas de diálogo nem bancos de dados.");
        a.put("Não invente regra, sucesso ou falha. O motor mecânico é a autoridade.");
        if ("OPENING".equals(sceneType)) {
            a.put("Abertura: estabeleça contexto suficiente, aproxime a câmera e termine em um estímulo jogável.");
        } else if ("ARRIVAL".equals(sceneType)) {
            a.put("Chegada: aproximação -> limiar -> primeiro impacto interno -> identidade do lugar -> gancho.");
        } else if ("COMBAT".equals(sceneType) || "CHASE".equals(sceneType)) {
            a.put("Ação: frases menores, concretas, espaciais e rápidas; cada ação produz uma imagem.");
        } else if ("INVESTIGATION".equals(sceneType) || "HORROR".equals(sceneType)) {
            a.put("Mistério: ofereça evidências, inconsistências, ausências e reações; não entregue a conclusão.");
        } else if ("TRAVEL".equals(sceneType)) {
            a.put("Viagem: comprima trechos sem decisão e expanda encontro, risco, clima, bifurcação, desgaste ou descoberta.");
        } else if ("REST".equals(sceneType) || "DOWNTIME".equals(sceneType)) {
            a.put("Descanso: reduza o ritmo e abra espaço para conversa, reparos, reflexão, relações e planejamento.");
        }
        if (intensity >= 3) a.put("Cena relevante: aumente presença sensorial e especificidade sem tomar a agência do jogador.");
        return a;
    }

    private String classify(String action, String occasion, JSONObject scene, JSONObject world, JSONObject environmental) {
        String all = plain(action + " " + occasion + " " + (scene == null ? "" : scene.toString()) + " " +
                (world == null ? "" : world.toString()) + " " + (environmental == null ? "" : environmental.toString()));
        if (contains(all, "opening", "abertura", "adventure_start", "inicio da aventura", "inicio de campanha")) return "OPENING";
        if (contains(all, "climax", "clímax", "boss", "decisiva", "catastrofe", "catástrofe")) return "CLIMAX";
        if (contains(all, "combat", "combate", "attack", "ataque", "initiative", "iniciativa", "battle")) return "COMBAT";
        if (contains(all, "chase", "persegu", "fuga")) return "CHASE";
        if (contains(all, "arrival", "cheg", "novo local", "cidade nova", "entrou", "entrada")) return "ARRIVAL";
        if (contains(all, "travel", "viagem", "estrada", "rota")) return "TRAVEL";
        if (contains(all, "investig", "pista", "mister", "mistér", "interrogar")) return "INVESTIGATION";
        if (contains(all, "horror", "terror", "assombr", "sinistro")) return "HORROR";
        if (contains(all, "revel", "descoberta importante")) return "REVELATION";
        if (contains(all, "discover", "descob", "encontrou", "achou")) return "DISCOVERY";
        if (contains(all, "danger", "perigo", "ameaça", "ameaca")) return "DANGER";
        if (contains(all, "social", "conversa", "dialog", "fala", "pergunta", "npc")) return "SOCIAL";
        if (contains(all, "rest", "descanso", "acamp", "dormir")) return "REST";
        if (contains(all, "downtime", "tempo livre", "administra")) return "DOWNTIME";
        if (contains(all, "explor", "hex", "ruina", "ruína", "caverna", "floresta")) return "EXPLORATION";
        if (contains(all, "transition", "transicao", "transição", "depois", "horas depois", "dias depois")) return "TRANSITION";
        return "EXPLORATION";
    }

    private int intensity(String type, String action, String occasion) {
        if ("OPENING".equals(type) || "CLIMAX".equals(type) || "REVELATION".equals(type)) return 4;
        if ("ARRIVAL".equals(type) || "DISCOVERY".equals(type) || "DANGER".equals(type) || "HORROR".equals(type)) return 3;
        if ("EXPLORATION".equals(type) || "SOCIAL".equals(type) || "INVESTIGATION".equals(type) || "REST".equals(type) || "TRAVEL".equals(type)) return 2;
        String p = plain(action + " " + occasion);
        if (p.length() < 45 && contains(p, "abro", "pego", "olho", "sim", "nao", "não")) return 0;
        return 1;
    }

    private String cameraFor(String type) {
        if ("OPENING".equals(type)) return "ampla -> região/local -> detalhe -> estímulo -> decisão";
        if ("ARRIVAL".equals(type)) return "aproximação -> limiar -> interior -> detalhe distintivo -> gancho";
        if ("COMBAT".equals(type) || "CHASE".equals(type)) return "próxima e espacial; foco em movimento, distância, obstáculo e impacto";
        if ("INVESTIGATION".equals(type) || "HORROR".equals(type)) return "normal -> detalhe anômalo -> reação -> aproximação progressiva";
        if ("TRAVEL".equals(type)) return "ampla nas transições; aproxima apenas quando algo se torna interativo";
        return "intermediária -> detalhe relevante -> próximo ponto de decisão";
    }

    private String focusFor(String type) {
        if ("SOCIAL".equals(type)) return "pessoas, subtexto, intenção, linguagem corporal e atividade do ambiente";
        if ("INVESTIGATION".equals(type)) return "observação, pistas, inconsistências e inferência";
        if ("EXPLORATION".equals(type)) return "espaço, rota, descoberta, risco, recurso e escolha";
        if ("TRAVEL".equals(type)) return "distância, transformação do terreno e acontecimentos significativos";
        if ("REST".equals(type)) return "conversa, reparos, relações, reflexão e planejamento";
        if ("COMBAT".equals(type)) return "posição, ameaça, ação, impacto e consequência imediata";
        return "o elemento mais específico e interativo da cena atual";
    }

    private String densityFor(int intensity) {
        switch (Math.max(0, Math.min(4, intensity))) {
            case 0: return "1 frase";
            case 1: return "1 a 3 frases";
            case 2: return "3 a 6 frases";
            case 3: return "1 a 3 parágrafos curtos";
            default: return "cinematográfica, apenas até a próxima decisão real";
        }
    }

    private String canonicalText() {
        String cached = canonicalSource;
        if (cached != null) return cached;
        if (context == null) return canonicalSource = "";
        StringBuilder b = new StringBuilder();
        try (InputStream in = context.getAssets().open(ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) b.append(line).append('\n');
        } catch (Exception e) {
            AppLog.w("HumanMasterNarrativeProfile", "Perfil narrativo canônico não pôde ser lido.", e);
        }
        return canonicalSource = b.toString();
    }

    private static String sha256(String value) {
        if (value == null || value.length() == 0) return "";
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte x : bytes) b.append(String.format(Locale.US, "%02x", x & 0xff));
            return b.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String tail(String s, int max) { String v=clean(s); return v.length()<=max?v:v.substring(v.length()-max); }
    private static String plain(String s) {
        return clean(s).toLowerCase(new Locale("pt","BR"))
                .replace('á','a').replace('à','a').replace('â','a').replace('ã','a')
                .replace('é','e').replace('ê','e').replace('í','i')
                .replace('ó','o').replace('ô','o').replace('õ','o')
                .replace('ú','u').replace('ç','c');
    }
    private static boolean contains(String s, String... terms) {
        for (String term : terms) if (s.contains(plain(term))) return true;
        return false;
    }
}
