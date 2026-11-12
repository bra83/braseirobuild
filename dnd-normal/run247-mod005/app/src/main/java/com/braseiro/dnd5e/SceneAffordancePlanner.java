package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Small conservative affordance catalogue, driven by physically narrated
 * facts rather than topic keywords from dialogue, previous scenes or lore.
 * A fallback is a decision to attempt, never a narrated result.
 */
public final class SceneAffordancePlanner {
    private SceneAffordancePlanner(){}

    static List<JSONObject> candidates(String narration){
        List<JSONObject> out=new ArrayList<>();
        String stage=SceneActionEvidence.normalize(SceneActionEvidence.currentStage(narration));
        if(stage.length()<16)return out;
        // Independent decisions for a visible student activity and a separate
        // enrollment desk. Neither invents an enrollment result or unseen staff.
        if(has(stage,"guilda de rpg live action") && has(stage,"armadilhas de papelao")
                && has(stage,"encenacao") && has(stage,"funcionaria")
                && has(stage,"inscricoes")){
            out.add(choice("OBSERVAR","EXAMINAR AS ARMADILHAS DE PAPELÃO",
                "Observo as armadilhas de papelão preparadas pela Guilda de RPG Live-Action para a encenação, sem presumir como funcionam."));
            out.add(choice("AGIR","PERGUNTAR À FUNCIONÁRIA SOBRE INSCRIÇÕES",
                "Pergunto à funcionária como funcionam as inscrições dos recém-chegados, aguardando a explicação antes de decidir."));
            out.add(choice("EXPLORAR","APROXIMAR-ME DA GUILDA",
                "Aproximo-me da Guilda de RPG Live-Action para acompanhar os preparativos da encenação, sem interromper ou assumir participação."));
        }
        // The shrubs are physically narrated in THIS garden. Past debts and
        // NPCs in saved metadata do not authorize related suggestions here.
        if(has(stage,"arbustos despertados") && has(stage,"raizes")
                && has(stage,"estudantes") && has(stage,"canteiros")){
            out.add(choice("OBSERVAR","OBSERVAR OS ARBUSTOS DESPERTADOS",
                "Observo os arbustos despertados enquanto ajustam as raízes junto aos canteiros, procurando perceber seus movimentos."));
            out.add(choice("AGIR","APROXIMAR-ME DOS ARBUSTOS",
                "Aproximo-me dos arbustos despertados nos canteiros para tentar estabelecer contato, sem presumir que respondam."));
            out.add(choice("CONVERSAR","PERGUNTAR AOS ESTUDANTES SOBRE OS ARBUSTOS",
                "Pergunto aos estudantes que passam o que sabem sobre os arbustos despertados, sem presumir que conheçam a resposta."));
        }
        if(has(stage,"folhetos")||has(stage,"folheto")){
            out.add(choice("OBSERVAR","LER OS FOLHETOS DISPONÍVEIS",
                "Leio os folhetos que estão à vista e verifico o que oferecem, sem presumir o conteúdo."));
        }
        if(has(stage,"balcoes")||has(stage,"balcao de atendimento")){
            out.add(choice("EXPLORAR","APROXIMAR-ME DOS BALCÕES DE ATENDIMENTO",
                "Aproximo-me dos balcões de atendimento para verificar como obter orientação, sem pressupor que haja um funcionário livre."));
        }
        if(has(stage,"veteranos")||has(stage,"veterano")){
            out.add(choice("CONVERSAR","PERGUNTAR A UM VETERANO",
                "Pergunto a um veterano presente se pode me orientar sobre as atividades do local, aguardando sua resposta."));
        }
        if(has(stage,"prancheta")&&(has(stage,"assistente")||has(stage,"auxiliar"))){
            out.add(choice("OBSERVAR","CONFERIR O PROCEDIMENTO DE TRIAGEM",
                "Observo como a assistente utiliza a prancheta antes de me apresentar, sem presumir a ordem da fila."));
        }
        if(has(stage,"pergaminho")||has(stage,"velino")){
            out.add(choice("OBSERVAR","LER O DOCUMENTO",
                "Leio as informações visíveis no pergaminho, sem presumir que já compreendi seu significado."));
            if(has(stage,"sobre a mesa")||has(stage,"diante de")||has(stage,"desenrolada")){
                out.add(choice("AGIR","CONFERIR O VERSO DO DOCUMENTO",
                    "Tento virar cuidadosamente o pergaminho para verificar o verso, sem pressupor que exista algo escrito."));
            }
        }
        if(has(stage,"guardas")&&has(stage,"ponte")){
            out.add(choice("OBSERVAR","ACOMPANHAR OS GUARDAS",
                "Observo os movimentos dos guardas na ponte antes de decidir como abordá-los."));
            out.add(choice("CONVERSAR","PERGUNTAR AOS GUARDAS SOBRE A PASSAGEM",
                "Pergunto aos guardas que vigiam a ponte se a passagem está permitida."));
            out.add(choice("EXPLORAR","EXAMINAR A TRAVESSIA",
                "Examino visualmente a estrutura da ponte e suas entradas para escolher um caminho, sem presumir sua segurança."));
        }
        return out;
    }

    private static JSONObject choice(String type,String label,String action){
        JSONObject o=new JSONObject();
        try{
            o.put("type",type).put("action_type",type)
             .put("label",type+" • "+label).put("action",action).put("description",action);
        }catch(Exception ignored){}
        return o;
    }
    private static boolean has(String context,String word){return SceneActionEvidence.has(context,word);}
}
