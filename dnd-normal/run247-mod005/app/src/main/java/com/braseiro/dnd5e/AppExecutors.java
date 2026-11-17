package com.braseiro.dnd5e;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Executores compartilhados para impedir a criação ilimitada de threads soltas. */
public final class AppExecutors {
    private static final ExecutorService IO = Executors.newFixedThreadPool(4, namedFactory("braseiro-io"));
    private static final ExecutorService NETWORK = Executors.newFixedThreadPool(4, namedFactory("braseiro-network"));
    private static final ExecutorService SERIAL = Executors.newSingleThreadExecutor(namedFactory("braseiro-serial"));
    private static final ExecutorService INDEXING = Executors.newSingleThreadExecutor(namedFactory("braseiro-index"));
    private static final ExecutorService HEROES = Executors.newSingleThreadExecutor(namedFactory("braseiro-heroes"));
    // Master context must not be rejected simply because two previous requests
    // ignored interruption after a deadline. Keep the queue bounded and purge
    // cancelled FutureTasks on every new submission; never execute stale scenes.
    // Core threads are prestarted so fresh work has a predictable execution path.
    private static final ThreadPoolExecutor MASTER_PREPARATION =
        new ThreadPoolExecutor(4,4,45L,TimeUnit.SECONDS,
                new java.util.concurrent.ArrayBlockingQueue<Runnable>(4),
                namedFactory("braseiro-master-prepare"),
                new ThreadPoolExecutor.AbortPolicy()) {
            @Override public void execute(Runnable command) {
                purge();
                super.execute(command);
            }
        };

    private AppExecutors() {}

    public static ExecutorService io() { return IO; }
    public static ExecutorService network() { return NETWORK; }
    public static ExecutorService serial() { return SERIAL; }
    /** Trabalho pesado de PDF fica serializado e isolado do IO geral para não saturar CPU/memória. */
    public static ExecutorService indexing() { return INDEXING; }
    /** Separate character sheet assembly from the Master's heavy I/O queue. */
    public static ExecutorService heroes() { return HEROES; }
    public static ExecutorService masterPreparation() { return MASTER_PREPARATION; }

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
