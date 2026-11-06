package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.json.JSONArray;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Checks the VISIBLE narration before displaying an option. Database scene
 * metadata can outlive the current beat; it is never proof that a proposed
 * character or prop is physically in front of the player.
 *
 * This gate intentionally uses word boundaries: "guarda" is NOT supported by
 * "aguarda", "guardado", or "guardados".
 */
public final class SceneOptionVisibleEvidence {
    private SceneOptionVisibleEvidence() {}

    private static final String[][] ANCHORS = {
        {"guarda","guardas","sentinela","sentinelas","soldado","soldados"},
        {"bibliotecaria","bibliotecario","bibliotecarias","bibliotecarios"},
        {"professor","professora","professores","professoras"},
        {"funcionario","funcionaria","funcionarios","funcionarias"},
        {"assistente","assistentes","auxiliar","auxiliares"},
        {"aluno","aluna","alunos","alunas","estudante","estudantes","calouro","caloura","calouros","calouras","veterano","veterana","veteranos","veteranas"},
        {"mercador","mercadora","mercadores","comerciante","comerciantes"},
        {"taverneiro","taverneira","taverneiros","taverneiras"},
        {"viajante","viajantes","caravaneiro","caravaneiros"},
        {"criatura","criaturas","monstro","monstros"},
        {"porta","portas","portao","portoes"},
        {"armario","armarios","bau","baus"},
        {"mapa","mapas","pergaminho","pergaminhos"},
        {"livro","livros","tomo","tomos"},
        {"chave","chaves"},
        {"carta","cartas","documento","documentos","contrato","contratos"},
        {"cartaz","cartazes","aviso","avisos","folheto","folhetos","panfleto","panfletos"},
        {"ponte","pontes"},
        {"rastro","rastros","pegada","pegadas"},
        {"prancheta","pranchetas"},
        {"mesa","mesas"},
        {"balcao","balcoes"},
        {"escada","escadas","escadaria","escadarias"}
    };

    static boolean allSupported(JSONArray options,String narration){
        if(options==null)return true;
        for(int i=0;i<options.length();i++){
            JSONObject option=options.optJSONObject(i);
            if(option!=null&&!supported(option,narration))return false;
        }
        return true;
    }

    static boolean supported(JSONObject option,String narration){
        if(option==null)return false;
        String action=option.optString("action","");
        if(StoryMomentumAction.isRequest(action))return true;
        String proposed=fold(option.optString("label","")+" "+action+" "+option.optString("description",""));
        String visible=fold(narration);
        if(visible.isEmpty()||proposed.isEmpty())return false;
        for(String[] aliases:ANCHORS){
            boolean mentioned=false;
            for(String alias:aliases){
                if(containsWord(proposed,alias)){mentioned=true;break;}
            }
            if(!mentioned)continue;
            // "guarda o papel" is the verb guardar, not an NPC guard.
            if(aliases==ANCHORS[0]&&containsWord(proposed,"guarda")
                &&!hasExplicitGuardNoun(visible))return false;
            boolean inScene=false;
            for(String alias:aliases){
                if(containsWord(visible,alias)&&!explicitlyAbsent(visible,alias)){inScene=true;break;}
            }
            if(!inScene)return false;
        }
        return true;
    }

    static boolean guardNounPresent(String narration){
        return hasExplicitGuardNoun(fold(narration));
    }

    private static boolean hasExplicitGuardNoun(String context){
        if(context==null)return false;
        if(containsWord(context,"guardas")||containsWord(context,"sentinela")
            ||containsWord(context,"sentinelas")||containsWord(context,"soldado")
            ||containsWord(context,"soldados"))return true;
        return context.matches("(?s).*(?:^|\\s)(?:o|a|um|uma|ao|do|da|pelo|pela|esse|essa|aquele|aquela|com|para|sobre|sem|nenhum|nenhuma)\\s+guarda(?:\\s|$).*");
    }
    private static boolean explicitlyAbsent(String context,String noun){
        return containsPhrase(context,"sem "+noun)
            ||containsPhrase(context,"nenhum "+noun)
            ||containsPhrase(context,"nenhuma "+noun)
            ||containsPhrase(context,"nao ha "+noun)
            ||containsPhrase(context,"nao havia "+noun)
            ||containsPhrase(context,"nao existe "+noun)
            ||containsPhrase(context,"ausencia de "+noun);
    }
    private static boolean containsPhrase(String context,String phrase){
        return (" "+context+" ").contains(" "+phrase+" ");
    }
    private static boolean containsWord(String context,String noun){
        return containsPhrase(context,noun);
    }
    private static String fold(String text){
        if(text==null)return "";
        return Normalizer.normalize(text,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
}
