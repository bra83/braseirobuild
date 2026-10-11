package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recuperação local obrigatória de regras e procedimentos.
 *
 * O índice permanece totalmente offline. Este motor expande a ação em várias
 * consultas bilíngues, pesquisa todos os manuais habilitados do mundo ativo,
 * inclui páginas vizinhas, reranqueia as passagens e produz um pacote de prova
 * que o Mestre e o validador mecânico compartilham.
 */
public final class RuleRetrievalEngine {
    public static final class Result {
        public final JSONObject bundle;
        public final List<JSONObject> hits;
        Result(JSONObject value,List<JSONObject> rows){bundle=value==null?new JSONObject():value;hits=rows==null?new ArrayList<JSONObject>():rows;}
    }

    private static final class IntentSpec {
        final String id;
        final String pt;
        final String en;
        final String[] triggers;
        IntentSpec(String key,String portuguese,String english,String... words){id=key;pt=portuguese;en=english;triggers=words;}
    }

    private static final IntentSpec[] INTENTS={
        new IntentSpec("ataque_dano","ataque dano acerto crítico classe de armadura","attack damage critical hit armor class","atac","golpe","dano","ferir","dispar","espada","arco","critical","attack","damage"),
        new IntentSpec("agarrar_empurrar","agarrar empurrar imobilizar disputa tamanho movimento","grapple shove restrain contest size movement","agarr","empurr","derrub","imobil","grapple","shove"),
        new IntentSpec("movimento_reacao_cobertura","movimento alcance reação ataque de oportunidade cobertura","movement reach reaction opportunity attack cover","mover","alcance","oportunidade","cobertura","reacao","reaction","cover","reach"),
        new IntentSpec("magia_concentracao","magia conjuração concentração componentes duração alvo área","spell casting concentration components duration target area","magia","feitico","concentr","conjur","spell","casting"),
        new IntentSpec("teste_salvaguarda","teste de habilidade perícia dificuldade jogada de proteção salvaguarda","ability check skill difficulty class saving throw","teste","pericia","habilidade","salvaguarda","protecao","saving throw","ability check"),
        new IntentSpec("condicoes","condições agarrado caído cego invisível envenenado exaustão","conditions grappled prone blinded invisible poisoned exhaustion","condicao","caido","cego","invis","enven","exaust","condition","prone"),
        new IntentSpec("cura_morte_descanso","cura pontos de vida morte estabilizar descanso curto descanso longo recuperação","healing hit points death stabilize short rest long rest recovery","cura","morte","morr","estabil","descanso","healing","death","rest"),
        new IntentSpec("furtividade_visao_surpresa","furtividade esconder percepção visão luz obscurecido surpresa","stealth hide perception vision light obscured surprise","furt","escond","percepc","visao","luz","surpresa","stealth","vision"),
        new IntentSpec("exploracao_armadilhas","exploração portas secretas armadilhas perigos busca investigação","exploration secret doors traps hazards search investigation","explor","porta","armad","perigo","investig","trap","hazard"),
        new IntentSpec("viagem_terreno","viagem deslocamento ritmo marcha terreno navegação perder-se forragear provisões","travel movement pace march terrain navigation lost foraging provisions","viaj","marcha","terreno","naveg","perder","forrage","provis","travel","terrain"),
        new IntentSpec("clima_ambiente","clima chuva vento frio calor visibilidade ambiente","weather rain wind cold heat visibility environment","clima","chuva","vento","frio","calor","weather","rain"),
        new IntentSpec("encontro_reacao_moral","encontros aleatórios reação de monstros moral retirada perseguição","random encounters monster reaction morale retreat pursuit","encontro","reacao","moral","retirada","persegu","encounter","morale"),
        new IntentSpec("social_negociacao","persuasão enganação intimidação intuição atitude negociação","persuasion deception intimidation insight attitude negotiation","persuad","engan","intimid","intuic","negoci","persuasion","deception"),
        new IntentSpec("economia_tempo_livre","tempo livre trabalho treinamento criação pesquisa comércio custos serviços","downtime work training crafting research trade costs services","trabalh","trein","criacao","artesan","pesquis","comerc","custo","servic","downtime","crafting"),
        new IntentSpec("faccoes_npcs","facções reputação seguidores aliados inimigos influência agenda objetivos","factions reputation followers allies enemies influence agenda goals","facc","reput","aliad","inimig","influenc","agenda","faction"),
        new IntentSpec("monstros_npcs","monstro criatura bloco de estatísticas habilidades especiais táticas","monster creature stat block special abilities tactics","monstro","criatura","inimigo","besta","dragao","monster","creature"),
        new IntentSpec("itens_tesouro","item arma armadura equipamento tesouro preço peso carga","item weapon armor equipment treasure price weight encumbrance","item","arma","armadura","tesouro","preco","peso","carga","treasure","weapon"),
        new IntentSpec("mundo_vivo","eventos facções npc agenda calendário domínio construção recrutamento administração","events factions npc schedule calendar domain building recruitment administration","evento","calend","dominio","construc","recrut","administr","world event","domain")
    };

    private static final Pattern DICE=Pattern.compile("(?iu)\\b(?:\\d{1,3})?d\\d{1,4}(?:\\s*[+\\-]\\s*\\d{1,3})?\\b");
    private static final Pattern NUMBER_WITH_UNIT=Pattern.compile("(?iu)\\b\\d+(?:[.,]\\d+)?\\s*(?:ft|feet|p[eé]s?|m|metros?|km|milhas?|horas?|dias?|rodadas?|turnos?|po|pp|pc|gp|sp|cp|%)\\b");
    private final AppDb db;

    public RuleRetrievalEngine(AppDb database){db=database;}

    public Result retrieve(String mode,String action,String lastNarration,JSONObject scene,boolean mystara,boolean economy){/*BR_JSON_WRAP*/try{
        JSONObject bundle=new JSONObject();ArrayList<JSONObject> finalHits=new ArrayList<JSONObject>();
        try{
            // A abertura também precisa consultar regras. O caminho antigo a
            // classificava como ficção pura e devolvia zero fontes, apesar de a
            // própria arquitetura pedir negociação, investigação, viagem ou
            // perigos. Agora a busca usa somente o foco explícito da abertura,
            // evitando que frases meta ou proibições contaminem os termos.
            boolean openingRequest=isNarrativeOpeningRequest(action);
            String retrievalAction=openingRequest?openingRuleFocus(action):action;
            String stateFocus=stateFocus(retrievalAction,mystara);
            String focus=clean(retrievalAction)+" "+tail(clean(lastNarration),openingRequest?260:900)+" "+sceneFocus(scene)+" "+stateFocus;
            JSONObject indexHealth=openingRequest?db.manualIndexHealthFast(mystara):db.manualIndexHealth(mystara);int ready=indexHealth.optInt("ready",0);int enabled=indexHealth.optInt("enabled",0);boolean completeIndex=indexHealth.optBoolean("complete",false);
            JSONArray intents=detectIntents(focus);
            JSONArray queries=openingRequest?buildOpeningFastQueries(retrievalAction,intents,mystara):buildQueries(mode,retrievalAction,lastNarration,scene,mystara,intents);
            if(!openingRequest){appendQuery(queries,trimWords(clean(retrievalAction)+" "+stateFocus,36),18);appendQuery(queries,trimWords(stateFocus+" "+(mystara?"Rules Cyclopedia BECMI procedimento tabela":"D&D 5e 2014 2024 regra procedimento"),36),18);}
            boolean narrative="Narrador".equals(mode);
            // RUN111: ser Narrador não torna toda mensagem uma candidata mecânica.
            // O sendAi já classificou a necessidade; aqui preservamos apenas sinais reais.
            boolean mechanicalCandidate=!openingRequest&&(!narrative||intents.length()>0||looksMechanical(retrievalAction));
            bundle.put("required",enabled>0).put("world",mystara?"mystara":"dnd").put("enabled_manuals",enabled).put("ready_manuals",ready).put("index_complete",completeIndex).put("index_missing_manuals",Math.max(0,enabled-ready))
                    .put("mechanical_candidate",mechanicalCandidate).put("opening_narrative",openingRequest).put("opening_rules_focus",openingRequest?retrievalAction:"")
                    .put("intents",intents).put("queries",queries).put("state_focus",tail(stateFocus,1200))
                    .put("retrieval_version",openingRequest?"opening-fast-index-v6":"hybrid-structured-v5-two-pass").put("retrieval_modes",new JSONArray().put("lexical").put("radical").put("synonym").put("semantic_concept"))
                    .put("searched_all_enabled_manuals",completeIndex).put("index_health",indexHealth);
            if(ready<=0){
                bundle.put("status","index_unavailable").put("evidence",new JSONArray()).put("structured_rules",new JSONArray())
                        .put("sufficient_for_mechanics",false).put("instruction","Nenhum manual local deste mundo está pronto; narração pode continuar, mas mutações mecânicas e estatísticas novas devem ser bloqueadas.");
                return new Result(bundle,finalHits);
            }

            long retrievalStarted=System.nanoTime();Map<String,JSONObject> merged=new LinkedHashMap<String,JSONObject>();int executedQueries=0;
            for(int qi=0;qi<queries.length();qi++){
                String query=queries.optString(qi).trim();if(query.length()==0)continue;
                List<JSONObject> rows=openingRequest?db.searchManualLocalPagesFast(query,mystara,8):db.searchManualLocalPages(query,mystara,economy?10:16);executedQueries++;
                for(JSONObject row:rows){
                    JSONObject copy=copy(row);String key=copy.optLong("manual_id")+"|"+copy.optInt("page_no");
                    int adjusted=copy.optInt("_score",0)+Math.max(0,42-qi*4)+intentCoverage(copy.optString("content"),intents)*7;
                    copy.put("_score",adjusted).put("_matched_query",query).put("_query_rank",qi);
                    JSONObject old=merged.get(key);if(old==null||adjusted>old.optInt("_score",0))merged.put(key,copy);
                }
                if(openingRequest&&(merged.size()>=8||elapsedMs(retrievalStarted)>=7000L))break;
            }
            bundle.put("executed_query_count",executedQueries).put("fast_opening_search",openingRequest).put("search_elapsed_ms",elapsedMs(retrievalStarted));

            ArrayList<JSONObject> ranked=new ArrayList<JSONObject>(merged.values());
            sortHits(ranked);
            int seedCount=Math.min(economy?3:6,ranked.size());
            for(int i=0;i<seedCount;i++){
                JSONObject seed=ranked.get(i);List<JSONObject> neighbors=db.getManualLocalPageWindow(seed.optLong("manual_id"),seed.optInt("page_no"),1);
                for(JSONObject n:neighbors){
                    String key=n.optLong("manual_id")+"|"+n.optInt("page_no");if(merged.containsKey(key))continue;
                    JSONObject copy=copy(n);copy.put("_score",Math.max(1,seed.optInt("_score")-18-Math.abs(copy.optInt("page_no")-seed.optInt("page_no"))*4))
                            .put("_matched_query",seed.optString("_matched_query")).put("_neighbor_of",seed.optInt("page_no"));merged.put(key,copy);
                }
            }
            ranked=new ArrayList<JSONObject>(merged.values());sortHits(ranked);

            // Segunda busca: primeiro identifica a mecânica, depois procura o
            // procedimento exato. A primeira página recuperada nunca recebe
            // autoridade automática para uma intenção diferente.
            JSONArray secondPassQueries=new JSONArray();LinkedHashSet<String> mechanicTerms=new LinkedHashSet<String>();
            for(int i=0;i<Math.min(5,ranked.size());i++){
                JSONObject row=ranked.get(i);String excerpt=bestExcerpt(clean(row.optString("content")),queries,row.optString("_matched_query"),2200);
                JSONObject preliminarySource=new JSONObject().put("rule_id",ruleId(row)).put("book",row.optString("manual_title",row.optString("title"))).put("page",row.optInt("page_no")).put("excerpt",excerpt).put("rule_profile",row.optString("rule_profile"));
                JSONObject preliminary=structuredFact(preliminarySource,intents);JSONArray keys=preliminary.optJSONArray("mechanic_keys");
                if(keys!=null)for(int k=0;k<keys.length();k++){String term=keys.optString(k).replace(':',' ').replace('_',' ').trim();if(term.length()>=3)mechanicTerms.add(term);}
            }
            if(!openingRequest&&!mechanicTerms.isEmpty()){
                StringBuilder exact=new StringBuilder();for(String term:mechanicTerms){if(exact.length()>0)exact.append(' ');exact.append(term);if(exact.length()>180)break;}
                for(int i=0;i<intents.length();i++){IntentSpec spec=findIntent(intents.optString(i));if(spec!=null)exact.append(' ').append(spec.pt);}
                String exactQuery=trimWords(exact.toString(),38);secondPassQueries.put(exactQuery);
                for(JSONObject row:db.searchManualLocalPages(exactQuery,mystara,economy?12:20)){
                    JSONObject copy=copy(row);String key=copy.optLong("manual_id")+"|"+copy.optInt("page_no");int adjusted=copy.optInt("_score",0)+75+intentCoverage(copy.optString("content"),intents)*10;
                    copy.put("_score",adjusted).put("_matched_query",exactQuery).put("_second_pass",true);
                    JSONObject old=merged.get(key);if(old==null||adjusted>old.optInt("_score",0))merged.put(key,copy);
                }
                ranked=new ArrayList<JSONObject>(merged.values());sortHits(ranked);
            }
            bundle.put("second_pass_queries",secondPassQueries).put("second_pass_performed",secondPassQueries.length()>0);

            int maxHits=openingRequest?6:(economy?8:16),maxChars=openingRequest?7600:(economy?9200:19000),usedChars=0;
            JSONArray evidence=new JSONArray(),facts=new JSONArray();Set<String> books=new LinkedHashSet<String>();int executableRules=0,bestScore=0;
            for(JSONObject row:ranked){
                if(finalHits.size()>=maxHits)break;
                if(!"ready".equals(row.optString("rag_status"))||!AppDb.LOCAL_MANUAL_STORE.equals(row.optString("rag_store_name"))||row.optString("rag_split_mode").toLowerCase(Locale.ROOT).startsWith("fallback"))continue;
                String content=clean(row.optString("content"));if(content.length()<24)continue;
                String excerpt=bestExcerpt(content,queries,row.optString("_matched_query"),economy?900:1450);
                if(excerpt.length()<24)continue;if(usedChars+excerpt.length()>maxChars&&finalHits.size()>=4)break;
                String book=row.optString("manual_title",row.optString("title","Manual"));
                JSONObject source=new JSONObject().put("rule_id",ruleId(row)).put("manual_id",row.optLong("manual_id"))
                        .put("book",book).put("title",book).put("page",row.optInt("page_no")).put("pdf_page",row.optInt("page_no"))
                        .put("excerpt",excerpt).put("score",row.optInt("_score")).put("coverage",row.optDouble("_coverage",0d)).put("matched_query",row.optString("_matched_query"))
                        .put("category",row.optString("category")).put("rule_profile",row.optString("rule_profile"))
                        .put("chapter",row.optString("chapter_title")).put("section",row.optString("section_title")).put("content_kind",row.optString("content_kind"))
                        .put("page_hash",row.optString("page_hash")).put("structure",jsonObject(row.optString("structure_json")))
                        .put("authority",row.optString("_authority")).put("second_pass",row.optBoolean("_second_pass",false)).put("page_reference_reliable",true).put("source","Índice local do aparelho");
                JSONObject fact=structuredFact(source,intents);int procedureRelevance=procedureRelevance(fact,intents,excerpt,row);int minimumRelevance=openingRequest?28:45;if(procedureRelevance<minimumRelevance)continue;
                source.put("procedure_relevance",procedureRelevance).put("minimum_relevance",minimumRelevance).put("confirmed",true);
                JSONObject hit=copy(row);hit.put("content",excerpt).put("procedure_relevance",procedureRelevance);finalHits.add(hit);usedChars+=excerpt.length();books.add(book);bestScore=Math.max(bestScore,row.optInt("_score"));
                if(fact.optBoolean("executable",false))executableRules++;
                evidence.put(source);facts.put(fact);
            }
            JSONObject rulesByIntent=rulesByIntent(intents,evidence,facts);
            boolean relevant=evidence.length()>0&&bestScore>=35;
            boolean sufficient=!openingRequest&&relevant&&completeIndex&&executableRules>0;
            String status;
            if(openingRequest){
                if(relevant)status=completeIndex?"opening_rules_ready":"opening_rules_partial";
                else status=completeIndex?"opening_no_relevant_evidence":"opening_index_incomplete";
            }else status=!completeIndex?"index_incomplete":(!relevant?"no_relevant_evidence":(executableRules<=0?"evidence_not_executable":"ready"));
            String instruction;
            if(openingRequest&&relevant)instruction="Use as passagens recuperadas para enquadrar corretamente a situação inicial, os NPCs e as escolhas possíveis. Não imponha teste nem aplique consequência mecânica antes de o jogador declarar uma ação."+(completeIndex?"":" O índice ainda está parcial, portanto nenhuma nova consequência mecânica pode ser aplicada nesta abertura.");
            else if(!completeIndex)instruction="O índice local está incompleto; a ficção pode continuar, mas novas consequências mecânicas ficam bloqueadas até todos os manuais habilitados estarem integralmente prontos.";
            else if(!relevant)instruction="Nenhuma passagem pertinente foi localizada; não invente regra, estatística ou efeito mecânico.";
            else if(executableRules<=0)instruction="Passagens relacionadas foram encontradas, mas não formam procedimento mecânico verificável. Continue apenas a ficção.";
            else instruction="Use structured_rules/rules_by_intent para o procedimento. Todo número aplicado exige calculation reproduzível por rolagem local ou valor fixo literal da regra.";
            bundle.put("status",status).put("evidence",evidence).put("structured_rules",facts).put("rules_by_intent",rulesByIntent)
                    .put("books_consulted",new JSONArray(books)).put("evidence_count",evidence.length()).put("executable_rule_count",executableRules).put("best_score",bestScore)
                    .put("sufficient_for_mechanics",sufficient).put("context_characters",usedChars).put("instruction",instruction).put("retrieval_elapsed_ms",elapsedMs(retrievalStarted));
        }catch(Exception failure){
            AppLog.e("RuleRetrievalEngine","Falha na recuperação obrigatória; mecânicas ficarão bloqueadas.",failure);
            try{bundle.put("status","error").put("error",safe(failure)).put("evidence",new JSONArray()).put("structured_rules",new JSONArray()).put("sufficient_for_mechanics",false).put("required",true);}catch(Exception ignored){AppLog.ignored("RuleRetrievalEngine","Falha ao registrar erro de recuperação.",ignored);}
            finalHits.clear();
        }
        return new Result(bundle,finalHits);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private String stateFocus(String action,boolean mystara){
        StringBuilder out=new StringBuilder();String normalized=norm(action);int included=0;
        try{
            List<JSONObject> heroes=db.listActiveCharacters();
            for(JSONObject hero:heroes){
                String name=hero.optString("name");boolean named=name.length()>0&&normalized.contains(norm(name));
                if(!named&&included>=2)continue;
                out.append(' ').append(name).append(' ').append(hero.optString("class_name")).append(" nível ").append(hero.optInt("level",1));
                out.append(' ').append(tail(hero.optString("equipment_json"),420)).append(' ').append(tail(hero.optString("spells_json"),520)).append(' ').append(tail(hero.optString("sheet_json"),520));
                included++;if(included>=4)break;
            }
            for(JSONObject combatant:db.listCombatants()){
                out.append(' ').append(combatant.optString("name")).append(' ').append(combatant.optString("conditions")).append(' ').append(combatant.optString("attacks"));
                if(out.length()>2200)break;
            }
            JSONObject travel=db.getTravelRulesState();out.append(' ').append(travel.optString("pace")).append(' ').append(travel.optString("terrain")).append(' ').append(travel.optString("weather"));
            JSONObject campaign=db.getCampaign();out.append(' ').append(campaign.optString("rules_profile",mystara?"Rules Cyclopedia":"5e"));
        }catch(Exception failure){AppLog.w("RuleRetrievalEngine","Estado complementar indisponível para a busca; a consulta principal continuará.",failure);}
        return trimWords(out.toString(),90);
    }

    private static void appendQuery(JSONArray queries,String query,int max){/*BR_JSON_WRAP*/try{if(queries==null||query==null||query.trim().length()<3||queries.length()>=max)return;String normalized=norm(query);for(int i=0;i<queries.length();i++)if(norm(queries.optString(i)).equals(normalized))return;queries.put(query.trim());}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public Result retrieveForWorld(JSONObject worldTick,JSONArray factionActions,JSONArray npcActions,JSONArray activities,boolean mystara){
        StringBuilder query=new StringBuilder("mundo vivo facções npc agenda eventos calendário downtime domain faction morale reaction travel economy");
        if(worldTick!=null)query.append(' ').append(worldTick.optString("travel_rules")).append(' ').append(worldTick.optString("company_resources_consumed"));
        appendActionText(query,factionActions,"summary");appendActionText(query,npcActions,"summary");appendActionText(query,activities,"title");
        JSONObject scene=db.getSceneState();return retrieve("Mundo Vivo",query.toString(),"",scene,mystara,true);
    }


    /** Recuperação dedicada a uma única decisão do Mundo Vivo. Evita anexar a
     * mesma página genérica a ações diferentes e permite ao simulador bloquear
     * apenas a consequência que não possui regra pertinente. */
    public Result retrieveForWorldAction(String kind,JSONObject action,boolean mystara){
        StringBuilder query=new StringBuilder();query.append(kind==null?"mundo vivo":kind).append(' ');
        if(action!=null){
            for(String key:new String[]{"rule_query","summary","title","action","plan","step_title","location","from","to","narrative"})query.append(action.optString(key)).append(' ');
            JSONObject consequence=action.optJSONObject("consequence_json");if(consequence!=null)query.append(consequence.toString());
        }
        return retrieve("Mundo Vivo",trimWords(query.toString(),90),"",db.getSceneState(),mystara,true);
    }

    private static long elapsedMs(long startedNanos){return Math.max(0L,(System.nanoTime()-startedNanos)/1000000L);}

    /**
     * A abertura não precisa executar as 16 consultas da arbitragem de uma ação.
     * Ela usa o foco explícito criado pelo planejador em uma única consulta
     * bilíngue consolidada. Assim o índice pronto é consultado uma vez, em vez de
     * relido repetidamente.
     */
    static JSONArray buildOpeningFastQueries(String focus,JSONArray intents,boolean mystara){
        JSONArray out=new JSONArray();StringBuilder consolidated=new StringBuilder();
        if(intents!=null)for(int i=0;i<intents.length()&&i<2;i++){IntentSpec spec=findIntent(intents.optString(i));if(spec==null)continue;if(consolidated.length()>0)consolidated.append(' ');consolidated.append(spec.pt).append(' ').append(spec.en);}
        if(consolidated.length()>0)consolidated.append(' ');
        consolidated.append(mystara?"Rules Cyclopedia reaction roll encounter exploration movement morale":"D&D 5e ability check exploration social interaction encounter initiative travel");
        consolidated.append(' ').append(clean(focus));appendQuery(out,trimWords(consolidated.toString(),48),1);return out;
    }

    public static JSONArray buildQueriesForTest(String mode,String action,String lastNarration,JSONObject scene,boolean mystara){return buildQueries(mode,action,lastNarration,scene,mystara,detectIntents(clean(action)+" "+clean(lastNarration)+" "+sceneFocus(scene)));}

    private static JSONArray buildQueries(String mode,String action,String lastNarration,JSONObject scene,boolean mystara,JSONArray intents){/*BR_JSON_WRAP*/try{
        LinkedHashSet<String> out=new LinkedHashSet<String>();String base=clean(action);String context=sceneFocus(scene);String history=tail(clean(lastNarration),520);
        if(base.length()>0)out.add(base);if(base.length()>0&&context.length()>0)out.add(base+" "+context);if(base.length()>0&&history.length()>0)out.add(base+" "+history);
        for(int i=0;i<intents.length();i++){
            String id=intents.optString(i);IntentSpec spec=findIntent(id);if(spec==null)continue;
            out.add(spec.pt+" "+base);out.add(spec.en+" "+base);
            if(context.length()>0)out.add(spec.pt+" "+context);
        }
        String profile=mystara?"Rules Cyclopedia BECMI armor class descending saving throws morale reaction":"D&D 5e 2014 2024 rules procedure";
        if(base.length()>0)out.add(base+" "+profile);else out.add(profile+" "+context);
        if("Mundo Vivo".equals(mode))out.add("downtime domain faction npc schedule events travel costs reaction morale world simulation");
        JSONArray array=new JSONArray();for(String q:out){String c=trimWords(q,32);if(c.length()>=3)array.put(c);if(array.length()>=16)break;}return array;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray detectIntents(String value){/*BR_JSON_WRAP*/try{JSONArray out=new JSONArray();String n=norm(value);for(IntentSpec spec:INTENTS){for(String trigger:spec.triggers)if(n.contains(norm(trigger))){out.put(spec.id);break;}}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static IntentSpec findIntent(String id){for(IntentSpec spec:INTENTS)if(spec.id.equals(id))return spec;return null;}
    private static boolean looksMechanical(String value){String n=norm(value);String[] tokens={"tento","rolar","teste","ataco","dano","cura","magia","movimento","descanso","viajo","comprar","vender","construir","treinar","cacar","pescar","faccao","npc","evento","combate"};for(String token:tokens)if(n.contains(norm(token)))return true;return false;}

    static boolean isNarrativeOpeningRequest(String value){String n=norm(value);return n.contains("inicio automatico")&&(n.contains("abertura de historia")||n.contains("primeira cena"));}

    static String openingRuleFocus(String value){
        String source=value==null?"":value;
        Matcher marker=Pattern.compile("(?iu)FOCO DE REGRAS PARA A ABERTURA\\s*:\\s*(.{8,420}?)(?:\\.\\s+(?:A CENA|GARANTA|NÃO|NAO|INTRODUZA)|\\n|$)").matcher(source);
        if(marker.find())return clean(marker.group(1));
        return "interação social investigação percepção exploração viagem encontro atitude de NPC e testes de habilidade";
    }

    private static JSONObject structuredFact(JSONObject source,JSONArray intents)throws Exception{return RuleProcedureCompiler.compile(source,intents);}

    private static JSONObject rulesByIntent(JSONArray intents,JSONArray evidence,JSONArray facts)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();if(intents==null)return out;
        for(int i=0;i<intents.length();i++){
            String intent=intents.optString(i);JSONArray ranked=new JSONArray();
            java.util.ArrayList<JSONObject> rows=new java.util.ArrayList<JSONObject>();
            for(int j=0;j<facts.length();j++){
                JSONObject fact=facts.optJSONObject(j);JSONObject source=evidence.optJSONObject(j);if(fact==null||source==null)continue;
                int score=source.optInt("score",0);JSONArray declared=fact.optJSONArray("intents");if(declared!=null)for(int k=0;k<declared.length();k++)if(intent.equals(declared.optString(k)))score+=40;
                JSONObject row=new JSONObject().put("rule_id",source.optString("rule_id")).put("book",source.optString("book")).put("page",source.optInt("page")).put("excerpt",source.optString("excerpt")).put("confidence",fact.optInt("confidence",0)).put("executable",fact.optBoolean("executable",false)).put("score",score);rows.add(row);
            }
            java.util.Collections.sort(rows,new java.util.Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){int ex=Boolean.compare(b.optBoolean("executable"),a.optBoolean("executable"));return ex!=0?ex:Integer.compare(b.optInt("score"),a.optInt("score"));}});
            for(JSONObject row:rows){if(ranked.length()>=4)break;ranked.put(row);}out.put(intent,ranked);
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static int intentCoverage(String content,JSONArray intents){String n=norm(content);int count=0;for(int i=0;i<intents.length();i++){IntentSpec spec=findIntent(intents.optString(i));if(spec==null)continue;for(String t:spec.triggers)if(n.contains(norm(t))){count++;break;}}return count;}
    private static int procedureRelevance(JSONObject fact,JSONArray intents,String excerpt,JSONObject row){
        int matchedIntents=intentCoverage(excerpt,intents);int score=matchedIntents*28;
        JSONArray keys=fact.optJSONArray("mechanic_keys");if(keys!=null)score+=Math.min(24,keys.length()*6);
        score+=(int)Math.round(Math.max(0d,Math.min(1d,row.optDouble("_coverage",0d)))*24d);
        if(row.optBoolean("_second_pass",false))score+=18;
        if(fact.optBoolean("executable",false))score+=8;
        return Math.min(100,score);
    }
    private static JSONObject jsonObject(String raw){try{return new JSONObject(raw==null||raw.trim().length()==0?"{}":raw);}catch(Exception ignored){return new JSONObject();}}
    private static void sortHits(List<JSONObject> rows){Collections.sort(rows,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){int byScore=Integer.compare(b.optInt("_score"),a.optInt("_score"));if(byScore!=0)return byScore;int byManual=Long.compare(a.optLong("manual_id"),b.optLong("manual_id"));return byManual!=0?byManual:Integer.compare(a.optInt("page_no"),b.optInt("page_no"));}});}
    private static JSONObject copy(JSONObject source){try{return new JSONObject(source==null?"{}":source.toString());}catch(Exception e){return new JSONObject();}}
    private static String ruleId(JSONObject row){return "manual:"+row.optLong("manual_id")+":page:"+row.optInt("page_no");}
    private static String bestExcerpt(String content,JSONArray queries,String matched,int max){
        String source=content==null?"":content;
        // A normalização usada para localizar o termo preserva o comprimento.
        // Assim, o índice encontrado continua válido no texto original mesmo
        // quando há acentos ou pontuação. A versão antiga colapsava espaços e
        // podia recortar uma passagem distante da regra realmente encontrada.
        String normalized=normForOffsets(source);int best=0;ArrayList<String> terms=new ArrayList<String>();
        if(matched!=null)for(String t:norm(matched).split("\\s+"))if(t.length()>=4)terms.add(t);
        if(queries!=null)for(int i=0;i<queries.length()&&terms.size()<30;i++)for(String t:norm(queries.optString(i)).split("\\s+"))if(t.length()>=5&&!terms.contains(t))terms.add(t);
        for(String term:terms){int at=normalized.indexOf(term);if(at>=0){best=at;break;}}
        int start=Math.max(0,best-max/3),end=Math.min(source.length(),start+max);
        if(start>0){int s=source.indexOf(' ',start);if(s>=0)start=s+1;}
        if(end<source.length()){int e=source.lastIndexOf(' ',end);if(e>start)end=e;}
        String out=source.substring(start,end).trim();return (start>0?"… ":"")+out+(end<source.length()?" …":"");
    }
    private static String normForOffsets(String value){
        if(value==null)return "";
        String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");
        // Não colapsar caracteres: cada símbolo inválido vira exatamente um espaço.
        return n.replaceAll("[^a-z0-9]"," ");
    }
    private static String sceneFocus(JSONObject scene){if(scene==null)return "";return clean(scene.optString("location")+" "+scene.optString("weather")+" "+scene.optString("danger")+" "+scene.optString("objective")+" "+scene.optString("threats_json")+" "+scene.optString("clues_json"));}
    private static void appendActionText(StringBuilder b,JSONArray rows,String key){if(rows==null)return;for(int i=0;i<rows.length()&&i<12;i++){JSONObject row=rows.optJSONObject(i);if(row!=null)b.append(' ').append(row.optString(key));}}
    private static String trimWords(String value,int max){String[] words=clean(value).split("\\s+");StringBuilder b=new StringBuilder();for(String word:words){if(word.length()==0)continue;if(b.length()>0)b.append(' ');b.append(word);if(--max<=0)break;}return b.toString();}
    private static String clean(String value){return value==null?"":value.replaceAll("\\s+"," ").trim();}
    private static String norm(String value){String n=Normalizer.normalize(clean(value).toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static String tail(String value,int max){String s=clean(value);return s.length()<=max?s:s.substring(s.length()-max);}
    private static String safe(Throwable failure){String m=failure==null?"":failure.getMessage();if(m==null||m.trim().length()==0)m=failure==null?"erro desconhecido":failure.getClass().getSimpleName();return clean(m);}
}
