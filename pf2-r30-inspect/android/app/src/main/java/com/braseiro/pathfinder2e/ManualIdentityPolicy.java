package com.braseiro.pathfinder2e;

import java.text.Normalizer;
import java.util.Locale;

/** Identidade estável de fontes e firewall fail-closed de autoridade Pathfinder 2e. */
public final class ManualIdentityPolicy {
    private ManualIdentityPolicy() {}

    public static boolean isExternalSystemTitle(String title){
        String n=normalize(title);
        return n.contains("dungeons dragons")||n.contains("d d 5e")||n.contains("dnd 5e")||n.contains("mystara")||
                n.contains("rules cyclopedia")||n.contains("becmi")||n.contains("pathfinder 1")||n.contains("pf1")||
                n.contains("traveller")||n.contains("mausritter")||n.contains("forbidden lands")||n.contains("gurps");
    }
    public static boolean isPf1Title(String title){String n=normalize(title);return n.contains("pathfinder 1")||n.contains("pf1")||n.contains("first edition pathfinder");}
    public static boolean isPf2AuthoritativeTitle(String title){
        if(isExternalSystemTitle(title))return false;String n=normalize(title);
        return n.contains("pathfinder 2")||n.contains("pf2")||n.contains("player core")||n.contains("gm core")||
                n.contains("monster core")||n.contains("remaster")||n.contains("rage of elements")||n.contains("treasure vault")||
                n.contains("war of immortals")||n.contains("kingmaker");
    }
    public static boolean isPf2LoreTitle(String title){
        if(isExternalSystemTitle(title))return false;String n=normalize(title);
        return isPf2AuthoritativeTitle(title)||n.contains("lost omens")||n.contains("golarion")||n.contains("inner sea");
    }
    public static boolean mechanicalAuthorityAllowed(String title,String ruleProfile){
        String joined=(title==null?"":title)+" "+(ruleProfile==null?"":ruleProfile);
        return !isExternalSystemTitle(joined)&&isPf2AuthoritativeTitle(joined);
    }
    public static String displayTitle(String title){return title==null?"":title.trim();}
    public static String duplicateKey(String title){return normalize(title)
            .replaceAll("\\b(pdf|comprimido|compressed|compactado|reduzido|copia|copy|lowres|low res|highres|high res)\\b"," ")
            .replaceAll("\\s+"," ").trim();}
    public static String normalize(String value){
        if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");
        return n.replaceAll("[^a-z0-9]+"," ").trim();
    }
}
