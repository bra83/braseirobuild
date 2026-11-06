package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression protection for user-reported four identical paid Gemini turns. */
public final class SessionTurnQualityRegressionTest {
    @Test public void paidGeminiResponseCannotRepeatLastVisibleScene() {
        String previous="O salão da biblioteca está repleto de estudantes. "
            +"Uma bibliotecária organiza fichas no balcão enquanto dois calouros discutem "
            +"a apresentação de teatro. Ao fundo, uma porta de madeira permanece fechada. "
            +"Kvothe está próximo do balcão e observa a movimentação.";
        assertTrue(BraseiroNarrativeDirector.repeatedTurnProblem(previous,previous)
                .contains("identica"));
        String recycled="O salão da biblioteca está repleto de estudantes. "
            +"Uma bibliotecária organiza fichas no balcão enquanto dois calouros discutem "
            +"a apresentação de teatro. Ao fundo, uma porta de madeira permanece fechada. "
            +"Depois alguém move uma cadeira.";
        assertTrue(BraseiroNarrativeDirector.repeatedTurnProblem(recycled,previous)
                .contains("reinicio"));
    }

    @Test public void directNpcAnswerMustBeAllowedAsARealNewTurn() {
        String old="A bibliotecária separa fichas, arruma livros e aponta a porta da galeria "
            +"ao lado da escadaria. Duas estudantes se afastam do balcão.";
        String answer="Bibliotecária: — O registro do teatro fica no segundo corredor. "
            +"Mas o caderno das apresentações canceladas está guardado comigo. "
            +"Ela fecha a gaveta e espera que Kvothe explique sua procura.";
        assertEquals("",BraseiroNarrativeDirector.repeatedTurnProblem(answer,old));
    }

    @Test public void masterProfileIsDeliveredAsActionSpecificDirection() {
        String turn=HumanMasterNarrativeProfile.turnInstruction(
                "Pergunto à bibliotecária sobre a peça cancelada.",true);
        assertTrue(turn.contains("MESA AO VIVO"));
        assertTrue(turn.contains("AÇÃO DECLARADA PELO JOGADOR"));
        assertTrue(turn.contains("responder DIRETAMENTE"));
        assertTrue(turn.contains("não complete a lista com filler"));
        String help=HumanMasterNarrativeProfile.turnInstruction("Como funciona o teste?",false);
        assertTrue(help.contains("sem avançar relógio"));
    }

    @Test public void shortDistinctRepliesAreNeverMistakenForLargeSceneReplays() {
        assertEquals("",BraseiroNarrativeDirector.repeatedTurnProblem(
                "— Não. — A bibliotecária fecha o livro.", 
                "— Sim. — A bibliotecária abre o livro."));
    }
}
