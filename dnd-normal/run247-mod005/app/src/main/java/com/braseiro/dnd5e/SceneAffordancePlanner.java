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
        // Reading a newly located announcement is a different player
        // decision from locating it. The notice must be visible, not merely
        // mentioned in a remote rumour.
        if(has(stage,"panfleto") && has(stage,"audicoes")){
            out.add(choice("EXPLORAR","LER O ANÚNCIO DE AUDIÇÕES",
                "Leio o panfleto de audições que está diante de mim, procurando identificar as instruções e sem presumir que me inscrevi."));
        }
        if(has(stage,"mesa de inscricoes")){
            out.add(choice("EXPLORAR","APROXIMAR-ME DA MESA DE INSCRIÇÕES",
                "Aproximo-me da mesa de inscrições indicada na cena para verificar o procedimento, sem presumir que já fui inscrito."));
        }
        if(has(stage,"roteiro") && has(stage,"anotacoes")
                && has(stage,"nota dobrada")){
            out.add(choice("OBSERVAR","CONFERIR AS ANOTAÇÕES DO ROTEIRO",
                "Leio as anotações nas margens do roteiro encadernado, sem presumir conhecer seu significado."));
            out.add(choice("AGIR","DESDOBRAR A NOTA DO ROTEIRO",
                "Tento desdobrar a nota encontrada no roteiro para ler exatamente o que ela informa, sem presumir outra pista."));
            out.add(choice("INVESTIGAR","COMPARAR O ROTEIRO E A NOTA",
                "Comparo a nota dobrada com as anotações do roteiro, procurando uma relação sem presumir que a encontrei."));
        }
        // A visible service counter is a separate exploration decision from
        // watching a document or questioning nearby students.
        if(has(stage,"balcao") && !has(stage,"balcao de atendimento")){
            out.add(choice("EXPLORAR","APROXIMAR-ME DO BALCÃO",
                "Aproximo-me do balcão que está à vista para conhecer o funcionamento do atendimento, sem presumir que sou atendido."));
        }
        // The notice at Firejolt Café gives two independent leads. They
        // authorize player choices to look for those places, not instant travel,
        // access, a completed task or the result of an investigation.
        if(has(stage,"bilhete") && has(stage,"caixas de ingredientes")
                && has(stage,"deposito dos fundos") && has(stage,"grade de ventilacao")){
            out.add(choice("AGIR","PROCURAR O DEPÓSITO DOS FUNDOS",
                "Decido procurar o depósito dos fundos mencionado no bilhete para saber como ajudar a organizar as caixas de ingredientes, sem presumir acesso."));
            out.add(choice("INVESTIGAR","INVESTIGAR O BARULHO MENCIONADO",
                "Procuro a grade de ventilação mencionada no bilhete para tentar localizar a origem do barulho, sem presumir ter encontrado algo."));
        }
        // A named interlocutor can disclose a destination without making its
        // props physically reachable. Following it is a voluntary attempt.
        if(has(SceneActionEvidence.normalize(narration),"recado no quadro")
                && has(SceneActionEvidence.normalize(narration),"cozinha")){
            out.add(choice("EXPLORAR","PROCURAR O QUADRO JUNTO À COZINHA",
                "Decido procurar o quadro junto à cozinha mencionado na conversa, sem presumir que já li o recado."));
        }
        if(has(stage,"placa solta") && has(stage,"monitor")){
            out.add(choice("OBSERVAR","OBSERVAR A PLACA SOLTA",
                "Observo a placa solta no piso que o monitor está examinando, sem pressupor a causa."));
            out.add(choice("EXPLORAR","APROXIMAR-ME DO MONITOR",
                "Aproximo-me do monitor junto à placa solta para saber se precisa de ajuda, aguardando sua reação."));
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
        // Two independent activities actually visible at the orientation:
        // the paperboard theatre rehearsal and the employee taking registrations.
        // These are intentional attempts, not assumed discoveries or enrolments.
        if(has(stage,"encenacao")&&has(stage,"armadilhas")&&has(stage,"papelao")
                &&has(stage,"inscricoes")&&has(stage,"funcionaria")){
            out.add(choice("OBSERVAR","OBSERVAR A ENCENAÇÃO",
                "Observo como a Guilda prepara as armadilhas de papelão para a encenação, sem interferir nos preparativos."));
            out.add(choice("CONVERSAR","PERGUNTAR À FUNCIONÁRIA SOBRE AS INSCRIÇÕES",
                "Pergunto à funcionária como funcionam as inscrições dos recém-chegados, aguardando sua resposta."));
            out.add(choice("EXPLORAR","CONHECER A GUILDA DE RPG LIVE-ACTION",
                "Aproximo-me da Guilda de RPG Live-Action para saber como participar da encenação, sem presumir que fui aceito."));
        }
        // Here the awakened shrubs are explicitly acting in the current garden,
        // unlike rumours about shrubs elsewhere. Observe, address, or explore
        // the visible planting beds; never materialize an absent NPC or clue.
        if(has(stage,"jardim dos livros")&&has(stage,"arbustos despertados")
                &&has(stage,"canteiros")&&has(stage,"estudantes")){
            out.add(choice("OBSERVAR","OBSERVAR OS ARBUSTOS DESPERTADOS",
                "Observo os arbustos despertados ajustando as raízes junto aos canteiros."));
            out.add(choice("CONVERSAR","FALAR COM OS ESTUDANTES SOBRE OS ARBUSTOS",
                "Pergunto aos estudantes que passam pelos canteiros se conhecem os arbustos despertados, sem presumir a resposta."));
            out.add(choice("EXPLORAR","EXAMINAR OS CANTEIROS",
                "Examino os canteiros onde os arbustos despertados ajustam as raízes, sem tocar nas plantas nem presumir uma descoberta."));
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
