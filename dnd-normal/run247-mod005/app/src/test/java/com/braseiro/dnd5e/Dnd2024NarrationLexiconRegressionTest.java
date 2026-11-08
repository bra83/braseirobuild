package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

public final class Dnd2024NarrationLexiconRegressionTest {
    @Test public void fixesMisnamedSpeciesBeforeTextToSpeech(){
        assertEquals("Um orc conversou com uma gnoma.",
            Dnd2024NarrationLexicon.correct("Um orco conversou com uma gnomia."));
        assertEquals("Os orcs e as gnomas retornaram.",
            Dnd2024NarrationLexicon.correct("Os orcos e as gnomias retornaram."));
        assertEquals("Orc! Gnoma! ORCS, GNOMAS.",
            Dnd2024NarrationLexicon.correct("Orco! Gnomia! ORCOS, GNOMIAS."));
        assertEquals("Uma gnoma e outra gnoma.",
            Dnd2024NarrationLexicon.correct("Uma gnômia e outra gnomia."));
    }

    @Test public void keepsAlreadyCorrectWordsAndUnrelatedWordsIntact(){
        String original="Um Orc e uma gnoma falavam Gnômico e Gnômica ao lado de uma orca. "
            +"A tecnologia gnomônica e o sobrenome Orcote pertencem ao contexto.";
        assertEquals(original,Dnd2024NarrationLexicon.correct(original));
        assertEquals("",Dnd2024NarrationLexicon.correct(null));
    }

    @Test public void repeatedPassesNeverChangeTheResult(){
        String text="A Gnomia viu dois orcos; outro orco falou com uma gnômia.";
        String once=Dnd2024NarrationLexicon.correct(text);
        assertEquals(once,Dnd2024NarrationLexicon.correct(once));
    }
}
