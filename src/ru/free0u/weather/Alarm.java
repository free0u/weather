package ru.free0u.weather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class Alarm extends BroadcastReceiver 
{    
     @Override
     public void onReceive(Context context, Intent intent) 
     {   
         PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
         PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
         wl.acquire();

         // Put here YOUR code.
         WeatherDatabase wd = new WeatherDatabase(context);
         wd.updateAll();
         
         wl.release();
     }

 public void setAlarm(Context context, int time)
 {
     AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
     Intent i = new Intent(context, Alarm.class);
     PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
     am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000L * time, pi); // Millisec * Second * Minute
     }

     public void cancelAlarm(Context context)
     {
         Intent intent = new Intent(context, Alarm.class);
         PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
         AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
         alarmManager.cancel(sender);
     }
 }