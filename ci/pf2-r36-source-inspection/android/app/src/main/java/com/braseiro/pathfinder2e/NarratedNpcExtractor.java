package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai participantes realmente presentes na prosa visível.
 *
 * Além de nomes próprios, reconhece figurantes anônimos que agem em primeiro
 * plano ("os dois corredores avançam", "um velho ergue o cajado") e grupos
 * explicitamente presentes. Esses figurantes são marcados como transitórios:
 * aparecem na barra da cena sem poluir a memória persistente de NPCs.
 */
public final class NarratedNpcExtractor {
    private static final String PROPER="[A-ZÁÉÍÓÚÂÊÔÃÕÇ][\\p{L}'’-]{1,28}";
    private static final String NAME=PROPER+"(?:(?:\\s+"+PROPER+")|(?:\\s+(?:de|da|do|dos|das)\\s+"+PROPER+")){0,2}";
    private static final String TITLES="barão|baronesa|lorde|lady|rei|rainha|duque|duquesa|conde|condessa|capitão|capitã|general|mestre|mestra|sacerdote|sacerdotisa|mercador|mercadora|prefeito|prefeita|arquimago|arquimaga";
    private static final Pattern TITLED=Pattern.compile("(?u)\\b((?i:"+TITLES+"))\\s+("+NAME+")\\b");
    private static final Pattern INTRODUCED=Pattern.compile("(?u)(?i:\\bchamad[oa]|\\bconhecid[oa] como|\\bse apresenta como|\\bdiz se chamar|\\bo nome (?:dele|dela) é)\\s+("+NAME+")");
    private static final Pattern ACTING=Pattern.compile("(?u)\\b("+NAME+")\\s+(?i:diz|fala|responde|pergunta|sussurra|grita|avisa|aponta|ergue|oferece|entrega|entra|surge|aproxima-se|observa|recua|sorri|hesita|declara|ordena|exige|anuncia|prossegue)\\b");
    private static final Pattern INVERTED_ATTRIBUTION=Pattern.compile("(?u)(?i:\\b(?:diz|fala|responde|pergunta|sussurra|grita|avisa|anuncia|declara|ordena|exige|prossegue))\\s+("+NAME+")\\b");

    private static final String GENERIC_ROLES=
            "velho|velha|homem|mulher|rapaz|moça|moca|garoto|garota|criança|crianca|"+
            "aldeão|aldeã|aldeoes|aldeões|camponês|camponesa|camponeses|camponesas|"+
            "viajante|viajantes|corredor|corredora|corredores|corredoras|transeunte|transeuntes|"+
            "guarda|guardas|soldado|soldada|soldados|soldadas|mercador|mercadora|mercadores|mercadoras|"+
            "estalajadeiro|estalajadeira|ferreiro|ferreira|sacerdote|sacerdotisa|acólito|acolito|acólita|acolita|"+
            "mensageiro|mensageira|batedor|batedora|bandido|bandida|bandidos|bandidas|cavaleiro|cavaleira|cavaleiros|cavaleiras|"+
            "marinheiro|marinheira|marinheiros|marinheiras|pescador|pescadora|pescadores|pescadoras|"+
            "carreteiro|carreteira|artesão|artesao|artesã|artesa|artesãos|artesaos|nobre|nobres|criado|criada|criados|criadas|serviçal|servical|serviçais|servicais|"+
            "refugiado|refugiada|refugiados|refugiadas|caçador|cacador|caçadora|cacadora|caçadores|cacadores|"+
            "anão|anao|anã|ana|anões|anoes|elfo|elfa|elfos|elfas|halfling|halflings|gnomo|gnoma|gnomos|gnomas";
    private static final String ACTIONS=
            "avança|avança|avancam|avançam|corre|correm|caminha|caminham|entra|entram|sai|saem|"+
            "fala|falam|diz|dizem|responde|respondem|pergunta|perguntam|sussurra|sussurram|grita|gritam|"+
            "avisa|avisam|aponta|apontam|ergue|erguem|oferece|oferecem|entrega|entregam|surge|surgem|"+
            "aproxima-se|aproximam-se|observa|observam|recua|recuam|sorri|sorriem|hesita|hesitam|declara|declaram|"+
            "ordena|ordenam|exige|exigem|bloqueia|bloqueiam|segura|seguram|carrega|carregam|empurra|empurram|"+
            "puxa|puxam|ataca|atacam|ameaça|ameaca|ameaçam|ameacam|acena|acenam|para|param|interrompe|interrompem|vigia|vigiam|patrulha|patrulham|aguarda|aguardam|cerca|cercam|acompanha|acompanham";
    private static final String COUNTS="dois|duas|três|tres|quatro|cinco|seis|sete|oito|nove|dez|\\d{1,2}";
    private static final Pattern GENERIC_ACTOR=Pattern.compile(
            "(?iu)\\b(?:(um|uma|o|a|os|as)\\s+)?(?:((?:"+COUNTS+"))\\s+)?("+GENERIC_ROLES+")\\b(?=[^.!?\\n]{0,120}\\b(?:"+ACTIONS+")\\b)");
    private static final Pattern GROUP_PRESENT=Pattern.compile(
            "(?iu)\\b(?:um|uma|o|a)?\\s*(?:grupo|aglomerado|multidão|multidao|fila|bando)\\s+de\\s+(viajantes|mercadores|guardas|soldados|refugiados|camponeses|aldeões|aldeoes|transeuntes|marinheiros|pescadores)\\b");

    private NarratedNpcExtractor(){}

    public static JSONArray extract(String narration,int limit){return extract(narration,limit,new HashSet<String>());}

    public static JSONArray extract(String narration,int limit,Set<String> excludedIdentities){
        JSONArray out=new JSONArray();if(narration==null||narration.trim().isEmpty()||limit<=0)return out;Set<String> seen=new HashSet<String>();Set<String> excluded=excludedIdentities==null?new HashSet<String>():excludedIdentities;
        collectTitled(out,seen,excluded,narration,limit);
        collectNamed(out,seen,excluded,INTRODUCED,narration,limit,"NPC presente","Introduzido nominalmente na narração atual.");
        collectNamed(out,seen,excluded,ACTING,narration,limit,"NPC presente","Personagem nomeado e ativo na narração atual.");
        collectNamed(out,seen,excluded,INVERTED_ATTRIBUTION,narration,limit,"NPC presente","Personagem nomeado por atribuição de fala/ação na narração atual.");
        collectGroups(out,seen,narration,limit);
        collectGenericActors(out,seen,narration,limit);
        return out;
    }

    private static void collectTitled(JSONArray out,Set<String> seen,Set<String> excluded,String text,int limit){
        Matcher matcher=TITLED.matcher(text);while(matcher.find()&&out.length()<limit){String title=titleCase(matcher.group(1));String proper=matcher.group(2).trim();addNamed(out,seen,excluded,title+" "+proper,title,"Autoridade ou figura titulada presente na cena atual.");}
    }

    private static void collectNamed(JSONArray out,Set<String> seen,Set<String> excluded,Pattern pattern,String text,int limit,String role,String summary){
        Matcher matcher=pattern.matcher(text);while(matcher.find()&&out.length()<limit)addNamed(out,seen,excluded,matcher.group(1),role,summary);
    }

    private static void collectGroups(JSONArray out,Set<String> seen,String text,int limit){
        Matcher matcher=GROUP_PRESENT.matcher(text);while(matcher.find()&&out.length()<limit){
            String role=displayRole(matcher.group(1));
            addAnonymous(out,seen,role,"GRUPO PRESENTE","Grupo visível na cena atual.",-1,true);
        }
    }

    private static void collectGenericActors(JSONArray out,Set<String> seen,String text,int limit){
        Matcher matcher=GENERIC_ACTOR.matcher(text);while(matcher.find()&&out.length()<limit){
            if(followedByProperName(text,matcher.end()))continue;
            String countWord=matcher.group(2);String roleRaw=matcher.group(3);int count=parseCount(countWord);
            boolean group=count>1||isPlural(roleRaw);
            String display=displayRole(roleRaw);
            addAnonymous(out,seen,display,group?"GRUPO PRESENTE":"NPC PRESENTE","Participante anônimo que age em primeiro plano na narração atual.",count,group);
        }
    }

    private static void addNamed(JSONArray out,Set<String> seen,Set<String> excluded,String name,String role,String summary){
        String clean=name==null?"":name.trim().replaceAll("[.,;:!?…]+$","");String key=normalize(clean);if(clean.length()<2||key.length()<2||isReservedIdentity(key)||(excluded!=null&&excluded.contains(key))||!seen.add(key))return;
        try{out.put(new JSONObject().put("name",clean).put("role",role).put("summary",summary).put("present",true));}catch(Exception ignored){/* objeto local simples */}
    }

    private static void addAnonymous(JSONArray out,Set<String> seen,String name,String role,String summary,int count,boolean group){
        String clean=name==null?"":name.trim();String key=normalize(clean);if(clean.length()<2||key.length()<2||isReservedIdentity(key)||!seen.add(key))return;
        try{
            JSONObject npc=new JSONObject().put("name",clean).put("role",role).put("summary",summary).put("present",true)
                    .put("anonymous",true).put("transient",true).put("group",group);
            if(count>0)npc.put("count",count);
            out.put(npc);
        }catch(Exception ignored){/* objeto local simples */}
    }

    private static boolean followedByProperName(String text,int end){if(text==null||end<0||end>=text.length())return false;int i=end;while(i<text.length()&&Character.isWhitespace(text.charAt(i)))i++;if(i>=text.length())return false;char c=text.charAt(i);return Character.isUpperCase(c);}
    private static int parseCount(String raw){if(raw==null||raw.trim().isEmpty())return 1;String n=normalize(raw);if("dois".equals(n)||"duas".equals(n))return 2;if("tres".equals(n))return 3;if("quatro".equals(n))return 4;if("cinco".equals(n))return 5;if("seis".equals(n))return 6;if("sete".equals(n))return 7;if("oito".equals(n))return 8;if("nove".equals(n))return 9;if("dez".equals(n))return 10;try{return Math.max(1,Math.min(99,Integer.parseInt(n)));}catch(Exception ignored){return 1;}}
    private static boolean isPlural(String raw){String n=normalize(raw);return n.endsWith("s")||n.endsWith("oes")||n.endsWith("ais");}
    private static String displayRole(String value){if(value==null||value.trim().isEmpty())return "NPC";String lower=value.trim().toLowerCase(new Locale("pt","BR"));return lower.substring(0,1).toUpperCase(new Locale("pt","BR"))+lower.substring(1);}
    private static final String[] RESERVED_IDENTITIES = new String[]{
        "voce","voces","eu","nos","ele","ela","eles","elas","jogador","jogadora","jogadores","jogadoras","o jogador","a jogadora",
        "grupo","o grupo","grupo de aventureiros","companhia","a companhia","heroi","herois","aventureiro","aventureiros",
        "player","players","party","the party","narrador","narradora","mestre do jogo","game master","gm",
        "restov","brevoy","rostland","issia","pitax","varnhold","drelev","fort drelev","golarion","terras roubadas","stolen lands",
        "cinturao verde","greenbelt","narlmarches","floresta narlmarches","kamelands","dunsward","hooktongue","pantano hooktongue",
        "glenebon","altiplanos de glenebon","thousand voices","tors of levenies","rio shrike","rio thorn","lago candlemere","tuskwater",
        "lago silverstep","lago hooktongue","lago rushlight","posto comercial de oleg","posto de oleg","oleg","forte serenko","serenko",
        "nivakta","travessia de nivakta","littletown","velho sicomoro","cavernas sootscale","sootscale","templo do alce",
        "fortaleza do senhor dos cervos","senhor dos cervos","ilha candlemere","candlemere","cabana de bokken","bokken",
        "arbustos fangberry","fangberry","travessia de nettle","nettle","covil do tatzlwyrm","tatzlwyrm","covil de tuskgutter",
        "tuskgutter","fortaleza de hargulka","hargulka","tatzlford","tumba de vordakai","vordakai","senhores tigres","tiger lords"
    };

    private static boolean isReservedIdentity(String normalized){
        for(String value:RESERVED_IDENTITIES)if(value.equals(normalized))return true;
        return false;
    }
    private static String titleCase(String value){if(value==null||value.isEmpty())return "";String lower=value.toLowerCase(new Locale("pt","BR"));return lower.substring(0,1).toUpperCase(new Locale("pt","BR"))+lower.substring(1);}
    private static String normalize(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
}
