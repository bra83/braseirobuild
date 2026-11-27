package com.braseiro.dnd5e;

import android.os.Handler;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Exactly-once ownership of the Master's pre-Gemini context preparation.
 * Completion, failure and deadline race on the UI thread, but only one wins.
 * A timed-out worker is interrupted; its late result is never consumed.
 */
public final class MasterPreparationCoordinator {
    public interface Callbacks<T> {
        void onPrepared(T result);
        void onFailure(Throwable error);
        void onTimeout();
    }

    private final Handler ui;
    private final ExecutorService executor;
    private final long timeoutMs;
    private long sequence;
    private long active = -1L;
    private Future<?> worker;
    private Runnable watchdog;
    private boolean taskStarted;
    private boolean graceUsed;

    public MasterPreparationCoordinator(Handler ui,ExecutorService executor,long timeoutMs) {
        if(ui==null||executor==null||timeoutMs<=0)throw new IllegalArgumentException("Invalid master context coordinator");
        this.ui=ui;this.executor=executor;this.timeoutMs=timeoutMs;
    }

    public <T> void prepare(final Callable<T> task,final Callbacks<T> callbacks) {
        if(task==null||callbacks==null)throw new IllegalArgumentException("Missing plan task/callback");
        cancel();
        final long ticket;
        synchronized(this) { ticket=++sequence;active=ticket;taskStarted=false;graceUsed=false; }
        final Runnable deadline=new Runnable(){@Override public void run(){
            final Future<?> expired;
            synchronized(MasterPreparationCoordinator.this){
                if(active!=ticket)return;
                // A running production preparation gets a soft deadline before the hard stop.
                if(timeoutMs==30000L&&taskStarted&&!graceUsed){
                    graceUsed=true;
                    ui.postDelayed(this,150000L);
                    return;
                }
                active=-1L;expired=worker;worker=null;watchdog=null;
            }
            if(expired!=null)expired.cancel(true);
            AppExecutors.masterPreparationTimedOut(executor);
            callbacks.onTimeout();
        }};
        synchronized(this){watchdog=deadline;}
        ui.postDelayed(deadline,timeoutMs);
        try {
            final Future<?> scheduled=executor.submit(new Runnable(){@Override public void run(){
                try {
                    synchronized(MasterPreparationCoordinator.this){if(active!=ticket)return;taskStarted=true;}
                    final T prepared=task.call();
                    ui.post(new Runnable(){@Override public void run(){
                        if(claimCompletion(ticket))callbacks.onPrepared(prepared);
                    }});
                }catch(final Throwable error){
                    ui.post(new Runnable(){@Override public void run(){
                        if(claimCompletion(ticket))callbacks.onFailure(error);
                    }});
                }
            }});
            synchronized(this){
                if(active==ticket)worker=scheduled;
                else scheduled.cancel(true);
            }
        }catch(final RejectedExecutionException rejected){
            ui.post(new Runnable(){@Override public void run(){
                if(claimCompletion(ticket))callbacks.onFailure(rejected);
            }});
        }
    }

    private boolean claimCompletion(long ticket){
        final Runnable oldWatchdog;
        synchronized(this){
            if(active!=ticket)return false;
            active=-1L;worker=null;oldWatchdog=watchdog;watchdog=null;
        }
        if(oldWatchdog!=null)ui.removeCallbacks(oldWatchdog);
        return true;
    }

    /** Stops old work before Activity teardown, without producing a false scene. */
    public void cancel(){
        final Future<?> f;final Runnable timer;
        synchronized(this){
            ++sequence;active=-1L;
            f=worker;worker=null;timer=watchdog;watchdog=null;
        }
        if(timer!=null)ui.removeCallbacks(timer);
        if(f!=null)f.cancel(true);
    }

    public synchronized boolean isPreparing(){return active>=0L;}
}
