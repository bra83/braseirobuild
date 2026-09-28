package com.braseiro.pathfinder2e;

import com.braseiro.core.system.GameSystemModule;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Adaptador PF2e. Mantem regras fora do Motor Barbara e impede fallback para outro sistema. */
public final class Pathfinder2eSystemModule implements GameSystemModule {
    public static final String ID="pathfinder_2e";
    private final Pathfinder2eRulesEngine rules = new Pathfinder2eRulesEngine();
    private final CharacterRulesValidator validator = new CharacterRulesValidator();

    @Override public String id(){return ID;}
    @Override public String displayName(){return "Pathfinder 2e Remaster";}
    @Override public String rulesProfile(){return Pathfinder2eRulesEngine.PROFILE;}
    @Override public String campaignIsolationKey(){return ID;}
    @Override public String rulesAuthorityPrompt(){return rules.authority();}
    @Override public String narrativeDirective(){
        return "Dirija uma mesa viva de Pathfinder 2e: cenas em andamento, NPCs com objetivos e memoria, consequencias fora da presenca do jogador e narrativa dramatizada em blocos curtos. Nao transforme a resposta em resumo, tutorial ou paragrafo generico.";
    }
    @Override public String visualIdentityDirective(){
        return "Preserve identidade visual persistente de cada personagem. Descreva ancestralidade, idade aparente, corpo, rosto, cabelo, pele, olhos, roupas, armadura, armas, simbolos, cicatrizes e detalhes recorrentes; use contexto de cena apenas para pose, luz e ambiente, nunca para trocar a identidade.";
    }
    @Override public boolean acceptsRuleSource(String titleOrProfile){
        String n=norm(titleOrProfile);
        if(n.contains("dungeons dragons")||n.contains("d d 5e")||n.contains("mystara")||n.contains("rules cyclopedia")||n.contains("pathfinder 1e")||n.contains("pf1"))return false;
        return n.contains("pathfinder")||n.contains("pf2")||n.contains("player core")||n.contains("gm core")||n.contains("monster core")||n.contains("lost omens")||n.contains("treasure vault")||n.contains("rage of elements")||n.contains("war of immortals")||n.contains("kingmaker")||n.contains("companion guide");
    }
    @Override public JSONObject validateCharacter(JSONObject sheet){return validator.validate(sheet,rulesProfile());}
    @Override public JSONObject resolveCheck(int naturalRoll,int modifier,int dc){return rules.resolveCheck(naturalRoll,modifier,dc);}
    @Override public JSONObject turnBudget(int actionsUsed,boolean reactionUsed,boolean quickened,boolean slowed,int stunned){return rules.turnBudget(actionsUsed,reactionUsed,quickened,slowed,stunned);}

    private static String norm(String value){
        String s=value==null?"":Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9]+"," ").trim();
    }
}
