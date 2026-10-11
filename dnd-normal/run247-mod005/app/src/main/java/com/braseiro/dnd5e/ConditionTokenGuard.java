package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/**
 * RUN204: exact condition tokens; a sentence such as "não está envenenado" must
 * NEVER trigger poison disadvantage. No AI inference or substring matching.
 */
public final class ConditionTokenGuard {
    private ConditionTokenGuard() {}
    private static String norm(String s){
        if(s==null)return "";
        String n=Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","");
        return n.replaceAll("[^a-z0-9]+"," ").trim().replaceAll(" +"," ");
    }
    private static String clean(String s){
        String n=norm(s);
        // Strip only explicit metadata, never arbitrary prose containing a keyword.
        n=n.replaceFirst("^(?:condicao|condicoes|estado|status|condition|conditions) +","");
        n=n.replaceFirst(" +(?:por|durante) +[0-9]+ +(?:rodadas?|turnos?|horas?|minutos?)$","");
        n=n.replaceFirst(" +[0-9]+ +(?:rodadas?|turnos?|horas?|minutos?)$","");
        return n;
    }
    public static String canonical(String value){
        String n=clean(value);
        if(n.equals("blind")||n.equals("blinded")||n.equals("cego")||n.equals("cega"))return "cego";
        if(n.equals("charmed")||n.equals("charm")||n.equals("enfeiticado")||n.equals("enfeiticada"))return "enfeiticado";
        if(n.equals("frightened")||n.equals("amedrontado")||n.equals("amedrontada"))return "amedrontado";
        if(n.equals("grappled")||n.equals("agarrado")||n.equals("agarrada"))return "agarrado";
        if(n.equals("incapacitated")||n.equals("incapacitado")||n.equals("incapacitada"))return "incapacitado";
        if(n.equals("invisible")||n.equals("invisivel"))return "invisivel";
        if(n.equals("paralyzed")||n.equals("paralisado")||n.equals("paralisada"))return "paralisado";
        if(n.equals("petrified")||n.equals("petrificado")||n.equals("petrificada"))return "petrificado";
        if(n.equals("poisoned")||n.equals("envenenado")||n.equals("envenenada"))return "envenenado";
        if(n.equals("prone")||n.equals("caido")||n.equals("caida"))return "caido";
        if(n.equals("restrained")||n.equals("impedido")||n.equals("impedida"))return "impedido";
        if(n.equals("stunned")||n.equals("aturdido")||n.equals("aturdida"))return "aturdido";
        if(n.equals("unconscious")||n.equals("inconsciente"))return "inconsciente";
        if(n.equals("deafened")||n.equals("surdo")||n.equals("surda"))return "surdo";
        if(n.equals("exhausted")||n.equals("exausto")||n.equals("exausta")||n.equals("exaustao"))return "exausto";
        return n;
    }
    private static boolean negated(String token){
        String n=norm(token);
        return n.startsWith("nao ")||n.startsWith("sem ")||n.startsWith("livre de ")||
            n.startsWith("imune a ")||n.startsWith("resistente a ")||n.startsWith("not ")||
            n.startsWith("no ")||n.startsWith("immune to ")||n.startsWith("resistant to ")||
            n.startsWith("curado de ")||n.startsWith("removido ")||n.startsWith("removida ");
    }
    public static boolean has(String all,String wanted){
        String goal=canonical(wanted);
        if(goal.isEmpty()||all==null||all.trim().isEmpty())return false;
        for(String segment:all.split("[,;•/\\n]+")){
            // Explicit conjunctions can join two condition names, not arbitrary prose.
            for(String part:segment.split("(?i)\\s+(?:e|and)\\s+")){
                if(!negated(part)&&goal.equals(canonical(part)))return true;
            }
        }
        return false;
    }
}
