package ru.free0u.weather;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity implements OnClickListener, OnItemSelectedListener {
	WeatherHelper weather;
	ArrayList<Map<String, Object>> weatherListData;
	boolean updateButtonIsActive = true;
	LayoutInflater inflater;
	
	final String ATTRIBUTE_NAME_CITIES = "cities";
	ArrayList<String> cities = null;
	private int curPosCity;
	
	int updateTime = 60 * 60; // one hour
	
	// TODO think
	WeatherDatabase weatherDatabase;
	
	// change UI - update weather forecast
	private void updateForecastWeather(ArrayList<Map<String, Object>> data) {
		LinearLayout lin = (LinearLayout)findViewById(R.id.linearLayoutWeatherList);
		lin.removeAllViewsInLayout();
		
		for (int i = 0; i < data.size(); ++i) {
			Map<String, Object> day = data.get(i);
			
			View v = inflater.inflate(R.layout.weather_item, null, false);
			
			TextView tv = (TextView)v.findViewById(R.id.textViewDate);
			tv.setText((String)day.get(weather.ATTRIBUTE_NAME_DATE));
			
			tv = (TextView)v.findViewById(R.id.textTemp);
			tv.setText((String)day.get(weather.ATTRIBUTE_NAME_TEMPERATURE));
			
			ImageView iv = (ImageView)v.findViewById(R.id.imageIconWeather);
			iv.setImageBitmap(null);
			ImageViewSetter task = new ImageViewSetter(iv);
			task.execute((String)day.get(weather.ATTRIBUTE_NAME_URL_ICON));
			
			lin.addView(v);
			
		}
		
	}

	// change UI - update current weather
	private void updateCurrentWeather(Map<String, String> data) {
		// icon
		ImageView icon = (ImageView)findViewById(R.id.imageWeatherNow);
		icon.setImageBitmap(null);
		if (Integer.parseInt(data.get("updated_icon")) == 0) { // need download
			ImageViewSetter task = new ImageViewSetter(icon);
			task.execute(data.get(weather.ATTRIBUTE_NAME_URL_ICON));
		}
		
		
		// temperature
		TextView tv = (TextView)findViewById(R.id.textViewTempNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_TEMPERATURE) + " " +
				getResources().getString(R.string.celsius));
		
		// wind
		tv = (TextView)findViewById(R.id.textViewWindNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_WIND));
		
		// pressure
		tv = (TextView)findViewById(R.id.TextViewPressureNow);
		tv.setText(data.get(weather.ATTRIBUTE_NAME_PRESSURE));
		
		// update "last updated" field
		tv = (TextView)findViewById(R.id.textViewLastUpdated);
		tv.setText(getResources().getString(R.string.neverupdated));
		if (curPosCity < cities.size()) {
			int time = weatherDatabase.getUpdateTime(cities.get(curPosCity));
			if (time != -1) {
				String timeStr = weather.getLastUpdated(time);
				tv.setText(timeStr);
			}
		}
		
		// city name
		tv = (TextView)findViewById(R.id.textViewCityName);
		if (curPosCity < cities.size()) {
			tv.setText(cities.get(curPosCity));
			int time = weatherDatabase.getUpdateTime(cities.get(curPosCity));
			String timeStr = weather.getLastUpdated(time);
			tv.setText(timeStr);
		} else
		{
			tv.setText("");
		}
	}
	
	
	private void updateWeather(boolean needDownload) {
		if (curPosCity < cities.size()) {
			String city = cities.get(curPosCity);
			int lastUpd = weatherDatabase.getUpdateTime(city);
			int curTime = WeatherHelper.getUnixTime();
			if (!needDownload && (curTime - lastUpd < updateTime)) {
				updateCurrentWeather(weatherDatabase.getCurrentWeather(city));
				updateForecastWeather(weatherDatabase.getForecastWeather(city));
			} else {
				WeatherDownloader task = new WeatherDownloader(city);
				task.execute();
			}
		}
	}
	
	private void setUpSpinner() {
		curPosCity = 0;
		Spinner spinner = (Spinner)findViewById(R.id.spinner1);
		ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cities);
    	ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	spinner.setAdapter(ad);
    	spinner.setOnItemSelectedListener(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data != null && data.hasExtra("city")) {
			//addCity(data.getStringExtra("city"));
			weatherDatabase.addCity(data.getStringExtra("city"));
		}
		cities = weatherDatabase.getCities();
		setUpSpinner();
		if (cities.size() > 0) {
			curPosCity = cities.size() - 1;
		}
		Spinner spinner = (Spinner)findViewById(R.id.spinner1);
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
	public void onDestroy() {
		super.onDestroy();
		weatherDatabase.close();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
   
        weatherDatabase = new WeatherDatabase(this);
        weatherDatabase.test();
        
        weather = new WeatherHelper();
        
        // add weather.xml view
        LinearLayout lin = (LinearLayout)findViewById(R.id.weatherLayout);
        inflater = getLayoutInflater();
        inflater.inflate(R.layout.weather, lin, true);
        
        // setup buttons
        ImageView updateButton = (ImageView)findViewById(R.id.imageViewUpdate);
        updateButton.setOnClickListener(this);
        ImageView addButton = (ImageView)findViewById(R.id.imageViewAdd);
        addButton.setOnClickListener(this);
    
        // setup spinner
        cities = weatherDatabase.getCities();
        
        if (cities.size() == 0) {
        	Intent intent = new Intent(this, CitiesActivity.class);
    		startActivityForResult(intent, 0);
        } else {
        	setUpSpinner();
            updateWeather(false);
        }
    }
    
    public void onClick(View v) {
    	switch(v.getId()) {
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
    		
    		Toast t = Toast.makeText(getApplicationContext(), "Weather downloading...", Toast.LENGTH_SHORT);
    		t.show();
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
				conn = (HttpURLConnection)url.openConnection();
				InputStream in = new BufferedInputStream(conn.getInputStream());
				res = convertStreamToString(in);
			}
			catch (Exception ignore) {
			}
			finally {
				conn.disconnect();
			}
			return res;
		}
    	
		@Override
		protected void onPostExecute(String data) {
			updateButtonIsActive = true;
			
			// change UI:
			if (data != null) {
				weatherDatabase.updateForecastWeather(city, weather.getForecastWeather(data));
				updateForecastWeather(weatherDatabase.getForecastWeather(city));
				
				weatherDatabase.updateCurrentWeather(city, weather.getCurrentWeather(data));
				updateCurrentWeather(weatherDatabase.getCurrentWeather(city));
			}
			
			Toast t = Toast.makeText(getApplicationContext(), "Weather downloaded!", Toast.LENGTH_SHORT);
    		t.show();
		}
    }
    
    // usage: 
    // ctor ImageViewSetter(ImageView v)
    // execute(url)
    class ImageViewSetter extends AsyncTask<String, Void, Bitmap> {
    	ImageView v;
    	
    	public ImageViewSetter(ImageView v) {
    		this.v = v;
		}
    	
		@Override
		protected Bitmap doInBackground(String... arg0) {
			Bitmap bm = null;
			try {
		        URLConnection conn = new URL(arg0[0]).openConnection();
		        conn.connect();
		        bm = BitmapFactory.decodeStream(conn.getInputStream());
		    } catch (Exception ignore) {
		    }
			return bm;
		}
    	
		@Override
		protected void onPostExecute(Bitmap bm) {
			v.setImageBitmap(bm);
		}
    }

	
}
