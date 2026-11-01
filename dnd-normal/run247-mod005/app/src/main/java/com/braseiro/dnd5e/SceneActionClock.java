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
    public static double minutesFor(String action) {
        if(action==null||action.trim().isEmpty())return 0d;
        String text=Normalizer.normalize(action.toLowerCase(new Locale("pt","BR")),
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
        if(matches(text,"\\b(durmo|dormir|vou dormir)\\b"))return 480d;
        if(matches(text,"\\b(descanso curto|descansamos por uma hora)\\b"))return 60d;
        if(matches(text,"\\b(viajo|viajamos|viajar|atravesso a regiao|caminho ate outra cidade)\\b"))return 0d;
        if(matches(text,"\\b(ataco|atacamos|conjuro|combate|iniciativa|disparo)\\b"))return 0.1d;
        if(matches(text,"\\b(estudo|estudamos|pesquiso em livros|leio um livro)\\b"))return 10d;
        if(matches(text,"\\b(converso|pergunto|dialogo|entrevisto|negocio)\\b"))return 5d;
        if(matches(text,"\\b(procuro|procuramos|busco|examino|investigo|leio|observo|vasculho|panfleto|folheto)\\b"))return 3d;
        return 2d;
    }
    private static boolean matches(String text,String regex){return Pattern.compile(regex).matcher(text).find();}
}
