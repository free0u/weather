package ru.free0u.weather;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ServiceUpdater extends Service {
	final static int NOTIFY_INTERVAL = 60 * 60 * 3; // 3 hours in sec

	public static int updateInterval = NOTIFY_INTERVAL;
	
	WeatherDatabase wd;
	private Handler mHandler = new Handler();
	private Timer mTimer = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		if (mTimer != null) {
			mTimer.cancel();
		}
		Log.i("db", "Service down!");
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		wd = new WeatherDatabase(getApplicationContext());
		if(mTimer != null) {
			mTimer.cancel();
		} else {
			mTimer = new Timer();
		}
		mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, updateInterval * 1000L);
		Log.i("db", "Timer run. Time: " + updateInterval);
		return START_STICKY;
	}

	class TimeDisplayTimerTask extends TimerTask {
		@Override
		public void run() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					wd.updateAll();
					Log.i("db", "updating weather");
				}
			});
		}
	}
}
