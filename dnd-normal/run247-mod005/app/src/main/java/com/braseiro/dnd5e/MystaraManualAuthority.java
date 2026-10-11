package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Hierarquia local de fontes para o perfil Rules Cyclopedia de Mystara.
 * Mantém livros de adaptação 5e disponíveis para lore, mas impede que
 * substituam a mecânica clássica da Enciclopédia de Regras.
 */
public final class MystaraManualAuthority {
    private MystaraManualAuthority() {}

    public static boolean hasExactPrefix(String title) {
        return ManualIdentityPolicy.isMystaraTitle(title);
    }

    public static boolean isMechanicalQuery(String query) {
        String q=norm(query);
        return containsAny(q,
            "regra","teste","jogada","ataque","dano","classe armadura","ca descendente",
            "protecao","salvaguarda","iniciativa","surpresa","moral","experiencia","xp","nivel",
            "feitico","magia","clerigo","guerreiro","mago","ladrao","anao","elfo","halfling",
            "druida","mistico","habilidade de ladrao","especializacao em arma","dominio de arma",
            "dados de vida","dado de vida","monstro","movimento","carga","mos","encontro",
            "afastar morto vivo","morto vivo","reacao","combate","alcance","armadura","escudo");
    }

    public static int scoreAdjustment(String query,String title,String category,String ruleProfile) {
        if(!hasExactPrefix(title)) return -10000;
        String t=norm(title), c=norm(category), p=norm(ruleProfile);
        boolean mechanical=isMechanicalQuery(query);
        int score=0;

        if(containsAny(t,"rules cyclopedia","enciclopedia de regras")) score+=mechanical?260:130;
        else if(containsAny(t,"basic rules","expert rules","companion rules","master rules")) score+=mechanical?210:100;

        if(containsAny(t,"monstrous compendium","monster","monstro","bestiario","bestiary","creature","criatura"))
            score+=containsAny(norm(query),"monstro","criatura","dados de vida","moral","ataque","encontro")?150:45;

        if(containsAny(t,"players guide 5e","player s guide 5e","players_guide_5e")) {
            // Excelente fonte de cenário, mas não é autoridade mecânica da Rules Cyclopedia.
            score+=mechanical?-450:35;
        }
        if(containsAny(t,"mythic")) score+=mechanical?-260:-10;
        if(containsAny(t,"threshold")) score+=mechanical?-110:15;

        if(containsAny(c,"nucleo oficial de regras","autoridade mecanica principal")) score+=mechanical?120:40;
        if(containsAny(c,"adaptacao 5e","sem autoridade mecanica")) score+=mechanical?-350:10;
        if(containsAny(p,"rules cyclopedia","becmi")) score+=40;
        if(containsAny(p,"5e")) score+=mechanical?-250:0;

        // Gazetteers e livros clássicos ganham autoridade apenas em consultas de cenário/regra específica.
        if(containsAny(t,"karameikos","glantri","thyatis","alphatia","darokin","ylaruam","rockhome",
            "ethengar","minrothad","ierendi","atrughin","five shires","northern reaches","dawn of the emperors",
            "red steel","orcs of thar","shadow elves","elves of alfheim","champions of mystara")) score+=mechanical?20:85;
        return score;
    }

    public static String categoryFor(String title) {
        String t=norm(title);
        if(!hasExactPrefix(title)) return "Fora do perfil Mystara";
        if(containsAny(t,"rules cyclopedia","enciclopedia de regras")) return "Autoridade mecânica principal — Rules Cyclopedia";
        if(containsAny(t,"players guide 5e","player s guide 5e","players_guide_5e")) return "Ambientação/adaptação 5e — sem autoridade mecânica no modo clássico";
        if(containsAny(t,"mythic")) return "Oráculo narrativo — sem autoridade mecânica";
        if(containsAny(t,"threshold")) return "Material comunitário de apoio";
        if(containsAny(t,"monstrous compendium","monster","monstro","bestiario","bestiary")) return "Bestiário de Mystara";
        return "Fonte de cenário de Mystara";
    }

    public static String profileFor(String title) {
        String t=norm(title);
        if(containsAny(t,"players guide 5e","player s guide 5e","players_guide_5e")) return "Mystara 5e — lore somente no perfil Rules Cyclopedia";
        if(containsAny(t,"mythic")) return "Ferramenta narrativa — Mystara";
        if(containsAny(t,"threshold")) return "Mystara comunitário — apoio";
        return "Mystara — Rules Cyclopedia/BECMI";
    }

    private static boolean containsAny(String text,String... needles){for(String n:needles)if(text.contains(norm(n)))return true;return false;}
    private static String norm(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
