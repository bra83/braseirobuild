package com.braseiro.dnd5e;

import org.json.JSONObject;

/**
 * One high-priority conditional directive, applied in the actual generation
 * path rather than as a second generic Master persona or an isolated demo.
 */
public final class FictionTurnDirective {
    private FictionTurnDirective(){}
    public static String forTurn(String playerText,JSONObject scene,String channel){
        JSONObject c=FictionTurnIntent.contract(playerText,scene,channel);
        FictionTurnIntent.Kind kind=FictionTurnIntent.Kind.valueOf(
            c.optString("kind",FictionTurnIntent.Kind.CHARACTER_ACTION.name()));
        String rule="HIERARQUIA VINCULANTE: fatos canônicos e motor de regras > canal e intenção > "+
            "conhecimento e visibilidade do observador > ocasião e objetivo atual > estilo. "+
            "O banco de dados de NPCs não prova conhecimento do protagonista. "+
            "Não invente armas, uniforme, identidade, porta, mapa, teste, sucesso ou segredo. "+
            "Não decida falas, emoções ou intenções do protagonista. ";
        switch(kind){
        case RULES_HELP:
            return rule+"PERGUNTA NORMATIVA NO CAMPO PRINCIPAL: responda à regra com "+
                "fonte recuperada quando disponível. Não avance a cena, relógio, clima, "+
                "NPCs, missão, combate ou ação. Não redirecione para outro painel.";
        case AMBIGUOUS_REFERENCE:
            return rule+"REFERENTE AMBÍGUO: faça UMA pergunta situada para identificar "+
                "a pessoa ou objeto. Não escolha um NPC arbitrariamente nem avance a ficção.";
        case FICTION_OBSERVATION:
            return rule+NpcPerceptionLedger.promptPolicy()+
                "PERGUNTA PERCEPTIVA DENTRO DA FICÇÃO: responda PRIMEIRO ao que o "+
                "personagem pode perceber NESTE instante. Narre na segunda pessoa, "+
                "com brevidade e tato; não se dirija ao leitor como ajuda fora do jogo. "+
                "Hora exata só com instrumento; clima externo só se visível. "+
                "Distinguir 'não vejo arma' de 'está desarmado'. O corpo e roupas de "+
                "NPCs devem manter descrições já aceitas. Uma pergunta simples não "+
                "consome minutos, ação, recurso, turno de combate nem gera cena nova. "+
                "Não reintroduza o panorama, não regere quatro escolhas, não execute "+
                "a iniciativa de NPC e não reaplique estado. Responda e devolva a vez.";
        case MIXED_CONDITIONAL:
            String condition=c.optString("condition","UNKNOWN");
            return rule+"PERGUNTA COM AÇÃO CONDICIONAL: processe em ORDEM, jamais "+
                "em paralelo. 1) informe o clima perceptível do estado canônico; "+
                "2) só execute a busca de abrigo SE a condição for verdadeira e "+
                "um caminho viável estiver confirmado. ESTADO DA CONDIÇÃO="+condition+
                ". Se FALSE ou UNKNOWN, não tente a ação condicional, não avance "+
                "o relógio ou a cena. Não narre sucesso sem motor, nem invente abrigo.";
        case CHARACTER_SPEECH:
            return rule+"DIÁLOGO: o NPC que está PRESENTE e é o interlocutor "+
                "responde em discurso direto, conforme seus objetivos, memórias "+
                "e o que realmente sabe. Na primeira aparição situe brevemente "+
                "rosto/postura/atividade; nos retornos não reapresente tudo. "+
                "Não ponha uma segunda fala na boca do protagonista. Sem resposta "+
                "verificável, descreva silêncio, dúvida ou recusa plausível.";
        case CHARACTER_ACTION:
        default:
            return rule+"AÇÃO DECLARADA: avance apenas consequências autorizadas "+
                "pela mecânica. Quando o jogador pedir teste, proponha procedimento "+
                "e aguarde rolagem em vez de anunciar sucesso. Descreva mudança, "+
                "reação e oportunidade concreta; não reinicie a cena nem use "+
                "um mínimo artificial de palavras. Quatro opções (3 válidas "+
                "mais 1 iniciativa) só depois de cena aceita e jogável.";
        }
    }
}
