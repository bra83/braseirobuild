package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Voluntary player-facing initiative handoff. This is not a GM_HELP question,
 * an auto-play command, a time skip, or permission to decide for a PC.
 */
public final class StoryMomentumAction {
    private StoryMomentumAction(){}

    public static final String LABEL="PERMANECER À ESCUTA";
    public static final String ACTION=
        "Dou espaço para que a situação atual se desenvolva por iniciativa do mundo e das pessoas presentes; "+
        "permaneço atento, mas não tomo decisões ou ações além desta espera voluntária.";

    public static boolean isRequest(String action){
        return action!=null&&(ACTION.equals(action.trim())||action.trim().startsWith("Sigo a pista revelada:"));
    }

    public static JSONObject option(){ return option(""); }

    public static JSONObject option(String narration){
        String beat=narration==null?"":narration.toLowerCase(java.util.Locale.ROOT);
        String label=LABEL;
        final boolean discovered=SceneDiscoveredLead.available(narration);
        if(discovered)label=SceneDiscoveredLead.progressLabel(narration);
        if(!discovered&&beat.contains("charada")&&beat.contains("selos"))label="DEIXAR A DISCUSSÃO DOS SELOS AVANÇAR";
        else if(beat.contains("bibliotecária")||beat.contains("bibliotecaria"))label="ESCUTAR A BIBLIOTECÁRIA";
        else if(beat.contains("professora"))label="ESCUTAR A PROFESSORA";
        else if(beat.contains("professor"))label="ESCUTAR O PROFESSOR";
        else if(beat.contains("orientação")||beat.contains("orientacao")||beat.contains("assistente"))label="AGUARDAR A PRÓXIMA INSTRUÇÃO";
        else if(beat.contains("calouros")||beat.contains("alunos")||beat.contains("estudantes"))label="OUVIR OS ESTUDANTES";
        else if(beat.matches("(?i).*\\b(?:guarda|guardas|sentinela|sentinelas)\\b.*"))label="OBSERVAR OS PRÓXIMOS MOVIMENTOS";
        else if(beat.contains("pausa")||beat.contains("silêncio"))label="PERMITIR UMA NOVA INICIATIVA";
        else if(beat.contains("taverna")||beat.contains("estalagem"))label="ESCUTAR O MOVIMENTO DO SALÃO";
        if(discovered)label=SceneDiscoveredLead.progressLabel(narration); // Explicit objective always beats historic keyword matching.
        String currentInterlocutor=SceneDialogueDecisionPlanner.namedInterlocutor(narration);
        if(!discovered&&!currentInterlocutor.isEmpty())
            label="DAR ESPAÇO PARA "+currentInterlocutor.toUpperCase(new java.util.Locale("pt","BR"))+" CONTINUAR";
        JSONObject o=new JSONObject();
        try{
            o.put("label",label);
            o.put("description",discovered
                ? "Decido seguir até o local indicado pela pista já revelada e procurar a próxima evidência; nenhuma descoberta ou teste é presumido."
                :beat.contains("charada")&&beat.contains("selos")
                ?"Acompanho a discussão da charada e dos selos, deixando os estudantes tomarem sua própria iniciativa sem decidir nada por meu personagem."
                :"Permaneço atento à iniciativa das pessoas e aos acontecimentos já presentes, sem presumir o que vão fazer.");
            o.put("action",discovered?SceneDiscoveredLead.progressAction(narration):ACTION);
            o.put("story_momentum",true);
        }catch(Exception ignored){}
        return SceneActionEvidence.bind(o,narration);
    }

    /** Compatible with persisted options from earlier versions. Never force
     * another action: the user has to click this dedicated choice. */
    /** Display an already vetted result. Never regenerate from saved cards. */
    public static JSONArray withChoices(JSONArray choices,String narration){
        JSONArray out=new JSONArray();
        if(narration==null||narration.trim().isEmpty())return out;
        out.put(option(narration));
        if(choices==null)return out;
        for(int i=0;i<choices.length()&&out.length()<4;i++){
            JSONObject item=choices.optJSONObject(i);
            if(item==null||isRequest(item.optString("action","")))continue;
            String ticket=item.optString("scene_fingerprint","");
            if(!ticket.isEmpty()&&!SceneActionEvidence.hash(narration).equals(ticket))continue;
            if(!SceneActionEvidence.valid(item,narration))continue;
            boolean duplicate=false;
            for(int j=1;j<out.length();j++)
                if(SceneActionEvidence.sameDecision(item.optString("action",""),
                        out.optJSONObject(j).optString("action",""))){duplicate=true;break;}
            if(!duplicate)out.put(SceneActionEvidence.bind(item,narration));
        }
        return out;
    }

    /** Restored choices are revalidated, never replaced by speculative fillers. */
    public static JSONArray ensureVisible(JSONArray options,String narration){
        return withChoices(options,narration);
    }

    /** A lead-follow click authorizes the stated player movement, unlike waiting. */
    public static String instruction(boolean strixhaven,String action){
        if(action==null||!action.trim().startsWith("Sigo a pista revelada:"))
            return instruction(strixhaven);
        return "AÇÃO VOLUNTÁRIA DO JOGADOR — SEGUIR A PISTA JÁ REVELADA. "
            +"O jogador escolheu expressamente executar este deslocamento/investigação: "+action
            +"\\nResolva somente essa tentativa e a próxima batida causal, mantendo a geografia e os NPCs atuais. "
            +"O objetivo é procurar a evidência no destino informado, NÃO pressupor que ela foi encontrada. "
            +"Se surgir dificuldade ou risco real, apresente o teste válido e PAUSE ANTES do resultado. "
            +"Não narre sucesso/fracasso de teste sem rolagem, não conceda prêmio, não crie pista nem pule o evento. "
            +"Não force autoplays, combate, transporte instantâneo ou avanço do relógio não comprovado. "
            +"Interrompa antes de outra escolha do protagonista. "
            +(strixhaven?"Respeite o marco atual do runtime de Strixhaven; não conclua Orientation Challenge.":"");
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
