package ru.free0u.weather;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.util.ByteArrayBuffer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity implements OnClickListener,
		OnItemSelectedListener {
	WeatherHelper weather;
	ArrayList<Map<String, Object>> weatherListData;
	boolean updateButtonIsActive = true;
	LayoutInflater inflater;

	final String ATTRIBUTE_NAME_CITIES = "cities";
	ArrayList<String> cities = null;
	private int curPosCity;

	int updateTime = 60 * 60; // one hour

	WeatherDatabase weatherDatabase;

	// change UI - update weather forecast
	private void updateForecastWeather(ArrayList<Map<String, Object>> data) {
		LinearLayout lin = (LinearLayout) findViewById(R.id.linearLayoutWeatherList);
		lin.removeAllViewsInLayout();

		for (int i = 0; i < data.size(); ++i) {
			Map<String, Object> day = data.get(i);

			View v = inflater.inflate(R.layout.weather_item, null, false);

			TextView tv = (TextView) v.findViewById(R.id.textViewDate);
			tv.setText((String) day.get(weather.ATTRIBUTE_NAME_DATE));

			tv = (TextView) v.findViewById(R.id.textTemp);
			String temp = (String) day.get(weather.ATTRIBUTE_NAME_TEMPERATURE);
			while (temp.length() < 11) {
				temp += " ";
			}
			tv.setText(temp);

			// icon
			ImageView iv = (ImageView) v.findViewById(R.id.imageIconWeather);
			iv.setImageBitmap(null);
			if (Integer.parseInt((String) day.get("updated_icon")) == 0) { // need
																			// download
				ImageViewSetter task = new ImageViewSetter(iv,
						(String) day.get(weather.ATTRIBUTE_NAME_URL_ICON));
				task.execute();
			} else { // get from database
				byte[] pic = (byte[]) day.get("icon");
				ByteArrayInputStream stream = new ByteArrayInputStream(pic);
				Bitmap bm = BitmapFactory.decodeStream(stream);
				iv.setImageBitmap(bm);
			}
			lin.addView(v);

		}

	}

	// change UI - update current weather
	private void updateCurrentWeather(Map<String, Object> data) {
		// icon
		ImageView icon = (ImageView) findViewById(R.id.imageWeatherNow);
		icon.setImageBitmap(null);
		if (Integer.parseInt((String) data.get("updated_icon")) == 0) { // need
																		// download
			ImageViewSetter task = new ImageViewSetter(icon,
					(String) data.get(weather.ATTRIBUTE_NAME_URL_ICON));
			task.execute();
		} else { // get from database
			byte[] pic = (byte[]) data.get("icon");
			ByteArrayInputStream stream = new ByteArrayInputStream(pic);
			Bitmap bm = BitmapFactory.decodeStream(stream);
			icon.setImageBitmap(bm);
		}

		// temperature
		TextView tv = (TextView) findViewById(R.id.textViewTempNow);
		tv.setText((String) data.get(weather.ATTRIBUTE_NAME_TEMPERATURE) + " "
				+ getResources().getString(R.string.celsius));

		// wind
		tv = (TextView) findViewById(R.id.textViewWindNow);
		tv.setText((String) data.get(weather.ATTRIBUTE_NAME_WIND));

		// pressure
		tv = (TextView) findViewById(R.id.TextViewPressureNow);
		tv.setText((String) data.get(weather.ATTRIBUTE_NAME_PRESSURE));

		// update "last updated" field
		tv = (TextView) findViewById(R.id.textViewLastUpdated);
		tv.setText(getResources().getString(R.string.neverupdated));
		if (curPosCity < cities.size()) {
			int time = weatherDatabase.getUpdateTime(cities.get(curPosCity));
			if (time != -1) {
				String timeStr = weather.getLastUpdated(time);
				tv.setText(timeStr);
			}
		}

		// city name
		tv = (TextView) findViewById(R.id.textViewCityName);
		if (curPosCity < cities.size()) {
			tv.setText(cities.get(curPosCity));
		} else {
			tv.setText("");
		}
	}

	private void updateWeather(boolean needDownload) {
		if (curPosCity < cities.size()) {
			String city = cities.get(curPosCity);
			int lastUpd = weatherDatabase.getUpdateTime(city);
			int curTime = WeatherHelper.getUnixTime();
			
			if (needDownload) {
				WeatherDownloader task = new WeatherDownloader(city);
				task.execute();
			} else
			{
				if (curTime - lastUpd < updateTime) {
					updateCurrentWeather(weatherDatabase.getCurrentWeather(city));
					updateForecastWeather(weatherDatabase.getForecastWeather(city));
				} else {
					WeatherDownloader task = new WeatherDownloader(city);
					task.execute();
				}
			}
			
		}
	}

	private void setUpSpinner() {
		curPosCity = 0;
		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, cities);
		ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(ad);
		spinner.setOnItemSelectedListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null && data.hasExtra("city")) {
			// addCity(data.getStringExtra("city"));
			weatherDatabase.addCity(data.getStringExtra("city"));
		}
		cities = weatherDatabase.getCities();
		setUpSpinner();
		if (cities.size() > 0) {
			curPosCity = cities.size() - 1;
		}
		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		spinner.setSelection(curPosCity);
		updateWeather(false);
	}

	public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
			long arg3) {
		if (curPosCity != pos) {
			curPosCity = pos;
			updateWeather(false);
		}
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		weatherDatabase = new WeatherDatabase(this);

		weather = new WeatherHelper();

		// add weather.xml view
		LinearLayout lin = (LinearLayout) findViewById(R.id.weatherLayout);
		inflater = getLayoutInflater();
		inflater.inflate(R.layout.weather, lin, true);

		// setup buttons
		ImageView updateButton = (ImageView) findViewById(R.id.imageViewUpdate);
		updateButton.setOnClickListener(this);
		ImageView addButton = (ImageView) findViewById(R.id.imageViewAdd);
		addButton.setOnClickListener(this);
		Button buttonService = (Button) findViewById(R.id.buttonService);
		buttonService.setOnClickListener(this);

		// setup spinner
		cities = weatherDatabase.getCities();

		if (cities.size() == 0) {
			Intent intent = new Intent(this, CitiesActivity.class);
			startActivityForResult(intent, 0);
		} else {
			setUpSpinner();
			updateWeather(false);
		}
		
		if (!isMyServiceRunning()) {
			//startServiceUpdate(ServiceUpdater.NOTIFY_INTERVAL);
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
	
	void startServiceUpdate(int time) { // time in sec
	    Log.i("db", "start service from main");
		ServiceUpdater.updateInterval = time;
		startService(new Intent(this, ServiceUpdater.class));
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imageViewUpdate:
			if (updateButtonIsActive) {
				updateButtonIsActive = false;
				updateWeather(true);
			}
			break;
		case R.id.imageViewAdd:
			Intent intent = new Intent(this, CitiesActivity.class);
			startActivityForResult(intent, 0);
			break;
		case R.id.buttonService:
			Intent intent2 = new Intent(this, ServiceSettingsActivity.class);
			startActivity(intent2);
			break;
		}

	}

	class WeatherDownloader extends AsyncTask<Void, Void, String> {
		String urlData;
		String city;

		public WeatherDownloader(String city) {
			this.city = city;
		}

		@Override
		protected void onPreExecute() {
			urlData = weather.getUrl(city);

			updateButtonIsActive = false;
		}

		protected String convertStreamToString(InputStream is) {
			java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}

		@Override
		protected String doInBackground(Void... arg0) {
			String res = "";
			HttpURLConnection conn = null;
			try {
				URL url = new URL(urlData);
				conn = (HttpURLConnection) url.openConnection();
				InputStream in = new BufferedInputStream(conn.getInputStream());
				res = convertStreamToString(in);
			} catch (Exception ignore) {
			} finally {
				conn.disconnect();
			}
			return res;
		}

		@Override
		protected void onPostExecute(String data) {
			updateButtonIsActive = true;

			// change UI:
			if (data != null) {
				Log.i("db", "main.onPostExecute(). updateWeather()");
				weatherDatabase.updateForecastWeather(city,
						weather.getForecastWeather(data));
				updateForecastWeather(weatherDatabase.getForecastWeather(city));

				weatherDatabase.updateCurrentWeather(city,
						weather.getCurrentWeather(data));
				updateCurrentWeather(weatherDatabase.getCurrentWeather(city));
			}
		}
	}

	// usage:
	// ctor ImageViewSetter(ImageView v)
	// execute(url)
	class ImageViewSetter extends AsyncTask<Void, Void, byte[]> {
		ImageView v;
		String url;

		public ImageViewSetter(ImageView v, String url) {
			this.v = v;
			this.url = url;
		}

		@Override
		protected byte[] doInBackground(Void... arg0) {
			byte[] res = new byte[0];
			try {
				URL imageUrl = new URL(url);
				URLConnection ucon = imageUrl.openConnection();
				InputStream is = ucon.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				ByteArrayBuffer baf = new ByteArrayBuffer(500);
				int current = 0;
				while ((current = bis.read()) != -1) {
					baf.append((byte) current);
				}
				res = baf.toByteArray();
			} catch (Exception ignore) {
			}
			return res;
		}

		@Override
		protected void onPostExecute(byte[] pic) {
			ByteArrayInputStream stream = new ByteArrayInputStream(pic);
			Bitmap bm = BitmapFactory.decodeStream(stream);
			v.setImageBitmap(bm);
			weatherDatabase.setIcon(url, pic);
		}
	}

}
