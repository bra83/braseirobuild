package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reveal that already points somewhere is a new player decision, NOT a new
 * invitation to wait for an unrelated orientation announcement.
 *
 * Read only the narration the player can see. Never infer a prop from an
 * adjective ("prato diário" is not a diary) or silently resolve a check.
 */
public final class SceneDiscoveredLead {
    private SceneDiscoveredLead(){}

    public static boolean available(String narration){
        String n=fold(narration);
        if(n.length()<80)return false;
        boolean reveal=has(n,"pista esta decifrada") || has(n,"pista foi decifrada")
            || has(n,"coordenadas") && has(n,"intersecao")
            || has(n,"proximo elemento") && has(n,"aponta")
            || has(n,"pista revelada") && has(n,"indica");
        return reveal&&!destination(narration).isEmpty();
    }

    /** The destination is a grounded noun phrase, never a generated map location. */
    public static String destination(String narration){
        String n=fold(narration);
        if(n.isEmpty())return "";
        if(has(n,"mural de raizes"))return "o mural de raízes";
        if(has(n,"mural"))return "o mural";
        if(has(n,"escadaria"))return "a escadaria";
        if(has(n,"ponte"))return "a ponte";
        if(has(n,"entrada") && has(n,"cafe do biblioplex"))return "a entrada do Café do Biblioplex";
        return "";
    }

    public static String progressLabel(String narration){
        String n=fold(narration);
        if(has(n,"mural de raizes"))return "IR AO PONTO INDICADO JUNTO AO MURAL";
        if(has(n,"intersecao"))return "SEGUIR ATÉ A INTERSEÇÃO INDICADA";
        return "SEGUIR A PISTA ATÉ "+destination(narration).toUpperCase(new Locale("pt","BR"));
    }

    public static String progressAction(String narration){
        String n=fold(narration);
        String dest=destination(narration);
        if(dest.isEmpty())return "";
        if(has(n,"intersecao")&&has(n,"cafe do biblioplex")&&has(n,"mural de raizes"))
            return "Sigo a pista revelada: decido ir até a interseção indicada entre a entrada do Café do Biblioplex e o mural de raízes para procurar o próximo elemento da caça. Não considero a pista seguinte encontrada antes de investigá-la.";
        return "Sigo a pista revelada: decido me aproximar de "+dest+" para procurar a próxima evidência mencionada na cena, interrompendo a ação diante de obstáculos ou testes necessários.";
    }

    public static JSONObject typedChoice(String type,String narration){
        if(!available(narration))return null;
        String n=fold(narration), label="", action="";
        boolean seal=has(n,"selo de cera")||has(n,"selo")&&has(n,"micrografica");
        boolean pergaminho=has(n,"pergaminho");
        boolean students=has(n,"calouros");
        if(GroundedSceneActionSlots.OBSERVAR.equals(type)){
            if(seal&&has(n,"micrografica")){
                label="EXAMINAR A MICROGRAFIA DO SELO";
                action="Examino a sequência micrográfica gravada no selo de cera para tentar identificar o ingrediente citado pela pista.";
            }else if(pergaminho){
                label="CONFERIR AS COORDENADAS DO PERGAMINHO";
                action="Confiro as coordenadas no pergaminho antes de decidir como seguir o caminho indicado.";
            }else{
                label="EXAMINAR A REFERÊNCIA AO "+destination(narration).toUpperCase(new Locale("pt","BR"));
                action="Examino a indicação que aponta para "+destination(narration)+", procurando um detalhe que ajude a interpretar a pista.";
            }
        }else if(GroundedSceneActionSlots.AGIR.equals(type)){
            if(students&&has(n,"caca de boas vindas")){
                label="PERGUNTAR AOS CALOUROS SOBRE A CAÇA";
                action="Pergunto aos calouros se conhecem a etapa seguinte da caça de boas-vindas e ouço o que têm a dizer.";
            }else{
                label="CONFERIR O LOCAL INDICADO NA CENA";
                action="Comparo a indicação revelada com "+destination(narration)+" antes de escolher meu próximo movimento.";
            }
        }else if(GroundedSceneActionSlots.DESPISTAR.equals(type)){
            if(students&&pergaminho){
                label="ESCONDER AS COORDENADAS DOS CALOUROS";
                action="Tento manter as coordenadas do pergaminho fora da vista dos calouros enquanto examino a pista, sem decidir por eles como vão reagir.";
            }else{
                label="EXAMINAR A PISTA COM DISCRIÇÃO";
                action="Examino discretamente a indicação que aponta para "+destination(narration)+", tentando não atrair atenção.";
            }
        }
        if(label.isEmpty()||action.isEmpty())return null;
        JSONObject o=new JSONObject();
        try{
            o.put("type",type).put("action_type",type)
                .put("label",type+" • "+label)
                .put("action",action).put("description",action);
        }catch(Exception ignored){}
        return o;
    }

    private static boolean has(String text,String phrase){
        return (" "+fold(text)+" ").contains(" "+fold(phrase)+" ");
    }
    private static String fold(String v){
        if(v==null)return "";
        return Normalizer.normalize(v,Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
}
