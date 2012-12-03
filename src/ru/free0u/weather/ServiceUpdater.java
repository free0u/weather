package ru.free0u.weather;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

public class ServiceUpdater extends Service {
	final static int NOTIFY_INTERVAL = 60 * 60 * 3; // 3 hours in sec

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
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		wd = new WeatherDatabase(getApplicationContext());
		int time = intent.getIntExtra("time", NOTIFY_INTERVAL); // sec
		
		if(mTimer != null) {
			mTimer.cancel();
		} else {
			mTimer = new Timer();
		}
		mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, time * 1000L);
		
		return super.onStartCommand(intent, flags, startId);
	}

	class TimeDisplayTimerTask extends TimerTask {
		@Override
		public void run() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					wd.updateAll();
				}
			});
		}
	}
}
