package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * General scene affordances; no Strixhaven/Faerun-specific place names.
 * It proposes choices only from ACTORS and TWO DISTINCT objects/places that
 * the accepted narration has visibly placed at the character's current beat.
 * A rumor, old scene state or arbitrary topic cannot create a present actor.
 * This is a generator of attempts, never a mechanical authorization or result.
 */
public final class SceneOpenWorldActionPlanner {
    private SceneOpenWorldActionPlanner(){}

    private static final String[][] ACTORS={
        {"oficial","o oficial"},{"oficiais","os oficiais"},
        {"fiscal","o fiscal"},{"fiscais","os fiscais"},
        {"funcionarios","os funcionários"},{"funcionario","o funcionário"},
        {"funcionarias","as funcionárias"},{"funcionaria","a funcionária"},
        {"guarda","o guarda"},{"guardas","os guardas"},
        {"sentinela","a sentinela"},{"sentinelas","as sentinelas"},
        {"mercadora","a mercadora"},{"mercador","o mercador"},
        {"comerciantes","os comerciantes"},{"comerciante","o comerciante"},
        {"cocheiro","o cocheiro"},{"cocheiros","os cocheiros"},
        {"estalajadeiro","o estalajadeiro"},{"taverneiro","o taverneiro"},
        {"barqueiro","o barqueiro"},{"marinheiros","os marinheiros"},
        {"mensageiro","o mensageiro"},{"assistente","a assistente"},
        {"bibliotecaria","a bibliotecária"},{"bibliotecario","o bibliotecário"},
        {"professora","a professora"},{"professor","o professor"},
        {"estudantes","os estudantes"},{"estudante","o estudante"},
        {"calouros","os calouros"},{"veteranos","os veteranos"},
        {"aluna","a aluna"},{"aluno","o aluno"},
        {"ferreiro","o ferreiro"},{"pesquisador","o pesquisador"},
        {"capitao","o capitão"},{"camponeses","os camponeses"},
        {"viajantes","os viajantes"},{"viajante","o viajante"}
    };
    private static final String[][] FOCI={
        {"portoes","os portões"},{"portao","o portão"},
        {"estabulos","os estábulos"},{"estabulo","o estábulo"},
        {"montarias","as montarias"},{"montaria","a montaria"},
        {"carrocas","as carroças"},{"carroca","a carroça"},
        {"mercadorias","as mercadorias"},{"mercadoria","a mercadoria"},
        {"fila","a fila"},{"estrada","a estrada"},
        {"ponte","a ponte"},{"docas","as docas"},{"cais","o cais"},
        {"navios","os navios"},{"navio","o navio"},
        {"arbustos","os arbustos"},{"arbusto","o arbusto"},
        {"plantas","as plantas"},{"planta","a planta"},
        {"estacas","as estacas"},{"estaca","a estaca"},
        {"raizes","as raízes"},{"canteiros","os canteiros"},{"canteiro","o canteiro"},
        {"trilha","a trilha"},{"bifurcacao","a bifurcação"},
        {"saguao","o saguão"},
        {"caminho","o caminho"},{"entrada","a entrada"},
        {"barraca","a barraca"},{"tenda","a tenda"},
        {"balcoes","os balcões"},{"balcao","o balcão"},
        {"quadro","o quadro"},{"avisos","os avisos"},
        {"folhetos","os folhetos"},{"folheto","o folheto"},
        {"pergaminho","o pergaminho"},{"livro","o livro"},
        {"mesa","a mesa"},{"carta","a carta"},{"mapa","o mapa"},
        {"moedas","as moedas"},{"baus","os baús"},{"bau","o baú"},
        {"porta","a porta"},{"escada","a escada"},
        {"ruinas","as ruínas"},{"ruina","a ruína"},
        {"rastros","os rastros"},{"fogueira","a fogueira"},
        {"caixotes","os caixotes"},{"caixote","o caixote"},
        {"prancheta","a prancheta"},{"obstaculo","o obstáculo"},
        {"armadilhas","as armadilhas"},{"altar","o altar"},
        {"inscricoes","as inscrições"},{"inscricao","a inscrição"}
    };

    static List<JSONObject> candidates(String narration){
        ArrayList<JSONObject> out=new ArrayList<>();
        String stage=visibleNarratorStage(narration);
        if(stage.length()<35)return out;
        // Full name of a person who actually speaks in THIS accepted narration.
        // Never take a name from a stale persisted NPC list.
        String actor=SceneDialogueDecisionPlanner.namedInterlocutor(narration);
        if(actor.isEmpty())actor=namedOnstageActor(narration);
        if(actor.isEmpty())actor=latest(stage,ACTORS);
        ArrayList<String> foci=visibleFoci(physicalStage(stage));
        // One visible noun is insufficient to fabricate three unrelated actions.
        // Continue to rely on authored/model options for sparse scenes.
        if(actor.isEmpty()||foci.size()<2)return out;
        String first=foci.get(0), second=foci.get(1);
        String destination=statedDirectionTarget(stage,foci);
        if(!destination.isEmpty()&&!destination.equals(first))second=destination;
        if(second.equals(first))return out;
        out.add(option("OBSERVAR","OBSERVAR "+upper(first),
            "Observo "+first+" e o movimento ao redor antes de decidir como agir."));
        out.add(option("AGIR","PERGUNTAR "+upper(dative(actor))+" SOBRE "+upper(first),
            "Pergunto "+dative(actor)+" como funciona a situação envolvendo "+first+
            " e aguardo a resposta antes de decidir."));
        if(!destination.isEmpty()&&destination.equals(second)){
            out.add(option("EXPLORAR","SEGUIR EM DIREÇÃO "+upper(dative(second)),
                "Decido seguir pelo caminho indicado em direção "+dative(second)+
                ", sem presumir que já cheguei ao destino ou que atravessei obstáculos."));
        }else if(SceneActionEvidence.normalize(second).contains("raizes")){
            // The actual onstage roots are a distinct inspectable focus;
            // "check the access to roots" would be semantically nonsensical.
            out.add(option("EXPLORAR","EXAMINAR "+upper(second),
                "Examino "+second+" junto "+genitive(first)+
                " para perceber seus detalhes visíveis, sem presumir que encontrei uma pista."));
        }else{
            out.add(option("EXPLORAR","VERIFICAR O ACESSO "+upper(dative(second)),
                "Aproximo-me "+genitive(second)+" para observar as condições de acesso, "+
                "sem presumir que já entrei ou que minha tentativa teve sucesso."));
        }
        return out;
    }
    /**
     * Named participants who are actively performing a visible action also
     * exist in the beat even when they do not deliver dialogue. Restrict this
     * to a full name immediately followed by a narrated action, never a mere
     * proper noun in a rumor, archive entry, map or prior scene.
     */
    private static String namedOnstageActor(String narration){
        if(narration==null)return "";
        String raw=SceneActionEvidence.currentStage(narration);
        java.util.regex.Matcher match=java.util.regex.Pattern.compile(
            "\\b([A-ZÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{L}'-]{2,}\\s+[A-ZÁÀÂÃÉÊÍÓÔÕÚÜÇ][\\p{L}'-]{2,})\\s+"
            +"(?:prende|fixa|crava|ajusta|amarra|segura|aponta|examina|observa|caminha|"
            +"abaixa|ergue|mostra|coloca|recolhe|entrega|puxa|empurra|conversa|fala|diz)\\b",
            java.util.regex.Pattern.UNICODE_CASE).matcher(raw);
        String actor="";
        while(match.find()){
            String prefix=SceneActionEvidence.normalize(raw.substring(
                Math.max(0,match.start()-45),match.start()));
            if(prefix.matches("(?s).*(?:relata que|conta que|menciona que|ouviu que|diz que|"
                +"lembra que|afirma que|escreveu que)\\s*"))continue;
            actor=match.group(1);
        }
        return actor;
    }

    /** Most prominent physically narrated focus, independent of NPC dialogue.
     * Dialogue proposals can combine it with the speaking character.
     */
    static String primaryVisibleFocus(String narration){
        ArrayList<String> found=visibleFoci(physicalStage(visibleNarratorStage(narration)));
        return found.isEmpty()?"":found.get(0);
    }

    /** Physical narration before AND after a separated dialogue paragraph.
     * Quoted speech can report an off-stage place but cannot place it next
     * to the hero. A narrator epilogue can reveal a new visible exit.
     */
    static String visibleNarratorStage(String narration){
        if(narration==null||narration.trim().isEmpty())return "";
        StringBuilder out=new StringBuilder();
        String[] blocks=narration.split("(?:\\r?\\n){2,}");
        for(String block:blocks){
            String t=block.trim();
            if(t.isEmpty())continue;
            if(t.matches("(?s)^[\\p{Lu}][\\p{L}'-]{2,}(?:[ \\t]+[\\p{Lu}][\\p{L}'-]{2,}){0,3}[ \\t]*:[ \\t]*[—-].*"))
                continue;
            String stage=SceneActionEvidence.currentStage(t);
            if(!stage.trim().isEmpty())out.append(' ').append(stage);
        }
        return SceneActionEvidence.normalize(out.toString());
    }

    /** A destination is accessible as a *player choice* only if narrator
     * explicitly describes a path leading toward it, not as a rumor.
     */
    private static String statedDirectionTarget(String stage,List<String> foci){
        String[] cues={"em direcao","caminho para","passagem para","que leva ao","que leva a","rumo ao","rumo a"};
        int at=-1;
        for(String cue:cues){int ix=stage.lastIndexOf(cue);if(ix>at)at=ix;}
        if(at<0)return "";
        String tail=stage.substring(at);
        if(tail.length()>130)tail=tail.substring(0,130);
        for(String focus:foci){
            String noun=SceneActionEvidence.normalize(focus).replaceFirst("^(o|a|os|as) ","");
            if(noun.length()>3&&contains(tail,noun))return focus;
        }
        return "";
    }

    private static JSONObject option(String type,String label,String action){
        JSONObject o=new JSONObject();
        try{o.put("type",type).put("action_type",type)
            .put("label",type+" • "+label)
            .put("action",action).put("description",action);}
        catch(Exception ignored){}
        return o;
    }
    private static String latest(String stage,String[][] table){
        int best=-1;String result="";
        for(String[] e:table){
            int p=(" "+stage+" ").lastIndexOf(" "+e[0]+" ");
            if(p>=0 && p>best && !absent(stage,e[0])){best=p;result=e[1];}
        }
        return result;
    }
    /** Reported objects are leads, not objects in the hero's reach. */
    private static String physicalStage(String stage){
        if(stage==null)return "";
        String[] cues={" relata que "," mencionou que "," menciona que ",
            " contou que "," conta que "," diz que "," afirmou que ",
            " afirma que "," ouviu que "," escreveu que "," lembra que "};
        int stop=stage.length();
        for(String cue:cues){
            int at=stage.indexOf(cue);
            if(at>=0&&at<stop)stop=at;
        }
        return stage.substring(0,stop);
    }

    private static ArrayList<String> visibleFoci(String stage){
        ArrayList<String> found=new ArrayList<>();
        for(String[] e:FOCI){
            if(contains(stage,e[0])&&!absent(stage,e[0])){
                boolean duplicate=false;
                for(String old:found){
                    String n=SceneActionEvidence.normalize(old),v=SceneActionEvidence.normalize(e[1]);
                    if(n.equals(v)||n.startsWith(v+" ")||v.startsWith(n+" ")){duplicate=true;break;}
                }
                if(!duplicate)found.add(e[1]);
            }
        }
        return found;
    }
    private static boolean absent(String stage,String target){
        return contains(stage,"sem "+target)||contains(stage,"nenhum "+target)
           ||contains(stage,"nenhuma "+target)||contains(stage,"nao ha "+target)
           ||contains(stage,"nao havia "+target)||contains(stage,"nao existe "+target);
    }
    private static boolean contains(String stage,String phrase){
        return (" "+stage+" ").contains(" "+phrase+" ");
    }
    private static String dative(String actor){
        if(actor.startsWith("a "))return "à "+actor.substring(2);
        if(actor.startsWith("as "))return "às "+actor.substring(3);
        if(actor.startsWith("os "))return "aos "+actor.substring(3);
        if(actor.startsWith("o "))return "ao "+actor.substring(2);
        return "a "+actor;
    }
    private static String genitive(String focus){
        if(focus.startsWith("a "))return "da "+focus.substring(2);
        if(focus.startsWith("as "))return "das "+focus.substring(3);
        if(focus.startsWith("os "))return "dos "+focus.substring(3);
        if(focus.startsWith("o "))return "do "+focus.substring(2);
        return "de "+focus;
    }
    private static String upper(String text){
        return text.toUpperCase(new java.util.Locale("pt","BR"));
    }
}
