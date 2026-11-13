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
        // A door explicitly ajar in the present corridor is an independent
        // affordance from reading the nearby notebook. Approaching does not
        // presume entry, a perception result, or permission to open it.
        if(has(stage,"porta entreaberta") && has(stage,"brilho azulado")
                && has(stage,"caderno")){
            out.add(choice("EXPLORAR","APROXIMAR-ME DA PORTA ENTREABERTA",
                "Aproximo-me da porta entreaberta para observar a origem aparente do brilho azulado, sem entrar nem pressupor o que há dentro."));
        }
        // Place-specific affordances are based on multiple present, independent
        // props/actors from the accepted beat. No fallback reads stale NPC state.
        if(has(stage,"capitao") && has(stage,"marcadores") && has(stage,"caixote")){
            out.add(choice("OBSERVAR","OBSERVAR OS MARCADORES",
                "Observo os marcadores de campo que o capitão está ajustando antes de interferir no treino."));
            out.add(choice("AGIR","FALAR COM O CAPITÃO",
                "Pergunto ao capitão como estão sendo preparados os marcadores para o treino."));
            out.add(choice("EXPLORAR","VERIFICAR O CAIXOTE",
                "Aproximo-me do caixote de equipamentos para ver o que está acessível, sem pegar nada."));
        }
        if(has(stage,"pesquisador") && has(stage,"fragmentos de ceramica") && has(stage,"caixa lacrada")){
            out.add(choice("OBSERVAR","EXAMINAR OS FRAGMENTOS",
                "Examino visualmente os fragmentos de cerâmica sobre a bancada sem removê-los."));
            out.add(choice("AGIR","FALAR COM O PESQUISADOR",
                "Pergunto ao pesquisador anão o que está fazendo com os fragmentos de cerâmica."));
            out.add(choice("EXPLORAR","VERIFICAR A CAIXA LACRADA",
                "Aproximo-me da caixa lacrada para observar seu exterior, sem abri-la."));
        }
        if(has(stage,"guardas") && has(stage,"carrocas") && has(stage,"barqueiro")){
            out.add(choice("OBSERVAR","OBSERVAR A INSPEÇÃO",
                "Observo os guardas verificando as carroças junto ao portão."));
            out.add(choice("AGIR","PERGUNTAR AOS GUARDAS",
                "Pergunto aos guardas como funciona a passagem pelo portão e aguardo a resposta."));
            out.add(choice("EXPLORAR","APROXIMAR-ME DO BARQUEIRO",
                "Aproximo-me do barqueiro junto ao píer para saber se há uma travessia disponível."));
        }
        if(has(stage,"mercadora de mapas") && has(stage,"mensageiro") && has(stage,"banca")){
            out.add(choice("OBSERVAR","OBSERVAR OS MAPAS",
                "Examino visualmente os mapas que a mercadora enrola sobre a banca."));
            out.add(choice("AGIR","PERGUNTAR À MERCADORA",
                "Pergunto à mercadora o que mostram os mapas da banca, sem presumir suas informações."));
            out.add(choice("EXPLORAR","ABORDAR O MENSAGEIRO",
                "Aproximo-me do mensageiro que procura viajantes rumo ao norte para ouvir sua proposta."));
        }
        if(has(stage,"mestre de cais") && has(stage,"caixotes") && has(stage,"passarela")){
            out.add(choice("OBSERVAR","OBSERVAR OS SELOS",
                "Observo os selos de cera nos caixotes conferidos pelo mestre de cais."));
            out.add(choice("AGIR","PERGUNTAR SOBRE OS CAIXOTES",
                "Pergunto ao mestre de cais sobre os caixotes que está conferindo."));
            out.add(choice("EXPLORAR","EXAMINAR A PASSARELA",
                "Examino visualmente a passarela de embarque antes de decidir se a utilizo."));
        }
        if(has(stage,"marco de pedra rachado") && has(stage,"bifurcacao") && has(stage,"carroca parada")){
            out.add(choice("OBSERVAR","LER O MARCO DE PEDRA",
                "Examino o marco de pedra rachado para ler o que ainda indica."));
            out.add(choice("AGIR","EXAMINAR A CARROÇA",
                "Aproximo-me da carroça parada à beira da bifurcação para observar sinais visíveis, sem presumir ocupantes."));
            out.add(choice("INVESTIGAR","COMPARAR AS DIREÇÕES",
                "Comparo as direções da bifurcação com o marco de pedra antes de escolher um trajeto."));
        }
        if(has(stage,"obelisco") && has(stage,"wilem") && has(stage,"simmon") && has(stage,"fogueira")){
            out.add(choice("OBSERVAR","EXAMINAR O OBELISCO",
                "Examino o obelisco diante dos viajantes sem assumir conhecer a inscrição."));
            out.add(choice("AGIR","PERGUNTAR A WILEM",
                "Pergunto a Wilem o que os viajantes sabem sobre o obelisco."));
            out.add(choice("CONVERSAR","FALAR COM SIMMON",
                "Dirijo-me a Simmon junto à fogueira e pergunto sobre a viagem do grupo."));
        }
        if(has(stage,"wilem") && has(stage,"inscricoes do obelisco") && has(stage,"rota leste")){
            out.add(choice("OBSERVAR","OBSERVAR AS INSCRIÇÕES",
                "Examino as inscrições do obelisco antes de decidir entre as alternativas."));
            out.add(choice("AGIR","PERGUNTAR SOBRE A ROTA",
                "Pergunto a Wilem o que sabe sobre a rota leste mencionada na conversa."));
            out.add(choice("EXPLORAR","AVALIAR O CAMINHO LESTE",
                "Procuro avaliar o começo da rota leste indicada por Wilem, sem presumir que já a percorri."));
        }
        if(has(stage,"estalajadeiro") && has(stage,"chaves de bronze") && has(stage,"quadro de avisos")){
            out.add(choice("OBSERVAR","LER OS AVISOS",
                "Leio os avisos visíveis no quadro da estrada, sem presumir seu conteúdo."));
            out.add(choice("AGIR","FALAR COM O ESTALAJADEIRO",
                "Pergunto ao estalajadeiro a que correspondem as chaves de bronze no balcão."));
            out.add(choice("EXPLORAR","OBSERVAR AS CHAVES",
                "Examino visualmente as chaves de bronze sobre o balcão sem pegá-las."));
        }
        if(has(stage,"porta de ferro") && has(stage,"relevo de serpente") && has(stage,"pedestal")){
            out.add(choice("OBSERVAR","EXAMINAR O RELEVO",
                "Observo o relevo de serpente na porta de ferro sem tocar no mecanismo."));
            out.add(choice("EXPLORAR","CONFERIR O PEDESTAL",
                "Aproximo-me do pedestal baixo no piso para examinar sua superfície sem acioná-lo."));
            out.add(choice("INVESTIGAR","COMPARAR PORTA E PEDESTAL",
                "Comparo os detalhes visíveis do pedestal com o relevo de serpente na porta, sem presumir ligação."));
        }
        if(has(stage,"rastros recentes") && has(stage,"estatua partida") && has(stage,"alavanca")){
            out.add(choice("OBSERVAR","SEGUIR OS RASTROS COM OS OLHOS",
                "Observo a direção dos rastros recentes no chão antes de pisar neles."));
            out.add(choice("AGIR","VERIFICAR A ALAVANCA",
                "Aproximo-me da alavanca na parede norte para inspecioná-la sem puxá-la."));
            out.add(choice("INVESTIGAR","EXAMINAR A ESTÁTUA PARTIDA",
                "Examino visualmente a estátua partida perto dos rastros, sem presumir que o objeto foi movido."));
        }
        if(has(stage,"altar de basalto") && has(stage,"cofre de prata") && has(stage,"pergaminho lacrado")){
            out.add(choice("OBSERVAR","EXAMINAR O PERGAMINHO",
                "Examino o pergaminho lacrado junto ao cofre de prata sem romper o lacre."));
            out.add(choice("AGIR","CONFERIR O COFRE",
                "Tento verificar o exterior do cofre de prata fechado sem abri-lo."));
            out.add(choice("EXPLORAR","OBSERVAR O ALTAR",
                "Aproximo-me do altar de basalto para examinar sua superfície sem tocar nos objetos."));
        }
        if(has(stage,"nichos") && has(stage,"inscricoes arcanas") && has(stage,"diario aberto") && has(stage,"frascos")){
            out.add(choice("OBSERVAR","OBSERVAR AS INSCRIÇÕES",
                "Examino as inscrições arcanas nos nichos de pedra antes de interpretá-las."));
            out.add(choice("AGIR","LER O DIÁRIO ABERTO",
                "Procuro ler as anotações visíveis no diário aberto sobre a mesa de alquimia."));
            out.add(choice("INVESTIGAR","CONFERIR OS FRASCOS",
                "Comparo as etiquetas dos frascos sobre a mesa sem destampá-los."));
        }
        if(has(stage,"marco do percurso") && has(stage,"mapa") && has(stage,"cofre recuperado")){
            out.add(choice("OBSERVAR","CONSULTAR O MAPA",
                "Examino o mapa reunido sobre a pedra plana junto ao grupo."));
            out.add(choice("AGIR","CONFERIR O COFRE",
                "Tento verificar o estado do cofre recuperado que está sobre a pedra plana, sem abri-lo."));
            out.add(choice("INVESTIGAR","CONFERIR O OBELISCO",
                "Examino o obelisco no Marco do Percurso em busca de marcas visíveis, sem presumir ter encontrado uma nova pista."));
        }
        // A stated destination is an invitation to choose a route, not proof
        // that its contents are accessible where the party currently stands.
        if(has(SceneActionEvidence.normalize(narration),"wilem")
                && has(SceneActionEvidence.normalize(narration),"inscricoes do obelisco")
                && has(SceneActionEvidence.normalize(narration),"rota leste")){
            out.add(choice("OBSERVAR","OBSERVAR WILEM","Observo a reação de Wilem enquanto ele explica as alternativas junto ao obelisco."));
            out.add(choice("AGIR","PERGUNTAR A WILEM","Pergunto a Wilem o que sabe sobre as inscrições do obelisco antes de escolher."));
            out.add(choice("EXPLORAR","PROCURAR O CAMINHO LESTE","Decido procurar o início da rota leste indicada por Wilem, sem presumir que cheguei ao destino."));
        }
        // Bow's End Tavern: the host and the active throwing game are two
        // independent interaction points. The challenge board is incidental.
        if(has(stage,"tulk")&&has(stage,"taverneiro")&&has(stage,"arremessos")
                &&has(stage,"grupo")&&has(stage,"quadro de desafios")){
            out.add(choice("OBSERVAR","OBSERVAR A DISPUTA DE ARREMESSOS",
                "Observo como o grupo disputa os arremessos junto ao quadro de desafios, sem participar ainda."));
            out.add(choice("AGIR","PERGUNTAR A TULK SOBRE A DISPUTA",
                "Pergunto a Tulk como funciona a disputa de arremessos e se é possível participar, sem presumir sua resposta."));
            out.add(choice("EXPLORAR","APROXIMAR-ME DOS PARTICIPANTES",
                "Aproximo-me dos participantes da disputa para entender as regras em vigor, sem iniciar um arremesso por conta própria."));
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
