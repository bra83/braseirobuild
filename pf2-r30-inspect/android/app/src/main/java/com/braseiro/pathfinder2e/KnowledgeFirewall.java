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
 * Separa o que existe na verdade do Mestre do que pode aparecer ao jogador.
 * Nenhuma chamada extra é usada: o filtro é local, antes e depois do Gemini.
 */
public final class KnowledgeFirewall {
    private final AppDb db;
    public KnowledgeFirewall(AppDb database){db=database;}

    public JSONObject promptView(String action){
        JSONObject out=new JSONObject();
        try{
            String focus=normalize((action==null?"":action)+" "+db.getStoryState().optString("last_action")+" "+db.getSceneState().optString("clues_json","[]"));
            Set<String> actors=actorNames(action);
            JSONArray known=new JSONArray(),revealable=new JSONArray();int hiddenCount=0;
            for(JSONObject fact:db.listKnowledgeFacts(160)){
                String visibility=normalize(fact.optString("visibility","grupo"));
                boolean hidden=visibility.equals("mestre")||visibility.equals("secreto")||visibility.equals("director");
                boolean condition=revealConditionMet(fact,focus);
                if(hidden&&!condition){hiddenCount++;continue;}
                if(!knownByAllows(fact,actors)&&!condition)continue;
                JSONObject compact=compactFact(fact);
                if(hidden&&condition)revealable.put(compact);else known.put(compact);
            }
            out.put("known_facts",limit(known,18));
            out.put("facts_revealable_now",limit(revealable,6));
            out.put("hidden_fact_count",hiddenCount);
            out.put("rule","Narre apenas known_facts e facts_revealable_now. Fatos ocultos não foram enviados ao narrador.");
            out.put("knowledge_mode",db.getPartyState().optString("knowledge_mode","jogador_total"));
        }catch(Exception ignored){AppLog.ignored("KnowledgeFirewall","Falha tolerada; a execução continuou.",ignored);}
        return out;
    }

    public JSONObject sanitizeEnvelope(JSONObject envelope,String action){
        if(envelope==null)envelope=new JSONObject();
        JSONArray warnings=new JSONArray();
        try{
            String narration=envelope.optString("narration","");
            String focus=normalize((action==null?"":action)+" "+db.getSceneState().optString("clues_json","[]"));
            List<JSONObject> blocked=new ArrayList<JSONObject>();
            for(JSONObject fact:db.listKnowledgeFacts(240)){
                String visibility=normalize(fact.optString("visibility","grupo"));
                if(!(visibility.equals("mestre")||visibility.equals("secreto")||visibility.equals("director")))continue;
                if(revealConditionMet(fact,focus))continue;
                blocked.add(fact);
            }
            String safe=redactLeakedSentences(narration,blocked,warnings);
            if(!safe.equals(narration))envelope.put("narration",safe);

            JSONArray npcUpdates=envelope.optJSONArray("npc_updates");
            if(npcUpdates!=null)for(int i=0;i<npcUpdates.length();i++){
                JSONObject npc=npcUpdates.optJSONObject(i);if(npc==null)continue;
                if(npc.has("secrets")){npc.remove("secrets");warnings.put("Uma tentativa de alterar segredo de NPC foi ignorada; segredos só mudam pela memória do Mestre.");}
                npc.remove("director_only_secrets");
            }
            JSONArray facts=envelope.optJSONArray("knowledge_updates");
            if(facts!=null){JSONArray clean=new JSONArray();for(int i=0;i<facts.length();i++){
                JSONObject f=facts.optJSONObject(i);if(f==null)continue;String content=f.optString("content").trim();if(content.length()<3)continue;
                String visibility=normalize(f.optString("visibility","grupo"));if(!visibility.matches("grupo|publico|personagem|subgrupo|mestre|secreto"))f.put("visibility","grupo");
                f.put("truth_status",safeTruth(f.optString("truth_status","verdade")));clean.put(f);
            }envelope.put("knowledge_updates",clean);}
            JSONObject report=new JSONObject();report.put("valid",true);report.put("warnings",warnings);report.put("blocked_hidden_facts",blocked.size());report.put("narration_redacted",warnings.length()>0);envelope.put("_firewall_report",report);
        }catch(Exception failure){
            try{
                // Falha fechada: nenhuma mutação sobre conhecimento/NPC segue adiante e
                // a narração é neutralizada para impedir vazamento de segredo.
                envelope.put("narration","A cena não pôde ser validada com segurança. Nenhuma consequência foi aplicada.");
                envelope.put("options",new JSONArray());
                envelope.put("knowledge_updates",new JSONArray());
                envelope.put("npc_updates",new JSONArray());
                envelope.put("npc_cognition_updates",new JSONArray());
                envelope.put("_firewall_report",new JSONObject().put("valid",false).put("warnings",warnings).put("error",failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage())));
            }catch(Exception ignored){AppLog.ignored("KnowledgeFirewall","Falha ao fechar o firewall.",ignored);}
            AppLog.e("KnowledgeFirewall","Firewall de conhecimento bloqueou a resposta.",failure);
        }
        return envelope;
    }

    public JSONObject diagnostic(){
        JSONObject out=new JSONObject();try{int hidden=0,visible=0;for(JSONObject f:db.listKnowledgeFacts(1000)){String v=normalize(f.optString("visibility"));if(v.equals("mestre")||v.equals("secreto")||v.equals("director"))hidden++;else visible++;}out.put("visible_facts",visible).put("hidden_facts",hidden).put("active",true);}catch(Exception ignored){AppLog.ignored("KnowledgeFirewall","Falha tolerada; a execução continuou.",ignored);}return out;
    }

    private String redactLeakedSentences(String narration,List<JSONObject> blocked,JSONArray warnings){
        if(narration==null||narration.trim().length()==0||blocked.isEmpty())return narration==null?"":narration;
        String[] sentences=narration.split("(?<=[.!?…])\\s+");StringBuilder out=new StringBuilder();
        for(String sentence:sentences){boolean leak=false;String ns=normalize(sentence);for(JSONObject fact:blocked){Set<String> terms=significantTerms(fact.optString("content"));if(terms.size()<3)continue;int overlap=0;for(String t:terms)if(ns.contains(" "+t+" ")||ns.startsWith(t+" ")||ns.endsWith(" "+t)||ns.equals(t))overlap++;double ratio=overlap/(double)Math.max(1,Math.min(8,terms.size()));if(overlap>=3&&ratio>=0.45){leak=true;warnings.put("Uma frase que coincidia com informação ainda oculta foi neutralizada localmente.");break;}}
            if(out.length()>0)out.append(' ');out.append(leak?"Há algo nessa situação que os aventureiros ainda não conseguem compreender por completo.":sentence.trim());
        }
        return out.toString().trim();
    }

    private boolean revealConditionMet(JSONObject fact,String focus){
        String condition=normalize(fact.optString("reveal_condition"));if(condition.length()==0)return false;
        if(condition.equals("sempre")||condition.equals("imediato")||condition.equals("revelado"))return true;
        int hits=0;for(String term:condition.split(" "))if(term.length()>=4&&focus.contains(term))hits++;
        return hits>=Math.min(2,Math.max(1,significantTerms(condition).size()));
    }
    private boolean knownByAllows(JSONObject fact,Set<String> actors){
        String visibility=normalize(fact.optString("visibility","grupo"));if(visibility.equals("grupo")||visibility.equals("publico"))return true;
        JSONArray known=parseArray(fact.optString("known_by_json","[]"));if(known.length()==0)return !visibility.equals("personagem")&&!visibility.equals("subgrupo");
        for(int i=0;i<known.length();i++){String who=normalize(String.valueOf(known.opt(i)));if(who.equals("grupo")||actors.contains(who))return true;}
        return false;
    }
    private Set<String> actorNames(String action){Set<String> out=new HashSet<String>();String n=normalize(action);for(JSONObject h:db.listActiveCharacters()){String name=normalize(h.optString("name"));if(name.length()>0&&n.contains(name))out.add(name);}return out;}
    private static JSONObject compactFact(JSONObject f){JSONObject x=new JSONObject();try{x.put("id",f.optLong("id"));x.put("subject",trim(f.optString("subject"),160));x.put("content",trim(f.optString("content"),520));x.put("truth_status",f.optString("truth_status","verdade"));x.put("visibility",f.optString("visibility","grupo"));x.put("known_by_json",f.optString("known_by_json","[]"));x.put("source",trim(f.optString("source"),160));}catch(Exception ignored){AppLog.ignored("KnowledgeFirewall","Falha tolerada; a execução continuou.",ignored);}return x;}
    private static Set<String> significantTerms(String value){Set<String> out=new HashSet<String>();for(String t:normalize(value).split(" "))if(t.length()>=5&&!isStop(t))out.add(t);return out;}
    private static boolean isStop(String t){return "sobre entre ainda aquele aquela desta deste para pelos pelas muito como quando onde porque sendo apenas grupo mestre personagem segredo verdade".contains(t);}
    private static String safeTruth(String raw){String n=normalize(raw);if(n.contains("rumor"))return "rumor";if(n.contains("mentir")||n.contains("falso"))return "mentira";if(n.contains("interpret"))return "interpretação";return "verdade";}
    private static JSONArray parseArray(String raw){try{return new JSONArray(raw==null?"[]":raw);}catch(Exception e){return new JSONArray();}}
    private static JSONArray limit(JSONArray source,int max){JSONArray out=new JSONArray();for(int i=0;i<source.length()&&i<max;i++)out.put(source.opt(i));return out;}
    private static String trim(String s,int max){String v=s==null?"":s.trim();return v.length()<=max?v:v.substring(0,max)+"…";}
    private static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return (" "+n.replaceAll("[^a-z0-9]+"," ").trim()+" ").replaceAll("\\s+"," ").trim();}
}
