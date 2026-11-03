package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tempo diegético estimado para uma ação aceita. Não usa duração do áudio, rede
 * ou relógio da parede. Uma atualização temporal explícita do Mestre prevalece.
 * Consultas ao Mestre, introduções e ações de deslocamento de duração não
 * conhecida não produzem minutos inventados.
 */
public final class SceneActionClock {
    private SceneActionClock(){}
    public static String cleanPlayerAction(String action) {
        if(action==null)return "";
        String clean=action;
        // Legacy Strixhaven logs accidentally stored internal system contracts and
        // source-book extracts as the player's declared action.
        int chosen=clean.lastIndexOf("O jogador escolheu: ");
        if(chosen>=0){
            String chosenText=clean.substring(chosen+"O jogador escolheu: ".length());
            int stop=chosenText.indexOf(". Continue a partir dessa escolha");
            if(stop>=0)chosenText=chosenText.substring(0,stop+1);
            int runtime=chosenText.indexOf("=== STRIXHAVEN OFICIAL");
            if(runtime>=0)chosenText=chosenText.substring(0,runtime);
            clean=chosenText.trim();
        }else if(clean.startsWith("CONTRATO NARRATIVO STRIXHAVEN")
                ||clean.startsWith("INÍCIO AUTOMÁTICO")
                ||clean.startsWith("INICIO AUTOMATICO")){
            return ""; // automated opening is not an action by the hero
        }
        if(clean.startsWith("DESLOCAMENTO CONFIRMADO PELO MAPA:")
                ||clean.startsWith("EXPLORAÇÃO LIVRE CONFIRMADA PELO MAPA:")){
            int end=clean.indexOf(". Narre ");
            if(end<0)end=clean.indexOf(". Não ");
            if(end>=0)clean=clean.substring(0,end+1);
            return clean.trim();
        }
        int busIdx=clean.indexOf("\n\nUse game_bus");
        if(busIdx>=0)clean=clean.substring(0,busIdx);
        int dirIdx=clean.indexOf("DIREÇÃO DE DIÁLOGO, PRIORIDADE ALTA:");
        if(dirIdx>=0)clean=clean.substring(0,dirIdx);
        int barbIdx=clean.indexOf("MOTOR BARBARA ");
        if(barbIdx>=0)clean=clean.substring(0,barbIdx);
        if(clean.startsWith("PERFIL OFICIAL STRIXHAVEN")){
            int lastBreak=clean.lastIndexOf("\n\n");
            if(lastBreak>=0&&lastBreak+2<clean.length())clean=clean.substring(lastBreak+2);
        }
        return clean.trim();
    }

    public static double minutesFor(String action) {
        String clean=cleanPlayerAction(action);
        if(clean.isEmpty())return 0d;
        String lower=normalizeActionMarker(clean);
        if(lower.startsWith("deslocamento confirmado pelo mapa")
                ||lower.startsWith("exploracao livre confirmada pelo mapa"))return 0d;
        String text=Normalizer.normalize(clean.toLowerCase(new Locale("pt","BR")),
                Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("\\s+"," ").trim();
        if(text.contains("inicio automatico")||text.contains("primeira cena do semestre")||
                text.contains("inicie a aventura")||text.contains("abertura da campanha")||
                text.contains("o que o mestre acha")||text.contains("consulta auxiliar"))return 0d;
        if(matches(text,"\\b(aguardo|espero|passo|deixo passar)\\b")){
            Matcher minutes=Pattern.compile("\\b(\\d{1,3})\\s*(?:minutos|minuto|min)\\b").matcher(text);
            if(minutes.find())return Math.min(720d,Integer.parseInt(minutes.group(1)));
            Matcher hours=Pattern.compile("\\b(\\d{1,2})\\s*(?:horas|hora)\\b").matcher(text);
            if(hours.find())return Math.min(720d,Integer.parseInt(hours.group(1))*60d);
            if(matches(text,"\\b(uma hora|1 hora)\\b"))return 60d;
        }
        if(matches(text,"\\b(durmo|dormir|vou dormir|descanso longo)\\b"))return 480d;
        if(matches(text,"\\b(descanso curto|descansamos por uma hora)\\b"))return 60d;
        if(matches(text,"\\b(viajo|viajamos|viajar|atravesso a regiao|caminho ate outra cidade)\\b"))return 0d;
        if(matches(text,"\\b(ataco|atacamos|conjuro|combate|iniciativa|disparo|golpeio|esquivo)\\b"))return 0.1d;
        if(matches(text,"\\b(estudo|estudamos|pesquiso em livros|leio um livro|catalogo|arquivo)\\b"))return 10d;
        if(matches(text,"\\b(converso|pergunto|dialogo|entrevisto|negocio|falo com|abordo|questiono|respondo)\\b"))return 5d;
        if(matches(text,"\\b(procuro|procuramos|busco|examino|investigo|leio|observo|vasculho|panfleto|folheto|inspeciono)\\b"))return 3d;
        if(matches(text,"\\b(caminho|entro|subo|desco|atravesso|aproximo|exploro|percorro|vou ate|sigo para)\\b"))return 4d;
        return 2d;
    }
    private static String normalizeActionMarker(String value){
        return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim();
    }
    private static boolean matches(String text,String regex){return Pattern.compile(regex).matcher(text).find();}
}
