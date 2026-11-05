package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Voluntary player-facing initiative handoff. This is not a GM_HELP question,
 * an auto-play command, a time skip, or permission to decide for a PC.
 */
public final class StoryMomentumAction {
    private StoryMomentumAction(){}

    public static final String LABEL="DEIXAR A HISTÓRIA ANDAR";
    public static final String ACTION=
        "Dou espaço para que a situação atual se desenvolva por iniciativa do mundo e das pessoas presentes; "+
        "permaneço atento, mas não tomo decisões ou ações além desta espera voluntária.";

    public static boolean isRequest(String action){
        return action!=null&&ACTION.equals(action.trim());
    }

    public static JSONObject option(){
        JSONObject o=new JSONObject();
        try{
            o.put("label",LABEL);
            o.put("description","Permitir que acontecimentos e NPCs deem o próximo passo, sem agir por você.");
            o.put("action",ACTION);
            o.put("story_momentum",true);
        }catch(Exception ignored){}
        return o;
    }

    /** Compatible with persisted options from earlier versions. Never force
     * another action: the user has to click this dedicated choice. */
    public static JSONArray ensureVisible(JSONArray options,String narration){
        JSONArray out=new JSONArray();
        if(narration==null||narration.trim().isEmpty())return options==null?out:options;
        out.put(option());
        int count=0;
        if(options!=null)for(int i=0;i<options.length()&&count<2;i++){
            JSONObject candidate=options.optJSONObject(i);
            if(candidate==null)continue;
            String action=candidate.optString("action",
                candidate.optString("description",candidate.optString("label","")));
            if(isRequest(action))continue;
            out.put(candidate);
            count++;
        }
        return out;
    }

    /**
     * Must enter actual model prompt, not merely an optional metadata flag.
     * The official Strixhaven event and persistent game state remain authoritative.
     */
    public static String instruction(boolean strixhaven){
        StringBuilder text=new StringBuilder();
        text.append("AÇÃO VOLUNTÁRIA — CEDER A INICIATIVA À HISTÓRIA, SEM AUTOPLAY.\n");
        text.append("O jogador escolheu observar e dar espaço para que o mundo se manifeste. ");
        text.append("Resolva exatamente UMA batida causal NOVA: um NPC presente inicia uma fala ou ação própria, ");
        text.append("um compromisso já estabelecido chama atenção, uma atividade em curso começa, ");
        text.append("ou uma consequência perceptível de algo já ocorrido se manifesta. ");
        text.append("Comece DIRETAMENTE pelo fato novo, em cena e com falas diretas quando houver interlocutor; ");
        text.append("não recapitule paisagem, entrada, horário, roupas ou a cena anterior. ");
        text.append("Se nenhum evento imediato for autorizado, mostre um pequeno desenvolvimento verificável da rotina atual ");
        text.append("e devolva ao jogador a oportunidade de escolher — não invente um mistério ou ataque para preencher a resposta.\n");
        text.append("LIMITES ABSOLUTOS: não fale, aceite proposta, responda pergunta, ande, se matricule, compre, ");
        text.append("conjure, investigue, assuma riscos, faça testes, gaste recursos ou escolha destinos pelo protagonista. ");
        text.append("Não force combate, revelação, romance, missão, aprovação, conclusão de evento ou salto temporal. ");
        text.append("Não pule decisão do jogador, teste pendente nem marco oficial; não antecipe evento futuro. ");
        text.append("NPCs só tomam iniciativas compatíveis com seu conhecimento, agenda e localização. ");
        text.append("Sem regras ou provas mecânicas, nenhuma mutação mecânica. Se tempo avançar por acontecimento ");
        text.append("real, use exclusivamente o tempo e as autoridades do host; não atribua duração arbitrária à espera.\n");
        if(strixhaven){
            text.append("STRIXHAVEN: priorize o marco CURRENT do runtime, jamais o próximo da lista. ");
            text.append("Deixe vida universitária respirar: orientação, aula, compromisso, contato social, ");
            text.append("professor, trabalho ou atividade extracurricular, somente quando sustentados pelo estado e fonte compilada. ");
            text.append("Em 'First Day on Campus', uma orientação só pode começar onde/como a fonte permite; ");
            text.append("se o aluno ainda não estiver no Hall dos Oráculos, convide ou sinalize o caminho, ");
            text.append("sem teletransportá-lo. Se já estiver lá, um responsável pode iniciar instruções. ");
            text.append("O primeiro ano não oferece matrícula em colégio; não conclua 'Orientation Challenge' ");
            text.append("nem qualquer outro evento pelo simples uso desta opção. ");
            text.append("Pistas e contatos só surgem quando plausíveis no evento em andamento.\n");
        }
        text.append("FORMATO: uma mudança observável e um gancho específico de resposta. ");
        text.append("Pare ANTES da decisão do jogador. Não ofereça outro botão para 'continuar' ");
        text.append("como única consequência; a nova informação/interação deve existir na própria narração.");
        return text.toString();
    }
}
