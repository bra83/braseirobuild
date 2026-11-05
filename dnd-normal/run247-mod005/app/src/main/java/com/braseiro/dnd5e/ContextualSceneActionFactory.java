package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Last-mile scene-action contract: four different player decisions grounded in
 * the CURRENT visible beat, even when the model returns zero valid suggestions.
 * This never performs actions, changes state, invents NPCs or grants results.
 */
public final class ContextualSceneActionFactory {
    public static final int MIN_CONTEXTUAL = 4;
    private ContextualSceneActionFactory() {}

    public static JSONArray complete(JSONArray source, String narration, JSONObject scene, int beatNumber) {
        JSONArray result = new JSONArray();
        if (narration == null || narration.trim().isEmpty()) return result;
        Set<String> actions = new HashSet<String>();
        Set<String> families = new HashSet<String>();
        if (source != null) for (int i = 0; i < source.length() && result.length() < MIN_CONTEXTUAL; i++) {
            JSONObject item = source.optJSONObject(i);
            if (item == null) continue;
            String action = item.optString("action", "").trim();
            if (action.isEmpty() || StoryMomentumAction.isRequest(action)) continue;
            String key = fold(action);
            if (actions.add(key)) {
                result.put(item);
                families.add(family(action));
            }
        }

        String beat = narration.substring(Math.max(0, narration.length() - 680));
        String actor = actor(beat, scene);
        String object = match(beat, OBJECTS);
        String place = match(beat, PLACES);
        if (place.isEmpty() && scene != null) {
            String location = scene.optString("location", "").trim();
            if (!location.isEmpty()) place = location;
        }
        ArrayList<JSONObject> generated = new ArrayList<JSONObject>();
        if (!actor.isEmpty()) {
            generated.add(choice("observar", "OBSERVAR " + upper(actor),
                "Observo " + actor + " por alguns instantes, atento aos gestos e ao que realmente fazem nesta cena."));
            generated.add(choice("conversar", "PUXAR CONVERSA COM " + upper(actor),
                "Tento iniciar uma conversa com " + actor + " sobre o que está acontecendo aqui, sem presumir a resposta."));
            generated.add(choice("despistar", "DISFARÇAR O INTERESSE POR " + upper(actor),
                "Disfarço meu interesse " + preposition("por",actor) + " e acompanho discretamente seus movimentos sem provocar alarme."));
            generated.add(choice("agir", "APROXIMAR-ME DE " + upper(actor),
                "Aproximo-me " + preposition("de",actor) + " com naturalidade, dando a chance de perceber minha presença antes de falar."));
        }
        if (!object.isEmpty()) {
            generated.add(choice("observar", "REPARAR EM " + upper(object),
                "Examino " + object + " à distância, buscando um detalhe visível que tenha passado despercebido."));
            generated.add(choice("agir", "EXAMINAR " + upper(object),
                "Aproximo-me para examinar " + object + " cuidadosamente, sem presumir o conteúdo nem modificar o objeto."));
            generated.add(choice("explorar", "CONFERIR " + upper(object),
                "Confiro " + object + " pelo lado acessível, procurando uma forma segura de entender sua utilidade."));
        }
        if (!place.isEmpty()) {
            generated.add(choice("explorar", "EXPLORAR " + upper(place),
                "Percorro uma pequena parte " + preposition("de",place) + " para reconhecer o espaço, sem sair deste local."));
            generated.add(choice("agir", "MUDAR DE POSIÇÃO EM " + upper(place),
                "Mudo discretamente de posição " + preposition("em",place) + " para ganhar outro ângulo sem abandonar a situação atual."));
            generated.add(choice("observar", "EXAMINAR " + upper(place),
                "Observo " + place + " com atenção, procurando distinguir acessos e movimentos que realmente estejam visíveis."));
        }
        // Prefer one distinct style per action. Never reuse an identical choice.
        addCandidates(result, generated, actions, families, true, narration, scene);
        addCandidates(result, generated, actions, families, false, narration, scene);

        // Sparse but playable prose may contain no named object or speaker. The
        // player can still decide HOW to react to the stated beat, without any
        // invented person, prop, threat, revelation, roll or automatic time skip.
        String anchor = sparseAnchor(beat);
        ArrayList<JSONObject> sparse = new ArrayList<JSONObject>();
        sparse.add(choice("observar", "OBSERVAR " + upper(anchor),
            "Permaneço atento " + preposition("a",anchor) + " e procuro perceber o que muda, sem antecipar um resultado."));
        sparse.add(choice("conversar", "QUEBRAR O SILÊNCIO",
            "Digo em voz alta o que penso sobre " + anchor + ", sem presumir que alguém esteja me ouvindo."));
        sparse.add(choice("agir", "MUDAR MINHA POSIÇÃO",
            "Mudo cautelosamente de posição diante " + preposition("de",anchor) + " para obter outra perspectiva, sem deixar a cena."));
        sparse.add(choice("despistar", "OCULTAR MINHA INTENÇÃO",
            "Evito demonstrar o que pretendo fazer diante " + preposition("de",anchor) + " e observo se isso altera a situação."));
        sparse.add(choice("explorar", "CONFERIR O QUE SEI",
            "Repasso mentalmente o que já percebi sobre " + anchor + ", sem inventar pistas nem tirar conclusões automáticas."));
        addCandidates(result, sparse, actions, families, true, narration, scene);
        addCandidates(result, sparse, actions, families, false, narration, scene);
        return result;
    }

    private static void addCandidates(JSONArray result, ArrayList<JSONObject> candidates,
                                      Set<String> actions, Set<String> families, boolean distinctFamily,
                                      String narration, JSONObject scene) {
        for (JSONObject item : candidates) {
            if (result.length() >= MIN_CONTEXTUAL) break;
            String action = item.optString("action", "");
            String key = fold(action), kind = item.optString("_family", "");
            if (actions.contains(key) || (distinctFamily && families.contains(kind))) continue;
            if (!SceneOptionDiversifier.isGroundedInScene(action, narration, scene)) continue;
            if (!SceneOptionDiversifier.isRelevantToCurrentBeat(action, narration)) continue;
            boolean duplicate = false;
            for (int i = 0; i < result.length(); i++) {
                JSONObject prior = result.optJSONObject(i);
                if (prior != null && SceneOptionDiversifier.similar(action, prior.optString("action", ""))) {
                    duplicate = true; break;
                }
            }
            if (duplicate) continue;
            result.put(item);
            actions.add(key);
            families.add(kind);
        }
    }

    private static JSONObject choice(String family, String label, String action) {
        JSONObject o = new JSONObject();
        try {
            o.put("label", label);
            o.put("description", "");
            o.put("action", action);
            o.put("_family", family);
        } catch (Exception ignored) { }
        return o;
    }

    private static String family(String action) {
        String n = fold(action);
        if (has(n,"dissimulo","disfarco","finjo","despisto","oculto","evito demonstrar")) return "despistar";
        if (has(n,"pergunto","converso","falo","dialogo","digo","quebro o silencio")) return "conversar";
        if (has(n,"observo","examino","reparo","leio","inspeciono")) return "observar";
        if (has(n,"percorro","sigo","exploro","confiro")) return "explorar";
        return "agir";
    }

    private static final String[][] ACTORS = {
        {"calouros","os calouros"},{"veteranos","os veteranos"},
        {"auxiliar","a auxiliar"},{"alunos distantes","os alunos distantes"},
        {"alunos","os alunos"},{"estudantes","os estudantes"},
        {"assistente","a assistente"},{"funcionária","a funcionária"},
        {"funcionário","o funcionário"},{"bibliotecário","o bibliotecário"},
        {"funcionário","o funcionário"},{"bibliotecária","a bibliotecária"},
        {"professora","a professora"},
        {"professor","o professor"},{"atendente","o atendente"},
        {"guarda","o guarda"},{"sentinela","a sentinela"},
        {"mercador","o mercador"},{"taverneiro","o taverneiro"},
        {"viajante","o viajante"},{"criaturas","as criaturas"},
        {"arbustos despertados","os arbustos despertados"}
    };
    private static final String[][] OBJECTS = {
        {"balcões de atendimento","os balcões de atendimento"},
        {"folhetos","os folhetos"},{"avisos","os avisos"},
        {"cartazes","os cartazes"},{"painel","o painel"},
        {"pergaminho","o pergaminho"},{"carta","a carta"},
        {"mapa","o mapa"},{"chave","a chave"},{"livros","os livros"},
        {"documento","o documento"},{"contrato","o contrato"},
        {"prancheta","a prancheta"},{"panfleto","o panfleto"},
        {"porta","a porta"},{"baú","o baú"},{"rastro","o rastro"}
    };
    private static final String[][] PLACES = {
        {"Hall dos Oráculos","o Hall dos Oráculos"},
        {"Biblioplex","o Biblioplex"},{"saguão","o saguão"},
        {"balcão","o balcão"},{"jardim","o jardim"},
        {"corredor","o corredor"},{"pátio","o pátio"},
        {"biblioteca","a biblioteca"},{"taverna","a taverna"},
        {"estrada","a estrada"},{"trilha","a trilha"},
        {"escadaria","a escadaria"},{"praça","a praça"},
        {"sala","a sala"},{"ponte","a ponte"}
    };

    private static String actor(String beat, JSONObject scene) {
        String fromBeat = match(beat, ACTORS);
        if (!fromBeat.isEmpty()) return fromBeat;
        if (scene == null) return "";
        String raw = scene.optString("present_npcs_json", "[]");
        try {
            JSONArray npcs = new JSONArray(raw);
            for (int i = 0; i < npcs.length(); i++) {
                JSONObject npc = npcs.optJSONObject(i);
                String name = npc == null ? npcs.optString(i, "") : npc.optString("name", "");
                if (!name.isEmpty() && fold(beat).contains(fold(name))) return name;
            }
        } catch (Exception ignored) { }
        return "";
    }

    private static String match(String text, String[][] choices) {
        String n = " " + fold(text) + " ";
        String winner="";int best=-1;
        for (String[] row : choices){
            int found=n.lastIndexOf(" " + fold(row[0]) + " ");
            if(found>best){best=found;winner=row[1];}
        }
        return winner;
    }

    private static String sparseAnchor(String beat) {
        String n = fold(beat);
        if (has(n,"pausa")) return "a pausa";
        if (has(n,"silencio")) return "o silêncio";
        if (has(n,"espera")) return "a espera";
        if (has(n,"aguarda")) return "quem aguarda";
        if (has(n,"pergunta")) return "a pergunta";
        if (has(n,"resposta")) return "a resposta";
        if (has(n,"ameaca")) return "a ameaça";
        if (has(n,"perigo")) return "o perigo";
        if (has(n,"orientacao")) return "a orientação";
        // Extract a real noun phrase from the latest narrated beat. This makes
        // sparse scenes playable without fabricating a prop or another character.
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile(
            "(?iu)\\b(?:a|o|os|as|um|uma|uns|umas)\\s+[\\p{L}]{4,}\\b")
            .matcher(beat);
        String best="";
        while(matcher.find()){
            String phrase=matcher.group().trim();
            String key=fold(phrase);
            if(has(key,"algo","coisa","algum","uma vez","outra vez"))continue;
            best=phrase;
        }
        if(!best.isEmpty())return best;
        java.util.regex.Matcher words=java.util.regex.Pattern.compile(
            "(?iu)\\b[\\p{L}]{5,}\\b").matcher(beat);
        String token="";
        while(words.find()){
            String t=words.group();
            if(!has(fold(t),"voces","voce","agora","ainda","entao","pergunta","responda","aquilo","porque","quando"))token=t;
        }
        if(!token.isEmpty())return "o detalhe “"+token+"”";
        return "o instante presente";
    }

    private static String preposition(String kind, String phrase) {
        if(phrase==null)return kind;
        String p=phrase.trim();
        String[] starts={"o ","a ","os ","as "};
        String[] from = "de".equals(kind)?new String[]{"do ","da ","dos ","das "}:
                        "em".equals(kind)?new String[]{"no ","na ","nos ","nas "}:
                        "por".equals(kind)?new String[]{"pelo ","pela ","pelos ","pelas "}:
                        "a".equals(kind)?new String[]{"ao ","à ","aos ","às "}:null;
        if(from!=null)for(int i=0;i<starts.length;i++)
            if(p.toLowerCase(Locale.ROOT).startsWith(starts[i]))return from[i]+p.substring(starts[i].length());
        return kind+" "+p;
    }

    private static boolean has(String input, String... fragments) {
        for (String v : fragments) if (input.contains(fold(v))) return true;
        return false;
    }
    private static String upper(String value) { return value.toUpperCase(new Locale("pt","BR")); }
    private static String fold(String v) {
        if (v == null) return "";
        return Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
