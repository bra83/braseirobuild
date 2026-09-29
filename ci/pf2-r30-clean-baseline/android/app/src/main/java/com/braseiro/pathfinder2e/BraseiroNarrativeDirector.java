package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Direção narrativa Braseiro 6.1.0 — Narrative Director 2.0 + Mesa Viva.
 *
 * Une quatro responsabilidades que antes ficavam misturadas:
 * 1) decidir se a mensagem do jogador realmente move a ficção;
 * 2) preservar o ponto exato de continuação da cena;
 * 3) dirigir percepção, progressão, espaço, pistas, tensão social e clímax;
 * 4) impedir repetição e vazamento de conhecimento do Mestre.
 */
public final class BraseiroNarrativeDirector {
    private BraseiroNarrativeDirector(){}

    public enum Intent {
        DIEGETIC_ACTION,
        DIEGETIC_SPEECH,
        RULE_QUERY,
        NARRATIVE_CONTEXT_QUERY,
        RECAP_QUERY,
        SPATIAL_CLARIFICATION,
        PLANNING,
        CONTINUE,
        UNKNOWN
    }

    /** Classificador local e determinístico. Não depende do modelo remoto. */
    public static Intent classify(String raw){
        String original=human(raw);
        String n=plain(original);
        if(n.length()==0)return Intent.UNKNOWN;

        if(startsAny(n,"consulta auxiliar fora da ficcao","pergunta ao mestre fora da ficcao","duvida fora da aventura","painel do mestre"))return Intent.NARRATIVE_CONTEXT_QUERY;

        if(matchesAny(n,
                "continua","continue","pode continuar","pode seguir","segue","siga","prossiga","retoma","retome",
                "volta pra narrativa","volte pra narrativa","continua de onde parou","continue de onde parou"))return Intent.CONTINUE;

        if(startsAny(n,
                "eu pergunto ","pergunto ","eu digo ","digo ","eu falo ","falo ","eu respondo ","respondo ",
                "eu grito ","grito ","eu sussurro ","sussurro ","meu personagem pergunta ","meu personagem diz "))return Intent.DIEGETIC_SPEECH;

        if(startsAny(n,
                "eu vou ","vou ","eu faco ","faco ","eu tento ","tento ","eu entro ","entro ","eu saio ","saio ",
                "eu abro ","abro ","eu fecho ","fecho ","eu pego ","pego ","eu examino ","examino ","eu procuro ","procuro ",
                "eu ataco ","ataco ","eu sigo ","sigo ","eu espero ","esperamos ","eu caminho ","caminho ","eu corro ","corro ",
                "eu puxo ","puxo ","eu empurro ","empurro ","eu uso ","uso ","eu mostro ","mostro ","eu entrego ","entrego "))return Intent.DIEGETIC_ACTION;

        boolean question=isQuestion(original,n);
        if(question&&(containsAny(n,
                "regra","d&d","dnd","5e","mystara","rules cyclopedia","becmi","como funciona","como calcula",
                "modificador","rolagem","rolar","teste de","dano","classe de armadura","ca descendente","iniciativa",
                "pericia","perícia","vantagem","desvantagem","critico","crítico","falha critica","falha crítica",
                "jogada de protecao","jogada de proteção","salvaguarda","magia","concentracao","concentração",
                "bonus de proficiencia","bônus de proficiência")||containsToken(n,"ca","pv","cd","xp","thac0")))return Intent.RULE_QUERY;

        if(question&&containsAny(n,
                "a porta","essa porta","a janela","essa janela","a escada","essa escada","o corredor","esse corredor",
                "a esquerda","a direita","na frente","atras de","perto de","longe de","distancia","quantos metros","onde fica",
                "fica onde","daqui","consigo ver","linha de visao","quantos andares","em cima","embaixo","mapa","posicao"))return Intent.SPATIAL_CLARIFICATION;

        if(question&&(startsAny(n,
                "quem estava ","quem era ","qual era o nome","o que aconteceu","o que ja sabemos","o que sabemos","onde estamos",
                "lembra ","recapitula","recapitule","resume ","resuma ")||containsAny(n,"quem estava na sala","o que aconteceu ate aqui","o que sabemos ate agora")))return Intent.RECAP_QUERY;

        if(!question&&containsAny(n,
                "talvez seja melhor","acho que seria melhor","acho melhor","poderiamos ","a gente poderia ","e se a gente ","e se eu ",
                "seria melhor ","talvez eu ","estou pensando em ","meu plano e ","o plano seria "))return Intent.PLANNING;

        if(question)return Intent.NARRATIVE_CONTEXT_QUERY;
        return Intent.UNKNOWN;
    }

    public static boolean isNonAdvancing(String raw){return isNonAdvancing(classify(raw));}
    public static boolean isNonAdvancing(Intent intent){return intent==Intent.RULE_QUERY||intent==Intent.NARRATIVE_CONTEXT_QUERY||intent==Intent.RECAP_QUERY||intent==Intent.SPATIAL_CLARIFICATION||intent==Intent.PLANNING;}
    public static boolean isContextLike(Intent intent){return intent==Intent.NARRATIVE_CONTEXT_QUERY||intent==Intent.RECAP_QUERY||intent==Intent.SPATIAL_CLARIFICATION||intent==Intent.PLANNING;}
    public static boolean isAdvancing(Intent intent){return intent==Intent.DIEGETIC_ACTION||intent==Intent.DIEGETIC_SPEECH||intent==Intent.CONTINUE||intent==Intent.UNKNOWN;}

    public static String intentLabel(Intent intent){
        if(intent==null)return "desconhecido";
        switch(intent){
            case DIEGETIC_ACTION:return "ação do personagem";
            case DIEGETIC_SPEECH:return "fala do personagem";
            case RULE_QUERY:return "consulta de regra";
            case NARRATIVE_CONTEXT_QUERY:return "dúvida sobre o mundo/narração";
            case RECAP_QUERY:return "recapitulação";
            case SPATIAL_CLARIFICATION:return "clarificação espacial";
            case PLANNING:return "planejamento ainda não executado";
            case CONTINUE:return "continuação da cena";
            default:return "entrada não classificada";
        }
    }

    /**
     * Contrato estilístico unificado: percepção + progressão + estranhamento.
     * É propositalmente operacional, não uma coleção de adjetivos como "cinematográfico".
     */
    public static String contract(JSONObject campaignConfig,JSONObject scene,JSONArray recentNarrations){
        StringBuilder p=new StringBuilder();
        p.append(PathfinderNarrativeContract.prompt()).append("\n");
        String tone=campaignConfig==null?"":campaignConfig.optString("tone","");
        String rules=campaignConfig==null?"":campaignConfig.optString("rules_profile","");
        if(tone.length()+rules.length()>0)p.append("PERFIL DA CAMPANHA: regras=").append(rules).append("; tom=").append(tone).append(". Cenário e gênero alteram vocabulário, ritmo, perigos e conteúdo; nunca alteram agência, continuidade, informação ou percepção.\n");
        p.append("MODELO PROFISSIONAL DE NARRAÇÃO — use percepção ordenada, não exposição. Estrutura-base flexível: CONTEXTO → ESPAÇO → MOVIMENTO → FOCO → IMPLICAÇÃO → ABERTURA PARA AÇÃO. O jogador precisa formar mapa mental, perceber algo acontecendo e receber informação suficiente para decidir; quando isso ocorrer, pare.\n");
        p.append("CÂMERA E ESCALA: quando a cena merece apresentação, progrida PANORAMA → LOCAL → PESSOAS → DETALHE. Aproximações, descidas, corredores, portas, pontes e outros limiares devem ser sentidos pela mudança de luz, som, material, distância, altura, saídas e linhas de visão. Nunca descreva lugar como catálogo de móveis.\n");
        p.append("MUNDO EM MOVIMENTO: o cenário já existia antes da chegada dos heróis. Mostre trabalho, trânsito, conversas, rotinas, portas, objetos sendo manipulados e reações pequenas. NPC entra fazendo algo. Caracterize NPC por SILHUETA → um traço-assinatura → comportamento → identidade; não entregue biografia nem adjetivo psicológico quando um gesto demonstra melhor.\n");
        p.append("CAMADAS DE INFORMAÇÃO: separe PERCEPTÍVEL, OBSERVÁVEL, INFERÍVEL e OCULTO. Narração comum entrega o perceptível; observação pode revelar o observável; inferência pertence ao jogador; segredo do Mestre nunca vaza só porque você o conhece. Não determine medo, atração, tristeza, desconfiança ou decisão do personagem do jogador.\n");
        p.append("DESCOBERTA E INVESTIGAÇÃO: prefira SINAL → SINAL MAIS GRAVE → CONSEQUÊNCIA → NOVA PERGUNTA. Uma boa pista muda a hipótese, não apenas acrescenta decoração. Objetos são mostrados por estado, posição e relação com o ambiente antes de qualquer conclusão. Revelações fortes recompensam memória: fatos novos devem fazer detalhes anteriores ganhar significado.\n");
        p.append("TOPOLOGIA E CONTINUIDADE: preserve de onde vieram, para onde cada passagem leva, obstáculos, portas, altura, cobertura e mudanças físicas. Inventar → fixar → respeitar. Se uma sala gira, porta fecha, janela quebra ou caminho abre, o novo estado substitui o antigo. Corrija ambiguidade como refinamento perceptivo, nunca reinventando o espaço.\n");
        p.append("ESTRANHAMENTO: estabeleça normalidade antes da anomalia. Um motivo recorrente passa por curioso → possível coincidência → padrão → problema. Multidão pode agir como personagem por silêncio, uniformidade, rotina ou ausência de reação. Não diga que algo é assustador; mostre a discrepância. Negação do evidente, detalhe tardio e interrupção súbita de som funcionam melhor quando apoiados em contexto já estabelecido.\n");
        p.append("RITMO: comprima rotina e deslocamento sem evento; desacelere revelação, mecanismo, perigo, emoção e mudança espacial. Em impacto extremo, o corpo pode perceber antes da conclusão intelectual: impacto → sensação → orientação → compreensão. Em combate, uma imagem dominante por ação basta; preserve vetor ORIGEM → TRAJETÓRIA → CONTATO → RESULTADO e geografia da cena. Nem todo golpe é épico: erro, bloqueio, recuo, tropeço e contato parcial devem ter escala compatível com o resultado real.\n");
        p.append("ESCALADA: use ameaça → resposta → breve alívio → novo sinal → ameaça transformada. Falsa vitória só funciona se durar o bastante para ser acreditada. Retorno de ameaça precisa mudar direção, recurso, posição ou ambiente. Clímax combina escala do ambiente, escala da ameaça e escala humana concreta.\n");
        p.append("AGÊNCIA: apresente o quadro e devolva a vez. Não escreva a próxima decisão do jogador, não crie trilho para preservar arco e não continue além do ponto necessário. Uma pergunta do jogador sobre o mundo não é automaticamente fala ou ação do personagem.\n");
        p.append("ANTI-REPETIÇÃO: não reutilize abertura, metáfora, gesto, transição, clima, estrutura de parágrafos ou encerramento das cenas recentes. Evite começar repetidamente por 'vocês', 'o ar', 'o silêncio', 'a luz', 'de repente' ou 'enquanto'.\n");
        p.append("FUNÇÃO DA CENA: escolha uma função dominante entre chegada, ambientação, exploração, investigação, observação, descoberta, revelação, diálogo cotidiano, negociação, interrogatório, conflito social, intimidade, preparação, viagem, transição, suspense, horror, perseguição, combate, aftermath, alívio, consequência, presságio ou encerramento. Não tente fazer todas ao mesmo tempo.\n");
        p.append("PLANEJADOR DE BATIDAS: antes da prosa, organize silenciosamente 1 a 5 batidas. Chegada pode usar DISTÂNCIA → APROXIMAÇÃO → SINAL DE VIDA → DETALHE → ENTRADA. Investigação: NORMAL → IRREGULARIDADE → SEGUNDO INDÍCIO → RELAÇÃO POSSÍVEL → DECISÃO. Conversa: ATIVIDADE DO NPC → RECEPÇÃO → RESPOSTA → SUBTEXTO → REAÇÃO. Nunca exponha esse planejamento.\n");
        p.append("DETALHES QUE TRABALHAM: cada detalhe deve situar, caracterizar, antecipar, contrastar, informar, ameaçar ou permitir interação. Prefira 1 imagem espacial forte + 1 movimento + 1 detalhe significativo + 1 possibilidade implícita a inventário decorativo.\n");
        p.append("DIÁLOGO ENCENADO: NPC continua existindo enquanto fala: trabalha, manipula objetos, olha para alguém, interrompe, hesita ou muda de assunto conforme personalidade e interesses. NPC não é banco de dados. Para cada informação, diferencie internamente SABE, ACREDITA, SUSPEITA, OUVIU, INVENTOU, ESCONDE ou RECUSA. Se o jogador fizer várias perguntas na mesma fala, responda a todas que o NPC possa responder; silêncio e evasão também são respostas significativas.\n");
        p.append("INFERÊNCIA JUSTA: evidência + conhecimento real do personagem pode autorizar uma inferência; evidência sozinha não autoriza o narrador a explicar o mistério. Pistas importantes devem possuir mais de um caminho de confirmação quando possível; uma única rolagem não deve destruir uma investigação inteira.\n");        p.append("PERSPECTIVA DO PERSONAGEM: a mesma cena pode destacar coisas diferentes conforme classe, antecedentes, perícias, idiomas, sentidos e conhecimento realmente registrados na ficha. Um guerreiro pode notar formação e cobertura; um ladrão, fechaduras e rotas; um conjurador, sinais arcanos — somente quando a ficha sustentar isso. Não invente competência para personalizar a descrição.\n");
        p.append("GRAFO DE PISTAS: trate cada clue como evidência, não conclusão. Quando houver relação real, use status e relações supports/contradicts/requires internamente para permitir convergência independente. Mistério importante não deve depender de uma única pista ou uma única rolagem.\n");
        p.append("CONTRADIÇÃO DE NPC NÃO É ERRO AUTOMÁTICO: compare SABE/ACREDITA/SUSPEITA/OUVIU com memória, interesse e evidência. Uma contradição pode ser mentira, lapso, medo ou informação parcial; nunca a resolva em favor do narrador sem suporte.\n");
        p.append("INTERRUPÇÕES: só interrompa uma cena quando houver causa no mundo — pessoa chegando, relógio vencendo, patrulha, clima, adversário, compromisso, ruído já originado. Nunca use evento súbito só para fabricar drama.\n");        p.append("PRESSÃO SEM TRILHO: relógio, concorrente, fechamento de estabelecimento, patrulha, tempestade ou decisão de NPC podem pressionar a cena quando já existirem no mundo. Pressão altera custo e oportunidade; nunca serve para obrigar o grupo a aceitar uma missão ou preservar um roteiro.\n");
        p.append("CONTINUIDADE SENSORIAL: som, chuva, fogo, fumaça, iluminação, multidão, terreno e objetos ativos persistem até uma causa alterá-los. Se a forja estava martelando, o ruído continua relevante; se começou a chover, roupas, pegadas e visibilidade podem responder a isso. Não reinicie o ambiente a cada parágrafo.\n");
        p.append("INÉRCIA EMOCIONAL E SOCIAL: medo, raiva, vergonha, confiança, intimidade e desconfiança não voltam ao neutro entre respostas. Favores, humilhações, ameaças, promessas, mentiras descobertas e dívidas permanecem na relação e reaparecem somente quando organicamente pertinentes.\n");
        p.append("CONSEQUÊNCIA: nem toda consequência aparece imediatamente. Favores, insultos, dívidas, promessas, testemunhas, objetos deixados e decisões podem retornar depois quando causalmente pertinente. Falha pode produzir atraso, ruído, custo, exposição, informação parcial ou posição pior; não reduza tudo a 'você não consegue', salvo quando a regra realmente exigir isso.\n");
        p.append("TEMPERATURA NARRATIVA: varie de 0 rotina, 1 curiosidade, 2 inquietação, 3 tensão, 4 perigo a 5 clímax. Não permaneça em 4–5 por muitas batidas; use contraste e respiração. Uma cena tranquila pode ser genuinamente tranquila, sem presságio obrigatório.\n");
        p.append("FORMA DO TEXTO: varie tamanho de parágrafos conforme importância. Rotina pode caber em poucas frases; exploração usa espaço suficiente para mapa mental; descoberta desacelera; impacto pode receber uma frase isolada. Não termine toda resposta com pergunta. Pare no primeiro ponto em que existe decisão real.\n");
        p.append("FOCO DRAMÁTICO: cada resposta tem um centro. Escolha o elemento que deve permanecer na memória do jogador e não empilhe três revelações, dois NPCs novos, mudança de clima e combate na mesma batida sem causa. Prefira deixar uma pergunta na cabeça do jogador a escrevê-la explicitamente.\n");
        p.append("SUGESTÕES: quando options ajudar, cada opção deve ser diegética, concreta e ligada a pessoa, pista, objeto, risco ou lugar presente. Nunca use 'continuar', 'investigar mais', 'agir', 'ler a situação' ou equivalentes vagos. A caixa de ação livre continua soberana.\n");
        p.append("TTS INVISÍVEL: audio_plan pode incluir delivery=calmo|baixo|normal|urgente, intensity=0..5 e pause_profile=curto|normal|longo. Isso dirige a voz, não deve aparecer na narration. Não teatralize toda frase.\n");
        p.append("QA ANTES DE ENTREGAR: verifique silenciosamente se vazou conhecimento oculto, moveu algo sem causa, teletransportou NPC, ignorou pergunta, pediu teste trivial, explicou pista em vez de mostrá-la, repetiu forma recente, avançou uma consulta não diegética, roubou decisão do jogador ou continuou além do ponto natural de parada. Corrija dentro da mesma resposta.\n");        p.append("TRÊS MICROEXEMPLOS DE COMPORTAMENTO — absorva a técnica, NUNCA copie nomes, frases ou fatos: (1) CHEGADA: ‘Do alto da estrada, os telhados aparecem antes das vozes. No portão, dois guardas retiram barris de uma carroça enquanto o dono protesta. Uma mulher desacelera ao ver o mesmo símbolo pintado nos três barris e segue sem olhar para os guardas.’ Espaço + atividade + detalhe + implicação. (2) INVESTIGAÇÃO/DIÁLOGO: ‘A governanta recolhe papéis do chão enquanto responde. Diz que só ela e o dono possuem chave. Ao ouvir um nome específico, o polegar para sobre a dobra da folha; depois ela diz que nunca ouviu falar dele.’ Resposta + gesto observável, sem declarar mentira. (3) AÇÃO: ‘O hobgoblin corre para a coluna, usa a pedra para ganhar altura e passa por cima do escudo. A lâmina bate no metal; ele cai entre o grupo e a porta. Só então fica claro que o objetivo era alcançar a saída.’ ORIGEM → TRAJETÓRIA → CONTATO → RESULTADO + nova posição.\n");
        p.append("FUNÇÃO/TEMPERATURA SUGERIDAS PELO DIRETOR LOCAL: ").append(sceneFunction("",scene)).append(" / ").append(sceneTemperature(scene,"")).append(" de 5. Ajuste somente se a ação atual justificar.\n");
        p.append("LENTE DE DIREÇÃO DESTA BATIDA: ").append(sceneLens(campaignConfig,scene,recentNarrations)).append(". Use como variação de enquadramento, nunca para contradizer fatos.\n");
        if(scene!=null){String where=scene.optString("location","");String time=scene.optString("time_of_day","");String weather=scene.optString("weather","");if(where.length()+time.length()+weather.length()>0)p.append("ÂNCORA ATUAL: local=").append(where).append("; período=").append(time).append("; ambiente=").append(weather).append(".\n");}
        Set<String> starts=recentStarts(recentNarrations);if(!starts.isEmpty()){p.append("FORMAS RECENTES A EVITAR: ");int n=0;for(String s:starts){if(n++>0)p.append(" | ");p.append(s);if(n>=8)break;}p.append(".\n");}
        p.append("MEMÓRIA NARRATIVA OPCIONAL NO JSON: narrative_state={checkpoint_summary:'resumo exato do ponto de pausa',scene_function:'função dominante',temperature:0,focal_point:'centro da batida',motifs:[{key,observation,status}],clues:[{key,evidence,status,supports:[],contradicts:[],requires:[]}],topology:[{from,to,relation}],open_questions:[],live_table:{mode,pattern,spotlight,threads,convergence,shared_clock,scene_promises,capsule,faction_choices,dramatic_irony,guest_role}}. Registre apenas padrões/relacões realmente estabelecidos; não invente segredo para preencher campos.\n");
        return p.toString();
    }

    /** Regras específicas da mensagem atual e do checkpoint persistente. */
    public static String interactionContract(String userText,JSONObject runtimeMemory){
        Intent intent=classify(userText);
        StringBuilder p=new StringBuilder();
        p.append("ROTEADOR LOCAL DA MENSAGEM: ").append(intentLabel(intent)).append(". A classificação foi feita pelo aplicativo e tem prioridade sobre suposições do modelo.\n");
        JSONObject checkpoint=runtimeMemory==null?null:runtimeMemory.optJSONObject("checkpoint");
        if(checkpoint!=null&&checkpoint.length()>0){
            p.append("CHECKPOINT DA ÚLTIMA CENA QUE REALMENTE AVANÇOU: local=").append(checkpoint.optString("location"))
                    .append("; período=").append(checkpoint.optString("time_of_day"))
                    .append("; ponto de pausa=").append(tail(checkpoint.optString("narration"),1500)).append(".\n");
        }
        JSONArray clarifications=runtimeMemory==null?null:runtimeMemory.optJSONArray("clarifications");
        if(clarifications!=null&&clarifications.length()>0){p.append("CLARIFICAÇÕES CANÔNICAS RECENTES, que não fizeram o tempo avançar: ");int start=Math.max(0,clarifications.length()-8);for(int i=start;i<clarifications.length();i++){JSONObject c=clarifications.optJSONObject(i);if(c==null)continue;if(i>start)p.append(" | ");p.append(tail(c.optString("question"),120)).append(" => ").append(tail(c.optString("answer"),220));}p.append(".\n");}

        JSONObject dna=runtimeMemory==null?null:runtimeMemory.optJSONObject("style_dna");
        if(dna!=null&&dna.optInt("beats",0)>=4){p.append("DNA OBSERVADO DA CAMPANHA: média de ").append(dna.optInt("avg_narration_words",0)).append(" palavras por batida; entradas do jogador ~").append(dna.optInt("avg_player_words",0)).append(" palavras; funções mais usadas=").append(dna.optJSONObject("scene_functions")==null?"{}":dna.optJSONObject("scene_functions").toString()).append(". Use isso apenas como adaptação suave de ritmo; nunca force a campanha a repetir o próprio padrão.\n");}

        p.append(LiveTableNarrativeDirector.memoryPrompt(runtimeMemory));

        if(isNonAdvancing(intent)){
            p.append("REGRA DE CONGELAMENTO: esta mensagem NÃO é uma ação ficcional. Responda à dúvida/planejamento e permaneça exatamente no mesmo instante da cena. Não avance relógio, clima, NPC, ameaça, viagem, mundo vivo, combate, mapa ou fila de ações. Não introduza um acontecimento novo no fim da resposta. Não gere pending_test, options ou mutações persistentes.\n");
            if(intent==Intent.RULE_QUERY)p.append("CONSULTA DE REGRA: responda somente com regra confirmada pelo motor/trechos recuperados. A consulta não move a ficção.\n");
            p.append("PAINEL AUXILIAR: esta resposta será exibida fora da caixa de narração. Responda diretamente cada pergunta feita, em itens separados quando houver mais de uma. Não encene a pergunta como fala do personagem, não continue acontecimentos, não acrescente clima, suspense, diálogo ou gancho. Para posição/distância, use somente o estado espacial disponível e declare quando a medida exata não está estabelecida. Para perícias, apresente possibilidades opcionais; não peça rolagem sem INCERTEZA + CONSEQUÊNCIA + COMPETÊNCIA RELEVANTE.\n");
            if(intent==Intent.SPATIAL_CLARIFICATION)p.append("CLARIFICAÇÃO ESPACIAL: complete apenas a geometria que já era compatível com a cena. Seja direto sobre posição, distância, acesso e linha de visão; não crie obstáculo ou oportunidade só para tornar a resposta interessante.\n");
            if(intent==Intent.RECAP_QUERY)p.append("RECAPITULAÇÃO: recupere somente fatos conhecidos pelo grupo até agora e diferencie hipótese de confirmação. Não avance a cena ao terminar.\n");
            if(intent==Intent.NARRATIVE_CONTEXT_QUERY)p.append("DÚVIDA DE MUNDO: responda como Mestre explicando o cenário que está sendo criado. Pode fixar um detalhe contextual coerente ainda não definido (lei local, costume, porte, arquitetura, reputação pública), mas isso é CLARIFICAÇÃO DE MUNDO, não passagem de tempo nem ação do personagem. Nunca revele segredo oculto porque a pergunta foi direta.\n");
            if(intent==Intent.PLANNING)p.append("PLANEJAMENTO: trate como intenção hipotética. Nada acontece até o jogador usar linguagem de execução inequívoca.\n");
        }else if(intent==Intent.CONTINUE){
            p.append("CONTINUAÇÃO: retome do checkpoint sem recapitulá-lo como nova abertura. Continue a mesma batida a partir do ponto de pausa, preservando espaço, elenco, tom e consequências já estabelecidos.\n");
        }else{
            p.append("MENSAGEM POTENCIALMENTE DIEGÉTICA: somente consequências explicitamente causadas pela ação/fala do personagem podem avançar a cena. Não transforme uma pergunta incidental do jogador em fala do personagem.\n");
        }
        return p.toString();
    }

    /**
     * Defesa local: mesmo que o modelo tente mover o mundo em uma consulta,
     * o envelope chega ao restante do app sem qualquer mutação.
     */
    public static JSONObject freezeEnvelope(JSONObject envelope,String userText){
        if(envelope==null)envelope=new JSONObject();
        Intent intent=classify(userText);
        if(!isNonAdvancing(intent))return envelope;
        try{
            String[] arrays={"state_updates","npc_updates","npc_cognition_updates","knowledge_updates","quest_updates","faction_updates","guild_updates","clock_updates","world_events","event_resolutions","pending_resolutions","map_updates","tactical_updates","journal_updates"};
            for(String key:arrays)envelope.put(key,new JSONArray());
            envelope.put("pending_test",JSONObject.NULL);
            envelope.put("options",new JSONArray());
            envelope.put("scene_state",new JSONObject());
            envelope.put("combat",new JSONObject().put("action","none").put("enemies",new JSONArray()).put("updates",new JSONArray()).put("reason","consulta não diegética"));
            envelope.put("visual_prompt","");
            envelope.put("_local_tick",new JSONObject());
            envelope.put("_npc_tick",new JSONObject());
            envelope.put("_non_advancing",true);
            envelope.put("_narrative_intent",intent.name());
        }catch(Exception ignored){/* JSONObject em memória; falha aqui apenas degrada a defesa adicional. */}
        return envelope;
    }

    /** Memória de checkpoint, topologia e motivos após uma batida que avançou a ficção. */
    public static JSONObject recordAdvancingMemory(JSONObject current,String action,String narration,JSONObject envelope,JSONObject scene){
        JSONObject out=copyObject(current);
        try{
            JSONObject cp=new JSONObject();cp.put("action",human(action));cp.put("narration",tail(human(narration),4200));cp.put("location",scene==null?"":scene.optString("location"));cp.put("hex_id",scene==null?"":scene.optString("hex_id"));cp.put("time_of_day",scene==null?"":scene.optString("time_of_day"));cp.put("weather",scene==null?"":scene.optString("weather"));cp.put("updated_at",System.currentTimeMillis());out.put("checkpoint",cp);
            JSONObject ns=envelope==null?null:envelope.optJSONObject("narrative_state");
            if(ns!=null){mergeObjectsByKey(out,"motifs",ns.optJSONArray("motifs"),"key",48);mergeObjectsByKey(out,"clues",ns.optJSONArray("clues"),"key",64);mergeTopology(out,ns.optJSONArray("topology"),80);JSONArray q=ns.optJSONArray("open_questions");if(q!=null)out.put("open_questions",trimArray(q,24));String summary=human(ns.optString("checkpoint_summary"));if(summary.length()>0)cp.put("summary",tail(summary,900));cp.put("scene_function",human(ns.optString("scene_function",sceneFunction(action,scene))));cp.put("temperature",Math.max(0,Math.min(5,ns.optInt("temperature",sceneTemperature(scene,action)))));cp.put("focal_point",tail(human(ns.optString("focal_point")),320));}
            updateStyleDna(out,action,narration,scene);out=LiveTableNarrativeDirector.record(out,action,narration,envelope,scene);out.put("last_intent",classify(action).name());out.put("updated_at",System.currentTimeMillis());
        }catch(Exception ignored){/* mantém o checkpoint mínimo */}
        return out;
    }

    /** Registra respostas de contexto como fatos de continuidade, sem avançar cena. */
    public static JSONObject recordClarification(JSONObject current,String question,String answer){
        JSONObject out=copyObject(current);
        try{
            JSONArray old=out.optJSONArray("clarifications");JSONArray next=new JSONArray();if(old!=null)for(int i=Math.max(0,old.length()-23);i<old.length();i++)next.put(old.opt(i));
            next.put(new JSONObject().put("question",tail(human(question),500)).put("answer",tail(human(answer),1200)).put("intent",classify(question).name()).put("created_at",System.currentTimeMillis()));
            out.put("clarifications",next);out.put("last_intent",classify(question).name());out.put("updated_at",System.currentTimeMillis());
        }catch(Exception ignored){AppLog.ignored("BraseiroNarrativeDirector","Falha tolerada ao registrar clarificação narrativa.",ignored);}
        return out;
    }

    /** Qualidade anti-repetição para cenas que realmente são cenas. */
    public static String qualityProblem(String narration,JSONArray recent){return qualityProblem(narration,recent,"");}
    public static String qualityProblem(String narration,JSONArray recent,String userText){
        Intent intent=classify(userText);String now=human(narration);
        if(isNonAdvancing(intent)){
            if(now.length()<8)return "resposta de esclarecimento vazia ou curta demais";
            String lower=plain(now);if(lower.matches(".*(enquanto isso|nesse momento|de repente).*")&&now.length()>160)return "consulta de contexto tentou continuar a cena";
            return "";
        }
        if(now.length()<40)return "cena curta demais para direção Braseiro";
        String nowStart=opening(now);Set<String> nowTokens=tokens(nowStart);
        if(recent!=null)for(int i=0;i<recent.length();i++){String old=human(String.valueOf(recent.opt(i)));if(old.length()==0)continue;String oldStart=opening(old);
            if(nowStart.equalsIgnoreCase(oldStart))return "repete literalmente a abertura de uma cena recente";
            double sim=jaccard(nowTokens,tokens(oldStart));if(sim>=0.72d&&Math.min(nowStart.length(),oldStart.length())>=45)return "repete a construção e o vocabulário de uma abertura recente";
        }
        Set<String> nowBody=tokens(now.length()>900?now.substring(0,900):now);
        if(recent!=null)for(int i=Math.max(0,recent.length()-8);i<recent.length();i++){String old=human(String.valueOf(recent.opt(i)));if(old.length()<120)continue;Set<String> oldBody=tokens(old.length()>900?old.substring(0,900):old);double bodySim=jaccard(nowBody,oldBody);if(bodySim>=0.58d&&Math.min(nowBody.size(),oldBody.size())>=24)return "recicla vocabulário e estrutura demais de uma cena recente";}
        String lower=now.toLowerCase(Locale.ROOT);String[] weak={"de repente","o ar parecia","o silêncio era","a luz filtrava","vocês percebem que","vocês veem que"};int weakHits=0;for(String w:weak)if(lower.contains(w))weakHits++;if(weakHits>=3)return "acumula clichês narrativos recorrentes em vez de encenar a situação";
        String tableProblem=LiveTableNarrativeDirector.qualityProblem(now,null,null,userText);if(tableProblem.length()>0)return tableProblem;
        return "";
    }

    private static void updateStyleDna(JSONObject root,String action,String narration,JSONObject scene)throws Exception{
        JSONObject dna=root.optJSONObject("style_dna");if(dna==null)dna=new JSONObject();int beats=Math.max(0,dna.optInt("beats",0));int nWords=wordCount(narration),pWords=wordCount(action);int nextBeats=Math.min(100000,beats+1);int avgNarr=beats==0?nWords:(int)Math.round((dna.optInt("avg_narration_words",nWords)*beats+nWords)/(double)nextBeats);int avgPlayer=beats==0?pWords:(int)Math.round((dna.optInt("avg_player_words",pWords)*beats+pWords)/(double)nextBeats);dna.put("beats",nextBeats).put("avg_narration_words",avgNarr).put("avg_player_words",avgPlayer);
        String fn=sceneFunction(action,scene);JSONObject counts=dna.optJSONObject("scene_functions");if(counts==null)counts=new JSONObject();counts.put(fn,counts.optInt(fn,0)+1);dna.put("scene_functions",counts);root.put("style_dna",dna);
    }

    private static int wordCount(String value){String h=human(value);return h.length()==0?0:h.split("\\s+").length;}

    public static String sceneFunction(String action,JSONObject scene){
        String n=plain(action);String mode=scene==null?"":plain(scene.optString("mode",scene.optString("scene_mode","")));
        if(containsAny(n,"interrogo","interrogar","pressiono","pressionar"))return "interrogatório";
        if(containsAny(n,"pergunto","digo","falo","converso","negocio","negociar"))return containsAny(n,"preco","preço","acordo","contrato")?"negociação":"diálogo";
        if(containsAny(n,"examino","procuro","investigo","pista","rastro","documento"))return "investigação";
        if(containsAny(n,"ataco","combate","golpe","disparo","magia contra" )||mode.contains("combate"))return "combate";
        if(containsAny(n,"viajo","caminho","estrada","rota","partimos")||mode.contains("viagem"))return "viagem";
        if(containsAny(n,"entro","chego","aproximo","atravesso"))return "chegada";
        if(containsAny(n,"fujo","persigo","corro atras","corro atrás"))return "perseguição";
        if(mode.contains("conversa"))return "diálogo";
        return "exploração";
    }

    public static int sceneTemperature(JSONObject scene,String action){
        String n=plain(action);String danger=scene==null?"":plain(scene.optString("danger",""));String tension=scene==null?"":plain(scene.optString("tension",""));
        if(containsAny(n,"combate","ataco","matar","fugir","explosao","explosão")||containsAny(danger,"mortal","ataque","combate","imediato"))return 4;
        if(containsAny(n,"ameaca","ameaça","interrogo","persigo")||tension.length()>0)return 3;
        if(containsAny(n,"investigo","examino","suspeito","pista"))return 2;
        if(containsAny(n,"chego","entro","observo","converso"))return 1;
        return 0;
    }

    private static String sceneLens(JSONObject campaignConfig,JSONObject scene,JSONArray recent){
        String[] lenses={
            "comece por uma ação já em andamento e revele o espaço por suas consequências",
            "abra em plano geral espacial e aproxime para um detalhe material que muda a decisão",
            "faça um som localizado chegar antes de sua fonte e revele-a pelo deslocamento",
            "comece pela conduta concreta de um NPC — trabalho, hábito, hesitação ou urgência — antes do diálogo",
            "encene primeiro a consequência física ou social da última escolha; contexto vem depois",
            "organize a cena por linhas de visão, distâncias, portas, saídas e corpos em movimento",
            "use um objeto funcional como eixo: quem o manipula, quem o deseja e o que ele permite ou impede",
            "mostre uma rotina normal e introduza uma discrepância pequena, verificável e ainda não explicada",
            "comece no meio de uma conversa, tarefa ou procedimento que existia antes da chegada dos heróis",
            "construa descoberta como vestígio → vestígio mais grave → consequência → nova pergunta",
            "apresente uma aproximação em estágios; deixe o destino ganhar forma antes de ser alcançado",
            "faça pressão de tempo aparecer por compromissos e movimentos do mundo, não por abstração",
            "apresente primeiro o custo humano/social da situação e só então a causa maior",
            "conduza a câmera pelo trajeto real que o personagem precisa percorrer para alcançar o foco",
            "abra com uma ausência concreta — algo que deveria estar ali e não está — sem explicar a causa",
            "use uma transição curta e mostre o que mudou e o que permaneceu estranhamente igual"
        };
        int n=recent==null?0:recent.length();String type=campaignConfig==null?"":campaignConfig.optString("adventure_type","");String loc=scene==null?"":scene.optString("location","");int seed=Math.abs((type+"|"+loc).hashCode());int idx=(seed+n)%lenses.length;return lenses[idx];
    }

    private static boolean isQuestion(String original,String n){if(original.contains("?"))return true;return startsAny(n,"quem ","qual ","quais ","quanto ","quantos ","quando ","onde ","como ","por que ","porque ","essa ","esse ","esta ","este ","tem ","e permitido ","pode ","da pra ","consigo ","eu sei ");}
    private static boolean startsAny(String n,String... values){for(String v:values)if(n.startsWith(v))return true;return false;}
    private static boolean containsAny(String n,String... values){for(String v:values)if(n.contains(v))return true;return false;}
    private static boolean containsToken(String n,String... values){String padded=" "+n.replaceAll("[^a-z0-9]+"," ")+" ";for(String v:values)if(padded.contains(" "+v+" "))return true;return false;}
    private static boolean matchesAny(String n,String... values){for(String v:values)if(n.equals(v))return true;return false;}

    private static String opening(String text){String x=human(text);String[] sentences=x.split("(?<=[.!?…])\\s+",3);String first=sentences.length==0?x:sentences[0];if(first.length()<85&&sentences.length>1)first=first+" "+sentences[1];return first.length()>180?first.substring(0,180):first;}
    private static Set<String> tokens(String text){Set<String> out=new LinkedHashSet<String>();String n=plain(text).replaceAll("[^a-z0-9 ]+"," ");for(String t:n.split("\\s+")){if(t.length()<4)continue;if("para|como|mais|uma|uns|umas|essa|esse|isso|aquela|aquele|onde|quando|entao|sobre|entre|pelos|pela|pelo|voces".contains(t))continue;out.add(t);}return out;}
    private static double jaccard(Set<String>a,Set<String>b){if(a.isEmpty()||b.isEmpty())return 0d;int inter=0;for(String x:a)if(b.contains(x))inter++;int union=a.size()+b.size()-inter;return union<=0?0d:(double)inter/(double)union;}
    private static Set<String> recentStarts(JSONArray recent){Set<String> out=new LinkedHashSet<String>();if(recent==null)return out;for(int i=Math.max(0,recent.length()-12);i<recent.length();i++){String raw=String.valueOf(recent.opt(i));String clean=human(raw);if(clean.length()==0)continue;String[] sentences=clean.split("(?<=[.!?])\\s+",2);String first=sentences[0];if(first.length()>90)first=first.substring(0,90);out.add(first);}return out;}
    private static String human(String s){if(s==null)return "";return Normalizer.normalize(s,Normalizer.Form.NFC).replaceAll("\\s+"," ").trim();}
    private static String plain(String s){String x=Normalizer.normalize(human(s).toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("\\s+"," ").trim();}
    private static String tail(String value,int max){String s=value==null?"":value;if(s.length()<=max)return s;return "…"+s.substring(s.length()-Math.max(1,max-1));}
    private static JSONObject copyObject(JSONObject src){try{return new JSONObject(src==null?"{}":src.toString());}catch(Exception e){return new JSONObject();}}
    private static JSONArray trimArray(JSONArray src,int max){JSONArray out=new JSONArray();if(src==null)return out;for(int i=Math.max(0,src.length()-max);i<src.length();i++)out.put(src.opt(i));return out;}

    private static void mergeObjectsByKey(JSONObject root,String field,JSONArray incoming,String keyField,int max)throws Exception{
        LinkedHashMap<String,JSONObject> map=new LinkedHashMap<String,JSONObject>();JSONArray old=root.optJSONArray(field);if(old!=null)for(int i=0;i<old.length();i++){JSONObject x=old.optJSONObject(i);if(x==null)continue;String key=human(x.optString(keyField));if(key.length()>0)map.put(key,x);}if(incoming!=null)for(int i=0;i<incoming.length();i++){JSONObject x=incoming.optJSONObject(i);if(x==null)continue;String key=human(x.optString(keyField));if(key.length()==0)continue;map.put(key,new JSONObject(x.toString()));}
        while(map.size()>max){String first=map.keySet().iterator().next();map.remove(first);}JSONArray out=new JSONArray();for(JSONObject x:map.values())out.put(x);root.put(field,out);
    }
    private static void mergeTopology(JSONObject root,JSONArray incoming,int max)throws Exception{
        LinkedHashMap<String,JSONObject> map=new LinkedHashMap<String,JSONObject>();JSONArray old=root.optJSONArray("topology");if(old!=null)for(int i=0;i<old.length();i++){JSONObject x=old.optJSONObject(i);if(x==null)continue;String key=plain(x.optString("from")+"|"+x.optString("to")+"|"+x.optString("relation"));if(key.length()>2)map.put(key,x);}if(incoming!=null)for(int i=0;i<incoming.length();i++){JSONObject x=incoming.optJSONObject(i);if(x==null)continue;String key=plain(x.optString("from")+"|"+x.optString("to")+"|"+x.optString("relation"));if(key.length()>2)map.put(key,new JSONObject(x.toString()));}while(map.size()>max){String first=map.keySet().iterator().next();map.remove(first);}JSONArray out=new JSONArray();for(Map.Entry<String,JSONObject> e:map.entrySet())out.put(e.getValue());root.put("topology",out);
    }
}
