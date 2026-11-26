package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Isolated app workers. The Master must never inherit a permanently saturated pool. */
public final class AppExecutors {
    private static final ExecutorService IO = Executors.newFixedThreadPool(4, namedFactory("braseiro-io"));
    // Never queue launch/preflight behind background indexing, RAG or map imports.
    private static final ExecutorService STARTUP = Executors.newSingleThreadExecutor(namedFactory("braseiro-startup"));
    // Chaquopy boot must not block launch or the general RAG/image I/O pool.
    private static final ExecutorService BARBARA_BOOT = Executors.newSingleThreadExecutor(namedFactory("braseiro-barbara-boot"));
    private static final ExecutorService NETWORK = Executors.newFixedThreadPool(4, namedFactory("braseiro-network"));
    private static final ExecutorService SERIAL = Executors.newSingleThreadExecutor(namedFactory("braseiro-serial"));
    private static final ExecutorService INDEXING = Executors.newSingleThreadExecutor(namedFactory("braseiro-index"));
    private static final ExecutorService HEROES = Executors.newSingleThreadExecutor(namedFactory("braseiro-heroes"));
    private static final ExecutorService MASTER_PREPARATION = new RecoveringMasterExecutor();
    // This pool cannot be saturated by failed Gemini/RAG context preparation.
    private static final ExecutorService OFFLINE_RECOVERY = new RecoveringMasterExecutor();

    private AppExecutors() {}

    public static ExecutorService io() { return IO; }
    public static ExecutorService startup() { return STARTUP; }
    public static ExecutorService barbaraBoot() { return BARBARA_BOOT; }
    public static ExecutorService network() { return NETWORK; }
    public static ExecutorService serial() { return SERIAL; }
    public static ExecutorService indexing() { return INDEXING; }
    public static ExecutorService heroes() { return HEROES; }
    public static ExecutorService masterPreparation() { return MASTER_PREPARATION; }
    public static ExecutorService offlineRecovery() { return OFFLINE_RECOVERY; }

    /** A real timeout must retire the blocked workers, not merely cancel their Future. */
    static void masterPreparationTimedOut(ExecutorService selected) {
        if (selected instanceof RecoveringMasterExecutor) {
            ((RecoveringMasterExecutor) selected).retireTimedOutWorkers();
        }
    }

    /** Fresh, isolated instance for the real-thread regression tests. */
    static ExecutorService isolatedMasterPreparationForTest() {
        return new RecoveringMasterExecutor();
    }

    /**
     * Future.cancel(true) does not free a thread when SQLite/native code ignores interruption.
     * With a SynchronousQueue and only two workers, the third attempt was rejected forever.
     * Retire a stuck generation when saturated or on a preparation deadline. The returned
     * ExecutorService identity remains stable for the Activity's coordinator; late futures
     * remain cancelled and cannot pass the coordinator's ticket gate.
     *
     * Deliberately cap retired *active* threads: no infinite thread creation if storage is
     * irrecoverably blocked. Never run preparation on the UI thread and never queue stale plans.
     */
    private static final class RecoveringMasterExecutor extends AbstractExecutorService {
        private static final int MAX_RETIRED_BUSY_THREADS = 8;
        private final List<ThreadPoolExecutor> retired = new ArrayList<>();
        private ThreadPoolExecutor current = newGeneration();
        private boolean stopped;

        private static ThreadPoolExecutor newGeneration() {
            return new ThreadPoolExecutor(0, 2, 45L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), namedFactory("braseiro-master-prepare"),
                    new ThreadPoolExecutor.AbortPolicy());
        }

        private int stillBusy() {
            int busy = 0;
            for (int i = retired.size() - 1; i >= 0; i--) {
                ThreadPoolExecutor older = retired.get(i);
                if (older.isTerminated()) retired.remove(i);
                else busy += older.getActiveCount();
            }
            return busy;
        }

        private void rotate() {
            if (stopped) throw new RejectedExecutionException("Executor do Mestre encerrado");
            if (stillBusy() + current.getActiveCount() > MAX_RETIRED_BUSY_THREADS) {
                throw new RejectedExecutionException(
                        "Preparações antigas permaneceram bloqueadas; force a parada e reabra o aplicativo. Nenhuma cena foi confirmada.");
            }
            ThreadPoolExecutor old = current;
            current = newGeneration();
            retired.add(old);
            old.shutdownNow();
            old.purge();
        }

        @Override public synchronized void execute(Runnable command) {
            if (command == null) throw new NullPointerException("command");
            if (stopped) throw new RejectedExecutionException("Executor do Mestre encerrado");
            try {
                current.execute(command);
            } catch (RejectedExecutionException busy) {
                if (current.isShutdown()) throw busy;
                rotate();
                current.execute(command);
            }
        }

        synchronized void retireTimedOutWorkers() {
            if (stopped || current.getActiveCount() == 0) return;
            // An interrupted Future can continue executing native/SQLite work.
            // The next attempt gets a new generation while stale results are ticket-fenced.
            try {
                rotate();
            } catch (RejectedExecutionException exhausted) {
                AppLog.e("AppExecutors", "Limite de recuperações do Mestre atingido.", exhausted);
            }
        }

        @Override public synchronized void shutdown() {
            if (stopped) return;
            stopped = true;
            current.shutdown();
            for (ThreadPoolExecutor older : retired) older.shutdown();
        }

        @Override public synchronized List<Runnable> shutdownNow() {
            stopped = true;
            List<Runnable> pending = new ArrayList<>(current.shutdownNow());
            for (ThreadPoolExecutor older : retired) pending.addAll(older.shutdownNow());
            return pending;
        }

        @Override public synchronized boolean isShutdown() { return stopped; }

        @Override public synchronized boolean isTerminated() {
            if (!stopped || !current.isTerminated()) return false;
            for (ThreadPoolExecutor older : retired) if (!older.isTerminated()) return false;
            return true;
        }

        @Override public boolean awaitTermination(long timeout, TimeUnit unit)
                throws InterruptedException {
            long end = System.nanoTime() + unit.toNanos(timeout);
            final List<ThreadPoolExecutor> pools;
            synchronized (this) {
                if (!stopped) return false;
                pools = new ArrayList<>(retired);
                pools.add(current);
            }
            for (ThreadPoolExecutor pool : pools) {
                long remaining = end - System.nanoTime();
                if (remaining <= 0 || !pool.awaitTermination(remaining, TimeUnit.NANOSECONDS))
                    return false;
            }
            return true;
        }
    }

    private static ThreadFactory namedFactory(final String prefix) {
        return new ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger(1);
            @Override public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, prefix + "-" + sequence.getAndIncrement());
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override public void uncaughtException(Thread t, Throwable e) {
                        AppLog.e("AppExecutors", "Falha não tratada em " + t.getName(), e);
                    }
                });
                return thread;
            }
        };
    }
}
