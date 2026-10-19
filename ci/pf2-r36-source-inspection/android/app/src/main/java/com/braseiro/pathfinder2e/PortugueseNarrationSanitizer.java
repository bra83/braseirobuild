package com.braseiro.pathfinder2e;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Corrige artefatos mecânicos de streaming/JSON e palavras coladas em PT-BR.
 *
 * A correção é deliberadamente conservadora: nomes próprios e palavras já
 * reconhecidas não são desmontados. Quando um token desconhecido pode ser
 * segmentado integralmente em duas a quatro palavras portuguesas conhecidas,
 * a melhor segmentação é aplicada.
 */
public final class PortugueseNarrationSanitizer {
    private static final LinkedHashMap<Pattern,String> FIXES=new LinkedHashMap<Pattern,String>();
    private static final Set<String> WORDS=new HashSet<String>();
    private static final Set<String> SHORT_WORDS=new HashSet<String>();
    private static final Set<String> NO_SPLIT=new HashSet<String>();
    private static final Pattern LETTER_TOKEN=Pattern.compile("\\p{L}{5,52}");
    private static final Pattern TRAILING_LETTERS=Pattern.compile("(\\p{L}+)$");
    private static final Pattern LEADING_LETTERS=Pattern.compile("^(\\p{L}+)");

    static{
        // Casos históricos observados em respostas reais do streaming.
        add("\\bdeuma\\b","de uma"); add("\\bdeum\\b","de um"); add("\\bdemadeira\\b","de madeira");
        add("\\bosagricultores\\b","os agricultores"); add("\\boagricultor\\b","o agricultor");
        add("\\bolocal\\b","o local"); add("\\boslocais\\b","os locais"); add("\\bumacarroça\\b","uma carroça");
        add("\\bumacarroca\\b","uma carroça"); add("\\bocarroceiro\\b","o carroceiro");
        add("\\baestrada\\b","a estrada"); add("\\baofazendeiro\\b","ao fazendeiro");
        add("\\bosperigos\\b","os perigos"); add("\\bpontilhãode\\b","pontilhão de"); add("\\bpontilhaode\\b","pontilhão de");
        add("\\bhomemveste\\b","homem veste"); add("\\bparecealiviado\\b","parece aliviado");
        add("\\bsaúdaolin\\b","saúda Olin"); add("\\bsaudaolin\\b","saúda Olin");
        add("\\bsoba\\b","sob a"); add("\\bdalvorada\\b","da alvorada"); add("\\bocabo\\b","o cabo");
        add("\\bverviajantes\\b","ver viajantes"); add("\\bviajantesarmados\\b","viajantes armados");
        add("\\brotascomerciais\\b","rotas comerciais"); add("\\bvelhaestrutura\\b","velha estrutura");
        add("\\bfigurasencapuzadas\\b","figuras encapuzadas"); add("\\bsimplescoincidência\\b","simples coincidência");
        add("\\bsimplescoincidencia\\b","simples coincidência"); add("\\btranquilidadeda\\b","tranquilidade da");
        add("\\bemestruturas\\b","em estruturas"); add("\\bdeestrada\\b","de estrada");
        add("\\bdoantigo\\b","do antigo"); add("\\bdaestrada\\b","da estrada"); add("\\bpelapoeira\\b","pela poeira");
        add("\\btrêsaventureiros\\b","três aventureiros"); add("\\btresaventureiros\\b","três aventureiros");
        add("\\bmesmamesa\\b","mesma mesa"); add("\\bpequenopouso\\b","pequeno pouso"); add("\\bervaslocais\\b","ervas locais");
        add("\\bseupovo\\b","seu povo"); add("\\bhorizonteaberto\\b","horizonte aberto"); add("\\bnaturezaaoredor\\b","natureza ao redor");
        add("\\bnão foram chamados às armas heroico\\b","não se uniram por um chamado heroico");
        add("\\bnao foram chamados as armas heroico\\b","não se uniram por um chamado heroico");
        add("\\bnão foi um chamado às armas heroico\\b","não foi um chamado heroico às armas");
        add("\\bmásintenções\\b","más intenções"); add("\\bmasintencoes\\b","más intenções");

        // Casos mostrados na captura da 4.3.1.
        add("\\bvastaextensão\\b","vasta extensão"); add("\\bvastaextensao\\b","vasta extensão");
        add("\\btrazendoconsigo\\b","trazendo consigo"); add("\\bmadeiravindo\\b","madeira vindo");
        add("\\bviajantescostumam\\b","viajantes costumam"); add("\\bnovasoportunidades\\b","novas oportunidades");
        add("\\bdeaco\\b","de aço"); add("\\bdeacoque\\b","de aço que"); add("\\bboaposição\\b","boa posição");
        add("\\bboaposicao\\b","boa posição"); add("\\bterrabatida\\b","terra batida");
        add("\\bdesfrutadeste\\b","desfruta deste"); add("\\bprimeirosraios\\b","primeiros raios");
        add("\\bvegetaçãoleve\\b","vegetação leve"); add("\\bvegetacaoleve\\b","vegetação leve");

        String common =
            "a ao aos à às o os as um uma uns umas de da do das dos em na no nas nos por pelo pela pelos pelas para " +
            "com sem sob sobre entre e ou mas que se sua seu suas seus meu minha meus minhas este esta estes estas esse essa " +
            "deste desta destes destas disso naquele naquela nesta neste nestas nestes naquele naquela " +
            "como quando enquanto onde ainda já não sim muito mais menos bem antes depois perto longe dentro fora contra até " +
            "homem mulher velho velha jovem criança menino menina anão anã elfo elfa humano humana barman andarilho mendigo " +
            "porta madeira pedra ferro aço metal estrada floresta cidade vila aldeia taverna estalagem ponte rio riacho céu sol lua " +
            "manhã tarde noite vento chuva fogo lareira mesa cadeira janela telhado parede corredor sala chão campo campos árvore árvores " +
            "aventureiro aventureiros viajante viajantes grupo companhia armas arma espada arco flecha escudo armadura mochila bolsas " +
            "simples coincidência itinerário compartilhando mesma bruto pequena pequeno pouso raiar três trocaram breves palavras rotas " +
            "seguiam pão rústico queijo cabra fresco caneca chá morno ervas locais ajustou sementes curiosidade típica povo encarando " +
            "horizonte aberto entusiasmo observava observa padrão copas distantes atenta atento sinais sutis natureza redor apreciava aprecia " +
            "solidez alvenaria antigo marco reconhecendo reconhece trabalho entalhe raramente estruturas tranquilidade ganha novo tom próximo " +
            "idoso comerciante aproxima aproximando armado armados perigo perigos sombra sombras estrutura intenção intenções " +
            "agricultor agricultores fazendeiro fazendeiros local locais carroça carroca carroceiro carroceiros mercado mercador mercadores " +
            "caminhante caminhantes patrulha patrulhas guarda guardas trilha trilhas montanha montanhas pântano pantano neve nevasca " +
            "vasta extensão trazendo consigo aroma fresca fresco terra molhada orvalho odor sutil fumaça fogueiras distantes passarinhada " +
            "canta arbustos espalhados murmúrio insetos completa atmosfera pacífica pacifica terras longo região fronteira costumam seguir " +
            "velhas tradições tradicoes cooperação cooperacao estradas novas oportunidades horizonte precisamente rotina reuniu redor paragem " +
            "improvisada ajustando paciência paciencia tiras peitoral refletem primeiros raios solares posição posicao tática tatica descansa " +
            "lado checando equilíbrio equilibrio machado brisa matinal limpa botas ouvindo atentamente farfalhar vegetação vegetacao rasteira " +
            "transcorre pressa brasas anterior apagam cinzas claras rastros batida levemente amassada margens caminho indicam poucas carroças " +
            "carrocas passaram desde última ultima detalhe natural demonstra calmaria momento dia apenas começando comecando desfruta desfrutam " +
            "aproveita aproveitam deste desta desta manhã manhãs jornada tranquilo tranquila ideal busca buscam nenhum companheiro companheiros " +
            "enfrente enfrentem trechos isolados barriga vazia notícias noticias adiante clima temperado limpo prometem garantido garantindo " +
            "reuniu bromir lirael milo peitoral disciplina valor boa posição aço vindo variadas diferentes região reino território territorio " +
            "planície planicie pradaria bosque colinas deserto lago água agua costa vale ruína ruina fortaleza cidade cidadezinha aldeia estrada " +
            "fala falou olha olhou caminha caminhou surge surgiu chega chegou deixa deixou mantém mantem encontra encontrou percebe percebeu " +
            "ouve ouviu sente sentiu avança avancou atravessa atravessou retorna retornou permanece permanecem continua continuam";
        addFolded(WORDS,common.split("\\s+"));
        addFolded(SHORT_WORDS,"a,o,as,os,e,ou,de,da,do,das,dos,em,na,no,nas,nos,por,para,com,sem,sob,ao,aos,um,uma,à,às".split(","));
        // Palavras legítimas que também podem ser decompostas em palavras do dicionário.
        addFolded(NO_SPLIT,("comporta contudo portanto embora apenas acaso agora ainda demais sobretudo entretanto " +
                "caminho campanha personagem personagens aventura aventuras completo completa companhia compreender " +
                "comportamento contemplar contato contexto conjunto contração controle corredor corrente consigo " +
                "sobretudo entorno adiante durante mediante perante conforme").split("\\s+"));
    }

    private PortugueseNarrationSanitizer(){}
    private static void addFolded(Set<String> target,String[] values){for(String value:values)if(value!=null&&value.trim().length()>0)target.add(fold(value.trim()));}
    private static void add(String regex,String replacement){FIXES.put(Pattern.compile(regex,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE),replacement);}

    public static String sanitize(String raw){
        if(raw==null)return "";
        String text=Normalizer.normalize(raw,Normalizer.Form.NFC)
                .replace('\u00a0',' ').replace("\u200B","").replace("\u2060","")
                .replace("\\n","\n").replace("\\r","").replace("\\t"," ");
        text=text.replaceAll("(?i)(^|\\s)(?:n\\s*n|nn)(?=\\s|$)","$1\n\n");
        text=text.replaceAll("(?i)(^|\\s)/n(?=\\s|$)","$1\n");

        // 1. Remove rótulos de interface vazados na prosa
        text=text.replaceAll("(?im)^\\s*(?:local atual|clima(?: atualizado)?|horário|horario|período|periodo|objetivo|status|hex|coordenada)\\s*:\\s*.*$", "");
        text=text.replaceAll("(?im)^\\s*(?:opções disponíveis|opcoes disponiveis|você pode|voce pode|ações sugeridas|acoes sugeridas)\\s*:\\s*$", "");

        // 2. Remove metalinguagem de software
        text=text.replaceAll("(?iu)\\b(?:a cena responde(?: ao último gesto)?|antes que qualquer explicação(?: seja necessária)?|a cena não pede uma resposta|o sistema calculou)[^.!?\\n]*[.!?]?", "");
        text=text.replaceAll("(?iu)\\b(?:como uma ia|como inteligência artificial|como inteligencia artificial|assistente virtual|prompt de sistema|pipeline de execução)\\b[^.!?\\n]*[.!?]?", "");

        // 3. Suaviza sequestro de agência e emoções involuntárias
        text=text.replaceAll("(?iu)\\bvocê sente medo\\b", "um calafrio percorre sua espinha");
        text=text.replaceAll("(?iu)\\bvocês sentem medo\\b", "um calafrio percorre a espinha de vocês");
        text=text.replaceAll("(?iu)\\bvocê fica impressionad[oa]\\b", "a cena chama sua atenção");
        text=text.replaceAll("(?iu)\\bvocês ficam impressionados\\b", "a cena chama a atenção de vocês");

        text=text.replaceAll("[ \\t]+"," ").replaceAll(" *\\n *","\n");
        text=text.replaceAll("([a-záàâãéêíóôõúç])([A-ZÁÀÂÃÉÊÍÓÔÕÚÇ])","$1 $2");
        for(Map.Entry<Pattern,String> entry:FIXES.entrySet())text=replacePreservingInitialCase(text,entry.getKey(),entry.getValue());
        // Duas passagens resolvem cadeias de três palavras sem permitir recursão infinita.
        text=splitJoinedWords(text);
        text=splitJoinedWords(text);
        text=text.replaceAll("\\s+([,.;:!?])","$1").replaceAll("([,.;:!?…])(?=[A-Za-zÀ-ÿ])","$1 ");
        text=text.replaceAll("([.!?…])([“\"'])(?=[A-Za-zÀ-ÿ])","$1 $2");
        text=text.replaceAll("(?<=[A-Za-zÀ-ÿ0-9])([”’\"')\\]])(?=[A-Za-zÀ-ÿ])","$1 ");
        text=text.replaceAll("(?iu)\\b(\\p{L}{3,})(?:\\s+\\1){1,2}\\b","$1");

        // 4. Remove muleta repetitiva de fim de turno ("O que você faz?")
        text=text.replaceAll("(?iu)(?:[,.;:!?…\\s]+)?\\b(?:o que você(?:s)? faz(?:em)?(?: agora)?|como você(?:s)? pretend(?:e|em) agir|qual é a sua escolha)\\??\\s*$", ".");

        text=text.replaceAll(" {2,}"," ").replaceAll("\\n{3,}","\n\n");
        return text.trim();
    }

    public static boolean hasMetaViolation(String text){
        if(text==null||text.isEmpty())return false;
        String lower=text.toLowerCase(new Locale("pt","BR"));
        return lower.matches("(?s).*\\b(?:como uma ia|assistente virtual|prompt|pipeline|json)\\b.*")
                || lower.matches("(?s).*\\b(?:a cena responde|antes que qualquer explicação)\\b.*");
    }

    /**
     * Junta eventos de streaming sem perder espaços entre palavras completas.
     * Fragmentos internos de uma palavra ("exten" + "são") continuam unidos;
     * palavras completas ("vasta" + "extensão") recebem o espaço ausente.
     */
    public static String mergeStreamingText(String current,String chunk){
        if(current==null||current.length()==0)return chunk==null?"":chunk;
        if(chunk==null||chunk.length()==0)return current;
        if(chunk.startsWith(current))return chunk;
        if(current.endsWith(chunk))return current;
        int max=Math.min(current.length(),chunk.length());
        for(int n=max;n>0;n--)if(current.regionMatches(current.length()-n,chunk,0,n))return current+chunk.substring(n);
        if(needsBoundarySpace(current,chunk))return current+" "+chunk;
        return current+chunk;
    }

    private static boolean needsBoundarySpace(String current,String chunk){
        char last=current.charAt(current.length()-1),first=chunk.charAt(0);
        if(Character.isWhitespace(last)||Character.isWhitespace(first))return false;
        if(!Character.isLetter(last)||!Character.isLetter(first))return false;
        Matcher lm=TRAILING_LETTERS.matcher(current),rm=LEADING_LETTERS.matcher(chunk);
        if(!lm.find()||!rm.find())return false;
        String left=fold(lm.group(1)),right=fold(rm.group(1));
        if(left.length()==0||right.length()==0)return false;
        String joined=left+right;
        if(WORDS.contains(joined)||NO_SPLIT.contains(joined))return false;
        if((isKnown(left)&&isKnown(right))||(SHORT_WORDS.contains(left)&&isKnown(right)))return true;
        List<Integer> segmentation=bestSegmentation(joined);
        return segmentation.size()==2&&segmentation.get(0)==left.length();
    }

    private static String splitJoinedWords(String text){
        Matcher m=LETTER_TOKEN.matcher(text);StringBuffer out=new StringBuffer();
        while(m.find()){String token=m.group();m.appendReplacement(out,Matcher.quoteReplacement(bestSplit(token)));}
        m.appendTail(out);return out.toString();
    }

    private static String bestSplit(String token){
        String folded=fold(token);
        if(isKnown(folded)||NO_SPLIT.contains(folded))return token;
        List<Integer> cuts=bestSegmentation(folded);
        if(cuts.size()<2)return token;
        StringBuilder out=new StringBuilder();int from=0;
        for(int cut:cuts){if(out.length()>0)out.append(' ');out.append(token,from,cut);from=cut;}
        return out.toString();
    }

    /** Retorna os fins de cada segmento, inclusive o comprimento total. */
    private static List<Integer> bestSegmentation(String token){
        int n=token.length();
        @SuppressWarnings("unchecked") List<Integer>[] paths=new List[n+1];
        int[] score=new int[n+1];Arrays.fill(score,Integer.MIN_VALUE);score[0]=0;paths[0]=new ArrayList<Integer>();
        for(int i=0;i<n;i++){
            if(paths[i]==null||paths[i].size()>=4)continue;
            int max=Math.min(n,i+22);
            for(int j=i+1;j<=max;j++){
                String part=token.substring(i,j);
                if(!isKnown(part))continue;
                if(part.length()==1&&!SHORT_WORDS.contains(part))continue;
                int partScore=part.length()*part.length();
                if(SHORT_WORDS.contains(part))partScore+=12;
                if(part.length()>=4)partScore+=10;
                int candidate=score[i]+partScore;
                if(candidate>score[j]){score[j]=candidate;paths[j]=new ArrayList<Integer>(paths[i]);paths[j].add(j);}
            }
        }
        if(paths[n]==null||paths[n].size()<2)return new ArrayList<Integer>();
        // Evita correções frágeis compostas apenas de partículas muito curtas.
        int longest=0,from=0;for(int cut:paths[n]){longest=Math.max(longest,cut-from);from=cut;}
        if(longest<4)return new ArrayList<Integer>();
        return paths[n];
    }

    private static boolean isKnown(String value){return WORDS.contains(value)||SHORT_WORDS.contains(value);}
    private static String fold(String value){return Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");}
    private static String replacePreservingInitialCase(String text,Pattern pattern,String replacement){Matcher matcher=pattern.matcher(text);StringBuffer out=new StringBuffer();while(matcher.find()){String use=replacement,found=matcher.group();if(found.length()>0&&Character.isUpperCase(found.codePointAt(0))&&use.length()>0)use=Character.toUpperCase(use.charAt(0))+use.substring(1);matcher.appendReplacement(out,Matcher.quoteReplacement(use));}matcher.appendTail(out);return out.toString();}
}
