package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decide quando o índice local de manuais precisa realmente ser consultado.
 *
 * Princípio RUN111: busca por necessidade mecânica, não por mensagem/rodada.
 * Mecânicas nucleares já compiladas ficam no motor local. Regras específicas
 * consultam o índice uma vez e podem ser reutilizadas por cena/procedimento.
 */
public final class RuleRetrievalPolicy {
    public static final String CACHE_NONE="none";
    public static final String CACHE_SCENE="scene";
    public static final String CACHE_PROCEDURE="procedure";

    public static final class Decision {
        public final boolean searchRequired;
        public final boolean mechanicalCandidate;
        public final String cacheScope;
        public final String cacheKey;
        public final String reason;
        Decision(boolean search,boolean mechanical,String scope,String key,String why){searchRequired=search;mechanicalCandidate=mechanical;cacheScope=scope;cacheKey=key==null?"":key;reason=why==null?"":why;}
    }

    private RuleRetrievalPolicy(){}

    public static Decision decide(String mode,String inputChannel,String text,boolean opening,boolean combatRunning){
        String n=norm(text),m=norm(mode),channel=inputChannel==null?"":inputChannel.trim().toUpperCase(Locale.ROOT);
        if(opening){
            boolean grounded=openingNeedsGrounding(n);
            return grounded?search(true,CACHE_SCENE,"opening","abertura com foco mecânico/contextual")
                    :skip(false,"abertura puramente ficcional");
        }
        if(m.contains("regras raw")||m.contains("sortear dos livros")){
            String key=specificProcedureKey(n);if(key.length()==0)key="explicit_rules:"+compactTopic(n);
            return search(true,CACHE_PROCEDURE,key,"modo de consulta explícita aos manuais");
        }
        if("GM_HELP".equals(channel)){
            if(explicitRuleQuestion(n)){
                String key=specificProcedureKey(n);if(key.length()==0)key="gm_rule:"+compactTopic(n);
                return search(true,CACHE_PROCEDURE,key,"pergunta auxiliar explicitamente mecânica");
            }
            return skip(false,"consulta auxiliar observacional/narrativa não avança a ficção");
        }
        if(n.contains("mecanica_local_verificada")||n.contains("mecanica local verificada")||n.contains("resultado de teste obrigatorio"))
            return skip(true,"resultado já resolvido e vinculado pelo motor local");

        String special=specificProcedureKey(n);
        if(special.length()>0)return search(true,CACHE_PROCEDURE,special,"regra/procedimento específico ainda precisa de evidência");

        String skill=skillProcedureKey(n);
        if(skill.length()>0)return search(true,CACHE_SCENE,skill,"ação concreta introduziu uma questão mecânica de perícia");

        if(containsAny(n,"viaj","marcha","forrage","naveg","atravess","terreno dificil","pântano","pantano","escalada longa"))
            return search(true,CACHE_SCENE,"travel_procedure","procedimento de viagem/exploração ainda não ancorado nesta cena");

        if(containsAny(n,"ataco","ataque","golpeio","disparo","acerto","dano","movo","movimento","corro","dash","desengajo","desengajar","esquivo","dodge","descanso curto","descanso longo","short rest","long rest"))
            return skip(true,combatRunning?"mecânica nuclear de combate já compilada no motor local":"mecânica nuclear já compilada no motor local");

        if(looksMechanical(n))return search(true,CACHE_SCENE,"contextual_mechanic:"+compactTopic(n),"nova consequência mecânica não coberta com segurança pelo núcleo local");
        return skip(false,"interação narrativa sem nova necessidade mecânica");
    }

    private static Decision search(boolean mechanical,String scope,String key,String why){return new Decision(true,mechanical,scope,key,why);}
    private static Decision skip(boolean mechanical,String why){return new Decision(false,mechanical,CACHE_NONE,"",why);}

    static boolean openingNeedsGrounding(String n){
        if(n.length()==0)return false;
        if(n.contains("foco de regras para a abertura"))return true;
        return containsAny(n,"explor","viag","social","negoci","investig","percep","perigo","encontro","combate","armadil","magia","furtiv","clima","terreno");
    }

    static boolean explicitRuleQuestion(String n){
        return containsAny(n,"consulte os manuais","regra","raw","rai","manual","livro","pagina","página","como funciona","mecanica","mecânica","qual cd","dificuldade","salvaguarda","saving throw","vantagem","desvantagem","acao bonus","ação bônus","reacao","reação","concentracao","concentração","ataque de oportunidade","opportunity attack","condicao","condição","talento","feat","habilidade de classe","class feature","magia","spell");
    }

    static String skillProcedureKey(String n){
        if(containsAny(n,"escal","nado","nadar","saltar","pular","empurrar objeto","forcar porta","forçar porta","arrombar"))return "skill:athletics";
        if(containsAny(n,"furtiv","esconder","me escond","sneak","hide"))return "skill:stealth";
        if(containsAny(n,"rastro","rastre","sobreviv","forrage","orientar","navigation","naveg"))return "skill:survival";
        if(containsAny(n,"investig","examino","examinar mecanismo","procurar pista","procuro pista","search clues"))return "skill:investigation";
        if(containsAny(n,"procuro armad","percepc","escuto","ouvir","vigio","observo atent","spot","perception"))return "skill:perception";
        if(containsAny(n,"intimid","ameaco","ameaço"))return "skill:intimidation";
        if(containsAny(n,"persuad","persuas","convenc","negoci"))return "skill:persuasion";
        if(containsAny(n,"engan","blefar","mentir","deception"))return "skill:deception";
        if(containsAny(n,"intuic","intuição","insight","ler intenc"))return "skill:insight";
        if(containsAny(n,"arcan","religiao","religião","natureza","medicina","historia","história"))return "skill:knowledge";
        return "";
    }

    static String specificProcedureKey(String n){
        String named=extractNamed(n,"(?:conjuro|conjurar|lan[cç]o|usar? a magia|magia|spell)\\s+([a-z0-9' -]{2,48})");
        if(named.length()>0)return "spell:"+compactTopic(named);
        named=extractNamed(n,"(?:talento|feat|habilidade de classe|class feature|manobra|maneuver)\\s+([a-z0-9' -]{2,48})");
        if(named.length()>0)return "feature:"+compactTopic(named);
        if(containsAny(n,"agarrar","grapple","empurrar","shove"))return "combat:grapple_shove";
        if(containsAny(n,"ataque de oportunidade","opportunity attack"))return "combat:opportunity_attack";
        if(containsAny(n,"reacao","reação","reaction"))return "combat:reaction:"+compactTopic(n);
        if(containsAny(n,"concentracao","concentração","concentration"))return "spell:concentration";
        if(containsAny(n,"condicao","condição","condition","exaust","invis","cego","enven","paralis","atordo","restrained","prone","grappled"))return "condition:"+compactTopic(n);
        if(containsAny(n,"fabric","criar item","craft","downtime","tempo livre","treinar","pesquis"))return "downtime:"+compactTopic(n);
        if(containsAny(n,"item magico","item mágico","magic item","propriedade da arma","weapon mastery","maestria"))return "item:"+compactTopic(n);
        if(containsAny(n,"habilidade especial","special ability","lair action","legendary action","acao lendaria","ação lendária"))return "monster_feature:"+compactTopic(n);
        return "";
    }

    static boolean looksMechanical(String n){
        return containsAny(n,"tento","teste","rolar","rola","cd ","dc ","d20","dano","cura","pv ","hp ","salvaguarda","saving","ataco","combate","magia","conjuro","uso ","ativar","gasto","recurso","condicao","condição","vantagem","desvantagem","fabric","trein","viaj","marcha","persegu","armadil","perigo");
    }

    private static String extractNamed(String n,String regex){try{Matcher m=Pattern.compile(regex).matcher(n);if(!m.find())return "";String value=m.group(1).trim();int cut=value.indexOf(" para ");if(cut>1)value=value.substring(0,cut);cut=value.indexOf(" contra ");if(cut>1)value=value.substring(0,cut);return value;}catch(Exception ignored){return "";}}
    private static boolean containsAny(String n,String... values){for(String v:values)if(n.contains(norm(v)))return true;return false;}
    static String compactTopic(String raw){String n=norm(raw).replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();if(n.length()==0)return "generic";String[] words=n.split(" ");StringBuilder b=new StringBuilder();int used=0;for(String w:words){if(w.length()<3||STOP.contains(" "+w+" "))continue;if(b.length()>0)b.append('_');b.append(w);if(++used>=6)break;}return b.length()==0?"generic":b.toString();}
    private static final String STOP=" de da do das dos e em no na nos nas um uma para por com que o a os as ao aos sua seu suas seus esta este isso isto jogador mestre cena ";
    static String norm(String value){if(value==null)return "";String s=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);return s.replaceAll("\\s+"," ").trim();}
}
