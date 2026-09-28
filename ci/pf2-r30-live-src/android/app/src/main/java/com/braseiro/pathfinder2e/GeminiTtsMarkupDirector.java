package com.braseiro.pathfinder2e;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Marca somente a transcrição enviada ao Gemini TTS. O texto visível da cena
 * nunca recebe tags. R28 privilegia as tags documentadas e usa a direção
 * semântica no prompt para emoção/ritmo; isso preserva expressividade sem
 * transformar toda frase em markup experimental.
 */
public final class GeminiTtsMarkupDirector {
    private static final Set<String> ALLOWED=new HashSet<String>();
    static{
        String[] tags={"whispers","shouting","laughs","sighs","gasp","cough"};
        for(String tag:tags)ALLOWED.add(tag);
    }
    private GeminiTtsMarkupDirector(){}

    public static boolean supportsInlineTags(String model){
        String n=model==null?"":model.toLowerCase(Locale.ROOT).replace("models/","");
        return n.contains("tts")&&(n.contains("gemini-2.5")||n.contains("gemini-3.1"));
    }

    public static String annotateForModel(String model,String raw){
        return annotateForModel(model,raw,"normal",1,"normal","Narrador");
    }

    public static String annotateForModel(String model,String raw,String delivery,int intensity,String pauseProfile,String speaker){
        String clean=PortugueseNarrationSanitizer.sanitize(raw==null?"":raw).replaceAll("\\s+"," ").trim();
        if(clean.length()==0||!supportsInlineTags(model))return clean;
        return annotate(clean,delivery,intensity,pauseProfile,speaker);
    }

    public static String annotate(String clean){return annotate(clean,"normal",1,"normal","Narrador");}

    /**
     * Tags inline ficam reservadas a sinais explícitos no próprio texto.
     * Tensão, suspense, ameaça, revelação e urgência são dirigidos em
     * NarrationPlaybackPolicy como arco de performance em linguagem natural.
     */
    public static String annotate(String clean,String delivery,int intensity,String pauseProfile,String speaker){
        if(clean==null||clean.trim().isEmpty())return "";
        String[] sentences=clean.trim().split("(?<=[.!?…])\\s+");
        ArrayList<String> out=new ArrayList<String>();
        int tags=0;
        for(int i=0;i<sentences.length;i++){
            String sentence=sentences[i].trim();if(sentence.isEmpty())continue;
            String style=tags<4?localStyleTag(sentence):"";
            StringBuilder part=new StringBuilder();
            if(style.length()>0){part.append('[').append(style).append("] ");tags++;}
            part.append(sentence);
            out.add(part.toString());
        }
        String joined=join(out," ");
        return validate(joined)?joined:clean.trim();
    }

    private static String localStyleTag(String sentence){
        String n=fold(sentence);
        if(has(n,"sussurra","sussurro","quase inaudivel","em voz baixa","baixinho"))return "whispers";
        if(has(n,"grita","gritou","berra","berrou","urrou","ordem aos gritos","aos gritos"))return "shouting";
        if(has(n,"suspira","solta um suspiro"))return "sighs";
        if(has(n,"ofega","puxa o ar de repente","prende a respiracao"))return "gasp";
        if(has(n,"tosse","tossiu","pigarreia"))return "cough";
        if(has(n,"ri ","ri.","rindo","gargalha","gargalhou"))return "laughs";
        return "";
    }

    public static boolean validate(String marked){
        if(marked==null||marked.trim().isEmpty())return false;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\[([^\\]]+)\\]").matcher(marked);
        int last=-1;
        while(m.find()){
            String tag=m.group(1).trim().toLowerCase(Locale.ROOT);
            if(!ALLOWED.contains(tag))return false;
            if(last>=0){String between=marked.substring(last,m.start());if(between.trim().isEmpty())return false;}
            last=m.end();
        }
        return true;
    }

    public static String stripTags(String value){
        return value==null?"":value.replaceAll("\\[(?:[^\\]]+)\\]\\s*","").replaceAll("\\s+"," ").trim();
    }

    public static int tagCount(String value){
        if(value==null||value.length()==0)return 0;int count=0;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\[([^\\]]+)\\]").matcher(value);
        while(m.find())if(ALLOWED.contains(m.group(1).trim().toLowerCase(Locale.ROOT)))count++;
        return count;
    }

    private static boolean has(String n,String... terms){for(String term:terms)if(n.contains(fold(term)))return true;return false;}
    private static String fold(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);}
    private static String join(ArrayList<String> values,String sep){StringBuilder b=new StringBuilder();for(String v:values){if(b.length()>0)b.append(sep);b.append(v);}return b.toString();}
}
