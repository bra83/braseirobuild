package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One narrative-initiative choice is owned by StoryMomentumAction.
 * This class owns exactly three different player decisions:
 * OBSERVAR, AGIR and DESPISTAR.
 *
 * Sources are the CURRENT visible narration and filtered model choices.
 * Persisted NPC lists are deliberately not evidence of physical presence.
 * Never imply a successful check or fabricate a person/prop to fill a slot.
 */
public final class GroundedSceneActionSlots {
    private GroundedSceneActionSlots() {}
    public static final String OBSERVAR="OBSERVAR", AGIR="AGIR", DESPISTAR="DESPISTAR";
    private static final String[] TYPES={OBSERVAR,AGIR,DESPISTAR};

    public static JSONArray contextual(JSONArray proposed,String narration,JSONObject ignoredPersistedScene,int beat) {
        JSONArray out=new JSONArray();
        if(narration==null||narration.trim().isEmpty())return out;
        String visible=narration;
        String actor=visibleActor(visible);
        String focus=visibleFocus(visible);
        String topic=visibleTopic(visible,focus);
        boolean silverquillRiddle=has(visible,"silverquill")&&has(visible,"charada")&&has(visible,"selos");

        for(String type:TYPES){
            JSONObject selected=null;
            // A purpose-built choice beats an LLM's generic "look around" sentence.
            if(silverquillRiddle) selected=silverquillChoice(type,actor,visible);
            if(selected==null)selected=bestModelChoice(proposed,type,visible,actor,focus,topic);
            if(selected==null)selected=groundedChoice(type,actor,focus,topic,visible);
            if(selected!=null && SceneOptionVisibleEvidence.supported(selected,visible)
                    && !duplicateAction(out,selected.optString("action","")))out.put(selected);
        }
        return out;
    }

    /** The option uses an explicit type so the UI never has to infer it from a verb. */
    private static JSONObject choice(String type,String title,String action){
        JSONObject o=new JSONObject();
        try{
            o.put("type",type).put("action_type",type)
              .put("label",type+" • "+title)
              .put("action",action).put("description",action);
        }catch(Exception ignored){}
        return o;
    }

    private static JSONObject silverquillChoice(String type,String actor,String narration){
        String student=has(narration,"aluna")?"a aluna de Silverquill":"os estudantes de Silverquill";
        if(OBSERVAR.equals(type))return choice(type,"EXAMINAR A CHARADA NAS FITAS",
            "Examino as palavras da charada nas fitas de tinta voadora, procurando compreender o que está escrito sem presumir que descobri a resposta.");
        if(AGIR.equals(type))return choice(type,"PERGUNTAR SOBRE OS SELOS",
            "Pergunto "+preposition("a",student)+" quais selos estão em discussão e o que significa juntá-los, aguardando a resposta antes de decidir.");
        if(DESPISTAR.equals(type))return choice(type,"OCULTAR O INTERESSE PELOS SELOS",
            "Disfarço meu interesse pela discussão dos selos e observo discretamente as reações dos estudantes de Silverquill, sem presumir que o disfarce funcionou.");
        return null;
    }

    private static JSONObject bestModelChoice(JSONArray proposed,String type,String narration,String actor,String focus,String topic){
        if(proposed==null)return null;
        JSONObject best=null;int score=-1;
        for(int i=0;i<proposed.length();i++){
            JSONObject o=proposed.optJSONObject(i);if(o==null)continue;
            String action=o.optString("action","").trim(), label=o.optString("label","");
            if(action.isEmpty()||StoryMomentumAction.isRequest(action)||!SceneOptionVisibleEvidence.supported(o,narration))continue;
            if(!type.equals(actionType(o)))continue;
            if(weak(action+" "+label)||!SceneOptionDiversifier.isRelevantToCurrentBeat(action,narration)
                   ||!SceneOptionDiversifier.isGroundedInScene(action,narration,new JSONObject()))continue;
            if(!SceneOptionVisibleEvidence.guardNounPresent(narration)&&has(action+" "+label,"guarda"))continue;
            int evidence=0;
            if(!actor.isEmpty()&&has(action+" "+label,stripArticle(actor)))evidence+=2;
            if(!focus.isEmpty()&&has(action+" "+label,stripArticle(focus)))evidence+=2;
            if(!topic.isEmpty()&&has(action+" "+label,stripArticle(topic)))evidence+=2;
            if(evidence==0)continue;
            if(evidence>score){
                best=choice(type,cleanTitle(label,type),action);
                JSONObject test=o.optJSONObject("test");
                if(test!=null)try{best.put("test",new JSONObject(test.toString()));}catch(Exception ignored){}
                score=evidence;
            }
        }
        return best;
    }

    private static boolean weak(String text){
        String n=fold(text);
        return has(n,"o que esta acontecendo aqui")||has(n,"a situacao atual")
           ||has(n,"sobre o que acontece")||has(n,"por alguns instantes")
           ||has(n,"ganhar outro angulo")||has(n,"mudo minha posicao")
           ||has(n,"a respeito da cena")||has(n,"o ambiente ao redor")
           ||has(n,"sem presumir a resposta")||has(n,"detalhes concretos");
    }

    private static String actionType(JSONObject o){
        String explicit=o.optString("action_type",o.optString("type","")).toUpperCase(Locale.ROOT);
        for(String type:TYPES)if(type.equals(explicit))return type;
        String label=fold(o.optString("label",""));
        if(label.startsWith("observar "))return OBSERVAR;
        if(label.startsWith("agir "))return AGIR;
        if(label.startsWith("despistar "))return DESPISTAR;
        String a=fold(o.optString("action","")+" "+o.optString("label",""));
        if(hasAny(a,"disfarco","despisto","despistar","finjo","oculto","mantenho em segredo","nao revelo"))return DESPISTAR;
        if(hasAny(a,"observo","observar","examino","analiso","leio","inspeciono","escuto","reparo"))return OBSERVAR;
        if(hasAny(a,"pergunto","falo","abordo","respondo","converso","tento","mostro","entrego","aproximo"))return AGIR;
        return "";
    }

    private static JSONObject groundedChoice(String type,String actor,String focus,String topic,String narration){
        if(actor.isEmpty()&&focus.isEmpty())return null; // No invented filler for an empty scene.
        String subject=!topic.isEmpty()?topic:focus;
        if(OBSERVAR.equals(type)){
            if(!focus.isEmpty())return choice(type,"OBSERVAR "+upper(focus),
               "Examino "+focus+" que está na cena, procurando somente detalhes perceptíveis antes de tirar conclusões.");
            return choice(type,"OBSERVAR "+upper(actor),
               "Observo "+actor+" durante a interação narrada, procurando perceber gestos e falas antes de responder.");
        }
        if(AGIR.equals(type)){
            if(!actor.isEmpty()){
                if(!subject.isEmpty())return choice(type,"FALAR COM "+upper(actor)+" SOBRE "+upper(subject),
                    "Pergunto "+preposition("a",actor)+" sobre "+subject+" mencionado nesta cena e espero a resposta antes de tomar outra decisão.");
                return choice(type,"FALAR COM "+upper(actor),
                    "Dirijo-me "+preposition("a",actor)+" e respondo à interação em curso com uma pergunta esclarecedora, sem presumir a resposta.");
            }
            return choice(type,"INTERAGIR COM "+upper(focus),
                "Aproximo-me "+preposition("de",focus)+" e tento examiná-lo diretamente, sem assumir que tenho acesso ou que obtive sucesso.");
        }
        if(DESPISTAR.equals(type)){
            if(!actor.isEmpty()){
                String target=!subject.isEmpty()?subject:("a conversa com "+actor);
                return choice(type,"DISFARÇAR INTERESSE "+upper(preposition("por",target)),
                    "Procuro não demonstrar meu interesse "+preposition("por",target)+" enquanto presto atenção às reações "+preposition("de",actor)+", sem presumir que consegui enganar alguém.");
            }
            return choice(type,"EXAMINAR DISCRETAMENTE "+upper(focus),
                "Tento examinar "+focus+" com discrição, evitando chamar atenção sem presumir que ninguém percebeu a tentativa.");
        }
        return null;
    }

    private static String visibleActor(String narration){
        // Identify the speaker, if a speaker label exists in the actual narration.
        Matcher m=Pattern.compile("(?iu)\\b(aluna(?: de [\\p{L}-]+)?|aluno(?: de [\\p{L}-]+)?|estudante(?: de [\\p{L}-]+)?|veterana|veterano|professora?|bibliotec[aá]ria?|auxiliar|assistente|funcion[aá]ria?|taverneira?|mercador|guarda)\\s*:").matcher(narration);
        String speaker="";
        while(m.find())speaker=m.group(1);
        if(!speaker.isEmpty() && (!has(speaker,"guarda")||SceneOptionVisibleEvidence.guardNounPresent(narration)))
            return article(speaker);
        // A character who actually speaks in this beat is reliable evidence,
        // unlike a stale NPC in the scene database. Preserve named speakers.
        Matcher named=Pattern.compile("(?m)\\b([A-ZÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{L}'-]{2,})\\s*:\\s*[—-]")
            .matcher(narration);
        String namedSpeaker="";
        while(named.find())namedSpeaker=named.group(1);
        if(!namedSpeaker.isEmpty())return namedSpeaker;
        String[][] roles={
          {"aluna","a aluna"},{"estudantes","os estudantes"},{"estudante","o estudante"},
          {"veterana","a veterana"},{"pesquisador","o pesquisador"},{"monitor","o monitor"},
          {"capitao","o capitão"},{"mercadora","a mercadora"},{"mercador","o mercador"},
          {"mensageiro","o mensageiro"},{"barqueiro","o barqueiro"},
          {"mestre de cais","o mestre de cais"},{"estalajadeiro","o estalajadeiro"},
          {"viajantes","os viajantes"},
          {"aluno","o aluno"},{"calouros","os calouros"},{"auxiliar","a auxiliar"},
          {"bibliotecaria","a bibliotecária"},{"professora","a professora"},
          {"professor","o professor"},{"funcionaria","a funcionária"},
          {"assistente","a assistente"},{"mercador","o mercador"},
          {"taverneiro","o taverneiro"},{"guardas","os guardas"},{"guarda","o guarda"}
        };
        String beat=narration.substring(Math.max(0,narration.length()-1000));
        String normalized=" "+fold(beat)+" ";
        int best=-1;String selected="";
        for(String[] role:roles){
            if(role[0].equals("guarda")&&!SceneOptionVisibleEvidence.guardNounPresent(beat))continue;
            int ix=normalized.lastIndexOf(" "+role[0]+" ");
            if(ix>best){best=ix;selected=role[1];}
        }
        return selected;
    }

    private static String visibleFocus(String narration){
        if(has(narration,"grupo aguarda"))return "o grupo que aguarda";
        String[][] objects={
          {"quadro de desafios","o quadro de desafios"},{"quadro","o quadro"},
          {"charada","a charada"},{"selos","os selos"},{"inscricoes","as inscrições"},
          {"alavanca","a alavanca"},{"marcadores","os marcadores"},
          {"cofre","o cofre"},{"obelisco","o obelisco"},{"fogueira","a fogueira"},
          {"caderno","o caderno"},{"frascos","os frascos"},
          {"diario","o diário"},{"caixote","o caixote"},{"armario","o armário"},
          {"bifurcacao","a bifurcação"},{"carroca","a carroça"},
          {"fitas de tinta","as fitas de tinta"},{"inscricao","a inscrição"},
          {"pergaminho","o pergaminho"},{"papel","o papel"},{"roteiro","o roteiro"},
          {"livro","o livro"},{"carta","a carta"},{"bilhete","o bilhete"},
          {"mapa","o mapa"},{"painel","o painel"},{"folhetos","os folhetos"},
          {"prancheta","a prancheta"},{"porta","a porta"},{"ponte","a ponte"},
          {"arbusto","o arbusto"},{"manuscrito","o manuscrito"},{"escada","a escada"},
          {"chuva","a chuva"},{"vento","o vento"},{"grupo","o grupo"},{"pausa","a pausa"},
          {"silencio","o silêncio"},{"salão","o salão"},{"sala","a sala"}
        };
        String n=" "+fold(narration)+" ";
        // The most distinctive object wins, not whichever noun appeared most recently.
        for(String[] entry:objects)if(n.contains(" "+entry[0]+" "))return entry[1];
        return "";
    }

    private static String visibleTopic(String narration,String focus){
        if(has(narration,"selos"))return "os selos";
        if(has(narration,"charada"))return "a charada";
        if(has(narration,"inscricoes"))return "as inscrições";
        if(has(narration,"orientacao"))return "a orientação";
        return focus;
    }

    private static String article(String phrase){
        String p=phrase.trim(),n=fold(p);
        if(n.startsWith("aluna")||n.startsWith("veterana")||n.startsWith("professora")||n.startsWith("bibliotecaria")
           ||n.startsWith("auxiliar")||n.startsWith("assistente")||n.startsWith("funcionaria"))
            return "a "+p.toLowerCase(Locale.ROOT);
        return "o "+p.toLowerCase(Locale.ROOT);
    }
    private static String preposition(String kind,String phrase){
        String p=phrase==null?"":phrase.trim();
        if(p.isEmpty())return "";
        String n=fold(p);
        String prefix="";
        if(n.startsWith("os "))prefix="os ";
        else if(n.startsWith("as "))prefix="as ";
        else if(n.startsWith("o "))prefix="o ";
        else if(n.startsWith("a "))prefix="a ";
        if(prefix.isEmpty())return kind+" "+p;
        String tail=p.substring(prefix.length());
        if("a".equals(kind)){
            if("a ".equals(prefix))return "à "+tail;
            if("os ".equals(prefix))return "aos "+tail;
            if("as ".equals(prefix))return "às "+tail;
            return "ao "+tail;
        }
        if("de".equals(kind)){
            if("a ".equals(prefix))return "da "+tail;
            if("os ".equals(prefix))return "dos "+tail;
            if("as ".equals(prefix))return "das "+tail;
            return "do "+tail;
        }
        if("por".equals(kind)){
            if("a ".equals(prefix))return "pela "+tail;
            if("os ".equals(prefix))return "pelos "+tail;
            if("as ".equals(prefix))return "pelas "+tail;
            return "pelo "+tail;
        }
        return kind+" "+p;
    }
    private static String stripArticle(String phrase){return fold(phrase).replaceFirst("^(o|a|os|as|um|uma)\\s+","");}
    private static String cleanTitle(String label,String type){
        String v=label==null?"":label.trim().replaceFirst("(?iu)^(OBSERVAR|AGIR|DESPISTAR)\\s*[•:—-]\\s*","");
        if(v.length()>70)v=v.substring(0,70).trim();
        return v.isEmpty()?"DECISÃO SOBRE A CENA":v;
    }
    private static String upper(String s){return s.toUpperCase(new Locale("pt","BR"));}
    private static boolean hasAny(String text,String...parts){for(String part:parts)if(has(text,part))return true;return false;}
    private static boolean has(String text,String term){return (" "+fold(text)+" ").contains(" "+fold(term)+" ");}
    private static String fold(String value){
        if(value==null)return "";
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
    private static boolean duplicateAction(JSONArray out,String action){
        String n=fold(action);
        for(int i=0;i<out.length();i++){
            JSONObject p=out.optJSONObject(i);
            if(p!=null&&n.equals(fold(p.optString("action",""))))return true;
        }
        return false;
    }
}
