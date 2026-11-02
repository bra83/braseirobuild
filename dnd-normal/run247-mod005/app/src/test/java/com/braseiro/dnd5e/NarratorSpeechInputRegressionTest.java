package com.braseiro.dnd5e;

import android.app.Activity;
import android.widget.EditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/** Restrito ao campo de ação: ditado nunca executa a ação automaticamente. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class NarratorSpeechInputRegressionTest {
    @Test public void speechAppendsWithoutErasingTheExistingDraft(){
        assertEquals("Procuro um folheto de teatro",
                MainActivity.mergeDictatedText("Procuro","um folheto de teatro"));
        assertEquals("Pergunto ao Mestre",
                MainActivity.mergeDictatedText("Pergunto ","ao Mestre"));
        assertEquals("Texto original",
                MainActivity.mergeDictatedText("Texto original","  "));
    }

    @Test public void speechInsertionRespectsTheCaretAndLeavesTheTextEditable(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        EditText field=new EditText(activity);
        field.setText("Procuro panfleto");
        field.setSelection(8);
        MainActivity.insertDictatedText(field,"um");
        assertEquals("Procuro um panfleto",field.getText().toString());
        field.setSelection(field.length());
        MainActivity.insertDictatedText(field,"sobre teatro");
        assertEquals("Procuro um panfleto sobre teatro",field.getText().toString());
        assertEquals(field.length(),field.getSelectionStart());
    }

    @Test public void emptyResultsDoNotModifyThePlayerText(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        EditText field=new EditText(activity);
        field.setText("Eu examino a porta");
        field.setSelection(field.length());
        MainActivity.insertDictatedText(field," ");
        assertEquals("Eu examino a porta",field.getText().toString());
    }
}
