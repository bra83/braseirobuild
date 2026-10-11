package com.braseiro.dnd5e;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/**
 * Mantém a mesa de áudio viva com a tela apagada ou durante uma troca breve de
 * aplicativo. O usuário continua controlando tudo pela aba Áudio.
 */
public final class AudioPlaybackService extends Service {
    private static final String CHANNEL_ID="braseiro_audio";
    private static final int NOTIFICATION_ID=520;
    private static AudioController controller;

    public static synchronized AudioController controller(Context context){
        if(controller==null)controller=new AudioController(context.getApplicationContext());
        return controller;
    }

    public void onCreate(){
        super.onCreate();controller(this);createChannel();startForeground(NOTIFICATION_ID,notification());
    }

    public int onStartCommand(Intent intent,int flags,int startId){
        startForeground(NOTIFICATION_ID,notification());return START_NOT_STICKY;
    }

    public void onTaskRemoved(Intent rootIntent){
        if(controller!=null)controller.stopAll();
        stopForeground(true);stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    public void onDestroy(){
        if(controller!=null)controller.stopAll();controller=null;super.onDestroy();
    }

    public IBinder onBind(Intent intent){return null;}

    private void createChannel(){
        if(Build.VERSION.SDK_INT<26)return;NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"Áudio do Braseiro",NotificationManager.IMPORTANCE_LOW);channel.setDescription("Mantém voz, música e ambiência durante a campanha.");NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);manager.createNotificationChannel(channel);
    }

    private Notification notification(){
        Intent open=new Intent(this,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);
        return builder.setContentTitle("Braseiro D&D").setContentText("Áudio imersivo ativo").setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(pending).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build();
    }
}
