package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Guarda determinístico para apresentações de cenário e descoberta contínua.
 *
 * Não tenta escrever a ficção. Impede que uma apresentação tecnicamente longa
 * ainda seja inútil para quem não leu o cenário: orientação vem antes do
 * conflito, e uma descoberta só é aceita se o texto realmente dramatizar o
 * tópico que será marcado como conhecido no ledger da campanha.
 */
public final class WorldOnboardingNarrativeGuard {
    private WorldOnboardingNarrativeGuard(){}

    public static boolean isPresentation(String occasion){
        return "campaign_opening".equals(occasion)||"first_arrival".equals(occasion)||"changed_return".equals(occasion)||
                "poi_discovery".equals(occasion)||"geography_discovery".equals(occasion)||"weather_transition".equals(occasion);
    }

    public static String problem(String narration,String occasion){return problem(narration,occasion,null);}

    public static String problem(String narration,String occasion,JSONObject discovery){
        if(!isPresentation(occasion))return "";
        String text=narration==null?"":narration.trim();
        int words=countWords(text),paragraphs=countParagraphs(text);
        int minWords=minWords(occasion),minParagraphs=minParagraphs(occasion);
        if(paragraphs<minParagraphs)return "a apresentação veio em "+paragraphs+" parágrafo(s); "+occasion+" exige pelo menos "+minParagraphs+" parágrafos narrativos";
        if(words<minWords)return "a apresentação tem apenas "+words+" palavras; "+occasion+" exige pelo menos "+minWords+" palavras de cena contextualizada";

        String low=text.toLowerCase(new Locale("pt","BR"));
        String[] preknowledge={"como você já sabe","como voce ja sabe","como vocês já sabem","como voces ja sabem","vocês sabem que","voces sabem que","como todos sabem","é claro que vocês conhecem","e claro que voces conhecem"};
        for(String marker:preknowledge)if(low.contains(marker))return "a narração pressupõe conhecimento prévio do cenário sem prova na memória da campanha";
        if(low.startsWith("capítulo ")||low.startsWith("capitulo ")||low.startsWith("resumo:")||low.startsWith("contexto:")||low.startsWith("estado atual:"))return "a apresentação começou como estrutura editorial/relatório em vez de cena";

        int window=Math.min(text.length(),"campaign_opening".equals(occasion)?5200:3800);
        String head=text.substring(0,window).toLowerCase(new Locale("pt","BR"));
        if("campaign_opening".equals(occasion)){
            boolean scale=containsAny(head,"mundo","continente","região","regiao","reino","território","territorio","fronteira","província","provincia","costa","vale","planície","planicie","montanha","floresta","deserto","cidade","vila","terras de","parte de");
            boolean approach=containsAny(head,"estrada","caminho","rota","viagem","jornada","chegada","aproxima","ao norte","ao sul","a leste","a oeste","entre ","dentro de","fica ","situa","distância","distancia","dias de","horas de","através","atraves");
            if(!scale||!approach)return "a abertura pulou a orientação de mundo/região e fechou a câmera cedo demais; situe escala e aproximação antes do conflito local";
        }else if("first_arrival".equals(occasion)){
            boolean place=containsAny(head,"cidade","vila","bairro","porto","fortaleza","templo","ruína","ruina","aldeia","acampamento","região","regiao","entrada","muralha","estrada","trilha","ponte","praça","praca");
            boolean threshold=containsAny(head,"chega","chegada","aproxima","entrada","portão","portao","atravessa","avista","surge","caminho","estrada","trilha","limiar","primeiros","ao se aproximar");
            if(!place||!threshold)return "a primeira chegada não orientou a aproximação e o limiar do novo lugar antes de exigir ação";
        }else if("poi_discovery".equals(occasion)){
            boolean landmark=containsAny(low,"entrada","fachada","torre","ruína","ruina","templo","ponte","portão","portao","estátua","estatua","monumento","taverna","estalagem","mercado","doca","cais","santuário","santuario","caverna","masmorra","cemitério","cemiterio","forte","castelo","edifício","edificio","marco","trilha","porta");
            boolean affordance=containsAny(low,"leva a","dá acesso","da acesso","pode entrar","entrada","passagem","vigia","guarda","movimento","atividade","fila","mercadoria","sinais","rastros","interior","saída","saida");
            if(!landmark||!affordance)return "o ponto de interesse foi marcado como descoberto sem ser apresentado como lugar concreto, reconhecível e acionável";
        }else if("geography_discovery".equals(occasion)){
            boolean terrain=containsAny(low,"relevo","encosta","vale","planície","planicie","colina","montanha","serra","floresta","bosque","pântano","pantano","deserto","campo","costa","rio","riacho","lago","estrada","trilha","solo","vegetação","vegetacao","rocha","terreno");
            boolean orientation=containsAny(low,"ao norte","ao sul","a leste","a oeste","adiante","atrás","atras","à esquerda","a esquerda","à direita","a direita","sobe","desce","contorna","atravessa","segue","entre","margem","horizonte","distância","distancia");
            if(!terrain||!orientation)return "a nova geografia foi marcada como conhecida sem orientar fisicamente terreno, direção ou relação espacial";
        }else if("weather_transition".equals(occasion)){
            boolean sensory=containsAny(low,"chuva","garoa","tempestade","vento","neve","granizo","névoa","nevoa","calor","frio","umidade","nuvem","céu","ceu","trovo","relâmp","relamp","poeira","areia","visibilidade");
            boolean consequence=containsAny(low,"lama","escorrega","atras","abrigo","fecha","recolhe","patrulha","mercado","rua","estrada","trilha","caravana","entrega","rastro","pegada","visibilidade","viagem","movimento","trânsito","transito","ench","acumula","desvia");
            if(!sensory||!consequence)return "a mudança de clima foi marcada como apresentada sem sensação física e consequência concreta na cena/mundo vivo";
        }

        String topical=topicProblem(low,discovery);
        if(topical.length()>0)return topical;
        return "";
    }

    private static String topicProblem(String low,JSONObject discovery){
        if(discovery==null)return "";JSONArray topics=discovery.optJSONArray("presentation_topics");if(topics==null)return "";
        for(int i=0;i<topics.length();i++){
            String topic=topics.optString(i,"");
            if("weather".equals(topic)&&!containsAny(low,"chuva","garoa","tempestade","vento","neve","granizo","névoa","nevoa","calor","frio","umidade","nuvem","céu","ceu","poeira","visibilidade"))return "a apresentação seria aceita e marcaria o clima como conhecido, mas o clima novo não aparece concretamente na prosa";
            if("geography".equals(topic)&&!containsAny(low,"vale","planície","planicie","colina","montanha","serra","floresta","bosque","pântano","pantano","deserto","campo","costa","estrada","trilha","solo","vegetação","vegetacao","rocha","terreno","rio","lago"))return "a apresentação seria aceita e marcaria a geografia como conhecida, mas o terreno/região novo não foi dramatizado";
            if("poi".equals(topic)&&!containsAny(low,"entrada","torre","ruína","ruina","templo","ponte","portão","portao","monumento","taverna","estalagem","mercado","doca","cais","santuário","santuario","caverna","masmorra","cemitério","cemiterio","forte","castelo","edifício","edificio","marco"))return "a apresentação seria aceita e marcaria um ponto de interesse como conhecido sem torná-lo perceptível na cena";
        }
        return "";
    }

    public static int minWords(String occasion){
        if("campaign_opening".equals(occasion))return 300;if("first_arrival".equals(occasion))return 220;if("changed_return".equals(occasion))return 120;
        if("poi_discovery".equals(occasion))return 110;if("geography_discovery".equals(occasion))return 110;if("weather_transition".equals(occasion))return 80;return 0;
    }
    public static int minParagraphs(String occasion){
        if("campaign_opening".equals(occasion))return 4;if("first_arrival".equals(occasion))return 3;if("changed_return".equals(occasion))return 2;
        if("poi_discovery".equals(occasion))return 2;if("geography_discovery".equals(occasion))return 2;if("weather_transition".equals(occasion))return 2;return 1;
    }
    private static boolean containsAny(String text,String...terms){for(String term:terms)if(text.contains(term))return true;return false;}
    public static int countWords(String text){String clean=text==null?"":text.trim();return clean.length()==0?0:clean.split("\\s+").length;}
    public static int countParagraphs(String text){String clean=text==null?"":text.trim();if(clean.length()==0)return 0;String[] blocks=clean.split("\\r?\\n\\s*\\r?\\n");int count=0;for(String block:blocks)if(block!=null&&block.trim().length()>0)count++;return Math.max(1,count);}
}
