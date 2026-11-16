package com.braseiro.dnd5e;
import org.junit.Test;
import static org.junit.Assert.*;

/** Handoff 2026-11-15: input field never silently changes channel. */
public final class NarratorConsultationRoutingRegressionTest {
    @Test public void queriesStayInMainSceneWithoutAutoRedirect(){
        for(String question:new String[]{
            "Como está o tempo?","Como essa pessoa é?","Está armado?",
            "Consigo ler a inscrição?","Onde está o Biblioplex?",
            "Faço algum teste pra tentar decifrar esse enigma?",
            "Tem algum teste que eu possa fazer para descobrir o local?",
            "Qual regra determina a Percepção passiva?",
            "Quais perícias posso usar?","Qual teste"
        })assertFalse(question,MainActivity.shouldRouteNarratorQuestionToMaster(question));
    }
    @Test public void declaredActionsAndSystemPromotionsRemainActions(){
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster("Observo os estudantes e aguardo a resposta."));
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster("Caminho até a mesa e examino o pergaminho."));
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster(
            "O jogador confirma que quer TENTAR na ficção a possibilidade discutida na consulta anterior."));
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster(
            "CONSULTA AUXILIAR FORA DA FICÇÃO. PERGUNTA DO JOGADOR: Posso testar Arcana?"));
    }
}
