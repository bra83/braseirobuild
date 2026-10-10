package com.braseiro.dnd5e;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Watchdog de homologação: registra congelamentos da thread UI sem derrubar o app. */
public final class UiResponsivenessMonitor {
    private final Handler main=new Handler(Looper.getMainLooper());private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();private final SharedPreferences prefs;private volatile long lastAck=System.currentTimeMillis();private volatile boolean running;
    public UiResponsivenessMonitor(Context c){prefs=c.getSharedPreferences("ui_responsiveness_v5",Context.MODE_PRIVATE);}
    public void start(){if(running)return;running=true;worker.scheduleAtFixedRate(()->{long sent=System.currentTimeMillis();main.post(()->lastAck=System.currentTimeMillis());worker.schedule(()->{long lag=System.currentTimeMillis()-Math.max(sent,lastAck);if(running&&lag>1200){long max=Math.max(lag,prefs.getLong("max_lag_ms",0));prefs.edit().putLong("max_lag_ms",max).putLong("last_lag_ms",lag).putLong("last_lag_at",System.currentTimeMillis()).putInt("lag_count",prefs.getInt("lag_count",0)+1).apply();AppLog.w("UiResponsiveness","Thread de interface atrasada em "+lag+" ms.",null);}},1400,TimeUnit.MILLISECONDS);},0,750,TimeUnit.MILLISECONDS);}
    public void stop(){running=false;worker.shutdownNow();}
    public long maxLagMs(){return prefs.getLong("max_lag_ms",0);}public int lagCount(){return prefs.getInt("lag_count",0);}
}
