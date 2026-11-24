package br.com.tutorclinico;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;

/** Protects user-initiated local transcription while another app is visible. No mic access. */
public final class TranscriptionService extends Service {
    private static volatile Runnable timeout;
    private PowerManager.WakeLock wake;
    static boolean begin(Context c,Runnable onTimeout){
        timeout=onTimeout;
        try{c.startForegroundService(new Intent(c,TranscriptionService.class));return true;}
        catch(RuntimeException e){timeout=null;return false;}
    }
    static void end(Context c){timeout=null;c.stopService(new Intent(c,TranscriptionService.class));}
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
