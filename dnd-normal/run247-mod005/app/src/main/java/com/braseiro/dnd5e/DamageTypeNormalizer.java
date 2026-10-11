package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/** D&D 5e damage types in PT-BR and EN. Pure Java, safe for desktop JVM tests. */
public final class DamageTypeNormalizer {
    private DamageTypeNormalizer(){}
    private static String plain(String value){
        if(value==null)return "";
        String s=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        return s.toLowerCase(Locale.ROOT).trim().replace('_',' ').replace('-',' ');
    }
    public static String canonical(String type){
        String t=plain(type);
        if(t.equals("acid")||t.equals("acido"))return "acid";
        if(t.equals("bludgeoning")||t.equals("contundente")||t.equals("concussao"))return "bludgeoning";
        if(t.equals("cold")||t.equals("frio")||t.equals("gelido"))return "cold";
        if(t.equals("fire")||t.equals("fogo")||t.equals("igneo"))return "fire";
        if(t.equals("force")||t.equals("forca")||t.equals("energetico"))return "force";
        if(t.equals("lightning")||t.equals("eletrico")||t.equals("eletricidade")||t.equals("relampago")||t.equals("raio"))return "lightning";
        if(t.equals("necrotic")||t.equals("necrotico"))return "necrotic";
        if(t.equals("piercing")||t.equals("perfurante"))return "piercing";
        if(t.equals("poison")||t.equals("veneno")||t.equals("venenoso"))return "poison";
        if(t.equals("psychic")||t.equals("psiquico"))return "psychic";
        if(t.equals("radiant")||t.equals("radiante"))return "radiant";
        if(t.equals("slashing")||t.equals("cortante"))return "slashing";
        if(t.equals("thunder")||t.equals("trovejante")||t.equals("trovao")||t.equals("sonico"))return "thunder";
        return "";
    }
    public static boolean known(String type){return !canonical(type).isEmpty();}
    public static boolean matches(String declaredTypes,String actualType){
        String target=canonical(actualType);
        boolean standard=!target.isEmpty();
        if(!standard)target=plain(actualType); // Preserve exact custom/homebrew damage types.
        if(target.isEmpty()||declaredTypes==null)return false;
        String source=plain(declaredTypes);
        if(source.isEmpty())return false;
        for(String chunk:source.split("[,;|/\\n]+")){
            String part=chunk.trim();
            if(part.equals("all")||part.equals("todos")||part.equals("tudo")||part.equals("all damage"))return true;
            if(target.equals(standard?canonical(part):plain(part)))return true;
            // Monster statblocks sometimes include qualifiers (e.g. 'fire (nonmagical)').
            for(String word:part.split("[() \\t]+"))if(target.equals(standard?canonical(word):plain(word)))return true;
        }
        return false;
    }
}
