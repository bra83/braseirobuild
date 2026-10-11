package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Braseiro 6.1.1 — painel auxiliar do Mestre.
 *
 * Produz informação de interface fora da ficção. Nada retornado por esta classe
 * altera cena, relógio, NPC, combate, mapa ou estado canônico.
 */
public final class MasterSupportPanelEngine {
    private MasterSupportPanelEngine(){}

    public static JSONObject build(JSONObject envelope, JSONObject retrieval, JSONArray citations,
                                   JSONObject scene, List<JSONObject> heroes, JSONArray options,
                                   JSONObject pendingTest, String playerInput, boolean mystara){
        return build(envelope,retrieval,citations,scene,heroes,options,pendingTest,playerInput,mystara,null);
    }

    public static JSONObject build(JSONObject envelope, JSONObject retrieval, JSONArray citations,
                                   JSONObject scene, List<JSONObject> heroes, JSONArray options,
                                   JSONObject pendingTest, String playerInput, boolean mystara,String inputChannel){
        JSONObject out=new JSONObject();
        try{
            JSONObject safeEnv=envelope==null?new JSONObject():envelope;
            JSONObject safeRetrieval=retrieval==null?new JSONObject():retrieval;
            JSONObject safeScene=scene==null?new JSONObject():scene;
            JSONArray safeCitations=citations==null?new JSONArray():copyArray(citations);
            int evidence=Math.max(safeRetrieval.optInt("evidence_count",0),safeCitations.length());
            String status=safeRetrieval.optString("status","not_run");
            boolean searched=safeRetrieval.optBoolean("required",true)||!"not_run".equals(status);
            boolean mechanicsAllowed=safeRetrieval.optBoolean("sufficient_for_mechanics",false)&&evidence>0;
            boolean nonAdvancing=safeEnv.optBoolean("_non_advancing",BraseiroNarrativeDirector.isNonAdvancing(playerInput,inputChannel));

            out.put("outside_fiction",true);
            out.put("rules_searched",searched);
            out.put("rules_status",status);
            out.put("evidence_count",evidence);
            out.put("mechanics_allowed",mechanicsAllowed);
            out.put("input_channel",inputChannel==null?"":inputChannel);
            out.put("intent",BraseiroNarrativeDirector.intentLabel(BraseiroNarrativeDirector.classify(playerInput,inputChannel)));
            out.put("what_happened",whatHappened(safeEnv,playerInput,nonAdvancing,inputChannel));
            out.put("rules_explanation",rulesExplanation(safeEnv,safeRetrieval,pendingTest,mystara,evidence,mechanicsAllowed));
            out.put("scene_summary",sceneSummary(safeScene));
            out.put("skill_suggestions",skillSuggestions(playerInput,safeEnv.optString("narration"),safeScene,heroes,mystara));
            out.put("sources",compactSources(safeCitations,safeRetrieval));
            out.put("options_visible",options!=null&&options.length()>0);
            out.put("question_hint","Pergunte quem está na cena, onde cada pessoa está, distâncias, saídas, o que seu personagem sabe ou qual regra/perícia pode se aplicar. A pergunta não executa uma ação.");
            out.put("updated_at",System.currentTimeMillis());
        }catch(Exception error){
            try{out.put("outside_fiction",true).put("rules_explanation","O painel auxiliar não conseguiu montar todos os detalhes, mas a narrativa permaneceu isolada.").put("error",safe(error));}catch(Exception ignored){AppLog.ignored("MasterSupportPanelEngine","Falha ao registrar degradação do painel.",ignored);}
        }
        return out;
    }

    public static JSONObject withAnswer(JSONObject support,String question,String answer,JSONObject retrieval,JSONArray citations){
        JSONObject out=copyObject(support);
        try{
            out.put("last_question",clean(question,900));
            out.put("last_answer",clean(answer,5000));
            out.put("answer_intent",BraseiroNarrativeDirector.intentLabel(BraseiroNarrativeDirector.classify(question,"GM_HELP")));
            if(retrieval!=null){out.put("rules_status",retrieval.optString("status",out.optString("rules_status")));out.put("evidence_count",Math.max(retrieval.optInt("evidence_count",0),citations==null?0:citations.length()));out.put("rules_searched",true);}
            if(citations!=null)out.put("sources",compactSources(citations,retrieval));
            out.put("outside_fiction",true).put("updated_at",System.currentTimeMillis());
        }catch(Exception ignored){AppLog.ignored("MasterSupportPanelEngine","Falha ao anexar resposta auxiliar.",ignored);}
        return out;
    }

    private static String whatHappened(JSONObject env,String input,boolean nonAdvancing,String inputChannel){
        if(nonAdvancing)return "A mensagem foi tratada como "+BraseiroNarrativeDirector.intentLabel(BraseiroNarrativeDirector.classify(input,inputChannel))+". A cena ficou congelada: tempo, NPCs, mapa, combate, Mundo Vivo e consequências não avançaram.";
        int changes=countArray(env,"state_updates")+countArray(env,"npc_updates")+countArray(env,"clock_updates")+countArray(env,"world_events")+countArray(env,"map_updates")+countArray(env,"tactical_updates");
        JSONObject combat=env.optJSONObject("combat");boolean combatChange=combat!=null&&!"none".equalsIgnoreCase(combat.optString("action","none"));
        if(changes==0&&!combatChange)return "O Mestre continuou a ficção, mas não propôs alteração persistente de ficha, relógio, NPC, mapa ou combate.";
        return "O Mestre propôs "+(changes+(combatChange?1:0))+" alteração(ões) estruturadas. Elas continuam sujeitas à tela de revisão antes de entrarem no cânone.";
    }

    private static String rulesExplanation(JSONObject env,JSONObject retrieval,JSONObject pending,boolean mystara,int evidence,boolean allowed){
        JSONObject test=pending!=null&&pending.length()>0?pending:env.optJSONObject("pending_test");
        if(test!=null&&test.optBoolean("required",true)){
            String label=mystara?mystaraTestLabel(test):dndTestLabel(test);
            JSONObject support=test.optJSONObject("rule_support");
            String source=support==null?"":sourceLine(support.optString("book"),support.optInt("page",0));
            return "Há incerteza e consequência suficientes para um teste: "+label+"."+(source.length()>0?" Base recuperada: "+source+".":allowed?" A regra foi sustentada pelas passagens recuperadas.":" Sem prova específica, o teste deve permanecer bloqueado.");
        }
        if(evidence>0)return "A biblioteca local foi consultada e devolveu "+evidence+" passagem(ns) pertinente(s). Nenhum teste foi exigido nesta batida; conversar, observar o óbvio e fazer perguntas simples continuam livres até surgir incerteza com consequência real.";
        String status=retrieval.optString("status","");
        if("index_unavailable".equals(status)||"index_incomplete".equals(status))return "A consulta de regras foi tentada, mas o índice local não está completo. Por segurança, o Mestre pode continuar apenas a ficção e não pode inventar teste, CD, dano, custo ou efeito mecânico.";
        if("retrieval_timeout".equals(status))return "A consulta local excedeu o limite de espera. A cena pode continuar como ficção, mas qualquer consequência mecânica fica bloqueada até haver prova recuperada.";
        return "Nenhuma regra específica foi necessária ou nenhuma passagem pertinente foi encontrada. O sistema não deve criar mecânica apenas para preencher a cena.";
    }

    private static JSONArray skillSuggestions(String input,String narration,JSONObject scene,List<JSONObject> heroes,boolean mystara){
        JSONArray out=new JSONArray();
        try{
            String n=plain((input==null?"":input)+" "+(narration==null?"":narration)+" "+scene.toString());
            ArrayList<String[]> candidates=new ArrayList<String[]>();
            if(has(n,"pista","documento","mecanismo","marca","sangue","pegada","janela","porta","examinar","investigar")){candidates.add(pair("Investigação","examinar relações, mecanismo ou evidência"));candidates.add(pair("Percepção","notar sinais visíveis, sons ou mudanças"));}
            if(has(n,"ment","hesit","nerv","conversa","negoci","guarda","testemunha","npc","fala")){candidates.add(pair("Intuição","ler comportamento sem transformar suspeita em certeza"));candidates.add(pair("Persuasão","pedir cooperação quando houver resistência real"));}
            if(has(n,"escon","silencio","silêncio","vigia","furt","sombra","seguir sem ser"))candidates.add(pair("Furtividade","mover-se ou ocultar-se sem chamar atenção"));
            if(has(n,"escalar","saltar","empurrar","segurar","forçar","arrombar","nadar"))candidates.add(pair("Atletismo","vencer esforço físico ou oposição corporal"));
            if(has(n,"equilibr","cair","telhado","corda","acrob"))candidates.add(pair("Acrobacia","equilíbrio, queda ou movimento preciso"));
            if(has(n,"floresta","rastro","trilha","caçar","forrage","viagem","ermo")){candidates.add(pair("Sobrevivência","seguir rastros e lidar com ambiente selvagem"));candidates.add(pair("Natureza","reconhecer terreno, fauna, flora ou fenômeno natural"));}
            if(has(n,"arcano","runa","magia","feitiço","portal"))candidates.add(pair("Arcanismo","interpretar fenômeno ou escrita arcana"));
            if(has(n,"templo","deus","culto","morto-vivo","ritual"))candidates.add(pair("Religião","reconhecer tradição, símbolo ou rito"));
            if(has(n,"ferido","veneno","doente","cadáver"))candidates.add(pair("Medicina","avaliar condição física ou causa aparente"));
            if(candidates.isEmpty()){candidates.add(pair("Percepção","observar melhor o que já está ao alcance"));candidates.add(pair("Intuição","avaliar comportamento quando houver interação social"));}

            ArrayList<String> used=new ArrayList<String>();
            for(String[] c:candidates){if(out.length()>=4||used.contains(c[0]))continue;used.add(c[0]);JSONObject suggestion=new JSONObject().put("skill",mystara?mystaraEquivalent(c[0]):c[0]).put("why",c[1]).put("optional",true).put("automatic_roll",false);String best=bestHeroFor(c[0],heroes);if(best.length()>0)suggestion.put("best_character",best);out.put(suggestion);}
        }catch(Exception ignored){AppLog.ignored("MasterSupportPanelEngine","Falha ao sugerir perícias.",ignored);}
        return out;
    }

    private static String bestHeroFor(String skill,List<JSONObject> heroes){
        if(heroes==null||heroes.isEmpty())return "";String best="";int bestBonus=-999;
        for(JSONObject h:heroes){int bonus=skillBonus(h,skill);if(bonus>bestBonus){bestBonus=bonus;best=h.optString("name","Herói")+" "+(bonus>=0?"+":"")+bonus;}}
        return best;
    }

    private static int skillBonus(JSONObject hero,String skill){
        try{
            JSONObject sheet=parseObject(hero.optString("sheet_json","{}"));String skills=sheet.optString("skills",hero.optString("proficiencies",""));String target=plain(skill);for(String part:skills.split("[•\\n]")){String p=plain(part);if(!p.startsWith(target))continue;java.util.regex.Matcher m=java.util.regex.Pattern.compile("([+-]?\\d+)").matcher(part);if(m.find())return Integer.parseInt(m.group(1));}
            int score=10;String s=plain(skill);if(has(s,"atletismo"))score=hero.optInt("str",10);else if(has(s,"acrobacia","furtividade","prestidigitacao"))score=hero.optInt("dex",10);else if(has(s,"arcanismo","historia","investigacao","natureza","religiao"))score=hero.optInt("int_score",10);else if(has(s,"adestrar","intuicao","medicina","percepcao","sobrevivencia"))score=hero.optInt("wis",10);else score=hero.optInt("cha",10);return (score-10)/2;
        }catch(Exception ignored){return 0;}
    }

    private static String sceneSummary(JSONObject scene){
        String location=scene.optString("location","local atual");String time=scene.optString("time_of_day","");String weather=scene.optString("weather","");StringBuilder b=new StringBuilder(location);if(time.length()>0)b.append(" • ").append(time);if(weather.length()>0)b.append(" • ").append(weather);JSONArray npcs=sceneArray(scene,"present_npcs","present_npcs_json");if(npcs.length()>0){b.append(" • presentes: ");for(int i=0;i<npcs.length()&&i<8;i++){if(i>0)b.append(", ");Object v=npcs.opt(i);if(v instanceof JSONObject)b.append(((JSONObject)v).optString("name","NPC"));else b.append(String.valueOf(v));}}return b.toString();
    }

    private static JSONArray compactSources(JSONArray citations,JSONObject retrieval){
        JSONArray out=new JSONArray();try{if(citations!=null)for(int i=0;i<citations.length()&&out.length()<5;i++){JSONObject c=citations.optJSONObject(i);if(c==null)continue;out.put(new JSONObject().put("book",c.optString("manual_title",c.optString("title",c.optString("book","Manual")))).put("page",c.optInt("page_no",c.optInt("page",0))).put("excerpt",clean(c.optString("excerpt",c.optString("content","")),320)));}if(out.length()==0&&retrieval!=null){JSONArray ev=retrieval.optJSONArray("evidence");if(ev!=null)for(int i=0;i<ev.length()&&out.length()<5;i++){JSONObject c=ev.optJSONObject(i);if(c!=null)out.put(new JSONObject().put("book",c.optString("book",c.optString("manual_title","Manual"))).put("page",c.optInt("page",c.optInt("page_no",0))).put("excerpt",clean(c.optString("excerpt",c.optString("content","")),320)));}}}catch(Exception ignored){AppLog.ignored("MasterSupportPanelEngine","Falha ao compactar fontes.",ignored);}return out;
    }

    private static String dndTestLabel(JSONObject t){String ability=t.optString("ability","Atributo"),skill=t.optString("skill","");int dc=t.optInt("dc",10);return ability+(skill.length()>0?" ("+skill+")":"")+" contra CD "+dc;}
    private static String mystaraTestLabel(JSONObject t){String type=t.optString("type","ability");if("save".equals(type))return "Jogada de Proteção — "+t.optString("save_type","Bastões, Cajados ou Feitiços");if("thief_skill".equals(type))return "Habilidade de Ladrão — "+t.optString("thief_skill","Mover-se em Silêncio");return "Teste de "+t.optString("ability","Destreza")+" com 1d20 igual ou abaixo do atributo";}
    private static String mystaraEquivalent(String dnd){String n=plain(dnd);if(has(n,"investigacao"))return "Inteligência ou perícia geral pertinente";if(has(n,"percepcao","intuicao","medicina","sobrevivencia"))return "Sabedoria ou perícia geral pertinente";if(has(n,"furtividade"))return "Mover-se em Silêncio / Destreza";if(has(n,"atletismo"))return "Força";if(has(n,"acrobacia"))return "Destreza";if(has(n,"persuasao"))return "Carisma ou perícia geral pertinente";return dnd;}
    private static String sourceLine(String book,int page){if(book==null||book.trim().isEmpty())return "";return book+(page>0?", página "+page:"");}
    private static int countArray(JSONObject o,String k){JSONArray a=o.optJSONArray(k);return a==null?0:a.length();}
    private static String[] pair(String a,String b){return new String[]{a,b};}
    private static boolean has(String n,String... terms){for(String t:terms)if(n.contains(plain(t)))return true;return false;}
    private static JSONArray sceneArray(JSONObject scene,String objectKey,String stringKey){JSONArray a=scene.optJSONArray(objectKey);if(a!=null)return a;try{return new JSONArray(scene.optString(stringKey,"[]"));}catch(Exception e){return new JSONArray();}}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null||raw.trim().isEmpty()?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static JSONObject copyObject(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
    private static JSONArray copyArray(JSONArray a){try{return a==null?new JSONArray():new JSONArray(a.toString());}catch(Exception e){return new JSONArray();}}
    private static String clean(String s,int max){String x=s==null?"":s.replaceAll("\\s+"," ").trim();return x.length()>max?x.substring(0,max):x;}
    private static String plain(String s){String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(new Locale("pt","BR"));return n.replaceAll("[^a-z0-9 ]+"," ").replaceAll("\\s+"," ").trim();}
    private static String safe(Throwable t){return t==null?"erro":(t.getMessage()==null?t.getClass().getSimpleName():t.getMessage());}
}
