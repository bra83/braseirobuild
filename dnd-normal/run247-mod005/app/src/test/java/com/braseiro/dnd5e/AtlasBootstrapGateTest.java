package com.braseiro.dnd5e;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AtlasBootstrapGateTest {
    @Test public void callbacksCannotStartAnotherAutomaticImport(){
        AtlasBootstrapGate gate=new AtlasBootstrapGate();
        assertTrue(gate.tryAutomaticImport());
        for(int i=0;i<100;i++)assertFalse(gate.tryAutomaticImport());
        gate.finish();
        for(int i=0;i<100;i++)assertFalse(gate.tryAutomaticImport());
    }
    @Test public void onlyOneConcurrentCallerStarts(){
        AtlasBootstrapGate gate=new AtlasBootstrapGate();
        java.util.concurrent.atomic.AtomicInteger accepted=new java.util.concurrent.atomic.AtomicInteger();
        java.util.stream.IntStream.range(0,100).parallel().forEach(i->{if(gate.tryAutomaticImport())accepted.incrementAndGet();});
        assertEquals(1,accepted.get());
    }
}
