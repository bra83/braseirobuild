package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decisions grounded in a named, speaking interlocutor and an actionable lead.
 * Do not turn scenery mentioned in a dialogue into a physically present prop.
 * No test, success, NPC movement or world mutation is inferred here.
 */
public final class SceneDialogueDecisionPlanner {
    private SceneDialogueDecisionPlanner() {}

    /** Actual interlocutor in the visible beat, not an old NPC from persistence. */
    public static String namedInterlocutor(String narration) {
        if (narration == null || narration.isEmpty()) return "";
        Matcher byLabel=Pattern.compile(
            "(?m)(?:^|\\n)\\s*([A-ZÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{L}'-]{2,})\\s*:\\s*[—-]"
        ).matcher(narration);
        while(byLabel.find())if(validName(byLabel.group(1)))return byLabel.group(1);
        Matcher byAttribution=Pattern.compile(
            "(?u)\\b(?:diz|pergunta|comenta|avisa|explica|responde|murmura|retruca|conta)\\s+([A-ZÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{L}'-]{2,})\\b"
        ).matcher(narration);
        String name="";
        while(byAttribution.find()) if (validName(byAttribution.group(1))) name=byAttribution.group(1);
        if (!name.isEmpty()) return name;
        Matcher byAction=Pattern.compile(
            "(?u)\\b([A-ZÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{L}'-]{2,})\\s+(?:solta|sorri|ri|comenta|pergunta|explica|responde|entrega|alcança|diz|fala|retruca)\\b"
        ).matcher(narration);
        while(byAction.find()) if (validName(byAction.group(1))) name=byAction.group(1);
        return name;
    }

    private static boolean validName(String name) {
        String n=fold(name);
        return !n.equals("kvothe") && !n.equals("estudante")
            && !n.equals("aluna") && !n.equals("aluno") && !n.equals("professor")
            && !n.equals("professora") && !n.equals("jogador") && !n.equals("mestre")
            && !n.equals("silencio") && !n.equals("grupo");
    }

    private static String topic(String narration) {
        String n=fold(narration);
        if(n.contains("folha desperta"))return "a expressão \"folha desperta\"";
        if(n.contains("jardim dos livros"))return "o Jardim dos Livros";
        if(n.contains("charada"))return "a charada";
        if(n.contains("enigma"))return "o enigma";
        if(n.contains("versos"))return "os versos";
        if(n.contains("pergaminho"))return "o pergaminho";
        if(n.contains("pista"))return "a pista";
        if(n.contains("orientacao"))return "a orientação";
        return "";
    }

    /** Return null rather than fabricating three choices from an abstract noun. */
    public static JSONObject choice(String slot, String narration) {
        String who=namedInterlocutor(narration), subject=topic(narration);
        if(who.isEmpty() || subject.isEmpty())return null;
        String n=fold(narration), label="", action="", type=slot;
        if(GroundedSceneActionSlots.OBSERVAR.equals(slot)) {
            label="OBSERVAR AS REAÇÕES DE "+upper(who);
            action="Presto atenção aos gestos e à expressão de "+who+" quando menciona "+subject+", sem pressupor o que está pensando.";
        } else if(GroundedSceneActionSlots.AGIR.equals(slot)) {
            label="PERGUNTAR A "+upper(who)+" SOBRE "+upper(subject);
            action="Pergunto a "+who+" o que sabe sobre "+subject+" e escuto sua resposta antes de decidir o próximo passo.";
        } else if(GroundedSceneActionSlots.DESPISTAR.equals(slot)) {
            // A named destination is not an object within reach: choosing to go
            // there is a movement attempt, not an immediate examination of it.
            if(n.contains("jardim dos livros")) {
                type="EXPLORAR";
                label="PROCURAR O CAMINHO ATÉ O JARDIM DOS LIVROS";
                action="Decido procurar o caminho até o Jardim dos Livros, sem presumir que já cheguei ou que resolvi a pista.";
            } else if(n.contains("enigma") || n.contains("charada") || n.contains("versos")) {
                type="INVESTIGAR";
                label="RELACIONAR AS INFORMAÇÕES DO ENIGMA";
                action="Comparo mentalmente o que "+who+" acabou de dizer com o enigma mencionado, sem assumir que encontrei a solução.";
            } else if(n.contains("veteranos") || n.contains("rivais") || n.contains("disputa")) {
                label="NÃO REVELAR MINHAS CONCLUSÕES";
                action="Evito revelar aos presentes a interpretação que estou formando sobre "+subject+", sem pressupor que alguém foi enganado.";
            } else return null;
        } else return null;
        JSONObject item=new JSONObject();
        try {
            item.put("type",type).put("action_type",type)
                .put("label",type+" • "+label).put("action",action).put("description",action);
        }catch(Exception ignored) {}
        return item;
    }
    private static String fold(String s) {
        return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static String upper(String s) {return s.toUpperCase(new Locale("pt","BR"));}
}
