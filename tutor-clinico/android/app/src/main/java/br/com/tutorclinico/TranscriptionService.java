package br.com.tutorclinico;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;

/** Protects user-initiated local transcription while another app is visible. Manages queue state. */
public final class TranscriptionService extends Service {
    private static volatile Runnable timeout;
    private static volatile String activeJobId = "";
    private static volatile String activeCaseId = "";
    private static volatile String activeSessionId = "";
    private static volatile String activeState = "IDLE";
    private PowerManager.WakeLock wake;
    // Process-owned engine; the Activity is only a detachable client. One active
    // decoder at a time; durable interrupted jobs remain in SQLite.
    private static MlSessionEngine engine;
    static synchronized MlSessionEngine engine(Context context){
        if(engine==null)engine=new MlSessionEngine(context.getApplicationContext());
        return engine;
    }
    static void detach(Activity host){
        MlSessionEngine e;
        synchronized(TranscriptionService.class){e=engine;}
        if(e!=null)e.detach(host);
    }

    public static synchronized void registerJob(String jobId, String caseId, String sessionId){
        activeJobId = jobId == null ? "" : jobId;
        activeCaseId = caseId == null ? "" : caseId;
        activeSessionId = sessionId == null ? "" : sessionId;
        activeState = "PROCESSING";
    }

    public static synchronized void registerJob(String jobId, String caseId){
        registerJob(jobId, caseId, "");
    }

    public static synchronized void clearJob(){
        activeJobId = "";
        activeCaseId = "";
        activeSessionId = "";
        activeState = "IDLE";
    }

    public static String getActiveJobId(){ return activeJobId; }
    public static String getActiveCaseId(){ return activeCaseId; }
    public static String getActiveSessionId(){ return activeSessionId; }
    public static String getActiveState(){ return activeState; }

    static boolean begin(Context c,Runnable onTimeout){
        timeout=onTimeout;
        try{c.startForegroundService(new Intent(c,TranscriptionService.class));return true;}
        catch(RuntimeException e){timeout=null;return false;}
    }
    static void end(Context c){timeout=null;clearJob();c.stopService(new Intent(c,TranscriptionService.class));}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int id){
        String channel="eco_transcription";
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(channel,"Transcrição local",NotificationManager.IMPORTANCE_LOW));
        Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,channel).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("ECO · transcrição local").setContentText("Processando áudio autorizado. Toque para acompanhar ou pausar.")
            .setOngoing(true).setVisibility(Notification.VISIBILITY_SECRET).setContentIntent(pi).build();
        if(Build.VERSION.SDK_INT>=35)startForeground(734,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING);
        else startForeground(734,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        if(timeout==null){stopSelf();return START_NOT_STICKY;}
        if(wake==null){wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ECO:transcription");wake.acquire(6*60*60*1000L);}
        return START_NOT_STICKY;
    }
    @Override public void onTimeout(int startId,int fgsType){Runnable action=timeout;if(action!=null)action.run();stopSelf();}
    @Override public void onDestroy(){if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
}
