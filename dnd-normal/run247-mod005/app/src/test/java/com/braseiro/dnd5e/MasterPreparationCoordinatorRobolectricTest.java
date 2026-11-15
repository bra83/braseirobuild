package com.braseiro.dnd5e;

import android.os.Handler;
import android.os.Looper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Simulates a queued/blocked MasterOrchestrator.prepare() without Gemini or save mutation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public final class MasterPreparationCoordinatorRobolectricTest {
    private static class ManualExecutor extends AbstractExecutorService {
        final List<Runnable> tasks=new ArrayList<>();
        boolean stopped=false;
        boolean reject=false;
        @Override public void execute(Runnable job){
            if(reject)throw new RejectedExecutionException("all master preparation workers are busy");
            tasks.add(job);
        }
        @Override public void shutdown(){stopped=true;}
        @Override public List<Runnable> shutdownNow(){stopped=true;return new ArrayList<>();}
        @Override public boolean isShutdown(){return stopped;}
        @Override public boolean isTerminated(){return stopped;}
        @Override public boolean awaitTermination(long timeout,TimeUnit unit){return stopped;}
    }

    private static final class Callback implements MasterPreparationCoordinator.Callbacks<String> {
        final AtomicInteger success=new AtomicInteger();
        final AtomicInteger failure=new AtomicInteger();
        final AtomicInteger timeout=new AtomicInteger();
        String payload;
        @Override public void onPrepared(String value){success.incrementAndGet();payload=value;}
        @Override public void onFailure(Throwable error){failure.incrementAndGet();payload=error.getClass().getSimpleName();}
        @Override public void onTimeout(){timeout.incrementAndGet();}
    }
    private MasterPreparationCoordinator coordinator(ManualExecutor pool,long deadline){
        return new MasterPreparationCoordinator(new Handler(Looper.getMainLooper()),pool,deadline);
    }

    @Test public void blockedMasterCannotTrapUserForeverOrConsumeLateResult(){
        ManualExecutor pool=new ManualExecutor();
        MasterPreparationCoordinator c=coordinator(pool,30000);
        Callback callback=new Callback();
        c.prepare(()->"LATE NARRATION",callback);
        assertTrue(c.isPreparing());
        // The queued worker never runs: same observable state as a hung planner.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));
        assertFalse("A deadline must release the blocked turn",c.isPreparing());
        assertEquals(1,callback.timeout.get());
        assertEquals(0,callback.success.get());
        pool.tasks.get(0).run(); // FutureTask was cancelled; MUST be ignored.
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("No stale scene may be accepted after timeout",0,callback.success.get());
        assertEquals(0,callback.failure.get());
    }

    @Test public void retryStartsFreshAndDoesNotReuseAbandonedPlan(){
        ManualExecutor pool=new ManualExecutor();
        MasterPreparationCoordinator c=coordinator(pool,80);
        Callback old=new Callback(),fresh=new Callback();
        c.prepare(()->"OLD",old);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(85));
        c.prepare(()->"NEW",fresh);
        pool.tasks.get(1).run();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("NEW",fresh.payload);
        assertEquals(1,fresh.success.get());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        assertEquals(0,fresh.timeout.get());
        assertEquals(1,old.timeout.get());
        assertEquals(0,old.success.get());
    }

    @Test public void executorSaturationFailsQuicklyInsteadOfLeavingStatusStuck(){
        ManualExecutor pool=new ManualExecutor();pool.reject=true;
        MasterPreparationCoordinator c=coordinator(pool,30000);
        Callback cb=new Callback();
        c.prepare(()->"UNREACHABLE",cb);
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(c.isPreparing());
        assertEquals(1,cb.failure.get());
        assertEquals("RejectedExecutionException",cb.payload);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));
        assertEquals(0,cb.timeout.get());
    }

    @Test public void activityTeardownCancelsWorkWithoutFabricatedSuccess(){
        ManualExecutor pool=new ManualExecutor();
        MasterPreparationCoordinator c=coordinator(pool,500);
        Callback cb=new Callback();
        c.prepare(()->"NOT APPLIED",cb);
        c.cancel();
        pool.tasks.get(0).run();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertFalse(c.isPreparing());
        assertEquals(0,cb.success.get()+cb.failure.get()+cb.timeout.get());
    }
}
