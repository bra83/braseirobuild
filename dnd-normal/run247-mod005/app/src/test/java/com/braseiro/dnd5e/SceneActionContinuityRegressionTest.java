package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression gates for the Galaxy A57 Biblioplex report (2026-11-01). */
public final class SceneActionContinuityRegressionTest {
    @Test public void morningAmbienceNeverResetsTheCampaignClock() {
        assertTrue(Double.isNaN(SceneTemporalResolver.inferExplicitHour(
                "O ar da manhã no Biblioplex carrega aromas de papel antigo.")));
        SceneTemporalResolver.Result time=SceneTemporalResolver.resolve(
                "Manhã",9.25d,true,"Manhã",0d,false,
                "A luz da manhã recai sobre os pergaminhos.","Luz natural da manhã");
        assertEquals(9.25d,time.hour,0.0001d);
        assertFalse(time.narrationWasAuthoritative);
    }

    @Test public void actualClockReadingRemainsExplicitAndAuthoritative() {
        assertEquals(10.5d,SceneTemporalResolver.inferExplicitHour(
                "O relógio toca. Agora são 10:30."),0.0001d);
        assertEquals(14.75d,SceneTemporalResolver.inferExplicitHour(
                "O relógio marca 14:45."),0.0001d);
    }

    @Test public void actionDurationIsIndependentFromTtsAndNetwork() {
        assertEquals(3d,SceneActionClock.minutesFor("Procuro um panfleto de teatro"),0.0001d);
        assertEquals(5d,SceneActionClock.minutesFor("Converso com a funcionária"),0.0001d);
        assertEquals(30d,SceneActionClock.minutesFor("Aguardo 30 minutos"),0.0001d);
        assertEquals(0d,SceneActionClock.minutesFor(
                "INÍCIO AUTOMÁTICO — PRIMEIRA CENA DO SEMESTRE"),0.0001d);
        assertEquals(0d,SceneActionClock.minutesFor("Viajo até outra cidade"),0.0001d);
    }

    @Test public void narratorProfileResolvesActionBeforeRepeatingScenery() {
        String profile=HumanMasterNarrativeProfile.systemInstruction();
        assertTrue(profile.contains("jamais reenumere roupa e carga"));
        assertTrue(profile.contains("comece diretamente no objeto"));
        String context=BraseiroNarrativeDirector.interactionContract(
                "Procuro um panfleto sobre teatro",new JSONObject(),"PLAYER_ACTION");
        assertTrue(context.contains("EXECUÇÃO IMEDIATA"));
    }

    @Test public void actionMustBeAddressedAndRepeatedFullSentenceIsRejected() {
        String noAnswer="O grande Biblioplex recebe os recém-chegados em um salão decorado, "
            +"com as colunas de mármore que refletem a luz dourada e o som dos passos no chão.";
        assertTrue(BraseiroNarrativeDirector.qualityProblem(noAnswer,new JSONArray(),
                "Procuro um panfleto sobre teatro","PLAYER_ACTION").contains("panfleto"));
        String previous="O ar da manhã no Biblioplex carrega o aroma denso de pergaminho antigo, "
            +"cera de abelha e o calor difuso de encantamentos que mantêm o grande salão aquecido. "
            +"Do lado de fora há escadarias.";
        String repeat="O ar da manhã no Biblioplex carrega o aroma denso de pergaminho antigo, "
            +"cera de abelha e o calor difuso de encantamentos que mantêm o grande salão aquecido. "
            +"Há estudantes por todos os lados.";
        assertTrue(BraseiroNarrativeDirector.qualityProblem(repeat,new JSONArray().put(previous),
                "Olho ao redor","PLAYER_ACTION").contains("frase descritiva"));
    }

    @Test public void theatreAndEnrollmentOfferIndependentVisibleActions() {
        String narration="A Guilda de RPG Live-Action prepara armadilhas de papelão "
            +"para uma encenação. Uma funcionária orienta inscrições dos recém-chegados.";
        JSONArray options=SceneOptionDiversifier.ensureAlways(
                new JSONArray(),new JSONArray(),narration,"Acabo de chegar",
                new JSONObject(),1);
        assertEquals(5,options.length());
        assertTrue(StoryMomentumAction.isRequest(options.optJSONObject(0).optString("action")));
        assertTrue(options.toString().contains("ENCENAÇÃO"));
        assertTrue(options.toString().contains("INSCRIÇÃO"));
    }
}
