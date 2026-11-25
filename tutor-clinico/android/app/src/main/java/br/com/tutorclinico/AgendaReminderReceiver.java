package br.com.tutorclinico;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Optional, approximate, private agenda reminders. No patient identifier in alarms or notifications. */
public final class AgendaReminderReceiver extends BroadcastReceiver {
    private static final String ACTION="br.com.tutorclinico.AGENDA_REMINDER";
    private static final String CHANNEL="eco_agenda_private";
    private static final long LEAD=30L*60*1000;
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("eco_agenda_reminders",Context.MODE_PRIVATE);}
    static boolean enabled(Context c){return prefs(c).getBoolean("enabled",false);}
    static boolean permitted(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    static void setEnabled(Context c,boolean enabled){
        prefs(c).edit().putBoolean("enabled",enabled).apply();
        if(enabled)refresh(c);else cancel(c);
    }
    private static PendingIntent alarmIntent(Context c){
        return PendingIntent.getBroadcast(c,601,new Intent(c,AgendaReminderReceiver.class).setAction(ACTION),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void cancel(Context c){
        c.getSystemService(AlarmManager.class).cancel(alarmIntent(c));
        c.getSystemService(NotificationManager.class).cancel(601);
    }
    static long nextReminder(JSONArray appointments,long now,ZoneId zone){
        long next=Long.MAX_VALUE;
        for(int i=0;i<appointments.length();i++){
            JSONObject a=appointments.optJSONObject(i);
            if(a==null||!"Agendado".equals(a.optString("status")))continue;
            try{
                long start=LocalDateTime.parse(a.getString("data")+"T"+a.getString("hora")).atZone(zone).toInstant().toEpochMilli();
                long due=start-LEAD;
                if(due>now&&due<next)next=due;
            }catch(Exception ignored){}
        }
        return next==Long.MAX_VALUE?-1:next;
    }
    static void refresh(Context c){
        Context app=c.getApplicationContext();
        if(!enabled(app))return;
        WORK.execute(()->refreshNow(app));
    }
    private static void refreshNow(Context c){
        if(!enabled(c)||!permitted(c)){cancel(c);return;}
        SecureStore store=null;
        try{
            store=new SecureStore(c);
            JSONArray appointments=store.readReminderAgenda();
            long due=nextReminder(appointments,System.currentTimeMillis(),ZoneId.systemDefault());
            if(!enabled(c)||!permitted(c))return;
            AlarmManager manager=c.getSystemService(AlarmManager.class);
            PendingIntent intent=alarmIntent(c);manager.cancel(intent);
            if(due>0)manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,intent);
        }catch(Exception ignored){
            // Do not expose clinical details on the lock screen or silently claim scheduling succeeded.
        }finally{if(store!=null)store.getDbHelper().close();}
    }
    @Override public void onReceive(Context context,Intent intent){
        Context c=context.getApplicationContext();
        if(!enabled(c)||!permitted(c))return;
        if(ACTION.equals(intent.getAction())){
            NotificationManager nm=c.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Lembretes privados do ECO",NotificationManager.IMPORTANCE_DEFAULT));
            PendingIntent open=PendingIntent.getActivity(c,602,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("ECO").setContentText("Confira sua agenda no aplicativo.").setVisibility(Notification.VISIBILITY_PRIVATE)
                .setContentIntent(open).setAutoCancel(true).build();
            nm.notify(601,n);
        }
        PendingResult pending=goAsync();
        WORK.execute(()->{try{refreshNow(c);}finally{pending.finish();}});
    }
}
