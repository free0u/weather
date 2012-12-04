package ru.free0u.weather;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ServiceSettingsActivity extends Activity implements OnClickListener {
	boolean isUpdating = false;
	SharedPreferences sPref;
	
	void startServiceUpdate() {
		startService(new Intent(this, ServiceUpdater.class));
	}
	
	void stopServiceUpdate() {
		stopService(new Intent(this, ServiceUpdater.class));
	}
	
	void setUpButtons() {
		Button buttonStart = (Button)findViewById(R.id.buttonStartUpd);
        Button buttonStop = (Button)findViewById(R.id.buttonStopUpd);
        EditText et = (EditText)findViewById(R.id.editTextUpdateInterval);
        
        buttonStart.setEnabled(!isUpdating);
        et.setEnabled(!isUpdating);
        buttonStop.setEnabled(isUpdating);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonStartUpd: // start
			EditText et = (EditText)findViewById(R.id.editTextUpdateInterval);
			String timeStr = et.getText().toString();
			// TODO int time = Integer.parseInt(timeStr) * 60;
			int time = Integer.parseInt(timeStr) * 60;
			
			ServiceUpdater.updateInterval = time;
			startServiceUpdate();
			
			isUpdating = !isUpdating;
			setUpButtons();
			break;
		case R.id.buttonStopUpd: // stop
			
			Log.i("db", "stop button. updateInterval: " + ServiceUpdater.updateInterval);
			
			stopServiceUpdate();
			isUpdating = !isUpdating;
			setUpButtons();
			break;
		}
	}
	
	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
		    if (ServiceUpdater.class.getName().equals(service.service.getClassName())) {
		    	return true;
		    }
		}
		return false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.background_settings);
        
        Button buttonStart = (Button)findViewById(R.id.buttonStartUpd);
        Button buttonStop = (Button)findViewById(R.id.buttonStopUpd);
        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        EditText et = (EditText)findViewById(R.id.editTextUpdateInterval);
    	et.setText("");
    	
    	isUpdating = isMyServiceRunning();
    	setUpButtons();
	}
}
