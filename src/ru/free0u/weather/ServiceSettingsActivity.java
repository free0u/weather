package ru.free0u.weather;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ServiceSettingsActivity extends Activity implements OnClickListener {
	boolean isUpdating = false;
	SharedPreferences sPref;
	
	void startServiceUpdate(int time) { // time in sec
		Intent intent = new Intent(this, ServiceUpdater.class);
		intent.putExtra("time", time);
		startService(intent);
	}
	
	void stopServiceUpdate() {
		stopService(new Intent(this, ServiceUpdater.class));
	}
	
	void setUpButtons() {
		Button buttonStart = (Button)findViewById(R.id.buttonStartUpd);
        Button buttonStop = (Button)findViewById(R.id.buttonStopUpd);
        
        buttonStart.setEnabled(!isUpdating);
        buttonStop.setEnabled(isUpdating);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonStartUpd: // start
			EditText et = (EditText)findViewById(R.id.editTextUpdateInterval);
			String timeStr = et.getText().toString();
			int time = Integer.parseInt(timeStr) * 60;
			saveIntInPref(time);
			startServiceUpdate(time);
			isUpdating = !isUpdating;
			setUpButtons();
			break;
		case R.id.buttonStopUpd: // stop
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
	
	private void saveIntInPref(int x) {
		sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
	    Editor ed = sPref.edit();
	    ed.putInt("myint", x);
	    ed.commit();
	}
	private int loadIntFromPref() {
		sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
	    return sPref.getInt("myint", 10);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.background_settings);
        
        Button buttonStart = (Button)findViewById(R.id.buttonStartUpd);
        Button buttonStop = (Button)findViewById(R.id.buttonStopUpd);
        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        
        // test service
        isUpdating = isMyServiceRunning();
        if (isUpdating) {
        	int x = loadIntFromPref();
        	EditText et = (EditText)findViewById(R.id.editTextUpdateInterval);
        	et.setText(String.valueOf(x / 60));
        }
        
        setUpButtons();
	}
}