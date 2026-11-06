package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Questions cannot be silently promoted into actions with presumed success. */
public final class NarratorConsultationRoutingRegressionTest {
    @Test public void questionsAreConsultationsNotResolvedActions(){
        assertTrue(MainActivity.shouldRouteNarratorQuestionToMaster("Faço algum teste pra tentar decifrar esse enigma?"));
        assertTrue(MainActivity.shouldRouteNarratorQuestionToMaster("Tem algum teste que eu possa fazer para descobrir o local?"));
        assertTrue(MainActivity.shouldRouteNarratorQuestionToMaster("Quais perícias posso usar?"));
        assertTrue(MainActivity.shouldRouteNarratorQuestionToMaster("Consigo ler a inscrição?"));
        assertTrue(MainActivity.shouldRouteNarratorQuestionToMaster("Qual teste"));
        assertTrue(MainActivity.shouldRouteNarratorQuestionToMaster("Onde está o Biblioplex"));
    }
    @Test public void explicitDeclaredActionsAndSystemPromotionsRemainActions(){
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster("Observo os estudantes e aguardo a resposta."));
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster("Caminho até a mesa e examino o pergaminho."));
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster(
            "O jogador confirma que quer TENTAR na ficção a possibilidade discutida na consulta anterior."));
        assertFalse(MainActivity.shouldRouteNarratorQuestionToMaster(
            "CONSULTA AUXILIAR FORA DA FICÇÃO. PERGUNTA DO JOGADOR: Posso testar Arcana?"));
    }
}
