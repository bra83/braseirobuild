package com.braseiro.dnd5e;

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
    private static final int MAX_OPTIONS = 2;
    private static final int MAX_HISTORY = 60; // cobre mais que a janela legada: MAX_HISTORY = 24

    private SceneOptionDiversifier() {}

    public static JSONArray parseHistory(String raw) {
        try {
            return new JSONArray(raw == null || raw.trim().isEmpty() ? "[]" : raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    public static JSONArray appendHistory(JSONArray history, JSONArray options) {/*BR_JSON_WRAP*/try{
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
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static JSONArray diversify(JSONArray source, JSONArray recentHistory,
                                      String narration, String declaredAction,
                                      JSONObject scene, int beatNumber) {/*BR_JSON_WRAP*/try{
        JSONArray out = new JSONArray();
        ArrayList<String> accepted = new ArrayList<String>();
        ArrayList<String> history = historyValues(recentHistory);
        ArrayList<String> immediateHistory = latestHistoryValues(recentHistory,MAX_OPTIONS);
        if(declaredAction!=null&&!declaredAction.trim().isEmpty())addUnique(history,declaredAction);
        int validSourceCount = 0;

        if (source != null) {
            for (int i = 0; i < source.length() && out.length() < MAX_OPTIONS; i++) {
                JSONObject original = source.optJSONObject(i);
                if (original == null) continue;
                JSONObject option = sanitize(original);
                String action = optionAction(option);
                if (action.isEmpty() || !isCompatibleWithScene(action, narration, scene) || !isGroundedInScene(action,narration,scene)) continue;
                validSourceCount++;
                if (isRepeated(action, history) || isRepeated(action, accepted) || repeatsIntent(action,accepted)) continue;
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
            if(action.isEmpty()||!isGroundedInScene(action,narration,scene)||isRepeated(action,history)||isRepeated(action,accepted)||repeatsIntent(action,accepted))continue;
            out.put(fallback);accepted.add(action);
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * A tela do Mestre deve manter até duas sugestões opcionais visíveis em toda
     * cena iniciada. Primeiro usa as opções do modelo/fallback contextual; se
     * faltarem alternativas, completa com ações universais que não presumem
     * objeto, NPC ou saída inexistente.
     */
    public static JSONArray ensureAlways(JSONArray source, JSONArray recentHistory,
                                         String narration, String declaredAction,
                                         JSONObject scene, int beatNumber) {/*BR_JSON_WRAP*/try{
        // RUN182: zero opções é um resultado válido. Nunca preencha a UI com ações
        // universais que não nasceram de entidade/fato realmente presente.
        return diversify(source,recentHistory,narration,declaredAction,scene,beatNumber);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static List<JSONObject> alwaysVisibleFallbacks(String narration,JSONObject scene,int beatNumber){
        ArrayList<JSONObject> out=new ArrayList<JSONObject>();
        try{
            String object=firstNarrativeObject(narration);String npc=firstPresentNpc(scene);
            if(!object.isEmpty())out.add(option("Examinar "+object,"Observar posição, estado e relações sem alterar a cena.","Examino "+object+" com atenção, sem tocá-lo."));
            else out.add(option("Observar o que está ao alcance","Rever detalhes visíveis antes de assumir uma conclusão.","Observo com atenção o ponto mais relevante que já está visível na cena, sem avançar."));
            if(!npc.isEmpty())out.add(option("Falar com "+npc,"Fazer uma pergunta direta; conversa simples não exige teste.","Pergunto a "+npc+" sobre o que acabou de acontecer."));
            else out.add(option("Reavaliar posição e saídas","Confirmar quem está onde e quais caminhos continuam visíveis.","Reavalio a posição do grupo, as distâncias relativas e as saídas visíveis antes de agir."));
            if((beatNumber&1)==1&&out.size()>1){JSONObject first=out.remove(0);out.add(first);}
        }catch(Exception ignored){/* fallback opcional; degrada para lista parcial */}
        return out;
    }

    private static String firstPresentNpc(JSONObject scene){
        if(scene==null)return "";JSONArray a=scene.optJSONArray("present_npcs");
        if(a==null)try{a=new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception ignored){a=new JSONArray();}
        for(int i=0;i<a.length();i++){Object v=a.opt(i);String name=v instanceof JSONObject?((JSONObject)v).optString("name",""):String.valueOf(v);name=clean(name,80);if(!name.isEmpty()&&!"voce".equalsIgnoreCase(name)&&!"você".equalsIgnoreCase(name))return name;}return "";
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

    private static JSONObject sanitize(JSONObject original) {/*BR_JSON_WRAP*/try{
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
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

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
        for(int i=0;i<additions.size();i++)if(!similar(history.get(offset+i),additions.get(i)))return false;
        return true;
    }

    private static boolean isRepeated(String candidate, List<String> values) {
        for (String previous : values) if (similar(candidate, previous)) return true;
        return false;
    }
    private static boolean repeatsIntent(String candidate,List<String> accepted){String intent=intentSignature(candidate);if(intent.isEmpty())return false;for(String previous:accepted)if(intent.equals(intentSignature(previous)))return true;return false;}

    static boolean similar(String a, String b) {
        String x = normalize(a), y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        if (Math.min(x.length(), y.length()) >= 22 && (x.contains(y) || y.contains(x))) return true;
        String ix=intentSignature(x),iy=intentSignature(y);
        if(!ix.isEmpty()&&ix.equals(iy)){
            Set<String> ex=entityTokens(x),ey=entityTokens(y);int shared=0;for(String token:ex)if(ey.contains(token))shared++;
            if(shared>0||ex.isEmpty()||ey.isEmpty())return true;
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
        String npc = firstEntity(scene, "present_npcs_json");
        String clue = firstEntity(scene, "clues_json");
        String threat = firstEntity(scene, "threats_json");
        String location = scene == null ? "" : clean(scene.optString("location", ""), 70);
        String objective = scene == null ? "" : clean(scene.optString("objective", ""), 100);
        String danger = scene == null ? "" : clean(scene.optString("danger", ""), 100);
        String narrative=normalize(narration);
        String document=firstNarrativeObject(narration);
        boolean socialDemand=containsAny(narrative,"dívida","divida","cobrança","cobranca","contrato","cessão","cessao","direitos","prazo","pagamento","guilda","credor","notas promissórias","notas promissorias","exigência","exigencia","ordem","tributo","multa");

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
        } else if (!npc.isEmpty()) {
            pool.add(option("PEDIR A " + upperShort(npc) + " QUE EXPLIQUE A EXIGÊNCIA",
                    "Obter objetivo, motivo e consequência concreta antes de decidir.",
                    "Pedir a " + npc + " que explique exatamente o que deseja, por que precisa disso agora e o que acontecerá se o grupo recusar."));
            pool.add(option("CONFRONTAR A FALA DE " + upperShort(npc) + " COM A CENA",
                    "Comparar a declaração com objetos, testemunhas e mudanças visíveis.",
                    "Confrontar a afirmação de " + npc + " com o elemento material mais relevante da cena antes de responder."));
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
            pool.add(option("COMPARAR A PISTA COM O AMBIENTE",
                    "Verificar se a pista pertence ao local ou foi trazida de outro lugar.",
                    "Comparar " + clue + " com o terreno, os objetos e as rotas disponíveis antes de decidir para onde seguir."));
        }
        if (!threat.isEmpty()) {
            pool.add(option("MARCAR A POSIÇÃO DA AMEAÇA",
                    "Identificar alcance, cobertura e rota de recuo sem iniciar o confronto.",
                    "Localizar " + threat + " com precisão e escolher uma posição que preserve cobertura e saída segura."));
            pool.add(option("CRIAR UMA DISTRAÇÃO CONTROLADA",
                    "Testar a reação da ameaça sem expor todo o grupo.",
                    "Produzir um estímulo longe do grupo para observar como " + threat + " reage antes de se comprometer."));
        }

        if(containsAny(narrative,"fumaca","fumaça"))pool.add(option("RASTREAR A ORIGEM DA FUMAÇA","Seguir direção, cheiro e intensidade mantendo cobertura.","Rastrear a origem da fumaça por uma rota protegida e interromper o avanço ao primeiro sinal de presença hostil."));
        if(containsAny(narrative,"patrulha","sentinela","vigia"))pool.add(option("MAPEAR A ROTA DA PATRULHA","Observar ritmo, intervalo e pontos cegos antes de atravessar.","Mapear o percurso da patrulha e escolher um intervalo seguro para avançar ou recuar."));
        if(containsAny(narrative,"pegada","rastro","trilha"))pool.add(option("SEGUIR OS RASTROS RECENTES","Distinguir direção, quantidade e velocidade sem apagar as marcas.","Seguir os rastros mais recentes até o próximo ponto de decisão, mantendo um marco para retornar."));
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

        String declared=clean(declaredAction,100);
        if(!declared.isEmpty())pool.add(option("TESTAR A CONSEQUÊNCIA DA AÇÃO",
                "Verificar o que mudou de fato antes de repetir a mesma abordagem.",
                "Observar a consequência imediata de “"+declared+"” e agir somente sobre a mudança confirmada."));

        // Sem entidade, pista, ameaça, objetivo ou objeto concreto, é melhor
        // deixar o campo de ação livre do que inventar duas escolhas genéricas.
        return pool;
    }

    private static boolean isWeakOption(String label,String description,String action){
        String l=normalize(label),d=normalize(description),a=normalize(action);
        if(a.length()<14||l.length()<5)return true;
        String[] weak={"agir sobre a mudanca","ler a situacao","buscar um novo angulo","testar outra hipotese","fazer alguma coisa","continuar a aventura","observar o cenario","reconhecer o local","examinar a entrada","localizar o som","proteger uma rota de recuo","confirmar o objetivo imediato","verificar o detalhe mais recente","verificar a travessia"};
        for(String value:weak)if(l.equals(value)||a.equals(value)||l.startsWith(value+" "))return true;
        if(d.length()>30&&a.contains(d)&&d.split(" ").length>8)return true;
        if(l.matches("^(agir|fazer|continuar|prosseguir|investigar|observar)$"))return true;
        return false;
    }

    private static String shortNoun(String value){String clean=clean(value,42);int comma=clean.indexOf(',');if(comma>6)clean=clean.substring(0,comma);return clean;}
    private static boolean containsAny(String value,String... terms){String n=" "+normalize(value)+" ";for(String term:terms){String t=normalize(term);if(!t.isEmpty()&&n.contains(" "+t+" "))return true;}return false;}

    static boolean isGroundedInScene(String action,String narration,JSONObject scene){
        String a=normalize(action);String context=normalize((narration==null?"":narration)+" "+(scene==null?"":scene.toString()));
        if(a.isEmpty()||context.isEmpty())return false;
        Set<String> candidate=entityTokens(a),world=tokens(context);int shared=0;for(String token:candidate)if(world.contains(token))shared++;
        if(shared>0)return true;
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
        String n=normalize(narration);String[][] objects={{"notas promissorias","as notas promissórias"},{"documento original","o documento original"},{"documento","o documento"},{"contrato","o contrato"},{"papeis","os papéis"},{"lacre vermelho","o lacre vermelho"},{"lacre","o lacre"},{"anel de selo","o anel de selo"},{"mapa","o mapa"},{"carta","a carta"},{"chave","a chave"},{"caixas de ferro","as caixas de ferro"},{"caixa de ferro","a caixa de ferro"},{"corpo","o corpo"},{"objeto","o objeto"}};for(String[] object:objects)if(containsAny(n,object[0]))return object[1];return "";
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

    private static JSONObject option(String label, String description, String action) {/*BR_JSON_WRAP*/try{
        try {
            return new JSONObject()
                    .put("label", clean(label, 80))
                    .put("description", clean(description, 180))
                    .put("action", clean(action, 300));
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

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
