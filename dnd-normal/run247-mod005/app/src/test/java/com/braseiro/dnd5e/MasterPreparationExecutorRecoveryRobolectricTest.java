package com.braseiro.dnd5e;

import android.os.Handler;
import android.os.Looper;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Reproduces the 6.6.63 screenshot: pool size=2, active=2, queued=0, completed=0. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public final class MasterPreparationExecutorRecoveryRobolectricTest {
    private static final class Recorder implements MasterPreparationCoordinator.Callbacks<String> {
        final AtomicInteger successes=new AtomicInteger();
        final AtomicInteger failures=new AtomicInteger();
        final AtomicInteger timeouts=new AtomicInteger();
        String result;
        @Override public void onPrepared(String value){result=value;successes.incrementAndGet();}
        @Override public void onFailure(Throwable error){failures.incrementAndGet();result=error.getMessage();}
        @Override public void onTimeout(){timeouts.incrementAndGet();}
    }

    private static void drainUiUntilSuccess(Recorder callback) throws InterruptedException {
        // The worker signals before returning; its main-looper callback is posted afterward.
        for (int i=0; i<200 && callback.successes.get()==0; i++) {
            shadowOf(Looper.getMainLooper()).idle();
            if (callback.successes.get()==0) Thread.sleep(15);
        }
    }

    private static Callable<String> uninterruptible(final CountDownLatch entered, final CountDownLatch release) {
        return () -> {
            entered.countDown();
            // Emulate blocked native/SQLite code that does not react to cancel(true).
            for (;;) {
                try { if (release.await(25, TimeUnit.MILLISECONDS)) return "STALE"; }
                catch (InterruptedException ignored) { /* deliberately non-cooperative */ }
            }
        };
    }

    @Test public void thirdAttemptRecoversFromTwoUninterruptibleStaleWorkers() throws Exception {
        ExecutorService pool=AppExecutors.isolatedMasterPreparationForTest();
        MasterPreparationCoordinator c=new MasterPreparationCoordinator(new Handler(Looper.getMainLooper()),pool,30000);
        CountDownLatch firstStarted=new CountDownLatch(1),secondStarted=new CountDownLatch(1);
        CountDownLatch release=new CountDownLatch(1),freshRan=new CountDownLatch(1);
        Recorder first=new Recorder(),second=new Recorder(),fresh=new Recorder();
        try {
            c.prepare(uninterruptible(firstStarted,release),first);
            assertTrue("First blocked worker must have started",firstStarted.await(3,TimeUnit.SECONDS));
            c.prepare(uninterruptible(secondStarted,release),second);
            assertTrue("Second blocked worker must have started",secondStarted.await(3,TimeUnit.SECONDS));
            c.prepare(()->{freshRan.countDown();return "FRESH SCENE";},fresh);
            assertTrue("The third attempt must not be rejected or starved",freshRan.await(3,TimeUnit.SECONDS));
            drainUiUntilSuccess(fresh);
            assertEquals(1,fresh.successes.get());
            assertEquals("FRESH SCENE",fresh.result);
            assertEquals(0,fresh.failures.get()+fresh.timeouts.get());
            assertEquals(0,first.successes.get()+second.successes.get());
        } finally {
            release.countDown();
            c.cancel();
            pool.shutdownNow();
        }
    }

    @Test public void runningPreparationSurvivesThirtySecondSoftDeadline() throws Exception {
        ExecutorService pool=AppExecutors.isolatedMasterPreparationForTest();
        MasterPreparationCoordinator c=new MasterPreparationCoordinator(new Handler(Looper.getMainLooper()),pool,30000);
        CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        Recorder cb=new Recorder();
        try{
            c.prepare(()->{entered.countDown();release.await();return "REGRAS E CONTEXTO PRONTOS";},cb);
            assertTrue(entered.await(3,TimeUnit.SECONDS));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));
            assertTrue(c.isPreparing());assertEquals(0,cb.timeouts.get());
            release.countDown();drainUiUntilSuccess(cb);
            assertEquals(1,cb.successes.get());assertEquals(0,cb.timeouts.get());
        }finally{release.countDown();c.cancel();pool.shutdownNow();}
    }

    @Test public void deadlineRetiresUninterruptibleWorkerAndRetrySucceeds() throws Exception {
        ExecutorService pool=AppExecutors.isolatedMasterPreparationForTest();
        MasterPreparationCoordinator c=new MasterPreparationCoordinator(new Handler(Looper.getMainLooper()),pool,60);
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),freshRan=new CountDownLatch(1);
        Recorder expired=new Recorder(),fresh=new Recorder();
        try {
            c.prepare(uninterruptible(entered,release),expired);
            assertTrue(entered.await(3,TimeUnit.SECONDS));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(75));
            assertEquals(1,expired.timeouts.get());
            c.prepare(()->{freshRan.countDown();return "CONTINUE";},fresh);
            assertTrue(freshRan.await(3,TimeUnit.SECONDS));
            drainUiUntilSuccess(fresh);
            assertEquals(1,fresh.successes.get());
            assertEquals(0,expired.successes.get());
        } finally {
            release.countDown();
            c.cancel();
            pool.shutdownNow();
        }
    }
}
