package ru.free0u.weather;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ServiceUpdater extends Service {
	final static int NOTIFY_INTERVAL = 60 * 60 * 3; // 3 hours in sec
	public static int updateInterval = NOTIFY_INTERVAL;
	Alarm alarm = new Alarm();
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("db", "onStartCommand. id = " + startId + " time: " + updateInterval);
		if (startId == 1) {
			alarm.setAlarm(getApplicationContext(), updateInterval);
			return START_STICKY;
		} else {
			return START_NOT_STICKY;
		}
	}
	
	@Override
	public void onDestroy() {
		Log.i("db", "Destroy service");
		alarm.cancelAlarm(getApplicationContext());
	}
	
}
