package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Sandbox time estimates are advisory until a validated host transaction commits. */
public class SandboxActionDurationTest {
    @Test public void recognizesWorkAndExploration() {
        assertEquals(1d,SandboxActionDuration.estimate("Procuro alguém que esteja contratando aventureiros").hours,0.001d);
        assertEquals(2d,SandboxActionDuration.estimate("Saio para caçar").hours,0.001d);
        assertEquals(4d,SandboxActionDuration.estimate("Trabalho na taverna").hours,0.001d);
        assertEquals(1d,SandboxActionDuration.estimate("Investigo os arredores da taverna").hours,0.001d);
        assertEquals(2d,SandboxActionDuration.estimate("Patrulho o bairro").hours,0.001d);
        assertEquals(1d,SandboxActionDuration.estimate("Investigar: Kvothe vasculha o arquivo").hours,0.001d);
        assertEquals(2d,SandboxActionDuration.estimate("Caçar: Kvothe segue os rastros").hours,0.001d);
    }

    @Test public void quickActivitiesHaveFractionalDuration() {
        assertEquals(.5d,SandboxActionDuration.estimate("Compro mantimentos").hours,0.001d);
        assertEquals(.25d,SandboxActionDuration.estimate("Converso com o taverneiro").hours,0.001d);
        assertEquals(.1d,SandboxActionDuration.estimate("Observo os arredores").hours,0.001d);
        assertEquals(.1d,SandboxActionDuration.estimate("Observar: Kvothe examina o barro").hours,0.001d);
    }

    @Test public void queriesAndTravelRemainNonAdvancing() {
        assertEquals(0d,SandboxActionDuration.estimate("Posso caçar aqui?").hours,0.001d);
        assertEquals(0d,SandboxActionDuration.estimate("Mestre, o que acontece se eu investigar?").hours,0.001d);
        assertEquals(0d,SandboxActionDuration.estimate("Não quero trabalhar na taverna").hours,0.001d);
        assertEquals(0d,SandboxActionDuration.estimate("Viajo até a cidade").hours,0.001d);
        assertEquals(0d,SandboxActionDuration.estimate("").hours,0.001d);
        assertEquals(0d,SandboxActionDuration.estimate(null).hours,0.001d);
    }

    @Test public void noRewardsChecksOrPositionMutationsAreProposed() {
        SandboxActionDuration.Estimate estimate=SandboxActionDuration.estimate("Saio para caçar");
        assertEquals("caca_coleta",estimate.category);
        assertEquals("sandbox_action_estimate",estimate.source);
        assertEquals(2d,estimate.hours,0.001d);
    }
}