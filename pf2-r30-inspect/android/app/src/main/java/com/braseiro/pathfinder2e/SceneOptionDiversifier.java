package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Impede que as sugestões opcionais de uma nova cena repitam ou apenas
 * parafraseiem as escolhas exibidas nas cenas recentes.
 *
 * A IA continua sendo a fonte primária das opções. Quando uma cena narrativa
 * chega sem opções, a geração local só cria alternativas ancoradas em elementos
 * concretos da cena. Se não houver opção confiável, o campo de ação livre permanece.
 */
public final class SceneOptionDiversifier {
    private static final int MAX_OPTIONS = 6;
    private static final int MIN_USEFUL_OPTIONS = 4;
    private static final int MAX_HISTORY = 60; // cobre mais que a janela legada: MAX_HISTORY = 24

    private SceneOptionDiversifier() {}

    public static JSONArray parseHistory(String raw) {
        try {
            return new JSONArray(raw == null || raw.trim().isEmpty() ? "[]" : raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    public static JSONArray appendHistory(JSONArray history, JSONArray options) {
        ArrayList<String> values = new ArrayList<String>();
        if (history != null) {
            for (int i = 0; i < history.length(); i++) addHistoryEntry(values, history.optString(i, ""));
        }
        ArrayList<String> additions = new ArrayList<String>();
        if (options != null) {
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.optJSONObject(i);
                if (option != null) addHistoryEntry(additions, optionAction(option));
            }
        }
        // A tela já salva as opções da cena quando elas são exibidas. Ao receber a
        // cena seguinte, previousOptions pode apontar para o mesmo par já gravado.
        // Não repita esse bloco no fim do histórico: a duplicação encurtava pela
        // metade a janela efetiva de variedade e fazia alternativas válidas sumirem.
        if (!tailMatches(values, additions)) values.addAll(additions);
        int first = Math.max(0, values.size() - MAX_HISTORY);
        JSONArray out = new JSONArray();
        for (int i = first; i < values.size(); i++) out.put(values.get(i));
        return out;
    }

    public static JSONArray diversify(JSONArray source, JSONArray recentHistory,
                                      String narration, String declaredAction,
                                      JSONObject scene, int beatNumber) {
        JSONArray out = new JSONArray();
        ArrayList<String> accepted = new ArrayList<String>();
        ArrayList<String> history = historyValues(recentHistory);
        ArrayList<String> immediateHistory = latestHistoryValues(recentHistory,MAX_OPTIONS);
        if(declaredAction!=null&&!declaredAction.trim().isEmpty()){addUnique(history,declaredAction);addUnique(immediateHistory,declaredAction);}
        int validSourceCount = 0;

        if (source != null) {
            for (int i = 0; i < source.length() && out.length() < MAX_OPTIONS; i++) {
                JSONObject original = source.optJSONObject(i);
                if (original == null) continue;
                JSONObject option = sanitize(original);
                String action = optionAction(option);
                if (action.isEmpty() || !isCompatibleWithScene(action, narration, scene) || !isGroundedInScene(action,narration,scene)) continue;
                validSourceCount++;
                if (isHistoryRepeated(action, immediateHistory) || isLongTermRepeated(action, history) || isRepeated(action, accepted) || repeatsIntent(action,accepted)) continue;
                out.put(option);
                accepted.add(action);
            }
        }

        List<JSONObject> fallbacks=contextualFallbacks(narration,declaredAction,scene,beatNumber);
        // 5.0: não existe obrigação de preencher dois cards. Uma opção só entra se
        // puder ser ligada a uma pessoa, objeto, ameaça, pista, lugar ou fato que
        // realmente esteja no estado/narração desta cena.
        for(JSONObject fallback:fallbacks){
            if(out.length()>=MAX_OPTIONS)break;
            String action=optionAction(fallback);
            if(action.isEmpty()||!isGroundedInScene(action,narration,scene)||isHistoryRepeated(action,immediateHistory)||isLongTermRepeated(action,history)||isRepeated(action,accepted)||repeatsIntent(action,accepted))continue;
            out.put(fallback);accepted.add(action);
        }
        return out;
    }

    /**
     * Em cenas jogáveis, o alvo é um leque curto de decisões realmente distintas:
     * quatro a seis quando há contexto suficiente e menos apenas quando a própria
     * ficção é estreita. Primeiro usa o modelo e os fallbacks ancorados; nunca
     * preenche a tela com frases genéricas só para atingir uma contagem.
     */
    public static JSONArray ensureAlways(JSONArray source, JSONArray recentHistory,
                                         String narration, String declaredAction,
                                         JSONObject scene, int beatNumber) {
        JSONArray out=diversify(source,recentHistory,narration,declaredAction,scene,beatNumber);
        ArrayList<String> accepted=optionValues(out);
        // Se o histórico antigo filtrou uma boa decisão que voltou a ser pertinente
        // na cena atual, recupere-a. A cena jogável vale mais que evitar repetição
        // entre sessões/crônicas diferentes.
        for(JSONObject fallback:contextualFallbacks(narration,declaredAction,scene,beatNumber)){
            if(out.length()>=MIN_USEFUL_OPTIONS)break;
            String action=optionAction(fallback);
            if(action.isEmpty()||!isGroundedInScene(action,narration,scene)||isRepeated(action,accepted)||repeatsIntent(action,accepted))continue;
            out.put(fallback);accepted.add(action);
        }
        for(JSONObject fallback:alwaysVisibleFallbacks(narration,scene,beatNumber)){
            if(out.length()>=MIN_USEFUL_OPTIONS)break;
            String action=optionAction(fallback);
            if(action.isEmpty()||!isGroundedInScene(action,narration,scene)||isRepeated(action,accepted)||repeatsIntent(action,accepted))continue;
            out.put(fallback);accepted.add(action);
        }
        return out;
    }

    private static List<JSONObject> alwaysVisibleFallbacks(String narration,JSONObject scene,int beatNumber){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();
        try{
            String object=firstNarrativeObject(narration);String npc=firstNamedNpc(scene);String group=firstAnonymousGroup(scene);
            String objective=scene==null?"":clean(scene.optString("objective",""),100);
            if(!object.isEmpty())out.add(option("EXAMINAR "+upperShort(object),"Conferir detalhes materiais antes de aceitar a interpretação mais óbvia.","Examinar "+object+" onde está, procurando autenticidade, marcas, uso recente e ligação com as pessoas presentes."));
            if(!npc.isEmpty()&&!object.isEmpty())out.add(option("PERGUNTAR A "+upperShort(npc)+" SOBRE "+upperShort(object),"Obter uma resposta específica sobre o elemento central da cena.","Perguntar a "+npc+" quem trouxe, autorizou ou controla "+object+" e o que acontecerá se o grupo não aceitar sua interpretação."));
            if(!group.isEmpty())out.add(option("OBSERVAR A REAÇÃO DE "+upperShort(group),"Descobrir se os figurantes confirmam, temem ou contestam o que está sendo dito sem iniciar confronto.","Observar discretamente a postura, atenção e reação de "+group+" enquanto a conversa continua, procurando apenas sinais visíveis."));
            if(!npc.isEmpty()&&!objective.isEmpty())out.add(option("LIGAR A PROPOSTA DE "+upperShort(npc)+" AO OBJETIVO ATUAL","Descobrir se a conversa ajuda, ameaça ou desvia o objetivo da cena.","Perguntar a "+npc+" como a proposta afeta diretamente o objetivo ‘"+objective+"’ antes de assumir qualquer compromisso."));
            String normalized=normalize(narration);
            if(containsAny(normalized,"fumaca","fumaça"))out.add(option("OBSERVAR A FUMAÇA ANTES DE SE APROXIMAR","Comparar direção, cor e pulsação da fumaça sem revelar a posição do grupo.","Observar a fumaça à distância e comparar direção, densidade e variações antes de escolher uma aproximação."));
            if(containsAny(normalized,"som","ruido","ruído","grito","voz"))out.add(option("MUDAR DE POSIÇÃO ANTES DE RESPONDER AO SOM","Evitar entregar a posição atual e procurar um ângulo melhor para identificar a origem.","Mudar de posição sob cobertura antes de responder ao som, mantendo a origem audível e uma rota de recuo."));
            if(containsAny(normalized,"porta","portao","portão","entrada","passagem","acesso"))out.add(option("PROCURAR OUTRA PASSAGEM","Evitar comprometer o grupo com o primeiro acesso disponível.","Procurar outra passagem ou ângulo de acesso próximo sem perder de vista a entrada atual."));
            if(containsAny(normalized,"patrulha","sentinela","vigia","ameaca","ameaça"))out.add(option("MANTER COBERTURA E OBSERVAR A AMEAÇA","Ganhar informação sem avançar nem anunciar a presença do grupo.","Manter cobertura e observar a ameaça até identificar movimento, alcance e uma janela segura de ação."));
            if((beatNumber&1)==1&&out.size()>1){JSONObject first=out.remove(0);out.add(first);}
        }catch(Exception ignored){/* fallback opcional; degrada para lista parcial */}
        return out;
    }

    private static String safeNpcName(Object value){
        if(value==null)return "";
        if(value instanceof JSONObject){
            JSONObject npc=(JSONObject)value;
            String name=GeneratedTextSanitizer.scalar(npc.optString("name",""),"name");
            if(name.isEmpty())name=GeneratedTextSanitizer.scalar(npc.toString(),"name");
            return clean(GeneratedTextSanitizer.humanText(name),80);
        }
        String name=firstEntityValue(value);
        return clean(GeneratedTextSanitizer.humanText(name),80);
    }

    private static String firstPresentNpc(JSONObject scene){
        if(scene==null)return "";JSONArray a=scene.optJSONArray("present_npcs");
        if(a==null)try{a=new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception ignored){a=new JSONArray();}
        for(int i=0;i<a.length();i++){String name=safeNpcName(a.opt(i));if(!name.isEmpty()&&!"voce".equalsIgnoreCase(name)&&!"você".equalsIgnoreCase(name))return name;}return "";
    }

    private static String firstNamedNpc(JSONObject scene){
        if(scene==null)return "";JSONArray a=scene.optJSONArray("present_npcs");if(a==null)try{a=new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception ignored){a=new JSONArray();}
        String fallback="";for(int i=0;i<a.length();i++){Object value=a.opt(i);String name=safeNpcName(value);if(name.isEmpty())continue;if(fallback.isEmpty())fallback=name;if(value instanceof JSONObject){JSONObject npc=(JSONObject)value;if(npc.optBoolean("anonymous",false)||npc.optBoolean("group",false)||npc.optBoolean("transient",false))continue;}return name;}return fallback;
    }

    private static String firstAnonymousGroup(JSONObject scene){
        if(scene==null)return "";JSONArray a=scene.optJSONArray("present_npcs");if(a==null)try{a=new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception ignored){a=new JSONArray();}
        for(int i=0;i<a.length();i++){JSONObject npc=a.optJSONObject(i);if(npc==null)continue;if(!(npc.optBoolean("anonymous",false)||npc.optBoolean("group",false)||npc.optBoolean("transient",false)))continue;String name=safeNpcName(npc);if(!name.isEmpty())return name;}return "";
    }

    public static boolean setsAreEquivalent(JSONArray a, JSONArray b) {
        ArrayList<String> left = optionValues(a);
        ArrayList<String> right = optionValues(b);
        if (left.isEmpty() || right.isEmpty() || left.size() != right.size()) return false;
        boolean[] used = new boolean[right.size()];
        for (String value : left) {
            boolean found = false;
            for (int i = 0; i < right.size(); i++) {
                if (!used[i] && similar(value, right.get(i))) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static JSONObject sanitize(JSONObject original) {
        try {
            String action = clean(GeneratedTextSanitizer.humanText(original.optString("action",
                    original.optString("description", original.optString("label", "")))), 300);
            String label = clean(GeneratedTextSanitizer.humanText(original.optString("label", action)), 80);
            String description = clean(GeneratedTextSanitizer.humanText(original.optString("description", "")), 180);
            String finalLabel=label.isEmpty()?action:label;
            if(isWeakOption(finalLabel,description,action))return new JSONObject();
            JSONObject out = new JSONObject();
            out.put("label", finalLabel);
            out.put("description", description);
            out.put("action", action);
            JSONObject test = original.optJSONObject("test");
            if (test != null) out.put("test", new JSONObject(test.toString()));
            return out;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static ArrayList<String> historyValues(JSONArray history) {
        ArrayList<String> values = new ArrayList<String>();
        if (history == null) return values;
        for (int i = 0; i < history.length(); i++) addUnique(values, history.optString(i, ""));
        return values;
    }

    private static ArrayList<String> latestHistoryValues(JSONArray history,int limit) {
        ArrayList<String> values=new ArrayList<String>();if(history==null||limit<=0)return values;
        for(int i=Math.max(0,history.length()-limit);i<history.length();i++)addUnique(values,history.optString(i,""));
        return values;
    }

    private static ArrayList<String> optionValues(JSONArray options) {
        ArrayList<String> values = new ArrayList<String>();
        if (options == null) return values;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null) addUnique(values, optionAction(option));
        }
        return values;
    }

    private static void addUnique(ArrayList<String> values, String value) {
        String clean = clean(value, 300);
        if (clean.isEmpty()) return;
        for (String existing : values) if (similar(clean, existing)) return;
        values.add(clean);
    }

    /** O histórico persistido conserva ordem/recência; a deduplicação semântica
     * acontece apenas ao comparar candidatos, para não perder a cena anterior. */
    private static void addHistoryEntry(ArrayList<String> values,String value){String clean=clean(value,300);if(!clean.isEmpty())values.add(clean);}

    private static boolean tailMatches(List<String> history,List<String> additions){
        if(history==null||additions==null||additions.isEmpty()||history.size()<additions.size())return false;
        int offset=history.size()-additions.size();
        for(int i=0;i<additions.size();i++)if(!normalize(history.get(offset+i)).equals(normalize(additions.get(i))))return false;
        return true;
    }

    private static boolean isRepeated(String candidate, List<String> values) {
        for (String previous : values) if (similar(candidate, previous)) return true;
        return false;
    }
    private static boolean isHistoryRepeated(String candidate,List<String> values){
        String c=normalize(candidate);if(c.isEmpty()||values==null)return false;
        for(String previous:values)if(c.equals(normalize(previous)))return true;
        return false;
    }
    private static boolean isLongTermRepeated(String candidate,List<String> values){
        // Histórico distante bloqueia apenas a mesma decisão literal. Reutilizar uma
        // boa família de abordagem em outro alvo/cena é preferível a deixar o Solo
        // sem escolhas úteis.
        return isHistoryRepeated(candidate,values);
    }
    private static boolean repeatsIntent(String candidate,List<String> accepted){
        String intent=intentSignature(candidate);if(intent.isEmpty())return false;int count=0;
        for(String previous:accepted)if(intent.equals(intentSignature(previous)))count++;
        // Uma cena útil pode legitimamente oferecer duas abordagens da mesma família
        // (por exemplo examinar rastros versus comparar direção/quantidade). O bloqueio
        // existe para impedir seis cartões quase idênticos, não para esmagar decisões.
        return count>=2;
    }

    static boolean similar(String a, String b) {
        String x = normalize(a), y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        if (Math.min(x.length(), y.length()) >= 22 && (x.contains(y) || y.contains(x))) return true;
        String ix=intentSignature(x),iy=intentSignature(y);
        if(!ix.isEmpty()&&ix.equals(iy)){
            Set<String> ex=entityTokens(x),ey=entityTokens(y);
            // Verbos da mesma família podem representar decisões honestamente
            // diferentes sobre o mesmo alvo (examinar marcas ≠ comparar marcas).
            // Só colapse automaticamente quando nenhum dos dois textos nomeia um
            // elemento concreto; o restante fica a cargo da similaridade lexical.
            if(ex.isEmpty()&&ey.isEmpty())return true;
        }

        Set<String> sx = tokens(x), sy = tokens(y);
        if (sx.isEmpty() || sy.isEmpty()) return false;
        int intersection = 0;
        for (String token : sx) if (sy.contains(token)) intersection++;
        int union = sx.size() + sy.size() - intersection;
        double jaccard = union == 0 ? 0.0 : (double) intersection / (double) union;
        double containment = (double) intersection / (double) Math.min(sx.size(), sy.size());
        // Limiares legados preservados como referência de auditoria: jaccard >= 0.68; containment >= 0.82.
        return jaccard >= 0.64 || (containment >= 0.78 && intersection >= 3) || trigramDice(x,y)>=0.82;
    }

    private static String intentSignature(String value){String n=normalize(value);
        if(n.matches(".*\\b(conversar|falar|dialogar|perguntar|questionar|interrogar|abordar|ouvir|entrevistar)\\b.*"))return "conversar";
        if(n.matches(".*\\b(examinar|investigar|inspecionar|procurar|buscar|vasculhar|analisar|observar|rastrear)\\b.*"))return "investigar";
        if(n.matches(".*\\b(atacar|golpear|combater|enfrentar|confrontar|emboscar|destruir)\\b.*"))return "confrontar";
        if(n.matches(".*\\b(fugir|recuar|retirar|escapar|evadir|abandonar)\\b.*"))return "recuar";
        if(n.matches(".*\\b(ir|seguir|viajar|avancar|mover|partir|entrar|atravessar|explorar)\\b.*"))return "mover";
        if(n.matches(".*\\b(negociar|barganhar|persuadir|convencer|trocar|comprar|vender)\\b.*"))return "negociar";
        if(n.matches(".*\\b(esconder|furtivo|furtividade|silencioso|esgueirar|infiltrar|disfarcar)\\b.*"))return "furtividade";
        if(n.matches(".*\\b(descansar|dormir|acampar|recuperar|esperar|vigiar)\\b.*"))return "descansar";
        if(n.matches(".*\\b(abrir|destrancar|forcar|romper|arrombar)\\b.*"))return "abrir";
        if(n.matches(".*\\b(ajudar|socorrer|resgatar|curar|proteger|salvar)\\b.*"))return "ajudar";
        if(n.matches(".*\\b(enganar|mentir|blefar|distrair|forjar)\\b.*"))return "enganar";
        if(n.matches(".*\\b(conjurar|magia|feitico|ritual|encantar)\\b.*"))return "magia";
        if(n.matches(".*\\b(roubar|furtar|subtrair|surrupiar)\\b.*"))return "roubar";
        return "";}

    private static double trigramDice(String a,String b){Set<String> x=trigrams(a),y=trigrams(b);if(x.isEmpty()||y.isEmpty())return 0d;int shared=0;for(String g:x)if(y.contains(g))shared++;return (2d*shared)/(x.size()+y.size());}
    private static Set<String> trigrams(String value){Set<String> out=new HashSet<String>();String s=normalize(value).replace(" ","_");for(int i=0;i+3<=s.length();i++)out.add(s.substring(i,i+3));return out;}

    private static Set<String> entityTokens(String value){Set<String> all=tokens(value);String[] generic={"conversar","falar","dialogar","perguntar","questionar","interrogar","abordar","ouvir","examinar","investigar","inspecionar","procurar","buscar","vasculhar","analisar","observar","atacar","golpear","combater","enfrentar","confrontar","fugir","recuar","retirar","escapar","seguir","viajar","avancar","mover","partir","entrar","negociar","barganhar","persuadir","convencer","esconder","furtivo","furtividade","silencioso","esgueirar","descansar","dormir","acampar","recuperar","cena","local","situacao","mudanca"};for(String word:generic)all.remove(word);return all;}

    private static Set<String> tokens(String value) {
        HashSet<String> out = new HashSet<String>();
        String[] stop = {"a","o","as","os","um","uma","de","da","do","das","dos","e","ou","em","no","na","nos","nas","para","por","com","sem","que","se","antes","depois","grupo","personagens","jogador"};
        HashSet<String> ignored = new HashSet<String>();
        for (String word : stop) ignored.add(word);
        for (String word : value.split("[^a-z0-9]+")) {
            if (word.length() >= 3 && !ignored.contains(word)) out.add(stem(word));
        }
        return out;
    }

    private static String stem(String word){String w=word;String[] suffixes={"mente","coes","cao","ando","endo","indo","ados","idos","adas","idas","ar","er","ir","os","as","s"};for(String suffix:suffixes)if(w.length()>suffix.length()+3&&w.endsWith(suffix)){w=w.substring(0,w.length()-suffix.length());break;}return w;}

    private static String normalize(String value) {
        if (value == null) return "";
        String out = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return out;
    }

    private static String optionAction(JSONObject option) {
        if (option == null) return "";
        return clean(option.optString("action",
                option.optString("description", option.optString("label", ""))), 300);
    }

    private static List<JSONObject> contextualFallbacks(String narration, String declaredAction,
                                                         JSONObject scene, int beatNumber) {
        ArrayList<JSONObject> pool = new ArrayList<JSONObject>();
        String npc = firstNamedNpc(scene);
        String clue = firstEntity(scene, "clues_json");
        String threat = firstEntity(scene, "threats_json");
        String location = scene == null ? "" : clean(scene.optString("location", ""), 70);
        String objective = scene == null ? "" : clean(scene.optString("objective", ""), 100);
        String danger = scene == null ? "" : clean(scene.optString("danger", ""), 100);
        String narrative=normalize(narration);
        String document=firstNarrativeObject(narration);
        boolean socialDemand=containsAny(narrative,"dívida","divida","cobrança","cobranca","contrato","cessão","cessao","direitos","prazo","pagamento","guilda","credor","notas promissórias","notas promissorias","exigência","exigencia","ordem","decreto","jurisdição","jurisdicao","tributo","multa");

        if (!npc.isEmpty() && socialDemand) {
            pool.add(option("NEGOCIAR A EXIGÊNCIA COM " + upperShort(npc),
                    "Questionar prazo, autoridade e contrapartida sem aceitar a cobrança automaticamente.",
                    "Negociar com " + npc + " os termos, o prazo e a prova da exigência antes de assinar, entregar ou prometer qualquer recurso."));
            if(!document.isEmpty())pool.add(option("EXAMINAR " + upperShort(document),
                    "Conferir autenticidade, cláusulas, selo, nomes e consequências antes de responder.",
                    "Examinar " + document + " diante de " + npc + ", comparando texto, selo, assinaturas e autoridade declarada antes de aceitar a cobrança."));
            else pool.add(option("EXIGIR PROVA DA COBRANÇA",
                    "Pedir origem, autoridade e registro verificável da dívida ou obrigação apresentada.",
                    "Exigir de " + npc + " a prova documental e a base da cobrança, sem assumir que a versão apresentada é verdadeira."));
            pool.add(option("PEDIR A " + upperShort(npc) + " A BASE DA AUTORIDADE",
                    "Descobrir quem autorizou a exigência e o que pode ser verificado agora; pode aumentar a tensão se a autoridade for contestada.",
                    "Pedir a " + npc + " que identifique a autoridade, o registro ou o acordo que sustenta a exigência antes de qualquer compromisso do grupo."));
            pool.add(option("PROPOR UMA CONDIÇÃO ALTERNATIVA A " + upperShort(npc),
                    "Trocar prazo, garantia ou contrapartida por uma condição concreta sem aceitar os termos atuais.",
                    "Propor a " + npc + " uma condição alternativa verificável — prazo, garantia ou contrapartida — e esperar a reação antes de prometer recursos."));
            pool.add(option("RECUSAR O COMPROMISSO IMEDIATO",
                    "Manter recursos e liberdade de ação, aceitando o risco social de endurecer a negociação.",
                    "Recusar assumir a obrigação neste instante e pedir que " + npc + " declare qual consequência concreta pretende impor agora."));
        } else if (!npc.isEmpty()) {
            pool.add(option("PEDIR A " + upperShort(npc) + " QUE DIGA O QUE QUER AGORA",
                    "Obter objetivo, motivo e consequência concreta antes de decidir.",
                    "Pedir a " + npc + " que diga exatamente o que deseja agora, por que isso importa nesta cena e o que espera que o grupo faça em seguida."));
            pool.add(option("CONFRONTAR A FALA DE " + upperShort(npc) + " COM A CENA",
                    "Comparar a declaração com objetos, testemunhas e mudanças visíveis.",
                    "Confrontar a afirmação de " + npc + " com o elemento material mais relevante da cena antes de responder."));
            pool.add(option("PEDIR A " + upperShort(npc) + " UMA PROVA CONCRETA",
                    "Separar informação verificável de opinião, medo ou interesse pessoal.",
                    "Pedir a " + npc + " que mostre a evidência concreta que sustenta o que acabou de afirmar, sem aceitar a conclusão por antecipação."));
            pool.add(option("MUDAR A POSIÇÃO DO GRUPO ANTES DE RESPONDER A " + upperShort(npc),
                    "Preservar distância, visão e saída enquanto a conversa continua.",
                    "Reposicionar o grupo sem encerrar a conversa com " + npc + ", mantendo uma rota de retirada visível antes de dar uma resposta decisiva."));
        }
        if(!document.isEmpty()&&!socialDemand){
            pool.add(option("EXAMINAR " + upperShort(document),
                    "Procurar origem, autenticidade, alterações e ligação com as pessoas presentes.",
                    "Examinar " + document + " sem afastá-lo da cena, procurando marcas, texto, selo, uso recente e quem tenta controlá-lo."));
        }
        if (!clue.isEmpty()) {
            pool.add(option("EXAMINAR " + upperShort(shortNoun(clue)),
                    "Proteger a pista contra contaminação e procurar origem, direção ou uso recente.",
                    "Examinar " + clue + " no ponto onde foi encontrada e confrontá-la com marcas, testemunhas ou objetos próximos."));
            pool.add(option("COMPARAR " + upperShort(shortNoun(clue)) + " COM O AMBIENTE",
                    "Verificar se a pista pertence ao local ou foi trazida de outro lugar.",
                    "Comparar " + clue + " com o terreno, os objetos e as rotas disponíveis antes de decidir para onde seguir."));
            pool.add(option("REGISTRAR " + upperShort(shortNoun(clue)) + " ANTES DE MOVER",
                    "Preservar posição e estado da pista para não perder informação durante a exploração.",
                    "Registrar onde " + clue + " está, sua orientação e relação com os elementos próximos antes de tocá-la ou abandonar o ponto."));
            pool.add(option("SEGUIR O QUE " + upperShort(shortNoun(clue)) + " INDICA",
                    "Transformar a pista em deslocamento concreto sem tratar hipótese como certeza.",
                    "Usar " + clue + " apenas como direção provisória e avançar até o próximo ponto em que a hipótese possa ser confirmada ou descartada."));
        }
        if (!threat.isEmpty()) {
            pool.add(option("MARCAR A POSIÇÃO DE " + upperShort(shortNoun(threat)),
                    "Identificar alcance, cobertura e rota de recuo sem iniciar o confronto.",
                    "Localizar " + threat + " com precisão e escolher uma posição que preserve cobertura e saída segura."));
            pool.add(option("CRIAR UMA DISTRAÇÃO PARA " + upperShort(shortNoun(threat)),
                    "Testar a reação da ameaça sem expor todo o grupo.",
                    "Produzir um estímulo longe do grupo para observar como " + threat + " reage antes de se comprometer."));
            pool.add(option("MUDAR PARA COBERTURA ANTES DE LIDAR COM " + upperShort(shortNoun(threat)),
                    "Reduzir exposição e manter uma rota de retirada antes de qualquer confronto.",
                    "Mover o grupo até uma cobertura já visível sem avançar sobre " + threat + ", preservando distância e saída."));
            pool.add(option("EVITAR " + upperShort(shortNoun(threat)) + " E PROCURAR OUTRA ABORDAGEM",
                    "Trocar confronto imediato por uma rota, posição ou conversa menos arriscada.",
                    "Recuar da linha direta com " + threat + " e procurar uma abordagem alternativa sem perder de vista o objetivo da cena."));
        }

        if(containsAny(narrative,"fumaca","fumaça"))pool.add(option("RASTREAR A ORIGEM DA FUMAÇA","Seguir direção, cheiro e intensidade mantendo cobertura.","Rastrear a origem da fumaça por uma rota protegida e interromper o avanço ao primeiro sinal de presença hostil."));
        if(containsAny(narrative,"patrulha","sentinela","vigia")){
            pool.add(option("MAPEAR A ROTA DA PATRULHA","Observar ritmo, intervalo e pontos cegos antes de atravessar.","Mapear o percurso da patrulha e escolher um intervalo seguro para avançar ou recuar."));
            pool.add(option("ESPERAR A JANELA ENTRE AS RONDAS","Deixar a patrulha passar e usar o intervalo mais seguro, aceitando perder tempo para reduzir exposição.","Esperar sob cobertura até a patrulha completar a ronda e atravessar somente quando o intervalo observado abrir uma janela real."));
            pool.add(option("CONTORNAR A PATRULHA","Trocar rapidez por uma rota menos exposta, procurando terreno que quebre a linha de visão dos sentinelas.","Contornar a rota da patrulha por terreno protegido, mantendo um caminho de retirada caso a passagem alternativa também esteja vigiada."));
            pool.add(option("CRIAR UMA DISTRAÇÃO LONGE DA PASSAGEM","Deslocar a atenção dos vigias sem iniciar combate direto; o ruído ou isca pode também alertar reforços.","Criar uma distração longe da passagem para puxar a patrulha para outro ponto e usar a reação dos vigias como janela de movimento."));
            pool.add(option("RECUAR E OBSERVAR MAIS UMA RONDA","Abrir mão do avanço imediato para confirmar número, padrão e direção da patrulha antes de se comprometer.","Recuar para uma posição segura e observar mais uma ronda completa da patrulha antes de escolher entre atravessar, contornar ou abandonar a rota."));
        }
        if(containsAny(narrative,"pegada","pegadas","rastro","rastros","trilha","trilhas")){
            pool.add(option("SEGUIR OS RASTROS RECENTES","Avançar enquanto as marcas ainda são legíveis; o risco é entrar no caminho de quem as deixou.","Seguir os rastros mais recentes até o próximo ponto de decisão, mantendo um marco para retornar."));
            pool.add(option("EXAMINAR OS RASTROS ANTES DE AVANÇAR","Estimar direção, quantidade, velocidade e peso antes de comprometer o grupo.","Examinar os rastros no ponto atual, comparando profundidade, espaçamento e sobreposição para inferir apenas o que as marcas realmente sustentam."));
            pool.add(option("PROCURAR UM SEGUNDO CONJUNTO DE MARCAS AO REDOR","Descobrir se alguém acompanhava, cruzou ou tentou esconder o trajeto; custa tempo e pode esfriar a pista principal.","Varrer uma faixa curta ao redor dos rastros procurando pegadas paralelas, retornos, arrasto ou marcas de quem tentou ocultar a passagem."));
            pool.add(option("AVANÇAR PARALELO À TRILHA EM SILÊNCIO","Manter a direção sem pisar sobre as marcas; reduz a chance de destruir a pista, mas pode tornar o terreno mais difícil.","Avançar em silêncio por uma linha paralela aos rastros, mantendo-os visíveis de lado e evitando caminhar diretamente sobre as marcas."));
            pool.add(option("PROCURAR UM PONTO DE INTERCEPTAÇÃO DOS RASTROS","Tentar ganhar posição à frente em vez de seguir cada marca; pode economizar tempo, mas exige não perder a referência do trajeto.","Usar a direção já confirmada dos rastros para procurar adiante um ponto natural onde o trajeto precise convergir, sem abandonar a referência da trilha atual."));
            pool.add(option("MARCAR ESTE PONTO E SEGUIR COM ROTA DE RETORNO","Prosseguir sem perder a posição da última evidência segura caso a trilha desapareça.","Marcar o ponto atual dos rastros e então avançar, mantendo uma referência clara para voltar se as marcas se perderem ou o grupo precisar recuar."));
        }
        if(npc.isEmpty()&&containsAny(narrative,"porta","portao","portão","entrada","passagem"))pool.add(option("TESTAR O ACESSO SEM SE EXPOR","Usar ferramenta, cobertura ou objeto intermediário para descobrir como o acesso reage.","Testar o acesso à distância ou sob cobertura, procurando mecanismo, ocupante e rota alternativa antes de atravessar."));
        if(npc.isEmpty()&&containsAny(narrative,"som","ruido","ruído","voz","grito"))pool.add(option("PROVOCAR UMA RESPOSTA CONTROLADA","Produzir um sinal curto a partir de cobertura e observar quem ou o que reage.","Produzir um sinal controlado longe da posição do grupo e usar a resposta para identificar presença, distância e intenção."));
        if(containsAny(narrative,"rio","agua","água","ponte"))pool.add(option("VERIFICAR A TRAVESSIA","Medir corrente, estabilidade e pontos de observação nas duas margens.","Verificar a travessia mais segura e identificar o custo de cruzar agora ou procurar outro ponto."));

        if (!objective.isEmpty()) {
            pool.add(option("AVANÇAR EM " + upperShort(shortNoun(objective)),
                    "Converter o objetivo atual em uma etapa observável e concluível nesta cena.",
                    "Escolher a próxima etapa concreta de “" + objective + "” e executá-la sem abandonar cobertura, testemunhas ou evidências já obtidas."));
        }
        if (!danger.isEmpty()) {
            pool.add(option("REDUZIR O RISCO DE " + upperShort(shortNoun(danger)),
                    "Mudar posição, preparação ou abordagem antes que o perigo se transforme em consequência.",
                    "Identificar o sinal verificável de “" + danger + "” e reduzir esse risco com cobertura, distância, ferramenta ou negociação adequada."));
        }
        if (!location.isEmpty()) {
            pool.add(option("ESCOLHER UM PONTO DE OBSERVAÇÃO",
                    "Ganhar visão do local sem bloquear a própria retirada.",
                    "Escolher em " + location + " um ponto de observação com cobertura, campo de visão e rota de saída definidos."));
        }
        if(npc.isEmpty()&&clue.isEmpty()&&threat.isEmpty()&&objective.isEmpty()&&danger.isEmpty()&&document.isEmpty()){
            String recent=lastSentence(narration);
            if(!recent.isEmpty()&&containsAny(narrative,"pedido","exigência","exigencia","oferta","ameaça","ameaca","ordem"))pool.add(option("RESPONDER À EXIGÊNCIA ATUAL",
                    "Escolher entre aceitar, recusar ou pedir condições com base no que foi realmente dito.",
                    "Responder ao ponto de decisão expresso em “"+recent+"”, nomeando a condição que o grupo aceita, recusa ou deseja renegociar."));
        }

        // Sem entidade, pista, ameaça, objetivo ou objeto concreto, é melhor
        // deixar o campo de ação livre do que inventar escolhas genéricas.
        return pool;
    }

    private static boolean isWeakOption(String label,String description,String action){
        String l=normalize(label),d=normalize(description),a=normalize(action);
        if(a.length()<14||l.length()<5)return true;
        String[] weak={"agir sobre a mudanca","ler a situacao","buscar um novo angulo","testar outra hipotese","testar a consequencia da acao","verificar o que mudou","perguntar o que mudou","fazer alguma coisa","continuar a aventura","observar o cenario","reconhecer o local","examinar a entrada","localizar o som","proteger uma rota de recuo","confirmar o objetivo imediato","verificar o detalhe mais recente","verificar a travessia"};
        for(String value:weak)if(l.equals(value)||a.equals(value)||l.startsWith(value+" "))return true;
        if(d.length()>30&&a.contains(d)&&d.split(" ").length>8)return true;
        if(l.matches("^(agir|fazer|continuar|prosseguir|investigar|observar)$"))return true;
        if(l.matches("^testar (a )?consequencia.*")||a.matches(".*\bverificar o que mudou\b.*"))return true;
        if(l.matches("^perguntar a .{2,80} o que mudou.*")||a.matches("^perguntar a .{2,80} qual mudanca acabou de perceber.*"))return true;
        return false;
    }

    private static String shortNoun(String value){String clean=clean(value,42);int comma=clean.indexOf(',');if(comma>6)clean=clean.substring(0,comma);return clean;}
    private static boolean containsAny(String value,String... terms){String n=" "+normalize(value)+" ";for(String term:terms){String t=normalize(term);if(!t.isEmpty()&&n.contains(" "+t+" "))return true;}return false;}

    static boolean isGroundedInScene(String action,String narration,JSONObject scene){
        String a=normalize(action);String context=normalize((narration==null?"":narration)+" "+(scene==null?"":scene.toString()));
        if(a.isEmpty()||context.isEmpty())return false;
        Set<String> candidate=entityTokens(a),world=tokens(context);int shared=0;for(String token:candidate)if(world.contains(token))shared++;
        if(shared>0)return true;
        // Famílias semânticas equivalentes ainda contam como a mesma âncora concreta.
        // Isso evita descartar "seguir os rastros" quando a prosa chama a evidência
        // de pegadas, trilha ou marcas.
        if(containsAny(a,"rastro","rastros","trilha","trilhas","pegada","pegadas","marca","marcas")&&
                containsAny(context,"rastro","rastros","trilha","trilhas","pegada","pegadas","marca","marcas"))return true;
        if(containsAny(a,"porta","portao","portão","entrada","passagem","acesso")&&
                containsAny(context,"porta","portao","portão","entrada","passagem","acesso","arco"))return true;
        if(containsAny(a,"rio","riacho","agua","água","ponte","travessia","margem")&&
                containsAny(context,"rio","riacho","agua","água","ponte","travessia","margem"))return true;
        // Verbos sozinhos não são âncora. Apenas elementos concretos explicitamente
        // presentes autorizam ações sem nome próprio.
        String[] anchors={"contrato","documento","carta","mapa","chave","lacre","porta","portao","entrada","passagem","rastro","trilha","pegada","fumaca","fogo","rio","ponte","agua","patrulha","sentinela","vigia","ameaca","pista","tesouro","corpo","altar","ruina","caverna","estrada","caravana"};
        for(String anchor:anchors)if(containsAny(a,anchor)&&containsAny(context,anchor))return true;
        return false;
    }

    static boolean isCompatibleWithScene(String action,String narration,JSONObject scene){
        String candidate=normalize(action);String context=normalize((narration==null?"":narration)+" "+(scene==null?"":scene.toString()));
        if(containsAny(candidate,"travessia","atravessar","ponte","corrente","margens","rio")&&!containsAny(context,"travessia","atravessar","ponte","corrente","margens","rio","riacho","água","agua"))return false;
        if(containsAny(candidate,"porta","portão","portao","entrada","passagem")&&!containsAny(context,"porta","portão","portao","entrada","passagem","arco","acesso"))return false;
        if(containsAny(candidate,"pegadas","rastro","rastros","trilha")&&!containsAny(context,"pegadas","rastro","rastros","trilha","marcas"))return false;
        if(containsAny(candidate,"fumaça","fumaca")&&!containsAny(context,"fumaça","fumaca","incêndio","incendio","fogo"))return false;
        if(containsAny(candidate,"som","ruído","ruido","grito","voz")&&!containsAny(context,"som","ruído","ruido","grito","voz"))return false;
        return true;
    }

    private static String firstNarrativeObject(String narration){
        String n=normalize(narration);String[][] objects={{"notas promissorias","as notas promissórias"},{"decreto","o decreto"},{"documento original","o documento original"},{"documento","o documento"},{"contrato","o contrato"},{"papeis","os papéis"},{"lacre vermelho","o lacre vermelho"},{"lacre","o lacre"},{"anel de selo","o anel de selo"},{"mapa","o mapa"},{"carta","a carta"},{"chave","a chave"},{"caixas de ferro","as caixas de ferro"},{"caixa de ferro","a caixa de ferro"},{"corpo","o corpo"},{"objeto","o objeto"}};for(String[] object:objects)if(containsAny(n,object[0]))return object[1];return "";
    }

    private static String firstEntity(JSONObject scene, String key) {
        if (scene == null) return "";
        String alias=key.endsWith("_json")?key.substring(0,key.length()-5):key;
        Object raw=scene.has(alias)?scene.opt(alias):scene.opt(key);
        if(raw==null||clean(String.valueOf(raw),120).isEmpty()||"[]".equals(String.valueOf(raw).trim()))raw=scene.opt(key);
        return firstEntityValue(raw);
    }

    private static String firstEntityValue(Object raw) {
        if(raw==null)return "";
        if(raw instanceof JSONArray){
            JSONArray values=(JSONArray)raw;
            for(int i=0;i<values.length();i++){String found=firstEntityValue(values.opt(i));if(!found.isEmpty())return found;}
            return "";
        }
        if(raw instanceof JSONObject){
            JSONObject object=(JSONObject)raw;
            String[] keys={"name","title","description","content","summary","label"};
            for(String candidate:keys){
                String text=clean(GeneratedTextSanitizer.scalar(object.toString(),candidate),100);
                if(!text.isEmpty()&&!looksStructured(text))return text;
            }
            return "";
        }
        String text=clean(String.valueOf(raw),500);
        if(text.isEmpty()||"null".equalsIgnoreCase(text)||"[]".equals(text)||"{}".equals(text))return "";
        try{return firstEntityValue(new JSONArray(text));}catch(Exception ignored){/* texto simples */}
        try{return firstEntityValue(new JSONObject(text));}catch(Exception ignored){/* texto simples */}
        String scalar=clean(GeneratedTextSanitizer.scalar(text,"name"),100);
        if(!scalar.isEmpty()&&!looksStructured(scalar))return scalar;
        return clean(GeneratedTextSanitizer.humanText(text.replace("[","").replace("]","").replace("\"","")),100);
    }

    private static boolean looksStructured(String value){
        if(value==null)return false;
        String n=value.toLowerCase(Locale.ROOT);
        return n.contains("visual_description")||n.contains("stats_json")||
                (n.contains("{")&&n.matches("(?s).*\\b(name|nome|role|papel|summary|resumo)\\b\\s*[:=].*"));
    }

    private static String lastSentence(String narration) {
        String text = clean(narration, 1200);
        if (text.isEmpty()) return "";
        int start = Math.max(text.lastIndexOf('.', Math.max(0, text.length() - 2)),
                Math.max(text.lastIndexOf('!', Math.max(0, text.length() - 2)),
                        text.lastIndexOf('?', Math.max(0, text.length() - 2))));
        String sentence = start >= 0 && start + 1 < text.length() ? text.substring(start + 1).trim() : text;
        if (sentence.length() < 24) {
            int previous = Math.max(text.lastIndexOf('.', Math.max(0, start - 1)),
                    Math.max(text.lastIndexOf('!', Math.max(0, start - 1)),
                            text.lastIndexOf('?', Math.max(0, start - 1))));
            if (previous >= 0 && previous + 1 < text.length()) sentence = text.substring(previous + 1).trim();
        }
        return clean(sentence, 150);
    }

    private static JSONObject option(String label, String description, String action) {
        try {
            return new JSONObject()
                    .put("label", clean(label, 80))
                    .put("description", clean(description, 180))
                    .put("action", clean(action, 300));
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static String upperShort(String value) {
        return clean(value, 42).toUpperCase(Locale.ROOT);
    }

    private static String clean(String value, int max) {
        String out = value == null ? "" : value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (out.length() <= max) return out;
        int cut = out.lastIndexOf(' ', max - 1);
        if (cut < max / 2) cut = max - 1;
        return out.substring(0, cut).replaceAll("[ ,;:.-]+$", "").trim() + "…";
    }
}
